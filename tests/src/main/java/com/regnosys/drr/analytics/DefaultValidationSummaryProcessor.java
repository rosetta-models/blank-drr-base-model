package com.regnosys.drr.analytics;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.io.Resources;
import com.google.inject.Injector;
import com.regnosys.rosetta.common.serialisation.RosettaObjectMapper;
import com.regnosys.rosetta.common.serialisation.RosettaObjectMapperCreator;
import com.regnosys.rosetta.common.transform.TestPackModel;
import com.regnosys.rosetta.common.transform.TestPackUtils;
import com.regnosys.rosetta.common.validation.RosettaTypeValidator;
import com.regnosys.rosetta.common.validation.ValidationReport;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.validation.ValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DefaultValidationSummaryProcessor<IN extends RosettaModelObject> implements ValidationSummaryProcessor {

    private final Logger LOGGER = LoggerFactory.getLogger(DefaultValidationSummaryProcessor.class);

    private final Injector injector;
    private final RosettaTypeValidator validator;
    private final Path configPath;
    private final ObjectMapper objectMapper;
    private final ObjectWriter xmlObjectWriter;
    private final Validator xsdValidator;

    public DefaultValidationSummaryProcessor(Injector injector, RosettaTypeValidator validator, Path configPath, URL xmlConfig, URL xsdSchema) {
        this.injector = injector;
        this.validator = validator;
        this.configPath = configPath;
        this.objectMapper = RosettaObjectMapper.getNewRosettaObjectMapper();
        try {
            this.xmlObjectWriter = RosettaObjectMapperCreator.forXML(xmlConfig.openStream()).create().writerWithDefaultPrettyPrinter();
            SchemaFactory schemaFactory = SchemaFactory
                    .newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            // required to process xml elements with an maxOccurs greater than 5000 (rather than unbounded)
            schemaFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, false);
            Schema schema = schemaFactory.newSchema(xsdSchema);
            this.xsdValidator = schema.newValidator();
        } catch (IOException | SAXException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<ValidationData> processValidation(TransformData transformData) {
        String functionClassName = transformData.getFunctionClass().getName();
        Class<IN> inputClass = (Class<IN>) transformData.getInputClass();
        return getValidationData(
                getProjectionTestPackName(transformData.getPath()), // this is nasty
                functionClassName,
                inputClass,
                getTransformFunction(transformData.getFunctionClass(), inputClass));
    }

    private Function<IN, RosettaModelObject> getTransformFunction(Class<?> functionType, Class<IN> inputType) {
        Object functionInstance = injector.getInstance(functionType);
        Method evaluateMethod;
        try {
            evaluateMethod = functionInstance.getClass().getMethod("evaluate", inputType);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(String.format("Evaluate method with input type %s not found", inputType.getName()), e);
        }
        return (resolvedInput) -> {
            try {
                return (RosettaModelObject) evaluateMethod.invoke(functionInstance, resolvedInput);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException("Failed to invoke evaluate method", e);
            }
        };
    }

    public static String getProjectionTestPackName(String reportId) {
        return "test-pack-projection-" + reportId + "-report-to-iso20022.*jpm.*\\.json";
    }

    private <IN extends RosettaModelObject, OUT extends RosettaModelObject> List<ValidationData> getValidationData(String projectionTestPackFileName,
                                                                                                                   String projectionName,
                                                                                                                   Class<IN> inputType,
                                                                                                                   Function<IN, OUT> projectionFunc) {
        List<URL> expectationFiles = TestPackUtils.findPaths(configPath, getClass().getClassLoader(), projectionTestPackFileName);

        return expectationFiles.stream()
                .map(expectationUrl -> {
                    // Read projection expectation file
                    TestPackModel testPackModel =
                            TestPackUtils.readFile(expectationUrl, objectMapper, TestPackModel.class);
                    String testPack = testPackModel.getName();

                    return testPackModel.getSamples().stream()
                            .map(sampleModel -> {
                                // For each test pack sample
                                String inputFile = sampleModel.getInputPath();
                                URL inputFileUrl = Resources.getResource(inputFile);
                                String sampleName = getFileName(inputFileUrl);
                                LOGGER.info("Validating projection {}, test pack {}, sample {}", projectionName, testPack, sampleName);

                                IN transactionReport = TestPackUtils.readFile(inputFileUrl, objectMapper, inputType);

                                // Transaction report Rosetta validation
                                ValidationReport transactionValidation =
                                        validator.runProcessStep(transactionReport.getClass(), transactionReport);

                                TypeValidationData reportValidation = getReportValidation(transactionValidation);

                                TypeValidationData isoReportValidation;
                                String xsdSchemaValidationErrors;

                                try {
                                    // Run projection
                                    OUT isoReport = projectionFunc.apply(transactionReport);
                                    // ISO report Rosetta validation
                                    ValidationReport isoDocumentValidation =
                                            validator.runProcessStep(isoReport.getClass(), isoReport);

                                    isoReportValidation = getReportValidation(isoDocumentValidation);

                                    // XSD validation
                                    xsdSchemaValidationErrors = getXsdSchemaValidationErrors(isoReport);
                                } catch (Exception e) {
                                    LOGGER.error("Exception occurred generating project validation data", e);
                                    isoReportValidation = new TypeValidationData(e);
                                    xsdSchemaValidationErrors = null;
                                }

                                return new ValidationData(projectionName,
                                        testPack,
                                        sampleName,
                                        reportValidation,
                                        isoReportValidation,
                                        xsdSchemaValidationErrors);
                            })
                            .collect(Collectors.toList());
                })
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    private String getFileName(URL inputFileUrl) {
        try {
            return Path.of(inputFileUrl.toURI()).getFileName().toString();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private TypeValidationData getReportValidation(ValidationReport reportValidation) {
        AttributeValidationData cardinalityValidationData = getAttributeValidationData(reportValidation, ValidationResult.ValidationType.CARDINALITY);
        AttributeValidationData typeFormatValidationData = getAttributeValidationData(reportValidation, ValidationResult.ValidationType.TYPE_FORMAT);
        ConditionValidationData conditionValidationData = getConditionValidationData(reportValidation);
        return new TypeValidationData(cardinalityValidationData, typeFormatValidationData, conditionValidationData);
    }

    private AttributeValidationData getAttributeValidationData(ValidationReport reportValidation, ValidationResult.ValidationType validationType) {
        int totalCount =
                reportValidation.results()
                        .stream()
                        .filter(r -> r.getValidationType() == validationType)
                        .collect(Collectors.toList())
                        .size();
        List<ValidationResult<?>> failures = reportValidation.validationFailures();
        List<String> failedAttributeNames = failures.stream()
                .filter(r -> r.getValidationType() == validationType)
                .map(r -> r.getFailureReason().get())
                .map(this::getAttributeName)
                .sorted()
                .collect(Collectors.toList());
        return new AttributeValidationData(failedAttributeNames.size(), failedAttributeNames, totalCount);
    }

    private ConditionValidationData getConditionValidationData(ValidationReport reportValidation) {
        int totalConditionsCount =
                reportValidation.results()
                        .stream()
                        .filter(r -> r.getValidationType() == ValidationResult.ValidationType.DATA_RULE)
                        .collect(Collectors.toList())
                        .size();
        List<String> failedConditions =
                reportValidation.validationFailures()
                        .stream()
                        .filter(r -> r.getValidationType() == ValidationResult.ValidationType.DATA_RULE)
                        .map(r -> r.getName())
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
        return new ConditionValidationData(failedConditions.size(), failedConditions, totalConditionsCount);
    }

    private String getAttributeName(String reason) {
        try {
            String substring = reason.substring(reason.indexOf("'") + 1);
            return substring.substring(0, substring.indexOf("'"));
        } catch (Exception e) {
            LOGGER.error("Failed to get attribute name from reason {}", reason, e);
            throw e;
        }

    }

    private String getXsdSchemaValidationErrors(RosettaModelObject isoReport) {
        String actualXml;
        try {
            actualXml = xmlObjectWriter.writeValueAsString(isoReport);
        } catch (JsonProcessingException e) {
            LOGGER.error("Failed to serialise to xml", e);
            throw new RuntimeException(e);
        }
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(actualXml.getBytes(StandardCharsets.UTF_8))) {
            xsdValidator.validate(new StreamSource(inputStream));
            return null;
        } catch (SAXException e) {
            // Schema validation errors
            return e.getMessage();
        } catch (IOException e) {
            LOGGER.error("Failed to validate against xsd", e);
            throw new RuntimeException(e);
        }
    }
}
