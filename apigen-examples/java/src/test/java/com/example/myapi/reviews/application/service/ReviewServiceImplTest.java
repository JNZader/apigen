package com.example.myapi.reviews.application.service;

import com.jnzader.apigen.core.application.service.CacheEvictionService;
import com.jnzader.apigen.core.application.util.Result;
import com.example.myapi.reviews.domain.entity.Review;
import com.example.myapi.reviews.infrastructure.repository.ReviewRepository;
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
@DisplayName("ReviewService Tests")
class ReviewServiceImplTest {

    @Mock
    private ReviewRepository repository;

    @Mock
    private CacheEvictionService cacheEvictionService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private AuditorAware<String> auditorAware;

    @Mock
    private EntityManager entityManager;

    private ReviewServiceImpl service;

    private Review review;

    @BeforeEach
    void setUp() {
        service = new ReviewServiceImpl(repository, cacheEvictionService, eventPublisher, auditorAware);
        // Inject mock EntityManager for batch operations (saveAll uses flush/clear)
        ReflectionTestUtils.setField(service, "entityManager", entityManager);
        review = new Review();
        review.setId(1L);
        review.setEstado(true);
    }

    @Nested
    @DisplayName("Find Operations")
    class FindOperations {

        @Test
        @DisplayName("Should find Review by ID successfully")
        void shouldFindReviewById() {
            when(repository.findById(1L)).thenReturn(Optional.of(review));

            Result<Review, Exception> result = service.findById(1L);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.orElseThrow()).isEqualTo(review);
            verify(repository).findById(1L);
        }

        @Test
        @DisplayName("Should return failure when Review not found by ID")
        void shouldReturnFailureWhenNotFound() {
            when(repository.findById(anyLong())).thenReturn(Optional.empty());

            Result<Review, Exception> result = service.findById(999L);

            assertThat(result.isFailure()).isTrue();
            verify(repository).findById(999L);
        }

        @Test
        @DisplayName("Should find all Review with pagination")
        void shouldFindAllWithPagination() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Review> page = new PageImpl<>(List.of(review));
            when(repository.findAll(pageable)).thenReturn(page);

            Result<Page<Review>, Exception> result = service.findAll(pageable);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.orElseThrow().getContent()).hasSize(1);
            verify(repository).findAll(pageable);
        }

        @Test
        @DisplayName("Should find all active Review with pagination")
        void shouldFindAllActiveWithPagination() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Review> page = new PageImpl<>(List.of(review));
            when(repository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

            Result<Page<Review>, Exception> result = service.findAllActive(pageable);

            assertThat(result.isSuccess()).isTrue();
            verify(repository).findAll(any(Specification.class), eq(pageable));
        }

        @Test
        @DisplayName("Should check if Review exists by ID")
        void shouldCheckExistsById() {
            when(repository.existsById(1L)).thenReturn(true);

            Result<Boolean, Exception> result = service.existsById(1L);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.orElseThrow()).isTrue();
            verify(repository).existsById(1L);
        }

        @Test
        @DisplayName("Should count all Review")
        void shouldCountAll() {
            when(repository.count()).thenReturn(5L);

            Result<Long, Exception> result = service.count();

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.orElseThrow()).isEqualTo(5L);
            verify(repository).count();
        }

        @Test
        @DisplayName("Should count active Review")
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
        @DisplayName("Should save new Review successfully")
        void shouldSaveNewReview() {
            Review newReview = new Review();
            when(repository.save(any(Review.class))).thenReturn(review);

            Result<Review, Exception> result = service.save(newReview);

            assertThat(result.isSuccess()).isTrue();
            verify(repository).save(any(Review.class));
        }

        @Test
        @DisplayName("Should update existing Review")
        void shouldUpdateExistingReview() {
            when(repository.findById(1L)).thenReturn(Optional.of(review));
            when(repository.save(any(Review.class))).thenReturn(review);

            Result<Review, Exception> result = service.update(1L, review);

            assertThat(result.isSuccess()).isTrue();
            verify(repository).findById(1L);
            verify(repository).save(any(Review.class));
            verify(cacheEvictionService).evictListsByEntityName(anyString());
        }

        @Test
        @DisplayName("Should partial update Review")
        void shouldPartialUpdateReview() {
            when(repository.findById(1L)).thenReturn(Optional.of(review));
            when(repository.save(any(Review.class))).thenReturn(review);

            Result<Review, Exception> result = service.partialUpdate(1L, review);

            assertThat(result.isSuccess()).isTrue();
            verify(repository).findById(1L);
            verify(repository).save(any(Review.class));
        }

        @Test
        @DisplayName("Should save all Review in batch")
        void shouldSaveAllInBatch() {
            List<Review> entities = List.of(review, new Review());
            when(repository.saveAll(anyList())).thenReturn(entities);

            Result<List<Review>, Exception> result = service.saveAll(entities);

            assertThat(result.isSuccess()).isTrue();
            verify(repository).saveAll(anyList());
        }
    }

    @Nested
    @DisplayName("Delete Operations")
    class DeleteOperations {

        @Test
        @DisplayName("Should soft delete Review")
        void shouldSoftDeleteReview() {
            when(auditorAware.getCurrentAuditor()).thenReturn(Optional.of("testUser"));
            when(repository.findById(1L)).thenReturn(Optional.of(review));
            when(repository.save(any(Review.class))).thenReturn(review);

            Result<Void, Exception> result = service.softDelete(1L);

            assertThat(result.isSuccess()).isTrue();
            verify(repository).findById(1L);
            verify(repository).save(any(Review.class));
            verify(cacheEvictionService).evictListsByEntityName(anyString());
        }

        @Test
        @DisplayName("Should hard delete Review")
        void shouldHardDeleteReview() {
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
        @DisplayName("Should soft delete all Review in batch")
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
        @DisplayName("Should restore all Review in batch")
        void shouldRestoreAllInBatch() {
            when(repository.restoreAllByIds(anyList())).thenReturn(2);

            Result<Integer, Exception> result = service.restoreAll(List.of(1L, 2L));

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.orElseThrow()).isEqualTo(2);
        }
    }
}
