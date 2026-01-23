# APiGen - Go/Gin Example

This folder will contain a generated API using **Go + Gin + GORM**.

## Equivalent Code (based on Java example)

### Entity (models/product.go)
```go
package models

import (
	"time"

	"gorm.io/gorm"
)

type Product struct {
	ID          int64          `gorm:"primaryKey;autoIncrement" json:"id"`
	Name        string         `gorm:"size:255;not null" json:"name" binding:"required"`
	Description *string        `gorm:"size:1000" json:"description,omitempty"`
	Price       float64        `gorm:"type:decimal(10,2);not null" json:"price" binding:"required,gte=0"`
	Stock       int            `gorm:"not null" json:"stock" binding:"required,gte=0"`
	Sku         string         `gorm:"size:100;not null;unique" json:"sku" binding:"required"`
	ImageUrl    *string        `gorm:"column:image_url;size:500" json:"imageUrl,omitempty"`
	Activo      bool           `gorm:"default:true" json:"activo"`
	CategoryID  *int64         `gorm:"column:category_id" json:"categoryId,omitempty"`
	Category    *Category      `gorm:"foreignKey:CategoryID" json:"category,omitempty"`
	OrderItems  []OrderItem    `gorm:"foreignKey:ProductID" json:"orderItems,omitempty"`
	Reviews     []Review       `gorm:"foreignKey:ProductID" json:"reviews,omitempty"`
	CreatedAt   time.Time      `gorm:"autoCreateTime" json:"createdAt"`
	UpdatedAt   time.Time      `gorm:"autoUpdateTime" json:"updatedAt"`
	CreatedBy   *string        `gorm:"size:100" json:"createdBy,omitempty"`
	UpdatedBy   *string        `gorm:"size:100" json:"updatedBy,omitempty"`
	DeletedAt   gorm.DeletedAt `gorm:"index" json:"-"`
}

func (Product) TableName() string {
	return "products"
}
```

### DTO (dto/product_dto.go)
```go
package dto

type ProductDTO struct {
	ID          int64    `json:"id"`
	Name        string   `json:"name"`
	Description *string  `json:"description,omitempty"`
	Price       float64  `json:"price"`
	Stock       int      `json:"stock"`
	Sku         string   `json:"sku"`
	ImageUrl    *string  `json:"imageUrl,omitempty"`
	Activo      bool     `json:"activo"`
	CategoryID  *int64   `json:"categoryId,omitempty"`
}

type CreateProductDTO struct {
	Name        string   `json:"name" binding:"required,max=255"`
	Description *string  `json:"description,omitempty" binding:"omitempty,max=1000"`
	Price       float64  `json:"price" binding:"required,gte=0"`
	Stock       int      `json:"stock" binding:"required,gte=0"`
	Sku         string   `json:"sku" binding:"required,max=100"`
	ImageUrl    *string  `json:"imageUrl,omitempty" binding:"omitempty,max=500,url"`
	CategoryID  *int64   `json:"categoryId,omitempty"`
}

type UpdateProductDTO struct {
	Name        *string  `json:"name,omitempty" binding:"omitempty,max=255"`
	Description *string  `json:"description,omitempty" binding:"omitempty,max=1000"`
	Price       *float64 `json:"price,omitempty" binding:"omitempty,gte=0"`
	Stock       *int     `json:"stock,omitempty" binding:"omitempty,gte=0"`
	Sku         *string  `json:"sku,omitempty" binding:"omitempty,max=100"`
	ImageUrl    *string  `json:"imageUrl,omitempty" binding:"omitempty,max=500"`
	CategoryID  *int64   `json:"categoryId,omitempty"`
}

type Page[T any] struct {
	Content       []T   `json:"content"`
	TotalElements int64 `json:"totalElements"`
	TotalPages    int   `json:"totalPages"`
	Page          int   `json:"page"`
	Size          int   `json:"size"`
}
```

