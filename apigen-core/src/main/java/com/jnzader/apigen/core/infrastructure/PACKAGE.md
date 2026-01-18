# Core Infrastructure Layer

La capa de infraestructura contiene las **implementaciones técnicas**: controladores REST, configuraciones Spring, filtros HTTP y acceso a datos.

## Estructura

```
infrastructure/
├── controller/          # Controladores REST base
│   ├── BaseController.java
│   └── BaseControllerImpl.java
├── config/              # Configuraciones Spring
│   ├── CacheConfig.java
│   ├── AsyncConfig.java
│   ├── OpenApiConfig.java
│   ├── WebConfig.java
│   └── JpaConfig.java
├── filter/              # Filtros HTTP
│   ├── RequestLoggingFilter.java
│   └── RequestIdFilter.java
├── exception/           # Manejadores de excepciones
│   └── GlobalExceptionHandler.java
└── repository/          # Repositorios base
    └── BaseRepository.java
```

## Propósito

Esta capa:
1. **Expone** la API REST al exterior
2. **Configura** el framework Spring
3. **Intercepta** requests para logging, seguridad, métricas
4. **Accede** a la base de datos

## Características por Componente

| Componente | Características |
|------------|-----------------|
| **Controllers** | HATEOAS, ETags, paginación, validación |
| **Config** | Cache, async, CORS, OpenAPI, JPA |
| **Filters** | Logging, request ID, métricas |
| **Exception Handler** | RFC 7807 Problem Details |
| **Repository** | Specifications, soft delete filter |

## Dependencias

```
infrastructure/ ──────► application/ (usa servicios, DTOs)
                ──────► domain/ (usa entidades, excepciones)
                ──────► Spring Web, Data, Security
                ──────► Caffeine, OpenAPI, etc.
```

## Reglas de Arquitectura

1. **Controllers solo orquestan** - No contienen lógica de negocio
2. **Delegación a servicios** - Controllers llaman servicios
3. **Configuración centralizada** - En clases @Configuration
4. **Filtros son transversales** - No conocen entidades específicas
