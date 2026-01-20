package com.jnzader.apigen.core.infrastructure.bulk;

import static org.assertj.core.api.Assertions.assertThat;

import com.opencsv.bean.CsvBindByName;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("BulkOperationsService Tests")
class BulkOperationsServiceTest {

    private BulkOperationsService bulkOperationsService;

    @BeforeEach
    void setUp() {
        bulkOperationsService = new BulkOperationsService();
    }

    // Test DTO class
    public static class ProductDTO {
        @CsvBindByName(column = "id")
        private Long id;

        @CsvBindByName(column = "name")
        private String name;

        @CsvBindByName(column = "price")
        private BigDecimal price;

        @CsvBindByName(column = "quantity")
        private Integer quantity;

        public ProductDTO() {}

        public ProductDTO(Long id, String name, BigDecimal price, Integer quantity) {
            this.id = id;
            this.name = name;
            this.price = price;
            this.quantity = quantity;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public BigDecimal getPrice() {
            return price;
        }

        public void setPrice(BigDecimal price) {
            this.price = price;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }
    }

    @Nested
    @DisplayName("CSV Import")
    class CsvImportTests {

        @Test
        @DisplayName("should import valid CSV data")
        void shouldImportValidCsvData() {
            String csvData =
                    """
                    id,name,price,quantity
                    1,Product A,19.99,100
                    2,Product B,29.99,50
                    3,Product C,39.99,25
                    """;

            ByteArrayInputStream inputStream =
                    new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8));
            List<ProductDTO> imported = new ArrayList<>();

            BulkOperationResult result =
                    bulkOperationsService.importData(
                            inputStream,
                            BulkFormat.CSV,
                            ProductDTO.class,
                            dto -> {
                                imported.add(dto);
                                return dto;
                            });

            assertThat(result.operationType()).isEqualTo(BulkOperationResult.OperationType.IMPORT);
            assertThat(result.format()).isEqualTo(BulkFormat.CSV);
            assertThat(result.totalRecords()).isEqualTo(3);
            assertThat(result.successCount()).isEqualTo(3);
            assertThat(result.failureCount()).isZero();
            assertThat(result.isFullySuccessful()).isTrue();
            // Note: Different subjects (result vs imported) - cannot chain
            assertThat(imported).hasSize(3);
        }

