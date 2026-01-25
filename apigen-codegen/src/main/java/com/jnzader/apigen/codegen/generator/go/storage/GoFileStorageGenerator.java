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
package com.jnzader.apigen.codegen.generator.go.storage;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Generates file storage service code for Go/Gin applications.
 *
 * @author APiGen
 * @since 2.13.0
 */
@SuppressWarnings({
    "java:S2479",
    "java:S1192",
    "java:S3400"
}) // S2479: Literal tabs for Go code; S1192: template strings; S3400: template methods return
// constants
public class GoFileStorageGenerator {

    private final String moduleName;

    public GoFileStorageGenerator(String moduleName) {
        this.moduleName = moduleName;
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

        files.put("internal/storage/file_handler.go", generateHandler());
        files.put("internal/storage/file_service.go", generateService());
        files.put("internal/storage/storage_interface.go", generateInterface());
        files.put("internal/storage/local_storage.go", generateLocalStorage());
        files.put("internal/models/stored_file.go", generateModel());
        files.put("internal/dto/file_dto.go", generateDto());

        if (useS3) {
            files.put("internal/storage/s3_storage.go", generateS3Storage());
        }
        if (useAzure) {
            files.put("internal/storage/azure_storage.go", generateAzureStorage());
        }

        return files;
    }

    private String generateHandler() {
        return String.format(
                """
                package storage

                import (
                \t"net/http"

                \t"%s/internal/dto"

                \t"github.com/gin-gonic/gin"
                \t"github.com/google/uuid"
                )

                // FileHandler handles file upload endpoints.
                type FileHandler struct {
                \tservice *FileService
                }

                // NewFileHandler creates a new file handler.
                func NewFileHandler(service *FileService) *FileHandler {
                \treturn &FileHandler{service: service}
                }

                // RegisterRoutes registers file handling routes.
                func (h *FileHandler) RegisterRoutes(rg *gin.RouterGroup) {
                \tfiles := rg.Group("/files")
                \t{
                \t\tfiles.POST("/upload", h.Upload)
                \t\tfiles.GET("", h.List)
                \t\tfiles.GET("/:id", h.GetMetadata)
                \t\tfiles.GET("/:id/download", h.Download)
                \t\tfiles.DELETE("/:id", h.Delete)
                \t}
                }

                // Upload godoc
                // @Summary Upload a file
                // @Tags Files
                // @Accept multipart/form-data
                // @Produce json
                // @Param file formData file true "File to upload"
                // @Param directory formData string false "Directory to store file in"
                // @Success 201 {object} dto.StoredFileResponse
                // @Failure 400 {object} dto.ErrorResponse
                // @Router /files/upload [post]
                func (h *FileHandler) Upload(c *gin.Context) {
                \tfile, header, err := c.Request.FormFile("file")
                \tif err != nil {
                \t\tc.JSON(http.StatusBadRequest, dto.ErrorResponse{Error: "No file provided"})
                \t\treturn
                \t}
                \tdefer file.Close()

                \tdirectory := c.PostForm("directory")
                \tif directory == "" {
                \t\tdirectory = "uploads"
                \t}

                \tresult, err := h.service.Store(file, header, directory)
                \tif err != nil {
                \t\tc.JSON(http.StatusInternalServerError, dto.ErrorResponse{Error: err.Error()})
                \t\treturn
                \t}

                \tc.JSON(http.StatusCreated, result)
                }

                // GetMetadata godoc
                // @Summary Get file metadata
                // @Tags Files
                // @Produce json
                // @Param id path string true "File ID"
                // @Success 200 {object} dto.StoredFileResponse
                // @Failure 404 {object} dto.ErrorResponse
                // @Router /files/{id} [get]
                func (h *FileHandler) GetMetadata(c *gin.Context) {
                \tid, err := uuid.Parse(c.Param("id"))
                \tif err != nil {
                \t\tc.JSON(http.StatusBadRequest, dto.ErrorResponse{Error: "Invalid file ID"})
                \t\treturn
                \t}

                \tfile, err := h.service.GetMetadata(id)
                \tif err != nil {
                \t\tc.JSON(http.StatusNotFound, dto.ErrorResponse{Error: "File not found"})
                \t\treturn
                \t}

                \tc.JSON(http.StatusOK, file)
                }

                // Download godoc
                // @Summary Download a file
                // @Tags Files
                // @Produce octet-stream
                // @Param id path string true "File ID"
                // @Success 200 {file} binary
                // @Failure 404 {object} dto.ErrorResponse
                // @Router /files/{id}/download [get]
                func (h *FileHandler) Download(c *gin.Context) {
                \tid, err := uuid.Parse(c.Param("id"))
                \tif err != nil {
                \t\tc.JSON(http.StatusBadRequest, dto.ErrorResponse{Error: "Invalid file ID"})
                \t\treturn
                \t}

                \treader, metadata, err := h.service.Download(id)
                \tif err != nil {
                \t\tc.JSON(http.StatusNotFound, dto.ErrorResponse{Error: "File not found"})
                \t\treturn
                \t}
                \tdefer reader.Close()

                \tc.Header("Content-Disposition", "attachment; filename="+metadata.OriginalName)
                \tc.Header("Content-Type", metadata.MimeType)
                \tc.DataFromReader(http.StatusOK, metadata.Size, metadata.MimeType, reader, nil)
                }

                // Delete godoc
                // @Summary Delete a file
                // @Tags Files
                // @Param id path string true "File ID"
                // @Success 204 "File deleted"
                // @Failure 404 {object} dto.ErrorResponse
                // @Router /files/{id} [delete]
                func (h *FileHandler) Delete(c *gin.Context) {
                \tid, err := uuid.Parse(c.Param("id"))
                \tif err != nil {
                \t\tc.JSON(http.StatusBadRequest, dto.ErrorResponse{Error: "Invalid file ID"})
                \t\treturn
                \t}

                \tif err := h.service.Delete(id); err != nil {
                \t\tc.JSON(http.StatusNotFound, dto.ErrorResponse{Error: "File not found"})
                \t\treturn
                \t}

                \tc.Status(http.StatusNoContent)
                }

                // List godoc
                // @Summary List all files
                // @Tags Files
                // @Produce json
                // @Param directory query string false "Directory filter"
                // @Success 200 {array} dto.StoredFileResponse
                // @Router /files [get]
                func (h *FileHandler) List(c *gin.Context) {
                \tdirectory := c.Query("directory")
                \tfiles, err := h.service.List(directory)
                \tif err != nil {
                \t\tc.JSON(http.StatusInternalServerError, dto.ErrorResponse{Error: err.Error()})
                \t\treturn
                \t}

                \tc.JSON(http.StatusOK, files)
                }
                """,
                moduleName);
    }

