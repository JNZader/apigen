package com.jnzader.apigen.core.application.util;

import com.jnzader.apigen.core.domain.exception.OperationFailedException;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Una interfaz sellada que representa el resultado de una operación, que puede ser exitosa o
 * fallida. Este patrón permite un manejo de errores explícito y funcional, similar a Either en
 * lenguajes funcionales.
 *
 * @param <T> El tipo del valor en caso de éxito.
 * @param <E> El tipo del error en caso de fallo.
 */
public sealed interface Result<T, E> {

    /**
     * Implementación de {@link Result} que representa un éxito. Contiene el valor de la operación.
     */
    record Success<T, E>(T value) implements Result<T, E> {}

    /**
     * Implementación de {@link Result} que representa un fallo. Contiene el error de la operación.
     */
    record Failure<T, E>(E error) implements Result<T, E> {}

    // ==================== Factory Methods ====================

    /** Crea un Result exitoso con el valor proporcionado. */
    static <T, E> Result<T, E> success(T value) {
        return new Success<>(value);
    }

    /** Crea un Result fallido con el error proporcionado. */
    static <T, E> Result<T, E> failure(E error) {
        return new Failure<>(error);
    }

    /** Ejecuta una operación que puede lanzar una excepción y la envuelve en un Result. */
    static <T> Result<T, Exception> of(Supplier<T> supplier) {
        try {
            return success(supplier.get());
        } catch (Exception e) {
            return failure(e);
        }
    }

    /** Crea un Result a partir de un Optional. */
    static <T, E> Result<T, E> fromOptional(Optional<T> optional, Supplier<E> errorSupplier) {
        return optional.<Result<T, E>>map(Result::success)
                .orElseGet(() -> failure(errorSupplier.get()));
    }

    // ==================== Transformaciones ====================

    /**
     * Permite transformar el resultado aplicando una función dependiendo de si es un éxito o un
     * fallo.
     *
     * @param onSuccess La función a aplicar si es un éxito.
     * @param onFailure La función a aplicar si es un fallo.
     * @return El resultado de aplicar una de las dos funciones.
     * @param <R> El tipo del nuevo resultado.
     */
    default <R> R fold(
            Function<? super T, ? extends R> onSuccess,
            Function<? super E, ? extends R> onFailure) {
        return switch (this) {
            case Success<T, E>(T value) -> onSuccess.apply(value);
            case Failure<T, E>(E error) -> onFailure.apply(error);
        };
    }

    /** Transforma el valor si es un éxito, manteniendo el error si es un fallo. */
    default <U> Result<U, E> map(Function<? super T, ? extends U> mapper) {
        return switch (this) {
            case Success<T, E>(T value) -> new Success<>(mapper.apply(value));
            case Failure<T, E>(E error) -> new Failure<>(error);
        };
    }

    /** Encadena operaciones que retornan Result (monadic bind). */
    default <U> Result<U, E> flatMap(Function<? super T, Result<U, E>> mapper) {
        return switch (this) {
            case Success<T, E>(T value) -> mapper.apply(value);
            case Failure<T, E>(E error) -> new Failure<>(error);
        };
    }

    /** Transforma el error si es un fallo, manteniendo el valor si es un éxito. */
    default <F> Result<T, F> mapError(Function<? super E, ? extends F> mapper) {
        return switch (this) {
            case Success<T, E>(T value) -> new Success<>(value);
            case Failure<T, E>(E error) -> new Failure<>(mapper.apply(error));
        };
    }

    /** Encadena operaciones de error que retornan Result. */
    default <F> Result<T, F> flatMapError(Function<? super E, Result<T, F>> mapper) {
        return switch (this) {
            case Success<T, E>(T value) -> new Success<>(value);
            case Failure<T, E>(E error) -> mapper.apply(error);
        };
    }

    // ==================== Extracción de valores ====================

    /** Retorna el valor si es éxito, o el valor por defecto si es fallo. */
    default T orElse(T defaultValue) {
        return switch (this) {
            case Success<T, E>(T value) -> value;
            case Failure<T, E> _ -> defaultValue;
        };
    }

