# APiGen - Guía de Uso: Como Librería

Esta guía explica cómo agregar APiGen como dependencia a un proyecto Spring Boot existente.

## Índice

1. [Requisitos Previos](#requisitos-previos)
2. [Instalación](#instalación)
3. [Configuración Básica](#configuración-básica)
4. [Crear tu Primera Entidad](#crear-tu-primera-entidad)
5. [Endpoints Disponibles](#endpoints-disponibles)
6. [Configuración Avanzada](#configuración-avanzada)
7. [Ejemplos Completos](#ejemplos-completos)

---

## Requisitos Previos

- Java 25+
- Spring Boot 4.0+
- Gradle 8.x o Maven 3.9+
- PostgreSQL 17+ (recomendado) o H2 para desarrollo

## Instalación

### Gradle (Kotlin DSL)

```kotlin
// build.gradle.kts
plugins {
    id("org.springframework.boot") version "4.0.0"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("jvm") version "2.0.0" // o java
}

dependencies {
    // BOM para gestión de versiones
    implementation(platform("com.jnzader:apigen-bom:1.0.0-SNAPSHOT"))

    // Módulo core (obligatorio)
    implementation("com.jnzader:apigen-core")

    // Módulo de seguridad (opcional)
    implementation("com.jnzader:apigen-security")

    // Base de datos
    runtimeOnly("org.postgresql:postgresql")

    // Lombok y MapStruct
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    annotationProcessor("org.mapstruct:mapstruct-processor:1.6.3")
    annotationProcessor("org.projectlombok:lombok-mapstruct-binding:0.2.0")
}

// Configuración de MapStruct
tasks.withType<JavaCompile> {
    options.compilerArgs.addAll(listOf(
        "-Amapstruct.defaultComponentModel=spring",
        "-Amapstruct.unmappedTargetPolicy=IGNORE"
    ))
}
```

### Gradle (Groovy)

```groovy
// build.gradle
plugins {
    id 'java'
    id 'org.springframework.boot' version '4.0.0'
    id 'io.spring.dependency-management' version '1.1.7'
}

dependencies {
    // BOM para gestión de versiones
    implementation platform('com.jnzader:apigen-bom:1.0.0-SNAPSHOT')

    // Módulo core (obligatorio)
    implementation 'com.jnzader:apigen-core'

    // Módulo de seguridad (opcional)
    implementation 'com.jnzader:apigen-security'

    // Base de datos
    runtimeOnly 'org.postgresql:postgresql'

    // Lombok y MapStruct
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
    annotationProcessor 'org.mapstruct:mapstruct-processor:1.6.3'
    annotationProcessor 'org.projectlombok:lombok-mapstruct-binding:0.2.0'
}

// Configuración de MapStruct
tasks.withType(JavaCompile).configureEach {
    options.compilerArgs += [
        '-Amapstruct.defaultComponentModel=spring',
        '-Amapstruct.unmappedTargetPolicy=IGNORE'
    ]
}
```

### Maven

```xml
<!-- pom.xml -->
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
    <!-- Módulo core (obligatorio) -->
    <dependency>
        <groupId>com.jnzader</groupId>
        <artifactId>apigen-core</artifactId>
    </dependency>

    <!-- Módulo de seguridad (opcional) -->
    <dependency>
        <groupId>com.jnzader</groupId>
        <artifactId>apigen-security</artifactId>
    </dependency>

    <!-- Base de datos -->
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <scope>runtime</scope>
    </dependency>
</dependencies>

<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <configuration>
                <annotationProcessorPaths>
                    <path>
                        <groupId>org.projectlombok</groupId>
                        <artifactId>lombok</artifactId>
                        <version>1.18.34</version>
                    </path>
                    <path>
                        <groupId>org.mapstruct</groupId>
                        <artifactId>mapstruct-processor</artifactId>
                        <version>1.6.3</version>
                    </path>
                    <path>
                        <groupId>org.projectlombok</groupId>
                        <artifactId>lombok-mapstruct-binding</artifactId>
                        <version>0.2.0</version>
                    </path>
                </annotationProcessorPaths>
                <compilerArgs>
                    <arg>-Amapstruct.defaultComponentModel=spring</arg>
                    <arg>-Amapstruct.unmappedTargetPolicy=IGNORE</arg>
                </compilerArgs>
            </configuration>
        </plugin>
    </plugins>
</build>
```

---

## Configuración Básica

### Application Class

```java
package com.example.myapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;

@SpringBootApplication
@EntityScan(basePackages = {
    "com.example.myapp",                    // Tus entidades
    "com.jnzader.apigen.core.domain.entity" // Entidades de APiGen
})
public class MyApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyApplication.class, args);
    }
}
```

### application.yaml

```yaml
spring:
  application:
    name: my-api

  # Virtual Threads (Java 21+)
  threads:
    virtual:
      enabled: true

  # Base de datos
  datasource:
    url: jdbc:postgresql://localhost:5432/mydb
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:postgres}

  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false

# Configuración de APiGen
apigen:
  core:
    enabled: true  # Habilitado por defecto

  security:
    enabled: true  # Habilitar autenticación JWT
    jwt:
      secret: ${JWT_SECRET:your-256-bit-secret-key-here-min-32-chars}
      expiration-minutes: 15
      refresh-expiration-minutes: 10080  # 7 días

# Configuración de la aplicación
app:
  api:
    version: v1
    base-path: /api

  cors:
    allowed-origins: http://localhost:3000,http://localhost:4200
    allowed-methods: GET,POST,PUT,PATCH,DELETE,OPTIONS

  cache:
    entities:
      max-size: 1000
      expire-after-write: 10m

  rate-limit:
    max-requests: 100
    window-seconds: 60

# Swagger/OpenAPI
springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /swagger-ui.html
```

---

## Crear tu Primera Entidad

### Paso 1: Crear la Entidad

```java
package com.example.myapp.domain.entity;

import com.jnzader.apigen.core.domain.entity.Base;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Product extends Base {

    @NotBlank(message = "El nombre es obligatorio")
    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 1000)
    private String description;

    @NotNull
    @Positive(message = "El precio debe ser positivo")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @PositiveOrZero(message = "El stock no puede ser negativo")
    @Column(nullable = false)
    private Integer stock = 0;

    @Column(length = 100)
    private String category;
}
```

**Lo que hereda de `Base`:**
- `id` (Long) - ID auto-generado con secuencia
- `estado` (Boolean) - Para soft delete (true = activo)
- `fechaCreacion` / `fechaActualizacion` - Auditoría automática
- `creadoPor` / `modificadoPor` - Usuario que creó/modificó
- `version` (Long) - Control de concurrencia optimista
- `fechaEliminacion` / `eliminadoPor` - Información de soft delete

### Paso 2: Crear el DTO

```java
package com.example.myapp.application.dto;

import com.jnzader.apigen.core.application.dto.BaseDTO;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record ProductDTO(
    Long id,
    Boolean activo,

    @NotBlank(message = "El nombre es obligatorio")
    String name,

    String description,

    @NotNull
    @Positive(message = "El precio debe ser positivo")
    BigDecimal price,

    @PositiveOrZero
    Integer stock,

    String category
) implements BaseDTO {

    // Factory method para crear nuevos productos
    public static ProductDTO create(String name, BigDecimal price, Integer stock) {
        return new ProductDTO(null, true, name, null, price, stock, null);
    }
}
```

### Paso 3: Crear el Mapper

```java
package com.example.myapp.application.mapper;

import com.jnzader.apigen.core.application.mapper.BaseMapper;
import com.example.myapp.application.dto.ProductDTO;
import com.example.myapp.domain.entity.Product;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface ProductMapper extends BaseMapper<Product, ProductDTO> {

    @Override
    @Mapping(source = "estado", target = "activo")
    ProductDTO toDTO(Product entity);

    @Override
    @Mapping(source = "activo", target = "estado")
    Product toEntity(ProductDTO dto);

    @Override
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(source = "activo", target = "estado")
    void updateEntityFromDTO(ProductDTO dto, @MappingTarget Product entity);

    // Records son inmutables, este método no aplica
    @Override
    default void updateDTOFromEntity(Product entity, @MappingTarget ProductDTO dto) {
        // No aplicable para records
    }
}
```

### Paso 4: Crear el Repository

```java
package com.example.myapp.domain.repository;

import com.jnzader.apigen.core.domain.repository.BaseRepository;
import com.example.myapp.domain.entity.Product;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends BaseRepository<Product, Long> {

    // Métodos de búsqueda personalizados
    List<Product> findByCategory(String category);

    Optional<Product> findByNameIgnoreCase(String name);

    List<Product> findByPriceBetween(java.math.BigDecimal min, java.math.BigDecimal max);

    long countByCategory(String category);
}
```

### Paso 5: Crear el Service

```java
package com.example.myapp.application.service;

import com.jnzader.apigen.core.application.service.BaseServiceImpl;
import com.jnzader.apigen.core.application.service.CacheEvictionService;
import com.example.myapp.domain.entity.Product;
import com.example.myapp.domain.repository.ProductRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ProductService extends BaseServiceImpl<Product, Long> {

    private final ProductRepository productRepository;

    public ProductService(
            ProductRepository repository,
            CacheEvictionService cacheEvictionService,
            ApplicationEventPublisher eventPublisher,
            AuditorAware<String> auditorAware
    ) {
        super(repository, cacheEvictionService, eventPublisher, auditorAware);
        this.productRepository = repository;
    }

    @Override
    protected Class<Product> getEntityClass() {
        return Product.class;
    }

    // Métodos de negocio personalizados
    public List<Product> findByCategory(String category) {
        return productRepository.findByCategory(category);
    }

    public List<Product> findByPriceRange(BigDecimal min, BigDecimal max) {
        return productRepository.findByPriceBetween(min, max);
    }

    public boolean isInStock(Long productId) {
        return findById(productId)
            .map(product -> product.getStock() > 0)
            .getOrElse(false);
    }
}
```

### Paso 6: Crear el Resource Assembler (HATEOAS)

```java
package com.example.myapp.infrastructure.hateoas;

import com.jnzader.apigen.core.infrastructure.hateoas.BaseResourceAssembler;
import com.example.myapp.application.dto.ProductDTO;
import com.example.myapp.infrastructure.controller.ProductController;
import org.springframework.stereotype.Component;

@Component
public class ProductResourceAssembler extends BaseResourceAssembler<ProductDTO, Long> {

    public ProductResourceAssembler() {
        super(ProductController.class);
    }
}
```

### Paso 7: Crear el Controller

```java
package com.example.myapp.infrastructure.controller;

import com.jnzader.apigen.core.domain.specification.FilterSpecificationBuilder;
import com.jnzader.apigen.core.infrastructure.controller.BaseControllerImpl;
import com.example.myapp.application.dto.ProductDTO;
import com.example.myapp.application.mapper.ProductMapper;
import com.example.myapp.application.service.ProductService;
import com.example.myapp.domain.entity.Product;
import com.example.myapp.infrastructure.hateoas.ProductResourceAssembler;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/products")
@Tag(name = "Products", description = "API de gestión de productos")
public class ProductController extends BaseControllerImpl<Product, ProductDTO, Long> {

    private final ProductService productService;
    private final ProductMapper productMapper;

    public ProductController(
            ProductService service,
            ProductMapper mapper,
            ProductResourceAssembler assembler,
            FilterSpecificationBuilder filterBuilder
    ) {
        super(service, mapper, assembler, filterBuilder);
        this.productService = service;
        this.productMapper = mapper;
    }

    @Override
    protected String getResourceName() {
        return "Product";
    }

    @Override
    protected Class<Product> getEntityClass() {
        return Product.class;
    }

    // Endpoints personalizados

    @GetMapping("/category/{category}")
    public ResponseEntity<List<ProductDTO>> findByCategory(@PathVariable String category) {
        List<ProductDTO> products = productService.findByCategory(category)
            .stream()
            .map(productMapper::toDTO)
            .toList();
        return ResponseEntity.ok(products);
    }

    @GetMapping("/price-range")
    public ResponseEntity<List<ProductDTO>> findByPriceRange(
            @RequestParam BigDecimal min,
            @RequestParam BigDecimal max) {
        List<ProductDTO> products = productService.findByPriceRange(min, max)
            .stream()
            .map(productMapper::toDTO)
            .toList();
        return ResponseEntity.ok(products);
    }
}
```

### Paso 8: Crear la Migración SQL

```sql
-- src/main/resources/db/migration/V1__create_products.sql

CREATE SEQUENCE IF NOT EXISTS base_sequence
    START WITH 1
    INCREMENT BY 50
    CACHE 50;

CREATE TABLE products (
    id BIGINT PRIMARY KEY DEFAULT nextval('base_sequence'),

    -- Campos de dominio
    name VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    price DECIMAL(10, 2) NOT NULL,
    stock INTEGER NOT NULL DEFAULT 0,
    category VARCHAR(100),

    -- Campos de Base (auditoría y soft delete)
    estado BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_eliminacion TIMESTAMP,
    eliminado_por VARCHAR(100),
    creado_por VARCHAR(100),
    modificado_por VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0
);

-- Índices
CREATE INDEX idx_products_name ON products(name);
CREATE INDEX idx_products_category ON products(category);
CREATE INDEX idx_products_estado ON products(estado) WHERE estado = true;
CREATE INDEX idx_products_fecha_creacion ON products(fecha_creacion DESC);
```

---

## Endpoints Disponibles

Con el controller anterior, automáticamente tienes:

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/api/products` | Listar con paginación y filtrado |
| GET | `/api/products/{id}` | Obtener por ID con ETag |
| HEAD | `/api/products` | Obtener conteo |
| HEAD | `/api/products/{id}` | Verificar existencia |
| POST | `/api/products` | Crear producto |
| PUT | `/api/products/{id}` | Actualización completa |
| PATCH | `/api/products/{id}` | Actualización parcial |
| DELETE | `/api/products/{id}` | Soft delete |
| DELETE | `/api/products/{id}?permanent=true` | Hard delete |
| POST | `/api/products/{id}/restore` | Restaurar eliminado |
| GET | `/api/products/cursor` | Paginación por cursor |

### Ejemplos de Uso

```bash
# Listar productos
curl http://localhost:8080/api/products

# Listar con paginación
curl "http://localhost:8080/api/products?page=0&size=10&sort=name,asc"

# Filtrar por categoría
curl "http://localhost:8080/api/products?filter=category:eq:Electronics"

# Filtrar por precio
curl "http://localhost:8080/api/products?filter=price:gte:100,price:lte:500"

# Buscar por nombre (contiene)
curl "http://localhost:8080/api/products?filter=name:like:laptop"

# Sparse fieldsets (solo ciertos campos)
curl "http://localhost:8080/api/products?fields=id,name,price"

# Obtener un producto (con ETag)
curl -i http://localhost:8080/api/products/1

# Crear producto
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{"name":"Laptop","price":999.99,"stock":10}'

# Actualizar producto
curl -X PUT http://localhost:8080/api/products/1 \
  -H "Content-Type: application/json" \
  -d '{"id":1,"activo":true,"name":"Laptop Pro","price":1099.99,"stock":8}'

# Actualización parcial
curl -X PATCH http://localhost:8080/api/products/1 \
  -H "Content-Type: application/json" \
  -d '{"price":899.99}'

# Eliminar (soft delete)
curl -X DELETE http://localhost:8080/api/products/1

# Restaurar
curl -X POST http://localhost:8080/api/products/1/restore

# Eliminar permanentemente
curl -X DELETE "http://localhost:8080/api/products/1?permanent=true"
```

---

## Configuración Avanzada

### Deshabilitar Seguridad

```yaml
apigen:
  security:
    enabled: false
```

### Personalizar Rate Limiting

```yaml
app:
  rate-limit:
    max-requests: 200
    window-seconds: 60

resilience4j:
  ratelimiter:
    instances:
      strict:
        limitForPeriod: 10
        limitRefreshPeriod: 1s
```

### Configurar CORS

```yaml
app:
  cors:
    allowed-origins: https://myapp.com,https://admin.myapp.com
    allowed-methods: GET,POST,PUT,PATCH,DELETE
    allow-credentials: true
    max-age: 3600
```

### Configurar Caché

```yaml
app:
  cache:
    entities:
      max-size: 5000
      expire-after-write: 30m
    lists:
      max-size: 500
      expire-after-write: 10m
```

---

## Ejemplos Completos

### Ejemplo con Relaciones

```java
// Order.java
@Entity
@Table(name = "orders")
public class Order extends Base {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> items = new ArrayList<>();

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    private OrderStatus status = OrderStatus.PENDING;
}

// OrderDTO.java
public record OrderDTO(
    Long id,
    Boolean activo,
    Long customerId,
    String customerName,  // Campo derivado
    List<OrderItemDTO> items,
    BigDecimal total,
    OrderStatus status
) implements BaseDTO { }

// OrderMapper.java
@Mapper(componentModel = "spring", uses = {OrderItemMapper.class})
public interface OrderMapper extends BaseMapper<Order, OrderDTO> {

    @Override
    @Mapping(source = "estado", target = "activo")
    @Mapping(source = "customer.id", target = "customerId")
    @Mapping(source = "customer.name", target = "customerName")
    OrderDTO toDTO(Order entity);

    @Override
    @Mapping(source = "activo", target = "estado")
    @Mapping(source = "customerId", target = "customer.id")
    @Mapping(target = "customer.name", ignore = true)
    Order toEntity(OrderDTO dto);
}
```

### Ejemplo con Validación Personalizada

```java
@Service
public class OrderService extends BaseServiceImpl<Order, Long> {

    @Override
    @Transactional
    public Result<Order, Exception> save(Order order) {
        // Validación de negocio antes de guardar
        if (order.getItems().isEmpty()) {
            return Result.failure(new IllegalArgumentException("Order must have at least one item"));
        }

        // Calcular total
        BigDecimal total = order.getItems().stream()
            .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setTotal(total);

        return super.save(order);
    }
}
```

---

## Siguiente Paso

- Ver [Guía de Uso: Módulo de Ejemplo](USAGE_GUIDE_EXAMPLE.md) para clonar y usar apigen-example
- Ver [Guía de Uso: Generación de Código](USAGE_GUIDE_CODEGEN.md) para generar entidades desde SQL
