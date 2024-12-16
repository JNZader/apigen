package com.example.api.services;

import com.example.api.entities.Base;
import com.example.api.mappers.GenericMapper;
import com.example.api.repositories.BaseRepository;
import com.example.api.utils.OperationFailedException;
import com.example.api.utils.ResourceNotFoundException;
import com.example.api.utils.ValidationException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.beans.SimpleTypeConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

/**
 * Implementación abstracta del servicio base que proporciona operaciones
 * CRUD genéricas para entidades que extienden de la clase Base.
 *
 * @param <E>  el tipo de la entidad que extiende de Base
 * @param <D>  el tipo del Data Transfer Object (DTO)
 * @param <ID> el tipo del identificador de la entidad
 */
public abstract class BaseServiceImpl<E extends Base, D, ID extends Serializable> implements BaseService<E, D, ID> {

   protected final BaseRepository<E, ID> baseRepository;
   protected final GenericMapper<E, D> mapper;
   private final EntityManager entityManager;

   /**
    * Constructor que inicializa el repositorio base, el mapeador y el EntityManager.
    *
    * @param baseRepository el repositorio base utilizado para realizar operaciones CRUD
    * @param mapper         el mapeador que convierte entre entidades y DTOs
    * @param entityManager  el EntityManager para realizar operaciones de persistencia
    */
   @Autowired
   protected BaseServiceImpl(BaseRepository<E, ID> baseRepository, GenericMapper<E, D> mapper,
                             EntityManager entityManager) {
      this.baseRepository = baseRepository;
      this.mapper = mapper;
      this.entityManager = entityManager;
   }

   /**
    * Recupera todas las entidades de la base de datos y las convierte en una lista de DTOs.
    *
    * @return una lista de objetos DTO que representan todas las entidades en la base de datos
    * @throws OperationFailedException si ocurre un error al obtener la lista de entidades
    */
   @Override
   @Transactional(readOnly = true)
   public List<D> findAll() {
      try {
         List<E> entities = baseRepository.findAll();
         return mapper.toDTOList(entities);
      } catch (Exception e) {
         throw new OperationFailedException("Error al obtener la lista de entidades");
      }
   }


   /**
    * Recupera una página de entidades de la base de datos y las convierte en una página de DTOs.
    *
    * @param pageable la información de paginación y ordenamiento
    * @return una página de objetos DTO que representan las entidades en la base de datos
    * @throws OperationFailedException si ocurre un error al obtener la lista de entidades
    */
   @Override
   @Transactional(readOnly = true)
   public Page<D> findAll(Pageable pageable) {
      try {
         Page<E> entitiesPage = baseRepository.findAll(pageable);
         return entitiesPage.map(mapper::toDTO);
      } catch (Exception e) {
         throw new OperationFailedException("Error al obtener la lista de entidades");
      }
   }


   /**
    * Recupera una entidad de la base de datos por su identificador y la convierte en un DTO.
    *
    * @param id el identificador de la entidad a recuperar
    * @return el objeto DTO que representa la entidad, o lanza una excepción si no se encuentra
    * @throws ResourceNotFoundException si la entidad no se encuentra en la base de datos
    * @throws OperationFailedException  si ocurre un error al buscar la entidad
    */
   @Override
   @Transactional(readOnly = true)
   public D findById(ID id) {
      try {
         E entity = baseRepository.findById(id)
                 .orElseThrow(() -> new ResourceNotFoundException("Entidad no encontrada"));
         return mapper.toDTO(entity);
      } catch (Exception e) {
         throw new OperationFailedException("Error al buscar la entidad");
      }
   }


   /**
    * Guarda una nueva entidad en la base de datos a partir del DTO proporcionado.
    *
    * @param dto el objeto DTO que representa la entidad a guardar
    * @return el objeto DTO que representa la entidad guardada
    * @throws OperationFailedException si ocurre un error al guardar la entidad
    */
   @Override
   @Transactional
   public D save(D dto) {
      try {
         E entity = mapper.toEntity(dto);
         E savedEntity = baseRepository.save(entity);
         return mapper.toDTO(savedEntity);
      } catch (Exception e) {
         throw new OperationFailedException("Error al guardar la entidad");
      }
   }


   /**
    * Actualiza una entidad existente en la base de datos con los datos del DTO proporcionado.
    *
    * @param id  el identificador de la entidad a actualizar
    * @param dto el objeto DTO que contiene los nuevos datos para la entidad
    * @return el objeto DTO que representa la entidad actualizada
    * @throws ResourceNotFoundException si la entidad con el ID proporcionado no existe
    * @throws OperationFailedException  si ocurre un error al actualizar la entidad
    */
   @Override
   @Transactional
   public D update(ID id, D dto) {
      try {
         if (!baseRepository.existsById(id)) {
            throw new ResourceNotFoundException("Entidad no encontrada para actualizar");
         }
         E existingEntity = baseRepository.findById(id)
                 .orElseThrow(() -> new ResourceNotFoundException("Entidad no encontrada"));
         E updatedEntity = mapper.toEntity(dto);
         updatedEntity.setId(existingEntity.getId()); // Mantener el ID existente
         updatedEntity = baseRepository.save(updatedEntity);
         return mapper.toDTO(updatedEntity);
      } catch (Exception e) {
         throw new OperationFailedException("Error al actualizar la entidad");
      }
   }


   /**
    * Elimina una entidad de la base de datos identificada por el ID proporcionado.
    *
    * @param id el identificador de la entidad a eliminar
    * @return true si la entidad se eliminó con éxito
    * @throws ResourceNotFoundException si la entidad con el ID proporcionado no existe
    * @throws OperationFailedException  si ocurre un error al eliminar la entidad
    */
   @Override
   @Transactional
   public boolean delete(ID id) {
      try {
         if (!baseRepository.existsById(id)) {
            throw new ResourceNotFoundException("Entidad no encontrada para eliminar");
         }
         baseRepository.deleteById(id);
         return true;
      } catch (Exception e) {
         throw new OperationFailedException("Error al eliminar la entidad");
      }
   }
}