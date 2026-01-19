package com.jnzader.apigen.server.service.generator;

import com.jnzader.apigen.server.config.GeneratedProjectVersions;
import com.jnzader.apigen.server.dto.GenerateRequest;
import org.springframework.stereotype.Component;

/**
 * Generates Docker configuration files (Dockerfile, docker-compose.yml, .dockerignore, DOCKER.md).
 */
@Component
public class DockerConfigGenerator {

    /**
     * Generates the Dockerfile content.
     *
     * @param config the project configuration
     * @return the Dockerfile content
     */
    public String generateDockerfile(GenerateRequest.ProjectConfig config) {
        String javaVersion =
                config.getJavaVersion() != null
                        ? config.getJavaVersion()
                        : GeneratedProjectVersions.JAVA_VERSION;
        String artifactId = config.getArtifactId();

        return
"""
# ============================================
# %s - Dockerfile
# ============================================
# Runtime-only container optimized for Java %s
#
# Prerequisites:
#   ./gradlew bootJar -x test
#
# Build:
#   docker build -t %s:latest .
#
# Run (standalone):
#   docker run -p 8080:8080 %s:latest
#
# Run (with docker-compose):
#   docker-compose up -d
# ============================================

# Runtime Stage (JAR must be pre-built)
# Build locally first with: ./gradlew bootJar -x test
FROM eclipse-temurin:%s-jre-alpine

# Metadata
LABEL maintainer="%s"
LABEL description="%s API Service"
LABEL version="%s"

# Create non-root user for security
RUN addgroup -g 1001 -S appgroup && \\
    adduser -u 1001 -S appuser -G appgroup

WORKDIR /app

# Create logs directory
RUN mkdir -p /app/logs && chown -R appuser:appgroup /app

# Copy pre-built JAR (build locally first with: ./gradlew bootJar -x test)
COPY --chown=appuser:appgroup build/libs/*.jar app.jar

# Switch to non-root user
USER appuser

# Environment variables
ENV JAVA_OPTS="-XX:+UseZGC -XX:+ZGenerational -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"
ENV SPRING_PROFILES_ACTIVE=docker
ENV SERVER_PORT=8080

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \\
    CMD wget -q --spider http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
"""
                .formatted(
                        artifactId,
                        javaVersion,
                        artifactId,
                        artifactId,
                        javaVersion,
                        config.getGroupId(),
                        artifactId,
                        GeneratedProjectVersions.INITIAL_PROJECT_VERSION);
    }

    /**
     * Generates the docker-compose.yml content.
     *
     * @param config the project configuration
     * @return the docker-compose.yml content
     */
    public String generateDockerCompose(GenerateRequest.ProjectConfig config) {
        String artifactId = config.getArtifactId();
        GenerateRequest.DatabaseConfig db =
                config.getDatabase() != null
                        ? config.getDatabase()
                        : new GenerateRequest.DatabaseConfig();

        String dbType = db.getType().toLowerCase();
        String dbImage = db.getDockerImage();
        int dbPort = db.getDefaultPort();

        StringBuilder compose = new StringBuilder();
        compose.append(
"""
# ============================================
# %s - Docker Compose
# ============================================
# Start all services:
#   docker-compose up -d
#
# View logs:
#   docker-compose logs -f
#
# Stop all services:
#   docker-compose down
#
# Stop and remove volumes:
#   docker-compose down -v
# ============================================

services:
  # Application Service
  app:
    build:
      context: .
      dockerfile: Dockerfile
    container_name: %s-app
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - SPRING_DATASOURCE_URL=%s
      - SPRING_DATASOURCE_USERNAME=%s
      - SPRING_DATASOURCE_PASSWORD=%s
    depends_on:
      db:
        condition: service_healthy
    networks:
      - app-network
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "wget", "-q", "--spider", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s

"""
                        .formatted(
                                artifactId,
                                artifactId,
                                db.getJdbcUrl(),
                                db.getUsername(),
                                db.getPassword()));

        // Add database service based on type
        if (dbImage != null) {
            compose.append(generateDatabaseService(db, dbType, dbImage, dbPort));
        }

        // Add networks and volumes
        compose.append(
"""
networks:
  app-network:
    driver: bridge

volumes:
  db-data:
    driver: local
""");

        return compose.toString();
    }

