package com.example.api.utils;

/**
 * Excepción personalizada que se lanza cuando una acción no está autorizada.
 * Esta clase extiende RuntimeException, lo que significa que es una excepción
 * no comprobada (unchecked) y no requiere declaración en los métodos.
 */
public class UnauthorizedActionException extends RuntimeException {

   /**
    * Constructor que crea una nueva instancia de UnauthorizedActionException.
    *
    * @param message Mensaje que describe el motivo de la excepción. Este mensaje
    *                se pasará a la clase base RuntimeException.
    */
   public UnauthorizedActionException(String message) {
      super(message); // Llama al constructor de la clase base con el mensaje proporcionado.
   }
}
