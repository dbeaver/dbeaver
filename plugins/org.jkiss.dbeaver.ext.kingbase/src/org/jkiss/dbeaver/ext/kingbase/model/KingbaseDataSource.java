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
package org.jkiss.dbeaver.ext.kingbase.model;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.ext.kingbase.KingbaseConstants;
import org.jkiss.dbeaver.ext.kingbase.KingbaseDataSourceProvider;
import org.jkiss.dbeaver.ext.kingbase.KingbaseUtils;
import org.jkiss.dbeaver.ext.kingbase.model.impls.KingbaseServerKingbaseSQL;
import org.jkiss.dbeaver.ext.kingbase.model.impls.KingbaseServerType;
import org.jkiss.dbeaver.ext.kingbase.model.jdbc.KingbaseJdbcFactory;
import org.jkiss.dbeaver.ext.kingbase.model.plan.KingbaseQueryPlaner;
import org.jkiss.dbeaver.ext.kingbase.model.session.KingbaseSessionManager;
import org.jkiss.dbeaver.model.DBPAdaptable;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPDataSourceInfo;
import org.jkiss.dbeaver.model.DBPErrorAssistant;
import org.jkiss.dbeaver.model.DBPObjectStatisticsCollector;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.DatabaseURL;
import org.jkiss.dbeaver.model.access.DBAUserPasswordManager;
import org.jkiss.dbeaver.model.admin.sessions.DBAServerSessionManager;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.connection.DBPDriverConfigurationType;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCQueryTransformType;
import org.jkiss.dbeaver.model.exec.DBCQueryTransformer;
import org.jkiss.dbeaver.model.exec.DBExecUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCFactory;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.exec.output.DBCServerOutputReader;
import org.jkiss.dbeaver.model.exec.plan.DBCQueryPlanner;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCConstants;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCRemoteInstance;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectLookupCache;
import org.jkiss.dbeaver.model.impl.net.SSLHandlerTrustStoreImpl;
import org.jkiss.dbeaver.model.impl.sql.QueryTransformerLimit;
import org.jkiss.dbeaver.model.meta.ForTest;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLState;
import org.jkiss.dbeaver.model.struct.DBSDataBulkLoader;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSInstanceContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectFilter;
import org.jkiss.dbeaver.model.struct.DBSStructureAssistant;
import org.jkiss.dbeaver.model.struct.cache.SimpleObjectCache;
import org.jkiss.dbeaver.registry.timezone.TimezoneRegistry;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.net.DefaultCallbackHandler;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.BeanUtils;
import org.jkiss.utils.CommonUtils;

/**
 * KingbaseDataSource
 */
