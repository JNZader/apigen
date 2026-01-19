package com.jnzader.apigen.core.infrastructure.config;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Validador de configuración que se ejecuta al iniciar la aplicación.
 *
 * <p>Verifica que todas las propiedades críticas estén configuradas correctamente antes de que la
 * aplicación comience a procesar solicitudes.
 *
 * <p>Si alguna validación falla, la aplicación fallará en el inicio con un mensaje descriptivo del
 * problema.
 *
 * <p>Propiedades validadas:
 *
 * <ul>
 *   <li>app.api.version - Versión de la API
 *   <li>app.api.base-path - Ruta base de la API
 *   <li>spring.datasource.url - URL de conexión a base de datos
 *   <li>Configuración de caché
 *   <li>Configuración de CORS
 * </ul>
 */
@Component
public class ConfigurationValidator implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ConfigurationValidator.class);

    private final Environment environment;

    @Value("${app.api.version:}")
    private String apiVersion;

    @Value("${app.api.base-path:}")
    private String apiBasePath;

    @Value("${spring.datasource.url:}")
    private String datasourceUrl;

    @Value("${app.cache.entities.max-size:0}")
    private int cacheEntitiesMaxSize;

    @Value("${app.cors.allowed-origins:}")
    private String corsAllowedOrigins;

    @Value("${app.rate-limit.max-requests:0}")
    private int rateLimitMaxRequests;

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    public ConfigurationValidator(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        // Skip validation in test profile (TestContainers configures datasource dynamically)
        if (isTestProfile()) {
            log.info("Skipping configuration validation for test profile");
            return;
        }

        log.info("Validating application configuration for profile: {}", activeProfile);

        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        // Validaciones críticas (fallan el inicio)
        validateCriticalProperties(errors);

        // Validaciones de advertencia (solo log)
        validateRecommendedProperties(warnings);

        // Validaciones específicas por perfil
        validateProfileSpecificProperties(errors, warnings);

        // Reportar advertencias
        if (!warnings.isEmpty()) {
            log.warn("Configuration warnings detected:");
            warnings.forEach(w -> log.warn("  - {}", w));
        }

        // Fallar si hay errores críticos
        if (!errors.isEmpty()) {
            String errorMessage =
                    "Configuration validation failed:\n"
                            + String.join("\n", errors.stream().map(e -> "  - " + e).toList());
            log.error(errorMessage);
            throw new IllegalStateException(errorMessage);
        }

        log.info("Configuration validation completed successfully");
        logConfigurationSummary();
    }

    private void validateCriticalProperties(List<String> errors) {
        // API Version
        if (isBlank(apiVersion)) {
            errors.add("app.api.version must be configured (e.g., 'v1')");
        }

        // API Base Path
        if (isBlank(apiBasePath)) {
            errors.add("app.api.base-path must be configured (e.g., '/api')");
        } else if (!apiBasePath.startsWith("/")) {
            errors.add("app.api.base-path must start with '/' (current: '" + apiBasePath + "')");
        }

        // Datasource URL
        if (isBlank(datasourceUrl)) {
            errors.add("spring.datasource.url must be configured");
        } else if (datasourceUrl.contains("localhost") && isProductionProfile()) {
            errors.add(
                    "spring.datasource.url contains 'localhost' in production profile - this is"
                            + " likely a configuration error");
        }
    }

    private void validateRecommendedProperties(List<String> warnings) {
        // Cache configuration
        if (cacheEntitiesMaxSize <= 0) {
            warnings.add(
                    "app.cache.entities.max-size is not configured or is 0 - caching may be"
                            + " disabled");
        }

        // Rate limiting
        if (rateLimitMaxRequests <= 0) {
            warnings.add(
                    "app.rate-limit.max-requests is not configured - rate limiting may be"
                            + " disabled");
        }

        // CORS
        if (isBlank(corsAllowedOrigins)) {
            warnings.add(
                    "app.cors.allowed-origins is not configured - CORS may not work correctly");
        } else if (corsAllowedOrigins.contains("*") && isProductionProfile()) {
            warnings.add(
                    "app.cors.allowed-origins contains '*' in production - consider restricting to"
                            + " specific domains");
        }
    }

    private void validateProfileSpecificProperties(List<String> errors, List<String> warnings) {
        if (isProductionProfile()) {
            validateProductionProfile(errors, warnings);
        } else if (isDevelopmentProfile()) {
            validateDevelopmentProfile(warnings);
        }
    }

    private void validateProductionProfile(List<String> errors, List<String> warnings) {
        // Verificar que no se use ddl-auto: update o create en producción
        String ddlAuto = environment.getProperty("spring.jpa.hibernate.ddl-auto", "");
        if ("update".equals(ddlAuto) || "create".equals(ddlAuto) || "create-drop".equals(ddlAuto)) {
            errors.add(
                    "spring.jpa.hibernate.ddl-auto='"
                            + ddlAuto
                            + "' is not safe for production. Use 'none' or 'validate'");
        }

        // Verificar que show-sql esté deshabilitado
        boolean showSql = environment.getProperty("spring.jpa.show-sql", Boolean.class, false);
        if (showSql) {
            warnings.add("spring.jpa.show-sql=true in production may impact performance");
        }

        // Verificar nivel de logging
        String rootLogLevel = environment.getProperty("logging.level.root", "INFO");
        if ("DEBUG".equalsIgnoreCase(rootLogLevel) || "TRACE".equalsIgnoreCase(rootLogLevel)) {
            warnings.add(
                    "Root logging level is "
                            + rootLogLevel
                            + " in production - consider using INFO or WARN");
        }
    }

    private void validateDevelopmentProfile(List<String> warnings) {
        // Advertir si las credenciales por defecto están en uso
        String dbUsername = environment.getProperty("spring.datasource.username", "");
        if ("apigen_user".equals(dbUsername) || "postgres".equals(dbUsername)) {
            warnings.add(
                    "Using default database username '"
                            + dbUsername
                            + "' - consider using environment variables");
        }
    }

    private void logConfigurationSummary() {
        log.info("=== Configuration Summary ===");
        log.info("  Profile: {}", activeProfile);
        log.info("  API Version: {}", apiVersion);
        log.info("  API Base Path: {}", apiBasePath);
        if (log.isInfoEnabled()) {
            log.info("  Database URL: {}", maskSensitiveUrl(datasourceUrl));
        }
        log.info("  Cache Max Size: {}", cacheEntitiesMaxSize);
        log.info("  Rate Limit: {} requests/window", rateLimitMaxRequests);
        log.info("=============================");
    }

    private boolean isProductionProfile() {
        return "prod".equalsIgnoreCase(activeProfile)
                || "production".equalsIgnoreCase(activeProfile);
    }

    private boolean isDevelopmentProfile() {
        return "dev".equalsIgnoreCase(activeProfile)
                || "development".equalsIgnoreCase(activeProfile)
                || "local".equalsIgnoreCase(activeProfile);
    }

    private boolean isTestProfile() {
        // Check both the property and Spring's active profiles (for @ActiveProfiles in tests)
        if ("test".equalsIgnoreCase(activeProfile)) {
            return true;
        }
        // Also check Environment.getActiveProfiles() for @ActiveProfiles annotation
        for (String profile : environment.getActiveProfiles()) {
            if ("test".equalsIgnoreCase(profile)) {
                return true;
            }
        }
        return false;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String maskSensitiveUrl(String url) {
        if (url == null) return "null";
        // Ocultar credenciales en la URL si las hay
        return url.replaceAll("://[^:]+:[^@]+@", "://***:***@");
    }
}
