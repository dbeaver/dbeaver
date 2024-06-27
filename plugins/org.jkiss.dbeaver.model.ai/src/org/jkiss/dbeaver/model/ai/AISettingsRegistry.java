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

import com.google.gson.Gson;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.WorkspaceConfigEventManager;
import org.jkiss.dbeaver.model.auth.SMSessionPersistent;
import org.jkiss.dbeaver.model.rm.RMConstants;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.CommonUtils;

import java.io.StringReader;
import java.util.*;
import java.util.function.Consumer;

public class AISettingsRegistry {
    private static final Log log = Log.getLog(AISettingsRegistry.class);

    public static final String AI_CONFIGURATION_JSON = "ai-configuration.json";

    private static AISettingsRegistry instance = null;

    private static final Gson gson = new Gson();

    private final Set<Consumer<AISettingsRegistry>> settingsChangedListeners = Collections.synchronizedSet(new HashSet<>());

    private interface AISettingsHolder {
        AISettings getSettings();
        void setSettings(AISettings mruSettings);
        void reset();
    }

    private static class AISettingsSessionHolder implements AISettingsHolder {
        private static final Map<SMSessionPersistent, AISettingsSessionHolder> holderBySession
            = Collections.synchronizedMap(new WeakHashMap<>());

        private final SMSessionPersistent session;

        private volatile AISettings mruSettings = null;
        private volatile boolean settingsReadInProgress = false;

        private AISettingsSessionHolder(SMSessionPersistent session) {
            this.session = session;
        }

        public static AISettingsHolder getForSession(SMSessionPersistent session) {
            return holderBySession.computeIfAbsent(session, s -> new AISettingsSessionHolder(s));
        }

        public static void resetAll() {
            holderBySession.clear();
        }

        @Override
        public synchronized AISettings getSettings() {
            AISettings mruSettings = this.mruSettings;
            AISettings sharedSettings = this.session.getAttribute(AISettings.class.getName());
            if (mruSettings == null || !mruSettings.equals(sharedSettings)) {
                if (settingsReadInProgress) {
                    // FIXME: it is a hack. Settings loading may cause infinite recursion because
                    // conf loading shows UI which may re-ask settings
                    // The fix is to disable UI during config read? But this lead to UI freeze..
                    return new AISettings();
                }
                settingsReadInProgress = true;
                try {
                    // if current context is not initialized or was invalidated, then reload settings for this session
                    this.setSettings(mruSettings = loadSettingsFromConfig());
                } finally {
                    settingsReadInProgress = false;
                }
            }
            return mruSettings;
        }

        @Override
        public synchronized void setSettings(AISettings mruSettings) {
            this.mruSettings = mruSettings;
            this.session.setAttribute(AISettings.class.getName(), mruSettings);
        }

        @Override
        public synchronized void reset() {
            // session contexts are not differentiated for now, so simply invalidate all of them
            resetAll();
        }
    }

    private static class AISettingsLocalHolder implements AISettingsHolder {
        public static final AISettingsHolder INSTANCE = new AISettingsLocalHolder();

        private AISettings settings = null;

        @Override
        public synchronized AISettings getSettings() {
            AISettings settings = this.settings;
            if (settings == null) {
                // if current context is not initialized or was invalidated, then reload settings
                this.settings = settings = loadSettingsFromConfig();
            }
            return settings;
        }

        @Override
        public synchronized void setSettings(AISettings mruSettings) {
            this.settings = mruSettings;
        }

        @Override
        public synchronized void reset() {
            this.settings = null;
        }
    }


    private AISettingsRegistry() {
        WorkspaceConfigEventManager.addConfigChangedListener(AI_CONFIGURATION_JSON, o -> {
            // reset current context for settings to be lazily reloaded when needed
            this.getSettingsHolder().reset();
            this.raiseChangedEvent(this); // consider detailed event info
        });
    }

    public static synchronized AISettingsRegistry getInstance() {
        if (instance == null) {
            instance = new AISettingsRegistry();
        }
        return instance;
    }

    public void addChangedListener(Consumer<AISettingsRegistry> listener) {
        this.settingsChangedListeners.add(listener);
    }

    public void removeChangedListener(Consumer<AISettingsRegistry> listener) {
        this.settingsChangedListeners.remove(listener);
    }

    private void raiseChangedEvent(AISettingsRegistry registry) {
        for (Consumer<AISettingsRegistry> listener : this.settingsChangedListeners.toArray(Consumer[]::new)) {
            listener.accept(registry);
        }
    }

    private AISettingsHolder getSettingsHolder() {
        if (DBWorkbench.getPlatform().getWorkspace().getWorkspaceSession() instanceof SMSessionPersistent session) {
            return AISettingsSessionHolder.getForSession(session);
        } else {
            return AISettingsLocalHolder.INSTANCE;
        }
    }

    @NotNull
    public AISettings getSettings() {
        return this.getSettingsHolder().getSettings();
    }

    @NotNull
    private static AISettings loadSettingsFromConfig() {
        AISettings settings;
        try {
            String content = loadConfig();
            if (CommonUtils.isEmpty(content)) {
                settings = prepareDefaultSettings();
            } else {
                settings = gson.fromJson(new StringReader(content), AISettings.class);
            }
        } catch (Exception e) {
            log.error("Error loading AI settings, falling back to defaults.", e);
            settings = prepareDefaultSettings();
        }

        if (settings.getActiveEngine() == null) {
            settings.setActiveEngine(AIConstants.OPENAI_ENGINE);
        }

        return settings;
    }

    private static AISettings prepareDefaultSettings() {
        AISettings settings = new AISettings();
        if (DBWorkbench.getPlatform().getPreferenceStore().getString(AICompletionConstants.AI_DISABLED) != null) {
            settings.setAiDisabled(DBWorkbench.getPlatform().getPreferenceStore().getBoolean(AICompletionConstants.AI_DISABLED));
        } else {
            // Enable AI by default
            settings.setAiDisabled(false);
        }
        return settings;
    }

    public void saveSettings(AISettings settings) {
        try {
            if (!DBWorkbench.getPlatform().getWorkspace().hasRealmPermission(RMConstants.PERMISSION_CONFIGURATION_MANAGER)) {
                log.warn("The user has no permission to save AI configuration");
                return;
            }
            String content = gson.toJson(settings, AISettings.class);
            DBWorkbench.getPlatform().getConfigurationController().saveConfigurationFile(AI_CONFIGURATION_JSON, content);
            this.getSettingsHolder().setSettings(settings);
        } catch (Exception e) {
            log.error("Error saving AI configuration", e);
        }
        raiseChangedEvent(this);
    }

    private static String loadConfig() throws DBException {
        return DBWorkbench.getPlatform()
            .getConfigurationController()
            .loadConfigurationFile(AI_CONFIGURATION_JSON);
    }

    public static boolean isConfigExists() throws DBException {
        String content = loadConfig();
        return CommonUtils.isNotEmpty(content);
    }
}
