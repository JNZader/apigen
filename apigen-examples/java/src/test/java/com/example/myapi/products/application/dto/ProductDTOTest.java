package com.example.myapi.products.application.dto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ProductDTO Tests")
class ProductDTOTest {

    private ProductDTO dto;

    @BeforeEach
    void setUp() {
        dto = new ProductDTO();
    }

    @Nested
    @DisplayName("BaseDTO Interface Methods")
    class BaseDTOMethods {

        @Test
        @DisplayName("id() should return the id value")
        void idShouldReturnIdValue() {
            dto.setId(1L);

            assertThat(dto.id()).isEqualTo(1L);
        }

        @Test
        @DisplayName("id() should return null when not set")
        void idShouldReturnNullWhenNotSet() {
            assertThat(dto.id()).isNull();
        }

        @Test
        @DisplayName("activo() should return the activo value")
        void activoShouldReturnActivoValue() {
            dto.setActivo(true);

            assertThat(dto.activo()).isTrue();
        }

        @Test
        @DisplayName("activo() should return default true from builder")
        void activoShouldReturnDefaultTrue() {
            ProductDTO builtDto = ProductDTO.builder().build();

            assertThat(builtDto.activo()).isTrue();
        }

        @Test
        @DisplayName("activo() can be set to false")
        void activoCanBeSetToFalse() {
            dto.setActivo(false);

            assertThat(dto.activo()).isFalse();
        }
    }

    @Nested
    @DisplayName("Builder Tests")
    class BuilderTests {

        @Test
        @DisplayName("Should build DTO with all fields")
        void shouldBuildDtoWithAllFields() {
            ProductDTO builtDto = ProductDTO.builder()
                    .id(1L)
                    .activo(true)
                    .build();

            assertThat(builtDto.getId()).isEqualTo(1L);
            assertThat(builtDto.getActivo()).isTrue();
        }

        @Test
        @DisplayName("Should build empty DTO")
        void shouldBuildEmptyDto() {
            ProductDTO builtDto = ProductDTO.builder().build();

            assertThat(builtDto.getId()).isNull();
            assertThat(builtDto.getActivo()).isTrue(); // Default value
        }
    }

    @Nested
    @DisplayName("Equals and HashCode Tests")
    class EqualsHashCodeTests {

        @Test
        @DisplayName("DTOs with same values should be equal")
        void dtosWithSameValuesShouldBeEqual() {
            ProductDTO dto1 = ProductDTO.builder().id(1L).activo(true).build();
            ProductDTO dto2 = ProductDTO.builder().id(1L).activo(true).build();

            assertThat(dto1).isEqualTo(dto2);
            assertThat(dto1.hashCode()).isEqualTo(dto2.hashCode());
        }

        @Test
        @DisplayName("DTOs with different values should not be equal")
        void dtosWithDifferentValuesShouldNotBeEqual() {
            ProductDTO dto1 = ProductDTO.builder().id(1L).build();
            ProductDTO dto2 = ProductDTO.builder().id(2L).build();

            assertThat(dto1).isNotEqualTo(dto2);
        }
    }

    @Nested
    @DisplayName("ToString Tests")
    class ToStringTests {

        @Test
        @DisplayName("toString should contain class name and field values")
        void toStringShouldContainFieldValues() {
            dto.setId(1L);
            dto.setActivo(true);

            String result = dto.toString();

            assertThat(result).contains("ProductDTO");
            assertThat(result).contains("id=1");
            assertThat(result).contains("activo=true");
        }
    }
}
