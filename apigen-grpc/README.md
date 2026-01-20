# APiGen gRPC Module

gRPC support for inter-service communication in APiGen applications.

## Features

- **Server Infrastructure**: GrpcServer wrapper with fluent builder API
- **Client Infrastructure**: GrpcChannelFactory for managed channel creation
- **Interceptors**: Logging, Authentication, and Exception handling
- **Health Checks**: HealthServiceManager for service health monitoring
- **Proto Definitions**: Common types (pagination, errors, audit info)

## Quick Start

### 1. Add Dependency

```gradle
implementation 'com.jnzader:apigen-grpc:1.0.0-SNAPSHOT'
```

### 2. Enable gRPC

```yaml
apigen:
  grpc:
    enabled: true
    server:
      port: 9090
      logging:
        enabled: true
    client:
      deadline-ms: 10000
      use-plaintext: false
```

### 3. Create a gRPC Server

```java
@Configuration
public class GrpcServerConfig {

    @Bean
    public GrpcServer grpcServer(
            List<BindableService> services,
            List<ServerInterceptor> interceptors) throws IOException {

        return GrpcServer.builder(9090)
            .addServices(services)
            .addInterceptors(interceptors)
            .build()
            .start();
    }
}
```

### 4. Create a Client

```java
@Service
public class ProductClient {

    private final GrpcChannelFactory channelFactory;
    private final ProductServiceGrpc.ProductServiceBlockingStub stub;

    public ProductClient(GrpcChannelFactory channelFactory) {
        this.channelFactory = channelFactory;
        ManagedChannel channel = channelFactory.getChannel("product-service:9090", true);
        this.stub = ProductServiceGrpc.newBlockingStub(channel);
    }

    public Product getProduct(String id) {
        return stub.getProduct(GetProductRequest.newBuilder().setId(id).build());
    }
}
```

## Configuration Properties

| Property | Default | Description |
|----------|---------|-------------|
| `apigen.grpc.enabled` | `false` | Enable gRPC support |
| `apigen.grpc.server.port` | `9090` | gRPC server port |
| `apigen.grpc.server.logging.enabled` | `true` | Enable server logging |
| `apigen.grpc.server.max-inbound-message-size` | `4MB` | Max message size |
| `apigen.grpc.client.deadline-ms` | `10000` | Default deadline |
| `apigen.grpc.client.use-plaintext` | `false` | Use plaintext (no TLS) |
| `apigen.grpc.client.logging.enabled` | `true` | Enable client logging |

## Components

### Server Interceptors

#### LoggingServerInterceptor

Logs all gRPC calls with timing information:

```java
@Bean
public LoggingServerInterceptor loggingInterceptor() {
    return new LoggingServerInterceptor();
}
```

#### ExceptionHandlingInterceptor

Converts exceptions to gRPC status codes:

| Exception | Status Code |
|-----------|-------------|
| `EntityNotFoundException` | `NOT_FOUND` |
| `ConstraintViolationException` | `INVALID_ARGUMENT` |
| `IllegalArgumentException` | `INVALID_ARGUMENT` |
| `IllegalStateException` | `FAILED_PRECONDITION` |
| `OptimisticLockException` | `ABORTED` |
| `SecurityException` | `PERMISSION_DENIED` |
| Generic Exception | `INTERNAL` |

#### AuthenticationServerInterceptor

Token-based authentication:

```java
@Bean
public AuthenticationServerInterceptor authInterceptor(JwtService jwtService) {
    return new AuthenticationServerInterceptor(
        token -> {
            try {
                Claims claims = jwtService.validate(token);
                return AuthResult.success(
                    claims.getSubject(),
                    Set.of(claims.get("roles", String.class).split(","))
                );
            } catch (Exception e) {
                return AuthResult.failure("Invalid token");
            }
        },
        Set.of("health.HealthService/Check") // Excluded methods
    );
}
```

### Client Interceptors

#### AuthenticationClientInterceptor

Adds Bearer token to outgoing requests:

```java
@Bean
public AuthenticationClientInterceptor clientAuthInterceptor(TokenProvider tokenProvider) {
    return new AuthenticationClientInterceptor(tokenProvider::getToken);
}
```

### Health Checks

Register health checks for monitoring:

```java
@Component
public class DatabaseHealthCheck implements HealthCheck {

    private final DataSource dataSource;

    @Override
    public String getName() {
        return "database";
    }

    @Override
    public Result check() {
        try (Connection conn = dataSource.getConnection()) {
            return Result.healthy("Connected", Map.of("url", conn.getMetaData().getURL()));
        } catch (SQLException e) {
            return Result.unhealthy("Connection failed: " + e.getMessage());
        }
    }
}
```

## Proto Definitions

The module includes common proto definitions:

### common.proto

- `Timestamp` - Time representation
- `PageRequest` / `PageMetadata` - Pagination
- `OperationResult` - Generic operation results
- `ErrorDetail` - RFC 7807-style errors
- `FieldError` / `ValidationErrors` - Validation errors
- `EntityId` - Entity identifiers (long, string, UUID)
- `AuditInfo` - Audit metadata

### health.proto

- `HealthService` - gRPC health checking protocol
- `HealthCheckRequest` / `HealthCheckResponse`
- `ComponentHealth` - Individual component health

## Dependencies

- gRPC Java 1.72.0
- Protobuf 4.31.1
- gRPC Spring Boot Starter 3.1.0.RELEASE
- Spring Boot 4.0.0

## License

MIT License
