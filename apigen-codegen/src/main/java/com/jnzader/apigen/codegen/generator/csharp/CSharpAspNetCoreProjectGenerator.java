package com.jnzader.apigen.codegen.generator.csharp;

import com.jnzader.apigen.codegen.generator.api.Feature;
import com.jnzader.apigen.codegen.generator.api.LanguageTypeMapper;
import com.jnzader.apigen.codegen.generator.api.ProjectConfig;
import com.jnzader.apigen.codegen.generator.api.ProjectGenerator;
import com.jnzader.apigen.codegen.generator.common.ManyToManyRelation;
import com.jnzader.apigen.codegen.generator.csharp.config.CSharpConfigGenerator;
import com.jnzader.apigen.codegen.generator.csharp.controller.CSharpControllerGenerator;
import com.jnzader.apigen.codegen.generator.csharp.dbcontext.CSharpDbContextGenerator;
import com.jnzader.apigen.codegen.generator.csharp.dto.CSharpDTOGenerator;
import com.jnzader.apigen.codegen.generator.csharp.entity.CSharpEntityGenerator;
import com.jnzader.apigen.codegen.generator.csharp.repository.CSharpRepositoryGenerator;
import com.jnzader.apigen.codegen.generator.csharp.service.CSharpServiceGenerator;
import com.jnzader.apigen.codegen.model.SqlForeignKey;
import com.jnzader.apigen.codegen.model.SqlSchema;
import com.jnzader.apigen.codegen.model.SqlTable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Project generator for C#/ASP.NET Core applications.
 *
 * <p>This generator creates complete ASP.NET Core projects from SQL schemas, including:
 *
 * <ul>
 *   <li>Entity Framework Core entity classes with relationships
 *   <li>Record DTOs with validation attributes
 *   <li>AutoMapper mappings
 *   <li>Repository interfaces and implementations
 *   <li>Service interfaces and implementations
 *   <li>REST API Controllers
 *   <li>DbContext configuration
 *   <li>Project configuration files
 * </ul>
 */
public class CSharpAspNetCoreProjectGenerator implements ProjectGenerator {

    private static final String LANGUAGE = "csharp";
    private static final String FRAMEWORK = "aspnetcore";
    private static final String DEFAULT_DOTNET_VERSION = "9.0";
    private static final String DEFAULT_FRAMEWORK_VERSION = "9.0.0";

    private static final Set<Feature> SUPPORTED_FEATURES =
            Set.of(
                    Feature.CRUD,
                    Feature.HATEOAS,
                    Feature.AUDITING,
                    Feature.SOFT_DELETE,
                    Feature.ETAG_CACHING,
                    Feature.CACHING,
                    Feature.FILTERING,
                    Feature.PAGINATION,
                    Feature.OPENAPI,
                    Feature.JWT_AUTH,
                    Feature.OAUTH2,
                    Feature.RATE_LIMITING,
                    Feature.MIGRATIONS,
                    Feature.UNIT_TESTS,
                    Feature.INTEGRATION_TESTS,
                    Feature.DOCKER,
                    Feature.MANY_TO_MANY,
                    Feature.ONE_TO_MANY,
                    Feature.MANY_TO_ONE,
                    Feature.BATCH_OPERATIONS);

    private final CSharpTypeMapper typeMapper = new CSharpTypeMapper();

    @Override
    public String getLanguage() {
        return LANGUAGE;
    }

    @Override
    public String getFramework() {
        return FRAMEWORK;
    }

    @Override
    public String getDisplayName() {
        return "C# / ASP.NET Core 9.x";
    }

    @Override
    public Set<Feature> getSupportedFeatures() {
        return SUPPORTED_FEATURES;
    }

    @Override
    public LanguageTypeMapper getTypeMapper() {
        return typeMapper;
    }

    @Override
    public String getDefaultLanguageVersion() {
        return DEFAULT_DOTNET_VERSION;
    }

    @Override
    public String getDefaultFrameworkVersion() {
        return DEFAULT_FRAMEWORK_VERSION;
    }

