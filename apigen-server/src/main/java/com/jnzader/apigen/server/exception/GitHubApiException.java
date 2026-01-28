package com.jnzader.apigen.server.exception;

import com.jnzader.apigen.core.domain.exception.ExternalServiceException;
import java.time.Instant;

/**
 * Exception thrown when GitHub API operations fail.
 *
 * <p>Extends ExternalServiceException with GitHub-specific information like rate limit details.
 */
public class GitHubApiException extends ExternalServiceException {

    private static final String SERVICE_NAME = "github";

    private final int rateLimitRemaining;
    private final Instant rateLimitReset;

    public GitHubApiException(String message) {
        super(message, SERVICE_NAME);
        this.rateLimitRemaining = -1;
        this.rateLimitReset = null;
    }

    public GitHubApiException(String message, Throwable cause) {
        super(message, SERVICE_NAME, cause);
        this.rateLimitRemaining = -1;
        this.rateLimitReset = null;
    }

    public GitHubApiException(String message, int rateLimitRemaining, Instant rateLimitReset) {
        super(message, SERVICE_NAME);
        this.rateLimitRemaining = rateLimitRemaining;
        this.rateLimitReset = rateLimitReset;
    }

    public GitHubApiException(
            String message, Throwable cause, int rateLimitRemaining, Instant rateLimitReset) {
        super(message, SERVICE_NAME, cause);
        this.rateLimitRemaining = rateLimitRemaining;
        this.rateLimitReset = rateLimitReset;
    }

    public int getRateLimitRemaining() {
        return rateLimitRemaining;
    }

    public Instant getRateLimitReset() {
        return rateLimitReset;
    }

    public boolean hasRateLimitInfo() {
        return rateLimitRemaining >= 0 && rateLimitReset != null;
    }
}
