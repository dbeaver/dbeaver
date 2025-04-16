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

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.ai.AIEngineConfiguration;

import java.util.Map;

public class CopilotConfiguration implements AIEngineConfiguration {
    private boolean engineEnabled;
    @NotNull
    private CopilotProperties properties = new CopilotProperties();

    public boolean isEngineEnabled() {
        return engineEnabled;
    }

    public void setEngineEnabled(boolean engineEnabled) {
        this.engineEnabled = engineEnabled;
    }

    @NotNull
    public CopilotProperties getProperties() {
        return properties;
    }

    public void setProperties(@NotNull CopilotProperties properties) {
        this.properties = properties;
    }

    @Override
    public void resolveSecrets() throws DBException {
        properties.resolveSecrets();
    }

    @Override
    public void saveSecrets() throws DBException {
        properties.saveSecrets();
    }

    @Override
    public Map<String, Object> toMap() {
        return Map.of();
    }
}
