# APiGen - Python/FastAPI Example

This folder will contain a generated API using **Python + FastAPI + SQLAlchemy**.

## Equivalent Code (based on Java example)

### Entity (models/product.py)
```python
from sqlalchemy import Column, String, Integer, Numeric, ForeignKey, Boolean
from sqlalchemy.orm import relationship, Mapped, mapped_column
from typing import Optional, List
from decimal import Decimal
from .base import Base, AuditMixin, SoftDeleteMixin

class Product(Base, AuditMixin, SoftDeleteMixin):
    __tablename__ = "products"

    id: Mapped[int] = mapped_column(primary_key=True, autoincrement=True)
    name: Mapped[str] = mapped_column(String(255), nullable=False)
    description: Mapped[Optional[str]] = mapped_column(String(1000))
    price: Mapped[Decimal] = mapped_column(Numeric(10, 2), nullable=False)
    stock: Mapped[int] = mapped_column(Integer, nullable=False)
    sku: Mapped[str] = mapped_column(String(100), nullable=False, unique=True)
    image_url: Mapped[Optional[str]] = mapped_column(String(500))

    # Relationships
    category_id: Mapped[Optional[int]] = mapped_column(ForeignKey("categories.id"))
    category: Mapped[Optional["Category"]] = relationship(back_populates="products")
    order_items: Mapped[List["OrderItem"]] = relationship(back_populates="product", cascade="all, delete-orphan")
    reviews: Mapped[List["Review"]] = relationship(back_populates="product", cascade="all, delete-orphan")
```

### DTO/Schema (schemas/product.py)
```python
from pydantic import BaseModel, Field
from typing import Optional
from decimal import Decimal

class ProductBase(BaseModel):
    name: str = Field(..., min_length=1, max_length=255)
    description: Optional[str] = Field(None, max_length=1000)
    price: Decimal = Field(..., ge=0)
    stock: int = Field(..., ge=0)
    sku: str = Field(..., min_length=1, max_length=100)
    image_url: Optional[str] = Field(None, max_length=500)
    category_id: Optional[int] = None

class ProductCreate(ProductBase):
    pass

class ProductUpdate(ProductBase):
    name: Optional[str] = Field(None, min_length=1, max_length=255)
    price: Optional[Decimal] = Field(None, ge=0)
    stock: Optional[int] = Field(None, ge=0)
    sku: Optional[str] = Field(None, min_length=1, max_length=100)

class ProductDTO(ProductBase):
    id: int
    activo: bool = True

    class Config:
        from_attributes = True
```

### Repository (repositories/product_repository.py)
```python
from typing import Optional
from sqlalchemy.orm import Session
from .base_repository import BaseRepository
from ..models.product import Product

class ProductRepository(BaseRepository[Product]):
    def __init__(self, db: Session):
        super().__init__(Product, db)

    def find_by_sku(self, sku: str) -> Optional[Product]:
        return self.db.query(Product).filter(
            Product.sku == sku,
            Product.activo == True
        ).first()
```

### Service (services/product_service.py)
```python
from typing import Optional
from sqlalchemy.orm import Session
from .base_service import BaseService
from ..models.product import Product
from ..repositories.product_repository import ProductRepository
from ..schemas.product import ProductCreate, ProductUpdate

class ProductService(BaseService[Product, ProductCreate, ProductUpdate]):
    def __init__(self, db: Session):
        self.repository = ProductRepository(db)
        super().__init__(self.repository)

    def find_by_sku(self, sku: str) -> Optional[Product]:
        return self.repository.find_by_sku(sku)
```

### Controller/Router (routers/product_router.py)
```python
from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.orm import Session
from typing import List, Optional
from ..database import get_db
from ..schemas.product import ProductDTO, ProductCreate, ProductUpdate
from ..schemas.pagination import Page
from ..services.product_service import ProductService

router = APIRouter(prefix="/api/products", tags=["Products"])

@router.get("", response_model=Page[ProductDTO])
async def get_all(
    page: int = Query(0, ge=0),
    size: int = Query(10, ge=1, le=100),
    sort: Optional[str] = None,
    db: Session = Depends(get_db)
):
    service = ProductService(db)
    return service.find_all_paginated(page, size, sort)

@router.get("/{id}", response_model=ProductDTO)
async def get_by_id(id: int, db: Session = Depends(get_db)):
    service = ProductService(db)
    product = service.find_by_id(id)
    if not product:
        raise HTTPException(status_code=404, detail="Product not found")
    return product

@router.post("", response_model=ProductDTO, status_code=201)
async def create(product: ProductCreate, db: Session = Depends(get_db)):
    service = ProductService(db)
    return service.create(product)

@router.put("/{id}", response_model=ProductDTO)
async def update(id: int, product: ProductUpdate, db: Session = Depends(get_db)):
    service = ProductService(db)
    return service.update(id, product)

@router.delete("/{id}", status_code=204)
async def delete(id: int, db: Session = Depends(get_db)):
    service = ProductService(db)
    service.soft_delete(id)
```

## Project Structure
```
my-api/
├── app/
│   ├── __init__.py
│   ├── main.py
│   ├── database.py
│   ├── models/
│   │   ├── __init__.py
│   │   ├── base.py
│   │   └── product.py
│   ├── schemas/
│   │   ├── __init__.py
│   │   ├── pagination.py
│   │   └── product.py
│   ├── repositories/
│   │   ├── __init__.py
│   │   ├── base_repository.py
│   │   └── product_repository.py
│   ├── services/
│   │   ├── __init__.py
│   │   ├── base_service.py
│   │   └── product_service.py
│   └── routers/
│       ├── __init__.py
│       └── product_router.py
├── requirements.txt
├── Dockerfile
├── docker-compose.yml
└── README.md
```

## Status
- [ ] Pending implementation in APiGen
