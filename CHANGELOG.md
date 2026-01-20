# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

#### Fase 0: Fundamentos DevOps
- ROADMAP.md with improvement plan
- CHANGELOG.md following Keep a Changelog convention
- README.md for each module (bom, core, security, codegen, server)
- LICENSE file (MIT)

#### Fase 1: Calidad de Código
- Spotless plugin (google-java-format) for consistent formatting
- Error Prone static analysis for compile-time bug detection
- JaCoCo code coverage with 70% minimum threshold
- ArchUnit tests for architecture validation (hexagonal, naming conventions)
- Pre-commit hooks via Gradle task for validation before commits

#### Fase 2: Seguridad Básica
- Configurable security headers (CSP, HSTS, Referrer-Policy, Permissions-Policy)
- Rate limiting with Bucket4j 8.16.0 (in-memory + Redis distributed)
- JWT key rotation support with `kid` header for zero-downtime rotation
- Multiple signing keys registry for transition periods

#### Fase 3: Testing Avanzado
- PIT mutation testing with Gradle plugin 1.19.0-rc.2
- JUnit 5 integration (pitest 1.22.0, junit5-plugin 1.2.1)
- Mutation threshold 40%, coverage threshold 50%
- `pitestAll` aggregate task for all modules
- Results: core 68%, security 60%, codegen 67% mutation coverage
- JMH benchmark infrastructure with plugin v0.7.3
- ResultBenchmark demonstrating ~1M+ ops/ms for core operations
- `jmhAll` aggregate task for all modules
- **Contract Testing (3.2)**: Spring Cloud Contract consumer-driven contract tests
  - Spring Cloud Contract Verifier 5.0.1 (Gradle plugin)
  - Spring Cloud Dependencies 2025.1.0 BOM for Spring Boot 4.0 compatibility
  - REST-Assured spring-mock-mvc 5.5.7 (Spring Framework 7 compatible)
  - Groovy DSL contracts for REST endpoint validation
  - Contracts for pagination (`findAll`), count headers, error responses (400, 404, 412)
  - RFC 7807 Problem Detail format validation (`application/problem+json`)
  - Optimistic concurrency control testing (If-Match/ETag headers)
  - `BaseContractTest` base class with test entity setup
  - `contractTest` source set and Gradle task integration
  - 6 contract tests covering REST API compliance

#### Fase 4: Rendimiento + Feature Flags
- Togglz feature flags with manual configuration (Spring Boot 4 compatible)
- Redis distributed cache support (RedisCacheConfig) as alternative to Caffeine
- HikariCP metrics exposed to Prometheus (HikariMetricsConfig)
- N+1 query detection via Hibernate Statistics (QueryAnalysisConfig)
- Async batch operations with Virtual Threads (BatchService)
- FeatureChecker utility for runtime feature state checks
- QueryAssertions for test query count validation

#### Fase 5: Documentación
- OpenAPI examples with @Schema annotations on all DTOs (security, example modules)
- Enhanced OpenApiConfig with security schemes and error response schemas
- RFC 7807 Problem Detail examples for all error types (400, 401, 403, 404, 409, 412, 500)
- C4 architecture diagrams (Context, Container, Component levels) in Mermaid format
- Sequence diagrams for Create Resource and Authentication flows
- Updated FEATURES.md with Phase 4 features documentation (sections 17-21)
- Added docs/architecture folder with C4_ARCHITECTURE.md

#### Fase 6: Features Adicionales (Partial)
- **Internationalization (i18n)**: Full i18n support for error messages
  - `MessageService` for retrieving localized messages
  - `I18nConfig` with Accept-Language header locale resolution
  - Support for English (default) and Spanish locales
  - Message bundles: `messages.properties`, `messages_es.properties`
  - LocaleChangeInterceptor for `?lang=` query parameter
  - GlobalExceptionHandler integration for localized RFC 7807 responses
  - Comprehensive test coverage for both locales
