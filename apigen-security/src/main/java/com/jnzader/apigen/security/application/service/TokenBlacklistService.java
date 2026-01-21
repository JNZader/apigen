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
 * Service for managing the JWT token blacklist.
 *
 * <p>Allows invalidating tokens before their natural expiration.
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
     * Adds a token to the blacklist.
     *
     * @param tokenId Unique token identifier (jti claim)
     * @param username Token owner username
     * @param expiration Token expiration date
     * @param reason Reason for invalidation
     */
    @Transactional
    public void blacklistToken(
            String tokenId, String username, Instant expiration, BlacklistReason reason) {
        // Call repository directly to avoid transactional self-invocation
        if (repository.existsByTokenId(tokenId)) {
            log.debug("Token {} is already blacklisted", tokenId);
            return;
        }

        TokenBlacklist entry = new TokenBlacklist(tokenId, username, expiration, reason);
        repository.save(entry);
        log.info("Token {} from user {} added to blacklist. Reason: {}", tokenId, username, reason);
    }

    /**
     * Checks if a token is blacklisted.
     *
     * @param tokenId Token identifier
     * @return true if the token is invalidated
     */
    @Transactional(readOnly = true)
    public boolean isBlacklisted(String tokenId) {
        return repository.existsByTokenId(tokenId);
    }

    /**
     * Invalidates all tokens for a user. Useful when the user changes their password.
     *
     * @param username Username
     */
    @Transactional
    public void revokeAllUserTokens(String username) {
        int deleted = repository.deleteByUsername(username);
        log.info("Revoked {} tokens for user {}", deleted, username);
    }

    /** Cleans up expired tokens from the blacklist. Runs automatically every hour. */
    @Scheduled(fixedRate = 3600000) // 1 hour
    @Transactional
    public void cleanupExpiredTokens() {
        int deleted = repository.deleteExpiredTokens(Instant.now());
        if (deleted > 0) {
            log.info("Cleaned up {} expired tokens from blacklist", deleted);
        }
    }
}
