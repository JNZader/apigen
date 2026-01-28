package com.jnzader.apigen.exceptions.domain;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when an external service call fails.
 *
 * <p>Maps to HTTP 502 Bad Gateway or 503 Service Unavailable depending on the nature of the
 * failure.
 */
@ResponseStatus(HttpStatus.BAD_GATEWAY)
public class ExternalServiceException extends RuntimeException {

    private final String serviceName;
    private final String originalError;

    public ExternalServiceException(String message, String serviceName) {
        super(message);
        this.serviceName = serviceName;
        this.originalError = null;
    }

    public ExternalServiceException(String message, String serviceName, Throwable cause) {
        super(message, cause);
        this.serviceName = serviceName;
        this.originalError = cause != null ? cause.getMessage() : null;
    }

    public ExternalServiceException(
            String message, String serviceName, String originalError, Throwable cause) {
        super(message, cause);
        this.serviceName = serviceName;
        this.originalError = originalError;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getOriginalError() {
        return originalError;
    }
}
