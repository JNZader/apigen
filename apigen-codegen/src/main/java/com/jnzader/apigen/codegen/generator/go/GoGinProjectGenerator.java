package com.jnzader.apigen.codegen.generator.go;

import com.jnzader.apigen.codegen.generator.api.Feature;
import com.jnzader.apigen.codegen.generator.api.LanguageTypeMapper;
import com.jnzader.apigen.codegen.generator.api.ProjectConfig;
import com.jnzader.apigen.codegen.generator.api.ProjectGenerator;
import com.jnzader.apigen.codegen.generator.go.auth.GoJwtAuthGenerator;
import com.jnzader.apigen.codegen.generator.go.auth.GoRateLimitGenerator;
import com.jnzader.apigen.codegen.generator.go.config.GoConfigGenerator;
import com.jnzader.apigen.codegen.generator.go.dto.GoDtoGenerator;
import com.jnzader.apigen.codegen.generator.go.handler.GoHandlerGenerator;
import com.jnzader.apigen.codegen.generator.go.model.GoModelGenerator;
import com.jnzader.apigen.codegen.generator.go.repository.GoRepositoryGenerator;
import com.jnzader.apigen.codegen.generator.go.router.GoRouterGenerator;
import com.jnzader.apigen.codegen.generator.go.service.GoServiceGenerator;
import com.jnzader.apigen.codegen.model.SqlSchema;
import com.jnzader.apigen.codegen.model.SqlTable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Project generator for Go/Gin applications.
 *
 * <p>This generator creates complete Gin API projects from SQL schemas, including:
 *
 * <ul>
 *   <li>GORM models with relationships
 *   <li>DTOs with go-playground/validator tags
 *   <li>Repositories for data access
 *   <li>Services with business logic
 *   <li>HTTP handlers with Swagger documentation
 *   <li>Router configuration
 *   <li>Project configuration files (go.mod, Dockerfile, etc.)
 * </ul>
 */
public class GoGinProjectGenerator implements ProjectGenerator {

    private static final String LANGUAGE = "go";
    private static final String FRAMEWORK = "gin";
    private static final String DEFAULT_GO_VERSION = "1.23";
    private static final String DEFAULT_FRAMEWORK_VERSION = "1.10";

    private static final Set<Feature> SUPPORTED_FEATURES =
            Set.of(
                    Feature.CRUD,
                    Feature.AUDITING,
                    Feature.SOFT_DELETE,
                    Feature.FILTERING,
                    Feature.PAGINATION,
                    Feature.OPENAPI,
                    Feature.DOCKER,
                    Feature.MANY_TO_ONE,
                    Feature.ONE_TO_MANY,
                    // Security features
                    Feature.JWT_AUTH,
                    Feature.RATE_LIMITING);

    private final GoTypeMapper typeMapper = new GoTypeMapper();

    @Override
    public String getLanguage() {
        return LANGUAGE;
    }

    @Override
    public String getFramework() {
        return FRAMEWORK;
    }

    @Override
    public String getDisplayName() {
        return "Go / Gin 1.10.x";
    }

    @Override
    public Set<Feature> getSupportedFeatures() {
        return SUPPORTED_FEATURES;
    }

    @Override
    public LanguageTypeMapper getTypeMapper() {
        return typeMapper;
    }

    @Override
    public String getDefaultLanguageVersion() {
        return DEFAULT_GO_VERSION;
    }

    @Override
    public String getDefaultFrameworkVersion() {
        return DEFAULT_FRAMEWORK_VERSION;
    }

