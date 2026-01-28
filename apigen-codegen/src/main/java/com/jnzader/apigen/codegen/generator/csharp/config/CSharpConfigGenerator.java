package com.jnzader.apigen.codegen.generator.csharp.config;

import static com.jnzader.apigen.codegen.generator.util.NamingUtils.*;

import com.jnzader.apigen.codegen.generator.api.ProjectConfig;
import com.jnzader.apigen.codegen.model.SqlSchema;
import com.jnzader.apigen.codegen.model.SqlTable;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Generates configuration files for C#/ASP.NET Core projects. */
@SuppressWarnings({
    "java:S1192",
    "java:S3400"
}) // S1192: template strings; S3400: template methods return constants
public class CSharpConfigGenerator {

    private static final String DEFAULT_DOTNET_VERSION = "8.0";

    private final String baseNamespace;

    public CSharpConfigGenerator(String baseNamespace) {
        this.baseNamespace = baseNamespace;
    }

    /** Generates all configuration files. */
    public Map<String, String> generate(SqlSchema schema, ProjectConfig config) {
        Map<String, String> files = new LinkedHashMap<>();

        files.put(baseNamespace + ".csproj", generateCsproj(config));
        files.put("Program.cs", generateProgramCs(schema));
        files.put("appsettings.json", generateAppSettings());
        files.put("appsettings.Development.json", generateAppSettingsDevelopment());

        return files;
    }

    private String generateCsproj(ProjectConfig config) {
        String dotnetVersion =
                config.getFrameworkVersion() != null
                        ? config.getFrameworkVersion()
                        : DEFAULT_DOTNET_VERSION;

        return
"""
<Project Sdk="Microsoft.NET.Sdk.Web">

  <PropertyGroup>
    <TargetFramework>net%s</TargetFramework>
    <Nullable>enable</Nullable>
    <ImplicitUsings>enable</ImplicitUsings>
    <RootNamespace>%s</RootNamespace>
  </PropertyGroup>

  <ItemGroup>
    <!-- Entity Framework Core -->
    <PackageReference Include="Microsoft.EntityFrameworkCore" Version="8.0.11" />
    <PackageReference Include="Microsoft.EntityFrameworkCore.Design" Version="8.0.11">
      <PrivateAssets>all</PrivateAssets>
      <IncludeAssets>runtime; build; native; contentfiles; analyzers; buildtransitive</IncludeAssets>
    </PackageReference>
    <PackageReference Include="Npgsql.EntityFrameworkCore.PostgreSQL" Version="8.0.11" />

    <!-- AutoMapper for DTO mapping (12.0.1 is last free version) -->
    <PackageReference Include="AutoMapper" Version="12.0.1" />
    <PackageReference Include="AutoMapper.Extensions.Microsoft.DependencyInjection" Version="12.0.1" />

    <!-- OpenAPI / Swagger -->
    <PackageReference Include="Swashbuckle.AspNetCore" Version="6.9.0" />

    <!-- FluentValidation -->
    <PackageReference Include="FluentValidation" Version="11.11.0" />
    <PackageReference Include="FluentValidation.DependencyInjectionExtensions" Version="11.11.0" />

    <!-- Logging -->
    <PackageReference Include="Serilog.AspNetCore" Version="8.0.3" />

    <!-- Testing -->
    <PackageReference Include="Microsoft.NET.Test.Sdk" Version="17.12.0" />
    <PackageReference Include="xunit" Version="2.9.2" />
    <PackageReference Include="xunit.runner.visualstudio" Version="3.0.0">
      <PrivateAssets>all</PrivateAssets>
      <IncludeAssets>runtime; build; native; contentfiles; analyzers; buildtransitive</IncludeAssets>
    </PackageReference>
    <PackageReference Include="Moq" Version="4.20.72" />
    <PackageReference Include="FluentAssertions" Version="6.12.2" />
    <PackageReference Include="Microsoft.AspNetCore.Mvc.Testing" Version="8.0.11" />
    <PackageReference Include="Microsoft.EntityFrameworkCore.InMemory" Version="8.0.11" />
    <PackageReference Include="coverlet.collector" Version="6.0.2">
      <PrivateAssets>all</PrivateAssets>
      <IncludeAssets>runtime; build; native; contentfiles; analyzers; buildtransitive</IncludeAssets>
    </PackageReference>
  </ItemGroup>

</Project>
"""
                .formatted(dotnetVersion, baseNamespace);
    }

