package com.example.api.services;

import com.example.api.dto.BaseDTO;
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

   /**
    * La lista de repositorios utilizados para realizar operaciones CRUD.
    */
   protected final List<BaseRepository<?, ID>> repositories;

   /**
    * La lista de mappers utilizados para convertir entre entidades y DTO.
    */
   protected final List<GenericMapper<? extends Base, ? extends BaseDTO>> mapperGen;

   /**
    * El repositorio base utilizado para realizar operaciones CRUD.
    */
   protected final BaseRepository<E, ID> baseRepository;

   /**
    * El mapeador que convierte entre entidades y DTOs.
    */
   protected final GenericMapper<E, D> mapper;

   /**
    * El EntityManager para realizar operaciones de persistencia.
    */
   private final EntityManager entityManager;

   /**
    * Constructor que inicializa el repositorio base, el mapeador y el EntityManager.
    *
    * @param repositories   la lista de repositorios utilizados para realizar operaciones CRUD
    * @param mapperGen      la lista de mappers utilizados para convertir entre entidades y DTOs
    * @param baseRepository el repositorio base utilizado para realizar operaciones CRUD
    * @param mapper         el mapeador que convierte entre entidades y DTOs
    * @param entityManager  el EntityManager para realizar operaciones de persistencia
    */
   @Autowired
   protected BaseServiceImpl(List<BaseRepository<?, ID>> repositories,
                             List<GenericMapper<? extends Base, ? extends BaseDTO>> mapperGen,
                             BaseRepository<E, ID> baseRepository,
                             GenericMapper<E, D> mapper,
                             EntityManager entityManager) {
      this.repositories = repositories;
      this.mapperGen = mapperGen;
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
         // Utiliza el repositorio base para obtener todas las entidades
         List<E> entities = baseRepository.findAll();
         // Convierte las entidades en una lista de DTOs utilizando el mapeador
         return mapper.toDTOList(entities);
      } catch (Exception e) {
         // Lanza una excepción de operación fallida si ocurre un error
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
         // Utiliza el repositorio base para obtener una página de entidades
         Page<E> entitiesPage = baseRepository.findAll(pageable);
         // Convierte la página de entidades en una página de DTOs utilizando el mapeador
         return entitiesPage.map(mapper::toDTO);
      } catch (Exception e) {
         // Lanza una excepción de operación fallida si ocurre un error
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
         // Utiliza el repositorio base para obtener la entidad por su identificador
         E entity = baseRepository.findById(id)
                 .orElseThrow(() -> new ResourceNotFoundException("Entidad no encontrada"));
         // Convierte la entidad en un DTO utilizando el mapeador
         return mapper.toDTO(entity);
      } catch (Exception e) {
         // Lanza una excepción de operación fallida si ocurre un error
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
         // Convierte el DTO en una entidad utilizando el mapeador
         E entity = mapper.toEntity(dto);
         // Utiliza el repositorio base para guardar la entidad
         E savedEntity = baseRepository.save(entity);
         // Convierte la entidad guardada en un DTO utilizando el mapeador
         return mapper.toDTO(savedEntity);
      } catch (Exception e) {
         // Lanza una excepción de operación fallida si ocurre un error
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
         // Verifica si la entidad existe en la base de datos
         if (!baseRepository.existsById(id)) {
            // Lanza una excepción de recurso no encontrado si la entidad no existe
            throw new ResourceNotFoundException("Entidad no encontrada para actualizar");
         }
         // Utiliza el repositorio base para obtener la entidad por su identificador
         E existingEntity = baseRepository.findById(id)
                 .orElseThrow(() -> new ResourceNotFoundException("Entidad no encontrada"));
         // Convierte el DTO en una entidad utilizando el mapeador
         E updatedEntity = mapper.toEntity(dto);
         // Mantén el ID existente de la entidad
         updatedEntity.setId(existingEntity.getId());
         // Utiliza el repositorio base para guardar la entidad actualizada
         updatedEntity = baseRepository.save(updatedEntity);
         // Convierte la entidad actualizada en un DTO utilizando el mapeador
         return mapper.toDTO(updatedEntity);
      } catch (Exception e) {
         // Lanza una excepción de operación fallida si ocurre un error
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
         // Verifica si la entidad existe en la base de datos
         if (!baseRepository.existsById(id)) {
            // Lanza una excepción de recurso no encontrado si la entidad no existe
            throw new ResourceNotFoundException("Entidad no encontrada para eliminar");
         }
         // Utiliza el repositorio base para eliminar la entidad
         baseRepository.deleteById(id);
         // Regresa true si la entidad se eliminó con éxito
         return true;
      } catch (Exception e) {
         // Lanza una excepción de operación fallida si ocurre un error
         throw new OperationFailedException("Error al eliminar la entidad");
      }
   }

   /**
    * Obtiene un repositorio específico de una entidad.
    *
    * @param clazz el tipo de repositorio a obtener
    * @return el repositorio específico de la entidad
    * @throws ResourceNotFoundException si el repositorio no se encuentra
    */
   public <T extends BaseRepository<?, Long>> T getRepository(Class<T> clazz) {
      return getRepositoryImpl(clazz);
   }

   @SuppressWarnings("unchecked")
   private <T extends BaseRepository<?, Long>> T getRepositoryImpl(Class<T> clazz) {
      // Utiliza el método `stream` para obtener un flujo de repositorios
      return (T) repositories.stream()
              .filter(clazz::isInstance)
              .findFirst()
              .orElseThrow(() -> new ResourceNotFoundException("Repositorio no encontrado"));
   }

   /**
    * Obtiene el mapper específico para convertir entre una entidad y su correspondiente DTO
    */
   @SuppressWarnings("unchecked")
   protected <T extends Base, U extends BaseDTO> GenericMapper<T, U> getMapper(Class<T> entityClass, Class<U> dtoClass) {
      return (GenericMapper<T, U>) mapperGen.stream()
              .filter(mapper
                      ->mapper.getEntityClass().equals(entityClass)
                      &&mapper.getDtoClass().equals(dtoClass))
              .findFirst()
              .orElseThrow(()->new ResourceNotFoundException("Mapper no encontrado para "+entityClass.getSimpleName()));
   }

}
