# Core Module

The `apigen-core` module provides the foundation for building REST APIs.

## Features

- **BaseEntity** - Common entity fields (id, timestamps, version)
- **CRUD Operations** - Automatic REST endpoints
- **HATEOAS** - Hypermedia links
- **Filtering** - RSQL/FIQL query syntax
- **Pagination** - Configurable page sizes
- **Soft Delete** - Recoverable data deletion
- **Auditing** - Automatic timestamps

## Installation

```groovy
implementation 'com.github.jnzader.apigen:apigen-core'
```

## BaseEntity

All entities should extend `BaseEntity`:

```java
@Entity
public class Product extends BaseEntity {
    private String name;
    private BigDecimal price;
}
```

Inherited fields:
- `id` - Auto-generated UUID
- `createdAt` - Creation timestamp
- `updatedAt` - Last update timestamp
- `version` - Optimistic locking

## @ApiGenCrud Annotation

```java
@Entity
@ApiGenCrud(
    path = "/products",          // Custom path
    enableSoftDelete = true,     // Enable soft delete
    enableAudit = true           // Enable auditing
)
public class Product extends BaseEntity {
    // ...
}
```

## Filtering

Use RSQL syntax for filtering:

```bash
# Equal
GET /api/products?filter=name==Laptop

# Greater than
GET /api/products?filter=price>500

# Multiple conditions
GET /api/products?filter=price>100;stock>0

# Like (contains)
GET /api/products?filter=name=like=*phone*
```

## Pagination

```bash
GET /api/products?page=0&size=20&sort=name,asc
```

Response includes:
- `_embedded.products` - List of items
- `_links` - Navigation links (self, first, last, prev, next)
- `page` - Pagination metadata
