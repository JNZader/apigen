# Repositories

Este paquete contiene la **interface base** para repositorios Spring Data JPA.

## Ubicación Arquitectónica

> **Nota sobre DDD**: En DDD estricto, las interfaces de repositorio pertenecen al **dominio** (definen el contrato) y las implementaciones a **infraestructura**. Sin embargo, este proyecto usa un enfoque **pragmático**:

| Enfoque | Ubicación Interface | Ubicación Impl | Usado aquí |
|---------|--------------------|--------------------|------------|
| **DDD Puro** | `domain/repository/` | `infrastructure/persistence/` | No |
| **Pragmático** | `infrastructure/repository/` | (Spring Data genera) | ✅ Sí |

**Razón de esta decisión**:
1. Las interfaces extienden `JpaRepository` (dependencia de Spring/JPA)
2. Spring Data genera la implementación automáticamente
3. Cambiar de JPA es improbable en este contexto
4. Reduce complejidad sin perder testabilidad

**Si necesitas DDD puro**, puedes:
1. Crear interface pura en `domain/repository/` sin dependencias de Spring
2. Crear adapter en `infrastructure/` que implemente esa interface usando Spring Data

```java
// domain/repository/ProductRepository.java (puro)
public interface ProductRepository {
    Optional<Product> findById(Long id);
    Product save(Product product);
}

// infrastructure/persistence/JpaProductRepository.java (adapter)
@Repository
public class JpaProductRepository implements ProductRepository {
    private final SpringDataProductRepository springRepo;
    // Delega a Spring Data
}
```

---

## Archivos

| Archivo | Descripción |
|---------|-------------|
| `BaseRepository.java` | Interface genérica que extiende JpaRepository |

## BaseRepository

```java
@NoRepositoryBean  // No crear implementación de esta interface
public interface BaseRepository<E extends Base, ID>
        extends JpaRepository<E, ID>, JpaSpecificationExecutor<E> {

    // Métodos adicionales para soft delete
    @Query("SELECT e FROM #{#entityName} e WHERE e.estado = true")
    List<E> findAllActive();

    @Query("SELECT e FROM #{#entityName} e WHERE e.estado = true")
    Page<E> findAllActive(Pageable pageable);

    @Query("SELECT COUNT(e) FROM #{#entityName} e WHERE e.estado = true")
    long countActive();

    @Query("SELECT e FROM #{#entityName} e WHERE e.id = :id AND e.estado = true")
    Optional<E> findActiveById(@Param("id") ID id);

    // Para incluir eliminados (bypass @SQLRestriction)
    @Query("SELECT e FROM #{#entityName} e WHERE e.id = :id")
    Optional<E> findByIdIncludingDeleted(@Param("id") ID id);

    @Query("SELECT e FROM #{#entityName} e WHERE e.estado = false")
    Page<E> findAllDeleted(Pageable pageable);
}
```

## Interfaces Heredadas

`BaseRepository` hereda de:

### JpaRepository<E, ID>
```java
// CRUD básico
E save(E entity);
Optional<E> findById(ID id);
List<E> findAll();
void delete(E entity);
void deleteById(ID id);
boolean existsById(ID id);
long count();

// Batch
List<E> saveAll(Iterable<E> entities);
void deleteAll(Iterable<E> entities);

// Paginación
Page<E> findAll(Pageable pageable);
List<E> findAll(Sort sort);
```

### JpaSpecificationExecutor<E>
```java
// Queries dinámicas
Optional<E> findOne(Specification<E> spec);
List<E> findAll(Specification<E> spec);
Page<E> findAll(Specification<E> spec, Pageable pageable);
long count(Specification<E> spec);
boolean exists(Specification<E> spec);
```

## Soft Delete con @SQLRestriction

La entidad `Base` tiene:
```java
@SQLRestriction("estado = true")
public abstract class Base { }
```

Esto hace que **todas las queries automáticamente filtren** registros eliminados:

```java
// Esto automáticamente agrega: WHERE estado = true
repository.findById(1L);
repository.findAll();
repository.count();
```

Para incluir eliminados, usar los métodos explícitos:
```java
repository.findByIdIncludingDeleted(1L);
repository.findAllDeleted(pageable);
```

## Crear Repositorio Específico