    /**
     * Generates the database service configuration for docker-compose.
     *
     * @param db the database configuration
     * @param dbType the database type
     * @param dbImage the Docker image
     * @param dbPort the database port
     * @return the database service YAML
     */
    public String generateDatabaseService(
            GenerateRequest.DatabaseConfig db, String dbType, String dbImage, int dbPort) {
        return switch (dbType) {
            case "mysql" ->
"""
  # MySQL Database
  db:
    image: %s
    container_name: %s-db
    ports:
      - "%d:%d"
    environment:
      - MYSQL_DATABASE=%s
      - MYSQL_USER=%s
      - MYSQL_PASSWORD=%s
      - MYSQL_ROOT_PASSWORD=%s
    volumes:
      - db-data:/var/lib/mysql
    networks:
      - app-network
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-u", "root", "-p${MYSQL_ROOT_PASSWORD}"]
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 30s

"""
                            .formatted(
                                    dbImage,
                                    db.getName(),
                                    dbPort,
                                    dbPort,
                                    db.getName(),
                                    db.getUsername(),
                                    db.getPassword(),
                                    db.getPassword());

            case "mariadb" ->
"""
  # MariaDB Database
  db:
    image: %s
    container_name: %s-db
    ports:
      - "%d:%d"
    environment:
      - MARIADB_DATABASE=%s
      - MARIADB_USER=%s
      - MARIADB_PASSWORD=%s
      - MARIADB_ROOT_PASSWORD=%s
    volumes:
      - db-data:/var/lib/mysql
    networks:
      - app-network
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "healthcheck.sh", "--connect", "--innodb_initialized"]
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 30s

"""
                            .formatted(
                                    dbImage,
                                    db.getName(),
                                    dbPort,
                                    dbPort,
                                    db.getName(),
                                    db.getUsername(),
                                    db.getPassword(),
                                    db.getPassword());

            case "sqlserver" ->
"""
  # SQL Server Database
  db:
    image: %s
    container_name: %s-db
    ports:
      - "%d:%d"
    environment:
      - ACCEPT_EULA=Y
      - MSSQL_SA_PASSWORD=%s
      - MSSQL_PID=Express
    volumes:
      - db-data:/var/opt/mssql
    networks:
      - app-network
    restart: unless-stopped
    healthcheck:
      test: ["CMD-SHELL", "/opt/mssql-tools/bin/sqlcmd -S localhost -U sa -P ${MSSQL_SA_PASSWORD} -Q 'SELECT 1' || exit 1"]
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 30s

"""
                            .formatted(dbImage, db.getName(), dbPort, dbPort, db.getPassword());

            case "oracle" ->
"""
  # Oracle Database
  db:
    image: %s
    container_name: %s-db
    ports:
      - "%d:%d"
    environment:
      - ORACLE_PASSWORD=%s
      - APP_USER=%s
      - APP_USER_PASSWORD=%s
    volumes:
      - db-data:/opt/oracle/oradata
    networks:
      - app-network
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "healthcheck.sh"]
      interval: 10s
      timeout: 5s
      retries: 10
      start_period: 60s

"""
                            .formatted(
                                    dbImage,
                                    db.getName(),
                                    dbPort,
                                    dbPort,
                                    db.getPassword(),
                                    db.getUsername(),
                                    db.getPassword());

            default ->
"""
  # PostgreSQL Database
  db:
    image: %s
    container_name: %s-db
    ports:
      - "%d:%d"
    environment:
      - POSTGRES_DB=%s
      - POSTGRES_USER=%s
      - POSTGRES_PASSWORD=%s
    volumes:
      - db-data:/var/lib/postgresql/data
    networks:
      - app-network
    restart: unless-stopped
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U %s -d %s"]
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 10s

"""
                            .formatted(
                                    dbImage,
                                    db.getName(),
                                    dbPort,
                                    dbPort,
                                    db.getName(),
                                    db.getUsername(),
                                    db.getPassword(),
                                    db.getUsername(),
                                    db.getName());
        };
    }

