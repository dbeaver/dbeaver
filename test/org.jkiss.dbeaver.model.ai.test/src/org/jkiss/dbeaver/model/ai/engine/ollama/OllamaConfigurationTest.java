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

import org.eclipse.core.runtime.Platform;
import org.jkiss.dbeaver.model.ai.AISettings;
import org.jkiss.dbeaver.model.ai.registry.AIEngineDescriptor;
import org.jkiss.dbeaver.model.ai.registry.AIEngineRegistry;
import org.jkiss.dbeaver.model.ai.registry.AISettingsManager;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

public class OllamaConfigurationTest extends DBeaverUnitTest {
    @Test
    public void engineIsContributedByCommunityBundle() throws Exception {
        var contributions = Arrays.stream(Platform.getExtensionRegistry().getConfigurationElementsFor(AIEngineDescriptor.EXTENSION_ID))
            .filter(element -> "ollama".equals(element.getAttribute("id")))
            .toList();
        Assertions.assertEquals(1, contributions.size());
        Assertions.assertEquals("org.jkiss.dbeaver.model.ai", contributions.getFirst().getContributor().getName());
        AIEngineDescriptor descriptor = AIEngineRegistry.getInstance().getEngineDescriptor("ollama");
        Assertions.assertNotNull(descriptor);
        Assertions.assertEquals(OllamaProperties.class, descriptor.getPropertiesType());
        Assertions.assertTrue(descriptor.isSupportsFunctions());
        try (var engine = descriptor.createEngineInstance(new OllamaProperties())) {
            Assertions.assertInstanceOf(OllamaCompletionEngine.class, engine);
        }
    }

    @Test
    public void restoresExistingProProfileAndRoundTripsSettings() throws Exception {
        String config = """
            {
              "defaultConfiguration": "local-model",
              "configurations": {
                "local-model": {
                  "name": "Local Ollama",
                  "engine": "ollama",
                  "configuration": {
                    "ollama.hostname": "http://localhost:11434",
                    "ollama.model": "llama3",
                    "ollama.context.size": 8192,
                    "gpt.model.temperature": 0.4,
                    "gpt.timeout": 120,
                    "gpt.log.query": true,
                    "ai.useEnvVariables": true,
                    "global": false
                  }
                }
              }
            }
            """;
        AISettings settings = AISettingsManager.READ_PROPS_GSON.fromJson(config, AISettings.class);
        settings.finishSettingsLoading();
        settings.resolveSecrets();
        for (int iteration = 0; iteration < 2; iteration++) {
            var profile = settings.getDefaultConfiguration();
            Assertions.assertEquals("local-model", profile.getProfileId());
            Assertions.assertEquals("Local Ollama", profile.getProfileName());
            Assertions.assertEquals("ollama", profile.getEngineId());
            Assertions.assertFalse(profile.isGlobal());
            OllamaProperties properties = Assertions.assertInstanceOf(OllamaProperties.class, profile.getConfiguration());
            Assertions.assertEquals("http://localhost:11434", properties.getHostname());
            Assertions.assertEquals("llama3", properties.getModel());
            Assertions.assertEquals(8192, properties.getContextWindowSize());
            Assertions.assertEquals(0.4, properties.getTemperature());
            Assertions.assertEquals(120, properties.getTimeout());
            Assertions.assertTrue(properties.isLoggingEnabled());
            Assertions.assertTrue(properties.isUseEnvVariables());
            settings = AISettingsManager.READ_PROPS_GSON.fromJson(AISettingsManager.SAVE_PROPS_GSON.toJson(settings), AISettings.class);
            settings.finishSettingsLoading();
        }
    }

    @Test
    public void migratesLegacyProEngineConfiguration() throws Exception {
        String config = """
            {
              "activeEngine": "ollama",
              "engineConfigurations": {
                "ollama": {
                  "properties": {
                    "ollama.hostname": "localhost:11434",
                    "ollama.model": "llama3",
                    "ollama.context.size": 4096,
                    "gpt.model.temperature": 0.2
                  }
                }
              }
            }
            """;
        AISettings settings = AISettingsManager.READ_PROPS_GSON.fromJson(config, AISettings.class);
        settings.finishSettingsLoading();
        var profile = settings.getDefaultConfiguration();
        Assertions.assertEquals("ollama", profile.getEngineId());
        Assertions.assertTrue(profile.isMigrated());
        OllamaProperties properties = Assertions.assertInstanceOf(OllamaProperties.class, profile.getConfiguration());
        Assertions.assertEquals("http://localhost:11434", properties.getEffectiveHostname());
        Assertions.assertEquals("llama3", properties.getModel());
        Assertions.assertEquals(4096, properties.getContextWindowSize());
        Assertions.assertEquals(0.2, properties.getTemperature());
    }
}
