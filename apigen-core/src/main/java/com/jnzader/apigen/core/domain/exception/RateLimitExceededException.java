package com.jnzader.apigen.core.domain.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when rate limit is exceeded.
 *
 * <p>Maps to HTTP 429 Too Many Requests.
 */
@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
public class RateLimitExceededException extends RuntimeException {

    private final long retryAfterSeconds;
    private final String tier;
    private final long limit;

    public RateLimitExceededException(String message, long retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
        this.tier = null;
        this.limit = 0;
    }

    public RateLimitExceededException(
            String message, long retryAfterSeconds, String tier, long limit) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
        this.tier = tier;
        this.limit = limit;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }

    public String getTier() {
        return tier;
    }

    public long getLimit() {
        return limit;
    }
}
