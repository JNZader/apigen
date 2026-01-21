package com.jnzader.apigen.core.infrastructure.eventsourcing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.Instant;
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

@ExtendWith(MockitoExtension.class)
@DisplayName("AggregateRepository Tests")
class AggregateRepositoryTest {

    @Mock private EventStore eventStore;

    @Mock private EventSerializer serializer;

    @Captor private ArgumentCaptor<Snapshot> snapshotCaptor;

    private AggregateRepository<TestAggregate> repository;

    @BeforeEach
    void setUp() {
        repository =
                new AggregateRepository<>(
                        eventStore, serializer, "TestAggregate", TestAggregate::new);
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("should create repository without snapshot support")
        void shouldCreateRepositoryWithoutSnapshotSupport() {
            AggregateRepository<TestAggregate> repo =
                    new AggregateRepository<>(
                            eventStore, serializer, "TestAggregate", TestAggregate::new);

            assertThat(repo).isNotNull();
        }

        @Test
        @DisplayName("should create repository with snapshot support")
        void shouldCreateRepositoryWithSnapshotSupport() {
            AggregateRepository<TestAggregate> repo =
                    new AggregateRepository<>(
                            eventStore, serializer, "TestAggregate", TestAggregate::new, 10);

            assertThat(repo).isNotNull();
        }
    }

    @Nested
    @DisplayName("Save Tests")
    class SaveTests {

        @Test
        @DisplayName("should save aggregate with uncommitted events")
        void shouldSaveAggregateWithUncommittedEvents() {
            TestAggregate aggregate = new TestAggregate();
            aggregate.create("agg-1", "Test Name");

            repository.save(aggregate);

            verify(eventStore).append(eq("agg-1"), eq("TestAggregate"), any(List.class), eq(-1L));
            assertThat(aggregate.hasUncommittedEvents()).isFalse();
        }

        @Test
        @DisplayName("should not save aggregate without uncommitted events")
        void shouldNotSaveAggregateWithoutUncommittedEvents() {
            TestAggregate aggregate = new TestAggregate();
            aggregate.create("agg-1", "Test Name");
            aggregate.markEventsAsCommitted();

            repository.save(aggregate);

            verify(eventStore, never()).append(any(), any(), any(), anyLong());
        }

        @Test
        @DisplayName("should calculate expected version correctly")
        void shouldCalculateExpectedVersionCorrectly() {
            TestAggregate aggregate = new TestAggregate();
            aggregate.create("agg-1", "Test");
            aggregate.markEventsAsCommitted();
            aggregate.rename("New Name");
            aggregate.rename("Another Name");

            repository.save(aggregate);

            // Version is 2 (0 from create + 2 renames), uncommitted count is 2
            // Expected version should be 2 - 2 = 0
            verify(eventStore).append(eq("agg-1"), eq("TestAggregate"), any(List.class), eq(0L));
        }

        @Test
        @DisplayName("should create snapshot when frequency is reached")
        void shouldCreateSnapshotWhenFrequencyIsReached() {
            AggregateRepository<TestAggregate> repoWithSnapshots =
                    new AggregateRepository<>(
                            eventStore, serializer, "TestAggregate", TestAggregate::new, 2);

            TestAggregate aggregate = new TestAggregate();
            aggregate.create("agg-1", "Test"); // version 0
            aggregate.rename("Name 1"); // version 1
            aggregate.markEventsAsCommitted();
            aggregate.rename("Name 2"); // version 2 - divisible by snapshot frequency (2)

            when(serializer.serializeState(any())).thenReturn("{\"state\":\"data\"}");

            repoWithSnapshots.save(aggregate);

            verify(eventStore).saveSnapshot(any(Snapshot.class));
        }

        @Test
        @DisplayName("should not create snapshot when frequency not reached")
        void shouldNotCreateSnapshotWhenFrequencyNotReached() {
            AggregateRepository<TestAggregate> repoWithSnapshots =
                    new AggregateRepository<>(
                            eventStore, serializer, "TestAggregate", TestAggregate::new, 5);

            TestAggregate aggregate = new TestAggregate();
            aggregate.create("agg-1", "Test");

            repoWithSnapshots.save(aggregate);

            verify(eventStore, never()).saveSnapshot(any(Snapshot.class));
        }
    }

    @Nested
    @DisplayName("FindById Tests")
    class FindByIdTests {

