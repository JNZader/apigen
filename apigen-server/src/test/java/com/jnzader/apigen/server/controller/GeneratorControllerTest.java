package com.jnzader.apigen.server.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jnzader.apigen.server.dto.GenerateRequest;
import com.jnzader.apigen.server.dto.GenerateResponse;
import com.jnzader.apigen.server.exception.ProjectGenerationException;
import com.jnzader.apigen.server.service.GeneratorService;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
@DisplayName("GeneratorController Tests")
class GeneratorControllerTest {

    @Mock private GeneratorService generatorService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private GeneratorController controller;

    @BeforeEach
    void setUp() {
        controller = new GeneratorController(generatorService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();
    }

    @Nested
    @DisplayName("POST /api/generate")
    class GenerateEndpointTests {

        @Test
        @DisplayName("Should generate project and return ZIP file")
        void shouldGenerateProjectAndReturnZipFile() throws Exception {
            GenerateRequest request = createValidRequest();
            byte[] zipBytes = "mock-zip-content".getBytes(StandardCharsets.UTF_8);

            when(generatorService.generateProject(any(GenerateRequest.class))).thenReturn(zipBytes);

            mockMvc.perform(
                            post("/api/generate")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(
                            header().string(
                                            "Content-Disposition",
                                            "form-data; name=\"attachment\";"
                                                    + " filename=\"test-api.zip\""))
                    .andExpect(
                            header().string(
                                            "Content-Type",
                                            MediaType.APPLICATION_OCTET_STREAM_VALUE))
                    .andExpect(content().bytes(zipBytes));

            verify(generatorService).generateProject(any(GenerateRequest.class));
        }

        @Test
        @DisplayName("Should return correct filename based on artifact ID")
        void shouldReturnCorrectFilenameBasedOnArtifactId() throws Exception {
            GenerateRequest request = createValidRequest();
            request.getProject().setArtifactId("my-awesome-api");
            byte[] zipBytes = "mock-zip".getBytes(StandardCharsets.UTF_8);

            when(generatorService.generateProject(any(GenerateRequest.class))).thenReturn(zipBytes);

            mockMvc.perform(
                            post("/api/generate")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(
                            header().string(
                                            "Content-Disposition",
                                            "form-data; name=\"attachment\";"
                                                    + " filename=\"my-awesome-api.zip\""));
        }

        @Test
        @DisplayName("Should return Content-Length header")
        void shouldReturnContentLengthHeader() throws Exception {
            GenerateRequest request = createValidRequest();
            byte[] zipBytes = "mock-zip-content-with-known-length".getBytes(StandardCharsets.UTF_8);

            when(generatorService.generateProject(any(GenerateRequest.class))).thenReturn(zipBytes);

            mockMvc.perform(
                            post("/api/generate")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(header().longValue("Content-Length", zipBytes.length));
        }

        @Test
        @DisplayName("Should throw GenerationException on service error")
        void shouldThrowGenerationExceptionOnServiceError() throws Exception {
            GenerateRequest request = createValidRequest();

            when(generatorService.generateProject(any(GenerateRequest.class)))
                    .thenThrow(new IOException("Disk full"));

            mockMvc.perform(
                            post("/api/generate")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isInternalServerError());
        }
    }

    @Nested
    @DisplayName("POST /api/validate")
    class ValidateEndpointTests {

        @Test
        @DisplayName("Should return success response for valid SQL")
        void shouldReturnSuccessResponseForValidSql() throws Exception {
            GenerateRequest request = createValidRequest();
            GenerateResponse response =
                    GenerateResponse.builder()
                            .success(true)
                            .message("SQL schema is valid")
                            .stats(
                                    GenerateResponse.GenerationStats.builder()
                                            .tablesProcessed(1)
                                            .entitiesGenerated(1)
                                            .build())
                            .build();

            when(generatorService.validate(any(GenerateRequest.class))).thenReturn(response);

            mockMvc.perform(
                            post("/api/validate")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("SQL schema is valid"));

            verify(generatorService).validate(any(GenerateRequest.class));
        }

        @Test
        @DisplayName("Should return error response for invalid SQL")
        void shouldReturnErrorResponseForInvalidSql() throws Exception {
            GenerateRequest request = createValidRequest();
            request.setSql("INVALID SQL");
            GenerateResponse response =
                    GenerateResponse.error("Validation failed", java.util.List.of("Parse error"));

            when(generatorService.validate(any(GenerateRequest.class))).thenReturn(response);

            mockMvc.perform(
                            post("/api/validate")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors").isArray());
        }

        @Test
        @DisplayName("Should include stats in response")
        void shouldIncludeStatsInResponse() throws Exception {
            GenerateRequest request = createValidRequest();
            GenerateResponse response =
                    GenerateResponse.builder()
                            .success(true)
                            .message("Valid")
                            .stats(
                                    GenerateResponse.GenerationStats.builder()
                                            .tablesProcessed(5)
                                            .entitiesGenerated(3)
                                            .build())
                            .build();

            when(generatorService.validate(any(GenerateRequest.class))).thenReturn(response);

            mockMvc.perform(
                            post("/api/validate")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.stats.tablesProcessed").value(5))
                    .andExpect(jsonPath("$.stats.entitiesGenerated").value(3));
        }
    }

    @Nested
    @DisplayName("GET /api/health")
    class HealthEndpointTests {

        @Test
        @DisplayName("Should return ok status")
        void shouldReturnOkStatus() throws Exception {
            mockMvc.perform(get("/api/health"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("ok"))
                    .andExpect(jsonPath("$.message").value("APiGen Server is running"));
        }

        @Test
        @DisplayName("Should return JSON content type")
        void shouldReturnJsonContentType() throws Exception {
            mockMvc.perform(get("/api/health"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }
    }

    @Nested
    @DisplayName("HealthResponse")
    class HealthResponseTests {

        @Test
        @DisplayName("Should create HealthResponse with values")
        void shouldCreateHealthResponseWithValues() {
            GeneratorController.HealthResponse response =
                    new GeneratorController.HealthResponse("ok", "test message");

            org.assertj.core.api.Assertions.assertThat(response.status()).isEqualTo("ok");
            org.assertj.core.api.Assertions.assertThat(response.message())
                    .isEqualTo("test message");
        }
    }

    @Nested
    @DisplayName("ProjectGenerationException")
    class ProjectGenerationExceptionTests {

        @Test
        @DisplayName("Should create exception with message and cause")
        void shouldCreateExceptionWithMessageAndCause() {
            Throwable cause = new IOException("Original error");
            ProjectGenerationException exception =
                    new ProjectGenerationException("Generation failed", cause);

            org.assertj.core.api.Assertions.assertThat(exception.getMessage())
                    .isEqualTo("Generation failed");
            org.assertj.core.api.Assertions.assertThat(exception.getCause()).isEqualTo(cause);
        }

        @Test
        @DisplayName("Should be a RuntimeException")
        void shouldBeARuntimeException() {
            ProjectGenerationException exception =
                    new ProjectGenerationException("test", new Exception());

            org.assertj.core.api.Assertions.assertThat(exception)
                    .isInstanceOf(RuntimeException.class);
        }
    }

    private GenerateRequest createValidRequest() {
        return GenerateRequest.builder()
                .project(
                        GenerateRequest.ProjectConfig.builder()
                                .name("Test Project")
                                .groupId("com.example")
                                .artifactId("test-api")
                                .features(
                                        GenerateRequest.FeaturesConfig.builder()
                                                .docker(true)
                                                .swagger(true)
                                                .hateoas(true)
                                                .auditing(true)
                                                .softDelete(true)
                                                .caching(true)
                                                .build())
                                .database(
                                        GenerateRequest.DatabaseConfig.builder()
                                                .type("postgresql")
                                                .name("testdb")
                                                .username("testuser")
                                                .password("testpass")
                                                .build())
                                .build())
                .sql(
                        """
                        CREATE TABLE products (
                            id BIGINT PRIMARY KEY,
                            name VARCHAR(255) NOT NULL
                        );
                        """)
                .build();
    }
}
