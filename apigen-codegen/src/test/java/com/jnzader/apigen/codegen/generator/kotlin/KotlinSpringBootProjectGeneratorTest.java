package com.jnzader.apigen.codegen.generator.kotlin;

import static org.assertj.core.api.Assertions.assertThat;

import com.jnzader.apigen.codegen.generator.api.Feature;
import com.jnzader.apigen.codegen.generator.api.ProjectConfig;
import com.jnzader.apigen.codegen.model.SqlColumn;
import com.jnzader.apigen.codegen.model.SqlSchema;
import com.jnzader.apigen.codegen.model.SqlTable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("KotlinSpringBootProjectGenerator Tests")
class KotlinSpringBootProjectGeneratorTest {

    private KotlinSpringBootProjectGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new KotlinSpringBootProjectGenerator();
    }

    @Nested
    @DisplayName("Language and Framework")
    class LanguageAndFramework {

        @Test
        @DisplayName("Should return 'kotlin' as language")
        void shouldReturnKotlinAsLanguage() {
            assertThat(generator.getLanguage()).isEqualTo("kotlin");
        }

        @Test
        @DisplayName("Should return 'spring-boot' as framework")
        void shouldReturnSpringBootAsFramework() {
            assertThat(generator.getFramework()).isEqualTo("spring-boot");
        }

        @Test
        @DisplayName("Should return Kotlin / Spring Boot 4.x as display name")
        void shouldReturnCorrectDisplayName() {
            assertThat(generator.getDisplayName()).isEqualTo("Kotlin / Spring Boot 4.x");
        }

        @Test
        @DisplayName("Should return default Kotlin version")
        void shouldReturnDefaultKotlinVersion() {
            assertThat(generator.getDefaultLanguageVersion()).isEqualTo("2.1.0");
        }

        @Test
        @DisplayName("Should return default Spring Boot version")
        void shouldReturnDefaultSpringBootVersion() {
            assertThat(generator.getDefaultFrameworkVersion()).isEqualTo("4.0.0");
        }
    }

    @Nested
    @DisplayName("Supported Features")
    class SupportedFeatures {

        @Test
        @DisplayName("Should support CRUD feature")
        void shouldSupportCrudFeature() {
            assertThat(generator.getSupportedFeatures()).contains(Feature.CRUD);
        }

        @Test
        @DisplayName("Should support HATEOAS feature")
        void shouldSupportHateoasFeature() {
            assertThat(generator.getSupportedFeatures()).contains(Feature.HATEOAS);
        }

        @Test
        @DisplayName("Should support all relationship features")
        void shouldSupportAllRelationshipFeatures() {
            Set<Feature> features = generator.getSupportedFeatures();
            assertThat(features)
                    .contains(Feature.MANY_TO_MANY, Feature.ONE_TO_MANY, Feature.MANY_TO_ONE);
        }

        @Test
        @DisplayName("Should support authentication features")
        void shouldSupportAuthenticationFeatures() {
            Set<Feature> features = generator.getSupportedFeatures();
            assertThat(features).contains(Feature.JWT_AUTH, Feature.OAUTH2);
        }

        @Test
        @DisplayName("Should support testing features")
        void shouldSupportTestingFeatures() {
            Set<Feature> features = generator.getSupportedFeatures();
            assertThat(features).contains(Feature.UNIT_TESTS, Feature.INTEGRATION_TESTS);
        }
    }

    @Nested
    @DisplayName("Type Mapper")
    class TypeMapperTests {

        @Test
        @DisplayName("Should return KotlinTypeMapper instance")
        void shouldReturnKotlinTypeMapper() {
            assertThat(generator.getTypeMapper()).isInstanceOf(KotlinTypeMapper.class);
        }
    }

    @Nested
    @DisplayName("generate()")
    class GenerateTests {

        @Test
        @DisplayName("Should generate files with .kt extension")
        void shouldGenerateFilesWithKtExtension() {
            SqlSchema schema = createSimpleSchema("products");
            ProjectConfig config = createConfig("com.example");

            Map<String, String> files = generator.generate(schema, config);

            assertThat(files.keySet())
                    .anyMatch(path -> path.endsWith(".kt"))
                    .noneMatch(path -> path.contains("src/main/kotlin") && path.endsWith(".java"))
                    .noneMatch(path -> path.contains("src/test/kotlin") && path.endsWith(".java"));
        }

        @Test
        @DisplayName("Should generate all required files for an entity")
        void shouldGenerateAllRequiredFilesForEntity() {
            SqlSchema schema = createSimpleSchema("products");
            ProjectConfig config = createConfig("com.example");

            Map<String, String> files = generator.generate(schema, config);

            assertThat(files.keySet())
                    .contains(
                            "src/main/kotlin/com/example/products/domain/entity/Product.kt",
                            "src/main/kotlin/com/example/products/application/dto/ProductDTO.kt",
                            "src/main/kotlin/com/example/products/application/mapper/ProductMapper.kt",
                            "src/main/kotlin/com/example/products/infrastructure/repository/ProductRepository.kt",
                            "src/main/kotlin/com/example/products/application/service/ProductService.kt",
                            "src/main/kotlin/com/example/products/application/service/ProductServiceImpl.kt",
                            "src/main/kotlin/com/example/products/infrastructure/controller/ProductController.kt",
                            "src/main/kotlin/com/example/products/infrastructure/controller/ProductControllerImpl.kt");
        }

        @Test
        @DisplayName("Should place files in src/main/kotlin directory")
        void shouldPlaceFilesInSrcMainKotlinDirectory() {
            SqlSchema schema = createSimpleSchema("products");
            ProjectConfig config = createConfig("com.example");

            Map<String, String> files = generator.generate(schema, config);

            assertThat(files.keySet())
                    .anyMatch(path -> path.startsWith("src/main/kotlin/"))
                    .noneMatch(
                            path ->
                                    path.contains("domain/entity")
                                            && path.startsWith("src/main/java/"));
        }

        @Test
        @DisplayName("Should place test files in src/test/kotlin directory")
        void shouldPlaceTestFilesInSrcTestKotlinDirectory() {
            SqlSchema schema = createSimpleSchema("products");
            ProjectConfig config =
                    ProjectConfig.builder()
                            .basePackage("com.example")
                            .enabledFeatures(Set.of(Feature.UNIT_TESTS, Feature.INTEGRATION_TESTS))
                            .build();

            Map<String, String> files = generator.generate(schema, config);

            assertThat(files.keySet())
                    .anyMatch(path -> path.startsWith("src/test/kotlin/"))
                    .noneMatch(
                            path ->
                                    path.contains("ServiceImplTest")
                                            && path.startsWith("src/test/java/"));
        }

        @Test
        @DisplayName("Should generate unit test files when UNIT_TESTS feature is enabled")
        void shouldGenerateUnitTestFilesWhenUnitTestsEnabled() {
            SqlSchema schema = createSimpleSchema("products");
            ProjectConfig config =
                    ProjectConfig.builder()
                            .basePackage("com.example")
                            .enabledFeatures(Set.of(Feature.UNIT_TESTS))
                            .build();

            Map<String, String> files = generator.generate(schema, config);

            assertThat(files.keySet())
                    .contains(
                            "src/test/kotlin/com/example/products/application/service/ProductServiceImplTest.kt",
                            "src/test/kotlin/com/example/products/application/dto/ProductDTOTest.kt",
                            "src/test/kotlin/com/example/products/infrastructure/controller/ProductControllerImplTest.kt");
        }

        @Test
        @DisplayName("Should generate integration test when INTEGRATION_TESTS feature is enabled")
        void shouldGenerateIntegrationTestWhenIntegrationTestsEnabled() {
            SqlSchema schema = createSimpleSchema("products");
            ProjectConfig config =
                    ProjectConfig.builder()
                            .basePackage("com.example")
                            .enabledFeatures(Set.of(Feature.INTEGRATION_TESTS))
                            .build();

            Map<String, String> files = generator.generate(schema, config);

            assertThat(files.keySet())
                    .contains("src/test/kotlin/com/example/products/ProductIntegrationTest.kt");
        }

        @Test
        @DisplayName("Should generate migrations when MIGRATIONS feature is enabled")
        void shouldGenerateMigrationsWhenMigrationsEnabled() {
            SqlSchema schema = createSimpleSchema("products");
            ProjectConfig config =
                    ProjectConfig.builder()
                            .basePackage("com.example")
                            .enabledFeatures(Set.of(Feature.MIGRATIONS))
                            .build();

            Map<String, String> files = generator.generate(schema, config);

            assertThat(files.keySet())
                    .anyMatch(path -> path.startsWith("src/main/resources/db/migration/"))
                    .anyMatch(path -> path.endsWith(".sql"));
        }

        @Test
        @DisplayName("Should generate Kotlin open class syntax in entity files")
        void shouldGenerateKotlinOpenClassSyntaxInEntityFiles() {
            SqlSchema schema = createSimpleSchema("products");
            ProjectConfig config = createConfig("com.example");

            Map<String, String> files = generator.generate(schema, config);

            String entityContent =
                    files.get("src/main/kotlin/com/example/products/domain/entity/Product.kt");
            assertThat(entityContent).contains("open class Product(").contains(") : Base()");
        }

        @Test
        @DisplayName("Should generate Kotlin data class syntax in DTO files")
        void shouldGenerateKotlinDataClassSyntaxInDtoFiles() {
            SqlSchema schema = createSimpleSchema("products");
            ProjectConfig config = createConfig("com.example");

            Map<String, String> files = generator.generate(schema, config);

            String dtoContent =
                    files.get("src/main/kotlin/com/example/products/application/dto/ProductDTO.kt");
            assertThat(dtoContent).contains("data class ProductDTO(");
        }

        @Test
        @DisplayName("Should generate Kotlin interface syntax in service files")
        void shouldGenerateKotlinInterfaceSyntaxInServiceFiles() {
            SqlSchema schema = createSimpleSchema("products");
            ProjectConfig config = createConfig("com.example");

            Map<String, String> files = generator.generate(schema, config);

            String serviceContent =
                    files.get(
                            "src/main/kotlin/com/example/products/application/service/ProductService.kt");
            assertThat(serviceContent)
                    .contains("interface ProductService : BaseService<Product, Long>");
        }
    }

    @Nested
    @DisplayName("validateConfig()")
    class ValidateConfigTests {

        @Test
        @DisplayName("Should return error when base package is null")
        void shouldReturnErrorWhenBasePackageIsNull() {
            ProjectConfig config = ProjectConfig.builder().basePackage(null).build();

            List<String> errors = generator.validateConfig(config);

            assertThat(errors).contains("Base package is required for Kotlin/Spring Boot projects");
        }

        @Test
        @DisplayName("Should return error when base package is blank")
        void shouldReturnErrorWhenBasePackageIsBlank() {
            ProjectConfig config = ProjectConfig.builder().basePackage("   ").build();

            List<String> errors = generator.validateConfig(config);

            assertThat(errors).contains("Base package is required for Kotlin/Spring Boot projects");
        }

        @Test
        @DisplayName("Should return no errors for valid config")
        void shouldReturnNoErrorsForValidConfig() {
            ProjectConfig config = ProjectConfig.builder().basePackage("com.example").build();

            List<String> errors = generator.validateConfig(config);

            assertThat(errors).isEmpty();
        }
    }

    private SqlSchema createSimpleSchema(String tableName) {
        SqlTable table =
                SqlTable.builder()
                        .name(tableName)
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
                                                .length(100)
                                                .build()))
                        .foreignKeys(new ArrayList<>())
                        .indexes(new ArrayList<>())
                        .build();

        return SqlSchema.builder().tables(List.of(table)).functions(List.of()).build();
    }

    private ProjectConfig createConfig(String basePackage) {
        return ProjectConfig.builder().basePackage(basePackage).enabledFeatures(Set.of()).build();
    }
}
