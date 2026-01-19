package com.jnzader.apigen.core.application.service;

import static com.jnzader.apigen.core.support.TestConstants.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import com.jnzader.apigen.core.application.util.Result;
import com.jnzader.apigen.core.domain.entity.Base;
import com.jnzader.apigen.core.domain.exception.ResourceNotFoundException;
import com.jnzader.apigen.core.domain.specification.BaseSpecification;
import com.jnzader.apigen.core.fixtures.TestEntity;
import com.jnzader.apigen.core.fixtures.TestEntityRepository;
import com.jnzader.apigen.core.fixtures.TestEntityServiceImpl;
import com.jnzader.apigen.core.support.TestEntityBuilder;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Tests unitarios para BaseServiceImpl.
 *
 * <p>Estos tests verifican la lógica de negocio del servicio base usando mocks para las
 * dependencias (repository, entityManager, eventPublisher).
 *
 * <p>Patrón de test: Given-When-Then (BDD)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BaseServiceImpl Unit Tests")
class BaseServiceImplTest {

    @Mock private TestEntityRepository repository;

    @Mock private EntityManager entityManager;

    @Mock private ApplicationEventPublisher eventPublisher;

    @Mock private CacheEvictionService cacheEvictionService;

    @Mock private AuditorAware<String> auditorAware;

    @Mock private Query nativeQuery;

    private TestEntityServiceImpl service;

    @Captor private ArgumentCaptor<TestEntity> entityCaptor;

    @Captor private ArgumentCaptor<List<TestEntity>> entityListCaptor;

    private TestEntity testEntity;

    @BeforeEach
    void setUp() {
        // Crear el servicio manualmente para tener control total de las dependencias
        service =
                new TestEntityServiceImpl(
                        repository, cacheEvictionService, eventPublisher, auditorAware);
        // Inyectar entityManager que usa @PersistenceContext
        ReflectionTestUtils.setField(service, "entityManager", entityManager);

        TestEntityBuilder.resetIdCounter();
        testEntity = TestEntityBuilder.aTestEntityWithId().withName(VALID_NAME).build();
    }

    // ==================== findById Tests ====================

    @Nested
    @DisplayName("findById()")
    class FindByIdTests {

        @Test
        @DisplayName("should return entity when found")
        void shouldReturnEntityWhenFound() {
            // Given
            given(repository.findById(VALID_ID)).willReturn(Optional.of(testEntity));

            // When
            Result<TestEntity, Exception> result = service.findById(VALID_ID);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.orElseThrow().getId()).isEqualTo(testEntity.getId());
            assertThat(result.orElseThrow().getName()).isEqualTo(VALID_NAME);
            then(repository).should().findById(VALID_ID);
        }

