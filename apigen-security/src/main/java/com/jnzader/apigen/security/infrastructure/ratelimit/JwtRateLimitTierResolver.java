package com.jnzader.apigen.security.infrastructure.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Default implementation of {@link RateLimitTierResolver} that resolves tier from JWT claims.
 *
 * <p>Tier resolution order:
 *
 * <ol>
 *   <li>Check 'tier' claim in JWT
 *   <li>Check 'subscription' claim in JWT
 *   <li>Check 'plan' claim in JWT
 *   <li>Check authorities for tier-related roles (ROLE_TIER_PRO, etc.)
 *   <li>Default to FREE for authenticated users, ANONYMOUS for unauthenticated
 * </ol>
 *
 * <p>JWT example:
 *
 * <pre>{@code
 * {
 *   "sub": "user123",
 *   "tier": "pro",
 *   ...
 * }
 * }</pre>
 *
 * <p>Or with authorities:
 *
 * <pre>{@code
 * {
 *   "sub": "user123",
 *   "authorities": ["ROLE_USER", "ROLE_TIER_BASIC"],
 *   ...
 * }
 * }</pre>
 */
@Component
@ConditionalOnProperty(name = "apigen.security.rate-limit.tiers-enabled", havingValue = "true")
public class JwtRateLimitTierResolver implements RateLimitTierResolver {

    private static final Logger log = LoggerFactory.getLogger(JwtRateLimitTierResolver.class);

    private static final String TIER_CLAIM = "tier";
    private static final String SUBSCRIPTION_CLAIM = "subscription";
    private static final String PLAN_CLAIM = "plan";
    private static final String ROLE_TIER_PREFIX = "ROLE_TIER_";

    @Override
    public RateLimitTier resolve(Authentication authentication, HttpServletRequest request) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return RateLimitTier.ANONYMOUS;
        }

        // Try to resolve from JWT claims
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            RateLimitTier tier = resolveFromJwt(jwtAuth.getToken());
            if (tier != null) {
                log.debug("Resolved tier {} from JWT claims for user {}", tier, jwtAuth.getName());
                return tier;
            }
        }

        // Try to resolve from authorities
        RateLimitTier tierFromAuthorities = resolveFromAuthorities(authentication);
        if (tierFromAuthorities != null) {
            log.debug(
                    "Resolved tier {} from authorities for user {}",
                    tierFromAuthorities,
                    authentication.getName());
            return tierFromAuthorities;
        }

        // Default to FREE for authenticated users
        log.debug("No tier found for user {}, defaulting to FREE", authentication.getName());
        return RateLimitTier.FREE;
    }

    private RateLimitTier resolveFromJwt(Jwt jwt) {
        // Check 'tier' claim
        String tier = jwt.getClaimAsString(TIER_CLAIM);
        if (tier != null && !tier.isBlank()) {
            return RateLimitTier.fromString(tier);
        }

        // Check 'subscription' claim
        String subscription = jwt.getClaimAsString(SUBSCRIPTION_CLAIM);
        if (subscription != null && !subscription.isBlank()) {
            return RateLimitTier.fromString(subscription);
        }

        // Check 'plan' claim
        String plan = jwt.getClaimAsString(PLAN_CLAIM);
        if (plan != null && !plan.isBlank()) {
            return RateLimitTier.fromString(plan);
        }

        return null;
    }

    private RateLimitTier resolveFromAuthorities(Authentication authentication) {
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String role = authority.getAuthority();
            if (role != null && role.toUpperCase().startsWith(ROLE_TIER_PREFIX)) {
                String tierName = role.substring(ROLE_TIER_PREFIX.length());
                RateLimitTier tier = RateLimitTier.fromString(tierName);
                if (tier != RateLimitTier.ANONYMOUS) {
                    return tier;
                }
            }
        }
        return null;
    }

    @Override
    public String getUserIdentifier(Authentication authentication, String clientIp) {
        if (authentication != null && authentication.isAuthenticated()) {
            // Use 'sub' claim from JWT if available
            if (authentication instanceof JwtAuthenticationToken jwtAuth) {
                String subject = jwtAuth.getToken().getSubject();
                if (subject != null && !subject.isBlank()) {
                    return "user:" + subject;
                }
            }
            // Fallback to authentication name
            String name = authentication.getName();
            if (name != null && !name.isBlank()) {
                return "user:" + name;
            }
        }
        return "ip:" + clientIp;
    }
}
