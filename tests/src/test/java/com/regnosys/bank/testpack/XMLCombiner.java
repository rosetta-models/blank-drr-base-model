package com.regnosys.bank.testpack;

import com.regnosys.ingest.test.framework.ingestor.ExpectationDefaults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;

import static com.regnosys.bank.testpack.FileUtils.combineXMLFilesFromFolder;
import static com.regnosys.rosetta.common.transform.TransformType.PROJECTION;

public class XMLCombiner {

    private static final Logger LOGGER = LoggerFactory.getLogger(XMLCombiner.class);


    public static void main(String[] args) {
        ExpectationDefaults expectationDefaults = new ExpectationDefaults();
        if (expectationDefaults.testWriteBasePath().isEmpty()) {
            LOGGER.info("TEST_WRITE_BASE_PATH not set");
            return;
        }
        Path writePath = expectationDefaults.testWriteBasePath().get();
        Path projectionPath = writePath.resolve(PROJECTION.getResourcePath());
        Path submissionsPath = BankStatics.XML_SUBMISSIONS_PATH;

        String asicInputPath = projectionPath + "/output/asic-trade-report-to-iso20022";
        String asicSubmissionFilePath = submissionsPath + "/ASICDTCCSubmission.xml";

        String masInputPath = projectionPath + "/output/mas-trade-report-to-iso20022";
        String masSubmissionFilePath = submissionsPath +"/MASDTCCSubmission.xml";
        try {
            combineXMLFilesFromFolder(asicInputPath, asicSubmissionFilePath);
            combineXMLFilesFromFolder(masInputPath, masSubmissionFilePath);
            LOGGER.info("XML files combined successfully into {}", asicSubmissionFilePath);
            LOGGER.info("XML files combined successfully into {}", masSubmissionFilePath);
        } catch (IOException e) {
            LOGGER.error("An error occurred while combining XML files. {} ", e.getMessage(), e);
        }
    }
}
