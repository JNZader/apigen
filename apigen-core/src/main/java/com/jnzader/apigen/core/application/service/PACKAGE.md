# Services

Este paquete contiene los **servicios base** que implementan operaciones CRUD genéricas con caching, eventos y auditoría.

## Archivos

| Archivo | Descripción |
|---------|-------------|
| `BaseService.java` | Interface con operaciones CRUD |
| `BaseServiceImpl.java` | Implementación genérica (~600 líneas) |
| `CacheEvictionService.java` | Servicio para invalidar caches |

## BaseService Interface

### Operaciones de Lectura

```java
public interface BaseService<E extends Base, ID> {

    // Búsqueda
    Result<E, Exception> findById(ID id);
    Result<E, Exception> findOne(Specification<E> spec);

    // Listados
    Result<List<E>, Exception> findAll();
    Result<Page<E>, Exception> findAll(Pageable pageable);
    Result<Page<E>, Exception> findAllActive(Pageable pageable);
    Result<Page<E>, Exception> findAll(Specification<E> spec, Pageable pageable);

    // Verificación
    Result<Boolean, Exception> existsById(ID id);
    Result<Long, Exception> count();
    Result<Long, Exception> countActive();
}
```

### Operaciones de Escritura

```java
public interface BaseService<E extends Base, ID> {

    // Crear/Actualizar
    Result<E, Exception> save(E entity);
    Result<E, Exception> update(ID id, E entity);
    Result<E, Exception> partialUpdate(ID id, E entity);

    // Soft Delete
    Result<Void, Exception> softDelete(ID id);
    Result<Void, Exception> softDelete(ID id, String usuario);

    // Restaurar
    Result<E, Exception> restore(ID id);

    // Hard Delete (permanente)
    Result<Void, Exception> hardDelete(ID id);
}
```

### Operaciones Batch

```java
public interface BaseService<E extends Base, ID> {

    Result<List<E>, Exception> saveAll(List<E> entities);
    Result<Integer, Exception> softDeleteAll(List<ID> ids);
    Result<Integer, Exception> restoreAll(List<ID> ids);
    Result<Integer, Exception> hardDeleteAll(List<ID> ids);
}
```

## BaseServiceImpl

### Características

| Feature | Descripción |
|---------|-------------|
| **Caching** | 3 niveles (entities, lists, counts) |
| **Events** | Publicación automática de domain events |
| **Transactions** | @Transactional con propagación correcta |
| **Batch Processing** | Chunks de 50 con flush/clear |
| **Error Handling** | Retorna Result<T,E>, nunca lanza excepciones |

### Caching

```java
// Cache de entidades
@Cacheable(value = "entities", key = "'EntityName:' + #id")
public Result<E, Exception> findById(ID id) { }

// Cache de listas
@Cacheable(value = "lists", key = "'EntityName:' + #pageable")
public Result<Page<E>, Exception> findAll(Pageable pageable) { }

// Cache de contadores
@Cacheable(value = "counts", key = "'EntityName:count'")
public Result<Long, Exception> count() { }
```

#### Invalidación

```java
// save() invalida lists y counts
@CacheEvict(value = {"lists", "counts"}, allEntries = true)
public Result<E, Exception> save(E entity) { }

// update() invalida entity específica + lists
@Caching(evict = {
    @CacheEvict(value = "entities", key = "'EntityName:' + #id"),
    @CacheEvict(value = {"lists", "counts"}, allEntries = true)
})
public Result<E, Exception> update(ID id, E entity) { }
```

### Domain Events

```java
public Result<E, Exception> save(E entity) {
    boolean isNew = entity.isNew();

    E saved = repository.save(entity);

    // Publicar evento apropiado
    if (isNew) {
        eventPublisher.publishEvent(new EntityCreatedEvent<>(saved));
    } else {
        eventPublisher.publishEvent(new EntityUpdatedEvent<>(saved));
    }

    return Result.success(saved);
}
```

### Batch Processing

```java
private static final int BATCH_SIZE = 50;
private static final int MAX_BATCH_OPERATION_SIZE = 10_000;

public Result<List<E>, Exception> saveAll(List<E> entities) {
    if (entities.size() > MAX_BATCH_OPERATION_SIZE) {
        return Result.failure(new IllegalArgumentException("Máximo 10,000 entidades"));
    }

    List<E> results = new ArrayList<>();

    for (int i = 0; i < entities.size(); i++) {
        E saved = repository.save(entities.get(i));
        results.add(saved);

        // Flush cada BATCH_SIZE para evitar OutOfMemory
        if (i % BATCH_SIZE == 0) {
            entityManager.flush();
            entityManager.clear();
        }
    }

    return Result.success(results);
}
```

