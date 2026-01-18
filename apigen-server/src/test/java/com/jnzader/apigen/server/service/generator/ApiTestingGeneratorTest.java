package com.jnzader.apigen.server.service.generator;

import com.jnzader.apigen.codegen.model.SqlColumn;
import com.jnzader.apigen.codegen.model.SqlSchema;
import com.jnzader.apigen.codegen.model.SqlTable;
import com.jnzader.apigen.server.config.GeneratedProjectVersions;
import com.jnzader.apigen.server.dto.GenerateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ApiTestingGenerator Tests")
@SuppressWarnings("java:S5976") // Tests validate different specific HTTP endpoints, not the same feature with different inputs
class ApiTestingGeneratorTest {

    private ApiTestingGenerator apiTestingGenerator;

    @BeforeEach
    void setUp() {
        apiTestingGenerator = new ApiTestingGenerator();
    }

    @Nested
    @DisplayName("generateHttpTestFile()")
    class GenerateHttpTestFileTests {

        @Test
        @DisplayName("Should include header comment with usage instructions")
        void shouldIncludeHeaderCommentWithUsageInstructions() {
            SqlSchema schema = createSchemaWithTable("products");

            String result = apiTestingGenerator.generateHttpTestFile(schema);

            assertThat(result)
                    .contains("API Test Collection")
                    .contains("APiGen Studio")
                    .contains("VS Code")
                    .contains("IntelliJ IDEA");
        }

        @Test
        @DisplayName("Should include baseUrl variable")
        void shouldIncludeBaseUrlVariable() {
            SqlSchema schema = createSchemaWithTable("products");

            String result = apiTestingGenerator.generateHttpTestFile(schema);

            assertThat(result).contains("@baseUrl = http://localhost:8080/api/v1");
        }

        @Test
        @DisplayName("Should generate section header for each entity")
        void shouldGenerateSectionHeaderForEachEntity() {
            SqlSchema schema = createSchemaWithTable("products");

            String result = apiTestingGenerator.generateHttpTestFile(schema);

            assertThat(result)
                    .contains("PRODUCT ENDPOINTS")
                    .contains("(products)");
        }

        @Test
        @DisplayName("Should generate GET list endpoint with pagination")
        void shouldGenerateGetListEndpointWithPagination() {
            SqlSchema schema = createSchemaWithTable("products");

            String result = apiTestingGenerator.generateHttpTestFile(schema);

            assertThat(result)
                    .contains("### 1. List all products (paginated)")
                    .contains("GET {{baseUrl}}/products?page=0&size=10&sort=id,desc");
        }

        @Test
        @DisplayName("Should generate GET list with filter")
        void shouldGenerateGetListWithFilter() {
            SqlSchema schema = createSchemaWithTable("products");

            String result = apiTestingGenerator.generateHttpTestFile(schema);

            assertThat(result)
                    .contains("### 2. List products with filter")
                    .contains("filter=id:gt:0");
        }

        @Test
        @DisplayName("Should generate GET list with sparse fieldsets")
        void shouldGenerateGetListWithSparseFieldsets() {
            SqlSchema schema = createSchemaWithTable("products");

            String result = apiTestingGenerator.generateHttpTestFile(schema);

            assertThat(result)
                    .contains("### 3. List products with sparse fieldsets")
                    .contains("fields=id");
        }

        @Test
        @DisplayName("Should generate HEAD count endpoint")
        void shouldGenerateHeadCountEndpoint() {
            SqlSchema schema = createSchemaWithTable("products");

            String result = apiTestingGenerator.generateHttpTestFile(schema);

            assertThat(result)
                    .contains("### 4. Count products")
                    .contains("HEAD {{baseUrl}}/products");
        }

        @Test
        @DisplayName("Should generate GET by ID endpoint")
        void shouldGenerateGetByIdEndpoint() {
            SqlSchema schema = createSchemaWithTable("products");

            String result = apiTestingGenerator.generateHttpTestFile(schema);

            assertThat(result)
                    .contains("### 5. Get product by ID")
                    .contains("GET {{baseUrl}}/products/1");
        }

