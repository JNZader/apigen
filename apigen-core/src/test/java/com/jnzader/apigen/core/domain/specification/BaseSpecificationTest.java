package com.jnzader.apigen.core.domain.specification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.jnzader.apigen.core.domain.entity.Base;
import jakarta.persistence.criteria.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
@DisplayName("BaseSpecification Tests")
@SuppressWarnings({"unchecked", "rawtypes"})
class BaseSpecificationTest {

    @Mock private Root root;

    @Mock private CriteriaQuery query;

    @Mock private CriteriaBuilder cb;

    @Mock private Path path;

    @Mock private Predicate predicate;

    @Mock private Predicate conjunction;

    @Mock private Predicate disjunction;

    @Mock private Expression stringExpression;

    static class TestEntity extends Base {}

    @BeforeEach
    void setUp() {
        lenient().when(cb.conjunction()).thenReturn(conjunction);
        lenient().when(cb.disjunction()).thenReturn(disjunction);
    }

    @Nested
    @DisplayName("Estado Specifications")
    class EstadoSpecificationTests {

        @Test
        @DisplayName("isActive() should create specification for active entities")
        void isActiveShouldCreateSpecForActiveEntities() {
            when(root.get("estado")).thenReturn(path);
            when(cb.isTrue(path)).thenReturn(predicate);

            Specification<TestEntity> spec = BaseSpecification.isActive();
            Predicate result = spec.toPredicate(root, query, cb);

            assertThat(result).isEqualTo(predicate);
            verify(cb).isTrue(path);
        }

        @Test
        @DisplayName("isInactive() should create specification for inactive entities")
        void isInactiveShouldCreateSpecForInactiveEntities() {
            when(root.get("estado")).thenReturn(path);
            when(cb.isFalse(path)).thenReturn(predicate);

            Specification<TestEntity> spec = BaseSpecification.isInactive();
            Predicate result = spec.toPredicate(root, query, cb);

            assertThat(result).isEqualTo(predicate);
            verify(cb).isFalse(path);
        }

        @Test
        @DisplayName("hasEstado() should create specification for specific estado")
        void hasEstadoShouldCreateSpecForSpecificEstado() {
            when(root.get("estado")).thenReturn(path);
            when(cb.equal(path, true)).thenReturn(predicate);

            Specification<TestEntity> spec = BaseSpecification.hasEstado(true);
            Predicate result = spec.toPredicate(root, query, cb);

            assertThat(result).isEqualTo(predicate);
        }

        @Test
        @DisplayName("hasEstado() should return conjunction for null estado")
        void hasEstadoShouldReturnConjunctionForNullEstado() {
            Specification<TestEntity> spec = BaseSpecification.hasEstado(null);
            Predicate result = spec.toPredicate(root, query, cb);

            assertThat(result).isEqualTo(conjunction);
        }
    }

    @Nested
    @DisplayName("ID Specifications")
    class IdSpecificationTests {

        @Test
        @DisplayName("hasId() should create specification for specific ID")
        void hasIdShouldCreateSpecForSpecificId() {
            when(root.get("id")).thenReturn(path);
            when(cb.equal(path, 1L)).thenReturn(predicate);

            Specification<TestEntity> spec = BaseSpecification.hasId(1L);
            Predicate result = spec.toPredicate(root, query, cb);

            assertThat(result).isEqualTo(predicate);
        }

        @Test
        @DisplayName("hasId() should return conjunction for null ID")
        void hasIdShouldReturnConjunctionForNullId() {
            Specification<TestEntity> spec = BaseSpecification.hasId(null);
            Predicate result = spec.toPredicate(root, query, cb);

            assertThat(result).isEqualTo(conjunction);
        }

        @Test
        @DisplayName("hasIdIn() should create specification for IDs in list")
        void hasIdInShouldCreateSpecForIdsInList() {
            CriteriaBuilder.In inClause = mock(CriteriaBuilder.In.class);
            when(root.get("id")).thenReturn(path);
            when(path.in(List.of(1L, 2L, 3L))).thenReturn(inClause);

            Specification<TestEntity> spec = BaseSpecification.hasIdIn(List.of(1L, 2L, 3L));
            Predicate result = spec.toPredicate(root, query, cb);

            assertThat(result).isEqualTo(inClause);
        }

        @Test
        @DisplayName("hasIdIn() should return conjunction for null list")
        void hasIdInShouldReturnConjunctionForNullList() {
            Specification<TestEntity> spec = BaseSpecification.hasIdIn(null);
            Predicate result = spec.toPredicate(root, query, cb);

            assertThat(result).isEqualTo(conjunction);
        }

