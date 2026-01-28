package com.jnzader.apigen.server.service.generator;

import static com.jnzader.apigen.server.service.generator.util.StringTransformationUtil.toCamelCase;
import static com.jnzader.apigen.server.service.generator.util.StringTransformationUtil.toKebabCase;
import static com.jnzader.apigen.server.service.generator.util.StringTransformationUtil.toPascalCase;

import com.jnzader.apigen.codegen.model.SqlColumn;
import com.jnzader.apigen.codegen.model.SqlSchema;
import com.jnzader.apigen.codegen.model.SqlTable;
import com.jnzader.apigen.server.dto.GenerateRequest;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

/** Generates project structure files (Main class, README). */
@Component
@SuppressWarnings("java:S1192") // String repetitions intentional for clarity
public class ProjectStructureGenerator {

    private static final String GITIGNORE_CONTENT =
"""
# Gradle
.gradle/
build/
!gradle/wrapper/gradle-wrapper.jar

# IDE
.idea/
*.iml
*.ipr
*.iws
.project
.classpath
.settings/
.vscode/

# Logs
*.log
logs/

# OS
.DS_Store
Thumbs.db

# Env
.env
*.local
""";

    /**
     * Generates the main Application class.
     *
     * @param config the project configuration
     * @return the main class content
     */
    public String generateMainClass(GenerateRequest.ProjectConfig config) {
        String className = toPascalCase(config.getArtifactId()) + "Application";
        String basePackage = config.getBasePackage();
        boolean securityEnabled = config.getModules() != null && config.getModules().isSecurity();

        String jpaRepoPackages;
        String entityScanPackages;

        if (securityEnabled) {
            jpaRepoPackages =
                    String.format(
                            "{\"%s\", \"com.jnzader.apigen.security.domain.repository\"}",
                            basePackage);
            entityScanPackages =
                    String.format(
                            "{\"%s\", \"com.jnzader.apigen.security.domain.entity\"}", basePackage);
        } else {
            jpaRepoPackages = String.format("\"%s\"", basePackage);
            entityScanPackages = String.format("\"%s\"", basePackage);
        }

        return
"""
package %s;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan(basePackages = {"%s", "com.jnzader.apigen.core"})
@EnableJpaRepositories(basePackages = %s)
@EntityScan(basePackages = %s)
@EnableCaching
public class %s {

    public static void main(String[] args) {
        SpringApplication.run(%s.class, args);
    }
}
"""
                .formatted(
                        basePackage,
                        basePackage,
                        jpaRepoPackages,
                        entityScanPackages,
                        className,
                        className);
    }

    /**
     * Gets the main class name for a project.
     *
     * @param config the project configuration
     * @return the main class name
     */
    public String getMainClassName(GenerateRequest.ProjectConfig config) {
        return toPascalCase(config.getArtifactId()) + "Application";
    }

    /**
     * Generates the README.md file content (legacy method for backward compatibility).
     *
     * @param config the project configuration
     * @return the README content
     */
    public String generateReadme(GenerateRequest.ProjectConfig config) {
        return generateReadme(config, null);
    }

    /**
     * Generates a comprehensive README.md file content with entity and API documentation.
     *
     * @param config the project configuration
     * @param schema the parsed SQL schema (nullable for backward compatibility)
     * @return the README content
     */
    public String generateReadme(GenerateRequest.ProjectConfig config, SqlSchema schema) {
        StringBuilder readme = new StringBuilder();
        GenerateRequest.FeaturesConfig features = getFeatures(config);
        GenerateRequest.DatabaseConfig db = config.getDatabase();
        boolean securityEnabled = config.getModules() != null && config.getModules().isSecurity();
        boolean miseEnabled = features.isMiseTasks();

        // Header
        appendHeader(readme, config);

        // Tech Stack
        appendTechStack(readme, config, db, securityEnabled);

        // Features
        appendFeatures(readme, features, securityEnabled);

        // Mise Tasks (if enabled)
        if (miseEnabled) {
            appendMiseTasks(readme);
        }

        // Quick Start
        appendQuickStart(readme, config, db, features, miseEnabled);

        // Entities and Endpoints
        if (schema != null && !schema.getEntityTables().isEmpty()) {
            appendEntitiesAndEndpoints(readme, schema);
        }

        // API Endpoints Reference
        appendApiEndpointsReference(readme);

        // Filtering and Pagination
        appendFilteringAndPagination(readme);

        // Authentication (if security enabled)
        if (securityEnabled) {
            appendAuthentication(readme);
        }

        // Core Features Documentation
        appendCoreFeatures(readme, features);

        // API Documentation URLs
        appendApiDocumentation(readme, features);

        // Configuration
        appendConfiguration(readme, config, db, securityEnabled);

        // Project Structure
        appendProjectStructure(readme, config);

        // Testing
        appendTesting(readme, miseEnabled);

        // Troubleshooting
        appendTroubleshooting(readme);

        // License
        appendLicense(readme);

        return readme.toString();
    }

