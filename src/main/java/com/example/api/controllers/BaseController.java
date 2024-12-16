package com.example.api.controllers;

import com.example.api.entities.Base;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * BaseController es una interfaz que define las operaciones básicas
 * para el manejo de entidades en la aplicación.
 *
 * @param <E>  el tipo de la entidad que extiende de {@link Base}
 * @param <D>  el tipo del DTO (Data Transfer Object) asociado
 * @param <ID> el tipo del identificador de la entidad, que debe ser serializable
 */
public interface BaseController<E extends Base, D, ID extends Serializable> {

   /**
    * Obtiene todas las entidades.
    *
    * @return una respuesta que contiene una lista de todas las entidades.
    */
   public ResponseEntity<?> getAll();

   /**
    * Obtiene todas las entidades con paginación.
    *
    * @param pageable objeto que contiene información de paginación
    * @return una respuesta que contiene una lista paginada de entidades.
    */
   public ResponseEntity<?> getAll(Pageable pageable);

   /**
    * Obtiene una entidad específica por su ID.
    *
    * @param id el identificador de la entidad a recuperar
    * @return una respuesta que contiene la entidad correspondiente al ID.
    */
   public ResponseEntity<?> getOne(@PathVariable ID id);

   /**
    * Guarda una nueva entidad.
    *
    * @param dto el DTO que representa la entidad a guardar
    * @return una respuesta que indica el resultado de la operación.
    */
   public ResponseEntity<?> save(@RequestBody D dto);

   /**
    * Actualiza una entidad existente.
    *
    * @param id  el identificador de la entidad a actualizar
    * @param dto el DTO que contiene los datos actualizados de la entidad
    * @return una respuesta que indica el resultado de la operación.
    */
   public ResponseEntity<?> update(@PathVariable ID id, @RequestBody D dto);

   /**
    * Elimina una entidad por su ID.
    *
    * @param id el identificador de la entidad a eliminar
    * @return una respuesta que indica el resultado de la operación.
    */
   public ResponseEntity<?> delete(@PathVariable ID id);
}
