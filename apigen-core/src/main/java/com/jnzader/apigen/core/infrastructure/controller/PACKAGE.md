# Controllers

Este paquete contiene el **controlador REST base** que implementa endpoints CRUD genéricos con HATEOAS.

## Archivos

| Archivo | Descripción |
|---------|-------------|
| `BaseController.java` | Interface con endpoints CRUD |
| `BaseControllerImpl.java` | Implementación con HATEOAS, ETags, paginación |

## BaseController Interface

```java
public interface BaseController<D extends BaseDTO, ID> {

    // Lectura
    ResponseEntity<PagedModel<EntityModel<D>>> findAll(Pageable pageable);
    ResponseEntity<EntityModel<D>> findById(ID id);

    // Escritura
    ResponseEntity<EntityModel<D>> create(D dto);
    ResponseEntity<EntityModel<D>> update(ID id, D dto);
    ResponseEntity<EntityModel<D>> partialUpdate(ID id, D dto);

    // Eliminación
    ResponseEntity<Void> delete(ID id);
}
```

## Características de BaseControllerImpl

### 1. HATEOAS

Cada respuesta incluye links de navegación:

```json
{
  "id": 1,
  "name": "Product",
  "_links": {
    "self": { "href": "/api/v1/products/1" },
    "products": { "href": "/api/v1/products" },
    "update": { "href": "/api/v1/products/1" },
    "delete": { "href": "/api/v1/products/1" }
  }
}
```

### 2. ETags

Cache HTTP con validación condicional:

```java
// GET - Genera ETag
@GetMapping("/{id}")
public ResponseEntity<EntityModel<D>> findById(@PathVariable ID id) {
    return service.findById(id)
        .map(mapper::toDto)
        .map(dto -> {
            String etag = generateETag(dto);
            return ResponseEntity.ok()
                .eTag(etag)
                .body(EntityModel.of(dto));
        })
        .fold(...);
}
```

**Request condicional:**
```http
GET /api/v1/products/1
If-None-Match: "abc123"

→ 304 Not Modified (si no cambió)
→ 200 OK + nuevo ETag (si cambió)
```

### 3. Optimistic Locking

Control de concurrencia con If-Match:

```java
@PutMapping("/{id}")
public ResponseEntity<EntityModel<D>> update(
        @PathVariable ID id,
        @RequestHeader(value = "If-Match", required = false) String ifMatch,
        @RequestBody D dto) {

    // Verificar versión antes de actualizar
    if (ifMatch != null && !matchesVersion(id, ifMatch)) {
        throw new PreconditionFailedException();
    }
    // ...
}
```

**Request:**
```http
PUT /api/v1/products/1
If-Match: "5"

→ 200 OK (si versión es 5)
→ 412 Precondition Failed (si versión cambió)
```

### 4. Paginación

```java
@GetMapping
public ResponseEntity<PagedModel<EntityModel<D>>> findAll(
        @PageableDefault(size = 20, sort = "id") Pageable pageable) {

    return service.findAll(pageable)
        .map(page -> page.map(mapper::toDto))
        .map(page -> pagedResourcesAssembler.toModel(page, assembler))
        .fold(ResponseEntity::ok, ...);
}
```

**Request:**
```http
GET /api/v1/products?page=0&size=20&sort=name,asc
```

**Response:**
```json
{
  "_embedded": {
    "products": [...]
  },
  "_links": {
    "first": { "href": "...?page=0&size=20" },
    "self": { "href": "...?page=0&size=20" },
    "next": { "href": "...?page=1&size=20" },
    "last": { "href": "...?page=5&size=20" }
  },
  "page": {
    "size": 20,
    "totalElements": 100,
    "totalPages": 5,
    "number": 0
  }
}
```

### 5. Validación con Grupos

```java
@PostMapping
public ResponseEntity<EntityModel<D>> create(
        @Validated(OnCreate.class) @RequestBody D dto) {
    // Solo validaciones de OnCreate
}

@PutMapping("/{id}")
public ResponseEntity<EntityModel<D>> update(
        @PathVariable ID id,
        @Validated(OnUpdate.class) @RequestBody D dto) {
    // Solo validaciones de OnUpdate
}
```

### 6. Location Header en Create

```java
@PostMapping
public ResponseEntity<EntityModel<D>> create(@RequestBody D dto) {
    return service.save(mapper.toEntity(dto))
        .map(mapper::toDto)
        .map(saved -> {
            URI location = linkTo(methodOn(getClass()).findById(saved.getId())).toUri();
            return ResponseEntity
                .created(location)  // 201 Created + Location header
                .body(EntityModel.of(saved));
        })
        .fold(...);
}
```

## Crear Controlador Específico

### 1. Interface

```java
@Tag(name = "Products", description = "Product management API")
@RequestMapping("/api/v1/products")
public interface ProductController extends BaseController<ProductDTO, Long> {

    // Endpoints adicionales
    @Operation(summary = "Find by SKU")
    @GetMapping("/sku/{sku}")
    ResponseEntity<EntityModel<ProductDTO>> findBySku(@PathVariable String sku);

    @Operation(summary = "Update stock")
    @PatchMapping("/{id}/stock")
    ResponseEntity<EntityModel<ProductDTO>> updateStock(
        @PathVariable Long id,
        @RequestParam int delta);
}
```

### 2. Implementación

```java
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
            .map(dto -> EntityModel.of(dto, createSelfLink(dto.getId())))
            .fold(
                ResponseEntity::ok,
                error -> { throw mapException(error); }
            );
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EntityModel<ProductDTO>> updateStock(Long id, int delta) {
        return productService.updateStock(id, delta)
            .map(productMapper::toDto)
            .map(dto -> EntityModel.of(dto))
            .fold(
                ResponseEntity::ok,
                error -> { throw mapException(error); }
            );
    }
}
```

## Códigos HTTP Retornados

| Operación | Éxito | Errores Comunes |
|-----------|-------|-----------------|
| GET /{id} | 200 OK | 404 Not Found |
| GET / | 200 OK | - |
| POST | 201 Created | 400 Bad Request, 409 Conflict |
| PUT | 200 OK | 400, 404, 409, 412 |
| PATCH | 200 OK | 400, 404, 409, 412 |
| DELETE | 204 No Content | 404 Not Found |

## Testing

```java
@WebMvcTest(ProductControllerImpl.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    @MockBean
    private ProductMapper productMapper;

    @Test
    void shouldReturn201WhenCreatingProduct() throws Exception {
        ProductDTO dto = ProductDTO.builder().name("Test").build();
        Product entity = new Product();
        entity.setId(1L);

        when(productMapper.toEntity(any())).thenReturn(entity);
        when(productService.save(any())).thenReturn(Result.success(entity));
        when(productMapper.toDto(any())).thenReturn(dto);

        mockMvc.perform(post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isCreated())
            .andExpect(header().exists("Location"));
    }
}
```

## Buenas Prácticas

1. **Controllers delgados** - Solo orquestación, lógica en servicios
2. **Siempre HATEOAS** - Links en todas las respuestas
3. **Documentar con OpenAPI** - @Operation, @ApiResponse
4. **Validar entrada** - @Validated con grupos
5. **Códigos HTTP correctos** - 201 para create, 204 para delete
