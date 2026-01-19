package com.jnzader.apigen.core.domain.event;

import java.time.LocalDateTime;

/**
 * Interfaz base para todos los eventos de dominio. Los eventos de dominio representan hechos que
 * han ocurrido en el dominio del negocio.
 */
public interface DomainEvent {

    /**
     * Retorna el momento en que ocurrió el evento.
     *
     * @return La fecha y hora del evento.
     */
    LocalDateTime occurredOn();

    /**
     * Retorna el tipo de evento como string.
     *
     * @return El nombre del tipo de evento.
     */
    default String eventType() {
        return this.getClass().getSimpleName();
    }
}
