package com.jnzader.apigen.codegen.generator.python.config;

import com.jnzader.apigen.codegen.generator.api.Feature;
import com.jnzader.apigen.codegen.generator.api.ProjectConfig;
import com.jnzader.apigen.codegen.generator.python.PythonTypeMapper;
import com.jnzader.apigen.codegen.model.SqlSchema;
import com.jnzader.apigen.codegen.model.SqlTable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Generates configuration files for Python/FastAPI projects.
 *
 * <p>Generates:
 *
 * <ul>
 *   <li>pyproject.toml - Project metadata and dependencies
 *   <li>main.py - FastAPI application entry point
 *   <li>config.py - Application configuration using Pydantic Settings
 *   <li>database.py - Database connection setup
 *   <li>requirements.txt - Alternative dependency file
 *   <li>.env.example - Environment variables template
 * </ul>
 */
@SuppressWarnings({"java:S1192", "java:S2068", "java:S3400", "java:S3457", "java:S6126"})
public class PythonConfigGenerator {

    private static final String PYTHON_VERSION = "3.12";
    private static final String FASTAPI_VERSION = "0.128.0";
    private static final String SQLALCHEMY_VERSION = "2.0.46";
    private static final String PYDANTIC_VERSION = "2.12.5";

    private final String projectName;
    private final PythonTypeMapper typeMapper;

    public PythonConfigGenerator(String projectName) {
        this.projectName = toSnakeCase(projectName);
        this.typeMapper = new PythonTypeMapper();
    }

    /**
     * Generates all configuration files.
     *
     * @param schema the SQL schema
     * @param config the project configuration
     * @return map of file paths to content
     */
    public Map<String, String> generate(SqlSchema schema, ProjectConfig config) {
        Map<String, String> files = new LinkedHashMap<>();

        boolean hasJwtAuth = config.isFeatureEnabled(Feature.JWT_AUTH);
        boolean hasRateLimit = config.isFeatureEnabled(Feature.RATE_LIMITING);

        files.put("pyproject.toml", generatePyprojectToml(hasJwtAuth, hasRateLimit));
        files.put("requirements.txt", generateRequirements(hasJwtAuth, hasRateLimit));
        files.put("app/__init__.py", "");
        files.put("app/main.py", generateMainPy(schema, hasJwtAuth, hasRateLimit));
        files.put("app/core/__init__.py", "");
        files.put("app/core/config.py", generateConfigPy(hasJwtAuth, hasRateLimit));
        files.put("app/core/database.py", generateDatabasePy());
        files.put(".env.example", generateEnvExample(hasJwtAuth, hasRateLimit));
        files.put(".gitignore", generateGitignore());
        files.put("README.md", generateReadme(hasJwtAuth));
        files.put("Dockerfile", generateDockerfile());
        files.put("docker-compose.yml", generateDockerCompose(hasRateLimit));

        return files;
    }

    private String generatePyprojectToml(boolean hasJwtAuth, boolean hasRateLimit) {
        StringBuilder deps = new StringBuilder();
        deps.append("    \"fastapi>=%s\",\n".formatted(FASTAPI_VERSION));
        deps.append("    \"uvicorn[standard]>=0.32.0\",\n");
        deps.append("    \"sqlalchemy>=%s\",\n".formatted(SQLALCHEMY_VERSION));
        deps.append("    \"asyncpg>=0.30.0\",\n");
        deps.append("    \"pydantic>=%s\",\n".formatted(PYDANTIC_VERSION));
        deps.append("    \"pydantic-settings>=2.6.0\",\n");
        deps.append("    \"alembic>=1.14.0\",\n");
        deps.append("    \"python-dotenv>=1.0.0\",\n");

        // JWT dependencies
        if (hasJwtAuth) {
            deps.append("    \"python-jose[cryptography]>=3.3.0\",\n");
            deps.append("    \"passlib[bcrypt]>=1.7.4\",\n");
        }

        // Rate limiting dependencies
        if (hasRateLimit) {
            deps.append("    \"slowapi>=0.1.9\",\n");
        }

        return """
        [project]
        name = "%s"
        version = "0.1.0"
        description = "Generated FastAPI project"
        readme = "README.md"
        requires-python = ">=%s"
        dependencies = [
        %s]

        [project.optional-dependencies]
        dev = [
            "pytest>=8.3.0",
            "pytest-asyncio>=0.24.0",
            "httpx>=0.28.0",
            "pytest-cov>=6.0.0",
            "ruff>=0.8.0",
            "mypy>=1.13.0",
        ]

        [build-system]
        requires = ["hatchling"]
        build-backend = "hatchling.build"

        [tool.ruff]
        line-length = 100
        target-version = "py312"

        [tool.ruff.lint]
        select = ["E", "F", "I", "N", "W", "UP"]

        [tool.pytest.ini_options]
        asyncio_mode = "auto"
        testpaths = ["tests"]

        [tool.mypy]
        python_version = "%s"
        strict = true
        """
                .formatted(projectName, PYTHON_VERSION, deps.toString(), PYTHON_VERSION);
    }

