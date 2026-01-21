package com.jnzader.apigen.core.infrastructure.sse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@DisplayName("SseEmitterService Tests")
class SseEmitterServiceTest {

    private SseEmitterService sseEmitterService;

    @BeforeEach
    void setUp() {
        sseEmitterService = new SseEmitterService();
    }

    @Nested
    @DisplayName("Subscribe Tests")
    class SubscribeTests {

        @Test
        @DisplayName("should subscribe to topic and return emitter")
        void shouldSubscribeToTopicAndReturnEmitter() {
            SseEmitter emitter = sseEmitterService.subscribe("orders");

            assertThat(emitter).isNotNull();
            assertThat(sseEmitterService.getSubscriberCount("orders")).isEqualTo(1);
            assertThat(sseEmitterService.getTotalClientCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("should subscribe with custom client ID")
        void shouldSubscribeWithCustomClientId() {
            SseEmitter emitter = sseEmitterService.subscribe("orders", "client-123");

            assertThat(emitter).isNotNull();
            assertThat(sseEmitterService.getSubscriberCount("orders")).isEqualTo(1);
        }

        @Test
        @DisplayName("should generate client ID when not provided")
        void shouldGenerateClientIdWhenNotProvided() {
            SseEmitter emitter = sseEmitterService.subscribe("orders", null);

            assertThat(emitter).isNotNull();
            assertThat(sseEmitterService.getTotalClientCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("should support multiple subscribers per topic")
        void shouldSupportMultipleSubscribersPerTopic() {
            sseEmitterService.subscribe("orders", "client-1");
            sseEmitterService.subscribe("orders", "client-2");
            sseEmitterService.subscribe("orders", "client-3");

            assertThat(sseEmitterService.getSubscriberCount("orders")).isEqualTo(3);
            assertThat(sseEmitterService.getTotalClientCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("should support multiple topics")
        void shouldSupportMultipleTopics() {
            sseEmitterService.subscribe("orders", "client-1");
            sseEmitterService.subscribe("users", "client-2");
            sseEmitterService.subscribe("notifications", "client-3");

            assertThat(sseEmitterService.getSubscriberCount("orders")).isEqualTo(1);
            assertThat(sseEmitterService.getSubscriberCount("users")).isEqualTo(1);
            assertThat(sseEmitterService.getSubscriberCount("notifications")).isEqualTo(1);
            assertThat(sseEmitterService.getTotalClientCount()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("Broadcast Tests")
    class BroadcastTests {

        @Test
        @DisplayName("should broadcast to all subscribers of a topic")
        void shouldBroadcastToAllSubscribersOfTopic() {
            sseEmitterService.subscribe("orders", "client-1");
            sseEmitterService.subscribe("orders", "client-2");

            // Broadcasting doesn't throw and clients stay connected
            sseEmitterService.broadcast("orders", Map.of("orderId", 123));

            assertThat(sseEmitterService.getSubscriberCount("orders")).isEqualTo(2);
        }

        @Test
        @DisplayName("should broadcast with event name")
        void shouldBroadcastWithEventName() {
            sseEmitterService.subscribe("orders", "client-1");

            sseEmitterService.broadcast("orders", "order.created", Map.of("orderId", 123));

            assertThat(sseEmitterService.getSubscriberCount("orders")).isEqualTo(1);
        }

        @Test
        @DisplayName("should handle broadcast to non-existent topic")
        void shouldHandleBroadcastToNonExistentTopic() {
            // Should not throw
            sseEmitterService.broadcast("non-existent-topic", Map.of("data", "value"));

            assertThat(sseEmitterService.getSubscriberCount("non-existent-topic")).isZero();
        }

        @Test
        @DisplayName("should handle broadcast to empty topic")
        void shouldHandleBroadcastToEmptyTopic() {
            // Subscribe and then disconnect
            sseEmitterService.subscribe("orders", "client-1");
            sseEmitterService.disconnectClient("client-1");

            // Should not throw
            sseEmitterService.broadcast("orders", Map.of("data", "value"));

            assertThat(sseEmitterService.getSubscriberCount("orders")).isZero();
        }
    }

    @Nested
    @DisplayName("SendToClient Tests")
    class SendToClientTests {

        @Test
        @DisplayName("should return false when client not found")
        void shouldReturnFalseWhenClientNotFound() {
            boolean result = sseEmitterService.sendToClient("non-existent", "event", "data");

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("Heartbeat Tests")
    class HeartbeatTests {

        @Test
        @DisplayName("should send heartbeat to all clients")
        void shouldSendHeartbeatToAllClients() {
            sseEmitterService.subscribe("orders", "client-1");
            sseEmitterService.subscribe("users", "client-2");

            // Should not throw
            sseEmitterService.sendHeartbeatToAll();

            assertThat(sseEmitterService.getTotalClientCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("should handle heartbeat with no clients")
        void shouldHandleHeartbeatWithNoClients() {
            // Should not throw
            sseEmitterService.sendHeartbeatToAll();

            assertThat(sseEmitterService.getTotalClientCount()).isZero();
        }
    }

    @Nested
    @DisplayName("Disconnect Tests")
    class DisconnectTests {

        @Test
        @DisplayName("should disconnect specific client")
        void shouldDisconnectSpecificClient() {
            sseEmitterService.subscribe("orders", "client-1");
            sseEmitterService.subscribe("orders", "client-2");

            sseEmitterService.disconnectClient("client-1");

            assertThat(sseEmitterService.getSubscriberCount("orders")).isEqualTo(1);
            assertThat(sseEmitterService.getTotalClientCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("should handle disconnect of non-existent client")
        void shouldHandleDisconnectOfNonExistentClient() {
            // Should not throw
            sseEmitterService.disconnectClient("non-existent");

            assertThat(sseEmitterService.getTotalClientCount()).isZero();
        }

        @Test
        @DisplayName("should disconnect all clients of a topic")
        void shouldDisconnectAllClientsOfTopic() {
            sseEmitterService.subscribe("orders", "client-1");
            sseEmitterService.subscribe("orders", "client-2");
            sseEmitterService.subscribe("users", "client-3");

            sseEmitterService.disconnectTopic("orders");

            assertThat(sseEmitterService.getSubscriberCount("orders")).isZero();
            assertThat(sseEmitterService.getSubscriberCount("users")).isEqualTo(1);
            assertThat(sseEmitterService.getTotalClientCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("should handle disconnect of non-existent topic")
        void shouldHandleDisconnectOfNonExistentTopic() {
            // Should not throw
            sseEmitterService.disconnectTopic("non-existent");

            assertThat(sseEmitterService.getTotalClientCount()).isZero();
        }
    }

    @Nested
    @DisplayName("Statistics Tests")
    class StatisticsTests {

        @Test
        @DisplayName("should return correct subscriber count")
        void shouldReturnCorrectSubscriberCount() {
            assertThat(sseEmitterService.getSubscriberCount("orders")).isZero();

            sseEmitterService.subscribe("orders", "client-1");
            assertThat(sseEmitterService.getSubscriberCount("orders")).isEqualTo(1);

            sseEmitterService.subscribe("orders", "client-2");
            assertThat(sseEmitterService.getSubscriberCount("orders")).isEqualTo(2);
        }

        @Test
        @DisplayName("should return correct total client count")
        void shouldReturnCorrectTotalClientCount() {
            assertThat(sseEmitterService.getTotalClientCount()).isZero();

            sseEmitterService.subscribe("orders", "client-1");
            sseEmitterService.subscribe("users", "client-2");

            assertThat(sseEmitterService.getTotalClientCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("should return zero for non-existent topic")
        void shouldReturnZeroForNonExistentTopic() {
            assertThat(sseEmitterService.getSubscriberCount("non-existent")).isZero();
        }
    }
}
