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
package org.jkiss.dbeaver.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.dbeaver.runtime.IPluginService;

import java.util.ArrayList;
import java.util.List;

public class PluginServiceRegistry
{
    private static final Log log = Log.getLog(PluginServiceRegistry.class);

    public static final String EXTENSION_ID = "org.jkiss.dbeaver.pluginService"; //$NON-NLS-1$

    private static PluginServiceRegistry instance = null;

    private class ServiceDescriptor extends AbstractDescriptor {

        private final ObjectType type;

        protected ServiceDescriptor(IConfigurationElement config) {
            super(config);
            type = new ObjectType(config.getAttribute(RegistryConstants.ATTR_CLASS));
        }
    }

    public synchronized static PluginServiceRegistry getInstance()
    {
        if (instance == null) {
            instance = new PluginServiceRegistry(Platform.getExtensionRegistry());
        }
        return instance;
    }

    private final List<IPluginService> services = new ArrayList<>();

    private PluginServiceRegistry(IExtensionRegistry registry)
    {
        IConfigurationElement[] extElements = registry.getConfigurationElementsFor(EXTENSION_ID);
        for (IConfigurationElement ext : extElements) {
            ServiceDescriptor serviceDescriptor = new ServiceDescriptor(ext);
            try {
                IPluginService pluginService = serviceDescriptor.type.createInstance(IPluginService.class);
                services.add(pluginService);
            } catch (DBException e) {
                log.error("Can't create plugin service", e);
            }
        }
    }

    public List<IPluginService> getServices()
    {
        return services;
    }
    
}
