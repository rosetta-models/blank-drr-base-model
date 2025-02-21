package com.regnosys.bank;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.Patch;
import com.github.difflib.unifieddiff.UnifiedDiff;
import com.github.difflib.unifieddiff.UnifiedDiffFile;
import com.github.difflib.unifieddiff.UnifiedDiffWriter;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class ModelForkTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ModelForkTest.class);
    static final Boolean UPDATE_OR_CREATE_MODEL_DIFFS = Optional.ofNullable(System.getenv("UPDATE_OR_CREATE_MODEL_DIFFS")).map(Boolean::parseBoolean).orElse(false);

    public static Stream<Arguments> inputs() throws IOException {

        Map<String, String> expectedDiffs = Files.list(Path.of("src/main/resources"))
                .filter(x -> x.getFileName().toString().endsWith(".diff"))
                .collect(Collectors.toMap(ModelForkTest::rosettaFileNameFromDiff, ModelForkTest::readDiffFile));

        List<Arguments> arguments = new ArrayList<>();

        List<Path> modelFiles = Files.walk(Path.of("../rosetta-source/src/main/rosetta"))
                .filter(x -> x.getFileName().toString().endsWith(".rosetta"))
                .collect(Collectors.toList());

        Set<String> modelFileNames = modelFiles.stream().map(x -> x.getFileName().toString()).collect(Collectors.toSet());

        List<Path> parentModelFiles = Files.walk(Path.of("../rosetta-source/target/parent-dependency"))
                .filter(x -> x.getFileName().toString().endsWith(".rosetta"))
                .filter(x -> modelFileNames.contains(x.getFileName().toString()))
                .collect(Collectors.toList());


        System.out.println();

        for (Path parentModelFile : parentModelFiles) {
            String parentModelFileName = parentModelFile.getFileName().toString();

            Path modelFile = modelFiles.stream()
                    .filter(x -> x.getFileName().toString().equals(parentModelFileName))
                    .findFirst().orElseThrow();

            String modelFileName = modelFile.getFileName().toString();
            String expectedDiff = expectedDiffs.get(modelFileName);

            arguments.add(Arguments.of(modelFileName, parentModelFile, modelFile, expectedDiff));
        }

        return arguments.stream();
    }

    @MethodSource("inputs")
    @ParameterizedTest(autoCloseArguments = true, name = "{0}")
    void checkIfFileIsForked(String modelFileName, Path parentModelFile, Path modelFile, String expectedDiff) throws IOException {
        List<String> parentModelFileContent = Files.readAllLines(parentModelFile);
        parentModelFileContent.remove(1);
        List<String> modelFileContent = Files.readAllLines(modelFile);
        modelFileContent.remove(1);


        if (parentModelFileContent.equals(modelFileContent)) {
            printFilePaths(parentModelFile, modelFile);
            assertFalse(expectedDiff != null, "Stale diff file  " + modelFileName + ". You probably want to delete it");
            assertEquals(Files.readString(parentModelFile), Files.readString(modelFile), "File " + modelFileName + " was forked but there is no change in the file. It is likely that this is forked accidentally by Rosetta");
        } else {
            String parentModelFileName = parentModelFile.getFileName().toString();
            String actualDiff = createPatchFile(parentModelFileContent, modelFileContent, parentModelFileName, modelFileName);
//            assertTrue(modelFileContent.toString().startsWith("[override"));
            if (expectedDiff != null) {
                printFilePaths(parentModelFile, modelFile);
                updateOrCreateForkedModelDiffs(modelFileName, actualDiff);
                assertEquals(expectedDiff, actualDiff);
            } else {
                updateOrCreateForkedModelDiffs(modelFileName, actualDiff);
                assertEquals(Files.readString(parentModelFile), Files.readString(modelFile), "File " + modelFileName + " was unexpected forked ." + actualDiff);
            }
        }
    }

    private static void updateOrCreateForkedModelDiffs(String modelFileName, String actualDiff) throws IOException {
        if (UPDATE_OR_CREATE_MODEL_DIFFS){
        Files.write(Path.of(String.format("src/main/resources/expected-%s.diff", modelFileName.replace(".rosetta", ""))), actualDiff.getBytes());
        }
    }

    private static void printFilePaths(Path parentModelFile, Path modelFile) {
        // Print the file names so its easier to nav
        System.out.println("parentModelFile : (file://" + parentModelFile.toAbsolutePath().normalize() + ")");
        System.out.println("modelFile       : (file://" + modelFile.toAbsolutePath().normalize() + ")");
        System.out.println();
    }

    @NotNull
    private static String createPatchFile(List<String> parentModelFileContent, List<String> modelFileContent, String parentModelFileName, String modelFileName) throws IOException {
        Patch<String> patch = DiffUtils.diff(parentModelFileContent, modelFileContent);
        UnifiedDiffFile revised = UnifiedDiffFile.from(parentModelFileName, modelFileName, patch);
        UnifiedDiff unifiedDiff = UnifiedDiff.from(null, null, revised);
        StringBuilder writer = new StringBuilder();
        UnifiedDiffWriter.write(unifiedDiff, f -> parentModelFileContent, s -> writer.append(s.trim()).append("\n"), 0);
        return writer.toString();
    }


    private static String readDiffFile(String fileName) {
        return readDiffFile(Path.of(fileName));
    }

    private static String rosettaFileNameFromDiff(Path file) {
        List<String> diffLines;
        try {
            diffLines = Files.readAllLines(file);
        } catch (IOException e) {
            LOGGER.error("Failed to read file " + file.getFileName().toString(), e);
            throw new RuntimeException(e);
        }

        String firstDiffLine = diffLines.get(0);
        if (!firstDiffLine.startsWith("---")) {
            throw new IllegalStateException("Expected --- as the first line of the diff " + file.getFileName().toString());
        }
        if (!firstDiffLine.endsWith(".rosetta")) {
            throw new IllegalStateException("Expected diff file to be a .rosetta file" + file.getFileName().toString());
        }

        return firstDiffLine.replace("---", "").trim();
    }


    private static String readDiffFile(Path file) {
        try {
            return Files.readString(file);
        } catch (IOException e) {
            LOGGER.error("Failed to read file " + file.getFileName().toString(), e);
            throw new RuntimeException(e);
        }
    }

}
