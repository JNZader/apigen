package com.jnzader.apigen.codegen.generator.csharp.repository;

import com.jnzader.apigen.codegen.generator.csharp.CSharpTypeMapper;
import com.jnzader.apigen.codegen.model.SqlColumn;
import com.jnzader.apigen.codegen.model.SqlTable;

/** Generates Repository interface and implementation for C#/ASP.NET Core. */
@SuppressWarnings("java:S1192") // Duplicate strings intentional for code generation templates
public class CSharpRepositoryGenerator {

    private final String baseNamespace;
    private final CSharpTypeMapper typeMapper = new CSharpTypeMapper();

    public CSharpRepositoryGenerator(String baseNamespace) {
        this.baseNamespace = baseNamespace;
    }

    /** Generates the Repository interface. */
    public String generateInterface(SqlTable table) {
        String entityName = table.getEntityName();
        String moduleName = table.getModuleName();
        String namespace = baseNamespace + "." + toPascalCase(moduleName) + ".Domain.Interfaces";

        StringBuilder sb = new StringBuilder();

        // Using statements
        sb.append("using ").append(baseNamespace).append(".Domain.Interfaces;\n");
        sb.append("using ")
                .append(baseNamespace)
                .append(".")
                .append(toPascalCase(moduleName))
                .append(".Domain.Entities;\n\n");

        sb.append("namespace ").append(namespace).append(";\n\n");

        // Interface declaration
        sb.append("/// <summary>\n");
        sb.append("/// Repository interface for ").append(entityName).append(" entity.\n");
        sb.append("/// </summary>\n");
        sb.append("public interface I")
                .append(entityName)
                .append("Repository : IRepository<")
                .append(entityName)
                .append(", long>\n");
        sb.append("{\n");

        // Custom finder methods for unique columns
        for (SqlColumn col : table.getColumns()) {
            if (col.isUnique() && !col.isPrimaryKey()) {
                String fieldName = toPascalCase(col.getJavaFieldName());
                String csharpType = typeMapper.mapColumnType(col);

                sb.append("    /// <summary>\n");
                sb.append("    /// Finds a ")
                        .append(entityName)
                        .append(" by ")
                        .append(fieldName)
                        .append(".\n");
                sb.append("    /// </summary>\n");
                sb.append("    Task<")
                        .append(entityName)
                        .append("?> FindBy")
                        .append(fieldName)
                        .append("Async(")
                        .append(csharpType)
                        .append(" ")
                        .append(toCamelCase(fieldName))
                        .append(", CancellationToken cancellationToken = default);\n\n");
            }
        }

        sb.append("}\n");
        return sb.toString();
    }

    /** Generates the Repository implementation. */
    public String generateImpl(SqlTable table) {
        String entityName = table.getEntityName();
        String moduleName = table.getModuleName();
        String namespace =
                baseNamespace + "." + toPascalCase(moduleName) + ".Infrastructure.Repositories";

        StringBuilder sb = new StringBuilder();

        // Using statements
        sb.append("using Microsoft.EntityFrameworkCore;\n");
        sb.append("using ").append(baseNamespace).append(".Infrastructure.Persistence;\n");
        sb.append("using ").append(baseNamespace).append(".Infrastructure.Repositories;\n");
        sb.append("using ")
                .append(baseNamespace)
                .append(".")
                .append(toPascalCase(moduleName))
                .append(".Domain.Entities;\n");
        sb.append("using ")
                .append(baseNamespace)
                .append(".")
                .append(toPascalCase(moduleName))
                .append(".Domain.Interfaces;\n\n");

        sb.append("namespace ").append(namespace).append(";\n\n");

        // Class declaration
        sb.append("/// <summary>\n");
        sb.append("/// Repository implementation for ").append(entityName).append(" entity.\n");
        sb.append("/// </summary>\n");
        sb.append("public class ")
                .append(entityName)
                .append("Repository : Repository<")
                .append(entityName)
                .append(", long>, I")
                .append(entityName)
                .append("Repository\n");
        sb.append("{\n");

        // Constructor
        sb.append("    public ")
                .append(entityName)
                .append("Repository(ApplicationDbContext context) : base(context)\n");
        sb.append("    {\n");
        sb.append("    }\n");

        // Custom finder methods for unique columns
        for (SqlColumn col : table.getColumns()) {
            if (col.isUnique() && !col.isPrimaryKey()) {
                String fieldName = toPascalCase(col.getJavaFieldName());
                String csharpType = typeMapper.mapColumnType(col);
                String paramName = toCamelCase(fieldName);

                sb.append("\n");
                sb.append("    /// <inheritdoc />\n");
                sb.append("    public async Task<")
                        .append(entityName)
                        .append("?> FindBy")
                        .append(fieldName)
                        .append("Async(")
                        .append(csharpType)
                        .append(" ")
                        .append(paramName)
                        .append(", CancellationToken cancellationToken = default)\n");
                sb.append("    {\n");
                sb.append("        return await DbSet.FirstOrDefaultAsync(e => e.")
                        .append(fieldName)
                        .append(" == ")
                        .append(paramName)
                        .append(", cancellationToken);\n");
                sb.append("    }\n");
            }
        }

        sb.append("}\n");
        return sb.toString();
    }

    private String toPascalCase(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;
        for (char c : name.toCharArray()) {
            if (c == '_' || c == '-') {
                capitalizeNext = true;
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(Character.toLowerCase(c));
            }
        }
        return result.toString();
    }

    private String toCamelCase(String name) {
        String pascal = toPascalCase(name);
        if (pascal == null || pascal.isEmpty()) {
            return pascal;
        }
        return Character.toLowerCase(pascal.charAt(0)) + pascal.substring(1);
    }
}
