package com.jnzader.apigen.core.domain.event;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Evento que se dispara cuando una entidad es eliminada permanentemente (hard delete).
 * <p>
 * A diferencia de {@link EntityDeletedEvent}, este evento no contiene la entidad
 * ya que fue eliminada permanentemente de la base de datos. En su lugar, almacena
 * la información necesaria para identificar qué entidad fue eliminada.
 *
 * @param <I> El tipo del identificador de la entidad.
 */
public record EntityHardDeletedEvent<I extends Serializable>(
        I entityId,
        String entityType,
        String deletedBy,
        LocalDateTime occurredOn
) implements DomainEvent {

    /**
     * Constructor de conveniencia con fecha actual.
     *
     * @param entityId   El ID de la entidad eliminada.
     * @param entityType El nombre de la clase de la entidad.
     * @param deletedBy  El usuario que realizó la eliminación (puede ser null).
     */
    public EntityHardDeletedEvent(I entityId, String entityType, String deletedBy) {
        this(entityId, entityType, deletedBy, LocalDateTime.now());
    }

    /**
     * Constructor de conveniencia sin información de usuario.
     *
     * @param entityId   El ID de la entidad eliminada.
     * @param entityType El nombre de la clase de la entidad.
     */
    public EntityHardDeletedEvent(I entityId, String entityType) {
        this(entityId, entityType, null, LocalDateTime.now());
    }
}
