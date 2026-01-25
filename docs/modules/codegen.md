# CodeGen Module

The `apigen-codegen` module is a multi-language code generator that creates complete API projects from SQL schemas.

## Supported Languages (9)

| Language | Framework | Version | Key Features |
|----------|-----------|---------|--------------|
| **Java** | Spring Boot | 4.x (Java 25) | CRUD, HATEOAS, JWT, OAuth2, Rate Limiting, Batch Ops, Full tests |
| **Kotlin** | Spring Boot | 4.x (Kotlin 2.1) | Data classes, sealed classes, coroutines |
| **Python** | FastAPI | 0.128.0 (Python 3.12) | Async SQLAlchemy, Pydantic, automatic OpenAPI |
| **TypeScript** | NestJS | 11.x (TS 5.9) | TypeORM, class-validator, OpenAPI decorators |
| **PHP** | Laravel | 12.0 (PHP 8.4) | Eloquent, migrations, API resources |
| **Go** | Gin | 1.10.x (Go 1.23) | GORM, go-playground/validator, Swagger |
| **Go** | Chi | 5.2.x (Go 1.23) | pgx (no ORM), Viper, JWT, bcrypt, Redis |
| **Rust** | Axum | 0.8.x (Rust 1.85) | Tokio, serde, sqlx, Edge computing |
| **C#** | ASP.NET Core | 8.x (.NET 8.0) | Entity Framework Core, AutoMapper |

## Installation

```groovy
implementation 'com.github.jnzader.apigen:apigen-codegen'
```

## Usage

The CodeGen module is primarily used via the APiGen Server REST API.

### Generate via API

```bash
curl -X POST http://localhost:8080/api/generate \
  -H "Content-Type: application/json" \
  -d '{
    "language": "JAVA",
    "framework": "SPRING_BOOT",
    "projectName": "my-api",
    "packageName": "com.example.myapi",
    "sql": "CREATE TABLE users (id BIGSERIAL PRIMARY KEY, name VARCHAR(255) NOT NULL, email VARCHAR(255) UNIQUE);",
    "features": ["SWAGGER", "DOCKER", "TESTS", "JWT_AUTH"]
  }' \
  --output my-api.zip
```

### Available Languages and Frameworks

| Language Enum | Framework Enum | Description |
|---------------|----------------|-------------|
| `JAVA` | `SPRING_BOOT` | Java Spring Boot 4.x |
| `KOTLIN` | `SPRING_BOOT` | Kotlin Spring Boot 4.x |
| `PYTHON` | `FASTAPI` | Python FastAPI |
| `TYPESCRIPT` | `NESTJS` | TypeScript NestJS |
| `PHP` | `LARAVEL` | PHP Laravel |
| `GO` | `GIN` | Go Gin with GORM |
| `GO` | `CHI` | Go Chi with pgx |
| `RUST` | `AXUM` | Rust Axum |
| `CSHARP` | `ASPNET_CORE` | C# ASP.NET Core |

## Available Features

| Feature | Description |
|---------|-------------|
| `CRUD` | Basic CRUD operations |
| `PAGINATION_OFFSET` | Offset-based pagination |
| `PAGINATION_CURSOR` | Cursor-based pagination |
| `FILTERING` | Dynamic query filters |
| `SOFT_DELETE` | Recoverable deletion |
| `AUDITING` | Automatic timestamps |
| `SWAGGER` | OpenAPI documentation |
| `VALIDATION` | Input validation |
| `DTOS` | Data Transfer Objects |
| `DOCKER` | Dockerfile + docker-compose |
| `TESTS` | Unit + integration tests |
| `HATEOAS` | Hypermedia links |
| `CACHING` | Response caching |
| `JWT_AUTH` | JWT authentication |
| `OAUTH2` | OAuth2 resource server |
| `RATE_LIMITING` | Request throttling |
| `FLYWAY` | Database migrations |

## Rust Edge Computing Presets

The Rust/Axum generator includes specialized presets for edge computing scenarios:

