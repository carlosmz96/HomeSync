package com.homesync.household.service;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.homesync.household.dto.HouseholdMemberResponse;
import com.homesync.household.entity.HouseholdMembership;
import com.homesync.household.entity.HouseholdRole;
import com.homesync.household.repository.HouseholdMembershipRepository;
import com.homesync.identity.entity.User;
import com.homesync.identity.repository.UserRepository;
import com.homesync.shared.security.CurrentUserService;
import com.homesync.task.entity.TaskAssignment;
import com.homesync.task.repository.TaskAssignmentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HouseholdMembershipService {

    private final HouseholdMembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final TaskAssignmentRepository taskAssignmentRepository;

    private final CurrentUserService currentUserService;
    private final HouseholdValidationService householdValidationService;

    @Transactional(readOnly = true)
    public List<HouseholdMemberResponse> getHouseholdMembers(UUID householdId) {
        UUID currentUserId = currentUserService.getCurrentUserId();

        householdValidationService.validateHouseholdExists(householdId);

        boolean isMember = membershipRepository.existsByHouseholdIdAndUserIdAndStatus(
                householdId,
                currentUserId,
                "ACTIVE");

        if (!isMember) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Hogar no encontrado.");
        }

        var memberships = membershipRepository
                .findByHouseholdIdAndStatus(householdId, "ACTIVE");

        var userIds = memberships.stream()
                .map(HouseholdMembership::getUserId)
                .distinct()
                .toList();

        var users = userRepository.findAllByIdIn(userIds);

        var usersById = users.stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        return memberships.stream()
                .filter(membership -> usersById.containsKey(membership.getUserId()))
                .map(membership -> {
                    User user = usersById.get(membership.getUserId());

                    return new HouseholdMemberResponse(
                            user.getId(),
                            user.getDisplayName(),
                            user.getEmail(),
                            membership.getRole());
                })
                .toList();
    }

    @Transactional
    public void leaveHousehold(UUID householdId) {
        UUID userId = currentUserService.getCurrentUserId();

        householdValidationService.validateHouseholdExists(householdId);

        HouseholdMembership membership = membershipRepository
                .findByHouseholdIdAndUserIdAndStatus(householdId, userId, "ACTIVE")
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No perteneces a este hogar."));

        if (membership.getRole() == HouseholdRole.ADMIN) {
            List<HouseholdMembership> activeAdmins = membershipRepository
                    .findByHouseholdIdAndStatus(householdId, "ACTIVE")
                    .stream()
                    .filter(m -> m.getRole() == HouseholdRole.ADMIN)
                    .toList();

            if (activeAdmins.size() <= 1) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "No puedes abandonar el hogar porque eres su único administrador.");
            }
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneId.of("Europe/Madrid"));

        int removed = membershipRepository.removeMembership(
                householdId,
                userId,
                now);

        if (removed != 1) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No se pudo abandonar el hogar.");
        }

        List<TaskAssignment> activeAssignments = taskAssignmentRepository
                .findByHouseholdIdAndMembershipIdAndUnassignedAtIsNull(
                        householdId,
                        membership.getId());

        activeAssignments.forEach(TaskAssignment::unassign);

        taskAssignmentRepository.saveAll(activeAssignments);
    }

    @Transactional
    public void transferAdministration(UUID householdId, UUID newAdminUserId) {
        UUID currentUserId = currentUserService.getCurrentUserId();

        householdValidationService.validateHouseholdExists(householdId);

        boolean isAdmin = membershipRepository
                .existsByHouseholdIdAndUserIdAndRoleAndStatus(
                        householdId,
                        currentUserId,
                        HouseholdRole.ADMIN,
                        "ACTIVE");

        if (!isAdmin) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Solo un administrador puede transferir la administración.");
        }

        if (currentUserId.equals(newAdminUserId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "No puedes transferirte la administración a ti mismo.");
        }

        boolean isMember = membershipRepository
                .existsByHouseholdIdAndUserIdAndStatus(
                        householdId,
                        newAdminUserId,
                        "ACTIVE");

        if (!isMember) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "El usuario no pertenece a este hogar.");
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneId.of("Europe/Madrid"));

        int promoted = membershipRepository.updateMemberRole(
                householdId, newAdminUserId, HouseholdRole.ADMIN, now);

        if (promoted != 1) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No se pudo transferir la administración.");
        }

        int demoted = membershipRepository.updateMemberRole(
                householdId, currentUserId, HouseholdRole.MEMBER, now);

        if (demoted != 1) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No se pudo transferir la administración.");
        }
    }

}