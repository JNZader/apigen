package com.jnzader.apigen.codegen.generator.java.storage;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Generates file storage functionality including entity, service, controller, and providers.
 *
 * <p>This generator creates:
 *
 * <ul>
 *   <li>FileMetadata entity for storing file information
 *   <li>FileStorageService interface with multiple implementations (local, S3, Azure)
 *   <li>FileController with REST endpoints for upload/download/delete
 *   <li>Configuration classes for each storage provider
 *   <li>Flyway migration for the metadata table
 * </ul>
 */
@SuppressWarnings({
    "java:S1192",
    "java:S3400"
}) // S1192: template strings; S3400: template methods return constants
public class FileStorageGenerator {

    private static final String PKG_STORAGE = "storage";

    private final String basePackage;

    public FileStorageGenerator(String basePackage) {
        this.basePackage = basePackage;
    }

    /**
     * Generates all file storage files.
     *
     * @param storageType storage type: "local", "s3", or "azure"
     * @param maxFileSizeMb maximum file size in MB
     * @param allowedExtensions comma-separated list of allowed extensions
     * @param generateMetadataEntity whether to generate file metadata entity
     * @return map of file path to content
     */
    public Map<String, String> generate(
            String storageType,
            int maxFileSizeMb,
            String allowedExtensions,
            boolean generateMetadataEntity) {

        Map<String, String> files = new LinkedHashMap<>();
        String basePath = "src/main/java/" + basePackage.replace('.', '/');

        // Generate common interface
        files.put(
                basePath + "/" + PKG_STORAGE + "/service/FileStorageService.java",
                generateServiceInterface());

        // Generate StoredFile record
        files.put(
                basePath + "/" + PKG_STORAGE + "/dto/StoredFile.java", generateStoredFileRecord());

        // Generate FileUploadResult record
        files.put(
                basePath + "/" + PKG_STORAGE + "/dto/FileUploadResult.java",
                generateFileUploadResult());

        // Generate implementation based on storage type
        switch (storageType.toLowerCase()) {
            case "s3" -> {
                files.put(
                        basePath + "/" + PKG_STORAGE + "/service/S3FileStorageService.java",
                        generateS3Implementation());
                files.put(
                        basePath + "/" + PKG_STORAGE + "/config/S3Config.java", generateS3Config());
            }
            case "azure" -> {
                files.put(
                        basePath + "/" + PKG_STORAGE + "/service/AzureBlobStorageService.java",
                        generateAzureImplementation());
                files.put(
                        basePath + "/" + PKG_STORAGE + "/config/AzureStorageConfig.java",
                        generateAzureConfig());
            }
            default -> {
                files.put(
                        basePath + "/" + PKG_STORAGE + "/service/LocalFileStorageService.java",
                        generateLocalImplementation());
                files.put(
                        basePath + "/" + PKG_STORAGE + "/config/LocalStorageConfig.java",
                        generateLocalConfig());
            }
        }

        // Generate controller
        files.put(
                basePath + "/" + PKG_STORAGE + "/controller/FileController.java",
                generateController(maxFileSizeMb, allowedExtensions));

        // Generate metadata entity and repository if enabled
        if (generateMetadataEntity) {
            files.put(basePath + "/" + PKG_STORAGE + "/entity/FileMetadata.java", generateEntity());
            files.put(
                    basePath + "/" + PKG_STORAGE + "/repository/FileMetadataRepository.java",
                    generateRepository());
            files.put(
                    "src/main/resources/db/migration/V998__create_file_metadata_table.sql",
                    generateMigration());
        }

        // Generate exception class
        files.put(
                basePath + "/" + PKG_STORAGE + "/exception/FileStorageException.java",
                generateException());

        return files;
    }

