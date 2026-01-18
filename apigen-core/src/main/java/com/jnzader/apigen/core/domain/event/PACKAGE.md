# Domain Events

Este paquete contiene los **eventos de dominio** que se publican automáticamente cuando ocurren cambios en las entidades.

## Archivos

| Archivo | Descripción |
|---------|-------------|
| `EntityCreatedEvent.java` | Publicado al crear una entidad |
| `EntityUpdatedEvent.java` | Publicado al actualizar una entidad |
| `EntityDeletedEvent.java` | Publicado al hacer soft delete |
| `EntityRestoredEvent.java` | Publicado al restaurar una entidad |
| `EntityHardDeletedEvent.java` | Publicado al eliminar permanentemente |

## Propósito

Los eventos de dominio permiten:
1. **Desacoplar** productores y consumidores de cambios
2. **Extender** comportamiento sin modificar código existente
3. **Auditar** acciones del sistema
4. **Sincronizar** sistemas externos de forma asíncrona

## Estructura de Eventos

Todos los eventos son **records** (inmutables):

```java
public record EntityCreatedEvent<E extends Base>(
    E entity,              // La entidad creada
    String entityType,     // Nombre de la clase
    Long entityId,         // ID de la entidad
    Instant timestamp      // Momento del evento
) { }
```

## Ciclo de Vida

```
1. Servicio llama save(entity)
     │
     ▼
2. BaseServiceImpl registra evento
   entity.registerEvent(new EntityCreatedEvent<>(entity))
     │
     ▼
3. Spring detecta @DomainEvents en entidad
     │
     ▼
4. ApplicationEventPublisher publica evento
     │
     ▼
5. @EventListener métodos reciben evento
     │
     ▼
6. @AfterDomainEventPublication limpia eventos
```

## Cómo Escuchar Eventos

### Listener Síncrono

```java
@Component
public class ProductEventListener {

    @EventListener
    public void onProductCreated(EntityCreatedEvent<Product> event) {
        log.info("Producto creado: {}", event.entityId());
        // Ejecuta en el mismo thread/transacción
    }
}
```

### Listener Asíncrono (Recomendado)

```java
@Component
public class ProductEventListener {

    @Async
    @EventListener
    public void onProductCreated(EntityCreatedEvent<Product> event) {
        log.info("Producto creado: {}", event.entityId());
        // Ejecuta en thread separado
        // Transacción ya committed
        notificationService.sendEmail(...);
    }
}
```

### Filtrar por Tipo de Entidad

```java
@EventListener(condition = "#event.entityType == 'Product'")
public void onProductOnly(EntityCreatedEvent<?> event) {
    // Solo para Product
}
```

## Eventos Disponibles

### EntityCreatedEvent
- **Cuándo**: Después de `save()` en entidad nueva (id era null)
- **Datos**: Entidad completa con ID asignado
- **Uso típico**: Enviar email de bienvenida, indexar en búsqueda

### EntityUpdatedEvent
- **Cuándo**: Después de `save()` en entidad existente
- **Datos**: Entidad con nuevos valores
- **Uso típico**: Invalidar cache externo, notificar cambios

### EntityDeletedEvent
- **Cuándo**: Después de `softDelete()`
- **Datos**: Entidad marcada como eliminada
- **Uso típico**: Audit log, cleanup de recursos relacionados

### EntityRestoredEvent
- **Cuándo**: Después de `restore()`
- **Datos**: Entidad restaurada
- **Uso típico**: Re-indexar, notificar restauración

### EntityHardDeletedEvent
- **Cuándo**: Después de `hardDelete()`
- **Datos**: ID y tipo de entidad (entidad ya no existe)
- **Uso típico**: Cleanup permanente, eliminar de índices

## Crear Eventos Personalizados

```java
// 1. Definir el evento (record recomendado)
public record LowStockEvent(
    Long productId,
    String sku,
    int currentStock,
    Instant timestamp
) {
    public LowStockEvent(Long productId, String sku, int currentStock) {
        this(productId, sku, currentStock, Instant.now());
    }
}

// 2. Publicar desde la entidad o servicio
public class Product extends Base {
    public void decrementStock(int quantity) {
        this.stock -= quantity;
        if (this.stock < 10) {
            registerEvent(new LowStockEvent(this.id, this.sku, this.stock));
        }
    }
}

// 3. Escuchar el evento
@Component
public class InventoryAlertListener {

    @Async
    @EventListener
    public void onLowStock(LowStockEvent event) {
        alertService.sendLowStockAlert(event.sku(), event.currentStock());
    }
}
```

## Consideraciones

### Transaccionalidad
- **Síncronos**: Ejecutan en la misma transacción (rollback afecta listener)
- **Asíncronos**: Ejecutan después del commit (no hay rollback)

### Orden de Ejecución
- Múltiples listeners para mismo evento no tienen orden garantizado
- Usar `@Order(n)` si el orden importa

### Errores en Listeners
- **Síncronos**: Excepción hace rollback de transacción completa
- **Asíncronos**: Excepción se loguea, no afecta transacción original

## Testing

```java
@Test
void shouldPublishEventOnCreate() {
    // Capturar eventos publicados
    ApplicationEvents events = ApplicationEvents.open();

    productService.save(new Product("Test"));

    assertThat(events.stream(EntityCreatedEvent.class))
        .hasSize(1)
        .first()
        .satisfies(e -> {
            assertThat(e.entityType()).isEqualTo("Product");
        });
}
```
