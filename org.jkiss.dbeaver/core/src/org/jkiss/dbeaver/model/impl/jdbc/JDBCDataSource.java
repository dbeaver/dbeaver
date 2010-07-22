/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc;

import net.sf.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPConnectionInfo;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceInfo;
import org.jkiss.dbeaver.model.dbc.DBCQueryTransformProvider;
import org.jkiss.dbeaver.model.dbc.DBCQueryTransformType;
import org.jkiss.dbeaver.model.dbc.DBCQueryTransformer;
import org.jkiss.dbeaver.model.impl.jdbc.api.ConnectionManagable;
import org.jkiss.dbeaver.model.jdbc.JDBCConnector;
import org.jkiss.dbeaver.model.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSStructureContainer;

import java.sql.Connection;
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
        DBSObject,
        DBCQueryTransformProvider
{
    static final Log log = LogFactory.getLog(JDBCDataSource.class);

    private DBSDataSourceContainer container;
    private Connection connection;

    protected DBPDataSourceInfo dataSourceInfo;

    public JDBCDataSource(DBRProgressMonitor monitor, DBSDataSourceContainer container)
        throws DBException
    {
        this.container = container;
        this.connection = openConnection(monitor);
    }

    protected Connection openConnection(DBRProgressMonitor monitor)
        throws DBException
    {
        // It MUST be a JDBC driver
        Driver driverInstance = Driver.class.cast(container.getDriver().getDriverInstance());

        // Set properties
        Properties driverProps = new Properties();

        Properties internalProps = getInternalConnectionProperties();
        if (internalProps != null) {
            driverProps.putAll(internalProps);
        }

        DBPConnectionInfo connectionInfo = container.getConnectionInfo();
        if (connectionInfo.getProperties() != null) {
            driverProps.putAll(connectionInfo.getProperties());
        }
        if (!CommonUtils.isEmpty(connectionInfo.getUserName())) {
            driverProps.put(DBConstants.PROPERTY_USER, connectionInfo.getUserName());
        }
        if (!CommonUtils.isEmpty(connectionInfo.getUserPassword())) {
            driverProps.put(DBConstants.PROPERTY_PASSWORD, connectionInfo.getUserPassword());
        }

        // Obtain connection
        try {
            if (!driverInstance.acceptsURL(connectionInfo.getUrl())) {
                throw new DBException("Bad URL: " + connectionInfo.getUrl());
            }
            Connection connection = driverInstance.connect(connectionInfo.getUrl(), driverProps);
            if (connection == null) {
                throw new DBException("Null connection returned by " + driverInstance);
            }
            return connection;
        }
        catch (SQLException ex) {
            throw new DBException(ex);
        }
    }

    /**
     * Could be overrided by extenders. May contain any additional connection properties.
     * Note: these properties may be overwrited by connection advanced properties.
     * @return
     */
    protected Properties getInternalConnectionProperties()
    {
        return null;
    }

    public Connection getConnection()
    {
        return connection;
    }

    public JDBCExecutionContext openContext(DBRProgressMonitor monitor)
    {
        return openContext(monitor, null);
    }

    public JDBCExecutionContext openContext(DBRProgressMonitor monitor, String taskTitle)
    {
        if (connection == null) {
            throw new IllegalStateException("Not connected to database");
        }
        return new ConnectionManagable(this, monitor, taskTitle);
    }

    public DBSDataSourceContainer getContainer()
    {
        return container;
    }

    public DBPDataSourceInfo getInfo()
    {
        return dataSourceInfo;
    }

    public void invalidateConnection(DBRProgressMonitor monitor)
        throws DBException
    {
        if (connection == null) {
            connection = openConnection(monitor);
            return;
        }

        if (!JDBCUtils.isConnectionAlive(connection)) {
            close(monitor);
            connection = openConnection(monitor);
        }
    }

    public void initialize(DBRProgressMonitor monitor)
        throws DBException
    {
        JDBCExecutionContext context = openContext(monitor, "Read dtabase metadata");
        try {
            dataSourceInfo = new JDBCDataSourceInfo(
                context.getMetaData());
        } catch (SQLException ex) {
            throw new DBException("Error getting JDBC metadata", ex);
        }
        finally {
            context.close();
        }
    }

    public void close(DBRProgressMonitor monitor)
    {
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                }
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

    public boolean refreshObject(DBRProgressMonitor monitor) throws DBException {
        this.dataSourceInfo = null;
        return true;
    }

    public DBCQueryTransformer createQueryTransformer(DBCQueryTransformType type)
    {
        return null;
    }

}
