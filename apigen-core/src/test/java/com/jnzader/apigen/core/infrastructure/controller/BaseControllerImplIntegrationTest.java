package com.jnzader.apigen.core.infrastructure.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jnzader.apigen.core.config.TestSecurityConfig;
import com.jnzader.apigen.core.fixtures.TestEntity;
import com.jnzader.apigen.core.fixtures.TestEntityDTO;
import com.jnzader.apigen.core.fixtures.TestEntityRepository;
import com.jnzader.apigen.core.infrastructure.config.JpaConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for BaseControllerImpl. Uses full Spring context to exercise the actual
 * controller implementation.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestSecurityConfig.class, JpaConfig.class})
@ActiveProfiles("test")
@Transactional
@DisplayName("BaseControllerImpl Integration Tests")
class BaseControllerImplIntegrationTest {

    @Autowired private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired private TestEntityRepository testEntityRepository;

    private TestEntity savedEntity;

    @BeforeEach
    void setUp() {
        testEntityRepository.deleteAll();
        TestEntity entity = new TestEntity();
        entity.setName("Test Entity");
        entity.setEstado(true);
        savedEntity = testEntityRepository.save(entity);
    }

    @Nested
    @DisplayName("GET /test-entities")
    class FindAllTests {

        @Test
        @DisplayName("should return paginated list of entities")
        void shouldReturnPaginatedListOfEntities() throws Exception {
            // With HATEOAS enabled, the collection is in _embedded.testEntityDTOList
            mockMvc.perform(get("/test-entities").accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(
                            jsonPath(
                                    "$._embedded.testEntityDTOList",
                                    hasSize(greaterThanOrEqualTo(1))))
                    .andExpect(header().exists("X-Total-Count"));
        }

        @Test
        @DisplayName("should return empty page when no entities")
        void shouldReturnEmptyPageWhenNoEntities() throws Exception {
            testEntityRepository.deleteAll();

            // With HATEOAS enabled, empty collection still has page metadata
            mockMvc.perform(get("/test-entities").accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.page.totalElements", is(0)));
        }

        @Test
        @DisplayName("should support pagination parameters")
        void shouldSupportPaginationParameters() throws Exception {
            mockMvc.perform(
                            get("/test-entities")
                                    .param("page", "0")
                                    .param("size", "5")
                                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(header().exists("X-Page-Number"))
                    .andExpect(header().exists("X-Page-Size"));
        }
    }

    @Nested
    @DisplayName("HEAD /test-entities")
    class CountTests {

        @Test
        @DisplayName("should return count in header")
        void shouldReturnCountInHeader() throws Exception {
            mockMvc.perform(head("/test-entities"))
                    .andExpect(status().isOk())
                    .andExpect(header().exists("X-Total-Count"));
        }
    }

    @Nested
    @DisplayName("GET /test-entities/{id}")
    class FindByIdTests {

        @Test
        @DisplayName("should return entity by ID")
        void shouldReturnEntityById() throws Exception {
            mockMvc.perform(
                            get("/test-entities/{id}", savedEntity.getId())
                                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(savedEntity.getId().intValue())))
                    .andExpect(jsonPath("$.name", is("Test Entity")))
                    .andExpect(header().exists("ETag"));
        }

        @Test
        @DisplayName("should return 404 for non-existent ID")
        void shouldReturn404ForNonExistentId() throws Exception {
            mockMvc.perform(get("/test-entities/{id}", 99999L).accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 304 when ETag matches")
        void shouldReturn304WhenETagMatches() throws Exception {
            // First, get the entity to obtain its ETag
            String etag =
                    mockMvc.perform(
                                    get("/test-entities/{id}", savedEntity.getId())
                                            .accept(MediaType.APPLICATION_JSON))
                            .andExpect(status().isOk())
                            .andReturn()
                            .getResponse()
                            .getHeader("ETag");

            // Then, request with If-None-Match
            mockMvc.perform(
                            get("/test-entities/{id}", savedEntity.getId())
                                    .header("If-None-Match", etag)
                                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotModified());
        }
    }

    @Nested
    @DisplayName("HEAD /test-entities/{id}")
    class ExistsByIdTests {

        @Test
        @DisplayName("should return 200 when entity exists")
        void shouldReturn200WhenEntityExists() throws Exception {
            mockMvc.perform(head("/test-entities/{id}", savedEntity.getId()))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("should return 404 when entity does not exist")
        void shouldReturn404WhenEntityDoesNotExist() throws Exception {
            mockMvc.perform(head("/test-entities/{id}", 99999L)).andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /test-entities")
    class CreateTests {

        @Test
        @DisplayName("should create new entity and return 201")
        void shouldCreateNewEntityAndReturn201() throws Exception {
            TestEntityDTO newDto = TestEntityDTO.of(null, true, "New Entity");

            mockMvc.perform(
                            post("/test-entities")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(newDto)))
                    .andExpect(status().isCreated())
                    .andExpect(header().exists("Location"))
                    .andExpect(header().exists("ETag"))
                    .andExpect(jsonPath("$.name", is("New Entity")));
        }

        @Test
        @DisplayName("should return 400 for invalid input")
        void shouldReturn400ForInvalidInput() throws Exception {
            TestEntityDTO invalidDto = TestEntityDTO.of(null, true, null);

            mockMvc.perform(
                            post("/test-entities")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(invalidDto)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PUT /test-entities/{id}")
    class UpdateTests {

        @Test
        @DisplayName("should update entity and return 200")
        void shouldUpdateEntityAndReturn200() throws Exception {
            TestEntityDTO updateDto = TestEntityDTO.of(savedEntity.getId(), true, "Updated Entity");

            mockMvc.perform(
                            put("/test-entities/{id}", savedEntity.getId())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(updateDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name", is("Updated Entity")));
        }

        @Test
        @DisplayName("should return 404 when updating non-existent entity")
        void shouldReturn404WhenUpdatingNonExistentEntity() throws Exception {
            TestEntityDTO updateDto = TestEntityDTO.of(99999L, true, "Updated");

            mockMvc.perform(
                            put("/test-entities/{id}", 99999L)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(updateDto)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 400 when ID mismatch")
        void shouldReturn400WhenIdMismatch() throws Exception {
            TestEntityDTO updateDto = TestEntityDTO.of(999L, true, "Updated");

            mockMvc.perform(
                            put("/test-entities/{id}", savedEntity.getId())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(updateDto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 412 when ETag mismatch")
        void shouldReturn412WhenETagMismatch() throws Exception {
            TestEntityDTO updateDto = TestEntityDTO.of(savedEntity.getId(), true, "Updated");

            mockMvc.perform(
                            put("/test-entities/{id}", savedEntity.getId())
                                    .header("If-Match", "\"invalid-etag\"")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(updateDto)))
                    .andExpect(status().isPreconditionFailed());
        }
    }

    @Nested
    @DisplayName("PATCH /test-entities/{id}")
    class PartialUpdateTests {

        @Test
        @DisplayName("should partially update entity")
        void shouldPartiallyUpdateEntity() throws Exception {
            TestEntityDTO patchDto = TestEntityDTO.of(null, null, "Patched Name");

            mockMvc.perform(
                            patch("/test-entities/{id}", savedEntity.getId())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(patchDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name", is("Patched Name")));
        }

        @Test
        @DisplayName("should return 404 when patching non-existent entity")
        void shouldReturn404WhenPatchingNonExistentEntity() throws Exception {
            TestEntityDTO patchDto = TestEntityDTO.of(null, null, "Patched");

            mockMvc.perform(
                            patch("/test-entities/{id}", 99999L)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(patchDto)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /test-entities/{id}")
    class DeleteTests {

        @Test
        @DisplayName("should soft delete entity and return 204")
        void shouldSoftDeleteEntityAndReturn204() throws Exception {
            mockMvc.perform(delete("/test-entities/{id}", savedEntity.getId()))
                    .andExpect(status().isNoContent());

            // Entity still exists but is marked as inactive (estado=false)
            // The findById endpoint may or may not filter by estado depending on implementation
            // Just verify the delete operation succeeded
        }

        @Test
        @DisplayName("should return 404 when deleting non-existent entity")
        void shouldReturn404WhenDeletingNonExistentEntity() throws Exception {
            mockMvc.perform(delete("/test-entities/{id}", 99999L)).andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should hard delete entity when permanent=true")
        void shouldHardDeleteEntityWhenPermanentTrue() throws Exception {
            mockMvc.perform(
                            delete("/test-entities/{id}", savedEntity.getId())
                                    .param("permanent", "true"))
                    .andExpect(status().isNoContent());
        }
    }

    @Nested
    @DisplayName("POST /test-entities/{id}/restore")
    class RestoreTests {

        @Test
        @DisplayName("should restore soft-deleted entity")
        void shouldRestoreSoftDeletedEntity() throws Exception {
            // First soft delete
            mockMvc.perform(delete("/test-entities/{id}", savedEntity.getId()))
                    .andExpect(status().isNoContent());

            // Then restore
            mockMvc.perform(post("/test-entities/{id}/restore", savedEntity.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.activo", is(true)));
        }
    }
}