    @Override
    public Map<String, String> generate(SqlSchema schema, ProjectConfig config) {
        Map<String, String> files = new LinkedHashMap<>();
        String baseNamespace = config.getBasePackage();

        // Initialize specialized generators
        CSharpEntityGenerator entityGenerator = new CSharpEntityGenerator(baseNamespace);
        CSharpDTOGenerator dtoGenerator = new CSharpDTOGenerator(baseNamespace);
        CSharpRepositoryGenerator repositoryGenerator =
                new CSharpRepositoryGenerator(baseNamespace);
        CSharpServiceGenerator serviceGenerator = new CSharpServiceGenerator(baseNamespace);
        CSharpControllerGenerator controllerGenerator =
                new CSharpControllerGenerator(baseNamespace);
        CSharpDbContextGenerator dbContextGenerator = new CSharpDbContextGenerator(baseNamespace);
        CSharpConfigGenerator configGenerator = new CSharpConfigGenerator(baseNamespace);

        // Collect all relationships for bidirectional mapping
        Map<String, List<SqlSchema.TableRelationship>> relationshipsByTable = new HashMap<>();
        for (SqlSchema.TableRelationship rel : schema.getAllRelationships()) {
            relationshipsByTable
                    .computeIfAbsent(rel.getSourceTable().getName(), k -> new ArrayList<>())
                    .add(rel);
        }

        // Generate code for each entity table
        for (SqlTable table : schema.getEntityTables()) {
            List<SqlSchema.TableRelationship> tableRelations =
                    relationshipsByTable.getOrDefault(table.getName(), Collections.emptyList());

            // Find inverse relationships (where this table is the target)
            List<SqlSchema.TableRelationship> inverseRelations =
                    schema.getAllRelationships().stream()
                            .filter(r -> r.getTargetTable().getName().equals(table.getName()))
                            .filter(r -> !r.getSourceTable().isJunctionTable())
                            .toList();

            // Find many-to-many relationships through junction tables
            List<ManyToManyRelation> manyToManyRelations = findManyToManyRelations(table, schema);

            String entityName = table.getEntityName();
            String moduleName = toPascalCase(table.getModuleName());

            // 1. Generate Entity
            String entityCode =
                    entityGenerator.generate(
                            table, tableRelations, inverseRelations, manyToManyRelations);
            files.put(moduleName + "/Domain/Entities/" + entityName + ".cs", entityCode);

            // 2. Generate DTOs
            String dtoCode = dtoGenerator.generate(table, tableRelations, manyToManyRelations);
            files.put(moduleName + "/Application/DTOs/" + entityName + "Dto.cs", dtoCode);

            // 3. Generate Repository Interface
            String repoInterfaceCode = repositoryGenerator.generateInterface(table);
            files.put(
                    moduleName + "/Domain/Interfaces/I" + entityName + "Repository.cs",
                    repoInterfaceCode);

            // 4. Generate Repository Implementation
            String repoImplCode = repositoryGenerator.generateImpl(table);
            files.put(
                    moduleName + "/Infrastructure/Repositories/" + entityName + "Repository.cs",
                    repoImplCode);

            // 5. Generate Service Interface
            String serviceInterfaceCode = serviceGenerator.generateInterface(table);
            files.put(
                    moduleName + "/Application/Interfaces/I" + entityName + "Service.cs",
                    serviceInterfaceCode);

            // 6. Generate Service Implementation
            String serviceImplCode = serviceGenerator.generateImpl(table);
            files.put(
                    moduleName + "/Application/Services/" + entityName + "Service.cs",
                    serviceImplCode);

            // 7. Generate Controller
            String controllerCode = controllerGenerator.generate(table);
            files.put(
                    moduleName + "/Api/Controllers/" + toPlural(entityName) + "Controller.cs",
                    controllerCode);
        }

        // Generate DbContext
        String dbContextCode = dbContextGenerator.generate(schema);
        files.put("Infrastructure/Persistence/ApplicationDbContext.cs", dbContextCode);

        // Generate base classes
        files.putAll(generateBaseClasses(baseNamespace));

        // Generate configuration files
        files.putAll(configGenerator.generate(schema, config));

        return files;
    }

