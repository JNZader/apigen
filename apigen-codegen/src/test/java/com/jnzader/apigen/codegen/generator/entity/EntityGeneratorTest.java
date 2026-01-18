package com.jnzader.apigen.codegen.generator.entity;

import com.jnzader.apigen.codegen.generator.entity.EntityGenerator.ManyToManyRelation;
import com.jnzader.apigen.codegen.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("EntityGenerator Tests")
class EntityGeneratorTest {

    private EntityGenerator entityGenerator;

    @BeforeEach
    void setUp() {
        entityGenerator = new EntityGenerator("com.example");
    }

    @Nested
    @DisplayName("generate()")
    class GenerateTests {

        @Test
        @DisplayName("Should generate basic entity with package and imports")
        void shouldGenerateBasicEntityWithPackageAndImports() {
            SqlTable table = createSimpleTable("products");

            String result = entityGenerator.generate(table, List.of(), List.of(), List.of());

            assertThat(result)
                    .contains("package com.example.products.domain.entity;")
                    .contains("@Entity")
                    .contains("@Table(name = \"products\")")
                    .contains("@Audited")
                    .contains("@Getter")
                    .contains("@Setter")
                    .contains("public class Product extends Base");
        }

        @Test
        @DisplayName("Should generate entity with string field")
        void shouldGenerateEntityWithStringField() {
            SqlTable table = createTableWithColumn("products",
                    createColumn("title", "String", false, false, 255));

            String result = entityGenerator.generate(table, List.of(), List.of(), List.of());

            assertThat(result)
                    .contains("@NotBlank")
                    .contains("@Size(max = 255)")
                    .contains("@Column(name = \"title\", nullable = false, length = 255)")
                    .contains("private String title;");
        }

        @Test
        @DisplayName("Should generate entity with nullable field")
        void shouldGenerateEntityWithNullableField() {
            SqlTable table = createTableWithColumn("products",
                    createColumn("description", "String", true, false, 1000));

            String result = entityGenerator.generate(table, List.of(), List.of(), List.of());

            assertThat(result)
                    .doesNotContain("@NotBlank")
                    .contains("@Column(name = \"description\", length = 1000)")
                    .contains("private String description;");
        }

        @Test
        @DisplayName("Should generate entity with unique field")
        void shouldGenerateEntityWithUniqueField() {
            SqlTable table = createTableWithColumn("users",
                    createColumn("email", "String", false, true, 100));

            String result = entityGenerator.generate(table, List.of(), List.of(), List.of());

            assertThat(result).contains("unique = true");
        }

        @Test
        @DisplayName("Should generate entity with numeric field")
        void shouldGenerateEntityWithNumericField() {
            SqlColumn column = SqlColumn.builder()
                    .name("price")
                    .javaType("BigDecimal")
                    .nullable(false)
                    .unique(false)
                    .build();

            SqlTable table = createTableWithColumn("products", column);

            String result = entityGenerator.generate(table, List.of(), List.of(), List.of());

            assertThat(result)
                    .contains("import java.math.BigDecimal;")
                    .contains("@NotNull")
                    .contains("private BigDecimal price;");
        }

        @Test
        @DisplayName("Should generate entity with date field")
        void shouldGenerateEntityWithDateField() {
            SqlColumn column = SqlColumn.builder()
                    .name("event_date")
                    .javaType("LocalDateTime")
                    .nullable(true)
                    .build();

            SqlTable table = SqlTable.builder()
                    .name("events")
                    .columns(List.of(
                            SqlColumn.builder().name("id").javaType("Long").primaryKey(true).build(),
                            column
                    ))
                    .foreignKeys(new ArrayList<>())
                    .indexes(new ArrayList<>())
                    .build();

            String result = entityGenerator.generate(table, List.of(), List.of(), List.of());

            assertThat(result).contains("import java.time.LocalDateTime;");
        }

        @Test
        @DisplayName("Should generate ManyToOne relationship")
        void shouldGenerateManyToOneRelationship() {
            SqlTable sourceTable = createSimpleTable("orders");
            SqlTable targetTable = createSimpleTable("customers");

            SqlForeignKey fk = SqlForeignKey.builder()
                    .columnName("customer_id")
                    .referencedTable("customers")
                    .referencedColumn("id")
                    .build();

            SqlSchema.TableRelationship relationship = SqlSchema.TableRelationship.builder()
                    .sourceTable(sourceTable)
                    .targetTable(targetTable)
                    .foreignKey(fk)
                    .relationType(RelationType.MANY_TO_ONE)
                    .build();

            String result = entityGenerator.generate(sourceTable, List.of(relationship), List.of(), List.of());

            assertThat(result)
                    .contains("@ManyToOne(fetch = FetchType.LAZY)")
                    .contains("@JoinColumn(name = \"customer_id\")")
                    .contains("private Customer customer;")
                    .contains("import com.example.customers.domain.entity.Customer;");
        }

