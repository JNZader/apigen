# APiGen - C#/ASP.NET Core Example

This folder will contain a generated API using **C# + ASP.NET Core + Entity Framework Core**.

## Equivalent Code (based on Java example)

### Entity (Entities/Product.cs)
```csharp
using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace MyApi.Entities;

[Table("products")]
public class Product : BaseEntity
{
    [Required]
    [MaxLength(255)]
    [Column("name")]
    public string Name { get; set; } = string.Empty;

    [MaxLength(1000)]
    [Column("description")]
    public string? Description { get; set; }

    [Required]
    [Column("price", TypeName = "decimal(10,2)")]
    public decimal Price { get; set; }

    [Required]
    [Column("stock")]
    public int Stock { get; set; }

    [Required]
    [MaxLength(100)]
    [Column("sku")]
    public string Sku { get; set; } = string.Empty;

    [MaxLength(500)]
    [Column("image_url")]
    public string? ImageUrl { get; set; }

    // Relationships
    [Column("category_id")]
    public long? CategoryId { get; set; }

    [ForeignKey(nameof(CategoryId))]
    public virtual Category? Category { get; set; }

    public virtual ICollection<OrderItem> OrderItems { get; set; } = new List<OrderItem>();
    public virtual ICollection<Review> Reviews { get; set; } = new List<Review>();
}

public abstract class BaseEntity
{
    [Key]
    [Column("id")]
    public long Id { get; set; }

    [Column("activo")]
    public bool Activo { get; set; } = true;

    [Column("created_at")]
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

    [Column("updated_at")]
    public DateTime UpdatedAt { get; set; } = DateTime.UtcNow;

    [MaxLength(100)]
    [Column("created_by")]
    public string? CreatedBy { get; set; }

    [MaxLength(100)]
    [Column("updated_by")]
    public string? UpdatedBy { get; set; }
}
```

### DTO (DTOs/ProductDto.cs)
```csharp
using System.ComponentModel.DataAnnotations;

namespace MyApi.DTOs;

public record ProductDto(
    long Id,
    string Name,
    string? Description,
    decimal Price,
    int Stock,
    string Sku,
    string? ImageUrl,
    bool Activo,
    long? CategoryId,
    DateTime CreatedAt,
    DateTime UpdatedAt
);

public record CreateProductDto(
    [Required][MaxLength(255)] string Name,
    [MaxLength(1000)] string? Description,
    [Required][Range(0, double.MaxValue)] decimal Price,
    [Required][Range(0, int.MaxValue)] int Stock,
    [Required][MaxLength(100)] string Sku,
    [MaxLength(500)] string? ImageUrl,
    long? CategoryId
);

public record UpdateProductDto(
    [MaxLength(255)] string? Name,
    [MaxLength(1000)] string? Description,
    [Range(0, double.MaxValue)] decimal? Price,
    [Range(0, int.MaxValue)] int? Stock,
    [MaxLength(100)] string? Sku,
    [MaxLength(500)] string? ImageUrl,
    long? CategoryId
);
```

### Repository (Repositories/ProductRepository.cs)
```csharp
using Microsoft.EntityFrameworkCore;
using MyApi.Data;
using MyApi.Entities;

namespace MyApi.Repositories;

public interface IProductRepository : IBaseRepository<Product>
{
    Task<Product?> FindBySkuAsync(string sku);
}

public class ProductRepository : BaseRepository<Product>, IProductRepository
{
    public ProductRepository(ApplicationDbContext context) : base(context)
    {
    }

    public async Task<Product?> FindBySkuAsync(string sku)
    {
        return await _dbSet
            .Where(p => p.Sku == sku && p.Activo)
            .FirstOrDefaultAsync();
    }
}

public interface IBaseRepository<T> where T : BaseEntity
{
    Task<(IEnumerable<T> Items, int TotalCount)> FindAllAsync(int page, int size, string? sort = null);
    Task<T?> FindByIdAsync(long id);
    Task<T> CreateAsync(T entity);
    Task<T> UpdateAsync(T entity);
    Task SoftDeleteAsync(long id);
}

public class BaseRepository<T> : IBaseRepository<T> where T : BaseEntity
{
    protected readonly ApplicationDbContext _context;
    protected readonly DbSet<T> _dbSet;

    public BaseRepository(ApplicationDbContext context)
    {
        _context = context;
        _dbSet = context.Set<T>();
    }

    public async Task<(IEnumerable<T> Items, int TotalCount)> FindAllAsync(int page, int size, string? sort = null)
    {
        var query = _dbSet.Where(e => e.Activo);
        var totalCount = await query.CountAsync();

        if (!string.IsNullOrEmpty(sort))
        {
            var parts = sort.Split(',');
            var field = parts[0];
            var desc = parts.Length > 1 && parts[1].Equals("desc", StringComparison.OrdinalIgnoreCase);
            // Dynamic ordering would go here
        }

        var items = await query
            .Skip(page * size)
            .Take(size)
            .ToListAsync();

        return (items, totalCount);
    }

    public async Task<T?> FindByIdAsync(long id)
    {
        return await _dbSet.FirstOrDefaultAsync(e => e.Id == id && e.Activo);
    }

    public async Task<T> CreateAsync(T entity)
    {
        _dbSet.Add(entity);
        await _context.SaveChangesAsync();
        return entity;
    }

    public async Task<T> UpdateAsync(T entity)
    {
        entity.UpdatedAt = DateTime.UtcNow;
        _context.Entry(entity).State = EntityState.Modified;
        await _context.SaveChangesAsync();
        return entity;
    }

    public async Task SoftDeleteAsync(long id)
    {
        var entity = await FindByIdAsync(id);
        if (entity != null)
        {
            entity.Activo = false;
            entity.UpdatedAt = DateTime.UtcNow;
            await _context.SaveChangesAsync();
        }
    }
}
```

