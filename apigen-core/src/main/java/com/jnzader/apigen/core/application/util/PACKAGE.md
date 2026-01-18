# Utilities

Este paquete contiene **utilidades** genéricas usadas en toda la aplicación.

## Archivos

| Archivo | Descripción |
|---------|-------------|
| `Result.java` | Tipo Result<T,E> para manejo funcional de errores |

## Result<T, E>

### Propósito

`Result` es un tipo que representa el resultado de una operación que puede **tener éxito** (con un valor) o **fallar** (con un error), sin usar excepciones para control de flujo.

### Definición

```java
public sealed interface Result<T, E> permits Result.Success, Result.Failure {

    record Success<T, E>(T value) implements Result<T, E> { }
    record Failure<T, E>(E error) implements Result<T, E> { }
}
```

### Por qué usar Result?

| Aspecto | Excepciones | Result<T,E> |
|---------|-------------|-------------|
| Visibilidad | Oculto en firma | Explícito en tipo retorno |
| Flujo de control | GOTO invisible | Datos normales |
| Composición | try-catch anidados | map/flatMap fluido |
| Performance | Stacktrace costoso | Solo objeto nuevo |
| Manejo obligatorio | Runtime error si olvidas | Compilador te obliga |

### Crear Results

```java
// Éxito
Result<User, Exception> success = Result.success(user);

// Fallo
Result<User, Exception> failure = Result.failure(new UserNotFoundException(id));

// Desde operación que puede lanzar excepción
Result<User, Exception> result = Result.of(() -> repository.findById(id).orElseThrow());

// Desde Optional
Result<User, Exception> result = Result.fromOptional(
    optional,
    () -> new UserNotFoundException(id)
);
```

### Consultar Estado

```java
Result<User, Exception> result = service.findById(id);

// Verificar estado
if (result.isSuccess()) {
    User user = result.getValue();
}

if (result.isFailure()) {
    Exception error = result.getError();
}

// Con callbacks
result.ifSuccess(user -> log.info("Found: {}", user));
result.ifFailure(error -> log.error("Failed: {}", error.getMessage()));
```

### Transformar con map()

`map()` transforma el valor de éxito, dejando el error intacto:

```java
Result<User, Exception> userResult = service.findById(id);

// Transformar User → UserDTO
Result<UserDTO, Exception> dtoResult = userResult.map(user -> mapper.toDto(user));

// Encadenar transformaciones
Result<String, Exception> nameResult = userResult
    .map(User::getName)
    .map(String::toUpperCase);
```

### Encadenar con flatMap()

`flatMap()` encadena operaciones que también retornan Result:

```java
// Cada operación puede fallar independientemente
Result<Order, Exception> orderResult = userService.findById(userId)
    .flatMap(user -> cartService.getCart(user))
    .flatMap(cart -> orderService.createOrder(cart))
    .flatMap(order -> paymentService.processPayment(order));
```

### Recuperar con recover()

`recover()` convierte un fallo en éxito:

```java
Result<User, Exception> result = service.findById(id)
    .recover(error -> {
        if (error instanceof NotFoundException) {
            return createDefaultUser();
        }
        throw error;  // Re-lanzar si no podemos recuperar
    });
```

### Obtener Valor con fold()

`fold()` maneja ambos casos y retorna un valor final:

```java
// Convertir Result a ResponseEntity
ResponseEntity<UserDTO> response = userService.findById(id)
    .map(mapper::toDto)
    .fold(
        dto -> ResponseEntity.ok(dto),
        error -> {
            if (error instanceof NotFoundException) {
                throw (NotFoundException) error;  // Handler global
            }
            throw new RuntimeException(error);
        }
    );
```

### Filtrar con filter()

```java
Result<User, Exception> activeUser = service.findById(id)
    .filter(
        user -> user.isActive(),
        user -> new InactiveUserException(user.getId())
    );
```

### Combinar Results

```java
// Combinar dos Results
Result<Pair<User, Order>, Exception> combined =
    userResult.zip(orderResult, (user, order) -> Pair.of(user, order));

// Ejecutar solo si ambos son éxito
Result<Invoice, Exception> invoice = userResult.zipWith(orderResult,
    (user, order) -> invoiceService.create(user, order));
```

## API Completa

### Métodos de Creación

