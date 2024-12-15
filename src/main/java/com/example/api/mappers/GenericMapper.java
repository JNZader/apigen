package com.example.api.mappers;

import org.modelmapper.ModelMapper;

import java.util.List;
import java.util.stream.Collectors;

/**
 * La clase GenericMapper es un mapeador genérico que facilita la conversión
 * entre entidades y Data Transfer Objects (DTOs) utilizando ModelMapper.
 *
 * @param <E> el tipo de la entidad
 * @param <D> el tipo del DTO
 */
public class GenericMapper<E, D> {

   private final Class<E> entityClass;
   private final Class<D> dtoClass;
   private final ModelMapper modelMapper;

   /**
    * Constructor para inicializar el GenericMapper.
    *
    * @param entityClass la clase de la entidad
    * @param dtoClass    la clase del DTO
    */
   public GenericMapper(Class<E> entityClass, Class<D> dtoClass) {
      this.entityClass = entityClass;
      this.dtoClass = dtoClass;
      this.modelMapper = new ModelMapper();
   }

   /**
    * Convierte una entidad a su correspondiente DTO.
    *
    * @param entity la entidad a convertir
    * @return el DTO correspondiente
    */
   public D toDTO(E entity) {
      return modelMapper.map(entity, dtoClass);
   }

   /**
    * Convierte un DTO a su correspondiente entidad.
    *
    * @param dto el DTO a convertir
    * @return la entidad correspondiente
    */
   public E toEntity(D dto) {
      return modelMapper.map(dto, entityClass);
   }

   /**
    * Convierte una lista de entidades a una lista de DTOs.
    *
    * @param entities la lista de entidades a convertir
    * @return la lista de DTOs correspondientes
    */
   public List<D> toDTOList(List<E> entities) {
      return entities.stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
   }

   /**
    * Convierte una lista de DTOs a una lista de entidades.
    *
    * @param dtos la lista de DTOs a convertir
    * @return la lista de entidades correspondientes
    */
   public List<E> toEntityList(List<D> dtos) {
      return dtos.stream()
            .map(this::toEntity)
            .collect(Collectors.toList());
   }
}
