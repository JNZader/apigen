package com.jnzader.apigen.server.service.generator;

import static org.assertj.core.api.Assertions.assertThat;

import com.jnzader.apigen.server.config.GeneratedProjectVersions;
import com.jnzader.apigen.server.dto.GenerateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ApplicationConfigGenerator Tests")
class ApplicationConfigGeneratorTest {

    private ApplicationConfigGenerator applicationConfigGenerator;

    @BeforeEach
    void setUp() {
        applicationConfigGenerator = new ApplicationConfigGenerator();
    }

    @Nested
    @DisplayName("generateApplicationYml()")
    class GenerateApplicationYmlTests {

        @Test
        @DisplayName("Should generate application name from artifact ID")
        void shouldGenerateApplicationNameFromArtifactId() {
            GenerateRequest.ProjectConfig config = createDefaultConfig();

            String result = applicationConfigGenerator.generateApplicationYml(config);

            assertThat(result).contains("name: my-api");
        }

        @Test
        @DisplayName("Should configure H2 in-memory database by default")
        void shouldConfigureH2InMemoryDatabaseByDefault() {
            GenerateRequest.ProjectConfig config = createDefaultConfig();

            String result = applicationConfigGenerator.generateApplicationYml(config);

            assertThat(result)
                    .contains("url: jdbc:h2:mem:testdb")
                    .contains("driver-class-name: org.h2.Driver")
                    .contains("username: sa");
        }

        @Test
        @DisplayName("Should configure JPA with create-drop mode")
        void shouldConfigureJPAWithCreateDropMode() {
            GenerateRequest.ProjectConfig config = createDefaultConfig();

            String result = applicationConfigGenerator.generateApplicationYml(config);

            assertThat(result)
                    .contains("ddl-auto: create-drop")
                    .contains("show-sql: true")
                    .contains("format_sql: true");
        }

        @Test
        @DisplayName("Should enable H2 console")
        void shouldEnableH2Console() {
            GenerateRequest.ProjectConfig config = createDefaultConfig();

            String result = applicationConfigGenerator.generateApplicationYml(config);

            assertThat(result)
                    .contains("h2:")
                    .contains("console:")
                    .contains("enabled: true")
                    .contains("path: /h2-console");
        }

        @Test
        @DisplayName("Should configure server port 8080")
        void shouldConfigureServerPort8080() {
            GenerateRequest.ProjectConfig config = createDefaultConfig();

            String result = applicationConfigGenerator.generateApplicationYml(config);

            assertThat(result).contains("server:").contains("port: 8080");
        }

        @Test
        @DisplayName("Should configure APiGen settings")
        void shouldConfigureApiGenSettings() {
            GenerateRequest.ProjectConfig config = createDefaultConfig();

            String result = applicationConfigGenerator.generateApplicationYml(config);

            assertThat(result)
                    .contains("app:")
                    .contains("api:")
                    .contains("version: v1")
                    .contains("base-path: /api");
        }

        @Test
        @DisplayName("Should configure SpringDoc OpenAPI")
        void shouldConfigureSpringDocOpenAPI() {
            GenerateRequest.ProjectConfig config = createDefaultConfig();

            String result = applicationConfigGenerator.generateApplicationYml(config);

            assertThat(result)
                    .contains("springdoc:")
                    .contains("api-docs:")
                    .contains("path: /api-docs")
                    .contains("swagger-ui:")
                    .contains("path: /swagger-ui.html");
        }

        @Test
        @DisplayName("Should configure actuator endpoints")
        void shouldConfigureActuatorEndpoints() {
            GenerateRequest.ProjectConfig config = createDefaultConfig();

            String result = applicationConfigGenerator.generateApplicationYml(config);

            assertThat(result)
                    .contains("management:")
                    .contains("endpoints:")
                    .contains("include: health,info,metrics");
        }
    }

    @Nested
    @DisplayName("generateApplicationDockerYml()")
    class GenerateApplicationDockerYmlTests {

        @Test
        @DisplayName("Should include Docker profile comment")
        void shouldIncludeDockerProfileComment() {
            GenerateRequest.ProjectConfig config = createDefaultConfig();

            String result = applicationConfigGenerator.generateApplicationDockerYml(config);

            assertThat(result).contains("Docker Profile Configuration");
        }

        @Test
        @DisplayName("Should use environment variable placeholders")
        void shouldUseEnvironmentVariablePlaceholders() {
            GenerateRequest.ProjectConfig config = createDefaultConfig();
            config.setDatabase(
                    GenerateRequest.DatabaseConfig.builder()
                            .type("postgresql")
                            .username("testuser")
                            .password("testpass")
                            .build());

            String result = applicationConfigGenerator.generateApplicationDockerYml(config);

            assertThat(result)
                    .contains("${SPRING_DATASOURCE_URL:")
                    .contains("${SPRING_DATASOURCE_USERNAME:")
                    .contains("${SPRING_DATASOURCE_PASSWORD:");
        }