    private String generateServiceInterface() {
        return """
        package %s.storage.service;

        import %s.storage.dto.FileUploadResult;
        import %s.storage.dto.StoredFile;
        import java.io.InputStream;
        import java.util.Optional;

        /**
         * Service interface for file storage operations.
         *
         * <p>Provides abstraction over different storage backends (local, S3, Azure).
         */
        public interface FileStorageService {

            /**
             * Stores a file.
             *
             * @param filename original filename
             * @param contentType MIME type
             * @param inputStream file content
             * @param size file size in bytes
             * @return upload result with file ID and URL
             */
            FileUploadResult store(String filename, String contentType, InputStream inputStream, long size);

            /**
             * Retrieves a file by ID.
             *
             * @param fileId the file identifier
             * @return the stored file if found
             */
            Optional<StoredFile> retrieve(String fileId);

            /**
             * Deletes a file by ID.
             *
             * @param fileId the file identifier
             * @return true if deleted, false if not found
             */
            boolean delete(String fileId);

            /**
             * Checks if a file exists.
             *
             * @param fileId the file identifier
             * @return true if exists
             */
            boolean exists(String fileId);

            /**
             * Gets the public URL for a file.
             *
             * @param fileId the file identifier
             * @return the URL or null if not public
             */
            String getUrl(String fileId);

            /**
             * Gets the storage type name.
             *
             * @return storage type (e.g., "local", "s3", "azure")
             */
            String getStorageType();
        }
        """
                .formatted(basePackage, basePackage, basePackage);
    }

    private String generateStoredFileRecord() {
        return """
        package %s.storage.dto;

        import java.io.InputStream;

        /**
         * Represents a stored file with its content and metadata.
         */
        public record StoredFile(
                String fileId,
                String filename,
                String contentType,
                long size,
                InputStream content) {}
        """
                .formatted(basePackage);
    }

    private String generateFileUploadResult() {
        return """
        package %s.storage.dto;

        import lombok.Builder;
        import lombok.Data;

        /**
         * Result of a file upload operation.
         */
        @Data
        @Builder
        public class FileUploadResult {
            private String fileId;
            private String filename;
            private String contentType;
            private long size;
            private String url;
            private String storageType;
        }
        """
                .formatted(basePackage);
    }

