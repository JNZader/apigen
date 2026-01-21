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
 * REST controller for Server-Sent Events (SSE).
 *
 * <p>Allows clients to subscribe to real-time events.
 *
 * <p>JavaScript client usage:
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
 * <p>Available events: - connected: Sent when connection is established (includes clientId) -
 * heartbeat: Periodic keepalive - message: Generic event (configurable by topic)
 */
@RestController
@RequestMapping("${app.api.base-path:}/v1/events")
@Tag(name = "Server-Sent Events", description = "Endpoints for real-time events")
public class SseController {

    private final SseEmitterService sseEmitterService;

    public SseController(SseEmitterService sseEmitterService) {
        this.sseEmitterService = sseEmitterService;
    }

    @Operation(
            summary = "Subscribe to events from a topic",
            description =
                    """
                    Establishes an SSE connection to receive real-time events.

                    The connection remains open and will receive events whenever
                    one is published to the specified topic.

                    Available topics depend on the application configuration.
                    Common examples: 'orders', 'notifications', 'updates'.
                    """)
    @ApiResponse(
            responseCode = "200",
            description = "SSE connection established",
            content = @Content(mediaType = MediaType.TEXT_EVENT_STREAM_VALUE))
    @GetMapping(value = "/{topic}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(
            @Parameter(description = "Topic to subscribe to", example = "orders") @PathVariable
                    String topic,
            @Parameter(description = "Optional client ID (one is generated if not provided)")
                    @RequestParam(required = false)
                    String clientId) {
        return sseEmitterService.subscribe(topic, clientId);
    }

    @Operation(
            summary = "Get SSE statistics",
            description = "Returns information about active connections by topic")
    @ApiResponse(
            responseCode = "200",
            description = "Connection statistics",
            content = @Content(schema = @Schema(implementation = SseStats.class)))
    @GetMapping("/stats")
    public ResponseEntity<SseStats> getStats(
            @Parameter(description = "Filter by specific topic") @RequestParam(required = false)
                    String topic) {
        int totalClients = sseEmitterService.getTotalClientCount();
        int topicClients =
                topic != null ? sseEmitterService.getSubscriberCount(topic) : totalClients;

        return ResponseEntity.ok(new SseStats(totalClients, topicClients, topic));
    }

    /** DTO for SSE statistics. */
    @Schema(description = "SSE connection statistics")
    public record SseStats(
            @Schema(description = "Total connected clients") int totalClients,
            @Schema(description = "Clients in the filtered topic (or total if no filter)")
                    int topicClients,
            @Schema(description = "Filtered topic (null if no filter)") String filteredTopic) {}
}