    private String generateService() {
        return String.format(
                """
                package storage

                import (
                \t"fmt"
                \t"io"
                \t"log"
                \t"mime/multipart"
                \t"path/filepath"

                \t"%s/internal/dto"
                \t"%s/internal/models"

                \t"github.com/google/uuid"
                \t"gorm.io/gorm"
                )

                // FileService handles file storage operations.
                type FileService struct {
                \tdb      *gorm.DB
                \tstorage StorageBackend
                }

                // NewFileService creates a new file service.
                func NewFileService(db *gorm.DB, storage StorageBackend) *FileService {
                \treturn &FileService{
                \t\tdb:      db,
                \t\tstorage: storage,
                \t}
                }

                // Store uploads a file and saves metadata.
                func (s *FileService) Store(file multipart.File, header *multipart.FileHeader, directory string) (*dto.StoredFileResponse, error) {
                \t// Generate unique filename
                \text := filepath.Ext(header.Filename)
                \tfilename := uuid.New().String() + ext
                \tpath := directory + "/" + filename

                \t// Upload to storage backend
                \tif err := s.storage.Upload(path, file); err != nil {
                \t\treturn nil, fmt.Errorf("failed to upload file: %%w", err)
                \t}

                \t// Detect MIME type
                \tmimeType := header.Header.Get("Content-Type")
                \tif mimeType == "" {
                \t\tmimeType = "application/octet-stream"
                \t}

                \t// Save metadata to database
                \tstoredFile := models.StoredFile{
                \t\tOriginalName: header.Filename,
                \t\tFilename:     filename,
                \t\tPath:         path,
                \t\tMimeType:     mimeType,
                \t\tSize:         header.Size,
                \t\tStorageType:  s.storage.Type(),
                \t}

                \tif err := s.db.Create(&storedFile).Error; err != nil {
                \t\t// Try to clean up uploaded file
                \t\t_ = s.storage.Delete(path)
                \t\treturn nil, fmt.Errorf("failed to save file metadata: %%w", err)
                \t}

                \tlog.Printf("File uploaded: %%s", path)
                \treturn storedFile.ToResponse(s.storage.GetURL(path)), nil
                }

                // GetMetadata returns file metadata.
                func (s *FileService) GetMetadata(id uuid.UUID) (*dto.StoredFileResponse, error) {
                \tvar file models.StoredFile
                \tif err := s.db.First(&file, id).Error; err != nil {
                \t\treturn nil, err
                \t}
                \treturn file.ToResponse(s.storage.GetURL(file.Path)), nil
                }

                // Download returns a file reader.
                func (s *FileService) Download(id uuid.UUID) (io.ReadCloser, *models.StoredFile, error) {
                \tvar file models.StoredFile
                \tif err := s.db.First(&file, id).Error; err != nil {
                \t\treturn nil, nil, err
                \t}

                \treader, err := s.storage.Download(file.Path)
                \tif err != nil {
                \t\treturn nil, nil, err
                \t}

                \treturn reader, &file, nil
                }

                // Delete removes a file from storage and database.
                func (s *FileService) Delete(id uuid.UUID) error {
                \tvar file models.StoredFile
                \tif err := s.db.First(&file, id).Error; err != nil {
                \t\treturn err
                \t}

                \t// Delete from storage
                \tif err := s.storage.Delete(file.Path); err != nil {
                \t\tlog.Printf("Warning: failed to delete file from storage: %%v", err)
                \t}

                \t// Delete from database
                \tif err := s.db.Delete(&file).Error; err != nil {
                \t\treturn err
                \t}

                \tlog.Printf("File deleted: %%s", file.Path)
                \treturn nil
                }

                // List returns all files, optionally filtered by directory.
                func (s *FileService) List(directory string) ([]*dto.StoredFileResponse, error) {
                \tvar files []models.StoredFile
                \tquery := s.db.Order("created_at DESC")

                \tif directory != "" {
                \t\tquery = query.Where("path LIKE ?", directory+"/%%")
                \t}

                \tif err := query.Find(&files).Error; err != nil {
                \t\treturn nil, err
                \t}

                \tresponses := make([]*dto.StoredFileResponse, len(files))
                \tfor i, file := range files {
                \t\tresponses[i] = file.ToResponse(s.storage.GetURL(file.Path))
                \t}

                \treturn responses, nil
                }

                // GetTemporaryURL returns a temporary signed URL for a file.
                func (s *FileService) GetTemporaryURL(id uuid.UUID, expirationMinutes int) (string, error) {
                \tvar file models.StoredFile
                \tif err := s.db.First(&file, id).Error; err != nil {
                \t\treturn "", err
                \t}

                \treturn s.storage.GetTemporaryURL(file.Path, expirationMinutes)
                }
                """,
                moduleName, moduleName);
    }

