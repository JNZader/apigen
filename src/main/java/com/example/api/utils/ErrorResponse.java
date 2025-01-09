package com.example.api.utils;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Clase que representa la respuesta de error que se devuelve en caso de que
 * ocurra una excepción.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorResponse {

    /**
     * Mensaje descriptivo del error.
     */
    private String mensaje;

    /**
     * Código de error asociado a la respuesta.
     */
    private int codigoError;

    /**
     * Detalles adicionales sobre el error.
     */
    private String detalles;

    /**
     * Lista de errores específicos de validación.
     */
    private List<String> errores;

}

