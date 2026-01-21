package com.jnzader.apigen.core.infrastructure.config;

import com.jnzader.apigen.core.infrastructure.config.properties.AppProperties;
import org.springframework.stereotype.Component;

/**
 * Provides configurable defaults for service operations.
 *
 * <p>Values can be configured via application.yml:
 *
 * <pre>
 * app:
 *   service:
 *     batch-size: 50
 *     max-results-without-pagination: 1000
 *     warn-threshold: 500
 *     max-batch-operation-size: 10000
 *   pagination:
 *     default-size: 20
 *     max-size: 100
 * </pre>
 */
@Component
public class ServiceDefaults {

    private final int batchSize;
    private final int maxResultsWithoutPagination;
    private final int warnThreshold;
    private final int maxBatchOperationSize;
    private final int defaultPageSize;
    private final int maxPageSize;

    public ServiceDefaults(AppProperties appProperties) {
        AppProperties.ServiceProperties service = appProperties.service();
        AppProperties.PaginationProperties pagination = appProperties.pagination();

        this.batchSize = service != null ? service.batchSize() : 50;
        this.maxResultsWithoutPagination =
                service != null ? service.maxResultsWithoutPagination() : 1000;
        this.warnThreshold = service != null ? service.warnThreshold() : 500;
        this.maxBatchOperationSize = service != null ? service.maxBatchOperationSize() : 10000;
        this.defaultPageSize = pagination != null ? pagination.defaultSize() : 20;
        this.maxPageSize = pagination != null ? pagination.maxSize() : 100;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public int getMaxResultsWithoutPagination() {
        return maxResultsWithoutPagination;
    }

    public int getWarnThreshold() {
        return warnThreshold;
    }

    public int getMaxBatchOperationSize() {
        return maxBatchOperationSize;
    }

    public int getDefaultPageSize() {
        return defaultPageSize;
    }

    public int getMaxPageSize() {
        return maxPageSize;
    }
}
