package com.jnzader.apigen.core.infrastructure.sse;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Service for managing SSE (Server-Sent Events) connections.
 *
 * <p>Features: - Management of multiple clients per topic - Broadcast to all subscribers of a topic
 * - Send to specific clients by ID - Automatic cleanup of dead connections - Thread-safe for
 * concurrent use
 *
 * <p>Typical usage:
 *
 * <pre>
 * // In a controller
 * {@literal @}GetMapping(value = "/events/{topic}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
 * public SseEmitter subscribe({@literal @}PathVariable String topic) {
 *     return sseEmitterService.subscribe(topic);
 * }
 *
 * // In a service to send events
 * sseEmitterService.broadcast("orders", new OrderEvent(order));
 * </pre>
 */
@Service
public class SseEmitterService {

    private static final Logger log = LoggerFactory.getLogger(SseEmitterService.class);

    /**
     * Timeout for SSE connections (30 minutes by default). Higher value than typical to avoid
     * frequent reconnections.
     */
    private static final long DEFAULT_TIMEOUT = 30 * 60 * 1000L;

    /** Map of topics to their subscribers. Each topic can have multiple connected clients. */
    private final Map<String, Set<SseClient>> topicSubscribers = new ConcurrentHashMap<>();

    /** Map of client ID to its emitter (for direct send). */
    private final Map<String, SseClient> clientsById = new ConcurrentHashMap<>();

    /**
     * Subscribes a new client to a topic.
     *
     * @param topic Topic to subscribe to
     * @return SseEmitter for the connection
     */
    public SseEmitter subscribe(String topic) {
        return subscribe(topic, null);
    }

    /**
     * Subscribes a new client to a topic with a specific ID.
     *
     * @param topic Topic to subscribe to
     * @param clientId Unique client ID (optional)
     * @return SseEmitter for the connection
     */
    public SseEmitter subscribe(String topic, String clientId) {
        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT);
        String id = clientId != null ? clientId : java.util.UUID.randomUUID().toString();

        SseClient client = new SseClient(id, topic, emitter);

        // Add to topic subscribers
        topicSubscribers.computeIfAbsent(topic, _ -> new CopyOnWriteArraySet<>()).add(client);

        // Add to clients by ID map
        clientsById.put(id, client);

        // Configure callbacks for cleanup
        emitter.onCompletion(() -> removeClient(client));
        emitter.onTimeout(
                () -> {
                    log.debug("SSE timeout for client {} in topic {}", id, topic);
                    removeClient(client);
                });
        emitter.onError(
                ex -> {
                    log.debug(
                            "SSE error for client {} in topic {}: {}", id, topic, ex.getMessage());
                    removeClient(client);
                });

        // Send initial connection event
        try {
            emitter.send(
                    SseEmitter.event()
                            .name("connected")
                            .id(id)
                            .data(
                                    Map.of(
                                            "clientId", id,
                                            "topic", topic,
                                            "timestamp", java.time.Instant.now().toString())));
            log.debug("Client {} connected to topic {}", id, topic);
        } catch (IOException e) {
            log.warn("Error sending connection event to client {}: {}", id, e.getMessage());
            removeClient(client);
        }

        return emitter;
    }

    /**
     * Sends an event to all subscribers of a topic.
     *
     * @param topic Target topic
     * @param event Event data
     */
    public void broadcast(String topic, Object event) {
        broadcast(topic, "message", event);
    }

    /**
     * Sends a named event to all subscribers of a topic.
     *
     * @param topic Target topic
     * @param eventName Event name (for client-side filtering)
     * @param event Event data
     */
    public void broadcast(String topic, String eventName, Object event) {
        Set<SseClient> subscribers = topicSubscribers.get(topic);
        if (subscribers == null || subscribers.isEmpty()) {
            log.trace("No subscribers for topic {}", topic);
            return;
        }

        log.debug(
                "Sending event '{}' to {} subscribers of topic {}",
                eventName,
                subscribers.size(),
                topic);

        String eventId = java.util.UUID.randomUUID().toString();

        for (SseClient client : subscribers) {
            try {
                client.emitter().send(SseEmitter.event().name(eventName).id(eventId).data(event));
            } catch (IOException e) {
                log.debug("Error sending to client {}: {}", client.id(), e.getMessage());
                removeClient(client);
            }
        }
    }

    /**
     * Sends an event to a specific client by ID.
     *
     * @param clientId Target client ID
     * @param eventName Event name
     * @param event Event data
     * @return true if the send was successful
     */
    public boolean sendToClient(String clientId, String eventName, Object event) {
        SseClient client = clientsById.get(clientId);
        if (client == null) {
            log.debug("Client {} not found", clientId);
            return false;
        }

        try {
            client.emitter()
                    .send(
                            SseEmitter.event()
                                    .name(eventName)
                                    .id(java.util.UUID.randomUUID().toString())
                                    .data(event));
            return true;
        } catch (IOException e) {
            log.debug("Error sending to client {}: {}", clientId, e.getMessage());
            removeClient(client);
            return false;
        }
    }

    /**
     * Sends a heartbeat to all connected clients. Useful for keeping connections alive and
     * detecting dead clients.
     *
     * <p>Uses parallel processing to not block when there are many connected clients.
     */
    public void sendHeartbeatToAll() {
        long timestamp = System.currentTimeMillis();
        var heartbeatEvent = Map.of("timestamp", timestamp);

        // Collect all clients first to use parallelStream effectively
        topicSubscribers.values().parallelStream()
                .flatMap(Set::stream)
                .forEach(client -> sendHeartbeatToClient(client, heartbeatEvent));
    }

    /**
     * Sends a heartbeat to a specific client.
     *
     * @param client Target client
     * @param heartbeatEvent Heartbeat data
     */
    private void sendHeartbeatToClient(SseClient client, Map<String, Long> heartbeatEvent) {
        try {
            client.emitter().send(SseEmitter.event().name("heartbeat").data(heartbeatEvent));
        } catch (IOException _) {
            removeClient(client);
        }
    }

    /** Gets the number of subscribers for a topic. */
    public int getSubscriberCount(String topic) {
        Set<SseClient> subscribers = topicSubscribers.get(topic);
        return subscribers != null ? subscribers.size() : 0;
    }

    /** Gets the total number of connected clients. */
    public int getTotalClientCount() {
        return clientsById.size();
    }

    /** Disconnects a specific client. */
    public void disconnectClient(String clientId) {
        SseClient client = clientsById.get(clientId);
        if (client != null) {
            client.emitter().complete();
            removeClient(client);
        }
    }

    /** Disconnects all subscribers of a topic. */
    public void disconnectTopic(String topic) {
        Set<SseClient> subscribers = topicSubscribers.remove(topic);
        if (subscribers != null) {
            for (SseClient client : subscribers) {
                client.emitter().complete();
                clientsById.remove(client.id());
            }
        }
    }

    /** Removes a client from all data structures. */
    private void removeClient(SseClient client) {
        clientsById.remove(client.id());
        Set<SseClient> subscribers = topicSubscribers.get(client.topic());
        if (subscribers != null) {
            subscribers.remove(client);
            if (subscribers.isEmpty()) {
                topicSubscribers.remove(client.topic());
            }
        }
        log.debug("Client {} removed from topic {}", client.id(), client.topic());
    }

    /** Internal record representing an SSE client. */
    private record SseClient(String id, String topic, SseEmitter emitter) {}
}
