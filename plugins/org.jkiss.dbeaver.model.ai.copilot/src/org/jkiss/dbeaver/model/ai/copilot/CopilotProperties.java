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
package org.jkiss.dbeaver.model.ai.copilot;

import com.google.gson.annotations.SerializedName;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.ai.AIEngineProperties;
import org.jkiss.dbeaver.model.ai.utils.AIUtils;
import org.jkiss.dbeaver.model.meta.SecureProperty;
import org.jkiss.dbeaver.model.secret.DBSSecretController;
import org.jkiss.utils.CommonUtils;

public class CopilotProperties implements AIEngineProperties {
    @SecureProperty
    @SerializedName("copilot.access.token")
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

    public void setToken(String token) {
        this.token = token;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

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
        DBSSecretController.getGlobalSecretController().setPrivateSecretValue(
            CopilotConstants.COPILOT_ACCESS_TOKEN, token
        );
    }

    public boolean isValidConfiguration() {
        return !CommonUtils.isEmpty(getToken());
    }
}
