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
package com.jnzader.apigen.codegen.generator.typescript.storage;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Generates file upload and storage code for TypeScript/NestJS applications.
 *
 * @author APiGen
 * @since 2.13.0
 */
public class TypeScriptFileStorageGenerator {

    /**
     * Generates file storage files.
     *
     * @param useS3 whether to include S3 storage backend
     * @param useAzure whether to include Azure Blob storage backend
     * @return map of file path to content
     */
    public Map<String, String> generate(boolean useS3, boolean useAzure) {
        Map<String, String> files = new LinkedHashMap<>();

        // Determine primary storage type for module configuration
        String primaryStorage = useS3 ? "s3" : (useAzure ? "azure" : "local");

        files.put("src/files/files.module.ts", generateModule(primaryStorage));
        files.put("src/files/files.service.ts", generateService(primaryStorage));
        files.put("src/files/files.controller.ts", generateController());
        files.put("src/files/dto/file.dto.ts", generateDto());
        files.put("src/files/entities/file-metadata.entity.ts", generateEntity());
        files.put("src/files/storage/storage.interface.ts", generateStorageInterface());

        // Always include local storage as fallback
        files.put("src/files/storage/local.storage.ts", generateLocalStorage());

        // Add cloud storage backends as requested
        if (useS3) {
            files.put("src/files/storage/s3.storage.ts", generateS3Storage());
        }
        if (useAzure) {
            files.put("src/files/storage/azure.storage.ts", generateAzureStorage());
        }

        return files;
    }

    private String generateModule(String storageType) {
        String storageProvider =
                switch (storageType.toLowerCase()) {
                    case "s3" -> "S3Storage";
                    case "azure" -> "AzureStorage";
                    default -> "LocalStorage";
                };

        String storageImport =
                switch (storageType.toLowerCase()) {
                    case "s3" -> "import { S3Storage } from './storage/s3.storage';";
                    case "azure" -> "import { AzureStorage } from './storage/azure.storage';";
                    default -> "import { LocalStorage } from './storage/local.storage';";
                };

        return String.format(
                """
                import { Module } from '@nestjs/common';
                import { TypeOrmModule } from '@nestjs/typeorm';
                import { ConfigModule } from '@nestjs/config';
                import { FilesService } from './files.service';
                import { FilesController } from './files.controller';
                import { FileMetadata } from './entities/file-metadata.entity';
                import { STORAGE_PROVIDER } from './storage/storage.interface';
                %s

                @Module({
                  imports: [
                    TypeOrmModule.forFeature([FileMetadata]),
                    ConfigModule,
                  ],
                  controllers: [FilesController],
                  providers: [
                    FilesService,
                    {
                      provide: STORAGE_PROVIDER,
                      useClass: %s,
                    },
                  ],
                  exports: [FilesService],
                })
                export class FilesModule {}
                """,
                storageImport, storageProvider);
    }

    private String generateService(String storageType) {
        return """
        import { Injectable, Inject, NotFoundException, BadRequestException } from '@nestjs/common';
        import { InjectRepository } from '@nestjs/typeorm';
        import { Repository } from 'typeorm';
        import { FileMetadata } from './entities/file-metadata.entity';
        import { IStorageProvider, STORAGE_PROVIDER } from './storage/storage.interface';
        import { v4 as uuidv4 } from 'uuid';
        import { extname } from 'path';

        @Injectable()
        export class FilesService {
          private readonly allowedExtensions = new Set([
            'jpg', 'jpeg', 'png', 'gif', 'webp', // Images
            'pdf', 'doc', 'docx', 'xls', 'xlsx', // Documents
            'txt', 'csv', 'json', 'xml',         // Text
            'zip', 'tar', 'gz',                  // Archives
          ]);

          private readonly maxFileSize = 10 * 1024 * 1024; // 10 MB

          constructor(
            @InjectRepository(FileMetadata)
            private readonly fileRepository: Repository<FileMetadata>,
            @Inject(STORAGE_PROVIDER)
            private readonly storageProvider: IStorageProvider,
          ) {}

          async upload(
            file: Express.Multer.File,
            userId?: string,
          ): Promise<FileMetadata> {
            this.validateFile(file);

            const extension = extname(file.originalname).slice(1).toLowerCase();
            const storedFilename = `${uuidv4()}${extension ? '.' + extension : ''}`;

            const storagePath = await this.storageProvider.upload(storedFilename, file.buffer);

            const metadata = this.fileRepository.create({
              originalFilename: file.originalname,
              storedFilename,
              contentType: file.mimetype,
              size: file.size,
              storagePath,
              storageType: this.storageProvider.storageType,
              uploadedBy: userId,
            });

            return this.fileRepository.save(metadata);
          }

          async getFile(id: string): Promise<{ metadata: FileMetadata; content: Buffer }> {
            const metadata = await this.fileRepository.findOne({ where: { id } });
            if (!metadata) {
              throw new NotFoundException('File not found');
            }

            const content = await this.storageProvider.download(metadata.storagePath);
            if (!content) {
              throw new NotFoundException('File content not found');
            }

            return { metadata, content };
          }

          async getMetadata(id: string): Promise<FileMetadata> {
            const metadata = await this.fileRepository.findOne({ where: { id } });
            if (!metadata) {
              throw new NotFoundException('File not found');
            }
            return metadata;
          }

          async delete(id: string): Promise<boolean> {
            const metadata = await this.fileRepository.findOne({ where: { id } });
            if (!metadata) {
              return false;
            }

            await this.storageProvider.delete(metadata.storagePath);
            await this.fileRepository.remove(metadata);
            return true;
          }

          async list(
            page: number = 1,
            pageSize: number = 20,
            userId?: string,
          ): Promise<{ files: FileMetadata[]; total: number }> {
            const where = userId ? { uploadedBy: userId } : {};

            const [files, total] = await this.fileRepository.findAndCount({
              where,
              skip: (page - 1) * pageSize,
              take: pageSize,
              order: { createdAt: 'DESC' },
            });

            return { files, total };
          }

          getDownloadUrl(metadata: FileMetadata): string {
            return this.storageProvider.getUrl(metadata.storagePath);
          }

          private validateFile(file: Express.Multer.File): void {
            if (!file.originalname) {
              throw new BadRequestException('Filename is required');
            }

            if (file.size > this.maxFileSize) {
              throw new BadRequestException('File size exceeds maximum allowed');
            }

            const extension = extname(file.originalname).slice(1).toLowerCase();
            if (extension && !this.allowedExtensions.has(extension)) {
              throw new BadRequestException(`File type not allowed: ${extension}`);
            }
          }
        }
        """;
    }

