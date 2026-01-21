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

    /** Configuración de PKCE OAuth2. */
    private PkceProperties pkce = new PkceProperties();

    /** Configuración de bloqueo de cuentas por intentos fallidos. */
    private AccountLockoutProperties accountLockout = new AccountLockoutProperties();

    /** Configuración de protección de Swagger/OpenAPI UI. */
    private SwaggerProperties swagger = new SwaggerProperties();

    /** Configuración de proxies de confianza para X-Forwarded-For. */
    private TrustedProxiesProperties trustedProxies = new TrustedProxiesProperties();

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

    public PkceProperties getPkce() {
        return pkce;
    }

    public void setPkce(PkceProperties pkce) {
        this.pkce = pkce;
    }

    public AccountLockoutProperties getAccountLockout() {
        return accountLockout;
    }

    public void setAccountLockout(AccountLockoutProperties accountLockout) {
        this.accountLockout = accountLockout;
    }

    public SwaggerProperties getSwagger() {
        return swagger;
    }

    public void setSwagger(SwaggerProperties swagger) {
        this.swagger = swagger;
    }

    public TrustedProxiesProperties getTrustedProxies() {
        return trustedProxies;
    }

    public void setTrustedProxies(TrustedProxiesProperties trustedProxies) {
        this.trustedProxies = trustedProxies;
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

        /** Configuración de rotación de claves. */
        private KeyRotationProperties keyRotation = new KeyRotationProperties();

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

        public KeyRotationProperties getKeyRotation() {
            return keyRotation;
        }

        public void setKeyRotation(KeyRotationProperties keyRotation) {
            this.keyRotation = keyRotation;
        }
    }

    /**
     * Propiedades para rotación de claves JWT.
     *
     * <p>Permite mantener múltiples claves activas durante la transición para evitar invalidar
     * tokens existentes. Uso:
     *
     * <pre>
     * apigen:
     *   security:
     *     jwt:
     *       secret: ${JWT_SECRET_CURRENT}
     *       key-rotation:
     *         enabled: true
     *         current-key-id: "key-2025-01"
     *         previous-secrets:
     *           - id: "key-2024-10"
     *             secret: ${JWT_SECRET_PREVIOUS}
     * </pre>
     */
    public static class KeyRotationProperties {

        /**
         * Habilitar rotación de claves con soporte para múltiples keys. Cuando está habilitado, los
         * tokens incluyen header 'kid' y se pueden verificar con claves anteriores.
         */
        private boolean enabled = false;

        /**
         * ID de la clave actual (kid header). Se recomienda usar formato: "key-YYYY-MM" Ejemplo:
         * "key-2025-01"
         */
        private String currentKeyId = "key-1";

        /**
         * Lista de claves anteriores que aún son válidas para verificación. Mantener hasta que
         * todos los tokens firmados con ellas hayan expirado.
         */
        private java.util.List<PreviousKey> previousSecrets = new java.util.ArrayList<>();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getCurrentKeyId() {
            return currentKeyId;
        }

        public void setCurrentKeyId(String currentKeyId) {
            this.currentKeyId = currentKeyId;
        }

        public java.util.List<PreviousKey> getPreviousSecrets() {
            return previousSecrets;
        }

        public void setPreviousSecrets(java.util.List<PreviousKey> previousSecrets) {
            this.previousSecrets = previousSecrets;
        }
    }

    /** Representa una clave anterior para rotación. */
    public static class PreviousKey {

        /** ID único de la clave (debe coincidir con el 'kid' en tokens firmados con esta clave). */
        private String id;

        /** El secret de la clave anterior (mínimo 256 bits). */
        private String secret;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
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
     *
     * <p>Soporta rate limiting por tiers de usuario: - anonymous: usuarios no autenticados - free:
     * usuarios tier gratuito - basic: usuarios tier básico - pro: usuarios tier profesional
     */
    public static class RateLimitProperties {

        /** Habilitar rate limiting general para API. Default: true. */
        private boolean enabled = true;

        /** Habilitar rate limiting basado en tiers de usuario. Default: false. */
        private boolean tiersEnabled = false;

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

        /** Configuración de tiers para rate limiting basado en usuario. */
        private TiersConfig tiers = new TiersConfig();

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

        public boolean isTiersEnabled() {
            return tiersEnabled;
        }

        public void setTiersEnabled(boolean tiersEnabled) {
            this.tiersEnabled = tiersEnabled;
        }

        public TiersConfig getTiers() {
            return tiers;
        }

        public void setTiers(TiersConfig tiers) {
            this.tiers = tiers;
        }

        /**
         * Configuración de tiers para rate limiting basado en usuario.
         *
         * <p>Cada tier tiene su propio límite de requests por segundo y capacidad de burst.
         */
        public static class TiersConfig {

            /** Configuración para usuarios no autenticados. */
            private TierConfig anonymous = new TierConfig(10, 20);

            /** Configuración para usuarios tier gratuito. */
            private TierConfig free = new TierConfig(50, 100);

            /** Configuración para usuarios tier básico. */
            private TierConfig basic = new TierConfig(200, 400);

            /** Configuración para usuarios tier profesional. */
            private TierConfig pro = new TierConfig(1000, 2000);

            public TierConfig getAnonymous() {
                return anonymous;
            }

            public void setAnonymous(TierConfig anonymous) {
                this.anonymous = anonymous;
            }

            public TierConfig getFree() {
                return free;
            }

            public void setFree(TierConfig free) {
                this.free = free;
            }

            public TierConfig getBasic() {
                return basic;
            }

            public void setBasic(TierConfig basic) {
                this.basic = basic;
            }

            public TierConfig getPro() {
                return pro;
            }

            public void setPro(TierConfig pro) {
                this.pro = pro;
            }

            /**
             * Gets the tier configuration for a specific tier name.
             *
             * @param tierName the tier name (anonymous, free, basic, pro)
             * @return the tier configuration
             */
            public TierConfig getForTier(String tierName) {
                return switch (tierName.toLowerCase()) {
                    case "free" -> free;
                    case "basic" -> basic;
                    case "pro" -> pro;
                    default -> anonymous;
                };
            }
        }

        /**
         * Configuration for a specific rate limit tier.
         *
         * <p>Defines the requests per second and burst capacity for a tier.
         */
        public static class TierConfig {

            /** Requests per second allowed for this tier. */
            private int requestsPerSecond;

            /** Maximum burst capacity for this tier. */
            private int burstCapacity;

            public TierConfig() {}

            public TierConfig(int requestsPerSecond, int burstCapacity) {
                this.requestsPerSecond = requestsPerSecond;
                this.burstCapacity = burstCapacity;
            }

            public int getRequestsPerSecond() {
                return requestsPerSecond;
            }

            public void setRequestsPerSecond(int requestsPerSecond) {
                this.requestsPerSecond = requestsPerSecond;
            }

            public int getBurstCapacity() {
                return burstCapacity;
            }

            public void setBurstCapacity(int burstCapacity) {
                this.burstCapacity = burstCapacity;
            }
        }
    }

    /**
     * Propiedades de configuración para PKCE OAuth2.
     *
     * <p>PKCE (Proof Key for Code Exchange) es una extensión de OAuth 2.0 que mejora la seguridad
     * para clientes públicos como SPAs y aplicaciones móviles.
     *
     * <p>Uso en application.yml:
     *
     * <pre>
     * apigen:
     *   security:
     *     pkce:
     *       enabled: true
     *       code-expiration-minutes: 10
     *       require-s256: true
     * </pre>
     */
    public static class PkceProperties {

        /** Habilitar PKCE OAuth2 flow. Default: true. */
        private boolean enabled = true;

        /**
         * Tiempo de expiración del authorization code en minutos. Default: 10 minutos (según RFC
         * 6749, los códigos deben tener vida corta).
         */
        private int codeExpirationMinutes = 10;

        /**
         * Requerir método S256 (SHA-256). Si es false, también se permite 'plain'. Default: true.
         */
        private boolean requireS256 = true;

        /**
         * Longitud mínima del code_verifier. Default: 43 (mínimo RFC 7636). El máximo siempre es
         * 128.
         */
        private int minCodeVerifierLength = 43;

        /**
         * Permitir el endpoint /oauth2/pkce/generate para desarrollo. Debe estar deshabilitado en
         * producción. Default: false.
         */
        private boolean allowPkceHelperEndpoint = false;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getCodeExpirationMinutes() {
            return codeExpirationMinutes;
        }

        public void setCodeExpirationMinutes(int codeExpirationMinutes) {
            this.codeExpirationMinutes = codeExpirationMinutes;
        }

        public boolean isRequireS256() {
            return requireS256;
        }

        public void setRequireS256(boolean requireS256) {
            this.requireS256 = requireS256;
        }

        public int getMinCodeVerifierLength() {
            return minCodeVerifierLength;
        }

        public void setMinCodeVerifierLength(int minCodeVerifierLength) {
            this.minCodeVerifierLength = minCodeVerifierLength;
        }

        public boolean isAllowPkceHelperEndpoint() {
            return allowPkceHelperEndpoint;
        }

        public void setAllowPkceHelperEndpoint(boolean allowPkceHelperEndpoint) {
            this.allowPkceHelperEndpoint = allowPkceHelperEndpoint;
        }
    }

    /**
     * Propiedades de configuración para bloqueo de cuentas por intentos fallidos.
     *
     * <p>Protege contra ataques de fuerza bruta bloqueando temporalmente las cuentas después de
     * múltiples intentos de login fallidos.
     *
     * <p>Uso en application.yml:
     *
     * <pre>
     * apigen:
     *   security:
     *     account-lockout:
     *       enabled: true
     *       max-failed-attempts: 5
     *       lockout-duration-minutes: 15
     *       reset-after-minutes: 30
     * </pre>
     */
    public static class AccountLockoutProperties {

        /** Habilitar bloqueo de cuentas por intentos fallidos. Default: true. */
        private boolean enabled = true;

        /**
         * Número máximo de intentos fallidos antes de bloquear la cuenta. Default: 5 intentos.
         * Recomendación OWASP: 3-5 intentos.
         */
        private int maxFailedAttempts = 5;

        /**
         * Duración del bloqueo en minutos después de exceder los intentos. Default: 15 minutos.
         * Recomendación: incrementar exponencialmente en bloqueos repetidos.
         */
        private int lockoutDurationMinutes = 15;

        /**
         * Tiempo en minutos después del cual se resetean los intentos fallidos si no hay más
         * intentos. Default: 30 minutos. Previene acumulación de intentos en ataques lentos.
         */
        private int resetAfterMinutes = 30;

        /**
         * Notificar al usuario por email cuando su cuenta es bloqueada. Default: false. Requiere
         * configuración de email.
         */
        private boolean notifyOnLockout = false;

        /**
         * Habilitar desbloqueo permanente después de múltiples bloqueos. Requiere intervención de
         * admin. Default: false.
         */
        private boolean permanentLockoutEnabled = false;

        /**
         * Número de bloqueos temporales antes de bloqueo permanente. Solo aplica si
         * permanentLockoutEnabled=true. Default: 3.
         */
        private int lockoutsBeforePermanent = 3;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getMaxFailedAttempts() {
            return maxFailedAttempts;
        }

        public void setMaxFailedAttempts(int maxFailedAttempts) {
            this.maxFailedAttempts = maxFailedAttempts;
        }

        public int getLockoutDurationMinutes() {
            return lockoutDurationMinutes;
        }

        public void setLockoutDurationMinutes(int lockoutDurationMinutes) {
            this.lockoutDurationMinutes = lockoutDurationMinutes;
        }

        public int getResetAfterMinutes() {
            return resetAfterMinutes;
        }

        public void setResetAfterMinutes(int resetAfterMinutes) {
            this.resetAfterMinutes = resetAfterMinutes;
        }

        public boolean isNotifyOnLockout() {
            return notifyOnLockout;
        }

        public void setNotifyOnLockout(boolean notifyOnLockout) {
            this.notifyOnLockout = notifyOnLockout;
        }

        public boolean isPermanentLockoutEnabled() {
            return permanentLockoutEnabled;
        }

        public void setPermanentLockoutEnabled(boolean permanentLockoutEnabled) {
            this.permanentLockoutEnabled = permanentLockoutEnabled;
        }

        public int getLockoutsBeforePermanent() {
            return lockoutsBeforePermanent;
        }

        public void setLockoutsBeforePermanent(int lockoutsBeforePermanent) {
            this.lockoutsBeforePermanent = lockoutsBeforePermanent;
        }
    }

    /**
     * Propiedades de configuración para protección de Swagger/OpenAPI UI.
     *
     * <p>En producción, es recomendable proteger o deshabilitar la documentación de la API para
     * evitar exponer información sensible sobre la estructura de la API.
     *
     * <p>Uso en application.yml:
     *
     * <pre>
     * apigen:
     *   security:
     *     swagger:
     *       protection-mode: admin  # 'public', 'authenticated', 'admin', 'disabled'
     *       allowed-roles:          # Solo si protection-mode=roles
     *         - ADMIN
     *         - API_DOCS
     * </pre>
     */
    public static class SwaggerProperties {

        /**
         * Modo de protección para Swagger UI y OpenAPI docs. Default: public (desarrollo). En
         * producción se recomienda 'admin' o 'disabled'.
         */
        private SwaggerProtectionMode protectionMode = SwaggerProtectionMode.PUBLIC;

        /** Roles permitidos para acceder a Swagger cuando protection-mode=roles. Default: ADMIN. */
        private java.util.List<String> allowedRoles = java.util.List.of("ADMIN");

        /**
         * Deshabilitar en perfiles de producción automáticamente. Si está habilitado, cambia a
         * 'admin' cuando el perfil activo es 'prod' o 'production'. Default: true.
         */
        private boolean autoProtectInProduction = true;

        /** Modos de protección para Swagger/OpenAPI. */
        public enum SwaggerProtectionMode {
            /** Acceso público sin autenticación. Recomendado solo para desarrollo. */
            PUBLIC,
            /** Requiere autenticación (cualquier usuario autenticado). */
            AUTHENTICATED,
            /** Requiere rol ADMIN. */
            ADMIN,
            /** Requiere uno de los roles especificados en allowedRoles. */
            ROLES,
            /** Swagger completamente deshabilitado. Los endpoints devuelven 404. */
            DISABLED
        }

        public SwaggerProtectionMode getProtectionMode() {
            return protectionMode;
        }

        public void setProtectionMode(SwaggerProtectionMode protectionMode) {
            this.protectionMode = protectionMode;
        }

        public java.util.List<String> getAllowedRoles() {
            return allowedRoles;
        }

        public void setAllowedRoles(java.util.List<String> allowedRoles) {
            this.allowedRoles = allowedRoles;
        }

        public boolean isAutoProtectInProduction() {
            return autoProtectInProduction;
        }

        public void setAutoProtectInProduction(boolean autoProtectInProduction) {
            this.autoProtectInProduction = autoProtectInProduction;
        }

        /** Helper: determina si Swagger está completamente deshabilitado. */
        public boolean isDisabled() {
            return protectionMode == SwaggerProtectionMode.DISABLED;
        }

        /** Helper: determina si Swagger es público (sin autenticación). */
        public boolean isPublic() {
            return protectionMode == SwaggerProtectionMode.PUBLIC;
        }

        /** Helper: determina si requiere autenticación. */
        public boolean requiresAuthentication() {
            return protectionMode == SwaggerProtectionMode.AUTHENTICATED
                    || protectionMode == SwaggerProtectionMode.ADMIN
                    || protectionMode == SwaggerProtectionMode.ROLES;
        }

        /** Helper: determina si requiere rol ADMIN específicamente. */
        public boolean requiresAdmin() {
            return protectionMode == SwaggerProtectionMode.ADMIN;
        }
    }

    /**
     * Propiedades de configuración para validación de proxies de confianza.
     *
     * <p>Controla cómo se determina la IP real del cliente cuando la aplicación está detrás de un
     * proxy o load balancer que establece headers como X-Forwarded-For.
     *
     * <p>IMPORTANTE: Sin una configuración adecuada, un atacante podría falsificar su IP enviando
     * headers X-Forwarded-For directamente. Esto afecta:
     *
     * <ul>
     *   <li>Rate limiting (bypass de límites por IP)
     *   <li>Account lockout (bloqueo de IPs incorrectas)
     *   <li>Logs de auditoría (registro de IPs falsas)
     * </ul>
     *
     * <p>Uso en application.yml:
     *
     * <pre>
     * apigen:
     *   security:
     *     trusted-proxies:
     *       mode: configured  # 'trust-all', 'trust-direct', 'configured'
     *       addresses:
     *         - 10.0.0.0/8    # Red interna
     *         - 172.16.0.0/12
     *         - 192.168.0.0/16
     *         - 127.0.0.1     # Localhost
     * </pre>
     */
    public static class TrustedProxiesProperties {

        /**
         * Modo de confianza para proxies. - TRUST_ALL: Confía en cualquier X-Forwarded-For
         * (INSEGURO, solo para desarrollo) - TRUST_DIRECT: Solo usa remoteAddr, ignora headers de
         * proxy (más seguro si no hay proxy) - CONFIGURED: Solo confía en proxies de la lista
         * configurada (RECOMENDADO para producción)
         *
         * <p>Default: TRUST_ALL para compatibilidad con versiones anteriores. En producción se
         * recomienda CONFIGURED.
         */
        private TrustMode mode = TrustMode.TRUST_ALL;

        /**
         * Lista de direcciones IP o rangos CIDR de proxies de confianza. Solo aplica cuando
         * mode=CONFIGURED.
         *
         * <p>Ejemplos: - 127.0.0.1 (localhost) - 10.0.0.0/8 (rango CIDR clase A privada) - ::1
         * (IPv6 localhost)
         *
         * <p>Valores comunes para cloud providers: - AWS ALB: IPs del VPC - GCP GLB:
         * 130.211.0.0/22, 35.191.0.0/16 - Cloudflare: Ver https://www.cloudflare.com/ips/
         */
        private java.util.List<String> addresses =
                java.util.List.of(
                        "127.0.0.1", // IPv4 localhost
                        "::1", // IPv6 localhost
                        "10.0.0.0/8", // Private Class A
                        "172.16.0.0/12", // Private Class B
                        "192.168.0.0/16" // Private Class C
                        );

        /**
         * Nombre del header a usar para obtener la IP del cliente. Default: X-Forwarded-For
         * (estándar de facto).
         */
        private String forwardedForHeader = "X-Forwarded-For";

        /**
         * Si usar el primer o último IP en la cadena X-Forwarded-For. - true: Toma el primer IP
         * (cliente original, asumiendo proxies confiables) - false: Toma el último IP añadido antes
         * del proxy de confianza
         *
         * <p>Default: true (comportamiento estándar).
         */
        private boolean useFirstInChain = true;

        /** Modos de confianza para proxies. */
        public enum TrustMode {
            /**
             * Confía en cualquier header X-Forwarded-For. INSEGURO para producción, solo usar en
             * desarrollo.
             */
            TRUST_ALL,
            /**
             * Ignora todos los headers de proxy, solo usa la IP directa (remoteAddr). Usar si la
             * aplicación no está detrás de un proxy.
             */
            TRUST_DIRECT,
            /**
             * Solo confía en proxies con IPs en la lista configurada. RECOMENDADO para producción.
             */
            CONFIGURED
        }

        public TrustMode getMode() {
            return mode;
        }

        public void setMode(TrustMode mode) {
            this.mode = mode;
        }

        public java.util.List<String> getAddresses() {
            return addresses;
        }

        public void setAddresses(java.util.List<String> addresses) {
            this.addresses = addresses;
        }

        public String getForwardedForHeader() {
            return forwardedForHeader;
        }

        public void setForwardedForHeader(String forwardedForHeader) {
            this.forwardedForHeader = forwardedForHeader;
        }

        public boolean isUseFirstInChain() {
            return useFirstInChain;
        }

        public void setUseFirstInChain(boolean useFirstInChain) {
            this.useFirstInChain = useFirstInChain;
        }

        /** Helper: determina si se deben validar los proxies. */
        public boolean shouldValidateProxies() {
            return mode == TrustMode.CONFIGURED;
        }

        /** Helper: determina si se ignoran los headers de proxy. */
        public boolean shouldIgnoreProxyHeaders() {
            return mode == TrustMode.TRUST_DIRECT;
        }
    }
}