        @Test
        @DisplayName("Should generate OneToOne relationship")
        void shouldGenerateOneToOneRelationship() {
            SqlTable sourceTable = createSimpleTable("users");
            SqlTable targetTable = createSimpleTable("profiles");

            SqlForeignKey fk = SqlForeignKey.builder()
                    .columnName("profile_id")
                    .referencedTable("profiles")
                    .referencedColumn("id")
                    .build();

            SqlSchema.TableRelationship relationship = SqlSchema.TableRelationship.builder()
                    .sourceTable(sourceTable)
                    .targetTable(targetTable)
                    .foreignKey(fk)
                    .relationType(RelationType.ONE_TO_ONE)
                    .build();

            String result = entityGenerator.generate(sourceTable, List.of(relationship), List.of(), List.of());

            assertThat(result)
                    .contains("@OneToOne(fetch = FetchType.LAZY)")
                    .contains("@JoinColumn(name = \"profile_id\", unique = true)")
                    .contains("private Profile profile;");
        }

        @Test
        @DisplayName("Should generate OneToMany inverse relationship")
        void shouldGenerateOneToManyInverseRelationship() {
            SqlTable sourceTable = createSimpleTable("orders");
            SqlTable targetTable = createSimpleTable("customers");

            SqlForeignKey fk = SqlForeignKey.builder()
                    .columnName("customer_id")
                    .referencedTable("customers")
                    .referencedColumn("id")
                    .build();

            SqlSchema.TableRelationship relationship = SqlSchema.TableRelationship.builder()
                    .sourceTable(sourceTable)
                    .targetTable(targetTable)
                    .foreignKey(fk)
                    .relationType(RelationType.MANY_TO_ONE)
                    .build();

            String result = entityGenerator.generate(targetTable, List.of(), List.of(relationship), List.of());

            assertThat(result)
                    .contains("@OneToMany(mappedBy = \"customer\", cascade = CascadeType.ALL, orphanRemoval = true)")
                    .contains("private List<Order> orders = new ArrayList<>();")
                    .contains("import java.util.List;")
                    .contains("import java.util.ArrayList;");
        }

        @Test
        @DisplayName("Should generate ManyToMany relationship")
        void shouldGenerateManyToManyRelationship() {
            SqlTable sourceTable = createSimpleTable("students");
            SqlTable targetTable = createSimpleTable("courses");

            ManyToManyRelation m2m = new ManyToManyRelation(
                    "student_courses", "student_id", "course_id", targetTable
            );

            String result = entityGenerator.generate(sourceTable, List.of(), List.of(), List.of(m2m));

            assertThat(result)
                    .contains("@ManyToMany")
                    .contains("@JoinTable(")
                    .contains("name = \"student_courses\"")
                    .contains("joinColumns = @JoinColumn(name = \"student_id\")")
                    .contains("inverseJoinColumns = @JoinColumn(name = \"course_id\")")
                    .contains("private List<Course> courses = new ArrayList<>();");
        }

        @Test
        @DisplayName("Should generate entity with indexes")
        void shouldGenerateEntityWithIndexes() {
            SqlIndex index = SqlIndex.builder()
                    .name("idx_products_sku")
                    .columns(List.of("sku"))
                    .unique(false)
                    .build();

            SqlTable table = SqlTable.builder()
                    .name("products")
                    .columns(List.of(
                            SqlColumn.builder().name("id").javaType("Long").primaryKey(true).build(),
                            SqlColumn.builder().name("sku").javaType("String").nullable(false).build()
                    ))
                    .indexes(List.of(index))
                    .foreignKeys(new ArrayList<>())
                    .build();

            String result = entityGenerator.generate(table, List.of(), List.of(), List.of());

            assertThat(result)
                    .contains("@Table(name = \"products\", indexes = {")
                    .contains("@Index");
        }

        @Test
        @DisplayName("Should handle reserved Java keywords as field names")
        void shouldHandleReservedJavaKeywordsAsFieldNames() {
            SqlColumn column = SqlColumn.builder()
                    .name("class")
                    .javaType("String")
                    .nullable(true)
                    .build();

            SqlTable table = createTableWithColumn("items", column);

            String result = entityGenerator.generate(table, List.of(), List.of(), List.of());

            assertThat(result).contains("private String classField;");
        }
    }

    private SqlTable createSimpleTable(String tableName) {
        return SqlTable.builder()
                .name(tableName)
                .columns(List.of(
                        SqlColumn.builder()
                                .name("id")
                                .javaType("Long")
                                .primaryKey(true)
                                .build()
                ))
                .foreignKeys(new ArrayList<>())
                .indexes(new ArrayList<>())
                .build();
    }

    private SqlTable createTableWithColumn(String tableName, SqlColumn column) {
        return SqlTable.builder()
                .name(tableName)
                .columns(List.of(
                        SqlColumn.builder()
                                .name("id")
                                .javaType("Long")
                                .primaryKey(true)
                                .build(),
                        column
                ))
                .foreignKeys(new ArrayList<>())
                .indexes(new ArrayList<>())
                .build();
    }

    private SqlColumn createColumn(String name, String type, boolean nullable, boolean unique, Integer length) {
        return SqlColumn.builder()
                .name(name)
                .javaType(type)
                .nullable(nullable)
                .unique(unique)
                .length(length)
                .build();
    }
}
