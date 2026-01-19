package com.jnzader.apigen.server.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("GenerateResponse Tests")
class GenerateResponseTest {

    @Nested
    @DisplayName("Factory Methods")
    class FactoryMethodTests {

        @Test
        @DisplayName("success() should create success response with files and stats")
        void successShouldCreateSuccessResponse() {
            List<String> files = List.of("Entity.java", "Repository.java", "Service.java");
            GenerateResponse.GenerationStats stats =
                    GenerateResponse.GenerationStats.builder()
                            .tablesProcessed(3)
                            .entitiesGenerated(3)
                            .filesGenerated(15)
                            .generationTimeMs(500)
                            .build();

            GenerateResponse response =
                    GenerateResponse.success("Generation complete", files, stats);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getMessage()).isEqualTo("Generation complete");
            assertThat(response.getGeneratedFiles()).containsExactlyElementsOf(files);
            assertThat(response.getStats()).isNotNull();
            assertThat(response.getStats().getTablesProcessed()).isEqualTo(3);
            assertThat(response.getErrors()).isEmpty();
        }

        @Test
        @DisplayName("error() should create error response with errors list")
        void errorShouldCreateErrorResponse() {
            List<String> errors = List.of("Invalid SQL syntax", "Missing primary key");

            GenerateResponse response = GenerateResponse.error("Validation failed", errors);

            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getMessage()).isEqualTo("Validation failed");
            assertThat(response.getErrors()).containsExactlyElementsOf(errors);
            assertThat(response.getGeneratedFiles()).isEmpty();
            assertThat(response.getStats()).isNull();
        }
    }

    @Nested
    @DisplayName("Builder")
    class BuilderTests {

        @Test
        @DisplayName("Should use empty lists as defaults")
        void shouldUseEmptyListsAsDefaults() {
            GenerateResponse response =
                    GenerateResponse.builder().success(true).message("Test").build();

            assertThat(response.getGeneratedFiles()).isNotNull().isEmpty();
            assertThat(response.getWarnings()).isNotNull().isEmpty();
            assertThat(response.getErrors()).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("Should build response with all fields")
        void shouldBuildResponseWithAllFields() {
            GenerateResponse.GenerationStats stats =
                    GenerateResponse.GenerationStats.builder()
                            .tablesProcessed(5)
                            .entitiesGenerated(4)
                            .filesGenerated(20)
                            .generationTimeMs(1200)
                            .build();

            GenerateResponse response =
                    GenerateResponse.builder()
                            .success(true)
                            .message("Complete")
                            .generatedFiles(List.of("file1.java", "file2.java"))
                            .warnings(List.of("Deprecated feature used"))
                            .errors(List.of())
                            .stats(stats)
                            .build();

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getMessage()).isEqualTo("Complete");
            assertThat(response.getGeneratedFiles()).hasSize(2);
            assertThat(response.getWarnings()).hasSize(1);
            assertThat(response.getErrors()).isEmpty();
            assertThat(response.getStats().getFilesGenerated()).isEqualTo(20);
        }
    }

    @Nested
    @DisplayName("GenerationStats")
    class GenerationStatsTests {

        @Test
        @DisplayName("Should build stats with all fields")
        void shouldBuildStatsWithAllFields() {
            GenerateResponse.GenerationStats stats =
                    GenerateResponse.GenerationStats.builder()
                            .tablesProcessed(10)
                            .entitiesGenerated(8)
                            .filesGenerated(50)
                            .generationTimeMs(2500)
                            .build();

            assertThat(stats.getTablesProcessed()).isEqualTo(10);
            assertThat(stats.getEntitiesGenerated()).isEqualTo(8);
            assertThat(stats.getFilesGenerated()).isEqualTo(50);
            assertThat(stats.getGenerationTimeMs()).isEqualTo(2500);
        }

        @Test
        @DisplayName("Should have default values for unset fields")
        void shouldHaveDefaultValues() {
            GenerateResponse.GenerationStats stats = new GenerateResponse.GenerationStats();

            assertThat(stats.getTablesProcessed()).isZero();
            assertThat(stats.getEntitiesGenerated()).isZero();
            assertThat(stats.getFilesGenerated()).isZero();
            assertThat(stats.getGenerationTimeMs()).isZero();
        }
    }

    @Nested
    @DisplayName("Default Constructor")
    class DefaultConstructorTests {

        @Test
        @DisplayName("Should initialize lists to empty collections")
        void shouldInitializeListsToEmptyCollections() {
            GenerateResponse response = new GenerateResponse();

            assertThat(response.getGeneratedFiles()).isNotNull();
            assertThat(response.getWarnings()).isNotNull();
            assertThat(response.getErrors()).isNotNull();
        }
    }
}
