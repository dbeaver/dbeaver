/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.debug.ui.internal;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;

import java.util.ArrayList;
import java.util.List;

public class DebugConfigurationPanelRegistry
{
    private static DebugConfigurationPanelRegistry instance = null;

    public synchronized static DebugConfigurationPanelRegistry getInstance()
    {
        if (instance == null) {
            instance = new DebugConfigurationPanelRegistry(Platform.getExtensionRegistry());
        }
        return instance;
    }

    private final List<DebugConfigurationPanelDescriptor> descriptors = new ArrayList<>();

    private DebugConfigurationPanelRegistry(IExtensionRegistry registry)
    {
        // Load data descriptors from external plugins
        {
            IConfigurationElement[] extElements = registry.getConfigurationElementsFor(DebugConfigurationPanelDescriptor.EXTENSION_ID);
            for (IConfigurationElement ext : extElements) {
                DebugConfigurationPanelDescriptor formatterDescriptor = new DebugConfigurationPanelDescriptor(ext);
                descriptors.add(formatterDescriptor);
            }
        }
    }

    public List<DebugConfigurationPanelDescriptor> getPanels()
    {
        return descriptors;
    }

    public List<DebugConfigurationPanelDescriptor> getPanels(DBPDataSourceContainer dataSource) {
        List<DebugConfigurationPanelDescriptor> result = new ArrayList<>();
        for (DebugConfigurationPanelDescriptor desc : descriptors) {
            if (desc.supportsDataSource(dataSource)) {
                result.add(desc);
            }
        }
        return result;
    }

    public DebugConfigurationPanelDescriptor getPanel(String id)
    {
        for (DebugConfigurationPanelDescriptor descriptor : descriptors) {
            if (descriptor.getId().equals(id)) {
                return descriptor;
            }
        }
        return null;
    }

}
