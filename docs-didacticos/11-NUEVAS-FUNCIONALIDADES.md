# Nuevas Funcionalidades de APiGen

Este documento cubre las 5 nuevas funcionalidades implementadas:

1. **Cursor-based Pagination** - Paginación eficiente para datasets grandes
2. **Entity Generator con Relaciones** - Soporte para @ManyToOne, @OneToMany, etc.
3. **OAuth2 Resource Server** - Integración con IdPs externos (Auth0, Keycloak, Azure AD)
4. **Contract Testing** - Spring Cloud Contract para tests de contrato
5. **Server-Sent Events (SSE)** - Eventos en tiempo real

---

## 1. Cursor-based Pagination

### ¿Por qué usar Cursor en lugar de Offset?

| Offset Pagination | Cursor Pagination |
|-------------------|-------------------|
| `OFFSET 10000` escanea 10K filas | Siempre O(1) sin importar la página |
| Puede perder/duplicar registros | Consistente ante cambios |
| Ideal para páginas pequeñas | Ideal para scroll infinito |

### Uso del Endpoint

```bash
# Primera página
GET /api/v1/products/cursor?size=20&sort=id&direction=DESC

# Respuesta incluye nextCursor
{
  "content": [...],
  "pageInfo": {
    "size": 20,
    "hasNext": true,
    "hasPrevious": false,
    "nextCursor": "eyJpZCI6MTAwLCJzb3J0IjoiaWQiLCJ2YWx1ZSI6IjEwMCIsImRpciI6IkRFU0MifQ==",
    "prevCursor": null
  }
}

# Siguiente página (usar nextCursor)
GET /api/v1/products/cursor?cursor=eyJpZCI6MTAwLCJzb3J0IjoiaWQiLCJ2YWx1ZSI6IjEwMCIsImRpciI6IkRFU0MifQ==&size=20
```

### Uso Programático

```java
// En servicio
CursorPageRequest request = new CursorPageRequest(null, 20, "id", SortDirection.DESC);
Result<CursorPageResponse<Product>, Exception> result = productService.findAllWithCursor(request);

result.onSuccess(page -> {
    List<Product> products = page.content();
    String nextCursor = page.pageInfo().nextCursor();
    boolean hasMore = page.pageInfo().hasNext();
});
```

### Archivos Relacionados

- `core/application/dto/pagination/CursorPageRequest.java`
- `core/application/dto/pagination/CursorPageResponse.java`
- `core/application/service/BaseServiceImpl.java` (métodos `findAllWithCursor`)
- `core/infrastructure/controller/BaseControllerImpl.java` (endpoint `/cursor`)

---

## 2. Entity Generator con Relaciones

### Uso Básico

```bash
# Entidad simple (sin relaciones)
./gradlew generateEntity -Pname=Product -Pmodule=products -Pfields=name:string,price:decimal

# Entidad con relaciones
./gradlew generateEntity \
  -Pname=Order \
  -Pmodule=orders \
  -Pfields=orderNumber:string,total:decimal \
  -Prelations=customer:Customer:ManyToOne:customers,items:OrderItem:OneToMany:orderitems
```

### Formato de Relaciones

```
-Prelations=campo:Entidad:TipoRelacion:modulo
```

| Tipo de Relación | Ejemplo | En Entity | En DTO |
|------------------|---------|-----------|--------|
| `ManyToOne` | `category:Category:ManyToOne:categories` | `@ManyToOne Category category` | `Long categoryId` |
| `OneToMany` | `items:OrderItem:OneToMany:orderitems` | `@OneToMany List<OrderItem> items` | `List<Long> itemsIds` |
| `ManyToMany` | `tags:Tag:ManyToMany:tags` | `@ManyToMany List<Tag> tags` | `List<Long> tagsIds` |
| `OneToOne` | `profile:Profile:OneToOne:profiles` | `@OneToOne Profile profile` | `Long profileId` |

### Ejemplo Completo: Sistema de Productos con Categorías y Tags

```bash
# 1. Crear Category primero (no tiene dependencias)
./gradlew generateEntity \
  -Pname=Category \
  -Pmodule=categories \
  -Pfields=name:string,description:string

# 2. Crear Tag (no tiene dependencias)
./gradlew generateEntity \
  -Pname=Tag \
  -Pmodule=tags \
  -Pfields=name:string,color:string

# 3. Crear Product con relaciones a Category y Tags
./gradlew generateEntity \
  -Pname=Product \
  -Pmodule=products \
  -Pfields=name:string,price:decimal,stock:integer \
  -Prelations=category:Category:ManyToOne:categories,tags:Tag:ManyToMany:tags
```

### Archivos Generados

Para `Product` con relaciones:

```
src/main/java/com/jnzader/apigen/products/
├── domain/
│   └── entity/Product.java           # Con @ManyToOne y @ManyToMany
├── application/
│   ├── dto/ProductDTO.java           # Con categoryId y tagsIds
│   ├── mapper/ProductMapper.java     # Con @Mapping para relaciones
│   └── service/ProductService*.java
├── infrastructure/
│   ├── repository/ProductRepository.java
│   └── controller/ProductController*.java
└── src/main/resources/db/migration/
    └── V2__create_products_table.sql  # Con FK y junction tables
```

