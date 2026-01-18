package com.jnzader.apigen.security.application.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO para solicitud de refresh token.
 */
public record RefreshTokenRequestDTO(
        @NotBlank(message = "El refresh token es requerido")
        String refreshToken
) {
}
