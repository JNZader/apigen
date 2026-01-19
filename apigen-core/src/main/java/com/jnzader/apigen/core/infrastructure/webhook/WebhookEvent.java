package com.jnzader.apigen.core.infrastructure.webhook;

/**
 * Standard webhook event types that can trigger webhook notifications.
 *
 * <p>These events follow a consistent naming pattern: {@code RESOURCE.ACTION}
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * webhookService.dispatch(WebhookEvent.ENTITY_CREATED, payload);
 * }</pre>
 */
public enum WebhookEvent {

    // Entity lifecycle events
    /** Fired when a new entity is created. */
    ENTITY_CREATED("entity.created"),

    /** Fired when an entity is updated. */
    ENTITY_UPDATED("entity.updated"),

    /** Fired when an entity is soft-deleted. */
    ENTITY_DELETED("entity.deleted"),

    /** Fired when a soft-deleted entity is restored. */
    ENTITY_RESTORED("entity.restored"),

    /** Fired when an entity is permanently deleted. */
    ENTITY_PURGED("entity.purged"),

    // Batch operation events
    /** Fired when a batch import completes. */
    BATCH_IMPORT_COMPLETED("batch.import.completed"),

    /** Fired when a batch export completes. */
    BATCH_EXPORT_COMPLETED("batch.export.completed"),

    // Authentication events
    /** Fired when a user logs in successfully. */
    USER_LOGIN("user.login"),

    /** Fired when a user logs out. */
    USER_LOGOUT("user.logout"),

    /** Fired when a login attempt fails. */
    USER_LOGIN_FAILED("user.login.failed"),

    // Security events
    /** Fired when rate limit is exceeded. */
    RATE_LIMIT_EXCEEDED("security.rate_limit.exceeded"),

    /** Fired when an unauthorized access attempt is detected. */
    UNAUTHORIZED_ACCESS("security.unauthorized.access"),

    // System events
    /** Fired for health check or ping requests. */
    PING("system.ping");

    private final String eventType;

    WebhookEvent(String eventType) {
        this.eventType = eventType;
    }

    /**
     * Gets the event type string representation.
     *
     * @return the event type (e.g., "entity.created")
     */
    public String getEventType() {
        return eventType;
    }

    /**
     * Gets a WebhookEvent from its string representation.
     *
     * @param eventType the event type string
     * @return the corresponding WebhookEvent
     * @throws IllegalArgumentException if no matching event is found
     */
    public static WebhookEvent fromString(String eventType) {
        for (WebhookEvent event : values()) {
            if (event.eventType.equals(eventType)) {
                return event;
            }
        }
        throw new IllegalArgumentException("Unknown webhook event type: " + eventType);
    }
}
