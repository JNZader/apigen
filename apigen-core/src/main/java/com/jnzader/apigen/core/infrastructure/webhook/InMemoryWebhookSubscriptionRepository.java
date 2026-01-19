package com.jnzader.apigen.core.infrastructure.webhook;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of WebhookSubscriptionRepository.
 *
 * <p>This implementation stores subscriptions in memory and is suitable for development or
 * single-instance deployments. For production with multiple instances, use a database-backed or
 * Redis-backed implementation.
 */
public class InMemoryWebhookSubscriptionRepository implements WebhookSubscriptionRepository {

    private final Map<String, WebhookSubscription> subscriptions = new ConcurrentHashMap<>();

    @Override
    public List<WebhookSubscription> findByEvent(WebhookEvent event) {
        return subscriptions.values().stream().filter(sub -> sub.isInterestedIn(event)).toList();
    }

    @Override
    public Optional<WebhookSubscription> findById(String id) {
        return Optional.ofNullable(subscriptions.get(id));
    }

    @Override
    public List<WebhookSubscription> findAll() {
        return List.copyOf(subscriptions.values());
    }

    @Override
    public WebhookSubscription save(WebhookSubscription subscription) {
        subscriptions.put(subscription.id(), subscription);
        return subscription;
    }

    @Override
    public void deleteById(String id) {
        subscriptions.remove(id);
    }

    @Override
    public boolean existsById(String id) {
        return subscriptions.containsKey(id);
    }

    /** Clears all subscriptions. Useful for testing. */
    public void clear() {
        subscriptions.clear();
    }
}
