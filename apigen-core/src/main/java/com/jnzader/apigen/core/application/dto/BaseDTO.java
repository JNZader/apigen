package com.jnzader.apigen.core.application.dto;

/**
 * Interfaz base para Data Transfer Objects (DTOs). Define los atributos comunes que todos los DTOs
 * de la aplicación deben tener, como el identificador y el estado de activación.
 */
public interface BaseDTO {
    Long id();

    Boolean activo();
}
