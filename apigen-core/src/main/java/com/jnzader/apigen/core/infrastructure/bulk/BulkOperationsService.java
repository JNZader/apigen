package com.jnzader.apigen.core.infrastructure.bulk;

import com.opencsv.CSVWriter;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.lang.reflect.RecordComponent;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of bulk import and export operations.
 *
 * <p>Supports CSV and Excel (XLSX) formats with features:
 *
 * <ul>
 *   <li>Streaming support for large datasets
 *   <li>Validation and error reporting
 *   <li>Configurable separators and date formats
 *   <li>Header auto-detection
 * </ul>
 */
public class BulkOperationsService implements BulkImportService, BulkExportService {

    private static final Logger log = LoggerFactory.getLogger(BulkOperationsService.class);

    // ========== IMPORT OPERATIONS ==========

    @Override
    public <T> BulkOperationResult importData(
            InputStream inputStream,
            BulkFormat format,
            Class<T> targetClass,
            Function<T, Object> processor) {
        return importData(inputStream, format, targetClass, processor, ImportConfig.defaults());
    }

    @Override
    public <T> BulkOperationResult importData(
            InputStream inputStream,
            BulkFormat format,
            Class<T> targetClass,
            Function<T, Object> processor,
            ImportConfig config) {

        Instant startTime = Instant.now();
        BulkOperationResult.Builder resultBuilder =
                BulkOperationResult.builder()
                        .operationType(BulkOperationResult.OperationType.IMPORT)
                        .format(format)
                        .startTime(startTime);

        try {
            List<T> records = parseData(inputStream, format, targetClass, config);
            resultBuilder.totalRecords(records.size());

            int successCount = 0;
            int failureCount = 0;
            int rowNumber = config.skipHeader() ? 2 : 1;

            for (T record : records) {
                try {
                    processor.apply(record);
                    successCount++;
                } catch (Exception e) {
                    failureCount++;
                    resultBuilder.addError(rowNumber, e.getMessage());
                    if (config.stopOnError()) {
                        break;
                    }
                }
                rowNumber++;
            }

            resultBuilder.successCount(successCount).failureCount(failureCount);

        } catch (Exception e) {
            log.error("Import failed", e);
            resultBuilder
                    .totalRecords(0)
                    .successCount(0)
                    .failureCount(1)
                    .addError(0, e.getMessage());
        }

        return resultBuilder.endTime(Instant.now()).build();
    }

    @Override
    public <T> List<T> parseData(InputStream inputStream, BulkFormat format, Class<T> targetClass) {
        return parseData(inputStream, format, targetClass, ImportConfig.defaults());
    }

    private <T> List<T> parseData(
            InputStream inputStream, BulkFormat format, Class<T> targetClass, ImportConfig config) {
        return switch (format) {
            case CSV -> parseCsv(inputStream, targetClass, config);
            case EXCEL -> parseExcel(inputStream, targetClass, config);
        };
    }

    @Override
    public <T> BulkOperationResult validateData(
            InputStream inputStream, BulkFormat format, Class<T> targetClass) {
        return importData(
                inputStream, format, targetClass, record -> null, ImportConfig.defaults());
    }

    private <T> List<T> parseCsv(
            InputStream inputStream, Class<T> targetClass, ImportConfig config) {
        try (InputStreamReader reader =
                new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {

            CsvToBean<T> csvToBean =
                    new CsvToBeanBuilder<T>(reader)
                            .withType(targetClass)
                            .withSeparator(config.csvSeparator())
                            .withIgnoreLeadingWhiteSpace(true)
                            .withIgnoreEmptyLine(true)
                            .build();

            return csvToBean.parse();

        } catch (IOException e) {
            throw new BulkOperationException("Failed to parse CSV data", e);
        }
    }

    private <T> List<T> parseExcel(
            InputStream inputStream, Class<T> targetClass, ImportConfig config) {
        List<T> results = new ArrayList<>();
        DataFormatter formatter = new DataFormatter();

        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet =
                    config.sheetName() != null
                            ? workbook.getSheet(config.sheetName())
                            : workbook.getSheetAt(0);

            if (sheet == null) {
                throw new BulkOperationException("Sheet not found: " + config.sheetName());
            }

            Iterator<Row> rowIterator = sheet.iterator();

            // Get headers
            if (!rowIterator.hasNext()) {
                return results;
            }
            Row headerRow = rowIterator.next();
            List<String> headers = new ArrayList<>();
            headerRow.forEach(cell -> headers.add(formatter.formatCellValue(cell)));

            // Get field mapping
            List<FieldMapping> fieldMappings = getFieldMappings(targetClass, headers);

            // Parse data rows
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                T record = createInstance(targetClass, row, fieldMappings, formatter, config);
                if (record != null) {
                    results.add(record);
                }
            }

        } catch (IOException e) {
            throw new BulkOperationException("Failed to parse Excel data", e);
        }

