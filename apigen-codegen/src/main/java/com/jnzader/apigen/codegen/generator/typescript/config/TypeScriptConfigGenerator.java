package com.jnzader.apigen.codegen.generator.typescript.config;

import com.jnzader.apigen.codegen.generator.api.Feature;
import com.jnzader.apigen.codegen.generator.api.ProjectConfig;
import com.jnzader.apigen.codegen.generator.typescript.TypeScriptTypeMapper;
import com.jnzader.apigen.codegen.model.SqlSchema;
import com.jnzader.apigen.codegen.model.SqlTable;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Generates configuration files for NestJS projects.
 *
 * <p>Generates:
 *
 * <ul>
 *   <li>package.json
 *   <li>tsconfig.json
 *   <li>nest-cli.json
 *   <li>.env.example
 *   <li>Dockerfile
 *   <li>docker-compose.yml
 *   <li>README.md
 *   <li>.gitignore
 * </ul>
 */
@SuppressWarnings({
    "java:S1192",
    "java:S3400"
}) // S1192: template strings; S3400: template methods return constants
public class TypeScriptConfigGenerator {

    private final String projectName;
    private final TypeScriptTypeMapper typeMapper;

    public TypeScriptConfigGenerator(String projectName) {
        this.projectName = projectName;
        this.typeMapper = new TypeScriptTypeMapper();
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

        files.put("package.json", generatePackageJson(hasJwtAuth, hasRateLimit));
        files.put("tsconfig.json", generateTsConfig());
        files.put("tsconfig.build.json", generateTsBuildConfig());
        files.put("nest-cli.json", generateNestCliJson());
        files.put(".env.example", generateEnvExample(hasJwtAuth, hasRateLimit));
        files.put(".env", generateEnvExample(hasJwtAuth, hasRateLimit));
        files.put(".gitignore", generateGitignore());
        files.put("README.md", generateReadme(schema, hasJwtAuth, hasRateLimit));
        files.put(".prettierrc", generatePrettierRc());
        files.put(".eslintrc.js", generateEslintRc());

        if (config.isFeatureEnabled(Feature.DOCKER)) {
            files.put("Dockerfile", generateDockerfile());
            files.put("docker-compose.yml", generateDockerCompose(hasRateLimit));
            files.put(".dockerignore", generateDockerignore());
        }

        return files;
    }

