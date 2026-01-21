package com.jnzader.apigen.codegen.generator.test;

import com.jnzader.apigen.codegen.model.SqlTable;

/** Generates unit test classes for Controller implementations. */
public class ControllerTestGenerator {

    private static final String APIGEN_CORE_PKG = "com.jnzader.apigen.core";

    private final String basePackage;

    public ControllerTestGenerator(String basePackage) {
        this.basePackage = basePackage;
    }

    /** Generates the Controller test class code. */
    public String generate(SqlTable table) {
        String entityName = table.getEntityName();
        String entityVarName = table.getEntityVariableName();
        String moduleName = table.getModuleName();

        return
"""
package %s.%s.infrastructure.controller;

import %s.%s.application.dto.%sDTO;
import %s.%s.application.mapper.%sMapper;
import %s.%s.application.service.%sService;
import %s.%s.domain.entity.%s;
import %s.application.util.Result;
import tools.jackson.databind.ObjectMapper;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("%sController Tests")
class %sControllerImplTest {

    @Mock
    private %sService service;

    @Mock
    private %sMapper mapper;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private %sControllerImpl controller;

    private %s %s;
    private %sDTO dto;

    @BeforeEach
    void setUp() {
        controller = new %sControllerImpl(service, mapper);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();

        %s = new %s();
        %s.setId(1L);
        %s.setEstado(true);

        dto = new %sDTO();
        dto.setId(1L);
        dto.setActivo(true);
    }

    @Nested
    @DisplayName("GET Operations")
    class GetOperations {

        @Test
        @DisplayName("Should get all %s with pagination")
        @SuppressWarnings("unchecked")
        void shouldGetAllWithPagination() throws Exception {
            Page<%s> page = new PageImpl<>(List.of(%s));
            when(service.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Result.success(page));
            when(mapper.toDTO(any(%s.class))).thenReturn(dto);

            mockMvc.perform(get("/api/v1/%s")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk());

            verify(service).findAll(any(Specification.class), any(Pageable.class));
        }

        @Test
        @DisplayName("Should get %s by ID")
        void shouldGetById() throws Exception {
            when(service.findById(1L)).thenReturn(Result.success(%s));
            when(mapper.toDTO(%s)).thenReturn(dto);

            mockMvc.perform(get("/api/v1/%s/1"))
                    .andExpect(status().isOk());

            verify(service).findById(1L);
        }

        @Test
        @DisplayName("Should check if %s exists")
        void shouldCheckExists() throws Exception {
            when(service.existsById(1L)).thenReturn(Result.success(true));

            mockMvc.perform(head("/api/v1/%s/1"))
                    .andExpect(status().isOk());

            verify(service).existsById(1L);
        }
    }

    @Nested
    @DisplayName("POST Operations")
    class PostOperations {

        @Test
        @DisplayName("Should create new %s")
        void shouldCreateNew() throws Exception {
            when(mapper.toEntity(any(%sDTO.class))).thenReturn(%s);
            when(service.save(any(%s.class))).thenReturn(Result.success(%s));
            when(mapper.toDTO(any(%s.class))).thenReturn(dto);

            mockMvc.perform(post("/api/v1/%s")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isCreated());

            verify(service).save(any(%s.class));
        }

        @Test
        @DisplayName("Should restore soft-deleted %s")
        void shouldRestore() throws Exception {
            when(service.restore(1L)).thenReturn(Result.success(%s));
            when(mapper.toDTO(%s)).thenReturn(dto);

            mockMvc.perform(post("/api/v1/%s/1/restore"))
                    .andExpect(status().isOk());

            verify(service).restore(1L);
        }
    }

    @Nested
    @DisplayName("PUT Operations")
    class PutOperations {

        @Test
        @DisplayName("Should update %s")
        void shouldUpdate() throws Exception {
            when(mapper.toEntity(any(%sDTO.class))).thenReturn(%s);
            when(service.update(anyLong(), any(%s.class))).thenReturn(Result.success(%s));
            when(mapper.toDTO(any(%s.class))).thenReturn(dto);

            mockMvc.perform(put("/api/v1/%s/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk());

            verify(service).update(eq(1L), any(%s.class));
        }
    }

    @Nested
    @DisplayName("PATCH Operations")
    class PatchOperations {

        @Test
        @DisplayName("Should partial update %s")
        void shouldPartialUpdate() throws Exception {
            when(mapper.toEntity(any(%sDTO.class))).thenReturn(%s);
            when(service.partialUpdate(anyLong(), any(%s.class))).thenReturn(Result.success(%s));
            when(mapper.toDTO(any(%s.class))).thenReturn(dto);

            mockMvc.perform(patch("/api/v1/%s/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk());

            verify(service).partialUpdate(eq(1L), any(%s.class));
        }
    }

    @Nested
    @DisplayName("DELETE Operations")
    class DeleteOperations {

        @Test
        @DisplayName("Should soft delete %s")
        void shouldSoftDelete() throws Exception {
            when(service.softDelete(1L)).thenReturn(Result.success(null));

            mockMvc.perform(delete("/api/v1/%s/1"))
                    .andExpect(status().isNoContent());

            verify(service).softDelete(1L);
        }

        @Test
        @DisplayName("Should hard delete %s with permanent flag")
        void shouldHardDelete() throws Exception {
            when(service.hardDelete(1L)).thenReturn(Result.success(null));

            mockMvc.perform(delete("/api/v1/%s/1")
                            .param("permanent", "true"))
                    .andExpect(status().isNoContent());

            verify(service).hardDelete(1L);
        }
    }
}
"""
                .formatted(
                        basePackage,
                        moduleName,
                        basePackage,
                        moduleName,
                        entityName,
                        basePackage,
                        moduleName,
                        entityName,
                        basePackage,
                        moduleName,
                        entityName,
                        basePackage,
                        moduleName,
                        entityName,
                        APIGEN_CORE_PKG,
                        entityName,
                        entityName,
                        entityName,
                        entityName,
                        entityName,
                        entityName,
                        entityVarName,
                        entityName,
                        entityName,
                        entityVarName,
                        entityName,
                        entityVarName,
                        entityVarName,
                        entityName,
                        // GET Operations
                        entityName,
                        entityName,
                        entityVarName,
                        entityName,
                        moduleName,
                        entityName,
                        entityVarName,
                        entityVarName,
                        moduleName,
                        entityName,
                        moduleName,
                        // POST Operations
                        entityName,
                        entityName,
                        entityVarName,
                        entityName,
                        entityVarName,
                        entityName,
                        moduleName,
                        entityName,
                        entityName,
                        entityVarName,
                        entityVarName,
                        moduleName,
                        // PUT Operations
                        entityName,
                        entityName,
                        entityVarName,
                        entityName,
                        entityVarName,
                        entityName,
                        moduleName,
                        entityName,
                        // PATCH Operations
                        entityName,
                        entityName,
                        entityVarName,
                        entityName,
                        entityVarName,
                        entityName,
                        moduleName,
                        entityName,
                        // DELETE Operations
                        entityName,
                        moduleName,
                        entityName,
                        moduleName);
    }
}
