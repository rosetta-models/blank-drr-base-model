package com.regnosys.BANKABC.projection.iso20022.mas;

import com.google.common.io.Resources;
import com.regnosys.BANKABC.report.ReportTestRuntimeModule;
import com.regnosys.rosetta.common.transform.TestPackModel;
import com.regnosys.testing.transform.TransformTestExtension;
import drr.projection.iso20022.mas.rewrite.valuation.functions.Project_MASValuationReportToIso20022;
import iso20022.Auth030MasModelConfig;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static com.regnosys.rosetta.common.transform.TestPackUtils.PROJECTION_CONFIG_PATH_WITHOUT_ISO20022;
@Disabled("Enable when the test data is available")
public class MASValuationIso20022ReportTest {
    @RegisterExtension
    static TransformTestExtension<Project_MASValuationReportToIso20022> testExtension =
            new TransformTestExtension<>(
                    "bankabc",
                    new ReportTestRuntimeModule(),
                    PROJECTION_CONFIG_PATH_WITHOUT_ISO20022,
                    Project_MASValuationReportToIso20022.class)
                    .withSchemaValidation(Resources.getResource(Auth030MasModelConfig.SCHEMA_PATH));

    @ParameterizedTest(name = "{0}")
    @MethodSource("inputFiles")
    void runReport(String testName,
                   String testPackId,
                   TestPackModel.SampleModel sampleModel) {
        testExtension.runTransformAndAssert(testPackId, sampleModel);
    }

    private static Stream<Arguments> inputFiles() {
        return testExtension.getArguments();
    }
}