    private String generateRequirements(boolean hasJwtAuth, boolean hasRateLimit) {
        StringBuilder sb = new StringBuilder();
        sb.append("fastapi>=%s\n".formatted(FASTAPI_VERSION));
        sb.append("uvicorn[standard]>=0.32.0\n");
        sb.append("sqlalchemy>=%s\n".formatted(SQLALCHEMY_VERSION));
        sb.append("asyncpg>=0.30.0\n");
        sb.append("pydantic>=%s\n".formatted(PYDANTIC_VERSION));
        sb.append("pydantic-settings>=2.6.0\n");
        sb.append("alembic>=1.14.0\n");
        sb.append("python-dotenv>=1.0.0\n");

        if (hasJwtAuth) {
            sb.append("python-jose[cryptography]>=3.3.0\n");
            sb.append("passlib[bcrypt]>=1.7.4\n");
        }

        if (hasRateLimit) {
            sb.append("slowapi>=0.1.9\n");
        }

        return sb.toString();
    }

    private String generateMainPy(SqlSchema schema, boolean hasJwtAuth, boolean hasRateLimit) {
        StringBuilder sb = new StringBuilder();

        sb.append("from contextlib import asynccontextmanager\n");
        sb.append("from fastapi import FastAPI\n");
        sb.append("from fastapi.middleware.cors import CORSMiddleware\n");
        sb.append("\n");
        sb.append("from app.core.config import settings\n");
        sb.append("from app.core.database import engine\n");
        sb.append("from app.models.base import Base\n");

        // Rate limiting imports
        if (hasRateLimit) {
            sb.append("from app.middleware.rate_limit import setup_rate_limiting\n");
        }

        // Auth router import
        if (hasJwtAuth) {
            sb.append("from app.auth.router import router as auth_router\n");
        }

        // Import routers
        for (SqlTable table : schema.getEntityTables()) {
            String varName = typeMapper.toSnakeCase(table.getEntityName());
            sb.append("from app.routers.").append(varName).append("_router import router as ");
            sb.append(varName).append("_router\n");
        }

        sb.append("\n\n");

        // Lifespan context manager
        sb.append("@asynccontextmanager\n");
        sb.append("async def lifespan(app: FastAPI):\n");
        sb.append("    \"\"\"Application lifespan handler.\"\"\"\n");
        sb.append("    # Startup\n");
        sb.append("    async with engine.begin() as conn:\n");
        sb.append("        await conn.run_sync(Base.metadata.create_all)\n");
        sb.append("    yield\n");
        sb.append("    # Shutdown\n");
        sb.append("    await engine.dispose()\n");
        sb.append("\n\n");

        // App creation
        sb.append("app = FastAPI(\n");
        sb.append("    title=\"").append(toPascalCase(projectName)).append(" API\",\n");
        sb.append("    description=\"Generated API with FastAPI\",\n");
        sb.append("    version=\"0.1.0\",\n");
        sb.append("    lifespan=lifespan,\n");
        sb.append("    docs_url=\"/docs\",\n");
        sb.append("    redoc_url=\"/redoc\",\n");
        sb.append("    openapi_url=\"/openapi.json\",\n");
        sb.append(")\n\n");

        // Rate limiting setup
        if (hasRateLimit) {
            sb.append("# Setup rate limiting\n");
            sb.append("setup_rate_limiting(app)\n\n");
        }

        // CORS middleware
        sb.append("# CORS middleware\n");
        sb.append("app.add_middleware(\n");
        sb.append("    CORSMiddleware,\n");
        sb.append("    allow_origins=settings.cors_origins,\n");
        sb.append("    allow_credentials=True,\n");
        sb.append("    allow_methods=[\"*\"],\n");
        sb.append("    allow_headers=[\"*\"],\n");
        sb.append(")\n\n");

        // Include auth router first if present
        if (hasJwtAuth) {
            sb.append("# Auth router\n");
            sb.append("app.include_router(auth_router, prefix=\"/api\")\n\n");
        }

        // Include entity routers
        sb.append("# Include routers\n");
        for (SqlTable table : schema.getEntityTables()) {
            String varName = typeMapper.toSnakeCase(table.getEntityName());
            sb.append("app.include_router(").append(varName).append("_router)\n");
        }
        sb.append("\n\n");

        // Health check
        sb.append("@app.get(\"/health\", tags=[\"Health\"])\n");
        sb.append("async def health_check():\n");
        sb.append("    \"\"\"Health check endpoint.\"\"\"\n");
        sb.append("    return {\"status\": \"healthy\"}\n");
        sb.append("\n\n");

        // Main entry point
        sb.append("if __name__ == \"__main__\":\n");
        sb.append("    import uvicorn\n");
        sb.append("    uvicorn.run(\n");
        sb.append("        \"app.main:app\",\n");
        sb.append("        host=settings.host,\n");
        sb.append("        port=settings.port,\n");
        sb.append("        reload=settings.debug,\n");
        sb.append("    )\n");

        return sb.toString();
    }

