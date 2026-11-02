package com.homesync.task.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record AssignTaskRequest(

        @NotNull(message = "La membresía del responsable es obligatoria.")
        UUID membershipId

) {
}