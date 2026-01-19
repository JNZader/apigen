# APiGen Architecture - C4 Model

This document describes the APiGen architecture using the C4 model with Mermaid diagrams.

## Level 1: System Context Diagram

Shows how APiGen fits into the broader system landscape.

```mermaid
C4Context
    title System Context Diagram - APiGen

    Person(developer, "Developer", "Uses APiGen to build REST APIs")
    Person(enduser, "End User", "Consumes the generated APIs")

    System(apigen, "APiGen", "REST API Framework for Spring Boot")

    System_Ext(database, "Database", "PostgreSQL, MySQL, H2")
    System_Ext(cache, "Cache", "Redis, Caffeine")
    System_Ext(monitoring, "Monitoring", "Prometheus, Grafana")
    System_Ext(tracing, "Tracing", "OpenTelemetry, Jaeger")

    Rel(developer, apigen, "Extends base classes, configures")
    Rel(enduser, apigen, "HTTP requests", "REST/JSON")
    Rel(apigen, database, "Reads/Writes", "JDBC/JPA")
    Rel(apigen, cache, "Caches data", "Redis/Caffeine")
    Rel(apigen, monitoring, "Exports metrics", "Prometheus")
    Rel(apigen, tracing, "Sends traces", "OTLP")
```

## Level 2: Container Diagram

Shows the major modules that compose APiGen.

```mermaid
C4Container
    title Container Diagram - APiGen Modules

    Person(developer, "Developer", "Builds APIs with APiGen")

    Container_Boundary(apigen, "APiGen Framework") {
        Container(core, "apigen-core", "Java/Spring Boot", "Base entities, services, controllers, CRUD operations")
        Container(security, "apigen-security", "Java/Spring Security", "JWT auth, rate limiting, security headers")
        Container(codegen, "apigen-codegen", "Java", "Code generation from SQL schemas")
        Container(bom, "apigen-bom", "Gradle BOM", "Centralized dependency management")
        Container(example, "apigen-example", "Java/Spring Boot", "Reference implementation")
    }

    ContainerDb(db, "Database", "PostgreSQL", "Stores application data")
    Container_Ext(redis, "Redis", "Cache", "Distributed caching & rate limiting")

    Rel(developer, core, "Extends")
    Rel(developer, security, "Configures")
    Rel(developer, codegen, "Generates code")
    Rel(core, db, "JDBC/JPA")
    Rel(core, redis, "Caches")
    Rel(security, redis, "Rate limits")
    Rel(security, core, "Secures")
    Rel(example, core, "Uses")
    Rel(example, security, "Uses")
```

## Level 3: Component Diagram - Core Module

Shows the internal components of apigen-core following hexagonal architecture.

```mermaid
C4Component
    title Component Diagram - apigen-core

    Container_Boundary(core, "apigen-core") {

        Component_Ext(domain, "Domain Layer", "Entities, Value Objects, Domain Events")

        Boundary(application, "Application Layer") {
            Component(baseservice, "BaseService", "Service interface with Result pattern")
            Component(baseserviceimpl, "BaseServiceImpl", "CRUD operations, caching, events")
            Component(batchservice, "BatchService", "Async batch operations with Virtual Threads")
            Component(basedto, "BaseDTO", "Data Transfer Objects interface")
            Component(basemapper, "BaseMapper", "MapStruct mapping interface")
        }

        Boundary(infrastructure, "Infrastructure Layer") {
            Component(basecontroller, "BaseControllerImpl", "REST endpoints, HATEOAS, filtering")
            Component(baserepository, "BaseRepository", "JPA repository with soft delete")
            Component(cacheconfig, "CacheConfig", "Caffeine local cache")
            Component(rediscache, "RedisCacheConfig", "Redis distributed cache")
            Component(openapi, "OpenApiConfig", "Swagger/OpenAPI documentation")
            Component(hikari, "HikariMetricsConfig", "Connection pool metrics")
            Component(togglz, "FeatureFlagConfig", "Togglz feature flags")
        }
    }

    ContainerDb(db, "Database", "PostgreSQL")
    Container_Ext(redis, "Redis", "Cache")

    Rel(basecontroller, baseservice, "Uses")
    Rel(baseserviceimpl, baserepository, "Uses")
    Rel(baseserviceimpl, basemapper, "Uses")
    Rel(baseserviceimpl, cacheconfig, "Caches with")
    Rel(baserepository, db, "Queries")
    Rel(rediscache, redis, "Caches")
```

