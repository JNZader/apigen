package com.jnzader.apigen.security.domain.entity;

import com.jnzader.apigen.core.domain.entity.Base;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Entidad que representa un usuario en el sistema.
 *
 * <p>Implementa UserDetails de Spring Security para integración directa con el sistema de
 * autenticación.
 */
@Entity
@Table(
        name = "users",
        indexes = {
            @Index(name = "idx_users_username", columnList = "username"),
            @Index(name = "idx_users_email", columnList = "email")
        })
@SuppressWarnings("java:S2160") // equals/hashCode heredados de Base (basado en ID)
public class User extends Base implements UserDetails {

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(length = 100)
    private String firstName;

    @Column(length = 100)
    private String lastName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @Column(nullable = false)
    private boolean accountNonExpired = true;

    @Column(nullable = false)
    private boolean accountNonLocked = true;

    @Column(nullable = false)
    private boolean credentialsNonExpired = true;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column private Instant lastLoginAt;

    @Column private String lastLoginIp;

    /**
     * Cache de authorities para evitar N+1 queries. Se invalida automáticamente cuando cambia el
     * rol.
     */
    @Transient private transient Collection<? extends GrantedAuthority> cachedAuthorities;

    public User() {}

    public User(String username, String password, String email, Role role) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.role = role;
    }

    // ==================== UserDetails Implementation ====================

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Retornar cache si existe (evita N+1 queries)
        if (cachedAuthorities != null) {
            return cachedAuthorities;
        }

        // Construir authorities desde rol y permisos
        Set<SimpleGrantedAuthority> authorities =
                role.getPermissions().stream()
                        .map(permission -> new SimpleGrantedAuthority(permission.getName()))
                        .collect(Collectors.toSet());

        // Agregar el rol también (prefijo ROLE_ para Spring Security)
        authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName()));

        // Cachear resultado inmutable
        this.cachedAuthorities = Collections.unmodifiableSet(authorities);
        return cachedAuthorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return accountNonExpired;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return credentialsNonExpired;
    }

    @Override
    public boolean isEnabled() {
        return enabled && getEstado(); // Combinar con estado de Base
    }

    // ==================== Getters & Setters ====================

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
        // Invalidar cache de authorities cuando cambia el rol
        this.cachedAuthorities = null;
    }

    public void setAccountNonExpired(boolean accountNonExpired) {
        this.accountNonExpired = accountNonExpired;
    }

    public void setAccountNonLocked(boolean accountNonLocked) {
        this.accountNonLocked = accountNonLocked;
    }

    public void setCredentialsNonExpired(boolean credentialsNonExpired) {
        this.credentialsNonExpired = credentialsNonExpired;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(Instant lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public String getLastLoginIp() {
        return lastLoginIp;
    }

    public void setLastLoginIp(String lastLoginIp) {
        this.lastLoginIp = lastLoginIp;
    }

    // ==================== Helper Methods ====================

    public String getFullName() {
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        }
        if (firstName != null) {
            return firstName;
        }
        if (lastName != null) {
            return lastName;
        }
        return username;
    }

    public boolean hasPermission(String permissionName) {
        return role != null && role.hasPermission(permissionName);
    }

    public boolean hasRole(String roleName) {
        return role != null && role.getName().equals(roleName);
    }
}
