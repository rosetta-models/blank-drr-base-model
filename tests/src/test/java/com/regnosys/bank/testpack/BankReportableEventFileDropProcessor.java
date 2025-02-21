package com.regnosys.bank.testpack;

import cdm.base.staticdata.asset.common.ProductBase;
import cdm.base.staticdata.asset.common.ProductTaxonomy;
import cdm.base.staticdata.asset.common.TaxonomySourceEnum;
import cdm.event.common.Trade;
import cdm.event.common.TradeState;
import cdm.product.template.Product;
import cdm.product.template.TradableProduct;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.CaseFormat;
import com.regnosys.rosetta.common.serialisation.RosettaObjectMapper;
import drr.regulation.common.ReportableEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class BankReportableEventFileDropProcessor {

    public static final ObjectMapper OBJECT_MAPPER = RosettaObjectMapper.getNewRosettaObjectMapper();
    private static final Logger LOGGER = LoggerFactory.getLogger(BankReportableEventFileDropProcessor.class);

    void runFileProcessor(String groupName, Path fileDropPath, Path enrichInputPath) throws IOException {
        List<Path> jsonFilePathList = Files.walk(fileDropPath.resolve(groupName))
                .filter(f -> f.getFileName().toString().endsWith(".json"))
                .collect(Collectors.toList());

        for (Path filePath : jsonFilePathList) {
            processSample(groupName, filePath, enrichInputPath);
        }
    }

    private void processSample(String groupName, Path inputFile, Path outputDirectory) throws IOException {
        ReportableEvent reportableEvent = OBJECT_MAPPER.readValue(inputFile.toUri().toURL(), ReportableEvent.class);
        String fileName = inputFile.getFileName().toString();
        String productQualifier = getProductQualifier(reportableEvent);
        String testPackName = testPackName(fileName, productQualifier);
        Path testPackPath = Files.createDirectories(outputDirectory.resolve(groupName).resolve(testPackName));
        Path newFile = testPackPath.resolve(fileName);
        OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValue(newFile.toFile(), reportableEvent);
        LOGGER.info("File written: {} to {}", fileName, testPackPath);
    }

    private String getProductQualifier(ReportableEvent reportableEvent) {
        return Optional.of(reportableEvent)
                .map(ReportableEvent::getReportableTrade)
                .map(TradeState::getTrade)
                .map(Trade::getTradableProduct)
                .map(TradableProduct::getProduct)
                .map(Product::getContractualProduct)
                .map(ProductBase::getProductTaxonomy)
                .stream().flatMap(Collection::stream)
                .filter(x -> x.getSource() == TaxonomySourceEnum.ISDA)
                .map(ProductTaxonomy::getProductQualifier)
                .findFirst().orElse("NotQualified");
    }

    private String testPackName(String filename, String productQualifier) {
        String productQualifierLowerUnderscore = CaseFormat.UPPER_CAMEL.converterTo(CaseFormat.LOWER_UNDERSCORE).convert(productQualifier);
        String productQualifierFormatted = productQualifierLowerUnderscore.replaceAll("[-._/ ]+", "-");
        Matcher matcher = Pattern.compile("^BATCH_([0-9]*)_.*$").matcher(filename);
        if (matcher.find()) {
            return "bank-batch-" + matcher.group(1) + "-" + productQualifierFormatted;
        }
        return "bank-" + productQualifierFormatted;
    }
}
