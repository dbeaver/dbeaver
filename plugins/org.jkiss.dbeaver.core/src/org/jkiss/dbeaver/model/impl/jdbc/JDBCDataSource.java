/*
 * Copyright (C) 2010-2013 Serge Rieder
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

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCConnectionHolder;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.exec.JDBCConnectionImpl;
import org.jkiss.dbeaver.model.impl.sql.JDBCSQLDialect;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLDataSource;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLState;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
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
        SQLDataSource,
        DBPDataTypeProvider,
        DBPErrorAssistant,
        DBPRefreshableObject,
        DBSObject,
        DBSObjectContainer,
        DBCQueryTransformProvider
{
    private final DBSDataSourceContainer container;

    private final JDBCExecutionContext executionContext;
    private volatile JDBCExecutionContext metaContext;
    protected volatile DBPDataSourceInfo dataSourceInfo;
    protected volatile SQLDialect sqlDialect;

    public JDBCDataSource(DBRProgressMonitor monitor, DBSDataSourceContainer container)
        throws DBException
    {
        this.container = container;
        this.executionContext = new JDBCExecutionContext(this, "Main connection", false);
        this.executionContext.connect(monitor);
    }

    protected JDBCConnectionHolder openConnection(DBRProgressMonitor monitor, String purpose)
        throws DBCException
    {
        // It MUST be a JDBC driver
        Driver driverInstance;
        try {
            driverInstance = getDriverInstance(monitor);
        } catch (DBException e) {
            throw new DBCException("Can't create driver instance", e);
        }

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
                for (Map.Entry<Object,Object> prop : driverProperties.entrySet()) {
                    connectProps.setProperty(CommonUtils.toString(prop.getKey()), CommonUtils.toString(prop.getValue()));
                }
            }
        }

        DBPConnectionInfo connectionInfo = container.getActualConnectionInfo();
        if (connectionInfo.getProperties() != null) {
            for (Map.Entry<Object,Object> prop : connectionInfo.getProperties().entrySet()) {
                connectProps.setProperty(CommonUtils.toString(prop.getKey()), CommonUtils.toString(prop.getValue()));
            }
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
            throw new DBCException(ex, this);
        }
        catch (Throwable e) {
            throw new DBCException("Unexpected driver error occurred while connecting to database", e);
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
    @Nullable
    protected Map<String, String> getInternalConnectionProperties()
    {
        return null;
    }

    @Override
    public JDBCSession openSession(DBRProgressMonitor monitor, DBCExecutionPurpose purpose, String taskTitle)
    {
        if (purpose == DBCExecutionPurpose.META && metaContext != null) {
            return createConnection(monitor, this.metaContext, purpose, taskTitle);
        }
        return createConnection(monitor, executionContext, purpose, taskTitle);
    }

    @Override
    public DBCExecutionContext openIsolatedContext(DBRProgressMonitor monitor, String purpose) throws DBCException
    {
        return new JDBCExecutionContext(this, purpose, true);
    }

    protected JDBCConnectionImpl createConnection(
        DBRProgressMonitor monitor,
        JDBCExecutionContext context,
        DBCExecutionPurpose purpose,
        String taskTitle)
    {
        return new JDBCConnectionImpl(context, monitor, purpose, taskTitle);
    }

    @NotNull
    @Override
    public DBSDataSourceContainer getContainer()
    {
        return container;
    }

    @Override
    public DBPDataSourceInfo getInfo()
    {
        return dataSourceInfo;
    }

    @Override
    public SQLDialect getSQLDialect() {
        return sqlDialect;
    }

    @Override
    public String getContextName() {
        return executionContext.getContextName();
    }

    @Override
    public boolean isConnected()
    {
        return executionContext.isConnected();
    }

    @Override
    public void invalidateContext(DBRProgressMonitor monitor)
        throws DBException
    {
        this.executionContext.invalidateContext(monitor);
        if (metaContext != null && metaContext.isConnected()) {
            metaContext.invalidateContext(monitor);
        }
    }

    @Override
    public synchronized void initialize(DBRProgressMonitor monitor)
        throws DBException
    {
        if (container.getPreferenceStore().getBoolean(DBeaverPreferences.META_SEPARATE_CONNECTION)) {
            this.metaContext = new JDBCExecutionContext(this, "Metadata reader", false);
            this.metaContext.connect(monitor, true, null);
        }
        JDBCSession session = openSession(monitor, DBCExecutionPurpose.META, CoreMessages.model_html_read_database_meta_data);
        try {
            JDBCDatabaseMetaData metaData = session.getMetaData();
            dataSourceInfo = createDataSourceInfo(metaData);
            sqlDialect = createSQLDialect(metaData);
        } catch (SQLException ex) {
            throw new DBException("Error getting JDBC meta data", ex, this);
        }
        finally {
            session.close();
        }
    }

    @Override
    public void close()
    {
        // [JDBC] Need sync here because real connection close could take some time
        // while UI may invoke callbacks to operate with connection
        synchronized (this) {
            executionContext.close();
            if (metaContext != null) {
                metaContext.close();
            }
        }
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

    @Nullable
    @Override
    public DBCQueryTransformer createQueryTransformer(DBCQueryTransformType type)
    {
//        if (type == DBCQueryTransformType.ORDER_BY) {
//
//        } else if (type == DBCQueryTransformType.FILTER) {
//
//        }
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

    public DBPDataKind resolveDataKind(String typeName, int valueType)
    {
        return getDataKind(typeName, valueType);
    }

    public static DBPDataKind getDataKind(String typeName, int valueType)
    {
        switch (getValueTypeByTypeName(typeName, valueType)) {
            case Types.BOOLEAN:
                return DBPDataKind.BOOLEAN;
            case Types.CHAR:
            case Types.VARCHAR:
            case Types.NVARCHAR:
            case Types.LONGVARCHAR:
            case Types.LONGNVARCHAR:
                return DBPDataKind.STRING;
            case Types.BIGINT:
            case Types.DECIMAL:
            case Types.DOUBLE:
            case Types.FLOAT:
            case Types.INTEGER:
            case Types.NUMERIC:
            case Types.REAL:
            case Types.SMALLINT:
                return DBPDataKind.NUMERIC;
            case Types.BIT:
                return DBPDataKind.BOOLEAN;
            case Types.TINYINT:
                if (typeName.toLowerCase().contains("bool")) {
                    // Declared as numeric but actually it's a boolean
                    return DBPDataKind.BOOLEAN;
                } else {
                    return DBPDataKind.NUMERIC;
                }
            case Types.DATE:
            case Types.TIME:
            case Types.TIMESTAMP:
                return DBPDataKind.DATETIME;
            case Types.BINARY:
            case Types.VARBINARY:
            case Types.LONGVARBINARY:
                return DBPDataKind.BINARY;
            case Types.BLOB:
            case Types.CLOB:
            case Types.NCLOB:
                return DBPDataKind.LOB;
            case Types.SQLXML:
                return DBPDataKind.LOB;
            case Types.STRUCT:
                return DBPDataKind.STRUCT;
            case Types.ARRAY:
                return DBPDataKind.ARRAY;
            case Types.ROWID:
                return DBPDataKind.ROWID;
            case Types.REF:
                return DBPDataKind.REFERENCE;

        }
        return DBPDataKind.UNKNOWN;
    }

    @Override
    public DBSDataType resolveDataType(DBRProgressMonitor monitor, String typeFullName) throws DBException
    {
        return getDataType(typeFullName);
    }

    @Override
    public String getDefaultDataType(DBPDataKind dataKind)
    {
        switch (dataKind) {
            case BOOLEAN: return "BOOLEAN";
            case NUMERIC: return "NUMERIC";
            case STRING: return "VARCHAR";
            case DATETIME: return "TIMESTAMP";
            case BINARY: return "BLOB";
            case LOB: return "BLOB";
            case STRUCT: return "VARCHAR";
            case ARRAY: return "VARCHAR";
            case OBJECT: return "VARCHAR";
            case REFERENCE: return "VARCHAR";
            case ROWID: return "ROWID";
            default: return "VARCHAR";
        }
    }

    /////////////////////////////////////////////////
    // Overridable functions

    protected DBPDataSourceInfo createDataSourceInfo(JDBCDatabaseMetaData metaData)
    {
        return new JDBCDataSourceInfo(metaData);
    }

    protected SQLDialect createSQLDialect(JDBCDatabaseMetaData metaData)
    {
        return new JDBCSQLDialect(this, "JDBC", metaData);
    }

    /////////////////////////////////////////////////
    // Error assistance

    @Override
    public ErrorType discoverErrorType(DBException error)
    {
        String sqlState = error.getDatabaseState();
        if (SQLState.SQL_08000.getCode().equals(sqlState) ||
            SQLState.SQL_08003.getCode().equals(sqlState) ||
            SQLState.SQL_08S01.getCode().equals(sqlState))
        {
            return ErrorType.CONNECTION_LOST;
        }
        return ErrorType.NORMAL;
    }

}
