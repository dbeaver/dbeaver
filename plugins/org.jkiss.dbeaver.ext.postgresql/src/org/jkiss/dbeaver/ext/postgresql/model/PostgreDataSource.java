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
package org.jkiss.dbeaver.ext.postgresql.model;

import org.eclipse.core.runtime.IAdaptable;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.ext.postgresql.PostgreConstants;
import org.jkiss.dbeaver.ext.postgresql.PostgreDataSourceProvider;
import org.jkiss.dbeaver.ext.postgresql.PostgreUtils;
import org.jkiss.dbeaver.ext.postgresql.model.impls.PostgreServerPostgreSQL;
import org.jkiss.dbeaver.ext.postgresql.model.impls.PostgreServerType;
import org.jkiss.dbeaver.ext.postgresql.model.jdbc.PostgreJdbcFactory;
import org.jkiss.dbeaver.ext.postgresql.model.plan.PostgreQueryPlaner;
import org.jkiss.dbeaver.ext.postgresql.model.session.PostgreSessionManager;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.admin.sessions.DBAServerSessionManager;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.exec.jdbc.*;
import org.jkiss.dbeaver.model.exec.plan.DBCQueryPlanner;
import org.jkiss.dbeaver.model.impl.AsyncServerOutputReader;
import org.jkiss.dbeaver.model.impl.jdbc.*;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectLookupCache;
import org.jkiss.dbeaver.model.impl.sql.QueryTransformerLimit;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLState;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.runtime.net.DefaultCallbackHandler;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.BeanUtils;
import org.jkiss.utils.CommonUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PostgreDataSource
 */
public class PostgreDataSource extends JDBCDataSource implements DBSInstanceContainer, IAdaptable, DBPObjectStatisticsCollector {

    private static final Log log = Log.getLog(PostgreDataSource.class);

    private DatabaseCache databaseCache;
    private String activeDatabaseName;
    private PostgreServerExtension serverExtension;
    private String serverVersion;

    private volatile boolean hasStatistics;

    public PostgreDataSource(DBRProgressMonitor monitor, DBPDataSourceContainer container)
        throws DBException
    {
        super(monitor, container, new PostgreDialect());
    }

    @Override
    public Object getDataSourceFeature(String featureId) {
        switch (featureId) {
            case DBConstants.FEATURE_MAX_STRING_LENGTH:
                return 10485760;
            case DBConstants.FEATURE_LOB_REQUIRE_TRANSACTIONS:
                return true;
        }
        return super.getDataSourceFeature(featureId);
    }

    @Override
    protected void initializeRemoteInstance(@NotNull DBRProgressMonitor monitor) throws DBException {
        activeDatabaseName = getContainer().getConnectionConfiguration().getBootstrap().getDefaultCatalogName();
        if (CommonUtils.isEmpty(activeDatabaseName)) {
            activeDatabaseName = getContainer().getConnectionConfiguration().getDatabaseName();
        }
        if (CommonUtils.isEmpty(activeDatabaseName)) {
            activeDatabaseName = PostgreConstants.DEFAULT_DATABASE;
        }
        databaseCache = new DatabaseCache();

        DBPConnectionConfiguration configuration = getContainer().getActualConnectionConfiguration();
        final boolean showNDD = CommonUtils.getBoolean(configuration.getProviderProperty(PostgreConstants.PROP_SHOW_NON_DEFAULT_DB), false);
        List<PostgreDatabase> dbList = new ArrayList<>();
        if (!showNDD) {
            PostgreDatabase defDatabase = new PostgreDatabase(monitor, this, activeDatabaseName);
            dbList.add(defDatabase);
        } else {
            loadAvailableDatabases(monitor, configuration, dbList);
        }
        databaseCache.setCache(dbList);
        // Initiate default context
        getDefaultInstance().checkInstanceConnection(monitor);
    }

