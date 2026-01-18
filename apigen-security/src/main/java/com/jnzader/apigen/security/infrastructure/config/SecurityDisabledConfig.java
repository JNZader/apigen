package com.jnzader.apigen.security.infrastructure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Configuración de seguridad cuando está deshabilitada (apigen.security.enabled=false).
 * <p>
 * Permite acceso a todos los endpoints sin autenticación pero mantiene:
 * - Headers de seguridad (XSS, HSTS, CSP)
 * - Configuración CORS
 * <p>
 * Útil para desarrollo o APIs públicas.
 */
@Configuration
@EnableWebSecurity
@ConditionalOnProperty(name = "apigen.security.enabled", havingValue = "false", matchIfMissing = true)
public class SecurityDisabledConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityDisabledConfig.class);

    @Bean
    @SuppressWarnings({"java:S112", "java:S1130", "java:S4502", "java:S4834"})
    // S112/S1130: Exception requerido por Spring Security API
    // S4502: CSRF deshabilitado es SEGURO - config solo para desarrollo/testing
    // S4834: permitAll() es INTENCIONAL - esta config SOLO se activa cuando
    //        apigen.security.enabled=false (desarrollo/testing, NO produccion)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        log.warn("========================================");
        log.warn("  SEGURIDAD DESHABILITADA");
        log.warn("  Todos los endpoints son públicos.");
        log.warn("  NO USAR EN PRODUCCIÓN.");
        log.warn("  Activar con: apigen.security.enabled=true");
        log.warn("========================================");

        http
                // CORS habilitado
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // CSRF deshabilitado - config solo para dev/test (ver @SuppressWarnings)
                .csrf(AbstractHttpConfigurer::disable)

                // Mantener headers de seguridad
                .headers(headers -> headers
                        .xssProtection(xss -> xss
                                .headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK)
                        )
                        .frameOptions(frame -> frame.deny())
                        .contentTypeOptions(content -> {})
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives(
                                        "default-src 'self'; " +
                                        "script-src 'self' 'unsafe-inline'; " +
                                        "style-src 'self' 'unsafe-inline'; " +
                                        "img-src 'self' data:; " +
                                        "font-src 'self'; " +
                                        "frame-ancestors 'none'"
                                )
                        )
                )

                // Permitir acceso total
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Orígenes permitidos para desarrollo
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:3000",
                "http://localhost:4200",
                "http://localhost:8080"
        ));

        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));

        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "Accept",
                "Origin"
        ));

        configuration.setExposedHeaders(Arrays.asList(
                "Authorization",
                "X-Total-Count",
                "X-Page-Number",
                "X-Page-Size",
                "ETag",
                "Last-Modified"
        ));

        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    /**
     * PasswordEncoder disponible incluso con seguridad deshabilitada.
     * Útil para tests y preparación de datos.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
