package com.jnzader.apigen.core.infrastructure.bulk;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for bulk import/export operations.
 *
 * <p>Enabled by default. Can be disabled with:
 *
 * <pre>
 * apigen.bulk.enabled=false
 * </pre>
 *
 * <p>Configuration properties:
 *
 * <pre>
 * apigen.bulk.enabled=true
 * apigen.bulk.default-batch-size=100
 * apigen.bulk.csv-separator=,
 * apigen.bulk.date-format=yyyy-MM-dd
 * apigen.bulk.excel-sheet-name=Data
 * </pre>
 */
@AutoConfiguration
@EnableConfigurationProperties(BulkAutoConfiguration.BulkProperties.class)
@ConditionalOnProperty(
        prefix = "apigen.bulk",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class BulkAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(BulkImportService.class)
    public BulkImportService bulkImportService() {
        return new BulkOperationsService();
    }

    @Bean
    @ConditionalOnMissingBean(BulkExportService.class)
    public BulkExportService bulkExportService() {
        return new BulkOperationsService();
    }

    @Bean
    @ConditionalOnMissingBean(BulkOperationsService.class)
    public BulkOperationsService bulkOperationsService() {
        return new BulkOperationsService();
    }

    /** Configuration properties for bulk operations. */
    @ConfigurationProperties(prefix = "apigen.bulk")
    public record BulkProperties(
            boolean enabled,
            int defaultBatchSize,
            char csvSeparator,
            String dateFormat,
            String excelSheetName,
            int maxImportSize,
            int maxExportSize) {

        public BulkProperties {
            if (defaultBatchSize <= 0) {
                defaultBatchSize = 100;
            }
            if (csvSeparator == '\0') {
                csvSeparator = ',';
            }
            if (dateFormat == null || dateFormat.isBlank()) {
                dateFormat = "yyyy-MM-dd";
            }
            if (excelSheetName == null || excelSheetName.isBlank()) {
                excelSheetName = "Data";
            }
            if (maxImportSize <= 0) {
                maxImportSize = 10000;
            }
            if (maxExportSize <= 0) {
                maxExportSize = 100000;
            }
        }

        /** Default configuration. */
        public static BulkProperties defaults() {
            return new BulkProperties(true, 100, ',', "yyyy-MM-dd", "Data", 10000, 100000);
        }

        /** Creates import configuration from these properties. */
        public BulkImportService.ImportConfig toImportConfig() {
            return BulkImportService.ImportConfig.builder()
                    .batchSize(defaultBatchSize)
                    .csvSeparator(csvSeparator)
                    .dateFormat(dateFormat)
                    .sheetName(excelSheetName)
                    .build();
        }

        /** Creates export configuration from these properties. */
        public BulkExportService.ExportConfig toExportConfig() {
            return BulkExportService.ExportConfig.builder()
                    .csvSeparator(csvSeparator)
                    .dateFormat(dateFormat)
                    .sheetName(excelSheetName)
                    .build();
        }
    }
}
