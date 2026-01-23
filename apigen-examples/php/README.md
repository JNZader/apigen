# APiGen - PHP/Laravel Example

This folder will contain a generated API using **PHP + Laravel + Eloquent**.

## Equivalent Code (based on Java example)

### Entity/Model (app/Models/Product.php)
```php
<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\SoftDeletes;
use Illuminate\Database\Eloquent\Relations\BelongsTo;
use Illuminate\Database\Eloquent\Relations\HasMany;

class Product extends Model
{
    use SoftDeletes;

    protected $table = 'products';

    protected $fillable = [
        'name',
        'description',
        'price',
        'stock',
        'sku',
        'image_url',
        'category_id',
        'activo',
    ];

    protected $casts = [
        'price' => 'decimal:2',
        'stock' => 'integer',
        'activo' => 'boolean',
        'created_at' => 'datetime',
        'updated_at' => 'datetime',
    ];

    protected $attributes = [
        'activo' => true,
    ];

    // Relationships
    public function category(): BelongsTo
    {
        return $this->belongsTo(Category::class);
    }

    public function orderItems(): HasMany
    {
        return $this->hasMany(OrderItem::class);
    }

    public function reviews(): HasMany
    {
        return $this->hasMany(Review::class);
    }

    // Scopes
    public function scopeActive($query)
    {
        return $query->where('activo', true);
    }
}
```

### DTO/Resource (app/Http/Resources/ProductResource.php)
```php
<?php

namespace App\Http\Resources;

use Illuminate\Http\Resources\Json\JsonResource;

class ProductResource extends JsonResource
{
    public function toArray($request): array
    {
        return [
            'id' => $this->id,
            'name' => $this->name,
            'description' => $this->description,
            'price' => $this->price,
            'stock' => $this->stock,
            'sku' => $this->sku,
            'imageUrl' => $this->image_url,
            'activo' => $this->activo,
            'categoryId' => $this->category_id,
            'createdAt' => $this->created_at?->toISOString(),
            'updatedAt' => $this->updated_at?->toISOString(),
        ];
    }
}
```

### Request Validation (app/Http/Requests/ProductRequest.php)
```php
<?php

namespace App\Http\Requests;

use Illuminate\Foundation\Http\FormRequest;

class CreateProductRequest extends FormRequest
{
    public function authorize(): bool
    {
        return true;
    }

    public function rules(): array
    {
        return [
            'name' => 'required|string|max:255',
            'description' => 'nullable|string|max:1000',
            'price' => 'required|numeric|min:0',
            'stock' => 'required|integer|min:0',
            'sku' => 'required|string|max:100|unique:products,sku',
            'imageUrl' => 'nullable|string|max:500|url',
            'categoryId' => 'nullable|exists:categories,id',
        ];
    }
}

class UpdateProductRequest extends FormRequest
{
    public function authorize(): bool
    {
        return true;
    }

    public function rules(): array
    {
        return [
            'name' => 'sometimes|string|max:255',
            'description' => 'nullable|string|max:1000',
            'price' => 'sometimes|numeric|min:0',
            'stock' => 'sometimes|integer|min:0',
            'sku' => 'sometimes|string|max:100|unique:products,sku,' . $this->route('id'),
            'imageUrl' => 'nullable|string|max:500|url',
            'categoryId' => 'nullable|exists:categories,id',
        ];
    }
}
```

### Repository (app/Repositories/ProductRepository.php)
```php
<?php

namespace App\Repositories;

use App\Models\Product;
use Illuminate\Pagination\LengthAwarePaginator;

class ProductRepository extends BaseRepository
{
    public function __construct(Product $model)
    {
        parent::__construct($model);
    }

    public function findBySku(string $sku): ?Product
    {
        return $this->model
            ->active()
            ->where('sku', $sku)
            ->first();
    }
}

abstract class BaseRepository
{
    protected $model;

    public function __construct($model)
    {
        $this->model = $model;
    }

    public function findAll(int $page = 0, int $size = 10, ?string $sort = null): LengthAwarePaginator
    {
        $query = $this->model->active();

        if ($sort) {
            [$field, $direction] = explode(',', $sort . ',asc');
            $query->orderBy($field, $direction);
        }

        return $query->paginate($size, ['*'], 'page', $page + 1);
    }

    public function findById(int $id)
    {
        return $this->model->active()->find($id);
    }

    public function create(array $data)
    {
        return $this->model->create($data);
    }

    public function update(int $id, array $data)
    {
        $entity = $this->findById($id);
        if ($entity) {
            $entity->update($data);
        }
        return $entity;
    }

    public function softDelete(int $id): bool
    {
        $entity = $this->findById($id);
        if ($entity) {
            $entity->activo = false;
            $entity->save();
            return true;
        }
        return false;
    }
}
```