    /**
     * Generates the .dockerignore file content.
     *
     * @return the .dockerignore content
     */
    public String generateDockerignore() {
        return
"""
# Build artifacts (keep JARs for runtime-only Docker build)
build/
!build/libs/
!build/libs/*.jar
.gradle/
bin/
out/

# IDE files
.idea/
*.iml
*.ipr
*.iws
.project
.classpath
.settings/
.vscode/

# Git
.git/
.gitignore

# Logs
*.log
logs/

# OS files
.DS_Store
Thumbs.db

# Environment files
.env
*.local
.env.*

# Test files (not needed in production image)
src/test/

# Documentation
*.md
!README.md

# Docker files (not needed inside container)
docker-compose*.yml
Dockerfile*
.dockerignore
""";
    }

    /**
     * Generates DOCKER.md with instructions.
     *
     * @param config the project configuration
     * @return the DOCKER.md content
     */
    public String generateDockerReadme(GenerateRequest.ProjectConfig config) {
        String artifactId = config.getArtifactId();
        GenerateRequest.DatabaseConfig db =
                config.getDatabase() != null
                        ? config.getDatabase()
                        : new GenerateRequest.DatabaseConfig();

        return
"""
# Docker Guide for %s

## Quick Start

### Using Docker Compose (Recommended)

**Step 1: Build the JAR**
```bash
./gradlew bootJar -x test
```

**Step 2: Start all services (app + database)**
```bash
docker-compose up -d --build
```

View logs:
```bash
docker-compose logs -f app
```

Stop all services:
```bash
docker-compose down
```

Stop and remove all data:
```bash
docker-compose down -v
```

### Building the Image Manually

**Step 1: Build the JAR**
```bash
./gradlew bootJar -x test
```

**Step 2: Build Docker image**
```bash
docker build -t %s:latest .
```

### Running Standalone

If you have an external database:
```bash
docker run -p 8080:8080 \\
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host:5432/mydb \\
  -e SPRING_DATASOURCE_USERNAME=user \\
  -e SPRING_DATASOURCE_PASSWORD=pass \\
  %s:latest
```

## Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `SPRING_PROFILES_ACTIVE` | Active Spring profile | `docker` |
| `SPRING_DATASOURCE_URL` | Database JDBC URL | See docker-compose.yml |
| `SPRING_DATASOURCE_USERNAME` | Database username | `%s` |
| `SPRING_DATASOURCE_PASSWORD` | Database password | `%s` |
| `SERVER_PORT` | Application port | `8080` |
| `JAVA_OPTS` | JVM options | `-XX:+UseZGC -XX:MaxRAMPercentage=75.0` |

### Database Configuration

This project is configured to use **%s**.

Default connection settings:
- Host: `db` (Docker network) or `localhost` (external)
- Port: `%d`
- Database: `%s`
- Username: `%s`
- Password: `%s`

## Health Checks

The application exposes health endpoints:
- `http://localhost:8080/actuator/health` - Overall health
- `http://localhost:8080/actuator/health/liveness` - Liveness probe
- `http://localhost:8080/actuator/health/readiness` - Readiness probe

## Accessing the API

Once running, access:
- **API Base URL**: http://localhost:8080/api/v1
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI Spec**: http://localhost:8080/v3/api-docs

## Production Considerations

1. **Change default passwords** in docker-compose.yml
2. **Use Docker secrets** for sensitive data
3. **Configure proper logging** (e.g., to external log aggregator)
4. **Set resource limits** in docker-compose.yml:
   ```yaml
   deploy:
     resources:
       limits:
         cpus: '2'
         memory: 2G
   ```
5. **Use a reverse proxy** (nginx, traefik) for SSL termination
6. **Regular backups** of the database volume

## Troubleshooting

### Container won't start
```bash
docker-compose logs app
```

### Database connection issues
```bash
docker-compose exec db psql -U %s -d %s
```

### Check container status
```bash
docker-compose ps
```

### Restart services
```bash
docker-compose restart
```
"""
                .formatted(
                        artifactId,
                        artifactId,
                        artifactId,
                        db.getUsername(),
                        db.getPassword(),
                        db.getType().toUpperCase(),
                        db.getDefaultPort(),
                        db.getName(),
                        db.getUsername(),
                        db.getPassword(),
                        db.getUsername(),
                        db.getName());
    }
}