    private String generateLocalImplementation() {
        return """
        package %s.storage.service;

        import %s.storage.dto.FileUploadResult;
        import %s.storage.dto.StoredFile;
        import %s.storage.exception.FileStorageException;
        import java.io.IOException;
        import java.io.InputStream;
        import java.nio.file.Files;
        import java.nio.file.Path;
        import java.nio.file.StandardCopyOption;
        import java.util.Optional;
        import java.util.UUID;
        import org.slf4j.Logger;
        import org.slf4j.LoggerFactory;
        import org.springframework.beans.factory.annotation.Value;
        import org.springframework.stereotype.Service;

        /**
         * Local filesystem implementation of FileStorageService.
         */
        @Service
        public class LocalFileStorageService implements FileStorageService {

            private static final Logger log = LoggerFactory.getLogger(LocalFileStorageService.class);

            private final Path storagePath;
            private final String baseUrl;

            public LocalFileStorageService(
                    @Value("${app.storage.local.path:./uploads}") String storagePath,
                    @Value("${app.storage.local.base-url:http://localhost:8080/api/files}") String baseUrl) {
                this.storagePath = Path.of(storagePath);
                this.baseUrl = baseUrl;
                initStorage();
            }

            private void initStorage() {
                try {
                    Files.createDirectories(storagePath);
                    log.info("Local file storage initialized at: {}", storagePath.toAbsolutePath());
                } catch (IOException e) {
                    throw new FileStorageException("Could not initialize storage", e);
                }
            }

            @Override
            public FileUploadResult store(String filename, String contentType, InputStream inputStream, long size) {
                try {
                    String fileId = UUID.randomUUID().toString();
                    String extension = getExtension(filename);
                    String storedFilename = fileId + (extension.isEmpty() ? "" : "." + extension);

                    Path targetPath = storagePath.resolve(storedFilename);
                    Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);

                    // Store metadata in a sidecar file
                    String metadataContent = filename + "\\n" + contentType + "\\n" + size;
                    Files.writeString(storagePath.resolve(fileId + ".meta"), metadataContent);

                    log.info("File stored: {} -> {}", filename, storedFilename);

                    return FileUploadResult.builder()
                            .fileId(fileId)
                            .filename(filename)
                            .contentType(contentType)
                            .size(size)
                            .url(baseUrl + "/" + fileId + "/download")
                            .storageType(getStorageType())
                            .build();
                } catch (IOException e) {
                    throw new FileStorageException("Failed to store file: " + filename, e);
                }
            }

            @Override
            public Optional<StoredFile> retrieve(String fileId) {
                try {
                    Path metaPath = storagePath.resolve(fileId + ".meta");
                    if (!Files.exists(metaPath)) {
                        return Optional.empty();
                    }

                    String[] meta = Files.readString(metaPath).split("\\\\n");
                    if (meta.length < 3) {
                        log.error("Invalid metadata file format for fileId: {}", fileId);
                        return Optional.empty();
                    }
                    String filename = meta[0];
                    String contentType = meta[1];
                    long size = Long.parseLong(meta[2]);

                    String extension = getExtension(filename);
                    String storedFilename = fileId + (extension.isEmpty() ? "" : "." + extension);
                    Path filePath = storagePath.resolve(storedFilename);

                    if (!Files.exists(filePath)) {
                        return Optional.empty();
                    }

                    // Note: Caller is responsible for closing the InputStream
                    return Optional.of(new StoredFile(
                            fileId,
                            filename,
                            contentType,
                            size,
                            Files.newInputStream(filePath)));
                } catch (IOException e) {
                    throw new FileStorageException("Failed to retrieve file: " + fileId, e);
                }
            }

            @Override
            public boolean delete(String fileId) {
                try {
                    Path metaPath = storagePath.resolve(fileId + ".meta");
                    if (!Files.exists(metaPath)) {
                        return false;
                    }

                    String[] meta = Files.readString(metaPath).split("\\\\n");
                    if (meta.length < 1) {
                        log.error("Invalid metadata file format for fileId: {}", fileId);
                        return false;
                    }
                    String filename = meta[0];
                    String extension = getExtension(filename);
                    String storedFilename = fileId + (extension.isEmpty() ? "" : "." + extension);

                    Files.deleteIfExists(storagePath.resolve(storedFilename));
                    Files.deleteIfExists(metaPath);

                    log.info("File deleted: {}", fileId);
                    return true;
                } catch (IOException e) {
                    throw new FileStorageException("Failed to delete file: " + fileId, e);
                }
            }

            @Override
            public boolean exists(String fileId) {
                return Files.exists(storagePath.resolve(fileId + ".meta"));
            }

            @Override
            public String getUrl(String fileId) {
                return baseUrl + "/" + fileId + "/download";
            }

            @Override
            public String getStorageType() {
                return "local";
            }

            private String getExtension(String filename) {
                int dotIndex = filename.lastIndexOf('.');
                return dotIndex > 0 ? filename.substring(dotIndex + 1) : "";
            }
        }
        """
                .formatted(basePackage, basePackage, basePackage, basePackage);
    }

