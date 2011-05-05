/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.properties;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.IFilter;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.ui.preferences.PrefConstants;

/**
 * Datasource property filter
 */
public class DataSourcePropertyFilter implements IFilter {

    private final boolean showExpensive;

    public DataSourcePropertyFilter(DBPDataSource dataSource)
    {
        IPreferenceStore store = dataSource != null ?
            dataSource.getContainer().getPreferenceStore() :
            DBeaverCore.getInstance().getGlobalPreferenceStore();
        this.showExpensive = store.getBoolean(PrefConstants.READ_EXPENSIVE_PROPERTIES);
    }

    public boolean select(Object toTest)
    {
        if (toTest instanceof ObjectPropertyDescriptor) {
            ObjectPropertyDescriptor prop = (ObjectPropertyDescriptor)toTest;
            if (prop.isExpensive() && !showExpensive) {
                return false;
            }
            return true;
        }
        return false;
    }
}
