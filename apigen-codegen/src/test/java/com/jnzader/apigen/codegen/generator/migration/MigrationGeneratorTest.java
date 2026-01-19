package com.jnzader.apigen.codegen.generator.migration;

import static org.assertj.core.api.Assertions.assertThat;

import com.jnzader.apigen.codegen.model.*;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("MigrationGenerator Tests")
@SuppressWarnings(
        "java:S5976") // Tests validate different specific migration features, not the same feature
// with different inputs
class MigrationGeneratorTest {

    private MigrationGenerator migrationGenerator;

    @BeforeEach
    void setUp() {
        migrationGenerator = new MigrationGenerator();
    }

    @Nested
    @DisplayName("generate()")
    class GenerateTests {

        @Test
        @DisplayName("Should generate comment header with entity name")
        void shouldGenerateCommentHeaderWithEntityName() {
            SqlTable table = createSimpleTable("products");
            SqlSchema schema = createEmptySchema();

            String result = migrationGenerator.generate(table, schema);

            assertThat(result).contains("-- Auto-generated migration for: Product");
        }

        @Test
        @DisplayName("Should generate CREATE TABLE statement with table name")
        void shouldGenerateCreateTableStatementWithTableName() {
            SqlTable table = createSimpleTable("products");
            SqlSchema schema = createEmptySchema();

            String result = migrationGenerator.generate(table, schema);

            assertThat(result).contains("CREATE TABLE products (");
        }

        @Test
        @DisplayName("Should generate primary key column")
        void shouldGeneratePrimaryKeyColumn() {
            SqlTable table = createSimpleTable("products");
            SqlSchema schema = createEmptySchema();

            String result = migrationGenerator.generate(table, schema);

            assertThat(result).contains("id BIGINT PRIMARY KEY");
        }

        @Test
        @DisplayName("Should generate business columns")
        void shouldGenerateBusinessColumns() {
            SqlColumn column =
                    SqlColumn.builder()
                            .name("title")
                            .sqlType("VARCHAR(255)")
                            .javaType("String")
                            .nullable(false)
                            .build();

            SqlTable table = createTableWithColumn("products", column);
            SqlSchema schema = createEmptySchema();

            String result = migrationGenerator.generate(table, schema);

            assertThat(result).contains("title VARCHAR(255) NOT NULL");
        }

        @Test
        @DisplayName("Should generate nullable column without NOT NULL")
        void shouldGenerateNullableColumnWithoutNotNull() {
            SqlColumn column =
                    SqlColumn.builder()
                            .name("description")
                            .sqlType("TEXT")
                            .javaType("String")
                            .nullable(true)
                            .build();

            SqlTable table = createTableWithColumn("products", column);
            SqlSchema schema = createEmptySchema();

            String result = migrationGenerator.generate(table, schema);

            assertThat(result)
                    .contains("description TEXT")
                    .doesNotContain("description TEXT NOT NULL");
        }

        @Test
        @DisplayName("Should generate column with default value")
        void shouldGenerateColumnWithDefaultValue() {
            SqlColumn column =
                    SqlColumn.builder()
                            .name("status")
                            .sqlType("VARCHAR(20)")
                            .javaType("String")
                            .nullable(false)
                            .defaultValue("'ACTIVE'")
                            .build();

            SqlTable table = createTableWithColumn("products", column);
            SqlSchema schema = createEmptySchema();

            String result = migrationGenerator.generate(table, schema);

            assertThat(result).contains("status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'");
        }

        @Test
        @DisplayName("Should generate base entity fields")
        void shouldGenerateBaseEntityFields() {
            SqlTable table = createSimpleTable("products");
            SqlSchema schema = createEmptySchema();

            String result = migrationGenerator.generate(table, schema);

            assertThat(result)
                    .contains("-- Base entity fields")
                    .contains("estado BOOLEAN NOT NULL DEFAULT TRUE,")
                    .contains("fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,")
                    .contains("fecha_actualizacion TIMESTAMP,")
                    .contains("fecha_eliminacion TIMESTAMP,")
                    .contains("creado_por VARCHAR(100),")
                    .contains("modificado_por VARCHAR(100),")
                    .contains("eliminado_por VARCHAR(100),")
                    .contains("version BIGINT NOT NULL DEFAULT 0");
        }

        @Test
        @DisplayName("Should generate audit table for Hibernate Envers")
        void shouldGenerateAuditTableForHibernateEnvers() {
            SqlTable table = createSimpleTable("products");
            SqlSchema schema = createEmptySchema();

            String result = migrationGenerator.generate(table, schema);

            assertThat(result)
                    .contains("-- Audit table (Hibernate Envers)")
                    .contains("CREATE TABLE products_aud (")
                    .contains("id BIGINT NOT NULL,")
                    .contains("rev INTEGER NOT NULL REFERENCES revision_info(id),")
                    .contains("revtype SMALLINT,")
                    .contains("PRIMARY KEY (id, rev)");
        }

        @Test
        @DisplayName("Should generate business columns in audit table")
        void shouldGenerateBusinessColumnsInAuditTable() {
            SqlColumn column =
                    SqlColumn.builder()
                            .name("title")
                            .sqlType("VARCHAR(255)")
                            .javaType("String")
                            .nullable(false)
                            .build();

            SqlTable table = createTableWithColumn("products", column);
            SqlSchema schema = createEmptySchema();

            String result = migrationGenerator.generate(table, schema);

            assertThat(result).contains("title VARCHAR(255),");
        }

