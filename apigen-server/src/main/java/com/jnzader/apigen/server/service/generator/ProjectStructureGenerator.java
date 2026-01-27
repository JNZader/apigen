package com.jnzader.apigen.server.service.generator;

import static com.jnzader.apigen.server.service.generator.util.StringTransformationUtil.toCamelCase;
import static com.jnzader.apigen.server.service.generator.util.StringTransformationUtil.toKebabCase;
import static com.jnzader.apigen.server.service.generator.util.StringTransformationUtil.toPascalCase;

import com.jnzader.apigen.codegen.model.SqlColumn;
import com.jnzader.apigen.codegen.model.SqlSchema;
import com.jnzader.apigen.codegen.model.SqlTable;
import com.jnzader.apigen.server.dto.GenerateRequest;
import java.util.List;
import org.springframework.stereotype.Component;

/** Generates project structure files (Main class, README). */
@Component
@SuppressWarnings("java:S1192") // Gitignore strings intentional for clarity
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
        GenerateRequest.FeaturesConfig features = config.getFeatures();
        GenerateRequest.DatabaseConfig db = config.getDatabase();

        // Header
        readme.append("# ").append(config.getName()).append("\n\n");
        readme.append(
                "REST API generated with [APiGen Studio](https://apigen-studio.vercel.app)\n\n");

        // Tech Stack
        readme.append("## Tech Stack\n\n");
        readme.append("| Technology | Version |\n");
        readme.append("|------------|--------|\n");
        readme.append("| Java | ").append(config.getJavaVersion()).append(" |\n");
        readme.append("| Spring Boot | ").append(config.getSpringBootVersion()).append(" |\n");
        if (db != null) {
            readme.append("| Database | ").append(db.getType()).append(" |\n");
        }
        readme.append("\n");

        // Features
        if (features != null) {
            readme.append("## Features\n\n");
            if (features.isSwagger()) readme.append("- OpenAPI/Swagger documentation\n");
            if (features.isHateoas()) readme.append("- HATEOAS hypermedia links\n");
            if (features.isAuditing())
                readme.append("- Audit trails (created/updated timestamps)\n");
            if (features.isSoftDelete()) readme.append("- Soft delete support\n");
            if (features.isCaching()) readme.append("- Redis/Caffeine caching\n");
            if (features.isDocker()) readme.append("- Docker containerization\n");
            if (config.getModules() != null && config.getModules().isSecurity()) {
                readme.append("- JWT authentication & authorization\n");
            }
            readme.append("- Pagination and filtering\n");
            readme.append("- Input validation\n");
            readme.append("- Exception handling\n\n");
        }

        // Entities and Endpoints
        if (schema != null && !schema.getEntityTables().isEmpty()) {
            readme.append("## Entities\n\n");
            List<SqlTable> entities = schema.getEntityTables();

            for (SqlTable table : entities) {
                String entityName = toPascalCase(table.getName());
                String endpoint = "/" + toKebabCase(table.getName());

                readme.append("### ").append(entityName).append("\n\n");

                // Fields table
                readme.append("| Field | Type | Constraints |\n");
                readme.append("|-------|------|-------------|\n");
                for (SqlColumn col : table.getColumns()) {
                    String constraints = buildConstraints(col);
                    readme.append("| ")
                            .append(toCamelCase(col.getName()))
                            .append(" | ")
                            .append(col.getJavaType())
                            .append(" | ")
                            .append(constraints)
                            .append(" |\n");
                }
                readme.append("\n");

                // Endpoints
                readme.append("**Endpoints:**\n");
                readme.append("| Method | Path | Description |\n");
                readme.append("|--------|------|-------------|\n");
                readme.append("| GET | `").append(endpoint).append("` | List all (paginated) |\n");
                readme.append("| GET | `").append(endpoint).append("/{id}` | Get by ID |\n");
                readme.append("| POST | `").append(endpoint).append("` | Create new |\n");
                readme.append("| PUT | `").append(endpoint).append("/{id}` | Update |\n");
                readme.append("| DELETE | `").append(endpoint).append("/{id}` | Delete |\n");
                readme.append("\n");
            }
        }

        // Quick Start
        readme.append("## Quick Start\n\n");
        readme.append("### Prerequisites\n\n");
        readme.append("- Java ").append(config.getJavaVersion()).append("+\n");
        if (db != null && !"h2".equalsIgnoreCase(db.getType())) {
            readme.append("- ").append(db.getType()).append(" database\n");
        }
        if (features != null && features.isDocker()) {
            readme.append("- Docker & Docker Compose (optional)\n");
        }
        readme.append("\n");

        readme.append("### Run Locally\n\n");
        readme.append("```bash\n");
        readme.append("# Clone the repository\n");
        readme.append("git clone <repository-url>\n");
        readme.append("cd ").append(config.getArtifactId()).append("\n\n");
        readme.append("# Build the project\n");
        readme.append("./gradlew build\n\n");
        readme.append("# Run the application\n");
        readme.append("./gradlew bootRun\n");
        readme.append("```\n\n");

        // Docker
        if (features != null && features.isDocker()) {
            readme.append("### Run with Docker\n\n");
            readme.append("```bash\n");
            readme.append("# Build and start all services\n");
            readme.append("docker-compose up -d\n\n");
            readme.append("# View logs\n");
            readme.append("docker-compose logs -f app\n\n");
            readme.append("# Stop services\n");
            readme.append("docker-compose down\n");
            readme.append("```\n\n");
        }

        // API Documentation
        readme.append("## API Documentation\n\n");
        readme.append("Once running, access:\n\n");
        readme.append("| Resource | URL |\n");
        readme.append("|----------|-----|\n");
        if (features != null && features.isSwagger()) {
            readme.append("| Swagger UI | http://localhost:8080/swagger-ui.html |\n");
            readme.append("| OpenAPI JSON | http://localhost:8080/api-docs |\n");
        }
        if (db != null && "h2".equalsIgnoreCase(db.getType())) {
            readme.append("| H2 Console | http://localhost:8080/h2-console |\n");
        }
        readme.append("| Health Check | http://localhost:8080/actuator/health |\n\n");

        // Environment Variables
        readme.append("## Environment Variables\n\n");
        readme.append("| Variable | Description | Default |\n");
        readme.append("|----------|-------------|--------|\n");
        readme.append("| `SERVER_PORT` | Application port | `8080` |\n");
        if (db != null && !"h2".equalsIgnoreCase(db.getType())) {
            readme.append("| `DB_HOST` | Database host | `localhost` |\n");
            readme.append("| `DB_PORT` | Database port | `")
                    .append(db.getPort() != null ? db.getPort() : getDefaultPort(db.getType()))
                    .append("` |\n");
            readme.append("| `DB_NAME` | Database name | `")
                    .append(db.getName() != null ? db.getName() : config.getArtifactId())
                    .append("` |\n");
            readme.append("| `DB_USERNAME` | Database user | - |\n");
            readme.append("| `DB_PASSWORD` | Database password | - |\n");
        }
        if (config.getModules() != null && config.getModules().isSecurity()) {
            readme.append("| `JWT_SECRET` | JWT signing key | - |\n");
            readme.append("| `JWT_EXPIRATION` | Token expiration (ms) | `86400000` |\n");
        }
        readme.append("\n");

        // Project Structure
        readme.append("## Project Structure\n\n");
        readme.append("```\n");
        readme.append("src/main/java/")
                .append(config.getBasePackage().replace('.', '/'))
                .append("/\n");
        readme.append("├── Application.java           # Main entry point\n");
        readme.append("├── [module]/\n");
        readme.append("│   ├── domain/\n");
        readme.append("│   │   ├── entity/           # JPA entities\n");
        readme.append("│   │   └── repository/       # Spring Data repositories\n");
        readme.append("│   ├── application/\n");
        readme.append("│   │   ├── dto/              # Request/Response DTOs\n");
        readme.append("│   │   ├── mapper/           # MapStruct mappers\n");
        readme.append("│   │   └── service/          # Business logic\n");
        readme.append("│   └── infrastructure/\n");
        readme.append("│       └── controller/       # REST controllers\n");
        readme.append("```\n\n");

        // Testing
        readme.append("## Testing\n\n");
        readme.append("```bash\n");
        readme.append("# Run all tests\n");
        readme.append("./gradlew test\n\n");
        readme.append("# Run with coverage\n");
        readme.append("./gradlew test jacocoTestReport\n");
        readme.append("```\n\n");
        readme.append("API test files are included:\n");
        readme.append("- `api-tests.http` - HTTP client tests (IntelliJ/VS Code REST Client)\n");
        readme.append("- `postman-collection.json` - Postman collection\n\n");

        // License
        readme.append("## License\n\n");
        readme.append("This project was generated with APiGen Studio.\n");

        return readme.toString();
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
        return switch (dbType.toLowerCase()) {
            case "mysql", "mariadb" -> "3306";
            case "postgresql", "postgres" -> "5432";
            case "mongodb" -> "27017";
            case "sqlserver" -> "1433";
            case "oracle" -> "1521";
            default -> "5432";
        };
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
