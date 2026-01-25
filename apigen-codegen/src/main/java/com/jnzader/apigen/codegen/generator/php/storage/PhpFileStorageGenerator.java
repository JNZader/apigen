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
package com.jnzader.apigen.codegen.generator.php.storage;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Generates file storage service code for PHP/Laravel applications.
 *
 * @author APiGen
 * @since 2.13.0
 */
@SuppressWarnings({
    "java:S1192",
    "java:S3400"
}) // S1192: template strings; S3400: template methods return constants
public class PhpFileStorageGenerator {

    /**
     * Generates file storage files.
     *
     * @param useS3 whether to generate S3 storage support
     * @param useAzure whether to generate Azure Blob storage support
     * @return map of file path to content
     */
    public Map<String, String> generate(boolean useS3, boolean useAzure) {
        Map<String, String> files = new LinkedHashMap<>();

        files.put("app/Http/Controllers/Api/V1/FileController.php", generateController());
        files.put("app/Services/FileStorageService.php", generateService(useS3, useAzure));
        files.put("app/Models/StoredFile.php", generateModel());
        files.put("app/Http/Resources/StoredFileResource.php", generateResource());
        files.put("app/Http/Requests/UploadFileRequest.php", generateUploadRequest());
        files.put(
                "database/migrations/0001_01_01_000004_create_stored_files_table.php",
                generateMigration());

        if (useS3) {
            files.put("config/filesystems_s3.php", generateS3Config());
        }
        if (useAzure) {
            files.put("config/filesystems_azure.php", generateAzureConfig());
        }

        return files;
    }

    private String generateController() {
        return """
        <?php

        namespace App\\Http\\Controllers\\Api\\V1;

        use App\\Http\\Controllers\\Controller;
        use App\\Http\\Requests\\UploadFileRequest;
        use App\\Http\\Resources\\StoredFileResource;
        use App\\Services\\FileStorageService;
        use Illuminate\\Http\\JsonResponse;
        use Illuminate\\Http\\Response;
        use Symfony\\Component\\HttpFoundation\\StreamedResponse;

        /**
         * @OA\\Tag(
         *     name="Files",
         *     description="File upload and management endpoints"
         * )
         */
        class FileController extends Controller
        {
            public function __construct(
                private FileStorageService $fileStorageService
            ) {}

            /**
             * @OA\\Post(
             *     path="/api/files/upload",
             *     tags={"Files"},
             *     summary="Upload a file",
             *     @OA\\RequestBody(
             *         required=true,
             *         @OA\\MediaType(
             *             mediaType="multipart/form-data",
             *             @OA\\Schema(
             *                 required={"file"},
             *                 @OA\\Property(
             *                     property="file",
             *                     type="string",
             *                     format="binary"
             *                 ),
             *                 @OA\\Property(
             *                     property="directory",
             *                     type="string",
             *                     description="Optional subdirectory"
             *                 )
             *             )
             *         )
             *     ),
             *     @OA\\Response(
             *         response=201,
             *         description="File uploaded successfully",
             *         @OA\\JsonContent(ref="#/components/schemas/StoredFileResource")
             *     ),
             *     @OA\\Response(response=422, description="Validation error")
             * )
             */
            public function upload(UploadFileRequest $request): JsonResponse
            {
                $file = $request->file('file');
                $directory = $request->input('directory', 'uploads');

                $storedFile = $this->fileStorageService->store($file, $directory);

                return (new StoredFileResource($storedFile))
                    ->response()
                    ->setStatusCode(Response::HTTP_CREATED);
            }

            /**
             * @OA\\Get(
             *     path="/api/files/{id}",
             *     tags={"Files"},
             *     summary="Get file metadata",
             *     @OA\\Parameter(
             *         name="id",
             *         in="path",
             *         required=true,
             *         @OA\\Schema(type="string", format="uuid")
             *     ),
             *     @OA\\Response(
             *         response=200,
             *         description="File metadata",
             *         @OA\\JsonContent(ref="#/components/schemas/StoredFileResource")
             *     ),
             *     @OA\\Response(response=404, description="File not found")
             * )
             */
            public function show(string $id): StoredFileResource
            {
                $file = $this->fileStorageService->findOrFail($id);
                return new StoredFileResource($file);
            }

            /**
             * @OA\\Get(
             *     path="/api/files/{id}/download",
             *     tags={"Files"},
             *     summary="Download a file",
             *     @OA\\Parameter(
             *         name="id",
             *         in="path",
             *         required=true,
             *         @OA\\Schema(type="string", format="uuid")
             *     ),
             *     @OA\\Response(
             *         response=200,
             *         description="File content",
             *         @OA\\MediaType(mediaType="application/octet-stream")
             *     ),
             *     @OA\\Response(response=404, description="File not found")
             * )
             */
            public function download(string $id): StreamedResponse
            {
                return $this->fileStorageService->download($id);
            }

            /**
             * @OA\\Delete(
             *     path="/api/files/{id}",
             *     tags={"Files"},
             *     summary="Delete a file",
             *     @OA\\Parameter(
             *         name="id",
             *         in="path",
             *         required=true,
             *         @OA\\Schema(type="string", format="uuid")
             *     ),
             *     @OA\\Response(response=204, description="File deleted successfully"),
             *     @OA\\Response(response=404, description="File not found")
             * )
             */
            public function destroy(string $id): Response
            {
                $this->fileStorageService->delete($id);
                return response()->noContent();
            }

            /**
             * @OA\\Get(
             *     path="/api/files",
             *     tags={"Files"},
             *     summary="List all files",
             *     @OA\\Parameter(
             *         name="directory",
             *         in="query",
             *         required=false,
             *         @OA\\Schema(type="string")
             *     ),
             *     @OA\\Response(
             *         response=200,
             *         description="List of files",
             *         @OA\\JsonContent(
             *             type="array",
             *             @OA\\Items(ref="#/components/schemas/StoredFileResource")
             *         )
             *     )
             * )
             */
            public function index(): JsonResponse
            {
                $directory = request()->query('directory');
                $files = $this->fileStorageService->list($directory);

                return StoredFileResource::collection($files)->response();
            }
        }
        """;
    }

