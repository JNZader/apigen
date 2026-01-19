package com.jnzader.apigen.codegen.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("SqlTable Tests")
class SqlTableTest {

    @Nested
    @DisplayName("getEntityName()")
    class GetEntityNameTests {

        @ParameterizedTest
        @CsvSource({
            "products, Product",
            "users, User",
            "categories, Category",
            "order_items, OrderItem",
            "user_roles, UserRole",
            "addresses, Address",
            "statuses, Status",
            "companies, Company"
        })
        @DisplayName("Should convert table name to PascalCase entity name")
        void shouldConvertTableNameToPascalCaseEntityName(String tableName, String expected) {
            SqlTable table = SqlTable.builder().name(tableName).build();

            String result = table.getEntityName();

            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("Should return null for null name")
        void shouldReturnNullForNullName() {
            SqlTable table = SqlTable.builder().name(null).build();

            assertThat(table.getEntityName()).isNull();
        }

        @Test
        @DisplayName("Should handle table with 'sses' ending")
        void shouldHandleTableWithSsesEnding() {
            SqlTable table = SqlTable.builder().name("classes").build();

            // 'classes' ends with 'sses', so it removes only 'es' → "Class"
            assertThat(table.getEntityName()).isEqualTo("Class");
        }
    }

    @Nested
    @DisplayName("getModuleName()")
    class GetModuleNameTests {

        @ParameterizedTest
        @CsvSource({
            "products, products",
            "order_items, orderitems",
            "user_roles, userroles",
            "USERS, users"
        })
        @DisplayName("Should return lowercase module name without underscores")
        void shouldReturnLowercaseModuleNameWithoutUnderscores(String tableName, String expected) {
            SqlTable table = SqlTable.builder().name(tableName).build();

            String result = table.getModuleName();

            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("Should return null for null name")
        void shouldReturnNullForNullName() {
            SqlTable table = SqlTable.builder().name(null).build();

            assertThat(table.getModuleName()).isNull();
        }
    }

    @Nested
    @DisplayName("getEntityVariableName()")
    class GetEntityVariableNameTests {

        @Test
        @DisplayName("Should return camelCase variable name")
        void shouldReturnCamelCaseVariableName() {
            SqlTable table = SqlTable.builder().name("products").build();

            String result = table.getEntityVariableName();

            assertThat(result).isEqualTo("product");
        }

        @Test
        @DisplayName("Should handle multi-word table names")
        void shouldHandleMultiWordTableNames() {
            SqlTable table = SqlTable.builder().name("order_items").build();

            String result = table.getEntityVariableName();

            assertThat(result).isEqualTo("orderItem");
        }

        @Test
        @DisplayName("Should return null for null name")
        void shouldReturnNullForNullName() {
            SqlTable table = SqlTable.builder().name(null).build();

            assertThat(table.getEntityVariableName()).isNull();
        }
    }

    @Nested
    @DisplayName("isJunctionTable()")
    class IsJunctionTableTests {

        @Test
        @DisplayName("Should return true for valid junction table")
        void shouldReturnTrueForValidJunctionTable() {
            SqlTable table =
                    SqlTable.builder()
                            .name("user_roles")
                            .foreignKeys(
                                    List.of(
                                            SqlForeignKey.builder()
                                                    .columnName("user_id")
                                                    .referencedTable("users")
                                                    .build(),
                                            SqlForeignKey.builder()
                                                    .columnName("role_id")
                                                    .referencedTable("roles")
                                                    .build()))
                            .primaryKeyColumns(List.of("user_id", "role_id"))
                            .build();

            assertThat(table.isJunctionTable()).isTrue();
        }

        @Test
        @DisplayName("Should return false for table with one foreign key")
        void shouldReturnFalseForTableWithOneForeignKey() {
            SqlTable table =
                    SqlTable.builder()
                            .name("orders")
                            .foreignKeys(
                                    List.of(
                                            SqlForeignKey.builder()
                                                    .columnName("user_id")
                                                    .referencedTable("users")
                                                    .build()))
                            .primaryKeyColumns(List.of("id"))
                            .build();

            assertThat(table.isJunctionTable()).isFalse();
        }

        @Test
        @DisplayName("Should return false for table with non-composite primary key")
        void shouldReturnFalseForTableWithNonCompositePrimaryKey() {
            SqlTable table =
                    SqlTable.builder()
                            .name("some_table")
                            .foreignKeys(
                                    List.of(
                                            SqlForeignKey.builder()
                                                    .columnName("user_id")
                                                    .referencedTable("users")
                                                    .build(),
                                            SqlForeignKey.builder()
                                                    .columnName("role_id")
                                                    .referencedTable("roles")
                                                    .build()))
                            .primaryKeyColumns(List.of("id"))
                            .build();

            assertThat(table.isJunctionTable()).isFalse();
        }

        @Test
        @DisplayName("Should return false for table with mismatched FK and PK columns")
        void shouldReturnFalseForTableWithMismatchedFkAndPkColumns() {
            SqlTable table =
                    SqlTable.builder()
                            .name("some_table")
                            .foreignKeys(
                                    List.of(
                                            SqlForeignKey.builder()
                                                    .columnName("user_id")
                                                    .referencedTable("users")
                                                    .build(),
                                            SqlForeignKey.builder()
                                                    .columnName("role_id")
                                                    .referencedTable("roles")
                                                    .build()))
                            .primaryKeyColumns(List.of("id", "other_id"))
                            .build();

            assertThat(table.isJunctionTable()).isFalse();
        }
    }

    @Nested
    @DisplayName("getColumnByName()")
    class GetColumnByNameTests {

