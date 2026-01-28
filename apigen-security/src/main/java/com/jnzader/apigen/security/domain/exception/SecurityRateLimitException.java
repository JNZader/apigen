package com.jnzader.apigen.security.domain.exception;

import com.jnzader.apigen.core.domain.exception.RateLimitExceededException;

/**
 * Security-specific exception for rate limit exceeded scenarios.
 *
 * <p>Provides additional context about which endpoint type was rate limited.
 */
public class SecurityRateLimitException extends RateLimitExceededException {

    private final EndpointType endpointType;

    public SecurityRateLimitException(String message, long retryAfterSeconds) {
        super(message, retryAfterSeconds);
        this.endpointType = EndpointType.API;
    }

    public SecurityRateLimitException(
            String message, long retryAfterSeconds, EndpointType endpointType) {
        super(message, retryAfterSeconds);
        this.endpointType = endpointType;
    }

    public SecurityRateLimitException(
            String message,
            long retryAfterSeconds,
            String tier,
            long limit,
            EndpointType endpointType) {
        super(message, retryAfterSeconds, tier, limit);
        this.endpointType = endpointType;
    }

    public EndpointType getEndpointType() {
        return endpointType;
    }

    /** Types of endpoints that can be rate limited. */
    public enum EndpointType {
        AUTHENTICATION("authentication"),
        API("API"),
        PUBLIC("public");

        private final String displayName;

        EndpointType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
