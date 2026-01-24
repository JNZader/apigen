package com.jnzader.apigen.codegen.generator.php.config;

import com.jnzader.apigen.codegen.generator.api.ProjectConfig;
import com.jnzader.apigen.codegen.generator.php.PhpTypeMapper;
import com.jnzader.apigen.codegen.model.SqlSchema;
import com.jnzader.apigen.codegen.model.SqlTable;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Generates configuration files for Laravel projects.
 *
 * <p>Generates:
 *
 * <ul>
 *   <li>composer.json - Project dependencies
 *   <li>routes/api.php - API routes
 *   <li>.env.example - Environment template
 *   <li>config files
 *   <li>Dockerfile and docker-compose.yml
 * </ul>
 */
public class PhpConfigGenerator {

    private static final String PHP_VERSION = "8.4";
    private static final String LARAVEL_VERSION = "^12.0";

    private final String projectName;
    private final PhpTypeMapper typeMapper;

    public PhpConfigGenerator(String projectName) {
        this.projectName = toSnakeCase(projectName);
        this.typeMapper = new PhpTypeMapper();
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

        files.put("composer.json", generateComposerJson(hasJwtAuth, hasRateLimit));
        files.put("routes/api.php", generateApiRoutes(schema, hasJwtAuth));
        files.put(".env.example", generateEnvExample(hasJwtAuth, hasRateLimit));
        files.put(".gitignore", generateGitignore());
        files.put("README.md", generateReadme(hasJwtAuth, hasRateLimit));
        files.put("Dockerfile", generateDockerfile());
        files.put("docker-compose.yml", generateDockerCompose(hasRateLimit));
        files.put("config/api.php", generateApiConfig());
        files.put("app/Providers/AppServiceProvider.php", generateAppServiceProvider(schema));

        return files;
    }

    @SuppressWarnings("UnusedVariable") // hasRateLimit reserved for future Redis dependencies
    private String generateComposerJson(boolean hasJwtAuth, boolean hasRateLimit) {
        StringBuilder require = new StringBuilder();
        require.append("        \"php\": \"^").append(PHP_VERSION).append("\",\n");
        require.append("        \"laravel/framework\": \"").append(LARAVEL_VERSION).append("\",\n");
        require.append("        \"darkaonline/l5-swagger\": \"^8.6\",\n");
        require.append("        \"spatie/laravel-query-builder\": \"^6.0\"");

        if (hasJwtAuth) {
            require.append(",\n        \"laravel/sanctum\": \"^4.0\"");
        }

        return """
        {
            "name": "apigen/%s",
            "type": "project",
            "description": "Generated Laravel API project",
            "keywords": ["laravel", "api", "rest"],
            "license": "MIT",
            "require": {
        %s
            },
            "require-dev": {
                "fakerphp/faker": "^1.23",
                "laravel/pint": "^1.13",
                "laravel/sail": "^1.26",
                "mockery/mockery": "^1.6",
                "nunomaduro/collision": "^8.1",
                "pestphp/pest": "^2.34",
                "pestphp/pest-plugin-laravel": "^2.3"
            },
            "autoload": {
                "psr-4": {
                    "App\\\\": "app/",
                    "Database\\\\Factories\\\\": "database/factories/",
                    "Database\\\\Seeders\\\\": "database/seeders/"
                }
            },
            "autoload-dev": {
                "psr-4": {
                    "Tests\\\\": "tests/"
                }
            },
            "scripts": {
                "post-autoload-dump": [
                    "Illuminate\\\\Foundation\\\\ComposerScripts::postAutoloadDump",
                    "@php artisan package:discover --ansi"
                ],
                "post-update-cmd": [
                    "@php artisan vendor:publish --tag=laravel-assets --ansi --force"
                ],
                "test": "./vendor/bin/pest",
                "lint": "./vendor/bin/pint",
                "swagger": "php artisan l5-swagger:generate"
            },
            "extra": {
                "laravel": {
                    "dont-discover": []
                }
            },
            "config": {
                "optimize-autoloader": true,
                "preferred-install": "dist",
                "sort-packages": true,
                "allow-plugins": {
                    "pestphp/pest-plugin": true,
                    "php-http/discovery": true
                }
            },
            "minimum-stability": "stable",
            "prefer-stable": true
        }
        """
                .formatted(projectName, require);
    }

