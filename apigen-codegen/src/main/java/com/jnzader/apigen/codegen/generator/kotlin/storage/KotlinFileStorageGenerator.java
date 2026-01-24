package com.jnzader.apigen.codegen.generator.kotlin.storage;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Generates Kotlin file storage functionality including entity, service, controller, and providers.
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
public class KotlinFileStorageGenerator {

    private static final String PKG_STORAGE = "storage";

    private final String basePackage;

    public KotlinFileStorageGenerator(String basePackage) {
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
        String basePath = "src/main/kotlin/" + basePackage.replace('.', '/');

        // Generate common interface
        files.put(
                basePath + "/" + PKG_STORAGE + "/service/FileStorageService.kt",
                generateServiceInterface());

        // Generate StoredFile data class
        files.put(basePath + "/" + PKG_STORAGE + "/dto/StoredFile.kt", generateStoredFile());

        // Generate FileUploadResult data class
        files.put(
                basePath + "/" + PKG_STORAGE + "/dto/FileUploadResult.kt",
                generateFileUploadResult());

        // Generate implementation based on storage type
        switch (storageType.toLowerCase(Locale.ROOT)) {
            case "s3" -> {
                files.put(
                        basePath + "/" + PKG_STORAGE + "/service/S3FileStorageService.kt",
                        generateS3Implementation());
                files.put(basePath + "/" + PKG_STORAGE + "/config/S3Config.kt", generateS3Config());
            }
            case "azure" -> {
                files.put(
                        basePath + "/" + PKG_STORAGE + "/service/AzureBlobStorageService.kt",
                        generateAzureImplementation());
                files.put(
                        basePath + "/" + PKG_STORAGE + "/config/AzureStorageConfig.kt",
                        generateAzureConfig());
            }
            default -> {
                files.put(
                        basePath + "/" + PKG_STORAGE + "/service/LocalFileStorageService.kt",
                        generateLocalImplementation());
                files.put(
                        basePath + "/" + PKG_STORAGE + "/config/LocalStorageConfig.kt",
                        generateLocalConfig());
            }
        }

        // Generate controller
        files.put(
                basePath + "/" + PKG_STORAGE + "/controller/FileController.kt",
                generateController(maxFileSizeMb, allowedExtensions));

        // Generate metadata entity and repository if enabled
        if (generateMetadataEntity) {
            files.put(basePath + "/" + PKG_STORAGE + "/entity/FileMetadata.kt", generateEntity());
            files.put(
                    basePath + "/" + PKG_STORAGE + "/repository/FileMetadataRepository.kt",
                    generateRepository());
            files.put(
                    "src/main/resources/db/migration/V998__create_file_metadata_table.sql",
                    generateMigration());
        }

        // Generate exception class
        files.put(
                basePath + "/" + PKG_STORAGE + "/exception/FileStorageException.kt",
                generateException());

        return files;
    }

    private String generateServiceInterface() {
        return """
        package %s.storage.service

        import %s.storage.dto.FileUploadResult
        import %s.storage.dto.StoredFile
        import java.io.InputStream
        import java.util.Optional

        /**
         * Service interface for file storage operations.
         *
         * Provides abstraction over different storage backends (local, S3, Azure).
         */
        interface FileStorageService {

            /**
             * Stores a file.
             *
             * @param filename original filename
             * @param contentType MIME type
             * @param inputStream file content
             * @param size file size in bytes
             * @return upload result with file ID and URL
             */
            fun store(filename: String, contentType: String, inputStream: InputStream, size: Long): FileUploadResult

            /**
             * Retrieves a file by ID.
             *
             * @param fileId the file identifier
             * @return the stored file if found
             */
            fun retrieve(fileId: String): Optional<StoredFile>

            /**
             * Deletes a file by ID.
             *
             * @param fileId the file identifier
             * @return true if deleted, false if not found
             */
            fun delete(fileId: String): Boolean

            /**
             * Checks if a file exists.
             *
             * @param fileId the file identifier
             * @return true if exists
             */
            fun exists(fileId: String): Boolean

            /**
             * Gets the public URL for a file.
             *
             * @param fileId the file identifier
             * @return the URL or null if not public
             */
            fun getUrl(fileId: String): String?

            /**
             * Gets the storage type name.
             *
             * @return storage type (e.g., "local", "s3", "azure")
             */
            fun getStorageType(): String
        }
        """
                .formatted(basePackage, basePackage, basePackage);
    }

