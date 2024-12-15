package com.example.api.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;

/**
 * La clase Base es una superclase que proporciona atributos comunes para
 * las entidades en la aplicación. Incluye un identificador único y un
 * estado que indica si la entidad está activa o inactiva.
 */
@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Base implements Serializable {

   private static final long serialVersionUID = 1L;

   /**
    * El identificador único de la entidad. Se genera automáticamente
    * utilizando la estrategia de identidad.
    */
   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   private Long id;

   /**
    * El estado de la entidad, que indica si está activa (true) o
    * inactiva (false). Este campo es obligatorio.
    */
   @Column(nullable = false)
   private boolean estado;
}
