package com.jnzader.apigen.security.domain.repository;

import com.jnzader.apigen.security.domain.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio para la entidad User.
 * <p>
 * Usa @EntityGraph para cargar rol y permisos eficientemente en queries de autenticación.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Busca un usuario por nombre de usuario con rol y permisos.
     * Usado en autenticación.
     */
    @EntityGraph(attributePaths = {"role", "role.permissions"})
    Optional<User> findByUsername(String username);

    /**
     * Busca un usuario por email con rol y permisos.
     */
    @EntityGraph(attributePaths = {"role", "role.permissions"})
    Optional<User> findByEmail(String email);

    /**
     * Verifica si existe un usuario con el nombre de usuario dado.
     */
    boolean existsByUsername(String username);

    /**
     * Verifica si existe un usuario con el email dado.
     */
    boolean existsByEmail(String email);

    /**
     * Busca un usuario activo por nombre de usuario con rol y permisos.
     * Usado en CustomUserDetailsService.
     */
    @EntityGraph(attributePaths = {"role", "role.permissions"})
    @Query("SELECT u FROM User u WHERE u.username = :username AND u.estado = true AND u.enabled = true")
    Optional<User> findActiveByUsername(String username);

    /**
     * Cuenta usuarios por rol.
     */
    long countByRoleName(String roleName);
}
