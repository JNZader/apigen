package com.jnzader.apigen.core.infrastructure.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jnzader.apigen.core.fixtures.TestEntity;
import com.jnzader.apigen.core.fixtures.TestEntityDTO;
import com.jnzader.apigen.core.fixtures.TestEntityControllerImpl;
import com.jnzader.apigen.core.fixtures.TestEntityMapperAdapter;
import com.jnzader.apigen.core.fixtures.TestEntityResourceAssembler;
import com.jnzader.apigen.core.fixtures.TestEntityService;
import com.jnzader.apigen.core.support.TestEntityBuilder;
import com.jnzader.apigen.core.domain.exception.ResourceNotFoundException;
import com.jnzader.apigen.core.application.util.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import com.jnzader.apigen.core.fixtures.MockJwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.MediaType;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static com.jnzader.apigen.core.support.TestConstants.*;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests de controlador usando @WebMvcTest.
 * <p>
 * Estos tests verifican:
 * - Mappings HTTP correctos
 * - Serialización/deserialización JSON
 * - Códigos de respuesta HTTP
 * - Validación de entrada
 * - Manejo de errores
 * <p>
 * Usa MockMvc para tests rápidos sin levantar el servidor completo.
 */
@WebMvcTest(controllers = TestEntityControllerImpl.class)
@ActiveProfiles("test")
@DisplayName("BaseController Tests")
@Tag("security")
class BaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // ObjectMapper creado manualmente ya que @AutoConfigureJson no funciona en Spring Boot 4.x con @WebMvcTest
    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private TestEntityService testEntityService;

    @MockitoBean
    private TestEntityMapperAdapter testEntityMapper;

    @MockitoBean
    private TestEntityResourceAssembler testEntityResourceAssembler;

    // Mocks necesarios para la configuración de seguridad
    @MockitoBean
    private MockJwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private MeterRegistry meterRegistry;

    private TestEntity testEntity;
    private TestEntityDTO testEntityDTO;

    @BeforeEach
    void setUp() {
        TestEntityBuilder.resetIdCounter();
        testEntity = TestEntityBuilder.aTestEntityWithId()
                .withName(VALID_NAME)
                .build();
        testEntityDTO = TestEntityDTO.of(testEntity.getId(), true, VALID_NAME);

        // Default HATEOAS assembler stubbing
        given(testEntityResourceAssembler.toModel(any(TestEntityDTO.class)))
                .willAnswer(invocation -> EntityModel.of(invocation.getArgument(0)));
        given(testEntityResourceAssembler.toPagedModel(any()))
                .willAnswer(invocation -> {
                    org.springframework.data.domain.Page<TestEntityDTO> page = invocation.getArgument(0);
                    java.util.List<EntityModel<TestEntityDTO>> content = page.getContent().stream()
                            .map(EntityModel::of)
                            .toList();
                    return PagedModel.of(content,
                            new PagedModel.PageMetadata(page.getSize(), page.getNumber(), page.getTotalElements()));
                });
    }

    // ==================== GET Tests ====================

    @Nested
    @DisplayName("GET /test-entities")
    class GetAllTests {

        @Test
        @WithMockUser
        @DisplayName("should return list of entities")
        void shouldReturnListOfEntities() throws Exception {
            // Given - El controlador llama a findAll(Specification, Pageable)
            Page<TestEntity> page = new PageImpl<>(List.of(testEntity), PageRequest.of(0, 20), 1);
            given(testEntityService.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(org.springframework.data.domain.Pageable.class)))
                    .willReturn(Result.success(page));
            given(testEntityMapper.toDTO(testEntity)).willReturn(testEntityDTO);

            // When/Then - HATEOAS response format uses _embedded
            mockMvc.perform(get("/test-entities")
                            .accept(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$._embedded.testEntityDTOList", hasSize(1)))
                    .andExpect(jsonPath("$._embedded.testEntityDTOList[0].id", is(testEntity.getId().intValue())))
                    .andExpect(jsonPath("$._embedded.testEntityDTOList[0].name", is(VALID_NAME)));
        }

        @Test
        @WithMockUser
        @DisplayName("should return empty list when no entities")
        void shouldReturnEmptyListWhenNoEntities() throws Exception {
            // Given - El controlador llama a findAll(Specification, Pageable)
            Page<TestEntity> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
            given(testEntityService.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(org.springframework.data.domain.Pageable.class)))
                    .willReturn(Result.success(emptyPage));

            // When/Then - HATEOAS doesn't include _embedded when empty
            mockMvc.perform(get("/test-entities")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded").doesNotExist());
        }
    }

    @Nested
    @DisplayName("GET /test-entities (paginated)")
    class GetPagedTests {

        @Test
        @WithMockUser
        @DisplayName("should return paginated results with page params")
        void shouldReturnPaginatedResults() throws Exception {
            // Given - El endpoint principal / soporta paginación
            Page<TestEntity> page = new PageImpl<>(List.of(testEntity), PageRequest.of(0, 10), 1);
            given(testEntityService.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(org.springframework.data.domain.Pageable.class)))
                    .willReturn(Result.success(page));
            given(testEntityMapper.toDTO(testEntity)).willReturn(testEntityDTO);

            // When/Then - HATEOAS format
            mockMvc.perform(get("/test-entities")
                            .param("page", "0")
                            .param("size", "10")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.testEntityDTOList", hasSize(1)))
                    .andExpect(jsonPath("$.page.totalElements", is(1)));
        }
    }

    @Nested
    @DisplayName("GET /test-entities/{id}")
    class GetByIdTests {

        @Test
        @WithMockUser
        @DisplayName("should return entity when found")
        void shouldReturnEntityWhenFound() throws Exception {
            // Given
            given(testEntityService.findById(VALID_ID)).willReturn(Result.success(testEntity));
            given(testEntityMapper.toDTO(testEntity)).willReturn(testEntityDTO);

            // When/Then
            mockMvc.perform(get("/test-entities/{id}", VALID_ID)
                            .accept(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(VALID_ID.intValue())))
                    .andExpect(jsonPath("$.name", is(VALID_NAME)));
        }

        @Test
        @WithMockUser
        @DisplayName("should return 404 when entity not found")
        void shouldReturn404WhenNotFound() throws Exception {
            // Given
            given(testEntityService.findById(INVALID_ID))
                    .willReturn(Result.failure(new ResourceNotFoundException("Not found")));

            // When/Then
            mockMvc.perform(get("/test-entities/{id}", INVALID_ID)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());
        }
    }

    // ==================== POST Tests ====================

    @Nested
    @DisplayName("POST /test-entities")
    class CreateTests {

        @Test
        @WithMockUser
        @DisplayName("should create entity and return 201")
        void shouldCreateEntityAndReturn201() throws Exception {
            // Given
            TestEntityDTO newDto = TestEntityDTO.of(null, true, VALID_NAME);
            given(testEntityMapper.toEntity(any(TestEntityDTO.class))).willReturn(testEntity);
            given(testEntityService.save(any(TestEntity.class))).willReturn(Result.success(testEntity));
            given(testEntityMapper.toDTO(testEntity)).willReturn(testEntityDTO);

            // When/Then
            mockMvc.perform(post("/test-entities")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(newDto)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", notNullValue()))
                    .andExpect(jsonPath("$.name", is(VALID_NAME)));
        }

        @Test
        @WithMockUser
        @DisplayName("should return 400 for invalid input")
        void shouldReturn400ForInvalidInput() throws Exception {
            // Given
            TestEntityDTO invalidDto = TestEntityDTO.of(null, true, null);

            // When/Then - Validation should fail
            mockMvc.perform(post("/test-entities")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidDto)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ==================== PUT Tests ====================

    @Nested
    @DisplayName("PUT /test-entities/{id}")
    class UpdateTests {

        @Test
        @WithMockUser
        @DisplayName("should update entity and return 200")
        void shouldUpdateEntityAndReturn200() throws Exception {
            // Given
            TestEntityDTO updatedDto = TestEntityDTO.of(VALID_ID, true, UPDATED_NAME);
            TestEntity updatedEntity = TestEntityBuilder.aTestEntityWithId()
                    .withName(UPDATED_NAME)
                    .build();
            given(testEntityMapper.toEntity(any(TestEntityDTO.class))).willReturn(updatedEntity);
            given(testEntityService.update(eq(VALID_ID), any(TestEntity.class)))
                    .willReturn(Result.success(updatedEntity));
            given(testEntityMapper.toDTO(updatedEntity)).willReturn(updatedDto);

            // When/Then
            mockMvc.perform(put("/test-entities/{id}", VALID_ID)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updatedDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name", is(UPDATED_NAME)));
        }

        @Test
        @WithMockUser
        @DisplayName("should return 404 when updating non-existent entity")
        void shouldReturn404WhenUpdatingNonExistentEntity() throws Exception {
            // Given
            TestEntityDTO dto = TestEntityDTO.of(INVALID_ID, true, UPDATED_NAME);
            given(testEntityMapper.toEntity(any(TestEntityDTO.class))).willReturn(testEntity);
            given(testEntityService.update(eq(INVALID_ID), any(TestEntity.class)))
                    .willReturn(Result.failure(new ResourceNotFoundException("Not found")));

            // When/Then
            mockMvc.perform(put("/test-entities/{id}", INVALID_ID)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isNotFound());
        }
    }

    // ==================== DELETE Tests ====================

    @Nested
    @DisplayName("DELETE /test-entities/{id}")
    class DeleteTests {

        @Test
        @WithMockUser
        @DisplayName("should soft delete entity and return 204")
        void shouldSoftDeleteEntityAndReturn204() throws Exception {
            // Given
            given(testEntityService.softDelete(VALID_ID)).willReturn(Result.success(null));

            // When/Then
            mockMvc.perform(delete("/test-entities/{id}", VALID_ID)
                            .with(csrf()))
                    .andExpect(status().isNoContent());
        }

        @Test
        @WithMockUser
        @DisplayName("should return 404 when deleting non-existent entity")
        void shouldReturn404WhenDeletingNonExistentEntity() throws Exception {
            // Given
            given(testEntityService.softDelete(INVALID_ID))
                    .willReturn(Result.failure(new ResourceNotFoundException("Not found")));

            // When/Then
            mockMvc.perform(delete("/test-entities/{id}", INVALID_ID)
                            .with(csrf()))
                    .andExpect(status().isNotFound());
        }
    }

    // ==================== Restore Tests ====================

    @Nested
    @DisplayName("POST /test-entities/{id}/restore")
    class RestoreTests {

        @Test
        @WithMockUser
        @DisplayName("should restore entity and return 200")
        void shouldRestoreEntityAndReturn200() throws Exception {
            // Given
            given(testEntityService.restore(VALID_ID)).willReturn(Result.success(testEntity));
            given(testEntityMapper.toDTO(testEntity)).willReturn(testEntityDTO);

            // When/Then
            mockMvc.perform(post("/test-entities/{id}/restore", VALID_ID)
                            .with(csrf()))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser
        @DisplayName("should return 404 when restoring non-existent entity")
        void shouldReturn404WhenRestoringNonExistentEntity() throws Exception {
            // Given
            given(testEntityService.restore(INVALID_ID))
                    .willReturn(Result.failure(new ResourceNotFoundException("Not found")));

            // When/Then
            mockMvc.perform(post("/test-entities/{id}/restore", INVALID_ID)
                            .with(csrf()))
                    .andExpect(status().isNotFound());
        }
    }

    // ==================== HEAD Tests ====================

    @Nested
    @DisplayName("HEAD /test-entities")
    class CountTests {

        @Test
        @WithMockUser
        @DisplayName("should return count in header")
        void shouldReturnCountInHeader() throws Exception {
            // Given
            given(testEntityService.count()).willReturn(Result.success(42L));

            // When/Then
            mockMvc.perform(head("/test-entities"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("X-Total-Count", "42"));
        }

        @Test
        @WithMockUser
        @DisplayName("should return zero count when no entities")
        void shouldReturnZeroCountWhenNoEntities() throws Exception {
            // Given
            given(testEntityService.count()).willReturn(Result.success(0L));

            // When/Then
            mockMvc.perform(head("/test-entities"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("X-Total-Count", "0"));
        }
    }

    @Nested
    @DisplayName("HEAD /test-entities/{id}")
    class ExistsByIdTests {

        @Test
        @WithMockUser
        @DisplayName("should return 200 when entity exists")
        void shouldReturn200WhenEntityExists() throws Exception {
            // Given
            given(testEntityService.existsById(VALID_ID)).willReturn(Result.success(true));

            // When/Then
            mockMvc.perform(head("/test-entities/{id}", VALID_ID))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser
        @DisplayName("should return 404 when entity does not exist")
        void shouldReturn404WhenEntityDoesNotExist() throws Exception {
            // Given
            given(testEntityService.existsById(INVALID_ID)).willReturn(Result.success(false));

            // When/Then
            mockMvc.perform(head("/test-entities/{id}", INVALID_ID))
                    .andExpect(status().isNotFound());
        }
    }

    // ==================== PATCH Tests ====================

    @Nested
    @DisplayName("PATCH /test-entities/{id}")
    class PatchTests {

        @Test
        @WithMockUser
        @DisplayName("should partially update entity")
        void shouldPartiallyUpdateEntity() throws Exception {
            // Given
            TestEntityDTO patchDto = TestEntityDTO.of(null, null, UPDATED_NAME);
            given(testEntityService.findById(VALID_ID)).willReturn(Result.success(testEntity));
            given(testEntityService.save(any(TestEntity.class))).willReturn(Result.success(testEntity));
            given(testEntityMapper.toDTO(testEntity)).willReturn(TestEntityDTO.of(VALID_ID, true, UPDATED_NAME));

            // When/Then
            mockMvc.perform(patch("/test-entities/{id}", VALID_ID)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(patchDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name", is(UPDATED_NAME)));
        }

        @Test
        @WithMockUser
        @DisplayName("should return 404 when patching non-existent entity")
        void shouldReturn404WhenPatchingNonExistentEntity() throws Exception {
            // Given
            TestEntityDTO patchDto = TestEntityDTO.of(null, null, UPDATED_NAME);
            given(testEntityService.findById(INVALID_ID))
                    .willReturn(Result.failure(new ResourceNotFoundException("Not found")));

            // When/Then
            mockMvc.perform(patch("/test-entities/{id}", INVALID_ID)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(patchDto)))
                    .andExpect(status().isNotFound());
        }
    }

    // ==================== Hard Delete Tests ====================

    @Nested
    @DisplayName("DELETE /test-entities/{id} with permanent=true")
    class HardDeleteTests {

        @Test
        @WithMockUser
        @DisplayName("should hard delete entity when permanent=true")
        void shouldHardDeleteEntityWhenPermanentTrue() throws Exception {
            // Given
            given(testEntityService.hardDelete(VALID_ID)).willReturn(Result.success(null));

            // When/Then
            mockMvc.perform(delete("/test-entities/{id}", VALID_ID)
                            .param("permanent", "true")
                            .with(csrf()))
                    .andExpect(status().isNoContent());
        }

        @Test
        @WithMockUser
        @DisplayName("should return 404 when hard deleting non-existent entity")
        void shouldReturn404WhenHardDeletingNonExistentEntity() throws Exception {
            // Given
            given(testEntityService.hardDelete(INVALID_ID))
                    .willReturn(Result.failure(new ResourceNotFoundException("Not found")));

            // When/Then
            mockMvc.perform(delete("/test-entities/{id}", INVALID_ID)
                            .param("permanent", "true")
                            .with(csrf()))
                    .andExpect(status().isNotFound());
        }
    }

    // ==================== Sorting Tests ====================

    @Nested
    @DisplayName("GET /test-entities with sorting")
    class SortingTests {

        @Test
        @WithMockUser
        @DisplayName("should support sort parameter")
        void shouldSupportSortParameter() throws Exception {
            // Given
            Page<TestEntity> page = new PageImpl<>(List.of(testEntity), PageRequest.of(0, 20), 1);
            given(testEntityService.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(org.springframework.data.domain.Pageable.class)))
                    .willReturn(Result.success(page));
            given(testEntityMapper.toDTO(testEntity)).willReturn(testEntityDTO);

            // When/Then - HATEOAS format
            mockMvc.perform(get("/test-entities")
                            .param("sort", "name,asc")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.testEntityDTOList", hasSize(1)));
        }

        @Test
        @WithMockUser
        @DisplayName("should support multiple sort parameters")
        void shouldSupportMultipleSortParameters() throws Exception {
            // Given
            Page<TestEntity> page = new PageImpl<>(List.of(testEntity), PageRequest.of(0, 20), 1);
            given(testEntityService.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(org.springframework.data.domain.Pageable.class)))
                    .willReturn(Result.success(page));
            given(testEntityMapper.toDTO(testEntity)).willReturn(testEntityDTO);

            // When/Then
            mockMvc.perform(get("/test-entities")
                            .param("sort", "name,asc")
                            .param("sort", "id,desc")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }
    }

    // ==================== Filter Tests ====================

    @Nested
    @DisplayName("GET /test-entities with filter")
    class FilterTests {

        @Test
        @WithMockUser
        @DisplayName("should support filter parameter")
        void shouldSupportFilterParameter() throws Exception {
            // Given
            Page<TestEntity> page = new PageImpl<>(List.of(testEntity), PageRequest.of(0, 20), 1);
            given(testEntityService.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(org.springframework.data.domain.Pageable.class)))
                    .willReturn(Result.success(page));
            given(testEntityMapper.toDTO(testEntity)).willReturn(testEntityDTO);

            // When/Then - HATEOAS format
            mockMvc.perform(get("/test-entities")
                            .param("filter", "name:like:Test")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.testEntityDTOList", hasSize(1)));
        }

        @Test
        @WithMockUser
        @DisplayName("should support fields parameter for sparse fieldsets")
        void shouldSupportFieldsParameter() throws Exception {
            // Given
            Page<TestEntity> page = new PageImpl<>(List.of(testEntity), PageRequest.of(0, 20), 1);
            given(testEntityService.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(org.springframework.data.domain.Pageable.class)))
                    .willReturn(Result.success(page));
            given(testEntityMapper.toDTO(testEntity)).willReturn(testEntityDTO);

            // When/Then
            mockMvc.perform(get("/test-entities")
                            .param("fields", "id,name")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }
    }

    // ==================== Validation Tests ====================

    @Nested
    @DisplayName("Pagination Validation")
    class PaginationValidationTests {

        @Test
        @WithMockUser
        @DisplayName("should return 400 for negative page number")
        void shouldReturn400ForNegativePageNumber() throws Exception {
            // When/Then
            mockMvc.perform(get("/test-entities")
                            .param("page", "-1")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser
        @DisplayName("should return 400 for zero size")
        void shouldReturn400ForZeroSize() throws Exception {
            // When/Then
            mockMvc.perform(get("/test-entities")
                            .param("size", "0")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser
        @DisplayName("should return 400 for negative size")
        void shouldReturn400ForNegativeSize() throws Exception {
            // When/Then
            mockMvc.perform(get("/test-entities")
                            .param("size", "-5")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser
        @DisplayName("should return 400 for non-numeric page")
        void shouldReturn400ForNonNumericPage() throws Exception {
            // When/Then
            mockMvc.perform(get("/test-entities")
                            .param("page", "abc")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser
        @DisplayName("should return 400 for non-numeric size")
        void shouldReturn400ForNonNumericSize() throws Exception {
            // When/Then
            mockMvc.perform(get("/test-entities")
                            .param("size", "xyz")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }
    }

    // ==================== ETag Tests ====================

    @Nested
    @DisplayName("ETag Handling")
    class ETagTests {

        @Test
        @WithMockUser
        @DisplayName("should return ETag header on GET by ID")
        void shouldReturnETagHeaderOnGetById() throws Exception {
            // Given
            given(testEntityService.findById(VALID_ID)).willReturn(Result.success(testEntity));
            given(testEntityMapper.toDTO(testEntity)).willReturn(testEntityDTO);

            // When/Then
            mockMvc.perform(get("/test-entities/{id}", VALID_ID)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(header().exists("ETag"));
        }

        @Test
        @WithMockUser
        @DisplayName("should return 304 when If-None-Match matches ETag")
        void shouldReturn304WhenIfNoneMatchMatchesETag() throws Exception {
            // Given
            given(testEntityService.findById(VALID_ID)).willReturn(Result.success(testEntity));
            given(testEntityMapper.toDTO(testEntity)).willReturn(testEntityDTO);

            // First request to get ETag
            String etag = mockMvc.perform(get("/test-entities/{id}", VALID_ID)
                            .accept(MediaType.APPLICATION_JSON))
                    .andReturn()
                    .getResponse()
                    .getHeader("ETag");

            // When/Then - Second request with If-None-Match
            mockMvc.perform(get("/test-entities/{id}", VALID_ID)
                            .header("If-None-Match", etag)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotModified());
        }
    }

    // ==================== ID Mismatch Tests ====================

    @Nested
    @DisplayName("ID Mismatch Handling")
    class IdMismatchTests {

        @Test
        @WithMockUser
        @DisplayName("should return 400 when DTO ID doesn't match path ID")
        void shouldReturn400WhenIdMismatch() throws Exception {
            // Given - DTO with different ID than path
            TestEntityDTO mismatchedDto = TestEntityDTO.of(999L, true, UPDATED_NAME);

            // When/Then
            mockMvc.perform(put("/test-entities/{id}", VALID_ID)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(mismatchedDto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser
        @DisplayName("should accept when DTO ID is null")
        void shouldAcceptWhenDtoIdIsNull() throws Exception {
            // Given - DTO with null ID is acceptable
            TestEntityDTO dtoWithNullId = TestEntityDTO.of(null, true, UPDATED_NAME);
            TestEntity updatedEntity = TestEntityBuilder.aTestEntityWithId()
                    .withName(UPDATED_NAME)
                    .build();
            given(testEntityMapper.toEntity(any(TestEntityDTO.class))).willReturn(updatedEntity);
            given(testEntityService.update(eq(VALID_ID), any(TestEntity.class)))
                    .willReturn(Result.success(updatedEntity));
            given(testEntityMapper.toDTO(updatedEntity)).willReturn(TestEntityDTO.of(VALID_ID, true, UPDATED_NAME));

            // When/Then
            mockMvc.perform(put("/test-entities/{id}", VALID_ID)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dtoWithNullId)))
                    .andExpect(status().isOk());
        }
    }

    // ==================== Optimistic Concurrency Tests ====================

    @Nested
    @DisplayName("Optimistic Concurrency (If-Match)")
    class OptimisticConcurrencyTests {

        @Test
        @WithMockUser
        @DisplayName("should return 412 when If-Match doesn't match current ETag")
        void shouldReturn412WhenETagMismatch() throws Exception {
            // Given
            given(testEntityService.findById(VALID_ID)).willReturn(Result.success(testEntity));
            given(testEntityMapper.toDTO(testEntity)).willReturn(testEntityDTO);
            TestEntityDTO updateDto = TestEntityDTO.of(VALID_ID, true, UPDATED_NAME);

            // When/Then - Using mismatched ETag
            mockMvc.perform(put("/test-entities/{id}", VALID_ID)
                            .with(csrf())
                            .header("If-Match", "\"wrong-etag\"")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateDto)))
                    .andExpect(status().isPreconditionFailed());
        }

        @Test
        @WithMockUser
        @DisplayName("should return 412 when If-Match doesn't match on PATCH")
        void shouldReturn412WhenETagMismatchOnPatch() throws Exception {
            // Given
            given(testEntityService.findById(VALID_ID)).willReturn(Result.success(testEntity));
            given(testEntityMapper.toDTO(testEntity)).willReturn(testEntityDTO);
            TestEntityDTO patchDto = TestEntityDTO.of(null, null, UPDATED_NAME);

            // When/Then - Using mismatched ETag
            mockMvc.perform(patch("/test-entities/{id}", VALID_ID)
                            .with(csrf())
                            .header("If-Match", "\"wrong-etag\"")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(patchDto)))
                    .andExpect(status().isPreconditionFailed());
        }
    }

    // ==================== Sparse Fieldsets on Single Resource ====================

    @Nested
    @DisplayName("Sparse Fieldsets on Single Resource")
    class SparseFieldsetsSingleResourceTests {

        @Test
        @WithMockUser
        @DisplayName("should support fields parameter on GET by ID")
        void shouldSupportFieldsParameterOnGetById() throws Exception {
            // Given
            given(testEntityService.findById(VALID_ID)).willReturn(Result.success(testEntity));
            given(testEntityMapper.toDTO(testEntity)).willReturn(testEntityDTO);

            // When/Then
            mockMvc.perform(get("/test-entities/{id}", VALID_ID)
                            .param("fields", "id,name")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }
    }

    // ==================== Error Handling Tests ====================

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandlingTests {

        @Test
        @WithMockUser
        @DisplayName("should return 500 when count fails")
        void shouldReturn500WhenCountFails() throws Exception {
            // Given
            given(testEntityService.count()).willReturn(Result.failure(new RuntimeException("DB error")));

            // When/Then
            mockMvc.perform(head("/test-entities"))
                    .andExpect(status().isInternalServerError());
        }

        @Test
        @WithMockUser
        @DisplayName("should return 500 when existsById fails")
        void shouldReturn500WhenExistsByIdFails() throws Exception {
            // Given
            given(testEntityService.existsById(VALID_ID))
                    .willReturn(Result.failure(new RuntimeException("DB error")));

            // When/Then
            mockMvc.perform(head("/test-entities/{id}", VALID_ID))
                    .andExpect(status().isInternalServerError());
        }

        @Test
        @WithMockUser
        @DisplayName("should return 500 when restore fails")
        void shouldReturn500WhenRestoreFails() throws Exception {
            // Given
            given(testEntityService.restore(VALID_ID))
                    .willReturn(Result.failure(new RuntimeException("DB error")));

            // When/Then
            mockMvc.perform(post("/test-entities/{id}/restore", VALID_ID)
                            .with(csrf()))
                    .andExpect(status().isInternalServerError());
        }

        @Test
        @WithMockUser
        @DisplayName("should return 500 when soft delete fails with unexpected error")
        void shouldReturn500WhenSoftDeleteFailsWithUnexpectedError() throws Exception {
            // Given
            given(testEntityService.softDelete(VALID_ID))
                    .willReturn(Result.failure(new RuntimeException("Unexpected error")));

            // When/Then
            mockMvc.perform(delete("/test-entities/{id}", VALID_ID)
                            .with(csrf()))
                    .andExpect(status().isInternalServerError());
        }

        @Test
        @WithMockUser
        @DisplayName("should return 500 when hard delete fails with unexpected error")
        void shouldReturn500WhenHardDeleteFailsWithUnexpectedError() throws Exception {
            // Given
            given(testEntityService.hardDelete(VALID_ID))
                    .willReturn(Result.failure(new RuntimeException("Unexpected error")));

            // When/Then
            mockMvc.perform(delete("/test-entities/{id}", VALID_ID)
                            .param("permanent", "true")
                            .with(csrf()))
                    .andExpect(status().isInternalServerError());
        }
    }

    // ==================== Security Tests (Basic) ====================
    // NOTA: Con perfil test, la seguridad está deshabilitada

    @Nested
    @DisplayName("Security (Disabled)")
    class SecurityTests {

        @Test
        @DisplayName("should allow access without authentication when security disabled")
        void shouldAllowAccessWithoutAuthentication() throws Exception {
            // Given - Configurar mock para la llamada que hará el controlador
            Page<TestEntity> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
            given(testEntityService.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(org.springframework.data.domain.Pageable.class)))
                    .willReturn(Result.success(emptyPage));

            // When/Then - Con seguridad deshabilitada, el acceso es permitido
            mockMvc.perform(get("/test-entities"))
                    .andExpect(status().isOk());
        }
    }
}
