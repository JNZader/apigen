# 09 - GENERADORES DE CODIGO

> **Documento Didactico APiGen v1.0**
> Code Generators & SDK Generation
> **Autor**: Juan Nolberto Zader Floreano

---

## Indice

1. [Introduccion](#introduccion)
2. [Generador de Entidades](#generador-de-entidades)
3. [Generacion de SDK con OpenAPI](#generacion-de-sdk-con-openapi)
4. [SpringDoc OpenAPI](#springdoc-openapi)
5. [Flujos de Trabajo](#flujos-de-trabajo)
6. [Scripts de Automatizacion](#scripts-de-automatizacion)
7. [Best Practices](#best-practices)

---

## Introduccion

APiGen incluye un sistema de **generadores de codigo automaticos** que permite crear rapidamente:

- **Entidades completas** con toda su estructura (Entity, DTO, Mapper, Service, Controller, Tests, Migrations)
- **SDK Clients** en multiples lenguajes (Java, TypeScript, Python) desde la especificacion OpenAPI
- **Documentacion API** automatica con SpringDoc

### Ventajas de los Generadores

```
┌──────────────────────────────────────────────────────────────┐
│                   GENERADORES DE CODIGO                       │
├──────────────────────────────────────────────────────────────┤
│                                                               │
│  ✓ Reduce tiempo de desarrollo en 80%                        │
│  ✓ Garantiza consistencia arquitectonica                     │
│  ✓ Elimina errores de copiar/pegar                           │
│  ✓ Genera codigo siguiendo best practices                    │
│  ✓ Incluye tests unitarios basicos                           │
│  ✓ Crea migraciones de BD automaticamente                    │
│  ✓ Produce SDKs type-safe para clientes                      │
│                                                               │
└──────────────────────────────────────────────────────────────┘
```

---

## Generador de Entidades

El generador de entidades crea una **estructura completa** siguiendo la arquitectura DDD del proyecto.

### Comando Basico

```bash
./gradlew generateEntity -Pname=Product -Pmodule=products
```

### Parametros

| Parametro | Requerido | Descripcion | Ejemplo |
|-----------|-----------|-------------|---------|
| `name` | Si | Nombre de la entidad (PascalCase) | `Product`, `Customer`, `Order` |
| `module` | Si | Nombre del modulo (lowercase) | `products`, `customers`, `orders` |
| `fields` | No | Lista de campos (nombre:tipo) | `name:string,price:decimal,stock:int` |

### Tipos de Datos Soportados

| Tipo | Java Type | SQL Type | Ejemplo |
|------|-----------|----------|---------|
| `string` | `String` | `VARCHAR(255)` | `name:string` |
| `int` / `integer` | `Integer` | `INTEGER` | `quantity:int` |
| `long` | `Long` | `BIGINT` | `userId:long` |
| `decimal` | `BigDecimal` | `DECIMAL(10, 2)` | `price:decimal` |
| `double` | `Double` | `DOUBLE PRECISION` | `weight:double` |
| `float` | `Float` | `REAL` | `rating:float` |
| `boolean` / `bool` | `Boolean` | `BOOLEAN` | `active:boolean` |
| `date` | `LocalDate` | `DATE` | `birthDate:date` |
| `datetime` | `LocalDateTime` | `TIMESTAMP` | `createdAt:datetime` |
| `instant` | `Instant` | `TIMESTAMP` | `lastLogin:instant` |

### Ejemplo Completo

```bash
./gradlew generateEntity \
  -Pname=Product \
  -Pmodule=products \
  -Pfields="name:string,code:string,price:decimal,stock:int,description:string,active:boolean"
```

### Estructura Generada

```
src/main/java/com/jnzader/apigen/products/
├── domain/
│   ├── entity/
│   │   └── Product.java                    # Entidad JPA con Hibernate Envers
│   ├── event/                               # (directorio creado)
│   └── exception/                           # (directorio creado)
├── application/
│   ├── dto/
│   │   └── ProductDTO.java                  # DTO validado con BaseDTOValidated
│   ├── mapper/
│   │   └── ProductMapper.java               # Mapper MapStruct
│   └── service/
│       ├── ProductService.java              # Interface de servicio
│       └── ProductServiceImpl.java          # Implementacion con BaseServiceImpl
└── infrastructure/
    ├── repository/
    │   └── ProductRepository.java           # Repository JPA
    └── controller/
        ├── ProductController.java           # Interface REST
        └── ProductControllerImpl.java       # Implementacion con BaseControllerImpl

src/test/java/com/jnzader/apigen/products/
└── application/service/
    └── ProductServiceImplTest.java          # Tests unitarios basicos

src/main/resources/db/migration/
└── V2__create_products_table.sql            # Migracion Flyway
```

### 1. Entidad Generada

**`Product.java`**:

```java
package com.jnzader.apigen.products.domain.entity;

import com.jnzader.apigen.core.domain.entity.Base;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.envers.Audited;
import java.math.BigDecimal;

@Entity
@Table(name = "products")
@Audited
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product extends Base {

    @Column(name = "name")
    private String name;

    @Column(name = "code")
    private String code;

    @Column(name = "price")
    private BigDecimal price;

    @Column(name = "stock")
    private Integer stock;

    @Column(name = "description")
    private String description;

    @Column(name = "active")
    private Boolean active;
}
```

**Caracteristicas**:
- Extiende `Base` (id, auditoria, soft delete)
- Anotada con `@Audited` para historial
- Lombok para getters/setters/builders
- Nombres de columnas en snake_case

### 2. DTO Generado

**`ProductDTO.java`**:

```java
package com.jnzader.apigen.products.application.dto;

import com.jnzader.apigen.core.application.dto.BaseDTO;
import com.jnzader.apigen.core.application.dto.validation.OnCreate;
import com.jnzader.apigen.core.application.dto.validation.OnUpdate;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class ProductDTO extends BaseDTO {

    private String name;
    private String code;
    private BigDecimal price;
    private Integer stock;
    private String description;
    private Boolean active;
}
```

**Puedes agregar validaciones manualmente**:

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class ProductDTO extends BaseDTO {

    @NotBlank(groups = {OnCreate.class, OnUpdate.class},
              message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    @NotBlank(groups = OnCreate.class, message = "Code is required")
    @Pattern(regexp = "^[A-Z0-9]{3,10}$", message = "Invalid code format")
    private String code;

    @NotNull(groups = OnCreate.class, message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    @DecimalMax(value = "999999.99", message = "Price cannot exceed 999999.99")
    private BigDecimal price;

    @Min(value = 0, message = "Stock cannot be negative")
    private Integer stock;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    private Boolean active;
}
```

### 3. Mapper Generado

**`ProductMapper.java`**:

```java
package com.jnzader.apigen.products.application.mapper;

import com.jnzader.apigen.core.application.mapper.BaseMapper;
import com.jnzader.apigen.products.application.dto.ProductDTO;
import com.jnzader.apigen.products.domain.entity.Product;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface ProductMapper extends BaseMapper<Product, ProductDTO> {

    @Override
    ProductDTO toDto(Product entity);

    @Override
    Product toEntity(ProductDTO dto);

    @Override
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDto(ProductDTO dto, @MappingTarget Product entity);
}
```

**MapStruct genera automaticamente la implementacion en `build/generated/sources/annotationProcessor`**.

### 4. Repository Generado

**`ProductRepository.java`**:

```java
package com.jnzader.apigen.products.infrastructure.repository;

import com.jnzader.apigen.core.infrastructure.repository.BaseRepository;
import com.jnzader.apigen.products.domain.entity.Product;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends BaseRepository<Product, Long> {

    // Add custom query methods here
    // Example:
    // Optional<Product> findByCode(String code);
    // List<Product> findByCategoryId(Long categoryId);
}
```

**Puedes agregar queries custom**:

```java
@Repository
public interface ProductRepository extends BaseRepository<Product, Long> {

    Optional<Product> findByCode(String code);

    List<Product> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice);

    @Query("SELECT p FROM Product p WHERE p.stock < :threshold AND p.estado = true")
    List<Product> findLowStockProducts(@Param("threshold") Integer threshold);

    @Query(value = "SELECT * FROM products WHERE category_id = :categoryId",
           nativeQuery = true)
    List<Product> findByCategoryNative(@Param("categoryId") Long categoryId);
}
```

### 5. Service Generado

**`ProductService.java`** (Interface):

```java
package com.jnzader.apigen.products.application.service;

import com.jnzader.apigen.core.application.service.BaseService;
import com.jnzader.apigen.products.domain.entity.Product;

public interface ProductService extends BaseService<Product, Long> {

    // Add custom business methods here
}
```

**`ProductServiceImpl.java`** (Implementacion):

```java
package com.jnzader.apigen.products.application.service;

import com.jnzader.apigen.core.application.service.BaseServiceImpl;
import com.jnzader.apigen.products.domain.entity.Product;
import com.jnzader.apigen.products.infrastructure.repository.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@Transactional
public class ProductServiceImpl
        extends BaseServiceImpl<Product, Long>
        implements ProductService {

    private final ProductRepository productRepository;

    public ProductServiceImpl(ProductRepository repository) {
        super(repository);
        this.productRepository = repository;
    }

    @Override
    protected String getEntityName() {
        return "Product";
    }
}
```

**Extiende facilmente con metodos custom**:

```java
@Service
@Slf4j
@Transactional
public class ProductServiceImpl
        extends BaseServiceImpl<Product, Long>
        implements ProductService {

    private final ProductRepository productRepository;

    public ProductServiceImpl(ProductRepository repository) {
        super(repository);
        this.productRepository = repository;
    }

    @Override
    protected String getEntityName() {
        return "Product";
    }

    @Transactional(readOnly = true)
    public Result<Product, Exception> findByCode(String code) {
        log.debug("Finding product by code: {}", code);
        return Result.fromOptional(
            productRepository.findByCode(code),
            () -> new ResourceNotFoundException("Product not found: " + code)
        );
    }

    @Transactional
    public Result<Product, Exception> updateStock(Long id, Integer newStock) {
        return findById(id).flatMap(product -> {
            product.setStock(newStock);
            return save(product);
        });
    }

    @Transactional(readOnly = true)
    public Result<List<Product>, Exception> findLowStock(Integer threshold) {
        return Result.of(() -> productRepository.findLowStockProducts(threshold));
    }
}
```

### 6. Controller Generado

**`ProductController.java`** (Interface):

```java
package com.jnzader.apigen.products.infrastructure.controller;

import com.jnzader.apigen.core.infrastructure.controller.BaseController;
import com.jnzader.apigen.products.application.dto.ProductDTO;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Products", description = "Product management API")
@RequestMapping("/api/v1/products")
public interface ProductController extends BaseController<ProductDTO, Long> {

    // Add custom endpoints here
}
```

**`ProductControllerImpl.java`** (Implementacion):

```java
package com.jnzader.apigen.products.infrastructure.controller;

import com.jnzader.apigen.core.infrastructure.controller.BaseControllerImpl;
import com.jnzader.apigen.products.application.dto.ProductDTO;
import com.jnzader.apigen.products.application.mapper.ProductMapper;
import com.jnzader.apigen.products.application.service.ProductService;
import com.jnzader.apigen.products.domain.entity.Product;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class ProductControllerImpl
        extends BaseControllerImpl<Product, ProductDTO, Long>
        implements ProductController {

    private final ProductService productService;
    private final ProductMapper productMapper;

    public ProductControllerImpl(ProductService service, ProductMapper mapper) {
        super(service, mapper);
        this.productService = service;
        this.productMapper = mapper;
    }
}
```

**Endpoints disponibles automaticamente**:

```
GET    /api/v1/products              - Lista con paginacion
HEAD   /api/v1/products              - Conteo total
GET    /api/v1/products/{id}         - Obtener por ID
HEAD   /api/v1/products/{id}         - Verificar existencia
POST   /api/v1/products              - Crear
PUT    /api/v1/products/{id}         - Actualizar completo
PATCH  /api/v1/products/{id}         - Actualizar parcial
DELETE /api/v1/products/{id}         - Soft delete
DELETE /api/v1/products/{id}?permanent=true - Hard delete
POST   /api/v1/products/{id}/restore - Restaurar
```

### 7. Migracion Generada

**`V2__create_products_table.sql`**:

```sql
-- V2__create_products_table.sql
-- Auto-generated migration for: Product

CREATE TABLE products (
    id BIGINT PRIMARY KEY,
    name VARCHAR(255),
    code VARCHAR(255),
    price DECIMAL(10, 2),
    stock INTEGER,
    description VARCHAR(255),
    active BOOLEAN,
    -- Base entity fields
    estado BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP,
    fecha_eliminacion TIMESTAMP,
    creado_por VARCHAR(100),
    modificado_por VARCHAR(100),
    eliminado_por VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0
);

-- Audit table (Hibernate Envers)
CREATE TABLE products_aud (
    id BIGINT NOT NULL,
    rev INTEGER NOT NULL REFERENCES revision_info(id),
    revtype SMALLINT,
    name VARCHAR(255),
    code VARCHAR(255),
    price DECIMAL(10, 2),
    stock INTEGER,
    description VARCHAR(255),
    active BOOLEAN,
    estado BOOLEAN,
    fecha_eliminacion TIMESTAMP,
    modificado_por VARCHAR(100),
    eliminado_por VARCHAR(100),
    PRIMARY KEY (id, rev)
);

-- Indexes
CREATE INDEX idx_products_estado ON products(estado);
CREATE INDEX idx_products_fecha_creacion ON products(fecha_creacion DESC);

COMMENT ON TABLE products IS 'Products table';
```

**Caracteristicas**:
- Numeracion automatica (V2, V3, V4...)
- Incluye campos de auditoria
- Tabla de auditoria para Envers
- Indices basicos
- Compatible con Flyway

### 8. Test Generado

**`ProductServiceImplTest.java`**:

```java
package com.jnzader.apigen.products.application.service;

import com.jnzader.apigen.products.domain.entity.Product;
import com.jnzader.apigen.products.infrastructure.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService Tests")
class ProductServiceImplTest {

    @Mock
    private ProductRepository repository;

    @InjectMocks
    private ProductServiceImpl service;

    private Product product;

    @BeforeEach
    void setUp() {
        product = new Product();
        product.setId(1L);
    }

    @Test
    @DisplayName("Should find product by ID")
    void shouldFindProductById() {
        when(repository.findById(1L)).thenReturn(Optional.of(product));

        var result = service.findById(1L);

        assertThat(result.isSuccess()).isTrue();
        verify(repository).findById(1L);
    }

    @Test
    @DisplayName("Should save product")
    void shouldSaveProduct() {
        when(repository.save(any(Product.class))).thenReturn(product);

        var result = service.save(product);

        assertThat(result.isSuccess()).isTrue();
        verify(repository).save(any(Product.class));
    }
}
```

### Pasos Posteriores a la Generacion

```bash
# 1. Compilar para generar MapStruct implementations
./gradlew compileJava

# 2. Ejecutar migraciones (o arrancar la app)
./gradlew flywayMigrate

# 3. Ejecutar tests
./gradlew test

# 4. Probar endpoints
curl http://localhost:8080/api/v1/products

# 5. Ver documentacion
open http://localhost:8080/swagger-ui.html
```

---

## Generacion de SDK con OpenAPI

APiGen genera **clientes SDK type-safe** en multiples lenguajes a partir de la especificacion OpenAPI.

### Arquitectura de Generacion

```
┌─────────────────────────────────────────────────────────────┐
│                      APiGen API                              │
│                  (Spring Boot + Controllers)                 │
└────────────────────────┬────────────────────────────────────┘
                         │
                         │ SpringDoc extrae metadatos
                         ▼
┌─────────────────────────────────────────────────────────────┐
│             src/main/resources/openapi/                      │
│                   api-spec.json                              │
│                                                               │
│  {                                                            │
│    "openapi": "3.0.1",                                        │
│    "info": { ... },                                           │
│    "paths": { ... },                                          │
│    "components": { ... }                                      │
│  }                                                            │
└────────────────────────┬────────────────────────────────────┘
                         │
                         │ OpenAPI Generator
                         ▼
┌─────────────────────────────────────────────────────────────┐
│              build/generated-sdks/                           │
│  ├── java-client/       (Java + Native HTTP Client)         │
│  ├── typescript-client/ (TypeScript + Axios)                │
│  └── python-client/     (Python + urllib3)                   │
└─────────────────────────────────────────────────────────────┘
```

### Configuracion en build.gradle

```gradle
// Variables de configuracion
def openApiSpecFile = "${projectDir}/src/main/resources/openapi/api-spec.json"
def sdkOutputDir = "${buildDir}/generated-sdks"

// Configuracion de SpringDoc para generar el spec
openApi {
    outputDir = file("${projectDir}/src/main/resources/openapi")
    outputFileName = 'api-spec.json'
    apiDocsUrl = "http://localhost:8080/v3/api-docs"
}

// Task: Genera cliente Java
tasks.register('generateJavaClient', GenerateTask) {
    group = 'sdk generation'
    description = 'Generates Java client SDK from OpenAPI spec'
    dependsOn 'generateOpenApiDocs'

    generatorName = 'java'
    inputSpec = openApiSpecFile
    outputDir = "${sdkOutputDir}/java-client"

    apiPackage = 'com.jnzader.apigen.client.api'
    modelPackage = 'com.jnzader.apigen.client.model'
    invokerPackage = 'com.jnzader.apigen.client'

    configOptions = [
        'library': 'native',
        'dateLibrary': 'java8',
        'useJakartaEe': 'true',
        'openApiNullable': 'false',
        'documentationProvider': 'none',
        'serializationLibrary': 'jackson',
        'developerName': 'APiGen',
        'developerEmail': 'dev@example.com',
        'artifactId': 'apigen-java-client',
        'groupId': 'com.jnzader',
        'artifactVersion': project.version.toString()
    ]
}

// Task: Genera cliente TypeScript
tasks.register('generateTypeScriptClient', GenerateTask) {
    group = 'sdk generation'
    description = 'Generates TypeScript/Axios client SDK from OpenAPI spec'
    dependsOn 'generateOpenApiDocs'

    generatorName = 'typescript-axios'
    inputSpec = openApiSpecFile
    outputDir = "${sdkOutputDir}/typescript-client"

    configOptions = [
        'npmName': '@apigen/client',
        'npmVersion': project.version.toString(),
        'supportsES6': 'true',
        'withSeparateModelsAndApi': 'true',
        'modelPropertyNaming': 'camelCase',
        'enumPropertyNaming': 'UPPERCASE'
    ]
}

// Task: Genera cliente Python
tasks.register('generatePythonClient', GenerateTask) {
    group = 'sdk generation'
    description = 'Generates Python client SDK from OpenAPI spec'
    dependsOn 'generateOpenApiDocs'

    generatorName = 'python'
    inputSpec = openApiSpecFile
    outputDir = "${sdkOutputDir}/python-client"

    configOptions = [
        'packageName': 'apigen_client',
        'projectName': 'apigen-python-client',
        'packageVersion': project.version.toString()
    ]
}

// Task: Genera todos los clientes
tasks.register('generateAllClients') {
    group = 'sdk generation'
    description = 'Generates all client SDKs (Java, TypeScript, Python)'

    dependsOn 'generateJavaClient', 'generateTypeScriptClient', 'generatePythonClient'

    doLast {
        println "\n=========================================="
        println "SDK Generation Complete!"
        println "=========================================="
        println "Generated SDKs available at:"
        println "  - Java:       ${sdkOutputDir}/java-client"
        println "  - TypeScript: ${sdkOutputDir}/typescript-client"
        println "  - Python:     ${sdkOutputDir}/python-client"
        println "=========================================="
    }
}
```

### Comandos de Generacion

```bash
# 1. Generar especificacion OpenAPI desde la aplicacion
./gradlew generateOpenApiDocs

# 2. Generar SDK de Java
./gradlew generateJavaClient

# 3. Generar SDK de TypeScript
./gradlew generateTypeScriptClient

# 4. Generar SDK de Python
./gradlew generatePythonClient

# 5. Generar TODOS los SDKs
./gradlew generateAllClients
```

### Ejemplo de Uso: Cliente Java

**Estructura generada**:

```
build/generated-sdks/java-client/
├── src/main/java/com/jnzader/apigen/client/
│   ├── api/
│   │   ├── ProductsApi.java
│   │   ├── AuthenticationApi.java
│   │   └── ...
│   ├── model/
│   │   ├── ProductDTO.java
│   │   ├── AuthResponseDTO.java
│   │   └── ...
│   └── ApiClient.java
├── pom.xml
└── README.md
```

**Codigo de uso**:

```java
import com.jnzader.apigen.client.ApiClient;
import com.jnzader.apigen.client.api.ProductsApi;
import com.jnzader.apigen.client.model.ProductDTO;

// Configurar cliente
ApiClient client = new ApiClient();
client.setBasePath("http://localhost:8080");
client.setBearerToken("your-jwt-token");

// Crear API
ProductsApi productsApi = new ProductsApi(client);

// Listar productos
List<ProductDTO> products = productsApi.findAll(0, 20, null);
System.out.println("Total products: " + products.size());

// Obtener producto por ID
ProductDTO product = productsApi.findById(1L);
System.out.println("Product: " + product.getName());

// Crear producto
ProductDTO newProduct = new ProductDTO()
    .name("New Product")
    .code("NP001")
    .price(new BigDecimal("99.99"))
    .stock(100);

ProductDTO created = productsApi.save(newProduct);
System.out.println("Created product with ID: " + created.getId());

// Actualizar producto
created.setPrice(new BigDecimal("89.99"));
ProductDTO updated = productsApi.update(created.getId(), created);

// Eliminar producto
productsApi.delete(created.getId(), false); // soft delete
```

### Ejemplo de Uso: Cliente TypeScript

**Estructura generada**:

```
build/generated-sdks/typescript-client/
├── src/
│   ├── api/
│   │   ├── products-api.ts
│   │   ├── authentication-api.ts
│   │   └── ...
│   ├── model/
│   │   ├── product-dto.ts
│   │   ├── auth-response-dto.ts
│   │   └── ...
│   └── index.ts
├── package.json
├── tsconfig.json
└── README.md
```

**Codigo de uso**:

```typescript
import { Configuration, ProductsApi, ProductDTO } from '@apigen/client';

// Configurar cliente
const config = new Configuration({
  basePath: 'http://localhost:8080',
  accessToken: 'your-jwt-token'
});

const productsApi = new ProductsApi(config);

// Listar productos
const products = await productsApi.findAll(0, 20);
console.log(`Total products: ${products.data.length}`);

// Obtener producto por ID
const product = await productsApi.findById(1);
console.log(`Product: ${product.data.name}`);

// Crear producto
const newProduct: ProductDTO = {
  name: 'New Product',
  code: 'NP001',
  price: 99.99,
  stock: 100
};

const created = await productsApi.save(newProduct);
console.log(`Created product with ID: ${created.data.id}`);

// Actualizar producto
const updated = await productsApi.update(created.data.id!, {
  ...created.data,
  price: 89.99
});

// Eliminar producto
await productsApi.delete(created.data.id!);
```

### Ejemplo de Uso: Cliente Python

**Estructura generada**:

```
build/generated-sdks/python-client/
├── apigen_client/
│   ├── api/
│   │   ├── products_api.py
│   │   ├── authentication_api.py
│   │   └── ...
│   ├── models/
│   │   ├── product_dto.py
│   │   ├── auth_response_dto.py
│   │   └── ...
│   └── __init__.py
├── setup.py
├── requirements.txt
└── README.md
```

**Codigo de uso**:

```python
from apigen_client import ApiClient, Configuration
from apigen_client.api import ProductsApi
from apigen_client.models import ProductDTO

# Configurar cliente
config = Configuration()
config.host = "http://localhost:8080"
config.access_token = "your-jwt-token"

client = ApiClient(configuration=config)
products_api = ProductsApi(client)

# Listar productos
products = products_api.find_all(page=0, size=20)
print(f"Total products: {len(products)}")

# Obtener producto por ID
product = products_api.find_by_id(1)
print(f"Product: {product.name}")

# Crear producto
new_product = ProductDTO(
    name="New Product",
    code="NP001",
    price=99.99,
    stock=100
)

created = products_api.save(new_product)
print(f"Created product with ID: {created.id}")

# Actualizar producto
created.price = 89.99
updated = products_api.update(created.id, created)

# Eliminar producto
products_api.delete(created.id)
```

### Templates Personalizados

Puedes personalizar los templates de generacion para adaptar el codigo generado:

```gradle
tasks.register('generateJavaClient', GenerateTask) {
    // ... configuracion basica ...

    // Usar templates personalizados
    templateDir = "${projectDir}/openapi-templates/java"

    // Opciones adicionales
    configOptions = [
        'library': 'native',
        'useJakartaEe': 'true',
        // ... mas opciones ...

        // Custom templates
        'apiTemplateFiles': [
            'api.mustache': '.java'
        ],
        'modelTemplateFiles': [
            'model.mustache': '.java'
        ]
    ]
}
```

**Estructura de templates**:

```
openapi-templates/java/
├── api.mustache
├── model.mustache
├── pom.mustache
└── README.mustache
```

---

## SpringDoc OpenAPI

APiGen usa **SpringDoc OpenAPI** para generar automaticamente la especificacion OpenAPI desde los controladores.

### Configuracion OpenAPI

**`OpenApiConfig.java`**:

```java
package com.jnzader.apigen.core.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API Generica de Microservicios")
                        .version("1.0.0")
                        .description("Plantilla de API RESTful generica construida con " +
                                     "Spring Boot 4, Java 25, Spring Security y MapStruct. " +
                                     "Proporciona funcionalidades CRUD basicas para entidades " +
                                     "con auditoria.")
                        .termsOfService("http://swagger.io/terms/")
                        .contact(new Contact()
                                .name("Javier N. Zader")
                                .url("https://github.com/JNZader")
                                .email("tu_email@example.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("http://springdoc.org")));
    }
}
```

### Configuracion en application.yaml

```yaml
springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /swagger-ui.html
    operations-sorter: method
    tags-sorter: alpha
    display-request-duration: true
```

### Anotaciones Swagger

#### @Tag - Agrupar Endpoints

```java
@Tag(name = "Products", description = "Product management API")
@RequestMapping("/api/v1/products")
public interface ProductController extends BaseController<ProductDTO, Long> {
    // endpoints...
}
```

#### @Operation - Documentar Endpoint

```java
@Operation(
    summary = "Find product by code",
    description = "Retrieves a single product by its unique code",
    tags = {"Products"}
)
@ApiResponses(value = {
    @ApiResponse(
        responseCode = "200",
        description = "Product found",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ProductDTO.class)
        )
    ),
    @ApiResponse(
        responseCode = "404",
        description = "Product not found",
        content = @Content
    )
})
@GetMapping("/code/{code}")
ResponseEntity<ProductDTO> findByCode(
    @Parameter(description = "Product code", example = "PROD001")
    @PathVariable String code
);
```

#### @Schema - Documentar Modelos

```java
@Schema(description = "Product Data Transfer Object")
public class ProductDTO extends BaseDTO {

    @Schema(description = "Product name", example = "Laptop HP", required = true)
    @NotBlank
    private String name;

    @Schema(description = "Product code", example = "LAPTOP-001", required = true)
    @NotBlank
    private String code;

    @Schema(description = "Product price", example = "999.99", required = true)
    @DecimalMin("0.01")
    private BigDecimal price;

    @Schema(description = "Stock quantity", example = "50", minimum = "0")
    @Min(0)
    private Integer stock;
}
```

#### @Parameter - Documentar Parametros

```java
@GetMapping
ResponseEntity<Page<ProductDTO>> findAll(
    @Parameter(description = "Page number (0-indexed)", example = "0")
    @RequestParam(defaultValue = "0") int page,

    @Parameter(description = "Page size", example = "20")
    @RequestParam(defaultValue = "20") int size,

    @Parameter(description = "Sort criteria", example = "name,asc")
    @RequestParam(required = false) String sort
);
```

### Personalizacion de Swagger UI

Puedes personalizar la interfaz de Swagger UI con CSS:

```html
<!-- src/main/resources/static/swagger-ui-custom.css -->
<style>
.swagger-ui .topbar {
    background-color: #2c3e50;
}

.swagger-ui .info .title {
    color: #3498db;
}
</style>
```

Cargar en `application.yaml`:

```yaml
springdoc:
  swagger-ui:
    path: /swagger-ui.html
    custom-css: classpath:/static/swagger-ui-custom.css
```

### Endpoints de Documentacion

```
# OpenAPI JSON spec
http://localhost:8080/v3/api-docs

# Swagger UI
http://localhost:8080/swagger-ui.html

# OpenAPI YAML spec
http://localhost:8080/v3/api-docs.yaml
```

---

## Flujos de Trabajo

### Flujo 1: Crear Nueva Entidad de Negocio

```
┌──────────────────────────────────────────────────────────────┐
│          CREAR NUEVA ENTIDAD DE NEGOCIO                      │
└──────────────────────────────────────────────────────────────┘

1. Analizar requisitos
   ├── Nombre de entidad: Product
   ├── Modulo: products
   └── Campos: name, code, price, stock, description

2. Generar entidad con Gradle
   $ ./gradlew generateEntity \
       -Pname=Product \
       -Pmodule=products \
       -Pfields="name:string,code:string,price:decimal,stock:int,description:string"

3. Revisar archivos generados
   ├── Product.java          ✓
   ├── ProductDTO.java       ✓
   ├── ProductMapper.java    ✓
   ├── ProductRepository.java ✓
   ├── ProductService.java   ✓
   ├── ProductController.java ✓
   └── V2__create_products.sql ✓

4. Customizar archivos (opcional)
   ├── Agregar validaciones al DTO
   ├── Agregar metodos custom al Service
   ├── Agregar queries custom al Repository
   └── Agregar endpoints custom al Controller

5. Compilar codigo
   $ ./gradlew compileJava

   ├── MapStruct genera implementations ✓
   ├── Lombok procesa anotaciones ✓
   └── Spring procesa components ✓

6. Ejecutar migraciones
   $ ./gradlew flywayMigrate

   ├── V2__create_products.sql ejecutada ✓
   └── Tablas products y products_aud creadas ✓

7. Ejecutar tests
   $ ./gradlew test

   └── ProductServiceImplTest pasa ✓

8. Arrancar aplicacion
   $ ./gradlew bootRun

   ├── Endpoints disponibles en /api/v1/products ✓
   └── Documentacion en /swagger-ui.html ✓

9. Probar endpoints
   $ curl http://localhost:8080/api/v1/products

   └── API funcional ✓

10. Generar SDKs (opcional)
    $ ./gradlew generateAllClients

    ├── Java client generado ✓
    ├── TypeScript client generado ✓
    └── Python client generado ✓
```

### Flujo 2: Extender Funcionalidad Existente

```
┌──────────────────────────────────────────────────────────────┐
│          EXTENDER FUNCIONALIDAD EXISTENTE                    │
└──────────────────────────────────────────────────────────────┘

Ejemplo: Agregar busqueda por codigo en Product

1. Agregar query method al Repository

   ProductRepository.java:
   └── Optional<Product> findByCode(String code);

2. Agregar metodo al Service Interface

   ProductService.java:
   └── Result<Product, Exception> findByCode(String code);

3. Implementar en ServiceImpl

   ProductServiceImpl.java:
   └── @Transactional(readOnly = true)
       public Result<Product, Exception> findByCode(String code) {
           return Result.fromOptional(
               productRepository.findByCode(code),
               () -> new ResourceNotFoundException("Product not found")
           );
       }

4. Agregar endpoint al Controller Interface

   ProductController.java:
   └── @GetMapping("/code/{code}")
       ResponseEntity<ProductDTO> findByCode(@PathVariable String code);

5. Implementar en ControllerImpl

   ProductControllerImpl.java:
   └── @Override
       public ResponseEntity<ProductDTO> findByCode(String code) {
           return productService.findByCode(code)
               .map(productMapper::toDto)
               .fold(
                   ResponseEntity::ok,
                   error -> ResponseEntity.notFound().build()
               );
       }

6. Agregar test

   ProductServiceImplTest.java:
   └── @Test
       void shouldFindProductByCode() { ... }

7. Compilar y probar
   $ ./gradlew compileJava test

8. Regenerar OpenAPI spec
   $ ./gradlew generateOpenApiDocs

9. Regenerar SDKs (si es necesario)
   $ ./gradlew generateAllClients
```

### Flujo 3: Customizar Controladores

```
┌──────────────────────────────────────────────────────────────┐
│              CUSTOMIZAR CONTROLADORES                        │
└──────────────────────────────────────────────────────────────┘

Ejemplo: Agregar endpoint para productos con bajo stock

1. Agregar metodo al Service

   ProductService.java:
   └── Result<List<Product>, Exception> findLowStock(Integer threshold);

   ProductServiceImpl.java:
   └── @Transactional(readOnly = true)
       public Result<List<Product>, Exception> findLowStock(Integer threshold) {
           return Result.of(() ->
               productRepository.findByStockLessThan(threshold)
           );
       }

2. Agregar endpoint custom al Controller

   ProductController.java:
   └── @Operation(summary = "Find products with low stock")
       @GetMapping("/low-stock")
       ResponseEntity<List<ProductDTO>> findLowStock(
           @Parameter(description = "Stock threshold", example = "10")
           @RequestParam(defaultValue = "10") Integer threshold
       );

   ProductControllerImpl.java:
   └── @Override
       public ResponseEntity<List<ProductDTO>> findLowStock(Integer threshold) {
           return productService.findLowStock(threshold)
               .map(products -> products.stream()
                   .map(productMapper::toDto)
                   .toList())
               .fold(
                   ResponseEntity::ok,
                   error -> ResponseEntity.status(500).build()
               );
       }

3. Documentar con Swagger
   └── Ya esta documentado con @Operation y @Parameter ✓

4. Test
   $ curl http://localhost:8080/api/v1/products/low-stock?threshold=5

5. Regenerar spec y SDKs
   $ ./gradlew generateOpenApiDocs generateAllClients
```

---

## Scripts de Automatizacion

### Comandos Gradle Disponibles

#### Tests

```bash
# Tests unitarios
./gradlew test

# Tests de seguridad
./gradlew test -Ptest.security

# Tests de integracion
./gradlew test -Ptest.integration

# Tests de arquitectura
./gradlew test -Ptest.architecture

# Todos los tests
./gradlew test -Ptest.all
```

#### Generacion de Codigo

```bash
# Generar entidad completa
./gradlew generateEntity -Pname=Product -Pmodule=products

# Generar entidad con campos
./gradlew generateEntity \
  -Pname=Product \
  -Pmodule=products \
  -Pfields="name:string,price:decimal,stock:int"
```

#### Generacion de SDKs

```bash
# Generar especificacion OpenAPI
./gradlew generateOpenApiDocs

# Generar cliente Java
./gradlew generateJavaClient

# Generar cliente TypeScript
./gradlew generateTypeScriptClient

# Generar cliente Python
./gradlew generatePythonClient

# Generar todos los clientes
./gradlew generateAllClients
```

#### Base de Datos

```bash
# Ejecutar migraciones
./gradlew flywayMigrate

# Ver informacion de migraciones
./gradlew flywayInfo

# Validar migraciones
./gradlew flywayValidate

# Limpiar base de datos (CUIDADO!)
./gradlew flywayClean
```

#### Aplicacion

```bash
# Arrancar aplicacion
./gradlew bootRun

# Empaquetar en JAR
./gradlew bootJar

# Empaquetar en WAR
./gradlew bootWar

# Limpiar build
./gradlew clean

# Build completo
./gradlew clean build
```

### Tasks Personalizadas

Puedes crear tasks personalizadas en `build.gradle`:

```gradle
// Task: Regenerar todo (migraciones, spec, SDKs)
tasks.register('regenerateAll') {
    group = 'code generation'
    description = 'Regenerates migrations, OpenAPI spec and all SDKs'

    dependsOn 'flywayMigrate', 'generateOpenApiDocs', 'generateAllClients'

    doLast {
        println "All code regenerated successfully!"
    }
}

// Task: Setup proyecto completo
tasks.register('setupProject') {
    group = 'setup'
    description = 'Sets up the project (compile, migrate, generate docs)'

    dependsOn 'clean', 'compileJava', 'flywayMigrate', 'generateOpenApiDocs'

    doLast {
        println """
        ==========================================
        Project setup complete!
        ==========================================

        Next steps:
        1. Start the application: ./gradlew bootRun
        2. Open Swagger UI: http://localhost:8080/swagger-ui.html
        3. Generate SDKs: ./gradlew generateAllClients

        ==========================================
        """
    }
}

// Task: Verificacion completa antes de commit
tasks.register('preCommit') {
    group = 'verification'
    description = 'Runs all checks before committing'

    dependsOn 'clean', 'compileJava', 'test', 'flywayValidate'

    doLast {
        println "Pre-commit checks passed! Ready to commit."
    }
}
```

**Uso**:

```bash
# Regenerar todo
./gradlew regenerateAll

# Setup completo del proyecto
./gradlew setupProject

# Verificacion pre-commit
./gradlew preCommit
```

---

## Best Practices

### 1. Generacion de Entidades

**DO**:
```bash
# ✓ Usar nombres descriptivos en PascalCase
./gradlew generateEntity -Pname=CustomerOrder -Pmodule=orders

# ✓ Especificar campos con tipos apropiados
-Pfields="orderDate:datetime,totalAmount:decimal,status:string"

# ✓ Revisar y customizar codigo generado
# - Agregar validaciones al DTO
# - Agregar indices a la migracion
# - Agregar metodos de negocio al servicio
```

**DON'T**:
```bash
# ✗ Usar nombres genericos
./gradlew generateEntity -Pname=Data -Pmodule=stuff

# ✗ No especificar tipos
-Pfields="field1,field2,field3"

# ✗ Usar codigo generado sin revisar
# - Puede necesitar ajustes para tu dominio
```

### 2. Migraciones

**DO**:
```sql
-- ✓ Agregar comentarios descriptivos
COMMENT ON TABLE products IS 'Products catalog table';
COMMENT ON COLUMN products.code IS 'Unique product identifier code';

-- ✓ Crear indices apropiados
CREATE INDEX idx_products_code ON products(code);
CREATE INDEX idx_products_category ON products(category_id);

-- ✓ Agregar constraints de negocio
ALTER TABLE products ADD CONSTRAINT chk_price_positive
    CHECK (price > 0);
```

**DON'T**:
```sql
-- ✗ No modificar migraciones ya ejecutadas
-- Crear nueva migracion para cambios

-- ✗ No usar DROP CASCADE sin cuidado
DROP TABLE products CASCADE; -- Peligroso!
```

### 3. DTOs y Validacion

**DO**:
```java
// ✓ Validaciones especificas por contexto
@NotBlank(groups = OnCreate.class, message = "Name is required")
@Size(min = 2, max = 100, groups = {OnCreate.class, OnUpdate.class})
private String name;

// ✓ Validaciones custom con metodos
public ProductDTO {
    if (Boolean.TRUE.equals(active) && stock != null && stock <= 0) {
        throw new IllegalArgumentException(
            "Active product must have positive stock"
        );
    }
}
```

**DON'T**:
```java
// ✗ Misma validacion para todo
@NotBlank // Sin especificar grupos

// ✗ Validacion en el Controller
if (dto.getName() == null) { // NO!
    throw new IllegalArgumentException("Name required");
}
```

### 4. Servicios Custom

**DO**:
```java
// ✓ Usar Result pattern
@Transactional(readOnly = true)
public Result<Product, Exception> findByCode(String code) {
    return Result.fromOptional(
        repository.findByCode(code),
        () -> new ResourceNotFoundException("Product not found")
    );
}

// ✓ Logging apropiado
log.debug("Finding product by code: {}", code);
log.info("Product {} created successfully", id);
log.warn("Low stock alert for product {}", id);
```

**DON'T**:
```java
// ✗ Lanzar excepciones para control de flujo
@Transactional(readOnly = true)
public Product findByCode(String code) {
    return repository.findByCode(code)
        .orElseThrow(() -> new NotFoundException()); // NO!
}

// ✗ Log excesivo
log.info("Entering method findByCode"); // Muy verbose
log.info("Exiting method findByCode");
```

### 5. Controllers Custom

**DO**:
```java
// ✓ Documentar con Swagger
@Operation(summary = "Find product by code",
           description = "Retrieves a product by its unique code")
@ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Product found"),
    @ApiResponse(responseCode = "404", description = "Product not found")
})
@GetMapping("/code/{code}")
ResponseEntity<ProductDTO> findByCode(@PathVariable String code);

// ✓ Usar Result.fold para mapear respuestas
return service.findByCode(code)
    .map(mapper::toDto)
    .fold(
        ResponseEntity::ok,
        error -> ResponseEntity.notFound().build()
    );
```

**DON'T**:
```java
// ✗ Sin documentacion
@GetMapping("/code/{code}")
ResponseEntity<ProductDTO> findByCode(@PathVariable String code);

// ✗ Try-catch manual
try {
    Product p = service.findByCode(code);
    return ResponseEntity.ok(mapper.toDto(p));
} catch (Exception e) {
    return ResponseEntity.notFound().build();
}
```

### 6. Generacion de SDKs

**DO**:
```bash
# ✓ Regenerar SDKs despues de cambios en API
./gradlew generateOpenApiDocs generateAllClients

# ✓ Versionar SDKs generados (Git submodule o package registry)
git submodule add ./generated-sdks sdks

# ✓ Publicar SDKs en registry
npm publish ./generated-sdks/typescript-client
mvn deploy -f ./generated-sdks/java-client/pom.xml
```

**DON'T**:
```bash
# ✗ Usar SDKs desactualizados
# Regenerar siempre despues de cambios

# ✗ Modificar codigo generado manualmente
# Usar templates personalizados en su lugar
```

### 7. OpenAPI Spec

**DO**:
```java
// ✓ Documentar todos los endpoints
@Operation(summary = "...", description = "...")

// ✓ Documentar todos los parametros
@Parameter(description = "...", example = "...")

// ✓ Documentar todas las respuestas
@ApiResponses(value = {
    @ApiResponse(responseCode = "200", ...),
    @ApiResponse(responseCode = "404", ...),
    @ApiResponse(responseCode = "400", ...)
})

// ✓ Ejemplos realistas
@Schema(description = "Product price", example = "99.99")
```

**DON'T**:
```java
// ✗ Sin documentacion
@GetMapping("/{id}")
ResponseEntity<?> get(@PathVariable Long id);

// ✗ Ejemplos poco utiles
@Schema(example = "123") // Poco descriptivo
```

---

## Resumen

Los **generadores de codigo de APiGen** proporcionan:

### Generador de Entidades
- Crea estructura DDD completa (Entity, DTO, Mapper, Service, Controller)
- Genera migraciones Flyway con auditoria
- Incluye tests unitarios basicos
- Soporta multiples tipos de datos
- Sigue arquitectura hexagonal

### Generador de SDKs
- Genera clientes type-safe en Java, TypeScript, Python
- Usa especificacion OpenAPI del proyecto
- Configurable con templates personalizados
- Listo para publicar en registries (npm, Maven, PyPI)

### SpringDoc OpenAPI
- Documentacion automatica desde codigo
- Swagger UI interactivo
- Anotaciones completas (@Operation, @Schema, @Parameter)
- Personalizable y extensible

### Comandos Principales

```bash
# Generar entidad
./gradlew generateEntity -Pname=Product -Pmodule=products

# Generar SDKs
./gradlew generateAllClients

# Generar spec OpenAPI
./gradlew generateOpenApiDocs

# Setup completo
./gradlew clean compileJava flywayMigrate test
```

**Proximos Pasos**:
- Ver `10-CHEATSHEET.md` para referencia rapida
- Ver `PATRONES-DISENO.md` para patrones arquitectonicos
- Ver `ARQUITECTURA-DDD.md` para arquitectura detallada

---

**Documento generado por**: APiGen Framework v1.0
**Fecha**: 2025-11-27
**Licencia**: MIT
