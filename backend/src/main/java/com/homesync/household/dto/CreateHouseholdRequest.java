package com.homesync.household.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateHouseholdRequest(

        @NotBlank(message = "El nombre del hogar es obligatorio.")
        @Size(max = 120, message = "El nombre no puede superar los 120 caracteres.")
        String name

) {
}