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

    /** Configuración de security headers HTTP. */
    private HeadersProperties headers = new HeadersProperties();

    /** Configuración de rate limiting. */
    private RateLimitProperties rateLimit = new RateLimitProperties();

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

    public HeadersProperties getHeaders() {
        return headers;
    }

    public void setHeaders(HeadersProperties headers) {
        this.headers = headers;
    }

    public RateLimitProperties getRateLimit() {
        return rateLimit;
    }

    public void setRateLimit(RateLimitProperties rateLimit) {
        this.rateLimit = rateLimit;
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

    /**
     * Propiedades de configuración para Security Headers HTTP.
     *
     * <p>Configura headers de seguridad como CSP, HSTS, Referrer-Policy, etc.
     */
    public static class HeadersProperties {

        /**
         * Content Security Policy. Define qué recursos puede cargar el navegador. Default:
         * restrictivo para APIs.
         */
        private String contentSecurityPolicy =
                "default-src 'self'; script-src 'self'; style-src 'self'; "
                        + "img-src 'self' data:; font-src 'self'; frame-ancestors 'none'; "
                        + "form-action 'self'";

        /**
         * Referrer-Policy header. Controla qué información de referrer se envía. Opciones:
         * no-referrer, no-referrer-when-downgrade, origin, origin-when-cross-origin, same-origin,
         * strict-origin, strict-origin-when-cross-origin, unsafe-url Default: strict-origin (solo
         * origen en HTTPS)
         */
        private String referrerPolicy = "strict-origin-when-cross-origin";

        /**
         * Permissions-Policy header (antes Feature-Policy). Controla qué features del navegador
         * están permitidas. Default: deshabilita geolocalización, cámara, micrófono, etc.
         */
        private String permissionsPolicy =
                "geolocation=(), camera=(), microphone=(), payment=(), usb=(), magnetometer=(), "
                        + "gyroscope=(), accelerometer=()";

        /** Habilitar HSTS (HTTP Strict Transport Security). Default: true. */
        private boolean hstsEnabled = true;

        /** HSTS max-age en segundos. Default: 1 año (31536000). */
        private long hstsMaxAgeSeconds = 31536000;

        /** HSTS incluir subdominios. Default: true. */
        private boolean hstsIncludeSubDomains = true;

        /** HSTS preload. Permite inclusión en listas de preload de navegadores. Default: false. */
        private boolean hstsPreload = false;

        /** Habilitar X-Content-Type-Options: nosniff. Default: true. */
        private boolean contentTypeOptionsEnabled = true;

        /** Habilitar X-Frame-Options: DENY. Default: true. */
        private boolean frameOptionsEnabled = true;

        /** Habilitar X-XSS-Protection. Default: true. */
        private boolean xssProtectionEnabled = true;

        public String getContentSecurityPolicy() {
            return contentSecurityPolicy;
        }

        public void setContentSecurityPolicy(String contentSecurityPolicy) {
            this.contentSecurityPolicy = contentSecurityPolicy;
        }

        public String getReferrerPolicy() {
            return referrerPolicy;
        }

        public void setReferrerPolicy(String referrerPolicy) {
            this.referrerPolicy = referrerPolicy;
        }

        public String getPermissionsPolicy() {
            return permissionsPolicy;
        }

        public void setPermissionsPolicy(String permissionsPolicy) {
            this.permissionsPolicy = permissionsPolicy;
        }

        public boolean isHstsEnabled() {
            return hstsEnabled;
        }

        public void setHstsEnabled(boolean hstsEnabled) {
            this.hstsEnabled = hstsEnabled;
        }

        public long getHstsMaxAgeSeconds() {
            return hstsMaxAgeSeconds;
        }

        public void setHstsMaxAgeSeconds(long hstsMaxAgeSeconds) {
            this.hstsMaxAgeSeconds = hstsMaxAgeSeconds;
        }

        public boolean isHstsIncludeSubDomains() {
            return hstsIncludeSubDomains;
        }

        public void setHstsIncludeSubDomains(boolean hstsIncludeSubDomains) {
            this.hstsIncludeSubDomains = hstsIncludeSubDomains;
        }

        public boolean isHstsPreload() {
            return hstsPreload;
        }

        public void setHstsPreload(boolean hstsPreload) {
            this.hstsPreload = hstsPreload;
        }

        public boolean isContentTypeOptionsEnabled() {
            return contentTypeOptionsEnabled;
        }

        public void setContentTypeOptionsEnabled(boolean contentTypeOptionsEnabled) {
            this.contentTypeOptionsEnabled = contentTypeOptionsEnabled;
        }

        public boolean isFrameOptionsEnabled() {
            return frameOptionsEnabled;
        }

        public void setFrameOptionsEnabled(boolean frameOptionsEnabled) {
            this.frameOptionsEnabled = frameOptionsEnabled;
        }

        public boolean isXssProtectionEnabled() {
            return xssProtectionEnabled;
        }

        public void setXssProtectionEnabled(boolean xssProtectionEnabled) {
            this.xssProtectionEnabled = xssProtectionEnabled;
        }
    }

    /**
     * Propiedades de configuración para Rate Limiting.
     *
     * <p>Soporta dos modos de almacenamiento: - in-memory: usa Bucket4j con almacenamiento local
     * (single instance) - redis: usa Bucket4j con Redis/Lettuce (distributed, multi-instance)
     */
    public static class RateLimitProperties {

        /** Habilitar rate limiting general para API. Default: true. */
        private boolean enabled = true;

        /**
         * Modo de almacenamiento: 'in-memory' o 'redis'. - in-memory: para desarrollo o single
         * instance - redis: para producción multi-instancia
         */
        private StorageMode storageMode = StorageMode.IN_MEMORY;

        /** Requests por segundo permitidos para la API general. Default: 100. */
        private int requestsPerSecond = 100;

        /** Requests por minuto para endpoints de autenticación (más restrictivo). Default: 10. */
        private int authRequestsPerMinute = 10;

        /** Capacidad máxima del bucket (burst). Default: 150. */
        private int burstCapacity = 150;

        /** Capacidad burst para auth endpoints. Default: 15. */
        private int authBurstCapacity = 15;

        /** Tiempo de bloqueo en segundos después de exceder el límite. Default: 60. */
        private int blockDurationSeconds = 60;

        /** Prefijo para las claves en Redis. Default: "rate-limit:". */
        private String redisKeyPrefix = "rate-limit:";

        /** TTL en segundos para las claves de Redis. Default: 3600 (1 hora). */
        private int redisTtlSeconds = 3600;

        public enum StorageMode {
            /** Almacenamiento en memoria local (single instance) */
            IN_MEMORY,
            /** Almacenamiento distribuido en Redis (multi-instance) */
            REDIS
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public StorageMode getStorageMode() {
            return storageMode;
        }

        public void setStorageMode(StorageMode storageMode) {
            this.storageMode = storageMode;
        }

        public int getRequestsPerSecond() {
            return requestsPerSecond;
        }

        public void setRequestsPerSecond(int requestsPerSecond) {
            this.requestsPerSecond = requestsPerSecond;
        }

        public int getAuthRequestsPerMinute() {
            return authRequestsPerMinute;
        }

        public void setAuthRequestsPerMinute(int authRequestsPerMinute) {
            this.authRequestsPerMinute = authRequestsPerMinute;
        }

        public int getBurstCapacity() {
            return burstCapacity;
        }

        public void setBurstCapacity(int burstCapacity) {
            this.burstCapacity = burstCapacity;
        }

        public int getAuthBurstCapacity() {
            return authBurstCapacity;
        }

        public void setAuthBurstCapacity(int authBurstCapacity) {
            this.authBurstCapacity = authBurstCapacity;
        }

        public int getBlockDurationSeconds() {
            return blockDurationSeconds;
        }

        public void setBlockDurationSeconds(int blockDurationSeconds) {
            this.blockDurationSeconds = blockDurationSeconds;
        }

        public String getRedisKeyPrefix() {
            return redisKeyPrefix;
        }

        public void setRedisKeyPrefix(String redisKeyPrefix) {
            this.redisKeyPrefix = redisKeyPrefix;
        }

        public int getRedisTtlSeconds() {
            return redisTtlSeconds;
        }

        public void setRedisTtlSeconds(int redisTtlSeconds) {
            this.redisTtlSeconds = redisTtlSeconds;
        }

        /** Determina si está configurado para usar Redis. */
        public boolean isRedisMode() {
            return storageMode == StorageMode.REDIS;
        }
    }
}
