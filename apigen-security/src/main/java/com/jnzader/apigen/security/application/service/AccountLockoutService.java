package com.jnzader.apigen.security.application.service;

import com.jnzader.apigen.security.domain.entity.User;
import com.jnzader.apigen.security.domain.repository.UserRepository;
import com.jnzader.apigen.security.infrastructure.config.SecurityProperties;
import com.jnzader.apigen.security.infrastructure.config.SecurityProperties.AccountLockoutProperties;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing account lockout due to failed authentication attempts.
 *
 * <p>Protects against brute force attacks by temporarily locking accounts after multiple failed
 * login attempts.
 *
 * <p>Features:
 *
 * <ul>
 *   <li>Configurable temporary lockout (default: 15 minutes after 5 attempts)
 *   <li>Automatic counter reset after inactivity period
 *   <li>Support for permanent lockout after multiple lockouts
 *   <li>Security event audit logging
 * </ul>
 */
@Service
@ConditionalOnProperty(name = "apigen.security.enabled", havingValue = "true")
public class AccountLockoutService {

    private static final Logger log = LoggerFactory.getLogger(AccountLockoutService.class);

    private final UserRepository userRepository;
    private final AccountLockoutProperties lockoutProperties;

    public AccountLockoutService(
            UserRepository userRepository, SecurityProperties securityProperties) {
        this.userRepository = userRepository;
        this.lockoutProperties = securityProperties.getAccountLockout();
    }

    /**
     * Records a failed login attempt for a user.
     *
     * <p>Increments the failed attempt counter and locks the account if the maximum is exceeded.
     *
     * @param username username that failed authentication
     */
    @Transactional
    public void recordFailedAttempt(String username) {
        if (!lockoutProperties.isEnabled()) {
            return;
        }

        Optional<User> optionalUser = userRepository.findByUsername(username);
        if (optionalUser.isEmpty()) {
            // User doesn't exist - don't reveal this information
            log.debug(
                    "SECURITY: Failed login attempt for non-existent user (not revealing in"
                            + " response)");
            return;
        }

        User user = optionalUser.get();

        // Check if counter should be reset due to inactivity
        if (shouldResetFailedAttempts(user)) {
            user.setFailedAttemptCount(0);
            log.debug("SECURITY: Reset failed attempts for user '{}' due to inactivity", username);
        }

        // Increment counter
        int newCount = user.getFailedAttemptCount() + 1;
        user.setFailedAttemptCount(newCount);
        user.setLastFailedAttemptAt(Instant.now());

        log.info(
                "SECURITY: Failed login attempt #{} for user '{}' (max: {})",
                newCount,
                username,
                lockoutProperties.getMaxFailedAttempts());

        // Check if account should be locked
        if (newCount >= lockoutProperties.getMaxFailedAttempts()) {
            lockAccount(user);
        }

        userRepository.save(user);
    }

    /**
     * Records a successful login and resets the failed attempt counters.
     *
     * @param username username that authenticated successfully
     */
    @Transactional
    public void recordSuccessfulLogin(String username) {
        if (!lockoutProperties.isEnabled()) {
            return;
        }

        Optional<User> optionalUser = userRepository.findByUsername(username);
        if (optionalUser.isEmpty()) {
            return;
        }

        User user = optionalUser.get();

        // Only reset if there were failed attempts or temporary lockout
        if (user.getFailedAttemptCount() > 0 || user.getLockedUntil() != null) {
            log.info(
                    "SECURITY: Successful login for user '{}'. Resetting {} failed attempts",
                    username,
                    user.getFailedAttemptCount());

            user.setFailedAttemptCount(0);
            user.setLockedUntil(null);
            user.setLastFailedAttemptAt(null);

            userRepository.save(user);
        }
    }

