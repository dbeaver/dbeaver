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
package org.jkiss.dbeaver.model.ai.engine.minimax;

import com.google.gson.annotations.SerializedName;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.ai.engine.AIModel;
import org.jkiss.dbeaver.model.ai.engine.openai.OpenAIBaseProperties;
import org.jkiss.dbeaver.model.ai.utils.AIUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.SecureProperty;
import org.jkiss.dbeaver.model.secret.DBSSecretController;
import org.jkiss.utils.CommonUtils;

/**
 * MiniMax engine configuration properties
 */
public class MiniMaxProperties implements OpenAIBaseProperties {

    private static final String MINIMAX_BASE_URL = "minimax.base_url"; //$NON-NLS-1$
    private static final String MINIMAX_TOKEN = "minimax.token"; //$NON-NLS-1$
    private static final String MINIMAX_MODEL = "minimax.model"; //$NON-NLS-1$
    private static final String MINIMAX_CONTEXT_WINDOW_SIZE = "minimax.contextWindowSize"; //$NON-NLS-1$
    private static final String MINIMAX_TEMPERATURE = "minimax.model.temperature"; //$NON-NLS-1$
    private static final String MINIMAX_LOG_QUERY = "minimax.log.query"; //$NON-NLS-1$

    @Nullable
    @SerializedName(MINIMAX_BASE_URL)
    private String baseUrl;

    @Nullable
    @SecureProperty
    @SerializedName(MINIMAX_TOKEN)
    private String token;

    @Nullable
    @SerializedName(MINIMAX_MODEL)
    private String model;

    @Nullable
    @SerializedName(MINIMAX_CONTEXT_WINDOW_SIZE)
    private Integer contextWindowSize;

    @SerializedName(MINIMAX_TEMPERATURE)
    private Double temperature;

    @SerializedName(MINIMAX_LOG_QUERY)
    private Boolean loggingEnabled;

    @NotNull
    @Override
    @Property(order = 2, required = true)
    public String getBaseUrl() {
        if (baseUrl == null || baseUrl.isEmpty()) {
            return MiniMaxConstants.MINIMAX_ENDPOINT;
        }
        return baseUrl;
    }

    public void setBaseUrl(@Nullable String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @Nullable
    @Override
    @Property(order = 1, password = true, required = true)
    public String getToken() {
        return token;
    }

    public void setToken(@Nullable String token) {
        this.token = token;
    }

    @Override
    public boolean isLegacyApi() {
        return true;
    }

    @Nullable
    @Override
    @Property(order = 3)
    public String getModel() {
        if (model != null && !model.isEmpty()) {
            return model;
        }
        return MiniMaxConstants.DEFAULT_MODEL;
    }

    public void setModel(@Nullable String model) {
        this.model = model;
    }

    @Override
    @Property(order = 4)
    public double getTemperature() {
        if (temperature != null
            && Double.isFinite(temperature)
            && temperature != AIUtils.DEFAULT_TEMPERATURE
        ) {
            return clampTemperature(temperature);
        }
        return MiniMaxConstants.DEFAULT_TEMPERATURE;
    }

    public void setTemperature(double temperature) {
        this.temperature = AIUtils.normalizeTemperature(temperature);
    }

    @Override
    @Property(order = 5)
    public boolean isLoggingEnabled() {
        if (loggingEnabled != null) {
            return loggingEnabled;
        }
        return false;
    }

    public void setLoggingEnabled(boolean loggingEnabled) {
        this.loggingEnabled = loggingEnabled;
    }

    @Nullable
    @Override
    @Property(order = 6)
    public Integer getContextWindowSize() {
        if (contextWindowSize != null) {
            return contextWindowSize;
        }
        return MiniMaxModels.getModelByName(getModel())
            .map(AIModel::contextWindowSize)
            .orElse(null);
    }

    public void setContextWindowSize(@Nullable Integer contextWindowSize) {
        this.contextWindowSize = contextWindowSize;
    }

    @Override
    public void resolveSecrets() throws DBException {
        token = AIUtils.getSecretValueOrDefault(
            MiniMaxConstants.MINIMAX_API_TOKEN, token
        );
    }

    @Override
    public void saveSecrets() throws DBException {
        if (token != null) {
            DBSSecretController.getGlobalSecretController()
                .setPrivateSecretValue(MiniMaxConstants.MINIMAX_API_TOKEN, token);
        }
    }

    @Override
    public boolean isValidConfiguration() {
        return !CommonUtils.isEmpty(getToken());
    }

    /**
     * Clamps temperature to the MiniMax valid range (0.0, 1.0].
     * MiniMax does not accept temperature = 0.
     */
    static double clampTemperature(double temperature) {
        if (temperature <= 0.0) {
            return MiniMaxConstants.DEFAULT_TEMPERATURE;
        }
        return Math.min(temperature, 1.0);
    }
}
