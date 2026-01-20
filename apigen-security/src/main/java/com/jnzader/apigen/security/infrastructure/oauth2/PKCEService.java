package com.jnzader.apigen.security.infrastructure.oauth2;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.stereotype.Service;

/**
 * Service for PKCE (Proof Key for Code Exchange) operations.
 *
 * <p>PKCE is an extension to the OAuth 2.0 Authorization Code flow that provides additional
 * security for public clients (SPAs, mobile apps) that cannot securely store a client secret.
 *
 * <p>Flow:
 *
 * <ol>
 *   <li>Client generates a random code_verifier
 *   <li>Client computes code_challenge = BASE64URL(SHA256(code_verifier))
 *   <li>Client sends code_challenge in authorization request
 *   <li>Server stores code_challenge with the authorization code
 *   <li>Client sends code_verifier in token request
 *   <li>Server verifies SHA256(code_verifier) matches stored code_challenge
 * </ol>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // On client side (or for testing)
 * String codeVerifier = pkceService.generateCodeVerifier();
 * String codeChallenge = pkceService.generateCodeChallenge(codeVerifier);
 *
 * // Authorization request includes: code_challenge, code_challenge_method=S256
 * // Token request includes: code_verifier
 *
 * // On server side
 * boolean valid = pkceService.verifyCodeChallenge(codeVerifier, storedCodeChallenge);
 * }</pre>
 *
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc7636">RFC 7636 - PKCE</a>
 */
@Service
public class PKCEService {

    private static final int CODE_VERIFIER_MIN_LENGTH = 43;
    private static final int CODE_VERIFIER_MAX_LENGTH = 128;
    private static final int DEFAULT_CODE_VERIFIER_LENGTH = 64;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Supported code challenge methods.
     *
     * <p>S256 (SHA-256) is the recommended method. PLAIN should only be used if the client cannot
     * support S256.
     */
    public enum CodeChallengeMethod {
        /** SHA-256 hash of the code verifier (recommended). */
        S256,
        /** Plain text code verifier (not recommended, use only if S256 is not supported). */
        PLAIN
    }

    /**
     * Generates a cryptographically random code verifier.
     *
     * <p>The code verifier is a high-entropy cryptographic random string using the unreserved
     * characters [A-Z] / [a-z] / [0-9] / "-" / "." / "_" / "~", with a minimum length of 43
     * characters and a maximum length of 128 characters.
     *
     * @return a random code verifier string
     */
    public String generateCodeVerifier() {
        return generateCodeVerifier(DEFAULT_CODE_VERIFIER_LENGTH);
    }

    /**
     * Generates a cryptographically random code verifier with specified length.
     *
     * @param length the desired length (43-128 characters)
     * @return a random code verifier string
     * @throws IllegalArgumentException if length is not between 43 and 128
     */
    public String generateCodeVerifier(int length) {
        if (length < CODE_VERIFIER_MIN_LENGTH || length > CODE_VERIFIER_MAX_LENGTH) {
            throw new IllegalArgumentException(
                    String.format(
                            "Code verifier length must be between %d and %d characters",
                            CODE_VERIFIER_MIN_LENGTH, CODE_VERIFIER_MAX_LENGTH));
        }

        byte[] randomBytes = new byte[length];
        SECURE_RANDOM.nextBytes(randomBytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes)
                .substring(0, length);
    }

    /**
     * Generates a code challenge from the code verifier using SHA-256.
     *
     * <p>The code challenge is computed as: BASE64URL(SHA256(code_verifier))
     *
     * @param codeVerifier the code verifier to hash
     * @return the code challenge (BASE64URL encoded SHA-256 hash)
     * @throws IllegalArgumentException if codeVerifier is null or empty
     */
    public String generateCodeChallenge(String codeVerifier) {
        return generateCodeChallenge(codeVerifier, CodeChallengeMethod.S256);
    }

    /**
     * Generates a code challenge from the code verifier using the specified method.
     *
     * @param codeVerifier the code verifier
     * @param method the challenge method (S256 or PLAIN)
     * @return the code challenge
     * @throws IllegalArgumentException if codeVerifier is null or empty
     */
    public String generateCodeChallenge(String codeVerifier, CodeChallengeMethod method) {
        if (codeVerifier == null || codeVerifier.isEmpty()) {
            throw new IllegalArgumentException("Code verifier cannot be null or empty");
        }

        return switch (method) {
            case S256 -> computeS256Challenge(codeVerifier);
            case PLAIN -> codeVerifier;
        };
    }

    /**
     * Verifies that the code verifier matches the code challenge.
     *
     * @param codeVerifier the code verifier from the token request
     * @param codeChallenge the code challenge from the authorization request
     * @param method the challenge method used
     * @return true if the code verifier is valid, false otherwise
     */
    public boolean verifyCodeChallenge(
            String codeVerifier, String codeChallenge, CodeChallengeMethod method) {
        if (codeVerifier == null || codeChallenge == null) {
            return false;
        }

        String computedChallenge = generateCodeChallenge(codeVerifier, method);
        return MessageDigest.isEqual(
                codeChallenge.getBytes(StandardCharsets.UTF_8),
                computedChallenge.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Verifies that the code verifier matches the code challenge using S256 method.
     *
     * @param codeVerifier the code verifier from the token request
     * @param codeChallenge the code challenge from the authorization request
     * @return true if the code verifier is valid, false otherwise
     */
    public boolean verifyCodeChallenge(String codeVerifier, String codeChallenge) {
        return verifyCodeChallenge(codeVerifier, codeChallenge, CodeChallengeMethod.S256);
    }

    /**
     * Validates that a code verifier meets the RFC 7636 requirements.
     *
     * @param codeVerifier the code verifier to validate
     * @return true if valid, false otherwise
     */
    public boolean isValidCodeVerifier(String codeVerifier) {
        if (codeVerifier == null) {
            return false;
        }

        int length = codeVerifier.length();
        if (length < CODE_VERIFIER_MIN_LENGTH || length > CODE_VERIFIER_MAX_LENGTH) {
            return false;
        }

        // Check that all characters are unreserved: [A-Z] / [a-z] / [0-9] / "-" / "." / "_" / "~"
        return codeVerifier.matches("^[A-Za-z0-9\\-._~]+$");
    }

    private String computeS256Challenge(String codeVerifier) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is required by the JVM specification, this should never happen
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
