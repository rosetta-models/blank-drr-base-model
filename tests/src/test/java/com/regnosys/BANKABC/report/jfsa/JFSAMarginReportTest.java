package com.regnosys.BANKABC.report.jfsa;

import com.regnosys.BANKABC.report.ReportTestRuntimeModule;
import com.regnosys.rosetta.common.transform.TestPackModel;
import com.regnosys.testing.transform.TransformTestExtension;
import drr.regulation.jfsa.rewrite.margin.reports.JFSAMarginReportFunction;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static com.regnosys.rosetta.common.transform.TestPackUtils.REPORT_CONFIG_PATH;
@Disabled("Enable when the test data is available")
public class JFSAMarginReportTest {

    @RegisterExtension
    static TransformTestExtension<JFSAMarginReportFunction> testExtension =
            new TransformTestExtension<>(
                    "pipeline-report-BANKABC-jfsa-margin",
                    new ReportTestRuntimeModule(),
                    REPORT_CONFIG_PATH,
                    JFSAMarginReportFunction.class);

    @ParameterizedTest(name = "{0}")
    @MethodSource("inputFiles")
    void runReport(String testName,
                   String testPackId,
                   TestPackModel.SampleModel sampleModel,
                   JFSAMarginReportFunction func) {
        testExtension.runTransformAndAssert(testPackId, sampleModel, func::evaluate);
    }

    @SuppressWarnings("unused")//used by the junit parameterized test
    private static Stream<Arguments> inputFiles() {
        return testExtension.getArguments();
    }
}
