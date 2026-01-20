# APiGen

> Spring Boot REST API Library - Production-ready CRUD in minutes

## What is APiGen?

APiGen is a comprehensive library for building production-ready REST APIs with Spring Boot. It provides:

- **Complete CRUD operations** with a single annotation
- **HATEOAS support** with automatic link generation
- **Advanced filtering** using RSQL/FIQL syntax
- **Pagination** with customizable page sizes
- **Soft delete** for data recovery
- **Auditing** with automatic timestamps
- **ETag caching** for performance
- **Domain events** for reactive patterns

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
    implementation platform('com.github.jnzader.apigen:apigen-bom:1.0.0')
    implementation 'com.github.jnzader.apigen:apigen-core'
    implementation 'com.github.jnzader.apigen:apigen-security' // Optional
}
```

### 3. Create Your Entity

```java
@Entity
@ApiGenCrud
public class Product extends BaseEntity {
    private String name;
    private BigDecimal price;
    private String description;

    // Getters and setters
}
```

That's it! APiGen automatically generates:
- REST endpoints (`GET`, `POST`, `PUT`, `PATCH`, `DELETE`)
- DTOs and mappers
- Repository with filtering support
- Service layer with validation

## Features

| Feature | Description |
|---------|-------------|
| CRUD Operations | Full REST API with single annotation |
| HATEOAS | Automatic hypermedia links |
| Filtering | RSQL/FIQL query syntax |
| Pagination | Configurable page sizes |
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
| `apigen-codegen` | Code generation utilities |
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