        @Test
        @DisplayName("should return empty when aggregate does not exist")
        void shouldReturnEmptyWhenAggregateDoesNotExist() {
            when(eventStore.exists("agg-1")).thenReturn(false);

            Optional<TestAggregate> result = repository.findById("agg-1");

            assertThat(result).isEmpty();
            verify(eventStore, never()).getEvents(any());
        }

        @Test
        @DisplayName("should load aggregate from events when exists")
        void shouldLoadAggregateFromEventsWhenExists() {
            when(eventStore.exists("agg-1")).thenReturn(true);

            StoredEvent storedEvent =
                    StoredEvent.builder()
                            .eventId("evt-1")
                            .aggregateId("agg-1")
                            .aggregateType("TestAggregate")
                            .eventType("TestCreated")
                            .version(1)
                            .payload("{\"name\":\"Test\"}")
                            .occurredAt(Instant.now())
                            .build();
            when(eventStore.getEvents("agg-1")).thenReturn(List.of(storedEvent));

            TestCreatedEvent event = new TestCreatedEvent("agg-1", "Test", Instant.now());
            when(serializer.deserialize("{\"name\":\"Test\"}", "TestCreated")).thenReturn(event);

            Optional<TestAggregate> result = repository.findById("agg-1");

            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo("agg-1");
            assertThat(result.get().getName()).isEqualTo("Test");
        }

        @Test
        @DisplayName("should load aggregate from snapshot when available")
        void shouldLoadAggregateFromSnapshotWhenAvailable() {
            AggregateRepository<TestAggregate> repoWithSnapshots =
                    new AggregateRepository<>(
                            eventStore, serializer, "TestAggregate", TestAggregate::new, 5);

            when(eventStore.exists("agg-1")).thenReturn(true);

            Snapshot snapshot =
                    Snapshot.builder()
                            .aggregateId("agg-1")
                            .aggregateType("TestAggregate")
                            .version(5)
                            .state("{\"id\":\"agg-1\",\"name\":\"Snapshot\",\"active\":true}")
                            .build();
            when(eventStore.getLatestSnapshot("agg-1")).thenReturn(Optional.of(snapshot));

            TestAggregate.SnapshotState snapshotState =
                    new TestAggregate.SnapshotState("agg-1", "Snapshot", true);
            when(serializer.deserializeState(any(), any())).thenReturn(snapshotState);

            when(eventStore.getEventsFrom("agg-1", 5)).thenReturn(List.of());

            Optional<TestAggregate> result = repoWithSnapshots.findById("agg-1");

            assertThat(result).isPresent();
            verify(eventStore).getLatestSnapshot("agg-1");
            verify(eventStore).getEventsFrom("agg-1", 5);
            verify(eventStore, never()).getEvents("agg-1");
        }

        @Test
        @DisplayName("should load events after snapshot")
        void shouldLoadEventsAfterSnapshot() {
            AggregateRepository<TestAggregate> repoWithSnapshots =
                    new AggregateRepository<>(
                            eventStore, serializer, "TestAggregate", TestAggregate::new, 5);

            when(eventStore.exists("agg-1")).thenReturn(true);

            Snapshot snapshot =
                    Snapshot.builder()
                            .aggregateId("agg-1")
                            .aggregateType("TestAggregate")
                            .version(5)
                            .state("{}")
                            .build();
            when(eventStore.getLatestSnapshot("agg-1")).thenReturn(Optional.of(snapshot));

            TestAggregate.SnapshotState snapshotState =
                    new TestAggregate.SnapshotState("agg-1", "Initial", true);
            when(serializer.deserializeState(any(), any())).thenReturn(snapshotState);

            StoredEvent eventAfterSnapshot =
                    StoredEvent.builder()
                            .eventId("evt-6")
                            .aggregateId("agg-1")
                            .aggregateType("TestAggregate")
                            .eventType("TestRenamed")
                            .version(6)
                            .payload("{\"newName\":\"Updated\"}")
                            .occurredAt(Instant.now())
                            .build();
            when(eventStore.getEventsFrom("agg-1", 5)).thenReturn(List.of(eventAfterSnapshot));

            TestRenamedEvent renamedEvent = new TestRenamedEvent("agg-1", "Updated", Instant.now());
            when(serializer.deserialize("{\"newName\":\"Updated\"}", "TestRenamed"))
                    .thenReturn(renamedEvent);

            Optional<TestAggregate> result = repoWithSnapshots.findById("agg-1");

            assertThat(result).isPresent();
            assertThat(result.get().getName()).isEqualTo("Updated");
        }

