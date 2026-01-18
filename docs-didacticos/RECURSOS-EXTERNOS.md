# Recursos Externos por Guia

Este documento contiene videos, documentacion oficial, tutoriales y repositorios de ejemplo para cada guia didactica del proyecto APiGen.

---

## 01-SETUP-INICIAL

### Spring Boot + Gradle

| Tipo | Recurso | URL |
|------|---------|-----|
| Docs | Spring Boot Gradle Plugin Reference | https://docs.spring.io/spring-boot/docs/current/gradle-plugin/reference/htmlsingle/ |
| Docs | Gradle - Building Spring Boot Apps | https://docs.gradle.org/current/samples/sample_building_spring_boot_web_applications.html |
| Tutorial | Java Guides - Spring Boot Gradle (2024) | https://www.javaguides.net/2024/05/spring-boot-gradle-project-example.html |
| Tutorial | HowToDoInJava - Spring Boot + Gradle | https://howtodoinjava.com/spring-boot/create-spring-boot-project-with-gradle/ |
| Tutorial | GeeksforGeeks - Setup Gradle Project | https://www.geeksforgeeks.org/advance-java/spring-boot--setting-up-a-spring-boot-project-with-gradle/ |

### Docker Compose + PostgreSQL

| Tipo | Recurso | URL |
|------|---------|-----|
| Tutorial | Baeldung - Spring Boot PostgreSQL Docker | https://www.baeldung.com/spring-boot-postgresql-docker |
| Tutorial | Java Guides - Docker Compose PostgreSQL | https://www.javaguides.net/2024/05/spring-boot-with-postgresql-using-docker-compose.html |
| Tutorial | DEV.to - Docker Compose Spring + Postgres | https://dev.to/tienbku/docker-compose-spring-boot-and-postgres-example-4l82 |
| Tutorial | JavaToDev - Docker Compose Guide | https://www.javatodev.com/docker-compose-spring-boot-postgresql/ |
| GitHub | bezkoder/docker-compose-spring-boot-postgres | https://github.com/bezkoder/docker-compose-spring-boot-postgres |

---

## 02-DOMINIO-BASE

### JPA Auditing (@CreatedBy, @LastModifiedBy)

| Tipo | Recurso | URL |
|------|---------|-----|
| Docs | Spring Data JPA - Auditing (Oficial) | https://docs.spring.io/spring-data/jpa/reference/auditing.html |
| Docs | Spring Data Commons - Auditing | https://docs.spring.io/spring-data/commons/reference/auditing.html |
| Tutorial | Baeldung - Auditing with JPA | https://www.baeldung.com/database-auditing-jpa |
| Tutorial | LogicBig - @CreatedBy @LastModifiedBy | https://www.logicbig.com/tutorials/spring-framework/spring-data/created-by-and-last-modified-by.html |
| Tutorial | Medium - JPA Auditing + Spring Security | https://medium.com/thefreshwrites/jpa-auditing-spring-boot-spring-security-575c77867570 |
| GitHub | Spring Data Audit Example | https://rashidi.github.io/spring-boot-data-audit/ |

### Soft Delete + @SQLRestriction

| Tipo | Recurso | URL |
|------|---------|-----|
| Docs | Hibernate @SQLRestriction JavaDoc | https://docs.jboss.org/hibernate/orm/6.4/javadocs/org/hibernate/annotations/SQLRestriction.html |
| Tutorial | Baeldung - Soft Delete with Spring JPA | https://www.baeldung.com/spring-jpa-soft-delete |
| Tutorial | Baeldung - @SoftDelete Annotation (Hibernate 6.4+) | https://www.baeldung.com/java-hibernate-softdelete-annotation |
| Tutorial | Thorben Janssen - Implement Soft Delete | https://thorben-janssen.com/implement-soft-delete-hibernate/ |
| Tutorial | Vlad Mihalcea - Best Way to Soft Delete | https://vladmihalcea.com/the-best-way-to-soft-delete-with-hibernate/ |
| Tutorial | Medium - Soft Delete Best Practices | https://medium.com/@samrat.alam/soft-delete-in-spring-boot-jpa-best-practices-real-world-implementation-2d831e60bb3e |

### Domain Events

