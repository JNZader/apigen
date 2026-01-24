/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jnzader.apigen.codegen.generator.csharp.storage;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Generates file storage code for C#/ASP.NET Core applications.
 *
 * @author APiGen
 * @since 2.13.0
 */
public class CSharpFileStorageGenerator {

    private final String namespace;

    public CSharpFileStorageGenerator(String namespace) {
        this.namespace = namespace;
    }

    /**
     * Generates file storage files.
     *
     * @param useS3 whether to generate S3 storage support
     * @param useAzure whether to generate Azure Blob storage support
     * @return map of file path to content
     */
    public Map<String, String> generate(boolean useS3, boolean useAzure) {
        Map<String, String> files = new LinkedHashMap<>();

        files.put("Models/FileMetadata.cs", generateModel());
        files.put("DTOs/FileStorageDtos.cs", generateDtos());
        files.put("Services/Storage/IStorageService.cs", generateInterface());
        files.put("Services/Storage/LocalStorageService.cs", generateLocalStorage());
        files.put("Services/Storage/FileStorageService.cs", generateService(useS3, useAzure));
        files.put("Controllers/FilesController.cs", generateController());
        files.put("Configuration/StorageSettings.cs", generateConfig(useS3, useAzure));

        if (useS3) {
            files.put("Services/Storage/S3StorageService.cs", generateS3Storage());
        }

        if (useAzure) {
            files.put("Services/Storage/AzureBlobStorageService.cs", generateAzureStorage());
        }

        return files;
    }

    private String generateModel() {
        return String.format(
                """
                using System.ComponentModel.DataAnnotations;

                namespace %s.Models;

                /// <summary>
                /// File metadata entity.
                /// </summary>
                public class FileMetadata
                {
                    [Key]
                    public Guid Id { get; set; }

                    [Required]
                    [MaxLength(255)]
                    public string FileName { get; set; } = string.Empty;

                    [Required]
                    [MaxLength(500)]
                    public string StorageKey { get; set; } = string.Empty;

                    [Required]
                    [MaxLength(100)]
                    public string ContentType { get; set; } = string.Empty;

                    public long Size { get; set; }

                    [MaxLength(2000)]
                    public string Url { get; set; } = string.Empty;

                    public Guid? UserId { get; set; }

                    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

                    // Navigation
                    public virtual User? User { get; set; }
                }
                """,
                namespace);
    }

    private String generateDtos() {
        return String.format(
                """
                namespace %s.DTOs;

                /// <summary>
                /// File upload response.
                /// </summary>
                public record FileUploadResponse
                {
                    public Guid Id { get; init; }
                    public string FileName { get; init; } = string.Empty;
                    public string Url { get; init; } = string.Empty;
                    public long Size { get; init; }
                    public string ContentType { get; init; } = string.Empty;
                }

                /// <summary>
                /// File info response.
                /// </summary>
                public record FileInfoResponse
                {
                    public Guid Id { get; init; }
                    public string FileName { get; init; } = string.Empty;
                    public string Url { get; init; } = string.Empty;
                    public long Size { get; init; }
                    public string ContentType { get; init; } = string.Empty;
                    public DateTime CreatedAt { get; init; }
                }
                """,
                namespace);
    }

    private String generateInterface() {
        return String.format(
                """
                namespace %s.Services.Storage;

                /// <summary>
                /// Storage backend interface.
                /// </summary>
                public interface IStorageBackend
                {
                    /// <summary>
                    /// Upload a file.
                    /// </summary>
                    Task<string> UploadAsync(string key, Stream data, string contentType);

                    /// <summary>
                    /// Download a file.
                    /// </summary>
                    Task<Stream> DownloadAsync(string key);

                    /// <summary>
                    /// Delete a file.
                    /// </summary>
                    Task DeleteAsync(string key);

                    /// <summary>
                    /// Get file URL.
                    /// </summary>
                    string GetUrl(string key);
                }
                """,
                namespace);
    }

