package com.regnosys.BANKABC.report.mas;

import com.regnosys.BANKABC.report.ReportTestRuntimeModule;
import com.regnosys.rosetta.common.transform.TestPackModel;
import com.regnosys.testing.transform.TransformTestExtension;
import drr.regulation.mas.rewrite.valuation.reports.MASValuationReportFunction;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static com.regnosys.rosetta.common.transform.TestPackUtils.REPORT_CONFIG_PATH;

@Disabled("Enable when the test data is available")

public class MASValuationReportTest {
    @RegisterExtension
    static TransformTestExtension<MASValuationReportFunction> testExtension =
            new TransformTestExtension<>(
                    "pipeline-report-BANKABC-mas-valuation",
                    new ReportTestRuntimeModule(),
                    REPORT_CONFIG_PATH,
                    MASValuationReportFunction.class);

    @ParameterizedTest(name = "{0}")
    @MethodSource("inputFiles")
    void runReport(String testName,
                   String testPackId,
                   TestPackModel.SampleModel sampleModel,
                   MASValuationReportFunction func) {
        testExtension.runTransformAndAssert(testPackId, sampleModel, func::evaluate);
    }

    @SuppressWarnings("unused")//used by the junit parameterized test
    private static Stream<Arguments> inputFiles() {
        return testExtension.getArguments();
    }
}
