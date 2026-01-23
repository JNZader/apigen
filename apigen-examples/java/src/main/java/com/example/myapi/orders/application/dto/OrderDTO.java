package com.example.myapi.orders.application.dto;

import com.jnzader.apigen.core.application.dto.BaseDTO;
import com.jnzader.apigen.core.application.validation.ValidationGroups;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDTO implements BaseDTO {

    @Null(groups = ValidationGroups.Create.class, message = "ID debe ser nulo al crear")
    @NotNull(groups = ValidationGroups.Update.class, message = "ID es requerido al actualizar")
    private Long id;

    @Builder.Default
    private Boolean activo = true;

    @NotBlank // @Unique
    private String orderNumber;
    @NotNull
    private BigDecimal total;
    @NotBlank
    private String status;
    @NotBlank
    private String shippingAddress;
    @NotNull
    private LocalDateTime orderDate;
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
