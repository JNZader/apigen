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

@DisplayName("ServiceTestGenerator Tests")
class ServiceTestGeneratorTest {

    private ServiceTestGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new ServiceTestGenerator("com.example");
    }

    @Nested
    @DisplayName("generate()")
    class GenerateTests {

        @Test
        @DisplayName("Should generate test class with correct package")
        void shouldGenerateTestClassWithCorrectPackage() {
            SqlTable table = createSimpleTable("products");

            String result = generator.generate(table);

            assertThat(result).contains("package com.example.products.application.service;");
        }

        @Test
        @DisplayName(
                "Should generate correct imports including EntityManager and ReflectionTestUtils")
        void shouldGenerateCorrectImports() {
            SqlTable table = createSimpleTable("products");

            String result = generator.generate(table);

            assertThat(result)
                    .contains(
                            "import"
                                + " com.jnzader.apigen.core.application.service.CacheEvictionService;")
                    .contains("import com.jnzader.apigen.core.application.util.Result;")
                    .contains("import com.example.products.domain.entity.Product;")
                    .contains(
                            "import"
                                + " com.example.products.infrastructure.repository.ProductRepository;")
                    .contains("import org.junit.jupiter.api.BeforeEach;")
                    .contains("import org.junit.jupiter.api.Test;")
                    .contains("import org.mockito.Mock;")
                    .contains("import jakarta.persistence.EntityManager;")
                    .contains("import org.springframework.test.util.ReflectionTestUtils;");
        }

        @Test
        @DisplayName("Should generate test class with correct name")
        void shouldGenerateTestClassWithCorrectName() {
            SqlTable table = createSimpleTable("products");

            String result = generator.generate(table);

            assertThat(result)
                    .contains("class ProductServiceImplTest")
                    .contains("@DisplayName(\"ProductService Tests\")");
        }

        @Test
        @DisplayName("Should generate mock fields including EntityManager")
        void shouldGenerateMockFields() {
            SqlTable table = createSimpleTable("products");

            String result = generator.generate(table);

            assertThat(result)
                    .contains("@Mock")
                    .contains("private ProductRepository repository;")
                    .contains("private CacheEvictionService cacheEvictionService;")
                    .contains("private ApplicationEventPublisher eventPublisher;")
                    .contains("private AuditorAware<String> auditorAware;")
                    .contains("private EntityManager entityManager;");
        }

        @Test
        @DisplayName("Should generate service and entity fields")
        void shouldGenerateServiceAndEntityFields() {
            SqlTable table = createSimpleTable("products");

            String result = generator.generate(table);

            assertThat(result)
                    .contains("private ProductServiceImpl service;")
                    .contains("private Product product;");
        }

        @Test
        @DisplayName("Should generate setUp method with EntityManager injection")
        void shouldGenerateSetUpMethod() {
            SqlTable table = createSimpleTable("products");

            String result = generator.generate(table);

            assertThat(result)
                    .contains("@BeforeEach")
                    .contains("void setUp()")
                    .contains(
                            "service = new ProductServiceImpl(repository, cacheEvictionService,"
                                    + " eventPublisher, auditorAware);")
                    .contains(
                            "ReflectionTestUtils.setField(service, \"entityManager\","
                                    + " entityManager);")
                    .contains("product = new Product();")
                    .contains("product.setId(1L);")
                    .contains("product.setEstado(true);");
        }
    }

    @Nested
    @DisplayName("Find Operations")
    class FindOperationsTests {

        @Test
        @DisplayName("Should generate findById test with correct variable usage")
        void shouldGenerateFindByIdTestWithCorrectVariableUsage() {
            SqlTable table = createSimpleTable("categories");

            String result = generator.generate(table);

            // Should use variable name (category) in Optional.of and assertEquals
            assertThat(result)
                    .contains("Optional.of(category)")
                    .contains("isEqualTo(category)")
                    // Should use class name (Category) in Result type
                    .contains("Result<Category, Exception>");
        }

        @Test
        @DisplayName("Should generate findAll test with correct types")
        void shouldGenerateFindAllTestWithCorrectTypes() {
            SqlTable table = createSimpleTable("products");

            String result = generator.generate(table);

            assertThat(result)
                    .contains("Page<Product> page = new PageImpl<>(List.of(product))")
                    .contains("Result<Page<Product>, Exception>");
        }
    }

    @Nested
    @DisplayName("Save Operations")
    class SaveOperationsTests {

        @Test
        @DisplayName("Should generate save test with correct class and variable usage")
        void shouldGenerateSaveTestWithCorrectUsage() {
            SqlTable table = createSimpleTable("products");

            String result = generator.generate(table);

            // Should use class name in type declaration and any()
            assertThat(result)
                    .contains("Product newProduct = new Product();")
                    .contains("any(Product.class)")
                    // Should use variable name in thenReturn
                    .contains(".thenReturn(product)");
        }

        @Test
        @DisplayName("Should generate update test with correct variable usage")
        void shouldGenerateUpdateTestWithCorrectVariableUsage() {
            SqlTable table = createSimpleTable("categories");

            String result = generator.generate(table);

            // In update test: findById returns variable, save uses class.class, thenReturn uses
            // variable
            assertThat(result)
                    .contains("when(repository.findById(1L)).thenReturn(Optional.of(category));")
                    .contains("when(repository.save(any(Category.class))).thenReturn(category);")
                    .contains("service.update(1L, category)");
        }

        @Test
        @DisplayName("Should generate saveAll test with correct usage")
        void shouldGenerateSaveAllTestWithCorrectUsage() {
            SqlTable table = createSimpleTable("orders");

            String result = generator.generate(table);

            // List.of should use variable, new should use class
            assertThat(result)
                    .contains("List<Order> entities = List.of(order, new Order())")
                    .contains("Result<List<Order>, Exception>");
        }
    }

    @Nested
    @DisplayName("Delete Operations")
    class DeleteOperationsTests {

        @Test
        @DisplayName("Should generate softDelete test with correct variable and class usage")
        void shouldGenerateSoftDeleteTestWithCorrectUsage() {
            SqlTable table = createSimpleTable("categories");

            String result = generator.generate(table);

            // This is the critical test that catches the bug we fixed
            // findById should return Optional.of(variable) not Optional.of(ClassName)
            assertThat(result)
                    .contains("when(repository.findById(1L)).thenReturn(Optional.of(category));")
                    // save should use any(ClassName.class) not any(variablename.class)
                    .contains("when(repository.save(any(Category.class))).thenReturn(category);")
                    // verify should use any(ClassName.class)
                    .contains("verify(repository).save(any(Category.class));");
        }

        @Test
        @DisplayName("Should NOT have lowercase class in any() calls")
        void shouldNotHaveLowercaseClassInAnyCalls() {
            SqlTable table = createSimpleTable("categories");

            String result = generator.generate(table);

            // This would be the bug: any(category.class) instead of any(Category.class)
            assertThat(result)
                    .doesNotContain("any(category.class)")
                    .doesNotContain("any(product.class)")
                    .doesNotContain("any(order.class)");
        }

        @Test
        @DisplayName("Should NOT have class name in Optional.of()")
        void shouldNotHaveClassNameInOptionalOf() {
            SqlTable table = createSimpleTable("categories");

            String result = generator.generate(table);

            // This would be the bug: Optional.of(Category) instead of Optional.of(category)
            assertThat(result)
                    .doesNotContain("Optional.of(Category)")
                    .doesNotContain("Optional.of(Product)")
                    .doesNotContain("Optional.of(Order)");
        }

        @Test
        @DisplayName("Should generate hardDelete test")
        void shouldGenerateHardDeleteTest() {
            SqlTable table = createSimpleTable("products");

            String result = generator.generate(table);

            assertThat(result)
                    .contains("@DisplayName(\"Should hard delete Product\")")
                    .contains("void shouldHardDeleteProduct()")
                    .contains("when(repository.hardDeleteById(1L)).thenReturn(1);")
                    .contains("service.hardDelete(1L)");
        }

        @Test
        @DisplayName("Should generate softDeleteAll test")
        void shouldGenerateSoftDeleteAllTest() {
            SqlTable table = createSimpleTable("products");

            String result = generator.generate(table);

            assertThat(result)
                    .contains("@DisplayName(\"Should soft delete all Product in batch\")")
                    .contains("void shouldSoftDeleteAllInBatch()")
                    .contains("service.softDeleteAll(List.of(1L, 2L))");
        }
    }

    @Nested
    @DisplayName("Restore Operations")
    class RestoreOperationsTests {

        @Test
        @DisplayName("Should generate restoreAll test")
        void shouldGenerateRestoreAllTest() {
            SqlTable table = createSimpleTable("products");

            String result = generator.generate(table);

            assertThat(result)
                    .contains("@DisplayName(\"Should restore all Product in batch\")")
                    .contains("void shouldRestoreAllInBatch()")
                    .contains("service.restoreAll(List.of(1L, 2L))");
        }
    }

    @Nested
    @DisplayName("Placeholder Count Validation")
    class PlaceholderCountValidationTests {

        @Test
        @DisplayName("Should have matching placeholder count - no MissingFormatArgumentException")
        void shouldHaveMatchingPlaceholderCount() {
            SqlTable table = createSimpleTable("test_entities");

            // If placeholder count doesn't match, this will throw MissingFormatArgumentException
            String result = generator.generate(table);

            assertThat(result)
                    .isNotNull()
                    .isNotEmpty()
                    // Basic sanity check that the template was fully formatted
                    .doesNotContain("%s");
        }

        @Test
        @DisplayName("Should generate valid Java code for various table names")
        void shouldGenerateValidJavaCodeForVariousTableNames() {
            List<String> tableNames =
                    List.of("products", "categories", "order_items", "user_profiles", "api_keys");

            for (String tableName : tableNames) {
                SqlTable table = createSimpleTable(tableName);
                String result = generator.generate(table);

                // Should not have unformatted placeholders
                assertThat(result).as("Table: " + tableName).doesNotContain("%s");

                // Should have proper class structure
                assertThat(result)
                        .as("Table: " + tableName)
                        .contains("class ")
                        .contains("@Test")
                        .contains("@BeforeEach");
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
