/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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

package org.jkiss.dbeaver.debug.core;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.debug.DBGControllerRegistry;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.ui.data.IStreamValueManager;
import org.jkiss.dbeaver.ui.data.IValueManager;
import org.jkiss.dbeaver.ui.data.managers.DefaultValueManager;
import org.jkiss.dbeaver.ui.data.registry.StreamValueManagerDescriptor;
import org.jkiss.dbeaver.ui.data.registry.ValueManagerDescriptor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * EntityEditorsRegistry
 */
public class DebugControllersRegistry {

    private static DebugControllersRegistry instance = null;

    public synchronized static DebugControllersRegistry getInstance() {
        if (instance == null) {
            instance = new DebugControllersRegistry(Platform.getExtensionRegistry());
        }
        return instance;
    }

    private List<DebugControllerDescriptor> controllers = new ArrayList<>();

    private DebugControllersRegistry(IExtensionRegistry registry) {
        // Load datasource providers from external plugins
        IConfigurationElement[] extElements = registry.getConfigurationElementsFor(DebugControllerDescriptor.EXTENSION_ID);
        for (IConfigurationElement ext : extElements) {
            if (DebugControllerDescriptor.TAG_CONTROLLER.equals(ext.getName())) {
                controllers.add(new DebugControllerDescriptor(ext));
            }
        }
    }

    @NotNull
    public DBGControllerRegistry createControllerRegistry(@NotNull DBPDataSourceContainer dataSource) {
        for (DebugControllerDescriptor controllerDescriptor : controllers) {
            if (controllerDescriptor.getDataSourceID().equals(dataSource.getDriver().getProviderId())) {
                return controllerDescriptor.getInstance();
            }
        }
        return null;
    }

}
