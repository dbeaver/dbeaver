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
package org.jkiss.dbeaver.model.ai.engine.copilot;

import com.google.gson.annotations.SerializedName;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.ai.engine.AIEngineProperties;
import org.jkiss.dbeaver.model.ai.engine.AIModel;
import org.jkiss.dbeaver.model.ai.utils.AIUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.SecureProperty;
import org.jkiss.dbeaver.model.secret.DBSSecretController;
import org.jkiss.utils.CommonUtils;

public class CopilotProperties implements AIEngineProperties {
    private static final String COPILOT_ACCESS_TOKEN = "copilot.access.token";
    private static final String GPT_MODEL = "gpt.model";
    private static final String GPT_CONTEXT_WINDOW_SIZE = "gpt.contextWindowSize";
    private static final String GPT_MODEL_TEMPERATURE = "gpt.model.temperature";
    private static final String GPT_LOG_QUERY = "gpt.log.query";

    @Nullable
    @SecureProperty
    @SerializedName(COPILOT_ACCESS_TOKEN)
    private String token;

    @Nullable
    @SerializedName(GPT_MODEL)
    private String model;

    @Nullable
    @SerializedName(GPT_CONTEXT_WINDOW_SIZE)
    private Integer contextWindowSize;

    @SerializedName(GPT_MODEL_TEMPERATURE)
    private double temperature;

    @SerializedName(GPT_LOG_QUERY)
    private boolean loggingEnabled;

    @Nullable
    @Property(order = 1, password = true)
    public String getToken() {
        return token;
    }

    public void setToken(@Nullable String token) {
        this.token = token;
    }

    @Nullable
    @Property(order = 2)
    public String getModel() {
        return model;
    }

    public void setModel(@Nullable String model) {
        this.model = model;
    }

    @Override
    @Property(order = 3)
    public double getTemperature() {
        if (temperature != 0.0) {
            return temperature;
        }
        return CopilotModels.getModelByName(model)
            .map(AIModel::defaultTemperature)
            .orElse(0.0);
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    @Override
    @Nullable
    @Property(order = 4)
    public Integer getContextWindowSize() {
        if (contextWindowSize != null) {
            return contextWindowSize;
        }

        return CopilotModels.getModelByName(model)
            .map(AIModel::contextWindowSize)
            .orElse(null);
    }

    public void setContextWindowSize(@Nullable Integer contextWindowSize) {
        this.contextWindowSize = contextWindowSize;
    }

    @Property(order = 5)
    public boolean isLoggingEnabled() {
        return loggingEnabled;
    }

    public void setLoggingEnabled(boolean loggingEnabled) {
        this.loggingEnabled = loggingEnabled;
    }

    /**
     * Resolve secrets from the secret controller.
     */
    public void resolveSecrets() throws DBException {
        token = AIUtils.getSecretValueOrDefault(CopilotConstants.COPILOT_ACCESS_TOKEN, token);
    }

    /**
     * Save secrets to the secret controller.
     */
    public void saveSecrets() throws DBException {
        if (token != null) {
            DBSSecretController.getGlobalSecretController().setPrivateSecretValue(
                CopilotConstants.COPILOT_ACCESS_TOKEN, token
            );
        }
    }

    @Override
    public boolean isValidConfiguration() {
        return !CommonUtils.isEmpty(getToken());
    }
}
