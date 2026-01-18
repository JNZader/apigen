package com.jnzader.example.infrastructure.hateoas;

import com.jnzader.apigen.core.infrastructure.hateoas.BaseResourceAssembler;
import com.jnzader.example.application.dto.ProductDTO;
import com.jnzader.example.infrastructure.controller.ProductController;
import org.springframework.hateoas.EntityModel;
import org.springframework.stereotype.Component;

/**
 * HATEOAS resource assembler for Product.
 * <p>
 * Extends {@link BaseResourceAssembler} which provides:
 * <ul>
 *     <li>toModel(dto) - Convert DTO to EntityModel with HATEOAS links</li>
 *     <li>toCollectionModel(dtos) - Convert collection with links</li>
 *     <li>toPagedModel(page) - Convert paginated results with navigation links</li>
 * </ul>
 * <p>
 * Generated links:
 * <ul>
 *     <li>self - Link to this resource</li>
 *     <li>collection - Link to product list</li>
 *     <li>update - Link to update endpoint</li>
 *     <li>delete - Link to delete endpoint</li>
 * </ul>
 */
@Component
public class ProductResourceAssembler extends BaseResourceAssembler<ProductDTO, Long> {

    public ProductResourceAssembler() {
        super(ProductController.class);
    }

    @Override
    protected void addCustomLinks(EntityModel<ProductDTO> model, ProductDTO dto) {
        // Override this method to add custom HATEOAS links specific to Product
        // For example, links to related resources like category or reviews
    }
}