| Tipo | Recurso | URL |
|------|---------|-----|
| Docs | Spring Data JPA - Domain Events | https://docs.spring.io/spring-data/jpa/reference/repositories/core-domain-events.html |
| Tutorial | Baeldung - DDD Aggregates @DomainEvents | https://www.baeldung.com/spring-data-ddd |
| Tutorial | DEV.to - Spring Data Domain Events | https://dev.to/kirekov/spring-data-power-of-domain-events-2okm |
| Tutorial | DEV.to - Publishing Domain Events | https://dev.to/peholmst/publishing-domain-events-with-spring-data-53m2 |

---

## 03-APLICACION-BASE

### MapStruct (DTO Mapping)

| Tipo | Recurso | URL |
|------|---------|-----|
| Docs | MapStruct Official | https://mapstruct.org/ |
| Tutorial | Baeldung - Quick Guide to MapStruct | https://www.baeldung.com/mapstruct |
| Tutorial | Java Guides - MapStruct Tutorial (2024) | https://www.javaguides.net/2024/05/spring-boot-mapstruct-tutorial.html |
| Tutorial | Auth0 - Map JPA Entities to DTOs | https://auth0.com/blog/how-to-automatically-map-jpa-entities-into-dtos-in-spring-boot-using-mapstruct/ |
| Tutorial | Medium - DTOs with Records + MapStruct | https://medium.com/@cat.edelveis/how-to-create-dtos-with-records-and-mapstruct-in-spring-boot-ed9931ba5191 |
| Tutorial | Medium - MapStruct + Vavr + Lombok | https://medium.com/@tijl.b/a-guide-to-mapstruct-with-spring-boot-vavr-lombok-d5325b436220 |

### Validation Groups (@Valid, OnCreate, OnUpdate)

| Tipo | Recurso | URL |
|------|---------|-----|
| Tutorial | Reflectoring - Validation Complete Guide | https://reflectoring.io/bean-validation-with-spring-boot/ |
| Tutorial | DEV.to - @Valid and @Validated Guide | https://dev.to/gianfcop98/spring-boot-and-validation-a-complete-guide-with-valid-and-validated-471p |
| Tutorial | Medium - Validation with Groups | https://medium.com/@AlexanderObregon/request-body-validation-in-spring-boot-with-groups-bd2ca1033bdb |
| Tutorial | Medium - Deep Dive Validation Groups | https://medium.com/@piratedaman/a-deep-dive-into-validation-in-spring-boot-with-groups-validated-52e7d736e114 |
| Tutorial | Medium - Conditional Validation | https://medium.com/@sanyal.s271/using-groups-for-conditional-validation-in-spring-boot-b561c0f377db |

### Result Pattern (Either Monad)

| Tipo | Recurso | URL |
|------|---------|-----|
| Tutorial | Medium - Either Monads in Java | https://medium.com/@samuelavike/either-monads-in-java-elegant-error-handling-for-the-modern-developer-423bbf7300e6 |
| Tutorial | Medium - Monadic Error Handling Java | https://medium.com/@tyrion85/monadic-error-handling-in-java-a207ce01559 |
| Tutorial | Java Design Patterns - Monad | https://java-design-patterns.com/patterns/monad/ |
| Tutorial | Medium - Result and Log Monads | https://medium.com/@afcastano/monads-for-java-developers-part-2-the-result-and-log-monads-a9ecc0f231bb |
| Book | Manning - FP in Java Chapter 7 | https://livebook.manning.com/book/functional-programming-in-java/chapter-7/ |

---

## 04-INFRAESTRUCTURA-BASE

### Spring HATEOAS

| Tipo | Recurso | URL |
|------|---------|-----|
| Docs | Spring HATEOAS Reference (Oficial) | https://docs.spring.io/spring-hateoas/docs/current/reference/html/ |
| Guide | Spring.io - Building HATEOAS Service | https://spring.io/guides/gs/rest-hateoas/ |
| Tutorial | Baeldung - Intro to Spring HATEOAS | https://www.baeldung.com/spring-hateoas-tutorial |
| Tutorial | CodeJava - REST API CRUD with HATEOAS | https://www.codejava.net/frameworks/spring-boot/rest-api-crud-with-hateoas-tutorial |
| Tutorial | HowToDoInJava - HATEOAS Links | https://howtodoinjava.com/spring-boot/spring-boot-hateoas-example/ |
| Tutorial | Devot - HATEOAS Best Practices | https://devot.team/blog/spring-hateoas |
| GitHub | spring-projects/spring-hateoas | https://github.com/spring-projects/spring-hateoas |

