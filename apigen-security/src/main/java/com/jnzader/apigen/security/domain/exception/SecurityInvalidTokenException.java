package com.jnzader.apigen.security.domain.exception;

import com.jnzader.apigen.core.domain.exception.InvalidTokenException;

/**
 * Security-specific exception for invalid JWT tokens.
 *
 * <p>Wraps the core InvalidTokenException with additional security context.
 */
public class SecurityInvalidTokenException extends InvalidTokenException {

    private final TokenInvalidReason reason;

    public SecurityInvalidTokenException(String message, TokenInvalidReason reason) {
        super(message);
        this.reason = reason;
    }

    public SecurityInvalidTokenException(
            String message, TokenInvalidReason reason, Throwable cause) {
        super(message, cause);
        this.reason = reason;
    }

    public TokenInvalidReason getReason() {
        return reason;
    }

    /** Specific reasons why a token may be invalid. */
    public enum TokenInvalidReason {
        MALFORMED("Token format is invalid"),
        SIGNATURE_MISMATCH("Token signature verification failed"),
        UNSUPPORTED("Token type is not supported"),
        EMPTY_CLAIMS("Token claims are empty"),
        WRONG_TYPE("Token type mismatch (expected access/refresh)"),
        BLACKLISTED("Token has been revoked");

        private final String description;

        TokenInvalidReason(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}
