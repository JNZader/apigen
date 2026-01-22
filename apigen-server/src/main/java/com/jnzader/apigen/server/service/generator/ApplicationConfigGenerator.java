package com.jnzader.apigen.server.service.generator;

import com.jnzader.apigen.server.dto.GenerateRequest.*;
import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.stereotype.Component;

/** Generates application configuration files (application.yml, application-docker.yml). */
@Component
public class ApplicationConfigGenerator {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Generates the main application.yml file content.
     *
     * @param config the project configuration
     * @return the application.yml content
     */
    public String generateApplicationYml(ProjectConfig config) {
        StringBuilder yml = new StringBuilder();

        // Spring configuration
        yml.append(
                """
                spring:
                  application:
                    name: %s

                  datasource:
                    url: jdbc:h2:mem:testdb
                    driver-class-name: org.h2.Driver
                    username: sa
                    password:

                  jpa:
                    hibernate:
                      ddl-auto: create-drop
                    show-sql: true
                    properties:
                      hibernate:
                        format_sql: true

                  h2:
                    console:
                      enabled: true
                      path: /h2-console

                server:
                  port: 8080

                """
                        .formatted(config.getArtifactId()));

        // APiGen Core Configuration
        yml.append("# APiGen Core Configuration\n");
        yml.append("app:\n");
        yml.append("  api:\n");
        yml.append("    version: v1\n");
        yml.append("    base-path: /api\n");

        // CORS configuration
        appendCorsConfig(yml, config.getCorsConfig());

        // Cache configuration
        appendCacheConfig(yml, config.getCacheConfig());

        // Rate limit configuration
        appendRateLimitConfig(yml, config.getRateLimitConfig());

        // Security configuration (if security module is enabled)
        if (config.getModules() != null && config.getModules().isSecurity()) {
            appendSecurityConfig(yml, config.getSecurityConfig());
        }

        // OpenAPI configuration
        yml.append(
                """

                # OpenAPI
                springdoc:
                  api-docs:
                    path: /api-docs
                  swagger-ui:
                    path: /swagger-ui.html
                    operationsSorter: method
                  enable-hateoas: false

                # Actuator
                management:
                  endpoints:
                    web:
                      exposure:
                        include: health,info,metrics
                  endpoint:
                    health:
                      show-details: always
                """);

        return yml.toString();
    }

    private void appendCorsConfig(StringBuilder yml, CorsConfig cors) {
        yml.append("\n  # CORS configuration\n");
        yml.append("  cors:\n");

        if (cors != null
                && cors.getAllowedOrigins() != null
                && !cors.getAllowedOrigins().isEmpty()) {
            yml.append("    allowed-origins:\n");
            for (String origin : cors.getAllowedOrigins()) {
                yml.append("      - ").append(origin).append("\n");
            }
        } else {
            yml.append("    allowed-origins:\n");
            yml.append("      - http://localhost:3000\n");
            yml.append("      - http://localhost:4200\n");
            yml.append("      - http://localhost:8080\n");
        }

        if (cors != null
                && cors.getAllowedMethods() != null
                && !cors.getAllowedMethods().isEmpty()) {
            yml.append("    allowed-methods:\n");
            for (String method : cors.getAllowedMethods()) {
                yml.append("      - ").append(method).append("\n");
            }
        } else {
            yml.append("    allowed-methods:\n");
            yml.append("      - GET\n");
            yml.append("      - POST\n");
            yml.append("      - PUT\n");
            yml.append("      - PATCH\n");
            yml.append("      - DELETE\n");
            yml.append("      - OPTIONS\n");
        }

        if (cors != null
                && cors.getAllowedHeaders() != null
                && !cors.getAllowedHeaders().isEmpty()) {
            yml.append("    allowed-headers:\n");
            for (String header : cors.getAllowedHeaders()) {
                yml.append("      - ").append(header).append("\n");
            }
        } else {
            yml.append("    allowed-headers:\n");
            yml.append("      - Authorization\n");
            yml.append("      - Content-Type\n");
            yml.append("      - X-Requested-With\n");
        }

        if (cors != null
                && cors.getExposedHeaders() != null
                && !cors.getExposedHeaders().isEmpty()) {
            yml.append("    exposed-headers:\n");
            for (String header : cors.getExposedHeaders()) {
                yml.append("      - ").append(header).append("\n");
            }
        } else {
            yml.append("    exposed-headers:\n");
            yml.append("      - Authorization\n");
            yml.append("      - X-Total-Count\n");
            yml.append("      - X-Page-Number\n");
            yml.append("      - X-Page-Size\n");
        }

        yml.append("    allow-credentials: ")
                .append(cors != null ? cors.isAllowCredentials() : true)
                .append("\n");
        yml.append("    max-age: ")
                .append(cors != null ? cors.getMaxAgeSeconds() : 3600)
                .append("\n");
    }

    private void appendCacheConfig(StringBuilder yml, CacheConfig cache) {
        yml.append("\n  # Cache configuration\n");
        yml.append("  cache:\n");

        int entitiesMaxSize = 1000;
        int entitiesExpire = 10;
        int listsMaxSize = 100;
        int listsExpire = 5;

        if (cache != null) {
            if (cache.getEntities() != null) {
                entitiesMaxSize = cache.getEntities().getMaxSize();
                entitiesExpire = cache.getEntities().getExpireAfterWriteMinutes();
            }
            if (cache.getLists() != null) {
                listsMaxSize = cache.getLists().getMaxSize();
                listsExpire = cache.getLists().getExpireAfterWriteMinutes();
            }
        }

        yml.append("    entities:\n");
        yml.append("      max-size: ").append(entitiesMaxSize).append("\n");
        yml.append("      expire-after-write-minutes: ").append(entitiesExpire).append("\n");
        yml.append("    lists:\n");
        yml.append("      max-size: ").append(listsMaxSize).append("\n");
        yml.append("      expire-after-write-minutes: ").append(listsExpire).append("\n");
    }

