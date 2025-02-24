package com.regnosys.template.testpack;

import com.google.inject.Injector;
import com.regnosys.template.report.ReportTestRuntimeModule;
import com.regnosys.ingest.test.framework.ingestor.ExpectationDefaults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

import static com.regnosys.rosetta.common.transform.TransformType.*;


public class TemplatePipelineRun {
    private static final Logger LOGGER = LoggerFactory.getLogger(TemplatePipelineRun.class);

    private static final Path BANK_FILE_DROP = Path.of("template-file-drop");

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
            TemplateCleanAndPrep.emptySampleAndTestPackFolders(enrichPath, reportPath, projectionPath);

            Path enrichInputPath = enrichPath.resolve("input");

            //DrrRuntimeModule
            Injector injector = new ReportTestRuntimeModule.InjectorProvider().getInjector();

            // 2 Runs the FileDropProcessors. jpm-file-drop/trade --> enrich/input
            TemplateReportableEventFileDropProcessor templateReportableEventFileDropProcessor = injector.getInstance(TemplateReportableEventFileDropProcessor.class);
            templateReportableEventFileDropProcessor.runFileProcessor(TRADE_GROUP, jpmFileDrop, enrichInputPath);
            TemplateValuationFileDropProcessor templateValuationFileDropProcessor = injector.getInstance(TemplateValuationFileDropProcessor.class);
            templateValuationFileDropProcessor.runFileProcessor(ASIC_VALUATION_GROUP, jpmFileDrop, enrichInputPath);
            templateValuationFileDropProcessor.runFileProcessor(MAS_VALUATION_GROUP, jpmFileDrop, enrichInputPath);

            // 3. Run the TemplateTestPackCreator --> started from regulatory-reporting/input --> everywhere else. Standard.
            TemplateTestPackCreator creator = injector.getInstance(TemplateTestPackCreator.class);
            creator.run(TRADE_GROUP, ASIC_VALUATION_GROUP, MAS_VALUATION_GROUP);

            System.exit(0);
        } catch (Exception e) {
            LOGGER.error("Error executing {}.main()", TemplatePipelineRun.class.getName(), e);
            System.exit(1);
        }
    }

}
