package com.jnzader.apigen.core.infrastructure.webhook;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for dispatching webhook notifications to subscribers.
 *
 * <p>Features:
 *
 * <ul>
 *   <li>Asynchronous delivery using virtual threads
 *   <li>HMAC-SHA256 signatures for request authentication
 *   <li>Configurable retry logic with exponential backoff
 *   <li>Delivery tracking and logging
 * </ul>
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * WebhookPayload payload = WebhookPayload.builder()
 *     .event(WebhookEvent.ENTITY_CREATED)
 *     .resourceType("User")
 *     .resourceId(123L)
 *     .data(userData)
 *     .build();
 *
 * webhookService.dispatch(WebhookEvent.ENTITY_CREATED, payload);
 * }</pre>
 */
public class WebhookService {

    private static final Logger log = LoggerFactory.getLogger(WebhookService.class);

    private static final String HEADER_WEBHOOK_ID = "X-Webhook-Id";
    private static final String HEADER_WEBHOOK_SIGNATURE = "X-Webhook-Signature";
    private static final String HEADER_WEBHOOK_TIMESTAMP = "X-Webhook-Timestamp";
    private static final String HEADER_WEBHOOK_EVENT = "X-Webhook-Event";
    private static final String CONTENT_TYPE_JSON = "application/json";

    private final WebhookSubscriptionRepository subscriptionRepository;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final ExecutorService executorService;
    private final WebhookConfig config;
    private Consumer<WebhookDelivery> deliveryCallback;

    /**
     * Creates a new WebhookService with the given repository and default configuration.
     *
     * @param subscriptionRepository repository for webhook subscriptions
     */
    public WebhookService(WebhookSubscriptionRepository subscriptionRepository) {
        this(subscriptionRepository, new ObjectMapper(), WebhookConfig.defaults());
    }

    /**
     * Creates a new WebhookService with the given configuration.
     *
     * @param subscriptionRepository repository for webhook subscriptions
     * @param objectMapper JSON object mapper
     * @param config webhook configuration
     */
    public WebhookService(
            WebhookSubscriptionRepository subscriptionRepository,
            ObjectMapper objectMapper,
            WebhookConfig config) {
        this.subscriptionRepository = subscriptionRepository;
        this.objectMapper = objectMapper;
        this.config = config;
        this.httpClient =
                HttpClient.newBuilder()
                        .connectTimeout(config.connectTimeout())
                        .executor(Executors.newVirtualThreadPerTaskExecutor())
                        .build();
        this.executorService = Executors.newVirtualThreadPerTaskExecutor();
    }

    /**
     * Sets a callback to be invoked after each delivery attempt.
     *
     * @param callback the callback to invoke
     */
    public void setDeliveryCallback(Consumer<WebhookDelivery> callback) {
        this.deliveryCallback = callback;
    }

    /**
     * Dispatches a webhook event to all interested subscribers.
     *
     * @param event the event type
     * @param payload the payload to send
     * @return completable futures for each delivery
     */
    public List<CompletableFuture<WebhookDelivery>> dispatch(
            WebhookEvent event, WebhookPayload payload) {
        List<WebhookSubscription> subscribers = subscriptionRepository.findByEvent(event);

        if (subscribers.isEmpty()) {
            log.debug("No subscribers for event: {}", event.getEventType());
            return List.of();
        }

        log.info(
                "Dispatching event {} to {} subscriber(s)",
                event.getEventType(),
                subscribers.size());

        return subscribers.stream()
                .map(subscription -> deliverAsync(subscription, payload))
                .toList();
    }

    /**
     * Dispatches a webhook event synchronously and waits for all deliveries.
     *
     * @param event the event type
     * @param payload the payload to send
     * @return list of delivery results
     */
    public List<WebhookDelivery> dispatchAndWait(WebhookEvent event, WebhookPayload payload) {
        List<CompletableFuture<WebhookDelivery>> futures = dispatch(event, payload);
        return futures.stream().map(CompletableFuture::join).toList();
    }

