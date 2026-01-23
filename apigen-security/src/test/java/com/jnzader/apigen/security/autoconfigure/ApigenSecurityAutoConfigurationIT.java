package com.jnzader.apigen.security.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import com.jnzader.apigen.security.domain.entity.Permission;
import com.jnzader.apigen.security.domain.entity.Role;
import com.jnzader.apigen.security.domain.entity.TokenBlacklist;
import com.jnzader.apigen.security.domain.entity.User;
import com.jnzader.apigen.security.domain.repository.PermissionRepository;
import com.jnzader.apigen.security.domain.repository.RoleRepository;
import com.jnzader.apigen.security.domain.repository.TokenBlacklistRepository;
import com.jnzader.apigen.security.domain.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.ManagedType;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.ApplicationContext;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;

/**
 * Integration test for ApigenSecurityAutoConfiguration JPA components.
 *
 * <p>This test validates that:
 *
 * <ul>
 *   <li>JPA entities in domain.entity are scanned as managed types
 *   <li>JPA repositories in domain.repository are properly registered
 * </ul>
 *
 * <p>This test catches configuration errors like "Not a managed type" that unit tests miss.
 */
@DataJpaTest
@ContextConfiguration(classes = ApigenSecurityAutoConfigurationIT.TestJpaConfig.class)
@DisplayName("ApigenSecurityAutoConfiguration JPA Integration Tests")
class ApigenSecurityAutoConfigurationIT {

    @Autowired private ApplicationContext applicationContext;

    @Autowired private EntityManager entityManager;

    @Test
    @DisplayName("should scan all security entities as JPA managed types")
    void shouldScanAllSecurityEntitiesAsManagedTypes() {
        Set<Class<?>> managedTypes =
                entityManager.getMetamodel().getManagedTypes().stream()
                        .map(ManagedType::getJavaType)
                        .collect(Collectors.toSet());

        // All security entities must be recognized as JPA managed types
        assertThat(managedTypes)
                .as("Security entities should be scanned by @EntityScan")
                .contains(TokenBlacklist.class, User.class, Role.class, Permission.class);
    }

    @Test
    @DisplayName("should register TokenBlacklistRepository as bean")
    void shouldRegisterTokenBlacklistRepository() {
        assertThat(applicationContext.getBean(TokenBlacklistRepository.class))
                .as("TokenBlacklistRepository should be registered")
                .isNotNull();
    }

    @Test
    @DisplayName("should register UserRepository as bean")
    void shouldRegisterUserRepository() {
        assertThat(applicationContext.getBean(UserRepository.class))
                .as("UserRepository should be registered")
                .isNotNull();
    }

    @Test
    @DisplayName("should register RoleRepository as bean")
    void shouldRegisterRoleRepository() {
        assertThat(applicationContext.getBean(RoleRepository.class))
                .as("RoleRepository should be registered")
                .isNotNull();
    }

    @Test
    @DisplayName("should register PermissionRepository as bean")
    void shouldRegisterPermissionRepository() {
        assertThat(applicationContext.getBean(PermissionRepository.class))
                .as("PermissionRepository should be registered")
                .isNotNull();
    }

    /**
     * Minimal JPA configuration that mirrors ApigenSecurityAutoConfiguration's JPA settings. This
     * tests the same @EntityScan and @EnableJpaRepositories configuration without loading all the
     * security beans.
     */
    @EnableAutoConfiguration
    @EntityScan(basePackages = "com.jnzader.apigen.security.domain.entity")
    @EnableJpaRepositories(basePackages = "com.jnzader.apigen.security.domain.repository")
    static class TestJpaConfig {
        // Minimal config to test JPA scanning - mirrors ApigenSecurityAutoConfiguration
    }
}
