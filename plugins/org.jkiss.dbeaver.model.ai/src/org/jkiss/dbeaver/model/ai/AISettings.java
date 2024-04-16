/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.runtime.DBWorkbench;

import java.util.*;

/**
 * AI integration
 */
public class AISettings {

    private static final Log log = Log.getLog(AISettings.class);


    private boolean aiDisabled;
    private String activeEngine;

    private final Map<String, AIEngineSettings> engineConfigurations = new LinkedHashMap<>();


    public AISettings() {
    }

    public boolean isAiDisabled() {
        return aiDisabled;
    }

    public void setAiDisabled(boolean aiDisabled) {
        this.aiDisabled = aiDisabled;
    }

    @Property
    public String getActiveEngine() {
        return activeEngine;
    }

    public void setActiveEngine(String activeEngine) {
        this.activeEngine = activeEngine;
    }

    @NotNull
    public Map<String, AIEngineSettings> getEngineConfigurations() {
        return engineConfigurations;
    }

    @NotNull
    public AIEngineSettings getEngineConfiguration(String engine) {
        AIEngineSettings settings = engineConfigurations.get(engine);
        if (settings == null) {
            settings = new AIEngineSettings();
            settings.setEngineEnabled(!aiDisabled);
            engineConfigurations.put(engine, settings);
        }
        tryMigrateFromPrefStore(engine, settings);
        return settings;
    }

    private void tryMigrateFromPrefStore(String engine, AIEngineSettings settings) {
        // migrate from pref store
        if (AIConstants.OPENAI_ENGINE.equals(engine) && settings.getProperties().get(AIConstants.GPT_MODEL) == null) {
            DBPPreferenceStore preferenceStore = DBWorkbench.getPlatform().getPreferenceStore();
            String model = preferenceStore.getString(AIConstants.GPT_MODEL);
            if (model != null) {
                settings.getProperties().put(AIConstants.GPT_MODEL, model);
            }
            Double temperature = preferenceStore.getDouble(AIConstants.AI_TEMPERATURE);
            settings.getProperties().put(AIConstants.AI_TEMPERATURE, temperature);
            Boolean logQuery = preferenceStore.getBoolean(AIConstants.AI_LOG_QUERY);
            settings.getProperties().put(AIConstants.AI_LOG_QUERY, logQuery);
        }
    }
}

