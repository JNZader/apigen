package com.example.api.repositories;

import com.example.api.entities.Base;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.io.Serializable;

/**
 * La interfaz BaseRepository proporciona métodos de acceso a datos para las
 * entidades que extienden de la clase Base. Extiende de JpaRepository para
 * aprovechar las operaciones CRUD y la paginación.
 *
 * @param <E> el tipo de la entidad que extiende de Base
 * @param <ID> el tipo del identificador de la entidad
 */
@NoRepositoryBean
public interface BaseRepository<E extends Base, ID extends Serializable> extends JpaRepository<E, ID> {

   /**
    * Obtiene una página de entidades de tipo E.
    *
    * @param pageable objeto que contiene información de paginación
    * @return una página de entidades de tipo E
    */
   Page<E> findAll(Pageable pageable); // no es necesario al extender de PagingAndSortingRepository
}
