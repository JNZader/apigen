package com.jnzader.apigen.core.application.dto;

import com.jnzader.apigen.core.application.validation.ValidationGroups;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Positive;

/**
 * DTO base con validaciones comunes.
 * <p>
 * Esta interfaz define los campos base con anotaciones de validación.
 * Las implementaciones concretas deben usar records que implementen esta interfaz.
 * <p>
 * Ejemplo de implementación:
 * <pre>
 * {@code
 * public record ExampleDTO(
 *     @Null(groups = ValidationGroups.Create.class, message = "ID debe ser nulo al crear")
 *     @NotNull(groups = ValidationGroups.Update.class, message = "ID es requerido al actualizar")
 *     @Positive(message = "ID debe ser positivo")
 *     Long id,
 *
 *     Boolean activo,
 *
 *     @NotBlank(groups = {ValidationGroups.Create.class, ValidationGroups.Update.class})
 *     @Size(min = 2, max = 100)
 *     String nombre,
 *
 *     @Email
 *     String email
 * ) implements BaseDTOValidated {
 *     // Constructor compacto con validaciones adicionales si es necesario
 *     public ExampleDTO {
 *         if (nombre != null) {
 *             nombre = nombre.trim();
 *         }
 *     }
 * }
 * }
 * </pre>
 */
public interface BaseDTOValidated extends BaseDTO {

    /**
     * ID de la entidad.
     * - Debe ser nulo en creación (se auto-genera)
     * - Debe ser positivo si tiene valor
     */
    @Null(groups = ValidationGroups.Create.class, message = "ID debe ser nulo al crear una nueva entidad")
    @NotNull(groups = ValidationGroups.Update.class, message = "ID es requerido para actualizar")
    @Positive(message = "ID debe ser un número positivo")
    Long id();

    /**
     * Estado de la entidad.
     * - true: entidad activa
     * - false: entidad eliminada lógicamente (soft delete)
     */
    Boolean activo();
}