        @Test
        @DisplayName("Should configure HikariCP connection pool")
        void shouldConfigureHikariCPConnectionPool() {
            GenerateRequest.ProjectConfig config = createDefaultConfig();

            String result = applicationConfigGenerator.generateApplicationDockerYml(config);

            assertThat(result)
                    .contains("hikari:")
                    .contains("maximum-pool-size: 10")
                    .contains("minimum-idle: 5")
                    .contains("connection-timeout: 30000");
        }

        @Test
        @DisplayName("Should configure JPA for production")
        void shouldConfigureJPAForProduction() {
            GenerateRequest.ProjectConfig config = createDefaultConfig();

            String result = applicationConfigGenerator.generateApplicationDockerYml(config);

            assertThat(result)
                    .contains("ddl-auto: update")
                    .contains("show-sql: false")
                    .contains("batch_size: 50")
                    .contains("order_inserts: true")
                    .contains("order_updates: true");
        }

        @Test
        @DisplayName("Should disable H2 console in Docker")
        void shouldDisableH2ConsoleInDocker() {
            GenerateRequest.ProjectConfig config = createDefaultConfig();

            String result = applicationConfigGenerator.generateApplicationDockerYml(config);

            assertThat(result).contains("h2:").contains("console:").contains("enabled: false");
        }

        @Test
        @DisplayName("Should configure actuator probes for containers")
        void shouldConfigureActuatorProbesForContainers() {
            GenerateRequest.ProjectConfig config = createDefaultConfig();

            String result = applicationConfigGenerator.generateApplicationDockerYml(config);

            assertThat(result)
                    .contains("probes:")
                    .contains("enabled: true")
                    .contains("livenessState:")
                    .contains("readinessState:");
        }

        @Test
        @DisplayName("Should configure production logging")
        void shouldConfigureProductionLogging() {
            GenerateRequest.ProjectConfig config = createDefaultConfig();

            String result = applicationConfigGenerator.generateApplicationDockerYml(config);

            assertThat(result).contains("logging:").contains("level:").contains("root: INFO");
        }

        @Test
        @DisplayName("Should include package name in logging")
        void shouldIncludePackageNameInLogging() {
            GenerateRequest.ProjectConfig config = createDefaultConfig();

            String result = applicationConfigGenerator.generateApplicationDockerYml(config);

            assertThat(result).contains("com.example.myapi: INFO");
        }

        @Test
        @DisplayName("Should include correct PostgreSQL driver class")
        void shouldIncludeCorrectPostgreSQLDriverClass() {
            GenerateRequest.ProjectConfig config = createDefaultConfig();

            String result = applicationConfigGenerator.generateApplicationDockerYml(config);

            assertThat(result)
                    .contains("driver-class-name: org.postgresql.Driver")
                    .contains("dialect: org.hibernate.dialect.PostgreSQLDialect");
        }

        @Test
        @DisplayName("Should include correct MySQL configuration")
        void shouldIncludeCorrectMySQLConfiguration() {
            GenerateRequest.ProjectConfig config = createDefaultConfig();
            config.setDatabase(GenerateRequest.DatabaseConfig.builder().type("mysql").build());

            String result = applicationConfigGenerator.generateApplicationDockerYml(config);

            assertThat(result)
                    .contains("driver-class-name: com.mysql.cj.jdbc.Driver")
                    .contains("dialect: org.hibernate.dialect.MySQLDialect");
        }

        @Test
        @DisplayName("Should use default database config when null")
        void shouldUseDefaultDatabaseConfigWhenNull() {
            GenerateRequest.ProjectConfig config = createDefaultConfig();
            config.setDatabase(null);

            String result = applicationConfigGenerator.generateApplicationDockerYml(config);

            assertThat(result).contains("jdbc:postgresql://db:5432/appdb");
        }
    }

    @Nested
    @DisplayName("generateApplicationTestYml()")
    class GenerateApplicationTestYmlTests {

        @Test
        @DisplayName("Should include test profile comment")
        void shouldIncludeTestProfileComment() {
            String result = applicationConfigGenerator.generateApplicationTestYml();

            assertThat(result).contains("Test Profile Configuration");
        }

        @Test
        @DisplayName("Should disable rate limiting for tests")
        void shouldDisableRateLimitingForTests() {
            String result = applicationConfigGenerator.generateApplicationTestYml();

            assertThat(result).contains("app:").contains("rate-limit:").contains("enabled: false");
        }

        @Test
        @DisplayName("Should configure logging for tests")
        void shouldConfigureLoggingForTests() {
            String result = applicationConfigGenerator.generateApplicationTestYml();

            assertThat(result).contains("logging:").contains("level:").contains("root: WARN");
        }
    }

    private GenerateRequest.ProjectConfig createDefaultConfig() {
        return GenerateRequest.ProjectConfig.builder()
                .name("My API")
                .groupId("com.example")
                .artifactId("my-api")
                .javaVersion(GeneratedProjectVersions.JAVA_VERSION)
                .springBootVersion(GeneratedProjectVersions.SPRING_BOOT_VERSION)
                .database(GenerateRequest.DatabaseConfig.builder().build())
                .build();
    }
}
