package com.jnzader.apigen.security.infrastructure.ratelimit;

import com.jnzader.apigen.security.infrastructure.config.SecurityProperties;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filtro de Rate Limiting para la API usando Bucket4j.
 *
 * <p>Aplica rate limiting basado en IP a todos los endpoints de la API. Usa diferentes límites para
 * endpoints de autenticación (más restrictivos) vs. endpoints generales.
 *
 * <p>Headers de respuesta: - X-RateLimit-Limit: Límite total de requests - X-RateLimit-Remaining:
 * Requests restantes - X-RateLimit-Reset: Segundos hasta reset del límite - Retry-After: Segundos a
 * esperar (solo en 429)
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
@ConditionalOnProperty(
        name = "apigen.security.rate-limit.enabled",
        havingValue = "true",
        matchIfMissing = true)
@ConditionalOnBean(RateLimitService.class)
public class ApiRateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ApiRateLimitFilter.class);
    private static final String UNKNOWN_IP = "unknown";

    private final RateLimitService rateLimitService;
    private final SecurityProperties securityProperties;

    public ApiRateLimitFilter(
            RateLimitService rateLimitService, SecurityProperties securityProperties) {
        this.rateLimitService = rateLimitService;
        this.securityProperties = securityProperties;
        log.info(
                "API Rate Limit Filter initialized (storage: {})",
                securityProperties.getRateLimit().getStorageMode());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Skip rate limiting for static resources and actuator health
        if (shouldSkipRateLimiting(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = getClientIp(request);
        boolean isAuthEndpoint = isAuthEndpoint(request);

        ConsumptionProbe probe =
                rateLimitService.tryConsumeAndReturnRemaining(clientIp, isAuthEndpoint);

        // Add rate limit headers
        addRateLimitHeaders(response, probe, isAuthEndpoint);

        if (probe.isConsumed()) {
            // Request allowed
            filterChain.doFilter(request, response);
        } else {
            // Rate limit exceeded
            long waitSeconds = TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill());
            log.warn(
                    "Rate limit exceeded for IP: {} on {} endpoint, wait: {}s",
                    clientIp,
                    isAuthEndpoint ? "auth" : "api",
                    waitSeconds);
            sendRateLimitResponse(response, waitSeconds, isAuthEndpoint);
        }
    }

    private boolean shouldSkipRateLimiting(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.startsWith("/actuator/health")
                || uri.startsWith("/actuator/info")
                || uri.startsWith("/swagger-ui")
                || uri.startsWith("/v3/api-docs")
                || uri.startsWith("/favicon.ico")
                || uri.endsWith(".js")
                || uri.endsWith(".css")
                || uri.endsWith(".png")
                || uri.endsWith(".jpg")
                || uri.endsWith(".ico");
    }

    private boolean isAuthEndpoint(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String method = request.getMethod();

        // Solo POST a endpoints de auth son considerados "auth endpoints" para rate limiting
        return "POST".equalsIgnoreCase(method)
                && (uri.contains("/auth/login")
                        || uri.contains("/auth/register")
                        || uri.contains("/auth/refresh"));
    }

    private String getClientIp(HttpServletRequest request) {
        String[] headerNames = {
            "X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP", "WL-Proxy-Client-IP"
        };

        for (String header : headerNames) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isBlank() && !UNKNOWN_IP.equalsIgnoreCase(ip)) {
                // X-Forwarded-For puede tener múltiples IPs
                int commaIndex = ip.indexOf(',');
                return commaIndex > 0 ? ip.substring(0, commaIndex).trim() : ip.trim();
            }
        }

        return request.getRemoteAddr();
    }

    private void addRateLimitHeaders(
            HttpServletResponse response, ConsumptionProbe probe, boolean isAuthEndpoint) {
        SecurityProperties.RateLimitProperties config = securityProperties.getRateLimit();

        long limit = isAuthEndpoint ? config.getAuthBurstCapacity() : config.getBurstCapacity();
        long remaining = probe.getRemainingTokens();
        long resetSeconds = TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill());

        response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));
        response.setHeader("X-RateLimit-Reset", String.valueOf(resetSeconds));
    }

    private void sendRateLimitResponse(
            HttpServletResponse response, long waitSeconds, boolean isAuthEndpoint)
            throws IOException {

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Retry-After", String.valueOf(waitSeconds));

        String endpointType = isAuthEndpoint ? "autenticación" : "API";
        String jsonResponse =
                String.format(
                        """
                        {
                            "type": "about:blank",
                            "title": "Too Many Requests",
                            "status": 429,
                            "detail": "Límite de requests excedido para %s. Intente nuevamente en %d segundos.",
                            "instance": "%s"
                        }
                        """,
                        endpointType, waitSeconds, isAuthEndpoint ? "/auth" : "/api");

        response.getWriter().write(jsonResponse);
    }
}