```java
@Repository
public interface ProductRepository extends BaseRepository<Product, Long> {

    // Query methods (Spring Data genera implementación)
    Optional<Product> findBySku(String sku);
    List<Product> findByCategory(Category category);
    boolean existsBySku(String sku);

    // JPQL custom
    @Query("SELECT p FROM Product p WHERE p.stock < :threshold AND p.estado = true")
    List<Product> findLowStockProducts(@Param("threshold") int threshold);

    // Native SQL
    @Query(value = "SELECT * FROM products WHERE stock < :threshold", nativeQuery = true)
    List<Product> findLowStockProductsNative(@Param("threshold") int threshold);

    // Modificación
    @Modifying
    @Query("UPDATE Product p SET p.stock = p.stock + :delta WHERE p.id = :id")
    int updateStock(@Param("id") Long id, @Param("delta") int delta);
}
```

## Query Methods

Spring Data genera queries desde nombres de métodos:

| Método | Query Generada |
|--------|----------------|
| `findByName(String)` | `WHERE name = ?` |
| `findByNameContaining(String)` | `WHERE name LIKE %?%` |
| `findByNameIgnoreCase(String)` | `WHERE LOWER(name) = LOWER(?)` |
| `findByPriceLessThan(BigDecimal)` | `WHERE price < ?` |
| `findByPriceBetween(min, max)` | `WHERE price BETWEEN ? AND ?` |
| `findByActiveTrue()` | `WHERE active = true` |
| `findByCreatedAtAfter(date)` | `WHERE created_at > ?` |
| `findByNameAndPrice(n, p)` | `WHERE name = ? AND price = ?` |
| `findByNameOrSku(n, s)` | `WHERE name = ? OR sku = ?` |
| `findByNameOrderByPriceDesc(n)` | `WHERE name = ? ORDER BY price DESC` |
| `countByCategory(cat)` | `SELECT COUNT(*) WHERE category = ?` |
| `existsByEmail(email)` | `SELECT 1 WHERE email = ? LIMIT 1` |
| `deleteByExpiredTrue()` | `DELETE WHERE expired = true` |

## Specifications (Queries Dinámicas)

Para filtros complejos y dinámicos:

```java
public class ProductSpecifications {

    public static Specification<Product> hasName(String name) {
        return (root, query, cb) ->
            name == null ? null : cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }

    public static Specification<Product> hasPriceBetween(BigDecimal min, BigDecimal max) {
        return (root, query, cb) -> {
            if (min == null && max == null) return null;
            if (min == null) return cb.lessThanOrEqualTo(root.get("price"), max);
            if (max == null) return cb.greaterThanOrEqualTo(root.get("price"), min);
            return cb.between(root.get("price"), min, max);
        };
    }

    public static Specification<Product> isActive() {
        return (root, query, cb) -> cb.equal(root.get("estado"), true);
    }
}
```

**Uso:**
```java
Specification<Product> spec = Specification
    .where(ProductSpecifications.hasName("laptop"))
    .and(ProductSpecifications.hasPriceBetween(500, 2000))
    .and(ProductSpecifications.isActive());

Page<Product> products = repository.findAll(spec, pageable);
```

## Projections

Para retornar solo campos específicos:

```java
// Interface projection
public interface ProductSummary {
    Long getId();
    String getName();
    BigDecimal getPrice();
}

@Repository
public interface ProductRepository extends BaseRepository<Product, Long> {
    List<ProductSummary> findAllProjectedBy();
    Optional<ProductSummary> findSummaryById(Long id);
}
```

## Testing

```java
@DataJpaTest
class ProductRepositoryTest {

    @Autowired
    private ProductRepository repository;

    @Test
    void shouldFindBySku() {
        Product product = Product.builder()
            .name("Test")
            .sku("TEST-001")
            .build();
        repository.save(product);

        Optional<Product> found = repository.findBySku("TEST-001");

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Test");
    }

    @Test
    void shouldNotFindDeletedProducts() {
        Product product = Product.builder().name("Deleted").build();
        product.setEstado(false);  // Soft deleted
        repository.save(product);

        List<Product> all = repository.findAll();

        assertThat(all).isEmpty();  // @SQLRestriction filtra
    }
}
```

## Buenas Prácticas

1. **@NoRepositoryBean en base** - Evita crear implementación innecesaria
2. **Query methods para queries simples** - Más legible que @Query
3. **Specifications para filtros dinámicos** - Composables y reutilizables
4. **Projections para lecturas** - Reducen transferencia de datos
5. **@Modifying para updates/deletes** - Requerido por Spring Data
6. **Pageable siempre** - Evitar findAll() sin límites
