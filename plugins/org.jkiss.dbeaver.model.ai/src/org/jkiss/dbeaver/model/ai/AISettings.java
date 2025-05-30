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

import org.eclipse.core.runtime.IAdaptable;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AISettings implements IAdaptable {
    private boolean aiDisabled;
    private String activeEngine;
    private final Map<String, AIEngineSettings<?>> engineConfigurations = new HashMap<>();
    private final Set<String> resolvedSecrets = new HashSet<>();

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
    public synchronized <T extends AIEngineSettings<?>> T getEngineConfiguration(String engineId) throws DBException {
        AIEngineSettings<?> aiEngineSettings = engineConfigurations.get(engineId);

        if (!resolvedSecrets.contains(engineId)) {
            aiEngineSettings.resolveSecrets();
            resolvedSecrets.add(engineId);
        }

        return (T) aiEngineSettings;
    }

    public void setEngineConfiguration(String engineId, AIEngineSettings<?> engineConfiguration) {
        engineConfigurations.put(engineId, engineConfiguration);
    }

    public void setEngineConfigurations(Map<String, AIEngineSettings<?>> engineConfigurations) {
        this.engineConfigurations.putAll(engineConfigurations);
    }

    public void saveSecrets() throws DBException {
        for (AIEngineSettings<?> engineConfiguration : engineConfigurations.values()) {
            engineConfiguration.saveSecrets();
        }
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        return null;
    }
}
