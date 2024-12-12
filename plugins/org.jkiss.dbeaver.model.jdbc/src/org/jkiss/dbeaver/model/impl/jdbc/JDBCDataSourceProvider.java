/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.impl.jdbc;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPDataSourceProvider;
import org.jkiss.dbeaver.model.app.DBPPlatform;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.impl.PropertyDescriptor;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

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
    public void init(@NotNull DBPPlatform platform) {

    }

    @Override
    public DBPPropertyDescriptor[] getConnectionProperties(
        DBRProgressMonitor monitor,
        DBPDriver driver,
        DBPConnectionConfiguration connectionInfo)
        throws DBException {
        Collection<DBPPropertyDescriptor> props = null;
        if (driver.isInternalDriver()) {
            // Do not load properties from internal (ODBC) driver.
            // There is a bug in sun's JdbcOdbc bridge driver (#830): if connection fails during props reading
            // then all subsequent calls to openConnection will fail until another props reading will succeed.
            props = null;
        } else {
            Object driverInstance = driver.getDriverInstance(monitor);
            if (driverInstance instanceof Driver jdbcDriver) {
                props = readDriverProperties(connectionInfo, jdbcDriver, driver.isPropagateDriverProperties());
            }
        }
        if (props == null) {
            return null;
        }
        return props.toArray(new DBPPropertyDescriptor[0]);
    }

    private Collection<DBPPropertyDescriptor> readDriverProperties(
        DBPConnectionConfiguration connectionInfo,
        Driver driver,
        boolean propagateDriverProperties
    ) throws DBException {
        Properties driverProps = new Properties();
        if (propagateDriverProperties) {
            driverProps.putAll(connectionInfo.getProperties());
        }
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
            if (desc == null || DBConstants.DATA_SOURCE_PROPERTY_USER.equals(desc.name) || DBConstants.DATA_SOURCE_PROPERTY_PASSWORD.equals(desc.name)) {
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
