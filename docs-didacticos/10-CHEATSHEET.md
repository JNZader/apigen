# 10 - Cheatsheet de Referencia Rapida

## Estructura Multi-Modulo

```
apigen/
├── apigen-core/        # Libreria base (Entity, Repository, Service, Controller)
├── apigen-security/    # Seguridad JWT (opcional)
├── apigen-codegen/     # Generador de codigo desde SQL
├── apigen-bom/         # Bill of Materials
└── apigen-example/     # Aplicacion de ejemplo
```

## Comandos Gradle Mas Usados

### Multi-Modulo

```bash
# Compilar TODO el proyecto
./gradlew clean build

# Compilar solo un modulo
./gradlew :apigen-core:build
./gradlew :apigen-security:build
./gradlew :apigen-example:build

# Ejecutar tests de un modulo
./gradlew :apigen-core:test
./gradlew :apigen-example:test

# Ejecutar la aplicacion de ejemplo
./gradlew :apigen-example:bootRun

# Ejecutar con perfil dev (H2 en memoria)
./gradlew :apigen-example:bootRun --args='--spring.profiles.active=dev'

# Publicar a Maven Local (para usar como dependencia)
./gradlew publishToMavenLocal

# Ver dependencias de un modulo
./gradlew :apigen-core:dependencies
```

### Desarrollo Rapido

```bash
# Compilar sin tests (mas rapido)
./gradlew clean build -x test

# Limpiar y reconstruir
./gradlew clean build

# Solo compilar (sin JAR)
./gradlew compileJava
```

### Generacion de Codigo (apigen-codegen)

```bash
# Generar desde SQL
java -jar apigen-codegen/build/libs/apigen-codegen.jar schema.sql ./output com.mycompany

# Ejemplo
java -jar apigen-codegen.jar create_products.sql ./src com.example.store
```

## Docker

### Docker Compose

```bash
# Iniciar todos los servicios (app + postgres + pgadmin + prometheus + grafana)
docker-compose up -d

# Iniciar solo postgres
docker-compose up -d postgres

# Iniciar con monitoring
docker-compose --profile monitoring up -d

# Ver logs
docker-compose logs -f apigen

# Detener todos los servicios
docker-compose down

# Detener y eliminar volumenes (CUIDADO: borra datos)
docker-compose down -v

# Rebuild de la app
docker-compose up -d --build apigen
```

### Docker Build Manual

```bash
# Build de imagen
docker build -t apigen:latest .

# Run manual
docker run -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=dev \
  -e DB_URL=jdbc:postgresql://host.docker.internal:5432/apigen \
  apigen:latest
```

## URLs Importantes

### Desarrollo Local

```
App:            http://localhost:8080
Health Check:   http://localhost:8080/actuator/health
Swagger UI:     http://localhost:8080/swagger-ui.html
API Docs JSON:  http://localhost:8080/v3/api-docs
Prometheus:     http://localhost:8080/actuator/prometheus
SSE Events:     http://localhost:8080/api/v1/events/{topic}
SSE Stats:      http://localhost:8080/api/v1/events/stats

PostgreSQL:     localhost:5432 (user: apigen_user, pass: apigen_password)
pgAdmin:        http://localhost:5050 (user: admin@apigen.local, pass: admin123)
Prometheus:     http://localhost:9090
Grafana:        http://localhost:3000 (user: admin, pass: admin123)
```

## Estructura de Proyecto Multi-Modulo

```
apigen/
├── apigen-core/                      # LIBRERIA BASE
│   └── src/main/java/com/jnzader/apigen/core/
│       ├── domain/
│       │   ├── entity/Base.java         # ⭐ Entidad base
│       │   ├── repository/BaseRepository.java
│       │   ├── event/DomainEvent.java
│       │   └── specification/FilterSpecificationBuilder.java
│       ├── application/
│       │   ├── dto/BaseDTO.java
│       │   ├── mapper/BaseMapper.java
│       │   └── service/BaseServiceImpl.java  # ⭐ Servicio base
│       └── infrastructure/
│           ├── controller/BaseControllerImpl.java  # ⭐ Controller base
│           ├── config/ApigenCoreAutoConfiguration.java  # Auto-config
│           ├── exception/GlobalExceptionHandler.java
│           └── hateoas/BaseResourceAssembler.java
│
├── apigen-security/                  # MODULO DE SEGURIDAD (opcional)
│   └── src/main/java/com/jnzader/apigen/security/
│       ├── domain/entity/{User,Role,Permission}.java
│       ├── application/service/{AuthService,JwtService}.java
│       └── infrastructure/
│           ├── config/ApigenSecurityAutoConfiguration.java
│           └── controller/AuthController.java
│
├── apigen-codegen/                   # GENERADOR DE CODIGO
│   └── src/main/java/.../codegen/
│       ├── SqlParser.java
│       └── CodeGenerator.java
│
└── apigen-example/                   # APLICACION DE EJEMPLO
    └── src/main/java/com/jnzader/example/
        ├── domain/entity/Product.java
        ├── application/
        │   ├── dto/ProductDTO.java
        │   ├── mapper/ProductMapper.java
        │   └── service/ProductService.java
        └── infrastructure/
            ├── controller/ProductController.java
            └── hateoas/ProductResourceAssembler.java
```

