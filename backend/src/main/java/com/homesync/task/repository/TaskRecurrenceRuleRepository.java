package com.homesync.task.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import com.homesync.task.entity.TaskRecurrenceRule;

public interface TaskRecurrenceRuleRepository
        extends JpaRepository<TaskRecurrenceRule, UUID> {

    Optional<TaskRecurrenceRule> findByHouseholdIdAndTaskId(
            UUID householdId,
            UUID taskId);

    boolean existsByHouseholdIdAndTaskIdAndActiveTrue(
            UUID householdId,
            UUID taskId);

    List<TaskRecurrenceRule> findByActiveTrueAndNextOccurrenceAtLessThanEqual(
            OffsetDateTime limit);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT r
            FROM TaskRecurrenceRule r
            WHERE r.id = :ruleId
              AND r.householdId = :householdId
              AND r.taskId = :taskId
            """)
    Optional<TaskRecurrenceRule> findByIdForUpdate(
            @Param("ruleId") UUID ruleId,
            @Param("householdId") UUID householdId,
            @Param("taskId") UUID taskId);

}