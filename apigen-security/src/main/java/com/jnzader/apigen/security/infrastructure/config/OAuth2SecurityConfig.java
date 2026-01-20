package com.jnzader.apigen.security.infrastructure.config;

import com.jnzader.apigen.core.infrastructure.config.properties.AppProperties;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Configuracion de seguridad OAuth2 Resource Server.
 *
 * <p>Se activa cuando: - apigen.security.enabled=true - apigen.security.mode=oauth2
 *
 * <p>Soporta IdPs externos como: - Auth0 - Keycloak - Azure AD / Entra ID - Okta - AWS Cognito
 *
 * <p>Valida tokens JWT usando JWKS del IdP y extrae roles del claim configurado.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
@ConditionalOnProperty(name = "apigen.security.enabled", havingValue = "true")
@ConditionalOnExpression("'${apigen.security.mode:jwt}'.equalsIgnoreCase('oauth2')")
public class OAuth2SecurityConfig {

    private final SecurityProperties securityProperties;
    private final AppProperties appProperties;

    public OAuth2SecurityConfig(
            SecurityProperties securityProperties, AppProperties appProperties) {
        this.securityProperties = securityProperties;
        this.appProperties = appProperties;
    }

    @Bean
    @SuppressWarnings({"java:S112", "java:S1130", "java:S4502", "java:S4834"})
    // S112/S1130: Exception requerido por Spring Security API
    // S4502: CSRF deshabilitado es SEGURO para API REST stateless con Bearer tokens
    // S4834: permitAll() es SEGURO para endpoints especificos:
    //   - /actuator/health,info : health checks para load balancers/k8s probes
    //   - /swagger-ui/** : documentacion API (puede restringirse en prod)
    //   - OPTIONS : CORS preflight (requerido por navegadores)
    public SecurityFilterChain oauth2SecurityFilterChain(HttpSecurity http) throws Exception {
        http
                // Configuracion CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // CSRF deshabilitado - seguro para API REST stateless con Bearer tokens
                .csrf(AbstractHttpConfigurer::disable)

                // Sesion stateless
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Headers de seguridad (configurables via apigen.security.headers.*)
                .headers(this::configureSecurityHeaders)

                // Autorizacion de endpoints
                .authorizeHttpRequests(
                        auth ->
                                auth
                                        // Actuator health/info publicos
                                        .requestMatchers("/actuator/health", "/actuator/info")
                                        .permitAll()
                                        // Swagger/OpenAPI publicos
                                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**")
                                        .permitAll()
                                        // CORS preflight
                                        .requestMatchers(HttpMethod.OPTIONS, "/**")
                                        .permitAll()
                                        // Actuator completo solo para ADMIN
                                        .requestMatchers("/actuator/**")
                                        .hasRole("ADMIN")
                                        // El resto requiere autenticacion
                                        .anyRequest()
                                        .authenticated())

                // OAuth2 Resource Server con JWT
                .oauth2ResourceServer(
                        oauth2 ->
                                oauth2.jwt(
                                        jwt ->
                                                jwt.decoder(jwtDecoder())
                                                        .jwtAuthenticationConverter(
                                                                jwtAuthenticationConverter())));

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
            HeadersConfigurer<
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

    /** Configura el JwtDecoder para validar tokens del IdP externo. */
    @Bean
    public JwtDecoder jwtDecoder() {
        String issuerUri = securityProperties.getOauth2().getIssuerUri();

        NimbusJwtDecoder decoder = JwtDecoders.fromIssuerLocation(issuerUri);

        // Validar issuer (audience validation se hace custom en el converter si es necesario)
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuerUri));

