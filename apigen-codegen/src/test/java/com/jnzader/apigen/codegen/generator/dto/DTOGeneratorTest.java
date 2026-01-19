package com.jnzader.apigen.codegen.generator.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.jnzader.apigen.codegen.generator.entity.EntityGenerator.ManyToManyRelation;
import com.jnzader.apigen.codegen.model.*;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("DTOGenerator Tests")
class DTOGeneratorTest {

    private DTOGenerator dtoGenerator;

    @BeforeEach
    void setUp() {
        dtoGenerator = new DTOGenerator("com.example");
    }

    @Nested
    @DisplayName("generate()")
    class GenerateTests {

        @Test
        @DisplayName("Should generate basic DTO with package and imports")
        void shouldGenerateBasicDTOWithPackageAndImports() {
            SqlTable table = createSimpleTable("products");

            String result = dtoGenerator.generate(table, List.of(), List.of());

            assertThat(result)
                    .contains("package com.example.products.application.dto;")
                    .contains("@Data")
                    .contains("@NoArgsConstructor")
                    .contains("@AllArgsConstructor")
                    .contains("@Builder")
                    .contains("public class ProductDTO implements BaseDTO");
        }

        @Test
        @DisplayName("Should include base fields id and activo")
        void shouldIncludeBaseFieldsIdAndActivo() {
            SqlTable table = createSimpleTable("products");

            String result = dtoGenerator.generate(table, List.of(), List.of());

            assertThat(result)
                    .contains("@Null(groups = ValidationGroups.Create.class")
                    .contains("@NotNull(groups = ValidationGroups.Update.class")
                    .contains("private Long id;")
                    .contains("@Builder.Default")
                    .contains("private Boolean activo = true;");
        }

        @Test
        @DisplayName("Should implement BaseDTO interface methods")
        void shouldImplementBaseDTOInterfaceMethods() {
            SqlTable table = createSimpleTable("products");

            String result = dtoGenerator.generate(table, List.of(), List.of());

            assertThat(result)
                    .contains("@Override")
                    .contains("public Long id()")
                    .contains("return this.id;")
                    .contains("public Boolean activo()")
                    .contains("return this.activo;");
        }

        @Test
        @DisplayName("Should generate DTO with string field and validations")
        void shouldGenerateDTOWithStringFieldAndValidations() {
            SqlColumn column =
                    SqlColumn.builder()
                            .name("title")
                            .javaType("String")
                            .nullable(false)
                            .length(255)
                            .build();

            SqlTable table = createTableWithColumn("products", column);

            String result = dtoGenerator.generate(table, List.of(), List.of());

            assertThat(result)
                    .contains("@NotBlank @Size(max = 255)")
                    .contains("private String title;");
        }

        @Test
        @DisplayName("Should generate DTO with nullable field without NotBlank")
        void shouldGenerateDTOWithNullableFieldWithoutNotBlank() {
            SqlColumn column =
                    SqlColumn.builder()
                            .name("description")
                            .javaType("String")
                            .nullable(true)
                            .length(1000)
                            .build();

            SqlTable table = createTableWithColumn("products", column);

            String result = dtoGenerator.generate(table, List.of(), List.of());

            assertThat(result)
                    .doesNotContain("@NotBlank")
                    .contains("@Size(max = 1000)")
                    .contains("private String description;");
        }

        @Test
        @DisplayName("Should generate DTO with numeric field")
        void shouldGenerateDTOWithNumericField() {
            SqlColumn column =
                    SqlColumn.builder()
                            .name("price")
                            .javaType("BigDecimal")
                            .nullable(false)
                            .build();

            SqlTable table = createTableWithColumn("products", column);

            String result = dtoGenerator.generate(table, List.of(), List.of());

            assertThat(result)
                    .contains("import java.math.BigDecimal;")
                    .contains("@NotNull")
                    .contains("private BigDecimal price;");
        }