    private String generatePackageJson(boolean hasJwtAuth, boolean hasRateLimit) {
        String kebabName = typeMapper.toKebabCase(projectName);

        StringBuilder deps = new StringBuilder();
        deps.append("    \"@nestjs/common\": \"^11.1.0\",\n");
        deps.append("    \"@nestjs/config\": \"^4.0.0\",\n");
        deps.append("    \"@nestjs/core\": \"^11.1.0\",\n");
        deps.append("    \"@nestjs/platform-express\": \"^11.1.0\",\n");
        deps.append("    \"@nestjs/swagger\": \"^11.1.0\",\n");
        deps.append("    \"@nestjs/typeorm\": \"^11.0.0\",\n");

        // JWT Authentication dependencies
        if (hasJwtAuth) {
            deps.append("    \"@nestjs/jwt\": \"^11.0.0\",\n");
            deps.append("    \"@nestjs/passport\": \"^11.0.0\",\n");
            deps.append("    \"passport\": \"^0.7.0\",\n");
            deps.append("    \"passport-jwt\": \"^4.0.1\",\n");
            deps.append("    \"bcrypt\": \"^6.0.0\",\n");
        }

        // Rate Limiting dependencies
        if (hasRateLimit) {
            deps.append("    \"@nestjs/throttler\": \"^6.4.0\",\n");
        }

        deps.append("    \"class-transformer\": \"^0.5.1\",\n");
        deps.append("    \"class-validator\": \"^0.14.3\",\n");
        deps.append("    \"pg\": \"^8.17.1\",\n");
        deps.append("    \"reflect-metadata\": \"^0.2.2\",\n");
        deps.append("    \"rxjs\": \"^7.8.2\",\n");
        deps.append("    \"typeorm\": \"^0.3.28\"");

        StringBuilder devDeps = new StringBuilder();
        devDeps.append("    \"@nestjs/cli\": \"^11.0.0\",\n");
        devDeps.append("    \"@nestjs/schematics\": \"^11.0.0\",\n");
        devDeps.append("    \"@nestjs/testing\": \"^11.1.0\",\n");
        devDeps.append("    \"@types/express\": \"^4.17.21\",\n");
        devDeps.append("    \"@types/jest\": \"^29.5.14\",\n");
        devDeps.append("    \"@types/node\": \"^22.10.0\",\n");

        // JWT dev dependencies
        if (hasJwtAuth) {
            devDeps.append("    \"@types/passport-jwt\": \"^4.0.1\",\n");
            devDeps.append("    \"@types/bcrypt\": \"^5.0.2\",\n");
        }

        devDeps.append("    \"@typescript-eslint/eslint-plugin\": \"^8.20.0\",\n");
        devDeps.append("    \"@typescript-eslint/parser\": \"^8.20.0\",\n");
        devDeps.append("    \"eslint\": \"^9.18.0\",\n");
        devDeps.append("    \"eslint-config-prettier\": \"^10.0.0\",\n");
        devDeps.append("    \"eslint-plugin-prettier\": \"^5.2.3\",\n");
        devDeps.append("    \"jest\": \"^29.7.0\",\n");
        devDeps.append("    \"prettier\": \"^3.8.0\",\n");
        devDeps.append("    \"source-map-support\": \"^0.5.21\",\n");
        devDeps.append("    \"supertest\": \"^7.0.0\",\n");
        devDeps.append("    \"ts-jest\": \"^29.2.0\",\n");
        devDeps.append("    \"ts-loader\": \"^9.5.0\",\n");
        devDeps.append("    \"ts-node\": \"^10.9.2\",\n");
        devDeps.append("    \"tsconfig-paths\": \"^4.2.0\",\n");
        devDeps.append("    \"typescript\": \"^5.9.0\"");

        return """
        {
          "name": "%s",
          "version": "1.0.0",
          "description": "REST API generated by APiGen",
          "author": "",
          "private": true,
          "license": "MIT",
          "scripts": {
            "build": "nest build",
            "format": "prettier --write \\"src/**/*.ts\\" \\"test/**/*.ts\\"",
            "start": "nest start",
            "start:dev": "nest start --watch",
            "start:debug": "nest start --debug --watch",
            "start:prod": "node dist/main",
            "lint": "eslint \\"{src,apps,libs,test}/**/*.ts\\" --fix",
            "test": "jest",
            "test:watch": "jest --watch",
            "test:cov": "jest --coverage",
            "test:debug": "node --inspect-brk -r tsconfig-paths/register -r ts-node/register node_modules/.bin/jest --runInBand",
            "test:e2e": "jest --config ./test/jest-e2e.json",
            "typeorm": "ts-node -r tsconfig-paths/register ./node_modules/typeorm/cli.js",
            "migration:generate": "npm run typeorm -- migration:generate -d src/config/data-source.ts",
            "migration:run": "npm run typeorm -- migration:run -d src/config/data-source.ts",
            "migration:revert": "npm run typeorm -- migration:revert -d src/config/data-source.ts"
          },
          "dependencies": {
        %s
          },
          "devDependencies": {
        %s
          },
          "jest": {
            "moduleFileExtensions": ["js", "json", "ts"],
            "rootDir": "src",
            "testRegex": ".*\\\\.spec\\\\.ts$",
            "transform": {
              "^.+\\\\.(t|j)s$": "ts-jest"
            },
            "collectCoverageFrom": ["**/*.(t|j)s"],
            "coverageDirectory": "../coverage",
            "testEnvironment": "node"
          }
        }
        """
                .formatted(kebabName, deps, devDeps);
    }

