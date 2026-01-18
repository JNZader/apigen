# 04 - Infraestructura Base: Controllers, HATEOAS y Filtrado

## Objetivo

Entender la capa de infraestructura que es la fachada REST del sistema. Este documento cubre los componentes del modulo `apigen-core`:

- BaseController con operaciones CRUD completas
- GlobalExceptionHandler con RFC 7807
- HATEOAS con BaseResourceAssembler
- Filtrado dinamico con JPA Specifications

## Ubicacion en el Proyecto Multi-Modulo

```
apigen/
├── apigen-core/                      # <-- Este modulo contiene la infraestructura base
│   └── src/main/java/com/jnzader/apigen/core/
│       └── infrastructure/
│           ├── controller/
│           │   ├── BaseController.java
│           │   └── BaseControllerImpl.java
│           ├── exception/
│           │   ├── GlobalExceptionHandler.java
│           │   └── ApiError.java
│           ├── hateoas/
│           │   └── BaseResourceAssembler.java
│           ├── config/
│           │   └── ApigenCoreAutoConfiguration.java
│           └── filter/
│               ├── RequestLoggingFilter.java
│               └── RateLimitingFilter.java
│
└── apigen-example/                   # Ejemplo de uso
    └── src/main/java/com/jnzader/example/
        └── infrastructure/
            ├── controller/ProductController.java
            └── hateoas/ProductResourceAssembler.java
```

## Formas de Usar la Infraestructura Base

### Opcion 1: Como Dependencia (Recomendado)

```groovy
dependencies {
    implementation platform('com.jnzader:apigen-bom:1.0.0-SNAPSHOT')
    implementation 'com.jnzader:apigen-core'
}
```

```java
// Tu controller extiende BaseControllerImpl
import com.jnzader.apigen.core.infrastructure.controller.BaseControllerImpl;

@RestController
@RequestMapping("/api/products")
public class ProductController extends BaseControllerImpl<Product, ProductDTO, Long> {
    // Heredas 12+ endpoints CRUD automaticamente
}
```

### Opcion 2: Explorar el Codigo Fuente

```bash
# Ver el BaseController
cat apigen-core/src/main/java/com/jnzader/apigen/core/infrastructure/controller/BaseController.java

# Ver el GlobalExceptionHandler
cat apigen-core/src/main/java/com/jnzader/apigen/core/infrastructure/exception/GlobalExceptionHandler.java
```

---

## Introduccion

La **capa de infraestructura** en APiGen proporciona los componentes técnicos necesarios para exponer la funcionalidad del dominio como una API REST robusta y profesional. Esta capa implementa:

- **Controladores base genéricos** con operaciones CRUD completas
- **Manejo centralizado de excepciones** con RFC 7807
- **Configuraciones Spring** para JPA, Web, OpenAPI y Async
- **Filtros HTTP** para logging, rate limiting y correlación
- **HATEOAS** para navegabilidad de recursos
- **Filtrado dinámico** mediante Specifications JPA

### Características RESTful Implementadas

- ✅ **Location header** en POST (201 Created)
- ✅ **ETag** para cache condicional (If-None-Match → 304)
- ✅ **Concurrencia optimista** (If-Match → 412)
- ✅ **Validación de ID** en PUT
- ✅ **HATEOAS** en respuestas
- ✅ **Filtrado dinámico** via query params
- ✅ **Sparse fieldsets** (selección de campos)
- ✅ **Paginación** estándar
- ✅ **Soft delete** con opción de hard delete
- ✅ **Problem Details** (RFC 7807)

---

## BaseController y BaseControllerImpl

### 1. Interfaz BaseController

Define el contrato para controladores REST genéricos con operaciones CRUD estándar.

**Ubicacion:** `apigen-core/src/main/java/com/jnzader/apigen/core/infrastructure/controller/BaseController.java`

```java
public interface BaseController<D extends BaseDTO, ID extends Serializable> {

    /**
     * Lista recursos con paginación, filtrado dinámico y selección de campos.
     *
     * Ejemplos:
     * GET /?page=0&size=20&sort=id,desc
     * GET /?filter=nombre:like:Juan,estado:eq:true
     * GET /?filter=fechaCreacion:between:2024-01-01;2024-12-31&fields=id,nombre
     */
    ResponseEntity<?> findAll(
            @RequestParam(required = false) String filter,
            @RequestParam(required = false) Map<String, String> filters,
            @RequestParam(required = false) Set<String> fields,
            Pageable pageable
    );

    /**
     * Obtiene el conteo de recursos.
     * HEAD / → X-Total-Count header
     */
    ResponseEntity<Void> count(@RequestParam(required = false) Map<String, String> filters);

    /**
     * Obtiene un recurso por ID con soporte para ETag.
     * GET /{id}
     */
    ResponseEntity<?> findById(
            @PathVariable ID id,
            @RequestParam(required = false) Set<String> fields,
            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch
    );

    /**
     * Verifica existencia de un recurso.
     * HEAD /{id} → 200 si existe, 404 si no
     */
    ResponseEntity<Void> existsById(@PathVariable ID id);

    /**
     * Crea un nuevo recurso.
     * POST / → 201 Created + Location header
     */
    ResponseEntity<?> save(@Valid @RequestBody D dto);

    /**
     * Actualiza completamente un recurso.
     * PUT /{id} - Valida ID y soporta concurrencia optimista con If-Match
     */
    ResponseEntity<?> update(
            @PathVariable ID id,
            @Valid @RequestBody D dto,
            @RequestHeader(value = "If-Match", required = false) String ifMatch
    );

    /**
     * Actualiza parcialmente un recurso.
     * PATCH /{id}
     */
    ResponseEntity<?> partialUpdate(
            @PathVariable ID id,
            @RequestBody D dto,
            @RequestHeader(value = "If-Match", required = false) String ifMatch
    );

    /**
     * Elimina un recurso (soft delete por defecto).
     * DELETE /{id}?permanent=true para eliminación permanente
     */
    ResponseEntity<Void> delete(
            @PathVariable ID id,
            @RequestParam(required = false, defaultValue = "false") boolean permanent
    );

    /**
     * Restaura un recurso eliminado con soft delete.
     * POST /{id}/restore
     */
    ResponseEntity<?> restore(@PathVariable ID id);
}
```

### 2. Implementación BaseControllerImpl

Implementación completa con todas las características RESTful.

**Características destacadas:**

#### A. GET / - Listar con Paginación y Filtrado

```java
@Override
@GetMapping("")
@Operation(summary = "Listar recursos", description = """
    Lista recursos con paginación, filtrado dinámico y selección de campos.

    Operadores soportados:
    - eq: Igual (=)
    - neq: No igual (!=)
    - like: Contiene (LIKE %value%)
    - starts: Empieza con
    - ends: Termina con
    - gt/gte: Mayor / Mayor o igual
    - lt/lte: Menor / Menor o igual
    - in: En lista (valores separados por ;)
    - between: Entre dos valores (v1;v2)
    - null/notnull: Es nulo / No es nulo
    """)
public ResponseEntity<?> findAll(
        @RequestParam(required = false) String filter,
        @RequestParam(required = false) Map<String, String> filters,
        @RequestParam(required = false) Set<String> fields,
        Pageable pageable) {

    // Construir specification desde filtros
    Specification<E> spec = Specification.where((root, query, cb) -> cb.conjunction());

    if (filter != null && !filter.isBlank()) {
        spec = spec.and(filterBuilder.build(filter, getEntityClass()));
    }

    if (filters != null && !filters.isEmpty()) {
        spec = spec.and(filterBuilder.build(filters));
    }

    return baseService.findAll(spec, pageable).fold(
            entitiesPage -> {
                Page<D> dtoPage = entitiesPage.map(baseMapper::toDTO);

                // Headers de paginación
                HttpHeaders headers = new HttpHeaders();
                headers.add("X-Total-Count", String.valueOf(entitiesPage.getTotalElements()));
                headers.add("X-Page-Number", String.valueOf(entitiesPage.getNumber()));
                headers.add("X-Page-Size", String.valueOf(entitiesPage.getSize()));
                headers.add("X-Total-Pages", String.valueOf(entitiesPage.getTotalPages()));

                // Aplicar HATEOAS si está disponible
                if (resourceAssembler != null) {
                    PagedModel<EntityModel<D>> pagedModel = resourceAssembler.toPagedModel(dtoPage);
                    return ResponseEntity.ok().headers(headers).body(pagedModel);
                }

                // Aplicar sparse fieldsets si se solicitaron
                if (fields != null && !fields.isEmpty()) {
                    List<Map<String, Object>> filtered = dtoPage.getContent().stream()
                            .map(dto -> filterFields(dto, fields))
                            .toList();
                    return ResponseEntity.ok().headers(headers).body(Map.of(
                            "content", filtered,
                            "page", Map.of(
                                    "number", dtoPage.getNumber(),
                                    "size", dtoPage.getSize(),
                                    "totalElements", dtoPage.getTotalElements(),
                                    "totalPages", dtoPage.getTotalPages()
                            )
                    ));
                }

                return ResponseEntity.ok().headers(headers).body(dtoPage);
            },
            this::handleFailure
    );
}
```

