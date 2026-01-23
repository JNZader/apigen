package com.example.myapi.customers.infrastructure.controller;

import com.example.myapi.customers.application.dto.CustomerDTO;
import com.example.myapi.customers.application.mapper.CustomerMapper;
import com.example.myapi.customers.application.service.CustomerService;
import com.example.myapi.customers.domain.entity.Customer;
import com.jnzader.apigen.core.application.util.Result;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.MediaType;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomerController Tests")
class CustomerControllerImplTest {

    @Mock
    private CustomerService service;

    @Mock
    private CustomerMapper mapper;

    private MockMvc mockMvc;
    private JsonMapper jsonMapper;
    private CustomerControllerImpl controller;

    private Customer customer;
    private CustomerDTO dto;

    @BeforeEach
    void setUp() {
        controller = new CustomerControllerImpl(service, mapper);
        jsonMapper = JsonMapper.builder().build();
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setMessageConverters(new JacksonJsonHttpMessageConverter(jsonMapper))
                .build();

        customer = new Customer();
        customer.setId(1L);
        customer.setEstado(true);
        customer.setEmail("Test email");
        customer.setFirstName("Test firstName");
        customer.setLastName("Test lastName");
        customer.setPhone("Test phone");
        customer.setAddress("Test address");

        dto = new CustomerDTO();
        dto.setId(1L);
        dto.setActivo(true);
        dto.setEmail("Test email");
        dto.setFirstName("Test firstName");
        dto.setLastName("Test lastName");
        dto.setPhone("Test phone");
        dto.setAddress("Test address");
    }

    @Nested
    @DisplayName("GET Operations")
    class GetOperations {

        @Test
        @DisplayName("Should get all Customer with pagination")
        @SuppressWarnings("unchecked")
        void shouldGetAllWithPagination() throws Exception {
            Page<Customer> page = new PageImpl<>(new ArrayList<>(List.of(customer)), PageRequest.of(0, 10), 1);
            when(service.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Result.success(page));
            when(mapper.toDTO(any(Customer.class))).thenReturn(dto);

            mockMvc.perform(get("/api/v1/customers")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk());

            verify(service).findAll(any(Specification.class), any(Pageable.class));
        }

        @Test
        @DisplayName("Should get Customer by ID")
        void shouldGetById() throws Exception {
            when(service.findById(1L)).thenReturn(Result.success(customer));
            when(mapper.toDTO(customer)).thenReturn(dto);

            mockMvc.perform(get("/api/v1/customers/1"))
                    .andExpect(status().isOk());

            verify(service).findById(1L);
        }

        @Test
        @DisplayName("Should check if Customer exists")
        void shouldCheckExists() throws Exception {
            when(service.existsById(1L)).thenReturn(Result.success(true));

            mockMvc.perform(head("/api/v1/customers/1"))
                    .andExpect(status().isOk());

            verify(service).existsById(1L);
        }
    }

    @Nested
    @DisplayName("POST Operations")
    class PostOperations {

        @Test
        @DisplayName("Should create new Customer")
        void shouldCreateNew() throws Exception {
            when(mapper.toEntity(any(CustomerDTO.class))).thenReturn(customer);
            when(service.save(any(Customer.class))).thenReturn(Result.success(customer));
            when(mapper.toDTO(any(Customer.class))).thenReturn(dto);

            mockMvc.perform(post("/api/v1/customers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(dto)))
                    .andExpect(status().isCreated());

            verify(service).save(any(Customer.class));
        }

        @Test
        @DisplayName("Should restore soft-deleted Customer")
        void shouldRestore() throws Exception {
            when(service.restore(1L)).thenReturn(Result.success(customer));
            when(mapper.toDTO(customer)).thenReturn(dto);

            mockMvc.perform(post("/api/v1/customers/1/restore"))
                    .andExpect(status().isOk());

            verify(service).restore(1L);
        }
    }

    @Nested
    @DisplayName("PUT Operations")
    class PutOperations {

        @Test
        @DisplayName("Should update Customer")
        void shouldUpdate() throws Exception {
            when(mapper.toEntity(any(CustomerDTO.class))).thenReturn(customer);
            when(service.update(anyLong(), any(Customer.class))).thenReturn(Result.success(customer));
            when(mapper.toDTO(any(Customer.class))).thenReturn(dto);

            mockMvc.perform(put("/api/v1/customers/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk());

            verify(service).update(eq(1L), any(Customer.class));
        }
    }

    @Nested
    @DisplayName("PATCH Operations")
    class PatchOperations {

        @Test
        @DisplayName("Should partial update Customer")
        void shouldPartialUpdate() throws Exception {
            // Controller PATCH calls: findById -> updateEntityFromDTO -> save
            when(service.findById(1L)).thenReturn(Result.success(customer));
            // updateEntityFromDTO is void, no need to mock return
            when(service.save(any(Customer.class))).thenReturn(Result.success(customer));
            when(mapper.toDTO(any(Customer.class))).thenReturn(dto);

            mockMvc.perform(patch("/api/v1/customers/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk());

            verify(service).findById(1L);
            verify(service).save(any(Customer.class));
        }
    }

    @Nested
    @DisplayName("DELETE Operations")
    class DeleteOperations {

        @Test
        @DisplayName("Should soft delete Customer")
        void shouldSoftDelete() throws Exception {
            when(service.softDelete(1L)).thenReturn(Result.success(null));

            mockMvc.perform(delete("/api/v1/customers/1"))
                    .andExpect(status().isNoContent());

            verify(service).softDelete(1L);
        }

        @Test
        @DisplayName("Should hard delete Customer with permanent flag")
        void shouldHardDelete() throws Exception {
            when(service.hardDelete(1L)).thenReturn(Result.success(null));

            mockMvc.perform(delete("/api/v1/customers/1")
                            .param("permanent", "true"))
                    .andExpect(status().isNoContent());

            verify(service).hardDelete(1L);
        }
    }
}
