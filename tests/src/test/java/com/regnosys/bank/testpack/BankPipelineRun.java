package com.regnosys.bank.testpack;

import com.google.inject.Injector;
import com.regnosys.bank.report.ReportTestRuntimeModule;
import com.regnosys.ingest.test.framework.ingestor.ExpectationDefaults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

import static com.regnosys.rosetta.common.transform.TransformType.*;


public class BankPipelineRun {
    private static final Logger LOGGER = LoggerFactory.getLogger(BankPipelineRun.class);

    private static final Path BANK_FILE_DROP = Path.of("bank-file-drop");

    public static final String TRADE_GROUP = "trade";
    public static final String ASIC_VALUATION_GROUP = "asic-valuation";
    public static final String MAS_VALUATION_GROUP = "mas-valuation";

    public static void main(String[] args) {
        ExpectationDefaults expectationDefaults = new ExpectationDefaults();
        if (expectationDefaults.testWriteBasePath().isEmpty()) {
            LOGGER.info("TEST_WRITE_BASE_PATH not set");
            return;
        }
        Path writePath = expectationDefaults.testWriteBasePath().get();

        try {
            Path jpmFileDrop = writePath.resolve(BANK_FILE_DROP);
            Path enrichPath = writePath.resolve(ENRICH.getResourcePath());
            Path reportPath = writePath.resolve(REPORT.getResourcePath());
            Path projectionPath = writePath.resolve(PROJECTION.getResourcePath());

            // 1. Delete pre-report/report/projection...
            BankCleanAndPrep.emptySampleAndTestPackFolders(enrichPath, reportPath, projectionPath);

            Path enrichInputPath = enrichPath.resolve("input");

            //DrrRuntimeModule
            Injector injector = new ReportTestRuntimeModule.InjectorProvider().getInjector();

            // 2 Runs the FileDropProcessors. jpm-file-drop/trade --> enrich/input
            BankReportableEventFileDropProcessor bankReportableEventFileDropProcessor = injector.getInstance(BankReportableEventFileDropProcessor.class);
            bankReportableEventFileDropProcessor.runFileProcessor(TRADE_GROUP, jpmFileDrop, enrichInputPath);
            BankValuationFileDropProcessor bankValuationFileDropProcessor = injector.getInstance(BankValuationFileDropProcessor.class);
            bankValuationFileDropProcessor.runFileProcessor(ASIC_VALUATION_GROUP, jpmFileDrop, enrichInputPath);
            bankValuationFileDropProcessor.runFileProcessor(MAS_VALUATION_GROUP, jpmFileDrop, enrichInputPath);

            // 3. Run the BankTestPackCreator --> started from regulatory-reporting/input --> everywhere else. Standard.
            BankTestPackCreator creator = injector.getInstance(BankTestPackCreator.class);
            creator.run(TRADE_GROUP, ASIC_VALUATION_GROUP, MAS_VALUATION_GROUP);

            System.exit(0);
        } catch (Exception e) {
            LOGGER.error("Error executing {}.main()", BankPipelineRun.class.getName(), e);
            System.exit(1);
        }
    }

}
