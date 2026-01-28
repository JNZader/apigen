package com.jnzader.apigen.gateway.error;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Utility class for writing RFC 7807 compliant error responses in the gateway context.
 *
 * <p>Provides consistent error formatting for reactive gateway filters.
 */
public class GatewayErrorWriter {

    private static final Logger log = LoggerFactory.getLogger(GatewayErrorWriter.class);
    private static final MediaType APPLICATION_PROBLEM_JSON =
            MediaType.valueOf("application/problem+json");

    private final ObjectMapper objectMapper;

    public GatewayErrorWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Writes an RFC 7807 compliant error response.
     *
     * @param exchange the server web exchange
     * @param status HTTP status code
     * @param type problem type URI
     * @param title short human-readable summary
     * @param detail human-readable explanation
     * @return Mono<Void> that completes when the response is written
     */
    public Mono<Void> writeError(
            ServerWebExchange exchange,
            HttpStatus status,
            String type,
            String title,
            String detail) {
        return writeError(exchange, status, type, title, detail, null);
    }

    /**
     * Writes an RFC 7807 compliant error response with extensions.
     *
     * @param exchange the server web exchange
     * @param status HTTP status code
     * @param type problem type URI
     * @param title short human-readable summary
     * @param detail human-readable explanation
     * @param extensions additional problem-specific fields
     * @return Mono<Void> that completes when the response is written
     */
    public Mono<Void> writeError(
            ServerWebExchange exchange,
            HttpStatus status,
            String type,
            String title,
            String detail,
            Map<String, Object> extensions) {

        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(APPLICATION_PROBLEM_JSON);

        Map<String, Object> problemDetail = new HashMap<>();
        problemDetail.put("type", URI.create(type).toString());
        problemDetail.put("title", title);
        problemDetail.put("status", status.value());
        problemDetail.put("detail", detail);
        problemDetail.put("instance", exchange.getRequest().getURI().getPath());
        problemDetail.put("timestamp", Instant.now().toString());

        if (extensions != null && !extensions.isEmpty()) {
            problemDetail.putAll(extensions);
        }

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(problemDetail);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize error response", e);
            return response.setComplete();
        }
    }

    /**
     * Writes an authentication error response (HTTP 401).
     *
     * @param exchange the server web exchange
     * @param detail error detail message
     * @return Mono<Void> that completes when the response is written
     */
    public Mono<Void> writeUnauthorized(ServerWebExchange exchange, String detail) {
        return writeError(
                exchange,
                HttpStatus.UNAUTHORIZED,
                "urn:apigen:problem:unauthorized",
                "Unauthorized",
                detail);
    }

    /**
     * Writes an authentication error response with error code.
     *
     * @param exchange the server web exchange
     * @param detail error detail message
     * @param errorCode specific error code
     * @return Mono<Void> that completes when the response is written
     */
    public Mono<Void> writeUnauthorized(
            ServerWebExchange exchange, String detail, String errorCode) {
        Map<String, Object> extensions = new HashMap<>();
        extensions.put("errorCode", errorCode);
        return writeError(
                exchange,
                HttpStatus.UNAUTHORIZED,
                "urn:apigen:problem:unauthorized",
                "Unauthorized",
                detail,
                extensions);
    }

    /**
     * Writes a forbidden error response (HTTP 403).
     *
     * @param exchange the server web exchange
     * @param detail error detail message
     * @return Mono<Void> that completes when the response is written
     */
    public Mono<Void> writeForbidden(ServerWebExchange exchange, String detail) {
        return writeError(
                exchange,
                HttpStatus.FORBIDDEN,
                "urn:apigen:problem:forbidden",
                "Forbidden",
                detail);
    }

    /**
     * Writes a gateway timeout error response (HTTP 504).
     *
     * @param exchange the server web exchange
     * @param detail error detail message
     * @return Mono<Void> that completes when the response is written
     */
    public Mono<Void> writeGatewayTimeout(ServerWebExchange exchange, String detail) {
        return writeError(
                exchange,
                HttpStatus.GATEWAY_TIMEOUT,
                "urn:apigen:problem:gateway-timeout",
                "Gateway Timeout",
                detail);
    }

    /**
     * Writes a gateway timeout error response with circuit breaker info.
     *
     * @param exchange the server web exchange
     * @param circuitBreakerId the circuit breaker identifier
     * @return Mono<Void> that completes when the response is written
     */
    public Mono<Void> writeGatewayTimeout(
            ServerWebExchange exchange, String detail, String circuitBreakerId) {
        Map<String, Object> extensions = new HashMap<>();
        extensions.put("circuitBreakerId", circuitBreakerId);
        return writeError(
                exchange,
                HttpStatus.GATEWAY_TIMEOUT,
                "urn:apigen:problem:gateway-timeout",
                "Gateway Timeout",
                detail,
                extensions);
    }

    /**
     * Writes a service unavailable error response (HTTP 503).
     *
     * @param exchange the server web exchange
     * @param detail error detail message
     * @return Mono<Void> that completes when the response is written
     */
    public Mono<Void> writeServiceUnavailable(ServerWebExchange exchange, String detail) {
        return writeError(
                exchange,
                HttpStatus.SERVICE_UNAVAILABLE,
                "urn:apigen:problem:service-unavailable",
                "Service Unavailable",
                detail);
    }

    /**
     * Writes a service unavailable error response with circuit breaker info.
     *
     * @param exchange the server web exchange
     * @param detail error detail message
     * @param circuitBreakerId the circuit breaker identifier
     * @return Mono<Void> that completes when the response is written
     */
    public Mono<Void> writeServiceUnavailable(
            ServerWebExchange exchange, String detail, String circuitBreakerId) {
        Map<String, Object> extensions = new HashMap<>();
        extensions.put("circuitBreakerId", circuitBreakerId);
        return writeError(
                exchange,
                HttpStatus.SERVICE_UNAVAILABLE,
                "urn:apigen:problem:service-unavailable",
                "Service Unavailable",
                detail,
                extensions);
    }

    /**
     * Writes a rate limit exceeded error response (HTTP 429).
     *
     * @param exchange the server web exchange
     * @param retryAfterSeconds seconds to wait before retrying
     * @return Mono<Void> that completes when the response is written
     */
    public Mono<Void> writeRateLimitExceeded(ServerWebExchange exchange, long retryAfterSeconds) {
        Map<String, Object> extensions = new HashMap<>();
        extensions.put("retryAfterSeconds", retryAfterSeconds);

        ServerHttpResponse response = exchange.getResponse();
        response.getHeaders().add("Retry-After", String.valueOf(retryAfterSeconds));

        return writeError(
                exchange,
                HttpStatus.TOO_MANY_REQUESTS,
                "urn:apigen:problem:rate-limit-exceeded",
                "Too Many Requests",
                String.format("Rate limit exceeded. Retry after %d seconds.", retryAfterSeconds),
                extensions);
    }

    /**
     * Writes a bad gateway error response (HTTP 502).
     *
     * @param exchange the server web exchange
     * @param detail error detail message
     * @return Mono<Void> that completes when the response is written
     */
    public Mono<Void> writeBadGateway(ServerWebExchange exchange, String detail) {
        return writeError(
                exchange,
                HttpStatus.BAD_GATEWAY,
                "urn:apigen:problem:bad-gateway",
                "Bad Gateway",
                detail);
    }
}
