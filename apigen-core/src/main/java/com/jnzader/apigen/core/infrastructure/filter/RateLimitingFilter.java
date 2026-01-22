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
 * Rate limiting filter based on client IP.
 *
 * <p>Features: - Limits the number of requests per IP within a time window - Uses Caffeine cache
 * for efficient counter storage - Adds standard rate limit headers (X-RateLimit-*) - Returns 429
 * Too Many Requests when limit is exceeded - Configurable via application.yaml
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitingFilter.class);
    private static final String UNKNOWN_IP = "unknown";

    private final boolean enabled;
    private final int maxRequestsPerWindow;
    private final Duration windowDuration;
    private final Cache<String, AtomicInteger> requestCounts;

    public RateLimitingFilter(
            @Value("${app.rate-limit.enabled:true}") boolean enabled,
            @Value("${app.rate-limit.max-requests:100}") int maxRequestsPerWindow,
            @Value("${app.rate-limit.window-seconds:60}") int windowSeconds) {

        this.enabled = enabled;
        this.maxRequestsPerWindow = maxRequestsPerWindow;
        this.windowDuration = Duration.ofSeconds(windowSeconds);

        this.requestCounts =
                Caffeine.newBuilder()
                        .expireAfterWrite(windowDuration)
                        .maximumSize(10000) // Maximum 10k IPs in cache
                        .build();

        if (enabled) {
            log.info(
                    "Rate limiting configured: {} requests per {}s",
                    maxRequestsPerWindow,
                    windowSeconds);
        } else {
            log.info("Rate limiting is DISABLED");
        }
    }

    @Override
    @SuppressWarnings("java:S4449") // The k parameter is never null in Caffeine Cache
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String clientIp = getClientIp(request);
        AtomicInteger counter = requestCounts.get(clientIp, k -> new AtomicInteger(0));

        int currentCount = counter.incrementAndGet();
        int remaining = Math.max(0, maxRequestsPerWindow - currentCount);

        // Add rate limit headers
        response.setHeader("X-RateLimit-Limit", String.valueOf(maxRequestsPerWindow));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));
        response.setHeader("X-RateLimit-Reset", String.valueOf(windowDuration.toSeconds()));

        if (currentCount > maxRequestsPerWindow) {
            log.warn("Rate limit exceeded for IP: {} ({} requests)", clientIp, currentCount);

            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter()
                    .write(
                            """
                            {
                                "error": "Too Many Requests",
                                "message": "You have exceeded the limit of %d requests per %d seconds",
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
        // Skip if rate limiting is disabled
        if (!enabled) {
            return true;
        }
        String path = request.getRequestURI();
        // Do not apply rate limit to actuator endpoints
        return path.startsWith("/actuator/");
    }
}
