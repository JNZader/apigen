package com.jnzader.apigen.core.infrastructure.eventsourcing;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("EventSourcedAggregate Tests")
class EventSourcedAggregateTest {

    @Nested
    @DisplayName("Raise Events")
    class RaiseEventsTests {

        @Test
        @DisplayName("should raise event and update state")
        void shouldRaiseEventAndUpdateState() {
            TestAggregate aggregate = new TestAggregate();

            aggregate.create("agg-1", "Test Name");

            assertThat(aggregate.getId()).isEqualTo("agg-1");
            assertThat(aggregate.getName()).isEqualTo("Test Name");
            assertThat(aggregate.isActive()).isTrue();
        }

        @Test
        @DisplayName("should increment version on event")
        void shouldIncrementVersionOnEvent() {
            TestAggregate aggregate = new TestAggregate();

            aggregate.create("agg-1", "Test");
            assertThat(aggregate.getVersion()).isEqualTo(0);

            aggregate.rename("New Name");
            assertThat(aggregate.getVersion()).isEqualTo(1);

            aggregate.deactivate();
            assertThat(aggregate.getVersion()).isEqualTo(2);
        }

        @Test
        @DisplayName("should track uncommitted events")
        void shouldTrackUncommittedEvents() {
            TestAggregate aggregate = new TestAggregate();

            aggregate.create("agg-1", "Test");
            aggregate.rename("New Name");

            List<DomainEvent> uncommitted = aggregate.getUncommittedEvents();

            assertThat(uncommitted).hasSize(2);
            assertThat(uncommitted.get(0)).isInstanceOf(TestCreatedEvent.class);
            assertThat(uncommitted.get(1)).isInstanceOf(TestRenamedEvent.class);
        }

        @Test
        @DisplayName("should clear uncommitted events after commit")
        void shouldClearUncommittedEventsAfterCommit() {
            TestAggregate aggregate = new TestAggregate();
            aggregate.create("agg-1", "Test");

            assertThat(aggregate.hasUncommittedEvents()).isTrue();

            aggregate.markEventsAsCommitted();

            assertThat(aggregate.hasUncommittedEvents()).isFalse();
            assertThat(aggregate.getUncommittedEvents()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Load From History")
    class LoadFromHistoryTests {

        @Test
        @DisplayName("should replay events from history")
        void shouldReplayEventsFromHistory() {
            List<DomainEvent> history =
                    List.of(
                            new TestCreatedEvent("agg-1", "Original", Instant.now()),
                            new TestRenamedEvent("agg-1", "Renamed", Instant.now()),
                            new TestDeactivatedEvent("agg-1", Instant.now()));

            TestAggregate aggregate = new TestAggregate();
            aggregate.loadFromHistory(history);

            assertThat(aggregate.getId()).isEqualTo("agg-1");
            assertThat(aggregate.getName()).isEqualTo("Renamed");
            assertThat(aggregate.isActive()).isFalse();
            assertThat(aggregate.getVersion()).isEqualTo(2);
        }

        @Test
        @DisplayName("should not add history events to uncommitted")
        void shouldNotAddHistoryEventsToUncommitted() {
            List<DomainEvent> history = List.of(new TestCreatedEvent("agg-1", "Test", Instant.now()));

            TestAggregate aggregate = new TestAggregate();
            aggregate.loadFromHistory(history);

            assertThat(aggregate.hasUncommittedEvents()).isFalse();
        }

        @Test
        @DisplayName("should handle empty history")
        void shouldHandleEmptyHistory() {
            TestAggregate aggregate = new TestAggregate();
            aggregate.loadFromHistory(List.of());

            assertThat(aggregate.getId()).isNull();
            assertThat(aggregate.getVersion()).isEqualTo(-1);
        }
    }

    @Nested
    @DisplayName("Snapshot Support")
    class SnapshotSupportTests {

        @Test
        @DisplayName("should create snapshot of current state")
        void shouldCreateSnapshotOfCurrentState() {
            TestAggregate aggregate = new TestAggregate();
            aggregate.create("agg-1", "Test Name");
            aggregate.rename("Updated Name");

            Object snapshot = aggregate.createSnapshot();

            assertThat(snapshot).isInstanceOf(TestAggregate.SnapshotState.class);
            TestAggregate.SnapshotState state = (TestAggregate.SnapshotState) snapshot;
            assertThat(state.name()).isEqualTo("Updated Name");
            assertThat(state.active()).isTrue();
        }

        @Test
        @DisplayName("should restore from snapshot")
        void shouldRestoreFromSnapshot() {
            TestAggregate.SnapshotState snapshot = new TestAggregate.SnapshotState("agg-1", "Snapshot Name", false);

            TestAggregate aggregate = new TestAggregate();
            aggregate.restoreFromSnapshot(snapshot);

            assertThat(aggregate.getId()).isEqualTo("agg-1");
            assertThat(aggregate.getName()).isEqualTo("Snapshot Name");
            assertThat(aggregate.isActive()).isFalse();
        }

        @Test
        @DisplayName("should load from snapshot and subsequent events")
        void shouldLoadFromSnapshotAndSubsequentEvents() {
            TestAggregate aggregate = new TestAggregate();
            TestAggregate.SnapshotState snapshot = new TestAggregate.SnapshotState("agg-1", "Snapshot Name", true);
            aggregate.restoreFromSnapshot(snapshot);

            List<DomainEvent> subsequentEvents = List.of(new TestRenamedEvent("agg-1", "Final Name", Instant.now()));
            aggregate.loadFromSnapshot(5, subsequentEvents);

            assertThat(aggregate.getName()).isEqualTo("Final Name");
            assertThat(aggregate.getVersion()).isEqualTo(6); // 5 + 1 event
        }
    }

    @Nested
    @DisplayName("Utility Methods")
    class UtilityMethodsTests {

        @Test
        @DisplayName("should count uncommitted events")
        void shouldCountUncommittedEvents() {
            TestAggregate aggregate = new TestAggregate();

            assertThat(aggregate.getUncommittedEventCount()).isEqualTo(0);

            aggregate.create("agg-1", "Test");
            assertThat(aggregate.getUncommittedEventCount()).isEqualTo(1);

            aggregate.rename("New");
            assertThat(aggregate.getUncommittedEventCount()).isEqualTo(2);
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

        public void deactivate() {
            raiseEvent(new TestDeactivatedEvent(getId(), Instant.now()));
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
                case TestDeactivatedEvent e -> this.active = false;
                default -> {}
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

    record TestCreatedEvent(String aggregateId, String name, Instant occurredAt) implements DomainEvent {
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

    record TestRenamedEvent(String aggregateId, String newName, Instant occurredAt) implements DomainEvent {
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

    record TestDeactivatedEvent(String aggregateId, Instant occurredAt) implements DomainEvent {
        @Override
        public String getAggregateId() {
            return aggregateId;
        }

        @Override
        public String getEventType() {
            return "TestDeactivated";
        }

        @Override
        public Instant getOccurredAt() {
            return occurredAt;
        }
    }
}
