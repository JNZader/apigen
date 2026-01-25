# APiGen - Multi-Language API Generator & Spring Boot Library

[![CI](https://github.com/jnzader/apigen/actions/workflows/ci.yml/badge.svg)](https://github.com/jnzader/apigen/actions/workflows/ci.yml)
[![Java 25](https://img.shields.io/badge/Java-25-blue.svg)](https://openjdk.org/projects/jdk/25/)
[![Spring Boot 4.0](https://img.shields.io/badge/Spring%20Boot-4.0-green.svg)](https://spring.io/projects/spring-boot)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

**APiGen** is a production-ready Spring Boot 4.0 multi-module library AND a multi-language code generator that supports **9 languages/frameworks**. Design your API visually or from SQL, generate complete projects instantly.

## Code Generation: 9 Languages Supported

| Language | Framework | Version | Key Features |
|----------|-----------|---------|--------------|
| **Java** | Spring Boot | 4.x (Java 25) | CRUD, HATEOAS, JWT, OAuth2, Rate Limiting, Batch Ops, Full tests |
| **Kotlin** | Spring Boot | 4.x (Kotlin 2.1) | Data classes, sealed classes, coroutines |
| **Python** | FastAPI | 0.128.0 (Python 3.12) | Async SQLAlchemy, Pydantic, OpenAPI |
| **TypeScript** | NestJS | 11.x (TS 5.9) | TypeORM, class-validator, OpenAPI decorators |
| **PHP** | Laravel | 12.0 (PHP 8.4) | Eloquent, migrations, API resources |
| **Go** | Gin | 1.10.x (Go 1.23) | GORM, go-playground/validator, Swagger |
| **Go** | Chi | 5.2.x (Go 1.23) | pgx (no ORM), Viper, JWT, bcrypt, Redis, NATS/MQTT |
| **Rust** | Axum | 0.8.x (Rust 1.85) | Tokio, serde, sqlx, Edge computing (MQTT, Modbus, ONNX) |
| **C#** | ASP.NET Core | 8.x (.NET 8.0) | Entity Framework Core, AutoMapper, record DTOs |

### Rust Edge Computing Presets

The Rust/Axum generator includes specialized presets for edge computing:

| Preset | Use Case | Technologies |
|--------|----------|--------------|
| `cloud` | Standard cloud deployment | PostgreSQL, Redis, JWT, OpenTelemetry |
| `edge-gateway` | IoT gateway devices | MQTT, Modbus, Serial communication |
| `edge-anomaly` | Local anomaly detection | SQLite, MQTT, ndarray |
| `edge-ai` | AI inference at edge | ONNX Runtime, tokenizers |

## Library Features

APiGen also works as a Spring Boot library with enterprise features:

- **Generic CRUD Operations** - Base classes for entities, services, controllers, and repositories
- **HATEOAS Support** - Full hypermedia-driven API responses out of the box
- **Soft Delete & Auditing** - Built-in soft delete with automatic audit fields
- **JWT Authentication** - Complete auth flow with access/refresh tokens and blacklisting
- **OAuth2 Resource Server** - Auth0, Keycloak, Azure AD integration
- **Rate Limiting** - Per-endpoint throttling with Bucket4j (in-memory + Redis)
- **ETag Caching** - HTTP conditional requests with automatic 304 responses
- **Dynamic Filtering** - Query filters with 12+ operators
- **Cursor Pagination** - Efficient pagination for large datasets
- **Virtual Threads** - Java 21+ virtual threads for high concurrency
- **GraphQL & gRPC** - Alternative API protocols
- **API Gateway** - Spring Cloud Gateway integration
- **Multi-Tenancy** - Header/subdomain tenant isolation
- **Event Sourcing** - Event store with snapshots
- **Webhooks** - HMAC-SHA256 signed async delivery
- **i18n** - Message service with locale support

## Project Structure

```
apigen/
├── apigen-core/        # Core library (entities, services, controllers)
├── apigen-security/    # JWT authentication module (optional)
├── apigen-codegen/     # Multi-language code generator (9 languages)
├── apigen-server/      # API Generator HTTP server
├── apigen-graphql/     # GraphQL support module
├── apigen-grpc/        # gRPC support module
├── apigen-gateway/     # API Gateway module
├── apigen-bom/         # Bill of Materials for version management
└── apigen-example/     # Demo application showcasing APiGen
```

## Quick Start

### Option 1: Use as Library

**Gradle (with JitPack):**
```groovy
repositories {
    mavenCentral()
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation platform('com.github.jnzader.apigen:apigen-bom:v2.12.0')
    implementation 'com.github.jnzader.apigen:apigen-core'
    implementation 'com.github.jnzader.apigen:apigen-security'  // Optional
}
```

**Maven:**
```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.github.jnzader.apigen</groupId>
            <artifactId>apigen-bom</artifactId>
            <version>v2.12.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <dependency>
        <groupId>com.github.jnzader.apigen</groupId>
        <artifactId>apigen-core</artifactId>
    </dependency>
</dependencies>
```

### Option 2: Generate a Project

Use the APiGen Server to generate complete projects:

```bash
# Clone and run the server
git clone https://github.com/jnzader/apigen.git
cd apigen
./gradlew :apigen-server:bootRun --args='--spring.profiles.active=dev'

# Visit: http://localhost:8080/swagger-ui.html
```

**Generate via API:**
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

### Create Your Entity (Library Usage)

```java
import com.jnzader.apigen.core.domain.entity.Base;

@Entity
@Table(name = "products")
public class Product extends Base {

    @Column(nullable = false)
    private String name;

    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    private Integer stock;

    // Getters and setters (or use Lombok @Getter @Setter)
}
```

### Create Repository, Mapper & Service

```java
@Repository
public interface ProductRepository extends BaseRepository<Product, Long> {
    List<Product> findByNameContainingIgnoreCase(String name);
}

@Mapper(componentModel = "spring")
public interface ProductMapper extends BaseMapper<Product, ProductDTO> {}

@Service
public class ProductService extends BaseServiceImpl<Product, Long> {
    @Override
    protected Class<Product> getEntityClass() { return Product.class; }

    @Override
    public String getEntityName() { return "Product"; }
}
```

### Create Controller

```java
@RestController
@RequestMapping("/api/products")
public class ProductController extends BaseControllerImpl<Product, ProductDTO, Long> {

    public ProductController(ProductService service, ProductMapper mapper,
                            ProductResourceAssembler assembler) {
        super(service, mapper, assembler);
    }
}
```

Your API now has **12+ endpoints automatically**:

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/products` | Paginated list with filtering |
| GET | `/api/products/{id}` | Single resource with ETag |
| POST | `/api/products` | Create with validation |
| PUT | `/api/products/{id}` | Full update with optimistic locking |
| PATCH | `/api/products/{id}` | Partial update |
| DELETE | `/api/products/{id}` | Soft delete |
| DELETE | `/api/products/{id}?permanent=true` | Hard delete |
| POST | `/api/products/{id}/restore` | Restore soft-deleted |
| HEAD | `/api/products` | Count |
| HEAD | `/api/products/{id}` | Exists check |
| GET | `/api/products/cursor` | Cursor-based pagination |

## Advanced Features

### Dynamic Filtering

```http
GET /api/products?filter=name:like:laptop,price:gte:100,price:lte:500
```

Supported operators: `eq`, `neq`, `gt`, `gte`, `lt`, `lte`, `like`, `starts`, `ends`, `in`, `between`, `null`, `notnull`

### Cursor Pagination

```http
GET /api/products/cursor?size=20&sort=id&direction=DESC
# Response includes nextCursor for next page

GET /api/products/cursor?cursor=eyJpZCI6MTAwLC4uLn0=&size=20
```

### ETag Caching

```http
GET /api/products/1
# Response: ETag: "abc123"

GET /api/products/1
If-None-Match: "abc123"
# Response: 304 Not Modified
```

## Configuration

```yaml
app:
  api:
    version: v1
    base-path: /api
  rate-limit:
    enabled: true
    max-requests: 100
    window-seconds: 60
  cache:
    entities:
      max-size: 1000
      expire-after-write: 10m
  pagination:
    default-size: 20
    max-size: 100

apigen:
  security:
    enabled: true
    mode: jwt
    jwt:
      secret: ${JWT_SECRET}
      expiration-minutes: 15
      refresh-expiration-minutes: 10080

spring:
  threads:
    virtual:
      enabled: true
```

## Requirements

- Java 25+ (or Java 21+)
- Spring Boot 4.0+
- PostgreSQL 17 (recommended) or H2 for development

## Documentation

- **[Features](docs/FEATURES.md)** - Complete feature list
- **[Usage Guide](docs/USAGE_GUIDE.md)** - Detailed usage documentation
- **[Getting Started](docs/getting-started.md)** - Quick start guide
- **[Roadmap](docs/ROADMAP.md)** - Development roadmap

### Usage Guides by Method

| Method | Documentation |
|--------|---------------|
| As Library Dependency | [USAGE_GUIDE_LIBRARY.md](docs/USAGE_GUIDE_LIBRARY.md) |
| Clone Example | [USAGE_GUIDE_EXAMPLE.md](docs/USAGE_GUIDE_EXAMPLE.md) |
| GitHub Template | [USAGE_GUIDE_TEMPLATE.md](docs/USAGE_GUIDE_TEMPLATE.md) |
| Code Generation | [USAGE_GUIDE_CODEGEN.md](docs/USAGE_GUIDE_CODEGEN.md) |

## Contributing

Contributions are welcome! Please read our [Contributing Guide](CONTRIBUTING.md) before submitting PRs.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

Built with Java 25, Spring Boot 4.0, and Virtual Threads.
