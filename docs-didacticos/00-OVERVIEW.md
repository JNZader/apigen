# APiGen - Overview del Proyecto

## Que es APiGen

APiGen es una **libreria multi-modulo reutilizable** para construir APIs REST con Spring Boot 4.0.0 y Java 25. Proporciona clases base, auto-configuracion y generadores de codigo para acelerar el desarrollo de APIs.

### Evolucion del Proyecto

```
v1.0 (2024)          v2.0 (2024)              v3.0 (2025)
Monolito       -->   Template Repo      -->   Libreria Multi-Modulo
(copiar/pegar)       (clonar y modificar)     (dependencia + auto-config)
```

### Por que existe este proyecto

**El Problema:** Cada vez que inicias un proyecto de API REST, terminas escribiendo el mismo codigo boilerplate:
- CRUD basico
- Validaciones
- Manejo de errores
- Seguridad JWT
- Paginacion
- Cache

**La Solucion:** APiGen proporciona todo esto como una **libreria que agregas como dependencia**:

```groovy
dependencies {
    implementation platform('com.jnzader:apigen-bom:1.0.0-SNAPSHOT')
    implementation 'com.jnzader:apigen-core'
    implementation 'com.jnzader:apigen-security'  // opcional
}
```

## Arquitectura Multi-Modulo

```
apigen/
├── apigen-core/        # Clases base (Entity, Repository, Service, Controller)
├── apigen-security/    # Seguridad JWT/OAuth2 (opcional)
├── apigen-codegen/     # Generador de codigo desde SQL
├── apigen-bom/         # Bill of Materials (gestion de versiones)
└── apigen-example/     # Aplicacion de ejemplo funcionando
```

### Por que Multi-Modulo?

| Modulo | Proposito | Cuando usarlo |
|--------|-----------|---------------|
| **apigen-core** | Clases base genericas | Siempre (obligatorio) |
| **apigen-security** | Autenticacion JWT | Cuando necesites seguridad |
| **apigen-codegen** | Generar desde SQL | Database-first |
| **apigen-bom** | Control de versiones | Proyectos multi-modulo |
| **apigen-example** | Referencia y template | Para aprender/empezar |

## Arquitectura DDD por Modulo

Cada modulo sigue Domain-Driven Design con capas separadas:

```
apigen-core/
└── src/main/java/com/jnzader/apigen/core/
    ├── domain/                    # CAPA DE DOMINIO
    │   ├── entity/
    │   │   └── Base.java          # Entidad base con auditoria
    │   ├── repository/
    │   │   └── BaseRepository.java
    │   ├── event/                 # Domain events
    │   ├── exception/             # Excepciones de dominio
    │   └── specification/
    │       └── FilterSpecificationBuilder.java
    │
    ├── application/               # CAPA DE APLICACION
    │   ├── dto/
    │   │   └── BaseDTO.java
    │   ├── mapper/
    │   │   └── BaseMapper.java
    │   ├── service/
    │   │   ├── BaseService.java
    │   │   └── BaseServiceImpl.java
    │   └── validation/
    │
    └── infrastructure/            # CAPA DE INFRAESTRUCTURA
        ├── controller/
        │   ├── BaseController.java
        │   └── BaseControllerImpl.java
        ├── config/
        │   └── ApigenCoreAutoConfiguration.java  # Spring Boot Starter
        ├── exception/
        │   └── GlobalExceptionHandler.java
        └── hateoas/
            └── BaseResourceAssembler.java
```

### Flujo de Dependencias

```
┌─────────────────────────────────────────────────────────────────┐
│                     ARQUITECTURA DDD                            │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  INFRAESTRUCTURA                                         │  │
│  │  - BaseControllerImpl (REST endpoints)                   │  │
│  │  - GlobalExceptionHandler (RFC 7807)                     │  │
│  │  - ApigenCoreAutoConfiguration (Spring Boot Starter)     │  │
│  └────────────────────┬─────────────────────────────────────┘  │
│                       │ depende de                              │
│                       ▼                                         │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  APLICACION                                              │  │
│  │  - BaseDTO (validaciones)                                │  │
│  │  - BaseMapper (MapStruct)                                │  │
│  │  - BaseServiceImpl (logica de negocio)                   │  │
│  │  - Result Pattern (Vavr)                                 │  │
│  └────────────────────┬─────────────────────────────────────┘  │
│                       │ depende de                              │
│                       ▼                                         │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  DOMINIO (sin dependencias externas)                     │  │
│  │  - Base (entidad base con auditoria)                     │  │
│  │  - BaseRepository (queries genericas)                    │  │
│  │  - Domain Events (comunicacion entre agregados)          │  │
│  │  - Soft Delete (eliminacion logica)                      │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

## Caracteristicas Principales

### 1. Clases Base (apigen-core)

```java
// Tu entidad extiende Base
@Entity
public class Product extends Base {
    private String name;
    private BigDecimal price;
    // id, estado, auditoria, version se heredan
}

