package com.jnzader.apigen.core.infrastructure.webhook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("WebhookEvent Tests")
class WebhookEventTest {

    @Nested
    @DisplayName("Event Types")
    class EventTypeTests {

        @ParameterizedTest
        @EnumSource(WebhookEvent.class)
        @DisplayName("should have non-null event type")
        void shouldHaveNonNullEventType(WebhookEvent event) {
            assertThat(event.getEventType()).isNotNull();
            assertThat(event.getEventType()).isNotBlank();
        }

        @ParameterizedTest
        @EnumSource(WebhookEvent.class)
        @DisplayName("should follow naming convention")
        void shouldFollowNamingConvention(WebhookEvent event) {
            String eventType = event.getEventType();
            // Event types should be lowercase with dots as separators
            assertThat(eventType).matches("[a-z]+\\.[a-z_.]+");
        }

        @Test
        @DisplayName("should have unique event types")
        void shouldHaveUniqueEventTypes() {
            long uniqueCount =
                    java.util.Arrays.stream(WebhookEvent.values())
                            .map(WebhookEvent::getEventType)
                            .distinct()
                            .count();
            assertThat(uniqueCount).isEqualTo(WebhookEvent.values().length);
        }
    }

    @Nested
    @DisplayName("FromString")
    class FromStringTests {

        @ParameterizedTest
        @EnumSource(WebhookEvent.class)
        @DisplayName("should convert from string for all events")
        void shouldConvertFromStringForAllEvents(WebhookEvent event) {
            WebhookEvent result = WebhookEvent.fromString(event.getEventType());
            assertThat(result).isEqualTo(event);
        }

        @Test
        @DisplayName("should throw for unknown event type")
        void shouldThrowForUnknownEventType() {
            assertThatThrownBy(() -> WebhookEvent.fromString("unknown.event"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Unknown webhook event type");
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "  ", "ENTITY.CREATED"})
        @DisplayName("should throw for invalid event types")
        void shouldThrowForInvalidEventTypes(String eventType) {
            assertThatThrownBy(() -> WebhookEvent.fromString(eventType))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Specific Events")
    class SpecificEventTests {

        @Test
        @DisplayName("entity lifecycle events should follow entity.* pattern")
        void entityLifecycleEventsShouldFollowPattern() {
            assertThat(WebhookEvent.ENTITY_CREATED.getEventType()).startsWith("entity.");
            assertThat(WebhookEvent.ENTITY_UPDATED.getEventType()).startsWith("entity.");
            assertThat(WebhookEvent.ENTITY_DELETED.getEventType()).startsWith("entity.");
            assertThat(WebhookEvent.ENTITY_RESTORED.getEventType()).startsWith("entity.");
            assertThat(WebhookEvent.ENTITY_PURGED.getEventType()).startsWith("entity.");
        }

        @Test
        @DisplayName("batch events should follow batch.* pattern")
        void batchEventsShouldFollowPattern() {
            assertThat(WebhookEvent.BATCH_IMPORT_COMPLETED.getEventType()).startsWith("batch.");
            assertThat(WebhookEvent.BATCH_EXPORT_COMPLETED.getEventType()).startsWith("batch.");
        }

        @Test
        @DisplayName("user events should follow user.* pattern")
        void userEventsShouldFollowPattern() {
            assertThat(WebhookEvent.USER_LOGIN.getEventType()).startsWith("user.");
            assertThat(WebhookEvent.USER_LOGOUT.getEventType()).startsWith("user.");
            assertThat(WebhookEvent.USER_LOGIN_FAILED.getEventType()).startsWith("user.");
        }

        @Test
        @DisplayName("security events should follow security.* pattern")
        void securityEventsShouldFollowPattern() {
            assertThat(WebhookEvent.RATE_LIMIT_EXCEEDED.getEventType()).startsWith("security.");
            assertThat(WebhookEvent.UNAUTHORIZED_ACCESS.getEventType()).startsWith("security.");
        }

        @Test
        @DisplayName("system events should follow system.* pattern")
        void systemEventsShouldFollowPattern() {
            assertThat(WebhookEvent.PING.getEventType()).startsWith("system.");
        }
    }
}
