package com.jnzader.apigen.security;

import com.jnzader.apigen.core.autoconfigure.ApigenCoreAutoConfiguration;
import com.jnzader.apigen.core.autoconfigure.ApigenJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Minimal test application for Spring Boot tests in the security module.
 * <p>
 * Scans security packages and excludes JPA auto-configuration from core
 * module that requires EntityManagerFactory bean.
 */
@SpringBootApplication(
        scanBasePackages = "com.jnzader.apigen.security",
        exclude = {ApigenJpaAutoConfiguration.class, ApigenCoreAutoConfiguration.class}
)
public class TestSecurityApplication {
    // Empty class - Spring Boot auto-configuration handles all setup via annotations
}
