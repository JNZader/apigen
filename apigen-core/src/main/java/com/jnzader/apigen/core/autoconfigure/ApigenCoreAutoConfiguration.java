package com.jnzader.apigen.core.autoconfigure;

import com.jnzader.apigen.core.infrastructure.config.*;
import com.jnzader.apigen.core.infrastructure.config.properties.AppProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;

/**
 * Auto-configuration for APiGen Core module.
 *
 * <p>This auto-configuration automatically enables:
 *
 * <ul>
 *   <li>Component scanning for core services and controllers
 *   <li>All core configurations (JPA, Cache, Tracing, Web, etc.)
 * </ul>
 *
 * <p>Can be disabled with: {@code apigen.core.enabled=false}
 *
 * <p>Usage: Just add the dependency and everything configures automatically:
 *
 * <pre>
 * implementation 'com.jnzader:apigen-core'
 * </pre>
 */
@AutoConfiguration
@ConditionalOnProperty(name = "apigen.core.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(AppProperties.class)
@ComponentScan(
        basePackages = "com.jnzader.apigen.core",
        excludeFilters =
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaConfig.class))
@Import({
    CacheConfig.class,
    WebConfig.class,
    AsyncConfig.class,
    TracingConfig.class,
    OpenApiConfig.class,
    ETagConfig.class,
    ApiVersionConfig.class,
    ResilienceConfig.class,
    ConfigurationValidator.class
})
public class ApigenCoreAutoConfiguration {

    /** Marker bean to indicate APiGen Core is auto-configured. */
    @Bean
    @ConditionalOnMissingBean(name = "apigenCoreMarker")
    public ApigenCoreMarker apigenCoreMarker() {
        return new ApigenCoreMarker();
    }

    /**
     * Marker class for APiGen Core auto-configuration. Used by @ConditionalOnBean to detect if core
     * module is auto-configured.
     */
    @SuppressWarnings("java:S2094")
    // S2094: Clase vacia INTENCIONAL - es un marker bean para deteccion de auto-configuracion
    public static class ApigenCoreMarker {
        // Marker class to detect auto-configuration
    }
}
