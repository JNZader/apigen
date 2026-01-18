# APiGen - Spring Boot REST API Library

[![CI](https://github.com/jnzader/apigen/actions/workflows/ci.yml/badge.svg)](https://github.com/jnzader/apigen/actions/workflows/ci.yml)
[![Java 25](https://img.shields.io/badge/Java-25-blue.svg)](https://openjdk.org/projects/jdk/25/)
[![Spring Boot 4.0](https://img.shields.io/badge/Spring%20Boot-4.0-green.svg)](https://spring.io/projects/spring-boot)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

**APiGen** is a production-ready Spring Boot 4.0 multi-module library that provides everything you need to build robust REST APIs. Just add the dependency and start building your domain - no boilerplate required.

## Features

- **Generic CRUD Operations** - Base classes for entities, services, controllers, and repositories
- **HATEOAS Support** - Full hypermedia-driven API responses out of the box
- **Soft Delete & Auditing** - Built-in soft delete with automatic audit fields
- **JWT Authentication** - Complete auth flow with access/refresh tokens and blacklisting
- **Rate Limiting** - Per-endpoint throttling for authentication
- **ETag Caching** - HTTP conditional requests with automatic 304 responses
- **Dynamic Filtering** - Query filters with 12+ operators
- **Cursor Pagination** - Efficient pagination for large datasets
- **Virtual Threads** - Java 21+ virtual threads for high concurrency
- **Auto-Configuration** - Spring Boot Starter pattern for zero-config setup
- **Code Generation** - Generate complete entity structure from SQL

## Project Structure

```
apigen/
├── apigen-core/        # Core library (entities, services, controllers)
├── apigen-security/    # JWT authentication module (optional)
├── apigen-codegen/     # Code generator from SQL
├── apigen-bom/         # Bill of Materials for version management
└── apigen-example/     # Working example application
```

## Quick Start

### 1. Add Dependencies

**Gradle (with BOM - Recommended):**
```groovy
dependencies {
    implementation platform('com.jnzader:apigen-bom:1.0.0-SNAPSHOT')
    implementation 'com.jnzader:apigen-core'
    implementation 'com.jnzader:apigen-security'  // Optional
}
```

**Gradle (explicit versions):**
```groovy
dependencies {
    implementation 'com.jnzader:apigen-core:1.0.0-SNAPSHOT'
    implementation 'com.jnzader:apigen-security:1.0.0-SNAPSHOT'
}
```

**Maven:**
```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.jnzader</groupId>
            <artifactId>apigen-bom</artifactId>
            <version>1.0.0-SNAPSHOT</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <dependency>
        <groupId>com.jnzader</groupId>
        <artifactId>apigen-core</artifactId>
    </dependency>
</dependencies>
```

### 2. Create Your Entity

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

### 3. Create Your DTO

```java
import com.jnzader.apigen.core.application.dto.BaseDTO;

public record ProductDTO(
    Long id,
    @NotBlank String name,
    @Positive BigDecimal price,
    Integer stock
) implements BaseDTO {}
```

### 4. Create Repository, Mapper & Service

```java
import com.jnzader.apigen.core.domain.repository.BaseRepository;

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

### 5. Create Controller

```java
import com.jnzader.apigen.core.infrastructure.controller.BaseControllerImpl;

@RestController
@RequestMapping("/api/products")
public class ProductController extends BaseControllerImpl<Product, ProductDTO, Long> {

    public ProductController(ProductService service, ProductMapper mapper,
                            ProductResourceAssembler assembler) {
        super(service, mapper, assembler);
    }
}
```

### 6. Run!

That's it! Your API now has **12+ endpoints automatically**:

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

## Modules

| Module | Description |
|--------|-------------|
| `apigen-core` | Core functionality: Base entity, repository, service, controller, HATEOAS, filtering |
| `apigen-security` | JWT authentication with refresh tokens, rate limiting, security configuration |
| `apigen-codegen` | Code generation from SQL schemas |
| `apigen-bom` | Bill of Materials for dependency management |
| `apigen-example` | Complete working example application |

## Configuration

APiGen auto-configures with sensible defaults. Override in `application.yml`:

```yaml
apigen:
  core:
    enabled: true  # Enable core auto-configuration

  security:
    enabled: true  # Enable JWT security
    jwt:
      secret: ${JWT_SECRET}  # Required in production
      expiration-minutes: 15
      refresh-expiration-minutes: 10080  # 7 days

spring:
  threads:
    virtual:
      enabled: true  # Enable virtual threads

  data:
    web:
      pageable:
        default-page-size: 20
        max-page-size: 100
```

## Advanced Features

### Dynamic Filtering

```http
GET /api/products?filter=name:like:laptop,price:gte:100,price:lte:500
```

Supported operators: `eq`, `neq`, `gt`, `gte`, `lt`, `lte`, `like`, `starts`, `ends`, `in`, `between`, `null`, `notnull`

### Cursor Pagination (for large datasets)

```http
GET /api/products/cursor?size=20&sort=id&direction=DESC
# Response includes nextCursor for next page

GET /api/products/cursor?cursor=eyJpZCI6MTAwLC4uLn0=&size=20
```

### Sorting

```http
GET /api/products?sort=price,desc&sort=name,asc
```

### ETag Caching

```http
GET /api/products/1
# Response: ETag: "abc123"

GET /api/products/1
If-None-Match: "abc123"
# Response: 304 Not Modified
```

### Optimistic Locking

```http
PUT /api/products/1
If-Match: "abc123"
Content-Type: application/json

{"name": "Updated Product"}
# Returns 412 Precondition Failed if version doesn't match
```

## Code Generation

Generate complete entity structure from SQL:

```bash
java -jar apigen-codegen.jar schema.sql ./output com.mycompany
```

Generated files:
- Entity extending Base with JPA annotations
- DTO record with validation
- MapStruct mapper
- Repository interface
- Service implementation
- Controller with HATEOAS
- ResourceAssembler

## Running the Example

```bash
# Clone the repository
git clone https://github.com/jnzader/apigen.git
cd apigen

# Start PostgreSQL
docker-compose up -d postgres

# Run the example application
./gradlew :apigen-example:bootRun

# Or with dev profile (H2 in-memory, no Docker needed)
./gradlew :apigen-example:bootRun --args='--spring.profiles.active=dev'
```

Visit: http://localhost:8080/swagger-ui.html

## Requirements

- Java 25+ (or Java 21+)
- Spring Boot 4.0+
- PostgreSQL 17 (recommended) or H2 for development

## Documentation

- **[Usage Guide](docs/USAGE_GUIDE.md)** - Detailed usage documentation
- **[Didactic Docs](docs-didacticos/README.md)** - Step-by-step learning documentation (Spanish)
- **[Features](docs/FEATURES.md)** - Complete feature list

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

**Version:** 3.0.0 (Multi-module)
