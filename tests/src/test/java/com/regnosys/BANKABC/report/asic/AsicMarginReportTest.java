package com.regnosys.BANKABC.report.asic;

import com.regnosys.BANKABC.report.ReportTestRuntimeModule;
import com.regnosys.rosetta.common.transform.TestPackModel;
import com.regnosys.testing.transform.TransformTestExtension;
import drr.regulation.asic.rewrite.margin.reports.ASICMarginReportFunction;
import drr.regulation.esma.emir.refit.margin.reports.ESMAEMIRMarginReportFunction;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static com.regnosys.rosetta.common.transform.TestPackUtils.REPORT_CONFIG_PATH;

public class AsicMarginReportTest {

    @RegisterExtension
    static TransformTestExtension<ASICMarginReportFunction> testExtension =
            new TransformTestExtension<>(
//                    "pipeline-report-BANKABC-asic-margin",
                    new ReportTestRuntimeModule(),
                    REPORT_CONFIG_PATH,
                    ASICMarginReportFunction.class);

    @ParameterizedTest(name = "{0}")
    @MethodSource("inputFiles")
    void runReport(String testName,
                   String testPackId,
                   TestPackModel.SampleModel sampleModel,
                   ASICMarginReportFunction func) {
        testExtension.runTransformAndAssert(testPackId, sampleModel, func::evaluate);
    }

    @SuppressWarnings("unused")//used by the junit parameterized test
    private static Stream<Arguments> inputFiles() {
        return testExtension.getArguments();
    }
}
