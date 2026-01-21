package com.jnzader.apigen.security.domain.entity;

import com.jnzader.apigen.core.domain.entity.Base;
import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * Entidad que representa un rol en el sistema.
 *
 * <p>Los roles agrupan permisos y se asignan a usuarios. Ejemplos: ADMIN, MANAGER, USER, GUEST.
 *
 * <p>This entity is cached in Hibernate L2 cache for improved performance since roles are
 * frequently read but rarely modified.
 */
@Entity
@Table(name = "roles")
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "roles")
@SuppressWarnings("java:S2160") // equals/hashCode heredados de Base (basado en ID)
public class Role extends Base {

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Column(length = 255)
    private String description;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "role_permissions",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id"))
    @BatchSize(size = 25)
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "role_permissions")
    private Set<Permission> permissions = new HashSet<>();

    public Role() {}

    public Role(String name) {
        this.name = name;
    }

    public Role(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Set<Permission> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<Permission> permissions) {
        this.permissions = permissions;
    }

    public void addPermission(Permission permission) {
        this.permissions.add(permission);
    }

    public void removePermission(Permission permission) {
        this.permissions.remove(permission);
    }

    /** Verifica si el rol tiene un permiso específico. */
    public boolean hasPermission(String permissionName) {
        return permissions.stream().anyMatch(p -> p.getName().equals(permissionName));
    }
}
