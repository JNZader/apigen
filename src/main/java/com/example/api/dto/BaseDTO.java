package com.example.api.dto;

import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * BaseDTO es una clase base que representa un Data Transfer Object (DTO)
 * para las entidades en la aplicación. Proporciona los atributos comunes
 * para las entidades, como el identificador y el estado.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class BaseDTO {

   /**
    * El identificador único de la entidad.
    */
   private Long id;

   /**
    * El estado de la entidad, que puede indicar si está activa o inactiva.
    */
   private Boolean estado;
}