    private String generateStoredFile() {
        return """
        package %s.storage.dto

        import java.io.InputStream

        /**
         * Represents a stored file with its content and metadata.
         */
        data class StoredFile(
            val fileId: String,
            val filename: String,
            val contentType: String,
            val size: Long,
            val content: InputStream
        )
        """
                .formatted(basePackage);
    }

    private String generateFileUploadResult() {
        return """
        package %s.storage.dto

        /**
         * Result of a file upload operation.
         */
        data class FileUploadResult(
            val fileId: String,
            val filename: String,
            val contentType: String,
            val size: Long,
            val url: String,
            val storageType: String
        )
        """
                .formatted(basePackage);
    }

    private String generateLocalImplementation() {
        return """
        package %s.storage.service

        import %s.storage.dto.FileUploadResult
        import %s.storage.dto.StoredFile
        import %s.storage.exception.FileStorageException
        import org.slf4j.LoggerFactory
        import org.springframework.beans.factory.annotation.Value
        import org.springframework.stereotype.Service
        import java.io.IOException
        import java.nio.file.Files
        import java.nio.file.Path
        import java.nio.file.StandardCopyOption
        import java.util.Optional
        import java.util.UUID

        /**
         * Local filesystem implementation of FileStorageService.
         */
        @Service
        class LocalFileStorageService(
            @Value("\\${app.storage.local.path:./uploads}") storagePath: String,
            @Value("\\${app.storage.local.base-url:http://localhost:8080/api/files}") private val baseUrl: String
        ) : FileStorageService {

            private val log = LoggerFactory.getLogger(LocalFileStorageService::class.java)
            private val storagePath: Path = Path.of(storagePath)

            init {
                initStorage()
            }

            private fun initStorage() {
                try {
                    Files.createDirectories(storagePath)
                    log.info("Local file storage initialized at: {}", storagePath.toAbsolutePath())
                } catch (e: IOException) {
                    throw FileStorageException("Could not initialize storage", e)
                }
            }

            override fun store(filename: String, contentType: String, inputStream: java.io.InputStream, size: Long): FileUploadResult {
                try {
                    val fileId = UUID.randomUUID().toString()
                    val extension = getExtension(filename)
                    val storedFilename = fileId + if (extension.isEmpty()) "" else ".$extension"

                    val targetPath = storagePath.resolve(storedFilename)
                    Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING)

                    // Store metadata in a sidecar file
                    val metadataContent = "$filename\\n$contentType\\n$size"
                    Files.writeString(storagePath.resolve("$fileId.meta"), metadataContent)

                    log.info("File stored: {} -> {}", filename, storedFilename)

                    return FileUploadResult(
                        fileId = fileId,
                        filename = filename,
                        contentType = contentType,
                        size = size,
                        url = "$baseUrl/$fileId/download",
                        storageType = getStorageType()
                    )
                } catch (e: IOException) {
                    throw FileStorageException("Failed to store file: $filename", e)
                }
            }

            override fun retrieve(fileId: String): Optional<StoredFile> {
                try {
                    val metaPath = storagePath.resolve("$fileId.meta")
                    if (!Files.exists(metaPath)) {
                        return Optional.empty()
                    }

                    val meta = Files.readString(metaPath).split("\\n")
                    val filename = meta[0]
                    val contentType = meta[1]
                    val size = meta[2].toLong()

                    val extension = getExtension(filename)
                    val storedFilename = fileId + if (extension.isEmpty()) "" else ".$extension"
                    val filePath = storagePath.resolve(storedFilename)

                    if (!Files.exists(filePath)) {
                        return Optional.empty()
                    }

                    return Optional.of(
                        StoredFile(
                            fileId = fileId,
                            filename = filename,
                            contentType = contentType,
                            size = size,
                            content = Files.newInputStream(filePath)
                        )
                    )
                } catch (e: IOException) {
                    throw FileStorageException("Failed to retrieve file: $fileId", e)
                }
            }

            override fun delete(fileId: String): Boolean {
                try {
                    val metaPath = storagePath.resolve("$fileId.meta")
                    if (!Files.exists(metaPath)) {
                        return false
                    }

                    val meta = Files.readString(metaPath).split("\\n")
                    val filename = meta[0]
                    val extension = getExtension(filename)
                    val storedFilename = fileId + if (extension.isEmpty()) "" else ".$extension"

                    Files.deleteIfExists(storagePath.resolve(storedFilename))
                    Files.deleteIfExists(metaPath)

                    log.info("File deleted: {}", fileId)
                    return true
                } catch (e: IOException) {
                    throw FileStorageException("Failed to delete file: $fileId", e)
                }
            }

            override fun exists(fileId: String): Boolean =
                Files.exists(storagePath.resolve("$fileId.meta"))

            override fun getUrl(fileId: String): String =
                "$baseUrl/$fileId/download"

            override fun getStorageType(): String = "local"

            private fun getExtension(filename: String): String {
                val dotIndex = filename.lastIndexOf('.')
                return if (dotIndex > 0) filename.substring(dotIndex + 1) else ""
            }
        }
        """
                .formatted(basePackage, basePackage, basePackage, basePackage);
    }

