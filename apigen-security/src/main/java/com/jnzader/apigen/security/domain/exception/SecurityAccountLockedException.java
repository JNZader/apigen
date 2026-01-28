package com.jnzader.apigen.security.domain.exception;

import com.jnzader.apigen.core.domain.exception.AccountLockedException;
import java.time.Instant;

/**
 * Security-specific exception for locked accounts.
 *
 * <p>Provides additional context about why the account was locked.
 */
public class SecurityAccountLockedException extends AccountLockedException {

    private final LockReason lockReason;
    private final int failedAttempts;

    public SecurityAccountLockedException(String message, Instant unlockTime) {
        super(message, unlockTime);
        this.lockReason = LockReason.TOO_MANY_FAILED_ATTEMPTS;
        this.failedAttempts = 0;
    }

    public SecurityAccountLockedException(
            String message, Instant unlockTime, LockReason lockReason, int failedAttempts) {
        super(message, unlockTime);
        this.lockReason = lockReason;
        this.failedAttempts = failedAttempts;
    }

    public SecurityAccountLockedException(
            String message, long remainingSeconds, LockReason lockReason, int failedAttempts) {
        super(message, remainingSeconds);
        this.lockReason = lockReason;
        this.failedAttempts = failedAttempts;
    }

    public LockReason getLockReason() {
        return lockReason;
    }

    public int getFailedAttempts() {
        return failedAttempts;
    }

    /** Reasons why an account may be locked. */
    public enum LockReason {
        TOO_MANY_FAILED_ATTEMPTS("Too many failed login attempts"),
        SUSPICIOUS_ACTIVITY("Suspicious activity detected"),
        ADMIN_ACTION("Locked by administrator"),
        SECURITY_POLICY("Security policy violation");

        private final String description;

        LockReason(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}
