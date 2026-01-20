package com.jnzader.apigen.core.application.mapper;

import com.jnzader.apigen.core.application.dto.BaseDTO;
import com.jnzader.apigen.core.domain.entity.Base;
import java.util.List;
import org.mapstruct.BeanMapping;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * Interfaz de mapeo genérica para la conversión entre una entidad y su DTO correspondiente. Utiliza
 * MapStruct para generar la implementación en tiempo de compilación.
 *
 * <p>Proporciona métodos para: - Conversión entity -> DTO - Conversión DTO -> entity -
 * Actualización parcial de entidades (para PATCH) - Conversión de listas
 *
 * @param <E> El tipo de la entidad, que extiende {@link Base}.
 * @param <D> El tipo del DTO, que extiende {@link BaseDTO}.
 */
public interface BaseMapper<E extends Base, D extends BaseDTO> {

    /**
     * Convierte una entidad a su DTO correspondiente. Mapea el campo 'estado' de la entidad al
     * campo 'activo' del DTO.
     *
     * @param entity La entidad a convertir.
     * @return El DTO convertido.
     */
    @Mapping(source = "estado", target = "activo")
    D toDTO(E entity);

    /**
     * Convierte un DTO a su entidad correspondiente. Realiza el mapeo inverso de 'activo' a
     * 'estado'.
     *
     * @param dto El DTO a convertir.
     * @return La entidad convertida.
     */
    @InheritInverseConfiguration(name = "toDTO")
    E toEntity(D dto);

    /**
     * Convierte una lista de entidades a una lista de DTOs.
     *
     * @param entities La lista de entidades a convertir.
     * @return La lista de DTOs convertidos.
     */
    List<D> toDTOList(List<E> entities);

    /**
     * Convierte una lista de DTOs a una lista de entidades.
     *
     * @param dtos La lista de DTOs a convertir.
     * @return La lista de entidades convertidas.
     */
    List<E> toEntityList(List<D> dtos);

    /**
     * Actualiza una entidad existente con los valores no nulos del DTO. Útil para operaciones PATCH
     * donde solo se envían los campos a modificar. Los campos nulos en el DTO no sobrescribirán los
     * valores existentes en la entidad.
     *
     * @param dto El DTO con los valores a actualizar.
     * @param entity La entidad destino a actualizar.
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(source = "activo", target = "estado")
    void updateEntityFromDTO(D dto, @MappingTarget E entity);

    /**
     * Actualiza un DTO existente con los valores de la entidad.
     *
     * @param entity La entidad fuente.
     * @param dto El DTO destino a actualizar.
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(source = "estado", target = "activo")
    void updateDTOFromEntity(E entity, @MappingTarget D dto);
}