    private String generateS3Implementation() {
        return """
        package %s.storage.service;

        import %s.storage.dto.FileUploadResult;
        import %s.storage.dto.StoredFile;
        import %s.storage.exception.FileStorageException;
        import java.io.InputStream;
        import java.util.Optional;
        import java.util.UUID;
        import org.slf4j.Logger;
        import org.slf4j.LoggerFactory;
        import org.springframework.beans.factory.annotation.Value;
        import org.springframework.stereotype.Service;
        import software.amazon.awssdk.core.sync.RequestBody;
        import software.amazon.awssdk.services.s3.S3Client;
        import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
        import software.amazon.awssdk.services.s3.model.GetObjectRequest;
        import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
        import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
        import software.amazon.awssdk.services.s3.model.PutObjectRequest;

        /**
         * AWS S3 implementation of FileStorageService.
         */
        @Service
        public class S3FileStorageService implements FileStorageService {

            private static final Logger log = LoggerFactory.getLogger(S3FileStorageService.class);

            private final S3Client s3Client;
            private final String bucketName;
            private final String baseUrl;

            public S3FileStorageService(
                    S3Client s3Client,
                    @Value("${app.storage.s3.bucket}") String bucketName,
                    @Value("${app.storage.s3.base-url:}") String baseUrl) {
                this.s3Client = s3Client;
                this.bucketName = bucketName;
                this.baseUrl = baseUrl.isEmpty() ?
                        "https://" + bucketName + ".s3.amazonaws.com" : baseUrl;
            }

            @Override
            public FileUploadResult store(String filename, String contentType, InputStream inputStream, long size) {
                try {
                    String fileId = UUID.randomUUID().toString();
                    String extension = getExtension(filename);
                    String key = fileId + (extension.isEmpty() ? "" : "." + extension);

                    PutObjectRequest request = PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(key)
                            .contentType(contentType)
                            .metadata(java.util.Map.of(
                                    "original-filename", filename,
                                    "file-id", fileId))
                            .build();

                    s3Client.putObject(request, RequestBody.fromInputStream(inputStream, size));

                    log.info("File stored to S3: {} -> {}", filename, key);

                    return FileUploadResult.builder()
                            .fileId(fileId)
                            .filename(filename)
                            .contentType(contentType)
                            .size(size)
                            .url(baseUrl + "/" + key)
                            .storageType(getStorageType())
                            .build();
                } catch (Exception e) {
                    throw new FileStorageException("Failed to store file to S3: " + filename, e);
                }
            }

            @Override
            public Optional<StoredFile> retrieve(String fileId) {
                try {
                    // Try to find the file with any extension
                    String key = findKeyByFileId(fileId);
                    if (key == null) {
                        return Optional.empty();
                    }

                    GetObjectRequest request = GetObjectRequest.builder()
                            .bucket(bucketName)
                            .key(key)
                            .build();

                    var response = s3Client.getObject(request);
                    var metadata = response.response();

                    String originalFilename = metadata.metadata().getOrDefault("original-filename", key);

                    return Optional.of(new StoredFile(
                            fileId,
                            originalFilename,
                            metadata.contentType(),
                            metadata.contentLength(),
                            response));
                } catch (NoSuchKeyException e) {
                    return Optional.empty();
                } catch (Exception e) {
                    throw new FileStorageException("Failed to retrieve file from S3: " + fileId, e);
                }
            }

            @Override
            public boolean delete(String fileId) {
                try {
                    String key = findKeyByFileId(fileId);
                    if (key == null) {
                        return false;
                    }

                    DeleteObjectRequest request = DeleteObjectRequest.builder()
                            .bucket(bucketName)
                            .key(key)
                            .build();

                    s3Client.deleteObject(request);
                    log.info("File deleted from S3: {}", fileId);
                    return true;
                } catch (Exception e) {
                    throw new FileStorageException("Failed to delete file from S3: " + fileId, e);
                }
            }

            @Override
            public boolean exists(String fileId) {
                return findKeyByFileId(fileId) != null;
            }

            @Override
            public String getUrl(String fileId) {
                String key = findKeyByFileId(fileId);
                return key != null ? baseUrl + "/" + key : null;
            }

            @Override
            public String getStorageType() {
                return "s3";
            }

            private String findKeyByFileId(String fileId) {
                // Try common extensions
                String[] extensions = {"", ".jpg", ".jpeg", ".png", ".gif", ".pdf", ".doc", ".docx"};
                for (String ext : extensions) {
                    String key = fileId + ext;
                    try {
                        HeadObjectRequest request = HeadObjectRequest.builder()
                                .bucket(bucketName)
                                .key(key)
                                .build();
                        s3Client.headObject(request);
                        return key;
                    } catch (NoSuchKeyException e) {
                        // Continue to next extension
                    }
                }
                return null;
            }

            private String getExtension(String filename) {
                int dotIndex = filename.lastIndexOf('.');
                return dotIndex > 0 ? filename.substring(dotIndex + 1) : "";
            }
        }
        """
                .formatted(basePackage, basePackage, basePackage, basePackage);
    }

