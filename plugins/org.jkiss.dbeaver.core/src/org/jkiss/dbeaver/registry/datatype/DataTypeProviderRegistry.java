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
package org.jkiss.dbeaver.registry.datatype;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.data.*;
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
            if (!dtProvider.isGlobal() && dtProvider.supportsDataSource(dsProvider) && dtProvider.supportsType(typedObject)) {
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
                ((!descriptor.isGlobal() && descriptor.supportsDataSource(dsProvider) && descriptor.supportsType(typedObject)) ||
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
