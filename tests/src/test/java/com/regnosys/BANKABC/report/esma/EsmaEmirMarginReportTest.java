package com.regnosys.BANKABC.report.esma;

import com.regnosys.BANKABC.report.ReportTestRuntimeModule;
import com.regnosys.rosetta.common.transform.TestPackModel;
import com.regnosys.testing.transform.TransformTestExtension;
import drr.regulation.esma.emir.refit.margin.reports.ESMAEMIRMarginReportFunction;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static com.regnosys.rosetta.common.transform.TestPackUtils.REPORT_CONFIG_PATH;

@Disabled("Enable when the test data is available")
public class EsmaEmirMarginReportTest {

    @RegisterExtension
    static TransformTestExtension<ESMAEMIRMarginReportFunction> testExtension =
            new TransformTestExtension<>(
                    "bankabc",
                    new ReportTestRuntimeModule(),
                    REPORT_CONFIG_PATH,
                    ESMAEMIRMarginReportFunction.class);

    @ParameterizedTest(name = "{0}")
    @MethodSource("inputFiles")
    void runReport(String testName,
                   String testPackId,
                   TestPackModel.SampleModel sampleModel) {
        testExtension.runTransformAndAssert(testPackId, sampleModel);
    }

    @SuppressWarnings("unused")//used by the junit parameterized test
    private static Stream<Arguments> inputFiles() {
        return testExtension.getArguments();
    }
}
