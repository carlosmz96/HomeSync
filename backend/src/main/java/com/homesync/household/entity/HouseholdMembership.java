package com.homesync.household.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Entity
@Table(name = "household_memberships")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HouseholdMembership {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "household_id", nullable = false)
    private UUID householdId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 10)
    private HouseholdRole role;

    @Column(name = "status", nullable = false, length = 10)
    private String status;

    @Column(name = "joined_at", nullable = false)
    private OffsetDateTime joinedAt;

    @Column(name = "removed_at")
    private OffsetDateTime removedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public HouseholdMembership(
            UUID id,
            UUID householdId,
            UUID userId,
            HouseholdRole role,
            OffsetDateTime joinedAt,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt) {
        this.id = id;
        this.householdId = householdId;
        this.userId = userId;
        this.role = role;
        this.status = "ACTIVE";
        this.joinedAt = joinedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void changeRole(HouseholdRole newRole) {
        this.role = newRole;
        this.updatedAt = OffsetDateTime.now();
    }

}