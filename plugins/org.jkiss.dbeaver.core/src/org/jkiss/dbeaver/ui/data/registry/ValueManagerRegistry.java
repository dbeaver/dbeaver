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

package org.jkiss.dbeaver.ui.data.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.ui.data.IStreamValueManager;
import org.jkiss.dbeaver.ui.data.IValueManager;
import org.jkiss.dbeaver.ui.data.managers.DefaultValueManager;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * EntityEditorsRegistry
 */
public class ValueManagerRegistry {

    private static ValueManagerRegistry instance = null;

    public synchronized static ValueManagerRegistry getInstance() {
        if (instance == null) {
            instance = new ValueManagerRegistry(Platform.getExtensionRegistry());
        }
        return instance;
    }

    private List<ValueManagerDescriptor> managers = new ArrayList<>();
    private List<StreamValueManagerDescriptor> streamManagers = new ArrayList<>();

    private ValueManagerRegistry(IExtensionRegistry registry) {
        // Load datasource providers from external plugins
        IConfigurationElement[] extElements = registry.getConfigurationElementsFor(ValueManagerDescriptor.EXTENSION_ID);
        for (IConfigurationElement ext : extElements) {
            if (ValueManagerDescriptor.TAG_MANAGER.equals(ext.getName())) {
                managers.add(new ValueManagerDescriptor(ext));
            } else if (StreamValueManagerDescriptor.TAG_STREAM_MANAGER.equals(ext.getName())) {
                streamManagers.add(new StreamValueManagerDescriptor(ext));
            }
        }
    }

    @NotNull
    public IValueManager getManager(@Nullable DBPDataSourceContainer dataSource, @NotNull DBSTypedObject dataKind, @NotNull Class<?> valueType) {
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

    private IValueManager findManager(@Nullable DBPDataSourceContainer dataSource, DBSTypedObject typedObject, Class<?> valueType, boolean checkDataSource, boolean checkType) {
        for (ValueManagerDescriptor manager : managers) {
            if (manager.supportsType(dataSource, typedObject, valueType, checkDataSource, checkType)) {
                return manager.getInstance();
            }
        }
        return null;
    }

    @NotNull
    public static IValueManager findValueManager(@Nullable DBPDataSourceContainer dataSource, @NotNull DBSTypedObject typedObject, @NotNull Class<?> valueType) {
        return getInstance().getManager(dataSource, typedObject, valueType);
    }

    public Map<StreamValueManagerDescriptor, IStreamValueManager.MatchType> getApplicableStreamManagers(@NotNull DBRProgressMonitor monitor, @NotNull DBSTypedObject attribute, @Nullable DBDContent value) {
        Map<StreamValueManagerDescriptor, IStreamValueManager.MatchType> result = new LinkedHashMap<>();
        for (StreamValueManagerDescriptor contentManager : streamManagers) {
            IStreamValueManager.MatchType matchType = contentManager.getInstance().matchesTo(monitor, attribute, value);
            switch (matchType) {
                case NONE:
                    continue;
                case EXCLUSIVE:
                    result.clear();
                    result.put(contentManager, matchType);
                    return result;
                default:
                    result.put(contentManager, matchType);
                    break;
            }
        }
        return result;
    }

}