### Service (Services/ProductService.cs)
```csharp
using MyApi.DTOs;
using MyApi.Entities;
using MyApi.Repositories;

namespace MyApi.Services;

public interface IProductService : IBaseService<Product, ProductDto, CreateProductDto, UpdateProductDto>
{
    Task<ProductDto?> FindBySkuAsync(string sku);
}

public class ProductService : BaseService<Product, ProductDto, CreateProductDto, UpdateProductDto>, IProductService
{
    private readonly IProductRepository _productRepository;

    public ProductService(IProductRepository repository) : base(repository)
    {
        _productRepository = repository;
    }

    public async Task<ProductDto?> FindBySkuAsync(string sku)
    {
        var product = await _productRepository.FindBySkuAsync(sku);
        return product != null ? ToDto(product) : null;
    }

    protected override ProductDto ToDto(Product entity) => new(
        entity.Id,
        entity.Name,
        entity.Description,
        entity.Price,
        entity.Stock,
        entity.Sku,
        entity.ImageUrl,
        entity.Activo,
        entity.CategoryId,
        entity.CreatedAt,
        entity.UpdatedAt
    );

    protected override Product ToEntity(CreateProductDto dto) => new()
    {
        Name = dto.Name,
        Description = dto.Description,
        Price = dto.Price,
        Stock = dto.Stock,
        Sku = dto.Sku,
        ImageUrl = dto.ImageUrl,
        CategoryId = dto.CategoryId
    };

    protected override void UpdateEntity(Product entity, UpdateProductDto dto)
    {
        if (dto.Name != null) entity.Name = dto.Name;
        if (dto.Description != null) entity.Description = dto.Description;
        if (dto.Price.HasValue) entity.Price = dto.Price.Value;
        if (dto.Stock.HasValue) entity.Stock = dto.Stock.Value;
        if (dto.Sku != null) entity.Sku = dto.Sku;
        if (dto.ImageUrl != null) entity.ImageUrl = dto.ImageUrl;
        if (dto.CategoryId.HasValue) entity.CategoryId = dto.CategoryId;
    }
}
```

### Controller (Controllers/ProductController.cs)
```csharp
using Microsoft.AspNetCore.Mvc;
using MyApi.DTOs;
using MyApi.Services;

namespace MyApi.Controllers;

[ApiController]
[Route("api/products")]
[Produces("application/json")]
public class ProductController : ControllerBase
{
    private readonly IProductService _productService;

    public ProductController(IProductService productService)
    {
        _productService = productService;
    }

    /// <summary>
    /// Get all products with pagination
    /// </summary>
    [HttpGet]
    [ProducesResponseType(typeof(Page<ProductDto>), StatusCodes.Status200OK)]
    public async Task<ActionResult<Page<ProductDto>>> GetAll(
        [FromQuery] int page = 0,
        [FromQuery] int size = 10,
        [FromQuery] string? sort = null)
    {
        return Ok(await _productService.FindAllAsync(page, size, sort));
    }

    /// <summary>
    /// Get product by ID
    /// </summary>
    [HttpGet("{id}")]
    [ProducesResponseType(typeof(ProductDto), StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<ActionResult<ProductDto>> GetById(long id)
    {
        var product = await _productService.FindByIdAsync(id);
        if (product == null)
            return NotFound();
        return Ok(product);
    }

    /// <summary>
    /// Create a new product
    /// </summary>
    [HttpPost]
    [ProducesResponseType(typeof(ProductDto), StatusCodes.Status201Created)]
    [ProducesResponseType(StatusCodes.Status400BadRequest)]
    public async Task<ActionResult<ProductDto>> Create([FromBody] CreateProductDto dto)
    {
        var product = await _productService.CreateAsync(dto);
        return CreatedAtAction(nameof(GetById), new { id = product.Id }, product);
    }

    /// <summary>
    /// Update a product
    /// </summary>
    [HttpPut("{id}")]
    [ProducesResponseType(typeof(ProductDto), StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<ActionResult<ProductDto>> Update(long id, [FromBody] UpdateProductDto dto)
    {
        var product = await _productService.UpdateAsync(id, dto);
        if (product == null)
            return NotFound();
        return Ok(product);
    }

    /// <summary>
    /// Delete a product (soft delete)
    /// </summary>
    [HttpDelete("{id}")]
    [ProducesResponseType(StatusCodes.Status204NoContent)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<IActionResult> Delete(long id)
    {
        await _productService.SoftDeleteAsync(id);
        return NoContent();
    }
}
```

## Project Structure
```
MyApi/
├── Controllers/
│   └── ProductController.cs
├── Data/
│   └── ApplicationDbContext.cs
├── DTOs/
│   ├── Pagination.cs
│   └── ProductDto.cs
├── Entities/
│   ├── BaseEntity.cs
│   └── Product.cs
├── Repositories/
│   ├── BaseRepository.cs
│   └── ProductRepository.cs
├── Services/
│   ├── BaseService.cs
│   └── ProductService.cs
├── Program.cs
├── appsettings.json
├── MyApi.csproj
├── Dockerfile
├── docker-compose.yml
└── README.md
```

## Status
- [ ] Pending implementation in APiGen