    private String generateS3Implementation() {
        return """
        package %s.storage.service

        import %s.storage.dto.FileUploadResult
        import %s.storage.dto.StoredFile
        import %s.storage.exception.FileStorageException
        import org.slf4j.LoggerFactory
        import org.springframework.beans.factory.annotation.Value
        import org.springframework.stereotype.Service
        import software.amazon.awssdk.core.sync.RequestBody
        import software.amazon.awssdk.services.s3.S3Client
        import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
        import software.amazon.awssdk.services.s3.model.GetObjectRequest
        import software.amazon.awssdk.services.s3.model.HeadObjectRequest
        import software.amazon.awssdk.services.s3.model.NoSuchKeyException
        import software.amazon.awssdk.services.s3.model.PutObjectRequest
        import java.io.InputStream
        import java.util.Optional
        import java.util.UUID

        /**
         * AWS S3 implementation of FileStorageService.
         */
        @Service
        class S3FileStorageService(
            private val s3Client: S3Client,
            @Value("\\${app.storage.s3.bucket}") private val bucketName: String,
            @Value("\\${app.storage.s3.base-url:}") baseUrl: String
        ) : FileStorageService {

            private val log = LoggerFactory.getLogger(S3FileStorageService::class.java)
            private val baseUrl = baseUrl.ifEmpty { "https://$bucketName.s3.amazonaws.com" }

            override fun store(filename: String, contentType: String, inputStream: InputStream, size: Long): FileUploadResult {
                try {
                    val fileId = UUID.randomUUID().toString()
                    val extension = getExtension(filename)
                    val key = fileId + if (extension.isEmpty()) "" else ".$extension"

                    val request = PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .contentType(contentType)
                        .metadata(
                            mapOf(
                                "original-filename" to filename,
                                "file-id" to fileId
                            )
                        )
                        .build()

                    s3Client.putObject(request, RequestBody.fromInputStream(inputStream, size))

                    log.info("File stored to S3: {} -> {}", filename, key)

                    return FileUploadResult(
                        fileId = fileId,
                        filename = filename,
                        contentType = contentType,
                        size = size,
                        url = "$baseUrl/$key",
                        storageType = getStorageType()
                    )
                } catch (e: Exception) {
                    throw FileStorageException("Failed to store file to S3: $filename", e)
                }
            }

            override fun retrieve(fileId: String): Optional<StoredFile> {
                try {
                    val key = findKeyByFileId(fileId) ?: return Optional.empty()

                    val request = GetObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .build()

                    val response = s3Client.getObject(request)
                    val metadata = response.response()

                    val originalFilename = metadata.metadata()["original-filename"] ?: key

                    return Optional.of(
                        StoredFile(
                            fileId = fileId,
                            filename = originalFilename,
                            contentType = metadata.contentType(),
                            size = metadata.contentLength(),
                            content = response
                        )
                    )
                } catch (e: NoSuchKeyException) {
                    return Optional.empty()
                } catch (e: Exception) {
                    throw FileStorageException("Failed to retrieve file from S3: $fileId", e)
                }
            }

            override fun delete(fileId: String): Boolean {
                try {
                    val key = findKeyByFileId(fileId) ?: return false

                    val request = DeleteObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .build()

                    s3Client.deleteObject(request)
                    log.info("File deleted from S3: {}", fileId)
                    return true
                } catch (e: Exception) {
                    throw FileStorageException("Failed to delete file from S3: $fileId", e)
                }
            }

            override fun exists(fileId: String): Boolean =
                findKeyByFileId(fileId) != null

            override fun getUrl(fileId: String): String? {
                val key = findKeyByFileId(fileId)
                return key?.let { "$baseUrl/$it" }
            }

            override fun getStorageType(): String = "s3"

            private fun findKeyByFileId(fileId: String): String? {
                val extensions = listOf("", ".jpg", ".jpeg", ".png", ".gif", ".pdf", ".doc", ".docx")
                for (ext in extensions) {
                    val key = fileId + ext
                    try {
                        val request = HeadObjectRequest.builder()
                            .bucket(bucketName)
                            .key(key)
                            .build()
                        s3Client.headObject(request)
                        return key
                    } catch (_: NoSuchKeyException) {
                        // Continue to next extension
                    }
                }
                return null
            }

            private fun getExtension(filename: String): String {
                val dotIndex = filename.lastIndexOf('.')
                return if (dotIndex > 0) filename.substring(dotIndex + 1) else ""
            }
        }
        """
                .formatted(basePackage, basePackage, basePackage, basePackage);
    }