    private String generateLocalStorage() {
        return String.format(
                """
                using Microsoft.Extensions.Options;
                using %s.Configuration;

                namespace %s.Services.Storage;

                /// <summary>
                /// Local filesystem storage backend.
                /// </summary>
                public class LocalStorageService : IStorageBackend
                {
                    private readonly LocalStorageSettings _settings;
                    private readonly ILogger<LocalStorageService> _logger;

                    public LocalStorageService(
                        IOptions<StorageSettings> settings,
                        ILogger<LocalStorageService> logger)
                    {
                        _settings = settings.Value.Local ?? new LocalStorageSettings();
                        _logger = logger;

                        // Ensure directory exists
                        Directory.CreateDirectory(_settings.Path);
                    }

                    /// <inheritdoc />
                    public async Task<string> UploadAsync(string key, Stream data, string contentType)
                    {
                        var filePath = Path.Combine(_settings.Path, key);
                        var directory = Path.GetDirectoryName(filePath);

                        if (!string.IsNullOrEmpty(directory))
                        {
                            Directory.CreateDirectory(directory);
                        }

                        await using var fileStream = File.Create(filePath);
                        await data.CopyToAsync(fileStream);

                        _logger.LogInformation("File uploaded to local storage: {Key}", key);

                        return GetUrl(key);
                    }

                    /// <inheritdoc />
                    public Task<Stream> DownloadAsync(string key)
                    {
                        var filePath = Path.Combine(_settings.Path, key);

                        if (!File.Exists(filePath))
                        {
                            throw new FileNotFoundException("File not found", key);
                        }

                        return Task.FromResult<Stream>(File.OpenRead(filePath));
                    }

                    /// <inheritdoc />
                    public Task DeleteAsync(string key)
                    {
                        var filePath = Path.Combine(_settings.Path, key);

                        if (File.Exists(filePath))
                        {
                            File.Delete(filePath);
                            _logger.LogInformation("File deleted from local storage: {Key}", key);
                        }

                        return Task.CompletedTask;
                    }

                    /// <inheritdoc />
                    public string GetUrl(string key)
                    {
                        return $"{_settings.BaseUrl}/{key}";
                    }
                }
                """,
                namespace, namespace);
    }

