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
package com.jnzader.apigen.codegen.generator.python.storage;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Generates file upload and storage code for Python/FastAPI applications.
 *
 * @author APiGen
 * @since 2.13.0
 */
public class PythonFileStorageGenerator {

    /**
     * Generates file storage files.
     *
     * @param storageType storage type: "local", "s3", or "azure"
     * @return map of file path to content
     */
    public Map<String, String> generate(String storageType) {
        Map<String, String> files = new LinkedHashMap<>();

        files.put("app/models/file_metadata.py", generateModel());
        files.put("app/schemas/file.py", generateSchemas());
        files.put("app/services/file_storage_service.py", generateService(storageType));
        files.put("app/routers/files.py", generateRouter());

        if ("s3".equalsIgnoreCase(storageType)) {
            files.put("app/services/storage/s3_storage.py", generateS3Storage());
        } else if ("azure".equalsIgnoreCase(storageType)) {
            files.put("app/services/storage/azure_storage.py", generateAzureStorage());
        } else {
            files.put("app/services/storage/local_storage.py", generateLocalStorage());
        }

        files.put("app/services/storage/__init__.py", generateStorageInit(storageType));

        return files;
    }

    private String generateModel() {
        return """
        \"\"\"File metadata model.\"\"\"

        from datetime import datetime
        from uuid import uuid4

        from sqlalchemy import BigInteger, Column, DateTime, ForeignKey, String
        from sqlalchemy.dialects.postgresql import UUID
        from sqlalchemy.orm import relationship

        from app.core.database import Base


        class FileMetadata(Base):
            \"\"\"Model for storing file metadata.\"\"\"

            __tablename__ = "file_metadata"

            id = Column(UUID(as_uuid=True), primary_key=True, default=uuid4)
            original_filename = Column(String(255), nullable=False)
            stored_filename = Column(String(255), nullable=False)
            content_type = Column(String(100), nullable=False)
            size = Column(BigInteger, nullable=False)
            storage_path = Column(String(500), nullable=False)
            storage_type = Column(String(50), nullable=False, default="local")
            uploaded_by = Column(UUID(as_uuid=True), ForeignKey("users.id"), nullable=True)
            created_at = Column(DateTime, default=datetime.utcnow)
            updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)

            uploader = relationship("User", back_populates="uploaded_files")

            @property
            def extension(self) -> str:
                \"\"\"Get file extension.\"\"\"
                if "." in self.original_filename:
                    return self.original_filename.rsplit(".", 1)[1].lower()
                return ""
        """;
    }

    private String generateSchemas() {
        return """
        \"\"\"File schemas.\"\"\"

        from datetime import datetime
        from uuid import UUID

        from pydantic import BaseModel


        class FileUploadResponse(BaseModel):
            \"\"\"Response after file upload.\"\"\"

            id: UUID
            filename: str
            content_type: str
            size: int
            url: str

            model_config = {"from_attributes": True}


        class FileMetadataResponse(BaseModel):
            \"\"\"File metadata response.\"\"\"

            id: UUID
            original_filename: str
            content_type: str
            size: int
            storage_type: str
            created_at: datetime
            url: str | None = None

            model_config = {"from_attributes": True}


        class FileListResponse(BaseModel):
            \"\"\"List of files response.\"\"\"

            files: list[FileMetadataResponse]
            total: int
            page: int
            page_size: int


        class FileDeleteResponse(BaseModel):
            \"\"\"Response after file deletion.\"\"\"

            success: bool
            message: str
        """;
    }

