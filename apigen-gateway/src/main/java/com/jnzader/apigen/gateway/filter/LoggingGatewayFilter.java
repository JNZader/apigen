package com.jnzader.apigen.gateway.filter;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Global filter for logging incoming requests and outgoing responses. Adds a correlation ID to
 * track requests across services.
 */
public class LoggingGatewayFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(LoggingGatewayFilter.class);

    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    public static final String REQUEST_START_TIME = "requestStartTime";

    private final boolean includeHeaders;

    public LoggingGatewayFilter() {
        this(false);
    }

    public LoggingGatewayFilter(boolean includeHeaders) {
        this.includeHeaders = includeHeaders;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        // Generate or extract correlation ID
        String correlationId = extractOrGenerateCorrelationId(request);

        // Record start time
        long startTime = System.currentTimeMillis();
        exchange.getAttributes().put(REQUEST_START_TIME, startTime);

        // Add correlation ID to request and response headers
        ServerHttpRequest mutatedRequest =
                request.mutate().header(CORRELATION_ID_HEADER, correlationId).build();

        ServerWebExchange mutatedExchange = exchange.mutate().request(mutatedRequest).build();

        // Log incoming request
        logRequest(mutatedRequest, correlationId);

        return chain.filter(mutatedExchange)
                .then(
                        Mono.fromRunnable(
                                () -> {
                                    // Add correlation ID to response
                                    ServerHttpResponse response = exchange.getResponse();
                                    response.getHeaders().add(CORRELATION_ID_HEADER, correlationId);

                                    // Log response
                                    long duration = System.currentTimeMillis() - startTime;
                                    logResponse(response, correlationId, duration);
                                }));
    }

    private String extractOrGenerateCorrelationId(ServerHttpRequest request) {
        String correlationId = request.getHeaders().getFirst(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        return correlationId;
    }

    private void logRequest(ServerHttpRequest request, String correlationId) {
        StringBuilder sb = new StringBuilder();
        sb.append("Incoming request: ")
                .append(request.getMethod())
                .append(" ")
                .append(request.getURI().getPath());

        if (request.getURI().getQuery() != null) {
            sb.append("?").append(request.getURI().getQuery());
        }

        sb.append(" [correlationId=").append(correlationId).append("]");

        if (includeHeaders) {
            sb.append(" headers=").append(sanitizeHeaders(request.getHeaders()));
        }

        if (log.isInfoEnabled()) {
            log.info(sb.toString());
        }
    }

    private void logResponse(ServerHttpResponse response, String correlationId, long duration) {
        if (!log.isInfoEnabled()) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Outgoing response: ")
                .append(response.getStatusCode())
                .append(" [correlationId=")
                .append(correlationId)
                .append("]")
                .append(" duration=")
                .append(duration)
                .append("ms");

        if (includeHeaders) {
            sb.append(" headers=").append(sanitizeHeaders(response.getHeaders()));
        }

        log.info(sb.toString());
    }

    private String sanitizeHeaders(HttpHeaders headers) {
        // Remove sensitive headers from logging
        HttpHeaders sanitized = new HttpHeaders();
        headers.forEach(
                (name, values) -> {
                    if (!isSensitiveHeader(name)) {
                        sanitized.addAll(name, values);
                    } else {
                        sanitized.add(name, "[REDACTED]");
                    }
                });
        return sanitized.toString();
    }

    private boolean isSensitiveHeader(String headerName) {
        String lower = headerName.toLowerCase();
        return lower.contains("authorization")
                || lower.contains("cookie")
                || lower.contains("token")
                || lower.contains("secret")
                || lower.contains("password")
                || lower.contains("api-key");
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
