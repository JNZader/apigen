package com.jnzader.apigen.security.infrastructure.audit;

import java.time.Instant;
import java.util.Map;

/**
 * Representa un evento de auditoría de seguridad.
 *
 * @param timestamp   Momento en que ocurrió el evento.
 * @param eventType   Tipo de evento de seguridad.
 * @param username    Usuario involucrado (puede ser null para eventos anónimos).
 * @param ipAddress   Dirección IP del cliente.
 * @param userAgent   User-Agent del cliente.
 * @param resource    Recurso accedido.
 * @param action      Acción realizada.
 * @param outcome     Resultado (SUCCESS, FAILURE, DENIED).
 * @param details     Detalles adicionales del evento.
 */
public record SecurityAuditEvent(
        Instant timestamp,
        SecurityEventType eventType,
        String username,
        String ipAddress,
        String userAgent,
        String resource,
        String action,
        SecurityOutcome outcome,
        Map<String, Object> details
) {
    public SecurityAuditEvent {
        if (timestamp == null) timestamp = Instant.now();
        if (details == null) details = Map.of();
    }

    /**
     * Tipos de eventos de seguridad.
     */
    public enum SecurityEventType {
        AUTHENTICATION_SUCCESS,
        AUTHENTICATION_FAILURE,
        AUTHORIZATION_SUCCESS,
        AUTHORIZATION_FAILURE,
        ACCESS_DENIED,
        SESSION_CREATED,
        SESSION_DESTROYED,
        PASSWORD_CHANGE,
        ACCOUNT_LOCKED,
        SUSPICIOUS_ACTIVITY,
        RATE_LIMIT_EXCEEDED,
        RESOURCE_ACCESS,
        DATA_EXPORT,
        ADMIN_ACTION
    }

    /**
     * Resultado del evento de seguridad.
     */
    public enum SecurityOutcome {
        SUCCESS,
        FAILURE,
        DENIED,
        BLOCKED
    }

    /**
     * Builder para crear eventos de auditoría.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Instant timestamp = Instant.now();
        private SecurityEventType eventType;
        private String username;
        private String ipAddress;
        private String userAgent;
        private String resource;
        private String action;
        private SecurityOutcome outcome;
        private Map<String, Object> details = Map.of();

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder eventType(SecurityEventType eventType) {
            this.eventType = eventType;
            return this;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder ipAddress(String ipAddress) {
            this.ipAddress = ipAddress;
            return this;
        }

        public Builder userAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }

        public Builder resource(String resource) {
            this.resource = resource;
            return this;
        }

        public Builder action(String action) {
            this.action = action;
            return this;
        }

        public Builder outcome(SecurityOutcome outcome) {
            this.outcome = outcome;
            return this;
        }

        public Builder details(Map<String, Object> details) {
            this.details = details;
            return this;
        }

        public SecurityAuditEvent build() {
            return new SecurityAuditEvent(
                    timestamp, eventType, username, ipAddress, userAgent,
                    resource, action, outcome, details
            );
        }
    }
}
