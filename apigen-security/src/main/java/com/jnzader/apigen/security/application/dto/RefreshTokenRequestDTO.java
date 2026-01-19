package com.jnzader.apigen.security.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/** DTO para solicitud de refresh token. */
@Schema(description = "Solicitud para renovar el access token usando un refresh token")
public record RefreshTokenRequestDTO(
        @Schema(
                description = "Refresh token obtenido en el login",
                example = "dGhpcyBpcyBhIHJlZnJlc2ggdG9rZW4gZXhhbXBsZQ==",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "El refresh token es requerido")
        String refreshToken) {}
