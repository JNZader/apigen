package com.jnzader.apigen.core.domain.event;

import com.jnzader.apigen.core.domain.entity.Base;

import java.time.LocalDateTime;

/**
 * Evento que se dispara cuando una entidad es creada.
 *
 * @param <E> El tipo de la entidad.
 */
public record EntityCreatedEvent<E extends Base>(
        E entity,
        String createdBy,
        LocalDateTime occurredOn
) implements DomainEvent {

    /**
     * Constructor de conveniencia que establece la fecha actual.
     */
    public EntityCreatedEvent(E entity, String createdBy) {
        this(entity, createdBy, LocalDateTime.now());
    }

    /**
     * Constructor de conveniencia sin usuario.
     */
    public EntityCreatedEvent(E entity) {
        this(entity, null, LocalDateTime.now());
    }
}
