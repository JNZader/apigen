package com.example.myapi.products.infrastructure.controller;

import com.example.myapi.products.application.dto.ProductDTO;
import com.example.myapi.products.application.mapper.ProductMapper;
import com.example.myapi.products.application.service.ProductService;
import com.example.myapi.products.domain.entity.Product;
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
@DisplayName("ProductController Tests")
class ProductControllerImplTest {

    @Mock
    private ProductService service;

    @Mock
    private ProductMapper mapper;

    private MockMvc mockMvc;
    private JsonMapper jsonMapper;
    private ProductControllerImpl controller;

    private Product product;
    private ProductDTO dto;

    @BeforeEach
    void setUp() {
        controller = new ProductControllerImpl(service, mapper);
        jsonMapper = JsonMapper.builder().build();
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setMessageConverters(new JacksonJsonHttpMessageConverter(jsonMapper))
                .build();

        product = new Product();
        product.setId(1L);
        product.setEstado(true);
        product.setName("Test name");
        product.setDescription("Test description");
        product.setPrice(new java.math.BigDecimal("199.99"));
        product.setStock(100);
        product.setSku("Test sku");
        product.setImageUrl("Test imageUrl");

        dto = new ProductDTO();
        dto.setId(1L);
        dto.setActivo(true);
        dto.setName("Test name");
        dto.setDescription("Test description");
        dto.setPrice(new java.math.BigDecimal("199.99"));
        dto.setStock(100);
        dto.setSku("Test sku");
        dto.setImageUrl("Test imageUrl");
    }

    @Nested
    @DisplayName("GET Operations")
    class GetOperations {

        @Test
        @DisplayName("Should get all Product with pagination")
        @SuppressWarnings("unchecked")
        void shouldGetAllWithPagination() throws Exception {
            Page<Product> page = new PageImpl<>(new ArrayList<>(List.of(product)), PageRequest.of(0, 10), 1);
            when(service.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Result.success(page));
            when(mapper.toDTO(any(Product.class))).thenReturn(dto);

            mockMvc.perform(get("/api/v1/products")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk());

            verify(service).findAll(any(Specification.class), any(Pageable.class));
        }

        @Test
        @DisplayName("Should get Product by ID")
        void shouldGetById() throws Exception {
            when(service.findById(1L)).thenReturn(Result.success(product));
            when(mapper.toDTO(product)).thenReturn(dto);

            mockMvc.perform(get("/api/v1/products/1"))
                    .andExpect(status().isOk());

            verify(service).findById(1L);
        }

        @Test
        @DisplayName("Should check if Product exists")
        void shouldCheckExists() throws Exception {
            when(service.existsById(1L)).thenReturn(Result.success(true));

            mockMvc.perform(head("/api/v1/products/1"))
                    .andExpect(status().isOk());

            verify(service).existsById(1L);
        }
    }

    @Nested
    @DisplayName("POST Operations")
    class PostOperations {

        @Test
        @DisplayName("Should create new Product")
        void shouldCreateNew() throws Exception {
            when(mapper.toEntity(any(ProductDTO.class))).thenReturn(product);
            when(service.save(any(Product.class))).thenReturn(Result.success(product));
            when(mapper.toDTO(any(Product.class))).thenReturn(dto);

            mockMvc.perform(post("/api/v1/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(dto)))
                    .andExpect(status().isCreated());

            verify(service).save(any(Product.class));
        }

        @Test
        @DisplayName("Should restore soft-deleted Product")
        void shouldRestore() throws Exception {
            when(service.restore(1L)).thenReturn(Result.success(product));
            when(mapper.toDTO(product)).thenReturn(dto);

            mockMvc.perform(post("/api/v1/products/1/restore"))
                    .andExpect(status().isOk());

            verify(service).restore(1L);
        }
    }

    @Nested
    @DisplayName("PUT Operations")
    class PutOperations {

        @Test
        @DisplayName("Should update Product")
        void shouldUpdate() throws Exception {
            when(mapper.toEntity(any(ProductDTO.class))).thenReturn(product);
            when(service.update(anyLong(), any(Product.class))).thenReturn(Result.success(product));
            when(mapper.toDTO(any(Product.class))).thenReturn(dto);

            mockMvc.perform(put("/api/v1/products/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk());

            verify(service).update(eq(1L), any(Product.class));
        }
    }

    @Nested
    @DisplayName("PATCH Operations")
    class PatchOperations {

        @Test
        @DisplayName("Should partial update Product")
        void shouldPartialUpdate() throws Exception {
            // Controller PATCH calls: findById -> updateEntityFromDTO -> save
            when(service.findById(1L)).thenReturn(Result.success(product));
            // updateEntityFromDTO is void, no need to mock return
            when(service.save(any(Product.class))).thenReturn(Result.success(product));
            when(mapper.toDTO(any(Product.class))).thenReturn(dto);

            mockMvc.perform(patch("/api/v1/products/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk());

            verify(service).findById(1L);
            verify(service).save(any(Product.class));
        }
    }

    @Nested
    @DisplayName("DELETE Operations")
    class DeleteOperations {

        @Test
        @DisplayName("Should soft delete Product")
        void shouldSoftDelete() throws Exception {
            when(service.softDelete(1L)).thenReturn(Result.success(null));

            mockMvc.perform(delete("/api/v1/products/1"))
                    .andExpect(status().isNoContent());

            verify(service).softDelete(1L);
        }

        @Test
        @DisplayName("Should hard delete Product with permanent flag")
        void shouldHardDelete() throws Exception {
            when(service.hardDelete(1L)).thenReturn(Result.success(null));

            mockMvc.perform(delete("/api/v1/products/1")
                            .param("permanent", "true"))
                    .andExpect(status().isNoContent());

            verify(service).hardDelete(1L);
        }
    }
}
