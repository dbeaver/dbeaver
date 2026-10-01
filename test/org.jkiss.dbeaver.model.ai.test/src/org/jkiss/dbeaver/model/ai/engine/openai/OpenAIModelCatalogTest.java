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
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

class OpenAIModelCatalogTest extends DBeaverUnitTest {
    @ParameterizedTest
    @ValueSource(strings = {"gpt-test", "gpt-test-2025-04-14"})
    void enrichesOnlyAvailableModelsAndUsesOnlyCacheForCompletions(@NotNull String modelId) throws Exception {
        OpenAIProperties properties = new OpenAIProperties();
        properties.setModel(modelId);
        properties.setTemperature(0.7);
        DBRProgressMonitor monitor = Mockito.mock(DBRProgressMonitor.class);
        OpenAIClientResponses client = Mockito.mock(OpenAIClientResponses.class);
        Mockito.when(client.getModels(monitor)).thenReturn(List.of(new OAIModel(modelId, "model", 0, "openai", null)));
        AIModelCatalogEntry entry = new Gson().fromJson("""
            {"limit":{"context":500000},"temperature":false,"tool_call":true}
            """, AIModelCatalogEntry.class);
        Map<String, AIModelCatalogEntry> entries = Map.of("gpt-test", entry, "not-available", entry);
        try (AutoCloseable ignored = AIModelCatalog.useForTests(Map.of("openai", entries))) {
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
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"gpt-test", "llama-test", "deepseek-test"})
    void compatibleEndpointsDoNotUseFirstPartyCatalogOrTemperatureRestrictions(@NotNull String modelId) throws Exception {
        OpenAIProperties properties = new OpenAIProperties();
        properties.setBaseUrl("https://custom-provider.example/v1");
        properties.setModel(modelId);
        properties.setTemperature(0.7);
        DBRProgressMonitor monitor = Mockito.mock(DBRProgressMonitor.class);
        OpenAIClientResponses client = Mockito.mock(OpenAIClientResponses.class);
        Mockito.when(client.getModels(monitor)).thenReturn(List.of(new OAIModel(modelId, "model", 0, "custom", 32000)));

        AIModelCatalogEntry entry = new Gson().fromJson(
            "{\"limit\":{\"context\":500000},\"temperature\":false}", AIModelCatalogEntry.class);
        try (AutoCloseable ignored = AIModelCatalog.useForTests(Map.of("openai", Map.of(modelId, entry)))) {
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
                Assertions.assertEquals(Set.of(AIModelFeature.CHAT, AIModelFeature.STREAMING), nativeModel.features());
                properties.selectModel(nativeModel);
                Assertions.assertEquals(32_000, engine.getContextWindowSize(monitor));
                properties.selectModel(new AIModel(modelId, null, Set.of(AIModelFeature.CHAT)));
                Assertions.assertEquals(AIConstants.DEFAULT_CONTEXT_WINDOW_SIZE, properties.getContextWindowSize());
                AIEngineResponseConsumer consumer = Mockito.mock(AIEngineResponseConsumer.class);
                engine.requestCompletionStream(monitor, new AIEngineRequest(AIMessage.userMessage("test")), consumer);

                ArgumentCaptor<OAIResponsesRequest> sent = ArgumentCaptor.forClass(OAIResponsesRequest.class);
                Mockito.verify(client).createChatCompletionStream(Mockito.same(monitor), sent.capture(), Mockito.same(consumer));
                Assertions.assertEquals(0.7, sent.getValue().temperature);
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
    void cachedContextRespectsEndpointChangesAndExplicitOverrides() throws Exception {
        AIModelCatalogEntry entry = new Gson().fromJson("{\"limit\":{\"context\":500000}}", AIModelCatalogEntry.class);
        try (AutoCloseable ignored = AIModelCatalog.useForTests(Map.of("openai", Map.of("gpt-test", entry)))) {
            OpenAIProperties properties = new OpenAIProperties();
            properties.setModel("gpt-test-2025-04-14");
            Assertions.assertEquals(500_000, properties.getContextWindowSize());
            Assertions.assertNull(properties.getConfiguredContextWindowSize());

            properties.setBaseUrl("https://custom-provider.example/v1");
            Assertions.assertNull(properties.getContextWindowSize());
            properties.setContextWindowSize(32_000);
            Assertions.assertEquals(32_000, properties.getContextWindowSize());
            Assertions.assertEquals(32_000, properties.getConfiguredContextWindowSize());
        }
    }

    @Test
    void accountAuthenticationIgnoresUnusedCustomApiBaseUrl() throws Exception {
        AIModelCatalogEntry entry = new Gson().fromJson("{\"limit\":{\"context\":500000}}", AIModelCatalogEntry.class);
        try (AutoCloseable ignored = AIModelCatalog.useForTests(Map.of("openai", Map.of("gpt-test", entry)))) {
            OpenAIProperties properties = new OpenAIProperties();
            properties.setBaseUrl("https://custom-provider.example/v1");
            properties.setAuthentication(OpenAIProperties.AUTHENTICATION_CHATGPT_ACCOUNT);
            properties.setModel("gpt-test-2025-04-14");

            Assertions.assertEquals(500_000, properties.getContextWindowSize());
            properties.selectModel(new AIModel("gpt-test-2025-04-14", 272_000, Set.of(AIModelFeature.CHAT)));
            Assertions.assertEquals(272_000, properties.getContextWindowSize());
        }
    }

    @Test
    void coldContextLookupInitializesCatalogOutsidePropertyGetters() throws Exception {
        AtomicInteger downloads = new AtomicInteger();
        AIModelCatalog catalog = new AIModelCatalog(null, Clock.systemUTC(), progress -> {
            Assertions.assertSame(monitor, progress);
            downloads.incrementAndGet();
            return "{\"openai\":{\"models\":{\"gpt-test\":{\"limit\":{\"context\":128000}}}}}";
        });
        OpenAIProperties properties = new OpenAIProperties();
        properties.setModel("gpt-test");
        try (
            AutoCloseable ignored = AIModelCatalog.useForTests(catalog);
            OpenAIEngine<OpenAIProperties> engine = new OpenAIEngine<>(properties)
        ) {
            Assertions.assertNull(properties.getContextWindowSize());
            Assertions.assertEquals(0, downloads.get());
            Assertions.assertEquals(128_000, engine.getContextWindowSize(monitor));
            Assertions.assertEquals(128_000, engine.getContextWindowSize(monitor));
            Assertions.assertEquals(1, downloads.get());
        }
    }

    @Test
    void coldOfflineLookupDoesNotRetryOnEveryRequest() throws Exception {
        AtomicInteger downloads = new AtomicInteger();
        AIModelCatalog catalog = new AIModelCatalog(null, Clock.systemUTC(), progress -> {
            downloads.incrementAndGet();
            throw new IOException("offline");
        });
        OpenAIProperties properties = new OpenAIProperties();
        properties.setModel("gpt-test");
        try (
            AutoCloseable ignored = AIModelCatalog.useForTests(catalog);
            OpenAIEngine<OpenAIProperties> engine = new OpenAIEngine<>(properties)
        ) {
            Assertions.assertThrows(DBException.class, () -> engine.getContextWindowSize(monitor));
            Assertions.assertThrows(DBException.class, () -> engine.getContextWindowSize(monitor));
            Assertions.assertEquals(1, downloads.get());
            properties.setContextWindowSize(32_000);
            Assertions.assertEquals(32_000, engine.getContextWindowSize(monitor));
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void contextLookupRefreshesExpiredCatalogAndPreservesExplicitBudget(boolean configured) throws Exception {
        Clock clock = Mockito.mock(Clock.class);
        AtomicInteger downloads = new AtomicInteger();
        AIModelCatalog catalog = new AIModelCatalog(null, clock, progress -> {
            int context = downloads.incrementAndGet() == 1 ? 128_000 : 64_000;
            return "{\"openai\":{\"models\":{\"gpt-test\":{\"limit\":{\"context\":" + context + "},\"temperature\":false}}}}";
        });
        OpenAIProperties properties = new OpenAIProperties();
        properties.setModel("gpt-test");
        if (configured) {
            properties.setContextWindowSize(32_000);
        }
        try (
            AutoCloseable ignored = AIModelCatalog.useForTests(catalog);
            OpenAIEngine<OpenAIProperties> engine = new OpenAIEngine<>(properties)
        ) {
            Assertions.assertEquals(configured ? 32_000 : null, properties.getContextWindowSize());
            Assertions.assertEquals(0, downloads.get());
            Assertions.assertEquals(configured ? 32_000 : 128_000, engine.getContextWindowSize(monitor));
            Assertions.assertEquals(1, downloads.get());

            Mockito.when(clock.millis()).thenReturn(Duration.ofDays(8).toMillis());
            Assertions.assertEquals(configured ? 32_000 : 128_000, properties.getContextWindowSize());
            Assertions.assertEquals(1, downloads.get());
            Assertions.assertEquals(configured ? 32_000 : 64_000, engine.getContextWindowSize(monitor));
            Assertions.assertEquals(configured ? 32_000 : 64_000, engine.getContextWindowSize(monitor));
            Assertions.assertEquals(2, downloads.get());
            Assertions.assertFalse(catalog.getCachedModels("openai").get("gpt-test").temperature());
        }
    }
}
