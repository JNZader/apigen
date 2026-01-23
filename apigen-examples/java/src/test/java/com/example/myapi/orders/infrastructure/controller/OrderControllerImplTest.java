package com.example.myapi.orders.infrastructure.controller;

import com.example.myapi.orders.application.dto.OrderDTO;
import com.example.myapi.orders.application.mapper.OrderMapper;
import com.example.myapi.orders.application.service.OrderService;
import com.example.myapi.orders.domain.entity.Order;
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
@DisplayName("OrderController Tests")
class OrderControllerImplTest {

    @Mock
    private OrderService service;

    @Mock
    private OrderMapper mapper;

    private MockMvc mockMvc;
    private JsonMapper jsonMapper;
    private OrderControllerImpl controller;

    private Order order;
    private OrderDTO dto;

    @BeforeEach
    void setUp() {
        controller = new OrderControllerImpl(service, mapper);
        jsonMapper = JsonMapper.builder().build();
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setMessageConverters(new JacksonJsonHttpMessageConverter(jsonMapper))
                .build();

        order = new Order();
        order.setId(1L);
        order.setEstado(true);
        order.setOrderNumber("Test orderNumber");
        order.setTotal(new java.math.BigDecimal("199.99"));
        order.setStatus("Test status");
        order.setShippingAddress("Test shippingAddress");
        order.setOrderDate(java.time.LocalDateTime.now());

        dto = new OrderDTO();
        dto.setId(1L);
        dto.setActivo(true);
        dto.setOrderNumber("Test orderNumber");
        dto.setTotal(new java.math.BigDecimal("199.99"));
        dto.setStatus("Test status");
        dto.setShippingAddress("Test shippingAddress");
        dto.setOrderDate(java.time.LocalDateTime.now());
    }

    @Nested
    @DisplayName("GET Operations")
    class GetOperations {

        @Test
        @DisplayName("Should get all Order with pagination")
        @SuppressWarnings("unchecked")
        void shouldGetAllWithPagination() throws Exception {
            Page<Order> page = new PageImpl<>(new ArrayList<>(List.of(order)), PageRequest.of(0, 10), 1);
            when(service.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Result.success(page));
            when(mapper.toDTO(any(Order.class))).thenReturn(dto);

            mockMvc.perform(get("/api/v1/orders")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk());

            verify(service).findAll(any(Specification.class), any(Pageable.class));
        }

        @Test
        @DisplayName("Should get Order by ID")
        void shouldGetById() throws Exception {
            when(service.findById(1L)).thenReturn(Result.success(order));
            when(mapper.toDTO(order)).thenReturn(dto);

            mockMvc.perform(get("/api/v1/orders/1"))
                    .andExpect(status().isOk());

            verify(service).findById(1L);
        }

        @Test
        @DisplayName("Should check if Order exists")
        void shouldCheckExists() throws Exception {
            when(service.existsById(1L)).thenReturn(Result.success(true));

            mockMvc.perform(head("/api/v1/orders/1"))
                    .andExpect(status().isOk());

            verify(service).existsById(1L);
        }
    }

    @Nested
    @DisplayName("POST Operations")
    class PostOperations {

        @Test
        @DisplayName("Should create new Order")
        void shouldCreateNew() throws Exception {
            when(mapper.toEntity(any(OrderDTO.class))).thenReturn(order);
            when(service.save(any(Order.class))).thenReturn(Result.success(order));
            when(mapper.toDTO(any(Order.class))).thenReturn(dto);

            mockMvc.perform(post("/api/v1/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(dto)))
                    .andExpect(status().isCreated());

            verify(service).save(any(Order.class));
        }

        @Test
        @DisplayName("Should restore soft-deleted Order")
        void shouldRestore() throws Exception {
            when(service.restore(1L)).thenReturn(Result.success(order));
            when(mapper.toDTO(order)).thenReturn(dto);

            mockMvc.perform(post("/api/v1/orders/1/restore"))
                    .andExpect(status().isOk());

            verify(service).restore(1L);
        }
    }

    @Nested
    @DisplayName("PUT Operations")
    class PutOperations {

        @Test
        @DisplayName("Should update Order")
        void shouldUpdate() throws Exception {
            when(mapper.toEntity(any(OrderDTO.class))).thenReturn(order);
            when(service.update(anyLong(), any(Order.class))).thenReturn(Result.success(order));
            when(mapper.toDTO(any(Order.class))).thenReturn(dto);

            mockMvc.perform(put("/api/v1/orders/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk());

            verify(service).update(eq(1L), any(Order.class));
        }
    }

    @Nested
    @DisplayName("PATCH Operations")
    class PatchOperations {

        @Test
        @DisplayName("Should partial update Order")
        void shouldPartialUpdate() throws Exception {
            // Controller PATCH calls: findById -> updateEntityFromDTO -> save
            when(service.findById(1L)).thenReturn(Result.success(order));
            // updateEntityFromDTO is void, no need to mock return
            when(service.save(any(Order.class))).thenReturn(Result.success(order));
            when(mapper.toDTO(any(Order.class))).thenReturn(dto);

            mockMvc.perform(patch("/api/v1/orders/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk());

            verify(service).findById(1L);
            verify(service).save(any(Order.class));
        }
    }

    @Nested
    @DisplayName("DELETE Operations")
    class DeleteOperations {

        @Test
        @DisplayName("Should soft delete Order")
        void shouldSoftDelete() throws Exception {
            when(service.softDelete(1L)).thenReturn(Result.success(null));

            mockMvc.perform(delete("/api/v1/orders/1"))
                    .andExpect(status().isNoContent());

            verify(service).softDelete(1L);
        }

        @Test
        @DisplayName("Should hard delete Order with permanent flag")
        void shouldHardDelete() throws Exception {
            when(service.hardDelete(1L)).thenReturn(Result.success(null));

            mockMvc.perform(delete("/api/v1/orders/1")
                            .param("permanent", "true"))
                    .andExpect(status().isNoContent());

            verify(service).hardDelete(1L);
        }
    }
}