// Tu controller extiende BaseControllerImpl
@RestController
@RequestMapping("/api/products")
public class ProductController extends BaseControllerImpl<Product, ProductDTO, Long> {
    // Heredas 12+ endpoints CRUD automaticamente
}
```

**Endpoints heredados:**

| Metodo | Endpoint | Descripcion |
|--------|----------|-------------|
| GET | `/` | Listar con paginacion y filtrado |
| GET | `/{id}` | Obtener por ID con ETag |
| HEAD | `/` | Obtener conteo |
| HEAD | `/{id}` | Verificar existencia |
| POST | `/` | Crear |
| PUT | `/{id}` | Actualizar completo |
| PATCH | `/{id}` | Actualizar parcial |
| DELETE | `/{id}` | Soft delete |
| DELETE | `/{id}?permanent=true` | Hard delete |
| POST | `/{id}/restore` | Restaurar |
| GET | `/cursor` | Paginacion por cursor |

### 2. Auto-Configuracion (Spring Boot Starter)

APiGen usa `@AutoConfiguration` para configurarse automaticamente:

```java
// En apigen-core
@AutoConfiguration
@ConditionalOnProperty(name = "apigen.core.enabled", havingValue = "true", matchIfMissing = true)
public class ApigenCoreAutoConfiguration {
    // Configura:
    // - CacheEvictionService
    // - FilterSpecificationBuilder
    // - GlobalExceptionHandler
    // - AuditorAware
}
```

Solo necesitas agregar la dependencia y funciona:

```yaml
# application.yaml (opcional, todo tiene defaults)
apigen:
  core:
    enabled: true  # default
  security:
    enabled: true
    jwt:
      secret: ${JWT_SECRET}
```

### 3. Seguridad JWT (apigen-security)

```java
// Auto-configurado cuando agregas la dependencia
@AutoConfiguration
@ConditionalOnProperty(name = "apigen.security.enabled", havingValue = "true")
public class ApigenSecurityAutoConfiguration {
    // Configura:
    // - JwtService
    // - AuthService
    // - SecurityFilterChain
    // - AuthController (/auth/login, /auth/refresh, /auth/logout)
}
```

### 4. Filtrado Dinamico

```bash
# Filtrar por campo
GET /api/products?filter=category:eq:Electronics

# Multiples filtros
GET /api/products?filter=price:gte:100,price:lte:500

# Operadores: eq, neq, like, starts, ends, gt, gte, lt, lte, in, between, null, notnull
```

### 5. Result Pattern (Vavr)

```java
// En lugar de excepciones, usamos Result<T, E>
public Result<Product, Exception> findById(Long id) {
    return Result.of(() -> repository.findById(id)
        .orElseThrow(() -> new EntityNotFoundException("Product", id)));
}

// Uso funcional
service.findById(1L)
    .map(mapper::toDTO)
    .peek(dto -> log.info("Found: {}", dto))
    .getOrElseThrow(error -> new ResponseStatusException(404));
```

### 6. Soft Delete + Auditoria

Todas las entidades heredan de `Base`:

```java
public abstract class Base {
    @Id
    private Long id;

    private Boolean estado = true;           // Soft delete

    @CreatedDate
    private LocalDateTime fechaCreacion;

    @LastModifiedDate
    private LocalDateTime fechaActualizacion;

    @CreatedBy
    private String creadoPor;

    @LastModifiedBy
    private String modificadoPor;

    private LocalDateTime fechaEliminacion;
    private String eliminadoPor;

