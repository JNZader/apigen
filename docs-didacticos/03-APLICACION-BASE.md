# 03 - Capa de Aplicacion Base

## Objetivo

Entender la capa de aplicacion que orquesta las operaciones de negocio. Este documento cubre los componentes del modulo `apigen-core`:

- DTOs base con validaciones
- Mappers con MapStruct
- Result Pattern (Either)
- BaseService y BaseServiceImpl

## Ubicacion en el Proyecto Multi-Modulo

```
apigen/
├── apigen-core/                      # <-- Este modulo contiene la capa de aplicacion
│   └── src/main/java/com/jnzader/apigen/core/
│       └── application/
│           ├── dto/
│           │   ├── BaseDTO.java
│           │   └── BaseDTOValidated.java
│           ├── mapper/
│           │   └── BaseMapper.java
│           ├── service/
│           │   ├── BaseService.java
│           │   └── BaseServiceImpl.java
│           ├── util/
│           │   └── Result.java
│           └── validation/
│               └── ValidationGroups.java
│
└── apigen-example/                   # Ejemplo de uso
    └── src/main/java/com/jnzader/example/
        └── application/
            ├── dto/ProductDTO.java
            ├── mapper/ProductMapper.java
            └── service/ProductService.java
```

## Formas de Usar la Capa de Aplicacion

### Opcion 1: Como Dependencia (Recomendado)

```groovy
dependencies {
    implementation platform('com.jnzader:apigen-bom:1.0.0-SNAPSHOT')
    implementation 'com.jnzader:apigen-core'
}
```

```java
// Tu DTO implementa BaseDTO
import com.jnzader.apigen.core.application.dto.BaseDTO;

public record ProductDTO(
    Long id,
    Boolean activo,
    String name,
    BigDecimal price
) implements BaseDTO {}
```

### Opcion 2: Explorar el Codigo Fuente

```bash
# Ver los componentes de aplicacion
ls apigen-core/src/main/java/com/jnzader/apigen/core/application/
```

## Componentes de la Capa de Aplicacion

```
application/
├── dto/                    # Data Transfer Objects
│   ├── BaseDTO.java
│   └── BaseDTOValidated.java
├── mapper/                 # Mappers Entity <-> DTO
│   └── BaseMapper.java
├── service/                # Servicios de aplicacion
│   ├── BaseService.java
│   └── BaseServiceImpl.java
├── util/                   # Utilidades
│   └── Result.java
└── validation/             # Validacion
    └── ValidationGroups.java
```

---

## BaseDTO

**Ubicacion:** `apigen-core/src/main/java/com/jnzader/apigen/core/application/dto/BaseDTO.java`

Interfaz minimalista que define el contrato basico para todos los DTOs.

```java
package com.jnzader.apigen.core.application.dto;

/**
 * Interfaz base para Data Transfer Objects (DTOs).
 */
public interface BaseDTO {
    Long id();
    Boolean activo();
}
```

### Ejemplo de Implementacion (en apigen-example)

```java
package com.jnzader.example.application.dto;

import com.jnzader.apigen.core.application.dto.BaseDTO;
import com.jnzader.apigen.core.application.validation.ValidationGroups;
import jakarta.validation.constraints.*;

public record ProductDTO(
    @Null(groups = ValidationGroups.Create.class)
    @NotNull(groups = ValidationGroups.Update.class)
    @Positive
    Long id,

    Boolean activo,

    @NotBlank(groups = {ValidationGroups.Create.class, ValidationGroups.Update.class})
    @Size(min = 2, max = 100)
    String name,

    @NotNull(groups = ValidationGroups.Create.class)
    @DecimalMin("0.01")
    BigDecimal price,

    @Min(0)
    Integer stock
) implements BaseDTO {}
```

---

## ValidationGroups

**Ubicacion:** `apigen-core/src/main/java/com/jnzader/apigen/core/application/validation/ValidationGroups.java`