**Ejemplos de uso:**

```bash
# Paginación básica
GET /api/v1/usuarios?page=0&size=20&sort=id,desc

# Filtrado simple
GET /api/v1/usuarios?filter=nombre:like:Juan,estado:eq:true

# Filtrado avanzado
GET /api/v1/usuarios?filter=edad:gte:18,fechaCreacion:between:2024-01-01;2024-12-31

# Sparse fieldsets
GET /api/v1/usuarios?fields=id,nombre,email

# Combinado
GET /api/v1/usuarios?filter=estado:eq:true&fields=id,nombre&page=0&size=10
```

#### B. GET /{id} - Obtener con ETag

```java
@Override
@GetMapping("/{id}")
public ResponseEntity<?> findById(
        @PathVariable ID id,
        @RequestParam(required = false) Set<String> fields,
        @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch) {

    return baseService.findById(id).fold(
            entity -> {
                D dto = baseMapper.toDTO(entity);
                String etag = ETagGenerator.generate(dto);

                // Verificar cache condicional
                if (etag != null && ETagGenerator.matchesIfNoneMatch(etag, ifNoneMatch)) {
                    log.debug("ETag match for {} {}, returning 304", getResourceName(), id);
                    return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                            .eTag(etag)
                            .build();
                }

                ResponseEntity.BodyBuilder responseBuilder = ResponseEntity.ok();
                if (etag != null) {
                    responseBuilder.eTag(etag);
                }

                // Agregar Last-Modified
                if (entity.getFechaActualizacion() != null) {
                    ZonedDateTime lastModified = entity.getFechaActualizacion()
                            .atZone(ZoneOffset.UTC);
                    responseBuilder.lastModified(lastModified);
                }

                // HATEOAS
                if (resourceAssembler != null) {
                    return responseBuilder.body(resourceAssembler.toModel(dto));
                }

                // Sparse fieldsets
                if (fields != null && !fields.isEmpty()) {
                    return responseBuilder.body(filterFields(dto, fields));
                }

                return responseBuilder.body(dto);
            },
            this::handleFailure
    );
}
```

**Ejemplo de uso con cache:**

```bash
# Primera request
GET /api/v1/usuarios/1
Response: 200 OK
ETag: "a3f5c9d2"
{
  "id": 1,
  "nombre": "Juan",
  "email": "juan@example.com"
}

# Segunda request con ETag
GET /api/v1/usuarios/1
If-None-Match: "a3f5c9d2"
Response: 304 Not Modified
(sin body, ahorro de bandwidth)
```

#### C. POST / - Crear con Location Header

```java
@Override
@PostMapping("")
public ResponseEntity<?> save(@Valid @RequestBody D dto) {
    E entity = baseMapper.toEntity(dto);
    return baseService.save(entity).fold(
            savedEntity -> {
                D savedDto = baseMapper.toDTO(savedEntity);

                // Construir Location URI
                URI location = ServletUriComponentsBuilder
                        .fromCurrentRequest()
                        .path("/{id}")
                        .buildAndExpand(savedEntity.getId())
                        .toUri();

                // Generar ETag
                String etag = ETagGenerator.generate(savedDto);

                log.info("Creado {} con ID: {} en {}", getResourceName(), savedEntity.getId(), location);

                ResponseEntity.BodyBuilder responseBuilder = ResponseEntity.created(location);
                if (etag != null) {
                    responseBuilder.eTag(etag);
                }

                if (resourceAssembler != null) {
                    return responseBuilder.body(resourceAssembler.toModel(savedDto));
                }

                return responseBuilder.body(savedDto);
            },
            this::handleFailure
    );
}
```

**Ejemplo de respuesta:**

```bash
POST /api/v1/usuarios
{
  "nombre": "Juan",
  "email": "juan@example.com"
}

Response: 201 Created
Location: /api/v1/usuarios/123
ETag: "a3f5c9d2"
{
  "id": 123,
  "nombre": "Juan",
  "email": "juan@example.com",
  "_links": {
    "self": { "href": "/api/v1/usuarios/123" },
    "collection": { "href": "/api/v1/usuarios" }
  }
}
```

#### D. PUT /{id} - Actualizar con Concurrencia Optimista

```java
@Override
@PutMapping("/{id}")
public ResponseEntity<?> update(
        @PathVariable ID id,
        @Valid @RequestBody D dto,
        @RequestHeader(value = "If-Match", required = false) String ifMatch) {

    // Validar que el ID del path coincide con el del DTO
    if (dto.id() != null && !dto.id().equals(extractIdAsLong(id))) {
        throw new IdMismatchException(id, dto.id());
    }

    // Verificar concurrencia optimista si se proporciona If-Match
    if (ifMatch != null && !ifMatch.isBlank()) {
        return baseService.findById(id).fold(
                existingEntity -> {
                    D existingDto = baseMapper.toDTO(existingEntity);
                    String currentEtag = ETagGenerator.generate(existingDto);

                    if (!ETagGenerator.matchesIfMatch(currentEtag, ifMatch)) {
                        throw PreconditionFailedException.etagMismatch(currentEtag, ifMatch);
                    }

                    return performUpdate(id, dto);
                },
                this::handleFailure
        );
    }

    return performUpdate(id, dto);
}
```

**Ejemplo de uso con concurrencia optimista:**

```bash
# Obtener el recurso con ETag
GET /api/v1/usuarios/1
Response: 200 OK
ETag: "abc123"

# Actualizar con If-Match
PUT /api/v1/usuarios/1
If-Match: "abc123"
{
  "id": 1,
  "nombre": "Juan Modificado"
}

# Si el ETag coincide → 200 OK
# Si el ETag NO coincide (otro usuario modificó) → 412 Precondition Failed
```

#### E. PATCH /{id} - Actualización Parcial

```java
@Override
@PatchMapping("/{id}")
public ResponseEntity<?> partialUpdate(
        @PathVariable ID id,
        @RequestBody D dto,
        @RequestHeader(value = "If-Match", required = false) String ifMatch) {

    return baseService.findById(id).fold(
            existingEntity -> {
                // Verificar concurrencia optimista
                if (ifMatch != null && !ifMatch.isBlank()) {
                    D existingDto = baseMapper.toDTO(existingEntity);
                    String currentEtag = ETagGenerator.generate(existingDto);

                    if (!ETagGenerator.matchesIfMatch(currentEtag, ifMatch)) {
                        throw PreconditionFailedException.etagMismatch(currentEtag, ifMatch);
                    }
                }

                // Actualizar solo campos no nulos
                baseMapper.updateEntityFromDTO(dto, existingEntity);

                return baseService.save(existingEntity).fold(
                        updatedEntity -> {
                            D updatedDto = baseMapper.toDTO(updatedEntity);
                            String etag = ETagGenerator.generate(updatedDto);

                            ResponseEntity.BodyBuilder responseBuilder = ResponseEntity.ok();
                            if (etag != null) {
                                responseBuilder.eTag(etag);
                            }

                            if (resourceAssembler != null) {
                                return responseBuilder.body(resourceAssembler.toModel(updatedDto));
                            }

                            return responseBuilder.body(updatedDto);
                        },
                        this::handleFailure
                );
            },
            this::handleFailure
    );
}
```

#### F. DELETE /{id} - Soft/Hard Delete

