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
package org.jkiss.dbeaver.ext.mysql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBDatabaseException;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.ext.mysql.MySQLConstants;
import org.jkiss.dbeaver.ext.mysql.MySQLDataSourceProvider;
import org.jkiss.dbeaver.ext.mysql.model.plan.MySQLPlanAnalyser;
import org.jkiss.dbeaver.ext.mysql.model.session.MySQLSessionManager;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.access.DBAuthUtils;
import org.jkiss.dbeaver.model.admin.sessions.DBAServerSessionManager;
import org.jkiss.dbeaver.model.app.DBACertificateStorage;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCQueryTransformType;
import org.jkiss.dbeaver.model.exec.DBCQueryTransformer;
import org.jkiss.dbeaver.model.exec.jdbc.*;
import org.jkiss.dbeaver.model.exec.plan.DBCQueryPlanner;
import org.jkiss.dbeaver.model.gis.GisConstants;
import org.jkiss.dbeaver.model.gis.SpatialDataProvider;
import org.jkiss.dbeaver.model.impl.jdbc.*;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCBasicDataTypeCache;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCDataType;
import org.jkiss.dbeaver.model.impl.net.SSLConstants;
import org.jkiss.dbeaver.model.impl.net.SSLHandlerTrustStoreImpl;
import org.jkiss.dbeaver.model.impl.sql.QueryTransformerLimit;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLHelpProvider;
import org.jkiss.dbeaver.model.sql.SQLState;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectFilter;
import org.jkiss.dbeaver.model.struct.DBSStructureAssistant;
import org.jkiss.dbeaver.model.struct.rdb.DBSIndexType;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.CommonUtils;
import org.osgi.framework.Version;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * GenericDataSource
 */
public class MySQLDataSource extends JDBCDataSource implements DBPObjectStatisticsCollector {
    private static final Log log = Log.getLog(MySQLDataSource.class);
    private static final Pattern VERSION_PATTERN = Pattern.compile("([0-9]+\\.[0-9]+\\.[0-9]+).+");

    private static final Map<String, String> PROHIBITED_DRIVER_PROPERTIES = new HashMap<>();

    static {
        PROHIBITED_DRIVER_PROPERTIES.putAll(Map.of(
            "autoDeserialize", "false",
            "allowLocalInfile", "false",
            "allowLoadLocalInfile", "false",
            "allowUrlInLocalInfile", "false"
        ));
        PROHIBITED_DRIVER_PROPERTIES.put("allowLoadLocalInfileInPath", null);
    }

    private final JDBCBasicDataTypeCache<MySQLDataSource, JDBCDataType> dataTypeCache;
    private List<MySQLEngine> engines;
    private final CatalogCache catalogCache = new CatalogCache() {
        @Override
        protected void detectCaseSensitivity(DBSObject object) {
            setCaseSensitive(!getDataSource().getSQLDialect().useCaseInsensitiveNameLookup());
        }
    };
    private List<MySQLPrivilege> privileges;
    private List<MySQLUser> users;
    private List<MySQLCharset> charsets;
    private List<MySQLPlugin> plugins;
    private Map<String, MySQLCollation> collations;
    private String defaultCharset, defaultCollation;
    private int lowerCaseTableNames = 1;
    private SQLHelpProvider helpProvider;
    private volatile boolean hasStatistics;
    private boolean containsCheckConstraintTable;

    private transient boolean inServerTimezoneHandle;

    private Boolean readeAllCaches;
    private Version version;

    public MySQLDataSource(DBRProgressMonitor monitor, DBPDataSourceContainer container) throws DBException {
        this(monitor, container, new MySQLDialect());
    }
    
    public MySQLDataSource(DBRProgressMonitor monitor, DBPDataSourceContainer container, SQLDialect dialect)
            throws DBException {
        super(monitor, container, dialect);
        dataTypeCache = new JDBCBasicDataTypeCache<>(this);
        hasStatistics = !container.getPreferenceStore().getBoolean(ModelPreferences.READ_EXPENSIVE_STATISTICS);
    }

    @Override
    public Object getDataSourceFeature(String featureId) {
        switch (featureId) {
            case DBPDataSource.FEATURE_MAX_STRING_LENGTH:
                if (isServerVersionAtLeast(5, 0)) {
                    return 65535;
                } else {
                    return 255;
                }
            case DBPDataSource.FEATURE_LIMIT_AFFECTS_DML:
                return true;
        }
        return super.getDataSourceFeature(featureId);
    }

    int getLowerCaseTableNames() {
        return lowerCaseTableNames;
    }

