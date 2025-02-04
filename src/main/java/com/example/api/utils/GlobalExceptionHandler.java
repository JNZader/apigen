package com.example.api.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Clase para manejar globalmente las excepciones que ocurren en la aplicación.
 * Proporciona respuestas estandarizadas para diferentes tipos de excepciones.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    // Logger para registrar los errores
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Manejo de excepciones cuando no se encuentra un recurso.
     *
     * @param ex la excepción de tipo ResourceNotFoundException
     * @param request el objeto WebRequest que contiene información sobre la
     * solicitud
     * @return una respuesta con el error correspondiente y el código de estado
     * 404 (NOT FOUND)
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException ex, WebRequest request) {
        logger.error("ResourceNotFoundException: {}", ex.getMessage(), ex); // Registra el error
        ErrorResponse errorResponse = ErrorResponse.builder()
                .mensaje(ex.getMessage())
                .codigoError(HttpStatus.NOT_FOUND.value())
                .detalles("El recurso solicitado no existe")
                .build();
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND); // Devuelve la respuesta con estado 404
    }

    /**
     * Manejo de excepciones de validación.
     *
     * @param ex la excepción de tipo ValidationException
     * @return una respuesta con el error correspondiente y el código de estado
     * 400 (BAD REQUEST)
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(ValidationException ex) {
        logger.error("ValidationException: {}", ex.getMessage(), ex); // Registra el error
        ErrorResponse errorResponse = ErrorResponse.builder()
                .mensaje(ex.getMessage())
                .codigoError(HttpStatus.BAD_REQUEST.value())
                .detalles("Error en la validación de los datos")
                .build();
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST); // Devuelve la respuesta con estado 400
    }

    /**
     * Manejo de excepciones de NoSuchFieldException.
     *
     * @param ex la excepción de tipo NoSuchFieldException
     * @return una respuesta con el error correspondiente y el código de estado
     * 400 (BAD REQUEST)
     */
    @ExceptionHandler(NoSuchFieldException.class)
    public ResponseEntity<ErrorResponse> handleNoSuchFieldException(NoSuchFieldException ex) {
        logger.error("NoSuchFieldException: {}", ex.getMessage(), ex); // Registra el error
        ErrorResponse errorResponse = ErrorResponse.builder()
                .mensaje(ex.getMessage())
                .codigoError(HttpStatus.BAD_REQUEST.value())
                .detalles("Atributo no válido")
                .build();
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST); // Devuelve la respuesta con estado 400
    }

    /**
     * Manejo de excepciones cuando un recurso ya existe.
     *
     * @param ex la excepción de tipo DuplicateResourceException
     * @return una respuesta con el error correspondiente y el código de estado
     * 409 (CONFLICT)
     */
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResourceException(DuplicateResourceException ex) {
        logger.error("DuplicateResourceException: {}", ex.getMessage(), ex); // Registra el error
        ErrorResponse errorResponse = ErrorResponse.builder()
                .mensaje(ex.getMessage())
                .codigoError(HttpStatus.CONFLICT.value())
                .detalles("El recurso ya existe")
                .build();
        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT); // Devuelve la respuesta con estado 409
    }

    /**
     * Manejo de excepciones de operación fallida.
     *
     * @param ex la excepción de tipo OperationFailedException
     * @return una respuesta con el error correspondiente y el código de estado
     * 500 (INTERNAL SERVER ERROR)
     */
    @ExceptionHandler(OperationFailedException.class)
    public ResponseEntity<ErrorResponse> handleOperationFailedException(OperationFailedException ex) {
        logger.error("OperationFailedException: {}", ex.getMessage(), ex); // Registra el error
        ErrorResponse errorResponse = ErrorResponse.builder()
                .mensaje(ex.getMessage())
                .codigoError(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .detalles("La operación no pudo completarse")
                .build();
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR); // Devuelve la respuesta con estado 500
    }

    /**
     * Manejo de excepciones de violación de integridad de datos.
     *
     * @param ex la excepción de tipo DataIntegrityViolationException
     * @return una respuesta con el error correspondiente y el código de estado
     * 409 (CONFLICT)
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
        // Extraer detalles del mensaje de la excepción
        String detalles = Objects.requireNonNull(ex.getRootCause()).getMessage();

        // Crear un mensaje de respuesta más amigable y útil
        if (detalles.contains("foreign key constraint")) {
            detalles = "Error de integridad referencial: " + detalles; // Mensaje específico para errores de clave foránea
        } else {
            detalles = "Error de integridad de datos: " + detalles; // Mensaje genérico para otros errores de integridad
        }

        ErrorResponse errorResponse = ErrorResponse.builder()
                .mensaje("Ha ocurrido un error de integridad de datos")
                .codigoError(HttpStatus.CONFLICT.value())
                .detalles(detalles)
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT); // Devuelve la respuesta con estado 409
    }

    /**
     * Manejo de otras excepciones generales que no están específicamente
     * manejadas.
     *
     * @param ex la excepción general
     * @param request el objeto WebRequest que contiene información sobre la
     * solicitud
     * @return una respuesta con el error correspondiente y el código de estado
     * 500 (INTERNAL SERVER ERROR)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(Exception ex, WebRequest request) {
        logger.error("Exception: {}", ex.getMessage(), ex); // Registra el error

        // Obtiene información sobre la solicitud y la ubicación del error
        String path = request.getDescription(false);
        StackTraceElement element = ex.getStackTrace()[0];
        String detalles = String.format("Error en %s.%s() en %s: %s",
                element.getClassName(),
                element.getMethodName(),
                path,
                ex.getMessage()); // Detalles del error

        ErrorResponse errorResponse = ErrorResponse.builder()
                .mensaje("Ha ocurrido un error interno")
                .codigoError(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .detalles(detalles)
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR); // Devuelve la respuesta con estado 500
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult().getAllErrors().stream()
                .map(error -> {
                    if (error instanceof FieldError fieldError) {
                        return fieldError.getField() + ": " + fieldError.getDefaultMessage();
                    } else {
                        return error.getDefaultMessage();
                    }
                })
                .collect(Collectors.toList());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .mensaje("Error de validación")
                .codigoError(HttpStatus.BAD_REQUEST.value())
                .detalles("Por favor, corrija los siguientes errores de validación")
                .errores(errors)
                .build();
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

}
