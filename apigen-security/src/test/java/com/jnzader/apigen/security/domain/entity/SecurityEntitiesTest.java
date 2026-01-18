package com.jnzader.apigen.security.domain.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Security Entities Tests")
class SecurityEntitiesTest {

    @Nested
    @DisplayName("User Entity")
    class UserEntityTests {

        private User user;
        private Role role;

        @BeforeEach
        void setUp() {
            Permission readPermission = new Permission();
            readPermission.setId(1L);
            readPermission.setName("READ");

            Permission writePermission = new Permission();
            writePermission.setId(2L);
            writePermission.setName("WRITE");

            Set<Permission> permissions = new HashSet<>();
            permissions.add(readPermission);
            permissions.add(writePermission);

            role = new Role();
            role.setId(1L);
            role.setName("ADMIN");
            role.setPermissions(permissions);

            user = new User("testuser", "password123", "test@example.com", role);
            user.setId(1L);
        }

        @Test
        @DisplayName("should create user with constructor")
        void shouldCreateUserWithConstructor() {
            assertThat(user.getUsername()).isEqualTo("testuser");
            assertThat(user.getPassword()).isEqualTo("password123");
            assertThat(user.getEmail()).isEqualTo("test@example.com");
            assertThat(user.getRole()).isEqualTo(role);
        }

        @Test
        @DisplayName("should create user with default constructor")
        void shouldCreateUserWithDefaultConstructor() {
            User newUser = new User();
            assertThat(newUser.getUsername()).isNull();
            assertThat(newUser.isAccountNonExpired()).isTrue();
            assertThat(newUser.isAccountNonLocked()).isTrue();
            assertThat(newUser.isCredentialsNonExpired()).isTrue();
        }

        @Test
        @DisplayName("should return authorities from role and permissions")
        void shouldReturnAuthoritiesFromRoleAndPermissions() {
            Collection<? extends GrantedAuthority> authorities = user.getAuthorities();

            assertThat(authorities)
                    .isNotEmpty()
                    .extracting(GrantedAuthority::getAuthority)
                    .contains("ROLE_ADMIN", "READ", "WRITE");
        }

        @Test
        @DisplayName("should cache authorities")
        void shouldCacheAuthorities() {
            Collection<? extends GrantedAuthority> first = user.getAuthorities();
            Collection<? extends GrantedAuthority> second = user.getAuthorities();

            assertThat(first).isSameAs(second);
        }

        @Test
        @DisplayName("should invalidate authorities cache when role changes")
        void shouldInvalidateAuthoritiesCacheWhenRoleChanges() {
            Collection<? extends GrantedAuthority> first = user.getAuthorities();

            Role newRole = new Role();
            newRole.setName("USER");
            newRole.setPermissions(new HashSet<>());
            user.setRole(newRole);

            Collection<? extends GrantedAuthority> second = user.getAuthorities();

            assertThat(first).isNotSameAs(second);
        }