### GlobalExceptionHandler + RFC 7807 ProblemDetail

| Tipo | Recurso | URL |
|------|---------|-----|
| Tutorial | Baeldung - ProblemDetail in Spring Boot | https://www.baeldung.com/spring-boot-return-errors-problemdetail |
| Tutorial | HowToDoInJava - ProblemDetail ErrorResponse | https://howtodoinjava.com/spring-mvc/spring-problemdetail-errorresponse/ |
| Tutorial | SivaLabs - Spring Boot 3 Problem Details | https://www.sivalabs.in/blog/spring-boot-3-error-reporting-using-problem-details/ |
| Tutorial | DEV.to - RFC-9457 Error Handling | https://dev.to/abdelrani/error-handling-in-spring-web-using-rfc-9457-specification-5dj1 |
| Tutorial | GeeksforGeeks - ProblemDetail | https://www.geeksforgeeks.org/advance-java/returning-errors-using-problemdetail-in-spring-boot/ |

### JPA Specifications (Dynamic Queries)

| Tipo | Recurso | URL |
|------|---------|-----|
| Docs | Spring Blog - Specifications & Querydsl | https://spring.io/blog/2011/04/26/advanced-spring-data-jpa-specifications-and-querydsl/ |
| Tutorial | Reflectoring - Spring Data Specifications | https://reflectoring.io/spring-data-specifications/ |
| Tutorial | Attacomsian - Dynamic Queries | https://attacomsian.com/blog/spring-data-jpa-specifications |
| Tutorial | Medium - Dynamic Query Specification | https://medium.com/@bubu.tripathy/dynamic-query-with-specification-interface-in-spring-data-jpa-ae8764e32162 |
| Tutorial | Asimio Tech - Dynamic SQL Queries | https://tech.asimio.net/2020/11/21/Implementing-dynamic-SQL-queries-using-Spring-Data-JPA-Specification-and-Criteria-API.html |
| GitHub | JPA Specifications Example | https://github.com/AhmetAksunger/Jpa-Specifications-Example |

---

## 05-SEGURIDAD-JWT

### JWT Authentication + Refresh Tokens

| Tipo | Recurso | URL |
|------|---------|-----|
| Tutorial | Java Code Geeks - JWT Refresh Tokens (2024) | https://www.javacodegeeks.com/2024/12/managing-jwt-refresh-tokens-in-spring-security-a-complete-guide.html |
| Tutorial | Medium - Spring Boot 3 JWT Part 3 | https://medium.com/@max.difranco/user-registration-and-jwt-authentication-with-spring-boot-3-part-3-refresh-token-logout-ea0704f1b436 |
| Tutorial | BezKoder - Refresh Token JWT | https://www.bezkoder.com/spring-boot-refresh-token-jwt/ |
| Tutorial | BezKoder - JWT Authentication | https://www.bezkoder.com/spring-boot-jwt-authentication/ |
| Tutorial | Java Guides - JWT Tutorial (2024) | https://www.javaguides.net/2024/01/spring-boot-security-jwt-tutorial.html |
| Tutorial | Medium - Refresh Token + Logout | https://medium.com/@victoronu/implementing-refresh-token-logout-in-a-spring-boot-jwt-application-b9d31de953d6 |
| GitHub | bezkoder/spring-boot-refresh-token-jwt | https://github.com/bezkoder/spring-boot-refresh-token-jwt |
| GitHub | bezkoder/spring-security-refresh-token-jwt | https://github.com/bezkoder/spring-security-refresh-token-jwt |

---

## 06-CACHE-RESILIENCIA

### Caffeine Cache

