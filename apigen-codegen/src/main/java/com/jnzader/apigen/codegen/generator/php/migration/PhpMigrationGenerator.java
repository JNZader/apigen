package com.jnzader.apigen.codegen.generator.php.migration;

import com.jnzader.apigen.codegen.generator.php.PhpTypeMapper;
import com.jnzader.apigen.codegen.model.SqlColumn;
import com.jnzader.apigen.codegen.model.SqlSchema;
import com.jnzader.apigen.codegen.model.SqlTable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Generates Laravel migration files for database schema.
 *
 * <p>Generates:
 *
 * <ul>
 *   <li>Create table migrations
 *   <li>Foreign key constraints
 *   <li>Indexes
 * </ul>
 */
public class PhpMigrationGenerator {

    private final PhpTypeMapper typeMapper;
    private int migrationIndex = 0;

    public PhpMigrationGenerator() {
        this.typeMapper = new PhpTypeMapper();
    }

    /**
     * Generates a migration for a table.
     *
     * @param table the SQL table
     * @param relationships relationships involving this table
     * @return the migration file content
     */
    public String generate(SqlTable table, List<SqlSchema.TableRelationship> relationships) {
        StringBuilder sb = new StringBuilder();
        String tableName = table.getName();
        String className = "Create" + typeMapper.toPascalCase(tableName) + "Table";

        sb.append("<?php\n\n");
        sb.append("use Illuminate\\Database\\Migrations\\Migration;\n");
        sb.append("use Illuminate\\Database\\Schema\\Blueprint;\n");
        sb.append("use Illuminate\\Support\\Facades\\Schema;\n\n");

        sb.append("return new class extends Migration\n");
        sb.append("{\n");

        // Up method
        sb.append("    /**\n");
        sb.append("     * Run the migrations.\n");
        sb.append("     */\n");
        sb.append("    public function up(): void\n");
        sb.append("    {\n");
        sb.append("        Schema::create('")
                .append(tableName)
                .append("', function (Blueprint $table) {\n");

        // Primary key
        sb.append("            $table->id();\n");

        // Entity-specific columns
        for (SqlColumn column : table.getColumns()) {
            if (isBaseField(column.getName()) || column.isPrimaryKey()) {
                continue;
            }
            String migrationCol = typeMapper.getMigrationColumnType(column);
            sb.append("            ").append(migrationCol).append(";\n");
        }

        // Base audit fields
        sb.append("\n            // Audit fields\n");
        sb.append("            $table->boolean('activo')->default(true);\n");
        sb.append("            $table->timestamps();\n");
        sb.append("            $table->string('created_by')->nullable();\n");
        sb.append("            $table->string('updated_by')->nullable();\n");
        sb.append("            $table->timestamp('deleted_at')->nullable();\n");
        sb.append("            $table->string('deleted_by')->nullable();\n");

        // Foreign keys
        for (SqlSchema.TableRelationship rel : relationships) {
            String fkColumn = typeMapper.toSnakeCase(rel.getForeignKey().getColumnName());
            String targetTable = rel.getTargetTable().getName();

            sb.append("\n            // Foreign key to ").append(targetTable).append("\n");
            sb.append("            $table->unsignedBigInteger('")
                    .append(fkColumn)
                    .append("')->nullable();\n");
            sb.append("            $table->foreign('").append(fkColumn).append("')\n");
            sb.append("                ->references('id')\n");
            sb.append("                ->on('").append(targetTable).append("')\n");
            sb.append("                ->onDelete('set null');\n");
        }

        // Add indexes for commonly searched fields
        sb.append("\n            // Indexes\n");
        sb.append("            $table->index('activo');\n");
        sb.append("            $table->index('created_at');\n");

        sb.append("        });\n");
        sb.append("    }\n\n");

        // Down method
        sb.append("    /**\n");
        sb.append("     * Reverse the migrations.\n");
        sb.append("     */\n");
        sb.append("    public function down(): void\n");
        sb.append("    {\n");
        sb.append("        Schema::dropIfExists('").append(tableName).append("');\n");
        sb.append("    }\n");

        sb.append("};\n");

        return sb.toString();
    }

    /**
     * Generates the migration filename with timestamp.
     *
     * @param tableName the table name
     * @return the migration filename
     */
    public String getMigrationFileName(String tableName) {
        LocalDateTime now = LocalDateTime.now().plusSeconds(migrationIndex++);
        String timestamp = now.format(DateTimeFormatter.ofPattern("yyyy_MM_dd_HHmmss"));
        return timestamp + "_create_" + tableName + "_table.php";
    }

    private boolean isBaseField(String columnName) {
        String lower = columnName.toLowerCase();
        return lower.equals("id")
                || lower.equals("activo")
                || lower.equals("created_at")
                || lower.equals("updated_at")
                || lower.equals("created_by")
                || lower.equals("updated_by")
                || lower.equals("deleted_at")
                || lower.equals("deleted_by");
    }
}
