package com.jnzader.apigen.codegen.generator.csharp.test;

import static com.jnzader.apigen.codegen.generator.util.NamingUtils.toPascalCase;
import static com.jnzader.apigen.codegen.generator.util.TestValueProvider.getSampleTestValueCSharp;

import com.jnzader.apigen.codegen.model.SqlColumn;
import com.jnzader.apigen.codegen.model.SqlTable;

/** Generates unit test classes for Service implementations in C#/ASP.NET Core using xUnit. */
@SuppressWarnings("java:S1192") // Duplicate strings intentional for code generation templates
public class CSharpServiceTestGenerator {

    private final String baseNamespace;

    public CSharpServiceTestGenerator(String baseNamespace) {
        this.baseNamespace = baseNamespace;
    }

    /** Generates the Service test class code in C#. */
    public String generate(SqlTable table) {
        String entityName = table.getEntityName();
        String moduleName = toPascalCase(table.getModuleName());

        // Generate sample field assignments
        StringBuilder entityFieldAssignments = new StringBuilder();
        StringBuilder dtoFieldAssignments = new StringBuilder();

        for (SqlColumn col : table.getBusinessColumns()) {
            String fieldName = toPascalCase(col.getJavaFieldName());
            String sampleValue = getSampleTestValueCSharp(col);

            entityFieldAssignments
                    .append("\n            ")
                    .append(fieldName)
                    .append(" = ")
                    .append(sampleValue)
                    .append(",");

            dtoFieldAssignments
                    .append("\n            ")
                    .append(fieldName)
                    .append(" = ")
                    .append(sampleValue)
                    .append(",");
        }

        return
"""
using AutoMapper;
using FluentAssertions;
using Moq;
using Xunit;
using %s.Application.Exceptions;
using %s.%s.Application.DTOs;
using %s.%s.Application.Services;
using %s.%s.Domain.Entities;
using %s.%s.Domain.Interfaces;

namespace %s.Tests.%s.Application.Services;

public class %sServiceTests
{
    private readonly Mock<I%sRepository> _mockRepository;
    private readonly Mock<IMapper> _mockMapper;
    private readonly %sService _service;

    public %sServiceTests()
    {
        _mockRepository = new Mock<I%sRepository>();
        _mockMapper = new Mock<IMapper>();
        _service = new %sService(_mockRepository.Object, _mockMapper.Object);
    }

    private %s CreateTestEntity()
    {
        return new %s
        {
            Id = 1,%s
            Estado = true
        };
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
    public async Task GetByIdAsync_ShouldReturnDto_WhenEntityExists()
    {
        // Arrange
        var entity = CreateTestEntity();
        var dto = CreateTestDto();

        _mockRepository.Setup(r => r.GetByIdAsync(1, It.IsAny<CancellationToken>()))
            .Returns(Task.FromResult(entity));
        _mockMapper.Setup(m => m.Map<%sDto>(entity))
            .Returns(dto);

        // Act
        var result = await _service.GetByIdAsync(1, CancellationToken.None);

        // Assert
        result.Should().NotBeNull();
        result!.Id.Should().Be(1);
    }

    [Fact]
    public async Task GetByIdAsync_ShouldThrowNotFoundException_WhenEntityNotExists()
    {
        // Arrange
        _mockRepository.Setup(r => r.GetByIdAsync(It.IsAny<long>(), It.IsAny<CancellationToken>()))
            .Returns(Task.FromResult<%s?>(null));

        // Act & Assert
        await Assert.ThrowsAsync<NotFoundException>(() => _service.GetByIdAsync(999999, CancellationToken.None));
    }

    [Fact]
    public async Task GetAllAsync_ShouldReturnPagedResult()
    {
        // Arrange
        var entities = new List<%s> { CreateTestEntity() };
        var dtos = new List<%sDto> { CreateTestDto() };

        _mockRepository.Setup(r => r.GetAllAsync(0, 10, It.IsAny<CancellationToken>()))
            .Returns(Task.FromResult<IEnumerable<%s>>(entities));
        _mockRepository.Setup(r => r.CountAsync(It.IsAny<CancellationToken>()))
            .Returns(Task.FromResult(1));
        _mockMapper.Setup(m => m.Map<IEnumerable<%sDto>>(entities))
            .Returns(dtos);

        // Act
        var result = await _service.GetAllAsync(0, 10, CancellationToken.None);

        // Assert
        result.Should().NotBeNull();
        result.Content.Should().HaveCount(1);
        result.TotalElements.Should().Be(1);
    }

    [Fact]
    public async Task CreateAsync_ShouldReturnCreatedDto()
    {
        // Arrange
        var createDto = new Create%sDto
        {%s
        };
        var entity = CreateTestEntity();
        var resultDto = CreateTestDto();

        _mockMapper.Setup(m => m.Map<%s>(createDto))
            .Returns(entity);
        _mockRepository.Setup(r => r.AddAsync(entity, It.IsAny<CancellationToken>()))
            .Returns(Task.FromResult(entity));
        _mockMapper.Setup(m => m.Map<%sDto>(entity))
            .Returns(resultDto);

        // Act
        var result = await _service.CreateAsync(createDto, CancellationToken.None);

        // Assert
        result.Should().NotBeNull();
        result.Id.Should().Be(1);
    }

    [Fact]
    public async Task UpdateAsync_ShouldReturnUpdatedDto()
    {
        // Arrange
        var updateDto = new Update%sDto
        {%s
        };
        var existingEntity = CreateTestEntity();
        var updatedDto = CreateTestDto();

        _mockRepository.Setup(r => r.GetByIdAsync(1, It.IsAny<CancellationToken>()))
            .Returns(Task.FromResult(existingEntity));
        _mockMapper.Setup(m => m.Map(updateDto, existingEntity));
        _mockRepository.Setup(r => r.UpdateAsync(existingEntity, It.IsAny<CancellationToken>()))
            .Returns(Task.FromResult(existingEntity));
        _mockMapper.Setup(m => m.Map<%sDto>(existingEntity))
            .Returns(updatedDto);

        // Act
        var result = await _service.UpdateAsync(1, updateDto, CancellationToken.None);

        // Assert
        result.Should().NotBeNull();
    }

    [Fact]
    public async Task DeleteAsync_ShouldCallRepository()
    {
        // Arrange
        var entity = CreateTestEntity();

        _mockRepository.Setup(r => r.GetByIdAsync(1, It.IsAny<CancellationToken>()))
            .Returns(Task.FromResult(entity));
        _mockRepository.Setup(r => r.SoftDeleteAsync(entity, It.IsAny<CancellationToken>()))
            .Returns(Task.CompletedTask);

        // Act
        await _service.DeleteAsync(1, CancellationToken.None);

        // Assert
        _mockRepository.Verify(r => r.SoftDeleteAsync(entity, It.IsAny<CancellationToken>()), Times.Once);
    }
}
"""
                .formatted(
                        // Using statements
                        baseNamespace, // .Application.Exceptions
                        baseNamespace,
                        moduleName, // .Application.DTOs
                        baseNamespace,
                        moduleName, // .Application.Services
                        baseNamespace,
                        moduleName, // .Domain.Entities
                        baseNamespace,
                        moduleName, // .Domain.Interfaces
                        // Namespace
                        baseNamespace,
                        moduleName, // .Tests.X.Application.Services
                        // Class and constructor
                        entityName, // ServiceTests class
                        entityName, // Mock<IXRepository>
                        entityName, // XService _service
                        entityName, // XServiceTests()
                        entityName, // Mock<IXRepository>()
                        entityName, // new XService()
                        // Helper methods
                        entityName, // CreateTestEntity return type
                        entityName, // new X
                        entityFieldAssignments.toString(),
                        entityName, // CreateTestDto return type
                        entityName, // new XDto
                        dtoFieldAssignments.toString(),
                        // GetByIdAsync_ShouldReturnDto
                        entityName, // Map<XDto>
                        // GetByIdAsync_ShouldThrowNotFoundException
                        entityName, // (X?)null
                        // GetAllAsync
                        entityName, // List<X>
                        entityName, // List<XDto>
                        entityName, // IEnumerable<X>
                        entityName, // Map<IEnumerable<XDto>>
                        // CreateAsync
                        entityName, // CreateXDto
                        dtoFieldAssignments.toString(),
                        entityName, // Map<X>
                        entityName, // Map<XDto>
                        // UpdateAsync
                        entityName, // UpdateXDto
                        dtoFieldAssignments.toString(),
                        entityName); // Map<XDto>
    }
}
