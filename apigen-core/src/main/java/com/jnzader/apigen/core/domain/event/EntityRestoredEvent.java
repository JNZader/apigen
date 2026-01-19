package com.jnzader.apigen.core.domain.event;

import com.jnzader.apigen.core.domain.entity.Base;
import java.time.LocalDateTime;

/**
 * Evento que se dispara cuando una entidad eliminada es restaurada.
 *
 * @param <E> El tipo de la entidad.
 */
public record EntityRestoredEvent<E extends Base>(
        E entity, String restoredBy, LocalDateTime occurredOn) implements DomainEvent {

    /** Constructor de conveniencia que establece la fecha actual. */
    public EntityRestoredEvent(E entity, String restoredBy) {
        this(entity, restoredBy, LocalDateTime.now());
    }

    /** Constructor de conveniencia sin usuario. */
    public EntityRestoredEvent(E entity) {
        this(entity, null, LocalDateTime.now());
    }
}
