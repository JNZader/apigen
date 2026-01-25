# Core Module

The `apigen-core` module provides the foundation for building REST APIs with Spring Boot.

## Features

- **Base Entity** - Common entity fields (id, timestamps, version, soft delete)
- **CRUD Operations** - Base classes for repositories, services, and controllers
- **HATEOAS** - Automatic hypermedia links
- **Filtering** - Dynamic query operators (12+)
- **Pagination** - Offset and cursor-based
- **Soft Delete** - Recoverable data deletion
- **Auditing** - Automatic timestamps
- **ETag Caching** - HTTP conditional requests

## Installation

```groovy
implementation 'com.github.jnzader.apigen:apigen-core'
```

## Base Entity

All entities should extend `Base`:

```java
import com.jnzader.apigen.core.domain.entity.Base;

@Entity
@Table(name = "products")
public class Product extends Base {

    @Column(nullable = false)
    private String name;

    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    // Getters and setters
}
```

### Inherited Fields

The `Base` class provides these fields automatically:

| Field | Type | Description |
|-------|------|-------------|
| `id` | Long | Auto-generated primary key |
| `createdAt` | LocalDateTime | Creation timestamp |
| `updatedAt` | LocalDateTime | Last update timestamp |
| `version` | Long | Optimistic locking |
| `estado` | Integer | Soft delete status (1=active, 0=deleted) |

## Base Repository

Extend `BaseRepository` for your entities:

```java
import com.jnzader.apigen.core.domain.repository.BaseRepository;

@Repository
public interface ProductRepository extends BaseRepository<Product, Long> {
    // Custom query methods
    List<Product> findByNameContainingIgnoreCase(String name);
    Optional<Product> findByEmail(String email);
}
```

`BaseRepository` extends `JpaRepository` and `JpaSpecificationExecutor`, providing:
- Standard CRUD operations
- Pagination support
- Specification-based queries
- Soft delete filtering

## Base Service

Extend `BaseServiceImpl` for business logic:

```java
import com.jnzader.apigen.core.application.service.impl.BaseServiceImpl;

@Service
public class ProductService extends BaseServiceImpl<Product, Long> {

    @Override
    protected Class<Product> getEntityClass() {
        return Product.class;
    }

    @Override
    public String getEntityName() {
        return "Product";
    }

    // Custom business methods
    public List<Product> findExpensiveProducts(BigDecimal minPrice) {
        return repository.findAll(
            (root, query, cb) -> cb.greaterThan(root.get("price"), minPrice)
        );
    }
}
```

### Service Methods

| Method | Description |
|--------|-------------|
| `findById(ID id)` | Find entity by ID |
| `findAll(Pageable pageable)` | Paginated list |
| `findAll(Specification spec, Pageable pageable)` | Filtered, paginated list |
| `save(T entity)` | Create or update |
| `delete(ID id)` | Soft delete |
| `hardDelete(ID id)` | Permanent delete |
| `restore(ID id)` | Restore soft-deleted |
| `count()` | Count active entities |
| `exists(ID id)` | Check if exists |

## Base Controller

Extend `BaseControllerImpl` for REST endpoints:

```java
import com.jnzader.apigen.core.infrastructure.controller.BaseControllerImpl;

@RestController
@RequestMapping("/api/products")
public class ProductController extends BaseControllerImpl<Product, ProductDTO, Long> {

    public ProductController(ProductService service,
                            ProductMapper mapper,
                            ProductResourceAssembler assembler) {
        super(service, mapper, assembler);
    }

    // Custom endpoints
    @GetMapping("/expensive")
    public ResponseEntity<List<ProductDTO>> getExpensive(
            @RequestParam BigDecimal minPrice) {
        return ResponseEntity.ok(
            service.findExpensiveProducts(minPrice)
                .stream()
                .map(mapper::toDto)
                .toList()
        );
    }
}
```

### Auto-Generated Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/` | Paginated list with filtering |
| GET | `/{id}` | Single resource with ETag |
| POST | `/` | Create with validation |
| PUT | `/{id}` | Full update |
| PATCH | `/{id}` | Partial update |
| DELETE | `/{id}` | Soft delete |
| DELETE | `/{id}?permanent=true` | Hard delete |
| POST | `/{id}/restore` | Restore soft-deleted |
| HEAD | `/` | Count |
| HEAD | `/{id}` | Exists check |
| GET | `/cursor` | Cursor-based pagination |

## Filtering

Use dynamic query operators in the `filter` parameter:

```bash
# Equals
GET /api/products?filter=name:eq:Laptop

# Greater than
GET /api/products?filter=price:gt:500

# Like (contains)
GET /api/products?filter=name:like:phone

# Multiple conditions
GET /api/products?filter=price:gte:100,stock:gt:0

# Between
GET /api/products?filter=price:between:100,500

# In list
GET /api/products?filter=category:in:electronics,computers
```

### Supported Operators

| Operator | Description | Example |
|----------|-------------|---------|
| `eq` | Equals | `name:eq:Laptop` |
| `neq` | Not equals | `status:neq:inactive` |
| `gt` | Greater than | `price:gt:100` |
| `gte` | Greater than or equal | `price:gte:100` |
| `lt` | Less than | `stock:lt:10` |
| `lte` | Less than or equal | `stock:lte:10` |
| `like` | Contains | `name:like:phone` |
| `starts` | Starts with | `name:starts:Pro` |
| `ends` | Ends with | `name:ends:Max` |
| `in` | In list | `category:in:a,b,c` |
| `between` | Between values | `price:between:100,500` |
| `null` | Is null | `description:null` |
| `notnull` | Is not null | `email:notnull` |

## Pagination

### Offset-Based (Default)

```bash
GET /api/products?page=0&size=20&sort=name,asc
```

Response includes:
- `_embedded.products` - List of items
- `_links` - Navigation links (self, first, last, prev, next)
- `page` - Pagination metadata

### Cursor-Based

More efficient for large datasets:

```bash
GET /api/products/cursor?size=20&sort=id&direction=DESC

# Next page
GET /api/products/cursor?cursor=eyJpZCI6MTAwfQ==&size=20
```

## Configuration

Configure in `application.yml`:

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
```

## HATEOAS

All responses include hypermedia links:

```json
{
  "id": 1,
  "name": "Laptop",
  "price": 999.99,
  "_links": {
    "self": {
      "href": "http://localhost:8080/api/products/1"
    },
    "products": {
      "href": "http://localhost:8080/api/products"
    }
  }
}
```

## ETag Support

Automatic ETag generation for caching:

```bash
# First request
GET /api/products/1
# Response: ETag: "abc123"

# Conditional request
GET /api/products/1
If-None-Match: "abc123"
# Response: 304 Not Modified (if unchanged)
```

## Soft Delete

Entities are soft-deleted by default (setting `estado = 0`):

```bash
# Soft delete (recoverable)
DELETE /api/products/1

# Hard delete (permanent)
DELETE /api/products/1?permanent=true

# Restore soft-deleted
POST /api/products/1/restore
```
