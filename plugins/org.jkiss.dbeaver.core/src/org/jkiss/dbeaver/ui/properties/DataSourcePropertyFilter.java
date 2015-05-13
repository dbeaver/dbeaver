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
package org.jkiss.dbeaver.ui.properties;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.IFilter;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.DBeaverPreferences;

/**
 * Datasource property filter
 */
public class DataSourcePropertyFilter implements IFilter {

    private final boolean showExpensive;

    public DataSourcePropertyFilter(DBPDataSource dataSource)
    {
        IPreferenceStore store = dataSource != null ?
            dataSource.getContainer().getPreferenceStore() :
            DBeaverCore.getGlobalPreferenceStore();
        this.showExpensive = store.getBoolean(DBeaverPreferences.READ_EXPENSIVE_PROPERTIES);
    }

    @Override
    public boolean select(Object toTest)
    {
        if (toTest instanceof ObjectPropertyDescriptor) {
            ObjectPropertyDescriptor prop = (ObjectPropertyDescriptor)toTest;
            return !(prop.isExpensive() && !showExpensive);
        }
        return false;
    }
}
