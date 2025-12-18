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
package org.jkiss.dbeaver.registry.settings;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPObjectSettingsListener;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNUtils;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.registry.DataSourceNavigatorSettingsUtils;

public class DataSourceNavigatorSettingsListener implements DBPObjectSettingsListener {
    private static final Log log = Log.getLog(DataSourceNavigatorSettingsListener.class);

    @NotNull
    private final DBPDataSourceRegistry registry;

    public DataSourceNavigatorSettingsListener(@NotNull DBPDataSourceRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void objectSettingUpdated(@NotNull String objectId, @NotNull String settingId) {
        DBPDataSourceContainer dataSourceContainer = registry.getDataSource(objectId);
        if (dataSourceContainer == null) {
            log.warn("Data source container '" + objectId + "' not found in registry");
            return;
        }
        if (settingId.equals(DataSourceNavigatorSettingsUtils.PARAM_ID_NAVIGATOR_SETTINGS)) {
            DataSourceNavigatorSettingsUtils.setCustomNavigatorSettings(dataSourceContainer);
        } else {
            log.debug("Unsupported data source setting change: " + settingId);
        }
        // Refresh data source
        DBNNode node = DBNUtils.getNodeByObject(dataSourceContainer);
        if (node != null) {
            try {
                node.refreshNode(new VoidProgressMonitor(), this);
            } catch (DBException e) {
                log.warn("Error refreshing data source settings", e);
            }
        }

    }
}
