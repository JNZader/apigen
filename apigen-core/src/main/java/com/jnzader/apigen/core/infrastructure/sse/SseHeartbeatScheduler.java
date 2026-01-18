package com.jnzader.apigen.core.infrastructure.sse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler para enviar heartbeats periódicos a clientes SSE.
 * <p>
 * Mantiene las conexiones vivas y detecta clientes desconectados.
 * Se ejecuta cada 30 segundos por defecto.
 * <p>
 * Puede deshabilitarse con: apigen.sse.heartbeat.enabled=false
 */
@Component
@ConditionalOnProperty(name = "apigen.sse.heartbeat.enabled", havingValue = "true", matchIfMissing = true)
public class SseHeartbeatScheduler {

    private static final Logger log = LoggerFactory.getLogger(SseHeartbeatScheduler.class);

    private final SseEmitterService sseEmitterService;

    public SseHeartbeatScheduler(SseEmitterService sseEmitterService) {
        this.sseEmitterService = sseEmitterService;
    }

    /**
     * Envía heartbeat a todos los clientes conectados.
     * Se ejecuta cada 30 segundos.
     */
    @Scheduled(fixedRateString = "${apigen.sse.heartbeat.interval:30000}")
    public void sendHeartbeat() {
        int clientCount = sseEmitterService.getTotalClientCount();
        if (clientCount > 0) {
            log.trace("Enviando heartbeat a {} clientes SSE", clientCount);
            sseEmitterService.sendHeartbeatToAll();
        }
    }
}