    private String generateApiRoutes(SqlSchema schema, boolean hasJwtAuth) {
        StringBuilder sb = new StringBuilder();

        sb.append("<?php\n\n");
        sb.append("use Illuminate\\Support\\Facades\\Route;\n");

        // Import controllers
        for (SqlTable table : schema.getEntityTables()) {
            String className = table.getEntityName();
            sb.append("use App\\Http\\Controllers\\Api\\V1\\")
                    .append(className)
                    .append("Controller;\n");
        }

        sb.append("\n/*\n");
        sb.append("|--------------------------------------------------------------------------\n");
        sb.append("| API Routes\n");
        sb.append("|--------------------------------------------------------------------------\n");
        sb.append("*/\n\n");

        // Health check
        sb.append("Route::get('/health', fn () => response()->json(['status' => 'healthy']));\n\n");

        // Include auth routes if JWT is enabled
        if (hasJwtAuth) {
            sb.append("// Include authentication routes\n");
            sb.append("require __DIR__ . '/auth.php';\n\n");
        }

        // API v1 routes
        sb.append("Route::prefix('v1')");
        if (hasJwtAuth) {
            sb.append("->middleware('auth:sanctum')");
        }
        sb.append("->group(function () {\n");

        for (SqlTable table : schema.getEntityTables()) {
            String className = table.getEntityName();
            String pluralName =
                    typeMapper
                            .pluralize(typeMapper.toCamelCase(className))
                            .toLowerCase(Locale.ROOT);

            sb.append("    // ").append(className).append(" routes\n");
            sb.append("    Route::apiResource('")
                    .append(pluralName)
                    .append("', ")
                    .append(className)
                    .append("Controller::class);\n");
            sb.append("    Route::post('")
                    .append(pluralName)
                    .append("/{id}/restore', [")
                    .append(className)
                    .append("Controller::class, 'restore']);\n\n");
        }

        sb.append("});\n");

        return sb.toString();
    }

    private String generateEnvExample(boolean hasJwtAuth, boolean hasRateLimit) {
        StringBuilder sb = new StringBuilder();
        sb.append("APP_NAME=").append(toPascalCase(projectName)).append("\n");
        sb.append("APP_ENV=local\n");
        sb.append("APP_KEY=\n");
        sb.append("APP_DEBUG=true\n");
        sb.append("APP_TIMEZONE=UTC\n");
        sb.append("APP_URL=http://localhost:8000\n\n");

        sb.append("LOG_CHANNEL=stack\n");
        sb.append("LOG_DEPRECATIONS_CHANNEL=null\n");
        sb.append("LOG_LEVEL=debug\n\n");

        sb.append("DB_CONNECTION=pgsql\n");
        sb.append("DB_HOST=127.0.0.1\n");
        sb.append("DB_PORT=5432\n");
        sb.append("DB_DATABASE=").append(projectName).append("\n");
        sb.append("DB_USERNAME=postgres\n");
        sb.append("DB_PASSWORD=postgres\n\n");

        sb.append("BROADCAST_DRIVER=log\n");
        sb.append("CACHE_DRIVER=").append(hasRateLimit ? "redis" : "file").append("\n");
        sb.append("FILESYSTEM_DISK=local\n");
        sb.append("QUEUE_CONNECTION=sync\n");
        sb.append("SESSION_DRIVER=file\n");
        sb.append("SESSION_LIFETIME=120\n\n");

        if (hasRateLimit) {
            sb.append("# Redis Configuration\n");
            sb.append("REDIS_HOST=127.0.0.1\n");
            sb.append("REDIS_PASSWORD=null\n");
            sb.append("REDIS_PORT=6379\n\n");
        }

        if (hasJwtAuth) {
            sb.append("# JWT Authentication\n");
            sb.append(
                    "SANCTUM_STATEFUL_DOMAINS=localhost,localhost:3000,127.0.0.1,127.0.0.1:8000\n");
            sb.append("JWT_ACCESS_EXPIRATION=60\n");
            sb.append("JWT_REFRESH_EXPIRATION=10080\n\n");
        }

        if (hasRateLimit) {
            sb.append("# Rate Limiting\n");
            sb.append("RATE_LIMIT_API=60\n");
            sb.append("RATE_LIMIT_AUTH=5\n");
            sb.append("RATE_LIMIT_HEAVY=10\n");
            sb.append("RATE_LIMIT_CACHE_DRIVER=redis\n\n");
        }

        sb.append("L5_SWAGGER_GENERATE_ALWAYS=true\n");
        sb.append("L5_SWAGGER_CONST_HOST=http://localhost:8000/api\n\n");

        sb.append("# Pagination\n");
        sb.append("API_DEFAULT_PAGE_SIZE=10\n");
        sb.append("API_MAX_PAGE_SIZE=100\n");

        return sb.toString();
    }