Define grupos de validacion para controlar que validaciones se aplican en cada operacion.

```java
package com.jnzader.apigen.core.application.validation;

import jakarta.validation.groups.Default;

public final class ValidationGroups {

    private ValidationGroups() {}

    /** Grupo para operaciones de creacion (POST). ID debe ser nulo. */
    public interface Create extends Default {}

    /** Grupo para operaciones de actualizacion (PUT). ID requerido. */
    public interface Update extends Default {}

    /** Grupo para actualizacion parcial (PATCH). Campos opcionales. */
    public interface PartialUpdate {}

    /** Grupo para operaciones de eliminacion. */
    public interface Delete {}

    /** Grupo para busquedas/filtros. */
    public interface Search {}
}
```

### Uso en Controladores

```java
@PostMapping
public ResponseEntity<ProductDTO> create(
    @Validated(ValidationGroups.Create.class) @RequestBody ProductDTO dto
) {
    // ID debe ser nulo, campos obligatorios validados
}

@PutMapping("/{id}")
public ResponseEntity<ProductDTO> update(
    @PathVariable Long id,
    @Validated(ValidationGroups.Update.class) @RequestBody ProductDTO dto
) {
    // ID requerido, todos los campos validados
}
```

---

## BaseMapper con MapStruct

**Ubicacion:** `apigen-core/src/main/java/com/jnzader/apigen/core/application/mapper/BaseMapper.java`

Interfaz generica para mapeo Entity <-> DTO usando MapStruct.

```java
package com.jnzader.apigen.core.application.mapper;

import com.jnzader.apigen.core.application.dto.BaseDTO;
import com.jnzader.apigen.core.domain.entity.Base;
import org.mapstruct.*;

import java.util.List;

public interface BaseMapper<E extends Base, D extends BaseDTO> {

    @Mapping(source = "estado", target = "activo")
    D toDTO(E entity);

    @InheritInverseConfiguration
    E toEntity(D dto);

    List<D> toDTOList(List<E> entities);

    List<E> toEntityList(List<D> dtos);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(source = "activo", target = "estado")
    void updateEntityFromDTO(D dto, @MappingTarget E entity);
}
```

### Ejemplo de Mapper (en apigen-example)

```java
package com.jnzader.example.application.mapper;

import com.jnzader.apigen.core.application.mapper.BaseMapper;
import com.jnzader.example.application.dto.ProductDTO;
import com.jnzader.example.domain.entity.Product;
import org.mapstruct.*;

@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    injectionStrategy = InjectionStrategy.CONSTRUCTOR
)
public interface ProductMapper extends BaseMapper<Product, ProductDTO> {

    @Override
    @Mapping(source = "estado", target = "activo")
    ProductDTO toDTO(Product entity);

    @Override
    @Mapping(source = "activo", target = "estado")
    Product toEntity(ProductDTO dto);
}
```

**Nota:** MapStruct genera automaticamente la implementacion en tiempo de compilacion.

---

## Result Pattern

**Ubicacion:** `apigen-core/src/main/java/com/jnzader/apigen/core/application/util/Result.java`

El patron `Result` permite manejar errores de forma funcional y explicita, evitando excepciones para el control de flujo.