### Service (app/Services/ProductService.php)
```php
<?php

namespace App\Services;

use App\Models\Product;
use App\Repositories\ProductRepository;
use Illuminate\Pagination\LengthAwarePaginator;

class ProductService
{
    protected ProductRepository $repository;

    public function __construct(ProductRepository $repository)
    {
        $this->repository = $repository;
    }

    public function findAll(int $page, int $size, ?string $sort): LengthAwarePaginator
    {
        return $this->repository->findAll($page, $size, $sort);
    }

    public function findById(int $id): ?Product
    {
        return $this->repository->findById($id);
    }

    public function findBySku(string $sku): ?Product
    {
        return $this->repository->findBySku($sku);
    }

    public function create(array $data): Product
    {
        // Convert camelCase to snake_case for database
        $data = [
            'name' => $data['name'],
            'description' => $data['description'] ?? null,
            'price' => $data['price'],
            'stock' => $data['stock'],
            'sku' => $data['sku'],
            'image_url' => $data['imageUrl'] ?? null,
            'category_id' => $data['categoryId'] ?? null,
        ];

        return $this->repository->create($data);
    }

    public function update(int $id, array $data): ?Product
    {
        $updateData = [];
        if (isset($data['name'])) $updateData['name'] = $data['name'];
        if (isset($data['description'])) $updateData['description'] = $data['description'];
        if (isset($data['price'])) $updateData['price'] = $data['price'];
        if (isset($data['stock'])) $updateData['stock'] = $data['stock'];
        if (isset($data['sku'])) $updateData['sku'] = $data['sku'];
        if (isset($data['imageUrl'])) $updateData['image_url'] = $data['imageUrl'];
        if (isset($data['categoryId'])) $updateData['category_id'] = $data['categoryId'];

        return $this->repository->update($id, $updateData);
    }

    public function softDelete(int $id): bool
    {
        return $this->repository->softDelete($id);
    }
}
```

### Controller (app/Http/Controllers/ProductController.php)
```php
<?php

namespace App\Http\Controllers;

use App\Http\Requests\CreateProductRequest;
use App\Http\Requests\UpdateProductRequest;
use App\Http\Resources\ProductResource;
use App\Services\ProductService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\AnonymousResourceCollection;

/**
 * @OA\Tag(name="Products", description="Product management")
 */
class ProductController extends Controller
{
    protected ProductService $service;

    public function __construct(ProductService $service)
    {
        $this->service = $service;
    }

    /**
     * @OA\Get(
     *     path="/api/products",
     *     tags={"Products"},
     *     summary="Get all products",
     *     @OA\Parameter(name="page", in="query", @OA\Schema(type="integer")),
     *     @OA\Parameter(name="size", in="query", @OA\Schema(type="integer")),
     *     @OA\Parameter(name="sort", in="query", @OA\Schema(type="string")),
     *     @OA\Response(response=200, description="Success")
     * )
     */
    public function index(Request $request): AnonymousResourceCollection
    {
        $page = $request->query('page', 0);
        $size = $request->query('size', 10);
        $sort = $request->query('sort');

        $products = $this->service->findAll($page, $size, $sort);

        return ProductResource::collection($products);
    }

    /**
     * @OA\Get(
     *     path="/api/products/{id}",
     *     tags={"Products"},
     *     summary="Get product by ID",
     *     @OA\Parameter(name="id", in="path", required=true, @OA\Schema(type="integer")),
     *     @OA\Response(response=200, description="Success"),
     *     @OA\Response(response=404, description="Not found")
     * )
     */
    public function show(int $id): JsonResponse
    {
        $product = $this->service->findById($id);

        if (!$product) {
            return response()->json(['message' => 'Product not found'], 404);
        }

        return response()->json(new ProductResource($product));
    }

    /**
     * @OA\Post(
     *     path="/api/products",
     *     tags={"Products"},
     *     summary="Create a new product",
     *     @OA\RequestBody(required=true, @OA\JsonContent(ref="#/components/schemas/CreateProduct")),
     *     @OA\Response(response=201, description="Created")
     * )
     */
    public function store(CreateProductRequest $request): JsonResponse
    {
        $product = $this->service->create($request->validated());

        return response()->json(new ProductResource($product), 201);
    }

    /**
     * @OA\Put(
     *     path="/api/products/{id}",
     *     tags={"Products"},
     *     summary="Update a product",
     *     @OA\Parameter(name="id", in="path", required=true, @OA\Schema(type="integer")),
     *     @OA\RequestBody(required=true, @OA\JsonContent(ref="#/components/schemas/UpdateProduct")),
     *     @OA\Response(response=200, description="Success"),
     *     @OA\Response(response=404, description="Not found")
     * )
     */
    public function update(UpdateProductRequest $request, int $id): JsonResponse
    {
        $product = $this->service->update($id, $request->validated());

        if (!$product) {
            return response()->json(['message' => 'Product not found'], 404);
        }

        return response()->json(new ProductResource($product));
    }

    /**
     * @OA\Delete(
     *     path="/api/products/{id}",
     *     tags={"Products"},
     *     summary="Delete a product (soft delete)",
     *     @OA\Parameter(name="id", in="path", required=true, @OA\Schema(type="integer")),
     *     @OA\Response(response=204, description="Deleted"),
     *     @OA\Response(response=404, description="Not found")
     * )
     */
    public function destroy(int $id): JsonResponse
    {
        $deleted = $this->service->softDelete($id);

        if (!$deleted) {
            return response()->json(['message' => 'Product not found'], 404);
        }

        return response()->json(null, 204);
    }
}
```

## Project Structure
```
my-api/
├── app/
│   ├── Http/
│   │   ├── Controllers/
│   │   │   └── ProductController.php
│   │   ├── Requests/
│   │   │   └── ProductRequest.php
│   │   └── Resources/
│   │       └── ProductResource.php
│   ├── Models/
│   │   └── Product.php
│   ├── Repositories/
│   │   ├── BaseRepository.php
│   │   └── ProductRepository.php
│   └── Services/
│       └── ProductService.php
├── database/
│   └── migrations/
│       └── create_products_table.php
├── routes/
│   └── api.php
├── composer.json
├── Dockerfile
├── docker-compose.yml
└── README.md
```

## Status
- [ ] Pending implementation in APiGen
