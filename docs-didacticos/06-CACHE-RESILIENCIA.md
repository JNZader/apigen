# Caché y Resiliencia en APiGen

## Tabla de Contenidos
- [Introducción](#introducción)
- [Caché Multi-nivel con Caffeine](#caché-multi-nivel-con-caffeine)
- [Circuit Breaker con Resilience4j](#circuit-breaker-con-resilience4j)
- [Retry Pattern](#retry-pattern)
- [Rate Limiting](#rate-limiting)
- [Integración con Métricas](#integración-con-métricas)
- [Mejores Prácticas](#mejores-prácticas)

---

## Introducción

APiGen implementa múltiples patrones de resiliencia para garantizar alta disponibilidad y rendimiento:

- **Caché Multi-nivel**: Reduce la carga en base de datos con Caffeine
- **Circuit Breaker**: Protege contra fallos en cascada
- **Retry Pattern**: Reintentos inteligentes con backoff exponencial
- **Rate Limiting**: Limita solicitudes para prevenir abusos

### Stack Tecnológico

```gradle
// Caching
implementation "com.github.ben-manes.caffeine:caffeine:3.2.3"

// Resilience4j
implementation "io.github.resilience4j:resilience4j-spring-boot3:2.3.0"
implementation "io.github.resilience4j:resilience4j-circuitbreaker:2.3.0"
implementation "io.github.resilience4j:resilience4j-ratelimiter:2.3.0"
implementation "io.github.resilience4j:resilience4j-retry:2.3.0"
implementation "io.github.resilience4j:resilience4j-micrometer:2.3.0"
```

---

## Caché Multi-nivel con Caffeine

### ¿Por qué Caffeine?

Caffeine es una biblioteca de caché de alto rendimiento para Java, basada en el diseño de Guava Cache pero con mejor rendimiento (hasta 10x más rápido).

**Ventajas:**
- Alto rendimiento con algoritmo W-TinyLFU
- Expiration policies flexibles (TTL, TTI)
- Estadísticas integradas
- Thread-safe sin sincronización excesiva
- Listeners para eventos de caché

### Configuración de Caché

#### 1. AppProperties - Propiedades Type-Safe

```java
/**
 * Propiedades de caché.
 */
public record CacheProperties(
        CacheConfig entities,
        CacheConfig lists,
        CacheConfig counts
) {
    public CacheProperties {
        if (entities == null) entities = new CacheConfig(1000, Duration.ofMinutes(10));
        if (lists == null) lists = new CacheConfig(100, Duration.ofMinutes(5));
        if (counts == null) counts = new CacheConfig(50, Duration.ofMinutes(2));
    }

    public record CacheConfig(
            @Positive long maxSize,
            Duration expireAfterWrite
    ) {}
}
```

#### 2. application.yaml - Configuración

```yaml
app:
  cache:
    entities:
      max-size: 1000
      expire-after-write: 10m
    lists:
      max-size: 100
      expire-after-write: 5m
    counts:
      max-size: 50
      expire-after-write: 2m
```

#### 3. CacheConfig - Configuración de Spring Cache

```java
@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {

    private static final Logger log = LoggerFactory.getLogger(CacheConfig.class);

    private final AppProperties.CacheProperties cacheProperties;

    public CacheConfig(AppProperties appProperties) {
        this.cacheProperties = appProperties != null && appProperties.cache() != null
                ? appProperties.cache()
                : new AppProperties.CacheProperties(null, null, null);
    }

    /**
     * Configura el CacheManager con múltiples cachés especializados.
     * Cada caché usa su propia configuración optimizada.
     */
    @Bean
    @Override
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();

        List<CaffeineCache> caches = Arrays.asList(
                buildCache("entities", entitiesCaffeineBuilder()),
                buildCache("lists", listsCaffeineBuilder()),
                buildCache("counts", countsCaffeineBuilder())
        );

        cacheManager.setCaches(caches);

        log.info("Cache manager configurado con Caffeine. Cachés especializados: entities ({}), lists ({}), counts ({})",
                cacheProperties.entities().expireAfterWrite(),
                cacheProperties.lists().expireAfterWrite(),
                cacheProperties.counts().expireAfterWrite());

        return cacheManager;
    }

    /**
     * Construye un caché Caffeine con la configuración dada.
     */
    private CaffeineCache buildCache(String name, Caffeine<Object, Object> caffeineBuilder) {
        return new CaffeineCache(name, caffeineBuilder.build());
    }

    /**
     * Builder específico para caché de entidades.
     * Mayor tamaño y tiempo de expiración.
     */
    private Caffeine<Object, Object> entitiesCaffeineBuilder() {
        var config = cacheProperties.entities();
        return Caffeine.newBuilder()
                .maximumSize(config.maxSize())
                .expireAfterWrite(config.expireAfterWrite())
                .expireAfterAccess(Duration.ofMinutes(30))
                .recordStats()
                .removalListener((key, value, cause) ->
                        log.debug("Cache 'entities' - removida key: {}, causa: {}", key, cause));
    }

    /**
     * Builder específico para caché de listas.
     * Menor tamaño y tiempo de expiración más corto.
     */
    private Caffeine<Object, Object> listsCaffeineBuilder() {
        var config = cacheProperties.lists();
        return Caffeine.newBuilder()
                .maximumSize(config.maxSize())
                .expireAfterWrite(config.expireAfterWrite())
                .recordStats()
                .removalListener((key, value, cause) ->
                        log.debug("Cache 'lists' - removida key: {}, causa: {}", key, cause));
    }

    /**
     * Builder específico para caché de conteos.
     * Muy pequeño y expiración rápida.
     */
    private Caffeine<Object, Object> countsCaffeineBuilder() {
        var config = cacheProperties.counts();
        return Caffeine.newBuilder()
                .maximumSize(config.maxSize())
                .expireAfterWrite(config.expireAfterWrite())
                .recordStats()
                .removalListener((key, value, cause) ->
                        log.debug("Cache 'counts' - removida key: {}, causa: {}", key, cause));
    }

    /**
     * Manejador de errores de caché.
     * Loguea los errores pero permite que la aplicación continúe funcionando.
     */
    @Override
    public CacheErrorHandler errorHandler() {
        return new SimpleCacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                log.error("Error al obtener del caché '{}' con key '{}': {}",
                        cache.getName(), key, exception.getMessage());
            }

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                log.error("Error al guardar en caché '{}' con key '{}': {}",
                        cache.getName(), key, exception.getMessage());
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                log.error("Error al eliminar del caché '{}' con key '{}': {}",
                        cache.getName(), key, exception.getMessage());
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                log.error("Error al limpiar caché '{}': {}",
                        cache.getName(), exception.getMessage());
            }
        };
    }
}
```

### Tipos de Caché

#### 1. Caché de Entidades (entities)

**Propósito:** Cachear entidades individuales por ID
**Configuración:**
- Tamaño máximo: 1000 entradas
- TTL (expireAfterWrite): 10 minutos
- TTI (expireAfterAccess): 30 minutos
- Estadísticas: Habilitadas

**Uso:**
```java
@Cacheable(value = "entities",
           key = "#root.target.entityName + ':' + #id",
           unless = "#result.isFailure()")
public Result<E, Exception> findById(ID id) {
    log.debug("Buscando {} con ID: {}", getEntityName(), id);
    return Result.fromOptional(
            baseRepository.findById(id),
            () -> new ResourceNotFoundException("Entity not found: " + id)
    );
}
```

**Key Pattern:** `{EntityName}:{id}`
- Ejemplo: `User:123`, `Product:456`

#### 2. Caché de Listas (lists)

**Propósito:** Cachear resultados de búsquedas paginadas
**Configuración:**
- Tamaño máximo: 100 entradas
- TTL: 5 minutos
- Estadísticas: Habilitadas

**Uso:**
```java
@Cacheable(value = "lists",
        key = "#root.target.entityName + ':all:' + #pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort.toString()",
        unless = "#result.isFailure()")
public Result<Page<E>, Exception> findAll(Pageable pageable) {
    return Result.of(() -> {
        log.debug("Buscando entidades de tipo {} con paginación: {}", getEntityName(), pageable);
        return baseRepository.findAll(pageable);
    });
}
```

**Key Pattern:** `{EntityName}:{operation}:{page}:{size}:{sort}`
- Ejemplo: `User:all:0:20:id:ASC`
- Ejemplo: `User:active:1:10:name:DESC`

#### 3. Caché de Conteos (counts)

**Propósito:** Cachear resultados de COUNT queries
**Configuración:**
- Tamaño máximo: 50 entradas
- TTL: 2 minutos
- Estadísticas: Habilitadas

**Key Pattern:** `{EntityName}:count{Operation}`
- Ejemplo: `User:count`, `Product:countActive`

### Anotaciones de Caché

#### @Cacheable - Cachea el resultado

```java
@Cacheable(value = "entities",
           key = "#root.target.entityName + ':' + #id",
           unless = "#result.isFailure()")
public Result<E, Exception> findById(ID id) {
    // Se ejecuta solo si no está en caché
    return repository.findById(id);
}
```

**Parámetros:**
- `value`: Nombre del caché a usar
- `key`: SpEL expression para generar la key
- `unless`: Condición para NO cachear (ej: resultados fallidos)
- `condition`: Condición para cachear

**SpEL Context:**
- `#root.target.entityName`: Nombre de la entidad actual
- `#id`: Parámetro del método
- `#result`: Valor de retorno
- `#pageable`: Objeto Pageable

#### @CacheEvict - Invalida el caché

```java
@CacheEvict(value = "entities", key = "#root.target.entityName + ':' + #id")
public Result<E, Exception> update(ID id, E entity) {
    // Invalida la entrada del caché antes de ejecutar
    return updateLogic(id, entity);
}
```

**Parámetros:**
- `value`: Nombre del caché
- `key`: Key a invalidar
- `allEntries`: Si es `true`, invalida TODO el caché
- `beforeInvocation`: Si es `true`, invalida antes de ejecutar el método

**Problema con allEntries:**
```java
// ❌ MALO: Invalida TODO el caché
@CacheEvict(value = "lists", allEntries = true)
public Result<E, Exception> update(ID id, E entity) {
    // Invalida listas de TODAS las entidades
    // Cache hit rate: ~20%
}

// ✅ BUENO: Invalida solo las listas de esta entidad
@CacheEvict(value = "entities", key = "#root.target.entityName + ':' + #id")
public Result<E, Exception> update(ID id, E entity) {
    cacheEvictionService.evictListsByEntityName(getEntityName());
    // Cache hit rate: ~80%
}
```

#### @CachePut - Actualiza el caché

```java
@CachePut(value = "entities", key = "#root.target.entityName + ':' + #entity.id")
public Result<E, Exception> save(E entity) {
    // Ejecuta el método y actualiza el caché con el resultado
    return repository.save(entity);
}
```

### CacheEvictionService - Eviction Selectivo

El `CacheEvictionService` permite invalidar caché de forma selectiva, evitando el uso de `allEntries = true`.

```java
@Service
public class CacheEvictionService {

    private static final Logger log = LoggerFactory.getLogger(CacheEvictionService.class);

    private final CacheManager cacheManager;

    public CacheEvictionService(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    /**
     * Invalida todas las entradas del cache "lists" que empiezan con el prefijo de la entidad.
     *
     * Por ejemplo, si entityName = "User", invalida:
     * - "User:all:0:20:id:ASC"
     * - "User:active:0:10:name:DESC"
     *
     * Pero NO invalida:
     * - "Product:all:0:20:id:ASC"
     * - "Order:active:0:10:name:DESC"
     *
     * @param entityName Nombre de la entidad cuyas listas se deben invalidar
     */
    public void evictListsByEntityName(String entityName) {
        org.springframework.cache.Cache listsCache = cacheManager.getCache("lists");

        if (listsCache == null) {
            log.warn("Cache 'lists' no encontrado");
            return;
        }

        if (listsCache instanceof CaffeineCache caffeineCache) {
            Cache<Object, Object> nativeCache = caffeineCache.getNativeCache();
            String prefix = entityName + ":";

            long evictedCount = nativeCache.asMap().keySet().stream()
                    .filter(key -> key.toString().startsWith(prefix))
                    .peek(key -> {
                        nativeCache.invalidate(key);
                        log.debug("Cache evicted: {}", key);
                    })
                    .count();

            if (evictedCount > 0) {
                log.info("Evicted {} cache entries for entity: {}", evictedCount, entityName);
            }
        } else {
            // Fallback: invalidar todo el cache si no es Caffeine
            log.warn("Cache 'lists' no es CaffeineCache, usando invalidación completa");
            listsCache.clear();
        }
    }

    /**
     * Invalida una entrada específica del cache de entidades.
     */
    public void evictEntity(String entityName, Object id) {
        org.springframework.cache.Cache entitiesCache = cacheManager.getCache("entities");

        if (entitiesCache != null) {
            String key = entityName + ":" + id;
            entitiesCache.evict(key);
            log.debug("Entity cache evicted: {}", key);
        }
    }

    /**
     * Invalida todas las entradas relacionadas con una entidad (entity + lists).
     */
    public void evictEntityAndLists(String entityName, Object id) {
        if (id != null) {
            evictEntity(entityName, id);
        }
        evictListsByEntityName(entityName);
    }

    /**
     * Invalida el cache de counts para una entidad.
     */
    public void evictCounts(String entityName) {
        org.springframework.cache.Cache countsCache = cacheManager.getCache("counts");

        if (countsCache != null) {
            if (countsCache instanceof CaffeineCache caffeineCache) {
                Cache<Object, Object> nativeCache = caffeineCache.getNativeCache();
                String prefix = entityName + ":";

                nativeCache.asMap().keySet().stream()
                        .filter(key -> key.toString().startsWith(prefix))
                        .forEach(nativeCache::invalidate);
            }
        }
    }

    /**
     * Invalida todos los caches relacionados con una entidad.
     * Útil para operaciones de update/delete.
     */
    public void evictAll(String entityName, Object id) {
        evictEntity(entityName, id);
        evictListsByEntityName(entityName);
        evictCounts(entityName);
    }
}
```

**Uso en servicios:**

```java
@Service
public class BaseServiceImpl<E extends BaseEntity<ID>, ID> implements BaseService<E, ID> {

    @Autowired
    private CacheEvictionService cacheEvictionService;

    @Override
    @Transactional
    @CacheEvict(value = "entities", key = "#root.target.entityName + ':' + #id")
    public Result<E, Exception> update(ID id, E entity) {
        return findById(id).flatMap(existingEntity -> {
            setEntityId(entity, id, existingEntity);

            return Result.of(() -> {
                E updated = baseRepository.save(entity);
                log.info("{} actualizado exitosamente con ID: {}", getEntityName(), id);

                // Invalidar listas solo de esta entidad
                cacheEvictionService.evictListsByEntityName(getEntityName());

                return updated;
            });
        });
    }
}
```

**Mejora de rendimiento:**
- **Sin eviction selectivo:** Cache hit rate ~20% (invalida TODO)
- **Con eviction selectivo:** Cache hit rate ~80% (invalida solo lo necesario)

---

## Circuit Breaker con Resilience4j

### ¿Qué es un Circuit Breaker?

Un Circuit Breaker es un patrón de diseño que previene fallos en cascada al detectar servicios que fallan y evitar llamadas innecesarias.

### Estados del Circuit Breaker

```
          ┌──────────┐
          │  CLOSED  │ ◄─── Estado normal
          │          │      Todas las llamadas pasan
          └────┬─────┘
               │
               │ Failure rate > threshold
               │
               ▼
          ┌──────────┐
          │   OPEN   │ ◄─── Estado de fallo
          │          │      Rechaza todas las llamadas
          └────┬─────┘      Ejecuta fallback
               │
               │ waitDuration expired
               │
               ▼
          ┌──────────┐
          │ HALF_OPEN│ ◄─── Estado de prueba
          │          │      Permite N llamadas de prueba
          └──────────┘
               │
               ├─► Success → CLOSED
               └─► Failure → OPEN
```

### Configuración

#### 1. application.yaml

```yaml
resilience4j:
  circuitbreaker:
    configs:
      default:
        registerHealthIndicator: true
        slidingWindowSize: 100
        minimumNumberOfCalls: 10
        permittedNumberOfCallsInHalfOpenState: 3
        automaticTransitionFromOpenToHalfOpenEnabled: true
        waitDurationInOpenState: 60s
        failureRateThreshold: 50
        slowCallRateThreshold: 80
        slowCallDurationThreshold: 2s
        recordExceptions:
          - java.lang.Exception
        ignoreExceptions:
          - java.lang.IllegalArgumentException
    instances:
      external:
        baseConfig: default
        waitDurationInOpenState: 30s
      database:
        baseConfig: default
        failureRateThreshold: 25
```

**Parámetros:**
- `slidingWindowSize`: Tamaño de la ventana deslizante (100 llamadas)
- `minimumNumberOfCalls`: Mínimo de llamadas antes de calcular métricas (10)
- `failureRateThreshold`: % de fallos para abrir el circuito (50%)
- `slowCallRateThreshold`: % de llamadas lentas para abrir (80%)
- `slowCallDurationThreshold`: Duración para considerar una llamada lenta (2s)
- `waitDurationInOpenState`: Tiempo en estado OPEN antes de HALF_OPEN (60s)
- `permittedNumberOfCallsInHalfOpenState`: Llamadas de prueba en HALF_OPEN (3)

#### 2. ResilienceConfig.java

```java
@Configuration
public class ResilienceConfig {

    private static final Logger log = LoggerFactory.getLogger(ResilienceConfig.class);

    /**
     * Configuración del Circuit Breaker.
     */
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        CircuitBreakerConfig defaultConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .slowCallRateThreshold(100)
                .slowCallDurationThreshold(Duration.ofSeconds(2))
                .waitDurationInOpenState(Duration.ofSeconds(60))
                .permittedNumberOfCallsInHalfOpenState(3)
                .minimumNumberOfCalls(10)
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(100)
                .recordExceptions(Exception.class)
                .ignoreExceptions(IllegalArgumentException.class)
                .build();

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(defaultConfig);

        // Crear circuit breakers predefinidos
        CircuitBreaker defaultCb = registry.circuitBreaker("default");
        CircuitBreaker externalCb = registry.circuitBreaker("external");
        CircuitBreaker databaseCb = registry.circuitBreaker("database");

        // Event handlers para logging
        registerCircuitBreakerEventHandlers(defaultCb);
        registerCircuitBreakerEventHandlers(externalCb);
        registerCircuitBreakerEventHandlers(databaseCb);

        log.info("Circuit Breaker registry initialized with default, external, and database breakers");

        return registry;
    }

    private void registerCircuitBreakerEventHandlers(CircuitBreaker cb) {
        cb.getEventPublisher()
                .onStateTransition(event ->
                        log.warn("Circuit Breaker '{}' state transition: {} -> {}",
                                event.getCircuitBreakerName(),
                                event.getStateTransition().getFromState(),
                                event.getStateTransition().getToState()))
                .onSlowCallRateExceeded(event ->
                        log.warn("Circuit Breaker '{}' slow call rate exceeded: {}%",
                                event.getCircuitBreakerName(),
                                event.getSlowCallRate()))
                .onFailureRateExceeded(event ->
                        log.warn("Circuit Breaker '{}' failure rate exceeded: {}%",
                                event.getCircuitBreakerName(),
                                event.getFailureRate()));
    }
}
```

### Uso de @CircuitBreaker

```java
@Service
public class ExternalApiService {

    private static final Logger log = LoggerFactory.getLogger(ExternalApiService.class);

    /**
     * Llamada a API externa con Circuit Breaker
     */
    @CircuitBreaker(name = "external", fallbackMethod = "fallbackGetData")
    public Result<ApiResponse, Exception> getData(String id) {
        log.info("Llamando a API externa con id: {}", id);

        // Simulación de llamada externa
        return Result.of(() -> {
            ApiResponse response = restTemplate.getForObject(
                "https://api.example.com/data/" + id,
                ApiResponse.class
            );

            if (response == null) {
                throw new RuntimeException("Response is null");
            }

            return response;
        });
    }

    /**
     * Método fallback cuando el Circuit Breaker está OPEN
     *
     * IMPORTANTE: Debe tener la misma firma + Exception al final
     */
    private Result<ApiResponse, Exception> fallbackGetData(String id, Exception e) {
        log.warn("Circuit Breaker OPEN - usando fallback para id: {}. Error: {}",
                 id, e.getMessage());

        // Opciones de fallback:
        // 1. Retornar datos cacheados
        // 2. Retornar datos por defecto
        // 3. Retornar error con mensaje amigable

        return Result.failure(
            new ServiceUnavailableException(
                "El servicio externo no está disponible temporalmente. Por favor intente más tarde."
            )
        );
    }

    /**
     * Ejemplo con múltiples patrones de resiliencia
     */
    @CircuitBreaker(name = "external", fallbackMethod = "fallbackGetDataWithRetry")
    @Retry(name = "external")
    @RateLimiter(name = "public-api")
    public Result<ApiResponse, Exception> getDataWithRetry(String id) {
        return getData(id);
    }

    private Result<ApiResponse, Exception> fallbackGetDataWithRetry(String id, Exception e) {
        return fallbackGetData(id, e);
    }
}
```

### Instancias de Circuit Breaker

#### 1. default

**Uso:** Servicios internos
```java
@CircuitBreaker(name = "default")
public Result<Data, Exception> internalOperation() {
    // ...
}
```

#### 2. external

**Uso:** APIs externas (más permisivo)
**Configuración:**
- waitDuration: 30s (recupera más rápido)

```java
@CircuitBreaker(name = "external")
public Result<Data, Exception> externalApiCall() {
    // ...
}
```

#### 3. database

**Uso:** Operaciones de base de datos (más estricto)
**Configuración:**
- failureRateThreshold: 25% (más sensible)

```java
@CircuitBreaker(name = "database")
public Result<Data, Exception> complexDatabaseQuery() {
    // ...
}
```

---

## Retry Pattern

### ¿Qué es el Retry Pattern?

El Retry Pattern reintenta automáticamente operaciones fallidas con estrategias de backoff exponencial.

### Configuración

#### 1. application.yaml

```yaml
resilience4j:
  retry:
    configs:
      default:
        maxAttempts: 3
        waitDuration: 500ms
        enableExponentialBackoff: true
        exponentialBackoffMultiplier: 2
        retryExceptions:
          - java.lang.Exception
        ignoreExceptions:
          - java.lang.IllegalArgumentException
    instances:
      database:
        baseConfig: default
      external:
        maxAttempts: 2
        waitDuration: 1s
```

**Parámetros:**
- `maxAttempts`: Número máximo de intentos (3)
- `waitDuration`: Tiempo de espera inicial (500ms)
- `enableExponentialBackoff`: Habilitar backoff exponencial
- `exponentialBackoffMultiplier`: Multiplicador para backoff (2x)
- `retryExceptions`: Excepciones que disparan reintentos
- `ignoreExceptions`: Excepciones que NO disparan reintentos

**Backoff Exponencial:**
```
Intento 1: inmediato
Intento 2: 500ms espera
Intento 3: 1000ms espera (500ms * 2)
Intento 4: 2000ms espera (1000ms * 2)
```

#### 2. ResilienceConfig.java

```java
@Bean
public RetryRegistry retryRegistry() {
    RetryConfig defaultConfig = RetryConfig.custom()
            .maxAttempts(3)
            .waitDuration(Duration.ofMillis(500))
            .retryExceptions(Exception.class)
            .ignoreExceptions(IllegalArgumentException.class)
            .build();

    RetryConfig conservativeConfig = RetryConfig.custom()
            .maxAttempts(2)
            .waitDuration(Duration.ofSeconds(1))
            .retryExceptions(Exception.class)
            .build();

    RetryRegistry registry = RetryRegistry.of(defaultConfig);

    // Crear retry configs predefinidos
    Retry defaultRetry = registry.retry("default");
    registry.retry("conservative", conservativeConfig);
    registry.retry("database");

    // Event handler para logging
    defaultRetry.getEventPublisher()
            .onRetry(event -> log.warn("Retry attempt {} for '{}'",
                    event.getNumberOfRetryAttempts(), event.getName()));

    log.info("Retry registry initialized with default, conservative, and database retries");

    return registry;
}
```

### Uso de @Retry

```java
@Service
public class DatabaseService {

    private static final Logger log = LoggerFactory.getLogger(DatabaseService.class);

    /**
     * Query compleja con reintentos automáticos
     */
    @Retry(name = "database")
    public Result<List<Entity>, Exception> complexQuery() {
        return Result.of(() -> {
            log.info("Ejecutando query compleja");

            // Query que puede fallar temporalmente
            // por locks, deadlocks, timeouts, etc.
            return entityRepository.findByComplexCriteria();
        });
    }

    /**
     * Llamada externa con reintentos
     */
    @Retry(name = "external")
    @CircuitBreaker(name = "external", fallbackMethod = "fallbackExternalCall")
    public Result<Data, Exception> externalCall() {
        return Result.of(() -> {
            // Retry se ejecuta primero
            // Si todos los reintentos fallan, Circuit Breaker registra el fallo
            return restTemplate.getForObject("https://api.example.com/data", Data.class);
        });
    }

    private Result<Data, Exception> fallbackExternalCall(Exception e) {
        log.error("Todos los reintentos fallaron, usando fallback: {}", e.getMessage());
        return Result.failure(new ServiceUnavailableException("Servicio no disponible"));
    }
}
```

### Instancias de Retry

#### 1. default

**Configuración:**
- maxAttempts: 3
- waitDuration: 500ms
- exponentialBackoff: enabled (2x)

**Tiempo total máximo:** ~3.5 segundos
```
Intento 1: 0ms
Intento 2: 500ms
Intento 3: 1000ms
Intento 4: 2000ms
Total: 3500ms
```

#### 2. database

**Uso:** Queries de base de datos
**Configuración:** Usa defaults

```java
@Retry(name = "database")
public Result<Data, Exception> databaseQuery() {
    // ...
}
```

#### 3. external

**Uso:** APIs externas (más conservador)
**Configuración:**
- maxAttempts: 2
- waitDuration: 1s

**Tiempo total máximo:** ~3 segundos
```
Intento 1: 0ms
Intento 2: 1000ms
Intento 3: 2000ms
Total: 3000ms
```

### Excepciones a Ignorar

```java
RetryConfig config = RetryConfig.custom()
        .retryExceptions(Exception.class)
        .ignoreExceptions(
            IllegalArgumentException.class,  // Errores de validación
            ResourceNotFoundException.class,  // Entidad no existe
            BadRequestException.class         // Request inválido
        )
        .build();
```

**Razón:** Estas excepciones son errores del cliente, no transitorios. Reintentarlas no ayudará.

---

## Rate Limiting

### ¿Qué es Rate Limiting?

Rate Limiting limita el número de solicitudes que un cliente puede hacer en un período de tiempo, protegiendo contra abusos y garantizando disponibilidad para todos los usuarios.

### RateLimitingFilter - Basado en IP

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
                .maximumSize(10000) // Máximo 10k IPs en cache
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
                    """.formatted(maxRequestsPerWindow, windowDuration.toSeconds(),
                                  windowDuration.toSeconds()));

            return;
        }

        filterChain.doFilter(request, response);
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

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // No aplicar rate limit a actuator endpoints
        return path.startsWith("/actuator/");
    }
}
```

### Configuración

```yaml
app:
  rate-limit:
    max-requests: 100
    window-seconds: 60
```

### Headers de Rate Limit

```http
HTTP/1.1 200 OK
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 73
X-RateLimit-Reset: 60
```

**Significado:**
- `X-RateLimit-Limit`: Máximo de requests permitidos
- `X-RateLimit-Remaining`: Requests restantes en la ventana actual
- `X-RateLimit-Reset`: Segundos hasta que se resetee el límite

### Respuesta cuando se excede el límite

```http
HTTP/1.1 429 Too Many Requests
Content-Type: application/json
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 0
X-RateLimit-Reset: 60

{
  "error": "Too Many Requests",
  "message": "Has excedido el límite de 100 requests por 60 segundos",
  "retryAfter": 60
}
```

### @RateLimiter Annotation

Resilience4j también proporciona una annotation para rate limiting a nivel de método:

```java
@Service
public class PublicApiService {

    /**
     * Endpoint público con rate limit estricto
     */
    @RateLimiter(name = "strict")
    public Result<Data, Exception> publicEndpoint() {
        return Result.of(() -> {
            // Máximo 10 llamadas por segundo
            return processPublicRequest();
        });
    }

    /**
     * Endpoint con rate limit por defecto
     */
    @RateLimiter(name = "default")
    public Result<Data, Exception> standardEndpoint() {
        return Result.of(() -> {
            // Máximo 100 llamadas por segundo
            return processRequest();
        });
    }
}
```

### Configuración en application.yaml

```yaml
resilience4j:
  ratelimiter:
    configs:
      default:
        registerHealthIndicator: true
        limitForPeriod: 100
        limitRefreshPeriod: 1s
        timeoutDuration: 0s  # fail-fast
    instances:
      strict:
        limitForPeriod: 10
        limitRefreshPeriod: 1s
        timeoutDuration: 0s
      public-api:
        baseConfig: default
```

**Parámetros:**
- `limitForPeriod`: Número de llamadas permitidas por período
- `limitRefreshPeriod`: Duración del período
- `timeoutDuration`:
  - `0s` = fail-fast (rechaza inmediatamente)
  - `> 0` = espera hasta ese tiempo antes de rechazar

---

## Integración con Métricas

### Métricas de Resilience4j en Micrometer

Resilience4j se integra automáticamente con Micrometer para exponer métricas:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
  health:
    circuitbreakers:
      enabled: true
    ratelimiters:
      enabled: true
```

### Métricas de Circuit Breaker

```
# Número total de llamadas
resilience4j_circuitbreaker_calls_total{name="external",kind="successful"} 850
resilience4j_circuitbreaker_calls_total{name="external",kind="failed"} 150

# Tasa de fallos
resilience4j_circuitbreaker_failure_rate{name="external"} 0.15

# Estado del circuito (0=CLOSED, 1=OPEN, 2=HALF_OPEN)
resilience4j_circuitbreaker_state{name="external"} 0

# Llamadas lentas
resilience4j_circuitbreaker_slow_calls{name="external"} 20
resilience4j_circuitbreaker_slow_call_rate{name="external"} 0.02
```

### Métricas de Retry

```
# Reintentos exitosos/fallidos
resilience4j_retry_calls{name="database",kind="successful_without_retry"} 950
resilience4j_retry_calls{name="database",kind="successful_with_retry"} 30
resilience4j_retry_calls{name="database",kind="failed_without_retry"} 5
resilience4j_retry_calls{name="database",kind="failed_with_retry"} 15
```

### Métricas de Rate Limiter

```
# Llamadas permitidas/rechazadas
resilience4j_ratelimiter_available_permissions{name="public-api"} 73
resilience4j_ratelimiter_waiting_threads{name="public-api"} 0
```

### Métricas de Caché (Caffeine)

```
# Cache hits/misses
cache_gets_total{cache="entities",result="hit"} 8500
cache_gets_total{cache="entities",result="miss"} 1500

# Cache hit rate
cache_hit_ratio{cache="entities"} 0.85

# Tamaño del cache
cache_size{cache="entities"} 245

# Evictions
cache_evictions_total{cache="entities"} 155
```

### Dashboard de Grafana

```yaml
# Ejemplo de query para Grafana
# Circuit Breaker - Failure Rate
rate(resilience4j_circuitbreaker_calls_total{kind="failed"}[5m]) /
rate(resilience4j_circuitbreaker_calls_total[5m])

# Cache Hit Rate
sum(rate(cache_gets_total{result="hit"}[5m])) /
sum(rate(cache_gets_total[5m]))

# Rate Limit - Requests Rechazados
rate(http_server_requests_total{status="429"}[1m])
```

### Alertas Basadas en Circuit Breaker

```yaml
# Prometheus AlertManager rules
groups:
  - name: resilience
    rules:
      # Alerta cuando Circuit Breaker está OPEN
      - alert: CircuitBreakerOpen
        expr: resilience4j_circuitbreaker_state == 1
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "Circuit Breaker {{ $labels.name }} is OPEN"
          description: "The circuit breaker {{ $labels.name }} has been open for more than 1 minute"

      # Alerta cuando failure rate es alto
      - alert: HighFailureRate
        expr: resilience4j_circuitbreaker_failure_rate > 0.5
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High failure rate on {{ $labels.name }}"
          description: "Failure rate is {{ $value }} on circuit breaker {{ $labels.name }}"

      # Alerta cuando cache hit rate es bajo
      - alert: LowCacheHitRate
        expr: cache_hit_ratio < 0.5
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "Low cache hit rate on {{ $labels.cache }}"
          description: "Cache hit rate is {{ $value }} on cache {{ $labels.cache }}"

      # Alerta cuando hay muchos requests rechazados por rate limit
      - alert: HighRateLimitRejections
        expr: rate(http_server_requests_total{status="429"}[5m]) > 10
        for: 1m
        labels:
          severity: warning
        annotations:
          summary: "High rate of 429 responses"
          description: "{{ $value }} requests/sec are being rate limited"
```

### Health Indicators

Spring Boot Actuator expone health indicators para Resilience4j:

```bash
# Verificar health de circuit breakers
curl http://localhost:8080/actuator/health

# Response:
{
  "status": "UP",
  "components": {
    "circuitBreakers": {
      "status": "UP",
      "details": {
        "external": {
          "status": "UP",
          "state": "CLOSED",
          "failureRate": "15.0%",
          "slowCallRate": "2.0%"
        },
        "database": {
          "status": "UP",
          "state": "CLOSED",
          "failureRate": "5.0%",
          "slowCallRate": "0.0%"
        }
      }
    },
    "rateLimiters": {
      "status": "UP",
      "details": {
        "public-api": {
          "status": "UP",
          "availablePermissions": 73
        }
      }
    }
  }
}
```

---

## Mejores Prácticas

### Caché

1. **Usar keys específicas**: Incluir entityName en las keys para evitar colisiones

```java
// ✅ BUENO
@Cacheable(key = "#root.target.entityName + ':' + #id")

// ❌ MALO
@Cacheable(key = "#id")  // Colisión entre User:1 y Product:1
```

2. **Evitar cachear fallos**

```java
@Cacheable(value = "entities", unless = "#result.isFailure()")
```

3. **Usar eviction selectivo**

```java
// ✅ BUENO
cacheEvictionService.evictListsByEntityName(getEntityName());

// ❌ MALO
@CacheEvict(value = "lists", allEntries = true)
```

4. **Configurar TTL apropiados**

```
Entities (frecuentemente accedidas): 10 minutos
Lists (pueden cambiar): 5 minutos
Counts (cambian constantemente): 2 minutos
```

5. **Monitorear métricas de caché**

```
Cache Hit Rate > 70% = Excelente
Cache Hit Rate 50-70% = Aceptable
Cache Hit Rate < 50% = Revisar estrategia
```

### Circuit Breaker

1. **Usar fallback methods**

```java
@CircuitBreaker(name = "external", fallbackMethod = "fallback")
public Result<Data, Exception> externalCall() {
    // ...
}

// Misma firma + Exception
private Result<Data, Exception> fallback(Exception e) {
    // Retornar datos cacheados o por defecto
}
```

2. **Configurar thresholds apropiados**

```
Servicios externos: failureRateThreshold = 50%
Base de datos: failureRateThreshold = 25% (más estricto)
Servicios críticos: failureRateThreshold = 10%
```

3. **Monitorear transiciones de estado**

```java
cb.getEventPublisher()
    .onStateTransition(event -> {
        // Enviar alerta cuando pase a OPEN
        if (event.getStateTransition().getToState() == State.OPEN) {
            alertService.sendAlert("Circuit Breaker OPEN: " + event.getCircuitBreakerName());
        }
    });
```

4. **Considerar slowCallRate**

```yaml
slowCallRateThreshold: 80
slowCallDurationThreshold: 2s
```

### Retry

1. **No reintentar errores del cliente**

```java
RetryConfig.custom()
    .ignoreExceptions(
        IllegalArgumentException.class,
        ResourceNotFoundException.class,
        BadRequestException.class
    )
```

2. **Usar backoff exponencial**

```yaml
enableExponentialBackoff: true
exponentialBackoffMultiplier: 2
```

3. **Limitar reintentos**

```yaml
maxAttempts: 3  # No más de 3-5 reintentos
```

4. **Combinar con Circuit Breaker**

```java
@Retry(name = "external")
@CircuitBreaker(name = "external", fallbackMethod = "fallback")
public Result<Data, Exception> externalCall() {
    // Retry se ejecuta primero
    // Si todos los reintentos fallan, Circuit Breaker registra el fallo
}
```

### Rate Limiting

1. **Diferentes límites por tipo de endpoint**

```yaml
# Endpoints públicos: más restrictivo
public-api:
  limitForPeriod: 10
  limitRefreshPeriod: 1s

# Endpoints autenticados: menos restrictivo
authenticated-api:
  limitForPeriod: 100
  limitRefreshPeriod: 1s
```

2. **Usar fail-fast**

```yaml
timeoutDuration: 0s  # No esperar, rechazar inmediatamente
```

3. **Agregar headers de rate limit**

```java
response.setHeader("X-RateLimit-Limit", String.valueOf(maxRequests));
response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));
response.setHeader("X-RateLimit-Reset", String.valueOf(resetTime));
```

4. **Excluir endpoints de salud**

```java
@Override
protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getRequestURI();
    return path.startsWith("/actuator/");
}
```

### Combinación de Patrones

```java
/**
 * Combina todos los patrones de resiliencia:
 * 1. Cache (rápido)
 * 2. Rate Limit (controla carga)
 * 3. Retry (maneja fallos transitorios)
 * 4. Circuit Breaker (previene cascada de fallos)
 */
@Cacheable(value = "entities", key = "#id", unless = "#result.isFailure()")
@RateLimiter(name = "default")
@Retry(name = "database")
@CircuitBreaker(name = "database", fallbackMethod = "fallback")
public Result<Entity, Exception> findById(Long id) {
    return repository.findById(id);
}
```

**Orden de ejecución:**
1. Cache (si existe, retorna inmediatamente)
2. Rate Limit (verifica que no exceda límite)
3. Retry (intenta hasta 3 veces)
4. Circuit Breaker (registra éxito/fallo)
5. Cache Put (guarda resultado exitoso)

---

## Resumen

### Arquitectura de Resiliencia

```
┌─────────────────────────────────────────────────┐
│                   Cliente                        │
└────────────────────┬────────────────────────────┘
                     │
                     ▼
          ┌──────────────────────┐
          │ RateLimitingFilter   │ ◄─── Limita requests por IP
          │  (100 req/min)       │
          └──────────┬───────────┘
                     │
                     ▼
          ┌──────────────────────┐
          │   Spring Security    │ ◄─── Autenticación/Autorización
          └──────────┬───────────┘
                     │
                     ▼
          ┌──────────────────────┐
          │    Controller        │
          └──────────┬───────────┘
                     │
                     ▼
          ┌──────────────────────┐
          │  @RateLimiter        │ ◄─── Rate limit a nivel método
          │  @Cacheable          │ ◄─── Verifica caché primero
          │  @Retry              │ ◄─── Reintentos automáticos
          │  @CircuitBreaker     │ ◄─── Protección contra fallos
          └──────────┬───────────┘
                     │
                     ▼
          ┌──────────────────────┐
          │      Service         │
          └──────────┬───────────┘
                     │
        ┌────────────┼────────────┐
        │            │            │
        ▼            ▼            ▼
   ┌────────┐  ┌─────────┐  ┌──────────┐
   │ Cache  │  │Database │  │External  │
   │(Caffeine)  │  (JPA)  │  │   API    │
   └────────┘  └─────────┘  └──────────┘
```

### Métricas Clave

| Métrica                  | Objetivo       | Crítico si    |
|--------------------------|----------------|---------------|
| Cache Hit Rate           | > 70%          | < 50%         |
| Circuit Breaker State    | CLOSED         | OPEN          |
| Failure Rate             | < 5%           | > 25%         |
| Slow Call Rate           | < 10%          | > 50%         |
| Rate Limit Rejections    | < 1%           | > 10%         |
| Retry Success Rate       | > 90%          | < 50%         |

### Próximos Pasos

1. Implementar monitoring con Grafana
2. Configurar alertas en AlertManager
3. Ajustar thresholds según métricas reales
4. Implementar fallbacks personalizados
5. Revisar logs de Circuit Breaker

---

## Referencias

- [Caffeine Documentation](https://github.com/ben-manes/caffeine)
- [Resilience4j Documentation](https://resilience4j.readme.io/)
- [Spring Cache Abstraction](https://docs.spring.io/spring-framework/reference/integration/cache.html)
- [Micrometer Documentation](https://micrometer.io/docs)
- [Circuit Breaker Pattern](https://martinfowler.com/bliki/CircuitBreaker.html)
