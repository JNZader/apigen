package com.jnzader.apigen.core.infrastructure.eventsourcing;

import java.util.List;
import java.util.Optional;

/**
 * Interface for storing and retrieving domain events.
 *
 * <p>The event store is the primary persistence mechanism for event-sourced aggregates. It provides
 * append-only storage of events and supports optimistic concurrency control.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Append events to an aggregate
 * eventStore.append("product-123", "Product", events, expectedVersion);
 *
 * // Load all events for an aggregate
 * List<StoredEvent> events = eventStore.getEvents("product-123");
 *
 * // Load events from a specific version
 * List<StoredEvent> newEvents = eventStore.getEventsFrom("product-123", 5);
 * }</pre>
 */
public interface EventStore {

    /**
     * Appends events to the event stream for an aggregate.
     *
     * @param aggregateId the aggregate identifier
     * @param aggregateType the type of aggregate
     * @param events the events to append
     * @param expectedVersion the expected current version for optimistic concurrency (-1 for new
     *     aggregates)
     * @throws ConcurrencyException if the expected version doesn't match the current version
     */
    void append(
            String aggregateId,
            String aggregateType,
            List<DomainEvent> events,
            long expectedVersion);

    /**
     * Retrieves all events for an aggregate.
     *
     * @param aggregateId the aggregate identifier
     * @return list of stored events in version order
     */
    List<StoredEvent> getEvents(String aggregateId);

    /**
     * Retrieves events for an aggregate starting from a specific version.
     *
     * @param aggregateId the aggregate identifier
     * @param fromVersion the version to start from (exclusive)
     * @return list of stored events after the specified version
     */
    List<StoredEvent> getEventsFrom(String aggregateId, long fromVersion);

    /**
     * Retrieves events for an aggregate within a version range.
     *
     * @param aggregateId the aggregate identifier
     * @param fromVersion the version to start from (exclusive)
     * @param toVersion the version to end at (inclusive)
     * @return list of stored events in the specified range
     */
    List<StoredEvent> getEventsBetween(String aggregateId, long fromVersion, long toVersion);

    /**
     * Gets the current version of an aggregate's event stream.
     *
     * @param aggregateId the aggregate identifier
     * @return the current version, or -1 if no events exist
     */
    long getCurrentVersion(String aggregateId);

    /**
     * Checks if an aggregate exists in the event store.
     *
     * @param aggregateId the aggregate identifier
     * @return true if at least one event exists for the aggregate
     */
    boolean exists(String aggregateId);

    /**
     * Retrieves events by type across all aggregates.
     *
     * @param eventType the event type to filter by
     * @param limit maximum number of events to return
     * @return list of stored events of the specified type
     */
    List<StoredEvent> getEventsByType(String eventType, int limit);

    /**
     * Retrieves all events across all aggregates, ordered by stored timestamp.
     *
     * @param fromPosition the global position to start from
     * @param limit maximum number of events to return
     * @return list of stored events
     */
    List<StoredEvent> getAllEvents(long fromPosition, int limit);

    /**
     * Saves a snapshot of an aggregate's state.
     *
     * @param snapshot the snapshot to save
     */
    void saveSnapshot(Snapshot snapshot);

    /**
     * Retrieves the latest snapshot for an aggregate.
     *
     * @param aggregateId the aggregate identifier
     * @return the latest snapshot, or empty if none exists
     */
    Optional<Snapshot> getLatestSnapshot(String aggregateId);
}
