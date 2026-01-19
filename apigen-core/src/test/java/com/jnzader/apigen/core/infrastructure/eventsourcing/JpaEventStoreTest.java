package com.jnzader.apigen.core.infrastructure.eventsourcing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

@DisplayName("JpaEventStore Tests")
class JpaEventStoreTest {

    private StoredEventRepository eventRepository;
    private SnapshotRepository snapshotRepository;
    private EventSerializer serializer;
    private ApplicationEventPublisher eventPublisher;
    private JpaEventStore eventStore;

    @BeforeEach
    void setUp() {
        eventRepository = mock(StoredEventRepository.class);
        snapshotRepository = mock(SnapshotRepository.class);
        serializer = new EventSerializer();
        eventPublisher = mock(ApplicationEventPublisher.class);
        eventStore = new JpaEventStore(eventRepository, snapshotRepository, serializer, eventPublisher);
    }

    @Nested
    @DisplayName("Append Events")
    class AppendEventsTests {

        @Test
        @DisplayName("should append events to new aggregate")
        void shouldAppendEventsToNewAggregate() {
            when(eventRepository.findMaxVersionByAggregateId("agg-1")).thenReturn(Optional.empty());

            TestEvent event = new TestEvent("agg-1", "TestCreated", Instant.now(), "data");

            eventStore.append("agg-1", "TestAggregate", List.of(event), -1);

            verify(eventRepository).save(any(StoredEvent.class));
            verify(eventPublisher).publishEvent(any(JpaEventStore.StoredEventWrapper.class));
        }

        @Test
        @DisplayName("should append multiple events with sequential versions")
        void shouldAppendMultipleEventsWithSequentialVersions() {
            when(eventRepository.findMaxVersionByAggregateId("agg-1")).thenReturn(Optional.of(2L));

            TestEvent event1 = new TestEvent("agg-1", "Event1", Instant.now(), "data1");
            TestEvent event2 = new TestEvent("agg-1", "Event2", Instant.now(), "data2");

            eventStore.append("agg-1", "TestAggregate", List.of(event1, event2), 2);

            verify(eventRepository, times(2)).save(any(StoredEvent.class));
        }

        @Test
        @DisplayName("should throw ConcurrencyException on version mismatch")
        void shouldThrowConcurrencyExceptionOnVersionMismatch() {
            when(eventRepository.findMaxVersionByAggregateId("agg-1")).thenReturn(Optional.of(5L));

            TestEvent event = new TestEvent("agg-1", "TestCreated", Instant.now(), "data");

            assertThatThrownBy(() -> eventStore.append("agg-1", "TestAggregate", List.of(event), 3))
                    .isInstanceOf(ConcurrencyException.class)
                    .hasMessageContaining("expected version 3 but found 5");
        }

        @Test
        @DisplayName("should not throw for new aggregate with expected version -1")
        void shouldNotThrowForNewAggregateWithExpectedVersionMinusOne() {
            when(eventRepository.findMaxVersionByAggregateId("agg-1")).thenReturn(Optional.empty());

            TestEvent event = new TestEvent("agg-1", "TestCreated", Instant.now(), "data");

            eventStore.append("agg-1", "TestAggregate", List.of(event), -1);

            verify(eventRepository).save(any(StoredEvent.class));
        }