    private String generateGitignore() {
        return """
        /.phpunit.cache
        /node_modules
        /public/build
        /public/hot
        /public/storage
        /storage/*.key
        /storage/pail
        /vendor
        .env
        .env.backup
        .env.production
        .phpactor.json
        .phpunit.result.cache
        Homestead.json
        Homestead.yaml
        npm-debug.log
        yarn-error.log
        /.fleet
        /.idea
        /.vscode
        """;
    }

    private String generateReadme(boolean hasJwtAuth, boolean hasRateLimit) {
        StringBuilder sb = new StringBuilder();

        sb.append("# ").append(toPascalCase(projectName)).append("\n\n");
        sb.append("Generated Laravel API project.\n\n");

        sb.append("## Requirements\n\n");
        sb.append("- PHP ").append(PHP_VERSION).append("+\n");
        sb.append("- Composer 2.x\n");
        sb.append("- PostgreSQL 15+\n");
        if (hasRateLimit) {
            sb.append("- Redis 7+ (for rate limiting)\n");
        }
        sb.append("\n");

        sb.append("## Setup\n\n");
        sb.append("```bash\n");
        sb.append("# Install dependencies\n");
        sb.append("composer install\n\n");
        sb.append("# Copy environment file\n");
        sb.append("cp .env.example .env\n\n");
        sb.append("# Generate application key\n");
        sb.append("php artisan key:generate\n\n");
        sb.append("# Run migrations\n");
        sb.append("php artisan migrate\n\n");
        sb.append("# Start development server\n");
        sb.append("php artisan serve\n");
        sb.append("```\n\n");

        if (hasJwtAuth) {
            sb.append("## Authentication\n\n");
            sb.append("This API uses Laravel Sanctum for authentication.\n\n");
            sb.append("### Endpoints\n\n");
            sb.append("| Method | Endpoint | Description |\n");
            sb.append("|--------|----------|-------------|\n");
            sb.append("| POST | `/api/auth/register` | Register a new user |\n");
            sb.append("| POST | `/api/auth/login` | Login and get tokens |\n");
            sb.append("| POST | `/api/auth/refresh` | Refresh access token |\n");
            sb.append("| POST | `/api/auth/logout` | Logout (revoke tokens) |\n");
            sb.append("| GET | `/api/auth/profile` | Get current user |\n");
            sb.append("| PUT | `/api/auth/password` | Change password |\n\n");
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
            sb.append("### Default Limits\n\n");
            sb.append("| Tier | Limit | Window |\n");
            sb.append("|------|-------|--------|\n");
            sb.append("| API | 60 requests | 1 minute |\n");
            sb.append("| Auth | 5 requests | 1 minute |\n");
            sb.append("| Heavy | 10 requests | 1 minute |\n\n");
        }

        sb.append("## API Documentation\n\n");
        sb.append("- Swagger UI: http://localhost:8000/api/documentation\n\n");
        sb.append("Generate Swagger docs:\n");
        sb.append("```bash\n");
        sb.append("php artisan l5-swagger:generate\n");
        sb.append("```\n\n");

        sb.append("## Testing\n\n");
        sb.append("```bash\n");
        sb.append("# Run tests\n");
        sb.append("composer test\n\n");
        sb.append("# Run tests with coverage\n");
        sb.append("./vendor/bin/pest --coverage\n");
        sb.append("```\n\n");

        sb.append("## Code Style\n\n");
        sb.append("```bash\n");
        sb.append("# Fix code style\n");
        sb.append("composer lint\n");
        sb.append("```\n\n");

        sb.append("## Docker\n\n");
        sb.append("```bash\n");
        sb.append("docker-compose up -d\n");
        sb.append("```\n");

        return sb.toString();
    }

