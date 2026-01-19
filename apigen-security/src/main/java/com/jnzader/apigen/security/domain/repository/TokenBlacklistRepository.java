package com.jnzader.apigen.security.domain.repository;

import com.jnzader.apigen.security.domain.entity.TokenBlacklist;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/** Repositorio para gestionar tokens invalidados. */
@Repository
public interface TokenBlacklistRepository extends JpaRepository<TokenBlacklist, Long> {

    /** Verifica si un token está en la blacklist. */
    boolean existsByTokenId(String tokenId);

    /**
     * Elimina tokens expirados de la blacklist.
     *
     * @return Número de tokens eliminados
     */
    @Modifying
    @Query("DELETE FROM TokenBlacklist t WHERE t.expiration < :now")
    int deleteExpiredTokens(Instant now);

    /** Invalida todos los tokens de un usuario. Útil cuando cambia la contraseña. */
    @Modifying
    @Query("DELETE FROM TokenBlacklist t WHERE t.username = :username")
    int deleteByUsername(String username);

    /** Cuenta tokens invalidados por usuario. */
    long countByUsername(String username);
}
