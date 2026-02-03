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
package org.jkiss.dbeaver.registry;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPDataSourceContainerProvider;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.impl.preferences.AbstractPreferenceStore;
import org.jkiss.dbeaver.model.impl.preferences.SimplePreferenceStore;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.security.SMController;
import org.jkiss.dbeaver.model.security.SMControllerProvider;
import org.jkiss.dbeaver.model.security.SMObjectSettings;
import org.jkiss.dbeaver.model.security.SMObjectType;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * DataSourcePreferenceStore
 */
public class DataSourcePreferenceStore extends SimplePreferenceStore implements DBPDataSourceContainerProvider
{
    private static final Log log = Log.getLog(DataSourcePreferenceStore.class);
    private final DataSourceDescriptor dataSourceDescriptor;

    DataSourcePreferenceStore(
        @NotNull DBPPreferenceStore parentStore,
        @NotNull DataSourceDescriptor dataSourceDescriptor
    ) {
        super(parentStore);
        this.dataSourceDescriptor = dataSourceDescriptor;
        // Init default properties from driver overrides
        Map<String, Object> defaultConnectionProperties = dataSourceDescriptor.getDriver()
            .getDefaultConnectionProperties();
        fetchSettingFromSettingProvider();
        for (Map.Entry<String, Object> prop : defaultConnectionProperties.entrySet()) {
            String propName = prop.getKey();
            if (propName.startsWith(DBConstants.DEFAULT_DRIVER_PROP_PREFIX)) {
                getDefaultProperties().put(
                    propName.substring(DBConstants.DEFAULT_DRIVER_PROP_PREFIX.length()),
                    CommonUtils.toString(prop.getValue())
                );
            }
        }
    }

    DataSourcePreferenceStore(DataSourceDescriptor dataSourceDescriptor) {
        this(dataSourceDescriptor.getRegistry().getPreferenceStore(), dataSourceDescriptor);
    }

    @Override
    public void save()
        throws IOException
    {
        dataSourceDescriptor.getRegistry().flushConfig();
    }

    @Override
    public Map<String, String> getProperties() {
        fetchSettingFromSettingProvider();
        return super.getProperties();
    }

    private void fetchSettingFromSettingProvider() {
        DBPDataSourceRegistry registry = dataSourceDescriptor.getRegistry();
        Map<String, String> properties = super.getProperties();
        if (registry instanceof SMControllerProvider smControllerProvider) {
            SMObjectType objectType = SMObjectType.datasource;
            try {
                SMController smController = smControllerProvider.getSecurityController();
                if (smController == null) {
                    return;
                }
                List<SMObjectSettings> objectSettings = smController
                    .getObjectSettings(dataSourceDescriptor.getProjectId(), objectType, dataSourceDescriptor.getId(), null);
                objectSettings.forEach(setting -> properties.putAll(setting.settings()));
            } catch (DBException e) {
                log.warn(
                    "Error fetching datasource settings from security controller for datasource '"
                        + dataSourceDescriptor.getName() + "'", e
                );
            }
        }
    }

    @Nullable
    @Override
    public DBPDataSourceContainer getDataSourceContainer() {
        return dataSourceDescriptor;
    }

    @Override
    public void firePropertyChangeEvent(@NotNull String name, @Nullable Object oldValue, @Nullable Object newValue) {
        super.firePropertyChangeEvent(name, oldValue, newValue);

        // Forward event to global DS prefs store
        DBPPreferenceStore gps = DBWorkbench.getPlatform().getDataSourceProviderRegistry().getGlobalDataSourcePreferenceStore();
        if (gps instanceof AbstractPreferenceStore) {
            ((AbstractPreferenceStore) gps).firePropertyChangeEvent(this, name, oldValue, newValue);
        }
    }
}
