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
                	"net/http"

                	"%s/internal/dto"

                	"github.com/gin-gonic/gin"
                	"github.com/google/uuid"
                )

                // FileHandler handles file upload endpoints.
                type FileHandler struct {
                	service *FileService
                }

                // NewFileHandler creates a new file handler.
                func NewFileHandler(service *FileService) *FileHandler {
                	return &FileHandler{service: service}
                }

                // RegisterRoutes registers file handling routes.
                func (h *FileHandler) RegisterRoutes(rg *gin.RouterGroup) {
                	files := rg.Group("/files")
                	{
                		files.POST("/upload", h.Upload)
                		files.GET("", h.List)
                		files.GET("/:id", h.GetMetadata)
                		files.GET("/:id/download", h.Download)
                		files.DELETE("/:id", h.Delete)
                	}
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
                	file, header, err := c.Request.FormFile("file")
                	if err != nil {
                		c.JSON(http.StatusBadRequest, dto.ErrorResponse{Error: "No file provided"})
                		return
                	}
                	defer file.Close()

                	directory := c.PostForm("directory")
                	if directory == "" {
                		directory = "uploads"
                	}

                	result, err := h.service.Store(file, header, directory)
                	if err != nil {
                		c.JSON(http.StatusInternalServerError, dto.ErrorResponse{Error: err.Error()})
                		return
                	}

                	c.JSON(http.StatusCreated, result)
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
                	id, err := uuid.Parse(c.Param("id"))
                	if err != nil {
                		c.JSON(http.StatusBadRequest, dto.ErrorResponse{Error: "Invalid file ID"})
                		return
                	}

                	file, err := h.service.GetMetadata(id)
                	if err != nil {
                		c.JSON(http.StatusNotFound, dto.ErrorResponse{Error: "File not found"})
                		return
                	}

                	c.JSON(http.StatusOK, file)
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
                	id, err := uuid.Parse(c.Param("id"))
                	if err != nil {
                		c.JSON(http.StatusBadRequest, dto.ErrorResponse{Error: "Invalid file ID"})
                		return
                	}

                	reader, metadata, err := h.service.Download(id)
                	if err != nil {
                		c.JSON(http.StatusNotFound, dto.ErrorResponse{Error: "File not found"})
                		return
                	}
                	defer reader.Close()

                	c.Header("Content-Disposition", "attachment; filename="+metadata.OriginalName)
                	c.Header("Content-Type", metadata.MimeType)
                	c.DataFromReader(http.StatusOK, metadata.Size, metadata.MimeType, reader, nil)
                }

                // Delete godoc
                // @Summary Delete a file
                // @Tags Files
                // @Param id path string true "File ID"
                // @Success 204 "File deleted"
                // @Failure 404 {object} dto.ErrorResponse
                // @Router /files/{id} [delete]
                func (h *FileHandler) Delete(c *gin.Context) {
                	id, err := uuid.Parse(c.Param("id"))
                	if err != nil {
                		c.JSON(http.StatusBadRequest, dto.ErrorResponse{Error: "Invalid file ID"})
                		return
                	}

                	if err := h.service.Delete(id); err != nil {
                		c.JSON(http.StatusNotFound, dto.ErrorResponse{Error: "File not found"})
                		return
                	}

                	c.Status(http.StatusNoContent)
                }

                // List godoc
                // @Summary List all files
                // @Tags Files
                // @Produce json
                // @Param directory query string false "Directory filter"
                // @Success 200 {array} dto.StoredFileResponse
                // @Router /files [get]
                func (h *FileHandler) List(c *gin.Context) {
                	directory := c.Query("directory")
                	files, err := h.service.List(directory)
                	if err != nil {
                		c.JSON(http.StatusInternalServerError, dto.ErrorResponse{Error: err.Error()})
                		return
                	}

                	c.JSON(http.StatusOK, files)
                }
                """,
                moduleName);
    }

    private String generateService() {
        return String.format(
                """
                package storage

                import (
                	"fmt"
                	"io"
                	"log"
                	"mime/multipart"
                	"path/filepath"

                	"%s/internal/dto"
                	"%s/internal/models"

                	"github.com/google/uuid"
                	"gorm.io/gorm"
                )

                // FileService handles file storage operations.
                type FileService struct {
                	db      *gorm.DB
                	storage StorageBackend
                }

                // NewFileService creates a new file service.
                func NewFileService(db *gorm.DB, storage StorageBackend) *FileService {
                	return &FileService{
                		db:      db,
                		storage: storage,
                	}
                }

                // Store uploads a file and saves metadata.
                func (s *FileService) Store(file multipart.File, header *multipart.FileHeader, directory string) (*dto.StoredFileResponse, error) {
                	// Generate unique filename
                	ext := filepath.Ext(header.Filename)
                	filename := uuid.New().String() + ext
                	path := directory + "/" + filename

                	// Upload to storage backend
                	if err := s.storage.Upload(path, file); err != nil {
                		return nil, fmt.Errorf("failed to upload file: %%w", err)
                	}

                	// Detect MIME type
                	mimeType := header.Header.Get("Content-Type")
                	if mimeType == "" {
                		mimeType = "application/octet-stream"
                	}

                	// Save metadata to database
                	storedFile := models.StoredFile{
                		OriginalName: header.Filename,
                		Filename:     filename,
                		Path:         path,
                		MimeType:     mimeType,
                		Size:         header.Size,
                		StorageType:  s.storage.Type(),
                	}

                	if err := s.db.Create(&storedFile).Error; err != nil {
                		// Try to clean up uploaded file
                		_ = s.storage.Delete(path)
                		return nil, fmt.Errorf("failed to save file metadata: %%w", err)
                	}

                	log.Printf("File uploaded: %%s", path)
                	return storedFile.ToResponse(s.storage.GetURL(path)), nil
                }

                // GetMetadata returns file metadata.
                func (s *FileService) GetMetadata(id uuid.UUID) (*dto.StoredFileResponse, error) {
                	var file models.StoredFile
                	if err := s.db.First(&file, id).Error; err != nil {
                		return nil, err
                	}
                	return file.ToResponse(s.storage.GetURL(file.Path)), nil
                }

                // Download returns a file reader.
                func (s *FileService) Download(id uuid.UUID) (io.ReadCloser, *models.StoredFile, error) {
                	var file models.StoredFile
                	if err := s.db.First(&file, id).Error; err != nil {
                		return nil, nil, err
                	}

                	reader, err := s.storage.Download(file.Path)
                	if err != nil {
                		return nil, nil, err
                	}

                	return reader, &file, nil
                }

                // Delete removes a file from storage and database.
                func (s *FileService) Delete(id uuid.UUID) error {
                	var file models.StoredFile
                	if err := s.db.First(&file, id).Error; err != nil {
                		return err
                	}

                	// Delete from storage
                	if err := s.storage.Delete(file.Path); err != nil {
                		log.Printf("Warning: failed to delete file from storage: %%v", err)
                	}

                	// Delete from database
                	if err := s.db.Delete(&file).Error; err != nil {
                		return err
                	}

                	log.Printf("File deleted: %%s", file.Path)
                	return nil
                }

                // List returns all files, optionally filtered by directory.
                func (s *FileService) List(directory string) ([]*dto.StoredFileResponse, error) {
                	var files []models.StoredFile
                	query := s.db.Order("created_at DESC")

                	if directory != "" {
                		query = query.Where("path LIKE ?", directory+"/%%")
                	}

                	if err := query.Find(&files).Error; err != nil {
                		return nil, err
                	}

                	responses := make([]*dto.StoredFileResponse, len(files))
                	for i, file := range files {
                		responses[i] = file.ToResponse(s.storage.GetURL(file.Path))
                	}

                	return responses, nil
                }

                // GetTemporaryURL returns a temporary signed URL for a file.
                func (s *FileService) GetTemporaryURL(id uuid.UUID, expirationMinutes int) (string, error) {
                	var file models.StoredFile
                	if err := s.db.First(&file, id).Error; err != nil {
                		return "", err
                	}

                	return s.storage.GetTemporaryURL(file.Path, expirationMinutes)
                }
                """,
                moduleName, moduleName);
    }

    private String generateInterface() {
        return """
        package storage

        import (
        	"io"
        	"mime/multipart"
        )

        // StorageBackend defines the interface for file storage backends.
        type StorageBackend interface {
        	// Upload uploads a file to the storage backend.
        	Upload(path string, file multipart.File) error

        	// Download returns a reader for the file at the given path.
        	Download(path string) (io.ReadCloser, error)

        	// Delete removes a file from the storage backend.
        	Delete(path string) error

        	// Exists checks if a file exists at the given path.
        	Exists(path string) bool

        	// GetURL returns the public URL for a file.
        	GetURL(path string) string

        	// GetTemporaryURL returns a temporary signed URL for a file.
        	GetTemporaryURL(path string, expirationMinutes int) (string, error)

        	// Type returns the storage backend type.
        	Type() string
        }
        """;
    }

    private String generateLocalStorage() {
        return """
        package storage

        import (
        	"errors"
        	"io"
        	"mime/multipart"
        	"os"
        	"path/filepath"

        	"github.com/spf13/viper"
        )

        // LocalStorage implements StorageBackend for local filesystem storage.
        type LocalStorage struct {
        	basePath string
        	baseURL  string
        }

        // NewLocalStorage creates a new local storage backend.
        func NewLocalStorage() *LocalStorage {
        	basePath := viper.GetString("storage.local.base_path")
        	if basePath == "" {
        		basePath = "./uploads"
        	}

        	baseURL := viper.GetString("storage.local.base_url")
        	if baseURL == "" {
        		baseURL = "/files"
        	}

        	// Ensure base directory exists
        	_ = os.MkdirAll(basePath, 0755)

        	return &LocalStorage{
        		basePath: basePath,
        		baseURL:  baseURL,
        	}
        }

        // Upload uploads a file to local storage.
        func (s *LocalStorage) Upload(path string, file multipart.File) error {
        	fullPath := filepath.Join(s.basePath, path)

        	// Ensure directory exists
        	dir := filepath.Dir(fullPath)
        	if err := os.MkdirAll(dir, 0755); err != nil {
        		return err
        	}

        	// Create destination file
        	dst, err := os.Create(fullPath)
        	if err != nil {
        		return err
        	}
        	defer dst.Close()

        	// Copy content
        	_, err = io.Copy(dst, file)
        	return err
        }

        // Download returns a reader for a local file.
        func (s *LocalStorage) Download(path string) (io.ReadCloser, error) {
        	fullPath := filepath.Join(s.basePath, path)
        	return os.Open(fullPath)
        }

        // Delete removes a file from local storage.
        func (s *LocalStorage) Delete(path string) error {
        	fullPath := filepath.Join(s.basePath, path)
        	return os.Remove(fullPath)
        }

        // Exists checks if a file exists in local storage.
        func (s *LocalStorage) Exists(path string) bool {
        	fullPath := filepath.Join(s.basePath, path)
        	_, err := os.Stat(fullPath)
        	return err == nil
        }

        // GetURL returns the URL for a local file.
        func (s *LocalStorage) GetURL(path string) string {
        	return s.baseURL + "/" + path
        }

        // GetTemporaryURL returns the URL (local storage doesn't support signed URLs).
        func (s *LocalStorage) GetTemporaryURL(path string, expirationMinutes int) (string, error) {
        	return "", errors.New("temporary URLs not supported for local storage")
        }

        // Type returns the storage type.
        func (s *LocalStorage) Type() string {
        	return "local"
        }
        """;
    }

    private String generateModel() {
        return String.format(
                """
                package models

                import (
                	"time"

                	"%s/internal/dto"

                	"github.com/google/uuid"
                	"gorm.io/gorm"
                )

                // StoredFile represents a stored file in the database.
                type StoredFile struct {
                	ID           uuid.UUID `gorm:"type:uuid;primaryKey"`
                	OriginalName string    `gorm:"type:varchar(255);not null"`
                	Filename     string    `gorm:"type:varchar(255);not null"`
                	Path         string    `gorm:"type:varchar(512);not null;index"`
                	MimeType     string    `gorm:"type:varchar(100);not null;index"`
                	Size         int64     `gorm:"not null"`
                	StorageType  string    `gorm:"type:varchar(50);not null;default:'local'"`
                	CreatedAt    time.Time
                	UpdatedAt    time.Time
                }

                // BeforeCreate sets the UUID before creating the record.
                func (f *StoredFile) BeforeCreate(tx *gorm.DB) error {
                	if f.ID == uuid.Nil {
                		f.ID = uuid.New()
                	}
                	return nil
                }

                // TableName returns the table name for GORM.
                func (StoredFile) TableName() string {
                	return "stored_files"
                }

                // ToResponse converts the model to a response DTO.
                func (f *StoredFile) ToResponse(url string) *dto.StoredFileResponse {
                	return &dto.StoredFileResponse{
                		ID:           f.ID.String(),
                		OriginalName: f.OriginalName,
                		MimeType:     f.MimeType,
                		Size:         f.Size,
                		SizeHuman:    dto.FormatFileSize(f.Size),
                		URL:          url,
                		CreatedAt:    f.CreatedAt.Format(time.RFC3339),
                	}
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
        	ID           string `json:"id"`
        	OriginalName string `json:"original_name"`
        	MimeType     string `json:"mime_type"`
        	Size         int64  `json:"size"`
        	SizeHuman    string `json:"size_human"`
        	URL          string `json:"url"`
        	CreatedAt    string `json:"created_at"`
        }

        // FileUploadRequest represents a file upload request.
        type FileUploadRequest struct {
        	Directory string `form:"directory"`
        }

        // FormatFileSize formats a file size in bytes to a human-readable string.
        func FormatFileSize(bytes int64) string {
        	const unit = 1024
        	if bytes < unit {
        		return fmt.Sprintf("%d B", bytes)
        	}
        	div, exp := int64(unit), 0
        	for n := bytes / unit; n >= unit; n /= unit {
        		div *= unit
        		exp++
        	}
        	return fmt.Sprintf("%.2f %cB", float64(bytes)/float64(div), "KMGTPE"[exp])
        }
        """;
    }

    private String generateS3Storage() {
        return """
        package storage

        import (
        	"context"
        	"io"
        	"mime/multipart"
        	"time"

        	"github.com/aws/aws-sdk-go-v2/aws"
        	"github.com/aws/aws-sdk-go-v2/config"
        	"github.com/aws/aws-sdk-go-v2/service/s3"
        	"github.com/spf13/viper"
        )

        // S3Storage implements StorageBackend for AWS S3.
        type S3Storage struct {
        	client *s3.Client
        	bucket string
        	region string
        }

        // NewS3Storage creates a new S3 storage backend.
        func NewS3Storage() (*S3Storage, error) {
        	region := viper.GetString("storage.s3.region")
        	if region == "" {
        		region = "us-east-1"
        	}

        	cfg, err := config.LoadDefaultConfig(context.Background(),
        		config.WithRegion(region),
        	)
        	if err != nil {
        		return nil, err
        	}

        	client := s3.NewFromConfig(cfg)
        	bucket := viper.GetString("storage.s3.bucket")

        	return &S3Storage{
        		client: client,
        		bucket: bucket,
        		region: region,
        	}, nil
        }

        // Upload uploads a file to S3.
        func (s *S3Storage) Upload(path string, file multipart.File) error {
        	_, err := s.client.PutObject(context.Background(), &s3.PutObjectInput{
        		Bucket: aws.String(s.bucket),
        		Key:    aws.String(path),
        		Body:   file,
        	})
        	return err
        }

        // Download returns a reader for an S3 object.
        func (s *S3Storage) Download(path string) (io.ReadCloser, error) {
        	result, err := s.client.GetObject(context.Background(), &s3.GetObjectInput{
        		Bucket: aws.String(s.bucket),
        		Key:    aws.String(path),
        	})
        	if err != nil {
        		return nil, err
        	}
        	return result.Body, nil
        }

        // Delete removes a file from S3.
        func (s *S3Storage) Delete(path string) error {
        	_, err := s.client.DeleteObject(context.Background(), &s3.DeleteObjectInput{
        		Bucket: aws.String(s.bucket),
        		Key:    aws.String(path),
        	})
        	return err
        }

        // Exists checks if a file exists in S3.
        func (s *S3Storage) Exists(path string) bool {
        	_, err := s.client.HeadObject(context.Background(), &s3.HeadObjectInput{
        		Bucket: aws.String(s.bucket),
        		Key:    aws.String(path),
        	})
        	return err == nil
        }

        // GetURL returns the public URL for an S3 object.
        func (s *S3Storage) GetURL(path string) string {
        	return "https://" + s.bucket + ".s3." + s.region + ".amazonaws.com/" + path
        }

        // GetTemporaryURL returns a presigned URL for an S3 object.
        func (s *S3Storage) GetTemporaryURL(path string, expirationMinutes int) (string, error) {
        	presignClient := s3.NewPresignClient(s.client)
        	result, err := presignClient.PresignGetObject(context.Background(), &s3.GetObjectInput{
        		Bucket: aws.String(s.bucket),
        		Key:    aws.String(path),
        	}, s3.WithPresignExpires(time.Duration(expirationMinutes)*time.Minute))
        	if err != nil {
        		return "", err
        	}
        	return result.URL, nil
        }

        // Type returns the storage type.
        func (s *S3Storage) Type() string {
        	return "s3"
        }
        """;
    }

    private String generateAzureStorage() {
        return """
        package storage

        import (
        	"context"
        	"fmt"
        	"io"
        	"mime/multipart"
        	"time"

        	"github.com/Azure/azure-sdk-for-go/sdk/storage/azblob"
        	"github.com/spf13/viper"
        )

        // AzureStorage implements StorageBackend for Azure Blob Storage.
        type AzureStorage struct {
        	client    *azblob.Client
        	container string
        	accountName string
        }

        // NewAzureStorage creates a new Azure Blob storage backend.
        func NewAzureStorage() (*AzureStorage, error) {
        	accountName := viper.GetString("storage.azure.account_name")
        	accountKey := viper.GetString("storage.azure.account_key")
        	container := viper.GetString("storage.azure.container")

        	if container == "" {
        		container = "files"
        	}

        	serviceURL := fmt.Sprintf("https://%s.blob.core.windows.net/", accountName)

        	cred, err := azblob.NewSharedKeyCredential(accountName, accountKey)
        	if err != nil {
        		return nil, err
        	}

        	client, err := azblob.NewClientWithSharedKeyCredential(serviceURL, cred, nil)
        	if err != nil {
        		return nil, err
        	}

        	return &AzureStorage{
        		client:    client,
        		container: container,
        		accountName: accountName,
        	}, nil
        }

        // Upload uploads a file to Azure Blob Storage.
        func (s *AzureStorage) Upload(path string, file multipart.File) error {
        	_, err := s.client.UploadStream(context.Background(), s.container, path, file, nil)
        	return err
        }

        // Download returns a reader for an Azure blob.
        func (s *AzureStorage) Download(path string) (io.ReadCloser, error) {
        	resp, err := s.client.DownloadStream(context.Background(), s.container, path, nil)
        	if err != nil {
        		return nil, err
        	}
        	return resp.Body, nil
        }

        // Delete removes a blob from Azure Storage.
        func (s *AzureStorage) Delete(path string) error {
        	_, err := s.client.DeleteBlob(context.Background(), s.container, path, nil)
        	return err
        }

        // Exists checks if a blob exists in Azure Storage.
        func (s *AzureStorage) Exists(path string) bool {
        	_, err := s.client.DownloadStream(context.Background(), s.container, path, &azblob.DownloadStreamOptions{
        		Range: azblob.HTTPRange{Offset: 0, Count: 1},
        	})
        	return err == nil
        }

        // GetURL returns the public URL for an Azure blob.
        func (s *AzureStorage) GetURL(path string) string {
        	return fmt.Sprintf("https://%s.blob.core.windows.net/%s/%s", s.accountName, s.container, path)
        }

        // GetTemporaryURL returns a SAS URL for an Azure blob.
        func (s *AzureStorage) GetTemporaryURL(path string, expirationMinutes int) (string, error) {
        	// Note: Full SAS URL generation requires service client with proper credentials
        	// This is a simplified version - in production, use proper SAS token generation
        	expiry := time.Now().Add(time.Duration(expirationMinutes) * time.Minute)
        	return fmt.Sprintf("%s?se=%s", s.GetURL(path), expiry.Format(time.RFC3339)), nil
        }

        // Type returns the storage type.
        func (s *AzureStorage) Type() string {
        	return "azure"
        }
        """;
    }
}