        @Test
        @DisplayName("hasIdIn() should return conjunction for empty list")
        void hasIdInShouldReturnConjunctionForEmptyList() {
            Specification<TestEntity> spec = BaseSpecification.hasIdIn(List.of());
            Predicate result = spec.toPredicate(root, query, cb);

            assertThat(result).isEqualTo(conjunction);
        }

        @Test
        @DisplayName("hasIdNotIn() should create specification for IDs not in list")
        void hasIdNotInShouldCreateSpecForIdsNotInList() {
            CriteriaBuilder.In inClause = mock(CriteriaBuilder.In.class);
            when(root.get("id")).thenReturn(path);
            when(path.in(Set.of(1L, 2L))).thenReturn(inClause);
            when(cb.not(inClause)).thenReturn(predicate);

            Specification<TestEntity> spec = BaseSpecification.hasIdNotIn(Set.of(1L, 2L));
            Predicate result = spec.toPredicate(root, query, cb);

            assertThat(result).isEqualTo(predicate);
        }

        @Test
        @DisplayName("hasIdNotIn() should return conjunction for null list")
        void hasIdNotInShouldReturnConjunctionForNullList() {
            Specification<TestEntity> spec = BaseSpecification.hasIdNotIn(null);
            Predicate result = spec.toPredicate(root, query, cb);

            assertThat(result).isEqualTo(conjunction);
        }

        @Test
        @DisplayName("hasIdNotIn() should return conjunction for empty list")
        void hasIdNotInShouldReturnConjunctionForEmptyList() {
            Specification<TestEntity> spec = BaseSpecification.hasIdNotIn(Set.of());
            Predicate result = spec.toPredicate(root, query, cb);

            assertThat(result).isEqualTo(conjunction);
        }
    }

    @Nested
    @DisplayName("Date Specifications")
    class DateSpecificationTests {

        @Test
        @DisplayName("createdAfter() should create specification for entities created after date")
        void createdAfterShouldCreateSpec() {
            LocalDateTime date = LocalDateTime.now();
            when(root.get("fechaCreacion")).thenReturn(path);
            when(cb.greaterThan(path, date)).thenReturn(predicate);

            Specification<TestEntity> spec = BaseSpecification.createdAfter(date);
            Predicate result = spec.toPredicate(root, query, cb);

            assertThat(result).isEqualTo(predicate);
        }

        @Test
        @DisplayName("createdAfter() should return conjunction for null date")
        void createdAfterShouldReturnConjunctionForNullDate() {
            Specification<TestEntity> spec = BaseSpecification.createdAfter(null);
            Predicate result = spec.toPredicate(root, query, cb);

            assertThat(result).isEqualTo(conjunction);
        }

        @Test
        @DisplayName("createdBefore() should create specification for entities created before date")
        void createdBeforeShouldCreateSpec() {
            LocalDateTime date = LocalDateTime.now();
            when(root.get("fechaCreacion")).thenReturn(path);
            when(cb.lessThan(path, date)).thenReturn(predicate);

            Specification<TestEntity> spec = BaseSpecification.createdBefore(date);
            Predicate result = spec.toPredicate(root, query, cb);

            assertThat(result).isEqualTo(predicate);
        }

        @Test
        @DisplayName("createdBefore() should return conjunction for null date")
        void createdBeforeShouldReturnConjunctionForNullDate() {
            Specification<TestEntity> spec = BaseSpecification.createdBefore(null);
            Predicate result = spec.toPredicate(root, query, cb);

            assertThat(result).isEqualTo(conjunction);
        }

        @Test
        @DisplayName("createdBetween() should create specification with both dates")
        void createdBetweenShouldCreateSpecWithBothDates() {
            LocalDateTime start = LocalDateTime.now().minusDays(1);
            LocalDateTime end = LocalDateTime.now();
            when(root.get("fechaCreacion")).thenReturn(path);
            when(cb.between(path, start, end)).thenReturn(predicate);

            Specification<TestEntity> spec = BaseSpecification.createdBetween(start, end);
            Predicate result = spec.toPredicate(root, query, cb);

            assertThat(result).isEqualTo(predicate);
        }

        @Test
        @DisplayName("createdBetween() should use gte when only start is provided")
        void createdBetweenShouldUseGteWhenOnlyStartProvided() {
            LocalDateTime start = LocalDateTime.now().minusDays(1);
            when(root.get("fechaCreacion")).thenReturn(path);
            when(cb.greaterThanOrEqualTo(path, start)).thenReturn(predicate);

            Specification<TestEntity> spec = BaseSpecification.createdBetween(start, null);
            Predicate result = spec.toPredicate(root, query, cb);

            assertThat(result).isEqualTo(predicate);
        }

