package com.jnzader.apigen.core.application.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests para el patrón Result.
 * <p>
 * Verifica el comportamiento correcto de:
 * - Creación de Success y Failure
 * - Transformaciones (map, flatMap)
 * - Extracción de valores
 * - Manejo de excepciones
 */
@DisplayName("Result Pattern Tests")
class ResultTest {

    // ==================== Creation Tests ====================

    @Nested
    @DisplayName("Creation")
    class CreationTests {

        @Test
        @DisplayName("success() should create successful result")
        void successShouldCreateSuccessfulResult() {
            // When
            Result<String, Exception> result = Result.success("value");

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.isFailure()).isFalse();
            assertThat(result.orElseThrow()).isEqualTo("value");
        }

        @Test
        @DisplayName("failure() should create failed result")
        void failureShouldCreateFailedResult() {
            // When
            Exception error = new RuntimeException("error");
            Result<String, Exception> result = Result.failure(error);

            // Then
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.isFailure()).isTrue();
            assertThatThrownBy(result::orElseThrow)
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("error");
        }

        @Test
        @DisplayName("of() should capture successful computation")
        void ofShouldCaptureSuccessfulComputation() {
            // When
            Result<Integer, Exception> result = Result.of(() -> 1 + 1);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.orElseThrow()).isEqualTo(2);
        }

        @Test
        @DisplayName("of() should capture exception")
        void ofShouldCaptureException() {
            // When
            Result<Integer, Exception> result = Result.of(() -> {
                throw new IllegalArgumentException("test error");
            });

            // Then
            assertThat(result.isFailure()).isTrue();
        }

        @Test
        @DisplayName("fromOptional() should create success for present value")
        void fromOptionalShouldCreateSuccessForPresentValue() {
            // When
            Result<String, Exception> result = Result.fromOptional(
                    Optional.of("value"),
                    () -> new RuntimeException("not found")
            );

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.orElseThrow()).isEqualTo("value");
        }

        @Test
        @DisplayName("fromOptional() should create failure for empty optional")
        void fromOptionalShouldCreateFailureForEmptyOptional() {
            // When
            Result<String, Exception> result = Result.fromOptional(
                    Optional.empty(),
                    () -> new RuntimeException("not found")
            );

            // Then
            assertThat(result.isFailure()).isTrue();
        }
    }

    // ==================== Transformation Tests ====================

    @Nested
    @DisplayName("Transformations")
    class TransformationTests {

        @Test
        @DisplayName("map() should transform success value")
        void mapShouldTransformSuccessValue() {
            // Given
            Result<Integer, Exception> result = Result.success(5);

            // When
            Result<String, Exception> mapped = result.map(n -> "Number: " + n);

            // Then
            assertThat(mapped.isSuccess()).isTrue();
            assertThat(mapped.orElseThrow()).isEqualTo("Number: 5");
        }

        @Test
        @DisplayName("map() should preserve failure")
        void mapShouldPreserveFailure() {
            // Given
            Result<Integer, Exception> result = Result.failure(new RuntimeException("error"));

            // When
            Result<String, Exception> mapped = result.map(n -> "Number: " + n);

            // Then
            assertThat(mapped.isFailure()).isTrue();
        }

        @Test
        @DisplayName("flatMap() should chain successful results")
        void flatMapShouldChainSuccessfulResults() {
            // Given
            Result<Integer, Exception> result = Result.success(5);

            // When
            Result<Integer, Exception> chained = result.flatMap(n -> Result.success(n * 2));

            // Then
            assertThat(chained.isSuccess()).isTrue();
            assertThat(chained.orElseThrow()).isEqualTo(10);
        }

        @Test
        @DisplayName("flatMap() should short-circuit on failure")
        void flatMapShouldShortCircuitOnFailure() {
            // Given
            Result<Integer, Exception> result = Result.failure(new RuntimeException("first error"));
            AtomicBoolean called = new AtomicBoolean(false);

            // When
            Result<Integer, Exception> chained = result.flatMap(n -> {
                called.set(true);
                return Result.success(n * 2);
            });

            // Then
            assertThat(chained.isFailure()).isTrue();
            assertThat(called.get()).isFalse();
        }

        @Test
        @DisplayName("flatMap() should propagate failure from mapper")
        void flatMapShouldPropagateFailureFromMapper() {
            // Given
            Result<Integer, Exception> result = Result.success(5);

            // When
            Result<Integer, Exception> chained = result.flatMap(n ->
                    Result.failure(new RuntimeException("mapper error")));

            // Then
            assertThat(chained.isFailure()).isTrue();
        }
    }

    // ==================== Value Extraction Tests ====================

    @Nested
    @DisplayName("Value Extraction")
    class ValueExtractionTests {

        @Test
        @DisplayName("orElse() should return value on success")
        void orElseShouldReturnValueOnSuccess() {
            // Given
            Result<String, Exception> result = Result.success("value");

            // When
            String value = result.orElse("default");

            // Then
            assertThat(value).isEqualTo("value");
        }

        @Test
        @DisplayName("orElse() should return default on failure")
        void orElseShouldReturnDefaultOnFailure() {
            // Given
            Result<String, Exception> result = Result.failure(new RuntimeException());

            // When
            String value = result.orElse("default");

            // Then
            assertThat(value).isEqualTo("default");
        }

        @Test
        @DisplayName("orElseGet() should not call supplier on success")
        void orElseGetShouldNotCallSupplierOnSuccess() {
            // Given
            Result<String, Exception> result = Result.success("value");
            AtomicBoolean called = new AtomicBoolean(false);

            // When
            String value = result.orElseGet(e -> {
                called.set(true);
                return "default";
            });

            // Then
            assertThat(value).isEqualTo("value");
            assertThat(called.get()).isFalse();
        }

        @Test
        @DisplayName("orElseGet() should call supplier on failure")
        void orElseGetShouldCallSupplierOnFailure() {
            // Given
            Result<String, Exception> result = Result.failure(new RuntimeException());

            // When
            String value = result.orElseGet(e -> "default");

            // Then
            assertThat(value).isEqualTo("default");
        }

        @Test
        @DisplayName("orElseThrow() should return value on success")
        void orElseThrowShouldReturnValueOnSuccess() {
            // Given
            Result<String, Exception> result = Result.success("value");

            // When/Then
            assertThat(result.orElseThrow()).isEqualTo("value");
        }

        @Test
        @DisplayName("orElseThrow() should throw RuntimeException on failure")
        void orElseThrowShouldThrowRuntimeExceptionOnFailure() {
            // Given
            Result<String, Exception> result = Result.failure(new RuntimeException("test"));

            // When/Then
            assertThatThrownBy(result::orElseThrow)
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("test");
        }

        @Test
        @DisplayName("orElseThrow() should wrap non-RuntimeException")
        void orElseThrowShouldWrapNonRuntimeException() {
            // Given
            Result<String, Exception> result = Result.failure(new Exception("checked"));

            // When/Then
            assertThatThrownBy(result::orElseThrow)
                    .isInstanceOf(RuntimeException.class)
                    .hasCauseInstanceOf(Exception.class);
        }
    }

    // ==================== Side Effect Tests ====================

    @Nested
    @DisplayName("Side Effects")
    class SideEffectTests {

        @Test
        @DisplayName("onSuccess() should execute action on success")
        void onSuccessShouldExecuteActionOnSuccess() {
            // Given
            Result<String, Exception> result = Result.success("value");
            AtomicBoolean called = new AtomicBoolean(false);

            // When
            result.onSuccess(v -> called.set(true));

            // Then
            assertThat(called.get()).isTrue();
        }

        @Test
        @DisplayName("onSuccess() should not execute action on failure")
        void onSuccessShouldNotExecuteActionOnFailure() {
            // Given
            Result<String, Exception> result = Result.failure(new RuntimeException());
            AtomicBoolean called = new AtomicBoolean(false);

            // When
            result.onSuccess(v -> called.set(true));

            // Then
            assertThat(called.get()).isFalse();
        }

        @Test
        @DisplayName("onFailure() should execute action on failure")
        void onFailureShouldExecuteActionOnFailure() {
            // Given
            Result<String, Exception> result = Result.failure(new RuntimeException("error"));
            AtomicBoolean called = new AtomicBoolean(false);

            // When
            result.onFailure(e -> called.set(true));

            // Then
            assertThat(called.get()).isTrue();
        }

        @Test
        @DisplayName("onFailure() should not execute action on success")
        void onFailureShouldNotExecuteActionOnSuccess() {
            // Given
            Result<String, Exception> result = Result.success("value");
            AtomicBoolean called = new AtomicBoolean(false);

            // When
            result.onFailure(e -> called.set(true));

            // Then
            assertThat(called.get()).isFalse();
        }
    }

    // ==================== Chaining Tests ====================

    @Nested
    @DisplayName("Chaining Operations")
    class ChainingTests {

        @Test
        @DisplayName("should support fluent chaining")
        void shouldSupportFluentChaining() {
            // Given
            Result<Integer, Exception> result = Result.success(5);

            // When
            Result<String, Exception> chained = result
                    .map(n -> n * 2)
                    .flatMap(n -> Result.success(n + 1))
                    .map(n -> "Result: " + n);

            // Then
            assertThat(chained.isSuccess()).isTrue();
            assertThat(chained.orElseThrow()).isEqualTo("Result: 11");
        }

        @Test
        @DisplayName("should stop chain on first failure")
        void shouldStopChainOnFirstFailure() {
            // Given
            AtomicBoolean secondMapCalled = new AtomicBoolean(false);

            // When
            Result<String, Exception> result = Result.<Integer, Exception>success(5)
                    .flatMap(n -> Result.failure(new RuntimeException("error")))
                    .map(n -> {
                        secondMapCalled.set(true);
                        return "should not reach";
                    });

            // Then
            assertThat(result.isFailure()).isTrue();
            assertThat(secondMapCalled.get()).isFalse();
        }
    }

    // ==================== Fold Tests ====================

    @Nested
    @DisplayName("Fold Operations")
    class FoldTests {

        @Test
        @DisplayName("fold() should apply success function on success")
        void foldShouldApplySuccessFunctionOnSuccess() {
            // Given
            Result<Integer, Exception> result = Result.success(5);

            // When
            String folded = result.fold(
                    v -> "Value: " + v,
                    e -> "Error: " + e.getMessage()
            );

            // Then
            assertThat(folded).isEqualTo("Value: 5");
        }

        @Test
        @DisplayName("fold() should apply failure function on failure")
        void foldShouldApplyFailureFunctionOnFailure() {
            // Given
            Result<Integer, Exception> result = Result.failure(new RuntimeException("oops"));

            // When
            String folded = result.fold(
                    v -> "Value: " + v,
                    e -> "Error: " + e.getMessage()
            );

            // Then
            assertThat(folded).isEqualTo("Error: oops");
        }
    }

    // ==================== Error Transformation Tests ====================

    @Nested
    @DisplayName("Error Transformations")
    class ErrorTransformationTests {

        @Test
        @DisplayName("mapError() should transform error on failure")
        void mapErrorShouldTransformErrorOnFailure() {
            // Given
            Result<Integer, String> result = Result.failure("error");

            // When
            Result<Integer, Integer> mapped = result.mapError(String::length);

            // Then
            assertThat(mapped.isFailure()).isTrue();
        }

        @Test
        @DisplayName("mapError() should preserve value on success")
        void mapErrorShouldPreserveValueOnSuccess() {
            // Given
            Result<Integer, String> result = Result.success(5);

            // When
            Result<Integer, Integer> mapped = result.mapError(String::length);

            // Then
            assertThat(mapped.isSuccess()).isTrue();
            assertThat(mapped.orElse(-1)).isEqualTo(5);
        }

        @Test
        @DisplayName("flatMapError() should chain error results on failure")
        void flatMapErrorShouldChainErrorResultsOnFailure() {
            // Given
            Result<Integer, String> result = Result.failure("error");

            // When
            Result<Integer, Integer> chained = result.flatMapError(e -> Result.failure(e.length()));

            // Then
            assertThat(chained.isFailure()).isTrue();
        }

        @Test
        @DisplayName("flatMapError() should preserve success value")
        void flatMapErrorShouldPreserveSuccessValue() {
            // Given
            Result<Integer, String> result = Result.success(10);

            // When
            Result<Integer, Integer> chained = result.flatMapError(e -> Result.failure(e.length()));

            // Then
            assertThat(chained.isSuccess()).isTrue();
            assertThat(chained.orElse(-1)).isEqualTo(10);
        }

        @Test
        @DisplayName("flatMapError() can recover to success")
        void flatMapErrorCanRecoverToSuccess() {
            // Given
            Result<Integer, String> result = Result.failure("error");

            // When
            Result<Integer, Integer> recovered = result.flatMapError(e -> Result.success(42));

            // Then
            assertThat(recovered.isSuccess()).isTrue();
            assertThat(recovered.orElse(-1)).isEqualTo(42);
        }
    }

    // ==================== orElseThrow with mapper Tests ====================

    @Nested
    @DisplayName("orElseThrow with Mapper")
    class OrElseThrowWithMapperTests {

        @Test
        @DisplayName("orElseThrow(mapper) should return value on success")
        void orElseThrowMapperShouldReturnValueOnSuccess() throws Exception {
            // Given
            Result<String, String> result = Result.success("value");

            // When
            String value = result.orElseThrow(RuntimeException::new);

            // Then
            assertThat(value).isEqualTo("value");
        }

        @Test
        @DisplayName("orElseThrow(mapper) should throw mapped exception on failure")
        void orElseThrowMapperShouldThrowMappedExceptionOnFailure() {
            // Given
            Result<String, String> result = Result.failure("error message");

            // When/Then
            assertThatThrownBy(() -> result.orElseThrow(IllegalArgumentException::new))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("error message");
        }

        @Test
        @DisplayName("orElseThrow() should throw Error directly")
        void orElseThrowShouldThrowErrorDirectly() {
            // Given
            Error error = new OutOfMemoryError("test error");
            Result<String, Error> result = Result.failure(error);

            // When/Then
            assertThatThrownBy(result::orElseThrow)
                    .isInstanceOf(OutOfMemoryError.class)
                    .hasMessage("test error");
        }

        @Test
        @DisplayName("orElseThrow() should wrap non-Throwable errors")
        void orElseThrowShouldWrapNonThrowableErrors() {
            // Given
            Result<String, String> result = Result.failure("plain error string");

            // When/Then
            assertThatThrownBy(result::orElseThrow)
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("plain error string");
        }
    }

    // ==================== toOptional Tests ====================

    @Nested
    @DisplayName("toOptional")
    class ToOptionalTests {

        @Test
        @DisplayName("toOptional() should return present Optional on success")
        void toOptionalShouldReturnPresentOnSuccess() {
            // Given
            Result<String, Exception> result = Result.success("value");

            // When
            Optional<String> optional = result.toOptional();

            // Then
            assertThat(optional).isPresent().contains("value");
        }

        @Test
        @DisplayName("toOptional() should return empty Optional on failure")
        void toOptionalShouldReturnEmptyOnFailure() {
            // Given
            Result<String, Exception> result = Result.failure(new RuntimeException());

            // When
            Optional<String> optional = result.toOptional();

            // Then
            assertThat(optional).isEmpty();
        }

        @Test
        @DisplayName("toOptional() should return empty for null success value")
        void toOptionalShouldReturnEmptyForNullSuccessValue() {
            // Given
            Result<String, Exception> result = Result.success(null);

            // When
            Optional<String> optional = result.toOptional();

            // Then
            assertThat(optional).isEmpty();
        }
    }

    // ==================== Peek Tests ====================

    @Nested
    @DisplayName("Peek")
    class PeekTests {

        @Test
        @DisplayName("peek() should execute action and return same result")
        void peekShouldExecuteActionAndReturnSameResult() {
            // Given
            Result<String, Exception> result = Result.success("value");
            AtomicBoolean called = new AtomicBoolean(false);

            // When
            Result<String, Exception> same = result.peek(r -> called.set(true));

            // Then
            assertThat(called.get()).isTrue();
            assertThat(same).isSameAs(result);
        }

        @Test
        @DisplayName("peek() should execute action on failure too")
        void peekShouldExecuteActionOnFailureToo() {
            // Given
            Result<String, Exception> result = Result.failure(new RuntimeException());
            AtomicBoolean called = new AtomicBoolean(false);

            // When
            Result<String, Exception> same = result.peek(r -> called.set(true));

            // Then
            assertThat(called.get()).isTrue();
            assertThat(same).isSameAs(result);
        }
    }

    // ==================== Recovery Tests ====================

    @Nested
    @DisplayName("Recovery Operations")
    class RecoveryTests {

        @Test
        @DisplayName("recover() should return success with recovered value on failure")
        void recoverShouldReturnSuccessOnFailure() {
            // Given
            Result<String, Exception> result = Result.failure(new RuntimeException("error"));

            // When
            Result<String, Exception> recovered = result.recover(e -> "recovered");

            // Then
            assertThat(recovered.isSuccess()).isTrue();
            assertThat(recovered.orElseThrow()).isEqualTo("recovered");
        }

        @Test
        @DisplayName("recover() should preserve original success")
        void recoverShouldPreserveOriginalSuccess() {
            // Given
            Result<String, Exception> result = Result.success("original");

            // When
            Result<String, Exception> same = result.recover(e -> "recovered");

            // Then
            assertThat(same.isSuccess()).isTrue();
            assertThat(same.orElseThrow()).isEqualTo("original");
        }

        @Test
        @DisplayName("recoverWith() should return recovered result on failure")
        void recoverWithShouldReturnRecoveredResultOnFailure() {
            // Given
            Result<String, Exception> result = Result.failure(new RuntimeException("error"));

            // When
            Result<String, Exception> recovered = result.recoverWith(e -> Result.success("recovered"));

            // Then
            assertThat(recovered.isSuccess()).isTrue();
            assertThat(recovered.orElseThrow()).isEqualTo("recovered");
        }

        @Test
        @DisplayName("recoverWith() can fail with different error")
        void recoverWithCanFailWithDifferentError() {
            // Given
            Result<String, Exception> result = Result.failure(new RuntimeException("first"));

            // When
            Result<String, Exception> stillFailed = result.recoverWith(
                    e -> Result.failure(new IllegalArgumentException("second"))
            );

            // Then
            assertThat(stillFailed.isFailure()).isTrue();
        }

        @Test
        @DisplayName("recoverWith() should preserve original success")
        void recoverWithShouldPreserveOriginalSuccess() {
            // Given
            Result<String, Exception> result = Result.success("original");

            // When
            Result<String, Exception> same = result.recoverWith(e -> Result.success("recovered"));

            // Then
            assertThat(same.isSuccess()).isTrue();
            assertThat(same.orElseThrow()).isEqualTo("original");
        }
    }

    // ==================== Filter Tests ====================

    @Nested
    @DisplayName("Filter Operations")
    class FilterTests {

        @Test
        @DisplayName("filter() should keep success when predicate passes")
        void filterShouldKeepSuccessWhenPredicatePasses() {
            // Given
            Result<Integer, Exception> result = Result.success(10);

            // When
            Result<Integer, Exception> filtered = result.filter(
                    n -> n > 5,
                    () -> new RuntimeException("too small")
            );

            // Then
            assertThat(filtered.isSuccess()).isTrue();
            assertThat(filtered.orElseThrow()).isEqualTo(10);
        }

        @Test
        @DisplayName("filter() should convert to failure when predicate fails")
        void filterShouldConvertToFailureWhenPredicateFails() {
            // Given
            Result<Integer, Exception> result = Result.success(3);

            // When
            Result<Integer, Exception> filtered = result.filter(
                    n -> n > 5,
                    () -> new RuntimeException("too small")
            );

            // Then
            assertThat(filtered.isFailure()).isTrue();
        }

        @Test
        @DisplayName("filter() should preserve failure")
        void filterShouldPreserveFailure() {
            // Given
            Result<Integer, Exception> result = Result.failure(new RuntimeException("original"));

            // When
            Result<Integer, Exception> filtered = result.filter(
                    n -> n > 5,
                    () -> new RuntimeException("from filter")
            );

            // Then
            assertThat(filtered.isFailure()).isTrue();
        }
    }
}