    private String generateService(boolean useS3, boolean useAzure) {
        String diskSelection = "config('filesystems.default')";
        if (useS3) {
            diskSelection = "config('filesystems.cloud', 's3')";
        } else if (useAzure) {
            diskSelection = "config('filesystems.cloud', 'azure')";
        }

        return String.format(
                """
                <?php

                namespace App\\Services;

                use App\\Models\\StoredFile;
                use Illuminate\\Http\\UploadedFile;
                use Illuminate\\Support\\Facades\\Log;
                use Illuminate\\Support\\Facades\\Storage;
                use Illuminate\\Support\\Str;
                use Symfony\\Component\\HttpFoundation\\StreamedResponse;
                use Symfony\\Component\\HttpKernel\\Exception\\NotFoundHttpException;

                class FileStorageService
                {
                    private string $disk;

                    public function __construct()
                    {
                        $this->disk = %s;
                    }

                    /**
                     * Store an uploaded file.
                     */
                    public function store(UploadedFile $file, string $directory = 'uploads'): StoredFile
                    {
                        $filename = $this->generateUniqueFilename($file);
                        $path = $directory . '/' . $filename;

                        // Store file
                        Storage::disk($this->disk)->put($path, file_get_contents($file->getRealPath()));

                        // Create database record
                        $storedFile = StoredFile::create([
                            'original_name' => $file->getClientOriginalName(),
                            'filename' => $filename,
                            'path' => $path,
                            'mime_type' => $file->getMimeType(),
                            'size' => $file->getSize(),
                            'disk' => $this->disk,
                        ]);

                        Log::info("File uploaded: {$path}");
                        return $storedFile;
                    }

                    /**
                     * Find a file by ID or fail.
                     */
                    public function findOrFail(string $id): StoredFile
                    {
                        $file = StoredFile::find($id);
                        if (!$file) {
                            throw new NotFoundHttpException('File not found');
                        }
                        return $file;
                    }

                    /**
                     * Download a file.
                     */
                    public function download(string $id): StreamedResponse
                    {
                        $file = $this->findOrFail($id);

                        if (!Storage::disk($file->disk)->exists($file->path)) {
                            throw new NotFoundHttpException('File not found in storage');
                        }

                        return Storage::disk($file->disk)->download($file->path, $file->original_name);
                    }

                    /**
                     * Get a file URL.
                     */
                    public function getUrl(string $id): string
                    {
                        $file = $this->findOrFail($id);
                        return Storage::disk($file->disk)->url($file->path);
                    }

                    /**
                     * Get a temporary URL (for private files).
                     */
                    public function getTemporaryUrl(string $id, int $expirationMinutes = 60): string
                    {
                        $file = $this->findOrFail($id);
                        return Storage::disk($file->disk)->temporaryUrl(
                            $file->path,
                            now()->addMinutes($expirationMinutes)
                        );
                    }

                    /**
                     * Delete a file.
                     */
                    public function delete(string $id): bool
                    {
                        $file = $this->findOrFail($id);

                        // Delete from storage
                        if (Storage::disk($file->disk)->exists($file->path)) {
                            Storage::disk($file->disk)->delete($file->path);
                        }

                        // Delete database record
                        $file->delete();

                        Log::info("File deleted: {$file->path}");
                        return true;
                    }

                    /**
                     * List files in a directory.
                     */
                    public function list(?string $directory = null): \\Illuminate\\Database\\Eloquent\\Collection
                    {
                        $query = StoredFile::query();

                        if ($directory) {
                            $query->where('path', 'like', $directory . '/%%');
                        }

                        return $query->orderBy('created_at', 'desc')->get();
                    }

                    /**
                     * Check if a file exists.
                     */
                    public function exists(string $id): bool
                    {
                        $file = StoredFile::find($id);
                        if (!$file) {
                            return false;
                        }
                        return Storage::disk($file->disk)->exists($file->path);
                    }

                    /**
                     * Generate a unique filename.
                     */
                    private function generateUniqueFilename(UploadedFile $file): string
                    {
                        $extension = $file->getClientOriginalExtension();
                        return Str::uuid() . '.' . $extension;
                    }
                }
                """,
                diskSelection);
    }

