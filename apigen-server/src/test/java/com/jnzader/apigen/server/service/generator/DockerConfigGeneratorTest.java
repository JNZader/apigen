package com.jnzader.apigen.server.service.generator;

import static org.assertj.core.api.Assertions.assertThat;

import com.jnzader.apigen.server.config.GeneratedProjectVersions;
import com.jnzader.apigen.server.dto.GenerateRequest;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("DockerConfigGenerator Tests")
@SuppressWarnings(
        "java:S5976") // Tests validate different specific features, not the same feature with
// different inputs
class DockerConfigGeneratorTest {

    private DockerConfigGenerator dockerConfigGenerator;

    @BeforeEach
    void setUp() {
        dockerConfigGenerator = new DockerConfigGenerator();
    }

    @Nested
    @DisplayName("generateDockerfile()")
    class GenerateDockerfileTests {

        @Test
        @DisplayName("Should include artifact ID in comments and labels")
        void shouldIncludeArtifactIdInCommentsAndLabels() {
            GenerateRequest.ProjectConfig config = createDefaultConfig();

            String result = dockerConfigGenerator.generateDockerfile(config);

            assertThat(result)
                    .contains("my-api - Dockerfile")
                    .contains("docker build -t my-api:latest .");
        }

        @Test
        @DisplayName("Should use provided Java version")
        void shouldUseProvidedJavaVersion() {
            GenerateRequest.ProjectConfig config = createDefaultConfig();
            config.setJavaVersion("21");

            String result = dockerConfigGenerator.generateDockerfile(config);

            assertThat(result)
                    .contains("eclipse-temurin:21-jre-alpine")
                    .contains("optimized for Java 21");
        }

        @Test
        @DisplayName("Should include security best practices")
        void shouldIncludeSecurityBestPractices() {
            GenerateRequest.ProjectConfig config = createDefaultConfig();

            String result = dockerConfigGenerator.generateDockerfile(config);

            assertThat(result).contains("addgroup").contains("adduser").contains("USER appuser");
        }

        @Test
        @DisplayName("Should copy JAR file with correct permissions")
        void shouldCopyJarFileWithCorrectPermissions() {
            GenerateRequest.ProjectConfig config = createDefaultConfig();

            String result = dockerConfigGenerator.generateDockerfile(config);

            assertThat(result).contains("COPY --chown=appuser:appgroup build/libs/*.jar app.jar");
        }

        @Test
        @DisplayName("Should include Java optimization flags")
        void shouldIncludeJavaOptimizationFlags() {
            GenerateRequest.ProjectConfig config = createDefaultConfig();

            String result = dockerConfigGenerator.generateDockerfile(config);

            assertThat(result)
                    .contains("JAVA_OPTS")
                    .contains("-XX:+UseZGC")
                    .contains("-XX:MaxRAMPercentage=75.0");
        }

        @Test
        @DisplayName("Should set Docker profile")
        void shouldSetDockerProfile() {
            GenerateRequest.ProjectConfig config = createDefaultConfig();

            String result = dockerConfigGenerator.generateDockerfile(config);

            assertThat(result).contains("SPRING_PROFILES_ACTIVE=docker");
        }

        @Test
        @DisplayName("Should expose port 8080")
        void shouldExposePort8080() {
            GenerateRequest.ProjectConfig config = createDefaultConfig();

            String result = dockerConfigGenerator.generateDockerfile(config);

            assertThat(result).contains("EXPOSE 8080");
        }

        @Test
        @DisplayName("Should include health check")
        void shouldIncludeHealthCheck() {
            GenerateRequest.ProjectConfig config = createDefaultConfig();

            String result = dockerConfigGenerator.generateDockerfile(config);

            assertThat(result).contains("HEALTHCHECK").contains("/actuator/health");
        }

