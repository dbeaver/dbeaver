/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
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
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCConnector;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.impl.jdbc.api.JDBCConnectionImpl;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.qm.QMUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
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

        {
            // Notify QM
            boolean autoCommit = false;
            try {
                autoCommit = connection.getAutoCommit();
            } catch (Throwable e) {
                log.warn("Could not check auto-commit state", e);
            }
            QMUtils.getDefaultHandler().handleSessionStart(this, !autoCommit);
        }
    }

    protected Connection openConnection()
        throws DBException
    {
        // It MUST be a JDBC driver
        Driver driverInstance = getDriverInstance();

        // Set properties
        Properties connectProps = new Properties();

        {
            // Use properties defined by datasource itself
            Properties internalProps = getInternalConnectionProperties();
            if (internalProps != null) {
                connectProps.putAll(internalProps);
            }
        }

        {
            // Use driver properties
            final Map<Object, Object> driverProperties = container.getDriver().getConnectionProperties();
            if (driverProperties != null) {
                connectProps.putAll(driverProperties);
            }
        }

        DBPConnectionInfo connectionInfo = container.getConnectionInfo();
        if (connectionInfo.getProperties() != null) {
            connectProps.putAll(connectionInfo.getProperties());
        }
        if (!CommonUtils.isEmpty(connectionInfo.getUserName())) {
            connectProps.put(DBConstants.DATA_SOURCE_PROPERTY_USER, connectionInfo.getUserName());
        }
        if (!CommonUtils.isEmpty(connectionInfo.getUserPassword())) {
            connectProps.put(DBConstants.DATA_SOURCE_PROPERTY_PASSWORD, connectionInfo.getUserPassword());
        }

        // Obtain connection
        try {
            if (driverInstance != null && !driverInstance.acceptsURL(connectionInfo.getUrl())) {
                throw new DBException("Bad URL: " + connectionInfo.getUrl());
            }
            Connection connection;
            if (driverInstance == null) {
                connection = DriverManager.getConnection(connectionInfo.getUrl(), connectProps);
            } else {
                connection = driverInstance.connect(connectionInfo.getUrl(), connectProps);
            }
            if (connection == null) {
                throw new DBException("Null connection returned");
            }

            return connection;
        }
        catch (SQLException ex) {
            throw new DBException(ex);
        }
        catch (Throwable e) {
            throw new DBException("Unexpected driver error occurred while connecting to database", e);
        }
    }

    protected Driver getDriverInstance()
        throws DBException
    {
        return Driver.class.cast(container.getDriver().getDriverInstance());
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

    public JDBCExecutionContext openContext(DBRProgressMonitor monitor, DBCExecutionPurpose purpose, String taskTitle)
    {
        if (connection == null) {
            throw new IllegalStateException("Not connected to database");
        }
        return new JDBCConnectionImpl(this, monitor, purpose, taskTitle, false);
    }

    public DBCExecutionContext openIsolatedContext(DBRProgressMonitor monitor, DBCExecutionPurpose purpose, String taskTitle) {
        return new JDBCConnectionImpl(this, monitor, purpose, taskTitle, true);
    }

    public DBSDataSourceContainer getContainer()
    {
        return container;
    }

    public synchronized DBPDataSourceInfo getInfo()
    {
        return dataSourceInfo;
    }

    public synchronized boolean isConnected()
    {
        return connection != null;
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

    public synchronized void initialize(DBRProgressMonitor monitor)
        throws DBException
    {
        JDBCExecutionContext context = openContext(monitor, DBCExecutionPurpose.META, "Read database meta data");
        try {
            dataSourceInfo = makeInfo(context.getMetaData());
        } catch (SQLException ex) {
            throw new DBException("Error getting JDBC meta data", ex);
        }
        finally {
            context.close();
        }
    }

    protected DBPDataSourceInfo makeInfo(JDBCDatabaseMetaData metaData)
    {
        return new JDBCDataSourceInfo(metaData);
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
        QMUtils.getDefaultHandler().handleSessionEnd(this);
    }

    @Property(name = "Name", viewable = true, order = 1)
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

    public boolean isPersisted()
    {
        return true;
    }

    public boolean refreshEntity(DBRProgressMonitor monitor) throws DBException {
        this.dataSourceInfo = null;
        return true;
    }

    public DBCQueryTransformer createQueryTransformer(DBCQueryTransformType type)
    {
        if (type == DBCQueryTransformType.ORDER_BY) {

        } else if (type == DBCQueryTransformType.FILTER) {

        }
        return null;
    }

}
