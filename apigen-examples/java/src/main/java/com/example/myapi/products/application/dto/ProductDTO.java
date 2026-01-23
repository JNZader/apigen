package com.example.myapi.products.application.dto;

import com.jnzader.apigen.core.application.dto.BaseDTO;
import com.jnzader.apigen.core.application.validation.ValidationGroups;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDTO implements BaseDTO {

    @Null(groups = ValidationGroups.Create.class, message = "ID debe ser nulo al crear")
    @NotNull(groups = ValidationGroups.Update.class, message = "ID es requerido al actualizar")
    private Long id;

    @Builder.Default
    private Boolean activo = true;

    @NotBlank
    private String name;
    private String description;
    @NotNull
    private BigDecimal price;
    @NotNull
    private Integer stock;
    @NotBlank // @Unique
    private String sku;
    private String imageUrl;
    private Long categoryId;

    // BaseDTO interface methods
    @Override
    public Long id() {
        return this.id;
    }

    @Override
    public Boolean activo() {
        return this.activo;
    }
}
