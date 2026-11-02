package com.homesync.household.service;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.homesync.household.entity.Household;
import com.homesync.household.entity.HouseholdMembership;
import com.homesync.household.entity.HouseholdRole;
import com.homesync.household.repository.HouseholdMembershipRepository;
import com.homesync.household.repository.HouseholdRepository;
import com.homesync.shared.security.CurrentUserService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HouseholdService {

    private final HouseholdRepository householdRepository;
    private final HouseholdMembershipRepository membershipRepository;

    private final CurrentUserService currentUserService;
    private final HouseholdValidationService householdValidationService;

    @Transactional
    public Household createHousehold(String name) {
        UUID userId = currentUserService.getCurrentUserId();
        UUID householdId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now(ZoneId.of("Europe/Madrid"));

        Household household = new Household(
                householdId,
                name.trim(),
                "Europe/Madrid",
                userId,
                now,
                now);

        householdRepository.save(household);

        HouseholdMembership membership = new HouseholdMembership(
                UUID.randomUUID(),
                householdId,
                userId,
                HouseholdRole.ADMIN,
                now,
                now,
                now);

        membershipRepository.save(membership);

        return household;
    }

    @Transactional(readOnly = true)
    public List<Household> getCurrentUserHouseholds() {
        UUID userId = currentUserService.getCurrentUserId();

        var memberships = membershipRepository.findByUserIdAndStatus(userId, "ACTIVE");

        var householdIds = memberships.stream()
                .map(HouseholdMembership::getHouseholdId)
                .toList();

        if (householdIds.isEmpty()) {
            return List.of();
        }

        return householdRepository.findByIdInAndDeletedAtIsNull(householdIds);
    }

    @Transactional(readOnly = true)
    public Household getHouseholdById(UUID householdId) {
        UUID userId = currentUserService.getCurrentUserId();

        boolean isMember = membershipRepository.existsByHouseholdIdAndUserIdAndStatus(
                householdId,
                userId,
                "ACTIVE");

        if (!isMember) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Hogar no encontrado.");
        }

        return householdValidationService.getActiveHousehold(householdId);
    }

    @Transactional
    public Household updateHouseholdName(UUID householdId, String name) {
        UUID userId = currentUserService.getCurrentUserId();

        boolean isAdmin = membershipRepository.existsByHouseholdIdAndUserIdAndRoleAndStatus(
                householdId,
                userId,
                HouseholdRole.ADMIN,
                "ACTIVE");

        if (!isAdmin) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND,
                    "Hogar no encontrado.");
        }

        Household household = householdValidationService.getActiveHousehold(householdId);

        household.updateName(name.trim());

        return householdRepository.save(household);
    }

}