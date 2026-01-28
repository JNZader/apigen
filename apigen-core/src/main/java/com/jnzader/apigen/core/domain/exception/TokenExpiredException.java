package com.jnzader.apigen.core.domain.exception;

import java.time.Instant;

/**
 * Exception thrown when a token has expired.
 *
 * <p>Maps to HTTP 401 Unauthorized.
 */
public class TokenExpiredException extends AuthenticationException {

    private final Instant expiredAt;

    public TokenExpiredException(String message) {
        super(message, "TOKEN_EXPIRED");
        this.expiredAt = null;
    }

    public TokenExpiredException(String message, Instant expiredAt) {
        super(message, "TOKEN_EXPIRED");
        this.expiredAt = expiredAt;
    }

    public TokenExpiredException(String message, Throwable cause) {
        super(message, "TOKEN_EXPIRED", cause);
        this.expiredAt = null;
    }

    public Instant getExpiredAt() {
        return expiredAt;
    }
}
