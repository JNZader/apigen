package com.jnzader.apigen.codegen.generator.python;

import com.jnzader.apigen.codegen.generator.api.Feature;
import com.jnzader.apigen.codegen.generator.api.LanguageTypeMapper;
import com.jnzader.apigen.codegen.generator.api.ProjectConfig;
import com.jnzader.apigen.codegen.generator.api.ProjectGenerator;
import com.jnzader.apigen.codegen.generator.python.auth.PythonJwtAuthGenerator;
import com.jnzader.apigen.codegen.generator.python.auth.PythonRateLimitGenerator;
import com.jnzader.apigen.codegen.generator.python.config.PythonConfigGenerator;
import com.jnzader.apigen.codegen.generator.python.mail.PythonMailServiceGenerator;
import com.jnzader.apigen.codegen.generator.python.model.PythonModelGenerator;
import com.jnzader.apigen.codegen.generator.python.repository.PythonRepositoryGenerator;
import com.jnzader.apigen.codegen.generator.python.router.PythonRouterGenerator;
import com.jnzader.apigen.codegen.generator.python.schema.PythonSchemaGenerator;
import com.jnzader.apigen.codegen.generator.python.security.reset.PythonPasswordResetGenerator;
import com.jnzader.apigen.codegen.generator.python.security.social.PythonSocialLoginGenerator;
import com.jnzader.apigen.codegen.generator.python.service.PythonServiceGenerator;
import com.jnzader.apigen.codegen.generator.python.storage.PythonFileStorageGenerator;
import com.jnzader.apigen.codegen.generator.python.test.PythonTestGenerator;
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
 * Project generator for Python/FastAPI applications.
 *
 * <p>This generator creates complete FastAPI projects from SQL schemas, including:
 *
 * <ul>
 *   <li>SQLAlchemy models with relationships
 *   <li>Pydantic schemas (DTOs) with validation
 *   <li>Repository classes with async database operations
 *   <li>Service classes with business logic
 *   <li>FastAPI routers with REST endpoints
 *   <li>Project configuration files (pyproject.toml, Dockerfile, etc.)
 * </ul>
 */
@SuppressWarnings("java:S3400") // Template methods return constants for code generation
public class PythonFastApiProjectGenerator implements ProjectGenerator {

    private static final String LANGUAGE = "python";
    private static final String FRAMEWORK = "fastapi";
    private static final String DEFAULT_PYTHON_VERSION = "3.12";
    private static final String DEFAULT_FRAMEWORK_VERSION = "0.128.0";

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

