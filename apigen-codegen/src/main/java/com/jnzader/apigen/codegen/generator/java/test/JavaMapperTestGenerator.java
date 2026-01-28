package com.jnzader.apigen.codegen.generator.java.test;

import static com.jnzader.apigen.codegen.generator.util.TestValueProvider.getSampleTestValue;

import com.jnzader.apigen.codegen.model.SqlColumn;
import com.jnzader.apigen.codegen.model.SqlTable;

/** Generates unit test classes for Mapper implementations in Java/Spring Boot. */
@SuppressWarnings("java:S1192") // Duplicate strings intentional for code generation templates
public class JavaMapperTestGenerator {

    private final String basePackage;

    public JavaMapperTestGenerator(String basePackage) {
        this.basePackage = basePackage;
    }

    /** Generates the Mapper test class code. */
    public String generate(SqlTable table) {
        String entityName = table.getEntityName();
        String entityVarName = table.getEntityVariableName();
        String moduleName = table.getModuleName();

        // Generate field assertions for toDTO
        StringBuilder entitySetters = new StringBuilder();
        StringBuilder dtoAssertions = new StringBuilder();
        StringBuilder dtoSetters = new StringBuilder();
        StringBuilder entityAssertions = new StringBuilder();

        for (SqlColumn col : table.getBusinessColumns()) {
            String fieldName = col.getJavaFieldName();
            String capitalField =
                    Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
            String sampleValue = getSampleTestValue(col);

            entitySetters
                    .append("\n        ")
                    .append(entityVarName)
                    .append(".set")
                    .append(capitalField)
                    .append("(")
                    .append(sampleValue)
                    .append(");");

            dtoAssertions
                    .append("\n            assertThat(dto.get")
                    .append(capitalField)
                    .append("()).isEqualTo(")
                    .append(sampleValue)
                    .append(");");

            dtoSetters
                    .append("\n        inputDto.set")
                    .append(capitalField)
                    .append("(")
                    .append(sampleValue)
                    .append(");");

            entityAssertions
                    .append("\n            assertThat(result.get")
                    .append(capitalField)
                    .append("()).isEqualTo(")
                    .append(sampleValue)
                    .append(");");
        }

        return
"""
package %s.%s.application.mapper;

import %s.%s.application.dto.%sDTO;
import %s.%s.domain.entity.%s;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("%sMapper Tests")
class %sMapperTest {

    private %sMapper mapper;

    private %s %s;
    private %sDTO inputDto;

    @BeforeEach
    void setUp() {
        mapper = Mappers.getMapper(%sMapper.class);

        %s = new %s();
        %s.setId(1L);
        %s.setEstado(true);%s

        inputDto = new %sDTO();
        inputDto.setId(1L);
        inputDto.setActivo(true);%s
    }

    @Nested
    @DisplayName("Entity to DTO Mapping")
    class EntityToDtoMapping {

        @Test
        @DisplayName("Should map entity to DTO")
        void shouldMapEntityToDto() {
            %sDTO dto = mapper.toDTO(%s);

            assertThat(dto).isNotNull();
            assertThat(dto.getId()).isEqualTo(1L);
            assertThat(dto.getActivo()).isTrue();%s
        }

        @Test
        @DisplayName("Should map null entity to null DTO")
        void shouldMapNullEntityToNullDto() {
            %sDTO dto = mapper.toDTO(null);

            assertThat(dto).isNull();
        }

        @Test
        @DisplayName("Should map estado to activo correctly")
        void shouldMapEstadoToActivo() {
            %s.setEstado(false);

            %sDTO dto = mapper.toDTO(%s);

            assertThat(dto.getActivo()).isFalse();
        }
    }

    @Nested
    @DisplayName("DTO to Entity Mapping")
    class DtoToEntityMapping {

        @Test
        @DisplayName("Should map DTO to entity")
        void shouldMapDtoToEntity() {
            %s result = mapper.toEntity(inputDto);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getEstado()).isTrue();%s
        }

        @Test
        @DisplayName("Should map null DTO to null entity")
        void shouldMapNullDtoToNullEntity() {
            %s result = mapper.toEntity(null);

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Should map activo to estado correctly")
        void shouldMapActivoToEstado() {
            inputDto.setActivo(false);

            %s result = mapper.toEntity(inputDto);

            assertThat(result.getEstado()).isFalse();
        }
    }

    @Nested
    @DisplayName("List Mapping")
    class ListMapping {

        @Test
        @DisplayName("Should map entity list to DTO list")
        void shouldMapEntityListToDtoList() {
            List<%s> entities = List.of(%s);

            List<%sDTO> dtos = mapper.toDTOList(entities);

            assertThat(dtos).hasSize(1);
            assertThat(dtos.get(0).getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Should map null entity list to null DTO list")
        void shouldMapNullEntityListToNullDtoList() {
            List<%sDTO> dtos = mapper.toDTOList(null);

            assertThat(dtos).isNull();
        }

        @Test
        @DisplayName("Should map empty entity list to empty DTO list")
        void shouldMapEmptyEntityListToEmptyDtoList() {
            List<%sDTO> dtos = mapper.toDTOList(List.of());

            assertThat(dtos).isEmpty();
        }

        @Test
        @DisplayName("Should map DTO list to entity list")
        void shouldMapDtoListToEntityList() {
            List<%sDTO> dtos = List.of(inputDto);

            List<%s> entities = mapper.toEntityList(dtos);

            assertThat(entities).hasSize(1);
            assertThat(entities.get(0).getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Should map null DTO list to null entity list")
        void shouldMapNullDtoListToNullEntityList() {
            List<%s> entities = mapper.toEntityList(null);

            assertThat(entities).isNull();
        }

        @Test
        @DisplayName("Should map empty DTO list to empty entity list")
        void shouldMapEmptyDtoListToEmptyEntityList() {
            List<%s> entities = mapper.toEntityList(List.of());

            assertThat(entities).isEmpty();
        }
    }

    @Nested
    @DisplayName("Update Entity From DTO")
    class UpdateEntityFromDto {

        @Test
        @DisplayName("Should update entity from DTO")
        void shouldUpdateEntityFromDto() {
            %sDTO updateDto = new %sDTO();
            updateDto.setActivo(false);%s

            mapper.updateEntityFromDTO(updateDto, %s);

            assertThat(%s.getEstado()).isFalse();
        }

        @Test
        @DisplayName("Should not fail when updating with null DTO")
        void shouldNotFailWhenUpdatingWithNullDto() {
            mapper.updateEntityFromDTO(null, %s);

            // Entity should remain unchanged
            assertThat(%s.getId()).isEqualTo(1L);
        }
    }
}
"""
                .formatted(
                        basePackage,
                        moduleName,
                        basePackage,
                        moduleName,
                        entityName,
                        basePackage,
                        moduleName,
                        entityName,
                        entityName,
                        entityName,
                        entityName,
                        entityName,
                        entityVarName,
                        entityName,
                        entityName,
                        entityVarName,
                        entityName,
                        entityVarName,
                        entityVarName,
                        entitySetters.toString(),
                        entityName,
                        dtoSetters.toString(),
                        // Entity to DTO Mapping
                        entityName,
                        entityVarName,
                        dtoAssertions.toString(),
                        entityName,
                        entityVarName,
                        entityName,
                        entityVarName,
                        // DTO to Entity Mapping
                        entityName,
                        entityAssertions.toString(),
                        entityName,
                        entityName,
                        // List Mapping
                        entityName,
                        entityVarName,
                        entityName,
                        entityName,
                        entityName,
                        entityName,
                        entityName,
                        entityName,
                        entityName,
                        // Update Entity From DTO
                        entityName,
                        entityName,
                        dtoSetters.toString(),
                        entityVarName,
                        entityVarName,
                        entityVarName,
                        entityVarName);
    }
}
