package com.jnzader.apigen.core.domain.exception;

import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when an account is locked.
 *
 * <p>Maps to HTTP 423 Locked.
 */
@ResponseStatus(HttpStatus.LOCKED)
public class AccountLockedException extends RuntimeException {

    private final Instant unlockTime;
    private final long remainingSeconds;

    public AccountLockedException(String message) {
        super(message);
        this.unlockTime = null;
        this.remainingSeconds = 0;
    }

    public AccountLockedException(String message, Instant unlockTime) {
        super(message);
        this.unlockTime = unlockTime;
        this.remainingSeconds =
                unlockTime != null
                        ? Math.max(0, unlockTime.getEpochSecond() - Instant.now().getEpochSecond())
                        : 0;
    }

    public AccountLockedException(String message, long remainingSeconds) {
        super(message);
        this.remainingSeconds = remainingSeconds;
        this.unlockTime = Instant.now().plusSeconds(remainingSeconds);
    }

    public Instant getUnlockTime() {
        return unlockTime;
    }

    public long getRemainingSeconds() {
        return remainingSeconds;
    }
}
