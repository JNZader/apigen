package com.jnzader.apigen.core.infrastructure.eventsourcing;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * JPA repository for stored events.
 *
 * <p>Provides optimized queries for event stream operations.
 */
@Repository
public interface StoredEventRepository extends JpaRepository<StoredEvent, String> {

    /**
     * Finds all events for an aggregate ordered by version.
     *
     * @param aggregateId the aggregate identifier
     * @return events in version order
     */
    List<StoredEvent> findByAggregateIdOrderByVersionAsc(String aggregateId);

    /**
     * Finds events for an aggregate starting from a specific version.
     *
     * @param aggregateId the aggregate identifier
     * @param version the version to start from (exclusive)
     * @return events after the specified version
     */
    List<StoredEvent> findByAggregateIdAndVersionGreaterThanOrderByVersionAsc(
            String aggregateId, long version);

    /**
     * Finds events for an aggregate within a version range.
     *
     * @param aggregateId the aggregate identifier
     * @param fromVersion the version to start from (exclusive)
     * @param toVersion the version to end at (inclusive)
     * @return events in the specified range
     */
    List<StoredEvent>
            findByAggregateIdAndVersionGreaterThanAndVersionLessThanEqualOrderByVersionAsc(
                    String aggregateId, long fromVersion, long toVersion);

    /**
     * Gets the current version of an aggregate's event stream.
     *
     * @param aggregateId the aggregate identifier
     * @return the maximum version, or null if no events exist
     */
    @Query("SELECT MAX(e.version) FROM StoredEvent e WHERE e.aggregateId = :aggregateId")
    Optional<Long> findMaxVersionByAggregateId(@Param("aggregateId") String aggregateId);

    /**
     * Checks if any events exist for an aggregate.
     *
     * @param aggregateId the aggregate identifier
     * @return true if at least one event exists
     */
    boolean existsByAggregateId(String aggregateId);

    /**
     * Finds events by type with a limit.
     *
     * @param eventType the event type
     * @return events of the specified type
     */
    List<StoredEvent> findByEventTypeOrderByStoredAtDesc(String eventType);

    /**
     * Counts events for an aggregate.
     *
     * @param aggregateId the aggregate identifier
     * @return the number of events
     */
    long countByAggregateId(String aggregateId);
}