## Patrones de Codigo

### Usar APiGen como Dependencia

```groovy
// build.gradle de tu proyecto
dependencies {
    implementation platform('com.jnzader:apigen-bom:1.0.0-SNAPSHOT')
    implementation 'com.jnzader:apigen-core'
    implementation 'com.jnzader:apigen-security'  // Opcional
}
```

### Crear Entidad Nueva (7 archivos)

```java
// 1. Entity (extiende de Base)
package com.jnzader.example.domain.entity;

import com.jnzader.apigen.core.domain.entity.Base;

@Entity
@Table(name = "products")
public class Product extends Base {
    @Column(nullable = false)
    private String name;

    @Column(precision = 10, scale = 2)
    private BigDecimal price;
}

// 2. DTO (record con validaciones)
package com.jnzader.example.application.dto;

public record ProductDTO(
    Long id,
    @NotBlank String name,
    @Positive BigDecimal price
) implements BaseDTO {}

// 3. Mapper (MapStruct)
package com.jnzader.example.application.mapper;

@Mapper(componentModel = "spring")
public interface ProductMapper extends BaseMapper<Product, ProductDTO> {}

// 4. Repository (extiende BaseRepository)
package com.jnzader.example.domain.repository;

@Repository
public interface ProductRepository extends BaseRepository<Product, Long> {
    List<Product> findByNameContainingIgnoreCase(String name);
}

// 5. Service
package com.jnzader.example.application.service;

@Service
public class ProductService extends BaseServiceImpl<Product, Long> {
    @Override
    protected Class<Product> getEntityClass() { return Product.class; }

    @Override
    public String getEntityName() { return "Product"; }
}

// 6. ResourceAssembler
package com.jnzader.example.infrastructure.hateoas;

@Component
public class ProductResourceAssembler extends BaseResourceAssembler<ProductDTO, Long> {
    // Hereda generacion automatica de links HATEOAS
}

// 7. Controller
package com.jnzader.example.infrastructure.controller;

@RestController
@RequestMapping("/api/products")
public class ProductController extends BaseControllerImpl<Product, ProductDTO, Long> {
    // Hereda 12+ endpoints CRUD automaticamente
}
```

### Uso del Result Pattern

```java
// Servicio retorna Result
public Result<Product, Exception> findById(Long id) {
    return baseRepository.findById(id)
        .map(Result::success)
        .orElseGet(() -> Result.failure(new ResourceNotFoundException("Product not found")));
}

// Controller consume Result
return productService.findById(id).fold(
    product -> ResponseEntity.ok(mapper.toDTO(product)),  // Success
    error -> handleFailure(error)  // Failure
);

// Encadenar operaciones
return productService.findById(id)
    .map(product -> product.getName())
    .map(String::toUpperCase)
    .onSuccess(name -> log.info("Product: {}", name))
    .onFailure(error -> log.error("Error: {}", error))
    .orElse("UNKNOWN");
```

### Cache

```java
// Cache en Service
@Cacheable(value = "entities", key = "#root.target.entityName + ':' + #id")
public Result<Product, Exception> findById(Long id) {
    // Solo se ejecuta si no esta en cache
    return baseRepository.findById(id)...;
}

// Evict cache
@CacheEvict(value = "entities", key = "#root.target.entityName + ':' + #id")
public Result<Void, Exception> softDelete(Long id) {
    // Elimina del cache despues de soft delete
}
```

### Resiliencia (Resilience4j)

```java
@CircuitBreaker(name = "database")
@Retry(name = "database", fallbackMethod = "findByIdFallback")
@RateLimiter(name = "public-api")
public Result<Product, Exception> findById(Long id) {
    return baseRepository.findById(id)...;
}

// Fallback si falla
public Result<Product, Exception> findByIdFallback(Long id, Exception ex) {
    log.error("Fallback activado para product {}", id, ex);
    return Result.failure(ex);
}
```

## Configuracion de Perfiles

### application.yaml (Base)

```yaml
spring:
  application:
    name: apigen-example

  threads:
    virtual:
      enabled: true  # Virtual Threads (Java 21+)

apigen:
  core:
    enabled: true  # Siempre true

  security:
    enabled: true  # true = JWT habilitado
    jwt:
      secret: ${JWT_SECRET}  # REQUERIDO en produccion
      expiration-minutes: 15
      refresh-expiration-minutes: 10080  # 7 dias
```

### application-dev.yaml (Desarrollo)

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:devdb  # H2 en memoria
    driver-class-name: org.h2.Driver

  h2:
    console:
      enabled: true
      path: /h2-console

  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true

apigen:
  security:
    enabled: false  # Deshabilitado en dev

logging:
  level:
    com.jnzader: DEBUG
```

### application-prod.yaml (Produccion)

```yaml
spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}

  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false

