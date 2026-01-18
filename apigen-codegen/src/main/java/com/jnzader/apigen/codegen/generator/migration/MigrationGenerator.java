package com.jnzader.apigen.codegen.generator.migration;

import com.jnzader.apigen.codegen.model.SqlColumn;
import com.jnzader.apigen.codegen.model.SqlForeignKey;
import com.jnzader.apigen.codegen.model.SqlSchema;
import com.jnzader.apigen.codegen.model.SqlTable;

import static com.jnzader.apigen.codegen.generator.util.CodeGenerationUtils.toSnakeCase;

/**
 * Generates Flyway SQL migration scripts from SQL table definitions.
 */
public class MigrationGenerator {

    private static final String CREATE_INDEX_PREFIX = "CREATE INDEX idx_";
    private static final String COMMA_NEWLINE_INDENT = ",\n    ";

    /**
     * Generates the SQL migration script for a table.
     */
    @SuppressWarnings("java:S1172")
    // S1172: El parametro 'schema' se mantiene para futura expansion (relaciones cross-table)
    public String generate(SqlTable table, SqlSchema schema) {
        StringBuilder sql = new StringBuilder();
        sql.append("-- Auto-generated migration for: ").append(table.getEntityName()).append("\n\n");

        // CREATE TABLE
        sql.append("CREATE TABLE ").append(table.getName()).append(" (\n");

        // Primary key
        sql.append("    id BIGINT PRIMARY KEY");

        // Business columns
        for (SqlColumn col : table.getBusinessColumns()) {
            sql.append(COMMA_NEWLINE_INDENT).append(toSnakeCase(col.getJavaFieldName())).append(" ")
                    .append(col.getSqlType());
            if (!col.isNullable()) sql.append(" NOT NULL");
            if (col.getDefaultValue() != null) sql.append(" DEFAULT ").append(col.getDefaultValue());
        }

        // FK columns
        for (SqlForeignKey fk : table.getForeignKeys()) {
            sql.append(COMMA_NEWLINE_INDENT).append(fk.getColumnName()).append(" BIGINT");
        }

        // Base columns
        sql.append(",\n    -- Base entity fields");
        sql.append("\n    estado BOOLEAN NOT NULL DEFAULT TRUE,");
        sql.append("\n    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,");
        sql.append("\n    fecha_actualizacion TIMESTAMP,");
        sql.append("\n    fecha_eliminacion TIMESTAMP,");
        sql.append("\n    creado_por VARCHAR(100),");
        sql.append("\n    modificado_por VARCHAR(100),");
        sql.append("\n    eliminado_por VARCHAR(100),");
        sql.append("\n    version BIGINT NOT NULL DEFAULT 0");

        sql.append("\n);\n\n");

        // Audit table
        sql.append("-- Audit table (Hibernate Envers)\n");
        sql.append("CREATE TABLE ").append(table.getName()).append("_aud (\n");
        sql.append("    id BIGINT NOT NULL,\n");
        sql.append("    rev INTEGER NOT NULL REFERENCES revision_info(id),\n");
        sql.append("    revtype SMALLINT,\n");

        for (SqlColumn col : table.getBusinessColumns()) {
            sql.append("    ").append(toSnakeCase(col.getJavaFieldName())).append(" ")
                    .append(col.getSqlType()).append(",\n");
        }

        sql.append("    estado BOOLEAN,\n");
        sql.append("    fecha_eliminacion TIMESTAMP,\n");
        sql.append("    modificado_por VARCHAR(100),\n");
        sql.append("    eliminado_por VARCHAR(100),\n");
        sql.append("    PRIMARY KEY (id, rev)\n");
        sql.append(");\n\n");

        // Indexes
        sql.append("-- Indexes\n");
        sql.append(CREATE_INDEX_PREFIX).append(table.getName()).append("_estado ON ")
                .append(table.getName()).append("(estado);\n");
        sql.append(CREATE_INDEX_PREFIX).append(table.getName()).append("_fecha_creacion ON ")
                .append(table.getName()).append("(fecha_creacion DESC);\n");

        for (SqlForeignKey fk : table.getForeignKeys()) {
            sql.append(CREATE_INDEX_PREFIX).append(table.getName()).append("_")
                    .append(toSnakeCase(fk.getJavaFieldName())).append(" ON ")
                    .append(table.getName()).append("(").append(fk.getColumnName()).append(");\n");
        }

        sql.append("\n");

        // Foreign key constraints
        if (!table.getForeignKeys().isEmpty()) {
            sql.append("-- Foreign Key Constraints\n");
            for (SqlForeignKey fk : table.getForeignKeys()) {
                sql.append("ALTER TABLE ").append(table.getName())
                        .append(" ADD CONSTRAINT fk_").append(table.getName()).append("_")
                        .append(toSnakeCase(fk.getJavaFieldName()))
                        .append(" FOREIGN KEY (").append(fk.getColumnName())
                        .append(") REFERENCES ").append(fk.getReferencedTable())
                        .append("(").append(fk.getReferencedColumn()).append(");\n");
            }
        }

        return sql.toString();
    }
}
