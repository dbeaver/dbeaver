/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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

import org.eclipse.core.runtime.IAdaptable;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.mysql.MySQLConstants;
import org.jkiss.dbeaver.ext.mysql.MySQLDataSourceProvider;
import org.jkiss.dbeaver.ext.mysql.MySQLUtils;
import org.jkiss.dbeaver.ext.mysql.model.plan.MySQLPlanAnalyser;
import org.jkiss.dbeaver.ext.mysql.model.session.MySQLSessionManager;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPDataSourceInfo;
import org.jkiss.dbeaver.model.DBPErrorAssistant;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.admin.sessions.DBAServerSessionManager;
import org.jkiss.dbeaver.model.app.DBACertificateStorage;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.exec.jdbc.*;
import org.jkiss.dbeaver.model.exec.plan.DBCPlan;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanStyle;
import org.jkiss.dbeaver.model.exec.plan.DBCQueryPlanner;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCBasicDataTypeCache;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCDataType;
import org.jkiss.dbeaver.model.impl.sql.QueryTransformerLimit;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLHelpProvider;
import org.jkiss.dbeaver.model.sql.SQLState;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.IOUtils;

import java.io.File;
import java.net.MalformedURLException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * GenericDataSource
 */
public class MySQLDataSource extends JDBCDataSource implements DBSObjectSelector, DBCQueryPlanner, IAdaptable
{
    private static final Log log = Log.getLog(MySQLDataSource.class);

    private final JDBCBasicDataTypeCache<MySQLDataSource, JDBCDataType> dataTypeCache;
    private List<MySQLEngine> engines;
    private final CatalogCache catalogCache = new CatalogCache();
    private List<MySQLPrivilege> privileges;
    private List<MySQLUser> users;
    private List<MySQLCharset> charsets;
    private Map<String, MySQLCollation> collations;
    private String activeCatalogName;
    private SQLHelpProvider helpProvider;

    public MySQLDataSource(DBRProgressMonitor monitor, DBPDataSourceContainer container)
        throws DBException
    {
        super(monitor, container, new MySQLDialect());
        dataTypeCache = new JDBCBasicDataTypeCache<>(this);
    }

    @Override
    protected Map<String, String> getInternalConnectionProperties(DBRProgressMonitor monitor, String purpose)
        throws DBCException
    {
        Map<String, String> props = new LinkedHashMap<>(MySQLDataSourceProvider.getConnectionsProps());
        final DBWHandlerConfiguration sslConfig = getContainer().getActualConnectionConfiguration().getDeclaredHandler(MySQLConstants.HANDLER_SSL);
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
/*
        if (CommonUtils.toBoolean(connectionInfo.getProperty(MySQLConstants.PROP_USE_SSL))) {
            url.append("?useSSL=true&requireSSL=true");
        }
*/

        return props;
    }

    @Override
    protected DBPDataSourceInfo createDataSourceInfo(@NotNull JDBCDatabaseMetaData metaData) {
        return new MySQLDataSourceInfo(metaData);
    }

    private void initSSL(DBRProgressMonitor monitor, Map<String, String> props, DBWHandlerConfiguration sslConfig) throws Exception {
        monitor.subTask("Install SSL certificates");
        final DBACertificateStorage securityManager = getContainer().getPlatform().getCertificateStorage();

        props.put("useSSL", "true");
        props.put("verifyServerCertificate", String.valueOf(CommonUtils.toBoolean(sslConfig.getProperties().get(MySQLConstants.PROP_VERIFY_SERVER_SERT))));
        props.put("requireSSL", String.valueOf(CommonUtils.toBoolean(sslConfig.getProperties().get(MySQLConstants.PROP_REQUIRE_SSL))));

        final String caCertProp = sslConfig.getProperties().get(MySQLConstants.PROP_SSL_CA_CERT);
        final String clientCertProp = sslConfig.getProperties().get(MySQLConstants.PROP_SSL_CLIENT_CERT);
        final String clientCertKeyProp = sslConfig.getProperties().get(MySQLConstants.PROP_SSL_CLIENT_KEY);

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
            props.put("clientCertificateKeyStoreUrl", ksPath);
            props.put("trustCertificateKeyStoreUrl", ksPath);
        }
        final String cipherSuites = sslConfig.getProperties().get(MySQLConstants.PROP_SSL_CIPHER_SUITES);
        if (!CommonUtils.isEmpty(cipherSuites)) {
            props.put("enabledSSLCipherSuites;", cipherSuites);
        }
        final boolean retrievePublicKey = CommonUtils.getBoolean(sslConfig.getProperties().get(MySQLConstants.PROP_SSL_PUBLIC_KEY_RETRIEVE), false);
        if (retrievePublicKey) {
            props.put("allowPublicKeyRetrieval", "true");
        }