    private void appendRateLimitConfig(StringBuilder yml, RateLimitConfig rateLimit) {
        yml.append("\n  # Rate limiting configuration\n");
        yml.append("  rate-limit:\n");
        yml.append("    enabled: true\n");

        int requestsPerSecond = 100;
        int burstCapacity = 150;

        if (rateLimit != null) {
            requestsPerSecond = rateLimit.getRequestsPerSecond();
            burstCapacity = rateLimit.getBurstCapacity();
        }

        yml.append("    max-requests: ").append(requestsPerSecond).append("\n");
        yml.append("    window-seconds: 1\n");
        yml.append("    burst-capacity: ").append(burstCapacity).append("\n");
    }

    private void appendSecurityConfig(StringBuilder yml, SecurityConfigDTO security) {
        yml.append("\n# APiGen Security Configuration\n");
        yml.append("apigen:\n");
        yml.append("  security:\n");
        yml.append("    enabled: true\n");

        int accessTokenExp = 30;
        int refreshTokenExp = 7;
        int jwtSecretLength = 64;

        if (security != null) {
            accessTokenExp = security.getAccessTokenExpiration();
            refreshTokenExp = security.getRefreshTokenExpiration();
            jwtSecretLength = security.getJwtSecretLength();
        }

        // Generate a secure random JWT secret
        String jwtSecret = generateSecureSecret(jwtSecretLength);

        yml.append("    jwt:\n");
        yml.append("      secret: ").append(jwtSecret).append("\n");
        yml.append("      access-token-expiration: ").append(accessTokenExp).append("m\n");
        yml.append("      refresh-token-expiration: ").append(refreshTokenExp).append("d\n");
    }

    /**
     * Generates a cryptographically secure random secret.
     *
     * @param length the desired length of the secret
     * @return a Base64-encoded secure random string
     */
    private String generateSecureSecret(int length) {
        byte[] bytes = new byte[length];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes).substring(0, length);
    }

    /**
     * Generates application-docker.yml for Docker-specific configuration.
     *
     * @param config the project configuration
     * @return the application-docker.yml content
     */
    public String generateApplicationDockerYml(ProjectConfig config) {
        DatabaseConfig db =
                config.getDatabase() != null ? config.getDatabase() : new DatabaseConfig();

        return """
        # =============================================================================
        # Docker Profile Configuration
        # =============================================================================
        # This profile is automatically activated when running in Docker.
        # Database connection uses the Docker network hostname 'db'.
        # =============================================================================

        spring:
          datasource:
            url: ${SPRING_DATASOURCE_URL:%s}
            username: ${SPRING_DATASOURCE_USERNAME:%s}
            password: ${SPRING_DATASOURCE_PASSWORD:%s}
            driver-class-name: %s
            hikari:
              maximum-pool-size: 10
              minimum-idle: 5
              connection-timeout: 30000
              idle-timeout: 600000
              max-lifetime: 1800000

          jpa:
            hibernate:
              ddl-auto: update
            show-sql: false
            properties:
              hibernate:
                format_sql: false
                dialect: %s
                jdbc:
                  batch_size: 50
                order_inserts: true
                order_updates: true

          # Disable H2 console in Docker
          h2:
            console:
              enabled: false

        # Actuator configuration for container health checks
        management:
          endpoints:
            web:
              exposure:
                include: health,info,metrics,prometheus
          endpoint:
            health:
              show-details: always
              probes:
                enabled: true
          health:
            livenessState:
              enabled: true
            readinessState:
              enabled: true

        # Logging for production
        logging:
          level:
            root: INFO
            %s: INFO
            org.hibernate.SQL: WARN
            org.hibernate.type.descriptor.sql: WARN
          pattern:
            console: "%%d{yyyy-MM-dd HH:mm:ss} [%%thread] %%-5level %%logger{36} - %%msg%%n"
        """
                .formatted(
                        db.getJdbcUrl(),
                        db.getUsername(),
                        db.getPassword(),
                        db.getDriverClassName(),
                        db.getHibernateDialect(),
                        config.getBasePackage());
    }

    /**
     * Generates application-test.yml for test-specific configuration. Disables rate limiting and
     * other production features that interfere with tests.
     *
     * @param config the project configuration
     * @return the application-test.yml content
     */
    public String generateApplicationTestYml(ProjectConfig config) {
        boolean securityEnabled = config.getModules() != null && config.getModules().isSecurity();

        StringBuilder yml = new StringBuilder();
        yml.append(
                """
                # =============================================================================
                # Test Profile Configuration
                # =============================================================================
                # This profile is automatically activated by @ActiveProfiles("test").
                # Disables rate limiting and other features that interfere with integration tests.
                # =============================================================================

                # Disable rate limiting for tests (apigen-core)
                app:
                  rate-limit:
                    enabled: false
                """);

        if (securityEnabled) {
            yml.append(
                    """

                    # Disable security for tests (apigen-security)
                    # This allows integration tests to run without authentication
                    apigen:
                      security:
                        enabled: false
                    """);
        }

        yml.append(
                """

                # Logging configuration for tests
                logging:
                  level:
                    root: WARN
                    org.springframework.test: INFO
                """);

        return yml.toString();
    }
}
