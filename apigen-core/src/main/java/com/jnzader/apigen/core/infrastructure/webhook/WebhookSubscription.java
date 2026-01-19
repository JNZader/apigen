package com.jnzader.apigen.core.infrastructure.webhook;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Represents a webhook subscription configuration.
 *
 * <p>A subscription defines:
 *
 * <ul>
 *   <li>The target URL to receive webhook notifications
 *   <li>Which events to subscribe to
 *   <li>A secret for HMAC signature verification
 *   <li>Whether the subscription is active
 * </ul>
 *
 * @param id Unique identifier for this subscription
 * @param name Human-readable name for the subscription
 * @param url Target URL to send webhook payloads
 * @param secret Secret key for HMAC-SHA256 signature
 * @param events Set of events this subscription is interested in
 * @param active Whether this subscription is currently active
 * @param createdAt When the subscription was created
 * @param updatedAt When the subscription was last updated
 */
public record WebhookSubscription(
        String id,
        String name,
        String url,
        String secret,
        Set<WebhookEvent> events,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {

    /** Creates a new builder for WebhookSubscription. */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Checks if this subscription is interested in the given event.
     *
     * @param event the event to check
     * @return true if this subscription should receive the event
     */
    public boolean isInterestedIn(WebhookEvent event) {
        return active && events != null && events.contains(event);
    }

    /** Builder for creating WebhookSubscription instances. */
    public static class Builder {
        private String id;
        private String name;
        private String url;
        private String secret;
        private Set<WebhookEvent> events;
        private boolean active = true;
        private Instant createdAt;
        private Instant updatedAt;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder url(String url) {
            this.url = url;
            return this;
        }

        public Builder secret(String secret) {
            this.secret = secret;
            return this;
        }

        public Builder events(Set<WebhookEvent> events) {
            this.events = events;
            return this;
        }

        public Builder active(boolean active) {
            this.active = active;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public WebhookSubscription build() {
            if (id == null) {
                id = UUID.randomUUID().toString();
            }
            Instant now = Instant.now();
            if (createdAt == null) {
                createdAt = now;
            }
            if (updatedAt == null) {
                updatedAt = now;
            }
            return new WebhookSubscription(
                    id, name, url, secret, events, active, createdAt, updatedAt);
        }
    }
}
