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
package org.jkiss.dbeaver.model.datadam;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.ai.engine.BaseAIEngineProperties;
import org.jkiss.dbeaver.model.ai.engine.openai.OpenAIProperties;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;

class DDAIEnvironmentTest {
    @Test
    void dataDamKeepsItsOwnEnvironmentAndDefaultEndpoint() {
        DDAIEngineProperties properties = withEnvironment(new DDAIEngineProperties(), Map.of(
            OpenAIProperties.ENV_API_KEY, "openai-key", OpenAIProperties.ENV_BASE_URL, "https://openai.example/",
            DDAIEngineProperties.ENV_API_KEY, "datadam-key", DDAIEngineProperties.ENV_MODEL, "datadam-model"
        ));
        properties.setUseEnvVariables(true);
        Assertions.assertEquals("datadam-key", properties.getEffectiveToken());
        Assertions.assertEquals(DDAIEngineProperties.DEFAULT_ENDPOINT, properties.getEffectiveBaseUrl());
        Assertions.assertEquals("datadam-model", properties.getEffectiveModel());
        Assertions.assertNull(properties.getToken());
    }

    @NotNull
    @SuppressWarnings("unchecked")
    private static <PROPERTIES extends BaseAIEngineProperties> PROPERTIES withEnvironment(
        @NotNull PROPERTIES properties,
        @NotNull Map<String, String> environment
    ) {
        return (PROPERTIES) Mockito.mock(properties.getClass(), Mockito.withSettings().spiedInstance(properties)
            .defaultAnswer(invocation -> "getEnvironmentVariable".equals(invocation.getMethod().getName())
                ? environment.get(invocation.getArgument(0)) : invocation.callRealMethod()));
    }
}
