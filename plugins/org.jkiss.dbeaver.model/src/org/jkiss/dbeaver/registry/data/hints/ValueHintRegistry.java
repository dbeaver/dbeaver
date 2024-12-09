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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.data.hints.DBDValueHintProvider;
import org.jkiss.dbeaver.model.data.hints.standard.VoidHintProvider;
import org.jkiss.dbeaver.runtime.DBWorkbench;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ValueHintRegistry
 */
public class ValueHintRegistry extends AbstractValueBindingRegistry<DBDValueHintProvider, ValueHintProviderDescriptor> {

    private static final Log log = Log.getLog(ValueHintRegistry.class);
    public static final String CONFIG_FILE_NAME = "data-hints.json";

    private static final Gson gson = new GsonBuilder().create();

    private static ValueHintRegistry instance = null;

    public synchronized static ValueHintRegistry getInstance() {
        if (instance == null) {
            instance = new ValueHintRegistry(Platform.getExtensionRegistry());
        }
        return instance;
    }

    private final List<ValueHintProviderDescriptor> descriptors = new ArrayList<>();
    private Map<String, ValueHintProviderConfiguration> configurationMap = new LinkedHashMap<>();

    private ValueHintRegistry(IExtensionRegistry registry) {
        // Load datasource providers from external plugins
        IConfigurationElement[] extElements = registry.getConfigurationElementsFor(ValueHintProviderDescriptor.EXTENSION_ID);
        for (IConfigurationElement ext : extElements) {
            if (ValueHintProviderDescriptor.TAG_HINT_PROVIDER.equals(ext.getName())) {
                descriptors.add(new ValueHintProviderDescriptor(ext));
            }
        }

        loadConfiguration();
    }

    public List<ValueHintProviderDescriptor> getHintDescriptors() {
        return getDescriptors();
    }

    @NotNull
    @Override
    protected List<ValueHintProviderDescriptor> getDescriptors() {
        return descriptors;
    }

    @NotNull
    @Override
    protected DBDValueHintProvider getDefaultValueBinding() {
        return VoidHintProvider.INSTANCE;
    }

    @NotNull
    public ValueHintProviderConfiguration getConfiguration(ValueHintProviderDescriptor descriptor) {
        ValueHintProviderConfiguration configuration = configurationMap.get(descriptor.getId());
        if (configuration == null) {
            configuration = new ValueHintProviderConfiguration(descriptor.getId());
            configuration.setEnabled(descriptor.isVisibleByDefault());
            return configuration;
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

    public void saveConfiguration() {
        try {
            String json = gson.toJson(configurationMap);
            DBWorkbench.getPlatform()
                .getConfigurationController()
                .saveConfigurationFile(CONFIG_FILE_NAME, json);
        } catch (DBException e) {
            log.error("Error saving hint providers configuration", e);
        }
    }

    private void loadConfiguration() {
        try {
            String configContent = DBWorkbench.getPlatform()
                .getConfigurationController()
                .loadConfigurationFile(CONFIG_FILE_NAME);
            if (configContent != null) {
                configurationMap = gson.fromJson(
                    configContent,
                    new TypeToken<Map<String, ValueHintProviderConfiguration>>() {}.getType());
            }
        } catch (Exception e) {
            log.error("Error loading hint providers configuration", e);
        }
    }

    public boolean isHintEnabled(ValueHintProviderDescriptor descriptor) {
        ValueHintProviderConfiguration configuration = configurationMap.get(descriptor.getId());
        return configuration == null || configuration.isEnabled();
    }
}
