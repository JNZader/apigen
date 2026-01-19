package com.jnzader.apigen.core.infrastructure.webhook;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing webhook subscriptions.
 *
 * <p>Implementations can be in-memory, database-backed, or use external configuration.
 */
public interface WebhookSubscriptionRepository {

    /**
     * Finds all active subscriptions that are interested in a specific event.
     *
     * @param event the event type
     * @return list of matching subscriptions
     */
    List<WebhookSubscription> findByEvent(WebhookEvent event);

    /**
     * Finds a subscription by its ID.
     *
     * @param id the subscription ID
     * @return the subscription if found
     */
    Optional<WebhookSubscription> findById(String id);

    /**
     * Returns all subscriptions.
     *
     * @return list of all subscriptions
     */
    List<WebhookSubscription> findAll();

    /**
     * Saves a subscription.
     *
     * @param subscription the subscription to save
     * @return the saved subscription
     */
    WebhookSubscription save(WebhookSubscription subscription);

    /**
     * Deletes a subscription by ID.
     *
     * @param id the subscription ID
     */
    void deleteById(String id);

    /**
     * Checks if a subscription exists.
     *
     * @param id the subscription ID
     * @return true if exists
     */
    boolean existsById(String id);
}
