package com.jnzader.apigen.core.infrastructure.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jnzader.apigen.core.config.TestSecurityConfig;
import com.jnzader.apigen.core.infrastructure.config.JpaConfig;
import com.jnzader.apigen.core.fixtures.TestEntity;
import com.jnzader.apigen.core.fixtures.TestEntityDTO;
import com.jnzader.apigen.core.fixtures.TestEntityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for BaseControllerImpl WITHOUT HATEOAS.
 * Tests sparse fieldsets, content-based responses, and non-HATEOAS behavior.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestSecurityConfig.class, JpaConfig.class})
@ActiveProfiles("test")
@Transactional
@DisplayName("NoHateoas Controller Integration Tests")
class NoHateoasControllerIntegrationTest {

    private static final String BASE_URL = "/test-entities-no-hateoas";

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private TestEntityRepository testEntityRepository;

    private TestEntity savedEntity;

    @BeforeEach
    void setUp() {
        testEntityRepository.deleteAll();
        TestEntity entity = new TestEntity();
        entity.setName("Test Entity NoHateoas");
        entity.setEstado(true);
        savedEntity = testEntityRepository.save(entity);
    }

    @Nested
    @DisplayName("GET /test-entities-no-hateoas (without HATEOAS)")
    class FindAllNoHateoasTests {

