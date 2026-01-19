package com.jnzader.apigen.core.infrastructure.eventsourcing;

import java.time.Instant;
import java.util.UUID;

/**
 * Base interface for all domain events in an event-sourced system.
 *
 * <p>Domain events represent something that happened in the domain. They are immutable facts that
 * describe state changes.
 *
 * <p>Example:
 *
 * <pre>{@code
 * public record ProductCreatedEvent(
 *     String aggregateId,
 *     String name,
 *     BigDecimal price,
 *     Instant occurredAt
 * ) implements DomainEvent {
 *
 *     @Override
 *     public String getEventType() {
 *         return "ProductCreated";
 *     }
 * }
 * }</pre>
 */
public interface DomainEvent {

    /**
     * Returns the unique identifier of the aggregate this event belongs to.
     *
     * @return the aggregate ID
     */
    String getAggregateId();

    /**
     * Returns the type of this event. Used for deserialization and event routing.
     *
     * @return the event type name
     */
    String getEventType();

    /**
     * Returns the timestamp when this event occurred.
     *
     * @return the occurrence timestamp
     */
    Instant getOccurredAt();

    /**
     * Returns the version of this event in the aggregate's event stream. This is typically set by
     * the event store when the event is persisted.
     *
     * @return the event version, or -1 if not yet persisted
     */
    default long getVersion() {
        return -1;
    }

    /**
     * Returns a unique identifier for this specific event instance.
     *
     * @return the event ID
     */
    default String getEventId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Returns optional metadata associated with this event.
     *
     * @return metadata map, or null if no metadata
     */
    default java.util.Map<String, String> getMetadata() {
        return null;
    }
}
