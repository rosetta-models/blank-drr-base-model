package com.regnosys.bank.testpack;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class SampleIngestor {
    public static void main(String[] args) {

        String outputDirectoryPath = "/Users/user/dev/github/rosetta-models/jpm/rosetta-source/src/main/resources/jpm-file-drop";
        Path sampleSourceDir = Paths.get("/Users/user/Documents/Client_Sample_Data/Oct/18_09_24_JPM_SAMPLE/Downloads"); // This should be where you are storing the samples.

        List<String> jsonFilePathList = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(sampleSourceDir)) {
            for (Path entry : stream) {
                if (entry.toString().endsWith(".json")) {
                    jsonFilePathList.add(entry.toString());
                }
            }
        } catch (IOException | DirectoryIteratorException ex) {
            System.err.println(ex);
        }

        for (String filePath : jsonFilePathList) {
            splitJsonSampleArray(filePath, outputDirectoryPath);
        }
    }

    private static void splitJsonSampleArray(String inputFilePath, String outputDirectoryPath) {
        File inputFile = new File(inputFilePath);
        File outputDirectory = new File(outputDirectoryPath);

        if (!outputDirectory.exists()) {
            System.out.println("Output directory does not exist. Creating directory...");
            if (!outputDirectory.mkdirs()) {
                System.out.println("Failed to create output directory.");
                return;
            }
        }

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        try {
            // Read the JSON file into a JsonNode
            JsonNode rootNode = objectMapper.readTree(inputFile);

            if (!rootNode.isArray()) {
                System.out.println("The provided JSON file does not contain a JSON array.");
                return;
            }

            // Get the base filename without extension
            String baseName = getBaseName(inputFilePath);
            String extension = getExtension(inputFilePath);

            if(rootNode.size()>1){
                // Iterate over the elements of the array and write each to a new file
                for (int i = 0; i < rootNode.size(); i++) {
                    JsonNode node = rootNode.get(i);
                    String newFileName;
                    if(i<10){
                        newFileName = baseName + "_0" + (i + 1) + extension;
                    }else{
                        newFileName = baseName + "_" + (i + 1) + extension;
                    }
                    File newFile = new File(outputDirectory, newFileName);
                    objectMapper.writeValue(newFile, node);
                }
            }else{
                JsonNode node = rootNode.get(0);
                String newFileName;
                newFileName = baseName + extension;
                File newFile = new File(outputDirectory, newFileName);
                objectMapper.writeValue(newFile, node);
            }

            System.out.println("JSON array entries have been split into separate files.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String getBaseName(String filePath) {
        File file = new File(filePath);
        String name = file.getName();
        int lastDot = name.lastIndexOf('.');
        if (lastDot == -1) {
            return name;
        }
        return name.substring(0, lastDot);
    }

    private static String getExtension(String filePath) {
        File file = new File(filePath);
        String name = file.getName();
        int lastDot = name.lastIndexOf('.');
        if (lastDot == -1) {
            return "";
        }
        return name.substring(lastDot);
    }
}