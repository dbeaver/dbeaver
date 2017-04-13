/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.model.impl.jdbc;

import org.eclipse.core.runtime.IAdaptable;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.data.DBDDataFormatterProfile;
import org.jkiss.dbeaver.model.data.DBDPreferences;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCFactory;
import org.jkiss.dbeaver.model.impl.jdbc.data.handlers.JDBCObjectValueHandler;
import org.jkiss.dbeaver.model.impl.jdbc.exec.JDBCFactoryDefault;
import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.exec.JDBCConnectionImpl;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.MonitorRunnableContext;
import org.jkiss.dbeaver.model.sql.SQLDataSource;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLState;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.sql.*;
import java.util.*;

/**
 * JDBC data source
 */
public abstract class JDBCDataSource
    implements
        DBPDataSource,
        SQLDataSource,
        DBPDataTypeProvider,
        DBPErrorAssistant,
        DBPRefreshableObject,
        DBDPreferences,
        DBSObject,
        DBSObjectContainer,
        DBCQueryTransformProvider,
        IAdaptable
{
    private static final Log log = Log.getLog(JDBCDataSource.class);

    @NotNull
    private final DBPDataSourceContainer container;
    @NotNull
    protected final JDBCExecutionContext executionContext;
    @Nullable
    protected JDBCExecutionContext metaContext;
    @NotNull
    protected final List<JDBCExecutionContext> allContexts = new ArrayList<>();
    @NotNull
    protected volatile DBPDataSourceInfo dataSourceInfo;
    protected volatile SQLDialect sqlDialect;
    protected final JDBCFactory jdbcFactory;

    public JDBCDataSource(@NotNull DBRProgressMonitor monitor, @NotNull DBPDataSourceContainer container)
        throws DBException
    {
        this.dataSourceInfo = new JDBCDataSourceInfo(container);
        this.sqlDialect = BasicSQLDialect.INSTANCE;
        this.jdbcFactory = createJdbcFactory();
        this.container = container;
        this.executionContext = new JDBCExecutionContext(this, "Main");
        this.executionContext.connect(monitor, null, null, false);
    }

    protected Connection openConnection(@NotNull DBRProgressMonitor monitor, @NotNull String purpose)
        throws DBCException
    {
        // It MUST be a JDBC driver
        Driver driverInstance;
        try {
            driverInstance = getDriverInstance(monitor);
        } catch (DBException e) {
            throw new DBCConnectException("Can't create driver instance", e, this);
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
            for (Map.Entry<Object,Object> prop : driverProperties.entrySet()) {
                connectProps.setProperty(CommonUtils.toString(prop.getKey()), CommonUtils.toString(prop.getValue()));
            }
        }

        DBPConnectionConfiguration connectionInfo = container.getActualConnectionConfiguration();
        for (Map.Entry<Object,Object> prop : connectionInfo.getProperties().entrySet()) {
            connectProps.setProperty(CommonUtils.toString(prop.getKey()), CommonUtils.toString(prop.getValue()));
        }
        if (!CommonUtils.isEmpty(connectionInfo.getUserName())) {
            connectProps.put(DBConstants.DATA_SOURCE_PROPERTY_USER, getConnectionUserName(connectionInfo));
        }
        if (!CommonUtils.isEmpty(connectionInfo.getUserPassword())) {
            connectProps.put(DBConstants.DATA_SOURCE_PROPERTY_PASSWORD, getConnectionUserPassword(connectionInfo));
        }
        for (Iterator<Object> iter = connectProps.keySet().iterator(); iter.hasNext(); ) {
            if (CommonUtils.toString(iter.next()).startsWith(DBConstants.INTERNAL_PROP_PREFIX)) {
                iter.remove();
            }
        }
        // Obtain connection
        try {
            if (driverInstance != null) {
                try {
                    if (!driverInstance.acceptsURL(connectionInfo.getUrl())) {
                        // Just write a warning in log. Some drivers are poorly coded and always returns false here.
                        log.error("Bad URL: " + connectionInfo.getUrl());
                    }
                } catch (Throwable e) {
                    log.debug("Error in " + driverInstance.getClass().getName() + ".acceptsURL()", e);
                }
            }
            Connection connection;
            if (driverInstance == null) {
                connection = DriverManager.getConnection(connectionInfo.getUrl(), connectProps);
            } else {
                connection = driverInstance.connect(connectionInfo.getUrl(), connectProps);
            }
            if (connection == null) {
                throw new DBCException("Null connection returned");
            }

            // Set read-only flag
            if (container.isConnectionReadOnly() && !isConnectionReadOnlyBroken()) {
                connection.setReadOnly(true);
            }

            return connection;
        }
        catch (SQLException ex) {
            throw new DBCConnectException(ex.getMessage(), ex, this);
        }
        catch (DBCException ex) {
            throw ex;
        }
        catch (Throwable e) {
            throw new DBCConnectException("Unexpected driver error occurred while connecting to database", e);
        }
    }

    protected void closeConnection(Connection connection)
    {
        if (connection != null) {
            try {
                // If we in transaction - rollback it.
                // Any valuable transaction changes should be commited by UI
                // so here we do it just in case to avoid error messages on close with open transaction
                if (!connection.getAutoCommit()) {
                    connection.rollback();
                }
            } catch (Throwable e) {
                // Do not write warning because connection maybe broken before the moment of close
                log.debug("Error closing transaction", e);
            }
            try {
                connection.close();
            }
            catch (Throwable ex) {
                log.error("Error closing connection", ex);
            }
        }
    }

/*
    @Override
    public JDBCSession openSession(DBRProgressMonitor monitor, DBCExecutionPurpose purpose, String taskTitle)
    {
        if (metaContext != null && (purpose == DBCExecutionPurpose.META || purpose == DBCExecutionPurpose.META_DDL)) {
            return createConnection(monitor, this.metaContext, purpose, taskTitle);
        }
        return createConnection(monitor, executionContext, purpose, taskTitle);
    }
*/

    @NotNull
    @Override
    public DBCExecutionContext openIsolatedContext(@NotNull DBRProgressMonitor monitor, @NotNull String purpose) throws DBException
    {
        JDBCExecutionContext context = new JDBCExecutionContext(this, purpose);
        context.connect(monitor, null, null, true);
        return context;
    }

    protected void initializeContextState(@NotNull DBRProgressMonitor monitor, @NotNull JDBCExecutionContext context, boolean setActiveObject) throws DBCException {

    }

    @NotNull
    protected JDBCConnectionImpl createConnection(
        @NotNull DBRProgressMonitor monitor,
        @NotNull JDBCExecutionContext context,
        @NotNull DBCExecutionPurpose purpose,
        @NotNull String taskTitle)
    {
        return new JDBCConnectionImpl(context, monitor, purpose, taskTitle);
    }

    @NotNull
    @Override
    public DBPDataSourceContainer getContainer()
    {
        return container;
    }

    @NotNull
    @Override
    public DBPDataSourceInfo getInfo()
    {
        return dataSourceInfo;
    }

    @NotNull
    @Override
    public SQLDialect getSQLDialect() {
        return sqlDialect;
    }

    @NotNull
    public JDBCFactory getJdbcFactory() {
        return jdbcFactory;
    }

    @NotNull
    @Override
    public synchronized JDBCExecutionContext getDefaultContext(boolean meta) {
        if (metaContext != null && meta) {
            return this.metaContext;
        }
        return executionContext;
    }

    @NotNull
    @Override
    public Collection<JDBCExecutionContext> getAllContexts() {
        return allContexts;
    }

    @Override
    public void initialize(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
        if (!container.getDriver().isEmbedded() && container.getPreferenceStore().getBoolean(ModelPreferences.META_SEPARATE_CONNECTION)) {
            synchronized (this) {
                this.metaContext = new JDBCExecutionContext(this, "Metadata");
                this.metaContext.connect(monitor, true, null, false);
            }
        }
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, ModelMessages.model_jdbc_read_database_meta_data)) {
            JDBCDatabaseMetaData metaData = session.getMetaData();
            try {
                sqlDialect = createSQLDialect(metaData);
            } catch (Exception e) {
                log.error("Error creating SQL dialect", e);
            }
            try {
                dataSourceInfo = createDataSourceInfo(metaData);
            } catch (Exception e) {
                log.error("Error obtaining database info");
            }
        } catch (SQLException ex) {
            throw new DBException("Error getting JDBC meta data", ex, this);
        } finally {
            if (sqlDialect == null) {
                log.warn("NULL SQL dialect was created");
                sqlDialect = BasicSQLDialect.INSTANCE;
            }
            if (dataSourceInfo == null) {
                log.warn("NULL datasource info was created");
                dataSourceInfo = new JDBCDataSourceInfo(container);
            }
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

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName()
    {
        return container.getName();
    }

    @Nullable
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
    public boolean refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        this.dataSourceInfo = new JDBCDataSourceInfo(container);
        return true;
    }

    @Nullable
    @Override
    public DBCQueryTransformer createQueryTransformer(@NotNull DBCQueryTransformType type)
    {
//        if (type == DBCQueryTransformType.ORDER_BY) {
//
//        } else if (type == DBCQueryTransformType.FILTER) {
//
//        }
        return null;
    }

    private static int getValueTypeByTypeName(@NotNull String typeName, int valueType)
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

    @NotNull
    public DBPDataKind resolveDataKind(@NotNull String typeName, int valueType)
    {
        return getDataKind(typeName, valueType);
    }

    @NotNull
    public static DBPDataKind getDataKind(@NotNull String typeName, int valueType)
    {
        switch (getValueTypeByTypeName(typeName, valueType)) {
            case Types.BOOLEAN:
                return DBPDataKind.BOOLEAN;
            case Types.CHAR:
            case Types.VARCHAR:
            case Types.NVARCHAR:
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
            case Types.TINYINT:
                if (typeName.toLowerCase().contains("bool")) {
                    // Declared as numeric but actually it's a boolean
                    return DBPDataKind.BOOLEAN;
                }
                return DBPDataKind.NUMERIC;
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
            case Types.LONGVARCHAR:
            case Types.LONGNVARCHAR:
                return DBPDataKind.CONTENT;
            case Types.SQLXML:
                return DBPDataKind.CONTENT;
            case Types.STRUCT:
                return DBPDataKind.STRUCT;
            case Types.ARRAY:
                return DBPDataKind.ARRAY;
            case Types.ROWID:
                return DBPDataKind.ROWID;
            case Types.REF:
                return DBPDataKind.REFERENCE;
            case Types.OTHER:
                // TODO: really?
                return DBPDataKind.OBJECT;
        }
        return DBPDataKind.UNKNOWN;
    }

    @Nullable
    @Override
    public DBSDataType resolveDataType(@NotNull DBRProgressMonitor monitor, @NotNull String typeFullName) throws DBException
    {
        return getLocalDataType(typeFullName);
    }

    @Override
    public String getDefaultDataTypeName(@NotNull DBPDataKind dataKind)
    {
        switch (dataKind) {
            case BOOLEAN: return "BOOLEAN";
            case NUMERIC: return "NUMERIC";
            case STRING: return "VARCHAR";
            case DATETIME: return "TIMESTAMP";
            case BINARY: return "BLOB";
            case CONTENT: return "BLOB";
            case STRUCT: return "VARCHAR";
            case ARRAY: return "VARCHAR";
            case OBJECT: return "VARCHAR";
            case REFERENCE: return "VARCHAR";
            case ROWID: return "ROWID";
            case ANY: return "VARCHAR";
            default: return "VARCHAR";
        }
    }

    /////////////////////////////////////////////////
    // Overridable functions

    protected boolean isConnectionReadOnlyBroken() {
        return false;
    }

    protected String getConnectionUserName(@NotNull DBPConnectionConfiguration connectionInfo)
    {
        return connectionInfo.getUserName();
    }

    protected String getConnectionUserPassword(@NotNull DBPConnectionConfiguration connectionInfo)
    {
        return connectionInfo.getUserPassword();
    }

    @Nullable
    protected Driver getDriverInstance(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
        return Driver.class.cast(
            container.getDriver().getDriverInstance(
                new MonitorRunnableContext(monitor)
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

    protected DBPDataSourceInfo createDataSourceInfo(@NotNull JDBCDatabaseMetaData metaData)
    {
        return new JDBCDataSourceInfo(metaData);
    }

    protected SQLDialect createSQLDialect(@NotNull JDBCDatabaseMetaData metaData)
    {
        return new JDBCSQLDialect(this, "JDBC", metaData);
    }

    @NotNull
    protected JDBCFactory createJdbcFactory() {
        return new JDBCFactoryDefault();
    }

    /////////////////////////////////////////////////
    // Error assistance

    @Override
    public ErrorType discoverErrorType(@NotNull DBException error)
    {
        String sqlState = error.getDatabaseState();
        if (SQLState.SQL_08000.getCode().equals(sqlState) ||
            SQLState.SQL_08003.getCode().equals(sqlState) ||
            SQLState.SQL_08S01.getCode().equals(sqlState))
        {
            return ErrorType.CONNECTION_LOST;
        }
        if (error instanceof DBCConnectException) {
            Throwable rootCause = GeneralUtils.getRootCause(error);
            if (rootCause instanceof ClassNotFoundException) {
                // Looks like bad driver configuration
                return ErrorType.DRIVER_CLASS_MISSING;
            }
        }

        return ErrorType.NORMAL;
    }

    @Nullable
    @Override
    public ErrorPosition[] getErrorPosition(@NotNull Throwable error) {
        return null;
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter == DBCTransactionManager.class) {
            return adapter.cast(executionContext);
        }
        return null;
    }

    /////////////////////////////////////////////////
    // DBDPreferences

    @Override
    public DBDDataFormatterProfile getDataFormatterProfile() {
        return container.getDataFormatterProfile();
    }

    @Override
    public void setDataFormatterProfile(DBDDataFormatterProfile formatterProfile) {
        container.setDataFormatterProfile(formatterProfile);
    }

    @NotNull
    @Override
    public DBDValueHandler getDefaultValueHandler() {
        return JDBCObjectValueHandler.INSTANCE;
    }
}
