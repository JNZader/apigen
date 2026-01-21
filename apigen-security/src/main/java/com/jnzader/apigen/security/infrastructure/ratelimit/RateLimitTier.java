package com.jnzader.apigen.security.infrastructure.ratelimit;

/**
 * Rate limit tiers for user-based rate limiting.
 *
 * <p>Each tier defines different rate limits for API access:
 *
 * <ul>
 *   <li><b>ANONYMOUS</b>: Unauthenticated users (most restrictive)
 *   <li><b>FREE</b>: Free tier users
 *   <li><b>BASIC</b>: Basic subscription users
 *   <li><b>PRO</b>: Professional subscription users (most permissive)
 * </ul>
 *
 * <p>Configuration example:
 *
 * <pre>{@code
 * apigen:
 *   security:
 *     rate-limit:
 *       tiers:
 *         anonymous:
 *           requests-per-second: 10
 *           burst-capacity: 20
 *         free:
 *           requests-per-second: 50
 *           burst-capacity: 100
 *         basic:
 *           requests-per-second: 200
 *           burst-capacity: 400
 *         pro:
 *           requests-per-second: 1000
 *           burst-capacity: 2000
 * }</pre>
 */
public enum RateLimitTier {

    /** Unauthenticated users - most restrictive limits. */
    ANONYMOUS("anonymous", 10, 20),

    /** Free tier users - limited access. */
    FREE("free", 50, 100),

    /** Basic subscription users - moderate limits. */
    BASIC("basic", 200, 400),

    /** Professional subscription users - high limits. */
    PRO("pro", 1000, 2000);

    private final String name;
    private final int defaultRequestsPerSecond;
    private final int defaultBurstCapacity;

    RateLimitTier(String name, int defaultRequestsPerSecond, int defaultBurstCapacity) {
        this.name = name;
        this.defaultRequestsPerSecond = defaultRequestsPerSecond;
        this.defaultBurstCapacity = defaultBurstCapacity;
    }

    /** Returns the tier name (lowercase). */
    public String getName() {
        return name;
    }

    /** Returns the default requests per second for this tier. */
    public int getDefaultRequestsPerSecond() {
        return defaultRequestsPerSecond;
    }

    /** Returns the default burst capacity for this tier. */
    public int getDefaultBurstCapacity() {
        return defaultBurstCapacity;
    }

    /**
     * Parses a tier from a string value.
     *
     * @param value the tier name (case-insensitive)
     * @return the matching tier, or ANONYMOUS if not found
     */
    public static RateLimitTier fromString(String value) {
        if (value == null || value.isBlank()) {
            return ANONYMOUS;
        }
        for (RateLimitTier tier : values()) {
            if (tier.name.equalsIgnoreCase(value) || tier.name().equalsIgnoreCase(value)) {
                return tier;
            }
        }
        return ANONYMOUS;
    }
}