        @Test
        @DisplayName("createdBetween() should use lte when only end is provided")
        void createdBetweenShouldUseLteWhenOnlyEndProvided() {
            LocalDateTime end = LocalDateTime.now();
            when(root.get("fechaCreacion")).thenReturn(path);
            when(cb.lessThanOrEqualTo(path, end)).thenReturn(predicate);

            Specification<TestEntity> spec = BaseSpecification.createdBetween(null, end);
            Predicate result = spec.toPredicate(root, query, cb);

            assertThat(result).isEqualTo(predicate);
        }

        @Test
        @DisplayName("createdBetween() should return conjunction when both dates are null")
        void createdBetweenShouldReturnConjunctionWhenBothDatesNull() {
            Specification<TestEntity> spec = BaseSpecification.createdBetween(null, null);
            Predicate result = spec.toPredicate(root, query, cb);

            assertThat(result).isEqualTo(conjunction);
        }

        @Test
        @DisplayName("updatedAfter() should create specification")
        void updatedAfterShouldCreateSpec() {
            LocalDateTime date = LocalDateTime.now();
            when(root.get("fechaActualizacion")).thenReturn(path);
            when(cb.greaterThan(path, date)).thenReturn(predicate);

            Specification<TestEntity> spec = BaseSpecification.updatedAfter(date);
            Predicate result = spec.toPredicate(root, query, cb);

            assertThat(result).isEqualTo(predicate);
        }

        @Test
        @DisplayName("updatedAfter() should return conjunction for null date")
        void updatedAfterShouldReturnConjunctionForNullDate() {
            Specification<TestEntity> spec = BaseSpecification.updatedAfter(null);
            Predicate result = spec.toPredicate(root, query, cb);

            assertThat(result).isEqualTo(conjunction);
        }

        @Test
        @DisplayName("updatedBefore() should create specification")
        void updatedBeforeShouldCreateSpec() {
            LocalDateTime date = LocalDateTime.now();
            when(root.get("fechaActualizacion")).thenReturn(path);
            when(cb.lessThan(path, date)).thenReturn(predicate);

            Specification<TestEntity> spec = BaseSpecification.updatedBefore(date);
            Predicate result = spec.toPredicate(root, query, cb);

            assertThat(result).isEqualTo(predicate);
        }

        @Test
        @DisplayName("updatedBefore() should return conjunction for null date")
        void updatedBeforeShouldReturnConjunctionForNullDate() {
            Specification<TestEntity> spec = BaseSpecification.updatedBefore(null);
            Predicate result = spec.toPredicate(root, query, cb);

            assertThat(result).isEqualTo(conjunction);
        }

        @Test
        @DisplayName("updatedBetween() should create specification with both dates")
        void updatedBetweenShouldCreateSpecWithBothDates() {
            LocalDateTime start = LocalDateTime.now().minusDays(1);
            LocalDateTime end = LocalDateTime.now();
            when(root.get("fechaActualizacion")).thenReturn(path);
            when(cb.between(path, start, end)).thenReturn(predicate);

            Specification<TestEntity> spec = BaseSpecification.updatedBetween(start, end);
            Predicate result = spec.toPredicate(root, query, cb);

            assertThat(result).isEqualTo(predicate);
        }

        @Test
        @DisplayName("updatedBetween() should use gte when only start is provided")
        void updatedBetweenShouldUseGteWhenOnlyStartProvided() {
            LocalDateTime start = LocalDateTime.now().minusDays(1);
            when(root.get("fechaActualizacion")).thenReturn(path);
            when(cb.greaterThanOrEqualTo(path, start)).thenReturn(predicate);

            Specification<TestEntity> spec = BaseSpecification.updatedBetween(start, null);
            Predicate result = spec.toPredicate(root, query, cb);

            assertThat(result).isEqualTo(predicate);
        }

        @Test
        @DisplayName("updatedBetween() should use lte when only end is provided")
        void updatedBetweenShouldUseLteWhenOnlyEndProvided() {
            LocalDateTime end = LocalDateTime.now();
            when(root.get("fechaActualizacion")).thenReturn(path);
            when(cb.lessThanOrEqualTo(path, end)).thenReturn(predicate);

            Specification<TestEntity> spec = BaseSpecification.updatedBetween(null, end);
            Predicate result = spec.toPredicate(root, query, cb);

            assertThat(result).isEqualTo(predicate);
        }

