package com.jnzader.apigen.core.domain.event;

import com.jnzader.apigen.core.domain.entity.Base;

import java.time.LocalDateTime;

/**
 * Evento que se dispara cuando una entidad es eliminada (soft o hard delete).
 *
 * @param <E> El tipo de la entidad.
 */
public record EntityDeletedEvent<E extends Base>(
        E entity,
        String deletedBy,
        boolean softDelete,
        LocalDateTime occurredOn
) implements DomainEvent {

    /**
     * Constructor de conveniencia para soft delete con fecha actual.
     */
    public EntityDeletedEvent(E entity, String deletedBy, boolean softDelete) {
        this(entity, deletedBy, softDelete, LocalDateTime.now());
    }

    /**
     * Constructor de conveniencia para soft delete.
     */
    public EntityDeletedEvent(E entity, String deletedBy) {
        this(entity, deletedBy, true, LocalDateTime.now());
    }

    /**
     * Constructor de conveniencia mínimo.
     */
    public EntityDeletedEvent(E entity) {
        this(entity, null, true, LocalDateTime.now());
    }
}