        @Test
        @DisplayName("Should include metadata labels")
        void shouldIncludeMetadataLabels() {
            GenerateRequest.ProjectConfig config = createDefaultConfig();
            config.setGroupId("com.mycompany");

            String result = dockerConfigGenerator.generateDockerfile(config);

            assertThat(result)
                    .contains("LABEL maintainer=\"com.mycompany\"")
                    .contains("LABEL description=\"my-api API Service\"");
        }
    }

    @Nested
    @DisplayName("generateDockerCompose()")
    class GenerateDockerComposeTests {

        @Test
        @DisplayName("Should include app service configuration")
        void shouldIncludeAppServiceConfiguration() {
            GenerateRequest.ProjectConfig config = createDefaultConfig();

            String result = dockerConfigGenerator.generateDockerCompose(config);

            assertThat(result)
                    .contains("services:")
                    .contains("app:")
                    .contains("container_name: my-api-app")
                    .contains("ports:")
                    .contains("\"8080:8080\"");
        }

        @Test
        @DisplayName("Should configure database environment variables")
        void shouldConfigureDatabaseEnvironmentVariables() {
            GenerateRequest.ProjectConfig config = createDefaultConfig();

            String result = dockerConfigGenerator.generateDockerCompose(config);

            assertThat(result)
                    .contains("SPRING_DATASOURCE_URL")
                    .contains("SPRING_DATASOURCE_USERNAME")
                    .contains("SPRING_DATASOURCE_PASSWORD");
        }

        @Test
        @DisplayName("Should set app dependency on db")
        void shouldSetAppDependencyOnDb() {
            GenerateRequest.ProjectConfig config = createDefaultConfig();

            String result = dockerConfigGenerator.generateDockerCompose(config);

            assertThat(result)
                    .contains("depends_on:")
                    .contains("db:")
                    .contains("condition: service_healthy");
        }

        @Test
        @DisplayName("Should include network configuration")
        void shouldIncludeNetworkConfiguration() {
            GenerateRequest.ProjectConfig config = createDefaultConfig();

            String result = dockerConfigGenerator.generateDockerCompose(config);

            assertThat(result)
                    .contains("networks:")
                    .contains("app-network:")
                    .contains("driver: bridge");
        }

        @Test
        @DisplayName("Should include volumes configuration")
        void shouldIncludeVolumesConfiguration() {
            GenerateRequest.ProjectConfig config = createDefaultConfig();

            String result = dockerConfigGenerator.generateDockerCompose(config);

            assertThat(result).contains("volumes:").contains("db-data:").contains("driver: local");
        }

        @Test
        @DisplayName("Should include app health check")
        void shouldIncludeAppHealthCheck() {
            GenerateRequest.ProjectConfig config = createDefaultConfig();

            String result = dockerConfigGenerator.generateDockerCompose(config);

            assertThat(result)
                    .contains("healthcheck:")
                    .contains("http://localhost:8080/actuator/health");
        }
    }

    @Nested
    @DisplayName("generateDatabaseService()")
    class GenerateDatabaseServiceTests {

        static Stream<Arguments> databaseServiceTestCases() {
            return Stream.of(
                    Arguments.of(
                            "postgresql",
                            GeneratedProjectVersions.POSTGRES_DOCKER_IMAGE,
                            5432,
                            new String[] {
                                "# PostgreSQL Database",
                                "POSTGRES_DB",
                                "POSTGRES_USER",
                                "POSTGRES_PASSWORD",
                                "/var/lib/postgresql/data"
                            }),
                    Arguments.of(
                            "mysql",
                            GeneratedProjectVersions.MYSQL_DOCKER_IMAGE,
                            3306,
                            new String[] {
                                "# MySQL Database",
                                "MYSQL_DATABASE",
                                "MYSQL_USER",
                                "MYSQL_PASSWORD",
                                "MYSQL_ROOT_PASSWORD",
                                "/var/lib/mysql"
                            }),
                    Arguments.of(
                            "mariadb",
                            GeneratedProjectVersions.MARIADB_DOCKER_IMAGE,
                            3306,
                            new String[] {
                                "# MariaDB Database",
                                "MARIADB_DATABASE",
                                "MARIADB_USER",
                                "MARIADB_PASSWORD"
                            }),
                    Arguments.of(
                            "sqlserver",
                            GeneratedProjectVersions.SQLSERVER_DOCKER_IMAGE,
                            1433,
                            new String[] {
                                "# SQL Server Database",
                                "ACCEPT_EULA=Y",
                                "MSSQL_SA_PASSWORD",
                                "MSSQL_PID=Express",
                                "/var/opt/mssql"
                            }),
                    Arguments.of(
                            "oracle",
                            GeneratedProjectVersions.ORACLE_DOCKER_IMAGE,
                            1521,
                            new String[] {
                                "# Oracle Database",
                                "ORACLE_PASSWORD",
                                "APP_USER",
                                "APP_USER_PASSWORD",
                                "/opt/oracle/oradata"
                            }));
        }

