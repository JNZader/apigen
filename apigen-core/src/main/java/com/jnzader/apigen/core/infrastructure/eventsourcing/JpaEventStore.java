package com.jnzader.apigen.core.infrastructure.eventsourcing;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

/**
 * JPA-based implementation of the event store.
 *
 * <p>This implementation uses JPA repositories to persist events and snapshots. It provides
 * optimistic concurrency control and optionally publishes events to Spring's event system.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * @Autowired
 * private EventStore eventStore;
 *
 * // Append events
 * eventStore.append("product-123", "Product", events, expectedVersion);
 *
 * // Load events and reconstruct aggregate
 * List<StoredEvent> events = eventStore.getEvents("product-123");
 * Product product = new Product();
 * events.forEach(e -> product.apply(deserialize(e)));
 * }</pre>
 */
public class JpaEventStore implements EventStore {

    private static final Logger log = LoggerFactory.getLogger(JpaEventStore.class);

    private final StoredEventRepository eventRepository;
    private final SnapshotRepository snapshotRepository;
    private final EventSerializer serializer;
    private final ApplicationEventPublisher eventPublisher;

    public JpaEventStore(
            StoredEventRepository eventRepository,
            SnapshotRepository snapshotRepository,
            EventSerializer serializer,
            ApplicationEventPublisher eventPublisher) {
        this.eventRepository = eventRepository;
        this.snapshotRepository = snapshotRepository;
        this.serializer = serializer;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public void append(
            String aggregateId,
            String aggregateType,
            List<DomainEvent> events,
            long expectedVersion) {
        if (events == null || events.isEmpty()) {
            return;
        }

        // Check for concurrency conflicts
        long currentVersion = getCurrentVersion(aggregateId);
        if (expectedVersion != -1 && currentVersion != expectedVersion) {
            throw new ConcurrencyException(aggregateId, expectedVersion, currentVersion);
        }

        long version = currentVersion;
        for (DomainEvent event : events) {
            version++;

            StoredEvent storedEvent =
                    StoredEvent.builder()
                            .eventId(
                                    event.getEventId() != null
                                            ? event.getEventId()
                                            : UUID.randomUUID().toString())
                            .aggregateId(aggregateId)
                            .aggregateType(aggregateType)
                            .eventType(event.getEventType())
                            .version(version)
                            .payload(serializer.serialize(event))
                            .metadata(serializer.serializeMetadata(event.getMetadata()))
                            .occurredAt(event.getOccurredAt())
                            .build();

            eventRepository.save(storedEvent);

            log.debug(
                    "Stored event {} v{} for aggregate {}",
                    event.getEventType(),
                    version,
                    aggregateId);

            // Publish to Spring event system
            if (eventPublisher != null) {
                eventPublisher.publishEvent(new StoredEventWrapper(storedEvent, event));
            }
        }

        log.info(
                "Appended {} events to aggregate {} (v{} -> v{})",
                events.size(),
                aggregateId,
                expectedVersion,
                version);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StoredEvent> getEvents(String aggregateId) {
        return eventRepository.findByAggregateIdOrderByVersionAsc(aggregateId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StoredEvent> getEventsFrom(String aggregateId, long fromVersion) {
        return eventRepository.findByAggregateIdAndVersionGreaterThanOrderByVersionAsc(
                aggregateId, fromVersion);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StoredEvent> getEventsBetween(
            String aggregateId, long fromVersion, long toVersion) {
        return eventRepository
                .findByAggregateIdAndVersionGreaterThanAndVersionLessThanEqualOrderByVersionAsc(
                        aggregateId, fromVersion, toVersion);
    }

    @Override
    @Transactional(readOnly = true)
    public long getCurrentVersion(String aggregateId) {
        return eventRepository.findMaxVersionByAggregateId(aggregateId).orElse(-1L);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean exists(String aggregateId) {
        return eventRepository.existsByAggregateId(aggregateId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StoredEvent> getEventsByType(String eventType, int limit) {
        List<StoredEvent> events = eventRepository.findByEventTypeOrderByStoredAtDesc(eventType);
        if (events.size() > limit) {
            return events.subList(0, limit);
        }
        return events;
    }

    @Override
    @Transactional(readOnly = true)
    public List<StoredEvent> getAllEvents(long fromPosition, int limit) {
        // For simplicity, use findAll with pagination
        // In production, you might want a more efficient approach with a global sequence
        return eventRepository.findAll().stream().skip(fromPosition).limit(limit).toList();
    }

    @Override
    @Transactional
    public void saveSnapshot(Snapshot snapshot) {
        snapshotRepository.save(snapshot);
        log.debug(
                "Saved snapshot for aggregate {} at version {}",
                snapshot.getAggregateId(),
                snapshot.getVersion());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Snapshot> getLatestSnapshot(String aggregateId) {
        return snapshotRepository.findFirstByAggregateIdOrderByVersionDesc(aggregateId);
    }

    /**
     * Wrapper class for publishing stored events to Spring's event system.
     *
     * <p>Contains both the stored event (for persistence info) and the original domain event (for
     * type-safe handling).
     */
    public record StoredEventWrapper(StoredEvent storedEvent, DomainEvent domainEvent) {}
}
