package com.homesync.task.dto;

import com.homesync.task.entity.TaskStatus;

import jakarta.validation.constraints.NotNull;

public record UpdateTaskStatusRequest(

        @NotNull(message = "El estado es obligatorio.")
        TaskStatus status

) {
}