package com.regnosys.BANKABC.testpack;

import BANKABC.enrichment.functions.Enrich_ReportableEventToTransactionReportInstruction;
import BANKABC.enrichment.functions.Enrich_ReportableValuationToValuationReportInstruction;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.regnosys.BANKABC.report.ReportTestRuntimeModule;
import com.regnosys.rosetta.common.transform.TransformType;
import com.regnosys.testing.pipeline.PipelineConfigWriter;
import com.regnosys.testing.pipeline.PipelineTreeConfig;
import drr.projection.iso20022.asic.rewrite.trade.functions.Project_ASICTradeReportToIso20022;
import drr.projection.iso20022.asic.rewrite.valuation.functions.Project_ASICValuationReportToIso20022;
import drr.projection.iso20022.esma.emir.refit.trade.functions.Project_EsmaEmirTradeReportToIso20022;
import drr.projection.iso20022.jfsa.rewrite.trade.functions.Project_JFSARewriteTradeReportToIso20022;
import drr.projection.iso20022.mas.rewrite.trade.functions.Project_MASTradeReportToIso20022;
import drr.projection.iso20022.mas.rewrite.valuation.functions.Project_MASValuationReportToIso20022;
import drr.regulation.asic.rewrite.trade.reports.ASICTradeReportFunction;
import drr.regulation.asic.rewrite.valuation.reports.ASICValuationReportFunction;
import drr.regulation.esma.emir.refit.trade.reports.ESMAEMIRTradeReportFunction;
import drr.regulation.fca.ukemir.refit.trade.reports.FCAUKEMIRTradeReportFunction;
import drr.regulation.jfsa.rewrite.trade.reports.JFSATradeReportFunction;
import drr.regulation.mas.rewrite.trade.reports.MASTradeReportFunction;

import drr.regulation.mas.rewrite.valuation.reports.MASValuationReportFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static com.regnosys.testing.pipeline.PipelineFilter.startsWith;
import static iso20022.Iso20022ModelConfig.TYPE_TO_SCHEMA_MAP;
import static iso20022.Iso20022ModelConfig.TYPE_TO_XML_CONFIG_MAP;

public class BANKABCTestPackCreator {

    private static final Logger LOGGER = LoggerFactory.getLogger(BANKABCTestPackCreator.class);

    public static void main(String[] args) {
        try {
            Injector injector = new ReportTestRuntimeModule.InjectorProvider().getInjector();
            BANKABCTestPackCreator creator = injector.getInstance(BANKABCTestPackCreator.class);
            creator.run("PLACEHOLDER");
            System.exit(0);
        } catch (Exception e) {
            LOGGER.error("Error executing {}.main()", BANKABCTestPackCreator.class.getName(), e);
            System.exit(1);
        }
    }

    @Inject
    private PipelineConfigWriter pipelineConfigWriter;

    void run(String prefix) throws IOException {
        pipelineConfigWriter.writePipelinesAndTestPacks(createReportableEventTreeConfig(prefix).withTestPackIdFilter(startsWith("trade")));
        pipelineConfigWriter.writePipelinesAndTestPacks(createValutionReportTreeConfig(prefix).withTestPackIdFilter(startsWith("valuation")));
    }

    private PipelineTreeConfig createReportableEventTreeConfig(String prefix) {
        return new PipelineTreeConfig(prefix)

                .starting(TransformType.ENRICH, Enrich_ReportableEventToTransactionReportInstruction.class)

                .add(Enrich_ReportableEventToTransactionReportInstruction.class, TransformType.REPORT, ASICTradeReportFunction.class)
                .add(ASICTradeReportFunction.class, TransformType.PROJECTION, Project_ASICTradeReportToIso20022.class)

                .add(Enrich_ReportableEventToTransactionReportInstruction.class, TransformType.REPORT, ESMAEMIRTradeReportFunction.class)
                .add(ESMAEMIRTradeReportFunction.class, TransformType.PROJECTION, Project_EsmaEmirTradeReportToIso20022.class)

                .add(Enrich_ReportableEventToTransactionReportInstruction.class, TransformType.REPORT, FCAUKEMIRTradeReportFunction.class)
                .add(FCAUKEMIRTradeReportFunction.class, TransformType.PROJECTION, Project_JFSARewriteTradeReportToIso20022.class)

                .add(Enrich_ReportableEventToTransactionReportInstruction.class, TransformType.REPORT, JFSATradeReportFunction.class)
                .add(JFSATradeReportFunction.class, TransformType.PROJECTION, Project_JFSARewriteTradeReportToIso20022.class)

                .add(Enrich_ReportableEventToTransactionReportInstruction.class, TransformType.REPORT, MASTradeReportFunction.class)
                .add(MASTradeReportFunction.class, TransformType.PROJECTION, Project_MASTradeReportToIso20022.class)

                .withXmlConfigMap(TYPE_TO_XML_CONFIG_MAP)
                .withXmlSchemaMap(TYPE_TO_SCHEMA_MAP);
    }

    private PipelineTreeConfig createValutionReportTreeConfig(String prefix) {
        return new PipelineTreeConfig(prefix)

                .starting(TransformType.ENRICH, Enrich_ReportableValuationToValuationReportInstruction.class)
                .add(Enrich_ReportableValuationToValuationReportInstruction.class, TransformType.REPORT, ASICValuationReportFunction.class)
                .add(ASICValuationReportFunction.class, TransformType.PROJECTION, Project_ASICValuationReportToIso20022.class)

                .add(Enrich_ReportableValuationToValuationReportInstruction.class, TransformType.REPORT, MASValuationReportFunction.class)
                .add(MASValuationReportFunction.class, TransformType.PROJECTION, Project_MASValuationReportToIso20022.class)

                .withXmlConfigMap(TYPE_TO_XML_CONFIG_MAP)
                .withXmlSchemaMap(TYPE_TO_SCHEMA_MAP);
    }

    private PipelineTreeConfig createMarginReportTreeConfig(String prefix) {
        return new PipelineTreeConfig(prefix);

//                .starting()
    }

}