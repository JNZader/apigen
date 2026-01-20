package com.jnzader.apigen.core.infrastructure.eventsourcing;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Repository for event-sourced aggregates.
 *
 * <p>Provides methods to save and load aggregates using the event store. Handles event persistence,
 * optimistic concurrency, and optional snapshot support.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * @Service
 * public class ProductService {
 *
 *     private final AggregateRepository<Product> repository;
 *
 *     public ProductService(EventStore eventStore, EventSerializer serializer) {
 *         this.repository = new AggregateRepository<>(
 *             eventStore,
 *             serializer,
 *             "Product",
 *             Product::new
 *         );
 *     }
 *
 *     public void createProduct(String id, String name, BigDecimal price) {
 *         Product product = new Product();
 *         product.create(id, name, price);
 *         repository.save(product);
 *     }
 *
 *     public Product getProduct(String id) {
 *         return repository.findById(id)
 *             .orElseThrow(() -> new NotFoundException("Product not found"));
 *     }
 * }
 * }</pre>
 *
 * @param <T> the aggregate type
 */
public class AggregateRepository<T extends EventSourcedAggregate> {

    private final EventStore eventStore;
    private final EventSerializer serializer;
    private final String aggregateType;
    private final Supplier<T> aggregateFactory;
    private final int snapshotFrequency;

    /**
     * Creates a new aggregate repository without snapshot support.
     *
     * @param eventStore the event store
     * @param serializer the event serializer
     * @param aggregateType the aggregate type name
     * @param aggregateFactory factory to create new aggregate instances
     */
    public AggregateRepository(
            EventStore eventStore,
            EventSerializer serializer,
            String aggregateType,
            Supplier<T> aggregateFactory) {
        this(eventStore, serializer, aggregateType, aggregateFactory, 0);
    }

    /**
     * Creates a new aggregate repository with snapshot support.
     *
     * @param eventStore the event store
     * @param serializer the event serializer
     * @param aggregateType the aggregate type name
     * @param aggregateFactory factory to create new aggregate instances
     * @param snapshotFrequency how often to create snapshots (0 = disabled)
     */
    public AggregateRepository(
            EventStore eventStore,
            EventSerializer serializer,
            String aggregateType,
            Supplier<T> aggregateFactory,
            int snapshotFrequency) {
        this.eventStore = eventStore;
        this.serializer = serializer;
        this.aggregateType = aggregateType;
        this.aggregateFactory = aggregateFactory;
        this.snapshotFrequency = snapshotFrequency;
    }

    /**
     * Saves an aggregate by persisting its uncommitted events.
     *
     * @param aggregate the aggregate to save
     * @throws ConcurrencyException if there's a version conflict
     */
    public void save(T aggregate) {
        if (!aggregate.hasUncommittedEvents()) {
            return;
        }

        long expectedVersion = aggregate.getVersion() - aggregate.getUncommittedEventCount();

        eventStore.append(
                aggregate.getId(),
                aggregateType,
                aggregate.getUncommittedEvents(),
                expectedVersion);

        // Create snapshot if needed
        if (shouldCreateSnapshot(aggregate)) {
            createSnapshot(aggregate);
        }

        aggregate.markEventsAsCommitted();
    }

    /**
     * Finds an aggregate by ID.
     *
     * @param id the aggregate identifier
     * @return the aggregate if found
     */
    public Optional<T> findById(String id) {
        if (!eventStore.exists(id)) {
            return Optional.empty();
        }

        T aggregate = aggregateFactory.get();

        // Try to load from snapshot first
        if (snapshotFrequency > 0) {
            Optional<Snapshot> snapshot = eventStore.getLatestSnapshot(id);
            if (snapshot.isPresent()) {
                restoreFromSnapshot(aggregate, snapshot.get());
                // Load events after snapshot
                var events = eventStore.getEventsFrom(id, snapshot.get().getVersion());
                replayEvents(aggregate, events);
                return Optional.of(aggregate);
            }
        }

        // Load all events
        var events = eventStore.getEvents(id);
        replayEvents(aggregate, events);

        return Optional.of(aggregate);
    }

    /**
     * Checks if an aggregate exists.
     *
     * @param id the aggregate identifier
     * @return true if the aggregate exists
     */
    public boolean exists(String id) {
        return eventStore.exists(id);
    }

    /**
     * Gets the current version of an aggregate without loading it.
     *
     * @param id the aggregate identifier
     * @return the current version, or -1 if not found
     */
    public long getVersion(String id) {
        return eventStore.getCurrentVersion(id);
    }

    private void replayEvents(T aggregate, java.util.List<StoredEvent> storedEvents) {
        var events =
                storedEvents.stream()
                        .map(se -> serializer.deserialize(se.getPayload(), se.getEventType()))
                        .toList();
        aggregate.loadFromHistory(events);
    }

    private void restoreFromSnapshot(T aggregate, Snapshot snapshot) {
        Object state = serializer.deserializeState(snapshot.getState(), getSnapshotClass());
        aggregate.restoreFromSnapshot(state);
    }

    private boolean shouldCreateSnapshot(T aggregate) {
        return snapshotFrequency > 0
                && aggregate.getVersion() > 0
                && aggregate.getVersion() % snapshotFrequency == 0;
    }

    private void createSnapshot(T aggregate) {
        Object state = aggregate.createSnapshot();
        if (state != null) {
            Snapshot snapshot =
                    Snapshot.builder()
                            .aggregateId(aggregate.getId())
                            .aggregateType(aggregateType)
                            .version(aggregate.getVersion())
                            .state(serializer.serializeState(state))
                            .build();
            eventStore.saveSnapshot(snapshot);
        }
    }

    /**
     * Override this method to specify the snapshot state class for deserialization.
     *
     * @return the snapshot state class
     */
    @SuppressWarnings("unchecked")
    protected Class<?> getSnapshotClass() {
        return Object.class;
    }
}
