package com.homesync.household.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AcceptInvitationRequest(

        @NotBlank(message = "El token de invitación es obligatorio.")
        @Size(max = 200, message = "El token de invitación no es válido.")
        String token

) {
}