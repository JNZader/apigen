# Guía Didáctica: Arquitectura DDD en APiGen

## Tabla de Contenidos

1. [¿Qué es DDD y por qué usarlo?](#1-qué-es-ddd-y-por-qué-usarlo)
2. [Capas de la arquitectura](#2-capas-de-la-arquitectura)
3. [Estructura de carpetas del proyecto](#3-estructura-de-carpetas-del-proyecto)
4. [Domain Layer en detalle](#4-domain-layer-en-detalle)
5. [Application Layer en detalle](#5-application-layer-en-detalle)
6. [Infrastructure Layer en detalle](#6-infrastructure-layer-en-detalle)
7. [Flujo de una petición HTTP](#7-flujo-de-una-petición-http)
8. [Reglas de dependencias](#8-reglas-de-dependencias)

---

## 1. ¿Qué es DDD y por qué usarlo?

### 1.1 Conceptos Básicos

**Domain-Driven Design (DDD)** es un enfoque de desarrollo de software que pone el **dominio del negocio** en el centro del diseño. Fue introducido por Eric Evans en su libro "Domain-Driven Design: Tackling Complexity in the Heart of Software".

**Principios fundamentales:**

- **Ubicuidad del lenguaje**: Usar el mismo vocabulario entre desarrolladores y expertos del negocio
- **Separación por capas**: Aislar la lógica de negocio de preocupaciones técnicas
- **Modelo rico**: Las entidades contienen comportamiento, no solo datos
- **Contextos acotados**: Dividir el sistema en módulos con límites claros

### 1.2 Beneficios en APIs Empresariales

**¿Por qué DDD es ideal para APIs REST empresariales?**

| Beneficio | Descripción |
|-----------|-------------|
| **Mantenibilidad** | Código organizado que refleja el dominio del negocio |
| **Escalabilidad** | Fácil agregar nuevos módulos sin afectar existentes |
| **Testeabilidad** | Lógica de negocio independiente de infraestructura |
| **Flexibilidad** | Cambiar tecnologías sin reescribir el dominio |
| **Comprensibilidad** | Nuevos desarrolladores entienden rápido la estructura |

**Ejemplo del proyecto APiGen:**
```java
// ✅ CORRECTO: Lógica de negocio en el dominio
public class User extends Base {
    public boolean hasPermission(String permissionName) {
        return role != null && role.hasPermission(permissionName);
    }
}

// ❌ INCORRECTO: Lógica de negocio en el controlador
@RestController
public class UserController {
    public boolean checkPermission(User user, String permission) {
        // ¡La lógica de negocio NO va aquí!
    }
}
```

### 1.3 ¿Cuándo aplicar DDD?

**DDD es ideal cuando:**
- La lógica de negocio es compleja
- El proyecto es a largo plazo (> 6 meses)
- Hay múltiples módulos interrelacionados
- El equipo necesita comunicación clara con el negocio

**DDD podría ser excesivo cuando:**
- Es un CRUD simple sin lógica de negocio
- Es un proyecto temporal o prototipo
- El equipo es muy pequeño (1-2 desarrolladores)

**En APiGen**, usamos DDD porque:
1. Es una **plantilla reutilizable** para múltiples proyectos empresariales
2. Requiere **extensibilidad**: agregar módulos (usuarios, roles, permisos, etc.)
3. Necesita **separación clara** entre negocio e infraestructura

---

## 2. Capas de la arquitectura

APiGen implementa una **arquitectura hexagonal** (también llamada Ports & Adapters) basada en DDD.

### 2.1 Diagrama de Capas

```
┌─────────────────────────────────────────────────────────────┐
│                    INFRASTRUCTURE LAYER                      │
│  (Adaptadores externos: HTTP, DB, Config, Filters)          │
│                                                              │
│  ┌────────────┐  ┌──────────┐  ┌────────────┐             │
│  │Controllers │  │  Config  │  │  Filters   │             │
│  │   (REST)   │  │  (Spring)│  │  (HTTP)    │             │
│  └─────┬──────┘  └────────┬─┘  └─────────┬──┘             │
│        │                  │               │                 │
└────────┼──────────────────┼───────────────┼─────────────────┘
         │                  │               │
         ▼                  ▼               ▼
┌─────────────────────────────────────────────────────────────┐
│                   APPLICATION LAYER                          │
│  (Casos de uso: Orquestación, DTOs, Validación)             │
│                                                              │
│  ┌────────────┐  ┌──────────┐  ┌────────────┐             │
│  │  Services  │  │  Mappers │  │    DTOs    │             │
│  │ (Casos uso)│  │(Entity→DTO)│ │(Validación)│             │
│  └─────┬──────┘  └────────┬─┘  └─────────┬──┘             │
│        │                  │               │                 │
└────────┼──────────────────┼───────────────┼─────────────────┘
         │                  │               │
         ▼                  ▼               ▼
┌─────────────────────────────────────────────────────────────┐
│                     DOMAIN LAYER                             │
│  (Núcleo del negocio: Entidades, Reglas, Excepciones)       │
│                                                              │
│  ┌────────────┐  ┌──────────┐  ┌────────────┐             │
│  │ Entities   │  │Repository│  │ Exceptions │             │
│  │  (Base)    │  │(Interfaces)│ │ (Negocio)  │             │
│  └────────────┘  └──────────┘  └────────────┘             │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 Descripción de Capas

#### **Domain Layer (Núcleo)**

Es el **corazón del sistema**. Contiene la lógica de negocio pura.

**Características:**
- **No tiene dependencias** de frameworks externos (excepto JPA mínimo)
- Define **interfaces de repositorios** (implementadas en Infrastructure)
- Contiene **entidades ricas** con comportamiento
- Define **excepciones de dominio** específicas del negocio

**Ejemplo:**
```java
// core/domain/entity/Base.java
@MappedSuperclass
public abstract class Base {
    private Long id;
    private Boolean estado = true;

    // ✅ Comportamiento de negocio
    public void softDelete(String usuario) {
        this.estado = false;
        this.fechaEliminacion = LocalDateTime.now();
        this.eliminadoPor = usuario;
    }
}
```

#### **Application Layer (Casos de Uso)**

Orquesta las operaciones del sistema usando el dominio.

**Características:**
- Implementa **servicios de aplicación** (casos de uso)
- Define **DTOs** para entrada/salida
- Realiza **validaciones de negocio**
- Coordina **transacciones**

**Ejemplo:**
```java
// core/application/service/BaseService.java
public interface BaseService<E extends Base, ID> {
    Result<E, Exception> findById(ID id);
    Result<E, Exception> save(E entity);
    Result<Void, Exception> softDelete(ID id);
}
```

#### **Infrastructure Layer (Adaptadores)**

Conecta el sistema con el mundo exterior.

**Características:**
- **Controllers REST**: Exponen endpoints HTTP
- **Configuraciones Spring**: Beans, seguridad, cache
- **Implementaciones JPA**: Repositorios concretos
- **Filtros/Aspectos**: Logging, métricas, rate limiting

**Ejemplo:**
```java
// core/infrastructure/controller/BaseController.java
public interface BaseController<D extends BaseDTO, ID> {
    ResponseEntity<?> findAll(Pageable pageable);
    ResponseEntity<?> findById(@PathVariable ID id);
    ResponseEntity<?> save(@Valid @RequestBody D dto);
}
```

---

## 3. Estructura de carpetas del proyecto

### 3.1 Vista general del árbol

```
src/main/java/com/jnzader/apigen/
│
├── core/                           # MÓDULO CORE (reutilizable)
│   ├── domain/                     # ← DOMAIN LAYER
│   │   ├── entity/
│   │   │   ├── Base.java           # Entidad base con auditoría
│   │   │   └── audit/
│   │   │       └── Revision.java   # Auditoría con Envers
│   │   ├── event/
│   │   │   ├── DomainEvent.java    # Interfaz de eventos
│   │   │   ├── EntityCreatedEvent.java
│   │   │   └── EntityUpdatedEvent.java
│   │   ├── exception/              # Excepciones de dominio
│   │   │   ├── ResourceNotFoundException.java
│   │   │   ├── ValidationException.java
│   │   │   └── DuplicateResourceException.java
│   │   ├── repository/
│   │   │   └── BaseRepository.java # Interfaz repositorio genérico
│   │   └── specification/
│   │       └── BaseSpecification.java
│   │
│   ├── application/                # ← APPLICATION LAYER
│   │   ├── dto/
│   │   │   ├── BaseDTO.java        # DTO base
│   │   │   └── BaseDTOValidated.java
│   │   ├── mapper/
│   │   │   └── BaseMapper.java     # MapStruct mapper
│   │   ├── service/
│   │   │   ├── BaseService.java    # Interfaz servicio
│   │   │   └── BaseServiceImpl.java # Implementación base
│   │   ├── util/
│   │   │   └── Result.java         # Patrón Result (Either)
│   │   └── validation/
│   │       └── ValidationGroups.java
│   │
│   └── infrastructure/             # ← INFRASTRUCTURE LAYER
│       ├── controller/
│       │   └── BaseController.java # Interfaz REST base
│       ├── config/
│       │   ├── JpaConfig.java      # Config persistencia
│       │   ├── OpenApiConfig.java  # Config Swagger
│       │   ├── CacheConfig.java    # Config Redis/Caffeine
│       │   └── SecurityConfig.java # Config Spring Security
│       ├── filter/
│       │   ├── RateLimitingFilter.java
│       │   └── RequestLoggingFilter.java
│       ├── aspect/
│       │   └── MetricsAspect.java  # Métricas con AOP
│       └── exception/
│           ├── GlobalExceptionHandler.java
│           └── ApiError.java
│
├── security/                       # MÓDULO SECURITY (específico)
│   ├── domain/
│   │   ├── entity/
│   │   │   ├── User.java
│   │   │   ├── Role.java
│   │   │   └── Permission.java
│   │   └── repository/
│   │       ├── UserRepository.java
│   │       └── RoleRepository.java
│   ├── application/
│   │   ├── dto/
│   │   │   ├── LoginRequestDTO.java
│   │   │   └── AuthResponseDTO.java
│   │   └── service/
│   │       ├── AuthService.java
│   │       └── CustomUserDetailsService.java
│   └── infrastructure/
│       ├── controller/
│       │   └── AuthController.java
│       ├── jwt/
│       │   ├── JwtService.java
│       │   └── JwtAuthenticationFilter.java
│       └── config/
│           └── SecurityProperties.java
│
└── ApigenApplication.java          # Clase principal Spring Boot
```

### 3.2 Organización: core/ vs módulos específicos

#### **Carpeta `core/`**
Contiene código **genérico y reutilizable** para cualquier módulo:

- `Base.java`: Entidad base con ID, auditoría, soft delete
- `BaseService.java`: CRUD genérico
- `BaseController.java`: Endpoints REST genéricos
- `Result.java`: Patrón para manejo de errores
- Configuraciones compartidas (Cache, OpenAPI, JPA)

**¿Cuándo usar core/?**
- Cuando el código es **aplicable a múltiples entidades**
- Cuando NO depende de lógica de negocio específica

#### **Módulos específicos (ej: `security/`)**
Contienen lógica particular de un dominio:

- **Entidades**: `User`, `Role`, `Permission`
- **Servicios**: `AuthService`, `JwtService`
- **DTOs**: `LoginRequestDTO`, `RegisterRequestDTO`
- **Controllers**: `AuthController` (login, registro)

**¿Cuándo crear un nuevo módulo?**
- Cuando representa un **contexto acotado** del negocio
- Ejemplos: `inventory/`, `orders/`, `billing/`

---

## 4. Domain Layer en detalle

### 4.1 Entidades (Base.java)

La clase `Base` es una **entidad genérica** que proporciona funcionalidad común a todas las entidades del sistema.

**Código fuente:**
```java
// core/domain/entity/Base.java
@MappedSuperclass
@Getter
@Setter
@Audited
@EntityListeners(AuditingEntityListener.class)
@SQLRestriction("estado = true")
public abstract class Base implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    // ====== AUDITORÍA ======
    @CreatedDate
    private LocalDateTime fechaCreacion;

    @LastModifiedDate
    private LocalDateTime fechaActualizacion;

    @CreatedBy
    private String creadoPor;

    @LastModifiedBy
    private String modificadoPor;

    // ====== SOFT DELETE ======
    @Column(nullable = false)
    private Boolean estado = true;

    private LocalDateTime fechaEliminacion;
    private String eliminadoPor;

    // ====== CONCURRENCIA OPTIMISTA ======
    @Version
    private Long version = 0L;

    // ====== DOMAIN EVENTS ======
    @Transient
    private final List<DomainEvent> domainEvents = new CopyOnWriteArrayList<>();

    // ====== COMPORTAMIENTO DE NEGOCIO ======
    public void softDelete(String usuario) {
        this.estado = false;
        this.fechaEliminacion = LocalDateTime.now();
        this.eliminadoPor = usuario;
    }

    public void restore() {
        this.estado = true;
        this.fechaEliminacion = null;
        this.eliminadoPor = null;
    }

    public void registerEvent(DomainEvent event) {
        this.domainEvents.add(event);
    }
}
```

**Características clave:**

| Característica | Descripción |
|----------------|-------------|
| `@MappedSuperclass` | No crea tabla propia, pero hereda campos a subclases |
| `@Audited` | Hibernate Envers audita cambios automáticamente |
| `@SQLRestriction` | Solo muestra registros con `estado=true` |
| `@Version` | Control de concurrencia optimista (evita pérdidas de actualizaciones) |
| Domain Events | Patrón para notificar cambios sin acoplamiento |

**Ejemplo de uso:**
```java
// security/domain/entity/User.java
@Entity
@Table(name = "users")
public class User extends Base implements UserDetails {
    private String username;
    private String password;
    private String email;

    @ManyToOne
    private Role role;

    // Hereda automáticamente:
    // - id, fechaCreacion, creadoPor
    // - softDelete(), restore()
    // - registerEvent()
}
```

### 4.2 Value Objects

Los **Value Objects** representan conceptos del dominio sin identidad propia (se comparan por valor, no por ID).

**Ejemplo en APiGen:**
```java
// Podríamos agregar Value Objects como:
public record Email(String value) {
    public Email {
        if (!value.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new ValidationException("Email inválido");
        }
    }
}

// Uso en entidad:
@Embedded
private Email email;
```

**Diferencia con Entidades:**
| Entidad | Value Object |
|---------|--------------|
| Tiene ID único | No tiene ID |
| Mutable (puede cambiar) | Inmutable (record) |
| Se compara por ID | Se compara por valor |
| Ejemplo: `User`, `Product` | Ejemplo: `Email`, `Money`, `Address` |

### 4.3 Repositorios (interfaces)

Los repositorios son **interfaces** definidas en el dominio, implementadas en Infrastructure.

**Código fuente:**
```java
// core/domain/repository/BaseRepository.java
@NoRepositoryBean
public interface BaseRepository<E extends Base, ID extends Serializable>
        extends JpaRepository<E, ID>, JpaSpecificationExecutor<E> {

    // ====== CONSULTAS BÁSICAS ======
    List<E> findByEstadoTrue();
    long countByEstadoTrue();

    // ====== SOFT DELETE ======
    @Modifying
    @Query("UPDATE #{#entityName} e SET e.estado = false, " +
           "e.fechaEliminacion = :fecha, e.eliminadoPor = :usuario " +
           "WHERE e.id = :id")
    int softDeleteById(@Param("id") ID id,
                       @Param("fecha") LocalDateTime fecha,
                       @Param("usuario") String usuario);

    // ====== RESTAURACIÓN ======
    @Modifying
    @Query("UPDATE #{#entityName} e SET e.estado = true " +
           "WHERE e.id = :id")
    int restoreById(@Param("id") ID id);

    // ====== HARD DELETE ======
    @Modifying
    @Query("DELETE FROM #{#entityName} e WHERE e.id = :id")
    int hardDeleteById(@Param("id") ID id);
}
```

**Puntos importantes:**

1. **`@NoRepositoryBean`**: Evita que Spring cree implementación de esta interfaz base
2. **`#{#entityName}`**: Usa el nombre de la entidad hija (ej: `User`)
3. **Soft Delete personalizado**: Necesario porque `@SQLRestriction` no afecta a `UPDATE`/`DELETE`

**Ejemplo de repositorio específico:**
```java
// security/domain/repository/UserRepository.java
public interface UserRepository extends BaseRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByEmail(String email);
}
```

### 4.4 Domain Events

Los **eventos de dominio** notifican que algo importante ocurrió en el negocio.

**Interfaz base:**
```java
// core/domain/event/DomainEvent.java
public interface DomainEvent {
    LocalDateTime occurredOn();

    default String eventType() {
        return this.getClass().getSimpleName();
    }
}
```

**Eventos implementados:**
```java
// core/domain/event/EntityCreatedEvent.java
public record EntityCreatedEvent(
    Object entity,
    LocalDateTime occurredOn
) implements DomainEvent {}

// core/domain/event/EntityDeletedEvent.java
public record EntityDeletedEvent(
    Long entityId,
    String entityType,
    LocalDateTime occurredOn
) implements DomainEvent {}
```

**Cómo se usan:**
```java
// En BaseServiceImpl
public Result<E, Exception> save(E entity) {
    E saved = repository.save(entity);

    // Registrar evento
    saved.registerEvent(new EntityCreatedEvent(saved, LocalDateTime.now()));

    return Result.success(saved);
}

// Spring Data lo publica automáticamente al final de la transacción
```

**Ventajas:**
- Desacoplamiento: El servicio no necesita conocer qué pasa después
- Auditoría: Se puede registrar en logs/base de datos
- Integración: Enviar a un bus de mensajes (RabbitMQ, Kafka)

### 4.5 Excepciones de dominio

Las excepciones del dominio representan **errores de negocio**.

**Excepciones implementadas:**

```java
// core/domain/exception/ResourceNotFoundException.java
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}

// core/domain/exception/ValidationException.java
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class ValidationException extends RuntimeException {
    public ValidationException(String message) {
        super(message);
    }
}

// core/domain/exception/DuplicateResourceException.java
@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}
```

**Uso correcto:**
```java
// ✅ CORRECTO: Lanzar en el dominio/servicio
public Result<User, Exception> findById(Long id) {
    return repository.findById(id)
        .map(Result::<User, Exception>success)
        .orElseGet(() -> Result.failure(
            new ResourceNotFoundException("Usuario no encontrado: " + id)
        ));
}

// ❌ INCORRECTO: Lanzar en el controller
@GetMapping("/{id}")
public ResponseEntity<?> findById(@PathVariable Long id) {
    if (id < 0) {
        throw new ValidationException("ID inválido");  // ¡NO!
    }
}
```

---

## 5. Application Layer en detalle

### 5.1 DTOs y validación

Los **DTOs (Data Transfer Objects)** transfieren datos entre capas sin exponer entidades.

**DTO Base:**
```java
// core/application/dto/BaseDTO.java
public abstract class BaseDTO implements Serializable {
    private Long id;
    private Boolean activo = true;  // Mapea a 'estado' en entidad
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
}
```

**DTO con validación:**
```java
// security/application/dto/RegisterRequestDTO.java
public record RegisterRequestDTO(
    @NotBlank(message = "El username es obligatorio")
    @Size(min = 3, max = 50)
    String username,

    @NotBlank(message = "El password es obligatorio")
    @StrongPassword  // Validador personalizado
    String password,

    @NotBlank @Email
    String email
) {}
```

**Validador personalizado:**
```java
// security/application/validation/StrongPassword.java
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = StrongPasswordValidator.class)
public @interface StrongPassword {
    String message() default "Password debe tener 8+ caracteres, " +
                             "mayúscula, minúscula y número";
}

// Implementación:
public class StrongPasswordValidator
        implements ConstraintValidator<StrongPassword, String> {

    @Override
    public boolean isValid(String password, ConstraintValidatorContext ctx) {
        if (password == null) return false;

        return password.length() >= 8 &&
               password.matches(".*[A-Z].*") &&
               password.matches(".*[a-z].*") &&
               password.matches(".*\\d.*");
    }
}
```

**Grupos de validación:**
```java
// core/application/validation/ValidationGroups.java
public interface ValidationGroups {
    interface Create {}
    interface Update {}
    interface Import {}
}

// Uso:
public record UserDTO(
    @Null(groups = Create.class)  // No enviar ID al crear
    @NotNull(groups = Update.class)  // Requerido al actualizar
    Long id,

    @NotBlank(groups = {Create.class, Update.class})
    String username
) {}
```

### 5.2 Mappers

Los **Mappers** convierten entre Entidades y DTOs usando **MapStruct**.

**Interfaz base:**
```java
// core/application/mapper/BaseMapper.java
public interface BaseMapper<E extends Base, D extends BaseDTO> {

    @Mapping(source = "estado", target = "activo")
    D toDTO(E entity);

    @InheritInverseConfiguration
    E toEntity(D dto);

    List<D> toDTOList(List<E> entities);

    // Para PATCH: solo actualiza campos no-null
    @BeanMapping(nullValuePropertyMappingStrategy = IGNORE)
    @Mapping(source = "activo", target = "estado")
    void updateEntityFromDTO(D dto, @MappingTarget E entity);
}
```

**Mapper específico:**
```java
// Ejemplo: UserMapper (generado por MapStruct)
@Mapper(componentModel = "spring")
public interface UserMapper extends BaseMapper<User, UserDTO> {

    // Mapeo personalizado para campo complejo
    @Mapping(source = "role.name", target = "roleName")
    @Mapping(source = "estado", target = "activo")
    UserDTO toDTO(User user);

    // No mapear password al DTO (seguridad)
    @Mapping(target = "password", ignore = true)
    User toEntity(UserDTO dto);
}
```

**MapStruct genera automáticamente:**
```java
@Component
public class UserMapperImpl implements UserMapper {

    public UserDTO toDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setActivo(user.getEstado());  // ← Mapeo automático
        dto.setRoleName(user.getRole().getName());
        return dto;
    }
}
```

### 5.3 Services

Los **servicios** implementan los **casos de uso** del sistema.

**Interfaz del servicio:**
```java
// core/application/service/BaseService.java
public interface BaseService<E extends Base, ID extends Serializable> {

    // ====== CONSULTAS ======
    Result<List<E>, Exception> findAll();
    Result<Page<E>, Exception> findAll(Pageable pageable);
    Result<E, Exception> findById(ID id);
    Result<Boolean, Exception> existsById(ID id);

    // ====== BÚSQUEDA CON SPECIFICATIONS ======
    Result<List<E>, Exception> findAll(Specification<E> spec);
    Result<Page<E>, Exception> findAll(Specification<E> spec, Pageable pageable);

    // ====== ESCRITURA ======
    Result<E, Exception> save(E entity);
    Result<E, Exception> update(ID id, E entity);
    Result<E, Exception> partialUpdate(ID id, E entity);

    // ====== SOFT DELETE ======
    Result<Void, Exception> softDelete(ID id);
    Result<E, Exception> restore(ID id);

    // ====== BATCH ======
    Result<List<E>, Exception> saveAll(List<E> entities);
    Result<Integer, Exception> softDeleteAll(List<ID> ids);
}
```

**Implementación típica:**
```java
// Ejemplo: AuthService
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public Result<AuthResponseDTO, Exception> login(LoginRequestDTO request) {
        // 1. Validar usuario
        return userRepository.findByUsername(request.username())
            .map(user -> {
                // 2. Verificar password
                if (!passwordEncoder.matches(request.password(), user.getPassword())) {
                    return Result.<AuthResponseDTO, Exception>failure(
                        new UnauthorizedException("Credenciales inválidas")
                    );
                }

                // 3. Generar token
                String token = jwtService.generateToken(user);

                // 4. Retornar respuesta
                return Result.success(new AuthResponseDTO(token, user.getUsername()));
            })
            .orElseGet(() -> Result.failure(
                new ResourceNotFoundException("Usuario no encontrado")
            ));
    }
}
```

### 5.4 Result pattern para errores

El patrón **Result** es una alternativa funcional a las excepciones para manejo de errores.

**Implementación:**
```java
// core/application/util/Result.java
public sealed interface Result<T, E> {

    record Success<T, E>(T value) implements Result<T, E> {}
    record Failure<T, E>(E error) implements Result<T, E> {}

    // Factory methods
    static <T, E> Result<T, E> success(T value) {
        return new Success<>(value);
    }

    static <T, E> Result<T, E> failure(E error) {
        return new Failure<>(error);
    }

    // Transformaciones
    default <U> Result<U, E> map(Function<T, U> mapper) {
        return switch (this) {
            case Success<T, E> s -> success(mapper.apply(s.value()));
            case Failure<T, E> f -> failure(f.error());
        };
    }

    default <U> Result<U, E> flatMap(Function<T, Result<U, E>> mapper) {
        return switch (this) {
            case Success<T, E> s -> mapper.apply(s.value());
            case Failure<T, E> f -> failure(f.error());
        };
    }

    // Extracción
    default T orElseThrow() {
        return switch (this) {
            case Success<T, E> s -> s.value();
            case Failure<T, E> f -> throw (RuntimeException) f.error();
        };
    }
}
```

**Ventajas sobre excepciones:**

| Result Pattern | Excepciones |
|----------------|-------------|
| Explícito en firma | Oculto (checked vs unchecked) |
| Funcional (map, flatMap) | Imperativo (try-catch) |
| Composible | Difícil de componer |
| Performance predecible | Stack trace costoso |

**Ejemplo de uso:**
```java
// ✅ Composición funcional con Result
public Result<UserDTO, Exception> getUserProfile(Long id) {
    return userService.findById(id)
        .map(userMapper::toDTO)
        .map(this::enrichWithPermissions)
        .onFailure(error -> log.error("Error obteniendo perfil", error));
}

// ❌ Imperativo con try-catch
public UserDTO getUserProfile(Long id) {
    try {
        User user = userService.findById(id);
        UserDTO dto = userMapper.toDTO(user);
        return enrichWithPermissions(dto);
    } catch (Exception e) {
        log.error("Error obteniendo perfil", e);
        throw e;
    }
}
```

---

## 6. Infrastructure Layer en detalle

### 6.1 Controllers

Los **Controllers** exponen la API REST y delegan al servicio.

**Interfaz base:**
```java
// core/infrastructure/controller/BaseController.java
public interface BaseController<D extends BaseDTO, ID extends Serializable> {

    // GET / - Listar con filtros
    ResponseEntity<?> findAll(
        @RequestParam(required = false) String filter,
        @RequestParam(required = false) Map<String, String> filters,
        @RequestParam(required = false) Set<String> fields,
        Pageable pageable
    );

    // GET /{id} - Obtener por ID
    ResponseEntity<?> findById(
        @PathVariable ID id,
        @RequestParam(required = false) Set<String> fields,
        @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch
    );

    // POST / - Crear
    ResponseEntity<?> save(@Valid @RequestBody D dto);

    // PUT /{id} - Actualizar completo
    ResponseEntity<?> update(
        @PathVariable ID id,
        @Valid @RequestBody D dto,
        @RequestHeader(value = "If-Match", required = false) String ifMatch
    );

    // PATCH /{id} - Actualizar parcial
    ResponseEntity<?> partialUpdate(@PathVariable ID id, @RequestBody D dto);

    // DELETE /{id} - Soft delete
    ResponseEntity<Void> delete(
        @PathVariable ID id,
        @RequestParam(defaultValue = "false") boolean permanent
    );
}
```

**Implementación:**
```java
// security/infrastructure/controller/AuthController.java
@RestController
@RequestMapping("/api/v1/auth")
@Validated
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDTO request) {
        return authService.login(request)
            .fold(
                // Success: 200 OK
                authResponse -> ResponseEntity.ok(authResponse),
                // Failure: mapear a código HTTP
                error -> switch (error) {
                    case UnauthorizedException e ->
                        ResponseEntity.status(401).body(new ApiError(e.getMessage()));
                    case ResourceNotFoundException e ->
                        ResponseEntity.status(404).body(new ApiError(e.getMessage()));
                    default ->
                        ResponseEntity.status(500).body(new ApiError("Error interno"));
                }
            );
    }

    @PostMapping("/register")
    @Validated(ValidationGroups.Create.class)
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequestDTO request) {
        return authService.register(request)
            .fold(
                user -> ResponseEntity
                    .status(201)
                    .header("Location", "/api/v1/users/" + user.id())
                    .body(user),
                error -> ResponseEntity.badRequest().body(new ApiError(error.getMessage()))
            );
    }
}
```

### 6.2 Configuraciones Spring

#### **JpaConfig**
```java
// core/infrastructure/config/JpaConfig.java
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
@EnableJpaRepositories(basePackages = "com.jnzader.apigen")
public class JpaConfig {

    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            return Optional.ofNullable(auth)
                .map(Authentication::getName)
                .or(() -> Optional.of("SYSTEM"));
        };
    }
}
```

#### **CacheConfig**
```java
// core/infrastructure/config/CacheConfig.java
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        return new CaffeineCacheManager("users", "roles", "permissions");
    }

    @Bean
    public Caffeine<Object, Object> caffeineConfig() {
        return Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .maximumSize(1000)
            .recordStats();
    }
}
```

#### **OpenApiConfig**
```java
// core/infrastructure/config/OpenApiConfig.java
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("APiGen - API Genérica")
                .version("1.0.0")
                .description("Plantilla REST con DDD, Spring Boot 4, Java 25")
                .contact(new Contact()
                    .name("Javier N. Zader")
                    .url("https://github.com/JNZader")))
            .addSecurityItem(new SecurityRequirement().addList("Bearer"))
            .components(new Components()
                .addSecuritySchemes("Bearer", new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")));
    }
}
```

### 6.3 Persistencia JPA

**BaseRepository implementa:**
```java
// Spring Data JPA genera automáticamente:
public interface UserRepository extends BaseRepository<User, Long> {
    // Métodos derivados del nombre
    Optional<User> findByUsername(String username);
    List<User> findByEmailContaining(String email);

    // Queries personalizadas
    @Query("SELECT u FROM User u WHERE u.role.name = :roleName")
    List<User> findByRoleName(@Param("roleName") String roleName);

    // Specifications para filtros dinámicos
    default Specification<User> hasUsername(String username) {
        return (root, query, cb) ->
            cb.equal(root.get("username"), username);
    }
}
```

**Uso de Specifications:**
```java
// Filtro dinámico:
Specification<User> spec = Specification
    .where(hasUsername("admin"))
    .and((root, query, cb) -> cb.equal(root.get("enabled"), true));

List<User> users = userRepository.findAll(spec);
```

### 6.4 Filtros y aspectos

#### **RequestLoggingFilter**
```java
// core/infrastructure/filter/RequestLoggingFilter.java
@Component
public class RequestLoggingFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) {
        HttpServletRequest req = (HttpServletRequest) request;

        String requestId = UUID.randomUUID().toString();
        MDC.put("requestId", requestId);

        log.info("REQUEST: {} {} from {}",
            req.getMethod(), req.getRequestURI(), req.getRemoteAddr());

        long start = System.currentTimeMillis();
        chain.doFilter(request, response);
        long duration = System.currentTimeMillis() - start;

        log.info("RESPONSE: {} ms", duration);
        MDC.clear();
    }
}
```

#### **MetricsAspect**
```java
// core/infrastructure/aspect/MetricsAspect.java
@Aspect
@Component
public class MetricsAspect {

    private final MeterRegistry meterRegistry;

    @Around("@annotation(measured)")
    public Object measureExecution(ProceedingJoinPoint pjp, Measured measured) {
        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            return pjp.proceed();
        } finally {
            sample.stop(Timer.builder("method.execution")
                .tag("class", pjp.getSignature().getDeclaringTypeName())
                .tag("method", pjp.getSignature().getName())
                .register(meterRegistry));
        }
    }
}

// Uso:
@Measured
public Result<User, Exception> findById(Long id) {
    // ...
}
```

---

## 7. Flujo de una petición HTTP

### 7.1 Diagrama de secuencia

```
Cliente HTTP                Controller              Service              Repository           Database
    │                          │                      │                      │                   │
    │  GET /api/v1/users/1     │                      │                      │                   │
    │─────────────────────────>│                      │                      │                   │
    │                          │                      │                      │                   │
    │                          │ findById(1)          │                      │                   │
    │                          │─────────────────────>│                      │                   │
    │                          │                      │                      │                   │
    │                          │                      │ findById(1)          │                   │
    │                          │                      │─────────────────────>│                   │
    │                          │                      │                      │                   │
    │                          │                      │                      │  SELECT * FROM... │
    │                          │                      │                      │──────────────────>│
    │                          │                      │                      │                   │
    │                          │                      │                      │<──────────────────│
    │                          │                      │                      │  User entity      │
    │                          │                      │                      │                   │
    │                          │                      │<─────────────────────│                   │
    │                          │                      │  Optional<User>      │                   │
    │                          │                      │                      │                   │
    │                          │<─────────────────────│                      │                   │
    │                          │  Result<User, Ex>    │                      │                   │
    │                          │                      │                      │                   │
    │                          │ toDTO(user)          │                      │                   │
    │                          │─────────────────────>│                      │                   │
    │                          │  Mapper              │                      │                   │
    │                          │                      │                      │                   │
    │                          │<─────────────────────│                      │                   │
    │                          │  UserDTO             │                      │                   │
    │                          │                      │                      │                   │
    │<─────────────────────────│                      │                      │                   │
    │  200 OK                  │                      │                      │                   │
    │  { "id": 1, "username":  │                      │                      │                   │
    │    "admin", ... }        │                      │                      │                   │
```

### 7.2 Paso a paso detallado

**1. Cliente envía request:**
```http
GET /api/v1/users/1 HTTP/1.1
Host: localhost:8080
Authorization: Bearer eyJhbGci...
Accept: application/json
If-None-Match: "abc123"
```

**2. Filtros procesan request:**
```java
// RateLimitingFilter: Verifica límite de peticiones
// JwtAuthenticationFilter: Valida token JWT
// RequestLoggingFilter: Registra en logs
```

**3. Controller recibe petición:**
```java
@GetMapping("/{id}")
public ResponseEntity<?> findById(
    @PathVariable Long id,
    @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch
) {
    // Delega al servicio
    return userService.findById(id)
        .fold(
            user -> {
                // Mapear a DTO
                UserDTO dto = userMapper.toDTO(user);

                // Generar ETag
                String etag = etagGenerator.generateETag(dto);

                // Cache condicional
                if (etag.equals(ifNoneMatch)) {
                    return ResponseEntity.status(304).build();  // Not Modified
                }

                // Retornar con ETag
                return ResponseEntity.ok()
                    .eTag(etag)
                    .body(dto);
            },
            error -> ResponseEntity.status(404).body(new ApiError(error.getMessage()))
        );
}
```

**4. Service busca en repositorio:**
```java
@Cacheable(value = "users", key = "#id")
@Measured  // Métricas
public Result<User, Exception> findById(Long id) {
    log.debug("Buscando usuario con ID: {}", id);

    return repository.findById(id)
        .map(user -> {
            // Registrar evento de consulta
            user.registerEvent(new EntityAccessedEvent(user, LocalDateTime.now()));
            return Result.<User, Exception>success(user);
        })
        .orElseGet(() -> Result.failure(
            new ResourceNotFoundException("Usuario no encontrado: " + id)
        ));
}
```

**5. Repository ejecuta query:**
```java
// Spring Data JPA genera automáticamente:
@Query("SELECT u FROM User u WHERE u.id = :id AND u.estado = true")
Optional<User> findById(@Param("id") Long id);

// SQL generado:
// SELECT u.id, u.username, u.email, ...
// FROM users u
// WHERE u.id = ? AND u.estado = true
```

**6. Hibernate consulta base de datos:**
```sql
-- SQL ejecutado:
SELECT
    u.id, u.username, u.email, u.password,
    u.created_at, u.updated_at, u.created_by, u.version,
    r.id, r.name  -- JOIN con Role
FROM users u
LEFT JOIN roles r ON u.role_id = r.id
WHERE u.id = 1 AND u.estado = true;
```

**7. Entidad se convierte a DTO:**
```java
// MapStruct genera automáticamente:
public UserDTO toDTO(User user) {
    UserDTO dto = new UserDTO();
    dto.setId(user.getId());
    dto.setUsername(user.getUsername());
    dto.setEmail(user.getEmail());
    dto.setActivo(user.getEstado());  // estado → activo
    dto.setRoleName(user.getRole().getName());
    // NO incluye password (seguridad)
    return dto;
}
```

**8. Response se envía al cliente:**
```http
HTTP/1.1 200 OK
Content-Type: application/json
ETag: "abc123"
Cache-Control: max-age=300
X-Request-Id: 550e8400-e29b-41d4-a716-446655440000

{
  "id": 1,
  "username": "admin",
  "email": "admin@example.com",
  "activo": true,
  "roleName": "ADMIN",
  "fechaCreacion": "2024-11-26T10:00:00",
  "fechaActualizacion": "2024-11-26T15:30:00"
}
```

### 7.3 Manejo de errores

**Si el usuario no existe:**
```java
// Repository retorna Optional.empty()
// Service retorna Result.failure(ResourceNotFoundException)
// Controller mapea a 404:

return ResponseEntity.status(404).body(new ApiError(
    "NOT_FOUND",
    "Usuario no encontrado: 1",
    "/api/v1/users/1"
));
```

**Respuesta HTTP:**
```http
HTTP/1.1 404 Not Found
Content-Type: application/problem+json

{
  "type": "NOT_FOUND",
  "title": "Recurso no encontrado",
  "status": 404,
  "detail": "Usuario no encontrado: 1",
  "instance": "/api/v1/users/1",
  "timestamp": "2024-11-26T15:30:00Z"
}
```

---

## 8. Reglas de dependencias

### 8.1 Qué puede importar qué

```
┌─────────────────────────────────────────────────────────────┐
│                    INFRASTRUCTURE                            │
│  Puede importar: ✅ Domain, ✅ Application, ✅ Spring         │
└─────────────────────────────────────────────────────────────┘
                              ▲
                              │ OK
                              │
┌─────────────────────────────────────────────────────────────┐
│                    APPLICATION                               │
│  Puede importar: ✅ Domain, ✅ Spring básico                 │
│  NO puede: ❌ Infrastructure                                 │
└─────────────────────────────────────────────────────────────┘
                              ▲
                              │ OK
                              │
┌─────────────────────────────────────────────────────────────┐
│                      DOMAIN                                  │
│  Puede importar: ✅ Java estándar, ✅ JPA mínimo             │
│  NO puede: ❌ Application, ❌ Infrastructure, ❌ Spring       │
└─────────────────────────────────────────────────────────────┘
```

### 8.2 Tabla de dependencias permitidas

| Desde / Hacia | Domain | Application | Infrastructure |
|---------------|--------|-------------|----------------|
| **Domain** | ✅ | ❌ | ❌ |
| **Application** | ✅ | ✅ | ❌ |
| **Infrastructure** | ✅ | ✅ | ✅ |

**Reglas específicas:**

1. **Domain NO debe depender de nadie:**
   ```java
   // ❌ INCORRECTO
   import org.springframework.stereotype.Service;
   import com.jnzader.apigen.core.application.dto.UserDTO;

   public class User extends Base {
       // ...
   }
   ```

2. **Application NO debe depender de Infrastructure:**
   ```java
   // ❌ INCORRECTO
   import com.jnzader.apigen.core.infrastructure.controller.UserController;

   @Service
   public class UserService {
       private UserController controller;  // ¡NO!
   }
   ```

3. **Infrastructure puede depender de todo:**
   ```java
   // ✅ CORRECTO
   import com.jnzader.apigen.core.domain.entity.User;
   import com.jnzader.apigen.core.application.service.UserService;

   @RestController
   public class UserController {
       private final UserService userService;  // OK
   }
   ```

### 8.3 Tests de arquitectura con ArchUnit

APiGen valida automáticamente estas reglas con **ArchUnit**:

```java
// architecture/ArchitectureTest.java
@Test
void shouldRespectLayerDependencies() {
    ArchRule rule = layeredArchitecture()
        .consideringAllDependencies()

        // Definir capas
        .layer("Domain").definedBy(
            "..domain.entity..",
            "..domain.event..",
            "..domain.exception..",
            "..domain.repository..",
            "..domain.specification.."
        )
        .layer("Application").definedBy(
            "..application.dto..",
            "..application.mapper..",
            "..application.service..",
            "..application.validation.."
        )
        .layer("Infrastructure").definedBy(
            "..infrastructure.config..",
            "..infrastructure.controller..",
            "..infrastructure.filter..",
            "..infrastructure.aspect.."
        )

        // Reglas de dependencia
        .whereLayer("Domain")
            .mayOnlyBeAccessedByLayers("Application", "Infrastructure")
        .whereLayer("Application")
            .mayOnlyBeAccessedByLayers("Infrastructure")
        .whereLayer("Infrastructure")
            .mayNotBeAccessedByAnyLayer();

    rule.check(importedClasses);
}
```

**Otros tests de arquitectura:**

```java
@Test
void domainShouldNotDependOnOuterLayers() {
    noClasses()
        .that().resideInAPackage("..domain..")
        .should().dependOnClassesThat().resideInAnyPackage(
            "..application..",
            "..infrastructure.."
        )
        .check(importedClasses);
}

@Test
void servicesShouldNotDependOnControllers() {
    noClasses()
        .that().resideInAPackage("..application.service..")
        .should().dependOnClassesThat().resideInPackage("..infrastructure.controller..")
        .check(importedClasses);
}

@Test
void controllersShouldBeSuffixedWithController() {
    classes()
        .that().resideInAPackage("..infrastructure.controller..")
        .and().areNotInterfaces()
        .should().haveSimpleNameEndingWith("Controller")
        .orShould().haveSimpleNameEndingWith("ControllerImpl")
        .check(importedClasses);
}
```

**Ejecutar tests:**
```bash
./mvnw test -Dtest=ArchitectureTest
```

**Si fallas un test:**
```
java.lang.AssertionError: Architecture Violation [Priority: MEDIUM] - Rule 'no classes that reside in a package '..domain..' should depend on classes that reside in any package ['..application..', '..infrastructure..']' was violated (1 times):
Method <com.jnzader.apigen.security.domain.entity.User.setRole(Role)> calls method <com.jnzader.apigen.security.application.service.RoleService.validateRole(Role)> in (User.java:175)
```

---

## Conclusión

### Beneficios de esta arquitectura

1. **Separación de preocupaciones**: Cada capa tiene una responsabilidad clara
2. **Testabilidad**: Puedes testear dominio sin Spring
3. **Mantenibilidad**: Fácil encontrar código relacionado
4. **Escalabilidad**: Agregar módulos sin romper existentes
5. **Flexibilidad**: Cambiar Spring por otro framework sin reescribir dominio

### Próximos pasos

1. Leer el código fuente en `C:\Programacion\Portfolio\java\apigen`
2. Crear tu primer módulo siguiendo esta estructura
3. Ejecutar los tests de arquitectura con `ArchitectureTest`
4. Experimentar con `Result` pattern vs excepciones
5. Implementar un caso de uso completo (CRUD + validación)

### Recursos adicionales

- **Código fuente**: `C:\Programacion\Portfolio\java\apigen\src`
- **Tests**: `C:\Programacion\Portfolio\java\apigen\src\test`
- **Swagger UI**: http://localhost:8080/swagger-ui.html (al ejecutar)
- **H2 Console**: http://localhost:8080/h2-console (en desarrollo)

---

**Autor**: Javier N. Zader
**Proyecto**: APiGen - API Genérica con DDD
**Versión**: 1.0.0
**Última actualización**: 2024-11-27
