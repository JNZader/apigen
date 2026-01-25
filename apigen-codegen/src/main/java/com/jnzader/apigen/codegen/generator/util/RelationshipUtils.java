package com.jnzader.apigen.codegen.generator.util;

import com.jnzader.apigen.codegen.generator.common.ManyToManyRelation;
import com.jnzader.apigen.codegen.model.SqlForeignKey;
import com.jnzader.apigen.codegen.model.SqlSchema;
import com.jnzader.apigen.codegen.model.SqlSchema.TableRelationship;
import com.jnzader.apigen.codegen.model.SqlTable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility methods for processing table relationships in SQL schemas.
 *
 * <p>This utility class provides methods to extract and group relationships between tables,
 * including:
 *
 * <ul>
 *   <li>Building relationship maps by source table
 *   <li>Finding inverse relationships (where a table is the target)
 *   <li>Finding many-to-many relationships through junction tables
 * </ul>
 */
public final class RelationshipUtils {

    private RelationshipUtils() {
        // Utility class
    }

    /**
     * Builds a map of relationships grouped by source table name.
     *
     * <p>This method iterates through all relationships in the schema and groups them by the source
     * table name. Useful for efficiently looking up all outgoing relationships for a given table.
     *
     * @param schema the SQL schema containing all tables and relationships
     * @return a map where keys are source table names and values are lists of relationships
     *     originating from that table
     */
    public static Map<String, List<TableRelationship>> buildRelationshipsByTable(SqlSchema schema) {
        Map<String, List<TableRelationship>> relationshipsByTable = new HashMap<>();

        for (TableRelationship rel : schema.getAllRelationships()) {
            relationshipsByTable
                    .computeIfAbsent(rel.getSourceTable().getName(), k -> new ArrayList<>())
                    .add(rel);
        }

        return relationshipsByTable;
    }

    /**
     * Gets relationships for a specific table from the relationships map.
     *
     * <p>This is a convenience method that safely retrieves relationships from a pre-built map,
     * returning an empty list if no relationships exist for the table.
     *
     * @param tableName the name of the table to look up
     * @param relationshipsByTable the pre-built relationships map from {@link
     *     #buildRelationshipsByTable(SqlSchema)}
     * @return the list of relationships for the table, or an empty list if none exist
     */
    public static List<TableRelationship> getRelationshipsForTable(
            String tableName, Map<String, List<TableRelationship>> relationshipsByTable) {
        return relationshipsByTable.getOrDefault(tableName, Collections.emptyList());
    }

    /**
     * Finds all inverse relationships where the given table is the target.
     *
     * <p>An inverse relationship represents the "other side" of a foreign key relationship. For
     * example, if table A has a foreign key to table B, then B has an inverse relationship to A.
     *
     * <p>This method filters out relationships where the source table is a junction table, as those
     * are handled separately as many-to-many relationships.
     *
     * @param table the target table to find inverse relationships for
     * @param schema the SQL schema containing all tables and relationships
     * @return a list of relationships where the given table is the target and the source is not a
     *     junction table
     */
    public static List<TableRelationship> findInverseRelationships(
            SqlTable table, SqlSchema schema) {
        return schema.getAllRelationships().stream()
                .filter(r -> r.getTargetTable().getName().equals(table.getName()))
                .filter(r -> !r.getSourceTable().isJunctionTable())
                .toList();
    }

    /**
     * Finds all many-to-many relationships for a table through junction tables.
     *
     * <p>A many-to-many relationship is identified by examining junction tables (tables with
     * exactly two foreign keys that form a composite primary key). For each junction table that
     * references the given table, this method creates a {@link ManyToManyRelation} describing the
     * relationship to the "other" table.
     *
     * <p>For example, if there's a junction table "user_roles" with foreign keys to "users" and
     * "roles", calling this method for the "users" table would return a ManyToManyRelation pointing
     * to "roles".
     *
     * @param table the entity table to find many-to-many relationships for
     * @param schema the SQL schema containing all tables, including junction tables
     * @return a list of many-to-many relations where the given table participates
     */
    public static List<ManyToManyRelation> findManyToManyRelations(
            SqlTable table, SqlSchema schema) {
        List<ManyToManyRelation> relations = new ArrayList<>();

        for (SqlTable junctionTable : schema.getJunctionTables()) {
            List<SqlForeignKey> fks = junctionTable.getForeignKeys();

            // Junction tables should have exactly 2 foreign keys
            if (fks.size() != 2) {
                continue;
            }

            SqlForeignKey fk1 = fks.get(0);
            SqlForeignKey fk2 = fks.get(1);

            SqlForeignKey thisFk = null;
            SqlForeignKey otherFk = null;

            // Determine which FK points to our table and which points to the other table
            if (fk1.getReferencedTable().equalsIgnoreCase(table.getName())) {
                thisFk = fk1;
                otherFk = fk2;
            } else if (fk2.getReferencedTable().equalsIgnoreCase(table.getName())) {
                thisFk = fk2;
                otherFk = fk1;
            }

            // If this table is part of the junction, create the relation
            if (thisFk != null && otherFk != null) {
                SqlTable otherTable = schema.getTableByName(otherFk.getReferencedTable());
                if (otherTable != null) {
                    relations.add(
                            new ManyToManyRelation(
                                    junctionTable.getName(),
                                    thisFk.getColumnName(),
                                    otherFk.getColumnName(),
                                    otherTable));
                }
            }
        }

        return relations;
    }
}