    private String generateAzureImplementation() {
        return """
        package %s.storage.service;

        import %s.storage.dto.FileUploadResult;
        import %s.storage.dto.StoredFile;
        import %s.storage.exception.FileStorageException;
        import com.azure.storage.blob.BlobClient;
        import com.azure.storage.blob.BlobContainerClient;
        import com.azure.storage.blob.models.BlobHttpHeaders;
        import java.io.ByteArrayInputStream;
        import java.io.ByteArrayOutputStream;
        import java.io.InputStream;
        import java.util.Optional;
        import java.util.UUID;
        import org.slf4j.Logger;
        import org.slf4j.LoggerFactory;
        import org.springframework.stereotype.Service;

        /**
         * Azure Blob Storage implementation of FileStorageService.
         */
        @Service
        public class AzureBlobStorageService implements FileStorageService {

            private static final Logger log = LoggerFactory.getLogger(AzureBlobStorageService.class);

            private final BlobContainerClient containerClient;

            public AzureBlobStorageService(BlobContainerClient containerClient) {
                this.containerClient = containerClient;
            }

            @Override
            public FileUploadResult store(String filename, String contentType, InputStream inputStream, long size) {
                try {
                    String fileId = UUID.randomUUID().toString();
                    String extension = getExtension(filename);
                    String blobName = fileId + (extension.isEmpty() ? "" : "." + extension);

                    BlobClient blobClient = containerClient.getBlobClient(blobName);

                    blobClient.upload(inputStream, size, true);
                    blobClient.setHttpHeaders(new BlobHttpHeaders().setContentType(contentType));
                    blobClient.setMetadata(java.util.Map.of(
                            "originalFilename", filename,
                            "fileId", fileId));

                    log.info("File stored to Azure Blob: {} -> {}", filename, blobName);

                    return FileUploadResult.builder()
                            .fileId(fileId)
                            .filename(filename)
                            .contentType(contentType)
                            .size(size)
                            .url(blobClient.getBlobUrl())
                            .storageType(getStorageType())
                            .build();
                } catch (Exception e) {
                    throw new FileStorageException("Failed to store file to Azure Blob: " + filename, e);
                }
            }

            @Override
            public Optional<StoredFile> retrieve(String fileId) {
                try {
                    String blobName = findBlobByFileId(fileId);
                    if (blobName == null) {
                        return Optional.empty();
                    }

                    BlobClient blobClient = containerClient.getBlobClient(blobName);
                    if (!blobClient.exists()) {
                        return Optional.empty();
                    }

                    var properties = blobClient.getProperties();
                    String originalFilename = properties.getMetadata().getOrDefault("originalFilename", blobName);

                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    blobClient.downloadStream(outputStream);

                    return Optional.of(new StoredFile(
                            fileId,
                            originalFilename,
                            properties.getContentType(),
                            properties.getBlobSize(),
                            new ByteArrayInputStream(outputStream.toByteArray())));
                } catch (Exception e) {
                    throw new FileStorageException("Failed to retrieve file from Azure Blob: " + fileId, e);
                }
            }

            @Override
            public boolean delete(String fileId) {
                try {
                    String blobName = findBlobByFileId(fileId);
                    if (blobName == null) {
                        return false;
                    }

                    BlobClient blobClient = containerClient.getBlobClient(blobName);
                    blobClient.delete();
                    log.info("File deleted from Azure Blob: {}", fileId);
                    return true;
                } catch (Exception e) {
                    throw new FileStorageException("Failed to delete file from Azure Blob: " + fileId, e);
                }
            }

            @Override
            public boolean exists(String fileId) {
                return findBlobByFileId(fileId) != null;
            }

            @Override
            public String getUrl(String fileId) {
                String blobName = findBlobByFileId(fileId);
                return blobName != null ?
                        containerClient.getBlobClient(blobName).getBlobUrl() : null;
            }

            @Override
            public String getStorageType() {
                return "azure";
            }

            private String findBlobByFileId(String fileId) {
                String[] extensions = {"", ".jpg", ".jpeg", ".png", ".gif", ".pdf", ".doc", ".docx"};
                for (String ext : extensions) {
                    String blobName = fileId + ext;
                    BlobClient blobClient = containerClient.getBlobClient(blobName);
                    if (blobClient.exists()) {
                        return blobName;
                    }
                }
                return null;
            }

            private String getExtension(String filename) {
                int dotIndex = filename.lastIndexOf('.');
                return dotIndex > 0 ? filename.substring(dotIndex + 1) : "";
            }
        }
        """
                .formatted(basePackage, basePackage, basePackage, basePackage);
    }