    private String generateTsConfig() {
        return """
        {
          "compilerOptions": {
            "module": "commonjs",
            "declaration": true,
            "removeComments": true,
            "emitDecoratorMetadata": true,
            "experimentalDecorators": true,
            "allowSyntheticDefaultImports": true,
            "target": "ES2022",
            "sourceMap": true,
            "outDir": "./dist",
            "baseUrl": "./",
            "incremental": true,
            "skipLibCheck": true,
            "strictNullChecks": true,
            "noImplicitAny": true,
            "strictBindCallApply": true,
            "forceConsistentCasingInFileNames": true,
            "noFallthroughCasesInSwitch": true,
            "esModuleInterop": true,
            "resolveJsonModule": true
          }
        }
        """;
    }

    private String generateTsBuildConfig() {
        return """
        {
          "extends": "./tsconfig.json",
          "exclude": ["node_modules", "test", "dist", "**/*spec.ts"]
        }
        """;
    }

    private String generateNestCliJson() {
        return """
        {
          "$schema": "https://json.schemastore.org/nest-cli",
          "collection": "@nestjs/schematics",
          "sourceRoot": "src",
          "compilerOptions": {
            "deleteOutDir": true
          }
        }
        """;
    }

    private String generateEnvExample(boolean hasJwtAuth, boolean hasRateLimit) {
        String snakeName =
                projectName
                        .replaceAll("([a-z])([A-Z])", "$1_$2")
                        .replace("-", "_")
                        .toLowerCase(Locale.ROOT);

        StringBuilder sb = new StringBuilder();
        sb.append("# Application\n");
        sb.append("NODE_ENV=development\n");
        sb.append("PORT=3000\n");
        sb.append("\n");
        sb.append("# Database\n");
        sb.append("DB_HOST=localhost\n");
        sb.append("DB_PORT=5432\n");
        sb.append("DB_USERNAME=postgres\n");
        sb.append("DB_PASSWORD=postgres\n");
        sb.append("DB_DATABASE=").append(snakeName).append("\n");

        if (hasJwtAuth) {
            sb.append("\n");
            sb.append("# JWT Authentication\n");
            sb.append("JWT_SECRET=your-very-secure-secret-key-change-in-production-min-32-chars\n");
            sb.append("JWT_ACCESS_EXPIRATION=30m\n");
            sb.append(
                    "JWT_REFRESH_SECRET=your-refresh-secret-key-change-in-production-min-32-chars\n");
            sb.append("JWT_REFRESH_EXPIRATION=7d\n");
        }

        if (hasRateLimit) {
            sb.append("\n");
            sb.append("# Rate Limiting\n");
            sb.append("RATE_LIMIT_TTL=60000\n");
            sb.append("RATE_LIMIT_MAX=100\n");
            sb.append("\n");
            sb.append("# Redis (optional - for distributed rate limiting)\n");
            sb.append("# REDIS_HOST=localhost\n");
            sb.append("# REDIS_PORT=6379\n");
            sb.append("# REDIS_PASSWORD=\n");
        }

        return sb.toString();
    }

    private String generateGitignore() {
        return """
        # Dependencies
        node_modules/

        # Build output
        dist/
        build/

        # Environment files
        .env
        .env.local
        .env.*.local

        # IDE
        .idea/
        .vscode/
        *.swp
        *.swo

        # Logs
        logs/
        *.log
        npm-debug.log*

        # Coverage
        coverage/

        # OS
        .DS_Store
        Thumbs.db

        # Test
        .nyc_output/
        """;
    }

