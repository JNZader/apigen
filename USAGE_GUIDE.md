# Guía de Uso - APiGen

Esta guía proporciona instrucciones detalladas para usar APiGen como template para desarrollar tu propia API REST.

## Tabla de Contenidos

1. [Crear una Nueva Entidad](#1-crear-una-nueva-entidad)
   - [Generador Automático](#generador-automático-recomendado)
   - [Creación Manual](#creación-manual)
2. [Operaciones CRUD](#2-operaciones-crud)
3. [Autenticación y Autorización](#3-autenticación-y-autorización)
4. [Validaciones](#4-validaciones)
5. [Manejo de Errores](#5-manejo-de-errores)
6. [Caché Personalizado](#6-caché-personalizado)
7. [Eventos de Dominio](#7-eventos-de-dominio)
8. [Filtros y Búsquedas](#8-filtros-y-búsquedas)
9. [Generación de SDKs](#9-generación-de-sdks)
10. [Testing](#10-testing)
11. [Despliegue](#11-despliegue)

---

## 1. Crear una Nueva Entidad

APiGen incluye un **generador automático** que crea toda la estructura necesaria para una nueva entidad en segundos. También puedes crear los archivos manualmente si prefieres mayor control.

### Generador Automático (Recomendado)

El generador crea automáticamente **10 archivos** con código funcional:
- Entity, DTO, Mapper, Repository
- Service (interface + implementation)
- Controller (interface + implementation)
- Migración SQL
- Test unitario

#### Requisitos Previos

- Java 25+ instalado y configurado en `JAVA_HOME`
- Estar en el directorio raíz del proyecto (`apigen/`)

---

### Opción 1: Usando Gradle (Multiplataforma)

La tarea Gradle funciona en **Windows, Linux y macOS**.

#### Sintaxis

```bash
./gradlew generateEntity -Pname=<EntityName> -Pmodule=<modulename> [-Pfields=<field1:type1,field2:type2,...>]
```

#### Parámetros

| Parámetro | Requerido | Descripción | Ejemplo |
|-----------|-----------|-------------|---------|
| `-Pname` | Sí | Nombre de la entidad en PascalCase | `Product`, `CustomerOrder` |
| `-Pmodule` | Sí | Nombre del módulo en minúsculas | `products`, `orders` |
| `-Pfields` | No | Campos separados por coma en formato `nombre:tipo` | `name:string,price:decimal` |

#### Ejemplos de Uso

```bash
# Entidad sin campos (solo estructura base)
./gradlew generateEntity -Pname=Category -Pmodule=categories

# Entidad con campos básicos
./gradlew generateEntity -Pname=Product -Pmodule=products -Pfields=name:string,price:decimal,stock:int

# Entidad con múltiples campos
./gradlew generateEntity -Pname=Customer -Pmodule=customers -Pfields=firstName:string,lastName:string,email:string,phone:string,birthDate:date,active:boolean

# Entidad para órdenes
./gradlew generateEntity -Pname=Order -Pmodule=orders -Pfields=orderNumber:string,total:decimal,status:string,orderDate:datetime

# Entidad para inventario
./gradlew generateEntity -Pname=InventoryItem -Pmodule=inventory -Pfields=sku:string,quantity:int,minStock:int,location:string
```

#### En Windows (CMD o PowerShell)

```cmd
# CMD
gradlew.bat generateEntity -Pname=Product -Pmodule=products -Pfields=name:string,price:decimal

# PowerShell
.\gradlew.bat generateEntity -Pname=Product -Pmodule=products -Pfields="name:string,price:decimal"
```

#### Ver Ayuda de la Tarea

```bash
./gradlew help --task generateEntity
```

---

### Opción 2: Usando PowerShell (Windows)

El script PowerShell ofrece una sintaxis más simple y salida con colores.

#### Requisitos

1. **PowerShell 5.1+** (incluido en Windows 10/11)
2. **Política de ejecución** que permita scripts locales

#### Configurar Política de Ejecución (Primera vez)

Abre PowerShell como **Administrador** y ejecuta:

```powershell
# Ver política actual
Get-ExecutionPolicy

# Permitir scripts locales (recomendado)
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser

# O permitir solo para esta sesión
Set-ExecutionPolicy -ExecutionPolicy Bypass -Scope Process
```

#### Sintaxis

```powershell
.\generate-entity.ps1 <EntityName> <modulename> [fields]
```

#### Parámetros

| Posición | Parámetro | Requerido | Descripción |
|----------|-----------|-----------|-------------|
| 1 | `EntityName` | Sí | Nombre en PascalCase |
| 2 | `modulename` | Sí | Nombre del módulo en minúsculas |
| 3 | `fields` | No | Campos entre comillas |

#### Ejemplos de Uso

```powershell
# Entidad sin campos
.\generate-entity.ps1 Category categories

# Entidad con campos
.\generate-entity.ps1 Product products "name:string,price:decimal,stock:int,sku:string"

# Entidad Customer completa
.\generate-entity.ps1 Customer customers "firstName:string,lastName:string,email:string,phone:string,birthDate:date"

# Entidad con campos booleanos y fechas
.\generate-entity.ps1 Task tasks "title:string,description:string,completed:boolean,dueDate:datetime,priority:int"
```

#### Ver Ayuda del Script

```powershell
Get-Help .\generate-entity.ps1 -Full
```

#### Solución de Problemas PowerShell

**Error: "No se puede cargar el archivo porque la ejecución de scripts está deshabilitada"**
```powershell
# Solución: Permitir ejecución para el usuario actual
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
```

**Error: "El archivo no tiene firma digital"**
```powershell
# Solución: Ejecutar con bypass temporal
powershell -ExecutionPolicy Bypass -File .\generate-entity.ps1 Product products "name:string"
```

**Caracteres extraños en la salida**
```powershell
# Solución: Configurar codificación UTF-8
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
.\generate-entity.ps1 Product products
```

---

### Tipos de Campo Soportados

| Tipo | Java | SQL | Ejemplo de uso |
|------|------|-----|----------------|
| `string` | String | VARCHAR(255) | `name:string` |
| `int` | Integer | INTEGER | `stock:int` |
| `long` | Long | BIGINT | `totalViews:long` |
| `decimal` | BigDecimal | DECIMAL(10,2) | `price:decimal` |
| `double` | Double | DOUBLE PRECISION | `rating:double` |
| `float` | Float | REAL | `weight:float` |
| `boolean` | Boolean | BOOLEAN | `active:boolean` |
| `date` | LocalDate | DATE | `birthDate:date` |
| `datetime` | LocalDateTime | TIMESTAMP | `createdAt:datetime` |
| `instant` | Instant | TIMESTAMP | `lastLogin:instant` |

---

### Ejemplo Completo Paso a Paso

#### 1. Generar la entidad

```bash
./gradlew generateEntity -Pname=Product -Pmodule=products -Pfields=name:string,description:string,price:decimal,stock:int,sku:string
```

**Salida:**
```
==========================================
APiGen - Entity Generator
==========================================
Entity: Product
Module: products
Table:  products
Fields: 5

[+] Directory: src/main/java/com/jnzader/apigen/products/domain/entity
[+] Entity: Product.java
[+] DTO: ProductDTO.java
[+] Mapper: ProductMapper.java
[+] Repository: ProductRepository.java
[+] Service: ProductService.java
[+] Service Impl: ProductServiceImpl.java
[+] Controller: ProductController.java
[+] Controller Impl: ProductControllerImpl.java
[+] Migration: V2__create_products_table.sql
[+] Test: ProductServiceImplTest.java

==========================================
Entity generation complete!
==========================================

Endpoint available at: /api/v1/products
```

#### 2. Revisar archivos generados

```bash
# Ver estructura creada
tree src/main/java/com/jnzader/apigen/products/

# En Windows
dir /s src\main\java\com\jnzader\apigen\products\
```

#### 3. Personalizar la entidad (opcional)

Editar `src/main/java/com/jnzader/apigen/products/domain/entity/Product.java`:

```java
@Entity
@Table(name = "products")
@Audited
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Product extends Base {

    @NotBlank(message = "El nombre es requerido")
    @Size(min = 2, max = 100)
    @Column(nullable = false, length = 100)
    private String name;

    @Size(max = 500)
    @Column(length = 500)
    private String description;

    @NotNull(message = "El precio es requerido")
    @Positive(message = "El precio debe ser positivo")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @NotNull
    @PositiveOrZero
    @Column(nullable = false)
    private Integer stock;

    @NotBlank
    @Column(nullable = false, unique = true, length = 50)
    private String sku;
}
```

#### 4. Agregar validaciones al DTO

Editar `src/main/java/com/jnzader/apigen/products/application/dto/ProductDTO.java`:

```java
@Data
@NoArgsConstructor @AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class ProductDTO extends BaseDTO {

    @NotBlank(message = "El nombre es requerido", groups = OnCreate.class)
    @Size(min = 2, max = 100)
    private String name;

    @Size(max = 500)
    private String description;

    @NotNull(message = "El precio es requerido", groups = OnCreate.class)
    @Positive(message = "El precio debe ser positivo")
    private BigDecimal price;

    @NotNull(groups = OnCreate.class)
    @PositiveOrZero
    private Integer stock;

    @NotBlank(groups = OnCreate.class)
    private String sku;
}
```

#### 5. Compilar y verificar

```bash
# Compilar
./gradlew compileJava

# Ejecutar tests de la nueva entidad
./gradlew test --tests "*ProductServiceImplTest*"
```

#### 6. Iniciar la aplicación

```bash
./gradlew bootRun
```

#### 7. Probar el endpoint

```bash
# Crear producto
curl -X POST http://localhost:8080/api/v1/products \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${TOKEN}" \
  -d '{"name":"Laptop","price":999.99,"stock":50,"sku":"LAP-001"}'

# Listar productos
curl http://localhost:8080/api/v1/products \
  -H "Authorization: Bearer ${TOKEN}"
```

---

### Estructura de Archivos Generados

```
src/main/java/com/jnzader/apigen/{module}/
├── domain/
│   ├── entity/
│   │   └── {Entity}.java              # Entidad JPA
│   ├── event/                         # (directorio para eventos)
│   └── exception/                     # (directorio para excepciones)
├── application/
│   ├── dto/
│   │   └── {Entity}DTO.java           # Data Transfer Object
│   ├── mapper/
│   │   └── {Entity}Mapper.java        # MapStruct mapper
│   └── service/
│       ├── {Entity}Service.java       # Interface del servicio
│       └── {Entity}ServiceImpl.java   # Implementación
└── infrastructure/
    ├── repository/
    │   └── {Entity}Repository.java    # Spring Data JPA repository
    └── controller/
        ├── {Entity}Controller.java    # Interface del controlador
        └── {Entity}ControllerImpl.java # Implementación REST

src/main/resources/db/migration/
└── V{n}__create_{table}_table.sql     # Migración Flyway

src/test/java/com/jnzader/apigen/{module}/
└── application/service/
    └── {Entity}ServiceImplTest.java   # Test unitario
```

---

### Pasos Post-Generación

1. **Revisar y personalizar** los archivos generados
2. **Agregar validaciones** al DTO (`@NotNull`, `@Size`, `@Email`, etc.)
3. **Ajustar la migración SQL** si es necesario:
   - Agregar constraints (`UNIQUE`, `NOT NULL`, `CHECK`)
   - Agregar índices adicionales
   - Modificar tipos de columna
4. **Agregar métodos personalizados** al Repository y Service
5. **Compilar**: `./gradlew compileJava`
6. **Ejecutar tests**: `./gradlew test`
7. **Iniciar la app**: `./gradlew bootRun`

---

### Resumen de Comandos

| Tarea | Gradle | PowerShell |
|-------|--------|------------|
| Generar entidad básica | `./gradlew generateEntity -Pname=X -Pmodule=y` | `.\generate-entity.ps1 X y` |
| Generar con campos | `./gradlew generateEntity -Pname=X -Pmodule=y -Pfields=a:string,b:int` | `.\generate-entity.ps1 X y "a:string,b:int"` |
| Compilar | `./gradlew compileJava` | `.\gradlew.bat compileJava` |
| Ejecutar tests | `./gradlew test` | `.\gradlew.bat test` |
| Iniciar app | `./gradlew bootRun` | `.\gradlew.bat bootRun` |

---

### Creación Manual

Si prefieres crear los archivos manualmente para mayor control, sigue estos pasos:

#### Paso 1: Definir la Entidad

Crea la clase de entidad en `src/main/java/com/jnzader/apigen/{modulo}/domain/entity/`:

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

    @NotBlank(message = "El nombre es requerido")
    @Size(min = 2, max = 100)
    @Column(nullable = false, length = 100)
    private String name;

    @Size(max = 500)
    @Column(length = 500)
    private String description;

    @NotNull(message = "El precio es requerido")
    @Positive(message = "El precio debe ser positivo")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @NotNull(message = "El stock es requerido")
    @PositiveOrZero(message = "El stock no puede ser negativo")
    @Column(nullable = false)
    private Integer stock;

    @NotBlank(message = "El SKU es requerido")
    @Column(nullable = false, unique = true, length = 50)
    private String sku;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductStatus status = ProductStatus.ACTIVE;

    public enum ProductStatus {
        ACTIVE, INACTIVE, OUT_OF_STOCK
    }
}
```

### Paso 2: Crear el DTO

Crea el DTO en `src/main/java/com/jnzader/apigen/{modulo}/application/dto/`:

```java
package com.jnzader.apigen.products.application.dto;

import com.jnzader.apigen.core.application.dto.BaseDTO;
import com.jnzader.apigen.core.application.dto.validation.OnCreate;
import com.jnzader.apigen.core.application.dto.validation.OnUpdate;
import com.jnzader.apigen.products.domain.entity.Product.ProductStatus;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class ProductDTO extends BaseDTO {

    @NotBlank(message = "El nombre es requerido", groups = OnCreate.class)
    @Size(min = 2, max = 100, groups = {OnCreate.class, OnUpdate.class})
    private String name;

    @Size(max = 500, groups = {OnCreate.class, OnUpdate.class})
    private String description;

    @NotNull(message = "El precio es requerido", groups = OnCreate.class)
    @Positive(message = "El precio debe ser positivo", groups = {OnCreate.class, OnUpdate.class})
    private BigDecimal price;

    @NotNull(message = "El stock es requerido", groups = OnCreate.class)
    @PositiveOrZero(message = "El stock no puede ser negativo", groups = {OnCreate.class, OnUpdate.class})
    private Integer stock;

    @NotBlank(message = "El SKU es requerido", groups = OnCreate.class)
    private String sku;

    private ProductStatus status;
}
```

### Paso 3: Crear el Mapper (MapStruct)

Crea el mapper en `src/main/java/com/jnzader/apigen/{modulo}/application/mapper/`:

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

### Paso 4: Crear el Repositorio

Crea el repositorio en `src/main/java/com/jnzader/apigen/{modulo}/infrastructure/repository/`:

```java
package com.jnzader.apigen.products.infrastructure.repository;

import com.jnzader.apigen.core.infrastructure.repository.BaseRepository;
import com.jnzader.apigen.products.domain.entity.Product;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends BaseRepository<Product, Long> {

    Optional<Product> findBySku(String sku);

    boolean existsBySku(String sku);

    @Query("SELECT p FROM Product p WHERE p.stock < :threshold AND p.estado = true")
    List<Product> findLowStockProducts(int threshold);
}
```

### Paso 5: Crear la Interfaz de Servicio

Crea la interfaz en `src/main/java/com/jnzader/apigen/{modulo}/application/service/`:

```java
package com.jnzader.apigen.products.application.service;

import com.jnzader.apigen.core.application.service.BaseService;
import com.jnzader.apigen.core.application.util.Result;
import com.jnzader.apigen.products.domain.entity.Product;

import java.util.List;

public interface ProductService extends BaseService<Product, Long> {

    Result<Product, Exception> findBySku(String sku);

    Result<List<Product>, Exception> findLowStockProducts(int threshold);

    Result<Product, Exception> updateStock(Long id, int quantity);
}
```

### Paso 6: Implementar el Servicio

Crea la implementación en `src/main/java/com/jnzader/apigen/{modulo}/application/service/`:

```java
package com.jnzader.apigen.products.application.service;

import com.jnzader.apigen.core.application.service.BaseServiceImpl;
import com.jnzader.apigen.core.application.util.Result;
import com.jnzader.apigen.core.domain.exception.ResourceNotFoundException;
import com.jnzader.apigen.products.domain.entity.Product;
import com.jnzader.apigen.products.infrastructure.repository.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
    @Transactional(readOnly = true)
    public Result<Product, Exception> findBySku(String sku) {
        return Result.of(() -> productRepository.findBySku(sku)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "sku", sku)));
    }

    @Override
    @Transactional(readOnly = true)
    public Result<List<Product>, Exception> findLowStockProducts(int threshold) {
        return Result.of(() -> productRepository.findLowStockProducts(threshold));
    }

    @Override
    @CacheEvict(value = {"entities", "lists"}, allEntries = true)
    public Result<Product, Exception> updateStock(Long id, int quantity) {
        return findById(id)
                .flatMap(product -> {
                    int newStock = product.getStock() + quantity;
                    if (newStock < 0) {
                        return Result.failure(
                            new IllegalArgumentException("Stock insuficiente")
                        );
                    }
                    product.setStock(newStock);

                    if (newStock == 0) {
                        product.setStatus(Product.ProductStatus.OUT_OF_STOCK);
                    } else if (product.getStatus() == Product.ProductStatus.OUT_OF_STOCK) {
                        product.setStatus(Product.ProductStatus.ACTIVE);
                    }

                    return save(product);
                });
    }

    @Override
    protected String getEntityName() {
        return "Product";
    }
}
```

### Paso 7: Crear la Interfaz del Controlador

Crea la interfaz en `src/main/java/com/jnzader/apigen/{modulo}/infrastructure/controller/`:

```java
package com.jnzader.apigen.products.infrastructure.controller;

import com.jnzader.apigen.core.infrastructure.controller.BaseController;
import com.jnzader.apigen.products.application.dto.ProductDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Products", description = "API de gestión de productos")
@RequestMapping("/api/v1/products")
public interface ProductController extends BaseController<ProductDTO, Long> {

    @Operation(summary = "Buscar producto por SKU")
    @GetMapping("/sku/{sku}")
    ResponseEntity<EntityModel<ProductDTO>> findBySku(@PathVariable String sku);

    @Operation(summary = "Obtener productos con stock bajo")
    @GetMapping("/low-stock")
    ResponseEntity<CollectionModel<EntityModel<ProductDTO>>> getLowStockProducts(
            @RequestParam(defaultValue = "10") int threshold);

    @Operation(summary = "Actualizar stock de producto")
    @PatchMapping("/{id}/stock")
    ResponseEntity<EntityModel<ProductDTO>> updateStock(
            @PathVariable Long id,
            @RequestParam int quantity);
}
```

### Paso 8: Implementar el Controlador

Crea la implementación en `src/main/java/com/jnzader/apigen/{modulo}/infrastructure/controller/`:

```java
package com.jnzader.apigen.products.infrastructure.controller;

import com.jnzader.apigen.core.infrastructure.controller.BaseControllerImpl;
import com.jnzader.apigen.products.application.dto.ProductDTO;
import com.jnzader.apigen.products.application.mapper.ProductMapper;
import com.jnzader.apigen.products.application.service.ProductService;
import com.jnzader.apigen.products.domain.entity.Product;
import lombok.extern.slf4j.Slf4j;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

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

    @Override
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<EntityModel<ProductDTO>> findBySku(String sku) {
        return productService.findBySku(sku)
                .map(productMapper::toDto)
                .map(dto -> EntityModel.of(dto,
                        linkTo(methodOn(ProductController.class).findById(dto.getId())).withSelfRel(),
                        linkTo(methodOn(ProductController.class).findAll(null)).withRel("products")))
                .fold(
                        dto -> ResponseEntity.ok(dto),
                        error -> { throw new RuntimeException(error); }
                );
    }

    @Override
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<CollectionModel<EntityModel<ProductDTO>>> getLowStockProducts(int threshold) {
        return productService.findLowStockProducts(threshold)
                .map(products -> products.stream()
                        .map(productMapper::toDto)
                        .map(dto -> EntityModel.of(dto,
                                linkTo(methodOn(ProductController.class).findById(dto.getId())).withSelfRel()))
                        .toList())
                .map(models -> CollectionModel.of(models,
                        linkTo(methodOn(ProductController.class).getLowStockProducts(threshold)).withSelfRel()))
                .fold(
                        ResponseEntity::ok,
                        error -> { throw new RuntimeException(error); }
                );
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EntityModel<ProductDTO>> updateStock(Long id, int quantity) {
        return productService.updateStock(id, quantity)
                .map(productMapper::toDto)
                .map(dto -> EntityModel.of(dto,
                        linkTo(methodOn(ProductController.class).findById(id)).withSelfRel()))
                .fold(
                        ResponseEntity::ok,
                        error -> { throw new RuntimeException(error); }
                );
    }
}
```

### Paso 9: Crear Migración de Base de Datos

Crea el archivo `V2__create_products_table.sql` en `src/main/resources/db/migration/`:

```sql
-- V2__create_products_table.sql

CREATE TABLE products (
    id BIGINT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    price DECIMAL(10, 2) NOT NULL,
    stock INTEGER NOT NULL DEFAULT 0,
    sku VARCHAR(50) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

    -- Campos heredados de Base
    estado BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP,
    fecha_eliminacion TIMESTAMP,
    creado_por VARCHAR(100),
    modificado_por VARCHAR(100),
    eliminado_por VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0
);

-- Tabla de auditoría (Hibernate Envers)
CREATE TABLE products_aud (
    id BIGINT NOT NULL,
    rev INTEGER NOT NULL REFERENCES revision_info(id),
    revtype SMALLINT,
    name VARCHAR(100),
    description VARCHAR(500),
    price DECIMAL(10, 2),
    stock INTEGER,
    sku VARCHAR(50),
    status VARCHAR(20),
    estado BOOLEAN,
    fecha_eliminacion TIMESTAMP,
    modificado_por VARCHAR(100),
    eliminado_por VARCHAR(100),
    PRIMARY KEY (id, rev)
);

-- Índices
CREATE INDEX idx_products_sku ON products(sku);
CREATE INDEX idx_products_status ON products(status) WHERE estado = TRUE;
CREATE INDEX idx_products_stock ON products(stock) WHERE estado = TRUE;
CREATE INDEX idx_products_estado ON products(estado);
```

---

## 2. Operaciones CRUD

### Crear (POST)

```bash
curl -X POST http://localhost:8080/api/v1/products \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${TOKEN}" \
  -d '{
    "name": "Laptop Gaming",
    "description": "Laptop para gaming con RTX 4080",
    "price": 2499.99,
    "stock": 50,
    "sku": "LAPTOP-001"
  }'
```

Respuesta (201 Created):
```json
{
  "id": 1,
  "name": "Laptop Gaming",
  "description": "Laptop para gaming con RTX 4080",
  "price": 2499.99,
  "stock": 50,
  "sku": "LAPTOP-001",
  "status": "ACTIVE",
  "estado": true,
  "fechaCreacion": "2024-01-15T10:30:00",
  "creadoPor": "admin",
  "_links": {
    "self": { "href": "http://localhost:8080/api/v1/products/1" },
    "products": { "href": "http://localhost:8080/api/v1/products" }
  }
}
```

### Leer (GET)

```bash
# Obtener por ID
curl http://localhost:8080/api/v1/products/1 \
  -H "Authorization: Bearer ${TOKEN}"

# Listar con paginación
curl "http://localhost:8080/api/v1/products?page=0&size=10&sort=name,asc" \
  -H "Authorization: Bearer ${TOKEN}"

# Buscar por SKU (endpoint personalizado)
curl http://localhost:8080/api/v1/products/sku/LAPTOP-001 \
  -H "Authorization: Bearer ${TOKEN}"
```

### Actualizar (PUT/PATCH)

```bash
# Actualización completa (PUT)
curl -X PUT http://localhost:8080/api/v1/products/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "If-Match: \"abc123\"" \
  -d '{
    "name": "Laptop Gaming Pro",
    "description": "Laptop gaming actualizada",
    "price": 2799.99,
    "stock": 45,
    "sku": "LAPTOP-001",
    "status": "ACTIVE"
  }'

# Actualización parcial (PATCH)
curl -X PATCH http://localhost:8080/api/v1/products/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${TOKEN}" \
  -d '{
    "price": 2299.99
  }'
```

### Eliminar (DELETE)

```bash
# Soft delete (por defecto)
curl -X DELETE http://localhost:8080/api/v1/products/1 \
  -H "Authorization: Bearer ${TOKEN}"
```

---

## 3. Autenticación y Autorización

### Login

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123"
  }'
```

Respuesta:
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 900
}
```

### Registro

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "newuser",
    "password": "SecurePass123!",
    "email": "newuser@example.com",
    "firstName": "New",
    "lastName": "User"
  }'
```

### Refresh Token

```bash
curl -X POST http://localhost:8080/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }'
```

### Logout

```bash
curl -X POST http://localhost:8080/api/v1/auth/logout \
  -H "Authorization: Bearer ${TOKEN}"
```

### Proteger Endpoints con Roles

```java
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<?> adminOnlyEndpoint() { ... }

@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
public ResponseEntity<?> userOrAdminEndpoint() { ... }

@PreAuthorize("hasAuthority('READ')")
public ResponseEntity<?> readPermissionEndpoint() { ... }
```

---

## 4. Validaciones

### Grupos de Validación

APiGen usa grupos de validación para diferentes operaciones:

```java
// En el DTO
public class ProductDTO {

    @NotBlank(groups = OnCreate.class)  // Solo requerido al crear
    @Size(min = 2, max = 100, groups = {OnCreate.class, OnUpdate.class})
    private String name;

    @NotNull(groups = OnCreate.class)   // Solo requerido al crear
    @Positive(groups = {OnCreate.class, OnUpdate.class})
    private BigDecimal price;
}
```

### Aplicar Validación en Controlador

```java
// Para creación
@PostMapping
public ResponseEntity<?> create(
    @Validated(OnCreate.class) @RequestBody ProductDTO dto) { ... }

// Para actualización
@PutMapping("/{id}")
public ResponseEntity<?> update(
    @PathVariable Long id,
    @Validated(OnUpdate.class) @RequestBody ProductDTO dto) { ... }
```

### Validaciones Personalizadas

```java
// Crear anotación
@Target({FIELD})
@Retention(RUNTIME)
@Constraint(validatedBy = UniqueSkuValidator.class)
public @interface UniqueSku {
    String message() default "El SKU ya existe";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

// Crear validador
@Component
public class UniqueSkuValidator implements ConstraintValidator<UniqueSku, String> {

    @Autowired
    private ProductRepository repository;

    @Override
    public boolean isValid(String sku, ConstraintValidatorContext context) {
        return sku == null || !repository.existsBySku(sku);
    }
}

// Usar en DTO
public class ProductDTO {
    @UniqueSku(groups = OnCreate.class)
    private String sku;
}
```

---

## 5. Manejo de Errores

### Estructura de Error Estándar

APiGen devuelve errores en formato RFC 7807 (Problem Details):

```json
{
  "type": "about:blank",
  "title": "Not Found",
  "status": 404,
  "detail": "Producto con id 999 no encontrado",
  "instance": "/api/v1/products/999",
  "timestamp": "2024-01-15T10:30:00Z",
  "requestId": "abc-123-def"
}
```

### Excepciones Personalizadas

```java
// Crear excepción
public class InsufficientStockException extends BusinessException {

    public InsufficientStockException(String productName, int requested, int available) {
        super(String.format(
            "Stock insuficiente para '%s'. Solicitado: %d, Disponible: %d",
            productName, requested, available
        ));
    }
}

// Usar en servicio
if (product.getStock() < quantity) {
    throw new InsufficientStockException(
        product.getName(),
        quantity,
        product.getStock()
    );
}
```

### Usar el Patrón Result

```java
// En servicio
public Result<Product, Exception> processOrder(Long productId, int quantity) {
    return findById(productId)
        .flatMap(product -> {
            if (product.getStock() < quantity) {
                return Result.failure(new InsufficientStockException(...));
            }
            product.setStock(product.getStock() - quantity);
            return save(product);
        });
}

// En controlador
@PostMapping("/{id}/order")
public ResponseEntity<?> processOrder(@PathVariable Long id, @RequestParam int quantity) {
    return productService.processOrder(id, quantity)
        .fold(
            product -> ResponseEntity.ok(mapper.toDto(product)),
            error -> {
                if (error instanceof InsufficientStockException) {
                    throw (InsufficientStockException) error;
                }
                throw new RuntimeException(error);
            }
        );
}
```

---

## 6. Caché Personalizado

### Configurar Caché para Nueva Entidad

El caché se configura automáticamente para operaciones base. Para operaciones personalizadas:

```java
@Service
public class ProductServiceImpl extends BaseServiceImpl<Product, Long> {

    @Override
    @Cacheable(
        value = "entities",
        key = "'Product:sku:' + #sku",
        unless = "#result == null"
    )
    public Result<Product, Exception> findBySku(String sku) {
        return Result.of(() -> repository.findBySku(sku)
            .orElseThrow(() -> new ResourceNotFoundException("Product", "sku", sku)));
    }

    @Override
    @CacheEvict(value = {"entities", "lists"}, allEntries = true)
    public Result<Product, Exception> updateStock(Long id, int quantity) {
        // ... implementación
    }
}
```

### Invalidar Caché Manualmente

```java
@Autowired
private CacheEvictionService cacheEvictionService;

public void clearProductCache() {
    cacheEvictionService.evictAllCaches();
}

public void clearSpecificCache(Long productId) {
    cacheEvictionService.evictEntityCache("Product", productId);
}
```

---

## 7. Eventos de Dominio

### Escuchar Eventos de Entidad

```java
@Component
@Slf4j
public class ProductEventListener {

    @EventListener
    @Async
    public void handleProductCreated(EntityCreatedEvent<Product> event) {
        Product product = event.getEntity();
        log.info("Producto creado: {} (SKU: {})", product.getName(), product.getSku());
        // Enviar notificación, actualizar inventario, etc.
    }

    @EventListener
    @Async
    public void handleProductDeleted(EntityDeletedEvent<Product> event) {
        Product product = event.getEntity();
        log.info("Producto eliminado: {}", product.getId());
        // Limpiar recursos relacionados
    }

    @EventListener
    @Async
    public void handleLowStock(LowStockEvent event) {
        log.warn("Stock bajo para producto {}: {} unidades",
            event.getProductId(), event.getCurrentStock());
        // Enviar alerta
    }
}
```

### Crear Eventos Personalizados

```java
// Definir evento
public record LowStockEvent(Long productId, String sku, int currentStock) {}

// Publicar evento desde servicio
@Autowired
private ApplicationEventPublisher eventPublisher;

public Result<Product, Exception> updateStock(Long id, int quantity) {
    return findById(id)
        .flatMap(product -> {
            product.setStock(product.getStock() + quantity);

            if (product.getStock() < 10) {
                eventPublisher.publishEvent(
                    new LowStockEvent(product.getId(), product.getSku(), product.getStock())
                );
            }

            return save(product);
        });
}
```

---

## 8. Filtros y Búsquedas

### Usar JPA Specifications

```java
// Crear especificación
public class ProductSpecifications {

    public static Specification<Product> hasName(String name) {
        return (root, query, cb) ->
            name == null ? null : cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }

    public static Specification<Product> hasPriceBetween(BigDecimal min, BigDecimal max) {
        return (root, query, cb) -> {
            if (min == null && max == null) return null;
            if (min == null) return cb.lessThanOrEqualTo(root.get("price"), max);
            if (max == null) return cb.greaterThanOrEqualTo(root.get("price"), min);
            return cb.between(root.get("price"), min, max);
        };
    }

    public static Specification<Product> hasStatus(Product.ProductStatus status) {
        return (root, query, cb) ->
            status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Product> isActive() {
        return (root, query, cb) -> cb.equal(root.get("estado"), true);
    }
}
```

### Implementar Búsqueda en Controlador

```java
@GetMapping("/search")
public ResponseEntity<PagedModel<EntityModel<ProductDTO>>> search(
        @RequestParam(required = false) String name,
        @RequestParam(required = false) BigDecimal minPrice,
        @RequestParam(required = false) BigDecimal maxPrice,
        @RequestParam(required = false) Product.ProductStatus status,
        Pageable pageable) {

    Specification<Product> spec = Specification.where(ProductSpecifications.isActive())
        .and(ProductSpecifications.hasName(name))
        .and(ProductSpecifications.hasPriceBetween(minPrice, maxPrice))
        .and(ProductSpecifications.hasStatus(status));

    return productService.findAll(spec, pageable)
        .map(page -> pagedResourcesAssembler.toModel(
            page.map(productMapper::toDto)))
        .fold(
            ResponseEntity::ok,
            error -> { throw new RuntimeException(error); }
        );
}
```

### Ejemplo de Uso

```bash
# Buscar productos con filtros
curl "http://localhost:8080/api/v1/products/search?\
name=laptop&\
minPrice=1000&\
maxPrice=3000&\
status=ACTIVE&\
page=0&\
size=20&\
sort=price,asc" \
  -H "Authorization: Bearer ${TOKEN}"
```

---

## 9. Generación de SDKs

### Generar Especificación OpenAPI

```bash
# Generar spec durante build
./gradlew build

# O generar solo la spec
./gradlew generateOpenApiDocs
```

La especificación se genera en: `src/main/resources/openapi/api-spec.json`

### Generar Clientes

```bash
# Todos los clientes
./gradlew generateAllClients

# Cliente específico
./gradlew generateJavaClient
./gradlew generateTypeScriptClient
./gradlew generatePythonClient
```

### Personalizar Generación

Editar `build.gradle`:

```gradle
openApiGenerate {
    generatorName = 'java'
    inputSpec = "${projectDir}/src/main/resources/openapi/api-spec.json"
    outputDir = "${buildDir}/generated-sdks/java-client"

    configOptions = [
        'library': 'native',
        'dateLibrary': 'java8',
        'useJakartaEe': 'true',
        'apiPackage': 'com.mycompany.api',        // Personalizar
        'modelPackage': 'com.mycompany.model',    // Personalizar
        'invokerPackage': 'com.mycompany.client'  // Personalizar
    ]
}
```

### Publicar SDK

```bash
# Java (Maven Central)
cd build/generated-sdks/java-client
./gradlew publish

# TypeScript (npm)
cd build/generated-sdks/typescript-client
npm publish

# Python (PyPI)
cd build/generated-sdks/python-client
python -m twine upload dist/*
```

---

## 10. Testing

### Test Unitario de Servicio

```java
@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository repository;

    @InjectMocks
    private ProductServiceImpl service;

    @Test
    void shouldFindProductBySku() {
        // given
        Product product = Product.builder()
            .name("Test Product")
            .sku("TEST-001")
            .build();
        when(repository.findBySku("TEST-001")).thenReturn(Optional.of(product));

        // when
        Result<Product, Exception> result = service.findBySku("TEST-001");

        // then
        assertThat(result.isSuccess()).isTrue();
        result.ifSuccess(p -> {
            assertThat(p.getSku()).isEqualTo("TEST-001");
        });
    }

    @Test
    void shouldFailWhenSkuNotFound() {
        // given
        when(repository.findBySku("INVALID")).thenReturn(Optional.empty());

        // when
        Result<Product, Exception> result = service.findBySku("INVALID");

        // then
        assertThat(result.isFailure()).isTrue();
    }
}
```

### Test de Integración

```java
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ProductIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldCreateProduct() throws Exception {
        ProductDTO dto = ProductDTO.builder()
            .name("Integration Test Product")
            .description("Test description")
            .price(new BigDecimal("99.99"))
            .stock(100)
            .sku("INT-TEST-001")
            .build();

        mockMvc.perform(post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isCreated())
            .andExpect(header().exists("Location"))
            .andExpect(jsonPath("$.name").value("Integration Test Product"))
            .andExpect(jsonPath("$.sku").value("INT-TEST-001"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void shouldGetProductBySku() throws Exception {
        // given
        Product product = repository.save(Product.builder()
            .name("Test Product")
            .price(new BigDecimal("49.99"))
            .stock(50)
            .sku("SKU-001")
            .build());

        // when/then
        mockMvc.perform(get("/api/v1/products/sku/SKU-001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(product.getId()))
            .andExpect(jsonPath("$.sku").value("SKU-001"));
    }
}
```

### Ejecutar Tests

```bash
# Todos los tests unitarios
./gradlew test

# Tests de integración
./gradlew test -Ptest.integration

# Con cobertura (cuando JaCoCo soporte Java 25)
./gradlew test jacocoTestReport
```

---

## 11. Despliegue

### Variables de Entorno Requeridas

| Variable | Descripción | Ejemplo |
|----------|-------------|---------|
| `DATABASE_URL` | URL JDBC PostgreSQL | `jdbc:postgresql://db:5432/apigen` |
| `DATABASE_USERNAME` | Usuario de DB | `apigen` |
| `DATABASE_PASSWORD` | Contraseña de DB | `secure_password` |
| `JWT_SECRET` | Secreto JWT (Base64, 64+ chars) | `base64encodedstring...` |
| `SPRING_PROFILES_ACTIVE` | Perfil activo | `prod` |

### Despliegue con Docker

```bash
# Build de imagen
docker build -t apigen:latest .

# Ejecutar con variables
docker run -d \
  --name apigen \
  -p 8080:8080 \
  -e DATABASE_URL=jdbc:postgresql://db:5432/apigen \
  -e DATABASE_USERNAME=apigen \
  -e DATABASE_PASSWORD=secure_password \
  -e JWT_SECRET=your_base64_secret \
  -e SPRING_PROFILES_ACTIVE=prod \
  apigen:latest
```

### Despliegue con Docker Compose

```bash
# Producción
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d
```

### Health Checks

```bash
# Verificar salud
curl http://localhost:8080/actuator/health

# Respuesta esperada
{
  "status": "UP",
  "components": {
    "db": { "status": "UP" },
    "diskSpace": { "status": "UP" },
    "ping": { "status": "UP" }
  }
}
```

### Monitoreo

- **Métricas Prometheus**: `http://localhost:8080/actuator/prometheus`
- **Grafana Dashboard**: `http://localhost:3000` (admin/admin)
- **Logs**: `docker logs -f apigen`

---

## Resumen de Comandos

| Comando | Descripción |
|---------|-------------|
| `./gradlew bootRun` | Ejecutar en desarrollo |
| `./gradlew build` | Compilar y testear |
| `./gradlew test` | Ejecutar tests unitarios |
| `./gradlew test -Ptest.all` | Ejecutar todos los tests |
| `./gradlew generateAllClients` | Generar todos los SDKs |
| `./gradlew flywayMigrate` | Ejecutar migraciones |
| `docker-compose up -d` | Iniciar con Docker |
| `docker-compose logs -f` | Ver logs |

---

## Soporte

Para reportar bugs o solicitar features, crear un issue en el repositorio.
