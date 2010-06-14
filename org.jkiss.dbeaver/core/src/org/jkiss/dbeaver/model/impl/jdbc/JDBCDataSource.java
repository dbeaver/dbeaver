/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc;

import net.sf.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPConnectionInfo;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceInfo;
import org.jkiss.dbeaver.model.dbc.DBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.api.ConnectionManagable;
import org.jkiss.dbeaver.model.jdbc.JDBCConnector;
import org.jkiss.dbeaver.model.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSStructureContainer;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Properties;

/**
 * GenericDataSource
 */
public abstract class JDBCDataSource
    implements
        DBPDataSource,
        JDBCConnector,
        DBSStructureContainer,
        DBSObject
{
    static Log log = LogFactory.getLog(JDBCDataSource.class);

    private DBSDataSourceContainer container;
    private Connection connection;

    private DBPDataSourceInfo info;

    public JDBCDataSource(DBSDataSourceContainer container)
        throws DBException
    {
        this.container = container;
        this.connection = openConnection();
    }

    protected Connection openConnection()
        throws DBException
    {
        // It MUST be a JDBC driver
        Driver driverInstance = Driver.class.cast(container.getDriver().getDriverInstance());

        // Set properties
        Properties driverProps = new Properties();
        DBPConnectionInfo connectionInfo = container.getConnectionInfo();
        if (connectionInfo.getProperties() != null) {
            driverProps.putAll(connectionInfo.getProperties());
        }
        if (!CommonUtils.isEmpty(connectionInfo.getUserName())) {
            driverProps.put("user", connectionInfo.getUserName());
        }
        if (!CommonUtils.isEmpty(connectionInfo.getUserPassword())) {
            driverProps.put("password", connectionInfo.getUserPassword());
        }

        // Obtain connection
        try {
            if (!driverInstance.acceptsURL(connectionInfo.getJdbcURL())) {
                throw new DBException("Bad URL: " + connectionInfo.getJdbcURL());
            }
            Connection connection = driverInstance.connect(connectionInfo.getJdbcURL(), driverProps);
            if (connection == null) {
                throw new DBException("Null connection returned by " + driverInstance);
            }
            return connection;
        }
        catch (SQLException ex) {
            throw new DBException(ex);
        }
    }

    public Connection getConnection()
    {
        return connection;
    }

    public JDBCExecutionContext getExecutionContext(DBRProgressMonitor monitor)
    {
        return new ConnectionManagable(this, monitor);
    }

    public DBSDataSourceContainer getContainer()
    {
        return container;
    }

    public DBPDataSourceInfo getInfo()
    {
        return info;
    }

    public DBCSession getSession(boolean forceNew)
        throws DBException
    {
        if (forceNew) {
            return new JDBCSession(this, this.openConnection());
        } else {
            return new JDBCSession(this);
        }
    }

    public void checkConnection(DBRProgressMonitor monitor)
        throws DBException
    {
        if (connection == null) {
            throw new DBException("Not connected");
        }
        try {
            JDBCResultSet dbResult = getExecutionContext(monitor).getMetaData().getTables(null, null, null, null);
            try {
                dbResult.next();
            }
            finally {
                dbResult.close();
            }
        }
        catch (SQLException ex) {
            throw new DBException(ex);
        }
    }

    public void initialize(DBRProgressMonitor monitor)
        throws DBException
    {
        try {
            info = new JDBCDataSourceInfo(
                getExecutionContext(monitor).getMetaData());
        } catch (SQLException ex) {
            throw new DBException("Error getting JDBC metadata", ex);
        }
    }

    protected void reconnect()
        throws DBException
    {
        close();
        this.connection = openConnection();
    }

    public void close()
    {
        if (connection != null) {
            try {
                connection.close();
            }
            catch (SQLException ex) {
                log.error(ex);
            }
            connection = null;
        }
    }

    public String getName()
    {
        return container.getName();
    }

    public String getDescription()
    {
        return container.getDescription();
    }

    public DBSObject getParentObject()
    {
        return container;
    }

    public JDBCDataSource getDataSource()
    {
        return this;
    }

}
