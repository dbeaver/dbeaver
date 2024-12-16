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
package org.jkiss.dbeaver.ext.mssql.model;

import net.sf.jsqlparser.expression.NextValExpression;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectBody;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.ext.mssql.SQLServerConstants;
import org.jkiss.dbeaver.ext.mssql.SQLServerUtils;
import org.jkiss.dbeaver.ext.mssql.model.session.SQLServerSessionManager;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.access.DBAUserPasswordManager;
import org.jkiss.dbeaver.model.admin.sessions.DBAServerSessionManager;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.exec.jdbc.*;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCRemoteInstance;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.impl.net.SSLHandlerTrustStoreImpl;
import org.jkiss.dbeaver.model.impl.sql.QueryTransformerTop;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLQuery;
import org.jkiss.dbeaver.model.sql.parser.SQLSemanticProcessor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.BeanUtils;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

public class SQLServerDataSource extends JDBCDataSource implements DBSInstanceContainer, DBPObjectStatisticsCollector, DBPAdaptable, DBCQueryTransformProviderExt {

    private static final Log log = Log.getLog(SQLServerDataSource.class);
    private static final String PROP_ENCRYPT_SSL = "encrypt";

    // Delegate data type reading to the driver
    private final SystemDataTypeCache dataTypeCache = new SystemDataTypeCache();
    private final DatabaseCache databaseCache = new DatabaseCache();
    private final ServerLoginCache serverLoginCache = new ServerLoginCache();

    private boolean supportsColumnProperty;
    private String serverVersion;
    private volatile Boolean supportsIsExternalColumn;

    private volatile transient boolean hasStatistics;
    private final boolean isBabelfish;
    private boolean isSynapseDatabase;

    public SQLServerDataSource(DBRProgressMonitor monitor, DBPDataSourceContainer container)
        throws DBException
    {
        super(monitor, container, new SQLServerDialect());
        isBabelfish = SQLServerUtils.isDriverBabelfish(getContainer().getDriver());
    }

    public boolean supportsColumnProperty() {
        return supportsColumnProperty;
    }

    /**
     * @deprecated This method is intended to be used only within
     * the {@code visibleIf} attribute of a navigator tree node.
     * <p>
     * This method will be removed once #16366 is implemented.
     */
    @Deprecated(forRemoval = true)
    public boolean supportsExternalTables() {
        if (supportsIsExternalColumn != null) {
            return supportsIsExternalColumn;
        }
        try (JDBCSession session = DBUtils.openMetaSession(new VoidProgressMonitor(), this, "Determine external tables availability")) {
            return supportsExternalTables(session);
        } catch (DBCException ignored) {
            return false;
        }
    }

    public boolean supportsExternalTables(JDBCSession session) {
        if (supportsIsExternalColumn != null) {
            return supportsIsExternalColumn;
        }
        if (isBabelfish) {
            supportsIsExternalColumn = false;
            return false;
        }

        // The "is_external" column can be used to identify external tables support.
        // But not all SQL Server versions supports this column in the all_columns view
        // Sometimes checking the version does not work for some reason - see #15036
        // Let's check the existence of column directly at the database
        try {
            JDBCUtils.queryString(session, "SELECT TOP 1 is_external from sys.tables where 1<>1");
            this.supportsIsExternalColumn = true;
        } catch (Exception ignored) {
            this.supportsIsExternalColumn = false;
        }

        return supportsIsExternalColumn;
    }

    @Override
    protected DBPDataSourceInfo createDataSourceInfo(DBRProgressMonitor monitor, @NotNull JDBCDatabaseMetaData metaData)
    {
        SQLServerDataSourceInfo info = new SQLServerDataSourceInfo(this, metaData);
        if (isDataWarehouseServer(monitor)) {
            info.setSupportsResultSetScroll(false);
        }
        return info;
    }

    public boolean isDataWarehouseServer(DBRProgressMonitor monitor) {
        return getServerVersion(monitor).contains(SQLServerConstants.SQL_DW_SERVER_LABEL);
    }

    public String getServerVersion() {
        return serverVersion;
    }

    public boolean supportsTriggers() {
        return !isBabelfish && !isSynapseDatabase;
    }

