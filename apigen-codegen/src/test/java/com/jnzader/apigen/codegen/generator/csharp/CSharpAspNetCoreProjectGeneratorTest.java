package com.jnzader.apigen.codegen.generator.csharp;

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

@DisplayName("CSharpAspNetCoreProjectGenerator Tests")
class CSharpAspNetCoreProjectGeneratorTest {

    private CSharpAspNetCoreProjectGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new CSharpAspNetCoreProjectGenerator();
    }

    @Nested
    @DisplayName("Language and Framework")
    class LanguageAndFramework {

        @Test
        @DisplayName("Should return 'csharp' as language")
        void shouldReturnCSharpAsLanguage() {
            assertThat(generator.getLanguage()).isEqualTo("csharp");
        }

        @Test
        @DisplayName("Should return 'aspnet-core' as framework")
        void shouldReturnAspNetCoreAsFramework() {
            assertThat(generator.getFramework()).isEqualTo("aspnet-core");
        }

        @Test
        @DisplayName("Should return C# / ASP.NET Core 8.x as display name")
        void shouldReturnCorrectDisplayName() {
            assertThat(generator.getDisplayName()).isEqualTo("C# / ASP.NET Core 8.x");
        }

        @Test
        @DisplayName("Should return default .NET version")
        void shouldReturnDefaultDotNetVersion() {
            assertThat(generator.getDefaultLanguageVersion()).isEqualTo("8.0");
        }

        @Test
        @DisplayName("Should return default ASP.NET Core version")
        void shouldReturnDefaultAspNetCoreVersion() {
            assertThat(generator.getDefaultFrameworkVersion()).isEqualTo("8.0.0");
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
        @DisplayName("Should support all relationship features")
        void shouldSupportAllRelationshipFeatures() {
            Set<Feature> features = generator.getSupportedFeatures();
            assertThat(features)
                    .contains(Feature.MANY_TO_MANY, Feature.ONE_TO_MANY, Feature.MANY_TO_ONE);
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
    }

    @Nested
    @DisplayName("Type Mapper")
    class TypeMapperTests {

        @Test
        @DisplayName("Should return CSharpTypeMapper instance")
        void shouldReturnCSharpTypeMapper() {
            assertThat(generator.getTypeMapper()).isInstanceOf(CSharpTypeMapper.class);
        }
    }

    @Nested
    @DisplayName("generate()")
    class GenerateTests {

        @Test
        @DisplayName("Should generate files with .cs extension")
        void shouldGenerateFilesWithCsExtension() {
            SqlSchema schema = createSimpleSchema("products");
            ProjectConfig config = createConfig("MyApi");

            Map<String, String> files = generator.generate(schema, config);

            assertThat(files.keySet())
                    .anyMatch(path -> path.endsWith(".cs"))
                    .noneMatch(path -> path.contains("Domain/Entities") && path.endsWith(".java"))
                    .noneMatch(path -> path.contains("Domain/Entities") && path.endsWith(".kt"));
        }

        @Test
        @DisplayName("Should generate all required files for an entity")
        void shouldGenerateAllRequiredFilesForEntity() {
            SqlSchema schema = createSimpleSchema("products");
            ProjectConfig config = createConfig("MyApi");

            Map<String, String> files = generator.generate(schema, config);

            // Note: All DTOs (ProductDto, CreateProductDto, UpdateProductDto) are in a single file
            assertThat(files)
                    .containsKeys(
                            "Products/Domain/Entities/Product.cs",
                            "Products/Application/DTOs/ProductDto.cs",
                            "Products/Domain/Interfaces/IProductRepository.cs",
                            "Products/Infrastructure/Repositories/ProductRepository.cs",
                            "Products/Application/Interfaces/IProductService.cs",
                            "Products/Application/Services/ProductService.cs",
                            "Products/Api/Controllers/ProductsController.cs");
        }

        @Test
        @DisplayName("Should generate base classes for the project")
        void shouldGenerateBaseClassesForProject() {
            SqlSchema schema = createSimpleSchema("products");
            ProjectConfig config = createConfig("MyApi");

            Map<String, String> files = generator.generate(schema, config);

            assertThat(files)
                    .containsKeys(
                            "Domain/Common/BaseEntity.cs",
                            "Domain/Interfaces/IRepository.cs",
                            "Infrastructure/Repositories/Repository.cs",
                            "Application/Interfaces/IService.cs",
                            "Application/Services/Service.cs",
                            "Application/Common/PagedResult.cs",
                            "Application/Exceptions/NotFoundException.cs");
        }

        @Test
        @DisplayName("Should generate DbContext file")
        void shouldGenerateDbContextFile() {
            SqlSchema schema = createSimpleSchema("products");
            ProjectConfig config = createConfig("MyApi");

            Map<String, String> files = generator.generate(schema, config);

            assertThat(files).containsKeys("Infrastructure/Persistence/ApplicationDbContext.cs");
        }

        @Test
        @DisplayName("Should generate configuration files")
        void shouldGenerateConfigurationFiles() {
            SqlSchema schema = createSimpleSchema("products");
            ProjectConfig config = createConfig("MyApi");

            Map<String, String> files = generator.generate(schema, config);

            assertThat(files)
                    .containsKeys(
                            "MyApi.csproj",
                            "Program.cs",
                            "appsettings.json",
                            "appsettings.Development.json");
        }

        @Test
        @DisplayName("Should generate C# class syntax in entity files")
        void shouldGenerateCSharpClassSyntaxInEntityFiles() {
            SqlSchema schema = createSimpleSchema("products");
            ProjectConfig config = createConfig("MyApi");

            Map<String, String> files = generator.generate(schema, config);

            String entityContent = files.get("Products/Domain/Entities/Product.cs");
            assertThat(entityContent)
                    .contains("public class Product : BaseEntity")
                    .contains("[Table(\"products\")]")
                    .contains("namespace MyApi.Products.Domain.Entities;");
        }

        @Test
        @DisplayName("Should generate C# class with init properties in DTO files")
        void shouldGenerateCSharpClassSyntaxInDtoFiles() {
            SqlSchema schema = createSimpleSchema("products");
            ProjectConfig config = createConfig("MyApi");

            Map<String, String> files = generator.generate(schema, config);

            String dtoContent = files.get("Products/Application/DTOs/ProductDto.cs");
            assertThat(dtoContent)
                    .contains("public class ProductDto")
                    .contains("{ get; init; }")
                    .contains("namespace MyApi.Products.Application.DTOs;");
        }

        @Test
        @DisplayName("Should generate C# interface syntax in repository interface files")
        void shouldGenerateCSharpInterfaceSyntaxInRepositoryInterfaceFiles() {
            SqlSchema schema = createSimpleSchema("products");
            ProjectConfig config = createConfig("MyApi");

            Map<String, String> files = generator.generate(schema, config);

            String repoInterfaceContent =
                    files.get("Products/Domain/Interfaces/IProductRepository.cs");
            assertThat(repoInterfaceContent)
                    .contains("public interface IProductRepository : IRepository<Product, long>");
        }

        @Test
        @DisplayName("Should generate C# controller with attributes")
        void shouldGenerateCSharpControllerWithAttributes() {
            SqlSchema schema = createSimpleSchema("products");
            ProjectConfig config = createConfig("MyApi");

            Map<String, String> files = generator.generate(schema, config);

            String controllerContent = files.get("Products/Api/Controllers/ProductsController.cs");
            assertThat(controllerContent)
                    .contains("[ApiController]")
                    .contains("[Route(\"api/v1/products\")]")
                    .contains("[Produces(\"application/json\")]")
                    .contains("public class ProductsController : ControllerBase");
        }

        @Test
        @DisplayName("Should generate Program.cs with all service registrations")
        void shouldGenerateProgramCsWithServiceRegistrations() {
            SqlSchema schema = createSimpleSchema("products");
            ProjectConfig config = createConfig("MyApi");

            Map<String, String> files = generator.generate(schema, config);

            String programContent = files.get("Program.cs");
            assertThat(programContent)
                    .contains("var builder = WebApplication.CreateBuilder(args);")
                    .contains("builder.Services.AddControllers();")
                    .contains("builder.Services.AddSwaggerGen")
                    .contains("builder.Services.AddDbContext<ApplicationDbContext>")
                    .contains(
                            "builder.Services.AddScoped<IProductRepository, ProductRepository>();")
                    .contains("builder.Services.AddScoped<IProductService, ProductService>();");
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

            assertThat(errors).contains("Base namespace is required for C#/ASP.NET Core projects");
        }

        @Test
        @DisplayName("Should return error when base package is blank")
        void shouldReturnErrorWhenBasePackageIsBlank() {
            ProjectConfig config = ProjectConfig.builder().basePackage("   ").build();

            List<String> errors = generator.validateConfig(config);

            assertThat(errors).contains("Base namespace is required for C#/ASP.NET Core projects");
        }

        @Test
        @DisplayName("Should return no errors for valid config")
        void shouldReturnNoErrorsForValidConfig() {
            ProjectConfig config = ProjectConfig.builder().basePackage("MyApi").build();

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
