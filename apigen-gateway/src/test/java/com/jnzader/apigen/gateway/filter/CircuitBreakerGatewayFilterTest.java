package com.jnzader.apigen.gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
@DisplayName("CircuitBreakerGatewayFilter Tests")
class CircuitBreakerGatewayFilterTest {

    @Mock private GatewayFilterChain chain;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Nested
    @DisplayName("Normal Operation")
    class NormalOperationTests {

        @Test
        @DisplayName("should pass through when downstream succeeds")
        void shouldPassThroughWhenDownstreamSucceeds() {
            CircuitBreakerGatewayFilter filter =
                    new CircuitBreakerGatewayFilter(
                            "test-cb", Duration.ofSeconds(10), objectMapper);

            MockServerHttpRequest request = MockServerHttpRequest.get("/api/test").build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            when(chain.filter(exchange)).thenReturn(Mono.empty());

            StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

            verify(chain).filter(exchange);
        }
    }

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandlingTests {

        @Test
        @DisplayName("should return 503 on general error with default fallback")
        void shouldReturn503OnGeneralErrorWithDefaultFallback() {
            CircuitBreakerGatewayFilter filter =
                    new CircuitBreakerGatewayFilter(
                            "test-cb", Duration.ofSeconds(10), objectMapper);

            MockServerHttpRequest request = MockServerHttpRequest.get("/api/test").build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            when(chain.filter(exchange))
                    .thenReturn(Mono.error(new RuntimeException("Service unavailable")));

            StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

            assertThat(exchange.getResponse().getStatusCode())
                    .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        }

        @Test
        @DisplayName("should return 504 on timeout with default fallback")
        void shouldReturn504OnTimeoutWithDefaultFallback() {
            CircuitBreakerGatewayFilter filter =
                    new CircuitBreakerGatewayFilter(
                            "test-cb", Duration.ofSeconds(10), objectMapper);

            MockServerHttpRequest request = MockServerHttpRequest.get("/api/test").build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            when(chain.filter(exchange))
                    .thenReturn(Mono.error(new java.util.concurrent.TimeoutException("Timeout")));

            StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

            assertThat(exchange.getResponse().getStatusCode())
                    .isEqualTo(HttpStatus.GATEWAY_TIMEOUT);
        }

        @Test
        @DisplayName("should use custom fallback when provided")
        void shouldUseCustomFallbackWhenProvided() {
            CircuitBreakerGatewayFilter filter =
                    new CircuitBreakerGatewayFilter(
                            "test-cb",
                            Duration.ofSeconds(10),
                            ex -> {
                                ex.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                                return ex.getResponse().setComplete();
                            },
                            objectMapper);

            MockServerHttpRequest request = MockServerHttpRequest.get("/api/test").build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            when(chain.filter(exchange)).thenReturn(Mono.error(new RuntimeException("Error")));

            StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

            assertThat(exchange.getResponse().getStatusCode())
                    .isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        }
    }

    @Nested
    @DisplayName("Builder")
    class BuilderTests {

        @Test
        @DisplayName("should build with default timeout")
        void shouldBuildWithDefaultTimeout() {
            CircuitBreakerGatewayFilter filter =
                    CircuitBreakerGatewayFilter.builder("my-cb", objectMapper).build();

            assertThat(filter.getCircuitBreakerId()).isEqualTo("my-cb");
            assertThat(filter.getTimeout()).isEqualTo(Duration.ofSeconds(10));
        }

        @Test
        @DisplayName("should build with custom timeout")
        void shouldBuildWithCustomTimeout() {
            CircuitBreakerGatewayFilter filter =
                    CircuitBreakerGatewayFilter.builder("my-cb", objectMapper)
                            .timeout(Duration.ofSeconds(30))
                            .build();

            assertThat(filter.getTimeout()).isEqualTo(Duration.ofSeconds(30));
        }

        @Test
        @DisplayName("should build with fallback")
        void shouldBuildWithFallback() {
            CircuitBreakerGatewayFilter filter =
                    CircuitBreakerGatewayFilter.builder("my-cb", objectMapper)
                            .fallback(ex -> Mono.empty())
                            .build();

            assertThat(filter).isNotNull();
        }
    }

    @Nested
    @DisplayName("Accessors")
    class AccessorsTests {

        @Test
        @DisplayName("should provide circuit breaker id")
        void shouldProvideCircuitBreakerId() {
            CircuitBreakerGatewayFilter filter =
                    new CircuitBreakerGatewayFilter("cb-id", Duration.ofSeconds(5), objectMapper);

            assertThat(filter.getCircuitBreakerId()).isEqualTo("cb-id");
        }

        @Test
        @DisplayName("should provide timeout")
        void shouldProvideTimeout() {
            CircuitBreakerGatewayFilter filter =
                    new CircuitBreakerGatewayFilter("cb-id", Duration.ofSeconds(5), objectMapper);

            assertThat(filter.getTimeout()).isEqualTo(Duration.ofSeconds(5));
        }
    }
}
