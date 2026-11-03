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
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.ai.engine.AIModel;
import org.jkiss.dbeaver.model.ai.engine.AIModelFeature;
import org.jkiss.dbeaver.model.ai.engine.ollama.dto.OllamaModelInfo;
import org.jkiss.dbeaver.model.ai.utils.MonitoredHttpClient;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.net.http.HttpRequest;
import java.util.List;
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

    @ParameterizedTest
    @ValueSource(strings = {"unavailable", "null", "", "{invalid", "[]"})
    void ollamaLoadsShowMetadataAndKeepsModelsWhenDetailsAreUnavailable(@NotNull String metadata) throws Exception {
        OllamaProperties properties = new OllamaProperties();
        properties.setHostname("http://localhost:11434");
        properties.setContextWindowSize(4096);
        DBRProgressMonitor monitor = Mockito.mock(DBRProgressMonitor.class);
        MonitoredHttpClient transport = Mockito.mock(MonitoredHttpClient.class);
        Mockito.when(transport.send(Mockito.any(), Mockito.any()))
            .thenReturn("{\"models\":[{\"name\":\"local-model\"},{\"name\":\"missing-details\"}]}")
            .thenReturn("""
                {"model_info":{"general.architecture":"llama","llama.context_length":8192},
                 "capabilities":["completion","vision"],"parameters":"temperature 0.7"}
                """)
            .thenAnswer(invocation -> {
                if (metadata.equals("unavailable")) {
                    throw new DBException("Model details unavailable");
                }
                return metadata;
            });
        try (
            OllamaCompletionEngine engine = new OllamaCompletionEngine(properties) {
                @NotNull
                @Override
                protected OllamaClient createClient() {
                    return new OllamaClient(properties.getHostname(), transport);
                }
            }
        ) {
            final List<AIModel> models = engine.getModels(monitor);

            ArgumentCaptor<HttpRequest> requests = ArgumentCaptor.forClass(HttpRequest.class);
            Mockito.verify(transport, Mockito.times(3)).send(Mockito.same(monitor), requests.capture());
            Assertions.assertEquals(List.of("GET", "POST", "POST"), requests.getAllValues().stream().map(HttpRequest::method).toList());
            Assertions.assertEquals(List.of("/api/tags", "/api/show", "/api/show"),
                requests.getAllValues().stream().map(request -> request.uri().getPath()).toList());
            Assertions.assertEquals(2, models.size());
            Assertions.assertEquals(8192, models.get(0).contextWindowSize());
            Assertions.assertEquals(0.7, models.get(0).defaultTemperature());
            Assertions.assertTrue(models.get(0).features().contains(AIModelFeature.VISION));
            Assertions.assertNull(models.get(1).contextWindowSize());
            Assertions.assertEquals(Set.of(AIModelFeature.CHAT), models.get(1).features());
            Assertions.assertEquals(4096, engine.getContextWindowSize(monitor));
        }
    }
}
