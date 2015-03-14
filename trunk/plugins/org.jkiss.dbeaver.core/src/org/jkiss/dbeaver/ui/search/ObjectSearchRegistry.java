/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
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

    private final List<ObjectSearchProvider> providers = new ArrayList<ObjectSearchProvider>();

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
