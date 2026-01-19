package com.jnzader.apigen.core.infrastructure.webhook;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("WebhookService Tests")
class WebhookServiceTest {

    private WebhookService webhookService;
    private InMemoryWebhookSubscriptionRepository repository;
    private HttpServer mockServer;
    private int serverPort;

    @BeforeEach
    void setUp() throws IOException {
        repository = new InMemoryWebhookSubscriptionRepository();

        WebhookService.WebhookConfig config =
                WebhookService.WebhookConfig.builder()
                        .connectTimeout(Duration.ofSeconds(2))
                        .requestTimeout(Duration.ofSeconds(10))
                        .maxRetries(2)
                        .retryBaseDelay(Duration.ofMillis(50))
                        .retryMaxDelay(Duration.ofMillis(500))
                        .build();

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        webhookService = new WebhookService(repository, objectMapper, config);

        // Start a simple HTTP server for testing
        mockServer = HttpServer.create(new InetSocketAddress(0), 0);
        serverPort = mockServer.getAddress().getPort();
        mockServer.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(4));
        mockServer.start();
    }

    @AfterEach
    void tearDown() {
        if (mockServer != null) {
            mockServer.stop(0);
        }
        repository.clear();
    }

    @Nested
    @DisplayName("Dispatch")
    class DispatchTests {

        @Test
        @DisplayName("should dispatch to subscribed endpoints")
        void shouldDispatchToSubscribedEndpoints() {
            AtomicInteger requestCount = new AtomicInteger(0);

            mockServer.createContext(
                    "/webhook",
                    exchange -> {
                        requestCount.incrementAndGet();
                        exchange.sendResponseHeaders(200, 0);
                        exchange.close();
                    });

            WebhookSubscription subscription =
                    WebhookSubscription.builder()
                            .name("Test Subscription")
                            .url("http://localhost:" + serverPort + "/webhook")
                            .secret("test-secret")
                            .events(Set.of(WebhookEvent.ENTITY_CREATED))
                            .build();
            repository.save(subscription);

            WebhookPayload payload =
                    WebhookPayload.builder()
                            .event(WebhookEvent.ENTITY_CREATED)
                            .resourceType("User")
                            .resourceId(1L)
                            .data("{\"name\":\"test\"}")
                            .build();

            List<WebhookDelivery> deliveries =
                    webhookService.dispatchAndWait(WebhookEvent.ENTITY_CREATED, payload);

            assertThat(deliveries).hasSize(1);
            assertThat(deliveries.get(0).status())
                    .isEqualTo(WebhookDelivery.DeliveryStatus.SUCCESS);
            assertThat(requestCount.get()).isEqualTo(1);
        }

        @Test
        @DisplayName("should not dispatch to inactive subscriptions")
        void shouldNotDispatchToInactiveSubscriptions() {
            WebhookSubscription subscription =
                    WebhookSubscription.builder()
                            .name("Inactive Subscription")
                            .url("http://localhost:" + serverPort + "/webhook")
                            .secret("test-secret")
                            .events(Set.of(WebhookEvent.ENTITY_CREATED))
                            .active(false)
                            .build();
            repository.save(subscription);

            WebhookPayload payload =
                    WebhookPayload.builder().event(WebhookEvent.ENTITY_CREATED).build();

            List<WebhookDelivery> deliveries =
                    webhookService.dispatchAndWait(WebhookEvent.ENTITY_CREATED, payload);

            assertThat(deliveries).isEmpty();
        }

        @Test
        @DisplayName("should not dispatch to unsubscribed events")
        void shouldNotDispatchToUnsubscribedEvents() {
            WebhookSubscription subscription =
                    WebhookSubscription.builder()
                            .name("Test Subscription")
                            .url("http://localhost:" + serverPort + "/webhook")
                            .secret("test-secret")
                            .events(Set.of(WebhookEvent.ENTITY_DELETED))
                            .build();
            repository.save(subscription);

            WebhookPayload payload =
                    WebhookPayload.builder().event(WebhookEvent.ENTITY_CREATED).build();

            List<WebhookDelivery> deliveries =
                    webhookService.dispatchAndWait(WebhookEvent.ENTITY_CREATED, payload);

            assertThat(deliveries).isEmpty();
        }

        @Test
        @DisplayName("should dispatch to multiple subscribers")
        void shouldDispatchToMultipleSubscribers() {
            AtomicInteger requestCount1 = new AtomicInteger(0);
            AtomicInteger requestCount2 = new AtomicInteger(0);

            mockServer.createContext(
                    "/webhook1",
                    exchange -> {
                        requestCount1.incrementAndGet();
                        exchange.sendResponseHeaders(200, 0);
                        exchange.close();
                    });
            mockServer.createContext(
                    "/webhook2",
                    exchange -> {
                        requestCount2.incrementAndGet();
                        exchange.sendResponseHeaders(200, 0);
                        exchange.close();
                    });

            repository.save(
                    WebhookSubscription.builder()
                            .name("Subscription 1")
                            .url("http://localhost:" + serverPort + "/webhook1")
                            .secret("secret1")
                            .events(Set.of(WebhookEvent.ENTITY_CREATED))
                            .build());
            repository.save(
                    WebhookSubscription.builder()
                            .name("Subscription 2")
                            .url("http://localhost:" + serverPort + "/webhook2")
                            .secret("secret2")
                            .events(Set.of(WebhookEvent.ENTITY_CREATED))
                            .build());

            WebhookPayload payload =
                    WebhookPayload.builder().event(WebhookEvent.ENTITY_CREATED).build();

            List<WebhookDelivery> deliveries =
                    webhookService.dispatchAndWait(WebhookEvent.ENTITY_CREATED, payload);

            assertThat(deliveries).hasSize(2);
            assertThat(deliveries)
                    .allMatch(d -> d.status() == WebhookDelivery.DeliveryStatus.SUCCESS);
            assertThat(requestCount1.get()).isEqualTo(1);
            assertThat(requestCount2.get()).isEqualTo(1);
        }

        @Test
        @DisplayName("should include proper headers")
        void shouldIncludeProperHeaders() {
            AtomicReference<String> webhookId = new AtomicReference<>();
            AtomicReference<String> webhookSignature = new AtomicReference<>();
            AtomicReference<String> webhookEvent = new AtomicReference<>();
            AtomicReference<String> contentType = new AtomicReference<>();

            mockServer.createContext(
                    "/webhook",
                    exchange -> {
                        webhookId.set(exchange.getRequestHeaders().getFirst("X-Webhook-Id"));
                        webhookSignature.set(
                                exchange.getRequestHeaders().getFirst("X-Webhook-Signature"));
                        webhookEvent.set(exchange.getRequestHeaders().getFirst("X-Webhook-Event"));
                        contentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
                        exchange.sendResponseHeaders(200, 0);
                        exchange.close();
                    });

            WebhookSubscription subscription =
                    WebhookSubscription.builder()
                            .name("Test")
                            .url("http://localhost:" + serverPort + "/webhook")
                            .secret("test-secret")
                            .events(Set.of(WebhookEvent.ENTITY_CREATED))
                            .build();
            repository.save(subscription);

            WebhookPayload payload =
                    WebhookPayload.builder().event(WebhookEvent.ENTITY_CREATED).build();

            webhookService.dispatchAndWait(WebhookEvent.ENTITY_CREATED, payload);

            assertThat(webhookId.get()).isNotNull().isNotBlank();
            assertThat(webhookSignature.get()).isNotNull().startsWith("t=");
            assertThat(webhookEvent.get()).isEqualTo("entity.created");
            assertThat(contentType.get()).isEqualTo("application/json");
        }
    }

    @Nested
    @DisplayName("Retry Logic")
    class RetryTests {

        @Test
        @DisplayName("should retry on server error")
        void shouldRetryOnServerError() {
            AtomicInteger attempts = new AtomicInteger(0);

            mockServer.createContext(
                    "/webhook",
                    exchange -> {
                        int attempt = attempts.incrementAndGet();
                        if (attempt < 2) {
                            exchange.sendResponseHeaders(500, 0);
                        } else {
                            exchange.sendResponseHeaders(200, 0);
                        }
                        exchange.close();
                    });

            WebhookSubscription subscription =
                    WebhookSubscription.builder()
                            .name("Test")
                            .url("http://localhost:" + serverPort + "/webhook")
                            .secret("secret")
                            .events(Set.of(WebhookEvent.ENTITY_CREATED))
                            .build();
            repository.save(subscription);

            WebhookPayload payload =
                    WebhookPayload.builder().event(WebhookEvent.ENTITY_CREATED).build();

            List<WebhookDelivery> deliveries =
                    webhookService.dispatchAndWait(WebhookEvent.ENTITY_CREATED, payload);

            assertThat(attempts.get()).isEqualTo(2);
            assertThat(deliveries).hasSize(1);
            assertThat(deliveries.get(0).status())
                    .isEqualTo(WebhookDelivery.DeliveryStatus.SUCCESS);
            assertThat(deliveries.get(0).attemptNumber()).isEqualTo(2);
        }

        @Test
        @DisplayName("should fail permanently after max retries")
        void shouldFailPermanentlyAfterMaxRetries() {
            AtomicInteger attempts = new AtomicInteger(0);

            mockServer.createContext(
                    "/webhook",
                    exchange -> {
                        attempts.incrementAndGet();
                        exchange.sendResponseHeaders(500, 0);
                        exchange.close();
                    });

            WebhookSubscription subscription =
                    WebhookSubscription.builder()
                            .name("Test")
                            .url("http://localhost:" + serverPort + "/webhook")
                            .secret("secret")
                            .events(Set.of(WebhookEvent.ENTITY_CREATED))
                            .build();
            repository.save(subscription);

            WebhookPayload payload =
                    WebhookPayload.builder().event(WebhookEvent.ENTITY_CREATED).build();

            List<WebhookDelivery> deliveries =
                    webhookService.dispatchAndWait(WebhookEvent.ENTITY_CREATED, payload);

            assertThat(attempts.get()).isEqualTo(2);
            assertThat(deliveries).hasSize(1);
            assertThat(deliveries.get(0).status())
                    .isEqualTo(WebhookDelivery.DeliveryStatus.FAILED_PERMANENT);
        }

        @Test
        @DisplayName("should not retry on client error (4xx)")
        void shouldNotRetryOnClientError() {
            AtomicInteger attempts = new AtomicInteger(0);

            mockServer.createContext(
                    "/webhook",
                    exchange -> {
                        attempts.incrementAndGet();
                        exchange.sendResponseHeaders(400, 0);
                        exchange.close();
                    });

            WebhookSubscription subscription =
                    WebhookSubscription.builder()
                            .name("Test")
                            .url("http://localhost:" + serverPort + "/webhook")
                            .secret("secret")
                            .events(Set.of(WebhookEvent.ENTITY_CREATED))
                            .build();
            repository.save(subscription);

            WebhookPayload payload =
                    WebhookPayload.builder().event(WebhookEvent.ENTITY_CREATED).build();

            List<WebhookDelivery> deliveries =
                    webhookService.dispatchAndWait(WebhookEvent.ENTITY_CREATED, payload);

            assertThat(attempts.get()).isEqualTo(1);
            assertThat(deliveries.get(0).status())
                    .isEqualTo(WebhookDelivery.DeliveryStatus.FAILED_PERMANENT);
        }
    }

    @Nested
    @DisplayName("Delivery Callback")
    class DeliveryCallbackTests {

        @Test
        @DisplayName("should invoke callback after delivery")
        void shouldInvokeCallbackAfterDelivery() throws Exception {
            CountDownLatch latch = new CountDownLatch(1);
            List<WebhookDelivery> callbackDeliveries = new CopyOnWriteArrayList<>();

            mockServer.createContext(
                    "/webhook",
                    exchange -> {
                        exchange.sendResponseHeaders(200, 0);
                        exchange.close();
                    });

            webhookService.setDeliveryCallback(
                    delivery -> {
                        callbackDeliveries.add(delivery);
                        latch.countDown();
                    });

            WebhookSubscription subscription =
                    WebhookSubscription.builder()
                            .name("Test")
                            .url("http://localhost:" + serverPort + "/webhook")
                            .secret("secret")
                            .events(Set.of(WebhookEvent.ENTITY_CREATED))
                            .build();
            repository.save(subscription);

            WebhookPayload payload =
                    WebhookPayload.builder().event(WebhookEvent.ENTITY_CREATED).build();

            // Use dispatch (async) + wait on latch
            webhookService.dispatch(WebhookEvent.ENTITY_CREATED, payload);

            assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
            assertThat(callbackDeliveries).hasSize(1);
            assertThat(callbackDeliveries.get(0).status())
                    .isEqualTo(WebhookDelivery.DeliveryStatus.SUCCESS);
        }
    }

    @Nested
    @DisplayName("Ping")
    class PingTests {

        @Test
        @DisplayName("should send ping to subscription")
        void shouldSendPingToSubscription() {
            AtomicReference<String> receivedEvent = new AtomicReference<>();

            mockServer.createContext(
                    "/webhook",
                    exchange -> {
                        receivedEvent.set(exchange.getRequestHeaders().getFirst("X-Webhook-Event"));
                        exchange.sendResponseHeaders(200, 0);
                        exchange.close();
                    });

            WebhookSubscription subscription =
                    WebhookSubscription.builder()
                            .name("Test")
                            .url("http://localhost:" + serverPort + "/webhook")
                            .secret("secret")
                            .events(Set.of(WebhookEvent.ENTITY_CREATED))
                            .build();
            repository.save(subscription);

            CompletableFuture<WebhookDelivery> future = webhookService.sendPing(subscription.id());
            WebhookDelivery delivery = future.join();

            assertThat(receivedEvent.get()).isEqualTo("system.ping");
            assertThat(delivery.status()).isEqualTo(WebhookDelivery.DeliveryStatus.SUCCESS);
        }

        @Test
        @DisplayName("should return error for non-existent subscription")
        void shouldReturnErrorForNonExistentSubscription() {
            CompletableFuture<WebhookDelivery> future = webhookService.sendPing("non-existent-id");
            WebhookDelivery delivery = future.join();

            assertThat(delivery.status())
                    .isEqualTo(WebhookDelivery.DeliveryStatus.FAILED_PERMANENT);
            assertThat(delivery.errorMessage()).contains("not found");
        }
    }
}