        if (CommonUtils.getBoolean(sslConfig.getProperties().get(MySQLConstants.PROP_SSL_DEBUG), false)) {
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

    protected void initializeContextState(@NotNull DBRProgressMonitor monitor, @NotNull JDBCExecutionContext context, boolean setActiveObject) throws DBCException {
        if (setActiveObject) {
            MySQLCatalog object = getDefaultObject();
            if (object != null) {
                useDatabase(monitor, context, object);
            }
        }
    }

    public String[] getTableTypes()
    {
        return MySQLConstants.TABLE_TYPES;
    }

    public CatalogCache getCatalogCache()
    {
        return catalogCache;
    }

    public Collection<MySQLCatalog> getCatalogs()
    {
        return catalogCache.getCachedObjects();
    }

    public MySQLCatalog getCatalog(String name)
    {
        return catalogCache.getCachedObject(name);
    }

    @Override
    public void initialize(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
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
                Collections.sort(charsets, DBUtils.<MySQLCharset>nameComparator());


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
            }

            // Read catalogs
            catalogCache.getAllObjects(monitor, this);
            activeCatalogName = MySQLUtils.determineCurrentDatabase(session);
        }
    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
        super.refreshObject(monitor);

        this.engines = null;
        this.catalogCache.clearCache();
        this.users = null;
        this.activeCatalogName = null;

        this.initialize(monitor);