    private String generateController() {
        return """
        import {
          Controller,
          Post,
          Get,
          Delete,
          Param,
          Query,
          UseInterceptors,
          UploadedFile,
          ParseUUIDPipe,
          Res,
          HttpStatus,
        } from '@nestjs/common';
        import { FileInterceptor } from '@nestjs/platform-express';
        import { ApiTags, ApiOperation, ApiConsumes, ApiBody, ApiResponse } from '@nestjs/swagger';
        import { Response } from 'express';
        import { FilesService } from './files.service';
        import {
          FileUploadResponseDto,
          FileMetadataResponseDto,
          FileListResponseDto,
          FileDeleteResponseDto,
        } from './dto/file.dto';

        @ApiTags('Files')
        @Controller('files')
        export class FilesController {
          constructor(private readonly filesService: FilesService) {}

          @Post('upload')
          @UseInterceptors(FileInterceptor('file'))
          @ApiOperation({ summary: 'Upload a file' })
          @ApiConsumes('multipart/form-data')
          @ApiBody({
            schema: {
              type: 'object',
              properties: {
                file: { type: 'string', format: 'binary' },
              },
            },
          })
          @ApiResponse({ status: 201, type: FileUploadResponseDto })
          async upload(
            @UploadedFile() file: Express.Multer.File,
          ): Promise<FileUploadResponseDto> {
            const metadata = await this.filesService.upload(file);
            return {
              id: metadata.id,
              filename: metadata.originalFilename,
              contentType: metadata.contentType,
              size: metadata.size,
              url: this.filesService.getDownloadUrl(metadata),
            };
          }

          @Get(':id')
          @ApiOperation({ summary: 'Get file metadata' })
          @ApiResponse({ status: 200, type: FileMetadataResponseDto })
          async getMetadata(
            @Param('id', ParseUUIDPipe) id: string,
          ): Promise<FileMetadataResponseDto> {
            const metadata = await this.filesService.getMetadata(id);
            return {
              id: metadata.id,
              originalFilename: metadata.originalFilename,
              contentType: metadata.contentType,
              size: metadata.size,
              storageType: metadata.storageType,
              createdAt: metadata.createdAt,
              url: this.filesService.getDownloadUrl(metadata),
            };
          }

          @Get(':id/download')
          @ApiOperation({ summary: 'Download a file' })
          async download(
            @Param('id', ParseUUIDPipe) id: string,
            @Res() res: Response,
          ): Promise<void> {
            const { metadata, content } = await this.filesService.getFile(id);

            res.set({
              'Content-Type': metadata.contentType,
              'Content-Disposition': `attachment; filename="${metadata.originalFilename}"`,
            });
            res.status(HttpStatus.OK).send(content);
          }

          @Delete(':id')
          @ApiOperation({ summary: 'Delete a file' })
          @ApiResponse({ status: 200, type: FileDeleteResponseDto })
          async delete(
            @Param('id', ParseUUIDPipe) id: string,
          ): Promise<FileDeleteResponseDto> {
            const success = await this.filesService.delete(id);
            return {
              success,
              message: success ? 'File deleted successfully' : 'File not found',
            };
          }

          @Get()
          @ApiOperation({ summary: 'List files' })
          @ApiResponse({ status: 200, type: FileListResponseDto })
          async list(
            @Query('page') page: number = 1,
            @Query('pageSize') pageSize: number = 20,
          ): Promise<FileListResponseDto> {
            const { files, total } = await this.filesService.list(page, pageSize);
            return {
              files: files.map((f) => ({
                id: f.id,
                originalFilename: f.originalFilename,
                contentType: f.contentType,
                size: f.size,
                storageType: f.storageType,
                createdAt: f.createdAt,
                url: this.filesService.getDownloadUrl(f),
              })),
              total,
              page,
              pageSize,
            };
          }
        }
        """;
    }

