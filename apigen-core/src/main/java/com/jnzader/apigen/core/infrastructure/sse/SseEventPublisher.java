package com.jnzader.apigen.core.infrastructure.sse;

import com.jnzader.apigen.core.domain.event.DomainEvent;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Publisher que escucha eventos de dominio y los envía via SSE.
 *
 * <p>Convierte eventos de dominio del patrón DDD en eventos SSE para clientes conectados.
 *
 * <p>Ejemplo de uso:
 *
 * <pre>
 * // En tu servicio de dominio
 * {@literal @}Autowired
 * private ApplicationEventPublisher eventPublisher;
 *
 * public void createOrder(Order order) {
 *     // ... lógica de negocio ...
 *     eventPublisher.publishEvent(new OrderCreatedEvent(order));
 * }
 *
 * // El SseEventPublisher escucha automáticamente y envía via SSE
 * </pre>
 *
 * <p>El tópico SSE se determina del nombre del evento: - OrderCreatedEvent → tópico "orders" -
 * UserRegisteredEvent → tópico "users"
 *
 * <p>Los clientes deben estar suscritos al tópico apropiado para recibir eventos.
 */
@Component
public class SseEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(SseEventPublisher.class);

    private final SseEmitterService sseEmitterService;

    public SseEventPublisher(SseEmitterService sseEmitterService) {
        this.sseEmitterService = sseEmitterService;
    }

    /**
     * Escucha eventos de dominio y los publica via SSE.
     *
     * <p>El procesamiento es asíncrono para no bloquear el flujo principal.
     *
     * @param event Evento de dominio a publicar
     */
    @Async
    @EventListener
    public void handleDomainEvent(DomainEvent event) {
        String topic = resolveTopicFromEvent(event);
        String eventName = resolveEventName(event);

        log.debug("Publicando evento {} en tópico SSE: {}", eventName, topic);

        sseEmitterService.broadcast(
                topic,
                eventName,
                Map.of(
                        "eventType", event.getClass().getSimpleName(),
                        "timestamp", event.occurredOn().toString(),
                        "data", event));
    }

    /**
     * Publica un evento personalizado a un tópico específico.
     *
     * @param topic Tópico destino
     * @param eventName Nombre del evento
     * @param data Datos del evento
     */
    public void publish(String topic, String eventName, Object data) {
        log.debug("Publicando evento {} en tópico SSE: {}", eventName, topic);
        sseEmitterService.broadcast(topic, eventName, data);
    }

    /**
     * Publica un evento a un cliente específico.
     *
     * @param clientId ID del cliente destino
     * @param eventName Nombre del evento
     * @param data Datos del evento
     * @return true si el envío fue exitoso
     */
    public boolean publishToClient(String clientId, String eventName, Object data) {
        return sseEmitterService.sendToClient(clientId, eventName, data);
    }

    /**
     * Resuelve el tópico SSE a partir del evento.
     *
     * <p>Convención: EntityNameEvent → "entitynames" (plural, minúsculas)
     */
    private String resolveTopicFromEvent(DomainEvent event) {
        String className = event.getClass().getSimpleName();

        // Remover sufijos comunes de eventos
        String baseName =
                className
                        .replace("CreatedEvent", "")
                        .replace("UpdatedEvent", "")
                        .replace("DeletedEvent", "")
                        .replace("Event", "");

        // Convertir a minúsculas y pluralizar simplemente
        return baseName.toLowerCase() + "s";
    }

    /**
     * Resuelve el nombre del evento para el tipo SSE.
     *
     * <p>Convención: OrderCreatedEvent → "order.created"
     */
    private String resolveEventName(DomainEvent event) {
        String className = event.getClass().getSimpleName().replace("Event", "");

        // Convertir CamelCase a dot.notation
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