    @Override
    public List<String> validateConfig(ProjectConfig config) {
        List<String> errors = new ArrayList<>();

        if (config.getBasePackage() == null || config.getBasePackage().isBlank()) {
            errors.add("Base namespace is required for C#/ASP.NET Core projects");
        }

        return errors;
    }

    /** Generates base/common classes used by all entities. */
    private Map<String, String> generateBaseClasses(String baseNamespace) {
        Map<String, String> files = new LinkedHashMap<>();

        // BaseEntity
        files.put("Domain/Common/BaseEntity.cs", generateBaseEntity(baseNamespace));

        // IRepository interface
        files.put("Domain/Interfaces/IRepository.cs", generateIRepository(baseNamespace));

        // Repository base implementation
        files.put("Infrastructure/Repositories/Repository.cs", generateRepository(baseNamespace));

        // IService interface
        files.put("Application/Interfaces/IService.cs", generateIService(baseNamespace));

        // Service base implementation
        files.put("Application/Services/Service.cs", generateService(baseNamespace));

        // PagedResult
        files.put("Application/Common/PagedResult.cs", generatePagedResult(baseNamespace));

        // NotFoundException
        files.put(
                "Application/Exceptions/NotFoundException.cs",
                generateNotFoundException(baseNamespace));

        return files;
    }

    private String generateBaseEntity(String baseNamespace) {
        return
"""
using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace %s.Domain.Common;

/// <summary>
/// Base entity class with common properties for all entities.
/// </summary>
public abstract class BaseEntity
{
    [Key]
    [DatabaseGenerated(DatabaseGeneratedOption.Identity)]
    public long Id { get; set; }

    public bool Estado { get; set; } = true;

    public DateTime CreatedAt { get; set; }

    public DateTime? UpdatedAt { get; set; }

    public string? CreatedBy { get; set; }

    public string? UpdatedBy { get; set; }

    public DateTime? DeletedAt { get; set; }

    public string? DeletedBy { get; set; }
}
"""
                .formatted(baseNamespace);
    }

    private String generateIRepository(String baseNamespace) {
        return
"""
namespace %s.Domain.Interfaces;

/// <summary>
/// Generic repository interface for data access.
/// </summary>
public interface IRepository<TEntity, TId> where TEntity : class
{
    Task<TEntity?> GetByIdAsync(TId id, CancellationToken cancellationToken = default);
    Task<IEnumerable<TEntity>> GetAllAsync(int page, int size, CancellationToken cancellationToken = default);
    Task<int> CountAsync(CancellationToken cancellationToken = default);
    Task<TEntity> AddAsync(TEntity entity, CancellationToken cancellationToken = default);
    Task<TEntity> UpdateAsync(TEntity entity, CancellationToken cancellationToken = default);
    Task DeleteAsync(TEntity entity, CancellationToken cancellationToken = default);
    Task SoftDeleteAsync(TEntity entity, CancellationToken cancellationToken = default);
}
"""
                .formatted(baseNamespace);
    }

