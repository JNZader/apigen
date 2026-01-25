package com.jnzader.apigen.codegen.generator.csharp.dbcontext;

import com.jnzader.apigen.codegen.generator.common.ManyToManyRelation;
import com.jnzader.apigen.codegen.model.SqlForeignKey;
import com.jnzader.apigen.codegen.model.SqlSchema;
import com.jnzader.apigen.codegen.model.SqlTable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Generates Entity Framework Core DbContext for C#/ASP.NET Core. */
@SuppressWarnings({
    "java:S1192",
    "java:S3400",
    "java:S6126"
}) // S1192: template strings; S3400: template methods; S6126: concatenated lines for readability
public class CSharpDbContextGenerator {

    private final String baseNamespace;

    public CSharpDbContextGenerator(String baseNamespace) {
        this.baseNamespace = baseNamespace;
    }

    /** Generates the ApplicationDbContext class. */
    public String generate(SqlSchema schema) {
        StringBuilder sb = new StringBuilder();

        String namespace = baseNamespace + ".Infrastructure.Persistence";

        // Using statements
        sb.append("using Microsoft.EntityFrameworkCore;\n");
        sb.append("using ").append(baseNamespace).append(".Domain.Common;\n");

        // Collect all entity namespaces
        for (SqlTable table : schema.getEntityTables()) {
            String moduleName = table.getModuleName();
            sb.append("using ")
                    .append(baseNamespace)
                    .append(".")
                    .append(toPascalCase(moduleName))
                    .append(".Domain.Entities;\n");
        }

        sb.append("\n");
        sb.append("namespace ").append(namespace).append(";\n\n");

        // Class declaration
        sb.append("/// <summary>\n");
        sb.append("/// Application database context for Entity Framework Core.\n");
        sb.append("/// </summary>\n");
        sb.append("public class ApplicationDbContext : DbContext\n");
        sb.append("{\n");

        // Constructor
        sb.append(
                "    public ApplicationDbContext(DbContextOptions<ApplicationDbContext>"
                        + " options)\n");
        sb.append("        : base(options)\n");
        sb.append("    {\n");
        sb.append("    }\n\n");

        // DbSet properties
        for (SqlTable table : schema.getEntityTables()) {
            String entityName = table.getEntityName();
            String pluralName = toPlural(entityName);
            sb.append("    public DbSet<")
                    .append(entityName)
                    .append("> ")
                    .append(pluralName)
                    .append(" => Set<")
                    .append(entityName)
                    .append(">();\n");
        }

        sb.append("\n");

        // OnModelCreating
        sb.append("    protected override void OnModelCreating(ModelBuilder modelBuilder)\n");
        sb.append("    {\n");
        sb.append("        base.OnModelCreating(modelBuilder);\n\n");

        // Apply configurations from assembly
        sb.append("        // Apply all entity configurations from this assembly\n");
        sb.append(
                "       "
                    + " modelBuilder.ApplyConfigurationsFromAssembly(typeof(ApplicationDbContext).Assembly);\n\n");

        // Configure global query filters for soft delete
        sb.append(
                "        // Configure soft delete query filter for all entities inheriting from"
                        + " BaseEntity\n");
        for (SqlTable table : schema.getEntityTables()) {
            String entityName = table.getEntityName();
            sb.append("        modelBuilder.Entity<")
                    .append(entityName)
                    .append(">().HasQueryFilter(e => e.Estado);\n");
        }

        sb.append("\n");

        // Configure many-to-many relationships
        Map<String, List<ManyToManyRelation>> manyToManyByTable =
                findAllManyToManyRelations(schema);
        if (!manyToManyByTable.isEmpty()) {
            sb.append("        // Configure many-to-many relationships\n");
            for (Map.Entry<String, List<ManyToManyRelation>> entry : manyToManyByTable.entrySet()) {
                for (ManyToManyRelation rel : entry.getValue()) {
                    String entityName = toPascalCase(entry.getKey().replaceAll("s$", ""));
                    String targetEntityName = rel.targetTable().getEntityName();
                    String collectionName = toPlural(targetEntityName);

                    sb.append("        modelBuilder.Entity<").append(entityName).append(">()\n");
                    sb.append("            .HasMany(e => e.").append(collectionName).append(")\n");
                    sb.append("            .WithMany(e => e.")
                            .append(toPlural(entityName))
                            .append(")\n");
                    sb.append("            .UsingEntity(j => j.ToTable(\"")
                            .append(rel.junctionTable())
                            .append("\"));\n\n");
                }
            }
        }

        sb.append("    }\n\n");

        // SaveChangesAsync override for auditing
        sb.append(
                "    public override Task<int> SaveChangesAsync(CancellationToken cancellationToken"
                        + " = default)\n");
        sb.append("    {\n");
        sb.append("        var entries = ChangeTracker.Entries<BaseEntity>();\n");
        sb.append("        var now = DateTime.UtcNow;\n\n");
        sb.append("        foreach (var entry in entries)\n");
        sb.append("        {\n");
        sb.append("            switch (entry.State)\n");
        sb.append("            {\n");
        sb.append("                case EntityState.Added:\n");
        sb.append("                    entry.Entity.CreatedAt = now;\n");
        sb.append("                    entry.Entity.Estado = true;\n");
        sb.append("                    break;\n");
        sb.append("                case EntityState.Modified:\n");
        sb.append("                    entry.Entity.UpdatedAt = now;\n");
        sb.append("                    break;\n");
        sb.append("            }\n");
        sb.append("        }\n\n");
        sb.append("        return base.SaveChangesAsync(cancellationToken);\n");
        sb.append("    }\n");

        sb.append("}\n");
        return sb.toString();
    }

    private Map<String, List<ManyToManyRelation>> findAllManyToManyRelations(SqlSchema schema) {
        Map<String, List<ManyToManyRelation>> result = new HashMap<>();

        for (SqlTable junctionTable : schema.getJunctionTables()) {
            List<SqlForeignKey> fks = junctionTable.getForeignKeys();
            if (fks.size() != 2) continue;

            SqlForeignKey fk1 = fks.get(0);
            SqlForeignKey fk2 = fks.get(1);

            SqlTable table1 = schema.getTableByName(fk1.getReferencedTable());
            SqlTable table2 = schema.getTableByName(fk2.getReferencedTable());

            if (table1 != null && table2 != null) {
                result.computeIfAbsent(table1.getName(), k -> new ArrayList<>())
                        .add(
                                new ManyToManyRelation(
                                        junctionTable.getName(),
                                        fk1.getColumnName(),
                                        fk2.getColumnName(),
                                        table2));
            }
        }

        return result;
    }

    private String toPlural(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        if (name.endsWith("y")) {
            return name.substring(0, name.length() - 1) + "ies";
        } else if (name.endsWith("s") || name.endsWith("x") || name.endsWith("ch")) {
            return name + "es";
        }
        return name + "s";
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
}