    private String generateConfigPy(boolean hasJwtAuth, boolean hasRateLimit) {
        StringBuilder sb = new StringBuilder();

        sb.append("from pydantic_settings import BaseSettings, SettingsConfigDict\n\n\n");
        sb.append("class Settings(BaseSettings):\n");
        sb.append("    \"\"\"Application settings.\"\"\"\n\n");
        sb.append("    model_config = SettingsConfigDict(\n");
        sb.append("        env_file=\".env\",\n");
        sb.append("        env_file_encoding=\"utf-8\",\n");
        sb.append("        case_sensitive=False,\n");
        sb.append("    )\n\n");

        // Application settings
        sb.append("    # Application\n");
        sb.append("    app_name: str = \"%s\"\n".formatted(toPascalCase(projectName)));
        sb.append("    debug: bool = False\n");
        sb.append("    host: str = \"0.0.0.0\"\n");
        sb.append("    port: int = 8000\n\n");

        // Database settings
        sb.append("    # Database\n");
        sb.append(
                "    database_url: str = \"postgresql+asyncpg://postgres:postgres@localhost:5432/%s\"\n\n"
                        .formatted(projectName));

        // CORS settings
        sb.append("    # CORS\n");
        sb.append(
                "    cors_origins: list[str] = [\"http://localhost:3000\","
                        + " \"http://localhost:8080\"]\n\n");

        // Pagination settings
        sb.append("    # Pagination\n");
        sb.append("    default_page_size: int = 10\n");
        sb.append("    max_page_size: int = 100\n\n");

        // JWT settings
        if (hasJwtAuth) {
            sb.append("    # JWT Authentication\n");
            sb.append("    SECRET_KEY: str = \"change-me-in-production-use-secure-key\"\n");
            sb.append("    ACCESS_TOKEN_EXPIRE_MINUTES: int = 30\n");
            sb.append("    REFRESH_TOKEN_EXPIRE_DAYS: int = 7\n\n");
        }

        // Rate limiting settings
        if (hasRateLimit) {
            sb.append("    # Rate Limiting\n");
            sb.append("    RATE_LIMIT_DEFAULT: str = \"100/minute\"\n");
            sb.append(
                    "    REDIS_URL: str | None = None  # Optional Redis URL for distributed rate"
                            + " limiting\n\n");
        }

        sb.append("\nsettings = Settings()\n");

        return sb.toString();
    }