apigen:
  security:
    enabled: true  # SIEMPRE habilitado en prod
    jwt:
      secret: ${JWT_SECRET}  # Variable de entorno

logging:
  level:
    root: WARN
    com.jnzader: INFO
```

## Variables de Entorno

```bash
# Base de datos
export DB_URL=jdbc:postgresql://localhost:5432/apigen
export DB_USERNAME=apigen_user
export DB_PASSWORD=apigen_password

# JWT
export JWT_SECRET=$(openssl rand -base64 64)
export JWT_EXPIRATION_MINUTES=15
export JWT_REFRESH_EXPIRATION_MINUTES=10080

# Tracing (opcional)
export TRACING_ENABLED=true
export OTLP_ENDPOINT=http://localhost:4317

# CORS
export CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:4200
```

## Queries Utiles

### Soft Delete

```java
// Soft delete
productService.softDelete(1L, "admin");

// Verificar que NO aparece en findAll()
List<Product> all = productService.findAll().orElse(List.of());  // Vacia

// Restaurar
productService.restore(1L);

// Hard delete (CUIDADO: irreversible)
productService.hardDelete(1L);
```

### Filtrado Dinamico

```bash
# GET con filtros
curl "http://localhost:8080/api/v1/products?filter=name:like:laptop,price:gte:500"

# Operadores soportados:
# eq, neq, like, starts, ends, gt, gte, lt, lte, in, between, null, notnull
```

### Paginacion Offset (Tradicional)

```bash
# GET con paginacion offset
curl "http://localhost:8080/api/v1/products?page=0&size=20&sort=name,asc"

# Headers de respuesta:
# X-Total-Count: 150
# X-Page-Number: 0
# X-Page-Size: 20
# X-Total-Pages: 8
```

### Paginacion Cursor (Para datasets grandes)

```bash
# Primera pagina
curl "http://localhost:8080/api/v1/products/cursor?size=20&sort=id&direction=DESC"

# Respuesta incluye nextCursor:
# {
#   "content": [...],
#   "pageInfo": {
#     "size": 20,
#     "hasNext": true,
#     "nextCursor": "eyJpZCI6MTAwLCJzb3J0IjoiaWQiLC..."
#   }
# }

# Siguiente pagina (usar nextCursor)
curl "http://localhost:8080/api/v1/products/cursor?cursor=eyJpZCI6MTAwLCJzb3J0IjoiaWQiLC...&size=20"
```

### Server-Sent Events (SSE)

```bash
# Suscribirse a eventos de un topico
curl -N "http://localhost:8080/api/v1/events/orders"

# Suscribirse con client ID especifico
curl -N "http://localhost:8080/api/v1/events/orders?clientId=user-123"

# Ver estadisticas de conexiones
curl "http://localhost:8080/api/v1/events/stats"
curl "http://localhost:8080/api/v1/events/stats?topic=orders"
```

## Troubleshooting

### Error: "Failed to configure a DataSource"

```bash
# Solucion: Asegurate de que PostgreSQL esta corriendo
docker-compose up -d postgres
```

### Error: "OptimisticLockException"

```java
// Causa: Dos usuarios modificaron simultaneamente
// Solucion: Recargar entidad y reintentar

try {
    service.update(id, entity);
} catch (OptimisticLockException ex) {
    // Recargar y reintentar
    entity = service.findById(id).orElseThrow();
    service.update(id, entity);
}
```

### Error: "JWT signature does not match"

```bash
# Causa: JWT_SECRET incorrecto o cambiado
# Solucion: Asegurate de usar el mismo secret

export JWT_SECRET=$(cat .env | grep JWT_SECRET | cut -d '=' -f2)
```

### Error: "Circuit Breaker OPEN"

```bash
# Causa: Demasiados fallos consecutivos
# Solucion: Esperar 60s (waitDurationInOpenState) o arreglar causa raiz

# Ver estado en Actuator:
curl http://localhost:8080/actuator/circuitbreakers
```

## Testing

### Test Unitario

```java
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository repository;

    @InjectMocks
    private ProductServiceImpl service;

    @Test
    void shouldFindById() {
        when(repository.findById(1L)).thenReturn(Optional.of(product));

        Result<Product, Exception> result = service.findById(1L);

        assertTrue(result.isSuccess());
        verify(repository).findById(1L);
    }
}
```

### Test de Integracion (TestContainers)

```java
@SpringBootTest
@Testcontainers
@Tag("integration")
class ProductIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Test
    void shouldSaveProduct() {
        Product product = repository.save(new Product());
        assertNotNull(product.getId());
    }
}
```

## Endpoints Heredados de BaseControllerImpl

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

---

**Anterior:** [09-GENERADORES.md](./09-GENERADORES.md)
**Siguiente:** [11-NUEVAS-FUNCIONALIDADES.md](./11-NUEVAS-FUNCIONALIDADES.md)

---

**Version:** 3.0.0 (Multi-modulo)
**Ultima actualizacion:** Enero 2025