```java
package com.jnzader.apigen.core.application.util;

import java.util.Optional;
import java.util.function.*;

public sealed interface Result<T, E> {

    record Success<T, E>(T value) implements Result<T, E> {}
    record Failure<T, E>(E error) implements Result<T, E> {}

    // Factory Methods
    static <T, E> Result<T, E> success(T value) {
        return new Success<>(value);
    }

    static <T, E> Result<T, E> failure(E error) {
        return new Failure<>(error);
    }

    static <T> Result<T, Exception> of(Supplier<T> supplier) {
        try {
            return success(supplier.get());
        } catch (Exception e) {
            return failure(e);
        }
    }

    static <T, E> Result<T, E> fromOptional(Optional<T> optional, Supplier<E> errorSupplier) {
        return optional
                .<Result<T, E>>map(Result::success)
                .orElseGet(() -> failure(errorSupplier.get()));
    }

    // Transformaciones
    default <R> R fold(Function<? super T, ? extends R> onSuccess,
                       Function<? super E, ? extends R> onFailure) {
        return switch (this) {
            case Success<T, E> s -> onSuccess.apply(s.value());
            case Failure<T, E> f -> onFailure.apply(f.error());
        };
    }

    default <U> Result<U, E> map(Function<? super T, ? extends U> mapper) {
        return switch (this) {
            case Success<T, E> s -> new Success<>(mapper.apply(s.value()));
            case Failure<T, E> f -> new Failure<>(f.error());
        };
    }

    default <U> Result<U, E> flatMap(Function<? super T, Result<U, E>> mapper) {
        return switch (this) {
            case Success<T, E> s -> mapper.apply(s.value());
            case Failure<T, E> f -> new Failure<>(f.error());
        };
    }

    // Predicados
    default boolean isSuccess() { return this instanceof Success; }
    default boolean isFailure() { return this instanceof Failure; }

    // Extraccion
    default T orElse(T defaultValue) {
        return switch (this) {
            case Success<T, E> s -> s.value();
            case Failure<T, E> f -> defaultValue;
        };
    }

    default T orElseThrow() {
        return switch (this) {
            case Success<T, E> s -> s.value();
            case Failure<T, E> f -> {
                if (f.error() instanceof RuntimeException re) throw re;
                throw new RuntimeException("Result failed: " + f.error());
            }
        };
    }

    // Side effects
    default Result<T, E> onSuccess(Consumer<? super T> action) {
        if (this instanceof Success<T, E> s) action.accept(s.value());
        return this;
    }

    default Result<T, E> onFailure(Consumer<? super E> action) {
        if (this instanceof Failure<T, E> f) action.accept(f.error());
        return this;
    }
}
```

### Uso del Result Pattern

```java
// Ejemplo 1: Operacion simple con fold
Result<Product, Exception> result = productService.findById(id);

String message = result.fold(
    product -> "Producto encontrado: " + product.getName(),
    error -> "Error: " + error.getMessage()
);

// Ejemplo 2: Encadenamiento con flatMap
Result<OrderDTO, Exception> orderResult = userService.findById(userId)
    .flatMap(user -> orderService.findByUserId(user.getId()))
    .map(mapper::toDTO);

// Ejemplo 3: Side effects
productService.save(product)
    .onSuccess(p -> log.info("Producto guardado: {}", p.getId()))
    .onFailure(e -> log.error("Error al guardar", e));
```

---

## BaseService y BaseServiceImpl

### BaseService Interface

**Ubicacion:** `apigen-core/src/main/java/com/jnzader/apigen/core/application/service/BaseService.java`

Define el contrato para servicios genericos con operaciones CRUD.

```java
package com.jnzader.apigen.core.application.service;

import com.jnzader.apigen.core.domain.entity.Base;
import com.jnzader.apigen.core.application.util.Result;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.io.Serializable;
import java.util.List;

public interface BaseService<E extends Base, ID extends Serializable> {

    // Consultas basicas
    Result<List<E>, Exception> findAll();
    Result<List<E>, Exception> findAllActive();
    Result<Page<E>, Exception> findAll(Pageable pageable);
    Result<E, Exception> findById(ID id);
    Result<Boolean, Exception> existsById(ID id);
    Result<Long, Exception> count();

    // Busqueda con especificaciones
    Result<List<E>, Exception> findAll(Specification<E> spec);
    Result<Page<E>, Exception> findAll(Specification<E> spec, Pageable pageable);

    // Operaciones de escritura
    Result<E, Exception> save(E entity);
    Result<E, Exception> update(ID id, E entity);
    Result<E, Exception> partialUpdate(ID id, E entity);

    // Soft Delete
    Result<Void, Exception> softDelete(ID id);
    Result<Void, Exception> softDelete(ID id, String usuario);
    Result<E, Exception> restore(ID id);

    // Hard Delete
    Result<Void, Exception> hardDelete(ID id);

    // Batch Operations
    Result<List<E>, Exception> saveAll(List<E> entities);
    Result<Integer, Exception> softDeleteAll(List<ID> ids);
    Result<Integer, Exception> restoreAll(List<ID> ids);
}
```

