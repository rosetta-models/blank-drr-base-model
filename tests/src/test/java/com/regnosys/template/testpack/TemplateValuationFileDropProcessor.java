package com.regnosys.template.testpack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Collectors;

public class TemplateValuationFileDropProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(TemplateValuationFileDropProcessor.class);

    void runFileProcessor(String groupName, Path fileDropPath, Path enrichInputPath) throws IOException {
        List<Path> jsonFilePathList = Files.walk(fileDropPath.resolve(groupName))
                .filter(f -> f.getFileName().toString().endsWith(".json"))
                .collect(Collectors.toList());

        for (Path filePath : jsonFilePathList) {
            processSample(groupName, filePath, enrichInputPath);
        }
    }

    private void processSample(String groupName, Path inputFile, Path outputDirectory) throws IOException {
        String fileName = inputFile.getFileName().toString();
        Path testPackPath = Files.createDirectories(outputDirectory.resolve(groupName));
        Path newFile = testPackPath.resolve(fileName);
        Files.copy(inputFile, newFile, StandardCopyOption.REPLACE_EXISTING);
        LOGGER.info("File written: {} to {}", fileName, testPackPath);
    }
}