    private String generateService(boolean useS3, boolean useAzure) {
        StringBuilder backendSwitch = new StringBuilder();
        backendSwitch.append(
                "            \"local\" or _ =>"
                        + " _serviceProvider.GetRequiredService<LocalStorageService>()");

        if (useS3) {
            backendSwitch.insert(
                    0,
                    "            \"s3\" =>"
                            + " _serviceProvider.GetRequiredService<S3StorageService>(),\n");
        }
        if (useAzure) {
            backendSwitch.insert(
                    0,
                    "            \"azure\" =>"
                            + " _serviceProvider.GetRequiredService<AzureBlobStorageService>(),\n");
        }

        return String.format(
                """
                using Microsoft.EntityFrameworkCore;
                using Microsoft.Extensions.Options;
                using %1$s.Configuration;
                using %1$s.Data;
                using %1$s.DTOs;
                using %1$s.Models;

                namespace %1$s.Services.Storage;

                /// <summary>
                /// File storage service.
                /// </summary>
                public class FileStorageService
                {
                    private readonly ApplicationDbContext _context;
                    private readonly StorageSettings _settings;
                    private readonly IStorageBackend _backend;
                    private readonly IServiceProvider _serviceProvider;
                    private readonly ILogger<FileStorageService> _logger;

                    public FileStorageService(
                        ApplicationDbContext context,
                        IOptions<StorageSettings> settings,
                        IServiceProvider serviceProvider,
                        ILogger<FileStorageService> logger)
                    {
                        _context = context;
                        _settings = settings.Value;
                        _serviceProvider = serviceProvider;
                        _logger = logger;
                        _backend = GetBackend();
                    }

                    private IStorageBackend GetBackend()
                    {
                        return _settings.StorageType?.ToLower() switch
                        {
                %2$s
                        };
                    }

                    /// <summary>
                    /// Upload a file.
                    /// </summary>
                    public async Task<FileUploadResponse> UploadAsync(IFormFile file, Guid? userId = null)
                    {
                        // Validate file size
                        if (file.Length > _settings.MaxFileSize)
                        {
                            throw new InvalidOperationException($"File size exceeds maximum of {_settings.MaxFileSize} bytes");
                        }

                        // Validate extension
                        var extension = Path.GetExtension(file.FileName)?.TrimStart('.').ToLower();
                        if (string.IsNullOrEmpty(extension) || !_settings.AllowedExtensions.Contains(extension))
                        {
                            throw new InvalidOperationException($"File extension '{extension}' is not allowed");
                        }

                        // Generate unique key
                        var fileId = Guid.NewGuid();
                        var key = $"{DateTime.UtcNow:yyyy/MM/dd}/{fileId}.{extension}";

                        // Upload to backend
                        await using var stream = file.OpenReadStream();
                        var url = await _backend.UploadAsync(key, stream, file.ContentType);

                        // Save metadata
                        var metadata = new FileMetadata
                        {
                            Id = fileId,
                            FileName = file.FileName,
                            StorageKey = key,
                            ContentType = file.ContentType,
                            Size = file.Length,
                            Url = url,
                            UserId = userId
                        };

                        _context.FileMetadata.Add(metadata);
                        await _context.SaveChangesAsync();

                        _logger.LogInformation("File uploaded: {FileName} -> {Key}", file.FileName, key);

                        return new FileUploadResponse
                        {
                            Id = fileId,
                            FileName = file.FileName,
                            Url = url,
                            Size = file.Length,
                            ContentType = file.ContentType
                        };
                    }

                    /// <summary>
                    /// Get file info.
                    /// </summary>
                    public async Task<FileInfoResponse?> GetInfoAsync(Guid id)
                    {
                        var metadata = await _context.FileMetadata.FindAsync(id);
                        if (metadata == null) return null;

                        return new FileInfoResponse
                        {
                            Id = metadata.Id,
                            FileName = metadata.FileName,
                            Url = metadata.Url,
                            Size = metadata.Size,
                            ContentType = metadata.ContentType,
                            CreatedAt = metadata.CreatedAt
                        };
                    }

                    /// <summary>
                    /// Download a file.
                    /// </summary>
                    public async Task<(Stream Stream, string FileName, string ContentType)?> DownloadAsync(Guid id)
                    {
                        var metadata = await _context.FileMetadata.FindAsync(id);
                        if (metadata == null) return null;

                        var stream = await _backend.DownloadAsync(metadata.StorageKey);
                        return (stream, metadata.FileName, metadata.ContentType);
                    }

                    /// <summary>
                    /// Delete a file.
                    /// </summary>
                    public async Task<bool> DeleteAsync(Guid id)
                    {
                        var metadata = await _context.FileMetadata.FindAsync(id);
                        if (metadata == null) return false;

                        await _backend.DeleteAsync(metadata.StorageKey);
                        _context.FileMetadata.Remove(metadata);
                        await _context.SaveChangesAsync();

                        _logger.LogInformation("File deleted: {Id}", id);
                        return true;
                    }
                }
                """,
                namespace, backendSwitch.toString());
    }

    private String generateController() {
        return String.format(
                """
                using Microsoft.AspNetCore.Mvc;
                using %1$s.DTOs;
                using %1$s.Services.Storage;

                namespace %1$s.Controllers;

                /// <summary>
                /// File storage endpoints.
                /// </summary>
                [ApiController]
                [Route("api/files")]
                public class FilesController : ControllerBase
                {
                    private readonly FileStorageService _storageService;
                    private readonly ILogger<FilesController> _logger;

                    public FilesController(
                        FileStorageService storageService,
                        ILogger<FilesController> logger)
                    {
                        _storageService = storageService;
                        _logger = logger;
                    }

                    /// <summary>
                    /// Upload a file.
                    /// </summary>
                    [HttpPost("upload")]
                    [ProducesResponseType(typeof(FileUploadResponse), StatusCodes.Status200OK)]
                    [ProducesResponseType(StatusCodes.Status400BadRequest)]
                    public async Task<ActionResult<FileUploadResponse>> Upload(IFormFile file)
                    {
                        if (file == null || file.Length == 0)
                        {
                            return BadRequest("No file provided");
                        }

                        try
                        {
                            var response = await _storageService.UploadAsync(file);
                            return Ok(response);
                        }
                        catch (InvalidOperationException ex)
                        {
                            return BadRequest(ex.Message);
                        }
                    }

                    /// <summary>
                    /// Get file info.
                    /// </summary>
                    [HttpGet("{id:guid}")]
                    [ProducesResponseType(typeof(FileInfoResponse), StatusCodes.Status200OK)]
                    [ProducesResponseType(StatusCodes.Status404NotFound)]
                    public async Task<ActionResult<FileInfoResponse>> GetInfo(Guid id)
                    {
                        var info = await _storageService.GetInfoAsync(id);
                        if (info == null)
                        {
                            return NotFound();
                        }
                        return Ok(info);
                    }

                    /// <summary>
                    /// Download a file.
                    /// </summary>
                    [HttpGet("{id:guid}/download")]
                    [ProducesResponseType(StatusCodes.Status200OK)]
                    [ProducesResponseType(StatusCodes.Status404NotFound)]
                    public async Task<IActionResult> Download(Guid id)
                    {
                        var result = await _storageService.DownloadAsync(id);
                        if (result == null)
                        {
                            return NotFound();
                        }

                        var (stream, fileName, contentType) = result.Value;
                        return File(stream, contentType, fileName);
                    }

                    /// <summary>
                    /// Delete a file.
                    /// </summary>
                    [HttpDelete("{id:guid}")]
                    [ProducesResponseType(StatusCodes.Status204NoContent)]
                    [ProducesResponseType(StatusCodes.Status404NotFound)]
                    public async Task<IActionResult> Delete(Guid id)
                    {
                        var deleted = await _storageService.DeleteAsync(id);
                        return deleted ? NoContent() : NotFound();
                    }
                }
                """,
                namespace);
    }

