package com.jnzader.apigen.core.infrastructure.sse;

import com.jnzader.apigen.core.domain.event.DomainEvent;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Publisher that listens to domain events and sends them via SSE.
 *
 * <p>Converts DDD pattern domain events to SSE events for connected clients.
 *
 * <p>Usage example:
 *
 * <pre>
 * // In your domain service
 * {@literal @}Autowired
 * private ApplicationEventPublisher eventPublisher;
 *
 * public void createOrder(Order order) {
 *     // ... business logic ...
 *     eventPublisher.publishEvent(new OrderCreatedEvent(order));
 * }
 *
 * // SseEventPublisher automatically listens and sends via SSE
 * </pre>
 *
 * <p>The SSE topic is determined from the event name: - OrderCreatedEvent -> topic "orders" -
 * UserRegisteredEvent -> topic "users"
 *
 * <p>Clients must be subscribed to the appropriate topic to receive events.
 */
@Component
public class SseEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(SseEventPublisher.class);

    private final SseEmitterService sseEmitterService;

    public SseEventPublisher(SseEmitterService sseEmitterService) {
        this.sseEmitterService = sseEmitterService;
    }

    /**
     * Listens to domain events and publishes them via SSE.
     *
     * <p>Processing is asynchronous to not block the main flow.
     *
     * @param event Domain event to publish
     */
    @Async
    @EventListener
    public void handleDomainEvent(DomainEvent event) {
        String topic = resolveTopicFromEvent(event);
        String eventName = resolveEventName(event);

        log.debug("Publishing event {} to SSE topic: {}", eventName, topic);

        sseEmitterService.broadcast(
                topic,
                eventName,
                Map.of(
                        "eventType", event.getClass().getSimpleName(),
                        "timestamp", event.occurredOn().toString(),
                        "data", event));
    }

    /**
     * Publishes a custom event to a specific topic.
     *
     * @param topic Target topic
     * @param eventName Event name
     * @param data Event data
     */
    public void publish(String topic, String eventName, Object data) {
        log.debug("Publishing event {} to SSE topic: {}", eventName, topic);
        sseEmitterService.broadcast(topic, eventName, data);
    }

    /**
     * Publishes an event to a specific client.
     *
     * @param clientId Target client ID
     * @param eventName Event name
     * @param data Event data
     * @return true if the send was successful
     */
    public boolean publishToClient(String clientId, String eventName, Object data) {
        return sseEmitterService.sendToClient(clientId, eventName, data);
    }

    /**
     * Resolves the SSE topic from the event.
     *
     * <p>Convention: EntityNameEvent -> "entitynames" (plural, lowercase)
     */
    private String resolveTopicFromEvent(DomainEvent event) {
        String className = event.getClass().getSimpleName();

        // Remove common event suffixes
        String baseName =
                className
                        .replace("CreatedEvent", "")
                        .replace("UpdatedEvent", "")
                        .replace("DeletedEvent", "")
                        .replace("Event", "");

        // Convert to lowercase and simple pluralize
        return baseName.toLowerCase() + "s";
    }

    /**
     * Resolves the event name for the SSE type.
     *
     * <p>Convention: OrderCreatedEvent -> "order.created"
     */
    private String resolveEventName(DomainEvent event) {
        String className = event.getClass().getSimpleName().replace("Event", "");

        // Convert CamelCase to dot.notation
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < className.length(); i++) {
            char c = className.charAt(i);
            if (Character.isUpperCase(c) && i > 0) {
                result.append('.');
            }
            result.append(Character.toLowerCase(c));
        }

        return result.toString();
    }
}
