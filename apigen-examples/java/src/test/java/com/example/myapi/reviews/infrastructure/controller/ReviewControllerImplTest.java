package com.example.myapi.reviews.infrastructure.controller;

import com.example.myapi.reviews.application.dto.ReviewDTO;
import com.example.myapi.reviews.application.mapper.ReviewMapper;
import com.example.myapi.reviews.application.service.ReviewService;
import com.example.myapi.reviews.domain.entity.Review;
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
@DisplayName("ReviewController Tests")
class ReviewControllerImplTest {

    @Mock
    private ReviewService service;

    @Mock
    private ReviewMapper mapper;

    private MockMvc mockMvc;
    private JsonMapper jsonMapper;
    private ReviewControllerImpl controller;

    private Review review;
    private ReviewDTO dto;

    @BeforeEach
    void setUp() {
        controller = new ReviewControllerImpl(service, mapper);
        jsonMapper = JsonMapper.builder().build();
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setMessageConverters(new JacksonJsonHttpMessageConverter(jsonMapper))
                .build();

        review = new Review();
        review.setId(1L);
        review.setEstado(true);
        review.setRating(100);
        review.setComment("Test comment");
        review.setReviewDate(java.time.LocalDateTime.now());

        dto = new ReviewDTO();
        dto.setId(1L);
        dto.setActivo(true);
        dto.setRating(100);
        dto.setComment("Test comment");
        dto.setReviewDate(java.time.LocalDateTime.now());
    }

    @Nested
    @DisplayName("GET Operations")
    class GetOperations {

        @Test
        @DisplayName("Should get all Review with pagination")
        @SuppressWarnings("unchecked")
        void shouldGetAllWithPagination() throws Exception {
            Page<Review> page = new PageImpl<>(new ArrayList<>(List.of(review)), PageRequest.of(0, 10), 1);
            when(service.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Result.success(page));
            when(mapper.toDTO(any(Review.class))).thenReturn(dto);

            mockMvc.perform(get("/api/v1/reviews")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk());

            verify(service).findAll(any(Specification.class), any(Pageable.class));
        }

        @Test
        @DisplayName("Should get Review by ID")
        void shouldGetById() throws Exception {
            when(service.findById(1L)).thenReturn(Result.success(review));
            when(mapper.toDTO(review)).thenReturn(dto);

            mockMvc.perform(get("/api/v1/reviews/1"))
                    .andExpect(status().isOk());

            verify(service).findById(1L);
        }

        @Test
        @DisplayName("Should check if Review exists")
        void shouldCheckExists() throws Exception {
            when(service.existsById(1L)).thenReturn(Result.success(true));

            mockMvc.perform(head("/api/v1/reviews/1"))
                    .andExpect(status().isOk());

            verify(service).existsById(1L);
        }
    }

    @Nested
    @DisplayName("POST Operations")
    class PostOperations {

        @Test
        @DisplayName("Should create new Review")
        void shouldCreateNew() throws Exception {
            when(mapper.toEntity(any(ReviewDTO.class))).thenReturn(review);
            when(service.save(any(Review.class))).thenReturn(Result.success(review));
            when(mapper.toDTO(any(Review.class))).thenReturn(dto);

            mockMvc.perform(post("/api/v1/reviews")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(dto)))
                    .andExpect(status().isCreated());

            verify(service).save(any(Review.class));
        }

        @Test
        @DisplayName("Should restore soft-deleted Review")
        void shouldRestore() throws Exception {
            when(service.restore(1L)).thenReturn(Result.success(review));
            when(mapper.toDTO(review)).thenReturn(dto);

            mockMvc.perform(post("/api/v1/reviews/1/restore"))
                    .andExpect(status().isOk());

            verify(service).restore(1L);
        }
    }

    @Nested
    @DisplayName("PUT Operations")
    class PutOperations {

        @Test
        @DisplayName("Should update Review")
        void shouldUpdate() throws Exception {
            when(mapper.toEntity(any(ReviewDTO.class))).thenReturn(review);
            when(service.update(anyLong(), any(Review.class))).thenReturn(Result.success(review));
            when(mapper.toDTO(any(Review.class))).thenReturn(dto);

            mockMvc.perform(put("/api/v1/reviews/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk());

            verify(service).update(eq(1L), any(Review.class));
        }
    }

    @Nested
    @DisplayName("PATCH Operations")
    class PatchOperations {

        @Test
        @DisplayName("Should partial update Review")
        void shouldPartialUpdate() throws Exception {
            // Controller PATCH calls: findById -> updateEntityFromDTO -> save
            when(service.findById(1L)).thenReturn(Result.success(review));
            // updateEntityFromDTO is void, no need to mock return
            when(service.save(any(Review.class))).thenReturn(Result.success(review));
            when(mapper.toDTO(any(Review.class))).thenReturn(dto);

            mockMvc.perform(patch("/api/v1/reviews/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk());

            verify(service).findById(1L);
            verify(service).save(any(Review.class));
        }
    }

    @Nested
    @DisplayName("DELETE Operations")
    class DeleteOperations {

        @Test
        @DisplayName("Should soft delete Review")
        void shouldSoftDelete() throws Exception {
            when(service.softDelete(1L)).thenReturn(Result.success(null));

            mockMvc.perform(delete("/api/v1/reviews/1"))
                    .andExpect(status().isNoContent());

            verify(service).softDelete(1L);
        }

        @Test
        @DisplayName("Should hard delete Review with permanent flag")
        void shouldHardDelete() throws Exception {
            when(service.hardDelete(1L)).thenReturn(Result.success(null));

            mockMvc.perform(delete("/api/v1/reviews/1")
                            .param("permanent", "true"))
                    .andExpect(status().isNoContent());

            verify(service).hardDelete(1L);
        }
    }
}
