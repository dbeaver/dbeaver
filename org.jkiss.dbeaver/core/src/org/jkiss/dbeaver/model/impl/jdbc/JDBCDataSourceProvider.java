/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.prop.DBPProperty;
import org.jkiss.dbeaver.model.prop.DBPPropertyGroup;

import java.sql.Driver;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

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

    public DBPPropertyGroup getConnectionProperties(
        DBPDriver driver,
        DBPConnectionInfo connectionInfo)
        throws DBException
    {
        Object driverInstance = driver.getDriverInstance();
        if (driverInstance instanceof Driver) {
            return readDriverProperties(connectionInfo, (Driver) driverInstance);
        } else {
            return null;
        }
    }

    private DBPPropertyGroup readDriverProperties(
        DBPConnectionInfo connectionInfo,
        Driver driver)
        throws DBException
    {
        Properties driverProps = new Properties();
        //driverProps.putAll(connectionInfo.getProperties());
        DriverPropertyInfo[] propDescs;
        try {
            propDescs = driver.getPropertyInfo(connectionInfo.getUrl(), driverProps);
        } catch (SQLException e) {
            log.debug("Could not obtain driver's properties", e);
            return null;
        }
        if (propDescs == null) {
            return null;
        }

        final List<DBPProperty> result = new ArrayList<DBPProperty>();
        DBPPropertyGroup propGroup = new DBPPropertyGroup() {
            public String getName() {
                return "Driver properties";
            }

            public String getDescription() {
                return "JDBC Driver Properties";
            }

            public List<? extends DBPProperty> getProperties() {
                return result;
            }
        };
        for (DriverPropertyInfo desc : propDescs) {
            if (DBConstants.PROPERTY_USER.equals(desc.name) || DBConstants.PROPERTY_PASSWORD.equals(desc.name)) {
                // Skip user/password properties
                continue;
            }
            desc.value = getConnectionPropertyDefaultValue(desc.name, desc.value);
            result.add(new JDBCConnectionProperty(propGroup, desc));
        }
        return propGroup;
    }

    protected String getConnectionPropertyDefaultValue(String name, String value)
    {
        return value;
    }
}