| Tipo | Recurso | URL |
|------|---------|-----|
| Docs | Spring Boot Caching (Oficial) | https://docs.spring.io/spring-boot/docs/2.1.6.RELEASE/reference/html/boot-features-caching.html |
| Tutorial | Baeldung - Spring Boot Caffeine Cache | https://www.baeldung.com/spring-boot-caffeine-cache |
| Tutorial | HowToDoInJava - Caffeine Cache | https://howtodoinjava.com/spring-boot/spring-boot-caffeine-cache/ |
| Tutorial | DEV.to - Spring Cache with Caffeine | https://dev.to/noelopez/spring-cache-with-caffeine-384l |
| Tutorial | Medium - Multiple Caffeine Caches | https://medium.com/@ronarazi/how-to-configuring-multiple-caffeine-caches-in-spring-boot-using-specification-property-50adaf2d7a1c |
| Tutorial | Multi-Layer Cache (L1+L2) | https://gaetanopiazzolla.github.io/java/2025/01/27/multicache.html |
| GitHub | spring-caffeine-cache-tutorial | https://github.com/mvpjava/spring-caffeine-cache-tutorial |
| GitHub | spring-boot-multilevel-cache-starter | https://github.com/SuppieRK/spring-boot-multilevel-cache-starter |

### Resilience4j (Circuit Breaker, Retry, Rate Limiter)

| Tipo | Recurso | URL |
|------|---------|-----|
| Docs | Resilience4j Official | https://resilience4j.readme.io/docs/getting-started |
| Docs | Getting Started Spring Boot | https://resilience4j.readme.io/docs/getting-started-3 |
| Tutorial | Baeldung - Resilience4j Spring Boot | https://www.baeldung.com/spring-boot-resilience4j |
| Tutorial | Baeldung - Guide to Resilience4j | https://www.baeldung.com/resilience4j |
| Tutorial | GeeksforGeeks - Circuit Breaker | https://www.geeksforgeeks.org/advance-java/spring-boot-circuit-breaker-pattern-with-resilience4j/ |
| Tutorial | Reflectoring - Retry Pattern | https://reflectoring.io/retry-with-springboot-resilience4j/ |
| Tutorial | Mobisoft - Complete Tutorial | https://mobisoftinfotech.com/resources/blog/microservices/resilience4j-circuit-breaker-retry-bulkhead-spring-boot |
| Tutorial | Medium - Circuit Breaker Implementation | https://medium.com/@mustafa_ciminli/implementing-circuit-breaker-with-resilience4j-in-spring-boot-fe8cc9b43e89 |
| Tutorial | Coding Shuttle - All Patterns | https://www.codingshuttle.com/spring-boot-handbook/microservice-circuit-breaker-retry-and-rate-limiter-with-resilience4-j/ |

---

## 07-OBSERVABILIDAD

### Micrometer + Prometheus + OpenTelemetry

| Tipo | Recurso | URL |
|------|---------|-----|
| Docs | Spring.io - Observability Spring Boot 3 | https://spring.io/blog/2022/10/12/observability-with-spring-boot-3/ |
| Docs | Spring.io - OpenTelemetry (2025) | https://spring.io/blog/2025/11/18/opentelemetry-with-spring-boot/ |
| Tutorial | Baeldung - OpenTelemetry Setup | https://www.baeldung.com/spring-boot-opentelemetry-setup |
| Tutorial | Practical Guide OpenTelemetry | https://vorozco.com/blog/2024/2024-11-18-A-practical-guide-spring-boot-open-telemetry.html |
| Tutorial | Grafana Labs - Capture Metrics | https://grafana.com/blog/2022/05/04/how-to-capture-spring-boot-metrics-with-the-opentelemetry-java-instrumentation-agent/ |
| Tutorial | Uptrace - Monitoring Microservices | https://uptrace.dev/blog/spring-boot-microservices-monitoring |
| Tutorial | SigNoz - OpenTelemetry Guide | https://signoz.io/blog/opentelemetry-spring-boot/ |
| Tutorial | Medium - Distributed Tracing | https://medium.com/javarevisited/distributed-request-tracing-spring-boot-3-micrometer-tracing-with-opentelemetry-3fb129ec8753 |
| GitHub | spring-boot-observability (3 Pillars) | https://github.com/blueswen/spring-boot-observability |

---

## 08-TESTING

### TestContainers + PostgreSQL

