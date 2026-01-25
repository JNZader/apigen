package com.jnzader.apigen.codegen.generator.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.jnzader.apigen.codegen.generator.entity.EntityGenerator.ManyToManyRelation;
import com.jnzader.apigen.codegen.model.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("MapperGenerator Tests")
@SuppressWarnings({
    "java:S5976", // Tests validate different specific mapper features
    "java:S1874", // Tests deprecated generators for backward compatibility
    "deprecation"
})
class MapperGeneratorTest {

    private MapperGenerator mapperGenerator;

    @BeforeEach
    void setUp() {
        mapperGenerator = new MapperGenerator("com.example");
    }

    @Nested
    @DisplayName("generate()")
    class GenerateTests {

        @Test
        @DisplayName("Should generate mapper interface with correct package")
        void shouldGenerateMapperInterfaceWithCorrectPackage() {
            SqlTable table = createSimpleTable("products");

            String result =
                    mapperGenerator.generate(
                            table,
                            Collections.emptyList(),
                            Collections.emptyList(),
                            Collections.emptyList());

            assertThat(result).contains("package com.example.products.application.mapper;");
        }

        @Test
        @DisplayName("Should generate correct imports")
        void shouldGenerateCorrectImports() {
            SqlTable table = createSimpleTable("products");

            String result =
                    mapperGenerator.generate(
                            table,
                            Collections.emptyList(),
                            Collections.emptyList(),
                            Collections.emptyList());

            assertThat(result)
                    .contains("import com.jnzader.apigen.core.application.mapper.BaseMapper;")
                    .contains("import com.example.products.application.dto.ProductDTO;")
                    .contains("import com.example.products.domain.entity.Product;")
                    .contains("import org.mapstruct.*;");
        }

        @Test
        @DisplayName("Should generate MapStruct mapper annotation")
        void shouldGenerateMapStructMapperAnnotation() {
            SqlTable table = createSimpleTable("products");

            String result =
                    mapperGenerator.generate(
                            table,
                            Collections.emptyList(),
                            Collections.emptyList(),
                            Collections.emptyList());

            assertThat(result).contains("@Mapper(componentModel = \"spring\")");
        }

        @Test
        @DisplayName("Should extend BaseMapper with correct types")
        void shouldExtendBaseMapperWithCorrectTypes() {
            SqlTable table = createSimpleTable("products");

            String result =
                    mapperGenerator.generate(
                            table,
                            Collections.emptyList(),
                            Collections.emptyList(),
                            Collections.emptyList());

            assertThat(result)
                    .contains(
                            "public interface ProductMapper extends BaseMapper<Product,"
                                    + " ProductDTO>");
        }

        @Test
        @DisplayName("Should include comment about inherited methods")
        void shouldIncludeCommentAboutInheritedMethods() {
            SqlTable table = createSimpleTable("products");

            String result =
                    mapperGenerator.generate(
                            table,
                            Collections.emptyList(),
                            Collections.emptyList(),
                            Collections.emptyList());

            assertThat(result)
                    .contains(
                            "// Inherits toDTO, toEntity, updateEntityFromDTO, updateDTOFromEntity"
                                    + " from BaseMapper");
        }

        @ParameterizedTest(name = "Should handle table {0} -> {1}Mapper")
        @CsvSource({
            "categories, Category, categories, 'Category, CategoryDTO'",
            "boxes, Box, boxes, 'Box, BoxDTO'",
            "user_profiles, UserProfile, userprofiles, 'UserProfile, UserProfileDTO'",
            "products, Product, products, 'Product, ProductDTO'"
        })
        @DisplayName("Should handle various table names correctly")
        void shouldHandleVariousTableNames(
                String tableName, String entityName, String moduleName, String expectedTypes) {
            SqlTable table = createSimpleTable(tableName);

            String result =
                    mapperGenerator.generate(
                            table,
                            Collections.emptyList(),
                            Collections.emptyList(),
                            Collections.emptyList());

            assertThat(result)
                    .contains("package com.example." + moduleName + ".application.mapper;")
                    .contains(
                            "import com.example."
                                    + moduleName
                                    + ".application.dto."
                                    + entityName
                                    + "DTO;")
                    .contains(
                            "import com.example."
                                    + moduleName
                                    + ".domain.entity."
                                    + entityName
                                    + ";")
                    .contains(
                            "public interface "
                                    + entityName
                                    + "Mapper extends BaseMapper<"
                                    + entityName
                                    + ", "
                                    + entityName
                                    + "DTO>")
                    .contains(expectedTypes);
        }

