package com.jnzader.apigen.core.infrastructure.webhook;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Webhook payload that is sent to webhook subscribers.
 *
 * <p>The payload includes metadata about the event along with the actual data.
 *
 * @param id Unique identifier for this webhook delivery
 * @param event The event type that triggered this webhook
 * @param timestamp When the event occurred
 * @param resourceType The type of resource involved (e.g., "User", "Product")
 * @param resourceId The ID of the resource, if applicable
 * @param data The actual event data
 * @param metadata Additional metadata about the event
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record WebhookPayload(
        String id,
        String event,
        Instant timestamp,
        String resourceType,
        Object resourceId,
        Object data,
        Map<String, Object> metadata) {

    /** Creates a new builder for WebhookPayload. */
    public static Builder builder() {
        return new Builder();
    }

    /** Builder for creating WebhookPayload instances. */
    public static class Builder {
        private String id;
        private String event;
        private Instant timestamp;
        private String resourceType;
        private Object resourceId;
        private Object data;
        private Map<String, Object> metadata;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder event(WebhookEvent event) {
            this.event = event.getEventType();
            return this;
        }

        public Builder event(String event) {
            this.event = event;
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder resourceType(String resourceType) {
            this.resourceType = resourceType;
            return this;
        }

        public Builder resourceId(Object resourceId) {
            this.resourceId = resourceId;
            return this;
        }

        public Builder data(Object data) {
            this.data = data;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public WebhookPayload build() {
            if (id == null) {
                id = UUID.randomUUID().toString();
            }
            if (timestamp == null) {
                timestamp = Instant.now();
            }
            return new WebhookPayload(
                    id, event, timestamp, resourceType, resourceId, data, metadata);
        }
    }
}