```java
@Override
@DeleteMapping("/{id}")
public ResponseEntity<Void> delete(
        @PathVariable ID id,
        @RequestParam(required = false, defaultValue = "false") boolean permanent) {

    if (permanent) {
        log.warn("DELETE {} - hardDelete: {} - OPERACIÓN IRREVERSIBLE", getResourceName(), id);
        return baseService.hardDelete(id).fold(
                success -> {
                    log.info("Eliminado permanentemente {} con ID: {}", getResourceName(), id);
                    return ResponseEntity.noContent().build();
                },
                error -> {
                    handleFailure(error);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                }
        );
    }

    log.debug("DELETE {} - softDelete: {}", getResourceName(), id);
    return baseService.softDelete(id).fold(
            success -> {
                log.info("Eliminado lógicamente {} con ID: {}", getResourceName(), id);
                return ResponseEntity.noContent().build();
            },
            error -> {
                handleFailure(error);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
    );
}
```

**Ejemplos:**

```bash
# Soft delete (por defecto)
DELETE /api/v1/usuarios/1
Response: 204 No Content

# Hard delete (permanente)
DELETE /api/v1/usuarios/1?permanent=true
Response: 204 No Content

# Restaurar un recurso eliminado
POST /api/v1/usuarios/1/restore
Response: 200 OK
```

---

## GlobalExceptionHandler

Maneja todas las excepciones de forma centralizada, proporcionando respuestas consistentes conforme a **RFC 7807 (Problem Details for HTTP APIs)**.

**Ubicacion:** `apigen-core/src/main/java/com/jnzader/apigen/core/infrastructure/exception/GlobalExceptionHandler.java`

### 1. Estructura Principal

```java
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    public static final MediaType APPLICATION_PROBLEM_JSON =
        MediaType.valueOf("application/problem+json");
```

### 2. Excepciones de Dominio

#### A. ResourceNotFoundException → 404

```java
@ExceptionHandler(ResourceNotFoundException.class)
public ResponseEntity<ProblemDetail> handleResourceNotFoundException(
        ResourceNotFoundException ex,
        HttpServletRequest request) {

    log.debug("Resource not found: {}", ex.getMessage());

    ProblemDetail problem = ProblemDetail.builder()
            .type(URI.create("https://api.example.com/problems/not-found"))
            .title("Recurso no encontrado")
            .status(HttpStatus.NOT_FOUND.value())
            .detail(ex.getMessage())
            .instance(request.getRequestURI())
            .build();

    return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .contentType(APPLICATION_PROBLEM_JSON)
            .body(problem);
}
```

**Ejemplo de respuesta:**

```json
HTTP/1.1 404 Not Found
Content-Type: application/problem+json

{
  "type": "https://api.example.com/problems/not-found",
  "title": "Recurso no encontrado",
  "status": 404,
  "detail": "Usuario con ID '999' no encontrado",
  "instance": "/api/v1/usuarios/999",
  "timestamp": "2024-01-20T10:30:00Z",
  "requestId": "a3f5c9d2e8b1"
}
```

#### B. DuplicateResourceException → 409

```java
@ExceptionHandler(DuplicateResourceException.class)
public ResponseEntity<ProblemDetail> handleDuplicateResourceException(
        DuplicateResourceException ex,
        HttpServletRequest request) {

    log.debug("Duplicate resource: {}", ex.getMessage());

    ProblemDetail problem = ProblemDetail.builder()
            .type(URI.create("https://api.example.com/problems/conflict"))
            .title("Conflicto de recurso")
            .status(HttpStatus.CONFLICT.value())
            .detail(ex.getMessage())
            .instance(request.getRequestURI())
            .build();

    return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .contentType(APPLICATION_PROBLEM_JSON)
            .body(problem);
}
```

#### C. PreconditionFailedException → 412

```java
@ExceptionHandler(PreconditionFailedException.class)
public ResponseEntity<ProblemDetail> handlePreconditionFailedException(
        PreconditionFailedException ex,
        HttpServletRequest request) {

    log.debug("Precondition failed: {}", ex.getMessage());

    ProblemDetail problem = ProblemDetail.builder()
            .type(URI.create("https://api.example.com/problems/precondition-failed"))
            .title("Precondición fallida")
            .status(HttpStatus.PRECONDITION_FAILED.value())
            .detail(ex.getMessage())
            .instance(request.getRequestURI())
            .build();

    return ResponseEntity
            .status(HttpStatus.PRECONDITION_FAILED)
            .contentType(APPLICATION_PROBLEM_JSON)
            .body(problem);
}
```

### 3. Excepciones de Validación

#### A. MethodArgumentNotValidException → 400

```java
@ExceptionHandler(MethodArgumentNotValidException.class)
public ResponseEntity<ProblemDetail> handleMethodArgumentNotValid(
        MethodArgumentNotValidException ex,
        HttpServletRequest request) {

    log.debug("Validation failed for request to {}", request.getRequestURI());

    // Recopilar errores de campo
    Map<String, List<String>> fieldErrors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .collect(Collectors.groupingBy(
                    error -> error.getField(),
                    Collectors.mapping(
                            error -> error.getDefaultMessage(),
                            Collectors.toList()
                    )
            ));

    // Recopilar errores globales
    List<String> globalErrors = ex.getBindingResult()
            .getGlobalErrors()
            .stream()
            .map(error -> error.getDefaultMessage())
            .collect(Collectors.toList());

    Map<String, Object> extensions = new HashMap<>();
    if (!fieldErrors.isEmpty()) {
        extensions.put("fieldErrors", fieldErrors);
    }
    if (!globalErrors.isEmpty()) {
        extensions.put("globalErrors", globalErrors);
    }
    extensions.put("errorCount", ex.getBindingResult().getErrorCount());

    ProblemDetail problem = ProblemDetail.builder()
            .type(URI.create("https://api.example.com/problems/validation-error"))
            .title("Error de validación de entrada")
            .status(HttpStatus.BAD_REQUEST.value())
            .detail("La solicitud contiene " + ex.getBindingResult().getErrorCount() + " error(es) de validación")
            .instance(request.getRequestURI())
            .extensions(extensions)
            .build();

    return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .contentType(APPLICATION_PROBLEM_JSON)
            .body(problem);
}
```

**Ejemplo de respuesta:**

```json
HTTP/1.1 400 Bad Request
Content-Type: application/problem+json

{
  "type": "https://api.example.com/problems/validation-error",
  "title": "Error de validación de entrada",
  "status": 400,
  "detail": "La solicitud contiene 2 error(es) de validación",
  "instance": "/api/v1/usuarios",
  "timestamp": "2024-01-20T10:30:00Z",
  "requestId": "a3f5c9d2e8b1",
  "fieldErrors": {
    "email": ["El email no es válido"],
    "edad": ["Debe ser mayor a 18"]
  },
  "errorCount": 2
}
```

### 4. ApiError (Sealed Interface)

Modelo type-safe para errores con pattern matching exhaustivo.

**Ubicacion:** `apigen-core/src/main/java/com/jnzader/apigen/core/infrastructure/exception/ApiError.java`

