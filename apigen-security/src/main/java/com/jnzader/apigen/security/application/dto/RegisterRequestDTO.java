package com.jnzader.apigen.security.application.dto;

import com.jnzader.apigen.security.application.validation.StrongPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO para solicitud de registro de usuario.
 */
public record RegisterRequestDTO(
        @NotBlank(message = "El nombre de usuario es requerido")
        @Size(min = 3, max = 50, message = "El nombre de usuario debe tener entre 3 y 50 caracteres")
        @Pattern(regexp = "^\\w+$", message = "El nombre de usuario solo puede contener letras, números y guiones bajos")
        String username,

        @NotBlank(message = "La contraseña es requerida")
        @StrongPassword
        String password,

        @NotBlank(message = "El email es requerido")
        @Email(message = "El email debe ser válido")
        @Size(max = 100, message = "El email no puede exceder 100 caracteres")
        String email,

        @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
        String firstName,

        @Size(max = 100, message = "El apellido no puede exceder 100 caracteres")
        String lastName
) {
}
