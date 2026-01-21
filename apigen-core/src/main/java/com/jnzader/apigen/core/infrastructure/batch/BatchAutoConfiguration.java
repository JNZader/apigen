package com.jnzader.apigen.core.infrastructure.batch;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * Auto-configuration for batch API operations.
 *
 * <p>Enables the batch endpoint that allows executing multiple API operations in a single HTTP
 * request.
 *
 * <p>Enabled by default. Can be disabled with:
 *
 * <pre>
 * apigen.batch.enabled=false
 * </pre>
 *
 * <p>Configuration properties:
 *
 * <pre>
 * apigen.batch.enabled=true              # Enable/disable batch endpoint
 * apigen.batch.path=/api/batch           # Endpoint path
 * apigen.batch.max-operations=100        # Maximum operations per batch
 * apigen.batch.parallel-threshold=10     # Suggest parallel for batches larger than this
 * </pre>
 */
@AutoConfiguration(
        afterName = "org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration")
@EnableConfigurationProperties(BatchAutoConfiguration.BatchProperties.class)
@ConditionalOnWebApplication
@ConditionalOnBean(ObjectMapper.class)
@ConditionalOnProperty(
        prefix = "apigen.batch",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class BatchAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(BatchService.class)
    public BatchService batchService(
            DispatcherServlet dispatcherServlet, ObjectMapper objectMapper) {
        return new BatchService(dispatcherServlet, objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean(BatchController.class)
    public BatchController batchController(BatchService batchService) {
        return new BatchController(batchService);
    }

    /** Configuration properties for batch API operations. */
    @ConfigurationProperties(prefix = "apigen.batch")
    public record BatchProperties(
            boolean enabled,
            String path,
            int maxOperations,
            int parallelThreshold,
            boolean allowNestedBatch,
            int timeoutSeconds) {

        public BatchProperties {
            if (path == null || path.isBlank()) {
                path = "/api/batch";
            }
            if (maxOperations <= 0) {
                maxOperations = 100;
            }
            if (parallelThreshold <= 0) {
                parallelThreshold = 10;
            }
            if (timeoutSeconds <= 0) {
                timeoutSeconds = 30;
            }
        }

        /** Default configuration. */
        public static BatchProperties defaults() {
            return new BatchProperties(true, "/api/batch", 100, 10, false, 30);
        }
    }
}
