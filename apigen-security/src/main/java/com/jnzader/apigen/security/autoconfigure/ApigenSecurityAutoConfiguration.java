package com.jnzader.apigen.security.autoconfigure;

import com.jnzader.apigen.core.autoconfigure.ApigenCoreAutoConfiguration;
import com.jnzader.apigen.security.infrastructure.config.OAuth2SecurityConfig;
import com.jnzader.apigen.security.infrastructure.config.SecurityConfig;
import com.jnzader.apigen.security.infrastructure.config.SecurityProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

/**
 * Auto-configuration for APiGen Security module.
 *
 * <p>This auto-configuration automatically enables:
 *
 * <ul>
 *   <li>JWT authentication with access and refresh tokens
 *   <li>Security components via ComponentScan
 *   <li>Auth controller for login/logout/refresh endpoints
 *   <li>Rate limiting for authentication endpoints
 *   <li>Security audit logging
 * </ul>
 *
 * <p>Security modes (configurable via {@code apigen.security.mode}):
 *
 * <ul>
 *   <li>{@code jwt} - JWT-based authentication (default)
 *   <li>{@code oauth2} - OAuth2 resource server mode
 * </ul>
 *
 * <p>Can be disabled with: {@code apigen.security.enabled=false}
 *
 * <p>Usage: Just add the dependency and configure security properties:
 *
 * <pre>
 * implementation 'com.jnzader:apigen-security'
 * </pre>
 *
 * <p>Required properties for JWT mode:
 *
 * <pre>
 * apigen:
 *   security:
 *     enabled: true
 *     jwt:
 *       secret: your-256-bit-secret-key-here
 * </pre>
 *
 * <p>Note: This auto-configuration runs after ApigenCoreAutoConfiguration. The @ConditionalOnBean
 * was removed for Spring Framework 7.0 compatibility (cannot combine @ConditionalOnBean
 * with @ComponentScan). The apigen-security module requires apigen-core as a dependency, so core
 * will always be present.
 *
 * <p>Important: JPA repository and entity scanning for the security module is handled by the
 * generated Application class (via @EnableJpaRepositories and @EntityScan annotations). This avoids
 * BeanDefinitionOverrideException when both configurations would try to register the same
 * repositories.
 */
@AutoConfiguration(after = ApigenCoreAutoConfiguration.class)
@ConditionalOnProperty(
        name = "apigen.security.enabled",
        havingValue = "true",
        matchIfMissing = false)
@EnableConfigurationProperties(SecurityProperties.class)
@ComponentScan(basePackages = "com.jnzader.apigen.security")
@Import({SecurityConfig.class, OAuth2SecurityConfig.class})
public class ApigenSecurityAutoConfiguration {

    /** Marker bean to indicate APiGen Security is auto-configured. */
    @Bean
    @ConditionalOnMissingBean(name = "apigenSecurityMarker")
    public ApigenSecurityMarker apigenSecurityMarker() {
        return new ApigenSecurityMarker();
    }

    /**
     * Marker class for APiGen Security auto-configuration. Used by @ConditionalOnBean to detect if
     * security module is auto-configured.
     */
    @SuppressWarnings("java:S2094")
    // S2094: Clase vacia INTENCIONAL - es un marker bean para deteccion de auto-configuracion
    public static class ApigenSecurityMarker {
        // Marker class to detect auto-configuration
    }
}
