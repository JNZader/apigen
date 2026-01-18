package com.jnzader.apigen.core.infrastructure.controller;

import com.jnzader.apigen.core.application.dto.BaseDTO;
import com.jnzader.apigen.core.application.dto.pagination.CursorPageRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

/**
 * Interfaz base para controladores REST genéricos.
 * Define los endpoints estándar RESTful para operaciones CRUD sobre DTOs.
 * <p>
 * Endpoints:
 * - GET    /                    - Listar con paginación y filtrado
 * - HEAD   /                    - Obtener conteo (X-Total-Count header)
 * - GET    /{id}                - Obtener por ID (soporta ETag)
 * - HEAD   /{id}                - Verificar existencia
 * - POST   /                    - Crear (retorna Location header)
 * - PUT    /{id}                - Actualizar completo (valida ID, soporta If-Match)
 * - PATCH  /{id}                - Actualizar parcial
 * - DELETE /{id}                - Eliminar (soft delete por defecto)
 * - DELETE /{id}?permanent=true - Eliminar permanente
 *
 * @param <D> El tipo del DTO que extiende {@link BaseDTO}.
 * @param <I> El tipo del identificador de la entidad.
 */
@SuppressWarnings("java:S1452") // Wildcards intencionales para API REST genérica
public interface BaseController<D extends BaseDTO, I extends Serializable> {

    /**
     * Lista recursos con paginación, filtrado dinámico y selección de campos.
     * <p>
     * Ejemplos de uso:
     * <pre>
     * GET /?page=0&size=20&sort=id,desc
     * GET /?filter=nombre:like:Juan,estado:eq:true
     * GET /?filter=fechaCreacion:between:2024-01-01;2024-12-31&fields=id,nombre
     * </pre>
     *
     * @param filter   Filtros dinámicos en formato: campo:operador:valor
     * @param filters  Filtros simples via query params (campo=valor)
     * @param fields   Campos a incluir en la respuesta (sparse fieldsets)
     * @param pageable Configuración de paginación
     */
    ResponseEntity<?> findAll(
            @RequestParam(required = false) String filter,
            @RequestParam(required = false) Map<String, String> filters,
            @RequestParam(required = false) Set<String> fields,
            Pageable pageable
    );

    /**
     * Obtiene el conteo de recursos.
     * HEAD / → X-Total-Count header
     */
    ResponseEntity<Void> count(
            @RequestParam(required = false) Map<String, String> filters
    );

    /**
     * Obtiene un recurso por ID con soporte para ETag.
     * GET /{id}
     *
     * @param id          ID del recurso
     * @param fields      Campos a incluir (sparse fieldsets)
     * @param ifNoneMatch ETag para cache condicional (304 Not Modified)
     */
    ResponseEntity<?> findById(
            @PathVariable I id,
            @RequestParam(required = false) Set<String> fields,
            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch
    );

    /**
     * Verifica existencia de un recurso.
     * HEAD /{id} → 200 si existe, 404 si no
     */
    ResponseEntity<Void> existsById(@PathVariable I id);

    /**
     * Crea un nuevo recurso.
     * POST / → 201 Created + Location header
     */
    ResponseEntity<?> save(@Valid @RequestBody D dto);

    /**
     * Actualiza completamente un recurso con validación de ID y concurrencia optimista.
     * PUT /{id}
     *
     * @param id      ID del recurso (debe coincidir con DTO)
     * @param dto     Datos completos del recurso
     * @param ifMatch ETag para concurrencia optimista (412 si no coincide)
     */
    ResponseEntity<?> update(
            @PathVariable I id,
            @Valid @RequestBody D dto,
            @RequestHeader(value = "If-Match", required = false) String ifMatch
    );

    /**
     * Actualiza parcialmente un recurso.
     * PATCH /{id}
     */
    ResponseEntity<?> partialUpdate(
            @PathVariable I id,
            @RequestBody D dto,
            @RequestHeader(value = "If-Match", required = false) String ifMatch
    );

    /**
     * Elimina un recurso (soft delete por defecto, hard delete con ?permanent=true).
     * DELETE /{id}
     * DELETE /{id}?permanent=true
     */
    ResponseEntity<Void> delete(
            @PathVariable I id,
            @RequestParam(required = false, defaultValue = "false") boolean permanent
    );

    /**
     * Restaura un recurso eliminado con soft delete.
     * POST /{id}/restore
     */
    ResponseEntity<?> restore(@PathVariable I id);

    /**
     * Lista recursos usando paginación basada en cursor.
     * <p>
     * La paginación por cursor es más eficiente que offset para datasets grandes:
     * - Performance constante O(1) vs O(n) de offset
     * - Sin duplicados/omisiones al insertar/eliminar datos
     * - Ideal para scroll infinito
     * <p>
     * GET /cursor?size=20&sort=id&direction=DESC
     * GET /cursor?cursor=eyJpZCI6MTAwfQ==&size=20
     *
     * @param cursor    Cursor de la página anterior (null para primera página)
     * @param size      Tamaño de página (default 20, max 100)
     * @param sort      Campo de ordenamiento (default "id")
     * @param direction Dirección de ordenamiento (ASC o DESC, default DESC)
     * @param filter    Filtros dinámicos en formato campo:operador:valor
     */
    ResponseEntity<?> findAllWithCursor(
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false, defaultValue = "20") int size,
            @RequestParam(required = false, defaultValue = "id") String sort,
            @RequestParam(required = false, defaultValue = "DESC") CursorPageRequest.SortDirection direction,
            @RequestParam(required = false) String filter
    );
}
