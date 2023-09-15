package org.jkiss.dbeaver.ext.yashandb.model;

import org.eclipse.core.runtime.IAdaptable;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.ext.yashandb.model.session.YashanDBServerSessionManager;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.access.DBAUserPasswordManager;
import org.jkiss.dbeaver.model.admin.sessions.DBAServerSessionManager;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.exec.plan.DBCQueryPlanner;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCRemoteInstance;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructCache;
import org.jkiss.dbeaver.model.impl.jdbc.exec.JDBCConnectionImpl;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.ForTest;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLConstants;
import org.jkiss.dbeaver.model.sql.SQLState;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSStructureAssistant;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

public class YashanDBDataSource extends JDBCDataSource implements DBPObjectStatisticsCollector, IAdaptable {

    private static final Log log = Log.getLog(YashanDBDataSource.class);

    private YashanDBSchema publicSchema;
    final public SchemaCache schemaCache = new SchemaCache();
    final DataTypeCache dataTypeCache = new DataTypeCache();
    final TablespaceCache tablespaceCache = new TablespaceCache();
    final UserCache userCache = new UserCache();
    final RoleCache roleCache = new RoleCache();
    final public ProfileCache profileCache = new ProfileCache();

    final public DBLinkCache dbLinkCache = new DBLinkCache();

    final public PublicSynonymCache publicSynonymCache=new PublicSynonymCache();

    private boolean isAdmin;
    private boolean isAdminVisible;
    private boolean useRuleHint;
    private boolean resolveGeometryAsStruct = true;
    private boolean hasStatistics;

    private final Map<String, Boolean> availableViews = new HashMap<>();


    @Override
    public JDBCExecutionContext createExecutionContext(JDBCRemoteInstance instance, String type) {
        return new YashanDBExecutionContext(instance, type);
    }

    protected void initializeContextState(@NotNull DBRProgressMonitor monitor, @NotNull JDBCExecutionContext context, JDBCExecutionContext initFrom) throws DBException {
        if (initFrom != null) {
            ((YashanDBExecutionContext) context).setCurrentSchema(monitor, ((YashanDBExecutionContext) initFrom).getDefaultSchema());
        } else {
            ((YashanDBExecutionContext) context).refreshDefaults(monitor, true);
        }
    }

    public YashanDBDataSource(DBRProgressMonitor monitor, DBPDataSourceContainer container) throws DBException {
        super(monitor, container, new YashanDBSQLDialect());
        log.debug(">>>Initialize {YashanDBDataSource}....");
    }

    // Constructor for tests
    @ForTest
    public YashanDBDataSource(DBPDataSourceContainer container) {
        super(container, new YashanDBSQLDialect());
//        this.outputReader = new OracleOutputReader();

//        YashanDBCo configurator = GeneralUtils.adapt(this, OracleConfigurator.class);
//        if (configurator != null) {
//            resolveGeometryAsStruct = configurator.resolveGeometryAsStruct();
//        }
        this.hasStatistics = false;

        YashanDBSchema defSchema = new YashanDBSchema(this, -1, "TEST_SCHEMA");
        schemaCache.setCache(Collections.singletonList(defSchema));
    }


    @Override
    public void initialize(@NotNull DBRProgressMonitor monitor) throws DBException {
        super.initialize(monitor);

        DBPConnectionConfiguration connectionInfo = getContainer().getConnectionConfiguration();

        {
            String useRuleHintProp = connectionInfo.getProviderProperty(YashanDBConstants.PROP_USE_RULE_HINT);
            if (useRuleHintProp != null) {
                useRuleHint = CommonUtils.getBoolean(useRuleHintProp, false);
            }
        }
        this.publicSchema = new YashanDBSchema(this, 1, "PUBLIC");

        {
            try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load data source meta info")) {
                // Check DBA role
                this.isAdmin = "YES".equals(
                        JDBCUtils.queryString(
                                session,
                                "SELECT 'YES' FROM dba_role_privs WHERE GRANTED_ROLE='DBA'"));
                this.isAdminVisible = isAdmin;
                if (!isAdminVisible) {
                    String showAdmin = connectionInfo.getProviderProperty(YashanDBConstants.PROP_ALWAYS_SHOW_DBA);
                    if (showAdmin != null) {
                        isAdminVisible = CommonUtils.getBoolean(showAdmin, false);
                    }
                }
            } catch (SQLException e) {
                log.warn(e);
            }
        }

