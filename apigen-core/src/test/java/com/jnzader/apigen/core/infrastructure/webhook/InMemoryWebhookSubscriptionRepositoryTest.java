package com.jnzader.apigen.core.infrastructure.webhook;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("InMemoryWebhookSubscriptionRepository Tests")
class InMemoryWebhookSubscriptionRepositoryTest {

    private InMemoryWebhookSubscriptionRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryWebhookSubscriptionRepository();
    }

    @Nested
    @DisplayName("Save Tests")
    class SaveTests {

        @Test
        @DisplayName("should save and return subscription")
        void shouldSaveAndReturnSubscription() {
            WebhookSubscription subscription = createSubscription("sub-1", "Test");

            WebhookSubscription saved = repository.save(subscription);

            assertThat(saved).isSameAs(subscription);
            assertThat(repository.findById("sub-1")).isPresent();
        }

        @Test
        @DisplayName("should overwrite existing subscription with same id")
        void shouldOverwriteExistingSubscription() {
            WebhookSubscription original = createSubscription("sub-1", "Original");
            WebhookSubscription updated = createSubscription("sub-1", "Updated");

            repository.save(original);
            repository.save(updated);

            Optional<WebhookSubscription> found = repository.findById("sub-1");
            assertThat(found).isPresent();
            assertThat(found.get().name()).isEqualTo("Updated");
        }
    }

    @Nested
    @DisplayName("FindById Tests")
    class FindByIdTests {

        @Test
        @DisplayName("should find existing subscription")
        void shouldFindExistingSubscription() {
            WebhookSubscription subscription = createSubscription("sub-1", "Test");
            repository.save(subscription);

            Optional<WebhookSubscription> result = repository.findById("sub-1");

            assertThat(result).isPresent().contains(subscription);
        }

        @Test
        @DisplayName("should return empty for non-existent subscription")
        void shouldReturnEmptyForNonExistent() {
            Optional<WebhookSubscription> result = repository.findById("non-existent");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("FindByEvent Tests")
    class FindByEventTests {

        @Test
        @DisplayName("should find subscriptions interested in event")
        void shouldFindSubscriptionsInterestedInEvent() {
            WebhookSubscription sub1 =
                    WebhookSubscription.builder()
                            .id("sub-1")
                            .name("Sub 1")
                            .url("http://example.com/1")
                            .secret("secret")
                            .events(Set.of(WebhookEvent.ENTITY_CREATED))
                            .active(true)
                            .build();
            WebhookSubscription sub2 =
                    WebhookSubscription.builder()
                            .id("sub-2")
                            .name("Sub 2")
                            .url("http://example.com/2")
                            .secret("secret")
                            .events(Set.of(WebhookEvent.ENTITY_DELETED))
                            .active(true)
                            .build();

            repository.save(sub1);
            repository.save(sub2);

            List<WebhookSubscription> result = repository.findByEvent(WebhookEvent.ENTITY_CREATED);

            assertThat(result).hasSize(1).contains(sub1);
        }

        @Test
        @DisplayName("should return empty list when no subscriptions interested")
        void shouldReturnEmptyWhenNoSubscriptionsInterested() {
            WebhookSubscription subscription =
                    WebhookSubscription.builder()
                            .id("sub-1")
                            .name("Sub 1")
                            .url("http://example.com")
                            .secret("secret")
                            .events(Set.of(WebhookEvent.ENTITY_CREATED))
                            .active(true)
                            .build();
            repository.save(subscription);

            List<WebhookSubscription> result = repository.findByEvent(WebhookEvent.ENTITY_DELETED);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should exclude inactive subscriptions")
        void shouldExcludeInactiveSubscriptions() {
            WebhookSubscription inactive =
                    WebhookSubscription.builder()
                            .id("sub-1")
                            .name("Inactive")
                            .url("http://example.com")
                            .secret("secret")
                            .events(Set.of(WebhookEvent.ENTITY_CREATED))
                            .active(false)
                            .build();
            repository.save(inactive);

            List<WebhookSubscription> result = repository.findByEvent(WebhookEvent.ENTITY_CREATED);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("FindAll Tests")
    class FindAllTests {

        @Test
        @DisplayName("should return all subscriptions")
        void shouldReturnAllSubscriptions() {
            WebhookSubscription sub1 = createSubscription("sub-1", "Sub 1");
            WebhookSubscription sub2 = createSubscription("sub-2", "Sub 2");
            WebhookSubscription sub3 = createSubscription("sub-3", "Sub 3");

            repository.save(sub1);
            repository.save(sub2);
            repository.save(sub3);

            List<WebhookSubscription> result = repository.findAll();

            assertThat(result).hasSize(3).containsExactlyInAnyOrder(sub1, sub2, sub3);
        }

        @Test
        @DisplayName("should return empty list when no subscriptions")
        void shouldReturnEmptyListWhenNoSubscriptions() {
            List<WebhookSubscription> result = repository.findAll();

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return immutable copy")
        void shouldReturnImmutableCopy() {
            repository.save(createSubscription("sub-1", "Test"));

            List<WebhookSubscription> result = repository.findAll();

            assertThat(result).isUnmodifiable();
        }
    }

    @Nested
    @DisplayName("DeleteById Tests")
    class DeleteByIdTests {

        @Test
        @DisplayName("should delete existing subscription")
        void shouldDeleteExistingSubscription() {
            repository.save(createSubscription("sub-1", "Test"));

            repository.deleteById("sub-1");

            assertThat(repository.findById("sub-1")).isEmpty();
        }

        @Test
        @DisplayName("should do nothing when deleting non-existent subscription")
        void shouldDoNothingWhenDeletingNonExistent() {
            repository.save(createSubscription("sub-1", "Test"));

            repository.deleteById("non-existent");

            assertThat(repository.findById("sub-1")).isPresent();
        }
    }

    @Nested
    @DisplayName("ExistsById Tests")
    class ExistsByIdTests {

        @Test
        @DisplayName("should return true for existing subscription")
        void shouldReturnTrueForExisting() {
            repository.save(createSubscription("sub-1", "Test"));

            assertThat(repository.existsById("sub-1")).isTrue();
        }

        @Test
        @DisplayName("should return false for non-existent subscription")
        void shouldReturnFalseForNonExistent() {
            assertThat(repository.existsById("non-existent")).isFalse();
        }
    }

    @Nested
    @DisplayName("Clear Tests")
    class ClearTests {

        @Test
        @DisplayName("should clear all subscriptions")
        void shouldClearAllSubscriptions() {
            repository.save(createSubscription("sub-1", "Test 1"));
            repository.save(createSubscription("sub-2", "Test 2"));

            repository.clear();

            assertThat(repository.findAll()).isEmpty();
        }

        @Test
        @DisplayName("should be safe to clear empty repository")
        void shouldBeSafeToClearEmptyRepository() {
            repository.clear();

            assertThat(repository.findAll()).isEmpty();
        }
    }

    private WebhookSubscription createSubscription(String id, String name) {
        return WebhookSubscription.builder()
                .id(id)
                .name(name)
                .url("http://example.com/webhook")
                .secret("test-secret")
                .events(Set.of(WebhookEvent.ENTITY_CREATED))
                .active(true)
                .build();
    }
}
