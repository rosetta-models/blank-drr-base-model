package com.regnosys.BANKABC.testpack;

import com.google.inject.Injector;
import com.regnosys.BANKABC.report.ReportTestRuntimeModule;
import com.regnosys.ingest.test.framework.ingestor.ExpectationDefaults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

import static com.regnosys.rosetta.common.transform.TransformType.*;


public class BANKABCPipelineRun {
    private static final Logger LOGGER = LoggerFactory.getLogger(BANKABCPipelineRun.class);

    private static final Path BANKABC_FILE_DROP = Path.of("BANKABC-file-drop");

    public static final String PREFIX = "PLACEHOLDER";

    public static void main(String[] args) {
        ExpectationDefaults expectationDefaults = new ExpectationDefaults();
        if (expectationDefaults.testWriteBasePath().isEmpty()) {
            LOGGER.info("TEST_WRITE_BASE_PATH not set");
            return;
        }
        Path writePath = expectationDefaults.testWriteBasePath().get();

        try {
            Path BANKABCFileDrop = writePath.resolve(BANKABC_FILE_DROP);
            Path enrichPath = writePath.resolve(ENRICH.getResourcePath());
            Path reportPath = writePath.resolve(REPORT.getResourcePath());
            Path projectionPath = writePath.resolve(PROJECTION.getResourcePath());

            // 1. Delete pre-report/report/projection...
            BANKABCCleanAndPrep.emptySampleAndTestPackFolders(enrichPath, reportPath, projectionPath);

            Path enrichInputPath = enrichPath.resolve("input");

            //DrrRuntimeModule
            Injector injector = new ReportTestRuntimeModule.InjectorProvider().getInjector();

            // 2 Runs the FileDropProcessors. BANKABC-file-drop/trade --> enrich/input
            BANKABCReportableEventFileDropProcessor BANKABCReportableEventFileDropProcessor = injector.getInstance(BANKABCReportableEventFileDropProcessor.class);
            BANKABCReportableEventFileDropProcessor.runFileProcessor(PREFIX, BANKABCFileDrop, enrichInputPath);
            BANKABCValuationFileDropProcessor BANKABCValuationFileDropProcessor = injector.getInstance(BANKABCValuationFileDropProcessor.class);
            BANKABCValuationFileDropProcessor.runFileProcessor(PREFIX, BANKABCFileDrop, enrichInputPath);
            BANKABCValuationFileDropProcessor.runFileProcessor(PREFIX, BANKABCFileDrop, enrichInputPath);

            // 3. Run the TemplateTestPackCreator --> started from regulatory-reporting/input --> everywhere else. Standard.
            BANKABCTestPackCreator creator = injector.getInstance(BANKABCTestPackCreator.class);
            creator.run(PREFIX);

            System.exit(0);
        } catch (Exception e) {
            LOGGER.error("Error executing {}.main()", BANKABCPipelineRun.class.getName(), e);
            System.exit(1);
        }
    }

}