    private final PythonTypeMapper typeMapper = new PythonTypeMapper();

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
        return "Python / FastAPI 0.115.x";
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
        return DEFAULT_PYTHON_VERSION;
    }

    @Override
    public String getDefaultFrameworkVersion() {
        return DEFAULT_FRAMEWORK_VERSION;
    }

    @Override
    public Map<String, String> generate(SqlSchema schema, ProjectConfig config) {
        Map<String, String> files = new LinkedHashMap<>();
        String projectName = config.getProjectName();

        // Initialize specialized generators
        PythonModelGenerator modelGenerator = new PythonModelGenerator();
        PythonSchemaGenerator schemaGenerator = new PythonSchemaGenerator();
        PythonRepositoryGenerator repositoryGenerator = new PythonRepositoryGenerator();
        PythonServiceGenerator serviceGenerator = new PythonServiceGenerator();
        PythonRouterGenerator routerGenerator = new PythonRouterGenerator();
        PythonConfigGenerator configGenerator = new PythonConfigGenerator(projectName);

        // Collect all relationships for bidirectional mapping
        Map<String, List<SqlSchema.TableRelationship>> relationshipsByTable = new HashMap<>();
        for (SqlSchema.TableRelationship rel : schema.getAllRelationships()) {
            relationshipsByTable
                    .computeIfAbsent(rel.getSourceTable().getName(), k -> new ArrayList<>())
                    .add(rel);
        }

        // Generate base classes
        files.put("app/models/__init__.py", "");
        files.put("app/models/base.py", modelGenerator.generateBase());
        files.put("app/schemas/__init__.py", "");
        files.put("app/schemas/common.py", schemaGenerator.generateCommonSchemas());
        files.put("app/repositories/__init__.py", "");
        files.put("app/repositories/base_repository.py", repositoryGenerator.generateBase());
        files.put("app/services/__init__.py", "");
        files.put("app/services/base_service.py", serviceGenerator.generateBase());
        files.put("app/routers/__init__.py", "");

        // Generate code for each entity table
        for (SqlTable table : schema.getEntityTables()) {
            List<SqlSchema.TableRelationship> tableRelations =
                    relationshipsByTable.getOrDefault(table.getName(), Collections.emptyList());

            // Find inverse relationships (where this table is the target)
            List<SqlSchema.TableRelationship> inverseRelations =
                    schema.getAllRelationships().stream()
                            .filter(r -> r.getTargetTable().getName().equals(table.getName()))
                            .filter(r -> !r.getSourceTable().isJunctionTable())
                            .toList();

            String varName = typeMapper.toSnakeCase(table.getEntityName());

            // 1. Generate Model
            String modelCode = modelGenerator.generate(table, tableRelations, inverseRelations);
            files.put("app/models/" + varName + ".py", modelCode);

            // 2. Generate Schemas (DTOs)
            String schemaCode = schemaGenerator.generate(table, tableRelations);
            files.put("app/schemas/" + varName + ".py", schemaCode);

            // 3. Generate Repository
            String repoCode = repositoryGenerator.generate(table);
            files.put("app/repositories/" + varName + "_repository.py", repoCode);

            // 4. Generate Service
            String serviceCode = serviceGenerator.generate(table);
            files.put("app/services/" + varName + "_service.py", serviceCode);

            // 5. Generate Router
            String routerCode = routerGenerator.generate(table);
            files.put("app/routers/" + varName + "_router.py", routerCode);
        }

        // Generate configuration files
        files.putAll(configGenerator.generate(schema, config));

        // Generate security features
        generateSecurityFiles(files, config);

        // Generate tests directory structure
        files.put("tests/__init__.py", "");
        files.put("tests/conftest.py", generateConftest());

        // Generate tests for each entity
        if (config.isFeatureEnabled(Feature.UNIT_TESTS)
                || config.isFeatureEnabled(Feature.INTEGRATION_TESTS)) {
            PythonTestGenerator testGenerator = new PythonTestGenerator();

            for (SqlTable table : schema.getEntityTables()) {
                if (config.isFeatureEnabled(Feature.UNIT_TESTS)) {
                    files.putAll(testGenerator.generateTests(table));
                }
                if (config.isFeatureEnabled(Feature.INTEGRATION_TESTS)) {
                    String snakeName = typeMapper.toSnakeCase(table.getEntityName());
                    files.put(
                            "tests/integration/test_" + snakeName + "_api.py",
                            testGenerator.generateIntegrationTest(table));
                }
            }
            files.put("tests/integration/__init__.py", "");
        }

        return files;
    }

    @Override
    public List<String> validateConfig(ProjectConfig config) {
        List<String> errors = new ArrayList<>();

        if (config.getProjectName() == null || config.getProjectName().isBlank()) {
            errors.add("Project name is required for Python/FastAPI projects");
        }

        return errors;
    }

    /** Generates security-related files based on enabled features. */
    private void generateSecurityFiles(Map<String, String> files, ProjectConfig config) {
        // JWT Authentication
        if (config.isFeatureEnabled(Feature.JWT_AUTH)) {
            PythonJwtAuthGenerator jwtGenerator = new PythonJwtAuthGenerator();
            // 30 minute access token, 7 day refresh token
            files.putAll(jwtGenerator.generate(30, 7));
        }

        // Rate Limiting
        if (config.isFeatureEnabled(Feature.RATE_LIMITING)) {
            PythonRateLimitGenerator rateLimitGenerator = new PythonRateLimitGenerator();
            // 100 requests per minute, no Redis by default
            files.putAll(rateLimitGenerator.generate("100/minute", false));
        }

        // Mail Service
        if (config.isFeatureEnabled(Feature.MAIL_SERVICE)) {
            PythonMailServiceGenerator mailGenerator = new PythonMailServiceGenerator();
            boolean hasPasswordReset = config.isFeatureEnabled(Feature.PASSWORD_RESET);
            files.putAll(mailGenerator.generate(true, hasPasswordReset, true));
        }

        // Password Reset
        if (config.isFeatureEnabled(Feature.PASSWORD_RESET)) {
            PythonPasswordResetGenerator resetGenerator = new PythonPasswordResetGenerator();
            files.putAll(resetGenerator.generate(30)); // 30 minute token expiration
        }

        // Social Login
        if (config.isFeatureEnabled(Feature.SOCIAL_LOGIN)) {
            PythonSocialLoginGenerator socialGenerator = new PythonSocialLoginGenerator();
            files.putAll(socialGenerator.generate(List.of("google", "github", "linkedin")));
        }

        // File Upload/Storage
        if (config.isFeatureEnabled(Feature.FILE_UPLOAD)) {
            PythonFileStorageGenerator storageGenerator = new PythonFileStorageGenerator();
            String storageType = "local";
            if (config.isFeatureEnabled(Feature.S3_STORAGE)) {
                storageType = "s3";
            } else if (config.isFeatureEnabled(Feature.AZURE_STORAGE)) {
                storageType = "azure";
            }
            files.putAll(storageGenerator.generate(storageType));
        }
    }

    /** Generates the pytest conftest.py with common fixtures. */
    private String generateConftest() {
        return """
        import asyncio
        from typing import AsyncGenerator, Generator

        import pytest
        import pytest_asyncio
        from httpx import AsyncClient, ASGITransport
        from sqlalchemy.ext.asyncio import AsyncSession, create_async_engine, async_sessionmaker

        from app.main import app
        from app.models.base import Base
        from app.core.database import get_db


        # Use in-memory SQLite for tests
        TEST_DATABASE_URL = "sqlite+aiosqlite:///:memory:"


        @pytest.fixture(scope="session")
        def event_loop() -> Generator:
            \"""Create an event loop for the test session.\"""
            loop = asyncio.get_event_loop_policy().new_event_loop()
            yield loop
            loop.close()


        @pytest_asyncio.fixture(scope="function")
        async def db_session() -> AsyncGenerator[AsyncSession, None]:
            \"""Create a database session for testing.\"""
            engine = create_async_engine(TEST_DATABASE_URL, echo=False)

            async with engine.begin() as conn:
                await conn.run_sync(Base.metadata.create_all)

            async_session = async_sessionmaker(
                engine, class_=AsyncSession, expire_on_commit=False
            )

            async with async_session() as session:
                yield session

            async with engine.begin() as conn:
                await conn.run_sync(Base.metadata.drop_all)

            await engine.dispose()


        @pytest_asyncio.fixture(scope="function")
        async def client(db_session: AsyncSession) -> AsyncGenerator[AsyncClient, None]:
            \"""Create a test client with database override.\"""

            async def override_get_db() -> AsyncGenerator[AsyncSession, None]:
                yield db_session

            app.dependency_overrides[get_db] = override_get_db

            transport = ASGITransport(app=app)
            async with AsyncClient(transport=transport, base_url="http://test") as ac:
                yield ac

            app.dependency_overrides.clear()
        """;
    }
}
