# Docker Guide for my-api

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
docker build -t my-api:latest .
```

### Running Standalone

If you have an external database:
```bash
docker run -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host:5432/mydb \
  -e SPRING_DATASOURCE_USERNAME=user \
  -e SPRING_DATASOURCE_PASSWORD=pass \
  my-api:latest
```

## Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `SPRING_PROFILES_ACTIVE` | Active Spring profile | `docker` |
| `SPRING_DATASOURCE_URL` | Database JDBC URL | See docker-compose.yml |
| `SPRING_DATASOURCE_USERNAME` | Database username | `appuser` |
| `SPRING_DATASOURCE_PASSWORD` | Database password | `changeme` |
| `SERVER_PORT` | Application port | `8080` |
| `JAVA_OPTS` | JVM options | `-XX:+UseZGC -XX:MaxRAMPercentage=75.0` |

### Database Configuration

This project is configured to use **POSTGRESQL**.

Default connection settings:
- Host: `db` (Docker network) or `localhost` (external)
- Port: `5432`
- Database: `appdb`
- Username: `appuser`
- Password: `changeme`

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
docker-compose exec db psql -U appuser -d appdb
```

### Check container status
```bash
docker-compose ps
```

### Restart services
```bash
docker-compose restart
```
