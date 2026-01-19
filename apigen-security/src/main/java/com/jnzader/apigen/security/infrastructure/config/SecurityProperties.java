package com.jnzader.apigen.security.infrastructure.config;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Propiedades de configuración para el módulo de seguridad.
 *
 * <p>Soporta dos modos de autenticación:
 *
 * <ul>
 *   <li><b>jwt</b>: JWT propio con HS256 (default)
 *   <li><b>oauth2</b>: OAuth2 Resource Server para IdPs externos (Auth0, Keycloak, Azure AD)
 * </ul>
 *
 * <p>Uso en application.yml:
 *
 * <pre>
 * apigen:
 *   security:
 *     enabled: true
 *     mode: jwt  # 'jwt' o 'oauth2'
 *     jwt:
 *       secret: ${JWT_SECRET}  # REQUERIDO cuando mode=jwt
 *       expiration-minutes: 15
 *       refresh-expiration-minutes: 1440
 *     oauth2:
 *       issuer-uri: https://your-tenant.auth0.com/
 *       audience: your-api-identifier
 *       roles-claim: permissions  # claim que contiene los roles
 * </pre>
 */
@Component
@ConfigurationProperties(prefix = "apigen.security")
public class SecurityProperties {

    private static final Logger log = LoggerFactory.getLogger(SecurityProperties.class);
    private static final int MIN_SECRET_LENGTH_BYTES = 32; // 256 bits para HS256

    /**
     * Feature flag para habilitar/deshabilitar seguridad. - true: Endpoints protegidos según el
     * modo configurado - false: Todos los endpoints son públicos (permitAll)
     */
    private boolean enabled = false;

    /** Modo de autenticación: 'jwt' (propio) o 'oauth2' (IdP externo). */
    private AuthMode mode = AuthMode.JWT;

    /** Configuración específica de JWT propio. */
    private JwtProperties jwt = new JwtProperties();

    /** Configuración de OAuth2 Resource Server para IdPs externos. */
    private OAuth2Properties oauth2 = new OAuth2Properties();

    /** Modos de autenticación soportados. */
    public enum AuthMode {
        /** JWT propio con secret compartido (HS256) */
        JWT,
        /** OAuth2 Resource Server con IdP externo (RS256) */
        OAUTH2
    }

    /**
     * Valida la configuración al iniciar la aplicación. Falla rápido si la configuración requerida
     * no está presente.
     */
    @PostConstruct
    public void validate() {
        if (enabled) {
            if (mode == AuthMode.JWT) {
                validateJwtSecret();
                log.info("Seguridad JWT (propio) habilitada");
                log.info("  - Token expiration: {} minutos", jwt.getExpirationMinutes());
                log.info(
                        "  - Refresh token expiration: {} minutos",
                        jwt.getRefreshExpirationMinutes());
            } else if (mode == AuthMode.OAUTH2) {
                validateOAuth2Config();
                log.info("Seguridad OAuth2 Resource Server habilitada");
                log.info("  - Issuer URI: {}", oauth2.getIssuerUri());
                log.info("  - Audience: {}", oauth2.getAudience());
                log.info("  - Roles claim: {}", oauth2.getRolesClaim());
            }
        } else {
            log.warn("Seguridad DESHABILITADA - Todos los endpoints son públicos");
        }
    }

    private void validateOAuth2Config() {
        if (oauth2.getIssuerUri() == null || oauth2.getIssuerUri().isBlank()) {
            throw new IllegalStateException(
                    "OAuth2 issuer-uri es requerido cuando apigen.security.mode=oauth2. "
                            + "Configure la propiedad apigen.security.oauth2.issuer-uri");
        }
    }

