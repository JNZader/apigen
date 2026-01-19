package com.jnzader.apigen.core.infrastructure.bulk;

import java.io.InputStream;
import java.util.List;
import java.util.function.Function;

/**
 * Service for bulk importing data from files.
 *
 * <p>Supports CSV and Excel formats with validation and error handling.
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * BulkOperationResult result = bulkImportService.importData(
 *     inputStream,
 *     BulkFormat.CSV,
 *     ProductDTO.class,
 *     dto -> productService.save(mapper.toEntity(dto))
 * );
 *
 * if (result.hasFailures()) {
 *     result.errors().forEach(e -> log.warn("Row {}: {}", e.rowNumber(), e.errorMessage()));
 * }
 * }</pre>
 */
public interface BulkImportService {

    /**
     * Imports data from an input stream.
     *
     * @param inputStream the input stream containing the data
     * @param format the format of the data (CSV or EXCEL)
     * @param targetClass the class to map records to
     * @param processor function to process each mapped record
     * @param <T> the type of records being imported
     * @return the result of the import operation
     */
    <T> BulkOperationResult importData(
            InputStream inputStream,
            BulkFormat format,
            Class<T> targetClass,
            Function<T, Object> processor);

    /**
     * Imports data from an input stream with custom configuration.
     *
     * @param inputStream the input stream containing the data
     * @param format the format of the data
     * @param targetClass the class to map records to
     * @param processor function to process each mapped record
     * @param config import configuration options
     * @param <T> the type of records being imported
     * @return the result of the import operation
     */
    <T> BulkOperationResult importData(
            InputStream inputStream,
            BulkFormat format,
            Class<T> targetClass,
            Function<T, Object> processor,
            ImportConfig config);

    /**
     * Parses data from an input stream without processing.
     *
     * @param inputStream the input stream containing the data
     * @param format the format of the data
     * @param targetClass the class to map records to
     * @param <T> the type of records being parsed
     * @return list of parsed records
     */
    <T> List<T> parseData(InputStream inputStream, BulkFormat format, Class<T> targetClass);

    /**
     * Validates data from an input stream without importing.
     *
     * @param inputStream the input stream containing the data
     * @param format the format of the data
     * @param targetClass the class to validate against
     * @param <T> the type of records being validated
     * @return the validation result
     */
    <T> BulkOperationResult validateData(
            InputStream inputStream, BulkFormat format, Class<T> targetClass);

    /** Configuration options for import operations. */
    record ImportConfig(
            boolean skipHeader,
            boolean stopOnError,
            int batchSize,
            boolean validateBeforeProcess,
            char csvSeparator,
            String dateFormat,
            String sheetName) {

        /** Default import configuration. */
        public static ImportConfig defaults() {
            return new ImportConfig(true, false, 100, true, ',', "yyyy-MM-dd", null);
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private boolean skipHeader = true;
            private boolean stopOnError = false;
            private int batchSize = 100;
            private boolean validateBeforeProcess = true;
            private char csvSeparator = ',';
            private String dateFormat = "yyyy-MM-dd";
            private String sheetName = null;

            public Builder skipHeader(boolean skipHeader) {
                this.skipHeader = skipHeader;
                return this;
            }

            public Builder stopOnError(boolean stopOnError) {
                this.stopOnError = stopOnError;
                return this;
            }

            public Builder batchSize(int batchSize) {
                this.batchSize = batchSize;
                return this;
            }

            public Builder validateBeforeProcess(boolean validateBeforeProcess) {
                this.validateBeforeProcess = validateBeforeProcess;
                return this;
            }

            public Builder csvSeparator(char csvSeparator) {
                this.csvSeparator = csvSeparator;
                return this;
            }

            public Builder dateFormat(String dateFormat) {
                this.dateFormat = dateFormat;
                return this;
            }

            public Builder sheetName(String sheetName) {
                this.sheetName = sheetName;
                return this;
            }

            public ImportConfig build() {
                return new ImportConfig(
                        skipHeader,
                        stopOnError,
                        batchSize,
                        validateBeforeProcess,
                        csvSeparator,
                        dateFormat,
                        sheetName);
            }
        }
    }
}
