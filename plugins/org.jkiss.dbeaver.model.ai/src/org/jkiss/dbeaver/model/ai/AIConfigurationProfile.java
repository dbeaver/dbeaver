/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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

import com.google.gson.annotations.JsonAdapter;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.ai.engine.AIEngineProperties;
import org.jkiss.dbeaver.model.ai.registry.AIEngineDescriptor;
import org.jkiss.dbeaver.model.ai.registry.AIEngineRegistry;
import org.jkiss.dbeaver.model.ai.registry.AISettingsManager;

/**
 * AI engine settings profile
 * Keeps global parameters and configuration of an AI engine
 */
@JsonAdapter(AISettingsManager.ConfigProfileAdapter.class)
public class AIConfigurationProfile {
    protected static final Log log = Log.getLog(AIConfigurationProfile.class);

    private boolean migrated;
    private String profileId;
    private String profileName;
    private String engineId;
    private AIEngineProperties configuration;

    private transient AIEngineDescriptor engineDescriptor;

    public AIConfigurationProfile() {
    }

    @NotNull
    public String getProfileId() {
        return profileId;
    }

    public void setProfileId(@NotNull String profileId) {
        this.profileId = profileId;
    }

    @NotNull
    public String getProfileName() {
        return profileName;
    }

    public void setProfileName(@NotNull String profileName) {
        this.profileName = profileName;
    }

    @NotNull
    public String getEngineId() {
        return engineId;
    }

    public void setEngineId(@NotNull String engineId) {
        this.engineId = engineId;
    }

    // Legacy configuration
    public boolean isMigrated() {
        return migrated;
    }

    public void setMigrated(boolean migrated) {
        this.migrated = migrated;
    }

    @NotNull
    public synchronized AIEngineDescriptor getEngineDescriptor() throws DBException {
        if (engineDescriptor == null) {
            engineDescriptor = AIEngineRegistry.getInstance().getEngineDescriptor(engineId);
            if (engineDescriptor == null) {
                throw new DBException("AI engine " + engineId + " not found");
            }
        }
        return engineDescriptor;
    }

    @NotNull
    public AIEngineProperties getConfiguration() throws DBException {
        AIEngineDescriptor engineDescriptor = getEngineDescriptor();
        synchronized (this) {
            if (configuration != null) {
                return configuration;
            }
            configuration = engineDescriptor.createPropertiesInstance();
        }

        return configuration;
    }

    public void setConfiguration(@NotNull AIEngineProperties configuration) {
        this.configuration = configuration;
    }

    public void saveSecrets() throws DBException {
        if (configuration != null) {
            configuration.saveSecrets(this);
        }
    }

    public void resolveSecrets() throws DBException {
        getConfiguration().resolveSecrets(this);
    }

    @Override
    public String toString() {
        return profileId + "(" + engineId + ")";
    }
}