    private String generateDto() {
        return """
        import { ApiProperty } from '@nestjs/swagger';

        export class FileUploadResponseDto {
          @ApiProperty()
          id: string;

          @ApiProperty()
          filename: string;

          @ApiProperty()
          contentType: string;

          @ApiProperty()
          size: number;

          @ApiProperty()
          url: string;
        }

        export class FileMetadataResponseDto {
          @ApiProperty()
          id: string;

          @ApiProperty()
          originalFilename: string;

          @ApiProperty()
          contentType: string;

          @ApiProperty()
          size: number;

          @ApiProperty()
          storageType: string;

          @ApiProperty()
          createdAt: Date;

          @ApiProperty({ required: false })
          url?: string;
        }

        export class FileListResponseDto {
          @ApiProperty({ type: [FileMetadataResponseDto] })
          files: FileMetadataResponseDto[];

          @ApiProperty()
          total: number;

          @ApiProperty()
          page: number;

          @ApiProperty()
          pageSize: number;
        }

        export class FileDeleteResponseDto {
          @ApiProperty()
          success: boolean;

          @ApiProperty()
          message: string;
        }
        """;
    }

    private String generateEntity() {
        return """
        import {
          Entity,
          Column,
          PrimaryGeneratedColumn,
          CreateDateColumn,
          UpdateDateColumn,
          ManyToOne,
          JoinColumn,
        } from 'typeorm';
        import { User } from '../../users/entities/user.entity';

        @Entity('file_metadata')
        export class FileMetadata {
          @PrimaryGeneratedColumn('uuid')
          id: string;

          @Column({ name: 'original_filename' })
          originalFilename: string;

          @Column({ name: 'stored_filename' })
          storedFilename: string;

          @Column({ name: 'content_type' })
          contentType: string;

          @Column({ type: 'bigint' })
          size: number;

          @Column({ name: 'storage_path' })
          storagePath: string;

          @Column({ name: 'storage_type', default: 'local' })
          storageType: string;

          @Column({ name: 'uploaded_by', nullable: true })
          uploadedBy?: string;

          @CreateDateColumn({ name: 'created_at' })
          createdAt: Date;

          @UpdateDateColumn({ name: 'updated_at' })
          updatedAt: Date;

          @ManyToOne(() => User, { nullable: true })
          @JoinColumn({ name: 'uploaded_by' })
          uploader?: User;
        }
        """;
    }

    private String generateStorageInterface() {
        return """
        export const STORAGE_PROVIDER = 'STORAGE_PROVIDER';

        export interface IStorageProvider {
          readonly storageType: string;

          upload(filename: string, content: Buffer): Promise<string>;
          download(path: string): Promise<Buffer | null>;
          delete(path: string): Promise<boolean>;
          getUrl(path: string): string;
        }
        """;
    }

    private String generateLocalStorage() {
        return """
        import { Injectable } from '@nestjs/common';
        import { ConfigService } from '@nestjs/config';
        import { IStorageProvider } from './storage.interface';
        import { promises as fs } from 'fs';
        import { join } from 'path';

        @Injectable()
        export class LocalStorage implements IStorageProvider {
          readonly storageType = 'local';
          private readonly basePath: string;

          constructor(private readonly configService: ConfigService) {
            this.basePath = this.configService.get('UPLOAD_DIR', './uploads');
            this.ensureDirectory();
          }

          private async ensureDirectory(): Promise<void> {
            try {
              await fs.mkdir(this.basePath, { recursive: true });
            } catch (error) {
              // Directory exists
            }
          }

          async upload(filename: string, content: Buffer): Promise<string> {
            const filePath = join(this.basePath, filename);
            await fs.writeFile(filePath, content);
            return filePath;
          }

          async download(path: string): Promise<Buffer | null> {
            try {
              return await fs.readFile(path);
            } catch {
              return null;
            }
          }

          async delete(path: string): Promise<boolean> {
            try {
              await fs.unlink(path);
              return true;
            } catch {
              return false;
            }
          }

          getUrl(path: string): string {
            const filename = path.split('/').pop();
            return `/api/files/${filename}/download`;
          }
        }
        """;
    }

