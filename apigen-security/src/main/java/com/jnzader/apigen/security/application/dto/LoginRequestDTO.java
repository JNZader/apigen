package com.jnzader.apigen.security.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/** DTO para solicitud de login. */
@Schema(description = "Credenciales para iniciar sesión")
public record LoginRequestDTO(
        @Schema(description = "Nombre de usuario", example = "admin", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "El nombre de usuario es requerido")
        String username,

        @Schema(description = "Contraseña del usuario", example = "Admin123!", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "La contraseña es requerida")
        String password) {}
