package com.jnzader.apigen.security.infrastructure.audit;

import com.jnzader.apigen.security.application.service.AccountLockoutService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

/**
 * Listener for Spring Security authentication events.
 *
 * <p>Captures successful and failed login events to:
 *
 * <ul>
 *   <li>Integrate with the account lockout service
 *   <li>Log security audit events
 * </ul>
 */
@Component
@ConditionalOnProperty(name = "apigen.security.enabled", havingValue = "true")
public class AuthenticationEventListener {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationEventListener.class);

    private final AccountLockoutService accountLockoutService;

    public AuthenticationEventListener(AccountLockoutService accountLockoutService) {
        this.accountLockoutService = accountLockoutService;
    }

    /**
     * Handles authentication failure events due to bad credentials.
     *
     * <p>This event is fired when the user provides invalid credentials (wrong username or
     * password).
     *
     * @param event the authentication failure event
     */
    @EventListener
    public void onAuthenticationFailure(AuthenticationFailureBadCredentialsEvent event) {
        String username = extractUsername(event);

        log.debug(
                "SECURITY EVENT: Authentication failed for user '{}' - {}",
                username,
                event.getException().getMessage());

        // Record failed attempt for account lockout
        accountLockoutService.recordFailedAttempt(username);
    }

    /**
     * Handles successful authentication events.
     *
     * <p>Used to reset the failed attempts counter after a successful login.
     *
     * @param event the successful authentication event
     */
    @EventListener
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        String username = event.getAuthentication().getName();

        log.debug("SECURITY EVENT: Authentication successful for user '{}'", username);

        // Reset failed attempts counter
        accountLockoutService.recordSuccessfulLogin(username);
    }

    /**
     * Extracts the username from the authentication failure event.
     *
     * @param event the failure event
     * @return username or "unknown" if it cannot be determined
     */
    private String extractUsername(AuthenticationFailureBadCredentialsEvent event) {
        Object principal = event.getAuthentication().getPrincipal();

        if (principal instanceof String username) {
            return username;
        }

        // Fallback: try to get from authentication name
        String name = event.getAuthentication().getName();
        return name != null ? name : "unknown";
    }
}