        @Test
        @DisplayName("Should generate DTO with date field")
        void shouldGenerateDTOWithDateField() {
            SqlColumn column =
                    SqlColumn.builder()
                            .name("release_date")
                            .javaType("LocalDate")
                            .nullable(true)
                            .build();

            SqlTable table = createTableWithColumn("products", column);

            String result = dtoGenerator.generate(table, List.of(), List.of());

            assertThat(result)
                    .contains("import java.time.LocalDate;")
                    .contains("private LocalDate releaseDate;");
        }

        @Test
        @DisplayName("Should add relation ID field for ManyToOne")
        void shouldAddRelationIdFieldForManyToOne() {
            SqlTable sourceTable = createSimpleTable("orders");
            SqlTable targetTable = createSimpleTable("customers");

            SqlForeignKey fk =
                    SqlForeignKey.builder()
                            .columnName("customer_id")
                            .referencedTable("customers")
                            .referencedColumn("id")
                            .build();

            SqlSchema.TableRelationship relationship =
                    SqlSchema.TableRelationship.builder()
                            .sourceTable(sourceTable)
                            .targetTable(targetTable)
                            .foreignKey(fk)
                            .relationType(RelationType.MANY_TO_ONE)
                            .build();

            String result = dtoGenerator.generate(sourceTable, List.of(relationship), List.of());

            assertThat(result).contains("private Long customerId;");
        }

        @Test
        @DisplayName("Should add list of IDs for ManyToMany")
        void shouldAddListOfIdsForManyToMany() {
            SqlTable sourceTable = createSimpleTable("students");
            SqlTable targetTable = createSimpleTable("courses");

            ManyToManyRelation m2m =
                    new ManyToManyRelation(
                            "student_courses", "student_id", "course_id", targetTable);

            String result = dtoGenerator.generate(sourceTable, List.of(), List.of(m2m));

            assertThat(result)
                    .contains("import java.util.List;")
                    .contains("private List<Long> coursesIds;");
        }

        @Test
        @DisplayName("Should handle reserved Java keywords as field names")
        void shouldHandleReservedJavaKeywordsAsFieldNames() {
            SqlColumn column =
                    SqlColumn.builder().name("class").javaType("String").nullable(true).build();

            SqlTable table = createTableWithColumn("items", column);

            String result = dtoGenerator.generate(table, List.of(), List.of());

            assertThat(result).contains("private String classField;");
        }

        @Test
        @DisplayName("Should handle multiple business columns")
        void shouldHandleMultipleBusinessColumns() {
            List<SqlColumn> columns =
                    List.of(
                            SqlColumn.builder()
                                    .name("id")
                                    .javaType("Long")
                                    .primaryKey(true)
                                    .build(),
                            SqlColumn.builder()
                                    .name("name")
                                    .javaType("String")
                                    .nullable(false)
                                    .length(100)
                                    .build(),
                            SqlColumn.builder()
                                    .name("price")
                                    .javaType("BigDecimal")
                                    .nullable(false)
                                    .build(),
                            SqlColumn.builder()
                                    .name("stock")
                                    .javaType("Integer")
                                    .nullable(true)
                                    .build());

            SqlTable table =
                    SqlTable.builder()
                            .name("products")
                            .columns(columns)
                            .foreignKeys(new ArrayList<>())
                            .indexes(new ArrayList<>())
                            .build();

            String result = dtoGenerator.generate(table, List.of(), List.of());

            assertThat(result)
                    .contains("private String name;")
                    .contains("private BigDecimal price;")
                    .contains("private Integer stock;");
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

    private SqlTable createTableWithColumn(String tableName, SqlColumn column) {
        return SqlTable.builder()
                .name(tableName)
                .columns(
                        List.of(
                                SqlColumn.builder()
                                        .name("id")
                                        .javaType("Long")
                                        .primaryKey(true)
                                        .build(),
                                column))
                .foreignKeys(new ArrayList<>())
                .indexes(new ArrayList<>())
                .build();
    }
}