    private String generateConfig(boolean useS3, boolean useAzure) {
        StringBuilder extraConfigs = new StringBuilder();

        if (useS3) {
            extraConfigs.append(
                    """

                        /// <summary>
                        /// S3 storage configuration.
                        /// </summary>
                        public S3StorageSettings? S3 { get; set; }
                    """);
        }

        if (useAzure) {
            extraConfigs.append(
                    """

                        /// <summary>
                        /// Azure Blob storage configuration.
                        /// </summary>
                        public AzureStorageSettings? Azure { get; set; }
                    """);
        }

        StringBuilder extraClasses = new StringBuilder();

        if (useS3) {
            extraClasses.append(
                    """

                    /// <summary>
                    /// S3 storage settings.
                    /// </summary>
                    public class S3StorageSettings
                    {
                        public string BucketName { get; set; } = string.Empty;
                        public string Region { get; set; } = "us-east-1";
                        public string AccessKeyId { get; set; } = string.Empty;
                        public string SecretAccessKey { get; set; } = string.Empty;
                        public string? ServiceUrl { get; set; }
                    }
                    """);
        }

        if (useAzure) {
            extraClasses.append(
                    """

                    /// <summary>
                    /// Azure Blob storage settings.
                    /// </summary>
                    public class AzureStorageSettings
                    {
                        public string ConnectionString { get; set; } = string.Empty;
                        public string ContainerName { get; set; } = string.Empty;
                    }
                    """);
        }

        return String.format(
                """
                namespace %s.Configuration;

                /// <summary>
                /// Storage configuration settings.
                /// </summary>
                public class StorageSettings
                {
                    /// <summary>
                    /// Storage type: local, s3, azure.
                    /// </summary>
                    public string StorageType { get; set; } = "local";

                    /// <summary>
                    /// Maximum file size in bytes.
                    /// </summary>
                    public long MaxFileSize { get; set; } = 10 * 1024 * 1024; // 10MB

                    /// <summary>
                    /// Allowed file extensions.
                    /// </summary>
                    public List<string> AllowedExtensions { get; set; } = new()
                    {
                        "jpg", "jpeg", "png", "gif", "webp",
                        "pdf", "doc", "docx", "xls", "xlsx",
                        "txt", "csv", "zip"
                    };

                    /// <summary>
                    /// Local storage configuration.
                    /// </summary>
                    public LocalStorageSettings? Local { get; set; }
                %s
                }

                /// <summary>
                /// Local storage settings.
                /// </summary>
                public class LocalStorageSettings
                {
                    public string Path { get; set; } = "./uploads";
                    public string BaseUrl { get; set; } = "http://localhost:5000/files";
                }
                %s
                """,
                namespace, extraConfigs.toString(), extraClasses.toString());
    }

