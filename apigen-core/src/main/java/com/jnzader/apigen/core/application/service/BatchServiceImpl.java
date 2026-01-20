package com.jnzader.apigen.core.application.service;

import com.jnzader.apigen.core.application.util.Result;
import com.jnzader.apigen.core.infrastructure.feature.FeatureChecker;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Implementation of BatchService using Java 21+ Virtual Threads.
 *
 * <p>Virtual threads provide efficient parallelism for I/O-bound operations without the overhead of
 * traditional thread pools.
 *
 * <p>Features:
 *
 * <ul>
 *   <li>Parallel processing with virtual threads
 *   <li>Configurable parallelism limits
 *   <li>Comprehensive error handling
 *   <li>Detailed batch results with timing
 * </ul>
 */
@Service
@ConditionalOnProperty(name = "apigen.batch.enabled", havingValue = "true", matchIfMissing = true)
public class BatchServiceImpl<I> implements BatchService<I> {

    private static final Logger log = LoggerFactory.getLogger(BatchServiceImpl.class);

    private static final int DEFAULT_MAX_PARALLELISM = 100;
    private static final int SHUTDOWN_TIMEOUT_SECONDS = 60;

    private final FeatureChecker featureChecker;

    public BatchServiceImpl(FeatureChecker featureChecker) {
        this.featureChecker = featureChecker;
    }

    @Override
    public <T, D> BatchResult<T> batchCreate(
            List<D> dtos, Function<D, Result<T, String>> createFunction) {
        return batchProcess(dtos, createFunction);
    }

    @Override
    public <T, D> BatchResult<T> batchUpdate(
            List<D> dtos, Function<D, Result<T, String>> updateFunction) {
        return batchProcess(dtos, updateFunction);
    }

    @Override
    public BatchResult<I> batchDelete(List<I> ids, Function<I, Result<I, String>> deleteFunction) {
        return batchProcess(ids, deleteFunction);
    }

    @Override
    public <T, R> BatchResult<R> batchProcess(
            List<T> items, Function<T, Result<R, String>> processor) {
        return batchProcess(items, processor, DEFAULT_MAX_PARALLELISM);
    }

    @Override
    public <T, R> BatchResult<R> batchProcess(
            List<T> items, Function<T, Result<R, String>> processor, int maxParallelism) {
        if (!featureChecker.isBatchOperationsEnabled()) {
            log.warn(
                    "Batch operations are disabled via feature flag. Processing sequentially"
                            + " instead.");
            return processSequentially(items, processor);
        }

        if (items == null || items.isEmpty()) {
            return new BatchResult<>(Collections.emptyList(), Collections.emptyList(), 0L);
        }

        log.debug(
                "Starting batch processing of {} items with max parallelism {}",
                items.size(),
                maxParallelism);
        long startTime = System.currentTimeMillis();

        List<R> successes = Collections.synchronizedList(new ArrayList<>());
        List<BatchFailure> failures = Collections.synchronizedList(new ArrayList<>());

        Semaphore semaphore = new Semaphore(maxParallelism);

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<?>> futures = new ArrayList<>(items.size());

            for (int i = 0; i < items.size(); i++) {
                final int index = i;
                final T item = items.get(i);

                Future<?> future =
                        executor.submit(
                                () ->
                                        processItem(
                                                item, index, processor, semaphore, successes,
                                                failures));

                futures.add(future);
            }

            awaitAllFutures(futures);
        }

        long durationMs = System.currentTimeMillis() - startTime;

        log.info(
                "Batch processing completed: {} successes, {} failures in {}ms",
                successes.size(),
                failures.size(),
                durationMs);

        return new BatchResult<>(successes, failures, durationMs);
    }

    /** Processes a single item with semaphore-controlled parallelism. */
    private <T, R> void processItem(
            T item,
            int index,
            Function<T, Result<R, String>> processor,
            Semaphore semaphore,
            List<R> successes,
            List<BatchFailure> failures) {
        try {
            semaphore.acquire();
            try {
                Result<R, String> result = processor.apply(item);
                handleProcessorResult(result, index, item, successes, failures);
            } finally {
                semaphore.release();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            failures.add(new BatchFailure(index, item, "Interrupted: " + e.getMessage()));
        } catch (Exception e) {
            failures.add(new BatchFailure(index, item, "Error: " + e.getMessage()));
            log.error("Error processing batch item at index {}: {}", index, e.getMessage(), e);
        }
    }

    /** Handles the result of processing a single item. */
    private <T, R> void handleProcessorResult(
            Result<R, String> result,
            int index,
            T item,
            List<R> successes,
            List<BatchFailure> failures) {
        if (result.isSuccess()) {
            successes.add(result.orElseThrow());
        } else {
            failures.add(new BatchFailure(index, item, result.fold(v -> "Unknown error", e -> e)));
        }
    }

    /** Waits for all futures to complete with timeout handling. */
    private void awaitAllFutures(List<Future<?>> futures) {
        for (Future<?> future : futures) {
            try {
                future.get(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Batch task interrupted: {}", e.getMessage());
            } catch (Exception e) {
                log.error("Error waiting for batch task completion: {}", e.getMessage());
            }
        }
    }

    /** Processes items sequentially when batch operations feature is disabled. */
    private <T, R> BatchResult<R> processSequentially(
            List<T> items, Function<T, Result<R, String>> processor) {
        if (items == null || items.isEmpty()) {
            return new BatchResult<>(Collections.emptyList(), Collections.emptyList(), 0L);
        }

        long startTime = System.currentTimeMillis();
        List<R> successes = new ArrayList<>();
        List<BatchFailure> failures = new ArrayList<>();

        for (int i = 0; i < items.size(); i++) {
            T item = items.get(i);
            try {
                Result<R, String> result = processor.apply(item);
                if (result.isSuccess()) {
                    successes.add(result.orElseThrow());
                } else {
                    failures.add(
                            new BatchFailure(i, item, result.fold(v -> "Unknown error", e -> e)));
                }
            } catch (Exception e) {
                failures.add(new BatchFailure(i, item, "Error: " + e.getMessage()));
            }
        }

        long durationMs = System.currentTimeMillis() - startTime;
        return new BatchResult<>(successes, failures, durationMs);
    }
}
