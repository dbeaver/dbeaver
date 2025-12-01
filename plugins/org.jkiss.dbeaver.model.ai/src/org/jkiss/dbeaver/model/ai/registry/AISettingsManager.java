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
package org.jkiss.dbeaver.model.ai.registry;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.WorkspaceConfigEventManager;
import org.jkiss.dbeaver.model.ai.AISettings;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public class AISettingsManager {
    private static final Log log = Log.getLog(AISettingsManager.class);

    public static final String AI_CONFIGURATION_FILE_NAME = "ai-configuration.json";

    private static AISettingsManager instance = null;

    private final AISettingsWriter settingsWriter = new AISettingsWriter(AI_CONFIGURATION_FILE_NAME);
    private final AtomicReference<AISettings> settingsHolder = new AtomicReference<>();

    private final Set<AISettingsEventListener> settingsChangedListeners = Collections.synchronizedSet(new HashSet<>());

    private AISettingsManager() {
        WorkspaceConfigEventManager.addConfigChangedListener(
            AI_CONFIGURATION_FILE_NAME, o -> {
                // reset current context for settings to be lazily reloaded when needed
                this.settingsHolder.set(null);
                this.raiseChangedEvent(this); // consider detailed event info
            });
    }

    public static synchronized AISettingsManager getInstance() {
        if (instance == null) {
            instance = new AISettingsManager();
        }
        return instance;
    }

    public void addChangedListener(AISettingsEventListener listener) {
        this.settingsChangedListeners.add(listener);
    }

    public void removeChangedListener(AISettingsEventListener listener) {
        this.settingsChangedListeners.remove(listener);
    }

    private void raiseChangedEvent(AISettingsManager registry) {
        for (AISettingsEventListener listener : this.settingsChangedListeners.toArray(AISettingsEventListener[]::new)) {
            listener.onSettingsUpdate(registry);
        }
    }

    public boolean isConfigExists() throws DBException {
        return settingsWriter.isConfigExists();
    }

    @NotNull
    public AISettings getSettings() {
        return settingsHolder.updateAndGet(
            cachedSettings -> {
                if (cachedSettings != null) {
                    return cachedSettings;
                }

                AISettings aiSettings = settingsWriter.readSettings();
                AISettingsInitializerRegistry.getInstance()
                    .createInitializer()
                    .initializeDefaultSettings(aiSettings);

                saveSettings(aiSettings); // save back to persist any default settings

                return aiSettings;
            }
        );
    }

    public void saveSettings(AISettings settings) {
        try {
            settingsWriter.writeSettings(settings);
            settingsHolder.set(settings);
        } catch (Exception e) {
            log.error("Error saving AI settings", e);
        }
        raiseChangedEvent(this);
    }
}
