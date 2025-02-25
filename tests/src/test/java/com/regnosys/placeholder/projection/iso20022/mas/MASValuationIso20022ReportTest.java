package com.regnosys.placeholder.projection.iso20022.mas;

import com.google.common.io.Resources;
import com.regnosys.placeholder.report.ReportTestRuntimeModule;
import com.regnosys.rosetta.common.transform.TestPackModel;
import com.regnosys.testing.transform.TransformTestExtension;
import drr.projection.iso20022.mas.rewrite.valuation.functions.Project_MASValuationReportToIso20022;
import iso20022.Auth030MasModelConfig;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static com.regnosys.rosetta.common.transform.TestPackUtils.PROJECTION_CONFIG_PATH_WITHOUT_ISO20022;

public class MASValuationIso20022ReportTest {

    @RegisterExtension
    static TransformTestExtension<Project_MASValuationReportToIso20022> testExtension =
            new TransformTestExtension<>(
                    new ReportTestRuntimeModule(),
                    PROJECTION_CONFIG_PATH_WITHOUT_ISO20022,
                    Project_MASValuationReportToIso20022.class)
                    .withSchemaValidation(Resources.getResource(Auth030MasModelConfig.SCHEMA_PATH));

    @ParameterizedTest(name = "{0}")
    @MethodSource("inputFiles")
    void runReport(String testName,
                   String testPackId,
                   TestPackModel.SampleModel sampleModel,
                   Project_MASValuationReportToIso20022 func) {
        testExtension.runTransformAndAssert(testPackId, sampleModel, func::evaluate);
    }

    private static Stream<Arguments> inputFiles() {
        return testExtension.getArguments();
        //TODO: we should not be doing this. This is to fileter out the test packs that we want to run
//                .filter(testPack -> testPack.get()[1].toString().startsWith("test-pack-projection-mas-valuation-report-to-iso20022-mas-valuation"));
    }
}
