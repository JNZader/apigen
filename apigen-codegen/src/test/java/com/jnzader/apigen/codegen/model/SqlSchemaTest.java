package com.jnzader.apigen.codegen.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("SqlSchema Tests")
class SqlSchemaTest {

    @Nested
    @DisplayName("getEntityTables()")
    class GetEntityTablesTests {

        @Test
        @DisplayName("Should exclude junction tables")
        void shouldExcludeJunctionTables() {
            SqlTable products = createSimpleTable("products");
            SqlTable categories = createSimpleTable("categories");
            SqlTable productCategories =
                    createJunctionTable("product_categories", "products", "categories");

            SqlSchema schema =
                    SqlSchema.builder()
                            .tables(List.of(products, categories, productCategories))
                            .build();

            List<SqlTable> result = schema.getEntityTables();

            assertThat(result)
                    .containsExactlyInAnyOrder(products, categories)
                    .doesNotContain(productCategories);
        }

        @Test
        @DisplayName("Should exclude audit tables")
        void shouldExcludeAuditTables() {
            SqlTable products = createSimpleTable("products");
            SqlTable productsAud = createSimpleTable("products_aud");
            SqlTable productsAudit = createSimpleTable("products_audit");
            SqlTable revisionInfo = createSimpleTable("revision_info");

            SqlSchema schema =
                    SqlSchema.builder()
                            .tables(List.of(products, productsAud, productsAudit, revisionInfo))
                            .build();

            List<SqlTable> result = schema.getEntityTables();

            assertThat(result).containsExactly(products);
        }
    }

    @Nested
    @DisplayName("getJunctionTables()")
    class GetJunctionTablesTests {

        @Test
        @DisplayName("Should return only junction tables")
        void shouldReturnOnlyJunctionTables() {
            SqlTable products = createSimpleTable("products");
            SqlTable categories = createSimpleTable("categories");
            SqlTable productCategories =
                    createJunctionTable("product_categories", "products", "categories");

            SqlSchema schema =
                    SqlSchema.builder()
                            .tables(List.of(products, categories, productCategories))
                            .build();

            List<SqlTable> result = schema.getJunctionTables();

            assertThat(result).containsExactly(productCategories);
        }
    }

    @Nested
    @DisplayName("getTableByName()")
    class GetTableByNameTests {

        @Test
        @DisplayName("Should find table by name (case insensitive)")
        void shouldFindTableByNameCaseInsensitive() {
            SqlTable products = createSimpleTable("products");
            SqlTable categories = createSimpleTable("categories");

            SqlSchema schema = SqlSchema.builder().tables(List.of(products, categories)).build();

            assertThat(schema.getTableByName("PRODUCTS")).isEqualTo(products);
            assertThat(schema.getTableByName("products")).isEqualTo(products);
        }

        @Test
        @DisplayName("Should return null for non-existent table")
        void shouldReturnNullForNonExistentTable() {
            SqlSchema schema =
                    SqlSchema.builder().tables(List.of(createSimpleTable("products"))).build();

            assertThat(schema.getTableByName("nonexistent")).isNull();
        }
    }

    @Nested
    @DisplayName("getAllRelationships()")
    class GetAllRelationshipsTests {

        @Test
        @DisplayName("Should return relationships for foreign keys")
        void shouldReturnRelationshipsForForeignKeys() {
            SqlTable products = createSimpleTable("products");
            SqlTable categories = createSimpleTable("categories");

            SqlColumn categoryIdColumn =
                    SqlColumn.builder().name("category_id").javaType("Long").unique(false).build();
            products.getColumns().add(categoryIdColumn);
            products.getForeignKeys()
                    .add(
                            SqlForeignKey.builder()
                                    .columnName("category_id")
                                    .referencedTable("categories")
                                    .referencedColumn("id")
                                    .build());

            SqlSchema schema = SqlSchema.builder().tables(List.of(products, categories)).build();

            List<SqlSchema.TableRelationship> result = schema.getAllRelationships();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getSourceTable()).isEqualTo(products);
            assertThat(result.get(0).getTargetTable()).isEqualTo(categories);
            assertThat(result.get(0).getRelationType()).isEqualTo(RelationType.MANY_TO_ONE);
        }

