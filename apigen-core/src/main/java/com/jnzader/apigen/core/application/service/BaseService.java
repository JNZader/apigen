package com.jnzader.apigen.core.application.service;

import com.jnzader.apigen.core.application.dto.pagination.CursorPageRequest;
import com.jnzader.apigen.core.application.dto.pagination.CursorPageResponse;
import com.jnzader.apigen.core.application.util.Result;
import com.jnzader.apigen.core.domain.entity.Base;
import java.io.Serializable;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

/**
 * Interfaz base para servicios genéricos. Define las operaciones CRUD estándar para entidades que
 * extienden {@link Base}. Utiliza el patrón Result para un manejo de errores explícito y funcional.
 *
 * <p>Características: - Operaciones CRUD básicas - Soporte para paginación - Soft delete y
 * restauración - Operaciones en batch - Búsqueda con especificaciones JPA - Caché integrado
 * (opcional en implementaciones)
 *
 * @param <E> El tipo de la entidad que extiende {@link Base}.
 * @param <I> El tipo del identificador de la entidad, que debe ser {@link Serializable}.
 */
public interface BaseService<E extends Base, I extends Serializable> {

    // ==================== Consultas básicas ====================

    /**
     * Obtiene todas las entidades.
     *
     * @return Result con la lista de entidades o error.
     */
    Result<List<E>, Exception> findAll();

    /**
     * Obtiene todas las entidades activas (no eliminadas).
     *
     * @return Result con la lista de entidades activas o error.
     */
    Result<List<E>, Exception> findAllActive();

    /**
     * Obtiene todas las entidades con paginación.
     *
     * @param pageable Configuración de paginación.
     * @return Result con la página de entidades o error.
     */
    Result<Page<E>, Exception> findAll(Pageable pageable);

    /**
     * Obtiene todas las entidades activas con paginación.
     *
     * @param pageable Configuración de paginación.
     * @return Result con la página de entidades activas o error.
     */
    Result<Page<E>, Exception> findAllActive(Pageable pageable);

    /**
     * Busca una entidad por su ID.
     *
     * @param id El identificador de la entidad.
     * @return Result con la entidad encontrada o error si no existe.
     */
    Result<E, Exception> findById(I id);

    /**
     * Verifica si existe una entidad con el ID dado.
     *
     * @param id El identificador a verificar.
     * @return Result con true si existe, false si no.
     */
    Result<Boolean, Exception> existsById(I id);

    /**
     * Cuenta el total de entidades.
     *
     * @return Result con el conteo o error.
     */
    Result<Long, Exception> count();

    /**
     * Cuenta el total de entidades activas.
     *
     * @return Result con el conteo de entidades activas o error.
     */
    Result<Long, Exception> countActive();

    // ==================== Búsqueda con especificaciones ====================

    /**
     * Busca entidades usando una especificación JPA.
     *
     * @param spec La especificación de búsqueda.
     * @return Result con la lista de entidades que cumplen la especificación.
     */
    Result<List<E>, Exception> findAll(Specification<E> spec);

    /**
     * Busca entidades usando una especificación JPA con paginación.
     *
     * @param spec La especificación de búsqueda.
     * @param pageable Configuración de paginación.
     * @return Result con la página de entidades que cumplen la especificación.
     */
    Result<Page<E>, Exception> findAll(Specification<E> spec, Pageable pageable);

    /**
     * Busca una única entidad usando una especificación JPA.
     *
     * @param spec La especificación de búsqueda.
     * @return Result con la entidad encontrada o error si no existe o hay múltiples.
     */
    Result<E, Exception> findOne(Specification<E> spec);

    // ==================== Operaciones de escritura ====================

    /**
     * Guarda una nueva entidad.
     *
     * @param entity La entidad a guardar.
     * @return Result con la entidad guardada (con ID asignado) o error.
     */
    Result<E, Exception> save(E entity);

    /**
     * Actualiza una entidad existente.
     *
     * @param id El ID de la entidad a actualizar.
     * @param entity La entidad con los nuevos valores.
     * @return Result con la entidad actualizada o error si no existe.
     */
    Result<E, Exception> update(I id, E entity);