    private String generateReadme(SqlSchema schema, boolean hasJwtAuth, boolean hasRateLimit) {
        StringBuilder sb = new StringBuilder();
        String kebabName = typeMapper.toKebabCase(projectName);

        sb.append("# ").append(projectName).append("\n\n");
        sb.append("REST API generated by APiGen using NestJS and TypeORM.\n\n");

        sb.append("## Requirements\n\n");
        sb.append("- Node.js 20+\n");
        sb.append("- PostgreSQL 15+\n");
        sb.append("- npm or yarn\n\n");

        sb.append("## Getting Started\n\n");
        sb.append("### 1. Install dependencies\n\n");
        sb.append("```bash\n");
        sb.append("npm install\n");
        sb.append("```\n\n");

        sb.append("### 2. Configure environment\n\n");
        sb.append("```bash\n");
        sb.append("cp .env.example .env\n");
        sb.append("# Edit .env with your database credentials\n");
        sb.append("```\n\n");

        sb.append("### 3. Create database\n\n");
        sb.append("```bash\n");
        sb.append("createdb ").append(kebabName).append("\n");
        sb.append("```\n\n");

        sb.append("### 4. Run the application\n\n");
        sb.append("```bash\n");
        sb.append("# Development\n");
        sb.append("npm run start:dev\n\n");
        sb.append("# Production\n");
        sb.append("npm run build\n");
        sb.append("npm run start:prod\n");
        sb.append("```\n\n");

        sb.append("## API Documentation\n\n");
        sb.append("Once the application is running, visit:\n");
        sb.append("- Swagger UI: http://localhost:3000/api/docs\n\n");

        // Authentication documentation
        if (hasJwtAuth) {
            sb.append("## Authentication\n\n");
            sb.append("This API uses JWT (JSON Web Tokens) for authentication.\n\n");
            sb.append("### Endpoints\n\n");
            sb.append("| Method | Endpoint | Description |\n");
            sb.append("|--------|----------|-------------|\n");
            sb.append("| POST | `/api/auth/register` | Register a new user |\n");
            sb.append("| POST | `/api/auth/login` | Login and get tokens |\n");
            sb.append("| POST | `/api/auth/refresh` | Refresh access token |\n");
            sb.append("| POST | `/api/auth/logout` | Logout (invalidate tokens) |\n");
            sb.append("| GET | `/api/auth/profile` | Get current user profile |\n");
            sb.append("| PUT | `/api/auth/password` | Change password |\n\n");
            sb.append("### Usage\n\n");
            sb.append("1. Register or login to get access and refresh tokens\n");
            sb.append("2. Include the access token in the Authorization header:\n");
            sb.append("   ```\n");
            sb.append("   Authorization: Bearer <access_token>\n");
            sb.append("   ```\n");
            sb.append("3. Use the refresh token to get new access tokens when expired\n\n");
        }

        // Rate limiting documentation
        if (hasRateLimit) {
            sb.append("## Rate Limiting\n\n");
            sb.append("This API implements rate limiting to prevent abuse.\n\n");
            sb.append("### Default Limits\n\n");
            sb.append("| Tier | Limit | Window |\n");
            sb.append("|------|-------|--------|\n");
            sb.append("| Standard | 100 requests | 1 minute |\n");
            sb.append("| Auth | 5 requests | 1 minute |\n");
            sb.append("| Heavy | 10 requests | 1 minute |\n\n");
            sb.append("### Headers\n\n");
            sb.append("Rate limit information is included in response headers:\n");
            sb.append("- `X-RateLimit-Limit`: Maximum requests allowed\n");
            sb.append("- `X-RateLimit-Remaining`: Requests remaining\n");
            sb.append("- `Retry-After`: Seconds until limit resets (when exceeded)\n\n");
        }

        sb.append("## Available Endpoints\n\n");
        for (SqlTable table : schema.getEntityTables()) {
            String entityName = table.getEntityName();
            String pluralKebab = typeMapper.toKebabCase(typeMapper.pluralize(entityName));
            sb.append("### ").append(entityName).append("\n\n");
            sb.append("| Method | Endpoint | Description |\n");
            sb.append("|--------|----------|-------------|\n");
            sb.append("| GET | `/api/v1/")
                    .append(pluralKebab)
                    .append("` | List all with pagination |\n");
            sb.append("| GET | `/api/v1/").append(pluralKebab).append("/:id` | Get by ID |\n");
            sb.append("| POST | `/api/v1/").append(pluralKebab).append("` | Create new |\n");
            sb.append("| PUT | `/api/v1/").append(pluralKebab).append("/:id` | Update |\n");
            sb.append("| DELETE | `/api/v1/").append(pluralKebab).append("/:id` | Soft delete |\n");
            sb.append("| POST | `/api/v1/")
                    .append(pluralKebab)
                    .append("/:id/restore` | Restore deleted |\n\n");
        }

        sb.append("## Docker\n\n");
        sb.append("```bash\n");
        sb.append("# Build and run with Docker Compose\n");
        sb.append("docker-compose up -d\n");
        sb.append("```\n\n");

        sb.append("## Testing\n\n");
        sb.append("```bash\n");
        sb.append("# Unit tests\n");
        sb.append("npm run test\n\n");
        sb.append("# E2E tests\n");
        sb.append("npm run test:e2e\n\n");
        sb.append("# Test coverage\n");
        sb.append("npm run test:cov\n");
        sb.append("```\n\n");

        sb.append("## License\n\n");
        sb.append("MIT\n");

        return sb.toString();
    }

