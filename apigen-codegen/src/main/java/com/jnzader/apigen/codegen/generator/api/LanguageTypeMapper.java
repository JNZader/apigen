package com.jnzader.apigen.codegen.generator.api;

import com.jnzader.apigen.codegen.model.SqlColumn;
import java.util.Set;

/**
 * Interface for mapping SQL types to language-specific types.
 *
 * <p>Each language generator should provide an implementation that maps SQL column definitions to
 * the appropriate types in the target language.
 */
public interface LanguageTypeMapper {

    /**
     * Maps an SQL column to the appropriate type in the target language.
     *
     * @param column the SQL column to map
     * @return the type name in the target language (e.g., "String", "str", "string")
     */
    String mapColumnType(SqlColumn column);

    /**
     * Returns the set of imports/includes required for a column's type.
     *
     * @param column the SQL column
     * @return set of import statements or includes (may be empty)
     */
    Set<String> getRequiredImports(SqlColumn column);

    /**
     * Returns the default value expression for a column type.
     *
     * @param column the SQL column
     * @return the default value expression in the target language, or null if no default
     */
    String getDefaultValue(SqlColumn column);

    /**
     * Returns the type for primary key fields.
     *
     * @return the primary key type (e.g., "Long", "int", "uuid")
     */
    String getPrimaryKeyType();

    /**
     * Returns the imports required for the primary key type.
     *
     * @return set of import statements (may be empty)
     */
    Set<String> getPrimaryKeyImports();

    /**
     * Returns the list/collection type for the target language.
     *
     * @param elementType the type of elements in the list
     * @return the list type (e.g., "List<String>", "list[str]", "[]string")
     */
    String getListType(String elementType);

    /**
     * Returns the nullable wrapper type for a primitive type.
     *
     * @param type the primitive type
     * @return the nullable type (e.g., "Integer" for "int" in Java)
     */
    String getNullableType(String type);
}
