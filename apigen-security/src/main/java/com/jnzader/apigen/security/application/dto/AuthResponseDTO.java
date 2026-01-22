package com.jnzader.apigen.security.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.Set;

/** DTO de respuesta de autenticación. */
@Schema(description = "Respuesta de autenticación con tokens JWT")
public record AuthResponseDTO(
        @Schema(
                        description = "Token de acceso JWT",
                        example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyIn0.EXAMPLE_SIGNATURE")
                String accessToken,
        @Schema(
                        description = "Token de refresco para obtener nuevos access tokens",
                        example = "dGhpcyBpcyBhIHJlZnJlc2ggdG9rZW4gZXhhbXBsZQ==")
                String refreshToken,
        @Schema(description = "Tipo de token", example = "Bearer", defaultValue = "Bearer")
                String tokenType,
        @Schema(
                        description = "Fecha y hora de expiración del access token",
                        example = "2024-01-23T12:00:00Z")
                Instant expiresAt,
        @Schema(description = "Información del usuario autenticado") UserInfoDTO user) {

    public AuthResponseDTO(
            String accessToken, String refreshToken, Instant expiresAt, UserInfoDTO user) {
        this(accessToken, refreshToken, "Bearer", expiresAt, user);
    }

    /** Información básica del usuario autenticado. */
    @Schema(description = "Información del usuario autenticado")
    public record UserInfoDTO(
            @Schema(description = "ID único del usuario", example = "1") Long id,
            @Schema(description = "Nombre de usuario", example = "admin") String username,
            @Schema(description = "Email del usuario", example = "admin@example.com") String email,
            @Schema(description = "Nombre completo", example = "Admin User") String fullName,
            @Schema(description = "Rol principal del usuario", example = "ROLE_ADMIN") String role,
            @Schema(
                            description = "Permisos del usuario",
                            example = "[\"READ\", \"WRITE\", \"DELETE\"]")
                    Set<String> permissions) {}
}
