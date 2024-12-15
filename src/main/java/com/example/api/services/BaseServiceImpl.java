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


   /**
    * Busca entidades en la base de datos basándose en los criterios proporcionados.
    *
    * @param criterios una lista de mapas, donde cada mapa contiene:
    *                  - "atributo": el nombre del atributo por el cual se realizará la búsqueda
    *                  - "valor": el valor que debe coincidir con el atributo
    *                  - "operador": el operador de comparación (por ejemplo, "=", ">", "<")
    * @return una lista de DTOs que cumplen con los criterios de búsqueda
    * @throws ValidationException      si no se proporciona al menos un criterio de búsqueda
    * @throws OperationFailedException si ocurre un error al realizar la búsqueda
    */
   @Override
   @Transactional(readOnly = true)
   public List<D> buscar(List<Map<String, String>> criterios) {
      // Verifica que se proporcionen criterios de búsqueda
      if (criterios == null || criterios.isEmpty()) {
         throw new ValidationException("Debe proporcionar al menos un criterio de búsqueda");
      }

      // Obtiene las clases de DTO y entidad
      Class<D> dtoClass = getDTOClass();
      Class<E> entityClass = getEntityClass();

      // Crea un CriteriaBuilder para construir la consulta
      CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
      // Crea una CriteriaQuery para la entidad especificada
      CriteriaQuery<E> criteriaQuery = criteriaBuilder.createQuery(entityClass);
      // Define el "root" de la consulta, que representa la entidad principal
      Root<E> root = criteriaQuery.from(entityClass);

      // Crea un predicado vacío para combinar los criterios
      Predicate combinedPredicate = criteriaBuilder.conjunction();

      // Itera sobre cada criterio proporcionado
      for (Map<String, String> criterio : criterios) {
         // Extrae el atributo, valor y operador del criterio
         String atributo = criterio.get("atributo");
         String valor = criterio.get("valor");
         String operador = criterio.get("operador");

         // Valida que el atributo proporcionado sea válido
         validateAttribute(dtoClass, atributo);
         // Convierte el valor al tipo adecuado según el atributo
         Object valorConvertido = convertValue(dtoClass, atributo, valor);
         // Crea un predicado basado en el atributo, valor y operador
         Predicate predicate = createPredicate(criteriaBuilder, root, atributo, valorConvertido, operador);
         // Combina el predicado actual con los predicados anteriores
         combinedPredicate = criteriaBuilder.and(combinedPredicate, predicate);
      }

      // Configura la consulta para seleccionar el root con los predicados combinados
      criteriaQuery.select(root).where(combinedPredicate);
      // Ejecuta la consulta y obtiene los resultados
      List<E> resultados = entityManager.createQuery(criteriaQuery).getResultList();
      // Convierte los resultados de entidades a DTOs y los devuelve
      return mapper.toDTOList(resultados);
   }


   /**
    * Valida si el atributo proporcionado existe en la clase DTO especificada.
    *
    * @param dtoClass la clase DTO en la que se buscará el atributo
    * @param atributo el nombre del atributo a validar
    * @throws ValidationException si el atributo no es válido o no existe en la clase DTO
    */
   private void validateAttribute(Class<D> dtoClass, String atributo) {
      try {
         // Itera sobre los campos declarados de la clase DTO
         for (Field field : dtoClass.getDeclaredFields()) {
            // Compara el nombre del campo con el atributo proporcionado
            if (field.getName().equals(atributo)) {
               return; // Si se encuentra, el atributo es válido
            }
         }
         // Si no se encuentra el atributo, lanza una excepción
         throw new NoSuchFieldException("Atributo no válido: " + atributo);
      } catch (NoSuchFieldException e) {
         // Maneja la excepción lanzada si el atributo no es válido
         throw new ValidationException("Atributo no válido: " + atributo);
      }
   }


   /**
    * Convierte el valor proporcionado a un tipo correspondiente al atributo especificado en la clase DTO.
    *
    * @param dtoClass la clase DTO en la que se encuentra el atributo
    * @param atributo el nombre del atributo cuyo tipo se utilizará para la conversión
    * @param valor    el valor en forma de cadena que se desea convertir
    * @return el valor convertido al tipo del atributo
    * @throws ValidationException si ocurre un error al convertir el valor o si el atributo no existe
    */
   private Object convertValue(Class<D> dtoClass, String atributo, String valor) {
      try {
         // Obtiene el tipo del campo correspondiente al atributo de la clase DTO
         Class<?> fieldType = dtoClass.getDeclaredField(atributo).getType();
         // Crea un conversor de tipos simple
         SimpleTypeConverter typeConverter = new SimpleTypeConverter();
         // Convierte el valor al tipo correspondiente del atributo
         return typeConverter.convertIfNecessary(valor, fieldType);
      } catch (NumberFormatException e) {
         // Lanza una excepción de validación si hay un error en la conversión
         throw new ValidationException("Error al convertir el valor: " + valor);
      } catch (NoSuchFieldException e) {
         // Lanza una excepción de validación si el atributo no se encuentra en la clase DTO
         throw new ValidationException("Atributo no encontrado: " + atributo);
      }
   }


   /**
    * Crea un objeto Predicate basado en el operador y los valores proporcionados para el atributo especificado.
    *
    * @param criteriaBuilder el CriteriaBuilder utilizado para construir la consulta
    * @param root            la raíz de la consulta que representa la entidad
    * @param atributo        el nombre del atributo para el que se crea el predicado
    * @param valorConvertido el valor convertido que se utilizará en la comparación
    * @param operador        el operador de comparación a utilizar (por ejemplo, "mayor", "menor", "igual")
    * @return un Predicate que representa la condición de comparación para la consulta
    * @throws ValidationException si se proporciona un operador no válido
    */
   private Predicate createPredicate(CriteriaBuilder criteriaBuilder, Root<E> root, String atributo, Object valorConvertido, String operador) {
      // Verifica si se ha proporcionado un operador
      if (operador != null) {
         switch (operador.toLowerCase()) {
            case "mayor":
               // Crea un predicado que representa "mayor que"
               return criteriaBuilder.greaterThan(root.get(atributo), (Comparable) valorConvertido);
            case "menor":
               // Crea un predicado que representa "menor que"
               return criteriaBuilder.lessThan(root.get(atributo), (Comparable) valorConvertido);
            case "igual":
               // Crea un predicado que representa "igual a"
               return criteriaBuilder.equal(root.get(atributo), valorConvertido);
            default:
               // Lanza una excepción si se proporciona un operador no válido
               throw new ValidationException("Operador no válido: " + operador);
         }
      } else {
         // Si no se proporciona un operador, se predetermina a igualdad
         return criteriaBuilder.equal(root.get(atributo), valorConvertido);
      }
   }


   /**
    * Obtiene la clase del DTO (Data Transfer Object) asociado a este servicio.
    *
    * @return la clase del DTO correspondiente
    * @throws ClassCastException si ocurre un error al cast de los tipos genericos
    */
   @SuppressWarnings("unchecked")
   private Class<D> getDTOClass() {
      // Obtiene el tipo genérico de la superclase
      Type genericSuperclass = getClass().getGenericSuperclass();
      // Cast a ParameterizedType para acceder a los tipos de los parámetros
      ParameterizedType parameterizedType = (ParameterizedType) genericSuperclass;
      // Devuelve la clase del segundo tipo de argumento, que corresponde al DTO
      return (Class<D>) parameterizedType.getActualTypeArguments()[1];
   }


   /**
    * Obtiene la clase de la entidad asociada a este servicio.
    *
    * @return la clase de la entidad correspondiente
    * @throws ClassCastException si ocurre un error al cast de los tipos genericos
    */
   @SuppressWarnings("unchecked")
   private Class<E> getEntityClass() {
      // Obtiene el tipo genérico de la superclase
      Type genericSuperclass = getClass().getGenericSuperclass();
      // Cast a ParameterizedType para acceder a los tipos de los parámetros
      ParameterizedType parameterizedType = (ParameterizedType) genericSuperclass;
      // Devuelve la clase del primer tipo de argumento, que corresponde a la entidad
      return (Class<E>) parameterizedType.getActualTypeArguments()[0];
   }

}
