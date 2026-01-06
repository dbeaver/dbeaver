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
package org.jkiss.dbeaver.registry.data.hints;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.data.hints.DBDValueHintContext;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ValueHintProviderConfiguration
 */
public abstract class ValueHintContextConfiguration {

    private final transient DBDValueHintContext.HintConfigurationLevel level;
    protected final Map<String, ValueHintProviderConfiguration> configurationMap = new LinkedHashMap<>();

    public ValueHintContextConfiguration(
        @NotNull DBDValueHintContext.HintConfigurationLevel level
    ) {
        this.level = level;
    }

    public abstract ValueHintContextConfiguration getParent();

    public DBDValueHintContext.HintConfigurationLevel getLevel() {
        return level;
    }

    public Map<String, ValueHintProviderConfiguration> getConfigurationMap() {
        return configurationMap;
    }

    public void setConfigurationMap(Map<String, ValueHintProviderConfiguration> configurationMap) {
        this.configurationMap.clear();
        this.configurationMap.putAll(configurationMap);
    }

    @NotNull
    public ValueHintProviderConfiguration getProviderConfiguration(@NotNull ValueHintProviderDescriptor descriptor) {
        ValueHintProviderConfiguration configuration = configurationMap.get(descriptor.getId());
        if (configuration == null) {
            configuration = new ValueHintProviderConfiguration(descriptor.getId());
            ValueHintContextConfiguration parent = getParent();
            if (parent == null) {
                configuration.setEnabled(descriptor.isVisibleByDefault());
            } else {
                ValueHintProviderConfiguration pConfig = parent.getProviderConfiguration(descriptor);
                configuration.setEnabled(pConfig.isEnabled());
                configuration.setParameters(pConfig.getParameters());
            }
            configurationMap.put(descriptor.getId(), configuration);
        }
        return configuration;
    }

    public void setConfiguration(
        @NotNull ValueHintProviderDescriptor descriptor,
        @Nullable ValueHintProviderConfiguration configuration
    ) {
        if (configuration == null) {
            configurationMap.remove(descriptor.getId());
        } else {
            configurationMap.put(descriptor.getId(), configuration);
        }
    }

    public boolean isHintEnabled(ValueHintProviderDescriptor descriptor) {
        return getProviderConfiguration(descriptor).isEnabled();
    }

    public abstract void saveConfiguration();

    public abstract void deleteConfiguration();
}