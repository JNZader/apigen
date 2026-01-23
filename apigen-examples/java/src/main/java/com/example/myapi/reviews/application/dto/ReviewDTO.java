package com.example.myapi.reviews.application.dto;

import com.jnzader.apigen.core.application.dto.BaseDTO;
import com.jnzader.apigen.core.application.validation.ValidationGroups;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewDTO implements BaseDTO {

    @Null(groups = ValidationGroups.Create.class, message = "ID debe ser nulo al crear")
    @NotNull(groups = ValidationGroups.Update.class, message = "ID es requerido al actualizar")
    private Long id;

    @Builder.Default
    private Boolean activo = true;

    @NotNull
    private Integer rating;
    private String comment;
    @NotNull
    private LocalDateTime reviewDate;
    private Long productId;
    private Long customerId;

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