```java
public sealed interface ApiError {

    record NotFound(String resourceType, Object resourceId, String message) implements ApiError {
        public NotFound(String resourceType, Object resourceId) {
            this(resourceType, resourceId,
                    String.format("%s con ID '%s' no encontrado", resourceType, resourceId));
        }
    }

    record Validation(String message, Map<String, String> fieldErrors) implements ApiError {
        public Validation(String message) {
            this(message, Map.of());
        }
    }

    record Conflict(String message, String conflictType) implements ApiError {
        public Conflict(String message) {
            this(message, "CONFLICT");
        }
    }

    record Forbidden(String message, String requiredRole, String currentUser) implements ApiError {
        public Forbidden(String message) {
            this(message, null, null);
        }
    }

    record PreconditionFailed(
            String message,
            String expectedEtag,
            String providedEtag
    ) implements ApiError {}

    record IdMismatch(Object pathId, Object bodyId, String message) implements ApiError {}

    record Internal(String message, Throwable cause) implements ApiError {}

    // Factory method
    static ApiError from(Exception exception) {
        return switch (exception) {
            case ResourceNotFoundException ex -> new NotFound(ex.getMessage());
            case ValidationException ex -> new Validation(ex.getMessage());
            case DuplicateResourceException ex -> new Conflict(ex.getMessage(), "DUPLICATE_RESOURCE");
            case UnauthorizedActionException ex -> new Forbidden(ex.getMessage());
            case PreconditionFailedException ex ->
                new PreconditionFailed(ex.getMessage(), ex.getCurrentEtag(), ex.getProvidedEtag());
            case IdMismatchException ex ->
                new IdMismatch(ex.getPathId(), ex.getBodyId(), ex.getMessage());
            case OperationFailedException ex -> new Internal(ex.getMessage(), ex.getCause());
            case IllegalArgumentException ex -> new Validation(ex.getMessage());
            case null, default -> new Internal(
                    exception != null ? exception.getMessage() : "Error desconocido",
                    exception
            );
        };
    }

    // Obtener HTTP status
    default HttpStatus httpStatus() {
        return switch (this) {
            case NotFound _ -> HttpStatus.NOT_FOUND;
            case Validation _ -> HttpStatus.BAD_REQUEST;
            case Conflict _ -> HttpStatus.CONFLICT;
            case Forbidden _ -> HttpStatus.FORBIDDEN;
            case PreconditionFailed _ -> HttpStatus.PRECONDITION_FAILED;
            case IdMismatch _ -> HttpStatus.BAD_REQUEST;
            case Internal _ -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }

    // Convertir a ProblemDetail
    default ProblemDetail toProblemDetail(String instance) {
        var builder = ProblemDetail.builder()
                .type(java.net.URI.create(typeUri()))
                .title(title())
                .status(statusCode())
                .detail(detail())
                .instance(instance);

        // Agregar extensiones específicas
        switch (this) {
            case Validation v when !v.fieldErrors().isEmpty() ->
                    builder.extension("fieldErrors", v.fieldErrors());
            case PreconditionFailed pf when pf.expectedEtag() != null -> {
                builder.extension("expectedEtag", pf.expectedEtag());
                builder.extension("providedEtag", pf.providedEtag());
            }
            case IdMismatch im -> {
                builder.extension("pathId", im.pathId());
                builder.extension("bodyId", im.bodyId());
            }
            case Conflict c -> builder.extension("conflictType", c.conflictType());
            default -> { /* No extensions needed */ }
        }

        return builder.build();
    }
}
```

### 5. ProblemDetail Record

Implementación de RFC 7807.

```java
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProblemDetail(
        URI type,           // URI que identifica el tipo de problema
        String title,       // Título breve del problema
        int status,         // Código HTTP
        String detail,      // Explicación detallada
        URI instance,       // URI de la ocurrencia específica
        Instant timestamp,  // Cuándo ocurrió
        String requestId,   // ID de request para trazabilidad
        Map<String, Object> extensions  // Campos adicionales
) {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        // ... métodos fluidos para construir ProblemDetail
    }
}
```

---

## Configuraciones Spring

### 1. JpaConfig

Configura JPA Auditing para poblar automáticamente campos de auditoría.

**Ubicacion:** `apigen-core/src/main/java/com/jnzader/apigen/core/infrastructure/config/JpaConfig.java`

```java
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaConfig {

    /**
     * Provee el usuario actual para auditoría.
     * Obtiene el nombre del usuario autenticado del SecurityContext.
     */
    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || !authentication.isAuthenticated()) {
                return Optional.of("system");
            }

            String principal = authentication.getName();

            // Si es anónimo, usar "anonymous"
            if ("anonymousUser".equals(principal)) {
                return Optional.of("anonymous");
            }

            return Optional.of(principal);
        };
    }
}
```

**Funcionalidad:**

- Pobla automáticamente `creadoPor` y `modificadoPor`
- Obtiene el usuario del `SecurityContext`
- Usa "system" o "anonymous" como fallback

### 2. WebConfig

Configura interceptores y negociación de contenido.

**Ubicacion:** `apigen-core/src/main/java/com/jnzader/apigen/core/infrastructure/config/WebConfig.java`

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final RequestIdInterceptor requestIdInterceptor;

    public WebConfig(RequestIdInterceptor requestIdInterceptor) {
        this.requestIdInterceptor = requestIdInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Request ID interceptor para todas las requests de la API
        registry.addInterceptor(requestIdInterceptor)
                .addPathPatterns("/api/**");
    }

    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer
                .favorParameter(false)  // No usar parámetros
                .defaultContentType(MediaType.APPLICATION_JSON)
                .mediaType("json", MediaType.APPLICATION_JSON)
                .mediaType("xml", MediaType.APPLICATION_XML);
    }
}
```

### 3. OpenApiConfig

Configura SpringDoc para documentación OpenAPI/Swagger.

**Ubicacion:** `apigen-core/src/main/java/com/jnzader/apigen/core/infrastructure/config/OpenApiConfig.java`

```java
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API Genérica de Microservicios")
                        .version("1.0.0")
                        .description("Plantilla de API RESTful genérica construida con " +
                                     "Spring Boot 4, Java 25, Spring Security y MapStruct. " +
                                     "Proporciona funcionalidades CRUD básicas para entidades con auditoría.")
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

**Acceso a Swagger UI:**

```
http://localhost:8080/swagger-ui/index.html
```

### 4. AsyncConfig (Virtual Threads)

Configura procesamiento asíncrono con Virtual Threads (Java 21+).

**Ubicacion:** `apigen-core/src/main/java/com/jnzader/apigen/core/infrastructure/config/AsyncConfig.java`

```java
@Configuration
@EnableAsync
public class AsyncConfig {

    private static final Logger log = LoggerFactory.getLogger(AsyncConfig.class);

    /**
     * Executor para eventos de dominio usando Virtual Threads.
     * Virtual Threads son más eficientes para I/O-bound tasks.
     */
    @Bean(name = "domainEventExecutor")
    @ConditionalOnProperty(name = "spring.threads.virtual.enabled",
                          havingValue = "true",
                          matchIfMissing = true)
    public Executor virtualThreadDomainEventExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("domain-event-");
        executor.setVirtualThreads(true);
        executor.setTaskDecorator(task -> {
            return () -> {
                long start = System.currentTimeMillis();
                try {
                    task.run();
                } finally {
                    log.debug("Domain event task completed in {}ms",
                             System.currentTimeMillis() - start);
                }
            };
        });

        log.info("Domain event executor initialized with Virtual Threads");
        return executor;
    }

    /**
     * Executor alternativo usando ThreadPool para entornos sin Virtual Threads.
     */
    @Bean(name = "domainEventExecutor")
    @ConditionalOnProperty(name = "spring.threads.virtual.enabled", havingValue = "false")
    public Executor threadPoolDomainEventExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("domain-event-");
        executor.setRejectedExecutionHandler((r, e) ->
                log.warn("Domain event task rejected, queue full. Consider increasing capacity."));
        executor.initialize();

        log.info("Domain event executor initialized: corePoolSize={}, maxPoolSize={}, queueCapacity={}",
                executor.getCorePoolSize(), executor.getMaxPoolSize(), 100);

        return executor;
    }

    /**
     * Executor general para tareas asíncronas con Virtual Threads.
     */
    @Bean(name = "taskExecutor")
    @ConditionalOnProperty(name = "spring.threads.virtual.enabled",
                          havingValue = "true",
                          matchIfMissing = true)
    public TaskExecutor virtualThreadTaskExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("async-");
        executor.setVirtualThreads(true);
        log.info("Default task executor configured with Virtual Threads");
        return executor;
    }
}
```

**Configuración en application.yaml:**

```yaml
spring:
  threads:
    virtual:
      enabled: true  # Habilitar Virtual Threads (requiere Java 21+)
```

**Ventajas de Virtual Threads:**

- Menor consumo de memoria
- Mejor escalabilidad para I/O-bound tasks
- Sintaxis simple (sin CompletableFuture)
- Compatible con código bloqueante

### 5. ETagConfig

Configura ETag automático para caché HTTP.

**Ubicacion:** `apigen-core/src/main/java/com/jnzader/apigen/core/infrastructure/config/ETagConfig.java`

