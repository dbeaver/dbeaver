/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.dashboard.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;

import java.util.ArrayList;
import java.util.List;

public class DashboardRegistry {
    private static DashboardRegistry instance = null;

    public synchronized static DashboardRegistry getInstance() {
        if (instance == null) {
            instance = new DashboardRegistry(Platform.getExtensionRegistry());
        }
        return instance;
    }

    private final List<DashboardDescriptor> dashboardList = new ArrayList<>();
    private final List<DashboardTypeDescriptor> dashboardTypeList = new ArrayList<>();

    private DashboardRegistry(IExtensionRegistry registry) {
        // Load data dashboardList from external plugins
        IConfigurationElement[] extElements = registry.getConfigurationElementsFor(DashboardDescriptor.EXTENSION_ID);
        // Load types
        for (IConfigurationElement ext : extElements) {
            if ("dashboardType".equals(ext.getName())) {
                dashboardTypeList.add(
                    new DashboardTypeDescriptor(ext));
            }
        }
        // Load dashboards
        for (IConfigurationElement ext : extElements) {
            if ("dashboard".equals(ext.getName())) {
                dashboardList.add(
                    new DashboardDescriptor(this, ext));
            }
        }
    }

    public DashboardTypeDescriptor getDashboardType(String id) {
        for (DashboardTypeDescriptor descriptor : dashboardTypeList) {
            if (descriptor.getId().equals(id)) {
                return descriptor;
            }
        }
        return null;
    }

    public List<DashboardDescriptor> getAllDashboards() {
        return dashboardList;
    }

    public DashboardDescriptor getDashboards(String id) {
        for (DashboardDescriptor descriptor : dashboardList) {
            if (descriptor.getId().equals(id)) {
                return descriptor;
            }
        }
        return null;
    }

    public List<DashboardDescriptor> getDashboards(DBPDataSourceContainer dataSourceContainer, boolean defaultOnly) {
        List<DashboardDescriptor> result = new ArrayList<>();
        for (DashboardDescriptor dd : dashboardList) {
            if (dd.matches(dataSourceContainer)) {
                if (!defaultOnly || dd.isShowByDefault()) {
                    result.add(dd);
                }
            }
        }
        return result;
    }

}
