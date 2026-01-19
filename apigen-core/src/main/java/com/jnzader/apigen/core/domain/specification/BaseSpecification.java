package com.jnzader.apigen.core.domain.specification;

import com.jnzader.apigen.core.domain.entity.Base;
import java.time.LocalDateTime;
import java.util.Collection;
import org.springframework.data.jpa.domain.Specification;

/**
 * Clase utilitaria con especificaciones JPA comunes para queries dinámicas. Utiliza el patrón
 * Specification de Spring Data JPA para construir consultas de forma type-safe y componible.
 */
public final class BaseSpecification {

    // Constantes para nombres de campos
    private static final String FIELD_ESTADO = "estado";
    private static final String FIELD_ID = "id";
    private static final String FIELD_FECHA_CREACION = "fechaCreacion";
    private static final String FIELD_FECHA_ACTUALIZACION = "fechaActualizacion";
    private static final String FIELD_FECHA_ELIMINACION = "fechaEliminacion";
    private static final String FIELD_ELIMINADO_POR = "eliminadoPor";
    private static final String FIELD_CREADO_POR = "creadoPor";
    private static final String FIELD_MODIFICADO_POR = "modificadoPor";

    private BaseSpecification() {
        // Utility class
    }

    // ==================== Estado ====================

    /** Filtra entidades activas (estado = true). */
    public static <E extends Base> Specification<E> isActive() {
        return (root, query, cb) -> cb.isTrue(root.get(FIELD_ESTADO));
    }

    /** Filtra entidades inactivas/eliminadas (estado = false). */
    public static <E extends Base> Specification<E> isInactive() {
        return (root, query, cb) -> cb.isFalse(root.get(FIELD_ESTADO));
    }

    /** Filtra por estado específico. */
    public static <E extends Base> Specification<E> hasEstado(Boolean estado) {
        return (root, query, cb) ->
                estado != null ? cb.equal(root.get(FIELD_ESTADO), estado) : cb.conjunction();
    }

    // ==================== ID ====================

    /** Filtra por ID específico. */
    public static <E extends Base> Specification<E> hasId(Long id) {
        return (root, query, cb) ->
                id != null ? cb.equal(root.get(FIELD_ID), id) : cb.conjunction();
    }

    /** Filtra por IDs en una lista. */
    public static <E extends Base> Specification<E> hasIdIn(Collection<Long> ids) {
        return (root, query, cb) ->
                ids != null && !ids.isEmpty() ? root.get(FIELD_ID).in(ids) : cb.conjunction();
    }

    /** Filtra por IDs que NO están en una lista. */
    public static <E extends Base> Specification<E> hasIdNotIn(Collection<Long> ids) {
        return (root, query, cb) ->
                ids != null && !ids.isEmpty()
                        ? cb.not(root.get(FIELD_ID).in(ids))
                        : cb.conjunction();
    }

    // ==================== Fechas ====================

    /** Filtra entidades creadas después de una fecha. */
    public static <E extends Base> Specification<E> createdAfter(LocalDateTime date) {
        return (root, query, cb) ->
                date != null
                        ? cb.greaterThan(root.get(FIELD_FECHA_CREACION), date)
                        : cb.conjunction();
    }

    /** Filtra entidades creadas antes de una fecha. */
    public static <E extends Base> Specification<E> createdBefore(LocalDateTime date) {
        return (root, query, cb) ->
                date != null ? cb.lessThan(root.get(FIELD_FECHA_CREACION), date) : cb.conjunction();
    }

    /** Filtra entidades creadas entre dos fechas (inclusivo). */
    public static <E extends Base> Specification<E> createdBetween(
            LocalDateTime start, LocalDateTime end) {
        return (root, query, cb) -> {
            if (start != null && end != null) {
                return cb.between(root.get(FIELD_FECHA_CREACION), start, end);
            } else if (start != null) {
                return cb.greaterThanOrEqualTo(root.get(FIELD_FECHA_CREACION), start);
            } else if (end != null) {
                return cb.lessThanOrEqualTo(root.get(FIELD_FECHA_CREACION), end);
            }
            return cb.conjunction();
        };
    }

    /** Filtra entidades actualizadas después de una fecha. */
    public static <E extends Base> Specification<E> updatedAfter(LocalDateTime date) {
        return (root, query, cb) ->
                date != null
                        ? cb.greaterThan(root.get(FIELD_FECHA_ACTUALIZACION), date)
                        : cb.conjunction();
    }

