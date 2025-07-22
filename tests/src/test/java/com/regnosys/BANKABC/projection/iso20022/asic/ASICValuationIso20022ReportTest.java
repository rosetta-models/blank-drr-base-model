package com.regnosys.BANKABC.projection.iso20022.asic;

import com.google.common.io.Resources;
import com.regnosys.BANKABC.report.ReportTestRuntimeModule;
import com.regnosys.rosetta.common.transform.TestPackModel;
import com.regnosys.testing.transform.TransformTestExtension;
import drr.projection.iso20022.asic.rewrite.valuation.functions.Project_ASICValuationReportToIso20022;
import iso20022.Auth030AsicModelConfig;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static com.regnosys.rosetta.common.transform.TestPackUtils.PROJECTION_CONFIG_PATH_WITHOUT_ISO20022;

@Disabled("Enable when the test data is available")
public class ASICValuationIso20022ReportTest {

    @RegisterExtension
    static TransformTestExtension<Project_ASICValuationReportToIso20022> testExtension =
            new TransformTestExtension<>(
//                    "bankabc",
                    new ReportTestRuntimeModule(),
                    PROJECTION_CONFIG_PATH_WITHOUT_ISO20022,
                    Project_ASICValuationReportToIso20022.class)
                    .withSchemaValidation(Resources.getResource(Auth030AsicModelConfig.SCHEMA_PATH));

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
