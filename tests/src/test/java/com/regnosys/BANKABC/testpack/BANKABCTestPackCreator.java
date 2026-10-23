package com.regnosys.BANKABC.testpack;

import BANKABC.enrich.functions.Enrich_ReportableEventToTransactionReportInstruction;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.regnosys.BANKABC.report.ReportTestRuntimeModule;
import com.regnosys.rosetta.common.transform.TransformType;
import com.regnosys.runefpml.RuneFpmlModelConfig;
import com.regnosys.testing.TestingExpectationUtil;
import com.regnosys.testing.pipeline.PipelineConfigWriter;
import com.regnosys.testing.pipeline.PipelineTreeConfig;
import com.rosetta.model.lib.functions.RosettaFunction;
import drr.ingest.fpml.recordkeeping.message.functions.Ingest_FpmlRecordKeepingToReportableEvent;
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
import drr.regulation.asic.rewrite.margin.reports.ASICMarginReportFunction;
import drr.regulation.asic.rewrite.trade.reports.ASICTradeReportFunction;
import drr.regulation.asic.rewrite.valuation.reports.ASICValuationReportFunction;
import drr.regulation.esma.emir.refit.margin.reports.ESMAEMIRMarginReportFunction;
import drr.regulation.esma.emir.refit.trade.reports.ESMAEMIRTradeReportFunction;
import drr.regulation.fca.ukemir.refit.margin.reports.FCAUKEMIRMarginReportFunction;
import drr.regulation.fca.ukemir.refit.trade.reports.FCAUKEMIRTradeReportFunction;
import drr.regulation.jfsa.rewrite.margin.reports.JFSAMarginReportFunction;
import drr.regulation.jfsa.rewrite.trade.reports.JFSATradeReportFunction;
import drr.regulation.mas.rewrite.margin.reports.MASMarginReportFunction;
import drr.regulation.mas.rewrite.trade.reports.MASTradeReportFunction;
import drr.regulation.mas.rewrite.valuation.reports.MASValuationReportFunction;
import fpml.consolidated.doc.Document;
import iso20022.Iso20022ModelConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import static com.regnosys.testing.pipeline.PipelineFilter.startsWith;
import static iso20022.Iso20022ModelConfig.TYPE_TO_SCHEMA_MAP;
import static iso20022.Iso20022ModelConfig.TYPE_TO_XML_CONFIG_MAP;
import static org.apache.commons.io.FileUtils.deleteDirectory;

/**
 * Generates the BANKABC pipeline configs, test packs and expected output samples.
 *
 * <p>Run it with {@code mvn clean -P update-expectations install -DskipTests}, then {@code mvn test}
 * to check the generated expectations back in.
 *
 * <p>How test pack discovery works, because it drives the shape of this class: the test pack writer
 * looks for sample files on disk underneath {@code TEST_WRITE_BASE_PATH}. A transform at the root of
 * a tree reads {@code <transform>/input}; every downstream transform reads the output samples written
 * by its upstream transform. Two consequences:
 * <ul>
 *     <li>A pipeline must be configured as a <em>single</em> tree from TRANSLATE through to
 *     PROJECTION. A tree that starts at ENRICH looks for {@code enrich/input} instead of the ingest
 *     output, finds nothing, and silently generates zero test packs.</li>
 *     <li>The DRR samples have to be on disk. Depending on {@code com.regnosys.drr:rosetta-source} is
 *     not enough - the jar is only visible to the tests at runtime. The {@code update-expectations}
 *     profile unpacks {@code ingest/input} (FpML record-keeping messages) and
 *     {@code regulatory-reporting/input} (valuation and collateral report instructions) from the DRR
 *     model before this class runs. Those directories are generated, so they are gitignored.</li>
 * </ul>
 *
 * <p>Every tree is prefixed with {@link #MODEL_ID}. DRR's own pipeline and test pack configs are on
 * the classpath via the DRR jar, and an unprefixed tree generates config ids that clash with them.
 *
 * <p>To add a jurisdiction of your own, replace the DRR report and projection functions below with
 * yours - the ingest and enrich stages stay the same.
 */
public class BANKABCTestPackCreator {

    private static final Logger LOGGER = LoggerFactory.getLogger(BANKABCTestPackCreator.class);