        @Test
        @DisplayName("Should generate HEAD exists check endpoint")
        void shouldGenerateHeadExistsCheckEndpoint() {
            SqlSchema schema = createSchemaWithTable("products");

            String result = apiTestingGenerator.generateHttpTestFile(schema);

            assertThat(result)
                    .contains("### 6. Check if product exists")
                    .contains("HEAD {{baseUrl}}/products/1");
        }

        @Test
        @DisplayName("Should generate POST create endpoint with JSON body")
        void shouldGeneratePostCreateEndpointWithJsonBody() {
            SqlSchema schema = createSchemaWithTable("products");

            String result = apiTestingGenerator.generateHttpTestFile(schema);

            assertThat(result)
                    .contains("### 7. Create new product")
                    .contains("POST {{baseUrl}}/products")
                    .contains("Content-Type: application/json")
                    .contains("{");
        }

        @Test
        @DisplayName("Should generate PUT full update endpoint")
        void shouldGeneratePutFullUpdateEndpoint() {
            SqlSchema schema = createSchemaWithTable("products");

            String result = apiTestingGenerator.generateHttpTestFile(schema);

            assertThat(result)
                    .contains("### 8. Update product (full)")
                    .contains("PUT {{baseUrl}}/products/1")
                    .contains("\"id\": 1");
        }

        @Test
        @DisplayName("Should generate PATCH partial update endpoint")
        void shouldGeneratePatchPartialUpdateEndpoint() {
            SqlSchema schema = createSchemaWithTable("products");

            String result = apiTestingGenerator.generateHttpTestFile(schema);

            assertThat(result)
                    .contains("### 9. Update product (partial)")
                    .contains("PATCH {{baseUrl}}/products/1");
        }

        @Test
        @DisplayName("Should generate DELETE soft delete endpoint")
        void shouldGenerateDeleteSoftDeleteEndpoint() {
            SqlSchema schema = createSchemaWithTable("products");

            String result = apiTestingGenerator.generateHttpTestFile(schema);

            assertThat(result)
                    .contains("### 10. Soft delete product")
                    .contains("DELETE {{baseUrl}}/products/1");
        }

        @Test
        @DisplayName("Should generate POST restore endpoint")
        void shouldGeneratePostRestoreEndpoint() {
            SqlSchema schema = createSchemaWithTable("products");

            String result = apiTestingGenerator.generateHttpTestFile(schema);

            assertThat(result)
                    .contains("### 11. Restore soft-deleted product")
                    .contains("POST {{baseUrl}}/products/1/restore");
        }

        @Test
        @DisplayName("Should generate DELETE permanent endpoint")
        void shouldGenerateDeletePermanentEndpoint() {
            SqlSchema schema = createSchemaWithTable("products");

            String result = apiTestingGenerator.generateHttpTestFile(schema);

            assertThat(result)
                    .contains("### 12. Permanently delete product")
                    .contains("DELETE {{baseUrl}}/products/1?permanent=true");
        }

        @Test
        @DisplayName("Should generate cursor pagination endpoint")
        void shouldGenerateCursorPaginationEndpoint() {
            SqlSchema schema = createSchemaWithTable("products");

            String result = apiTestingGenerator.generateHttpTestFile(schema);

            assertThat(result)
                    .contains("### BONUS: Cursor-based pagination")
                    .contains("{{baseUrl}}/products/cursor?size=10&sort=id&direction=DESC");
        }

        @Test
        @DisplayName("Should generate endpoints for multiple entities")
        void shouldGenerateEndpointsForMultipleEntities() {
            SqlSchema schema = createSchemaWithTables("products", "categories");

            String result = apiTestingGenerator.generateHttpTestFile(schema);

            assertThat(result)
                    .contains("PRODUCT ENDPOINTS")
                    .contains("CATEGORY ENDPOINTS")
                    .contains("{{baseUrl}}/products")
                    .contains("{{baseUrl}}/categories");
        }
    }

    @Nested
    @DisplayName("generatePostmanCollection()")
    class GeneratePostmanCollectionTests {

        @Test
        @DisplayName("Should generate valid Postman collection structure")
        void shouldGenerateValidPostmanCollectionStructure() {
            SqlSchema schema = createSchemaWithTable("products");
            GenerateRequest.ProjectConfig config = createDefaultConfig();

            String result = apiTestingGenerator.generatePostmanCollection(schema, config);

            assertThat(result)
                    .contains("\"info\":")
                    .contains("\"name\":")
                    .contains("\"schema\": \"https://schema.getpostman.com/json/collection/v2.1.0/collection.json\"");
        }

