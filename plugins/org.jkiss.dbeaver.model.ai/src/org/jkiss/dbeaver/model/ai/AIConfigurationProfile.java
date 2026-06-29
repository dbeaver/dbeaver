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

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.ai.engine.AIEngineProperties;
import org.jkiss.dbeaver.model.ai.registry.AIEngineDescriptor;
import org.jkiss.dbeaver.model.ai.registry.AIEngineRegistry;
import org.jkiss.dbeaver.model.ai.registry.AISettingsManager;

import java.util.HashSet;
import java.util.Set;

/**
 * AI engine settings profile
 * Keeps global parameters and configuration of an AI engine
 */
public class AIConfigurationProfile {
    protected static final Log log = Log.getLog(AIConfigurationProfile.class);

    private String profileId;
    private String profileName;
    private String engineId;
    private AIEngineProperties configuration;

    private final Set<String> resolvedSecrets = new HashSet<>();
    private transient AIEngineDescriptor engineDescriptor;

    public AIConfigurationProfile() {
    }

    @NotNull
    public String getEngineId() {
        return engineId;
    }

    @NotNull
    public AIEngineDescriptor getEngineDescriptor() throws DBException {
        if (engineDescriptor == null) {
            engineDescriptor = AIEngineRegistry.getInstance().getEngineDescriptor(engineId);
            if (engineDescriptor == null) {
                throw new DBException("AI engine " + engineId + " not found");
            }
        }
        return engineDescriptor;
    }

    @NotNull
    public synchronized AIEngineProperties getConfiguration() throws DBException {
        if (configuration != null) {
            return configuration;
        }
        AIEngineDescriptor engineDescriptor = getEngineDescriptor();
        configuration = engineDescriptor.createPropertiesInstance();

        if (!AISettingsManager.saveSecretsAsPlainText()) {
            if (!resolvedSecrets.contains(engineId)) {
                configuration.resolveSecrets();
                resolvedSecrets.add(engineId);
            }
        }

        return configuration;
    }

    public void saveSecrets() throws DBException {
        if (configuration != null) {
            configuration.saveSecrets();
        }
    }

}
