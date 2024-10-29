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
package org.jkiss.dbeaver.model.dashboard.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.dashboard.DBDashboardContext;
import org.jkiss.dbeaver.model.dashboard.DBDashboardFolder;
import org.jkiss.dbeaver.model.dashboard.DBDashboardProvider;
import org.jkiss.dbeaver.model.dashboard.DashboardConstants;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Database dashboard context
 */
public class DatabaseDashboardProvider implements DBDashboardProvider {

    @NotNull
    @Override
    public String getId() {
        return DashboardConstants.DEF_DASHBOARD_PROVIDER;
    }

    @Override
    public List<DashboardItemConfiguration> loadStaticDashboards(@NotNull DashboardProviderDescriptor dp) {
        IExtensionRegistry registry = Platform.getExtensionRegistry();

        IConfigurationElement[] extElements = registry.getConfigurationElementsFor(DashboardItemConfiguration.EXTENSION_ID);

        List<DashboardItemConfiguration> dashboards = new ArrayList<>();
        Map<String, DashboardMapQueryDescriptor> mapQueries = new LinkedHashMap<>();

        // Load map queries
        for (IConfigurationElement ext : extElements) {
            if ("mapQuery".equals(ext.getName())) {
                DashboardMapQueryDescriptor query = new DashboardMapQueryDescriptor(ext);
                if (!CommonUtils.isEmpty(query.getId()) && !CommonUtils.isEmpty(query.getQueryText())) {
                    mapQueries.put(query.getId(), query);
                }
            }
        }
        // Load dashboards from extensions
        for (IConfigurationElement ext : extElements) {
            if ("dashboard".equals(ext.getName())) {
                DashboardItemConfiguration dashboard = new DashboardItemConfiguration(dp, mapQueries::get, ext);
                if (dashboard.isSupportedByLocalSystem()) {
                    dashboards.add(dashboard);
                }
            }
        }

        return dashboards;
    }

    @NotNull
    @Override
    public List<DBDashboardFolder> loadRootFolders(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DashboardProviderDescriptor provider,
        @NotNull DBDashboardContext context
    ) {
        return List.of();
    }

    @Override
    public boolean appliesTo(@NotNull DBPDataSourceContainer dataSource) {
        return true;
    }
}
