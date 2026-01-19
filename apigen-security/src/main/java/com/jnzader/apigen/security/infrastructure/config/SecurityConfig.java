package com.jnzader.apigen.security.infrastructure.config;

import com.jnzader.apigen.core.infrastructure.config.properties.AppProperties;
import com.jnzader.apigen.security.infrastructure.jwt.JwtAuthenticationFilter;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Configuración de seguridad cuando está habilitada (apigen.security.enabled=true).
 *
 * <p>Características: - JWT Authentication con access y refresh tokens - CORS configurables -
 * Headers de seguridad (XSS, HSTS, CSP, Frame Options) - Sesión stateless para API REST -
 * Method-level security habilitada
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
@ConditionalOnProperty(name = "apigen.security.enabled", havingValue = "true")
@ConditionalOnExpression("'${apigen.security.mode:jwt}'.equalsIgnoreCase('jwt')")
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;
    private final AppProperties appProperties;
    private final SecurityProperties securityProperties;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthFilter,
            UserDetailsService userDetailsService,
            AppProperties appProperties,
            SecurityProperties securityProperties) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.userDetailsService = userDetailsService;
        this.appProperties = appProperties;
        this.securityProperties = securityProperties;
    }

    @Bean
    @SuppressWarnings({"java:S112", "java:S1130", "java:S4502", "java:S4834"})
    // S112/S1130: Exception requerido por Spring Security API
    // S4502: CSRF deshabilitado es SEGURO aquí porque:
    //   - API REST stateless usando JWT en header Authorization (no cookies)
    //   - CSRF ataca autenticación basada en cookies, no aplica a JWT
    //   - Sesión configurada como STATELESS (sin estado en servidor)
    // S4834: permitAll() es SEGURO para endpoints especificos:
    //   - /api/auth/** : necesario para login/registro sin token
    //   - /actuator/health,info : health checks para load balancers
    //   - /swagger-ui/** : documentacion API (puede restringirse en prod)
    //   - OPTIONS : CORS preflight (requerido por navegadores)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Configuración CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // CSRF deshabilitado - seguro para API REST stateless con JWT (ver
                // @SuppressWarnings)
                .csrf(AbstractHttpConfigurer::disable)

                // Sesión stateless
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Headers de seguridad (configurables via apigen.security.headers.*)
                .headers(headers -> configureSecurityHeaders(headers))

                // Autorización de endpoints
                .authorizeHttpRequests(
                        auth ->
                                auth
                                        // Endpoints de autenticación públicos
                                        .requestMatchers("/api/auth/**")
                                        .permitAll()
                                        // Actuator health/info públicos
                                        .requestMatchers("/actuator/health", "/actuator/info")
                                        .permitAll()
                                        // Swagger/OpenAPI públicos
                                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**")
                                        .permitAll()
                                        // CORS preflight
                                        .requestMatchers(HttpMethod.OPTIONS, "/**")
                                        .permitAll()
                                        // Actuator completo solo para ADMIN
                                        .requestMatchers("/actuator/**")
                                        .hasRole("ADMIN")
                                        // El resto requiere autenticación
                                        .anyRequest()
                                        .authenticated())

                // Proveedor de autenticación
                .authenticationProvider(authenticationProvider())

                // Filtro JWT antes del filtro de autenticación
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Configura los headers de seguridad HTTP basados en SecurityProperties.
     *
     * <p>Headers configurados: - X-XSS-Protection - X-Frame-Options - X-Content-Type-Options -
     * Content-Security-Policy - Strict-Transport-Security (HSTS) - Referrer-Policy -
     * Permissions-Policy
     */
    private void configureSecurityHeaders(
            org.springframework.security.config.annotation.web.configurers.HeadersConfigurer<
                            org.springframework.security.config.annotation.web.builders
                                    .HttpSecurity>
                    headers) {

        SecurityProperties.HeadersProperties headersConfig = securityProperties.getHeaders();

        // X-XSS-Protection
        if (headersConfig.isXssProtectionEnabled()) {
            headers.xssProtection(
                    xss ->
                            xss.headerValue(
                                    XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK));
        } else {
            headers.xssProtection(xss -> xss.disable());
        }

        // X-Frame-Options
        if (headersConfig.isFrameOptionsEnabled()) {
            headers.frameOptions(frame -> frame.deny());
        } else {
            headers.frameOptions(frame -> frame.disable());
        }

        // X-Content-Type-Options
        if (headersConfig.isContentTypeOptionsEnabled()) {
            headers.contentTypeOptions(content -> {});
        } else {
            headers.contentTypeOptions(content -> content.disable());
        }

        // Content-Security-Policy
        String csp = headersConfig.getContentSecurityPolicy();
        if (csp != null && !csp.isBlank()) {
            headers.contentSecurityPolicy(policy -> policy.policyDirectives(csp));
        }

        // HSTS
        if (headersConfig.isHstsEnabled()) {
            headers.httpStrictTransportSecurity(
                    hsts ->
                            hsts.includeSubDomains(headersConfig.isHstsIncludeSubDomains())
                                    .maxAgeInSeconds(headersConfig.getHstsMaxAgeSeconds())
                                    .preload(headersConfig.isHstsPreload()));
        } else {
            headers.httpStrictTransportSecurity(hsts -> hsts.disable());
        }

        // Referrer-Policy
        String referrerPolicy = headersConfig.getReferrerPolicy();
        if (referrerPolicy != null && !referrerPolicy.isBlank()) {
            headers.referrerPolicy(
                    referrer ->
                            referrer.policy(
                                    ReferrerPolicyHeaderWriter.ReferrerPolicy.get(referrerPolicy)));
        }

        // Permissions-Policy (custom header)
        String permissionsPolicy = headersConfig.getPermissionsPolicy();
        if (permissionsPolicy != null && !permissionsPolicy.isBlank()) {
            headers.addHeaderWriter(
                    new StaticHeadersWriter("Permissions-Policy", permissionsPolicy));
        }
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        AppProperties.CorsProperties corsProps = appProperties.cors();

        // Orígenes permitidos desde configuración
        configuration.setAllowedOrigins(corsProps.allowedOrigins());

        // Métodos HTTP permitidos
        configuration.setAllowedMethods(corsProps.allowedMethods());

        // Headers permitidos
        configuration.setAllowedHeaders(corsProps.allowedHeaders());

        // Headers expuestos al cliente (añadir ETag y Last-Modified si no están)
        List<String> exposedHeaders = new java.util.ArrayList<>(corsProps.exposedHeaders());
        if (!exposedHeaders.contains("ETag")) {
            exposedHeaders.add("ETag");
        }
        if (!exposedHeaders.contains("Last-Modified")) {
            exposedHeaders.add("Last-Modified");
        }
        configuration.setExposedHeaders(exposedHeaders);

        // Permitir credenciales
        configuration.setAllowCredentials(corsProps.allowCredentials());

        // Cache de preflight
        configuration.setMaxAge(corsProps.maxAge());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    @SuppressWarnings({"java:S112", "java:S1130"}) // Exception requerido por Spring Security API
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt con factor de trabajo 12 (más seguro que el default 10)
        return new BCryptPasswordEncoder(12);
    }
}
