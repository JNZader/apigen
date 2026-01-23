package com.example.myapi.products.application.service;

import com.jnzader.apigen.core.application.service.CacheEvictionService;
import com.jnzader.apigen.core.application.util.Result;
import com.example.myapi.products.domain.entity.Product;
import com.example.myapi.products.infrastructure.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import jakarta.persistence.EntityManager;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.AuditorAware;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService Tests")
class ProductServiceImplTest {

    @Mock
    private ProductRepository repository;

    @Mock
    private CacheEvictionService cacheEvictionService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private AuditorAware<String> auditorAware;

    @Mock
    private EntityManager entityManager;

    private ProductServiceImpl service;

    private Product product;

    @BeforeEach
    void setUp() {
        service = new ProductServiceImpl(repository, cacheEvictionService, eventPublisher, auditorAware);
        // Inject mock EntityManager for batch operations (saveAll uses flush/clear)
        ReflectionTestUtils.setField(service, "entityManager", entityManager);
        product = new Product();
        product.setId(1L);
        product.setEstado(true);
    }

    @Nested
    @DisplayName("Find Operations")
    class FindOperations {

        @Test
        @DisplayName("Should find Product by ID successfully")
        void shouldFindProductById() {
            when(repository.findById(1L)).thenReturn(Optional.of(product));

            Result<Product, Exception> result = service.findById(1L);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.orElseThrow()).isEqualTo(product);
            verify(repository).findById(1L);
        }

        @Test
        @DisplayName("Should return failure when Product not found by ID")
        void shouldReturnFailureWhenNotFound() {
            when(repository.findById(anyLong())).thenReturn(Optional.empty());

            Result<Product, Exception> result = service.findById(999L);

            assertThat(result.isFailure()).isTrue();
            verify(repository).findById(999L);
        }

        @Test
        @DisplayName("Should find all Product with pagination")
        void shouldFindAllWithPagination() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Product> page = new PageImpl<>(List.of(product));
            when(repository.findAll(pageable)).thenReturn(page);

            Result<Page<Product>, Exception> result = service.findAll(pageable);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.orElseThrow().getContent()).hasSize(1);
            verify(repository).findAll(pageable);
        }

        @Test
        @DisplayName("Should find all active Product with pagination")
        void shouldFindAllActiveWithPagination() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Product> page = new PageImpl<>(List.of(product));
            when(repository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

            Result<Page<Product>, Exception> result = service.findAllActive(pageable);

            assertThat(result.isSuccess()).isTrue();
            verify(repository).findAll(any(Specification.class), eq(pageable));
        }

        @Test
        @DisplayName("Should check if Product exists by ID")
        void shouldCheckExistsById() {
            when(repository.existsById(1L)).thenReturn(true);

            Result<Boolean, Exception> result = service.existsById(1L);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.orElseThrow()).isTrue();
            verify(repository).existsById(1L);
        }

        @Test
        @DisplayName("Should count all Product")
        void shouldCountAll() {
            when(repository.count()).thenReturn(5L);

            Result<Long, Exception> result = service.count();

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.orElseThrow()).isEqualTo(5L);
            verify(repository).count();
        }

        @Test
        @DisplayName("Should count active Product")
        void shouldCountActive() {
            when(repository.count(any(Specification.class))).thenReturn(3L);

            Result<Long, Exception> result = service.countActive();

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.orElseThrow()).isEqualTo(3L);
        }
    }

    @Nested
    @DisplayName("Save Operations")
    class SaveOperations {

        @Test
        @DisplayName("Should save new Product successfully")
        void shouldSaveNewProduct() {
            Product newProduct = new Product();
            when(repository.save(any(Product.class))).thenReturn(product);

            Result<Product, Exception> result = service.save(newProduct);

            assertThat(result.isSuccess()).isTrue();
            verify(repository).save(any(Product.class));
        }

        @Test
        @DisplayName("Should update existing Product")
        void shouldUpdateExistingProduct() {
            when(repository.findById(1L)).thenReturn(Optional.of(product));
            when(repository.save(any(Product.class))).thenReturn(product);

            Result<Product, Exception> result = service.update(1L, product);

            assertThat(result.isSuccess()).isTrue();
            verify(repository).findById(1L);
            verify(repository).save(any(Product.class));
            verify(cacheEvictionService).evictListsByEntityName(anyString());
        }

        @Test
        @DisplayName("Should partial update Product")
        void shouldPartialUpdateProduct() {
            when(repository.findById(1L)).thenReturn(Optional.of(product));
            when(repository.save(any(Product.class))).thenReturn(product);

            Result<Product, Exception> result = service.partialUpdate(1L, product);

            assertThat(result.isSuccess()).isTrue();
            verify(repository).findById(1L);
            verify(repository).save(any(Product.class));
        }

        @Test
        @DisplayName("Should save all Product in batch")
        void shouldSaveAllInBatch() {
            List<Product> entities = List.of(product, new Product());
            when(repository.saveAll(anyList())).thenReturn(entities);

            Result<List<Product>, Exception> result = service.saveAll(entities);

            assertThat(result.isSuccess()).isTrue();
            verify(repository).saveAll(anyList());
        }
    }

    @Nested
    @DisplayName("Delete Operations")
    class DeleteOperations {

        @Test
        @DisplayName("Should soft delete Product")
        void shouldSoftDeleteProduct() {
            when(auditorAware.getCurrentAuditor()).thenReturn(Optional.of("testUser"));
            when(repository.findById(1L)).thenReturn(Optional.of(product));
            when(repository.save(any(Product.class))).thenReturn(product);

            Result<Void, Exception> result = service.softDelete(1L);

            assertThat(result.isSuccess()).isTrue();
            verify(repository).findById(1L);
            verify(repository).save(any(Product.class));
            verify(cacheEvictionService).evictListsByEntityName(anyString());
        }

        @Test
        @DisplayName("Should hard delete Product")
        void shouldHardDeleteProduct() {
            // Use lenient() to avoid strict stubbing issues with Mockito 5
            when(repository.hardDeleteById(1L)).thenReturn(1);
            lenient().doNothing().when(eventPublisher).publishEvent(any());
            lenient().doNothing().when(cacheEvictionService).evictListsByEntityName(anyString());
            lenient().doNothing().when(cacheEvictionService).evictCounts(anyString());

            Result<Void, Exception> result = service.hardDelete(1L);

            assertThat(result.isSuccess()).isTrue();
            verify(repository).hardDeleteById(1L);
        }

        @Test
        @DisplayName("Should soft delete all Product in batch")
        void shouldSoftDeleteAllInBatch() {
            when(auditorAware.getCurrentAuditor()).thenReturn(Optional.of("testUser"));
            when(repository.softDeleteAllByIds(anyList(), any(), anyString())).thenReturn(2);

            Result<Integer, Exception> result = service.softDeleteAll(List.of(1L, 2L));

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.orElseThrow()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Restore Operations")
    class RestoreOperations {

        @Test
        @DisplayName("Should restore all Product in batch")
        void shouldRestoreAllInBatch() {
            when(repository.restoreAllByIds(anyList())).thenReturn(2);

            Result<Integer, Exception> result = service.restoreAll(List.of(1L, 2L));

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.orElseThrow()).isEqualTo(2);
        }
    }
}
