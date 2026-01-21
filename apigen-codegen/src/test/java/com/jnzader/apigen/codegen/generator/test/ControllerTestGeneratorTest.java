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

@DisplayName("ControllerTestGenerator Tests")
class ControllerTestGeneratorTest {

    private ControllerTestGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new ControllerTestGenerator("com.example");
    }

    @Nested
    @DisplayName("generate()")
    class GenerateTests {

        @Test
        @DisplayName("Should generate test class with correct package")
        void shouldGenerateTestClassWithCorrectPackage() {
            SqlTable table = createSimpleTable("products");

            String result = generator.generate(table);

            assertThat(result).contains("package com.example.products.infrastructure.controller;");
        }

        @Test
        @DisplayName("Should generate correct imports")
        void shouldGenerateCorrectImports() {
            SqlTable table = createSimpleTable("products");

            String result = generator.generate(table);

            assertThat(result)
                    .contains("import com.example.products.application.dto.ProductDTO;")
                    .contains("import com.example.products.application.mapper.ProductMapper;")
                    .contains("import com.example.products.application.service.ProductService;")
                    .contains("import com.example.products.domain.entity.Product;")
                    .contains("import com.jnzader.apigen.core.application.util.Result;")
                    .contains("import org.junit.jupiter.api.Test;")
                    .contains("import org.mockito.Mock;")
                    .contains("import org.springframework.test.web.servlet.MockMvc;");
        }

        @Test
        @DisplayName("Should generate test class with correct name")
        void shouldGenerateTestClassWithCorrectName() {
            SqlTable table = createSimpleTable("products");

            String result = generator.generate(table);

            assertThat(result)
                    .contains("class ProductControllerImplTest")
                    .contains("@DisplayName(\"ProductController Tests\")");
        }

        @Test
        @DisplayName("Should generate mock fields")
        void shouldGenerateMockFields() {
            SqlTable table = createSimpleTable("categories");

            String result = generator.generate(table);

            assertThat(result)
                    .contains("@Mock")
                    .contains("private CategoryService service;")
                    .contains("private CategoryMapper mapper;");
        }

        @Test
        @DisplayName("Should generate controller and entity fields")
        void shouldGenerateControllerAndEntityFields() {
            SqlTable table = createSimpleTable("products");

            String result = generator.generate(table);

            assertThat(result)
                    .contains("private ProductControllerImpl controller;")
                    .contains("private Product product;")
                    .contains("private ProductDTO dto;");
        }
    }

    @Nested
    @DisplayName("setUp Method")
    class SetUpMethodTests {

        @Test
        @DisplayName("Should generate setUp with correct variable names")
        void shouldGenerateSetUpWithCorrectVariableNames() {
            SqlTable table = createSimpleTable("categories");

            String result = generator.generate(table);

            assertThat(result)
                    .contains("@BeforeEach")
                    .contains("void setUp()")
                    .contains("controller = new CategoryControllerImpl(service, mapper);")
                    .contains("mockMvc = MockMvcBuilders.standaloneSetup(controller).build();");
        }

        @Test
        @DisplayName("Should initialize entity with correct variable name")
        void shouldInitializeEntityWithCorrectVariableName() {
            SqlTable table = createSimpleTable("orders");

            String result = generator.generate(table);

            assertThat(result)
                    .contains("order = new Order();")
                    .contains("order.setId(1L);")
                    .contains("order.setEstado(true);");
        }

        @Test
        @DisplayName("Should initialize DTO correctly")
        void shouldInitializeDtoCorrectly() {
            SqlTable table = createSimpleTable("products");

            String result = generator.generate(table);

            assertThat(result)
                    .contains("dto = new ProductDTO();")
                    .contains("dto.setId(1L);")
                    .contains("dto.setActivo(true);");
        }
    }

    @Nested
    @DisplayName("GET Operations")
    class GetOperationsTests {

        @Test
        @DisplayName("Should generate findAll test with correct types")
        void shouldGenerateFindAllTestWithCorrectTypes() {
            SqlTable table = createSimpleTable("products");

            String result = generator.generate(table);

            assertThat(result)
                    .contains("Page<Product> page = new PageImpl<>(List.of(product))")
                    .contains(
                            "when(service.findAll(any(Specification.class),"
                                    + " any(Pageable.class))).thenReturn(Result.success(page));")
                    .contains("get(\"/api/v1/products\")");
        }

        @Test
        @DisplayName("Should generate findById test with correct variable usage")
        void shouldGenerateFindByIdTestWithCorrectVariableUsage() {
            SqlTable table = createSimpleTable("categories");

            String result = generator.generate(table);

            assertThat(result)
                    .contains("when(service.findById(1L)).thenReturn(Result.success(category));")
                    .contains("when(mapper.toDTO(category)).thenReturn(dto);")
                    .contains("get(\"/api/v1/categories/1\")");
        }

        @Test
        @DisplayName("Should generate exists test")
        void shouldGenerateExistsTest() {
            SqlTable table = createSimpleTable("products");

            String result = generator.generate(table);

            assertThat(result)
                    .contains("@DisplayName(\"Should check if Product exists\")")
                    .contains("when(service.existsById(1L)).thenReturn(Result.success(true));")
                    .contains("head(\"/api/v1/products/1\")");
        }
    }

    @Nested
    @DisplayName("POST Operations")
    class PostOperationsTests {

        @Test
        @DisplayName("Should generate create test with correct class and variable usage")
        void shouldGenerateCreateTestWithCorrectUsage() {
            SqlTable table = createSimpleTable("products");

            String result = generator.generate(table);

            assertThat(result)
                    .contains("when(mapper.toEntity(any(ProductDTO.class))).thenReturn(product);")
                    .contains(
                            "when(service.save(any(Product.class))).thenReturn(Result.success(product));")
                    .contains("post(\"/api/v1/products\")");
        }

        @Test
        @DisplayName("Should generate restore test")
        void shouldGenerateRestoreTest() {
            SqlTable table = createSimpleTable("categories");

            String result = generator.generate(table);

            assertThat(result)
                    .contains("@DisplayName(\"Should restore soft-deleted Category\")")
                    .contains("when(service.restore(1L)).thenReturn(Result.success(category));")
                    .contains("post(\"/api/v1/categories/1/restore\")");
        }
    }

    @Nested
    @DisplayName("PUT Operations")
    class PutOperationsTests {

        @Test
        @DisplayName("Should generate update test with correct usage")
        void shouldGenerateUpdateTestWithCorrectUsage() {
            SqlTable table = createSimpleTable("orders");

            String result = generator.generate(table);

            assertThat(result)
                    .contains("when(mapper.toEntity(any(OrderDTO.class))).thenReturn(order);")
                    .contains(
                            "when(service.update(anyLong(),"
                                    + " any(Order.class))).thenReturn(Result.success(order));")
                    .contains("put(\"/api/v1/orders/1\")")
                    .contains("verify(service).update(eq(1L), any(Order.class));");
        }
    }

    @Nested
    @DisplayName("PATCH Operations")
    class PatchOperationsTests {

        @Test
        @DisplayName("Should generate partial update test using findById and save")
        void shouldGeneratePartialUpdateTest() {
            SqlTable table = createSimpleTable("products");

            String result = generator.generate(table);

            // PATCH controller calls: findById -> updateEntityFromDTO -> save (not partialUpdate)
            assertThat(result)
                    .contains("@DisplayName(\"Should partial update Product\")")
                    .contains("when(service.findById(1L)).thenReturn(Result.success(product));")
                    .contains(
                            "when(service.save(any(Product.class))).thenReturn(Result.success(product));")
                    .contains("patch(\"/api/v1/products/1\")")
                    .contains("verify(service).findById(1L);")
                    .contains("verify(service).save(any(Product.class));");
        }
    }

    @Nested
    @DisplayName("DELETE Operations")
    class DeleteOperationsTests {

        @Test
        @DisplayName("Should generate soft delete test")
        void shouldGenerateSoftDeleteTest() {
            SqlTable table = createSimpleTable("products");

            String result = generator.generate(table);

            assertThat(result)
                    .contains("@DisplayName(\"Should soft delete Product\")")
                    .contains("when(service.softDelete(1L)).thenReturn(Result.success(null));")
                    .contains("delete(\"/api/v1/products/1\")")
                    .contains("verify(service).softDelete(1L);");
        }

        @Test
        @DisplayName("Should generate hard delete test")
        void shouldGenerateHardDeleteTest() {
            SqlTable table = createSimpleTable("categories");

            String result = generator.generate(table);

            assertThat(result)
                    .contains("@DisplayName(\"Should hard delete Category with permanent flag\")")
                    .contains("when(service.hardDelete(1L)).thenReturn(Result.success(null));")
                    .contains(".param(\"permanent\", \"true\")")
                    .contains("verify(service).hardDelete(1L);");
        }
    }

    @Nested
    @DisplayName("Variable vs Class Name Consistency")
    class VariableClassNameConsistencyTests {

        @Test
        @DisplayName("Should use variable name in Result.success() calls")
        void shouldUseVariableNameInResultSuccessCalls() {
            SqlTable table = createSimpleTable("categories");

            String result = generator.generate(table);

            // Should use variable (category) not class (Category) in Result.success
            assertThat(result)
                    .contains("Result.success(category)")
                    .doesNotContain("Result.success(Category)");
        }

        @Test
        @DisplayName("Should use class name in any() calls")
        void shouldUseClassNameInAnyCalls() {
            SqlTable table = createSimpleTable("categories");

            String result = generator.generate(table);

            assertThat(result)
                    .contains("any(Category.class)")
                    .contains("any(CategoryDTO.class)")
                    // Should NOT have lowercase class name
                    .doesNotContain("any(category.class)");
        }

        @Test
        @DisplayName("Should use variable name in thenReturn calls")
        void shouldUseVariableNameInThenReturnCalls() {
            SqlTable table = createSimpleTable("orders");

            String result = generator.generate(table);

            assertThat(result)
                    .contains(".thenReturn(Result.success(order))")
                    // Should NOT have class name in thenReturn
                    .doesNotContain(".thenReturn(Result.success(Order))");
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

                assertThat(result)
                        .as("Table: " + tableName)
                        .doesNotContain("%s")
                        .contains("class ")
                        .contains("@Test")
                        .contains("MockMvc");
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
