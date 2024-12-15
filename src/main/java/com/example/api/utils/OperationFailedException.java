package com.example.api.utils;

/**
 * Excepción personalizada que se lanza cuando una operación no puede completarse.
 * Esta clase extiende RuntimeException, lo que significa que es una excepción
 * no comprobada (unchecked) y no requiere declaración en los métodos.
 */
public class OperationFailedException extends RuntimeException {

   /**
    * Constructor que crea una nueva instancia de OperationFailedException.
    *
    * @param message Mensaje que describe el motivo de la excepción. Este mensaje
    *                se pasará a la clase base RuntimeException.
    */
   public OperationFailedException(String message) {
      super(message); // Llama al constructor de la clase base con el mensaje proporcionado.
   }
}
