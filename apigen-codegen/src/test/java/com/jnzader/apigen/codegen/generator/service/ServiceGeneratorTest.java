package com.jnzader.apigen.codegen.generator.service;

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

@DisplayName("ServiceGenerator Tests")
@SuppressWarnings({
    "java:S5976", // Tests validate different specific service features
    "java:S1874", // Tests deprecated generators for backward compatibility
    "deprecation"
})
class ServiceGeneratorTest {

    private ServiceGenerator serviceGenerator;

    @BeforeEach
    void setUp() {
        serviceGenerator = new ServiceGenerator("com.example");
    }

    @Nested
    @DisplayName("generateInterface()")
    class GenerateInterfaceTests {

        @Test
        @DisplayName("Should generate service interface with correct package")
        void shouldGenerateServiceInterfaceWithCorrectPackage() {
            SqlTable table = createSimpleTable("products");

            String result = serviceGenerator.generateInterface(table);

            assertThat(result).contains("package com.example.products.application.service;");
        }

        @Test
        @DisplayName("Should generate correct imports")
        void shouldGenerateCorrectImports() {
            SqlTable table = createSimpleTable("products");

            String result = serviceGenerator.generateInterface(table);

            assertThat(result)
                    .contains("import com.jnzader.apigen.core.application.service.BaseService;")
                    .contains("import com.example.products.domain.entity.Product;");
        }

        @Test
        @DisplayName("Should extend BaseService with correct types")
        void shouldExtendBaseServiceWithCorrectTypes() {
            SqlTable table = createSimpleTable("products");

            String result = serviceGenerator.generateInterface(table);

            assertThat(result)
                    .contains("public interface ProductService extends BaseService<Product, Long>");
        }

        @Test
        @DisplayName("Should include comment about custom business methods")
        void shouldIncludeCommentAboutCustomBusinessMethods() {
            SqlTable table = createSimpleTable("products");

            String result = serviceGenerator.generateInterface(table);

            assertThat(result).contains("// Add custom business methods here");
        }

        @ParameterizedTest(name = "Should handle table {0} -> {1}Service interface")
        @CsvSource({
            "order_items, orderitems, OrderItem",
            "user_profiles, userprofiles, UserProfile",
            "categories, categories, Category"
        })
        @DisplayName("Should handle various table names in interface")
        void shouldHandleTableWithDifferentModuleName(
                String tableName, String moduleName, String entityName) {
            SqlTable table = createSimpleTable(tableName);

            String result = serviceGenerator.generateInterface(table);

            assertThat(result)
                    .contains("package com.example." + moduleName + ".application.service;")
                    .contains(
                            "import com.example."
                                    + moduleName
                                    + ".domain.entity."
                                    + entityName
                                    + ";")
                    .contains(
                            "public interface "
                                    + entityName
                                    + "Service extends BaseService<"
                                    + entityName
                                    + ", Long>");
        }
    }

    @Nested
    @DisplayName("generateImpl()")
    class GenerateImplTests {

        @Test
        @DisplayName("Should generate service implementation with correct package")
        void shouldGenerateServiceImplWithCorrectPackage() {
            SqlTable table = createSimpleTable("products");

            String result = serviceGenerator.generateImpl(table);

            assertThat(result).contains("package com.example.products.application.service;");
        }

        @Test
        @DisplayName("Should generate correct imports")
        void shouldGenerateCorrectImports() {
            SqlTable table = createSimpleTable("products");

            String result = serviceGenerator.generateImpl(table);

            assertThat(result)
                    .contains("import com.jnzader.apigen.core.application.service.BaseServiceImpl;")
                    .contains(
                            "import"
                                + " com.jnzader.apigen.core.application.service.CacheEvictionService;")
                    .contains("import com.example.products.domain.entity.Product;")
                    .contains(
                            "import"
                                + " com.example.products.infrastructure.repository.ProductRepository;")
                    .contains("import lombok.extern.slf4j.Slf4j;")
                    .contains("import org.springframework.context.ApplicationEventPublisher;")
                    .contains("import org.springframework.data.domain.AuditorAware;")
                    .contains("import org.springframework.stereotype.Service;")
                    .contains("import org.springframework.transaction.annotation.Transactional;");
        }

        @Test
        @DisplayName("Should generate class annotations")
        void shouldGenerateClassAnnotations() {
            SqlTable table = createSimpleTable("products");

            String result = serviceGenerator.generateImpl(table);

            assertThat(result).contains("@Service").contains("@Slf4j").contains("@Transactional");
        }

        @Test
        @DisplayName("Should extend BaseServiceImpl and implement interface")
        void shouldExtendBaseServiceImplAndImplementInterface() {
            SqlTable table = createSimpleTable("products");

            String result = serviceGenerator.generateImpl(table);

            assertThat(result)
                    .contains("public class ProductServiceImpl")
                    .contains("extends BaseServiceImpl<Product, Long>")
                    .contains("implements ProductService");
        }

        @Test
        @DisplayName("Should generate constructor with dependencies")
        void shouldGenerateConstructorWithDependencies() {
            SqlTable table = createSimpleTable("products");

            String result = serviceGenerator.generateImpl(table);

            assertThat(result)
                    .contains("public ProductServiceImpl(")
                    .contains("ProductRepository repository,")
                    .contains("CacheEvictionService cacheEvictionService,")
                    .contains("ApplicationEventPublisher eventPublisher,")
                    .contains("AuditorAware<String> auditorAware)")
                    .contains(
                            "super(repository, cacheEvictionService, eventPublisher,"
                                    + " auditorAware);");
        }

        @Test
        @DisplayName("Should override getEntityClass method")
        void shouldOverrideGetEntityClassMethod() {
            SqlTable table = createSimpleTable("products");

            String result = serviceGenerator.generateImpl(table);

            assertThat(result)
                    .contains("@Override")
                    .contains("protected Class<Product> getEntityClass()")
                    .contains("return Product.class;");
        }

        @ParameterizedTest(name = "Should handle table {0} -> {1}ServiceImpl")
        @CsvSource({
            "user_profiles, userprofiles, UserProfile",
            "order_items, orderitems, OrderItem",
            "categories, categories, Category"
        })
        @DisplayName("Should handle various table names in implementation")
        void shouldHandleTableWithDifferentModuleName(
                String tableName, String moduleName, String entityName) {
            SqlTable table = createSimpleTable(tableName);

            String result = serviceGenerator.generateImpl(table);

            assertThat(result)
                    .contains("package com.example." + moduleName + ".application.service;")
                    .contains(
                            "import com.example."
                                    + moduleName
                                    + ".domain.entity."
                                    + entityName
                                    + ";")
                    .contains(
                            "import com.example."
                                    + moduleName
                                    + ".infrastructure.repository."
                                    + entityName
                                    + "Repository;")
                    .contains("public class " + entityName + "ServiceImpl")
                    .contains("extends BaseServiceImpl<" + entityName + ", Long>")
                    .contains("implements " + entityName + "Service");
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
