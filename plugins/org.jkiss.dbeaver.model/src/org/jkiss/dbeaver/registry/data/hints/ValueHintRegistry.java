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
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.data.hints.DBDValueHintContext;
import org.jkiss.dbeaver.model.data.hints.DBDValueHintProvider;
import org.jkiss.dbeaver.model.data.hints.standard.VoidHintProvider;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.virtual.DBVContainer;
import org.jkiss.dbeaver.model.virtual.DBVEntity;
import org.jkiss.dbeaver.model.virtual.DBVObject;
import org.jkiss.dbeaver.model.virtual.DBVUtils;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ValueHintRegistry
 */
public class ValueHintRegistry extends AbstractValueBindingRegistry<DBDValueHintProvider, DBDValueHintContext, ValueHintProviderDescriptor> {

    private static final Log log = Log.getLog(ValueHintRegistry.class);
    public static final String CONFIG_FILE_NAME = "data-hints.json";

    private static final Gson gson = new GsonBuilder().create();
    private static final String HINT_CONFIG_PROPERTY = "data.hints.configuration";

    private static ValueHintRegistry instance = null;

    public synchronized static ValueHintRegistry getInstance() {
        if (instance == null) {
            instance = new ValueHintRegistry(Platform.getExtensionRegistry());
        }
        return instance;
    }

    private final List<ValueHintProviderDescriptor> descriptors = new ArrayList<>();
    private final ValueHintContextConfiguration globalContextConfiguration;

    private ValueHintRegistry(IExtensionRegistry registry) {
        // Load datasource providers from external plugins
        IConfigurationElement[] extElements = registry.getConfigurationElementsFor(ValueHintProviderDescriptor.EXTENSION_ID);
        for (IConfigurationElement ext : extElements) {
            if (ValueHintProviderDescriptor.TAG_HINT_PROVIDER.equals(ext.getName())) {
                descriptors.add(new ValueHintProviderDescriptor(ext));
            }
        }

        this.globalContextConfiguration = new GlobalHintContextConfiguration();
    }

    public List<ValueHintProviderDescriptor> getHintDescriptors() {
        return getDescriptors();
    }

