package com.jnzader.apigen.server.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.jnzader.apigen.server.dto.GenerateRequest;
import com.jnzader.apigen.server.dto.GenerateResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("GeneratorService Tests")
@SuppressWarnings(
        "java:S5976") // Tests validate different specific generation scenarios, not the same
// feature with different inputs
class GeneratorServiceTest {

    private GeneratorService generatorService;

    @BeforeEach
    void setUp() {
        generatorService = new GeneratorService();
    }

    @Nested
    @DisplayName("validate()")
    class ValidateTests {

        @Test
        @DisplayName("Should validate valid SQL schema")
        void shouldValidateValidSqlSchema() {
            String sql =
                    """
                    CREATE TABLE products (
                        id BIGINT PRIMARY KEY,
                        name VARCHAR(255) NOT NULL,
                        price DECIMAL(10,2)
                    );
                    """;

            GenerateRequest request = createRequest(sql);
            GenerateResponse response = generatorService.validate(request);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getMessage()).contains("valid");
            assertThat(response.getStats()).isNotNull();
            assertThat(response.getStats().getTablesProcessed()).isGreaterThan(0);
        }

        @Test
        @DisplayName("Should return error for empty SQL")
        void shouldReturnErrorForEmptySql() {
            GenerateRequest request = createRequest("");

            GenerateResponse response = generatorService.validate(request);

            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getErrors()).isNotEmpty();
        }

        @Test
        @DisplayName("Should return error when no entity tables found")
        void shouldReturnErrorWhenNoEntityTablesFound() {
            // Only junction tables (2 FKs, no other columns)
            String sql =
                    """
                    CREATE TABLE product_categories (
                        product_id BIGINT REFERENCES products(id),
                        category_id BIGINT REFERENCES categories(id),
                        PRIMARY KEY (product_id, category_id)
                    );
                    """;

            GenerateRequest request = createRequest(sql);
            GenerateResponse response = generatorService.validate(request);

            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getErrors()).anyMatch(e -> e.contains("No valid entity tables"));
        }

        @Test
        @DisplayName("Should validate multiple tables")
        void shouldValidateMultipleTables() {
            String sql =
                    """
                    CREATE TABLE categories (
                        id BIGINT PRIMARY KEY,
                        name VARCHAR(100) NOT NULL
                    );

                    CREATE TABLE products (
                        id BIGINT PRIMARY KEY,
                        name VARCHAR(255) NOT NULL,
                        category_id BIGINT REFERENCES categories(id)
                    );
                    """;

            GenerateRequest request = createRequest(sql);
            GenerateResponse response = generatorService.validate(request);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getStats().getTablesProcessed()).isEqualTo(2);
            assertThat(response.getStats().getEntitiesGenerated()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should return error for invalid SQL syntax")
        void shouldReturnErrorForInvalidSqlSyntax() {
            String sql = "THIS IS NOT VALID SQL";

            GenerateRequest request = createRequest(sql);
            GenerateResponse response = generatorService.validate(request);

            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getErrors()).isNotEmpty();
        }

        @Test
        @DisplayName("Should include validation errors from schema")
        void shouldIncludeSchemaValidationErrors() {
            // Table without primary key
            String sql =
                    """
                    CREATE TABLE products (
                        name VARCHAR(255) NOT NULL
                    );
                    """;

            GenerateRequest request = createRequest(sql);
            GenerateResponse response = generatorService.validate(request);

            assertThat(response.isSuccess()).isFalse();
        }

        @Test
        @DisplayName("Should validate table with various column types")
        void shouldValidateTableWithVariousColumnTypes() {
            String sql =
                    """
                    CREATE TABLE orders (
                        id BIGINT PRIMARY KEY,
                        customer_name VARCHAR(255) NOT NULL,
                        order_date DATE NOT NULL,
                        total DECIMAL(12,2),
                        notes TEXT,
                        is_shipped BOOLEAN DEFAULT FALSE,
                        items_count INTEGER
                    );
                    """;

            GenerateRequest request = createRequest(sql);
            GenerateResponse response = generatorService.validate(request);

            assertThat(response.isSuccess()).isTrue();
        }
    }

    @Nested
    @DisplayName("generateProject()")
    class GenerateProjectTests {

        @Test
        @DisplayName("Should generate ZIP file with project structure")
        void shouldGenerateZipFileWithProjectStructure() throws IOException {
            String sql =
                    """
                    CREATE TABLE products (
                        id BIGINT PRIMARY KEY,
                        name VARCHAR(255) NOT NULL,
                        price DECIMAL(10,2)
                    );
                    """;

            GenerateRequest request = createFullRequest(sql);
            byte[] zipBytes = generatorService.generateProject(request);

            assertThat(zipBytes).isNotNull().hasSizeGreaterThan(0);

            List<String> zipEntries = extractZipEntries(zipBytes);
            assertThat(zipEntries).isNotEmpty();
        }

        @Test
        @DisplayName("Should include build.gradle in generated ZIP")
        void shouldIncludeBuildGradleInGeneratedZip() throws IOException {
            String sql =
                    """
                    CREATE TABLE categories (
                        id BIGINT PRIMARY KEY,
                        name VARCHAR(100) NOT NULL
                    );
                    """;

            GenerateRequest request = createFullRequest(sql);
            byte[] zipBytes = generatorService.generateProject(request);

            List<String> zipEntries = extractZipEntries(zipBytes);
            assertThat(zipEntries).anyMatch(entry -> entry.endsWith("build.gradle"));
        }

        @Test
        @DisplayName("Should include application.yml in generated ZIP")
        void shouldIncludeApplicationYmlInGeneratedZip() throws IOException {
            String sql =
                    """
                    CREATE TABLE users (
                        id BIGINT PRIMARY KEY,
                        username VARCHAR(50) NOT NULL
                    );
                    """;

            GenerateRequest request = createFullRequest(sql);
            byte[] zipBytes = generatorService.generateProject(request);

            List<String> zipEntries = extractZipEntries(zipBytes);
            assertThat(zipEntries).anyMatch(entry -> entry.contains("application.yml"));
        }

        @Test
        @DisplayName("Should include Docker files when docker feature is enabled")
        void shouldIncludeDockerFilesWhenEnabled() throws IOException {
            String sql =
                    """
                    CREATE TABLE items (
                        id BIGINT PRIMARY KEY,
                        title VARCHAR(200) NOT NULL
                    );
                    """;

            GenerateRequest request = createFullRequest(sql);
            request.getProject().getFeatures().setDocker(true);

            byte[] zipBytes = generatorService.generateProject(request);

            List<String> zipEntries = extractZipEntries(zipBytes);
            assertThat(zipEntries)
                    .anyMatch(entry -> entry.endsWith("Dockerfile"))
                    .anyMatch(entry -> entry.endsWith("docker-compose.yml"));
        }

        @Test
        @DisplayName("Should include HTTP test file")
        void shouldIncludeHttpTestFile() throws IOException {
            String sql =
                    """
                    CREATE TABLE orders (
                        id BIGINT PRIMARY KEY,
                        customer_name VARCHAR(255) NOT NULL
                    );
                    """;

            GenerateRequest request = createFullRequest(sql);
            byte[] zipBytes = generatorService.generateProject(request);

            List<String> zipEntries = extractZipEntries(zipBytes);
            assertThat(zipEntries).anyMatch(entry -> entry.endsWith("api-tests.http"));
        }

        @Test
        @DisplayName("Should include Postman collection")
        void shouldIncludePostmanCollection() throws IOException {
            String sql =
                    """
                    CREATE TABLE customers (
                        id BIGINT PRIMARY KEY,
                        email VARCHAR(255) NOT NULL
                    );
                    """;

            GenerateRequest request = createFullRequest(sql);
            byte[] zipBytes = generatorService.generateProject(request);

            List<String> zipEntries = extractZipEntries(zipBytes);
            assertThat(zipEntries).anyMatch(entry -> entry.endsWith("postman-collection.json"));
        }

        @Test
        @DisplayName("Should include README.md")
        void shouldIncludeReadme() throws IOException {
            String sql =
                    """
                    CREATE TABLE articles (
                        id BIGINT PRIMARY KEY,
                        title VARCHAR(255) NOT NULL
                    );
                    """;

            GenerateRequest request = createFullRequest(sql);
            byte[] zipBytes = generatorService.generateProject(request);

            List<String> zipEntries = extractZipEntries(zipBytes);
            assertThat(zipEntries).anyMatch(entry -> entry.endsWith("README.md"));
        }

        @Test
        @DisplayName("Should generate entity classes for each table")
        void shouldGenerateEntityClassesForEachTable() throws IOException {
            String sql =
                    """
                    CREATE TABLE products (
                        id BIGINT PRIMARY KEY,
                        name VARCHAR(255) NOT NULL
                    );

                    CREATE TABLE categories (
                        id BIGINT PRIMARY KEY,
                        name VARCHAR(100) NOT NULL
                    );
                    """;

            GenerateRequest request = createFullRequest(sql);
            byte[] zipBytes = generatorService.generateProject(request);

            List<String> zipEntries = extractZipEntries(zipBytes);
            assertThat(zipEntries)
                    .anyMatch(entry -> entry.contains("Product.java"))
                    .anyMatch(entry -> entry.contains("Category.java"));
        }

        @Test
        @DisplayName("Should generate repository interfaces")
        void shouldGenerateRepositoryInterfaces() throws IOException {
            String sql =
                    """
                    CREATE TABLE products (
                        id BIGINT PRIMARY KEY,
                        name VARCHAR(255) NOT NULL
                    );
                    """;

            GenerateRequest request = createFullRequest(sql);
            byte[] zipBytes = generatorService.generateProject(request);

            List<String> zipEntries = extractZipEntries(zipBytes);
            assertThat(zipEntries).anyMatch(entry -> entry.contains("ProductRepository.java"));
        }

        @Test
        @DisplayName("Should generate service implementations")
        void shouldGenerateServiceImplementations() throws IOException {
            String sql =
                    """
                    CREATE TABLE orders (
                        id BIGINT PRIMARY KEY,
                        total DECIMAL(10,2)
                    );
                    """;

            GenerateRequest request = createFullRequest(sql);
            byte[] zipBytes = generatorService.generateProject(request);

            List<String> zipEntries = extractZipEntries(zipBytes);
            assertThat(zipEntries).anyMatch(entry -> entry.contains("OrderServiceImpl.java"));
        }

        @Test
        @DisplayName("Should generate controller implementations")
        void shouldGenerateControllerImplementations() throws IOException {
            String sql =
                    """
                    CREATE TABLE items (
                        id BIGINT PRIMARY KEY,
                        name VARCHAR(100)
                    );
                    """;

            GenerateRequest request = createFullRequest(sql);
            byte[] zipBytes = generatorService.generateProject(request);

            List<String> zipEntries = extractZipEntries(zipBytes);
            assertThat(zipEntries).anyMatch(entry -> entry.contains("ItemControllerImpl.java"));
        }

        @Test
        @DisplayName("Should generate test classes")
        void shouldGenerateTestClasses() throws IOException {
            String sql =
                    """
                    CREATE TABLE products (
                        id BIGINT PRIMARY KEY,
                        name VARCHAR(255) NOT NULL
                    );
                    """;

            GenerateRequest request = createFullRequest(sql);
            byte[] zipBytes = generatorService.generateProject(request);

            List<String> zipEntries = extractZipEntries(zipBytes);
            assertThat(zipEntries).anyMatch(entry -> entry.contains("Test.java"));
        }

        @Test
        @DisplayName("Should include .gitignore")
        void shouldIncludeGitignore() throws IOException {
            String sql =
                    """
                    CREATE TABLE data (
                        id BIGINT PRIMARY KEY,
                        value TEXT
                    );
                    """;

            GenerateRequest request = createFullRequest(sql);
            byte[] zipBytes = generatorService.generateProject(request);

            List<String> zipEntries = extractZipEntries(zipBytes);
            assertThat(zipEntries).anyMatch(entry -> entry.endsWith(".gitignore"));
        }

        @Test
        @DisplayName("Should handle tables with foreign keys")
        void shouldHandleTablesWithForeignKeys() throws IOException {
            String sql =
                    """
                    CREATE TABLE categories (
                        id BIGINT PRIMARY KEY,
                        name VARCHAR(100) NOT NULL
                    );

                    CREATE TABLE products (
                        id BIGINT PRIMARY KEY,
                        name VARCHAR(255) NOT NULL,
                        category_id BIGINT REFERENCES categories(id)
                    );
                    """;

            GenerateRequest request = createFullRequest(sql);
            byte[] zipBytes = generatorService.generateProject(request);

            assertThat(zipBytes).isNotNull().hasSizeGreaterThan(0);
        }

        private List<String> extractZipEntries(byte[] zipBytes) throws IOException {
            List<String> entries = new ArrayList<>();
            try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    entries.add(entry.getName());
                    zis.closeEntry();
                }
            }
            return entries;
        }

        private GenerateRequest createFullRequest(String sql) {
            return GenerateRequest.builder()
                    .project(
                            GenerateRequest.ProjectConfig.builder()
                                    .name("Test Project")
                                    .groupId("com.example")
                                    .artifactId("test-api")
                                    .features(
                                            GenerateRequest.FeaturesConfig.builder()
                                                    .docker(true)
                                                    .swagger(true)
                                                    .hateoas(true)
                                                    .auditing(true)
                                                    .softDelete(true)
                                                    .caching(true)
                                                    .build())
                                    .database(
                                            GenerateRequest.DatabaseConfig.builder()
                                                    .type("postgresql")
                                                    .name("testdb")
                                                    .username("testuser")
                                                    .password("testpass")
                                                    .build())
                                    .build())
                    .sql(sql)
                    .build();
        }
    }

    private GenerateRequest createRequest(String sql) {
        return GenerateRequest.builder()
                .project(
                        GenerateRequest.ProjectConfig.builder()
                                .name("Test Project")
                                .groupId("com.example")
                                .artifactId("test-api")
                                .build())
                .sql(sql)
                .build();
    }
}
