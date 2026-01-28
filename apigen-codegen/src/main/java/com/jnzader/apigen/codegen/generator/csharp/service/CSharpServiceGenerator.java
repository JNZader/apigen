package com.jnzader.apigen.codegen.generator.csharp.service;

import static com.jnzader.apigen.codegen.generator.util.NamingUtils.*;

import com.jnzader.apigen.codegen.model.SqlTable;

/** Generates Service interface and implementation for C#/ASP.NET Core. */
@SuppressWarnings("java:S1192") // Duplicate strings intentional for code generation templates
public class CSharpServiceGenerator {

    private final String baseNamespace;

    public CSharpServiceGenerator(String baseNamespace) {
        this.baseNamespace = baseNamespace;
    }

    /** Generates the Service interface. */
    public String generateInterface(SqlTable table) {
        String entityName = table.getEntityName();
        String moduleName = table.getModuleName();
        String pascalModule = toPascalCase(moduleName);
        String namespace = baseNamespace + "." + pascalModule + ".Application.Interfaces";

        StringBuilder sb = new StringBuilder();

        // Using statements
        sb.append("using ").append(baseNamespace).append(".Application.Interfaces;\n");
        sb.append("using ")
                .append(baseNamespace)
                .append(".")
                .append(pascalModule)
                .append(".Application.DTOs;\n\n");

        sb.append("namespace ").append(namespace).append(";\n\n");

        // Interface declaration
        sb.append("/// <summary>\n");
        sb.append("/// Service interface for ").append(entityName).append(" operations.\n");
        sb.append("/// </summary>\n");
        sb.append("public interface I")
                .append(entityName)
                .append("Service : IService<")
                .append(entityName)
                .append("Dto, Create")
                .append(entityName)
                .append("Dto, Update")
                .append(entityName)
                .append("Dto, long>\n");
        sb.append("{\n");
        sb.append("}\n");

        return sb.toString();
    }

    /** Generates the Service implementation. */
    public String generateImpl(SqlTable table) {
        String entityName = table.getEntityName();
        String moduleName = table.getModuleName();
        String pascalModule = toPascalCase(moduleName);
        String namespace = baseNamespace + "." + pascalModule + ".Application.Services";

        StringBuilder sb = new StringBuilder();

        // Using statements
        sb.append("using AutoMapper;\n");
        sb.append("using ").append(baseNamespace).append(".Application.Exceptions;\n");
        sb.append("using ").append(baseNamespace).append(".Application.Services;\n");
        sb.append("using ")
                .append(baseNamespace)
                .append(".")
                .append(pascalModule)
                .append(".Application.DTOs;\n");
        sb.append("using ")
                .append(baseNamespace)
                .append(".")
                .append(pascalModule)
                .append(".Application.Interfaces;\n");
        sb.append("using ")
                .append(baseNamespace)
                .append(".")
                .append(pascalModule)
                .append(".Domain.Entities;\n");
        sb.append("using ")
                .append(baseNamespace)
                .append(".")
                .append(pascalModule)
                .append(".Domain.Interfaces;\n\n");

        sb.append("namespace ").append(namespace).append(";\n\n");

        // Class declaration
        sb.append("/// <summary>\n");
        sb.append("/// Service implementation for ").append(entityName).append(" operations.\n");
        sb.append("/// </summary>\n");
        sb.append("public class ")
                .append(entityName)
                .append("Service : Service<")
                .append(entityName)
                .append(", ")
                .append(entityName)
                .append("Dto, Create")
                .append(entityName)
                .append("Dto, Update")
                .append(entityName)
                .append("Dto, long>, I")
                .append(entityName)
                .append("Service\n");
        sb.append("{\n");

        // Private fields
        sb.append("    private readonly I")
                .append(entityName)
                .append("Repository _repository;\n\n");

        // Constructor
        sb.append("    public ").append(entityName).append("Service(\n");
        sb.append("        I").append(entityName).append("Repository repository,\n");
        sb.append("        IMapper mapper)\n");
        sb.append("        : base(repository, mapper)\n");
        sb.append("    {\n");
        sb.append("        _repository = repository;\n");
        sb.append("    }\n\n");

        // Override GetByIdAsync to provide entity-specific error message
        sb.append("    /// <inheritdoc />\n");
        sb.append("    public override async Task<")
                .append(entityName)
                .append("Dto> GetByIdAsync(long id, CancellationToken cancellationToken)\n");
        sb.append("    {\n");
        sb.append("        var entity = await _repository.GetByIdAsync(id, cancellationToken);\n");
        sb.append("        if (entity is null)\n");
        sb.append("        {\n");
        sb.append("            throw new NotFoundException(nameof(")
                .append(entityName)
                .append("), id);\n");
        sb.append("        }\n");
        sb.append("        return Mapper.Map<").append(entityName).append("Dto>(entity);\n");
        sb.append("    }\n");

        sb.append("}\n");

        return sb.toString();
    }
}