    private String generateAzureImplementation() {
        return """
        package %s.storage.service

        import %s.storage.dto.FileUploadResult
        import %s.storage.dto.StoredFile
        import %s.storage.exception.FileStorageException
        import com.azure.storage.blob.BlobContainerClient
        import com.azure.storage.blob.models.BlobHttpHeaders
        import org.slf4j.LoggerFactory
        import org.springframework.stereotype.Service
        import java.io.ByteArrayInputStream
        import java.io.ByteArrayOutputStream
        import java.io.InputStream
        import java.util.Optional
        import java.util.UUID

        /**
         * Azure Blob Storage implementation of FileStorageService.
         */
        @Service
        class AzureBlobStorageService(
            private val containerClient: BlobContainerClient
        ) : FileStorageService {

            private val log = LoggerFactory.getLogger(AzureBlobStorageService::class.java)

            override fun store(filename: String, contentType: String, inputStream: InputStream, size: Long): FileUploadResult {
                try {
                    val fileId = UUID.randomUUID().toString()
                    val extension = getExtension(filename)
                    val blobName = fileId + if (extension.isEmpty()) "" else ".$extension"

                    val blobClient = containerClient.getBlobClient(blobName)

                    blobClient.upload(inputStream, size, true)
                    blobClient.setHttpHeaders(BlobHttpHeaders().setContentType(contentType))
                    blobClient.setMetadata(
                        mapOf(
                            "originalFilename" to filename,
                            "fileId" to fileId
                        )
                    )

                    log.info("File stored to Azure Blob: {} -> {}", filename, blobName)

                    return FileUploadResult(
                        fileId = fileId,
                        filename = filename,
                        contentType = contentType,
                        size = size,
                        url = blobClient.blobUrl,
                        storageType = getStorageType()
                    )
                } catch (e: Exception) {
                    throw FileStorageException("Failed to store file to Azure Blob: $filename", e)
                }
            }

            override fun retrieve(fileId: String): Optional<StoredFile> {
                try {
                    val blobName = findBlobByFileId(fileId) ?: return Optional.empty()

                    val blobClient = containerClient.getBlobClient(blobName)
                    if (!blobClient.exists()) {
                        return Optional.empty()
                    }

                    val properties = blobClient.properties
                    val originalFilename = properties.metadata["originalFilename"] ?: blobName

                    val outputStream = ByteArrayOutputStream()
                    blobClient.downloadStream(outputStream)

                    return Optional.of(
                        StoredFile(
                            fileId = fileId,
                            filename = originalFilename,
                            contentType = properties.contentType,
                            size = properties.blobSize,
                            content = ByteArrayInputStream(outputStream.toByteArray())
                        )
                    )
                } catch (e: Exception) {
                    throw FileStorageException("Failed to retrieve file from Azure Blob: $fileId", e)
                }
            }

            override fun delete(fileId: String): Boolean {
                try {
                    val blobName = findBlobByFileId(fileId) ?: return false

                    val blobClient = containerClient.getBlobClient(blobName)
                    blobClient.delete()
                    log.info("File deleted from Azure Blob: {}", fileId)
                    return true
                } catch (e: Exception) {
                    throw FileStorageException("Failed to delete file from Azure Blob: $fileId", e)
                }
            }

            override fun exists(fileId: String): Boolean =
                findBlobByFileId(fileId) != null

            override fun getUrl(fileId: String): String? {
                val blobName = findBlobByFileId(fileId)
                return blobName?.let { containerClient.getBlobClient(it).blobUrl }
            }

            override fun getStorageType(): String = "azure"

            private fun findBlobByFileId(fileId: String): String? {
                val extensions = listOf("", ".jpg", ".jpeg", ".png", ".gif", ".pdf", ".doc", ".docx")
                for (ext in extensions) {
                    val blobName = fileId + ext
                    val blobClient = containerClient.getBlobClient(blobName)
                    if (blobClient.exists()) {
                        return blobName
                    }
                }
                return null
            }

            private fun getExtension(filename: String): String {
                val dotIndex = filename.lastIndexOf('.')
                return if (dotIndex > 0) filename.substring(dotIndex + 1) else ""
            }
        }
        """
                .formatted(basePackage, basePackage, basePackage, basePackage);
    }