    @Override
    protected Map<String, String> getInternalConnectionProperties(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBPDriver driver,
        @NotNull JDBCExecutionContext context,
        @NotNull String purpose,
        @NotNull DBPConnectionConfiguration connectionInfo
    ) throws DBCException {
        Map<String, String> props = new LinkedHashMap<>(MySQLDataSourceProvider.getConnectionsProps());
        final DBWHandlerConfiguration sslConfig = getContainer().getActualConnectionConfiguration().getHandler(MySQLConstants.HANDLER_SSL);
        if (sslConfig != null && sslConfig.isEnabled()) {
            try {
                initSSL(monitor, props, sslConfig);
            } catch (Exception e) {
                throw new DBCException("Error configuring SSL certificates", e);
            }
        } else {
            // Newer MySQL servers/connectors requires explicit SSL disable
            props.put("useSSL", "false");
        }

        String serverTZ = connectionInfo.getProviderProperty(MySQLConstants.PROP_SERVER_TIMEZONE);
        if (CommonUtils.isEmpty(serverTZ) && inServerTimezoneHandle/*&& getContainer().getDriver().getId().equals(MySQLConstants.DRIVER_ID_MYSQL8)*/) {
            serverTZ = "UTC";
        }
        if (!CommonUtils.isEmpty(serverTZ)) {
            props.put("serverTimezone", serverTZ);
        }

        if (!isMariaDB()) {
            // Hacking different MySQL drivers zeroDateTimeBehavior property (#4103)
            String zeroDateTimeBehavior = connectionInfo.getProperty(MySQLConstants.PROP_ZERO_DATETIME_BEHAVIOR);
            if (zeroDateTimeBehavior == null) {
                try {
                    Driver driverInstance = (Driver) driver.getDriverInstance(monitor);
                    if (driverInstance != null) {
                        if (driverInstance.getMajorVersion() >= 8) {
                            props.put(MySQLConstants.PROP_ZERO_DATETIME_BEHAVIOR, "CONVERT_TO_NULL");
                        } else {
                            props.put(MySQLConstants.PROP_ZERO_DATETIME_BEHAVIOR, "convertToNull");
                        }
                    }
                } catch (Exception e) {
                    log.debug("Error setting MySQL " + MySQLConstants.PROP_ZERO_DATETIME_BEHAVIOR + " property default");
                }
            }
        }

        return props;
    }

    @Override
    protected DBPDataSourceInfo createDataSourceInfo(DBRProgressMonitor monitor, @NotNull JDBCDatabaseMetaData metaData) {
        return new MySQLDataSourceInfo(metaData);
    }

    private void initSSL(DBRProgressMonitor monitor, Map<String, String> props, DBWHandlerConfiguration sslConfig) throws Exception {
        monitor.subTask("Install SSL certificates");
        final DBACertificateStorage securityManager = DBWorkbench.getPlatform().getCertificateStorage();

        props.put("useSSL", "true");
        if (isMariaDB()) {
            props.put("trustServerCertificate", String.valueOf(!sslConfig.getBooleanProperty(MySQLConstants.PROP_VERIFY_SERVER_SERT)));
        } else {
            props.put("verifyServerCertificate", String.valueOf(sslConfig.getBooleanProperty(MySQLConstants.PROP_VERIFY_SERVER_SERT)));
            props.put("requireSSL", String.valueOf(sslConfig.getBooleanProperty(MySQLConstants.PROP_REQUIRE_SSL)));
        }

        {
            // Trust keystore
            byte[] caCertData = SSLHandlerTrustStoreImpl.readCertificate(sslConfig, SSLHandlerTrustStoreImpl.PROP_SSL_CA_CERT, MySQLConstants.PROP_SSL_CA_CERT);
            byte[] clientCertData = SSLHandlerTrustStoreImpl.readCertificate(sslConfig, SSLHandlerTrustStoreImpl.PROP_SSL_CLIENT_CERT, MySQLConstants.PROP_SSL_CLIENT_CERT);
            byte[] keyData = SSLHandlerTrustStoreImpl.readCertificate(sslConfig, SSLHandlerTrustStoreImpl.PROP_SSL_CLIENT_KEY, MySQLConstants.PROP_SSL_CLIENT_KEY);
            if (caCertData != null || clientCertData != null) {
                securityManager.addCertificate(getContainer(), SSLConstants.SSL_CERT_TYPE, caCertData, clientCertData, keyData);
            } else {
                securityManager.deleteCertificate(getContainer(), SSLConstants.SSL_CERT_TYPE);
            }
            final String ksPath = makeKeyStorePath(securityManager.getKeyStorePath(getContainer(), SSLConstants.SSL_CERT_TYPE));
            final char[] ksPass = securityManager.getKeyStorePassword(getContainer(), SSLConstants.SSL_CERT_TYPE);
            if (isMariaDB()) {
                props.put("trustStore", ksPath);
                props.put("trustStorePassword", String.valueOf(ksPass));
            } else {
                props.put("clientCertificateKeyStoreUrl", ksPath);
                props.put("trustCertificateKeyStoreUrl", ksPath);
                props.put("clientCertificateKeyStorePassword", String.valueOf(ksPass));
                props.put("trustCertificateKeyStorePassword", String.valueOf(ksPass));
            }
        }
        final String cipherSuites = sslConfig.getStringProperty(MySQLConstants.PROP_SSL_CIPHER_SUITES);
        if (!CommonUtils.isEmpty(cipherSuites)) {
            props.put("enabledSSLCipherSuites;", cipherSuites);
        }
        final boolean retrievePublicKey = sslConfig.getBooleanProperty(MySQLConstants.PROP_SSL_PUBLIC_KEY_RETRIEVE);
        if (retrievePublicKey) {
            props.put("allowPublicKeyRetrieval", "true");
        }

        if (sslConfig.getBooleanProperty(MySQLConstants.PROP_SSL_DEBUG)) {
            System.setProperty("javax.net.debug", "all");
        }
    }

