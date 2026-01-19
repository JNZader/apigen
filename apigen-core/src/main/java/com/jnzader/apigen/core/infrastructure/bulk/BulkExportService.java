package com.jnzader.apigen.core.infrastructure.bulk;

import java.io.OutputStream;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Service for bulk exporting data to files.
 *
 * <p>Supports CSV and Excel formats with streaming for large datasets.
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * // Export list of products
 * byte[] data = bulkExportService.exportData(
 *     products,
 *     ProductDTO.class,
 *     BulkFormat.EXCEL
 * );
 *
 * // Stream large dataset
 * bulkExportService.exportStream(
 *     () -> productRepository.streamAll().map(mapper::toDTO),
 *     ProductDTO.class,
 *     BulkFormat.CSV,
 *     outputStream
 * );
 * }</pre>
 */
public interface BulkExportService {

    /**
     * Exports a list of records to the specified format.
     *
     * @param records the records to export
     * @param recordClass the class of the records
     * @param format the output format
     * @param <T> the type of records
     * @return byte array containing the exported data
     */
    <T> byte[] exportData(List<T> records, Class<T> recordClass, BulkFormat format);

    /**
     * Exports a list of records with custom configuration.
     *
     * @param records the records to export
     * @param recordClass the class of the records
     * @param format the output format
     * @param config export configuration
     * @param <T> the type of records
     * @return byte array containing the exported data
     */
    <T> byte[] exportData(
            List<T> records, Class<T> recordClass, BulkFormat format, ExportConfig config);

    /**
     * Exports records directly to an output stream.
     *
     * @param records the records to export
     * @param recordClass the class of the records
     * @param format the output format
     * @param outputStream the output stream to write to
     * @param <T> the type of records
     * @return result of the export operation
     */
    <T> BulkOperationResult exportToStream(
            List<T> records, Class<T> recordClass, BulkFormat format, OutputStream outputStream);

    /**
     * Exports a stream of records (for large datasets).
     *
     * <p>Uses streaming to avoid loading all records into memory.
     *
     * @param recordSupplier supplier that provides a stream of records
     * @param recordClass the class of the records
     * @param format the output format
     * @param outputStream the output stream to write to
     * @param <T> the type of records
     * @return result of the export operation
     */
    <T> BulkOperationResult exportStream(
            Supplier<Stream<T>> recordSupplier,
            Class<T> recordClass,
            BulkFormat format,
            OutputStream outputStream);

    /**
     * Exports a stream of records with custom configuration.
     *
     * @param recordSupplier supplier that provides a stream of records
     * @param recordClass the class of the records
     * @param format the output format
     * @param outputStream the output stream to write to
     * @param config export configuration
     * @param <T> the type of records
     * @return result of the export operation
     */
    <T> BulkOperationResult exportStream(
            Supplier<Stream<T>> recordSupplier,
            Class<T> recordClass,
            BulkFormat format,
            OutputStream outputStream,
            ExportConfig config);

    /**
     * Gets the headers for a record class.
     *
     * @param recordClass the class to extract headers from
     * @param <T> the type of records
     * @return list of header names
     */
    <T> List<String> getHeaders(Class<T> recordClass);

    /** Configuration options for export operations. */
    record ExportConfig(
            boolean includeHeader,
            char csvSeparator,
            String dateFormat,
            String sheetName,
            boolean autoSizeColumns,
            List<String> includeFields,
            List<String> excludeFields) {

        /** Default export configuration. */
        public static ExportConfig defaults() {
            return new ExportConfig(true, ',', "yyyy-MM-dd", "Data", true, List.of(), List.of());
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private boolean includeHeader = true;
            private char csvSeparator = ',';
            private String dateFormat = "yyyy-MM-dd";
            private String sheetName = "Data";
            private boolean autoSizeColumns = true;
            private List<String> includeFields = List.of();
            private List<String> excludeFields = List.of();

            public Builder includeHeader(boolean includeHeader) {
                this.includeHeader = includeHeader;
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

            public Builder autoSizeColumns(boolean autoSizeColumns) {
                this.autoSizeColumns = autoSizeColumns;
                return this;
            }

            public Builder includeFields(List<String> includeFields) {
                this.includeFields = includeFields;
                return this;
            }

            public Builder excludeFields(List<String> excludeFields) {
                this.excludeFields = excludeFields;
                return this;
            }

            public ExportConfig build() {
                return new ExportConfig(
                        includeHeader,
                        csvSeparator,
                        dateFormat,
                        sheetName,
                        autoSizeColumns,
                        includeFields,
                        excludeFields);
            }
        }
    }
}
