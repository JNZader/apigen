# 02 - Dominio Base: Entidades, Repositorios y Soft Delete

## Objetivo

Entender la capa de dominio base que es el fundamento de todas las entidades del sistema. Este documento cubre los componentes del modulo `apigen-core`:

- Entidad `Base` con auditoria automatica
- `BaseRepository` con queries optimizadas
- Sistema de soft delete
- Domain events para comunicacion entre agregados

## Ubicacion en el Proyecto Multi-Modulo

```
apigen/
├── apigen-core/                      # <-- Este modulo contiene el dominio base
│   └── src/main/java/com/jnzader/apigen/core/
│       └── domain/
│           ├── entity/
│           │   └── Base.java         # Entidad base
│           ├── repository/
│           │   └── BaseRepository.java
│           ├── event/
│           │   └── DomainEvent.java  # Eventos de dominio
│           └── specification/
│               └── FilterSpecificationBuilder.java
│
└── apigen-example/                   # Ejemplo de uso
    └── src/main/java/com/jnzader/example/
        └── domain/entity/
            └── Product.java          # Extiende Base
```

## Formas de Usar el Dominio Base

### Opcion 1: Como Dependencia (Recomendado)

Si usas APiGen como libreria, las clases base ya estan disponibles:

```groovy
// build.gradle
dependencies {
    implementation platform('com.jnzader:apigen-bom:1.0.0-SNAPSHOT')
    implementation 'com.jnzader:apigen-core'
}
```

```java
// Tu entidad extiende Base de apigen-core
import com.jnzader.apigen.core.domain.entity.Base;

@Entity
public class Product extends Base {
    private String name;
    private BigDecimal price;
}
```

### Opcion 2: Explorar el Codigo Fuente

Si clonaste el repositorio, puedes ver la implementacion en:

```bash
# Ver la entidad Base
cat apigen-core/src/main/java/com/jnzader/apigen/core/domain/entity/Base.java

# Ver el BaseRepository
cat apigen-core/src/main/java/com/jnzader/apigen/core/domain/repository/BaseRepository.java
```

## Por que empezar con el dominio

En Domain-Driven Design (DDD), el **dominio es el corazon del sistema**. Todo lo demas (aplicacion, infraestructura) es accesorio. Empezar por el dominio te obliga a pensar en la logica de negocio antes que en detalles tecnicos.

### El error comun

```java
// MAL: Empezar con el controller
@RestController
public class ProductController {
    // ¿Que hace el dominio? No lo se aun, pero tengo un endpoint
}
```

### El enfoque correcto

```java
// BIEN: Empezar con el dominio
@Entity
public class Product extends Base {
    // El dominio define las reglas de negocio
    public void aplicarDescuento(BigDecimal porcentaje) {
        if (porcentaje.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El descuento no puede ser negativo");
        }
        // Logica de negocio aqui
    }
}
```

## La Entidad Base

**Ubicacion:** `apigen-core/src/main/java/com/jnzader/apigen/core/domain/entity/Base.java`

La clase `Base` es la **entidad madre** de la que heredan todas las demas. Proporciona:

- **Auditoria automatica:** Quien creo, quien modifico, cuando
- **Soft delete:** Nunca borramos, solo marcamos como inactivo
- **Control de concurrencia:** Version optimista con `@Version`
- **Domain events:** Para pub/sub entre agregados