        @Test
        @DisplayName("should return content array instead of _embedded")
        void shouldReturnContentArrayInsteadOfEmbedded() throws Exception {
            MvcResult result = mockMvc.perform(get(BASE_URL)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();
            // Without HATEOAS, response uses "content" not "_embedded"
            assertThat(responseBody)
                    .contains("\"content\"")
                    .doesNotContain("\"_embedded\"")
                    .doesNotContain("\"_links\"");
        }

        @Test
        @DisplayName("should return paginated list with content array")
        void shouldReturnPaginatedListWithContentArray() throws Exception {
            mockMvc.perform(get(BASE_URL)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))))
                    .andExpect(header().exists("X-Total-Count"));
        }

        @Test
        @DisplayName("should support sparse fieldsets on collection")
        void shouldSupportSparseFieldsetsOnCollection() throws Exception {
            MvcResult result = mockMvc.perform(get(BASE_URL)
                            .param("fields", "id,name")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();
            // Should contain requested fields
            assertThat(responseBody)
                    .contains("\"id\"")
                    .contains("\"name\"");
        }
    }

    @Nested
    @DisplayName("GET /test-entities-no-hateoas/{id} (without HATEOAS)")
    class FindByIdNoHateoasTests {

        @Test
        @DisplayName("should return entity without HATEOAS links")
        void shouldReturnEntityWithoutHateoasLinks() throws Exception {
            MvcResult result = mockMvc.perform(get(BASE_URL + "/{id}", savedEntity.getId())
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();
            // Without HATEOAS, no _links in response
            assertThat(responseBody)
                    .contains("\"name\":\"Test Entity NoHateoas\"")
                    .doesNotContain("\"_links\"");
        }

        @Test
        @DisplayName("should support sparse fieldsets on single resource")
        void shouldSupportSparseFieldsetsOnSingleResource() throws Exception {
            MvcResult result = mockMvc.perform(get(BASE_URL + "/{id}", savedEntity.getId())
                            .param("fields", "id,name")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();
            // Should contain only requested fields plus id
            assertThat(responseBody)
                    .contains("\"id\"")
                    .contains("\"name\"");
        }

        @Test
        @DisplayName("should filter fields correctly with sparse fieldsets")
        void shouldFilterFieldsCorrectlyWithSparseFieldsets() throws Exception {
            MvcResult result = mockMvc.perform(get(BASE_URL + "/{id}", savedEntity.getId())
                            .param("fields", "name")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();
            // Should always include id and the requested field
            assertThat(responseBody)
                    .contains("\"id\"")
                    .contains("\"name\"");
        }

        @Test
        @DisplayName("should return 404 for non-existent ID")
        void shouldReturn404ForNonExistentId() throws Exception {
            mockMvc.perform(get(BASE_URL + "/{id}", 99999L)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /test-entities-no-hateoas (without HATEOAS)")
    class CreateNoHateoasTests {

        @Test
        @DisplayName("should create entity without HATEOAS links in response")
        void shouldCreateEntityWithoutHateoasLinks() throws Exception {
            TestEntityDTO newDto = TestEntityDTO.of(null, true, "New NoHateoas Entity");

            MvcResult result = mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(newDto)))
                    .andExpect(status().isCreated())
                    .andExpect(header().exists("Location"))
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();
            assertThat(responseBody)
                    .contains("\"name\":\"New NoHateoas Entity\"")
                    .doesNotContain("\"_links\"");
        }
    }

    @Nested
    @DisplayName("PUT /test-entities-no-hateoas/{id} (without HATEOAS)")
    class UpdateNoHateoasTests {

        @Test
        @DisplayName("should update entity without HATEOAS links in response")
        void shouldUpdateEntityWithoutHateoasLinks() throws Exception {
            TestEntityDTO updateDto = TestEntityDTO.of(savedEntity.getId(), true, "Updated NoHateoas");

            MvcResult result = mockMvc.perform(put(BASE_URL + "/{id}", savedEntity.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateDto)))
                    .andExpect(status().isOk())
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();
            assertThat(responseBody)
                    .contains("\"name\":\"Updated NoHateoas\"")
                    .doesNotContain("\"_links\"");
        }
    }

    @Nested
    @DisplayName("PATCH /test-entities-no-hateoas/{id} (without HATEOAS)")
    class PartialUpdateNoHateoasTests {

        @Test
        @DisplayName("should partially update entity without HATEOAS links")
        void shouldPartiallyUpdateEntityWithoutHateoasLinks() throws Exception {
            TestEntityDTO patchDto = TestEntityDTO.of(null, null, "Patched NoHateoas");

            MvcResult result = mockMvc.perform(patch(BASE_URL + "/{id}", savedEntity.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(patchDto)))
                    .andExpect(status().isOk())
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();
            assertThat(responseBody)
                    .contains("\"name\":\"Patched NoHateoas\"")
                    .doesNotContain("\"_links\"");
        }
    }

    @Nested
    @DisplayName("DELETE /test-entities-no-hateoas/{id}")
    class DeleteNoHateoasTests {

        @Test
        @DisplayName("should soft delete entity")
        void shouldSoftDeleteEntity() throws Exception {
            mockMvc.perform(delete(BASE_URL + "/{id}", savedEntity.getId()))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("should hard delete entity with permanent=true")
        void shouldHardDeleteEntityWhenPermanent() throws Exception {
            mockMvc.perform(delete(BASE_URL + "/{id}", savedEntity.getId())
                            .param("permanent", "true"))
                    .andExpect(status().isNoContent());
        }
    }

    @Nested
    @DisplayName("POST /test-entities-no-hateoas/{id}/restore")
    class RestoreNoHateoasTests {

        @Test
        @DisplayName("should restore soft-deleted entity without HATEOAS links")
        void shouldRestoreSoftDeletedEntityWithoutHateoasLinks() throws Exception {
            // First soft delete
            mockMvc.perform(delete(BASE_URL + "/{id}", savedEntity.getId()))
                    .andExpect(status().isNoContent());

            // Then restore
            MvcResult result = mockMvc.perform(post(BASE_URL + "/{id}/restore", savedEntity.getId()))
                    .andExpect(status().isOk())
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();
            assertThat(responseBody)
                    .contains("\"activo\":true")
                    .doesNotContain("\"_links\"");
        }
    }

    @Nested
    @DisplayName("HEAD /test-entities-no-hateoas")
    class CountNoHateoasTests {

        @Test
        @DisplayName("should return count in header")
        void shouldReturnCountInHeader() throws Exception {
            mockMvc.perform(head(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(header().exists("X-Total-Count"));
        }
    }

    @Nested
    @DisplayName("HEAD /test-entities-no-hateoas/{id}")
    class ExistsByIdNoHateoasTests {

        @Test
        @DisplayName("should return 200 when entity exists")
        void shouldReturn200WhenEntityExists() throws Exception {
            mockMvc.perform(head(BASE_URL + "/{id}", savedEntity.getId()))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("should return 404 when entity does not exist")
        void shouldReturn404WhenEntityDoesNotExist() throws Exception {
            mockMvc.perform(head(BASE_URL + "/{id}", 99999L))
                    .andExpect(status().isNotFound());
        }
    }
}
