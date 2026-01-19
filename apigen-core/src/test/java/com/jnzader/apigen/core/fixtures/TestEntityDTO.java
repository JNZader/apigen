package com.jnzader.apigen.core.fixtures;

import com.jnzader.apigen.core.application.dto.BaseDTO;
import jakarta.validation.constraints.NotBlank;

/** Test DTO for unit tests. */
public record TestEntityDTO(
        Long id,
        Boolean activo,
        @NotBlank(message = "Name is required") String name,
        String description,
        Integer value)
        implements BaseDTO {

    /** Convenience factory for tests that only need id, activo, and name. */
    public static TestEntityDTO of(Long id, Boolean activo, String name) {
        return new TestEntityDTO(id, activo, name, null, null);
    }

    /** Convenience factory for tests that only need name. */
    public static TestEntityDTO ofName(String name) {
        return new TestEntityDTO(null, true, name, null, null);
    }
}
