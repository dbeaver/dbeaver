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
package org.jkiss.dbeaver.model.ai.engine.ollama;

import com.google.gson.Gson;
import org.jkiss.dbeaver.model.ai.engine.AIModel;
import org.jkiss.dbeaver.model.ai.engine.AIModelFeature;
import org.jkiss.dbeaver.model.ai.engine.ollama.dto.OllamaModelInfo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

class OllamaModelMetadataTest {
    private static final Gson GSON = new Gson();

    @Test
    void ollamaReadsArchitectureContextInsteadOfConfiguredRuntimeContext() {
        AIModel model = GSON.fromJson("""
            {"model_info":{"general.architecture":"qwen3","qwen3.context_length":131072},
             "capabilities":["completion","tools","thinking"],
             "parameters":"num_ctx 4096\\ntemperature 0.6\\nstop \\\"<end>\\\""}
            """, OllamaModelInfo.class).toAIModel("local-model");

        Assertions.assertEquals(131_072, model.contextWindowSize());
        Assertions.assertEquals(0.6, model.defaultTemperature());
        Assertions.assertFalse(model.imageInputSupported());
        Assertions.assertEquals(Set.of(AIModelFeature.CHAT, AIModelFeature.TOOL_CALL, AIModelFeature.REASONING), model.features());
    }

    @Test
    void ollamaEmbeddingIsNotOfferedAsChat() {
        AIModel model = GSON.fromJson("""
            {"capabilities":["embedding"],"parameters":"temperature NaN"}
            """, OllamaModelInfo.class).toAIModel("embedding-model");

        Assertions.assertEquals(Set.of(AIModelFeature.EMBEDDING), model.features());
        Assertions.assertNull(model.contextWindowSize());
        Assertions.assertEquals(0.0, model.defaultTemperature());
    }

    @Test
    void oldOllamaWithoutMetadataPreservesChatFallback() {
        AIModel model = GSON.fromJson("{}", OllamaModelInfo.class).toAIModel("legacy-model");

        Assertions.assertEquals(Set.of(AIModelFeature.CHAT), model.features());
        Assertions.assertNull(model.contextWindowSize());
        Assertions.assertEquals(0.0, model.defaultTemperature());
        Assertions.assertTrue(model.imageInputSupported());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void ollamaVisionCapabilityControlsImageInput(boolean imageInputSupported) {
        OllamaModelInfo response = GSON.fromJson(
            imageInputSupported ? "{\"capabilities\":[\"completion\",\"vision\"]}" : "{\"capabilities\":[\"completion\"]}",
            OllamaModelInfo.class);

        AIModel model = response.toAIModel("local-model");

        Assertions.assertEquals(imageInputSupported, model.imageInputSupported());
        Assertions.assertEquals(imageInputSupported, model.features().contains(AIModelFeature.VISION));
    }

}
