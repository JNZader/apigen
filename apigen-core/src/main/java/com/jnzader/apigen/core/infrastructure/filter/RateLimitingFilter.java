package com.jnzader.apigen.core.infrastructure.filter;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filtro de rate limiting basado en IP del cliente.
 *
 * <p>Características: - Limita el número de requests por IP en una ventana de tiempo - Usa Caffeine
 * cache para almacenamiento eficiente de contadores - Agrega headers estándar de rate limit
 * (X-RateLimit-*) - Retorna 429 Too Many Requests cuando se excede el límite - Configurable via
 * application.yaml
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitingFilter.class);
    private static final String UNKNOWN_IP = "unknown";

    private final int maxRequestsPerWindow;
    private final Duration windowDuration;
    private final Cache<String, AtomicInteger> requestCounts;

    public RateLimitingFilter(
            @Value("${app.rate-limit.max-requests:100}") int maxRequestsPerWindow,
            @Value("${app.rate-limit.window-seconds:60}") int windowSeconds) {

        this.maxRequestsPerWindow = maxRequestsPerWindow;
        this.windowDuration = Duration.ofSeconds(windowSeconds);

        this.requestCounts =
                Caffeine.newBuilder()
                        .expireAfterWrite(windowDuration)
                        .maximumSize(10000) // Máximo 10k IPs en cache
                        .build();

        log.info(
                "Rate limiting configurado: {} requests por {}s",
                maxRequestsPerWindow,
                windowSeconds);
    }

    @Override
    @SuppressWarnings("java:S4449") // El parámetro k nunca es null en Caffeine Cache
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String clientIp = getClientIp(request);
        AtomicInteger counter = requestCounts.get(clientIp, k -> new AtomicInteger(0));

        int currentCount = counter.incrementAndGet();
        int remaining = Math.max(0, maxRequestsPerWindow - currentCount);

        // Agregar headers de rate limit
        response.setHeader("X-RateLimit-Limit", String.valueOf(maxRequestsPerWindow));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));
        response.setHeader("X-RateLimit-Reset", String.valueOf(windowDuration.toSeconds()));

        if (currentCount > maxRequestsPerWindow) {
            log.warn("Rate limit excedido para IP: {} ({} requests)", clientIp, currentCount);

            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter()
                    .write(
                            """
                            {
                                "error": "Too Many Requests",
                                "message": "Has excedido el límite de %d requests por %d segundos",
                                "retryAfter": %d
                            }
                            """
                                    .formatted(
                                            maxRequestsPerWindow,
                                            windowDuration.toSeconds(),
                                            windowDuration.toSeconds()));

            return;
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || UNKNOWN_IP.equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isBlank() || UNKNOWN_IP.equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // No aplicar rate limit a actuator endpoints
        return path.startsWith("/actuator/");
    }
}
