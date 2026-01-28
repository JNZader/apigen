package com.jnzader.apigen.codegen.generator.python.model;

import com.jnzader.apigen.codegen.generator.python.PythonTypeMapper;
import com.jnzader.apigen.codegen.model.SqlColumn;
import com.jnzader.apigen.codegen.model.SqlSchema;
import com.jnzader.apigen.codegen.model.SqlTable;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Generates SQLAlchemy model classes for Python/FastAPI projects.
 *
 * <p>Generates:
 *
 * <ul>
 *   <li>SQLAlchemy declarative models
 *   <li>Relationship definitions
 *   <li>Column constraints and indexes
 * </ul>
 */
public class PythonModelGenerator {

    private final PythonTypeMapper typeMapper;

    public PythonModelGenerator() {
        this.typeMapper = new PythonTypeMapper();
    }

    /**
     * Generates the base model class that all models inherit from.
     *
     * @return the base.py content
     */
    public String generateBase() {
        return """
        from datetime import datetime
        from sqlalchemy import Column, BigInteger, Boolean, DateTime, String
        from sqlalchemy.orm import declarative_base, declared_attr

        Base = declarative_base()


        class BaseModel(Base):
            \"""Base model with common fields for all entities.\"""

            __abstract__ = True

            id = Column(BigInteger, primary_key=True, index=True, autoincrement=True)
            activo = Column(Boolean, default=True, nullable=False)
            created_at = Column(DateTime, default=datetime.utcnow, nullable=False)
            updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)
            created_by = Column(String(255))
            updated_by = Column(String(255))
            deleted_at = Column(DateTime)
            deleted_by = Column(String(255))

            @declared_attr
            def __tablename__(cls) -> str:
                \"""Generate table name from class name.\"""
                # Convert CamelCase to snake_case
                name = cls.__name__
                return ''.join(['_' + c.lower() if c.isupper() else c for c in name]).lstrip('_') + 's'
        """;
    }

    /**
     * Generates a SQLAlchemy model for a table.
     *
     * @param table the SQL table
     * @param relationships relationships involving this table
     * @param inverseRelations inverse relationships (where this table is target)
     * @return the model.py content
     */
    public String generate(
            SqlTable table,
            List<SqlSchema.TableRelationship> relationships,
            List<SqlSchema.TableRelationship> inverseRelations) {

        StringBuilder sb = new StringBuilder();
        String className = table.getEntityName();
        String tableName = table.getName();

        // Collect imports
        Set<String> sqlalchemyImports = new HashSet<>();
        Set<String> typeImports = new HashSet<>();

        sqlalchemyImports.add("Column");
        sqlalchemyImports.add("ForeignKey");

        // Analyze columns for imports
        for (SqlColumn column : table.getColumns()) {
            if (column.isPrimaryKey()) continue; // Handled by BaseModel

            String sqlalchemyType = typeMapper.getSqlAlchemyType(column);
            String baseType = sqlalchemyType.split("\\(")[0];
            sqlalchemyImports.add(baseType);

            String typeImport = typeMapper.getTypeImport(column.getJavaType());
            if (typeImport != null) {
                typeImports.add(typeImport);
            }
        }

        // Check for relationships
        boolean hasRelationships = !relationships.isEmpty() || !inverseRelations.isEmpty();
        if (hasRelationships) {
            sqlalchemyImports.add("relationship");
        }

        // Write imports
        sb.append("from sqlalchemy import ");
        sb.append(String.join(", ", sqlalchemyImports.stream().sorted().toList()));
        sb.append("\n");

        if (hasRelationships) {
            sb.append("from sqlalchemy.orm import relationship\n");
        }

        for (String typeImport : typeImports.stream().sorted().toList()) {
            sb.append(typeImport).append("\n");
        }

        sb.append("from app.models.base import BaseModel\n");
        sb.append("\n\n");

        // Class definition
        sb.append("class ").append(className).append("(BaseModel):\n");
        sb.append("    \"\"\"").append(className).append(" model.\"\"\"\n\n");

        // Table name (explicit)
        sb.append("    __tablename__ = \"").append(tableName).append("\"\n\n");

        // Columns (excluding base fields)
        for (SqlColumn column : table.getColumns()) {
            if (isBaseField(column.getName())) {
                continue;
            }

            generateColumn(sb, column);
        }

        // Relationships (ManyToOne - this entity has FK)
        for (SqlSchema.TableRelationship rel : relationships) {
            generateManyToOneRelationship(sb, rel);
        }

        // Inverse relationships (OneToMany - other entities have FK to this)
        for (SqlSchema.TableRelationship rel : inverseRelations) {
            generateOneToManyRelationship(sb, rel);
        }

        // Repr method
        sb.append("\n    def __repr__(self) -> str:\n");
        sb.append("        return f\"<").append(className).append("(id={self.id})>\"\n");

        return sb.toString();
    }

    private void generateColumn(StringBuilder sb, SqlColumn column) {
        String fieldName = typeMapper.toSnakeCase(column.getName());
        fieldName = typeMapper.safePythonFieldName(fieldName);

        String sqlalchemyType = typeMapper.getSqlAlchemyType(column);

        sb.append("    ").append(fieldName).append(" = Column(");
        sb.append(sqlalchemyType);

        // Add constraints
        if (column.isUnique()) {
            sb.append(", unique=True");
        }
        if (!column.isNullable()) {
            sb.append(", nullable=False");
        }
        if (column.getName().toLowerCase(Locale.ROOT).contains("email")
                || column.getName().toLowerCase(Locale.ROOT).endsWith("_id")) {
            sb.append(", index=True");
        }

        sb.append(")\n");
    }

    private void generateManyToOneRelationship(StringBuilder sb, SqlSchema.TableRelationship rel) {
        String targetEntity = rel.getTargetTable().getEntityName();
        String fieldName = typeMapper.toSnakeCase(targetEntity);
        String fkColumn = typeMapper.toSnakeCase(rel.getForeignKey().getColumnName());

        // Foreign key column (if not already generated)
        if (!fkColumn.equals("id")) {
            sb.append("    ")
                    .append(fkColumn)
                    .append(" = Column(BigInteger, ForeignKey(\"")
                    .append(rel.getTargetTable().getName())
                    .append(".id\"))\n");
        }

        // Relationship
        sb.append("    ")
                .append(fieldName)
                .append(" = relationship(\"")
                .append(targetEntity)
                .append("\", back_populates=\"")
                .append(typeMapper.toSnakeCase(rel.getSourceTable().getEntityName()))
                .append("s\")\n");
    }

    private void generateOneToManyRelationship(StringBuilder sb, SqlSchema.TableRelationship rel) {
        String sourceEntity = rel.getSourceTable().getEntityName();
        String fieldName = typeMapper.toSnakeCase(sourceEntity) + "s";

        sb.append("    ")
                .append(fieldName)
                .append(" = relationship(\"")
                .append(sourceEntity)
                .append("\", back_populates=\"")
                .append(typeMapper.toSnakeCase(rel.getTargetTable().getEntityName()))
                .append("\")\n");
    }

    private boolean isBaseField(String columnName) {
        String lower = columnName.toLowerCase(Locale.ROOT);
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
