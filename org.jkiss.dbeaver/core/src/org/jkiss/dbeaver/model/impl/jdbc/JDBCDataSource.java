/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc;

import net.sf.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IProduct;
import org.eclipse.core.runtime.Platform;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPConnectionInfo;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceInfo;
import org.jkiss.dbeaver.model.dbc.DBCExecutionContext;
import org.jkiss.dbeaver.model.dbc.DBCQueryTransformProvider;
import org.jkiss.dbeaver.model.dbc.DBCQueryTransformType;
import org.jkiss.dbeaver.model.dbc.DBCQueryTransformer;
import org.jkiss.dbeaver.model.impl.jdbc.api.ConnectionManagable;
import org.jkiss.dbeaver.model.jdbc.JDBCConnector;
import org.jkiss.dbeaver.model.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * GenericDataSource
 */
public abstract class JDBCDataSource
    implements
        DBPDataSource,
        JDBCConnector,
        DBSEntity,
        DBSEntityContainer,
        DBCQueryTransformProvider
{
    static final Log log = LogFactory.getLog(JDBCDataSource.class);

    private DBSDataSourceContainer container;
    private Connection connection;

    protected DBPDataSourceInfo dataSourceInfo;

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
            if (driverInstance != null && !driverInstance.acceptsURL(connectionInfo.getUrl())) {
                throw new DBException("Bad URL: " + connectionInfo.getUrl());
            }
            Connection connection;
            if (driverInstance == null) {
                connection = DriverManager.getConnection(connectionInfo.getUrl(), driverProps);
            } else {
                connection = driverInstance.connect(connectionInfo.getUrl(), driverProps);
            }
            if (connection == null) {
                throw new DBException("Null connection returned");
            }

            {
                // Provide client info
                IProduct product = Platform.getProduct();
                if (product != null) {
                    String appName = "DBeaver " + product.getDefiningBundle().getVersion().toString();
                    try {
                        connection.setClientInfo("ApplicationName", appName);
                    } catch (Throwable e) {
                        // just ignore
                    }
                }
            }
            return connection;
        }
        catch (SQLException ex) {
            throw new DBException(ex);
        }
    }

    /**
     * Could be overridden by extenders. May contain any additional connection properties.
     * Note: these properties may be overwritten by connection advanced properties.
     * @return predefined connection properties
     */
    protected Properties getInternalConnectionProperties()
    {
        return null;
    }

    public Connection getConnection()
    {
        return connection;
    }

    public Connection openIsolatedConnection() throws SQLException {
        try {
            return openConnection();
        } catch (DBException e) {
            if (e.getCause() instanceof SQLException) {
                throw (SQLException)e.getCause();
            } else {
                throw new SQLException("Could not open isolated connection", e);
            }
        }
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
        return new ConnectionManagable(this, monitor, taskTitle, false);
    }

    public DBCExecutionContext openIsolatedContext(DBRProgressMonitor monitor, String taskTitle) {
        return new ConnectionManagable(this, monitor, taskTitle, true);
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
            connection = openConnection();
            return;
        }

        if (!JDBCUtils.isConnectionAlive(connection)) {
            close(monitor);
            connection = openConnection();
        }
    }

    public void initialize(DBRProgressMonitor monitor)
        throws DBException
    {
        JDBCExecutionContext context = openContext(monitor, "Read database meta data");
        try {
            dataSourceInfo = new JDBCDataSourceInfo(
                context.getMetaData());
        } catch (SQLException ex) {
            throw new DBException("Error getting JDBC meta data", ex);
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

    public String getObjectId() {
        return container.getObjectId();
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

    public boolean refreshEntity(DBRProgressMonitor monitor) throws DBException {
        this.dataSourceInfo = null;
        return true;
    }

    public DBCQueryTransformer createQueryTransformer(DBCQueryTransformType type)
    {
        return null;
    }

}