        @Test
        @DisplayName("Should generate relationship mappings for ManyToOne")
        void shouldGenerateRelationshipMappingsForManyToOne() {
            SqlTable orderTable = createSimpleTable("orders");
            SqlTable customerTable = createSimpleTable("customers");

            SqlForeignKey fk =
                    SqlForeignKey.builder()
                            .columnName("customer_id")
                            .referencedTable("customers")
                            .referencedColumn("id")
                            .build();

            SqlSchema.TableRelationship rel =
                    SqlSchema.TableRelationship.builder()
                            .sourceTable(orderTable)
                            .targetTable(customerTable)
                            .foreignKey(fk)
                            .relationType(RelationType.MANY_TO_ONE)
                            .build();

            String result =
                    mapperGenerator.generate(
                            orderTable,
                            List.of(rel),
                            Collections.emptyList(),
                            Collections.emptyList());

            assertThat(result)
                    .contains("@Override")
                    .contains("@Mapping(source = \"estado\", target = \"activo\")")
                    .contains("@Mapping(source = \"customer.id\", target = \"customerId\")")
                    .contains("OrderDTO toDTO(Order entity)")
                    .contains("@Mapping(target = \"customer\", ignore = true)")
                    .contains("Order toEntity(OrderDTO dto)")
                    .contains("void updateEntityFromDTO(OrderDTO dto, @MappingTarget Order entity)")
                    .contains(
                            "void updateDTOFromEntity(Order entity, @MappingTarget OrderDTO dto)");
        }

        @Test
        @DisplayName("Should generate relationship mappings for ManyToMany")
        void shouldGenerateRelationshipMappingsForManyToMany() {
            SqlTable productTable = createSimpleTable("products");
            SqlTable categoryTable = createSimpleTable("categories");

            ManyToManyRelation m2mRel =
                    new ManyToManyRelation(
                            "product_categories", "product_id", "category_id", categoryTable);

            String result =
                    mapperGenerator.generate(
                            productTable,
                            Collections.emptyList(),
                            Collections.emptyList(),
                            List.of(m2mRel));

            assertThat(result)
                    .contains("@Override")
                    .contains("@Mapping(target = \"categoriesIds\", ignore = true)")
                    .contains("ProductDTO toDTO(Product entity)")
                    .contains("@Mapping(target = \"categories\", ignore = true)")
                    .contains("Product toEntity(ProductDTO dto)")
                    .contains(
                            "void updateEntityFromDTO(ProductDTO dto, @MappingTarget Product"
                                    + " entity)")
                    .contains(
                            "void updateDTOFromEntity(Product entity, @MappingTarget ProductDTO"
                                    + " dto)");
        }

        @Test
        @DisplayName("Should generate mappings for inverse OneToMany relationships")
        void shouldGenerateMappingsForInverseRelationships() {
            SqlTable customerTable = createSimpleTable("customers");
            SqlTable orderTable = createSimpleTable("orders");

            SqlForeignKey fk =
                    SqlForeignKey.builder()
                            .columnName("customer_id")
                            .referencedTable("customers")
                            .referencedColumn("id")
                            .build();

            // Incoming relationship: orders -> customers (customer has many orders)
            SqlSchema.TableRelationship incomingRel =
                    SqlSchema.TableRelationship.builder()
                            .sourceTable(orderTable)
                            .targetTable(customerTable)
                            .foreignKey(fk)
                            .relationType(RelationType.MANY_TO_ONE)
                            .build();

            String result =
                    mapperGenerator.generate(
                            customerTable,
                            Collections.emptyList(),
                            List.of(incomingRel),
                            Collections.emptyList());

            // Should ignore the inverse collection "orders" in toEntity and updateEntityFromDTO
            assertThat(result)
                    .contains("@Mapping(target = \"orders\", ignore = true)")
                    .contains("Customer toEntity(CustomerDTO dto)")
                    .contains(
                            "void updateEntityFromDTO(CustomerDTO dto, @MappingTarget Customer"
                                    + " entity)");
        }
    }

    private SqlTable createSimpleTable(String tableName) {
        return SqlTable.builder()
                .name(tableName)
                .columns(
                        List.of(
                                SqlColumn.builder()
                                        .name("id")
                                        .javaType("Long")
                                        .primaryKey(true)
                                        .build()))
                .foreignKeys(new ArrayList<>())
                .indexes(new ArrayList<>())
                .build();
    }
}
