package com.regnosys.placeholder;

import cdm.base.datetime.functions.Now;
import com.regnosys.rosetta.common.model.FunctionMemoisingModuleBuilder;
import com.rosetta.model.lib.ModuleConfig;
import drr.enrichment.lei.functions.API_GetLeiData;
import drr.enrichment.lei.functions.API_GetLeiDataImpl;
import drr.standards.iso.functions.API_GetMicData;
import drr.standards.iso.functions.API_GetMicDataImpl;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

/**
 * Runtime module that overrides the Now function to return a fixed date time which can be used in expectations.
 * Template should be replaced with the model name.
 */
@ModuleConfig(model = "PLACEHOLDER_DRR", type = "Rosetta")
public class PlaceholderRuntimeModuleTesting extends PlaceholderRuntimeModule {
    @Override
    protected void configure() {
        super.configure();
        bind(API_GetLeiData.class).toInstance(bindApiGetLeiDataInstance());
        bind(API_GetMicData.class).to(bindAPIGetMicData()).asEagerSingleton();
        install(new FunctionMemoisingModuleBuilder()
                .setFromEnvironment()
                .build());
    }

    protected API_GetLeiData bindApiGetLeiDataInstance() {
        return new API_GetLeiDataImpl();
    }

    protected Class<? extends API_GetMicData> bindAPIGetMicData() {
        return API_GetMicDataImpl.class;
    }

    protected Class<? extends Now> bindNow() {
        return FixedZonedDateTime.class;
    }

    /**
     * Configure a fixed datetime for the report expectations
     */
    private static class FixedZonedDateTime extends Now {
        @Override
        protected ZonedDateTime doEvaluate() {
            return ZonedDateTime.now(Clock.fixed(Instant.parse("2024-04-29T00:00:00.00Z"), ZoneOffset.UTC));
        }
    }
}