## Level 3: Component Diagram - Security Module

Shows the internal components of apigen-security.

```mermaid
C4Component
    title Component Diagram - apigen-security

    Container_Boundary(security, "apigen-security") {

        Boundary(domain_sec, "Domain Layer") {
            Component(user, "User", "User entity with roles")
            Component(role, "Role", "Role with permissions")
            Component(token, "TokenBlacklist", "Revoked tokens")
        }

        Boundary(application_sec, "Application Layer") {
            Component(authservice, "AuthService", "Login, logout, refresh tokens")
            Component(jwtservice, "JwtService", "Token generation & validation")
            Component(keyrotation, "KeyRotationService", "JWT key rotation")
        }

        Boundary(infrastructure_sec, "Infrastructure Layer") {
            Component(authcontroller, "AuthController", "Auth REST endpoints")
            Component(jwtfilter, "JwtAuthenticationFilter", "Token validation filter")
            Component(ratelimit, "RateLimitFilter", "Bucket4j rate limiting")
            Component(secheaders, "SecurityHeadersFilter", "CSP, HSTS, etc.")
            Component(secconfig, "SecurityConfig", "Spring Security configuration")
        }
    }

    Container_Ext(redis_rl, "Redis", "Rate limit buckets")

    Rel(authcontroller, authservice, "Uses")
    Rel(authservice, jwtservice, "Uses")
    Rel(jwtservice, keyrotation, "Uses")
    Rel(jwtfilter, jwtservice, "Validates with")
    Rel(ratelimit, redis_rl, "Stores buckets")
```

## Data Flow: Create Resource

```mermaid
sequenceDiagram
    participant Client
    participant Controller as BaseControllerImpl
    participant Service as BaseServiceImpl
    participant Mapper as BaseMapper
    participant Repository as BaseRepository
    participant DB as Database
    participant Cache as CacheManager
    participant Events as DomainEventPublisher

    Client->>Controller: POST /api/resources
    Controller->>Controller: Validate DTO
    Controller->>Service: create(dto)

    Service->>Mapper: toEntity(dto)
    Mapper-->>Service: entity

    Service->>Repository: save(entity)
    Repository->>DB: INSERT
    DB-->>Repository: saved entity

    Service->>Events: publish(CreatedEvent)
    Service->>Cache: evict(listCache)

    Service->>Mapper: toDto(entity)
    Mapper-->>Service: dto

    Service-->>Controller: Result.success(dto)

    Controller->>Controller: Add HATEOAS links
    Controller->>Controller: Set Location header
    Controller->>Controller: Set ETag header

    Controller-->>Client: 201 Created + DTO
```

## Data Flow: Authentication

```mermaid
sequenceDiagram
    participant Client
    participant RateLimit as RateLimitFilter
    participant Auth as AuthController
    participant AuthSvc as AuthService
    participant JWT as JwtService
    participant UserRepo as UserRepository
    participant DB as Database

    Client->>RateLimit: POST /v1/auth/login
    RateLimit->>RateLimit: Check rate limit
    alt Rate limit exceeded
        RateLimit-->>Client: 429 Too Many Requests
    end

    RateLimit->>Auth: Forward request
    Auth->>AuthSvc: login(credentials)

    AuthSvc->>UserRepo: findByUsername
    UserRepo->>DB: SELECT
    DB-->>UserRepo: User

    AuthSvc->>AuthSvc: Verify password

    alt Invalid credentials
        AuthSvc-->>Auth: Result.failure
        Auth-->>Client: 401 Unauthorized
    end

    AuthSvc->>JWT: generateTokens(user)
    JWT->>JWT: Create access token (1h)
    JWT->>JWT: Create refresh token (7d)
    JWT-->>AuthSvc: TokenPair

    AuthSvc-->>Auth: Result.success(AuthResponse)
    Auth-->>Client: 200 OK + Tokens
```