        return decoder;
    }

    /**
     * Convierte claims JWT a GrantedAuthorities de Spring Security. Extrae roles del claim
     * configurado (por defecto "permissions").
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(
                new CustomJwtGrantedAuthoritiesConverter(securityProperties));
        return converter;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        AppProperties.CorsProperties corsProps = appProperties.cors();

        configuration.setAllowedOrigins(corsProps.allowedOrigins());
        configuration.setAllowedMethods(corsProps.allowedMethods());
        configuration.setAllowedHeaders(corsProps.allowedHeaders());

        List<String> exposedHeaders = new ArrayList<>(corsProps.exposedHeaders());
        if (!exposedHeaders.contains("ETag")) {
            exposedHeaders.add("ETag");
        }
        if (!exposedHeaders.contains("Last-Modified")) {
            exposedHeaders.add("Last-Modified");
        }
        configuration.setExposedHeaders(exposedHeaders);
        configuration.setAllowCredentials(corsProps.allowCredentials());
        configuration.setMaxAge(corsProps.maxAge());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    /**
     * Converter personalizado que extrae roles de diferentes IdPs.
     *
     * <p>Soporta estructuras de claims: - Simple array: "permissions": ["read", "write"] -
     * Keycloak: "realm_access": {"roles": ["admin", "user"]} - Azure AD: "roles": ["Admin.Read",
     * "Admin.Write"]
     */
    @SuppressWarnings({"java:S3776", "java:S2638"})
    // S3776: Complejidad cognitiva es INHERENTE a soportar multiples IdPs
    // (Auth0, Keycloak, Azure AD, Okta, Cognito) cada uno con estructura de claims diferente
    // S2638: El contrato de Converter permite null pero este metodo NUNCA recibe null
    // porque Spring Security valida el JWT antes de llamar al converter
    private static class CustomJwtGrantedAuthoritiesConverter
            implements Converter<Jwt, Collection<GrantedAuthority>> {

        private final SecurityProperties securityProperties;

        CustomJwtGrantedAuthoritiesConverter(SecurityProperties securityProperties) {
            this.securityProperties = securityProperties;
        }

        @Override
        public Collection<GrantedAuthority> convert(Jwt jwt) {
            Collection<GrantedAuthority> authorities = new ArrayList<>();
            String rolesClaim = securityProperties.getOauth2().getRolesClaim();
            String rolePrefix = securityProperties.getOauth2().getRolePrefix();

            // Intentar extraer roles de diferentes estructuras de claims
            List<String> roles = extractRoles(jwt, rolesClaim);

            for (String role : roles) {
                // Normalizar rol: remover espacios, convertir a uppercase para comparaciones
                String normalizedRole = role.trim();
                if (!normalizedRole.isEmpty()) {
                    // Agregar prefijo si no lo tiene
                    if (!normalizedRole.startsWith(rolePrefix)) {
                        normalizedRole = rolePrefix + normalizedRole.toUpperCase();
                    }
                    authorities.add(new SimpleGrantedAuthority(normalizedRole));
                }
            }

            // Si no hay roles especificos, agregar rol default de usuario autenticado
            if (authorities.isEmpty()) {
                authorities.add(new SimpleGrantedAuthority(rolePrefix + "USER"));
            }

            return authorities;
        }

        /** Extrae roles del JWT soportando diferentes estructuras de IdPs. */
        private List<String> extractRoles(Jwt jwt, String rolesClaim) {
            List<String> roles = new ArrayList<>();

            // Caso 1: Claim directo con array (Auth0 permissions, Azure AD roles)
            Object claimValue = jwt.getClaim(rolesClaim);
            if (claimValue instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof String s) {
                        roles.add(s);
                    }
                }
                if (!roles.isEmpty()) {
                    return roles;
                }
            }

            // Caso 2: Keycloak realm_access.roles
            if (rolesClaim.contains(".")) {
                String[] parts = rolesClaim.split("\\.", 2);
                Object parentClaim = jwt.getClaim(parts[0]);
                if (parentClaim instanceof Map<?, ?> map) {
                    Object nestedRoles = map.get(parts[1]);
                    if (nestedRoles instanceof List<?> list) {
                        for (Object item : list) {
                            if (item instanceof String s) {
                                roles.add(s);
                            }
                        }
                    }
                }
            }

            // Caso 3: Keycloak resource_access.{client}.roles
            Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
            if (resourceAccess != null) {
                for (Object value : resourceAccess.values()) {
                    if (value instanceof Map<?, ?> clientAccess) {
                        Object clientRoles = clientAccess.get("roles");
                        if (clientRoles instanceof List<?> list) {
                            for (Object item : list) {
                                if (item instanceof String s && !roles.contains(s)) {
                                    roles.add(s);
                                }
                            }
                        }
                    }
                }
            }

            // Caso 4: Cognito cognito:groups
            List<String> cognitoGroups = jwt.getClaim("cognito:groups");
            if (cognitoGroups != null) {
                roles.addAll(cognitoGroups);
            }

            return roles;
        }
    }
}
