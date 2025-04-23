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

import org.jkiss.dbeaver.model.ai.AISettingsRegistry;
import org.jkiss.dbeaver.model.ai.openai.OpenAIModel;
import org.jkiss.utils.CommonUtils;

public class CopilotConfigurationImpl implements CopilotConfiguration {
    public CopilotConfigurationImpl(AISettingsRegistry registry) {
        this.registry = registry;
    }

    private final AISettingsRegistry registry;

    public String getModelName() {
        return CommonUtils.toString(
            getSettings().getProperties().getModel(),
            OpenAIModel.GPT_TURBO.getName()
        );
    }

    public String getAccessToken() {
        return getSettings().getProperties().getToken();
    }

    public boolean isValidConfiguration() {
        return !CommonUtils.isEmpty(getSettings().getProperties().getToken());
    }


    public double getTemperature() {
        return CommonUtils.toDouble(
            getSettings().getProperties().getTemperature(),
            0.0
        );
    }

    public boolean isLoggingEnabled() {
        return getSettings().getProperties().isLoggingEnabled();
    }

    private CopilotSettings getSettings() {
        return (CopilotSettings) registry.getSettings().getEngineConfiguration("copilot");
    }
}