    private GenerateRequest.FeaturesConfig getFeatures(GenerateRequest.ProjectConfig config) {
        return config.getFeatures() != null
                ? config.getFeatures()
                : GenerateRequest.FeaturesConfig.builder().build();
    }

    private void appendHeader(StringBuilder readme, GenerateRequest.ProjectConfig config) {
        readme.append("# ").append(config.getName()).append("\n\n");
        readme.append("REST API generated with [APiGen Studio](https://apigen-studio.vercel.app)");
        readme.append(" | [GitHub](https://github.com/jnzader/apigen)\n\n");
    }

    private void appendTechStack(
            StringBuilder readme,
            GenerateRequest.ProjectConfig config,
            GenerateRequest.DatabaseConfig db,
            boolean securityEnabled) {
        readme.append("## Tech Stack\n\n");
        readme.append("| Technology | Version | Description |\n");
        readme.append("|------------|---------|-------------|\n");
        readme.append("| Java | ").append(config.getJavaVersion()).append(" | Runtime |\n");
        readme.append("| Spring Boot | ")
                .append(config.getSpringBootVersion())
                .append(" | Framework |\n");
        readme.append("| APiGen Core | Latest | CRUD & REST infrastructure |\n");
        if (securityEnabled) {
            readme.append("| APiGen Security | Latest | JWT auth & rate limiting |\n");
        }
        if (db != null) {
            readme.append("| ").append(capitalize(db.getType())).append(" | Latest | Database |\n");
        }
        readme.append("| Gradle | 9.x | Build tool |\n");
        readme.append("\n");
    }

    private void appendFeatures(
            StringBuilder readme,
            GenerateRequest.FeaturesConfig features,
            boolean securityEnabled) {
        readme.append("## Features\n\n");
        readme.append("### Core Features\n\n");
        readme.append(
                "- **CRUD operations** - Complete Create, Read, Update, Delete for all entities\n");
        readme.append(
                "- **Pagination and filtering** - Cursor and offset-based pagination with 12+"
                        + " filter operators\n");
        readme.append(
                "- **Input validation** - JSR-380 Bean Validation with detailed error messages\n");
        readme.append("- **Exception handling** - RFC 7807 Problem Details for HTTP APIs\n");

        if (features.isHateoas()) {
            readme.append("- **HATEOAS links** - Hypermedia navigation in all responses\n");
        }
        if (features.isAuditing()) {
            readme.append(
                    "- **Audit trails** - Automatic `createdAt`, `updatedAt`, `createdBy`,"
                            + " `updatedBy`\n");
        }
        if (features.isSoftDelete()) {
            readme.append("- **Soft delete** - Logical deletion with restore capability\n");
        }
        if (features.isCaching()) {
            readme.append("- **Caching** - Caffeine cache with ETag support for HTTP 304\n");
        }
        if (features.isSwagger()) {
            readme.append("- **OpenAPI documentation** - Interactive Swagger UI\n");
        }
        if (features.isDocker()) {
            readme.append("- **Docker support** - Production-ready containerization\n");
        }

        if (securityEnabled) {
            readme.append("\n### Security Features\n\n");
            readme.append("- **JWT authentication** - Access and refresh tokens\n");
            readme.append("- **Role-based access control** - User, Admin, and custom roles\n");
            readme.append("- **Rate limiting** - Per-IP and per-user request throttling\n");
            readme.append("- **Account lockout** - Brute force protection\n");
            readme.append("- **Strong password policy** - Configurable validation rules\n");
        }

        readme.append("\n");
    }

    private void appendMiseTasks(StringBuilder readme) {
        readme.append("## Mise Tasks\n\n");
        readme.append(
                "[Mise](https://mise.jdx.dev) is used for task automation. Install with `curl"
                        + " https://mise.run | sh`\n\n");
        readme.append("### Available Tasks\n\n");
        readme.append("| Task | Description |\n");
        readme.append("|------|-------------|\n");
        readme.append("| `mise run dev` | Start app + database in development mode |\n");
        readme.append("| `mise run dev:app` | Start only the application |\n");
        readme.append("| `mise run test` | Run all tests |\n");
        readme.append("| `mise run test:unit` | Run unit tests only |\n");
        readme.append("| `mise run test:integration` | Run integration tests |\n");
        readme.append("| `mise run test:coverage` | Run tests with coverage report |\n");
        readme.append("| `mise run build` | Build production JAR |\n");
        readme.append("| `mise run docker:up` | Start Docker services |\n");
        readme.append("| `mise run docker:down` | Stop Docker services |\n");
        readme.append("| `mise run docker:build` | Build Docker image |\n");
        readme.append("| `mise run db:migrate` | Run database migrations |\n");
        readme.append("| `mise run db:reset` | Reset database (WARNING: deletes data) |\n");
        readme.append("| `mise run lint` | Run code linter |\n");
        readme.append("| `mise run format` | Format code |\n");
        readme.append("| `mise run clean` | Clean build artifacts |\n");
        readme.append("\n");
        readme.append("### Common Workflows\n\n");
        readme.append("```bash\n");
        readme.append("# First time setup\n");
        readme.append("mise install           # Install Java and tools\n");
        readme.append("mise run docker:up     # Start database\n");
        readme.append("mise run dev           # Start application\n\n");
        readme.append("# Daily development\n");
        readme.append("mise run dev           # Start everything\n");
        readme.append("mise run test          # Run tests before commit\n\n");
        readme.append("# Before PR\n");
        readme.append("mise run format        # Format code\n");
        readme.append("mise run lint          # Check for issues\n");
        readme.append("mise run test:coverage # Ensure coverage\n");
        readme.append("```\n\n");
    }

