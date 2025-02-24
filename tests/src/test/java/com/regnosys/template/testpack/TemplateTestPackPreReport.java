package com.regnosys.template.testpack;

import cdm.base.staticdata.party.Party;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.regnosys.rosetta.common.serialisation.RosettaObjectMapper;
import drr.enrichment.common.trade.functions.Create_TransactionReportInstruction;
import drr.regulation.common.PartyInformation;
import drr.regulation.common.ReportingSide;
import drr.regulation.common.TransactionReportInstruction;
import drr.regulation.common.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class TemplateTestPackPreReport {

    public static final ObjectMapper OBJECT_MAPPER = RosettaObjectMapper.getNewRosettaObjectMapper();
    private static final Logger LOGGER = LoggerFactory.getLogger(TemplateTestPackPreReport.class);

    private final Create_TransactionReportInstruction createTransactionReportInstruction;

    @Inject
    public TemplateTestPackPreReport(Create_TransactionReportInstruction createTransactionReportInstruction) {
        this.createTransactionReportInstruction = createTransactionReportInstruction;
    }

    public List<String> preReport(Path writePath, Path preReportBasePath, Path reportInputBasePath) throws IOException {

        List<Path> jsonSampleFiles = Files.walk(preReportBasePath)
                .filter(f -> f.getFileName().toString().endsWith(".json"))
                .collect(Collectors.toList());

        List<String> preReportPaths = new ArrayList<>();
        for (Path preReportInputSamplePath : jsonSampleFiles) {
            TransactionReportInstruction original = OBJECT_MAPPER.readValue(preReportInputSamplePath.toUri().toURL(), TransactionReportInstruction.class);
            TransactionReportInstruction transactionReportInstruction = preReportSample(original);
            String preReported = OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(transactionReportInstruction);
            Path relativize = writePath.resolve(preReportBasePath).relativize(preReportInputSamplePath);
            Path reportInputSamplePath = reportInputBasePath.resolve(relativize);
            Path fullPath = writePath.resolve(reportInputSamplePath);
            Files.createDirectories(fullPath.getParent());
            Files.write(fullPath, preReported.getBytes(StandardCharsets.UTF_8));

            LOGGER.info("File written: {}", fullPath);

            preReportPaths.add(reportInputSamplePath.toString());
        }
        return preReportPaths;
    }
    
    private TransactionReportInstruction preReportSample(TransactionReportInstruction reportableEvent) {
        if (reportableEvent.getReportingSide() != null) {
            return reportableEvent;
        }
        List<? extends PartyInformation> list = Optional.of(reportableEvent)
                .map(ReportableEvent::getReportableInformation)
                .map(ReportableInformation::getPartyInformation)
                .stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        Party value = !list.isEmpty() ? list.get(0).getPartyReference().getValue() : null;
        Party cptyVal = list.size() >= 2 ? list.get(1).getPartyReference().getValue() : null;
        return createTransactionReportInstruction.evaluate(reportableEvent, ReportingSide.builder()
                .setReportingPartyValue(value)
                .setReportingCounterpartyValue(cptyVal)
                .setReportSubmittingPartyValue(value)
                .build());
    }
    
}
