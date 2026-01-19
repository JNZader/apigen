# APiGen - Guía de Características

Esta guía documenta **todas las características** disponibles en APiGen, independientemente de cómo lo uses (template, ejemplo, librería o codegen).

## Índice

1. [Entidad Base](#1-entidad-base)
2. [Repository Base](#2-repository-base)
3. [Servicio Base](#3-servicio-base)
4. [Controller Base](#4-controller-base)
5. [Filtrado Dinámico](#5-filtrado-dinámico)
6. [Paginación](#6-paginación)
7. [HATEOAS](#7-hateoas)
8. [ETag y Caché](#8-etag-y-caché)
9. [Soft Delete](#9-soft-delete)
10. [Auditoría](#10-auditoría)
11. [Validación](#11-validación)
12. [Eventos de Dominio](#12-eventos-de-dominio)
13. [Result Pattern](#13-result-pattern)
14. [Rate Limiting](#14-rate-limiting)
15. [Seguridad JWT](#15-seguridad-jwt)
16. [OpenAPI/Swagger](#16-openapiswagger)
17. [Feature Flags](#17-feature-flags)
18. [Redis Cache Distribuido](#18-redis-cache-distribuido)
19. [Métricas HikariCP](#19-métricas-hikaricp)
20. [Detección N+1](#20-detección-n1)
21. [Batch Operations](#21-batch-operations)

---

## 1. Entidad Base

Todas las entidades extienden `Base` para obtener campos comunes automáticamente.

### Campos Heredados

```java
public abstract class Base {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "base_seq")
    @SequenceGenerator(name = "base_seq", sequenceName = "base_sequence", allocationSize = 50)
    private Long id;

    @Column(nullable = false)
    private Boolean estado = true;  // Para soft delete

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime fechaCreacion;

    @LastModifiedDate
    private LocalDateTime fechaActualizacion;

    private LocalDateTime fechaEliminacion;

    @CreatedBy
    @Column(updatable = false, length = 100)
    private String creadoPor;

    @LastModifiedBy
    @Column(length = 100)
    private String modificadoPor;

    @Column(length = 100)
    private String eliminadoPor;

    @Version
    private Long version;  // Optimistic locking
}
```

### Uso

```java
@Entity
@Table(name = "products")
public class Product extends Base {
    // Solo tus campos de dominio
    private String name;
    private BigDecimal price;
    // id, estado, fechas, auditoría, version se heredan
}
```

### SQL Correspondiente

```sql
CREATE TABLE products (
    id BIGINT PRIMARY KEY DEFAULT nextval('base_sequence'),
    name VARCHAR(255),
    price DECIMAL(10, 2),

    -- Campos de Base
    estado BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_eliminacion TIMESTAMP,
    creado_por VARCHAR(100),
    modificado_por VARCHAR(100),
    eliminado_por VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0
);
```

---

## 2. Repository Base

`BaseRepository` extiende `JpaRepository` y `JpaSpecificationExecutor` con métodos adicionales.

### Métodos Disponibles

```java
public interface BaseRepository<T extends Base, ID>
        extends JpaRepository<T, ID>, JpaSpecificationExecutor<T> {

    // Métodos de JpaRepository heredados:
    // - findById(ID id)
    // - findAll()
    // - findAll(Pageable pageable)
    // - save(T entity)
    // - delete(T entity)
    // - count()
    // etc.

    // Métodos adicionales de Base:
    List<T> findByEstadoTrue();  // Solo registros activos
    List<T> findByEstadoFalse(); // Solo registros eliminados (soft)

    Optional<T> findByIdAndEstadoTrue(ID id);  // Por ID solo si activo

    long countByEstadoTrue();   // Contar activos
    long countByEstadoFalse();  // Contar eliminados
}
```

### Uso

```java
@Repository
public interface ProductRepository extends BaseRepository<Product, Long> {

    // Tus métodos personalizados
    List<Product> findByCategory(String category);

    @Query("SELECT p FROM Product p WHERE p.price BETWEEN :min AND :max AND p.estado = true")
    List<Product> findByPriceRange(@Param("min") BigDecimal min, @Param("max") BigDecimal max);
}
```

---

## 3. Servicio Base

`BaseServiceImpl` proporciona lógica CRUD completa con Result pattern, eventos y caché.

### Métodos Disponibles

```java
public abstract class BaseServiceImpl<T extends Base, ID> implements BaseService<T, ID> {

    // CRUD básico
    Result<T, Exception> findById(ID id);
    Result<List<T>, Exception> findAll();
    Result<Page<T>, Exception> findAll(Pageable pageable);
    Result<Page<T>, Exception> findAll(Specification<T> spec, Pageable pageable);
    Result<T, Exception> save(T entity);
    Result<T, Exception> update(ID id, T entity);
    Result<Void, Exception> delete(ID id);

    // Soft delete
    Result<Void, Exception> softDelete(ID id);
    Result<T, Exception> restore(ID id);

    // Búsqueda
    Result<Boolean, Exception> existsById(ID id);
    Result<Long, Exception> count();
    Result<Long, Exception> countBySpecification(Specification<T> spec);

    // Cursor pagination
    Result<CursorPage<T>, Exception> findAllWithCursor(
        String cursor, int size, String sortField, Sort.Direction direction);
}
```

### Uso

```java
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

    // Override para lógica personalizada
    @Override
    @Transactional
    public Result<Product, Exception> save(Product product) {
        // Validación de negocio
        if (product.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            return Result.failure(new IllegalArgumentException("Price must be positive"));
        }
        return super.save(product);
    }
}
```

---

## 4. Controller Base

`BaseControllerImpl` proporciona endpoints REST completos con HATEOAS.

### Endpoints Automáticos

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| `GET` | `/` | Listar con paginación y filtrado |
| `GET` | `/{id}` | Obtener por ID (con ETag) |
| `HEAD` | `/` | Obtener conteo total |
| `HEAD` | `/{id}` | Verificar existencia |
| `POST` | `/` | Crear nuevo registro |
| `PUT` | `/{id}` | Actualización completa |
| `PATCH` | `/{id}` | Actualización parcial |
| `DELETE` | `/{id}` | Soft delete |
| `DELETE` | `/{id}?permanent=true` | Hard delete |
| `POST` | `/{id}/restore` | Restaurar eliminado |
| `GET` | `/cursor` | Paginación por cursor |

### Uso

```java
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
}
```

---

## 5. Filtrado Dinámico

Sistema de filtrado flexible vía query parameters.

### Sintaxis

```
GET /api/products?filter=campo:operador:valor
GET /api/products?filter=campo1:op1:val1,campo2:op2:val2
```

### Operadores Disponibles

| Operador | Descripción | Ejemplo |
|----------|-------------|---------|
| `eq` | Igual a | `category:eq:Electronics` |
| `neq` | Diferente de | `status:neq:deleted` |
| `like` | Contiene (case insensitive) | `name:like:laptop` |
| `starts` | Empieza con | `name:starts:Mac` |
| `ends` | Termina con | `name:ends:Pro` |
| `gt` | Mayor que | `price:gt:100` |
| `gte` | Mayor o igual | `stock:gte:10` |
| `lt` | Menor que | `price:lt:1000` |
| `lte` | Menor o igual | `stock:lte:50` |
| `in` | En lista | `category:in:Electronics;Accessories` |
| `between` | Entre valores | `price:between:100;500` |
| `null` | Es nulo | `description:null` |
| `notnull` | No es nulo | `sku:notnull` |

### Ejemplos

```bash
# Filtro simple
GET /api/products?filter=category:eq:Electronics

# Múltiples filtros (AND)
GET /api/products?filter=category:eq:Electronics,price:lte:500

# Búsqueda por texto
GET /api/products?filter=name:like:laptop

# Rango de valores
GET /api/products?filter=price:between:100;500

# Lista de valores
GET /api/products?filter=status:in:ACTIVE;PENDING;PROCESSING

# Combinar con paginación
GET /api/products?filter=category:eq:Electronics&page=0&size=10&sort=price,desc
```

### Sparse Fieldsets

Seleccionar solo campos específicos en la respuesta:

```bash
GET /api/products?fields=id,name,price
```

Respuesta:
```json
[
  {"id": 1, "name": "Laptop", "price": 999.99},
  {"id": 2, "name": "Mouse", "price": 29.99}
]
```

---

## 6. Paginación

### Paginación Tradicional (Offset)

```bash
GET /api/products?page=0&size=10&sort=name,asc
GET /api/products?page=2&size=20&sort=price,desc&sort=name,asc
```

Respuesta:
```json
{
  "_embedded": {
    "products": [...]
  },
  "_links": {
    "first": {"href": "/api/products?page=0&size=10"},
    "prev": {"href": "/api/products?page=1&size=10"},
    "self": {"href": "/api/products?page=2&size=10"},
    "next": {"href": "/api/products?page=3&size=10"},
    "last": {"href": "/api/products?page=9&size=10"}
  },
  "page": {
    "size": 10,
    "totalElements": 100,
    "totalPages": 10,
    "number": 2
  }
}
```

### Paginación por Cursor (Keyset)

Más eficiente para grandes datasets:

```bash
GET /api/products/cursor?size=10&sort=id&direction=DESC
GET /api/products/cursor?cursor=eyJpZCI6MTAwfQ&size=10&sort=id&direction=DESC
```

Respuesta:
```json
{
  "content": [...],
  "cursor": "eyJpZCI6OTB9",  // Base64 del último registro
  "hasNext": true,
  "hasPrevious": true,
  "size": 10
}
```

**Ventajas del cursor:**
- O(1) en lugar de O(n) para páginas grandes
- No se pierden registros si se insertan durante la navegación
- Ideal para infinite scroll

---

## 7. HATEOAS

Hypermedia As The Engine Of Application State - links automáticos en respuestas.

### Configuración

```java
@Component
public class ProductResourceAssembler extends BaseResourceAssembler<ProductDTO, Long> {

    public ProductResourceAssembler() {
        super(ProductController.class);
    }

    // Override para agregar links personalizados
    @Override
    public EntityModel<ProductDTO> toModel(ProductDTO dto) {
        EntityModel<ProductDTO> model = super.toModel(dto);

        // Agregar links adicionales
        model.add(Link.of("/api/products/" + dto.id() + "/reviews", "reviews"));
        model.add(Link.of("/api/categories/" + dto.categoryId(), "category"));

        return model;
    }
}
```

### Respuesta con Links

```json
{
  "id": 1,
  "name": "Laptop",
  "price": 999.99,
  "_links": {
    "self": {"href": "/api/products/1"},
    "collection": {"href": "/api/products"},
    "update": {"href": "/api/products/1"},
    "delete": {"href": "/api/products/1"},
    "reviews": {"href": "/api/products/1/reviews"},
    "category": {"href": "/api/categories/5"}
  }
}
```

---

## 8. ETag y Caché

### ETag para Validación de Caché

El controller genera ETags automáticamente basados en el hash del contenido.

```bash
# Primera petición
GET /api/products/1
# Response headers:
# ETag: "abc123"

# Peticiones siguientes
GET /api/products/1
If-None-Match: "abc123"
# Response: 304 Not Modified (si no cambió)
```

### Optimistic Locking

Prevenir conflictos de actualización concurrente:

```bash
# Obtener producto
GET /api/products/1
# ETag: "abc123"

# Actualizar con If-Match
PUT /api/products/1
If-Match: "abc123"
Content-Type: application/json

{"name": "Laptop Pro", "price": 1099.99}

# Si otro usuario modificó el recurso:
# Response: 412 Precondition Failed
```

### Configuración de Caché

```yaml
app:
  cache:
    entities:
      max-size: 1000
      expire-after-write: 10m
    lists:
      max-size: 500
      expire-after-write: 5m
```

---

## 9. Soft Delete

Los registros no se eliminan físicamente, solo se marcan como inactivos.

### Cómo Funciona

```bash
# Soft delete (por defecto)
DELETE /api/products/1
# estado = false, fechaEliminacion = now(), eliminadoPor = currentUser

# Hard delete (eliminación permanente)
DELETE /api/products/1?permanent=true
# Registro eliminado de la BD

# Restaurar
POST /api/products/1/restore
# estado = true, fechaEliminacion = null, eliminadoPor = null
```

### Filtrado Automático

Por defecto, las consultas solo devuelven registros activos (`estado = true`).

```java
// En el repository
List<Product> findByEstadoTrue();  // Solo activos
List<Product> findByEstadoFalse(); // Solo eliminados (soft)

// En el servicio - findAll ya filtra por estado = true
```

### Incluir Eliminados

Para consultas que necesitan incluir eliminados:

```java
@Query("SELECT p FROM Product p")  // Sin filtro de estado
List<Product> findAllIncludingDeleted();
```

---

## 10. Auditoría

### Campos de Auditoría

| Campo | Cuándo se actualiza |
|-------|---------------------|
| `fechaCreacion` | Al crear (una vez) |
| `fechaActualizacion` | En cada update |
| `creadoPor` | Al crear (una vez) |
| `modificadoPor` | En cada update |
| `fechaEliminacion` | Al soft delete |
| `eliminadoPor` | Al soft delete |

### Configuración del Usuario

APiGen usa `AuditorAware` para obtener el usuario actual:

```java
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaConfig {

    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> Optional.ofNullable(SecurityContextHolder.getContext())
            .map(SecurityContext::getAuthentication)
            .filter(Authentication::isAuthenticated)
            .map(Authentication::getName)
            .or(() -> Optional.of("system"));
    }
}
```

### Respuesta con Auditoría

```json
{
  "id": 1,
  "name": "Laptop",
  "price": 999.99,
  "activo": true,
  "fechaCreacion": "2024-01-15T10:30:00",
  "fechaActualizacion": "2024-01-16T14:20:00",
  "creadoPor": "admin",
  "modificadoPor": "john.doe"
}
```

---

## 11. Validación

### Validaciones en DTO

```java
public record ProductDTO(
    Long id,
    Boolean activo,

    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 3, max = 255, message = "El nombre debe tener entre 3 y 255 caracteres")
    String name,

    @Size(max = 1000, message = "La descripción no puede exceder 1000 caracteres")
    String description,

    @NotNull(message = "El precio es obligatorio")
    @Positive(message = "El precio debe ser positivo")
    @Digits(integer = 8, fraction = 2, message = "Formato de precio inválido")
    BigDecimal price,

    @PositiveOrZero(message = "El stock no puede ser negativo")
    Integer stock,

    @Email(message = "Email inválido")
    String contactEmail,

    @Pattern(regexp = "^[A-Z]{2,3}-\\d{4,6}$", message = "SKU inválido")
    String sku
) implements BaseDTO { }
```

### Respuesta de Error de Validación

```json
{
  "timestamp": "2024-01-15T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "errors": [
    {
      "field": "name",
      "message": "El nombre es obligatorio",
      "rejectedValue": null
    },
    {
      "field": "price",
      "message": "El precio debe ser positivo",
      "rejectedValue": -10
    }
  ]
}
```

### Validación de Negocio en Servicio

```java
@Override
@Transactional
public Result<Product, Exception> save(Product product) {
    // Validación de negocio
    if (productRepository.existsBySku(product.getSku())) {
        return Result.failure(new BusinessException("SKU already exists"));
    }

    if (product.getPrice().compareTo(product.getCost()) < 0) {
        return Result.failure(new BusinessException("Price cannot be less than cost"));
    }

    return super.save(product);
}
```

---

## 12. Eventos de Dominio

APiGen publica eventos automáticamente en operaciones CRUD.

### Eventos Disponibles

| Evento | Cuándo se publica |
|--------|-------------------|
| `EntityCreatedEvent` | Después de crear |
| `EntityUpdatedEvent` | Después de actualizar |
| `EntityDeletedEvent` | Después de soft/hard delete |
| `EntityRestoredEvent` | Después de restaurar |

### Escuchar Eventos

```java
@Component
@Slf4j
public class ProductEventListener {

    @EventListener
    public void handleProductCreated(EntityCreatedEvent<Product> event) {
        Product product = event.getEntity();
        log.info("Product created: {}", product.getName());

        // Notificar, indexar en Elasticsearch, enviar email, etc.
    }

    @EventListener
    public void handleProductUpdated(EntityUpdatedEvent<Product> event) {
        Product product = event.getEntity();
        log.info("Product updated: {}", product.getName());
    }

    @EventListener
    @Async  // Procesar de forma asíncrona
    public void handleProductDeleted(EntityDeletedEvent<Product> event) {
        Long productId = event.getEntityId();
        log.info("Product deleted: {}", productId);

        // Limpiar caché, actualizar índices, etc.
    }
}
```

### Eventos Personalizados

```java
// Definir evento
public record LowStockEvent(Product product, int currentStock, int threshold) {}

// Publicar en servicio
@Override
public Result<Product, Exception> update(Long id, Product product) {
    Result<Product, Exception> result = super.update(id, product);

    result.peek(p -> {
        if (p.getStock() < 10) {
            eventPublisher.publishEvent(new LowStockEvent(p, p.getStock(), 10));
        }
    });

    return result;
}
```

---

## 13. Result Pattern

APiGen usa el pattern Result para manejo de errores funcional (usando Vavr).

### Concepto

```java
Result<T, Exception>
├── Success(value)   // Operación exitosa con valor
└── Failure(error)   // Operación fallida con excepción
```

### Uso en Servicio

```java
// Crear resultado exitoso
return Result.success(entity);

// Crear resultado fallido
return Result.failure(new EntityNotFoundException("Product not found"));

// Operaciones funcionales
Result<Product, Exception> result = findById(id);

// Map - transformar el valor si es Success
Result<ProductDTO, Exception> dtoResult = result.map(productMapper::toDTO);

// FlatMap - encadenar operaciones que retornan Result
Result<Order, Exception> orderResult = findById(productId)
    .flatMap(product -> createOrder(product));

// Peek - efectos secundarios
result.peek(product -> log.info("Found: {}", product));

// Recover - manejar errores
Product product = result.getOrElse(defaultProduct);

// Fold - manejar ambos casos
ResponseEntity<?> response = result.fold(
    error -> ResponseEntity.status(500).body(error.getMessage()),
    product -> ResponseEntity.ok(product)
);
```

### En Controller

```java
@GetMapping("/{id}")
public ResponseEntity<ProductDTO> getById(@PathVariable Long id) {
    return service.findById(id)
        .map(mapper::toDTO)
        .map(dto -> ResponseEntity.ok(dto))
        .getOrElseGet(error -> {
            if (error instanceof EntityNotFoundException) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.internalServerError().build();
        });
}
```

---

## 14. Rate Limiting

Protección contra abuso de la API.

### Configuración

```yaml
app:
  rate-limit:
    max-requests: 100      # Requests por ventana
    window-seconds: 60     # Ventana de tiempo

resilience4j:
  ratelimiter:
    instances:
      default:
        limitForPeriod: 100
        limitRefreshPeriod: 1m
        timeoutDuration: 0s
      strict:
        limitForPeriod: 10
        limitRefreshPeriod: 1s
        timeoutDuration: 0s
```

### Aplicar en Controller

```java
@GetMapping
@RateLimiter(name = "default")
public ResponseEntity<Page<ProductDTO>> getAll(...) {
    // ...
}

@PostMapping
@RateLimiter(name = "strict")  // Más restrictivo para escrituras
public ResponseEntity<ProductDTO> create(...) {
    // ...
}
```

### Headers de Respuesta

```
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 95
X-RateLimit-Reset: 1705312800
```

### Respuesta cuando se excede

```json
{
  "status": 429,
  "error": "Too Many Requests",
  "message": "Rate limit exceeded. Try again in 45 seconds."
}
```

---

## 15. Seguridad JWT

Módulo `apigen-security` proporciona autenticación JWT completa.

### Endpoints de Auth

| Endpoint | Método | Descripción |
|----------|--------|-------------|
| `/auth/login` | POST | Login con username/password |
| `/auth/refresh` | POST | Refrescar access token |
| `/auth/logout` | POST | Invalidar tokens |
| `/auth/me` | GET | Obtener usuario actual |

### Login

```bash
POST /auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "password123"
}
```

Respuesta:
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
  "tokenType": "Bearer",
  "expiresIn": 900
}
```

### Usar Token

```bash
GET /api/products
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
```

### Refresh Token

```bash
POST /auth/refresh
Content-Type: application/json

{
  "refreshToken": "eyJhbGciOiJIUzI1NiIs..."
}
```

### Configuración

```yaml
apigen:
  security:
    enabled: true
    jwt:
      secret: ${JWT_SECRET}
      expiration-minutes: 15
      refresh-expiration-minutes: 10080  # 7 días
      issuer: my-api
    public-paths:
      - /actuator/**
      - /swagger-ui/**
      - /v3/api-docs/**
```

---

## 16. OpenAPI/Swagger

Documentación automática de la API.

### Acceso

- **Swagger UI:** `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON:** `http://localhost:8080/v3/api-docs`
- **OpenAPI YAML:** `http://localhost:8080/v3/api-docs.yaml`

### Personalizar Documentación

```java
@RestController
@RequestMapping("/api/products")
@Tag(name = "Products", description = "API de gestión de productos")
public class ProductController {

    @Operation(
        summary = "Obtener producto por ID",
        description = "Retorna un producto con sus detalles completos"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Producto encontrado"),
        @ApiResponse(responseCode = "404", description = "Producto no encontrado"),
        @ApiResponse(responseCode = "500", description = "Error interno")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ProductDTO> getById(
        @Parameter(description = "ID del producto", required = true)
        @PathVariable Long id
    ) {
        // ...
    }
}
```

### Configuración

```yaml
springdoc:
  api-docs:
    path: /v3/api-docs
    enabled: true
  swagger-ui:
    path: /swagger-ui.html
    enabled: true
    tags-sorter: alpha
    operations-sorter: method
  default-produces-media-type: application/json
  default-consumes-media-type: application/json

  # Información del API
  info:
    title: Mi API
    version: 1.0.0
    description: API REST para gestión de productos
    contact:
      name: Soporte
      email: soporte@miempresa.com
```

---

## Resumen de Características por Módulo

| Característica | apigen-core | apigen-security |
|----------------|:-----------:|:---------------:|
| Entidad Base | ✅ | - |
| Repository Base | ✅ | - |
| Servicio Base | ✅ | - |
| Controller Base | ✅ | - |
| Filtrado Dinámico | ✅ | - |
| Paginación | ✅ | - |
| HATEOAS | ✅ | - |
| ETag/Caché | ✅ | - |
| Soft Delete | ✅ | - |
| Auditoría | ✅ | ✅ |
| Validación | ✅ | ✅ |
| Eventos | ✅ | ✅ |
| Result Pattern | ✅ | ✅ |
| Rate Limiting | ✅ | - |
| JWT Auth | - | ✅ |
| OAuth2 | - | ✅ |
| OpenAPI/Swagger | ✅ | ✅ |

---

## 17. Feature Flags

Sistema de feature flags usando Togglz para habilitar/deshabilitar funcionalidades en runtime.

### Features Disponibles

```java
public enum ApigenFeatures implements Feature {
    @Label("Habilitar caché distribuido Redis")
    DISTRIBUTED_CACHE,

    @Label("Habilitar métricas detalladas")
    DETAILED_METRICS,

    @Label("Habilitar detección de queries N+1")
    N_PLUS_ONE_DETECTION,

    @Label("Habilitar operaciones batch")
    BATCH_OPERATIONS,

    @Label("Habilitar eventos de dominio")
    @EnabledByDefault
    DOMAIN_EVENTS
}
```

### Uso en Código

```java
@Service
public class ProductService {

    private final FeatureChecker featureChecker;

    public void processProducts(List<Product> products) {
        if (featureChecker.isEnabled(ApigenFeatures.BATCH_OPERATIONS)) {
            // Usar procesamiento batch optimizado
            batchService.processAsync(products);
        } else {
            // Procesamiento tradicional
            products.forEach(this::process);
        }
    }
}
```

### Configuración

```yaml
apigen:
  features:
    enabled: true  # Habilita el sistema de feature flags

togglz:
  features:
    DISTRIBUTED_CACHE:
      enabled: true
    DETAILED_METRICS:
      enabled: false
```

### Consola Web

Accede a `/togglz-console` para gestionar features en runtime (requiere autenticación admin).

---

## 18. Redis Cache Distribuido

Caché distribuido como alternativa a Caffeine para entornos multi-instancia.

### Configuración

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      password: ${REDIS_PASSWORD:}

apigen:
  cache:
    type: redis  # 'caffeine' (default) o 'redis'
    redis:
      ttl: 10m
      key-prefix: "apigen:"
```

### Características

- **Serialización JSON** para compatibilidad
- **TTL configurable** por tipo de caché
- **Prefijos de clave** para evitar colisiones
- **Fallback a Caffeine** si Redis no está disponible

### Uso Programático

```java
@Service
public class ProductService {

    @Cacheable(value = "products", key = "#id")
    public Product findById(Long id) {
        return repository.findById(id).orElseThrow();
    }

    @CacheEvict(value = "products", key = "#product.id")
    public Product update(Product product) {
        return repository.save(product);
    }

    @CacheEvict(value = "products", allEntries = true)
    public void clearCache() {
        // Limpia todo el caché de productos
    }
}
```

---

## 19. Métricas HikariCP

Exporta métricas del pool de conexiones a Prometheus para monitoreo.

### Métricas Disponibles

| Métrica | Descripción |
|---------|-------------|
| `hikaricp_connections_active` | Conexiones activas |
| `hikaricp_connections_idle` | Conexiones idle |
| `hikaricp_connections_pending` | Conexiones pendientes |
| `hikaricp_connections_timeout_total` | Timeouts totales |
| `hikaricp_connections_acquire_seconds` | Tiempo de adquisición |
| `hikaricp_connections_usage_seconds` | Tiempo de uso |
| `hikaricp_connections_creation_seconds` | Tiempo de creación |

### Configuración

```yaml
spring:
  datasource:
    hikari:
      pool-name: APiGenPool
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      register-mbeans: true  # Para JMX

management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

### Dashboard Grafana

Importa el dashboard incluido en `docker/grafana/dashboards/hikaricp.json` para visualizar:
- Uso del pool en tiempo real
- Tiempos de espera de conexión
- Alertas de pool exhausted

---

## 20. Detección N+1

Detecta queries N+1 automáticamente usando Hibernate Statistics.

### Configuración

```yaml
apigen:
  query-analysis:
    enabled: true
    warn-threshold: 5      # Advertencia si > 5 queries similares
    error-threshold: 10    # Error si > 10 queries similares
    log-slow-queries: true
    slow-query-threshold: 100ms

spring:
  jpa:
    properties:
      hibernate:
        generate_statistics: true
```

### Logs de Advertencia

```
WARN  QueryAnalysis - Possible N+1 detected: 15 similar SELECT queries for entity 'Product'
WARN  QueryAnalysis - Consider using JOIN FETCH or @EntityGraph
```

### Assertions en Tests

```java
@Test
void findAllProducts_shouldNotCauseNPlusOne() {
    QueryAssertions.assertMaxQueries(3, () -> {
        List<Product> products = productService.findAll();
        products.forEach(p -> p.getCategory().getName()); // Trigger lazy load
    });
}
```

### Soluciones Comunes

```java
// 1. JOIN FETCH en JPQL
@Query("SELECT p FROM Product p JOIN FETCH p.category")
List<Product> findAllWithCategory();

// 2. EntityGraph
@EntityGraph(attributePaths = {"category", "tags"})
List<Product> findAll();

// 3. Batch fetching
@BatchSize(size = 25)
@OneToMany(mappedBy = "product")
private List<Review> reviews;
```

---

## 21. Batch Operations

Procesamiento batch asíncrono usando Virtual Threads (Java 21+).

### Interfaz BatchService

```java
public interface BatchService {

    /**
     * Procesa items en paralelo usando Virtual Threads.
     */
    <T, R> CompletableFuture<List<R>> processAsync(
        List<T> items,
        Function<T, R> processor
    );

    /**
     * Procesa items en lotes con tamaño configurable.
     */
    <T, R> CompletableFuture<List<R>> processBatchAsync(
        List<T> items,
        Function<List<T>, List<R>> batchProcessor,
        int batchSize
    );

    /**
     * Procesa items con rate limiting.
     */
    <T, R> CompletableFuture<List<R>> processWithRateLimit(
        List<T> items,
        Function<T, R> processor,
        int maxPerSecond
    );
}
```

### Uso

```java
@Service
public class ProductImportService {

    private final BatchService batchService;

    public CompletableFuture<ImportResult> importProducts(List<ProductDTO> dtos) {
        return batchService.processAsync(dtos, dto -> {
            Product product = mapper.toEntity(dto);
            return repository.save(product);
        }).thenApply(products -> new ImportResult(products.size(), 0));
    }

    public CompletableFuture<List<Product>> updatePrices(
            List<Long> productIds,
            BigDecimal percentage
    ) {
        return batchService.processBatchAsync(
            productIds,
            batch -> {
                List<Product> products = repository.findAllById(batch);
                products.forEach(p -> p.setPrice(
                    p.getPrice().multiply(BigDecimal.ONE.add(percentage))
                ));
                return repository.saveAll(products);
            },
            100  // Batch size
        );
    }
}
```

### Configuración

```yaml
apigen:
  batch:
    enabled: true
    default-batch-size: 100
    max-concurrent: 50
    timeout: 30s
```

### Características

- **Virtual Threads** para máxima concurrencia
- **Backpressure** automático
- **Retry** con exponential backoff
- **Métricas** de procesamiento batch
- **Cancelación** de operaciones en progreso

---

## Resumen de Características por Módulo

| Característica | apigen-core | apigen-security |
|----------------|:-----------:|:---------------:|
| Entidad Base | ✅ | - |
| Repository Base | ✅ | - |
| Servicio Base | ✅ | - |
| Controller Base | ✅ | - |
| Filtrado Dinámico | ✅ | - |
| Paginación | ✅ | - |
| HATEOAS | ✅ | - |
| ETag/Caché | ✅ | - |
| Soft Delete | ✅ | - |
| Auditoría | ✅ | ✅ |
| Validación | ✅ | ✅ |
| Eventos | ✅ | ✅ |
| Result Pattern | ✅ | ✅ |
| Rate Limiting | ✅ | ✅ |
| JWT Auth | - | ✅ |
| OAuth2 | - | ✅ |
| OpenAPI/Swagger | ✅ | ✅ |
| Feature Flags | ✅ | - |
| Redis Cache | ✅ | - |
| HikariCP Metrics | ✅ | - |
| N+1 Detection | ✅ | - |
| Batch Operations | ✅ | - |

---

## Ver También

- [USAGE_GUIDE.md](USAGE_GUIDE.md) - Cómo usar APiGen
- [USAGE_GUIDE_LIBRARY.md](USAGE_GUIDE_LIBRARY.md) - Guía detallada de la librería
- [C4_ARCHITECTURE.md](architecture/C4_ARCHITECTURE.md) - Diagramas de arquitectura
- [apigen-example README](../apigen-example/README.md) - Ejemplos prácticos
