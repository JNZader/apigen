package com.jnzader.apigen.core.domain.event;

import static org.assertj.core.api.Assertions.*;

import com.jnzader.apigen.core.fixtures.TestEntity;
import com.jnzader.apigen.core.infrastructure.event.handler.DomainEventHandler;
import com.jnzader.apigen.core.support.TestEntityBuilder;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests para eventos de dominio.
 *
 * <p>Verifica: - Creación correcta de eventos - Datos del evento - Manejo de eventos por handlers -
 * Métricas de eventos
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Domain Event Tests")
class DomainEventTest {

    private TestEntity testEntity;
    private MeterRegistry meterRegistry;
    private DomainEventHandler eventHandler;

    @BeforeEach
    void setUp() {
        TestEntityBuilder.resetIdCounter();
        testEntity =
                TestEntityBuilder.aTestEntityWithId()
                        .withName("Test Entity")
                        .withCreadoPor("test-user")
                        .build();
        meterRegistry = new SimpleMeterRegistry();
        eventHandler = new DomainEventHandler(meterRegistry);
    }

    // ==================== EntityCreatedEvent Tests ====================

    @Nested
    @DisplayName("EntityCreatedEvent")
    class EntityCreatedEventTests {

        @Test
        @DisplayName("should create event with entity and user")
        void shouldCreateEventWithEntityAndUser() {
            // When
            EntityCreatedEvent<TestEntity> event = new EntityCreatedEvent<>(testEntity, "creator");

            // Then
            assertThat(event.entity()).isEqualTo(testEntity);
            assertThat(event.createdBy()).isEqualTo("creator");
            assertThat(event.occurredOn()).isNotNull();
            assertThat(event.eventType()).isEqualTo("EntityCreatedEvent");
        }

        @Test
        @DisplayName("should create event with current timestamp")
        void shouldCreateEventWithCurrentTimestamp() {
            // Given
            LocalDateTime before = LocalDateTime.now();

            // When
            EntityCreatedEvent<TestEntity> event = new EntityCreatedEvent<>(testEntity, "creator");

            // Then
            LocalDateTime after = LocalDateTime.now();
            assertThat(event.occurredOn()).isAfterOrEqualTo(before).isBeforeOrEqualTo(after);
        }

        @Test
        @DisplayName("should create event without user")
        void shouldCreateEventWithoutUser() {
            // When
            EntityCreatedEvent<TestEntity> event = new EntityCreatedEvent<>(testEntity);

            // Then
            assertThat(event.createdBy()).isNull();
        }
    }

    // ==================== EntityUpdatedEvent Tests ====================

    @Nested
    @DisplayName("EntityUpdatedEvent")
    class EntityUpdatedEventTests {

        @Test
        @DisplayName("should create update event")
        void shouldCreateUpdateEvent() {
            // When
            EntityUpdatedEvent<TestEntity> event = new EntityUpdatedEvent<>(testEntity, "updater");

            // Then
            assertThat(event.entity()).isEqualTo(testEntity);
            assertThat(event.updatedBy()).isEqualTo("updater");
            assertThat(event.eventType()).isEqualTo("EntityUpdatedEvent");
        }
    }

    // ==================== EntityDeletedEvent Tests ====================

    @Nested
    @DisplayName("EntityDeletedEvent")
    class EntityDeletedEventTests {

        @Test
        @DisplayName("should create soft delete event by default")
        void shouldCreateSoftDeleteEventByDefault() {
            // When
            EntityDeletedEvent<TestEntity> event = new EntityDeletedEvent<>(testEntity, "deleter");

            // Then
            assertThat(event.entity()).isEqualTo(testEntity);
            assertThat(event.deletedBy()).isEqualTo("deleter");
            assertThat(event.softDelete()).isTrue();
        }

        @Test
        @DisplayName("should create hard delete event when specified")
        void shouldCreateHardDeleteEventWhenSpecified() {
            // When
            EntityDeletedEvent<TestEntity> event =
                    new EntityDeletedEvent<>(testEntity, "deleter", false);

            // Then
            assertThat(event.softDelete()).isFalse();
        }

        @Test
        @DisplayName("should create event without user")
        void shouldCreateEventWithoutUser() {
            // When
            EntityDeletedEvent<TestEntity> event = new EntityDeletedEvent<>(testEntity);

            // Then
            assertThat(event.deletedBy()).isNull();
            assertThat(event.softDelete()).isTrue();
        }
    }

    // ==================== EntityRestoredEvent Tests ====================

    @Nested
    @DisplayName("EntityRestoredEvent")
    class EntityRestoredEventTests {

        @Test
        @DisplayName("should create restore event")
        void shouldCreateRestoreEvent() {
            // When
            EntityRestoredEvent<TestEntity> event = new EntityRestoredEvent<>(testEntity);

            // Then
            assertThat(event.entity()).isEqualTo(testEntity);
            assertThat(event.eventType()).isEqualTo("EntityRestoredEvent");
        }
    }

