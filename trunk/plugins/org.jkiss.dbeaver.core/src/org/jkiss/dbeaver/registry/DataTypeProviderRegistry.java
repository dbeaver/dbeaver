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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDriver;
import org.jkiss.dbeaver.model.data.DBDValueHandlerProvider;
import org.jkiss.dbeaver.model.data.DBDValueHandlerRegistry;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.util.ArrayList;
import java.util.List;

public class DataTypeProviderRegistry implements DBDValueHandlerRegistry
{
    static final Log log = Log.getLog(DataTypeProviderRegistry.class);

    private static DataTypeProviderRegistry instance = null;

    public synchronized static DataTypeProviderRegistry getInstance()
    {
        if (instance == null) {
            instance = new DataTypeProviderRegistry();
            instance.loadExtensions(Platform.getExtensionRegistry());
        }
        return instance;
    }

    private final List<DataTypeProviderDescriptor> dataTypeProviders = new ArrayList<DataTypeProviderDescriptor>();

    private DataTypeProviderRegistry()
    {
    }

    public void loadExtensions(IExtensionRegistry registry)
    {
        // Load data type providers from external plugins
        {
            IConfigurationElement[] extElements = registry.getConfigurationElementsFor(DataTypeProviderDescriptor.EXTENSION_ID);
            for (IConfigurationElement ext : extElements) {
                DataTypeProviderDescriptor provider = new DataTypeProviderDescriptor(this, ext);
                dataTypeProviders.add(provider);
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
    public DBDValueHandlerProvider getDataTypeProvider(DBPDataSource dataSource, DBSTypedObject typedObject)
    {
        DBPDriver driver = dataSource.getContainer().getDriver();
        if (!(driver instanceof DriverDescriptor)) {
            log.warn("Bad datasource specified (driver is not recognized by registry) - " + dataSource);
            return null;
        }
        DataSourceProviderDescriptor dsProvider = ((DriverDescriptor) driver).getProviderDescriptor();

        // First try to find type provider for specific datasource type
        for (DataTypeProviderDescriptor dtProvider : dataTypeProviders) {
            if (!dtProvider.isDefault() && dtProvider.supportsDataSource(dsProvider) && dtProvider.supportsType(typedObject)) {
                return dtProvider.getInstance();
            }
        }

        // Find in default providers
        for (DataTypeProviderDescriptor dtProvider : dataTypeProviders) {
            if (dtProvider.isDefault() && dtProvider.supportsType(typedObject)) {
                return dtProvider.getInstance();
            }
        }
        return null;
    }

}
