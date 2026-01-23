package com.example.myapi.orderitems.infrastructure.controller;

import com.example.myapi.orderitems.application.dto.OrderItemDTO;
import com.example.myapi.orderitems.application.mapper.OrderItemMapper;
import com.example.myapi.orderitems.application.service.OrderItemService;
import com.example.myapi.orderitems.domain.entity.OrderItem;
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
@DisplayName("OrderItemController Tests")
class OrderItemControllerImplTest {

    @Mock
    private OrderItemService service;

    @Mock
    private OrderItemMapper mapper;

    private MockMvc mockMvc;
    private JsonMapper jsonMapper;
    private OrderItemControllerImpl controller;

    private OrderItem orderItem;
    private OrderItemDTO dto;

    @BeforeEach
    void setUp() {
        controller = new OrderItemControllerImpl(service, mapper);
        jsonMapper = JsonMapper.builder().build();
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setMessageConverters(new JacksonJsonHttpMessageConverter(jsonMapper))
                .build();

        orderItem = new OrderItem();
        orderItem.setId(1L);
        orderItem.setEstado(true);
        orderItem.setQuantity(100);
        orderItem.setUnitPrice(new java.math.BigDecimal("199.99"));

        dto = new OrderItemDTO();
        dto.setId(1L);
        dto.setActivo(true);
        dto.setQuantity(100);
        dto.setUnitPrice(new java.math.BigDecimal("199.99"));
    }

    @Nested
    @DisplayName("GET Operations")
    class GetOperations {

        @Test
        @DisplayName("Should get all OrderItem with pagination")
        @SuppressWarnings("unchecked")
        void shouldGetAllWithPagination() throws Exception {
            Page<OrderItem> page = new PageImpl<>(new ArrayList<>(List.of(orderItem)), PageRequest.of(0, 10), 1);
            when(service.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Result.success(page));
            when(mapper.toDTO(any(OrderItem.class))).thenReturn(dto);

            mockMvc.perform(get("/api/v1/orderitems")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk());

            verify(service).findAll(any(Specification.class), any(Pageable.class));
        }

        @Test
        @DisplayName("Should get OrderItem by ID")
        void shouldGetById() throws Exception {
            when(service.findById(1L)).thenReturn(Result.success(orderItem));
            when(mapper.toDTO(orderItem)).thenReturn(dto);

            mockMvc.perform(get("/api/v1/orderitems/1"))
                    .andExpect(status().isOk());

            verify(service).findById(1L);
        }

        @Test
        @DisplayName("Should check if OrderItem exists")
        void shouldCheckExists() throws Exception {
            when(service.existsById(1L)).thenReturn(Result.success(true));

            mockMvc.perform(head("/api/v1/orderitems/1"))
                    .andExpect(status().isOk());

            verify(service).existsById(1L);
        }
    }

    @Nested
    @DisplayName("POST Operations")
    class PostOperations {

        @Test
        @DisplayName("Should create new OrderItem")
        void shouldCreateNew() throws Exception {
            when(mapper.toEntity(any(OrderItemDTO.class))).thenReturn(orderItem);
            when(service.save(any(OrderItem.class))).thenReturn(Result.success(orderItem));
            when(mapper.toDTO(any(OrderItem.class))).thenReturn(dto);

            mockMvc.perform(post("/api/v1/orderitems")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(dto)))
                    .andExpect(status().isCreated());

            verify(service).save(any(OrderItem.class));
        }

        @Test
        @DisplayName("Should restore soft-deleted OrderItem")
        void shouldRestore() throws Exception {
            when(service.restore(1L)).thenReturn(Result.success(orderItem));
            when(mapper.toDTO(orderItem)).thenReturn(dto);

            mockMvc.perform(post("/api/v1/orderitems/1/restore"))
                    .andExpect(status().isOk());

            verify(service).restore(1L);
        }
    }

    @Nested
    @DisplayName("PUT Operations")
    class PutOperations {

        @Test
        @DisplayName("Should update OrderItem")
        void shouldUpdate() throws Exception {
            when(mapper.toEntity(any(OrderItemDTO.class))).thenReturn(orderItem);
            when(service.update(anyLong(), any(OrderItem.class))).thenReturn(Result.success(orderItem));
            when(mapper.toDTO(any(OrderItem.class))).thenReturn(dto);

            mockMvc.perform(put("/api/v1/orderitems/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk());

            verify(service).update(eq(1L), any(OrderItem.class));
        }
    }

    @Nested
    @DisplayName("PATCH Operations")
    class PatchOperations {

        @Test
        @DisplayName("Should partial update OrderItem")
        void shouldPartialUpdate() throws Exception {
            // Controller PATCH calls: findById -> updateEntityFromDTO -> save
            when(service.findById(1L)).thenReturn(Result.success(orderItem));
            // updateEntityFromDTO is void, no need to mock return
            when(service.save(any(OrderItem.class))).thenReturn(Result.success(orderItem));
            when(mapper.toDTO(any(OrderItem.class))).thenReturn(dto);

            mockMvc.perform(patch("/api/v1/orderitems/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk());

            verify(service).findById(1L);
            verify(service).save(any(OrderItem.class));
        }
    }

    @Nested
    @DisplayName("DELETE Operations")
    class DeleteOperations {

        @Test
        @DisplayName("Should soft delete OrderItem")
        void shouldSoftDelete() throws Exception {
            when(service.softDelete(1L)).thenReturn(Result.success(null));

            mockMvc.perform(delete("/api/v1/orderitems/1"))
                    .andExpect(status().isNoContent());

            verify(service).softDelete(1L);
        }

        @Test
        @DisplayName("Should hard delete OrderItem with permanent flag")
        void shouldHardDelete() throws Exception {
            when(service.hardDelete(1L)).thenReturn(Result.success(null));

            mockMvc.perform(delete("/api/v1/orderitems/1")
                            .param("permanent", "true"))
                    .andExpect(status().isNoContent());

            verify(service).hardDelete(1L);
        }
    }
}
