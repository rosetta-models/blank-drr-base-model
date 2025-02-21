package com.regnosys.bank.testpack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileUtils.class);

    private static final String SUFFIX = ".json";

    public static List<Path> getDirectoriesWithJsonFiles(Path path) {
        try (Stream<Path> fileStream = Files.walk(path)) {
            return fileStream.filter(Files::isDirectory)
                    .filter(FileUtils::hasJsonFile)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static boolean hasJsonFile(Path path) {
        try (Stream<Path> fileStream = Files.list(path)) {
            return fileStream
                    .filter(Files::isRegularFile)
                    .anyMatch(y -> y.toString().endsWith(SUFFIX));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static List<Path> getJsonFiles(Path path) {
        try (Stream<Path> fileStream = Files.walk(path)) {
            return fileStream
                    .filter(Files::isRegularFile)
                    .filter(d -> d.getFileName().toString().endsWith(SUFFIX))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void writeFile(Path dest, String contentToWrite) {
        try {
            Files.createDirectories(dest.getParent());
            Files.write(dest, contentToWrite.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void combineXMLFilesFromFolder(String inputFolderPath, String outputFile) throws IOException {
        LOGGER.info("Starting to combine XML files from folder: {}", inputFolderPath);
        List<String> xmlFiles = new ArrayList<>();

        // Recursively collect all XML files from the directory and subdirectories
        Files.walk(Paths.get(inputFolderPath))
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".xml"))
                .forEach(path -> xmlFiles.add(path.toString()));

        if (xmlFiles.isEmpty()) {
            LOGGER.warn("No XML files found in the specified directory: {}", inputFolderPath);
            return;
        }

        LOGGER.info("Found {} XML files to combine.", xmlFiles.size());
        Path outputPath = Paths.get(outputFile);
        Path parentDir = outputPath.getParent();

        if (parentDir != null && !Files.exists(parentDir)) {
            try {
                Files.createDirectories(parentDir);
                LOGGER.info("Created parent directories for the output file: {}", parentDir);
            } catch (IOException e) {
                LOGGER.error("Failed to create parent directories for the output file: {}", parentDir, e);
                throw e;
            }
        }
        if (!Files.exists(outputPath)) {
            try {
                Files.createFile(outputPath);
                LOGGER.info("Created the output file: {}", outputPath);
            } catch (IOException e) {
                LOGGER.error("Failed to create the output file: {}", outputPath, e);
                throw e;
            }
        }
        combineXMLFiles(xmlFiles, outputFile);
    }


    private static void combineXMLFiles(List<String> inputFiles, String outputFile) throws IOException {
        LOGGER.info("Combining XML files into: {}", outputFile);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            boolean firstFile = true;

            for (String inputFile : inputFiles) {
                LOGGER.info("Processing file: {}", inputFile);
                List<String> lines = Files.readAllLines(Paths.get(inputFile));

                if (!firstFile && !lines.isEmpty() && lines.get(0).startsWith("<?xml")) {
                    lines.remove(0);
                    LOGGER.info("Skipped XML declaration in file: {}", inputFile);
                }

                for (String line : lines) {
                    writer.write(line);
                    writer.newLine();
                }
                firstFile = false;
            }
            LOGGER.info("Successfully combined XML files into: {}", outputFile);
        } catch (IOException e) {
            LOGGER.error("An error occurred while combining XML files: {}", e.getMessage(), e);
            throw e;
        }
    }
}
