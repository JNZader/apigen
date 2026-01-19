package com.jnzader.apigen.grpc.health;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manager for health check components.
 *
 * <p>Aggregates health checks from multiple components and provides overall health status.
 */
public class HealthServiceManager {

    private static final Logger log = LoggerFactory.getLogger(HealthServiceManager.class);

    private final Map<String, HealthCheck> healthChecks = new ConcurrentHashMap<>();

    /**
     * Registers a health check component.
     *
     * @param healthCheck the health check to register
     */
    public void register(HealthCheck healthCheck) {
        healthChecks.put(healthCheck.getName(), healthCheck);
        log.debug("Registered health check: {}", healthCheck.getName());
    }

    /**
     * Unregisters a health check component.
     *
     * @param name the name of the health check to unregister
     */
    public void unregister(String name) {
        healthChecks.remove(name);
        log.debug("Unregistered health check: {}", name);
    }

    /**
     * Checks the health of a specific component.
     *
     * @param name the component name
     * @return the health result, or null if not found
     */
    public HealthCheck.Result checkComponent(String name) {
        HealthCheck check = healthChecks.get(name);
        if (check == null) {
            return null;
        }
        try {
            return check.check();
        } catch (Exception e) {
            log.error("Health check failed for component: {}", name, e);
            return HealthCheck.Result.unhealthy("Health check failed: " + e.getMessage());
        }
    }

    /**
     * Checks the overall health of all components.
     *
     * @return the aggregated health status
     */
    public AggregatedHealth checkAll() {
        List<ComponentStatus> statuses = new ArrayList<>();
        boolean allHealthy = true;

        for (HealthCheck check : healthChecks.values()) {
            try {
                HealthCheck.Result result = check.check();
                statuses.add(new ComponentStatus(check.getName(), result));
                if (!result.isHealthy()) {
                    allHealthy = false;
                }
            } catch (Exception e) {
                log.error("Health check failed for component: {}", check.getName(), e);
                statuses.add(
                        new ComponentStatus(
                                check.getName(),
                                HealthCheck.Result.unhealthy("Exception: " + e.getMessage())));
                allHealthy = false;
            }
        }

        return new AggregatedHealth(allHealthy, statuses);
    }

    /**
     * Gets all registered health check names.
     *
     * @return list of health check names
     */
    public List<String> getRegisteredChecks() {
        return List.copyOf(healthChecks.keySet());
    }

    /** Aggregated health status from all components. */
    public record AggregatedHealth(boolean healthy, List<ComponentStatus> components) {}

    /** Health status of a single component. */
    public record ComponentStatus(String name, HealthCheck.Result result) {}
}