```java
package com.jnzader.apigen.core.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.jnzader.apigen.core.domain.event.DomainEvent;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.envers.Audited;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.domain.AfterDomainEventPublication;
import org.springframework.data.domain.DomainEvents;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Clase base abstracta para todas las entidades del sistema.
 *
 * Proporciona:
 * - Campos comunes de auditoria
 * - Soporte para soft delete
 * - Domain events para comunicacion entre agregados
 * - Control de concurrencia optimista
 */
@MappedSuperclass  // No es una entidad en si, sino un padre
@Getter
@Setter
@Audited  // Hibernate Envers guardara todas las versiones
@EntityListeners(AuditingEntityListener.class)  // Para @CreatedDate, etc
@SQLRestriction("estado = true")  // SOLO muestra registros activos en queries
public abstract class Base implements Serializable {

    // ==================== Identificador ====================

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "base_seq_gen")
    @SequenceGenerator(
        name = "base_seq_gen",
        sequenceName = "base_sequence",
        allocationSize = 50  // Pre-asigna 50 IDs (mejora performance)
    )
    private Long id;

    // ==================== Soft Delete ====================

    /**
     * Estado de la entidad (true = activo, false = eliminado).
     * @SQLRestriction hace que solo se vean los activos en queries normales.
     */
    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    private Boolean estado = true;

    /**
     * Fecha y hora de eliminacion logica (solo si estado = false).
     */
    @Column
    private LocalDateTime fechaEliminacion;

    /**
     * Usuario que elimino la entidad.
     */
    @Column(length = 100)
    private String eliminadoPor;

    // ==================== Auditoria ====================

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime fechaActualizacion;

    @CreatedBy
    @Column(length = 100, updatable = false)
    private String creadoPor;

    @LastModifiedBy
    @Column(length = 100)
    private String modificadoPor;

    // ==================== Control de Concurrencia ====================

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    // ==================== Domain Events ====================

    @Transient
    @JsonIgnore
    private final List<DomainEvent> domainEvents = new CopyOnWriteArrayList<>();

    public void registerEvent(DomainEvent event) {
        this.domainEvents.add(event);
    }

    @DomainEvents
    public List<DomainEvent> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    @AfterDomainEventPublication
    public void clearDomainEvents() {
        this.domainEvents.clear();
    }

    // ==================== Soft Delete Methods ====================

    public void softDelete(String usuario) {
        this.estado = false;
        this.fechaEliminacion = LocalDateTime.now();
        this.eliminadoPor = usuario;
    }

    public void restore() {
        this.estado = true;
        this.fechaEliminacion = null;
        this.eliminadoPor = null;
    }

    public boolean isDeleted() {
        return !Boolean.TRUE.equals(estado);
    }

    public boolean isActive() {
        return Boolean.TRUE.equals(estado);
    }

    // ==================== Equals & HashCode ====================

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Base base = (Base) o;
        if (id != null && base.id != null) {
            return Objects.equals(id, base.id);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return id != null ? Objects.hash(id) : System.identityHashCode(this);
    }
}
```

### Conceptos Clave Explicados

#### 1. @SQLRestriction (Soft Delete Automatico)

```java
@SQLRestriction("estado = true")
```

Esta anotacion de Hibernate 6.2+ agrega automaticamente `WHERE estado = true` a **TODAS** las queries JPA:

```java
// Tu codigo
repository.findAll();

// Query real ejecutada por Hibernate
SELECT * FROM productos WHERE estado = true;
```

**Ventaja:** No tienes que recordar filtrar por estado en cada query.
**Desventaja:** Para buscar eliminados, necesitas queries nativas.

#### 2. @Version (Concurrencia Optimista)

```java
@Version
private Long version = 0L;
```

Hibernate usa este campo para detectar modificaciones concurrentes:

```sql
-- Usuario A lee Product ID=1 (version=5)
-- Usuario B lee Product ID=1 (version=5)

-- Usuario A actualiza primero
UPDATE products SET name='A', version=6 WHERE id=1 AND version=5;
-- Exito, version pasa a 6

-- Usuario B intenta actualizar
UPDATE products SET name='B', version=6 WHERE id=1 AND version=5;
-- Falla (version ya no es 5), lanza OptimisticLockException
```

#### 3. Domain Events

Los domain events permiten comunicacion desacoplada entre agregados:

```java
// En el servicio
entity.registerEvent(new ProductCreatedEvent(product));
repository.save(entity);  // Al finalizar, Spring publica el evento

// En otro lugar
@EventListener
public void onProductCreated(ProductCreatedEvent event) {
    // Actualizar cache, enviar email, etc
}
```

## BaseRepository

**Ubicacion:** `apigen-core/src/main/java/com/jnzader/apigen/core/domain/repository/BaseRepository.java`

```java
package com.jnzader.apigen.core.domain.repository;

import com.jnzader.apigen.core.domain.entity.Base;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Interfaz base para repositorios JPA genericos.
 */
@NoRepositoryBean
public interface BaseRepository<E extends Base, ID extends Serializable>
        extends JpaRepository<E, ID>, JpaSpecificationExecutor<E> {

    // ==================== Queries basicas ====================

    List<E> findByEstadoTrue();
    List<E> findByEstadoFalse();
    long countByEstadoTrue();
    long countByEstadoFalse();

    // ==================== Soft Delete ====================

    @Modifying
    @Query("UPDATE #{#entityName} e SET e.estado = false, e.fechaEliminacion = :fecha, e.eliminadoPor = :usuario WHERE e.id = :id")
    int softDeleteById(@Param("id") ID id, @Param("fecha") LocalDateTime fecha, @Param("usuario") String usuario);

    @Modifying
    @Query("UPDATE #{#entityName} e SET e.estado = false, e.fechaEliminacion = :fecha, e.eliminadoPor = :usuario WHERE e.id IN :ids")
    int softDeleteAllByIds(@Param("ids") List<ID> ids, @Param("fecha") LocalDateTime fecha, @Param("usuario") String usuario);

    @Modifying
    @Query("UPDATE #{#entityName} e SET e.estado = true, e.fechaEliminacion = null, e.eliminadoPor = null WHERE e.id = :id")
    int restoreById(@Param("id") ID id);

    @Modifying
    @Query("UPDATE #{#entityName} e SET e.estado = true, e.fechaEliminacion = null, e.eliminadoPor = null WHERE e.id IN :ids")
    int restoreAllByIds(@Param("ids") List<ID> ids);

    // ==================== Hard Delete ====================

    @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM #{#entityName} e WHERE e.id = :id")
    boolean existsByIdIncludingDeleted(@Param("id") ID id);

    @Modifying
    @Query("DELETE FROM #{#entityName} e WHERE e.id = :id")
    int hardDeleteById(@Param("id") ID id);

    // ==================== Queries de fecha ====================

    List<E> findByFechaCreacionBetween(LocalDateTime start, LocalDateTime end);
    List<E> findByFechaActualizacionAfter(LocalDateTime date);
}
```

