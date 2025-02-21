package com.regnosys.bank.report.mas;

import com.regnosys.bank.report.ReportTestRuntimeModule;
import com.regnosys.rosetta.common.transform.TestPackModel;
import com.regnosys.testing.transform.TransformTestExtension;
import drr.regulation.mas.rewrite.trade.reports.MASTradeReportFunction;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static com.regnosys.rosetta.common.transform.TestPackUtils.REPORT_CONFIG_PATH;

public class MASTradeBankReportTest {


    @RegisterExtension
    static TransformTestExtension<MASTradeReportFunction> testExtension =
            new TransformTestExtension<>(
                    new ReportTestRuntimeModule(),
                    REPORT_CONFIG_PATH,
                    MASTradeReportFunction.class);

    @ParameterizedTest(name = "{0}")
    @MethodSource("inputFiles")
    void runReport(String testName,
                   String testPackId,
                   TestPackModel.SampleModel sampleModel,
                   MASTradeReportFunction func) {
        testExtension.runTransformAndAssert(testPackId, sampleModel, func::evaluate);
    }

    @SuppressWarnings("unused")//used by the junit parameterized test
    private static Stream<Arguments> inputFiles() {
        return testExtension.getArguments();
                //TODO: we should not be doing this. This is to fileter out the test packs that we want to run
//                .filter(testPack -> testPack.get()[1].toString().startsWith("test-pack-report-mas-trade-trade-bank"));
    }
}
