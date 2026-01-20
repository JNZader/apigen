package com.jnzader.apigen.core.infrastructure.sse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Controlador REST para Server-Sent Events (SSE).
 *
 * <p>Permite a los clientes suscribirse a eventos en tiempo real.
 *
 * <p>Uso en cliente JavaScript:
 *
 * <pre>
 * const eventSource = new EventSource('/api/v1/events/orders');
 *
 * eventSource.onopen = () => console.log('Connected');
 *
 * eventSource.addEventListener('connected', (e) => {
 *     const data = JSON.parse(e.data);
 *     console.log('Client ID:', data.clientId);
 * });
 *
 * eventSource.addEventListener('message', (e) => {
 *     const event = JSON.parse(e.data);
 *     console.log('Event:', event);
 * });
 *
 * eventSource.onerror = (e) => {
 *     console.log('Connection error');
 *     eventSource.close();
 * };
 * </pre>
 *
 * <p>Eventos disponibles: - connected: Enviado al establecer conexión (incluye clientId) -
 * heartbeat: Keepalive periódico - message: Evento genérico (configurable por tópico)
 */
@RestController
@RequestMapping("${app.api.base-path:}/v1/events")
@Tag(name = "Server-Sent Events", description = "Endpoints para eventos en tiempo real")
public class SseController {

    private final SseEmitterService sseEmitterService;

    public SseController(SseEmitterService sseEmitterService) {
        this.sseEmitterService = sseEmitterService;
    }

    @Operation(
            summary = "Suscribirse a eventos de un tópico",
            description =
                    """
                    Establece una conexión SSE para recibir eventos en tiempo real.

                    La conexión permanece abierta y recibirá eventos cada vez que
                    se publique uno en el tópico especificado.

                    Tópicos disponibles dependen de la configuración de la aplicación.
                    Ejemplos comunes: 'orders', 'notifications', 'updates'.
                    """)
    @ApiResponse(
            responseCode = "200",
            description = "Conexión SSE establecida",
            content = @Content(mediaType = MediaType.TEXT_EVENT_STREAM_VALUE))
    @GetMapping(value = "/{topic}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(
            @Parameter(description = "Tópico al que suscribirse", example = "orders") @PathVariable
                    String topic,
            @Parameter(description = "ID de cliente opcional (se genera uno si no se proporciona)")
                    @RequestParam(required = false)
                    String clientId) {
        return sseEmitterService.subscribe(topic, clientId);
    }

    @Operation(
            summary = "Obtener estadísticas de SSE",
            description = "Retorna información sobre conexiones activas por tópico")
    @ApiResponse(
            responseCode = "200",
            description = "Estadísticas de conexiones",
            content = @Content(schema = @Schema(implementation = SseStats.class)))
    @GetMapping("/stats")
    public ResponseEntity<SseStats> getStats(
            @Parameter(description = "Filtrar por tópico específico")
                    @RequestParam(required = false)
                    String topic) {
        int totalClients = sseEmitterService.getTotalClientCount();
        int topicClients =
                topic != null ? sseEmitterService.getSubscriberCount(topic) : totalClients;

        return ResponseEntity.ok(new SseStats(totalClients, topicClients, topic));
    }

    /** DTO para estadísticas de SSE. */
    @Schema(description = "Estadísticas de conexiones SSE")
    public record SseStats(
            @Schema(description = "Total de clientes conectados") int totalClients,
            @Schema(description = "Clientes en el tópico filtrado (o total si no hay filtro)")
                    int topicClients,
            @Schema(description = "Tópico filtrado (null si no hay filtro)")
                    String filteredTopic) {}
}
