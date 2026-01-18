package com.jnzader.apigen.core.infrastructure.hateoas;

import com.jnzader.apigen.core.config.TestSecurityConfig;
import com.jnzader.apigen.core.infrastructure.config.JpaConfig;
import com.jnzader.apigen.core.fixtures.TestEntity;
import com.jnzader.apigen.core.fixtures.TestEntityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
 * Integration tests for BaseResourceAssembler.
 * Tests HATEOAS link generation through the controller endpoints.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestSecurityConfig.class, JpaConfig.class})
@ActiveProfiles("test")
@Transactional
@DisplayName("BaseResourceAssembler Integration Tests")
class BaseResourceAssemblerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestEntityRepository testEntityRepository;

    private TestEntity savedEntity;

    @BeforeEach
    void setUp() {
        testEntityRepository.deleteAll();
        TestEntity entity = new TestEntity();
        entity.setName("HATEOAS Test Entity");
        entity.setEstado(true);
        savedEntity = testEntityRepository.save(entity);
    }

    @Nested
    @DisplayName("Single Resource HATEOAS Links")
    class SingleResourceTests {

        @Test
        @DisplayName("should include self link in response")
        void shouldIncludeSelfLinkInResponse() throws Exception {
            MvcResult result = mockMvc.perform(get("/test-entities/{id}", savedEntity.getId())
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();
            // The response should contain the entity data and HATEOAS links
            assertThat(responseBody)
                    .contains("\"name\":\"HATEOAS Test Entity\"")
                    .contains("_links")
                    .contains("\"self\"");
        }

        @ParameterizedTest(name = "should include {0} link in response")
        @ValueSource(strings = {"collection", "update", "delete"})
        void shouldIncludeExpectedLinkInResponse(String linkRel) throws Exception {
            MvcResult result = mockMvc.perform(get("/test-entities/{id}", savedEntity.getId())
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();
            assertThat(responseBody).contains("\"" + linkRel + "\"");
        }
    }

    @Nested
    @DisplayName("Collection Resource HATEOAS Links")
    class CollectionResourceTests {

        @Test
        @DisplayName("should return paginated collection")
        void shouldReturnPaginatedCollection() throws Exception {
            // Create additional entities
            for (int i = 0; i < 5; i++) {
                TestEntity entity = new TestEntity();
                entity.setName("Entity " + i);
                entity.setEstado(true);
                testEntityRepository.save(entity);
            }

            // With HATEOAS enabled, the collection is in _embedded.testEntityDTOList
            mockMvc.perform(get("/test-entities")
                            .param("page", "0")
                            .param("size", "3")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.testEntityDTOList", hasSize(3)))
                    .andExpect(header().string("X-Total-Count", "6"));
        }

        @Test
        @DisplayName("should include pagination metadata")
        void shouldIncludePaginationMetadata() throws Exception {
            mockMvc.perform(get("/test-entities")
                            .param("page", "0")
                            .param("size", "10")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(header().exists("X-Page-Number"))
                    .andExpect(header().exists("X-Page-Size"))
                    .andExpect(header().exists("X-Total-Pages"));
        }

        @Test
        @DisplayName("should include HATEOAS page links in response")
        void shouldIncludeHateoasPageLinksInResponse() throws Exception {
            // Create additional entities
            for (int i = 0; i < 15; i++) {
                TestEntity entity = new TestEntity();
                entity.setName("Entity " + i);
                entity.setEstado(true);
                testEntityRepository.save(entity);
            }

            MvcResult result = mockMvc.perform(get("/test-entities")
                            .param("page", "0")
                            .param("size", "5")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();
            // Should contain pagination links
            assertThat(responseBody)
                    .contains("_links")
                    .contains("\"self\"")
                    .contains("\"first\"")
                    .contains("\"next\"")
                    .contains("\"last\"");
        }

        @Test
        @DisplayName("should include next link when not on last page")
        void shouldIncludeNextLinkWhenNotOnLastPage() throws Exception {
            // Create additional entities
            for (int i = 0; i < 10; i++) {
                TestEntity entity = new TestEntity();
                entity.setName("Entity " + i);
                entity.setEstado(true);
                testEntityRepository.save(entity);
            }

            MvcResult result = mockMvc.perform(get("/test-entities")
                            .param("page", "0")
                            .param("size", "5")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();
            assertThat(responseBody).contains("\"next\"");
        }

        @Test
        @DisplayName("should include prev link when not on first page")
        void shouldIncludePrevLinkWhenNotOnFirstPage() throws Exception {
            // Create additional entities
            for (int i = 0; i < 10; i++) {
                TestEntity entity = new TestEntity();
                entity.setName("Entity " + i);
                entity.setEstado(true);
                testEntityRepository.save(entity);
            }

            MvcResult result = mockMvc.perform(get("/test-entities")
                            .param("page", "1")
                            .param("size", "5")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();
            assertThat(responseBody).contains("\"prev\"");
        }

        @Test
        @DisplayName("should include items with HATEOAS links in collection")
        void shouldIncludeItemsWithHateoasLinksInCollection() throws Exception {
            MvcResult result = mockMvc.perform(get("/test-entities")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();
            // With HATEOAS enabled, items are in _embedded with HATEOAS links
            assertThat(responseBody)
                    .contains("_embedded")
                    .contains("_links");
        }

        @Test
        @DisplayName("should include page metadata in response")
        void shouldIncludePageMetadataInResponse() throws Exception {
            MvcResult result = mockMvc.perform(get("/test-entities")
                            .param("page", "0")
                            .param("size", "10")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();
            assertThat(responseBody).contains("\"page\"");
        }
    }

    @Nested
    @DisplayName("Created Resource HATEOAS")
    class CreatedResourceTests {

        @Test
        @DisplayName("should return location header on create")
        void shouldReturnLocationHeaderOnCreate() throws Exception {
            String newEntityJson = """
                {
                    "name": "New HATEOAS Entity",
                    "activo": true
                }
                """;

            MvcResult result = mockMvc.perform(post("/test-entities")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(newEntityJson))
                    .andExpect(status().isCreated())
                    .andExpect(header().exists("Location"))
                    .andReturn();

            String location = result.getResponse().getHeader("Location");
            assertThat(location).contains("/test-entities/");
        }

        @Test
        @DisplayName("should return HATEOAS links on create")
        void shouldReturnHateoasLinksOnCreate() throws Exception {
            String newEntityJson = """
                {
                    "name": "New HATEOAS Entity",
                    "activo": true
                }
                """;

            MvcResult result = mockMvc.perform(post("/test-entities")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(newEntityJson))
                    .andExpect(status().isCreated())
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();
            assertThat(responseBody)
                    .contains("_links")
                    .contains("\"self\"");
        }
    }

    @Nested
    @DisplayName("Updated Resource HATEOAS")
    class UpdatedResourceTests {

        @Test
        @DisplayName("should return ETag on update")
        void shouldReturnETagOnUpdate() throws Exception {
            String updateJson = String.format("""
                {
                    "id": %d,
                    "name": "Updated HATEOAS Entity",
                    "activo": true
                }
                """, savedEntity.getId());

            mockMvc.perform(put("/test-entities/{id}", savedEntity.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateJson))
                    .andExpect(status().isOk())
                    .andExpect(header().exists("ETag"))
                    .andExpect(jsonPath("$.name", is("Updated HATEOAS Entity")));
        }

        @Test
        @DisplayName("should return HATEOAS links on update")
        void shouldReturnHateoasLinksOnUpdate() throws Exception {
            String updateJson = String.format("""
                {
                    "id": %d,
                    "name": "Updated HATEOAS Entity",
                    "activo": true
                }
                """, savedEntity.getId());

            MvcResult result = mockMvc.perform(put("/test-entities/{id}", savedEntity.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateJson))
                    .andExpect(status().isOk())
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();
            assertThat(responseBody)
                    .contains("_links")
                    .contains("\"self\"");
        }

        @Test
        @DisplayName("should return HATEOAS links on partial update")
        void shouldReturnHateoasLinksOnPartialUpdate() throws Exception {
            String patchJson = """
                {
                    "name": "Patched HATEOAS Entity"
                }
                """;

            MvcResult result = mockMvc.perform(patch("/test-entities/{id}", savedEntity.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(patchJson))
                    .andExpect(status().isOk())
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();
            assertThat(responseBody)
                    .contains("_links")
                    .contains("\"self\"");
        }
    }

    @Nested
    @DisplayName("Restored Resource HATEOAS")
    class RestoredResourceTests {

        @Test
        @DisplayName("should return HATEOAS links on restore")
        void shouldReturnHateoasLinksOnRestore() throws Exception {
            // First soft delete
            mockMvc.perform(delete("/test-entities/{id}", savedEntity.getId()))
                    .andExpect(status().isNoContent());

            // Then restore
            MvcResult result = mockMvc.perform(post("/test-entities/{id}/restore", savedEntity.getId()))
                    .andExpect(status().isOk())
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();
            assertThat(responseBody)
                    .contains("_links")
                    .contains("\"self\"");
        }
    }

    @Nested
    @DisplayName("Filtering and Sparse Fieldsets")
    class FilteringTests {

        @Test
        @DisplayName("should support filter parameter")
        void shouldSupportFilterParameter() throws Exception {
            // With HATEOAS enabled, the collection is in _embedded.testEntityDTOList
            mockMvc.perform(get("/test-entities")
                            .param("filter", "name:like:HATEOAS")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.testEntityDTOList", hasSize(greaterThanOrEqualTo(1))));
        }

        @Test
        @DisplayName("should support sparse fieldsets")
        void shouldSupportSparseFieldsets() throws Exception {
            mockMvc.perform(get("/test-entities")
                            .param("fields", "id,name")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("should support sparse fieldsets on single resource")
        void shouldSupportSparseFieldsetsOnSingleResource() throws Exception {
            mockMvc.perform(get("/test-entities/{id}", savedEntity.getId())
                            .param("fields", "id,name")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }
    }
}