```java
@Configuration
public class ETagConfig {

    @Bean
    public FilterRegistrationBean<ShallowEtagHeaderFilter> shallowEtagHeaderFilter() {
        FilterRegistrationBean<ShallowEtagHeaderFilter> filterRegistrationBean =
                new FilterRegistrationBean<>(new ShallowEtagHeaderFilter());

        // Aplicar a todos los endpoints de la API
        filterRegistrationBean.addUrlPatterns("/api/*");

        // Orden (después del logging, antes del rate limiting)
        filterRegistrationBean.setOrder(2);

        filterRegistrationBean.setName("etagFilter");

        return filterRegistrationBean;
    }
}
```

**Funcionamiento:**

1. Calcula un hash MD5 del contenido de la respuesta
2. Lo envía como header `ETag`
3. Si el cliente envía `If-None-Match` con el mismo ETag → **304 Not Modified**
4. Ahorra bandwidth (no se envía el body)

**Limitaciones:**

- El servidor procesa la request completamente
- Solo ahorra bandwidth, no procesamiento
- Para verdadero ahorro, usar caché del lado del servidor (Redis, Caffeine)

---

## Filtros HTTP

### 1. RequestLoggingFilter

Filtro para logging estructurado de requests/responses HTTP.

**Ubicacion:** `apigen-core/src/main/java/com/jnzader/apigen/core/infrastructure/filter/RequestLoggingFilter.java`

#### Características

- Genera ID único de correlación para cada request (`traceId`)
- Registra método, URI, headers, duración
- Sanitiza información sensible (passwords, tokens, tarjetas)
- Thread-safe usando MDC (Mapped Diagnostic Context)
- Logging estructurado JSON

#### Implementación

```java
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    // Headers de correlación
    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    // MDC Keys (deben coincidir con logback-spring.xml)
    private static final String MDC_TRACE_ID = "traceId";
    private static final String MDC_SPAN_ID = "spanId";
    private static final String MDC_REQUEST_ID = "requestId";
    private static final String MDC_CORRELATION_ID = "correlationId";
    private static final String MDC_USER_ID = "userId";
    private static final String MDC_CLIENT_IP = "clientIp";
    private static final String MDC_HTTP_METHOD = "httpMethod";
    private static final String MDC_REQUEST_URI = "requestUri";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Generar o recuperar IDs de correlación
        String traceId = getOrGenerateId(request, TRACE_ID_HEADER);
        String correlationId = getOrGenerateId(request, CORRELATION_ID_HEADER);
        String requestId = UUID.randomUUID().toString();
        String spanId = UUID.randomUUID().toString().substring(0, 8);

        // Poblar MDC con toda la información de contexto
        populateMdc(request, traceId, correlationId, requestId, spanId);

        // Agregar IDs al response para correlación cliente-servidor
        response.setHeader(TRACE_ID_HEADER, traceId);
        response.setHeader(CORRELATION_ID_HEADER, correlationId);
        response.setHeader(REQUEST_ID_HEADER, requestId);

        // Cachear request y response para leer el body múltiples veces
        ContentCachingRequestWrapper wrappedRequest =
            new ContentCachingRequestWrapper(request, MAX_PAYLOAD_LENGTH);
        ContentCachingResponseWrapper wrappedResponse =
            new ContentCachingResponseWrapper(response);

        long startTime = System.currentTimeMillis();

        try {
            // Log request entrante
            logRequest(wrappedRequest);

            // Procesar request
            filterChain.doFilter(wrappedRequest, wrappedResponse);

        } finally {
            // Log response saliente
            long duration = System.currentTimeMillis() - startTime;
            logResponse(wrappedRequest, wrappedResponse, duration);

            // Copiar el body cacheado al response original
            wrappedResponse.copyBodyToResponse();

            // Limpiar MDC
            clearMdc();
        }
    }

    private void populateMdc(HttpServletRequest request, String traceId,
                             String correlationId, String requestId, String spanId) {
        MDC.put(MDC_TRACE_ID, traceId);
        MDC.put(MDC_CORRELATION_ID, correlationId);
        MDC.put(MDC_REQUEST_ID, requestId);
        MDC.put(MDC_SPAN_ID, spanId);
        MDC.put(MDC_CLIENT_IP, getClientIp(request));
        MDC.put(MDC_HTTP_METHOD, request.getMethod());
        MDC.put(MDC_REQUEST_URI, request.getRequestURI());
    }

    private void logRequest(ContentCachingRequestWrapper request) {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String queryString = request.getQueryString();

        StringBuilder message = new StringBuilder();
        message.append(">>> ").append(method).append(" ").append(uri);
        if (queryString != null) {
            message.append("?").append(sanitize(queryString));
        }

        log.info(message.toString());
    }

    private void logResponse(ContentCachingRequestWrapper request,
                            ContentCachingResponseWrapper response,
                            long duration) {
        int status = response.getStatus();
        String method = request.getMethod();
        String uri = request.getRequestURI();

        StringBuilder message = new StringBuilder();
        message.append("<<< ").append(method).append(" ").append(uri);
        message.append(" | Status: ").append(status);
        message.append(" | Duration: ").append(duration).append("ms");

        // Usar nivel apropiado según status
        if (status >= 500) {
            log.error(message.toString());
        } else if (status >= 400) {
            log.warn(message.toString());
        } else if (duration > 1000) {
            message.append(" [SLOW]");
            log.warn(message.toString());
        } else {
            log.info(message.toString());
        }
    }

    // Sanitizar información sensible
    private String sanitize(String text) {
        if (text == null || text.isBlank()) return text;

        String result = text;
        result = JWT_PATTERN.matcher(result).replaceAll("[JWT_REDACTED]");
        result = PASSWORD_PATTERN.matcher(result).replaceAll("$1$2[REDACTED]");
        result = CREDIT_CARD_PATTERN.matcher(result).replaceAll("[CARD_REDACTED]");

        return result;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
```

#### Ejemplo de Log

```json
{
  "timestamp": "2024-01-20T10:30:00.123Z",
  "level": "INFO",
  "logger": "RequestLoggingFilter",
  "message": ">>> GET /api/v1/usuarios",
  "traceId": "a3f5c9d2-e8b1-4a7c-9d2e-8b1f5c9d2e8b",
  "requestId": "f5c9d2e8b1f5c9d2",
  "correlationId": "client-correlation-123",
  "clientIp": "192.168.1.100",
  "httpMethod": "GET",
  "requestUri": "/api/v1/usuarios"
}
```

### 2. RateLimitingFilter

Filtro de limitación de requests por IP usando Caffeine cache.

**Ubicacion:** `apigen-core/src/main/java/com/jnzader/apigen/core/infrastructure/filter/RateLimitingFilter.java`

```java
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitingFilter.class);

    private final int maxRequestsPerWindow;
    private final Duration windowDuration;
    private final Cache<String, AtomicInteger> requestCounts;

    public RateLimitingFilter(
            @Value("${app.rate-limit.max-requests:100}") int maxRequestsPerWindow,
            @Value("${app.rate-limit.window-seconds:60}") int windowSeconds) {

        this.maxRequestsPerWindow = maxRequestsPerWindow;
        this.windowDuration = Duration.ofSeconds(windowSeconds);

        this.requestCounts = Caffeine.newBuilder()
                .expireAfterWrite(windowDuration)
                .maximumSize(10000)  // Máximo 10k IPs en cache
                .build();

        log.info("Rate limiting configurado: {} requests por {}s",
                maxRequestsPerWindow, windowSeconds);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String clientIp = getClientIp(request);
        AtomicInteger counter = requestCounts.get(clientIp, k -> new AtomicInteger(0));

        int currentCount = counter.incrementAndGet();
        int remaining = Math.max(0, maxRequestsPerWindow - currentCount);

        // Agregar headers de rate limit
        response.setHeader("X-RateLimit-Limit", String.valueOf(maxRequestsPerWindow));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));
        response.setHeader("X-RateLimit-Reset", String.valueOf(windowDuration.toSeconds()));

        if (currentCount > maxRequestsPerWindow) {
            log.warn("Rate limit excedido para IP: {} ({} requests)", clientIp, currentCount);

            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write("""
                    {
                        "error": "Too Many Requests",
                        "message": "Has excedido el límite de %d requests por %d segundos",
                        "retryAfter": %d
                    }
                    """.formatted(maxRequestsPerWindow,
                                 windowDuration.toSeconds(),
                                 windowDuration.toSeconds()));

            return;
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // No aplicar rate limit a actuator endpoints
        return path.startsWith("/actuator/");
    }
}
```