| Tipo | Recurso | URL |
|------|---------|-----|
| Docs | Testcontainers Official Guide | https://testcontainers.com/guides/testing-spring-boot-rest-api-using-testcontainers/ |
| Docs | Getting Started Java | https://testcontainers.com/guides/getting-started-with-testcontainers-for-java/ |
| Tutorial | Baeldung - TestContainers Integration | https://www.baeldung.com/spring-boot-testcontainers-integration-test |
| Tutorial | JetBrains - Testing Spring Boot (2024) | https://blog.jetbrains.com/idea/2024/12/testing-spring-boot-applications-using-testcontainers/ |
| Tutorial | DEV.to - Integration Tests PostgreSQL | https://dev.to/mspilari/integration-tests-on-spring-boot-with-postgresql-and-testcontainers-4dpc |
| Tutorial | Mkyong - TestContainers Example | https://mkyong.com/spring-boot/spring-boot-testcontainers-example/ |
| Tutorial | BellSW - TestContainers Guide | https://bell-sw.com/blog/how-to-use-testcontainers-with-spring-boot-applications-for-integration-testing/ |
| GitHub | testcontainers-java-spring-boot-quickstart | https://github.com/testcontainers/testcontainers-java-spring-boot-quickstart |

### ArchUnit (Architecture Tests)

| Tipo | Recurso | URL |
|------|---------|-----|
| Docs | ArchUnit User Guide | https://www.archunit.org/userguide/html/000_Index.html |
| Docs | ArchUnit Official | https://www.archunit.org/ |
| Tutorial | Baeldung - Introduction to ArchUnit | https://www.baeldung.com/java-archunit-intro |
| Tutorial | DZone - Unit Test Architecture | https://dzone.com/articles/spring-boot-unit-test-your-project-architecture-wi |
| Tutorial | Reflectoring - Enforce Architecture | https://reflectoring.io/enforce-architecture-with-arch-unit/ |
| Tutorial | Medium - Spring Boot 3 ArchUnit | https://boottechnologies-ci.medium.com/spring-boot-3-unit-testing-project-architecture-with-archunit-9b7a4a31271a |
| Tutorial | Cloudflight - Intro to ArchUnit | https://engineering.cloudflight.io/archunit-linting-your-achitecture-with-unit-tests |
| GitHub | archunit-examples (Ports & Adapters) | https://github.com/JonasHavers/archunit-examples |

---

## ARQUITECTURA-DDD

### Domain-Driven Design + Spring Boot

| Tipo | Recurso | URL |
|------|---------|-----|
| Tutorial | Baeldung - Hexagonal Architecture, DDD | https://www.baeldung.com/hexagonal-architecture-ddd-spring |
| Video | Spring I/O 2024 - Implementing DDD | https://www.youtube.com/watch?v=VGhg6Tfxb60 |
| Video | Spring I/O 2017 - DDD Strategic Design | https://www.youtube.com/watch?v=2OzW5zj9OTU |
| Tutorial | Howik - Implementing DDD in Spring Boot | https://howik.com/implementing-ddd-in-spring-boot |
| Tutorial | Medium - DDD Spring Boot Kata | https://medium.com/@gsigety/domain-driven-design-spring-boot-kata-1-8a85034babec |
| Tutorial | Adrian Kodja - DDD and Spring Boot | https://adriankodja.com/domain-driven-design-ddd-and-spring-boot |
| GitHub | ZaTribune/springboot-ddd-example | https://github.com/ZaTribune/springboot-ddd-example |
| GitHub | sandokandias/spring-boot-ddd | https://github.com/sandokandias/spring-boot-ddd |
| GitHub | ddd-structure-demo | https://github.com/chatchatabc/ddd-structure-demo |

### Recursos en Espanol

| Tipo | Recurso | URL |
|------|---------|-----|
| Curso | EDteam - Curso DDD | https://ed.team/cursos/ddd |
| Curso | Udemy - Arquitectura Moderna DDD | https://www.udemy.com/course/arquitectura-software-moderna-ddd-eventos-microservicios-cqrs/ |
| GitHub | Asombroso DDD (Lista Curada) | https://github.com/ddd-espanol/asombroso-ddd |
| Tutorial | Medium - DDD Principios (ES) | https://medium.com/@jonathanloscalzo/domain-driven-design-principios-beneficios-y-elementos-primera-parte-aad90f30aa35 |

---

## PATRONES-DISENO

### Patrones Generales