    private String generateService(String storageType) {
        String storageImport =
                switch (storageType.toLowerCase()) {
                    case "s3" -> "from app.services.storage.s3_storage import S3Storage as Storage";
                    case "azure" ->
                            "from app.services.storage.azure_storage import AzureStorage as"
                                    + " Storage";
                    default ->
                            "from app.services.storage.local_storage import LocalStorage as"
                                    + " Storage";
                };

        return String.format(
                """
                \"\"\"File storage service.\"\"\"

                import mimetypes
                from uuid import UUID, uuid4

                from fastapi import UploadFile
                from sqlalchemy import func, select
                from sqlalchemy.ext.asyncio import AsyncSession

                from app.models.file_metadata import FileMetadata
                %s


                class FileStorageService:
                    \"\"\"Service for managing file uploads and storage.\"\"\"

                    ALLOWED_EXTENSIONS = {
                        "jpg", "jpeg", "png", "gif", "webp",  # Images
                        "pdf", "doc", "docx", "xls", "xlsx",  # Documents
                        "txt", "csv", "json", "xml",          # Text
                        "zip", "tar", "gz",                   # Archives
                    }
                    MAX_FILE_SIZE = 10 * 1024 * 1024  # 10 MB

                    def __init__(self, db: AsyncSession):
                        self.db = db
                        self.storage = Storage()

                    async def upload_file(
                        self, file: UploadFile, user_id: UUID | None = None
                    ) -> FileMetadata:
                        \"\"\"Upload a file and store its metadata.\"\"\"
                        # Validate file
                        self._validate_file(file)

                        # Read file content
                        content = await file.read()
                        file_size = len(content)

                        # Generate stored filename
                        extension = self._get_extension(file.filename or "")
                        stored_filename = f"{uuid4()}.{extension}" if extension else str(uuid4())

                        # Upload to storage
                        storage_path = await self.storage.upload(stored_filename, content)

                        # Save metadata
                        metadata = FileMetadata(
                            original_filename=file.filename or "unknown",
                            stored_filename=stored_filename,
                            content_type=file.content_type or "application/octet-stream",
                            size=file_size,
                            storage_path=storage_path,
                            storage_type=self.storage.storage_type,
                            uploaded_by=user_id,
                        )

                        self.db.add(metadata)
                        await self.db.commit()
                        await self.db.refresh(metadata)

                        return metadata

                    async def get_file(self, file_id: UUID) -> tuple[FileMetadata, bytes] | None:
                        \"\"\"Get file metadata and content.\"\"\"
                        result = await self.db.execute(
                            select(FileMetadata).where(FileMetadata.id == file_id)
                        )
                        metadata = result.scalar_one_or_none()

                        if not metadata:
                            return None

                        content = await self.storage.download(metadata.storage_path)
                        if content is None:
                            return None

                        return metadata, content

                    async def get_metadata(self, file_id: UUID) -> FileMetadata | None:
                        \"\"\"Get file metadata only.\"\"\"
                        result = await self.db.execute(
                            select(FileMetadata).where(FileMetadata.id == file_id)
                        )
                        return result.scalar_one_or_none()

                    async def delete_file(self, file_id: UUID) -> bool:
                        \"\"\"Delete a file from storage and database.\"\"\"
                        result = await self.db.execute(
                            select(FileMetadata).where(FileMetadata.id == file_id)
                        )
                        metadata = result.scalar_one_or_none()

                        if not metadata:
                            return False

                        # Delete from storage
                        await self.storage.delete(metadata.storage_path)

                        # Delete metadata
                        await self.db.delete(metadata)
                        await self.db.commit()

                        return True

                    async def list_files(
                        self, page: int = 1, page_size: int = 20, user_id: UUID | None = None
                    ) -> tuple[list[FileMetadata], int]:
                        \"\"\"List files with pagination.\"\"\"
                        query = select(FileMetadata)

                        if user_id:
                            query = query.where(FileMetadata.uploaded_by == user_id)

                        # Get total count
                        count_result = await self.db.execute(
                            select(func.count()).select_from(query.subquery())
                        )
                        total = count_result.scalar() or 0

                        # Get paginated results
                        query = query.offset((page - 1) * page_size).limit(page_size)
                        result = await self.db.execute(query)
                        files = result.scalars().all()

                        return list(files), total

                    def get_download_url(self, metadata: FileMetadata) -> str:
                        \"\"\"Get download URL for a file.\"\"\"
                        return self.storage.get_url(metadata.storage_path)

                    def _validate_file(self, file: UploadFile) -> None:
                        \"\"\"Validate file before upload.\"\"\"
                        if not file.filename:
                            raise ValueError("Filename is required")

                        extension = self._get_extension(file.filename)
                        if extension and extension not in self.ALLOWED_EXTENSIONS:
                            raise ValueError(f"File type not allowed: {extension}")

                    def _get_extension(self, filename: str) -> str:
                        \"\"\"Get file extension from filename.\"\"\"
                        if "." in filename:
                            return filename.rsplit(".", 1)[1].lower()
                        return ""
                """,
                storageImport);
    }

    private String generateLocalStorage() {
        return """
        \"\"\"Local file storage implementation.\"\"\"

        import os
        from pathlib import Path

        import aiofiles


        class LocalStorage:
            \"\"\"Local filesystem storage.\"\"\"

            storage_type = "local"

            def __init__(self, base_path: str | None = None):
                self.base_path = Path(base_path or os.getenv("UPLOAD_DIR", "./uploads"))
                self.base_path.mkdir(parents=True, exist_ok=True)

            async def upload(self, filename: str, content: bytes) -> str:
                \"\"\"Upload file to local storage.\"\"\"
                file_path = self.base_path / filename
                async with aiofiles.open(file_path, "wb") as f:
                    await f.write(content)
                return str(file_path)

            async def download(self, path: str) -> bytes | None:
                \"\"\"Download file from local storage.\"\"\"
                try:
                    async with aiofiles.open(path, "rb") as f:
                        return await f.read()
                except FileNotFoundError:
                    return None

            async def delete(self, path: str) -> bool:
                \"\"\"Delete file from local storage.\"\"\"
                try:
                    os.remove(path)
                    return True
                except FileNotFoundError:
                    return False

            def get_url(self, path: str) -> str:
                \"\"\"Get URL for local file (API endpoint).\"\"\"
                filename = Path(path).name
                return f"/api/files/{filename}/download"
        """;
    }

