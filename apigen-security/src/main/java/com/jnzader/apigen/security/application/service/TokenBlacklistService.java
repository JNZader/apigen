package com.jnzader.apigen.security.application.service;

import com.jnzader.apigen.security.domain.entity.TokenBlacklist;
import com.jnzader.apigen.security.domain.entity.TokenBlacklist.BlacklistReason;
import com.jnzader.apigen.security.domain.repository.TokenBlacklistRepository;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio para gestionar la blacklist de tokens JWT.
 *
 * <p>Permite invalidar tokens antes de su expiración natural.
 */
@Service
@ConditionalOnProperty(name = "apigen.security.enabled", havingValue = "true")
public class TokenBlacklistService {

    private static final Logger log = LoggerFactory.getLogger(TokenBlacklistService.class);

    private final TokenBlacklistRepository repository;

    public TokenBlacklistService(TokenBlacklistRepository repository) {
        this.repository = repository;
    }

    /**
     * Añade un token a la blacklist.
     *
     * @param tokenId Identificador único del token (jti claim)
     * @param username Usuario propietario del token
     * @param expiration Fecha de expiración del token
     * @param reason Razón de la invalidación
     */
    @Transactional
    public void blacklistToken(
            String tokenId, String username, Instant expiration, BlacklistReason reason) {
        // Llamar directamente al repository para evitar self-invocation transaccional
        if (repository.existsByTokenId(tokenId)) {
            log.debug("Token {} ya está en la blacklist", tokenId);
            return;
        }

        TokenBlacklist entry = new TokenBlacklist(tokenId, username, expiration, reason);
        repository.save(entry);
        log.info(
                "Token {} del usuario {} añadido a blacklist. Razón: {}",
                tokenId,
                username,
                reason);
    }

    /**
     * Verifica si un token está en la blacklist.
     *
     * @param tokenId Identificador del token
     * @return true si el token está invalidado
     */
    @Transactional(readOnly = true)
    public boolean isBlacklisted(String tokenId) {
        return repository.existsByTokenId(tokenId);
    }

    /**
     * Invalida todos los tokens de un usuario. Útil cuando el usuario cambia su contraseña.
     *
     * @param username Nombre de usuario
     */
    @Transactional
    public void revokeAllUserTokens(String username) {
        int deleted = repository.deleteByUsername(username);
        log.info("Revocados {} tokens del usuario {}", deleted, username);
    }

    /** Limpia tokens expirados de la blacklist. Se ejecuta automáticamente cada hora. */
    @Scheduled(fixedRate = 3600000) // 1 hora
    @Transactional
    public void cleanupExpiredTokens() {
        int deleted = repository.deleteExpiredTokens(Instant.now());
        if (deleted > 0) {
            log.info("Limpiados {} tokens expirados de la blacklist", deleted);
        }
    }
}
