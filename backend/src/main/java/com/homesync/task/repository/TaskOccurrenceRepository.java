package com.homesync.task.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.homesync.task.entity.TaskOccurrence;

public interface TaskOccurrenceRepository
        extends JpaRepository<TaskOccurrence, UUID> {

    List<TaskOccurrence> findByHouseholdIdAndTaskIdOrderByStartsAtAsc(
            UUID householdId,
            UUID taskId);

    Optional<TaskOccurrence> findByIdAndHouseholdIdAndTaskId(
            UUID id,
            UUID householdId,
            UUID taskId);

    boolean existsByTaskIdAndDueAt(
            UUID taskId,
            OffsetDateTime dueAt);

}