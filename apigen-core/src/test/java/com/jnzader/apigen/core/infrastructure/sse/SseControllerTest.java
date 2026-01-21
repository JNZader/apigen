package com.jnzader.apigen.core.infrastructure.sse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@ExtendWith(MockitoExtension.class)
@DisplayName("SseController Tests")
class SseControllerTest {

    @Mock private SseEmitterService sseEmitterService;

    private SseController sseController;

    @BeforeEach
    void setUp() {
        sseController = new SseController(sseEmitterService);
    }

    @Nested
    @DisplayName("Subscribe Tests")
    class SubscribeTests {

        @Test
        @DisplayName("should subscribe to topic")
        void shouldSubscribeToTopic() {
            SseEmitter mockEmitter = new SseEmitter();
            when(sseEmitterService.subscribe("orders", null)).thenReturn(mockEmitter);

            SseEmitter result = sseController.subscribe("orders", null);

            assertThat(result).isSameAs(mockEmitter);
            verify(sseEmitterService).subscribe("orders", null);
        }

        @Test
        @DisplayName("should subscribe with client ID")
        void shouldSubscribeWithClientId() {
            SseEmitter mockEmitter = new SseEmitter();
            when(sseEmitterService.subscribe("orders", "client-123")).thenReturn(mockEmitter);

            SseEmitter result = sseController.subscribe("orders", "client-123");

            assertThat(result).isSameAs(mockEmitter);
            verify(sseEmitterService).subscribe("orders", "client-123");
        }
    }

    @Nested
    @DisplayName("GetStats Tests")
    class GetStatsTests {

        @Test
        @DisplayName("should return stats without topic filter")
        void shouldReturnStatsWithoutTopicFilter() {
            when(sseEmitterService.getTotalClientCount()).thenReturn(10);

            ResponseEntity<SseController.SseStats> response = sseController.getStats(null);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().totalClients()).isEqualTo(10);
            assertThat(response.getBody().topicClients()).isEqualTo(10);
            assertThat(response.getBody().filteredTopic()).isNull();
        }

        @Test
        @DisplayName("should return stats with topic filter")
        void shouldReturnStatsWithTopicFilter() {
            when(sseEmitterService.getTotalClientCount()).thenReturn(10);
            when(sseEmitterService.getSubscriberCount("orders")).thenReturn(3);

            ResponseEntity<SseController.SseStats> response = sseController.getStats("orders");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().totalClients()).isEqualTo(10);
            assertThat(response.getBody().topicClients()).isEqualTo(3);
            assertThat(response.getBody().filteredTopic()).isEqualTo("orders");
        }

        @Test
        @DisplayName("should return zero when no clients connected")
        void shouldReturnZeroWhenNoClientsConnected() {
            when(sseEmitterService.getTotalClientCount()).thenReturn(0);

            ResponseEntity<SseController.SseStats> response = sseController.getStats(null);

            assertThat(response.getBody().totalClients()).isZero();
            assertThat(response.getBody().topicClients()).isZero();
        }
    }
}
