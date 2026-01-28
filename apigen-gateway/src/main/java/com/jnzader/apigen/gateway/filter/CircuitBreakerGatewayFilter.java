package com.jnzader.apigen.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jnzader.apigen.gateway.error.GatewayErrorWriter;
import java.time.Duration;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Gateway filter that provides circuit breaker functionality. Wraps downstream calls with timeout
 * and fallback capabilities.
 *
 * <p>Returns RFC 7807 compliant error responses for circuit breaker triggers.
 */
public class CircuitBreakerGatewayFilter implements GatewayFilter {

    private static final Logger log = LoggerFactory.getLogger(CircuitBreakerGatewayFilter.class);

    private final String circuitBreakerId;
    private final Duration timeout;
    private final Function<ServerWebExchange, Mono<Void>> fallback;
    private final GatewayErrorWriter errorWriter;

    public CircuitBreakerGatewayFilter(
            String circuitBreakerId, Duration timeout, ObjectMapper objectMapper) {
        this(circuitBreakerId, timeout, null, objectMapper);
    }

    public CircuitBreakerGatewayFilter(
            String circuitBreakerId,
            Duration timeout,
            Function<ServerWebExchange, Mono<Void>> fallback,
            ObjectMapper objectMapper) {
        this.circuitBreakerId = circuitBreakerId;
        this.timeout = timeout;
        this.fallback = fallback;
        this.errorWriter = new GatewayErrorWriter(objectMapper);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange)
                .timeout(timeout)
                .onErrorResume(
                        throwable -> {
                            log.warn(
                                    "Circuit breaker [{}] triggered for {}: {}",
                                    circuitBreakerId,
                                    exchange.getRequest().getURI().getPath(),
                                    throwable.getMessage());

                            if (fallback != null) {
                                return fallback.apply(exchange);
                            }

                            return defaultFallback(exchange, throwable);
                        });
    }

    private Mono<Void> defaultFallback(ServerWebExchange exchange, Throwable throwable) {
        if (isTimeout(throwable)) {
            return errorWriter.writeGatewayTimeout(
                    exchange,
                    String.format(
                            "Request to downstream service timed out after %d seconds",
                            timeout.toSeconds()),
                    circuitBreakerId);
        } else {
            return errorWriter.writeServiceUnavailable(
                    exchange,
                    String.format(
                            "Downstream service is unavailable: %s",
                            throwable.getMessage() != null
                                    ? throwable.getMessage()
                                    : "Unknown error"),
                    circuitBreakerId);
        }
    }

    private boolean isTimeout(Throwable throwable) {
        return throwable instanceof java.util.concurrent.TimeoutException
                || throwable.getCause() instanceof java.util.concurrent.TimeoutException;
    }

    public String getCircuitBreakerId() {
        return circuitBreakerId;
    }

    public Duration getTimeout() {
        return timeout;
    }

    /** Builder for creating CircuitBreakerGatewayFilter instances. */
    public static Builder builder(String circuitBreakerId, ObjectMapper objectMapper) {
        return new Builder(circuitBreakerId, objectMapper);
    }

    public static class Builder {
        private final String circuitBreakerId;
        private final ObjectMapper objectMapper;
        private Duration timeout = Duration.ofSeconds(10);
        private Function<ServerWebExchange, Mono<Void>> fallback;

        private Builder(String circuitBreakerId, ObjectMapper objectMapper) {
            this.circuitBreakerId = circuitBreakerId;
            this.objectMapper = objectMapper;
        }

        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder fallback(Function<ServerWebExchange, Mono<Void>> fallback) {
            this.fallback = fallback;
            return this;
        }

        public CircuitBreakerGatewayFilter build() {
            return new CircuitBreakerGatewayFilter(
                    circuitBreakerId, timeout, fallback, objectMapper);
        }
    }
}