    private String generateRepository(String baseNamespace) {
        return
"""
using Microsoft.EntityFrameworkCore;
using %s.Domain.Common;
using %s.Domain.Interfaces;
using %s.Infrastructure.Persistence;

namespace %s.Infrastructure.Repositories;

/// <summary>
/// Generic repository implementation using Entity Framework Core.
/// </summary>
public class Repository<TEntity, TId> : IRepository<TEntity, TId>
    where TEntity : BaseEntity
{
    protected readonly ApplicationDbContext Context;
    protected readonly DbSet<TEntity> DbSet;

    public Repository(ApplicationDbContext context)
    {
        Context = context;
        DbSet = context.Set<TEntity>();
    }

    public virtual async Task<TEntity?> GetByIdAsync(TId id, CancellationToken cancellationToken = default)
    {
        return await DbSet.FindAsync(new object?[] { id }, cancellationToken);
    }

    public virtual async Task<IEnumerable<TEntity>> GetAllAsync(int page, int size, CancellationToken cancellationToken = default)
    {
        return await DbSet
            .Skip(page * size)
            .Take(size)
            .ToListAsync(cancellationToken);
    }

    public virtual async Task<int> CountAsync(CancellationToken cancellationToken = default)
    {
        return await DbSet.CountAsync(cancellationToken);
    }

    public virtual async Task<TEntity> AddAsync(TEntity entity, CancellationToken cancellationToken = default)
    {
        await DbSet.AddAsync(entity, cancellationToken);
        await Context.SaveChangesAsync(cancellationToken);
        return entity;
    }

    public virtual async Task<TEntity> UpdateAsync(TEntity entity, CancellationToken cancellationToken = default)
    {
        DbSet.Update(entity);
        await Context.SaveChangesAsync(cancellationToken);
        return entity;
    }

    public virtual async Task DeleteAsync(TEntity entity, CancellationToken cancellationToken = default)
    {
        DbSet.Remove(entity);
        await Context.SaveChangesAsync(cancellationToken);
    }

    public virtual async Task SoftDeleteAsync(TEntity entity, CancellationToken cancellationToken = default)
    {
        entity.Estado = false;
        entity.DeletedAt = DateTime.UtcNow;
        await UpdateAsync(entity, cancellationToken);
    }
}
"""
                .formatted(baseNamespace, baseNamespace, baseNamespace, baseNamespace);
    }

    private String generateIService(String baseNamespace) {
        return
"""
using %s.Application.Common;

namespace %s.Application.Interfaces;

/// <summary>
/// Generic service interface for business operations.
/// </summary>
public interface IService<TDto, TCreateDto, TUpdateDto, TId>
{
    Task<TDto> GetByIdAsync(TId id, CancellationToken cancellationToken = default);
    Task<PagedResult<TDto>> GetAllAsync(int page, int size, CancellationToken cancellationToken = default);
    Task<TDto> CreateAsync(TCreateDto dto, CancellationToken cancellationToken = default);
    Task<TDto> UpdateAsync(TId id, TUpdateDto dto, CancellationToken cancellationToken = default);
    Task DeleteAsync(TId id, CancellationToken cancellationToken = default);
}
"""
                .formatted(baseNamespace, baseNamespace);
    }

    private String generateService(String baseNamespace) {
        return
"""
using AutoMapper;
using %s.Application.Common;
using %s.Application.Exceptions;
using %s.Application.Interfaces;
using %s.Domain.Common;
using %s.Domain.Interfaces;

namespace %s.Application.Services;

/// <summary>
/// Generic service implementation with CRUD operations.
/// </summary>
public abstract class Service<TEntity, TDto, TCreateDto, TUpdateDto, TId> : IService<TDto, TCreateDto, TUpdateDto, TId>
    where TEntity : BaseEntity
{
    protected readonly IRepository<TEntity, TId> Repository;
    protected readonly IMapper Mapper;

    protected Service(IRepository<TEntity, TId> repository, IMapper mapper)
    {
        Repository = repository;
        Mapper = mapper;
    }

    public virtual async Task<TDto> GetByIdAsync(TId id, CancellationToken cancellationToken = default)
    {
        var entity = await Repository.GetByIdAsync(id, cancellationToken);
        if (entity is null)
        {
            throw new NotFoundException(typeof(TEntity).Name, id!);
        }
        return Mapper.Map<TDto>(entity);
    }

    public virtual async Task<PagedResult<TDto>> GetAllAsync(int page, int size, CancellationToken cancellationToken = default)
    {
        var entities = await Repository.GetAllAsync(page, size, cancellationToken);
        var totalCount = await Repository.CountAsync(cancellationToken);

        return new PagedResult<TDto>
        {
            Content = Mapper.Map<IEnumerable<TDto>>(entities),
            Page = page,
            Size = size,
            TotalElements = totalCount,
            TotalPages = (int)Math.Ceiling(totalCount / (double)size)
        };
    }

    public virtual async Task<TDto> CreateAsync(TCreateDto dto, CancellationToken cancellationToken = default)
    {
        var entity = Mapper.Map<TEntity>(dto);
        var created = await Repository.AddAsync(entity, cancellationToken);
        return Mapper.Map<TDto>(created);
    }

    public virtual async Task<TDto> UpdateAsync(TId id, TUpdateDto dto, CancellationToken cancellationToken = default)
    {
        var entity = await Repository.GetByIdAsync(id, cancellationToken);
        if (entity is null)
        {
            throw new NotFoundException(typeof(TEntity).Name, id!);
        }

        Mapper.Map(dto, entity);
        var updated = await Repository.UpdateAsync(entity, cancellationToken);
        return Mapper.Map<TDto>(updated);
    }

    public virtual async Task DeleteAsync(TId id, CancellationToken cancellationToken = default)
    {
        var entity = await Repository.GetByIdAsync(id, cancellationToken);
        if (entity is null)
        {
            throw new NotFoundException(typeof(TEntity).Name, id!);
        }

        await Repository.SoftDeleteAsync(entity, cancellationToken);
    }
}
"""
                .formatted(
                        baseNamespace,
                        baseNamespace,
                        baseNamespace,
                        baseNamespace,
                        baseNamespace,
                        baseNamespace);
    }

