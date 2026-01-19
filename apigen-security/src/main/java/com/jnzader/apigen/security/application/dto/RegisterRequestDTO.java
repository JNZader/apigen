package com.jnzader.apigen.security.application.dto;

import com.jnzader.apigen.security.application.validation.StrongPassword;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** DTO para solicitud de registro de usuario. */
@Schema(description = "Datos para registrar un nuevo usuario")
public record RegisterRequestDTO(
        @Schema(
                        description =
                                "Nombre de usuario único (3-50 caracteres, solo alfanuméricos y"
                                        + " guión bajo)",
                        example = "john_doe",
                        minLength = 3,
                        maxLength = 50,
                        requiredMode = Schema.RequiredMode.REQUIRED)
                @NotBlank(message = "El nombre de usuario es requerido")
                @Size(
                        min = 3,
                        max = 50,
                        message = "El nombre de usuario debe tener entre 3 y 50 caracteres")
                @Pattern(
                        regexp = "^\\w+$",
                        message =
                                "El nombre de usuario solo puede contener letras, números y guiones"
                                        + " bajos")
                String username,
        @Schema(
                        description =
                                "Contraseña segura (mínimo 8 caracteres, mayúscula, minúscula,"
                                        + " número y especial)",
                        example = "SecurePass123!",
                        minLength = 8,
                        requiredMode = Schema.RequiredMode.REQUIRED)
                @NotBlank(message = "La contraseña es requerida")
                @StrongPassword
                String password,
        @Schema(
                        description = "Dirección de email válida",
                        example = "john.doe@example.com",
                        maxLength = 100,
                        requiredMode = Schema.RequiredMode.REQUIRED)
                @NotBlank(message = "El email es requerido")
                @Email(message = "El email debe ser válido")
                @Size(max = 100, message = "El email no puede exceder 100 caracteres")
                String email,
        @Schema(description = "Nombre del usuario", example = "John", maxLength = 100)
                @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
                String firstName,
        @Schema(description = "Apellido del usuario", example = "Doe", maxLength = 100)
                @Size(max = 100, message = "El apellido no puede exceder 100 caracteres")
                String lastName) {}
