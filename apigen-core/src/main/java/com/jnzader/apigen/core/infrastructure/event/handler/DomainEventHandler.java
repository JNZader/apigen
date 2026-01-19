package com.jnzader.apigen.core.infrastructure.event.handler;

import com.jnzader.apigen.core.domain.entity.Base;
import com.jnzader.apigen.core.domain.event.EntityCreatedEvent;
import com.jnzader.apigen.core.domain.event.EntityDeletedEvent;
import com.jnzader.apigen.core.domain.event.EntityRestoredEvent;
import com.jnzader.apigen.core.domain.event.EntityUpdatedEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Handler genérico para eventos de dominio.
 *
 * <p>Características: - Escucha eventos después de que la transacción se complete (AFTER_COMMIT) -
 * Procesa eventos de forma asíncrona para no bloquear la transacción - Registra métricas de eventos
 * para monitoreo - Logging estructurado de eventos
 *
 * <p>Las subclases pueden sobrescribir los métodos handle* para añadir comportamiento específico
 * (notificaciones, integraciones, etc.)
 */
@Component
public class DomainEventHandler {

    private static final Logger log = LoggerFactory.getLogger(DomainEventHandler.class);

    private final Counter entityCreatedCounter;
    private final Counter entityUpdatedCounter;
    private final Counter entityDeletedCounter;
    private final Counter entityRestoredCounter;

    public DomainEventHandler(MeterRegistry meterRegistry) {
        this.entityCreatedCounter =
                Counter.builder("domain.events.created")
                        .description("Number of entity created events")
                        .register(meterRegistry);
        this.entityUpdatedCounter =
                Counter.builder("domain.events.updated")
                        .description("Number of entity updated events")
                        .register(meterRegistry);
        this.entityDeletedCounter =
                Counter.builder("domain.events.deleted")
                        .description("Number of entity deleted events")
                        .register(meterRegistry);
        this.entityRestoredCounter =
                Counter.builder("domain.events.restored")
                        .description("Number of entity restored events")
                        .register(meterRegistry);
    }

    /**
     * Maneja eventos de creación de entidades. Se ejecuta después de que la transacción se complete
     * exitosamente.
     */
    @Async("domainEventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public <E extends Base> void handleEntityCreated(EntityCreatedEvent<E> event) {
        entityCreatedCounter.increment();

        E entity = event.entity();
        String entityType = entity.getClass().getSimpleName();
        Object entityId = entity.getId();

        log.info(
                "Entity created: type={}, id={}, createdBy={}, occurredOn={}",
                entityType,
                entityId,
                event.createdBy(),
                event.occurredOn());

        // Hook para subclases
        onEntityCreated(event);
    }

    /** Maneja eventos de actualización de entidades. */
    @Async("domainEventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public <E extends Base> void handleEntityUpdated(EntityUpdatedEvent<E> event) {
        entityUpdatedCounter.increment();

        E entity = event.entity();
        String entityType = entity.getClass().getSimpleName();
        Object entityId = entity.getId();

        log.info(
                "Entity updated: type={}, id={}, updatedBy={}, occurredOn={}",
                entityType,
                entityId,
                event.updatedBy(),
                event.occurredOn());

        // Hook para subclases
        onEntityUpdated(event);
    }

    /** Maneja eventos de eliminación de entidades. */
    @Async("domainEventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public <E extends Base> void handleEntityDeleted(EntityDeletedEvent<E> event) {
        entityDeletedCounter.increment();

        E entity = event.entity();
        String entityType = entity.getClass().getSimpleName();
        Object entityId = entity.getId();
        String deleteType = event.softDelete() ? "soft" : "hard";

        log.info(
                "Entity deleted: type={}, id={}, deletedBy={}, deleteType={}, occurredOn={}",
                entityType,
                entityId,
                event.deletedBy(),
                deleteType,
                event.occurredOn());

        // Hook para subclases
        onEntityDeleted(event);
    }

    /** Maneja eventos de restauración de entidades. */
    @Async("domainEventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public <E extends Base> void handleEntityRestored(EntityRestoredEvent<E> event) {
        entityRestoredCounter.increment();

        E entity = event.entity();
        String entityType = entity.getClass().getSimpleName();
        Object entityId = entity.getId();

        log.info(
                "Entity restored: type={}, id={}, occurredOn={}",
                entityType,
                entityId,
                event.occurredOn());

        // Hook para subclases
        onEntityRestored(event);
    }

    // ==================== Hooks para extensibilidad ====================

    /**
     * Hook llamado después de crear una entidad. Las subclases pueden sobrescribir para añadir
     * comportamiento.
     */
    protected <E extends Base> void onEntityCreated(EntityCreatedEvent<E> event) {
        // Las subclases pueden sobrescribir para notificaciones, integraciones, etc.
    }

    /** Hook llamado después de actualizar una entidad. */
    protected <E extends Base> void onEntityUpdated(EntityUpdatedEvent<E> event) {
        // Las subclases pueden sobrescribir
    }

    /** Hook llamado después de eliminar una entidad. */
    protected <E extends Base> void onEntityDeleted(EntityDeletedEvent<E> event) {
        // Las subclases pueden sobrescribir
    }

    /** Hook llamado después de restaurar una entidad. */
    protected <E extends Base> void onEntityRestored(EntityRestoredEvent<E> event) {
        // Las subclases pueden sobrescribir
    }
}
