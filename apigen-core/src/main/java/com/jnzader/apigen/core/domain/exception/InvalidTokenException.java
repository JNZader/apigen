package com.jnzader.apigen.core.domain.exception;

/**
 * Exception thrown when a token is invalid (malformed, signature mismatch, etc.).
 *
 * <p>Maps to HTTP 401 Unauthorized.
 */
public class InvalidTokenException extends AuthenticationException {

    public InvalidTokenException(String message) {
        super(message, "INVALID_TOKEN");
    }

    public InvalidTokenException(String message, Throwable cause) {
        super(message, "INVALID_TOKEN", cause);
    }
}
