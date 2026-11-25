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
package org.jkiss.dbeaver.model.ai.engine.ollama;

import com.google.gson.annotations.SerializedName;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.ai.AIConfigurationProfile;
import org.jkiss.dbeaver.model.ai.engine.BaseAIEngineProperties;
import org.jkiss.dbeaver.model.ai.utils.AIUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.utils.CommonUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

public class OllamaProperties extends BaseAIEngineProperties {
    public static final String ENV_HOST = "OLLAMA_HOST";
    public static final String ENV_MODEL = "DBEAVER_AI_OLLAMA_MODEL";
    public static final String ENV_API_KEY = "OLLAMA_API_KEY";

    private static final String OLLAMA_HOSTNAME = "ollama.hostname";
    private static final String OLLAMA_MODEL = "ollama.model";
    private static final String OLLAMA_CONTEXT_SIZE = "ollama.context.size";
    private static final int DEFAULT_CONTEXT_SIZE = 30000;
    private static final int DEFAULT_PORT = 11434;

    @SerializedName(OLLAMA_HOSTNAME)
    private String hostname;
    @SerializedName(OLLAMA_MODEL)
    private String model;
    @SerializedName(OLLAMA_CONTEXT_SIZE)
    private int contextSize;

    public OllamaProperties() {
        this.contextSize = DEFAULT_CONTEXT_SIZE;
    }

    @Nullable
    @Property(order = 1, required = true)
    public String getHostname() {
        return hostname;
    }

    public void setHostname(@Nullable String hostname) {
        this.hostname = hostname;
    }

    @Nullable
    @Override
    @Property(order = 2, required = true)
    public String getModel() {
        return model;
    }

    public void setModel(@Nullable String model) {
        this.model = model;
    }

    @Nullable
    @Property(order = 3, min = 1)
    public Integer getContextWindowSize() {
        return contextSize;
    }

    public void setContextWindowSize(int contextSize) {
        this.contextSize = contextSize;
    }

    @Property(order = 4)
    public double getTemperature() {
        return AIUtils.normalizeTemperature(temperature);
    }

    @Override
    public void resolveSecrets(@NotNull AIConfigurationProfile profile) {

    }

    @Override
    public void saveSecrets(@NotNull AIConfigurationProfile profile) {

    }

    @Override
    public void deleteSecrets(@NotNull AIConfigurationProfile profile) throws DBException {

    }

    @Override
    public boolean isValidConfiguration() {
        return !CommonUtils.isEmpty(getEffectiveHostname()) && !CommonUtils.isEmpty(getEffectiveModel()) && getContextWindowSize() > 0;
    }

    @NotNull
    @Override
    public List<String> getEnvironmentVariables() {
        return List.of(ENV_HOST, ENV_API_KEY, ENV_MODEL);
    }

    @Nullable
    @Override
    protected String getEnvironmentModel() {
        return getEnvironmentValue(ENV_MODEL);
    }

    @Nullable
    public String getEffectiveHostname() {
        String host = resolveEnvironmentValue(getHostname(), ENV_HOST);
        if (CommonUtils.isEmptyTrimmed(host)) {
            return null;
        }
        if (host.contains("://")) {
            return host;
        }
        String baseUrl = "http://" + host;
        try {
            URI uri = new URI(baseUrl);
            String authority = uri.getRawAuthority();
            if (authority != null && uri.getPort() == -1) {
                return "http://" + authority + ":" + DEFAULT_PORT + host.substring(authority.length());
            }
        } catch (URISyntaxException expected) {
            return baseUrl;
        }
        return baseUrl;
    }

    @Nullable
    public String getEffectiveToken() {
        return getEnvironmentValue(ENV_API_KEY);
    }
}
