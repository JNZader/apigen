package com.jnzader.apigen.security.domain.entity;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Entidad para almacenar tokens JWT invalidados.
 *
 * <p>Permite revocar tokens antes de su expiración natural.
 */
@Entity
@Table(
        name = "token_blacklist",
        indexes = {
            @Index(name = "idx_token_blacklist_token_id", columnList = "token_id"),
            @Index(name = "idx_token_blacklist_expiration", columnList = "expiration"),
            @Index(name = "idx_token_blacklist_username", columnList = "username")
        })
public class TokenBlacklist {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "base_seq_gen")
    @SequenceGenerator(name = "base_seq_gen", sequenceName = "base_sequence", allocationSize = 50)
    private Long id;

    /** Identificador único del token (claim jti). */
    @Column(name = "token_id", nullable = false, unique = true)
    private String tokenId;

    /** Usuario propietario del token. */
    @Column(nullable = false, length = 50)
    private String username;

    /** Fecha de expiración original del token. Usado para limpieza automática. */
    @Column(nullable = false)
    private Instant expiration;

    /** Fecha en que se añadió a la blacklist. */
    @Column(name = "blacklisted_at", nullable = false)
    private Instant blacklistedAt;

    /** Razón de la invalidación. */
    @Column(length = 50)
    @Enumerated(EnumType.STRING)
    private BlacklistReason reason;

    public TokenBlacklist() {}

    public TokenBlacklist(
            String tokenId, String username, Instant expiration, BlacklistReason reason) {
        this.tokenId = tokenId;
        this.username = username;
        this.expiration = expiration;
        this.blacklistedAt = Instant.now();
        this.reason = reason;
    }

    public enum BlacklistReason {
        LOGOUT,
        PASSWORD_CHANGE,
        ADMIN_REVOKE,
        SECURITY_BREACH,
        SESSION_EXPIRED,
        TOKEN_ROTATED
    }

    // Getters y Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTokenId() {
        return tokenId;
    }

    public void setTokenId(String tokenId) {
        this.tokenId = tokenId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Instant getExpiration() {
        return expiration;
    }

    public void setExpiration(Instant expiration) {
        this.expiration = expiration;
    }

    public Instant getBlacklistedAt() {
        return blacklistedAt;
    }

    public void setBlacklistedAt(Instant blacklistedAt) {
        this.blacklistedAt = blacklistedAt;
    }

    public BlacklistReason getReason() {
        return reason;
    }

    public void setReason(BlacklistReason reason) {
        this.reason = reason;
    }
}