    private String generateInterface() {
        return """
        package storage

        import (
        \t"io"
        \t"mime/multipart"
        )

        // StorageBackend defines the interface for file storage backends.
        type StorageBackend interface {
        \t// Upload uploads a file to the storage backend.
        \tUpload(path string, file multipart.File) error

        \t// Download returns a reader for the file at the given path.
        \tDownload(path string) (io.ReadCloser, error)

        \t// Delete removes a file from the storage backend.
        \tDelete(path string) error

        \t// Exists checks if a file exists at the given path.
        \tExists(path string) bool

        \t// GetURL returns the public URL for a file.
        \tGetURL(path string) string

        \t// GetTemporaryURL returns a temporary signed URL for a file.
        \tGetTemporaryURL(path string, expirationMinutes int) (string, error)

        \t// Type returns the storage backend type.
        \tType() string
        }
        """;
    }

    private String generateLocalStorage() {
        return """
        package storage

        import (
        \t"errors"
        \t"io"
        \t"mime/multipart"
        \t"os"
        \t"path/filepath"

        \t"github.com/spf13/viper"
        )

        // LocalStorage implements StorageBackend for local filesystem storage.
        type LocalStorage struct {
        \tbasePath string
        \tbaseURL  string
        }

        // NewLocalStorage creates a new local storage backend.
        func NewLocalStorage() *LocalStorage {
        \tbasePath := viper.GetString("storage.local.base_path")
        \tif basePath == "" {
        \t\tbasePath = "./uploads"
        \t}

        \tbaseURL := viper.GetString("storage.local.base_url")
        \tif baseURL == "" {
        \t\tbaseURL = "/files"
        \t}

        \t// Ensure base directory exists
        \t_ = os.MkdirAll(basePath, 0755)

        \treturn &LocalStorage{
        \t\tbasePath: basePath,
        \t\tbaseURL:  baseURL,
        \t}
        }

        // Upload uploads a file to local storage.
        func (s *LocalStorage) Upload(path string, file multipart.File) error {
        \tfullPath := filepath.Join(s.basePath, path)

        \t// Ensure directory exists
        \tdir := filepath.Dir(fullPath)
        \tif err := os.MkdirAll(dir, 0755); err != nil {
        \t\treturn err
        \t}

        \t// Create destination file
        \tdst, err := os.Create(fullPath)
        \tif err != nil {
        \t\treturn err
        \t}
        \tdefer dst.Close()

        \t// Copy content
        \t_, err = io.Copy(dst, file)
        \treturn err
        }

        // Download returns a reader for a local file.
        func (s *LocalStorage) Download(path string) (io.ReadCloser, error) {
        \tfullPath := filepath.Join(s.basePath, path)
        \treturn os.Open(fullPath)
        }

        // Delete removes a file from local storage.
        func (s *LocalStorage) Delete(path string) error {
        \tfullPath := filepath.Join(s.basePath, path)
        \treturn os.Remove(fullPath)
        }

        // Exists checks if a file exists in local storage.
        func (s *LocalStorage) Exists(path string) bool {
        \tfullPath := filepath.Join(s.basePath, path)
        \t_, err := os.Stat(fullPath)
        \treturn err == nil
        }

        // GetURL returns the URL for a local file.
        func (s *LocalStorage) GetURL(path string) string {
        \treturn s.baseURL + "/" + path
        }

        // GetTemporaryURL returns the URL (local storage doesn't support signed URLs).
        func (s *LocalStorage) GetTemporaryURL(path string, expirationMinutes int) (string, error) {
        \treturn "", errors.New("temporary URLs not supported for local storage")
        }

        // Type returns the storage type.
        func (s *LocalStorage) Type() string {
        \treturn "local"
        }
        """;
    }

