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
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
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
 * Security configuration when enabled (apigen.security.enabled=true).
 *
 * <p>Features: - JWT Authentication with access and refresh tokens - Configurable CORS - Security
 * headers (XSS, HSTS, CSP, Frame Options) - Stateless session for REST API - Method-level security
 * enabled
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
    // S112/S1130: Exception required by Spring Security API
    // S4502: CSRF disabled is SAFE here because:
    //   - Stateless REST API using JWT in Authorization header (not cookies)
    //   - CSRF attacks target cookie-based authentication, not applicable to JWT
    //   - Session configured as STATELESS (no server-side state)
    // S4834: permitAll() is SAFE for specific endpoints:
    //   - /api/auth/** : required for login/registration without token
    //   - /actuator/health,info : health checks for load balancers
    //   - /swagger-ui/** : API documentation (can be restricted in prod)
    //   - OPTIONS : CORS preflight (required by browsers)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CORS configuration
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // CSRF disabled - safe for stateless REST API with JWT (see @SuppressWarnings)
                .csrf(AbstractHttpConfigurer::disable)

                // Stateless session
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Security headers (configurable via apigen.security.headers.*)
                .headers(this::configureSecurityHeaders)

                // Endpoint authorization
                .authorizeHttpRequests(this::configureAuthorizationRules)

                // Authentication provider
                .authenticationProvider(authenticationProvider())

                // JWT filter before authentication filter
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Configures authorization rules for HTTP endpoints.
     *
     * <p>Includes conditional configuration for Swagger/OpenAPI based on
     * apigen.security.swagger.protection-mode.
     */
    private void configureAuthorizationRules(
            org.springframework.security.config.annotation.web.configurers
                                            .AuthorizeHttpRequestsConfigurer<
                                    org.springframework.security.config.annotation.web.builders
                                            .HttpSecurity>
                            .AuthorizationManagerRequestMatcherRegistry
                    auth) {

        // Authentication endpoints always public
        auth.requestMatchers("/api/auth/**").permitAll();

        // Actuator health/info public for health checks
        auth.requestMatchers("/actuator/health", "/actuator/info").permitAll();

        // Configure Swagger according to protection mode
        configureSwaggerAuthorization(auth);

        // CORS preflight always allowed
        auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();

        // Full Actuator only for ADMIN
        auth.requestMatchers("/actuator/**").hasRole("ADMIN");

        // Everything else requires authentication
        auth.anyRequest().authenticated();
    }

    /**
     * Configures authorization for Swagger/OpenAPI according to protection mode.
     *
     * <p>Supported modes:
     *
     * <ul>
     *   <li>PUBLIC: Access without authentication (default for development)
     *   <li>AUTHENTICATED: Requires any authenticated user
     *   <li>ADMIN: Requires ADMIN role
     *   <li>ROLES: Requires one of the specified roles
     *   <li>DISABLED: Swagger completely blocked (denyAll)
     * </ul>
     */
    private void configureSwaggerAuthorization(
            org.springframework.security.config.annotation.web.configurers
                                            .AuthorizeHttpRequestsConfigurer<
                                    org.springframework.security.config.annotation.web.builders
                                            .HttpSecurity>
                            .AuthorizationManagerRequestMatcherRegistry
                    auth) {

        SecurityProperties.SwaggerProperties swaggerConfig = securityProperties.getSwagger();
        String[] swaggerPaths = {"/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html"};

        switch (swaggerConfig.getProtectionMode()) {
            case PUBLIC -> auth.requestMatchers(swaggerPaths).permitAll();

            case AUTHENTICATED -> auth.requestMatchers(swaggerPaths).authenticated();

            case ADMIN -> auth.requestMatchers(swaggerPaths).hasRole("ADMIN");

            case ROLES -> {
                String[] roles =
                        swaggerConfig.getAllowedRoles().stream()
                                .map(role -> role.startsWith("ROLE_") ? role.substring(5) : role)
                                .toArray(String[]::new);
                auth.requestMatchers(swaggerPaths).hasAnyRole(roles);
            }

            case DISABLED -> auth.requestMatchers(swaggerPaths).denyAll();
        }
    }

    /**
     * Configures HTTP security headers based on SecurityProperties.
     *
     * <p>Headers configured: - X-XSS-Protection - X-Frame-Options - X-Content-Type-Options -
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
            headers.xssProtection(HeadersConfigurer.XXssConfig::disable);
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
            headers.httpStrictTransportSecurity(HeadersConfigurer.HstsConfig::disable);
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

        // Allowed origins from configuration
        configuration.setAllowedOrigins(corsProps.allowedOrigins());

        // Allowed HTTP methods
        configuration.setAllowedMethods(corsProps.allowedMethods());

        // Allowed headers
        configuration.setAllowedHeaders(corsProps.allowedHeaders());

        // Headers exposed to client (add ETag and Last-Modified if not present)
        List<String> exposedHeaders = new java.util.ArrayList<>(corsProps.exposedHeaders());
        if (!exposedHeaders.contains("ETag")) {
            exposedHeaders.add("ETag");
        }
        if (!exposedHeaders.contains("Last-Modified")) {
            exposedHeaders.add("Last-Modified");
        }
        configuration.setExposedHeaders(exposedHeaders);

        // Allow credentials
        configuration.setAllowCredentials(corsProps.allowCredentials());

        // Preflight cache
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
    @SuppressWarnings({"java:S112", "java:S1130"}) // Exception required by Spring Security API
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt with work factor 12 (more secure than default 10)
        return new BCryptPasswordEncoder(12);
    }
}
