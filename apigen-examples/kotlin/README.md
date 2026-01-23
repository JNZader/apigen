# APiGen - Kotlin/Spring Boot Example

This folder will contain a generated API using **Kotlin + Spring Boot**.

## Equivalent Code (based on Java example)

### Entity (Product.kt)
```kotlin
package com.example.myapi.products.domain.entity

import com.jnzader.apigen.core.domain.entity.Base
import jakarta.persistence.*
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import org.hibernate.envers.Audited
import java.math.BigDecimal

@Entity
@Table(name = "products")
@Audited
data class Product(
    @NotBlank
    @Column(name = "name", nullable = false)
    var name: String = "",

    @Column(name = "description")
    var description: String? = null,

    @NotNull
    @Column(name = "price", nullable = false)
    var price: BigDecimal = BigDecimal.ZERO,

    @NotNull
    @Column(name = "stock", nullable = false)
    var stock: Int = 0,

    @NotBlank
    @Column(name = "sku", nullable = false, unique = true)
    var sku: String = "",

    @Column(name = "image_url")
    var imageUrl: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    var category: Category? = null,

    @OneToMany(mappedBy = "product", cascade = [CascadeType.PERSIST, CascadeType.MERGE], orphanRemoval = true)
    var orderItems: MutableList<OrderItem> = mutableListOf(),

    @OneToMany(mappedBy = "product", cascade = [CascadeType.PERSIST, CascadeType.MERGE], orphanRemoval = true)
    var reviews: MutableList<Review> = mutableListOf()
) : Base()
```

### DTO (ProductDTO.kt)
```kotlin
package com.example.myapi.products.application.dto

import com.jnzader.apigen.core.application.dto.BaseDTO
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal

data class ProductDTO(
    override val id: Long? = null,
    override val activo: Boolean = true,
    @field:NotBlank val name: String = "",
    val description: String? = null,
    @field:NotNull val price: BigDecimal = BigDecimal.ZERO,
    @field:NotNull val stock: Int = 0,
    @field:NotBlank val sku: String = "",
    val imageUrl: String? = null,
    val categoryId: Long? = null
) : BaseDTO
```

### Repository (ProductRepository.kt)
```kotlin
package com.example.myapi.products.infrastructure.repository

import com.jnzader.apigen.core.domain.repository.BaseRepository
import com.example.myapi.products.domain.entity.Product
import org.springframework.stereotype.Repository

@Repository
interface ProductRepository : BaseRepository<Product, Long> {
    fun findBySku(sku: String): Product?
}
```

### Service (ProductServiceImpl.kt)
```kotlin
package com.example.myapi.products.application.service

import com.jnzader.apigen.core.application.service.BaseServiceImpl
import com.jnzader.apigen.core.application.service.CacheEvictionService
import com.example.myapi.products.domain.entity.Product
import com.example.myapi.products.infrastructure.repository.ProductRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.AuditorAware
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class ProductServiceImpl(
    repository: ProductRepository,
    cacheEvictionService: CacheEvictionService,
    eventPublisher: ApplicationEventPublisher,
    auditorAware: AuditorAware<String>
) : BaseServiceImpl<Product, Long>(repository, cacheEvictionService, eventPublisher, auditorAware),
    ProductService {

    override fun getEntityClass(): Class<Product> = Product::class.java
}
```

### Controller (ProductControllerImpl.kt)
```kotlin
package com.example.myapi.products.infrastructure.controller

import com.jnzader.apigen.core.infrastructure.controller.BaseControllerImpl
import com.example.myapi.products.application.dto.ProductDTO
import com.example.myapi.products.application.mapper.ProductMapper
import com.example.myapi.products.application.service.ProductService
import com.example.myapi.products.domain.entity.Product
import org.springframework.web.bind.annotation.RestController

@RestController
class ProductControllerImpl(
    private val productService: ProductService,
    private val productMapper: ProductMapper
) : BaseControllerImpl<Product, ProductDTO, Long>(productService, productMapper),
    ProductController
```

## Status
- [ ] Pending implementation in APiGen
