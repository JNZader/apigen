package com.jnzader.apigen.security.infrastructure.ratelimit;

import com.jnzader.apigen.security.infrastructure.config.SecurityProperties;
import com.jnzader.apigen.security.infrastructure.config.SecurityProperties.RateLimitProperties.TierConfig;
import com.jnzader.apigen.security.infrastructure.network.ClientIpResolver;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.annotation.Nullable;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Rate limiting filter for the API using Bucket4j.
 *
 * <p>Applies IP or user-based rate limiting to all API endpoints. Supports:
 *
 * <ul>
 *   <li>Traditional IP-based rate limiting (general and auth endpoints)
 *   <li>User tier-based rate limiting (ANONYMOUS, FREE, BASIC, PRO)
 * </ul>
 *
 * <p>Response headers:
 *
 * <ul>
 *   <li>X-RateLimit-Limit: Total request limit
 *   <li>X-RateLimit-Remaining: Remaining requests
 *   <li>X-RateLimit-Reset: Seconds until limit reset
 *   <li>X-RateLimit-Tier: Current user tier (if tiers enabled)
 *   <li>Retry-After: Seconds to wait (only on 429)
 * </ul>
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
    private static final String TIER_HEADER = "X-RateLimit-Tier";

    private final RateLimitService rateLimitService;
    private final SecurityProperties securityProperties;
    private final ClientIpResolver clientIpResolver;
    private final RateLimitTierResolver tierResolver;

    public ApiRateLimitFilter(
            RateLimitService rateLimitService,
            SecurityProperties securityProperties,
            ClientIpResolver clientIpResolver,
            @Nullable RateLimitTierResolver tierResolver) {
        this.rateLimitService = rateLimitService;
        this.securityProperties = securityProperties;
        this.clientIpResolver = clientIpResolver;
        this.tierResolver = tierResolver;

        String mode = rateLimitService.isTiersEnabled() ? "tier-based" : "ip-based";
        log.info(
                "API Rate Limit Filter initialized (storage: {}, mode: {})",
                securityProperties.getRateLimit().getStorageMode(),
                mode);
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

        // Use tier-based rate limiting if enabled
        if (rateLimitService.isTiersEnabled() && tierResolver != null) {
            handleTierBasedRateLimiting(request, response, filterChain, clientIp);
        } else {
            handleIpBasedRateLimiting(request, response, filterChain, clientIp);
        }
    }

    private void handleTierBasedRateLimiting(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain,
            String clientIp)
            throws ServletException, IOException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        RateLimitTier tier = tierResolver.resolve(authentication, request);
        String userIdentifier = tierResolver.getUserIdentifier(authentication, clientIp);

        ConsumptionProbe probe =
                rateLimitService.tryConsumeForTierAndReturnRemaining(userIdentifier, tier);

        // Add rate limit headers with tier info
        addTierRateLimitHeaders(response, probe, tier);

        if (probe.isConsumed()) {
            filterChain.doFilter(request, response);
        } else {
            long waitSeconds = TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill());
            log.warn(
                    "Rate limit exceeded for {} (tier: {}) on {}, wait: {}s",
                    userIdentifier,
                    tier,
                    request.getRequestURI(),
                    waitSeconds);
            sendTierRateLimitResponse(response, waitSeconds, tier);
        }
    }

    private void handleIpBasedRateLimiting(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain,
            String clientIp)
            throws ServletException, IOException {

        boolean isAuthEndpoint = isAuthEndpoint(request);

        ConsumptionProbe probe =
                rateLimitService.tryConsumeAndReturnRemaining(clientIp, isAuthEndpoint);

        // Add rate limit headers
        addRateLimitHeaders(response, probe, isAuthEndpoint);

        if (probe.isConsumed()) {
            filterChain.doFilter(request, response);
        } else {
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

        // Only POST to auth endpoints are considered "auth endpoints" for rate limiting
        return "POST".equalsIgnoreCase(method)
                && (uri.contains("/auth/login")
                        || uri.contains("/auth/register")
                        || uri.contains("/auth/refresh"));
    }

    private String getClientIp(HttpServletRequest request) {
        return clientIpResolver.resolveClientIp(request);
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

    private void addTierRateLimitHeaders(
            HttpServletResponse response, ConsumptionProbe probe, RateLimitTier tier) {
        TierConfig tierConfig = rateLimitService.getTierConfig(tier);

        long limit = tierConfig.getBurstCapacity();
        long remaining = probe.getRemainingTokens();
        long resetSeconds = TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill());

        response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));
        response.setHeader("X-RateLimit-Reset", String.valueOf(resetSeconds));
        response.setHeader(TIER_HEADER, tier.getName());
    }

    private void sendRateLimitResponse(
            HttpServletResponse response, long waitSeconds, boolean isAuthEndpoint)
            throws IOException {

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Retry-After", String.valueOf(waitSeconds));

        String endpointType = isAuthEndpoint ? "authentication" : "API";
        String jsonResponse =
                String.format(
                        """
                        {
                            "type": "about:blank",
                            "title": "Too Many Requests",
                            "status": 429,
                            "detail": "Request limit exceeded for %s. Please try again in %d seconds.",
                            "instance": "%s"
                        }
                        """,
                        endpointType, waitSeconds, isAuthEndpoint ? "/auth" : "/api");

        response.getWriter().write(jsonResponse);
    }

    private void sendTierRateLimitResponse(
            HttpServletResponse response, long waitSeconds, RateLimitTier tier) throws IOException {

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Retry-After", String.valueOf(waitSeconds));
        response.setHeader(TIER_HEADER, tier.getName());

        TierConfig tierConfig = rateLimitService.getTierConfig(tier);
        String jsonResponse =
                String.format(
                        """
                        {
                            "type": "urn:apigen:problem:rate-limit-exceeded",
                            "title": "Too Many Requests",
                            "status": 429,
                            "detail": "Rate limit exceeded for tier '%s'. Retry in %d seconds.",
                            "tier": "%s",
                            "limit": %d,
                            "upgradeUrl": "/api/plans"
                        }
                        """,
                        tier.getName(),
                        waitSeconds,
                        tier.getName(),
                        tierConfig.getRequestsPerSecond());

        response.getWriter().write(jsonResponse);
    }
}