### Repository (repositories/product_repository.go)
```go
package repositories

import (
	"myapi/models"

	"gorm.io/gorm"
)

type ProductRepository struct {
	*BaseRepository[models.Product]
}

func NewProductRepository(db *gorm.DB) *ProductRepository {
	return &ProductRepository{
		BaseRepository: NewBaseRepository[models.Product](db),
	}
}

func (r *ProductRepository) FindBySku(sku string) (*models.Product, error) {
	var product models.Product
	result := r.db.Where("sku = ? AND activo = ?", sku, true).First(&product)
	if result.Error != nil {
		return nil, result.Error
	}
	return &product, nil
}

// BaseRepository
type BaseRepository[T any] struct {
	db *gorm.DB
}

func NewBaseRepository[T any](db *gorm.DB) *BaseRepository[T] {
	return &BaseRepository[T]{db: db}
}

func (r *BaseRepository[T]) FindAll(page, size int, sort string) ([]T, int64, error) {
	var items []T
	var total int64

	query := r.db.Model(new(T)).Where("activo = ?", true)
	query.Count(&total)

	if sort != "" {
		query = query.Order(sort)
	}

	result := query.Offset(page * size).Limit(size).Find(&items)
	return items, total, result.Error
}

func (r *BaseRepository[T]) FindByID(id int64) (*T, error) {
	var item T
	result := r.db.Where("id = ? AND activo = ?", id, true).First(&item)
	if result.Error != nil {
		return nil, result.Error
	}
	return &item, nil
}

func (r *BaseRepository[T]) Create(item *T) error {
	return r.db.Create(item).Error
}

func (r *BaseRepository[T]) Update(item *T) error {
	return r.db.Save(item).Error
}

func (r *BaseRepository[T]) SoftDelete(id int64) error {
	var item T
	return r.db.Model(&item).Where("id = ?", id).Update("activo", false).Error
}
```

### Service (services/product_service.go)
```go
package services

import (
	"myapi/dto"
	"myapi/models"
	"myapi/repositories"
)

type ProductService struct {
	repo *repositories.ProductRepository
}

func NewProductService(repo *repositories.ProductRepository) *ProductService {
	return &ProductService{repo: repo}
}

func (s *ProductService) FindAll(page, size int, sort string) (*dto.Page[dto.ProductDTO], error) {
	items, total, err := s.repo.FindAll(page, size, sort)
	if err != nil {
		return nil, err
	}

	dtos := make([]dto.ProductDTO, len(items))
	for i, item := range items {
		dtos[i] = s.toDTO(&item)
	}

	totalPages := int(total) / size
	if int(total)%size > 0 {
		totalPages++
	}

	return &dto.Page[dto.ProductDTO]{
		Content:       dtos,
		TotalElements: total,
		TotalPages:    totalPages,
		Page:          page,
		Size:          size,
	}, nil
}

func (s *ProductService) FindByID(id int64) (*dto.ProductDTO, error) {
	product, err := s.repo.FindByID(id)
	if err != nil {
		return nil, err
	}
	result := s.toDTO(product)
	return &result, nil
}

func (s *ProductService) FindBySku(sku string) (*dto.ProductDTO, error) {
	product, err := s.repo.FindBySku(sku)
	if err != nil {
		return nil, err
	}
	result := s.toDTO(product)
	return &result, nil
}

func (s *ProductService) Create(createDTO dto.CreateProductDTO) (*dto.ProductDTO, error) {
	product := &models.Product{
		Name:        createDTO.Name,
		Description: createDTO.Description,
		Price:       createDTO.Price,
		Stock:       createDTO.Stock,
		Sku:         createDTO.Sku,
		ImageUrl:    createDTO.ImageUrl,
		CategoryID:  createDTO.CategoryID,
		Activo:      true,
	}

	if err := s.repo.Create(product); err != nil {
		return nil, err
	}

	result := s.toDTO(product)
	return &result, nil
}

func (s *ProductService) Update(id int64, updateDTO dto.UpdateProductDTO) (*dto.ProductDTO, error) {
	product, err := s.repo.FindByID(id)
	if err != nil {
		return nil, err
	}

	if updateDTO.Name != nil {
		product.Name = *updateDTO.Name
	}
	if updateDTO.Description != nil {
		product.Description = updateDTO.Description
	}
	if updateDTO.Price != nil {
		product.Price = *updateDTO.Price
	}
	if updateDTO.Stock != nil {
		product.Stock = *updateDTO.Stock
	}
	if updateDTO.Sku != nil {
		product.Sku = *updateDTO.Sku
	}
	if updateDTO.ImageUrl != nil {
		product.ImageUrl = updateDTO.ImageUrl
	}
	if updateDTO.CategoryID != nil {
		product.CategoryID = updateDTO.CategoryID
	}

	if err := s.repo.Update(product); err != nil {
		return nil, err
	}

	result := s.toDTO(product)
	return &result, nil
}

func (s *ProductService) SoftDelete(id int64) error {
	return s.repo.SoftDelete(id)
}

func (s *ProductService) toDTO(product *models.Product) dto.ProductDTO {
	return dto.ProductDTO{
		ID:          product.ID,
		Name:        product.Name,
		Description: product.Description,
		Price:       product.Price,
		Stock:       product.Stock,
		Sku:         product.Sku,
		ImageUrl:    product.ImageUrl,
		Activo:      product.Activo,
		CategoryID:  product.CategoryID,
	}
}
```