    private String makeKeyStorePath(Path keyStorePath) throws MalformedURLException {
        if (isMariaDB()) {
            return keyStorePath.toAbsolutePath().toString();
        } else {
            return keyStorePath.toUri().toURL().toString();
        }
    }

    @Override
    protected JDBCExecutionContext createExecutionContext(JDBCRemoteInstance instance, String type) {
        return new MySQLExecutionContext(instance, type);
    }

    protected void initializeContextState(@NotNull DBRProgressMonitor monitor, @NotNull JDBCExecutionContext context, JDBCExecutionContext initFrom) throws DBException {
        if (initFrom != null && !context.getDataSource().getContainer().isConnectionReadOnly()) {
            MySQLCatalog object = ((MySQLExecutionContext)initFrom).getDefaultCatalog();
            if (object != null) {
                ((MySQLExecutionContext)context).setCurrentDatabase(monitor, object);
            }
        } else {
            ((MySQLExecutionContext)context).refreshDefaults(monitor, true);
        }
    }

    public String[] getTableTypes() {
        return MySQLConstants.TABLE_TYPES;
    }

    public CatalogCache getCatalogCache() {
        return catalogCache;
    }

    public Collection<MySQLCatalog> getCatalogs() {
        return catalogCache.getCachedObjects();
    }

    public MySQLCatalog getCatalog(String name) {
        return catalogCache.getCachedObject(name);
    }

    @Override
    public void initialize(@NotNull DBRProgressMonitor monitor)
        throws DBException {
        super.initialize(monitor);

        dataTypeCache.getAllObjects(monitor, this);
        if ((!isMariaDB() && isServerVersionAtLeast(5, 7))
            && dataTypeCache.getCachedObject(MySQLConstants.TYPE_JSON) == null)
        {
            // For MariaDB JSON is an alias for LONGTEXT introduced for compatibility reasons with MySQL's JSON data type.
            // Even if you through the SQL Editor create a JSON column, it will turn into longtext
            dataTypeCache.cacheObject(
                new JDBCDataType<>(
                    this,
                    java.sql.Types.OTHER,
                    MySQLConstants.TYPE_JSON,
                    MySQLConstants.TYPE_JSON,
                    false,
                    true,
                    0,
                    0,
                    0));
        }
        if ((isMariaDB() && isServerVersionAtLeast(5, 4))
            || (!isMariaDB() && isServerVersionAtLeast(5, 7))
            && dataTypeCache.getCachedObject(MySQLConstants.TYPE_GEOMETRY) == null) {
            addGISDatatype(MySQLConstants.TYPE_GEOMETRY);
            addGISDatatype(MySQLConstants.TYPE_POINT);
            addGISDatatype(MySQLConstants.TYPE_LINESTRING);
            addGISDatatype(MySQLConstants.TYPE_POLYGON);
            addGISDatatype(MySQLConstants.TYPE_MULTIPOINT);
            addGISDatatype(MySQLConstants.TYPE_MULTILINESTRING);
            addGISDatatype(MySQLConstants.TYPE_MULTIPOLYGON);
            addGISDatatype(MySQLConstants.TYPE_GEOMETRYCOLLECTION);

        }
        if (isMariaDB() && isServerVersionAtLeast(10, 7) && dataTypeCache.getCachedObject(MySQLConstants.TYPE_UUID) == null) {
            // Not supported by MariaDB driver for now (3.0.8). Waiting for the driver support
            dataTypeCache.cacheObject(
                new JDBCDataType<>(
                    this,
                    Types.CHAR,
                    MySQLConstants.TYPE_UUID,
                    MySQLConstants.TYPE_UUID,
                    false,
                    true,
                    0,
                    0,
                    0));
        }
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load basic datasource metadata")) {
            // Read engines
            {
                engines = new ArrayList<>();
                try (JDBCPreparedStatement dbStat = session.prepareStatement("SHOW ENGINES")) {
                    try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                        while (dbResult.next()) {
                            MySQLEngine engine = new MySQLEngine(this, dbResult);
                            engines.add(engine);
                        }
                    }
                } catch (SQLException ex) {
                    // Engines are not supported. Shame on it. Leave this list empty
                }
            }

