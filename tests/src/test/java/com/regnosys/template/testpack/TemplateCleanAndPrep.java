package com.regnosys.template.testpack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.Stream;

public class TemplateCleanAndPrep {

    private static final Logger LOGGER = LoggerFactory.getLogger(TemplateCleanAndPrep.class);

    static void emptySampleAndTestPackFolders(Path... foldersToClean) {
        // Iterate through each folder and delete its contents
       Arrays.stream(foldersToClean)
                .filter(Files::exists)
                .filter(Files::isDirectory)
                .forEach(TemplateCleanAndPrep::delete);
    }

    private static void delete(Path folder) {
        try (Stream<Path> paths = Files.walk(folder)) {
            paths.sorted(Comparator.reverseOrder())     // Sort to delete files before directories
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                            LOGGER.info("Deleted: {}", path);
                        } catch (IOException e) {
                            LOGGER.error("Failed to delete: {} ({})", path, e.getMessage());
                            throw new UncheckedIOException(e);
                        }
                    });
            LOGGER.info("Cleaned folder: {}", folder);
        } catch (IOException e) {
            LOGGER.error("Failed to clean folder: {} ({})", folder, e.getMessage());
            throw new UncheckedIOException(e);
        }
    }
}
