package com.jnzader.apigen.core.infrastructure.webhook;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Records the result of a webhook delivery attempt.
 *
 * <p>This record tracks:
 *
 * <ul>
 *   <li>Which subscription and payload were involved
 *   <li>The HTTP response status
 *   <li>Whether the delivery was successful
 *   <li>Retry information
 * </ul>
 *
 * @param id Unique identifier for this delivery attempt
 * @param subscriptionId The subscription this delivery is for
 * @param payloadId The webhook payload ID
 * @param url The target URL
 * @param status The delivery status
 * @param httpStatus The HTTP response status code (null if connection failed)
 * @param responseBody The response body (truncated if too long)
 * @param errorMessage Error message if delivery failed
 * @param attemptNumber Which retry attempt this is (1-based)
 * @param duration How long the request took
 * @param timestamp When this delivery attempt occurred
 */
public record WebhookDelivery(
        String id,
        String subscriptionId,
        String payloadId,
        String url,
        DeliveryStatus status,
        Integer httpStatus,
        String responseBody,
        String errorMessage,
        int attemptNumber,
        Duration duration,
        Instant timestamp) {

    /** Delivery status states. */
    public enum DeliveryStatus {
        /** Successfully delivered (2xx response). */
        SUCCESS,
        /** Delivery failed but will be retried. */
        FAILED_WILL_RETRY,
        /** Delivery permanently failed after max retries. */
        FAILED_PERMANENT,
        /** Delivery is pending. */
        PENDING
    }

    /** Creates a new builder for WebhookDelivery. */
    public static Builder builder() {
        return new Builder();
    }

    /** Creates a successful delivery record. */
    public static WebhookDelivery success(
            String subscriptionId,
            String payloadId,
            String url,
            int httpStatus,
            String responseBody,
            int attemptNumber,
            Duration duration) {
        return builder()
                .subscriptionId(subscriptionId)
                .payloadId(payloadId)
                .url(url)
                .status(DeliveryStatus.SUCCESS)
                .httpStatus(httpStatus)
                .responseBody(truncate(responseBody, 1000))
                .attemptNumber(attemptNumber)
                .duration(duration)
                .build();
    }

    /** Creates a failed delivery record that will be retried. */
    public static WebhookDelivery failedWillRetry(
            String subscriptionId,
            String payloadId,
            String url,
            Integer httpStatus,
            String errorMessage,
            int attemptNumber,
            Duration duration) {
        return builder()
                .subscriptionId(subscriptionId)
                .payloadId(payloadId)
                .url(url)
                .status(DeliveryStatus.FAILED_WILL_RETRY)
                .httpStatus(httpStatus)
                .errorMessage(errorMessage)
                .attemptNumber(attemptNumber)
                .duration(duration)
                .build();
    }

    /** Creates a permanently failed delivery record. */
    public static WebhookDelivery failedPermanent(
            String subscriptionId,
            String payloadId,
            String url,
            Integer httpStatus,
            String errorMessage,
            int attemptNumber,
            Duration duration) {
        return builder()
                .subscriptionId(subscriptionId)
                .payloadId(payloadId)
                .url(url)
                .status(DeliveryStatus.FAILED_PERMANENT)
                .httpStatus(httpStatus)
                .errorMessage(errorMessage)
                .attemptNumber(attemptNumber)
                .duration(duration)
                .build();
    }

    private static String truncate(String str, int maxLength) {
        if (str == null || str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength) + "...";
    }

    /** Builder for creating WebhookDelivery instances. */
    public static class Builder {
        private String id;
        private String subscriptionId;
        private String payloadId;
        private String url;
        private DeliveryStatus status;
        private Integer httpStatus;
        private String responseBody;
        private String errorMessage;
        private int attemptNumber = 1;
        private Duration duration;
        private Instant timestamp;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder subscriptionId(String subscriptionId) {
            this.subscriptionId = subscriptionId;
            return this;
        }

        public Builder payloadId(String payloadId) {
            this.payloadId = payloadId;
            return this;
        }

        public Builder url(String url) {
            this.url = url;
            return this;
        }

        public Builder status(DeliveryStatus status) {
            this.status = status;
            return this;
        }

        public Builder httpStatus(Integer httpStatus) {
            this.httpStatus = httpStatus;
            return this;
        }

        public Builder responseBody(String responseBody) {
            this.responseBody = responseBody;
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public Builder attemptNumber(int attemptNumber) {
            this.attemptNumber = attemptNumber;
            return this;
        }

        public Builder duration(Duration duration) {
            this.duration = duration;
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public WebhookDelivery build() {
            if (id == null) {
                id = UUID.randomUUID().toString();
            }
            if (timestamp == null) {
                timestamp = Instant.now();
            }
            return new WebhookDelivery(
                    id,
                    subscriptionId,
                    payloadId,
                    url,
                    status,
                    httpStatus,
                    responseBody,
                    errorMessage,
                    attemptNumber,
                    duration,
                    timestamp);
        }
    }
}