### Error Handling con Result

```java
@Override
public Result<E, Exception> findById(ID id) {
    return Result.of(() ->
        repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(getEntityName(), id))
    );
}
```

El método `Result.of()` captura cualquier excepción y la envuelve en `Result.failure()`.

## Crear Servicio Específico

### 1. Interface

```java
public interface ProductService extends BaseService<Product, Long> {

    // Métodos específicos de Product
    Result<Product, Exception> findBySku(String sku);
    Result<List<Product>, Exception> findByCategory(Long categoryId);
    Result<Product, Exception> updateStock(Long id, int delta);
}
```

### 2. Implementación

```java
@Service
@Slf4j
@Transactional
public class ProductServiceImpl
        extends BaseServiceImpl<Product, Long>
        implements ProductService {

    private final ProductRepository productRepository;

    public ProductServiceImpl(ProductRepository repository) {
        super(repository);  // Pasa al constructor padre
        this.productRepository = repository;
    }

    @Override
    protected String getEntityName() {
        return "Product";  // Para mensajes y cache keys
    }

    // Implementar métodos específicos
    @Override
    @Transactional(readOnly = true)
    public Result<Product, Exception> findBySku(String sku) {
        return Result.of(() ->
            productRepository.findBySku(sku)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "sku", sku))
        );
    }

    @Override
    @CacheEvict(value = {"entities", "lists"}, allEntries = true)
    public Result<Product, Exception> updateStock(Long id, int delta) {
        return findById(id)
            .flatMap(product -> {
                int newStock = product.getStock() + delta;
                if (newStock < 0) {
                    return Result.failure(new BusinessException("Stock insuficiente"));
                }
                product.setStock(newStock);
                return save(product);
            });
    }
}
```

## CacheEvictionService

Servicio auxiliar para invalidar caches programáticamente:

```java
@Service
public class CacheEvictionService {

    private final CacheManager cacheManager;

    public void evictAllCaches() {
        cacheManager.getCacheNames()
            .forEach(name -> cacheManager.getCache(name).clear());
    }

    public void evictEntityCache(String entityName, Object id) {
        Cache cache = cacheManager.getCache("entities");
        if (cache != null) {
            cache.evict(entityName + ":" + id);
        }
    }

    public void evictListsCache(String entityName) {
        Cache cache = cacheManager.getCache("lists");
        if (cache != null) {
            cache.clear();  // Todas las listas de esta entidad
        }
    }
}
```

## Transacciones

### Configuración por Defecto

```java
@Service
@Transactional  // Todas las operaciones en transacción
public class ProductServiceImpl extends BaseServiceImpl<Product, Long> {

    @Override
    @Transactional(readOnly = true)  // Optimizado para lectura
    public Result<E, Exception> findById(ID id) { }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)  // Default
    public Result<E, Exception> save(E entity) { }
}
```

### Rollback

```java
// Rollback en cualquier excepción
@Transactional(rollbackFor = Exception.class)
public Result<Order, Exception> createOrder(OrderRequest request) {
    // Si falla cualquier paso, todo se revierte
}

// No rollback en excepciones de negocio específicas
@Transactional(noRollbackFor = NotificationException.class)
public Result<Order, Exception> processOrder(Long orderId) {
    // Fallo en notificación no revierte la orden
}
```

## Testing

```java
@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository repository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ProductServiceImpl service;

    @Test
    void shouldReturnFailureWhenNotFound() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        Result<Product, Exception> result = service.findById(1L);

        assertThat(result.isFailure()).isTrue();
        result.ifFailure(e ->
            assertThat(e).isInstanceOf(ResourceNotFoundException.class)
        );
    }

    @Test
    void shouldPublishEventOnCreate() {
        Product product = new Product();
        when(repository.save(any())).thenReturn(product);

        service.save(product);

        verify(eventPublisher).publishEvent(any(EntityCreatedEvent.class));
    }
}
```

## Buenas Prácticas

1. **Siempre retornar Result** - Nunca lanzar excepciones desde servicios
2. **Usar flatMap para encadenar** - Operaciones dependientes
3. **@Transactional(readOnly=true)** - Para operaciones de lectura
4. **Invalidar cache apropiadamente** - No olvidar en métodos custom
5. **Logging significativo** - Log al inicio y fin de operaciones importantes
