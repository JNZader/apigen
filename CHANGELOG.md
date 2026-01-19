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

### Changed
- Updated .gitignore to exclude logs, .env, and .claude files
- Optimized Docker CI (single platform for main, multi-platform for releases)
- Converted image name to lowercase for registry compatibility

### Security
- Added X-RateLimit-* headers for API throttling visibility
- Added Retry-After header on 429 responses
- Stricter rate limits on authentication endpoints (10 req/min vs 100 req/s)

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
