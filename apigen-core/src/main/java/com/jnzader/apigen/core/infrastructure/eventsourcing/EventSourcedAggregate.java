package com.jnzader.apigen.core.infrastructure.eventsourcing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Base class for event-sourced aggregates.
 *
 * <p>Event-sourced aggregates derive their state from a sequence of domain events. Instead of
 * storing current state directly, all state changes are captured as events that can be replayed to
 * reconstruct the aggregate.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * public class Product extends EventSourcedAggregate {
 *     private String name;
 *     private BigDecimal price;
 *     private boolean active;
 *
 *     // Command method - validates and raises events
 *     public void create(String id, String name, BigDecimal price) {
 *         if (name == null || name.isBlank()) {
 *             throw new IllegalArgumentException("Name is required");
 *         }
 *         raiseEvent(new ProductCreatedEvent(id, name, price, Instant.now()));
 *     }
 *
 *     // Event handler - applies state changes
 *     @Override
 *     protected void apply(DomainEvent event) {
 *         if (event instanceof ProductCreatedEvent e) {
 *             this.id = e.aggregateId();
 *             this.name = e.name();
 *             this.price = e.price();
 *             this.active = true;
 *         }
 *     }
 * }
 * }</pre>
 *
 * <p>Key concepts:
 *
 * <ul>
 *   <li><strong>Commands</strong>: Methods that validate business rules and raise events
 *   <li><strong>Events</strong>: Immutable facts representing what happened
 *   <li><strong>Event handlers</strong>: Methods that update state based on events
 * </ul>
 */
public abstract class EventSourcedAggregate {

    private String id;
    private long version = -1;
    private final List<DomainEvent> uncommittedEvents = new ArrayList<>();

    protected EventSourcedAggregate() {}

    protected EventSourcedAggregate(String id) {
        this.id = id;
    }

    /**
     * Returns the aggregate identifier.
     *
     * @return the aggregate ID
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the aggregate identifier. Should only be called during initialization.
     *
     * @param id the aggregate ID
     */
    protected void setId(String id) {
        this.id = id;
    }

    /**
     * Returns the current version of the aggregate.
     *
     * <p>The version is incremented with each event applied. It's used for optimistic concurrency
     * control.
     *
     * @return the current version (-1 if no events have been applied)
     */
    public long getVersion() {
        return version;
    }

    /**
     * Returns the list of uncommitted events.
     *
     * <p>These are events that have been raised but not yet persisted to the event store.
     *
     * @return unmodifiable list of uncommitted events
     */
    public List<DomainEvent> getUncommittedEvents() {
        return Collections.unmodifiableList(uncommittedEvents);
    }

    /**
     * Clears the uncommitted events after they have been persisted.
     *
     * <p>Call this after successfully saving events to the event store.
     */
    public void markEventsAsCommitted() {
        uncommittedEvents.clear();
    }

    /**
     * Raises a new domain event.
     *
     * <p>This method:
     *
     * <ol>
     *   <li>Applies the event to update aggregate state
     *   <li>Adds the event to the uncommitted events list
     *   <li>Increments the version
     * </ol>
     *
     * @param event the domain event to raise
     */
    protected void raiseEvent(DomainEvent event) {
        applyEvent(event, true);
    }

    /**
     * Applies an event to update aggregate state.
     *
     * <p>Subclasses must implement this method to handle all event types.
     *
     * @param event the event to apply
     */
    protected abstract void apply(DomainEvent event);

    /**
     * Loads the aggregate from a list of historical events.
     *
     * <p>This method replays events to reconstruct the aggregate state.
     *
     * @param events the historical events
     */
    public void loadFromHistory(List<? extends DomainEvent> events) {
        for (DomainEvent event : events) {
            applyEvent(event, false);
        }
    }

    /**
     * Loads the aggregate from a snapshot and subsequent events.
     *
     * @param snapshotVersion the version of the snapshot
     * @param events events that occurred after the snapshot
     */
    public void loadFromSnapshot(long snapshotVersion, List<? extends DomainEvent> events) {
        this.version = snapshotVersion;
        for (DomainEvent event : events) {
            applyEvent(event, false);
        }
    }

    private void applyEvent(DomainEvent event, boolean isNew) {
        apply(event);
        version++;

        if (isNew) {
            uncommittedEvents.add(event);
        }
    }

    /**
     * Creates a snapshot of the current aggregate state.
     *
     * <p>Subclasses should override this to provide snapshot support.
     *
     * @return the snapshot state, or null if snapshots are not supported
     */
    public Object createSnapshot() {
        return null;
    }

    /**
     * Restores the aggregate from a snapshot.
     *
     * <p>Subclasses should override this to restore state from a snapshot.
     *
     * @param snapshot the snapshot state
     */
    public void restoreFromSnapshot(Object snapshot) {
        // Override in subclass if snapshot support is needed
    }

    /**
     * Checks if the aggregate has uncommitted events.
     *
     * @return true if there are uncommitted events
     */
    public boolean hasUncommittedEvents() {
        return !uncommittedEvents.isEmpty();
    }

    /**
     * Returns the number of uncommitted events.
     *
     * @return count of uncommitted events
     */
    public int getUncommittedEventCount() {
        return uncommittedEvents.size();
    }
}
