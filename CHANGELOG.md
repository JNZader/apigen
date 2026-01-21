# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [2.1.6](https://github.com/JNZader/apigen/compare/v2.1.5...v2.1.6) (2026-01-21)

### Bug Fixes

* **codegen:** fix test generators for Spring Boot 4 ([#40](https://github.com/JNZader/apigen/issues/40)) ([6b5b96a](https://github.com/JNZader/apigen/commit/6b5b96a2fe3b01866b3375900a76d756708e0494))

## [2.1.5](https://github.com/JNZader/apigen/compare/v2.1.4...v2.1.5) (2026-01-21)

### Bug Fixes

* **codegen:** correct JSON escaping in IntegrationTestGenerator ([#39](https://github.com/JNZader/apigen/issues/39)) ([da02415](https://github.com/JNZader/apigen/commit/da024157fbbb84b257f606540eeb467ab8520653))

## [2.1.4](https://github.com/JNZader/apigen/compare/v2.1.3...v2.1.4) (2026-01-21)

### Bug Fixes

* **codegen:** correct test generators for Spring Boot 4 compatibility ([#38](https://github.com/JNZader/apigen/issues/38)) ([15bcd62](https://github.com/JNZader/apigen/commit/15bcd62546649332394e6f13c1345b8ef4d181ab))

## [2.1.3](https://github.com/JNZader/apigen/compare/v2.1.2...v2.1.3) (2026-01-21)

### Bug Fixes

* **codegen:** use fully qualified @Order annotation ([#37](https://github.com/JNZader/apigen/issues/37)) ([93cb2d1](https://github.com/JNZader/apigen/commit/93cb2d173fae36de8bc354ac7adee2728da83ecd))

## [2.1.2](https://github.com/JNZader/apigen/compare/v2.1.1...v2.1.2) (2026-01-21)

### Bug Fixes

* **codegen:** update test generation for Spring Boot 4 compatibility ([#35](https://github.com/JNZader/apigen/issues/35)) ([4cd2470](https://github.com/JNZader/apigen/commit/4cd2470f9a354e9d97da4f2443e62f84b392db30))

## [2.1.1](https://github.com/JNZader/apigen/compare/v2.1.0...v2.1.1) (2026-01-21)

### Performance Improvements

* **db:** Phase 16 - Database Optimization ([#34](https://github.com/JNZader/apigen/issues/34)) ([496ce55](https://github.com/JNZader/apigen/commit/496ce55fa785ab2dfce5972205e3b4158eb0c4fd))

## [2.1.0](https://github.com/JNZader/apigen/compare/v2.0.0...v2.1.0) (2026-01-21)

### Features

* Phase 15 - API Enhancement ([#33](https://github.com/JNZader/apigen/issues/33)) ([4a39cb6](https://github.com/JNZader/apigen/commit/4a39cb6af41ad4a163ada1e8fc29d005aef062b0))

## [2.0.0](https://github.com/JNZader/apigen/compare/v1.1.1...v2.0.0) (2026-01-21)

### ⚠ BREAKING CHANGES

* Error messages and exception messages are now in English.
Applications using i18n should rely on the i18n module for localization.

Files updated:
- apigen-core: ApiError, ProblemDetail, FilterSpecificationBuilder,
  BaseServiceImpl, SSE components, config classes
- apigen-security: AuthService, JwtService, TokenBlacklistService,
  AccountLockoutService, CustomUserDetailsService, SecurityConfig
- Updated corresponding test files to expect English messages

### Features

* Phase 12 - Code Quality Improvements ([#31](https://github.com/JNZader/apigen/issues/31)) ([c609072](https://github.com/JNZader/apigen/commit/c60907288dc8e5274f4dae6ac6be0cf1685690ee))

## [1.1.1](https://github.com/JNZader/apigen/compare/v1.1.0...v1.1.1) (2026-01-21)

### Performance Improvements

* Phase 11 - Performance Optimization ([#30](https://github.com/JNZader/apigen/issues/30)) ([9e3ae30](https://github.com/JNZader/apigen/commit/9e3ae30715fbf801f8ad44f95a8a52304106250e))

## [1.1.0](https://github.com/JNZader/apigen/compare/v1.0.2...v1.1.0) (2026-01-20)

### Features

* **security:** Phase 10 Security Hardening ([#29](https://github.com/JNZader/apigen/issues/29)) ([244532d](https://github.com/JNZader/apigen/commit/244532d2393238f57b762d903790122ae9357d63))

## [1.0.2](https://github.com/JNZader/apigen/compare/v1.0.1...v1.0.2) (2026-01-20)

### Bug Fixes

* **docker:** add missing modules to Dockerfile ([adfa328](https://github.com/JNZader/apigen/commit/adfa3281dae23bb468a214616a6eaf1e770055ab))

### Documentation

* switch from Docusaurus to Docsify for simpler documentation ([e0f0d6c](https://github.com/JNZader/apigen/commit/e0f0d6cf4cad71afe02c1ec3cfbc0d7036de4208))

## [1.0.1](https://github.com/JNZader/apigen/compare/v1.0.0...v1.0.1) (2026-01-20)

### Bug Fixes

* **docs:** remove non-existent doc references from sidebar ([aaef87a](https://github.com/JNZader/apigen/commit/aaef87a5e275978695e6f86ae4cfc4f10cdc6763))
* **docs:** simplify to use only Redocusaurus for API reference ([2d1523b](https://github.com/JNZader/apigen/commit/2d1523b024063157d9230e3c809d5dd0f12070f8))

### Documentation

* add Docusaurus documentation site and JitPack configuration ([d3f3153](https://github.com/JNZader/apigen/commit/d3f3153bcf7eb137e29eec7844b1fc969fc700e4))

## 1.0.0 (2026-01-20)

### Features

* add code quality tools (Spotless, Errorprone, JaCoCo, ArchUnit) ([3b231d3](https://github.com/JNZader/apigen/commit/3b231d33207037efed36fae4a3082041ff3e8771))
* **core:** Phase 6 - Additional Features ([#21](https://github.com/JNZader/apigen/issues/21)) ([2930355](https://github.com/JNZader/apigen/commit/2930355346d0308deaad073dccb07295272b8749))
* Phase 3 - Advanced Testing (PIT + JMH) ([#18](https://github.com/JNZader/apigen/issues/18)) ([074297e](https://github.com/JNZader/apigen/commit/074297e6aafd3936f319fbb55a115aa65090de0b))
* Phase 4 - Performance & Feature Flags ([#19](https://github.com/JNZader/apigen/issues/19)) ([7f0469b](https://github.com/JNZader/apigen/commit/7f0469b3d8424878f9d5952f06f2b4ea2edf8ec9))
* Phase 7 - Advanced Architecture ([#27](https://github.com/JNZader/apigen/issues/27)) ([5394cec](https://github.com/JNZader/apigen/commit/5394ceccb069e73a95a9fec46a9f16e67e0606ac))
* Phase 8 - Release & Deploy ([#28](https://github.com/JNZader/apigen/issues/28)) ([f1a42de](https://github.com/JNZader/apigen/commit/f1a42de65c0a3cb3f2d8def25ab3372f62dcb5e1))
* **security:** add configurable headers and Bucket4j rate limiting ([6daac47](https://github.com/JNZader/apigen/commit/6daac475ef9c8bc0f24d298f7607e28f8e70eead))
* **security:** add JWT key rotation support ([50cc0a7](https://github.com/JNZader/apigen/commit/50cc0a7ff803044adbab322faa4366bd82b46a71))

### Bug Fixes

* **ci:** convert image name to lowercase for registry ([1fa10c8](https://github.com/JNZader/apigen/commit/1fa10c8236d4a2c4047ef847b82116239be97fd6))
* **ci:** optimize Docker build and fix attestation ([aeb462e](https://github.com/JNZader/apigen/commit/aeb462e303dc2bcec62a30c4ce11df1af5475273))
* **docker:** update Dockerfile for multi-module project structure ([fc934eb](https://github.com/JNZader/apigen/commit/fc934eb6a2697a0578007e4436abedbd64c0d155))
* **gateway:** update to Spring Cloud 2025.1.0 artifact names ([1ae546a](https://github.com/JNZader/apigen/commit/1ae546ab80106ea07ddf837ae732372db704a1bb))
* **test:** fix CI test failures ([7564546](https://github.com/JNZader/apigen/commit/756454638dff42bf72dc00a9fae9078d62073bd8))

### Documentation

* add CHANGELOG.md and update gitignore ([6069add](https://github.com/JNZader/apigen/commit/6069add24d8c8b7a4de6dc6bb012dcf537b50c67))
* Phase 5 - Documentation improvements ([#20](https://github.com/JNZader/apigen/issues/20)) ([e48bcb1](https://github.com/JNZader/apigen/commit/e48bcb1f376161286d2e72588b49fcc4b62e5a67))
* update CHANGELOG and ROADMAP with Phase 0-2 completion ([bb48f07](https://github.com/JNZader/apigen/commit/bb48f07539a599f948fcea089eec1aac54a9b72c))
* update CHANGELOG with Spring Cloud 2025.1.0 artifact name fix ([b25268c](https://github.com/JNZader/apigen/commit/b25268c768fe44d7b6a0928386e26e2fd9e4e469))

### Build System

* **deps:** bump ch.qos.logback:logback-classic from 1.5.16 to 1.5.25 ([#17](https://github.com/JNZader/apigen/issues/17)) ([1b7bb5b](https://github.com/JNZader/apigen/commit/1b7bb5bfb1d61c5496a701d6d7203fbb40deb025))
* **deps:** bump com.diffplug.spotless from 7.0.2 to 8.1.0 ([#11](https://github.com/JNZader/apigen/issues/11)) ([a5237c6](https://github.com/JNZader/apigen/commit/a5237c6e92db5417788b664a1673b4482e4d8148))
* **deps:** bump com.google.errorprone:error_prone_core ([#22](https://github.com/JNZader/apigen/issues/22)) ([0b36b87](https://github.com/JNZader/apigen/commit/0b36b872a8fd4dd4ea4cf4240e570512c726723f))
* **deps:** bump com.tngtech.archunit:archunit-junit5 from 1.4.0 to 1.4.1 ([#24](https://github.com/JNZader/apigen/issues/24)) ([7ac47d0](https://github.com/JNZader/apigen/commit/7ac47d0445d9c6583e000f6f825e062e078cc668))
* **deps:** bump io.micrometer:micrometer-tracing-bridge-otel ([4ab4d18](https://github.com/JNZader/apigen/commit/4ab4d1846bf767427458449fe7178a798c88edc1))
* **deps:** bump net.ltgt.errorprone from 4.1.0 to 4.4.0 ([#16](https://github.com/JNZader/apigen/issues/16)) ([75de3ec](https://github.com/JNZader/apigen/commit/75de3ec7830dcb0a0b384cc90a8df632ea57d457))
* **deps:** bump org.mockito:mockito-junit-jupiter ([#26](https://github.com/JNZader/apigen/issues/26)) ([68b25c1](https://github.com/JNZader/apigen/commit/68b25c18e4d55a1ea97db04c511878139a379349))
* **deps:** bump org.slf4j:slf4j-api from 2.0.16 to 2.0.17 ([#12](https://github.com/JNZader/apigen/issues/12)) ([8dbd57b](https://github.com/JNZader/apigen/commit/8dbd57b95a636b2dda95cf90515542d4b3bd60a4))
* **deps:** bump org.sonarqube from 6.3.1.5724 to 7.2.2.6593 ([b8316de](https://github.com/JNZader/apigen/commit/b8316de6b17e9d3a06013a0564af4bbf365e0265))
* **deps:** bump org.springdoc:springdoc-openapi-starter-webmvc-ui ([09fe95e](https://github.com/JNZader/apigen/commit/09fe95ed04264410042121927b4810aee4907873))
* **deps:** bump org.springframework.boot from 4.0.0 to 4.0.1 ([b4fa7c0](https://github.com/JNZader/apigen/commit/b4fa7c012079ef8b8fb2e0230912fb30dbc3588e))
* **deps:** bump org.springframework.cloud:spring-cloud-dependencies ([#25](https://github.com/JNZader/apigen/issues/25)) ([4b920f5](https://github.com/JNZader/apigen/commit/4b920f585f2f6f17fca7fe3db83ffe163fc96a58))
* **deps:** bump org.testcontainers:testcontainers-junit-jupiter ([daf8a3f](https://github.com/JNZader/apigen/commit/daf8a3f5231d5ec594860dca38d750f5c30c651d))
* **deps:** bump testcontainersVersion from 1.20.4 to 1.21.4 ([#14](https://github.com/JNZader/apigen/issues/14)) ([5b77870](https://github.com/JNZader/apigen/commit/5b778708704dff149edf80721bcc1c7ed60e6932))

### CI/CD

* **deps:** bump actions/attest-build-provenance from 1 to 3 ([#13](https://github.com/JNZader/apigen/issues/13)) ([a557623](https://github.com/JNZader/apigen/commit/a5576233024491b17681972b12dac95a22e11336))
* **deps:** bump actions/checkout from 4 to 6 ([c2ee47d](https://github.com/JNZader/apigen/commit/c2ee47dadf5cf23cfbef4d1317da1038a6bc4534))
* **deps:** bump actions/setup-java from 4 to 5 ([c5b5e89](https://github.com/JNZader/apigen/commit/c5b5e89c19aa3ca371b8f22c42de4ad3a11dd78f))
* **deps:** bump actions/upload-artifact from 4 to 6 ([90a9c8c](https://github.com/JNZader/apigen/commit/90a9c8c3fdbc631a5157a9b3db1fe27c3a23bab0))
* **deps:** bump github/codeql-action from 3 to 4 ([#15](https://github.com/JNZader/apigen/issues/15)) ([34c0df0](https://github.com/JNZader/apigen/commit/34c0df03bfd5418e20dfb01d2ded0f63fae8fc0a))
* **deps:** bump gradle/actions from 4 to 5 ([2b270e1](https://github.com/JNZader/apigen/commit/2b270e107d08f8731ed4016e66eeca47edf6d88a))

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

#### Fase 8: Release & Deploy
- **Semantic-release (8.1)**: Automated versioning and release workflow
  - GitHub Actions workflow `.github/workflows/release.yml`
  - Conventional Commits parsing for automatic version bumps
  - Automatic CHANGELOG generation on release
  - GitHub Releases with release notes
- **GraalVM Native Image (8.2)**: Ahead-of-time compilation support
  - `org.graalvm.buildtools.native` plugin 0.10.6 in apigen-example
  - Spring AOT processing enabled
  - Native image build configuration with URL protocols, charsets
  - GraalVM JDK 25 toolchain support
- **PKCE OAuth2 (8.3)**: Enhanced OAuth2 security for public clients
  - `PKCEService` implementing RFC 7636 (Proof Key for Code Exchange)
  - Code verifier generation (43-128 chars, URL-safe)
  - S256 (SHA-256) and plain code challenge methods
  - `PKCEAuthorizationStore` for authorization code lifecycle (single-use, expiration)
  - `OAuth2Controller` with `/oauth2/authorize`, `/oauth2/token`, `/oauth2/revoke` endpoints
  - `PKCETokenRequestDTO` supporting authorization_code and refresh_token grants
  - PKCE helper endpoint `/oauth2/pkce/generate` for development
  - `PkceProperties` configuration class for customization
  - Comprehensive test suite (21 PKCEService tests, store tests)

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
  - Spring Cloud Gateway 2025.1.0 (spring-cloud-gateway-server-webflux), Resilience4j reactor integration
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

### Fixed
- Updated apigen-gateway to use `spring-cloud-gateway-server-webflux` (replaces deprecated `spring-cloud-starter-gateway` in Spring Cloud 2025.1.0)
- Excluded eventsourcing infrastructure package from domain event architecture test (StoredEvent is infrastructure, not domain)

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