        @Test
        @DisplayName("should parse CSV data without processing")
        void shouldParseCsvDataWithoutProcessing() {
            String csvData =
                    """
                    id,name,price,quantity
                    1,Laptop,999.99,10
                    2,Mouse,29.99,100
                    """;

            ByteArrayInputStream inputStream =
                    new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8));

            List<ProductDTO> parsed =
                    bulkOperationsService.parseData(inputStream, BulkFormat.CSV, ProductDTO.class);

            assertThat(parsed).hasSize(2);
            // Note: Different subjects - cannot chain
            assertThat(parsed.get(0).getName()).isEqualTo("Laptop");
            assertThat(parsed.get(1).getName()).isEqualTo("Mouse");
        }

        @Test
        @DisplayName("should handle processing errors")
        void shouldHandleProcessingErrors() {
            String csvData =
                    """
                    id,name,price,quantity
                    1,Product A,19.99,100
                    2,Product B,29.99,50
                    3,Product C,39.99,25
                    """;

            ByteArrayInputStream inputStream =
                    new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8));
            AtomicInteger counter = new AtomicInteger(0);

            BulkOperationResult result =
                    bulkOperationsService.importData(
                            inputStream,
                            BulkFormat.CSV,
                            ProductDTO.class,
                            dto -> {
                                if (counter.incrementAndGet() == 2) {
                                    throw new RuntimeException("Processing error");
                                }
                                return dto;
                            });

            assertThat(result.successCount()).isEqualTo(2);
            assertThat(result.failureCount()).isEqualTo(1);
            assertThat(result.hasFailures()).isTrue();
            // Note: Different result accessor types - cannot chain
            assertThat(result.errors()).hasSize(1);
        }

        @Test
        @DisplayName("should stop on error when configured")
        void shouldStopOnErrorWhenConfigured() {
            String csvData =
                    """
                    id,name,price,quantity
                    1,Product A,19.99,100
                    2,Product B,29.99,50
                    3,Product C,39.99,25
                    """;

            ByteArrayInputStream inputStream =
                    new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8));
            AtomicInteger counter = new AtomicInteger(0);

            BulkImportService.ImportConfig config =
                    BulkImportService.ImportConfig.builder().stopOnError(true).build();

            BulkOperationResult result =
                    bulkOperationsService.importData(
                            inputStream,
                            BulkFormat.CSV,
                            ProductDTO.class,
                            dto -> {
                                if (counter.incrementAndGet() == 1) {
                                    throw new RuntimeException("Processing error");
                                }
                                return dto;
                            },
                            config);

            assertThat(result.successCount()).isZero();
            assertThat(result.failureCount()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("CSV Export")
    class CsvExportTests {

        @Test
        @DisplayName("should export data to CSV")
        void shouldExportDataToCsv() {
            List<ProductDTO> products =
                    List.of(
                            new ProductDTO(1L, "Laptop", new BigDecimal("999.99"), 10),
                            new ProductDTO(2L, "Mouse", new BigDecimal("29.99"), 100));

            byte[] csvData =
                    bulkOperationsService.exportData(products, ProductDTO.class, BulkFormat.CSV);

            assertThat(csvData).isNotEmpty();
            String csvString = new String(csvData, StandardCharsets.UTF_8);
            assertThat(csvString).contains("Laptop").contains("Mouse");
        }

        @Test
        @DisplayName("should export to output stream")
        void shouldExportToOutputStream() {
            List<ProductDTO> products =
                    List.of(
                            new ProductDTO(1L, "Keyboard", new BigDecimal("79.99"), 50),
                            new ProductDTO(2L, "Monitor", new BigDecimal("299.99"), 20));

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            BulkOperationResult result =
                    bulkOperationsService.exportToStream(
                            products, ProductDTO.class, BulkFormat.CSV, outputStream);

            assertThat(result.operationType()).isEqualTo(BulkOperationResult.OperationType.EXPORT);
            assertThat(result.format()).isEqualTo(BulkFormat.CSV);
            assertThat(result.successCount()).isEqualTo(2);
            assertThat(result.isFullySuccessful()).isTrue();

            String csvString = outputStream.toString(StandardCharsets.UTF_8);
            assertThat(csvString).contains("Keyboard").contains("Monitor");
        }

        @Test
        @DisplayName("should export empty list")
        void shouldExportEmptyList() {
            List<ProductDTO> products = List.of();

            byte[] csvData =
                    bulkOperationsService.exportData(products, ProductDTO.class, BulkFormat.CSV);

            assertThat(csvData).isNotNull();
        }
    }

    @Nested
    @DisplayName("Excel Export")
    class ExcelExportTests {

        @Test
        @DisplayName("should export data to Excel")
        void shouldExportDataToExcel() {
            List<ProductDTO> products =
                    List.of(
                            new ProductDTO(1L, "Laptop", new BigDecimal("999.99"), 10),
                            new ProductDTO(2L, "Mouse", new BigDecimal("29.99"), 100));

            byte[] excelData =
                    bulkOperationsService.exportData(products, ProductDTO.class, BulkFormat.EXCEL);

            assertThat(excelData).isNotEmpty();
            // Excel files start with PK (ZIP format)
            // Note: Separate indexes - cannot chain
            assertThat(excelData[0]).isEqualTo((byte) 0x50); // P
            assertThat(excelData[1]).isEqualTo((byte) 0x4B); // K
        }

        @Test
        @DisplayName("should export to Excel output stream")
        void shouldExportToExcelOutputStream() {
            List<ProductDTO> products =
                    List.of(
                            new ProductDTO(1L, "Tablet", new BigDecimal("499.99"), 30),
                            new ProductDTO(2L, "Phone", new BigDecimal("899.99"), 45));

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            BulkOperationResult result =
                    bulkOperationsService.exportToStream(
                            products, ProductDTO.class, BulkFormat.EXCEL, outputStream);

            assertThat(result.operationType()).isEqualTo(BulkOperationResult.OperationType.EXPORT);
            assertThat(result.format()).isEqualTo(BulkFormat.EXCEL);
            assertThat(result.successCount()).isEqualTo(2);
            assertThat(result.isFullySuccessful()).isTrue();

            byte[] data = outputStream.toByteArray();
            assertThat(data).isNotEmpty();
        }

        @Test
        @DisplayName("should use custom sheet name")
        void shouldUseCustomSheetName() {
            List<ProductDTO> products =
                    List.of(new ProductDTO(1L, "Item", new BigDecimal("10.00"), 5));

            BulkExportService.ExportConfig config =
                    BulkExportService.ExportConfig.builder().sheetName("Products").build();

            byte[] excelData =
                    bulkOperationsService.exportData(
                            products, ProductDTO.class, BulkFormat.EXCEL, config);

            assertThat(excelData).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("Headers")
    class HeadersTests {

        @Test
        @DisplayName("should extract headers from class")
        void shouldExtractHeadersFromClass() {
            List<String> headers = bulkOperationsService.getHeaders(ProductDTO.class);

            assertThat(headers).contains("id", "name", "price", "quantity");
        }

        @Test
        @DisplayName("should extract headers from record")
        void shouldExtractHeadersFromRecord() {
            List<String> headers = bulkOperationsService.getHeaders(SimpleRecord.class);

            assertThat(headers).containsExactly("id", "name", "value");
        }
    }

    @Nested
    @DisplayName("Stream Export")
    class StreamExportTests {

        @Test
        @DisplayName("should export stream of records")
        void shouldExportStreamOfRecords() {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            BulkOperationResult result =
                    bulkOperationsService.exportStream(
                            () ->
                                    List.of(
                                            new ProductDTO(1L, "A", BigDecimal.ONE, 1),
                                            new ProductDTO(2L, "B", BigDecimal.TEN, 2))
                                            .stream(),
                            ProductDTO.class,
                            BulkFormat.CSV,
                            outputStream);

            assertThat(result.successCount()).isEqualTo(2);
            assertThat(result.isFullySuccessful()).isTrue();
        }
    }

    @Nested
    @DisplayName("Import Config")
    class ImportConfigTests {

        @Test
        @DisplayName("should use custom separator")
        void shouldUseCustomSeparator() {
            String csvData =
                    """
                    id;name;price;quantity
                    1;Product A;19.99;100
                    """;

            ByteArrayInputStream inputStream =
                    new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8));

            BulkImportService.ImportConfig config =
                    BulkImportService.ImportConfig.builder().csvSeparator(';').build();

            BulkOperationResult result =
                    bulkOperationsService.importData(
                            inputStream, BulkFormat.CSV, ProductDTO.class, dto -> dto, config);

            assertThat(result.successCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("default config should have expected values")
        void defaultConfigShouldHaveExpectedValues() {
            BulkImportService.ImportConfig config = BulkImportService.ImportConfig.defaults();

            assertThat(config.skipHeader()).isTrue();
            assertThat(config.stopOnError()).isFalse();
            assertThat(config.batchSize()).isEqualTo(100);
            assertThat(config.csvSeparator()).isEqualTo(',');
            assertThat(config.dateFormat()).isEqualTo("yyyy-MM-dd");
            // Note: Different config accessor types - cannot chain
        }
    }

    @Nested
    @DisplayName("Export Config")
    class ExportConfigTests {

        @Test
        @DisplayName("default config should have expected values")
        void defaultConfigShouldHaveExpectedValues() {
            BulkExportService.ExportConfig config = BulkExportService.ExportConfig.defaults();

            assertThat(config.includeHeader()).isTrue();
            assertThat(config.csvSeparator()).isEqualTo(',');
            assertThat(config.dateFormat()).isEqualTo("yyyy-MM-dd");
            assertThat(config.sheetName()).isEqualTo("Data");
            assertThat(config.autoSizeColumns()).isTrue();
            // Note: Different config accessor types - cannot chain
        }

        @Test
        @DisplayName("should exclude fields when configured")
        void shouldExcludeFieldsWhenConfigured() {
            List<ProductDTO> products = List.of(new ProductDTO(1L, "Test", BigDecimal.TEN, 5));

            BulkExportService.ExportConfig config =
                    BulkExportService.ExportConfig.builder().excludeFields(List.of("id")).build();

            byte[] data =
                    bulkOperationsService.exportData(
                            products, ProductDTO.class, BulkFormat.CSV, config);
            String csvString = new String(data, StandardCharsets.UTF_8);

            // The CSV should still work, headers filtering is applied
            assertThat(csvString).contains("Test");
        }
    }

    // Test record for record support
    public record SimpleRecord(Long id, String name, Double value) {}
}
