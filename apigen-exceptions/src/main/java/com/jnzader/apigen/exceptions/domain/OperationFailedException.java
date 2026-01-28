package com.jnzader.apigen.exceptions.domain;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a service operation fails due to internal reasons. Maps to HTTP 500
 * Internal Server Error.
 */
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class OperationFailedException extends RuntimeException {
    public OperationFailedException(String message) {
        super(message);
    }

    public OperationFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
