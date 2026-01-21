package com.jnzader.apigen.security.infrastructure.audit;

import com.jnzader.apigen.security.infrastructure.audit.SecurityAuditEvent.SecurityEventType;
import com.jnzader.apigen.security.infrastructure.audit.SecurityAuditEvent.SecurityOutcome;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Service for security event auditing.
 *
 * <p>Provides methods for logging security events such as authentication, authorization, access
 * denied, etc.
 */
@Service
public class SecurityAuditService {

    private static final Logger auditLog = LoggerFactory.getLogger("SECURITY_AUDIT");
    private static final Logger log = LoggerFactory.getLogger(SecurityAuditService.class);
    private static final String ANONYMOUS_USER = "anonymous";
    private static final String UNKNOWN = "unknown";

    private final ApplicationEventPublisher eventPublisher;

    public SecurityAuditService(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    /** Logs a successful authentication event. */
    public void logAuthenticationSuccess(String username) {
        SecurityAuditEvent event =
                SecurityAuditEvent.builder()
                        .eventType(SecurityEventType.AUTHENTICATION_SUCCESS)
                        .username(username)
                        .ipAddress(getClientIp())
                        .userAgent(getUserAgent())
                        .action("LOGIN")
                        .outcome(SecurityOutcome.SUCCESS)
                        .build();

        logEvent(event);
    }

    /** Logs a failed authentication event. */
    public void logAuthenticationFailure(String username, String reason) {
        SecurityAuditEvent event =
                SecurityAuditEvent.builder()
                        .eventType(SecurityEventType.AUTHENTICATION_FAILURE)
                        .username(username)
                        .ipAddress(getClientIp())
                        .userAgent(getUserAgent())
                        .action("LOGIN")
                        .outcome(SecurityOutcome.FAILURE)
                        .details(Map.of("reason", reason))
                        .build();

        logEvent(event);
    }

    /** Logs an access denied event. */
    public void logAccessDenied(String resource, String requiredAuthority) {
        String username = getCurrentUsername().orElse(ANONYMOUS_USER);

        SecurityAuditEvent event =
                SecurityAuditEvent.builder()
                        .eventType(SecurityEventType.ACCESS_DENIED)
                        .username(username)
                        .ipAddress(getClientIp())
                        .userAgent(getUserAgent())
                        .resource(resource)
                        .action("ACCESS")
                        .outcome(SecurityOutcome.DENIED)
                        .details(Map.of("requiredAuthority", requiredAuthority))
                        .build();

        logEvent(event);
    }

    /** Logs a rate limit exceeded event. */
    public void logRateLimitExceeded(String endpoint) {
        String username = getCurrentUsername().orElse(ANONYMOUS_USER);

        SecurityAuditEvent event =
                SecurityAuditEvent.builder()
                        .eventType(SecurityEventType.RATE_LIMIT_EXCEEDED)
                        .username(username)
                        .ipAddress(getClientIp())
                        .userAgent(getUserAgent())
                        .resource(endpoint)
                        .action("REQUEST")
                        .outcome(SecurityOutcome.BLOCKED)
                        .build();

        logEvent(event);
    }

    /** Logs a resource access event. */
    public void logResourceAccess(String resource, String method, SecurityOutcome outcome) {
        String username = getCurrentUsername().orElse(ANONYMOUS_USER);

        SecurityAuditEvent event =
                SecurityAuditEvent.builder()
                        .eventType(SecurityEventType.RESOURCE_ACCESS)
                        .username(username)
                        .ipAddress(getClientIp())
                        .userAgent(getUserAgent())
                        .resource(resource)
                        .action(method)
                        .outcome(outcome)
                        .build();

        logEvent(event);
    }

    /** Logs an administrative action. */
    public void logAdminAction(String action, String targetResource, Map<String, Object> details) {
        String username = getCurrentUsername().orElse(UNKNOWN);

        SecurityAuditEvent event =
                SecurityAuditEvent.builder()
                        .eventType(SecurityEventType.ADMIN_ACTION)
                        .username(username)
                        .ipAddress(getClientIp())
                        .userAgent(getUserAgent())
                        .resource(targetResource)
                        .action(action)
                        .outcome(SecurityOutcome.SUCCESS)
                        .details(details)
                        .build();

        logEvent(event);
    }

    /** Logs suspicious activity. */
    public void logSuspiciousActivity(String description, Map<String, Object> details) {
        String username = getCurrentUsername().orElse(ANONYMOUS_USER);

        SecurityAuditEvent event =
                SecurityAuditEvent.builder()
                        .eventType(SecurityEventType.SUSPICIOUS_ACTIVITY)
                        .username(username)
                        .ipAddress(getClientIp())
                        .userAgent(getUserAgent())
                        .action(description)
                        .outcome(SecurityOutcome.BLOCKED)
                        .details(details)
                        .build();

        logEvent(event);
    }

    /** Logs the event to the audit log and publishes the event. */
    private void logEvent(SecurityAuditEvent event) {
        // Structured logging for audit
        auditLog.info(
                "SECURITY_EVENT type={} user={} ip={} resource={} action={} outcome={} details={}",
                event.eventType(),
                event.username() != null ? event.username() : ANONYMOUS_USER,
                event.ipAddress(),
                event.resource(),
                event.action(),
                event.outcome(),
                event.details());

        // Publish event for listeners (optional: persist to DB, send alerts, etc.)
        try {
            eventPublisher.publishEvent(event);
        } catch (Exception e) {
            log.warn("Could not publish audit event: {}", e.getMessage());
        }
    }

    /** Gets the client IP from the current request. */
    private String getClientIp() {
        return getRequest()
                .map(
                        request -> {
                            String xForwardedFor = request.getHeader("X-Forwarded-For");
                            if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                                return xForwardedFor.split(",")[0].trim();
                            }
                            return request.getRemoteAddr();
                        })
                .orElse(UNKNOWN);
    }

    /** Gets the client User-Agent. */
    private String getUserAgent() {
        return getRequest().map(request -> request.getHeader("User-Agent")).orElse(UNKNOWN);
    }

    /** Gets the username from the current security context. */
    private Optional<String> getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null
                && auth.isAuthenticated()
                && !"anonymousUser".equals(auth.getPrincipal())) {
            return Optional.of(auth.getName());
        }
        return Optional.empty();
    }

    /** Gets the current HttpServletRequest from the context. */
    private Optional<HttpServletRequest> getRequest() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return Optional.ofNullable(attrs).map(ServletRequestAttributes::getRequest);
        } catch (Exception _) {
            return Optional.empty();
        }
    }
}
