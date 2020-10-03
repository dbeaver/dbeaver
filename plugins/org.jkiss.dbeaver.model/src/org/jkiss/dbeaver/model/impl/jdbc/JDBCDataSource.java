/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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

import org.eclipse.core.runtime.IAdaptable;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.auth.DBAAuthCredentials;
import org.jkiss.dbeaver.model.auth.DBAAuthModel;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCFactory;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.exec.JDBCConnectionImpl;
import org.jkiss.dbeaver.model.impl.jdbc.exec.JDBCFactoryDefault;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLState;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSInstanceContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

import java.net.SocketException;
import java.sql.*;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * JDBC data source
 */
public abstract class JDBCDataSource
    implements
        DBPDataSource,
        DBPDataTypeProvider,
        DBPErrorAssistant,
        DBPRefreshableObject,
        DBSObject,
        DBSObjectContainer,
        DBSInstanceContainer,
        DBCQueryTransformProvider,
        IAdaptable
{
    private static final Log log = Log.getLog(JDBCDataSource.class);

    @NotNull
    private final DBPDataSourceContainer container;
    @NotNull
    protected volatile DBPDataSourceInfo dataSourceInfo;
    protected final SQLDialect sqlDialect;
    protected final JDBCFactory jdbcFactory;
    private JDBCRemoteInstance defaultRemoteInstance;

    private int databaseMajorVersion;
    private int databaseMinorVersion;

    protected JDBCDataSource(@NotNull DBRProgressMonitor monitor, @NotNull DBPDataSourceContainer container, @NotNull SQLDialect dialect)
        throws DBException
    {
        this(monitor, container, dialect, true);
    }

    protected JDBCDataSource(@NotNull DBRProgressMonitor monitor, @NotNull DBPDataSourceContainer container, @NotNull SQLDialect dialect, boolean initContext)
        throws DBException
    {
        this.dataSourceInfo = new JDBCDataSourceInfo(container);
        this.sqlDialect = dialect;
        this.jdbcFactory = createJdbcFactory();
        this.container = container;
        if (initContext) {
            initializeRemoteInstance(monitor);
        }
    }

    protected void initializeRemoteInstance(@NotNull DBRProgressMonitor monitor) throws DBException {
        this.defaultRemoteInstance = new JDBCRemoteInstance(monitor, this, true);
    }

    protected Connection openConnection(@NotNull DBRProgressMonitor monitor, @Nullable JDBCExecutionContext context, @NotNull String purpose)
        throws DBCException
    {
        // It MUST be a JDBC driver
        Driver driverInstance = null;
        DBPDriver driver = getContainer().getDriver();
        if (driver.isInstantiable() && !CommonUtils.isEmpty(driver.getDriverClassName())) {
            try {
                driverInstance = getDriverInstance(monitor);
            } catch (DBException e) {
                throw new DBCConnectException("Can't create driver instance", e, this);
            }
        } else {
            if (!CommonUtils.isEmpty(driver.getDriverClassName())) {
                try {
                    driver.loadDriver(monitor);
                    Class.forName(driver.getDriverClassName(), true, driver.getClassLoader());
                } catch (Exception e) {
                    throw new DBCException("Driver class '" + driver.getDriverClassName() + "' not found", e);
                }
            }
        }

        DBPConnectionConfiguration connectionInfo = new DBPConnectionConfiguration(container.getActualConnectionConfiguration());
        Properties connectProps = getAllConnectionProperties(monitor, context, purpose, connectionInfo);

        final JDBCConnectionConfigurer connectionConfigurer = GeneralUtils.adapt(this, JDBCConnectionConfigurer.class);

        DBAAuthModel authModel = connectionInfo.getAuthModel();

        // Obtain connection
        try {
            if (connectionConfigurer != null) {
                connectionConfigurer.beforeConnection(monitor, connectionInfo, connectProps);
            }
            final String url = getConnectionURL(connectionInfo);
            if (driverInstance != null) {
                try {
                    if (!driverInstance.acceptsURL(url)) {
                        // Just write a warning in log. Some drivers are poorly coded and always returns false here.
                        log.error("Bad URL: " + url);
                    }
                } catch (Throwable e) {
                    log.debug("Error in " + driverInstance.getClass().getName() + ".acceptsURL() - " + url, e);
                }
            }
            monitor.subTask("Connecting " + purpose);
            Connection[] connection = new Connection[1];
            Exception[] error = new Exception[1];
            int openTimeout = getContainer().getPreferenceStore().getInt(ModelPreferences.CONNECTION_OPEN_TIMEOUT);
            final Driver driverInstanceFinal = driverInstance;

            try {
                DBAAuthCredentials credentials = authModel.loadCredentials(getContainer(), connectionInfo);
                authModel.initAuthentication(monitor, this, credentials, connectionInfo, connectProps);
            } catch (DBException e) {
                throw new DBCException("Authentication error: " + e.getMessage(), e);
            }

            DBRRunnableWithProgress connectTask = monitor1 -> {
                try {
                    if (driverInstanceFinal == null) {
                        connection[0] = DriverManager.getConnection(url, connectProps);
                    } else {
                        connection[0] = driverInstanceFinal.connect(url, connectProps);
                    }
                } catch (Exception e) {
                    error[0] = e;
                } finally {
                    if (connectionConfigurer != null) {
                        try {
                            connectionConfigurer.afterConnection(monitor, connectionInfo, connectProps, connection[0], error[0]);
                        } catch (Exception e) {
                            log.debug(e);
                        }
                    }
                }
            };

            boolean openTaskFinished;
            try {
                if (openTimeout <= 0) {
                    openTaskFinished = true;
                    connectTask.run(monitor);
                } else {
                    openTaskFinished = RuntimeUtils.runTask(connectTask, "Opening database connection", openTimeout + 2000);
                }
            } finally {
                authModel.endAuthentication(container, connectionInfo, connectProps);
            }

            if (error[0] != null) {
                throw error[0];
            }
            if (!openTaskFinished) {
                throw new DBCException("Connection has timed out");
            }
            if (connection[0] == null) {
                throw new DBCException("Null connection returned");
            }

            // Set read-only flag
            if (container.isConnectionReadOnly() && !isConnectionReadOnlyBroken()) {
                connection[0].setReadOnly(true);
            }

            return connection[0];
        }
        catch (SQLException ex) {
            throw new DBCConnectException(ex.getMessage(), ex, this);
        }
        catch (DBCException ex) {
            throw ex;
        }
        catch (Throwable e) {
            throw new DBCConnectException("Unexpected driver error occurred while connecting to the database", e);
        }
    }

    protected void fillConnectionProperties(DBPConnectionConfiguration connectionInfo, Properties connectProps) {
        {
            // Use driver properties
            final Map<String, Object> driverProperties = container.getDriver().getConnectionProperties();
            for (Map.Entry<String, Object> prop : driverProperties.entrySet()) {
                connectProps.setProperty(prop.getKey(), CommonUtils.toString(prop.getValue()));
            }
        }

        for (Map.Entry<String, String> prop : connectionInfo.getProperties().entrySet()) {
            connectProps.setProperty(CommonUtils.toString(prop.getKey()), CommonUtils.toString(prop.getValue()));
        }
    }

    protected Properties getAllConnectionProperties(@NotNull DBRProgressMonitor monitor, JDBCExecutionContext context, String purpose, DBPConnectionConfiguration connectionInfo) throws DBCException {
        // Set properties
        Properties connectProps = new Properties();

        {
            // Use properties defined by datasource itself
            Map<String,String> internalProps = getInternalConnectionProperties(monitor, getContainer().getDriver(), context, purpose, connectionInfo);
            if (internalProps != null) {
                connectProps.putAll(internalProps);
            }
        }

        fillConnectionProperties(connectionInfo, connectProps);

        return connectProps;
    }

    protected String getConnectionURL(DBPConnectionConfiguration connectionInfo) {
        String url = connectionInfo.getUrl();
        if (CommonUtils.isEmpty(url)) {
            // It can be empty in some cases (e.g. when we create connections from command line command)
            url = getContainer().getDriver().getDataSourceProvider().getConnectionURL(getContainer().getDriver(), connectionInfo);
        }
        return url;
    }

    /**
     * Closes JDBC connection.
     * Do actual close in separate thread.
     * After ModelPreferences.CONNECTION_CLOSE_TIMEOUT delay returns false.
     * @return true on successful connection close
     */
    public boolean closeConnection(final Connection connection, String purpose, boolean doRollback)
    {
        if (connection != null) {
            // Close datasource (in async task)
            return RuntimeUtils.runTask(monitor -> {
                if (doRollback) {
                    try {
                        // If we in transaction - rollback it.
                        // Any valuable transaction changes should be committed by UI
                        // so here we do it just in case to avoid error messages on close with open transaction
                        if (!connection.isClosed() && !connection.getAutoCommit()) {
                            connection.rollback();
                        }
                    } catch (Throwable e) {
                        // Do not write warning because connection maybe broken before the moment of close
                        log.debug("Error closing active transaction", e);
                    }
                }
                try {
                    connection.close();
                }
                catch (Throwable ex) {
                    log.debug("Error closing connection", ex);
                }
            }, "Close JDBC connection (" + purpose + ")",
                getContainer().getPreferenceStore().getInt(ModelPreferences.CONNECTION_CLOSE_TIMEOUT));
        } else {
            log.debug("Null connection parameter");
            return true;
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

    protected void initializeContextState(@NotNull DBRProgressMonitor monitor, @NotNull JDBCExecutionContext context, JDBCExecutionContext initFrom) throws DBException {

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

    @Override
    public Object getDataSourceFeature(String featureId) {
        return null;
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
    public JDBCRemoteInstance getDefaultInstance() {
        return defaultRemoteInstance;
    }

    @NotNull
    @Override
    public List<? extends JDBCRemoteInstance> getAvailableInstances() {
        JDBCRemoteInstance defaultInstance = getDefaultInstance();
        return defaultInstance == null ?
            Collections.emptyList() :
            Collections.singletonList(defaultInstance);
    }

    @Override
    public void shutdown(DBRProgressMonitor monitor)
    {
        for (JDBCRemoteInstance instance : getAvailableInstances()) {
            Object exclusiveLock = instance.getExclusiveLock().acquireExclusiveLock();
            try {
                monitor.subTask("Disconnect from '" + instance.getName() + "'");
                instance.shutdown(monitor);
                monitor.worked(1);
            } finally {
                instance.getExclusiveLock().releaseExclusiveLock(exclusiveLock);
            }
        }
        defaultRemoteInstance = null;
    }

    @Override
    public void initialize(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
        getDefaultInstance().initializeMetaContext(monitor);
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, ModelMessages.model_jdbc_read_database_meta_data)) {
            JDBCDatabaseMetaData metaData = session.getMetaData();

            readDatabaseServerVersion(metaData);

            if (this.sqlDialect instanceof JDBCSQLDialect) {
                try {
                    ((JDBCSQLDialect) this.sqlDialect).initDriverSettings(this, metaData);
                } catch (Throwable e) {
                    log.error("Error initializing dialect driver settings", e);
                }
            }

            try {
                dataSourceInfo = createDataSourceInfo(monitor, metaData);
            } catch (Throwable e) {
                log.error("Error obtaining database info", e);
            }
        } catch (SQLException ex) {
            throw new DBException("Error getting JDBC meta data", ex, this);
        } finally {
            if (dataSourceInfo == null) {
                log.warn("NULL datasource info was created");
                dataSourceInfo = new JDBCDataSourceInfo(container);
            }
        }
    }

    protected void readDatabaseServerVersion(DatabaseMetaData metaData) {
        try {
            databaseMajorVersion = metaData.getDatabaseMajorVersion();
            databaseMinorVersion = metaData.getDatabaseMinorVersion();
        } catch (Throwable e) {
            log.error("Error determining server version", e);
        }
    }

    public boolean isServerVersionAtLeast(int major, int minor) {
        if (databaseMajorVersion < major) {
            return false;
        } else if (databaseMajorVersion == major && databaseMinorVersion < minor) {
            return false;
        }
        return true;
    }

    public boolean isDriverVersionAtLeast(int major, int minor) {
        try {
            Driver driver = getDriverInstance(new VoidProgressMonitor());
            int majorVersion = driver.getMajorVersion();
            if (majorVersion < major) {
                return false;
            } else if (majorVersion == major && driver.getMinorVersion() < minor) {
                return false;
            }
            return true;
        } catch (DBException e) {
            log.debug("Can't obtain driver instance", e);
            return false;
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
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        this.dataSourceInfo = new JDBCDataSourceInfo(container);
        return this;
    }

    protected JDBCExecutionContext createExecutionContext(JDBCRemoteInstance instance, String type) {
        return new JDBCExecutionContext(instance, type);
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
            case Types.TIME_WITH_TIMEZONE:
            case Types.TIMESTAMP:
            case Types.TIMESTAMP_WITH_TIMEZONE:
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
    public DBSDataType getLocalDataType(int typeID) {
        for (DBSDataType dataType : getLocalDataTypes()) {
            if (dataType.getTypeID() == typeID) {
                return dataType;
            }
        }
        return null;
    }

    @Override
    public String getDefaultDataTypeName(@NotNull DBPDataKind dataKind)
    {
        String typeName = getStandardSQLDataTypeName(dataKind);
        DBSDataType dataType = getLocalDataType(typeName);
        if (dataType == null) {
            // No such data type
            // Try to find first data type of this kind
            for (DBSDataType type : getLocalDataTypes()) {
                if (type.getDataKind() == dataKind) {
                    return type.getName();
                }
            }
        }
        return typeName;
    }

    @NotNull
    private String getStandardSQLDataTypeName(@NotNull DBPDataKind dataKind) {
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

    @Nullable
    protected Driver getDriverInstance(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
        return container.getDriver().getDriverInstance(monitor);
    }

    /**
     * Could be overridden by extenders. May contain any additional connection properties.
     * Note: these properties may be overwritten by connection advanced properties.
     * @return predefined connection properties
     */
    @Nullable
    protected Map<String, String> getInternalConnectionProperties(DBRProgressMonitor monitor, DBPDriver driver, JDBCExecutionContext context, String purpose, DBPConnectionConfiguration connectionInfo)
        throws DBCException
    {
        return null;
    }

    protected DBPDataSourceInfo createDataSourceInfo(DBRProgressMonitor monitor, @NotNull JDBCDatabaseMetaData metaData)
    {
        return new JDBCDataSourceInfo(metaData);
    }

    @NotNull
    protected JDBCFactory createJdbcFactory() {
        return new JDBCFactoryDefault();
    }

    /////////////////////////////////////////////////
    // Error assistance

    @Override
    public ErrorType discoverErrorType(@NotNull Throwable error)
    {
        String sqlState = SQLState.getStateFromException(error);
        if (sqlState != null) {
            if (SQLState.SQL_08000.getCode().equals(sqlState) ||
                    SQLState.SQL_08003.getCode().equals(sqlState) ||
                    SQLState.SQL_08006.getCode().equals(sqlState) ||
                    SQLState.SQL_08007.getCode().equals(sqlState) ||
                    SQLState.SQL_08S01.getCode().equals(sqlState)) {
                return ErrorType.CONNECTION_LOST;
            }
        }
        if (GeneralUtils.getRootCause(error) instanceof SocketException) {
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
    public ErrorPosition[] getErrorPosition(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext context, @NotNull String query, @NotNull Throwable error) {
        return null;
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter == DBCTransactionManager.class) {
            return adapter.cast(DBUtils.getDefaultContext(getDefaultInstance(), false));
        } else if (adapter == DBCQueryTransformProvider.class) {
            return adapter.cast(this);
        }
        return null;
    }

    /////////////////////////////////////////////////
    // DBDFormatSettings

    public void cancelStatementExecute(DBRProgressMonitor monitor, JDBCStatement statement) throws DBException {
        try {
            statement.cancel();
        }
        catch (SQLException e) {
            throw new DBException(e, this);
        }
    }

}
