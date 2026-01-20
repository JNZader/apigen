package com.jnzader.apigen.codegen.parser;

import com.jnzader.apigen.codegen.model.SqlColumn;
import com.jnzader.apigen.codegen.model.SqlForeignKey;
import com.jnzader.apigen.codegen.model.SqlFunction;
import com.jnzader.apigen.codegen.model.SqlIndex;
import com.jnzader.apigen.codegen.model.SqlSchema;
import com.jnzader.apigen.codegen.model.SqlTable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.Statements;
import net.sf.jsqlparser.statement.alter.Alter;
import net.sf.jsqlparser.statement.alter.AlterExpression;
import net.sf.jsqlparser.statement.create.function.CreateFunction;
import net.sf.jsqlparser.statement.create.index.CreateIndex;
import net.sf.jsqlparser.statement.create.table.ColDataType;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.create.table.ForeignKeyIndex;
import net.sf.jsqlparser.statement.create.table.Index;

/**
 * Parses SQL schema files and extracts table definitions, relationships, indexes, constraints,
 * functions, and stored procedures.
 */
@SuppressWarnings({"java:S5843", "java:S5852", "java:S1874", "java:S3776", "java:S1858"})
// S5843: CREATE_FUNCTION_PATTERN regex complexity is necessary for parsing varied SQL syntax
// S5852: Regex DoS is MITIGATED - input is developer-controlled SQL files, not user input
// S1874: getStatements() deprecated but no alternative in jsqlparser yet
// S3776: parseColumnDefinition complexity is inherent to SQL column parsing requirements
// S1858: toString() IS required on jsqlparser ReferenceOption enum objects
public class SqlSchemaParser {

    // String constants for S1192 (duplicate string literals)
    private static final String INDEX_TYPE_PRIMARY_KEY = "PRIMARY KEY";
    private static final String INDEX_TYPE_UNIQUE = "UNIQUE";
    private static final String JAVA_TYPE_STRING = "String";

    private static final Pattern REFERENCES_PATTERN =
            Pattern.compile(
                    "REFERENCES\\s+([\\w\\.]+)\\s*\\(\\s*(\\w+)\\s*\\)", Pattern.CASE_INSENSITIVE);

    private static final Pattern ON_DELETE_PATTERN =
            Pattern.compile(
                    "ON\\s+DELETE\\s+(CASCADE|SET\\s+NULL|SET\\s+DEFAULT|RESTRICT|NO\\s+ACTION)",
                    Pattern.CASE_INSENSITIVE);

    private static final Pattern ON_UPDATE_PATTERN =
            Pattern.compile(
                    "ON\\s+UPDATE\\s+(CASCADE|SET\\s+NULL|SET\\s+DEFAULT|RESTRICT|NO\\s+ACTION)",
                    Pattern.CASE_INSENSITIVE);

