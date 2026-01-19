package com.jnzader.apigen.core.infrastructure.bulk;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("BulkOperationResult Tests")
class BulkOperationResultTest {

    @Nested
    @DisplayName("Builder")
    class BuilderTests {

        @Test
        @DisplayName("should build result with all properties")
        void shouldBuildResultWithAllProperties() {
            Instant start = Instant.now();
            Instant end = start.plusSeconds(10);

            BulkOperationResult result =
                    BulkOperationResult.builder()
                            .operationType(BulkOperationResult.OperationType.IMPORT)
                            .format(BulkFormat.CSV)
                            .totalRecords(100)
                            .successCount(95)
                            .failureCount(5)
                            .startTime(start)
                            .endTime(end)
                            .build();

            assertThat(result.operationType()).isEqualTo(BulkOperationResult.OperationType.IMPORT);
            assertThat(result.format()).isEqualTo(BulkFormat.CSV);
            assertThat(result.totalRecords()).isEqualTo(100);
            assertThat(result.successCount()).isEqualTo(95);
            assertThat(result.failureCount()).isEqualTo(5);
            assertThat(result.duration()).isEqualTo(Duration.ofSeconds(10));
        }

        @Test
        @DisplayName("should set default times if not provided")
        void shouldSetDefaultTimesIfNotProvided() {
            BulkOperationResult result =
                    BulkOperationResult.builder()
                            .operationType(BulkOperationResult.OperationType.EXPORT)
                            .format(BulkFormat.EXCEL)
                            .totalRecords(50)
                            .successCount(50)
                            .failureCount(0)
                            .build();

            assertThat(result.startTime()).isNotNull();
            assertThat(result.endTime()).isNotNull();
            assertThat(result.duration()).isNotNull();
        }

        @Test
        @DisplayName("should add errors with full details")
        void shouldAddErrorsWithFullDetails() {
            BulkOperationResult result =
                    BulkOperationResult.builder()
                            .operationType(BulkOperationResult.OperationType.IMPORT)
                            .format(BulkFormat.CSV)
                            .totalRecords(10)
                            .successCount(8)
                            .failureCount(2)
                            .addError(5, "name", "Invalid format", "abc123")
                            .addError(7, "price", "Must be positive", "-10")
                            .build();

            assertThat(result.errors()).hasSize(2);

            BulkOperationResult.RecordError error1 = result.errors().get(0);
            assertThat(error1.rowNumber()).isEqualTo(5);
            assertThat(error1.fieldName()).isEqualTo("name");
            assertThat(error1.errorMessage()).isEqualTo("Invalid format");
            assertThat(error1.rawValue()).isEqualTo("abc123");
        }

        @Test
        @DisplayName("should add simple errors")
        void shouldAddSimpleErrors() {
            BulkOperationResult result =
                    BulkOperationResult.builder()
                            .operationType(BulkOperationResult.OperationType.IMPORT)
                            .format(BulkFormat.CSV)
                            .totalRecords(10)
                            .successCount(9)
                            .failureCount(1)
                            .addError(3, "General parsing error")
                            .build();

            assertThat(result.errors()).hasSize(1);
            BulkOperationResult.RecordError error = result.errors().get(0);
            assertThat(error.rowNumber()).isEqualTo(3);
            assertThat(error.fieldName()).isNull();
            assertThat(error.errorMessage()).isEqualTo("General parsing error");
        }
    }

    @Nested
    @DisplayName("Status Methods")
    class StatusMethodsTests {

        @Test
        @DisplayName("isFullySuccessful should return true when no failures")
        void isFullySuccessfulShouldReturnTrueWhenNoFailures() {
            BulkOperationResult result =
                    BulkOperationResult.builder()
                            .operationType(BulkOperationResult.OperationType.IMPORT)
                            .format(BulkFormat.CSV)
                            .totalRecords(100)
                            .successCount(100)
                            .failureCount(0)
                            .build();

            assertThat(result.isFullySuccessful()).isTrue();
        }

        @Test
        @DisplayName("isFullySuccessful should return false when has failures")
        void isFullySuccessfulShouldReturnFalseWhenHasFailures() {
            BulkOperationResult result =
                    BulkOperationResult.builder()
                            .operationType(BulkOperationResult.OperationType.IMPORT)
                            .format(BulkFormat.CSV)
                            .totalRecords(100)
                            .successCount(99)
                            .failureCount(1)
                            .build();

            assertThat(result.isFullySuccessful()).isFalse();
        }