    /** Filtra entidades actualizadas antes de una fecha. */
    public static <E extends Base> Specification<E> updatedBefore(LocalDateTime date) {
        return (root, query, cb) ->
                date != null
                        ? cb.lessThan(root.get(FIELD_FECHA_ACTUALIZACION), date)
                        : cb.conjunction();
    }

    /** Filtra entidades actualizadas entre dos fechas. */
    public static <E extends Base> Specification<E> updatedBetween(
            LocalDateTime start, LocalDateTime end) {
        return (root, query, cb) -> {
            if (start != null && end != null) {
                return cb.between(root.get(FIELD_FECHA_ACTUALIZACION), start, end);
            } else if (start != null) {
                return cb.greaterThanOrEqualTo(root.get(FIELD_FECHA_ACTUALIZACION), start);
            } else if (end != null) {
                return cb.lessThanOrEqualTo(root.get(FIELD_FECHA_ACTUALIZACION), end);
            }
            return cb.conjunction();
        };
    }

    // ==================== Soft Delete ====================

    /** Filtra entidades que NO han sido eliminadas (soft delete). */
    public static <E extends Base> Specification<E> notDeleted() {
        return (root, query, cb) -> cb.isNull(root.get(FIELD_FECHA_ELIMINACION));
    }

    /** Filtra entidades que HAN sido eliminadas (soft delete). */
    public static <E extends Base> Specification<E> isDeleted() {
        return (root, query, cb) -> cb.isNotNull(root.get(FIELD_FECHA_ELIMINACION));
    }

    /** Filtra entidades eliminadas por un usuario específico. */
    public static <E extends Base> Specification<E> deletedBy(String usuario) {
        return (root, query, cb) ->
                usuario != null
                        ? cb.equal(root.get(FIELD_ELIMINADO_POR), usuario)
                        : cb.conjunction();
    }

    // ==================== Búsqueda genérica ====================

    /** Busca por un campo de texto específico (case-insensitive, contains). */
    public static <E extends Base> Specification<E> fieldContains(String fieldName, String value) {
        return (root, query, cb) -> {
            if (value == null || value.isBlank()) {
                return cb.conjunction();
            }
            return cb.like(
                    cb.lower(root.get(fieldName).as(String.class)),
                    "%" + value.toLowerCase() + "%");
        };
    }

    /** Busca por un campo con valor exacto. */
    public static <E extends Base> Specification<E> fieldEquals(String fieldName, Object value) {
        return (root, query, cb) ->
                value != null ? cb.equal(root.get(fieldName), value) : cb.conjunction();
    }

    /** Busca por un campo que no sea null. */
    public static <E extends Base> Specification<E> fieldIsNotNull(String fieldName) {
        return (root, query, cb) -> cb.isNotNull(root.get(fieldName));
    }

    /** Busca por un campo que sea null. */
    public static <E extends Base> Specification<E> fieldIsNull(String fieldName) {
        return (root, query, cb) -> cb.isNull(root.get(fieldName));
    }

    // ==================== Auditoría ====================

    /** Filtra entidades creadas por un usuario específico. */
    public static <E extends Base> Specification<E> createdBy(String usuario) {
        return (root, query, cb) ->
                usuario != null ? cb.equal(root.get(FIELD_CREADO_POR), usuario) : cb.conjunction();
    }

    /** Filtra entidades modificadas por un usuario específico. */
    public static <E extends Base> Specification<E> modifiedBy(String usuario) {
        return (root, query, cb) ->
                usuario != null
                        ? cb.equal(root.get(FIELD_MODIFICADO_POR), usuario)
                        : cb.conjunction();
    }

    // ==================== Combinadores ====================

    /** Combina múltiples especificaciones con AND. */
    @SafeVarargs
    public static <E extends Base> Specification<E> allOf(Specification<E>... specs) {
        Specification<E> result = (root, query, cb) -> cb.conjunction();
        for (Specification<E> spec : specs) {
            if (spec != null) {
                result = result.and(spec);
            }
        }
        return result;
    }

    /** Combina múltiples especificaciones con OR. */
    @SafeVarargs
    public static <E extends Base> Specification<E> anyOf(Specification<E>... specs) {
        Specification<E> result = null;
        for (Specification<E> spec : specs) {
            if (spec != null) {
                result = (result == null) ? spec : result.or(spec);
            }
        }
        return result != null ? result : (root, query, cb) -> cb.disjunction();
    }

    /** Niega una especificación. */
    public static <E extends Base> Specification<E> not(Specification<E> spec) {
        return Specification.not(spec);
    }
}