        @Test
        @DisplayName("should return enabled when both enabled and estado are true")
        void shouldReturnEnabledWhenBothTrue() {
            user.setEnabled(true);
            user.setEstado(true);

            assertThat(user.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("should return disabled when estado is false")
        void shouldReturnDisabledWhenEstadoFalse() {
            user.setEnabled(true);
            user.setEstado(false);

            assertThat(user.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("should return disabled when enabled is false")
        void shouldReturnDisabledWhenEnabledFalse() {
            user.setEnabled(false);
            user.setEstado(true);

            assertThat(user.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("should return full name with first and last name")
        void shouldReturnFullNameWithFirstAndLastName() {
            user.setFirstName("John");
            user.setLastName("Doe");

            assertThat(user.getFullName()).isEqualTo("John Doe");
        }

        @Test
        @DisplayName("should return first name only when last name is null")
        void shouldReturnFirstNameOnlyWhenLastNameNull() {
            user.setFirstName("John");
            user.setLastName(null);

            assertThat(user.getFullName()).isEqualTo("John");
        }

        @Test
        @DisplayName("should return last name only when first name is null")
        void shouldReturnLastNameOnlyWhenFirstNameNull() {
            user.setFirstName(null);
            user.setLastName("Doe");

            assertThat(user.getFullName()).isEqualTo("Doe");
        }

        @Test
        @DisplayName("should return username when no names set")
        void shouldReturnUsernameWhenNoNamesSet() {
            user.setFirstName(null);
            user.setLastName(null);

            assertThat(user.getFullName()).isEqualTo("testuser");
        }

        @Test
        @DisplayName("should check permission correctly")
        void shouldCheckPermissionCorrectly() {
            assertThat(user.hasPermission("READ")).isTrue();
            assertThat(user.hasPermission("DELETE")).isFalse();
        }

        @Test
        @DisplayName("should return false for hasPermission when role is null")
        void shouldReturnFalseForHasPermissionWhenRoleNull() {
            user.setRole(null);
            assertThat(user.hasPermission("READ")).isFalse();
        }

        @Test
        @DisplayName("should check role correctly")
        void shouldCheckRoleCorrectly() {
            assertThat(user.hasRole("ADMIN")).isTrue();
            assertThat(user.hasRole("USER")).isFalse();
        }

        @Test
        @DisplayName("should return false for hasRole when role is null")
        void shouldReturnFalseForHasRoleWhenRoleNull() {
            user.setRole(null);
            assertThat(user.hasRole("ADMIN")).isFalse();
        }

        @Test
        @DisplayName("should set and get all fields")
        void shouldSetAndGetAllFields() {
            Instant now = Instant.now();
            user.setLastLoginAt(now);
            user.setLastLoginIp("192.168.1.1");
            user.setAccountNonExpired(false);
            user.setAccountNonLocked(false);
            user.setCredentialsNonExpired(false);

            assertThat(user.getLastLoginAt()).isEqualTo(now);
            assertThat(user.getLastLoginIp()).isEqualTo("192.168.1.1");
            assertThat(user.isAccountNonExpired()).isFalse();
            assertThat(user.isAccountNonLocked()).isFalse();
            assertThat(user.isCredentialsNonExpired()).isFalse();
        }
    }

    @Nested
    @DisplayName("Role Entity")
    class RoleEntityTests {

        private Role role;

        @BeforeEach
        void setUp() {
            role = new Role();
            role.setId(1L);
            role.setName("ADMIN");
            role.setDescription("Administrator role");
        }

        @Test
        @DisplayName("should create role with default constructor")
        void shouldCreateRoleWithDefaultConstructor() {
            Role newRole = new Role();
            assertThat(newRole.getPermissions()).isEmpty();
            assertThat(newRole.getName()).isNull();
        }

        @Test
        @DisplayName("should create role with name constructor")
        void shouldCreateRoleWithNameConstructor() {
            Role newRole = new Role("USER");
            assertThat(newRole.getName()).isEqualTo("USER");
        }

        @Test
        @DisplayName("should create role with name and description constructor")
        void shouldCreateRoleWithNameAndDescriptionConstructor() {
            Role newRole = new Role("MANAGER", "Manager role");
            assertThat(newRole.getName()).isEqualTo("MANAGER");
            assertThat(newRole.getDescription()).isEqualTo("Manager role");
        }

        @Test
        @DisplayName("should set and get all fields")
        void shouldSetAndGetAllFields() {
            assertThat(role.getId()).isEqualTo(1L);
            assertThat(role.getName()).isEqualTo("ADMIN");
            assertThat(role.getDescription()).isEqualTo("Administrator role");
        }

        @Test
        @DisplayName("should check permission correctly")
        void shouldCheckPermissionCorrectly() {
            Permission permission = new Permission();
            permission.setName("READ");

            Set<Permission> permissions = new HashSet<>();
            permissions.add(permission);
            role.setPermissions(permissions);

            assertThat(role.hasPermission("READ")).isTrue();
            assertThat(role.hasPermission("WRITE")).isFalse();
        }

        @Test
        @DisplayName("should add permission")
        void shouldAddPermission() {
            Permission permission = new Permission();
            permission.setName("NEW_PERMISSION");

            role.addPermission(permission);

            assertThat(role.getPermissions()).contains(permission);
        }

        @Test
        @DisplayName("should remove permission")
        void shouldRemovePermission() {
            Permission permission = new Permission();
            permission.setName("TO_REMOVE");

            role.addPermission(permission);
            role.removePermission(permission);

            assertThat(role.getPermissions()).doesNotContain(permission);
        }
    }

    @Nested
    @DisplayName("Permission Entity")
    class PermissionEntityTests {

        @Test
        @DisplayName("should create permission with default values")
        void shouldCreatePermissionWithDefaultValues() {
            Permission permission = new Permission();
            assertThat(permission.getName()).isNull();
            assertThat(permission.getDescription()).isNull();
        }

        @Test
        @DisplayName("should set and get all fields")
        void shouldSetAndGetAllFields() {
            Permission permission = new Permission();
            permission.setId(1L);
            permission.setName("READ");
            permission.setDescription("Read permission");

            assertThat(permission.getId()).isEqualTo(1L);
            assertThat(permission.getName()).isEqualTo("READ");
            assertThat(permission.getDescription()).isEqualTo("Read permission");
        }
    }

    @Nested
    @DisplayName("TokenBlacklist Entity")
    class TokenBlacklistEntityTests {

        @Test
        @DisplayName("should create token blacklist entry with constructor")
        void shouldCreateTokenBlacklistEntryWithConstructor() {
            Instant expiry = Instant.now().plusSeconds(3600);
            TokenBlacklist tokenBlacklist = new TokenBlacklist(
                    "token123", "testuser", expiry, TokenBlacklist.BlacklistReason.LOGOUT);

            assertThat(tokenBlacklist.getTokenId()).isEqualTo("token123");
            assertThat(tokenBlacklist.getUsername()).isEqualTo("testuser");
            assertThat(tokenBlacklist.getExpiration()).isEqualTo(expiry);
            assertThat(tokenBlacklist.getBlacklistedAt()).isNotNull();
            assertThat(tokenBlacklist.getReason()).isEqualTo(TokenBlacklist.BlacklistReason.LOGOUT);
        }

        @Test
        @DisplayName("should create token blacklist with default constructor")
        void shouldCreateTokenBlacklistWithDefaultConstructor() {
            TokenBlacklist tokenBlacklist = new TokenBlacklist();
            assertThat(tokenBlacklist.getTokenId()).isNull();
            assertThat(tokenBlacklist.getUsername()).isNull();
        }

        @Test
        @DisplayName("should set and get all fields")
        void shouldSetAndGetAllFields() {
            TokenBlacklist tokenBlacklist = new TokenBlacklist();
            Instant now = Instant.now();

            tokenBlacklist.setId(1L);
            tokenBlacklist.setTokenId("token456");
            tokenBlacklist.setUsername("user123");
            tokenBlacklist.setExpiration(now);
            tokenBlacklist.setBlacklistedAt(now);
            tokenBlacklist.setReason(TokenBlacklist.BlacklistReason.PASSWORD_CHANGE);

            assertThat(tokenBlacklist.getId()).isEqualTo(1L);
            assertThat(tokenBlacklist.getTokenId()).isEqualTo("token456");
            assertThat(tokenBlacklist.getUsername()).isEqualTo("user123");
            assertThat(tokenBlacklist.getExpiration()).isEqualTo(now);
            assertThat(tokenBlacklist.getBlacklistedAt()).isEqualTo(now);
            assertThat(tokenBlacklist.getReason()).isEqualTo(TokenBlacklist.BlacklistReason.PASSWORD_CHANGE);
        }

        @Test
        @DisplayName("should have all blacklist reasons")
        void shouldHaveAllBlacklistReasons() {
            assertThat(TokenBlacklist.BlacklistReason.values())
                    .containsExactlyInAnyOrder(
                            TokenBlacklist.BlacklistReason.LOGOUT,
                            TokenBlacklist.BlacklistReason.PASSWORD_CHANGE,
                            TokenBlacklist.BlacklistReason.ADMIN_REVOKE,
                            TokenBlacklist.BlacklistReason.SECURITY_BREACH,
                            TokenBlacklist.BlacklistReason.SESSION_EXPIRED,
                            TokenBlacklist.BlacklistReason.TOKEN_ROTATED
                    );
        }
    }
}
