package com.jnzader.apigen.codegen.generator.go.config;

import com.jnzader.apigen.codegen.generator.api.Feature;
import com.jnzader.apigen.codegen.generator.api.ProjectConfig;
import com.jnzader.apigen.codegen.generator.go.GoTypeMapper;
import com.jnzader.apigen.codegen.model.SqlSchema;
import com.jnzader.apigen.codegen.model.SqlTable;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Generates configuration files for Go/Gin projects.
 *
 * <p>Generates:
 *
 * <ul>
 *   <li>go.mod
 *   <li>main.go
 *   <li>.env.example
 *   <li>Dockerfile
 *   <li>docker-compose.yml
 *   <li>Makefile
 *   <li>README.md
 *   <li>.gitignore
 * </ul>
 */
@SuppressWarnings({"java:S1192", "java:S2068", "java:S2479", "java:S3400", "java:S6126"})
public class GoConfigGenerator {

    private final String moduleName;
    private final GoTypeMapper typeMapper;

    public GoConfigGenerator(String moduleName) {
        this.moduleName = moduleName;
        this.typeMapper = new GoTypeMapper();
    }

    /**
     * Generates all configuration files.
     *
     * @param schema the SQL schema
     * @param config the project configuration
     * @return map of file paths to content
     */
    public Map<String, String> generate(SqlSchema schema, ProjectConfig config) {
        return generate(schema, config, false, false);
    }

    /**
     * Generates all configuration files with security features.
     *
     * @param schema the SQL schema
     * @param config the project configuration
     * @param hasJwtAuth whether JWT authentication is enabled
     * @param hasRateLimit whether rate limiting is enabled
     * @return map of file paths to content
     */
    public Map<String, String> generate(
            SqlSchema schema, ProjectConfig config, boolean hasJwtAuth, boolean hasRateLimit) {
        Map<String, String> files = new LinkedHashMap<>();

        files.put("go.mod", generateGoMod(hasJwtAuth, hasRateLimit));
        files.put("main.go", generateMainGo(schema, hasJwtAuth, hasRateLimit));
        files.put(".env.example", generateEnvExample(hasJwtAuth, hasRateLimit));
        files.put(".env", generateEnvExample(hasJwtAuth, hasRateLimit));
        files.put(".gitignore", generateGitignore());
        files.put("README.md", generateReadme(schema, hasJwtAuth, hasRateLimit));
        files.put("Makefile", generateMakefile());

        // Config package
        files.put("internal/config/config.go", generateConfigGo(hasJwtAuth, hasRateLimit));
        files.put("internal/config/database.go", generateDatabaseGo());

        if (config.isFeatureEnabled(Feature.DOCKER)) {
            files.put("Dockerfile", generateDockerfile());
            files.put("docker-compose.yml", generateDockerCompose(hasRateLimit));
            files.put(".dockerignore", generateDockerignore());
        }

        return files;
    }

    private String generateGoMod(boolean hasJwtAuth, boolean hasRateLimit) {
        StringBuilder sb = new StringBuilder();
        sb.append("module ").append(moduleName).append("\n\n");
        sb.append("go 1.23\n\n");
        sb.append("require (\n");
        sb.append("\tgithub.com/gin-gonic/gin v1.10.0\n");
        sb.append("\tgithub.com/go-playground/validator/v10 v10.26.0\n");
        sb.append("\tgithub.com/google/uuid v1.6.0\n");
        sb.append("\tgithub.com/joho/godotenv v1.5.1\n");
        sb.append("\tgithub.com/shopspring/decimal v1.4.0\n");
        sb.append("\tgithub.com/swaggo/files v1.0.1\n");
        sb.append("\tgithub.com/swaggo/gin-swagger v1.6.0\n");
        sb.append("\tgithub.com/swaggo/swag v1.16.4\n");
        sb.append("\tgorm.io/driver/postgres v1.5.11\n");
        sb.append("\tgorm.io/gorm v1.25.12\n");

        if (hasJwtAuth) {
            sb.append("\tgithub.com/golang-jwt/jwt/v5 v5.2.1\n");
            sb.append("\tgolang.org/x/crypto v0.31.0\n");
        }

        if (hasRateLimit) {
            sb.append("\tgithub.com/redis/go-redis/v9 v9.7.0\n");
        }

        sb.append(")\n");
        return sb.toString();
    }

