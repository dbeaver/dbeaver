/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.registry.datatype;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.data.DBDAttributeTransformerDescriptor;
import org.jkiss.dbeaver.model.data.DBDRegistry;
import org.jkiss.dbeaver.model.data.DBDValueHandlerProvider;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.registry.DataSourceProviderDescriptor;

import java.util.*;

/**
 * DataTypeProviderRegistry
 */
public class DataTypeProviderRegistry implements DBDRegistry
{
    public static final String EXTENSION_ID = "org.jkiss.dbeaver.dataTypeProvider"; //$NON-NLS-1$

    private static DataTypeProviderRegistry instance = null;

    public synchronized static DataTypeProviderRegistry getInstance()
    {
        if (instance == null) {
            instance = new DataTypeProviderRegistry();
            instance.loadExtensions(Platform.getExtensionRegistry());
        }
        return instance;
    }

    private final List<ValueHandlerDescriptor> dataTypeProviders = new ArrayList<>();
    private final Map<String, ValueHandlerDescriptor> dataTypeProvidersMap = new HashMap<>();
    private final List<AttributeTransformerDescriptor> dataTypeTransformers = new ArrayList<>();

    private DataTypeProviderRegistry()
    {
    }

    public void loadExtensions(IExtensionRegistry registry)
    {
        // Load data type providers from external plugins
        {
            IConfigurationElement[] extElements = registry.getConfigurationElementsFor(EXTENSION_ID);
            for (IConfigurationElement ext : extElements) {
                if ("provider".equals(ext.getName())) {
                    ValueHandlerDescriptor provider = new ValueHandlerDescriptor(ext);
                    dataTypeProviders.add(provider);
                    dataTypeProvidersMap.put(provider.getId(), provider);
                } else if ("transformer".equals(ext.getName())) {
                    dataTypeTransformers.add(new AttributeTransformerDescriptor(ext));
                }
            }
        }
        // Order providers. More precise come first
        dataTypeProviders.sort((o1, o2) -> {
            if (o1.isGlobal()) {
                return o2.isGlobal() ? -1 : 0;
            } else if (o2.isGlobal()) {
                return 1;
            }
            String p1 = o1.getParentProvider();
            String p2 = o2.getParentProvider();
            if (p1 == null) {
                return p2 == null ? 0 : 1;
            } else if (p2 == null) {
                return -1;
            }

            ValueHandlerDescriptor parent1 = getValueHandler(p1);
            ValueHandlerDescriptor parent2 = getValueHandler(p2);
            if (parent1.isChildOf(parent2)) {
                return -1;
            } else if (parent2.isChildOf(parent1)) {
                return 1;
            }
            return 0;
        });
    }

    private static boolean isChildOfAnyProvider(List<DataSourceProviderDescriptor> dsList1, List<DataSourceProviderDescriptor> dsList2) {
        for (DataSourceProviderDescriptor ds1 : dsList1) {
            for (DataSourceProviderDescriptor ds2 : dsList2) {
                for (DataSourceProviderDescriptor parent = ds2.getParentProvider(); parent != null; parent = parent.getParentProvider()) {
                    if (parent == ds1) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public void dispose()
    {
        this.dataTypeProviders.clear();
    }

    ////////////////////////////////////////////////////
    // DataType providers

    ValueHandlerDescriptor getValueHandler(@NotNull String id) {
        return dataTypeProvidersMap.get(id);
    }

    @Nullable
    public DBDValueHandlerProvider getValueHandlerProvider(@NotNull DBPDataSource dataSource, @NotNull DBSTypedObject typedObject)
    {
        // First try to find type provider for specific datasource type
        for (ValueHandlerDescriptor dtProvider : dataTypeProviders) {
            if (!dtProvider.isGlobal() && dtProvider.supportsDataSource(dataSource) && dtProvider.supportsType(typedObject)) {
                return dtProvider.getInstance();
            }
        }

        // Find in global providers
        for (ValueHandlerDescriptor dtProvider : dataTypeProviders) {
            if (dtProvider.isGlobal() && dtProvider.supportsType(typedObject)) {
                return dtProvider.getInstance();
            }
        }
        return null;
    }

    @Override
    public List<AttributeTransformerDescriptor> findTransformers(DBPDataSource dataSource, DBSTypedObject typedObject, Boolean custom) {
        // Find in default providers
        List<AttributeTransformerDescriptor> result = null;
        for (AttributeTransformerDescriptor descriptor : dataTypeTransformers) {

            if ((custom == null || custom == descriptor.isCustom()) &&
                ((!descriptor.isGlobal() && descriptor.supportsDataSource(dataSource) && descriptor.supportsType(typedObject)) ||
                (descriptor.isGlobal() && descriptor.supportsType(typedObject))))
            {
                if (result == null) {
                    result = new ArrayList<>();
                }
                result.add(descriptor);
            }
        }
        if (result != null) {
            result.sort(Comparator.comparing(AttributeTransformerDescriptor::getName));
        }
        return result;
    }

    @Override
    public DBDAttributeTransformerDescriptor getTransformer(String id) {
        for (AttributeTransformerDescriptor descriptor : dataTypeTransformers) {
            if (id.equals(descriptor.getId())) {
                return descriptor;
            }
        }
        return null;
    }

}
