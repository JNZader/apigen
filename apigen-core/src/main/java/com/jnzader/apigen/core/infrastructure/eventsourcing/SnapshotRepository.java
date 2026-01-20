package com.jnzader.apigen.core.infrastructure.eventsourcing;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * JPA repository for snapshots.
 *
 * <p>Provides methods to store and retrieve aggregate snapshots.
 */
@Repository
public interface SnapshotRepository extends JpaRepository<Snapshot, String> {

    /**
     * Finds the latest snapshot for an aggregate.
     *
     * @param aggregateId the aggregate identifier
     * @return the most recent snapshot, or empty if none exists
     */
    Optional<Snapshot> findFirstByAggregateIdOrderByVersionDesc(String aggregateId);

    /**
     * Finds a snapshot at or before a specific version.
     *
     * @param aggregateId the aggregate identifier
     * @param version the maximum version to consider
     * @return the most recent snapshot at or before the version
     */
    Optional<Snapshot> findFirstByAggregateIdAndVersionLessThanEqualOrderByVersionDesc(
            String aggregateId, long version);

    /**
     * Deletes all snapshots for an aggregate.
     *
     * @param aggregateId the aggregate identifier
     */
    void deleteByAggregateId(String aggregateId);

    /**
     * Counts snapshots for an aggregate.
     *
     * @param aggregateId the aggregate identifier
     * @return the number of snapshots
     */
    long countByAggregateId(String aggregateId);
}
