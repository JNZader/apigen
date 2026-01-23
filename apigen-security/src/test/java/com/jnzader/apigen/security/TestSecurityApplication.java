package com.jnzader.apigen.security;

import com.jnzader.apigen.core.autoconfigure.ApigenCoreAutoConfiguration;
import com.jnzader.apigen.core.autoconfigure.ApigenJpaAutoConfiguration;
import com.jnzader.apigen.security.infrastructure.config.SecurityProperties;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Minimal test application for Spring Boot tests in the security module.
 *
 * <p>Scans security packages and excludes JPA auto-configuration from core module that requires
 * EntityManagerFactory bean.
 *
 * <p>Note: @EnableConfigurationProperties is needed because the auto-configurations are excluded
 * and SecurityProperties is no longer annotated with @Component (to avoid duplicate bean
 * registration in production).
 */
@SpringBootApplication(
        scanBasePackages = "com.jnzader.apigen.security",
        exclude = {ApigenJpaAutoConfiguration.class, ApigenCoreAutoConfiguration.class})
@EnableConfigurationProperties(SecurityProperties.class)
public class TestSecurityApplication {
    // Empty class - Spring Boot auto-configuration handles all setup via annotations
}
