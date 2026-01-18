# Resources

Este directorio contiene los **archivos de configuración** y recursos estáticos de la aplicación.

## Estructura

```
resources/
├── application.yaml           # Configuración principal
├── application-dev.yaml       # Perfil desarrollo
├── application-prod.yaml      # Perfil producción
├── logback-spring.xml         # Configuración de logging
├── db/
│   └── migration/             # Migraciones Flyway
│       ├── V1__initial_schema.sql
│       └── V{n}__description.sql
└── openapi/
    └── api-spec.json          # OpenAPI spec generado
```

## Archivos de Configuración

### application.yaml

Configuración base compartida por todos los perfiles:

```yaml
spring:
  application:
    name: apigen

apigen:
  security:
    jwt:
      secret: ${JWT_SECRET}
      expiration-minutes: 15
      refresh-expiration-minutes: 10080

app:
  api:
    version: v1
    base-path: /api
  cache:
    entities:
      max-size: 1000
      expire-after-write: 10m
```

### application-dev.yaml

Configuración para **desarrollo local**:

```yaml
spring:
  profiles: dev

apigen:
  security:
    enabled: false              # Sin auth en dev

spring:
  jpa:
    show-sql: true
    hibernate:
      ddl-auto: update          # Auto-crear tablas

logging:
  level:
    com.jnzader.apigen: DEBUG
    org.hibernate.SQL: DEBUG
```

### application-prod.yaml

Configuración para **producción**:

```yaml
spring:
  profiles: prod

apigen:
  security:
    enabled: true

spring:
  jpa:
    hibernate:
      ddl-auto: validate        # Solo validar schema

logging:
  level:
    root: WARN
    com.jnzader.apigen: INFO
```

## Activar Perfiles

```bash
# Variable de entorno
export SPRING_PROFILES_ACTIVE=dev

# Parámetro JVM
java -jar app.jar -Dspring.profiles.active=prod

# Gradle
./gradlew bootRun -Dspring.profiles.active=dev
```

## Variables de Entorno

| Variable | Descripción | Requerida |
|----------|-------------|-----------|
| `JWT_SECRET` | Secret para firmar JWT (64+ chars) | Sí |
| `DATABASE_URL` | JDBC URL de PostgreSQL | Sí |
| `DATABASE_USERNAME` | Usuario de DB | Sí |
| `DATABASE_PASSWORD` | Contraseña de DB | Sí |
| `SPRING_PROFILES_ACTIVE` | Perfil activo | No (default: none) |

## Migraciones Flyway

### Convención de Nombres

```
V{version}__{description}.sql
  │          │
  │          └── Descripción en snake_case
  └── Número de versión (1, 2, 3...)
```

**Ejemplos:**
```
V1__initial_schema.sql
V2__create_products_table.sql
V3__add_product_category_fk.sql
```

### Crear Nueva Migración

```sql
-- V2__create_products_table.sql

CREATE TABLE products (
    id BIGINT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    -- Campos de Base
    estado BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ...
);

-- Índices
CREATE INDEX idx_products_estado ON products(estado);

-- Tabla de auditoría
CREATE TABLE products_aud (...);
```

### Ejecutar Migraciones

```bash
# Automático al iniciar
./gradlew bootRun

# Manual
./gradlew flywayMigrate

# Ver estado
./gradlew flywayInfo
```

## Logging (logback-spring.xml)

### Configuración por Perfil

```xml
<!-- Desarrollo: Console con colores -->
<springProfile name="dev">
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss} %highlight(%-5level) %cyan(%logger{36}) - %msg%n</pattern>
        </encoder>
    </appender>
</springProfile>

<!-- Producción: JSON para ELK -->
<springProfile name="prod">
    <appender name="JSON" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashEncoder"/>
    </appender>
</springProfile>
```

### MDC (Mapped Diagnostic Context)

```xml
<pattern>%d [%X{requestId}] [%X{userId}] %msg%n</pattern>
```

Incluye automáticamente `requestId` y `userId` de los filtros.

## OpenAPI Spec

Generado automáticamente en build:

```bash
./gradlew generateOpenApiDocs
```

Ubicación: `src/main/resources/openapi/api-spec.json`

Usado para generar SDKs de cliente.
