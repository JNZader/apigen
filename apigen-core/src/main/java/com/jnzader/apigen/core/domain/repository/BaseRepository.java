package com.jnzader.apigen.core.domain.repository;

import com.jnzader.apigen.core.domain.entity.Base;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Interfaz base para repositorios JPA genéricos.
 * Proporciona métodos CRUD para entidades que extienden {@link Base}.
 * <p>
 * Características:
 * - CRUD básico heredado de JpaRepository
 * - Soporte para especificaciones JPA (queries dinámicas)
 * - Métodos adicionales para soft delete
 * - Queries optimizadas para operaciones comunes
 * <p>
 * La anotación {@code @NoRepositoryBean} asegura que Spring Data JPA
 * no cree una implementación para esta interfaz directamente, ya que
 * está destinada a ser extendida por otros repositorios.
 *
 * @param <E> El tipo de la entidad que extiende {@link Base}.
 * @param <I> El tipo del identificador de la entidad, que debe ser {@link Serializable}.
 */
@NoRepositoryBean
public interface BaseRepository<E extends Base, I extends Serializable>
        extends JpaRepository<E, I>, JpaSpecificationExecutor<E> {

    /**
     * Encuentra todas las entidades activas.
     */
    List<E> findByEstadoTrue();

    /**
     * Encuentra todas las entidades inactivas/eliminadas.
     */
    List<E> findByEstadoFalse();

    /**
     * Cuenta las entidades activas.
     */
    long countByEstadoTrue();

    /**
     * Cuenta las entidades inactivas.
     */
    long countByEstadoFalse();

    /**
     * Soft delete: marca la entidad como inactiva.
     */
    @Modifying
    @Query("UPDATE #{#entityName} e SET e.estado = false, e.fechaEliminacion = :fecha, e.eliminadoPor = :usuario WHERE e.id = :id")
    int softDeleteById(@Param("id") I id, @Param("fecha") LocalDateTime fecha, @Param("usuario") String usuario);

    /**
     * Soft delete múltiple: marca múltiples entidades como inactivas.
     */
    @Modifying
    @Query("UPDATE #{#entityName} e SET e.estado = false, e.fechaEliminacion = :fecha, e.eliminadoPor = :usuario WHERE e.id IN :ids")
    int softDeleteAllByIds(@Param("ids") List<I> ids, @Param("fecha") LocalDateTime fecha, @Param("usuario") String usuario);

    /**
     * Restaura una entidad eliminada lógicamente.
     * Usa UPDATE directo que no se ve afectado por @SQLRestriction.
     */
    @Modifying
    @Query("UPDATE #{#entityName} e SET e.estado = true, e.fechaEliminacion = null, e.eliminadoPor = null WHERE e.id = :id")
    int restoreById(@Param("id") I id);

    /**
     * Restaura múltiples entidades eliminadas lógicamente.
     */
    @Modifying
    @Query("UPDATE #{#entityName} e SET e.estado = true, e.fechaEliminacion = null, e.eliminadoPor = null WHERE e.id IN :ids")
    int restoreAllByIds(@Param("ids") List<I> ids);

    /**
     * Verifica si existe una entidad con el ID dado, incluyendo entidades soft deleted.
     * Esta consulta NO se ve afectada por @SQLRestriction porque usa JPQL con subquery.
     */
    @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM #{#entityName} e WHERE e.id = :id")
    boolean existsByIdIncludingDeleted(@Param("id") I id);

    /**
     * Elimina permanentemente una entidad por ID (hard delete).
     * Esta operación ignora @SQLRestriction al usar DELETE directo.
     */
    @Modifying
    @Query("DELETE FROM #{#entityName} e WHERE e.id = :id")
    int hardDeleteById(@Param("id") I id);

    /**
     * Busca entidades creadas en un rango de fechas.
     */
    List<E> findByFechaCreacionBetween(LocalDateTime start, LocalDateTime end);

    /**
     * Busca entidades actualizadas después de una fecha.
     */
    List<E> findByFechaActualizacionAfter(LocalDateTime date);
}
