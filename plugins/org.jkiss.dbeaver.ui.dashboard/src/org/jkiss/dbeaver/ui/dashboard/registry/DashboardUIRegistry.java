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
package org.jkiss.dbeaver.ui.dashboard.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.dashboard.DashboardDataType;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardViewType;
import org.jkiss.utils.ArrayUtils;

import java.util.ArrayList;
import java.util.List;

public class DashboardUIRegistry {
    private static final Log log = Log.getLog(DashboardUIRegistry.class);
    
    private static DashboardUIRegistry instance = null;

    private final Object syncRoot = new Object();

    public synchronized static DashboardUIRegistry getInstance() {
        if (instance == null) {
            instance = new DashboardUIRegistry(Platform.getExtensionRegistry());
        }
        return instance;
    }

    private final List<DashboardViewTypeDescriptor> viewTypeList = new ArrayList<>();

    private DashboardUIRegistry(IExtensionRegistry registry) {
        // Load data dashboardList from external plugins
        IConfigurationElement[] extElements = registry.getConfigurationElementsFor(DashboardViewTypeDescriptor.EXTENSION_ID);
        // Load view types
        for (IConfigurationElement ext : extElements) {
            if ("dashboardView".equals(ext.getName())) {
                viewTypeList.add(
                    new DashboardViewTypeDescriptor(ext));
            }
        }
    }


    public DashboardViewTypeDescriptor getViewType(String id) {
        for (DashboardViewTypeDescriptor descriptor : viewTypeList) {
            if (descriptor.getId().equals(id)) {
                return descriptor;
            }
        }
        return null;
    }


    public List<DashboardViewType> getAllViewTypes() {
        return new ArrayList<>(viewTypeList);
    }

    public List<DashboardViewType> getSupportedViewTypes(DashboardDataType dataType) {
        List<DashboardViewType> result = new ArrayList<>();
        for (DashboardViewType vt : viewTypeList) {
            if (ArrayUtils.contains(vt.getSupportedTypes(), dataType)) {
                result.add(vt);
            }
        }
        return result;
    }

}
