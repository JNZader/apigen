package com.jnzader.apigen.security.infrastructure.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authorization.event.AuthorizationDeniedEvent;
import org.springframework.stereotype.Component;

/**
 * Listener para eventos de seguridad de Spring Security.
 * <p>
 * Captura eventos de autenticación y autorización para auditoría.
 */
@Component
public class SecurityAuditListener {

    private static final Logger log = LoggerFactory.getLogger(SecurityAuditListener.class);

    private final SecurityAuditService auditService;

    public SecurityAuditListener(SecurityAuditService auditService) {
        this.auditService = auditService;
    }

    /**
     * Listener para eventos de autenticación exitosa.
     */
    @EventListener
    @Async("domainEventExecutor")
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        String username = event.getAuthentication().getName();
        log.debug("Authentication success event for user: {}", username);
        auditService.logAuthenticationSuccess(username);
    }

    /**
     * Listener para eventos de autenticación fallida por credenciales incorrectas.
     */
    @EventListener
    @Async("domainEventExecutor")
    public void onAuthenticationFailure(AuthenticationFailureBadCredentialsEvent event) {
        String username = event.getAuthentication().getName();
        String reason = event.getException().getMessage();
        log.debug("Authentication failure event for user: {} - reason: {}", username, reason);
        auditService.logAuthenticationFailure(username, reason);
    }

    /**
     * Listener para eventos de autorización denegada.
     */
    @EventListener
    @Async("domainEventExecutor")
    public void onAuthorizationDenied(AuthorizationDeniedEvent<?> event) {
        String username = event.getAuthentication().get().getName();
        log.debug("Authorization denied event for user: {}", username);
        auditService.logAccessDenied(
                event.getSource() != null ? event.getSource().toString() : "unknown",
                "ACCESS_DENIED"
        );
    }

    /**
     * Listener para eventos de auditoría de seguridad personalizados.
     * Permite procesar eventos adicionales (persistir en BD, enviar alertas, etc.)
     */
    @EventListener
    @Async("domainEventExecutor")
    public void onSecurityAuditEvent(SecurityAuditEvent event) {
        log.debug("Custom security audit event: {} - {} - {}",
                event.eventType(), event.username(), event.outcome());

        // Aquí se pueden agregar acciones adicionales:
        // - Persistir en base de datos
        // - Enviar alertas por email/Slack
        // - Integrar con SIEM (Security Information and Event Management)
    }
}
