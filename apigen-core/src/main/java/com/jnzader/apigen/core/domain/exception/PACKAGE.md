# Domain Exceptions

Este paquete contiene las **excepciones de negocio** que representan errores del dominio.

## Archivos

| Archivo | Descripción | HTTP Status |
|---------|-------------|-------------|
| `BusinessException.java` | Excepción base de negocio | 400/422 |
| `ResourceNotFoundException.java` | Recurso no encontrado | 404 |
| `DuplicateResourceException.java` | Recurso duplicado | 409 |
| `ValidationException.java` | Error de validación | 400 |
| `ConcurrencyException.java` | Conflicto de versión | 409 |
| `UnauthorizedException.java` | No autorizado | 401 |
| `ForbiddenException.java` | Acceso denegado | 403 |

## Jerarquía

```
RuntimeException
└── BusinessException (base)
    ├── ResourceNotFoundException (404)
    ├── DuplicateResourceException (409)
    ├── ValidationException (400)
    ├── ConcurrencyException (409)
    ├── UnauthorizedException (401)
    └── ForbiddenException (403)
```

## Diseño

### Por qué RuntimeException?
- No obliga a declarar `throws` en cada método
- Se propagan automáticamente hasta el `GlobalExceptionHandler`
- El patrón `Result<T,E>` se usa para errores esperados en el flujo normal

### Cuándo usar Excepciones vs Result
| Situación | Usar |
|-----------|------|
| Error inesperado (bug) | Exception |
| Validación de entrada | Exception (400) |
| Recurso no existe | Exception (404) |
| Regla de negocio violada | Result.failure() o Exception |
| Operación puede fallar normalmente | Result<T,E> |

## Excepciones Detalladas

### BusinessException (Base)

```java
public class BusinessException extends RuntimeException {
    private final String code;           // Código único del error
    private final String userMessage;    // Mensaje para el usuario
    private final Map<String, Object> details;  // Datos adicionales

    // Constructores
    BusinessException(String message)
    BusinessException(String message, String code)
    BusinessException(String message, String code, Map<String, Object> details)
}
```

**Uso**: Base para todas las excepciones de negocio.

### ResourceNotFoundException

```java
public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String resourceType, String field, Object value) {
        super(String.format("%s con %s '%s' no encontrado", resourceType, field, value));
    }

    public ResourceNotFoundException(String resourceType, Long id) {
        this(resourceType, "id", id);
    }
}
```

**Uso**: Cuando un recurso solicitado no existe.

```java
// Ejemplo
Product product = repository.findById(id)
    .orElseThrow(() -> new ResourceNotFoundException("Product", id));
```

### DuplicateResourceException

```java
public class DuplicateResourceException extends BusinessException {

    public DuplicateResourceException(String resourceType, String field, Object value) {
        super(String.format("%s con %s '%s' ya existe", resourceType, field, value));
    }
}
```

**Uso**: Cuando se intenta crear un recurso que viola constraint único.

```java
// Ejemplo
if (repository.existsByEmail(email)) {
    throw new DuplicateResourceException("User", "email", email);
}
```

### ValidationException

```java
public class ValidationException extends BusinessException {
    private final List<FieldError> errors;

    public record FieldError(String field, String message, Object rejectedValue) {}
}
```

**Uso**: Errores de validación complejos (más allá de Bean Validation).

```java
// Ejemplo
List<FieldError> errors = new ArrayList<>();
if (startDate.isAfter(endDate)) {
    errors.add(new FieldError("startDate", "debe ser anterior a endDate", startDate));
}
if (!errors.isEmpty()) {
    throw new ValidationException(errors);
}
```

### ConcurrencyException

```java
public class ConcurrencyException extends BusinessException {

    public ConcurrencyException(String resourceType, Long id) {
        super(String.format("%s con id %d fue modificado por otro usuario", resourceType, id));
    }
}
```

**Uso**: Cuando optimistic locking detecta conflicto.

```java
// Se convierte automáticamente desde OptimisticLockException
catch (OptimisticLockException e) {
    throw new ConcurrencyException("Product", id);
}
```

## Mapeo a HTTP

El `GlobalExceptionHandler` mapea excepciones a respuestas HTTP:

| Excepción | HTTP Status | Response |
|-----------|-------------|----------|
| `ResourceNotFoundException` | 404 Not Found | Problem Details |
| `DuplicateResourceException` | 409 Conflict | Problem Details |
| `ValidationException` | 400 Bad Request | Problem Details + errors |
| `ConcurrencyException` | 409 Conflict | Problem Details |
| `UnauthorizedException` | 401 Unauthorized | Problem Details |
| `ForbiddenException` | 403 Forbidden | Problem Details |
| `BusinessException` | 422 Unprocessable Entity | Problem Details |

## Formato de Respuesta (RFC 7807)

```json
{
  "type": "about:blank",
  "title": "Not Found",
  "status": 404,
  "detail": "Product con id '999' no encontrado",
  "instance": "/api/v1/products/999",
  "timestamp": "2024-01-15T10:30:00Z",
  "code": "RESOURCE_NOT_FOUND",
  "traceId": "abc123"
}
```

## Crear Excepciones Personalizadas

```java
// 1. Extender BusinessException
public class InsufficientStockException extends BusinessException {

    public InsufficientStockException(String productName, int requested, int available) {
        super(
            String.format("Stock insuficiente para '%s'. Solicitado: %d, Disponible: %d",
                productName, requested, available),
            "INSUFFICIENT_STOCK",
            Map.of(
                "productName", productName,
                "requested", requested,
                "available", available
            )
        );
    }
}

// 2. Registrar en GlobalExceptionHandler (opcional para status custom)
@ExceptionHandler(InsufficientStockException.class)
public ResponseEntity<ProblemDetail> handleInsufficientStock(InsufficientStockException ex) {
    return ResponseEntity.status(422).body(createProblemDetail(ex));
}

// 3. Usar en servicio
if (product.getStock() < quantity) {
    throw new InsufficientStockException(product.getName(), quantity, product.getStock());
}
```

## Testing

```java
@Test
void shouldThrowResourceNotFoundWhenProductDoesNotExist() {
    assertThatThrownBy(() -> productService.findById(999L))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Product")
        .hasMessageContaining("999");
}
```

## Buenas Prácticas

1. **Mensajes descriptivos**: Incluir qué, por qué, y contexto
2. **Códigos únicos**: Facilita búsqueda en logs y documentación
3. **No exponer detalles internos**: No incluir stack traces o queries SQL
4. **Inmutabilidad**: Excepciones deben ser inmutables
5. **Logging**: El handler loguea, no la excepción misma
