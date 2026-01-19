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
 * Servicio para auditoría de eventos de seguridad.
 *
 * <p>Proporciona métodos para registrar eventos de seguridad como autenticación, autorización,
 * acceso denegado, etc.
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

    /** Registra un evento de autenticación exitosa. */
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

    /** Registra un evento de autenticación fallida. */
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

    /** Registra un evento de acceso denegado. */
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

    /** Registra un evento de límite de tasa excedido. */
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

    /** Registra un evento de acceso a recurso. */
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

    /** Registra una acción administrativa. */
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

    /** Registra actividad sospechosa. */
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

    /** Registra el evento en el log de auditoría y publica el evento. */
    private void logEvent(SecurityAuditEvent event) {
        // Log estructurado para auditoría
        auditLog.info(
                "SECURITY_EVENT type={} user={} ip={} resource={} action={} outcome={} details={}",
                event.eventType(),
                event.username() != null ? event.username() : ANONYMOUS_USER,
                event.ipAddress(),
                event.resource(),
                event.action(),
                event.outcome(),
                event.details());

        // Publicar evento para listeners (opcional: persistir en BD, enviar alertas, etc.)
        try {
            eventPublisher.publishEvent(event);
        } catch (Exception e) {
            log.warn("No se pudo publicar el evento de auditoría: {}", e.getMessage());
        }
    }

    /** Obtiene la IP del cliente de la request actual. */
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

    /** Obtiene el User-Agent del cliente. */
    private String getUserAgent() {
        return getRequest().map(request -> request.getHeader("User-Agent")).orElse(UNKNOWN);
    }

    /** Obtiene el username del contexto de seguridad actual. */
    private Optional<String> getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null
                && auth.isAuthenticated()
                && !"anonymousUser".equals(auth.getPrincipal())) {
            return Optional.of(auth.getName());
        }
        return Optional.empty();
    }

    /** Obtiene la HttpServletRequest actual del contexto. */
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
