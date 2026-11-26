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
import org.jkiss.dbeaver.model.ai.engine.AIModelCatalog;
import org.jkiss.dbeaver.model.ai.engine.AIModelCatalogEntry;
import org.jkiss.dbeaver.model.ai.engine.copilot.CopilotModels;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

public class OpenAIModelsTest extends DBeaverUnitTest {

    @Test
    public void effectiveModelNameNullShouldReturnNull() {
        //when
        var result = OpenAIModels.getEffectiveModelName(null);
        //then
        Assertions.assertNull(result);
    }

    @Test
    public void modelMetadataComesFromEachProvidersCache() throws Exception {
        AIModelCatalogEntry openai = new Gson().fromJson("{\"limit\":{\"context\":500000}}", AIModelCatalogEntry.class);
        AIModelCatalogEntry copilot = new Gson().fromJson("{\"limit\":{\"context\":64000}}", AIModelCatalogEntry.class);
        try (AutoCloseable ignored = AIModelCatalog.useForTests(Map.of(
            "openai", Map.of("gpt-test", openai), "github-copilot", Map.of("gpt-test", copilot)
        ))) {

            Assertions.assertEquals(500_000, OpenAIModels.getModelByName("gpt-test").orElseThrow().contextWindowSize());
            Assertions.assertEquals(64_000, CopilotModels.getModelByName("gpt-test").orElseThrow().contextWindowSize());
            Assertions.assertTrue(OpenAIModels.getModelByName("gpt-5").isEmpty());
            Assertions.assertTrue(CopilotModels.getModelByName("claude-sonnet-4").isEmpty());
        }
    }

    @Test
    public void effectiveModelNameUnknownUppercaseShouldReturnKnownModelUppercase() {
        //given
        var inputModelName = "some-UNKNOWN-MODEL";
        //when
        var result = OpenAIModels.getEffectiveModelName(inputModelName);
        //then
        Assertions.assertEquals(inputModelName, result);
    }

    @Test
    public void copilotDoesNotFallBackToFirstPartyOpenAIMetadata() throws Exception {
        AIModelCatalogEntry entry = new Gson().fromJson("{\"limit\":{\"context\":500000}}", AIModelCatalogEntry.class);
        try (AutoCloseable ignored = AIModelCatalog.useForTests(Map.of("openai", Map.of("gpt-5", entry)))) {

            Assertions.assertTrue(CopilotModels.getModelByName("gpt-5").isEmpty());
        }
    }

}
