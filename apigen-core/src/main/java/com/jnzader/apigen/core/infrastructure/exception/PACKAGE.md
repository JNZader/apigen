# Exception Handling

Este paquete contiene el **manejador global de excepciones** que convierte excepciones en respuestas HTTP.

## Archivos

| Archivo | Descripción |
|---------|-------------|
| `GlobalExceptionHandler.java` | @RestControllerAdvice para todas las excepciones |

## GlobalExceptionHandler

Captura excepciones y las convierte en respuestas **RFC 7807 Problem Details**:

```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(ResourceNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.NOT_FOUND,
            ex.getMessage()
        );
        problem.setProperty("code", "RESOURCE_NOT_FOUND");
        problem.setProperty("timestamp", Instant.now());

        return ResponseEntity.status(404).body(problem);
    }
}
```

## Formato de Respuesta (RFC 7807)

```json
{
  "type": "about:blank",
  "title": "Not Found",
  "status": 404,
  "detail": "Product con id '999' no encontrado",
  "instance": "/api/v1/products/999",
  "code": "RESOURCE_NOT_FOUND",
  "timestamp": "2024-01-15T10:30:00Z",
  "traceId": "abc123"
}
```

## Excepciones Manejadas

| Excepción | HTTP Status | Descripción |
|-----------|-------------|-------------|
| `ResourceNotFoundException` | 404 | Recurso no existe |
| `DuplicateResourceException` | 409 | Violación de unique |
| `ValidationException` | 400 | Validación de negocio |
| `MethodArgumentNotValidException` | 400 | Bean Validation (@Valid) |
| `ConstraintViolationException` | 400 | Constraint JPA |
| `ConcurrencyException` | 409 | Optimistic lock failed |
| `OptimisticLockException` | 409 | Version mismatch |
| `AccessDeniedException` | 403 | Sin permisos |
| `AuthenticationException` | 401 | No autenticado |
| `BusinessException` | 422 | Error de negocio genérico |
| `Exception` | 500 | Error no esperado |

## Handlers Específicos

### Validación de Bean Validation

```java
@Override
protected ResponseEntity<Object> handleMethodArgumentNotValid(
        MethodArgumentNotValidException ex,
        HttpHeaders headers,
        HttpStatusCode status,
        WebRequest request) {

    ProblemDetail problem = ProblemDetail.forStatusAndDetail(
        HttpStatus.BAD_REQUEST,
        "Validation failed"
    );

    List<Map<String, Object>> errors = ex.getBindingResult()
        .getFieldErrors()
        .stream()
        .map(error -> Map.of(
            "field", error.getField(),
            "message", error.getDefaultMessage(),
            "rejectedValue", error.getRejectedValue()
        ))
        .toList();

    problem.setProperty("errors", errors);

    return ResponseEntity.badRequest().body(problem);
}
```

**Respuesta:**
```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Validation failed",
  "errors": [
    {
      "field": "name",
      "message": "El nombre es requerido",
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

### Concurrencia (Optimistic Lock)

```java
@ExceptionHandler(OptimisticLockException.class)
public ResponseEntity<ProblemDetail> handleOptimisticLock(OptimisticLockException ex) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(
        HttpStatus.CONFLICT,
        "El recurso fue modificado por otro usuario. Por favor, recarga y vuelve a intentar."
    );
    problem.setProperty("code", "CONCURRENT_MODIFICATION");

    return ResponseEntity.status(409).body(problem);
}
```

### Error Genérico (500)

```java
@ExceptionHandler(Exception.class)
public ResponseEntity<ProblemDetail> handleGeneric(Exception ex, WebRequest request) {
    // Loguear stack trace completo
    log.error("Unhandled exception", ex);

    ProblemDetail problem = ProblemDetail.forStatusAndDetail(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Error interno del servidor"  // No exponer detalles
    );
    problem.setProperty("code", "INTERNAL_ERROR");
    problem.setProperty("traceId", MDC.get("requestId"));

    return ResponseEntity.status(500).body(problem);
}
```

## Agregar Handler Custom

```java
@RestControllerAdvice
public class CustomExceptionHandler {

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ProblemDetail> handleInsufficientStock(InsufficientStockException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.UNPROCESSABLE_ENTITY,  // 422
            ex.getMessage()
        );
        problem.setProperty("code", "INSUFFICIENT_STOCK");
        problem.setProperty("productId", ex.getProductId());
        problem.setProperty("requested", ex.getRequested());
        problem.setProperty("available", ex.getAvailable());

        return ResponseEntity.status(422).body(problem);
    }
}
```

## Logging de Excepciones

```java
@ExceptionHandler(BusinessException.class)
public ResponseEntity<ProblemDetail> handleBusiness(BusinessException ex) {
    // Log WARN para errores de negocio (esperados)
    log.warn("Business error: {} - {}", ex.getCode(), ex.getMessage());

    return buildResponse(ex, HttpStatus.UNPROCESSABLE_ENTITY);
}

@ExceptionHandler(Exception.class)
public ResponseEntity<ProblemDetail> handleGeneric(Exception ex) {
    // Log ERROR para errores inesperados
    log.error("Unexpected error", ex);

    return buildResponse(ex, HttpStatus.INTERNAL_SERVER_ERROR);
}
```

## Testing

```java
@WebMvcTest(ProductController.class)
class ExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    @Test
    void shouldReturn404WhenProductNotFound() throws Exception {
        when(productService.findById(999L))
            .thenReturn(Result.failure(new ResourceNotFoundException("Product", 999L)));

        mockMvc.perform(get("/api/v1/products/999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.detail").value(containsString("999")));
    }

    @Test
    void shouldReturn400OnValidationError() throws Exception {
        ProductDTO invalid = new ProductDTO();  // Sin campos requeridos

        mockMvc.perform(post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalid)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors").isArray());
    }
}
```

## Buenas Prácticas

1. **No exponer detalles internos** - Stack traces, queries SQL
2. **Códigos únicos** - Para búsqueda en docs y logs
3. **Mensajes amigables** - El usuario final los ve
4. **Logging apropiado** - WARN para esperados, ERROR para inesperados
5. **Incluir traceId** - Para correlación en soporte
