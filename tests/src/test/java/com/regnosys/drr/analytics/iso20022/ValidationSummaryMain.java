package com.regnosys.drr.analytics.iso20022;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.regnosys.bank.report.ReportTestRuntimeModule;
import com.regnosys.drr.analytics.TransformData;
import com.regnosys.rosetta.common.transform.TestPackUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static com.regnosys.drr.analytics.iso20022.Iso20022ValidationSummaryCreator.*;

//TODO: Change visibility of Iso20022ValidationSummaryCreator to public
public class ValidationSummaryMain {

    private static final Logger LOGGER = LoggerFactory.getLogger(ValidationSummaryMain.class);

    private static final Path ANALYTICS_PATH = TestPackUtils.PROJECTION_PATH.resolve("analytics");

    private static final List<TransformData> PROJECTIONS = List.of(ASIC_TRADE, MAS_TRADE, ASIC_VALUATION, MAS_VALUATION);

    public static void main(String[] args) {
        try {
            Injector injector = Guice.createInjector(new ReportTestRuntimeModule());
            Iso20022ValidationSummaryCreator creator = injector.getInstance(Iso20022ValidationSummaryCreator.class);
            creator.generateValidationSummaryAndWriteCsv(PROJECTIONS, TestPackUtils.PROJECTION_CONFIG_PATH_WITHOUT_ISO20022, ANALYTICS_PATH);
            flatFile();
            System.exit(0);
        } catch (Exception e) {
            LOGGER.error("Error executing {}.main()", ValidationSummaryMain.class.getName(), e);
            System.exit(1);
        }
    }

    private static void flatFile() throws IOException {

        Path input = Iso20022ValidationSummaryWriter.TEST_WRITE_BASE_PATH.get().resolve(ANALYTICS_PATH.resolve("validation-file-summary.csv"));
        Path output = Iso20022ValidationSummaryWriter.TEST_WRITE_BASE_PATH.get().resolve(ANALYTICS_PATH.resolve("validation-file-summary-flat.csv"));
        if (!Files.exists(output)) {
            Files.createFile(output);
        }
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(output.toFile())));
        List<String> rows = Files.readAllLines(input);

        for (String row : rows) {
            String validations = row.split(",")[9];
            if (validations.contains("|")) {
                String[] validationList = validations.split("\\|");
                for (String s : validationList) {
                    String validation = s.trim();
                    String[] split = row.split(",");
                    split[9] = validation;
                    writer.println(String.join(",", split));
                }
            } else {
                writer.println(row);
            }
        }
        writer.flush();
    }
}