    private String generateDockerfile() {
        return """
        FROM php:%s-fpm-alpine

        # Install system dependencies
        RUN apk add --no-cache \\
            git \\
            curl \\
            libpng-dev \\
            oniguruma-dev \\
            libxml2-dev \\
            zip \\
            unzip \\
            postgresql-dev

        # Install PHP extensions
        RUN docker-php-ext-install pdo pdo_pgsql mbstring exif pcntl bcmath gd

        # Get latest Composer
        COPY --from=composer:latest /usr/bin/composer /usr/bin/composer

        # Set working directory
        WORKDIR /var/www

        # Copy existing application directory
        COPY . .

        # Install dependencies
        RUN composer install --optimize-autoloader --no-dev

        # Change ownership
        RUN chown -R www-data:www-data /var/www

        # Expose port 9000
        EXPOSE 9000

        CMD ["php-fpm"]
        """
                .formatted(PHP_VERSION);
    }

    private String generateDockerCompose(boolean hasRateLimit) {
        StringBuilder sb = new StringBuilder();

        sb.append("services:\n");
        sb.append("  app:\n");
        sb.append("    build:\n");
        sb.append("      context: .\n");
        sb.append("      dockerfile: Dockerfile\n");
        sb.append("    container_name: ").append(projectName).append("_app\n");
        sb.append("    restart: unless-stopped\n");
        sb.append("    working_dir: /var/www\n");
        sb.append("    volumes:\n");
        sb.append("      - .:/var/www\n");
        sb.append("    networks:\n");
        sb.append("      - app-network\n");
        sb.append("    depends_on:\n");
        sb.append("      - db\n");
        if (hasRateLimit) {
            sb.append("      - redis\n");
        }
        sb.append("\n");

        sb.append("  nginx:\n");
        sb.append("    image: nginx:alpine\n");
        sb.append("    container_name: ").append(projectName).append("_nginx\n");
        sb.append("    restart: unless-stopped\n");
        sb.append("    ports:\n");
        sb.append("      - \"8000:80\"\n");
        sb.append("    volumes:\n");
        sb.append("      - .:/var/www\n");
        sb.append("      - ./docker/nginx:/etc/nginx/conf.d\n");
        sb.append("    networks:\n");
        sb.append("      - app-network\n\n");

        sb.append("  db:\n");
        sb.append("    image: postgres:17-alpine\n");
        sb.append("    container_name: ").append(projectName).append("_db\n");
        sb.append("    restart: unless-stopped\n");
        sb.append("    environment:\n");
        sb.append("      POSTGRES_USER: postgres\n");
        sb.append("      POSTGRES_PASSWORD: postgres\n");
        sb.append("      POSTGRES_DB: ").append(projectName).append("\n");
        sb.append("    ports:\n");
        sb.append("      - \"5432:5432\"\n");
        sb.append("    volumes:\n");
        sb.append("      - postgres_data:/var/lib/postgresql/data\n");
        sb.append("    healthcheck:\n");
        sb.append("      test: [\"CMD-SHELL\", \"pg_isready -U postgres\"]\n");
        sb.append("      interval: 5s\n");
        sb.append("      timeout: 5s\n");
        sb.append("      retries: 5\n");
        sb.append("    networks:\n");
        sb.append("      - app-network\n");

        if (hasRateLimit) {
            sb.append("\n");
            sb.append("  redis:\n");
            sb.append("    image: redis:7-alpine\n");
            sb.append("    container_name: ").append(projectName).append("_redis\n");
            sb.append("    restart: unless-stopped\n");
            sb.append("    ports:\n");
            sb.append("      - \"6379:6379\"\n");
            sb.append("    volumes:\n");
            sb.append("      - redis_data:/data\n");
            sb.append("    healthcheck:\n");
            sb.append("      test: [\"CMD\", \"redis-cli\", \"ping\"]\n");
            sb.append("      interval: 5s\n");
            sb.append("      timeout: 5s\n");
            sb.append("      retries: 5\n");
            sb.append("    networks:\n");
            sb.append("      - app-network\n");
        }

        sb.append("\nnetworks:\n");
        sb.append("  app-network:\n");
        sb.append("    driver: bridge\n");

        sb.append("\nvolumes:\n");
        sb.append("  postgres_data:\n");
        if (hasRateLimit) {
            sb.append("  redis_data:\n");
        }

        return sb.toString();
    }

