package com.jnzader.apigen.codegen.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("SqlForeignKey Tests")
class SqlForeignKeyTest {

    @Nested
    @DisplayName("getReferencedEntityName()")
    class GetReferencedEntityNameTests {

        @ParameterizedTest
        @CsvSource({
            "users, User",
            "categories, Category",
            "order_items, OrderItem",
            "companies, Company",
            "addresses, Address"
        })
        @DisplayName("Should convert referenced table name to entity name")
        void shouldConvertReferencedTableNameToEntityName(String tableName, String expected) {
            SqlForeignKey fk = SqlForeignKey.builder().referencedTable(tableName).build();

            String result = fk.getReferencedEntityName();

            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("Should return null for null referenced table")
        void shouldReturnNullForNullReferencedTable() {
            SqlForeignKey fk = SqlForeignKey.builder().referencedTable(null).build();

            assertThat(fk.getReferencedEntityName()).isNull();
        }

        @Test
        @DisplayName("Should handle table names with 'sses' ending")
        void shouldHandleTableNamesWithSsesEnding() {
            SqlForeignKey fk = SqlForeignKey.builder().referencedTable("classes").build();

            // "classes" ends with "sses" so removes only 'es' → "Class"
            assertThat(fk.getReferencedEntityName()).isEqualTo("Class");
        }
    }

    @Nested
    @DisplayName("getJavaFieldName()")
    class GetJavaFieldNameTests {

        @ParameterizedTest
        @CsvSource({
            "user_id, user",
            "category_id, category",
            "created_by_id, createdBy",
            "parent_category_id, parentCategory",
            "status, status"
        })
        @DisplayName("Should convert column name to Java field name")
        void shouldConvertColumnNameToJavaFieldName(String columnName, String expected) {
            SqlForeignKey fk = SqlForeignKey.builder().columnName(columnName).build();

            String result = fk.getJavaFieldName();

            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("Should remove _id suffix")
        void shouldRemoveIdSuffix() {
            SqlForeignKey fk = SqlForeignKey.builder().columnName("category_id").build();

            String result = fk.getJavaFieldName();

            assertThat(result).isEqualTo("category").doesNotContain("Id");
        }
    }

    @Nested
    @DisplayName("inferRelationType()")
    class InferRelationTypeTests {

        @Test
        @DisplayName("Should return MANY_TO_ONE for regular FK")
        void shouldReturnManyToOneForRegularFk() {
            SqlForeignKey fk =
                    SqlForeignKey.builder()
                            .columnName("category_id")
                            .referencedTable("categories")
                            .build();

            SqlColumn fkColumn = SqlColumn.builder().name("category_id").unique(false).build();

            SqlTable parentTable =
                    SqlTable.builder()
                            .name("products")
                            .columns(
                                    List.of(
                                            SqlColumn.builder().name("id").primaryKey(true).build(),
                                            fkColumn))
                            .foreignKeys(List.of(fk))
                            .build();

            SqlTable referencedTable = SqlTable.builder().name("categories").build();

            RelationType result = fk.inferRelationType(parentTable, referencedTable);

            assertThat(result).isEqualTo(RelationType.MANY_TO_ONE);
        }

        @Test
        @DisplayName("Should return ONE_TO_ONE for unique FK")
        void shouldReturnOneToOneForUniqueFk() {
            SqlForeignKey fk =
                    SqlForeignKey.builder()
                            .columnName("profile_id")
                            .referencedTable("profiles")
                            .build();

            SqlColumn fkColumn = SqlColumn.builder().name("profile_id").unique(true).build();

            SqlTable parentTable =
                    SqlTable.builder()
                            .name("users")
                            .columns(
                                    List.of(
                                            SqlColumn.builder().name("id").primaryKey(true).build(),
                                            fkColumn))
                            .foreignKeys(List.of(fk))
                            .build();

            SqlTable referencedTable = SqlTable.builder().name("profiles").build();

            RelationType result = fk.inferRelationType(parentTable, referencedTable);

            assertThat(result).isEqualTo(RelationType.ONE_TO_ONE);
        }

        @Test
        @DisplayName("Should return MANY_TO_MANY for junction table FK")
        void shouldReturnManyToManyForJunctionTableFk() {
            SqlForeignKey fk1 =
                    SqlForeignKey.builder().columnName("user_id").referencedTable("users").build();

            SqlForeignKey fk2 =
                    SqlForeignKey.builder().columnName("role_id").referencedTable("roles").build();

            SqlTable junctionTable =
                    SqlTable.builder()
                            .name("user_roles")
                            .columns(
                                    List.of(
                                            SqlColumn.builder()
                                                    .name("user_id")
                                                    .primaryKey(true)
                                                    .build(),
                                            SqlColumn.builder()
                                                    .name("role_id")
                                                    .primaryKey(true)
                                                    .build()))
                            .foreignKeys(List.of(fk1, fk2))
                            .build();

            SqlTable referencedTable = SqlTable.builder().name("users").build();

            RelationType result = fk1.inferRelationType(junctionTable, referencedTable);

            assertThat(result).isEqualTo(RelationType.MANY_TO_MANY);
        }
    }

    @Nested
    @DisplayName("ForeignKeyAction enum")
    class ForeignKeyActionTests {

        @Test
        @DisplayName("Should have all expected values")
        void shouldHaveAllExpectedValues() {
            assertThat(SqlForeignKey.ForeignKeyAction.values())
                    .containsExactlyInAnyOrder(
                            SqlForeignKey.ForeignKeyAction.CASCADE,
                            SqlForeignKey.ForeignKeyAction.SET_NULL,
                            SqlForeignKey.ForeignKeyAction.SET_DEFAULT,
                            SqlForeignKey.ForeignKeyAction.RESTRICT,
                            SqlForeignKey.ForeignKeyAction.NO_ACTION);
        }
    }

    @Nested
    @DisplayName("Builder")
    class BuilderTests {

        @Test
        @DisplayName("Should build FK with all properties")
        void shouldBuildFkWithAllProperties() {
            SqlForeignKey fk =
                    SqlForeignKey.builder()
                            .name("fk_product_category")
                            .columnName("category_id")
                            .referencedTable("categories")
                            .referencedColumn("id")
                            .onDelete(SqlForeignKey.ForeignKeyAction.CASCADE)
                            .onUpdate(SqlForeignKey.ForeignKeyAction.NO_ACTION)
                            .build();

            assertThat(fk.getName()).isEqualTo("fk_product_category");
            assertThat(fk.getColumnName()).isEqualTo("category_id");
            assertThat(fk.getReferencedTable()).isEqualTo("categories");
            assertThat(fk.getReferencedColumn()).isEqualTo("id");
            assertThat(fk.getOnDelete()).isEqualTo(SqlForeignKey.ForeignKeyAction.CASCADE);
            assertThat(fk.getOnUpdate()).isEqualTo(SqlForeignKey.ForeignKeyAction.NO_ACTION);
        }
    }
}
