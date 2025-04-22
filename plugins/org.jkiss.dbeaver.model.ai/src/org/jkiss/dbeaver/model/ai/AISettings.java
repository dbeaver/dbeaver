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
package org.jkiss.dbeaver.model.ai;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;

import java.util.Map;

public class AISettings {
    private boolean aiDisabled;
    private String activeEngine;
    private Map<String, AIEngineConfiguration> engineConfigurations;

    public boolean isAiDisabled() {
        return aiDisabled;
    }

    public void setAiDisabled(boolean aiDisabled) {
        this.aiDisabled = aiDisabled;
    }

    public String activeEngine() {
        return activeEngine;
    }

    public void setActiveEngine(String activeEngine) {
        this.activeEngine = activeEngine;
    }

    @NotNull
    public <T extends AIEngineConfiguration> AIEngineConfiguration getEngineConfiguration(String engineId) {
        return engineConfigurations.get(engineId);
    }

    public void setEngineConfiguration(String engineId, AIEngineConfiguration engineConfiguration) {
        engineConfigurations.put(engineId, engineConfiguration);
    }

    public void setEngineConfigurations(Map<String, AIEngineConfiguration> engineConfigurations) {
        this.engineConfigurations = engineConfigurations;
    }

    public void resolveSecrets() throws DBException {
        for (AIEngineConfiguration engineConfiguration : engineConfigurations.values()) {
            engineConfiguration.resolveSecrets();
        }
    }

    public void saveSecrets() throws DBException {
        for (AIEngineConfiguration engineConfiguration : engineConfigurations.values()) {
            engineConfiguration.saveSecrets();
        }
    }
}
