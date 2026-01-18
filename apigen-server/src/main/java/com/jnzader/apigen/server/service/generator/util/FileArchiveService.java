package com.jnzader.apigen.server.service.generator.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Service for file archiving operations.
 */
@Component
@Slf4j
public class FileArchiveService {

    /**
     * Creates a ZIP file from a directory.
     *
     * @param sourceDir      the source directory to zip
     * @param rootFolderName the name of the root folder in the ZIP
     * @return the ZIP file as a byte array
     * @throws IOException if an I/O error occurs
     */
    public byte[] createZipFromDirectory(Path sourceDir, String rootFolderName) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            try (Stream<Path> paths = Files.walk(sourceDir)) {
                paths.filter(path -> !Files.isDirectory(path))
                        .forEach(path -> addFileToZip(zos, sourceDir, path, rootFolderName));
            }
        }

        return baos.toByteArray();
    }

    /**
     * Adds a single file to the ZIP output stream.
     *
     * @param zos            the ZIP output stream
     * @param sourceDir      the source directory base path
     * @param path           the file path to add
     * @param rootFolderName the root folder name in the ZIP
     */
    public void addFileToZip(ZipOutputStream zos, Path sourceDir, Path path, String rootFolderName) {
        String zipEntryName = rootFolderName + "/" +
                sourceDir.relativize(path).toString().replace("\\", "/");
        try {
            zos.putNextEntry(new ZipEntry(zipEntryName));
            Files.copy(path, zos);
            zos.closeEntry();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to add file to ZIP: " + path, e);
        }
    }

    /**
     * Deletes a directory recursively.
     *
     * @param directory the directory to delete
     */
    public void deleteDirectory(Path directory) {
        try (Stream<Path> paths = Files.walk(directory)) {
            paths.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException _) {
                            log.warn("Failed to delete temp file: {}", path);
                        }
                    });
        } catch (IOException _) {
            log.warn("Failed to clean up temp directory: {}", directory);
        }
    }
}
