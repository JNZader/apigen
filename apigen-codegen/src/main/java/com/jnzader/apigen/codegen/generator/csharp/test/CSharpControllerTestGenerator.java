package com.jnzader.apigen.codegen.generator.csharp.test;

import static com.jnzader.apigen.codegen.generator.util.NamingUtils.toPascalCase;
import static com.jnzader.apigen.codegen.generator.util.NamingUtils.toPlural;
import static com.jnzader.apigen.codegen.generator.util.TestValueProvider.getSampleTestValueCSharp;

import com.jnzader.apigen.codegen.model.SqlColumn;
import com.jnzader.apigen.codegen.model.SqlTable;

/** Generates unit test classes for Controller implementations in C#/ASP.NET Core using xUnit. */
@SuppressWarnings("java:S1192") // Duplicate strings intentional for code generation templates
public class CSharpControllerTestGenerator {

    private final String baseNamespace;

    public CSharpControllerTestGenerator(String baseNamespace) {
        this.baseNamespace = baseNamespace;
    }

    /** Generates the Controller test class code in C#. */
    public String generate(SqlTable table) {
        String entityName = table.getEntityName();
        String pluralName = toPlural(entityName);
        String moduleName = toPascalCase(table.getModuleName());

        // Generate sample field assignments
        StringBuilder dtoFieldAssignments = new StringBuilder();

        for (SqlColumn col : table.getBusinessColumns()) {
            String fieldName = toPascalCase(col.getJavaFieldName());
            String sampleValue = getSampleTestValueCSharp(col);

            dtoFieldAssignments
                    .append("\n            ")
                    .append(fieldName)
                    .append(" = ")
                    .append(sampleValue)
                    .append(",");
        }

        return
"""
using FluentAssertions;
using Microsoft.AspNetCore.Mvc;
using Moq;
using Xunit;
using %s.%s.Api.Controllers;
using %s.%s.Application.DTOs;
using %s.%s.Application.Interfaces;
using %s.Application.Common;
using %s.Application.Exceptions;

namespace %s.Tests.%s.Api.Controllers;

public class %sControllerTests
{
    private readonly Mock<I%sService> _mockService;
    private readonly %sController _controller;

    public %sControllerTests()
    {
        _mockService = new Mock<I%sService>();
        _controller = new %sController(_mockService.Object);
    }

    private %sDto CreateTestDto()
    {
        return new %sDto
        {
            Id = 1,%s
            Estado = true
        };
    }

    [Fact]
    public async Task GetById_ShouldReturnOk_WhenEntityExists()
    {
        // Arrange
        var dto = CreateTestDto();
        _mockService.Setup(s => s.GetByIdAsync(1, It.IsAny<CancellationToken>()))
            .Returns(Task.FromResult(dto));

        // Act
        var result = await _controller.GetById(1);

        // Assert
        var okResult = result.Result.Should().BeOfType<OkObjectResult>().Subject;
        okResult.Value.Should().BeEquivalentTo(dto);
    }

    [Fact]
    public async Task GetById_ShouldThrowNotFound_WhenEntityNotExists()
    {
        // Arrange
        _mockService.Setup(s => s.GetByIdAsync(It.IsAny<long>(), It.IsAny<CancellationToken>()))
            .ThrowsAsync(new NotFoundException(nameof(%s), 999999));

        // Act & Assert
        await _controller.Invoking(c => c.GetById(999999))
            .Should().ThrowAsync<NotFoundException>();
    }

    [Fact]
    public async Task GetAll_ShouldReturnOk_WithPagedResult()
    {
        // Arrange
        var pagedResult = new PagedResult<%sDto>
        {
            Content = new List<%sDto> { CreateTestDto() },
            TotalElements = 1,
            Page = 0,
            Size = 10,
            TotalPages = 1
        };
        _mockService.Setup(s => s.GetAllAsync(0, 10, It.IsAny<CancellationToken>()))
            .Returns(Task.FromResult(pagedResult));

        // Act
        var result = await _controller.GetAll(0, 10);

        // Assert
        var okResult = result.Result.Should().BeOfType<OkObjectResult>().Subject;
        var returnedResult = okResult.Value.Should().BeOfType<PagedResult<%sDto>>().Subject;
        returnedResult.Content.Should().HaveCount(1);
    }

    [Fact]
    public async Task Create_ShouldReturnCreated_WithNewEntity()
    {
        // Arrange
        var createDto = new Create%sDto
        {%s
        };
        var createdDto = CreateTestDto();

        _mockService.Setup(s => s.CreateAsync(createDto, It.IsAny<CancellationToken>()))
            .Returns(Task.FromResult(createdDto));

        // Act
        var result = await _controller.Create(createDto);

        // Assert
        var createdResult = result.Result.Should().BeOfType<CreatedAtActionResult>().Subject;
        createdResult.Value.Should().BeEquivalentTo(createdDto);
        createdResult.ActionName.Should().Be(nameof(_controller.GetById));
    }

    [Fact]
    public async Task Update_ShouldReturnOk_WhenEntityExists()
    {
        // Arrange
        var updateDto = new Update%sDto
        {%s
        };
        var updatedDto = CreateTestDto();

        _mockService.Setup(s => s.UpdateAsync(1, updateDto, It.IsAny<CancellationToken>()))
            .Returns(Task.FromResult(updatedDto));

        // Act
        var result = await _controller.Update(1, updateDto);

        // Assert
        var okResult = result.Result.Should().BeOfType<OkObjectResult>().Subject;
        okResult.Value.Should().BeEquivalentTo(updatedDto);
    }

    [Fact]
    public async Task Update_ShouldThrowNotFound_WhenEntityNotExists()
    {
        // Arrange
        var updateDto = new Update%sDto();

        _mockService.Setup(s => s.UpdateAsync(999999, updateDto, It.IsAny<CancellationToken>()))
            .ThrowsAsync(new NotFoundException(nameof(%s), 999999));

        // Act & Assert
        await _controller.Invoking(c => c.Update(999999, updateDto))
            .Should().ThrowAsync<NotFoundException>();
    }

    [Fact]
    public async Task Delete_ShouldReturnNoContent()
    {
        // Arrange
        _mockService.Setup(s => s.DeleteAsync(1, It.IsAny<CancellationToken>()))
            .Returns(Task.CompletedTask);

        // Act
        var result = await _controller.Delete(1);

        // Assert
        result.Should().BeOfType<NoContentResult>();
    }

}
"""
                .formatted(
                        baseNamespace,
                        moduleName,
                        baseNamespace,
                        moduleName,
                        baseNamespace,
                        moduleName,
                        baseNamespace,
                        baseNamespace,
                        moduleName,
                        pluralName,
                        entityName,
                        pluralName,
                        pluralName,
                        entityName,
                        pluralName,
                        entityName,
                        entityName,
                        dtoFieldAssignments.toString(),
                        entityName,
                        entityName,
                        entityName,
                        entityName,
                        entityName,
                        dtoFieldAssignments.toString(),
                        entityName,
                        dtoFieldAssignments.toString(),
                        entityName,
                        entityName);
    }
}