        @Test
        @DisplayName("should fall back to events when no snapshot exists")
        void shouldFallBackToEventsWhenNoSnapshotExists() {
            AggregateRepository<TestAggregate> repoWithSnapshots =
                    new AggregateRepository<>(
                            eventStore, serializer, "TestAggregate", TestAggregate::new, 5);

            when(eventStore.exists("agg-1")).thenReturn(true);
            when(eventStore.getLatestSnapshot("agg-1")).thenReturn(Optional.empty());

            StoredEvent storedEvent =
                    StoredEvent.builder()
                            .eventId("evt-1")
                            .aggregateId("agg-1")
                            .aggregateType("TestAggregate")
                            .eventType("TestCreated")
                            .version(1)
                            .payload("{}")
                            .occurredAt(Instant.now())
                            .build();
            when(eventStore.getEvents("agg-1")).thenReturn(List.of(storedEvent));

            TestCreatedEvent event = new TestCreatedEvent("agg-1", "Test", Instant.now());
            when(serializer.deserialize("{}", "TestCreated")).thenReturn(event);

            Optional<TestAggregate> result = repoWithSnapshots.findById("agg-1");

            assertThat(result).isPresent();
            verify(eventStore).getEvents("agg-1");
        }
    }

    @Nested
    @DisplayName("Exists Tests")
    class ExistsTests {

        @Test
        @DisplayName("should return true when aggregate exists")
        void shouldReturnTrueWhenAggregateExists() {
            when(eventStore.exists("agg-1")).thenReturn(true);

            boolean result = repository.exists("agg-1");

            assertThat(result).isTrue();
            verify(eventStore).exists("agg-1");
        }

        @Test
        @DisplayName("should return false when aggregate does not exist")
        void shouldReturnFalseWhenAggregateDoesNotExist() {
            when(eventStore.exists("agg-1")).thenReturn(false);

            boolean result = repository.exists("agg-1");

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("GetVersion Tests")
    class GetVersionTests {

        @Test
        @DisplayName("should return current version from event store")
        void shouldReturnCurrentVersionFromEventStore() {
            when(eventStore.getCurrentVersion("agg-1")).thenReturn(5L);

            long version = repository.getVersion("agg-1");

            assertThat(version).isEqualTo(5L);
            verify(eventStore).getCurrentVersion("agg-1");
        }

        @Test
        @DisplayName("should return -1 when aggregate does not exist")
        void shouldReturnMinusOneWhenAggregateDoesNotExist() {
            when(eventStore.getCurrentVersion("agg-1")).thenReturn(-1L);

            long version = repository.getVersion("agg-1");

            assertThat(version).isEqualTo(-1L);
        }
    }

    // Test implementation classes

    static class TestAggregate extends EventSourcedAggregate {
        private String name;
        private boolean active;

        public void create(String id, String name) {
            raiseEvent(new TestCreatedEvent(id, name, Instant.now()));
        }

        public void rename(String newName) {
            raiseEvent(new TestRenamedEvent(getId(), newName, Instant.now()));
        }

        @Override
        protected void apply(DomainEvent event) {
            switch (event) {
                case TestCreatedEvent e -> {
                    setId(e.aggregateId());
                    this.name = e.name();
                    this.active = true;
                }
                case TestRenamedEvent e -> this.name = e.newName();
                default -> {
                    // No action needed for unknown event types
                }
            }
        }

        public String getName() {
            return name;
        }

        public boolean isActive() {
            return active;
        }

        @Override
        public Object createSnapshot() {
            return new SnapshotState(getId(), name, active);
        }

        @Override
        public void restoreFromSnapshot(Object snapshot) {
            if (snapshot instanceof SnapshotState state) {
                setId(state.id());
                this.name = state.name();
                this.active = state.active();
            }
        }

        public record SnapshotState(String id, String name, boolean active) {}
    }

    record TestCreatedEvent(String aggregateId, String name, Instant occurredAt)
            implements DomainEvent {
        @Override
        public String getAggregateId() {
            return aggregateId;
        }

        @Override
        public String getEventType() {
            return "TestCreated";
        }

        @Override
        public Instant getOccurredAt() {
            return occurredAt;
        }
    }

    record TestRenamedEvent(String aggregateId, String newName, Instant occurredAt)
            implements DomainEvent {
        @Override
        public String getAggregateId() {
            return aggregateId;
        }

        @Override
        public String getEventType() {
            return "TestRenamed";
        }

        @Override
        public Instant getOccurredAt() {
            return occurredAt;
        }
    }
}
