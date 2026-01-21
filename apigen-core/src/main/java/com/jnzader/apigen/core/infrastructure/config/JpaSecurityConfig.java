package com.jnzader.apigen.core.infrastructure.config;

import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Auditing configuration with Spring Security.
 *
 * <p>This configuration is only activated when Spring Security is available in the classpath.
 * Provides an AuditorAware that gets the current user from the SecurityContext.
 */
@Configuration
@ConditionalOnClass(name = "org.springframework.security.core.context.SecurityContextHolder")
public class JpaSecurityConfig {

    /**
     * Provides the current user for auditing based on Spring Security. Gets the authenticated
     * user's name from the SecurityContext.
     */
    @Bean
    @Primary
    public AuditorAware<String> securityAuditorProvider() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || !authentication.isAuthenticated()) {
                return Optional.of("system");
            }

            String principal = authentication.getName();

            // If anonymous, use "anonymous"
            if ("anonymousUser".equals(principal)) {
                return Optional.of("anonymous");
            }

            return Optional.of(principal);
        };
    }
}