    private void appendQuickStart(
            StringBuilder readme,
            GenerateRequest.ProjectConfig config,
            GenerateRequest.DatabaseConfig db,
            GenerateRequest.FeaturesConfig features,
            boolean miseEnabled) {
        readme.append("## Quick Start\n\n");
        readme.append("### Prerequisites\n\n");
        readme.append("- Java ").append(config.getJavaVersion()).append("+");
        if (miseEnabled) {
            readme.append(" ([Mise](https://mise.jdx.dev) recommended for auto-install)");
        }
        readme.append("\n");
        if (db != null && !"h2".equalsIgnoreCase(db.getType())) {
            readme.append("- ").append(capitalize(db.getType())).append(" database\n");
        }
        if (features.isDocker()) {
            readme.append("- Docker & Docker Compose\n");
        }
        readme.append("\n");

        if (miseEnabled) {
            readme.append("### With Mise (Recommended)\n\n");
            readme.append("```bash\n");
            readme.append("# Clone and enter project\n");
            readme.append("git clone <repository-url>\n");
            readme.append("cd ").append(config.getArtifactId()).append("\n\n");
            readme.append("# Install tools and start\n");
            readme.append("mise install\n");
            readme.append("mise run dev\n");
            readme.append("```\n\n");
            readme.append("### Without Mise\n\n");
        } else {
            readme.append("### Run Locally\n\n");
        }

        readme.append("```bash\n");
        readme.append("# Clone the repository\n");
        readme.append("git clone <repository-url>\n");
        readme.append("cd ").append(config.getArtifactId()).append("\n\n");
        if (features.isDocker() && db != null && !"h2".equalsIgnoreCase(db.getType())) {
            readme.append("# Start database\n");
            readme.append("docker-compose up -d db\n\n");
        }
        readme.append("# Build the project\n");
        readme.append("./gradlew build\n\n");
        readme.append("# Run the application\n");
        readme.append("./gradlew bootRun\n");
        readme.append("```\n\n");

        if (features.isDocker()) {
            readme.append("### Run with Docker\n\n");
            readme.append("```bash\n");
            readme.append("# Build and start all services\n");
            readme.append("docker-compose up -d\n\n");
            readme.append("# View logs\n");
            readme.append("docker-compose logs -f app\n\n");
            readme.append("# Stop services\n");
            readme.append("docker-compose down\n\n");
            readme.append("# Stop and remove volumes (reset database)\n");
            readme.append("docker-compose down -v\n");
            readme.append("```\n\n");
        }

        readme.append("The API will be available at `http://localhost:8080`\n\n");
    }

    private void appendEntitiesAndEndpoints(StringBuilder readme, SqlSchema schema) {
        readme.append("## Entities\n\n");
        List<SqlTable> entities = schema.getEntityTables();

        // Summary table
        readme.append("| Entity | Fields | Endpoints |\n");
        readme.append("|--------|--------|----------|\n");
        for (SqlTable table : entities) {
            String entityName = toPascalCase(table.getName());
            String endpoint = "/api/" + toKebabCase(table.getName());
            int fieldCount = table.getColumns().size();
            readme.append("| ")
                    .append(entityName)
                    .append(" | ")
                    .append(fieldCount)
                    .append(" | `")
                    .append(endpoint)
                    .append("` |\n");
        }
        readme.append("\n");

        // Detailed documentation for each entity
        for (SqlTable table : entities) {
            String entityName = toPascalCase(table.getName());
            String endpoint = "/api/" + toKebabCase(table.getName());

            readme.append("### ").append(entityName).append("\n\n");

            // Fields table
            readme.append("#### Fields\n\n");
            readme.append("| Field | Type | Constraints |\n");
            readme.append("|-------|------|-------------|\n");
            for (SqlColumn col : table.getColumns()) {
                String constraints = buildConstraints(col);
                readme.append("| `")
                        .append(toCamelCase(col.getName()))
                        .append("` | ")
                        .append(col.getJavaType())
                        .append(" | ")
                        .append(constraints)
                        .append(" |\n");
            }
            readme.append("\n");

            // Endpoints table
            readme.append("#### Endpoints\n\n");
            readme.append("| Method | Path | Description |\n");
            readme.append("|--------|------|-------------|\n");
            readme.append("| GET | `")
                    .append(endpoint)
                    .append("` | List all (paginated, filterable) |\n");
            readme.append("| GET | `").append(endpoint).append("/{id}` | Get by ID |\n");
            readme.append("| HEAD | `").append(endpoint).append("/{id}` | Check existence |\n");
            readme.append("| POST | `").append(endpoint).append("` | Create new |\n");
            readme.append("| PUT | `").append(endpoint).append("/{id}` | Full update |\n");
            readme.append("| PATCH | `").append(endpoint).append("/{id}` | Partial update |\n");
            readme.append("| DELETE | `").append(endpoint).append("/{id}` | Soft delete |\n");
            readme.append("| POST | `")
                    .append(endpoint)
                    .append("/{id}/restore` | Restore deleted |\n");
            readme.append("| DELETE | `")
                    .append(endpoint)
                    .append("/{id}/hard` | Permanent delete |\n");
            readme.append("\n");
        }
    }

