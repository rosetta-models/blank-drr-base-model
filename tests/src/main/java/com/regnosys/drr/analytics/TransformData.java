package com.regnosys.drr.analytics;

public class TransformData {

    private final String path;
    private final Class<?> functionClass;
    private final Class<?> inputClass;
    private final String configXmlPath;
    private final String schemaPath;

    public TransformData(String path, Class<?> functionClass, Class<?> inputClass, String configXmlPath, String schemaPath) {
        this.path = path;
        this.functionClass = functionClass;
        this.inputClass = inputClass;
        this.configXmlPath = configXmlPath;
        this.schemaPath = schemaPath;
    }

    public String getPath() {
        return path;
    }

    public Class<?> getFunctionClass() {
        return functionClass;
    }

    public Class<?> getInputClass() {
        return inputClass;
    }

    public String getConfigXmlPath() {
        return configXmlPath;
    }

    public String getSchemaPath() {
        return schemaPath;
    }
}