    public static final String MODEL_ID = "BANKABC";

    /**
     * Two DRR ingest test packs, enough to exercise every stage of every pipeline while keeping the
     * expectations committed to this template small. Widen this in your own model - the full set of
     * packs that are not tied to a single regime's event scenarios is {@code commodity},
     * {@code credit}, {@code custom-scenarios}, {@code equity}, {@code events}, {@code fx} and
     * {@code rates}. {@code cftc-event-scenarios}, {@code delegated-reporting}, {@code etd},
     * {@code exotic} and {@code pre-enrich} are regime-specific or need a different enrichment.
     * The pack names are the directory names under {@code ingest/input}.
     */
    private static final Predicate<String> TRADE_TEST_PACKS = startsWith("credit", "events");
    private static final Predicate<String> VALUATION_TEST_PACKS = startsWith("valuation");
    private static final Predicate<String> COLLATERAL_TEST_PACKS = startsWith("collateral");

    public static void main(String[] args) {
        try {
            Injector injector = new ReportTestRuntimeModule.InjectorProvider().getInjector();
            BANKABCTestPackCreator creator = injector.getInstance(BANKABCTestPackCreator.class);

            Stopwatch t = Stopwatch.createStarted();
            creator.clean();
            creator.generatePipelines();
            LOGGER.info("{} pipeline / test pack update took {}", MODEL_ID, t);

            System.exit(0);
        } catch (Exception e) {
            LOGGER.error("Error executing {}.main()", BANKABCTestPackCreator.class.getName(), e);
            System.exit(1);
        }
    }

    @Inject
    private PipelineConfigWriter pipelineConfigWriter;

    void generatePipelines() throws IOException {
        write(createTradeTreeConfig());
        write(createValuationTreeConfig());
        write(createCollateralTreeConfig());
    }

    /**
     * Trade: ingest -> enrich -> report -> projection. All the report branches hang off the one
     * enrich node, so the ingest and enrich samples are generated once and shared.
     */
    private PipelineTreeConfig createTradeTreeConfig() {
        Class<? extends RosettaFunction> enrichFunc = Enrich_ReportableEventToTransactionReportInstruction.class;
        return new PipelineTreeConfig(MODEL_ID)
                .withTestPackIdFilter(TRADE_TEST_PACKS)

                .starting(TransformType.TRANSLATE, Ingest_FpmlRecordKeepingToReportableEvent.class)
                .add(Ingest_FpmlRecordKeepingToReportableEvent.class, TransformType.ENRICH, enrichFunc)

                .add(enrichFunc, TransformType.REPORT, ASICTradeReportFunction.class)
                .add(ASICTradeReportFunction.class, TransformType.PROJECTION, Project_ASICTradeReportToIso20022.class)

                .add(enrichFunc, TransformType.REPORT, ESMAEMIRTradeReportFunction.class)
                .add(ESMAEMIRTradeReportFunction.class, TransformType.PROJECTION, Project_EsmaEmirTradeReportToIso20022.class)

                .add(enrichFunc, TransformType.REPORT, FCAUKEMIRTradeReportFunction.class)
                .add(FCAUKEMIRTradeReportFunction.class, TransformType.PROJECTION, Project_FcaUkEmirTradeReportToIso20022.class)

                .add(enrichFunc, TransformType.REPORT, JFSATradeReportFunction.class)
                .add(JFSATradeReportFunction.class, TransformType.PROJECTION, Project_JFSARewriteTradeReportToIso20022.class)

                .add(enrichFunc, TransformType.REPORT, MASTradeReportFunction.class)
                .add(MASTradeReportFunction.class, TransformType.PROJECTION, Project_MASTradeReportToIso20022.class);
    }

    /** Valuation: report -> projection. The DRR samples are already ValuationReportInstruction. */
    private PipelineTreeConfig createValuationTreeConfig() {
        return new PipelineTreeConfig(MODEL_ID)
                .withTestPackIdFilter(VALUATION_TEST_PACKS)

                .starting(TransformType.REPORT, ASICValuationReportFunction.class)
                .add(ASICValuationReportFunction.class, TransformType.PROJECTION, Project_ASICValuationReportToIso20022.class)

                .starting(TransformType.REPORT, MASValuationReportFunction.class)
                .add(MASValuationReportFunction.class, TransformType.PROJECTION, Project_MASValuationReportToIso20022.class);
    }

