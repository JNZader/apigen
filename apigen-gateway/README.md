# APiGen Gateway Module

API Gateway with Spring Cloud Gateway for APiGen applications.

## Features

- **Global Filters**: Logging, Authentication, Rate Limiting, Request Timing
- **Dynamic Routes**: Runtime route configuration without restart
- **Circuit Breaker**: Resilience4j integration with timeout and fallback
- **CORS Support**: Configurable cross-origin resource sharing
- **Metrics**: Micrometer integration for Prometheus/Grafana

## Quick Start

### 1. Add Dependency

```gradle
implementation 'com.jnzader:apigen-gateway:1.0.0-SNAPSHOT'
```

### 2. Enable Gateway

```yaml
apigen:
  gateway:
    enabled: true
    logging:
      enabled: true
      include-headers: false
    auth:
      enabled: true
      excluded-paths:
        - /public/**
        - /health
        - /actuator/**
    rate-limit:
      enabled: true
      default-replenish-rate: 100
      default-burst-capacity: 200
    circuit-breaker:
      enabled: true
      timeout: 10s
    cors:
      enabled: true
      allowed-origins:
        - http://localhost:3000
      allowed-methods:
        - GET
        - POST
        - PUT
        - DELETE
```

### 3. Configure Routes

```java
@Configuration
public class GatewayRoutesConfig {

    @Bean
    public RouteLocator customRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
            .route("user-service", r -> r
                .path("/api/users/**")
                .filters(f -> f.stripPrefix(1))
                .uri("http://user-service:8080"))
            .route("product-service", r -> r
                .path("/api/products/**")
                .filters(f -> f
                    .stripPrefix(1)
                    .circuitBreaker(c -> c.setName("product-cb")))
                .uri("http://product-service:8080"))
            .build();
    }
}
```

## Configuration Properties

| Property | Default | Description |
|----------|---------|-------------|
| `apigen.gateway.enabled` | `false` | Enable gateway |
| `apigen.gateway.logging.enabled` | `true` | Enable request logging |
| `apigen.gateway.logging.include-headers` | `false` | Log request headers |
| `apigen.gateway.auth.enabled` | `true` | Enable authentication filter |
| `apigen.gateway.auth.excluded-paths` | `[]` | Paths to skip authentication |
| `apigen.gateway.auth.header-name` | `Authorization` | Auth header name |
| `apigen.gateway.auth.token-prefix` | `Bearer ` | Token prefix |
| `apigen.gateway.rate-limit.enabled` | `true` | Enable rate limiting |
| `apigen.gateway.rate-limit.default-replenish-rate` | `100` | Tokens per second |
| `apigen.gateway.rate-limit.default-burst-capacity` | `200` | Maximum burst |
| `apigen.gateway.circuit-breaker.enabled` | `true` | Enable circuit breaker |
| `apigen.gateway.circuit-breaker.timeout` | `10s` | Request timeout |
| `apigen.gateway.circuit-breaker.failure-rate-threshold` | `50` | Failure % to open |
| `apigen.gateway.cors.enabled` | `true` | Enable CORS |
| `apigen.gateway.cors.allowed-origins` | `*` | Allowed origins |

## Components

### LoggingGatewayFilter

Logs incoming requests and outgoing responses with correlation IDs:

```java
@Bean
public LoggingGatewayFilter loggingFilter() {
    return new LoggingGatewayFilter(true, false); // includeHeaders, includeBody
}
```

Output:
```
INFO  Incoming request: GET /api/users [correlationId=550e8400-e29b-41d4...]
INFO  Outgoing response: 200 OK [correlationId=550e8400-e29b-41d4...] duration=45ms
```

### AuthenticationGatewayFilter

JWT authentication at the gateway level:

```java
@Bean
public Function<String, AuthResult> tokenValidator(JwtService jwtService) {
    return token -> {
        try {
            Claims claims = jwtService.validate(token);
            return AuthResult.success(
                claims.getSubject(),
                List.of(claims.get("roles", String.class).split(","))
            );
        } catch (Exception e) {
            return AuthResult.failure("Invalid token: " + e.getMessage());
        }
    };
}
```

