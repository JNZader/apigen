package com.jnzader.apigen.core.infrastructure.bulk;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Result of a bulk import or export operation.
 *
 * <p>Contains statistics about the operation including:
 *
 * <ul>
 *   <li>Number of successful records processed
 *   <li>Number of failed records with error details
 *   <li>Operation timing information
 * </ul>
 *
 * @param operationType the type of operation (IMPORT or EXPORT)
 * @param format the format used (CSV or EXCEL)
 * @param totalRecords total number of records attempted
 * @param successCount number of successfully processed records
 * @param failureCount number of failed records
 * @param errors list of error details for failed records
 * @param startTime when the operation started
 * @param endTime when the operation completed
 * @param duration how long the operation took
 */
public record BulkOperationResult(
        OperationType operationType,
        BulkFormat format,
        int totalRecords,
        int successCount,
        int failureCount,
        List<RecordError> errors,
        Instant startTime,
        Instant endTime,
        Duration duration) {

    /** Type of bulk operation. */
    public enum OperationType {
        IMPORT,
        EXPORT
    }

    /**
     * Details about a record that failed to process.
     *
     * @param rowNumber the row number in the file (1-based)
     * @param fieldName the field that caused the error (if applicable)
     * @param errorMessage description of the error
     * @param rawValue the raw value that caused the error
     */
    public record RecordError(
            int rowNumber, String fieldName, String errorMessage, String rawValue) {

        public RecordError(int rowNumber, String errorMessage) {
            this(rowNumber, null, errorMessage, null);
        }
    }

    /** Creates a new builder for BulkOperationResult. */
    public static Builder builder() {
        return new Builder();
    }

    /** Whether the operation was completely successful (no failures). */
    public boolean isFullySuccessful() {
        return failureCount == 0;
    }

    /** Whether the operation had any successful records. */
    public boolean hasSuccesses() {
        return successCount > 0;
    }

    /** Whether the operation had any failures. */
    public boolean hasFailures() {
        return failureCount > 0;
    }

    /** Success rate as a percentage (0-100). */
    public double getSuccessRate() {
        if (totalRecords == 0) {
            return 100.0;
        }
        return (successCount * 100.0) / totalRecords;
    }

    /** Builder for creating BulkOperationResult instances. */
    public static class Builder {
        private OperationType operationType;
        private BulkFormat format;
        private int totalRecords;
        private int successCount;
        private int failureCount;
        private List<RecordError> errors = new ArrayList<>();
        private Instant startTime;
        private Instant endTime;

        public Builder operationType(OperationType operationType) {
            this.operationType = operationType;
            return this;
        }

        public Builder format(BulkFormat format) {
            this.format = format;
            return this;
        }

        public Builder totalRecords(int totalRecords) {
            this.totalRecords = totalRecords;
            return this;
        }

        public Builder successCount(int successCount) {
            this.successCount = successCount;
            return this;
        }

        public Builder failureCount(int failureCount) {
            this.failureCount = failureCount;
            return this;
        }

        public Builder errors(List<RecordError> errors) {
            this.errors = new ArrayList<>(errors);
            return this;
        }

        public Builder addError(RecordError error) {
            this.errors.add(error);
            return this;
        }

        public Builder addError(
                int rowNumber, String fieldName, String errorMessage, String rawValue) {
            this.errors.add(new RecordError(rowNumber, fieldName, errorMessage, rawValue));
            return this;
        }

        public Builder addError(int rowNumber, String errorMessage) {
            this.errors.add(new RecordError(rowNumber, errorMessage));
            return this;
        }

        public Builder startTime(Instant startTime) {
            this.startTime = startTime;
            return this;
        }

        public Builder endTime(Instant endTime) {
            this.endTime = endTime;
            return this;
        }

        public BulkOperationResult build() {
            if (startTime == null) {
                startTime = Instant.now();
            }
            if (endTime == null) {
                endTime = Instant.now();
            }
            Duration duration = Duration.between(startTime, endTime);
            return new BulkOperationResult(
                    operationType,
                    format,
                    totalRecords,
                    successCount,
                    failureCount,
                    Collections.unmodifiableList(errors),
                    startTime,
                    endTime,
                    duration);
        }
    }
}