- **Webhooks System**: Event-driven webhook notifications
  - `WebhookEvent` enum with 13 event types (entity lifecycle, batch, user, security, system)
  - `WebhookPayload` record with builder pattern for event data
  - `WebhookSubscription` record for managing webhook endpoints
  - `WebhookDelivery` record for tracking delivery status (SUCCESS, FAILED_WILL_RETRY, FAILED_PERMANENT)
  - `WebhookSignature` utility with HMAC-SHA256 signatures for request authentication
  - `WebhookService` with async delivery using virtual threads
  - Configurable retry logic with exponential backoff (base delay, max delay, max retries)
  - `WebhookSubscriptionRepository` interface with `InMemoryWebhookSubscriptionRepository` implementation
  - `WebhookAutoConfiguration` for Spring Boot auto-configuration (`apigen.webhooks.enabled=true`)
  - Ping functionality for testing webhook endpoints
  - Delivery callback support for custom delivery handling
  - Comprehensive test suite (79 tests) covering dispatch, retry logic, signatures, and callbacks
- **Bulk Import/Export**: CSV and Excel support for mass data operations
  - `BulkFormat` enum supporting CSV and EXCEL (XLSX) formats
  - Format detection from filename and content-type
  - `BulkOperationResult` record with operation statistics (success/failure counts, duration)
  - `RecordError` for detailed error tracking (row number, field name, raw value)
  - `BulkImportService` interface with configurable import options
    - `ImportConfig`: skipHeader, stopOnError, batchSize, csvSeparator, dateFormat, sheetName
    - Support for custom processing functions with error handling
    - Validation mode for dry-run imports
  - `BulkExportService` interface with flexible export options
    - `ExportConfig`: includeHeader, csvSeparator, dateFormat, sheetName, autoSizeColumns
    - Field inclusion/exclusion support
    - Stream-based export for large datasets
  - `BulkOperationsService` implementation
    - CSV parsing with OpenCSV 5.9 (CsvToBean/StatefulBeanToCsv)
    - Excel export with Apache POI 5.3.0 (SXSSFWorkbook streaming for memory efficiency)
    - Support for both Record types and POJOs via reflection
    - Automatic header extraction from @CsvBindByName annotations
  - `BulkAutoConfiguration` for Spring Boot auto-configuration (`apigen.bulk.enabled=true`)
  - Comprehensive test suite (26 tests) covering import, export, configurations

#### Fase 7: Arquitectura Avanzada (Partial)
- **API Versioning (7.6)**: Complete API versioning infrastructure
  - `@ApiVersion` annotation for marking API versions on controllers/methods
  - `@DeprecatedVersion` annotation with RFC 8594 support (since, sunset, successor, migrationGuide)
  - `VersioningStrategy` enum: PATH, HEADER, QUERY_PARAM, MEDIA_TYPE
  - `ApiVersionResolver` with builder pattern for configuring resolution strategies
  - `ApiVersionInterceptor` for automatic deprecation headers (Deprecation, Sunset, Link)
  - `VersionContext` thread-local holder for current API version with comparison utilities
  - `ApiVersionAutoConfiguration` for Spring Boot (`apigen.versioning.enabled=true`)
  - Comprehensive test suite (45 tests) covering all resolution strategies and deprecation headers
- **Multi-tenancy (7.1)**: Native SaaS multi-tenancy support
  - `TenantContext` using InheritableThreadLocal for tenant propagation
  - `TenantResolver` with multiple resolution strategies (HEADER, SUBDOMAIN, PATH, JWT_CLAIM)
  - `TenantResolutionStrategy` enum for configurable tenant identification
  - `TenantFilter` servlet filter with excluded paths support and tenant validation
  - `TenantAware` interface for tenant-aware entities
  - `TenantEntityListener` JPA listener for automatic tenant assignment on @PrePersist/@PreUpdate
  - `TenantMismatchException` for cross-tenant access attempts
  - `TenantAutoConfiguration` for Spring Boot (`apigen.multitenancy.enabled=true`)
  - Tenant ID validation with configurable patterns
  - Custom header name support (default: X-Tenant-ID)
  - Comprehensive test suite covering all resolution strategies and filter behavior