        @ParameterizedTest(name = "Should generate {0} service")
        @MethodSource("databaseServiceTestCases")
        @DisplayName("Should generate database service with correct configuration")
        void shouldGenerateDatabaseService(
                String dbType, String dockerImage, int port, String[] expectedContents) {
            GenerateRequest.DatabaseConfig db = createDefaultDbConfig();

            String result =
                    dockerConfigGenerator.generateDatabaseService(db, dbType, dockerImage, port);

            assertThat(result).contains(dockerImage);
            for (String expected : expectedContents) {
                assertThat(result).contains(expected);
            }
        }

        @Test
        @DisplayName("Should include health check for each database type")
        void shouldIncludeHealthCheckForEachDatabaseType() {
            GenerateRequest.DatabaseConfig db = createDefaultDbConfig();

            String postgresResult =
                    dockerConfigGenerator.generateDatabaseService(
                            db, "postgresql", GeneratedProjectVersions.POSTGRES_DOCKER_IMAGE, 5432);
            String mysqlResult =
                    dockerConfigGenerator.generateDatabaseService(
                            db, "mysql", GeneratedProjectVersions.MYSQL_DOCKER_IMAGE, 3306);

            assertThat(postgresResult).contains("pg_isready");
            // In YAML array format, mysqladmin and ping are separate elements
            assertThat(mysqlResult).contains("mysqladmin").contains("ping");
        }
    }

    @Nested
    @DisplayName("generateDockerignore()")
    class GenerateDockerignoreTests {

        @Test
        @DisplayName("Should exclude build artifacts except JARs")
        void shouldExcludeBuildArtifactsExceptJars() {
            String result = dockerConfigGenerator.generateDockerignore();

            assertThat(result)
                    .contains("build/")
                    .contains("!build/libs/")
                    .contains("!build/libs/*.jar")
                    .contains(".gradle/");
        }

        @Test
        @DisplayName("Should exclude IDE files")
        void shouldExcludeIdeFiles() {
            String result = dockerConfigGenerator.generateDockerignore();

            assertThat(result).contains(".idea/").contains("*.iml").contains(".vscode/");
        }

        @Test
        @DisplayName("Should exclude Git files")
        void shouldExcludeGitFiles() {
            String result = dockerConfigGenerator.generateDockerignore();

            assertThat(result).contains(".git/").contains(".gitignore");
        }

        @Test
        @DisplayName("Should exclude environment files")
        void shouldExcludeEnvironmentFiles() {
            String result = dockerConfigGenerator.generateDockerignore();

            assertThat(result).contains(".env").contains(".env.*");
        }

        @Test
        @DisplayName("Should exclude test files")
        void shouldExcludeTestFiles() {
            String result = dockerConfigGenerator.generateDockerignore();

            assertThat(result).contains("src/test/");
        }

        @Test
        @DisplayName("Should exclude Docker files from image")
        void shouldExcludeDockerFilesFromImage() {
            String result = dockerConfigGenerator.generateDockerignore();

            assertThat(result)
                    .contains("docker-compose*.yml")
                    .contains("Dockerfile*")
                    .contains(".dockerignore");
        }
    }

    @Nested
    @DisplayName("generateDockerReadme()")
    class GenerateDockerReadmeTests {

