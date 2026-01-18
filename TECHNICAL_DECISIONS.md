# Decisiones Técnicas - APiGen

Este documento describe las decisiones técnicas tomadas en el desarrollo de APiGen, explicando el **por qué** detrás de cada elección arquitectónica y tecnológica.

## Tabla de Contenidos

1. [Stack Tecnológico](#1-stack-tecnológico)
2. [Arquitectura](#2-arquitectura)
3. [Patrones de Diseño](#3-patrones-de-diseño)
4. [Seguridad](#4-seguridad)
5. [Persistencia](#5-persistencia)
6. [Performance](#6-performance)
7. [Resiliencia](#7-resiliencia)
8. [Observabilidad](#8-observabilidad)
9. [Testing](#9-testing)
10. [DevOps](#10-devops)
11. [Generación de Código](#11-generación-de-código)

---

## 1. Stack Tecnológico

### 1.1 Java 25

**Decisión:** Usar Java 25 como versión del lenguaje.

**Alternativas consideradas:**
- Java 17 LTS
- Java 21 LTS
- Kotlin

**Razones:**

| Factor | Justificación |
|--------|---------------|
| **Virtual Threads** | Java 21+ incluye Project Loom con virtual threads, permitiendo manejar millones de conexiones concurrentes sin el overhead de threads del OS. Ideal para APIs con alta concurrencia. |
| **Pattern Matching** | Sintaxis más expresiva con `switch` expressions, `instanceof` patterns y record patterns, reduciendo boilerplate. |
| **Records** | Tipos inmutables ideales para DTOs, eventos y value objects con menos código. |
| **Sealed Classes** | Mejor modelado del dominio con jerarquías de tipos restringidas (usado en `Result<T,E>`). |
| **String Templates** | Interpolación de strings más segura y legible (preview). |
| **Ecosystem Maturity** | Amplio ecosistema de librerías, herramientas y desarrolladores disponibles. |

**Trade-offs:**
- (-) Algunas librerías pueden no soportar la última versión inmediatamente
- (-) JaCoCo aún no soporta Java 25 bytecode
- (+) Acceso a las últimas optimizaciones de rendimiento del JVM

---

### 1.2 Spring Boot 4.0

**Decisión:** Usar Spring Boot 4.0 como framework base.

**Alternativas consideradas:**
- Quarkus
- Micronaut
- Vert.x
- Jakarta EE puro

**Razones:**

| Factor | Justificación |
|--------|---------------|
| **Madurez** | 10+ años de desarrollo, documentación exhaustiva, comunidad masiva. |
| **Productividad** | Auto-configuración, starters, y convenciones reducen tiempo de setup. |
| **Ecosistema** | Integración nativa con Spring Security, Data, HATEOAS, Actuator, etc. |
| **Virtual Threads** | Soporte nativo para virtual threads en Spring Boot 3.2+. |
| **GraalVM Ready** | Compilación a native image para menor footprint y startup instantáneo. |
| **Observability** | Micrometer y OpenTelemetry integrados out-of-the-box. |
| **Hiring** | Spring es el framework Java más demandado, facilitando contratación. |

**Trade-offs:**
- (-) Mayor consumo de memoria que Quarkus/Micronaut en modo JVM
- (-) Startup más lento que frameworks compile-time (mitigado con virtual threads)
- (+) Menor curva de aprendizaje para equipos Java existentes

---

### 1.3 PostgreSQL 17

**Decisión:** Usar PostgreSQL como base de datos principal.

**Alternativas consideradas:**
- MySQL/MariaDB
- Oracle Database
- Microsoft SQL Server
- MongoDB

**Razones:**

| Factor | Justificación |
|--------|---------------|
| **ACID Compliance** | Transacciones robustas para integridad de datos críticos. |
| **JSON Support** | Tipo `jsonb` nativo para datos semi-estructurados sin necesidad de NoSQL. |
| **Full-Text Search** | Búsqueda de texto completo integrada sin Elasticsearch para casos simples. |
| **Extensibilidad** | Extensiones como PostGIS, pg_trgm, pgvector para casos especializados. |
| **Performance** | Excelente rendimiento con índices parciales, BRIN, y parallel queries. |
| **Open Source** | Sin costos de licencia, comunidad activa, múltiples proveedores cloud. |
| **Hibernate Support** | Soporte maduro en Hibernate con optimizaciones específicas. |

**Trade-offs:**
- (-) Más complejo de escalar horizontalmente que NoSQL
- (-) Requiere más tuning para cargas muy altas
- (+) Consistencia garantizada vs eventual consistency de NoSQL

---

### 1.4 Gradle (vs Maven)

**Decisión:** Usar Gradle como herramienta de build.

**Alternativas consideradas:**
- Maven
- Bazel

**Razones:**

| Factor | Justificación |
|--------|---------------|
| **Performance** | Build cache y ejecución incremental reducen tiempos de build significativamente. |
| **Flexibilidad** | DSL Groovy/Kotlin permite lógica personalizada sin plugins complejos. |
| **Multi-project** | Mejor soporte para monorepos y proyectos multi-módulo. |
| **Tareas Custom** | Fácil crear tareas como `generateEntity` con código Groovy inline. |
| **Spring Boot** | Excelente integración con `bootJar`, `bootRun`, y dependencias BOM. |

**Trade-offs:**
- (-) Curva de aprendizaje mayor que Maven
- (-) Builds pueden ser más difíciles de depurar
- (+) Builds más rápidos en proyectos grandes

---

## 2. Arquitectura

### 2.1 Domain-Driven Design (DDD)

**Decisión:** Estructurar el código siguiendo principios de DDD con capas bien definidas.

**Alternativas consideradas:**
- Arquitectura en capas tradicional (Controller → Service → Repository)
- Hexagonal Architecture (Ports & Adapters)
- Clean Architecture

**Razones:**

| Factor | Justificación |
|--------|---------------|
| **Separación de Concerns** | Cada capa tiene responsabilidad única: dominio, aplicación, infraestructura. |
| **Domain Isolation** | La lógica de negocio está aislada de frameworks y detalles técnicos. |
| **Testabilidad** | Dominio testeable sin dependencias de infraestructura. |
| **Evolución** | Cambiar base de datos o framework no afecta la lógica de negocio. |
| **Ubiquitous Language** | Código refleja el lenguaje del negocio. |

**Estructura elegida:**

```
{module}/
├── domain/           # Entidades, eventos, excepciones de negocio
│   ├── entity/       # Agregados y entidades
│   ├── event/        # Eventos de dominio
│   └── exception/    # Excepciones de negocio
├── application/      # Casos de uso, DTOs, mappers
│   ├── dto/          # Objetos de transferencia
│   ├── mapper/       # Conversiones entity ↔ DTO
│   └── service/      # Lógica de aplicación
└── infrastructure/   # Implementaciones técnicas
    ├── controller/   # REST endpoints
    ├── repository/   # Acceso a datos
    └── config/       # Configuración Spring
```

**Trade-offs:**
- (-) Más archivos y directorios que arquitectura simple
- (-) Puede ser overkill para CRUDs simples
- (+) Escala bien para dominios complejos
- (+) Facilita trabajo en equipos grandes

#### Nota: Ubicación de Repositorios

En DDD estricto, las **interfaces** de repositorio van en `domain/` (contrato) y las **implementaciones** en `infrastructure/`. Este proyecto usa un enfoque **pragmático**:

| Ubicación | Razón |
|-----------|-------|
| `infrastructure/repository/` | Las interfaces extienden `JpaRepository` (dependencia de Spring) |

**Justificación:**
1. Spring Data genera la implementación automáticamente
2. No hay implementación manual que separar
3. Cambiar de JPA es improbable
4. Reduce complejidad sin perder testabilidad

**Para DDD puro**, se puede crear:
- Interface pura en `domain/repository/` sin dependencias
- Adapter en `infrastructure/persistence/` que use Spring Data

---

### 2.2 Clases Base Genéricas

**Decisión:** Crear `Base`, `BaseService`, `BaseController` genéricos para heredar.

**Alternativas consideradas:**
- Generar código repetitivo para cada entidad
- Usar Spring Data REST automático
- Implementar cada CRUD manualmente

**Razones:**

| Factor | Justificación |
|--------|---------------|
| **DRY** | Elimina duplicación de código CRUD en todas las entidades. |
| **Consistencia** | Todas las entidades tienen el mismo comportamiento base. |
| **Mantenibilidad** | Cambios en comportamiento CRUD se aplican a todas las entidades. |
| **Extensibilidad** | Fácil override de métodos específicos cuando se necesita. |
| **Auditoría Uniforme** | Campos de auditoría consistentes en todas las entidades. |

**Implementación:**

```java
// Una clase, comportamiento para todas las entidades
public abstract class BaseServiceImpl<E extends Base, ID> {
    // CRUD genérico + caching + eventos + auditoría
}

// Entidades específicas solo agregan lógica de negocio
public class ProductServiceImpl extends BaseServiceImpl<Product, Long> {
    // Solo métodos específicos de Product
}
```

**Trade-offs:**
- (-) Abstracción puede ocultar comportamiento
- (-) Debugging más complejo con herencia
- (+) Drástica reducción de código boilerplate

---

### 2.3 Interfaces + Implementación para Controllers/Services

**Decisión:** Separar interfaces de implementaciones (`ProductController` interface + `ProductControllerImpl`).

**Alternativas consideradas:**
- Solo clases concretas
- Clases abstractas

**Razones:**

| Factor | Justificación |
|--------|---------------|
| **OpenAPI** | Anotaciones `@Tag`, `@Operation` en interface mantienen impl limpia. |
| **Contract First** | Interface define el contrato público de la API. |
| **Testability** | Fácil mockear interfaces en tests. |
| **Multiple Impls** | Permite diferentes implementaciones (mock, real, cached). |
| **Documentation** | Javadoc en interface, implementación en clase. |

**Trade-offs:**
- (-) Más archivos por entidad
- (-) Navegación de código más compleja
- (+) Separación clara de contrato vs implementación

---

## 3. Patrones de Diseño

### 3.1 Result<T, E> Pattern

**Decisión:** Usar tipo `Result<T, E>` para manejo de errores en lugar de excepciones.

**Alternativas consideradas:**
- Excepciones checked/unchecked tradicionales
- Optional<T>
- Either<L, R> de Vavr
- Null returns

**Razones:**

| Factor | Justificación |
|--------|---------------|
| **Explicit Errors** | El tipo de retorno indica que la operación puede fallar. |
| **Composability** | `map()`, `flatMap()`, `fold()` permiten encadenar operaciones. |
| **No Hidden Flow** | Excepciones crean flujo de control oculto; Result es explícito. |
| **Forced Handling** | El compilador obliga a manejar ambos casos (success/failure). |
| **No Stacktrace Cost** | Crear Result es O(1) vs crear Exception con stacktrace. |

**Implementación:**

```java
public sealed interface Result<T, E> {
    record Success<T, E>(T value) implements Result<T, E> {}
    record Failure<T, E>(E error) implements Result<T, E> {}

    // Railway-oriented programming
    <U> Result<U, E> map(Function<T, U> mapper);
    <U> Result<U, E> flatMap(Function<T, Result<U, E>> mapper);
    T fold(Function<T, T> onSuccess, Function<E, T> onFailure);
}
```

**Uso:**

```java
// Composición funcional sin try-catch
return findById(id)
    .flatMap(this::validateStock)
    .map(this::applyDiscount)
    .flatMap(this::save);
```

**Trade-offs:**
- (-) Curva de aprendizaje para desarrolladores no funcionales
- (-) Más verboso para operaciones simples
- (+) Código más predecible y testeable

---

### 3.2 Soft Delete

**Decisión:** Implementar eliminación lógica (soft delete) por defecto.

**Alternativas consideradas:**
- Hard delete (eliminación física)
- Tabla de histórico separada
- Event sourcing

**Razones:**

| Factor | Justificación |
|--------|---------------|
| **Recuperación** | Datos eliminados pueden restaurarse fácilmente. |
| **Auditoría** | Historial completo de quién eliminó qué y cuándo. |
| **Integridad Referencial** | No rompe foreign keys de registros relacionados. |
| **Compliance** | Algunas regulaciones requieren retención de datos. |
| **Debugging** | Facilita investigar problemas con datos "eliminados". |

**Implementación:**

```java
@MappedSuperclass
public abstract class Base {
    private Boolean estado = true;           // false = eliminado
    private LocalDateTime fechaEliminacion;
    private String eliminadoPor;
}

// Filtro automático en queries
@SQLRestriction("estado = true")
```

**Trade-offs:**
- (-) Tablas crecen indefinidamente
- (-) Queries deben filtrar por estado
- (-) Índices más grandes
- (+) Mitigado con `@SQLRestriction` automático
- (+) Archivado periódico a tablas históricas

---

### 3.3 Domain Events

**Decisión:** Publicar eventos de dominio en operaciones CRUD.

**Alternativas consideradas:**
- Callbacks en servicios
- Triggers de base de datos
- No notificar cambios

**Razones:**

| Factor | Justificación |
|--------|---------------|
| **Desacoplamiento** | Productores y consumidores de eventos no se conocen. |
| **Extensibilidad** | Agregar comportamiento sin modificar código existente. |
| **Async Processing** | Eventos pueden procesarse asincrónicamente. |
| **Audit Trail** | Eventos pueden persistirse para auditoría. |
| **CQRS Ready** | Base para separar comandos y queries si se necesita. |

**Implementación:**

```java
// Entidad registra eventos
public abstract class Base {
    @Transient
    private final List<Object> domainEvents = new ArrayList<>();

    protected void registerEvent(Object event) {
        domainEvents.add(event);
    }
}

// Spring publica automáticamente
@DomainEvents
Collection<Object> domainEvents() { return domainEvents; }
```

**Eventos publicados:**

```java
EntityCreatedEvent<E>     // Después de save() en nueva entidad
EntityUpdatedEvent<E>     // Después de save() en entidad existente
EntityDeletedEvent<E>     // Después de softDelete()
EntityRestoredEvent<E>    // Después de restore()
```

**Trade-offs:**
- (-) Complejidad adicional
- (-) Debugging de flujos asincrónicos
- (+) Sistema más flexible y extensible

---

## 4. Seguridad

### 4.1 JWT para Autenticación

**Decisión:** Usar JSON Web Tokens (JWT) para autenticación stateless.

**Alternativas consideradas:**
- Sesiones con cookies
- OAuth2 con servidor externo
- API Keys
- Basic Auth

**Razones:**

| Factor | Justificación |
|--------|---------------|
| **Stateless** | No requiere almacenar sesión en servidor; escala horizontalmente. |
| **Self-contained** | Token contiene claims (userId, roles) sin consultar DB. |
| **Cross-domain** | Funciona con CORS para SPAs en diferentes dominios. |
| **Mobile Friendly** | Fácil de usar en apps móviles y clientes no-browser. |
| **Standard** | RFC 7519, amplio soporte en librerías y servicios. |

**Implementación:**

```
Access Token:  15 minutos, contiene claims completos
Refresh Token: 7 días, solo para renovar access token
Blacklist:     Tokens revocados almacenados en DB
```

**Trade-offs:**
- (-) Tokens no revocables instantáneamente (mitigado con blacklist)
- (-) Payload aumenta tamaño de requests
- (+) Eliminación de estado en servidor

---

### 4.2 Refresh Tokens + Blacklist

**Decisión:** Implementar refresh tokens con blacklist para revocación.

**Alternativas consideradas:**
- Solo access tokens de larga duración
- Sesiones server-side
- Token rotation sin blacklist

**Razones:**

| Factor | Justificación |
|--------|---------------|
| **Security** | Access tokens cortos limitan ventana de ataque si se comprometen. |
| **UX** | Refresh transparente evita re-login frecuente. |
| **Revocation** | Blacklist permite invalidar tokens en logout o compromiso. |
| **Audit** | Blacklist registra logouts y revocaciones. |

**Flujo:**

```
1. Login → Access (15min) + Refresh (7d)
2. Access expira → Cliente usa Refresh para obtener nuevo Access
3. Logout → Ambos tokens van a blacklist
4. Refresh expira → Usuario debe re-autenticar
```

**Trade-offs:**
- (-) Consulta a blacklist en cada request (mitigado con caché)
- (-) Complejidad adicional vs solo access tokens
- (+) Balance entre seguridad y UX

---

### 4.3 Rate Limiting en Autenticación

**Decisión:** Implementar rate limiting específico para endpoints de auth.

**Alternativas consideradas:**
- Rate limiting global únicamente
- CAPTCHA
- Sin rate limiting

**Razones:**

| Factor | Justificación |
|--------|---------------|
| **Brute Force** | Previene ataques de fuerza bruta contra contraseñas. |
| **Credential Stuffing** | Limita intentos de credenciales robadas de otros sitios. |
| **Resource Protection** | Login es operación costosa (bcrypt hash comparison). |
| **Targeted** | Límites más estrictos que endpoints normales. |

**Configuración:**

```yaml
auth-rate-limit:
  max-attempts: 5        # Máximo 5 intentos fallidos
  lockout-minutes: 15    # Bloqueo de 15 minutos
```

**Trade-offs:**
- (-) Puede bloquear usuarios legítimos que olvidan contraseña
- (-) Requiere tracking por IP/usuario
- (+) Protección esencial contra ataques automatizados

---

## 5. Persistencia

### 5.1 Hibernate Envers para Auditoría

**Decisión:** Usar Hibernate Envers para versionar entidades automáticamente.

**Alternativas consideradas:**
- Triggers de base de datos
- Event listeners manuales
- Tabla de cambios custom
- Solución externa (Javers, Audit4j)

**Razones:**

| Factor | Justificación |
|--------|---------------|
| **Automatic** | Solo `@Audited` en entidad, sin código adicional. |
| **Complete History** | Snapshot completo de entidad en cada cambio. |
| **Query Support** | API para consultar estado de entidad en cualquier punto del tiempo. |
| **Hibernate Integration** | Funciona con lazy loading, proxies, y transacciones existentes. |
| **Revision Info** | Metadata de quién y cuándo modificó. |

**Implementación:**

```java
@Entity
@Audited  // Solo esto necesario
public class Product extends Base { }

// Tablas generadas automáticamente
products_aud    // Historial de cambios
revision_info   // Metadata de revisiones
```

**Queries de auditoría:**

```java
AuditReader reader = AuditReaderFactory.get(entityManager);
Product productAtRevision = reader.find(Product.class, id, revisionNumber);
List<Number> revisions = reader.getRevisions(Product.class, id);
```

**Trade-offs:**
- (-) Duplica almacenamiento (cada versión guardada)
- (-) Puede impactar performance en writes intensivos
- (+) Auditoría completa sin esfuerzo de desarrollo

---

### 5.2 Flyway para Migraciones

**Decisión:** Usar Flyway para versionado de esquema de base de datos.

**Alternativas consideradas:**
- Liquibase
- Hibernate `ddl-auto=update`
- Scripts manuales
- Sin migraciones (schema fijo)

**Razones:**

| Factor | Justificación |
|--------|---------------|
| **Version Control** | Cambios de schema versionados junto con código. |
| **Repeatability** | Mismo resultado en dev, staging, producción. |
| **Rollback Support** | Migraciones pueden tener undo scripts. |
| **Team Collaboration** | Múltiples desarrolladores pueden agregar migraciones. |
| **Simplicity** | SQL plano, sin DSL adicional que aprender. |

**Convención:**

```
V1__initial_schema.sql       # Versión, descripción
V2__create_products.sql      # Incremental
V3__add_product_category.sql # Descriptivo
```

**Trade-offs:**
- (-) Migraciones son append-only (no editar existentes)
- (-) SQL específico de PostgreSQL (no portable)
- (+) Control total sobre DDL generado

---

### 5.3 Optimistic Locking con @Version

**Decisión:** Usar versionado optimista para control de concurrencia.

**Alternativas consideradas:**
- Pessimistic locking (SELECT FOR UPDATE)
- Last-write-wins (sin control)
- Timestamp-based

**Razones:**

| Factor | Justificación |
|--------|---------------|
| **Scalability** | No bloquea filas, mejor para alta concurrencia. |
| **Conflict Detection** | Detecta actualizaciones conflictivas automáticamente. |
| **API Friendly** | Funciona con HTTP ETag/If-Match pattern. |
| **Simple** | Solo agregar `@Version` a entidad. |

**Implementación:**

```java
public abstract class Base {
    @Version
    private Long version;  // Incrementa automáticamente
}
```

**Flujo HTTP:**

```
GET /products/1
→ ETag: "5"

PUT /products/1
If-Match: "5"
→ 200 OK (si version == 5)
→ 412 Precondition Failed (si version != 5)
```

**Trade-offs:**
- (-) Conflictos requieren retry del cliente
- (-) No previene conflictos, solo los detecta
- (+) Sin deadlocks ni contención de locks

---

## 6. Performance

### 6.1 Caffeine para Caché

**Decisión:** Usar Caffeine como proveedor de caché en memoria.

**Alternativas consideradas:**
- Redis/Memcached (caché distribuido)
- EhCache
- Guava Cache
- Spring Cache simple (ConcurrentHashMap)

**Razones:**

| Factor | Justificación |
|--------|---------------|
| **Performance** | Near-optimal hit rate con algoritmo W-TinyLFU. |
| **Memory Efficiency** | Mejor uso de memoria que LRU tradicional. |
| **Async Loading** | Refresh asíncrono sin bloquear reads. |
| **Statistics** | Métricas detalladas de hits/misses/evictions. |
| **Spring Integration** | Soporte nativo en Spring Cache abstraction. |

**Estrategia de tres niveles:**

```yaml
cache:
  entities:                    # Entidades individuales
    max-size: 1000
    expire-after-write: 10m
  lists:                       # Listas paginadas
    max-size: 100
    expire-after-write: 5m
  counts:                      # Contadores
    max-size: 50
    expire-after-write: 2m
```

**Trade-offs:**
- (-) No distribuido (cada instancia tiene su caché)
- (-) Cold start después de restart
- (+) Latencia sub-millisegundo
- (+) Sin infraestructura adicional

---

### 6.2 Virtual Threads

**Decisión:** Habilitar virtual threads de Java 21+ para concurrencia.

**Alternativas consideradas:**
- Thread pools tradicionales
- Reactive (WebFlux, Project Reactor)
- Coroutines (Kotlin)

**Razones:**

| Factor | Justificación |
|--------|---------------|
| **Scalability** | Millones de virtual threads vs miles de OS threads. |
| **Simplicity** | Código imperativo normal, sin callbacks/reactive. |
| **Blocking OK** | I/O bloqueante no desperdicia OS threads. |
| **Debugging** | Stack traces normales, herramientas existentes funcionan. |
| **Migration** | Código existente funciona sin cambios. |

**Configuración:**

```yaml
spring:
  threads:
    virtual:
      enabled: true
```

**Trade-offs:**
- (-) Thread-locals pueden tener comportamiento inesperado
- (-) Synchronized blocks pueden pinear carrier threads
- (+) Performance de reactive con simplicidad de blocking

---

### 6.3 ETags para Caché HTTP

**Decisión:** Implementar ETags para caché condicional en cliente.

**Alternativas consideradas:**
- Last-Modified headers
- Cache-Control únicamente
- Sin caché HTTP

**Razones:**

| Factor | Justificación |
|--------|---------------|
| **Bandwidth** | 304 Not Modified evita transferir datos sin cambios. |
| **Precision** | ETag basado en contenido, no en timestamp. |
| **Concurrency** | If-Match permite optimistic locking en HTTP. |
| **Standard** | RFC 7232, soportado por todos los clientes HTTP. |

**Implementación:**

```java
// ETag generado del hash del contenido + versión
String etag = "\"" + entity.hashCode() + "-" + entity.getVersion() + "\"";

// Request condicional
If-None-Match: "12345-3"
→ 304 Not Modified (datos no cambiaron)
→ 200 OK + nuevo ETag (datos cambiaron)
```

**Trade-offs:**
- (-) Overhead de calcular hash en cada response
- (-) Complejidad adicional en controller
- (+) Reducción significativa de tráfico

---

## 7. Resiliencia

### 7.1 Circuit Breaker (Resilience4j)

**Decisión:** Implementar circuit breaker para llamadas a servicios externos.

**Alternativas consideradas:**
- Hystrix (deprecated)
- Sentinel
- Sin circuit breaker

**Razones:**

| Factor | Justificación |
|--------|---------------|
| **Fail Fast** | Evita esperar timeouts cuando servicio está caído. |
| **Cascade Prevention** | Un servicio fallando no tumba toda la aplicación. |
| **Self-Healing** | Recuperación automática cuando servicio vuelve. |
| **Metrics** | Visibilidad del estado de dependencias. |

**Estados:**

```
CLOSED  → Operación normal, contando fallos
         ↓ (failure threshold exceeded)
OPEN    → Requests fallan inmediatamente
         ↓ (wait duration elapsed)
HALF_OPEN → Prueba si servicio se recuperó
         ↓ success → CLOSED
         ↓ failure → OPEN
```

**Configuración:**

```yaml
resilience4j:
  circuitbreaker:
    instances:
      default:
        sliding-window-size: 100
        failure-rate-threshold: 50      # 50% fallos → abre
        slow-call-rate-threshold: 80    # 80% lentas → abre
        slow-call-duration-threshold: 2s
        wait-duration-in-open-state: 60s
```

**Trade-offs:**
- (-) Complejidad de configuración de thresholds
- (-) Puede abrir innecesariamente si thresholds mal calibrados
- (+) Protección esencial para sistemas distribuidos

---

### 7.2 Retry con Exponential Backoff

**Decisión:** Implementar reintentos automáticos con backoff exponencial.

**Alternativas consideradas:**
- Retry lineal
- Retry fijo
- Sin reintentos

**Razones:**

| Factor | Justificación |
|--------|---------------|
| **Transient Failures** | Muchos errores se resuelven solos (network glitch). |
| **Backpressure** | Backoff evita sobrecargar servicio en recovery. |
| **Jitter** | Randomización evita thundering herd. |

**Configuración:**

```yaml
resilience4j:
  retry:
    instances:
      default:
        max-attempts: 3
        wait-duration: 500ms
        exponential-backoff-multiplier: 2
        # Intento 1: inmediato
        # Intento 2: 500ms después
        # Intento 3: 1000ms después
```

**Trade-offs:**
- (-) Aumenta latencia total en caso de fallos
- (-) Operaciones no idempotentes pueden causar duplicados
- (+) Mejora significativa en success rate

---

### 7.3 Rate Limiting

**Decisión:** Implementar rate limiting para proteger recursos.

**Alternativas consideradas:**
- API Gateway rate limiting únicamente
- Sin rate limiting
- Rate limiting por usuario en aplicación

**Razones:**

| Factor | Justificación |
|--------|---------------|
| **DoS Protection** | Limita impacto de ataques de denegación de servicio. |
| **Fair Usage** | Evita que un cliente acapare recursos. |
| **Cost Control** | Previene uso excesivo de APIs de pago downstream. |
| **Stability** | Protege servicios de picos inesperados. |

**Configuración:**

```yaml
resilience4j:
  ratelimiter:
    instances:
      default:
        limit-for-period: 100      # 100 requests
        limit-refresh-period: 1s   # por segundo
      strict:
        limit-for-period: 10
        limit-refresh-period: 1s
```

**Trade-offs:**
- (-) Puede rechazar requests legítimos en picos
- (-) Configuración por-instancia (no distribuida)
- (+) Protección básica sin infraestructura adicional

---

## 8. Observabilidad

### 8.1 OpenTelemetry para Tracing

**Decisión:** Usar OpenTelemetry como estándar de observabilidad.

**Alternativas consideradas:**
- Jaeger SDK directo
- Zipkin
- AWS X-Ray
- Datadog APM

**Razones:**

| Factor | Justificación |
|--------|---------------|
| **Vendor Neutral** | Exporta a cualquier backend (Jaeger, Zipkin, Datadog, etc). |
| **Standard** | CNCF graduated project, industria convergiendo en OTel. |
| **Auto-instrumentation** | Tracing automático de Spring, JDBC, HTTP clients. |
| **Unified** | Traces, metrics, y logs con mismo SDK. |
| **Future Proof** | Cambiar backend sin cambiar código. |

**Configuración:**

```yaml
management:
  tracing:
    sampling:
      probability: 1.0  # 100% en dev
otel:
  exporter:
    otlp:
      endpoint: http://localhost:4318/v1/traces
```

**Trade-offs:**
- (-) SDK más pesado que soluciones específicas
- (-) Más configuración inicial
- (+) Flexibilidad total de backends

---

### 8.2 Structured Logging (JSON)

**Decisión:** Usar logging estructurado en formato JSON para producción.

**Alternativas consideradas:**
- Logs de texto plano
- Log4j2 JSON
- Formato personalizado

**Razones:**

| Factor | Justificación |
|--------|---------------|
| **Parseable** | Logs JSON fácilmente ingestados por ELK, Splunk, etc. |
| **Searchable** | Campos individuales indexables y buscables. |
| **Context** | Request ID, user ID, trace ID incluidos estructuradamente. |
| **Aggregatable** | Métricas derivables de logs (error counts, latencies). |

**Implementación (Logstash Encoder):**

```xml
<encoder class="net.logstash.logback.encoder.LogstashEncoder">
    <includeMdcKeyName>requestId</includeMdcKeyName>
    <includeMdcKeyName>userId</includeMdcKeyName>
</encoder>
```

**Output:**

```json
{
  "timestamp": "2024-01-15T10:30:00.000Z",
  "level": "INFO",
  "logger": "c.j.apigen.ProductService",
  "message": "Product created",
  "requestId": "abc-123",
  "userId": 42,
  "productId": 100,
  "duration_ms": 45
}
```

**Trade-offs:**
- (-) Logs menos legibles para humanos
- (-) Mayor tamaño de logs
- (+) Análisis y alerting automatizado

---

### 8.3 Actuator Endpoints

**Decisión:** Exponer endpoints de Actuator para health, metrics, info.

**Alternativas consideradas:**
- Custom health endpoints
- Solo logs para monitoreo
- Agente externo (Datadog agent, etc)

**Razones:**

| Factor | Justificación |
|--------|---------------|
| **Kubernetes Ready** | Liveness/readiness probes out-of-the-box. |
| **Prometheus Integration** | Métricas en formato Prometheus nativo. |
| **Runtime Info** | Beans, env, conditions, mappings para debugging. |
| **Cache Stats** | Visibilidad de hit rates y evictions. |

**Endpoints habilitados:**

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus,caches
```

**Trade-offs:**
- (-) Exposición potencial de información sensible
- (-) Overhead de recolección de métricas
- (+) Observabilidad completa sin código adicional

---

## 9. Testing

### 9.1 TestContainers para Integration Tests

**Decisión:** Usar TestContainers para tests de integración con PostgreSQL real.

**Alternativas consideradas:**
- H2 in-memory database
- Base de datos de test compartida
- Mocks de repositorios

**Razones:**

| Factor | Justificación |
|--------|---------------|
| **Realistic** | Mismo PostgreSQL que producción, mismos comportamientos. |
| **Isolated** | Cada test run tiene container fresco. |
| **Queries Tested** | SQL nativo y features PostgreSQL-específicos testeados. |
| **No Mocking** | Tests de integración reales, no simulados. |

**Implementación:**

```java
@Testcontainers
class IntegrationTest {
    @Container
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:17-alpine");
}
```

**Trade-offs:**
- (-) Tests más lentos (startup de container)
- (-) Requiere Docker instalado
- (+) Confianza real en que código funciona

---

### 9.2 ArchUnit para Tests de Arquitectura

**Decisión:** Usar ArchUnit para validar reglas arquitectónicas.

**Alternativas consideradas:**
- Code reviews manuales
- Linters/static analysis
- Sin validación de arquitectura

**Razones:**

| Factor | Justificación |
|--------|---------------|
| **Automated** | Reglas verificadas en cada build. |
| **Documented** | Tests son documentación ejecutable de arquitectura. |
| **Prevents Drift** | Detecta violaciones antes de merge. |
| **Custom Rules** | Reglas específicas del proyecto. |

**Reglas implementadas:**

```java
@ArchTest
static final ArchRule domainShouldNotDependOnInfrastructure =
    noClasses()
        .that().resideInAPackage("..domain..")
        .should().dependOnClassesThat()
        .resideInAPackage("..infrastructure..");

@ArchTest
static final ArchRule servicesShouldBeAnnotatedWithService =
    classes()
        .that().resideInAPackage("..application.service..")
        .and().haveSimpleNameEndingWith("ServiceImpl")
        .should().beAnnotatedWith(Service.class);
```

**Trade-offs:**
- (-) Configuración inicial de reglas
- (-) Puede ser demasiado estricto
- (+) Mantiene integridad de arquitectura a largo plazo

---

## 10. DevOps

### 10.1 Multi-stage Dockerfile

**Decisión:** Usar Dockerfile multi-stage con extracción de layers.

**Alternativas consideradas:**
- Single stage Dockerfile
- Jib (Google container builder)
- Buildpacks

**Razones:**

| Factor | Justificación |
|--------|---------------|
| **Image Size** | Solo JRE en imagen final, no JDK ni Gradle. |
| **Layer Caching** | Dependencias en layer separado, rebuild más rápido. |
| **Security** | Imagen final mínima, menor superficie de ataque. |
| **Reproducibility** | Build completo en container, no depende de host. |

**Estructura:**

```dockerfile
# Stage 1: Build
FROM eclipse-temurin:25-jdk-alpine AS builder
COPY . .
RUN ./gradlew bootJar
RUN java -Djarmode=layertools -jar app.jar extract

# Stage 2: Runtime
FROM eclipse-temurin:25-jre-alpine
COPY --from=builder dependencies/ ./
COPY --from=builder spring-boot-loader/ ./
COPY --from=builder snapshot-dependencies/ ./
COPY --from=builder application/ ./
```

**Trade-offs:**
- (-) Dockerfile más complejo
- (-) Build más lento que Jib
- (+) Control total sobre imagen final

---

### 10.2 Docker Compose para Desarrollo

**Decisión:** Proveer docker-compose.yml con stack completo.

**Alternativas consideradas:**
- Solo documentación de setup manual
- DevContainers
- Kubernetes local (minikube)

**Razones:**

| Factor | Justificación |
|--------|---------------|
| **One Command** | `docker-compose up` inicia todo el stack. |
| **Consistent** | Mismo ambiente para todos los desarrolladores. |
| **Complete** | DB, app, monitoring, todo incluido. |
| **Portable** | Funciona en Windows, Mac, Linux. |

**Servicios incluidos:**

```yaml
services:
  apigen:      # Aplicación
  postgres:    # Base de datos
  pgadmin:     # UI de DB
  prometheus:  # Métricas
  grafana:     # Dashboards
```

**Trade-offs:**
- (-) Requiere Docker Desktop (licencia para empresas grandes)
- (-) Consumo de recursos en máquina local
- (+) Setup instantáneo para nuevos desarrolladores

---

## 11. Generación de Código

### 11.1 OpenAPI Generator para SDKs

**Decisión:** Generar SDKs de cliente desde especificación OpenAPI.

**Alternativas consideradas:**
- SDKs escritos manualmente
- GraphQL con codegen
- gRPC con protobuf
- Sin SDKs (clientes usan HTTP directo)

**Razones:**

| Factor | Justificación |
|--------|---------------|
| **Single Source** | API define contrato, SDKs derivados automáticamente. |
| **Type Safety** | Clientes tipados en Java, TypeScript, Python. |
| **Always Current** | Regenerar SDKs en cada cambio de API. |
| **Multi-language** | 50+ lenguajes soportados por OpenAPI Generator. |

**Lenguajes elegidos:**

| SDK | Razón |
|-----|-------|
| Java | Servicios backend, Android |
| TypeScript | SPAs (React, Angular, Vue) |
| Python | Data science, scripting, ML pipelines |

**Trade-offs:**
- (-) Código generado puede ser verboso
- (-) Customización limitada sin templates propios
- (+) Drástica reducción de trabajo manual

---

### 11.2 Generador de Entidades Custom

**Decisión:** Crear generador propio para scaffolding de entidades.

**Alternativas consideradas:**
- JHipster
- Spring Initializr customizado
- Copiar/pegar template manualmente

**Razones:**

| Factor | Justificación |
|--------|---------------|
| **Project-specific** | Genera código que sigue patrones exactos del proyecto. |
| **Simple** | Un comando genera 10 archivos correctamente integrados. |
| **Maintainable** | Templates en Gradle/PowerShell, fácil de modificar. |
| **No Dependencies** | No requiere herramientas externas. |

**Output:**

```
./gradlew generateEntity -Pname=Product -Pmodule=products

Genera:
- Entity, DTO, Mapper
- Repository, Service, ServiceImpl
- Controller, ControllerImpl
- Migration SQL
- Unit Test
```

**Trade-offs:**
- (-) Mantenimiento de templates propios
- (-) Menos features que JHipster
- (+) Control total y simplicidad

---

## Resumen de Decisiones

| Área | Decisión | Razón Principal |
|------|----------|-----------------|
| **Lenguaje** | Java 25 | Virtual threads, pattern matching |
| **Framework** | Spring Boot 4 | Madurez, ecosistema, hiring |
| **Database** | PostgreSQL 17 | ACID, JSON, extensibilidad |
| **Build** | Gradle | Performance, flexibilidad |
| **Arquitectura** | DDD | Separación, testabilidad, evolución |
| **Errors** | Result<T,E> | Explícito, composable, sin stacktrace |
| **Delete** | Soft delete | Recuperación, auditoría, compliance |
| **Auth** | JWT + Refresh | Stateless, mobile-friendly |
| **Audit** | Hibernate Envers | Automático, queries temporales |
| **Cache** | Caffeine | Performance, sin infraestructura |
| **Resilience** | Resilience4j | Circuit breaker, retry, rate limit |
| **Observability** | OpenTelemetry | Vendor-neutral, standard |
| **Testing** | TestContainers | Tests realistas con DB real |
| **Container** | Multi-stage Docker | Imagen pequeña, layers cached |
| **SDKs** | OpenAPI Generator | Multi-language, siempre actual |

---

## Referencias

- [Java 25 Release Notes](https://openjdk.org/projects/jdk/25/)
- [Spring Boot Reference](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [Resilience4j Documentation](https://resilience4j.readme.io/)
- [OpenTelemetry Java](https://opentelemetry.io/docs/java/)
- [Domain-Driven Design Reference](https://www.domainlanguage.com/ddd/reference/)
- [OpenAPI Specification](https://spec.openapis.org/oas/latest.html)
