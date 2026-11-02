package com.homesync.household.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.homesync.household.entity.HouseholdMembership;
import com.homesync.household.entity.HouseholdRole;

public interface HouseholdMembershipRepository extends JpaRepository<HouseholdMembership, UUID> {

    List<HouseholdMembership> findByUserIdAndStatus(
            UUID userId,
            String status);

    boolean existsByHouseholdIdAndUserIdAndStatus(
            UUID householdId,
            UUID userId,
            String status);

    boolean existsByHouseholdIdAndUserIdAndRoleAndStatus(
            UUID householdId,
            UUID userId,
            HouseholdRole role,
            String status);

    List<HouseholdMembership> findByHouseholdIdAndStatus(
            UUID householdId,
            String status);

    @Modifying
    @Query("""
            UPDATE HouseholdMembership m
            SET m.status = 'REMOVED',
                m.removedAt = :now,
                m.updatedAt = :now
            WHERE m.householdId = :householdId
              AND m.userId = :userId
              AND m.status = 'ACTIVE'
            """)
    int removeMembership(
            @Param("householdId") UUID householdId,
            @Param("userId") UUID userId,
            @Param("now") OffsetDateTime now);

    Optional<HouseholdMembership> findByHouseholdIdAndUserIdAndStatus(
            UUID householdId,
            UUID userId,
            String status);

    @Modifying
    @Query("""
            UPDATE HouseholdMembership m
            SET m.role = :role,
                m.updatedAt = :now
            WHERE m.householdId = :householdId
              AND m.userId = :userId
              AND m.status = 'ACTIVE'
            """)
    int updateMemberRole(
            @Param("householdId") UUID householdId,
            @Param("userId") UUID userId,
            @Param("role") HouseholdRole role,
            @Param("now") OffsetDateTime now);

}