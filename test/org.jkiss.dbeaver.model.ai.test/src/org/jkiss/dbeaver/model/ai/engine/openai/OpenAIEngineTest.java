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

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.ai.engine.AIModel;
import org.jkiss.dbeaver.model.ai.engine.AIModelFeature;
import org.jkiss.dbeaver.model.ai.engine.openai.dto.OAIModel;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import java.util.List;
import java.util.Set;

public class OpenAIEngineTest extends DBeaverUnitTest {
    private static final List<String> MODEL_NAMES = List.of(
        "nvidia/Nemotron-3-Nano-Omni-30B-A3B-Reasoning-FP8",
        "llama3.2:latest",
        "gpt-4o",
        "gpt-4o-audio-preview",
        "text-embedding-3-small"
    );

    @ParameterizedTest
    @ValueSource(strings = {"http://localhost:8000/v1/", "http://localhost:11434/v1", "https://api.openai.com.example/v1/"})
    public void customEndpointModelsAreAvailableForChat(@NotNull String baseUrl) throws DBException {
        List<AIModel> models = getModels(baseUrl);

        Assertions.assertEquals(MODEL_NAMES, models.stream().map(AIModel::name).toList());
        for (AIModel model : models) {
            Assertions.assertEquals(Set.of(AIModelFeature.CHAT, AIModelFeature.STREAMING), model.features());
            Assertions.assertNull(model.contextWindowSize());
        }
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {OpenAIClientResponses.OPENAI_ENDPOINT, "https://api.openai.com/v1"})
    public void defaultEndpointPreservesOpenAIModelClassification(@Nullable String baseUrl) throws DBException {
        List<AIModel> models = getModels(baseUrl);

        Assertions.assertEquals(
            List.of("gpt-4o"),
            models.stream().filter(model -> model.features().contains(AIModelFeature.CHAT)).map(AIModel::name).toList()
        );
        Assertions.assertEquals(OpenAIModels.KNOWN_MODELS.get("gpt-4o"), models.get(2));
        Assertions.assertEquals(OpenAIModels.KNOWN_MODELS.get("text-embedding-3-small"), models.get(4));
    }

    @NotNull
    private List<AIModel> getModels(@Nullable String baseUrl) throws DBException {
        OpenAIBaseProperties properties = Mockito.mock(OpenAIBaseProperties.class);
        Mockito.when(properties.getBaseUrl()).thenReturn(baseUrl);
        OpenAIClientResponses client = Mockito.mock(OpenAIClientResponses.class);
        DBRProgressMonitor monitor = new VoidProgressMonitor();
        Mockito.when(client.getModels(monitor)).thenReturn(
            MODEL_NAMES.stream().map(name -> new OAIModel(name, "model", 0, "test", null)).toList()
        );
        try (OpenAIEngine<OpenAIBaseProperties> engine = new OpenAIEngine<>(properties) {
            @NotNull
            @Override
            protected OpenAIClientResponses createClient() {
                return client;
            }
        }) {
            return engine.getModels(monitor);
        }
    }
}
