package com.jnzader.apigen.security.infrastructure.jwt;

import com.jnzader.apigen.security.application.service.TokenBlacklistService;
import com.jnzader.apigen.security.domain.entity.User;
import com.jnzader.apigen.security.infrastructure.config.SecurityProperties;
import com.jnzader.apigen.security.infrastructure.config.SecurityProperties.KeyRotationProperties;
import com.jnzader.apigen.security.infrastructure.config.SecurityProperties.PreviousKey;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * Service for JWT token generation and validation.
 *
 * <p>Features:
 *
 * <ul>
 *   <li>Access tokens with user claims
 *   <li>Refresh tokens for renewal
 *   <li>Integration with blacklist for revocation
 *   <li>Unique Token ID (jti) for each token
 *   <li>Support for key rotation with 'kid' header
 * </ul>
 *
 * <p>Key rotation: When enabled, tokens include a 'kid' (Key ID) header that identifies which key
 * was used to sign them. This allows previous keys to remain valid during the transition.
 */
@Service
@ConditionalOnProperty(name = "apigen.security.enabled", havingValue = "true")
@ConditionalOnExpression("'${apigen.security.mode:jwt}'.equalsIgnoreCase('jwt')")
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);
    private static final String CLAIM_USER_ID = "userId";
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final SecurityProperties securityProperties;
    private final TokenBlacklistService blacklistService;
    private final JsonMapper jsonMapper;
    private final SecretKey currentKey;
    private final String currentKeyId;
    private final boolean rotationEnabled;

    // Map of keyId -> SecretKey for validating tokens with previous keys
    private final Map<String, SecretKey> keyRegistry = new ConcurrentHashMap<>();

    public JwtService(
            SecurityProperties securityProperties,
            TokenBlacklistService blacklistService,
            JsonMapper jsonMapper) {
        this.securityProperties = securityProperties;
        this.blacklistService = blacklistService;
        this.jsonMapper = jsonMapper;

        // Initialize current key
        this.currentKey =
                Keys.hmacShaKeyFor(
                        securityProperties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8));

        KeyRotationProperties rotation = securityProperties.getJwt().getKeyRotation();
        this.rotationEnabled = rotation.isEnabled();
        this.currentKeyId = rotation.getCurrentKeyId();
    }

    @PostConstruct
    public void init() {
        // Register current key
        keyRegistry.put(currentKeyId, currentKey);

        if (rotationEnabled) {
            // Register previous keys for validation
            KeyRotationProperties rotation = securityProperties.getJwt().getKeyRotation();
            for (PreviousKey prevKey : rotation.getPreviousSecrets()) {
                if (prevKey.getId() != null && prevKey.getSecret() != null) {
                    SecretKey key =
                            Keys.hmacShaKeyFor(
                                    prevKey.getSecret().getBytes(StandardCharsets.UTF_8));
                    keyRegistry.put(prevKey.getId(), key);
                    log.info("Registered previous key for rotation: {}", prevKey.getId());
                }
            }
            log.info(
                    "JWT Key Rotation enabled. Current key: {}, Total keys: {}",
                    currentKeyId,
                    keyRegistry.size());
        } else {
            log.debug("JWT Key Rotation disabled. Using single key.");
        }
    }

    /** Generates an access token for the user. */
    public String generateAccessToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_USER_ID, user.getId());
        claims.put("role", user.getRole().getName());
        claims.put("email", user.getEmail());
        claims.put("type", "access");

        return buildToken(
                claims, user.getUsername(), securityProperties.getJwt().getExpirationMinutes());
    }

    /** Generates a refresh token for the user. */
    public String generateRefreshToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_USER_ID, user.getId());
        claims.put("type", "refresh");

        return buildToken(
                claims,
                user.getUsername(),
                securityProperties.getJwt().getRefreshExpirationMinutes());
    }

    /** Builds a JWT token with the specified claims and duration. */
    private String buildToken(Map<String, Object> claims, String subject, int expirationMinutes) {
        Instant now = Instant.now();
        Instant expiration = now.plus(expirationMinutes, ChronoUnit.MINUTES);

        var builder =
                Jwts.builder()
                        .id(UUID.randomUUID().toString()) // Unique Token ID for blacklist
                        .claims(claims)
                        .subject(subject)
                        .issuer(securityProperties.getJwt().getIssuer())
                        .issuedAt(Date.from(now))
                        .expiration(Date.from(expiration));

        // Add 'kid' header when rotation is enabled
        if (rotationEnabled) {
            builder.header().keyId(currentKeyId).and();
        }

        return builder.signWith(currentKey, Jwts.SIG.HS256).compact();
    }

    /** Extracts the username from the token. */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /** Extracts the token ID (jti). */
    public String extractTokenId(String token) {
        return extractClaim(token, Claims::getId);
    }

    /** Extracts the user ID from the token. */
    public Long extractUserId(String token) {
        return extractClaim(token, claims -> claims.get(CLAIM_USER_ID, Long.class));
    }

    /** Extracts the user role from the token. */
    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    /** Extracts the token type (access/refresh). */
    public String extractTokenType(String token) {
        return extractClaim(token, claims -> claims.get("type", String.class));
    }

    /** Extracts the expiration date from the token. */
    public Instant extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration).toInstant();
    }

    /** Extracts a specific claim from the token. */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /** Extracts all claims from the token. */
    private Claims extractAllClaims(String token) {
        SecretKey key = resolveSigningKey(token);
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    /**
     * Resolves the correct signing key for the token.
     *
     * <p>If rotation is enabled, looks up the 'kid' in the header and uses the corresponding key
     * from the registry. If there is no 'kid' or rotation is disabled, uses the current key.
     */
    private SecretKey resolveSigningKey(String token) {
        if (!rotationEnabled) {
            return currentKey;
        }

        try {
            // Parse only the header without verifying signature to get kid
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                return currentKey;
            }

            String headerJson =
                    new String(
                            java.util.Base64.getUrlDecoder().decode(parts[0]),
                            StandardCharsets.UTF_8);

            // Extract kid from header JSON simply
            String kid = extractKidFromHeader(headerJson);
            if (kid != null && keyRegistry.containsKey(kid)) {
                log.debug("Using key '{}' for token verification", kid);
                return keyRegistry.get(kid);
            }
        } catch (Exception e) {
            log.debug("Could not extract kid from token header: {}", e.getMessage());
        }

        return currentKey;
    }

    /** Extracts the kid from header JSON using JsonMapper. */
    private String extractKidFromHeader(String headerJson) {
        try {
            Map<String, Object> header = jsonMapper.readValue(headerJson, MAP_TYPE);
            Object kid = header.get("kid");
            return kid != null ? kid.toString() : null;
        } catch (Exception e) {
            log.debug("Failed to parse JWT header: {}", e.getMessage());
            return null;
        }
    }

    /** Validates if the token is valid for the given user. Includes blacklist verification. */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            final String tokenId = extractTokenId(token);

            // Verify blacklist
            if (blacklistService.isBlacklisted(tokenId)) {
                log.debug("Token {} is blacklisted", tokenId);
                return false;
            }

            return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Invalid token: {}", e.getMessage());
            return false;
        }
    }

    /** Checks if the token is expired. */
    public boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).isBefore(Instant.now());
        } catch (ExpiredJwtException _) {
            return true;
        }
    }

    /** Checks if the token is an access token. */
    public boolean isAccessToken(String token) {
        return "access".equals(extractTokenType(token));
    }

    /** Checks if the token is a refresh token. */
    public boolean isRefreshToken(String token) {
        return "refresh".equals(extractTokenType(token));
    }

    /**
     * Validates the token structure and signature without checking expiration. Useful for refresh
     * tokens.
     */
    public boolean isTokenStructureValid(String token) {
        try {
            SecretKey key = resolveSigningKey(token);
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Invalid token structure: {}", e.getMessage());
            return false;
        }
    }

    /** Extracts claims from a potentially expired token. Useful for refresh tokens. */
    public Claims extractClaimsIgnoringExpiration(String token) {
        try {
            SecretKey key = resolveSigningKey(token);
            return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }

    /**
     * Gets the current key ID used to sign new tokens.
     *
     * @return the current keyId
     */
    public String getCurrentKeyId() {
        return currentKeyId;
    }

    /**
     * Checks if key rotation is enabled.
     *
     * @return true if rotation is enabled
     */
    public boolean isKeyRotationEnabled() {
        return rotationEnabled;
    }

    /**
     * Gets the number of registered keys (including previous ones).
     *
     * @return number of keys in the registry
     */
    public int getRegisteredKeyCount() {
        return keyRegistry.size();
    }
}