    private String generateMainGo(SqlSchema schema, boolean hasJwtAuth, boolean hasRateLimit) {
        StringBuilder sb = new StringBuilder();

        sb.append("package main\n\n");

        sb.append("import (\n");
        sb.append("\t\"log\"\n");
        sb.append("\t\"os\"\n");
        sb.append("\n");
        if (hasJwtAuth) {
            sb.append("\t\"").append(moduleName).append("/internal/auth\"\n");
        }
        sb.append("\t\"").append(moduleName).append("/internal/config\"\n");
        sb.append("\t\"").append(moduleName).append("/internal/handler\"\n");
        if (hasRateLimit) {
            sb.append("\t\"").append(moduleName).append("/internal/middleware\"\n");
        }
        sb.append("\t\"").append(moduleName).append("/internal/repository\"\n");
        sb.append("\t\"").append(moduleName).append("/internal/router\"\n");
        sb.append("\t\"").append(moduleName).append("/internal/service\"\n");
        sb.append("\t\"github.com/joho/godotenv\"\n");
        sb.append(")\n\n");

        // Swagger info
        sb.append("// @title           ").append(getProjectName()).append(" API\n");
        sb.append("// @version         1.0\n");
        sb.append("// @description     REST API generated by APiGen\n");
        sb.append("// @host            localhost:8080\n");
        sb.append("// @BasePath        /api/v1\n");
        if (hasJwtAuth) {
            sb.append("// @securityDefinitions.apikey BearerAuth\n");
            sb.append("// @in header\n");
            sb.append("// @name Authorization\n");
        }
        sb.append("\n");

        sb.append("func main() {\n");
        sb.append("\t// Load environment variables\n");
        sb.append("\tif err := godotenv.Load(); err != nil {\n");
        sb.append("\t\tlog.Println(\"No .env file found, using environment variables\")\n");
        sb.append("\t}\n\n");

        sb.append("\t// Load configuration\n");
        sb.append("\tcfg := config.Load()\n\n");

        sb.append("\t// Initialize database\n");
        sb.append("\tdb, err := config.NewDatabase(cfg)\n");
        sb.append("\tif err != nil {\n");
        sb.append("\t\tlog.Fatalf(\"Failed to connect to database: %v\", err)\n");
        sb.append("\t}\n\n");

        if (hasJwtAuth) {
            sb.append("\t// Auto-migrate auth user model\n");
            sb.append("\tif err := db.AutoMigrate(&auth.User{}); err != nil {\n");
            sb.append("\t\tlog.Fatalf(\"Failed to migrate auth models: %v\", err)\n");
            sb.append("\t}\n\n");
        }

        sb.append("\t// Initialize repositories\n");
        for (SqlTable table : schema.getEntityTables()) {
            String entityName = typeMapper.toExportedName(table.getEntityName());
            String varName = typeMapper.toUnexportedName(entityName) + "Repo";
            sb.append("\t")
                    .append(varName)
                    .append(" := repository.New")
                    .append(entityName)
                    .append("Repository(db)\n");
        }
        sb.append("\n");

        sb.append("\t// Initialize services\n");
        for (SqlTable table : schema.getEntityTables()) {
            String entityName = typeMapper.toExportedName(table.getEntityName());
            String repoVar = typeMapper.toUnexportedName(entityName) + "Repo";
            String svcVar = typeMapper.toUnexportedName(entityName) + "Svc";
            sb.append("\t")
                    .append(svcVar)
                    .append(" := service.New")
                    .append(entityName)
                    .append("Service(")
                    .append(repoVar)
                    .append(")\n");
        }
        sb.append("\n");

        sb.append("\t// Initialize handlers\n");
        sb.append("\thandlers := &router.Handlers{\n");
        for (SqlTable table : schema.getEntityTables()) {
            String entityName = typeMapper.toExportedName(table.getEntityName());
            String svcVar = typeMapper.toUnexportedName(entityName) + "Svc";
            sb.append("\t\t")
                    .append(entityName)
                    .append(": handler.New")
                    .append(entityName)
                    .append("Handler(")
                    .append(svcVar)
                    .append("),\n");
        }
        sb.append("\t}\n\n");

        sb.append("\t// Setup router\n");
        sb.append("\tr := router.SetupRouter(handlers)\n\n");

        if (hasJwtAuth) {
            sb.append("\t// Setup authentication routes\n");
            sb.append("\tauth.SetupAuthRoutes(r.Group(\"/api/v1\"), db)\n\n");
        }

        if (hasRateLimit) {
            sb.append("\t// Setup rate limiting\n");
            sb.append("\trateLimiter := middleware.NewDefaultRateLimiter()\n");
            sb.append(
                    "\tr.Use(middleware.RateLimiterMiddleware(rateLimiter,"
                            + " middleware.IPKeyFunc))\n\n");
        }

        sb.append("\t// Start server\n");
        sb.append("\tport := os.Getenv(\"PORT\")\n");
        sb.append("\tif port == \"\" {\n");
        sb.append("\t\tport = \"8080\"\n");
        sb.append("\t}\n\n");

        sb.append("\tlog.Printf(\"Server starting on port %s\", port)\n");
        sb.append("\tif err := r.Run(\":\" + port); err != nil {\n");
        sb.append("\t\tlog.Fatalf(\"Failed to start server: %v\", err)\n");
        sb.append("\t}\n");
        sb.append("}\n");

        return sb.toString();
    }