    /**
     * Sends a test ping to a specific subscription.
     *
     * @param subscriptionId the subscription ID
     * @return the delivery result
     */
    public CompletableFuture<WebhookDelivery> sendPing(String subscriptionId) {
        return subscriptionRepository
                .findById(subscriptionId)
                .map(
                        subscription -> {
                            WebhookPayload payload =
                                    WebhookPayload.builder()
                                            .event(WebhookEvent.PING)
                                            .data("pong")
                                            .build();
                            return deliverAsync(subscription, payload);
                        })
                .orElseGet(
                        () ->
                                CompletableFuture.completedFuture(
                                        WebhookDelivery.failedPermanent(
                                                subscriptionId,
                                                null,
                                                null,
                                                null,
                                                "Subscription not found",
                                                1,
                                                Duration.ZERO)));
    }

    private CompletableFuture<WebhookDelivery> deliverAsync(
            WebhookSubscription subscription, WebhookPayload payload) {
        return CompletableFuture.supplyAsync(
                () -> deliverWithRetry(subscription, payload, 1), executorService);
    }

    private WebhookDelivery deliverWithRetry(
            WebhookSubscription subscription, WebhookPayload payload, int attemptNumber) {
        Instant startTime = Instant.now();
        String payloadJson;

        try {
            payloadJson = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize webhook payload", e);
            return createFailedDelivery(
                    subscription,
                    payload,
                    null,
                    "Serialization failed: " + e.getMessage(),
                    attemptNumber,
                    Duration.ZERO,
                    true);
        }

        try {
            Instant timestamp = Instant.now();
            String signature = WebhookSignature.sign(payloadJson, subscription.secret(), timestamp);

            HttpRequest request =
                    HttpRequest.newBuilder()
                            .uri(URI.create(subscription.url()))
                            .timeout(config.requestTimeout())
                            .header("Content-Type", CONTENT_TYPE_JSON)
                            .header(HEADER_WEBHOOK_ID, payload.id())
                            .header(HEADER_WEBHOOK_EVENT, payload.event())
                            .header(
                                    HEADER_WEBHOOK_TIMESTAMP,
                                    String.valueOf(timestamp.getEpochSecond()))
                            .header(HEADER_WEBHOOK_SIGNATURE, signature)
                            .POST(HttpRequest.BodyPublishers.ofString(payloadJson))
                            .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            Duration duration = Duration.between(startTime, Instant.now());

            int statusCode = response.statusCode();
            boolean success = statusCode >= 200 && statusCode < 300;

            if (success) {
                log.debug(
                        "Webhook delivered successfully to {} (status: {})",
                        subscription.url(),
                        statusCode);
                WebhookDelivery delivery =
                        WebhookDelivery.success(
                                subscription.id(),
                                payload.id(),
                                subscription.url(),
                                statusCode,
                                response.body(),
                                attemptNumber,
                                duration);
                notifyDelivery(delivery);
                return delivery;
            } else {
                log.warn(
                        "Webhook delivery failed to {} (status: {}, attempt: {})",
                        subscription.url(),
                        statusCode,
                        attemptNumber);
                return handleFailedDelivery(
                        subscription,
                        payload,
                        statusCode,
                        "HTTP " + statusCode,
                        attemptNumber,
                        duration);
            }

        } catch (Exception e) {
            Duration duration = Duration.between(startTime, Instant.now());
            log.warn(
                    "Webhook delivery error to {} (attempt: {}): {}",
                    subscription.url(),
                    attemptNumber,
                    e.getMessage());
            return handleFailedDelivery(
                    subscription, payload, null, e.getMessage(), attemptNumber, duration);
        }
    }

