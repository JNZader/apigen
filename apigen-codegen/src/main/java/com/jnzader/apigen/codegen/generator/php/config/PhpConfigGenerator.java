package com.jnzader.apigen.codegen.generator.php.config;

import com.jnzader.apigen.codegen.generator.api.ProjectConfig;
import com.jnzader.apigen.codegen.generator.php.PhpTypeMapper;
import com.jnzader.apigen.codegen.model.SqlSchema;
import com.jnzader.apigen.codegen.model.SqlTable;
import java.util.LinkedHashMap;
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

    private static final String PHP_VERSION = "8.3";
    private static final String LARAVEL_VERSION = "^11.0";

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
        Map<String, String> files = new LinkedHashMap<>();

        files.put("composer.json", generateComposerJson());
        files.put("routes/api.php", generateApiRoutes(schema));
        files.put(".env.example", generateEnvExample());
        files.put(".gitignore", generateGitignore());
        files.put("README.md", generateReadme());
        files.put("Dockerfile", generateDockerfile());
        files.put("docker-compose.yml", generateDockerCompose());
        files.put("config/api.php", generateApiConfig());
        files.put("app/Providers/AppServiceProvider.php", generateAppServiceProvider(schema));

        return files;
    }

    private String generateComposerJson() {
        return """
        {
            "name": "apigen/%s",
            "type": "project",
            "description": "Generated Laravel API project",
            "keywords": ["laravel", "api", "rest"],
            "license": "MIT",
            "require": {
                "php": "^%s",
                "laravel/framework": "%s",
                "darkaonline/l5-swagger": "^8.6",
                "spatie/laravel-query-builder": "^6.0"
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
                .formatted(projectName, PHP_VERSION, LARAVEL_VERSION);
    }

    private String generateApiRoutes(SqlSchema schema) {
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

        // API v1 routes
        sb.append("Route::prefix('v1')->group(function () {\n");

        for (SqlTable table : schema.getEntityTables()) {
            String className = table.getEntityName();
            String pluralName =
                    typeMapper.pluralize(typeMapper.toCamelCase(className)).toLowerCase();

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

    private String generateEnvExample() {
        return """
        APP_NAME=%s
        APP_ENV=local
        APP_KEY=
        APP_DEBUG=true
        APP_TIMEZONE=UTC
        APP_URL=http://localhost:8000

        LOG_CHANNEL=stack
        LOG_DEPRECATIONS_CHANNEL=null
        LOG_LEVEL=debug

        DB_CONNECTION=pgsql
        DB_HOST=127.0.0.1
        DB_PORT=5432
        DB_DATABASE=%s
        DB_USERNAME=postgres
        DB_PASSWORD=postgres

        BROADCAST_DRIVER=log
        CACHE_DRIVER=file
        FILESYSTEM_DISK=local
        QUEUE_CONNECTION=sync
        SESSION_DRIVER=file
        SESSION_LIFETIME=120

        L5_SWAGGER_GENERATE_ALWAYS=true
        L5_SWAGGER_CONST_HOST=http://localhost:8000/api

        # Pagination
        API_DEFAULT_PAGE_SIZE=10
        API_MAX_PAGE_SIZE=100
        """
                .formatted(toPascalCase(projectName), projectName);
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

    private String generateReadme() {
        return """
        # %s

        Generated Laravel API project.

        ## Requirements

        - PHP %s+
        - Composer 2.x
        - PostgreSQL 15+

        ## Setup

        ```bash
        # Install dependencies
        composer install

        # Copy environment file
        cp .env.example .env

        # Generate application key
        php artisan key:generate

        # Run migrations
        php artisan migrate

        # Start development server
        php artisan serve
        ```

        ## API Documentation

        - Swagger UI: http://localhost:8000/api/documentation

        Generate Swagger docs:
        ```bash
        php artisan l5-swagger:generate
        ```

        ## Testing

        ```bash
        # Run tests
        composer test

        # Run tests with coverage
        ./vendor/bin/pest --coverage
        ```

        ## Code Style

        ```bash
        # Fix code style
        composer lint
        ```

        ## Docker

        ```bash
        docker-compose up -d
        ```
        """
                .formatted(toPascalCase(projectName), PHP_VERSION);
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

    private String generateDockerCompose() {
        return """
        services:
          app:
            build:
              context: .
              dockerfile: Dockerfile
            container_name: %s_app
            restart: unless-stopped
            working_dir: /var/www
            volumes:
              - .:/var/www
            networks:
              - app-network
            depends_on:
              - db

          nginx:
            image: nginx:alpine
            container_name: %s_nginx
            restart: unless-stopped
            ports:
              - "8000:80"
            volumes:
              - .:/var/www
              - ./docker/nginx:/etc/nginx/conf.d
            networks:
              - app-network

          db:
            image: postgres:17-alpine
            container_name: %s_db
            restart: unless-stopped
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
            networks:
              - app-network

        networks:
          app-network:
            driver: bridge

        volumes:
          postgres_data:
        """
                .formatted(projectName, projectName, projectName, projectName);
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