    private String generateConfigGo(boolean hasJwtAuth, boolean hasRateLimit) {
        StringBuilder sb = new StringBuilder();
        sb.append("package config\n\n");
        sb.append("import (\n");
        sb.append("\t\"os\"\n");
        sb.append("\t\"strconv\"\n");
        if (hasRateLimit) {
            sb.append("\t\"time\"\n");
        }
        sb.append(")\n\n");

        sb.append("// Config holds application configuration.\n");
        sb.append("type Config struct {\n");
        sb.append("\tPort        string\n");
        sb.append("\tEnvironment string\n");
        sb.append("\tDatabase    DatabaseConfig\n");
        if (hasJwtAuth) {
            sb.append("\tJWT         JWTConfig\n");
        }
        if (hasRateLimit) {
            sb.append("\tRateLimit   RateLimitConfig\n");
        }
        sb.append("}\n\n");

        sb.append("// DatabaseConfig holds database configuration.\n");
        sb.append("type DatabaseConfig struct {\n");
        sb.append("\tHost     string\n");
        sb.append("\tPort     int\n");
        sb.append("\tUser     string\n");
        sb.append("\tPassword string\n");
        sb.append("\tDBName   string\n");
        sb.append("\tSSLMode  string\n");
        sb.append("}\n\n");

        if (hasJwtAuth) {
            sb.append("// JWTConfig holds JWT configuration.\n");
            sb.append("type JWTConfig struct {\n");
            sb.append("\tSecret           string\n");
            sb.append("\tExpirationHours  int\n");
            sb.append("\tRefreshHours     int\n");
            sb.append("}\n\n");
        }

        if (hasRateLimit) {
            sb.append("// RateLimitConfig holds rate limiting configuration.\n");
            sb.append("type RateLimitConfig struct {\n");
            sb.append("\tEnabled           bool\n");
            sb.append("\tRequestsPerSecond int\n");
            sb.append("\tBurst             int\n");
            sb.append("\tWindow            time.Duration\n");
            sb.append("}\n\n");
        }

        sb.append("// Load loads configuration from environment variables.\n");
        sb.append("func Load() *Config {\n");
        sb.append("\treturn &Config{\n");
        sb.append("\t\tPort:        getEnv(\"PORT\", \"8080\"),\n");
        sb.append("\t\tEnvironment: getEnv(\"ENVIRONMENT\", \"development\"),\n");
        sb.append("\t\tDatabase: DatabaseConfig{\n");
        sb.append("\t\t\tHost:     getEnv(\"DB_HOST\", \"localhost\"),\n");
        sb.append("\t\t\tPort:     getEnvAsInt(\"DB_PORT\", 5432),\n");
        sb.append("\t\t\tUser:     getEnv(\"DB_USER\", \"postgres\"),\n");
        sb.append("\t\t\tPassword: getEnv(\"DB_PASSWORD\", \"postgres\"),\n");
        sb.append("\t\t\tDBName:   getEnv(\"DB_NAME\", \"")
                .append(toSnakeCase(getProjectName()))
                .append("\"),\n");
        sb.append("\t\t\tSSLMode:  getEnv(\"DB_SSLMODE\", \"disable\"),\n");
        sb.append("\t\t},\n");

        if (hasJwtAuth) {
            sb.append("\t\tJWT: JWTConfig{\n");
            sb.append("\t\t\tSecret:          getEnv(\"JWT_SECRET\", \"your-secret-key\"),\n");
            sb.append("\t\t\tExpirationHours: getEnvAsInt(\"JWT_EXPIRATION_HOURS\", 1),\n");
            sb.append("\t\t\tRefreshHours:    getEnvAsInt(\"JWT_REFRESH_HOURS\", 168),\n");
            sb.append("\t\t},\n");
        }

        if (hasRateLimit) {
            sb.append("\t\tRateLimit: RateLimitConfig{\n");
            sb.append("\t\t\tEnabled:           getEnvAsBool(\"RATE_LIMIT_ENABLED\", true),\n");
            sb.append(
                    "\t\t\tRequestsPerSecond: getEnvAsInt(\"RATE_LIMIT_REQUESTS_PER_SECOND\","
                            + " 100),\n");
            sb.append("\t\t\tBurst:             getEnvAsInt(\"RATE_LIMIT_BURST\", 50),\n");
            sb.append(
                    "\t\t\tWindow:           "
                            + " time.Duration(getEnvAsInt(\"RATE_LIMIT_WINDOW_SECONDS\", 1)) *"
                            + " time.Second,\n");
            sb.append("\t\t},\n");
        }

        sb.append("\t}\n");
        sb.append("}\n\n");

        sb.append("func getEnv(key, defaultValue string) string {\n");
        sb.append("\tif value := os.Getenv(key); value != \"\" {\n");
        sb.append("\t\treturn value\n");
        sb.append("\t}\n");
        sb.append("\treturn defaultValue\n");
        sb.append("}\n\n");

        sb.append("func getEnvAsInt(key string, defaultValue int) int {\n");
        sb.append("\tif value := os.Getenv(key); value != \"\" {\n");
        sb.append("\t\tif i, err := strconv.Atoi(value); err == nil {\n");
        sb.append("\t\t\treturn i\n");
        sb.append("\t\t}\n");
        sb.append("\t}\n");
        sb.append("\treturn defaultValue\n");
        sb.append("}\n");

        if (hasRateLimit) {
            sb.append("\nfunc getEnvAsBool(key string, defaultValue bool) bool {\n");
            sb.append("\tif value := os.Getenv(key); value != \"\" {\n");
            sb.append("\t\tif b, err := strconv.ParseBool(value); err == nil {\n");
            sb.append("\t\t\treturn b\n");
            sb.append("\t\t}\n");
            sb.append("\t}\n");
            sb.append("\treturn defaultValue\n");
            sb.append("}\n");
        }

        return sb.toString();
    }