**Configuración:**

```yaml
app:
  rate-limit:
    max-requests: 100    # Máximo de requests
    window-seconds: 60   # Ventana de tiempo
```

**Headers de respuesta:**

```
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 87
X-RateLimit-Reset: 60
```

**Respuesta cuando se excede:**

```json
HTTP/1.1 429 Too Many Requests

{
  "error": "Too Many Requests",
  "message": "Has excedido el límite de 100 requests por 60 segundos",
  "retryAfter": 60
}
```

### 3. RequestIdInterceptor

Interceptor que agrega un Request ID único a cada solicitud.

**Ubicacion:** `apigen-core/src/main/java/com/jnzader/apigen/core/infrastructure/config/RequestIdInterceptor.java`

```java
@Component
public class RequestIdInterceptor implements HandlerInterceptor {

    public static final String REQUEST_ID_HEADER = "X-Request-ID";
    public static final String MDC_REQUEST_ID_KEY = "requestId";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                            Object handler) {
        // Obtener Request ID del header o generar uno nuevo
        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank()) {
            requestId = generateRequestId();
        }

        // Agregar al MDC para logging
        MDC.put(MDC_REQUEST_ID_KEY, requestId);

        // Agregar al header de respuesta
        response.setHeader(REQUEST_ID_HEADER, requestId);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                               Object handler, Exception ex) {
        // Limpiar MDC al finalizar
        MDC.remove(MDC_REQUEST_ID_KEY);
    }

    private String generateRequestId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
```

**Uso:**

```bash
# Cliente envía Request ID
GET /api/v1/usuarios
X-Request-ID: abc123def456

# Servidor retorna el mismo ID (o genera uno nuevo)
Response:
X-Request-ID: abc123def456
```

---

## HATEOAS

### BaseResourceAssembler

Ensamblador genérico que convierte DTOs en EntityModel con links de navegación.

**Ubicacion:** `apigen-core/src/main/java/com/jnzader/apigen/core/infrastructure/hateoas/BaseResourceAssembler.java`

```java
public abstract class BaseResourceAssembler<D extends BaseDTO, ID extends Serializable>
        implements RepresentationModelAssembler<D, EntityModel<D>> {

    private final Class<? extends BaseController<D, ID>> controllerClass;

    protected BaseResourceAssembler(Class<? extends BaseController<D, ID>> controllerClass) {
        this.controllerClass = controllerClass;
    }

    @Override
    public EntityModel<D> toModel(D dto) {
        ID id = extractId(dto);

        EntityModel<D> model = EntityModel.of(dto);

        // Self link
        model.add(linkTo(methodOn(controllerClass).findById(id, null, null))
                .withSelfRel());

        // Collection link
        model.add(linkTo(methodOn(controllerClass).findAll(null, null, null, Pageable.unpaged()))
                .withRel("collection"));

        // Update link
        model.add(linkTo(methodOn(controllerClass).update(id, null, null))
                .withRel("update"));

        // Delete link
        model.add(linkTo(methodOn(controllerClass).delete(id, false))
                .withRel("delete"));

        // Links adicionales específicos
        addCustomLinks(model, dto);

        return model;
    }

    /**
     * Convierte una página de DTOs en PagedModel con links de paginación.
     */
    public PagedModel<EntityModel<D>> toPagedModel(Page<D> page) {
        List<EntityModel<D>> content = page.getContent().stream()
                .map(this::toModel)
                .toList();

        PagedModel.PageMetadata metadata = new PagedModel.PageMetadata(
                page.getSize(),
                page.getNumber(),
                page.getTotalElements(),
                page.getTotalPages()
        );

        PagedModel<EntityModel<D>> pagedModel = PagedModel.of(content, metadata);

        // Self link
        pagedModel.add(linkTo(methodOn(controllerClass)
                .findAll(null, null, null, page.getPageable()))
                .withSelfRel());

        // Links de navegación
        if (page.hasNext()) {
            pagedModel.add(Link.of(buildPageUrl(page.getNumber() + 1, page.getSize()))
                    .withRel("next"));
        }
        if (page.hasPrevious()) {
            pagedModel.add(Link.of(buildPageUrl(page.getNumber() - 1, page.getSize()))
                    .withRel("prev"));
        }
        pagedModel.add(Link.of(buildPageUrl(0, page.getSize())).withRel("first"));
        if (page.getTotalPages() > 0) {
            pagedModel.add(Link.of(buildPageUrl(page.getTotalPages() - 1, page.getSize()))
                    .withRel("last"));
        }

        return pagedModel;
    }

    protected String buildPageUrl(int page, int size) {
        return "?page=" + page + "&size=" + size;
    }

    /**
     * Permite agregar links adicionales específicos.
     */
    protected void addCustomLinks(EntityModel<D> model, D dto) {
        // Las subclases pueden sobrescribir
    }

    @SuppressWarnings("unchecked")
    protected ID extractId(D dto) {
        Long id = dto.id();
        if (id == null) {
            throw new IllegalArgumentException(
                "El DTO debe tener un ID válido para generar links HATEOAS");
        }
        return (ID) id;
    }
}
```

### Ejemplo de Uso

```java
@Component
public class UsuarioResourceAssembler
        extends BaseResourceAssembler<UsuarioDTO, Long> {

    public UsuarioResourceAssembler() {
        super(UsuarioController.class);
    }

    @Override
    protected void addCustomLinks(EntityModel<UsuarioDTO> model, UsuarioDTO dto) {
        // Agregar link custom: activar/desactivar usuario
        model.add(linkTo(methodOn(UsuarioController.class)
                .activarUsuario(dto.id()))
                .withRel("activar"));
    }
}
```

### Respuesta con HATEOAS

```json
{
  "id": 1,
  "nombre": "Juan Pérez",
  "email": "juan@example.com",
  "_links": {
    "self": {
      "href": "http://localhost:8080/api/v1/usuarios/1"
    },
    "collection": {
      "href": "http://localhost:8080/api/v1/usuarios"
    },
    "update": {
      "href": "http://localhost:8080/api/v1/usuarios/1"
    },
    "delete": {
      "href": "http://localhost:8080/api/v1/usuarios/1"
    },
    "activar": {
      "href": "http://localhost:8080/api/v1/usuarios/1/activar"
    }
  }
}
```

### Respuesta Paginada con HATEOAS

```json
{
  "_embedded": {
    "usuarios": [
      {
        "id": 1,
        "nombre": "Juan",
        "_links": { ... }
      },
      {
        "id": 2,
        "nombre": "María",
        "_links": { ... }
      }
    ]
  },
  "_links": {
    "self": {
      "href": "http://localhost:8080/api/v1/usuarios?page=0&size=20"
    },
    "next": {
      "href": "http://localhost:8080/api/v1/usuarios?page=1&size=20"
    },
    "last": {
      "href": "http://localhost:8080/api/v1/usuarios?page=4&size=20"
    }
  },
  "page": {
    "size": 20,
    "number": 0,
    "totalElements": 100,
    "totalPages": 5
  }
}
```

---

## Specifications para Filtrado Dinámico

### FilterSpecificationBuilder

Constructor de especificaciones JPA dinámicas a partir de query params.

**Ubicacion:** `apigen-core/src/main/java/com/jnzader/apigen/core/domain/specification/FilterSpecificationBuilder.java`

#### Operadores Soportados

| Operador | Significado | Ejemplo | SQL Generado |
|----------|-------------|---------|--------------|
| `eq` | Igual | `nombre:eq:Juan` | `nombre = 'Juan'` |
| `neq` | No igual | `estado:neq:false` | `estado != false` |
| `like` | Contiene | `nombre:like:Juan` | `nombre LIKE '%Juan%'` |
| `starts` | Empieza con | `email:starts:admin` | `email LIKE 'admin%'` |
| `ends` | Termina con | `email:ends:.com` | `email LIKE '%.com'` |
| `gt` | Mayor que | `edad:gt:18` | `edad > 18` |
| `gte` | Mayor o igual | `edad:gte:18` | `edad >= 18` |
| `lt` | Menor que | `edad:lt:65` | `edad < 65` |
| `lte` | Menor o igual | `edad:lte:65` | `edad <= 65` |
| `in` | En lista | `id:in:1;2;3` | `id IN (1,2,3)` |
| `notin` | No en lista | `estado:notin:INACTIVO;ELIMINADO` | `estado NOT IN (...)` |
| `between` | Entre valores | `edad:between:18;65` | `edad BETWEEN 18 AND 65` |
| `null` | Es nulo | `email:null` | `email IS NULL` |
| `notnull` | No es nulo | `email:notnull` | `email IS NOT NULL` |

