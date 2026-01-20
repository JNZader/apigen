---
sidebar_position: 1
slug: /
---

# Welcome to APiGen

**APiGen** is a production-ready Spring Boot 4.0 multi-module library that provides everything you need to build robust REST APIs. Just add the dependency and start building your domain - no boilerplate required.

## Why APiGen?

Building REST APIs with Spring Boot involves a lot of repetitive code:
- Entity classes with auditing and soft delete
- Repository interfaces
- Service layers with CRUD operations
- Controllers with HATEOAS support
- DTOs and mappers
- Filtering, pagination, caching...

**APiGen handles all of this for you**, so you can focus on your business logic.

## Key Features

| Feature | Description |
|---------|-------------|
| **Generic CRUD** | Base classes for entities, services, controllers, and repositories |
| **HATEOAS** | Full hypermedia-driven API responses out of the box |
| **Soft Delete** | Built-in soft delete with automatic audit fields |
| **JWT Auth** | Complete auth flow with access/refresh tokens |
| **Rate Limiting** | Per-endpoint throttling with Bucket4j |
| **Dynamic Filtering** | Query filters with 12+ operators |
| **Cursor Pagination** | Efficient pagination for large datasets |
| **Virtual Threads** | Java 21+ virtual threads for high concurrency |
| **Code Generation** | Generate complete entity structure from SQL |

## Quick Example

```java
// 1. Create your entity
@Entity
public class Product extends Base {
    private String name;
    private BigDecimal price;
}

// 2. Create your DTO
public record ProductDTO(
    Long id,
    @NotBlank String name,
    @Positive BigDecimal price
) implements BaseDTO {}

// 3. Create your repository
public interface ProductRepository extends BaseRepository<Product, Long> {}

// 4. Create your service
@Service
public class ProductService extends BaseServiceImpl<Product, Long> {
    // All CRUD operations ready!
}

// 5. Create your controller
@RestController
@RequestMapping("/api/products")
public class ProductController extends BaseControllerImpl<Product, ProductDTO, Long> {
    // 12+ endpoints automatically available!
}
```

That's it! You now have a fully functional REST API with:
- GET, POST, PUT, PATCH, DELETE endpoints
- Pagination and filtering
- HATEOAS links
- ETag caching
- Soft delete support
- Audit fields

## Architecture

APiGen follows a modular architecture:

```
apigen/
├── apigen-core/        # Core library (entities, services, controllers)
├── apigen-security/    # JWT authentication module (optional)
├── apigen-codegen/     # Code generator from SQL
├── apigen-graphql/     # GraphQL support
├── apigen-grpc/        # gRPC support
├── apigen-gateway/     # API Gateway module
├── apigen-bom/         # Bill of Materials
└── apigen-example/     # Working example application
```

## Next Steps

import DocCardList from '@theme/DocCardList';

<DocCardList />
