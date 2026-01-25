/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jnzader.apigen.codegen.generator.rust;

import static com.jnzader.apigen.codegen.generator.util.RelationshipUtils.*;

import com.jnzader.apigen.codegen.generator.api.Feature;
import com.jnzader.apigen.codegen.generator.api.LanguageTypeMapper;
import com.jnzader.apigen.codegen.generator.api.ProjectConfig;
import com.jnzader.apigen.codegen.generator.api.ProjectGenerator;
import com.jnzader.apigen.codegen.generator.rust.config.RustConfigGenerator;
import com.jnzader.apigen.codegen.generator.rust.dto.RustDtoGenerator;
import com.jnzader.apigen.codegen.generator.rust.edge.RustModbusGenerator;
import com.jnzader.apigen.codegen.generator.rust.edge.RustMqttGenerator;
import com.jnzader.apigen.codegen.generator.rust.edge.RustOnnxGenerator;
import com.jnzader.apigen.codegen.generator.rust.edge.RustSerialGenerator;
import com.jnzader.apigen.codegen.generator.rust.handler.RustHandlerGenerator;
import com.jnzader.apigen.codegen.generator.rust.mail.RustMailServiceGenerator;
import com.jnzader.apigen.codegen.generator.rust.middleware.RustMiddlewareGenerator;
import com.jnzader.apigen.codegen.generator.rust.model.RustModelGenerator;
import com.jnzader.apigen.codegen.generator.rust.repository.RustRepositoryGenerator;
import com.jnzader.apigen.codegen.generator.rust.router.RustRouterGenerator;
import com.jnzader.apigen.codegen.generator.rust.security.reset.RustPasswordResetGenerator;
import com.jnzader.apigen.codegen.generator.rust.security.social.RustSocialLoginGenerator;
import com.jnzader.apigen.codegen.generator.rust.service.RustServiceGenerator;
import com.jnzader.apigen.codegen.generator.rust.storage.RustFileStorageGenerator;
import com.jnzader.apigen.codegen.generator.rust.test.RustTestGenerator;
import com.jnzader.apigen.codegen.model.SqlSchema;
import com.jnzader.apigen.codegen.model.SqlTable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Generates a complete Rust/Axum REST API project from SQL schema.
 *
 * <p>Supports multiple presets for different deployment scenarios:
 *
 * <ul>
 *   <li>{@code cloud} - Cloud-native API with PostgreSQL, Redis, JWT, OpenTelemetry
 *   <li>{@code edge-gateway} - IoT Gateway with MQTT, Modbus, Serial support
 *   <li>{@code edge-anomaly} - Anomaly detection with SQLite, MQTT, ndarray
 *   <li>{@code edge-ai} - ML inference with ONNX Runtime, tokenizers
 * </ul>
 *
 * @author APiGen
 * @since 2.12.0
 */
@SuppressWarnings({
    "java:S3776",
    "java:S6541"
}) // S3776/S6541: Complex generate() method inherent to code generation orchestration
public class RustAxumProjectGenerator implements ProjectGenerator {

    private static final String LANGUAGE = "rust";
    private static final String FRAMEWORK = "axum";
    private static final String DEFAULT_RUST_VERSION = "1.85";
    private static final String DEFAULT_FRAMEWORK_VERSION = "0.8";

    private static final Set<Feature> SUPPORTED_FEATURES =
            Set.of(
                    Feature.CRUD,
                    Feature.AUDITING,
                    Feature.SOFT_DELETE,
                    Feature.FILTERING,
                    Feature.PAGINATION,
                    Feature.DOCKER,
                    Feature.MANY_TO_ONE,
                    Feature.ONE_TO_MANY,
                    // Security features
                    Feature.JWT_AUTH,
                    Feature.RATE_LIMITING,
                    // Feature Pack 2025
                    Feature.MAIL_SERVICE,
                    Feature.PASSWORD_RESET,
                    Feature.SOCIAL_LOGIN,
                    Feature.FILE_UPLOAD,
                    Feature.S3_STORAGE,
                    Feature.AZURE_STORAGE,
                    // Testing
                    Feature.UNIT_TESTS,
                    Feature.INTEGRATION_TESTS);

