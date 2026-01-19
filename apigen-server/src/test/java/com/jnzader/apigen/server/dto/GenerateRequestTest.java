package com.jnzader.apigen.server.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.jnzader.apigen.server.config.GeneratedProjectVersions;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("GenerateRequest Tests")
class GenerateRequestTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Nested
    @DisplayName("ProjectConfig")
    class ProjectConfigTests {

        @Test
        @DisplayName("Should generate base package from groupId and artifactId")
        void shouldGenerateBasePackage() {
            GenerateRequest.ProjectConfig config =
                    GenerateRequest.ProjectConfig.builder()
                            .groupId("com.example")
                            .artifactId("my-api")
                            .build();

            assertThat(config.getBasePackage()).isEqualTo("com.example.myapi");
        }

        @Test
        @DisplayName("Should remove hyphens from artifactId in base package")
        void shouldRemoveHyphensFromArtifactId() {
            GenerateRequest.ProjectConfig config =
                    GenerateRequest.ProjectConfig.builder()
                            .groupId("org.company")
                            .artifactId("user-management-api")
                            .build();

            assertThat(config.getBasePackage()).isEqualTo("org.company.usermanagementapi");
        }

        @Test
        @DisplayName("Should use default values when not specified")
        void shouldUseDefaultValues() {
            GenerateRequest.ProjectConfig config = new GenerateRequest.ProjectConfig();

            assertThat(config.getJavaVersion()).isEqualTo(GeneratedProjectVersions.JAVA_VERSION);
            assertThat(config.getSpringBootVersion())
                    .isEqualTo(GeneratedProjectVersions.SPRING_BOOT_VERSION);
            assertThat(config.getModules()).isNotNull();
            assertThat(config.getFeatures()).isNotNull();
            assertThat(config.getDatabase()).isNotNull();
        }

        @ParameterizedTest
        @ValueSource(strings = {"com.example", "org.company.team", "io.github.user"})
        @DisplayName("Should accept valid group IDs")
        void shouldAcceptValidGroupIds(String groupId) {
            GenerateRequest.ProjectConfig config =
                    GenerateRequest.ProjectConfig.builder()
                            .name("Test Project")
                            .groupId(groupId)
                            .artifactId("my-api")
                            .build();

            Set<ConstraintViolation<GenerateRequest.ProjectConfig>> violations =
                    validator.validate(config);
            assertThat(violations).isEmpty();
        }

        @ParameterizedTest
        @ValueSource(
                strings = {"Com.Example", "com.Example", "123.company", ".invalid", "com..double"})
        @DisplayName("Should reject invalid group IDs")
        void shouldRejectInvalidGroupIds(String groupId) {
            GenerateRequest.ProjectConfig config =
                    GenerateRequest.ProjectConfig.builder()
                            .name("Test Project")
                            .groupId(groupId)
                            .artifactId("my-api")
                            .build();

            Set<ConstraintViolation<GenerateRequest.ProjectConfig>> violations =
                    validator.validate(config);
            assertThat(violations).isNotEmpty();
        }

        @ParameterizedTest
        @ValueSource(strings = {"my-api", "user-service", "api"})
        @DisplayName("Should accept valid artifact IDs")
        void shouldAcceptValidArtifactIds(String artifactId) {
            GenerateRequest.ProjectConfig config =
                    GenerateRequest.ProjectConfig.builder()
                            .name("Test Project")
                            .groupId("com.example")
                            .artifactId(artifactId)
                            .build();

            Set<ConstraintViolation<GenerateRequest.ProjectConfig>> violations =
                    validator.validate(config);
            assertThat(violations).isEmpty();
        }

        @ParameterizedTest
        @ValueSource(strings = {"My-API", "UserService", "123api", "-invalid"})
        @DisplayName("Should reject invalid artifact IDs")
        void shouldRejectInvalidArtifactIds(String artifactId) {
            GenerateRequest.ProjectConfig config =
                    GenerateRequest.ProjectConfig.builder()
                            .name("Test Project")
                            .groupId("com.example")
                            .artifactId(artifactId)
                            .build();

            Set<ConstraintViolation<GenerateRequest.ProjectConfig>> violations =
                    validator.validate(config);
            assertThat(violations).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("ModulesConfig")
    class ModulesConfigTests {

        @Test
        @DisplayName("Should use default module settings")
        void shouldUseDefaultModuleSettings() {
            GenerateRequest.ModulesConfig modules = new GenerateRequest.ModulesConfig();

            assertThat(modules.isCore()).isTrue();
            assertThat(modules.isSecurity()).isFalse();
        }
    }

    @Nested
    @DisplayName("FeaturesConfig")
    class FeaturesConfigTests {

        @Test
        @DisplayName("Should use default feature settings")
        void shouldUseDefaultFeatureSettings() {
            GenerateRequest.FeaturesConfig features = new GenerateRequest.FeaturesConfig();

            assertThat(features.isHateoas()).isTrue();
            assertThat(features.isSwagger()).isTrue();
            assertThat(features.isAuditing()).isTrue();
            assertThat(features.isSoftDelete()).isTrue();
            assertThat(features.isCaching()).isTrue();
            assertThat(features.isDocker()).isTrue();
        }
    }

    @Nested
    @DisplayName("DatabaseConfig")
    class DatabaseConfigTests {

        @Test
        @DisplayName("Should use default database settings")
        void shouldUseDefaultDatabaseSettings() {
            GenerateRequest.DatabaseConfig db = new GenerateRequest.DatabaseConfig();

            assertThat(db.getType()).isEqualTo("postgresql");
            assertThat(db.getName()).isEqualTo("appdb");
            assertThat(db.getUsername()).isEqualTo("appuser");
            assertThat(db.getPassword()).isEqualTo("changeme");
            assertThat(db.getPort()).isEqualTo(5432);
        }

        @Test
        @DisplayName("Should handle null values with defaults")
        void shouldHandleNullValues() {
            GenerateRequest.DatabaseConfig db =
                    GenerateRequest.DatabaseConfig.builder()
                            .type(null)
                            .name(null)
                            .username(null)
                            .password(null)
                            .port(null)
                            .build();

            assertThat(db.getType()).isEqualTo("postgresql");
            assertThat(db.getName()).isEqualTo("appdb");
            assertThat(db.getUsername()).isEqualTo("appuser");
            assertThat(db.getPassword()).isEqualTo("changeme");
            assertThat(db.getPort()).isEqualTo(5432);
        }

        @ParameterizedTest
        @CsvSource({
            "postgresql, " + GeneratedProjectVersions.POSTGRES_DOCKER_IMAGE,
            "mysql, " + GeneratedProjectVersions.MYSQL_DOCKER_IMAGE,
            "mariadb, " + GeneratedProjectVersions.MARIADB_DOCKER_IMAGE,
            "sqlserver, " + GeneratedProjectVersions.SQLSERVER_DOCKER_IMAGE,
            "oracle, " + GeneratedProjectVersions.ORACLE_DOCKER_IMAGE
        })
        @DisplayName("Should return correct Docker image for database type")
        void shouldReturnCorrectDockerImage(String type, String expectedImage) {
            GenerateRequest.DatabaseConfig db =
                    GenerateRequest.DatabaseConfig.builder().type(type).build();

            assertThat(db.getDockerImage()).isEqualTo(expectedImage);
        }

        @Test
        @DisplayName("Should return null Docker image for H2")
        void shouldReturnNullDockerImageForH2() {
            GenerateRequest.DatabaseConfig db =
                    GenerateRequest.DatabaseConfig.builder().type("h2").build();

            assertThat(db.getDockerImage()).isNull();
        }

        @ParameterizedTest
        @CsvSource({
            "postgresql, org.postgresql.Driver",
            "mysql, com.mysql.cj.jdbc.Driver",
            "mariadb, org.mariadb.jdbc.Driver",
            "sqlserver, com.microsoft.sqlserver.jdbc.SQLServerDriver",
            "oracle, oracle.jdbc.OracleDriver",
            "h2, org.h2.Driver"
        })
        @DisplayName("Should return correct driver class name")
        void shouldReturnCorrectDriverClassName(String type, String expectedDriver) {
            GenerateRequest.DatabaseConfig db =
                    GenerateRequest.DatabaseConfig.builder().type(type).build();

            assertThat(db.getDriverClassName()).isEqualTo(expectedDriver);
        }

        @ParameterizedTest
        @CsvSource({
            "postgresql, 5432",
            "mysql, 3306",
            "mariadb, 3306",
            "sqlserver, 1433",
            "oracle, 1521",
            "h2, 9092"
        })
        @DisplayName("Should return correct default port")
        void shouldReturnCorrectDefaultPort(String type, int expectedPort) {
            GenerateRequest.DatabaseConfig db =
                    GenerateRequest.DatabaseConfig.builder().type(type).build();

            assertThat(db.getDefaultPort()).isEqualTo(expectedPort);
        }

        @Test
        @DisplayName("Should generate correct PostgreSQL JDBC URL")
        void shouldGeneratePostgresJdbcUrl() {
            GenerateRequest.DatabaseConfig db =
                    GenerateRequest.DatabaseConfig.builder()
                            .type("postgresql")
                            .name("testdb")
                            .build();

            assertThat(db.getJdbcUrl()).isEqualTo("jdbc:postgresql://db:5432/testdb");
        }

        @Test
        @DisplayName("Should generate correct MySQL JDBC URL")
        void shouldGenerateMySqlJdbcUrl() {
            GenerateRequest.DatabaseConfig db =
                    GenerateRequest.DatabaseConfig.builder().type("mysql").name("testdb").build();

            assertThat(db.getJdbcUrl()).contains("jdbc:mysql://db:3306/testdb");
            assertThat(db.getJdbcUrl()).contains("useSSL=false");
        }

        @Test
        @DisplayName("Should generate correct H2 JDBC URL")
        void shouldGenerateH2JdbcUrl() {
            GenerateRequest.DatabaseConfig db =
                    GenerateRequest.DatabaseConfig.builder().type("h2").name("testdb").build();

            assertThat(db.getJdbcUrl()).isEqualTo("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1");
        }

        @ParameterizedTest
        @CsvSource({
            "postgresql, org.hibernate.dialect.PostgreSQLDialect",
            "mysql, org.hibernate.dialect.MySQLDialect",
            "mariadb, org.hibernate.dialect.MariaDBDialect",
            "sqlserver, org.hibernate.dialect.SQLServerDialect",
            "oracle, org.hibernate.dialect.OracleDialect",
            "h2, org.hibernate.dialect.H2Dialect"
        })
        @DisplayName("Should return correct Hibernate dialect")
        void shouldReturnCorrectHibernateDialect(String type, String expectedDialect) {
            GenerateRequest.DatabaseConfig db =
                    GenerateRequest.DatabaseConfig.builder().type(type).build();

            assertThat(db.getHibernateDialect()).isEqualTo(expectedDialect);
        }

        @Test
        @DisplayName("Should handle uppercase database types")
        void shouldHandleUppercaseDatabaseTypes() {
            GenerateRequest.DatabaseConfig db =
                    GenerateRequest.DatabaseConfig.builder().type("POSTGRESQL").build();

            assertThat(db.getDriverClassName()).isEqualTo("org.postgresql.Driver");
            assertThat(db.getDefaultPort()).isEqualTo(5432);
        }

        @Test
        @DisplayName("Should default to PostgreSQL for unknown types")
        void shouldDefaultToPostgresForUnknownTypes() {
            GenerateRequest.DatabaseConfig db =
                    GenerateRequest.DatabaseConfig.builder().type("unknown").build();

            assertThat(db.getDockerImage())
                    .isEqualTo(GeneratedProjectVersions.POSTGRES_DOCKER_IMAGE);
            assertThat(db.getDriverClassName()).isEqualTo("org.postgresql.Driver");
            assertThat(db.getDefaultPort()).isEqualTo(5432);
            assertThat(db.getHibernateDialect())
                    .isEqualTo("org.hibernate.dialect.PostgreSQLDialect");
        }
    }

    @Nested
    @DisplayName("Request Validation")
    class RequestValidationTests {

        @Test
        @DisplayName("Should require project configuration")
        void shouldRequireProjectConfiguration() {
            GenerateRequest request =
                    GenerateRequest.builder().sql("CREATE TABLE test (id BIGINT)").build();

            Set<ConstraintViolation<GenerateRequest>> violations = validator.validate(request);
            assertThat(violations)
                    .extracting(v -> v.getPropertyPath().toString())
                    .contains("project");
        }

        @Test
        @DisplayName("Should require SQL schema")
        void shouldRequireSqlSchema() {
            GenerateRequest request =
                    GenerateRequest.builder()
                            .project(
                                    GenerateRequest.ProjectConfig.builder()
                                            .name("Test")
                                            .groupId("com.example")
                                            .artifactId("test-api")
                                            .build())
                            .build();

            Set<ConstraintViolation<GenerateRequest>> violations = validator.validate(request);
            assertThat(violations).extracting(v -> v.getPropertyPath().toString()).contains("sql");
        }

        @Test
        @DisplayName("Should pass validation with valid request")
        void shouldPassValidationWithValidRequest() {
            GenerateRequest request =
                    GenerateRequest.builder()
                            .project(
                                    GenerateRequest.ProjectConfig.builder()
                                            .name("Test Project")
                                            .groupId("com.example")
                                            .artifactId("test-api")
                                            .build())
                            .sql("CREATE TABLE test (id BIGINT PRIMARY KEY)")
                            .build();

            Set<ConstraintViolation<GenerateRequest>> violations = validator.validate(request);
            assertThat(violations).isEmpty();
        }
    }
}
