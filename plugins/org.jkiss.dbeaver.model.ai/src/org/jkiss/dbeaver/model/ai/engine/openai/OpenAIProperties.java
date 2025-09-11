/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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

import com.google.gson.annotations.SerializedName;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.ai.AIConstants;
import org.jkiss.dbeaver.model.ai.engine.AIModel;
import org.jkiss.dbeaver.model.ai.engine.AIModelFeature;
import org.jkiss.dbeaver.model.ai.utils.AIUtils;
import org.jkiss.dbeaver.model.meta.IPropertyValueListProvider;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.SecureProperty;
import org.jkiss.dbeaver.model.secret.DBSSecretController;
import org.jkiss.dbeaver.runtime.DBWorkbench;

import java.util.Map;

public class OpenAIProperties implements OpenAIBaseProperties {
    private static final String GPT_BASE_URL = "gpt.base_url";
    private static final String GPT_TOKEN = "gpt.token";
    private static final String GPT_MODEL = "gpt.model";
    private static final String GPT_CONTEXT_WINDOW_SIZE = "gpt.contextWindowSize";
    private static final String GPT_MODEL_TEMPERATURE = "gpt.model.temperature";
    private static final String GPT_LOG_QUERY = "gpt.log.query";
    @Nullable
    @SerializedName(GPT_BASE_URL)
    private String baseUrl;

    @Nullable
    @SecureProperty
    @SerializedName(GPT_TOKEN)
    private String token;

    @Nullable
    @SerializedName(GPT_MODEL)
    private String model;

    @Nullable
    @SerializedName(GPT_CONTEXT_WINDOW_SIZE)
    private Integer contextWindowSize;

    @SerializedName(GPT_MODEL_TEMPERATURE)
    private Double temperature;

    @SerializedName(GPT_LOG_QUERY)
    private Boolean loggingEnabled;

    public OpenAIProperties() {
    }

    @Nullable
    @Override
    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(@Nullable String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @Nullable
    @Override
    @Property(order = 1, id = GPT_TOKEN, password = true)
    public String getToken() {
        return token;
    }

    public void setToken(@Nullable String token) {
        this.token = token;
    }

    @Override
    @Property(order = 2, id = GPT_MODEL, listProvider = OpenAIModelListProvider.class)
    public String getModel() {
        if (model != null) {
            return OpenAIModels.getEffectiveModelName(model);
        }

        String modelName = DBWorkbench.getPlatform()
            .getPreferenceStore()
            .getString(OpenAIConstants.GPT_MODEL);
        return OpenAIModels.getEffectiveModelName(modelName);
    }

    public void setModel(@Nullable String model) {
        this.model = model;
    }

    @Override
    @Property(order = 3, id = GPT_MODEL_TEMPERATURE)
    public double getTemperature() {
        if (temperature != null) {
            return temperature;
        }

        return DBWorkbench.getPlatform()
            .getPreferenceStore()
            .getDouble(OpenAIConstants.AI_TEMPERATURE);
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    @Override
    @Property(order = 4, id = GPT_LOG_QUERY)
    public boolean isLoggingEnabled() {
        if (loggingEnabled != null) {
            return loggingEnabled;
        }

        return DBWorkbench.getPlatform()
            .getPreferenceStore()
            .getBoolean(AIConstants.AI_LOG_QUERY);
    }

    public void setLoggingEnabled(boolean loggingEnabled) {
        this.loggingEnabled = loggingEnabled;
    }

    @Nullable
    @Override
    @Property(order = 5, id = GPT_CONTEXT_WINDOW_SIZE, required = true)
    public Integer getContextWindowSize() {
        if (contextWindowSize != null) {
            return contextWindowSize;
        }

        return OpenAIModels.getModelByName(getModel())
            .map(AIModel::contextWindowSize)
            .orElse(null);
    }

    public void setContextWindowSize(@Nullable Integer contextWindowSize) {
        this.contextWindowSize = contextWindowSize;
    }

    @Override
    public void resolveSecrets() throws DBException {
        token = AIUtils.getSecretValueOrDefault(OpenAIConstants.GPT_API_TOKEN, token);
    }

    @Override
    public void saveSecrets() throws DBException {
        if (token != null) {
            DBSSecretController.getGlobalSecretController().setPrivateSecretValue(OpenAIConstants.GPT_API_TOKEN, token);
        }
    }

    public static class OpenAIModelListProvider implements IPropertyValueListProvider<OpenAIProperties> {

        @Override
        public boolean allowCustomValue() {
            return false;
        }

        @Override
        public Object[] getPossibleValues(OpenAIProperties object) {
            return OpenAIModels.KNOWN_MODELS.entrySet().stream()
                .filter(entry -> !entry.getValue().features().contains(AIModelFeature.SPEECH_TO_TEXT))
                .map(Map.Entry::getKey)
                .toArray();
        }
    }
}