public class KingbaseDataSource extends JDBCDataSource implements DBSInstanceContainer, DBPAdaptable,
    DBPObjectStatisticsCollector {

    private static final Log log = Log.getLog(KingbaseDataSource.class);
    private static final KingbasePrivilegeType[] SUPPORTED_PRIVILEGE_TYPES = new KingbasePrivilegeType[]{
        KingbasePrivilegeType.SELECT,
        KingbasePrivilegeType.INSERT,
        KingbasePrivilegeType.UPDATE,
        KingbasePrivilegeType.DELETE,
        KingbasePrivilegeType.TRUNCATE,
        KingbasePrivilegeType.REFERENCES,
        KingbasePrivilegeType.TRIGGER,
        KingbasePrivilegeType.CREATE,
        KingbasePrivilegeType.CONNECT,
        KingbasePrivilegeType.TEMPORARY,
        KingbasePrivilegeType.EXECUTE,
        KingbasePrivilegeType.USAGE
    };

    private DatabaseCache databaseCache;
    private SettingCache settingCache;
    private String activeDatabaseName;
    private KingbaseServerExtension serverExtension;
    private String serverVersion;
    private volatile boolean hasStatistics;
    private boolean supportsEnumTable;
    private boolean supportsReltypeColumn = true;

    public KingbaseDataSource(DBRProgressMonitor monitor, DBPDataSourceContainer container)
        throws DBException
    {
        super(monitor, container, new KingbaseDialect());

        // Statistics was disabled then mark it as already read
        this.hasStatistics = !CommonUtils.getBoolean(container.getConnectionConfiguration().getProviderProperty(
            KingbaseConstants.PROP_SHOW_DATABASE_STATISTICS));
    }

    public KingbaseDataSource(@NotNull DBRProgressMonitor monitor,
                             @NotNull DBPDataSourceContainer container,
                             @NotNull SQLDialect dialect) throws DBException {
        super(monitor, container, dialect);

        this.hasStatistics = !CommonUtils.getBoolean(container.getConnectionConfiguration()
                    .getProviderProperty(KingbaseConstants.PROP_SHOW_DATABASE_STATISTICS));
    }

    @ForTest
    public KingbaseDataSource(DBPDataSourceContainer container, String serverVersion, String activeDatabaseName) {
        super(container, new KingbaseDialect());
        this.serverVersion = serverVersion;
        this.activeDatabaseName = activeDatabaseName;
        this.hasStatistics = false;

        databaseCache = new DatabaseCache();
        KingbaseDatabase defDatabase = new KingbaseDatabase(
            this,
            activeDatabaseName);
        databaseCache.setCache(Collections.singletonList(defDatabase));
        settingCache = new SettingCache();
    }

    @Override
    public Object getDataSourceFeature(String featureId) {
        return switch (featureId) {
            case DBPDataSource.FEATURE_MAX_STRING_LENGTH -> 10485760;
            case DBPDataSource.FEATURE_LOB_REQUIRE_TRANSACTIONS -> true;
            default -> super.getDataSourceFeature(featureId);
        };
    }

    @Override
    protected void initializeRemoteInstance(@NotNull DBRProgressMonitor monitor) throws DBException {
        DBPConnectionConfiguration configuration = getContainer().getActualConnectionConfiguration();
        if (configuration.getConfigurationType() == DBPDriverConfigurationType.MANUAL) {
            activeDatabaseName = configuration.getBootstrap().getDefaultCatalogName();
            if (CommonUtils.isEmpty(activeDatabaseName)) {
                activeDatabaseName = configuration.getDatabaseName();
            }
        } else {
            String url = configuration.getUrl();
            int divPos = url.lastIndexOf('/');
            if (divPos > 0) {
                int lastPos = -1;
                for (int i = divPos + 1; i < url.length(); i++) {
                    char c = url.charAt(i);
                    if (!Character.isLetterOrDigit(c) && c != '_' && c != '$' && c != '.') {
                        lastPos = i;
                    }
                }
                if (lastPos < 0) lastPos = url.length();
                activeDatabaseName = url.substring(divPos + 1, lastPos);
            }
        }
        if (CommonUtils.isEmpty(activeDatabaseName)) {
            if (!CommonUtils.isEmpty(configuration.getUserName())) {
                activeDatabaseName = configuration.getUserName();
            } else {
                activeDatabaseName = KingbaseConstants.DEFAULT_DATABASE;
            }
        }

        databaseCache = new DatabaseCache();
        settingCache = new SettingCache();

        final boolean showNDD = isReadDatabaseList(configuration);
        List<KingbaseDatabase> dbList = new ArrayList<>();
        if (!showNDD) {
            KingbaseDatabase defDatabase = createDatabaseImpl(monitor, activeDatabaseName);
            dbList.add(defDatabase);
        } else {
            loadAvailableDatabases(monitor, configuration, dbList);
        }
        databaseCache.setCache(dbList);
        getDefaultInstance().checkInstanceConnection(monitor, false);
        try {
            settingCache.getAllObjects(monitor, this);
        } catch (DBException e) {
            // ignore
        }
    }

    private void loadAvailableDatabases(@NotNull DBRProgressMonitor monitor, DBPConnectionConfiguration configuration, List<KingbaseDatabase> dbList) throws DBException {
        DBExecUtils.startContextInitiation(getContainer());
        try (Connection bootstrapConnection = openConnection(monitor, null, "Read Kingbase database list")) {

            readDatabaseServerVersion(bootstrapConnection.getMetaData());
            try (PreparedStatement dbStat = prepareReadDatabaseListStatement(monitor, bootstrapConnection, configuration)) {
                try (ResultSet dbResult = dbStat.executeQuery()) {
                    while (dbResult.next()) {
                        KingbaseDatabase database = createDatabaseImpl(monitor, dbResult);
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
            throw new DBException("Can't connect to remote Kingbase server", e);
        } finally {
            DBExecUtils.finishContextInitiation(getContainer());
        }
    }
    protected boolean isReadDatabaseList(DBPConnectionConfiguration configuration) {
        return configuration.getConfigurationType() != DBPDriverConfigurationType.URL &&
            CommonUtils.getBoolean(configuration.getProviderProperty(KingbaseConstants.PROP_SHOW_NON_DEFAULT_DB), false);
    }

    protected PreparedStatement prepareReadDatabaseListStatement(
        @NotNull DBRProgressMonitor monitor,
        @NotNull Connection bootstrapConnection,
        @NotNull DBPConnectionConfiguration configuration) throws SQLException
    {
        // Make initial connection to read database list
        DBSObjectFilter catalogFilters = getContainer().getObjectFilter(KingbaseDatabase.class, null, false);
        StringBuilder catalogQuery = new StringBuilder("SELECT db.oid,db.* FROM sys_catalog.sys_database db WHERE 1 = 1");
        boolean addExclusionName = false;
        String connectionDBName = getContainer().getConnectionConfiguration().getDatabaseName();
        {
            final boolean showTemplates = CommonUtils.toBoolean(configuration.getProviderProperty(KingbaseConstants.PROP_SHOW_TEMPLATES_DB));
            final boolean showUnavailable = CommonUtils.toBoolean(configuration.getProviderProperty(KingbaseConstants.PROP_SHOW_UNAVAILABLE_DB));

            if (!showUnavailable) {
                catalogQuery.append(" AND datallowconn");
            }
            if (!showTemplates) {
                catalogQuery.append(" AND NOT datistemplate ");
                if (!CommonUtils.isEmpty(connectionDBName)) {
                    catalogQuery.append("OR db.datname =?");
                    addExclusionName = true;
                }
            }
            if (catalogFilters != null) {
                JDBCUtils.appendFilterClause(catalogQuery, catalogFilters, "datname", false, this);
            }
            catalogQuery.append("\nORDER BY db.datname");
        }
        PreparedStatement dbStat = bootstrapConnection.prepareStatement(catalogQuery.toString());

        if (addExclusionName) {
            dbStat.setString(1, connectionDBName);
        }
        if (catalogFilters != null) {
            JDBCUtils.setFilterParameters(dbStat, addExclusionName ? 2 : 1, catalogFilters);
        }

        return dbStat;
    }

    @NotNull
    public KingbaseDatabase createDatabaseImpl(@NotNull DBRProgressMonitor monitor, ResultSet dbResult) throws DBException {
        return new KingbaseDatabase(monitor, this, dbResult);
    }

    @NotNull
    public KingbaseDatabase createDatabaseImpl(@NotNull DBRProgressMonitor monitor, String name) throws DBException {
        return new KingbaseDatabase(monitor, this, name);
    }

    @NotNull
    public KingbaseDatabase createDatabaseImpl(DBRProgressMonitor monitor, String name, KingbaseRole owner, String templateName, KingbaseTablespace tablespace, KingbaseCharset encoding) throws DBException {
        return new KingbaseDatabase(monitor, this, name, owner, templateName, tablespace, encoding);
    }

    @Override
    protected Map<String, String> getInternalConnectionProperties(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBPDriver driver,
        @NotNull JDBCExecutionContext context,
        @NotNull String purpose,
        @NotNull DBPConnectionConfiguration connectionInfo
    ) throws DBCException {
        Map<String, String> props = new LinkedHashMap<>(KingbaseDataSourceProvider.getConnectionsProps());
        final DBWHandlerConfiguration sslConfig = getContainer().getActualConnectionConfiguration().getHandler(KingbaseConstants.HANDLER_SSL);
        if (sslConfig != null && sslConfig.isEnabled()) {
            try {
                boolean useProxy = sslConfig.getBooleanProperty(KingbaseConstants.PROP_SSL_PROXY);
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
        KingbaseServerType serverType = getType();
        if (serverType.turnOffPreparedStatements()
            && !CommonUtils.toBoolean(getContainer().getActualConnectionConfiguration().getProviderProperty(KingbaseConstants.PROP_USE_PREPARED_STATEMENTS))) {
            props.put("prepareThreshold", "0");
        }

        if (getContainer().isConnectionReadOnly()) {
            props.put("readOnly", "true");
            props.put("readOnlyMode", "always");
        }

        return props;
    }

    private void initServerSSL(Map<String, String> props, DBWHandlerConfiguration sslConfig) throws DBException {
        props.put(KingbaseConstants.PROP_SSL, "true");

        if (!isMultiUserOrDistributed()) {
            // Local FS mode
            final String rootCertProp;
            final String clientCertProp;
            final String keyCertProp;

            if (CommonUtils.isEmpty(sslConfig.getStringProperty(SSLHandlerTrustStoreImpl.PROP_SSL_METHOD))) {
                // Backward compatibility
                rootCertProp = sslConfig.getStringProperty(KingbaseConstants.PROP_SSL_ROOT_CERT);
                clientCertProp = sslConfig.getStringProperty(KingbaseConstants.PROP_SSL_CLIENT_CERT);
                keyCertProp = sslConfig.getStringProperty(KingbaseConstants.PROP_SSL_CLIENT_KEY);
            } else {
                rootCertProp = sslConfig.getStringProperty(SSLHandlerTrustStoreImpl.PROP_SSL_CA_CERT);
                clientCertProp = sslConfig.getStringProperty(SSLHandlerTrustStoreImpl.PROP_SSL_CLIENT_CERT);
                keyCertProp = sslConfig.getStringProperty(SSLHandlerTrustStoreImpl.PROP_SSL_CLIENT_KEY);
            }

            if (!CommonUtils.isEmpty(rootCertProp)) {
                props.put("sslrootcert", rootCertProp);
            }
            if (!CommonUtils.isEmpty(clientCertProp)) {
                props.put("sslcert", clientCertProp);
            }
            if (!CommonUtils.isEmpty(keyCertProp)) {
                props.put("sslkey", keyCertProp);
            }
        } else {
            try {
                String rootCertProp = sslConfig.getSecureProperty(SSLHandlerTrustStoreImpl.PROP_SSL_CA_CERT_VALUE);
                if (!CommonUtils.isEmpty(rootCertProp)) {
                    props.put("sslrootcert", saveCertificateToFile(rootCertProp));
                }
                String clientCertProp = sslConfig.getSecureProperty(SSLHandlerTrustStoreImpl.PROP_SSL_CLIENT_CERT_VALUE);
                if (!CommonUtils.isEmpty(clientCertProp)) {
                    props.put("sslcert", saveCertificateToFile(clientCertProp));
                }
                String keyCertDer = sslConfig.getSecureProperty(SSLHandlerTrustStoreImpl.PROP_SSL_CLIENT_KEY);
                String keyCertProp = sslConfig.getSecureProperty(SSLHandlerTrustStoreImpl.PROP_SSL_CLIENT_KEY_VALUE);
                if (CommonUtils.isNotEmpty(keyCertDer)) { // may be after exception
                    props.put("sslkey", keyCertDer);
                } else if (CommonUtils.isNotEmpty(keyCertProp)) {
                    props.put("sslkey", saveCertificateToFile(keyCertProp));
                }
            } catch (IOException e) {
                throw new DBException("Can not configure SSL", e);
            }

        }

        final String modeProp = sslConfig.getStringProperty(KingbaseConstants.PROP_SSL_MODE);
        if (!CommonUtils.isEmpty(modeProp)) {
            props.put("sslmode", modeProp);
        }
        final String factoryProp = sslConfig.getStringProperty(KingbaseConstants.PROP_SSL_FACTORY);
        if (!CommonUtils.isEmpty(factoryProp)) {
            props.put("sslfactory", factoryProp);
        }
        props.put("sslpasswordcallback", DefaultCallbackHandler.class.getName());
    }

    private boolean isMultiUserOrDistributed() {
        return DBWorkbench.isDistributed() || DBWorkbench.getPlatform().getApplication().isMultiuser();
    }

    private void initProxySSL(Map<String, String> props, DBWHandlerConfiguration sslConfig) {
       
    }

    @Override
    protected KingbaseExecutionContext createExecutionContext(JDBCRemoteInstance instance, String type) {
        return new KingbaseExecutionContext((KingbaseDatabase) instance, type);
    }

    protected void initializeContextState(@NotNull DBRProgressMonitor monitor, @NotNull JDBCExecutionContext context,
                                          @Nullable JDBCExecutionContext initFrom) throws DBException {
        KingbaseExecutionContext kingbaseContext = (KingbaseExecutionContext) context;
        KingbaseSchema activeSchema = null;
        if (initFrom != null) {
            activeSchema = ((KingbaseExecutionContext)initFrom).getDefaultSchema();
        }
        kingbaseContext.refreshDefaults(monitor, true);
        if (activeSchema != null) {
            kingbaseContext.setDefaultCatalog(monitor, activeSchema.getDatabase(), activeSchema, true);
        }
    }

    public SimpleObjectCache<KingbaseDataSource, KingbaseDatabase> getDatabaseCache()
    {
        return databaseCache;
    }

    public List<KingbaseDatabase> getDatabases()
    {
        return databaseCache.getCachedObjects();
    }

    public KingbaseDatabase getDatabase(String name)
    {
        return databaseCache.getCachedObject(name);
    }

    SettingCache getSettingCache() {
        return settingCache;
    }

    public Collection<KingbaseSetting> getSettings() {
        return settingCache.getCachedObjects();
    }

    public KingbaseSetting getSetting(String name) {
        return settingCache.getCachedObject(name);
    }

    @Override
    public void initialize(@NotNull DBRProgressMonitor monitor)
        throws DBException {
        super.initialize(monitor);

        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Read server information")) {
            session.enableLogging(false);
            try {
                serverVersion = JDBCUtils.queryString(session, "SELECT version()");
            } catch (Exception e) {
                log.debug("Error reading Kingbase version: " + e.getMessage());
                serverVersion = "";
            }


            
            try {
                supportsEnumTable = KingbaseUtils.isMetaObjectExists(session, "sys_enum", "*");
            } catch (Exception e) {
                log.debug("Error reading sys_enum " + e.getMessage());
                supportsEnumTable = false;
            }
          
            try {
                supportsReltypeColumn = KingbaseUtils.isMetaObjectExists(session, "sys_class", "reltype");
            } catch (Exception e) {
                log.debug("Error reading sys_class.reltype " + e.getMessage());
                supportsReltypeColumn = false;
            }
        }
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
    public List<? extends DBSObject> getChildren(@NotNull DBRProgressMonitor monitor) {
        return getDatabases();
    }

    @Override
    public DBSObject getChild(@NotNull DBRProgressMonitor monitor, @NotNull String childName) {
        return getDatabase(childName);
    }

    @NotNull
    @Override
    public Class<? extends DBSObject> getPrimaryChildType(@Nullable DBRProgressMonitor monitor) {
        return KingbaseDatabase.class;
    }

    @Override
    public void cacheStructure(@NotNull DBRProgressMonitor monitor, int scope) {
        databaseCache.getAllObjects(monitor, this);
    }
    
    @Override
    protected Connection openConnection(@NotNull DBRProgressMonitor monitor, @Nullable JDBCExecutionContext context, @NotNull String purpose) throws DBCException {
        final DBPConnectionConfiguration conConfig = getContainer().getActualConnectionConfiguration();

        JDBCRemoteInstance instance = context == null ? null : context.getOwnerInstance();
        Connection kbConnection;
        if (instance != null) {
            log.debug("Initiate connection to " + getServerType().getServerTypeName() + " database [" + instance.getName() + "@" + conConfig.getHostName() + "] for " + purpose);
        }
        boolean timezoneOverridden = false;

        try {
            if (KingbaseConstants.NEW_UA_TIMEZONE.equals(TimeZone.getDefault().getID())) {
                TimezoneRegistry.setDefaultZone(ZoneId.of(KingbaseConstants.LEGACY_UA_TIMEZONE), false);
                timezoneOverridden = true;
            }
            if (conConfig.getConfigurationType() != DBPDriverConfigurationType.URL &&
                instance instanceof KingbaseDatabase &&
                !CommonUtils.equalObjects(instance.getName(), conConfig.getDatabaseName())
            ) {
                // If database was changed then use new name for connection
                final DBPConnectionConfiguration originalConfig = new DBPConnectionConfiguration(conConfig);
                try {
                    // Patch URL with new database name
                    if (CommonUtils.isEmpty(conConfig.getUrl()) || !CommonUtils.isEmpty(conConfig.getHostName())) {
                        conConfig.setDatabaseName(instance.getName());
                        final DBPDriver driver = getContainer().getDriver();
                        String newURL = DatabaseURL.generateUrlByTemplate(driver, conConfig);
                        if (CommonUtils.isEmpty(newURL)) {
                            newURL = driver.getDataSourceProvider().getConnectionURL(driver, conConfig);
                        }
                        conConfig.setUrl(newURL);
                    }

                    kbConnection = super.openConnection(monitor, context, purpose);
                }
                finally {
                    conConfig.setDatabaseName(originalConfig.getDatabaseName());
                    conConfig.setUrl(originalConfig.getUrl());
                }
            } else {
                kbConnection = super.openConnection(monitor, context, purpose);
            }
        } catch (DBCException e) {
            final Throwable cause = GeneralUtils.getRootCause(e);
            final StackTraceElement element = cause.getStackTrace()[0];

            final DBWHandlerConfiguration handler = conConfig.getHandler(KingbaseConstants.HANDLER_SSL);
            if ("sun.security.util.DerValue".equals(element.getClassName()) && handler != null) { //$NON-NLS-1$
                try {
                    final Path dst = DBWorkbench.getPlatform().getTempFolder(monitor, "ssl").resolve(container.getId() + ".pk8");
                    if (SSLHandlerTrustStoreImpl.loadDerFromPem(handler, dst)) {

                        return this.openConnection(monitor, context, purpose);
                    }
                } catch (IOException ex) {
                    log.error("Error converting SSL key", ex);
                    throw e;
                }
            }

            throw e;
        } finally {
            if (timezoneOverridden && KingbaseConstants.LEGACY_UA_TIMEZONE.equals(TimeZone.getDefault().getID())) {
                TimezoneRegistry.setDefaultZone(ZoneId.of(KingbaseConstants.NEW_UA_TIMEZONE), false);
            }
        }

        if (getServerType().supportsClientInfo() && !getContainer().getPreferenceStore().getBoolean(ModelPreferences.META_CLIENT_NAME_DISABLE)) {

            try {
                kbConnection.setClientInfo(JDBCConstants.APPLICATION_NAME_CLIENT_PROPERTY, DBUtils.getClientApplicationName(getContainer(), context, purpose));
            } catch (Throwable e) {
                log.debug(e);
            }
        }

        return kbConnection;
    }

    @Override
    public <T> T getAdapter(Class<T> adapter)
    {
        if (adapter == DBSStructureAssistant.class) {
            return adapter.cast(new KingbaseStructureAssistant(this));
        } else if (adapter == DBCServerOutputReader.class) {
            return adapter.cast(new KingbaseServerOutputReader());
        } else if (adapter == DBAServerSessionManager.class) {
            return adapter.cast(new KingbaseSessionManager(this));
        } else if (adapter == DBCQueryPlanner.class) {
            return adapter.cast(new KingbaseQueryPlaner(this));
        } else if (adapter == DBSDataBulkLoader.class) {
            if (getServerType().supportsCopyFromStdIn()) {
                return adapter.cast(new KingbaseCopyLoader(this));
            }
        } else if (adapter == DBAUserPasswordManager.class) {
            if (getServerType().supportsAlterUserChangePassword()) {
                return adapter.cast(new KingbaseUserPasswordManager(this));
            }
        }
        return super.getAdapter(adapter);
    }

    @Nullable
    @Override
    public DBSDataType resolveDataType(@NotNull DBRProgressMonitor monitor, @NotNull String typeFullName) throws DBException {
        DBSDataType dataType = super.resolveDataType(monitor, typeFullName);
        if (dataType != null) {
            return dataType;
        }
        return KingbaseUtils.resolveTypeFullName(monitor, this, typeFullName);
    }

    @Override
    public Collection<KingbaseDataType> getLocalDataTypes()
    {
        return getDefaultInstance().getLocalDataTypes();
    }

    @Override
    public KingbaseDataType getLocalDataType(String typeName)
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
    public KingbaseDatabase getDefaultInstance() {
        KingbaseDatabase defDatabase = databaseCache.getCachedObject(activeDatabaseName);
        if (defDatabase == null) {
            defDatabase = databaseCache.getCachedObject(KingbaseConstants.DEFAULT_DATABASE);
        }
        if (defDatabase == null) {
            final List<KingbaseDatabase> allDatabases = databaseCache.getCachedObjects();
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
    public List<KingbaseDatabase> getAvailableInstances() {
        return databaseCache.getCachedObjects();
    }

    void setActiveDatabase(KingbaseDatabase newDatabase, DBCExecutionContext context) {
        final KingbaseDatabase oldDatabase = getDefaultInstance();
        if (oldDatabase == newDatabase) {
            return;
        }

        activeDatabaseName = newDatabase.getName();

        // Notify UI
        DBUtils.fireObjectSelect(oldDatabase, false, context);
        DBUtils.fireObjectSelect(newDatabase, true, context);
    }

    public List<String> getTemplateDatabases(DBRProgressMonitor monitor) throws DBException {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load template databases")) {
            try (PreparedStatement dbStat = session.prepareStatement("SELECT db.datname FROM sys_catalog.sys_database db WHERE datistemplate")) {
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

    public KingbaseServerExtension getServerType() {
        if (serverExtension == null) {
            KingbaseServerType serverType = getType();

            try {
                serverExtension = serverType.createServerExtension(this);
            } catch (Throwable e) {
                log.error("Can't determine server type", e);
                serverExtension = new KingbaseServerKingbaseSQL(this);
            }
        }
        return serverExtension;
    }

    public KingbaseServerType getType() {
        return KingbaseUtils.getServerType(getContainer().getDriver());
    }

    public String getServerVersion() {
        return serverVersion;
    }

    public boolean supportsRoles() {
        return getServerType().supportsRoles() && !getContainer().getNavigatorSettings().isShowOnlyEntities() && !getContainer().getNavigatorSettings().isHideFolders();
    }

    @NotNull
    public KingbasePrivilegeType[] getSupportedPrivilegeTypes() {
        return SUPPORTED_PRIVILEGE_TYPES;
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
            Collection<KingbaseDatabase> databases = getDatabases();
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT db.datname,sys_database_size(db.oid) FROM sys_catalog.sys_database db " +
                    (databases.size() == 1 ? "WHERE db.oid=?" : "")))
            {
                if (databases.size() == 1) {
                    dbStat.setLong(1, databases.iterator().next().getObjectId());
                }
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    while (dbResult.next()) {
                        String dbName = JDBCUtils.safeGetString(dbResult, 1);
                        long dbSize = dbResult.getLong(2);
                        KingbaseDatabase database = getDatabase(dbName);
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

    private static class DatabaseCache extends SimpleObjectCache<KingbaseDataSource, KingbaseDatabase> {
    }

    static class SettingCache extends JDBCObjectLookupCache<KingbaseDataSource, KingbaseSetting> {
        @NotNull
        @Override
        public JDBCStatement prepareLookupStatement(@NotNull JDBCSession session, @NotNull KingbaseDataSource owner, @Nullable KingbaseSetting object, @Nullable String objectName) throws SQLException {
            if (object != null || objectName != null) {
                final JDBCPreparedStatement dbStat = session.prepareStatement("select * from sys_catalog.sys_settings where name=?");
                dbStat.setString(1, object != null ? object.getName() : objectName);
                return dbStat;
            }

            return session.prepareStatement("select * from sys_catalog.sys_settings");
        }

        @Nullable
        @Override
        protected KingbaseSetting fetchObject(@NotNull JDBCSession session, @NotNull KingbaseDataSource owner, @NotNull JDBCResultSet dbResult) throws SQLException, DBException {
            return new KingbaseSetting(owner, dbResult);
        }
    }


    private final Pattern ERROR_POSITION_PATTERN = Pattern.compile("\\n\\s*\\p{L}+\\s*: ([0-9]+)");

    @Nullable
    @Override
    public ErrorPosition[] getErrorPosition(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext context, @NotNull String query, @NotNull Throwable error) {
        Throwable rootCause = GeneralUtils.getRootCause(error);
        if (KingbaseConstants.KSQL_EXCEPTION_CLASS_NAME.equals(rootCause.getClass().getName())) {
            try {
                Object serverErrorMessage = BeanUtils.readObjectProperty(rootCause, "serverErrorMessage");
                if (serverErrorMessage != null) {
                    Object position = BeanUtils.readObjectProperty(serverErrorMessage, "position");
                    if (position instanceof Number) {
                        ErrorPosition pos = new ErrorPosition();
                        pos.position = ((Number) position).intValue() - 1;
                        return new ErrorPosition[] {pos};
                    }
                }
            } catch (Throwable e) {
                
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
        return new KingbaseJdbcFactory();
    }

    @Override
    public ErrorType discoverErrorType(@NotNull Throwable error) {
        String sqlState = SQLState.getStateFromException(error);
        if (sqlState != null) {
            if (KingbaseConstants.ERROR_ADMIN_SHUTDOWN.equals(sqlState)) {
                return ErrorType.CONNECTION_LOST;
            } else if (KingbaseConstants.ERROR_TRANSACTION_ABORTED.equals(sqlState)) {
                return ErrorType.TRANSACTION_ABORTED;
            }
        }
        if (getServerType() instanceof DBPErrorAssistant) {
            ErrorType errorType = ((DBPErrorAssistant) getServerType()).discoverErrorType(error);
            if (errorType != null) {
                return errorType;
            }
        }

        return super.discoverErrorType(error);
    }

    @Override
    protected DBPDataSourceInfo createDataSourceInfo(DBRProgressMonitor monitor, @NotNull JDBCDatabaseMetaData metaData)
    {
        return new KingbaseDataSourceInfo(this, metaData);
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

    public boolean supportReadingAllDataTypes() {
        return CommonUtils.toBoolean(getContainer().getActualConnectionConfiguration().getProviderProperty(KingbaseConstants.PROP_READ_ALL_DATA_TYPES));
    }

    public boolean supportsReadingKeysWithColumns() {
        return CommonUtils.toBoolean(
            getContainer().getActualConnectionConfiguration().getProviderProperty(KingbaseConstants.PROP_READ_KEYS_WITH_COLUMNS));
    }

    public boolean isSupportsEnumTable() {
        return supportsEnumTable;
    }

    /**
     * Returns true if a database support sys_ctalog.reltype column. True by default.
     */
    public boolean isSupportsReltypeColumn() {
        return supportsReltypeColumn;
    }
}