    private static final Pattern CREATE_FUNCTION_PATTERN =
            Pattern.compile(
                    "CREATE\\s+(OR\\s+REPLACE\\s+)?(FUNCTION|PROCEDURE)\\s+([\\w\\.]+)\\s*\\(([^)]*)\\)"
                        + "\\s*(RETURNS\\s+([\\w\\s\\[\\]]+))?\\s*(LANGUAGE\\s+(\\w+))?"
                        + "\\s*AS\\s*\\$\\$(.+?)\\$\\$",
                    Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    /** Parses a SQL file and returns the complete schema. */
    public SqlSchema parseFile(Path sqlFile) throws IOException {
        String content = Files.readString(sqlFile);
        return parse(content, sqlFile.getFileName().toString());
    }

    /** Parses SQL content from a string. Convenience method for parsing without a source name. */
    public SqlSchema parseString(String sqlContent) {
        return parse(sqlContent, "inline-sql");
    }

    /** Parses SQL content and returns the complete schema. */
    public SqlSchema parse(String sqlContent, String sourceName) {
        SqlSchema.SqlSchemaBuilder schemaBuilder =
                SqlSchema.builder().name(sourceName).sourceFile(sourceName);

        List<SqlTable> tables = new ArrayList<>();
        List<SqlFunction> functions = new ArrayList<>();
        List<SqlIndex> standaloneIndexes = new ArrayList<>();
        List<String> parseErrors = new ArrayList<>();

        // Pre-process: extract functions/procedures with $$ delimiters
        List<SqlFunction> extractedFunctions = extractFunctions(sqlContent);
        functions.addAll(extractedFunctions);

        // Remove functions from content to avoid parse errors
        String cleanedContent = removeFunctionBodies(sqlContent);

        try {
            Statements statements = CCJSqlParserUtil.parseStatements(cleanedContent);
            processStatements(statements, tables, standaloneIndexes, functions, parseErrors);
        } catch (JSQLParserException e) {
            // Try parsing statement by statement
            parseErrors.add("Batch parse failed, trying individual statements: " + e.getMessage());
            parseIndividualStatements(
                    cleanedContent, tables, standaloneIndexes, functions, parseErrors);
        }

        return schemaBuilder
                .tables(tables)
                .functions(functions)
                .standaloneIndexes(standaloneIndexes)
                .parseErrors(parseErrors)
                .build();
    }

    /** Processes parsed statements and categorizes them into tables, indexes, functions, etc. */
    private void processStatements(
            Statements statements,
            List<SqlTable> tables,
            List<SqlIndex> standaloneIndexes,
            List<SqlFunction> functions,
            List<String> parseErrors) {
        for (Statement statement : statements.getStatements()) {
            try {
                if (statement instanceof CreateTable createTable) {
                    tables.add(parseCreateTable(createTable));
                } else if (statement instanceof CreateIndex createIndex) {
                    standaloneIndexes.add(parseCreateIndex(createIndex));
                } else if (statement instanceof Alter alter) {
                    processAlterStatement(alter, tables, parseErrors);
                } else if (statement instanceof CreateFunction createFunction) {
                    functions.add(parseCreateFunction(createFunction));
                }
            } catch (Exception e) {
                parseErrors.add("Error parsing statement: " + e.getMessage());
            }
        }
    }

    /**
     * Parses a CREATE TABLE statement.
     *
     * <p>Note: This method has high cognitive complexity due to the comprehensive parsing of SQL
     * table structures including columns, constraints, indexes, and foreign keys. Breaking it down
     * further would reduce readability and maintainability.
     */
    @SuppressWarnings("java:S3776") // Cognitive complexity justified: comprehensive SQL parsing
    private SqlTable parseCreateTable(CreateTable createTable) {
        SqlTable.SqlTableBuilder tableBuilder =
                SqlTable.builder()
                        .name(createTable.getTable().getName())
                        .schema(createTable.getTable().getSchemaName());

        List<SqlColumn> columns = new ArrayList<>();
        List<SqlForeignKey> foreignKeys = new ArrayList<>();
        List<SqlIndex> indexes = new ArrayList<>();
        List<String> primaryKeyColumns = new ArrayList<>();
        List<String> uniqueConstraints = new ArrayList<>();
        List<String> checkConstraints = new ArrayList<>();

        // Parse column definitions
        if (createTable.getColumnDefinitions() != null) {
            for (ColumnDefinition colDef : createTable.getColumnDefinitions()) {
                SqlColumn column = parseColumnDefinition(colDef);
                columns.add(column);

                if (column.isPrimaryKey()) {
                    primaryKeyColumns.add(column.getName());
                }

                // Check for inline REFERENCES
                if (colDef.getColumnSpecs() != null) {
                    String specs = String.join(" ", colDef.getColumnSpecs());
                    SqlForeignKey fk = parseInlineReference(colDef.getColumnName(), specs);
                    if (fk != null) {
                        foreignKeys.add(fk);
                    }
                }
            }
        }

        // Parse table-level constraints (indexes, FKs, etc.)
        if (createTable.getIndexes() != null) {
            for (Index index : createTable.getIndexes()) {
                if (index instanceof ForeignKeyIndex fkIndex) {
                    foreignKeys.add(parseForeignKeyIndex(fkIndex));
                } else if (INDEX_TYPE_PRIMARY_KEY.equalsIgnoreCase(index.getType())) {
                    primaryKeyColumns.clear();
                    List<String> indexColumnNames =
                            index.getColumns().stream()
                                    .map(Index.ColumnParams::getColumnName)
                                    .toList();
                    indexColumnNames.forEach(
                            col -> {
                                primaryKeyColumns.add(col);
                                // Mark column as PK
                                columns.stream()
                                        .filter(c -> c.getName().equalsIgnoreCase(col))
                                        .forEach(c -> c.setPrimaryKey(true));
                            });
                } else if (INDEX_TYPE_UNIQUE.equalsIgnoreCase(index.getType())) {
                    List<String> indexColumnNames =
                            index.getColumns().stream()
                                    .map(Index.ColumnParams::getColumnName)
                                    .toList();
                    uniqueConstraints.add(String.join(",", indexColumnNames));
                    // Mark columns as unique
                    if (indexColumnNames.size() == 1) {
                        columns.stream()
                                .filter(c -> c.getName().equalsIgnoreCase(indexColumnNames.get(0)))
                                .forEach(c -> c.setUnique(true));
                    }
                } else {
                    indexes.add(parseTableIndex(index, createTable.getTable().getName()));
                }
            }
        }

        return tableBuilder
                .columns(columns)
                .foreignKeys(foreignKeys)
                .indexes(indexes)
                .primaryKeyColumns(primaryKeyColumns)
                .uniqueConstraints(uniqueConstraints)
                .checkConstraints(checkConstraints)
                .build();
    }

    /** Parses a column definition. */
    private SqlColumn parseColumnDefinition(ColumnDefinition colDef) {
        ColDataType dataType = colDef.getColDataType();
        String sqlType = dataType.getDataType();

        SqlColumn.SqlColumnBuilder builder =
                SqlColumn.builder()
                        .name(colDef.getColumnName())
                        .sqlType(sqlType)
                        .javaType(mapSqlTypeToJava(sqlType, dataType))
                        .nullable(true);

        // Parse type arguments (length, precision, scale)
        if (dataType.getArgumentsStringList() != null
                && !dataType.getArgumentsStringList().isEmpty()) {
            List<String> args = dataType.getArgumentsStringList();
            try {
                builder.length(Integer.parseInt(args.get(0)));
                if (args.size() > 1) {
                    builder.precision(Integer.parseInt(args.get(0)));
                    builder.scale(Integer.parseInt(args.get(1)));
                }
            } catch (NumberFormatException _) {
                // Non-numeric argument (e.g., enum values) - ignore silently
            }
        }

        // Parse column specs (NOT NULL, PRIMARY KEY, UNIQUE, DEFAULT, etc.)
        if (colDef.getColumnSpecs() != null) {
            String specs = String.join(" ", colDef.getColumnSpecs()).toUpperCase(Locale.ROOT);

            if (specs.contains("NOT NULL")) {
                builder.nullable(false);
            }
            if (specs.contains(INDEX_TYPE_PRIMARY_KEY)) {
                builder.primaryKey(true);
                builder.nullable(false);
            }
            if (specs.contains(INDEX_TYPE_UNIQUE)) {
                builder.unique(true);
            }
            if (specs.contains("AUTO_INCREMENT")
                    || specs.contains("SERIAL")
                    || specs.contains("GENERATED")
                    || specs.contains("IDENTITY")) {
                builder.autoIncrement(true);
            }

            // Parse DEFAULT value - use possessive quantifiers to prevent catastrophic backtracking
            Pattern defaultPattern =
                    Pattern.compile("DEFAULT\\s++(\\S++)", Pattern.CASE_INSENSITIVE);
            Matcher defaultMatcher =
                    defaultPattern.matcher(String.join(" ", colDef.getColumnSpecs()));
            if (defaultMatcher.find()) {
                builder.defaultValue(defaultMatcher.group(1).trim());
            }
        }

        return builder.build();
    }

    /**
     * Maps SQL types to Java types.
     *
     * <p>Note: This method has high cognitive complexity due to the comprehensive mapping of SQL
     * data types to Java types. The switch expression provides clear, readable mapping that would
     * be less maintainable if broken into smaller methods.
     */
    @SuppressWarnings("java:S3776") // Cognitive complexity justified: comprehensive type mapping
    private String mapSqlTypeToJava(String sqlType, ColDataType dataType) {
        // Strip any type arguments like (100) or (10,2) - JSqlParser may include them
        String cleanType = sqlType.replaceAll("\\s*\\(.*\\)\\s*", "").trim();
        String upperType = cleanType.toUpperCase(Locale.ROOT);

        return switch (upperType) {
            case "INTEGER", "INT", "INT4" -> "Integer";
            case "BIGINT", "INT8", "BIGSERIAL", "SERIAL8" -> "Long";
            case "SMALLINT", "INT2", "SMALLSERIAL", "SERIAL2" -> "Short";
            case "TINYINT" -> "Byte";
            case "DECIMAL", "NUMERIC", "NUMBER" -> "BigDecimal";
            case "REAL", "FLOAT4" -> "Float";
            case "DOUBLE", "FLOAT8", "DOUBLE PRECISION", "FLOAT" -> "Double";
            case "BOOLEAN", "BOOL", "BIT" -> "Boolean";
            case "VARCHAR",
                    "CHARACTER VARYING",
                    "NVARCHAR",
                    "TEXT",
                    "CHAR",
                    "CHARACTER",
                    "NCHAR",
                    "CLOB",
                    "NCLOB" ->
                    JAVA_TYPE_STRING;
            case "DATE" -> "LocalDate";
            case "TIME", "TIMETZ", "TIME WITH TIME ZONE" -> "LocalTime";
            case "TIMESTAMP", "TIMESTAMPTZ", "TIMESTAMP WITH TIME ZONE", "DATETIME" ->
                    "LocalDateTime";
            case "UUID" -> "UUID";
            case "JSON", "JSONB" -> JAVA_TYPE_STRING; // Could map to JsonNode
            case "BYTEA", "BLOB", "BINARY", "VARBINARY", "LONGVARBINARY" -> "byte[]";
            case "SERIAL", "SERIAL4" -> "Integer";
            case "MONEY" -> "BigDecimal";
            case "INET", "CIDR", "MACADDR" -> JAVA_TYPE_STRING;
            case "POINT", "LINE", "LSEG", "BOX", "PATH", "POLYGON", "CIRCLE" ->
                    JAVA_TYPE_STRING; // Geometry types
            case "INTERVAL" -> "Duration";
            case "ARRAY" -> "List<Object>";
            case "ENUM" -> JAVA_TYPE_STRING; // Could generate enum
            default -> {
                // Check for array types
                if (upperType.endsWith("[]")) {
                    String baseType =
                            mapSqlTypeToJava(
                                    upperType.substring(0, upperType.length() - 2), dataType);
                    yield "List<" + baseType + ">";
                }
                yield "Object";
            }
        };
    }

    /** Parses inline REFERENCES clause. */
    private SqlForeignKey parseInlineReference(String columnName, String specs) {
        Matcher refMatcher = REFERENCES_PATTERN.matcher(specs);
        if (refMatcher.find()) {
            SqlForeignKey.SqlForeignKeyBuilder builder =
                    SqlForeignKey.builder()
                            .columnName(columnName)
                            .referencedTable(refMatcher.group(1))
                            .referencedColumn(refMatcher.group(2));

            Matcher deleteMatcher = ON_DELETE_PATTERN.matcher(specs);
            if (deleteMatcher.find()) {
                builder.onDelete(parseFkAction(deleteMatcher.group(1)));
            }

            Matcher updateMatcher = ON_UPDATE_PATTERN.matcher(specs);
            if (updateMatcher.find()) {
                builder.onUpdate(parseFkAction(updateMatcher.group(1)));
            }

            return builder.build();
        }
        return null;
    }

    /**
     * Parses a FOREIGN KEY constraint.
     *
     * <p>Note: Uses getReferencedColumnNames() which is deprecated in jsqlparser 5.x. The new API
     * getReferencedColumns() should be used when available. Suppressing deprecation warning until
     * jsqlparser API stabilizes.
     */
    @SuppressWarnings("deprecation") // S1874: jsqlparser API migration in progress
    private SqlForeignKey parseForeignKeyIndex(ForeignKeyIndex fkIndex) {
        List<String> fkColumnNames =
                fkIndex.getColumns().stream().map(Index.ColumnParams::getColumnName).toList();
        List<String> refColumnNames = fkIndex.getReferencedColumnNames();

        SqlForeignKey.SqlForeignKeyBuilder builder =
                SqlForeignKey.builder()
                        .name(fkIndex.getName())
                        .columnName(fkColumnNames.get(0))
                        .referencedTable(fkIndex.getTable().getName())
                        .referencedColumn(refColumnNames.get(0));

        // S1858: toString() IS required - jsqlparser returns ReferenceOption enum, not String
        if (fkIndex.getOnDeleteReferenceOption() != null) {
            builder.onDelete(parseFkAction(fkIndex.getOnDeleteReferenceOption().toString()));
        }
        if (fkIndex.getOnUpdateReferenceOption() != null) {
            builder.onUpdate(parseFkAction(fkIndex.getOnUpdateReferenceOption().toString()));
        }

        return builder.build();
    }

    /** Parses FK action string to enum. */
    private SqlForeignKey.ForeignKeyAction parseFkAction(String action) {
        return switch (action.toUpperCase(Locale.ROOT).replace(" ", "_")) {
            case "CASCADE" -> SqlForeignKey.ForeignKeyAction.CASCADE;
            case "SET_NULL" -> SqlForeignKey.ForeignKeyAction.SET_NULL;
            case "SET_DEFAULT" -> SqlForeignKey.ForeignKeyAction.SET_DEFAULT;
            case "RESTRICT" -> SqlForeignKey.ForeignKeyAction.RESTRICT;
            default -> SqlForeignKey.ForeignKeyAction.NO_ACTION;
        };
    }

    /** Parses a table-level index. */
    private SqlIndex parseTableIndex(Index index, String tableName) {
        List<String> indexColumnNames =
                index.getColumns().stream().map(Index.ColumnParams::getColumnName).toList();
        return SqlIndex.builder()
                .name(index.getName())
                .tableName(tableName)
                .columns(new ArrayList<>(indexColumnNames))
                .unique(INDEX_TYPE_UNIQUE.equalsIgnoreCase(index.getType()))
                .type(SqlIndex.IndexType.BTREE)
                .build();
    }

    /** Parses a CREATE INDEX statement. */
    private SqlIndex parseCreateIndex(CreateIndex createIndex) {
        Index index = createIndex.getIndex();
        List<String> indexColumnNames =
                index.getColumns().stream().map(Index.ColumnParams::getColumnName).toList();
        return SqlIndex.builder()
                .name(index.getName())
                .tableName(createIndex.getTable().getName())
                .columns(new ArrayList<>(indexColumnNames))
                .unique(INDEX_TYPE_UNIQUE.equalsIgnoreCase(index.getType()))
                .type(parseIndexType(createIndex))
                .build();
    }

    /** Determines index type from CREATE INDEX statement. */
    private SqlIndex.IndexType parseIndexType(CreateIndex createIndex) {
        String indexSpec = createIndex.toString().toUpperCase(Locale.ROOT);
        if (indexSpec.contains("USING GIN")) return SqlIndex.IndexType.GIN;
        if (indexSpec.contains("USING GIST")) return SqlIndex.IndexType.GIST;
        if (indexSpec.contains("USING HASH")) return SqlIndex.IndexType.HASH;
        if (indexSpec.contains("USING BRIN")) return SqlIndex.IndexType.BRIN;
        return SqlIndex.IndexType.BTREE;
    }

    /** Processes ALTER TABLE statements to add FKs to existing tables. */
    private void processAlterStatement(Alter alter, List<SqlTable> tables, List<String> errors) {
        String tableName = alter.getTable().getName();
        SqlTable table =
                tables.stream()
                        .filter(t -> t.getName().equalsIgnoreCase(tableName))
                        .findFirst()
                        .orElse(null);

        if (table == null) {
            errors.add("ALTER references unknown table: " + tableName);
            return;
        }

        if (alter.getAlterExpressions() != null) {
            for (AlterExpression expr : alter.getAlterExpressions()) {
                if (expr.getIndex() instanceof ForeignKeyIndex fkIndex) {
                    table.getForeignKeys().add(parseForeignKeyIndex(fkIndex));
                }
            }
        }
    }

    /** Parses a CREATE FUNCTION statement. */
    private SqlFunction parseCreateFunction(CreateFunction createFunction) {
        return SqlFunction.builder()
                .name(createFunction.getFunctionDeclarationParts().get(0))
                .type(SqlFunction.FunctionType.FUNCTION)
                .parameters(new ArrayList<>())
                .build();
    }

    /** Extracts functions with $$ delimiters using regex. */
    private List<SqlFunction> extractFunctions(String content) {
        List<SqlFunction> functions = new ArrayList<>();

        Matcher matcher = CREATE_FUNCTION_PATTERN.matcher(content);
        while (matcher.find()) {
            String funcType = matcher.group(2);
            String funcName = matcher.group(3);
            String params = matcher.group(4);
            String returnType = matcher.group(6);
            String language = matcher.group(8);
            String body = matcher.group(9);

            List<SqlFunction.SqlParameter> parameters = parseParameters(params);

            functions.add(
                    SqlFunction.builder()
                            .name(funcName)
                            .type(
                                    "PROCEDURE".equalsIgnoreCase(funcType)
                                            ? SqlFunction.FunctionType.PROCEDURE
                                            : SqlFunction.FunctionType.FUNCTION)
                            .parameters(parameters)
                            .returnType(returnType != null ? returnType.trim() : null)
                            .language(language != null ? language : "sql")
                            .body(body.trim())
                            .build());
        }

        return functions;
    }

    /** Parses function parameters. */
    private List<SqlFunction.SqlParameter> parseParameters(String params) {
        List<SqlFunction.SqlParameter> parameters = new ArrayList<>();
        if (params == null || params.isBlank()) return parameters;

        // Split by comma, handling nested parentheses
        int depth = 0;
        StringBuilder current = new StringBuilder();
        for (char c : params.toCharArray()) {
            if (c == '(') depth++;
            else if (c == ')') depth--;
            else if (c == ',' && depth == 0) {
                addParameter(current.toString().trim(), parameters);
                current = new StringBuilder();
                continue;
            }
            current.append(c);
        }
        if (!current.isEmpty()) {
            addParameter(current.toString().trim(), parameters);
        }

        return parameters;
    }

    private void addParameter(String param, List<SqlFunction.SqlParameter> parameters) {
        if (param.isBlank()) return;

        String[] parts = param.split("\\s+", 3);
        SqlFunction.SqlParameter.ParameterMode mode = SqlFunction.SqlParameter.ParameterMode.IN;
        String name;
        String type;

        if (parts.length >= 2) {
            int idx = 0;
            if (parts[0].equalsIgnoreCase("IN")
                    || parts[0].equalsIgnoreCase("OUT")
                    || parts[0].equalsIgnoreCase("INOUT")) {
                mode =
                        SqlFunction.SqlParameter.ParameterMode.valueOf(
                                parts[0].toUpperCase(Locale.ROOT));
                idx = 1;
            }
            name = parts[idx];
            type = parts.length > idx + 1 ? parts[idx + 1] : "text";
        } else {
            name = "param" + (parameters.size() + 1);
            type = parts[0];
        }

        parameters.add(
                SqlFunction.SqlParameter.builder()
                        .name(name)
                        .sqlType(type)
                        .javaType(mapSqlTypeToJava(type, null))
                        .mode(mode)
                        .build());
    }

    /** Removes function bodies to avoid parse errors. */
    private String removeFunctionBodies(String content) {
        return content.replaceAll("\\$\\$[^$]*\\$\\$", "''");
    }

    /**
     * Parses statements individually when batch parsing fails. Note: functions parameter reserved
     * for future CREATE FUNCTION individual parsing.
     */
    @SuppressWarnings("java:S1172") // functions param reserved for future use
    private void parseIndividualStatements(
            String content,
            List<SqlTable> tables,
            List<SqlIndex> indexes,
            List<SqlFunction> functions,
            List<String> errors) {
        // Split by semicolon
        String[] statements = content.split(";");

        for (String stmtStr : statements) {
            String trimmed = stmtStr.trim();
            if (trimmed.isEmpty()) continue;

            try {
                Statement stmt = CCJSqlParserUtil.parse(trimmed);
                if (stmt instanceof CreateTable ct) {
                    tables.add(parseCreateTable(ct));
                } else if (stmt instanceof CreateIndex ci) {
                    indexes.add(parseCreateIndex(ci));
                } else if (stmt instanceof Alter alter) {
                    processAlterStatement(alter, tables, errors);
                }
            } catch (JSQLParserException _) {
                // Try regex parsing for unsupported syntax
                if (trimmed.toUpperCase(Locale.ROOT).startsWith("CREATE TABLE")) {
                    errors.add(
                            "Could not parse CREATE TABLE: "
                                    + trimmed.substring(0, Math.min(50, trimmed.length())));
                }
            }
        }
    }
}
