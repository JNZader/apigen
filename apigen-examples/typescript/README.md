# APiGen - TypeScript/NestJS Example

This folder will contain a generated API using **TypeScript + NestJS + TypeORM**.

## Equivalent Code (based on Java example)

### Entity (entities/product.entity.ts)
```typescript
import {
  Entity,
  Column,
  PrimaryGeneratedColumn,
  ManyToOne,
  OneToMany,
  JoinColumn,
  CreateDateColumn,
  UpdateDateColumn,
} from 'typeorm';
import { Category } from './category.entity';
import { OrderItem } from './order-item.entity';
import { Review } from './review.entity';

@Entity('products')
export class Product {
  @PrimaryGeneratedColumn()
  id: number;

  @Column({ length: 255 })
  name: string;

  @Column({ length: 1000, nullable: true })
  description?: string;

  @Column('decimal', { precision: 10, scale: 2 })
  price: number;

  @Column()
  stock: number;

  @Column({ length: 100, unique: true })
  sku: string;

  @Column({ name: 'image_url', length: 500, nullable: true })
  imageUrl?: string;

  @Column({ default: true })
  activo: boolean;

  @CreateDateColumn({ name: 'created_at' })
  createdAt: Date;

  @UpdateDateColumn({ name: 'updated_at' })
  updatedAt: Date;

  @Column({ name: 'created_by', nullable: true })
  createdBy?: string;

  @Column({ name: 'updated_by', nullable: true })
  updatedBy?: string;

  // Relationships
  @ManyToOne(() => Category, (category) => category.products)
  @JoinColumn({ name: 'category_id' })
  category?: Category;

  @OneToMany(() => OrderItem, (orderItem) => orderItem.product, { cascade: true })
  orderItems: OrderItem[];

  @OneToMany(() => Review, (review) => review.product, { cascade: true })
  reviews: Review[];
}
```

### DTO (dto/product.dto.ts)
```typescript
import {
  IsString,
  IsNumber,
  IsOptional,
  IsNotEmpty,
  Min,
  MaxLength,
} from 'class-validator';
import { ApiProperty, ApiPropertyOptional, PartialType } from '@nestjs/swagger';

export class CreateProductDto {
  @ApiProperty({ maxLength: 255 })
  @IsString()
  @IsNotEmpty()
  @MaxLength(255)
  name: string;

  @ApiPropertyOptional({ maxLength: 1000 })
  @IsString()
  @IsOptional()
  @MaxLength(1000)
  description?: string;

  @ApiProperty()
  @IsNumber()
  @Min(0)
  price: number;

  @ApiProperty()
  @IsNumber()
  @Min(0)
  stock: number;

  @ApiProperty({ maxLength: 100 })
  @IsString()
  @IsNotEmpty()
  @MaxLength(100)
  sku: string;

  @ApiPropertyOptional({ maxLength: 500 })
  @IsString()
  @IsOptional()
  @MaxLength(500)
  imageUrl?: string;

  @ApiPropertyOptional()
  @IsNumber()
  @IsOptional()
  categoryId?: number;
}

export class UpdateProductDto extends PartialType(CreateProductDto) {}

export class ProductDto {
  @ApiProperty()
  id: number;

  @ApiProperty()
  name: string;

  @ApiPropertyOptional()
  description?: string;

  @ApiProperty()
  price: number;

  @ApiProperty()
  stock: number;

  @ApiProperty()
  sku: string;

  @ApiPropertyOptional()
  imageUrl?: string;

  @ApiProperty()
  activo: boolean;

  @ApiPropertyOptional()
  categoryId?: number;

  @ApiProperty()
  createdAt: Date;

  @ApiProperty()
  updatedAt: Date;
}
```

### Repository (repositories/product.repository.ts)
```typescript
import { Injectable } from '@nestjs/common';
import { DataSource, Repository } from 'typeorm';
import { Product } from '../entities/product.entity';

@Injectable()
export class ProductRepository extends Repository<Product> {
  constructor(private dataSource: DataSource) {
    super(Product, dataSource.createEntityManager());
  }

  async findBySku(sku: string): Promise<Product | null> {
    return this.findOne({
      where: { sku, activo: true },
    });
  }

  async findAllActive(page: number, size: number, sort?: string): Promise<[Product[], number]> {
    const query = this.createQueryBuilder('product')
      .where('product.activo = :activo', { activo: true });

    if (sort) {
      const [field, order] = sort.split(',');
      query.orderBy(`product.${field}`, order?.toUpperCase() === 'DESC' ? 'DESC' : 'ASC');
    }

    return query
      .skip(page * size)
      .take(size)
      .getManyAndCount();
  }
}
```

