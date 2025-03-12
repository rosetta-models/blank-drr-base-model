package com.regnosys.BANKABC.enrich;

import BANKABC.enrichment.functions.Enrich_ReportableEventToTransactionReportInstruction;
import com.regnosys.BANKABC.report.ReportTestRuntimeModule;
import com.regnosys.rosetta.common.transform.TestPackModel;
import com.regnosys.rosetta.common.transform.TransformType;
import com.regnosys.testing.transform.TransformTestExtension;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

public class AcmeBankEnrichTest {
    public static final Path ENRICH_CONFIG_PATH = Paths.get(TransformType.ENRICH.getResourcePath()).resolve("config");
    @RegisterExtension
    static TransformTestExtension<Enrich_ReportableEventToTransactionReportInstruction> testExtension =
            new TransformTestExtension<>(
                    "pipeline-enrich-acmebank-workflow-step-to-transaction-report-instruction",
                    new ReportTestRuntimeModule(),
                    ENRICH_CONFIG_PATH,
                    Enrich_ReportableEventToTransactionReportInstruction.class);

    @ParameterizedTest(name = "{0}")
    @MethodSource("inputFiles")
    void runReport(String testName,
                   String testPackId,
                   TestPackModel.SampleModel sampleModel,
                   Enrich_ReportableEventToTransactionReportInstruction func) {
        testExtension.runTransformAndAssert(testPackId, sampleModel, func::evaluate);
    }

    private static Stream<Arguments> inputFiles() {
        return testExtension.getArguments();
    }
}