    /**
     * Actualiza parcialmente una entidad (solo campos no nulos).
     *
     * @param id El ID de la entidad a actualizar.
     * @param entity La entidad con los campos a actualizar.
     * @return Result con la entidad actualizada o error si no existe.
     */
    Result<E, Exception> partialUpdate(I id, E entity);

    // ==================== Soft Delete ====================

    /**
     * Elimina lógicamente una entidad (soft delete). Marca la entidad como inactiva pero no la
     * borra de la base de datos.
     *
     * @param id El ID de la entidad a eliminar.
     * @return Result con void si éxito o error.
     */
    Result<Void, Exception> softDelete(I id);

    /**
     * Elimina lógicamente una entidad con información del usuario.
     *
     * @param id El ID de la entidad a eliminar.
     * @param usuario El usuario que realiza la eliminación.
     * @return Result con void si éxito o error.
     */
    Result<Void, Exception> softDelete(I id, String usuario);

    /**
     * Restaura una entidad eliminada lógicamente.
     *
     * @param id El ID de la entidad a restaurar.
     * @return Result con la entidad restaurada o error.
     */
    Result<E, Exception> restore(I id);

    // ==================== Hard Delete ====================

    /**
     * Elimina permanentemente una entidad (hard delete). ¡CUIDADO! Esta operación es irreversible.
     *
     * @param id El ID de la entidad a eliminar permanentemente.
     * @return Result con void si éxito o error.
     */
    Result<Void, Exception> hardDelete(I id);

    // ==================== Operaciones en Batch ====================

    /**
     * Guarda múltiples entidades en una sola transacción.
     *
     * @param entities Las entidades a guardar.
     * @return Result con la lista de entidades guardadas o error.
     */
    Result<List<E>, Exception> saveAll(List<E> entities);

    /**
     * Elimina lógicamente múltiples entidades.
     *
     * @param ids Los IDs de las entidades a eliminar.
     * @return Result con el número de entidades eliminadas o error.
     */
    Result<Integer, Exception> softDeleteAll(List<I> ids);

    /**
     * Elimina lógicamente múltiples entidades con información del usuario.
     *
     * @param ids Los IDs de las entidades a eliminar.
     * @param usuario El usuario que realiza la eliminación.
     * @return Result con el número de entidades eliminadas o error.
     */
    Result<Integer, Exception> softDeleteAll(List<I> ids, String usuario);

    /**
     * Restaura múltiples entidades eliminadas.
     *
     * @param ids Los IDs de las entidades a restaurar.
     * @return Result con el número de entidades restauradas o error.
     */
    Result<Integer, Exception> restoreAll(List<I> ids);

    /**
     * Elimina permanentemente múltiples entidades. ¡CUIDADO! Esta operación es irreversible.
     *
     * @param ids Los IDs de las entidades a eliminar permanentemente.
     * @return Result con el número de entidades eliminadas o error.
     */
    Result<Integer, Exception> hardDeleteAll(List<I> ids);

    // ==================== Cursor-based Pagination ====================

    /**
     * Obtiene entidades usando paginación basada en cursor.
     *
     * <p>La paginación por cursor es más eficiente que offset para datasets grandes: - Performance
     * constante O(1) vs O(n) de offset - Sin duplicados/omisiones al insertar/eliminar datos -
     * Mejor experiencia en scroll infinito
     *
     * @param request Configuración del cursor (cursor, size, sortField, sortDirection)
     * @return Result con la respuesta paginada incluyendo cursores next/prev
     */
    Result<CursorPageResponse<E>, Exception> findAllWithCursor(CursorPageRequest request);

    /**
     * Obtiene entidades usando paginación basada en cursor con especificación.
     *
     * @param spec Especificación de filtrado
     * @param request Configuración del cursor
     * @return Result con la respuesta paginada
     */
    Result<CursorPageResponse<E>, Exception> findAllWithCursor(
            Specification<E> spec, CursorPageRequest request);
}