    @Version
    private Long version;                    // Optimistic locking
}
```

### 7. HATEOAS

```json
{
  "id": 1,
  "name": "Product A",
  "_links": {
    "self": { "href": "/api/products/1" },
    "collection": { "href": "/api/products" },
    "update": { "href": "/api/products/1" },
    "delete": { "href": "/api/products/1" }
  }
}
```

## Stack Tecnologico

### Core
| Tecnologia | Version | Proposito |
|------------|---------|-----------|
| Java | 25 | Virtual Threads habilitados |
| Spring Boot | 4.0.0 | Framework base |
| PostgreSQL | 17 | Base de datos |

### Mapping & Validation
| Tecnologia | Version | Proposito |
|------------|---------|-----------|
| MapStruct | 1.6.3 | Mapping DTO <-> Entity |
| Bean Validation | - | Validaciones declarativas |
| Vavr | 0.10.5 | Result Pattern |

### Security
| Tecnologia | Version | Proposito |
|------------|---------|-----------|
| Spring Security | - | Seguridad |
| JJWT | 0.13.0 | Tokens JWT |

### Cache & Resilience
| Tecnologia | Version | Proposito |
|------------|---------|-----------|
| Caffeine | 3.2.x | Cache en memoria |
| Resilience4j | 2.3.x | Rate limiting |

### Documentation
| Tecnologia | Version | Proposito |
|------------|---------|-----------|
| SpringDoc OpenAPI | 3.0.0 | Swagger UI |

## Formas de Usar APiGen

### 1. Como Libreria (Recomendado)

```groovy
dependencies {
    implementation platform('com.jnzader:apigen-bom:1.0.0-SNAPSHOT')
    implementation 'com.jnzader:apigen-core'
    implementation 'com.jnzader:apigen-security'
}
```

### 2. Como Template de GitHub

1. Click "Use this template" en GitHub
2. Crea tu repositorio
3. Personaliza segun necesites

### 3. Clonar apigen-example

```bash
git clone https://github.com/jnzader/apigen.git
cp -r apigen/apigen-example mi-proyecto
```

### 4. Generar desde SQL

```bash
java -jar apigen-codegen.jar schema.sql ./output com.mycompany
```

## Estructura de Directorios

```
apigen/
├── apigen-core/                      # LIBRERIA PRINCIPAL
│   ├── src/main/java/.../core/
│   │   ├── domain/
│   │   │   ├── entity/Base.java
│   │   │   ├── repository/BaseRepository.java
│   │   │   └── specification/FilterSpecificationBuilder.java
│   │   ├── application/
│   │   │   ├── dto/BaseDTO.java
│   │   │   ├── mapper/BaseMapper.java
│   │   │   └── service/BaseServiceImpl.java
│   │   └── infrastructure/
│   │       ├── config/ApigenCoreAutoConfiguration.java
│   │       ├── controller/BaseControllerImpl.java
│   │       └── hateoas/BaseResourceAssembler.java
│   └── build.gradle
│
├── apigen-security/                  # MODULO DE SEGURIDAD
│   ├── src/main/java/.../security/
│   │   ├── domain/entity/{User,Role,RefreshToken}.java
│   │   ├── application/service/{AuthService,JwtService}.java
│   │   └── infrastructure/
│   │       ├── config/ApigenSecurityAutoConfiguration.java
│   │       └── controller/AuthController.java
│   └── build.gradle
│
├── apigen-codegen/                   # GENERADOR DE CODIGO
│   ├── src/main/java/.../codegen/
│   │   ├── SqlParser.java
│   │   └── CodeGenerator.java
│   └── build.gradle
│
├── apigen-bom/                       # BILL OF MATERIALS
│   └── build.gradle
│
├── apigen-example/                   # APLICACION DE EJEMPLO
│   ├── src/main/java/com/jnzader/example/
│   │   ├── ExampleApplication.java
│   │   ├── domain/entity/Product.java
│   │   ├── application/
│   │   │   ├── dto/ProductDTO.java
│   │   │   ├── mapper/ProductMapper.java
│   │   │   └── service/ProductService.java
│   │   └── infrastructure/
│   │       ├── controller/ProductController.java
│   │       └── hateoas/ProductResourceAssembler.java
│   ├── src/main/resources/
│   │   ├── application.yaml
│   │   └── db/migration/
│   └── build.gradle
│
├── docs/                             # Documentacion de uso
│   ├── USAGE_GUIDE.md
│   ├── USAGE_GUIDE_LIBRARY.md
│   ├── USAGE_GUIDE_EXAMPLE.md
│   ├── USAGE_GUIDE_TEMPLATE.md
│   ├── USAGE_GUIDE_CODEGEN.md
│   └── FEATURES.md
│
├── docs-didacticos/                  # Documentacion didactica
│
├── .github/
│   ├── workflows/ci.yml
│   ├── ISSUE_TEMPLATE/
│   └── PULL_REQUEST_TEMPLATE.md
│
├── docker-compose.yml
├── build.gradle                      # Config multi-modulo
├── settings.gradle
└── README.md
```

## Principios de Diseno

### 1. Convention over Configuration

Todo tiene valores por defecto sensatos. Solo configuras lo que necesitas cambiar.

### 2. DRY (Don't Repeat Yourself)

El codigo generico esta en clases Base. Tus entidades solo definen lo especifico.

### 3. Fail-Fast

Validaciones tempranas. Si algo esta mal, falla rapido con mensajes claros.

### 4. Explicit Error Handling

Result Pattern en lugar de excepciones. El error es parte del tipo de retorno.

### 5. Separation of Concerns

Cada capa tiene una responsabilidad clara. No mezclar logica de negocio con infraestructura.

## Casos de Uso Ideales

APiGen es perfecto para:

- **APIs REST empresariales** con necesidades de auditoria
- **Microservicios** que necesitan estructura consistente
- **MVPs rapidos** con features robustas
- **Proyectos de aprendizaje** de arquitectura DDD
- **Equipos** que quieren estandarizar su stack

## Lo que NO es APiGen

- **No es un framework:** Es una libreria que extiendes
- **No es opinionated al 100%:** Puedes cambiar lo que quieras
- **No es solo para Java 25:** Funciona en Java 21+

## Proximos Pasos

1. Lee `01-SETUP-INICIAL.md` para configurar tu entorno
2. Explora `apigen-example` para ver todo funcionando
3. Sigue los documentos en orden para entender cada componente

---

**Version:** 3.0.0 (Multi-modulo)
**Ultima actualizacion:** Enero 2025
**Licencia:** MIT
**Autor:** jnzader