    private String generateProgramCs(SqlSchema schema) {
        StringBuilder sb = new StringBuilder();

        // Using statements
        sb.append("using Microsoft.EntityFrameworkCore;\n");
        sb.append("using ").append(baseNamespace).append(".Infrastructure.Persistence;\n");

        // Add repository and service registrations for each entity
        for (SqlTable table : schema.getEntityTables()) {
            String moduleName = toPascalCase(table.getModuleName());
            sb.append("using ")
                    .append(baseNamespace)
                    .append(".")
                    .append(moduleName)
                    .append(".Application.Interfaces;\n");
            sb.append("using ")
                    .append(baseNamespace)
                    .append(".")
                    .append(moduleName)
                    .append(".Application.Services;\n");
            sb.append("using ")
                    .append(baseNamespace)
                    .append(".")
                    .append(moduleName)
                    .append(".Domain.Interfaces;\n");
            sb.append("using ")
                    .append(baseNamespace)
                    .append(".")
                    .append(moduleName)
                    .append(".Infrastructure.Repositories;\n");
        }

        sb.append("\n");
        sb.append("var builder = WebApplication.CreateBuilder(args);\n\n");

        // Add services
        sb.append("// Add services to the container.\n");
        sb.append("builder.Services.AddControllers();\n");
        sb.append("builder.Services.AddEndpointsApiExplorer();\n");
        sb.append("builder.Services.AddSwaggerGen(c =>\n");
        sb.append("{\n");
        sb.append("    c.SwaggerDoc(\"v1\", new() { Title = \"")
                .append(baseNamespace)
                .append(" API\", Version = \"v1\" });\n");
        sb.append("});\n\n");

        // Database context
        sb.append("// Configure Entity Framework Core\n");
        sb.append("builder.Services.AddDbContext<ApplicationDbContext>(options =>\n");
        sb.append(
                "    options.UseNpgsql(builder.Configuration.GetConnectionString(\"DefaultConnection\")));\n\n");

        // AutoMapper
        sb.append("// Configure AutoMapper\n");
        sb.append("builder.Services.AddAutoMapper(typeof(Program).Assembly);\n\n");

        // Register repositories and services
        sb.append("// Register repositories\n");
        for (SqlTable table : schema.getEntityTables()) {
            String entityName = table.getEntityName();
            sb.append("builder.Services.AddScoped<I")
                    .append(entityName)
                    .append("Repository, ")
                    .append(entityName)
                    .append("Repository>();\n");
        }
        sb.append("\n");

        sb.append("// Register services\n");
        for (SqlTable table : schema.getEntityTables()) {
            String entityName = table.getEntityName();
            sb.append("builder.Services.AddScoped<I")
                    .append(entityName)
                    .append("Service, ")
                    .append(entityName)
                    .append("Service>();\n");
        }
        sb.append("\n");

        // Build app
        sb.append("var app = builder.Build();\n\n");

        // Configure pipeline
        sb.append("// Configure the HTTP request pipeline.\n");
        sb.append("if (app.Environment.IsDevelopment())\n");
        sb.append("{\n");
        sb.append("    app.UseSwagger();\n");
        sb.append("    app.UseSwaggerUI();\n");
        sb.append("}\n\n");

        sb.append("app.UseHttpsRedirection();\n");
        sb.append("app.UseAuthorization();\n");
        sb.append("app.MapControllers();\n\n");

        sb.append("app.Run();\n\n");
        sb.append("// Make Program accessible for integration tests\n");
        sb.append("public partial class Program { }\n");

        return sb.toString();
    }

    private String generateAppSettings() {
        return
"""
{
  "ConnectionStrings": {
    "DefaultConnection": "Host=localhost;Database=%s;Username=postgres;Password=postgres"
  },
  "Logging": {
    "LogLevel": {
      "Default": "Information",
      "Microsoft.AspNetCore": "Warning",
      "Microsoft.EntityFrameworkCore": "Warning"
    }
  },
  "AllowedHosts": "*"
}
"""
                .formatted(toSnakeCaseForDb(baseNamespace));
    }

    private String generateAppSettingsDevelopment() {
        return
"""
{
  "Logging": {
    "LogLevel": {
      "Default": "Debug",
      "Microsoft.AspNetCore": "Information",
      "Microsoft.EntityFrameworkCore": "Information"
    }
  }
}
""";
    }

    /**
     * Converts a namespace to a database-friendly snake_case name. This replaces dots with
     * underscores and lowercases the result.
     */
    private String toSnakeCaseForDb(String name) {
        return name.replace('.', '_').toLowerCase(Locale.ROOT);
    }
}
