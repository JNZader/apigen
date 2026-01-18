package com.jnzader.apigen.server.service.generator.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FileArchiveService Tests")
class FileArchiveServiceTest {

    private FileArchiveService fileArchiveService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        fileArchiveService = new FileArchiveService();
    }

    @Nested
    @DisplayName("createZipFromDirectory()")
    class CreateZipFromDirectoryTests {

        @Test
        @DisplayName("Should create ZIP with single file")
        void shouldCreateZipWithSingleFile() throws IOException {
            // Create a file
            Files.writeString(tempDir.resolve("test.txt"), "Hello World");

            // Create ZIP
            byte[] zipBytes = fileArchiveService.createZipFromDirectory(tempDir, "myproject");

            // Verify ZIP contents
            Set<String> entries = getZipEntries(zipBytes);
            assertThat(entries).containsExactly("myproject/test.txt");
        }

        @Test
        @DisplayName("Should create ZIP with nested directories")
        void shouldCreateZipWithNestedDirectories() throws IOException {
            // Create nested structure
            Path subDir = tempDir.resolve("src/main/java");
            Files.createDirectories(subDir);
            Files.writeString(subDir.resolve("App.java"), "public class App {}");
            Files.writeString(tempDir.resolve("build.gradle"), "plugins {}");

            // Create ZIP
            byte[] zipBytes = fileArchiveService.createZipFromDirectory(tempDir, "project");

            // Verify ZIP contents
            Set<String> entries = getZipEntries(zipBytes);
            assertThat(entries).containsExactlyInAnyOrder(
                    "project/src/main/java/App.java",
                    "project/build.gradle"
            );
        }

        @Test
        @DisplayName("Should create ZIP with multiple files")
        void shouldCreateZipWithMultipleFiles() throws IOException {
            Files.writeString(tempDir.resolve("file1.txt"), "Content 1");
            Files.writeString(tempDir.resolve("file2.txt"), "Content 2");
            Files.writeString(tempDir.resolve("file3.txt"), "Content 3");

            byte[] zipBytes = fileArchiveService.createZipFromDirectory(tempDir, "archive");

            Set<String> entries = getZipEntries(zipBytes);
            assertThat(entries).containsExactlyInAnyOrder(
                    "archive/file1.txt",
                    "archive/file2.txt",
                    "archive/file3.txt"
            );
        }

        @Test
        @DisplayName("Should handle empty directory")
        void shouldHandleEmptyDirectory() throws IOException {
            byte[] zipBytes = fileArchiveService.createZipFromDirectory(tempDir, "empty");

            assertThat(zipBytes).isNotNull();
            Set<String> entries = getZipEntries(zipBytes);
            assertThat(entries).isEmpty();
        }

        @Test
        @DisplayName("Should preserve file content")
        void shouldPreserveFileContent() throws IOException {
            String content = "This is test content with special chars: áéíóú ñ";
            Files.writeString(tempDir.resolve("content.txt"), content);

            byte[] zipBytes = fileArchiveService.createZipFromDirectory(tempDir, "test");

            // Extract and verify content
            try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
                ZipEntry entry = zis.getNextEntry();
                assertThat(entry).isNotNull();
                String extractedContent = new String(zis.readAllBytes());
                assertThat(extractedContent).isEqualTo(content);
            }
        }

        @Test
        @DisplayName("Should use forward slashes in ZIP paths")
        void shouldUseForwardSlashesInZipPaths() throws IOException {
            Path subDir = tempDir.resolve("src").resolve("main");
            Files.createDirectories(subDir);
            Files.writeString(subDir.resolve("file.txt"), "content");

            byte[] zipBytes = fileArchiveService.createZipFromDirectory(tempDir, "project");

            Set<String> entries = getZipEntries(zipBytes);
            assertThat(entries)
                    .isNotEmpty()
                    .allSatisfy(entry ->
                            assertThat(entry).doesNotContain("\\")
                    );
        }

        private Set<String> getZipEntries(byte[] zipBytes) throws IOException {
            Set<String> entries = new HashSet<>();
            try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    entries.add(entry.getName());
                }
            }
            return entries;
        }
    }

    @Nested
    @DisplayName("deleteDirectory()")
    class DeleteDirectoryTests {

        @Test
        @DisplayName("Should delete directory with files")
        void shouldDeleteDirectoryWithFiles() throws IOException {
            Path testDir = tempDir.resolve("toDelete");
            Files.createDirectories(testDir);
            Files.writeString(testDir.resolve("file1.txt"), "content1");
            Files.writeString(testDir.resolve("file2.txt"), "content2");

            fileArchiveService.deleteDirectory(testDir);

            assertThat(testDir).doesNotExist();
        }

        @Test
        @DisplayName("Should delete nested directories")
        void shouldDeleteNestedDirectories() throws IOException {
            Path testDir = tempDir.resolve("toDelete");
            Path nestedDir = testDir.resolve("level1/level2/level3");
            Files.createDirectories(nestedDir);
            Files.writeString(nestedDir.resolve("deep.txt"), "deep content");
            Files.writeString(testDir.resolve("shallow.txt"), "shallow content");

            fileArchiveService.deleteDirectory(testDir);

            assertThat(testDir).doesNotExist();
        }

        @Test
        @DisplayName("Should handle empty directory")
        void shouldHandleEmptyDirectoryDeletion() throws IOException {
            Path testDir = tempDir.resolve("emptyDir");
            Files.createDirectories(testDir);

            fileArchiveService.deleteDirectory(testDir);

            assertThat(testDir).doesNotExist();
        }

        @Test
        @DisplayName("Should handle non-existent directory gracefully")
        void shouldHandleNonExistentDirectoryGracefully() {
            Path nonExistent = tempDir.resolve("doesNotExist");

            // Should not throw exception and directory should still not exist
            fileArchiveService.deleteDirectory(nonExistent);

            assertThat(nonExistent).doesNotExist();
        }

        @Test
        @DisplayName("Should delete directory with mixed content")
        void shouldDeleteDirectoryWithMixedContent() throws IOException {
            Path testDir = tempDir.resolve("mixed");
            Files.createDirectories(testDir.resolve("subdir1"));
            Files.createDirectories(testDir.resolve("subdir2/nested"));
            Files.writeString(testDir.resolve("root.txt"), "root");
            Files.writeString(testDir.resolve("subdir1/file1.txt"), "file1");
            Files.writeString(testDir.resolve("subdir2/nested/file2.txt"), "file2");

            fileArchiveService.deleteDirectory(testDir);

            assertThat(testDir).doesNotExist();
        }
    }
}