        @Test
        @DisplayName("Should include project name in collection name")
        void shouldIncludeProjectNameInCollectionName() {
            SqlSchema schema = createSchemaWithTable("products");
            GenerateRequest.ProjectConfig config = createDefaultConfig();
            config.setName("my-awesome-api");

            String result = apiTestingGenerator.generatePostmanCollection(schema, config);

            assertThat(result).contains("My-awesome-api API Collection");
        }

        @Test
        @DisplayName("Should include baseUrl variable")
        void shouldIncludeBaseUrlVariable() {
            SqlSchema schema = createSchemaWithTable("products");
            GenerateRequest.ProjectConfig config = createDefaultConfig();

            String result = apiTestingGenerator.generatePostmanCollection(schema, config);

            assertThat(result)
                    .contains("\"variable\":")
                    .contains("\"key\": \"baseUrl\"")
                    .contains("\"value\": \"http://localhost:8080/api/v1\"");
        }

        @Test
        @DisplayName("Should create folder for each entity")
        void shouldCreateFolderForEachEntity() {
            SqlSchema schema = createSchemaWithTable("products");
            GenerateRequest.ProjectConfig config = createDefaultConfig();

            String result = apiTestingGenerator.generatePostmanCollection(schema, config);

            assertThat(result)
                    .contains("\"item\":")
                    .contains("\"name\": \"Product\"");
        }

        @Test
        @DisplayName("Should include list request")
        void shouldIncludeListRequest() {
            SqlSchema schema = createSchemaWithTable("products");
            GenerateRequest.ProjectConfig config = createDefaultConfig();

            String result = apiTestingGenerator.generatePostmanCollection(schema, config);

            assertThat(result)
                    .contains("\"name\": \"List all products\"")
                    .contains("\"method\": \"GET\"");
        }

        @Test
        @DisplayName("Should include get by ID request")
        void shouldIncludeGetByIdRequest() {
            SqlSchema schema = createSchemaWithTable("products");
            GenerateRequest.ProjectConfig config = createDefaultConfig();

            String result = apiTestingGenerator.generatePostmanCollection(schema, config);

            assertThat(result).contains("\"name\": \"Get product by ID\"");
        }

        @Test
        @DisplayName("Should include create request with body")
        void shouldIncludeCreateRequestWithBody() {
            SqlSchema schema = createSchemaWithTable("products");
            GenerateRequest.ProjectConfig config = createDefaultConfig();

            String result = apiTestingGenerator.generatePostmanCollection(schema, config);

            assertThat(result)
                    .contains("\"name\": \"Create new product\"")
                    .contains("\"method\": \"POST\"")
                    .contains("\"body\":")
                    .contains("\"mode\": \"raw\"");
        }

        @Test
        @DisplayName("Should include update request")
        void shouldIncludeUpdateRequest() {
            SqlSchema schema = createSchemaWithTable("products");
            GenerateRequest.ProjectConfig config = createDefaultConfig();

            String result = apiTestingGenerator.generatePostmanCollection(schema, config);

            assertThat(result)
                    .contains("\"name\": \"Update product (full)\"")
                    .contains("\"method\": \"PUT\"");
        }

        @Test
        @DisplayName("Should include partial update request")
        void shouldIncludePartialUpdateRequest() {
            SqlSchema schema = createSchemaWithTable("products");
            GenerateRequest.ProjectConfig config = createDefaultConfig();

            String result = apiTestingGenerator.generatePostmanCollection(schema, config);

            assertThat(result)
                    .contains("\"name\": \"Update product (partial)\"")
                    .contains("\"method\": \"PATCH\"");
        }

        @Test
        @DisplayName("Should include soft delete request")
        void shouldIncludeSoftDeleteRequest() {
            SqlSchema schema = createSchemaWithTable("products");
            GenerateRequest.ProjectConfig config = createDefaultConfig();

            String result = apiTestingGenerator.generatePostmanCollection(schema, config);

            assertThat(result)
                    .contains("\"name\": \"Soft delete product\"")
                    .contains("\"method\": \"DELETE\"");
        }

