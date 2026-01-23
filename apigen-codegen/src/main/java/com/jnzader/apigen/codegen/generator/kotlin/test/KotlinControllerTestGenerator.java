package com.jnzader.apigen.codegen.generator.kotlin.test;

import com.jnzader.apigen.codegen.model.SqlTable;

/** Generates controller test classes for Kotlin/Spring Boot. */
public class KotlinControllerTestGenerator {

    private static final String APIGEN_CORE_PKG = "com.jnzader.apigen.core";

    private final String basePackage;

    public KotlinControllerTestGenerator(String basePackage) {
        this.basePackage = basePackage;
    }

    /** Generates the Controller test class code in Kotlin. */
    public String generate(SqlTable table) {
        String entityName = table.getEntityName();
        String entityVarName = table.getEntityVariableName();
        String moduleName = table.getModuleName();

        return
"""
package %s.%s.infrastructure.controller

import %s.%s.application.dto.%sDTO
import %s.%s.application.mapper.%sMapper
import %s.%s.application.service.%sService
import %s.%s.domain.entity.%s
import %s.application.util.Result
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.MockMvcBuilders

@ExtendWith(MockitoExtension::class)
@DisplayName("%sController Tests")
class %sControllerImplTest {

    @Mock
    private lateinit var service: %sService

    @Mock
    private lateinit var mapper: %sMapper

    private lateinit var mockMvc: MockMvc
    private lateinit var controller: %sControllerImpl
    private lateinit var objectMapper: ObjectMapper

    private lateinit var %s: %s
    private lateinit var %sDTO: %sDTO

    @BeforeEach
    fun setUp() {
        controller = %sControllerImpl(service, mapper)
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build()
        objectMapper = ObjectMapper()

        %s = %s().apply {
            id = 1L
            estado = true
        }
        %sDTO = %sDTO(id = 1L, activo = true)
    }

    @Nested
    @DisplayName("GET Endpoints")
    inner class GetEndpoints {

        @Test
        @DisplayName("Should get %s by ID successfully")
        fun `should get by id successfully`() {
            whenever(service.findById(1L)).thenReturn(Result.success(%s))
            whenever(mapper.toDTO(%s)).thenReturn(%sDTO)

            mockMvc.perform(get("/api/v1/%s/1"))
                .andExpect(status().isOk)

            verify(service).findById(1L)
        }

        @Test
        @DisplayName("Should return 404 when %s not found")
        fun `should return 404 when not found`() {
            whenever(service.findById(999L)).thenReturn(Result.failure(RuntimeException("Not found")))

            mockMvc.perform(get("/api/v1/%s/999"))
                .andExpect(status().isNotFound)
        }

        @Test
        @DisplayName("Should get all %s with pagination")
        fun `should get all with pagination`() {
            val page = PageImpl(listOf(%s))
            whenever(service.findAll(any())).thenReturn(Result.success(page))
            whenever(mapper.toDTO(any<%s>())).thenReturn(%sDTO)

            mockMvc.perform(get("/api/v1/%s")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk)

            verify(service).findAll(any())
        }
    }

    @Nested
    @DisplayName("POST Endpoints")
    inner class PostEndpoints {

        @Test
        @DisplayName("Should create %s successfully")
        fun `should create successfully`() {
            val newDTO = %sDTO(id = null, activo = true)
            whenever(mapper.toEntity(any())).thenReturn(%s)
            whenever(service.save(any())).thenReturn(Result.success(%s))
            whenever(mapper.toDTO(any<%s>())).thenReturn(%sDTO)

            mockMvc.perform(post("/api/v1/%s")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newDTO)))
                .andExpect(status().isCreated)

            verify(service).save(any())
        }
    }

    @Nested
    @DisplayName("PUT Endpoints")
    inner class PutEndpoints {

        @Test
        @DisplayName("Should update %s successfully")
        fun `should update successfully`() {
            whenever(mapper.toEntity(any())).thenReturn(%s)
            whenever(service.update(eq(1L), any())).thenReturn(Result.success(%s))
            whenever(mapper.toDTO(any<%s>())).thenReturn(%sDTO)

            mockMvc.perform(put("/api/v1/%s/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(%sDTO)))
                .andExpect(status().isOk)

            verify(service).update(eq(1L), any())
        }
    }

    @Nested
    @DisplayName("DELETE Endpoints")
    inner class DeleteEndpoints {

        @Test
        @DisplayName("Should soft delete %s successfully")
        fun `should soft delete successfully`() {
            whenever(service.softDelete(1L)).thenReturn(Result.success(null))

            mockMvc.perform(delete("/api/v1/%s/1"))
                .andExpect(status().isNoContent)

            verify(service).softDelete(1L)
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
                        basePackage,
                        moduleName,
                        entityName,
                        basePackage,
                        moduleName,
                        entityName,
                        APIGEN_CORE_PKG,
                        entityName,
                        entityName,
                        entityName,
                        entityName,
                        entityName,
                        entityVarName,
                        entityName,
                        entityVarName,
                        entityName,
                        entityName,
                        entityVarName,
                        entityName,
                        entityVarName,
                        entityName,
                        // GET
                        entityName,
                        entityVarName,
                        entityVarName,
                        entityVarName,
                        moduleName,
                        entityName,
                        moduleName,
                        entityName,
                        entityVarName,
                        entityName,
                        entityVarName,
                        moduleName,
                        // POST
                        entityName,
                        entityName,
                        entityVarName,
                        entityVarName,
                        entityName,
                        entityVarName,
                        moduleName,
                        // PUT
                        entityName,
                        entityVarName,
                        entityVarName,
                        entityName,
                        entityVarName,
                        moduleName,
                        entityVarName,
                        // DELETE
                        entityName,
                        moduleName);
    }
}