    private String generateS3Storage() {
        return """
        \"\"\"AWS S3 storage implementation.\"\"\"

        import os
        from typing import Any

        import aioboto3
        from botocore.exceptions import ClientError


        class S3Storage:
            \"\"\"AWS S3 storage.\"\"\"

            storage_type = "s3"

            def __init__(self):
                self.bucket = os.getenv("AWS_S3_BUCKET", "")
                self.region = os.getenv("AWS_REGION", "us-east-1")
                self.session = aioboto3.Session()

            async def upload(self, filename: str, content: bytes) -> str:
                \"\"\"Upload file to S3.\"\"\"
                async with self.session.client("s3", region_name=self.region) as s3:
                    await s3.put_object(
                        Bucket=self.bucket,
                        Key=filename,
                        Body=content,
                    )
                return f"s3://{self.bucket}/{filename}"

            async def download(self, path: str) -> bytes | None:
                \"\"\"Download file from S3.\"\"\"
                try:
                    key = self._extract_key(path)
                    async with self.session.client("s3", region_name=self.region) as s3:
                        response = await s3.get_object(Bucket=self.bucket, Key=key)
                        return await response["Body"].read()
                except ClientError:
                    return None

            async def delete(self, path: str) -> bool:
                \"\"\"Delete file from S3.\"\"\"
                try:
                    key = self._extract_key(path)
                    async with self.session.client("s3", region_name=self.region) as s3:
                        await s3.delete_object(Bucket=self.bucket, Key=key)
                    return True
                except ClientError:
                    return False

            def get_url(self, path: str) -> str:
                \"\"\"Get presigned URL for S3 object.\"\"\"
                key = self._extract_key(path)
                return f"https://{self.bucket}.s3.{self.region}.amazonaws.com/{key}"

            def _extract_key(self, path: str) -> str:
                \"\"\"Extract S3 key from path.\"\"\"
                if path.startswith("s3://"):
                    return path.split("/", 3)[3]
                return path
        """;
    }

    private String generateAzureStorage() {
        return """
        \"\"\"Azure Blob Storage implementation.\"\"\"

        import os

        from azure.storage.blob.aio import BlobServiceClient


        class AzureStorage:
            \"\"\"Azure Blob Storage.\"\"\"

            storage_type = "azure"

            def __init__(self):
                connection_string = os.getenv("AZURE_STORAGE_CONNECTION_STRING", "")
                self.container_name = os.getenv("AZURE_STORAGE_CONTAINER", "uploads")
                self.client = BlobServiceClient.from_connection_string(connection_string)

            async def upload(self, filename: str, content: bytes) -> str:
                \"\"\"Upload file to Azure Blob Storage.\"\"\"
                container_client = self.client.get_container_client(self.container_name)
                blob_client = container_client.get_blob_client(filename)
                await blob_client.upload_blob(content, overwrite=True)
                return f"azure://{self.container_name}/{filename}"

            async def download(self, path: str) -> bytes | None:
                \"\"\"Download file from Azure Blob Storage.\"\"\"
                try:
                    blob_name = self._extract_blob_name(path)
                    container_client = self.client.get_container_client(self.container_name)
                    blob_client = container_client.get_blob_client(blob_name)
                    stream = await blob_client.download_blob()
                    return await stream.readall()
                except Exception:
                    return None

            async def delete(self, path: str) -> bool:
                \"\"\"Delete file from Azure Blob Storage.\"\"\"
                try:
                    blob_name = self._extract_blob_name(path)
                    container_client = self.client.get_container_client(self.container_name)
                    blob_client = container_client.get_blob_client(blob_name)
                    await blob_client.delete_blob()
                    return True
                except Exception:
                    return False

            def get_url(self, path: str) -> str:
                \"\"\"Get URL for Azure blob.\"\"\"
                blob_name = self._extract_blob_name(path)
                account_name = self.client.account_name
                return f"https://{account_name}.blob.core.windows.net/{self.container_name}/{blob_name}"

            def _extract_blob_name(self, path: str) -> str:
                \"\"\"Extract blob name from path.\"\"\"
                if path.startswith("azure://"):
                    return path.split("/", 3)[3]
                return path
        """;
    }