    private String generateS3Storage() {
        return """
        import { Injectable } from '@nestjs/common';
        import { ConfigService } from '@nestjs/config';
        import { IStorageProvider } from './storage.interface';
        import {
          S3Client,
          PutObjectCommand,
          GetObjectCommand,
          DeleteObjectCommand,
        } from '@aws-sdk/client-s3';
        import { Readable } from 'stream';

        @Injectable()
        export class S3Storage implements IStorageProvider {
          readonly storageType = 's3';
          private readonly client: S3Client;
          private readonly bucket: string;
          private readonly region: string;

          constructor(private readonly configService: ConfigService) {
            this.region = this.configService.get('AWS_REGION', 'us-east-1');
            this.bucket = this.configService.get('AWS_S3_BUCKET', '');

            this.client = new S3Client({
              region: this.region,
              credentials: {
                accessKeyId: this.configService.get('AWS_ACCESS_KEY_ID', ''),
                secretAccessKey: this.configService.get('AWS_SECRET_ACCESS_KEY', ''),
              },
            });
          }

          async upload(filename: string, content: Buffer): Promise<string> {
            await this.client.send(
              new PutObjectCommand({
                Bucket: this.bucket,
                Key: filename,
                Body: content,
              }),
            );
            return `s3://${this.bucket}/${filename}`;
          }

          async download(path: string): Promise<Buffer | null> {
            try {
              const key = this.extractKey(path);
              const response = await this.client.send(
                new GetObjectCommand({
                  Bucket: this.bucket,
                  Key: key,
                }),
              );

              const stream = response.Body as Readable;
              const chunks: Buffer[] = [];
              for await (const chunk of stream) {
                chunks.push(Buffer.from(chunk));
              }
              return Buffer.concat(chunks);
            } catch {
              return null;
            }
          }

          async delete(path: string): Promise<boolean> {
            try {
              const key = this.extractKey(path);
              await this.client.send(
                new DeleteObjectCommand({
                  Bucket: this.bucket,
                  Key: key,
                }),
              );
              return true;
            } catch {
              return false;
            }
          }

          getUrl(path: string): string {
            const key = this.extractKey(path);
            return `https://${this.bucket}.s3.${this.region}.amazonaws.com/${key}`;
          }

          private extractKey(path: string): string {
            if (path.startsWith('s3://')) {
              return path.split('/').slice(3).join('/');
            }
            return path;
          }
        }
        """;
    }

    private String generateAzureStorage() {
        return """
        import { Injectable } from '@nestjs/common';
        import { ConfigService } from '@nestjs/config';
        import { IStorageProvider } from './storage.interface';
        import { BlobServiceClient, ContainerClient } from '@azure/storage-blob';

        @Injectable()
        export class AzureStorage implements IStorageProvider {
          readonly storageType = 'azure';
          private readonly containerClient: ContainerClient;
          private readonly containerName: string;
          private readonly accountName: string;

          constructor(private readonly configService: ConfigService) {
            const connectionString = this.configService.get('AZURE_STORAGE_CONNECTION_STRING', '');
            this.containerName = this.configService.get('AZURE_STORAGE_CONTAINER', 'uploads');

            const blobServiceClient = BlobServiceClient.fromConnectionString(connectionString);
            this.containerClient = blobServiceClient.getContainerClient(this.containerName);
            this.accountName = blobServiceClient.accountName;
          }

          async upload(filename: string, content: Buffer): Promise<string> {
            const blobClient = this.containerClient.getBlockBlobClient(filename);
            await blobClient.upload(content, content.length);
            return `azure://${this.containerName}/${filename}`;
          }

          async download(path: string): Promise<Buffer | null> {
            try {
              const blobName = this.extractBlobName(path);
              const blobClient = this.containerClient.getBlockBlobClient(blobName);
              const response = await blobClient.downloadToBuffer();
              return response;
            } catch {
              return null;
            }
          }

          async delete(path: string): Promise<boolean> {
            try {
              const blobName = this.extractBlobName(path);
              const blobClient = this.containerClient.getBlockBlobClient(blobName);
              await blobClient.delete();
              return true;
            } catch {
              return false;
            }
          }

          getUrl(path: string): string {
            const blobName = this.extractBlobName(path);
            return `https://${this.accountName}.blob.core.windows.net/${this.containerName}/${blobName}`;
          }

          private extractBlobName(path: string): string {
            if (path.startsWith('azure://')) {
              return path.split('/').slice(3).join('/');
            }
            return path;
          }
        }
        """;
    }
}
