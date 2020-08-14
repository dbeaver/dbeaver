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
package org.jkiss.dbeaver.ext.mysql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.ext.mysql.MySQLConstants;
import org.jkiss.dbeaver.ext.mysql.MySQLDataSourceProvider;
import org.jkiss.dbeaver.ext.mysql.model.plan.MySQLPlanAnalyser;
import org.jkiss.dbeaver.ext.mysql.model.session.MySQLSessionManager;
import org.jkiss.dbeaver.model.*;
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
import org.jkiss.dbeaver.model.impl.sql.QueryTransformerLimit;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLHelpProvider;
import org.jkiss.dbeaver.model.sql.SQLState;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectFilter;
import org.jkiss.dbeaver.model.struct.DBSStructureAssistant;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.IOUtils;

import java.io.File;
import java.net.MalformedURLException;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * GenericDataSource
 */
public class MySQLDataSource extends JDBCDataSource implements DBPObjectStatisticsCollector {
    private static final Log log = Log.getLog(MySQLDataSource.class);

    private final JDBCBasicDataTypeCache<MySQLDataSource, JDBCDataType> dataTypeCache;
    private List<MySQLEngine> engines;
    private final CatalogCache catalogCache = new CatalogCache();
    private List<MySQLPrivilege> privileges;
    private List<MySQLUser> users;
    private List<MySQLCharset> charsets;
    private Map<String, MySQLCollation> collations;
    private String defaultCharset, defaultCollation;
    private int lowerCaseTableNames = 1;
    private SQLHelpProvider helpProvider;
    private volatile boolean hasStatistics;
    private boolean containsCheckConstraintTable;

    public MySQLDataSource(DBRProgressMonitor monitor, DBPDataSourceContainer container)
        throws DBException {
        super(monitor, container, new MySQLDialect());
        dataTypeCache = new JDBCBasicDataTypeCache<>(this);
    }

    @Override
    public Object getDataSourceFeature(String featureId) {
        switch (featureId) {
            case DBConstants.FEATURE_MAX_STRING_LENGTH:
                if (isServerVersionAtLeast(5, 0)) {
                    return 65535;
                } else {
                    return 255;
                }
        }
        return super.getDataSourceFeature(featureId);
    }

    int getLowerCaseTableNames() {
        return lowerCaseTableNames;
    }

