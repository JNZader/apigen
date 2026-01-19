package com.jnzader.apigen.core.infrastructure.hateoas;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import com.jnzader.apigen.core.application.dto.BaseDTO;
import com.jnzader.apigen.core.infrastructure.controller.BaseController;
import java.io.Serializable;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;

/**
 * Ensamblador genérico de recursos HATEOAS.
 *
 * <p>Convierte DTOs en EntityModel con links de navegación.
 *
 * <p>Links generados: - self: enlace al recurso actual - collection: enlace a la colección de
 * recursos - update: enlace para actualizar (PUT) - delete: enlace para eliminar (DELETE)
 *
 * @param <D> El tipo del DTO que extiende BaseDTO.
 * @param <I> El tipo del identificador.
 */
public abstract class BaseResourceAssembler<D extends BaseDTO, I extends Serializable>
        implements RepresentationModelAssembler<D, EntityModel<D>> {

    private final Class<? extends BaseController<D, I>> controllerClass;

    protected BaseResourceAssembler(Class<? extends BaseController<D, I>> controllerClass) {
        this.controllerClass = controllerClass;
    }

    @Override
    public EntityModel<D> toModel(D dto) {
        I id = extractId(dto);

        EntityModel<D> model = EntityModel.of(dto);

        // Self link - usando los nuevos parámetros
        model.add(linkTo(methodOn(controllerClass).findById(id, null, null)).withSelfRel());

        // Collection link
        model.add(
                linkTo(methodOn(controllerClass).findAll(null, null, null, Pageable.unpaged()))
                        .withRel("collection"));

        // Update link
        model.add(linkTo(methodOn(controllerClass).update(id, null, null)).withRel("update"));

        // Delete link (soft delete)
        model.add(linkTo(methodOn(controllerClass).delete(id, false)).withRel("delete"));

        // Links adicionales específicos de la subclase
        addCustomLinks(model, dto);

        return model;
    }

    @Override
    public CollectionModel<EntityModel<D>> toCollectionModel(Iterable<? extends D> entities) {
        List<EntityModel<D>> models = new java.util.ArrayList<>();
        entities.forEach(entity -> models.add(toModel(entity)));

        CollectionModel<EntityModel<D>> collectionModel = CollectionModel.of(models);

        // Self link para la colección
        collectionModel.add(
                linkTo(methodOn(controllerClass).findAll(null, null, null, Pageable.unpaged()))
                        .withSelfRel());

        return collectionModel;
    }

    /** Convierte una página de DTOs en un PagedModel con links de paginación. */
    public PagedModel<EntityModel<D>> toPagedModel(Page<D> page) {
        List<EntityModel<D>> content = page.getContent().stream().map(this::toModel).toList();

        PagedModel.PageMetadata metadata =
                new PagedModel.PageMetadata(
                        page.getSize(),
                        page.getNumber(),
                        page.getTotalElements(),
                        page.getTotalPages());

        PagedModel<EntityModel<D>> pagedModel = PagedModel.of(content, metadata);

        // Self link
        pagedModel.add(
                linkTo(methodOn(controllerClass).findAll(null, null, null, page.getPageable()))
                        .withSelfRel());

        // Links de navegación
        if (page.hasNext()) {
            pagedModel.add(
                    Link.of(buildPageUrl(page.getNumber() + 1, page.getSize())).withRel("next"));
        }
        if (page.hasPrevious()) {
            pagedModel.add(
                    Link.of(buildPageUrl(page.getNumber() - 1, page.getSize())).withRel("prev"));
        }
        pagedModel.add(Link.of(buildPageUrl(0, page.getSize())).withRel("first"));
        if (page.getTotalPages() > 0) {
            pagedModel.add(
                    Link.of(buildPageUrl(page.getTotalPages() - 1, page.getSize()))
                            .withRel("last"));
        }

        return pagedModel;
    }

    /**
     * Construye una URL de paginación. Las subclases pueden sobrescribir este método para
     * personalizar la URL.
     */
    protected String buildPageUrl(int page, int size) {
        return "?page=" + page + "&size=" + size;
    }

    /**
     * Permite a las subclases agregar links adicionales específicos.
     *
     * @param model El modelo al que agregar links.
     * @param dto El DTO fuente.
     */
    protected void addCustomLinks(EntityModel<D> model, D dto) {
        // Las subclases pueden sobrescribir para agregar links específicos
    }

    /**
     * Extrae el ID del DTO de forma segura. Las subclases pueden sobrescribir si necesitan un tipo
     * de ID diferente.
     *
     * @param dto El DTO del que extraer el ID.
     * @return El ID extraído.
     */
    @SuppressWarnings("unchecked")
    protected I extractId(D dto) {
        Long id = dto.id();
        if (id == null) {
            throw new IllegalArgumentException(
                    "El DTO debe tener un ID válido para generar links HATEOAS");
        }
        return (I) id;
    }
}
