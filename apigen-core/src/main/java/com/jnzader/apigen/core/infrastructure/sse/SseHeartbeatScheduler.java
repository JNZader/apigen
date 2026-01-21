package com.jnzader.apigen.core.infrastructure.sse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler for sending periodic heartbeats to SSE clients.
 *
 * <p>Keeps connections alive and detects disconnected clients. Runs every 30 seconds by default.
 *
 * <p>Can be disabled with: apigen.sse.heartbeat.enabled=false
 */
@Component
@ConditionalOnProperty(
        name = "apigen.sse.heartbeat.enabled",
        havingValue = "true",
        matchIfMissing = true)
public class SseHeartbeatScheduler {

    private static final Logger log = LoggerFactory.getLogger(SseHeartbeatScheduler.class);

    private final SseEmitterService sseEmitterService;

    public SseHeartbeatScheduler(SseEmitterService sseEmitterService) {
        this.sseEmitterService = sseEmitterService;
    }

    /** Sends heartbeat to all connected clients. Runs every 30 seconds. */
    @Scheduled(fixedRateString = "${apigen.sse.heartbeat.interval:30000}")
    public void sendHeartbeat() {
        int clientCount = sseEmitterService.getTotalClientCount();
        if (clientCount > 0) {
            log.trace("Sending heartbeat to {} SSE clients", clientCount);
            sseEmitterService.sendHeartbeatToAll();
        }
    }
}
