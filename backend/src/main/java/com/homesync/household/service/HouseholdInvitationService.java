package com.homesync.household.service;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.homesync.household.dto.CreateInvitationResponse;
import com.homesync.household.entity.Household;
import com.homesync.household.entity.HouseholdInvitation;
import com.homesync.household.entity.HouseholdMembership;
import com.homesync.household.entity.HouseholdRole;
import com.homesync.household.exception.InvalidInvitationException;
import com.homesync.household.repository.HouseholdInvitationRepository;
import com.homesync.household.repository.HouseholdMembershipRepository;
import com.homesync.shared.security.CurrentUserService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HouseholdInvitationService {

    private final HouseholdInvitationRepository invitationRepository;
    private final HouseholdMembershipRepository membershipRepository;

    private final CurrentUserService currentUserService;
    private final InvitationTokenService invitationTokenService;
    private final HouseholdValidationService householdValidationService;

    @Transactional
    public CreateInvitationResponse createInvitation(UUID householdId) {
        UUID userId = currentUserService.getCurrentUserId();

        boolean isAdmin = membershipRepository
                .existsByHouseholdIdAndUserIdAndRoleAndStatus(
                        householdId,
                        userId,
                        HouseholdRole.ADMIN,
                        "ACTIVE");

        if (!isAdmin) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Hogar no encontrado.");
        }

        Household household = householdValidationService.getActiveHousehold(householdId);

        String token = invitationTokenService.generateToken();
        String tokenHash = invitationTokenService.hashToken(token);

        OffsetDateTime now = OffsetDateTime.now(
                java.time.ZoneId.of("Europe/Madrid"));

        OffsetDateTime expiresAt = now.plus(7, ChronoUnit.DAYS);

        HouseholdInvitation invitation = new HouseholdInvitation(
                UUID.randomUUID(),
                household.getId(),
                userId,
                tokenHash,
                now,
                expiresAt);

        invitationRepository.save(invitation);

        return new CreateInvitationResponse(
                invitation.getId(),
                household.getId(),
                token,
                expiresAt);
    }

    @Transactional
    public Household acceptInvitation(String token) {
        UUID userId = currentUserService.getCurrentUserId();
        String tokenHash = invitationTokenService.hashToken(token);

        HouseholdInvitation invitation = invitationRepository
                .findByTokenHashAndConsumedAtIsNullAndRevokedAtIsNull(tokenHash)
                .orElseThrow(InvalidInvitationException::new);

        OffsetDateTime now = OffsetDateTime.now(ZoneId.of("Europe/Madrid"));

        if (!invitation.getExpiresAt().isAfter(now)) {
            throw new InvalidInvitationException();
        }

        boolean alreadyMember = membershipRepository
                .existsByHouseholdIdAndUserIdAndStatus(
                        invitation.getHouseholdId(),
                        userId,
                        "ACTIVE");

        if (alreadyMember) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Ya perteneces a este hogar.");
        }

        Household household;

        try {
            household = householdValidationService.getActiveHousehold(
                    invitation.getHouseholdId());
        } catch (ResponseStatusException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new InvalidInvitationException();
            }

            throw ex;
        }

        int consumed = invitationRepository.consumeInvitation(
                invitation.getId(),
                now);

        if (consumed != 1) {
            throw new InvalidInvitationException();
        }

        HouseholdMembership membership = new HouseholdMembership(
                UUID.randomUUID(),
                household.getId(),
                userId,
                HouseholdRole.MEMBER,
                now,
                now,
                now);

        membershipRepository.save(membership);

        return household;
    }

    @Transactional
    public void revokeInvitation(UUID householdId, UUID invitationId) {
        UUID userId = currentUserService.getCurrentUserId();

        householdValidationService.validateHouseholdExists(householdId);

        boolean isAdmin = membershipRepository
                .existsByHouseholdIdAndUserIdAndRoleAndStatus(
                        householdId,
                        userId,
                        HouseholdRole.ADMIN,
                        "ACTIVE");

        if (!isAdmin) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Solo un administrador puede revocar invitaciones.");
        }

        OffsetDateTime now = OffsetDateTime.now(
                ZoneId.of("Europe/Madrid"));

        int revoked = invitationRepository.revokeInvitation(
                invitationId,
                householdId,
                now);

        if (revoked != 1) {
            throw new InvalidInvitationException();
        }
    }

}