    private String generateStorageInit(String storageType) {
        String storageClass =
                switch (storageType.toLowerCase()) {
                    case "s3" -> "S3Storage";
                    case "azure" -> "AzureStorage";
                    default -> "LocalStorage";
                };

        String importPath =
                switch (storageType.toLowerCase()) {
                    case "s3" -> "s3_storage";
                    case "azure" -> "azure_storage";
                    default -> "local_storage";
                };

        return String.format(
                """
                \"\"\"Storage module.\"\"\"

                from .%s import %s

                __all__ = ["%s"]
                """,
                importPath, storageClass, storageClass);
    }

    private String generateRouter() {
        return """
        \"\"\"File upload router.\"\"\"

        from uuid import UUID

        from fastapi import APIRouter, Depends, File, HTTPException, UploadFile, status
        from fastapi.responses import Response
        from sqlalchemy.ext.asyncio import AsyncSession

        from app.core.database import get_db
        from app.schemas.file import (
            FileDeleteResponse,
            FileListResponse,
            FileMetadataResponse,
            FileUploadResponse,
        )
        from app.services.file_storage_service import FileStorageService

        router = APIRouter(prefix="/files", tags=["Files"])


        @router.post("/upload", response_model=FileUploadResponse)
        async def upload_file(
            file: UploadFile = File(...),
            db: AsyncSession = Depends(get_db),
        ) -> FileUploadResponse:
            \"\"\"Upload a file.\"\"\"
            try:
                service = FileStorageService(db)
                metadata = await service.upload_file(file)

                return FileUploadResponse(
                    id=metadata.id,
                    filename=metadata.original_filename,
                    content_type=metadata.content_type,
                    size=metadata.size,
                    url=service.get_download_url(metadata),
                )
            except ValueError as e:
                raise HTTPException(
                    status_code=status.HTTP_400_BAD_REQUEST,
                    detail=str(e),
                )


        @router.get("/{file_id}", response_model=FileMetadataResponse)
        async def get_file_metadata(
            file_id: UUID,
            db: AsyncSession = Depends(get_db),
        ) -> FileMetadataResponse:
            \"\"\"Get file metadata.\"\"\"
            service = FileStorageService(db)
            metadata = await service.get_metadata(file_id)

            if not metadata:
                raise HTTPException(
                    status_code=status.HTTP_404_NOT_FOUND,
                    detail="File not found",
                )

            return FileMetadataResponse(
                id=metadata.id,
                original_filename=metadata.original_filename,
                content_type=metadata.content_type,
                size=metadata.size,
                storage_type=metadata.storage_type,
                created_at=metadata.created_at,
                url=service.get_download_url(metadata),
            )


        @router.get("/{file_id}/download")
        async def download_file(
            file_id: UUID,
            db: AsyncSession = Depends(get_db),
        ) -> Response:
            \"\"\"Download a file.\"\"\"
            service = FileStorageService(db)
            result = await service.get_file(file_id)

            if not result:
                raise HTTPException(
                    status_code=status.HTTP_404_NOT_FOUND,
                    detail="File not found",
                )

            metadata, content = result

            return Response(
                content=content,
                media_type=metadata.content_type,
                headers={
                    "Content-Disposition": f'attachment; filename="{metadata.original_filename}"'
                },
            )


        @router.delete("/{file_id}", response_model=FileDeleteResponse)
        async def delete_file(
            file_id: UUID,
            db: AsyncSession = Depends(get_db),
        ) -> FileDeleteResponse:
            \"\"\"Delete a file.\"\"\"
            service = FileStorageService(db)
            success = await service.delete_file(file_id)

            if not success:
                raise HTTPException(
                    status_code=status.HTTP_404_NOT_FOUND,
                    detail="File not found",
                )

            return FileDeleteResponse(success=True, message="File deleted successfully")


        @router.get("/", response_model=FileListResponse)
        async def list_files(
            page: int = 1,
            page_size: int = 20,
            db: AsyncSession = Depends(get_db),
        ) -> FileListResponse:
            \"\"\"List uploaded files.\"\"\"
            service = FileStorageService(db)
            files, total = await service.list_files(page=page, page_size=page_size)

            return FileListResponse(
                files=[
                    FileMetadataResponse(
                        id=f.id,
                        original_filename=f.original_filename,
                        content_type=f.content_type,
                        size=f.size,
                        storage_type=f.storage_type,
                        created_at=f.created_at,
                        url=service.get_download_url(f),
                    )
                    for f in files
                ],
                total=total,
                page=page,
                page_size=page_size,
            )
        """;
    }
}