#### Implementación

```java
@Component
public class FilterSpecificationBuilder {

    private static final Logger log = LoggerFactory.getLogger(FilterSpecificationBuilder.class);

    private static final String FILTER_SEPARATOR = ",";
    private static final String OPERATOR_SEPARATOR = ":";
    private static final String VALUE_LIST_SEPARATOR = ";";

    /**
     * Construye una Specification a partir de un string de filtros.
     *
     * Formato: campo:operador:valor,campo2:operador2:valor2
     */
    public <E extends Base> Specification<E> build(String filterString, Class<E> entityClass) {
        if (filterString == null || filterString.isBlank()) {
            return (root, query, cb) -> cb.conjunction();
        }

        List<FilterCriteria> criteria = parseFilterString(filterString);
        return buildSpecification(criteria);
    }

    /**
     * Construye una Specification a partir de un Map de filtros.
     */
    public <E extends Base> Specification<E> build(Map<String, String> filters) {
        if (filters == null || filters.isEmpty()) {
            return (root, query, cb) -> cb.conjunction();
        }

        // Filtrar parámetros de sistema
        Set<String> systemParams = Set.of("page", "size", "sort", "fields", "filter");

        List<FilterCriteria> criteria = filters.entrySet().stream()
                .filter(e -> !systemParams.contains(e.getKey().toLowerCase()))
                .filter(e -> e.getValue() != null && !e.getValue().isBlank())
                .map(this::parseMapEntry)
                .filter(Objects::nonNull)
                .toList();

        return buildSpecification(criteria);
    }

    private <E extends Base> Specification<E> buildSpecification(List<FilterCriteria> criteria) {
        Specification<E> spec = (root, query, cb) -> cb.conjunction();

        for (FilterCriteria c : criteria) {
            Specification<E> criteriaSpec = buildCriteriaSpecification(c);
            if (criteriaSpec != null) {
                spec = spec.and(criteriaSpec);
            }
        }

        return spec;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <E extends Base> Specification<E> buildCriteriaSpecification(FilterCriteria c) {
        return (root, query, cb) -> {
            Path<?> path = getPath(root, c.field());
            Object typedValue = convertValue(c.value(), path.getJavaType());

            return switch (c.operator()) {
                case EQ -> cb.equal(path, typedValue);
                case NEQ -> cb.notEqual(path, typedValue);
                case LIKE -> cb.like(cb.lower((Path<String>) path),
                        "%" + c.value().toLowerCase() + "%");
                case STARTS -> cb.like(cb.lower((Path<String>) path),
                        c.value().toLowerCase() + "%");
                case ENDS -> cb.like(cb.lower((Path<String>) path),
                        "%" + c.value().toLowerCase());
                case GT -> cb.greaterThan((Path<Comparable>) path, (Comparable) typedValue);
                case GTE -> cb.greaterThanOrEqualTo((Path<Comparable>) path, (Comparable) typedValue);
                case LT -> cb.lessThan((Path<Comparable>) path, (Comparable) typedValue);
                case LTE -> cb.lessThanOrEqualTo((Path<Comparable>) path, (Comparable) typedValue);
                case IN -> buildInPredicate(cb, path, c.value());
                case NOT_IN -> cb.not(buildInPredicate(cb, path, c.value()));
                case BETWEEN -> buildBetweenPredicate(cb, (Path<Comparable>) path, c.value());
                case NULL -> cb.isNull(path);
                case NOT_NULL -> cb.isNotNull(path);
            };
        };
    }

    /**
     * Soporta notación con puntos para relaciones.
     * Ejemplo: "role.name" -> root.get("role").get("name")
     */
    private Path<?> getPath(Root<?> root, String field) {
        String[] parts = field.split("\\.");
        Path<?> path = root;
        for (String part : parts) {
            path = path.get(part);
        }
        return path;
    }

    private Object convertValue(String value, Class<?> targetType) {
        if (value == null) return null;

        try {
            if (targetType == String.class) return value;
            if (targetType == Long.class || targetType == long.class)
                return Long.parseLong(value);
            if (targetType == Integer.class || targetType == int.class)
                return Integer.parseInt(value);
            if (targetType == Double.class || targetType == double.class)
                return Double.parseDouble(value);
            if (targetType == Boolean.class || targetType == boolean.class)
                return Boolean.parseBoolean(value);
            if (targetType == LocalDateTime.class)
                return parseLocalDateTime(value);
            if (targetType == LocalDate.class)
                return LocalDate.parse(value);
            if (targetType.isEnum())
                return Enum.valueOf((Class<Enum>) targetType, value.toUpperCase());
        } catch (Exception e) {
            log.warn("Error convirtiendo valor '{}' a tipo {}: {}",
                    value, targetType.getSimpleName(), e.getMessage());
        }

        return value;
    }
}
```

### BaseSpecification

Especificaciones JPA predefinidas para queries comunes.

**Ubicacion:** `apigen-core/src/main/java/com/jnzader/apigen/core/domain/specification/BaseSpecification.java`

```java
public final class BaseSpecification<E extends Base> {

    // ==================== Estado ====================

    public static <E extends Base> Specification<E> isActive() {
        return (root, query, cb) -> cb.isTrue(root.get("estado"));
    }

    public static <E extends Base> Specification<E> isInactive() {
        return (root, query, cb) -> cb.isFalse(root.get("estado"));
    }

    // ==================== Fechas ====================

    public static <E extends Base> Specification<E> createdAfter(LocalDateTime date) {
        return (root, query, cb) -> date != null
                ? cb.greaterThan(root.get("fechaCreacion"), date)
                : cb.conjunction();
    }

    public static <E extends Base> Specification<E> createdBetween(
            LocalDateTime start, LocalDateTime end) {
        return (root, query, cb) -> {
            if (start != null && end != null) {
                return cb.between(root.get("fechaCreacion"), start, end);
            } else if (start != null) {
                return cb.greaterThanOrEqualTo(root.get("fechaCreacion"), start);
            } else if (end != null) {
                return cb.lessThanOrEqualTo(root.get("fechaCreacion"), end);
            }
            return cb.conjunction();
        };
    }

    // ==================== Soft Delete ====================

    public static <E extends Base> Specification<E> notDeleted() {
        return (root, query, cb) -> cb.isNull(root.get("fechaEliminacion"));
    }

    public static <E extends Base> Specification<E> isDeleted() {
        return (root, query, cb) -> cb.isNotNull(root.get("fechaEliminacion"));
    }

    // ==================== Auditoría ====================

    public static <E extends Base> Specification<E> createdBy(String usuario) {
        return (root, query, cb) -> usuario != null
                ? cb.equal(root.get("creadoPor"), usuario)
                : cb.conjunction();
    }

    // ==================== Combinadores ====================

    @SafeVarargs
    public static <E extends Base> Specification<E> allOf(Specification<E>... specs) {
        Specification<E> result = Specification.where(null);
        for (Specification<E> spec : specs) {
            if (spec != null) {
                result = result.and(spec);
            }
        }
        return result;
    }

    @SafeVarargs
    public static <E extends Base> Specification<E> anyOf(Specification<E>... specs) {
        Specification<E> result = Specification.where(null);
        boolean first = true;
        for (Specification<E> spec : specs) {
            if (spec != null) {
                if (first) {
                    result = Specification.where(spec);
                    first = false;
                } else {
                    result = result.or(spec);
                }
            }
        }
        return result;
    }
}
```

### Ejemplos de Uso Avanzado

#### 1. Combinación de Filtros