    private String generatePrettierRc() {
        return """
        {
          "singleQuote": true,
          "trailingComma": "all",
          "tabWidth": 2,
          "semi": true,
          "printWidth": 100
        }
        """;
    }

    private String generateEslintRc() {
        return """
        module.exports = {
          parser: '@typescript-eslint/parser',
          parserOptions: {
            project: 'tsconfig.json',
            tsconfigRootDir: __dirname,
            sourceType: 'module',
          },
          plugins: ['@typescript-eslint/eslint-plugin'],
          extends: [
            'plugin:@typescript-eslint/recommended',
            'plugin:prettier/recommended',
          ],
          root: true,
          env: {
            node: true,
            jest: true,
          },
          ignorePatterns: ['.eslintrc.js'],
          rules: {
            '@typescript-eslint/interface-name-prefix': 'off',
            '@typescript-eslint/explicit-function-return-type': 'off',
            '@typescript-eslint/explicit-module-boundary-types': 'off',
            '@typescript-eslint/no-explicit-any': 'off',
          },
        };
        """;
    }

    private String generateDockerfile() {
        return """
        # Build stage
        FROM node:20-alpine AS builder

        WORKDIR /app

        COPY package*.json ./
        RUN npm ci

        COPY . .
        RUN npm run build

        # Production stage
        FROM node:20-alpine

        WORKDIR /app

        COPY package*.json ./
        RUN npm ci --only=production

        COPY --from=builder /app/dist ./dist

        ENV NODE_ENV=production
        ENV PORT=3000

        EXPOSE 3000

        CMD ["node", "dist/main"]
        """;
    }

    private String generateDockerCompose(boolean hasRateLimit) {
        String kebabName = typeMapper.toKebabCase(projectName);
        String snakeName =
                projectName
                        .replaceAll("([a-z])([A-Z])", "$1_$2")
                        .replace("-", "_")
                        .toLowerCase(Locale.ROOT);

        StringBuilder sb = new StringBuilder();
        sb.append("version: '3.8'\n\n");
        sb.append("services:\n");
        sb.append("  app:\n");
        sb.append("    build: .\n");
        sb.append("    container_name: ").append(kebabName).append("-api\n");
        sb.append("    ports:\n");
        sb.append("      - \"3000:3000\"\n");
        sb.append("    environment:\n");
        sb.append("      - NODE_ENV=production\n");
        sb.append("      - DB_HOST=db\n");
        sb.append("      - DB_PORT=5432\n");
        sb.append("      - DB_USERNAME=postgres\n");
        sb.append("      - DB_PASSWORD=postgres\n");
        sb.append("      - DB_DATABASE=").append(snakeName).append("\n");

        if (hasRateLimit) {
            sb.append("      - REDIS_HOST=redis\n");
            sb.append("      - REDIS_PORT=6379\n");
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
        sb.append("    image: postgres:16-alpine\n");
        sb.append("    container_name: ").append(kebabName).append("-db\n");
        sb.append("    environment:\n");
        sb.append("      - POSTGRES_USER=postgres\n");
        sb.append("      - POSTGRES_PASSWORD=postgres\n");
        sb.append("      - POSTGRES_DB=").append(snakeName).append("\n");
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
            sb.append("\n");
            sb.append("  redis:\n");
            sb.append("    image: redis:7-alpine\n");
            sb.append("    container_name: ").append(kebabName).append("-redis\n");
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
        node_modules
        dist
        .git
        .gitignore
        .env
        .env.local
        *.md
        .idea
        .vscode
        coverage
        test
        """;
    }
}