### Conceptos Clave del Repository

#### 1. #{#entityName} - SpEL Magic

```java
@Query("UPDATE #{#entityName} e SET ...")
```

`#{#entityName}` es evaluado por Spring en runtime:
- Si el repositorio es `ProductRepository extends BaseRepository<Product, Long>`
- Entonces `#{#entityName}` se reemplaza por `Product`
- Y la query se convierte en `UPDATE Product e SET ...`

#### 2. @Modifying - Queries de Modificacion

```java
@Modifying
@Query("UPDATE ...")
int softDeleteById(...);
```

`@Modifying` le dice a Spring Data JPA que esta query MODIFICA datos.
**Retorna:** El numero de filas afectadas (int).

#### 3. Por que UPDATE directo es mejor

```java
// MAL (ineficiente)
Product product = repository.findById(id).get();  // 1 SELECT
product.softDelete("admin");
repository.save(product);  // 1 UPDATE

// BIEN (optimizado)
repository.softDeleteById(id, now(), "admin");  // 1 UPDATE directo
```

## Domain Events

**Ubicacion:** `apigen-core/src/main/java/com/jnzader/apigen/core/domain/event/`

```java
package com.jnzader.apigen.core.domain.event;

import java.time.LocalDateTime;

/**
 * Interfaz base para todos los domain events.
 */
public interface DomainEvent {
    LocalDateTime occurredOn();
}
```

Eventos disponibles:
- `EntityCreatedEvent<E>` - Cuando se crea una entidad
- `EntityUpdatedEvent<E>` - Cuando se actualiza
- `EntityDeletedEvent<E>` - Cuando se elimina (soft delete)
- `EntityRestoredEvent<E>` - Cuando se restaura

## Configuracion de Auditoria

La auto-configuracion de `apigen-core` incluye un `AuditorAware` que obtiene el usuario actual desde Spring Security:

```java
// En ApigenCoreAutoConfiguration
@Bean
@ConditionalOnMissingBean
public AuditorAware<String> auditorProvider() {
    return () -> {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return Optional.of(auth.getName());
        }
        return Optional.of("system");
    };
}
```

## Ejemplo Practico: Crear una Entidad

En tu proyecto (o en `apigen-example`):

```java
// 1. Entity (extiende de Base)
package com.jnzader.example.domain.entity;

import com.jnzader.apigen.core.domain.entity.Base;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "products")
@Getter
@Setter
public class Product extends Base {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    @Column
    private Integer stock;
}
```

```java
// 2. Repository (extiende BaseRepository)
package com.jnzader.example.domain.repository;

import com.jnzader.apigen.core.domain.repository.BaseRepository;
import com.jnzader.example.domain.entity.Product;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends BaseRepository<Product, Long> {
    List<Product> findByNameContainingIgnoreCase(String name);
}
```

## Probando el Soft Delete

```java
// Crear producto
Product product = new Product();
product.setName("Laptop");
product.setPrice(new BigDecimal("999.99"));
repository.save(product);

// Soft delete
repository.softDeleteById(product.getId(), LocalDateTime.now(), "admin");

// Verificar que NO aparece en findAll()
List<Product> all = repository.findAll();  // Lista vacia (soft delete funciona)

// Buscar eliminados
List<Product> deleted = repository.findByEstadoFalse();  // Contiene el producto

// Restaurar
repository.restoreById(product.getId());

// Ahora si aparece
all = repository.findAll();  // Contiene el producto restaurado
```

## Resumen

```
Usas APiGen como dependencia
         │
         └─> Tus entidades extienden Base
                    │
                    └─> Heredan automaticamente:
                           - id, estado, version
                           - fechaCreacion, fechaActualizacion
                           - creadoPor, modificadoPor
                           - Soft delete con @SQLRestriction
                           - Domain events
                           - Concurrencia optimista
```

---

**Anterior:** [01-SETUP-INICIAL.md](./01-SETUP-INICIAL.md)
**Siguiente:** [03-APLICACION-BASE.md](./03-APLICACION-BASE.md)