    /** Retorna el valor si es éxito, o calcula un valor por defecto si es fallo. */
    default T orElseGet(Function<? super E, ? extends T> supplier) {
        return switch (this) {
            case Success<T, E>(T value) -> value;
            case Failure<T, E>(E error) -> supplier.apply(error);
        };
    }

    /**
     * Retorna el valor si es éxito, o lanza la excepción si es fallo. Si el error no es un
     * Throwable, lanza OperationFailedException con el mensaje del error.
     *
     * @return El valor contenido si es éxito.
     * @throws OperationFailedException si es fallo y el error no es Throwable o es checked
     *     exception.
     * @throws RuntimeException si es fallo y el error es RuntimeException.
     * @throws Error si es fallo y el error es Error.
     */
    @SuppressWarnings("unchecked")
    default T orElseThrow() {
        return switch (this) {
            case Success<T, E>(T value) -> value;
            case Failure<T, E>(E error) -> {
                if (error instanceof RuntimeException re) {
                    throw re;
                }
                if (error instanceof Error err) {
                    throw err;
                }
                if (error instanceof Throwable t) {
                    throw new OperationFailedException("Operation failed", t);
                }
                throw new OperationFailedException("Result failed with error: " + error);
            }
        };
    }

    /** Retorna el valor si es éxito, o lanza la excepción proporcionada si es fallo. */
    default <X extends Throwable> T orElseThrow(Function<? super E, ? extends X> exceptionMapper)
            throws X {
        return switch (this) {
            case Success<T, E>(T value) -> value;
            case Failure<T, E>(E error) -> throw exceptionMapper.apply(error);
        };
    }

    /** Convierte el Result a un Optional (pierde información del error). */
    default Optional<T> toOptional() {
        return switch (this) {
            case Success<T, E>(T value) -> Optional.ofNullable(value);
            case Failure<T, E> _ -> Optional.empty();
        };
    }

    // ==================== Predicados ====================

    /** Retorna true si es un éxito. */
    default boolean isSuccess() {
        return this instanceof Success;
    }

    /** Retorna true si es un fallo. */
    default boolean isFailure() {
        return this instanceof Failure;
    }

    // ==================== Side effects ====================

    /** Ejecuta una acción si es éxito. */
    default Result<T, E> onSuccess(Consumer<? super T> action) {
        if (this instanceof Success<T, E>(T value)) {
            action.accept(value);
        }
        return this;
    }

    /** Ejecuta una acción si es fallo. */
    default Result<T, E> onFailure(Consumer<? super E> action) {
        if (this instanceof Failure<T, E>(E error)) {
            action.accept(error);
        }
        return this;
    }

    /** Ejecuta una acción independientemente del resultado. */
    default Result<T, E> peek(Consumer<Result<T, E>> action) {
        action.accept(this);
        return this;
    }

    // ==================== Recuperación ====================

    /** Intenta recuperarse de un fallo con un valor alternativo. */
    default Result<T, E> recover(Function<? super E, ? extends T> recoveryFunction) {
        return switch (this) {
            case Success<T, E> s -> s;
            case Failure<T, E>(E error) -> new Success<>(recoveryFunction.apply(error));
        };
    }

    /** Intenta recuperarse de un fallo con otro Result. */
    default Result<T, E> recoverWith(Function<? super E, Result<T, E>> recoveryFunction) {
        return switch (this) {
            case Success<T, E> s -> s;
            case Failure<T, E>(E error) -> recoveryFunction.apply(error);
        };
    }

    // ==================== Filtrado ====================

    /** Filtra el valor según un predicado, convirtiendo a fallo si no cumple. */
    default Result<T, E> filter(
            java.util.function.Predicate<? super T> predicate, Supplier<E> errorSupplier) {
        return switch (this) {
            case Success<T, E>(T value) ->
                    predicate.test(value)
                            ? new Success<>(value)
                            : new Failure<>(errorSupplier.get());
            case Failure<T, E> f -> f;
        };
    }
}