    private String generateLocalConfig() {
        return """
        package %s.storage.config;

        import org.springframework.boot.context.properties.ConfigurationProperties;
        import org.springframework.context.annotation.Configuration;
        import lombok.Data;

        /**
         * Configuration for local file storage.
         */
        @Configuration
        @ConfigurationProperties(prefix = "app.storage.local")
        @Data
        public class LocalStorageConfig {
            private String path = "./uploads";
            private String baseUrl = "http://localhost:8080/api/files";
        }
        """
                .formatted(basePackage);
    }

    private String generateS3Config() {
        return """
        package %s.storage.config;

        import org.springframework.beans.factory.annotation.Value;
        import org.springframework.context.annotation.Bean;
        import org.springframework.context.annotation.Configuration;
        import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
        import software.amazon.awssdk.regions.Region;
        import software.amazon.awssdk.services.s3.S3Client;

        /**
         * Configuration for AWS S3 file storage.
         */
        @Configuration
        public class S3Config {

            @Value("${app.storage.s3.region:us-east-1}")
            private String region;

            @Bean
            public S3Client s3Client() {
                return S3Client.builder()
                        .region(Region.of(region))
                        .credentialsProvider(DefaultCredentialsProvider.create())
                        .build();
            }
        }
        """
                .formatted(basePackage);
    }

    private String generateAzureConfig() {
        return """
        package %s.storage.config;

        import com.azure.storage.blob.BlobContainerClient;
        import com.azure.storage.blob.BlobServiceClient;
        import com.azure.storage.blob.BlobServiceClientBuilder;
        import org.springframework.beans.factory.annotation.Value;
        import org.springframework.context.annotation.Bean;
        import org.springframework.context.annotation.Configuration;

        /**
         * Configuration for Azure Blob Storage.
         */
        @Configuration
        public class AzureStorageConfig {

            @Value("${app.storage.azure.connection-string}")
            private String connectionString;

            @Value("${app.storage.azure.container}")
            private String containerName;

            @Bean
            public BlobServiceClient blobServiceClient() {
                return new BlobServiceClientBuilder()
                        .connectionString(connectionString)
                        .buildClient();
            }

            @Bean
            public BlobContainerClient blobContainerClient(BlobServiceClient blobServiceClient) {
                BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
                if (!containerClient.exists()) {
                    containerClient.create();
                }
                return containerClient;
            }
        }
        """
                .formatted(basePackage);
    }