        @Test
        @DisplayName("Should find column by name (case insensitive)")
        void shouldFindColumnByNameCaseInsensitive() {
            SqlColumn idColumn = SqlColumn.builder().name("id").javaType("Long").build();
            SqlColumn nameColumn = SqlColumn.builder().name("name").javaType("String").build();

            SqlTable table =
                    SqlTable.builder()
                            .name("products")
                            .columns(List.of(idColumn, nameColumn))
                            .build();

            assertThat(table.getColumnByName("NAME")).isEqualTo(nameColumn);
            assertThat(table.getColumnByName("name")).isEqualTo(nameColumn);
        }

        @Test
        @DisplayName("Should return null for non-existent column")
        void shouldReturnNullForNonExistentColumn() {
            SqlTable table =
                    SqlTable.builder()
                            .name("products")
                            .columns(List.of(SqlColumn.builder().name("id").build()))
                            .build();

            assertThat(table.getColumnByName("nonexistent")).isNull();
        }
    }

    @Nested
    @DisplayName("getBusinessColumns()")
    class GetBusinessColumnsTests {

        @Test
        @DisplayName("Should exclude primary key columns")
        void shouldExcludePrimaryKeyColumns() {
            SqlColumn idColumn = SqlColumn.builder().name("id").primaryKey(true).build();
            SqlColumn nameColumn = SqlColumn.builder().name("name").primaryKey(false).build();

            SqlTable table =
                    SqlTable.builder()
                            .name("products")
                            .columns(List.of(idColumn, nameColumn))
                            .foreignKeys(new ArrayList<>())
                            .build();

            List<SqlColumn> result = table.getBusinessColumns();

            assertThat(result).containsExactly(nameColumn).doesNotContain(idColumn);
        }

        @Test
        @DisplayName("Should exclude foreign key columns")
        void shouldExcludeForeignKeyColumns() {
            SqlColumn idColumn = SqlColumn.builder().name("id").primaryKey(true).build();
            SqlColumn categoryIdColumn =
                    SqlColumn.builder().name("category_id").primaryKey(false).build();
            SqlColumn nameColumn = SqlColumn.builder().name("name").primaryKey(false).build();

            SqlTable table =
                    SqlTable.builder()
                            .name("products")
                            .columns(List.of(idColumn, categoryIdColumn, nameColumn))
                            .foreignKeys(
                                    List.of(
                                            SqlForeignKey.builder()
                                                    .columnName("category_id")
                                                    .referencedTable("categories")
                                                    .build()))
                            .build();

            List<SqlColumn> result = table.getBusinessColumns();

            assertThat(result).containsExactly(nameColumn);
        }

        @Test
        @DisplayName("Should exclude base audit columns")
        void shouldExcludeBaseAuditColumns() {
            SqlColumn idColumn = SqlColumn.builder().name("id").primaryKey(true).build();
            SqlColumn nameColumn = SqlColumn.builder().name("name").primaryKey(false).build();
            SqlColumn createdAtColumn =
                    SqlColumn.builder().name("created_at").primaryKey(false).build();
            SqlColumn estadoColumn = SqlColumn.builder().name("estado").primaryKey(false).build();

            SqlTable table =
                    SqlTable.builder()
                            .name("products")
                            .columns(List.of(idColumn, nameColumn, createdAtColumn, estadoColumn))
                            .foreignKeys(new ArrayList<>())
                            .build();

            List<SqlColumn> result = table.getBusinessColumns();

            assertThat(result).containsExactly(nameColumn);
        }
    }

    @Nested
    @DisplayName("extendsBase()")
    class ExtendsBaseTests {

        @Test
        @DisplayName("Should return true when table has 'estado' column")
        void shouldReturnTrueWhenTableHasEstadoColumn() {
            SqlTable table =
                    SqlTable.builder()
                            .name("products")
                            .columns(
                                    List.of(
                                            SqlColumn.builder().name("id").build(),
                                            SqlColumn.builder().name("estado").build()))
                            .build();

            assertThat(table.extendsBase()).isTrue();
        }

        @Test
        @DisplayName("Should return true when table has 'created_at' column")
        void shouldReturnTrueWhenTableHasCreatedAtColumn() {
            SqlTable table =
                    SqlTable.builder()
                            .name("products")
                            .columns(
                                    List.of(
                                            SqlColumn.builder().name("id").build(),
                                            SqlColumn.builder().name("created_at").build()))
                            .build();

            assertThat(table.extendsBase()).isTrue();
        }

        @Test
        @DisplayName("Should return false when table has no audit columns")
        void shouldReturnFalseWhenTableHasNoAuditColumns() {
            SqlTable table =
                    SqlTable.builder()
                            .name("lookup_table")
                            .columns(
                                    List.of(
                                            SqlColumn.builder().name("id").build(),
                                            SqlColumn.builder().name("code").build(),
                                            SqlColumn.builder().name("description").build()))
                            .build();

            assertThat(table.extendsBase()).isFalse();
        }
    }

    @Nested
    @DisplayName("Builder defaults")
    class BuilderDefaultsTests {

        @Test
        @DisplayName("Should initialize lists with empty ArrayLists")
        void shouldInitializeListsWithEmptyArrayLists() {
            SqlTable table = SqlTable.builder().name("test").build();

            assertThat(table.getColumns()).isNotNull().isEmpty();
            assertThat(table.getForeignKeys()).isNotNull().isEmpty();
            assertThat(table.getIndexes()).isNotNull().isEmpty();
            assertThat(table.getPrimaryKeyColumns()).isNotNull().isEmpty();
            assertThat(table.getUniqueConstraints()).isNotNull().isEmpty();
            assertThat(table.getCheckConstraints()).isNotNull().isEmpty();
        }
    }
}
