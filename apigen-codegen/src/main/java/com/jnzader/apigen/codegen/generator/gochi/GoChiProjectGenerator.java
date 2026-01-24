package com.jnzader.apigen.codegen.generator.gochi;

import com.jnzader.apigen.codegen.generator.api.Feature;
import com.jnzader.apigen.codegen.generator.api.LanguageTypeMapper;
import com.jnzader.apigen.codegen.generator.api.ProjectConfig;
import com.jnzader.apigen.codegen.generator.api.ProjectGenerator;
import com.jnzader.apigen.codegen.generator.gochi.config.GoChiConfigGenerator;
import com.jnzader.apigen.codegen.generator.gochi.dto.GoChiDtoGenerator;
import com.jnzader.apigen.codegen.generator.gochi.handler.GoChiHandlerGenerator;
import com.jnzader.apigen.codegen.generator.gochi.middleware.GoChiMiddlewareGenerator;
import com.jnzader.apigen.codegen.generator.gochi.model.GoChiModelGenerator;
import com.jnzader.apigen.codegen.generator.gochi.repository.GoChiRepositoryGenerator;
import com.jnzader.apigen.codegen.generator.gochi.router.GoChiRouterGenerator;
import com.jnzader.apigen.codegen.generator.gochi.service.GoChiServiceGenerator;
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
 * Project generator for Go/Chi applications.
 *
 * <p>Generates lightweight, high-performance Go APIs using:
 *
 * <ul>
 *   <li>Chi Router - Lightweight HTTP router
 *   <li>pgx - High-performance PostgreSQL driver (no ORM)
 *   <li>Viper - Configuration management
 *   <li>golang-jwt - JWT authentication
 *   <li>bcrypt - Password hashing
 *   <li>OpenTelemetry - Observability (optional)
 *   <li>Redis - Caching/Sessions (optional)
 *   <li>NATS/MQTT - Messaging (optional)
 * </ul>
 *
 * <p>Designed for edge and cloud deployments with multi-tenant support.
 */
public class GoChiProjectGenerator implements ProjectGenerator {

