package com.jnzader.apigen.core.infrastructure.config.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.List;

/**
 * Propiedades de configuración de la aplicación.
 * <p>
 * Mapea todas las propiedades bajo el prefijo 'app' a clases Java
 * con validación y type-safety.
 */
@ConfigurationProperties(prefix = "app")
@Validated
public record AppProperties(
        @NestedConfigurationProperty
        ApiProperties api,

        @NestedConfigurationProperty
        MetricsProperties metrics,

        @NestedConfigurationProperty
        CorsProperties cors,

        @NestedConfigurationProperty
        SecurityProperties security,

        @NestedConfigurationProperty
        CacheProperties cache,

        @NestedConfigurationProperty
        RateLimitProperties rateLimit
) {
    /**
     * Propiedades de versionado de API.
     */
    @SuppressWarnings("java:S1075") // URIs por defecto son intencionales para configuración
    public record ApiProperties(
            @NotBlank String version,
            @NotBlank String basePath
    ) {
        public ApiProperties {
            if (version == null) version = "v1";
            if (basePath == null) basePath = "/api";
        }
    }

    /**
     * Propiedades de métricas.
     */
    public record MetricsProperties(
            boolean enabled,
            @Positive long slowThresholdMs
    ) {
        public MetricsProperties {
            if (slowThresholdMs <= 0) slowThresholdMs = 500;
        }
    }

    /**
     * Propiedades de CORS.
     */
    public record CorsProperties(
            List<String> allowedOrigins,
            List<String> allowedMethods,
            List<String> allowedHeaders,
            List<String> exposedHeaders,
            boolean allowCredentials,
            @Positive long maxAge
    ) {
        public CorsProperties {
            if (allowedOrigins == null) allowedOrigins = List.of("http://localhost:3000");
            if (allowedMethods == null) allowedMethods = List.of("GET", "POST", "PUT", "DELETE", "OPTIONS");
            if (allowedHeaders == null) allowedHeaders = List.of("*");
            if (exposedHeaders == null) exposedHeaders = List.of();
            if (maxAge <= 0) maxAge = 3600;
        }
    }

    /**
     * Propiedades de seguridad.
     */
    public record SecurityProperties(
            List<String> publicEndpoints
    ) {
        public SecurityProperties {
            if (publicEndpoints == null) publicEndpoints = List.of();
        }
    }

    /**
     * Propiedades de caché.
     */
    public record CacheProperties(
            CacheConfig entities,
            CacheConfig lists,
            CacheConfig counts
    ) {
        public CacheProperties {
            if (entities == null) entities = new CacheConfig(1000, Duration.ofMinutes(10));
            if (lists == null) lists = new CacheConfig(100, Duration.ofMinutes(5));
            if (counts == null) counts = new CacheConfig(50, Duration.ofMinutes(2));
        }

        public record CacheConfig(
                @Positive long maxSize,
                Duration expireAfterWrite
        ) {}
    }

    /**
     * Propiedades de rate limiting.
     */
    public record RateLimitProperties(
            @Positive int maxRequests,
            @Positive int windowSeconds
    ) {
        public RateLimitProperties {
            if (maxRequests <= 0) maxRequests = 100;
            if (windowSeconds <= 0) windowSeconds = 60;
        }
    }
}