### SQL Generado (Ejemplo)

```sql
CREATE TABLE products (
    id BIGINT PRIMARY KEY,
    name VARCHAR(255),
    price DECIMAL(10, 2),
    stock INTEGER,
    category_id BIGINT,  -- FK para ManyToOne
    -- campos de Base...
);

-- Junction table para ManyToMany
CREATE TABLE products_tags (
    product_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    PRIMARY KEY (product_id, tag_id),
    CONSTRAINT fk_products_tags_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT fk_products_tags_tag FOREIGN KEY (tag_id) REFERENCES tags(id)
);

-- Foreign Key para ManyToOne
ALTER TABLE products ADD CONSTRAINT fk_products_category
    FOREIGN KEY (category_id) REFERENCES categories(id);
```

---

## 3. OAuth2 Resource Server

### Modos de Autenticación

APiGen soporta dos modos de autenticación:

| Modo | Descripción | Caso de Uso |
|------|-------------|-------------|
| `jwt` | JWT propio con secret compartido (HS256) | Aplicaciones standalone |
| `oauth2` | OAuth2 Resource Server (RS256) | Integración con IdP externo |

### Configuración JWT Propio (Default)

```yaml
apigen:
  security:
    enabled: true
    mode: jwt  # Valor por defecto
    jwt:
      secret: ${JWT_SECRET}  # Mínimo 256 bits
      expiration-minutes: 15
      refresh-expiration-minutes: 10080
```

### Configuración OAuth2 (Auth0, Keycloak, Azure AD)

```yaml
apigen:
  security:
    enabled: true
    mode: oauth2
    oauth2:
      # Auth0
      issuer-uri: https://your-tenant.auth0.com/
      audience: your-api-identifier
      roles-claim: permissions

      # Keycloak
      # issuer-uri: https://keycloak.example.com/realms/your-realm
      # roles-claim: realm_access.roles

      # Azure AD
      # issuer-uri: https://login.microsoftonline.com/{tenant}/v2.0
      # roles-claim: roles
```

### IdPs Soportados

| IdP | issuer-uri | roles-claim |
|-----|------------|-------------|
| **Auth0** | `https://your-tenant.auth0.com/` | `permissions` |
| **Keycloak** | `https://keycloak/realms/realm` | `realm_access.roles` |
| **Azure AD** | `https://login.microsoftonline.com/{tenant}/v2.0` | `roles` |
| **AWS Cognito** | `https://cognito-idp.{region}.amazonaws.com/{pool}` | `cognito:groups` |
| **Okta** | `https://your-domain.okta.com` | `groups` |

### Extracción de Roles

El sistema extrae roles automáticamente de diferentes estructuras de claims:

```java
// Auth0: array simple
{"permissions": ["read:products", "write:products"]}

// Keycloak: objeto anidado
{"realm_access": {"roles": ["admin", "user"]}}

// Azure AD: array simple
{"roles": ["Admin.Read", "Admin.Write"]}

// Cognito: grupos
{"cognito:groups": ["admins", "users"]}
```

### Archivos Relacionados

- `security/infrastructure/config/OAuth2SecurityConfig.java`
- `security/infrastructure/config/SecurityProperties.java`

---

## 4. Contract Testing con Spring Cloud Contract

### ¿Qué es Contract Testing?

Los tests de contrato verifican que:
- **Provider** (API): Cumple con el contrato definido
- **Consumer** (Cliente): Puede usar stubs del contrato para tests

### Estructura de Contratos

```
src/test/resources/contracts/
├── health/
│   └── shouldReturnHealthStatus.groovy
└── api/
    ├── shouldReturnErrorForNotFound.groovy
    └── shouldReturnValidationError.groovy
```

### Ejemplo de Contrato (Groovy DSL)

```groovy
// src/test/resources/contracts/products/shouldCreateProduct.groovy
import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Should create a new product"

    request {
        method POST()
        url "/api/v1/products"
        headers {
            contentType(applicationJson())
        }
        body([
            name: "Laptop",
            price: 999.99
        ])
    }

    response {
        status CREATED()
        headers {
            contentType(applicationJson())
        }
        body([
            id: $(consumer(anyNumber()), producer(1)),
            name: "Laptop",
            price: 999.99
        ])
    }
}
```

### Comandos Gradle

```bash
# Generar tests a partir de contratos
./gradlew generateContractTests

# Ejecutar tests de contrato
./gradlew contractTest

# Generar stubs para consumidores
./gradlew generateClientStubs

# Publicar stubs al repo local Maven
./gradlew publishStubsPublicationToMavenLocal
```

### Usar Stubs en Tests de Consumidor

```java
@SpringBootTest
@AutoConfigureStubRunner(
    ids = "com.jnzader:apigen:+:stubs:8090",
    stubsMode = StubRunnerProperties.StubsMode.LOCAL
)
class ConsumerTest {

    @Test
    void shouldCallProductsApi() {
        // El stub está disponible en localhost:8090
        ResponseEntity<String> response = restTemplate
            .getForEntity("http://localhost:8090/api/v1/products/1", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
```

