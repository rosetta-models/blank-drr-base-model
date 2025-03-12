package com.regnosys.BANKABC.report.esma;

import com.regnosys.BANKABC.report.ReportTestRuntimeModule;
import com.regnosys.rosetta.common.transform.TestPackModel;
import com.regnosys.testing.transform.TransformTestExtension;
import drr.regulation.esma.emir.refit.trade.reports.ESMAEMIRTradeReportFunction;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static com.regnosys.rosetta.common.transform.TestPackUtils.REPORT_CONFIG_PATH;
@Disabled("Enable when the test data is available")
public class EsmaEmirTradeReportTest {
    @RegisterExtension
    static TransformTestExtension<ESMAEMIRTradeReportFunction> testExtension =
            new TransformTestExtension<>(
                    "pipeline-report-BANKABC-esma-emir-trade",
                    new ReportTestRuntimeModule(),
                    REPORT_CONFIG_PATH,
                    ESMAEMIRTradeReportFunction.class);

    @ParameterizedTest(name = "{0}")
    @MethodSource("inputFiles")
    void runReport(String testName,
                   String testPackId,
                   TestPackModel.SampleModel sampleModel,
                   ESMAEMIRTradeReportFunction func) {
        testExtension.runTransformAndAssert(testPackId, sampleModel, func::evaluate);
    }

    @SuppressWarnings("unused")//used by the junit parameterized test
    private static Stream<Arguments> inputFiles() {
        return testExtension.getArguments();
    }
}
