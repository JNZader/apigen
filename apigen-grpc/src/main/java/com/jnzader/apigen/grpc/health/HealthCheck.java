package com.jnzader.apigen.grpc.health;

import java.util.Map;

/**
 * Interface for health check components.
 *
 * <p>Components can implement this interface to report their health status.
 */
public interface HealthCheck {

    /**
     * Gets the name of the health check component.
     *
     * @return the component name
     */
    String getName();

    /**
     * Checks the health of the component.
     *
     * @return the health result
     */
    Result check();

    /** Health check result. */
    record Result(boolean isHealthy, String message, Map<String, String> details) {

        public static Result healthy() {
            return new Result(true, "OK", Map.of());
        }

        public static Result healthy(String message) {
            return new Result(true, message, Map.of());
        }

        public static Result healthy(String message, Map<String, String> details) {
            return new Result(true, message, details);
        }

        public static Result unhealthy(String message) {
            return new Result(false, message, Map.of());
        }

        public static Result unhealthy(String message, Map<String, String> details) {
            return new Result(false, message, details);
        }
    }
}
