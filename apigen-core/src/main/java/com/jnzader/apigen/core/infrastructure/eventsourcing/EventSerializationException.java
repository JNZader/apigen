package com.jnzader.apigen.core.infrastructure.eventsourcing;

/**
 * Exception thrown when event serialization or deserialization fails.
 *
 * <p>This can occur due to:
 *
 * <ul>
 *   <li>Invalid JSON format
 *   <li>Unknown event type during deserialization
 *   <li>Missing required fields
 *   <li>Type mismatches
 * </ul>
 */
public class EventSerializationException extends RuntimeException {

    public EventSerializationException(String message) {
        super(message);
    }

    public EventSerializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
