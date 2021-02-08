/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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

    private final List<AggregateFunctionDescriptor> aggregateFunctions = new ArrayList<>();

    private FunctionsRegistry()
    {
    }

    private void loadExtensions(IExtensionRegistry registry)
    {
        {
            IConfigurationElement[] extConfigs = registry.getConfigurationElementsFor(AggregateFunctionDescriptor.EXTENSION_ID);
            for (IConfigurationElement ext : extConfigs) {
                // Load aggregateFunctions
                if (TAG_FUNCTION.equals(ext.getName())) {
                    this.aggregateFunctions.add(
                        new AggregateFunctionDescriptor(ext));
                }
            }
        }
    }

    public void dispose()
    {
        aggregateFunctions.clear();
    }

    public List<AggregateFunctionDescriptor> getAggregateFunctions() {
        return aggregateFunctions;
    }

    public AggregateFunctionDescriptor getFunction(String id) {
        for (AggregateFunctionDescriptor func : aggregateFunctions) {
            if (func.getId().equals(id)) {
                return func;
            }
        }
        return null;
    }

}
