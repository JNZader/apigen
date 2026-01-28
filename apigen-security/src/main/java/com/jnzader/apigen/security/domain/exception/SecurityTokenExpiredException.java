package com.jnzader.apigen.security.domain.exception;

import com.jnzader.apigen.core.domain.exception.TokenExpiredException;
import java.time.Instant;

/**
 * Security-specific exception for expired JWT tokens.
 *
 * <p>Provides additional context about token expiration.
 */
public class SecurityTokenExpiredException extends TokenExpiredException {

    private final String tokenType;

    public SecurityTokenExpiredException(String message) {
        super(message);
        this.tokenType = "unknown";
    }

    public SecurityTokenExpiredException(String message, Instant expiredAt) {
        super(message, expiredAt);
        this.tokenType = "unknown";
    }

    public SecurityTokenExpiredException(String message, Instant expiredAt, String tokenType) {
        super(message, expiredAt);
        this.tokenType = tokenType;
    }

    public SecurityTokenExpiredException(String message, Throwable cause) {
        super(message, cause);
        this.tokenType = "unknown";
    }

    public String getTokenType() {
        return tokenType;
    }
}
