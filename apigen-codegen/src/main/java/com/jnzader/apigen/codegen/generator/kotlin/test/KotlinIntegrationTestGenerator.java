package com.jnzader.apigen.codegen.generator.kotlin.test;

import com.jnzader.apigen.codegen.model.SqlTable;

/** Generates integration test classes for Kotlin/Spring Boot. */
public class KotlinIntegrationTestGenerator {

    private final String basePackage;

    public KotlinIntegrationTestGenerator(String basePackage) {
        this.basePackage = basePackage;
    }

    /** Generates the Integration test class code in Kotlin. */
    public String generate(SqlTable table) {
        String entityName = table.getEntityName();
        String entityVarName = table.getEntityVariableName();
        String moduleName = table.getModuleName();

        return
"""
package %s.%s

import %s.%s.application.dto.%sDTO
import %s.%s.application.service.%sService
import %s.%s.domain.entity.%s
import %s.%s.infrastructure.repository.%sRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.annotation.Transactional
import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("%s Integration Tests")
class %sIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var repository: %sRepository

    @Autowired
    private lateinit var service: %sService

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private lateinit var saved%s: %s

    @BeforeEach
    fun setUp() {
        repository.deleteAll()

        val %s = %s().apply {
            estado = true
        }
        saved%s = repository.save(%s)
    }

    @Nested
    @DisplayName("GET /api/v1/%s")
    inner class GetEndpoints {

        @Test
        @DisplayName("Should get %s by ID")
        fun `should get by id`() {
            mockMvc.perform(get("/api/v1/%s/{id}", saved%s.id))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.id").value(saved%s.id))
        }

        @Test
        @DisplayName("Should return 404 for non-existent %s")
        fun `should return 404 for non-existent`() {
            mockMvc.perform(get("/api/v1/%s/{id}", 999999L))
                .andExpect(status().isNotFound)
        }

        @Test
        @DisplayName("Should get all %s with pagination")
        fun `should get all with pagination`() {
            mockMvc.perform(get("/api/v1/%s")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.content").isArray)
                .andExpect(jsonPath("$.content.length()").value(1))
        }
    }

    @Nested
    @DisplayName("POST /api/v1/%s")
    inner class PostEndpoints {

        @Test
        @DisplayName("Should create new %s")
        fun `should create new entity`() {
            val dto = %sDTO(id = null, activo = true)

            mockMvc.perform(post("/api/v1/%s")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.id").isNotEmpty)

            assertThat(repository.count()).isEqualTo(2)
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/%s/{id}")
    inner class PutEndpoints {

        @Test
        @DisplayName("Should update existing %s")
        fun `should update existing entity`() {
            val dto = %sDTO(id = saved%s.id, activo = false)

            mockMvc.perform(put("/api/v1/%s/{id}", saved%s.id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk)
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/%s/{id}")
    inner class DeleteEndpoints {

        @Test
        @DisplayName("Should soft delete %s")
        fun `should soft delete entity`() {
            mockMvc.perform(delete("/api/v1/%s/{id}", saved%s.id))
                .andExpect(status().isNoContent)

            val deleted = repository.findById(saved%s.id!!).orElseThrow()
            assertThat(deleted.estado).isFalse()
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
                        entityName,
                        entityName,
                        entityName,
                        entityName,
                        entityName,
                        entityName,
                        entityVarName,
                        entityName,
                        entityName,
                        entityVarName,
                        // GET
                        moduleName,
                        entityName,
                        moduleName,
                        entityName,
                        entityName,
                        entityName,
                        moduleName,
                        entityName,
                        moduleName,
                        // POST
                        moduleName,
                        entityName,
                        entityName,
                        moduleName,
                        // PUT
                        moduleName,
                        entityName,
                        entityName,
                        entityName,
                        moduleName,
                        entityName,
                        // DELETE
                        moduleName,
                        entityName,
                        moduleName,
                        entityName,
                        entityName);
    }
}