    @Override
    protected Map<String, String> getInternalConnectionProperties(DBRProgressMonitor monitor, DBPDriver driver, JDBCExecutionContext context, String purpose, DBPConnectionConfiguration connectionInfo)
        throws DBCException {
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
        final DBACertificateStorage securityManager = getContainer().getPlatform().getCertificateStorage();

        props.put("useSSL", "true");
        if (isMariaDB()) {
            props.put("trustServerCertificate", String.valueOf(!sslConfig.getBooleanProperty(MySQLConstants.PROP_VERIFY_SERVER_SERT)));
        } else {
            props.put("verifyServerCertificate", sslConfig.getStringProperty(MySQLConstants.PROP_VERIFY_SERVER_SERT));
            props.put("requireSSL", sslConfig.getStringProperty(MySQLConstants.PROP_REQUIRE_SSL));
        }

        final String caCertProp = sslConfig.getStringProperty(MySQLConstants.PROP_SSL_CA_CERT);
        final String clientCertProp = sslConfig.getStringProperty(MySQLConstants.PROP_SSL_CLIENT_CERT);
        final String clientCertKeyProp = sslConfig.getStringProperty(MySQLConstants.PROP_SSL_CLIENT_KEY);

        {
            // Trust keystore
            if (!CommonUtils.isEmpty(caCertProp) || !CommonUtils.isEmpty(clientCertProp)) {
                byte[] caCertData = CommonUtils.isEmpty(caCertProp) ? null : IOUtils.readFileToBuffer(new File(caCertProp));
                byte[] clientCertData = CommonUtils.isEmpty(clientCertProp) ? null : IOUtils.readFileToBuffer(new File(clientCertProp));
                byte[] keyData = CommonUtils.isEmpty(clientCertKeyProp) ? null : IOUtils.readFileToBuffer(new File(clientCertKeyProp));
                securityManager.addCertificate(getContainer(), "ssl", caCertData, clientCertData, keyData);
            } else {
                securityManager.deleteCertificate(getContainer(), "ssl");
            }
            final String ksPath = makeKeyStorePath(securityManager.getKeyStorePath(getContainer(), "ssl"));
            if (isMariaDB()) {
                props.put("trustStore", ksPath);
            } else {
                props.put("clientCertificateKeyStoreUrl", ksPath);
                props.put("trustCertificateKeyStoreUrl", ksPath);
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

    private String makeKeyStorePath(File keyStorePath) throws MalformedURLException {
        if (isMariaDB()) {
            return keyStorePath.getAbsolutePath();
        } else {
            return keyStorePath.toURI().toURL().toString();
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
        if (isServerVersionAtLeast(5, 7) && dataTypeCache.getCachedObject(MySQLConstants.TYPE_JSON) == null) {
            dataTypeCache.cacheObject(new JDBCDataType<>(this, java.sql.Types.OTHER, MySQLConstants.TYPE_JSON, MySQLConstants.TYPE_JSON, false, true, 0, 0, 0));
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
    public Collection<? extends MySQLCatalog> getChildren(@NotNull DBRProgressMonitor monitor)
        throws DBException {
        return getCatalogs();
    }

    @Override
    public MySQLCatalog getChild(@NotNull DBRProgressMonitor monitor, @NotNull String childName)
        throws DBException {
        return getCatalog(childName);
    }

    @NotNull
    @Override
    public Class<? extends MySQLCatalog> getPrimaryChildType(@NotNull DBRProgressMonitor monitor)
        throws DBException {
        return MySQLCatalog.class;
    }

    @Override
    public void cacheStructure(@NotNull DBRProgressMonitor monitor, int scope)
        throws DBException {

    }

    @Override
    protected Connection openConnection(@NotNull DBRProgressMonitor monitor, @Nullable JDBCExecutionContext context, @NotNull String purpose) throws DBCException {
        Connection mysqlConnection = super.openConnection(monitor, context, purpose);

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
            throw new DBException(ex, this);
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
                        MySQLPrivilege user = new MySQLPrivilege(this, dbResult);
                        privileges.add(user);
                    }
                    return privileges;
                }
            }
        } catch (SQLException ex) {
            throw new DBException(ex, this);
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
            throw new DBException(ex, this);
        }
    }

    @Override
    public DBCQueryTransformer createQueryTransformer(@NotNull DBCQueryTransformType type) {
        if (type == DBCQueryTransformType.RESULT_SET_LIMIT) {
            return new QueryTransformerLimit();
        } else if (type == DBCQueryTransformType.FETCH_ALL_TABLE) {
            return new QueryTransformerFetchAll();
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

    @NotNull
    @Override
    public MySQLDataSource getDataSource() {
        return this;
    }

    @Override
    public Collection<? extends DBSDataType> getLocalDataTypes() {
        return dataTypeCache.getCachedObjects();
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

    static class CatalogCache extends JDBCObjectCache<MySQLDataSource, MySQLCatalog> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull MySQLDataSource owner) throws SQLException {
            StringBuilder catalogQuery = new StringBuilder("show databases");
            DBSObjectFilter catalogFilters = owner.getContainer().getObjectFilter(MySQLCatalog.class, null, false);
            if (catalogFilters != null) {
                JDBCUtils.appendFilterClause(catalogQuery, catalogFilters, MySQLConstants.COL_DATABASE_NAME, true);
            }
            JDBCPreparedStatement dbStat = session.prepareStatement(catalogQuery.toString());
            if (catalogFilters != null) {
                JDBCUtils.setFilterParameters(dbStat, 1, catalogFilters);
            }
            return dbStat;
        }

        @Override
        protected MySQLCatalog fetchObject(@NotNull JDBCSession session, @NotNull MySQLDataSource owner, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new MySQLCatalog(owner, resultSet);
        }

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

}
