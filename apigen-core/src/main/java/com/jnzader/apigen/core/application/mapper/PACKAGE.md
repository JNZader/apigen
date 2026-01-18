# Mappers

Este paquete contiene la **interface base** para mapeo entre Entities y DTOs usando MapStruct.

## Archivos

| Archivo | Descripción |
|---------|-------------|
| `BaseMapper.java` | Interface genérica de mapeo |

## MapStruct

APiGen usa [MapStruct](https://mapstruct.org/) para generar código de mapeo en tiempo de compilación.

### Por qué MapStruct?

| Factor | MapStruct | Manual | ModelMapper/Dozer |
|--------|-----------|--------|-------------------|
| Performance | Compile-time | Manual | Runtime reflection |
| Type Safety | Errores en compilación | Runtime | Runtime |
| Debugging | Código generado visible | Tu código | Caja negra |
| Boilerplate | Mínimo | Máximo | Mínimo |

## BaseMapper

### Interface

```java
public interface BaseMapper<E extends Base, D extends BaseDTO> {

    // Entity → DTO
    D toDto(E entity);

    // DTO → Entity
    E toEntity(D dto);

    // Actualización parcial (para PATCH)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDto(D dto, @MappingTarget E entity);

    // Listas
    List<D> toDtoList(List<E> entities);
    List<E> toEntityList(List<D> dtos);
}
```

### Implementación Específica

```java
@Mapper(componentModel = "spring")
public interface ProductMapper extends BaseMapper<Product, ProductDTO> {

    @Override
    ProductDTO toDto(Product entity);

    @Override
    Product toEntity(ProductDTO dto);

    @Override
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDto(ProductDTO dto, @MappingTarget Product entity);
}
```

MapStruct genera automáticamente la implementación `ProductMapperImpl`.

## Anotaciones Comunes

### @Mapper

```java
@Mapper(
    componentModel = "spring",           // Crear como Spring Bean
    unmappedTargetPolicy = ReportingPolicy.IGNORE,  // Ignorar campos no mapeados
    uses = { OtroMapper.class }          // Usar otros mappers
)
public interface ProductMapper { }
```

### @Mapping

Para campos con nombres diferentes o transformaciones:

```java
@Mapping(source = "nombreCompleto", target = "fullName")
@Mapping(source = "fechaNacimiento", target = "birthDate", dateFormat = "dd/MM/yyyy")
@Mapping(target = "edad", expression = "java(calcularEdad(entity.getFechaNacimiento()))")
@Mapping(target = "status", constant = "ACTIVE")
@Mapping(target = "internalField", ignore = true)
UserDTO toDto(User entity);
```

### @BeanMapping

Para actualización parcial (PATCH):

```java
@BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
void updateEntityFromDto(ProductDTO dto, @MappingTarget Product entity);
```

Esto hace que campos `null` en el DTO **no sobrescriban** valores existentes en la entidad.

### @AfterMapping

Para lógica post-mapeo:

```java
@AfterMapping
default void enrichDto(Product entity, @MappingTarget ProductDTO dto) {
    dto.setDisplayName(entity.getName() + " - " + entity.getSku());
}
```

## Mapeo de Relaciones

### One-to-Many

```java
@Mapper(componentModel = "spring", uses = {OrderItemMapper.class})
public interface OrderMapper extends BaseMapper<Order, OrderDTO> {

    // OrderItemMapper se usa automáticamente para la lista
    @Override
    OrderDTO toDto(Order entity);
}
```

### Many-to-One (Referencia por ID)

```java
public interface ProductMapper {

    @Mapping(source = "category.id", target = "categoryId")
    @Mapping(source = "category.name", target = "categoryName")
    ProductDTO toDto(Product entity);

    @Mapping(source = "categoryId", target = "category.id")
    Product toEntity(ProductDTO dto);
}
```

## Código Generado

MapStruct genera en `build/generated/sources/annotationProcessor`:

```java
@Component
public class ProductMapperImpl implements ProductMapper {

    @Override
    public ProductDTO toDto(Product entity) {
        if (entity == null) return null;

        ProductDTO dto = new ProductDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setPrice(entity.getPrice());
        // ... todos los campos
        return dto;
    }

    @Override
    public void updateEntityFromDto(ProductDTO dto, Product entity) {
        if (dto == null) return;

        if (dto.getName() != null) {          // Solo si no es null
            entity.setName(dto.getName());
        }
        if (dto.getPrice() != null) {
            entity.setPrice(dto.getPrice());
        }
        // ... campos opcionales
    }
}
```

## Testing

```java
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {ProductMapperImpl.class})
class ProductMapperTest {

    @Autowired
    private ProductMapper mapper;

    @Test
    void shouldMapEntityToDto() {
        Product entity = Product.builder()
            .id(1L)
            .name("Test")
            .price(new BigDecimal("99.99"))
            .build();

        ProductDTO dto = mapper.toDto(entity);

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getName()).isEqualTo("Test");
    }

    @Test
    void shouldIgnoreNullFieldsOnUpdate() {
        Product entity = Product.builder()
            .name("Original")
            .price(new BigDecimal("100"))
            .build();

        ProductDTO dto = new ProductDTO();
        dto.setPrice(new BigDecimal("200"));  // Solo actualizar precio
        // dto.name es null

        mapper.updateEntityFromDto(dto, entity);

        assertThat(entity.getName()).isEqualTo("Original");  // No cambió
        assertThat(entity.getPrice()).isEqualTo(new BigDecimal("200"));
    }
}
```

## Errores Comunes

### Error: "Unmapped target property"

```
Warning: Unmapped target property: "internalField"
```

**Solución**: Agregar `@Mapping(target = "internalField", ignore = true)` o `unmappedTargetPolicy = IGNORE`.

### Error: "No implementation generated"

**Posibles causas**:
1. Falta `@Mapper` en la interface
2. Falta dependencia de MapStruct processor en build.gradle
3. Métodos abstractos sin implementación default

### Error: "Cannot map X to Y"

**Solución**: Agregar método de conversión manual o usar otro mapper en `uses = {}`.

## Buenas Prácticas

1. **Un mapper por entidad principal**
2. **Usar `uses = {}` para composición**, no herencia
3. **`componentModel = "spring"`** para inyección
4. **Evitar lógica compleja** - Si necesitas mucha lógica, hazlo en el servicio
5. **Tests para mapeos complejos** - Especialmente relaciones y transformaciones
