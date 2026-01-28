package com.jnzader.apigen.codegen.generator.kotlin.test;

import static com.jnzader.apigen.codegen.generator.util.TestValueProvider.getSampleTestValue;

import com.jnzader.apigen.codegen.model.SqlColumn;
import com.jnzader.apigen.codegen.model.SqlTable;

/** Generates unit test classes for Mapper implementations in Kotlin/Spring Boot. */
@SuppressWarnings("java:S1192") // Duplicate strings intentional for code generation templates
public class KotlinMapperTestGenerator {

    private final String basePackage;

    public KotlinMapperTestGenerator(String basePackage) {
        this.basePackage = basePackage;
    }

    /** Generates the Mapper test class code in Kotlin. */
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
                    .append(".")
                    .append(fieldName)
                    .append(" = ")
                    .append(sampleValue);

            dtoAssertions
                    .append("\n            assertThat(dto.")
                    .append(fieldName)
                    .append(").isEqualTo(")
                    .append(sampleValue)
                    .append(")");

            dtoSetters
                    .append("\n        inputDto.")
                    .append(fieldName)
                    .append(" = ")
                    .append(sampleValue);

            entityAssertions
                    .append("\n            assertThat(result.")
                    .append(fieldName)
                    .append(").isEqualTo(")
                    .append(sampleValue)
                    .append(")");
        }

        return
"""
package %s.%s.application.mapper

import %s.%s.application.dto.%sDTO
import %s.%s.domain.entity.%s
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mapstruct.factory.Mappers

@DisplayName("%sMapper Tests")
class %sMapperTest {

    private lateinit var mapper: %sMapper

    private lateinit var %s: %s
    private lateinit var inputDto: %sDTO

    @BeforeEach
    fun setUp() {
        mapper = Mappers.getMapper(%sMapper::class.java)

        %s = %s()
        %s.id = 1L
        %s.estado = true%s

        inputDto = %sDTO()
        inputDto.id = 1L
        inputDto.activo = true%s
    }

    @Nested
    @DisplayName("Entity to DTO Mapping")
    inner class EntityToDtoMapping {

        @Test
        @DisplayName("Should map entity to DTO")
        fun shouldMapEntityToDto() {
            val dto = mapper.toDTO(%s)

            assertThat(dto).isNotNull
            assertThat(dto.id).isEqualTo(1L)
            assertThat(dto.activo).isTrue()%s
        }

        @Test
        @DisplayName("Should map null entity to null DTO")
        fun shouldMapNullEntityToNullDto() {
            val dto = mapper.toDTO(null)

            assertThat(dto).isNull()
        }

        @Test
        @DisplayName("Should map estado to activo correctly")
        fun shouldMapEstadoToActivo() {
            %s.estado = false

            val dto = mapper.toDTO(%s)

            assertThat(dto.activo).isFalse()
        }
    }

    @Nested
    @DisplayName("DTO to Entity Mapping")
    inner class DtoToEntityMapping {

        @Test
        @DisplayName("Should map DTO to entity")
        fun shouldMapDtoToEntity() {
            val result = mapper.toEntity(inputDto)

            assertThat(result).isNotNull
            assertThat(result.id).isEqualTo(1L)
            assertThat(result.estado).isTrue()%s
        }

        @Test
        @DisplayName("Should map null DTO to null entity")
        fun shouldMapNullDtoToNullEntity() {
            val result = mapper.toEntity(null)

            assertThat(result).isNull()
        }

        @Test
        @DisplayName("Should map activo to estado correctly")
        fun shouldMapActivoToEstado() {
            inputDto.activo = false

            val result = mapper.toEntity(inputDto)

            assertThat(result.estado).isFalse()
        }
    }

    @Nested
    @DisplayName("List Mapping")
    inner class ListMapping {

        @Test
        @DisplayName("Should map entity list to DTO list")
        fun shouldMapEntityListToDtoList() {
            val entities = listOf(%s)

            val dtos = mapper.toDTOList(entities)

            assertThat(dtos).hasSize(1)
            assertThat(dtos[0].id).isEqualTo(1L)
        }

        @Test
        @DisplayName("Should map null entity list to null DTO list")
        fun shouldMapNullEntityListToNullDtoList() {
            val dtos = mapper.toDTOList(null)

            assertThat(dtos).isNull()
        }

        @Test
        @DisplayName("Should map empty entity list to empty DTO list")
        fun shouldMapEmptyEntityListToEmptyDtoList() {
            val dtos = mapper.toDTOList(emptyList())

            assertThat(dtos).isEmpty()
        }

        @Test
        @DisplayName("Should map DTO list to entity list")
        fun shouldMapDtoListToEntityList() {
            val dtos = listOf(inputDto)

            val entities = mapper.toEntityList(dtos)

            assertThat(entities).hasSize(1)
            assertThat(entities[0].id).isEqualTo(1L)
        }

        @Test
        @DisplayName("Should map null DTO list to null entity list")
        fun shouldMapNullDtoListToNullEntityList() {
            val entities = mapper.toEntityList(null)

            assertThat(entities).isNull()
        }

        @Test
        @DisplayName("Should map empty DTO list to empty entity list")
        fun shouldMapEmptyDtoListToEmptyEntityList() {
            val entities = mapper.toEntityList(emptyList())

            assertThat(entities).isEmpty()
        }
    }

    @Nested
    @DisplayName("Update Entity From DTO")
    inner class UpdateEntityFromDto {

        @Test
        @DisplayName("Should update entity from DTO")
        fun shouldUpdateEntityFromDto() {
            val updateDto = %sDTO()
            updateDto.activo = false%s

            mapper.updateEntityFromDTO(updateDto, %s)

            assertThat(%s.estado).isFalse()
        }

        @Test
        @DisplayName("Should not fail when updating with null DTO")
        fun shouldNotFailWhenUpdatingWithNullDto() {
            mapper.updateEntityFromDTO(null, %s)

            // Entity should remain unchanged
            assertThat(%s.id).isEqualTo(1L)
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
                        entityVarName,
                        entityName,
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
                        entityVarName,
                        dtoAssertions.toString(),
                        entityVarName,
                        entityVarName,
                        // DTO to Entity Mapping
                        entityAssertions.toString(),
                        // List Mapping
                        entityVarName,
                        // Update Entity From DTO
                        entityName,
                        dtoSetters.toString(),
                        entityVarName,
                        entityVarName,
                        entityVarName,
                        entityVarName);
    }
}
