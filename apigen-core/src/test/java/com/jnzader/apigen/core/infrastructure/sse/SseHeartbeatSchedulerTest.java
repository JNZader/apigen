package com.jnzader.apigen.core.infrastructure.sse;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("SseHeartbeatScheduler Tests")
class SseHeartbeatSchedulerTest {

    @Mock private SseEmitterService sseEmitterService;

    private SseHeartbeatScheduler heartbeatScheduler;

    @BeforeEach
    void setUp() {
        heartbeatScheduler = new SseHeartbeatScheduler(sseEmitterService);
    }

    @Nested
    @DisplayName("SendHeartbeat Tests")
    class SendHeartbeatTests {

        @Test
        @DisplayName("should send heartbeat when clients are connected")
        void shouldSendHeartbeatWhenClientsConnected() {
            when(sseEmitterService.getTotalClientCount()).thenReturn(5);

            heartbeatScheduler.sendHeartbeat();

            verify(sseEmitterService).sendHeartbeatToAll();
        }

        @Test
        @DisplayName("should not send heartbeat when no clients connected")
        void shouldNotSendHeartbeatWhenNoClientsConnected() {
            when(sseEmitterService.getTotalClientCount()).thenReturn(0);

            heartbeatScheduler.sendHeartbeat();

            verify(sseEmitterService, never()).sendHeartbeatToAll();
        }

        @Test
        @DisplayName("should check client count before sending heartbeat")
        void shouldCheckClientCountBeforeSendingHeartbeat() {
            when(sseEmitterService.getTotalClientCount()).thenReturn(3);

            heartbeatScheduler.sendHeartbeat();

            verify(sseEmitterService).getTotalClientCount();
            verify(sseEmitterService).sendHeartbeatToAll();
        }
    }
}
