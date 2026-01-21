package com.jnzader.apigen.core.infrastructure.webhook;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("WebhookDelivery Tests")
class WebhookDeliveryTest {

    @Nested
    @DisplayName("Success Factory Tests")
    class SuccessFactoryTests {

        @Test
        @DisplayName("should create successful delivery")
        void shouldCreateSuccessfulDelivery() {
            WebhookDelivery delivery =
                    WebhookDelivery.success(
                            "sub-1",
                            "payload-1",
                            "http://example.com/webhook",
                            200,
                            "OK",
                            1,
                            Duration.ofMillis(150));

            assertThat(delivery.subscriptionId()).isEqualTo("sub-1");
            assertThat(delivery.payloadId()).isEqualTo("payload-1");
            assertThat(delivery.url()).isEqualTo("http://example.com/webhook");
            assertThat(delivery.status()).isEqualTo(WebhookDelivery.DeliveryStatus.SUCCESS);
            assertThat(delivery.httpStatus()).isEqualTo(200);
            assertThat(delivery.responseBody()).isEqualTo("OK");
            assertThat(delivery.attemptNumber()).isEqualTo(1);
            assertThat(delivery.duration()).isEqualTo(Duration.ofMillis(150));
            assertThat(delivery.id()).isNotNull().isNotBlank();
            assertThat(delivery.timestamp()).isNotNull();
        }

        @Test
        @DisplayName("should truncate long response body")
        void shouldTruncateLongResponseBody() {
            String longResponse = "x".repeat(1500);

            WebhookDelivery delivery =
                    WebhookDelivery.success(
                            "sub-1",
                            "payload-1",
                            "http://example.com",
                            200,
                            longResponse,
                            1,
                            Duration.ofMillis(100));

            assertThat(delivery.responseBody()).hasSize(1003); // 1000 + "..."
            assertThat(delivery.responseBody()).endsWith("...");
        }

        @Test
        @DisplayName("should not truncate response body within limit")
        void shouldNotTruncateResponseBodyWithinLimit() {
            String shortResponse = "x".repeat(500);

            WebhookDelivery delivery =
                    WebhookDelivery.success(
                            "sub-1",
                            "payload-1",
                            "http://example.com",
                            200,
                            shortResponse,
                            1,
                            null);

            assertThat(delivery.responseBody()).isEqualTo(shortResponse);
        }

        @Test
        @DisplayName("should handle null response body")
        void shouldHandleNullResponseBody() {
            WebhookDelivery delivery =
                    WebhookDelivery.success(
                            "sub-1", "payload-1", "http://example.com", 200, null, 1, null);

            assertThat(delivery.responseBody()).isNull();
        }
    }

    @Nested
    @DisplayName("FailedWillRetry Factory Tests")
    class FailedWillRetryFactoryTests {

        @Test
        @DisplayName("should create failed delivery with retry")
        void shouldCreateFailedDeliveryWithRetry() {
            WebhookDelivery delivery =
                    WebhookDelivery.failedWillRetry(
                            "sub-1",
                            "payload-1",
                            "http://example.com/webhook",
                            500,
                            "Internal Server Error",
                            1,
                            Duration.ofMillis(200));

            assertThat(delivery.subscriptionId()).isEqualTo("sub-1");
            assertThat(delivery.payloadId()).isEqualTo("payload-1");
            assertThat(delivery.url()).isEqualTo("http://example.com/webhook");
            assertThat(delivery.status())
                    .isEqualTo(WebhookDelivery.DeliveryStatus.FAILED_WILL_RETRY);
            assertThat(delivery.httpStatus()).isEqualTo(500);
            assertThat(delivery.errorMessage()).isEqualTo("Internal Server Error");
            assertThat(delivery.attemptNumber()).isEqualTo(1);
            assertThat(delivery.duration()).isEqualTo(Duration.ofMillis(200));
        }

        @Test
        @DisplayName("should handle null http status for connection errors")
        void shouldHandleNullHttpStatusForConnectionErrors() {
            WebhookDelivery delivery =
                    WebhookDelivery.failedWillRetry(
                            "sub-1",
                            "payload-1",
                            "http://example.com",
                            null,
                            "Connection refused",
                            1,
                            Duration.ofMillis(50));

            assertThat(delivery.httpStatus()).isNull();
            assertThat(delivery.errorMessage()).isEqualTo("Connection refused");
        }
    }

    @Nested
    @DisplayName("FailedPermanent Factory Tests")
    class FailedPermanentFactoryTests {

        @Test
        @DisplayName("should create permanently failed delivery")
        void shouldCreatePermanentlyFailedDelivery() {
            WebhookDelivery delivery =
                    WebhookDelivery.failedPermanent(
                            "sub-1",
                            "payload-1",
                            "http://example.com/webhook",
                            400,
                            "Bad Request",
                            3,
                            Duration.ofMillis(100));

            assertThat(delivery.subscriptionId()).isEqualTo("sub-1");
            assertThat(delivery.payloadId()).isEqualTo("payload-1");
            assertThat(delivery.url()).isEqualTo("http://example.com/webhook");
            assertThat(delivery.status())
                    .isEqualTo(WebhookDelivery.DeliveryStatus.FAILED_PERMANENT);
            assertThat(delivery.httpStatus()).isEqualTo(400);
            assertThat(delivery.errorMessage()).isEqualTo("Bad Request");
            assertThat(delivery.attemptNumber()).isEqualTo(3);
        }

