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
import org.jkiss.dbeaver.model.ai.AISettingsRegistry;
import org.jkiss.utils.CommonUtils;

public class OpenAISettingsImpl implements OpenAISettings {
    private final AISettingsRegistry registry;

    public OpenAISettingsImpl(AISettingsRegistry registry) {
        this.registry = registry;
    }

    @Override
    public String getToken() {
        return getConfiguration().properties().token();
    }

    @NotNull
    @Override
    public OpenAIModel getModel() {
        return OpenAIModel.getByName(getConfiguration().properties().model());
    }

    @Override
    public double getTemperature() {
        return getConfiguration().properties().temperature();
    }

    @Override
    public boolean isLoggingEnabled() {
        return getConfiguration().properties().loggingEnabled();
    }

    @Override
    public boolean isValidConfiguration() {
        return !CommonUtils.isEmpty(getToken());
    }

    private OpenAIConfiguration getConfiguration() {
        return (OpenAIConfiguration) registry.getSettings().getEngineConfiguration(AIConstants.OPENAI_ENGINE);
    }
}
