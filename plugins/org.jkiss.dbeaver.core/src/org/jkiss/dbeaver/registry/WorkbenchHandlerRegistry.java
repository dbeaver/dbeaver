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
package org.jkiss.dbeaver.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.dbeaver.runtime.IPluginService;
import org.jkiss.dbeaver.ui.IWorkbenchWindowInitializer;

import java.util.ArrayList;
import java.util.List;

public class WorkbenchHandlerRegistry
{
    private static final Log log = Log.getLog(WorkbenchHandlerRegistry.class);

    public static final String EXTENSION_ID = "org.jkiss.dbeaver.workbenchHandler"; //$NON-NLS-1$
    public static final String WORKBENCH_WINDOW_INITIALIZER = "workbenchWindowInitializer";

    private static WorkbenchHandlerRegistry instance = null;

    private class HandlerDescriptor extends AbstractDescriptor {

        private final ObjectType type;

        protected HandlerDescriptor(IConfigurationElement config) {
            super(config);
            type = new ObjectType(config.getAttribute(RegistryConstants.ATTR_CLASS));
        }
    }

    public synchronized static WorkbenchHandlerRegistry getInstance()
    {
        if (instance == null) {
            instance = new WorkbenchHandlerRegistry(Platform.getExtensionRegistry());
        }
        return instance;
    }

    private final List<IWorkbenchWindowInitializer> wwInitializers = new ArrayList<>();

    private WorkbenchHandlerRegistry(IExtensionRegistry registry)
    {
        IConfigurationElement[] extElements = registry.getConfigurationElementsFor(EXTENSION_ID);
        for (IConfigurationElement ext : extElements) {
            if (ext.getName().equals(WORKBENCH_WINDOW_INITIALIZER)) {
                HandlerDescriptor handlerDescriptor = new HandlerDescriptor(ext);
                try {
                    IWorkbenchWindowInitializer wwInit = handlerDescriptor.type.createInstance(IWorkbenchWindowInitializer.class);
                    wwInitializers.add(wwInit);
                } catch (DBException e) {
                    log.error("Can't create workbench window initializer", e);
                }
            }
        }
    }

    public List<IWorkbenchWindowInitializer> getWorkbenchWindowInitializers()
    {
        return wwInitializers;
    }
    
}
