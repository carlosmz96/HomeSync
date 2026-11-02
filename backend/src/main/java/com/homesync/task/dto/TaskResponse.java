package com.homesync.task.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.homesync.task.entity.Task;
import com.homesync.task.entity.TaskPriority;
import com.homesync.task.entity.TaskStatus;

public record TaskResponse(
        UUID id,
        UUID householdId,
        UUID createdBy,
        String title,
        String description,
        TaskPriority priority,
        TaskStatus status,
        OffsetDateTime dueAt,
        OffsetDateTime completedAt,
        boolean recurring,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {

    public static TaskResponse from(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getHouseholdId(),
                task.getCreatedBy(),
                task.getTitle(),
                task.getDescription(),
                task.getPriority(),
                task.getStatus(),
                task.getDueAt(),
                task.getCompletedAt(),
                task.isRecurring(),
                task.getCreatedAt(),
                task.getUpdatedAt());
    }

}