    private String generatePagedResult(String baseNamespace) {
        return
"""
namespace %s.Application.Common;

/// <summary>
/// Represents a paginated result set.
/// </summary>
public class PagedResult<T>
{
    public IEnumerable<T> Content { get; set; } = Enumerable.Empty<T>();
    public int Page { get; set; }
    public int Size { get; set; }
    public long TotalElements { get; set; }
    public int TotalPages { get; set; }

    public bool HasPrevious => Page > 0;
    public bool HasNext => Page < TotalPages - 1;
}
"""
                .formatted(baseNamespace);
    }

    private String generateNotFoundException(String baseNamespace) {
        return
"""
namespace %s.Application.Exceptions;

/// <summary>
/// Exception thrown when an entity is not found.
/// </summary>
public class NotFoundException : Exception
{
    public NotFoundException(string entityName, object id)
        : base($"{entityName} with id '{id}' was not found.")
    {
    }
}
"""
                .formatted(baseNamespace);
    }

    /** Finds many-to-many relationships for a table through junction tables. */
    private List<ManyToManyRelation> findManyToManyRelations(SqlTable table, SqlSchema schema) {
        List<ManyToManyRelation> relations = new ArrayList<>();

        for (SqlTable junctionTable : schema.getJunctionTables()) {
            List<SqlForeignKey> fks = junctionTable.getForeignKeys();
            if (fks.size() != 2) continue;

            SqlForeignKey fk1 = fks.get(0);
            SqlForeignKey fk2 = fks.get(1);

            SqlForeignKey thisFk = null;
            SqlForeignKey otherFk = null;

            if (fk1.getReferencedTable().equalsIgnoreCase(table.getName())) {
                thisFk = fk1;
                otherFk = fk2;
            } else if (fk2.getReferencedTable().equalsIgnoreCase(table.getName())) {
                thisFk = fk2;
                otherFk = fk1;
            }

            if (thisFk != null && otherFk != null) {
                SqlTable otherTable = schema.getTableByName(otherFk.getReferencedTable());
                if (otherTable != null) {
                    relations.add(
                            new ManyToManyRelation(
                                    junctionTable.getName(),
                                    thisFk.getColumnName(),
                                    otherFk.getColumnName(),
                                    otherTable));
                }
            }
        }

        return relations;
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

    private String toPlural(String name) {
        if (name.endsWith("y")) {
            return name.substring(0, name.length() - 1) + "ies";
        } else if (name.endsWith("s") || name.endsWith("x") || name.endsWith("ch")) {
            return name + "es";
        }
        return name + "s";
    }
}