### RateLimitKeyResolver

Flexible rate limit key resolution:

```java
@Bean
public KeyResolver keyResolver() {
    // Options: IP, USER_ID, API_KEY, COMPOSITE, PATH
    return new RateLimitKeyResolver(KeyResolutionStrategy.COMPOSITE);
}
```

| Strategy | Key Format | Description |
|----------|------------|-------------|
| IP | `ip:192.168.1.100` | Rate limit by client IP |
| USER_ID | `user:user123` | Rate limit by authenticated user |
| API_KEY | `apikey:xxx` | Rate limit by API key header |
| COMPOSITE | `composite:ip:method:path` | Combined key |
| PATH | `path:/api/users/{id}` | Rate limit by endpoint |

### RequestTimingGatewayFilter

Metrics for Prometheus/Grafana:

```java
@Bean
public RequestTimingGatewayFilter timingFilter(MeterRegistry registry) {
    return new RequestTimingGatewayFilter(registry);
}
```

Metrics exposed:
- `gateway.requests` (Timer) - Request duration
- `gateway.requests.total` (Counter) - Request count

Tags: `method`, `path`, `route`, `status`

### Dynamic Routes

Add, update, or remove routes at runtime:

```java
@Service
public class RouteManagementService {

    private final DynamicRouteLocator routeLocator;

    public void addRoute(String id, String uri, String path) {
        RouteBuilder.RouteDefinition definition = RouteBuilder.route(id)
            .uri(uri)
            .path(path)
            .stripPrefix(1)
            .circuitBreaker(id + "-cb")
            .buildDefinition();

        routeLocator.addRoute(definition);
    }

    public void removeRoute(String id) {
        routeLocator.removeRoute(id);
    }
}
```

### RouteBuilder

Fluent API for route configuration:

```java
RouteBuilder.RouteDefinition route = RouteBuilder.route("orders-service")
    .uri("http://orders:8080")
    .path("/api/orders/**", "/api/v2/orders/**")
    .method("GET", "POST", "PUT", "DELETE")
    .header("X-Version", "2")
    .stripPrefix(1)
    .rewritePath("/api/v2/(?<segment>.*)", "/${segment}")
    .addRequestHeader("X-Gateway-Route", "orders-service")
    .circuitBreaker("orders-cb")
    .timeout(Duration.ofSeconds(30))
    .metadata("service", "orders")
    .order(100)
    .buildDefinition();
```

### CircuitBreakerGatewayFilter

Custom circuit breaker with fallback:

```java
@Bean
public CircuitBreakerGatewayFilter circuitBreaker() {
    return CircuitBreakerGatewayFilter.builder("my-service")
        .timeout(Duration.ofSeconds(15))
        .fallback(exchange -> {
            exchange.getResponse().setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
            return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse()
                    .bufferFactory()
                    .wrap("{\"error\":\"Service temporarily unavailable\"}".getBytes()))
            );
        })
        .build();
}
```

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    API Gateway                               │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │   Logging   │  │    Auth     │  │    Rate Limit       │  │
│  │   Filter    │──│   Filter    │──│    Key Resolver     │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
│         │               │                    │               │
│         ▼               ▼                    ▼               │
│  ┌─────────────────────────────────────────────────────────┐│
│  │              Route Locator (Dynamic)                    ││
│  └─────────────────────────────────────────────────────────┘│
│         │                                                    │
│         ▼                                                    │
│  ┌─────────────────────────────────────────────────────────┐│
│  │              Circuit Breaker + Timeout                  ││
│  └─────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────┘
         │                    │                    │
         ▼                    ▼                    ▼
    ┌─────────┐         ┌─────────┐         ┌─────────┐
    │ Service │         │ Service │         │ Service │
    │    A    │         │    B    │         │    C    │
    └─────────┘         └─────────┘         └─────────┘
```

## Dependencies

- Spring Cloud Gateway 2024.0.1
- Spring Cloud CircuitBreaker (Resilience4j)
- Micrometer Core
- Spring Boot 4.0.0

## License

MIT License
