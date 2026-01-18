# Patrones de Diseño en APiGen

**Documentación Didáctica**

Este documento describe los principales patrones de diseño implementados en el proyecto APiGen, explicando el problema que resuelven, su implementación y casos de uso reales.

---

## Tabla de Contenidos

1. [Result Pattern (Railway Oriented Programming)](#1-result-pattern-railway-oriented-programming)
2. [Repository Pattern con Specifications](#2-repository-pattern-con-specifications)
3. [Service Layer Pattern](#3-service-layer-pattern)
4. [Domain Events Pattern](#4-domain-events-pattern)
5. [Soft Delete Pattern](#5-soft-delete-pattern)
6. [Builder Pattern](#6-builder-pattern)
7. [Strategy Pattern (Specifications)](#7-strategy-pattern-specifications)
8. [Circuit Breaker Pattern](#8-circuit-breaker-pattern)

---

## 1. Result Pattern (Railway Oriented Programming)

### Problema que Resuelve

En Java tradicional, el manejo de errores se hace mediante excepciones que:
- Rompen el flujo del código
- Dificultan el encadenamiento de operaciones
- No son evidentes en la firma del método
- Pueden olvidarse de capturar

**El Result Pattern** resuelve esto haciendo el manejo de errores explícito y funcional.

### Diagrama Conceptual

```
Operación Tradicional (con excepciones):
┌──────────┐    try/catch    ┌──────────┐
│ Operación│ ──────────────> │ Success  │
│    A     │    throws       │ o Error  │
└──────────┘                 └──────────┘
                                   ↓
                              Error explota!

Railway Oriented Programming:
┌──────────┐    Success    ┌──────────┐    Success    ┌──────────┐
│ Operación│ ────────────> │ Operación│ ────────────> │ Operación│
│    A     │               │    B     │               │    C     │
└──────────┘               └──────────┘               └──────────┘
     │                          │                          │
  Failure                   Failure                    Failure
     └──────────────────────────┴──────────────────────────┘
                                ↓
                          Error manejado
```

### Implementación en APiGen

```java
/**
 * Interfaz sellada que representa el resultado de una operación.
 * Solo puede ser Success o Failure (sealed interface).
 */
public sealed interface Result<T, E> {

    record Success<T, E>(T value) implements Result<T, E> {}
    record Failure<T, E>(E error) implements Result<T, E> {}

    // Factory Methods
    static <T, E> Result<T, E> success(T value);
    static <T, E> Result<T, E> failure(E error);
    static <T> Result<T, Exception> of(Supplier<T> supplier);

    // Transformaciones
    <U> Result<U, E> map(Function<? super T, ? extends U> mapper);
    <U> Result<U, E> flatMap(Function<? super T, Result<U, E>> mapper);
    <R> R fold(Function<? super T, ? extends R> onSuccess,
               Function<? super E, ? extends R> onFailure);

    // Recuperación
    Result<T, E> recover(Function<? super E, ? extends T> recoveryFunction);
    Result<T, E> recoverWith(Function<? super E, Result<T, E>> recoveryFunction);

    // Extracción
    T orElse(T defaultValue);
    T orElseGet(Function<? super E, ? extends T> supplier);
    T orElseThrow();

    // Side Effects
    Result<T, E> onSuccess(Consumer<? super T> action);
    Result<T, E> onFailure(Consumer<? super E> action);
}
```

### Características Clave

1. **Sealed Interface (Java 17+)**: Solo puede tener dos implementaciones (Success/Failure)
2. **Records**: Inmutabilidad y pattern matching automático
3. **Monadic Operations**: map, flatMap para encadenar operaciones
4. **Railway-oriented**: Los errores se propagan automáticamente

### Ejemplos de Uso Real en el Proyecto

#### Ejemplo 1: Operación Simple con Result

```java
// En BaseServiceImpl.java
@Override
@Transactional(readOnly = true)
public Result<E, Exception> findById(ID id) {
    log.debug("Buscando {} con ID: {}", getEntityName(), id);
    return Result.fromOptional(
        baseRepository.findById(id),
        () -> new ResourceNotFoundException(ERROR_NOT_FOUND + id)
    );
}
```

**Ventajas**:
- El tipo de retorno indica que puede fallar
- No hay try/catch en el código de negocio
- El error es tipado y explícito

#### Ejemplo 2: Encadenamiento de Operaciones (flatMap)

```java
// En BaseServiceImpl.java
@Override
@Transactional
public Result<E, Exception> update(ID id, E entity) {
    log.debug("Actualizando {} con ID: {}", getEntityName(), id);
    return findById(id)                          // Result<E, Exception>
        .flatMap(existingEntity -> {             // Si existe, continuar
            setEntityId(entity, id, existingEntity);
            entity.setVersion(existingEntity.getVersion());
            return save(entity)                   // Retorna otro Result
                .map(saved -> {
                    cacheEvictionService.evictListsByEntityName(getEntityName());
                    return saved;
                });
        });
}
```

**Flujo**:
1. `findById(id)` → `Result<E, Exception>`
2. Si es Success → ejecuta flatMap
3. Si es Failure → se propaga automáticamente sin ejecutar flatMap
4. `save(entity)` → otro `Result<E, Exception>`
5. `map` transforma el valor exitoso

#### Ejemplo 3: Manejo de Errores con fold

```java
// Uso en controlador (hipotético)
Result<User, Exception> result = userService.findById(userId);

return result.fold(
    user -> ResponseEntity.ok(user),                    // Success case
    error -> ResponseEntity.status(404).body(error)     // Failure case
);
```

#### Ejemplo 4: Recuperación de Errores

```java
Result<String, Exception> result = Result.of(() -> fetchFromCache())
    .recoverWith(cacheError -> Result.of(() -> fetchFromDatabase()))
    .recover(dbError -> "default-value");
```

### Operaciones Principales

| Operación | Descripción | Ejemplo |
|-----------|-------------|---------|
| `map` | Transforma el valor si es success | `result.map(user -> user.getName())` |
| `flatMap` | Encadena operaciones que retornan Result | `result.flatMap(user -> findAddress(user.id))` |
| `fold` | Maneja ambos casos (success/failure) | `result.fold(success -> ..., error -> ...)` |
| `recover` | Provee valor por defecto en caso de fallo | `result.recover(error -> defaultUser)` |
| `recoverWith` | Intenta otra operación en caso de fallo | `result.recoverWith(e -> tryAlternative())` |
| `onSuccess` | Side effect solo si es success | `result.onSuccess(user -> log.info(...))` |
| `onFailure` | Side effect solo si es failure | `result.onFailure(e -> log.error(...))` |

### Cuándo Usar

**✅ USAR cuando:**
- Operaciones que pueden fallar predeciblemente
- Necesitas encadenar múltiples operaciones
- Quieres hacer el manejo de errores explícito
- Trabajas con operaciones funcionales

**❌ NO USAR cuando:**
- Errores verdaderamente excepcionales (OutOfMemoryError)
- Necesitas stack traces detallados para debugging
- El equipo no está familiarizado con programación funcional
- Performance es crítico (tiene overhead mínimo)

### Tests del Patrón

```java
// ResultTest.java
@Test
@DisplayName("flatMap() should chain successful results")
void flatMapShouldChainSuccessfulResults() {
    Result<Integer, Exception> result = Result.success(5);

    Result<Integer, Exception> chained = result.flatMap(n -> Result.success(n * 2));

    assertThat(chained.isSuccess()).isTrue();
    assertThat(chained.orElseThrow()).isEqualTo(10);
}

@Test
@DisplayName("flatMap() should short-circuit on failure")
void flatMapShouldShortCircuitOnFailure() {
    Result<Integer, Exception> result = Result.failure(new RuntimeException("error"));
    AtomicBoolean called = new AtomicBoolean(false);

    Result<Integer, Exception> chained = result.flatMap(n -> {
        called.set(true);  // No debería ejecutarse
        return Result.success(n * 2);
    });

    assertThat(chained.isFailure()).isTrue();
    assertThat(called.get()).isFalse();  // ✅ No se llamó
}
```

---

## 2. Repository Pattern con Specifications

### Problema que Resuelve

En aplicaciones tradicionales, cada query necesita un método en el repositorio:
- `findByName(String name)`
- `findByNameAndAge(String name, Integer age)`
- `findByNameOrEmail(String name, String email)`
- ... infinitas combinaciones

**Repository + Specifications** permite:
- Queries dinámicas sin métodos explícitos
- Composición de criterios de búsqueda
- Reutilización de fragmentos de queries
- Type-safety con JPA Criteria API

### Diagrama Conceptual

```
┌─────────────────────────────────────────────────────┐
│                  BaseRepository<E, ID>              │
│  (Generic Interface para todas las entidades)       │
├─────────────────────────────────────────────────────┤
│  + findAll(): List<E>                               │
│  + findById(ID): Optional<E>                        │
│  + save(E): E                                       │
│  + delete(E): void                                  │
│                                                     │
│  Soft Delete Methods:                              │
│  + softDeleteById(ID, fecha, usuario): int         │
│  + restoreById(ID): int                            │
│  + findByEstadoTrue(): List<E>                     │
│                                                     │
│  JPA Specifications Support:                       │
│  + findAll(Specification<E>): List<E>              │
│  + findAll(Spec<E>, Pageable): Page<E>             │
└─────────────────────────────────────────────────────┘
         ▲                            ▲
         │                            │
         │                            │
┌────────┴────────┐          ┌───────┴─────────┐
│ UserRepository  │          │ ProductRepository│
│ (extends Base)  │          │  (extends Base)  │
└─────────────────┘          └──────────────────┘
```

### Implementación en APiGen

```java
/**
 * Repositorio base genérico que todas las entidades extienden.
 * Proporciona CRUD + Soft Delete + Specifications.
 */
@NoRepositoryBean
public interface BaseRepository<E extends Base, ID extends Serializable>
        extends JpaRepository<E, ID>, JpaSpecificationExecutor<E> {

    // ==================== Consultas de Estado ====================

    /** Encuentra todas las entidades activas (estado = true) */
    List<E> findByEstadoTrue();

    /** Encuentra todas las entidades eliminadas (estado = false) */
    List<E> findByEstadoFalse();

    /** Cuenta entidades activas */
    long countByEstadoTrue();

    // ==================== Soft Delete ====================

    /**
     * Soft delete: marca la entidad como inactiva.
     * Usa UPDATE directo en JPQL.
     */
    @Modifying
    @Query("UPDATE #{#entityName} e SET e.estado = false, " +
           "e.fechaEliminacion = :fecha, e.eliminadoPor = :usuario " +
           "WHERE e.id = :id")
    int softDeleteById(@Param("id") ID id,
                       @Param("fecha") LocalDateTime fecha,
                       @Param("usuario") String usuario);

    /**
     * Restaura una entidad eliminada lógicamente.
     * Usa UPDATE directo que no se ve afectado por @SQLRestriction.
     */
    @Modifying
    @Query("UPDATE #{#entityName} e SET e.estado = true, " +
           "e.fechaEliminacion = null, e.eliminadoPor = null " +
           "WHERE e.id = :id")
    int restoreById(@Param("id") ID id);

    /**
     * Hard delete: elimina permanentemente.
     * DELETE directo ignora @SQLRestriction.
     */
    @Modifying
    @Query("DELETE FROM #{#entityName} e WHERE e.id = :id")
    int hardDeleteById(@Param("id") ID id);

    // ==================== Búsquedas por Fechas ====================

    List<E> findByFechaCreacionBetween(LocalDateTime start, LocalDateTime end);
    List<E> findByFechaActualizacionAfter(LocalDateTime date);
}
```

### Características Clave

1. **@NoRepositoryBean**: Spring Data NO crea una implementación para esta interfaz
2. **Generic `<E, ID>`**: Funciona para cualquier entidad
3. **JpaSpecificationExecutor**: Agrega soporte para Specifications
4. **`#{#entityName}`**: SpEL que se reemplaza por el nombre de la entidad
5. **@Modifying**: Indica que la query modifica datos

### Ejemplos de Uso Real

#### Ejemplo 1: Repositorio Específico

```java
// UserRepository.java (ejemplo hipotético)
public interface UserRepository extends BaseRepository<User, Long> {
    // Hereda TODOS los métodos de BaseRepository
    // Puede agregar métodos específicos de User
    Optional<User> findByEmail(String email);
}
```

**Lo que obtienes gratis**:
- `findAll()`, `findById()`, `save()`, `delete()`
- `softDeleteById()`, `restoreById()`, `hardDeleteById()`
- `findByEstadoTrue()` (solo activos)
- `findAll(Specification)` (queries dinámicas)

#### Ejemplo 2: Uso con Specifications

```java
// En el servicio
Specification<User> spec = (root, query, cb) ->
    cb.and(
        cb.equal(root.get("estado"), true),
        cb.like(root.get("name"), "%John%")
    );

List<User> users = userRepository.findAll(spec);
```

#### Ejemplo 3: Soft Delete desde el Servicio

```java
// En BaseServiceImpl.java
@Override
@Transactional
public Result<Void, Exception> softDelete(ID id, String usuario) {
    return findById(id).flatMap(entity -> {
        entity.softDelete(usuario);  // Marca como eliminado
        return Result.of(() -> {
            E saved = baseRepository.save(entity);
            saved.registerEvent(new EntityDeletedEvent<>(saved, usuario));
            cacheEvictionService.evictListsByEntityName(getEntityName());
            return null;
        });
    });
}
```

### Ventajas del Patrón

| Ventaja | Descripción |
|---------|-------------|
| **Reutilización** | Un solo repositorio base para TODAS las entidades |
| **Type Safety** | Compilador verifica tipos, no errores en runtime |
| **DRY** | No repetir código de CRUD en cada repositorio |
| **Queries Dinámicas** | Specifications permiten construir queries en runtime |
| **Soft Delete Automático** | Métodos ya implementados en la base |

### Cuándo Usar

**✅ USAR cuando:**
- Múltiples entidades comparten operaciones similares
- Necesitas soft delete en toda la aplicación
- Quieres queries dinámicas con type-safety
- Sigues DDD (Domain-Driven Design)

**❌ NO USAR cuando:**
- Entidades tienen comportamientos muy diferentes
- Necesitas control total del SQL generado
- La aplicación es muy simple (2-3 entidades)

---

## 3. Service Layer Pattern

### Problema que Resuelve

Sin una capa de servicio:
- Lógica de negocio mezclada con controladores
- Difícil de testear
- No hay transacciones consistentes
- Código duplicado entre endpoints

**Service Layer** centraliza:
- Lógica de negocio
- Manejo de transacciones
- Validaciones
- Orquestación de operaciones

### Diagrama de Arquitectura

```
┌──────────────────────────────────────────────────────┐
│                   Controller Layer                   │
│  (REST Endpoints, validación básica, DTOs)           │
└──────────────────┬───────────────────────────────────┘
                   │
                   ▼
┌──────────────────────────────────────────────────────┐
│                   Service Layer                      │
│  ┌────────────────────────────────────────────────┐  │
│  │         BaseServiceImpl<E, ID>                 │  │
│  │                                                │  │
│  │  - Lógica de negocio                          │  │
│  │  - Transacciones (@Transactional)             │  │
│  │  - Caché (@Cacheable, @CacheEvict)            │  │
│  │  - Eventos de dominio                         │  │
│  │  - Result pattern (manejo de errores)         │  │
│  └────────────────────────────────────────────────┘  │
└──────────────────┬───────────────────────────────────┘
                   │
                   ▼
┌──────────────────────────────────────────────────────┐
│              Repository Layer                        │
│  (Acceso a datos, JPA, Specifications)               │
└──────────────────────────────────────────────────────┘
```

### Implementación en APiGen

```java
/**
 * Servicio base que implementa operaciones CRUD genéricas.
 * Template Method Pattern: define el flujo, subclases personalizan.
 */
public abstract class BaseServiceImpl<E extends Base, ID extends Serializable>
        implements BaseService<E, ID> {

    protected final BaseRepository<E, ID> baseRepository;
    protected final CacheEvictionService cacheEvictionService;

    @PersistenceContext
    protected EntityManager entityManager;

    @Autowired
    protected ApplicationEventPublisher eventPublisher;

    // ==================== Template Method ====================

    /**
     * Las subclases DEBEN implementar esto.
     * Usado para logging, caché y operaciones específicas.
     */
    protected abstract Class<E> getEntityClass();

    /**
     * Nombre de la entidad para logs y caché.
     * Puede ser sobrescrito por subclases.
     */
    public String getEntityName() {
        return "Entity";  // Default
    }

    // ==================== Operaciones con Caché ====================

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "entities",
               key = "#root.target.entityName + ':' + #id",
               unless = "#result.isFailure()")
    public Result<E, Exception> findById(ID id) {
        log.debug("Buscando {} con ID: {}", getEntityName(), id);
        return Result.fromOptional(
            baseRepository.findById(id),
            () -> new ResourceNotFoundException(ERROR_NOT_FOUND + id)
        );
    }

    @Override
    @Transactional
    public Result<E, Exception> save(E entity) {
        return Result.of(() -> {
            boolean isNew = entity.getId() == null;
            E saved = baseRepository.save(entity);

            // Publicar evento de dominio
            if (isNew) {
                saved.registerEvent(new EntityCreatedEvent<>(saved));
            } else {
                saved.registerEvent(new EntityUpdatedEvent<>(saved));
            }

            return saved;
        });
    }

    // ==================== Operaciones Batch ====================

    @Override
    @Transactional(timeout = 300)  // 5 minutos
    public Result<List<E>, Exception> saveAll(List<E> entities) {
        return Result.of(() -> {
            validateBatchSize(entities.size(), "saveAll");

            List<E> allSaved = new ArrayList<>();

            // Procesar en lotes para evitar OOM
            for (int i = 0; i < entities.size(); i += BATCH_SIZE) {
                int end = Math.min(i + BATCH_SIZE, entities.size());
                List<E> batch = entities.subList(i, end);

                List<E> savedBatch = baseRepository.saveAll(batch);
                allSaved.addAll(savedBatch);

                // Flush y clear para liberar memoria
                entityManager.flush();
                entityManager.clear();
            }

            return allSaved;
        });
    }
}
```

### Características Clave del Servicio

1. **Transacciones Declarativas**: `@Transactional`
2. **Caché Integrado**: `@Cacheable`, `@CacheEvict`
3. **Result Pattern**: Manejo de errores funcional
4. **Domain Events**: Publicación automática de eventos
5. **Batch Processing**: Optimizado para grandes volúmenes
6. **Template Method**: Subclases personalizan comportamiento

### Ejemplo de Uso Real: TestEntityService

```java
// Servicio específico que extiende BaseServiceImpl
@Service
@Transactional
@Slf4j
public class TestEntityService extends BaseServiceImpl<TestEntity, Long> {

    private final TestEntityRepository repository;

    public TestEntityService(
            TestEntityRepository repository,
            CacheEvictionService cacheEvictionService) {
        super(repository, cacheEvictionService);
        this.repository = repository;
    }

    @Override
    protected Class<TestEntity> getEntityClass() {
        return TestEntity.class;
    }

    @Override
    public String getEntityName() {
        return "TestEntity";
    }

    // Métodos adicionales específicos de TestEntity
    public Result<List<TestEntity>, Exception> findByNameContaining(String name) {
        return Result.of(() -> repository.findByNameContaining(name));
    }
}
```

### Caché Strategy

El servicio implementa una estrategia de caché en dos niveles:

```java
// Nivel 1: Cachear entidad individual
@Cacheable(value = "entities",
           key = "#root.target.entityName + ':' + #id")
public Result<E, Exception> findById(ID id) { ... }

// Nivel 2: Cachear listas
@Cacheable(value = "lists",
           key = "#root.target.entityName + ':all:' + #pageable.pageNumber")
public Result<Page<E>, Exception> findAll(Pageable pageable) { ... }

// Eviction: Invalidar caché al actualizar
@CacheEvict(value = "entities",
            key = "#root.target.entityName + ':' + #id")
public Result<E, Exception> update(ID id, E entity) {
    return findById(id).flatMap(existing -> {
        // ... update logic
        cacheEvictionService.evictListsByEntityName(getEntityName());
        return save(entity);
    });
}
```

### Cuándo Usar

**✅ USAR cuando:**
- Aplicación con múltiples entidades similares
- Necesitas transacciones consistentes
- Quieres caché automático
- Sigues arquitectura en capas (Clean Architecture)

**❌ NO USAR cuando:**
- Aplicación muy simple (CRUD básico)
- Necesitas control total sobre transacciones
- Cada entidad tiene lógica MUY diferente

---

## 4. Domain Events Pattern

### Problema que Resuelve

Sin eventos de dominio:
- Lógica de negocio acoplada (notificaciones, auditoría, integraciones)
- Difícil agregar comportamientos nuevos
- Violación del Single Responsibility Principle
- Testing complicado

**Domain Events** permite:
- Desacoplar acciones secundarias (notificaciones, logs, auditoría)
- Comunicación asíncrona entre módulos
- Extensibilidad sin modificar código existente
- Testing más simple (mock de eventos)

### Diagrama de Flujo

```
┌─────────────────────────────────────────────────────────┐
│              Operación de Negocio                       │
│  (Servicio crea/actualiza/elimina entidad)              │
└──────────────────┬──────────────────────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────────────────────┐
│         Entidad registra DomainEvent                    │
│  entity.registerEvent(new EntityCreatedEvent<>(entity)) │
└──────────────────┬──────────────────────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────────────────────┐
│    Spring Data publica eventos (@DomainEvents)          │
│    Espera a que la transacción se complete              │
└──────────────────┬──────────────────────────────────────┘
                   │
                   ▼ (AFTER_COMMIT)
┌─────────────────────────────────────────────────────────┐
│         DomainEventHandler maneja eventos               │
│    - Logging                                            │
│    - Métricas (Micrometer)                              │
│    - Notificaciones (hooks)                             │
│    - Integraciones externas                             │
└─────────────────────────────────────────────────────────┘
```

### Implementación en APiGen

#### 1. Interfaz Base de Eventos

```java
/**
 * Interfaz base para todos los eventos de dominio.
 * Representa hechos que han ocurrido en el dominio.
 */
public interface DomainEvent {

    /** Momento en que ocurrió el evento */
    LocalDateTime occurredOn();

    /** Tipo de evento (nombre de la clase por defecto) */
    default String eventType() {
        return this.getClass().getSimpleName();
    }
}
```

#### 2. Eventos Concretos (Records)

```java
/**
 * Evento disparado cuando una entidad es creada.
 * Record inmutable con todos los datos necesarios.
 */
public record EntityCreatedEvent<E extends Base>(
        E entity,
        String createdBy,
        LocalDateTime occurredOn
) implements DomainEvent {

    // Constructor de conveniencia
    public EntityCreatedEvent(E entity, String createdBy) {
        this(entity, createdBy, LocalDateTime.now());
    }

    public EntityCreatedEvent(E entity) {
        this(entity, null, LocalDateTime.now());
    }
}

// Otros eventos similares:
// - EntityUpdatedEvent<E>
// - EntityDeletedEvent<E>
// - EntityRestoredEvent<E>
```

#### 3. Registro de Eventos en la Entidad

```java
@MappedSuperclass
public abstract class Base implements Serializable {

    // Lista thread-safe de eventos pendientes
    @Transient
    @JsonIgnore
    private final List<DomainEvent> domainEvents = new CopyOnWriteArrayList<>();

    /**
     * Registra un evento para ser publicado.
     */
    public void registerEvent(DomainEvent event) {
        this.domainEvents.add(event);
    }

    /**
     * Spring Data llama esto para obtener eventos.
     */
    @DomainEvents
    public List<DomainEvent> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    /**
     * Limpia eventos después de publicación.
     */
    @AfterDomainEventPublication
    public void clearDomainEvents() {
        this.domainEvents.clear();
    }
}
```

#### 4. Handler de Eventos

```java
@Component
public class DomainEventHandler {

    private final Counter entityCreatedCounter;
    private final Counter entityUpdatedCounter;

    public DomainEventHandler(MeterRegistry meterRegistry) {
        this.entityCreatedCounter = Counter.builder("domain.events.created")
                .description("Number of entity created events")
                .register(meterRegistry);
        // ... otros counters
    }

    /**
     * Maneja eventos de creación.
     * - @Async: No bloquea la transacción
     * - AFTER_COMMIT: Solo si la transacción fue exitosa
     */
    @Async("domainEventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public <E extends Base> void handleEntityCreated(EntityCreatedEvent<E> event) {
        entityCreatedCounter.increment();

        E entity = event.entity();
        log.info("Entity created: type={}, id={}, createdBy={}",
                 entity.getClass().getSimpleName(),
                 entity.getId(),
                 event.createdBy());

        // Hook para subclases
        onEntityCreated(event);
    }

    // Hooks extensibles
    protected <E extends Base> void onEntityCreated(EntityCreatedEvent<E> event) {
        // Subclases pueden sobrescribir para:
        // - Enviar notificaciones
        // - Integraciones con sistemas externos
        // - Auditoría adicional
    }
}
```

### Ejemplos de Uso Real

#### Ejemplo 1: Registro de Evento al Crear

```java
// En BaseServiceImpl.java
@Override
@Transactional
public Result<E, Exception> save(E entity) {
    return Result.of(() -> {
        boolean isNew = entity.getId() == null;
        E saved = baseRepository.save(entity);

        // Registrar evento DESPUÉS de guardar (para tener ID)
        if (isNew) {
            saved.registerEvent(new EntityCreatedEvent<>(saved, saved.getCreadoPor()));
            log.info("Entidad {} creada con ID: {}", getEntityName(), saved.getId());
        } else {
            saved.registerEvent(new EntityUpdatedEvent<>(saved, saved.getModificadoPor()));
        }

        return saved;
    });
}
```

#### Ejemplo 2: Evento de Eliminación

```java
@Override
@Transactional
public Result<Void, Exception> softDelete(ID id, String usuario) {
    return findById(id).flatMap(entity -> {
        entity.softDelete(usuario);
        return Result.of(() -> {
            E saved = baseRepository.save(entity);

            // Evento con información de quién eliminó
            saved.registerEvent(new EntityDeletedEvent<>(saved, usuario, true));

            cacheEvictionService.evictListsByEntityName(getEntityName());
            return null;
        });
    });
}
```

#### Ejemplo 3: Extensión del Handler

```java
@Component
public class NotificationEventHandler extends DomainEventHandler {

    private final EmailService emailService;

    @Override
    protected <E extends Base> void onEntityCreated(EntityCreatedEvent<E> event) {
        // Enviar email de notificación
        if (event.entity() instanceof User user) {
            emailService.sendWelcomeEmail(user.getEmail());
        }
    }

    @Override
    protected <E extends Base> void onEntityDeleted(EntityDeletedEvent<E> event) {
        // Auditoría especial para eliminaciones
        auditService.logDeletion(
            event.entity().getClass().getSimpleName(),
            event.entity().getId(),
            event.deletedBy()
        );
    }
}
```

### Características del Patrón

| Característica | Descripción |
|----------------|-------------|
| **Asíncrono** | `@Async` - No bloquea la operación principal |
| **Transaccional** | `AFTER_COMMIT` - Solo si la transacción fue exitosa |
| **Desacoplado** | Handler no conoce al servicio que generó el evento |
| **Extensible** | Fácil agregar nuevos handlers sin modificar código |
| **Type-safe** | Eventos tipados con genéricos |
| **Métricas** | Contadores automáticos con Micrometer |

### Cuándo Usar

**✅ USAR cuando:**
- Necesitas desacoplar acciones secundarias
- Quieres agregar funcionalidad sin modificar servicios
- Necesitas auditoría, notificaciones, integraciones
- Sigues Event-Driven Architecture

**❌ NO USAR cuando:**
- La acción secundaria es crítica para la operación principal
- Necesitas transacción única (todo o nada)
- La aplicación es muy simple
- No necesitas extensibilidad

---

## 5. Soft Delete Pattern

### Problema que Resuelve

Delete tradicional (hard delete):
- Pérdida permanente de datos
- No se puede recuperar información eliminada
- Problemas con auditoría y compliance
- Integridad referencial complicada

**Soft Delete**:
- Marca registros como "eliminados" sin borrarlos
- Permite recuperación
- Mantiene historial completo
- Cumple regulaciones (GDPR permite retención)

### Diagrama Conceptual

```
Hard Delete (Tradicional):
┌─────────────┐                    ┌─────────────┐
│   User      │  DELETE            │             │
│   id: 1     │ ──────────────────>│  DESTRUIDO  │
│   name: John│                    │             │
└─────────────┘                    └─────────────┘
                                    ❌ Irrecuperable

Soft Delete (APiGen):
┌─────────────────────────────────┐         ┌──────────────────────────────────┐
│   User                          │ UPDATE  │   User                           │
│   id: 1                         │────────>│   id: 1                          │
│   name: John                    │         │   name: John                     │
│   estado: true                  │         │   estado: false ❌               │
│   fechaEliminacion: null        │         │   fechaEliminacion: 2024-01-15   │
│   eliminadoPor: null            │         │   eliminadoPor: "admin"          │
└─────────────────────────────────┘         └──────────────────────────────────┘
         ✅ Visible                                  🚫 Oculto (pero existe)

Consultas normales NO ven registros con estado=false
Consultas especiales SÍ pueden verlos (restore, auditoría)
```

### Implementación en APiGen

#### 1. Campos en la Entidad Base

```java
@MappedSuperclass
@SQLRestriction("estado = true")  // ⬅️ CLAVE: Filtro automático
public abstract class Base implements Serializable {

    /**
     * Estado de la entidad (true = activo, false = eliminado).
     */
    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    private Boolean estado = true;

    /**
     * Fecha y hora de eliminación lógica.
     */
    @Column
    private LocalDateTime fechaEliminacion;

    /**
     * Usuario que realizó la eliminación.
     */
    @Column(length = 100)
    private String eliminadoPor;

    // ==================== Métodos de Soft Delete ====================

    /**
     * Marca la entidad como eliminada.
     */
    public void softDelete(String usuario) {
        this.estado = false;
        this.fechaEliminacion = LocalDateTime.now();
        this.eliminadoPor = usuario;
    }

    /**
     * Restaura una entidad eliminada.
     */
    public void restore() {
        this.estado = true;
        this.fechaEliminacion = null;
        this.eliminadoPor = null;
    }

    /**
     * Verifica si está eliminada.
     */
    public boolean isDeleted() {
        return !Boolean.TRUE.equals(estado);
    }

    public boolean isActive() {
        return Boolean.TRUE.equals(estado);
    }
}
```

#### 2. Anotación @SQLRestriction (Hibernate 6.2+)

```java
@SQLRestriction("estado = true")
```

**Qué hace**:
- Agrega automáticamente `WHERE estado = true` a TODAS las consultas
- Afecta a: `SELECT`, `JOIN`, `COUNT`, subconsultas
- NO afecta a: `UPDATE` directo, `DELETE` directo

**SQL Generado**:
```sql
-- findAll() genera:
SELECT * FROM users WHERE estado = true;

-- findById(1) genera:
SELECT * FROM users WHERE id = 1 AND estado = true;

-- JOIN genera:
SELECT u.*, r.* FROM users u
JOIN roles r ON u.role_id = r.id
WHERE u.estado = true AND r.estado = true;
```

#### 3. Métodos en BaseRepository

```java
public interface BaseRepository<E extends Base, ID extends Serializable> {

    /**
     * Soft delete: UPDATE directo que NO se ve afectado por @SQLRestriction
     */
    @Modifying
    @Query("UPDATE #{#entityName} e SET e.estado = false, " +
           "e.fechaEliminacion = :fecha, e.eliminadoPor = :usuario " +
           "WHERE e.id = :id")
    int softDeleteById(@Param("id") ID id,
                       @Param("fecha") LocalDateTime fecha,
                       @Param("usuario") String usuario);

    /**
     * Restaurar: UPDATE directo para bypass de @SQLRestriction
     */
    @Modifying
    @Query("UPDATE #{#entityName} e SET e.estado = true, " +
           "e.fechaEliminacion = null, e.eliminadoPor = null " +
           "WHERE e.id = :id")
    int restoreById(@Param("id") ID id);

    /**
     * Hard delete: Eliminación permanente (solo para admins)
     */
    @Modifying
    @Query("DELETE FROM #{#entityName} e WHERE e.id = :id")
    int hardDeleteById(@Param("id") ID id);

    /**
     * Verificar existencia incluyendo eliminados
     */
    @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END " +
           "FROM #{#entityName} e WHERE e.id = :id")
    boolean existsByIdIncludingDeleted(@Param("id") ID id);
}
```

#### 4. Restauración con Native SQL

```java
// En BaseServiceImpl.java
@Override
@Transactional
public Result<E, Exception> restore(ID id) {
    return Result.of(() -> {
        // ⚠️ JPQL UPDATE también es afectado por @SQLRestriction en Hibernate 6.2+
        // Solución: usar native SQL
        String tableName = getTableName();
        String sql = "UPDATE " + tableName +
                     " SET estado = true, fecha_eliminacion = NULL, eliminado_por = NULL " +
                     "WHERE id = :id";

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("id", id);
        int updated = query.executeUpdate();

        if (updated == 0) {
            throw new ResourceNotFoundException("Entidad no encontrada: " + id);
        }

        // Limpiar caché y refrescar EntityManager
        entityManager.flush();
        entityManager.clear();

        // Ahora findById funcionará (estado = true)
        E restored = baseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Error restaurando: " + id));

        restored.registerEvent(new EntityRestoredEvent<>(restored));
        return restored;
    });
}
```

### Flujo Completo de Soft Delete

```
1. Usuario solicita DELETE /api/users/1
         ↓
2. Controller llama service.softDelete(1, "admin")
         ↓
3. Service busca la entidad con findById(1)
         ↓ (SELECT * FROM users WHERE id = 1 AND estado = true)
         ↓
4. entity.softDelete("admin")
         ↓ (estado = false, fechaEliminacion = now())
         ↓
5. repository.save(entity)
         ↓ (UPDATE users SET estado = false, ... WHERE id = 1)
         ↓
6. Evento EntityDeletedEvent publicado
         ↓
7. findById(1) → NOT FOUND (filtrado por @SQLRestriction)
   findAll() → NO incluye el registro
         ↓
8. restore(1) → Usa native SQL para bypass
         ↓ (UPDATE users SET estado = true WHERE id = 1)
         ↓
9. findById(1) → FOUND (ahora estado = true)
```

### Casos Especiales

#### Ver Registros Eliminados

```java
// Opción 1: Query nativa
@Query(value = "SELECT * FROM users WHERE id = :id", nativeQuery = true)
Optional<User> findByIdIncludingDeleted(@Param("id") Long id);

// Opción 2: Specification custom
Specification<User> spec = (root, query, cb) -> cb.isNotNull(root.get("id"));
// Esto NO incluye el filtro de @SQLRestriction automáticamente
```

#### Cascade Delete (Relaciones)

```java
@Entity
public class User extends Base {

    // Si user se soft-delete, ¿qué pasa con sus posts?
    @OneToMany(mappedBy = "user")
    private List<Post> posts;
}

// Solución: Manejar manualmente
@Override
@Transactional
public Result<Void, Exception> softDelete(Long userId, String username) {
    return findById(userId).flatMap(user -> {
        // Soft delete en cascada
        user.getPosts().forEach(post -> post.softDelete(username));
        user.softDelete(username);
        return Result.of(() -> {
            repository.save(user);
            return null;
        });
    });
}
```

### Ventajas y Desventajas

| Ventajas | Desventajas |
|----------|-------------|
| ✅ Recuperación de datos | ❌ Crecimiento de base de datos |
| ✅ Auditoría completa | ❌ Índices deben incluir `estado` |
| ✅ Cumple regulaciones | ❌ Queries más complejas |
| ✅ Histórico preservado | ❌ Necesita limpieza periódica |
| ✅ Rollback fácil | ❌ Unicidad complicada (email + estado) |

### Cuándo Usar

**✅ USAR cuando:**
- Datos críticos de negocio
- Necesitas auditoría y compliance
- Los usuarios pueden arrepentirse
- Regulaciones requieren retención

**❌ NO USAR cuando:**
- Datos verdaderamente temporales (sessions, tokens)
- Volumen masivo de datos
- Performance crítica
- Datos sensibles que deben destruirse (GDPR right to erasure)

### Hard Delete (Eliminación Permanente)

```java
@Override
@Transactional
public Result<Void, Exception> hardDelete(ID id) {
    log.warn("⚠️ HARD DELETE {} ID: {} - IRREVERSIBLE!", getEntityName(), id);

    return Result.of(() -> {
        int deleted = baseRepository.hardDeleteById(id);
        if (deleted == 0) {
            throw new ResourceNotFoundException("Not found: " + id);
        }

        eventPublisher.publishEvent(new EntityHardDeletedEvent<>(id, getEntityName()));
        cacheEvictionService.evictAll(getEntityName(), null);
        return null;
    });
}
```

**Cuándo hacer Hard Delete**:
- Datos de prueba
- Cumplir "derecho al olvido" (GDPR)
- Limpiar datos muy antiguos (archiving)
- Solo con permisos de administrador

---

## 6. Builder Pattern

### Problema que Resuelve

Creación de objetos tradicional:
```java
// ❌ Difícil de leer, muchos parámetros
User user = new User(1L, "John", "john@example.com", "password123",
                     LocalDateTime.now(), true, "admin", null, null);

// ❌ Setters exponen mutabilidad
User user = new User();
user.setId(1L);
user.setName("John");
user.setEmail("john@example.com");
// ... 10 setters más
```

**Builder Pattern** permite:
- Creación fluida y legible
- Inmutabilidad (opcional)
- Valores por defecto
- Validación en construcción

### Diagrama Conceptual

```
Builder Pattern (Fluent API):

TestEntityBuilder.aTestEntity()
        ↓
    .withName("John")          ← Método fluido (retorna this)
        ↓
    .withEmail("john@test.com") ← Método fluido (retorna this)
        ↓
    .withEstado(true)           ← Método fluido (retorna this)
        ↓
    .build()                    ← Construye el objeto final
        ↓
    TestEntity (inmutable)
```

### Implementación con Lombok @Builder

```java
@Entity
@Getter
@Setter
@Builder  // ⬅️ Genera el builder automáticamente
@NoArgsConstructor
@AllArgsConstructor
public class User extends Base {

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String password;

    @ManyToOne
    @JoinColumn(name = "role_id")
    private Role role;
}

// Uso:
User user = User.builder()
    .email("john@example.com")
    .name("John Doe")
    .password(encodedPassword)
    .role(adminRole)
    .build();
```

**Lombok genera automáticamente**:
```java
public class User {
    public static UserBuilder builder() {
        return new UserBuilder();
    }

    public static class UserBuilder {
        private String email;
        private String name;
        private String password;
        private Role role;

        public UserBuilder email(String email) {
            this.email = email;
            return this;
        }

        public UserBuilder name(String name) {
            this.name = name;
            return this;
        }

        // ... otros métodos

        public User build() {
            return new User(email, name, password, role);
        }
    }
}
```

### Builder Manual para Tests

```java
/**
 * Builder manual para tests con valores por defecto.
 * Permite crear entidades rápidamente sin especificar todos los campos.
 */
public class TestEntityBuilder {

    private static final AtomicLong ID_COUNTER = new AtomicLong(1);

    // Valores por defecto
    private Long id;
    private String name = "Default Name";
    private Boolean estado = true;
    private LocalDateTime fechaCreacion = LocalDateTime.now();
    private String creadoPor = "test-user";
    private Long version = 0L;

    private TestEntityBuilder() {}

    // ==================== Factory Methods ====================

    public static TestEntityBuilder aTestEntity() {
        return new TestEntityBuilder();
    }

    public static TestEntityBuilder aTestEntityWithId() {
        return new TestEntityBuilder()
                .withId(ID_COUNTER.getAndIncrement());
    }

    // ==================== Fluent Methods ====================

    public TestEntityBuilder withId(Long id) {
        this.id = id;
        return this;
    }

    public TestEntityBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public TestEntityBuilder withEstado(Boolean estado) {
        this.estado = estado;
        return this;
    }

    public TestEntityBuilder withCreadoPor(String creadoPor) {
        this.creadoPor = creadoPor;
        return this;
    }

    // ==================== Convenience Methods ====================

    public TestEntityBuilder deleted() {
        this.estado = false;
        return this;
    }

    public TestEntityBuilder active() {
        this.estado = true;
        return this;
    }

    // ==================== Build ====================

    public TestEntity build() {
        TestEntity entity = new TestEntity();
        entity.setId(id);
        entity.setName(name);
        entity.setEstado(estado);
        entity.setFechaCreacion(fechaCreacion);
        entity.setCreadoPor(creadoPor);
        entity.setVersion(version);
        return entity;
    }

    /**
     * Resetea el contador de IDs (útil entre tests).
     */
    public static void resetIdCounter() {
        ID_COUNTER.set(1);
    }
}
```

### Ejemplos de Uso en Tests

```java
@Test
void testCreateEntity() {
    // ✅ Crear entidad con valores por defecto
    TestEntity entity = TestEntityBuilder.aTestEntity().build();

    assertThat(entity.getName()).isEqualTo("Default Name");
    assertThat(entity.getEstado()).isTrue();
}

@Test
void testCreateCustomEntity() {
    // ✅ Sobrescribir solo los campos necesarios
    TestEntity entity = TestEntityBuilder.aTestEntity()
        .withName("Custom Name")
        .withCreadoPor("admin")
        .build();

    assertThat(entity.getName()).isEqualTo("Custom Name");
    assertThat(entity.getCreadoPor()).isEqualTo("admin");
    assertThat(entity.getEstado()).isTrue();  // Valor por defecto
}

@Test
void testCreateDeletedEntity() {
    // ✅ Usar método de conveniencia
    TestEntity entity = TestEntityBuilder.aTestEntity()
        .deleted()  // ⬅️ Shortcut para .withEstado(false)
        .build();

    assertThat(entity.isDeleted()).isTrue();
}

@Test
void testCreateMultipleEntitiesWithAutoId() {
    // ✅ IDs autoincrementales para tests
    TestEntity entity1 = TestEntityBuilder.aTestEntityWithId().build();
    TestEntity entity2 = TestEntityBuilder.aTestEntityWithId().build();
    TestEntity entity3 = TestEntityBuilder.aTestEntityWithId().build();

    assertThat(entity1.getId()).isEqualTo(1L);
    assertThat(entity2.getId()).isEqualTo(2L);
    assertThat(entity3.getId()).isEqualTo(3L);
}

@BeforeEach
void setUp() {
    // Resetear contador entre tests
    TestEntityBuilder.resetIdCounter();
}
```

### Variantes del Builder Pattern

#### 1. Lombok @Builder con Valores por Defecto

```java
@Builder
public class User {

    @Builder.Default
    private Boolean active = true;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private String name;
    private String email;
}

// Uso:
User user = User.builder()
    .name("John")
    .email("john@test.com")
    .build();  // active y createdAt tienen valores por defecto
```

#### 2. Lombok @Builder con Validación

```java
@Getter
@Builder
public class User {
    private String name;
    private String email;

    public static class UserBuilder {
        public User build() {
            // Validación antes de construir
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("Name is required");
            }
            if (email == null || !email.contains("@")) {
                throw new IllegalArgumentException("Valid email is required");
            }
            return new User(name, email);
        }
    }
}
```

#### 3. Builder para DTOs

```java
@Builder
public record CreateUserDTO(
    @NotBlank String name,
    @Email String email,
    @StrongPassword String password
) {
    // Record + Builder = Inmutabilidad + Fluent API
}

// Test:
CreateUserDTO dto = CreateUserDTO.builder()
    .name("John")
    .email("john@test.com")
    .password("SecurePass123!")
    .build();
```

### Cuándo Usar

**✅ USAR Builder cuando:**
- Objeto tiene 4+ parámetros
- Algunos parámetros son opcionales
- Necesitas valores por defecto
- Creas muchas instancias en tests
- Quieres API fluida y legible

**✅ Lombok @Builder cuando:**
- Entidades/DTOs de producción
- No necesitas lógica compleja de construcción
- Quieres código mínimo

**✅ Builder Manual cuando:**
- Tests (valores por defecto personalizados)
- Necesitas lógica compleja (validación, IDs auto)
- Múltiples variantes del mismo objeto

**❌ NO USAR cuando:**
- Objeto simple (1-3 parámetros)
- Constructor ya es claro
- No necesitas inmutabilidad

### Comparación

```java
// Sin Builder (verbose)
User user = new User();
user.setName("John");
user.setEmail("john@test.com");
user.setActive(true);
user.setCreatedAt(LocalDateTime.now());
user.setRole(adminRole);

// Con Builder (fluido y legible)
User user = User.builder()
    .name("John")
    .email("john@test.com")
    .active(true)
    .createdAt(LocalDateTime.now())
    .role(adminRole)
    .build();

// Builder en Test (valores por defecto)
User user = UserBuilder.aUser()
    .withName("John")
    .build();  // Email, active, createdAt tienen defaults
```

---

## 7. Strategy Pattern (Specifications)

### Problema que Resuelve

Queries tradicionales en el repositorio:
```java
// ❌ Un método por cada combinación
List<User> findByName(String name);
List<User> findByNameAndEmail(String name, String email);
List<User> findByNameOrEmail(String name, String email);
List<User> findByAgeGreaterThan(Integer age);
List<User> findByNameAndAgeGreaterThan(String name, Integer age);
// ... infinitas combinaciones
```

**Strategy Pattern con Specifications** permite:
- Construir queries dinámicamente en runtime
- Combinar criterios como bloques LEGO
- Reutilizar fragmentos de queries
- Type-safety (no strings mágicos)

### Diagrama Conceptual

```
┌──────────────────────────────────────────────────────┐
│           FilterSpecificationBuilder                 │
│  Convierte parámetros a Specifications               │
└────────────────┬─────────────────────────────────────┘
                 │
                 ▼
Input: "name:like:John,age:gte:25,estado:eq:true"
                 │
                 ▼
      ┌──────────┴──────────┐
      │    ParseFilters     │
      │  name:like:John     │
      │  age:gte:25         │
      │  estado:eq:true     │
      └──────────┬──────────┘
                 │
                 ▼
      ┌────────────────────────────────┐
      │  Build Specifications          │
      │  ┌─────────────────────────┐   │
      │  │ name LIKE '%John%'      │   │
      │  └─────────────────────────┘   │
      │            AND                 │
      │  ┌─────────────────────────┐   │
      │  │ age >= 25               │   │
      │  └─────────────────────────┘   │
      │            AND                 │
      │  ┌─────────────────────────┐   │
      │  │ estado = true           │   │
      │  └─────────────────────────┘   │
      └────────────┬───────────────────┘
                   │
                   ▼
      JPA Criteria Query (type-safe)
                   │
                   ▼
      SELECT * FROM users
      WHERE name LIKE '%John%'
        AND age >= 25
        AND estado = true
```

### Implementación: FilterSpecificationBuilder

```java
@Component
public class FilterSpecificationBuilder {

    // ==================== Formato de Filtros ====================

    /**
     * Formato: campo:operador:valor,campo2:operador2:valor2
     *
     * Ejemplos:
     * - name:like:John
     * - age:gte:25
     * - email:eq:john@example.com
     * - createdAt:between:2024-01-01;2024-12-31
     * - status:in:ACTIVE;PENDING;APPROVED
     */

    // ==================== Operadores Soportados ====================

    public enum FilterOperator {
        EQ("eq"),           // Igual (=)
        NEQ("neq"),         // No igual (!=)
        LIKE("like"),       // Contiene (LIKE %value%)
        STARTS("starts"),   // Empieza con (LIKE value%)
        ENDS("ends"),       // Termina con (LIKE %value)
        GT("gt"),           // Mayor que (>)
        GTE("gte"),         // Mayor o igual (>=)
        LT("lt"),           // Menor que (<)
        LTE("lte"),         // Menor o igual (<=)
        IN("in"),           // En lista (IN (v1,v2,v3))
        NOT_IN("notin"),    // No en lista (NOT IN)
        BETWEEN("between"), // Entre dos valores (BETWEEN)
        NULL("null"),       // Es nulo (IS NULL)
        NOT_NULL("notnull");// No es nulo (IS NOT NULL)

        // ... implementación
    }

    // ==================== Build Method ====================

    /**
     * Construye una Specification a partir de un string de filtros.
     */
    public <E extends Base> Specification<E> build(
            String filterString,
            Class<E> entityClass) {

        if (filterString == null || filterString.isBlank()) {
            return (root, query, cb) -> cb.conjunction(); // WHERE true
        }

        List<FilterCriteria> criteria = parseFilterString(filterString);
        return buildSpecification(criteria);
    }

    // ==================== Parsing ====================

    private List<FilterCriteria> parseFilterString(String filterString) {
        List<FilterCriteria> criteria = new ArrayList<>();

        String[] filters = filterString.split(",");
        for (String filter : filters) {
            String[] parts = filter.split(":", 3);

            if (parts.length < 2) continue;

            String field = parts[0].trim();
            FilterOperator operator = FilterOperator.fromString(parts[1]);
            String value = parts.length > 2 ? parts[2].trim() : null;

            criteria.add(new FilterCriteria(field, operator, value));
        }

        return criteria;
    }

    // ==================== Specification Building ====================

    private <E extends Base> Specification<E> buildSpecification(
            List<FilterCriteria> criteria) {

        Specification<E> spec = (root, query, cb) -> cb.conjunction();

        for (FilterCriteria c : criteria) {
            spec = spec.and(buildCriteriaSpecification(c));
        }

        return spec;
    }

    private <E extends Base> Specification<E> buildCriteriaSpecification(
            FilterCriteria c) {

        return (root, query, cb) -> {
            Path<?> path = getPath(root, c.field());
            Object typedValue = convertValue(c.value(), path.getJavaType());

            return switch (c.operator()) {
                case EQ -> cb.equal(path, typedValue);
                case NEQ -> cb.notEqual(path, typedValue);
                case LIKE -> cb.like(
                    cb.lower((Path<String>) path),
                    "%" + c.value().toLowerCase() + "%"
                );
                case GT -> cb.greaterThan(
                    (Path<Comparable>) path,
                    (Comparable) typedValue
                );
                case GTE -> cb.greaterThanOrEqualTo(
                    (Path<Comparable>) path,
                    (Comparable) typedValue
                );
                case IN -> buildInPredicate(cb, path, c.value());
                case BETWEEN -> buildBetweenPredicate(cb, path, c.value());
                case NULL -> cb.isNull(path);
                case NOT_NULL -> cb.isNotNull(path);
                // ... otros casos
            };
        };
    }

    // ==================== Path Navigation (Joins) ====================

    /**
     * Soporta notación con puntos para relaciones.
     * Ejemplo: "role.name" → root.get("role").get("name")
     */
    private Path<?> getPath(Root<?> root, String field) {
        String[] parts = field.split("\\.");
        Path<?> path = root;
        for (String part : parts) {
            path = path.get(part);
        }
        return path;
    }
}
```

### Ejemplos de Uso Real

#### Ejemplo 1: Filtrado Simple en Controlador

```java
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;
    private final FilterSpecificationBuilder specBuilder;

    @GetMapping
    public ResponseEntity<Page<UserDTO>> findAll(
            @RequestParam(required = false) String filter,
            Pageable pageable) {

        // Construir specification desde parámetro
        Specification<User> spec = specBuilder.build(filter, User.class);

        // Usar en el servicio
        Result<Page<User>, Exception> result = userService.findAll(spec, pageable);

        return result.fold(
            page -> ResponseEntity.ok(page.map(userMapper::toDTO)),
            error -> ResponseEntity.badRequest().build()
        );
    }
}

// Ejemplos de requests:
// GET /api/v1/users?filter=name:like:John
// GET /api/v1/users?filter=age:gte:25,status:eq:ACTIVE
// GET /api/v1/users?filter=email:like:@gmail.com,createdAt:gt:2024-01-01
```

#### Ejemplo 2: Operadores Avanzados

```java
// IN operator (lista de valores)
GET /api/v1/products?filter=category:in:ELECTRONICS;BOOKS;CLOTHING

// Genera SQL:
WHERE category IN ('ELECTRONICS', 'BOOKS', 'CLOTHING')

// BETWEEN operator (rango)
GET /api/v1/orders?filter=total:between:100;500

// Genera SQL:
WHERE total BETWEEN 100 AND 500

// Combinación compleja
GET /api/v1/users?filter=name:like:John,age:gte:18,age:lte:65,status:neq:BANNED

// Genera SQL:
WHERE name LIKE '%John%'
  AND age >= 18
  AND age <= 65
  AND status != 'BANNED'
```

#### Ejemplo 3: Navegación de Relaciones (Joins)

```java
// Filtrar por campo de relación
GET /api/v1/users?filter=role.name:eq:ADMIN

// Genera SQL con JOIN:
SELECT u.* FROM users u
JOIN roles r ON u.role_id = r.id
WHERE r.name = 'ADMIN' AND u.estado = true

// Relación anidada
GET /api/v1/orders?filter=customer.address.city:eq:Madrid

// Genera SQL:
SELECT o.* FROM orders o
JOIN customers c ON o.customer_id = c.id
JOIN addresses a ON c.address_id = a.id
WHERE a.city = 'Madrid'
```

#### Ejemplo 4: Composición Manual de Specifications

```java
// En el servicio, crear specifications complejas
public Result<List<User>, Exception> findActiveAdmins() {
    Specification<User> isActive = (root, query, cb) ->
        cb.equal(root.get("estado"), true);

    Specification<User> isAdmin = (root, query, cb) ->
        cb.equal(root.join("role").get("name"), "ADMIN");

    Specification<User> createdThisYear = (root, query, cb) ->
        cb.greaterThanOrEqualTo(
            root.get("fechaCreacion"),
            LocalDateTime.of(2024, 1, 1, 0, 0)
        );

    // Combinar con AND
    Specification<User> combined = isActive
        .and(isAdmin)
        .and(createdThisYear);

    return Result.of(() -> userRepository.findAll(combined));
}
```

#### Ejemplo 5: Specifications Reutilizables

```java
/**
 * Biblioteca de Specifications reutilizables.
 */
public class UserSpecifications {

    public static Specification<User> hasRole(String roleName) {
        return (root, query, cb) ->
            cb.equal(root.join("role").get("name"), roleName);
    }

    public static Specification<User> emailDomain(String domain) {
        return (root, query, cb) ->
            cb.like(root.get("email"), "%" + domain);
    }

    public static Specification<User> createdBetween(
            LocalDateTime start, LocalDateTime end) {
        return (root, query, cb) ->
            cb.between(root.get("fechaCreacion"), start, end);
    }

    public static Specification<User> isActive() {
        return (root, query, cb) ->
            cb.equal(root.get("estado"), true);
    }
}

// Uso:
Specification<User> spec = UserSpecifications.isActive()
    .and(UserSpecifications.hasRole("ADMIN"))
    .and(UserSpecifications.emailDomain("@company.com"));

List<User> admins = userRepository.findAll(spec);
```

### Conversión de Tipos Automática

```java
/**
 * Convierte valores String al tipo Java apropiado.
 */
private Object convertValue(String value, Class<?> targetType) {
    if (value == null) return null;

    try {
        if (targetType == String.class) return value;
        if (targetType == Long.class) return Long.parseLong(value);
        if (targetType == Integer.class) return Integer.parseInt(value);
        if (targetType == Boolean.class) return Boolean.parseBoolean(value);
        if (targetType == LocalDateTime.class) return LocalDateTime.parse(value);
        if (targetType == LocalDate.class) return LocalDate.parse(value);
        if (targetType.isEnum()) {
            return Enum.valueOf((Class<Enum>) targetType, value.toUpperCase());
        }
    } catch (Exception e) {
        log.warn("Error convirtiendo '{}' a {}", value, targetType);
    }

    return value;  // Fallback
}
```

### Ventajas del Pattern

| Ventaja | Descripción |
|---------|-------------|
| **Queries Dinámicas** | Construir filtros en runtime sin métodos nuevos |
| **Type Safety** | JPA Criteria API detecta errores en compilación |
| **Composición** | Combinar specifications como bloques LEGO |
| **Reutilización** | Crear biblioteca de filtros comunes |
| **Testeable** | Specifications son funciones puras |
| **DRY** | No repetir código de queries |

### Cuándo Usar

**✅ USAR cuando:**
- Necesitas filtros dinámicos desde la UI
- Múltiples combinaciones de criterios
- API pública donde usuarios filtran datos
- Queries complejas con joins

**❌ NO USAR cuando:**
- Query simple y estática
- Performance crítica (native SQL más rápido)
- Queries muy complejas (mejor SQL nativo)
- Equipo no familiarizado con Criteria API

---

## 8. Circuit Breaker Pattern

### Problema que Resuelve

En sistemas distribuidos, cuando un servicio falla:
```
Cliente → Servicio A → Servicio B (FALLA) → ⏳ Timeout
                                          → ⏳ Timeout
                                          → ⏳ Timeout
                                          → ⏳ Timeout
```

Problemas:
- Cascada de fallos (Servicio A también falla)
- Recursos agotados (threads bloqueados)
- Timeouts acumulativos
- Degradación total del sistema

**Circuit Breaker** actúa como un fusible eléctrico:
- Detecta fallos repetidos
- "Abre el circuito" para prevenir más llamadas
- Permite recuperación gradual
- Falla rápido en lugar de esperar

### Estados del Circuit Breaker

```
┌─────────────────────────────────────────────────────────┐
│                   CLOSED (Normal)                       │
│  Peticiones pasan normalmente al servicio               │
│  Monitorea fallos y latencia                            │
└───────────────┬─────────────────────────────────────────┘
                │
                │ Tasa de fallos > 50%
                │ (configurable)
                ▼
┌─────────────────────────────────────────────────────────┐
│                   OPEN (Circuito Abierto)               │
│  ❌ TODAS las peticiones fallan inmediatamente          │
│  ⏱ Espera 60 segundos (configurable)                    │
│  No se hacen llamadas al servicio                       │
└───────────────┬─────────────────────────────────────────┘
                │
                │ Después de waitDuration
                ▼
┌─────────────────────────────────────────────────────────┐
│               HALF-OPEN (Semi-abierto)                  │
│  Permite 3 peticiones de prueba (configurable)          │
│  Si todas OK → CLOSED                                   │
│  Si alguna falla → OPEN                                 │
└─────────────────────────────────────────────────────────┘
```

### Implementación con Resilience4j

```java
@Configuration
public class ResilienceConfig {

    /**
     * Configuración del Circuit Breaker.
     */
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        CircuitBreakerConfig defaultConfig = CircuitBreakerConfig.custom()
            // Tasa de fallos para abrir el circuito
            .failureRateThreshold(50)  // 50% de fallos

            // Tasa de llamadas lentas para abrir
            .slowCallRateThreshold(100)  // 100% (deshabilitado)
            .slowCallDurationThreshold(Duration.ofSeconds(2))

            // Tiempo en estado OPEN antes de HALF-OPEN
            .waitDurationInOpenState(Duration.ofSeconds(60))

            // Llamadas de prueba en HALF-OPEN
            .permittedNumberOfCallsInHalfOpenState(3)

            // Mínimo de llamadas para calcular métricas
            .minimumNumberOfCalls(10)

            // Ventana deslizante de 100 llamadas
            .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
            .slidingWindowSize(100)

            // Qué excepciones registrar como fallos
            .recordExceptions(Exception.class)
            .ignoreExceptions(IllegalArgumentException.class)
            .build();

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(defaultConfig);

        // Crear circuit breakers predefinidos
        CircuitBreaker defaultCb = registry.circuitBreaker("default");
        CircuitBreaker externalCb = registry.circuitBreaker("external");
        CircuitBreaker databaseCb = registry.circuitBreaker("database");

        // Event handlers para logging
        registerEventHandlers(defaultCb);

        return registry;
    }

    private void registerEventHandlers(CircuitBreaker cb) {
        cb.getEventPublisher()
            .onStateTransition(event ->
                log.warn("Circuit Breaker '{}' transición: {} -> {}",
                    event.getCircuitBreakerName(),
                    event.getStateTransition().getFromState(),
                    event.getStateTransition().getToState()))
            .onFailureRateExceeded(event ->
                log.warn("Circuit Breaker '{}' tasa de fallos excedida: {}%",
                    event.getCircuitBreakerName(),
                    event.getFailureRate()));
    }
}
```

### Ejemplos de Uso

#### Ejemplo 1: Proteger Llamada a Servicio Externo

```java
@Service
public class PaymentService {

    /**
     * @CircuitBreaker protege la llamada al gateway de pagos.
     * Si falla repetidamente, abre el circuito y usa fallback.
     */
    @CircuitBreaker(name = "paymentGateway", fallbackMethod = "fallbackPayment")
    @Retry(name = "paymentGateway")
    @TimeLimiter(name = "paymentGateway")
    public CompletableFuture<PaymentResult> processPayment(Payment payment) {
        log.info("Procesando pago: {}", payment.getId());

        // Llamada al servicio externo
        return CompletableFuture.supplyAsync(() -> {
            PaymentResponse response = paymentGatewayClient.charge(payment);
            return new PaymentResult(response.getTransactionId(), "SUCCESS");
        });
    }

    /**
     * Fallback cuando el circuit breaker está abierto.
     */
    private CompletableFuture<PaymentResult> fallbackPayment(
            Payment payment,
            Exception ex) {
        log.error("Circuit breaker OPEN - usando fallback para pago: {}",
                  payment.getId(), ex);

        // Opción 1: Encolar para procesar después
        paymentQueue.enqueue(payment);
        return CompletableFuture.completedFuture(
            new PaymentResult(null, "QUEUED")
        );

        // Opción 2: Retornar error amigable
        // return CompletableFuture.completedFuture(
        //     new PaymentResult(null, "SERVICE_UNAVAILABLE")
        // );
    }
}
```

#### Ejemplo 2: Circuit Breaker con Result Pattern

```java
@Service
public class ExternalApiService {

    private final CircuitBreaker circuitBreaker;

    public ExternalApiService(CircuitBreakerRegistry registry) {
        this.circuitBreaker = registry.circuitBreaker("external-api");
    }

    public Result<UserData, Exception> fetchUserData(String userId) {
        // Decorar la operación con circuit breaker
        Supplier<UserData> supplier = CircuitBreaker.decorateSupplier(
            circuitBreaker,
            () -> externalApiClient.getUser(userId)
        );

        // Usar Result.of para capturar excepciones
        return Result.of(supplier)
            .recover(ex -> {
                if (ex instanceof CallNotPermittedException) {
                    log.warn("Circuit breaker OPEN - usando caché");
                    return cacheService.getUserData(userId);
                }
                throw new RuntimeException(ex);
            });
    }
}
```

#### Ejemplo 3: Monitoreo de Estados

```java
@RestController
@RequestMapping("/actuator/circuit-breakers")
public class CircuitBreakerController {

    private final CircuitBreakerRegistry registry;

    @GetMapping
    public Map<String, CircuitBreakerStatus> getAll() {
        return registry.getAllCircuitBreakers().stream()
            .collect(Collectors.toMap(
                CircuitBreaker::getName,
                this::getStatus
            ));
    }

    @GetMapping("/{name}")
    public CircuitBreakerStatus getStatus(@PathVariable String name) {
        CircuitBreaker cb = registry.circuitBreaker(name);
        CircuitBreaker.Metrics metrics = cb.getMetrics();

        return new CircuitBreakerStatus(
            cb.getName(),
            cb.getState().name(),
            metrics.getFailureRate(),
            metrics.getNumberOfSuccessfulCalls(),
            metrics.getNumberOfFailedCalls()
        );
    }

    @PostMapping("/{name}/reset")
    public void reset(@PathVariable String name) {
        CircuitBreaker cb = registry.circuitBreaker(name);
        cb.reset();
        log.info("Circuit breaker '{}' reseteado manualmente", name);
    }
}
```

### Integración con Rate Limiter y Retry

```java
/**
 * Combinar Circuit Breaker + Rate Limiter + Retry
 * para máxima resiliencia.
 */
@Service
public class ResilientService {

    @CircuitBreaker(name = "backend")      // Protege contra fallos en cascada
    @RateLimiter(name = "backend")         // Limita tasa de llamadas
    @Retry(name = "backend")               // Reintenta fallos transitorios
    @TimeLimiter(name = "backend")         // Timeout configurable
    public CompletableFuture<Data> fetchData(String id) {
        return CompletableFuture.supplyAsync(() -> {
            return backendClient.getData(id);
        });
    }
}
```

**Orden de ejecución**:
1. **Rate Limiter**: ¿Excede tasa de llamadas? → Falla rápido
2. **Circuit Breaker**: ¿Circuito abierto? → Falla rápido
3. **Time Limiter**: Inicia timeout
4. **Retry**: Si falla, reintenta (hasta max attempts)
5. **Circuit Breaker**: Registra resultado (éxito/fallo)

### Configuración YAML

```yaml
resilience4j:
  circuitbreaker:
    configs:
      default:
        failure-rate-threshold: 50
        slow-call-rate-threshold: 100
        slow-call-duration-threshold: 2s
        wait-duration-in-open-state: 60s
        permitted-number-of-calls-in-half-open-state: 3
        minimum-number-of-calls: 10
        sliding-window-type: COUNT_BASED
        sliding-window-size: 100
    instances:
      paymentGateway:
        base-config: default
        failure-rate-threshold: 30  # Más sensible
      externalApi:
        base-config: default
        wait-duration-in-open-state: 120s  # Espera más tiempo

  retry:
    configs:
      default:
        max-attempts: 3
        wait-duration: 500ms
        exponential-backoff-multiplier: 2
    instances:
      paymentGateway:
        max-attempts: 2
        wait-duration: 1s

  ratelimiter:
    configs:
      default:
        limit-for-period: 100
        limit-refresh-period: 1s
        timeout-duration: 5s
```

### Métricas y Observabilidad

```java
// Resilience4j expone métricas automáticamente
// Disponibles en /actuator/metrics

// Métricas del Circuit Breaker:
resilience4j.circuitbreaker.calls{name="backend", kind="successful"}
resilience4j.circuitbreaker.calls{name="backend", kind="failed"}
resilience4j.circuitbreaker.state{name="backend", state="closed"}
resilience4j.circuitbreaker.failure.rate{name="backend"}

// Métricas de Retry:
resilience4j.retry.calls{name="backend", kind="successful_without_retry"}
resilience4j.retry.calls{name="backend", kind="successful_with_retry"}
resilience4j.retry.calls{name="backend", kind="failed_with_retry"}

// Integración con Prometheus/Grafana para dashboards
```

### Cuándo Usar

**✅ USAR Circuit Breaker cuando:**
- Llamas a servicios externos (APIs, bases de datos remotas)
- Sistema distribuido con múltiples servicios
- Necesitas prevenir cascadas de fallos
- Quieres degradación elegante
- Tienes mecanismo de fallback

**❌ NO USAR cuando:**
- Aplicación monolítica sin dependencias externas
- Operación crítica que DEBE completarse (sin fallback)
- Servicio interno confiable (mismo servidor)
- Operaciones read-only sin efectos secundarios

### Buenas Prácticas

1. **Siempre definir fallback**: Nunca dejar al usuario sin respuesta
2. **Monitorear transiciones**: Alerta cuando circuito se abre
3. **Ajustar thresholds**: Basado en SLAs y tolerancia a fallos
4. **Combinar con Retry**: Para fallos transitorios
5. **Usar con Rate Limiter**: Proteger servicios downstream
6. **Testing**: Simular fallos para verificar comportamiento

---

## Conclusión

Los patrones de diseño en APiGen no son solo código elegante, sino **soluciones probadas** a problemas reales:

1. **Result Pattern**: Manejo de errores explícito y funcional
2. **Repository + Specifications**: Queries dinámicas con type-safety
3. **Service Layer**: Centralización de lógica de negocio
4. **Domain Events**: Desacoplamiento y extensibilidad
5. **Soft Delete**: Recuperación de datos y auditoría
6. **Builder**: Creación fluida de objetos complejos
7. **Strategy (Specifications)**: Filtros dinámicos componibles
8. **Circuit Breaker**: Resiliencia en sistemas distribuidos

### Próximos Pasos

- Explorar código en `src/main/java/com/jnzader/apigen/core/`
- Revisar tests en `src/test/java/`
- Experimentar con variaciones de los patrones
- Aplicar en tus propios proyectos

**¡Happy Coding!** 🚀