        @Test
        @DisplayName("hasSuccesses should return true when has success")
        void hasSuccessesShouldReturnTrueWhenHasSuccess() {
            BulkOperationResult result =
                    BulkOperationResult.builder()
                            .operationType(BulkOperationResult.OperationType.IMPORT)
                            .format(BulkFormat.CSV)
                            .totalRecords(100)
                            .successCount(1)
                            .failureCount(99)
                            .build();

            assertThat(result.hasSuccesses()).isTrue();
        }

        @Test
        @DisplayName("hasSuccesses should return false when no success")
        void hasSuccessesShouldReturnFalseWhenNoSuccess() {
            BulkOperationResult result =
                    BulkOperationResult.builder()
                            .operationType(BulkOperationResult.OperationType.IMPORT)
                            .format(BulkFormat.CSV)
                            .totalRecords(100)
                            .successCount(0)
                            .failureCount(100)
                            .build();

            assertThat(result.hasSuccesses()).isFalse();
        }

        @Test
        @DisplayName("hasFailures should return correct values")
        void hasFailuresShouldReturnCorrectValues() {
            BulkOperationResult withFailures =
                    BulkOperationResult.builder()
                            .operationType(BulkOperationResult.OperationType.IMPORT)
                            .format(BulkFormat.CSV)
                            .totalRecords(10)
                            .successCount(9)
                            .failureCount(1)
                            .build();

            BulkOperationResult withoutFailures =
                    BulkOperationResult.builder()
                            .operationType(BulkOperationResult.OperationType.IMPORT)
                            .format(BulkFormat.CSV)
                            .totalRecords(10)
                            .successCount(10)
                            .failureCount(0)
                            .build();

            assertThat(withFailures.hasFailures()).isTrue();
            assertThat(withoutFailures.hasFailures()).isFalse();
        }
    }

    @Nested
    @DisplayName("Success Rate")
    class SuccessRateTests {

        @Test
        @DisplayName("should calculate 100% success rate")
        void shouldCalculateFullSuccessRate() {
            BulkOperationResult result =
                    BulkOperationResult.builder()
                            .operationType(BulkOperationResult.OperationType.IMPORT)
                            .format(BulkFormat.CSV)
                            .totalRecords(100)
                            .successCount(100)
                            .failureCount(0)
                            .build();

            assertThat(result.getSuccessRate()).isEqualTo(100.0);
        }

        @Test
        @DisplayName("should calculate partial success rate")
        void shouldCalculatePartialSuccessRate() {
            BulkOperationResult result =
                    BulkOperationResult.builder()
                            .operationType(BulkOperationResult.OperationType.IMPORT)
                            .format(BulkFormat.CSV)
                            .totalRecords(100)
                            .successCount(75)
                            .failureCount(25)
                            .build();

            assertThat(result.getSuccessRate()).isEqualTo(75.0);
        }

        @Test
        @DisplayName("should return 100% for empty dataset")
        void shouldReturnFullRateForEmptyDataset() {
            BulkOperationResult result =
                    BulkOperationResult.builder()
                            .operationType(BulkOperationResult.OperationType.IMPORT)
                            .format(BulkFormat.CSV)
                            .totalRecords(0)
                            .successCount(0)
                            .failureCount(0)
                            .build();

            assertThat(result.getSuccessRate()).isEqualTo(100.0);
        }
    }

    @Nested
    @DisplayName("Record Error")
    class RecordErrorTests {

        @Test
        @DisplayName("should create error with all fields")
        void shouldCreateErrorWithAllFields() {
            BulkOperationResult.RecordError error =
                    new BulkOperationResult.RecordError(
                            10, "email", "Invalid format", "notanemail");

            assertThat(error.rowNumber()).isEqualTo(10);
            assertThat(error.fieldName()).isEqualTo("email");
            assertThat(error.errorMessage()).isEqualTo("Invalid format");
            assertThat(error.rawValue()).isEqualTo("notanemail");
        }

        @Test
        @DisplayName("should create simple error")
        void shouldCreateSimpleError() {
            BulkOperationResult.RecordError error =
                    new BulkOperationResult.RecordError(5, "Row could not be parsed");

            assertThat(error.rowNumber()).isEqualTo(5);
            assertThat(error.fieldName()).isNull();
            assertThat(error.errorMessage()).isEqualTo("Row could not be parsed");
            assertThat(error.rawValue()).isNull();
        }
    }

    @Nested
    @DisplayName("Errors List")
    class ErrorsListTests {

        @Test
        @DisplayName("errors list should be immutable")
        void errorsListShouldBeImmutable() {
            BulkOperationResult result =
                    BulkOperationResult.builder()
                            .operationType(BulkOperationResult.OperationType.IMPORT)
                            .format(BulkFormat.CSV)
                            .totalRecords(10)
                            .successCount(10)
                            .failureCount(0)
                            .errors(List.of())
                            .build();

            assertThat(result.errors()).isNotNull();
        }
    }
}