    private String generateDatabasePy() {
        return """
        from sqlalchemy.ext.asyncio import AsyncSession, create_async_engine, async_sessionmaker
        from app.core.config import settings

        engine = create_async_engine(
            settings.database_url,
            echo=settings.debug,
            future=True,
        )

        async_session_maker = async_sessionmaker(
            engine,
            class_=AsyncSession,
            expire_on_commit=False,
            autocommit=False,
            autoflush=False,
        )


        async def get_db() -> AsyncSession:
            \"""Dependency to get database session.\"""
            async with async_session_maker() as session:
                try:
                    yield session
                finally:
                    await session.close()
        """;
    }

    private String generateEnvExample(boolean hasJwtAuth, boolean hasRateLimit) {
        StringBuilder sb = new StringBuilder();

        sb.append("# Application\n");
        sb.append("APP_NAME=%s\n".formatted(toPascalCase(projectName)));
        sb.append("DEBUG=true\n");
        sb.append("HOST=0.0.0.0\n");
        sb.append("PORT=8000\n\n");

        sb.append("# Database\n");
        sb.append(
                "DATABASE_URL=postgresql+asyncpg://postgres:postgres@localhost:5432/%s\n\n"
                        .formatted(projectName));

        sb.append("# CORS (comma-separated)\n");
        sb.append("CORS_ORIGINS=[\"http://localhost:3000\"]\n\n");

        if (hasJwtAuth) {
            sb.append("# JWT Authentication\n");
            sb.append("SECRET_KEY=your-super-secret-key-change-in-production\n");
            sb.append("ACCESS_TOKEN_EXPIRE_MINUTES=30\n");
            sb.append("REFRESH_TOKEN_EXPIRE_DAYS=7\n\n");
        }

        if (hasRateLimit) {
            sb.append("# Rate Limiting\n");
            sb.append("RATE_LIMIT_DEFAULT=100/minute\n");
            sb.append(
                    "# REDIS_URL=redis://localhost:6379  # Uncomment for distributed rate"
                            + " limiting\n\n");
        }

        return sb.toString();
    }

    private String generateGitignore() {
        return """
        # Python
        __pycache__/
        *.py[cod]
        *$py.class
        *.so
        .Python
        build/
        develop-eggs/
        dist/
        downloads/
        eggs/
        .eggs/
        lib/
        lib64/
        parts/
        sdist/
        var/
        wheels/
        *.egg-info/
        .installed.cfg
        *.egg

        # Virtual environments
        .venv/
        venv/
        ENV/

        # IDE
        .idea/
        .vscode/
        *.swp
        *.swo

        # Environment
        .env
        .env.local
        .env.*.local

        # Testing
        .pytest_cache/
        .coverage
        htmlcov/

        # Type checking
        .mypy_cache/

        # Logs
        *.log

        # Database
        *.db
        *.sqlite
        """;
    }

    private String generateReadme(boolean hasJwtAuth) {
        StringBuilder sb = new StringBuilder();

        sb.append("# %s\n\n".formatted(toPascalCase(projectName)));
        sb.append("Generated FastAPI project.\n\n");

        sb.append("## Requirements\n\n");
        sb.append("- Python %s+\n".formatted(PYTHON_VERSION));
        sb.append("- PostgreSQL 15+\n\n");

        sb.append("## Setup\n\n");
        sb.append("```bash\n");
        sb.append("# Create virtual environment\n");
        sb.append("python -m venv .venv\n");
        sb.append("source .venv/bin/activate  # Linux/Mac\n");
        sb.append(".venv\\\\Scripts\\\\activate     # Windows\n\n");
        sb.append("# Install dependencies\n");
        sb.append("pip install -e \".[dev]\"\n\n");
        sb.append("# Copy environment file\n");
        sb.append("cp .env.example .env\n\n");
        sb.append("# Run database migrations\n");
        sb.append("alembic upgrade head\n\n");
        sb.append("# Start development server\n");
        sb.append("uvicorn app.main:app --reload\n");
        sb.append("```\n\n");

        sb.append("## API Documentation\n\n");
        sb.append("- Swagger UI: http://localhost:8000/docs\n");
        sb.append("- ReDoc: http://localhost:8000/redoc\n\n");

        if (hasJwtAuth) {
            sb.append("## Authentication\n\n");
            sb.append("This API uses JWT authentication. Available endpoints:\n\n");
            sb.append("- `POST /api/auth/register` - Register a new user\n");
            sb.append("- `POST /api/auth/login` - Login and get tokens\n");
            sb.append("- `POST /api/auth/refresh` - Refresh access token\n");
            sb.append("- `GET /api/auth/me` - Get current user info\n");
            sb.append("- `POST /api/auth/change-password` - Change password\n\n");
            sb.append("Include the access token in requests:\n");
            sb.append("```\n");
            sb.append("Authorization: Bearer <access_token>\n");
            sb.append("```\n\n");
        }

        sb.append("## Testing\n\n");
        sb.append("```bash\n");
        sb.append("pytest\n");
        sb.append("pytest --cov=app tests/\n");
        sb.append("```\n\n");

        sb.append("## Docker\n\n");
        sb.append("```bash\n");
        sb.append("docker-compose up -d\n");
        sb.append("```\n");

        return sb.toString();
    }

