# Docker

Este directorio contiene las **configuraciones de Docker** para los servicios de soporte.

## Estructura

```
docker/
├── postgres/
│   └── init/
│       └── 01-init.sql        # Script de inicialización
├── prometheus/
│   └── prometheus.yml         # Configuración de scraping
└── grafana/
    ├── provisioning/
    │   ├── datasources/
    │   │   └── prometheus.yml # Conexión a Prometheus
    │   └── dashboards/
    │       └── apigen.json    # Dashboard predefinido
    └── grafana.ini            # Configuración Grafana
```

## Servicios (docker-compose.yml)

| Servicio | Puerto | Descripción |
|----------|--------|-------------|
| `apigen` | 8080 | Aplicación principal |
| `postgres` | 5432 | Base de datos |
| `pgadmin` | 5050 | UI de PostgreSQL |
| `prometheus` | 9090 | Recolección de métricas |
| `grafana` | 3000 | Dashboards |

## Comandos

```bash
# Iniciar todos los servicios
docker-compose up -d

# Ver logs
docker-compose logs -f apigen

# Solo base de datos
docker-compose up -d postgres

# Reconstruir imagen de app
docker-compose up -d --build apigen

# Detener
docker-compose down

# Detener y eliminar volúmenes
docker-compose down -v
```

## PostgreSQL

### Inicialización

El script `postgres/init/01-init.sql` se ejecuta al crear el container:

```sql
-- Crear usuario y base de datos
CREATE USER apigen WITH PASSWORD 'apigen';
CREATE DATABASE apigen OWNER apigen;

-- Extensiones
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
```

### Conexión

```
Host: localhost (o postgres desde otros containers)
Port: 5432
Database: apigen
User: apigen
Password: apigen (dev) / ${DB_PASSWORD} (prod)
```

### Backup/Restore

```bash
# Backup
docker-compose exec postgres pg_dump -U apigen apigen > backup.sql

# Restore
docker-compose exec -T postgres psql -U apigen apigen < backup.sql
```

## Prometheus

### Configuración (prometheus.yml)

```yaml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: 'apigen'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['apigen:8080']
```

### Métricas Disponibles

| Métrica | Descripción |
|---------|-------------|
| `http_server_requests_seconds` | Latencia HTTP |
| `jvm_memory_used_bytes` | Uso de memoria |
| `hikaricp_connections_active` | Conexiones DB activas |
| `cache_gets_total` | Hits/misses de cache |
| `resilience4j_circuitbreaker_state` | Estado circuit breaker |

### Acceso

```
URL: http://localhost:9090
```

## Grafana

### Acceso

```
URL: http://localhost:3000
Usuario: admin
Password: admin
```

### Dashboard Predefinido

El dashboard `apigen.json` incluye:
- Request rate y latencia
- Error rate
- JVM metrics (heap, GC)
- Database connections
- Cache hit rate

### Agregar Dashboard

1. Ir a Dashboards → Import
2. Pegar JSON o cargar archivo
3. Seleccionar datasource Prometheus

## PgAdmin

### Acceso

```
URL: http://localhost:5050
Email: admin@apigen.com
Password: admin
```

### Conectar a PostgreSQL

1. Add New Server
2. Name: apigen
3. Connection:
   - Host: postgres
   - Port: 5432
   - Database: apigen
   - Username: apigen
   - Password: apigen

## Dockerfile (Multi-stage)

```dockerfile
# Stage 1: Build
FROM eclipse-temurin:25-jdk-alpine AS builder
WORKDIR /app
COPY . .
RUN ./gradlew bootJar --no-daemon
RUN java -Djarmode=layertools -jar build/libs/*.jar extract

# Stage 2: Runtime
FROM eclipse-temurin:25-jre-alpine
RUN addgroup -S apigen && adduser -S apigen -G apigen
USER apigen
WORKDIR /app

# Copiar layers (mejor cache)
COPY --from=builder /app/dependencies/ ./
COPY --from=builder /app/spring-boot-loader/ ./
COPY --from=builder /app/snapshot-dependencies/ ./
COPY --from=builder /app/application/ ./

EXPOSE 8080
HEALTHCHECK --interval=30s CMD wget -q -O /dev/null http://localhost:8080/actuator/health

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
```

## Variables de Entorno

### Development

```env
DATABASE_URL=jdbc:postgresql://postgres:5432/apigen
DATABASE_USERNAME=apigen
DATABASE_PASSWORD=apigen
JWT_SECRET=dev-secret-key-min-64-chars-for-development-only-not-production
SPRING_PROFILES_ACTIVE=dev
```

### Production

```env
DATABASE_URL=jdbc:postgresql://prod-db:5432/apigen
DATABASE_USERNAME=${DB_USER}
DATABASE_PASSWORD=${DB_PASSWORD}
JWT_SECRET=${JWT_SECRET}  # Generado con openssl rand -base64 64
SPRING_PROFILES_ACTIVE=prod
```

## Health Checks

```bash
# Aplicación
curl http://localhost:8080/actuator/health

# PostgreSQL
docker-compose exec postgres pg_isready -U apigen

# Todos los servicios
docker-compose ps
```

## Troubleshooting

### Container no inicia

```bash
# Ver logs
docker-compose logs apigen

# Verificar recursos
docker stats
```

### No conecta a DB

```bash
# Verificar red
docker network ls
docker network inspect apigen_default

# Test conexión
docker-compose exec apigen ping postgres
```

### Limpiar todo

```bash
# Eliminar containers, networks, volumes
docker-compose down -v --rmi all

# Limpiar sistema Docker
docker system prune -a
```