    private String generateApiConfig() {
        return """
        <?php

        return [
            /*
            |--------------------------------------------------------------------------
            | API Configuration
            |--------------------------------------------------------------------------
            */

            'version' => 'v1',

            'pagination' => [
                'default_size' => (int) env('API_DEFAULT_PAGE_SIZE', 10),
                'max_size' => (int) env('API_MAX_PAGE_SIZE', 100),
            ],

            'rate_limiting' => [
                'enabled' => true,
                'max_attempts' => 60,
                'decay_minutes' => 1,
            ],
        ];
        """;
    }

    private String generateAppServiceProvider(SqlSchema schema) {
        StringBuilder sb = new StringBuilder();

        sb.append("<?php\n\n");
        sb.append("namespace App\\Providers;\n\n");
        sb.append("use Illuminate\\Support\\ServiceProvider;\n");

        // Import services
        for (SqlTable table : schema.getEntityTables()) {
            String className = table.getEntityName();
            sb.append("use App\\Services\\").append(className).append("Service;\n");
        }

        sb.append("\nclass AppServiceProvider extends ServiceProvider\n");
        sb.append("{\n");

        // Register method
        sb.append("    /**\n");
        sb.append("     * Register any application services.\n");
        sb.append("     */\n");
        sb.append("    public function register(): void\n");
        sb.append("    {\n");

        // Register services as singletons
        for (SqlTable table : schema.getEntityTables()) {
            String className = table.getEntityName();
            sb.append("        $this->app->singleton(")
                    .append(className)
                    .append("Service::class);\n");
        }

        sb.append("    }\n\n");

        // Boot method
        sb.append("    /**\n");
        sb.append("     * Bootstrap any application services.\n");
        sb.append("     */\n");
        sb.append("    public function boot(): void\n");
        sb.append("    {\n");
        sb.append("        //\n");
        sb.append("    }\n");

        sb.append("}\n");

        return sb.toString();
    }

    private String toSnakeCase(String name) {
        if (name == null) return "";
        return name.replaceAll("([a-z])([A-Z])", "$1_$2")
                .replaceAll("[.-]", "_")
                .toLowerCase(Locale.ROOT);
    }

    @SuppressWarnings("LoopOverCharArray")
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
