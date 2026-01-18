# APiGen Example Application

This module demonstrates how to use the APiGen library to build a REST API.

## Quick Start

### 1. Run with Docker Compose (PostgreSQL)

```bash
# From the project root
docker-compose up -d postgres

# Run the application
cd apigen-example
../gradlew bootRun
```

### 2. Run with H2 (Development)

```bash
cd apigen-example
../gradlew bootRun --args='--spring.profiles.active=dev'
```

### 3. Access the API

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **API Docs**: http://localhost:8080/v3/api-docs
- **Health Check**: http://localhost:8080/actuator/health

## Example Entity Structure

This example includes a `Product` entity that demonstrates all APiGen features:

```
src/main/java/com/jnzader/example/
├── domain/
│   ├── entity/
│   │   └── Product.java           # JPA Entity extending Base
│   └── repository/
│       └── ProductRepository.java  # Repository extending BaseRepository
├── application/
│   ├── dto/
│   │   └── ProductDTO.java        # DTO record implementing BaseDTO
│   ├── mapper/
│   │   └── ProductMapper.java     # MapStruct mapper extending BaseMapper
│   └── service/
│       └── ProductService.java    # Service extending BaseServiceImpl
└── infrastructure/
    ├── controller/
    │   └── ProductController.java  # Controller extending BaseControllerImpl
    └── hateoas/
        └── ProductResourceAssembler.java  # HATEOAS links
```

## API Endpoints

The `ProductController` inherits all CRUD operations from `BaseControllerImpl`:

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/products` | List with pagination & filtering |
| GET | `/api/products/{id}` | Get by ID with ETag |
| HEAD | `/api/products` | Get count |
| HEAD | `/api/products/{id}` | Check existence |
| POST | `/api/products` | Create new product |
| PUT | `/api/products/{id}` | Full update |
| PATCH | `/api/products/{id}` | Partial update |
| DELETE | `/api/products/{id}` | Soft delete |
| DELETE | `/api/products/{id}?permanent=true` | Hard delete |
| POST | `/api/products/{id}/restore` | Restore deleted |
| GET | `/api/products/cursor` | Cursor pagination |

### Custom Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/products/category/{category}` | Find by category |
| GET | `/api/products/search?name=...` | Search by name |

## Filtering Examples

```bash
# Filter by name (contains)
GET /api/products?filter=name:like:laptop

# Filter by price range
GET /api/products?filter=price:gte:100,price:lte:500

# Filter by category
GET /api/products?filter=category:eq:Electronics

# Multiple filters
GET /api/products?filter=category:eq:Electronics,stock:gt:0

# Sparse fieldsets (only return specific fields)
GET /api/products?fields=id,name,price

# Pagination
GET /api/products?page=0&size=10&sort=name,asc
```

### Supported Filter Operators

| Operator | Description | Example |
|----------|-------------|---------|
| `eq` | Equals | `category:eq:Electronics` |
| `neq` | Not equals | `status:neq:deleted` |
| `like` | Contains | `name:like:phone` |
| `starts` | Starts with | `name:starts:Mac` |
| `ends` | Ends with | `name:ends:Pro` |
| `gt` | Greater than | `price:gt:100` |
| `gte` | Greater or equal | `stock:gte:10` |
| `lt` | Less than | `price:lt:1000` |
| `lte` | Less or equal | `stock:lte:50` |
| `in` | In list | `category:in:Electronics;Accessories` |
| `between` | Between values | `price:between:100;500` |
| `null` | Is null | `description:null` |
| `notnull` | Is not null | `sku:notnull` |

## ETag Caching

```bash
# First request returns ETag
GET /api/products/1
# Response: ETag: "abc123"

# Subsequent request with If-None-Match
GET /api/products/1
If-None-Match: "abc123"
# Response: 304 Not Modified (if unchanged)
```

## Optimistic Locking

```bash
# Update with If-Match (prevents concurrent modification)
PUT /api/products/1
If-Match: "abc123"
Content-Type: application/json

{"name": "Updated Product", ...}
# Response: 412 Precondition Failed if ETag doesn't match
```

## Creating Your Own Entity

1. **Entity** - Extend `Base`:
```java
@Entity
@Table(name = "orders")
public class Order extends Base {
    private String customerId;
    private BigDecimal total;
    // ...
}
```

2. **DTO** - Implement `BaseDTO`:
```java
public record OrderDTO(
    Long id,
    Boolean activo,
    String customerId,
    BigDecimal total
) implements BaseDTO { }
```

3. **Mapper** - Extend `BaseMapper`:
```java
@Mapper(componentModel = "spring")
public interface OrderMapper extends BaseMapper<Order, OrderDTO> { }
```

4. **Repository** - Extend `BaseRepository`:
```java
@Repository
public interface OrderRepository extends BaseRepository<Order, Long> { }
```

5. **Service** - Extend `BaseServiceImpl`:
```java
@Service
public class OrderService extends BaseServiceImpl<Order, Long> {
    public OrderService(OrderRepository repo, CacheEvictionService cache,
                        ApplicationEventPublisher events, AuditorAware<String> auditor) {
        super(repo, cache, events, auditor);
    }

    @Override
    protected Class<Order> getEntityClass() {
        return Order.class;
    }
}
```

6. **Controller** - Extend `BaseControllerImpl`:
```java
@RestController
@RequestMapping("/api/orders")
public class OrderController extends BaseControllerImpl<Order, OrderDTO, Long> {
    public OrderController(OrderService service, OrderMapper mapper,
                          OrderResourceAssembler assembler, FilterSpecificationBuilder filter) {
        super(service, mapper, assembler, filter);
    }

    @Override
    protected Class<Order> getEntityClass() {
        return Order.class;
    }
}
```

## Configuration

See `application.yaml` for all configuration options.

## License

MIT License
