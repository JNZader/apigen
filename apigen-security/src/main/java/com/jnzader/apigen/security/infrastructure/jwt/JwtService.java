package com.jnzader.apigen.security.infrastructure.jwt;

import com.jnzader.apigen.security.infrastructure.config.SecurityProperties;
import com.jnzader.apigen.security.domain.entity.User;
import com.jnzader.apigen.security.application.service.TokenBlacklistService;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * Servicio para generación y validación de tokens JWT.
 * <p>
 * Características:
 * - Access tokens con claims de usuario
 * - Refresh tokens para renovación
 * - Integración con blacklist para revocación
 * - Token ID (jti) único para cada token
 */
@Service
@ConditionalOnProperty(name = "apigen.security.enabled", havingValue = "true")
@ConditionalOnExpression("'${apigen.security.mode:jwt}'.equalsIgnoreCase('jwt')")
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);
    private static final String CLAIM_USER_ID = "userId";

    private final SecurityProperties securityProperties;
    private final TokenBlacklistService blacklistService;
    private final SecretKey secretKey;

    public JwtService(SecurityProperties securityProperties, TokenBlacklistService blacklistService) {
        this.securityProperties = securityProperties;
        this.blacklistService = blacklistService;
        this.secretKey = Keys.hmacShaKeyFor(
                securityProperties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8)
        );
    }

    /**
     * Genera un token de acceso para el usuario.
     */
    public String generateAccessToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_USER_ID, user.getId());
        claims.put("role", user.getRole().getName());
        claims.put("email", user.getEmail());
        claims.put("type", "access");

        return buildToken(claims, user.getUsername(),
                securityProperties.getJwt().getExpirationMinutes());
    }

    /**
     * Genera un refresh token para el usuario.
     */
    public String generateRefreshToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_USER_ID, user.getId());
        claims.put("type", "refresh");

        return buildToken(claims, user.getUsername(),
                securityProperties.getJwt().getRefreshExpirationMinutes());
    }

    /**
     * Construye un token JWT con los claims y duración especificados.
     */
    private String buildToken(Map<String, Object> claims, String subject, int expirationMinutes) {
        Instant now = Instant.now();
        Instant expiration = now.plus(expirationMinutes, ChronoUnit.MINUTES);

        return Jwts.builder()
                .id(UUID.randomUUID().toString()) // Token ID único para blacklist
                .claims(claims)
                .subject(subject)
                .issuer(securityProperties.getJwt().getIssuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Extrae el nombre de usuario del token.
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extrae el ID del token (jti).
     */
    public String extractTokenId(String token) {
        return extractClaim(token, Claims::getId);
    }

    /**
     * Extrae el ID del usuario del token.
     */
    public Long extractUserId(String token) {
        return extractClaim(token, claims -> claims.get(CLAIM_USER_ID, Long.class));
    }

    /**
     * Extrae el rol del usuario del token.
     */
    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    /**
     * Extrae el tipo de token (access/refresh).
     */
    public String extractTokenType(String token) {
        return extractClaim(token, claims -> claims.get("type", String.class));
    }

    /**
     * Extrae la fecha de expiración del token.
     */
    public Instant extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration).toInstant();
    }

    /**
     * Extrae un claim específico del token.
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Extrae todos los claims del token.
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Valida si el token es válido para el usuario dado.
     * Incluye verificación de blacklist.
     */
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

    /**
     * Verifica si el token está expirado.
     */
    public boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).isBefore(Instant.now());
        } catch (ExpiredJwtException _) {
            return true;
        }
    }

    /**
     * Verifica si el token es un access token.
     */
    public boolean isAccessToken(String token) {
        return "access".equals(extractTokenType(token));
    }

    /**
     * Verifica si el token es un refresh token.
     */
    public boolean isRefreshToken(String token) {
        return "refresh".equals(extractTokenType(token));
    }

    /**
     * Valida la estructura y firma del token sin verificar expiración.
     * Útil para refresh tokens.
     */
    public boolean isTokenStructureValid(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Estructura de token inválida: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Extrae claims de un token potencialmente expirado.
     * Útil para refresh tokens.
     */
    public Claims extractClaimsIgnoringExpiration(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }
}
