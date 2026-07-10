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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.ai.AIConfigurationProfile;
import org.jkiss.dbeaver.model.ai.engine.openai.OpenAIClientResponses;
import org.jkiss.dbeaver.model.ai.engine.openai.OpenAIModels;
import org.jkiss.dbeaver.model.ai.engine.openai.OpenAIProperties;
import org.jkiss.dbeaver.model.ai.utils.AIUtils;

public class DDAIEngineProperties extends OpenAIProperties {

    public static final String DEFAULT_ENDPOINT = "https://ai.datadam.cloud/v1/";
    public static final String DEFAULT_MODEL = "datadam-fast";

    private static final String DATADAM_API_TOKEN = "datadam.token";
    private static final int DEFAULT_CONTEXT_WINDOW_SIZE = 128_000;

    @NotNull
    @Override
    public String getBaseUrl() {
        String baseUrl = super.getBaseUrl();
        if (OpenAIClientResponses.OPENAI_ENDPOINT.equals(baseUrl)) {
            return DEFAULT_ENDPOINT;
        }
        return baseUrl;
    }

    @Nullable
    @Override
    public String getModel() {
        String model = super.getModel();
        if (OpenAIModels.DEFAULT_MODEL.equals(model)) {
            return DEFAULT_MODEL;
        }
        return model;
    }

    @Nullable
    @Override
    public Integer getContextWindowSize() {
        Integer contextWindowSize = super.getContextWindowSize();
        return contextWindowSize != null ? contextWindowSize : DEFAULT_CONTEXT_WINDOW_SIZE;
    }

    @Override
    public void resolveSecrets(@NotNull AIConfigurationProfile profile) throws DBException {
        if (getToken() == null) {
            setToken(AIUtils.getSecretValueOrDefault(profile, DATADAM_API_TOKEN, getToken()));
        }
    }

    @Override
    public void saveSecrets(@NotNull AIConfigurationProfile profile) throws DBException {
        AIUtils.setSecretValue(profile, DATADAM_API_TOKEN, getToken());
    }

    @Override
    public void deleteSecrets(@NotNull AIConfigurationProfile profile) throws DBException {
        AIUtils.deleteSecretValue(profile, DATADAM_API_TOKEN);
    }

//    @Override
//    public boolean isLoggingEnabled() {
//        return true;
//    }
}