        return this;
    }

    MySQLTable findTable(DBRProgressMonitor monitor, String catalogName, String tableName)
        throws DBException
    {
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
        throws DBException
    {
        return getCatalogs();
    }

    @Override
    public MySQLCatalog getChild(@NotNull DBRProgressMonitor monitor, @NotNull String childName)
        throws DBException
    {
        return getCatalog(childName);
    }

    @Override
    public Class<? extends MySQLCatalog> getChildType(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
        return MySQLCatalog.class;
    }

    @Override
    public void cacheStructure(@NotNull DBRProgressMonitor monitor, int scope)
        throws DBException
    {
        
    }

    @Override
    public boolean supportsDefaultChange()
    {
        return true;
    }

    @Override
    public MySQLCatalog getDefaultObject()
    {
        return CommonUtils.isEmpty(activeCatalogName) ? null : getCatalog(activeCatalogName);
    }

    @Override
    public void setDefaultObject(@NotNull DBRProgressMonitor monitor, @NotNull DBSObject object)
        throws DBException
    {
        final MySQLCatalog oldSelectedEntity = getDefaultObject();
        if (!(object instanceof MySQLCatalog)) {
            throw new DBException("Invalid object type: " + object);
        }
        for (JDBCExecutionContext context : getAllContexts()) {
            useDatabase(monitor, context, (MySQLCatalog) object);
        }
        activeCatalogName = object.getName();

        // Send notifications
        if (oldSelectedEntity != null) {
            DBUtils.fireObjectSelect(oldSelectedEntity, false);
        }
        if (this.activeCatalogName != null) {
            DBUtils.fireObjectSelect(object, true);
        }
    }

    @Override
    public boolean refreshDefaultObject(@NotNull DBCSession session) throws DBException {
        final String newCatalogName = MySQLUtils.determineCurrentDatabase((JDBCSession) session);
        if (!CommonUtils.equalObjects(newCatalogName, activeCatalogName)) {
            final MySQLCatalog newCatalog = getCatalog(newCatalogName);
            if (newCatalog != null) {
                setDefaultObject(session.getProgressMonitor(), newCatalog);
                return true;
            }
        }
        return false;
    }

    private void useDatabase(DBRProgressMonitor monitor, JDBCExecutionContext context, MySQLCatalog catalog) throws DBCException {
        if (catalog == null) {
            log.debug("Null current database");
            return;
        }
        try (JDBCSession session = context.openSession(monitor, DBCExecutionPurpose.UTIL, "Set active catalog")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement("use " + DBUtils.getQuotedIdentifier(catalog))) {
                dbStat.execute();
            }
        } catch (SQLException e) {
            throw new DBCException(e, this);
        }
    }

    @Override
    protected Connection openConnection(@NotNull DBRProgressMonitor monitor, @NotNull String purpose) throws DBCException {
        Connection mysqlConnection = super.openConnection(monitor, purpose);

        {
            // Provide client info
            try {
                mysqlConnection.setClientInfo("ApplicationName", DBUtils.getClientApplicationName(getContainer(), purpose));
            } catch (Throwable e) {
                // just ignore
                log.debug(e);
            }
        }

        return mysqlConnection;
    }

    public List<MySQLUser> getUsers(DBRProgressMonitor monitor)
        throws DBException
    {
        if (users == null) {
            users = loadUsers(monitor);
        }
        return users;
    }

    public MySQLUser getUser(DBRProgressMonitor monitor, String name)
        throws DBException
    {
        return DBUtils.findObject(getUsers(monitor), name);
    }

    private List<MySQLUser> loadUsers(DBRProgressMonitor monitor)
        throws DBException
    {
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

    public List<MySQLEngine> getEngines()
    {
        return engines;
    }

    public MySQLEngine getEngine(String name)
    {
        return DBUtils.findObject(engines, name);
    }

    public MySQLEngine getDefaultEngine()
    {
        for (MySQLEngine engine : engines) {
            if (engine.getSupport() == MySQLEngine.Support.DEFAULT) {
                return engine;
            }
        }
        return null;
    }

    public Collection<MySQLCharset> getCharsets()
    {
        return charsets;
    }

    public MySQLCharset getCharset(String name)
    {
        for (MySQLCharset charset : charsets) {
            if (charset.getName().equals(name)) {
                return charset;
            }
        }
        return null;
    }

    public MySQLCollation getCollation(String name)
    {
        return collations.get(name);
    }

    public List<MySQLPrivilege> getPrivileges(DBRProgressMonitor monitor)
        throws DBException
    {
        if (privileges == null) {
            privileges = loadPrivileges(monitor);
        }
        return privileges;
    }

    public List<MySQLPrivilege> getPrivilegesByKind(DBRProgressMonitor monitor, MySQLPrivilege.Kind kind)
        throws DBException
    {
        List<MySQLPrivilege> privs = new ArrayList<>();
        for (MySQLPrivilege priv : getPrivileges(monitor)) {
            if (priv.getKind() == kind) {
                privs.add(priv);
            }
        }
        return privs;
    }

    public MySQLPrivilege getPrivilege(DBRProgressMonitor monitor, String name)
        throws DBException
    {
        return DBUtils.findObject(getPrivileges(monitor), name);
    }

    private List<MySQLPrivilege> loadPrivileges(DBRProgressMonitor monitor)
        throws DBException
    {
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
        throws DBException
    {
        return loadParameters(monitor, true, false);
    }

    public List<MySQLParameter> getGlobalStatus(DBRProgressMonitor monitor)
        throws DBException
    {
        return loadParameters(monitor, true, true);
    }

    public List<MySQLParameter> getSessionVariables(DBRProgressMonitor monitor)
        throws DBException
    {
        return loadParameters(monitor, false, false);
    }

    public List<MySQLParameter> getGlobalVariables(DBRProgressMonitor monitor)
        throws DBException
    {
        return loadParameters(monitor, false, true);
    }

    public List<MySQLDataSource> getInformation()
    {
        return Collections.singletonList(this);
    }

    private List<MySQLParameter> loadParameters(DBRProgressMonitor monitor, boolean status, boolean global) throws DBException
    {
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

    @NotNull
    @Override
    public DBCPlan planQueryExecution(@NotNull DBCSession session, @NotNull String query) throws DBCException
    {
        MySQLPlanAnalyser plan = new MySQLPlanAnalyser(this, query);
        plan.explain(session);
        return plan;
    }

    @NotNull
    @Override
    public DBCPlanStyle getPlanStyle() {
        return DBCPlanStyle.PLAN;
    }

    @Override
    public <T> T getAdapter(Class<T> adapter)
    {
        if (adapter == DBSStructureAssistant.class) {
            return adapter.cast(new MySQLStructureAssistant(this));
        } else if (adapter == SQLHelpProvider.class) {
            if (helpProvider == null) {
                helpProvider = new MySQLHelpProvider(this);
            }
            return adapter.cast(helpProvider);
        } else if (adapter == DBAServerSessionManager.class) {
            return adapter.cast(new MySQLSessionManager(this));
        }
        return super.getAdapter(adapter);
    }

    @NotNull
    @Override
    public MySQLDataSource getDataSource() {
        return this;
    }

    @Override
    public Collection<? extends DBSDataType> getLocalDataTypes()
    {
        return dataTypeCache.getCachedObjects();
    }

    @Override
    public DBSDataType getLocalDataType(String typeName)
    {
        return dataTypeCache.getCachedObject(typeName);
    }

    static class CatalogCache extends JDBCObjectCache<MySQLDataSource, MySQLCatalog>
    {
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull MySQLDataSource owner) throws SQLException
        {
            StringBuilder catalogQuery = new StringBuilder("SELECT * FROM " + MySQLConstants.META_TABLE_SCHEMATA);
            DBSObjectFilter catalogFilters = owner.getContainer().getObjectFilter(MySQLCatalog.class, null, false);
            if (catalogFilters != null) {
                JDBCUtils.appendFilterClause(catalogQuery, catalogFilters, MySQLConstants.COL_SCHEMA_NAME, true);
            }
            JDBCPreparedStatement dbStat = session.prepareStatement(catalogQuery.toString());
            if (catalogFilters != null) {
                JDBCUtils.setFilterParameters(dbStat, 1, catalogFilters);
            }
            return dbStat;
        }

        @Override
        protected MySQLCatalog fetchObject(@NotNull JDBCSession session, @NotNull MySQLDataSource owner, @NotNull JDBCResultSet resultSet) throws SQLException, DBException
        {
            return new MySQLCatalog(owner, resultSet);
        }

    }

    public boolean isMariaDB() {
        return MySQLConstants.DRIVER_CLASS_MARIA_DB.equals(
            getContainer().getDriver().getDriverClassName());
    }

    @Override
    public ErrorType discoverErrorType(@NotNull Throwable error)
    {
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
                return new ErrorPosition[] { pos };
            }
        }
        return null;
    }

}