    private String generateModel() {
        return """
        <?php

        namespace App\\Models;

        use Illuminate\\Database\\Eloquent\\Concerns\\HasUuids;
        use Illuminate\\Database\\Eloquent\\Model;

        /**
         * @OA\\Schema(
         *     schema="StoredFile",
         *     @OA\\Property(property="id", type="string", format="uuid"),
         *     @OA\\Property(property="original_name", type="string"),
         *     @OA\\Property(property="filename", type="string"),
         *     @OA\\Property(property="path", type="string"),
         *     @OA\\Property(property="mime_type", type="string"),
         *     @OA\\Property(property="size", type="integer"),
         *     @OA\\Property(property="disk", type="string"),
         *     @OA\\Property(property="created_at", type="string", format="date-time"),
         *     @OA\\Property(property="updated_at", type="string", format="date-time")
         * )
         */
        class StoredFile extends Model
        {
            use HasUuids;

            protected $fillable = [
                'original_name',
                'filename',
                'path',
                'mime_type',
                'size',
                'disk',
            ];

            protected $casts = [
                'size' => 'integer',
            ];
        }
        """;
    }

    private String generateResource() {
        return """
        <?php

        namespace App\\Http\\Resources;

        use Illuminate\\Http\\Request;
        use Illuminate\\Http\\Resources\\Json\\JsonResource;
        use Illuminate\\Support\\Facades\\Storage;

        /**
         * @OA\\Schema(
         *     schema="StoredFileResource",
         *     @OA\\Property(property="id", type="string", format="uuid"),
         *     @OA\\Property(property="original_name", type="string"),
         *     @OA\\Property(property="mime_type", type="string"),
         *     @OA\\Property(property="size", type="integer"),
         *     @OA\\Property(property="url", type="string", format="uri"),
         *     @OA\\Property(property="created_at", type="string", format="date-time")
         * )
         */
        class StoredFileResource extends JsonResource
        {
            public function toArray(Request $request): array
            {
                return [
                    'id' => $this->id,
                    'original_name' => $this->original_name,
                    'mime_type' => $this->mime_type,
                    'size' => $this->size,
                    'size_human' => $this->formatSize($this->size),
                    'url' => Storage::disk($this->disk)->url($this->path),
                    'created_at' => $this->created_at->toIso8601String(),
                ];
            }

            private function formatSize(int $bytes): string
            {
                $units = ['B', 'KB', 'MB', 'GB', 'TB'];
                $i = 0;
                while ($bytes >= 1024 && $i < count($units) - 1) {
                    $bytes /= 1024;
                    $i++;
                }
                return round($bytes, 2) . ' ' . $units[$i];
            }
        }
        """;
    }

