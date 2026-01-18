package com.jnzader.apigen.security.domain.repository;

import com.jnzader.apigen.security.domain.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio para la entidad Role.
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    /**
     * Busca un rol por su nombre.
     */
    Optional<Role> findByName(String name);

    /**
     * Verifica si existe un rol con el nombre dado.
     */
    boolean existsByName(String name);
}