        @Test
        @DisplayName("updatedBetween() should return conjunction when both dates are null")
        void updatedBetweenShouldReturnConjunctionWhenBothDatesNull() {
            Specification<TestEntity> spec = BaseSpecification.updatedBetween(null, null);
            Predicate result = spec.toPredicate(root, query, cb);

            assertThat(result).isEqualTo(conjunction);
        }
    }

    @Nested
    @DisplayName("Soft Delete Specifications")
    class SoftDeleteSpecificationTests {

        @Test
        @DisplayName("notDeleted() should create specification for non-deleted entities")
        void notDeletedShouldCreateSpec() {
            when(root.get("fechaEliminacion")).thenReturn(path);
            when(cb.isNull(path)).thenReturn(predicate);

            Specification<TestEntity> spec = BaseSpecification.notDeleted();
            Predicate result = spec.toPredicate(root, query, cb);

            assertThat(result).isEqualTo(predicate);
        }

        @Test
        @DisplayName("isDeleted() should create specification for deleted entities")
        void isDeletedShouldCreateSpec() {
            when(root.get("fechaEliminacion")).thenReturn(path);
            when(cb.isNotNull(path)).thenReturn(predicate);

            Specification<TestEntity> spec = BaseSpecification.isDeleted();
            Predicate result = spec.toPredicate(root, query, cb);

            assertThat(result).isEqualTo(predicate);
        }

        @Test
        @DisplayName("deletedBy() should create specification for specific user")
        void deletedByShouldCreateSpec() {
            when(root.get("eliminadoPor")).thenReturn(path);
            when(cb.equal(path, "admin")).thenReturn(predicate);

            Specification<TestEntity> spec = BaseSpecification.deletedBy("admin");
            Predicate result = spec.toPredicate(root, query, cb);

            assertThat(result).isEqualTo(predicate);
        }

        @Test
        @DisplayName("deletedBy() should return conjunction for null user")
        void deletedByShouldReturnConjunctionForNullUser() {
            Specification<TestEntity> spec = BaseSpecification.deletedBy(null);
            Predicate result = spec.toPredicate(root, query, cb);

            assertThat(result).isEqualTo(conjunction);
        }
    }

    @Nested
    @DisplayName("Generic Field Specifications")
    class GenericFieldSpecificationTests {

        @Test
        @DisplayName("fieldContains() should create case-insensitive like specification")
        void fieldContainsShouldCreateCaseInsensitiveLikeSpec() {
            when(root.get("name")).thenReturn(path);
            when(path.as(String.class)).thenReturn(path);
            when(cb.lower(path)).thenReturn(stringExpression);
            when(cb.like(stringExpression, "%john%")).thenReturn(predicate);

            Specification<TestEntity> spec = BaseSpecification.fieldContains("name", "John");
            Predicate result = spec.toPredicate(root, query, cb);

            assertThat(result).isEqualTo(predicate);
        }

        @Test
        @DisplayName("fieldContains() should return conjunction for null value")
        void fieldContainsShouldReturnConjunctionForNullValue() {
            Specification<TestEntity> spec = BaseSpecification.fieldContains("name", null);
            Predicate result = spec.toPredicate(root, query, cb);

            assertThat(result).isEqualTo(conjunction);
        }

        @Test
        @DisplayName("fieldContains() should return conjunction for blank value")
        void fieldContainsShouldReturnConjunctionForBlankValue() {
            Specification<TestEntity> spec = BaseSpecification.fieldContains("name", "   ");
            Predicate result = spec.toPredicate(root, query, cb);

            assertThat(result).isEqualTo(conjunction);
        }

        @Test
        @DisplayName("fieldEquals() should create equality specification")
        void fieldEqualsShouldCreateEqualitySpec() {
            when(root.get("name")).thenReturn(path);
            when(cb.equal(path, "value")).thenReturn(predicate);

            Specification<TestEntity> spec = BaseSpecification.fieldEquals("name", "value");
            Predicate result = spec.toPredicate(root, query, cb);

            assertThat(result).isEqualTo(predicate);
        }

        @Test
        @DisplayName("fieldEquals() should return conjunction for null value")
        void fieldEqualsShouldReturnConjunctionForNullValue() {
            Specification<TestEntity> spec = BaseSpecification.fieldEquals("name", null);
            Predicate result = spec.toPredicate(root, query, cb);

            assertThat(result).isEqualTo(conjunction);
        }

        @Test
        @DisplayName("fieldIsNotNull() should create isNotNull specification")
        void fieldIsNotNullShouldCreateSpec() {
            when(root.get("name")).thenReturn(path);
            when(cb.isNotNull(path)).thenReturn(predicate);

            Specification<TestEntity> spec = BaseSpecification.fieldIsNotNull("name");
            Predicate result = spec.toPredicate(root, query, cb);

            assertThat(result).isEqualTo(predicate);
        }