    private String generateController(int maxFileSizeMb, String allowedExtensions) {
        return """
        package %s.storage.controller;

        import %s.storage.dto.FileUploadResult;
        import %s.storage.dto.StoredFile;
        import %s.storage.exception.FileStorageException;
        import %s.storage.service.FileStorageService;
        import io.swagger.v3.oas.annotations.Operation;
        import io.swagger.v3.oas.annotations.responses.ApiResponse;
        import io.swagger.v3.oas.annotations.responses.ApiResponses;
        import io.swagger.v3.oas.annotations.tags.Tag;
        import java.util.Arrays;
        import java.util.List;
        import org.springframework.core.io.InputStreamResource;
        import org.springframework.http.HttpHeaders;
        import org.springframework.http.MediaType;
        import org.springframework.http.ResponseEntity;
        import org.springframework.web.bind.annotation.DeleteMapping;
        import org.springframework.web.bind.annotation.GetMapping;
        import org.springframework.web.bind.annotation.PathVariable;
        import org.springframework.web.bind.annotation.PostMapping;
        import org.springframework.web.bind.annotation.RequestMapping;
        import org.springframework.web.bind.annotation.RequestParam;
        import org.springframework.web.bind.annotation.RestController;
        import org.springframework.web.multipart.MultipartFile;

        /**
         * REST controller for file operations.
         */
        @RestController
        @RequestMapping("/api/files")
        @Tag(name = "Files", description = "File upload, download, and management")
        public class FileController {

            private static final long MAX_FILE_SIZE = %dL * 1024 * 1024; // %d MB
            private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("%s".split(","));

            private final FileStorageService storageService;

            public FileController(FileStorageService storageService) {
                this.storageService = storageService;
            }

            @PostMapping("/upload")
            @Operation(summary = "Upload a file", description = "Uploads a file to storage")
            @ApiResponses({
                @ApiResponse(responseCode = "200", description = "File uploaded successfully"),
                @ApiResponse(responseCode = "400", description = "Invalid file or file too large"),
                @ApiResponse(responseCode = "415", description = "File type not allowed")
            })
            public ResponseEntity<FileUploadResult> upload(@RequestParam("file") MultipartFile file) {
                if (file.isEmpty()) {
                    throw new FileStorageException("File is empty");
                }

                if (file.getSize() > MAX_FILE_SIZE) {
                    throw new FileStorageException("File size exceeds maximum allowed size of %d MB");
                }

                String extension = getExtension(file.getOriginalFilename());
                if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
                    throw new FileStorageException("File type not allowed: " + extension);
                }

                try {
                    FileUploadResult result = storageService.store(
                            file.getOriginalFilename(),
                            file.getContentType(),
                            file.getInputStream(),
                            file.getSize());
                    return ResponseEntity.ok(result);
                } catch (Exception e) {
                    throw new FileStorageException("Failed to upload file", e);
                }
            }

            @GetMapping("/{fileId}")
            @Operation(summary = "Get file metadata", description = "Returns file metadata without content")
            @ApiResponses({
                @ApiResponse(responseCode = "200", description = "File metadata returned"),
                @ApiResponse(responseCode = "404", description = "File not found")
            })
            public ResponseEntity<FileUploadResult> getMetadata(@PathVariable String fileId) {
                return storageService.retrieve(fileId)
                        .map(file -> ResponseEntity.ok(FileUploadResult.builder()
                                .fileId(fileId)
                                .filename(file.filename())
                                .contentType(file.contentType())
                                .size(file.size())
                                .url(storageService.getUrl(fileId))
                                .storageType(storageService.getStorageType())
                                .build()))
                        .orElse(ResponseEntity.notFound().build());
            }

            @GetMapping("/{fileId}/download")
            @Operation(summary = "Download a file", description = "Downloads file content")
            @ApiResponses({
                @ApiResponse(responseCode = "200", description = "File content returned"),
                @ApiResponse(responseCode = "404", description = "File not found")
            })
            public ResponseEntity<InputStreamResource> download(@PathVariable String fileId) {
                return storageService.retrieve(fileId)
                        .map(file -> ResponseEntity.ok()
                                .contentType(MediaType.parseMediaType(file.contentType()))
                                .header(HttpHeaders.CONTENT_DISPOSITION,
                                        "attachment; filename=\\"" + file.filename() + "\\"")
                                .contentLength(file.size())
                                .body(new InputStreamResource(file.content())))
                        .orElse(ResponseEntity.notFound().build());
            }

            @DeleteMapping("/{fileId}")
            @Operation(summary = "Delete a file", description = "Permanently deletes a file")
            @ApiResponses({
                @ApiResponse(responseCode = "204", description = "File deleted"),
                @ApiResponse(responseCode = "404", description = "File not found")
            })
            public ResponseEntity<Void> delete(@PathVariable String fileId) {
                if (storageService.delete(fileId)) {
                    return ResponseEntity.noContent().build();
                }
                return ResponseEntity.notFound().build();
            }

            private String getExtension(String filename) {
                if (filename == null) return "";
                int dotIndex = filename.lastIndexOf('.');
                return dotIndex > 0 ? filename.substring(dotIndex + 1) : "";
            }
        }
        """
                .formatted(
                        basePackage,
                        basePackage,
                        basePackage,
                        basePackage,
                        basePackage,
                        maxFileSizeMb,
                        maxFileSizeMb,
                        allowedExtensions,
                        maxFileSizeMb);
    }

