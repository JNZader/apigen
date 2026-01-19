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
 * Configuración de auditoría con Spring Security.
 *
 * <p>Esta configuración solo se activa cuando Spring Security está disponible en el classpath.
 * Provee un AuditorAware que obtiene el usuario actual del SecurityContext.
 */
@Configuration
@ConditionalOnClass(name = "org.springframework.security.core.context.SecurityContextHolder")
public class JpaSecurityConfig {

    /**
     * Provee el usuario actual para auditoría basado en Spring Security. Obtiene el nombre del
     * usuario autenticado del SecurityContext.
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

            // Si es anónimo, usar "anonymous"
            if ("anonymousUser".equals(principal)) {
                return Optional.of("anonymous");
            }

            return Optional.of(principal);
        };
    }
}
