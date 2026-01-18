# Data Transfer Objects (DTOs)

Este paquete contiene los **DTOs base** y grupos de validación para transferencia de datos en la API.

## Archivos

| Archivo | Descripción |
|---------|-------------|
| `BaseDTO.java` | DTO base con campos de auditoría |
| `validation/OnCreate.java` | Grupo de validación para creación |
| `validation/OnUpdate.java` | Grupo de validación para actualización |

## BaseDTO

### Propósito
Proporcionar campos comunes que todos los DTOs de respuesta deben incluir.

### Campos

```java
public abstract class BaseDTO {
    private Long id;
    private Boolean estado;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
    private String creadoPor;
    private String modificadoPor;
    private Long version;
}
```

### Ejemplo de DTO Específico

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class ProductDTO extends BaseDTO {

    @NotBlank(message = "El nombre es requerido", groups = OnCreate.class)
    @Size(min = 2, max = 100, groups = {OnCreate.class, OnUpdate.class})
    private String name;

    @NotNull(message = "El precio es requerido", groups = OnCreate.class)
    @Positive(message = "El precio debe ser positivo", groups = {OnCreate.class, OnUpdate.class})
    private BigDecimal price;

    @PositiveOrZero(groups = {OnCreate.class, OnUpdate.class})
    private Integer stock;
}
```

## Grupos de Validación

### Problema que Resuelven

Diferentes operaciones requieren diferentes validaciones:
- **Create**: Todos los campos requeridos obligatorios
- **Update**: Campos opcionales (PATCH parcial)
- **Read**: Sin validación

### OnCreate

```java
public interface OnCreate { }
```

Usado para validaciones que **solo aplican al crear**:

```java
@NotBlank(groups = OnCreate.class)  // Requerido solo al crear
private String sku;
```

### OnUpdate

```java
public interface OnUpdate { }
```

Usado para validaciones que **solo aplican al actualizar**:

```java
@NotNull(groups = OnUpdate.class)   // Requerido solo al actualizar
private Long version;               // Para optimistic locking
```

### Sin Grupo (Default)

Validaciones sin grupo aplican siempre:

```java
@Email                              // Siempre debe ser email válido
private String email;
```

## Uso en Controllers

```java
// POST - Solo validaciones de OnCreate
@PostMapping
public ResponseEntity<?> create(
    @Validated(OnCreate.class) @RequestBody ProductDTO dto) { }

// PUT - Validaciones de OnUpdate
@PutMapping("/{id}")
public ResponseEntity<?> update(
    @PathVariable Long id,
    @Validated(OnUpdate.class) @RequestBody ProductDTO dto) { }

// PATCH - Sin validación estricta (campos opcionales)
@PatchMapping("/{id}")
public ResponseEntity<?> partialUpdate(
    @PathVariable Long id,
    @RequestBody ProductDTO dto) { }
```

## Patrón: Request vs Response DTOs

Para APIs complejas, considera separar:

```java
// Request (entrada)
public class CreateProductRequest {
    @NotBlank private String name;
    @NotNull private BigDecimal price;
    // Sin campos de auditoría
}

// Response (salida)
public class ProductResponse extends BaseDTO {
    private String name;
    private BigDecimal price;
    // Con campos de auditoría
}
```

**APiGen usa un solo DTO** para simplicidad, pero puedes separar si necesitas.

## Validaciones Comunes

| Anotación | Uso |
|-----------|-----|
| `@NotNull` | Campo no puede ser null |
| `@NotBlank` | String no vacío ni solo espacios |
| `@NotEmpty` | Collection/Array no vacío |
| `@Size(min, max)` | Longitud de string/collection |
| `@Min`, `@Max` | Valor numérico mínimo/máximo |
| `@Positive` | Número > 0 |
| `@PositiveOrZero` | Número >= 0 |
| `@Email` | Formato email válido |
| `@Pattern(regexp)` | Expresión regular |
| `@Past`, `@Future` | Fecha pasada/futura |
| `@Valid` | Validar objeto anidado |

## Validaciones Personalizadas

```java
// 1. Crear anotación
@Target({FIELD})
@Retention(RUNTIME)
@Constraint(validatedBy = SkuFormatValidator.class)
public @interface ValidSku {
    String message() default "SKU inválido (formato: XXX-000)";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

// 2. Crear validador
public class SkuFormatValidator implements ConstraintValidator<ValidSku, String> {
    private static final Pattern SKU_PATTERN = Pattern.compile("^[A-Z]{3}-\\d{3}$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || SKU_PATTERN.matcher(value).matches();
    }
}

// 3. Usar en DTO
public class ProductDTO extends BaseDTO {
    @ValidSku(groups = OnCreate.class)
    private String sku;
}
```

## Mapeo de Errores de Validación

Los errores de validación se convierten en:

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

## Buenas Prácticas

1. **Mensajes en español** (o i18n): `message = "El nombre es requerido"`
2. **Grupos apropiados**: No todo requiere grupo, solo lo que varía
3. **DTOs inmutables para respuesta**: Considera `record` para responses
4. **No exponer campos internos**: `version` puede ser solo en headers (ETag)
5. **Validar en frontend también**: DTOs validan, pero UX requiere validación client-side
