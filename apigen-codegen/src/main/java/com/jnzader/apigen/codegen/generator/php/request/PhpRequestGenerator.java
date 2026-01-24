package com.jnzader.apigen.codegen.generator.php.request;

import com.jnzader.apigen.codegen.generator.php.PhpTypeMapper;
import com.jnzader.apigen.codegen.model.SqlColumn;
import com.jnzader.apigen.codegen.model.SqlSchema;
import com.jnzader.apigen.codegen.model.SqlTable;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates Laravel Form Request classes for validation.
 *
 * <p>Generates:
 *
 * <ul>
 *   <li>CreateRequest for POST/create validation
 *   <li>UpdateRequest for PUT/PATCH/update validation
 * </ul>
 */
public class PhpRequestGenerator {

    private final PhpTypeMapper typeMapper;

    public PhpRequestGenerator() {
        this.typeMapper = new PhpTypeMapper();
    }

    /**
     * Generates the Create Form Request.
     *
     * @param table the SQL table
     * @param relationships relationships involving this table
     * @return the CreateRequest.php content
     */
    public String generateCreateRequest(
            SqlTable table, List<SqlSchema.TableRelationship> relationships) {
        return generateRequest(table, relationships, "Create", false);
    }

    /**
     * Generates the Update Form Request.
     *
     * @param table the SQL table
     * @param relationships relationships involving this table
     * @return the UpdateRequest.php content
     */
    public String generateUpdateRequest(
            SqlTable table, List<SqlSchema.TableRelationship> relationships) {
        return generateRequest(table, relationships, "Update", true);
    }

    private String generateRequest(
            SqlTable table,
            List<SqlSchema.TableRelationship> relationships,
            String prefix,
            boolean isUpdate) {

        StringBuilder sb = new StringBuilder();
        String className = table.getEntityName();
        String requestName = prefix + className + "Request";

        sb.append("<?php\n\n");
        sb.append("namespace App\\Http\\Requests;\n\n");
        sb.append("use Illuminate\\Foundation\\Http\\FormRequest;\n");
        sb.append("use Illuminate\\Validation\\Rule;\n\n");

        sb.append("class ").append(requestName).append(" extends FormRequest\n");
        sb.append("{\n");

        // Authorize
        sb.append("    /**\n");
        sb.append("     * Determine if the user is authorized to make this request.\n");
        sb.append("     */\n");
        sb.append("    public function authorize(): bool\n");
        sb.append("    {\n");
        sb.append("        return true; // Modify as needed for authorization\n");
        sb.append("    }\n\n");

        // Rules
        sb.append("    /**\n");
        sb.append("     * Get the validation rules that apply to the request.\n");
        sb.append("     */\n");
        sb.append("    public function rules(): array\n");
        sb.append("    {\n");
        sb.append("        return [\n");

        // Entity-specific fields
        for (SqlColumn column : table.getColumns()) {
            if (column.isPrimaryKey() || isBaseField(column.getName())) {
                continue;
            }

            String snakeName = typeMapper.toSnakeCase(column.getName());
            List<String> rules = buildValidationRules(column, table.getName(), isUpdate);

            sb.append("            '")
                    .append(snakeName)
                    .append("' => [")
                    .append(String.join(", ", rules))
                    .append("],\n");
        }

        // FK fields from relationships
        for (SqlSchema.TableRelationship rel : relationships) {
            String fkColumn = typeMapper.toSnakeCase(rel.getForeignKey().getColumnName());
            String targetTable = rel.getTargetTable().getName();

            List<String> rules = new ArrayList<>();
            rules.add(isUpdate ? "'sometimes'" : "'nullable'");
            rules.add("'integer'");
            rules.add("'exists:" + targetTable + ",id'");

            sb.append("            '")
                    .append(fkColumn)
                    .append("' => [")
                    .append(String.join(", ", rules))
                    .append("],\n");
        }

        sb.append("        ];\n");
        sb.append("    }\n\n");

        // Messages
        sb.append("    /**\n");
        sb.append("     * Get custom messages for validator errors.\n");
        sb.append("     */\n");
        sb.append("    public function messages(): array\n");
        sb.append("    {\n");
        sb.append("        return [\n");
        sb.append("            'required' => 'The :attribute field is required.',\n");
        sb.append("            'string' => 'The :attribute must be a string.',\n");
        sb.append(
                "            'max' => 'The :attribute may not be greater than :max"
                        + " characters.',\n");
        sb.append("            'email' => 'The :attribute must be a valid email address.',\n");
        sb.append("            'unique' => 'The :attribute has already been taken.',\n");
        sb.append("            'exists' => 'The selected :attribute is invalid.',\n");
        sb.append("            'integer' => 'The :attribute must be an integer.',\n");
        sb.append("            'boolean' => 'The :attribute must be true or false.',\n");
        sb.append("            'numeric' => 'The :attribute must be a number.',\n");
        sb.append("            'date' => 'The :attribute must be a valid date.',\n");
        sb.append("        ];\n");
        sb.append("    }\n");

        sb.append("}\n");

        return sb.toString();
    }

    private List<String> buildValidationRules(
            SqlColumn column, String tableName, boolean isUpdate) {
        List<String> rules = new ArrayList<>();
        String snakeName = typeMapper.toSnakeCase(column.getName());

        // Required or nullable/sometimes
        if (isUpdate) {
            rules.add("'sometimes'");
        } else if (!column.isNullable()) {
            rules.add("'required'");
        } else {
            rules.add("'nullable'");
        }

        // Type-specific rules
        String javaType = column.getJavaType();
        switch (javaType) {
            case "String" -> {
                rules.add("'string'");
                int maxLength =
                        column.getLength() != null && column.getLength() > 0
                                ? column.getLength()
                                : 255;
                rules.add("'max:" + maxLength + "'");

                // Email validation for email fields
                if (column.getName().toLowerCase().contains("email")) {
                    rules.add("'email'");
                }

                // URL validation for url fields
                if (column.getName().toLowerCase().contains("url")
                        || column.getName().toLowerCase().contains("website")) {
                    rules.add("'url'");
                }
            }
            case "Integer", "int", "Long", "long" -> rules.add("'integer'");
            case "Double", "double", "Float", "float", "BigDecimal" -> rules.add("'numeric'");
            case "Boolean", "boolean" -> rules.add("'boolean'");
            case "LocalDate" -> rules.add("'date'");
            case "LocalDateTime", "Instant", "ZonedDateTime" -> rules.add("'date'");
            case "UUID" -> rules.add("'uuid'");
        }

        // Unique constraint
        if (column.isUnique()) {
            if (isUpdate) {
                rules.add(
                        "Rule::unique('"
                                + tableName
                                + "', '"
                                + snakeName
                                + "')"
                                + "->ignore($this->route('id'))");
            } else {
                rules.add("'unique:" + tableName + "," + snakeName + "'");
            }
        }

        return rules;
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
