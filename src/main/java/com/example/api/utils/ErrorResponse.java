package com.example.api.utils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Clase que representa la respuesta de error que se devuelve en caso de que ocurra una excepción.
 */
@Data
@NoArgsConstructor // Genera un constructor sin argumentos
@AllArgsConstructor // Genera un constructor con todos los parámetros
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
}
