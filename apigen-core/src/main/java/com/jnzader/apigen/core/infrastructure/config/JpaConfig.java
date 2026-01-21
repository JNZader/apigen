package com.jnzader.apigen.core.infrastructure.config;

import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA and auditing configuration.
 *
 * <p>Enables JPA Auditing to automatically populate the fields: - createdBy / modifiedBy -
 * createdDate / updatedDate
 *
 * <p>IMPORTANT: This configuration is excluded from TestApplication's component scan to prevent
 * loading in @WebMvcTest. Import it explicitly in integration tests that need JPA
 * auditing: @Import(JpaConfig.class)
 *
 * <p>This default configuration provides a simple AuditorAware that returns "system". When Spring
 * Security is available, JpaSecurityConfig will override this with a security-aware implementation.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaConfig {

    /**
     * Provides the current user for auditing (version without Spring Security). Returns "system" as
     * the default user. This bean will be replaced by JpaSecurityConfig when Spring Security is
     * available.
     */
    @Bean
    @ConditionalOnMissingBean(name = "securityAuditorProvider")
    public AuditorAware<String> auditorProvider() {
        return () -> Optional.of("system");
    }
}
