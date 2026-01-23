package com.example.myapi.categories.application.dto;

import com.jnzader.apigen.core.application.dto.BaseDTO;
import com.jnzader.apigen.core.application.validation.ValidationGroups;
import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryDTO implements BaseDTO {

    @Null(groups = ValidationGroups.Create.class, message = "ID debe ser nulo al crear")
    @NotNull(groups = ValidationGroups.Update.class, message = "ID es requerido al actualizar")
    private Long id;

    @Builder.Default
    private Boolean activo = true;

    @NotBlank // @Unique
    private String name;
    private String description;
    @NotBlank // @Unique
    private String slug;

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
