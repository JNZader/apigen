package com.jnzader.apigen.core.infrastructure.eventsourcing;

/**
 * Exception thrown when a concurrency conflict is detected during event append.
 *
 * <p>This typically occurs when two processes try to append events to the same aggregate
 * simultaneously. The event store uses optimistic locking to detect such conflicts.
 *
 * <p>Example:
 *
 * <pre>{@code
 * try {
 *     eventStore.append(aggregateId, "Product", events, expectedVersion);
 * } catch (ConcurrencyException e) {
 *     // Reload aggregate and retry, or notify user
 *     log.warn("Concurrent modification detected: {}", e.getMessage());
 * }
 * }</pre>
 */
public class ConcurrencyException extends RuntimeException {

    private final String aggregateId;
    private final long expectedVersion;
    private final long actualVersion;

    /**
     * Creates a new ConcurrencyException.
     *
     * @param aggregateId the aggregate that had the conflict
     * @param expectedVersion the version that was expected
     * @param actualVersion the actual current version
     */
    public ConcurrencyException(String aggregateId, long expectedVersion, long actualVersion) {
        super(
                String.format(
                        "Concurrency conflict for aggregate '%s': expected version %d but found %d",
                        aggregateId, expectedVersion, actualVersion));
        this.aggregateId = aggregateId;
        this.expectedVersion = expectedVersion;
        this.actualVersion = actualVersion;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public long getExpectedVersion() {
        return expectedVersion;
    }

    public long getActualVersion() {
        return actualVersion;
    }
}
