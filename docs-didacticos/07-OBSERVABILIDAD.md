# 07 - OBSERVABILIDAD COMPLETA

## Tabla de Contenidos
- [Introduccion](#introduccion)
- [1. Logging Estructurado](#1-logging-estructurado)
- [2. Metricas con Micrometer/Prometheus](#2-metricas-con-micrometerprometheus)
- [3. Tracing Distribuido con OpenTelemetry](#3-tracing-distribuido-con-opentelemetry)
- [4. Dashboards Grafana](#4-dashboards-grafana)
- [5. Health Checks](#5-health-checks)
- [6. Integracion Completa](#6-integracion-completa)
- [Buenas Practicas](#buenas-practicas)
- [Troubleshooting](#troubleshooting)

---

## Introduccion

**APiGen** implementa un sistema completo de **Observabilidad** basado en los **Tres Pilares**:

1. **Logs** - Registro detallado de eventos
2. **Metrics** - Mediciones cuantitativas del sistema
3. **Traces** - Seguimiento de transacciones distribuidas

### Arquitectura de Observabilidad

```
┌─────────────────────────────────────────────────────────────┐
│                      APiGen Application                      │
│                                                               │
│  ┌──────────────┐  ┌──────────────┐  ┌───────────────┐     │
│  │   Logging    │  │   Metrics    │  │    Tracing    │     │
│  │   (Logback)  │  │ (Micrometer) │  │(OpenTelemetry)│     │
│  └──────┬───────┘  └──────┬───────┘  └───────┬───────┘     │
│         │                  │                   │              │
└─────────┼──────────────────┼───────────────────┼─────────────┘
          │                  │                   │
          ▼                  ▼                   ▼
    ┌─────────┐        ┌────────────┐     ┌──────────┐
    │  Logs   │        │ Prometheus │     │  Jaeger  │
    │ (JSON)  │        │  (Scraper) │     │  (OTLP)  │
    └────┬────┘        └──────┬─────┘     └────┬─────┘
         │                    │                  │
         └────────────────────┼──────────────────┘
                              ▼
                        ┌──────────┐
                        │ Grafana  │
                        │Dashboard │
                        └──────────┘
```

**Beneficios:**
- Debugging rápido con contexto completo
- Detección proactiva de problemas
- Análisis de rendimiento en tiempo real
- Correlación entre logs, metrics y traces

---

## 1. Logging Estructurado

### 1.1 Configuracion de Logback

**Archivo:** `src/main/resources/logback-spring.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <!-- Include Spring Boot defaults -->
    <include resource="org/springframework/boot/logging/logback/defaults.xml"/>

    <!-- Properties -->
    <property name="LOG_PATH" value="${LOG_PATH:-logs}"/>
    <property name="APP_NAME" value="${APP_NAME:-apigen}"/>

    <!-- ==========================================
         Console Appender - Human readable (dev)
         ========================================== -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%clr(%d{yyyy-MM-dd HH:mm:ss.SSS}){faint} %clr(${LOG_LEVEL_PATTERN:-%5p}) %clr([%15.15t]){faint} %clr(%-40.40logger{39}){cyan} %clr(:){faint} %m%n${LOG_EXCEPTION_CONVERSION_WORD:-%wEx}</pattern>
            <charset>UTF-8</charset>
        </encoder>
    </appender>

    <!-- ==========================================
         Console Appender - JSON (prod/structured)
         Uses Logstash encoder for ELK/Splunk
         ========================================== -->
    <appender name="CONSOLE_JSON" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <!-- Include MDC fields automatically -->
            <includeMdcKeyName>requestId</includeMdcKeyName>
            <includeMdcKeyName>correlationId</includeMdcKeyName>
            <includeMdcKeyName>userId</includeMdcKeyName>
            <includeMdcKeyName>clientIp</includeMdcKeyName>
            <includeMdcKeyName>httpMethod</includeMdcKeyName>
            <includeMdcKeyName>requestUri</includeMdcKeyName>
            <includeMdcKeyName>traceId</includeMdcKeyName>
            <includeMdcKeyName>spanId</includeMdcKeyName>

            <!-- Custom fields -->
            <customFields>{"application":"${APP_NAME}","environment":"${SPRING_PROFILES_ACTIVE:-default}"}</customFields>

            <!-- Timestamp format -->
            <timestampPattern>yyyy-MM-dd'T'HH:mm:ss.SSSZ</timestampPattern>

            <!-- Stack trace formatting -->
            <throwableConverter class="net.logstash.logback.stacktrace.ShortenedThrowableConverter">
                <maxDepthPerThrowable>30</maxDepthPerThrowable>
                <maxLength>4096</maxLength>
                <shortenedClassNameLength>20</shortenedClassNameLength>
                <rootCauseFirst>true</rootCauseFirst>
            </throwableConverter>
        </encoder>
    </appender>
</configuration>
```

**Características clave:**

1. **Dual Mode Output:**
   - `CONSOLE`: Logs legibles para desarrollo
   - `CONSOLE_JSON`: Logs estructurados para producción

2. **MDC (Mapped Diagnostic Context):**
   - Campos inyectados automáticamente en cada log
   - Permiten correlación entre requests

3. **Stack Trace Optimization:**
   - Limita profundidad para evitar logs gigantes
   - Root cause primero para debugging rápido

### 1.2 MDC Context en Requests

**Archivo:** `RequestLoggingFilter.java`

```java
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

    // MDC Keys (deben coincidir con logback-spring.xml)
    private static final String MDC_TRACE_ID = "traceId";
    private static final String MDC_REQUEST_ID = "requestId";
    private static final String MDC_CORRELATION_ID = "correlationId";
    private static final String MDC_USER_ID = "userId";
    private static final String MDC_CLIENT_IP = "clientIp";
    private static final String MDC_HTTP_METHOD = "httpMethod";
    private static final String MDC_REQUEST_URI = "requestUri";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 1. Generar o recuperar IDs de correlación
        String traceId = getOrGenerateId(request, "X-Trace-Id");
        String correlationId = getOrGenerateId(request, "X-Correlation-Id");
        String requestId = UUID.randomUUID().toString();
        String spanId = UUID.randomUUID().toString().substring(0, 8);

        // 2. Poblar MDC con contexto
        populateMdc(request, traceId, correlationId, requestId, spanId);

        // 3. Agregar headers de respuesta para correlación
        response.setHeader("X-Trace-Id", traceId);
        response.setHeader("X-Correlation-Id", correlationId);
        response.setHeader("X-Request-Id", requestId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            // 4. IMPORTANTE: Limpiar MDC para evitar memory leaks
            clearMdc();
        }
    }

    private void populateMdc(HttpServletRequest request, String traceId,
                             String correlationId, String requestId, String spanId) {
        MDC.put(MDC_TRACE_ID, traceId);
        MDC.put(MDC_CORRELATION_ID, correlationId);
        MDC.put(MDC_REQUEST_ID, requestId);
        MDC.put(MDC_SPAN_ID, spanId);
        MDC.put(MDC_CLIENT_IP, getClientIp(request));
        MDC.put(MDC_HTTP_METHOD, request.getMethod());
        MDC.put(MDC_REQUEST_URI, request.getRequestURI());
    }
}
```

**Como funciona:**

```
Request → Filter → MDC Population → Business Logic → MDC Cleanup
                         ↓
                   All logs in thread
                   have MDC context
```

### 1.3 Ejemplo de Log JSON

**Input (código):**
```java
log.info("User created successfully");
```

**Output (JSON en producción):**
```json
{
  "@timestamp": "2024-01-15T10:30:45.123Z",
  "level": "INFO",
  "thread": "http-nio-8080-exec-1",
  "logger": "com.jnzader.apigen.service.UserService",
  "message": "User created successfully",
  "application": "apigen",
  "environment": "prod",
  "traceId": "a1b2c3d4-e5f6-7890-abcd-1234567890ab",
  "correlationId": "a1b2c3d4-e5f6-7890-abcd-1234567890ab",
  "requestId": "req-12345",
  "spanId": "a1b2c3d4",
  "userId": "user@example.com",
  "clientIp": "192.168.1.100",
  "httpMethod": "POST",
  "requestUri": "/api/v1/users"
}
```

**Ventajas:**
- Fácil de parsear en ELK/Splunk/CloudWatch
- Todos los campos MDC incluidos automáticamente
- Búsqueda por `traceId` encuentra todos los logs de un request

### 1.4 Niveles de Logging por Perfil

**Development (`application-dev.yaml`):**
```yaml
logging:
  level:
    root: INFO
    com.jnzader.apigen: DEBUG
    org.springframework.security: DEBUG
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
```

**Production (`application-prod.yaml`):**
```yaml
logging:
  level:
    root: WARN
    com.jnzader.apigen: INFO
    org.springframework.security: WARN
    org.hibernate.SQL: WARN
```

### 1.5 Security Audit Logs

**Appender especial para auditoría de seguridad:**

```xml
<appender name="SECURITY_AUDIT_JSON" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>${LOG_PATH}/security-audit.json.log</file>
    <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
        <fileNamePattern>${LOG_PATH}/security-audit.%d{yyyy-MM-dd}.%i.json.log.gz</fileNamePattern>
        <maxHistory>90</maxHistory>  <!-- Retención de 90 días -->
        <totalSizeCap>5GB</totalSizeCap>
    </rollingPolicy>
    <encoder class="net.logstash.logback.encoder.LogstashEncoder">
        <includeMdcKeyName>userId</includeMdcKeyName>
        <includeMdcKeyName>action</includeMdcKeyName>
        <includeMdcKeyName>resource</includeMdcKeyName>
        <customFields>{"logType":"security-audit"}</customFields>
    </encoder>
</appender>
```

**Uso:**
```java
private static final Logger SECURITY_AUDIT = LoggerFactory.getLogger("SECURITY_AUDIT");

MDC.put("action", "LOGIN");
MDC.put("resource", "/api/v1/auth/login");
SECURITY_AUDIT.info("User login successful");
```

---

## 2. Metricas con Micrometer/Prometheus

### 2.1 Configuracion de Actuator

**Archivo:** `application.yaml`

```yaml
management:
  # Actuator endpoints
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus

  # Endpoint de Prometheus
  endpoint:
    prometheus:
      enabled: true
    health:
      show-details: when_authorized

  # Métricas
  metrics:
    export:
      prometheus:
        enabled: true
    distribution:
      percentiles-histogram:
        http.server.requests: true

  # Tracing
  tracing:
    enabled: true
    sampling:
      probability: 1.0  # 100% en dev, 0.1 en prod

  observations:
    key-values:
      application: ${spring.application.name}
```

**Endpoints disponibles:**
- `GET /actuator/health` - Estado de la aplicación
- `GET /actuator/metrics` - Lista de métricas disponibles
- `GET /actuator/prometheus` - Métricas en formato Prometheus

### 2.2 MetricsAspect con @Measured

**Archivo:** `MetricsAspect.java`

```java
@Aspect
@Component
public class MetricsAspect {

    private final MeterRegistry meterRegistry;

    @Value("${app.metrics.enabled:true}")
    private boolean metricsEnabled;

    @Value("${app.metrics.slow-threshold-ms:500}")
    private long slowThresholdMs;

    // ==================== Pointcuts ====================

    @Pointcut("@annotation(measured)")
    public void measuredMethod(Measured measured) {}

    @Pointcut("execution(public * com.jnzader.apigen.core.controller..*Controller.*(..))")
    public void controllerPublicMethod() {}

    @Pointcut("execution(* com.jnzader.apigen.core.service..*Service.save*(..)) || " +
              "execution(* com.jnzader.apigen.core.service..*Service.update*(..)) || " +
              "execution(* com.jnzader.apigen.core.service..*Service.*Delete*(..))")
    public void serviceWriteOperation() {}

    // ==================== Advices ====================

    @Around("measuredMethod(measured)")
    public Object measureAnnotatedMethod(ProceedingJoinPoint joinPoint, Measured measured)
            throws Throwable {
        if (!metricsEnabled) {
            return joinPoint.proceed();
        }

        String metricName = getMetricName(joinPoint, measured);
        long threshold = measured.slowThresholdMs() > 0
            ? measured.slowThresholdMs()
            : slowThresholdMs;

        return executeWithMetrics(joinPoint, metricName, "custom",
                                  measured.histogram(), threshold);
    }

    @Around("controllerPublicMethod() && !measuredMethod(com.jnzader.apigen.core.aspect.Measured)")
    public Object measureControllerEndpoint(ProceedingJoinPoint joinPoint) throws Throwable {
        if (!metricsEnabled) {
            return joinPoint.proceed();
        }

        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        String metricName = className + "." + methodName;

        return executeWithMetrics(joinPoint, metricName, "controller", false, slowThresholdMs);
    }

    // ==================== Helper Methods ====================

    private Object executeWithMetrics(ProceedingJoinPoint joinPoint, String metricName,
                                      String layer, boolean histogram, long threshold)
            throws Throwable {

        Timer.Builder timerBuilder = Timer.builder("apigen.method.duration")
                .tag("name", metricName)
                .tag("layer", layer)
                .description("Tiempo de ejecución de método");

        if (histogram) {
            timerBuilder.publishPercentileHistogram();
        }

        Timer timer = timerBuilder.register(meterRegistry);
        Timer.Sample sample = Timer.start(meterRegistry);

        long startTime = System.currentTimeMillis();
        String outcome = "success";

        try {
            return joinPoint.proceed();
        } catch (Throwable throwable) {
            outcome = "error";
            // Contador de errores
            meterRegistry.counter("apigen.method.errors",
                    "name", metricName,
                    "layer", layer,
                    "exception", throwable.getClass().getSimpleName()
            ).increment();
            throw throwable;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            sample.stop(timer);

            // Contador de llamadas
            meterRegistry.counter("apigen.method.calls",
                    "name", metricName,
                    "layer", layer,
                    "outcome", outcome
            ).increment();

            // Log de métodos lentos
            if (duration > threshold) {
                log.warn("[SLOW] {}.{}() took {}ms (threshold: {}ms)",
                        layer, metricName, duration, threshold);
            }
        }
    }
}
```

### 2.3 Anotacion @Measured

**Archivo:** `Measured.java`

```java
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Measured {

    /**
     * Nombre de la métrica (opcional).
     * Si no se especifica, se usa el nombre del método.
     */
    String name() default "";

    /**
     * Descripción de la métrica (opcional).
     */
    String description() default "";

    /**
     * Si true, también registra el histograma de distribución.
     */
    boolean histogram() default false;

    /**
     * Umbral en milisegundos para logging de métodos lentos.
     * -1 significa usar el valor por defecto del aspecto.
     */
    long slowThresholdMs() default -1;
}
```

**Uso:**

```java
@Service
public class OrderService {

    @Measured(
        name = "process-order",
        description = "Procesamiento completo de orden",
        histogram = true,
        slowThresholdMs = 1000
    )
    public Result<Order, Exception> processOrder(OrderRequest request) {
        // Business logic
    }
}
```

### 2.4 Metricas Automaticas

**Spring Boot Actuator expone automáticamente:**

1. **HTTP Server Metrics:**
   - `http_server_requests_seconds_count` - Total de requests
   - `http_server_requests_seconds_sum` - Suma total de duración
   - `http_server_requests_seconds_max` - Máxima duración
   - `http_server_requests_seconds_bucket` - Histograma

2. **JVM Metrics:**
   - `jvm_memory_used_bytes` - Memoria usada
   - `jvm_memory_max_bytes` - Memoria máxima
   - `jvm_gc_pause_seconds` - Tiempos de GC
   - `jvm_threads_live_threads` - Threads activos

3. **HikariCP Metrics:**
   - `hikaricp_connections_active` - Conexiones activas
   - `hikaricp_connections_idle` - Conexiones ociosas
   - `hikaricp_connections_pending` - Conexiones pendientes
   - `hikaricp_connections_max` - Máximo de conexiones

4. **Cache Metrics:**
   - `cache_gets_hit_total` - Cache hits
   - `cache_gets_miss_total` - Cache misses
   - `cache_evictions_total` - Evictions

### 2.5 Configuracion de Prometheus

**Archivo:** `docker/prometheus/prometheus.yml`

```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  # Prometheus self-monitoring
  - job_name: 'prometheus'
    static_configs:
      - targets: ['localhost:9090']

  # APiGen Application
  - job_name: 'apigen'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['apigen:8080']
    relabel_configs:
      - source_labels: [__address__]
        target_label: instance
        replacement: 'apigen-app'
```

**Prometheus scrapeará el endpoint cada 15 segundos.**

### 2.6 Alertas de Prometheus

**Archivo:** `docker/prometheus/alerts.yml`

```yaml
groups:
  # ==========================================
  # Application Health Alerts
  # ==========================================
  - name: apigen_application
    interval: 30s
    rules:
      - alert: ApplicationDown
        expr: up{job="apigen"} == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "APiGen application is down"
          description: "APiGen instance has been down for more than 1 minute."

      - alert: HighErrorRate
        expr: |
          (
            sum(rate(http_server_requests_seconds_count{job="apigen", status=~"5.."}[5m]))
            /
            sum(rate(http_server_requests_seconds_count{job="apigen"}[5m]))
          ) * 100 > 5
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High error rate detected"
          description: "Error rate is {{ $value | printf \"%.2f\" }}% (threshold: 5%)"

  # ==========================================
  # JVM Alerts
  # ==========================================
  - name: apigen_jvm
    interval: 30s
    rules:
      - alert: HighMemoryUsage
        expr: |
          (jvm_memory_used_bytes{job="apigen", area="heap"}
           / jvm_memory_max_bytes{job="apigen", area="heap"}) * 100 > 85
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High JVM heap memory usage"
          description: "Heap memory usage is {{ $value | printf \"%.2f\" }}%"

  # ==========================================
  # Database Connection Pool Alerts
  # ==========================================
  - name: apigen_database
    interval: 30s
    rules:
      - alert: DatabaseConnectionPoolExhaustion
        expr: |
          (hikaricp_connections_active{job="apigen"}
           / hikaricp_connections_max{job="apigen"}) * 100 > 80
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Database connection pool nearly exhausted"
          description: "Connection pool usage is {{ $value | printf \"%.2f\" }}%"
```

---

## 3. Tracing Distribuido con OpenTelemetry

### 3.1 TracingConfig

**Archivo:** `TracingConfig.java`

```java
@Configuration
@ConditionalOnProperty(
        name = "management.tracing.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class TracingConfig {

    private static final Logger log = LoggerFactory.getLogger(TracingConfig.class);

    /**
     * Habilita el aspecto @Observed para instrumentación declarativa.
     *
     * Uso:
     * @Observed(name = "my.operation", contextualName = "processOrder")
     * public void processOrder(Order order) {
     *     // Este método será trazado automáticamente
     * }
     */
    @Bean
    public ObservedAspect observedAspect(ObservationRegistry registry) {
        log.info("Tracing enabled - @Observed aspect configured");
        return new ObservedAspect(registry);
    }
}
```

### 3.2 Configuracion de Tracing

**Archivo:** `application.yaml`

```yaml
management:
  tracing:
    enabled: ${TRACING_ENABLED:true}
    sampling:
      probability: ${TRACING_SAMPLING_PROBABILITY:1.0}

  otlp:
    tracing:
      endpoint: ${OTLP_ENDPOINT:http://localhost:4318/v1/traces}

  observations:
    key-values:
      application: ${spring.application.name}
```

**Profiles:**

- **Development:** 100% sampling (`probability: 1.0`)
- **Production:** 10% sampling (`probability: 0.1`) para reducir overhead

### 3.3 Propagacion de Contexto

**Micrometer Tracing propaga automáticamente el contexto de tracing:**

```
Client Request → APiGen (Span 1)
                    ↓
              Database Call (Span 2)
                    ↓
              External API (Span 3)
                    ↓
              Response
```

**Headers HTTP propagados:**
- `traceparent`: W3C Trace Context
- `tracestate`: Estado adicional del trace

**Ejemplo de `traceparent`:**
```
00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01
│  │                                  │                 │
│  └─ Trace ID                       └─ Span ID        └─ Sampled
└─ Version
```

### 3.4 Tracing Manual

**Para operaciones específicas:**

```java
@Service
public class OrderService {

    private final Tracer tracer;

    @Autowired
    public OrderService(Tracer tracer) {
        this.tracer = tracer;
    }

    public Order processOrder(OrderRequest request) {
        // Crear un span manual
        Span span = tracer.nextSpan().name("process-order").start();

        try (Tracer.SpanInScope ws = tracer.withSpan(span)) {
            // Agregar atributos al span
            span.tag("order.id", request.getOrderId());
            span.tag("order.amount", String.valueOf(request.getAmount()));

            // Business logic
            Order order = createOrder(request);

            // Eventos dentro del span
            span.event("order-validated");

            validateOrder(order);

            span.event("order-persisted");

            return order;
        } catch (Exception e) {
            span.error(e);
            throw e;
        } finally {
            span.end();
        }
    }
}
```

### 3.5 Tracing con @Observed

**Forma declarativa (recomendada):**

```java
@Service
public class PaymentService {

    @Observed(
        name = "payment.process",
        contextualName = "process-payment",
        lowCardinalityKeyValues = {"service", "payment"}
    )
    public PaymentResult processPayment(PaymentRequest request) {
        // Este método será trazado automáticamente
        // incluyendo cualquier llamada interna
        return paymentGateway.charge(request);
    }
}
```

### 3.6 Integracion con Jaeger (Opcional)

**Docker Compose:**

```yaml
services:
  jaeger:
    image: jaegertracing/all-in-one:latest
    container_name: apigen-jaeger
    ports:
      - "16686:16686"  # Jaeger UI
      - "4318:4318"    # OTLP HTTP receiver
    environment:
      COLLECTOR_OTLP_ENABLED: true
    networks:
      - apigen-network
```

**Configuración en application.yaml:**

```yaml
management:
  otlp:
    tracing:
      endpoint: http://jaeger:4318/v1/traces
```

**Acceso a Jaeger UI:**
```
http://localhost:16686
```

---

## 4. Dashboards Grafana

### 4.1 Provisioning de Datasources

**Archivo:** `docker/grafana/provisioning/datasources/datasources.yml`

```yaml
apiVersion: 1

datasources:
  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://prometheus:9090
    isDefault: true
    editable: false
```

**Grafana conecta automáticamente a Prometheus al iniciar.**

### 4.2 Provisioning de Dashboards

**Archivo:** `docker/grafana/provisioning/dashboards/dashboards.yml`

```yaml
apiVersion: 1

providers:
  - name: 'APiGen Dashboards'
    orgId: 1
    folder: 'APiGen'
    folderUid: 'apigen'
    type: file
    disableDeletion: false
    updateIntervalSeconds: 30
    allowUiUpdates: true
    options:
      path: /etc/grafana/provisioning/dashboards/json
```

### 4.3 Dashboard APiGen Overview

**Archivo:** `docker/grafana/provisioning/dashboards/json/apigen-overview.json`

**Paneles principales:**

1. **Application Status**
   ```promql
   up{job="apigen"}
   ```
   - Valor: 1 (UP) o 0 (DOWN)

2. **Request Rate**
   ```promql
   sum(rate(http_server_requests_seconds_count{job="apigen"}[5m]))
   ```
   - Requests por segundo

3. **Response Time Percentiles**
   ```promql
   # P50
   histogram_quantile(0.50, sum(rate(http_server_requests_seconds_bucket{job="apigen"}[5m])) by (le))

   # P95
   histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket{job="apigen"}[5m])) by (le))

   # P99
   histogram_quantile(0.99, sum(rate(http_server_requests_seconds_bucket{job="apigen"}[5m])) by (le))
   ```

4. **Heap Usage**
   ```promql
   (jvm_memory_used_bytes{job="apigen", area="heap"} / jvm_memory_max_bytes{job="apigen", area="heap"}) * 100
   ```

5. **Database Connection Pool**
   ```promql
   # Active
   hikaricp_connections_active{job="apigen"}

   # Idle
   hikaricp_connections_idle{job="apigen"}

   # Pending
   hikaricp_connections_pending{job="apigen"}
   ```

6. **Cache Hit Rate**
   ```promql
   (sum(rate(cache_gets_hit_total{job="apigen"}[5m])) /
    (sum(rate(cache_gets_hit_total{job="apigen"}[5m])) +
     sum(rate(cache_gets_miss_total{job="apigen"}[5m])))) * 100
   ```

7. **HTTP Status Codes**
   ```promql
   # 2xx Success
   sum(rate(http_server_requests_seconds_count{job="apigen", status=~"2.."}[5m]))

   # 4xx Errors
   sum(rate(http_server_requests_seconds_count{job="apigen", status=~"4.."}[5m]))

   # 5xx Errors
   sum(rate(http_server_requests_seconds_seconds_count{job="apigen", status=~"5.."}[5m]))
   ```

8. **JVM Threads**
   ```promql
   # Live Threads
   jvm_threads_live_threads{job="apigen"}

   # Daemon Threads
   jvm_threads_daemon_threads{job="apigen"}
   ```

### 4.4 Queries PromQL Utiles

**Tasa de errores:**
```promql
(sum(rate(http_server_requests_seconds_count{job="apigen", status=~"5.."}[5m]))
 /
 sum(rate(http_server_requests_seconds_count{job="apigen"}[5m]))) * 100
```

**Latencia promedio:**
```promql
rate(http_server_requests_seconds_sum{job="apigen"}[5m])
/
rate(http_server_requests_seconds_count{job="apigen"}[5m])
```

**Top 5 endpoints más lentos:**
```promql
topk(5,
  histogram_quantile(0.95,
    sum(rate(http_server_requests_seconds_bucket{job="apigen"}[5m])) by (le, uri)
  )
)
```

**Throughput por endpoint:**
```promql
sum(rate(http_server_requests_seconds_count{job="apigen"}[5m])) by (uri)
```

### 4.5 Alertas en Grafana

**Configurar notificaciones:**

1. **Grafana UI → Alerting → Notification channels**
2. Agregar canal (Email, Slack, PagerDuty, etc.)
3. En dashboard, clic en panel → Edit → Alert
4. Configurar condición:
   ```
   WHEN avg() OF query(A, 5m, now) IS ABOVE 85
   ```

**Ejemplo de alerta de memoria:**

```json
{
  "alert": {
    "name": "High Memory Usage",
    "conditions": [
      {
        "evaluator": {
          "params": [85],
          "type": "gt"
        },
        "query": {
          "params": ["A", "5m", "now"]
        },
        "reducer": {
          "type": "avg"
        },
        "type": "query"
      }
    ],
    "frequency": "1m",
    "handler": 1,
    "notifications": [
      {
        "uid": "slack-notifications"
      }
    ]
  }
}
```

---

## 5. Health Checks

### 5.1 Endpoint /actuator/health

**Configuración:**

```yaml
management:
  endpoint:
    health:
      show-details: always  # Dev
      # show-details: when_authorized  # Prod
```

**Response:**

```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "PostgreSQL",
        "validationQuery": "isValid()"
      }
    },
    "diskSpace": {
      "status": "UP",
      "details": {
        "total": 499963174912,
        "free": 289587679232,
        "threshold": 10485760,
        "exists": true
      }
    },
    "ping": {
      "status": "UP"
    }
  }
}
```

### 5.2 Health Indicators Custom

**Ejemplo: External API Health Check**

```java
@Component
public class ExternalApiHealthIndicator implements HealthIndicator {

    private final RestTemplate restTemplate;
    private final String apiUrl;

    @Override
    public Health health() {
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(
                apiUrl + "/health",
                String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                return Health.up()
                    .withDetail("api", apiUrl)
                    .withDetail("status", response.getStatusCode())
                    .build();
            } else {
                return Health.down()
                    .withDetail("api", apiUrl)
                    .withDetail("status", response.getStatusCode())
                    .build();
            }
        } catch (Exception e) {
            return Health.down()
                .withDetail("api", apiUrl)
                .withException(e)
                .build();
        }
    }
}
```

### 5.3 Readiness y Liveness Probes

**Para Kubernetes:**

```yaml
management:
  endpoint:
    health:
      probes:
        enabled: true
      group:
        readiness:
          include: readinessState,db
        liveness:
          include: livenessState,ping
```

**Endpoints:**
- `GET /actuator/health/liveness` - Determina si reiniciar el pod
- `GET /actuator/health/readiness` - Determina si recibir tráfico

**Kubernetes Deployment:**

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: apigen
spec:
  template:
    spec:
      containers:
      - name: apigen
        image: apigen:latest
        ports:
        - containerPort: 8080
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
          timeoutSeconds: 5
          failureThreshold: 3
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 20
          periodSeconds: 5
          timeoutSeconds: 3
          failureThreshold: 3
```

### 5.4 Custom Health Groups

```yaml
management:
  endpoint:
    health:
      group:
        custom:
          include: db,diskSpace,externalApi
          show-details: always
```

**Acceso:**
```
GET /actuator/health/custom
```

---

## 6. Integracion Completa

### 6.1 Docker Compose con Stack de Observabilidad

**Archivo:** `docker-compose.yml`

```yaml
services:
  # ------------------------------
  # APiGen Application
  # ------------------------------
  apigen:
    build:
      context: .
      dockerfile: Dockerfile
    image: apigen:latest
    container_name: apigen-app
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: prod
      DB_URL: jdbc:postgresql://postgres:5432/apigen
      DB_USERNAME: apigen_user
      DB_PASSWORD: apigen_password
      LOG_PATH: /app/logs
      OTLP_ENDPOINT: http://jaeger:4318/v1/traces
    volumes:
      - apigen-logs:/app/logs
    networks:
      - apigen-network
    healthcheck:
      test: ["CMD", "wget", "-q", "--spider", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3

  # ------------------------------
  # PostgreSQL Database
  # ------------------------------
  postgres:
    image: postgres:17-alpine
    container_name: apigen-postgres
    ports:
      - "5432:5432"
    environment:
      POSTGRES_DB: apigen
      POSTGRES_USER: apigen_user
      POSTGRES_PASSWORD: apigen_password
    volumes:
      - postgres-data:/var/lib/postgresql/data
    networks:
      - apigen-network
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U apigen_user -d apigen"]
      interval: 10s
      timeout: 5s
      retries: 5

  # ------------------------------
  # Prometheus (Metrics)
  # ------------------------------
  prometheus:
    image: prom/prometheus:latest
    container_name: apigen-prometheus
    ports:
      - "9090:9090"
    volumes:
      - ./docker/prometheus/prometheus.yml:/etc/prometheus/prometheus.yml:ro
      - ./docker/prometheus/alerts.yml:/etc/prometheus/alerts.yml:ro
      - prometheus-data:/prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
      - '--web.enable-lifecycle'
    networks:
      - apigen-network
    profiles:
      - monitoring

  # ------------------------------
  # Grafana (Dashboards)
  # ------------------------------
  grafana:
    image: grafana/grafana:latest
    container_name: apigen-grafana
    ports:
      - "3000:3000"
    environment:
      GF_SECURITY_ADMIN_USER: admin
      GF_SECURITY_ADMIN_PASSWORD: admin123
      GF_USERS_ALLOW_SIGN_UP: "false"
    volumes:
      - grafana-data:/var/lib/grafana
      - ./docker/grafana/provisioning:/etc/grafana/provisioning:ro
    depends_on:
      - prometheus
    networks:
      - apigen-network
    profiles:
      - monitoring

  # ------------------------------
  # Jaeger (Tracing - Opcional)
  # ------------------------------
  jaeger:
    image: jaegertracing/all-in-one:latest
    container_name: apigen-jaeger
    ports:
      - "16686:16686"  # Jaeger UI
      - "4318:4318"    # OTLP HTTP receiver
    environment:
      COLLECTOR_OTLP_ENABLED: true
    networks:
      - apigen-network
    profiles:
      - monitoring

networks:
  apigen-network:
    driver: bridge

volumes:
  postgres-data:
  prometheus-data:
  grafana-data:
  apigen-logs:
```

### 6.2 Iniciar el Stack Completo

**Solo aplicación y base de datos:**
```bash
docker compose up -d
```

**Con stack de monitoreo:**
```bash
docker compose --profile monitoring up -d
```

**Servicios disponibles:**
- APiGen: http://localhost:8080
- Prometheus: http://localhost:9090
- Grafana: http://localhost:3000 (admin/admin123)
- Jaeger: http://localhost:16686

### 6.3 Flujo Completo de Observabilidad

```
1. Request → APiGen
   ↓
2. RequestLoggingFilter → MDC Population
   ↓
3. Controller (medido por MetricsAspect)
   ↓
4. Service (trazado por @Observed)
   ↓
5. Repository → Database
   ↓
6. Response
   ↓
7. Logs → JSON (con traceId, requestId, etc.)
   Metrics → Prometheus (duración, errores, etc.)
   Traces → Jaeger (spans de toda la transacción)
   ↓
8. Grafana Dashboard (visualización unificada)
```

### 6.4 Correlacion entre Logs, Metrics y Traces

**Scenario: Investigar request lento**

1. **Grafana Dashboard** muestra P95 alto
2. **Prometheus** identifica endpoint problemático:
   ```promql
   topk(1, histogram_quantile(0.95,
     sum(rate(http_server_requests_seconds_bucket{job="apigen"}[5m])) by (le, uri)
   ))
   ```
3. **Logs JSON** filtrar por `requestUri`:
   ```bash
   cat logs/apigen.json.log | jq 'select(.requestUri == "/api/v1/orders")'
   ```
4. **Obtener traceId** del log:
   ```json
   {
     "traceId": "a1b2c3d4-e5f6-7890-abcd-1234567890ab",
     "message": "Order processing took 2500ms"
   }
   ```
5. **Jaeger** buscar por `traceId` para ver toda la transacción:
   - Span 1: Controller (500ms)
   - Span 2: Service (200ms)
   - Span 3: Database Query (1800ms) ← **PROBLEMA AQUÍ**

**Resultado:** Query N+1 identificado y optimizado.

---

## Buenas Practicas

### 1. Logging

1. **Usar niveles apropiados:**
   - `ERROR`: Errores que requieren acción inmediata
   - `WARN`: Situaciones anómalas pero recuperables
   - `INFO`: Eventos importantes del negocio
   - `DEBUG`: Información detallada para debugging
   - `TRACE`: Información muy detallada (SQL params, etc.)

2. **Incluir contexto relevante:**
   ```java
   // Mal
   log.error("Error");

   // Bien
   log.error("Failed to process order [orderId={}] for user [userId={}]",
             order.getId(), user.getId(), exception);
   ```

3. **No loguear información sensible:**
   - Passwords
   - Tokens de autenticación
   - Números de tarjeta de crédito
   - PII (Personally Identifiable Information)

4. **Usar MDC para contexto:**
   ```java
   MDC.put("userId", user.getId());
   try {
       // Business logic
   } finally {
       MDC.remove("userId");
   }
   ```

### 2. Metricas

1. **Naming conventions:**
   - Usar puntos para jerarquía: `apigen.method.duration`
   - Lowercase
   - Descriptivos pero concisos

2. **Tags apropiados:**
   - **Baja cardinalidad:** `layer=controller`, `outcome=success`
   - **Evitar alta cardinalidad:** `userId=123` (miles de usuarios)

3. **Percentiles vs Promedio:**
   - Usar percentiles (P50, P95, P99) en lugar de promedio
   - El promedio oculta outliers

4. **Timers vs Counters:**
   - **Timer:** Duración de operaciones
   - **Counter:** Eventos discretos (requests, errores)
   - **Gauge:** Valores que suben y bajan (conexiones, memoria)

### 3. Tracing

1. **Sampling strategy:**
   - Development: 100% sampling
   - Production: 10-20% sampling (ajustar según volumen)

2. **Span naming:**
   - Descriptivo: `process-order` mejor que `method1`
   - Consistente en toda la aplicación

3. **Atributos útiles:**
   ```java
   span.tag("order.id", orderId);
   span.tag("order.total", total.toString());
   span.tag("user.tier", userTier);
   ```

4. **Eventos dentro de spans:**
   ```java
   span.event("order-validated");
   span.event("payment-processed");
   span.event("notification-sent");
   ```

### 4. Health Checks

1. **Rápidos y ligeros:**
   - No ejecutar lógica de negocio compleja
   - Timeout corto (2-3 segundos)

2. **Incluir dependencias críticas:**
   - Database
   - External APIs esenciales
   - Message brokers

3. **Diferentes niveles:**
   - Liveness: ¿Está viva la aplicación?
   - Readiness: ¿Puede recibir tráfico?

---

## Troubleshooting

### Problema 1: Logs no aparecen en JSON

**Síntoma:**
```
Logs en consola pero no en formato JSON
```

**Solución:**
```yaml
# Verificar perfil activo
SPRING_PROFILES_ACTIVE=prod

# En logback-spring.xml, verificar:
<springProfile name="prod">
    <appender-ref ref="CONSOLE_JSON"/>
</springProfile>
```

### Problema 2: Prometheus no scrapeando métricas

**Síntoma:**
```
Prometheus UI → Targets → APiGen: DOWN
```

**Diagnóstico:**
```bash
# 1. Verificar endpoint manualmente
curl http://localhost:8080/actuator/prometheus

# 2. Verificar configuración de Actuator
curl http://localhost:8080/actuator

# 3. Verificar prometheus.yml
docker exec -it apigen-prometheus cat /etc/prometheus/prometheus.yml
```

**Solución:**
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus  # ← Incluir prometheus
```

### Problema 3: Grafana Dashboard sin datos

**Síntoma:**
```
Dashboard muestra "No data"
```

**Diagnóstico:**
```bash
# 1. Verificar Prometheus tiene datos
# En Prometheus UI (http://localhost:9090):
up{job="apigen"}

# 2. Verificar datasource en Grafana
# Grafana → Configuration → Data Sources → Test

# 3. Verificar query en panel
http_server_requests_seconds_count{job="apigen"}
```

**Solución:**
- Verificar que el job name coincida: `job="apigen"`
- Verificar que haya tráfico en la aplicación
- Ajustar time range en dashboard

### Problema 4: Tracing no funcionando

**Síntoma:**
```
No aparecen traces en Jaeger
```

**Diagnóstico:**
```yaml
# 1. Verificar configuración
management:
  tracing:
    enabled: true  # ← Debe ser true
    sampling:
      probability: 1.0  # ← > 0
  otlp:
    tracing:
      endpoint: http://jaeger:4318/v1/traces  # ← Verificar URL
```

**Solución:**
```bash
# 1. Verificar que Jaeger está corriendo
docker ps | grep jaeger

# 2. Verificar logs de APiGen
docker logs apigen-app | grep -i tracing

# 3. Verificar endpoint OTLP
curl http://localhost:4318/v1/traces
```

### Problema 5: MDC no aparece en logs

**Síntoma:**
```json
{
  "message": "User created",
  // Faltan campos MDC: traceId, requestId, etc.
}
```

**Solución:**
```xml
<!-- En logback-spring.xml, verificar: -->
<encoder class="net.logstash.logback.encoder.LogstashEncoder">
    <includeMdcKeyName>requestId</includeMdcKeyName>  <!-- ← Agregar todos -->
    <includeMdcKeyName>traceId</includeMdcKeyName>
    <includeMdcKeyName>userId</includeMdcKeyName>
</encoder>
```

### Problema 6: Memoria alta por logs

**Síntoma:**
```
Disco lleno por archivos de log
```

**Solución:**
```xml
<rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
    <fileNamePattern>${LOG_PATH}/${APP_NAME}.%d{yyyy-MM-dd}.%i.json.log.gz</fileNamePattern>
    <maxHistory>30</maxHistory>  <!-- ← Retener solo 30 días -->
    <totalSizeCap>3GB</totalSizeCap>  <!-- ← Límite total -->
</rollingPolicy>
```

### Problema 7: @Measured no funcionando

**Síntoma:**
```
Métodos anotados con @Measured no generan métricas
```

**Diagnóstico:**
```java
// Verificar que @Measured está en el paquete correcto
import com.jnzader.apigen.core.infrastructure.aspect.Measured;

// Verificar que la clase es un Spring Bean
@Service  // ← Debe tener @Service, @Component, etc.
public class MyService {

    @Measured  // ← Solo funciona en Spring Beans
    public void myMethod() { }
}
```

**Solución:**
- Solo funciona en Spring Beans (Proxies)
- No funciona en métodos privados
- No funciona en llamadas internas (`this.method()`)

---

## Resumen

APiGen implementa **Observabilidad Completa** con:

1. **Logging Estructurado:**
   - Logback con JSON en producción
   - MDC para correlación
   - Sanitización de datos sensibles

2. **Métricas:**
   - Micrometer + Prometheus
   - MetricsAspect con @Measured
   - Métricas automáticas (HTTP, JVM, DB)

3. **Tracing:**
   - Micrometer Tracing + OpenTelemetry
   - @Observed para tracing declarativo
   - Integración con Jaeger

4. **Dashboards:**
   - Grafana con provisioning automático
   - Dashboard APiGen Overview
   - Alertas configuradas

5. **Health Checks:**
   - /actuator/health
   - Readiness y Liveness probes
   - Health indicators custom

**Stack completo con un comando:**
```bash
docker compose --profile monitoring up -d
```

**Accesos:**
- APiGen: http://localhost:8080
- Prometheus: http://localhost:9090
- Grafana: http://localhost:3000
- Jaeger: http://localhost:16686

**La observabilidad no es opcional en producción - es esencial para mantener sistemas confiables y debuggeables.**
