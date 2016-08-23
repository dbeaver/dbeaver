/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.registry.functions;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;

import java.util.ArrayList;
import java.util.List;

public class FunctionsRegistry
{

    static final String TAG_FUNCTION = "function"; //$NON-NLS-1$

    private static FunctionsRegistry instance = null;

    public synchronized static FunctionsRegistry getInstance()
    {
        if (instance == null) {
            instance = new FunctionsRegistry();
            instance.loadExtensions(Platform.getExtensionRegistry());
        }
        return instance;
    }

    private final List<AggregateFunctionDescriptor> functions = new ArrayList<>();

    private FunctionsRegistry()
    {
    }

    private void loadExtensions(IExtensionRegistry registry)
    {
        IConfigurationElement[] extConfigs = registry.getConfigurationElementsFor(AggregateFunctionDescriptor.EXTENSION_ID);
        for (IConfigurationElement ext : extConfigs) {
            // Load functions
            if (TAG_FUNCTION.equals(ext.getName())) {
                this.functions.add(
                    new AggregateFunctionDescriptor(ext));
            }
        }
    }

    public void dispose()
    {
        functions.clear();
    }

    public List<AggregateFunctionDescriptor> getFunctions() {
        return functions;
    }

    public AggregateFunctionDescriptor getFunction(String id) {
        for (AggregateFunctionDescriptor func : functions) {
            if (func.getId().equals(id)) {
                return func;
            }
        }
        return null;
    }

}
