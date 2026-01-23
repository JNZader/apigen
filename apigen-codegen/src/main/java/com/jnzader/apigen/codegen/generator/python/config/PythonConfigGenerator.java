package com.jnzader.apigen.codegen.generator.python.config;

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
public class PythonConfigGenerator {

    private static final String PYTHON_VERSION = "3.12";
    private static final String FASTAPI_VERSION = "0.115.0";
    private static final String SQLALCHEMY_VERSION = "2.0.36";
    private static final String PYDANTIC_VERSION = "2.10.0";

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

        files.put("pyproject.toml", generatePyprojectToml());
        files.put("requirements.txt", generateRequirements());
        files.put("app/__init__.py", "");
        files.put("app/main.py", generateMainPy(schema));
        files.put("app/core/__init__.py", "");
        files.put("app/core/config.py", generateConfigPy());
        files.put("app/core/database.py", generateDatabasePy());
        files.put(".env.example", generateEnvExample());
        files.put(".gitignore", generateGitignore());
        files.put("README.md", generateReadme());
        files.put("Dockerfile", generateDockerfile());
        files.put("docker-compose.yml", generateDockerCompose());

        return files;
    }

    private String generatePyprojectToml() {
        return """
        [project]
        name = "%s"
        version = "0.1.0"
        description = "Generated FastAPI project"
        readme = "README.md"
        requires-python = ">=%s"
        dependencies = [
            "fastapi>=%s",
            "uvicorn[standard]>=0.32.0",
            "sqlalchemy>=%s",
            "asyncpg>=0.30.0",
            "pydantic>=%s",
            "pydantic-settings>=2.6.0",
            "alembic>=1.14.0",
            "python-dotenv>=1.0.0",
        ]

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
                .formatted(
                        projectName,
                        PYTHON_VERSION,
                        FASTAPI_VERSION,
                        SQLALCHEMY_VERSION,
                        PYDANTIC_VERSION,
                        PYTHON_VERSION);
    }

    private String generateRequirements() {
        return """
        fastapi>=%s
        uvicorn[standard]>=0.32.0
        sqlalchemy>=%s
        asyncpg>=0.30.0
        pydantic>=%s
        pydantic-settings>=2.6.0
        alembic>=1.14.0
        python-dotenv>=1.0.0
        """
                .formatted(FASTAPI_VERSION, SQLALCHEMY_VERSION, PYDANTIC_VERSION);
    }

    private String generateMainPy(SqlSchema schema) {
        StringBuilder sb = new StringBuilder();

        sb.append("from contextlib import asynccontextmanager\n");
        sb.append("from fastapi import FastAPI\n");
        sb.append("from fastapi.middleware.cors import CORSMiddleware\n");
        sb.append("\n");
        sb.append("from app.core.config import settings\n");
        sb.append("from app.core.database import engine\n");
        sb.append("from app.models.base import Base\n");

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

        // CORS middleware
        sb.append("# CORS middleware\n");
        sb.append("app.add_middleware(\n");
        sb.append("    CORSMiddleware,\n");
        sb.append("    allow_origins=settings.cors_origins,\n");
        sb.append("    allow_credentials=True,\n");
        sb.append("    allow_methods=[\"*\"],\n");
        sb.append("    allow_headers=[\"*\"],\n");
        sb.append(")\n\n");

        // Include routers
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

    private String generateConfigPy() {
        return """
        from pydantic_settings import BaseSettings, SettingsConfigDict


        class Settings(BaseSettings):
            \"\"\"Application settings.\"\"\"

            model_config = SettingsConfigDict(
                env_file=".env",
                env_file_encoding="utf-8",
                case_sensitive=False,
            )

            # Application
            app_name: str = "%s"
            debug: bool = False
            host: str = "0.0.0.0"
            port: int = 8000

            # Database
            database_url: str = "postgresql+asyncpg://postgres:postgres@localhost:5432/%s"

            # CORS
            cors_origins: list[str] = ["http://localhost:3000", "http://localhost:8080"]

            # Pagination
            default_page_size: int = 10
            max_page_size: int = 100


        settings = Settings()
        """
                .formatted(toPascalCase(projectName), projectName);
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
            \"\"\"Dependency to get database session.\"\"\"
            async with async_session_maker() as session:
                try:
                    yield session
                finally:
                    await session.close()
        """;
    }

    private String generateEnvExample() {
        return """
        # Application
        APP_NAME=%s
        DEBUG=true
        HOST=0.0.0.0
        PORT=8000

        # Database
        DATABASE_URL=postgresql+asyncpg://postgres:postgres@localhost:5432/%s

        # CORS (comma-separated)
        CORS_ORIGINS=["http://localhost:3000"]
        """
                .formatted(toPascalCase(projectName), projectName);
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

    private String generateReadme() {
        return """
        # %s

        Generated FastAPI project.

        ## Requirements

        - Python %s+
        - PostgreSQL 15+

        ## Setup

        ```bash
        # Create virtual environment
        python -m venv .venv
        source .venv/bin/activate  # Linux/Mac
        .venv\\Scripts\\activate     # Windows

        # Install dependencies
        pip install -e ".[dev]"

        # Copy environment file
        cp .env.example .env

        # Run database migrations
        alembic upgrade head

        # Start development server
        uvicorn app.main:app --reload
        ```

        ## API Documentation

        - Swagger UI: http://localhost:8000/docs
        - ReDoc: http://localhost:8000/redoc

        ## Testing

        ```bash
        pytest
        pytest --cov=app tests/
        ```

        ## Docker

        ```bash
        docker-compose up -d
        ```
        """
                .formatted(toPascalCase(projectName), PYTHON_VERSION);
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

    private String generateDockerCompose() {
        return """
        services:
          app:
            build: .
            ports:
              - "8000:8000"
            environment:
              - DATABASE_URL=postgresql+asyncpg://postgres:postgres@db:5432/%s
              - DEBUG=false
            depends_on:
              db:
                condition: service_healthy

          db:
            image: postgres:17-alpine
            environment:
              POSTGRES_USER: postgres
              POSTGRES_PASSWORD: postgres
              POSTGRES_DB: %s
            ports:
              - "5432:5432"
            volumes:
              - postgres_data:/var/lib/postgresql/data
            healthcheck:
              test: ["CMD-SHELL", "pg_isready -U postgres"]
              interval: 5s
              timeout: 5s
              retries: 5

        volumes:
          postgres_data:
        """
                .formatted(projectName, projectName);
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