        @Test
        @DisplayName("fieldIsNull() should create isNull specification")
        void fieldIsNullShouldCreateSpec() {
            when(root.get("name")).thenReturn(path);
            when(cb.isNull(path)).thenReturn(predicate);

            Specification<TestEntity> spec = BaseSpecification.fieldIsNull("name");
            Predicate result = spec.toPredicate(root, query, cb);

            assertThat(result).isEqualTo(predicate);
        }
    }

    @Nested
    @DisplayName("Audit Specifications")
    class AuditSpecificationTests {

        @Test
        @DisplayName("createdBy() should create specification for specific user")
        void createdByShouldCreateSpec() {
            when(root.get("creadoPor")).thenReturn(path);
            when(cb.equal(path, "user1")).thenReturn(predicate);

            Specification<TestEntity> spec = BaseSpecification.createdBy("user1");
            Predicate result = spec.toPredicate(root, query, cb);

            assertThat(result).isEqualTo(predicate);
        }

        @Test
        @DisplayName("createdBy() should return conjunction for null user")
        void createdByShouldReturnConjunctionForNullUser() {
            Specification<TestEntity> spec = BaseSpecification.createdBy(null);
            Predicate result = spec.toPredicate(root, query, cb);

            assertThat(result).isEqualTo(conjunction);
        }

        @Test
        @DisplayName("modifiedBy() should create specification for specific user")
        void modifiedByShouldCreateSpec() {
            when(root.get("modificadoPor")).thenReturn(path);
            when(cb.equal(path, "user2")).thenReturn(predicate);

            Specification<TestEntity> spec = BaseSpecification.modifiedBy("user2");
            Predicate result = spec.toPredicate(root, query, cb);

            assertThat(result).isEqualTo(predicate);
        }

        @Test
        @DisplayName("modifiedBy() should return conjunction for null user")
        void modifiedByShouldReturnConjunctionForNullUser() {
            Specification<TestEntity> spec = BaseSpecification.modifiedBy(null);
            Predicate result = spec.toPredicate(root, query, cb);

            assertThat(result).isEqualTo(conjunction);
        }
    }

    @Nested
    @DisplayName("Combinator Specifications")
    class CombinatorSpecificationTests {

        @Test
        @DisplayName("allOf() should combine specifications with AND")
        void allOfShouldCombineWithAnd() {
            Specification<TestEntity> spec1 = (r, q, c) -> predicate;
            Specification<TestEntity> spec2 = (r, q, c) -> predicate;

            Specification<TestEntity> combined = BaseSpecification.allOf(spec1, spec2);

            assertThat(combined).isNotNull();
        }

        @Test
        @DisplayName("allOf() should handle null specifications")
        void allOfShouldHandleNullSpecs() {
            Specification<TestEntity> spec1 = (r, q, c) -> predicate;

            Specification<TestEntity> combined = BaseSpecification.allOf(spec1, null);

            assertThat(combined).isNotNull();
        }

        @Test
        @DisplayName("anyOf() should combine specifications with OR")
        void anyOfShouldCombineWithOr() {
            Specification<TestEntity> spec1 = (r, q, c) -> predicate;
            Specification<TestEntity> spec2 = (r, q, c) -> predicate;

            Specification<TestEntity> combined = BaseSpecification.anyOf(spec1, spec2);

            assertThat(combined).isNotNull();
        }

        @Test
        @DisplayName("anyOf() should return disjunction for all null specs")
        void anyOfShouldReturnDisjunctionForAllNullSpecs() {
            Specification<TestEntity> combined =
                    BaseSpecification.anyOf((Specification<TestEntity>) null);
            Predicate result = combined.toPredicate(root, query, cb);

            assertThat(result).isEqualTo(disjunction);
        }

        @Test
        @DisplayName("not() should negate specification")
        void notShouldNegateSpec() {
            Specification<TestEntity> spec = (r, q, c) -> predicate;

            Specification<TestEntity> negated = BaseSpecification.not(spec);

            assertThat(negated).isNotNull();
        }

        @Test
        @DisplayName("anyOf() should handle null first spec then valid spec")
        void anyOfShouldHandleNullFirstThenValidSpec() {
            Specification<TestEntity> spec = (r, q, c) -> predicate;

            Specification<TestEntity> combined = BaseSpecification.anyOf(null, spec);

            assertThat(combined).isNotNull();
        }
    }
}
