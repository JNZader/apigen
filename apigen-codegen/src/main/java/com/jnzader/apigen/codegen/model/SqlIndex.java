package com.jnzader.apigen.codegen.model;

import java.util.List;
import lombok.Builder;
import lombok.Data;

/** Represents an index parsed from SQL. */
@Data
@Builder
public class SqlIndex {
    private String name;
    private String tableName;
    private List<String> columns;
    private boolean unique;
    private IndexType type;
    private String condition; // For partial indexes

    public enum IndexType {
        BTREE, // Default
        HASH,
        GIN, // PostgreSQL full-text
        GIST, // PostgreSQL geometric
        BRIN // PostgreSQL block range
    }

    /** Generates JPA @Index annotation. */
    public String toJpaAnnotation() {
        StringBuilder sb = new StringBuilder();
        sb.append("@Index(name = \"").append(name).append("\"");
        sb.append(", columnList = \"").append(String.join(", ", columns)).append("\"");
        if (unique) {
            sb.append(", unique = true");
        }
        sb.append(")");
        return sb.toString();
    }
}
