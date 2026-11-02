package com.homesync.household.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Entity
@Table(name = "household_invitations")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HouseholdInvitation {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "household_id", nullable = false)
    private UUID householdId;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "consumed_at")
    private OffsetDateTime consumedAt;

    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;

    public HouseholdInvitation(
            UUID id,
            UUID householdId,
            UUID createdBy,
            String tokenHash,
            OffsetDateTime createdAt,
            OffsetDateTime expiresAt) {
        this.id = id;
        this.householdId = householdId;
        this.createdBy = createdBy;
        this.tokenHash = tokenHash;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

}