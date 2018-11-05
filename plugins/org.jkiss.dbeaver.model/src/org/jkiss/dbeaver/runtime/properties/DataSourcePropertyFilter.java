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
package org.jkiss.dbeaver.runtime.properties;

import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBWorkbench;
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
            DBWorkbench.getPlatform().getPreferenceStore();
        this.showExpensive = store.getBoolean(ModelPreferences.READ_EXPENSIVE_PROPERTIES);
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