    private String generateModel() {
        return String.format(
                """
                package models

                import (
                \t"time"

                \t"%s/internal/dto"

                \t"github.com/google/uuid"
                \t"gorm.io/gorm"
                )

                // StoredFile represents a stored file in the database.
                type StoredFile struct {
                \tID           uuid.UUID `gorm:"type:uuid;primaryKey"`
                \tOriginalName string    `gorm:"type:varchar(255);not null"`
                \tFilename     string    `gorm:"type:varchar(255);not null"`
                \tPath         string    `gorm:"type:varchar(512);not null;index"`
                \tMimeType     string    `gorm:"type:varchar(100);not null;index"`
                \tSize         int64     `gorm:"not null"`
                \tStorageType  string    `gorm:"type:varchar(50);not null;default:'local'"`
                \tCreatedAt    time.Time
                \tUpdatedAt    time.Time
                }

                // BeforeCreate sets the UUID before creating the record.
                func (f *StoredFile) BeforeCreate(tx *gorm.DB) error {
                \tif f.ID == uuid.Nil {
                \t\tf.ID = uuid.New()
                \t}
                \treturn nil
                }

                // TableName returns the table name for GORM.
                func (StoredFile) TableName() string {
                \treturn "stored_files"
                }

                // ToResponse converts the model to a response DTO.
                func (f *StoredFile) ToResponse(url string) *dto.StoredFileResponse {
                \treturn &dto.StoredFileResponse{
                \t\tID:           f.ID.String(),
                \t\tOriginalName: f.OriginalName,
                \t\tMimeType:     f.MimeType,
                \t\tSize:         f.Size,
                \t\tSizeHuman:    dto.FormatFileSize(f.Size),
                \t\tURL:          url,
                \t\tCreatedAt:    f.CreatedAt.Format(time.RFC3339),
                \t}
                }
                """,
                moduleName);
    }

