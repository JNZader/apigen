package com.jnzader.apigen.codegen.generator.csharp.test;

import static com.jnzader.apigen.codegen.generator.util.NamingUtils.toPascalCase;
import static com.jnzader.apigen.codegen.generator.util.TestValueProvider.getSampleTestValueCSharp;

import com.jnzader.apigen.codegen.model.SqlColumn;
import com.jnzader.apigen.codegen.model.SqlTable;

/** Generates unit test classes for DTO classes in C#/ASP.NET Core using xUnit. */
@SuppressWarnings("java:S1192") // Duplicate strings intentional for code generation templates
public class CSharpDTOTestGenerator {

    private final String baseNamespace;

    public CSharpDTOTestGenerator(String baseNamespace) {
        this.baseNamespace = baseNamespace;
    }

    /** Generates the DTO test class code in C#. */
    public String generate(SqlTable table) {
        String entityName = table.getEntityName();
        String moduleName = toPascalCase(table.getModuleName());

        // Generate field assignments and assertions
        StringBuilder dtoFieldAssignments = new StringBuilder();
        StringBuilder createDtoFieldAssignments = new StringBuilder();
        StringBuilder fieldAssertions = new StringBuilder();

        for (SqlColumn col : table.getBusinessColumns()) {
            String fieldName = toPascalCase(col.getJavaFieldName());
            String sampleValue = getSampleTestValueCSharp(col);

            dtoFieldAssignments
                    .append("\n            ")
                    .append(fieldName)
                    .append(" = ")
                    .append(sampleValue)
                    .append(",");

            createDtoFieldAssignments
                    .append("\n            ")
                    .append(fieldName)
                    .append(" = ")
                    .append(sampleValue)
                    .append(",");

            fieldAssertions
                    .append("\n        dto.")
                    .append(fieldName)
                    .append(".Should().Be(")
                    .append(sampleValue)
                    .append(");");
        }

        return
"""
using FluentAssertions;
using System.ComponentModel.DataAnnotations;
using Xunit;
using %s.%s.Application.DTOs;

namespace %s.Tests.%s.Application.DTOs;

public class %sDtoTests
{
    [Fact]
    public void Dto_ShouldInitialize_WithProperties()
    {
        // Arrange & Act
        var dto = new %sDto
        {
            Id = 1,%s
            Estado = true
        };

        // Assert
        dto.Id.Should().Be(1);
        dto.Estado.Should().BeTrue();%s
    }

    [Fact]
    public void Dto_ShouldHaveDefaultValues()
    {
        // Arrange & Act
        var dto = new %sDto();

        // Assert
        dto.Id.Should().Be(0);
        dto.Estado.Should().BeFalse();
    }

    [Fact]
    public void CreateDto_ShouldBeValid_WithRequiredFields()
    {
        // Arrange
        var createDto = new Create%sDto
        {%s
        };

        // Act
        var validationResults = ValidateModel(createDto);

        // Assert
        validationResults.Should().BeEmpty();
    }

    [Fact]
    public void UpdateDto_ShouldBeValid_WithOptionalFields()
    {
        // Arrange
        var updateDto = new Update%sDto
        {%s
        };

        // Act
        var validationResults = ValidateModel(updateDto);

        // Assert
        validationResults.Should().BeEmpty();
    }

    [Fact]
    public void UpdateDto_ShouldBeValid_WithEmptyFields()
    {
        // Arrange
        var updateDto = new Update%sDto();

        // Act
        var validationResults = ValidateModel(updateDto);

        // Assert - Update DTOs typically allow empty fields for partial updates
        validationResults.Should().BeEmpty();
    }

    [Fact]
    public void Dto_EstadoProperty_ShouldBeSetCorrectly()
    {
        // Arrange
        var dto = new %sDto { Estado = true };

        // Assert
        dto.Estado.Should().BeTrue();
    }

    private static List<ValidationResult> ValidateModel<T>(T model) where T : class
    {
        var validationResults = new List<ValidationResult>();
        var validationContext = new ValidationContext(model, null, null);
        Validator.TryValidateObject(model, validationContext, validationResults, true);
        return validationResults;
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
                        dtoFieldAssignments.toString(),
                        fieldAssertions.toString(),
                        entityName,
                        entityName,
                        createDtoFieldAssignments.toString(),
                        entityName,
                        createDtoFieldAssignments.toString(),
                        entityName,
                        entityName);
    }
}
