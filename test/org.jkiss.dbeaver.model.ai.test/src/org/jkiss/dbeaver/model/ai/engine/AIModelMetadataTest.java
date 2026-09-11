/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.model.ai.engine;

import com.google.gson.Gson;
import org.jkiss.dbeaver.model.ai.engine.copilot.dto.CopilotModel;
import org.jkiss.dbeaver.model.ai.engine.openai.OpenAIModels;
import org.jkiss.dbeaver.model.ai.engine.openai.dto.OAIModel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Set;

class AIModelMetadataTest {
    private static final Gson GSON = new Gson();

    @Test
    void providerMetadataDoesNotUsePerModelTemperaturePresets() {
        OAIModel response = GSON.fromJson("""
            {"id":"gpt-5","context_length":128000}
            """, OAIModel.class);

        AIModel model = OpenAIModels.fromApiModel(response);

        Assertions.assertEquals(128_000, model.contextWindowSize());
        Assertions.assertEquals(0.0, model.defaultTemperature());
        Assertions.assertFalse(model.features().contains(AIModelFeature.ALWAYS_DEFAULT_TEMPERATURE));
    }

    @Test
    void readsContextForUnknownCompatibleModel() {
        AIModel model = OpenAIModels.fromApiModel(GSON.fromJson("""
            {"id":"custom-model","context_length":32000}
            """, OAIModel.class));

        Assertions.assertEquals(32_000, model.contextWindowSize());
    }

    @Test
    void missingOrInvalidContextRemainsUnknownEvenForPreviouslyHardcodedModels() {
        for (String context : new String[]{"null", "0", "-1"}) {
            AIModel model = OpenAIModels.fromApiModel(GSON.fromJson(
                "{\"id\":\"gpt-5\",\"context_length\":" + context + "}", OAIModel.class
            ));
            Assertions.assertNull(model.contextWindowSize());
        }
        Assertions.assertNull(OpenAIModels.fromApiModel(GSON.fromJson("{\"id\":\"custom\"}", OAIModel.class)).contextWindowSize());
    }

    @Test
    void copilotPreservesSeparateLimitsAndExplicitCapabilities() {
        CopilotModel response = GSON.fromJson("""
            {"id":"custom-chat","capabilities":{
              "type":"chat",
              "limits":{"max_context_window_tokens":128000,"max_prompt_tokens":96000,"max_output_tokens":32000},
              "supports":{"streaming":true,"tool_calls":true,"vision":false,"reasoning_effort":["low","high"]}
            }}
            """, CopilotModel.class);

        AIModel model = response.toAIModel();

        Assertions.assertEquals(128_000, model.contextWindowSize());
        Assertions.assertEquals(96_000, model.inputTokenLimit());
        Assertions.assertEquals(32_000, model.outputTokenLimit());
        Assertions.assertEquals(Set.of(
            AIModelFeature.CHAT, AIModelFeature.STREAMING, AIModelFeature.TOOL_CALL, AIModelFeature.REASONING
        ), model.features());
    }

    @Test
    void missingCopilotCapabilitiesDoNotInventLimitsOrStreamingSupport() {
        AIModel model = GSON.fromJson("{\"id\":\"legacy-chat\"}", CopilotModel.class).toAIModel();

        Assertions.assertEquals(Set.of(AIModelFeature.CHAT), model.features());
        Assertions.assertNull(model.contextWindowSize());
        Assertions.assertNull(model.inputTokenLimit());
        Assertions.assertNull(model.outputTokenLimit());
        Assertions.assertNull(model.maxTemperature());
    }

    @Test
    void catalogFillsMissingMetadataButDoesNotReplaceNativeContext() {
        AIModelCatalogEntry entry = GSON.fromJson("""
            {"limit":{"context":500000,"input":300000,"output":100000},"temperature":false,"tool_call":true}
            """, AIModelCatalogEntry.class);
        OAIModel model = GSON.fromJson("{\"id\":\"gpt-5\"}", OAIModel.class);

        AIModel result = OpenAIModels.fromApiModel(model, entry);
        Assertions.assertEquals(500_000, result.contextWindowSize());
        Assertions.assertEquals(300_000, result.inputTokenLimit());
        Assertions.assertEquals(100_000, result.outputTokenLimit());
        Assertions.assertEquals(0.0, result.defaultTemperature());
        Assertions.assertFalse(OpenAIModels.isTemperatureEditable(result));
        Assertions.assertTrue(result.features().contains(AIModelFeature.TOOL_CALL));

        OAIModel limited = GSON.fromJson("{\"id\":\"gpt-5\",\"context_length\":64000}", OAIModel.class);
        Assertions.assertEquals(64_000, OpenAIModels.fromApiModel(limited, entry).contextWindowSize());
    }

    @Test
    void temperatureSupportIsNotConfusedWithNumericDefault() {
        AIModel model = new AIModel("gpt-test", 128_000, Set.of(AIModelFeature.CHAT), 0.7);
        AIModelCatalogEntry unsupported = GSON.fromJson("{\"temperature\":false}", AIModelCatalogEntry.class);

        AIModel result = unsupported.enrich(model);
        Assertions.assertEquals(0.7, result.defaultTemperature());
        Assertions.assertNull(result.maxTemperature());
        Assertions.assertTrue(result.features().contains(AIModelFeature.TEMPERATURE_UNSUPPORTED));

        AIModelCatalogEntry supported = GSON.fromJson("{\"temperature\":true}", AIModelCatalogEntry.class);
        Assertions.assertTrue(OpenAIModels.isTemperatureEditable(supported.enrich(result)));
    }

    @Test
    void missingCatalogFieldsPreserveFallbackFeaturesAndDoNotInventStreaming() {
        AIModel fallback = new AIModel("gpt-test", 32_000, Set.of(AIModelFeature.CHAT, AIModelFeature.REASONING), 0.0);
        AIModelCatalogEntry entry = GSON.fromJson("{\"tool_call\":true}", AIModelCatalogEntry.class);

        AIModel result = entry.enrich(fallback);

        Assertions.assertEquals(32_000, result.contextWindowSize());
        Assertions.assertEquals(Set.of(AIModelFeature.CHAT, AIModelFeature.REASONING, AIModelFeature.TOOL_CALL), result.features());
    }

    @Test
    void copilotCatalogFillsMissingFieldsButNativeLimitsAndCapabilitiesWin() {
        CopilotModel response = GSON.fromJson("""
            {"id":"gpt-test","capabilities":{"type":"chat",
              "limits":{"max_context_window_tokens":64000,"max_output_tokens":16000},
              "supports":{"tool_calls":false,"vision":false,"thinking":false}
            }}
            """, CopilotModel.class);
        AIModelCatalogEntry entry = GSON.fromJson("""
            {"limit":{"context":500000,"input":48000,"output":128000},
             "tool_call":true,"reasoning":true,"modalities":{"input":["text","image"]},"temperature":false}
            """, AIModelCatalogEntry.class);

        AIModel model = response.toAIModel(entry);

        Assertions.assertEquals(64_000, model.contextWindowSize());
        Assertions.assertEquals(48_000, model.inputTokenLimit());
        Assertions.assertEquals(16_000, model.outputTokenLimit());
        Assertions.assertFalse(model.features().contains(AIModelFeature.TOOL_CALL));
        Assertions.assertFalse(model.features().contains(AIModelFeature.VISION));
        Assertions.assertFalse(model.features().contains(AIModelFeature.REASONING));
        Assertions.assertTrue(model.features().contains(AIModelFeature.TEMPERATURE_UNSUPPORTED));
    }
}
