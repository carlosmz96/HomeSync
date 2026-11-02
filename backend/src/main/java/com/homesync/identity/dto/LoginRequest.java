package com.homesync.identity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(

        @NotBlank(message = "El correo electrónico es obligatorio.")
        @Email(message = "El correo electrónico no tiene un formato válido.")
        @Size(max = 255, message = "El correo electrónico es demasiado largo.")
        String email,

        @NotBlank(message = "La contraseña es obligatoria.")
        @Size(max = 72, message = "La contraseña es demasiado larga.")
        String password

) {
}