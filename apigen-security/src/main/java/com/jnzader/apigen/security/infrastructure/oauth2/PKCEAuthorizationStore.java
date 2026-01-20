package com.jnzader.apigen.security.infrastructure.oauth2;

import com.jnzader.apigen.security.infrastructure.oauth2.PKCEService.CodeChallengeMethod;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * In-memory store for PKCE authorization codes and their associated challenges.
 *
 * <p>This store manages the lifecycle of authorization codes during the PKCE flow:
 *
 * <ol>
 *   <li>When authorization is requested, a code is generated and stored with its challenge
 *   <li>When token is requested, the code is retrieved and validated against the verifier
 *   <li>Codes are automatically cleaned up after expiration
 * </ol>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // During authorization
 * String authCode = store.createAuthorizationCode(userId, codeChallenge, S256, clientId, redirectUri);
 *
 * // During token exchange
 * Optional<AuthorizationData> data = store.consumeAuthorizationCode(authCode);
 * if (data.isPresent() && pkceService.verifyCodeChallenge(codeVerifier, data.get().codeChallenge())) {
 *     // Issue tokens
 * }
 * }</pre>
 *
 * <p>For production use with multiple instances, consider implementing a Redis-based store.
 */
@Component
public class PKCEAuthorizationStore {

    private static final Logger log = LoggerFactory.getLogger(PKCEAuthorizationStore.class);
    private static final int AUTHORIZATION_CODE_LENGTH = 32;
    private static final Duration DEFAULT_CODE_EXPIRATION = Duration.ofMinutes(10);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final Map<String, AuthorizationData> authorizationCodes = new ConcurrentHashMap<>();
    private Duration codeExpiration = DEFAULT_CODE_EXPIRATION;

    /**
     * Data associated with an authorization code.
     *
     * @param userId the authenticated user's identifier
     * @param codeChallenge the PKCE code challenge
     * @param challengeMethod the method used to create the challenge
     * @param clientId the OAuth2 client identifier
     * @param redirectUri the redirect URI for the callback
     * @param scopes the requested scopes
     * @param createdAt when the authorization was created
     * @param expiresAt when the authorization expires
     */
    public record AuthorizationData(
            String userId,
            String codeChallenge,
            CodeChallengeMethod challengeMethod,
            String clientId,
            String redirectUri,
            String scopes,
            Instant createdAt,
            Instant expiresAt) {

        /**
         * Checks if this authorization has expired.
         *
         * @return true if expired
         */
        public boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }

    /**
     * Creates a new authorization code and stores it with the associated PKCE data.
     *
     * @param userId the authenticated user's identifier
     * @param codeChallenge the PKCE code challenge
     * @param challengeMethod the method used to create the challenge
     * @param clientId the OAuth2 client identifier
     * @param redirectUri the redirect URI for the callback
     * @param scopes the requested scopes (space-separated)
     * @return the generated authorization code
     */
    public String createAuthorizationCode(
            String userId,
            String codeChallenge,
            CodeChallengeMethod challengeMethod,
            String clientId,
            String redirectUri,
            String scopes) {

        String authorizationCode = generateAuthorizationCode();
        Instant now = Instant.now();

        AuthorizationData data =
                new AuthorizationData(
                        userId,
                        codeChallenge,
                        challengeMethod,
                        clientId,
                        redirectUri,
                        scopes,
                        now,
                        now.plus(codeExpiration));

        authorizationCodes.put(authorizationCode, data);

        log.debug(
                "Created authorization code for user {} with client {} (expires at {})",
                userId,
                clientId,
                data.expiresAt());

        return authorizationCode;
    }

    /**
     * Retrieves and removes an authorization code (single-use).
     *
     * <p>This method is atomic - the code is removed immediately upon retrieval to prevent replay
     * attacks.
     *
     * @param authorizationCode the authorization code to consume
     * @return the authorization data if found and not expired, empty otherwise
     */
    public Optional<AuthorizationData> consumeAuthorizationCode(String authorizationCode) {
        if (authorizationCode == null) {
            return Optional.empty();
        }

        AuthorizationData data = authorizationCodes.remove(authorizationCode);

        if (data == null) {
            log.debug("Authorization code not found");
            return Optional.empty();
        }

        if (data.isExpired()) {
            log.debug("Authorization code expired for user {}", data.userId());
            return Optional.empty();
        }

        log.debug(
                "Consumed authorization code for user {} with client {}",
                data.userId(),
                data.clientId());
        return Optional.of(data);
    }

    /**
     * Checks if an authorization code exists and is valid (not expired).
     *
     * @param authorizationCode the authorization code to check
     * @return true if the code exists and is not expired
     */
    public boolean isValidCode(String authorizationCode) {
        if (authorizationCode == null) {
            return false;
        }

        AuthorizationData data = authorizationCodes.get(authorizationCode);
        return data != null && !data.isExpired();
    }

    /**
     * Revokes all authorization codes for a specific user.
     *
     * <p>Useful when a user logs out or changes credentials.
     *
     * @param userId the user whose codes should be revoked
     * @return the number of codes revoked
     */
    public int revokeCodesForUser(String userId) {
        int count = 0;
        var iterator = authorizationCodes.entrySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getValue().userId().equals(userId)) {
                iterator.remove();
                count++;
            }
        }

        if (count > 0) {
            log.info("Revoked {} authorization codes for user {}", count, userId);
        }
        return count;
    }

    /**
     * Revokes all authorization codes for a specific client.
     *
     * @param clientId the client whose codes should be revoked
     * @return the number of codes revoked
     */
    public int revokeCodesForClient(String clientId) {
        int count = 0;
        var iterator = authorizationCodes.entrySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getValue().clientId().equals(clientId)) {
                iterator.remove();
                count++;
            }
        }

        if (count > 0) {
            log.info("Revoked {} authorization codes for client {}", count, clientId);
        }
        return count;
    }

    /**
     * Sets the expiration duration for authorization codes.
     *
     * @param duration the expiration duration (default: 10 minutes)
     */
    public void setCodeExpiration(Duration duration) {
        this.codeExpiration = duration;
    }

    /**
     * Returns the current number of stored authorization codes.
     *
     * @return the number of stored codes
     */
    public int getStoredCodeCount() {
        return authorizationCodes.size();
    }

    /** Scheduled cleanup of expired authorization codes. Runs every 5 minutes. */
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void cleanupExpiredCodes() {
        int beforeSize = authorizationCodes.size();
        authorizationCodes.entrySet().removeIf(entry -> entry.getValue().isExpired());
        int removed = beforeSize - authorizationCodes.size();

        if (removed > 0) {
            log.debug("Cleaned up {} expired authorization codes", removed);
        }
    }

    private String generateAuthorizationCode() {
        byte[] randomBytes = new byte[AUTHORIZATION_CODE_LENGTH];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}
