package com.homesync.identity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "El correo electrónico es obligatorio.")
        @Email(message = "El correo electrónico no tiene un formato válido.")
        @Size(max = 255, message = "El correo electrónico es demasiado largo.")
        String email,
        @NotBlank(message = "La contraseña es obligatoria.")
        @Size(min = 12, max = 72, message = "La contraseña debe tener entre 12 y 72 caracteres.")
        String password,
        @NotBlank(message = "El nombre es obligatorio.")
        @Size(max = 120, message = "El nombre no puede superar los 120 caracteres.")
        String displayName) {
}