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

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.ai.AIConstants;
import org.jkiss.dbeaver.model.ai.AIEngineSettings;
import org.jkiss.dbeaver.model.ai.AISettingsRegistry;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.CommonUtils;

public class OpenAISettings {
    public static final OpenAISettings INSTANCE = new OpenAISettings(
        AISettingsRegistry.getInstance().getSettings().getEngineConfiguration(AIConstants.OPENAI_ENGINE)
    );

    private final AIEngineSettings settings;

    private OpenAISettings(AIEngineSettings settings) {
        this.settings = settings;
    }

    /**
     * Returns the token used to authenticate with the OpenAI API.
     */
    public String token() {
        Object token = settings.getProperties().get(AIConstants.GPT_API_TOKEN);
        if (token != null) {
            return token.toString();
        }

        return DBWorkbench.getPlatform().getPreferenceStore().getString(AIConstants.GPT_API_TOKEN);
    }

    /**
     * Returns the model used by the OpenAI API.
     */
    @NotNull
    public OpenAIModel model() {
        String modelId = CommonUtils.toString(settings.getProperties().get(AIConstants.GPT_MODEL), "");
        return CommonUtils.isEmpty(modelId) ? OpenAIModel.GPT_TURBO : OpenAIModel.getByName(modelId);
    }

    /**
     * Returns the temperature used by the OpenAI API.
     */
    public double temperature() {
        return CommonUtils.toDouble(settings.getProperties().get(AIConstants.AI_TEMPERATURE), 0.0);
    }

    /**
     * Returns whether logging is enabled.
     */
    public boolean isLoggingEnabled() {
        return CommonUtils.toBoolean(settings.getProperties().get(AIConstants.AI_LOG_QUERY));
    }

    /**
     * Returns whether the current configuration is valid.
     */
    public boolean isValidConfiguration() {
        return !CommonUtils.isEmpty(token());
    }
}
