/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.ui.properties.PropertyDescriptorEx;

import java.sql.Driver;
import java.sql.DriverPropertyInfo;
import java.util.*;

/**
 * JDBCDataSourceProvider
 */
public abstract class JDBCDataSourceProvider implements DBPDataSourceProvider
{
    static final Log log = LogFactory.getLog(JDBCDataSourceProvider.class);

    public void close()
    {

    }

    public void init(DBPApplication application)
    {

    }

    public Collection<IPropertyDescriptor> getConnectionProperties(
        DBPDriver driver,
        DBPConnectionInfo connectionInfo)
        throws DBException
    {
        Collection<IPropertyDescriptor> props = null;
        Object driverInstance = driver.getDriverInstance();
        if (driverInstance instanceof Driver) {
            props = readDriverProperties(connectionInfo, (Driver) driverInstance);
        }
        if (props == null) {
            props = Collections.emptyList();
        }
        return props;
    }

    private Collection<IPropertyDescriptor> readDriverProperties(
        DBPConnectionInfo connectionInfo,
        Driver driver)
        throws DBException
    {
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

        List<IPropertyDescriptor> properties = new ArrayList<IPropertyDescriptor>();
        for (DriverPropertyInfo desc : propDescs) {
            if (DBConstants.DATA_SOURCE_PROPERTY_USER.equals(desc.name) || DBConstants.DATA_SOURCE_PROPERTY_PASSWORD.equals(desc.name)) {
                // Skip user/password properties
                continue;
            }
            desc.value = getConnectionPropertyDefaultValue(desc.name, desc.value);
            properties.add(new PropertyDescriptorEx(
                CoreMessages.model_jdbc_driver_properties,
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

    protected String getConnectionPropertyDefaultValue(String name, String value)
    {
        return value;
    }
}
