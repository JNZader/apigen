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
    implementation platform('com.github.jnzader.apigen:apigen-bom:1.0.0')

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
            <version>1.0.0</version>
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

```java
import com.jnzader.apigen.core.annotation.ApiGenCrud;
import com.jnzader.apigen.core.entity.BaseEntity;
import jakarta.persistence.Entity;

@Entity
@ApiGenCrud
public class Product extends BaseEntity {

    private String name;
    private String description;
    private BigDecimal price;
    private Integer stock;

    // Getters and setters
}
```

### 2. Run Your Application

That's it! APiGen automatically generates:

- **REST Endpoints:**
  - `GET /api/products` - List all products (with pagination)
  - `GET /api/products/{id}` - Get a single product
  - `POST /api/products` - Create a new product
  - `PUT /api/products/{id}` - Update a product
  - `PATCH /api/products/{id}` - Partial update
  - `DELETE /api/products/{id}` - Delete a product

- **Features included:**
  - HATEOAS links
  - Filtering with RSQL
  - Pagination
  - Validation
  - Error handling

### 3. Test Your API

```bash
# Create a product
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{"name": "Laptop", "price": 999.99}'

# List products with filtering
curl "http://localhost:8080/api/products?filter=price>500"

# Get a single product
curl http://localhost:8080/api/products/1
```

## Next Steps

- [Core Module](modules/core.md) - Learn about core features
- [Security Module](modules/security.md) - Add authentication
- [JitPack Deployment](deployment/jitpack.md) - Publish your own library
