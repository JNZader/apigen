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
 * Listener de eventos de autenticación de Spring Security.
 *
 * <p>Captura eventos de login exitoso y fallido para:
 *
 * <ul>
 *   <li>Integrar con el servicio de bloqueo de cuentas
 *   <li>Registrar eventos de auditoría de seguridad
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
     * Maneja eventos de autenticación fallida por credenciales incorrectas.
     *
     * <p>Este evento se dispara cuando el usuario proporciona credenciales inválidas (username o
     * password incorrectos).
     *
     * @param event evento de fallo de autenticación
     */
    @EventListener
    public void onAuthenticationFailure(AuthenticationFailureBadCredentialsEvent event) {
        String username = extractUsername(event);

        log.debug(
                "SECURITY EVENT: Authentication failed for user '{}' - {}",
                username,
                event.getException().getMessage());

        // Registrar intento fallido para bloqueo de cuenta
        accountLockoutService.recordFailedAttempt(username);
    }

    /**
     * Maneja eventos de autenticación exitosa.
     *
     * <p>Se usa para resetear el contador de intentos fallidos después de un login exitoso.
     *
     * @param event evento de autenticación exitosa
     */
    @EventListener
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        String username = event.getAuthentication().getName();

        log.debug("SECURITY EVENT: Authentication successful for user '{}'", username);

        // Resetear contador de intentos fallidos
        accountLockoutService.recordSuccessfulLogin(username);
    }

    /**
     * Extrae el username del evento de fallo de autenticación.
     *
     * @param event evento de fallo
     * @return username o "unknown" si no se puede determinar
     */
    private String extractUsername(AuthenticationFailureBadCredentialsEvent event) {
        Object principal = event.getAuthentication().getPrincipal();

        if (principal instanceof String) {
            return (String) principal;
        }

        // Fallback: intentar obtener del nombre de autenticación
        String name = event.getAuthentication().getName();
        return name != null ? name : "unknown";
    }
}