### BaseServiceImpl

**Ubicacion:** `apigen-core/src/main/java/com/jnzader/apigen/core/application/service/BaseServiceImpl.java`

Implementacion completa con:
- CRUD operations
- Cache integrado con Spring Cache
- Event publishing para Domain Events
- Batch operations con flush periodico
- Logging detallado

```java
package com.jnzader.apigen.core.application.service;

import com.jnzader.apigen.core.domain.entity.Base;
import com.jnzader.apigen.core.domain.repository.BaseRepository;
import com.jnzader.apigen.core.application.util.Result;
// ... mas imports

public abstract class BaseServiceImpl<E extends Base, ID extends Serializable>
    implements BaseService<E, ID> {

    protected final BaseRepository<E, ID> baseRepository;
    protected final CacheEvictionService cacheEvictionService;

    protected BaseServiceImpl(BaseRepository<E, ID> baseRepository,
                              CacheEvictionService cacheEvictionService) {
        this.baseRepository = baseRepository;
        this.cacheEvictionService = cacheEvictionService;
    }

    /** Retorna el nombre de la entidad para mensajes de log y cache. */
    public abstract String getEntityName();

    /** Retorna la clase de la entidad. */
    protected abstract Class<E> getEntityClass();

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "entities", key = "#root.target.entityName + ':' + #id")
    public Result<E, Exception> findById(ID id) {
        log.debug("Buscando {} con ID: {}", getEntityName(), id);
        return Result.fromOptional(
            baseRepository.findById(id),
            () -> new ResourceNotFoundException("Entidad no encontrada: " + id)
        );
    }

    @Override
    @Transactional
    public Result<E, Exception> save(E entity) {
        return Result.of(() -> {
            boolean isNew = entity.getId() == null;
            E saved = baseRepository.save(entity);
            log.info("{} {} con ID: {}",
                isNew ? "Creada" : "Actualizada",
                getEntityName(), saved.getId());
            return saved;
        });
    }

    @Override
    @Transactional
    @CacheEvict(value = "entities", key = "#root.target.entityName + ':' + #id")
    public Result<E, Exception> partialUpdate(ID id, E partialEntity) {
        return findById(id).flatMap(existing -> {
            BeanCopyUtils.copyNonNullProperties(partialEntity, existing);
            return save(existing);
        });
    }

    @Override
    @Transactional
    @CacheEvict(value = "entities", key = "#root.target.entityName + ':' + #id")
    public Result<Void, Exception> softDelete(ID id, String usuario) {
        return findById(id).flatMap(entity -> {
            entity.softDelete(usuario);
            return Result.of(() -> {
                baseRepository.save(entity);
                log.info("{} con ID: {} eliminada logicamente", getEntityName(), id);
                return null;
            });
        });
    }

    // ... mas metodos
}
```

---

## Ejemplo Practico: ProductService

**Ubicacion:** `apigen-example/src/main/java/com/jnzader/example/application/service/ProductService.java`

```java
package com.jnzader.example.application.service;

import com.jnzader.apigen.core.application.service.BaseServiceImpl;
import com.jnzader.apigen.core.application.service.CacheEvictionService;
import com.jnzader.apigen.core.application.util.Result;
import com.jnzader.example.domain.entity.Product;
import com.jnzader.example.domain.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService extends BaseServiceImpl<Product, Long> {

    private final ProductRepository productRepository;

    public ProductService(
        ProductRepository productRepository,
        CacheEvictionService cacheEvictionService
    ) {
        super(productRepository, cacheEvictionService);
        this.productRepository = productRepository;
    }

    @Override
    protected Class<Product> getEntityClass() {
        return Product.class;
    }

    @Override
    public String getEntityName() {
        return "Product";
    }

    // Metodos adicionales especificos
    @Transactional(readOnly = true)
    public Result<Product, Exception> findByName(String name) {
        return Result.fromOptional(
            productRepository.findByNameIgnoreCase(name),
            () -> new ResourceNotFoundException("Producto no encontrado: " + name)
        );
    }
}
```

