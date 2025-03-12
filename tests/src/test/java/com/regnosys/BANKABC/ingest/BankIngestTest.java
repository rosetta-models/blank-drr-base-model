package com.regnosys.acmebankdrr.ingest;

import acmebank.ingest.mapping.functions.Ingest_AcmeBankModelToWorkflowStep;
import com.google.common.io.Resources;
import com.regnosys.acmebankdrr.report.ReportTestRuntimeModule;
import com.regnosys.rosetta.common.transform.TestPackModel;
import com.regnosys.testing.transform.TransformTestExtension;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static com.regnosys.rosetta.common.transform.TestPackUtils.INGEST_CONFIG_PATH;

public class AcmeBankIngestTest {
    @RegisterExtension
    static TransformTestExtension<Ingest_AcmeBankModelToWorkflowStep> testExtension =
            new TransformTestExtension<>(
                    "pipeline-translate-acmebank-acme-bank-model-to-workflow-step",
                    new ReportTestRuntimeModule(),
                    INGEST_CONFIG_PATH,
                    Ingest_AcmeBankModelToWorkflowStep.class)
                    .withSchemaValidation(Resources.getResource("ingest/config/acmebank.xsd"));

    @ParameterizedTest(name = "{0}")
    @MethodSource("inputFiles")
    void runReport(String testName,
                   String testPackId,
                   TestPackModel.SampleModel sampleModel,
                   Ingest_AcmeBankModelToWorkflowStep func) {
        testExtension.runTransformAndAssert(testPackId, sampleModel, func::evaluate);
    }

    private static Stream<Arguments> inputFiles() {
        return testExtension.getArguments();
    }
}
