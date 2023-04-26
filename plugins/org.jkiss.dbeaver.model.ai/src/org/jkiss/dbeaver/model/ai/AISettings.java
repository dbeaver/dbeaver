/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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

import com.google.gson.Gson;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.auth.SMSession;
import org.jkiss.dbeaver.model.auth.SMSessionPersistent;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.CommonUtils;

import java.io.StringReader;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * AI integration
 */
public class AISettings {

    private static final Log log = Log.getLog(AISettings.class);

    private static final Gson gson = new Gson();
    private static final String AI_CONFIGURATION_JSON = "ai-configuration.json";

    private boolean aiDisabled;
    private final Set<String> disabledConnections = new LinkedHashSet<>();
    private final Set<String> enabledConnections = new LinkedHashSet<>();

    private final Map<String, AIEngineSettings> engineConfigurations = new LinkedHashMap<>();

    private AISettings() {
    }

    public boolean isAiDisabled() {
        return aiDisabled;
    }

    public void setAiDisabled(boolean aiDisabled) {
        this.aiDisabled = aiDisabled;
    }

    @NotNull
    public Set<String> getDisabledConnections() {
        return disabledConnections;
    }

    @NotNull
    public Set<String> getEnabledConnections() {
        return enabledConnections;
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
        return settings;
    }

    @NotNull
    public static AISettings getSettings() {
        try {
            AISettings settings = null;
            final SMSession session = DBWorkbench.getPlatform().getWorkspace().getWorkspaceSession();
            if (session instanceof SMSessionPersistent) {
                settings = ((SMSessionPersistent) session).getAttribute(AISettings.class.getName());
            }
            if (settings == null) {
                String content = DBWorkbench.getPlatform().getProductConfigurationController().loadConfigurationFile(AI_CONFIGURATION_JSON);
                if (CommonUtils.isEmpty(content)) {
                    settings = new AISettings();
                } else {
                    settings = gson.fromJson(new StringReader(content), AISettings.class);
                }
                if (session instanceof SMSessionPersistent) {
                    ((SMSessionPersistent) session).setAttribute(AISettings.class.getName(), settings);
                }
            }
            if (DBWorkbench.getPlatform().getPreferenceStore().getString(AICompletionConstants.AI_DISABLED) != null) {
                settings.setAiDisabled(DBWorkbench.getPlatform().getPreferenceStore().getBoolean(AICompletionConstants.AI_DISABLED));
            }
            return settings;
        } catch (Exception e) {
            log.error(e);
            return new AISettings();
        }
    }

    public void saveSettings() {
        try {
            String content = gson.toJson(this, AISettings.class);
            DBWorkbench.getPlatform().getProductConfigurationController().saveConfigurationFile(AI_CONFIGURATION_JSON, content);
            DBWorkbench.getPlatform().getPreferenceStore().setValue(AICompletionConstants.AI_DISABLED, aiDisabled);
        } catch (Exception e) {
            log.error(e);
        }
    }

}

