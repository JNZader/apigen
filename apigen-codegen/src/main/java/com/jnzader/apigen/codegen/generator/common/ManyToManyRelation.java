package com.jnzader.apigen.codegen.generator.common;

import com.jnzader.apigen.codegen.model.SqlTable;

/**
 * Represents a many-to-many relationship discovered through a junction table.
 *
 * <p>This record is used by generators to properly create the @ManyToMany JPA annotation or
 * equivalent constructs in other languages/frameworks.
 *
 * @param junctionTable the name of the junction/join table
 * @param sourceColumn the column in the junction table that references the source entity
 * @param targetColumn the column in the junction table that references the target entity
 * @param targetTable the target table of the relationship
 */
public record ManyToManyRelation(
        String junctionTable, String sourceColumn, String targetColumn, SqlTable targetTable) {}
