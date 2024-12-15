package com.example.api.utils;

/**
 * Excepción que se lanza cuando se intenta crear un recurso que ya existe.
 */
public class DuplicateResourceException extends RuntimeException {

   /**
    * Crea una nueva instancia de DuplicateResourceException con un mensaje específico.
    *
    * @param message el mensaje que describe la excepción
    */
   public DuplicateResourceException(String message) {
      super(message); // Llama al constructor de la clase padre (RuntimeException) con el mensaje
   }
}
