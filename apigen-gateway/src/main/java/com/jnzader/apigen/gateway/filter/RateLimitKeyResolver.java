package com.jnzader.apigen.gateway.filter;

import java.util.Objects;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Key resolver for rate limiting in Spring Cloud Gateway. Supports multiple resolution strategies:
 * IP, User ID, API Key, or composite keys.
 */
public class RateLimitKeyResolver implements KeyResolver {

    private final KeyResolutionStrategy strategy;
    private final String apiKeyHeader;

    public RateLimitKeyResolver() {
        this(KeyResolutionStrategy.IP, "X-API-Key");
    }

    public RateLimitKeyResolver(KeyResolutionStrategy strategy) {
        this(strategy, "X-API-Key");
    }

    public RateLimitKeyResolver(KeyResolutionStrategy strategy, String apiKeyHeader) {
        this.strategy = Objects.requireNonNull(strategy, "Strategy cannot be null");
        this.apiKeyHeader = Objects.requireNonNull(apiKeyHeader, "API key header cannot be null");
    }

    @Override
    public Mono<String> resolve(ServerWebExchange exchange) {
        return switch (strategy) {
            case IP -> resolveByIp(exchange);
            case USER_ID -> resolveByUserId(exchange);
            case API_KEY -> resolveByApiKey(exchange);
            case COMPOSITE -> resolveComposite(exchange);
            case PATH -> resolveByPath(exchange);
        };
    }

    private Mono<String> resolveByIp(ServerWebExchange exchange) {
        String ip = getClientIp(exchange);
        return Mono.just("ip:" + ip);
    }

    private Mono<String> resolveByUserId(ServerWebExchange exchange) {
        String userId =
                exchange.getRequest()
                        .getHeaders()
                        .getFirst(AuthenticationGatewayFilter.USER_ID_HEADER);
        if (userId == null || userId.isBlank()) {
            // Fall back to IP if no user ID
            return resolveByIp(exchange);
        }
        return Mono.just("user:" + userId);
    }

    private Mono<String> resolveByApiKey(ServerWebExchange exchange) {
        String apiKey = exchange.getRequest().getHeaders().getFirst(apiKeyHeader);
        if (apiKey == null || apiKey.isBlank()) {
            // Fall back to IP if no API key
            return resolveByIp(exchange);
        }
        return Mono.just("apikey:" + apiKey);
    }

    private Mono<String> resolveComposite(ServerWebExchange exchange) {
        String ip = getClientIp(exchange);
        String path = exchange.getRequest().getURI().getPath();
        String method = exchange.getRequest().getMethod().name();
        return Mono.just("composite:" + ip + ":" + method + ":" + normalizePath(path));
    }

    private Mono<String> resolveByPath(ServerWebExchange exchange) {
        String path = normalizePath(exchange.getRequest().getURI().getPath());
        return Mono.just("path:" + path);
    }

    private String getClientIp(ServerWebExchange exchange) {
        // Check for forwarded headers (proxy scenario)
        String xForwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            // X-Forwarded-For can contain multiple IPs, take the first one
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = exchange.getRequest().getHeaders().getFirst("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp;
        }

        // Fall back to remote address
        var remoteAddress = exchange.getRequest().getRemoteAddress();
        if (remoteAddress != null && remoteAddress.getAddress() != null) {
            return remoteAddress.getAddress().getHostAddress();
        }

        return "unknown";
    }

    private String normalizePath(String path) {
        // Normalize path for rate limiting grouping
        // Replace UUIDs and numeric IDs with placeholders
        return path.replaceAll(
                        "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}",
                        "{id}")
                .replaceAll("/\\d+", "/{id}");
    }

    public KeyResolutionStrategy getStrategy() {
        return strategy;
    }

    /** Strategy for resolving rate limit keys. */
    public enum KeyResolutionStrategy {
        /** Rate limit by client IP address */
        IP,
        /** Rate limit by authenticated user ID */
        USER_ID,
        /** Rate limit by API key */
        API_KEY,
        /** Rate limit by combination of IP, method, and path */
        COMPOSITE,
        /** Rate limit by normalized path */
        PATH
    }
}
