package com.jnzader.apigen.codegen.generator.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.jnzader.apigen.codegen.model.SqlColumn;
import com.jnzader.apigen.codegen.model.SqlTable;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("ControllerGenerator Tests")
@SuppressWarnings(
        "java:S5976") // Tests validate different specific controller features, not the same feature
// with different inputs
class ControllerGeneratorTest {

    private ControllerGenerator controllerGenerator;

    @BeforeEach
    void setUp() {
        controllerGenerator = new ControllerGenerator("com.example");
    }

    @Nested
    @DisplayName("generateInterface()")
    class GenerateInterfaceTests {

        @Test
        @DisplayName("Should generate controller interface with correct package")
        void shouldGenerateControllerInterfaceWithCorrectPackage() {
            SqlTable table = createSimpleTable("products");

            String result = controllerGenerator.generateInterface(table);

            assertThat(result).contains("package com.example.products.infrastructure.controller;");
        }

        @Test
        @DisplayName("Should generate correct imports")
        void shouldGenerateCorrectImports() {
            SqlTable table = createSimpleTable("products");

            String result = controllerGenerator.generateInterface(table);

            assertThat(result)
                    .contains(
                            "import"
                                + " com.jnzader.apigen.core.infrastructure.controller.BaseController;")
                    .contains("import com.example.products.application.dto.ProductDTO;")
                    .contains("import io.swagger.v3.oas.annotations.tags.Tag;")
                    .contains("import org.springframework.web.bind.annotation.RequestMapping;");
        }

        @Test
        @DisplayName("Should generate Swagger Tag annotation")
        void shouldGenerateSwaggerTagAnnotation() {
            SqlTable table = createSimpleTable("products");

            String result = controllerGenerator.generateInterface(table);

            assertThat(result)
                    .contains(
                            "@Tag(name = \"Products\", description = \"Product management API\")");
        }

        @Test
        @DisplayName("Should generate RequestMapping annotation with API path")
        void shouldGenerateRequestMappingAnnotationWithApiPath() {
            SqlTable table = createSimpleTable("products");

            String result = controllerGenerator.generateInterface(table);

            assertThat(result).contains("@RequestMapping(\"/api/v1/products\")");
        }

        @Test
        @DisplayName("Should extend BaseController with correct types")
        void shouldExtendBaseControllerWithCorrectTypes() {
            SqlTable table = createSimpleTable("products");

            String result = controllerGenerator.generateInterface(table);

            assertThat(result)
                    .contains(
                            "public interface ProductController extends BaseController<ProductDTO,"
                                    + " Long>");
        }

        @Test
        @DisplayName("Should include comment about custom endpoints")
        void shouldIncludeCommentAboutCustomEndpoints() {
            SqlTable table = createSimpleTable("products");

            String result = controllerGenerator.generateInterface(table);

            assertThat(result).contains("// Add custom endpoints here");
        }

        @ParameterizedTest(name = "Should handle table {0} -> {1}Controller")
        @CsvSource({
            "order_items, orderitems, OrderItems, OrderItem",
            "user_profiles, userprofiles, UserProfiles, UserProfile",
            "categories, categories, Categorys, Category"
        })
        @DisplayName("Should handle various table names in interface")
        void shouldHandleTableWithDifferentModuleName(
                String tableName, String moduleName, String tagName, String entityName) {
            SqlTable table = createSimpleTable(tableName);

            String result = controllerGenerator.generateInterface(table);

            assertThat(result)
                    .contains("package com.example." + moduleName + ".infrastructure.controller;")
                    .contains(
                            "@Tag(name = \""
                                    + tagName
                                    + "\", description = \""
                                    + entityName
                                    + " management API\")")
                    .contains("@RequestMapping(\"/api/v1/" + moduleName + "\")")
                    .contains(
                            "public interface "
                                    + entityName
                                    + "Controller extends BaseController<"
                                    + entityName
                                    + "DTO, Long>");
        }
    }

    @Nested
    @DisplayName("generateImpl()")
    class GenerateImplTests {

        @Test
        @DisplayName("Should generate controller implementation with correct package")
        void shouldGenerateControllerImplWithCorrectPackage() {
            SqlTable table = createSimpleTable("products");

            String result = controllerGenerator.generateImpl(table);

            assertThat(result).contains("package com.example.products.infrastructure.controller;");
        }

