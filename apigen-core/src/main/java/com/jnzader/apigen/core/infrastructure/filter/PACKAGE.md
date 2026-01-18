# Filters

Este paquete contiene **filtros HTTP** que interceptan requests para funcionalidad transversal.

## Archivos

| Archivo | Descripción |
|---------|-------------|
| `RequestLoggingFilter.java` | Log de requests/responses |
| `RequestIdFilter.java` | Genera X-Request-Id único |

## RequestLoggingFilter

Registra información de cada request para debugging y monitoreo:

```java
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) {
        long startTime = System.currentTimeMillis();
        String requestId = request.getHeader("X-Request-Id");

        MDC.put("requestId", requestId);
        MDC.put("method", request.getMethod());
        MDC.put("uri", request.getRequestURI());

        try {
            chain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;

            log.info("Request completed: {} {} -> {} ({}ms)",
                request.getMethod(),
                request.getRequestURI(),
                response.getStatus(),
                duration);

            MDC.clear();
        }
    }
}
```

**Log resultante:**
```json
{
  "timestamp": "2024-01-15T10:30:00Z",
  "level": "INFO",
  "message": "Request completed: GET /api/v1/products/1 -> 200 (45ms)",
  "requestId": "abc-123",
  "method": "GET",
  "uri": "/api/v1/products/1"
}
```

## RequestIdFilter

Genera o propaga ID único para tracing:

```java
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RequestIdFilter extends OncePerRequestFilter {

    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) {
        String requestId = request.getHeader(REQUEST_ID_HEADER);

        if (requestId == null || requestId.isEmpty()) {
            requestId = UUID.randomUUID().toString();
        }

        // Agregar a response para el cliente
        response.setHeader(REQUEST_ID_HEADER, requestId);

        // Agregar a MDC para logs
        MDC.put("requestId", requestId);

        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove("requestId");
        }
    }
}
```

**Uso del request ID:**
1. Cliente puede enviar su propio ID: `X-Request-Id: client-123`
2. Si no envía, servidor genera: `X-Request-Id: uuid-abc`
3. Response siempre incluye el ID usado
4. Logs incluyen el ID para correlación

## Orden de Filtros

```
Request
   │
   ▼
┌─────────────────────┐
│ RequestIdFilter     │  1. Genera/propaga ID
│ (Highest + 1)       │
└─────────────────────┘
   │
   ▼
┌─────────────────────┐
│ RequestLoggingFilter│  2. Inicia timer, log inicio
│ (Highest)           │
└─────────────────────┘
   │
   ▼
┌─────────────────────┐
│ SecurityFilter      │  3. Autenticación JWT
│ (Spring Security)   │
└─────────────────────┘
   │
   ▼
┌─────────────────────┐
│ Controller          │  4. Procesa request
└─────────────────────┘
   │
   ▼
Response (logs duración)
```

## Crear Filtro Custom

```java
@Component
@Order(50)  // Después de security
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimiter rateLimiter;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws IOException, ServletException {

        String clientIp = getClientIp(request);

        if (!rateLimiter.tryAcquire(clientIp)) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.getWriter().write("Rate limit exceeded");
            return;
        }

        chain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // No aplicar a health checks
        return request.getRequestURI().startsWith("/actuator");
    }
}
```

## MDC (Mapped Diagnostic Context)

MDC permite agregar contexto a todos los logs:

```java
// En filtro
MDC.put("userId", getUserId());
MDC.put("tenantId", getTenantId());

// En cualquier parte del código
log.info("Processing order");  // Automáticamente incluye userId, tenantId

// Limpiar al final
MDC.clear();
```

**Configurar en logback:**
```xml
<pattern>%d{ISO8601} [%X{requestId}] [%X{userId}] %msg%n</pattern>
```

## Testing

```java
@WebMvcTest
class RequestLoggingFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldAddRequestIdToResponse() throws Exception {
        mockMvc.perform(get("/api/v1/products"))
            .andExpect(header().exists("X-Request-Id"));
    }

    @Test
    void shouldPropagateClientRequestId() throws Exception {
        mockMvc.perform(get("/api/v1/products")
                .header("X-Request-Id", "client-123"))
            .andExpect(header().string("X-Request-Id", "client-123"));
    }
}
```

## Buenas Prácticas

1. **OncePerRequestFilter** - Garantiza ejecución única
2. **Orden explícito** - @Order o Ordered interface
3. **MDC para contexto** - No pasar parámetros manualmente
4. **Limpiar MDC en finally** - Evitar leaks entre requests
5. **shouldNotFilter()** - Excluir rutas que no necesitan el filtro