---

## Uso en Controlador REST

```java
@RestController
@RequestMapping("/api/products")
public class ProductController extends BaseControllerImpl<Product, ProductDTO, Long> {

    // GET /api/products/{id}
    // Heredado de BaseControllerImpl - usa Result pattern internamente

    // POST /api/products
    @PostMapping
    public ResponseEntity<ProductDTO> create(
        @Validated(ValidationGroups.Create.class) @RequestBody ProductDTO dto
    ) {
        return service.save(mapper.toEntity(dto))
            .map(mapper::toDTO)
            .fold(
                producto -> ResponseEntity.status(HttpStatus.CREATED).body(producto),
                error -> ResponseEntity.badRequest().build()
            );
    }

    // PATCH /api/products/{id}
    @PatchMapping("/{id}")
    public ResponseEntity<ProductDTO> partialUpdate(
        @PathVariable Long id,
        @RequestBody ProductDTO dto
    ) {
        return service.partialUpdate(id, mapper.toEntity(dto))
            .map(mapper::toDTO)
            .fold(
                ResponseEntity::ok,
                error -> ResponseEntity.notFound().build()
            );
    }
}
```

---

## Diagrama de Arquitectura

```
┌─────────────────────────────────────────────────────────────────┐
│                        INFRAESTRUCTURA                           │
│                     (REST Controllers)                           │
│  - Recibe DTOs validados                                         │
│  - Llama a servicios                                             │
│  - Maneja Result con fold()                                      │
└────────────────────────┬────────────────────────────────────────┘
                         │ DTOs
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│                     CAPA DE APLICACION                           │
│                                                                   │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐      │
│  │   BaseDTO    │◄───│ BaseMapper   │───►│   Entity     │      │
│  │              │    │  (MapStruct) │    │              │      │
│  └──────────────┘    └──────────────┘    └──────────────┘      │
│                                                                   │
│  ┌────────────────────────────────────────────────────────┐     │
│  │              BaseServiceImpl                            │     │
│  │  - CRUD operations con Result<T, E>                     │     │
│  │  - Cache integrado (@Cacheable, @CacheEvict)            │     │
│  │  - Event publishing (Domain Events)                     │     │
│  │  - Soft delete y restauracion                           │     │
│  └────────────────────┬───────────────────────────────────┘     │
└───────────────────────┼────────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────────────┐
│                       CAPA DE DOMINIO                            │
│  ┌──────────────┐    ┌──────────────┐                           │
│  │    Base      │◄───│BaseRepository│                           │
│  │   Entity     │    │   (JPA)      │                           │
│  └──────────────┘    └──────────────┘                           │
└─────────────────────────────────────────────────────────────────┘
```

---

## Resumen

```
Usas APiGen como dependencia
         │
         └─> Tu DTO implementa BaseDTO
                    │
                    └─> Tu Mapper extiende BaseMapper
                               │
                               └─> Tu Service extiende BaseServiceImpl
                                          │
                                          └─> Heredas automaticamente:
                                                 - CRUD completo
                                                 - Result Pattern
                                                 - Cache integrado
                                                 - Soft delete
                                                 - Batch operations
                                                 - Domain events
```

---

**Anterior:** [02-DOMINIO-BASE.md](./02-DOMINIO-BASE.md)
**Siguiente:** [04-INFRAESTRUCTURA-BASE.md](./04-INFRAESTRUCTURA-BASE.md)
