package com.homesync.task.dto;

import java.time.OffsetDateTime;

import com.homesync.task.entity.TaskPriority;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTaskRequest(

        @NotBlank(message = "El título es obligatorio.")
        @Size(max = 200, message = "El título no puede superar los 200 caracteres.")
        String title,

        @Size(max = 5000, message = "La descripción no puede superar los 5000 caracteres.")
        String description,

        TaskPriority priority,

        OffsetDateTime dueAt

) {
}