    private final RustTypeMapper typeMapper = new RustTypeMapper();

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
        return "Rust/Axum REST API";
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
        return DEFAULT_RUST_VERSION;
    }

    @Override
    public String getDefaultFrameworkVersion() {
        return DEFAULT_FRAMEWORK_VERSION;
    }

    @Override
    public boolean supports(Feature feature) {
        return SUPPORTED_FEATURES.contains(feature);
    }

    @Override
    public List<String> validateConfig(ProjectConfig config) {
        List<String> errors = new ArrayList<>();

        if (config.getProjectName() == null || config.getProjectName().isBlank()) {
            errors.add("Project name is required");
        }

        return errors;
    }

    @Override
    public Map<String, String> generate(SqlSchema schema, ProjectConfig config) {
        Map<String, String> files = new HashMap<>();
        RustAxumOptions options = RustAxumOptions.fromConfig(config);

        // Initialize generators
        RustConfigGenerator configGenerator = new RustConfigGenerator(typeMapper, options, config);
        RustModelGenerator modelGenerator = new RustModelGenerator(typeMapper, options);
        RustDtoGenerator dtoGenerator = new RustDtoGenerator(typeMapper);
        RustRepositoryGenerator repositoryGenerator =
                new RustRepositoryGenerator(typeMapper, options);
        RustServiceGenerator serviceGenerator = new RustServiceGenerator(typeMapper, options);
        RustHandlerGenerator handlerGenerator = new RustHandlerGenerator(typeMapper, options);
        RustRouterGenerator routerGenerator = new RustRouterGenerator(typeMapper, options);
        RustMiddlewareGenerator middlewareGenerator =
                new RustMiddlewareGenerator(typeMapper, options);

        // Edge generators
        RustMqttGenerator mqttGenerator = new RustMqttGenerator(typeMapper, options);
        RustModbusGenerator modbusGenerator = new RustModbusGenerator(typeMapper, options);
        RustSerialGenerator serialGenerator = new RustSerialGenerator(typeMapper, options);
        RustOnnxGenerator onnxGenerator = new RustOnnxGenerator(typeMapper, options);

        // Get entity tables
        List<SqlTable> tables = schema.getEntityTables();

        // Build relationship map
        Map<String, List<SqlSchema.TableRelationship>> relationshipsByTable =
                buildRelationshipsByTable(schema);

        // === Root config files ===
        files.put("Cargo.toml", configGenerator.generateCargoToml());
        files.put(".env.example", configGenerator.generateEnvExample());
        files.put("config.toml", configGenerator.generateConfigToml());
        files.put(".gitignore", configGenerator.generateGitignore());
        files.put("rust-toolchain.toml", configGenerator.generateRustToolchain());
        files.put("Makefile", configGenerator.generateMakefile());
        files.put("README.md", configGenerator.generateReadme());

        if (options.useDocker()) {
            files.put("Dockerfile", configGenerator.generateDockerfile());
            files.put("docker-compose.yml", configGenerator.generateDockerCompose());
        }

        // === src/ files ===
        files.put("src/main.rs", configGenerator.generateMainRs());
        files.put("src/lib.rs", configGenerator.generateLibRs());
        files.put("src/config.rs", configGenerator.generateConfigRs());
        files.put("src/error.rs", configGenerator.generateErrorRs());

        // === Models ===
        files.put("src/models/mod.rs", modelGenerator.generateModRs(tables));
        for (SqlTable table : tables) {
            String moduleName = typeMapper.toModuleName(table.getName());
            List<SqlSchema.TableRelationship> tableRelations =
                    getRelationshipsForTable(table.getName(), relationshipsByTable);
            files.put(
                    "src/models/" + moduleName + ".rs",
                    modelGenerator.generate(table, tableRelations));
        }

        // === DTOs ===
        files.put("src/dto/mod.rs", dtoGenerator.generateModRs(tables));
        files.put("src/dto/pagination.rs", dtoGenerator.generatePaginationRs());
        for (SqlTable table : tables) {
            String moduleName = typeMapper.toModuleName(table.getName());
            files.put("src/dto/" + moduleName + "_dto.rs", dtoGenerator.generate(table));
        }

        // === Repositories ===
        if (options.hasDatabase()) {
            files.put("src/repository/mod.rs", repositoryGenerator.generateModRs(tables));
            for (SqlTable table : tables) {
                String moduleName = typeMapper.toModuleName(table.getName());
                files.put(
                        "src/repository/" + moduleName + "_repository.rs",
                        repositoryGenerator.generate(table));
            }
        }

        // === Services ===
        files.put("src/service/mod.rs", serviceGenerator.generateModRs(tables));
        for (SqlTable table : tables) {
            String moduleName = typeMapper.toModuleName(table.getName());
            files.put(
                    "src/service/" + moduleName + "_service.rs", serviceGenerator.generate(table));
        }

        // === Handlers ===
        files.put("src/handlers/mod.rs", handlerGenerator.generateModRs(tables));
        files.put("src/handlers/health.rs", handlerGenerator.generateHealthHandler());
        for (SqlTable table : tables) {
            String moduleName = typeMapper.toModuleName(table.getName());
            files.put(
                    "src/handlers/" + moduleName + "_handler.rs", handlerGenerator.generate(table));
        }

        // === Router ===
        files.put("src/router.rs", routerGenerator.generate(tables));

        // === Middleware (optional) ===
        if (options.useJwt() || options.useArgon2() || options.useRateLimiting()) {
            files.put("src/middleware/mod.rs", middlewareGenerator.generateModRs());
            if (options.useJwt()) {
                files.put("src/middleware/jwt.rs", middlewareGenerator.generateJwtMiddleware());
                files.put("src/middleware/auth.rs", middlewareGenerator.generateAuthModule());
            }
            if (options.useArgon2()) {
                files.put(
                        "src/middleware/password.rs", middlewareGenerator.generatePasswordModule());
            }
            if (options.useRateLimiting()) {
                files.put(
                        "src/middleware/rate_limit.rs",
                        middlewareGenerator.generateRateLimitMiddleware());
            }
        }

        // === MQTT (optional) ===
        if (options.useMqtt()) {
            files.put("src/mqtt/mod.rs", mqttGenerator.generateModRs());
            files.put("src/mqtt/publisher.rs", mqttGenerator.generatePublisher());
            files.put("src/mqtt/subscriber.rs", mqttGenerator.generateSubscriber());
        }

        // === Modbus (optional) ===
        if (options.useModbus()) {
            files.put("src/modbus/mod.rs", modbusGenerator.generateModRs());
            files.put("src/modbus/client.rs", modbusGenerator.generateClient());
        }

        // === Serial (optional) ===
        if (options.useSerial()) {
            files.put("src/serial/mod.rs", serialGenerator.generateModRs());
            files.put("src/serial/port.rs", serialGenerator.generatePort());
        }

        // === ONNX (optional) ===
        if (options.useOnnx()) {
            files.put("src/inference/mod.rs", onnxGenerator.generateModRs());
            files.put("src/inference/onnx.rs", onnxGenerator.generateOnnx());
        }

        // === Feature Pack 2025 ===
        generateFeaturePackFiles(files, config);

        // Generate tests for each entity
        if (config.isFeatureEnabled(Feature.UNIT_TESTS)
                || config.isFeatureEnabled(Feature.INTEGRATION_TESTS)) {
            RustTestGenerator testGenerator = new RustTestGenerator();

            for (SqlTable table : schema.getEntityTables()) {
                if (config.isFeatureEnabled(Feature.UNIT_TESTS)) {
                    files.putAll(testGenerator.generateTests(table));
                }
                if (config.isFeatureEnabled(Feature.INTEGRATION_TESTS)) {
                    String snakeName = typeMapper.toSnakeCase(table.getEntityName());
                    files.put(
                            "tests/integration_" + snakeName + "_test.rs",
                            testGenerator.generateIntegrationTest(table));
                }
            }
        }

        return files;
    }

    /** Generates Feature Pack 2025 files based on enabled features. */
    private void generateFeaturePackFiles(Map<String, String> files, ProjectConfig config) {
        // Mail Service
        if (config.isFeatureEnabled(Feature.MAIL_SERVICE)) {
            RustMailServiceGenerator mailGenerator = new RustMailServiceGenerator();
            boolean hasPasswordReset = config.isFeatureEnabled(Feature.PASSWORD_RESET);
            files.putAll(mailGenerator.generate(true, hasPasswordReset, true));
        }

        // Password Reset
        if (config.isFeatureEnabled(Feature.PASSWORD_RESET)) {
            RustPasswordResetGenerator resetGenerator = new RustPasswordResetGenerator();
            // 30 minute token expiration
            files.putAll(resetGenerator.generate(30));
        }

        // Social Login (OAuth2)
        if (config.isFeatureEnabled(Feature.SOCIAL_LOGIN)) {
            RustSocialLoginGenerator socialGenerator = new RustSocialLoginGenerator();
            files.putAll(socialGenerator.generate(List.of("google", "github")));
        }

        // File Storage
        if (config.isFeatureEnabled(Feature.FILE_UPLOAD)) {
            RustFileStorageGenerator storageGenerator = new RustFileStorageGenerator();
            boolean useS3 = config.isFeatureEnabled(Feature.S3_STORAGE);
            boolean useAzure = config.isFeatureEnabled(Feature.AZURE_STORAGE);
            files.putAll(storageGenerator.generate(useS3, useAzure));
        }
    }
}
