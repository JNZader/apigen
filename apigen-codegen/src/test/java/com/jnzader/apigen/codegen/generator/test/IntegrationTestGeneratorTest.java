package com.jnzader.apigen.codegen.generator.test;

import static org.assertj.core.api.Assertions.assertThat;

import com.jnzader.apigen.codegen.model.SqlColumn;
import com.jnzader.apigen.codegen.model.SqlTable;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("IntegrationTestGenerator Tests")
class IntegrationTestGeneratorTest {

    private IntegrationTestGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new IntegrationTestGenerator("com.example");
    }

    @Nested
    @DisplayName("generate()")
    class GenerateTests {

        @Test
        @DisplayName("Should generate test class with correct package")
        void shouldGenerateTestClassWithCorrectPackage() {
            SqlTable table = createSimpleTable("products");

            String result = generator.generate(table);

            assertThat(result).contains("package com.example.products;");
        }

        @Test
        @DisplayName("Should generate correct imports")
        void shouldGenerateCorrectImports() {
            SqlTable table = createSimpleTable("products");

            String result = generator.generate(table);

            assertThat(result)
                    .contains("import com.example.products.application.dto.ProductDTO;")
                    .contains("import com.example.products.domain.entity.Product;")
                    .contains(
                            "import"
                                + " com.example.products.infrastructure.repository.ProductRepository;")
                    .contains("import org.springframework.boot.test.context.SpringBootTest;")
                    .contains(
                            "import"
                                + " org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;")
                    .contains("import org.springframework.test.context.ActiveProfiles;");
        }

        @Test
        @DisplayName("Should generate test class with correct annotations")
        void shouldGenerateTestClassWithCorrectAnnotations() {
            SqlTable table = createSimpleTable("products");

            String result = generator.generate(table);

            assertThat(result)
                    .contains("@SpringBootTest")
                    .contains("@AutoConfigureMockMvc")
                    .contains("@ActiveProfiles(\"test\")")
                    .contains("@TestMethodOrder(MethodOrderer.OrderAnnotation.class)");
        }

        @Test
        @DisplayName("Should generate test class with correct name")
        void shouldGenerateTestClassWithCorrectName() {
            SqlTable table = createSimpleTable("products");

            String result = generator.generate(table);

            assertThat(result)
                    .contains("class ProductIntegrationTest")
                    .contains("@DisplayName(\"Product Integration Tests\")");
        }

        @Test
        @DisplayName("Should generate autowired fields")
        void shouldGenerateAutowiredFields() {
            SqlTable table = createSimpleTable("categories");

            String result = generator.generate(table);

            assertThat(result)
                    .contains("@Autowired")
                    .contains("private MockMvc mockMvc;")
                    .contains("private JsonMapper jsonMapper;")
                    .contains("private CategoryRepository repository;");
        }

        @Test
        @DisplayName("Should generate BASE_URL constant")
        void shouldGenerateBaseUrlConstant() {
            SqlTable table = createSimpleTable("orders");

            String result = generator.generate(table);

            assertThat(result)
                    .contains("private static final String BASE_URL = \"/api/v1/orders\";");
        }
    }

    @Nested
    @DisplayName("setUp Method")
    class SetUpMethodTests {

        @Test
        @DisplayName("Should generate setUp with builder")
        void shouldGenerateSetUpWithBuilder() {
            SqlTable table = createSimpleTable("products");

            String result = generator.generate(table);

            assertThat(result)
                    .contains("@BeforeEach")
                    .contains("void setUp()")
                    .contains("testDto = ProductDTO.builder()")
                    .contains(".activo(true)")
                    .contains(".build();");
        }
    }

    @Nested
    @DisplayName("CRUD Test Methods")
    class CrudTestMethodsTests {

        @Test
        @DisplayName("Should generate create test (Order 1)")
        void shouldGenerateCreateTest() {
            SqlTable table = createSimpleTable("products");

            String result = generator.generate(table);

            assertThat(result)
                    .contains("@org.junit.jupiter.api.Order(1)")
                    .contains("@DisplayName(\"1. POST - Should create new Product\")")
                    .contains("void shouldCreateNewProduct()")
                    .contains("mockMvc.perform(post(BASE_URL)")
                    .contains(".andExpect(status().isCreated())");
        }

        @Test
        @DisplayName("Should generate findById test (Order 2)")
        void shouldGenerateFindByIdTest() {
            SqlTable table = createSimpleTable("categories");

            String result = generator.generate(table);

            assertThat(result)
                    .contains("@org.junit.jupiter.api.Order(2)")
                    .contains("@DisplayName(\"2. GET /{id} - Should find Category by ID\")")
                    .contains("void shouldFindCategoryById()");
        }

        @Test
        @DisplayName("Should generate list all test (Order 3)")
        void shouldGenerateListAllTest() {
            SqlTable table = createSimpleTable("orders");

            String result = generator.generate(table);

            assertThat(result)
                    .contains("@org.junit.jupiter.api.Order(3)")
                    .contains("@DisplayName(\"3. GET - Should list all Order with pagination\")")
                    .contains(".param(\"page\", \"0\")")
                    .contains(".param(\"size\", \"10\")");
        }

        @Test
        @DisplayName("Should generate exists test (Order 4)")
        void shouldGenerateExistsTest() {
            SqlTable table = createSimpleTable("products");

            String result = generator.generate(table);

            assertThat(result)
                    .contains("@org.junit.jupiter.api.Order(4)")
                    .contains("@DisplayName(\"4. HEAD /{id} - Should check Product exists\")")
                    .contains("mockMvc.perform(head(BASE_URL + \"/\" + createdId))");
        }

        @Test
        @DisplayName("Should generate count test (Order 5)")
        void shouldGenerateCountTest() {
            SqlTable table = createSimpleTable("products");

            String result = generator.generate(table);

            assertThat(result)
                    .contains("@org.junit.jupiter.api.Order(5)")
                    .contains("@DisplayName(\"5. HEAD - Should return count in header\")")
                    .contains(".andExpect(header().exists(\"X-Total-Count\"))");
        }

        @Test
        @DisplayName("Should generate update test (Order 6)")
        void shouldGenerateUpdateTest() {
            SqlTable table = createSimpleTable("categories");

            String result = generator.generate(table);

            assertThat(result)
                    .contains("@org.junit.jupiter.api.Order(6)")
                    .contains("@DisplayName(\"6. PUT /{id} - Should update Category\")")
                    .contains("void shouldUpdateCategory()")
                    .contains("mockMvc.perform(put(BASE_URL + \"/\" + createdId)");
        }

        @Test
        @DisplayName("Should generate partial update test (Order 7) with valid JSON")
        void shouldGeneratePartialUpdateTest() {
            SqlTable table = createSimpleTable("products");

            String result = generator.generate(table);

            assertThat(result)
                    .contains("@org.junit.jupiter.api.Order(7)")
                    .contains("@DisplayName(\"7. PATCH /{id} - Should partial update Product\")")
                    .contains("void shouldPartialUpdateProduct()")
                    .contains("mockMvc.perform(patch(BASE_URL + \"/\" + createdId)")
                    // Verify proper JSON escaping - output should have escaped quotes for valid
                    // Java
                    .contains("{\\\"activo\\\": true}");
        }

        @Test
        @DisplayName("Should generate soft delete test (Order 8)")
        void shouldGenerateSoftDeleteTest() {
            SqlTable table = createSimpleTable("orders");

            String result = generator.generate(table);

            assertThat(result)
                    .contains("@org.junit.jupiter.api.Order(8)")
                    .contains("@DisplayName(\"8. DELETE /{id} - Should soft delete Order\")")
                    .contains("void shouldSoftDeleteOrder()")
                    .contains("mockMvc.perform(delete(BASE_URL + \"/\" + createdId))");
        }

        @Test
        @DisplayName("Should generate restore test (Order 9)")
        void shouldGenerateRestoreTest() {
            SqlTable table = createSimpleTable("products");

            String result = generator.generate(table);

            assertThat(result)
                    .contains("@org.junit.jupiter.api.Order(9)")
                    .contains(
                            "@DisplayName(\"9. POST /{id}/restore - Should restore soft-deleted"
                                    + " Product\")")
                    .contains("void shouldRestoreProduct()")
                    .contains("mockMvc.perform(post(BASE_URL + \"/\" + createdId + \"/restore\"))");
        }

        @Test
        @DisplayName("Should generate hard delete test (Order 10)")
        void shouldGenerateHardDeleteTest() {
            SqlTable table = createSimpleTable("categories");

            String result = generator.generate(table);

            assertThat(result)
                    .contains("@org.junit.jupiter.api.Order(10)")
                    .contains(
                            "@DisplayName(\"10. DELETE /{id}?permanent=true - Should permanently"
                                    + " delete Category\")")
                    .contains("void shouldPermanentlyDeleteCategory()")
                    .contains(".param(\"permanent\", \"true\")")
                    .contains("assertThat(repository.findById(createdId)).isEmpty();");
        }

        @Test
        @DisplayName("Should generate 404 test (Order 11)")
        void shouldGenerate404Test() {
            SqlTable table = createSimpleTable("products");

            String result = generator.generate(table);

            assertThat(result)
                    .contains("@org.junit.jupiter.api.Order(11)")
                    .contains(
                            "@DisplayName(\"11. GET /{id} - Should return 404 for non-existent"
                                    + " Product\")")
                    .contains(".andExpect(status().isNotFound())");
        }

        @Test
        @DisplayName("Should generate validation test (Order 12)")
        void shouldGenerateValidationTest() {
            SqlTable table = createSimpleTable("orders");

            String result = generator.generate(table);

            assertThat(result)
                    .contains("@org.junit.jupiter.api.Order(12)")
                    .contains("@DisplayName(\"12. POST - Should validate required fields\")")
                    .contains("OrderDTO invalidDto = OrderDTO.builder().build();")
                    .contains(".andExpect(status().isBadRequest())");
        }

        @Test
        @DisplayName("Should generate cursor pagination test (Order 13)")
        void shouldGenerateCursorPaginationTest() {
            SqlTable table = createSimpleTable("products");

            String result = generator.generate(table);

            assertThat(result)
                    .contains("@org.junit.jupiter.api.Order(13)")
                    .contains(
                            "@DisplayName(\"13. GET /cursor - Should support cursor pagination\")")
                    .contains("mockMvc.perform(get(BASE_URL + \"/cursor\")")
                    .contains(".andExpect(jsonPath(\"$.pageInfo.hasNext\").isBoolean())");
        }

        @Test
        @DisplayName("Should generate cursor pagination test with unique DTOs for each entity")
        void shouldGenerateCursorPaginationWithUniqueDtos() {
            SqlTable table = createTableWithBusinessColumns();

            String result = generator.generate(table);

            // Verify unique DTOs are created inside the loop with index suffix for strings
            assertThat(result)
                    .contains("for (int i = 0; i < 3; i++)")
                    .contains("ProductDTO uniqueDto = ProductDTO.builder()")
                    // String fields should have index appended for uniqueness
                    .contains(".name(\"Test name\" + \" \" + i)");
        }

        @Test
        @DisplayName("Should generate filter test (Order 14)")
        void shouldGenerateFilterTest() {
            SqlTable table = createSimpleTable("products");

            String result = generator.generate(table);

            assertThat(result)
                    .contains("@org.junit.jupiter.api.Order(14)")
                    .contains("@DisplayName(\"14. GET with filter - Should filter results\")")
                    .contains(".param(\"filter\", \"id:gt:0\")");
        }
    }

    @Nested
    @DisplayName("Field Assignments")
    class FieldAssignmentsTests {

        @Test
        @DisplayName("Should generate field assignments for business columns")
        void shouldGenerateFieldAssignmentsForBusinessColumns() {
            SqlTable table = createTableWithBusinessColumns();

            String result = generator.generate(table);

            // TestValueProvider uses "Test " + fieldName (lowercase fieldName)
            assertThat(result).contains(".name(\"Test name\")").contains(".price(99.99)");
        }

        @Test
        @DisplayName("Should generate update assignments for business columns")
        void shouldGenerateUpdateAssignmentsForBusinessColumns() {
            SqlTable table = createTableWithBusinessColumns();

            String result = generator.generate(table);

            // TestValueProvider uses "Updated " + fieldName (lowercase fieldName)
            assertThat(result)
                    .contains("dto.setName(\"Updated name\");")
                    .contains("dto.setPrice(99.99);");
        }
    }

    @Nested
    @DisplayName("Placeholder Validation")
    class PlaceholderValidationTests {

        @Test
        @DisplayName("Should not have unformatted placeholders")
        void shouldNotHaveUnformattedPlaceholders() {
            SqlTable table = createSimpleTable("test_entities");

            String result = generator.generate(table);

            assertThat(result).doesNotContain("%s");
        }

        @Test
        @DisplayName("Should generate valid Java code for various table names")
        void shouldGenerateValidJavaCodeForVariousTableNames() {
            List<String> tableNames =
                    List.of("products", "categories", "order_items", "user_profiles");

            for (String tableName : tableNames) {
                SqlTable table = createSimpleTable(tableName);
                String result = generator.generate(table);

                assertThat(result).as("Table: " + tableName).doesNotContain("%s");

                assertThat(result)
                        .as("Table: " + tableName)
                        .contains("@SpringBootTest")
                        .contains("@org.junit.jupiter.api.Order(1)")
                        .contains("MockMvc");
            }
        }
    }

    @Nested
    @DisplayName("Entity Name Consistency")
    class EntityNameConsistencyTests {

        @Test
        @DisplayName("Should use correct entity name in all method names")
        void shouldUseCorrectEntityNameInAllMethodNames() {
            SqlTable table = createSimpleTable("order_items");

            String result = generator.generate(table);

            assertThat(result)
                    .contains("shouldCreateNewOrderItem()")
                    .contains("shouldFindOrderItemById()")
                    .contains("shouldUpdateOrderItem()")
                    .contains("shouldPartialUpdateOrderItem()")
                    .contains("shouldSoftDeleteOrderItem()")
                    .contains("shouldRestoreOrderItem()")
                    .contains("shouldPermanentlyDeleteOrderItem()");
        }

        @Test
        @DisplayName("Should use correct DTO class name")
        void shouldUseCorrectDtoClassName() {
            SqlTable table = createSimpleTable("user_profiles");

            String result = generator.generate(table);

            assertThat(result)
                    .contains("UserProfileDTO")
                    .contains("testDto = UserProfileDTO.builder()");
        }
    }

    private SqlTable createSimpleTable(String tableName) {
        return SqlTable.builder()
                .name(tableName)
                .columns(
                        List.of(
                                SqlColumn.builder()
                                        .name("id")
                                        .javaType("Long")
                                        .primaryKey(true)
                                        .build()))
                .foreignKeys(new ArrayList<>())
                .indexes(new ArrayList<>())
                .build();
    }

    private SqlTable createTableWithBusinessColumns() {
        return SqlTable.builder()
                .name("products")
                .columns(
                        List.of(
                                SqlColumn.builder()
                                        .name("id")
                                        .javaType("Long")
                                        .primaryKey(true)
                                        .build(),
                                SqlColumn.builder()
                                        .name("name")
                                        .javaType("String")
                                        .nullable(false)
                                        .build(),
                                SqlColumn.builder()
                                        .name("price")
                                        .javaType("Double")
                                        .nullable(false)
                                        .build()))
                .foreignKeys(new ArrayList<>())
                .indexes(new ArrayList<>())
                .build();
    }
}