        @Test
        @DisplayName("Should include artifact ID in title")
        void shouldIncludeArtifactIdInTitle() {
            GenerateRequest.ProjectConfig config = createDefaultConfig();

            String result = dockerConfigGenerator.generateDockerReadme(config);

            assertThat(result).contains("# Docker Guide for my-api");
        }

        @Test
        @DisplayName("Should include quick start instructions")
        void shouldIncludeQuickStartInstructions() {
            GenerateRequest.ProjectConfig config = createDefaultConfig();

            String result = dockerConfigGenerator.generateDockerReadme(config);

            assertThat(result)
                    .contains("./gradlew bootJar -x test")
                    .contains("docker-compose up -d --build")
                    .contains("docker-compose logs -f")
                    .contains("docker-compose down");
        }

        @Test
        @DisplayName("Should include manual build instructions")
        void shouldIncludeManualBuildInstructions() {
            GenerateRequest.ProjectConfig config = createDefaultConfig();

            String result = dockerConfigGenerator.generateDockerReadme(config);

            assertThat(result).contains("docker build -t my-api:latest .");
        }

        @Test
        @DisplayName("Should include environment variables table")
        void shouldIncludeEnvironmentVariablesTable() {
            GenerateRequest.ProjectConfig config = createDefaultConfig();

            String result = dockerConfigGenerator.generateDockerReadme(config);

            assertThat(result)
                    .contains("| Variable | Description | Default |")
                    .contains("SPRING_PROFILES_ACTIVE")
                    .contains("SPRING_DATASOURCE_URL")
                    .contains("SPRING_DATASOURCE_USERNAME")
                    .contains("SPRING_DATASOURCE_PASSWORD");
        }

        @Test
        @DisplayName("Should include database configuration section")
        void shouldIncludeDatabaseConfigurationSection() {
            GenerateRequest.ProjectConfig config = createDefaultConfig();

            String result = dockerConfigGenerator.generateDockerReadme(config);

            assertThat(result)
                    .contains("Database Configuration")
                    .contains("POSTGRESQL")
                    .contains("5432");
        }

        @Test
        @DisplayName("Should include health check endpoints")
        void shouldIncludeHealthCheckEndpoints() {
            GenerateRequest.ProjectConfig config = createDefaultConfig();

            String result = dockerConfigGenerator.generateDockerReadme(config);

            assertThat(result)
                    .contains("/actuator/health")
                    .contains("/actuator/health/liveness")
                    .contains("/actuator/health/readiness");
        }

        @Test
        @DisplayName("Should include API access URLs")
        void shouldIncludeApiAccessUrls() {
            GenerateRequest.ProjectConfig config = createDefaultConfig();

            String result = dockerConfigGenerator.generateDockerReadme(config);

            assertThat(result)
                    .contains("http://localhost:8080/api/v1")
                    .contains("http://localhost:8080/swagger-ui.html");
        }

        @Test
        @DisplayName("Should include production considerations")
        void shouldIncludeProductionConsiderations() {
            GenerateRequest.ProjectConfig config = createDefaultConfig();

            String result = dockerConfigGenerator.generateDockerReadme(config);

            assertThat(result)
                    .contains("Production Considerations")
                    .contains("Change default passwords")
                    .contains("Docker secrets")
                    .contains("resource limits");
        }

        @Test
        @DisplayName("Should include troubleshooting section")
        void shouldIncludeTroubleshootingSection() {
            GenerateRequest.ProjectConfig config = createDefaultConfig();

            String result = dockerConfigGenerator.generateDockerReadme(config);

            assertThat(result)
                    .contains("Troubleshooting")
                    .contains("docker-compose logs")
                    .contains("docker-compose ps")
                    .contains("docker-compose restart");
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

    private GenerateRequest.DatabaseConfig createDefaultDbConfig() {
        return GenerateRequest.DatabaseConfig.builder()
                .name("appdb")
                .username("appuser")
                .password("apppass")
                .build();
    }
}
