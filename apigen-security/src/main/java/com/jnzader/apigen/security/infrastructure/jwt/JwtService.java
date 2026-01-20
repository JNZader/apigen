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

/**
 * Servicio para generación y validación de tokens JWT.
 *
 * <p>Características:
 *
 * <ul>
 *   <li>Access tokens con claims de usuario
 *   <li>Refresh tokens para renovación
 *   <li>Integración con blacklist para revocación
 *   <li>Token ID (jti) único para cada token
 *   <li>Soporte para rotación de claves con header 'kid'
 * </ul>
 *
 * <p>Rotación de claves: Cuando está habilitada, los tokens incluyen un header 'kid' (Key ID) que
 * identifica qué clave se usó para firmarlos. Esto permite mantener claves anteriores válidas
 * durante la transición.
 */
@Service
@ConditionalOnProperty(name = "apigen.security.enabled", havingValue = "true")
@ConditionalOnExpression("'${apigen.security.mode:jwt}'.equalsIgnoreCase('jwt')")
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);
    private static final String CLAIM_USER_ID = "userId";

    private final SecurityProperties securityProperties;
    private final TokenBlacklistService blacklistService;
    private final SecretKey currentKey;
    private final String currentKeyId;
    private final boolean rotationEnabled;

    // Mapa de keyId -> SecretKey para validación de tokens con claves anteriores
    private final Map<String, SecretKey> keyRegistry = new ConcurrentHashMap<>();

    public JwtService(
            SecurityProperties securityProperties, TokenBlacklistService blacklistService) {
        this.securityProperties = securityProperties;
        this.blacklistService = blacklistService;

        // Inicializar clave actual
        this.currentKey =
                Keys.hmacShaKeyFor(
                        securityProperties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8));

        KeyRotationProperties rotation = securityProperties.getJwt().getKeyRotation();
        this.rotationEnabled = rotation.isEnabled();
        this.currentKeyId = rotation.getCurrentKeyId();
    }

    @PostConstruct
    public void init() {
        // Registrar clave actual
        keyRegistry.put(currentKeyId, currentKey);

        if (rotationEnabled) {
            // Registrar claves anteriores para validación
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

    /** Genera un token de acceso para el usuario. */
    public String generateAccessToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_USER_ID, user.getId());
        claims.put("role", user.getRole().getName());
        claims.put("email", user.getEmail());
        claims.put("type", "access");

        return buildToken(
                claims, user.getUsername(), securityProperties.getJwt().getExpirationMinutes());
    }

    /** Genera un refresh token para el usuario. */
    public String generateRefreshToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_USER_ID, user.getId());
        claims.put("type", "refresh");

        return buildToken(
                claims,
                user.getUsername(),
                securityProperties.getJwt().getRefreshExpirationMinutes());
    }

    /** Construye un token JWT con los claims y duración especificados. */
    private String buildToken(Map<String, Object> claims, String subject, int expirationMinutes) {
        Instant now = Instant.now();
        Instant expiration = now.plus(expirationMinutes, ChronoUnit.MINUTES);

        var builder =
                Jwts.builder()
                        .id(UUID.randomUUID().toString()) // Token ID único para blacklist
                        .claims(claims)
                        .subject(subject)
                        .issuer(securityProperties.getJwt().getIssuer())
                        .issuedAt(Date.from(now))
                        .expiration(Date.from(expiration));

        // Agregar header 'kid' cuando rotación está habilitada
        if (rotationEnabled) {
            builder.header().keyId(currentKeyId).and();
        }

        return builder.signWith(currentKey, Jwts.SIG.HS256).compact();
    }

    /** Extrae el nombre de usuario del token. */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /** Extrae el ID del token (jti). */
    public String extractTokenId(String token) {
        return extractClaim(token, Claims::getId);
    }

    /** Extrae el ID del usuario del token. */
    public Long extractUserId(String token) {
        return extractClaim(token, claims -> claims.get(CLAIM_USER_ID, Long.class));
    }

    /** Extrae el rol del usuario del token. */
    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    /** Extrae el tipo de token (access/refresh). */
    public String extractTokenType(String token) {
        return extractClaim(token, claims -> claims.get("type", String.class));
    }

    /** Extrae la fecha de expiración del token. */
    public Instant extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration).toInstant();
    }

    /** Extrae un claim específico del token. */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /** Extrae todos los claims del token. */
    private Claims extractAllClaims(String token) {
        SecretKey key = resolveSigningKey(token);
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    /**
     * Resuelve la clave de firma correcta para el token.
     *
     * <p>Si la rotación está habilitada, busca el 'kid' en el header y usa la clave correspondiente
     * del registry. Si no hay 'kid' o la rotación está deshabilitada, usa la clave actual.
     */
    private SecretKey resolveSigningKey(String token) {
        if (!rotationEnabled) {
            return currentKey;
        }

        try {
            // Parsear solo el header sin verificar firma para obtener kid
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                return currentKey;
            }

            String headerJson =
                    new String(
                            java.util.Base64.getUrlDecoder().decode(parts[0]),
                            StandardCharsets.UTF_8);

            // Extraer kid del header JSON de forma simple
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

    /** Extrae el kid del header JSON. */
    private String extractKidFromHeader(String headerJson) {
        // Búsqueda simple de "kid":"value" en el JSON
        int kidIndex = headerJson.indexOf("\"kid\"");
        if (kidIndex == -1) {
            return null;
        }

        int colonIndex = headerJson.indexOf(':', kidIndex);
        if (colonIndex == -1) {
            return null;
        }

        int startQuote = headerJson.indexOf('"', colonIndex);
        if (startQuote == -1) {
            return null;
        }

        int endQuote = headerJson.indexOf('"', startQuote + 1);
        if (endQuote == -1) {
            return null;
        }

        return headerJson.substring(startQuote + 1, endQuote);
    }

    /** Valida si el token es válido para el usuario dado. Incluye verificación de blacklist. */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            final String tokenId = extractTokenId(token);

            // Verificar blacklist
            if (blacklistService.isBlacklisted(tokenId)) {
                log.debug("Token {} está en blacklist", tokenId);
                return false;
            }

            return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Token inválido: {}", e.getMessage());
            return false;
        }
    }

    /** Verifica si el token está expirado. */
    public boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).isBefore(Instant.now());
        } catch (ExpiredJwtException _) {
            return true;
        }
    }

    /** Verifica si el token es un access token. */
    public boolean isAccessToken(String token) {
        return "access".equals(extractTokenType(token));
    }

    /** Verifica si el token es un refresh token. */
    public boolean isRefreshToken(String token) {
        return "refresh".equals(extractTokenType(token));
    }

    /**
     * Valida la estructura y firma del token sin verificar expiración. Útil para refresh tokens.
     */
    public boolean isTokenStructureValid(String token) {
        try {
            SecretKey key = resolveSigningKey(token);
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Estructura de token inválida: {}", e.getMessage());
            return false;
        }
    }

    /** Extrae claims de un token potencialmente expirado. Útil para refresh tokens. */
    public Claims extractClaimsIgnoringExpiration(String token) {
        try {
            SecretKey key = resolveSigningKey(token);
            return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }

    /**
     * Obtiene el ID de la clave actual usada para firmar nuevos tokens.
     *
     * @return el keyId actual
     */
    public String getCurrentKeyId() {
        return currentKeyId;
    }

    /**
     * Verifica si la rotación de claves está habilitada.
     *
     * @return true si la rotación está habilitada
     */
    public boolean isKeyRotationEnabled() {
        return rotationEnabled;
    }

    /**
     * Obtiene la cantidad de claves registradas (incluyendo anteriores).
     *
     * @return número de claves en el registry
     */
    public int getRegisteredKeyCount() {
        return keyRegistry.size();
    }
}
