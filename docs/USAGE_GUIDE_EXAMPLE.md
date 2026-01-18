# APiGen - Guía de Uso: Módulo de Ejemplo

Esta guía explica cómo usar el módulo `apigen-example` como punto de partida para tu proyecto.

## Índice

1. [Cuándo Usar Esta Opción](#cuándo-usar-esta-opción)
2. [Inicio Rápido](#inicio-rápido)
3. [Estructura del Proyecto](#estructura-del-proyecto)
4. [Personalizar el Ejemplo](#personalizar-el-ejemplo)
5. [Ejecutar la Aplicación](#ejecutar-la-aplicación)
6. [Probar la API](#probar-la-api)
7. [Agregar Nuevas Entidades](#agregar-nuevas-entidades)
8. [Configuración](#configuración)

---

## Cuándo Usar Esta Opción

Usa el módulo de ejemplo cuando:

- Quieres empezar un proyecto nuevo desde cero
- Prefieres tener todo configurado y listo para usar
- Quieres ver ejemplos de implementación reales
- Necesitas una estructura de proyecto probada

**No uses esta opción si:**
- Ya tienes un proyecto Spring Boot existente (usa la [guía de librería](USAGE_GUIDE_LIBRARY.md))
- Solo necesitas generar entidades desde SQL (usa la [guía de codegen](USAGE_GUIDE_CODEGEN.md))

---

## Inicio Rápido

### Opción 1: Clonar el repositorio completo

```bash
# Clonar el repositorio
git clone https://github.com/jnzader/apigen.git
cd apigen

# Copiar el módulo de ejemplo a una nueva ubicación
cp -r apigen-example ../mi-nuevo-proyecto
cd ../mi-nuevo-proyecto

# Modificar el nombre del proyecto
```

### Opción 2: Usar como template de GitHub

1. Ve a https://github.com/jnzader/apigen
2. Click en "Use this template" → "Create a new repository"
3. Nombra tu repositorio
4. Clona tu nuevo repositorio
5. Navega a `apigen-example/` y úsalo como base

### Opción 3: Descargar solo el ejemplo

```bash
# Descargar solo el módulo de ejemplo
curl -L https://github.com/jnzader/apigen/archive/main.zip -o apigen.zip
unzip apigen.zip
mv apigen-main/apigen-example mi-nuevo-proyecto
cd mi-nuevo-proyecto
```

---

## Estructura del Proyecto

```
apigen-example/
├── build.gradle                    # Configuración de Gradle
├── README.md                       # Documentación del ejemplo
├── src/
│   ├── main/
│   │   ├── java/com/jnzader/example/
│   │   │   ├── ExampleApplication.java          # Clase principal
│   │   │   ├── domain/
│   │   │   │   ├── entity/
│   │   │   │   │   └── Product.java             # Entidad de ejemplo
│   │   │   │   └── repository/
│   │   │   │       └── ProductRepository.java   # Repository
│   │   │   ├── application/
│   │   │   │   ├── dto/
│   │   │   │   │   └── ProductDTO.java          # DTO
│   │   │   │   ├── mapper/
│   │   │   │   │   └── ProductMapper.java       # MapStruct Mapper
│   │   │   │   └── service/
│   │   │   │       └── ProductService.java      # Service
│   │   │   └── infrastructure/
│   │   │       ├── controller/
│   │   │       │   └── ProductController.java   # REST Controller
│   │   │       └── hateoas/
│   │   │           └── ProductResourceAssembler.java
│   │   └── resources/
│   │       ├── application.yaml                 # Configuración principal
│   │       ├── application-dev.yaml             # Perfil desarrollo
│   │       ├── application-prod.yaml            # Perfil producción
│   │       └── db/migration/
│   │           ├── V1__initial_schema.sql       # Esquema de seguridad
│   │           └── V2__products_table.sql       # Tabla de productos
│   └── test/
│       └── java/                                # Tests
```

---

## Personalizar el Ejemplo

### Paso 1: Cambiar el nombre del paquete

1. Renombra los directorios:
```bash
# Desde apigen-example/src/main/java/
mv com/jnzader/example com/tuempresa/tuproyecto
```

2. Actualiza los imports en todos los archivos Java

3. Actualiza `ExampleApplication.java`:
```java
package com.tuempresa.tuproyecto;

@SpringBootApplication
@EntityScan(basePackages = {
    "com.tuempresa.tuproyecto",
    "com.jnzader.apigen.core.domain.entity",
    "com.jnzader.apigen.security.domain.entity"
})
public class TuProyectoApplication {
    public static void main(String[] args) {
        SpringApplication.run(TuProyectoApplication.class, args);
    }
}
```

### Paso 2: Actualizar build.gradle

```groovy
group = 'com.tuempresa'
version = '0.1.0-SNAPSHOT'
description = 'Mi Proyecto - API REST'

// Si estás usando el ejemplo como proyecto standalone, cambia:
dependencies {
    // De esto (referencia local):
    // implementation project(':apigen-core')
    // implementation project(':apigen-security')

    // A esto (dependencia de Maven):
    implementation 'com.jnzader:apigen-core:1.0.0-SNAPSHOT'
    implementation 'com.jnzader:apigen-security:1.0.0-SNAPSHOT'
}
```

### Paso 3: Actualizar application.yaml

```yaml
spring:
  application:
    name: tu-proyecto-api

  datasource:
    url: jdbc:postgresql://localhost:5432/tu_base_de_datos
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:postgres}

apigen:
  security:
    jwt:
      secret: ${JWT_SECRET:tu-clave-secreta-de-256-bits-aqui}
      issuer: tu-proyecto
```

---

## Ejecutar la Aplicación

### Con Docker Compose (recomendado)

1. Crea un `docker-compose.yml`:

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:17-alpine
    environment:
      POSTGRES_DB: apigen_db
      POSTGRES_USER: apigen_user
      POSTGRES_PASSWORD: apigen_password
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      DB_URL: jdbc:postgresql://postgres:5432/apigen_db
      DB_USERNAME: apigen_user
      DB_PASSWORD: apigen_password
      JWT_SECRET: your-super-secret-jwt-key-minimum-32-characters
    depends_on:
      - postgres

volumes:
  postgres_data:
```

2. Ejecuta:
```bash
docker-compose up -d postgres
./gradlew bootRun
```

### Con H2 (desarrollo rápido)

```bash
./gradlew bootRun --args='--spring.profiles.active=dev'
```

Crea `application-dev.yaml`:
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:devdb
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop
  h2:
    console:
      enabled: true
      path: /h2-console

apigen:
  security:
    enabled: false  # Deshabilitar en desarrollo
```

### Verificar que funciona

```bash
# Health check
curl http://localhost:8080/actuator/health

# Swagger UI
open http://localhost:8080/swagger-ui.html

# API Docs
curl http://localhost:8080/v3/api-docs
```

---

## Probar la API

### Endpoints de Productos

```bash
# Listar todos los productos
curl http://localhost:8080/api/products

# Obtener un producto
curl http://localhost:8080/api/products/1

# Crear un producto
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "MacBook Pro",
    "description": "Laptop profesional",
    "price": 2499.99,
    "stock": 10,
    "category": "Electronics",
    "sku": "MBP-001"
  }'

# Actualizar un producto
curl -X PUT http://localhost:8080/api/products/1 \
  -H "Content-Type: application/json" \
  -d '{
    "id": 1,
    "activo": true,
    "name": "MacBook Pro 16",
    "price": 2699.99,
    "stock": 8
  }'

# Actualización parcial
curl -X PATCH http://localhost:8080/api/products/1 \
  -H "Content-Type: application/json" \
  -d '{"stock": 5}'

# Eliminar (soft delete)
curl -X DELETE http://localhost:8080/api/products/1

# Restaurar
curl -X POST http://localhost:8080/api/products/1/restore

# Buscar por categoría (endpoint personalizado)
curl http://localhost:8080/api/products/category/Electronics

# Buscar por nombre (endpoint personalizado)
curl "http://localhost:8080/api/products/search?name=MacBook"
```

### Filtrado Avanzado

```bash
# Filtrar por múltiples criterios
curl "http://localhost:8080/api/products?filter=category:eq:Electronics,price:lte:1000"

# Paginación
curl "http://localhost:8080/api/products?page=0&size=10&sort=price,desc"

# Paginación por cursor (mejor rendimiento)
curl "http://localhost:8080/api/products/cursor?size=10&sort=id&direction=DESC"

# Sparse fieldsets
curl "http://localhost:8080/api/products?fields=id,name,price"
```

---

## Agregar Nuevas Entidades

### Ejemplo: Agregar entidad Category

**1. Crear la entidad:**

```java
// src/main/java/com/jnzader/example/domain/entity/Category.java
package com.jnzader.example.domain.entity;

import com.jnzader.apigen.core.domain.entity.Base;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor
public class Category extends Base {

    @NotBlank
    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY)
    private List<Product> products = new ArrayList<>();
}
```

**2. Crear el DTO:**

```java
// src/main/java/com/jnzader/example/application/dto/CategoryDTO.java
package com.jnzader.example.application.dto;

import com.jnzader.apigen.core.application.dto.BaseDTO;
import jakarta.validation.constraints.NotBlank;

public record CategoryDTO(
    Long id,
    Boolean activo,
    @NotBlank String name,
    String description,
    Integer productCount  // Campo calculado
) implements BaseDTO { }
```

**3. Crear el Mapper:**

```java
// src/main/java/com/jnzader/example/application/mapper/CategoryMapper.java
package com.jnzader.example.application.mapper;

import com.jnzader.apigen.core.application.mapper.BaseMapper;
import com.jnzader.example.application.dto.CategoryDTO;
import com.jnzader.example.domain.entity.Category;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface CategoryMapper extends BaseMapper<Category, CategoryDTO> {

    @Override
    @Mapping(source = "estado", target = "activo")
    @Mapping(target = "productCount", expression = "java(entity.getProducts().size())")
    CategoryDTO toDTO(Category entity);

    @Override
    @Mapping(source = "activo", target = "estado")
    @Mapping(target = "products", ignore = true)
    Category toEntity(CategoryDTO dto);

    @Override
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(source = "activo", target = "estado")
    @Mapping(target = "products", ignore = true)
    void updateEntityFromDTO(CategoryDTO dto, @MappingTarget Category entity);

    @Override
    default void updateDTOFromEntity(Category entity, @MappingTarget CategoryDTO dto) { }
}
```

**4. Crear el Repository:**

```java
// src/main/java/com/jnzader/example/domain/repository/CategoryRepository.java
package com.jnzader.example.domain.repository;

import com.jnzader.apigen.core.domain.repository.BaseRepository;
import com.jnzader.example.domain.entity.Category;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CategoryRepository extends BaseRepository<Category, Long> {
    Optional<Category> findByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCase(String name);
}
```

**5. Crear el Service:**

```java
// src/main/java/com/jnzader/example/application/service/CategoryService.java
package com.jnzader.example.application.service;

import com.jnzader.apigen.core.application.service.BaseServiceImpl;
import com.jnzader.apigen.core.application.service.CacheEvictionService;
import com.jnzader.example.domain.entity.Category;
import com.jnzader.example.domain.repository.CategoryRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Service;

@Service
public class CategoryService extends BaseServiceImpl<Category, Long> {

    private final CategoryRepository categoryRepository;

    public CategoryService(
            CategoryRepository repository,
            CacheEvictionService cacheEvictionService,
            ApplicationEventPublisher eventPublisher,
            AuditorAware<String> auditorAware
    ) {
        super(repository, cacheEvictionService, eventPublisher, auditorAware);
        this.categoryRepository = repository;
    }

    @Override
    protected Class<Category> getEntityClass() {
        return Category.class;
    }

    public boolean existsByName(String name) {
        return categoryRepository.existsByNameIgnoreCase(name);
    }
}
```

**6. Crear el Resource Assembler:**

```java
// src/main/java/com/jnzader/example/infrastructure/hateoas/CategoryResourceAssembler.java
package com.jnzader.example.infrastructure.hateoas;

import com.jnzader.apigen.core.infrastructure.hateoas.BaseResourceAssembler;
import com.jnzader.example.application.dto.CategoryDTO;
import com.jnzader.example.infrastructure.controller.CategoryController;
import org.springframework.stereotype.Component;

@Component
public class CategoryResourceAssembler extends BaseResourceAssembler<CategoryDTO, Long> {
    public CategoryResourceAssembler() {
        super(CategoryController.class);
    }
}
```

**7. Crear el Controller:**

```java
// src/main/java/com/jnzader/example/infrastructure/controller/CategoryController.java
package com.jnzader.example.infrastructure.controller;

import com.jnzader.apigen.core.domain.specification.FilterSpecificationBuilder;
import com.jnzader.apigen.core.infrastructure.controller.BaseControllerImpl;
import com.jnzader.example.application.dto.CategoryDTO;
import com.jnzader.example.application.mapper.CategoryMapper;
import com.jnzader.example.application.service.CategoryService;
import com.jnzader.example.domain.entity.Category;
import com.jnzader.example.infrastructure.hateoas.CategoryResourceAssembler;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/categories")
@Tag(name = "Categories", description = "API de gestión de categorías")
public class CategoryController extends BaseControllerImpl<Category, CategoryDTO, Long> {

    public CategoryController(
            CategoryService service,
            CategoryMapper mapper,
            CategoryResourceAssembler assembler,
            FilterSpecificationBuilder filterBuilder
    ) {
        super(service, mapper, assembler, filterBuilder);
    }

    @Override
    protected String getResourceName() {
        return "Category";
    }

    @Override
    protected Class<Category> getEntityClass() {
        return Category.class;
    }
}
```

**8. Crear la migración:**

```sql
-- src/main/resources/db/migration/V3__categories_table.sql

CREATE TABLE categories (
    id BIGINT PRIMARY KEY DEFAULT nextval('base_sequence'),
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(500),

    -- Campos de Base
    estado BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_eliminacion TIMESTAMP,
    eliminado_por VARCHAR(100),
    creado_por VARCHAR(100),
    modificado_por VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0
);

-- Agregar FK a products
ALTER TABLE products ADD COLUMN category_id BIGINT REFERENCES categories(id);
CREATE INDEX idx_products_category_id ON products(category_id);

-- Datos de ejemplo
INSERT INTO categories (name, description) VALUES
    ('Electronics', 'Electronic devices and accessories'),
    ('Accessories', 'Computer and mobile accessories'),
    ('Software', 'Software and licenses');
```

---

## Configuración

### Perfiles Disponibles

| Perfil | Descripción | Comando |
|--------|-------------|---------|
| `default` | Producción con PostgreSQL | `./gradlew bootRun` |
| `dev` | Desarrollo con H2 | `./gradlew bootRun --args='--spring.profiles.active=dev'` |
| `test` | Tests con H2 in-memory | Automático en tests |

### Variables de Entorno

| Variable | Descripción | Default |
|----------|-------------|---------|
| `DB_URL` | URL de conexión JDBC | `jdbc:postgresql://localhost:5432/apigen_db` |
| `DB_USERNAME` | Usuario de BD | `postgres` |
| `DB_PASSWORD` | Contraseña de BD | `postgres` |
| `JWT_SECRET` | Clave secreta JWT (min 32 chars) | - |
| `JWT_EXPIRATION_MINUTES` | Expiración del access token | `15` |

---

## Siguiente Paso

- Ver [Guía de Uso: Generación de Código](USAGE_GUIDE_CODEGEN.md) para generar entidades automáticamente
- Ver [Guía de Uso: Como Librería](USAGE_GUIDE_LIBRARY.md) para agregar a proyecto existente
