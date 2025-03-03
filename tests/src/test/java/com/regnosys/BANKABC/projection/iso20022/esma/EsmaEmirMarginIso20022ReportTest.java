package com.regnosys.BANKABC.projection.iso20022.esma;

import com.google.common.io.Resources;
import com.regnosys.BANKABC.report.ReportTestRuntimeModule;
import com.regnosys.rosetta.common.transform.TestPackModel;
import com.regnosys.testing.transform.TransformTestExtension;
import drr.projection.iso20022.esma.emir.refit.margin.functions.Project_EsmaEmirMarginReportToIso20022;
import iso20022.Auth108EsmaModelConfig;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static com.regnosys.rosetta.common.transform.TestPackUtils.PROJECTION_CONFIG_PATH_WITHOUT_ISO20022;

public class EsmaEmirMarginIso20022ReportTest {
    @RegisterExtension
    static TransformTestExtension<Project_EsmaEmirMarginReportToIso20022> testExtension =
            new TransformTestExtension<>(
                    "pipeline-projection-BANKABC-esma-emir-margin-report-to-iso20022",
                    new ReportTestRuntimeModule(),
                    PROJECTION_CONFIG_PATH_WITHOUT_ISO20022,
                    Project_EsmaEmirMarginReportToIso20022.class)
                    .withSchemaValidation(Resources.getResource(Auth108EsmaModelConfig.SCHEMA_PATH));

    @ParameterizedTest(name = "{0}")
    @MethodSource("inputFiles")
    void runReport(String testName,
                   String testPackId,
                   TestPackModel.SampleModel sampleModel,
                   Project_EsmaEmirMarginReportToIso20022 func) {
        testExtension.runTransformAndAssert(testPackId, sampleModel, func::evaluate);
    }

    private static Stream<Arguments> inputFiles() {
        return testExtension.getArguments();
    }
}
