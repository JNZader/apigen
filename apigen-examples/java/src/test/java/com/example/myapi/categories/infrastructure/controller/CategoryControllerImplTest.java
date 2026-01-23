package com.example.myapi.categories.infrastructure.controller;

import com.example.myapi.categories.application.dto.CategoryDTO;
import com.example.myapi.categories.application.mapper.CategoryMapper;
import com.example.myapi.categories.application.service.CategoryService;
import com.example.myapi.categories.domain.entity.Category;
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
@DisplayName("CategoryController Tests")
class CategoryControllerImplTest {

    @Mock
    private CategoryService service;

    @Mock
    private CategoryMapper mapper;

    private MockMvc mockMvc;
    private JsonMapper jsonMapper;
    private CategoryControllerImpl controller;

    private Category category;
    private CategoryDTO dto;

    @BeforeEach
    void setUp() {
        controller = new CategoryControllerImpl(service, mapper);
        jsonMapper = JsonMapper.builder().build();
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setMessageConverters(new JacksonJsonHttpMessageConverter(jsonMapper))
                .build();

        category = new Category();
        category.setId(1L);
        category.setEstado(true);
        category.setName("Test name");
        category.setDescription("Test description");
        category.setSlug("Test slug");

        dto = new CategoryDTO();
        dto.setId(1L);
        dto.setActivo(true);
        dto.setName("Test name");
        dto.setDescription("Test description");
        dto.setSlug("Test slug");
    }

    @Nested
    @DisplayName("GET Operations")
    class GetOperations {

        @Test
        @DisplayName("Should get all Category with pagination")
        @SuppressWarnings("unchecked")
        void shouldGetAllWithPagination() throws Exception {
            Page<Category> page = new PageImpl<>(new ArrayList<>(List.of(category)), PageRequest.of(0, 10), 1);
            when(service.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Result.success(page));
            when(mapper.toDTO(any(Category.class))).thenReturn(dto);

            mockMvc.perform(get("/api/v1/categories")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk());

            verify(service).findAll(any(Specification.class), any(Pageable.class));
        }

        @Test
        @DisplayName("Should get Category by ID")
        void shouldGetById() throws Exception {
            when(service.findById(1L)).thenReturn(Result.success(category));
            when(mapper.toDTO(category)).thenReturn(dto);

            mockMvc.perform(get("/api/v1/categories/1"))
                    .andExpect(status().isOk());

            verify(service).findById(1L);
        }

        @Test
        @DisplayName("Should check if Category exists")
        void shouldCheckExists() throws Exception {
            when(service.existsById(1L)).thenReturn(Result.success(true));

            mockMvc.perform(head("/api/v1/categories/1"))
                    .andExpect(status().isOk());

            verify(service).existsById(1L);
        }
    }

    @Nested
    @DisplayName("POST Operations")
    class PostOperations {

        @Test
        @DisplayName("Should create new Category")
        void shouldCreateNew() throws Exception {
            when(mapper.toEntity(any(CategoryDTO.class))).thenReturn(category);
            when(service.save(any(Category.class))).thenReturn(Result.success(category));
            when(mapper.toDTO(any(Category.class))).thenReturn(dto);

            mockMvc.perform(post("/api/v1/categories")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(dto)))
                    .andExpect(status().isCreated());

            verify(service).save(any(Category.class));
        }

        @Test
        @DisplayName("Should restore soft-deleted Category")
        void shouldRestore() throws Exception {
            when(service.restore(1L)).thenReturn(Result.success(category));
            when(mapper.toDTO(category)).thenReturn(dto);

            mockMvc.perform(post("/api/v1/categories/1/restore"))
                    .andExpect(status().isOk());

            verify(service).restore(1L);
        }
    }

    @Nested
    @DisplayName("PUT Operations")
    class PutOperations {

        @Test
        @DisplayName("Should update Category")
        void shouldUpdate() throws Exception {
            when(mapper.toEntity(any(CategoryDTO.class))).thenReturn(category);
            when(service.update(anyLong(), any(Category.class))).thenReturn(Result.success(category));
            when(mapper.toDTO(any(Category.class))).thenReturn(dto);

            mockMvc.perform(put("/api/v1/categories/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk());

            verify(service).update(eq(1L), any(Category.class));
        }
    }

    @Nested
    @DisplayName("PATCH Operations")
    class PatchOperations {

        @Test
        @DisplayName("Should partial update Category")
        void shouldPartialUpdate() throws Exception {
            // Controller PATCH calls: findById -> updateEntityFromDTO -> save
            when(service.findById(1L)).thenReturn(Result.success(category));
            // updateEntityFromDTO is void, no need to mock return
            when(service.save(any(Category.class))).thenReturn(Result.success(category));
            when(mapper.toDTO(any(Category.class))).thenReturn(dto);

            mockMvc.perform(patch("/api/v1/categories/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk());

            verify(service).findById(1L);
            verify(service).save(any(Category.class));
        }
    }

    @Nested
    @DisplayName("DELETE Operations")
    class DeleteOperations {

        @Test
        @DisplayName("Should soft delete Category")
        void shouldSoftDelete() throws Exception {
            when(service.softDelete(1L)).thenReturn(Result.success(null));

            mockMvc.perform(delete("/api/v1/categories/1"))
                    .andExpect(status().isNoContent());

            verify(service).softDelete(1L);
        }

        @Test
        @DisplayName("Should hard delete Category with permanent flag")
        void shouldHardDelete() throws Exception {
            when(service.hardDelete(1L)).thenReturn(Result.success(null));

            mockMvc.perform(delete("/api/v1/categories/1")
                            .param("permanent", "true"))
                    .andExpect(status().isNoContent());

            verify(service).hardDelete(1L);
        }
    }
}
