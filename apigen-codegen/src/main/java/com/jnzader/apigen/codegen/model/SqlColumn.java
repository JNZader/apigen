package com.jnzader.apigen.codegen.model;

import java.util.List;
import java.util.Locale;
import lombok.Builder;
import lombok.Data;

/** Represents a column parsed from SQL CREATE TABLE statement. */
@Data
@Builder
public class SqlColumn {
    private String name;
    private String sqlType;
    private String javaType;
    private boolean nullable;
    private boolean primaryKey;
    private boolean unique;
    private boolean autoIncrement;
    private String defaultValue;
    private Integer length;
    private Integer precision;
    private Integer scale;
    private String checkConstraint;
    private List<String> enumValues;
    private String comment;

    /** Converts snake_case column name to camelCase Java field name. */
    public String getJavaFieldName() {
        if (name == null) return null;
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = false;
        for (char c : name.toLowerCase(Locale.ROOT).toCharArray()) {
            if (c == '_') {
                capitalizeNext = true;
            } else {
                result.append(capitalizeNext ? Character.toUpperCase(c) : c);
                capitalizeNext = false;
            }
        }
        return result.toString();
    }

    /** Returns the validation annotations based on column constraints. */
    public String getValidationAnnotations() {
        StringBuilder annotations = new StringBuilder();

        if (!nullable && !primaryKey) {
            if ("String".equals(javaType)) {
                annotations.append("@NotBlank ");
            } else {
                annotations.append("@NotNull ");
            }
        }

        if ("String".equals(javaType) && length != null && length > 0) {
            annotations.append("@Size(max = ").append(length).append(") ");
        }

        if (unique && !primaryKey) {
            annotations.append("// @Unique ");
        }

        return annotations.toString().trim();
    }
}
