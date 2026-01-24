package com.jnzader.apigen.codegen.generator.go;

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

@DisplayName("GoGinProjectGenerator Tests")
class GoGinProjectGeneratorTest {

    private GoGinProjectGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new GoGinProjectGenerator();
    }

    @Nested
    @DisplayName("Language and Framework")
    class LanguageAndFramework {

        @Test
        @DisplayName("Should return 'go' as language")
        void shouldReturnGoAsLanguage() {
            assertThat(generator.getLanguage()).isEqualTo("go");
        }

        @Test
        @DisplayName("Should return 'gin' as framework")
        void shouldReturnGinAsFramework() {
            assertThat(generator.getFramework()).isEqualTo("gin");
        }

        @Test
        @DisplayName("Should return Go / Gin 1.10.x as display name")
        void shouldReturnCorrectDisplayName() {
            assertThat(generator.getDisplayName()).isEqualTo("Go / Gin 1.10.x");
        }

        @Test
        @DisplayName("Should return default Go version")
        void shouldReturnDefaultGoVersion() {
            assertThat(generator.getDefaultLanguageVersion()).isEqualTo("1.23");
        }

        @Test
        @DisplayName("Should return default Gin version")
        void shouldReturnDefaultGinVersion() {
            assertThat(generator.getDefaultFrameworkVersion()).isEqualTo("1.10");
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
        @DisplayName("Should support AUDITING feature")
        void shouldSupportAuditingFeature() {
            assertThat(generator.getSupportedFeatures()).contains(Feature.AUDITING);
        }

        @Test
        @DisplayName("Should support SOFT_DELETE feature")
        void shouldSupportSoftDeleteFeature() {
            assertThat(generator.getSupportedFeatures()).contains(Feature.SOFT_DELETE);
        }

        @Test
        @DisplayName("Should support relationship features")
        void shouldSupportRelationshipFeatures() {
            Set<Feature> features = generator.getSupportedFeatures();
            assertThat(features).contains(Feature.ONE_TO_MANY, Feature.MANY_TO_ONE);
        }

        @Test
        @DisplayName("Should support OPENAPI feature")
        void shouldSupportOpenApiFeature() {
            assertThat(generator.getSupportedFeatures()).contains(Feature.OPENAPI);
        }

        @Test
        @DisplayName("Should support PAGINATION feature")
        void shouldSupportPaginationFeature() {
            assertThat(generator.getSupportedFeatures()).contains(Feature.PAGINATION);
        }

        @Test
        @DisplayName("Should support DOCKER feature")
        void shouldSupportDockerFeature() {
            assertThat(generator.getSupportedFeatures()).contains(Feature.DOCKER);
        }
    }

    @Nested
    @DisplayName("Type Mapper")
    class TypeMapperTests {

        @Test
        @DisplayName("Should return GoTypeMapper instance")
        void shouldReturnGoTypeMapper() {
            assertThat(generator.getTypeMapper()).isInstanceOf(GoTypeMapper.class);
        }
    }

    @Nested
    @DisplayName("generate()")
    class GenerateTests {

        @Test
        @DisplayName("Should generate files with .go extension")
        void shouldGenerateFilesWithGoExtension() {
            SqlSchema schema = createSimpleSchema("products");
            ProjectConfig config = createConfig("myapi");

            Map<String, String> files = generator.generate(schema, config);

            assertThat(files.keySet())
                    .anyMatch(path -> path.endsWith(".go"))
                    .noneMatch(path -> path.contains("internal/models") && path.endsWith(".java"))
                    .noneMatch(path -> path.contains("internal/models") && path.endsWith(".kt"));
        }

        @Test
        @DisplayName("Should generate all required files for an entity")
        void shouldGenerateAllRequiredFilesForEntity() {
            SqlSchema schema = createSimpleSchema("products");
            ProjectConfig config = createConfig("myapi");

            Map<String, String> files = generator.generate(schema, config);

            assertThat(files.keySet())
                    .contains(
                            "internal/models/product.go",
                            "internal/dto/product_create_request.go",
                            "internal/dto/product_update_request.go",
                            "internal/dto/product_response.go",
                            "internal/repository/product_repository.go",
                            "internal/service/product_service.go",
                            "internal/handler/product_handler.go");
        }

        @Test
        @DisplayName("Should generate base files for the project")
        void shouldGenerateBaseFilesForProject() {
            SqlSchema schema = createSimpleSchema("products");
            ProjectConfig config = createConfig("myapi");

            Map<String, String> files = generator.generate(schema, config);

            assertThat(files.keySet())
                    .contains(
                            "internal/models/base.go",
                            "internal/repository/base.go",
                            "internal/dto/paginated_response.go",
                            "internal/dto/error_response.go");
        }

        @Test
        @DisplayName("Should generate router files")
        void shouldGenerateRouterFiles() {
            SqlSchema schema = createSimpleSchema("products");
            ProjectConfig config = createConfig("myapi");

            Map<String, String> files = generator.generate(schema, config);

            assertThat(files.keySet())
                    .contains("internal/router/router.go", "internal/router/middleware.go");
        }

        @Test
        @DisplayName("Should generate configuration files")
        void shouldGenerateConfigurationFiles() {
            SqlSchema schema = createSimpleSchema("products");
            ProjectConfig config = createConfig("myapi");

            Map<String, String> files = generator.generate(schema, config);

            assertThat(files.keySet())
                    .contains(
                            "go.mod",
                            "main.go",
                            "internal/config/config.go",
                            "internal/config/database.go",
                            "Makefile");
        }

        @Test
        @DisplayName("Should generate Docker files when feature is enabled")
        void shouldGenerateDockerFilesWhenFeatureIsEnabled() {
            SqlSchema schema = createSimpleSchema("products");
            ProjectConfig config =
                    ProjectConfig.builder()
                            .projectName("myapi")
                            .basePackage("github.com/user")
                            .enabledFeatures(Set.of(Feature.DOCKER))
                            .build();

            Map<String, String> files = generator.generate(schema, config);

            assertThat(files.keySet()).contains("Dockerfile", "docker-compose.yml");
        }

        @Test
        @DisplayName("Should generate Go struct syntax in model files")
        void shouldGenerateGoStructSyntaxInModelFiles() {
            SqlSchema schema = createSimpleSchema("products");
            ProjectConfig config = createConfig("myapi");

            Map<String, String> files = generator.generate(schema, config);

            String modelContent = files.get("internal/models/product.go");
            assertThat(modelContent)
                    .contains("package models")
                    .contains("type Product struct")
                    .contains("ID        int64")
                    .contains("gorm:\"primaryKey;autoIncrement\"")
                    .contains("json:\"id\"");
        }

        @Test
        @DisplayName("Should generate Go struct syntax in DTO files")
        void shouldGenerateGoStructSyntaxInDtoFiles() {
            SqlSchema schema = createSimpleSchema("products");
            ProjectConfig config = createConfig("myapi");

            Map<String, String> files = generator.generate(schema, config);

            String dtoContent = files.get("internal/dto/product_create_request.go");
            assertThat(dtoContent)
                    .contains("package dto")
                    .contains("type CreateProductRequest struct")
                    .contains("json:");
        }

        @Test
        @DisplayName("Should generate Go interface syntax in repository files")
        void shouldGenerateGoInterfaceSyntaxInRepositoryFiles() {
            SqlSchema schema = createSimpleSchema("products");
            ProjectConfig config = createConfig("myapi");

            Map<String, String> files = generator.generate(schema, config);

            String repoContent = files.get("internal/repository/product_repository.go");
            assertThat(repoContent)
                    .contains("type ProductRepository interface")
                    .contains("type productrepositoryImpl struct");
        }

        @Test
        @DisplayName("Should generate Go handler with Gin context")
        void shouldGenerateGoHandlerWithGinContext() {
            SqlSchema schema = createSimpleSchema("products");
            ProjectConfig config = createConfig("myapi");

            Map<String, String> files = generator.generate(schema, config);

            String handlerContent = files.get("internal/handler/product_handler.go");
            assertThat(handlerContent)
                    .contains("gin.Context")
                    .contains("type ProductHandler struct")
                    .contains("GetByID(c *gin.Context)");
        }

        @Test
        @DisplayName("Should generate main.go with all initializations")
        void shouldGenerateMainGoWithInitializations() {
            SqlSchema schema = createSimpleSchema("products");
            ProjectConfig config = createConfig("myapi");

            Map<String, String> files = generator.generate(schema, config);

            String mainContent = files.get("main.go");
            assertThat(mainContent)
                    .contains("package main")
                    .contains("func main()")
                    .contains("router.SetupRouter");
        }

        @Test
        @DisplayName("Should generate go.mod with dependencies")
        void shouldGenerateGoModWithDependencies() {
            SqlSchema schema = createSimpleSchema("products");
            ProjectConfig config = createConfig("myapi");

            Map<String, String> files = generator.generate(schema, config);

            String goModContent = files.get("go.mod");
            assertThat(goModContent)
                    .contains("module github.com/user/myapi")
                    .contains("github.com/gin-gonic/gin")
                    .contains("gorm.io/gorm")
                    .contains("gorm.io/driver/postgres");
        }
    }

    @Nested
    @DisplayName("validateConfig()")
    class ValidateConfigTests {

        @Test
        @DisplayName("Should return error when project name is null")
        void shouldReturnErrorWhenProjectNameIsNull() {
            ProjectConfig config = ProjectConfig.builder().projectName(null).build();

            List<String> errors = generator.validateConfig(config);

            assertThat(errors).contains("Project name is required for Go/Gin projects");
        }

        @Test
        @DisplayName("Should return error when project name is blank")
        void shouldReturnErrorWhenProjectNameIsBlank() {
            ProjectConfig config = ProjectConfig.builder().projectName("   ").build();

            List<String> errors = generator.validateConfig(config);

            assertThat(errors).contains("Project name is required for Go/Gin projects");
        }

        @Test
        @DisplayName("Should return no errors for valid config")
        void shouldReturnNoErrorsForValidConfig() {
            ProjectConfig config = ProjectConfig.builder().projectName("myapi").build();

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

    private ProjectConfig createConfig(String projectName) {
        return ProjectConfig.builder()
                .projectName(projectName)
                .basePackage("github.com/user")
                .enabledFeatures(Set.of())
                .build();
    }
}