    /** Collateral: report -> projection. The DRR samples are already CollateralReportInstruction. */
    private PipelineTreeConfig createCollateralTreeConfig() {
        return new PipelineTreeConfig(MODEL_ID)
                .withTestPackIdFilter(COLLATERAL_TEST_PACKS)

                .starting(TransformType.REPORT, ASICMarginReportFunction.class)
                .add(ASICMarginReportFunction.class, TransformType.PROJECTION, Project_ASICMarginReportToIso20022.class)

                .starting(TransformType.REPORT, ESMAEMIRMarginReportFunction.class)
                .add(ESMAEMIRMarginReportFunction.class, TransformType.PROJECTION, Project_EsmaEmirMarginReportToIso20022.class)

                .starting(TransformType.REPORT, FCAUKEMIRMarginReportFunction.class)
                .add(FCAUKEMIRMarginReportFunction.class, TransformType.PROJECTION, Project_FcaUkEmirMarginReportToIso20022.class)

                .starting(TransformType.REPORT, JFSAMarginReportFunction.class)
                .add(JFSAMarginReportFunction.class, TransformType.PROJECTION, Project_JFSARewriteMarginReportToIso20022.class)

                .starting(TransformType.REPORT, MASMarginReportFunction.class)
                .add(MASMarginReportFunction.class, TransformType.PROJECTION, Project_MASMarginReportToIso20022.class);
    }

    private void write(PipelineTreeConfig pipelineTreeConfig) {
        try {
            pipelineConfigWriter.writePipelinesAndTestPacks(addXMLAndSchemaMap(pipelineTreeConfig));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private PipelineTreeConfig addXMLAndSchemaMap(PipelineTreeConfig pipelineTreeConfig) {
        ImmutableMap<Class<?>, String> fpmlTypeToXmlConfigMap = ImmutableMap.<Class<?>, String>builder()
                .put(Document.class, RuneFpmlModelConfig.FPML_RECORDKEEPING_XML_CONFIG_PATH)
                .build();

        ImmutableMap<Class<?>, String> fpmlTypeToSchemaMap = ImmutableMap.<Class<?>, String>builder()
                .put(Document.class, RuneFpmlModelConfig.FPML_RECORDKEEPING_SCHEMA_PATH)
                .build();

        return pipelineTreeConfig
                .withXmlConfigMap(mergeMaps(fpmlTypeToXmlConfigMap, TYPE_TO_XML_CONFIG_MAP))
                .withXmlSchemaMap(mergeMaps(fpmlTypeToSchemaMap, TYPE_TO_SCHEMA_MAP))
                .withInputSerialisationFormatMap(RuneFpmlModelConfig.TYPE_TO_FORMAT_MAP)
                .withOutputSerialisationFormatMap(Iso20022ModelConfig.TYPE_TO_FORMAT_MAP)
                .strictUniqueIds();
    }

    private static <T> ImmutableMap<Class<?>, T> mergeMaps(Map<Class<?>, ? extends T> map1, Map<Class<?>, ? extends T> map2) {
        return ImmutableMap.<Class<?>, T>builder()
                .putAll(map1)
                .putAll(map2)
                .build();
    }

    void clean() throws IOException {
        Optional<Path> writeBasePath = TestingExpectationUtil.TEST_WRITE_BASE_PATH;
        if (writeBasePath.isEmpty()) {
            LOGGER.error("TEST_WRITE_BASE_PATH not set, skipping clean");
            return;
        }
        Path path = writeBasePath.get();
        // These will all get regenerated by the PipelineConfigWriter. The input sample directories
        // (ingest/input, regulatory-reporting/input) are unpacked from DRR and left untouched.
        deleteDirectory(path.resolve("ingest/config").toFile());
        deleteDirectory(path.resolve("ingest/output").toFile());
        deleteDirectory(path.resolve("enrich").toFile());
        deleteDirectory(path.resolve("regulatory-reporting/config").toFile());
        deleteDirectory(path.resolve("regulatory-reporting/output").toFile());
        deleteDirectory(path.resolve("projection").toFile());
    }
}
