package com.jnzader.apigen.core.infrastructure.webhook;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("WebhookPayload Tests")
class WebhookPayloadTest {

    @Nested
    @DisplayName("Builder Tests")
    class BuilderTests {

        @Test
        @DisplayName("should generate UUID when id not provided")
        void shouldGenerateUuidWhenIdNotProvided() {
            WebhookPayload payload =
                    WebhookPayload.builder().event(WebhookEvent.ENTITY_CREATED).build();

            assertThat(payload.id()).isNotNull().isNotBlank();
        }

        @Test
        @DisplayName("should use provided id")
        void shouldUseProvidedId() {
            WebhookPayload payload =
                    WebhookPayload.builder()
                            .id("custom-id")
                            .event(WebhookEvent.ENTITY_CREATED)
                            .build();

            assertThat(payload.id()).isEqualTo("custom-id");
        }

        @Test
        @DisplayName("should set timestamp when not provided")
        void shouldSetTimestampWhenNotProvided() {
            Instant before = Instant.now();
            WebhookPayload payload =
                    WebhookPayload.builder().event(WebhookEvent.ENTITY_CREATED).build();
            Instant after = Instant.now();

            assertThat(payload.timestamp()).isNotNull();
            assertThat(payload.timestamp()).isBetween(before, after);
        }

        @Test
        @DisplayName("should use provided timestamp")
        void shouldUseProvidedTimestamp() {
            Instant customTime = Instant.parse("2024-06-15T12:00:00Z");

            WebhookPayload payload =
                    WebhookPayload.builder()
                            .event(WebhookEvent.ENTITY_CREATED)
                            .timestamp(customTime)
                            .build();

            assertThat(payload.timestamp()).isEqualTo(customTime);
        }

        @Test
        @DisplayName("should accept WebhookEvent for event field")
        void shouldAcceptWebhookEventForEventField() {
            WebhookPayload payload =
                    WebhookPayload.builder().event(WebhookEvent.ENTITY_CREATED).build();

            assertThat(payload.event()).isEqualTo("entity.created");
        }

        @Test
        @DisplayName("should accept String for event field")
        void shouldAcceptStringForEventField() {
            WebhookPayload payload = WebhookPayload.builder().event("custom.event").build();

            assertThat(payload.event()).isEqualTo("custom.event");
        }

        @Test
        @DisplayName("should set all fields correctly")
        void shouldSetAllFieldsCorrectly() {
            Instant timestamp = Instant.parse("2024-06-15T12:00:00Z");
            Map<String, Object> metadata = Map.of("key", "value");
            Object data = Map.of("name", "Test");

            WebhookPayload payload =
                    WebhookPayload.builder()
                            .id("payload-1")
                            .event(WebhookEvent.ENTITY_UPDATED)
                            .timestamp(timestamp)
                            .resourceType("User")
                            .resourceId(123L)
                            .data(data)
                            .metadata(metadata)
                            .build();

            assertThat(payload.id()).isEqualTo("payload-1");
            assertThat(payload.event()).isEqualTo("entity.updated");
            assertThat(payload.timestamp()).isEqualTo(timestamp);
            assertThat(payload.resourceType()).isEqualTo("User");
            assertThat(payload.resourceId()).isEqualTo(123L);
            assertThat(payload.data()).isEqualTo(data);
            assertThat(payload.metadata()).isEqualTo(metadata);
        }

        @Test
        @DisplayName("should handle null optional fields")
        void shouldHandleNullOptionalFields() {
            WebhookPayload payload =
                    WebhookPayload.builder().event(WebhookEvent.ENTITY_DELETED).build();

            assertThat(payload.resourceType()).isNull();
            assertThat(payload.resourceId()).isNull();
            assertThat(payload.data()).isNull();
            assertThat(payload.metadata()).isNull();
        }

        @Test
        @DisplayName("should handle different event types")
        void shouldHandleDifferentEventTypes() {
            WebhookPayload createdPayload =
                    WebhookPayload.builder().event(WebhookEvent.ENTITY_CREATED).build();
            WebhookPayload updatedPayload =
                    WebhookPayload.builder().event(WebhookEvent.ENTITY_UPDATED).build();
            WebhookPayload deletedPayload =
                    WebhookPayload.builder().event(WebhookEvent.ENTITY_DELETED).build();

            assertThat(createdPayload.event()).isEqualTo("entity.created");
            assertThat(updatedPayload.event()).isEqualTo("entity.updated");
            assertThat(deletedPayload.event()).isEqualTo("entity.deleted");
        }

        @Test
        @DisplayName("should support different resourceId types")
        void shouldSupportDifferentResourceIdTypes() {
            WebhookPayload longIdPayload =
                    WebhookPayload.builder().event("test").resourceId(123L).build();
            WebhookPayload stringIdPayload =
                    WebhookPayload.builder().event("test").resourceId("uuid-123").build();
            WebhookPayload intIdPayload =
                    WebhookPayload.builder().event("test").resourceId(456).build();

            assertThat(longIdPayload.resourceId()).isEqualTo(123L);
            assertThat(stringIdPayload.resourceId()).isEqualTo("uuid-123");
            assertThat(intIdPayload.resourceId()).isEqualTo(456);
        }
    }
}
