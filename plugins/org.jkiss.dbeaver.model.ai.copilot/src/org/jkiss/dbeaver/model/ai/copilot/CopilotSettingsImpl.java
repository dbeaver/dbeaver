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

public class CopilotSettingsImpl implements CopilotSettings {
    public CopilotSettingsImpl(AISettingsRegistry registry) {
        this.registry = registry;
    }

    private final AISettingsRegistry registry;

    /**
     * Returns the model name to use for Copilot.
     */
    public String modelName() {
        return CommonUtils.toString(
            getCopilotConfiguration().getProperties().model(),
            OpenAIModel.GPT_TURBO16.getName()
        );
    }

    /**
     * Returns the access token to use for Copilot.
     */
    public String accessToken() {
        return getCopilotConfiguration().getProperties().token();
    }

    /**
     * Returns whether the configuration is valid.
     */
    public boolean isValidConfiguration() {
        return !CommonUtils.isEmpty(getCopilotConfiguration().getProperties().token());
    }

    /**
     * Returns the temperature to use for Copilot.
     */
    public double temperature() {
        return CommonUtils.toDouble(
            getCopilotConfiguration().getProperties().temperature(),
            0.0
        );
    }

    /**
     * Returns whether logging is enabled.
     */
    public boolean isLoggingEnabled() {
        return getCopilotConfiguration().getProperties().loggingEnabled();
    }

    private CopilotConfiguration getCopilotConfiguration() {
        return (CopilotConfiguration) registry.getSettings().getEngineConfiguration("copilot");
    }
}