- **Event Sourcing (7.2)**: Event sourcing infrastructure for aggregates
  - `DomainEvent` interface for all domain events with metadata support
  - `StoredEvent` JPA entity for persisting events with indexes
  - `EventStore` interface for append-only event storage with optimistic concurrency
  - `JpaEventStore` JPA implementation with Spring event publishing
  - `Snapshot` JPA entity for aggregate state snapshots
  - `EventSourcedAggregate` base class for event-sourced aggregates
  - `AggregateRepository` for loading/saving aggregates with snapshot support
  - `EventSerializer` JSON serializer with Jackson and Java Time support
  - `ConcurrencyException` for optimistic locking conflicts
  - `EventSourcingAutoConfiguration` for Spring Boot (`apigen.eventsourcing.enabled=true`)
  - Comprehensive test suite (37 tests) covering serialization, aggregates, and event store
- **GraphQL Module (7.3)**: New `apigen-graphql` module for GraphQL API layer
  - `SchemaBuilder` fluent API for constructing GraphQL schemas programmatically
  - `GraphQLExecutor` for executing queries, mutations, and subscriptions
  - `GraphQLContext` request-scoped context with user ID, locale, and custom attributes
  - `BaseDataFetcher` base class for data fetchers with utility methods
  - `DataLoaderRegistry` for N+1 query prevention with batched loading
  - `DataLoaderRegistrar` interface for registering DataLoaders
  - `GraphQLExceptionHandler` for RFC 7807-aligned error responses
  - `ApiGenGraphQLError` custom error with type, status code, and extensions
  - `GraphQLErrorType` enum for semantic error classification
  - `GraphQLRequest` record for HTTP request parsing
  - `GraphQLController` HTTP endpoint at `/graphql`
  - `GraphQLAutoConfiguration` for Spring Boot (`apigen.graphql.enabled=true`)
  - GraphQL Java 22.3 and Java DataLoader 3.4.0 integration
  - Comprehensive test suite covering schema building, execution, errors, and data loading
- **gRPC Module (7.4)**: New `apigen-grpc` module for inter-service communication
  - `GrpcServer` wrapper with fluent builder API for server lifecycle management
  - `GrpcChannelFactory` for creating and managing client channels with caching
  - `LoggingServerInterceptor` / `LoggingClientInterceptor` for call logging with timing
  - `ExceptionHandlingInterceptor` mapping exceptions to gRPC status codes
  - `AuthenticationServerInterceptor` for token-based authentication with excluded methods
  - `AuthenticationClientInterceptor` for adding Bearer tokens to outgoing requests
  - `HealthServiceManager` for aggregating health checks from multiple components
  - `HealthCheck` interface with Result record for health status reporting
  - Proto definitions: common.proto (Timestamp, PageRequest, OperationResult, ErrorDetail, EntityId, AuditInfo)
  - Proto definitions: health.proto (HealthService with Check and Watch RPCs)
  - `GrpcAutoConfiguration` for Spring Boot (`apigen.grpc.enabled=true`)
  - gRPC Java 1.72.0, Protobuf 4.31.1, gRPC Spring Boot Starter 3.1.0.RELEASE
  - Comprehensive test suite (37 tests) covering server, client, interceptors, and health checks
