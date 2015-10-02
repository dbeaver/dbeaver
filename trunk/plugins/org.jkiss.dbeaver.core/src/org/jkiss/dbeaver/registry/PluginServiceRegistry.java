/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
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
    static final Log log = Log.getLog(PluginServiceRegistry.class);

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
