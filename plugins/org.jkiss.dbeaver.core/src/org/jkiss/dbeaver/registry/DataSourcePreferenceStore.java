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
package org.jkiss.dbeaver.registry;

import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.utils.AbstractPreferenceStore;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.util.Map;

/**
 * DataSourcePreferenceStore
 */
public class DataSourcePreferenceStore extends AbstractPreferenceStore
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