- **API Gateway Module (7.5)**: New `apigen-gateway` module for API Gateway functionality
  - `LoggingGatewayFilter` global filter with correlation ID generation and request/response logging
  - `AuthenticationGatewayFilter` JWT-based authentication filter with configurable paths
  - `AuthResult` record for authentication results (success/failure with user details)
  - `RateLimitKeyResolver` with multiple strategies (IP, USER_ID, API_KEY, COMPOSITE, PATH)
  - Path normalization for rate limiting (replaces numeric IDs and UUIDs with placeholders)
  - `CircuitBreakerGatewayFilter` with configurable timeout and custom fallback support
  - `RequestTimingGatewayFilter` for metrics collection (request duration, status codes)
  - `RouteBuilder` fluent API for programmatic route definition
  - `RouteDefinition` record with predicates, filters, circuit breaker, timeout, metadata
  - `DynamicRouteLocator` for runtime route management (add/remove/update routes)
  - `GatewayProperties` configuration with nested classes for rate limiting, circuit breaker, auth, CORS
  - `GatewayAutoConfiguration` for Spring Boot (`apigen.gateway.enabled=true`)
  - Spring Cloud Gateway 2024.0.1, Resilience4j reactor integration
  - Comprehensive test suite (63 tests) covering filters, rate limiting, circuit breaker, routes

### Changed
- Updated .gitignore to exclude logs, .env, and .claude files
- Optimized Docker CI (single platform for main, multi-platform for releases)
- Converted image name to lowercase for registry compatibility
- Updated OpenTelemetry from 1.53.0 to 1.58.0
- Updated JSQLParser from 5.0 to 5.3

### Security
- Added X-RateLimit-* headers for API throttling visibility
- Added Retry-After header on 429 responses
- Stricter rate limits on authentication endpoints (10 req/min vs 100 req/s)
- Fixed CVE-2020-8908, CVE-2023-2976: Upgraded Guava from 30.1-jre to 33.5.0-jre (insecure temp directory)
- Fixed Docker image CVEs: Added `apk upgrade` for libpng, libtasn1, BusyBox vulnerabilities

---

## [1.0.0-SNAPSHOT] - 2025-01-18

### Added

#### Core Module (apigen-core)
- `Base` abstract entity with soft delete, auditing, optimistic locking
- `BaseService` / `BaseServiceImpl` with caching and Result pattern
- `BaseController` / `BaseControllerImpl` with HATEOAS support
- `BaseRepository` with soft delete filtering
- `BaseMapper` interface for MapStruct
- `BaseResourceAssembler` for HATEOAS links
- Dynamic filtering with 12+ operators (eq, like, gte, between, etc.)
- Cursor-based pagination for large datasets
- ETag support for conditional requests
- Domain events (Created, Updated, Deleted, Restored)
- Virtual threads async configuration
- Caffeine caching with configurable TTL
- Resilience4j circuit breaker integration
- OpenTelemetry tracing support
- Spring Boot auto-configuration

#### Security Module (apigen-security)
- JWT authentication with HS512 algorithm
- Access token + Refresh token flow
- Token blacklisting for logout/revocation
- User, Role, Permission entities
- Rate limiting on authentication endpoints
- Security audit logging
- Spring Security auto-configuration

#### CodeGen Module (apigen-codegen)
- SQL schema parser (JSQLParser)
- Entity generator (JPA + Base)
- DTO generator (records + validation)
- Repository generator
- Service generator
- Controller generator
- Mapper generator (MapStruct)
- Migration generator (Flyway)
- Test generator

#### Server Module (apigen-server)
- HTTP endpoint for code generation
- ZIP file response with generated project

#### BOM Module (apigen-bom)
- Centralized dependency version management
- Spring Boot 4.0.0
- Spring Cloud 2024.0.1
- All third-party versions managed

#### Example Module (apigen-example)
- Product entity example
- Complete CRUD demonstration
- Flyway migrations
- Dev/Prod profiles

#### DevOps
- Multi-stage Dockerfile with Alpine JRE 25
- Docker Compose stack (PostgreSQL, Prometheus, Grafana)
- GitHub Actions CI pipeline
- Dependabot configuration
- Issue and PR templates

---

## Types of Changes

- `Added` for new features
- `Changed` for changes in existing functionality
- `Deprecated` for soon-to-be removed features
- `Removed` for now removed features
- `Fixed` for any bug fixes
- `Security` for vulnerability fixes