| Preset | Use Case | Technologies |
|--------|----------|--------------|
| `cloud` | Standard cloud deployment | PostgreSQL, Redis, JWT, OpenTelemetry |
| `edge-gateway` | IoT gateway devices | MQTT, Modbus, Serial communication |
| `edge-anomaly` | Local anomaly detection | SQLite, MQTT, ndarray |
| `edge-ai` | AI inference at edge | ONNX Runtime, tokenizers |

### Example with Rust Edge Preset

```bash
curl -X POST http://localhost:8080/api/generate \
  -H "Content-Type: application/json" \
  -d '{
    "language": "RUST",
    "framework": "AXUM",
    "projectName": "iot-gateway",
    "sql": "CREATE TABLE sensors (id SERIAL PRIMARY KEY, name TEXT NOT NULL);",
    "rustAxumOptions": {
      "preset": "edge-gateway",
      "enableMqtt": true,
      "enableModbus": true
    }
  }' \
  --output iot-gateway.zip
```

## Generated Project Structure

### Java/Spring Boot Example

```
my-api/
├── src/main/java/com/example/myapi/
│   ├── MyApiApplication.java
│   ├── domain/
│   │   ├── entity/User.java
│   │   └── repository/UserRepository.java
│   ├── application/
│   │   ├── dto/UserDTO.java
│   │   ├── mapper/UserMapper.java
│   │   └── service/UserService.java
│   ├── infrastructure/
│   │   └── controller/UserController.java
│   └── config/
│       └── SecurityConfig.java
├── src/main/resources/
│   ├── application.yml
│   └── db/migration/V1__create_users.sql
├── src/test/java/
│   └── ...
├── build.gradle
├── Dockerfile
└── docker-compose.yml
```

### Python/FastAPI Example

```
my-api/
├── app/
│   ├── __init__.py
│   ├── main.py
│   ├── models/
│   │   └── user.py
│   ├── schemas/
│   │   └── user.py
│   ├── repositories/
│   │   └── user.py
│   ├── services/
│   │   └── user.py
│   ├── routers/
│   │   └── user.py
│   └── config.py
├── tests/
│   └── ...
├── pyproject.toml
├── Dockerfile
└── docker-compose.yml
```

## API Endpoints

### Generate Project

```
POST /api/generate
Content-Type: application/json

Request Body:
{
  "language": "JAVA",
  "framework": "SPRING_BOOT",
  "projectName": "my-api",
  "packageName": "com.example.myapi",
  "sql": "CREATE TABLE ...",
  "features": ["SWAGGER", "DOCKER", "TESTS"],
  "database": "POSTGRESQL",
  "javaVersion": "25"
}

Response: application/zip (generated project)
```

### Validate SQL

```
POST /api/validate
Content-Type: application/json

Request Body:
{
  "sql": "CREATE TABLE users (...)"
}

Response:
{
  "valid": true,
  "tables": ["users"],
  "errors": []
}
```

### List Available Generators

```
GET /api/generators

Response:
{
  "generators": [
    {
      "language": "JAVA",
      "framework": "SPRING_BOOT",
      "features": ["CRUD", "PAGINATION", ...]
    },
    ...
  ]
}
```

## SQL Input Format

The generator accepts standard SQL DDL:

```sql
-- Simple table
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Table with relationships
CREATE TABLE posts (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    content TEXT,
    user_id BIGINT REFERENCES users(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Many-to-many
CREATE TABLE user_roles (
    user_id BIGINT REFERENCES users(id),
    role_id BIGINT REFERENCES roles(id),
    PRIMARY KEY (user_id, role_id)
);
```

## OpenAPI Import

You can also generate projects from existing OpenAPI specifications:

```bash
curl -X POST http://localhost:8080/api/generate \
  -H "Content-Type: application/json" \
  -d '{
    "language": "JAVA",
    "framework": "SPRING_BOOT",
    "projectName": "my-api",
    "openApiSpec": "openapi: 3.0.0\ninfo:\n  title: My API\n  version: 1.0.0\npaths: ..."
  }' \
  --output my-api.zip
```

## Feature Parity

All generators aim for feature parity across languages. See [GENERATOR-FEATURES-STATUS.md](../GENERATOR-FEATURES-STATUS.md) for detailed status per language.