    private void appendApiEndpointsReference(StringBuilder readme) {
        readme.append("## API Endpoints Reference\n\n");
        readme.append(
                "All entities follow the same endpoint pattern. Example using `User` entity:\n\n");

        readme.append("### List with Pagination and Filters\n\n");
        readme.append("```http\n");
        readme.append(
                "GET /api/users?page=0&size=20&sort=createdAt,desc&filter=status:eq:ACTIVE\n");
        readme.append("```\n\n");

        readme.append("**Query Parameters:**\n\n");
        readme.append("| Parameter | Type | Description | Example |\n");
        readme.append("|-----------|------|-------------|--------|\n");
        readme.append("| `page` | int | Page number (0-indexed) | `page=0` |\n");
        readme.append("| `size` | int | Items per page (default: 20, max: 100) | `size=50` |\n");
        readme.append("| `sort` | string | Sort field and direction | `sort=name,asc` |\n");
        readme.append("| `filter` | string | Filter expression | `filter=status:eq:ACTIVE` |\n");
        readme.append("\n");

        readme.append("**Response (with HATEOAS):**\n\n");
        readme.append("```json\n");
        readme.append("{\n");
        readme.append("  \"content\": [\n");
        readme.append("    {\n");
        readme.append("      \"id\": 1,\n");
        readme.append("      \"name\": \"John Doe\",\n");
        readme.append("      \"email\": \"john@example.com\",\n");
        readme.append("      \"_links\": {\n");
        readme.append("        \"self\": { \"href\": \"/api/users/1\" }\n");
        readme.append("      }\n");
        readme.append("    }\n");
        readme.append("  ],\n");
        readme.append("  \"_links\": {\n");
        readme.append("    \"self\": { \"href\": \"/api/users?page=0&size=20\" },\n");
        readme.append("    \"next\": { \"href\": \"/api/users?page=1&size=20\" }\n");
        readme.append("  },\n");
        readme.append("  \"page\": {\n");
        readme.append("    \"size\": 20,\n");
        readme.append("    \"totalElements\": 150,\n");
        readme.append("    \"totalPages\": 8,\n");
        readme.append("    \"number\": 0\n");
        readme.append("  }\n");
        readme.append("}\n");
        readme.append("```\n\n");

        readme.append("### Create\n\n");
        readme.append("```http\n");
        readme.append("POST /api/users\n");
        readme.append("Content-Type: application/json\n\n");
        readme.append("{\n");
        readme.append("  \"name\": \"John Doe\",\n");
        readme.append("  \"email\": \"john@example.com\"\n");
        readme.append("}\n");
        readme.append("```\n\n");

        readme.append("### Get by ID\n\n");
        readme.append("```http\n");
        readme.append("GET /api/users/1\n");
        readme.append("```\n\n");

        readme.append("### Check Existence (HEAD)\n\n");
        readme.append("```http\n");
        readme.append("HEAD /api/users/1\n");
        readme.append("# Returns 200 if exists, 404 if not (no body)\n");
        readme.append("```\n\n");

        readme.append("### Update (Full - PUT)\n\n");
        readme.append("```http\n");
        readme.append("PUT /api/users/1\n");
        readme.append("Content-Type: application/json\n\n");
        readme.append("{\n");
        readme.append("  \"name\": \"John Updated\",\n");
        readme.append("  \"email\": \"john.updated@example.com\"\n");
        readme.append("}\n");
        readme.append("```\n\n");

        readme.append("### Update (Partial - PATCH)\n\n");
        readme.append("```http\n");
        readme.append("PATCH /api/users/1\n");
        readme.append("Content-Type: application/json\n\n");
        readme.append("{\n");
        readme.append("  \"email\": \"newemail@example.com\"\n");
        readme.append("}\n");
        readme.append("```\n\n");

        readme.append("### Delete (Soft)\n\n");
        readme.append("```http\n");
        readme.append("DELETE /api/users/1\n");
        readme.append("# Sets status=DELETED, data preserved\n");
        readme.append("```\n\n");

        readme.append("### Restore\n\n");
        readme.append("```http\n");
        readme.append("POST /api/users/1/restore\n");
        readme.append("```\n\n");

        readme.append("### Delete (Hard/Permanent)\n\n");
        readme.append("```http\n");
        readme.append("DELETE /api/users/1/hard\n");
        readme.append("# Permanent deletion - cannot be undone\n");
        readme.append("```\n\n");
    }