## Feature Flag Flow

```mermaid
flowchart TD
    A[Request] --> B{Feature Enabled?}
    B -->|Yes| C[Execute Feature]
    B -->|No| D[Skip/Fallback]

    C --> E[Response]
    D --> E

    subgraph Togglz
        F[FeatureChecker]
        G[StateRepository]
        H[ApigenFeatures Enum]
    end

    B --> F
    F --> G
    F --> H
```

## Caching Strategy

```mermaid
flowchart TD
    A[Request] --> B{In Cache?}
    B -->|Yes| C[Return Cached]
    B -->|No| D[Query Database]

    D --> E[Store in Cache]
    E --> F[Return Data]

    subgraph "Cache Layers"
        G[L1: Caffeine<br/>Local, Fast]
        H[L2: Redis<br/>Distributed, Shared]
    end

    B --> G
    G -->|Miss| H
    H -->|Miss| D

    subgraph "Eviction"
        I[TTL Expiry]
        J[Manual Eviction]
        K[Size-based]
    end
```

## Module Dependencies

```mermaid
graph TD
    BOM[apigen-bom] --> CORE[apigen-core]
    BOM --> SEC[apigen-security]
    BOM --> CODEGEN[apigen-codegen]

    CORE --> SEC
    CORE --> EXAMPLE[apigen-example]
    SEC --> EXAMPLE

    CODEGEN --> SERVER[apigen-server]

    subgraph "External"
        SPRING[Spring Boot 4]
        JPA[Spring Data JPA]
        SECURITY[Spring Security]
        MAPSTRUCT[MapStruct]
        TOGGLZ[Togglz]
        RESILIENCE[Resilience4j]
    end

    CORE --> SPRING
    CORE --> JPA
    CORE --> MAPSTRUCT
    CORE --> TOGGLZ
    CORE --> RESILIENCE
    SEC --> SECURITY
```

## Deployment Architecture

```mermaid
C4Deployment
    title Deployment Diagram - Production

    Deployment_Node(cloud, "Cloud Provider", "AWS/GCP/Azure") {
        Deployment_Node(k8s, "Kubernetes Cluster") {

            Deployment_Node(app_ns, "app namespace") {
                Container(api1, "APiGen API", "Spring Boot", "Instance 1")
                Container(api2, "APiGen API", "Spring Boot", "Instance 2")
                Container(api3, "APiGen API", "Spring Boot", "Instance 3")
            }

            Deployment_Node(data_ns, "data namespace") {
                ContainerDb(pg, "PostgreSQL", "Primary + Replica")
                Container(redis_cluster, "Redis Cluster", "3 nodes")
            }

            Deployment_Node(monitoring_ns, "monitoring namespace") {
                Container(prometheus, "Prometheus", "Metrics")
                Container(grafana, "Grafana", "Dashboards")
                Container(jaeger, "Jaeger", "Tracing")
            }
        }

        Deployment_Node(lb, "Load Balancer") {
            Container(ingress, "Ingress Controller", "NGINX")
        }
    }

    Rel(ingress, api1, "Routes to")
    Rel(ingress, api2, "Routes to")
    Rel(ingress, api3, "Routes to")
    Rel(api1, pg, "Queries")
    Rel(api1, redis_cluster, "Caches")
    Rel(api1, prometheus, "Metrics")
```

---

## Legend

| Symbol | Meaning |
|--------|---------|
| Person | Human user/actor |
| System | Software system |
| Container | Deployable unit (JAR, service) |
| Component | Code module/package |
| Database | Data store |
| _Ext | External system |

## References

- [C4 Model](https://c4model.com/)
- [Mermaid C4 Diagrams](https://mermaid.js.org/syntax/c4.html)
- [APiGen Source](https://github.com/JNZader/apigen)
