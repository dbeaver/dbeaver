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

    private final List<DashboardDescriptor> descriptors = new ArrayList<>();

    private DashboardRegistry(IExtensionRegistry registry) {
        // Load data descriptors from external plugins
        {
            IConfigurationElement[] extElements = registry.getConfigurationElementsFor(DashboardDescriptor.EXTENSION_ID);
            for (IConfigurationElement ext : extElements) {
                DashboardDescriptor formatterDescriptor = new DashboardDescriptor(ext);
                descriptors.add(formatterDescriptor);
            }
        }
    }

    public List<DashboardDescriptor> getAllDashboards() {
        return descriptors;
    }

    public DashboardDescriptor getDashboard(String id) {
        for (DashboardDescriptor descriptor : descriptors) {
            if (descriptor.getId().equals(id)) {
                return descriptor;
            }
        }
        return null;
    }

    public List<DashboardDescriptor> getDashboard(DBPDataSourceContainer dataSourceContainer) {
        List<DashboardDescriptor> result = new ArrayList<>();
        for (DashboardDescriptor dd : descriptors) {
            if (dd.matches(dataSourceContainer)) {
                result.add(dd);
            }
        }
        return result;
    }

}
