package com.jnzader.apigen.core.infrastructure.eventsourcing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * JPA entity representing a stored domain event.
 *
 * <p>This entity is used by the event store to persist events. Events are immutable once stored.
 *
 * <p>The table is optimized for event stream queries with indexes on aggregate_id and version.
 */
@Entity
@Table(
        name = "event_store",
        indexes = {
            @Index(name = "idx_event_aggregate", columnList = "aggregate_id, version"),
            @Index(name = "idx_event_type", columnList = "event_type"),
            @Index(name = "idx_event_occurred", columnList = "occurred_at")
        })
public class StoredEvent {

    @Id
    @Column(name = "event_id", length = 36)
    private String eventId;

    @Column(name = "aggregate_id", nullable = false, length = 100)
    private String aggregateId;

    @Column(name = "aggregate_type", nullable = false, length = 100)
    private String aggregateType;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "stored_at", nullable = false)
    private Instant storedAt;

    protected StoredEvent() {
        // JPA
    }

    private StoredEvent(Builder builder) {
        this.eventId = builder.eventId;
        this.aggregateId = builder.aggregateId;
        this.aggregateType = builder.aggregateType;
        this.eventType = builder.eventType;
        this.version = builder.version;
        this.payload = builder.payload;
        this.metadata = builder.metadata;
        this.occurredAt = builder.occurredAt;
        this.storedAt = Instant.now();
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getEventId() {
        return eventId;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public String getEventType() {
        return eventType;
    }

    public long getVersion() {
        return version;
    }

    public String getPayload() {
        return payload;
    }

    public String getMetadata() {
        return metadata;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public Instant getStoredAt() {
        return storedAt;
    }

    public static class Builder {
        private String eventId;
        private String aggregateId;
        private String aggregateType;
        private String eventType;
        private long version;
        private String payload;
        private String metadata;
        private Instant occurredAt;

        public Builder eventId(String eventId) {
            this.eventId = eventId;
            return this;
        }

        public Builder aggregateId(String aggregateId) {
            this.aggregateId = aggregateId;
            return this;
        }

        public Builder aggregateType(String aggregateType) {
            this.aggregateType = aggregateType;
            return this;
        }

        public Builder eventType(String eventType) {
            this.eventType = eventType;
            return this;
        }

        public Builder version(long version) {
            this.version = version;
            return this;
        }

        public Builder payload(String payload) {
            this.payload = payload;
            return this;
        }

        public Builder metadata(String metadata) {
            this.metadata = metadata;
            return this;
        }

        public Builder occurredAt(Instant occurredAt) {
            this.occurredAt = occurredAt;
            return this;
        }

        public StoredEvent build() {
            return new StoredEvent(this);
        }
    }
}
