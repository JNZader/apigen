# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- ROADMAP.md with improvement plan
- CHANGELOG.md following Keep a Changelog convention
- README.md for each module (bom, core, security, codegen, server)
- LICENSE file (MIT)

### Changed
- Updated .gitignore to exclude logs, .env, and .claude files

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