    private String generateDatabaseGo() {
        return """
        package config

        import (
        \t"fmt"
        \t"log"
        \t"time"

        \t"%s/internal/models"
        \t"gorm.io/driver/postgres"
        \t"gorm.io/gorm"
        \t"gorm.io/gorm/logger"
        )

        // NewDatabase creates a new database connection.
        func NewDatabase(cfg *Config) (*gorm.DB, error) {
        \tdsn := fmt.Sprintf(
        \t\t"host=%%s port=%%d user=%%s password=%%s dbname=%%s sslmode=%%s",
        \t\tcfg.Database.Host,
        \t\tcfg.Database.Port,
        \t\tcfg.Database.User,
        \t\tcfg.Database.Password,
        \t\tcfg.Database.DBName,
        \t\tcfg.Database.SSLMode,
        \t)

        \tlogLevel := logger.Info
        \tif cfg.Environment == "production" {
        \t\tlogLevel = logger.Warn
        \t}

        \tdb, err := gorm.Open(postgres.Open(dsn), &gorm.Config{
        \t\tLogger: logger.Default.LogMode(logLevel),
        \t})
        \tif err != nil {
        \t\treturn nil, fmt.Errorf("failed to connect to database: %%w", err)
        \t}

        \t// Configure connection pool
        \tsqlDB, err := db.DB()
        \tif err != nil {
        \t\treturn nil, fmt.Errorf("failed to get sql.DB: %%w", err)
        \t}

        \tsqlDB.SetMaxIdleConns(10)
        \tsqlDB.SetMaxOpenConns(100)
        \tsqlDB.SetConnMaxLifetime(time.Hour)

        \t// Auto-migrate models
        \tif err := autoMigrate(db); err != nil {
        \t\treturn nil, fmt.Errorf("failed to auto-migrate: %%w", err)
        \t}

        \tlog.Println("Database connection established")
        \treturn db, nil
        }

        func autoMigrate(db *gorm.DB) error {
        \treturn db.AutoMigrate(
        \t\t// Add your models here
        \t\t&models.BaseModel{},
        \t)
        }
        """
                .formatted(moduleName);
    }

