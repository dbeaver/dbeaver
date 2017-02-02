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
package org.jkiss.dbeaver.registry.datatype;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.data.DBDAttributeTransformerDescriptor;
import org.jkiss.dbeaver.model.data.DBDRegistry;
import org.jkiss.dbeaver.model.data.DBDValueHandlerProvider;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.registry.DataSourceProviderDescriptor;
import org.jkiss.dbeaver.registry.driver.DriverDescriptor;

import java.util.ArrayList;
import java.util.List;

/**
 * DataTypeProviderRegistry
 */
public class DataTypeProviderRegistry implements DBDRegistry
{
    private static final Log log = Log.getLog(DataTypeProviderRegistry.class);

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
                } else if ("transformer".equals(ext.getName())) {
                    dataTypeTransformers.add(new AttributeTransformerDescriptor(ext));
                }
            }
        }
    }

    public void dispose()
    {
        this.dataTypeProviders.clear();
    }

    ////////////////////////////////////////////////////
    // DataType providers

    @Nullable
    public DBDValueHandlerProvider getDataTypeProvider(@NotNull DBPDataSource dataSource, @NotNull DBSTypedObject typedObject)
    {
        DBPDriver driver = dataSource.getContainer().getDriver();
        if (!(driver instanceof DriverDescriptor)) {
            log.warn("Bad datasource specified (driver is not recognized by registry) - " + dataSource);
            return null;
        }
        DataSourceProviderDescriptor dsProvider = ((DriverDescriptor) driver).getProviderDescriptor();

        // First try to find type provider for specific datasource type
        for (ValueHandlerDescriptor dtProvider : dataTypeProviders) {
            if (!dtProvider.isGlobal() && dtProvider.supportsDataSource(dataSource, dsProvider) && dtProvider.supportsType(typedObject)) {
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
        DBPDriver driver = dataSource.getContainer().getDriver();
        if (!(driver instanceof DriverDescriptor)) {
            log.warn("Bad datasource specified (driver is not recognized by registry) - " + dataSource);
            return null;
        }
        DataSourceProviderDescriptor dsProvider = ((DriverDescriptor) driver).getProviderDescriptor();

        // Find in default providers
        List<AttributeTransformerDescriptor> result = null;
        for (AttributeTransformerDescriptor descriptor : dataTypeTransformers) {

            if ((custom == null || custom == descriptor.isCustom()) &&
                ((!descriptor.isGlobal() && descriptor.supportsDataSource(dataSource, dsProvider) && descriptor.supportsType(typedObject)) ||
                (descriptor.isGlobal() && descriptor.supportsType(typedObject))))
            {
                if (result == null) {
                    result = new ArrayList<>();
                }
                result.add(descriptor);
            }
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
