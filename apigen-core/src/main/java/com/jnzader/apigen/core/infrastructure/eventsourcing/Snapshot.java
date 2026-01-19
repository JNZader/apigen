package com.jnzader.apigen.core.infrastructure.eventsourcing;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * JPA entity representing a snapshot of an aggregate's state.
 *
 * <p>Snapshots are used to optimize aggregate reconstruction by storing periodic state snapshots.
 * Instead of replaying all events from the beginning, the aggregate can be restored from the latest
 * snapshot and only replay events that occurred after it.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Save snapshot every 100 events
 * if (aggregate.getVersion() % 100 == 0) {
 *     Snapshot snapshot = Snapshot.builder()
 *         .aggregateId(aggregate.getId())
 *         .aggregateType("Product")
 *         .version(aggregate.getVersion())
 *         .state(serializer.serialize(aggregate))
 *         .build();
 *     eventStore.saveSnapshot(snapshot);
 * }
 * }</pre>
 */
@Entity
@Table(
        name = "event_snapshots",
        indexes = {@Index(name = "idx_snapshot_aggregate", columnList = "aggregate_id, version DESC")})
public class Snapshot {

    @Id
    @Column(name = "snapshot_id", length = 36)
    private String snapshotId;

    @Column(name = "aggregate_id", nullable = false, length = 100)
    private String aggregateId;

    @Column(name = "aggregate_type", nullable = false, length = 100)
    private String aggregateType;

    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "state", nullable = false, columnDefinition = "TEXT")
    private String state;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Snapshot() {
        // JPA
    }

    private Snapshot(Builder builder) {
        this.snapshotId = java.util.UUID.randomUUID().toString();
        this.aggregateId = builder.aggregateId;
        this.aggregateType = builder.aggregateType;
        this.version = builder.version;
        this.state = builder.state;
        this.createdAt = Instant.now();
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getSnapshotId() {
        return snapshotId;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public long getVersion() {
        return version;
    }

    public String getState() {
        return state;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public static class Builder {
        private String aggregateId;
        private String aggregateType;
        private long version;
        private String state;

        public Builder aggregateId(String aggregateId) {
            this.aggregateId = aggregateId;
            return this;
        }

        public Builder aggregateType(String aggregateType) {
            this.aggregateType = aggregateType;
            return this;
        }

        public Builder version(long version) {
            this.version = version;
            return this;
        }

        public Builder state(String state) {
            this.state = state;
            return this;
        }

        public Snapshot build() {
            return new Snapshot(this);
        }
    }
}
