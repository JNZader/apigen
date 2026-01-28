package com.jnzader.apigen.codegen.generator.csharp.controller;

import static com.jnzader.apigen.codegen.generator.util.NamingUtils.*;

import com.jnzader.apigen.codegen.model.SqlTable;
import java.util.Locale;

/** Generates REST API controllers for C#/ASP.NET Core. */
@SuppressWarnings("java:S1192") // Duplicate strings intentional for code generation templates
public class CSharpControllerGenerator {

    private final String baseNamespace;

    public CSharpControllerGenerator(String baseNamespace) {
        this.baseNamespace = baseNamespace;
    }

    /** Generates the Controller class. */
    public String generate(SqlTable table) {
        String entityName = table.getEntityName();
        String moduleName = table.getModuleName();
        String pascalModule = toPascalCase(moduleName);
        String namespace = baseNamespace + "." + pascalModule + ".Api.Controllers";
        String routeName = toKebabCase(toPlural(entityName));

        StringBuilder sb = new StringBuilder();

        // Using statements
        sb.append("using Microsoft.AspNetCore.Mvc;\n");
        sb.append("using ").append(baseNamespace).append(".Application.Common;\n");
        sb.append("using ")
                .append(baseNamespace)
                .append(".")
                .append(pascalModule)
                .append(".Application.DTOs;\n");
        sb.append("using ")
                .append(baseNamespace)
                .append(".")
                .append(pascalModule)
                .append(".Application.Interfaces;\n\n");

        sb.append("namespace ").append(namespace).append(";\n\n");

        // Controller class
        sb.append("/// <summary>\n");
        sb.append("/// API controller for ").append(entityName).append(" operations.\n");
        sb.append("/// </summary>\n");
        sb.append("[ApiController]\n");
        sb.append("[Route(\"api/v1/").append(routeName).append("\")]\n");
        sb.append("[Produces(\"application/json\")]\n");
        sb.append("public class ")
                .append(toPlural(entityName))
                .append("Controller : ControllerBase\n");
        sb.append("{\n");

        // Private field
        sb.append("    private readonly I").append(entityName).append("Service _service;\n\n");

        // Constructor
        sb.append("    public ")
                .append(toPlural(entityName))
                .append("Controller(I")
                .append(entityName)
                .append("Service service)\n");
        sb.append("    {\n");
        sb.append("        _service = service;\n");
        sb.append("    }\n\n");

        // GET all with pagination
        sb.append("    /// <summary>\n");
        sb.append("    /// Gets all ")
                .append(toPlural(entityName).toLowerCase(Locale.ROOT))
                .append(" with pagination.\n");
        sb.append("    /// </summary>\n");
        sb.append("    [HttpGet]\n");
        sb.append("    [ProducesResponseType(typeof(PagedResult<")
                .append(entityName)
                .append("Dto>), StatusCodes.Status200OK)]\n");
        sb.append("    public async Task<ActionResult<PagedResult<")
                .append(entityName)
                .append("Dto>>> GetAll(\n");
        sb.append("        [FromQuery] int page = 0,\n");
        sb.append("        [FromQuery] int size = 10,\n");
        sb.append("        CancellationToken cancellationToken = default)\n");
        sb.append("    {\n");
        sb.append(
                "        var result = await _service.GetAllAsync(page, size,"
                        + " cancellationToken);\n");
        sb.append("        return Ok(result);\n");
        sb.append("    }\n\n");

        // GET by ID
        sb.append("    /// <summary>\n");
        sb.append("    /// Gets a ")
                .append(entityName.toLowerCase(Locale.ROOT))
                .append(" by ID.\n");
        sb.append("    /// </summary>\n");
        sb.append("    [HttpGet(\"{id}\")]\n");
        sb.append("    [ProducesResponseType(typeof(")
                .append(entityName)
                .append("Dto), StatusCodes.Status200OK)]\n");
        sb.append("    [ProducesResponseType(StatusCodes.Status404NotFound)]\n");
        sb.append("    public async Task<ActionResult<")
                .append(entityName)
                .append("Dto>> GetById(\n");
        sb.append("        long id,\n");
        sb.append("        CancellationToken cancellationToken = default)\n");
        sb.append("    {\n");
        sb.append("        var result = await _service.GetByIdAsync(id, cancellationToken);\n");
        sb.append("        return Ok(result);\n");
        sb.append("    }\n\n");

        // POST create
        sb.append("    /// <summary>\n");
        sb.append("    /// Creates a new ")
                .append(entityName.toLowerCase(Locale.ROOT))
                .append(".\n");
        sb.append("    /// </summary>\n");
        sb.append("    [HttpPost]\n");
        sb.append("    [ProducesResponseType(typeof(")
                .append(entityName)
                .append("Dto), StatusCodes.Status201Created)]\n");
        sb.append("    [ProducesResponseType(StatusCodes.Status400BadRequest)]\n");
        sb.append("    public async Task<ActionResult<")
                .append(entityName)
                .append("Dto>> Create(\n");
        sb.append("        [FromBody] Create").append(entityName).append("Dto dto,\n");
        sb.append("        CancellationToken cancellationToken = default)\n");
        sb.append("    {\n");
        sb.append("        var result = await _service.CreateAsync(dto, cancellationToken);\n");
        sb.append(
                "        return CreatedAtAction(nameof(GetById), new { id = result.Id },"
                        + " result);\n");
        sb.append("    }\n\n");

        // PUT update
        sb.append("    /// <summary>\n");
        sb.append("    /// Updates an existing ")
                .append(entityName.toLowerCase(Locale.ROOT))
                .append(".\n");
        sb.append("    /// </summary>\n");
        sb.append("    [HttpPut(\"{id}\")]\n");
        sb.append("    [ProducesResponseType(typeof(")
                .append(entityName)
                .append("Dto), StatusCodes.Status200OK)]\n");
        sb.append("    [ProducesResponseType(StatusCodes.Status404NotFound)]\n");
        sb.append("    [ProducesResponseType(StatusCodes.Status400BadRequest)]\n");
        sb.append("    public async Task<ActionResult<")
                .append(entityName)
                .append("Dto>> Update(\n");
        sb.append("        long id,\n");
        sb.append("        [FromBody] Update").append(entityName).append("Dto dto,\n");
        sb.append("        CancellationToken cancellationToken = default)\n");
        sb.append("    {\n");
        sb.append("        var result = await _service.UpdateAsync(id, dto, cancellationToken);\n");
        sb.append("        return Ok(result);\n");
        sb.append("    }\n\n");

        // DELETE (soft delete)
        sb.append("    /// <summary>\n");
        sb.append("    /// Soft deletes a ")
                .append(entityName.toLowerCase(Locale.ROOT))
                .append(".\n");
        sb.append("    /// </summary>\n");
        sb.append("    [HttpDelete(\"{id}\")]\n");
        sb.append("    [ProducesResponseType(StatusCodes.Status204NoContent)]\n");
        sb.append("    [ProducesResponseType(StatusCodes.Status404NotFound)]\n");
        sb.append("    public async Task<IActionResult> Delete(\n");
        sb.append("        long id,\n");
        sb.append("        CancellationToken cancellationToken = default)\n");
        sb.append("    {\n");
        sb.append("        await _service.DeleteAsync(id, cancellationToken);\n");
        sb.append("        return NoContent();\n");
        sb.append("    }\n");

        sb.append("}\n");

        return sb.toString();
    }
}
