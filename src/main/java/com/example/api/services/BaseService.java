package com.example.api.services;

import com.example.api.entities.Base;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * La interfaz BaseService define las operaciones básicas para el manejo de
 * entidades que extienden de la clase Base. Proporciona métodos para realizar
 * operaciones CRUD y búsquedas personalizadas.
 *
 * @param <E> el tipo de la entidad que extiende de Base
 * @param <D> el tipo del Data Transfer Object (DTO)
 * @param <ID> el tipo del identificador de la entidad
 */
public interface BaseService<E extends Base, D, ID extends Serializable> {

   /**
    * Obtiene una lista de todos los DTOs.
    *
    * @return una lista de DTOs
    * @throws Exception si ocurre un error al obtener los datos
    */
   List<D> findAll() throws Exception;

   /**
    * Obtiene una página de DTOs.
    *
    * @param pageable objeto que contiene información de paginación
    * @return una página de DTOs
    * @throws Exception si ocurre un error al obtener los datos
    */
   Page<D> findAll(Pageable pageable) throws Exception;

   /**
    * Obtiene un DTO por su identificador.
    *
    * @param id el identificador del DTO
    * @return el DTO correspondiente
    * @throws Exception si ocurre un error al obtener el dato
    */
   D findById(ID id) throws Exception;

   /**
    * Guarda un nuevo DTO.
    *
    * @param dto el DTO a guardar
    * @return el DTO guardado
    * @throws Exception si ocurre un error al guardar el dato
    */
   D save(D dto) throws Exception;

   /**
    * Actualiza un DTO existente.
    *
    * @param id  el identificador del DTO a actualizar
    * @param dto el DTO con los nuevos datos
    * @return el DTO actualizado
    * @throws Exception si ocurre un error al actualizar el dato
    */
   D update(ID id, D dto) throws Exception;

   /**
    * Elimina un DTO por su identificador.
    *
    * @param id el identificador del DTO a eliminar
    * @return true si se eliminó correctamente, false en caso contrario
    * @throws Exception si ocurre un error al eliminar el dato
    */
   boolean delete(ID id) throws Exception;
}
