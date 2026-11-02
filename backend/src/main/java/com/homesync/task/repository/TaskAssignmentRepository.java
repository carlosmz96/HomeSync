package com.homesync.task.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.homesync.task.entity.TaskAssignment;

public interface TaskAssignmentRepository extends JpaRepository<TaskAssignment, UUID> {

    List<TaskAssignment> findByHouseholdIdAndTaskIdAndOccurrenceIdIsNullAndUnassignedAtIsNull(
            UUID householdId,
            UUID taskId);

    Optional<TaskAssignment> findByIdAndHouseholdIdAndTaskIdAndOccurrenceIdIsNullAndUnassignedAtIsNull(
            UUID id,
            UUID householdId,
            UUID taskId);

    boolean existsByTaskIdAndMembershipIdAndOccurrenceIdIsNullAndUnassignedAtIsNull(
            UUID taskId,
            UUID membershipId);

    List<TaskAssignment> findByHouseholdIdAndMembershipIdAndUnassignedAtIsNull(
            UUID householdId,
            UUID membershipId);

}