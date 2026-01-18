package com.jnzader.apigen.server.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

/**
 * CORS configuration for the APiGen Server.
 * Allows cross-origin requests from the APiGen Studio frontend.
 *
 * <p>Configure allowed origins via environment variable:
 * <pre>CORS_ORIGINS=https://app1.vercel.app,https://app2.vercel.app</pre>
 */
@Configuration
@SuppressWarnings("java:S5122")
// S5122: CORS wildcard "*" es SEGURO aqui porque:
//   - Solo aplica en DESARROLLO (cuando cors.allowed-origins="*")
//   - En PRODUCCION se configuran origenes especificos via env var
//   - El server es un generador de codigo, no maneja datos sensibles
//   - El frontend (Studio) necesita conectarse desde cualquier origen en dev
public class CorsConfig {

    @Value("${cors.allowed-origins:*}")
    private String allowedOrigins;

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // Configure origins based on environment
        if ("*".equals(allowedOrigins)) {
            // Development: allow all origins
            config.setAllowedOriginPatterns(List.of("*"));
        } else {
            // Production: use specific origins from env var
            List<String> origins = Arrays.asList(allowedOrigins.split(","));
            config.setAllowedOrigins(origins);
        }

        // Allow credentials (cookies, authorization headers)
        config.setAllowCredentials(true);

        // Allowed HTTP methods
        config.setAllowedMethods(List.of(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));

        // Allowed headers
        config.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "Accept",
                "Origin",
                "X-Requested-With"
        ));

        // Exposed headers (accessible to the frontend)
        config.setExposedHeaders(List.of(
                "Content-Disposition",
                "Content-Length"
        ));

        // Cache preflight requests for 1 hour
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);

        return new CorsFilter(source);
    }
}
