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
package org.jkiss.dbeaver.ui.search;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;

import java.util.ArrayList;
import java.util.List;

public class ObjectSearchRegistry
{
    public static final String EXTENSION_ID = "org.jkiss.dbeaver.search"; //$NON-NLS-1$

    private static ObjectSearchRegistry instance = null;

    public synchronized static ObjectSearchRegistry getInstance()
    {
        if (instance == null) {
            instance = new ObjectSearchRegistry(Platform.getExtensionRegistry());
        }
        return instance;
    }

    private final List<ObjectSearchProvider> providers = new ArrayList<>();

    public ObjectSearchRegistry(IExtensionRegistry registry)
    {
        {
            IConfigurationElement[] extElements = registry.getConfigurationElementsFor(EXTENSION_ID);
            for (IConfigurationElement ext : extElements) {
                providers.add(new ObjectSearchProvider(ext));
            }
        }
    }

    public List<ObjectSearchProvider> getProviders()
    {
        return providers;
    }

}
