# Domain Entities

Este paquete contiene la **entidad base** de la cual heredan todas las entidades del sistema.

## Archivos

| Archivo | Descripción |
|---------|-------------|
| `Base.java` | Clase abstracta base para todas las entidades |

## Base.java

### Propósito
Proporcionar campos y comportamientos comunes a **todas** las entidades del sistema.

### Campos Heredados

| Campo | Tipo | Descripción | Auto-generado |
|-------|------|-------------|---------------|
| `id` | Long | Identificador único (PK) | Sí (secuencia) |
| `estado` | Boolean | Flag de soft delete (true=activo) | Default true |
| `fechaCreacion` | LocalDateTime | Timestamp de creación | Sí (@CreatedDate) |
| `fechaActualizacion` | LocalDateTime | Timestamp última modificación | Sí (@LastModifiedDate) |
| `fechaEliminacion` | LocalDateTime | Timestamp de soft delete | Manual |
| `creadoPor` | String | Usuario que creó | Sí (@CreatedBy) |
| `modificadoPor` | String | Usuario última modificación | Sí (@LastModifiedBy) |
| `eliminadoPor` | String | Usuario que eliminó | Manual |
| `version` | Long | Versión para optimistic locking | Sí (@Version) |

### Métodos Heredados

```java
// Soft delete
void softDelete()                    // Marca estado=false, fechaEliminacion=now
void softDelete(String usuario)      // + registra eliminadoPor

// Restauración
void restore()                       // Marca estado=true, limpia campos eliminación

// Eventos de dominio
void registerEvent(Object event)     // Registra evento para publicación
Collection<Object> domainEvents()    // Retorna eventos pendientes (@DomainEvents)
void clearDomainEvents()             // Limpia eventos (@AfterDomainEventPublication)

// Auditoría
boolean isNew()                      // true si id == null (para @CreatedDate)
```

### Anotaciones Clave

```java
@MappedSuperclass           // No crea tabla propia, campos van a subclases
@Audited                    // Hibernate Envers versiona automáticamente
@EntityListeners(...)       // Para @CreatedDate, @LastModifiedDate
@SQLRestriction("estado = true")  // Filtra soft-deleted en queries
```

### Ejemplo de Uso

```java
@Entity
@Table(name = "products")
@Audited
public class Product extends Base {

    private String name;
    private BigDecimal price;

    // Los campos de Base se heredan automáticamente:
    // id, estado, fechaCreacion, creadoPor, version, etc.
}
```

### Estrategia de ID

```java
@Id
@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "base_seq")
@SequenceGenerator(name = "base_seq", sequenceName = "base_sequence", allocationSize = 50)
private Long id;
```

- **Secuencia compartida**: Todas las entidades usan `base_sequence`
- **allocationSize=50**: Reduce round-trips a DB para IDs
- **Long**: Soporta ~9 quintillones de registros

### Soft Delete con @SQLRestriction

```java
@SQLRestriction("estado = true")
public abstract class Base { }
```

Esto hace que **todas las queries JPA** automáticamente filtren:
```sql
SELECT * FROM products WHERE ... AND estado = true
```

Para incluir eliminados, usar query nativa o `@Query` con `estado = false`.

## Cómo Extender

```java
// 1. Heredar de Base
public class Product extends Base {

    // 2. Agregar campos específicos
    private String sku;

    // 3. Agregar lógica de negocio
    public void updateStock(int delta) {
        this.stock += delta;
        if (this.stock == 0) {
            registerEvent(new OutOfStockEvent(this.id));
        }
    }
}
```

## Testing

```java
@Test
void shouldInheritBaseFields() {
    Product product = new Product();
    product.setName("Test");

    // Campos de Base disponibles
    assertThat(product.getEstado()).isTrue();
    assertThat(product.getVersion()).isNull(); // Se asigna en persist
}
```
