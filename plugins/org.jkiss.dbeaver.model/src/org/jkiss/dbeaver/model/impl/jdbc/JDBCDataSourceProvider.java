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
package org.jkiss.dbeaver.model.impl.jdbc;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.impl.PropertyDescriptor;

import java.sql.Driver;
import java.sql.DriverPropertyInfo;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

/**
 * JDBCDataSourceProvider
 */
public abstract class JDBCDataSourceProvider implements DBPDataSourceProvider {
    static final protected Log log = Log.getLog(JDBCDataSourceProvider.class);

    @Override
    public void init(@NotNull DBPApplication application) {

    }

    @Override
    public DBPPropertyDescriptor[] getConnectionProperties(
        DBRRunnableContext runnableContext,
        DBPDriver driver,
        DBPConnectionConfiguration connectionInfo)
        throws DBException {
        Collection<DBPPropertyDescriptor> props = null;
        Object driverInstance = driver.getDriverInstance(runnableContext);
        if (driverInstance instanceof Driver) {
            props = readDriverProperties(connectionInfo, (Driver) driverInstance);
        }
        if (props == null) {
            return null;
        }
        return props.toArray(new DBPPropertyDescriptor[props.size()]);
    }

    private Collection<DBPPropertyDescriptor> readDriverProperties(
        DBPConnectionConfiguration connectionInfo,
        Driver driver)
        throws DBException {
        Properties driverProps = new Properties();
        //driverProps.putAll(connectionInfo.getProperties());
        DriverPropertyInfo[] propDescs;
        try {
            propDescs = driver.getPropertyInfo(connectionInfo.getUrl(), driverProps);
        } catch (Throwable e) {
            log.debug("Cannot obtain driver's properties", e); //$NON-NLS-1$
            return null;
        }
        if (propDescs == null) {
            return null;
        }

        List<DBPPropertyDescriptor> properties = new ArrayList<>();
        for (DriverPropertyInfo desc : propDescs) {
            if (DBConstants.DATA_SOURCE_PROPERTY_USER.equals(desc.name) || DBConstants.DATA_SOURCE_PROPERTY_PASSWORD.equals(desc.name)) {
                // Skip user/password properties
                continue;
            }
            desc.value = getConnectionPropertyDefaultValue(desc.name, desc.value);
            properties.add(new PropertyDescriptor(
                ModelMessages.model_jdbc_driver_properties,
                desc.name,
                desc.name,
                desc.description,
                String.class,
                desc.required,
                desc.value,
                desc.choices,
                true));
        }
        return properties;
    }

    protected String getConnectionPropertyDefaultValue(String name, String value) {
        return value;
    }
}
