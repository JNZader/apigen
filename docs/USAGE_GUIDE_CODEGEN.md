# APiGen - Guía de Uso: Generación de Código

Esta guía explica cómo usar `apigen-codegen` para generar automáticamente entidades, DTOs, servicios y controladores desde archivos SQL.

## Índice

1. [Cuándo Usar Esta Opción](#cuándo-usar-esta-opción)
2. [Instalación](#instalación)
3. [Uso Básico](#uso-básico)
4. [Formato del Archivo SQL](#formato-del-archivo-sql)
5. [Estructura Generada](#estructura-generada)
6. [Ejemplos Completos](#ejemplos-completos)
7. [Personalización](#personalización)
8. [Referencia de Tipos SQL](#referencia-de-tipos-sql)

---

## Cuándo Usar Esta Opción

Usa el generador de código cuando:

- Tienes un esquema SQL existente y quieres generar la API completa
- Prefieres diseñar primero la base de datos (database-first)
- Quieres generar rápidamente entidades para un nuevo proyecto
- Necesitas mantener sincronizado el código con cambios en el esquema

**No uses esta opción si:**
- Prefieres diseñar primero las entidades (code-first)
- Tu esquema es muy complejo con muchas relaciones personalizadas
- Necesitas un control muy fino sobre la generación

---

## Instalación

### Como Dependencia de Proyecto

```groovy
// build.gradle
dependencies {
    implementation 'com.github.jnzader.apigen:apigen-codegen:v2.18.0'
}
```

### Como Herramienta CLI

```bash
# Descargar el JAR (desde GitHub Releases)
curl -L https://github.com/jnzader/apigen/releases/latest/download/apigen-codegen.jar -o apigen-codegen.jar

# Ejecutar
java -jar apigen-codegen.jar schema.sql /path/to/project com.mycompany.api
```

### Desde el Código Fuente

```bash
# Clonar y compilar
git clone https://github.com/jnzader/apigen.git
cd apigen
./gradlew :apigen-codegen:build

# Ejecutar
java -jar apigen-codegen/build/libs/apigen-codegen.jar
```

---

## Uso Básico

### Sintaxis

```bash
java -jar apigen-codegen.jar <archivo-sql> [directorio-proyecto] [paquete-base]
```

### Parámetros

| Parámetro | Descripción | Default |
|-----------|-------------|---------|
| `archivo-sql` | Ruta al archivo SQL con el esquema | (requerido) |
| `directorio-proyecto` | Directorio raíz del proyecto | `.` (actual) |
| `paquete-base` | Paquete base para el código generado | `com.jnzader.apigen` |

### Ejemplos

```bash
# Generar en el directorio actual con paquete por defecto
java -jar apigen-codegen.jar schema.sql

# Especificar proyecto y paquete
java -jar apigen-codegen.jar db/schema.sql ./mi-proyecto com.miempresa.api

# Ruta absoluta
java -jar apigen-codegen.jar /path/to/schema.sql /path/to/project com.example
```

---

## Formato del Archivo SQL

### Estructura Básica

```sql
-- schema.sql

-- Secuencia para IDs (requerida)
CREATE SEQUENCE base_sequence START WITH 1 INCREMENT BY 50;

-- Tabla de ejemplo
CREATE TABLE products (
    id BIGINT PRIMARY KEY DEFAULT nextval('base_sequence'),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(10, 2) NOT NULL,
    stock INTEGER DEFAULT 0,
    category_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_product_category
        FOREIGN KEY (category_id) REFERENCES categories(id)
);

-- Índices
CREATE INDEX idx_products_name ON products(name);
CREATE INDEX idx_products_category ON products(category_id);
```

### Características SQL Soportadas

#### 1. CREATE TABLE

```sql
CREATE TABLE table_name (
    column_name TYPE [constraints],
    ...
);
```

**Constraints soportados:**
- `PRIMARY KEY`
- `NOT NULL`
- `UNIQUE`
- `DEFAULT value`
- `CHECK (condition)`
- `REFERENCES table(column)`

#### 2. Tipos de Datos

| SQL Type | Java Type |
|----------|-----------|
| `BIGINT`, `BIGSERIAL` | `Long` |
| `INTEGER`, `INT`, `SERIAL` | `Integer` |
| `SMALLINT` | `Short` |
| `DECIMAL`, `NUMERIC` | `BigDecimal` |
| `REAL`, `FLOAT4` | `Float` |
| `DOUBLE PRECISION`, `FLOAT8` | `Double` |
| `VARCHAR`, `CHAR`, `TEXT` | `String` |
| `BOOLEAN`, `BOOL` | `Boolean` |
| `DATE` | `LocalDate` |
| `TIME` | `LocalTime` |
| `TIMESTAMP` | `LocalDateTime` |
| `TIMESTAMPTZ` | `Instant` |
| `UUID` | `UUID` |
| `BYTEA` | `byte[]` |
| `JSON`, `JSONB` | `String` |
| `INTERVAL` | `Duration` |

#### 3. Foreign Keys

```sql
-- Inline
CREATE TABLE orders (
    id BIGINT PRIMARY KEY,
    customer_id BIGINT REFERENCES customers(id),
    ...
);

-- Como constraint
CREATE TABLE orders (
    id BIGINT PRIMARY KEY,
    customer_id BIGINT,
    FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE SET NULL
);

-- ALTER TABLE
ALTER TABLE orders
    ADD CONSTRAINT fk_orders_customer
    FOREIGN KEY (customer_id) REFERENCES customers(id);
```

#### 4. Índices

```sql
-- Índice simple
CREATE INDEX idx_name ON table(column);

-- Índice único
CREATE UNIQUE INDEX idx_name ON table(column);

-- Índice compuesto
CREATE INDEX idx_name ON table(col1, col2);

-- Índice con tipo específico
CREATE INDEX idx_name ON table USING btree(column);
CREATE INDEX idx_name ON table USING gin(column);
CREATE INDEX idx_name ON table USING gist(column);

-- Índice parcial
CREATE INDEX idx_name ON table(column) WHERE condition;
```

#### 5. Funciones y Procedimientos

```sql
-- Función que retorna tabla
CREATE OR REPLACE FUNCTION get_active_products()
RETURNS TABLE (id BIGINT, name VARCHAR, price DECIMAL) AS $$
BEGIN
    RETURN QUERY SELECT p.id, p.name, p.price
                 FROM products p
                 WHERE p.estado = true;
END;
$$ LANGUAGE plpgsql;

-- Función con parámetros
CREATE FUNCTION find_products_by_category(cat_id BIGINT)
RETURNS SETOF products AS $$
    SELECT * FROM products WHERE category_id = cat_id;
$$ LANGUAGE sql;
```

#### 6. Tablas de Unión (Many-to-Many)

El generador detecta automáticamente tablas de unión:

```sql
-- Tabla de unión para productos-tags
CREATE TABLE product_tags (
    product_id BIGINT NOT NULL REFERENCES products(id),
    tag_id BIGINT NOT NULL REFERENCES tags(id),
    PRIMARY KEY (product_id, tag_id)
);
```

Genera automáticamente:
```java
// En Product.java
@ManyToMany
@JoinTable(
    name = "product_tags",
    joinColumns = @JoinColumn(name = "product_id"),
    inverseJoinColumns = @JoinColumn(name = "tag_id")
)
private List<Tag> tags = new ArrayList<>();
```

---

## Estructura Generada

Para cada tabla, se generan los siguientes archivos:

```
src/main/java/com/example/
└── products/                          # Módulo por tabla
    ├── domain/
    │   ├── entity/
    │   │   └── Product.java           # Entidad JPA
    │   ├── event/                     # (vacío, para eventos)
    │   └── exception/                 # (vacío, para excepciones)
    ├── application/
    │   ├── dto/
    │   │   └── ProductDTO.java        # DTO con validaciones
    │   ├── mapper/
    │   │   └── ProductMapper.java     # MapStruct mapper
    │   └── service/
    │       ├── ProductService.java     # Interfaz
    │       └── ProductServiceImpl.java # Implementación
    └── infrastructure/
        ├── repository/
        │   └── ProductRepository.java  # Repository Spring Data
        └── controller/
            ├── ProductController.java      # Interfaz
            └── ProductControllerImpl.java  # Implementación

src/main/resources/db/migration/
└── V2__create_products_table.sql      # Migración Flyway

src/test/java/com/example/products/
└── application/service/
    └── ProductServiceImplTest.java    # Tests unitarios
```

### Archivos Generados

#### 1. Entity (`Product.java`)

```java
@Entity
@Table(name = "products", indexes = {
    @Index(name = "idx_products_name", columnList = "name"),
    @Index(name = "idx_products_category", columnList = "category_id")
})
@Audited
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product extends Base {

    @NotBlank
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description")
    private String description;

    @NotNull
    @DecimalMin("0.01")
    @Column(name = "price", nullable = false)
    private BigDecimal price;

    @PositiveOrZero
    @Column(name = "stock")
    private Integer stock;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;
}
```

#### 2. DTO (`ProductDTO.java`)

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class ProductDTO extends BaseDTO {

    @NotBlank
    private String name;

    private String description;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal price;

    @PositiveOrZero
    private Integer stock;

    private Long categoryId;  // Solo ID para relaciones
}
```

#### 3. Mapper (`ProductMapper.java`)

```java
@Mapper(componentModel = "spring")
public interface ProductMapper extends BaseMapper<Product, ProductDTO> {

    @Override
    @Mapping(source = "category.id", target = "categoryId")
    ProductDTO toDto(Product entity);

    @Override
    @Mapping(target = "category", ignore = true)
    Product toEntity(ProductDTO dto);

    @Override
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "category", ignore = true)
    void updateEntityFromDto(ProductDTO dto, @MappingTarget Product entity);
}
```

#### 4. Repository (`ProductRepository.java`)

```java
@Repository
public interface ProductRepository extends BaseRepository<Product, Long> {

    // Métodos generados para columnas únicas
    Optional<Product> findBySku(String sku);

    // Métodos para funciones SQL
    @Query(value = "SELECT * FROM find_products_by_category(:catId)", nativeQuery = true)
    List<Product> findProductsByCategory(@Param("catId") Long catId);
}
```

#### 5. Service (`ProductServiceImpl.java`)

```java
@Service
@Slf4j
@Transactional
public class ProductServiceImpl
        extends BaseServiceImpl<Product, Long>
        implements ProductService {

    private final ProductRepository productRepository;

    public ProductServiceImpl(ProductRepository repository) {
        super(repository);
        this.productRepository = repository;
    }

    @Override
    protected String getEntityName() {
        return "Product";
    }
}
```

#### 6. Controller (`ProductControllerImpl.java`)

```java
@RestController
@Slf4j
public class ProductControllerImpl
        extends BaseControllerImpl<Product, ProductDTO, Long>
        implements ProductController {

    private final ProductService productService;
    private final ProductMapper productMapper;

    public ProductControllerImpl(ProductService service, ProductMapper mapper) {
        super(service, mapper);
        this.productService = service;
        this.productMapper = mapper;
    }
}
```

---

## Ejemplos Completos

### Ejemplo 1: E-commerce Básico

```sql
-- schema-ecommerce.sql

CREATE SEQUENCE base_sequence START WITH 1 INCREMENT BY 50;

-- Categorías
CREATE TABLE categories (
    id BIGINT PRIMARY KEY DEFAULT nextval('base_sequence'),
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    parent_id BIGINT REFERENCES categories(id)
);

-- Productos
CREATE TABLE products (
    id BIGINT PRIMARY KEY DEFAULT nextval('base_sequence'),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(10, 2) NOT NULL CHECK (price > 0),
    stock INTEGER NOT NULL DEFAULT 0 CHECK (stock >= 0),
    sku VARCHAR(50) UNIQUE,
    category_id BIGINT REFERENCES categories(id),
    is_featured BOOLEAN DEFAULT FALSE
);

-- Tags
CREATE TABLE tags (
    id BIGINT PRIMARY KEY DEFAULT nextval('base_sequence'),
    name VARCHAR(50) NOT NULL UNIQUE
);

-- Relación Many-to-Many: Productos-Tags
CREATE TABLE product_tags (
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    tag_id BIGINT NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
    PRIMARY KEY (product_id, tag_id)
);

-- Clientes
CREATE TABLE customers (
    id BIGINT PRIMARY KEY DEFAULT nextval('base_sequence'),
    email VARCHAR(255) NOT NULL UNIQUE,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    phone VARCHAR(20)
);

-- Órdenes
CREATE TABLE orders (
    id BIGINT PRIMARY KEY DEFAULT nextval('base_sequence'),
    customer_id BIGINT NOT NULL REFERENCES customers(id),
    order_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    total DECIMAL(12, 2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
);

-- Items de órdenes
CREATE TABLE order_items (
    id BIGINT PRIMARY KEY DEFAULT nextval('base_sequence'),
    order_id BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES products(id),
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    unit_price DECIMAL(10, 2) NOT NULL
);

-- Índices
CREATE INDEX idx_products_category ON products(category_id);
CREATE INDEX idx_products_sku ON products(sku);
CREATE INDEX idx_orders_customer ON orders(customer_id);
CREATE INDEX idx_orders_date ON orders(order_date DESC);
CREATE INDEX idx_order_items_order ON order_items(order_id);

-- Función: Obtener productos más vendidos
CREATE OR REPLACE FUNCTION get_best_sellers(limit_count INTEGER DEFAULT 10)
RETURNS TABLE (product_id BIGINT, product_name VARCHAR, total_sold BIGINT) AS $$
BEGIN
    RETURN QUERY
    SELECT p.id, p.name, SUM(oi.quantity) as total
    FROM products p
    JOIN order_items oi ON p.id = oi.product_id
    GROUP BY p.id, p.name
    ORDER BY total DESC
    LIMIT limit_count;
END;
$$ LANGUAGE plpgsql;
```

Ejecutar:
```bash
java -jar apigen-codegen.jar schema-ecommerce.sql ./ecommerce-api com.mystore.api
```

### Ejemplo 2: Blog

```sql
-- schema-blog.sql

CREATE SEQUENCE base_sequence START WITH 1 INCREMENT BY 50;

-- Autores
CREATE TABLE authors (
    id BIGINT PRIMARY KEY DEFAULT nextval('base_sequence'),
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    bio TEXT,
    avatar_url VARCHAR(500)
);

-- Categorías de posts
CREATE TABLE post_categories (
    id BIGINT PRIMARY KEY DEFAULT nextval('base_sequence'),
    name VARCHAR(100) NOT NULL UNIQUE,
    slug VARCHAR(100) NOT NULL UNIQUE
);

-- Posts
CREATE TABLE posts (
    id BIGINT PRIMARY KEY DEFAULT nextval('base_sequence'),
    title VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL UNIQUE,
    content TEXT NOT NULL,
    excerpt VARCHAR(500),
    author_id BIGINT NOT NULL REFERENCES authors(id),
    category_id BIGINT REFERENCES post_categories(id),
    published_at TIMESTAMP,
    is_draft BOOLEAN DEFAULT TRUE,
    view_count INTEGER DEFAULT 0
);

-- Comentarios
CREATE TABLE comments (
    id BIGINT PRIMARY KEY DEFAULT nextval('base_sequence'),
    post_id BIGINT NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    author_name VARCHAR(100) NOT NULL,
    author_email VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    is_approved BOOLEAN DEFAULT FALSE,
    parent_id BIGINT REFERENCES comments(id)
);

-- Índices
CREATE INDEX idx_posts_author ON posts(author_id);
CREATE INDEX idx_posts_category ON posts(category_id);
CREATE INDEX idx_posts_published ON posts(published_at DESC) WHERE is_draft = FALSE;
CREATE INDEX idx_posts_slug ON posts(slug);
CREATE INDEX idx_comments_post ON comments(post_id);
CREATE INDEX idx_comments_approved ON comments(is_approved) WHERE is_approved = TRUE;
```

---

## Personalización

### Después de Generar

1. **Agregar validaciones personalizadas:**
```java
// En ProductDTO.java - agregar
@Size(min = 3, max = 255, message = "Name must be between 3 and 255 characters")
private String name;
```

2. **Agregar lógica de negocio:**
```java
// En ProductServiceImpl.java
@Override
@Transactional
public Result<Product, Exception> save(Product product) {
    // Validación de negocio
    if (productRepository.existsBySku(product.getSku())) {
        return Result.failure(new DuplicateSkuException(product.getSku()));
    }
    return super.save(product);
}
```

3. **Agregar endpoints personalizados:**
```java
// En ProductControllerImpl.java
@GetMapping("/featured")
public ResponseEntity<List<ProductDTO>> getFeatured() {
    return ResponseEntity.ok(
        productService.findFeatured().stream()
            .map(productMapper::toDto)
            .toList()
    );
}
```

### Excluir Tablas

Para excluir tablas de la generación, prefíjalas con `_`:

```sql
-- Esta tabla será ignorada
CREATE TABLE _migrations (
    ...
);
```

### Columnas de Base Automáticas

El generador **no incluye** en los archivos generados las columnas que ya están en `Base`:
- `id`
- `estado`
- `fecha_creacion`
- `fecha_actualizacion`
- `fecha_eliminacion`
- `creado_por`
- `modificado_por`
- `eliminado_por`
- `version`

Estas columnas se heredan automáticamente de `Base`.

---

## Referencia de Tipos SQL

### Validaciones Automáticas

| SQL Constraint | Java Annotation |
|----------------|-----------------|
| `NOT NULL` | `@NotNull` |
| `NOT NULL` + `VARCHAR` | `@NotBlank` |
| `UNIQUE` | `@Column(unique = true)` |
| `CHECK (x > 0)` | `@Positive` |
| `CHECK (x >= 0)` | `@PositiveOrZero` |
| `VARCHAR(n)` | `@Size(max = n)` |
| `DECIMAL(p,s)` | `@Digits(integer = p-s, fraction = s)` |

### Relaciones Detectadas

| Patrón SQL | Relación JPA |
|------------|--------------|
| `FK a tabla sin FK salientes` | `@ManyToOne` |
| `FK única a otra tabla` | `@OneToOne` |
| `Tabla con solo 2 FKs como PK` | `@ManyToMany` |
| `FK con `ON DELETE CASCADE` | `cascade = CascadeType.ALL` |

---

## Solución de Problemas

### Error: "SQL parse error"

Verifica que tu SQL sea compatible con PostgreSQL. El parser usa JSqlParser que soporta sintaxis PostgreSQL.

### Error: "Cannot find referenced table"

Asegúrate de que las tablas referenciadas estén definidas antes de las que las referencian:

```sql
-- CORRECTO: categories antes de products
CREATE TABLE categories (...);
CREATE TABLE products (
    category_id BIGINT REFERENCES categories(id)
);

-- INCORRECTO: products antes de categories
CREATE TABLE products (
    category_id BIGINT REFERENCES categories(id)  -- Error!
);
CREATE TABLE categories (...);
```

### Warning: "Skipping junction table"

Las tablas de unión (many-to-many) no generan entidades propias. Si necesitas atributos en la relación, crea una entidad intermedia manualmente.

---

## Siguiente Paso

- Ver [Guía de Uso: Como Librería](USAGE_GUIDE_LIBRARY.md) para agregar a proyecto existente
- Ver [Guía de Uso: Módulo de Ejemplo](USAGE_GUIDE_EXAMPLE.md) para usar apigen-example como template