        @Test
        @DisplayName("Should generate correct imports")
        void shouldGenerateCorrectImports() {
            SqlTable table = createSimpleTable("products");

            String result = controllerGenerator.generateImpl(table);

            assertThat(result)
                    .contains(
                            "import"
                                + " com.jnzader.apigen.core.infrastructure.controller.BaseControllerImpl;")
                    .contains("import com.example.products.application.dto.ProductDTO;")
                    .contains("import com.example.products.application.mapper.ProductMapper;")
                    .contains("import com.example.products.application.service.ProductService;")
                    .contains("import com.example.products.domain.entity.Product;")
                    .contains("import lombok.extern.slf4j.Slf4j;")
                    .contains("import org.springframework.web.bind.annotation.RestController;");
        }

        @Test
        @DisplayName("Should generate class annotations")
        void shouldGenerateClassAnnotations() {
            SqlTable table = createSimpleTable("products");

            String result = controllerGenerator.generateImpl(table);

            assertThat(result).contains("@RestController").contains("@Slf4j");
        }

        @Test
        @DisplayName("Should extend BaseControllerImpl and implement interface")
        void shouldExtendBaseControllerImplAndImplementInterface() {
            SqlTable table = createSimpleTable("products");

            String result = controllerGenerator.generateImpl(table);

            assertThat(result)
                    .contains("public class ProductControllerImpl")
                    .contains("extends BaseControllerImpl<Product, ProductDTO, Long>")
                    .contains("implements ProductController");
        }

        @Test
        @DisplayName("Should generate private fields for service and mapper")
        void shouldGeneratePrivateFieldsForServiceAndMapper() {
            SqlTable table = createSimpleTable("products");

            String result = controllerGenerator.generateImpl(table);

            assertThat(result)
                    .contains("private final ProductService productService;")
                    .contains("private final ProductMapper productMapper;");
        }

        @Test
        @DisplayName("Should generate constructor with dependencies")
        void shouldGenerateConstructorWithDependencies() {
            SqlTable table = createSimpleTable("products");

            String result = controllerGenerator.generateImpl(table);

            assertThat(result)
                    .contains(
                            "public ProductControllerImpl(ProductService service, ProductMapper"
                                    + " mapper)")
                    .contains("super(service, mapper);")
                    .contains("this.productService = service;")
                    .contains("this.productMapper = mapper;");
        }

        @ParameterizedTest(name = "Should handle table {0} -> {1}ControllerImpl")
        @CsvSource({
            "user_profiles, userprofiles, UserProfile",
            "order_items, orderitems, OrderItem",
            "categories, categories, Category"
        })
        @DisplayName("Should handle various table names in implementation")
        void shouldHandleTableWithDifferentModuleName(
                String tableName, String moduleName, String entityName) {
            SqlTable table = createSimpleTable(tableName);

            String result = controllerGenerator.generateImpl(table);

            assertThat(result)
                    .contains("package com.example." + moduleName + ".infrastructure.controller;")
                    .contains(
                            "import com.example."
                                    + moduleName
                                    + ".application.dto."
                                    + entityName
                                    + "DTO;")
                    .contains(
                            "import com.example."
                                    + moduleName
                                    + ".application.mapper."
                                    + entityName
                                    + "Mapper;")
                    .contains(
                            "import com.example."
                                    + moduleName
                                    + ".application.service."
                                    + entityName
                                    + "Service;")
                    .contains(
                            "import com.example."
                                    + moduleName
                                    + ".domain.entity."
                                    + entityName
                                    + ";")
                    .contains("public class " + entityName + "ControllerImpl")
                    .contains(
                            "extends BaseControllerImpl<"
                                    + entityName
                                    + ", "
                                    + entityName
                                    + "DTO, Long>")
                    .contains("implements " + entityName + "Controller");
        }

        @Test
        @DisplayName("Should use camelCase variable names for fields")
        void shouldUseCamelCaseVariableNamesForFields() {
            SqlTable table = createSimpleTable("products");

            String result = controllerGenerator.generateImpl(table);

            assertThat(result)
                    .contains("private final ProductService productService;")
                    .contains("private final ProductMapper productMapper;")
                    .contains("this.productService = service;")
                    .contains("this.productMapper = mapper;");
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
