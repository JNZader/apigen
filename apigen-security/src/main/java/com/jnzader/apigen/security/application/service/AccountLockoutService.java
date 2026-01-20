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
 * Servicio para gestionar el bloqueo de cuentas por intentos fallidos de autenticación.
 *
 * <p>Protege contra ataques de fuerza bruta bloqueando temporalmente las cuentas después de
 * múltiples intentos de login fallidos.
 *
 * <p>Características:
 *
 * <ul>
 *   <li>Bloqueo temporal configurable (default: 15 minutos después de 5 intentos)
 *   <li>Reset automático del contador después de período de inactividad
 *   <li>Soporte para bloqueo permanente después de múltiples bloqueos
 *   <li>Logging de auditoría de eventos de seguridad
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
     * Registra un intento de login fallido para un usuario.
     *
     * <p>Incrementa el contador de intentos fallidos y bloquea la cuenta si se excede el máximo.
     *
     * @param username nombre de usuario que falló la autenticación
     */
    @Transactional
    public void recordFailedAttempt(String username) {
        if (!lockoutProperties.isEnabled()) {
            return;
        }

        Optional<User> optionalUser = userRepository.findByUsername(username);
        if (optionalUser.isEmpty()) {
            // Usuario no existe - no revelar esta información
            log.debug(
                    "SECURITY: Failed login attempt for non-existent user (not revealing in"
                            + " response)");
            return;
        }

        User user = optionalUser.get();

        // Verificar si el contador debe resetearse por inactividad
        if (shouldResetFailedAttempts(user)) {
            user.setFailedAttemptCount(0);
            log.debug("SECURITY: Reset failed attempts for user '{}' due to inactivity", username);
        }

        // Incrementar contador
        int newCount = user.getFailedAttemptCount() + 1;
        user.setFailedAttemptCount(newCount);
        user.setLastFailedAttemptAt(Instant.now());

        log.info(
                "SECURITY: Failed login attempt #{} for user '{}' (max: {})",
                newCount,
                username,
                lockoutProperties.getMaxFailedAttempts());

        // Verificar si se debe bloquear la cuenta
        if (newCount >= lockoutProperties.getMaxFailedAttempts()) {
            lockAccount(user);
        }

        userRepository.save(user);
    }

    /**
     * Registra un login exitoso y resetea los contadores de intentos fallidos.
     *
     * @param username nombre de usuario que se autenticó exitosamente
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

        // Solo resetear si había intentos fallidos o bloqueo temporal
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
     * Verifica si una cuenta está actualmente bloqueada.
     *
     * @param username nombre de usuario a verificar
     * @return true si la cuenta está bloqueada, false en caso contrario
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
     * Obtiene el tiempo restante de bloqueo para una cuenta.
     *
     * @param username nombre de usuario
     * @return duración restante del bloqueo, o Duration.ZERO si no está bloqueada
     */
    @Transactional(readOnly = true)
    public Duration getRemainingLockoutDuration(String username) {
        Optional<User> optionalUser = userRepository.findByUsername(username);
        if (optionalUser.isEmpty()) {
            return Duration.ZERO;
        }

        User user = optionalUser.get();

        // Bloqueo permanente
        if (!user.isAccountNonLocked() && user.getLockedUntil() == null) {
            return Duration.ofDays(365 * 100); // Efectivamente "infinito"
        }

        // Bloqueo temporal
        if (user.getLockedUntil() != null) {
            Duration remaining = Duration.between(Instant.now(), user.getLockedUntil());
            return remaining.isNegative() ? Duration.ZERO : remaining;
        }

        return Duration.ZERO;
    }

    /**
     * Desbloquea manualmente una cuenta (requiere permisos de admin).
     *
     * @param username nombre de usuario a desbloquear
     * @return true si se desbloqueó, false si el usuario no existe
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
     * Bloquea una cuenta por haber excedido los intentos fallidos.
     *
     * @param user usuario a bloquear
     */
    private void lockAccount(User user) {
        int newLockoutCount = user.getLockoutCount() + 1;
        user.setLockoutCount(newLockoutCount);

        // Verificar si corresponde bloqueo permanente
        if (lockoutProperties.isPermanentLockoutEnabled()
                && newLockoutCount >= lockoutProperties.getLockoutsBeforePermanent()) {

            user.setAccountNonLocked(false);
            user.setLockedUntil(null); // Null = permanente

            log.warn(
                    "SECURITY: Account '{}' PERMANENTLY LOCKED after {} lockouts. "
                            + "Requires admin intervention.",
                    user.getUsername(),
                    newLockoutCount);
        } else {
            // Bloqueo temporal
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

        // Resetear contador de intentos para el próximo ciclo
        user.setFailedAttemptCount(0);
    }

    /**
     * Determina si el contador de intentos fallidos debe resetearse por inactividad.
     *
     * @param user usuario a verificar
     * @return true si debe resetearse el contador
     */
    private boolean shouldResetFailedAttempts(User user) {
        if (user.getLastFailedAttemptAt() == null) {
            return false;
        }

        Duration sinceLastAttempt = Duration.between(user.getLastFailedAttemptAt(), Instant.now());
        return sinceLastAttempt.toMinutes() >= lockoutProperties.getResetAfterMinutes();
    }
}
