# APiGen

> Multi-Language API Generator & Spring Boot REST API Library

## What is APiGen?

APiGen is both a **multi-language code generator** (9 languages) and a **Spring Boot library** for building production-ready REST APIs.

### Code Generation

Generate complete API projects from SQL schemas in:

| Language | Framework | Features |
|----------|-----------|----------|
| Java | Spring Boot 4.x | Full-featured, HATEOAS, JWT, OAuth2 |
| Kotlin | Spring Boot 4.x | Data classes, coroutines |
| Python | FastAPI | Async, Pydantic, automatic OpenAPI |
| TypeScript | NestJS | TypeORM, class-validator |
| PHP | Laravel | Eloquent, migrations |
| Go | Gin | GORM, validator |
| Go | Chi | pgx, Viper, Redis |
| Rust | Axum | Tokio, sqlx, Edge computing |
| C# | ASP.NET Core | EF Core, AutoMapper |

### Library Features

- **Complete CRUD operations** with base classes
- **HATEOAS support** with automatic link generation
- **Advanced filtering** using dynamic operators
- **Pagination** (offset and cursor-based)
- **Soft delete** for data recovery
- **Auditing** with automatic timestamps
- **ETag caching** for performance
- **Domain events** for reactive patterns
- **JWT/OAuth2 authentication**
- **Rate limiting** with Bucket4j
- **GraphQL & gRPC** support
- **Multi-tenancy**
- **Event sourcing**

## Quick Start

### 1. Add JitPack Repository

```groovy
repositories {
    mavenCentral()
    maven { url 'https://jitpack.io' }
}
```

### 2. Add Dependencies

```groovy
dependencies {
    implementation platform('com.github.jnzader.apigen:apigen-bom:v2.18.0')
    implementation 'com.github.jnzader.apigen:apigen-core'
    implementation 'com.github.jnzader.apigen:apigen-security' // Optional
}
```

### 3. Create Your Entity

```java
import com.jnzader.apigen.core.domain.entity.Base;

@Entity
@Table(name = "products")
public class Product extends Base {

    @Column(nullable = false)
    private String name;

    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    private String description;

    // Getters and setters
}
```

### 4. Extend Base Classes

```java
// Repository
@Repository
public interface ProductRepository extends BaseRepository<Product, Long> {}

// Service
@Service
public class ProductService extends BaseServiceImpl<Product, Long> {
    @Override
    protected Class<Product> getEntityClass() { return Product.class; }

    @Override
    public String getEntityName() { return "Product"; }
}

// Controller
@RestController
@RequestMapping("/api/products")
public class ProductController extends BaseControllerImpl<Product, ProductDTO, Long> {
    public ProductController(ProductService service, ProductMapper mapper,
                            ProductResourceAssembler assembler) {
        super(service, mapper, assembler);
    }
}
```

That's it! APiGen automatically provides:
- REST endpoints (`GET`, `POST`, `PUT`, `PATCH`, `DELETE`)
- HATEOAS links
- Filtering support
- Pagination
- Validation
- Error handling

## Features

| Feature | Description |
|---------|-------------|
| CRUD Operations | Full REST API with base classes |
| HATEOAS | Automatic hypermedia links |
| Filtering | Dynamic query operators (12+) |
| Pagination | Offset and cursor-based |
| Soft Delete | Recoverable data deletion |
| Auditing | Automatic created/updated timestamps |
| ETag Caching | HTTP caching support |
| Security | JWT, OAuth2, RBAC |
| GraphQL | Alternative API protocol |
| gRPC | High-performance RPC |
| Gateway | API Gateway with rate limiting |

## Modules

| Module | Description |
|--------|-------------|
| `apigen-core` | Core CRUD functionality |
| `apigen-security` | JWT, OAuth2, RBAC authentication |
| `apigen-codegen` | Multi-language code generation (9 languages) |
| `apigen-graphql` | GraphQL API support |
| `apigen-grpc` | gRPC protocol support |
| `apigen-gateway` | API Gateway with Spring Cloud |

## Requirements

- Java 25+
- Spring Boot 4.0+
- Gradle or Maven

## Links

- [GitHub Repository](https://github.com/jnzader/apigen)
- [JitPack](https://jitpack.io/#jnzader/apigen)
- [Changelog](https://github.com/jnzader/apigen/blob/main/CHANGELOG.md)