    private String generateDockerfile() {
        return """
        FROM python:%s-slim

        WORKDIR /app

        # Install system dependencies
        RUN apt-get update && apt-get install -y --no-install-recommends \\
            gcc \\
            libpq-dev \\
            && rm -rf /var/lib/apt/lists/*

        # Install Python dependencies
        COPY requirements.txt .
        RUN pip install --no-cache-dir -r requirements.txt

        # Copy application
        COPY . .

        # Create non-root user
        RUN adduser --disabled-password --gecos "" appuser && chown -R appuser /app
        USER appuser

        EXPOSE 8000

        CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
        """
                .formatted(PYTHON_VERSION);
    }

    private String generateDockerCompose(boolean hasRateLimit) {
        StringBuilder sb = new StringBuilder();

        sb.append("services:\n");
        sb.append("  app:\n");
        sb.append("    build: .\n");
        sb.append("    ports:\n");
        sb.append("      - \"8000:8000\"\n");
        sb.append("    environment:\n");
        sb.append(
                "      - DATABASE_URL=postgresql+asyncpg://postgres:postgres@db:5432/%s\n"
                        .formatted(projectName));
        sb.append("      - DEBUG=false\n");

        if (hasRateLimit) {
            sb.append("      - REDIS_URL=redis://redis:6379\n");
        }

        sb.append("    depends_on:\n");
        sb.append("      db:\n");
        sb.append("        condition: service_healthy\n");

        if (hasRateLimit) {
            sb.append("      redis:\n");
            sb.append("        condition: service_healthy\n");
        }

        sb.append("\n  db:\n");
        sb.append("    image: postgres:17-alpine\n");
        sb.append("    environment:\n");
        sb.append("      POSTGRES_USER: postgres\n");
        sb.append("      POSTGRES_PASSWORD: postgres\n");
        sb.append("      POSTGRES_DB: %s\n".formatted(projectName));
        sb.append("    ports:\n");
        sb.append("      - \"5432:5432\"\n");
        sb.append("    volumes:\n");
        sb.append("      - postgres_data:/var/lib/postgresql/data\n");
        sb.append("    healthcheck:\n");
        sb.append("      test: [\"CMD-SHELL\", \"pg_isready -U postgres\"]\n");
        sb.append("      interval: 5s\n");
        sb.append("      timeout: 5s\n");
        sb.append("      retries: 5\n");

        if (hasRateLimit) {
            sb.append("\n  redis:\n");
            sb.append("    image: redis:7-alpine\n");
            sb.append("    ports:\n");
            sb.append("      - \"6379:6379\"\n");
            sb.append("    healthcheck:\n");
            sb.append("      test: [\"CMD\", \"redis-cli\", \"ping\"]\n");
            sb.append("      interval: 5s\n");
            sb.append("      timeout: 5s\n");
            sb.append("      retries: 5\n");
        }

        sb.append("\nvolumes:\n");
        sb.append("  postgres_data:\n");

        return sb.toString();
    }

    private String toSnakeCase(String name) {
        if (name == null) return "";
        return name.replaceAll("([a-z])([A-Z])", "$1_$2").replaceAll("[.-]", "_").toLowerCase();
    }

    private String toPascalCase(String name) {
        if (name == null || name.isEmpty()) return name;
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;
        for (char c : name.toCharArray()) {
            if (c == '_' || c == '-' || c == '.') {
                capitalizeNext = true;
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }
}
