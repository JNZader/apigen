package com.jnzader.apigen.server.service.generator;

import static com.jnzader.apigen.server.service.generator.util.StringTransformationUtil.toPascalCase;

import com.jnzader.apigen.server.dto.GenerateRequest;
import org.springframework.stereotype.Component;

/** Generates project structure files (Main class, README). */
@Component
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
     * Generates the README.md file content.
     *
     * @param config the project configuration
     * @return the README content
     */
    public String generateReadme(GenerateRequest.ProjectConfig config) {
        return
"""
# %s

Generated with [APiGen Studio](https://github.com/jnzader/apigen)

## Quick Start

```bash
# Build the project
./gradlew build

# Run the application
./gradlew bootRun
```

## API Documentation

Once running, access:
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/api-docs
- H2 Console: http://localhost:8080/h2-console

## Project Structure

```
src/main/java/%s/
├── Application.java           # Main entry point
├── [module]/
│   ├── domain/
│   │   ├── entity/           # JPA entities
│   │   └── repository/       # Spring Data repositories
│   ├── application/
│   │   ├── dto/              # Data Transfer Objects
│   │   ├── mapper/           # MapStruct mappers
│   │   └── service/          # Business services
│   └── infrastructure/
│       └── controller/       # REST controllers
```

## Features

- CRUD operations with pagination and filtering
- HATEOAS links
- Soft delete support
- Audit trails (created/updated by)
- OpenAPI documentation
- Caching support
"""
                .formatted(config.getName(), config.getBasePackage().replace('.', '/'));
    }

    /**
     * Gets the gitignore content.
     *
     * @return the .gitignore content
     */
    public String getGitignoreContent() {
        return GITIGNORE_CONTENT;
    }
}