    private void loadAvailableDatabases(@NotNull DBRProgressMonitor monitor, DBPConnectionConfiguration configuration, List<PostgreDatabase> dbList) throws DBException {
        // Make initial connection to read database list
        final boolean showTemplates = CommonUtils.toBoolean(configuration.getProviderProperty(PostgreConstants.PROP_SHOW_TEMPLATES_DB));
        StringBuilder catalogQuery = new StringBuilder(
                "SELECT db.oid,db.* FROM pg_catalog.pg_database db WHERE datallowconn ");
        if (!showTemplates) {
            catalogQuery.append(" AND NOT datistemplate ");
        }
        DBSObjectFilter catalogFilters = getContainer().getObjectFilter(PostgreDatabase.class, null, false);
        if (catalogFilters != null) {
            JDBCUtils.appendFilterClause(catalogQuery, catalogFilters, "datname", false);
        }
        catalogQuery.append("\nORDER BY db.datname");
        DBExecUtils.startContextInitiation(getContainer());
        try (Connection bootstrapConnection = openConnection(monitor, null, "Read PostgreSQL database list")) {
            // Read server version info here - it is needed during database metadata fetch (#8061)
            getDataSource().readDatabaseServerVersion(bootstrapConnection.getMetaData());
            // Get all databases
            try (PreparedStatement dbStat = bootstrapConnection.prepareStatement(catalogQuery.toString())) {
                if (catalogFilters != null) {
                    JDBCUtils.setFilterParameters(dbStat, 1, catalogFilters);
                }
                try (ResultSet dbResult = dbStat.executeQuery()) {
                    while (dbResult.next()) {
                        PostgreDatabase database = new PostgreDatabase(monitor, this, dbResult);
                        dbList.add(database);
                    }
                }
            }
            if (activeDatabaseName == null) {
                try (PreparedStatement stat = bootstrapConnection.prepareStatement("SELECT current_database()")) {
                    try (ResultSet rs = stat.executeQuery()) {
                        if (rs.next()) {
                            activeDatabaseName = JDBCUtils.safeGetString(rs, 1);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            throw new DBException("Can't connect ot remote PostgreSQL server", e);
        } finally {
            DBExecUtils.finishContextInitiation(getContainer());
        }
    }

    @Override
    protected Map<String, String> getInternalConnectionProperties(DBRProgressMonitor monitor, DBPDriver driver, JDBCExecutionContext context, String purpose, DBPConnectionConfiguration connectionInfo) throws DBCException
    {
        Map<String, String> props = new LinkedHashMap<>(PostgreDataSourceProvider.getConnectionsProps());
        final DBWHandlerConfiguration sslConfig = getContainer().getActualConnectionConfiguration().getHandler(PostgreConstants.HANDLER_SSL);
        if (sslConfig != null && sslConfig.isEnabled()) {
            try {
                boolean useProxy = sslConfig.getBooleanProperty(PostgreConstants.PROP_SSL_PROXY);
                if (useProxy) {
                    initProxySSL(props, sslConfig);
                } else {
                    initServerSSL(props, sslConfig);
                }
            } catch (Exception e) {
                throw new DBCException("Error configuring SSL certificates", e);
            }
        } else {
            getServerType().initDefaultSSLConfig(connectionInfo, props);
        }
        return props;
    }

    private void initServerSSL(Map<String, String> props, DBWHandlerConfiguration sslConfig) throws Exception {
        props.put(PostgreConstants.PROP_SSL, "true");

        final String rootCertProp = sslConfig.getStringProperty(PostgreConstants.PROP_SSL_ROOT_CERT);
        if (!CommonUtils.isEmpty(rootCertProp)) {
            props.put("sslrootcert", rootCertProp);
        }
        final String clientCertProp = sslConfig.getStringProperty(PostgreConstants.PROP_SSL_CLIENT_CERT);
        if (!CommonUtils.isEmpty(clientCertProp)) {
            props.put("sslcert", clientCertProp);
        }
        final String keyCertProp = sslConfig.getStringProperty(PostgreConstants.PROP_SSL_CLIENT_KEY);
        if (!CommonUtils.isEmpty(keyCertProp)) {
            props.put("sslkey", keyCertProp);
        }

        final String modeProp = sslConfig.getStringProperty(PostgreConstants.PROP_SSL_MODE);
        if (!CommonUtils.isEmpty(modeProp)) {
            props.put("sslmode", modeProp);
        }
        final String factoryProp = sslConfig.getStringProperty(PostgreConstants.PROP_SSL_FACTORY);
        if (!CommonUtils.isEmpty(factoryProp)) {
            props.put("sslfactory", factoryProp);
        }
        props.put("sslpasswordcallback", DefaultCallbackHandler.class.getName());
    }

    private void initProxySSL(Map<String, String> props, DBWHandlerConfiguration sslConfig) throws Exception {
        // No special config
        //initServerSSL(props, sslConfig);
    }

    @Override
    protected PostgreExecutionContext createExecutionContext(JDBCRemoteInstance instance, String type) {
        return new PostgreExecutionContext((PostgreDatabase) instance, type);
    }

    protected void initializeContextState(@NotNull DBRProgressMonitor monitor, @NotNull JDBCExecutionContext context, JDBCExecutionContext initFrom) throws DBException {
        ((PostgreExecutionContext)context).refreshDefaults(monitor, true);
        if (initFrom != null) {
            final PostgreSchema activeSchema = ((PostgreExecutionContext)initFrom).getDefaultSchema();
            if (activeSchema != null && activeSchema != ((PostgreExecutionContext) context).getDefaultSchema()) {
                ((PostgreExecutionContext)context).setDefaultSchema(monitor, activeSchema);
            }
        }
    }

    public DatabaseCache getDatabaseCache()
    {
        return databaseCache;
    }

    public Collection<PostgreDatabase> getDatabases()
    {
        return databaseCache.getCachedObjects();
    }

    public PostgreDatabase getDatabase(String name)
    {
        return databaseCache.getCachedObject(name);
    }

    @Override
    public void initialize(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
        super.initialize(monitor);

        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Read server version")) {
            serverVersion = JDBCUtils.queryString(session, "SELECT version()");
        } catch (Exception e) {
            log.debug("Error reading PostgreSQL version: " + e.getMessage());
            serverVersion = "";
        }

        // Read databases
        getDefaultInstance().cacheDataTypes(monitor, true);
    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
        super.refreshObject(monitor);
        shutdown(monitor);

        this.databaseCache.clearCache();
        this.activeDatabaseName = null;

        this.initializeRemoteInstance(monitor);
        this.initialize(monitor);

        return this;
    }

    @Override
    public Collection<? extends PostgreDatabase> getChildren(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
        return getDatabases();
    }

    @Override
    public PostgreDatabase getChild(@NotNull DBRProgressMonitor monitor, @NotNull String childName)
        throws DBException
    {
        return getDatabase(childName);
    }

    @NotNull
    @Override
    public Class<? extends PostgreDatabase> getPrimaryChildType(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
        return PostgreDatabase.class;
    }

    @Override
    public void cacheStructure(@NotNull DBRProgressMonitor monitor, int scope)
        throws DBException
    {
        databaseCache.getAllObjects(monitor, this);
    }

    ////////////////////////////////////////////
    // Connections

    @Override
    protected Connection openConnection(@NotNull DBRProgressMonitor monitor, @Nullable JDBCExecutionContext context, @NotNull String purpose) throws DBCException {
        final DBPConnectionConfiguration conConfig = getContainer().getActualConnectionConfiguration();

        JDBCRemoteInstance instance = context == null ? null : context.getOwnerInstance();
        Connection pgConnection;
        if (instance != null) {
            log.debug("Initiate connection to " + getServerType().getServerTypeName() + " database [" + instance.getName() + "@" + conConfig.getHostName() + "] for " + purpose);
        }
        if (instance instanceof PostgreDatabase &&
            instance.getName() != null &&
            !CommonUtils.equalObjects(instance.getName(), conConfig.getDatabaseName()))
        {
            // If database was changed then use new name for connection
            final DBPConnectionConfiguration originalConfig = new DBPConnectionConfiguration(conConfig);
            try {
                // Patch URL with new database name
                if (CommonUtils.isEmpty(conConfig.getUrl()) || !CommonUtils.isEmpty(conConfig.getHostName())) {
                    conConfig.setDatabaseName(instance.getName());
                    conConfig.setUrl(getContainer().getDriver().getDataSourceProvider().getConnectionURL(getContainer().getDriver(), conConfig));
                } else {
                    //String url = conConfig.getUrl();
                }

                pgConnection = super.openConnection(monitor, context, purpose);
            }
            finally {
                conConfig.setDatabaseName(originalConfig.getDatabaseName());
                conConfig.setUrl(originalConfig.getUrl());
            }
        } else {
            pgConnection = super.openConnection(monitor, context, purpose);
        }

        if (getServerType().supportsClientInfo() && !getContainer().getPreferenceStore().getBoolean(ModelPreferences.META_CLIENT_NAME_DISABLE)) {
            // Provide client info. Not supported by Redshift?
            try {
                pgConnection.setClientInfo(JDBCConstants.APPLICATION_NAME_CLIENT_PROPERTY, DBUtils.getClientApplicationName(getContainer(), context, purpose));
            } catch (Throwable e) {
                // just ignore
                log.debug(e);
            }
        }

        return pgConnection;
    }

    @Override
    public <T> T getAdapter(Class<T> adapter)
    {
        if (adapter == DBSStructureAssistant.class) {
            return adapter.cast(new PostgreStructureAssistant(this));
        } else if (adapter == DBCServerOutputReader.class) {
            return adapter.cast(new AsyncServerOutputReader());
        } else if (adapter == DBAServerSessionManager.class) {
            return adapter.cast(new PostgreSessionManager(this));
        } else if (adapter == DBCQueryPlanner.class) {
            return adapter.cast(new PostgreQueryPlaner(this));
        }
        return super.getAdapter(adapter);
    }

    @NotNull
    @Override
    public PostgreDataSource getDataSource() {
        return this;
    }

    @Override
    public Collection<PostgreDataType> getLocalDataTypes()
    {
        return getDefaultInstance().getLocalDataTypes();
    }

    @Override
    public PostgreDataType getLocalDataType(String typeName)
    {
        return getDefaultInstance().getLocalDataType(typeName);
    }

    @Override
    public DBSDataType getLocalDataType(int typeID) {
        return getDefaultInstance().getLocalDataType(typeID);
    }

    @Override
    public String getDefaultDataTypeName(@NotNull DBPDataKind dataKind) {
        return getDefaultInstance().getDefaultDataTypeName(dataKind);
    }

    @NotNull
    @Override
    public PostgreDatabase getDefaultInstance() {
        PostgreDatabase defDatabase = databaseCache.getCachedObject(activeDatabaseName);
        if (defDatabase == null) {
            defDatabase = databaseCache.getCachedObject(PostgreConstants.DEFAULT_DATABASE);
        }
        if (defDatabase == null) {
            final List<PostgreDatabase> allDatabases = databaseCache.getCachedObjects();
            if (allDatabases.isEmpty()) {
                // Looks like we are not connected or in connection process right now - no instance then
                throw new IllegalStateException("No databases found on the server");
            }
            defDatabase = allDatabases.get(0);
        }
        return defDatabase;
    }

    @NotNull
    @Override
    public List<PostgreDatabase> getAvailableInstances() {
        return databaseCache.getCachedObjects();
    }

    /**
     * Deprecated. Database change is not supported (as it is ambiguous)
     */
    @Deprecated
    public void setDefaultInstance(@NotNull DBRProgressMonitor monitor, @NotNull PostgreDatabase newDatabase, PostgreSchema schema)
        throws DBException
    {
        final PostgreDatabase oldDatabase = getDefaultInstance();
        if (oldDatabase != newDatabase) {
            newDatabase.initializeMetaContext(monitor);
            newDatabase.cacheDataTypes(monitor, false);
        }

        PostgreSchema oldDefaultSchema = null;
        if (schema != null) {
            oldDefaultSchema = newDatabase.getMetaContext().getDefaultSchema();
            newDatabase.getMetaContext().changeDefaultSchema(monitor, schema, false);
        }

        activeDatabaseName = newDatabase.getName();

        // Notify UI
        DBUtils.fireObjectSelect(oldDatabase, false);
        DBUtils.fireObjectSelect(newDatabase, true);

        if (schema != null && schema != oldDefaultSchema) {
            if (oldDefaultSchema != null) {
                DBUtils.fireObjectSelect(oldDefaultSchema, false);
            }
            DBUtils.fireObjectSelect(schema, true);
        }

        // Close all database connections but meta (we need it to browse metadata like navigator tree)
        oldDatabase.shutdown(monitor, true);
    }

    public List<String> getTemplateDatabases(DBRProgressMonitor monitor) throws DBException {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load template databases")) {
            try (PreparedStatement dbStat = session.prepareStatement("SELECT db.datname FROM pg_catalog.pg_database db WHERE datistemplate")) {
                try (ResultSet resultSet = dbStat.executeQuery()) {
                    List<String> dbNames = new ArrayList<>();
                    while (resultSet.next()) {
                        dbNames.add(resultSet.getString(1));
                    }
                    return dbNames;
                }
            }
        } catch (Exception e) {
            throw new DBException("Error reading template databases", e);
        }
    }

    public PostgreServerExtension getServerType() {
        if (serverExtension == null) {
            PostgreServerType serverType = PostgreUtils.getServerType(getContainer().getDriver());

            try {
                serverExtension = serverType.createServerExtension(this);
            } catch (Throwable e) {
                log.error("Can't determine server type", e);
                serverExtension = new PostgreServerPostgreSQL(this);
            }
        }
        return serverExtension;
    }

    public String getServerVersion() {
        return serverVersion;
    }

    public boolean supportsRoles() {
        return getServerType().supportsRoles() && !getContainer().getNavigatorSettings().isShowOnlyEntities();
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
        hasStatistics = true;
        if (!getServerType().supportsDatabaseSize()) {
            return;
        }
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load table status")) {
            Collection<PostgreDatabase> databases = getDatabases();
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT db.datname,pg_database_size(db.oid) FROM pg_catalog.pg_database db " +
                    (databases.size() == 1 ? "WHERE db.oid=?" : "")))
            {
                if (databases.size() == 1) {
                    dbStat.setLong(1, databases.iterator().next().getObjectId());
                }
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    while (dbResult.next()) {
                        String dbName = JDBCUtils.safeGetString(dbResult, 1);
                        long dbSize = dbResult.getLong(2);
                        PostgreDatabase database = getDatabase(dbName);
                        if (database != null) {
                            database.setDbTotalSize(dbSize);
                        }
                    }
                }
            } catch (SQLException e) {
                throw new DBCException(e, session.getExecutionContext());
            }
        } finally {
            hasStatistics = true;
        }
    }

    class DatabaseCache extends JDBCObjectLookupCache<PostgreDataSource, PostgreDatabase>
    {
        @Override
        protected PostgreDatabase fetchObject(@NotNull JDBCSession session, @NotNull PostgreDataSource owner, @NotNull JDBCResultSet resultSet) throws SQLException, DBException
        {
            return new PostgreDatabase(session.getProgressMonitor(), owner, resultSet);
        }

        @NotNull
        @Override
        public JDBCStatement prepareLookupStatement(JDBCSession session, PostgreDataSource owner, PostgreDatabase object, String objectName) throws SQLException {
            final boolean showNDD = CommonUtils.toBoolean(getContainer().getActualConnectionConfiguration().getProviderProperty(PostgreConstants.PROP_SHOW_NON_DEFAULT_DB));
            final boolean showTemplates = CommonUtils.toBoolean(getContainer().getActualConnectionConfiguration().getProviderProperty(PostgreConstants.PROP_SHOW_TEMPLATES_DB));
            StringBuilder catalogQuery = new StringBuilder(
                "SELECT db.oid,db.*" +
                    "\nFROM pg_catalog.pg_database db WHERE datallowconn ");
            if (object == null && !showTemplates) {
                catalogQuery.append(" AND NOT datistemplate ");
            }
            if (object != null) {
                catalogQuery.append("\nAND db.oid=?");
            } else if (objectName != null || !showNDD) {
                catalogQuery.append("\nAND db.datname=?");
            }
            DBSObjectFilter catalogFilters = owner.getContainer().getObjectFilter(PostgreDatabase.class, null, false);
            if (object == null && showNDD) {
                if (catalogFilters != null) {
                    JDBCUtils.appendFilterClause(catalogQuery, catalogFilters, "datname", false);
                }
                catalogQuery.append("\nORDER BY db.datname");
            }
            JDBCPreparedStatement dbStat = session.prepareStatement(catalogQuery.toString());
            if (object != null) {
                dbStat.setLong(1, object.getObjectId());
            } else if (objectName != null || !showNDD) {
                dbStat.setString(1, (objectName != null ? objectName : activeDatabaseName));
            } else if (catalogFilters != null) {
                JDBCUtils.setFilterParameters(dbStat, 1, catalogFilters);
            }
            return dbStat;
        }
    }

    private Pattern ERROR_POSITION_PATTERN = Pattern.compile("\\n\\s*\\p{L}+\\s*: ([0-9]+)");

    @Nullable
    @Override
    public ErrorPosition[] getErrorPosition(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext context, @NotNull String query, @NotNull Throwable error) {
        Throwable rootCause = GeneralUtils.getRootCause(error);
        if (rootCause != null && PostgreConstants.PSQL_EXCEPTION_CLASS_NAME.equals(rootCause.getClass().getName())) {
            try {
                Object serverErrorMessage = BeanUtils.readObjectProperty(rootCause, "serverErrorMessage");
                if (serverErrorMessage != null) {
                    Object position = BeanUtils.readObjectProperty(serverErrorMessage, "position");
                    if (position instanceof Number) {
                        ErrorPosition pos = new ErrorPosition();
                        pos.position = ((Number) position).intValue();
                        return new ErrorPosition[] {pos};
                    }
                }
            } catch (Throwable e) {
                // Something went wrong. Doesn't matter, ignore it as we are already in error handling routine
            }
        }
        String message = error.getMessage();
        if (!CommonUtils.isEmpty(message)) {
            Matcher matcher = ERROR_POSITION_PATTERN.matcher(message);
            if (matcher.find()) {
                DBPErrorAssistant.ErrorPosition pos = new DBPErrorAssistant.ErrorPosition();
                pos.position = Integer.parseInt(matcher.group(1)) - 1;
                return new ErrorPosition[] {pos};
            }
        }
        return null;
    }

    @NotNull
    @Override
    protected JDBCFactory createJdbcFactory() {
        return new PostgreJdbcFactory();
    }

    @Override
    public ErrorType discoverErrorType(@NotNull Throwable error) {
        String sqlState = SQLState.getStateFromException(error);
        if (sqlState != null) {
            if (PostgreConstants.ERROR_ADMIN_SHUTDOWN.equals(sqlState)) {
                return ErrorType.CONNECTION_LOST;
            } else if (PostgreConstants.ERROR_TRANSACTION_ABORTED.equals(sqlState)) {
                return ErrorType.TRANSACTION_ABORTED;
            }
        }

        return super.discoverErrorType(error);
    }

    @Override
    protected DBPDataSourceInfo createDataSourceInfo(DBRProgressMonitor monitor, @NotNull JDBCDatabaseMetaData metaData)
    {
        return new PostgreDataSourceInfo(this, metaData);
    }

    @Nullable
    @Override
    public DBCQueryTransformer createQueryTransformer(@NotNull DBCQueryTransformType type) {
        if (type == DBCQueryTransformType.RESULT_SET_LIMIT) {
            return new QueryTransformerLimit(false, true);
        } else if (type == DBCQueryTransformType.FETCH_ALL_TABLE) {
            return new QueryTransformerFetchAll();
        }
        return null;
    }

}
