package com.regnosys.drr.analytics.iso20022;

import com.google.common.io.Resources;
import com.google.inject.Injector;
import com.regnosys.drr.analytics.DefaultValidationSummaryProcessor;
import com.regnosys.drr.analytics.TransformData;
import com.regnosys.drr.analytics.ValidationData;
import com.regnosys.drr.analytics.ValidationSummaryProcessor;
import com.regnosys.rosetta.common.validation.RosettaTypeValidator;
import drr.projection.iso20022.asic.rewrite.margin.functions.Project_ASICMarginReportToIso20022;
import drr.projection.iso20022.asic.rewrite.trade.functions.Project_ASICTradeReportToIso20022;
import drr.projection.iso20022.asic.rewrite.valuation.functions.Project_ASICValuationReportToIso20022;
import drr.projection.iso20022.esma.emir.refit.margin.functions.Project_EsmaEmirMarginReportToIso20022;
import drr.projection.iso20022.esma.emir.refit.trade.functions.Project_EsmaEmirTradeReportToIso20022;
import drr.projection.iso20022.fca.ukemir.refit.margin.functions.Project_FcaUkEmirMarginReportToIso20022;
import drr.projection.iso20022.fca.ukemir.refit.trade.functions.Project_FcaUkEmirTradeReportToIso20022;
import drr.projection.iso20022.jfsa.rewrite.margin.functions.Project_JFSARewriteMarginReportToIso20022;
import drr.projection.iso20022.jfsa.rewrite.trade.functions.Project_JFSARewriteTradeReportToIso20022;
import drr.projection.iso20022.mas.rewrite.margin.functions.Project_MASMarginReportToIso20022;
import drr.projection.iso20022.mas.rewrite.trade.functions.Project_MASTradeReportToIso20022;
import drr.projection.iso20022.mas.rewrite.valuation.functions.Project_MASValuationReportToIso20022;
import drr.regulation.asic.rewrite.margin.ASICMarginReport;
import drr.regulation.asic.rewrite.trade.ASICTransactionReport;
import drr.regulation.asic.rewrite.valuation.ASICValuationReport;
import drr.regulation.esma.emir.refit.margin.ESMAEMIRMarginReport;
import drr.regulation.esma.emir.refit.trade.ESMAEMIRTransactionReport;
import drr.regulation.fca.ukemir.refit.margin.FCAUKEMIRMarginReport;
import drr.regulation.fca.ukemir.refit.trade.FCAUKEMIRTransactionReport;
import drr.regulation.jfsa.rewrite.margin.JFSAMarginReport;
import drr.regulation.jfsa.rewrite.trade.JFSATransactionReport;
import drr.regulation.mas.rewrite.margin.MASMarginReport;
import drr.regulation.mas.rewrite.trade.MASTransactionReport;
import drr.regulation.mas.rewrite.valuation.MASValuationReport;
import iso20022.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Iso20022ValidationSummaryCreator {

    private static final Logger LOGGER = LoggerFactory.getLogger(Iso20022ValidationSummaryCreator.class);

    public static final TransformData ASIC_TRADE = new TransformData("asic-trade", Project_ASICTradeReportToIso20022.class, ASICTransactionReport.class, Auth030AsicModelConfig.XML_CONFIG_PATH, Auth030AsicModelConfig.SCHEMA_PATH);
    public static final TransformData ASIC_MARGIN = new TransformData("asic-margin", Project_ASICMarginReportToIso20022.class, ASICMarginReport.class, Auth108AsicModelConfig.XML_CONFIG_PATH, Auth108AsicModelConfig.SCHEMA_PATH);
    public static final TransformData EMIR_TRADE = new TransformData("esma-emir-trade", Project_EsmaEmirTradeReportToIso20022.class, ESMAEMIRTransactionReport.class, Auth030EsmaModelConfig.XML_CONFIG_PATH, Auth030EsmaModelConfig.SCHEMA_PATH);
    public static final TransformData EMIR_MARGIN = new TransformData("esma-emir-margin", Project_EsmaEmirMarginReportToIso20022.class, ESMAEMIRMarginReport.class, Auth108EsmaModelConfig.XML_CONFIG_PATH, Auth108EsmaModelConfig.SCHEMA_PATH);
    public static final TransformData FCA_UKEMIR_TRADE = new TransformData("fca-uk-emir-trade", Project_FcaUkEmirTradeReportToIso20022.class, FCAUKEMIRTransactionReport.class, Auth030FcaModelConfig.XML_CONFIG_PATH, Auth030FcaModelConfig.SCHEMA_PATH);
    public static final TransformData FCA_UKEMIR_MARGIN = new TransformData("fca-uk-emir-margin", Project_FcaUkEmirMarginReportToIso20022.class, FCAUKEMIRMarginReport.class, Auth108FcaModelConfig.XML_CONFIG_PATH, Auth108FcaModelConfig.SCHEMA_PATH);
    public static final TransformData JFSA_TRADE = new TransformData("jfsa-rewrite-trade", Project_JFSARewriteTradeReportToIso20022.class, JFSATransactionReport.class, Auth030JfsaModelConfig.XML_CONFIG_PATH, Auth030JfsaModelConfig.SCHEMA_PATH);
    public static final TransformData JFSA_MARGIN = new TransformData("jfsa-rewrite-margin", Project_JFSARewriteMarginReportToIso20022.class, JFSAMarginReport.class, Auth108JfsaModelConfig.XML_CONFIG_PATH, Auth108JfsaModelConfig.SCHEMA_PATH);
    public static final TransformData MAS_TRADE = new TransformData("mas-trade", Project_MASTradeReportToIso20022.class, MASTransactionReport.class, Auth030MasModelConfig.XML_CONFIG_PATH, Auth030MasModelConfig.SCHEMA_PATH);
    public static final TransformData MAS_MARGIN = new TransformData("mas-margin", Project_MASMarginReportToIso20022.class, MASMarginReport.class, Auth108MasModelConfig.XML_CONFIG_PATH, Auth108MasModelConfig.SCHEMA_PATH);
    public static final TransformData ASIC_VALUATION = new TransformData("asic-valuation", Project_ASICValuationReportToIso20022.class, ASICValuationReport.class, Auth030AsicModelConfig.XML_CONFIG_PATH, Auth030AsicModelConfig.SCHEMA_PATH);
    public static final TransformData MAS_VALUATION = new TransformData("mas-valuation", Project_MASValuationReportToIso20022.class, MASValuationReport.class, Auth030MasModelConfig.XML_CONFIG_PATH, Auth030MasModelConfig.SCHEMA_PATH);


    @Inject
    Injector injector;
    
    @Inject
    RosettaTypeValidator validator;
    
    public void generateValidationSummaryAndWriteCsv(List<TransformData> transformDataList, Path configPath, Path analyticsPath) throws IOException {
        List<ValidationData> validationDataList = new ArrayList<>();

        // Process each report type
        for (TransformData transformData : transformDataList) {
            URL xmlConfigUrl = Resources.getResource(transformData.getConfigXmlPath());
            URL xmlSchemaUrl = Resources.getResource(transformData.getSchemaPath());
            ValidationSummaryProcessor validationSummaryProcessor = new DefaultValidationSummaryProcessor<>(injector, validator, configPath, xmlConfigUrl, xmlSchemaUrl);
            validationDataList.addAll(validationSummaryProcessor.processValidation(transformData));
        }

        // Write results
        new Iso20022ValidationSummaryWriter(analyticsPath).writeCsv(validationDataList);
        new Iso20022ValidationFileSummaryWriter(analyticsPath).writeCsv(validationDataList);
    }
}