    private void appendFilteringAndPagination(StringBuilder readme) {
        readme.append("## Filtering and Pagination\n\n");

        readme.append("### Filter Operators\n\n");
        readme.append("| Operator | Description | Example |\n");
        readme.append("|----------|-------------|---------|\n");
        readme.append("| `eq` | Equals | `filter=status:eq:ACTIVE` |\n");
        readme.append("| `ne` | Not equals | `filter=status:ne:DELETED` |\n");
        readme.append("| `gt` | Greater than | `filter=age:gt:18` |\n");
        readme.append("| `gte` | Greater than or equal | `filter=price:gte:100` |\n");
        readme.append("| `lt` | Less than | `filter=stock:lt:10` |\n");
        readme.append("| `lte` | Less than or equal | `filter=rating:lte:5` |\n");
        readme.append("| `like` | Contains (case-insensitive) | `filter=name:like:john` |\n");
        readme.append("| `startsWith` | Starts with | `filter=name:startsWith:A` |\n");
        readme.append("| `endsWith` | Ends with | `filter=email:endsWith:.com` |\n");
        readme.append("| `in` | In list | `filter=status:in:ACTIVE,PENDING` |\n");
        readme.append("| `between` | Between two values | `filter=price:between:10,100` |\n");
        readme.append("| `isNull` | Is null | `filter=deletedAt:isNull:true` |\n");
        readme.append("\n");

        readme.append("### Multiple Filters\n\n");
        readme.append("Combine multiple filters with `&`:\n\n");
        readme.append("```http\n");
        readme.append(
                "GET /api/products?filter=category:eq:electronics&filter=price:between:100,500&filter=inStock:eq:true\n");
        readme.append("```\n\n");

        readme.append("### Sorting\n\n");
        readme.append("```http\n");
        readme.append("# Single field\n");
        readme.append("GET /api/users?sort=name,asc\n\n");
        readme.append("# Multiple fields\n");
        readme.append("GET /api/users?sort=lastName,asc&sort=firstName,asc\n");
        readme.append("```\n\n");

        readme.append("### Pagination Modes\n\n");
        readme.append("**Offset-based (default):**\n");
        readme.append("```http\n");
        readme.append("GET /api/users?page=0&size=20\n");
        readme.append("```\n\n");
        readme.append("**Cursor-based (for large datasets):**\n");
        readme.append("```http\n");
        readme.append("GET /api/users?cursor=eyJpZCI6MTAwfQ&size=20\n");
        readme.append("```\n\n");
    }

    private void appendAuthentication(StringBuilder readme) {
        readme.append("## Authentication\n\n");
        readme.append("This API uses JWT (JSON Web Tokens) for authentication.\n\n");

        readme.append("### Register\n\n");
        readme.append("```http\n");
        readme.append("POST /api/auth/register\n");
        readme.append("Content-Type: application/json\n\n");
        readme.append("{\n");
        readme.append("  \"username\": \"newuser\",\n");
        readme.append("  \"email\": \"user@example.com\",\n");
        readme.append("  \"password\": \"SecurePass123!\"\n");
        readme.append("}\n");
        readme.append("```\n\n");

        readme.append("### Login\n\n");
        readme.append("```http\n");
        readme.append("POST /api/auth/login\n");
        readme.append("Content-Type: application/json\n\n");
        readme.append("{\n");
        readme.append("  \"username\": \"newuser\",\n");
        readme.append("  \"password\": \"SecurePass123!\"\n");
        readme.append("}\n\n");
        readme.append("Response:\n");
        readme.append("{\n");
        readme.append("  \"accessToken\": \"eyJhbGciOiJIUzI1NiIs...\",\n");
        readme.append("  \"refreshToken\": \"eyJhbGciOiJIUzI1NiIs...\",\n");
        readme.append("  \"tokenType\": \"Bearer\",\n");
        readme.append("  \"expiresIn\": 1800\n");
        readme.append("}\n");
        readme.append("```\n\n");

        readme.append("### Using the Token\n\n");
        readme.append("Include the access token in the `Authorization` header:\n\n");
        readme.append("```http\n");
        readme.append("GET /api/users\n");
        readme.append("Authorization: Bearer eyJhbGciOiJIUzI1NiIs...\n");
        readme.append("```\n\n");

        readme.append("### Refresh Token\n\n");
        readme.append("```http\n");
        readme.append("POST /api/auth/refresh\n");
        readme.append("Content-Type: application/json\n\n");
        readme.append("{\n");
        readme.append("  \"refreshToken\": \"eyJhbGciOiJIUzI1NiIs...\"\n");
        readme.append("}\n");
        readme.append("```\n\n");

        readme.append("### Logout\n\n");
        readme.append("```http\n");
        readme.append("POST /api/auth/logout\n");
        readme.append("Authorization: Bearer eyJhbGciOiJIUzI1NiIs...\n");
        readme.append("```\n\n");
    }