        @Test
        @DisplayName("should return failure when entity not found")
        void shouldReturnFailureWhenNotFound() {
            // Given
            given(repository.findById(INVALID_ID)).willReturn(Optional.empty());

            // When
            Result<TestEntity, Exception> result = service.findById(INVALID_ID);

            // Then
            assertThat(result.isFailure()).isTrue();
            assertThatThrownBy(result::orElseThrow).isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ==================== findAll Tests ====================

    @Nested
    @DisplayName("findAll()")
    class FindAllTests {

        @Test
        @DisplayName("should return all entities with limit")
        void shouldReturnAllEntitiesWithLimit() {
            // Given
            List<TestEntity> entities = List.of(testEntity);
            Page<TestEntity> page = new PageImpl<>(entities);
            given(repository.count()).willReturn(1L);
            given(repository.findAll(any(Pageable.class))).willReturn(page);

            // When
            Result<List<TestEntity>, Exception> result = service.findAll();

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.orElseThrow()).hasSize(1);
        }

        @Test
        @DisplayName("should return empty list when no entities")
        void shouldReturnEmptyListWhenNoEntities() {
            // Given - count returns 0, so findAll won't be called
            given(repository.count()).willReturn(0L);

            // When
            Result<List<TestEntity>, Exception> result = service.findAll();

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.orElseThrow()).isEmpty();
        }
    }

    // ==================== findAll with Pageable Tests ====================

    @Nested
    @DisplayName("findAll(Pageable)")
    class FindAllPageableTests {

        @Test
        @DisplayName("should return paginated results")
        void shouldReturnPaginatedResults() {
            // Given
            Pageable pageable = PageRequest.of(DEFAULT_PAGE, DEFAULT_SIZE);
            Page<TestEntity> page = new PageImpl<>(List.of(testEntity), pageable, 1);
            given(repository.findAll(pageable)).willReturn(page);

            // When
            Result<Page<TestEntity>, Exception> result = service.findAll(pageable);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.orElseThrow().getContent()).hasSize(1);
            assertThat(result.orElseThrow().getTotalElements()).isEqualTo(1);
        }
    }

    // ==================== findAllActive Tests ====================

    @Nested
    @DisplayName("findAllActive()")
    class FindAllActiveTests {

        @Test
        @DisplayName("should return only active entities")
        void shouldReturnOnlyActiveEntities() {
            // Given
            List<TestEntity> activeEntities = List.of(testEntity);
            Page<TestEntity> page = new PageImpl<>(activeEntities);
            given(repository.count(any(Specification.class))).willReturn(1L);
            given(repository.findAll(any(Specification.class), any(Pageable.class)))
                    .willReturn(page);

            // When
            Result<List<TestEntity>, Exception> result = service.findAllActive();

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.orElseThrow()).allMatch(Base::isActive);
        }
    }

    // ==================== save Tests ====================

    @Nested
    @DisplayName("save()")
    class SaveTests {

        @Test
        @DisplayName("should save new entity and register created event")
        void shouldSaveNewEntityAndRegisterCreatedEvent() {
            // Given
            TestEntity newEntity = TestEntityBuilder.aTestEntity().withName("New Entity").build();
            TestEntity savedEntity =
                    TestEntityBuilder.aTestEntityWithId().withName("New Entity").build();
            given(repository.save(any(TestEntity.class))).willReturn(savedEntity);

            // When
            Result<TestEntity, Exception> result = service.save(newEntity);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.orElseThrow().getId()).isNotNull();
            then(repository).should().save(any(TestEntity.class));
        }

        @Test
        @DisplayName("should update existing entity and register updated event")
        void shouldUpdateExistingEntityAndRegisterUpdatedEvent() {
            // Given
            given(repository.save(testEntity)).willReturn(testEntity);

            // When
            Result<TestEntity, Exception> result = service.save(testEntity);

            // Then
            assertThat(result.isSuccess()).isTrue();
            then(repository).should().save(testEntity);
        }
    }

    // ==================== update Tests ====================

    @Nested
    @DisplayName("update()")
    class UpdateTests {

        @Test
        @DisplayName("should update entity when exists")
        void shouldUpdateEntityWhenExists() {
            // Given
            TestEntity updatedEntity =
                    TestEntityBuilder.aTestEntity().withName(UPDATED_NAME).build();
            given(repository.findById(VALID_ID)).willReturn(Optional.of(testEntity));
            given(repository.save(any(TestEntity.class))).willAnswer(inv -> inv.getArgument(0));

            // When
            Result<TestEntity, Exception> result = service.update(VALID_ID, updatedEntity);

            // Then
            assertThat(result.isSuccess()).isTrue();
            then(repository).should().save(entityCaptor.capture());
            assertThat(entityCaptor.getValue().getId()).isEqualTo(VALID_ID);
        }

        @Test
        @DisplayName("should fail when entity not found")
        void shouldFailWhenEntityNotFound() {
            // Given
            given(repository.findById(INVALID_ID)).willReturn(Optional.empty());

            // When
            Result<TestEntity, Exception> result = service.update(INVALID_ID, testEntity);

            // Then
            assertThat(result.isFailure()).isTrue();
            then(repository).should(never()).save(any());
        }
    }

    // ==================== softDelete Tests ====================

    @Nested
    @DisplayName("softDelete()")
    class SoftDeleteTests {

        @Test
        @DisplayName("should soft delete entity")
        void shouldSoftDeleteEntity() {
            // Given
            given(repository.findById(VALID_ID)).willReturn(Optional.of(testEntity));
            given(repository.save(any(TestEntity.class))).willAnswer(inv -> inv.getArgument(0));

            // When
            Result<Void, Exception> result = service.softDelete(VALID_ID, TEST_USER);

            // Then
            assertThat(result.isSuccess()).isTrue();
            then(repository).should().save(entityCaptor.capture());
            assertThat(entityCaptor.getValue().getEstado()).isFalse();
            assertThat(entityCaptor.getValue().getEliminadoPor()).isEqualTo(TEST_USER);
        }

        @Test
        @DisplayName("should fail when entity not found")
        void shouldFailWhenEntityNotFound() {
            // Given
            given(repository.findById(INVALID_ID)).willReturn(Optional.empty());

            // When
            Result<Void, Exception> result = service.softDelete(INVALID_ID);

            // Then
            assertThat(result.isFailure()).isTrue();
        }
    }

    // ==================== restore Tests ====================

    @Nested
    @DisplayName("restore()")
    class RestoreTests {

        @Test
        @DisplayName("should restore soft deleted entity")
        void shouldRestoreSoftDeletedEntity() {
            // Given
            TestEntity restoredEntity =
                    TestEntityBuilder.aTestEntityWithId().withName(VALID_NAME).build();
            // restore() usa native SQL a través de EntityManager
            given(entityManager.createNativeQuery(anyString())).willReturn(nativeQuery);
            given(nativeQuery.setParameter(eq("id"), any())).willReturn(nativeQuery);
            given(nativeQuery.executeUpdate()).willReturn(1);
            willDoNothing().given(entityManager).flush();
            willDoNothing().given(entityManager).clear();
            given(repository.findById(VALID_ID)).willReturn(Optional.of(restoredEntity));

            // When
            Result<TestEntity, Exception> result = service.restore(VALID_ID);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.orElseThrow().getEstado()).isTrue();
            then(entityManager).should().createNativeQuery(anyString());
            then(nativeQuery).should().executeUpdate();
            then(repository).should().findById(VALID_ID);
        }

        @Test
        @DisplayName("should fail when entity not found")
        void shouldFailWhenEntityNotFound() {
            // Given - native query devuelve 0 cuando la entidad no existe
            given(entityManager.createNativeQuery(anyString())).willReturn(nativeQuery);
            given(nativeQuery.setParameter(eq("id"), any())).willReturn(nativeQuery);
            given(nativeQuery.executeUpdate()).willReturn(0);

            // When
            Result<TestEntity, Exception> result = service.restore(INVALID_ID);

            // Then
            assertThat(result.isFailure()).isTrue();
            then(entityManager).should().createNativeQuery(anyString());
            then(nativeQuery).should().executeUpdate();
        }
    }

    // ==================== hardDelete Tests ====================

    @Nested
    @DisplayName("hardDelete()")
    class HardDeleteTests {

        @Test
        @DisplayName("should permanently delete entity")
        void shouldPermanentlyDeleteEntity() {
            // Given
            given(repository.hardDeleteById(VALID_ID)).willReturn(1);
            // Usar lenient() para evitar problemas con strict stubbing de Mockito 5
            lenient().doNothing().when(eventPublisher).publishEvent(any(Object.class));
            lenient().doNothing().when(cacheEvictionService).evictListsByEntityName(anyString());
            lenient().doNothing().when(cacheEvictionService).evictCounts(anyString());

            // When
            Result<Void, Exception> result = service.hardDelete(VALID_ID);

            // Then
            assertThat(result.isSuccess()).isTrue();
            then(repository).should().hardDeleteById(VALID_ID);
        }
    }

    // ==================== saveAll Tests ====================

    @Nested
    @DisplayName("saveAll()")
    class SaveAllTests {

        @Test
        @DisplayName("should save all entities in batch")
        void shouldSaveAllEntitiesInBatch() {
            // Given
            List<TestEntity> entities =
                    List.of(
                            TestEntityBuilder.aTestEntity().withName("Entity 1").build(),
                            TestEntityBuilder.aTestEntity().withName("Entity 2").build());
            given(repository.saveAll(anyList())).willReturn(entities);
            willDoNothing().given(entityManager).flush();
            willDoNothing().given(entityManager).clear();

            // When
            Result<List<TestEntity>, Exception> result = service.saveAll(entities);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.orElseThrow()).hasSize(2);
            then(repository).should().saveAll(entities);
            then(entityManager).should().flush();
            then(entityManager).should().clear();
        }

        @Test
        @DisplayName("should return empty list for empty input")
        void shouldReturnEmptyListForEmptyInput() {
            // When
            Result<List<TestEntity>, Exception> result = service.saveAll(List.of());

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.orElseThrow()).isEmpty();
            then(repository).should(never()).saveAll(anyList());
        }

        @Test
        @DisplayName("should fail when batch size exceeds limit")
        void shouldFailWhenBatchSizeExceedsLimit() {
            // Given
            List<TestEntity> largeList = new ArrayList<>();
            for (int i = 0; i < 15000; i++) {
                largeList.add(TestEntityBuilder.aTestEntity().build());
            }

            // When
            Result<List<TestEntity>, Exception> result = service.saveAll(largeList);

            // Then
            assertThat(result.isFailure()).isTrue();
        }
    }

    // ==================== softDeleteAll Tests ====================

    @Nested
    @DisplayName("softDeleteAll()")
    class SoftDeleteAllTests {

        @Test
        @DisplayName("should soft delete multiple entities")
        void shouldSoftDeleteMultipleEntities() {
            // Given
            List<Long> ids = List.of(1L, 2L, 3L);
            given(repository.softDeleteAllByIds(anyList(), any(LocalDateTime.class), anyString()))
                    .willReturn(3);

            // When
            Result<Integer, Exception> result = service.softDeleteAll(ids, TEST_USER);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.orElseThrow()).isEqualTo(3);
        }

        @Test
        @DisplayName("should return 0 for empty list")
        void shouldReturnZeroForEmptyList() {
            // When
            Result<Integer, Exception> result = service.softDeleteAll(List.of(), TEST_USER);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.orElseThrow()).isZero();
        }
    }

    // ==================== existsById Tests ====================

    @Nested
    @DisplayName("existsById()")
    class ExistsByIdTests {

        @Test
        @DisplayName("should return true when entity exists")
        void shouldReturnTrueWhenEntityExists() {
            // Given
            given(repository.existsById(VALID_ID)).willReturn(true);

            // When
            Result<Boolean, Exception> result = service.existsById(VALID_ID);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.orElseThrow()).isTrue();
        }

        @Test
        @DisplayName("should return false when entity does not exist")
        void shouldReturnFalseWhenEntityDoesNotExist() {
            // Given
            given(repository.existsById(INVALID_ID)).willReturn(false);

            // When
            Result<Boolean, Exception> result = service.existsById(INVALID_ID);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.orElseThrow()).isFalse();
        }
    }

    // ==================== count Tests ====================

    @Nested
    @DisplayName("count()")
    class CountTests {

        @Test
        @DisplayName("should return total count")
        void shouldReturnTotalCount() {
            // Given
            given(repository.count()).willReturn(10L);

            // When
            Result<Long, Exception> result = service.count();

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.orElseThrow()).isEqualTo(10L);
        }
    }

    // ==================== countActive Tests ====================

    @Nested
    @DisplayName("countActive()")
    class CountActiveTests {

        @Test
        @DisplayName("should return count of active entities")
        void shouldReturnCountOfActiveEntities() {
            // Given
            given(repository.count(any(Specification.class))).willReturn(5L);

            // When
            Result<Long, Exception> result = service.countActive();

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.orElseThrow()).isEqualTo(5L);
        }
    }

    // ==================== findAllActive Pageable Tests ====================

    @Nested
    @DisplayName("findAllActive(Pageable)")
    class FindAllActivePageableTests {

        @Test
        @DisplayName("should return paginated active entities")
        void shouldReturnPaginatedActiveEntities() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<TestEntity> page = new PageImpl<>(List.of(testEntity), pageable, 1);
            given(repository.findAll(any(Specification.class), eq(pageable))).willReturn(page);

            // When
            Result<Page<TestEntity>, Exception> result = service.findAllActive(pageable);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.orElseThrow().getContent()).hasSize(1);
        }
    }

    // ==================== findAll Specification Tests ====================

    @Nested
    @DisplayName("findAll(Specification)")
    class FindAllSpecificationTests {

        @Test
        @DisplayName("should return entities matching specification")
        void shouldReturnEntitiesMatchingSpecification() {
            // Given
            Specification<TestEntity> spec = BaseSpecification.isActive();
            given(repository.findAll(any(Specification.class))).willReturn(List.of(testEntity));

            // When
            Result<List<TestEntity>, Exception> result = service.findAll(spec);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.orElseThrow()).hasSize(1);
        }
    }

    // ==================== findAll Specification Pageable Tests ====================

    @Nested
    @DisplayName("findAll(Specification, Pageable)")
    class FindAllSpecificationPageableTests {

        @Test
        @DisplayName("should return paginated entities matching specification")
        void shouldReturnPaginatedEntitiesMatchingSpecification() {
            // Given
            Specification<TestEntity> spec = BaseSpecification.isActive();
            Pageable pageable = PageRequest.of(0, 10);
            Page<TestEntity> page = new PageImpl<>(List.of(testEntity), pageable, 1);
            given(repository.findAll(any(Specification.class), eq(pageable))).willReturn(page);

            // When
            Result<Page<TestEntity>, Exception> result = service.findAll(spec, pageable);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.orElseThrow().getContent()).hasSize(1);
        }
    }

    // ==================== findOne Tests ====================

    @Nested
    @DisplayName("findOne()")
    class FindOneTests {

        @Test
        @DisplayName("should return entity matching specification")
        void shouldReturnEntityMatchingSpecification() {
            // Given
            Specification<TestEntity> spec = BaseSpecification.hasId(VALID_ID);
            given(repository.findOne(any(Specification.class))).willReturn(Optional.of(testEntity));

            // When
            Result<TestEntity, Exception> result = service.findOne(spec);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.orElseThrow().getId()).isEqualTo(testEntity.getId());
        }

        @Test
        @DisplayName("should return failure when no entity matches")
        void shouldReturnFailureWhenNoEntityMatches() {
            // Given
            Specification<TestEntity> spec = BaseSpecification.hasId(INVALID_ID);
            given(repository.findOne(any(Specification.class))).willReturn(Optional.empty());

            // When
            Result<TestEntity, Exception> result = service.findOne(spec);

            // Then
            assertThat(result.isFailure()).isTrue();
        }
    }

    // ==================== partialUpdate Tests ====================

    @Nested
    @DisplayName("partialUpdate()")
    class PartialUpdateTests {

        @Test
        @DisplayName("should partially update entity")
        void shouldPartiallyUpdateEntity() {
            // Given
            TestEntity partialEntity = TestEntityBuilder.aTestEntity().withName("New Name").build();
            given(repository.findById(VALID_ID)).willReturn(Optional.of(testEntity));
            given(repository.save(any(TestEntity.class))).willAnswer(inv -> inv.getArgument(0));

            // When
            Result<TestEntity, Exception> result = service.partialUpdate(VALID_ID, partialEntity);

            // Then
            assertThat(result.isSuccess()).isTrue();
            then(repository).should().save(any(TestEntity.class));
        }

        @Test
        @DisplayName("should fail when entity not found")
        void shouldFailWhenEntityNotFound() {
            // Given
            given(repository.findById(INVALID_ID)).willReturn(Optional.empty());

            // When
            Result<TestEntity, Exception> result = service.partialUpdate(INVALID_ID, testEntity);

            // Then
            assertThat(result.isFailure()).isTrue();
        }
    }

    // ==================== restoreAll Tests ====================

    @Nested
    @DisplayName("restoreAll()")
    class RestoreAllTests {

        @Test
        @DisplayName("should restore multiple entities")
        void shouldRestoreMultipleEntities() {
            // Given
            List<Long> ids = List.of(1L, 2L, 3L);
            given(repository.restoreAllByIds(ids)).willReturn(3);

            // When
            Result<Integer, Exception> result = service.restoreAll(ids);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.orElseThrow()).isEqualTo(3);
        }

        @Test
        @DisplayName("should return 0 for empty list")
        void shouldReturnZeroForEmptyList() {
            // When
            Result<Integer, Exception> result = service.restoreAll(List.of());

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.orElseThrow()).isZero();
        }
    }

    // ==================== hardDeleteAll Tests ====================

    @Nested
    @DisplayName("hardDeleteAll()")
    class HardDeleteAllTests {

        @Test
        @DisplayName("should hard delete multiple entities")
        void shouldHardDeleteMultipleEntities() {
            // Given
            List<Long> ids = List.of(1L, 2L, 3L);
            willDoNothing().given(repository).deleteAllByIdInBatch(ids);

            // When
            Result<Integer, Exception> result = service.hardDeleteAll(ids);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.orElseThrow()).isEqualTo(3);
            then(repository).should().deleteAllByIdInBatch(ids);
        }

        @Test
        @DisplayName("should return 0 for empty list")
        void shouldReturnZeroForEmptyList() {
            // When
            Result<Integer, Exception> result = service.hardDeleteAll(List.of());

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.orElseThrow()).isZero();
        }
    }

    // ==================== softDelete with auditor Tests ====================

    @Nested
    @DisplayName("softDelete() with auditor")
    class SoftDeleteWithAuditorTests {

        @Test
        @DisplayName("should use auditor for user when calling softDelete without user")
        void shouldUseAuditorForUser() {
            // Given
            given(auditorAware.getCurrentAuditor()).willReturn(Optional.of("auditor_user"));
            given(repository.findById(VALID_ID)).willReturn(Optional.of(testEntity));
            given(repository.save(any(TestEntity.class))).willAnswer(inv -> inv.getArgument(0));

            // When
            Result<Void, Exception> result = service.softDelete(VALID_ID);

            // Then
            assertThat(result.isSuccess()).isTrue();
            then(repository).should().save(entityCaptor.capture());
            assertThat(entityCaptor.getValue().getEliminadoPor()).isEqualTo("auditor_user");
        }
    }

    // ==================== Large batch warning Tests ====================

    @Nested
    @DisplayName("findAll() with large results")
    class FindAllLargeResultsTests {

        @Test
        @DisplayName("should handle large result sets with warning")
        void shouldHandleLargeResultSetsWithWarning() {
            // Given - count returns a large number
            List<TestEntity> entities = List.of(testEntity);
            Page<TestEntity> page = new PageImpl<>(entities);
            given(repository.count()).willReturn(600L); // Above WARN_THRESHOLD
            given(repository.findAll(any(Pageable.class))).willReturn(page);

            // When
            Result<List<TestEntity>, Exception> result = service.findAll();

            // Then
            assertThat(result.isSuccess()).isTrue();
        }

        @Test
        @DisplayName("should handle very large result sets")
        void shouldHandleVeryLargeResultSets() {
            // Given - count returns more than MAX_RESULTS_WITHOUT_PAGINATION
            List<TestEntity> entities = List.of(testEntity);
            Page<TestEntity> page = new PageImpl<>(entities);
            given(repository.count()).willReturn(1500L); // Above MAX_RESULTS
            given(repository.findAll(any(Pageable.class))).willReturn(page);

            // When
            Result<List<TestEntity>, Exception> result = service.findAll();

            // Then
            assertThat(result.isSuccess()).isTrue();
        }
    }

    // ==================== findAllActive large results Tests ====================

    @Nested
    @DisplayName("findAllActive() with large results")
    class FindAllActiveLargeResultsTests {

        @Test
        @DisplayName("should handle large active result sets")
        void shouldHandleLargeActiveResultSets() {
            // Given
            List<TestEntity> entities = List.of(testEntity);
            Page<TestEntity> page = new PageImpl<>(entities);
            given(repository.count(any(Specification.class))).willReturn(600L);
            given(repository.findAll(any(Specification.class), any(Pageable.class)))
                    .willReturn(page);

            // When
            Result<List<TestEntity>, Exception> result = service.findAllActive();

            // Then
            assertThat(result.isSuccess()).isTrue();
        }

        @Test
        @DisplayName("should return empty list when no active entities")
        void shouldReturnEmptyListWhenNoActiveEntities() {
            // Given
            given(repository.count(any(Specification.class))).willReturn(0L);

            // When
            Result<List<TestEntity>, Exception> result = service.findAllActive();

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.orElseThrow()).isEmpty();
        }
    }
}