```java
@GetMapping
public ResponseEntity<?> buscarUsuarios(
        @RequestParam(required = false) String filter,
        @RequestParam(required = false) String rol,
        Pageable pageable) {

    // Filtro dinámico desde query params
    Specification<Usuario> spec = filterBuilder.build(filter, Usuario.class);

    // Combinar con especificaciones predefinidas
    spec = spec.and(BaseSpecification.isActive())
               .and(BaseSpecification.notDeleted());

    // Agregar filtro de rol si se especifica
    if (rol != null) {
        spec = spec.and((root, query, cb) ->
            cb.equal(root.get("rol").get("nombre"), rol));
    }

    Page<Usuario> usuarios = usuarioRepository.findAll(spec, pageable);
    return ResponseEntity.ok(usuarios);
}
```

**Request:**

```bash
GET /api/v1/usuarios?filter=edad:gte:18,nombre:like:Juan&rol=ADMIN
```

**SQL Generado:**

```sql
SELECT * FROM usuarios u
INNER JOIN roles r ON u.rol_id = r.id
WHERE u.estado = true
  AND u.fecha_eliminacion IS NULL
  AND u.edad >= 18
  AND LOWER(u.nombre) LIKE '%juan%'
  AND r.nombre = 'ADMIN'
ORDER BY u.id DESC
LIMIT 20 OFFSET 0;
```

#### 2. Búsqueda Multi-campo

```java
public Specification<Usuario> busquedaGlobal(String termino) {
    return (root, query, cb) -> {
        String pattern = "%" + termino.toLowerCase() + "%";

        return cb.or(
            cb.like(cb.lower(root.get("nombre")), pattern),
            cb.like(cb.lower(root.get("email")), pattern),
            cb.like(cb.lower(root.get("telefono")), pattern),
            cb.like(cb.lower(root.get("documento")), pattern)
        );
    };
}
```

**Request:**

```bash
GET /api/v1/usuarios?q=juan
```

#### 3. Filtrado por Relaciones

```bash
# Buscar usuarios por nombre de rol
GET /api/v1/usuarios?filter=rol.nombre:eq:ADMIN

# Buscar por ciudad del domicilio
GET /api/v1/usuarios?filter=domicilio.ciudad:like:Buenos Aires
```

---

## Diagrama de Arquitectura

```
┌─────────────────────────────────────────────────────────────────┐
│                        HTTP REQUEST                              │
└─────────────────────────────────────────────────────────────────┘
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                     FILTROS HTTP                                 │
├─────────────────────────────────────────────────────────────────┤
│  1. RequestLoggingFilter (Logging + MDC + Correlación)          │
│  2. ETagFilter (Cache HTTP)                                      │
│  3. RateLimitingFilter (Protección DoS)                          │
└─────────────────────────────────────────────────────────────────┘
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    INTERCEPTORES                                 │
├─────────────────────────────────────────────────────────────────┤
│  - RequestIdInterceptor (ID único por request)                   │
└─────────────────────────────────────────────────────────────────┘
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                  BASE CONTROLLER                                 │
├─────────────────────────────────────────────────────────────────┤
│  GET    /           → findAll()     (Paginación + Filtrado)     │
│  HEAD   /           → count()                                    │
│  GET    /{id}       → findById()    (ETag + Cache)              │
│  HEAD   /{id}       → existsById()                               │
│  POST   /           → save()        (Location header)            │
│  PUT    /{id}       → update()      (If-Match)                  │
│  PATCH  /{id}       → partialUpdate()                            │
│  DELETE /{id}       → delete()      (Soft/Hard)                 │
│  POST   /{id}/restore → restore()                                │
└─────────────────────────────────────────────────────────────────┘
           │                    │                    │
           ▼                    ▼                    ▼
┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐
│ FilterBuilder    │  │ BaseMapper       │  │ ResourceAssembler│
│ (Specifications) │  │ (DTO ↔ Entity)   │  │ (HATEOAS Links)  │
└──────────────────┘  └──────────────────┘  └──────────────────┘
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                      BASE SERVICE                                │
│  (Lógica de negocio + Eventos + Result pattern)                 │
└─────────────────────────────────────────────────────────────────┘
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                   BASE REPOSITORY                                │
│  (JPA + Specifications + Soft Delete)                            │
└─────────────────────────────────────────────────────────────────┘
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                       DATABASE                                   │
└─────────────────────────────────────────────────────────────────┘

                    ┌─ EN CASO DE ERROR ─┐
                    ▼                     │
┌─────────────────────────────────────────────────────────────────┐
│              GLOBAL EXCEPTION HANDLER                            │
├─────────────────────────────────────────────────────────────────┤
│  - ResourceNotFoundException      → 404                          │
│  - ValidationException            → 400                          │
│  - DuplicateResourceException     → 409                          │
│  - PreconditionFailedException    → 412                          │
│  - UnauthorizedActionException    → 403                          │
│  - OperationFailedException       → 500                          │
│                                                                   │
│  Respuestas: RFC 7807 Problem Detail (application/problem+json)  │
└─────────────────────────────────────────────────────────────────┘
```

---

## Resumen

La **capa de infraestructura** de APiGen proporciona:

### 1. Controladores REST Completos
- **BaseController/BaseControllerImpl**: Operaciones CRUD genéricas
- **Headers HTTP**: Location, ETag, Last-Modified, X-Total-Count
- **Validación**: Bean Validation (@Valid)
- **Concurrencia optimista**: If-Match/If-None-Match
- **Paginación**: Spring Data Pageable
- **Sparse fieldsets**: Selección de campos

### 2. Manejo de Errores Profesional
- **GlobalExceptionHandler**: Centralizado
- **RFC 7807**: Problem Details estándar
- **ApiError**: Type-safe con sealed interfaces
- **ProblemDetail**: Builder pattern
- **Logging**: Estructurado con contexto

### 3. Configuraciones Spring
- **JpaConfig**: Auditoría automática
- **WebConfig**: Interceptores y negociación de contenido
- **OpenApiConfig**: Documentación Swagger
- **AsyncConfig**: Virtual Threads (Java 21+)
- **ETagConfig**: Cache HTTP

### 4. Filtros y Seguridad
- **RequestLoggingFilter**: Trazabilidad completa
- **RateLimitingFilter**: Protección DoS
- **RequestIdInterceptor**: Correlación de requests
- **Sanitización**: Datos sensibles

### 5. HATEOAS
- **BaseResourceAssembler**: Links automáticos
- **Navegabilidad**: self, collection, update, delete
- **Paginación**: first, prev, next, last

### 6. Filtrado Dinámico
- **FilterSpecificationBuilder**: Query language simple
- **12 operadores**: eq, like, gt, in, between, etc.
- **Relaciones**: Soporte para joins
- **Type-safe**: Conversión automática de tipos
- **BaseSpecification**: Queries predefinidas

Esta infraestructura proporciona una base sólida, profesional y altamente reutilizable para construir APIs REST en Spring Boot.

## Ejemplo Practico: ProductController

En `apigen-example`, el ProductController extiende BaseControllerImpl:

```java
package com.jnzader.example.infrastructure.controller;

import com.jnzader.apigen.core.infrastructure.controller.BaseControllerImpl;
import com.jnzader.example.domain.entity.Product;
import com.jnzader.example.application.dto.ProductDTO;
import com.jnzader.example.application.mapper.ProductMapper;
import com.jnzader.example.application.service.ProductService;
import com.jnzader.example.infrastructure.hateoas.ProductResourceAssembler;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
public class ProductController extends BaseControllerImpl<Product, ProductDTO, Long> {

    public ProductController(
            ProductService productService,
            ProductMapper productMapper,
            ProductResourceAssembler resourceAssembler) {
        super(productService, productMapper, resourceAssembler);
    }

    @Override
    protected Class<Product> getEntityClass() {
        return Product.class;
    }

    @Override
    protected String getResourceName() {
        return "Product";
    }
}
```

Con esto, automaticamente tienes disponibles:

```bash
# Listar con paginacion
GET /api/products?page=0&size=20

# Filtrar
GET /api/products?filter=price:gte:100,name:like:laptop

# Obtener por ID (con ETag)
GET /api/products/1

# Crear
POST /api/products

# Actualizar (con If-Match para concurrencia)
PUT /api/products/1

# Soft delete
DELETE /api/products/1

# Hard delete
DELETE /api/products/1?permanent=true

# Restaurar
POST /api/products/1/restore
```

---

**Anterior:** [03-APLICACION-BASE.md](./03-APLICACION-BASE.md)
**Siguiente:** [05-SEGURIDAD-JWT.md](./05-SEGURIDAD-JWT.md)