| Patron | Recurso | URL |
|--------|---------|-----|
| Result/Either | Medium - Either Monads Java | https://medium.com/@samuelavike/either-monads-in-java-elegant-error-handling-for-the-modern-developer-423bbf7300e6 |
| Repository | Baeldung - Spring Data DDD | https://www.baeldung.com/spring-data-ddd |
| Specification | Reflectoring - Spring Data Specs | https://reflectoring.io/spring-data-specifications/ |
| Circuit Breaker | Baeldung - Resilience4j | https://www.baeldung.com/spring-boot-resilience4j |
| Soft Delete | Baeldung - Soft Delete JPA | https://www.baeldung.com/spring-jpa-soft-delete |
| Builder | Lombok @Builder | https://projectlombok.org/features/Builder |
| Domain Events | Spring Data Domain Events | https://docs.spring.io/spring-data/jpa/reference/repositories/core-domain-events.html |

### Spring Events

| Tipo | Recurso | URL |
|------|---------|-----|
| Docs | Spring Framework - Transaction Events | https://docs.spring.io/spring-framework/reference/data-access/transaction/event.html |
| Tutorial | Baeldung - Spring Events | https://www.baeldung.com/spring-events |
| Tutorial | Reflectoring - Application Events | https://reflectoring.io/spring-boot-application-events-explained/ |
| Tutorial | DEV.to - TransactionalEventListener | https://dev.to/haraf/understanding-transactioneventlistener-in-spring-boot-use-cases-real-time-examples-and-4aof |
| Tutorial | DZone - @TransactionalEventListener | https://dzone.com/articles/transaction-synchronization-and-spring-application |

---

## Videos YouTube Recomendados

### Canales en Ingles

| Canal | Contenido | URL |
|-------|-----------|-----|
| Spring I/O | Conferencias oficiales Spring | https://www.youtube.com/@SpringIOConference |
| Amigoscode | Spring Boot tutorials | https://www.youtube.com/@aabornamigoscode |
| Java Brains | Spring Security, Microservices | https://www.youtube.com/@Java.Brains |
| Daily Code Buffer | Spring Boot completo | https://www.youtube.com/@DailyCodeBuffer |
| Bouali Ali | Spring Security JWT | https://www.youtube.com/@BousliAli |

### Videos Especificos

| Tema | Video | URL |
|------|-------|-----|
| DDD + Spring | Implementing DDD with Spring (2024) | https://www.youtube.com/watch?v=VGhg6Tfxb60 |
| DDD Strategic | DDD Strategic Design Spring Boot | https://www.youtube.com/watch?v=2OzW5zj9OTU |
| JWT Complete | Spring Security 6 + JWT | Buscar en Amigoscode/Bouali Ali |
| TestContainers | Testing with TestContainers | Buscar en Daily Code Buffer |

---

## Libros Recomendados

| Libro | Autor | Tema |
|-------|-------|------|
| Domain-Driven Design | Eric Evans | DDD Original |
| Implementing DDD | Vaughn Vernon | DDD Practico |
| Spring in Action | Craig Walls | Spring Boot |
| Cloud Native Java | Josh Long | Spring Cloud |
| Functional Programming in Java | Pierre-Yves Saumont | Result Pattern |

---

## Orden de Estudio Sugerido

```
Semana 1: Fundamentos
├── 01-SETUP-INICIAL
│   └── Spring Initializr + Gradle + Docker
└── 02-DOMINIO-BASE
    └── Auditing + Soft Delete + Events

Semana 2: Aplicacion
├── 03-APLICACION-BASE
│   └── MapStruct + Validation + Result Pattern
└── PATRONES-DISENO (Referencia)

Semana 3: Infraestructura
├── 04-INFRAESTRUCTURA-BASE
│   └── Controllers + HATEOAS + Exceptions
└── ARQUITECTURA-DDD (Profundizar)

Semana 4: Seguridad
└── 05-SEGURIDAD-JWT
    └── JWT + Refresh Tokens + Roles

Semana 5: Produccion
├── 06-CACHE-RESILIENCIA
│   └── Caffeine + Resilience4j
└── 07-OBSERVABILIDAD
    └── Micrometer + Prometheus + Tracing

Semana 6: Calidad
├── 08-TESTING
│   └── TestContainers + ArchUnit
└── 09-GENERADORES
    └── OpenAPI + SDK Generation
```

---

*Documento generado automaticamente para el proyecto APiGen*
*Ultima actualizacion: Noviembre 2025*
