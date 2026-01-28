package com.jnzader.apigen.codegen.generator.csharp.test;

import static com.jnzader.apigen.codegen.generator.util.NamingUtils.toPascalCase;
import static com.jnzader.apigen.codegen.generator.util.TestValueProvider.getSampleTestValueCSharp;

import com.jnzader.apigen.codegen.model.SqlColumn;
import com.jnzader.apigen.codegen.model.SqlTable;

/** Generates unit test classes for Repository implementations in C#/ASP.NET Core using xUnit. */
@SuppressWarnings("java:S1192") // Duplicate strings intentional for code generation templates
public class CSharpRepositoryTestGenerator {

    private final String baseNamespace;

    public CSharpRepositoryTestGenerator(String baseNamespace) {
        this.baseNamespace = baseNamespace;
    }

    /** Generates the Repository test class code in C#. */
    public String generate(SqlTable table) {
        String entityName = table.getEntityName();
        String moduleName = toPascalCase(table.getModuleName());

        // Generate sample field assignments for creating test data
        StringBuilder fieldAssignments = new StringBuilder();

        for (SqlColumn col : table.getBusinessColumns()) {
            String fieldName = toPascalCase(col.getJavaFieldName());
            String sampleValue = getSampleTestValueCSharp(col);

            fieldAssignments
                    .append("\n            ")
                    .append(fieldName)
                    .append(" = ")
                    .append(sampleValue)
                    .append(",");
        }

        return
"""
using FluentAssertions;
using Microsoft.EntityFrameworkCore;
using Xunit;
using %s.%s.Domain.Entities;
using %s.%s.Infrastructure.Repositories;
using %s.Infrastructure.Persistence;

namespace %s.Tests.%s.Infrastructure.Repositories;

public class %sRepositoryTests : IDisposable
{
    private readonly ApplicationDbContext _context;
    private readonly %sRepository _repository;

    public %sRepositoryTests()
    {
        var options = new DbContextOptionsBuilder<ApplicationDbContext>()
            .UseInMemoryDatabase(databaseName: Guid.NewGuid().ToString())
            .Options;

        _context = new ApplicationDbContext(options);
        _repository = new %sRepository(_context);
    }

    public void Dispose()
    {
        _context.Dispose();
        GC.SuppressFinalize(this);
    }

    private %s CreateTestEntity()
    {
        return new %s
        {%s
            Estado = true
        };
    }

    [Fact]
    public async Task GetByIdAsync_ShouldReturnEntity_WhenExists()
    {
        // Arrange
        var entity = CreateTestEntity();
        await _context.Set<%s>().AddAsync(entity);
        await _context.SaveChangesAsync();

        // Act
        var result = await _repository.GetByIdAsync(entity.Id);

        // Assert
        result.Should().NotBeNull();
        result!.Id.Should().Be(entity.Id);
    }

    [Fact]
    public async Task GetByIdAsync_ShouldReturnNull_WhenNotExists()
    {
        // Act
        var result = await _repository.GetByIdAsync(999999);

        // Assert
        result.Should().BeNull();
    }

    [Fact]
    public async Task GetAllAsync_ShouldReturnAllEntities()
    {
        // Arrange
        var entity1 = CreateTestEntity();
        var entity2 = CreateTestEntity();
        await _context.Set<%s>().AddRangeAsync(entity1, entity2);
        await _context.SaveChangesAsync();

        // Act
        var result = await _repository.GetAllAsync();

        // Assert
        result.Should().HaveCount(2);
    }

    [Fact]
    public async Task AddAsync_ShouldAddEntity()
    {
        // Arrange
        var entity = CreateTestEntity();

        // Act
        var result = await _repository.AddAsync(entity);
        await _context.SaveChangesAsync();

        // Assert
        result.Should().NotBeNull();
        result.Id.Should().BeGreaterThan(0);
        _context.Set<%s>().Should().ContainSingle();
    }

    [Fact]
    public async Task UpdateAsync_ShouldUpdateEntity()
    {
        // Arrange
        var entity = CreateTestEntity();
        await _context.Set<%s>().AddAsync(entity);
        await _context.SaveChangesAsync();

        // Act
        entity.Estado = false;
        await _repository.UpdateAsync(entity);
        await _context.SaveChangesAsync();

        // Assert
        var updated = await _context.Set<%s>().FindAsync(entity.Id);
        updated!.Estado.Should().BeFalse();
    }

    [Fact]
    public async Task DeleteAsync_ShouldRemoveEntity()
    {
        // Arrange
        var entity = CreateTestEntity();
        await _context.Set<%s>().AddAsync(entity);
        await _context.SaveChangesAsync();
        var id = entity.Id;

        // Act
        await _repository.DeleteAsync(entity);
        await _context.SaveChangesAsync();

        // Assert
        var deleted = await _context.Set<%s>().FindAsync(id);
        deleted.Should().BeNull();
    }

    [Fact]
    public async Task CountAsync_ShouldReturnCorrectCount()
    {
        // Arrange
        var entity1 = CreateTestEntity();
        var entity2 = CreateTestEntity();
        await _context.Set<%s>().AddRangeAsync(entity1, entity2);
        await _context.SaveChangesAsync();

        // Act
        var count = await _repository.CountAsync();

        // Assert
        count.Should().Be(2);
    }

    [Fact]
    public async Task SoftDeleteAsync_ShouldSetEstadoToFalse()
    {
        // Arrange
        var entity = CreateTestEntity();
        await _context.Set<%s>().AddAsync(entity);
        await _context.SaveChangesAsync();

        // Act
        entity.Estado = false;
        entity.DeletedAt = DateTime.UtcNow;
        entity.DeletedBy = "testUser";
        await _repository.UpdateAsync(entity);
        await _context.SaveChangesAsync();

        // Assert
        var softDeleted = await _context.Set<%s>().FindAsync(entity.Id);
        softDeleted!.Estado.Should().BeFalse();
        softDeleted.DeletedAt.Should().NotBeNull();
        softDeleted.DeletedBy.Should().Be("testUser");
    }
}
"""
                .formatted(
                        // Using statements
                        baseNamespace,
                        moduleName, // .Domain.Entities
                        baseNamespace,
                        moduleName, // .Infrastructure.Repositories
                        baseNamespace, // .Infrastructure.Persistence
                        // Namespace
                        baseNamespace,
                        moduleName, // .Tests.X.Infrastructure.Repositories
                        // Class and constructor
                        entityName, // RepositoryTests class
                        entityName, // XRepository _repository
                        entityName, // XRepositoryTests()
                        entityName, // new XRepository()
                        // Helper method
                        entityName, // CreateTestEntity return type
                        entityName, // new X
                        fieldAssignments.toString(),
                        // Test methods
                        entityName, // Set<X>() GetByIdAsync
                        entityName, // Set<X>() GetAllAsync
                        entityName, // Set<X>() AddAsync
                        entityName, // Set<X>() UpdateAsync add
                        entityName, // Set<X>() UpdateAsync find
                        entityName, // Set<X>() DeleteAsync add
                        entityName, // Set<X>() DeleteAsync find
                        entityName, // Set<X>() CountAsync
                        entityName, // Set<X>() SoftDeleteAsync add
                        entityName); // Set<X>() SoftDeleteAsync find
    }
}