    /**
     * Checks if an account is currently locked.
     *
     * @param username username to check
     * @return true if the account is locked, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean isAccountLocked(String username) {
        if (!lockoutProperties.isEnabled()) {
            return false;
        }

        Optional<User> optionalUser = userRepository.findByUsername(username);
        if (optionalUser.isEmpty()) {
            return false;
        }

        User user = optionalUser.get();
        return !user.isAccountNonLocked();
    }

    /**
     * Gets the remaining lockout duration for an account.
     *
     * @param username username
     * @return remaining lockout duration, or Duration.ZERO if not locked
     */
    @Transactional(readOnly = true)
    public Duration getRemainingLockoutDuration(String username) {
        Optional<User> optionalUser = userRepository.findByUsername(username);
        if (optionalUser.isEmpty()) {
            return Duration.ZERO;
        }

        User user = optionalUser.get();

        // Permanent lockout
        if (!user.isAccountNonLocked() && user.getLockedUntil() == null) {
            return Duration.ofDays(365 * 100); // Effectively "infinite"
        }

        // Temporary lockout
        if (user.getLockedUntil() != null) {
            Duration remaining = Duration.between(Instant.now(), user.getLockedUntil());
            return remaining.isNegative() ? Duration.ZERO : remaining;
        }

        return Duration.ZERO;
    }

    /**
     * Manually unlocks an account (requires admin permissions).
     *
     * @param username username to unlock
     * @return true if unlocked, false if user doesn't exist
     */
    @Transactional
    public boolean unlockAccount(String username) {
        Optional<User> optionalUser = userRepository.findByUsername(username);
        if (optionalUser.isEmpty()) {
            return false;
        }

        User user = optionalUser.get();
        user.setFailedAttemptCount(0);
        user.setLockedUntil(null);
        user.setLastFailedAttemptAt(null);
        user.setAccountNonLocked(true);
        user.setLockoutCount(0);

        userRepository.save(user);

        log.info("SECURITY: Account '{}' manually unlocked by admin", username);
        return true;
    }

    /**
     * Locks an account for exceeding failed attempts.
     *
     * @param user user to lock
     */
    private void lockAccount(User user) {
        int newLockoutCount = user.getLockoutCount() + 1;
        user.setLockoutCount(newLockoutCount);

        // Check if permanent lockout applies
        if (lockoutProperties.isPermanentLockoutEnabled()
                && newLockoutCount >= lockoutProperties.getLockoutsBeforePermanent()) {

            user.setAccountNonLocked(false);
            user.setLockedUntil(null); // Null = permanent

            log.warn(
                    "SECURITY: Account '{}' PERMANENTLY LOCKED after {} lockouts. "
                            + "Requires admin intervention.",
                    user.getUsername(),
                    newLockoutCount);
        } else {
            // Temporary lockout
            Instant lockUntil =
                    Instant.now()
                            .plus(
                                    Duration.ofMinutes(
                                            lockoutProperties.getLockoutDurationMinutes()));
            user.setLockedUntil(lockUntil);

            log.warn(
                    "SECURITY: Account '{}' temporarily locked until {} after {} failed attempts "
                            + "(lockout #{}/{})",
                    user.getUsername(),
                    lockUntil,
                    user.getFailedAttemptCount(),
                    newLockoutCount,
                    lockoutProperties.isPermanentLockoutEnabled()
                            ? lockoutProperties.getLockoutsBeforePermanent()
                            : "unlimited");
        }

        // Reset attempt counter for next cycle
        user.setFailedAttemptCount(0);
    }

    /**
     * Determines if the failed attempt counter should be reset due to inactivity.
     *
     * @param user user to check
     * @return true if counter should be reset
     */
    private boolean shouldResetFailedAttempts(User user) {
        if (user.getLastFailedAttemptAt() == null) {
            return false;
        }

        Duration sinceLastAttempt = Duration.between(user.getLastFailedAttemptAt(), Instant.now());
        return sinceLastAttempt.toMinutes() >= lockoutProperties.getResetAfterMinutes();
    }
}
