package com.jnzader.apigen.gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoggingGatewayFilter Tests")
class LoggingGatewayFilterTest {

    @Mock private GatewayFilterChain chain;

    private LoggingGatewayFilter filter;

    @BeforeEach
    void setUp() {
        filter = new LoggingGatewayFilter();
    }

    @Nested
    @DisplayName("Correlation ID")
    class CorrelationIdTests {

        @Test
        @DisplayName("should generate correlation ID when not present")
        void shouldGenerateCorrelationIdWhenNotPresent() {
            MockServerHttpRequest request = MockServerHttpRequest.get("/api/test").build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            when(chain.filter(any())).thenReturn(Mono.empty());

            StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

            verify(chain)
                    .filter(
                            argThat(
                                    ex -> {
                                        String correlationId =
                                                ex.getRequest()
                                                        .getHeaders()
                                                        .getFirst(
                                                                LoggingGatewayFilter
                                                                        .CORRELATION_ID_HEADER);
                                        return correlationId != null && !correlationId.isBlank();
                                    }));
        }

        @Test
        @DisplayName("should preserve existing correlation ID")
        void shouldPreserveExistingCorrelationId() {
            String existingCorrelationId = "existing-correlation-id";
            MockServerHttpRequest request =
                    MockServerHttpRequest.get("/api/test")
                            .header(
                                    LoggingGatewayFilter.CORRELATION_ID_HEADER,
                                    existingCorrelationId)
                            .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            when(chain.filter(any())).thenReturn(Mono.empty());

            StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

            verify(chain)
                    .filter(
                            argThat(
                                    ex -> {
                                        String correlationId =
                                                ex.getRequest()
                                                        .getHeaders()
                                                        .getFirst(
                                                                LoggingGatewayFilter
                                                                        .CORRELATION_ID_HEADER);
                                        return existingCorrelationId.equals(correlationId);
                                    }));
        }
    }

    @Nested
    @DisplayName("Request Start Time")
    class RequestStartTimeTests {

        @Test
        @DisplayName("should record request start time")
        void shouldRecordRequestStartTime() {
            MockServerHttpRequest request = MockServerHttpRequest.get("/api/test").build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            when(chain.filter(any())).thenReturn(Mono.empty());

            StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

            Object startTime = exchange.getAttribute(LoggingGatewayFilter.REQUEST_START_TIME);
            assertThat(startTime).isNotNull().isInstanceOf(Long.class);
        }
    }

    @Nested
    @DisplayName("Filter Order")
    class FilterOrderTests {

        @Test
        @DisplayName("should have highest precedence")
        void shouldHaveHighestPrecedence() {
            assertThat(filter.getOrder()).isEqualTo(Ordered.HIGHEST_PRECEDENCE);
        }
    }

    @Nested
    @DisplayName("Configuration")
    class ConfigurationTests {

        @Test
        @DisplayName("should create filter with default configuration")
        void shouldCreateFilterWithDefaultConfiguration() {
            LoggingGatewayFilter defaultFilter = new LoggingGatewayFilter();
            assertThat(defaultFilter).isNotNull();
        }

        @Test
        @DisplayName("should create filter with custom configuration")
        void shouldCreateFilterWithCustomConfiguration() {
            LoggingGatewayFilter customFilter = new LoggingGatewayFilter(true);
            assertThat(customFilter).isNotNull();
        }
    }
}
