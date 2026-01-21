package com.jnzader.apigen.core.infrastructure.sse;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.jnzader.apigen.core.domain.event.DomainEvent;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("SseEventPublisher Tests")
class SseEventPublisherTest {

    @Mock private SseEmitterService sseEmitterService;

    private SseEventPublisher sseEventPublisher;

    @BeforeEach
    void setUp() {
        sseEventPublisher = new SseEventPublisher(sseEmitterService);
    }

    @Nested
    @DisplayName("HandleDomainEvent Tests")
    class HandleDomainEventTests {

        @Test
        @DisplayName("should broadcast domain event to SSE")
        void shouldBroadcastDomainEventToSse() {
            TestOrderCreatedEvent event = new TestOrderCreatedEvent(123L, LocalDateTime.now());

            sseEventPublisher.handleDomainEvent(event);

            // TestOrderCreatedEvent -> remove "CreatedEvent" -> "TestOrder" -> "testorders"
            verify(sseEmitterService).broadcast(eq("testorders"), eq("test.order.created"), any());
        }

        @Test
        @DisplayName("should resolve topic from event class name")
        void shouldResolveTopicFromEventClassName() {
            TestUserRegisteredEvent event = new TestUserRegisteredEvent(1L, LocalDateTime.now());

            sseEventPublisher.handleDomainEvent(event);

            // TestUserRegisteredEvent -> remove "Event" -> "TestUserRegistered" ->
            // "testuserregistereds"
            verify(sseEmitterService).broadcast(eq("testuserregistereds"), any(), any());
        }

        @Test
        @DisplayName("should resolve event name from class name")
        void shouldResolveEventNameFromClassName() {
            TestOrderUpdatedEvent event = new TestOrderUpdatedEvent(123L, LocalDateTime.now());

            sseEventPublisher.handleDomainEvent(event);

            // TestOrderUpdatedEvent -> remove "UpdatedEvent" -> "TestOrder" -> "testorders"
            verify(sseEmitterService).broadcast(eq("testorders"), eq("test.order.updated"), any());
        }
    }

    @Nested
    @DisplayName("Publish Tests")
    class PublishTests {

        @Test
        @DisplayName("should publish to specific topic")
        void shouldPublishToSpecificTopic() {
            sseEventPublisher.publish("orders", "order.created", "order data");

            verify(sseEmitterService).broadcast("orders", "order.created", "order data");
        }
    }

    @Nested
    @DisplayName("PublishToClient Tests")
    class PublishToClientTests {

        @Test
        @DisplayName("should publish to specific client")
        void shouldPublishToSpecificClient() {
            when(sseEmitterService.sendToClient("client-1", "notification", "data"))
                    .thenReturn(true);

            boolean result = sseEventPublisher.publishToClient("client-1", "notification", "data");

            verify(sseEmitterService).sendToClient("client-1", "notification", "data");
            assert result;
        }

        @Test
        @DisplayName("should return false when client not found")
        void shouldReturnFalseWhenClientNotFound() {
            when(sseEmitterService.sendToClient("unknown", "event", "data")).thenReturn(false);

            boolean result = sseEventPublisher.publishToClient("unknown", "event", "data");

            assert !result;
        }
    }

    // Test event classes

    record TestOrderCreatedEvent(Long orderId, LocalDateTime occurredOn) implements DomainEvent {}

    record TestUserRegisteredEvent(Long userId, LocalDateTime occurredOn) implements DomainEvent {}

    record TestOrderUpdatedEvent(Long orderId, LocalDateTime occurredOn) implements DomainEvent {}
}