| Método | Descripción |
|--------|-------------|
| `Result.success(value)` | Crear éxito con valor |
| `Result.failure(error)` | Crear fallo con error |
| `Result.of(supplier)` | Ejecutar y capturar excepciones |
| `Result.fromOptional(opt, errorSupplier)` | Convertir Optional |

### Métodos de Consulta

| Método | Descripción |
|--------|-------------|
| `isSuccess()` | true si es éxito |
| `isFailure()` | true si es fallo |
| `getValue()` | Obtener valor (null si fallo) |
| `getError()` | Obtener error (null si éxito) |
| `getOrElse(default)` | Valor o default si fallo |
| `getOrElseGet(supplier)` | Valor o resultado de supplier |

### Métodos de Transformación

| Método | Descripción |
|--------|-------------|
| `map(fn)` | Transformar valor de éxito |
| `mapError(fn)` | Transformar error |
| `flatMap(fn)` | Encadenar operación que retorna Result |
| `filter(pred, errorFn)` | Filtrar con predicado |
| `recover(fn)` | Recuperar de error |
| `fold(onSuccess, onFailure)` | Manejar ambos casos |

### Métodos de Efectos

| Método | Descripción |
|--------|-------------|
| `ifSuccess(consumer)` | Ejecutar si éxito |
| `ifFailure(consumer)` | Ejecutar si fallo |
| `peek(consumer)` | Inspeccionar valor sin modificar |

## Patrones Comunes

### En Servicios

```java
@Override
public Result<Product, Exception> findById(Long id) {
    return Result.of(() ->
        repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product", id))
    );
}

@Override
public Result<Product, Exception> updateStock(Long id, int delta) {
    return findById(id)
        .flatMap(product -> {
            if (product.getStock() + delta < 0) {
                return Result.failure(new InsufficientStockException());
            }
            product.setStock(product.getStock() + delta);
            return save(product);
        });
}
```

### En Controllers

```java
@GetMapping("/{id}")
public ResponseEntity<EntityModel<ProductDTO>> findById(@PathVariable Long id) {
    return productService.findById(id)
        .map(mapper::toDto)
        .map(dto -> EntityModel.of(dto, createLinks(dto)))
        .fold(
            ResponseEntity::ok,
            this::handleError
        );
}

private ResponseEntity<EntityModel<ProductDTO>> handleError(Exception e) {
    if (e instanceof ResourceNotFoundException) {
        throw (ResourceNotFoundException) e;
    }
    throw new RuntimeException(e);
}
```

### Railway-Oriented Programming

```java
// Múltiples pasos, cualquiera puede fallar
Result<OrderConfirmation, Exception> processOrder(OrderRequest request) {
    return validateRequest(request)          // Result<ValidatedRequest, E>
        .flatMap(this::checkInventory)       // Result<InventoryCheck, E>
        .flatMap(this::reserveItems)         // Result<Reservation, E>
        .flatMap(this::processPayment)       // Result<Payment, E>
        .flatMap(this::createOrder)          // Result<Order, E>
        .flatMap(this::sendConfirmation);    // Result<OrderConfirmation, E>
}
```

## Testing

```java
@Test
void shouldMapSuccessValue() {
    Result<Integer, String> result = Result.success(5);

    Result<Integer, String> mapped = result.map(x -> x * 2);

    assertThat(mapped.isSuccess()).isTrue();
    assertThat(mapped.getValue()).isEqualTo(10);
}

@Test
void shouldNotMapFailure() {
    Result<Integer, String> result = Result.failure("error");

    Result<Integer, String> mapped = result.map(x -> x * 2);

    assertThat(mapped.isFailure()).isTrue();
    assertThat(mapped.getError()).isEqualTo("error");
}

@Test
void shouldFlatMapSuccess() {
    Result<Integer, String> result = Result.success(5);

    Result<String, String> flatMapped = result.flatMap(x ->
        x > 0 ? Result.success("positive") : Result.failure("not positive")
    );

    assertThat(flatMapped.getValue()).isEqualTo("positive");
}
```

## Buenas Prácticas

1. **Retornar Result desde servicios**, lanzar excepciones desde controllers
2. **Usar flatMap para operaciones dependientes**, map para transformaciones simples
3. **No anidar Results** - `Result<Result<T,E>,E>` indica mal diseño
4. **Documentar errores posibles** - Javadoc qué errores puede contener E
5. **Preferir Result sobre Optional** cuando hay múltiples tipos de fallo