    public boolean supportsSynonyms() {
        return !isBabelfish;
    }

    public boolean supportsSequences() {
        return !isBabelfish && !isSynapseDatabase;
    }

    public boolean isSynapseDatabase() {
        return isSynapseDatabase;
    }

    private String getServerVersion(DBRProgressMonitor monitor) {
        if (serverVersion == null) {
            try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Read server version")) {
                serverVersion = JDBCUtils.queryString(session, "SELECT @@VERSION");
            } catch (Exception e) {
                log.debug("Error reading SQL Server version: " + e.getMessage());
                serverVersion = "";
            }
        }
        return serverVersion;
    }

    public DatabaseCache getDatabaseCache() {
        return databaseCache;
    }

    @Association
    public List<SQLServerLogin> getLogins(@NotNull DBRProgressMonitor monitor) throws DBException {
        return serverLoginCache.getAllObjects(monitor, this);
    }

    @Association
    public SQLServerLogin getLogin(@NotNull DBRProgressMonitor monitor, @NotNull String loginName) throws DBException {
        return serverLoginCache.getObject(monitor, this, loginName);
    }

    public ServerLoginCache getServerLoginCache() {
        return serverLoginCache;
    }

    @NotNull
    @Override
    protected Properties getAllConnectionProperties(@NotNull DBRProgressMonitor monitor, JDBCExecutionContext context, String purpose, DBPConnectionConfiguration connectionInfo) throws DBCException {
        Properties properties = super.getAllConnectionProperties(monitor, context, purpose, connectionInfo);
        if (!getContainer().getPreferenceStore().getBoolean(ModelPreferences.META_CLIENT_NAME_DISABLE)) {// App name
            properties.put(
                SQLServerUtils.isDriverJtds(getContainer().getDriver()) ? SQLServerConstants.APPNAME_CLIENT_PROPERTY : SQLServerConstants.APPLICATION_NAME_CLIENT_PROPERTY,
                CommonUtils.truncateString(DBUtils.getClientApplicationName(getContainer(), context, purpose), 64));
        }

        if (SQLServerUtils.isDriverSqlServer(getContainer().getDriver())) {
            // Starting microsoft driver version 10.1 defaulted the encrypt option to true (previously false)
            // thus the driver will look for a certificate to validate unless trustServerCertificate is set to true
            // (i.e. do not validate, trust all when encrypt option is enabled)
            boolean trustCertificate = CommonUtils.getBoolean(
                connectionInfo.getProviderProperty(SQLServerConstants.PROP_SSL_TRUST_SERVER_CERTIFICATE),
                false);
            if (trustCertificate) {
                properties.put(SQLServerConstants.PROP_DRIVER_TRUST_SERVER_CERTIFICATE, Boolean.TRUE.toString());
            }
        }

        final DBWHandlerConfiguration sslConfig = getContainer().getActualConnectionConfiguration().getHandler(SQLServerConstants.HANDLER_SSL);
        if (sslConfig != null && sslConfig.isEnabled()) {
            initSSL(monitor, properties, sslConfig);
        } else if (CommonUtils.isEmpty(properties.getProperty(PROP_ENCRYPT_SSL))) {
            properties.setProperty(PROP_ENCRYPT_SSL, "false");
        }

        return properties;
    }

    private void initSSL(DBRProgressMonitor monitor, Properties properties, DBWHandlerConfiguration sslConfig) throws DBCException {
        monitor.subTask("Install SSL certificates");

        try {
//            SSLHandlerTrustStoreImpl.initializeTrustStore(monitor, this, sslConfig);
//            DBACertificateStorage certificateStorage = getContainer().getPlatform().getCertificateStorage();
//            String keyStorePath = certificateStorage.getKeyStorePath(getContainer(), "ssl").getAbsolutePath();

            properties.put(PROP_ENCRYPT_SSL, "true");

            final String keystoreFileProp;
            final String keystorePasswordProp;

            if (DBWorkbench.isDistributed() || DBWorkbench.getPlatform().getApplication().isMultiuser()) {
                var trustStoreData = SSLHandlerTrustStoreImpl.readTrustStoreData(
                    sslConfig, SSLHandlerTrustStoreImpl.PROP_SSL_KEYSTORE);
                if (trustStoreData != null && trustStoreData.length != 0) {
                    keystoreFileProp = saveTrustStoreToFile(trustStoreData);
                } else {
                    keystoreFileProp = null;
                }
                keystorePasswordProp = sslConfig.getSecureProperty(SSLHandlerTrustStoreImpl.PROP_SSL_KEYSTORE_PASSWORD);
            } else if (CommonUtils.isEmpty(sslConfig.getStringProperty(SSLHandlerTrustStoreImpl.PROP_SSL_METHOD))) {
                // Backward compatibility
                keystoreFileProp = sslConfig.getStringProperty(SQLServerConstants.PROP_SSL_KEYSTORE);
                keystorePasswordProp = sslConfig.getStringProperty(SQLServerConstants.PROP_SSL_KEYSTORE_PASSWORD);
            } else {
                keystoreFileProp = sslConfig.getStringProperty(SSLHandlerTrustStoreImpl.PROP_SSL_KEYSTORE);
                keystorePasswordProp = sslConfig.getPassword();
            }

            if (!CommonUtils.isEmpty(keystoreFileProp)) {
                properties.put("trustStore", keystoreFileProp);
            }

            if (!CommonUtils.isEmpty(keystorePasswordProp)) {
                properties.put("trustStorePassword", keystorePasswordProp);
            }

            final String keystoreHostnameProp = sslConfig.getStringProperty(SQLServerConstants.PROP_SSL_KEYSTORE_HOSTNAME);
            if (!CommonUtils.isEmpty(keystoreHostnameProp)) {
                properties.put("hostNameInCertificate", keystoreHostnameProp);
            }
        } catch (Exception e) {
            throw new DBCException("Error initializing SSL trust store", e);
        }
    }

    @Override
    protected JDBCExecutionContext createExecutionContext(JDBCRemoteInstance instance, String type) {
        return new SQLServerExecutionContext(instance, type);
    }

    @Override
    protected void initializeContextState(@NotNull DBRProgressMonitor monitor, @NotNull JDBCExecutionContext context, JDBCExecutionContext initFrom) throws DBException {
        super.initializeContextState(monitor, context, initFrom);
        if (initFrom != null) {
            if (!isDataWarehouseServer(monitor)) {
                SQLServerExecutionContext ssContext = (SQLServerExecutionContext) initFrom;
                SQLServerDatabase defaultObject = ssContext.getDefaultCatalog();
                if (defaultObject != null) {
                    ((SQLServerExecutionContext)context).setCurrentDatabase(monitor, defaultObject);
                }
                SQLServerSchema defaultSchema = ssContext.getDefaultSchema();
                if (defaultSchema != null && !isDataWarehouseServer(monitor)) {
                    ((SQLServerExecutionContext)context).setDefaultSchema(monitor, defaultSchema);
                }
            }
        } else {
            ((SQLServerExecutionContext)context).refreshDefaults(monitor, true);
        }
    }

    @Override
    public Object getDataSourceFeature(String featureId) {
        return switch (featureId) {
            case DBPDataSource.FEATURE_LIMIT_AFFECTS_DML -> true;
            case DBPDataSource.FEATURE_MAX_STRING_LENGTH -> 8000;
            default -> super.getDataSourceFeature(featureId);
        };
    }

    @Override
    public void initialize(@NotNull DBRProgressMonitor monitor) throws DBException {
        super.initialize(monitor);

        this.dataTypeCache.getAllObjects(monitor, this);
        this.databaseCache.getAllObjects(monitor, this);

        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load data source meta info")) {
            try {
                JDBCUtils.queryString(session, "SELECT COLUMNPROPERTY(0, NULL, NULL)");
                this.supportsColumnProperty = true;
            } catch (Exception e) {
                this.supportsColumnProperty = false;
            }

            // Read Database Engine edition of the instance of SQL Server installed on the server.
            try {
                String result = JDBCUtils.queryString(session, "SELECT SERVERPROPERTY('EngineEdition')");
                if ("6".equals(result) || "11".equals(result)) {
                    // SERVERPROPERTY returns int 6 or 11 if it is Azure Synapse
                    isSynapseDatabase = true;
                }
            } catch (SQLException e) {
                log.debug("Can't read Database Engine edition info", e);
            }
        } catch (Throwable e) {
            log.error("Error during connection initialization", e);
        }
    }

    //////////////////////////////////////////////////////////////////
    // Data types

    @NotNull
    @Override
    public DBPDataKind resolveDataKind(@NotNull String typeName, int valueType) {
        return getLocalDataType(valueType).getDataKind();
    }

    @Override
    public List<SQLServerDataType> getLocalDataTypes() {
        return dataTypeCache.getCachedObjects();
    }

    SQLServerDataType getSystemDataType(int systemTypeId) {
        for (SQLServerDataType dt : dataTypeCache.getCachedObjects()) {
            if (dt.getObjectId() == systemTypeId) {
                return dt;
            }
        }
        if (systemTypeId != SQLServerConstants.TABLE_TYPE_SYSTEM_ID) { // 243 - ID of user defined table types
            log.debug("System data type " + systemTypeId + " not found");
        }
        SQLServerDataType sdt = new SQLServerDataType(this, String.valueOf(systemTypeId), systemTypeId, DBPDataKind.OBJECT, java.sql.Types.OTHER);
        dataTypeCache.cacheObject(sdt);
        return sdt;
    }

    @Override
    public SQLServerDataType getLocalDataType(String typeName) {
        return dataTypeCache.getCachedObject(typeName);
    }

    @Override
    public SQLServerDataType getLocalDataType(int typeID) {
        DBSDataType dt = super.getLocalDataType(typeID);
        if (dt == null) {
            log.debug("System data type " + typeID + " not found");
        }
        return (SQLServerDataType) dt;
    }

    @Override
    public String getDefaultDataTypeName(@NotNull DBPDataKind dataKind) {
        return switch (dataKind) {
            case BOOLEAN -> "bit";
            case NUMERIC -> "int";
            case STRING -> "varchar";
            case DATETIME -> SQLServerConstants.TYPE_DATETIME;
            case BINARY, CONTENT -> "varbinary";
            case ROWID -> "uniqueidentifier";
            default -> super.getDefaultDataTypeName(dataKind);
        };
    }

    //////////////////////////////////////////////////////////
    // Databases

    protected boolean isShowAllSchemas() {
        return CommonUtils.toBoolean(getContainer().getConnectionConfiguration().getProviderProperty(SQLServerConstants.PROP_SHOW_ALL_SCHEMAS));
    }

    //////////////////////////////////////////////////////////////
    // Databases

    @Association
    public Collection<SQLServerDatabase> getDatabases(DBRProgressMonitor monitor) throws DBException {
        return databaseCache.getAllObjects(monitor, this);
    }

    public SQLServerDatabase getDatabase(DBRProgressMonitor monitor, String name) throws DBException {
        return databaseCache.getObject(monitor, this, name);
    }

    public SQLServerDatabase getDatabase(DBRProgressMonitor monitor, long dbId) throws DBException {
        for (SQLServerDatabase db : databaseCache.getAllObjects(monitor, this)) {
            if (db.getDatabaseId() == dbId) {
                return db;
            }
        }
        return null;
    }

    public SQLServerDatabase getDatabase(String name) {
        return databaseCache.getCachedObject(name);
    }

    public SQLServerDatabase getDefaultDatabase(@NotNull DBRProgressMonitor monitor) {
        return ((SQLServerExecutionContext)getDefaultInstance().getDefaultContext(monitor, true)).getDefaultCatalog();
    }

    @Override
    public Collection<? extends DBSObject> getChildren(@NotNull DBRProgressMonitor monitor) throws DBException {
        return databaseCache.getAllObjects(monitor, this);
    }

    @Override
    public DBSObject getChild(@NotNull DBRProgressMonitor monitor, @NotNull String childName) throws DBException {
        return databaseCache.getObject(monitor, this, childName);
    }

    @NotNull
    @Override
    public Class<? extends DBSObject> getPrimaryChildType(@Nullable DBRProgressMonitor monitor) {
        return SQLServerDatabase.class;
    }

    @Override
    public void cacheStructure(@NotNull DBRProgressMonitor monitor, int scope) throws DBException {
        databaseCache.getAllObjects(monitor, this);
    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        databaseCache.clearCache();
        serverLoginCache.clearCache();
        hasStatistics = false;
        return super.refreshObject(monitor);
    }

    @Override
    public DBCQueryTransformer createQueryTransformer(@NotNull DBCQueryTransformType type) {
        if (type == DBCQueryTransformType.RESULT_SET_LIMIT) {
            return new QueryTransformerTop();
        }
        return super.createQueryTransformer(type);
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter == DBSStructureAssistant.class) {
            return adapter.cast(new SQLServerStructureAssistant(this));
        } else if (adapter == DBAServerSessionManager.class) {
            return adapter.cast(new SQLServerSessionManager(this));
        } else if (adapter == DBAUserPasswordManager.class) {
            return adapter.cast(new SQLServerLoginPasswordManager(this));
        }
        return super.getAdapter(adapter);
    }

    @Override
    public ErrorPosition[] getErrorPosition(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext context, @NotNull String query, @NotNull Throwable error) {
        Throwable rootCause = CommonUtils.getRootCause(error);
        if (rootCause != null && SQLServerConstants.SQL_SERVER_EXCEPTION_CLASS_NAME.equals(rootCause.getClass().getName())) {
            // Read line number from SQLServerError class
            try {
                Object serverError = rootCause.getClass().getMethod("getSQLServerError").invoke(rootCause);
                if (serverError != null) {
                    Object serverErrorLine = BeanUtils.readObjectProperty(serverError, "lineNumber");
                    if (serverErrorLine instanceof Number) {
                        ErrorPosition pos = new ErrorPosition();
                        pos.line = ((Number) serverErrorLine).intValue() - 1;
                        return new ErrorPosition[] {pos};
                    }
                }
            } catch (Throwable e) {
                // ignore
            }
        }

        return super.getErrorPosition(monitor, context, query, error);
    }

    @Override
    public boolean isStatisticsCollected() {
        return hasStatistics;
    }

    @Override
    public void collectObjectStatistics(DBRProgressMonitor monitor, boolean totalSizeOnly, boolean forceRefresh) throws DBException {
        if (hasStatistics && !forceRefresh) {
            return;
        }
        if (SQLServerUtils.isDriverAzure(getContainer().getDriver()) || SQLServerUtils.isDriverBabelfish(getContainer().getDriver()) || isDataWarehouseServer(monitor)) {
            hasStatistics = true;
            return;
        }
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load schema statistics")) {
            try (JDBCStatement dbStat = session.createStatement()) {
                try (JDBCResultSet dbResult = dbStat.executeQuery("SELECT database_id, SUM(size)\n" +
                    "FROM sys.master_files WITH(NOWAIT)\n" +
                    "GROUP BY database_id")) {
                    while (dbResult.next()) {
                        long dbId = JDBCUtils.safeGetLong(dbResult, 1);
                        long bytes = dbResult.getLong(2) * 8 * 1024;
                        SQLServerDatabase database = getDatabase(monitor, dbId);
                        if (database != null) {
                            database.setDatabaseTotalSize(bytes);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            throw new DBCException("Error reading database statistics", e);
        } finally {
            hasStatistics = true;
        }
    }

    /**
     * Returns true only in case we find the table and this table has clustered COLUMNSTORE index.
     * These types of tables restrict special reading rules: do not scroll results or use TOP in the SELECT.
     */
    @Override
    public boolean isForceTransform(DBCSession session, SQLQuery sqlQuery) {
        try {
            SQLServerTableBase table = SQLServerUtils.getTableFromQuery(session, sqlQuery, this);
            return table != null && table.isClustered(session.getProgressMonitor());
        } catch (DBException | SQLException e) {
            log.debug("Table not found. ", e);
        }
        return false;
    }

    @Override
    public boolean isLimitApplicableTo(SQLQuery query) {
        boolean hasNextValExpr = false;
        try {
            Statement statement = SQLSemanticProcessor.parseQuery(this.sqlDialect, query.getText());
            if (statement instanceof Select) {
                SelectBody selectBody = ((Select) statement).getSelectBody();
                if (selectBody instanceof PlainSelect plainSelect) {
                    if (plainSelect.getFromItem() == null) {
                        hasNextValExpr = plainSelect.getSelectItems().stream().anyMatch(
                            item -> (item instanceof SelectExpressionItem) 
                                && (((SelectExpressionItem) item).getExpression() instanceof NextValExpression)
                        );
                    }
                }
            }
        } catch (DBCException e) {
            log.error("Can't parse query " + query.getText(), e);
        }
        return !hasNextValExpr;
    }
    
    public static class DatabaseCache extends JDBCObjectCache<SQLServerDataSource, SQLServerDatabase> {
        DatabaseCache() {
            setListOrderComparator(DBUtils.nameComparator());
        }

        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull SQLServerDataSource owner) throws SQLException {
            StringBuilder sql = new StringBuilder("SELECT db.* FROM sys.databases db");
            DBPDataSourceContainer container = owner.getContainer();
            DBPConnectionConfiguration configuration = container.getConnectionConfiguration();
            String property = configuration.getProviderProperty(SQLServerConstants.PROP_SHOW_ALL_DATABASES);
            // By default we will show all databases only for SQL Server
            // And for other databases if "Show All databases" setting is enabled
            boolean showSpecifiedDatabase = property != null && !CommonUtils.getBoolean(property) ||
                (property == null && (owner.isBabelfish || SQLServerUtils.isDriverAzure(owner.getContainer().getDriver())));
            String databaseName = configuration.getDatabaseName();
            boolean useCurrentDatabaseName = showSpecifiedDatabase && CommonUtils.isEmpty(databaseName);
            if (useCurrentDatabaseName) {
                sql.append("\nWHERE db.name = db_name()");
            } else if (showSpecifiedDatabase) {
                sql.append("\nWHERE db.name = ?");
            }
            JDBCPreparedStatement dbStat;
            if (!showSpecifiedDatabase) {
                DBSObjectFilter databaseFilters = container.getObjectFilter(
                    SQLServerDatabase.class,
                    null,
                    false);
                if (databaseFilters != null && databaseFilters.isEnabled()) {
                    JDBCUtils.appendFilterClause(
                        sql,
                        databaseFilters,
                        "name",
                        true,
                        owner);
                }
                sql.append("\nORDER BY db.name");
                dbStat = session.prepareStatement(sql.toString());
                if (databaseFilters != null) {
                    JDBCUtils.setFilterParameters(dbStat, 1, databaseFilters);
                }
            } else {
                dbStat = session.prepareStatement(sql.toString());
                if (!useCurrentDatabaseName) {
                    dbStat.setString(1, databaseName);
                }
            }
            return dbStat;
        }

        @Override
        protected SQLServerDatabase fetchObject(@NotNull JDBCSession session, @NotNull SQLServerDataSource owner, @NotNull JDBCResultSet resultSet) {
            String databaseName = JDBCUtils.safeGetString(resultSet, "name");
            if (CommonUtils.isEmpty(databaseName)) {
                log.debug("Empty database name fetched");
                return null;
            }
            return new SQLServerDatabase(session, owner, resultSet, databaseName);
        }

    }

    private static class SystemDataTypeCache extends JDBCObjectCache<SQLServerDataSource, SQLServerDataType> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull SQLServerDataSource sqlServerDataSource) throws SQLException {
            return session.prepareStatement("SELECT * FROM sys.types WHERE is_user_defined = 0 order by name");
        }

        @Override
        protected SQLServerDataType fetchObject(@NotNull JDBCSession session, @NotNull SQLServerDataSource dataSource, @NotNull JDBCResultSet resultSet) {
            return new SQLServerDataType(dataSource, resultSet);
        }
    }

    public static class ServerLoginCache extends JDBCObjectCache<SQLServerDataSource, SQLServerLogin> {

        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull SQLServerDataSource dataSource) throws SQLException {
            return session.prepareStatement("SELECT * FROM sys.server_principals");
        }

        @Nullable
        @Override
        protected SQLServerLogin fetchObject(@NotNull JDBCSession session, @NotNull SQLServerDataSource dataSource, @NotNull JDBCResultSet resultSet) {
            String loginName = JDBCUtils.safeGetString(resultSet, "name");
            if (CommonUtils.isNotEmpty(loginName)) {
                return new SQLServerLogin(dataSource, loginName, resultSet);
            }
            return null;
        }
    }
}