### Service (services/product.service.ts)
```typescript
import { Injectable, NotFoundException } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Product } from '../entities/product.entity';
import { ProductRepository } from '../repositories/product.repository';
import { CreateProductDto, UpdateProductDto, ProductDto } from '../dto/product.dto';
import { Page } from '../dto/pagination.dto';

@Injectable()
export class ProductService {
  constructor(
    @InjectRepository(ProductRepository)
    private readonly productRepository: ProductRepository,
  ) {}

  async findAll(page: number, size: number, sort?: string): Promise<Page<ProductDto>> {
    const [items, total] = await this.productRepository.findAllActive(page, size, sort);
    return {
      content: items.map(this.toDto),
      totalElements: total,
      totalPages: Math.ceil(total / size),
      page,
      size,
    };
  }

  async findById(id: number): Promise<ProductDto> {
    const product = await this.productRepository.findOne({
      where: { id, activo: true },
    });
    if (!product) {
      throw new NotFoundException(`Product with ID ${id} not found`);
    }
    return this.toDto(product);
  }

  async create(dto: CreateProductDto): Promise<ProductDto> {
    const product = this.productRepository.create(dto);
    const saved = await this.productRepository.save(product);
    return this.toDto(saved);
  }

  async update(id: number, dto: UpdateProductDto): Promise<ProductDto> {
    const product = await this.productRepository.findOne({ where: { id } });
    if (!product) {
      throw new NotFoundException(`Product with ID ${id} not found`);
    }
    Object.assign(product, dto);
    const updated = await this.productRepository.save(product);
    return this.toDto(updated);
  }

  async softDelete(id: number): Promise<void> {
    const product = await this.productRepository.findOne({ where: { id } });
    if (!product) {
      throw new NotFoundException(`Product with ID ${id} not found`);
    }
    product.activo = false;
    await this.productRepository.save(product);
  }

  private toDto(entity: Product): ProductDto {
    return {
      id: entity.id,
      name: entity.name,
      description: entity.description,
      price: entity.price,
      stock: entity.stock,
      sku: entity.sku,
      imageUrl: entity.imageUrl,
      activo: entity.activo,
      categoryId: entity.category?.id,
      createdAt: entity.createdAt,
      updatedAt: entity.updatedAt,
    };
  }
}
```

### Controller (controllers/product.controller.ts)
```typescript
import {
  Controller,
  Get,
  Post,
  Put,
  Delete,
  Body,
  Param,
  Query,
  ParseIntPipe,
  HttpCode,
  HttpStatus,
} from '@nestjs/common';
import { ApiTags, ApiOperation, ApiResponse, ApiQuery } from '@nestjs/swagger';
import { ProductService } from '../services/product.service';
import { CreateProductDto, UpdateProductDto, ProductDto } from '../dto/product.dto';
import { Page } from '../dto/pagination.dto';

@ApiTags('Products')
@Controller('api/products')
export class ProductController {
  constructor(private readonly productService: ProductService) {}

  @Get()
  @ApiOperation({ summary: 'Get all products' })
  @ApiQuery({ name: 'page', required: false, type: Number })
  @ApiQuery({ name: 'size', required: false, type: Number })
  @ApiQuery({ name: 'sort', required: false, type: String })
  @ApiResponse({ status: 200, description: 'List of products' })
  async findAll(
    @Query('page', new ParseIntPipe({ optional: true })) page = 0,
    @Query('size', new ParseIntPipe({ optional: true })) size = 10,
    @Query('sort') sort?: string,
  ): Promise<Page<ProductDto>> {
    return this.productService.findAll(page, size, sort);
  }

  @Get(':id')
  @ApiOperation({ summary: 'Get product by ID' })
  @ApiResponse({ status: 200, description: 'Product found' })
  @ApiResponse({ status: 404, description: 'Product not found' })
  async findById(@Param('id', ParseIntPipe) id: number): Promise<ProductDto> {
    return this.productService.findById(id);
  }

  @Post()
  @ApiOperation({ summary: 'Create a new product' })
  @ApiResponse({ status: 201, description: 'Product created' })
  async create(@Body() dto: CreateProductDto): Promise<ProductDto> {
    return this.productService.create(dto);
  }

  @Put(':id')
  @ApiOperation({ summary: 'Update a product' })
  @ApiResponse({ status: 200, description: 'Product updated' })
  @ApiResponse({ status: 404, description: 'Product not found' })
  async update(
    @Param('id', ParseIntPipe) id: number,
    @Body() dto: UpdateProductDto,
  ): Promise<ProductDto> {
    return this.productService.update(id, dto);
  }

  @Delete(':id')
  @HttpCode(HttpStatus.NO_CONTENT)
  @ApiOperation({ summary: 'Delete a product (soft delete)' })
  @ApiResponse({ status: 204, description: 'Product deleted' })
  @ApiResponse({ status: 404, description: 'Product not found' })
  async delete(@Param('id', ParseIntPipe) id: number): Promise<void> {
    return this.productService.softDelete(id);
  }
}
```

## Project Structure
```
my-api/
├── src/
│   ├── main.ts
│   ├── app.module.ts
│   ├── entities/
│   │   ├── index.ts
│   │   └── product.entity.ts
│   ├── dto/
│   │   ├── index.ts
│   │   ├── pagination.dto.ts
│   │   └── product.dto.ts
│   ├── repositories/
│   │   ├── index.ts
│   │   └── product.repository.ts
│   ├── services/
│   │   ├── index.ts
│   │   └── product.service.ts
│   └── controllers/
│       ├── index.ts
│       └── product.controller.ts
├── package.json
├── tsconfig.json
├── Dockerfile
├── docker-compose.yml
└── README.md
```

## Status
- [ ] Pending implementation in APiGen
