package com.jnzader.apigen.codegen.generator.php.model;

import com.jnzader.apigen.codegen.generator.php.PhpTypeMapper;
import com.jnzader.apigen.codegen.model.SqlColumn;
import com.jnzader.apigen.codegen.model.SqlSchema;
import com.jnzader.apigen.codegen.model.SqlTable;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates Eloquent model classes for Laravel projects.
 *
 * <p>Generates:
 *
 * <ul>
 *   <li>Eloquent models with relationships
 *   <li>Fillable and guarded properties
 *   <li>Casts for type conversion
 *   <li>Soft deletes support
 * </ul>
 */
public class PhpModelGenerator {

    private final PhpTypeMapper typeMapper;

    public PhpModelGenerator() {
        this.typeMapper = new PhpTypeMapper();
    }

    /**
     * Generates the base model trait for common functionality.
     *
     * @return the BaseModelTrait.php content
     */
    public String generateBaseTrait() {
        return """
        <?php

        namespace App\\Traits;

        use Illuminate\\Database\\Eloquent\\Builder;

        trait BaseModelTrait
        {
            /**
             * Scope to filter only active records.
             */
            public function scopeActive(Builder $query): Builder
            {
                return $query->where('activo', true);
            }

            /**
             * Scope to filter only inactive records.
             */
            public function scopeInactive(Builder $query): Builder
            {
                return $query->where('activo', false);
            }

            /**
             * Soft delete the model by setting activo to false.
             */
            public function softDelete(): bool
            {
                $this->activo = false;
                $this->deleted_at = now();
                $this->deleted_by = auth()->user()?->name ?? 'system';
                return $this->save();
            }

            /**
             * Restore a soft-deleted model.
             */
            public function restore(): bool
            {
                $this->activo = true;
                $this->deleted_at = null;
                $this->deleted_by = null;
                return $this->save();
            }

            /**
             * Boot the trait.
             */
            protected static function bootBaseModelTrait(): void
            {
                static::creating(function ($model) {
                    $model->created_by = auth()->user()?->name ?? 'system';
                    $model->activo = $model->activo ?? true;
                });

                static::updating(function ($model) {
                    $model->updated_by = auth()->user()?->name ?? 'system';
                });
            }
        }
        """;
    }

    /**
     * Generates an Eloquent model for a table.
     *
     * @param table the SQL table
     * @param relationships relationships involving this table
     * @param inverseRelations inverse relationships (where this table is target)
     * @return the Model.php content
     */
    public String generate(
            SqlTable table,
            List<SqlSchema.TableRelationship> relationships,
            List<SqlSchema.TableRelationship> inverseRelations) {

        StringBuilder sb = new StringBuilder();
        String className = table.getEntityName();
        String tableName = table.getName();

        sb.append("<?php\n\n");
        sb.append("namespace App\\Models;\n\n");
        sb.append("use App\\Traits\\BaseModelTrait;\n");
        sb.append("use Illuminate\\Database\\Eloquent\\Factories\\HasFactory;\n");
        sb.append("use Illuminate\\Database\\Eloquent\\Model;\n");
        sb.append("use Illuminate\\Database\\Eloquent\\Relations\\BelongsTo;\n");
        sb.append("use Illuminate\\Database\\Eloquent\\Relations\\HasMany;\n");
        sb.append("\n");

        // Class definition
        sb.append("class ").append(className).append(" extends Model\n");
        sb.append("{\n");
        sb.append("    use HasFactory, BaseModelTrait;\n\n");

        // Table name
        sb.append("    /**\n");
        sb.append("     * The table associated with the model.\n");
        sb.append("     */\n");
        sb.append("    protected $table = '").append(tableName).append("';\n\n");

        // Fillable fields
        List<String> fillable = new ArrayList<>();
        List<String> casts = new ArrayList<>();

        for (SqlColumn column : table.getColumns()) {
            if (isBaseField(column.getName()) || column.isPrimaryKey()) {
                continue;
            }

            String snakeName = typeMapper.toSnakeCase(column.getName());
            fillable.add(snakeName);

            String cast = typeMapper.getEloquentCast(column);
            if (cast != null) {
                casts.add("'" + snakeName + "' => '" + cast + "'");
            }
        }

        // Add base audit fields to casts
        casts.add("'activo' => 'boolean'");
        casts.add("'created_at' => 'datetime'");
        casts.add("'updated_at' => 'datetime'");
        casts.add("'deleted_at' => 'datetime'");

        sb.append("    /**\n");
        sb.append("     * The attributes that are mass assignable.\n");
        sb.append("     */\n");
        sb.append("    protected $fillable = [\n");
        for (String field : fillable) {
            sb.append("        '").append(field).append("',\n");
        }
        sb.append("    ];\n\n");

        // Casts
        if (!casts.isEmpty()) {
            sb.append("    /**\n");
            sb.append("     * The attributes that should be cast.\n");
            sb.append("     */\n");
            sb.append("    protected $casts = [\n");
            for (String cast : casts) {
                sb.append("        ").append(cast).append(",\n");
            }
            sb.append("    ];\n\n");
        }

        // Hidden fields
        sb.append("    /**\n");
        sb.append("     * The attributes that should be hidden for serialization.\n");
        sb.append("     */\n");
        sb.append("    protected $hidden = [\n");
        sb.append("        'deleted_at',\n");
        sb.append("        'deleted_by',\n");
        sb.append("    ];\n");

        // Relationships (ManyToOne/BelongsTo)
        for (SqlSchema.TableRelationship rel : relationships) {
            generateBelongsTo(sb, rel);
        }

        // Inverse relationships (OneToMany/HasMany)
        for (SqlSchema.TableRelationship rel : inverseRelations) {
            generateHasMany(sb, rel);
        }

        sb.append("}\n");

        return sb.toString();
    }

    private void generateBelongsTo(StringBuilder sb, SqlSchema.TableRelationship rel) {
        String targetEntity = rel.getTargetTable().getEntityName();
        String methodName = typeMapper.toCamelCase(targetEntity);
        String foreignKey = typeMapper.toSnakeCase(rel.getForeignKey().getColumnName());

        sb.append("\n    /**\n");
        sb.append("     * Get the ").append(methodName).append(" that owns this record.\n");
        sb.append("     */\n");
        sb.append("    public function ").append(methodName).append("(): BelongsTo\n");
        sb.append("    {\n");
        sb.append("        return $this->belongsTo(")
                .append(targetEntity)
                .append("::class, '")
                .append(foreignKey)
                .append("');\n");
        sb.append("    }\n");
    }

    private void generateHasMany(StringBuilder sb, SqlSchema.TableRelationship rel) {
        String sourceEntity = rel.getSourceTable().getEntityName();
        String methodName = typeMapper.toCamelCase(typeMapper.pluralize(sourceEntity));
        String foreignKey = typeMapper.toSnakeCase(rel.getForeignKey().getColumnName());

        sb.append("\n    /**\n");
        sb.append("     * Get the ").append(methodName).append(" for this record.\n");
        sb.append("     */\n");
        sb.append("    public function ").append(methodName).append("(): HasMany\n");
        sb.append("    {\n");
        sb.append("        return $this->hasMany(")
                .append(sourceEntity)
                .append("::class, '")
                .append(foreignKey)
                .append("');\n");
        sb.append("    }\n");
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
