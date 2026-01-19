package com.jnzader.apigen.core.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.jnzader.apigen.core.domain.event.DomainEvent;
import com.jnzader.apigen.core.domain.event.EntityCreatedEvent;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Base Entity Tests")
class BaseTest {

    private TestableBase entity;

    // Concrete implementation for testing
    static class TestableBase extends Base {
        // Empty implementation for testing
    }

    @BeforeEach
    void setUp() {
        entity = new TestableBase();
    }

    @Nested
    @DisplayName("Default Values")
    class DefaultValuesTests {

        @Test
        @DisplayName("should have estado true by default")
        void shouldHaveEstadoTrueByDefault() {
            assertThat(entity.getEstado()).isTrue();
        }

        @Test
        @DisplayName("should have version 0 by default")
        void shouldHaveVersionZeroByDefault() {
            assertThat(entity.getVersion()).isZero();
        }

        @Test
        @DisplayName("should have null id by default")
        void shouldHaveNullIdByDefault() {
            assertThat(entity.getId()).isNull();
        }

        @Test
        @DisplayName("should have empty domain events by default")
        void shouldHaveEmptyDomainEventsByDefault() {
            assertThat(entity.getDomainEvents()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Soft Delete")
    class SoftDeleteTests {

        @Test
        @DisplayName("should mark entity as deleted with softDelete")
        void shouldMarkEntityAsDeleted() {
            entity.softDelete("testUser");

            assertThat(entity.getEstado()).isFalse();
            assertThat(entity.getFechaEliminacion()).isNotNull();
            assertThat(entity.getEliminadoPor()).isEqualTo("testUser");
            assertThat(entity.isDeleted()).isTrue();
            assertThat(entity.isActive()).isFalse();
        }

        @Test
        @DisplayName("should restore deleted entity")
        void shouldRestoreDeletedEntity() {
            entity.softDelete("testUser");
            entity.restore();

            assertThat(entity.getEstado()).isTrue();
            assertThat(entity.getFechaEliminacion()).isNull();
            assertThat(entity.getEliminadoPor()).isNull();
            assertThat(entity.isDeleted()).isFalse();
            assertThat(entity.isActive()).isTrue();
        }

        @Test
        @DisplayName("isActive should return true for active entity")
        void isActiveShouldReturnTrueForActiveEntity() {
            assertThat(entity.isActive()).isTrue();
        }

        @Test
        @DisplayName("isDeleted should return false for active entity")
        void isDeletedShouldReturnFalseForActiveEntity() {
            assertThat(entity.isDeleted()).isFalse();
        }

        @Test
        @DisplayName("isDeleted should handle null estado")
        void isDeletedShouldHandleNullEstado() {
            entity.setEstado(null);
            assertThat(entity.isDeleted()).isTrue();
        }

        @Test
        @DisplayName("isActive should handle null estado")
        void isActiveShouldHandleNullEstado() {
            entity.setEstado(null);
            assertThat(entity.isActive()).isFalse();
        }
    }

    @Nested
    @DisplayName("Domain Events")
    class DomainEventsTests {

        @Test
        @DisplayName("should register domain event")
        void shouldRegisterDomainEvent() {
            DomainEvent event = new EntityCreatedEvent<>(entity);
            entity.registerEvent(event);

            assertThat(entity.getDomainEvents()).hasSize(1);
            assertThat(entity.getDomainEvents().get(0)).isEqualTo(event);
        }

        @Test
        @DisplayName("should register multiple domain events")
        void shouldRegisterMultipleDomainEvents() {
            DomainEvent event1 = new EntityCreatedEvent<>(entity);
            DomainEvent event2 = new EntityCreatedEvent<>(entity);

            entity.registerEvent(event1);
            entity.registerEvent(event2);

            assertThat(entity.getDomainEvents()).hasSize(2);
        }

        @Test
        @DisplayName("should clear domain events")
        void shouldClearDomainEvents() {
            entity.registerEvent(new EntityCreatedEvent<>(entity));
            entity.clearDomainEvents();

            assertThat(entity.getDomainEvents()).isEmpty();
        }

        @Test
        @DisplayName("getDomainEvents should return unmodifiable list")
        void getDomainEventsShouldReturnUnmodifiableList() {
            var events = entity.getDomainEvents();
            var event = new EntityCreatedEvent<>(entity);
            org.junit.jupiter.api.Assertions.assertThrows(
                    UnsupportedOperationException.class, () -> events.add(event));
        }
    }

    @Nested
    @DisplayName("Equals and HashCode")
    class EqualsHashCodeTests {

        @Test
        @DisplayName("should be equal to itself")
        void shouldBeEqualToItself() {
            entity.setId(1L);
            assertThat(entity).isEqualTo(entity);
        }

        @Test
        @DisplayName("should be equal when same id")
        void shouldBeEqualWhenSameId() {
            entity.setId(1L);
            TestableBase other = new TestableBase();
            other.setId(1L);

            assertThat(entity).isEqualTo(other);
        }

        @Test
        @DisplayName("should not be equal when different ids")
        void shouldNotBeEqualWhenDifferentIds() {
            entity.setId(1L);
            TestableBase other = new TestableBase();
            other.setId(2L);

            assertThat(entity).isNotEqualTo(other);
        }

        @Test
        @DisplayName("should not be equal to null")
        void shouldNotBeEqualToNull() {
            entity.setId(1L);
            assertThat(entity).isNotEqualTo(null);
        }

        @Test
        @DisplayName("should not be equal when both have null ids")
        void shouldNotBeEqualWhenBothHaveNullIds() {
            TestableBase other = new TestableBase();
            assertThat(entity).isNotEqualTo(other);
        }

        @Test
        @DisplayName("should not be equal when one has null id")
        void shouldNotBeEqualWhenOneHasNullId() {
            entity.setId(1L);
            TestableBase other = new TestableBase();
            assertThat(entity).isNotEqualTo(other);
        }

        @Test
        @DisplayName("should not be equal to different class")
        void shouldNotBeEqualToDifferentClass() {
            entity.setId(1L);
            assertThat(entity).isNotEqualTo("string");
        }

        @Test
        @DisplayName("should have consistent hashCode")
        void shouldHaveConsistentHashCode() {
            entity.setId(1L);
            int hash1 = entity.hashCode();
            int hash2 = entity.hashCode();
            assertThat(hash1).isEqualTo(hash2);
        }

        @Test
        @DisplayName("should have same hashCode for equal entities")
        void shouldHaveSameHashCodeForEqualEntities() {
            entity.setId(1L);
            TestableBase other = new TestableBase();
            other.setId(1L);

            assertThat(entity).hasSameHashCodeAs(other);
        }

        @Test
        @DisplayName("should have identity hashCode for null id")
        void shouldHaveIdentityHashCodeForNullId() {
            int hash = entity.hashCode();
            assertThat(hash).isEqualTo(System.identityHashCode(entity));
        }
    }

    @Nested
    @DisplayName("ToString")
    class ToStringTests {

        @Test
        @DisplayName("should include class name in toString")
        void shouldIncludeClassName() {
            String result = entity.toString();
            assertThat(result).contains("TestableBase");
        }

        @Test
        @DisplayName("should include id in toString")
        void shouldIncludeId() {
            entity.setId(123L);
            String result = entity.toString();
            assertThat(result).contains("id=123");
        }

        @Test
        @DisplayName("should include estado in toString")
        void shouldIncludeEstado() {
            String result = entity.toString();
            assertThat(result).contains("estado=true");
        }
    }

    @Nested
    @DisplayName("Setters and Getters")
    class SettersGettersTests {

        @Test
        @DisplayName("should set and get fechaCreacion")
        void shouldSetAndGetFechaCreacion() {
            LocalDateTime now = LocalDateTime.now();
            entity.setFechaCreacion(now);
            assertThat(entity.getFechaCreacion()).isEqualTo(now);
        }

        @Test
        @DisplayName("should set and get fechaActualizacion")
        void shouldSetAndGetFechaActualizacion() {
            LocalDateTime now = LocalDateTime.now();
            entity.setFechaActualizacion(now);
            assertThat(entity.getFechaActualizacion()).isEqualTo(now);
        }

        @Test
        @DisplayName("should set and get creadoPor")
        void shouldSetAndGetCreadoPor() {
            entity.setCreadoPor("creator");
            assertThat(entity.getCreadoPor()).isEqualTo("creator");
        }

        @Test
        @DisplayName("should set and get modificadoPor")
        void shouldSetAndGetModificadoPor() {
            entity.setModificadoPor("modifier");
            assertThat(entity.getModificadoPor()).isEqualTo("modifier");
        }
    }
}
