package com.jnzader.apigen.security.domain.repository;

import com.jnzader.apigen.security.domain.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para la entidad Permission.
 */
@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {

    /**
     * Busca un permiso por su nombre.
     */
    Optional<Permission> findByName(String name);

    /**
     * Verifica si existe un permiso con el nombre dado.
     */
    boolean existsByName(String name);

    /**
     * Busca permisos por categoría.
     */
    List<Permission> findByCategory(String category);

    /**
     * Busca permisos cuyo nombre contenga el texto dado.
     */
    List<Permission> findByNameContainingIgnoreCase(String name);
}