        @Test
        @DisplayName("Should include restore request")
        void shouldIncludeRestoreRequest() {
            SqlSchema schema = createSchemaWithTable("products");
            GenerateRequest.ProjectConfig config = createDefaultConfig();

            String result = apiTestingGenerator.generatePostmanCollection(schema, config);

            assertThat(result).contains("\"name\": \"Restore product\"");
        }

        @Test
        @DisplayName("Should include permanent delete request")
        void shouldIncludePermanentDeleteRequest() {
            SqlSchema schema = createSchemaWithTable("products");
            GenerateRequest.ProjectConfig config = createDefaultConfig();

            String result = apiTestingGenerator.generatePostmanCollection(schema, config);

            assertThat(result)
                    .contains("\"name\": \"Permanently delete product\"")
                    .contains("permanent=true");
        }

        @Test
        @DisplayName("Should include filter request")
        void shouldIncludeFilterRequest() {
            SqlSchema schema = createSchemaWithTable("products");
            GenerateRequest.ProjectConfig config = createDefaultConfig();

            String result = apiTestingGenerator.generatePostmanCollection(schema, config);

            assertThat(result).contains("\"name\": \"List products with filter\"");
        }

        @Test
        @DisplayName("Should include cursor pagination request")
        void shouldIncludeCursorPaginationRequest() {
            SqlSchema schema = createSchemaWithTable("products");
            GenerateRequest.ProjectConfig config = createDefaultConfig();

            String result = apiTestingGenerator.generatePostmanCollection(schema, config);

            assertThat(result).contains("\"name\": \"Cursor pagination\"");
        }

        @Test
        @DisplayName("Should include Content-Type header for POST requests")
        void shouldIncludeContentTypeHeaderForPostRequests() {
            SqlSchema schema = createSchemaWithTable("products");
            GenerateRequest.ProjectConfig config = createDefaultConfig();

            String result = apiTestingGenerator.generatePostmanCollection(schema, config);

            assertThat(result)
                    .contains("\"key\": \"Content-Type\"")
                    .contains("\"value\": \"application/json\"");
        }

        @Test
        @DisplayName("Should include descriptions for requests")
        void shouldIncludeDescriptionsForRequests() {
            SqlSchema schema = createSchemaWithTable("products");
            GenerateRequest.ProjectConfig config = createDefaultConfig();

            String result = apiTestingGenerator.generatePostmanCollection(schema, config);

            assertThat(result)
                    .contains("\"description\":")
                    .contains("Returns paginated list");
        }
    }

    private SqlSchema createSchemaWithTable(String tableName) {
        SqlTable table = SqlTable.builder()
                .name(tableName)
                .columns(List.of(
                        SqlColumn.builder().name("id").javaType("Long").primaryKey(true).build(),
                        SqlColumn.builder().name("title").javaType("String").nullable(false).build(),
                        SqlColumn.builder().name("price").javaType("BigDecimal").nullable(false).build()
                ))
                .foreignKeys(new ArrayList<>())
                .indexes(new ArrayList<>())
                .build();

        return SqlSchema.builder()
                .tables(List.of(table))
                .functions(new ArrayList<>())
                .build();
    }

    private SqlSchema createSchemaWithTables(String... tableNames) {
        List<SqlTable> tables = new ArrayList<>();
        for (String tableName : tableNames) {
            tables.add(SqlTable.builder()
                    .name(tableName)
                    .columns(List.of(
                            SqlColumn.builder().name("id").javaType("Long").primaryKey(true).build(),
                            SqlColumn.builder().name("name").javaType("String").nullable(false).build()
                    ))
                    .foreignKeys(new ArrayList<>())
                    .indexes(new ArrayList<>())
                    .build());
        }

        return SqlSchema.builder()
                .tables(tables)
                .functions(new ArrayList<>())
                .build();
    }

    private GenerateRequest.ProjectConfig createDefaultConfig() {
        return GenerateRequest.ProjectConfig.builder()
                .name("My API")
                .groupId("com.example")
                .artifactId("my-api")
                .javaVersion(GeneratedProjectVersions.JAVA_VERSION)
                .springBootVersion(GeneratedProjectVersions.SPRING_BOOT_VERSION)
                .build();
    }
}