### Controller (controllers/product_controller.go)
```go
package controllers

import (
	"net/http"
	"strconv"

	"myapi/dto"
	"myapi/services"

	"github.com/gin-gonic/gin"
)

type ProductController struct {
	service *services.ProductService
}

func NewProductController(service *services.ProductService) *ProductController {
	return &ProductController{service: service}
}

func (c *ProductController) RegisterRoutes(router *gin.RouterGroup) {
	products := router.Group("/products")
	{
		products.GET("", c.GetAll)
		products.GET("/:id", c.GetByID)
		products.POST("", c.Create)
		products.PUT("/:id", c.Update)
		products.DELETE("/:id", c.Delete)
	}
}

// @Summary Get all products
// @Tags Products
// @Param page query int false "Page number" default(0)
// @Param size query int false "Page size" default(10)
// @Param sort query string false "Sort field"
// @Success 200 {object} dto.Page[dto.ProductDTO]
// @Router /api/products [get]
func (c *ProductController) GetAll(ctx *gin.Context) {
	page, _ := strconv.Atoi(ctx.DefaultQuery("page", "0"))
	size, _ := strconv.Atoi(ctx.DefaultQuery("size", "10"))
	sort := ctx.Query("sort")

	result, err := c.service.FindAll(page, size, sort)
	if err != nil {
		ctx.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}

	ctx.JSON(http.StatusOK, result)
}

// @Summary Get product by ID
// @Tags Products
// @Param id path int true "Product ID"
// @Success 200 {object} dto.ProductDTO
// @Failure 404 {object} map[string]string
// @Router /api/products/{id} [get]
func (c *ProductController) GetByID(ctx *gin.Context) {
	id, err := strconv.ParseInt(ctx.Param("id"), 10, 64)
	if err != nil {
		ctx.JSON(http.StatusBadRequest, gin.H{"error": "Invalid ID"})
		return
	}

	product, err := c.service.FindByID(id)
	if err != nil {
		ctx.JSON(http.StatusNotFound, gin.H{"error": "Product not found"})
		return
	}

	ctx.JSON(http.StatusOK, product)
}

// @Summary Create a new product
// @Tags Products
// @Accept json
// @Param product body dto.CreateProductDTO true "Product data"
// @Success 201 {object} dto.ProductDTO
// @Failure 400 {object} map[string]string
// @Router /api/products [post]
func (c *ProductController) Create(ctx *gin.Context) {
	var createDTO dto.CreateProductDTO
	if err := ctx.ShouldBindJSON(&createDTO); err != nil {
		ctx.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	product, err := c.service.Create(createDTO)
	if err != nil {
		ctx.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}

	ctx.JSON(http.StatusCreated, product)
}

// @Summary Update a product
// @Tags Products
// @Accept json
// @Param id path int true "Product ID"
// @Param product body dto.UpdateProductDTO true "Product data"
// @Success 200 {object} dto.ProductDTO
// @Failure 404 {object} map[string]string
// @Router /api/products/{id} [put]
func (c *ProductController) Update(ctx *gin.Context) {
	id, err := strconv.ParseInt(ctx.Param("id"), 10, 64)
	if err != nil {
		ctx.JSON(http.StatusBadRequest, gin.H{"error": "Invalid ID"})
		return
	}

	var updateDTO dto.UpdateProductDTO
	if err := ctx.ShouldBindJSON(&updateDTO); err != nil {
		ctx.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	product, err := c.service.Update(id, updateDTO)
	if err != nil {
		ctx.JSON(http.StatusNotFound, gin.H{"error": "Product not found"})
		return
	}

	ctx.JSON(http.StatusOK, product)
}

// @Summary Delete a product (soft delete)
// @Tags Products
// @Param id path int true "Product ID"
// @Success 204
// @Failure 404 {object} map[string]string
// @Router /api/products/{id} [delete]
func (c *ProductController) Delete(ctx *gin.Context) {
	id, err := strconv.ParseInt(ctx.Param("id"), 10, 64)
	if err != nil {
		ctx.JSON(http.StatusBadRequest, gin.H{"error": "Invalid ID"})
		return
	}

	if err := c.service.SoftDelete(id); err != nil {
		ctx.JSON(http.StatusNotFound, gin.H{"error": "Product not found"})
		return
	}

	ctx.Status(http.StatusNoContent)
}
```

## Project Structure
```
my-api/
├── cmd/
│   └── main.go
├── config/
│   └── config.go
├── controllers/
│   └── product_controller.go
├── dto/
│   ├── pagination.go
│   └── product_dto.go
├── models/
│   └── product.go
├── repositories/
│   ├── base_repository.go
│   └── product_repository.go
├── services/
│   └── product_service.go
├── database/
│   └── database.go
├── go.mod
├── go.sum
├── Dockerfile
├── docker-compose.yml
└── README.md
```

## Status
- [ ] Pending implementation in APiGen