    private String generateDto() {
        return """
        package dto

        import "fmt"

        // StoredFileResponse represents a stored file response.
        type StoredFileResponse struct {
        \tID           string `json:"id"`
        \tOriginalName string `json:"original_name"`
        \tMimeType     string `json:"mime_type"`
        \tSize         int64  `json:"size"`
        \tSizeHuman    string `json:"size_human"`
        \tURL          string `json:"url"`
        \tCreatedAt    string `json:"created_at"`
        }

        // FileUploadRequest represents a file upload request.
        type FileUploadRequest struct {
        \tDirectory string `form:"directory"`
        }

        // FormatFileSize formats a file size in bytes to a human-readable string.
        func FormatFileSize(bytes int64) string {
        \tconst unit = 1024
        \tif bytes < unit {
        \t\treturn fmt.Sprintf("%d B", bytes)
        \t}
        \tdiv, exp := int64(unit), 0
        \tfor n := bytes / unit; n >= unit; n /= unit {
        \t\tdiv *= unit
        \t\texp++
        \t}
        \treturn fmt.Sprintf("%.2f %cB", float64(bytes)/float64(div), "KMGTPE"[exp])
        }
        """;
    }

    private String generateS3Storage() {
        return """
        package storage

        import (
        \t"context"
        \t"io"
        \t"mime/multipart"
        \t"time"

        \t"github.com/aws/aws-sdk-go-v2/aws"
        \t"github.com/aws/aws-sdk-go-v2/config"
        \t"github.com/aws/aws-sdk-go-v2/service/s3"
        \t"github.com/spf13/viper"
        )

        // S3Storage implements StorageBackend for AWS S3.
        type S3Storage struct {
        \tclient *s3.Client
        \tbucket string
        \tregion string
        }

        // NewS3Storage creates a new S3 storage backend.
        func NewS3Storage() (*S3Storage, error) {
        \tregion := viper.GetString("storage.s3.region")
        \tif region == "" {
        \t\tregion = "us-east-1"
        \t}

        \tcfg, err := config.LoadDefaultConfig(context.Background(),
        \t\tconfig.WithRegion(region),
        \t)
        \tif err != nil {
        \t\treturn nil, err
        \t}

        \tclient := s3.NewFromConfig(cfg)
        \tbucket := viper.GetString("storage.s3.bucket")

        \treturn &S3Storage{
        \t\tclient: client,
        \t\tbucket: bucket,
        \t\tregion: region,
        \t}, nil
        }

        // Upload uploads a file to S3.
        func (s *S3Storage) Upload(path string, file multipart.File) error {
        \t_, err := s.client.PutObject(context.Background(), &s3.PutObjectInput{
        \t\tBucket: aws.String(s.bucket),
        \t\tKey:    aws.String(path),
        \t\tBody:   file,
        \t})
        \treturn err
        }

        // Download returns a reader for an S3 object.
        func (s *S3Storage) Download(path string) (io.ReadCloser, error) {
        \tresult, err := s.client.GetObject(context.Background(), &s3.GetObjectInput{
        \t\tBucket: aws.String(s.bucket),
        \t\tKey:    aws.String(path),
        \t})
        \tif err != nil {
        \t\treturn nil, err
        \t}
        \treturn result.Body, nil
        }

        // Delete removes a file from S3.
        func (s *S3Storage) Delete(path string) error {
        \t_, err := s.client.DeleteObject(context.Background(), &s3.DeleteObjectInput{
        \t\tBucket: aws.String(s.bucket),
        \t\tKey:    aws.String(path),
        \t})
        \treturn err
        }

        // Exists checks if a file exists in S3.
        func (s *S3Storage) Exists(path string) bool {
        \t_, err := s.client.HeadObject(context.Background(), &s3.HeadObjectInput{
        \t\tBucket: aws.String(s.bucket),
        \t\tKey:    aws.String(path),
        \t})
        \treturn err == nil
        }

        // GetURL returns the public URL for an S3 object.
        func (s *S3Storage) GetURL(path string) string {
        \treturn "https://" + s.bucket + ".s3." + s.region + ".amazonaws.com/" + path
        }

        // GetTemporaryURL returns a presigned URL for an S3 object.
        func (s *S3Storage) GetTemporaryURL(path string, expirationMinutes int) (string, error) {
        \tpresignClient := s3.NewPresignClient(s.client)
        \tresult, err := presignClient.PresignGetObject(context.Background(), &s3.GetObjectInput{
        \t\tBucket: aws.String(s.bucket),
        \t\tKey:    aws.String(path),
        \t}, s3.WithPresignExpires(time.Duration(expirationMinutes)*time.Minute))
        \tif err != nil {
        \t\treturn "", err
        \t}
        \treturn result.URL, nil
        }

        // Type returns the storage type.
        func (s *S3Storage) Type() string {
        \treturn "s3"
        }
        """;
    }

