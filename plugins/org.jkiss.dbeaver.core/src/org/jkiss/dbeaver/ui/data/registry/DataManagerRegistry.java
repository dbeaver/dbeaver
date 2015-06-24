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

package org.jkiss.dbeaver.ui.data.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.ui.data.IValueManager;
import org.jkiss.dbeaver.ui.data.managers.DefaultValueManager;

import java.util.ArrayList;
import java.util.List;

/**
 * EntityEditorsRegistry
 */
public class DataManagerRegistry {

    private static DataManagerRegistry instance = null;

    public synchronized static DataManagerRegistry getInstance()
    {
        if (instance == null) {
            instance = new DataManagerRegistry(Platform.getExtensionRegistry());
        }
        return instance;
    }

    private List<DataManagerDescriptor> managers = new ArrayList<DataManagerDescriptor>();

    private DataManagerRegistry(IExtensionRegistry registry)
    {
        // Load datasource providers from external plugins
        IConfigurationElement[] extElements = registry.getConfigurationElementsFor(DataManagerDescriptor.EXTENSION_ID);
        for (IConfigurationElement ext : extElements) {
            if (DataManagerDescriptor.TAG_MANAGER.equals(ext.getName())) {
                managers.add(new DataManagerDescriptor(ext));
            }
        }
    }

    @NotNull
    public IValueManager getManager(@NotNull DBPDataSource dataSource, @NotNull DBPDataKind dataKind, @NotNull Class<?> valueType) {
        // Check starting from most restrictive to less restrictive
        IValueManager manager = findManager(dataSource, dataKind, valueType, true, true);
        if (manager == null) {
            manager = findManager(dataSource, dataKind, valueType, false, true);
        }
        if (manager == null) {
            manager = findManager(dataSource, dataKind, valueType, true, false);
        }
        if (manager == null) {
            manager = findManager(dataSource, dataKind, valueType, false, false);
        }
        if (manager == null) {
            manager = DefaultValueManager.INSTANCE;
        }
        return manager;
    }

    private IValueManager findManager(DBPDataSource dataSource, DBPDataKind dataKind, Class<?> valueType, boolean checkDataSource, boolean checkType) {
        for (DataManagerDescriptor manager : managers) {
            if (manager.supportsType(dataSource, dataKind, valueType, checkDataSource, checkType)) {
                return manager.getInstance();
            }
        }
        return null;
    }
}