    private void appendCoreFeatures(StringBuilder readme, GenerateRequest.FeaturesConfig features) {
        readme.append("## Core Features Documentation\n\n");

        if (features.isHateoas()) {
            readme.append("### HATEOAS Links\n\n");
            readme.append("All responses include hypermedia links for navigation:\n\n");
            readme.append("- `self` - Link to the current resource\n");
            readme.append("- `collection` - Link to the parent collection\n");
            readme.append("- Related entity links (e.g., `author` for a book)\n");
            readme.append("- Pagination links (`next`, `prev`, `first`, `last`)\n\n");
        }

        if (features.isSoftDelete()) {
            readme.append("### Soft Delete\n\n");
            readme.append("Entities are not permanently deleted by default:\n\n");
            readme.append(
                    "1. `DELETE /api/entity/{id}` - Sets `status=DELETED` and `deletedAt`"
                            + " timestamp\n");
            readme.append("2. Soft-deleted entities are excluded from normal queries\n");
            readme.append("3. `POST /api/entity/{id}/restore` - Restores the entity\n");
            readme.append("4. `DELETE /api/entity/{id}/hard` - Permanently deletes\n\n");
        }

        if (features.isAuditing()) {
            readme.append("### Auditing\n\n");
            readme.append("All entities automatically track:\n\n");
            readme.append("| Field | Description |\n");
            readme.append("|-------|-------------|\n");
            readme.append("| `createdAt` | Timestamp when created |\n");
            readme.append("| `updatedAt` | Timestamp of last update |\n");
            readme.append("| `createdBy` | User who created (if authenticated) |\n");
            readme.append("| `updatedBy` | User who last updated |\n");
            readme.append("\n");
        }

        if (features.isCaching()) {
            readme.append("### Caching\n\n");
            readme.append("The API uses Caffeine for caching with ETag support:\n\n");
            readme.append("**ETag for conditional requests:**\n");
            readme.append("```http\n");
            readme.append("GET /api/users/1\n");
            readme.append("# Response includes: ETag: \"abc123\"\n\n");
            readme.append("# Subsequent request with If-None-Match:\n");
            readme.append("GET /api/users/1\n");
            readme.append("If-None-Match: \"abc123\"\n");
            readme.append("# Returns 304 Not Modified if unchanged\n");
            readme.append("```\n\n");
        }

        readme.append("### Error Handling (RFC 7807)\n\n");
        readme.append("All errors follow the RFC 7807 Problem Details standard:\n\n");
        readme.append("```json\n");
        readme.append("{\n");
        readme.append("  \"type\": \"urn:problem-type:validation-error\",\n");
        readme.append("  \"title\": \"Validation Failed\",\n");
        readme.append("  \"status\": 400,\n");
        readme.append("  \"detail\": \"Request validation failed\",\n");
        readme.append("  \"instance\": \"/api/users\",\n");
        readme.append("  \"timestamp\": \"2024-01-15T10:30:00Z\",\n");
        readme.append("  \"errors\": [\n");
        readme.append("    { \"field\": \"email\", \"message\": \"must be a valid email\" }\n");
        readme.append("  ]\n");
        readme.append("}\n");
        readme.append("```\n\n");

        readme.append("**HTTP Status Codes:**\n\n");
        readme.append("| Code | Description |\n");
        readme.append("|------|-------------|\n");
        readme.append("| 200 | Success |\n");
        readme.append("| 201 | Created |\n");
        readme.append("| 204 | No Content (successful delete) |\n");
        readme.append("| 304 | Not Modified (ETag match) |\n");
        readme.append("| 400 | Bad Request (validation error) |\n");
        readme.append("| 401 | Unauthorized |\n");
        readme.append("| 403 | Forbidden |\n");
        readme.append("| 404 | Not Found |\n");
        readme.append("| 409 | Conflict (duplicate) |\n");
        readme.append("| 429 | Too Many Requests (rate limited) |\n");
        readme.append("| 500 | Internal Server Error |\n");
        readme.append("\n");
    }

    private void appendApiDocumentation(
            StringBuilder readme, GenerateRequest.FeaturesConfig features) {
        readme.append("## API Documentation\n\n");
        readme.append("Once running, access:\n\n");
        readme.append("| Resource | URL |\n");
        readme.append("|----------|-----|\n");
        if (features.isSwagger()) {
            readme.append("| Swagger UI | http://localhost:8080/swagger-ui.html |\n");
            readme.append("| OpenAPI JSON | http://localhost:8080/api-docs |\n");
            readme.append("| OpenAPI YAML | http://localhost:8080/api-docs.yaml |\n");
        }
        readme.append("| Health Check | http://localhost:8080/actuator/health |\n");
        readme.append("| Info | http://localhost:8080/actuator/info |\n");
        readme.append("| Metrics | http://localhost:8080/actuator/metrics |\n");
        // H2 Console is always available in dev profile (uses H2 in-memory by default)
        readme.append("| H2 Console (dev) | http://localhost:8080/h2-console |\n");
        readme.append("\n");
    }