        @Test
        @DisplayName("Should skip relationships to non-existent tables")
        void shouldSkipRelationshipsToNonExistentTables() {
            SqlTable products = createSimpleTable("products");
            products.getForeignKeys()
                    .add(
                            SqlForeignKey.builder()
                                    .columnName("category_id")
                                    .referencedTable("nonexistent")
                                    .build());

            SqlSchema schema = SqlSchema.builder().tables(List.of(products)).build();

            List<SqlSchema.TableRelationship> result = schema.getAllRelationships();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getTablesByModule()")
    class GetTablesByModuleTests {

        @Test
        @DisplayName("Should group tables by module name")
        void shouldGroupTablesByModuleName() {
            SqlTable products = createSimpleTable("products");
            SqlTable productVariants = createSimpleTable("product_variants");
            SqlTable users = createSimpleTable("users");

            SqlSchema schema =
                    SqlSchema.builder().tables(List.of(products, productVariants, users)).build();

            Map<String, List<SqlTable>> result = schema.getTablesByModule();

            assertThat(result)
                    .containsKey("products")
                    .containsKey("productvariants")
                    .containsKey("users");
        }
    }

    @Nested
    @DisplayName("validate()")
    class ValidateTests {

        @Test
        @DisplayName("Should report tables without primary keys")
        void shouldReportTablesWithoutPrimaryKeys() {
            SqlTable table =
                    SqlTable.builder()
                            .name("no_pk_table")
                            .columns(List.of(SqlColumn.builder().name("name").build()))
                            .primaryKeyColumns(new ArrayList<>())
                            .foreignKeys(new ArrayList<>())
                            .build();

            SqlSchema schema = SqlSchema.builder().tables(List.of(table)).build();

            List<String> issues = schema.validate();

            assertThat(issues).anyMatch(issue -> issue.contains("no primary key"));
        }

        @Test
        @DisplayName("Should report dangling foreign keys")
        void shouldReportDanglingForeignKeys() {
            SqlTable products = createSimpleTable("products");
            products.getForeignKeys()
                    .add(
                            SqlForeignKey.builder()
                                    .columnName("category_id")
                                    .referencedTable("nonexistent_table")
                                    .build());

            SqlSchema schema = SqlSchema.builder().tables(List.of(products)).build();

            List<String> issues = schema.validate();

            assertThat(issues).anyMatch(issue -> issue.contains("non-existent table"));
        }

        @Test
        @DisplayName("Should report duplicate entity names")
        void shouldReportDuplicateEntityNames() {
            // Both tables would generate "User" entity name
            SqlTable users = createSimpleTable("users");
            SqlTable user = createSimpleTable("user");

            SqlSchema schema = SqlSchema.builder().tables(List.of(users, user)).build();

            List<String> issues = schema.validate();

            assertThat(issues).anyMatch(issue -> issue.contains("Multiple tables"));
        }

        @Test
        @DisplayName("Should return empty list for valid schema")
        void shouldReturnEmptyListForValidSchema() {
            SqlTable products = createSimpleTable("products");
            SqlTable categories = createSimpleTable("categories");

            SqlSchema schema = SqlSchema.builder().tables(List.of(products, categories)).build();

            List<String> issues = schema.validate();

            assertThat(issues).isEmpty();
        }
    }

    @Nested
    @DisplayName("Builder defaults")
    class BuilderDefaultsTests {

        @Test
        @DisplayName("Should initialize lists with empty ArrayLists")
        void shouldInitializeListsWithEmptyArrayLists() {
            SqlSchema schema = SqlSchema.builder().build();

            assertThat(schema.getTables()).isNotNull().isEmpty();
            assertThat(schema.getFunctions()).isNotNull().isEmpty();
            assertThat(schema.getStandaloneIndexes()).isNotNull().isEmpty();
            assertThat(schema.getExtensions()).isNotNull().isEmpty();
            assertThat(schema.getParseErrors()).isNotNull().isEmpty();
        }
    }

    // Helper methods

    private SqlTable createSimpleTable(String name) {
        return SqlTable.builder()
                .name(name)
                .columns(
                        new ArrayList<>(
                                List.of(
                                        SqlColumn.builder()
                                                .name("id")
                                                .javaType("Long")
                                                .primaryKey(true)
                                                .build())))
                .primaryKeyColumns(new ArrayList<>(List.of("id")))
                .foreignKeys(new ArrayList<>())
                .indexes(new ArrayList<>())
                .build();
    }

    private SqlTable createJunctionTable(String name, String table1, String table2) {
        String col1 = table1.substring(0, table1.length() - 1) + "_id";
        String col2 = table2.substring(0, table2.length() - 1) + "_id";

        return SqlTable.builder()
                .name(name)
                .columns(
                        new ArrayList<>(
                                List.of(
                                        SqlColumn.builder()
                                                .name(col1)
                                                .javaType("Long")
                                                .primaryKey(true)
                                                .build(),
                                        SqlColumn.builder()
                                                .name(col2)
                                                .javaType("Long")
                                                .primaryKey(true)
                                                .build())))
                .primaryKeyColumns(new ArrayList<>(List.of(col1, col2)))
                .foreignKeys(
                        new ArrayList<>(
                                List.of(
                                        SqlForeignKey.builder()
                                                .columnName(col1)
                                                .referencedTable(table1)
                                                .build(),
                                        SqlForeignKey.builder()
                                                .columnName(col2)
                                                .referencedTable(table2)
                                                .build())))
                .indexes(new ArrayList<>())
                .build();
    }
}