### Archivos Relacionados

- `build.gradle` (configuración `contracts {}`)
- `src/test/java/com/jnzader/apigen/contracts/ContractTestBase.java`
- `src/test/java/com/jnzader/apigen/contracts/ContractStubRunner.java`
- `src/test/resources/contracts/**/*.groovy`

---

## 5. Server-Sent Events (SSE)

### ¿Qué son los SSE?

Server-Sent Events permiten enviar datos del servidor al cliente en tiempo real:

| Característica | SSE | WebSocket |
|----------------|-----|-----------|
| Dirección | Server → Client | Bidireccional |
| Protocolo | HTTP | WS/WSS |
| Reconexión | Automática | Manual |
| Complejidad | Baja | Alta |
| Caso de uso | Notificaciones, feeds | Chat, gaming |

### Suscribirse a Eventos (Cliente JavaScript)

```javascript
// Conectar a un tópico
const eventSource = new EventSource('/api/v1/events/orders');

// Evento de conexión establecida
eventSource.addEventListener('connected', (event) => {
    const data = JSON.parse(event.data);
    console.log('Connected! Client ID:', data.clientId);
});

// Escuchar eventos de negocio
eventSource.addEventListener('order.created', (event) => {
    const order = JSON.parse(event.data);
    console.log('New order:', order);
    showNotification(`Nuevo pedido #${order.id}`);
});

// Heartbeat (para debugging)
eventSource.addEventListener('heartbeat', (event) => {
    console.log('Server alive:', event.data);
});

// Manejo de errores
eventSource.onerror = (error) => {
    console.error('SSE Error:', error);
    // El navegador reintentará automáticamente
};

// Cerrar conexión
eventSource.close();
```

### Publicar Eventos (Servidor)

```java
@Service
public class OrderService {

    private final SseEventPublisher ssePublisher;
    private final ApplicationEventPublisher eventPublisher;

    public Order createOrder(OrderDTO dto) {
        Order order = repository.save(mapToEntity(dto));

        // Opción 1: Publicar evento de dominio (se envía automáticamente via SSE)
        eventPublisher.publishEvent(new OrderCreatedEvent(order));

        // Opción 2: Publicar directamente via SSE
        ssePublisher.publish("orders", "order.created", order);

        // Opción 3: Enviar a cliente específico
        ssePublisher.publishToClient(order.getCustomerId(), "your.order", order);

        return order;
    }
}
```

### Endpoints Disponibles

```bash
# Suscribirse a un tópico
GET /api/v1/events/{topic}
# Ejemplo: GET /api/v1/events/orders

# Suscribirse con client ID específico
GET /api/v1/events/{topic}?clientId=user-123

# Ver estadísticas de conexiones
GET /api/v1/events/stats
GET /api/v1/events/stats?topic=orders
```

### Configuración

```yaml
apigen:
  sse:
    heartbeat:
      enabled: true        # Enviar heartbeats periódicos
      interval: 30000      # Cada 30 segundos
```

### Integración con Domain Events

Los eventos de dominio se envían automáticamente via SSE:

```java
// Cuando se publica este evento...
eventPublisher.publishEvent(new OrderCreatedEvent(order));

// ...se envía automáticamente a los suscriptores del tópico "orders"
// con eventName = "order.created"
```

### Convención de Nombres

| Clase de Evento | Tópico SSE | Nombre de Evento |
|-----------------|------------|------------------|
| `OrderCreatedEvent` | `orders` | `order.created` |
| `ProductUpdatedEvent` | `products` | `product.updated` |
| `UserRegisteredEvent` | `users` | `user.registered` |

### Archivos Relacionados

- `core/infrastructure/sse/SseEmitterService.java` - Gestión de conexiones
- `core/infrastructure/sse/SseController.java` - Endpoints REST
- `core/infrastructure/sse/SseEventPublisher.java` - Integración con Domain Events
- `core/infrastructure/sse/SseHeartbeatScheduler.java` - Keepalive

---

## Resumen de Comandos Nuevos

```bash
# Cursor Pagination
GET /api/v1/{resource}/cursor?size=20&sort=id&direction=DESC

# Entity Generator con Relaciones
./gradlew generateEntity -Pname=X -Pmodule=x \
  -Pfields=... -Prelations=campo:Entidad:Tipo:modulo

# Contract Testing
./gradlew contractTest
./gradlew generateContractTests

# SSE
GET /api/v1/events/{topic}
GET /api/v1/events/stats
```

## Configuración Completa (application.yaml)

```yaml
apigen:
  security:
    enabled: true
    mode: jwt  # o 'oauth2'
    jwt:
      secret: ${JWT_SECRET}
      expiration-minutes: 15
    oauth2:
      issuer-uri: ${OAUTH2_ISSUER_URI:}
      audience: ${OAUTH2_AUDIENCE:}
      roles-claim: permissions

  sse:
    heartbeat:
      enabled: true
      interval: 30000
```

---

**Última actualización:** Diciembre 2024
**Versión:** 2.0.0
