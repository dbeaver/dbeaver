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
package org.jkiss.dbeaver.model.ai.openai;

import com.google.gson.annotations.SerializedName;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.ai.AIConstants;
import org.jkiss.dbeaver.model.ai.AIEngineProperties;
import org.jkiss.dbeaver.model.ai.utils.AIUtils;
import org.jkiss.dbeaver.model.meta.SecureProperty;
import org.jkiss.dbeaver.model.secret.DBSSecretController;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.CommonUtils;

public class OpenAIProperties implements AIEngineProperties {
    @SecureProperty
    @SerializedName("gpt.token")
    private String token;

    @SerializedName("gpt.model")
    private String model;

    @SerializedName("gpt.model.temperature")
    private double temperature;

    @SerializedName("gpt.log.query")
    private boolean loggingEnabled;

    public String getToken() {
        return token;
    }

    public String getModel() {
        if (fallbackToPrefStore()) {
            return DBWorkbench.getPlatform().getPreferenceStore().getString(OpenAIConstants.GPT_MODEL);
        }

        return model;
    }

    public double getTemperature() {
        if (fallbackToPrefStore()) {
            return DBWorkbench.getPlatform().getPreferenceStore().getDouble(OpenAIConstants.AI_TEMPERATURE);
        }

        return temperature;
    }

    public boolean isLoggingEnabled() {
        if (fallbackToPrefStore()) {
            return DBWorkbench.getPlatform().getPreferenceStore().getBoolean(AIConstants.AI_LOG_QUERY);
        }

        return loggingEnabled;
    }

    public void resolveSecrets() throws DBException {
        token = AIUtils.getSecretValueOrDefault(OpenAIConstants.GPT_API_TOKEN, token);
    }

    public void saveSecrets() throws DBException {
        DBSSecretController.getGlobalSecretController().setPrivateSecretValue(OpenAIConstants.GPT_API_TOKEN, token);
    }

    public void setToken(String token) {
        this.token = token;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public void setLoggingEnabled(boolean loggingEnabled) {
        this.loggingEnabled = loggingEnabled;
    }

    public boolean isValidConfiguration() {
        return !CommonUtils.isEmpty(getToken());
    }

    private boolean fallbackToPrefStore() {
        return this.model == null;
    }
}
