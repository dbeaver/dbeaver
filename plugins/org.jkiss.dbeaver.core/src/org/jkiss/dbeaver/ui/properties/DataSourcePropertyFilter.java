/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
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
