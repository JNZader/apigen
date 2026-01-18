package com.jnzader.apigen.core.domain.event;

import com.jnzader.apigen.core.domain.entity.Base;

import java.time.LocalDateTime;

/**
 * Evento que se dispara cuando una entidad es actualizada.
 *
 * @param <E> El tipo de la entidad.
 */
public record EntityUpdatedEvent<E extends Base>(
        E entity,
        String updatedBy,
        LocalDateTime occurredOn
) implements DomainEvent {

    /**
     * Constructor de conveniencia que establece la fecha actual.
     */
    public EntityUpdatedEvent(E entity, String updatedBy) {
        this(entity, updatedBy, LocalDateTime.now());
    }

    /**
     * Constructor de conveniencia sin usuario.
     */
    public EntityUpdatedEvent(E entity) {
        this(entity, null, LocalDateTime.now());
    }
}
