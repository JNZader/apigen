package com.example.api.controllers;

import com.example.api.dto.BaseDTO;
import com.example.api.entities.Base;
import com.example.api.services.BaseServiceImpl;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * BaseControllerImpl es una clase abstracta que implementa la interfaz {@link BaseController}
 * y proporciona la implementación básica de las operaciones CRUD para entidades.
 *
 * @param <E> el tipo de la entidad que extiende de {@link Base}
 * @param <D> el tipo del DTO (Data Transfer Object) asociado que extiende de {@link BaseDTO}
 * @param <S> el tipo del servicio que extiende de {@link BaseServiceImpl}
 */
public abstract class BaseControllerImpl<E extends Base, D extends BaseDTO, S extends BaseServiceImpl<E, D, Long>>
      implements BaseController<E, D, Long> {

   @Autowired
   protected S servicio;

   /**
    * Obtiene todas las entidades.
    *
    * @return una respuesta que contiene una lista de todos los DTOs.
    */
   @Override
   @GetMapping("")
   public ResponseEntity<List<D>> getAll() {
      List<D> dtos = servicio.findAll();
      return new ResponseEntity<>(dtos, HttpStatus.OK);
   }

   /**
    * Obtiene todas las entidades con paginación.
    *
    * @param pageable objeto que contiene información de paginación
    * @return una respuesta que contiene una lista paginada de DTOs.
    */
   @Override
   @GetMapping("/paged")
   public ResponseEntity<Page<D>> getAll(Pageable pageable) {
      Page<D> dtosPage = servicio.findAll(pageable);
      return new ResponseEntity<>(dtosPage, HttpStatus.OK);
   }

   /**
    * Obtiene una entidad específica por su ID.
    *
    * @param id el identificador de la entidad a recuperar
    * @return una respuesta que contiene el DTO correspondiente al ID.
    */
   @Override
   @GetMapping("/{id}")
   public ResponseEntity<D> getOne(@PathVariable Long id) {
      D dto = servicio.findById(id);
      return new ResponseEntity<>(dto, HttpStatus.OK);
   }

   /**
    * Guarda una nueva entidad.
    *
    * @param dto el DTO que representa la entidad a guardar
    * @return una respuesta que contiene el DTO guardado.
    */
   @Override
   @PostMapping("")
   public ResponseEntity<D> save(@Valid @RequestBody D dto) {
      D savedDto = servicio.save(dto);
      return new ResponseEntity<>(savedDto, HttpStatus.CREATED);
   }

   /**
    * Actualiza una entidad existente.
    *
    * @param id  el identificador de la entidad a actualizar
    * @param dto el DTO que contiene los datos actualizados de la entidad
    * @return una respuesta que contiene el DTO actualizado.
    */
   @Override
   @PutMapping("/{id}")
   public ResponseEntity<D> update(@PathVariable Long id, @Valid @RequestBody D dto) {
      D updatedDto = servicio.update(id, dto);
      return new ResponseEntity<>(updatedDto, HttpStatus.OK);
   }

   /**
    * Elimina una entidad por su ID.
    *
    * @param id el identificador de la entidad a eliminar
    * @return una respuesta que indica el resultado de la operación.
    */
   @Override
   @DeleteMapping("/{id}")
   public ResponseEntity<Void> delete(@PathVariable Long id) {
      boolean deleted = servicio.delete(id);
      return deleted ? new ResponseEntity<>(HttpStatus.NO_CONTENT) : new ResponseEntity<>(HttpStatus.NOT_FOUND);
   }
}