    private void appendConfiguration(
            StringBuilder readme,
            GenerateRequest.ProjectConfig config,
            GenerateRequest.DatabaseConfig db,
            boolean securityEnabled) {
        readme.append("## Configuration\n\n");

        readme.append("### Environment Variables\n\n");
        readme.append("| Variable | Description | Default | Required |\n");
        readme.append("|----------|-------------|---------|----------|\n");
        readme.append("| `SERVER_PORT` | Application port | `8080` | No |\n");
        readme.append("| `SPRING_PROFILES_ACTIVE` | Active profile | `dev` | No |\n");

        if (db != null && !"h2".equalsIgnoreCase(db.getType())) {
            readme.append("| `DB_HOST` | Database host | `localhost` | Yes (prod) |\n");
            readme.append("| `DB_PORT` | Database port | `")
                    .append(db.getPort() != null ? db.getPort() : getDefaultPort(db.getType()))
                    .append("` | No |\n");
            readme.append("| `DB_NAME` | Database name | `")
                    .append(db.getName() != null ? db.getName() : config.getArtifactId())
                    .append("` | No |\n");
            readme.append("| `DB_USERNAME` | Database user | - | Yes (prod) |\n");
            readme.append("| `DB_PASSWORD` | Database password | - | Yes (prod) |\n");
        }

        if (securityEnabled) {
            readme.append("| `JWT_SECRET` | JWT signing key (min 64 chars) | - | Yes |\n");
            readme.append(
                    "| `JWT_EXPIRATION` | Access token expiration (seconds) | `1800` | No |\n");
            readme.append(
                    "| `JWT_REFRESH_EXPIRATION` | Refresh token expiration (seconds) | `604800` |"
                            + " No |\n");
        }

        readme.append("\n");

        readme.append("### Profiles\n\n");
        readme.append("| Profile | Description | Database |\n");
        readme.append("|---------|-------------|----------|\n");
        readme.append("| `dev` | Development with hot reload | H2 in-memory |\n");
        readme.append("| `test` | Integration testing | TestContainers |\n");
        readme.append("| `prod` | Production | External DB |\n");
        readme.append("\n");

        readme.append("```bash\n");
        readme.append("# Run with specific profile\n");
        readme.append("./gradlew bootRun --args='--spring.profiles.active=prod'\n\n");
        readme.append("# Or with environment variable\n");
        readme.append("SPRING_PROFILES_ACTIVE=prod ./gradlew bootRun\n");
        readme.append("```\n\n");
    }

    private void appendProjectStructure(
            StringBuilder readme, GenerateRequest.ProjectConfig config) {
        readme.append("## Project Structure\n\n");
        readme.append("```\n");
        readme.append(config.getArtifactId()).append("/\n");
        readme.append("├── src/main/java/")
                .append(config.getBasePackage().replace('.', '/'))
                .append("/\n");
        readme.append("│   ├── Application.java              # Main entry point\n");
        readme.append("│   ├── config/                       # Configuration classes\n");
        readme.append("│   └── [entity]/                     # Per-entity modules\n");
        readme.append("│       ├── domain/\n");
        readme.append("│       │   ├── entity/               # JPA entities\n");
        readme.append("│       │   └── repository/           # Spring Data repositories\n");
        readme.append("│       ├── application/\n");
        readme.append("│       │   ├── dto/                  # Request/Response DTOs\n");
        readme.append("│       │   ├── mapper/               # MapStruct mappers\n");
        readme.append("│       │   └── service/              # Business logic\n");
        readme.append("│       └── infrastructure/\n");
        readme.append("│           └── controller/           # REST controllers\n");
        readme.append("├── src/main/resources/\n");
        readme.append("│   ├── application.yml               # Main config\n");
        readme.append("│   ├── application-dev.yml           # Dev profile\n");
        readme.append("│   ├── application-test.yml          # Test profile\n");
        readme.append("│   └── db/migration/                 # Flyway migrations\n");
        readme.append("├── src/test/java/                    # Unit & integration tests\n");
        readme.append("├── docker-compose.yml                # Docker services\n");
        readme.append("├── Dockerfile                        # App container\n");
        if (config.getFeatures() != null && config.getFeatures().isMiseTasks()) {
            readme.append("├── mise.toml                         # Mise configuration\n");
        }
        readme.append("├── api-tests.http                    # HTTP client tests\n");
        readme.append("└── postman-collection.json           # Postman collection\n");
        readme.append("```\n\n");
    }

    private void appendTesting(StringBuilder readme, boolean miseEnabled) {
        readme.append("## Testing\n\n");

        if (miseEnabled) {
            readme.append("### With Mise\n\n");
            readme.append("```bash\n");
            readme.append("mise run test              # All tests\n");
            readme.append("mise run test:unit         # Unit tests only\n");
            readme.append("mise run test:integration  # Integration tests\n");
            readme.append("mise run test:coverage     # With coverage report\n");
            readme.append("```\n\n");
            readme.append("### With Gradle\n\n");
        }

        readme.append("```bash\n");
        readme.append("# Run all tests\n");
        readme.append("./gradlew test\n\n");
        readme.append("# Run with coverage report\n");
        readme.append("./gradlew test jacocoTestReport\n");
        readme.append("# Report at: build/reports/jacoco/test/html/index.html\n\n");
        readme.append("# Run specific test class\n");
        readme.append("./gradlew test --tests \"UserServiceTest\"\n\n");
        readme.append("# Run with specific profile\n");
        readme.append("./gradlew test -Dspring.profiles.active=test\n");
        readme.append("```\n\n");

        readme.append("### Test Files Included\n\n");
        readme.append("- `api-tests.http` - HTTP client tests for IntelliJ/VS Code REST Client\n");
        readme.append("- `postman-collection.json` - Importable Postman collection\n");
        readme.append("- `src/test/java/` - Unit and integration tests\n\n");
    }

