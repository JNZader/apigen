package com.jnzader.apigen.core.infrastructure.eventsourcing;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Serializer for domain events using JSON.
 *
 * <p>This serializer handles conversion between {@link DomainEvent} objects and their JSON
 * representation for storage in the event store.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * EventSerializer serializer = new EventSerializer();
 *
 * // Register event types
 * serializer.registerEventType("ProductCreated", ProductCreatedEvent.class);
 *
 * // Serialize
 * String json = serializer.serialize(event);
 *
 * // Deserialize
 * DomainEvent event = serializer.deserialize(json, "ProductCreated");
 * }</pre>
 */
public class EventSerializer {

    private final ObjectMapper objectMapper;
    private final Map<String, Class<? extends DomainEvent>> eventTypes;

    public EventSerializer() {
        this.objectMapper = createObjectMapper();
        this.eventTypes = new ConcurrentHashMap<>();
    }

    public EventSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.eventTypes = new ConcurrentHashMap<>();
    }

    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.configure(
                com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }

    /**
     * Registers an event type for deserialization.
     *
     * @param eventType the event type name
     * @param eventClass the event class
     */
    public void registerEventType(String eventType, Class<? extends DomainEvent> eventClass) {
        eventTypes.put(eventType, eventClass);
    }

    /**
     * Serializes a domain event to JSON.
     *
     * @param event the event to serialize
     * @return JSON string representation
     * @throws EventSerializationException if serialization fails
     */
    public String serialize(DomainEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new EventSerializationException("Failed to serialize event: " + event.getEventType(), e);
        }
    }

    /**
     * Deserializes a JSON string to a domain event.
     *
     * @param json the JSON string
     * @param eventType the event type for determining the target class
     * @return the deserialized event
     * @throws EventSerializationException if deserialization fails or type is not registered
     */
    public DomainEvent deserialize(String json, String eventType) {
        Class<? extends DomainEvent> eventClass = eventTypes.get(eventType);
        if (eventClass == null) {
            throw new EventSerializationException("Unknown event type: " + eventType);
        }

        try {
            return objectMapper.readValue(json, eventClass);
        } catch (JsonProcessingException e) {
            throw new EventSerializationException("Failed to deserialize event: " + eventType, e);
        }
    }

    /**
     * Deserializes a JSON string to a specific event class.
     *
     * @param json the JSON string
     * @param eventClass the target class
     * @param <T> the event type
     * @return the deserialized event
     * @throws EventSerializationException if deserialization fails
     */
    public <T extends DomainEvent> T deserialize(String json, Class<T> eventClass) {
        try {
            return objectMapper.readValue(json, eventClass);
        } catch (JsonProcessingException e) {
            throw new EventSerializationException("Failed to deserialize event to " + eventClass.getSimpleName(), e);
        }
    }

    /**
     * Serializes metadata to JSON.
     *
     * @param metadata the metadata map
     * @return JSON string, or null if metadata is null or empty
     */
    public String serializeMetadata(Map<String, String> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            throw new EventSerializationException("Failed to serialize metadata", e);
        }
    }

    /**
     * Deserializes metadata from JSON.
     *
     * @param json the JSON string
     * @return metadata map, or empty map if json is null or empty
     */
    @SuppressWarnings("unchecked")
    public Map<String, String> deserializeMetadata(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (JsonProcessingException e) {
            throw new EventSerializationException("Failed to deserialize metadata", e);
        }
    }

    /**
     * Serializes an aggregate state for snapshot storage.
     *
     * @param state the aggregate state object
     * @return JSON string representation
     */
    public String serializeState(Object state) {
        try {
            return objectMapper.writeValueAsString(state);
        } catch (JsonProcessingException e) {
            throw new EventSerializationException("Failed to serialize aggregate state", e);
        }
    }

    /**
     * Deserializes an aggregate state from snapshot storage.
     *
     * @param json the JSON string
     * @param stateClass the target class
     * @param <T> the state type
     * @return the deserialized state
     */
    public <T> T deserializeState(String json, Class<T> stateClass) {
        try {
            return objectMapper.readValue(json, stateClass);
        } catch (JsonProcessingException e) {
            throw new EventSerializationException("Failed to deserialize aggregate state", e);
        }
    }

    /**
     * Checks if an event type is registered.
     *
     * @param eventType the event type name
     * @return true if registered
     */
    public boolean isEventTypeRegistered(String eventType) {
        return eventTypes.containsKey(eventType);
    }

    /**
     * Returns the underlying ObjectMapper for advanced customization.
     *
     * @return the ObjectMapper instance
     */
    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }
}
