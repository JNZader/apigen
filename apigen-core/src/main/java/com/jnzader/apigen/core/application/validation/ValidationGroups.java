package com.jnzader.apigen.core.application.validation;

import jakarta.validation.groups.Default;

/**
 * Grupos de validación para controlar qué validaciones se aplican en cada operación.
 *
 * <p>Uso en DTOs:
 *
 * <pre>{@code
 * public record UserDTO(
 *     @Null(groups = Create.class, message = "ID debe ser nulo al crear")
 *     @NotNull(groups = Update.class, message = "ID es requerido al actualizar")
 *     Long id,
 *
 *     @NotBlank(groups = {Create.class, Update.class})
 *     String username,
 *
 *     @NotBlank(groups = Create.class, message = "Password requerido al crear")
 *     String password
 * ) {}
 * }</pre>
 *
 * <p>Uso en controladores:
 *
 * <pre>{@code
 * @PostMapping
 * public ResponseEntity<?> create(@Validated(Create.class) @RequestBody UserDTO dto) { ... }
 *
 * @PutMapping("/{id}")
 * public ResponseEntity<?> update(@PathVariable Long id,
 *                                  @Validated(Update.class) @RequestBody UserDTO dto) { ... }
 * }</pre>
 */
public final class ValidationGroups {

    private ValidationGroups() {
        // Utility class
    }

    /**
     * Grupo de validación para operaciones de creación (POST).
     *
     * <p>Validaciones típicas: - ID debe ser nulo - Password requerido - Campos obligatorios
     * iniciales
     */
    public interface Create extends Default {}

    /**
     * Grupo de validación para operaciones de actualización (PUT).
     *
     * <p>Validaciones típicas: - ID requerido - Password opcional (solo si se quiere cambiar) -
     * Campos actualizables
     */
    public interface Update extends Default {}

    /**
     * Grupo de validación para operaciones de actualización parcial (PATCH).
     *
     * <p>Validaciones más relajadas que Update: - Todos los campos opcionales - Solo valida campos
     * presentes
     */
    public interface PartialUpdate {}

    /**
     * Grupo de validación para operaciones de eliminación.
     *
     * <p>Validaciones típicas: - Verificar que no tenga dependencias - Verificar permisos de
     * eliminación
     */
    public interface Delete {}

    /**
     * Grupo de validación para búsquedas/filtros.
     *
     * <p>Validaciones típicas: - Rangos de fechas válidos - Parámetros de paginación válidos
     */
    public interface Search {}

    /**
     * Grupo de validación para operaciones de importación/bulk.
     *
     * <p>Validaciones más estrictas para operaciones masivas.
     */
    public interface Import extends Create {}
}
