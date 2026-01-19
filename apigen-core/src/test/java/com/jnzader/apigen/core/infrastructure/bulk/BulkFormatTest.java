package com.jnzader.apigen.core.infrastructure.bulk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("BulkFormat Tests")
class BulkFormatTest {

    @Nested
    @DisplayName("Format Properties")
    class FormatPropertiesTests {

        @Test
        @DisplayName("CSV format should have correct properties")
        void csvFormatShouldHaveCorrectProperties() {
            assertThat(BulkFormat.CSV.getName()).isEqualTo("csv");
            assertThat(BulkFormat.CSV.getContentType()).isEqualTo("text/csv");
            assertThat(BulkFormat.CSV.getExtension()).isEqualTo(".csv");
        }

        @Test
        @DisplayName("Excel format should have correct properties")
        void excelFormatShouldHaveCorrectProperties() {
            assertThat(BulkFormat.EXCEL.getName()).isEqualTo("xlsx");
            assertThat(BulkFormat.EXCEL.getContentType())
                    .isEqualTo("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            assertThat(BulkFormat.EXCEL.getExtension()).isEqualTo(".xlsx");
        }
    }

    @Nested
    @DisplayName("From Filename")
    class FromFilenameTests {

        @ParameterizedTest
        @ValueSource(strings = {"data.csv", "DATA.CSV", "export.Csv", "path/to/file.csv"})
        @DisplayName("should detect CSV format from filename")
        void shouldDetectCsvFormatFromFilename(String filename) {
            assertThat(BulkFormat.fromFilename(filename)).isEqualTo(BulkFormat.CSV);
        }

        @ParameterizedTest
        @ValueSource(strings = {"data.xlsx", "DATA.XLSX", "export.Xlsx", "path/to/file.xlsx"})
        @DisplayName("should detect Excel XLSX format from filename")
        void shouldDetectExcelXlsxFormatFromFilename(String filename) {
            assertThat(BulkFormat.fromFilename(filename)).isEqualTo(BulkFormat.EXCEL);
        }

        @ParameterizedTest
        @ValueSource(strings = {"data.xls", "DATA.XLS", "export.Xls"})
        @DisplayName("should detect Excel XLS format from filename")
        void shouldDetectExcelXlsFormatFromFilename(String filename) {
            assertThat(BulkFormat.fromFilename(filename)).isEqualTo(BulkFormat.EXCEL);
        }

        @Test
        @DisplayName("should throw for null filename")
        void shouldThrowForNullFilename() {
            assertThatThrownBy(() -> BulkFormat.fromFilename(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("null");
        }

        @ParameterizedTest
        @ValueSource(strings = {"data.txt", "data.json", "data.xml", "data"})
        @DisplayName("should throw for unsupported format")
        void shouldThrowForUnsupportedFormat(String filename) {
            assertThatThrownBy(() -> BulkFormat.fromFilename(filename))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Unsupported");
        }
    }

    @Nested
    @DisplayName("From Content Type")
    class FromContentTypeTests {

        @ParameterizedTest
        @ValueSource(strings = {"text/csv", "application/csv", "text/csv; charset=utf-8"})
        @DisplayName("should detect CSV format from content type")
        void shouldDetectCsvFormatFromContentType(String contentType) {
            assertThat(BulkFormat.fromContentType(contentType)).isEqualTo(BulkFormat.CSV);
        }

        @ParameterizedTest
        @ValueSource(
                strings = {
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    "application/vnd.ms-excel",
                    "application/excel"
                })
        @DisplayName("should detect Excel format from content type")
        void shouldDetectExcelFormatFromContentType(String contentType) {
            assertThat(BulkFormat.fromContentType(contentType)).isEqualTo(BulkFormat.EXCEL);
        }

        @Test
        @DisplayName("should throw for null content type")
        void shouldThrowForNullContentType() {
            assertThatThrownBy(() -> BulkFormat.fromContentType(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("null");
        }

        @ParameterizedTest
        @ValueSource(strings = {"application/json", "text/plain", "application/xml"})
        @DisplayName("should throw for unsupported content type")
        void shouldThrowForUnsupportedContentType(String contentType) {
            assertThatThrownBy(() -> BulkFormat.fromContentType(contentType))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Unsupported");
        }
    }
}
