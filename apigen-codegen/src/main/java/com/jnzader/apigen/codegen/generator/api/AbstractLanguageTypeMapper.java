package com.jnzader.apigen.codegen.generator.api;

import com.jnzader.apigen.codegen.model.SqlColumn;
import java.util.Set;

/**
 * Abstract base class for language-specific type mappers.
 *
 * <p>Provides default implementations for common methods and defines template methods that
 * subclasses must implement for language-specific behavior.
 *
 * <p>This class reduces duplication across the 9 language type mappers by centralizing common
 * patterns while allowing each language to customize:
 *
 * <ul>
 *   <li>Type mappings (Java to target language)
 *   <li>Default values
 *   <li>List/collection type format
 *   <li>Nullable type handling
 *   <li>Import statements
 * </ul>
 */
public abstract class AbstractLanguageTypeMapper implements LanguageTypeMapper {

    /**
     * Maps an SQL column to the target language type.
     *
     * <p>Default implementation extracts the Java type from the column and delegates to {@link
     * #mapJavaType(String)}. Subclasses can override this method if they need access to additional
     * column metadata (e.g., nullability for Go pointer types).
     *
     * @param column the SQL column to map
     * @return the type name in the target language
     */
    @Override
    public String mapColumnType(SqlColumn column) {
        return mapJavaType(column.getJavaType());
    }

    /**
     * Converts a Java type name to the equivalent type in the target language.
     *
     * <p>Each language implementation must define how Java types like "String", "Integer", "Long",
     * "LocalDateTime", etc. map to their language equivalents.
     *
     * @param javaType the Java type name (e.g., "String", "Long", "LocalDateTime")
     * @return the equivalent type in the target language
     */
    public abstract String mapJavaType(String javaType);

    /**
     * Returns the set of imports required for a column's type.
     *
     * <p>Each language has different import syntax and requirements. The default implementation
     * returns an empty set; subclasses should override for types that need imports.
     *
     * @param column the SQL column
     * @return set of import statements (may be empty)
     */
    @Override
    public Set<String> getRequiredImports(SqlColumn column) {
        return Set.of();
    }

    /**
     * Returns the type for primary key fields.
     *
     * <p>Default implementation returns "Long" (suitable for Java/Kotlin). Subclasses should
     * override for languages with different primary key conventions.
     *
     * @return the primary key type
     */
    @Override
    public String getPrimaryKeyType() {
        return "Long";
    }

    /**
     * Returns the imports required for the primary key type.
     *
     * <p>Default implementation returns an empty set, as most languages don't require imports for
     * their primary key types.
     *
     * @return set of import statements (empty by default)
     */
    @Override
    public Set<String> getPrimaryKeyImports() {
        return Set.of();
    }

    /**
     * Returns the list/collection type for the target language.
     *
     * <p>Default implementation uses {@link #getListTypeFormat()} as a format string. Subclasses
     * can override this method for more complex list type construction.
     *
     * @param elementType the type of elements in the list
     * @return the list type (e.g., "List&lt;String&gt;", "list[str]", "[]string")
     */
    @Override
    public String getListType(String elementType) {
        return String.format(getListTypeFormat(), elementType);
    }

    /**
     * Returns the format string for list types in the target language.
     *
     * <p>The format string should contain exactly one "%s" placeholder where the element type will
     * be inserted.
     *
     * <p>Examples:
     *
     * <ul>
     *   <li>Java/Kotlin: "List&lt;%s&gt;"
     *   <li>Python: "list[%s]"
     *   <li>Go: "[]%s"
     *   <li>TypeScript: "%s[]"
     * </ul>
     *
     * @return the format string for list types
     */
    protected abstract String getListTypeFormat();
}