    private String generateLocalConfig() {
        return """
        package %s.storage.config

        import org.springframework.boot.context.properties.ConfigurationProperties
        import org.springframework.context.annotation.Configuration

        /**
         * Configuration for local file storage.
         */
        @Configuration
        @ConfigurationProperties(prefix = "app.storage.local")
        class LocalStorageConfig {
            var path: String = "./uploads"
            var baseUrl: String = "http://localhost:8080/api/files"
        }
        """
                .formatted(basePackage);
    }

    private String generateS3Config() {
        return """
        package %s.storage.config

        import org.springframework.beans.factory.annotation.Value
        import org.springframework.context.annotation.Bean
        import org.springframework.context.annotation.Configuration
        import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
        import software.amazon.awssdk.regions.Region
        import software.amazon.awssdk.services.s3.S3Client

        /**
         * Configuration for AWS S3 file storage.
         */
        @Configuration
        class S3Config {

            @Value("\\${app.storage.s3.region:us-east-1}")
            private lateinit var region: String

            @Bean
            fun s3Client(): S3Client {
                return S3Client.builder()
                    .region(Region.of(region))
                    .credentialsProvider(DefaultCredentialsProvider.create())
                    .build()
            }
        }
        """
                .formatted(basePackage);
    }

    private String generateAzureConfig() {
        return """
        package %s.storage.config

        import com.azure.storage.blob.BlobContainerClient
        import com.azure.storage.blob.BlobServiceClient
        import com.azure.storage.blob.BlobServiceClientBuilder
        import org.springframework.beans.factory.annotation.Value
        import org.springframework.context.annotation.Bean
        import org.springframework.context.annotation.Configuration

        /**
         * Configuration for Azure Blob Storage.
         */
        @Configuration
        class AzureStorageConfig {

            @Value("\\${app.storage.azure.connection-string}")
            private lateinit var connectionString: String

            @Value("\\${app.storage.azure.container}")
            private lateinit var containerName: String

            @Bean
            fun blobServiceClient(): BlobServiceClient {
                return BlobServiceClientBuilder()
                    .connectionString(connectionString)
                    .buildClient()
            }

            @Bean
            fun blobContainerClient(blobServiceClient: BlobServiceClient): BlobContainerClient {
                val containerClient = blobServiceClient.getBlobContainerClient(containerName)
                if (!containerClient.exists()) {
                    containerClient.create()
                }
                return containerClient
            }
        }
        """
                .formatted(basePackage);
    }

