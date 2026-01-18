# Core Module

El módulo `core` contiene la funcionalidad base reutilizable del framework APiGen. Todas las entidades, servicios y controladores del dominio de negocio heredan de las clases definidas aquí.

## Estructura

```
core/
├── domain/                    # Capa de Dominio
│   ├── entity/               # Entidades base
│   ├── event/                # Eventos de dominio
│   └── exception/            # Excepciones de negocio
├── application/              # Capa de Aplicación
│   ├── dto/                  # DTOs y validaciones
│   ├── mapper/               # Interfaces de mapeo
│   ├── service/              # Servicios genéricos
│   └── util/                 # Utilidades (Result, etc.)
└── infrastructure/           # Capa de Infraestructura
    ├── controller/           # Controladores REST base
    ├── config/               # Configuraciones Spring
    ├── filter/               # Filtros HTTP
    ├── exception/            # Manejadores de excepciones
    └── repository/           # Repositorios base
```

## Propósito

Este módulo implementa el **patrón Template Method** a nivel arquitectónico:
- Define el comportamiento común (CRUD, auditoría, cache, eventos)
- Permite a módulos específicos extender y personalizar

## Dependencias

```
core/ ──────► Spring Framework
      ──────► Hibernate/JPA
      ──────► MapStruct
      ──────► Caffeine Cache
```

## Cómo Usar

Los módulos de dominio (products, customers, etc.) heredan de core:

```java
// Entidad hereda de Base
public class Product extends Base { }

// Servicio hereda de BaseServiceImpl
public class ProductServiceImpl extends BaseServiceImpl<Product, Long> { }

// Controlador hereda de BaseControllerImpl
public class ProductControllerImpl extends BaseControllerImpl<Product, ProductDTO, Long> { }
```

## Reglas de Arquitectura

1. **Core NO depende de módulos específicos** - Solo de frameworks
2. **Módulos específicos SÍ dependen de core** - Heredan clases base
3. **Domain NO depende de Infrastructure** - Aislamiento del dominio
4. **Application puede depender de Domain** - Usa entidades y eventos
