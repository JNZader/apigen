package com.jnzader.apigen.core.infrastructure.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

/**
 * Configuración de JPA y auditoría.
 * <p>
 * Habilita JPA Auditing para poblar automáticamente los campos:
 * - creadoPor / modificadoPor
 * - fechaCreacion / fechaActualizacion
 * <p>
 * IMPORTANT: This configuration is excluded from TestApplication's component scan
 * to prevent loading in @WebMvcTest. Import it explicitly in integration tests
 * that need JPA auditing: @Import(JpaConfig.class)
 * <p>
 * This default configuration provides a simple AuditorAware that returns "system".
 * When Spring Security is available, JpaSecurityConfig will override this with
 * a security-aware implementation.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaConfig {

    /**
     * Provee el usuario actual para auditoría (versión sin Spring Security).
     * Retorna "system" como usuario por defecto.
     * Este bean será reemplazado por JpaSecurityConfig cuando Spring Security esté disponible.
     */
    @Bean
    @ConditionalOnMissingBean(name = "securityAuditorProvider")
    public AuditorAware<String> auditorProvider() {
        return () -> Optional.of("system");
    }
}