    private String generateController(int maxFileSizeMb, String allowedExtensions) {
        return """
        package %s.storage.controller

        import %s.storage.dto.FileUploadResult
        import %s.storage.exception.FileStorageException
        import %s.storage.service.FileStorageService
        import io.swagger.v3.oas.annotations.Operation
        import io.swagger.v3.oas.annotations.responses.ApiResponse
        import io.swagger.v3.oas.annotations.responses.ApiResponses
        import io.swagger.v3.oas.annotations.tags.Tag
        import org.springframework.core.io.InputStreamResource
        import org.springframework.http.HttpHeaders
        import org.springframework.http.MediaType
        import org.springframework.http.ResponseEntity
        import org.springframework.web.bind.annotation.DeleteMapping
        import org.springframework.web.bind.annotation.GetMapping
        import org.springframework.web.bind.annotation.PathVariable
        import org.springframework.web.bind.annotation.PostMapping
        import org.springframework.web.bind.annotation.RequestMapping
        import org.springframework.web.bind.annotation.RequestParam
        import org.springframework.web.bind.annotation.RestController
        import org.springframework.web.multipart.MultipartFile

        /**
         * REST controller for file operations.
         */
        @RestController
        @RequestMapping("/api/files")
        @Tag(name = "Files", description = "File upload, download, and management")
        class FileController(
            private val storageService: FileStorageService
        ) {

            companion object {
                private const val MAX_FILE_SIZE = %dL * 1024 * 1024 // %d MB
                private val ALLOWED_EXTENSIONS = "%s".split(",")
            }

            @PostMapping("/upload")
            @Operation(summary = "Upload a file", description = "Uploads a file to storage")
            @ApiResponses(
                ApiResponse(responseCode = "200", description = "File uploaded successfully"),
                ApiResponse(responseCode = "400", description = "Invalid file or file too large"),
                ApiResponse(responseCode = "415", description = "File type not allowed")
            )
            fun upload(@RequestParam("file") file: MultipartFile): ResponseEntity<FileUploadResult> {
                if (file.isEmpty) {
                    throw FileStorageException("File is empty")
                }

                if (file.size > MAX_FILE_SIZE) {
                    throw FileStorageException("File size exceeds maximum allowed size of %d MB")
                }

                val extension = getExtension(file.originalFilename ?: "")
                if (!ALLOWED_EXTENSIONS.contains(extension.lowercase())) {
                    throw FileStorageException("File type not allowed: $extension")
                }

                val result = storageService.store(
                    file.originalFilename ?: "unknown",
                    file.contentType ?: "application/octet-stream",
                    file.inputStream,
                    file.size
                )
                return ResponseEntity.ok(result)
            }

            @GetMapping("/{fileId}")
            @Operation(summary = "Get file metadata", description = "Returns file metadata without content")
            @ApiResponses(
                ApiResponse(responseCode = "200", description = "File metadata returned"),
                ApiResponse(responseCode = "404", description = "File not found")
            )
            fun getMetadata(@PathVariable fileId: String): ResponseEntity<FileUploadResult> {
                return storageService.retrieve(fileId)
                    .map { file ->
                        ResponseEntity.ok(
                            FileUploadResult(
                                fileId = fileId,
                                filename = file.filename,
                                contentType = file.contentType,
                                size = file.size,
                                url = storageService.getUrl(fileId) ?: "",
                                storageType = storageService.getStorageType()
                            )
                        )
                    }
                    .orElse(ResponseEntity.notFound().build())
            }

            @GetMapping("/{fileId}/download")
            @Operation(summary = "Download a file", description = "Downloads file content")
            @ApiResponses(
                ApiResponse(responseCode = "200", description = "File content returned"),
                ApiResponse(responseCode = "404", description = "File not found")
            )
            fun download(@PathVariable fileId: String): ResponseEntity<InputStreamResource> {
                return storageService.retrieve(fileId)
                    .map { file ->
                        ResponseEntity.ok()
                            .contentType(MediaType.parseMediaType(file.contentType))
                            .header(
                                HttpHeaders.CONTENT_DISPOSITION,
                                "attachment; filename=\\"${'$'}{file.filename}\\""
                            )
                            .contentLength(file.size)
                            .body(InputStreamResource(file.content))
                    }
                    .orElse(ResponseEntity.notFound().build())
            }

            @DeleteMapping("/{fileId}")
            @Operation(summary = "Delete a file", description = "Permanently deletes a file")
            @ApiResponses(
                ApiResponse(responseCode = "204", description = "File deleted"),
                ApiResponse(responseCode = "404", description = "File not found")
            )
            fun delete(@PathVariable fileId: String): ResponseEntity<Void> {
                return if (storageService.delete(fileId)) {
                    ResponseEntity.noContent().build()
                } else {
                    ResponseEntity.notFound().build()
                }
            }

            private fun getExtension(filename: String): String {
                val dotIndex = filename.lastIndexOf('.')
                return if (dotIndex > 0) filename.substring(dotIndex + 1) else ""
            }
        }
        """
                .formatted(
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
        package %s.storage.entity

        import jakarta.persistence.Column
        import jakarta.persistence.Entity
        import jakarta.persistence.GeneratedValue
        import jakarta.persistence.GenerationType
        import jakarta.persistence.Id
        import jakarta.persistence.Index
        import jakarta.persistence.Table
        import java.time.LocalDateTime

        /**
         * Entity for storing file metadata.
         */
        @Entity
        @Table(
            name = "file_metadata",
            indexes = [
                Index(name = "idx_fm_file_id", columnList = "file_id"),
                Index(name = "idx_fm_owner_id", columnList = "owner_id")
            ]
        )
        data class FileMetadata(
            @Id
            @GeneratedValue(strategy = GenerationType.IDENTITY)
            val id: Long? = null,

            @Column(name = "file_id", nullable = false, unique = true, length = 64)
            val fileId: String,

            @Column(nullable = false, length = 255)
            val filename: String,

            @Column(name = "content_type", nullable = false, length = 100)
            val contentType: String,

            @Column(nullable = false)
            val size: Long,

            @Column(name = "storage_type", nullable = false, length = 20)
            val storageType: String,

            @Column(length = 2048)
            val url: String? = null,

            @Column(name = "owner_id")
            val ownerId: Long? = null,

            @Column(name = "created_at", nullable = false)
            val createdAt: LocalDateTime = LocalDateTime.now(),

            @Column(name = "deleted_at")
            val deletedAt: LocalDateTime? = null
        )
        """
                .formatted(basePackage);
    }

    private String generateRepository() {
        return """
        package %s.storage.repository

        import %s.storage.entity.FileMetadata
        import org.springframework.data.jpa.repository.JpaRepository
        import org.springframework.stereotype.Repository
        import java.util.Optional

        /**
         * Repository for file metadata operations.
         */
        @Repository
        interface FileMetadataRepository : JpaRepository<FileMetadata, Long> {

            fun findByFileId(fileId: String): Optional<FileMetadata>

            fun findByFileIdAndDeletedAtIsNull(fileId: String): Optional<FileMetadata>

            fun findByOwnerIdAndDeletedAtIsNull(ownerId: Long): List<FileMetadata>
        }
        """
                .formatted(basePackage, basePackage);
    }

    private String generateException() {
        return """
        package %s.storage.exception

        /**
         * Exception thrown when file storage operations fail.
         */
        class FileStorageException : RuntimeException {
            constructor(message: String) : super(message)
            constructor(message: String, cause: Throwable) : super(message, cause)
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
