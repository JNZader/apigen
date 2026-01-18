package com.jnzader.apigen.security.application.dto;

import java.time.Instant;
import java.util.Set;

/**
 * DTO de respuesta de autenticación.
 */
public record AuthResponseDTO(
        String accessToken,
        String refreshToken,
        String tokenType,
        Instant expiresAt,
        UserInfoDTO user
) {
    public AuthResponseDTO(String accessToken, String refreshToken, Instant expiresAt, UserInfoDTO user) {
        this(accessToken, refreshToken, "Bearer", expiresAt, user);
    }

    /**
     * Información básica del usuario autenticado.
     */
    public record UserInfoDTO(
            Long id,
            String username,
            String email,
            String fullName,
            String role,
            Set<String> permissions
    ) {
    }
}