    private String generateUploadRequest() {
        return """
        <?php

        namespace App\\Http\\Requests;

        use Illuminate\\Foundation\\Http\\FormRequest;

        class UploadFileRequest extends FormRequest
        {
            public function authorize(): bool
            {
                return true;
            }

            public function rules(): array
            {
                return [
                    'file' => [
                        'required',
                        'file',
                        'max:10240', // 10MB max
                        'mimes:jpg,jpeg,png,gif,pdf,doc,docx,xls,xlsx,txt,csv,zip',
                    ],
                    'directory' => ['sometimes', 'string', 'max:255'],
                ];
            }

            public function messages(): array
            {
                return [
                    'file.required' => 'A file is required.',
                    'file.max' => 'The file may not be greater than 10MB.',
                    'file.mimes' => 'The file must be one of the following types: jpg, jpeg, png, gif, pdf, doc, docx, xls, xlsx, txt, csv, zip.',
                ];
            }
        }
        """;
    }

    private String generateMigration() {
        return """
        <?php

        use Illuminate\\Database\\Migrations\\Migration;
        use Illuminate\\Database\\Schema\\Blueprint;
        use Illuminate\\Support\\Facades\\Schema;

        return new class extends Migration
        {
            public function up(): void
            {
                Schema::create('stored_files', function (Blueprint $table) {
                    $table->uuid('id')->primary();
                    $table->string('original_name');
                    $table->string('filename');
                    $table->string('path');
                    $table->string('mime_type');
                    $table->unsignedBigInteger('size');
                    $table->string('disk')->default('local');
                    $table->timestamps();

                    $table->index('path');
                    $table->index('mime_type');
                });
            }

            public function down(): void
            {
                Schema::dropIfExists('stored_files');
            }
        };
        """;
    }

    private String generateS3Config() {
        return """
        <?php

        /**
         * S3 Storage Configuration.
         * Add this to your config/filesystems.php 'disks' array.
         */

        return [
            's3' => [
                'driver' => 's3',
                'key' => env('AWS_ACCESS_KEY_ID'),
                'secret' => env('AWS_SECRET_ACCESS_KEY'),
                'region' => env('AWS_DEFAULT_REGION', 'us-east-1'),
                'bucket' => env('AWS_BUCKET'),
                'url' => env('AWS_URL'),
                'endpoint' => env('AWS_ENDPOINT'),
                'use_path_style_endpoint' => env('AWS_USE_PATH_STYLE_ENDPOINT', false),
                'throw' => false,
            ],
        ];

        /*
         * Environment variables needed in .env:
         *
         * AWS_ACCESS_KEY_ID=your-key
         * AWS_SECRET_ACCESS_KEY=your-secret
         * AWS_DEFAULT_REGION=us-east-1
         * AWS_BUCKET=your-bucket
         * AWS_URL=
         * AWS_ENDPOINT=
         * AWS_USE_PATH_STYLE_ENDPOINT=false
         *
         * For production, also set:
         * FILESYSTEM_DISK=s3
         */
        """;
    }

    private String generateAzureConfig() {
        return """
        <?php

        /**
         * Azure Blob Storage Configuration.
         * Add this to your config/filesystems.php 'disks' array.
         *
         * First, install the Azure package:
         * composer require matthewbdaly/laravel-azure-storage
         */

        return [
            'azure' => [
                'driver' => 'azure',
                'name' => env('AZURE_STORAGE_NAME'),
                'key' => env('AZURE_STORAGE_KEY'),
                'container' => env('AZURE_STORAGE_CONTAINER', 'files'),
                'url' => env('AZURE_STORAGE_URL'),
                'prefix' => null,
                'connection_string' => env('AZURE_STORAGE_CONNECTION_STRING'),
            ],
        ];

        /*
         * Environment variables needed in .env:
         *
         * AZURE_STORAGE_NAME=your-storage-account-name
         * AZURE_STORAGE_KEY=your-storage-key
         * AZURE_STORAGE_CONTAINER=files
         * AZURE_STORAGE_URL=
         * AZURE_STORAGE_CONNECTION_STRING=
         *
         * For production, also set:
         * FILESYSTEM_DISK=azure
         */
        """;
    }
}
