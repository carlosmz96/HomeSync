package com.homesync.task.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.homesync.task.entity.Task;

public interface TaskRepository extends JpaRepository<Task, UUID> {

    List<Task> findByHouseholdIdAndArchivedAtIsNullOrderByCreatedAtDesc(
            UUID householdId);

    Optional<Task> findByIdAndHouseholdIdAndArchivedAtIsNull(
            UUID id,
            UUID householdId);

    boolean existsByIdAndHouseholdIdAndArchivedAtIsNull(
            UUID id,
            UUID householdId);

}