    private String generateS3Storage() {
        return String.format(
                """
                using Amazon.S3;
                using Amazon.S3.Model;
                using Microsoft.Extensions.Options;
                using %s.Configuration;

                namespace %s.Services.Storage;

                /// <summary>
                /// AWS S3 storage backend.
                /// </summary>
                public class S3StorageService : IStorageBackend
                {
                    private readonly S3StorageSettings _settings;
                    private readonly IAmazonS3 _s3Client;
                    private readonly ILogger<S3StorageService> _logger;

                    public S3StorageService(
                        IOptions<StorageSettings> settings,
                        IAmazonS3 s3Client,
                        ILogger<S3StorageService> logger)
                    {
                        _settings = settings.Value.S3 ?? throw new InvalidOperationException("S3 not configured");
                        _s3Client = s3Client;
                        _logger = logger;
                    }

                    /// <inheritdoc />
                    public async Task<string> UploadAsync(string key, Stream data, string contentType)
                    {
                        var request = new PutObjectRequest
                        {
                            BucketName = _settings.BucketName,
                            Key = key,
                            InputStream = data,
                            ContentType = contentType
                        };

                        await _s3Client.PutObjectAsync(request);
                        _logger.LogInformation("File uploaded to S3: {Key}", key);

                        return GetUrl(key);
                    }

                    /// <inheritdoc />
                    public async Task<Stream> DownloadAsync(string key)
                    {
                        var response = await _s3Client.GetObjectAsync(_settings.BucketName, key);
                        return response.ResponseStream;
                    }

                    /// <inheritdoc />
                    public async Task DeleteAsync(string key)
                    {
                        await _s3Client.DeleteObjectAsync(_settings.BucketName, key);
                        _logger.LogInformation("File deleted from S3: {Key}", key);
                    }

                    /// <inheritdoc />
                    public string GetUrl(string key)
                    {
                        return $"https://{_settings.BucketName}.s3.{_settings.Region}.amazonaws.com/{key}";
                    }
                }
                """,
                namespace, namespace);
    }

    private String generateAzureStorage() {
        return String.format(
                """
                using Azure.Storage.Blobs;
                using Azure.Storage.Blobs.Models;
                using Microsoft.Extensions.Options;
                using %s.Configuration;

                namespace %s.Services.Storage;

                /// <summary>
                /// Azure Blob storage backend.
                /// </summary>
                public class AzureBlobStorageService : IStorageBackend
                {
                    private readonly AzureStorageSettings _settings;
                    private readonly BlobContainerClient _containerClient;
                    private readonly ILogger<AzureBlobStorageService> _logger;

                    public AzureBlobStorageService(
                        IOptions<StorageSettings> settings,
                        ILogger<AzureBlobStorageService> logger)
                    {
                        _settings = settings.Value.Azure ?? throw new InvalidOperationException("Azure Blob not configured");
                        _logger = logger;

                        var blobServiceClient = new BlobServiceClient(_settings.ConnectionString);
                        _containerClient = blobServiceClient.GetBlobContainerClient(_settings.ContainerName);
                        _containerClient.CreateIfNotExists();
                    }

                    /// <inheritdoc />
                    public async Task<string> UploadAsync(string key, Stream data, string contentType)
                    {
                        var blobClient = _containerClient.GetBlobClient(key);

                        var options = new BlobUploadOptions
                        {
                            HttpHeaders = new BlobHttpHeaders { ContentType = contentType }
                        };

                        await blobClient.UploadAsync(data, options);
                        _logger.LogInformation("File uploaded to Azure Blob: {Key}", key);

                        return GetUrl(key);
                    }

                    /// <inheritdoc />
                    public async Task<Stream> DownloadAsync(string key)
                    {
                        var blobClient = _containerClient.GetBlobClient(key);
                        var response = await blobClient.DownloadStreamingAsync();
                        return response.Value.Content;
                    }

                    /// <inheritdoc />
                    public async Task DeleteAsync(string key)
                    {
                        var blobClient = _containerClient.GetBlobClient(key);
                        await blobClient.DeleteIfExistsAsync();
                        _logger.LogInformation("File deleted from Azure Blob: {Key}", key);
                    }

                    /// <inheritdoc />
                    public string GetUrl(string key)
                    {
                        var blobClient = _containerClient.GetBlobClient(key);
                        return blobClient.Uri.ToString();
                    }
                }
                """,
                namespace, namespace);
    }
}
