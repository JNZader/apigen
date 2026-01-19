package com.jnzader.apigen.core.infrastructure.eventsourcing;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Auto-configuration for event sourcing infrastructure.
 *
 * <p>Enabled by setting {@code apigen.eventsourcing.enabled=true}.
 *
 * <p>Provides:
 *
 * <ul>
 *   <li>{@link EventSerializer} for JSON serialization of events
 *   <li>{@link EventStore} JPA implementation for event persistence
 *   <li>JPA repositories for stored events and snapshots
 * </ul>
 *
 * <p>Configuration properties:
 *
 * <pre>{@code
 * apigen:
 *   eventsourcing:
 *     enabled: true
 * }</pre>
 *
 * <p>Note: Ensure your application scans the eventsourcing package for JPA entities.
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "apigen.eventsourcing", name = "enabled", havingValue = "true")
@EnableJpaRepositories(basePackageClasses = {StoredEventRepository.class, SnapshotRepository.class})
public class EventSourcingAutoConfiguration {

    /**
     * Creates the event serializer bean.
     *
     * @param objectMapper optional ObjectMapper from context
     * @return the event serializer
     */
    @Bean
    @ConditionalOnMissingBean
    public EventSerializer eventSerializer(ObjectMapper objectMapper) {
        return new EventSerializer(objectMapper);
    }

    /**
     * Creates the event store bean.
     *
     * @param eventRepository the stored event repository
     * @param snapshotRepository the snapshot repository
     * @param serializer the event serializer
     * @param eventPublisher Spring's event publisher
     * @return the JPA event store
     */
    @Bean
    @ConditionalOnMissingBean
    public EventStore eventStore(
            StoredEventRepository eventRepository,
            SnapshotRepository snapshotRepository,
            EventSerializer serializer,
            ApplicationEventPublisher eventPublisher) {
        return new JpaEventStore(eventRepository, snapshotRepository, serializer, eventPublisher);
    }
}
