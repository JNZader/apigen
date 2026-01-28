package com.jnzader.apigen.codegen.generator.csharp.test;

import static com.jnzader.apigen.codegen.generator.util.NamingUtils.toKebabCase;
import static com.jnzader.apigen.codegen.generator.util.NamingUtils.toPascalCase;
import static com.jnzader.apigen.codegen.generator.util.NamingUtils.toPlural;
import static com.jnzader.apigen.codegen.generator.util.TestValueProvider.getSampleTestValueCSharp;

import com.jnzader.apigen.codegen.model.SqlColumn;
import com.jnzader.apigen.codegen.model.SqlTable;

/** Generates integration test classes in C#/ASP.NET Core using xUnit and WebApplicationFactory. */
@SuppressWarnings("java:S1192") // Duplicate strings intentional for code generation templates
public class CSharpIntegrationTestGenerator {

    private final String baseNamespace;

    public CSharpIntegrationTestGenerator(String baseNamespace) {
        this.baseNamespace = baseNamespace;
    }

    /** Generates the Integration test class code in C#. */
    public String generate(SqlTable table) {
        String entityName = table.getEntityName();
        String kebabPlural = toKebabCase(toPlural(table.getName()));
        String moduleName = toPascalCase(table.getModuleName());

        // Generate C# object initializer properties
        StringBuilder objectInitializer = new StringBuilder();
        for (SqlColumn col : table.getBusinessColumns()) {
            String fieldName = toPascalCase(col.getJavaFieldName());
            String sampleValue = getSampleTestValueCSharp(col);

            objectInitializer
                    .append("\n            ")
                    .append(fieldName)
                    .append(" = ")
                    .append(sampleValue)
                    .append(",");
        }

        return
"""
using System.Net;
using System.Net.Http.Json;
using FluentAssertions;
using Microsoft.AspNetCore.Mvc.Testing;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.DependencyInjection;
using Xunit;
using %s.%s.Application.DTOs;
using %s.Infrastructure.Persistence;

namespace %s.Tests.%s.Integration;

public class %sIntegrationTests : IClassFixture<WebApplicationFactory<Program>>, IDisposable
{
    private readonly WebApplicationFactory<Program> _factory;
    private readonly HttpClient _client;
    private readonly IServiceScope _scope;
    private readonly ApplicationDbContext _context;

    public %sIntegrationTests(WebApplicationFactory<Program> factory)
    {
        _factory = factory.WithWebHostBuilder(builder =>
        {
            builder.ConfigureServices(services =>
            {
                // Remove the existing DbContext registration
                var descriptor = services.SingleOrDefault(
                    d => d.ServiceType == typeof(DbContextOptions<ApplicationDbContext>));
                if (descriptor != null)
                {
                    services.Remove(descriptor);
                }

                // Add in-memory database for testing
                services.AddDbContext<ApplicationDbContext>(options =>
                {
                    options.UseInMemoryDatabase("TestDb_" + Guid.NewGuid());
                });
            });
        });

        _client = _factory.CreateClient();
        _scope = _factory.Services.CreateScope();
        _context = _scope.ServiceProvider.GetRequiredService<ApplicationDbContext>();
    }

    public void Dispose()
    {
        _scope.Dispose();
        _client.Dispose();
        GC.SuppressFinalize(this);
    }

    [Fact]
    public async Task GetAll_ShouldReturnEmptyList_WhenNoEntitiesExist()
    {
        // Act
        var response = await _client.GetAsync("/api/v1/%s");

        // Assert
        response.StatusCode.Should().Be(HttpStatusCode.OK);
        var result = await response.Content.ReadFromJsonAsync<PagedResponse<%sDto>>();
        result.Should().NotBeNull();
        result!.Items.Should().BeEmpty();
    }

    [Fact]
    public async Task Create_ShouldReturnCreated_WithValidData()
    {
        // Arrange
        var createDto = new Create%sDto
        {%s
        };

        // Act
        var response = await _client.PostAsJsonAsync("/api/v1/%s", createDto);

        // Assert
        response.StatusCode.Should().Be(HttpStatusCode.Created);
        var result = await response.Content.ReadFromJsonAsync<%sDto>();
        result.Should().NotBeNull();
        result!.Id.Should().BeGreaterThan(0);
    }

    [Fact]
    public async Task GetById_ShouldReturnEntity_WhenExists()
    {
        // Arrange - Create an entity first
        var createDto = new Create%sDto
        {%s
        };
        var createResponse = await _client.PostAsJsonAsync("/api/v1/%s", createDto);
        var created = await createResponse.Content.ReadFromJsonAsync<%sDto>();

        // Act
        var response = await _client.GetAsync($"/api/v1/%s/{created!.Id}");

        // Assert
        response.StatusCode.Should().Be(HttpStatusCode.OK);
        var result = await response.Content.ReadFromJsonAsync<%sDto>();
        result.Should().NotBeNull();
        result!.Id.Should().Be(created.Id);
    }

    [Fact]
    public async Task GetById_ShouldReturnNotFound_WhenNotExists()
    {
        // Act
        var response = await _client.GetAsync("/api/v1/%s/999999");

        // Assert
        response.StatusCode.Should().Be(HttpStatusCode.NotFound);
    }

    [Fact]
    public async Task Update_ShouldReturnOk_WhenEntityExists()
    {
        // Arrange - Create an entity first
        var createDto = new Create%sDto
        {%s
        };
        var createResponse = await _client.PostAsJsonAsync("/api/v1/%s", createDto);
        var created = await createResponse.Content.ReadFromJsonAsync<%sDto>();

        var updateDto = new Update%sDto
        {%s
        };

        // Act
        var response = await _client.PutAsJsonAsync($"/api/v1/%s/{created!.Id}", updateDto);

        // Assert
        response.StatusCode.Should().Be(HttpStatusCode.OK);
    }

    [Fact]
    public async Task Delete_ShouldReturnNoContent_WhenEntityExists()
    {
        // Arrange - Create an entity first
        var createDto = new Create%sDto
        {%s
        };
        var createResponse = await _client.PostAsJsonAsync("/api/v1/%s", createDto);
        var created = await createResponse.Content.ReadFromJsonAsync<%sDto>();

        // Act
        var response = await _client.DeleteAsync($"/api/v1/%s/{created!.Id}");

        // Assert
        response.StatusCode.Should().Be(HttpStatusCode.NoContent);

        // Verify entity is deleted
        var getResponse = await _client.GetAsync($"/api/v1/%s/{created.Id}");
        getResponse.StatusCode.Should().Be(HttpStatusCode.NotFound);
    }

    [Fact]
    public async Task GetAll_ShouldSupportPagination()
    {
        // Arrange - Create multiple entities
        for (int i = 0; i < 15; i++)
        {
            var createDto = new Create%sDto
            {%s
            };
            await _client.PostAsJsonAsync("/api/v1/%s", createDto);
        }

        // Act
        var response = await _client.GetAsync("/api/v1/%s?page=0&size=10");

        // Assert
        response.StatusCode.Should().Be(HttpStatusCode.OK);
        var result = await response.Content.ReadFromJsonAsync<PagedResponse<%sDto>>();
        result.Should().NotBeNull();
        result!.Items.Should().HaveCount(10);
        result.TotalCount.Should().Be(15);
    }

    private record PagedResponse<%sDto>(
        IEnumerable<%sDto> Items,
        int TotalCount,
        int Page,
        int PageSize
    );
}
"""
                .formatted(
                        baseNamespace,
                        moduleName,
                        baseNamespace,
                        baseNamespace,
                        moduleName,
                        entityName,
                        entityName,
                        // GetAll empty
                        kebabPlural,
                        entityName,
                        // Create
                        entityName,
                        objectInitializer.toString(),
                        kebabPlural,
                        entityName,
                        // GetById exists
                        entityName,
                        objectInitializer.toString(),
                        kebabPlural,
                        entityName,
                        kebabPlural,
                        entityName,
                        // GetById not found
                        kebabPlural,
                        // Update
                        entityName,
                        objectInitializer.toString(),
                        kebabPlural,
                        entityName,
                        entityName,
                        objectInitializer.toString(),
                        kebabPlural,
                        // Delete
                        entityName,
                        objectInitializer.toString(),
                        kebabPlural,
                        entityName,
                        kebabPlural,
                        kebabPlural,
                        // Pagination
                        entityName,
                        objectInitializer.toString(),
                        kebabPlural,
                        kebabPlural,
                        entityName,
                        // PagedResponse record
                        entityName,
                        entityName);
    }
}