    private String generateEntity() {
        return """
        package %s.storage.entity;

        import jakarta.persistence.Column;
        import jakarta.persistence.Entity;
        import jakarta.persistence.GeneratedValue;
        import jakarta.persistence.GenerationType;
        import jakarta.persistence.Id;
        import jakarta.persistence.Index;
        import jakarta.persistence.Table;
        import java.time.LocalDateTime;
        import lombok.AllArgsConstructor;
        import lombok.Builder;
        import lombok.Data;
        import lombok.NoArgsConstructor;

        /**
         * Entity for storing file metadata.
         */
        @Entity
        @Table(
                name = "file_metadata",
                indexes = {
                    @Index(name = "idx_fm_file_id", columnList = "file_id"),
                    @Index(name = "idx_fm_owner_id", columnList = "owner_id")
                })
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public class FileMetadata {

            @Id
            @GeneratedValue(strategy = GenerationType.IDENTITY)
            private Long id;

            @Column(name = "file_id", nullable = false, unique = true, length = 64)
            private String fileId;

            @Column(nullable = false, length = 255)
            private String filename;

            @Column(name = "content_type", nullable = false, length = 100)
            private String contentType;

            @Column(nullable = false)
            private Long size;

            @Column(name = "storage_type", nullable = false, length = 20)
            private String storageType;

            @Column(length = 2048)
            private String url;

            @Column(name = "owner_id")
            private Long ownerId;

            @Column(name = "created_at", nullable = false)
            private LocalDateTime createdAt;

            @Column(name = "deleted_at")
            private LocalDateTime deletedAt;
        }
        """
                .formatted(basePackage);
    }

    private String generateRepository() {
        return """
        package %s.storage.repository;

        import %s.storage.entity.FileMetadata;
        import java.util.List;
        import java.util.Optional;
        import org.springframework.data.jpa.repository.JpaRepository;
        import org.springframework.stereotype.Repository;

        /**
         * Repository for file metadata operations.
         */
        @Repository
        public interface FileMetadataRepository extends JpaRepository<FileMetadata, Long> {

            Optional<FileMetadata> findByFileId(String fileId);

            Optional<FileMetadata> findByFileIdAndDeletedAtIsNull(String fileId);

            List<FileMetadata> findByOwnerIdAndDeletedAtIsNull(Long ownerId);
        }
        """
                .formatted(basePackage, basePackage);
    }

    private String generateException() {
        return """
        package %s.storage.exception;

        /**
         * Exception thrown when file storage operations fail.
         */
        public class FileStorageException extends RuntimeException {

            public FileStorageException(String message) {
                super(message);
            }

            public FileStorageException(String message, Throwable cause) {
                super(message, cause);
            }
        }
        """
                .formatted(basePackage);
    }

    private String generateMigration() {
        return """
        -- File Metadata table
        CREATE TABLE IF NOT EXISTS file_metadata (
            id BIGSERIAL PRIMARY KEY,
            file_id VARCHAR(64) NOT NULL UNIQUE,
            filename VARCHAR(255) NOT NULL,
            content_type VARCHAR(100) NOT NULL,
            size BIGINT NOT NULL,
            storage_type VARCHAR(20) NOT NULL,
            url VARCHAR(2048),
            owner_id BIGINT,
            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
            deleted_at TIMESTAMP
        );

        -- Indexes for efficient lookups
        CREATE INDEX IF NOT EXISTS idx_fm_file_id ON file_metadata(file_id);
        CREATE INDEX IF NOT EXISTS idx_fm_owner_id ON file_metadata(owner_id);
        """;
    }
}