    public List<ValueHintProviderDescriptor> getHintDescriptors(@NotNull DBDValueHintProvider.HintObject forObject) {
        return getDescriptors().stream().filter(d -> d.getForObject() == forObject).toList();
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

    public ValueHintContextConfiguration getContextConfiguration(
        @Nullable DBPDataSourceContainer ds,
        @Nullable DBSEntity entity,
        boolean forceCreate
    ) {
        if (entity != null) {
            // Try virt model
            ValueHintContextConfiguration configuration = findHintConfigFromVirtualObject(
                DBVUtils.getVirtualEntity(entity, forceCreate),
                forceCreate);
            if (configuration != null) {
                return configuration;
            }
        }
        if (ds != null) {
            ValueHintContextConfiguration configuration = findHintConfigFromVirtualObject(
                ds.getVirtualModel(),
                forceCreate);
            if (configuration != null) {
                return configuration;
            }
        }

        // Fallback to global
        return globalContextConfiguration;
    }

    /**
     * Optimized check. It doesn't deserialize entire providers config but checks virtual model internal state.
     */
    public boolean isHintEnabled(
        @NotNull ValueHintProviderDescriptor descriptor,
        @Nullable DBPDataSourceContainer ds,
        @Nullable DBSEntity entity
    ) {
        Boolean isEnabled;
        if (entity != null) {
            isEnabled = isHintEnabledInVirtualObject(descriptor, DBVUtils.getVirtualEntity(entity, false));
            if (isEnabled != null) {
                return isEnabled;
            }
        }
        if (ds != null) {
            isEnabled = isHintEnabledInVirtualObject(descriptor, ds.getVirtualModel());
            if (isEnabled != null) {
                return isEnabled;
            }
        }

        // Fallback to global
        return globalContextConfiguration.isHintEnabled(descriptor);
    }

    private Boolean isHintEnabledInVirtualObject(ValueHintProviderDescriptor descriptor, DBVObject vObject) {
        if (vObject != null) {
            Map<String, Object> dataHintsConfig = vObject.getProperty(HINT_CONFIG_PROPERTY);
            if (dataHintsConfig != null) {
                Map<String, Object> provConfig = JSONUtils.getObjectOrNull(dataHintsConfig, descriptor.getId());
                if (provConfig != null) {
                    Object isEnabled = provConfig.get("enabled");
                    if (isEnabled != null) {
                        return CommonUtils.toBoolean(isEnabled);
                    }
                }
            }
        }
        return null;
    }

    private ValueHintContextConfiguration findHintConfigFromVirtualObject(@Nullable DBVObject vObject, boolean forceCreate) {
        if (vObject != null) {
            Map<String, Object> dataHintsConfig = vObject.getProperty(HINT_CONFIG_PROPERTY);
            if (dataHintsConfig != null || forceCreate) {
                return new VirtualHintContextConfiguration(
                    vObject,
                    vObject instanceof DBVEntity ? DBDValueHintContext.HintConfigurationLevel.ENTITY : DBDValueHintContext.HintConfigurationLevel.DATASOURCE);
            }
        }
        return null;
    }

    public ValueHintProviderDescriptor getDescriptorByInstance(DBDValueHintProvider provider) {
        for (ValueHintProviderDescriptor descriptor : descriptors) {
            if (descriptor.getInstance() == provider) {
                return descriptor;
            }
        }
        return null;
    }

    private static class GlobalHintContextConfiguration extends ValueHintContextConfiguration {
        public GlobalHintContextConfiguration() {
            super(DBDValueHintContext.HintConfigurationLevel.GLOBAL);

            try {
                String configContent = DBWorkbench.getPlatform()
                    .getConfigurationController()
                    .loadConfigurationFile(CONFIG_FILE_NAME);
                if (configContent != null) {
                    Map<String, ValueHintProviderConfiguration> configurationMap = gson.fromJson(
                        configContent,
                        new TypeToken<Map<String, ValueHintProviderConfiguration>>() {
                        }.getType());
                    if (configurationMap == null) {
                        // May happen if json deserializes to null
                        configurationMap = new LinkedHashMap<>();
                    }
                    this.setConfigurationMap(configurationMap);
                }
            } catch (Exception e) {
                log.error("Error loading hint providers configuration", e);
            }
        }

        @Override
        public ValueHintContextConfiguration getParent() {
            return null;
        }

        @Override
        public void saveConfiguration() {
            try {
                String json = gson.toJson(this.getConfigurationMap());
                DBWorkbench.getPlatform()
                    .getConfigurationController()
                    .saveConfigurationFile(CONFIG_FILE_NAME, json);
            } catch (DBException e) {
                log.error("Error saving hint providers configuration", e);
            }
        }

        @Override
        public void deleteConfiguration() {
            log.error("Global configuration cannot be deleted");
        }
    }

    private static class VirtualHintContextConfiguration extends ValueHintContextConfiguration {

        private final DBVObject vObject;

        public VirtualHintContextConfiguration(
            @NotNull DBVObject vObject,
            @NotNull DBDValueHintContext.HintConfigurationLevel level
        ) {
            super(level);
            this.vObject = vObject;

            try {
                Map<String, Object> dataHintsConfig = vObject.getProperty(HINT_CONFIG_PROPERTY);
                if (dataHintsConfig != null) {
                    for (Map.Entry<String, Object> pc : dataHintsConfig.entrySet()) {
                        if (pc.getValue() instanceof Map map) {
                            ValueHintProviderConfiguration configuration = JSONUtils.deserializeObject(map, ValueHintProviderConfiguration.class);
                            configurationMap.put(pc.getKey(), configuration);
                        }
                    }
                } else {
                    vObject.setProperty(HINT_CONFIG_PROPERTY, new LinkedHashMap<>());
                }
            } catch (Exception e) {
                log.debug("Error reading hints configuration", e);
            }
        }

        @Override
        public ValueHintContextConfiguration getParent() {
            if (vObject instanceof DBVContainer) {
                return getInstance().globalContextConfiguration;
            }
            return getInstance().getContextConfiguration(vObject.getDataSourceContainer(), null, false);
        }

        @Override
        public void saveConfiguration() {
            Map<String, Object> dataHintsConfig = new LinkedHashMap<>();
            for (Map.Entry<String, ValueHintProviderConfiguration> hpc : configurationMap.entrySet()) {
                Map<String, Object> hpMap = new LinkedHashMap<>();
                hpMap.put("enabled", hpc.getValue().isEnabled());
                hpMap.put("parameters", hpc.getValue().getParameters());
                dataHintsConfig.put(hpc.getKey(), hpMap);
            }
            vObject.setProperty(HINT_CONFIG_PROPERTY, dataHintsConfig);
            persistConfiguration();
        }

        @Override
        public void deleteConfiguration() {
            vObject.setProperty(HINT_CONFIG_PROPERTY, null);
            persistConfiguration();
        }

        private void persistConfiguration() {
            DBPDataSourceContainer dataSourceContainer = vObject.getDataSourceContainer();
            if (dataSourceContainer == null) {
                log.error("Error saving virtual config for hints: not datasource container");
            } else {
                dataSourceContainer.persistConfiguration();
            }
        }
    }
}