    private void validateJwtSecret() {
        if (jwt.getSecret() == null || jwt.getSecret().isBlank()) {
            throw new IllegalStateException(
                    "JWT_SECRET es requerido cuando apigen.security.enabled=true. Configure la"
                            + " variable de entorno JWT_SECRET o la propiedad"
                            + " apigen.security.jwt.secret");
        }

        int secretLengthBytes = jwt.getSecret().getBytes(StandardCharsets.UTF_8).length;
        if (secretLengthBytes < MIN_SECRET_LENGTH_BYTES) {
            throw new IllegalStateException(
                    String.format(
                            "JWT_SECRET debe tener al menos %d bytes (256 bits) para HS256. "
                                    + "Longitud actual: %d bytes. "
                                    + "Genere un secret seguro con: openssl rand -base64 64",
                            MIN_SECRET_LENGTH_BYTES, secretLengthBytes));
        }

        // Advertir si parece ser un secret por defecto
        String secretLower = jwt.getSecret().toLowerCase();
        if (secretLower.contains("default")
                || secretLower.contains("change")
                || secretLower.contains("example")
                || secretLower.contains("secret-key")) {
            log.warn("========================================");
            log.warn("  ADVERTENCIA: JWT_SECRET parece ser un");
            log.warn("  valor por defecto. Cámbielo en producción!");
            log.warn("  Genere uno seguro con: openssl rand -base64 64");
            log.warn("========================================");
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public JwtProperties getJwt() {
        return jwt;
    }

    public void setJwt(JwtProperties jwt) {
        this.jwt = jwt;
    }

    public AuthMode getMode() {
        return mode;
    }

    public void setMode(AuthMode mode) {
        this.mode = mode;
    }

    public OAuth2Properties getOauth2() {
        return oauth2;
    }

    public void setOauth2(OAuth2Properties oauth2) {
        this.oauth2 = oauth2;
    }

    /** Determina si está en modo JWT propio. */
    public boolean isJwtMode() {
        return enabled && mode == AuthMode.JWT;
    }

    /** Determina si está en modo OAuth2 Resource Server. */
    public boolean isOAuth2Mode() {
        return enabled && mode == AuthMode.OAUTH2;
    }

    /** Propiedades de configuración JWT. */
    public static class JwtProperties {

        /**
         * Clave secreta para firmar tokens JWT (mínimo 256 bits para HS256). REQUERIDO cuando
         * seguridad está habilitada.
         */
        private String secret;

        /**
         * Tiempo de expiración del token de acceso en minutos. Default: 15 minutos (más seguro que
         * 60).
         */
        private int expirationMinutes = 15;

        /** Tiempo de expiración del refresh token en minutos. Default: 7 días (10080 minutos). */
        private int refreshExpirationMinutes = 10080;

        /** Issuer del token JWT. */
        private String issuer = "apigen";

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public int getExpirationMinutes() {
            return expirationMinutes;
        }

        public void setExpirationMinutes(int expirationMinutes) {
            this.expirationMinutes = expirationMinutes;
        }

        public int getRefreshExpirationMinutes() {
            return refreshExpirationMinutes;
        }

        public void setRefreshExpirationMinutes(int refreshExpirationMinutes) {
            this.refreshExpirationMinutes = refreshExpirationMinutes;
        }

        public String getIssuer() {
            return issuer;
        }

        public void setIssuer(String issuer) {
            this.issuer = issuer;
        }
    }

    /** Propiedades de configuración OAuth2 Resource Server. */
    public static class OAuth2Properties {

        /**
         * URI del issuer del IdP (Auth0, Keycloak, Azure AD, etc.). Usado para validar tokens y
         * obtener JWKS. Ejemplos: - Auth0: https://your-tenant.auth0.com/ - Keycloak:
         * https://keycloak.example.com/realms/your-realm - Azure AD:
         * https://login.microsoftonline.com/{tenant}/v2.0
         */
        private String issuerUri;

        /**
         * Audience esperada en el token (aud claim). Generalmente es el identificador de tu API.
         */
        private String audience;

        /**
         * Nombre del claim que contiene los roles/permisos. Varía según el IdP: - Auth0:
         * "permissions" o custom namespace - Keycloak: "realm_access.roles" - Azure AD: "roles"
         */
        private String rolesClaim = "permissions";

        /**
         * Prefijo para los roles extraídos del claim. Por ejemplo, "ROLE_" convierte "admin" en
         * "ROLE_admin".
         */
        private String rolePrefix = "ROLE_";

        /** Si usar el claim 'sub' como username o buscar otro claim. */
        private String usernameClaim = "sub";

        public String getIssuerUri() {
            return issuerUri;
        }

        public void setIssuerUri(String issuerUri) {
            this.issuerUri = issuerUri;
        }

        public String getAudience() {
            return audience;
        }

        public void setAudience(String audience) {
            this.audience = audience;
        }

        public String getRolesClaim() {
            return rolesClaim;
        }

        public void setRolesClaim(String rolesClaim) {
            this.rolesClaim = rolesClaim;
        }

        public String getRolePrefix() {
            return rolePrefix;
        }

        public void setRolePrefix(String rolePrefix) {
            this.rolePrefix = rolePrefix;
        }

        public String getUsernameClaim() {
            return usernameClaim;
        }

        public void setUsernameClaim(String usernameClaim) {
            this.usernameClaim = usernameClaim;
        }
    }
}
