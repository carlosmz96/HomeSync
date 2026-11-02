package com.homesync.household.repository;

import com.homesync.household.entity.HouseholdInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface HouseholdInvitationRepository
        extends JpaRepository<HouseholdInvitation, UUID> {

    Optional<HouseholdInvitation> findByTokenHash(String tokenHash);

    Optional<HouseholdInvitation> findByTokenHashAndConsumedAtIsNullAndRevokedAtIsNull(
            String tokenHash);

    @Modifying
    @Query("""
            UPDATE HouseholdInvitation i
            SET i.consumedAt = :now
            WHERE i.id = :id
              AND i.consumedAt IS NULL
              AND i.revokedAt IS NULL
              AND i.expiresAt > :now
            """)
    int consumeInvitation(
            @Param("id") UUID id,
            @Param("now") OffsetDateTime now);

    @Modifying
    @Query("""
            UPDATE HouseholdInvitation i
            SET i.revokedAt = :now
            WHERE i.id = :id
              AND i.householdId = :householdId
              AND i.consumedAt IS NULL
              AND i.revokedAt IS NULL
              AND i.expiresAt > :now
            """)
    int revokeInvitation(
            @Param("id") UUID id,
            @Param("householdId") UUID householdId,
            @Param("now") OffsetDateTime now);

}