        // when datatsource is loaded,dataType cache will be loaded. This is crucial step in showing data type name in UI box and type folder.
        dataTypeCache.setCaseSensitive(false);
        {
            List<YashanDBDataType> dtList = new ArrayList<>();
            for (Map.Entry<String, YashanDBDataType.TypeDesc> predefinedType : YashanDBDataType.PREDEFINED_TYPES.entrySet()) {
                YashanDBDataType dataType = new YashanDBDataType(this, predefinedType.getKey(), true);
                dtList.add(dataType);
            }
            this.dataTypeCache.setCache(dtList);
        }

    }

    @Override
    public DBSObject refreshObject(DBRProgressMonitor monitor) throws DBException {
        super.refreshObject(monitor);

        this.dataTypeCache.clearCache();
        this.schemaCache.clearCache();
        publicSchema.refreshObject(monitor);
        this.tablespaceCache.clearCache();
        this.userCache.clearCache();
        this.profileCache.clearCache();
        this.dbLinkCache.clearCache();
        this.publicSynonymCache.clearCache();
        this.initialize(monitor);

        return this;
    }

    public boolean isViewAvailable(@NotNull DBRProgressMonitor monitor, @Nullable String schemaName, @NotNull String viewName) {
        viewName = viewName.toUpperCase();
        Boolean available;
        synchronized (availableViews) {
            available = availableViews.get(viewName);
        }
        if (available == null) {
            try {
                try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Check view existence")) {
                    String viewNameQuoted = DBUtils.getQuotedIdentifier(this, viewName);
                    try (final JDBCPreparedStatement dbStat = session.prepareStatement(
                            "SELECT 1 FROM " +
                                    (schemaName == null ? viewNameQuoted : DBUtils.getQuotedIdentifier(this, schemaName) + "." + viewNameQuoted) +
                                    " WHERE 1<>1")) {
                        dbStat.setFetchSize(1);
                        dbStat.execute();
                        available = true;
                    }
                }
            } catch (Exception e) {
                available = false;
            }
            synchronized (availableViews) {
                availableViews.put(viewName, available);
            }
        }
        return available;
    }

    public boolean isAdminVisible() {
        return isAdmin || isAdminVisible;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public boolean isUseRuleHint() {
        return useRuleHint;
    }


    @NotNull
    @Override
    public DBPDataKind resolveDataKind(@NotNull String typeName, int valueType) {
        if ((typeName.equals(YashanDBConstants.TYPE_NAME_XML) || typeName.equals(YashanDBConstants.TYPE_FQ_XML))) {
            return DBPDataKind.CONTENT;
        }
        if ((typeName.equals(YashanDBConstants.TYPE_NAME_GEOMETRY) || typeName.equals(YashanDBConstants.TYPE_FQ_GEOMETRY))) {
            return resolveGeometryAsStruct ? DBPDataKind.STRUCT : DBPDataKind.OBJECT;
        }
        DBPDataKind dataKind = YashanDBDataType.getDataKind(typeName);
        if (dataKind != null) {
            return dataKind;
        }
        return super.resolveDataKind(typeName, valueType);
    }

    @NotNull
    public YashanDBDataType resolveDataType(@NotNull final DBRProgressMonitor monitor, @NotNull final String typeFullName) throws DBException {
        final int divPos = typeFullName.indexOf(SQLConstants.STRUCT_SEPARATOR);
        if (divPos == -1) {
            return this.getLocalDataType(typeFullName);
        }
        final String schemaName = typeFullName.substring(0, divPos);
        final String typeName = typeFullName.substring(divPos + 1);
        final YashanDBSchema schema = this.getSchema(monitor, schemaName);
        if (schema == null) {
            return null;
        }
        return schema.getDataType(monitor, typeName);
    }

    @Override
    public Collection<? extends DBSDataType> getLocalDataTypes() {
        return dataTypeCache.getCachedObjects();
    }

    @Override
    public YashanDBDataType getLocalDataType(String typeName) {
        return dataTypeCache.getCachedObject(typeName);
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
        try (final JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load tablespace '" + getName() + "' statistics")) {
            // Tablespace stats
            try (JDBCStatement dbStat = session.createStatement()) {
                try (JDBCResultSet dbResult = dbStat.executeQuery(
                        "SELECT\n" +
                                "\tTS.TABLESPACE_NAME, F.AVAILABLE_SPACE, S.USED_SPACE\n" +
                                "FROM\n" +
                                "\tSYS.DBA_TABLESPACES TS\n" +
                                "\tleft join (SELECT TABLESPACE_NAME, SUM(BYTES) AVAILABLE_SPACE FROM DBA_DATA_FILES GROUP BY TABLESPACE_NAME) F " +
                                "\ton F.TABLESPACE_NAME = TS.TABLESPACE_NAME\n" +
                                "\tleft join (SELECT TABLESPACE_NAME, SUM(BYTES) USED_SPACE FROM DBA_SEGMENTS GROUP BY TABLESPACE_NAME) S\n" +
                                "\ton S.TABLESPACE_NAME = TS.TABLESPACE_NAME")) {
                    while (dbResult.next()) {
                        String tsName = dbResult.getString(1);
                        YashanDBTablespace tablespace = tablespaceCache.getObject(monitor, getDataSource(), tsName);
                        if (tablespace != null) {
                            tablespace.fetchSizes(dbResult);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            throw new DBException("Can't read tablespace statistics", e, getDataSource());
        } finally {
            hasStatistics = true;
        }
    }

    @Override
    public YashanDBDataSource getDataSource() {
        return this;
    }

    @Override
    public Collection<YashanDBSchema> getChildren(@NotNull DBRProgressMonitor monitor)
            throws DBException {
        return getSchemas(monitor);
    }

    @Override
    public YashanDBSchema getChild(@NotNull DBRProgressMonitor monitor, @NotNull String childName)
            throws DBException {
        return getSchema(monitor, childName);
    }

    @Override
    public Class<? extends DBSObject> getPrimaryChildType(DBRProgressMonitor monitor) throws DBException {
        return YashanDBSchema.class;
    }

    @Override
    public void cacheStructure(DBRProgressMonitor monitor, int scope) throws DBException {

    }

    @Association
    public Collection<YashanDBSchema> getSchemas(DBRProgressMonitor monitor) throws DBException {
        return schemaCache.getAllObjects(monitor, this);
    }

    public YashanDBSchema getSchema(DBRProgressMonitor monitor, String name) throws DBException {
        if (publicSchema != null && publicSchema.getName().equals(name)) {
            return publicSchema;
        }
        // Schema cache may be null during DataSource initialization
        return schemaCache == null ? null : schemaCache.getObject(monitor, this, name);
    }

    @Association
    public Collection<YashanDBTablespace> getTablespaces(DBRProgressMonitor monitor) throws DBException {
        return tablespaceCache.getAllObjects(monitor, this);
    }

    @Association
    public Collection<YashanDBUser> getUsers(DBRProgressMonitor monitor) throws DBException {
        return userCache.getAllObjects(monitor, this);
    }

    @Association
    public YashanDBUser getUser(DBRProgressMonitor monitor, String name) throws DBException {
        return userCache.getObject(monitor, this, name);
    }

    @Association
    public Collection<YashanDBRole> getRoles(DBRProgressMonitor monitor) throws DBException {
        return roleCache.getAllObjects(monitor, this);
    }

    @Association
    public Collection<YashanDBUserProfile> getProfiles(DBRProgressMonitor monitor) throws DBException {
        return profileCache.getAllObjects(monitor, this);
    }

    public YashanDBGrantee getGrantee(DBRProgressMonitor monitor, String name) throws DBException {
        YashanDBUser user = userCache.getObject(monitor, this, name);
        if (user != null) {
            return user;
        }
        return roleCache.getObject(monitor, this, name);
    }

    @Association
    public Collection<YashanDBDBLink> getPublicDatabaseLinks(DBRProgressMonitor monitor) throws DBException {
        return dbLinkCache.getAllObjects(monitor,this);
    }

    ///////////////////////////
    public boolean isAtLeastV10() {
        return getInfo().getDatabaseVersion().getMajor() >= 10;
    }

    @Association
    public Collection<YashanDBPublicSynonym> getPublicSynonyms(DBRProgressMonitor monitor) throws DBException {
        return publicSynonymCache.getAllObjects(monitor,this);
    }

    @Association
    public Collection<YashanDBRecycledObject> getUserRecycledObjects(DBRProgressMonitor monitor) throws DBException {
        return publicSchema.getRecycledObjects(monitor);
    }



    @Nullable
    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter == DBSStructureAssistant.class) {
            return adapter.cast(new YashanDBStructureAssistant(this));
        }
        //else if (adapter == DBCServerOutputReader.class) {
        //    return adapter.cast(outputReader);
        //} else if (adapter == DBAServerSessionManager.class) {
        //    return adapter.cast(new YashanDBServerSessionManager(this));
        //} else if (adapter == DBCQueryPlanner.class) {
        //    return adapter.cast(new YashanDBQueryPlanner(this));
        //} else if(adapter == DBAUserPasswordManager.class) {
        //    return adapter.cast(new YashanDBChangeUserPasswordManager(this));
        //}

        if (adapter == DBAServerSessionManager.class) {
            return adapter.cast(new YashanDBServerSessionManager(this));
        }

        return super.getAdapter(adapter);
    }


    static class SchemaCache extends JDBCObjectCache<YashanDBDataSource, YashanDBSchema> {
        SchemaCache() {
            setListOrderComparator(DBUtils.<YashanDBSchema>nameComparator());
        }

        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull YashanDBDataSource owner) throws SQLException {
            StringBuilder schemasQuery = new StringBuilder();
            DBPConnectionConfiguration configuration = owner.getContainer().getConnectionConfiguration();

            schemasQuery.append("SELECT U.* FROM ALL_USERS U\n" +
                    "WHERE (U.USERNAME IS NOT NULL)");

            JDBCPreparedStatement dbStat = session.prepareStatement(schemasQuery.toString());

            return dbStat;
        }

        @Override
        protected YashanDBSchema fetchObject(@NotNull JDBCSession session, @NotNull YashanDBDataSource owner,
                                             @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new YashanDBSchema(owner, resultSet);
        }

        @Override
        protected void invalidateObjects(DBRProgressMonitor monitor, YashanDBDataSource owner, Iterator<YashanDBSchema> objectIter) {
            setListOrderComparator(DBUtils.<YashanDBSchema>nameComparator());
        }
    }

    static class DataTypeCache extends JDBCObjectCache<YashanDBDataSource, YashanDBDataType> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull YashanDBDataSource owner) throws SQLException {
            return session.prepareStatement(
                    "SELECT " + YashanDBUtils.getSysCatalogHint(owner.getDataSource()) + " * FROM " +
                            YashanDBUtils.getAdminAllViewPrefix(session.getProgressMonitor(), owner, "TYPES") + " WHERE OWNER IS NULL ORDER BY TYPE_NAME");
        }

        @Override
        protected YashanDBDataType fetchObject(@NotNull JDBCSession session, @NotNull YashanDBDataSource owner, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new YashanDBDataType(owner, resultSet);
        }
    }

    static class TablespaceCache extends JDBCObjectCache<YashanDBDataSource, YashanDBTablespace> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull YashanDBDataSource owner) throws SQLException {
            return session.prepareStatement(
                    "SELECT * FROM " + YashanDBUtils.getSysUserViewName(session.getProgressMonitor(), owner, "TABLESPACES") + " ORDER BY TABLESPACE_NAME");
        }

        @Override
        protected YashanDBTablespace fetchObject(@NotNull JDBCSession session, @NotNull YashanDBDataSource owner, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new YashanDBTablespace(owner, resultSet);
        }
    }

    static class UserCache extends JDBCObjectCache<YashanDBDataSource, YashanDBUser> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull YashanDBDataSource owner) throws SQLException {
            return session.prepareStatement(
                    "SELECT * FROM ALL_USERS");

        }

        @Override
        protected YashanDBUser fetchObject(@NotNull JDBCSession session, @NotNull YashanDBDataSource owner, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new YashanDBUser(owner, resultSet);
        }
    }

    static class RoleCache extends JDBCObjectCache<YashanDBDataSource, YashanDBRole> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull YashanDBDataSource owner) throws SQLException {
            return session.prepareStatement(
                    "SELECT * FROM DBA_ROLES ORDER BY ROLE");
        }

        @Override
        protected YashanDBRole fetchObject(@NotNull JDBCSession session, @NotNull YashanDBDataSource owner, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new YashanDBRole(owner, resultSet);
        }
    }

    static class ProfileCache extends JDBCStructCache<YashanDBDataSource, YashanDBUserProfile, YashanDBUserProfile.ProfileResource> {
        protected ProfileCache() {
            super("PROFILE");
        }

        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull YashanDBDataSource owner) throws SQLException {
            return session.prepareStatement(
//                    "SELECT DISTINCT PROFILE FROM DBA_PROFILES ORDER BY PROFILE");
                    "SELECT PROFILE ,GROUP_CONCAT(RESOURCE_NAME) PARMS,GROUP_CONCAT(\"LIMIT\") LIMITS  FROM DBA_PROFILES dp  GROUP BY PROFILE  ORDER BY PROFILE ");
        }

        @Override
        protected YashanDBUserProfile fetchObject(@NotNull JDBCSession session, @NotNull YashanDBDataSource owner, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new YashanDBUserProfile(owner, resultSet);
        }

        @Override
        protected JDBCStatement prepareChildrenStatement(@NotNull JDBCSession session, @NotNull YashanDBDataSource dataSource, @Nullable YashanDBUserProfile forObject) throws SQLException {
            final JDBCPreparedStatement dbStat = session.prepareStatement(
                    "SELECT RESOURCE_NAME,RESOURCE_TYPE,LIMIT FROM DBA_PROFILES " +
                            (forObject == null ? "" : "WHERE PROFILE=? ") +
                            "ORDER BY RESOURCE_NAME");
            if (forObject != null) {
                dbStat.setString(1, forObject.getName());
            }
            return dbStat;
        }

        @Override
        protected YashanDBUserProfile.ProfileResource fetchChild(@NotNull JDBCSession session, @NotNull YashanDBDataSource dataSource, @NotNull YashanDBUserProfile parent, @NotNull JDBCResultSet dbResult) throws SQLException, DBException {
            return new YashanDBUserProfile.ProfileResource(parent, dbResult);
        }
    }

    static class DBLinkCache extends JDBCObjectCache<YashanDBDataSource, YashanDBDBLink> {

        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull YashanDBDataSource owner)
                throws SQLException {
            JDBCPreparedStatement dbStat = session.prepareStatement(
                    "SELECT * FROM " + YashanDBUtils.getAdminAllViewPrefix(session.getProgressMonitor(), owner.getDataSource(), "DB_LINKS") + //" WHERE OWNER=? " +
                            " ORDER BY DB_LINK");
//            dbStat.setString(1, owner.getName());
            return dbStat;
        }

        @Override
        protected YashanDBDBLink fetchObject(@NotNull JDBCSession session, @NotNull YashanDBDataSource owner, @NotNull JDBCResultSet dbResult)
                throws SQLException, DBException {
            return new YashanDBDBLink( owner, dbResult);
        }

    }

    static class PublicSynonymCache extends JDBCObjectCache<YashanDBDataSource, YashanDBPublicSynonym> {

        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull YashanDBDataSource owner)
                throws SQLException {
            JDBCPreparedStatement dbStat = session.prepareStatement(
                    "SELECT OWNER, SYNONYM_NAME, MAX(TABLE_OWNER) as TABLE_OWNER, MAX(TABLE_NAME) as TABLE_NAME, MAX(DB_LINK) as DB_LINK, MAX(OBJECT_TYPE) as OBJECT_TYPE FROM (\n" +
                            "SELECT S.*, NULL OBJECT_TYPE FROM ALL_SYNONYMS S WHERE S.OWNER = 'PUBLIC'\n" +
                            "UNION ALL\n" +
                            "SELECT S.*,O.OBJECT_TYPE FROM ALL_SYNONYMS S, ALL_OBJECTS O\n" +
                            "WHERE S.OWNER = 'PUBLIC'\n" +
                            "AND O.OWNER=S.TABLE_OWNER AND O.OBJECT_NAME=S.TABLE_NAME AND O.SUBOBJECT_NAME IS NULL\n" +
                            ")\n" +
                            "GROUP BY OWNER, SYNONYM_NAME\n" +
                            "ORDER BY SYNONYM_NAME");
            return dbStat;
        }

        @Override
        protected YashanDBPublicSynonym fetchObject(@NotNull JDBCSession session, @NotNull YashanDBDataSource owner, @NotNull JDBCResultSet dbResult)
                throws SQLException, DBException {
            return new YashanDBPublicSynonym( owner, dbResult);
        }
    }

    @Override
    public ErrorType discoverErrorType(Throwable error) {
        //Caused by: com.yashandb.jdbc.exception.YasException: protocol error
        if ("58030".equalsIgnoreCase(SQLState.getStateFromException(error)))
            return ErrorType.CONNECTION_LOST;
        return super.discoverErrorType(error);
    }




}