            // Read charsets and collations
            {
                charsets = new ArrayList<>();
                try (JDBCPreparedStatement dbStat = session.prepareStatement("SHOW CHARSET")) {
                    try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                        while (dbResult.next()) {
                            MySQLCharset charset = new MySQLCharset(this, dbResult);
                            charsets.add(charset);
                        }
                    }
                } catch (SQLException ex) {
                    // Engines are not supported. Shame on it. Leave this list empty
                }
                charsets.sort(DBUtils.<MySQLCharset>nameComparator());

                collations = new LinkedHashMap<>();
                try (JDBCPreparedStatement dbStat = session.prepareStatement("SHOW COLLATION")) {
                    try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                        while (dbResult.next()) {
                            String charsetName = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_CHARSET);
                            MySQLCharset charset = getCharset(charsetName);
                            if (charset == null) {
                                log.warn("Charset '" + charsetName + "' not found.");
                                continue;
                            }
                            MySQLCollation collation = new MySQLCollation(charset, dbResult);
                            collations.put(collation.getName(), collation);
                            charset.addCollation(collation);
                        }
                    }
                } catch (SQLException ex) {
                    // Engines are not supported. Shame on it. Leave this list empty
                }

                try (JDBCPreparedStatement dbStat = session.prepareStatement("SELECT @@GLOBAL.character_set_server,@@GLOBAL.collation_server")) {
                    try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                        if (dbResult.next()) {
                            defaultCharset = JDBCUtils.safeGetString(dbResult, 1);
                            defaultCollation = JDBCUtils.safeGetString(dbResult, 2);
                        }
                    }
                } catch (Throwable ex) {
                    log.debug("Error reading default server charset/collation", ex);
                }

            }

            // Read plugins
            {
                plugins = new ArrayList<>();
                if (supportsPlugins()) {
                    try (JDBCPreparedStatement dbStat = session.prepareStatement("SHOW PLUGINS")) {
                        try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                            while (dbResult.next()) {
                                plugins.add(new MySQLPlugin(this, dbResult));
                            }
                        }
                    } catch (SQLException e) {
                        log.debug("Error reading plugins information", e);
                    }
                }
            }

            try (JDBCPreparedStatement dbStat = session.prepareStatement("SHOW VARIABLES LIKE 'lower_case_table_names'")) {
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    if (dbResult.next()) {
                        lowerCaseTableNames = JDBCUtils.safeGetInt(dbResult, 2);
                    }
                }
            } catch (Throwable ex) {
                log.debug("Error reading default server charset/collation", ex);
            }

            // Read catalogs
            catalogCache.getAllObjects(monitor, this);
            //activeCatalogName = MySQLUtils.determineCurrentDatabase(session);

            if (supportsInformationSchema()) {
                // Check check constraints in base
                try {
                    String resultSet = JDBCUtils.queryString(session, "SELECT * FROM information_schema.TABLES t\n" +
                            "WHERE\n" +
                            "\tt.TABLE_SCHEMA = 'information_schema'\n" +
                            "\tAND t.TABLE_NAME = 'CHECK_CONSTRAINTS'");
                    containsCheckConstraintTable = (resultSet != null);
                } catch (SQLException e) {
                    log.debug("Error reading information schema", e);
                }
            }
        }
    }

    private void addGISDatatype(String typeGeometry) {
        dataTypeCache.cacheObject(new JDBCDataType<>(this,
            Types.OTHER,
            typeGeometry.toUpperCase(Locale.ROOT),
            typeGeometry.toUpperCase(Locale.ROOT),
            false,
            true,
            0,
            0,
            0));
    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor)
        throws DBException {
        super.refreshObject(monitor);

        this.engines = null;
        this.catalogCache.clearCache();
        this.users = null;

        this.initialize(monitor);

        return this;
    }

    MySQLTable findTable(DBRProgressMonitor monitor, String catalogName, String tableName)
        throws DBException {
        if (CommonUtils.isEmpty(catalogName)) {
            return null;
        }
        MySQLCatalog catalog = getCatalog(catalogName);
        if (catalog == null) {
            log.error("Catalog " + catalogName + " not found");
            return null;
        }
        return catalog.getTable(monitor, tableName);
    }

    @Override
    public Collection<? extends MySQLCatalog> getChildren(@NotNull DBRProgressMonitor monitor) {
        return getCatalogs();
    }

    @Override
    public MySQLCatalog getChild(@NotNull DBRProgressMonitor monitor, @NotNull String childName) {
        return getCatalog(childName);
    }

    @NotNull
    @Override
    public Class<? extends MySQLCatalog> getPrimaryChildType(@Nullable DBRProgressMonitor monitor) {
        return MySQLCatalog.class;
    }

    @Override
    public void cacheStructure(@NotNull DBRProgressMonitor monitor, int scope) {

    }

    @Override
    protected Connection openConnection(@NotNull DBRProgressMonitor monitor, @Nullable JDBCExecutionContext context, @NotNull String purpose) throws DBCException {
        Connection mysqlConnection;
        try {
            mysqlConnection = super.openConnection(monitor, context, purpose);
        } catch (DBCException e) {
            if (e.getCause() instanceof SQLException &&
                SQLState.SQL_01S00.getCode().equals (((SQLException) e.getCause()).getSQLState()) &&
                CommonUtils.isEmpty(getContainer().getActualConnectionConfiguration().getProviderProperty(MySQLConstants.PROP_SERVER_TIMEZONE)))
            {
                // Workaround for nasty problem with MySQL 8 driver and serverTimezone error
                log.debug("Error connecting without serverTimezone. Trying to set serverTimezone=UTC. Original error: " + e.getMessage());
                inServerTimezoneHandle = true;
                try {
                    mysqlConnection = super.openConnection(monitor, context, purpose);
                } catch (DBCException e2) {
                    inServerTimezoneHandle = false;
                    throw e2;
                }
            } else if (
                SQLState.getCodeFromException(e) == MySQLConstants.ER_MUST_CHANGE_PASSWORD_LOGIN &&
                DBAuthUtils.promptAndChangePasswordForCurrentUser(monitor, container, this::changeUserPassword)
            ) {
                return openConnection(monitor, context, purpose);
            } else {
                throw e;
            }
        }

        if (!getContainer().getPreferenceStore().getBoolean(ModelPreferences.META_CLIENT_NAME_DISABLE)) {
            // Provide client info
            try {
                mysqlConnection.setClientInfo(JDBCConstants.APPLICATION_NAME_CLIENT_PROPERTY, DBUtils.getClientApplicationName(getContainer(), context, purpose));
            } catch (Throwable e) {
                // just ignore
                log.debug(e);
            }
        }

        return mysqlConnection;
    }

    public List<MySQLUser> getUsers(DBRProgressMonitor monitor)
        throws DBException {
        if (users == null) {
            users = loadUsers(monitor);
        }
        return users;
    }

    public MySQLUser getUser(DBRProgressMonitor monitor, String name)
        throws DBException {
        return DBUtils.findObject(getUsers(monitor), name);
    }

    private List<MySQLUser> loadUsers(DBRProgressMonitor monitor)
        throws DBException {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load users")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement("SELECT * FROM mysql.user ORDER BY user")) {
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    List<MySQLUser> userList = new ArrayList<>();
                    while (dbResult.next()) {
                        MySQLUser user = new MySQLUser(this, dbResult);
                        userList.add(user);
                    }
                    return userList;
                }
            }
        } catch (SQLException ex) {
            throw new DBDatabaseException(ex, this);
        }
    }

    public List<MySQLEngine> getEngines() {
        return engines;
    }

    public MySQLEngine getEngine(String name) {
        return DBUtils.findObject(engines, name);
    }

    public MySQLEngine getDefaultEngine() {
        for (MySQLEngine engine : engines) {
            if (engine.getSupport() == MySQLEngine.Support.DEFAULT) {
                return engine;
            }
        }
        return null;
    }

    public Collection<MySQLCharset> getCharsets() {
        return charsets;
    }

    public MySQLCharset getCharset(String name) {
        for (MySQLCharset charset : charsets) {
            if (charset.getName().equals(name)) {
                return charset;
            }
        }
        return null;
    }

    public MySQLCollation getCollation(String name) {
        return collations.get(name);
    }

    public MySQLCharset getDefaultCharset() {
        return getCharset(defaultCharset);
    }

    public MySQLCollation getDefaultCollation() {
        return getCollation(defaultCollation);
    }

    @NotNull
    public Collection<MySQLPlugin> getPlugins() {
        return plugins;
    }

    @Nullable
    public MySQLPlugin getPlugin(@NotNull String name) {
        for (MySQLPlugin plugin : plugins) {
            if (plugin.getName().equals(name)) {
                return plugin;
            }
        }
        return null;
    }

    public List<MySQLPrivilege> getPrivileges(DBRProgressMonitor monitor)
        throws DBException {
        if (privileges == null) {
            privileges = loadPrivileges(monitor);
        }
        return privileges;
    }

    public List<MySQLPrivilege> getPrivilegesByKind(DBRProgressMonitor monitor, MySQLPrivilege.Kind kind)
        throws DBException {
        List<MySQLPrivilege> privs = new ArrayList<>();
        for (MySQLPrivilege priv : getPrivileges(monitor)) {
            if (priv.getKind() == kind) {
                privs.add(priv);
            }
        }
        return privs;
    }

    public MySQLPrivilege getPrivilege(DBRProgressMonitor monitor, String name)
        throws DBException {
        return DBUtils.findObject(getPrivileges(monitor), name, true);
    }

    private List<MySQLPrivilege> loadPrivileges(DBRProgressMonitor monitor)
        throws DBException {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load privileges")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement("SHOW PRIVILEGES")) {
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    List<MySQLPrivilege> privileges = new ArrayList<>();
                    while (dbResult.next()) {
                        String context = JDBCUtils.safeGetString(dbResult, "context");
                        if (CommonUtils.isEmpty(context)) {
                            log.debug("Skip privilege with an empty context.");
                            continue;
                        }
                        MySQLPrivilege user = new MySQLPrivilege(this, context, dbResult);
                        privileges.add(user);
                    }
                    return privileges;
                }
            }
        } catch (SQLException ex) {
            throw new DBDatabaseException(ex, this);
        }
    }

    public List<MySQLParameter> getSessionStatus(DBRProgressMonitor monitor)
        throws DBException {
        return loadParameters(monitor, true, false);
    }

    public List<MySQLParameter> getGlobalStatus(DBRProgressMonitor monitor)
        throws DBException {
        return loadParameters(monitor, true, true);
    }

    public List<MySQLParameter> getSessionVariables(DBRProgressMonitor monitor)
        throws DBException {
        return loadParameters(monitor, false, false);
    }

    public List<MySQLParameter> getGlobalVariables(DBRProgressMonitor monitor)
        throws DBException {
        return loadParameters(monitor, false, true);
    }

    public List<MySQLDataSource> getInformation() {
        return Collections.singletonList(this);
    }

    private List<MySQLParameter> loadParameters(DBRProgressMonitor monitor, boolean status, boolean global) throws DBException {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load status")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SHOW " +
                    (global ? "GLOBAL " : "") +
                    (status ? "STATUS" : "VARIABLES"))) {
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    List<MySQLParameter> parameters = new ArrayList<>();
                    while (dbResult.next()) {
                        MySQLParameter parameter = new MySQLParameter(
                            this,
                            JDBCUtils.safeGetString(dbResult, "variable_name"),
                            JDBCUtils.safeGetString(dbResult, "value"));
                        parameters.add(parameter);
                    }
                    return parameters;
                }
            }
        } catch (SQLException ex) {
            throw new DBDatabaseException(ex, this);
        }
    }

    @Override
    public DBCQueryTransformer createQueryTransformer(@NotNull DBCQueryTransformType type) {
        if (type == DBCQueryTransformType.RESULT_SET_LIMIT) {
            return new QueryTransformerLimit();
        } else if (type == DBCQueryTransformType.FETCH_ALL_TABLE) {
            return new QueryTransformerFetchAll(this);
        }
        return super.createQueryTransformer(type);
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter == DBSStructureAssistant.class) {
            return adapter.cast(new MySQLStructureAssistant(this));
        } else if (adapter == SQLHelpProvider.class) {
            if (helpProvider == null) {
                helpProvider = new MySQLHelpProvider(this);
            }
            return adapter.cast(helpProvider);
        } else if (adapter == DBAServerSessionManager.class) {
            return adapter.cast(new MySQLSessionManager(this));
        } else if (adapter == SpatialDataProvider.class) {
            return adapter.cast(new SpatialDataProvider() {
                @Override
                public boolean isFlipCoordinates() {
                    return false;
                }
                @Override
                public int getDefaultSRID() {
                    return GisConstants.SRID_4326;
                }
            });
        } else if (adapter == DBCQueryPlanner.class) {
            return adapter.cast(new MySQLPlanAnalyser(this));
        }
        return super.getAdapter(adapter);
    }

    @Override
    public Collection<? extends DBSDataType> getLocalDataTypes() {
        return dataTypeCache.getCachedObjects();
    }

    public JDBCBasicDataTypeCache<MySQLDataSource, JDBCDataType> getDataTypeCache() {
        return dataTypeCache;
    }

    @Override
    public DBSDataType getLocalDataType(String typeName) {
        return dataTypeCache.getCachedObject(typeName);
    }

    @Override
    public DBSDataType getLocalDataType(int typeID) {
        return dataTypeCache.getCachedObject(typeID);
    }

    @Override
    public String getDefaultDataTypeName(DBPDataKind dataKind) {
        switch (dataKind) {
            case BOOLEAN:
                return "TINYINT(1)";
            case NUMERIC:
                return "BIGINT";
            case DATETIME:
                return "TIMESTAMP";
            case BINARY:
                return "BINARY";
            case CONTENT:
                return "LONGBLOB";
            case ROWID:
                return "BINARY";
            default:
                return "VARCHAR";
        }
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
        if (!this.isMariaDB() && !this.isServerVersionAtLeast(4, 1)) {
            // Not supported by MySQL server
            hasStatistics = true;
            return;
        }

        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load table status")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT table_schema, SUM(data_length + index_length) \n" +
                    "FROM information_schema.tables \n" +
                    "GROUP BY table_schema"))
            {
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    while (dbResult.next()) {
                        String dbName = dbResult.getString(1);
                        MySQLCatalog catalog = catalogCache.getObject(monitor, this, dbName);
                        if (catalog != null) {
                            long dbSize = dbResult.getLong(2);
                            catalog.setDatabaseSize(dbSize);
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

    public class CatalogCache extends JDBCObjectCache<MySQLDataSource, MySQLCatalog> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull MySQLDataSource owner) throws SQLException {
            StringBuilder catalogQuery = new StringBuilder("show databases");
            DBSObjectFilter catalogFilters = owner.getContainer().getObjectFilter(MySQLCatalog.class, null, false);
            if (catalogFilters != null) {
                boolean supportsCondition = owner.supportsConditionForShowDatabasesStatement();
                if (!supportsCondition) {
                    catalogQuery.setLength(0);
                    catalogQuery.append("SELECT SCHEMA_NAME FROM ").append(MySQLConstants.META_TABLE_SCHEMATA);
                }
                JDBCUtils.appendFilterClause(
                    catalogQuery,
                    catalogFilters,
                    supportsCondition ? MySQLConstants.COL_DATABASE_NAME : MySQLConstants.COL_SCHEMA_NAME,
                    true,
                    owner);
            }
            JDBCPreparedStatement dbStat = session.prepareStatement(catalogQuery.toString());
            if (catalogFilters != null) {
                JDBCUtils.setFilterParameters(dbStat, 1, catalogFilters);
            }
            return dbStat;
        }

        @Override
        protected MySQLCatalog fetchObject(@NotNull JDBCSession session, @NotNull MySQLDataSource owner, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return createCatalogInstance(owner, resultSet);
        }

    }

    @NotNull
    public MySQLCatalog createCatalogInstance(@NotNull MySQLDataSource owner, @NotNull JDBCResultSet resultSet) {
        return new MySQLCatalog(owner, resultSet);
    }

    public boolean isMariaDB() {
        return MySQLConstants.DRIVER_CLASS_MARIA_DB.equals(
            getContainer().getDriver().getDriverClassName());
    }

    @Override
    public ErrorType discoverErrorType(@NotNull Throwable error) {
        if (isMariaDB()) {
            // MariaDB-specific. They have bad SQLState support
            if ("08".equals(SQLState.getStateFromException(error))) {
                return ErrorType.CONNECTION_LOST;
            }
        }
        return super.discoverErrorType(error);
    }

    private Pattern ERROR_POSITION_PATTERN = Pattern.compile(" at line ([0-9]+)");

    @Nullable
    @Override
    public ErrorPosition[] getErrorPosition(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext context, @NotNull String query, @NotNull Throwable error) {
        String message = error.getMessage();
        if (!CommonUtils.isEmpty(message)) {
            Matcher matcher = ERROR_POSITION_PATTERN.matcher(message);
            if (matcher.find()) {
                DBPErrorAssistant.ErrorPosition pos = new DBPErrorAssistant.ErrorPosition();
                pos.line = Integer.parseInt(matcher.group(1)) - 1;
                return new ErrorPosition[]{pos};
            }
        }
        return null;
    }

    public boolean supportsCheckConstraints() {
        if (this.isMariaDB()) {
            return this.isServerVersionAtLeast(10, 2) && containsCheckConstraintTable;
        }
        else {
            return this.isServerVersionAtLeast(8, 0) && containsCheckConstraintTable;
        }
    }

    /**
     * Checks if information_schema table is supported.
     *
     * <p>The table was not supported up until MySQL 5.0
     *
     * @return {@code true} if information_schema is supported
     */
    public boolean supportsInformationSchema() {
        return isServerVersionAtLeast(5, 0);
    }

    public boolean supportsSequences() {
        if (this.isMariaDB()) {
            return this.isServerVersionAtLeast(10, 3);
        }
        return false;
    }

    /**
     * Checks if column statistics is supported.
     *
     * @return {@code true} if column statistics is supported
     */
    public boolean supportsColumnStatistics() {
        return !isMariaDB() && isServerVersionAtLeast(8, 0);
    }

    public boolean supportsUserManagement() {
        return CommonUtils.getBoolean(getContainer().getDriver().getDriverParameter("supports-users"), true);
    }

    public boolean supportsEvents() {
        return CommonUtils.getBoolean(getContainer().getDriver().getDriverParameter("supports-events"), true);
    }

    public boolean supportsAlterView() {
        return CommonUtils.getBoolean(getContainer().getDriver().getDriverParameter("supports-alter-view"), false);
    }

    /**
     * Checks plugins list reading is supported.
     *
     * @return {@code true} if plugins list reading is supported
     */
    @Association
    public boolean supportsPlugins() {
        return CommonUtils.getBoolean(getContainer().getDriver().getDriverParameter("supports-plugins"), true);
    }


    /**
     * Checks if table partitioning is supported.
     *
     * @return {@code true} if table partitioning is supported
     */
    @Association
    public boolean supportsPartitions() {
        return
            CommonUtils.getBoolean(getContainer().getDriver().getDriverParameter("supports-partitions"), true) &&
            isServerVersionAtLeast(5, 1);
    }

    /**
     * Returns true if table/catalog triggers are supported.
     */
    @Association
    public boolean supportsTriggers() {
        return CommonUtils.getBoolean(getContainer().getDriver().getDriverParameter("supports-triggers"), true);
    }

    /**
     * Returns true if the charsets information is supported. Ex. for table creation.
     */
    @Association
    public boolean supportsCharsets() {
        return CommonUtils.getBoolean(getContainer().getDriver().getDriverParameter("supports-charsets"), true);
    }

    /**
     * Returns true if the collation information is supported. Ex. for table creation.
     */
    @Association
    public boolean supportsCollations() {
        return CommonUtils.getBoolean(getContainer().getDriver().getDriverParameter("supports-collations"), true);
    }

    /**
     * Returns true if local clients using is supported.
     */
    @Association
    public boolean supportsNativeClients() {
        return CommonUtils.getBoolean(getContainer().getDriver().getDriverParameter(MySQLConstants.DRIVER_PARAM_CLIENTS), true);
    }

    public boolean isSystemCatalog(String name) {
        return MySQLConstants.INFO_SCHEMA_NAME.equalsIgnoreCase(name) ||
            MySQLConstants.PERFORMANCE_SCHEMA_NAME.equalsIgnoreCase(name) ||
            MySQLConstants.MYSQL_SCHEMA_NAME.equalsIgnoreCase(name);
    }

    /**
     * Checks if it is possible to fetch transform
     */
    public boolean supportsFetchTransform() {
        return CommonUtils.getBoolean(getContainer().getDriver().getDriverParameter("supports-mysql-fetch-transform"), true);
    }

    public boolean supportsSysSchema() {
        return isMariaDB() ? isServerVersionAtLeast(10, 6) : isServerVersionAtLeast(5, 7);
    }

    /**
     * Returns list of supported index types
     */
    public List<DBSIndexType> supportedIndexTypes() {
        return Arrays.asList(MySQLConstants.INDEX_TYPE_BTREE,
            MySQLConstants.INDEX_TYPE_FULLTEXT,
            MySQLConstants.INDEX_TYPE_HASH,
            MySQLConstants.INDEX_TYPE_RTREE);
    }

    /**
     * Returns true if different rename table syntax is used
     */
    public boolean supportsAlterTableRenameSyntax() {
        return false;
    }

    /**
     * Return true if WHERE condition can be added for SHOW DATABASES statement
     */
    public boolean supportsConditionForShowDatabasesStatement() {
        return true;
    }

    private Version getVersion() {
        if (version == null) {
            String versionInfo = getInfo().getDatabaseProductVersion(); // getInfo().getDatabaseVersion() can return incorrect value
            Matcher matcher = VERSION_PATTERN.matcher(versionInfo);
            if (matcher.matches()) {
                version = new Version(matcher.group(1));
            }
        }
        return version;
    }

    /**
     * Return true if a special setting about metadata cache reading was enabled in advanced driver parameters or by version number.
     */
    public boolean readKeysWithColumns() {
        if (readeAllCaches == null) {
            readeAllCaches = CommonUtils.getBoolean(getContainer().getDriver().getDriverParameter(
                MySQLConstants.PROP_CACHE_META_DATA),
                true);
            if (readeAllCaches) {
                if (isMariaDB()) {
                    readeAllCaches = isServerVersionAtLeast(10, 4);
                } else if (getVersion() != null) {
                    Version version = getVersion();
                    readeAllCaches = version.getMajor() >= 8 && version.getMinor() >= 0 && version.getMicro() >= 21;
                }
            }
        }
        return readeAllCaches;
    }

    @Override
    protected void fillConnectionProperties(DBPConnectionConfiguration connectionInfo, Properties connectProps) {
        super.fillConnectionProperties(connectionInfo, connectProps);

        if (!DBWorkbench.getPlatform().getApplication().isMultiuser()) {
            return;
        }

        for (String prohibitedDriverProperty : PROHIBITED_DRIVER_PROPERTIES.keySet()) {
            if (connectProps.containsKey(prohibitedDriverProperty)) {
                log.warn("The driver settings contain a prohibited property, this property will be forcibly removed: "
                    + prohibitedDriverProperty);
            }
            String propertyValue = PROHIBITED_DRIVER_PROPERTIES.get(prohibitedDriverProperty);
            if (propertyValue == null) {
                connectProps.remove(prohibitedDriverProperty);
            } else {
                log.trace("Set " + prohibitedDriverProperty + ":" + propertyValue);
                connectProps.put(prohibitedDriverProperty, propertyValue);
            }
        }
    }

    private void changeUserPassword(
        @NotNull DBRProgressMonitor monitor,
        @NotNull String userName,
        @NotNull String newPassword,
        @NotNull String oldPassword
    ) throws DBException {
        container.getActualConnectionConfiguration().setProperty("disconnectOnExpiredPasswords", "false");
        try (Connection connection = super.openConnection(monitor, null, "Change expired password")) {
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("SET PASSWORD = %s REPLACE %s".formatted(
                    SQLUtils.quoteString(this, newPassword),
                    SQLUtils.quoteString(this, oldPassword)
                ));
            }
        } catch (SQLException e) {
            throw new DBDatabaseException("Unable to change expired password", e);
        }
    }
}
