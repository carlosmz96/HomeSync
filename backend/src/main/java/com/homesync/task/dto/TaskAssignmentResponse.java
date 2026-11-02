package com.homesync.task.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.homesync.task.entity.TaskAssignment;

public record TaskAssignmentResponse(
        UUID id,
        UUID householdId,
        UUID taskId,
        UUID membershipId,
        OffsetDateTime assignedAt) {

    public static TaskAssignmentResponse from(TaskAssignment assignment) {
        return new TaskAssignmentResponse(
                assignment.getId(),
                assignment.getHouseholdId(),
                assignment.getTaskId(),
                assignment.getMembershipId(),
                assignment.getAssignedAt());
    }

}