    private String generateEnvExample(boolean hasJwtAuth, boolean hasRateLimit) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Application\n");
        sb.append("PORT=8080\n");
        sb.append("ENVIRONMENT=development\n\n");

        sb.append("# Database\n");
        sb.append("DB_HOST=localhost\n");
        sb.append("DB_PORT=5432\n");
        sb.append("DB_USER=postgres\n");
        sb.append("DB_PASSWORD=postgres\n");
        sb.append("DB_NAME=").append(toSnakeCase(getProjectName())).append("\n");
        sb.append("DB_SSLMODE=disable\n\n");

        if (hasJwtAuth) {
            sb.append("# JWT Authentication\n");
            sb.append("JWT_SECRET=your-secret-key-change-in-production\n");
            sb.append("JWT_EXPIRATION_HOURS=1\n");
            sb.append("JWT_REFRESH_HOURS=168\n\n");
        }

        if (hasRateLimit) {
            sb.append("# Rate Limiting\n");
            sb.append("RATE_LIMIT_ENABLED=true\n");
            sb.append("RATE_LIMIT_REQUESTS_PER_SECOND=100\n");
            sb.append("RATE_LIMIT_BURST=50\n");
            sb.append("RATE_LIMIT_WINDOW_SECONDS=1\n");
            sb.append("RATE_LIMIT_USE_REDIS=false\n");
            sb.append("REDIS_ADDR=localhost:6379\n");
            sb.append("REDIS_DB=0\n\n");
        }

