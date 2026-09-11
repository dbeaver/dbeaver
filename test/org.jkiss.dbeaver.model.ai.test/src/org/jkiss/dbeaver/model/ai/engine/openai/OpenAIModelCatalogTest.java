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
package org.jkiss.dbeaver.model.ai.engine.openai;

import com.google.gson.Gson;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.ai.AIConstants;
import org.jkiss.dbeaver.model.ai.AIMessage;
import org.jkiss.dbeaver.model.ai.engine.AIEngineRequest;
import org.jkiss.dbeaver.model.ai.engine.AIEngineResponseConsumer;
import org.jkiss.dbeaver.model.ai.engine.AIModel;
import org.jkiss.dbeaver.model.ai.engine.AIModelCatalog;
import org.jkiss.dbeaver.model.ai.engine.AIModelCatalogEntry;
import org.jkiss.dbeaver.model.ai.engine.AIModelFeature;
import org.jkiss.dbeaver.model.ai.engine.openai.dto.OAIModel;
import org.jkiss.dbeaver.model.ai.engine.openai.dto.OAIResponsesRequest;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;
import java.util.Set;

class OpenAIModelCatalogTest {
    @ParameterizedTest
    @ValueSource(strings = {"gpt-test", "gpt-test-2025-04-14"})
    void enrichesOnlyAvailableModelsAndUsesOnlyCacheForCompletions(@NotNull String modelId) throws Exception {
        OpenAIProperties properties = new OpenAIProperties();
        properties.setModel(modelId);
        properties.setTemperature(0.7);
        DBRProgressMonitor monitor = Mockito.mock(DBRProgressMonitor.class);
        OpenAIClientResponses client = Mockito.mock(OpenAIClientResponses.class);
        Mockito.when(client.getModels(monitor)).thenReturn(List.of(new OAIModel(modelId, "model", 0, "openai", null)));
        AIModelCatalog catalog = Mockito.mock(AIModelCatalog.class);
        AIModelCatalogEntry entry = new Gson().fromJson("""
            {"limit":{"context":500000},"temperature":false,"tool_call":true}
            """, AIModelCatalogEntry.class);
        Map<String, AIModelCatalogEntry> entries = Map.of("gpt-test", entry, "not-available", entry);
        Mockito.when(catalog.getModels("openai")).thenReturn(entries);
        Mockito.when(catalog.getCachedModels("openai")).thenReturn(entries);

        try (MockedStatic<AIModelCatalog> singleton = Mockito.mockStatic(AIModelCatalog.class)) {
            singleton.when(AIModelCatalog::getInstance).thenReturn(catalog);
            try (OpenAIEngine<OpenAIProperties> engine = new OpenAIEngine<>(properties) {
                @NotNull
                @Override
                protected OpenAIClientResponses createClient() {
                    return client;
                }
            }) {
                var models = engine.getModels(monitor);
                Assertions.assertEquals(1, models.size());
                Assertions.assertEquals(modelId, models.getFirst().name());
                Assertions.assertEquals(500_000, models.getFirst().contextWindowSize());
                Assertions.assertFalse(OpenAIModels.isTemperatureEditable(models.getFirst()));
                Assertions.assertEquals(500_000, engine.getContextWindowSize(monitor));

                AIEngineRequest request = new AIEngineRequest(AIMessage.userMessage("test"));
                AIEngineResponseConsumer consumer = Mockito.mock(AIEngineResponseConsumer.class);
                engine.requestCompletionStream(monitor, request, consumer);
                engine.requestCompletionStream(monitor, request, consumer);

                ArgumentCaptor<OAIResponsesRequest> sent = ArgumentCaptor.forClass(OAIResponsesRequest.class);
                Mockito.verify(client, Mockito.times(2)).createChatCompletionStream(
                    Mockito.same(monitor), sent.capture(), Mockito.same(consumer)
                );
                Assertions.assertTrue(sent.getAllValues().stream().allMatch(value -> value.temperature == null));
                Assertions.assertTrue(sent.getAllValues().stream().allMatch(value -> modelId.equals(value.model)));
                Mockito.verify(catalog, Mockito.times(1)).getModels("openai");
            }
        }
    }

