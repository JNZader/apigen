package com.jnzader.apigen.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jnzader.apigen.gateway.error.GatewayErrorWriter;
import java.util.List;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Global filter for JWT authentication at the gateway level. Validates tokens and extracts claims
 * before forwarding to downstream services.
 *
 * <p>Returns RFC 7807 compliant error responses for authentication failures.
 */
public class AuthenticationGatewayFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationGatewayFilter.class);

    public static final String USER_ID_HEADER = "X-User-ID";
    public static final String USER_ROLES_HEADER = "X-User-Roles";

    private final Function<String, AuthResult> tokenValidator;
    private final List<String> excludedPaths;
    private final String headerName;
    private final String tokenPrefix;
    private final AntPathMatcher pathMatcher;
    private final GatewayErrorWriter errorWriter;

    public AuthenticationGatewayFilter(
            Function<String, AuthResult> tokenValidator,
            List<String> excludedPaths,
            ObjectMapper objectMapper) {
        this(tokenValidator, excludedPaths, "Authorization", "Bearer ", objectMapper);
    }

    public AuthenticationGatewayFilter(
            Function<String, AuthResult> tokenValidator,
            List<String> excludedPaths,
            String headerName,
            String tokenPrefix,
            ObjectMapper objectMapper) {
        this.tokenValidator = tokenValidator;
        this.excludedPaths = excludedPaths;
        this.headerName = headerName;
        this.tokenPrefix = tokenPrefix;
        this.pathMatcher = new AntPathMatcher();
        this.errorWriter = new GatewayErrorWriter(objectMapper);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // Check if path is excluded from authentication
        if (isExcludedPath(path)) {
            log.debug("Path {} is excluded from authentication", path);
            return chain.filter(exchange);
        }

        // Extract token from header
        String authHeader = request.getHeaders().getFirst(headerName);
        if (authHeader == null || !authHeader.startsWith(tokenPrefix)) {
            log.warn("Missing or invalid {} header for path {}", headerName, path);
            return unauthorizedResponse(exchange, "Missing or invalid authorization header");
        }

        String token = authHeader.substring(tokenPrefix.length());

        // Validate token
        AuthResult result = tokenValidator.apply(token);
        if (!result.isAuthenticated()) {
            String errorMessage = result.getErrorMessage().orElse("Authentication failed");
            if (log.isWarnEnabled()) {
                log.warn("Authentication failed for path {}: {}", path, errorMessage);
            }
            return unauthorizedResponse(exchange, errorMessage, result.getErrorCode().orElse(null));
        }

        // Add user info to headers for downstream services
        ServerHttpRequest mutatedRequest =
                request.mutate()
                        .header(USER_ID_HEADER, result.getUserId().orElse(""))
                        .header(USER_ROLES_HEADER, String.join(",", result.roles()))
                        .build();

        if (log.isDebugEnabled()) {
            log.debug(
                    "Authentication successful for user {} on path {}",
                    result.getUserId().orElse("unknown"),
                    path);
        }

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    private boolean isExcludedPath(String path) {
        return excludedPaths.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    private Mono<Void> unauthorizedResponse(ServerWebExchange exchange, String detail) {
        exchange.getResponse().getHeaders().add(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        return errorWriter.writeUnauthorized(exchange, detail);
    }

    private Mono<Void> unauthorizedResponse(
            ServerWebExchange exchange, String detail, String errorCode) {
        exchange.getResponse().getHeaders().add(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        if (errorCode != null) {
            return errorWriter.writeUnauthorized(exchange, detail, errorCode);
        }
        return errorWriter.writeUnauthorized(exchange, detail);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 100;
    }

    /** Result of token authentication. */
    public record AuthResult(
            boolean authenticated,
            String userId,
            List<String> roles,
            String errorMessage,
            String errorCode) {

        public static AuthResult success(String userId, List<String> roles) {
            return new AuthResult(true, userId, roles, null, null);
        }

        public static AuthResult failure(String errorMessage) {
            return new AuthResult(false, null, List.of(), errorMessage, null);
        }

        public static AuthResult failure(String errorMessage, String errorCode) {
            return new AuthResult(false, null, List.of(), errorMessage, errorCode);
        }

        public boolean isAuthenticated() {
            return authenticated;
        }

        public java.util.Optional<String> getUserId() {
            return java.util.Optional.ofNullable(userId);
        }

        public java.util.Optional<String> getErrorMessage() {
            return java.util.Optional.ofNullable(errorMessage);
        }

        public java.util.Optional<String> getErrorCode() {
            return java.util.Optional.ofNullable(errorCode);
        }
    }
}
