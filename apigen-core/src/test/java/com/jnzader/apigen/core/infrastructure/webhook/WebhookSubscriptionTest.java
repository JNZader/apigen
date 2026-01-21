package com.jnzader.apigen.core.infrastructure.webhook;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("WebhookSubscription Tests")
class WebhookSubscriptionTest {

    @Nested
    @DisplayName("IsInterestedIn Tests")
    class IsInterestedInTests {

        @Test
        @DisplayName("should return true for active subscription with matching event")
        void shouldReturnTrueForActiveWithMatchingEvent() {
            WebhookSubscription subscription =
                    WebhookSubscription.builder()
                            .name("Test")
                            .url("http://example.com")
                            .secret("secret")
                            .events(
                                    Set.of(
                                            WebhookEvent.ENTITY_CREATED,
                                            WebhookEvent.ENTITY_UPDATED))
                            .active(true)
                            .build();

            assertThat(subscription.isInterestedIn(WebhookEvent.ENTITY_CREATED)).isTrue();
            assertThat(subscription.isInterestedIn(WebhookEvent.ENTITY_UPDATED)).isTrue();
        }

        @Test
        @DisplayName("should return false for active subscription without matching event")
        void shouldReturnFalseForActiveWithoutMatchingEvent() {
            WebhookSubscription subscription =
                    WebhookSubscription.builder()
                            .name("Test")
                            .url("http://example.com")
                            .secret("secret")
                            .events(Set.of(WebhookEvent.ENTITY_CREATED))
                            .active(true)
                            .build();

            assertThat(subscription.isInterestedIn(WebhookEvent.ENTITY_DELETED)).isFalse();
        }

        @Test
        @DisplayName("should return false for inactive subscription")
        void shouldReturnFalseForInactiveSubscription() {
            WebhookSubscription subscription =
                    WebhookSubscription.builder()
                            .name("Test")
                            .url("http://example.com")
                            .secret("secret")
                            .events(Set.of(WebhookEvent.ENTITY_CREATED))
                            .active(false)
                            .build();

            assertThat(subscription.isInterestedIn(WebhookEvent.ENTITY_CREATED)).isFalse();
        }

        @Test
        @DisplayName("should return false when events set is null")
        void shouldReturnFalseWhenEventsNull() {
            WebhookSubscription subscription =
                    new WebhookSubscription(
                            "id", "name", "http://example.com", "secret", null, true, null, null);

            assertThat(subscription.isInterestedIn(WebhookEvent.ENTITY_CREATED)).isFalse();
        }

        @Test
        @DisplayName("should return false when events set is empty")
        void shouldReturnFalseWhenEventsEmpty() {
            WebhookSubscription subscription =
                    WebhookSubscription.builder()
                            .name("Test")
                            .url("http://example.com")
                            .secret("secret")
                            .events(Set.of())
                            .active(true)
                            .build();

            assertThat(subscription.isInterestedIn(WebhookEvent.ENTITY_CREATED)).isFalse();
        }
    }

    @Nested
    @DisplayName("Builder Tests")
    class BuilderTests {

        @Test
        @DisplayName("should generate UUID when id not provided")
        void shouldGenerateUuidWhenIdNotProvided() {
            WebhookSubscription subscription =
                    WebhookSubscription.builder()
                            .name("Test")
                            .url("http://example.com")
                            .secret("secret")
                            .events(Set.of(WebhookEvent.ENTITY_CREATED))
                            .build();

            assertThat(subscription.id()).isNotNull().isNotBlank();
        }

        @Test
        @DisplayName("should use provided id")
        void shouldUseProvidedId() {
            WebhookSubscription subscription =
                    WebhookSubscription.builder()
                            .id("custom-id")
                            .name("Test")
                            .url("http://example.com")
                            .secret("secret")
                            .events(Set.of(WebhookEvent.ENTITY_CREATED))
                            .build();

            assertThat(subscription.id()).isEqualTo("custom-id");
        }

        @Test
        @DisplayName("should default active to true")
        void shouldDefaultActiveToTrue() {
            WebhookSubscription subscription =
                    WebhookSubscription.builder()
                            .name("Test")
                            .url("http://example.com")
                            .secret("secret")
                            .events(Set.of(WebhookEvent.ENTITY_CREATED))
                            .build();

            assertThat(subscription.active()).isTrue();
        }

