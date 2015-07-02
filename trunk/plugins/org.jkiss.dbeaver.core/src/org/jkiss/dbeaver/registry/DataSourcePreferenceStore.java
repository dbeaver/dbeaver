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

import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.impl.preferences.SimplePreferenceStore;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.util.Map;

/**
 * DataSourcePreferenceStore
 */
public class DataSourcePreferenceStore extends SimplePreferenceStore
{
    private final DataSourceDescriptor dataSourceDescriptor;

    DataSourcePreferenceStore(DataSourceDescriptor dataSourceDescriptor)
    {
        super(DBeaverCore.getGlobalPreferenceStore());
        this.dataSourceDescriptor = dataSourceDescriptor;
        // Init default properties from driver overrides
        Map<Object,Object> defaultConnectionProperties = dataSourceDescriptor.getDriver().getDefaultConnectionProperties();
        for (Map.Entry<Object, Object> prop : defaultConnectionProperties.entrySet()) {
            String propName = CommonUtils.toString(prop.getKey());
            if (propName.startsWith(DBConstants.DEFAULT_DRIVER_PROP_PREFIX)) {
                getDefaultProperties().put(
                    propName.substring(DBConstants.DEFAULT_DRIVER_PROP_PREFIX.length()),
                    CommonUtils.toString(prop.getValue()));
            }
        }
    }

    public DataSourceDescriptor getDataSourceDescriptor()
    {
        return dataSourceDescriptor;
    }

    @Override
    public void save()
        throws IOException
    {
        dataSourceDescriptor.getRegistry().flushConfig();
    }

}
