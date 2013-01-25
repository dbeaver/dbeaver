/*
 * Copyright (C) 2010-2012 Serge Rieder
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
package org.jkiss.dbeaver.model.impl.jdbc;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCConnector;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.impl.jdbc.exec.JDBCConnectionImpl;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.qm.QMUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.utils.CommonUtils;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

/**
 * GenericDataSource
 */
public abstract class JDBCDataSource
    implements
        DBPDataSource,
        DBPDataTypeProvider,
        DBPRefreshableObject,
        JDBCConnector,
        DBSObject,
        DBSObjectContainer,
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
                log.warn("Could not check auto-commit state", e); //$NON-NLS-1$
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
            Map<String,String> internalProps = getInternalConnectionProperties();
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

        DBPConnectionInfo connectionInfo = container.getActualConnectionInfo();
        if (connectionInfo.getProperties() != null) {
            connectProps.putAll(connectionInfo.getProperties());
        }
        if (!CommonUtils.isEmpty(connectionInfo.getUserName())) {
            connectProps.put(DBConstants.DATA_SOURCE_PROPERTY_USER, getConnectionUserName(connectionInfo));
        }
        if (!CommonUtils.isEmpty(connectionInfo.getUserPassword())) {
            connectProps.put(DBConstants.DATA_SOURCE_PROPERTY_PASSWORD, getConnectionUserPassword(connectionInfo));
        }
        for (Iterator<Object> iter = connectProps.keySet().iterator(); iter.hasNext(); ) {
            if (iter.next().toString().startsWith(DBConstants.INTERNAL_PROP_PREFIX)) {
                iter.remove();
            }
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

            // Set read-only flag
            if (container.isConnectionReadOnly()) {
                connection.setReadOnly(true);
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

    protected String getConnectionUserName(DBPConnectionInfo connectionInfo)
    {
        return connectionInfo.getUserName();
    }

    protected String getConnectionUserPassword(DBPConnectionInfo connectionInfo)
    {
        return connectionInfo.getUserPassword();
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
    protected Map<String, String> getInternalConnectionProperties()
    {
        return null;
    }

    @Override
    public Connection getConnection()
    {
        return connection;
    }

    @Override
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

    @Override
    public JDBCExecutionContext openContext(DBRProgressMonitor monitor, DBCExecutionPurpose purpose, String taskTitle)
    {
        if (connection == null) {
            throw new IllegalStateException(CoreMessages.editors_sql_status_not_connected_to_database);
        }
        return new JDBCConnectionImpl(this, monitor, purpose, taskTitle, false);
    }

    @Override
    public DBCExecutionContext openIsolatedContext(DBRProgressMonitor monitor, DBCExecutionPurpose purpose, String taskTitle) {
        return new JDBCConnectionImpl(this, monitor, purpose, taskTitle, true);
    }

    @Override
    public DBSDataSourceContainer getContainer()
    {
        return container;
    }

    @Override
    public synchronized DBPDataSourceInfo getInfo()
    {
        return dataSourceInfo;
    }

    @Override
    public synchronized boolean isConnected()
    {
        return connection != null;
    }

    @Override
    public void invalidateConnection(DBRProgressMonitor monitor)
        throws DBException
    {
        if (connection == null) {
            connection = openConnection();
            return;
        }

        if (!JDBCUtils.isConnectionAlive(connection)) {
            close();
            connection = openConnection();
        }
    }

    @Override
    public synchronized void initialize(DBRProgressMonitor monitor)
        throws DBException
    {
        JDBCExecutionContext context = openContext(monitor, DBCExecutionPurpose.META, CoreMessages.model_html_read_database_meta_data);
        try {
            dataSourceInfo = makeInfo(context.getMetaData());
        } catch (SQLException ex) {
            throw new DBException("Error getting JDBC meta data", ex);
        }
        finally {
            context.close();
        }
    }

    @Override
    public void close()
    {
        // [JDBC] Need sync here because real connection close could take some time
        // while UI may invoke callbacks to operate with connection
        synchronized (this) {
            if (connection != null) {
                try {
                    if (!connection.isClosed()) {
                        connection.close();
                    }
                }
                catch (SQLException ex) {
                    log.error(ex);
                }
            }
            connection = null;
        }
        QMUtils.getDefaultHandler().handleSessionEnd(this);
    }

    @Override
    @Property(viewable = true, order = 1)
    public String getName()
    {
        return container.getName();
    }

    @Override
    public String getDescription()
    {
        return container.getDescription();
    }

    @Override
    public DBSObject getParentObject()
    {
        return container;
    }

    @Override
    public boolean isPersisted()
    {
        return true;
    }

    @Override
    public boolean refreshObject(DBRProgressMonitor monitor) throws DBException {
        this.dataSourceInfo = null;
        return true;
    }

    @Override
    public DBCQueryTransformer createQueryTransformer(DBCQueryTransformType type)
    {
        if (type == DBCQueryTransformType.ORDER_BY) {

        } else if (type == DBCQueryTransformType.FILTER) {

        }
        return null;
    }

    @Override
    public DBSDataType resolveDataType(DBRProgressMonitor monitor, String typeFullName) throws DBException
    {
        return getDataType(typeFullName);
    }

    /////////////////////////////////////////////////
    // Overridable functions

    protected DBPDataSourceInfo makeInfo(JDBCDatabaseMetaData metaData)
    {
        return new JDBCDataSourceInfo(metaData);
    }

}
