package com.jnzader.apigen.core.infrastructure.eventsourcing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("EventSerializer Tests")
class EventSerializerTest {

    private EventSerializer serializer;

    @BeforeEach
    void setUp() {
        serializer = new EventSerializer();
    }

    @Nested
    @DisplayName("Event Serialization")
    class EventSerializationTests {

        @Test
        @DisplayName("should serialize domain event to JSON")
        void shouldSerializeDomainEventToJson() {
            TestEvent event =
                    new TestEvent(
                            "agg-1",
                            "TestCreated",
                            Instant.parse("2024-01-15T10:30:00Z"),
                            "value1");

            String json = serializer.serialize(event);

            assertThat(json).contains("\"aggregateId\":\"agg-1\"");
            assertThat(json).contains("\"eventType\":\"TestCreated\"");
            assertThat(json).contains("\"data\":\"value1\"");
        }

        @Test
        @DisplayName("should deserialize JSON to domain event")
        void shouldDeserializeJsonToDomainEvent() {
            serializer.registerEventType("TestCreated", TestEvent.class);
            String json =
                    "{\"aggregateId\":\"agg-1\",\"eventType\":\"TestCreated\",\"occurredAt\":\"2024-01-15T10:30:00Z\",\"data\":\"value1\"}";

            DomainEvent event = serializer.deserialize(json, "TestCreated");

            assertThat(event).isInstanceOf(TestEvent.class);
            assertThat(event.getAggregateId()).isEqualTo("agg-1");
            assertThat(((TestEvent) event).getData()).isEqualTo("value1");
        }

        @Test
        @DisplayName("should deserialize to specific class")
        void shouldDeserializeToSpecificClass() {
            String json =
                    "{\"aggregateId\":\"agg-1\",\"eventType\":\"TestCreated\",\"occurredAt\":\"2024-01-15T10:30:00Z\",\"data\":\"value1\"}";

            TestEvent event = serializer.deserialize(json, TestEvent.class);

            assertThat(event.getAggregateId()).isEqualTo("agg-1");
            assertThat(event.getData()).isEqualTo("value1");
        }

        @Test
        @DisplayName("should throw for unknown event type")
        void shouldThrowForUnknownEventType() {
            String json = "{\"aggregateId\":\"agg-1\"}";

            assertThatThrownBy(() -> serializer.deserialize(json, "UnknownType"))
                    .isInstanceOf(EventSerializationException.class)
                    .hasMessageContaining("Unknown event type");
        }

        @Test
        @DisplayName("should handle instant serialization")
        void shouldHandleInstantSerialization() {
            Instant now = Instant.now();
            TestEvent event = new TestEvent("agg-1", "TestCreated", now, "value");

            String json = serializer.serialize(event);
            TestEvent deserialized = serializer.deserialize(json, TestEvent.class);

            assertThat(deserialized.getOccurredAt()).isEqualTo(now);
        }
    }

    @Nested
    @DisplayName("Metadata Serialization")
    class MetadataSerializationTests {

        @Test
        @DisplayName("should serialize metadata to JSON")
        void shouldSerializeMetadataToJson() {
            Map<String, String> metadata = Map.of("userId", "user-1", "correlationId", "corr-123");

            String json = serializer.serializeMetadata(metadata);

            assertThat(json).contains("\"userId\":\"user-1\"");
            assertThat(json).contains("\"correlationId\":\"corr-123\"");
        }

        @Test
        @DisplayName("should return null for null or empty metadata")
        void shouldReturnNullForNullOrEmptyMetadata() {
            assertThat(serializer.serializeMetadata(null)).isNull();
            assertThat(serializer.serializeMetadata(Map.of())).isNull();
        }

        @Test
        @DisplayName("should deserialize metadata from JSON")
        void shouldDeserializeMetadataFromJson() {
            String json = "{\"userId\":\"user-1\",\"correlationId\":\"corr-123\"}";

            Map<String, String> metadata = serializer.deserializeMetadata(json);

            assertThat(metadata).containsEntry("userId", "user-1");
            assertThat(metadata).containsEntry("correlationId", "corr-123");
        }

        @Test
        @DisplayName("should return empty map for null or blank JSON")
        void shouldReturnEmptyMapForNullOrBlankJson() {
            assertThat(serializer.deserializeMetadata(null)).isEmpty();
            assertThat(serializer.deserializeMetadata("")).isEmpty();
            assertThat(serializer.deserializeMetadata("   ")).isEmpty();
        }
    }

    @Nested
    @DisplayName("State Serialization")
    class StateSerializationTests {

        @Test
        @DisplayName("should serialize state object")
        void shouldSerializeStateObject() {
            TestState state = new TestState("product-1", "Test Product", 100);

            String json = serializer.serializeState(state);

            assertThat(json).contains("\"id\":\"product-1\"");
            assertThat(json).contains("\"name\":\"Test Product\"");
            assertThat(json).contains("\"quantity\":100");
        }

        @Test
        @DisplayName("should deserialize state object")
        void shouldDeserializeStateObject() {
            String json = "{\"id\":\"product-1\",\"name\":\"Test Product\",\"quantity\":100}";

            TestState state = serializer.deserializeState(json, TestState.class);

            assertThat(state.id()).isEqualTo("product-1");
            assertThat(state.name()).isEqualTo("Test Product");
            assertThat(state.quantity()).isEqualTo(100);
        }
    }

    @Nested
    @DisplayName("Event Type Registration")
    class EventTypeRegistrationTests {

        @Test
        @DisplayName("should check if event type is registered")
        void shouldCheckIfEventTypeIsRegistered() {
            assertThat(serializer.isEventTypeRegistered("TestCreated")).isFalse();

            serializer.registerEventType("TestCreated", TestEvent.class);

            assertThat(serializer.isEventTypeRegistered("TestCreated")).isTrue();
        }

        @Test
        @DisplayName("should register multiple event types")
        void shouldRegisterMultipleEventTypes() {
            serializer.registerEventType("TestCreated", TestEvent.class);
            serializer.registerEventType("TestUpdated", TestEvent.class);

            assertThat(serializer.isEventTypeRegistered("TestCreated")).isTrue();
            assertThat(serializer.isEventTypeRegistered("TestUpdated")).isTrue();
        }
    }

    // Test helpers

    static class TestEvent implements DomainEvent {
        private String aggregateId;
        private String eventType;
        private Instant occurredAt;
        private String data;

        public TestEvent() {}

        public TestEvent(String aggregateId, String eventType, Instant occurredAt, String data) {
            this.aggregateId = aggregateId;
            this.eventType = eventType;
            this.occurredAt = occurredAt;
            this.data = data;
        }

        @Override
        public String getAggregateId() {
            return aggregateId;
        }

        @Override
        public String getEventType() {
            return eventType;
        }

        @Override
        public Instant getOccurredAt() {
            return occurredAt;
        }

        public String getData() {
            return data;
        }
    }

    record TestState(String id, String name, int quantity) {}
}