        @Test
        @DisplayName("should allow setting active to false")
        void shouldAllowSettingActiveToFalse() {
            WebhookSubscription subscription =
                    WebhookSubscription.builder()
                            .name("Test")
                            .url("http://example.com")
                            .secret("secret")
                            .events(Set.of(WebhookEvent.ENTITY_CREATED))
                            .active(false)
                            .build();

            assertThat(subscription.active()).isFalse();
        }

        @Test
        @DisplayName("should set createdAt when not provided")
        void shouldSetCreatedAtWhenNotProvided() {
            Instant before = Instant.now();
            WebhookSubscription subscription =
                    WebhookSubscription.builder()
                            .name("Test")
                            .url("http://example.com")
                            .secret("secret")
                            .events(Set.of(WebhookEvent.ENTITY_CREATED))
                            .build();
            Instant after = Instant.now();

            assertThat(subscription.createdAt()).isNotNull();
            assertThat(subscription.createdAt()).isBetween(before, after);
        }

        @Test
        @DisplayName("should use provided createdAt")
        void shouldUseProvidedCreatedAt() {
            Instant customTime = Instant.parse("2024-01-01T00:00:00Z");
            WebhookSubscription subscription =
                    WebhookSubscription.builder()
                            .name("Test")
                            .url("http://example.com")
                            .secret("secret")
                            .events(Set.of(WebhookEvent.ENTITY_CREATED))
                            .createdAt(customTime)
                            .build();

            assertThat(subscription.createdAt()).isEqualTo(customTime);
        }

        @Test
        @DisplayName("should set updatedAt when not provided")
        void shouldSetUpdatedAtWhenNotProvided() {
            Instant before = Instant.now();
            WebhookSubscription subscription =
                    WebhookSubscription.builder()
                            .name("Test")
                            .url("http://example.com")
                            .secret("secret")
                            .events(Set.of(WebhookEvent.ENTITY_CREATED))
                            .build();
            Instant after = Instant.now();

            assertThat(subscription.updatedAt()).isNotNull();
            assertThat(subscription.updatedAt()).isBetween(before, after);
        }

        @Test
        @DisplayName("should use provided updatedAt")
        void shouldUseProvidedUpdatedAt() {
            Instant customTime = Instant.parse("2024-06-15T12:30:00Z");
            WebhookSubscription subscription =
                    WebhookSubscription.builder()
                            .name("Test")
                            .url("http://example.com")
                            .secret("secret")
                            .events(Set.of(WebhookEvent.ENTITY_CREATED))
                            .updatedAt(customTime)
                            .build();

            assertThat(subscription.updatedAt()).isEqualTo(customTime);
        }

        @Test
        @DisplayName("should set all fields correctly")
        void shouldSetAllFieldsCorrectly() {
            Instant createdAt = Instant.parse("2024-01-01T00:00:00Z");
            Instant updatedAt = Instant.parse("2024-06-15T12:00:00Z");
            Set<WebhookEvent> events =
                    Set.of(WebhookEvent.ENTITY_CREATED, WebhookEvent.ENTITY_UPDATED);

            WebhookSubscription subscription =
                    WebhookSubscription.builder()
                            .id("test-id")
                            .name("Test Subscription")
                            .url("http://example.com/webhook")
                            .secret("my-secret")
                            .events(events)
                            .active(true)
                            .createdAt(createdAt)
                            .updatedAt(updatedAt)
                            .build();

            assertThat(subscription.id()).isEqualTo("test-id");
            assertThat(subscription.name()).isEqualTo("Test Subscription");
            assertThat(subscription.url()).isEqualTo("http://example.com/webhook");
            assertThat(subscription.secret()).isEqualTo("my-secret");
            assertThat(subscription.events()).isEqualTo(events);
            assertThat(subscription.active()).isTrue();
            assertThat(subscription.createdAt()).isEqualTo(createdAt);
            assertThat(subscription.updatedAt()).isEqualTo(updatedAt);
        }
    }
}
