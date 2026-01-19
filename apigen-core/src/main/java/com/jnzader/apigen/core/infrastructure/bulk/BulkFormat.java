package com.jnzader.apigen.core.infrastructure.bulk;

/** Supported formats for bulk import/export operations. */
public enum BulkFormat {
    /** Comma-separated values format */
    CSV("csv", "text/csv", ".csv"),

    /** Microsoft Excel format (XLSX) */
    EXCEL("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", ".xlsx");

    private final String name;
    private final String contentType;
    private final String extension;

    BulkFormat(String name, String contentType, String extension) {
        this.name = name;
        this.contentType = contentType;
        this.extension = extension;
    }

    public String getName() {
        return name;
    }

    public String getContentType() {
        return contentType;
    }

    public String getExtension() {
        return extension;
    }

    /**
     * Determines the format from a filename.
     *
     * @param filename the filename to analyze
     * @return the detected format
     * @throws IllegalArgumentException if format is not supported
     */
    public static BulkFormat fromFilename(String filename) {
        if (filename == null) {
            throw new IllegalArgumentException("Filename cannot be null");
        }
        String lower = filename.toLowerCase();
        if (lower.endsWith(".csv")) {
            return CSV;
        } else if (lower.endsWith(".xlsx") || lower.endsWith(".xls")) {
            return EXCEL;
        }
        throw new IllegalArgumentException("Unsupported file format: " + filename);
    }

    /**
     * Determines the format from a content type.
     *
     * @param contentType the content type to analyze
     * @return the detected format
     * @throws IllegalArgumentException if format is not supported
     */
    public static BulkFormat fromContentType(String contentType) {
        if (contentType == null) {
            throw new IllegalArgumentException("Content type cannot be null");
        }
        if (contentType.contains("csv")) {
            return CSV;
        } else if (contentType.contains("spreadsheet") || contentType.contains("excel")) {
            return EXCEL;
        }
        throw new IllegalArgumentException("Unsupported content type: " + contentType);
    }
}