        @Test
        @DisplayName("should skip empty event list")
        void shouldSkipEmptyEventList() {
            eventStore.append("agg-1", "TestAggregate", List.of(), 0);

            verify(eventRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Get Events")
    class GetEventsTests {

        @Test
        @DisplayName("should get all events for aggregate")
        void shouldGetAllEventsForAggregate() {
            StoredEvent stored =
                    StoredEvent.builder()
                            .eventId("evt-1")
                            .aggregateId("agg-1")
                            .aggregateType("Test")
                            .eventType("TestCreated")
                            .version(1)
                            .payload("{}")
                            .occurredAt(Instant.now())
                            .build();
            when(eventRepository.findByAggregateIdOrderByVersionAsc("agg-1")).thenReturn(List.of(stored));

            List<StoredEvent> events = eventStore.getEvents("agg-1");

            assertThat(events).hasSize(1);
            assertThat(events.get(0).getEventId()).isEqualTo("evt-1");
        }

        @Test
        @DisplayName("should get events from specific version")
        void shouldGetEventsFromSpecificVersion() {
            StoredEvent stored =
                    StoredEvent.builder()
                            .eventId("evt-2")
                            .aggregateId("agg-1")
                            .aggregateType("Test")
                            .eventType("TestUpdated")
                            .version(2)
                            .payload("{}")
                            .occurredAt(Instant.now())
                            .build();
            when(eventRepository.findByAggregateIdAndVersionGreaterThanOrderByVersionAsc("agg-1", 1L))
                    .thenReturn(List.of(stored));

            List<StoredEvent> events = eventStore.getEventsFrom("agg-1", 1);

            assertThat(events).hasSize(1);
            assertThat(events.get(0).getVersion()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Version and Existence")
    class VersionAndExistenceTests {

        @Test
        @DisplayName("should return current version")
        void shouldReturnCurrentVersion() {
            when(eventRepository.findMaxVersionByAggregateId("agg-1")).thenReturn(Optional.of(5L));

            long version = eventStore.getCurrentVersion("agg-1");

            assertThat(version).isEqualTo(5);
        }

        @Test
        @DisplayName("should return -1 for non-existent aggregate")
        void shouldReturnMinusOneForNonExistentAggregate() {
            when(eventRepository.findMaxVersionByAggregateId("agg-1")).thenReturn(Optional.empty());

            long version = eventStore.getCurrentVersion("agg-1");

            assertThat(version).isEqualTo(-1);
        }

        @Test
        @DisplayName("should check if aggregate exists")
        void shouldCheckIfAggregateExists() {
            when(eventRepository.existsByAggregateId("agg-1")).thenReturn(true);
            when(eventRepository.existsByAggregateId("agg-2")).thenReturn(false);

            assertThat(eventStore.exists("agg-1")).isTrue();
            assertThat(eventStore.exists("agg-2")).isFalse();
        }
    }

    @Nested
    @DisplayName("Snapshots")
    class SnapshotTests {

        @Test
        @DisplayName("should save snapshot")
        void shouldSaveSnapshot() {
            Snapshot snapshot =
                    Snapshot.builder()
                            .aggregateId("agg-1")
                            .aggregateType("Test")
                            .version(10)
                            .state("{\"name\":\"test\"}")
                            .build();

            eventStore.saveSnapshot(snapshot);

            verify(snapshotRepository).save(snapshot);
        }

        @Test
        @DisplayName("should get latest snapshot")
        void shouldGetLatestSnapshot() {
            Snapshot snapshot =
                    Snapshot.builder()
                            .aggregateId("agg-1")
                            .aggregateType("Test")
                            .version(10)
                            .state("{}")
                            .build();
            when(snapshotRepository.findFirstByAggregateIdOrderByVersionDesc("agg-1"))
                    .thenReturn(Optional.of(snapshot));

            Optional<Snapshot> result = eventStore.getLatestSnapshot("agg-1");

            assertThat(result).isPresent();
            assertThat(result.get().getVersion()).isEqualTo(10);
        }

        @Test
        @DisplayName("should return empty when no snapshot exists")
        void shouldReturnEmptyWhenNoSnapshotExists() {
            when(snapshotRepository.findFirstByAggregateIdOrderByVersionDesc("agg-1"))
                    .thenReturn(Optional.empty());

            Optional<Snapshot> result = eventStore.getLatestSnapshot("agg-1");

            assertThat(result).isEmpty();
        }
    }

    // Test helper

    record TestEvent(String aggregateId, String eventType, Instant occurredAt, String data) implements DomainEvent {
        @Override
        public String getAggregateId() {
            return aggregateId;
        }

        @Override
        public String getEventType() {
            return eventType;
        }

        @Override
        public Instant getOccurredAt() {
            return occurredAt;
        }
    }
}
