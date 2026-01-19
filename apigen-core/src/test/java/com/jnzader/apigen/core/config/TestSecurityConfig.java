package com.jnzader.apigen.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Test security configuration that disables security for integration tests.
 *
 * <p>This class intentionally has NO @Configuration, @Component, or similar annotations to prevent
 * it from being auto-scanned. Tests that need this configuration must explicitly import it
 * using @Import(TestSecurityConfig.class).
 *
 * <p>Note: This configuration requires HttpSecurity to be available, which only happens in full
 * Spring Boot tests (@SpringBootTest), not in slice tests (@WebMvcTest).
 */
public class TestSecurityConfig {

    @Bean
    @SuppressWarnings({"java:S112", "java:S1130", "java:S4502", "java:S4834"})
    // S112/S1130: Exception es requerido por la firma del metodo de Spring Security API
    // S4502: CSRF deshabilitado - SEGURO, solo para tests
    // S4834: permitAll() - INTENCIONAL, config de TEST para deshabilitar seguridad
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
