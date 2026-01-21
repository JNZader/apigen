package com.jnzader.apigen.security.infrastructure.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import org.springframework.security.core.Authentication;

/**
 * Interface for resolving the rate limit tier for a user.
 *
 * <p>Implementations can determine the tier from various sources:
 *
 * <ul>
 *   <li>JWT claims (e.g., 'tier' or 'subscription' claim)
 *   <li>Database lookup based on user ID
 *   <li>External service (e.g., billing/subscription service)
 *   <li>HTTP headers (e.g., API key tier)
 * </ul>
 *
 * <p>Example JWT claim-based implementation:
 *
 * <pre>{@code
 * @Component
 * public class JwtTierResolver implements RateLimitTierResolver {
 *     @Override
 *     public RateLimitTier resolve(Authentication auth, HttpServletRequest request) {
 *         if (auth instanceof JwtAuthenticationToken jwt) {
 *             String tier = jwt.getToken().getClaimAsString("tier");
 *             return RateLimitTier.fromString(tier);
 *         }
 *         return RateLimitTier.ANONYMOUS;
 *     }
 * }
 * }</pre>
 */
public interface RateLimitTierResolver {

    /**
     * Resolves the rate limit tier for the current request.
     *
     * @param authentication the current authentication, may be null for anonymous users
     * @param request the HTTP servlet request
     * @return the resolved tier, never null
     */
    RateLimitTier resolve(Authentication authentication, HttpServletRequest request);

    /**
     * Extracts the user identifier for rate limiting purposes.
     *
     * <p>This is used as part of the bucket key to ensure each user has their own rate limit
     * bucket. For authenticated users, this typically returns the user ID. For anonymous users, it
     * may return the client IP.
     *
     * @param authentication the current authentication, may be null
     * @param clientIp the client IP address as fallback
     * @return the user identifier for rate limiting
     */
    default String getUserIdentifier(Authentication authentication, String clientIp) {
        if (authentication != null && authentication.isAuthenticated()) {
            return Optional.ofNullable(authentication.getName()).orElse(clientIp);
        }
        return clientIp;
    }
}