    @Test
    void compatibleEndpointsDoNotUseFirstPartyCatalogOrTemperatureRestrictions() throws Exception {
        OpenAIProperties properties = new OpenAIProperties();
        properties.setBaseUrl("https://custom-provider.example/v1");
        properties.setModel("gpt-test");
        properties.setTemperature(0.7);
        DBRProgressMonitor monitor = Mockito.mock(DBRProgressMonitor.class);
        OpenAIClientResponses client = Mockito.mock(OpenAIClientResponses.class);
        Mockito.when(client.getModels(monitor)).thenReturn(List.of(new OAIModel("gpt-test", "model", 0, "custom", 32000)));

        try (MockedStatic<AIModelCatalog> singleton = Mockito.mockStatic(AIModelCatalog.class)) {
            try (OpenAIEngine<OpenAIProperties> engine = new OpenAIEngine<>(properties) {
                @NotNull
                @Override
                protected OpenAIClientResponses createClient() {
                    return client;
                }
            }) {
                Assertions.assertNull(properties.getContextWindowSize());
                Assertions.assertThrows(DBException.class, () -> engine.getContextWindowSize(monitor));
                AIModel nativeModel = engine.getModels(monitor).getFirst();
                properties.selectModel(nativeModel);
                Assertions.assertEquals(32_000, engine.getContextWindowSize(monitor));
                properties.selectModel(new AIModel("gpt-test", null, Set.of(AIModelFeature.CHAT)));
                Assertions.assertEquals(AIConstants.DEFAULT_CONTEXT_WINDOW_SIZE, properties.getContextWindowSize());
                AIEngineResponseConsumer consumer = Mockito.mock(AIEngineResponseConsumer.class);
                engine.requestCompletionStream(monitor, new AIEngineRequest(AIMessage.userMessage("test")), consumer);

                ArgumentCaptor<OAIResponsesRequest> sent = ArgumentCaptor.forClass(OAIResponsesRequest.class);
                Mockito.verify(client).createChatCompletionStream(Mockito.same(monitor), sent.capture(), Mockito.same(consumer));
                Assertions.assertEquals(0.7, sent.getValue().temperature);
                singleton.verifyNoInteractions();
            }
        }
    }

    @Test
    void exactSnapshotMetadataWinsAndNonDateSuffixesAreNotStripped() {
        AIModelCatalogEntry base = new Gson().fromJson("{\"limit\":{\"context\":500000}}", AIModelCatalogEntry.class);
        AIModelCatalogEntry snapshot = new Gson().fromJson("{\"limit\":{\"context\":64000}}", AIModelCatalogEntry.class);
        Map<String, AIModelCatalogEntry> entries = Map.of("gpt-test", base, "gpt-test-2025-04-14", snapshot);

        Assertions.assertSame(snapshot, OpenAIModels.findCatalogEntry(entries, "gpt-test-2025-04-14"));
        Assertions.assertSame(base, OpenAIModels.findCatalogEntry(entries, "gpt-test-2025-05-01"));
        Assertions.assertNull(OpenAIModels.findCatalogEntry(entries, "gpt-test-pro"));
        Assertions.assertNull(OpenAIModels.findCatalogEntry(entries, "gpt-test-mini-2025-05-01"));
        Assertions.assertNull(OpenAIModels.findCatalogEntry(entries, null));
    }

    @Test
    void cachedContextRespectsEndpointChangesAndExplicitOverrides() {
        AIModelCatalogEntry entry = new Gson().fromJson("{\"limit\":{\"context\":500000}}", AIModelCatalogEntry.class);
        AIModelCatalog catalog = Mockito.mock(AIModelCatalog.class);
        Mockito.when(catalog.getCachedModels("openai")).thenReturn(Map.of("gpt-test", entry));
        try (MockedStatic<AIModelCatalog> singleton = Mockito.mockStatic(AIModelCatalog.class)) {
            singleton.when(AIModelCatalog::getInstance).thenReturn(catalog);
            OpenAIProperties properties = new OpenAIProperties();
            properties.setModel("gpt-test-2025-04-14");
            Assertions.assertEquals(500_000, properties.getContextWindowSize());

            properties.setBaseUrl("https://custom-provider.example/v1");
            Assertions.assertNull(properties.getContextWindowSize());
            properties.setContextWindowSize(32_000);
            Assertions.assertEquals(32_000, properties.getContextWindowSize());
            Mockito.verify(catalog, Mockito.times(1)).getCachedModels("openai");
            Mockito.verify(catalog, Mockito.never()).getModels(Mockito.anyString());
        }
    }

    @Test
    void accountAuthenticationIgnoresUnusedCustomApiBaseUrl() {
        AIModelCatalogEntry entry = new Gson().fromJson("{\"limit\":{\"context\":500000}}", AIModelCatalogEntry.class);
        AIModelCatalog catalog = Mockito.mock(AIModelCatalog.class);
        Mockito.when(catalog.getCachedModels("openai")).thenReturn(Map.of("gpt-test", entry));
        try (MockedStatic<AIModelCatalog> singleton = Mockito.mockStatic(AIModelCatalog.class)) {
            singleton.when(AIModelCatalog::getInstance).thenReturn(catalog);
            OpenAIProperties properties = new OpenAIProperties();
            properties.setBaseUrl("https://custom-provider.example/v1");
            properties.setAuthentication(OpenAIProperties.AUTHENTICATION_CHATGPT_ACCOUNT);
            properties.setModel("gpt-test-2025-04-14");

            Assertions.assertEquals(500_000, properties.getContextWindowSize());
            properties.selectModel(new AIModel("gpt-test-2025-04-14", 272_000, Set.of(AIModelFeature.CHAT)));
            Assertions.assertEquals(272_000, properties.getContextWindowSize());
            Mockito.verify(catalog, Mockito.never()).getModels(Mockito.anyString());
        }
    }
}
