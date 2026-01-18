package com.jnzader.apigen.core.infrastructure.hateoas;

import com.jnzader.apigen.core.application.dto.BaseDTO;
import com.jnzader.apigen.core.infrastructure.controller.BaseController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedModel;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("BaseResourceAssembler Tests")
class BaseResourceAssemblerTest {

    private TestResourceAssembler assembler;

    @BeforeEach
    void setUp() {
        assembler = new TestResourceAssembler();
    }

    @Nested
    @DisplayName("extractId")
    class ExtractIdTests {

        @Test
        @DisplayName("should extract ID from DTO")
        void shouldExtractIdFromDto() {
            TestDTO dto = new TestDTO(42L, true);

            Long id = assembler.extractIdPublic(dto);

            assertThat(id).isEqualTo(42L);
        }

        @Test
        @DisplayName("should throw exception for null ID")
        void shouldThrowExceptionForNullId() {
            TestDTO dto = new TestDTO(null, true);

            assertThatThrownBy(() -> assembler.extractIdPublic(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("ID válido");
        }
    }

    @Nested
    @DisplayName("buildPageUrl")
    class BuildPageUrlTests {

        @Test
        @DisplayName("should build page URL with page and size params")
        void shouldBuildPageUrlWithPageAndSizeParams() {
            String url = assembler.buildPageUrlPublic(2, 20);

            assertThat(url).isEqualTo("?page=2&size=20");
        }

        @Test
        @DisplayName("should build page URL for first page")
        void shouldBuildPageUrlForFirstPage() {
            String url = assembler.buildPageUrlPublic(0, 10);

            assertThat(url).isEqualTo("?page=0&size=10");
        }

        @Test
        @DisplayName("should build page URL with zero size")
        void shouldBuildPageUrlWithZeroSize() {
            String url = assembler.buildPageUrlPublic(0, 0);

            assertThat(url).isEqualTo("?page=0&size=0");
        }

        @Test
        @DisplayName("should build page URL for large page numbers")
        void shouldBuildPageUrlForLargePageNumbers() {
            String url = assembler.buildPageUrlPublic(999, 100);

            assertThat(url).isEqualTo("?page=999&size=100");
        }
    }

    @Nested
    @DisplayName("addCustomLinks")
    class AddCustomLinksTests {

        @Test
        @DisplayName("should allow custom links to be added via override")
        void shouldAllowCustomLinksViaOverride() {
            TestDTO dto = new TestDTO(1L, true);
            EntityModel<TestDTO> model = EntityModel.of(dto);

            // Default implementation does nothing
            assembler.addCustomLinksPublic(model, dto);

            // No exception should be thrown
            assertThat(model.getContent()).isEqualTo(dto);
        }

        @Test
        @DisplayName("should allow subclass to add custom links")
        void shouldAllowSubclassToAddCustomLinks() {
            TestResourceAssemblerWithCustomLinks customAssembler = new TestResourceAssemblerWithCustomLinks();
            TestDTO dto = new TestDTO(1L, true);
            EntityModel<TestDTO> model = EntityModel.of(dto);

            customAssembler.addCustomLinksPublic(model, dto);

            assertThat(model.getLinks()).extracting(Link::getRel)
                    .anyMatch(rel -> rel.value().equals("custom"));
        }
    }

    /**
     * Tests for page navigation link URL building.
     * These test the buildPageUrl method which is used by toPagedModel for navigation links.
     */
    @Nested
    @DisplayName("Page Navigation URL Building")
    class PageNavigationTests {

        @Test
        @DisplayName("should build correct next page URL")
        void shouldBuildCorrectNextPageUrl() {
            String nextUrl = assembler.buildPageUrlPublic(1, 10);
            assertThat(nextUrl).isEqualTo("?page=1&size=10");
        }

        @Test
        @DisplayName("should build correct prev page URL")
        void shouldBuildCorrectPrevPageUrl() {
            String prevUrl = assembler.buildPageUrlPublic(0, 10);
            assertThat(prevUrl).isEqualTo("?page=0&size=10");
        }

        @Test
        @DisplayName("should build correct last page URL")
        void shouldBuildCorrectLastPageUrl() {
            // Last page for 50 items with size 10 is page 4 (0-indexed)
            String lastUrl = assembler.buildPageUrlPublic(4, 10);
            assertThat(lastUrl).isEqualTo("?page=4&size=10");
        }

        @Test
        @DisplayName("should build correct first page URL")
        void shouldBuildCorrectFirstPageUrl() {
            String firstUrl = assembler.buildPageUrlPublic(0, 20);
            assertThat(firstUrl).isEqualTo("?page=0&size=20");
        }
    }

    /**
     * Tests for PagedModel.PageMetadata creation logic.
     * Note: Full toPagedModel tests require Spring HATEOAS web context
     * and are covered in integration tests.
     */
    @Nested
    @DisplayName("Page Metadata Creation")
    class PageMetadataTests {

        @Test
        @DisplayName("should create correct metadata for empty page")
        void shouldCreateCorrectMetadataForEmptyPage() {
            PagedModel.PageMetadata metadata = new PagedModel.PageMetadata(10, 0, 0, 0);

            assertThat(metadata.getNumber()).isZero();
            assertThat(metadata.getSize()).isEqualTo(10);
            assertThat(metadata.getTotalElements()).isZero();
            assertThat(metadata.getTotalPages()).isZero();
        }

        @Test
        @DisplayName("should create correct metadata for single page")
        void shouldCreateCorrectMetadataForSinglePage() {
            PagedModel.PageMetadata metadata = new PagedModel.PageMetadata(10, 0, 5, 1);

            assertThat(metadata.getNumber()).isZero();
            assertThat(metadata.getSize()).isEqualTo(10);
            assertThat(metadata.getTotalElements()).isEqualTo(5);
            assertThat(metadata.getTotalPages()).isEqualTo(1);
        }

        @Test
        @DisplayName("should create correct metadata for multi-page result")
        void shouldCreateCorrectMetadataForMultiPageResult() {
            PagedModel.PageMetadata metadata = new PagedModel.PageMetadata(10, 2, 50, 5);

            assertThat(metadata.getNumber()).isEqualTo(2);
            assertThat(metadata.getSize()).isEqualTo(10);
            assertThat(metadata.getTotalElements()).isEqualTo(50);
            assertThat(metadata.getTotalPages()).isEqualTo(5);
        }

        @Test
        @DisplayName("should create correct metadata for last page")
        void shouldCreateCorrectMetadataForLastPage() {
            PagedModel.PageMetadata metadata = new PagedModel.PageMetadata(10, 4, 45, 5);

            assertThat(metadata.getNumber()).isEqualTo(4);
            assertThat(metadata.getSize()).isEqualTo(10);
            assertThat(metadata.getTotalElements()).isEqualTo(45);
            assertThat(metadata.getTotalPages()).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("Page Navigation Logic")
    class PageNavigationLogicTests {

        @Test
        @DisplayName("first page should not have previous")
        void firstPageShouldNotHavePrevious() {
            Page<TestDTO> page = new PageImpl<>(List.of(new TestDTO(1L, true)), PageRequest.of(0, 10), 50);
            assertThat(page.hasPrevious()).isFalse();
        }

        @Test
        @DisplayName("middle page should have both previous and next")
        void middlePageShouldHaveBothPreviousAndNext() {
            Page<TestDTO> page = new PageImpl<>(List.of(new TestDTO(1L, true)), PageRequest.of(2, 10), 50);
            assertThat(page.hasPrevious()).isTrue();
            assertThat(page.hasNext()).isTrue();
        }

        @Test
        @DisplayName("last page should not have next")
        void lastPageShouldNotHaveNext() {
            Page<TestDTO> page = new PageImpl<>(List.of(new TestDTO(1L, true)), PageRequest.of(4, 10), 50);
            assertThat(page.hasNext()).isFalse();
        }

        @Test
        @DisplayName("single page result should have neither previous nor next")
        void singlePageResultShouldHaveNeitherPreviousNorNext() {
            Page<TestDTO> page = new PageImpl<>(List.of(new TestDTO(1L, true)), PageRequest.of(0, 10), 5);
            assertThat(page.hasPrevious()).isFalse();
            assertThat(page.hasNext()).isFalse();
        }

        @Test
        @DisplayName("empty result should have neither previous nor next")
        void emptyResultShouldHaveNeitherPreviousNorNext() {
            Page<TestDTO> page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
            assertThat(page.hasPrevious()).isFalse();
            assertThat(page.hasNext()).isFalse();
            assertThat(page.getTotalPages()).isZero();
        }
    }

    // Test implementations

    record TestDTO(Long id, Boolean activo) implements BaseDTO {}

    @SuppressWarnings("unchecked")
    static class TestResourceAssembler extends BaseResourceAssembler<TestDTO, Long> {
        TestResourceAssembler() {
            super((Class) BaseController.class);
        }

        public Long extractIdPublic(TestDTO dto) {
            return extractId(dto);
        }

        public String buildPageUrlPublic(int page, int size) {
            return buildPageUrl(page, size);
        }

        public void addCustomLinksPublic(EntityModel<TestDTO> model, TestDTO dto) {
            addCustomLinks(model, dto);
        }
    }

    @SuppressWarnings("unchecked")
    static class TestResourceAssemblerWithCustomLinks extends BaseResourceAssembler<TestDTO, Long> {
        TestResourceAssemblerWithCustomLinks() {
            super((Class) BaseController.class);
        }

        public void addCustomLinksPublic(EntityModel<TestDTO> model, TestDTO dto) {
            addCustomLinks(model, dto);
        }

        @Override
        protected void addCustomLinks(EntityModel<TestDTO> model, TestDTO dto) {
            model.add(Link.of("/custom/" + dto.id()).withRel("custom"));
        }
    }
}