    @Override
    public Map<String, String> generate(SqlSchema schema, ProjectConfig config) {
        Map<String, String> files = new LinkedHashMap<>();
        String moduleName = getModuleName(config);

        // Initialize specialized generators
        GoModelGenerator modelGenerator = new GoModelGenerator(typeMapper, moduleName);
        GoDtoGenerator dtoGenerator = new GoDtoGenerator(typeMapper);
        GoRepositoryGenerator repositoryGenerator =
                new GoRepositoryGenerator(typeMapper, moduleName);
        GoServiceGenerator serviceGenerator = new GoServiceGenerator(typeMapper, moduleName);
        GoHandlerGenerator handlerGenerator = new GoHandlerGenerator(typeMapper, moduleName);
        GoRouterGenerator routerGenerator = new GoRouterGenerator(typeMapper, moduleName);
        GoConfigGenerator configGenerator = new GoConfigGenerator(moduleName);

        // Collect all relationships for bidirectional mapping
        Map<String, List<SqlSchema.TableRelationship>> relationshipsByTable = new HashMap<>();
        for (SqlSchema.TableRelationship rel : schema.getAllRelationships()) {
            relationshipsByTable
                    .computeIfAbsent(rel.getSourceTable().getName(), k -> new ArrayList<>())
                    .add(rel);
        }

        // Generate shared DTO files
        files.put("internal/dto/paginated_response.go", dtoGenerator.generatePaginatedResponse());
        files.put("internal/dto/error_response.go", dtoGenerator.generateErrorResponse());

        // Generate base model
        files.put("internal/models/base.go", modelGenerator.generateBaseModel());

        // Generate base repository
        files.put("internal/repository/base.go", repositoryGenerator.generateBaseRepository());

        // Generate router
        files.put("internal/router/router.go", routerGenerator.generate(schema));
        files.put("internal/router/middleware.go", routerGenerator.generateMiddleware());

        // Generate code for each entity table
        for (SqlTable table : schema.getEntityTables()) {
            String entityName = typeMapper.toExportedName(table.getEntityName());
            String snakeName = typeMapper.toSnakeCase(entityName);

            List<SqlSchema.TableRelationship> tableRelations =
                    relationshipsByTable.getOrDefault(table.getName(), Collections.emptyList());

            // Find inverse relationships (where this table is the target)
            List<SqlSchema.TableRelationship> inverseRelations =
                    schema.getAllRelationships().stream()
                            .filter(r -> r.getTargetTable().getName().equals(table.getName()))
                            .filter(r -> !r.getSourceTable().isJunctionTable())
                            .toList();

            // 1. Generate Model
            String modelCode = modelGenerator.generate(table, tableRelations, inverseRelations);
            files.put("internal/models/" + snakeName + ".go", modelCode);

            // 2. Generate DTOs
            String createDtoCode = dtoGenerator.generateCreateRequest(table, tableRelations);
            files.put("internal/dto/" + snakeName + "_create_request.go", createDtoCode);

            String updateDtoCode = dtoGenerator.generateUpdateRequest(table, tableRelations);
            files.put("internal/dto/" + snakeName + "_update_request.go", updateDtoCode);

            String responseDtoCode = dtoGenerator.generateResponse(table, tableRelations);
            files.put("internal/dto/" + snakeName + "_response.go", responseDtoCode);

            // 3. Generate Repository
            String repositoryCode = repositoryGenerator.generate(table);
            files.put("internal/repository/" + snakeName + "_repository.go", repositoryCode);

            // 4. Generate Service
            String serviceCode = serviceGenerator.generate(table);
            files.put("internal/service/" + snakeName + "_service.go", serviceCode);

            // 5. Generate Handler
            String handlerCode = handlerGenerator.generate(table);
            files.put("internal/handler/" + snakeName + "_handler.go", handlerCode);
        }

        // Generate configuration files
        boolean hasJwtAuth = config.isFeatureEnabled(Feature.JWT_AUTH);
        boolean hasRateLimit = config.isFeatureEnabled(Feature.RATE_LIMITING);
        files.putAll(configGenerator.generate(schema, config, hasJwtAuth, hasRateLimit));

        // Generate security files
        generateSecurityFiles(files, config, moduleName);

        // Generate docs directory placeholder
        files.put("docs/.gitkeep", "");

        return files;
    }

    /** Generates security-related files based on enabled features. */
    private void generateSecurityFiles(
            Map<String, String> files, ProjectConfig config, String moduleName) {
        // JWT Authentication
        if (config.isFeatureEnabled(Feature.JWT_AUTH)) {
            GoJwtAuthGenerator jwtGenerator = new GoJwtAuthGenerator(moduleName);
            // 1 hour access token, 7 day refresh token
            files.putAll(jwtGenerator.generate(1, 168));
        }

        // Rate Limiting
        if (config.isFeatureEnabled(Feature.RATE_LIMITING)) {
            GoRateLimitGenerator rateLimitGenerator = new GoRateLimitGenerator(moduleName);
            // 100 requests per second, 50 burst, no Redis by default
            files.putAll(rateLimitGenerator.generate(100, 50, false));
        }
    }

    @Override
    public List<String> validateConfig(ProjectConfig config) {
        List<String> errors = new ArrayList<>();

        if (config.getProjectName() == null || config.getProjectName().isBlank()) {
            errors.add("Project name is required for Go/Gin projects");
        }

        return errors;
    }

    /**
     * Gets the Go module name from project config.
     *
     * @param config the project configuration
     * @return the Go module name (e.g., "github.com/user/project")
     */
    private String getModuleName(ProjectConfig config) {
        String projectName = config.getProjectName();
        if (projectName == null || projectName.isBlank()) {
            projectName = "myapi";
        }

        // Convert to snake_case for module name
        String snakeName = typeMapper.toSnakeCase(projectName);

        // If a base package is provided, use it as the module base
        String basePackage = config.getBasePackage();
        if (basePackage != null && !basePackage.isBlank()) {
            // If already in Go module format (contains slash), use as-is
            // Otherwise convert Java-style package to Go module (com.example -> com/example)
            String moduleBase =
                    basePackage.contains("/") ? basePackage : basePackage.replace(".", "/");
            return moduleBase + "/" + snakeName;
        }

        // Default to github.com/username/project format
        return "github.com/user/" + snakeName;
    }
}