    private static final String LANGUAGE = "go";
    private static final String FRAMEWORK = "chi";
    private static final String DEFAULT_GO_VERSION = "1.23";
    private static final String DEFAULT_FRAMEWORK_VERSION = "5.2";

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
                    Feature.CACHING,
                    Feature.UNIT_TESTS,
                    Feature.INTEGRATION_TESTS);

    private final GoChiTypeMapper typeMapper = new GoChiTypeMapper();

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
        return "Go / Chi 5.x (pgx + Viper)";
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

        // Parse extra options from config
        GoChiOptions options = GoChiOptions.fromConfig(config);

        // Initialize generators
        GoChiConfigGenerator configGenerator = new GoChiConfigGenerator(moduleName, options);
        GoChiModelGenerator modelGenerator = new GoChiModelGenerator(typeMapper);
        GoChiDtoGenerator dtoGenerator = new GoChiDtoGenerator(typeMapper);
        GoChiRepositoryGenerator repositoryGenerator =
                new GoChiRepositoryGenerator(typeMapper, moduleName, options);
        GoChiServiceGenerator serviceGenerator =
                new GoChiServiceGenerator(typeMapper, moduleName, options);
        GoChiHandlerGenerator handlerGenerator =
                new GoChiHandlerGenerator(typeMapper, moduleName, options);
        GoChiRouterGenerator routerGenerator =
                new GoChiRouterGenerator(typeMapper, moduleName, options);
        GoChiMiddlewareGenerator middlewareGenerator =
                new GoChiMiddlewareGenerator(moduleName, options);

        // Collect relationships
        Map<String, List<SqlSchema.TableRelationship>> relationshipsByTable = new HashMap<>();
        for (SqlSchema.TableRelationship rel : schema.getAllRelationships()) {
            relationshipsByTable
                    .computeIfAbsent(rel.getSourceTable().getName(), k -> new ArrayList<>())
                    .add(rel);
        }

        // Generate configuration files
        files.putAll(configGenerator.generate(schema, config));

        // Generate shared DTOs
        files.put("internal/dto/pagination.go", dtoGenerator.generatePagination());
        files.put("internal/dto/response.go", dtoGenerator.generateResponse());

        // Generate middleware
        files.put("internal/middleware/middleware.go", middlewareGenerator.generateAll());

        // Generate router
        files.put("internal/router/router.go", routerGenerator.generate(schema));

        // Generate health check handler (required by router)
        files.put("internal/handler/health.go", routerGenerator.generateHealthHandler());

        // Generate database layer
        files.put(
                "internal/database/postgres.go", repositoryGenerator.generateDatabaseConnection());
        if (options.useSqlite()) {
            files.put(
                    "internal/database/sqlite.go", repositoryGenerator.generateSqliteConnection());
        }
        files.put(
                "internal/database/migrations.go", repositoryGenerator.generateMigrations(schema));

        // Generate code for each entity
        for (SqlTable table : schema.getEntityTables()) {
            String entityName = typeMapper.toExportedName(table.getEntityName());
            String snakeName = typeMapper.toSnakeCase(entityName);

            List<SqlSchema.TableRelationship> tableRelations =
                    relationshipsByTable.getOrDefault(table.getName(), Collections.emptyList());

            List<SqlSchema.TableRelationship> inverseRelations =
                    schema.getAllRelationships().stream()
                            .filter(r -> r.getTargetTable().getName().equals(table.getName()))
                            .filter(r -> !r.getSourceTable().isJunctionTable())
                            .toList();

            // Model
            files.put(
                    "internal/model/" + snakeName + ".go",
                    modelGenerator.generate(table, tableRelations, inverseRelations));

            // DTOs
            files.put(
                    "internal/dto/" + snakeName + "_dto.go",
                    dtoGenerator.generate(table, tableRelations));

            // Repository
            files.put(
                    "internal/repository/" + snakeName + "_repository.go",
                    repositoryGenerator.generate(table));

            // Service
            files.put(
                    "internal/service/" + snakeName + "_service.go",
                    serviceGenerator.generate(table));

            // Handler
            files.put(
                    "internal/handler/" + snakeName + "_handler.go",
                    handlerGenerator.generate(table));
        }

        // Generate shared handler helpers
        files.put("internal/handler/helpers.go", handlerGenerator.generateHelpers());

        // Generate optional components
        if (options.useRedis()) {
            files.put("internal/cache/redis.go", configGenerator.generateRedis());
        }

        if (options.useNats()) {
            files.put("internal/messaging/nats.go", configGenerator.generateNats());
        }

        if (options.useMqtt()) {
            files.put("internal/messaging/mqtt.go", configGenerator.generateMqtt());
        }

        if (options.useOpenTelemetry()) {
            files.put("internal/telemetry/otel.go", configGenerator.generateOpenTelemetry());
        }

        if (options.useJwt()) {
            // JWT middleware in middleware package (used by router as mw.JWTAuth)
            files.put("internal/middleware/jwt.go", middlewareGenerator.generateJwtMiddleware());
            // Auth utilities in auth package
            files.put("internal/auth/jwt.go", middlewareGenerator.generateJwtAuth());
            files.put("internal/auth/password.go", middlewareGenerator.generatePasswordHash());
        }

        // Docs placeholder
        files.put("docs/.gitkeep", "");

        return files;
    }

    @Override
    public List<String> validateConfig(ProjectConfig config) {
        List<String> errors = new ArrayList<>();

        if (config.getProjectName() == null || config.getProjectName().isBlank()) {
            errors.add("Project name is required for Go/Chi projects");
        }

        return errors;
    }

    private String getModuleName(ProjectConfig config) {
        String projectName = config.getProjectName();
        if (projectName == null || projectName.isBlank()) {
            projectName = "myapi";
        }

        String snakeName = typeMapper.toSnakeCase(projectName);
        String basePackage = config.getBasePackage();

        if (basePackage != null && !basePackage.isBlank()) {
            String moduleBase =
                    basePackage.contains("/") ? basePackage : basePackage.replace(".", "/");
            return moduleBase + "/" + snakeName;
        }

        return "github.com/user/" + snakeName;
    }
}