        @Test
        @DisplayName("Should generate standard indexes")
        void shouldGenerateStandardIndexes() {
            SqlTable table = createSimpleTable("products");
            SqlSchema schema = createEmptySchema();

            String result = migrationGenerator.generate(table, schema);

            assertThat(result)
                    .contains("-- Indexes")
                    .contains("CREATE INDEX idx_products_estado ON products(estado);")
                    .contains(
                            "CREATE INDEX idx_products_fecha_creacion ON products(fecha_creacion"
                                    + " DESC);");
        }

        @Test
        @DisplayName("Should generate foreign key column")
        void shouldGenerateForeignKeyColumn() {
            SqlForeignKey fk =
                    SqlForeignKey.builder()
                            .columnName("category_id")
                            .referencedTable("categories")
                            .referencedColumn("id")
                            .build();

            SqlTable table =
                    SqlTable.builder()
                            .name("products")
                            .columns(
                                    List.of(
                                            SqlColumn.builder()
                                                    .name("id")
                                                    .javaType("Long")
                                                    .primaryKey(true)
                                                    .build()))
                            .foreignKeys(List.of(fk))
                            .indexes(new ArrayList<>())
                            .build();

            SqlSchema schema = createEmptySchema();

            String result = migrationGenerator.generate(table, schema);

            assertThat(result).contains("category_id BIGINT");
        }

        @Test
        @DisplayName("Should generate index for foreign key")
        void shouldGenerateIndexForForeignKey() {
            SqlForeignKey fk =
                    SqlForeignKey.builder()
                            .columnName("category_id")
                            .referencedTable("categories")
                            .referencedColumn("id")
                            .build();

            SqlTable table =
                    SqlTable.builder()
                            .name("products")
                            .columns(
                                    List.of(
                                            SqlColumn.builder()
                                                    .name("id")
                                                    .javaType("Long")
                                                    .primaryKey(true)
                                                    .build()))
                            .foreignKeys(List.of(fk))
                            .indexes(new ArrayList<>())
                            .build();

            SqlSchema schema = createEmptySchema();

            String result = migrationGenerator.generate(table, schema);

            assertThat(result)
                    .contains("CREATE INDEX idx_products_category ON products(category_id);");
        }

        @Test
        @DisplayName("Should generate foreign key constraints")
        void shouldGenerateForeignKeyConstraints() {
            SqlForeignKey fk =
                    SqlForeignKey.builder()
                            .columnName("category_id")
                            .referencedTable("categories")
                            .referencedColumn("id")
                            .build();

            SqlTable table =
                    SqlTable.builder()
                            .name("products")
                            .columns(
                                    List.of(
                                            SqlColumn.builder()
                                                    .name("id")
                                                    .javaType("Long")
                                                    .primaryKey(true)
                                                    .build()))
                            .foreignKeys(List.of(fk))
                            .indexes(new ArrayList<>())
                            .build();

            SqlSchema schema = createEmptySchema();

            String result = migrationGenerator.generate(table, schema);

            assertThat(result)
                    .contains("-- Foreign Key Constraints")
                    .contains(
                            "ALTER TABLE products ADD CONSTRAINT fk_products_category FOREIGN KEY"
                                    + " (category_id) REFERENCES categories(id);");
        }

        @Test
        @DisplayName("Should handle multiple foreign keys")
        void shouldHandleMultipleForeignKeys() {
            List<SqlForeignKey> fks =
                    List.of(
                            SqlForeignKey.builder()
                                    .columnName("category_id")
                                    .referencedTable("categories")
                                    .referencedColumn("id")
                                    .build(),
                            SqlForeignKey.builder()
                                    .columnName("brand_id")
                                    .referencedTable("brands")
                                    .referencedColumn("id")
                                    .build());

            SqlTable table =
                    SqlTable.builder()
                            .name("products")
                            .columns(
                                    List.of(
                                            SqlColumn.builder()
                                                    .name("id")
                                                    .javaType("Long")
                                                    .primaryKey(true)
                                                    .build()))
                            .foreignKeys(fks)
                            .indexes(new ArrayList<>())
                            .build();

            SqlSchema schema = createEmptySchema();

            String result = migrationGenerator.generate(table, schema);

            assertThat(result)
                    .contains("category_id BIGINT")
                    .contains("brand_id BIGINT")
                    .contains("fk_products_category")
                    .contains("fk_products_brand");
        }

        @Test
        @DisplayName("Should preserve snake_case column names from SQL schema")
        void shouldPreserveSnakeCaseColumnNamesFromSqlSchema() {
            SqlColumn column =
                    SqlColumn.builder()
                            .name("event_date")
                            .sqlType("TIMESTAMP")
                            .javaType("LocalDateTime")
                            .nullable(true)
                            .build();

            SqlTable table = createTableWithColumn("events", column);
            SqlSchema schema = createEmptySchema();

            String result = migrationGenerator.generate(table, schema);

            assertThat(result).contains("event_date TIMESTAMP");
        }

        @Test
        @DisplayName("Should not include FK section when no foreign keys")
        void shouldNotIncludeFKSectionWhenNoForeignKeys() {
            SqlTable table = createSimpleTable("products");
            SqlSchema schema = createEmptySchema();

            String result = migrationGenerator.generate(table, schema);

            assertThat(result).doesNotContain("-- Foreign Key Constraints");
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

    private SqlSchema createEmptySchema() {
        return SqlSchema.builder().tables(new ArrayList<>()).functions(new ArrayList<>()).build();
    }
}
