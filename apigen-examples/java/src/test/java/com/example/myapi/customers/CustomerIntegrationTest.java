package com.example.myapi.customers;

import com.example.myapi.customers.application.dto.CustomerDTO;
import com.example.myapi.customers.domain.entity.Customer;
import com.example.myapi.customers.infrastructure.repository.CustomerRepository;
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
 * Integration tests for Customer entity.
 * Tests the full stack: Controller -> Service -> Repository -> Database (H2)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Customer Integration Tests")
class CustomerIntegrationTest {

    private static final String BASE_URL = "/api/v1/customers";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private CustomerRepository repository;

    private CustomerDTO testDto;
    private static Long createdId;

    @BeforeEach
    void setUp() {
        testDto = CustomerDTO.builder()
                .activo(true)
                .email("Test email")
                .firstName("Test firstName")
                .lastName("Test lastName")
                .phone("Test phone")
                .address("Test address")
                .build();
    }

    @Test
    @org.junit.jupiter.api.Order(1)
    @DisplayName("1. POST - Should create new Customer")
    void shouldCreateNewCustomer() throws Exception {
        MvcResult result = mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(testDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.activo").value(true))
                .andExpect(jsonPath("$.email").isNotEmpty())
                .andExpect(jsonPath("$.firstName").isNotEmpty())
                .andExpect(jsonPath("$.lastName").isNotEmpty())
                .andExpect(jsonPath("$.phone").isNotEmpty())
                .andExpect(jsonPath("$.address").isNotEmpty())
                .andReturn();

        // Extract created ID for subsequent tests
        String response = result.getResponse().getContentAsString();
        CustomerDTO created = jsonMapper.readValue(response, CustomerDTO.class);
        createdId = created.getId();

        assertThat(createdId).isNotNull();
    }

    @Test
    @org.junit.jupiter.api.Order(2)
    @DisplayName("2. GET /{id} - Should find Customer by ID")
    void shouldFindCustomerById() throws Exception {
        assertThat(createdId).isNotNull();

        mockMvc.perform(get(BASE_URL + "/" + createdId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(createdId))
                .andExpect(jsonPath("$.activo").value(true));
    }

    @Test
    @org.junit.jupiter.api.Order(3)
    @DisplayName("3. GET - Should list all Customer with pagination")
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
    @DisplayName("4. HEAD /{id} - Should check Customer exists")
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
    @DisplayName("6. PUT /{id} - Should update Customer")
    void shouldUpdateCustomer() throws Exception {
        assertThat(createdId).isNotNull();

        // Get current entity and modify
        MvcResult getResult = mockMvc.perform(get(BASE_URL + "/" + createdId))
                .andReturn();

        CustomerDTO dto = jsonMapper.readValue(
                getResult.getResponse().getContentAsString(), CustomerDTO.class);

        dto.setEmail("Updated email");
        dto.setFirstName("Updated firstName");
        dto.setLastName("Updated lastName");
        dto.setPhone("Updated phone");
        dto.setAddress("Updated address");
        mockMvc.perform(put(BASE_URL + "/" + createdId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(createdId));
    }

    @Test
    @org.junit.jupiter.api.Order(7)
    @DisplayName("7. PATCH /{id} - Should partial update Customer")
    void shouldPartialUpdateCustomer() throws Exception {
        assertThat(createdId).isNotNull();

        // Only update activo field
        String patchJson = "{\"activo\": true}";

        mockMvc.perform(patch(BASE_URL + "/" + createdId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(patchJson))
                .andExpect(status().isOk());
    }

    @Test
    @org.junit.jupiter.api.Order(8)
    @DisplayName("8. DELETE /{id} - Should soft delete Customer")
    void shouldSoftDeleteCustomer() throws Exception {
        assertThat(createdId).isNotNull();

        mockMvc.perform(delete(BASE_URL + "/" + createdId))
                .andExpect(status().isNoContent());

        // Verify entity is soft deleted (not visible in normal queries)
        mockMvc.perform(get(BASE_URL + "/" + createdId))
                .andExpect(status().isNotFound());
    }

    @Test
    @org.junit.jupiter.api.Order(9)
    @DisplayName("9. POST /{id}/restore - Should restore soft-deleted Customer")
    void shouldRestoreCustomer() throws Exception {
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
    @DisplayName("10. DELETE /{id}?permanent=true - Should permanently delete Customer")
    void shouldPermanentlyDeleteCustomer() throws Exception {
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
    @DisplayName("11. GET /{id} - Should return 404 for non-existent Customer")
    void shouldReturn404ForNonExistent() throws Exception {
        mockMvc.perform(get(BASE_URL + "/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @org.junit.jupiter.api.Order(12)
    @DisplayName("12. POST - Should validate required fields")
    void shouldValidateRequiredFields() throws Exception {
        CustomerDTO invalidDto = CustomerDTO.builder().build();

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @org.junit.jupiter.api.Order(13)
    @DisplayName("13. GET /cursor - Should support cursor pagination")
    void shouldSupportCursorPagination() throws Exception {
        // First, create a few entities for pagination with unique values
        for (int i = 0; i < 3; i++) {
            CustomerDTO uniqueDto = CustomerDTO.builder()
                        .activo(true)
                        .email("Test email" + "-" + java.util.UUID.randomUUID().toString().substring(0, 8) + "-" + i)
                        .firstName("Test firstName" + "-" + java.util.UUID.randomUUID().toString().substring(0, 8) + "-" + i)
                        .lastName("Test lastName" + "-" + java.util.UUID.randomUUID().toString().substring(0, 8) + "-" + i)
                        .phone("Test phone" + "-" + java.util.UUID.randomUUID().toString().substring(0, 8) + "-" + i)
                        .address("Test address" + "-" + java.util.UUID.randomUUID().toString().substring(0, 8) + "-" + i)
                        .build();
            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(uniqueDto)))
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