    private void appendTroubleshooting(StringBuilder readme) {
        readme.append("## Troubleshooting\n\n");

        readme.append("### Port Already in Use\n\n");
        readme.append("```bash\n");
        readme.append("# Find process using port 8080\n");
        readme.append("lsof -i :8080          # macOS/Linux\n");
        readme.append("netstat -ano | findstr :8080  # Windows\n\n");
        readme.append("# Kill the process\n");
        readme.append("kill -9 <PID>          # macOS/Linux\n");
        readme.append("taskkill /PID <PID> /F # Windows\n");
        readme.append("```\n\n");

        readme.append("### Database Connection Issues\n\n");
        readme.append("```bash\n");
        readme.append("# Check if database is running\n");
        readme.append("docker-compose ps\n\n");
        readme.append("# Restart database\n");
        readme.append("docker-compose restart db\n\n");
        readme.append("# View database logs\n");
        readme.append("docker-compose logs db\n");
        readme.append("```\n\n");

        readme.append("### Clean Build\n\n");
        readme.append("```bash\n");
        readme.append("# Clean and rebuild\n");
        readme.append("./gradlew clean build\n\n");
        readme.append("# Clean Docker volumes (reset database)\n");
        readme.append("docker-compose down -v\n");
        readme.append("docker-compose up -d\n");
        readme.append("```\n\n");

        readme.append("### View Application Logs\n\n");
        readme.append("```bash\n");
        readme.append("# Local development\n");
        readme.append("./gradlew bootRun  # Logs appear in console\n\n");
        readme.append("# Docker\n");
        readme.append("docker-compose logs -f app\n");
        readme.append("```\n\n");
    }

    private void appendLicense(StringBuilder readme) {
        readme.append("## License\n\n");
        readme.append(
                "This project was generated with [APiGen"
                        + " Studio](https://apigen-studio.vercel.app).\n\n");
        readme.append("---\n\n");
        readme.append(
                "Generated with APiGen | [Documentation](https://github.com/jnzader/apigen) |"
                        + " [Report Issues](https://github.com/jnzader/apigen/issues)\n");
    }

    /** Builds constraint description for a column. */
    private String buildConstraints(SqlColumn col) {
        List<String> constraints = new java.util.ArrayList<>();
        if (col.isPrimaryKey()) constraints.add("PK");
        if (!col.isNullable()) constraints.add("Required");
        if (col.isUnique()) constraints.add("Unique");
        if (col.getDefaultValue() != null) constraints.add("Default: " + col.getDefaultValue());
        return constraints.isEmpty() ? "-" : String.join(", ", constraints);
    }

    /** Gets default database port. */
    private String getDefaultPort(String dbType) {
        if (dbType == null) return "5432";
        return switch (dbType.toLowerCase(Locale.ROOT)) {
            case "mysql", "mariadb" -> "3306";
            case "postgresql", "postgres" -> "5432";
            case "mongodb" -> "27017";
            case "sqlserver" -> "1433";
            case "oracle" -> "1521";
            default -> "5432";
        };
    }

    /** Capitalizes the first letter of a string. */
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase(Locale.ROOT)
                + str.substring(1).toLowerCase(Locale.ROOT);
    }

    /**
     * Gets the gitignore content.
     *
     * @return the .gitignore content
     */
    public String getGitignoreContent() {
        return GITIGNORE_CONTENT;
    }

    /**
     * Generates the ApplicationContextTest class to verify Spring context loads correctly.
     *
     * <p>This test catches runtime configuration issues like:
     *
     * <ul>
     *   <li>BeanDefinitionOverrideException from duplicate bean registrations
     *   <li>Missing required beans
     *   <li>Circular dependencies
     *   <li>Invalid configuration properties
     * </ul>
     *
     * @param config the project configuration
     * @return the ApplicationContextTest class content
     */
    public String generateApplicationContextTest(GenerateRequest.ProjectConfig config) {
        String className = toPascalCase(config.getArtifactId()) + "Application";
        String basePackage = config.getBasePackage();

        return
"""
package %s;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke test to verify the Spring application context loads correctly.
 *
 * <p>This test catches configuration issues like:
 * <ul>
 *   <li>Duplicate bean definitions (BeanDefinitionOverrideException)
 *   <li>Missing required beans
 *   <li>Circular dependencies
 *   <li>Invalid configuration properties
 * </ul>
 */
@SpringBootTest(classes = %s.class)
@ActiveProfiles("test")
@DisplayName("Application Context Tests")
class ApplicationContextTest {

    @Test
    @DisplayName("Application context should load successfully")
    void contextLoads() {
        // If we get here, the context loaded successfully
    }
}
"""
                .formatted(basePackage, className);
    }
}
