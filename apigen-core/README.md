# APiGen Core

Core library providing base classes for building REST APIs with Spring Boot.

## Purpose

Provides generic CRUD operations, HATEOAS support, filtering, pagination, caching, and more - eliminating boilerplate code.

## Features

- `Base` - Abstract entity with soft delete, auditing, optimistic locking
- `BaseService` / `BaseServiceImpl` - Generic service layer with caching
- `BaseController` / `BaseControllerImpl` - REST endpoints with HATEOAS
- `BaseRepository` - JPA repository with soft delete filtering
- `BaseMapper` - MapStruct interface for entity-DTO mapping
- Dynamic filtering with 12+ operators
- Cursor-based pagination
- ETag support for conditional requests
- Domain events (Created, Updated, Deleted, Restored)

## Usage

**Gradle:**
```groovy
dependencies {
    implementation 'com.jnzader:apigen-core:1.0.0-SNAPSHOT'
}
```

**Maven:**
```xml
<dependency>
    <groupId>com.jnzader</groupId>
    <artifactId>apigen-core</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

## Quick Example

```java
// Entity
@Entity
public class Product extends Base {
    private String name;
    private BigDecimal price;
}

// Repository
public interface ProductRepository extends BaseRepository<Product, Long> {}

// Service
@Service
public class ProductService extends BaseServiceImpl<Product, Long> {
    @Override
    protected Class<Product> getEntityClass() { return Product.class; }

    @Override
    public String getEntityName() { return "Product"; }
}

// Controller - automatically gets 12+ endpoints
@RestController
@RequestMapping("/api/products")
public class ProductController extends BaseControllerImpl<Product, ProductDTO, Long> {
    public ProductController(ProductService service, ProductMapper mapper,
                            ProductResourceAssembler assembler) {
        super(service, mapper, assembler);
    }
}
```

See [main README](../README.md) for complete documentation.
