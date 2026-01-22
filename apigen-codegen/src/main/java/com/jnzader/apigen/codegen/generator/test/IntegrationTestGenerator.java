package com.jnzader.apigen.codegen.generator.test;

import static com.jnzader.apigen.codegen.generator.util.TestValueProvider.getSampleTestValue;

import com.jnzader.apigen.codegen.model.SqlColumn;
import com.jnzader.apigen.codegen.model.SqlTable;

/** Generates integration test classes for full stack testing. */
public class IntegrationTestGenerator {

    private final String basePackage;

    public IntegrationTestGenerator(String basePackage) {
        this.basePackage = basePackage;
    }

    /** Generates the Integration test class code. */
    public String generate(SqlTable table) {
        String entityName = table.getEntityName();
        String moduleName = table.getModuleName();

        // Generate sample field assignments for creating test data
        StringBuilder fieldAssignments = new StringBuilder();
        StringBuilder updateAssignments = new StringBuilder();
        StringBuilder fieldAssertions = new StringBuilder();

        for (SqlColumn col : table.getBusinessColumns()) {
            String fieldName = col.getJavaFieldName();
            String capitalField =
                    Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
            String sampleValue = getSampleTestValue(col);
            String updatedValue = getSampleTestValue(col, "Updated");

            fieldAssignments
                    .append("\n                .")
                    .append(fieldName)
                    .append("(")
                    .append(sampleValue)
                    .append(")");
            updateAssignments
                    .append("\n        dto.set")
                    .append(capitalField)
                    .append("(")
                    .append(updatedValue)
                    .append(");");

            if ("String".equals(col.getJavaType())) {
                fieldAssertions
                        .append("\n                .andExpect(jsonPath(\"$.")
                        .append(fieldName)
                        .append("\").isNotEmpty())");
            }
        }

        return
"""
package %s.%s;

import %s.%s.application.dto.%sDTO;
import %s.%s.domain.entity.%s;
import %s.%s.infrastructure.repository.%sRepository;
import tools.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for %s entity.
 * Tests the full stack: Controller -> Service -> Repository -> Database (H2)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("%s Integration Tests")
class %sIntegrationTest {

    private static final String BASE_URL = "/api/v1/%s";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private %sRepository repository;

    private %sDTO testDto;
    private static Long createdId;

    @BeforeEach
    void setUp() {
        testDto = %sDTO.builder()
                .activo(true)%s
                .build();
    }

    @Test
    @org.junit.jupiter.api.Order(1)
    @DisplayName("1. POST - Should create new %s")
    void shouldCreateNew%s() throws Exception {
        MvcResult result = mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(testDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.activo").value(true))%s
                .andReturn();

        // Extract created ID for subsequent tests
        String response = result.getResponse().getContentAsString();
        %sDTO created = jsonMapper.readValue(response, %sDTO.class);
        createdId = created.getId();

        assertThat(createdId).isNotNull();
    }

    @Test
    @org.junit.jupiter.api.Order(2)
    @DisplayName("2. GET /{id} - Should find %s by ID")
    void shouldFind%sById() throws Exception {
        assertThat(createdId).isNotNull();

        mockMvc.perform(get(BASE_URL + "/" + createdId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(createdId))
                .andExpect(jsonPath("$.activo").value(true));
    }

    @Test
    @org.junit.jupiter.api.Order(3)
    @DisplayName("3. GET - Should list all %s with pagination")
    void shouldListAllWithPagination() throws Exception {
        mockMvc.perform(get(BASE_URL)
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "id,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").isNumber())
                .andExpect(jsonPath("$.pageable").exists());
    }

    @Test
    @org.junit.jupiter.api.Order(4)
    @DisplayName("4. HEAD /{id} - Should check %s exists")
    void shouldCheckExists() throws Exception {
        assertThat(createdId).isNotNull();

        mockMvc.perform(head(BASE_URL + "/" + createdId))
                .andExpect(status().isOk());
    }

    @Test
    @org.junit.jupiter.api.Order(5)
    @DisplayName("5. HEAD - Should return count in header")
    void shouldReturnCount() throws Exception {
        mockMvc.perform(head(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Total-Count"));
    }

    @Test
    @org.junit.jupiter.api.Order(6)
    @DisplayName("6. PUT /{id} - Should update %s")
    void shouldUpdate%s() throws Exception {
        assertThat(createdId).isNotNull();

        // Get current entity and modify
        MvcResult getResult = mockMvc.perform(get(BASE_URL + "/" + createdId))
                .andReturn();

        %sDTO dto = jsonMapper.readValue(
                getResult.getResponse().getContentAsString(), %sDTO.class);
%s
        mockMvc.perform(put(BASE_URL + "/" + createdId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(createdId));
    }

    @Test
    @org.junit.jupiter.api.Order(7)
    @DisplayName("7. PATCH /{id} - Should partial update %s")
    void shouldPartialUpdate%s() throws Exception {
        assertThat(createdId).isNotNull();

        // Only update activo field
        String patchJson = "{\\\"activo\\\": true}";

        mockMvc.perform(patch(BASE_URL + "/" + createdId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(patchJson))
                .andExpect(status().isOk());
    }

    @Test
    @org.junit.jupiter.api.Order(8)
    @DisplayName("8. DELETE /{id} - Should soft delete %s")
    void shouldSoftDelete%s() throws Exception {
        assertThat(createdId).isNotNull();

        mockMvc.perform(delete(BASE_URL + "/" + createdId))
                .andExpect(status().isNoContent());

        // Verify entity is soft deleted (not visible in normal queries)
        mockMvc.perform(get(BASE_URL + "/" + createdId))
                .andExpect(status().isNotFound());
    }

    @Test
    @org.junit.jupiter.api.Order(9)
    @DisplayName("9. POST /{id}/restore - Should restore soft-deleted %s")
    void shouldRestore%s() throws Exception {
        assertThat(createdId).isNotNull();

        mockMvc.perform(post(BASE_URL + "/" + createdId + "/restore"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(createdId))
                .andExpect(jsonPath("$.activo").value(true));

        // Verify entity is visible again
        mockMvc.perform(get(BASE_URL + "/" + createdId))
                .andExpect(status().isOk());
    }

    @Test
    @org.junit.jupiter.api.Order(10)
    @DisplayName("10. DELETE /{id}?permanent=true - Should permanently delete %s")
    void shouldPermanentlyDelete%s() throws Exception {
        assertThat(createdId).isNotNull();

        mockMvc.perform(delete(BASE_URL + "/" + createdId)
                        .param("permanent", "true"))
                .andExpect(status().isNoContent());

        // Verify entity is completely gone
        mockMvc.perform(get(BASE_URL + "/" + createdId))
                .andExpect(status().isNotFound());

        // Verify not in database
        assertThat(repository.findById(createdId)).isEmpty();
    }

    @Test
    @org.junit.jupiter.api.Order(11)
    @DisplayName("11. GET /{id} - Should return 404 for non-existent %s")
    void shouldReturn404ForNonExistent() throws Exception {
        mockMvc.perform(get(BASE_URL + "/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @org.junit.jupiter.api.Order(12)
    @DisplayName("12. POST - Should validate required fields")
    void shouldValidateRequiredFields() throws Exception {
        %sDTO invalidDto = %sDTO.builder().build();

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @org.junit.jupiter.api.Order(13)
    @DisplayName("13. GET /cursor - Should support cursor pagination")
    void shouldSupportCursorPagination() throws Exception {
        // First, create a few entities for pagination
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(testDto)))
                    .andExpect(status().isCreated());
        }

        // Test cursor pagination
        mockMvc.perform(get(BASE_URL + "/cursor")
                        .param("size", "2")
                        .param("sort", "id")
                        .param("direction", "DESC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.pageInfo.hasNext").isBoolean())
                .andExpect(jsonPath("$.pageInfo.hasPrevious").isBoolean());
    }

    @Test
    @org.junit.jupiter.api.Order(14)
    @DisplayName("14. GET with filter - Should filter results")
    void shouldFilterResults() throws Exception {
        mockMvc.perform(get(BASE_URL)
                        .param("filter", "id:gt:0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
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
                        entityName,
                        entityName,
                        entityName,
                        moduleName,
                        entityName,
                        entityName,
                        entityName,
                        fieldAssignments.toString(),
                        // Test 1 - Create
                        entityName,
                        entityName,
                        fieldAssertions.toString(),
                        entityName,
                        entityName,
                        // Test 2 - Find by ID
                        entityName,
                        entityName,
                        // Test 3 - List
                        entityName,
                        // Test 4 - Exists
                        entityName,
                        // Test 6 - Update
                        entityName,
                        entityName,
                        entityName,
                        entityName,
                        updateAssignments.toString(),
                        // Test 7 - Partial Update
                        entityName,
                        entityName,
                        // Test 8 - Soft Delete
                        entityName,
                        entityName,
                        // Test 9 - Restore
                        entityName,
                        entityName,
                        // Test 10 - Hard Delete
                        entityName,
                        entityName,
                        // Test 11 - 404
                        entityName,
                        // Test 12 - Validation
                        entityName,
                        entityName);
    }
}
