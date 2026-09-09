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

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.ai.engine.copilot.CopilotProperties;
import org.jkiss.dbeaver.model.ai.engine.openai.OpenAIProperties;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Set;
import java.util.stream.Stream;

class AIModelSelectionTest {
    @ParameterizedTest
    @MethodSource("largeContextProperties")
    void switchingToSmallerModelReducesContextBudget(@NotNull AIEngineProperties properties) throws Exception {
        properties.setModel("gpt-4.1");

        properties.selectModel(new AIModel("gpt-4o", 128_000, Set.of(AIModelFeature.CHAT)));

        Assertions.assertEquals("gpt-4o", properties.getModel());
        Assertions.assertEquals(128_000, properties.getContextWindowSize());
    }

    @ParameterizedTest
    @MethodSource("smallContextProperties")
    void switchingToLargerModelIncreasesContextBudget(@NotNull AIEngineProperties properties) throws Exception {
        properties.setModel("gpt-4o");

        properties.selectModel(new AIModel("gpt-4.1", 1_048_576, Set.of(AIModelFeature.CHAT)));

        Assertions.assertEquals("gpt-4.1", properties.getModel());
        Assertions.assertEquals(1_048_576, properties.getContextWindowSize());
    }

    @ParameterizedTest
    @MethodSource("largeContextProperties")
    void missingMetadataDoesNotRetainPreviousModelWindow(@NotNull AIEngineProperties properties) throws Exception {
        properties.setModel("gpt-4.1");

        properties.selectModel(new AIModel("custom-model-without-metadata", null, Set.of(AIModelFeature.CHAT)));

        Assertions.assertEquals("custom-model-without-metadata", properties.getModel());
        Assertions.assertNull(properties.getContextWindowSize());
    }

    @NotNull
    private static Stream<AIEngineProperties> largeContextProperties() {
        return propertiesWithContextWindow(1_048_576);
    }

    @NotNull
    private static Stream<AIEngineProperties> smallContextProperties() {
        return propertiesWithContextWindow(128_000);
    }

    @NotNull
    private static Stream<AIEngineProperties> propertiesWithContextWindow(int contextWindowSize) {
        OpenAIProperties openAi = new OpenAIProperties();
        openAi.setContextWindowSize(contextWindowSize);
        CopilotProperties copilot = new CopilotProperties();
        copilot.setContextWindowSize(contextWindowSize);
        return Stream.of(openAi, copilot);
    }
}
