# Getting Started

## Prerequisites

- Java 25 or higher
- Spring Boot 4.0+
- Gradle 8.x or Maven 3.9+

## Installation

### Using JitPack (Recommended)

Add the JitPack repository to your build file:

**Gradle (Kotlin DSL):**
```kotlin
repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}
```

**Gradle (Groovy):**
```groovy
repositories {
    mavenCentral()
    maven { url 'https://jitpack.io' }
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
```

### Add Dependencies

**Gradle:**
```groovy
dependencies {
    // Using the BOM for version management
    implementation platform('com.github.jnzader.apigen:apigen-bom:v2.18.0')

    // Core module (required)
    implementation 'com.github.jnzader.apigen:apigen-core'

    // Optional modules
    implementation 'com.github.jnzader.apigen:apigen-security'
}
```

**Maven:**
```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.github.jnzader.apigen</groupId>
            <artifactId>apigen-bom</artifactId>
            <version>v2.18.0</version>
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

## Basic Usage

### 1. Create an Entity

Extend the `Base` class from apigen-core:

```java
import com.jnzader.apigen.core.domain.entity.Base;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Column;

@Entity
@Table(name = "products")
public class Product extends Base {

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    private Integer stock;

    // Getters and setters
}
```

The `Base` class provides:
- `id` - Auto-generated primary key (Long)
- `createdAt` - Creation timestamp
- `updatedAt` - Last update timestamp
- `version` - Optimistic locking
- `estado` - Soft delete status

### 2. Create a DTO

```java
import com.jnzader.apigen.core.application.dto.BaseDTO;

public record ProductDTO(
    Long id,
    @NotBlank String name,
    String description,
    @Positive BigDecimal price,
    Integer stock
) implements BaseDTO {}
```

### 3. Create Repository, Mapper, and Service

```java
// Repository
import com.jnzader.apigen.core.domain.repository.BaseRepository;

@Repository
public interface ProductRepository extends BaseRepository<Product, Long> {
    List<Product> findByNameContainingIgnoreCase(String name);
}

// Mapper
import com.jnzader.apigen.core.application.mapper.BaseMapper;

@Mapper(componentModel = "spring")
public interface ProductMapper extends BaseMapper<Product, ProductDTO> {}

// Service
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
}
```

### 4. Create Controller

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
}
```

### 5. Run Your Application

That's it! APiGen automatically provides these endpoints:

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/products` | List all products (with pagination) |
| GET | `/api/products/{id}` | Get a single product |
| POST | `/api/products` | Create a new product |
| PUT | `/api/products/{id}` | Update a product |
| PATCH | `/api/products/{id}` | Partial update |
| DELETE | `/api/products/{id}` | Soft delete a product |
| DELETE | `/api/products/{id}?permanent=true` | Hard delete |
| POST | `/api/products/{id}/restore` | Restore soft-deleted |
| GET | `/api/products/cursor` | Cursor-based pagination |

**Features included automatically:**
- HATEOAS links
- Dynamic filtering
- Pagination (offset and cursor)
- Validation
- Error handling (RFC 7807)
- ETag caching

### 6. Test Your API

```bash
# Create a product
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{"name": "Laptop", "price": 999.99}'

# List products with filtering
curl "http://localhost:8080/api/products?filter=price:gte:500"

# Get a single product
curl http://localhost:8080/api/products/1

# Cursor pagination
curl "http://localhost:8080/api/products/cursor?size=10&sort=id"
```

## Configuration

Configure APiGen in `application.yml`:

```yaml
app:
  api:
    version: v1
    base-path: /api
  rate-limit:
    enabled: true
    max-requests: 100
    window-seconds: 60
  pagination:
    default-size: 20
    max-size: 100

spring:
  threads:
    virtual:
      enabled: true
```

## Next Steps

- [Core Module](modules/core.md) - Learn about core features
- [Security Module](modules/security.md) - Add authentication
- [Features](FEATURES.md) - Complete feature list
- [Code Generation](USAGE_GUIDE_CODEGEN.md) - Generate projects from SQL