        return sb.toString();
    }

    private String generateGitignore() {
        return """
        # Binaries
        *.exe
        *.exe~
        *.dll
        *.so
        *.dylib
        /bin/
        /build/

        # Test binary
        *.test

        # Output of go coverage tool
        *.out
        coverage.html

        # Environment files
        .env
        .env.local
        .env.*.local

        # IDE
        .idea/
        .vscode/
        *.swp
        *.swo

        # Vendor (if not using go modules)
        /vendor/

        # OS
        .DS_Store
        Thumbs.db

        # Build artifacts
        /tmp/
        """;
    }

    private String generateReadme(SqlSchema schema, boolean hasJwtAuth, boolean hasRateLimit) {
        StringBuilder sb = new StringBuilder();
        String projectName = getProjectName();

        sb.append("# ").append(projectName).append("\n\n");
        sb.append("REST API generated by APiGen using Go and Gin framework.\n\n");

        sb.append("## Requirements\n\n");
        sb.append("- Go 1.23+\n");
        sb.append("- PostgreSQL 15+\n");
        if (hasRateLimit) {
            sb.append("- Redis 7+ (optional, for distributed rate limiting)\n");
        }
        sb.append("- Make (optional)\n\n");

        sb.append("## Getting Started\n\n");

        sb.append("### 1. Configure environment\n\n");
        sb.append("```bash\n");
        sb.append("cp .env.example .env\n");
        sb.append("# Edit .env with your database credentials\n");
        sb.append("```\n\n");

        sb.append("### 2. Create database\n\n");
        sb.append("```bash\n");
        sb.append("createdb ").append(toSnakeCase(projectName)).append("\n");
        sb.append("```\n\n");

        sb.append("### 3. Download dependencies\n\n");
        sb.append("```bash\n");
        sb.append("go mod download\n");
        sb.append("```\n\n");

        sb.append("### 4. Generate Swagger documentation\n\n");
        sb.append("```bash\n");
        sb.append("make swagger\n");
        sb.append("# or\n");
        sb.append("swag init\n");
        sb.append("```\n\n");

        sb.append("### 5. Run the application\n\n");
        sb.append("```bash\n");
        sb.append("# Development\n");
        sb.append("make run\n");
        sb.append("# or\n");
        sb.append("go run main.go\n\n");
        sb.append("# Production build\n");
        sb.append("make build\n");
        sb.append("./bin/").append(toSnakeCase(projectName)).append("\n");
        sb.append("```\n\n");

        sb.append("## API Documentation\n\n");
        sb.append("Once the application is running, visit:\n");
        sb.append("- Swagger UI: http://localhost:8080/swagger/index.html\n");
        sb.append("- Health check: http://localhost:8080/health\n\n");

        if (hasJwtAuth) {
            sb.append("## Authentication\n\n");
            sb.append("This API uses JWT for authentication.\n\n");
            sb.append("### Endpoints\n\n");
            sb.append("| Method | Endpoint | Description |\n");
            sb.append("|--------|----------|-------------|\n");
            sb.append("| POST | `/api/v1/auth/register` | Register a new user |\n");
            sb.append("| POST | `/api/v1/auth/login` | Login and get tokens |\n");
            sb.append("| POST | `/api/v1/auth/refresh` | Refresh access token |\n");
            sb.append("| GET | `/api/v1/auth/profile` | Get current user |\n");
            sb.append("| PUT | `/api/v1/auth/password` | Change password |\n\n");
            sb.append("### Usage\n\n");
            sb.append("1. Register or login to get access token\n");
            sb.append("2. Include the token in the Authorization header:\n");
            sb.append("   ```\n");
            sb.append("   Authorization: Bearer <access_token>\n");
            sb.append("   ```\n\n");
        }

        if (hasRateLimit) {
            sb.append("## Rate Limiting\n\n");
            sb.append("This API implements rate limiting to prevent abuse.\n\n");
            sb.append("### Configuration\n\n");
            sb.append("| Setting | Default | Description |\n");
            sb.append("|---------|---------|-------------|\n");
            sb.append("| RATE_LIMIT_REQUESTS_PER_SECOND | 100 | Max requests per second |\n");
            sb.append("| RATE_LIMIT_BURST | 50 | Burst size |\n\n");
            sb.append("### Response Headers\n\n");
            sb.append("- `X-RateLimit-Limit`: Max requests allowed\n");
            sb.append("- `X-RateLimit-Remaining`: Remaining requests\n");
            sb.append("- `X-RateLimit-Reset`: Reset timestamp\n\n");
        }

        sb.append("## Available Endpoints\n\n");
        for (SqlTable table : schema.getEntityTables()) {
            String entityName = table.getEntityName();
            String pluralPath = typeMapper.toSnakeCase(typeMapper.pluralize(entityName));
            sb.append("### ").append(entityName).append("\n\n");
            sb.append("| Method | Endpoint | Description |\n");
            sb.append("|--------|----------|-------------|\n");
            sb.append("| GET | `/api/v1/")
                    .append(pluralPath)
                    .append("` | List all with pagination |\n");
            sb.append("| GET | `/api/v1/").append(pluralPath).append("/:id` | Get by ID |\n");
            sb.append("| POST | `/api/v1/").append(pluralPath).append("` | Create new |\n");
            sb.append("| PUT | `/api/v1/").append(pluralPath).append("/:id` | Update |\n");
            sb.append("| DELETE | `/api/v1/")
                    .append(pluralPath)
                    .append("/:id` | Soft delete |\n\n");
        }

        sb.append("## Makefile Commands\n\n");
        sb.append("```bash\n");
        sb.append("make run        # Run the application\n");
        sb.append("make build      # Build the binary\n");
        sb.append("make test       # Run tests\n");
        sb.append("make lint       # Run linter\n");
        sb.append("make swagger    # Generate Swagger docs\n");
        sb.append("make clean      # Clean build artifacts\n");
        sb.append("```\n\n");

        sb.append("## Docker\n\n");
        sb.append("```bash\n");
        sb.append("# Build and run with Docker Compose\n");
        sb.append("docker-compose up -d\n\n");
        sb.append("# Build image only\n");
        sb.append("docker build -t ").append(toSnakeCase(projectName)).append(" .\n");
        sb.append("```\n\n");

        sb.append("## Project Structure\n\n");
        sb.append("```\n");
        sb.append(".\n");
        sb.append("├── main.go                 # Application entry point\n");
        sb.append("├── internal/\n");
        if (hasJwtAuth) {
            sb.append("│   ├── auth/              # Authentication\n");
        }
        sb.append("│   ├── config/            # Configuration\n");
        sb.append("│   ├── dto/               # Data Transfer Objects\n");
        sb.append("│   ├── handler/           # HTTP handlers\n");
        if (hasRateLimit) {
            sb.append("│   ├── middleware/        # Middleware (rate limiting)\n");
        }
        sb.append("│   ├── models/            # GORM models\n");
        sb.append("│   ├── repository/        # Data access layer\n");
        sb.append("│   ├── router/            # Route definitions\n");
        sb.append("│   └── service/           # Business logic\n");
        sb.append("├── docs/                  # Swagger documentation\n");
        sb.append("├── Dockerfile\n");
        sb.append("└── docker-compose.yml\n");
        sb.append("```\n\n");

        sb.append("## License\n\n");
        sb.append("MIT\n");

        return sb.toString();
    }

    private String generateMakefile() {
        String binaryName = toSnakeCase(getProjectName());
        return """
        .PHONY: run build test lint swagger clean help

        BINARY_NAME=%s
        MAIN_PATH=.

        ## help: Show this help message
        help:
        \t@echo "Usage: make [target]"
        \t@echo ""
        \t@echo "Targets:"
        \t@sed -n 's/^##//p' ${MAKEFILE_LIST} | column -t -s ':' | sed -e 's/^/ /'

        ## run: Run the application
        run:
        \tgo run $(MAIN_PATH)/main.go

        ## build: Build the binary
        build:
        \tgo build -o bin/$(BINARY_NAME) $(MAIN_PATH)/main.go

        ## test: Run tests
        test:
        \tgo test -v -race -coverprofile=coverage.out ./...

        ## test-coverage: Run tests and show coverage
        test-coverage: test
        \tgo tool cover -html=coverage.out -o coverage.html

        ## lint: Run linter
        lint:
        \tgolangci-lint run ./...

        ## swagger: Generate Swagger documentation
        swagger:
        \tswag init

        ## clean: Clean build artifacts
        clean:
        \trm -rf bin/
        \trm -f coverage.out coverage.html

        ## tidy: Tidy go modules
        tidy:
        \tgo mod tidy

        ## docker-build: Build Docker image
        docker-build:
        \tdocker build -t $(BINARY_NAME) .

        ## docker-run: Run with Docker Compose
        docker-run:
        \tdocker-compose up -d

        ## docker-stop: Stop Docker Compose
        docker-stop:
        \tdocker-compose down
        """
                .formatted(binaryName);
    }

    private String generateDockerfile() {
        String binaryName = toSnakeCase(getProjectName());
        return """
        # Build stage
        FROM golang:1.23-alpine AS builder

        WORKDIR /app

        # Install build dependencies
        RUN apk add --no-cache git

        # Copy go.mod and go.sum
        COPY go.mod go.sum ./
        RUN go mod download

        # Copy source code
        COPY . .

        # Build the binary
        RUN CGO_ENABLED=0 GOOS=linux go build -a -installsuffix cgo -o bin/%s main.go

        # Production stage
        FROM alpine:3.21

        WORKDIR /app

        # Install ca-certificates for HTTPS
        RUN apk --no-cache add ca-certificates tzdata

        # Copy binary from builder
        COPY --from=builder /app/bin/%s .

        # Create non-root user
        RUN adduser -D -g '' appuser
        USER appuser

        EXPOSE 8080

        ENTRYPOINT ["./%s"]
        """
                .formatted(binaryName, binaryName, binaryName);
    }

    private String generateDockerCompose(boolean hasRateLimit) {
        String projectName = toSnakeCase(getProjectName());
        StringBuilder sb = new StringBuilder();

        sb.append("services:\n");
        sb.append("  app:\n");
        sb.append("    build: .\n");
        sb.append("    container_name: ").append(projectName).append("-api\n");
        sb.append("    ports:\n");
        sb.append("      - \"8080:8080\"\n");
        sb.append("    environment:\n");
        sb.append("      - PORT=8080\n");
        sb.append("      - ENVIRONMENT=production\n");
        sb.append("      - DB_HOST=db\n");
        sb.append("      - DB_PORT=5432\n");
        sb.append("      - DB_USER=postgres\n");
        sb.append("      - DB_PASSWORD=postgres\n");
        sb.append("      - DB_NAME=").append(projectName).append("\n");
        sb.append("      - DB_SSLMODE=disable\n");
        if (hasRateLimit) {
            sb.append("      - RATE_LIMIT_USE_REDIS=true\n");
            sb.append("      - REDIS_ADDR=redis:6379\n");
        }
        sb.append("    depends_on:\n");
        sb.append("      db:\n");
        sb.append("        condition: service_healthy\n");
        if (hasRateLimit) {
            sb.append("      redis:\n");
            sb.append("        condition: service_healthy\n");
        }
        sb.append("    restart: unless-stopped\n\n");

        sb.append("  db:\n");
        sb.append("    image: postgres:17-alpine\n");
        sb.append("    container_name: ").append(projectName).append("-db\n");
        sb.append("    environment:\n");
        sb.append("      - POSTGRES_USER=postgres\n");
        sb.append("      - POSTGRES_PASSWORD=postgres\n");
        sb.append("      - POSTGRES_DB=").append(projectName).append("\n");
        sb.append("    volumes:\n");
        sb.append("      - postgres_data:/var/lib/postgresql/data\n");
        sb.append("    ports:\n");
        sb.append("      - \"5432:5432\"\n");
        sb.append("    healthcheck:\n");
        sb.append("      test: [\"CMD-SHELL\", \"pg_isready -U postgres\"]\n");
        sb.append("      interval: 10s\n");
        sb.append("      timeout: 5s\n");
        sb.append("      retries: 5\n");
        sb.append("    restart: unless-stopped\n");

        if (hasRateLimit) {
            sb.append("\n  redis:\n");
            sb.append("    image: redis:7-alpine\n");
            sb.append("    container_name: ").append(projectName).append("-redis\n");
            sb.append("    ports:\n");
            sb.append("      - \"6379:6379\"\n");
            sb.append("    volumes:\n");
            sb.append("      - redis_data:/data\n");
            sb.append("    healthcheck:\n");
            sb.append("      test: [\"CMD\", \"redis-cli\", \"ping\"]\n");
            sb.append("      interval: 10s\n");
            sb.append("      timeout: 5s\n");
            sb.append("      retries: 5\n");
            sb.append("    restart: unless-stopped\n");
        }

        sb.append("\nvolumes:\n");
        sb.append("  postgres_data:\n");
        if (hasRateLimit) {
            sb.append("  redis_data:\n");
        }

        return sb.toString();
    }

    private String generateDockerignore() {
        return """
        bin/
        tmp/
        *.exe
        *.test
        *.out
        .git
        .gitignore
        .env
        .env.local
        *.md
        .idea
        .vscode
        coverage.out
        coverage.html
        """;
    }

    private String getProjectName() {
        // Extract project name from module name (last part after /)
        int lastSlash = moduleName.lastIndexOf('/');
        if (lastSlash >= 0) {
            return moduleName.substring(lastSlash + 1);
        }
        return moduleName;
    }

    private String toSnakeCase(String name) {
        return name.replaceAll("([a-z])([A-Z])", "$1_$2")
                .replace("-", "_")
                .toLowerCase(Locale.ROOT);
    }
}