        return results;
    }

    // ========== EXPORT OPERATIONS ==========

    @Override
    public <T> byte[] exportData(List<T> records, Class<T> recordClass, BulkFormat format) {
        return exportData(records, recordClass, format, ExportConfig.defaults());
    }

    @Override
    public <T> byte[] exportData(
            List<T> records, Class<T> recordClass, BulkFormat format, ExportConfig config) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        exportToStream(records, recordClass, format, outputStream, config);
        return outputStream.toByteArray();
    }

    @Override
    public <T> BulkOperationResult exportToStream(
            List<T> records, Class<T> recordClass, BulkFormat format, OutputStream outputStream) {
        return exportToStream(records, recordClass, format, outputStream, ExportConfig.defaults());
    }

    private <T> BulkOperationResult exportToStream(
            List<T> records,
            Class<T> recordClass,
            BulkFormat format,
            OutputStream outputStream,
            ExportConfig config) {
        return exportStream(() -> records.stream(), recordClass, format, outputStream, config);
    }

    @Override
    public <T> BulkOperationResult exportStream(
            Supplier<Stream<T>> recordSupplier,
            Class<T> recordClass,
            BulkFormat format,
            OutputStream outputStream) {
        return exportStream(
                recordSupplier, recordClass, format, outputStream, ExportConfig.defaults());
    }

    @Override
    public <T> BulkOperationResult exportStream(
            Supplier<Stream<T>> recordSupplier,
            Class<T> recordClass,
            BulkFormat format,
            OutputStream outputStream,
            ExportConfig config) {

        Instant startTime = Instant.now();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        List<BulkOperationResult.RecordError> errors = new ArrayList<>();

        try {
            switch (format) {
                case CSV ->
                        exportToCsv(
                                recordSupplier, recordClass, outputStream, config, successCount);
                case EXCEL ->
                        exportToExcel(
                                recordSupplier, recordClass, outputStream, config, successCount);
            }
        } catch (Exception e) {
            log.error("Export failed", e);
            failureCount.incrementAndGet();
            errors.add(new BulkOperationResult.RecordError(0, e.getMessage()));
        }

        return BulkOperationResult.builder()
                .operationType(BulkOperationResult.OperationType.EXPORT)
                .format(format)
                .totalRecords(successCount.get() + failureCount.get())
                .successCount(successCount.get())
                .failureCount(failureCount.get())
                .errors(errors)
                .startTime(startTime)
                .endTime(Instant.now())
                .build();
    }

    @Override
    public <T> List<String> getHeaders(Class<T> recordClass) {
        if (recordClass.isRecord()) {
            return Arrays.stream(recordClass.getRecordComponents())
                    .map(RecordComponent::getName)
                    .collect(Collectors.toList());
        }
        return Arrays.stream(recordClass.getDeclaredFields())
                .filter(f -> !java.lang.reflect.Modifier.isStatic(f.getModifiers()))
                .map(Field::getName)
                .collect(Collectors.toList());
    }

    private <T> void exportToCsv(
            Supplier<Stream<T>> recordSupplier,
            Class<T> recordClass,
            OutputStream outputStream,
            ExportConfig config,
            AtomicInteger successCount) {

        try (OutputStreamWriter writer =
                        new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
                CSVWriter csvWriter =
                        new CSVWriter(
                                writer,
                                config.csvSeparator(),
                                CSVWriter.DEFAULT_QUOTE_CHARACTER,
                                CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                                CSVWriter.DEFAULT_LINE_END)) {

            // Write header
            if (config.includeHeader()) {
                List<String> headers = getFilteredHeaders(recordClass, config);
                csvWriter.writeNext(headers.toArray(new String[0]));
            }

            // Write data
            StatefulBeanToCsv<T> beanToCsv =
                    new StatefulBeanToCsvBuilder<T>(writer)
                            .withSeparator(config.csvSeparator())
                            .withApplyQuotesToAll(false)
                            .build();

            try (Stream<T> stream = recordSupplier.get()) {
                stream.forEach(
                        record -> {
                            try {
                                beanToCsv.write(record);
                                successCount.incrementAndGet();
                            } catch (Exception e) {
                                log.warn("Failed to write record: {}", e.getMessage());
                            }
                        });
            }

        } catch (Exception e) {
            throw new BulkOperationException("Failed to export CSV", e);
        }
    }

    private <T> void exportToExcel(
            Supplier<Stream<T>> recordSupplier,
            Class<T> recordClass,
            OutputStream outputStream,
            ExportConfig config,
            AtomicInteger successCount) {

        // Use SXSSFWorkbook for streaming (low memory footprint)
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100)) {
            Sheet sheet = workbook.createSheet(config.sheetName());

            List<String> headers = getFilteredHeaders(recordClass, config);
            int rowNum = 0;

            // Write header
            if (config.includeHeader()) {
                Row headerRow = sheet.createRow(rowNum++);
                CellStyle headerStyle = createHeaderStyle(workbook);
                for (int i = 0; i < headers.size(); i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(headers.get(i));
                    cell.setCellStyle(headerStyle);
                }
            }

            // Write data
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(config.dateFormat());

            try (Stream<T> stream = recordSupplier.get()) {
                Iterator<T> iterator = stream.iterator();
                while (iterator.hasNext()) {
                    T record = iterator.next();
                    Row row = sheet.createRow(rowNum++);
                    writeRecordToRow(record, row, headers, dateFormatter);
                    successCount.incrementAndGet();
                }
            }

            workbook.write(outputStream);

        } catch (IOException e) {
            throw new BulkOperationException("Failed to export Excel", e);
        }
    }

    // ========== HELPER METHODS ==========

    private <T> List<String> getFilteredHeaders(Class<T> recordClass, ExportConfig config) {
        List<String> headers = getHeaders(recordClass);

        if (!config.includeFields().isEmpty()) {
            headers =
                    headers.stream()
                            .filter(config.includeFields()::contains)
                            .collect(Collectors.toList());
        }

        if (!config.excludeFields().isEmpty()) {
            headers =
                    headers.stream()
                            .filter(h -> !config.excludeFields().contains(h))
                            .collect(Collectors.toList());
        }

        return headers;
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private <T> void writeRecordToRow(
            T record, Row row, List<String> headers, DateTimeFormatter dateFormatter) {

        Class<?> clazz = record.getClass();
        for (int i = 0; i < headers.size(); i++) {
            Cell cell = row.createCell(i);
            String fieldName = headers.get(i);

            try {
                Object value = getFieldValue(record, clazz, fieldName);
                setCellValue(cell, value, dateFormatter);
            } catch (Exception e) {
                cell.setCellValue("");
            }
        }
    }

    private Object getFieldValue(Object record, Class<?> clazz, String fieldName) throws Exception {
        if (clazz.isRecord()) {
            for (RecordComponent component : clazz.getRecordComponents()) {
                if (component.getName().equals(fieldName)) {
                    return component.getAccessor().invoke(record);
                }
            }
        } else {
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(record);
        }
        return null;
    }

    private void setCellValue(Cell cell, Object value, DateTimeFormatter dateFormatter) {
        if (value == null) {
            cell.setCellValue("");
        } else if (value instanceof Number num) {
            cell.setCellValue(num.doubleValue());
        } else if (value instanceof Boolean bool) {
            cell.setCellValue(bool);
        } else if (value instanceof LocalDate date) {
            cell.setCellValue(date.format(dateFormatter));
        } else if (value instanceof LocalDateTime dateTime) {
            cell.setCellValue(dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        } else if (value instanceof Instant instant) {
            cell.setCellValue(instant.toString());
        } else {
            cell.setCellValue(value.toString());
        }
    }

    private record FieldMapping(String headerName, String fieldName, Class<?> fieldType) {}

    private <T> List<FieldMapping> getFieldMappings(Class<T> targetClass, List<String> headers) {
        List<FieldMapping> mappings = new ArrayList<>();

        for (String header : headers) {
            String normalizedHeader = header.toLowerCase().replace("_", "").replace(" ", "");

            if (targetClass.isRecord()) {
                for (RecordComponent component : targetClass.getRecordComponents()) {
                    String normalizedField = component.getName().toLowerCase();
                    if (normalizedField.equals(normalizedHeader)) {
                        mappings.add(
                                new FieldMapping(header, component.getName(), component.getType()));
                        break;
                    }
                }
            } else {
                for (Field field : targetClass.getDeclaredFields()) {
                    String normalizedField = field.getName().toLowerCase();
                    if (normalizedField.equals(normalizedHeader)) {
                        mappings.add(new FieldMapping(header, field.getName(), field.getType()));
                        break;
                    }
                }
            }
        }

        return mappings;
    }

    @SuppressWarnings("unchecked")
    private <T> T createInstance(
            Class<T> targetClass,
            Row row,
            List<FieldMapping> fieldMappings,
            DataFormatter formatter,
            ImportConfig config) {

        try {
            if (targetClass.isRecord()) {
                return createRecordInstance(targetClass, row, fieldMappings, formatter, config);
            } else {
                T instance = targetClass.getDeclaredConstructor().newInstance();
                for (int i = 0; i < fieldMappings.size(); i++) {
                    FieldMapping mapping = fieldMappings.get(i);
                    Cell cell = row.getCell(i);
                    Object value = convertCellValue(cell, mapping.fieldType(), formatter, config);
                    setFieldValue(instance, mapping.fieldName(), value);
                }
                return instance;
            }
        } catch (Exception e) {
            log.warn("Failed to create instance for row {}: {}", row.getRowNum(), e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T createRecordInstance(
            Class<T> targetClass,
            Row row,
            List<FieldMapping> fieldMappings,
            DataFormatter formatter,
            ImportConfig config)
            throws Exception {

        RecordComponent[] components = targetClass.getRecordComponents();
        Object[] args = new Object[components.length];
        Class<?>[] paramTypes = new Class<?>[components.length];

        for (int i = 0; i < components.length; i++) {
            paramTypes[i] = components[i].getType();
            args[i] = null;
        }

        for (int i = 0; i < fieldMappings.size() && i < row.getLastCellNum(); i++) {
            FieldMapping mapping = fieldMappings.get(i);
            Cell cell = row.getCell(i);

            for (int j = 0; j < components.length; j++) {
                if (components[j].getName().equals(mapping.fieldName())) {
                    args[j] = convertCellValue(cell, mapping.fieldType(), formatter, config);
                    break;
                }
            }
        }

        return targetClass.getDeclaredConstructor(paramTypes).newInstance(args);
    }

    private Object convertCellValue(
            Cell cell, Class<?> targetType, DataFormatter formatter, ImportConfig config) {
        if (cell == null) {
            return getDefaultValue(targetType);
        }

        String stringValue = formatter.formatCellValue(cell);

        if (targetType == String.class) {
            return stringValue;
        } else if (targetType == Integer.class || targetType == int.class) {
            return cell.getCellType() == CellType.NUMERIC
                    ? (int) cell.getNumericCellValue()
                    : Integer.parseInt(stringValue);
        } else if (targetType == Long.class || targetType == long.class) {
            return cell.getCellType() == CellType.NUMERIC
                    ? (long) cell.getNumericCellValue()
                    : Long.parseLong(stringValue);
        } else if (targetType == Double.class || targetType == double.class) {
            return cell.getCellType() == CellType.NUMERIC
                    ? cell.getNumericCellValue()
                    : Double.parseDouble(stringValue);
        } else if (targetType == Boolean.class || targetType == boolean.class) {
            return cell.getCellType() == CellType.BOOLEAN
                    ? cell.getBooleanCellValue()
                    : Boolean.parseBoolean(stringValue);
        } else if (targetType == LocalDate.class) {
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(config.dateFormat());
            return LocalDate.parse(stringValue, dateFormatter);
        } else if (targetType == LocalDateTime.class) {
            return LocalDateTime.parse(stringValue);
        }

        return stringValue;
    }

    private Object getDefaultValue(Class<?> type) {
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == double.class) return 0.0;
        if (type == boolean.class) return false;
        return null;
    }

    private void setFieldValue(Object instance, String fieldName, Object value) throws Exception {
        Field field = instance.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(instance, value);
    }

    /** Exception thrown when bulk operations fail. */
    public static class BulkOperationException extends RuntimeException {
        public BulkOperationException(String message) {
            super(message);
        }

        public BulkOperationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
