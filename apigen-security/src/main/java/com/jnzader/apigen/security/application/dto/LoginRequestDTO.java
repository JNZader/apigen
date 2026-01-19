package com.jnzader.apigen.security.application.dto;

import jakarta.validation.constraints.NotBlank;

/** DTO para solicitud de login. */
public record LoginRequestDTO(
        @NotBlank(message = "El nombre de usuario es requerido") String username,
        @NotBlank(message = "La contraseña es requerida") String password) {}
