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

@DisplayName("TestGenerator Facade Tests")
@SuppressWarnings({
    "java:S1874",
    "deprecation"
}) // Tests deprecated generators for backward compatibility
class TestGeneratorTest {

    private TestGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new TestGenerator("com.example");
    }

    @Nested
    @DisplayName("Constructor")
    class ConstructorTests {

        @Test
        @DisplayName("Should create generator with base package")
        void shouldCreateGeneratorWithBasePackage() {
            TestGenerator gen = new TestGenerator("com.test");

            SqlTable table = createSimpleTable("products");
            String result = gen.generateServiceTest(table);

            assertThat(result).contains("package com.test.products");
        }
    }

    @Nested
    @DisplayName("generateServiceTest()")
    class GenerateServiceTestTests {

        @Test
        @DisplayName("Should delegate to ServiceTestGenerator")
        void shouldDelegateToServiceTestGenerator() {
            SqlTable table = createSimpleTable("products");

            String result = generator.generateServiceTest(table);

            assertThat(result)
                    .contains("class ProductServiceImplTest")
                    .contains("@Mock")
                    .contains("ProductRepository");
        }

        @Test
        @DisplayName("Should generate valid service test code")
        void shouldGenerateValidServiceTestCode() {
            SqlTable table = createSimpleTable("categories");

            String result = generator.generateServiceTest(table);

            assertThat(result)
                    .doesNotContain("%s")
                    .contains("@DisplayName(\"CategoryService Tests\")");
        }
    }

    @Nested
    @DisplayName("generateDTOTest()")
    class GenerateDTOTestTests {

        @Test
        @DisplayName("Should delegate to DTOTestGenerator")
        void shouldDelegateToDtoTestGenerator() {
            SqlTable table = createSimpleTable("orders");

            String result = generator.generateDTOTest(table);

            assertThat(result).contains("class OrderDTOTest").contains("OrderDTO dto");
        }

        @Test
        @DisplayName("Should generate valid DTO test code")
        void shouldGenerateValidDtoTestCode() {
            SqlTable table = createSimpleTable("products");

            String result = generator.generateDTOTest(table);

            assertThat(result).doesNotContain("%s").contains("@DisplayName(\"ProductDTO Tests\")");
        }
    }

    @Nested
    @DisplayName("generateControllerTest()")
    class GenerateControllerTestTests {

        @Test
        @DisplayName("Should delegate to ControllerTestGenerator")
        void shouldDelegateToControllerTestGenerator() {
            SqlTable table = createSimpleTable("users");

            String result = generator.generateControllerTest(table);

            assertThat(result)
                    .contains("class UserControllerImplTest")
                    .contains("MockMvc mockMvc")
                    .contains("UserService service");
        }

        @Test
        @DisplayName("Should generate valid controller test code")
        void shouldGenerateValidControllerTestCode() {
            SqlTable table = createSimpleTable("categories");

            String result = generator.generateControllerTest(table);

            assertThat(result)
                    .doesNotContain("%s")
                    .contains("@DisplayName(\"CategoryController Tests\")");
        }
    }

    @Nested
    @DisplayName("generateIntegrationTest()")
    class GenerateIntegrationTestTests {

        @Test
        @DisplayName("Should delegate to IntegrationTestGenerator")
        void shouldDelegateToIntegrationTestGenerator() {
            SqlTable table = createSimpleTable("products");

            String result = generator.generateIntegrationTest(table);

            assertThat(result)
                    .contains("class ProductIntegrationTest")
                    .contains("@SpringBootTest")
                    .contains("ProductRepository repository");
        }

        @Test
        @DisplayName("Should generate valid integration test code")
        void shouldGenerateValidIntegrationTestCode() {
            SqlTable table = createSimpleTable("orders");

            String result = generator.generateIntegrationTest(table);

            assertThat(result)
                    .doesNotContain("%s")
                    .contains("@DisplayName(\"Order Integration Tests\")");
        }
    }

    @Nested
    @DisplayName("All Generators")
    class AllGeneratorsTests {

        @Test
        @DisplayName("Should generate all test types for a table")
        void shouldGenerateAllTestTypesForATable() {
            SqlTable table = createSimpleTable("products");

            String serviceTest = generator.generateServiceTest(table);
            String dtoTest = generator.generateDTOTest(table);
            String controllerTest = generator.generateControllerTest(table);
            String integrationTest = generator.generateIntegrationTest(table);

            assertThat(serviceTest).contains("ProductServiceImplTest");
            assertThat(dtoTest).contains("ProductDTOTest");
            assertThat(controllerTest).contains("ProductControllerImplTest");
            assertThat(integrationTest).contains("ProductIntegrationTest");
        }

        @Test
        @DisplayName("Should use same base package in all generators")
        void shouldUseSameBasePackageInAllGenerators() {
            TestGenerator customGen = new TestGenerator("org.myapp");
            SqlTable table = createSimpleTable("items");

            String serviceTest = customGen.generateServiceTest(table);
            String dtoTest = customGen.generateDTOTest(table);
            String controllerTest = customGen.generateControllerTest(table);
            String integrationTest = customGen.generateIntegrationTest(table);

            assertThat(serviceTest).contains("package org.myapp.items");
            assertThat(dtoTest).contains("package org.myapp.items");
            assertThat(controllerTest).contains("package org.myapp.items");
            assertThat(integrationTest).contains("package org.myapp.items");
        }

        @Test
        @DisplayName("Should handle various table names correctly")
        void shouldHandleVariousTableNamesCorrectly() {
            List<String> tableNames =
                    List.of("products", "order_items", "user_profiles", "api_keys");

            for (String tableName : tableNames) {
                SqlTable table = createSimpleTable(tableName);

                String serviceTest = generator.generateServiceTest(table);
                String dtoTest = generator.generateDTOTest(table);
                String controllerTest = generator.generateControllerTest(table);
                String integrationTest = generator.generateIntegrationTest(table);

                assertThat(serviceTest).as("ServiceTest for " + tableName).doesNotContain("%s");
                assertThat(dtoTest).as("DTOTest for " + tableName).doesNotContain("%s");
                assertThat(controllerTest)
                        .as("ControllerTest for " + tableName)
                        .doesNotContain("%s");
                assertThat(integrationTest)
                        .as("IntegrationTest for " + tableName)
                        .doesNotContain("%s");
            }
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
}