    private String generateAzureStorage() {
        return """
        package storage

        import (
        \t"context"
        \t"fmt"
        \t"io"
        \t"mime/multipart"
        \t"time"

        \t"github.com/Azure/azure-sdk-for-go/sdk/storage/azblob"
        \t"github.com/spf13/viper"
        )

        // AzureStorage implements StorageBackend for Azure Blob Storage.
        type AzureStorage struct {
        \tclient    *azblob.Client
        \tcontainer string
        \taccountName string
        }

        // NewAzureStorage creates a new Azure Blob storage backend.
        func NewAzureStorage() (*AzureStorage, error) {
        \taccountName := viper.GetString("storage.azure.account_name")
        \taccountKey := viper.GetString("storage.azure.account_key")
        \tcontainer := viper.GetString("storage.azure.container")

        \tif container == "" {
        \t\tcontainer = "files"
        \t}

        \tserviceURL := fmt.Sprintf("https://%s.blob.core.windows.net/", accountName)

        \tcred, err := azblob.NewSharedKeyCredential(accountName, accountKey)
        \tif err != nil {
        \t\treturn nil, err
        \t}

        \tclient, err := azblob.NewClientWithSharedKeyCredential(serviceURL, cred, nil)
        \tif err != nil {
        \t\treturn nil, err
        \t}

        \treturn &AzureStorage{
        \t\tclient:    client,
        \t\tcontainer: container,
        \t\taccountName: accountName,
        \t}, nil
        }

        // Upload uploads a file to Azure Blob Storage.
        func (s *AzureStorage) Upload(path string, file multipart.File) error {
        \t_, err := s.client.UploadStream(context.Background(), s.container, path, file, nil)
        \treturn err
        }

        // Download returns a reader for an Azure blob.
        func (s *AzureStorage) Download(path string) (io.ReadCloser, error) {
        \tresp, err := s.client.DownloadStream(context.Background(), s.container, path, nil)
        \tif err != nil {
        \t\treturn nil, err
        \t}
        \treturn resp.Body, nil
        }

        // Delete removes a blob from Azure Storage.
        func (s *AzureStorage) Delete(path string) error {
        \t_, err := s.client.DeleteBlob(context.Background(), s.container, path, nil)
        \treturn err
        }

        // Exists checks if a blob exists in Azure Storage.
        func (s *AzureStorage) Exists(path string) bool {
        \t_, err := s.client.DownloadStream(context.Background(), s.container, path, &azblob.DownloadStreamOptions{
        \t\tRange: azblob.HTTPRange{Offset: 0, Count: 1},
        \t})
        \treturn err == nil
        }

        // GetURL returns the public URL for an Azure blob.
        func (s *AzureStorage) GetURL(path string) string {
        \treturn fmt.Sprintf("https://%s.blob.core.windows.net/%s/%s", s.accountName, s.container, path)
        }

        // GetTemporaryURL returns a SAS URL for an Azure blob.
        func (s *AzureStorage) GetTemporaryURL(path string, expirationMinutes int) (string, error) {
        \t// Note: Full SAS URL generation requires service client with proper credentials
        \t// This is a simplified version - in production, use proper SAS token generation
        \texpiry := time.Now().Add(time.Duration(expirationMinutes) * time.Minute)
        \treturn fmt.Sprintf("%s?se=%s", s.GetURL(path), expiry.Format(time.RFC3339)), nil
        }

        // Type returns the storage type.
        func (s *AzureStorage) Type() string {
        \treturn "azure"
        }
        """;
    }
}