    // ==================== EntityHardDeletedEvent Tests ====================

    @Nested
    @DisplayName("EntityHardDeletedEvent")
    class EntityHardDeletedEventTests {

        @Test
        @DisplayName("should create hard delete event with all parameters")
        void shouldCreateHardDeleteEventWithAllParameters() {
            // When
            EntityHardDeletedEvent<Long> event =
                    new EntityHardDeletedEvent<>(1L, "TestEntity", "deleter");

            // Then
            assertThat(event.entityId()).isEqualTo(1L);
            assertThat(event.entityType()).isEqualTo("TestEntity");
            assertThat(event.deletedBy()).isEqualTo("deleter");
            assertThat(event.occurredOn()).isNotNull();
            assertThat(event.eventType()).isEqualTo("EntityHardDeletedEvent");
        }

        @Test
        @DisplayName("should create hard delete event without user")
        void shouldCreateHardDeleteEventWithoutUser() {
            // When
            EntityHardDeletedEvent<Long> event = new EntityHardDeletedEvent<>(1L, "TestEntity");

            // Then
            assertThat(event.entityId()).isEqualTo(1L);
            assertThat(event.entityType()).isEqualTo("TestEntity");
            assertThat(event.deletedBy()).isNull();
        }
    }

    // ==================== DomainEventHandler Tests ====================

    @Nested
    @DisplayName("DomainEventHandler")
    class DomainEventHandlerTests {

        @Test
        @DisplayName("should increment created counter on EntityCreatedEvent")
        void shouldIncrementCreatedCounterOnEntityCreatedEvent() {
            // Given
            EntityCreatedEvent<TestEntity> event = new EntityCreatedEvent<>(testEntity, "creator");
            double countBefore = meterRegistry.counter("domain.events.created").count();

            // When
            eventHandler.handleEntityCreated(event);

            // Then
            double countAfter = meterRegistry.counter("domain.events.created").count();
            assertThat(countAfter).isEqualTo(countBefore + 1);
        }

        @Test
        @DisplayName("should increment updated counter on EntityUpdatedEvent")
        void shouldIncrementUpdatedCounterOnEntityUpdatedEvent() {
            // Given
            EntityUpdatedEvent<TestEntity> event = new EntityUpdatedEvent<>(testEntity, "updater");
            double countBefore = meterRegistry.counter("domain.events.updated").count();

            // When
            eventHandler.handleEntityUpdated(event);

            // Then
            double countAfter = meterRegistry.counter("domain.events.updated").count();
            assertThat(countAfter).isEqualTo(countBefore + 1);
        }

        @Test
        @DisplayName("should increment deleted counter on EntityDeletedEvent")
        void shouldIncrementDeletedCounterOnEntityDeletedEvent() {
            // Given
            EntityDeletedEvent<TestEntity> event = new EntityDeletedEvent<>(testEntity, "deleter");
            double countBefore = meterRegistry.counter("domain.events.deleted").count();

            // When
            eventHandler.handleEntityDeleted(event);

            // Then
            double countAfter = meterRegistry.counter("domain.events.deleted").count();
            assertThat(countAfter).isEqualTo(countBefore + 1);
        }

        @Test
        @DisplayName("should increment restored counter on EntityRestoredEvent")
        void shouldIncrementRestoredCounterOnEntityRestoredEvent() {
            // Given
            EntityRestoredEvent<TestEntity> event = new EntityRestoredEvent<>(testEntity);
            double countBefore = meterRegistry.counter("domain.events.restored").count();

            // When
            eventHandler.handleEntityRestored(event);

            // Then
            double countAfter = meterRegistry.counter("domain.events.restored").count();
            assertThat(countAfter).isEqualTo(countBefore + 1);
        }
    }

    // ==================== DomainEvent Interface Tests ====================

    @Nested
    @DisplayName("DomainEvent Interface")
    class DomainEventInterfaceTests {

        @Test
        @DisplayName("all events should implement DomainEvent")
        void allEventsShouldImplementDomainEvent() {
            // When
            EntityCreatedEvent<?> created = new EntityCreatedEvent<>(testEntity);
            EntityUpdatedEvent<?> updated = new EntityUpdatedEvent<>(testEntity, "user");
            EntityDeletedEvent<?> deleted = new EntityDeletedEvent<>(testEntity);
            EntityRestoredEvent<?> restored = new EntityRestoredEvent<>(testEntity);

            // Then
            assertThat(created).isInstanceOf(DomainEvent.class);
            assertThat(updated).isInstanceOf(DomainEvent.class);
            assertThat(deleted).isInstanceOf(DomainEvent.class);
            assertThat(restored).isInstanceOf(DomainEvent.class);
        }

        @Test
        @DisplayName("eventType should return class simple name")
        void eventTypeShouldReturnClassSimpleName() {
            // When
            DomainEvent event = new EntityCreatedEvent<>(testEntity);

            // Then
            assertThat(event.eventType()).isEqualTo("EntityCreatedEvent");
        }
    }
}
