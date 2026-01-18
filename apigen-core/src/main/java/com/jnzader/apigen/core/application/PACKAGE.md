# Core Application Layer

La capa de aplicación contiene la **lógica de casos de uso**: servicios, DTOs, mappers y utilidades que orquestan las operaciones del sistema.

## Estructura

```
application/
├── dto/                 # Data Transfer Objects
│   ├── BaseDTO.java     # DTO base con campos comunes
│   └── validation/      # Grupos de validación
│       ├── OnCreate.java
│       └── OnUpdate.java
├── mapper/              # Interfaces de mapeo
│   └── BaseMapper.java  # Mapper genérico entity ↔ DTO
├── service/             # Servicios de aplicación
│   ├── BaseService.java      # Interface genérica
│   ├── BaseServiceImpl.java  # Implementación CRUD
│   └── CacheEvictionService.java
└── util/                # Utilidades
    └── Result.java      # Tipo Result<T,E> para errores
```

## Propósito

Esta capa:
1. **Orquesta** operaciones usando entidades del dominio
2. **Transforma** entre DTOs (externos) y Entities (internos)
3. **Aplica** reglas de negocio transversales (cache, eventos, transacciones)
4. **No contiene** lógica de dominio pura (esa va en entities)

## Flujo de Datos

```
Controller                Application               Domain
    │                         │                        │
    │  ProductDTO             │                        │
    │ ───────────────────────►│                        │
    │                         │  Product (entity)      │
    │                         │ ──────────────────────►│
    │                         │                        │
    │                         │◄────────────────────── │
    │                         │  Product (modified)    │
    │◄─────────────────────── │                        │
    │  ProductDTO (response)  │                        │
```

## Responsabilidades por Componente

| Componente | Responsabilidad |
|------------|-----------------|
| **DTO** | Estructura de datos para API, validaciones de entrada |
| **Mapper** | Conversión entity ↔ DTO, sin lógica de negocio |
| **Service** | Casos de uso, transacciones, cache, eventos |
| **Util** | Helpers genéricos (Result, etc.) |

## Dependencias

```
application/ ──────► domain/ (usa entidades, eventos, excepciones)
             ──────► Spring Framework (transacciones, cache)
             ──────► MapStruct (mapeo)
             ──────► Jakarta Validation (validación)
```

## Reglas de Arquitectura

1. **Application puede usar Domain** - Entities, events, exceptions
2. **Application NO usa Infrastructure** - Sin controllers, repos directos
3. **Services reciben/retornan DTOs o Entities** - Depende del caso
4. **Transacciones se manejan aquí** - @Transactional en services
