package com.jnzader.apigen.core.infrastructure.sse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Servicio para gestionar conexiones SSE (Server-Sent Events).
 * <p>
 * Características:
 * - Gestión de múltiples clientes por tópico
 * - Broadcast a todos los suscriptores de un tópico
 * - Envío a clientes específicos por ID
 * - Limpieza automática de conexiones muertas
 * - Thread-safe para uso concurrente
 * <p>
 * Uso típico:
 * <pre>
 * // En un controlador
 * {@literal @}GetMapping(value = "/events/{topic}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
 * public SseEmitter subscribe({@literal @}PathVariable String topic) {
 *     return sseEmitterService.subscribe(topic);
 * }
 *
 * // En un servicio para enviar eventos
 * sseEmitterService.broadcast("orders", new OrderEvent(order));
 * </pre>
 */
@Service
public class SseEmitterService {

    private static final Logger log = LoggerFactory.getLogger(SseEmitterService.class);

    /**
     * Timeout para conexiones SSE (30 minutos por defecto).
     * Valor mayor que el típico para evitar reconexiones frecuentes.
     */
    private static final long DEFAULT_TIMEOUT = 30 * 60 * 1000L;

    /**
     * Mapa de tópicos a sus suscriptores.
     * Cada tópico puede tener múltiples clientes conectados.
     */
    private final Map<String, Set<SseClient>> topicSubscribers = new ConcurrentHashMap<>();

    /**
     * Mapa de cliente ID a su emitter (para envío directo).
     */
    private final Map<String, SseClient> clientsById = new ConcurrentHashMap<>();

    /**
     * Suscribe un nuevo cliente a un tópico.
     *
     * @param topic Tópico al que suscribirse
     * @return SseEmitter para la conexión
     */
    public SseEmitter subscribe(String topic) {
        return subscribe(topic, null);
    }

    /**
     * Suscribe un nuevo cliente a un tópico con ID específico.
     *
     * @param topic    Tópico al que suscribirse
     * @param clientId ID único del cliente (opcional)
     * @return SseEmitter para la conexión
     */
    public SseEmitter subscribe(String topic, String clientId) {
        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT);
        String id = clientId != null ? clientId : java.util.UUID.randomUUID().toString();

        SseClient client = new SseClient(id, topic, emitter);

        // Agregar a suscriptores del tópico
        topicSubscribers.computeIfAbsent(topic, _ -> new CopyOnWriteArraySet<>())
                .add(client);

        // Agregar al mapa de clientes por ID
        clientsById.put(id, client);

        // Configurar callbacks para limpieza
        emitter.onCompletion(() -> removeClient(client));
        emitter.onTimeout(() -> {
            log.debug("SSE timeout para cliente {} en tópico {}", id, topic);
            removeClient(client);
        });
        emitter.onError(ex -> {
            log.debug("SSE error para cliente {} en tópico {}: {}", id, topic, ex.getMessage());
            removeClient(client);
        });

        // Enviar evento inicial de conexión
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .id(id)
                    .data(Map.of(
                            "clientId", id,
                            "topic", topic,
                            "timestamp", java.time.Instant.now().toString()
                    )));
            log.debug("Cliente {} conectado a tópico {}", id, topic);
        } catch (IOException e) {
            log.warn("Error enviando evento de conexión a cliente {}: {}", id, e.getMessage());
            removeClient(client);
        }

        return emitter;
    }

    /**
     * Envía un evento a todos los suscriptores de un tópico.
     *
     * @param topic Tópico destino
     * @param event Datos del evento
     */
    public void broadcast(String topic, Object event) {
        broadcast(topic, "message", event);
    }

    /**
     * Envía un evento nombrado a todos los suscriptores de un tópico.
     *
     * @param topic     Tópico destino
     * @param eventName Nombre del evento (para filtrar en cliente)
     * @param event     Datos del evento
     */
    public void broadcast(String topic, String eventName, Object event) {
        Set<SseClient> subscribers = topicSubscribers.get(topic);
        if (subscribers == null || subscribers.isEmpty()) {
            log.trace("No hay suscriptores para tópico {}", topic);
            return;
        }

        log.debug("Enviando evento '{}' a {} suscriptores del tópico {}",
                eventName, subscribers.size(), topic);

        String eventId = java.util.UUID.randomUUID().toString();

        for (SseClient client : subscribers) {
            try {
                client.emitter().send(SseEmitter.event()
                        .name(eventName)
                        .id(eventId)
                        .data(event));
            } catch (IOException e) {
                log.debug("Error enviando a cliente {}: {}", client.id(), e.getMessage());
                removeClient(client);
            }
        }
    }

    /**
     * Envía un evento a un cliente específico por ID.
     *
     * @param clientId  ID del cliente destino
     * @param eventName Nombre del evento
     * @param event     Datos del evento
     * @return true si el envío fue exitoso
     */
    public boolean sendToClient(String clientId, String eventName, Object event) {
        SseClient client = clientsById.get(clientId);
        if (client == null) {
            log.debug("Cliente {} no encontrado", clientId);
            return false;
        }

        try {
            client.emitter().send(SseEmitter.event()
                    .name(eventName)
                    .id(java.util.UUID.randomUUID().toString())
                    .data(event));
            return true;
        } catch (IOException e) {
            log.debug("Error enviando a cliente {}: {}", clientId, e.getMessage());
            removeClient(client);
            return false;
        }
    }

    /**
     * Envía un heartbeat a todos los clientes conectados.
     * Útil para mantener conexiones vivas y detectar clientes muertos.
     */
    public void sendHeartbeatToAll() {
        long timestamp = System.currentTimeMillis();
        for (Set<SseClient> subscribers : topicSubscribers.values()) {
            for (SseClient client : subscribers) {
                try {
                    client.emitter().send(SseEmitter.event()
                            .name("heartbeat")
                            .data(Map.of("timestamp", timestamp)));
                } catch (IOException _) {
                    removeClient(client);
                }
            }
        }
    }

    /**
     * Obtiene el número de suscriptores de un tópico.
     */
    public int getSubscriberCount(String topic) {
        Set<SseClient> subscribers = topicSubscribers.get(topic);
        return subscribers != null ? subscribers.size() : 0;
    }

    /**
     * Obtiene el número total de clientes conectados.
     */
    public int getTotalClientCount() {
        return clientsById.size();
    }

    /**
     * Desconecta a un cliente específico.
     */
    public void disconnectClient(String clientId) {
        SseClient client = clientsById.get(clientId);
        if (client != null) {
            client.emitter().complete();
            removeClient(client);
        }
    }

    /**
     * Desconecta a todos los suscriptores de un tópico.
     */
    public void disconnectTopic(String topic) {
        Set<SseClient> subscribers = topicSubscribers.remove(topic);
        if (subscribers != null) {
            for (SseClient client : subscribers) {
                client.emitter().complete();
                clientsById.remove(client.id());
            }
        }
    }

    /**
     * Elimina un cliente de todas las estructuras de datos.
     */
    private void removeClient(SseClient client) {
        clientsById.remove(client.id());
        Set<SseClient> subscribers = topicSubscribers.get(client.topic());
        if (subscribers != null) {
            subscribers.remove(client);
            if (subscribers.isEmpty()) {
                topicSubscribers.remove(client.topic());
            }
        }
        log.debug("Cliente {} eliminado del tópico {}", client.id(), client.topic());
    }

    /**
     * Record interno para representar un cliente SSE.
     */
    private record SseClient(String id, String topic, SseEmitter emitter) {
    }
}
