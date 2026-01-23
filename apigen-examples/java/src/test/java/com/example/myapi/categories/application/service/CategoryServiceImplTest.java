package com.example.myapi.categories.application.service;

import com.jnzader.apigen.core.application.service.CacheEvictionService;
import com.jnzader.apigen.core.application.util.Result;
import com.example.myapi.categories.domain.entity.Category;
import com.example.myapi.categories.infrastructure.repository.CategoryRepository;
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
@DisplayName("CategoryService Tests")
class CategoryServiceImplTest {

    @Mock
    private CategoryRepository repository;

    @Mock
    private CacheEvictionService cacheEvictionService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private AuditorAware<String> auditorAware;

    @Mock
    private EntityManager entityManager;

    private CategoryServiceImpl service;

    private Category category;

    @BeforeEach
    void setUp() {
        service = new CategoryServiceImpl(repository, cacheEvictionService, eventPublisher, auditorAware);
        // Inject mock EntityManager for batch operations (saveAll uses flush/clear)
        ReflectionTestUtils.setField(service, "entityManager", entityManager);
        category = new Category();
        category.setId(1L);
        category.setEstado(true);
    }

    @Nested
    @DisplayName("Find Operations")
    class FindOperations {

        @Test
        @DisplayName("Should find Category by ID successfully")
        void shouldFindCategoryById() {
            when(repository.findById(1L)).thenReturn(Optional.of(category));

            Result<Category, Exception> result = service.findById(1L);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.orElseThrow()).isEqualTo(category);
            verify(repository).findById(1L);
        }

        @Test
        @DisplayName("Should return failure when Category not found by ID")
        void shouldReturnFailureWhenNotFound() {
            when(repository.findById(anyLong())).thenReturn(Optional.empty());

            Result<Category, Exception> result = service.findById(999L);

            assertThat(result.isFailure()).isTrue();
            verify(repository).findById(999L);
        }

        @Test
        @DisplayName("Should find all Category with pagination")
        void shouldFindAllWithPagination() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Category> page = new PageImpl<>(List.of(category));
            when(repository.findAll(pageable)).thenReturn(page);

            Result<Page<Category>, Exception> result = service.findAll(pageable);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.orElseThrow().getContent()).hasSize(1);
            verify(repository).findAll(pageable);
        }

        @Test
        @DisplayName("Should find all active Category with pagination")
        void shouldFindAllActiveWithPagination() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Category> page = new PageImpl<>(List.of(category));
            when(repository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

            Result<Page<Category>, Exception> result = service.findAllActive(pageable);

            assertThat(result.isSuccess()).isTrue();
            verify(repository).findAll(any(Specification.class), eq(pageable));
        }

        @Test
        @DisplayName("Should check if Category exists by ID")
        void shouldCheckExistsById() {
            when(repository.existsById(1L)).thenReturn(true);

            Result<Boolean, Exception> result = service.existsById(1L);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.orElseThrow()).isTrue();
            verify(repository).existsById(1L);
        }

        @Test
        @DisplayName("Should count all Category")
        void shouldCountAll() {
            when(repository.count()).thenReturn(5L);

            Result<Long, Exception> result = service.count();

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.orElseThrow()).isEqualTo(5L);
            verify(repository).count();
        }

        @Test
        @DisplayName("Should count active Category")
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
        @DisplayName("Should save new Category successfully")
        void shouldSaveNewCategory() {
            Category newCategory = new Category();
            when(repository.save(any(Category.class))).thenReturn(category);

            Result<Category, Exception> result = service.save(newCategory);

            assertThat(result.isSuccess()).isTrue();
            verify(repository).save(any(Category.class));
        }

        @Test
        @DisplayName("Should update existing Category")
        void shouldUpdateExistingCategory() {
            when(repository.findById(1L)).thenReturn(Optional.of(category));
            when(repository.save(any(Category.class))).thenReturn(category);

            Result<Category, Exception> result = service.update(1L, category);

            assertThat(result.isSuccess()).isTrue();
            verify(repository).findById(1L);
            verify(repository).save(any(Category.class));
            verify(cacheEvictionService).evictListsByEntityName(anyString());
        }

        @Test
        @DisplayName("Should partial update Category")
        void shouldPartialUpdateCategory() {
            when(repository.findById(1L)).thenReturn(Optional.of(category));
            when(repository.save(any(Category.class))).thenReturn(category);

            Result<Category, Exception> result = service.partialUpdate(1L, category);

            assertThat(result.isSuccess()).isTrue();
            verify(repository).findById(1L);
            verify(repository).save(any(Category.class));
        }

        @Test
        @DisplayName("Should save all Category in batch")
        void shouldSaveAllInBatch() {
            List<Category> entities = List.of(category, new Category());
            when(repository.saveAll(anyList())).thenReturn(entities);

            Result<List<Category>, Exception> result = service.saveAll(entities);

            assertThat(result.isSuccess()).isTrue();
            verify(repository).saveAll(anyList());
        }
    }

    @Nested
    @DisplayName("Delete Operations")
    class DeleteOperations {

        @Test
        @DisplayName("Should soft delete Category")
        void shouldSoftDeleteCategory() {
            when(auditorAware.getCurrentAuditor()).thenReturn(Optional.of("testUser"));
            when(repository.findById(1L)).thenReturn(Optional.of(category));
            when(repository.save(any(Category.class))).thenReturn(category);

            Result<Void, Exception> result = service.softDelete(1L);

            assertThat(result.isSuccess()).isTrue();
            verify(repository).findById(1L);
            verify(repository).save(any(Category.class));
            verify(cacheEvictionService).evictListsByEntityName(anyString());
        }

        @Test
        @DisplayName("Should hard delete Category")
        void shouldHardDeleteCategory() {
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
        @DisplayName("Should soft delete all Category in batch")
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
        @DisplayName("Should restore all Category in batch")
        void shouldRestoreAllInBatch() {
            when(repository.restoreAllByIds(anyList())).thenReturn(2);

            Result<Integer, Exception> result = service.restoreAll(List.of(1L, 2L));

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.orElseThrow()).isEqualTo(2);
        }
    }
}
