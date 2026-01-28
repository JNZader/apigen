package com.jnzader.apigen.codegen.generator.csharp.test;

import static com.jnzader.apigen.codegen.generator.util.NamingUtils.toPascalCase;
import static com.jnzader.apigen.codegen.generator.util.TestValueProvider.getSampleTestValueCSharp;

import com.jnzader.apigen.codegen.model.SqlColumn;
import com.jnzader.apigen.codegen.model.SqlTable;

/** Generates unit test classes for Entity classes in C#/ASP.NET Core using xUnit. */
@SuppressWarnings("java:S1192") // Duplicate strings intentional for code generation templates
public class CSharpEntityTestGenerator {

    private final String baseNamespace;

    public CSharpEntityTestGenerator(String baseNamespace) {
        this.baseNamespace = baseNamespace;
    }

    /** Generates the Entity test class code in C#. */
    public String generate(SqlTable table) {
        String entityName = table.getEntityName();
        String moduleName = toPascalCase(table.getModuleName());

        // Generate field assignments for business columns
        StringBuilder fieldAssignments = new StringBuilder();
        StringBuilder fieldAssertions = new StringBuilder();

        for (SqlColumn col : table.getBusinessColumns()) {
            String fieldName = toPascalCase(col.getJavaFieldName());
            String sampleValue = getSampleTestValueCSharp(col);

            fieldAssignments
                    .append("\n            ")
                    .append(fieldName)
                    .append(" = ")
                    .append(sampleValue)
                    .append(",");

            fieldAssertions
                    .append("\n        entity.")
                    .append(fieldName)
                    .append(".Should().Be(")
                    .append(sampleValue)
                    .append(");");
        }

        return
"""
using FluentAssertions;
using Xunit;
using %s.%s.Domain.Entities;

namespace %s.Tests.%s.Domain.Entities;

public class %sTests
{
    [Fact]
    public void Entity_ShouldInitialize_WithDefaultValues()
    {
        // Arrange & Act
        var entity = new %s();

        // Assert
        entity.Id.Should().Be(0);
        entity.Estado.Should().BeTrue();
        entity.CreatedAt.Should().Be(default);
        entity.UpdatedAt.Should().BeNull();
        entity.CreatedBy.Should().BeNull();
        entity.UpdatedBy.Should().BeNull();
        entity.DeletedAt.Should().BeNull();
        entity.DeletedBy.Should().BeNull();
    }

    [Fact]
    public void Entity_ShouldSetProperties_Correctly()
    {
        // Arrange
        var entity = new %s
        {
            Id = 1,%s
            Estado = true,
            CreatedAt = DateTime.UtcNow,
            CreatedBy = "testUser"
        };

        // Assert
        entity.Id.Should().Be(1);
        entity.Estado.Should().BeTrue();
        entity.CreatedBy.Should().Be("testUser");%s
    }

    [Fact]
    public void Entity_EstadoShouldDefaultToTrue()
    {
        // Arrange & Act
        var entity = new %s();

        // Assert
        entity.Estado.Should().BeTrue();
    }

    [Fact]
    public void Entity_ShouldAllowSoftDelete()
    {
        // Arrange
        var entity = new %s
        {
            Id = 1,
            Estado = true
        };

        // Act
        entity.Estado = false;
        entity.DeletedAt = DateTime.UtcNow;
        entity.DeletedBy = "admin";

        // Assert
        entity.Estado.Should().BeFalse();
        entity.DeletedAt.Should().NotBeNull();
        entity.DeletedBy.Should().Be("admin");
    }

    [Fact]
    public void Entity_ShouldTrackAuditFields()
    {
        // Arrange
        var now = DateTime.UtcNow;
        var entity = new %s
        {
            CreatedAt = now,
            CreatedBy = "creator",
            UpdatedAt = now.AddHours(1),
            UpdatedBy = "updater"
        };

        // Assert
        entity.CreatedAt.Should().Be(now);
        entity.CreatedBy.Should().Be("creator");
        entity.UpdatedAt.Should().Be(now.AddHours(1));
        entity.UpdatedBy.Should().Be("updater");
    }
}
"""
                .formatted(
                        baseNamespace,
                        moduleName,
                        baseNamespace,
                        moduleName,
                        entityName,
                        entityName,
                        entityName,
                        fieldAssignments.toString(),
                        fieldAssertions.toString(),
                        entityName,
                        entityName,
                        entityName);
    }
}