        @Test
        @DisplayName("should handle max retries exceeded")
        void shouldHandleMaxRetriesExceeded() {
            WebhookDelivery delivery =
                    WebhookDelivery.failedPermanent(
                            "sub-1",
                            "payload-1",
                            "http://example.com",
                            503,
                            "Service Unavailable - Max retries exceeded",
                            5,
                            Duration.ofMillis(300));

            assertThat(delivery.status())
                    .isEqualTo(WebhookDelivery.DeliveryStatus.FAILED_PERMANENT);
            assertThat(delivery.attemptNumber()).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("Builder Tests")
    class BuilderTests {

        @Test
        @DisplayName("should generate UUID when id not provided")
        void shouldGenerateUuidWhenIdNotProvided() {
            WebhookDelivery delivery =
                    WebhookDelivery.builder()
                            .subscriptionId("sub-1")
                            .payloadId("payload-1")
                            .url("http://example.com")
                            .status(WebhookDelivery.DeliveryStatus.PENDING)
                            .build();

            assertThat(delivery.id()).isNotNull().isNotBlank();
        }

        @Test
        @DisplayName("should use provided id")
        void shouldUseProvidedId() {
            WebhookDelivery delivery =
                    WebhookDelivery.builder()
                            .id("custom-id")
                            .subscriptionId("sub-1")
                            .payloadId("payload-1")
                            .url("http://example.com")
                            .status(WebhookDelivery.DeliveryStatus.PENDING)
                            .build();

            assertThat(delivery.id()).isEqualTo("custom-id");
        }

        @Test
        @DisplayName("should default attempt number to 1")
        void shouldDefaultAttemptNumberToOne() {
            WebhookDelivery delivery =
                    WebhookDelivery.builder()
                            .subscriptionId("sub-1")
                            .payloadId("payload-1")
                            .url("http://example.com")
                            .status(WebhookDelivery.DeliveryStatus.PENDING)
                            .build();

            assertThat(delivery.attemptNumber()).isEqualTo(1);
        }

        @Test
        @DisplayName("should set timestamp when not provided")
        void shouldSetTimestampWhenNotProvided() {
            Instant before = Instant.now();
            WebhookDelivery delivery =
                    WebhookDelivery.builder()
                            .subscriptionId("sub-1")
                            .payloadId("payload-1")
                            .url("http://example.com")
                            .status(WebhookDelivery.DeliveryStatus.PENDING)
                            .build();
            Instant after = Instant.now();

            assertThat(delivery.timestamp()).isNotNull();
            assertThat(delivery.timestamp()).isBetween(before, after);
        }

        @Test
        @DisplayName("should use provided timestamp")
        void shouldUseProvidedTimestamp() {
            Instant customTime = Instant.parse("2024-06-15T12:00:00Z");

            WebhookDelivery delivery =
                    WebhookDelivery.builder()
                            .subscriptionId("sub-1")
                            .payloadId("payload-1")
                            .url("http://example.com")
                            .status(WebhookDelivery.DeliveryStatus.SUCCESS)
                            .timestamp(customTime)
                            .build();

            assertThat(delivery.timestamp()).isEqualTo(customTime);
        }

        @Test
        @DisplayName("should set all fields correctly")
        void shouldSetAllFieldsCorrectly() {
            Instant timestamp = Instant.parse("2024-06-15T12:00:00Z");
            Duration duration = Duration.ofMillis(250);

            WebhookDelivery delivery =
                    WebhookDelivery.builder()
                            .id("delivery-1")
                            .subscriptionId("sub-1")
                            .payloadId("payload-1")
                            .url("http://example.com/webhook")
                            .status(WebhookDelivery.DeliveryStatus.SUCCESS)
                            .httpStatus(200)
                            .responseBody("OK")
                            .errorMessage(null)
                            .attemptNumber(2)
                            .duration(duration)
                            .timestamp(timestamp)
                            .build();

            assertThat(delivery.id()).isEqualTo("delivery-1");
            assertThat(delivery.subscriptionId()).isEqualTo("sub-1");
            assertThat(delivery.payloadId()).isEqualTo("payload-1");
            assertThat(delivery.url()).isEqualTo("http://example.com/webhook");
            assertThat(delivery.status()).isEqualTo(WebhookDelivery.DeliveryStatus.SUCCESS);
            assertThat(delivery.httpStatus()).isEqualTo(200);
            assertThat(delivery.responseBody()).isEqualTo("OK");
            assertThat(delivery.errorMessage()).isNull();
            assertThat(delivery.attemptNumber()).isEqualTo(2);
            assertThat(delivery.duration()).isEqualTo(duration);
            assertThat(delivery.timestamp()).isEqualTo(timestamp);
        }
    }

    @Nested
    @DisplayName("DeliveryStatus Tests")
    class DeliveryStatusTests {

        @Test
        @DisplayName("should have all expected status values")
        void shouldHaveAllExpectedStatusValues() {
            assertThat(WebhookDelivery.DeliveryStatus.values())
                    .containsExactlyInAnyOrder(
                            WebhookDelivery.DeliveryStatus.SUCCESS,
                            WebhookDelivery.DeliveryStatus.FAILED_WILL_RETRY,
                            WebhookDelivery.DeliveryStatus.FAILED_PERMANENT,
                            WebhookDelivery.DeliveryStatus.PENDING);
        }
    }
}