    private WebhookDelivery handleFailedDelivery(
            WebhookSubscription subscription,
            WebhookPayload payload,
            Integer httpStatus,
            String errorMessage,
            int attemptNumber,
            Duration duration) {

        boolean shouldRetry = attemptNumber < config.maxRetries();
        boolean isRetryableStatus = httpStatus == null || httpStatus >= 500 || httpStatus == 429;

        if (shouldRetry && isRetryableStatus) {
            // Schedule retry with exponential backoff
            int nextAttempt = attemptNumber + 1;
            long delayMs = calculateBackoff(attemptNumber);
            log.debug(
                    "Scheduling retry {} in {}ms for {}", nextAttempt, delayMs, subscription.url());

            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return createFailedDelivery(
                        subscription,
                        payload,
                        httpStatus,
                        "Interrupted during retry",
                        attemptNumber,
                        duration,
                        true);
            }

            return deliverWithRetry(subscription, payload, nextAttempt);
        }

        return createFailedDelivery(
                subscription, payload, httpStatus, errorMessage, attemptNumber, duration, true);
    }

    private WebhookDelivery createFailedDelivery(
            WebhookSubscription subscription,
            WebhookPayload payload,
            Integer httpStatus,
            String errorMessage,
            int attemptNumber,
            Duration duration,
            boolean permanent) {
        WebhookDelivery delivery;
        if (permanent) {
            delivery =
                    WebhookDelivery.failedPermanent(
                            subscription.id(),
                            payload.id(),
                            subscription.url(),
                            httpStatus,
                            errorMessage,
                            attemptNumber,
                            duration);
        } else {
            delivery =
                    WebhookDelivery.failedWillRetry(
                            subscription.id(),
                            payload.id(),
                            subscription.url(),
                            httpStatus,
                            errorMessage,
                            attemptNumber,
                            duration);
        }
        notifyDelivery(delivery);
        return delivery;
    }

    private long calculateBackoff(int attemptNumber) {
        // Exponential backoff: baseDelay * 2^(attempt-1), capped at maxDelay
        long delay = config.retryBaseDelay().toMillis() * (1L << (attemptNumber - 1));
        return Math.min(delay, config.retryMaxDelay().toMillis());
    }

    private void notifyDelivery(WebhookDelivery delivery) {
        if (deliveryCallback != null) {
            try {
                deliveryCallback.accept(delivery);
            } catch (Exception e) {
                log.error("Error in delivery callback", e);
            }
        }
    }

    /** Configuration for the webhook service. */
    public record WebhookConfig(
            Duration connectTimeout,
            Duration requestTimeout,
            int maxRetries,
            Duration retryBaseDelay,
            Duration retryMaxDelay) {

        /** Creates default configuration. */
        public static WebhookConfig defaults() {
            return new WebhookConfig(
                    Duration.ofSeconds(5), // connectTimeout
                    Duration.ofSeconds(30), // requestTimeout
                    3, // maxRetries
                    Duration.ofSeconds(1), // retryBaseDelay
                    Duration.ofMinutes(5) // retryMaxDelay
                    );
        }

        /** Builder for creating custom configuration. */
        public static Builder builder() {
            return new Builder();
        }

        /** Builder for WebhookConfig. */
        public static class Builder {
            private Duration connectTimeout = Duration.ofSeconds(5);
            private Duration requestTimeout = Duration.ofSeconds(30);
            private int maxRetries = 3;
            private Duration retryBaseDelay = Duration.ofSeconds(1);
            private Duration retryMaxDelay = Duration.ofMinutes(5);

            public Builder connectTimeout(Duration connectTimeout) {
                this.connectTimeout = connectTimeout;
                return this;
            }

            public Builder requestTimeout(Duration requestTimeout) {
                this.requestTimeout = requestTimeout;
                return this;
            }

            public Builder maxRetries(int maxRetries) {
                this.maxRetries = maxRetries;
                return this;
            }

            public Builder retryBaseDelay(Duration retryBaseDelay) {
                this.retryBaseDelay = retryBaseDelay;
                return this;
            }

            public Builder retryMaxDelay(Duration retryMaxDelay) {
                this.retryMaxDelay = retryMaxDelay;
                return this;
            }

            public WebhookConfig build() {
                return new WebhookConfig(
                        connectTimeout, requestTimeout, maxRetries, retryBaseDelay, retryMaxDelay);
            }
        }
    }
}
