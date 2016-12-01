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
package org.jkiss.dbeaver.runtime.properties;

import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;

/**
 * Datasource property filter
 */
public class DataSourcePropertyFilter implements IPropertyFilter {

    private final boolean showExpensive;

    public DataSourcePropertyFilter()
    {
        this((DBPDataSourceContainer)null);
    }
    public DataSourcePropertyFilter(DBPDataSource dataSource)
    {
        this(dataSource == null ? null : dataSource.getContainer());
    }

    public DataSourcePropertyFilter(DBPDataSourceContainer container)
    {
        DBPPreferenceStore store = container != null ?
            container.getPreferenceStore() :
            DBeaverCore.getGlobalPreferenceStore();
        this.showExpensive = store.getBoolean(DBeaverPreferences.READ_EXPENSIVE_PROPERTIES);
    }

    @Override
    public boolean select(DBPPropertyDescriptor toTest)
    {
        if (toTest instanceof ObjectPropertyDescriptor) {
            ObjectPropertyDescriptor prop = (ObjectPropertyDescriptor)toTest;
            return !(prop.isExpensive() && !showExpensive);
        }
        return false;
    }
}
