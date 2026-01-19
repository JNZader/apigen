package com.jnzader.apigen.core.application.service;

import com.jnzader.apigen.core.application.util.Result;
import java.util.List;
import java.util.function.Function;

/**
 * Service interface for batch operations using virtual threads.
 *
 * <p>Provides efficient parallel processing for bulk operations like:
 *
 * <ul>
 *   <li>Batch create/update/delete
 *   <li>Data migration
 *   <li>Bulk imports/exports
 *   <li>Background processing
 * </ul>
 *
 * <p>Uses Java 21+ Virtual Threads for efficient I/O-bound batch processing.
 *
 * @param <ID> the entity identifier type
 */
public interface BatchService<ID> {

    /**
     * Creates multiple entities in parallel.
     *
     * @param <T> the entity type
     * @param <D> the DTO type
     * @param dtos the DTOs to create entities from
     * @param createFunction the function to create each entity
     * @return a result containing successfully created entities and any failures
     */
    <T, D> BatchResult<T> batchCreate(List<D> dtos, Function<D, Result<T, String>> createFunction);

    /**
     * Updates multiple entities in parallel.
     *
     * @param <T> the entity type
     * @param <D> the DTO type
     * @param dtos the DTOs with updates
     * @param updateFunction the function to update each entity
     * @return a result containing successfully updated entities and any failures
     */
    <T, D> BatchResult<T> batchUpdate(List<D> dtos, Function<D, Result<T, String>> updateFunction);

    /**
     * Deletes multiple entities by IDs in parallel.
     *
     * @param ids the IDs of entities to delete
     * @param deleteFunction the function to delete each entity
     * @return a result containing successfully deleted IDs and any failures
     */
    BatchResult<ID> batchDelete(List<ID> ids, Function<ID, Result<ID, String>> deleteFunction);

    /**
     * Processes items in parallel using virtual threads.
     *
     * @param <T> the input type
     * @param <R> the result type
     * @param items the items to process
     * @param processor the function to process each item
     * @return a result containing successfully processed items and any failures
     */
    <T, R> BatchResult<R> batchProcess(List<T> items, Function<T, Result<R, String>> processor);

    /**
     * Processes items in parallel with configurable parallelism.
     *
     * @param <T> the input type
     * @param <R> the result type
     * @param items the items to process
     * @param processor the function to process each item
     * @param maxParallelism maximum number of parallel operations
     * @return a result containing successfully processed items and any failures
     */
    <T, R> BatchResult<R> batchProcess(
            List<T> items, Function<T, Result<R, String>> processor, int maxParallelism);

    /**
     * Result container for batch operations.
     *
     * @param <T> the result type
     */
    record BatchResult<T>(List<T> successes, List<BatchFailure> failures, long durationMs) {

        /**
         * Checks if all items were processed successfully.
         *
         * @return true if there are no failures
         */
        public boolean isFullySuccessful() {
            return failures.isEmpty();
        }

        /**
         * Gets the total number of items processed.
         *
         * @return total count (successes + failures)
         */
        public int totalCount() {
            return successes.size() + failures.size();
        }

        /**
         * Gets the success rate as a percentage.
         *
         * @return success rate (0.0 to 1.0)
         */
        public double successRate() {
            int total = totalCount();
            return total > 0 ? (double) successes.size() / total : 1.0;
        }
    }

    /**
     * Represents a failure during batch processing.
     *
     * @param index the index of the failed item in the original list
     * @param item the original item that failed
     * @param error the error message
     */
    record BatchFailure(int index, Object item, String error) {}
}
