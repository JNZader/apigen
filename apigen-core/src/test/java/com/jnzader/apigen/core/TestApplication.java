package com.jnzader.apigen.core;

import com.jnzader.apigen.core.config.TestSecurityConfig;
import com.jnzader.apigen.core.infrastructure.config.JpaConfig;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

/**
 * Minimal test application for Spring Boot tests.
 *
 * Excludes:
 * - TestcontainersConfiguration (for unit tests)
 * - TestSecurityConfig (import explicitly in integration tests)
 * - JpaConfig (import explicitly in integration tests that need JPA auditing)
 */
@SpringBootApplication
@ComponentScan(
        basePackages = {"com.jnzader.apigen"},
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = TestcontainersConfiguration.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = TestSecurityConfig.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaConfig.class)
        }
)
public class TestApplication {
    // Empty class - Spring Boot auto-configuration handles all setup via annotations
}
