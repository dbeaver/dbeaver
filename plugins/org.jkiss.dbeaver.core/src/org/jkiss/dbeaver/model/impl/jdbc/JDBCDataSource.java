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
import org.jkiss.dbeaver.model.exec.jdbc.JDBCConnectionHolder;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCConnector;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.impl.jdbc.exec.JDBCConnectionImpl;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.qm.QMUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

import java.sql.*;
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
    private JDBCConnectionHolder connection;

    protected DBPDataSourceInfo dataSourceInfo;

    public JDBCDataSource(DBRProgressMonitor monitor, DBSDataSourceContainer container)
        throws DBException
    {
        this.container = container;
        this.connection = openConnection(monitor);
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

    protected JDBCConnectionHolder openConnection(DBRProgressMonitor monitor)
        throws DBException
    {
        // It MUST be a JDBC driver
        Driver driverInstance = getDriverInstance(monitor);

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

            return new JDBCConnectionHolder(connection);
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

    protected Driver getDriverInstance(DBRProgressMonitor monitor)
        throws DBException
    {
        return Driver.class.cast(
            container.getDriver().getDriverInstance(
                RuntimeUtils.makeContext(monitor)
            ));
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
    public JDBCConnectionHolder getConnection()
    {
        return connection;
    }

    @Override
    public JDBCConnectionHolder openIsolatedConnection(DBRProgressMonitor monitor) throws SQLException {
        try {
            return openConnection(monitor);
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
        return createConnection(monitor, purpose, taskTitle, false);
    }

    @Override
    public DBCExecutionContext openIsolatedContext(DBRProgressMonitor monitor, DBCExecutionPurpose purpose, String taskTitle) {
        return createConnection(monitor, purpose, taskTitle, true);
    }

    protected JDBCConnectionImpl createConnection(DBRProgressMonitor monitor, DBCExecutionPurpose purpose, String taskTitle, boolean isolated)
    {
        return new JDBCConnectionImpl(this, monitor, purpose, taskTitle, isolated);
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
            connection = openConnection(monitor);
            return;
        }

        if (!JDBCUtils.isConnectionAlive(connection.getConnection())) {
            close();
            connection = openConnection(monitor);
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
                    if (!connection.getConnection().isClosed()) {
                        connection.getConnection().close();
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

    private static int getValueTypeByTypeName(String typeName, int valueType)
    {
        // [JDBC: SQLite driver uses VARCHAR value type for all LOBs]
        if (valueType == Types.OTHER || valueType == Types.VARCHAR) {
            if ("BLOB".equalsIgnoreCase(typeName)) {
                return Types.BLOB;
            } else if ("CLOB".equalsIgnoreCase(typeName)) {
                return Types.CLOB;
            } else if ("NCLOB".equalsIgnoreCase(typeName)) {
                return Types.NCLOB;
            }
        } else if (valueType == Types.BIT) {
            // Workaround for MySQL (and maybe others) when TINYINT(1) == BOOLEAN
            if ("TINYINT".equalsIgnoreCase(typeName)) {
                return Types.TINYINT;
            }
        }
        return valueType;
    }

    public DBSDataKind resolveDataKind(String typeName, int valueType)
    {
        return getDataKind(typeName, valueType);
    }

    public static DBSDataKind getDataKind(String typeName, int valueType)
    {
        switch (getValueTypeByTypeName(typeName, valueType)) {
            case Types.BOOLEAN:
                return DBSDataKind.BOOLEAN;
            case Types.CHAR:
            case Types.VARCHAR:
            case Types.NVARCHAR:
            case Types.LONGVARCHAR:
            case Types.LONGNVARCHAR:
                return DBSDataKind.STRING;
            case Types.BIGINT:
            case Types.DECIMAL:
            case Types.DOUBLE:
            case Types.FLOAT:
            case Types.INTEGER:
            case Types.NUMERIC:
            case Types.REAL:
            case Types.SMALLINT:
                return DBSDataKind.NUMERIC;
            case Types.BIT:
                return DBSDataKind.BOOLEAN;
            case Types.TINYINT:
                if (typeName.toLowerCase().contains("bool")) {
                    // Declared as numeric but actually it's a boolean
                    return DBSDataKind.BOOLEAN;
                } else {
                    return DBSDataKind.NUMERIC;
                }
            case Types.DATE:
            case Types.TIME:
            case Types.TIMESTAMP:
                return DBSDataKind.DATETIME;
            case Types.BINARY:
            case Types.VARBINARY:
            case Types.LONGVARBINARY:
                return DBSDataKind.BINARY;
            case Types.BLOB:
            case Types.CLOB:
            case Types.NCLOB:
                return DBSDataKind.LOB;
            case Types.SQLXML:
                return DBSDataKind.LOB;
            case Types.STRUCT:
                return DBSDataKind.STRUCT;
            case Types.ARRAY:
                return DBSDataKind.ARRAY;
            case Types.ROWID:
                return DBSDataKind.ROWID;
            case Types.REF:
                return DBSDataKind.REFERENCE;

        }
        return DBSDataKind.UNKNOWN;
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
