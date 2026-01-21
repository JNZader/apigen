package com.jnzader.apigen.server.service.generator;

import com.jnzader.apigen.server.config.GeneratedProjectVersions;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * Generates Gradle wrapper files for generated projects. Copies the wrapper scripts and jar from
 * bundled resources and generates the properties file with the configured Gradle version.
 */
@Component
@Slf4j
public class GradleWrapperGenerator {

    private static final String WRAPPER_RESOURCES_PATH = "gradle-wrapper/";

    /**
     * Generates the gradle-wrapper.properties file content.
     *
     * @return the properties file content
     */
    public String generateWrapperProperties() {
        return generateWrapperProperties(GeneratedProjectVersions.GRADLE_VERSION);
    }

    /**
     * Generates the gradle-wrapper.properties file content with a specific Gradle version.
     *
     * @param gradleVersion the Gradle version to use
     * @return the properties file content
     */
    public String generateWrapperProperties(String gradleVersion) {
        return """
        distributionBase=GRADLE_USER_HOME
        distributionPath=wrapper/dists
        distributionUrl=https\\://services.gradle.org/distributions/gradle-%s-bin.zip
        networkTimeout=10000
        validateDistributionUrl=true
        zipStoreBase=GRADLE_USER_HOME
        zipStorePath=wrapper/dists
        """
                .formatted(gradleVersion);
    }

    /**
     * Copies all Gradle wrapper files to the target project directory.
     *
     * @param projectRoot the project root directory
     * @throws IOException if file operations fail
     */
    public void generateWrapperFiles(Path projectRoot) throws IOException {
        // Create gradle/wrapper directory
        Path wrapperDir = projectRoot.resolve("gradle/wrapper");
        Files.createDirectories(wrapperDir);

        // Generate gradle-wrapper.properties
        Files.writeString(
                wrapperDir.resolve("gradle-wrapper.properties"), generateWrapperProperties());

        // Copy wrapper scripts and jar from resources
        copyResource("gradlew", projectRoot.resolve("gradlew"), true);
        copyResource("gradlew.bat", projectRoot.resolve("gradlew.bat"), false);
        copyResource("gradle-wrapper.jar", wrapperDir.resolve("gradle-wrapper.jar"), false);

        log.debug("Generated Gradle wrapper files for project at {}", projectRoot);
    }

    /**
     * Copies a resource file to the target path.
     *
     * @param resourceName the resource name
     * @param targetPath the target path
     * @param executable whether to make the file executable (Unix only)
     * @throws IOException if file operations fail
     */
    private void copyResource(String resourceName, Path targetPath, boolean executable)
            throws IOException {
        ClassPathResource resource = new ClassPathResource(WRAPPER_RESOURCES_PATH + resourceName);

        if (!resource.exists()) {
            log.warn("Gradle wrapper resource not found: {}", resourceName);
            return;
        }

        try (InputStream is = resource.getInputStream()) {
            Files.copy(is, targetPath);
        }

        // Try to make executable on Unix systems
        if (executable) {
            try {
                Set<PosixFilePermission> perms = Files.getPosixFilePermissions(targetPath);
                perms.add(PosixFilePermission.OWNER_EXECUTE);
                perms.add(PosixFilePermission.GROUP_EXECUTE);
                perms.add(PosixFilePermission.OTHERS_EXECUTE);
                Files.setPosixFilePermissions(targetPath, perms);
            } catch (UnsupportedOperationException e) {
                // Windows doesn't support POSIX permissions, ignore
                log.trace("POSIX file permissions not supported on this system");
            }
        }
    }
}
