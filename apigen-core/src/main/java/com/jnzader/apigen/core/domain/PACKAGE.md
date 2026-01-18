# Core Domain Layer

La capa de dominio contiene el **corazón del negocio**: entidades, eventos y excepciones que representan los conceptos fundamentales del sistema.

## Estructura

```
domain/
├── entity/          # Entidades JPA base
│   └── Base.java    # Superclase de todas las entidades
├── event/           # Eventos de dominio
│   ├── EntityCreatedEvent.java
│   ├── EntityUpdatedEvent.java
│   ├── EntityDeletedEvent.java
│   └── EntityRestoredEvent.java
└── exception/       # Excepciones de negocio
    ├── BusinessException.java
    ├── ResourceNotFoundException.java
    ├── DuplicateResourceException.java
    └── ValidationException.java
```

## Principios de Diseño

### 1. Independencia de Infraestructura
El dominio **NO debe conocer**:
- Controllers, REST, HTTP
- Base de datos específica
- Frameworks de cache
- Servicios externos

### 2. Modelo Rico vs Anémico
Las entidades **SÍ pueden tener**:
- Métodos de negocio (`softDelete()`, `restore()`)
- Validaciones internas
- Registro de eventos de dominio

### 3. Inmutabilidad donde sea posible
- Eventos son `record` (inmutables)
- Excepciones son inmutables
- Campos de auditoría solo se modifican por framework

## Convenciones

| Elemento | Convención | Ejemplo |
|----------|------------|---------|
| Entidades | PascalCase, singular | `Product`, `Customer` |
| Eventos | PascalCase + Event | `EntityCreatedEvent` |
| Excepciones | PascalCase + Exception | `ResourceNotFoundException` |

## Dependencias Permitidas

```
✅ java.time.*
✅ java.util.*
✅ jakarta.persistence.* (anotaciones JPA)
✅ jakarta.validation.* (anotaciones de validación)
✅ lombok.* (reducción de boilerplate)
✅ org.hibernate.envers.* (anotación @Audited)

❌ org.springframework.* (excepto @Transient para eventos)
❌ Cualquier clase de infrastructure/
❌ Cualquier clase de application/ (excepto DTOs para eventos)
```

## Reglas ArchUnit

```java
// El dominio no debe depender de infraestructura
noClasses()
    .that().resideInAPackage("..domain..")
    .should().dependOnClassesThat()
    .resideInAPackage("..infrastructure..");
```
