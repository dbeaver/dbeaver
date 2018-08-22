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

import java.util.ArrayList;
import java.util.List;

public class DataSourceViewRegistry
{

    public static final String EXTENSION_ID = "org.jkiss.dbeaver.dataSourceView"; //$NON-NLS-1$

    private static DataSourceViewRegistry instance = null;

    private final List<DataSourceViewDescriptor> views = new ArrayList<>();

    public synchronized static DataSourceViewRegistry getInstance()
    {
        if (instance == null) {
            instance = new DataSourceViewRegistry(Platform.getExtensionRegistry());
        }
        return instance;
    }

    private DataSourceViewRegistry(IExtensionRegistry registry)
    {
        IConfigurationElement[] extElements = registry.getConfigurationElementsFor(EXTENSION_ID);
        for (IConfigurationElement viewElement : extElements) {
            if (viewElement.getName().equals(RegistryConstants.TAG_VIEW)) {
                this.views.add(
                    new DataSourceViewDescriptor(viewElement));
            }
        }
    }

    public DataSourceViewDescriptor findView(DataSourceProviderDescriptor provider, String targetID)
    {
        for (DataSourceProviderDescriptor pd = provider; pd != null; pd = pd.getParentProvider()) {
            for (DataSourceViewDescriptor view : views) {
                if (pd.getId().equals(view.getDataSourceId()) && targetID.equals(view.getTargetID())) {
                    return view;
                }
            }
        }
        return null;
    }
    
}
