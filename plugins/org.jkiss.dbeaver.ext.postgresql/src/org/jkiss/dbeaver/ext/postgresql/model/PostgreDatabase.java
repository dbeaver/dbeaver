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
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBDatabaseException;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.postgresql.PostgreConstants;
import org.jkiss.dbeaver.ext.postgresql.PostgreUtils;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCRemoteInstance;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectLookupCache;
import org.jkiss.dbeaver.model.meta.*;
import org.jkiss.dbeaver.model.preferences.DBPPropertySource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLState;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.struct.rdb.DBSCatalog;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.LongKeyMap;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * PostgreDatabase
 */
public class PostgreDatabase extends JDBCRemoteInstance
    implements
        DBSCatalog,
        DBPRefreshableObject,
        DBPStatefulObject,
        DBPNamedObject2,
        PostgreObject,
        DBPDataTypeProvider,
        DBSInstanceLazy,
        DBPObjectStatistics,
        DBPObjectWithLazyDescription
{

    private static final Log log = Log.getLog(PostgreDatabase.class);

    private transient PostgreRole initialOwner;
    private transient PostgreTablespace initialTablespace;
    private transient PostgreCharset initialEncoding;

    private long oid;
    private String name;
    private long ownerId;
    private String templateName;
    private long encodingId;
    private String collate;
    private String ctype;
    private boolean isTemplate;
    private boolean allowConnect;
    private int connectionLimit;
    private long tablespaceId;
    private String description;
    private long dbTotalSize = -1;
    private Boolean supportTypColumn;

    private final PostgreDatabaseJDBCObjectCache<? extends PostgreRole> roleCache = createRoleCache();
    final AccessMethodCache accessMethodCache = new AccessMethodCache();
    final ForeignDataWrapperCache foreignDataWrapperCache = new ForeignDataWrapperCache();
    public final ForeignServerCache foreignServerCache = new ForeignServerCache();
    final LanguageCache languageCache = new LanguageCache();
    private final EncodingCache encodingCache = new EncodingCache();
    private final EventTriggersCache eventTriggersCache = new EventTriggersCache();
    public final ExtensionCache extensionCache = new ExtensionCache();
    private final AvailableExtensionCache availableExtensionCache = new AvailableExtensionCache();
    private final CollationCache collationCache = new CollationCache();
    public final TablespaceCache tablespaceCache = new TablespaceCache();
    private final LongKeyMap<PostgreDataType> dataTypeCache = new LongKeyMap<>();
    public final JobCache jobCache = new JobCache();
    public final JobClassCache jobClassCache = new JobClassCache();

    public JDBCObjectLookupCache<PostgreDatabase, PostgreSchema> schemaCache;
    private final EnumValueCache enumValueCache = new EnumValueCache();

    protected PostgreDatabase(DBRProgressMonitor monitor, PostgreDataSource dataSource, ResultSet dbResult)
        throws DBException {
        super(monitor, dataSource, false);
        this.initCaches();
        this.loadInfo(dbResult);
    }

    protected PostgreDatabase(DBRProgressMonitor monitor, PostgreDataSource dataSource, String databaseName)
        throws DBException {
        super(monitor, dataSource, false);
        // We need to set name first
        this.name = databaseName;
        this.initCaches();
        checkInstanceConnection(monitor, false);

        try {
            readDatabaseInfo(monitor);
        } catch (DBCException e) {
            // On some multi-tenant servers pg_database is not public so error may gappen here
            log.debug("Error reading database info", e);
        }
    }

    protected PostgreDatabase(DBRProgressMonitor monitor, PostgreDataSource dataSource, String name, PostgreRole owner, String templateName, PostgreTablespace tablespace, PostgreCharset encoding) throws DBException {
        super(monitor, dataSource, false);
        this.name = name;
        this.initialOwner = owner;
        this.initialTablespace = tablespace;
        this.initialEncoding = encoding;

        this.ownerId = owner == null ? 0 : owner.getObjectId();
        this.templateName = templateName;
        this.tablespaceId = tablespace == null ? 0 : tablespace.getObjectId();
        this.encodingId = encoding == null ? 0 : encoding.getObjectId();
        this.initCaches();
    }

    @ForTest
    PostgreDatabase(PostgreDataSource dataSource, String databaseName) {
        super(dataSource);
        this.name = databaseName;
        this.initCaches();
        PostgreSchema sysSchema = new PostgreSchema(this, PostgreConstants.CATALOG_SCHEMA_NAME);
        sysSchema.getDataTypeCache().loadDefaultTypes(sysSchema);
        schemaCache.cacheObject(sysSchema);
    }

    /**
     * Shared database doesn't need separate JDBC connection.
     * It reuses default database connection and its' object can be accessed with cross-database queries.
     */
    public boolean isSharedDatabase() {
        return false;
    }

    @NotNull
    public PostgreExecutionContext getMetaContext() {
        return (PostgreExecutionContext) super.getDefaultContext(true);
    }

    private void initCaches() {
        schemaCache = getDataSource().getServerType().createSchemaCache(this);
/*
        if (!getDataSource().isServerVersionAtLeast(8, 1)) {
            // Roles not supported
            roleCache.setCache(Collections.emptyList());
        }
*/
    }

    private void initEnumTypesCache(@NotNull DBRProgressMonitor monitor) throws DBException {
        if (((PostgreDataSource) dataSource).isSupportsEnumTable()) {
            enumValueCache.getAllObjects(monitor, this);
        }
    }

    private void readDatabaseInfo(DBRProgressMonitor monitor) throws DBCException {
        try (JDBCSession session = getMetaContext().openSession(monitor, DBCExecutionPurpose.META, "Load database info")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement("SELECT db.oid,db.* FROM pg_catalog.pg_database db WHERE datname=?")) {
                dbStat.setString(1, name);
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    if (dbResult.nextRow()) {
                        loadInfo(dbResult);
                    }
                }
            } catch (SQLException e) {
                throw new DBCException(e, session.getExecutionContext());
            }
        }
    }

    public PostgreRole getInitialOwner() {
        return initialOwner;
    }

    public void setInitialOwner(PostgreRole initialOwner) {
        this.initialOwner = initialOwner;
    }

    public PostgreTablespace getInitialTablespace() {
        return initialTablespace;
    }

    public void setInitialTablespace(PostgreTablespace initialTablespace) {
        this.initialTablespace = initialTablespace;
    }

    public PostgreCharset getInitialEncoding() {
        return initialEncoding;
    }

    public void setInitialEncoding(PostgreCharset initialEncoding) {
        this.initialEncoding = initialEncoding;
    }

    @Override
    public void checkInstanceConnection(@NotNull DBRProgressMonitor monitor) throws DBException {
        if (!isSharedDatabase() && executionContext == null) {
            checkInstanceConnection(monitor, true);
        }
    }

    // We mustn't cache metadata when checkInstanceConnection called during datasource instantiation
    // Because datasource is not fully initialized yet
    void checkInstanceConnection(@NotNull DBRProgressMonitor monitor, boolean cacheMetadata) throws DBException {
        if (!isSharedDatabase() && executionContext == null) {
            initializeMainContext(monitor);
            initializeMetaContext(monitor);
            if (cacheMetadata)
                cacheDataTypes(monitor, true);
        }
    }

    @Override
    public boolean isInstanceConnected() {
        return metaContext != null || executionContext != null || sharedInstance != null;
    }

    protected void loadInfo(ResultSet dbResult) {
        this.oid = JDBCUtils.safeGetLong(dbResult, "oid");
        this.name = JDBCUtils.safeGetString(dbResult, "datname");
        this.ownerId = JDBCUtils.safeGetLong(dbResult, "datdba");
        this.encodingId = JDBCUtils.safeGetLong(dbResult, "encoding");
        if (dataSource.isServerVersionAtLeast(8, 4)) {
            this.collate = JDBCUtils.safeGetString(dbResult, "datcollate");
            this.ctype = JDBCUtils.safeGetString(dbResult, "datctype");
        }
        this.isTemplate = JDBCUtils.safeGetBoolean(dbResult, "datistemplate");
        this.allowConnect = JDBCUtils.safeGetBoolean(dbResult, "datallowconn");
        if (dataSource.isServerVersionAtLeast(8, 1)) {
            this.connectionLimit = JDBCUtils.safeGetInt(dbResult, "datconnlimit");
        }
        this.tablespaceId = JDBCUtils.safeGetLong(dbResult, "dattablespace");
    }

    @NotNull
    @Override
    public PostgreDatabase getDatabase() {
        return this;
    }

    @Override
    public long getObjectId() {
        return this.oid;
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 2)
    public String getName() {
        return name;
    }

    @Override
    public void setName(String newName) {
        this.name = newName;
    }

    @Nullable
    @Override
    public String getDescription() {
        return description;
    }

    @NotNull
    @Override
    protected String getMainContextName() {
        return JDBCExecutionContext.TYPE_MAIN + " <" + getName() + ">";
    }

    @NotNull
    @Override
    protected String getMetadataContextName() {
        return JDBCExecutionContext.TYPE_METADATA + " <" + getName() + ">";
    }

    @NotNull
    @Override
    public PostgreExecutionContext openIsolatedContext(@NotNull DBRProgressMonitor monitor, @NotNull String purpose, @Nullable DBCExecutionContext initFrom) throws DBException {
        PostgreExecutionContext ec = (PostgreExecutionContext) super.openIsolatedContext(monitor, purpose, initFrom);
        ec.setIsolatedContext(true);
        return ec;
    }

    @Override
    @Property(viewable = true, editable = true, updatable = true, length = PropertyLength.MULTILINE, order = 100)
    public String getDescription(DBRProgressMonitor monitor) {
        if (!getDataSource().getServerType().supportsDatabaseDescription()) {
            return null;
        }
        if (description != null) {
            return description;
        }

        // Query row count
        try (JDBCSession session = DBUtils.openUtilSession(monitor, getDataSource(), "Read database description")) {
            description = JDBCUtils.queryString(session, "select description from pg_shdescription "
                    + "join pg_database on objoid = pg_database.oid where datname = ?", getName());
        } catch (Exception e) {
            log.debug("Error reading database description ", e);
        }
        if (description == null) {
            description = "";
        }
        
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public DBSObject getParentObject() {
        return dataSource.getContainer();
    }

    @NotNull
    @Override
    public PostgreDataSource getDataSource() {
        return (PostgreDataSource) dataSource;
    }

    @Override
    public boolean isPersisted() {
        return true;
    }

    public boolean isActiveDatabase() {
        return dataSource.getDefaultInstance() == this;
    }

    ///////////////////////////////////////////////////
    // Properties

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    @Nullable
    @Property(editable = true, updatable = true, order = 3, listProvider = RoleListProvider.class)
    public PostgreRole getDBA(DBRProgressMonitor monitor) throws DBException {
        checkInstanceConnection(monitor);
        return getRoleById(monitor, ownerId);
    }

    public void setDBA(PostgreRole owner) {
        this.ownerId = owner.getObjectId();
    }

    public PostgreDatabaseJDBCObjectCache<? extends PostgreRole> getRoleCache() {
        return roleCache;
    }

    @Nullable
    public PostgreRole getRoleById(DBRProgressMonitor monitor, long roleId) throws DBException {
        if (!getDataSource().getServerType().supportsRoles()) {
            return null;
        }
        checkInstanceConnection(monitor);
        return PostgreUtils.getObjectById(monitor, roleCache, this, roleId);
    }

    @Nullable
    public PostgreRole getRoleByReference(@NotNull DBRProgressMonitor monitor, @NotNull PostgreRoleReference reference) throws DBException {
        if (!getDataSource().getServerType().supportsRoles()) {
            return null;
        }
        checkInstanceConnection(monitor);
        return roleCache.getObject(monitor, this, reference.getRoleName());
    }

    @Property(editable = false, updatable = false, order = 5/*, listProvider = CharsetListProvider.class*/)
    public PostgreCharset getDefaultEncoding(DBRProgressMonitor monitor) throws DBException {
        if (!getDataSource().getServerType().supportsEncodings()) {
            return null;
        }
        checkInstanceConnection(monitor);
        return PostgreUtils.getObjectById(monitor, encodingCache, this, encodingId);
    }

    public void setDefaultEncoding(PostgreCharset charset) throws DBException {
        this.encodingId = charset.getObjectId();
    }

    @Property(order = 10)
    public String getCollate() {
        return collate;
    }

    @Property(order = 11)
    public String getCtype() {
        return ctype;
    }

    @Property(order = 12)
    public boolean isTemplate() {
        return isTemplate;
    }

    @Property(order = 13)
    public boolean isAllowConnect() {
        return allowConnect;
    }

    @Property(order = 14)
    public int getConnectionLimit() {
        return connectionLimit;
    }

    ///////////////////////////////////////////////
    // Infos

    @Association
    public Collection<? extends PostgreRole> getAuthIds(DBRProgressMonitor monitor) throws DBException {
        if (!getDataSource().supportsRoles()) {
            return Collections.emptyList();
        }
        checkInstanceConnection(monitor);
        return roleCache.getAllObjects(monitor, this);
    }

    @Association
    public Collection<PostgreAccessMethod> getAccessMethods(DBRProgressMonitor monitor) throws DBException {
        checkInstanceConnection(monitor);
        return accessMethodCache.getAllObjects(monitor, this);
    }

    @Association
    public Collection<PostgreForeignDataWrapper> getForeignDataWrappers(DBRProgressMonitor monitor) throws DBException {
        checkInstanceConnection(monitor);
        return foreignDataWrapperCache.getAllObjects(monitor, this);
    }

    @Association
    public Collection<PostgreForeignServer> getForeignServers(DBRProgressMonitor monitor) throws DBException {
        checkInstanceConnection(monitor);
        return foreignServerCache.getAllObjects(monitor, this);
    }

    @Association
    public Collection<PostgreLanguage> getLanguages(DBRProgressMonitor monitor) throws DBException {
        checkInstanceConnection(monitor);
        return languageCache.getAllObjects(monitor, this);
    }

    @Association
    public Collection<PostgreSetting> getSettings(DBRProgressMonitor monitor) throws DBException {
        checkInstanceConnection(monitor);
        return getDataSource().getSettingCache().getAllObjects(monitor, getDataSource());
    }

    @Association
    public Collection<PostgreCharset> getEncodings(DBRProgressMonitor monitor) throws DBException {
        if (!getDataSource().getServerType().supportsEncodings()) {
            return null;
        }
        checkInstanceConnection(monitor);
        return encodingCache.getAllObjects(monitor, this);
    }

    @Association
    public Collection<PostgreEventTrigger> getEventTriggers(DBRProgressMonitor monitor) throws DBException {
        checkInstanceConnection(monitor);
        return eventTriggersCache.getAllObjects(monitor, this);
    }

    @Association
    public PostgreEventTrigger getEventTrigger(DBRProgressMonitor monitor, String triggerName) throws DBException {
        return eventTriggersCache.getObject(monitor, this, triggerName);
    }

    public EventTriggersCache getEventTriggersCache() {
        return eventTriggersCache;
    }

    @Association
    public Collection<PostgreExtension> getExtensions(DBRProgressMonitor monitor)
        throws DBException {
        return extensionCache.getAllObjects(monitor, this);
    }

    @Association
    public Collection<PostgreAvailableExtension> getAvailableExtensions(DBRProgressMonitor monitor)
        throws DBException {
        return availableExtensionCache.getAllObjects(monitor, this);
    }
    

    @Association
    public Collection<PostgreCollation> getCollations(DBRProgressMonitor monitor)
        throws DBException {
        return collationCache.getAllObjects(monitor, this);
    }

    @Association
    public PostgreCollation getCollation(DBRProgressMonitor monitor, long id)
        throws DBException {
        for (PostgreCollation collation : collationCache.getAllObjects(monitor, this)) {
            if (collation.getObjectId() == id) {
                return collation;
            }
        }
        log.debug("Collation '" + id + "' not found in schema " + getName());
        return null;
    }
    ///////////////////////////////////////////////
    // Data types



    @NotNull
    @Override
    public DBPDataKind resolveDataKind(@NotNull String typeName, int typeID) {
        return dataSource.resolveDataKind(typeName, typeID);
    }

    @Override
    public DBSDataType resolveDataType(@NotNull DBRProgressMonitor monitor, @NotNull String typeFullName) throws DBException {
        return PostgreUtils.resolveTypeFullName(monitor, this, typeFullName);
    }

    @Override
    public Collection<PostgreDataType> getLocalDataTypes() {
        synchronized (dataTypeCache) {
            if (!CommonUtils.isEmpty(dataTypeCache)) {
                return new ArrayList<>(dataTypeCache.values());
            }
        }
        final PostgreSchema schema = getCatalogSchema();
        if (schema != null) {
            return schema.getDataTypeCache().getCachedObjects();
        }
        return null;
    }

    @Override
    public PostgreDataType getLocalDataType(String typeName) {
        return getDataType(null, typeName);
    }

    @Override
    public DBSDataType getLocalDataType(int typeID) {
        return getDataType(new VoidProgressMonitor(), typeID);
    }

    @Override
    public String getDefaultDataTypeName(@NotNull DBPDataKind dataKind) {
        return PostgreUtils.getDefaultDataTypeName(dataKind);
    }

    /**
     * @return enum values cache. Do not use is if database do not support enams. Check {@code PostgreDatasource#isSupportsEnumTable}
     */
    EnumValueCache getEnumValueCache() {
        return enumValueCache;
    }

    ///////////////////////////////////////////////
    // Tablespaces

    TablespaceCache getTablespaceCache() {
        return tablespaceCache;
    }

    @Association
    public Collection<PostgreTablespace> getTablespaces(DBRProgressMonitor monitor) throws DBException {
        checkInstanceConnection(monitor);
        return tablespaceCache.getAllObjects(monitor, this);
    }

    @Property(editable = true, updatable = true, order = 4, listProvider = TablespaceListProvider.class)
    public PostgreTablespace getDefaultTablespace(DBRProgressMonitor monitor) throws DBException {
        checkInstanceConnection(monitor);
        return PostgreUtils.getObjectById(monitor, tablespaceCache, this, tablespaceId);
    }

    public void setDefaultTablespace(PostgreTablespace tablespace) throws DBException {
        this.tablespaceId = tablespace.getObjectId();
    }

    public PostgreTablespace getTablespace(DBRProgressMonitor monitor, long tablespaceId) throws DBException {
        checkInstanceConnection(monitor);
        for (PostgreTablespace ts : tablespaceCache.getAllObjects(monitor, this)) {
            if (ts.getObjectId() == tablespaceId) {
                return ts;
            }
        }
        return null;
    }

    JobClassCache getJobClassCache() {
        return jobClassCache;
    }

    @Association
    public Collection<PostgreJob> getJobs(@NotNull DBRProgressMonitor monitor) throws DBException {
        checkInstanceConnection(monitor);
        return jobCache.getAllObjects(monitor, this);
    }

    @Nullable
    public PostgreJob getJob(@NotNull DBRProgressMonitor monitor, @NotNull String name) throws DBException {
        checkInstanceConnection(monitor);
        return jobCache.getObject(monitor, this, name);
    }

    @Association
    public Collection<PostgreJobClass> getJobClasses(@NotNull DBRProgressMonitor monitor) throws DBException {
        checkInstanceConnection(monitor);
        return jobClassCache.getAllObjects(monitor, this);
    }

    @Nullable
    public PostgreJobClass getJobClass(@NotNull DBRProgressMonitor monitor, long id) throws DBException {
        for (PostgreJobClass cls : getJobClasses(monitor)) {
            if (cls.getObjectId() == id) {
                return cls;
            }
        }
        return null;
    }

    ///////////////////////////////////////////////
    // Object container

    @Association
    public Collection<PostgreSchema> getSchemas(DBRProgressMonitor monitor) throws DBException {
        if (monitor != null) {
            checkInstanceConnection(monitor);
        }
        // Get all schemas
        return monitor == null ? schemaCache.getCachedObjects() : schemaCache.getAllObjects(monitor, this);
    }

    @Nullable
    public PostgreSchema getCatalogSchema(DBRProgressMonitor monitor) throws DBException {
        return getSchema(monitor, PostgreConstants.CATALOG_SCHEMA_NAME);
    }

    @Nullable
    PostgreSchema getCatalogSchema() {
        return schemaCache.getCachedObject(PostgreConstants.CATALOG_SCHEMA_NAME);
    }

    @Nullable
    PostgreSchema getActiveSchema() {
        return getMetaContext().getDefaultSchema();
    }

    @Nullable
    PostgreSchema getPublicSchema() {
        return schemaCache.getCachedObject(PostgreConstants.PUBLIC_SCHEMA_NAME);
    }

    void cacheDataTypes(DBRProgressMonitor monitor, boolean forceRefresh) throws DBException {
        boolean hasDataTypes;
        synchronized (dataTypeCache) {
            hasDataTypes = !dataTypeCache.isEmpty();
        }
        if (!hasDataTypes || forceRefresh) {
            synchronized (dataTypeCache) {
                dataTypeCache.clear();
                enumValueCache.clearCache();
            }
            // Cache data types

            PostgreDataSource postgreDataSource = getDataSource();
            boolean readAllTypes = postgreDataSource.supportReadingAllDataTypes();

            try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Read data types")) {
                StringBuilder sql = new StringBuilder(256);
                boolean supportsSysTypColumn = supportsSysTypCategoryColumn(session); // Do not read all array and table types, unless the user has decided otherwise
                sql.append("SELECT t.oid,t.*,c.relkind,").append(PostgreDataTypeCache.getBaseTypeNameClause(postgreDataSource)).append(", d.description" +
                        "\nFROM pg_catalog.pg_type t");
                if (!readAllTypes && supportsSysTypColumn) {
                    sql.append("\nLEFT OUTER JOIN pg_catalog.pg_type et ON et.oid=t.typelem "); // If typelem is not 0 then it identifies another row in pg_type
                }
                sql.append("\nLEFT OUTER JOIN pg_catalog.pg_class c ON c.oid=t.typrelid" +
                        "\nLEFT OUTER JOIN pg_catalog.pg_description d ON t.oid=d.objoid" +
                        "\nWHERE t.typname IS NOT NULL");
                if (!readAllTypes) {
                    sql.append("\nAND (c.relkind IS NULL OR c.relkind = 'c')");
                    if (supportsSysTypColumn) {
                        sql.append(" AND (et.typcategory IS NULL OR et.typcategory <> 'C')");
                    }
                }

                List<PostgreDataType> loadedDataTypes = new ArrayList<>();
                try (JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString())) {
                    try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                        Set<PostgreSchema> schemaList = new HashSet<>();
                        while (dbResult.next()) {
                            PostgreDataType dataType = PostgreDataType.readDataType(session, this, dbResult, !readAllTypes);
                            if (dataType != null) {
                                PostgreSchema schema = dataType.getParentObject();
                                schemaList.add(schema);
                                schema.getDataTypeCache().cacheObject(dataType);
                                loadedDataTypes.add(dataType);
                            }
                        }
                        if (!schemaList.isEmpty()) {
                            for (PostgreSchema schema : schemaList) {
                                schema.getDataTypeCache().setFullCache(true);
                            }
                        }
                        PostgreSchema catalogSchema = getCatalogSchema();
                        if (catalogSchema != null) {
                            catalogSchema.getDataTypeCache().mapAliases(catalogSchema);
                        }
                    }
                }
                synchronized (dataTypeCache) {
                    for (PostgreDataType dataType : loadedDataTypes) {
                        dataTypeCache.put(dataType.getObjectId(), dataType);
                    }
                }
            } catch (SQLException e) {
                throw new DBDatabaseException(e, postgreDataSource);
            }
            initEnumTypesCache(monitor);
        }
    }

    // Column "typcategory" appeared only in PG version 8.4 and before we relied on DB version to verify the conditions, but it was not the most universal solution.
    // So make a separate request to the database for checking.
    boolean supportsSysTypCategoryColumn(JDBCSession session) {
        if (supportTypColumn == null) {
            if (!dataSource.isServerVersionAtLeast(10, 0)) {
                if (!dataSource.isServerVersionAtLeast(8, 4)) {
                    supportTypColumn = false;
                } else {
                    try {
                        JDBCUtils.queryString(
                            session,
                            PostgreUtils.getQueryForSystemColumnChecking("pg_type", "typcategory"));
                        supportTypColumn = true;
                    } catch (SQLException e) {
                        log.debug("Error reading system information from the pg_type table: " + e.getMessage());
                        try {
                            if (!session.isClosed() && !session.getAutoCommit()) {
                                session.rollback();
                            }
                        } catch (SQLException ex) {
                            log.warn("Can't rollback transaction", e);
                        }
                        supportTypColumn = false;
                    }
                }
            } else {
                supportTypColumn = true;
            }
        }
        return supportTypColumn;
    }

    public PostgreSchema getSchema(DBRProgressMonitor monitor, String name) throws DBException {
        checkInstanceConnection(monitor);
        return schemaCache.getObject(monitor, this, name);
    }

    public PostgreSchema getSchema(DBRProgressMonitor monitor, long oid) throws DBException {
        checkInstanceConnection(monitor);
        for (PostgreSchema schema : schemaCache.getAllObjects(monitor, this)) {
            if (schema.getObjectId() == oid) {
                return schema;
            }
        }
        return null;
    }

    @Nullable
    public PostgreSchema getSchema(long oid) {
        for (PostgreSchema schema : schemaCache.getCachedObjects()) {
            if (schema.getObjectId() == oid) {
                return schema;
            }
        }
        return null;
    }

    public PostgreSchema createSchemaImpl(@NotNull PostgreDatabase owner, @NotNull String name, @NotNull JDBCResultSet resultSet) throws SQLException {
        return new PostgreSchema(owner, name, resultSet);
    }

    public PostgreSchema createSchemaImpl(@NotNull PostgreDatabase owner, @NotNull String name, @Nullable PostgreRole postgreRole) {
        return new PostgreSchema(owner, name, postgreRole);
    }

    PostgreTableBase findTable(DBRProgressMonitor monitor, long schemaId, long tableId)
        throws DBException {
        PostgreSchema schema = getSchema(monitor, schemaId);
        if (schema == null) {
            log.error("Catalog " + schemaId + " not found");
            return null;
        }
        return schema.getTable(monitor, tableId);
    }

    @Override
    public Collection<? extends DBSObject> getChildren(@NotNull DBRProgressMonitor monitor) throws DBException {
        return getSchemas(monitor);
    }

    @Override
    public DBSObject getChild(@NotNull DBRProgressMonitor monitor, @NotNull String childName) throws DBException {
        return getSchema(monitor, childName);
    }

    @NotNull
    @Override
    public Class<? extends DBSObject> getPrimaryChildType(@Nullable DBRProgressMonitor monitor) throws DBException {
        return PostgreSchema.class;
    }

    @Override
    public void cacheStructure(@NotNull DBRProgressMonitor monitor, int scope) throws DBException {

    }

    @NotNull
    @Override
    public DBSObjectState getObjectState() {
        if ((!dataSource.isConnectionRefreshing() && this == dataSource.getDefaultInstance()) || this.isSharedDatabase()) {
            return DBSObjectState.NORMAL;
        } else {
            return PostgreConstants.STATE_UNAVAILABLE;
        }
    }

    @Override
    public void refreshObjectState(@NotNull DBRProgressMonitor monitor) throws DBCException {

    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        if (sharedInstance == null && metaContext == null && executionContext == null) {
            // Nothing to refresh
            return this;
        }
        readDatabaseInfo(monitor);

        // Clear all caches
        roleCache.clearCache();
        accessMethodCache.clearCache();
        foreignDataWrapperCache.clearCache();
        foreignServerCache.clearCache();
        languageCache.clearCache();
        encodingCache.clearCache();
        eventTriggersCache.clearCache();
        extensionCache.clearCache();
        availableExtensionCache.clearCache();
        collationCache.clearCache();
        tablespaceCache.clearCache();
        jobCache.clearCache();
        jobClassCache.clearCache();
        schemaCache.clearCache();
        cacheDataTypes(monitor, true);
        enumValueCache.clearCache();

        return this;
    }

    public Collection<? extends PostgreRole> getUsers(DBRProgressMonitor monitor) throws DBException {
        if (!getDataSource().getServerType().supportsRoles()) {
            return Collections.emptyList();
        }
        checkInstanceConnection(monitor);
        return roleCache.getAllObjects(monitor, this);
    }

    ////////////////////////////////////////////////////
    // Default schema and search path

    /////////////////////////////////////////////////
    // Procedures

    public PostgreProcedure getProcedure(DBRProgressMonitor monitor, long schemaId, long procId)
        throws DBException {
        final PostgreSchema schema = getSchema(monitor, schemaId);
        if (schema != null) {
            return PostgreUtils.getObjectById(monitor, schema.getProceduresCache(), schema, procId);
        }
        return null;
    }

    public PostgreProcedure getProcedure(DBRProgressMonitor monitor, long procId)
        throws DBException {
        for (final PostgreSchema schema : getSchemas(monitor)) {
            PostgreProcedure procedure = PostgreUtils.getObjectById(monitor, schema.getProceduresCache(), schema, procId);
            if (procedure != null) {
                return procedure;
            }
        }
        return null;
    }

    public PostgreDataType getDataType(DBRProgressMonitor monitor, long typeId) {
        if (typeId <= 0) {
            return null;
        }

        PostgreDataType dataType;
        synchronized (dataTypeCache) {
            dataType = dataTypeCache.get(typeId);
            if (dataType != null) {
                return dataType;
            }
        }
        for (PostgreSchema schema : schemaCache.getCachedObjects()) {
            dataType = schema.getDataTypeCache().getDataType(typeId);
            if (dataType != null) {
                synchronized (dataTypeCache) {
                    dataTypeCache.put(typeId, dataType);
                }
                return dataType;
            }
        }
        // Type not found. Let's resolve it
        try {
            dataType = PostgreDataTypeCache.resolveDataType(monitor, this, typeId);
            dataType.getParentObject().getDataTypeCache().cacheObject(dataType);
            synchronized (dataTypeCache) {
                dataTypeCache.put(dataType.getObjectId(), dataType);
            }
            return dataType;
        } catch (Exception e) {
            log.debug("Can't resolve data type " + typeId, e);
            return null;
        }
    }

    public PostgreDataType getDataType(@Nullable DBRProgressMonitor monitor, String typeName) {
        if (typeName.endsWith("[]")) {
            // In some cases ResultSetMetadata returns it as []
            typeName = "_" + typeName.substring(0, typeName.length() - 2);
        }
        {
            // First check system catalog
            final PostgreSchema schema = getCatalogSchema();
            if (schema != null) {
                final PostgreDataType dataType = schema.getDataTypeCache().getCachedObject(typeName);
                if (dataType != null) {
                    return dataType;
                }
            }
        }

        // Check schemas in search path
        PostgreExecutionContext metaContext = getMetaContext();
        List<String> searchPath = metaContext == null ? Collections.singletonList(PostgreConstants.CATALOG_SCHEMA_NAME) : metaContext.getSearchPath();
        for (String schemaName : searchPath) {
            final PostgreSchema schema = schemaCache.getCachedObject(schemaName);
            if (schema != null) {
                final PostgreDataType dataType = schema.getDataTypeCache().getCachedObject(typeName);
                if (dataType != null) {
                    return dataType;
                }
            }
        }
        // Check the rest
        for (PostgreSchema schema : schemaCache.getCachedObjects()) {
            if (searchPath.contains(schema.getName())) {
                continue;
            }
            final PostgreDataType dataType = schema.getDataTypeCache().getCachedObject(typeName);
            if (dataType != null) {
                return dataType;
            }
        }

        if (monitor == null || monitor.isForceCacheUsage()) {
            return null;
        }

        // Type not found. Let's resolve it
        try {
            PostgreDataType dataType = PostgreDataTypeCache.resolveDataType(monitor, this, typeName);
            dataType.getParentObject().getDataTypeCache().cacheObject(dataType);
            synchronized (dataTypeCache) {
                dataTypeCache.put(dataType.getObjectId(), dataType);
            }
            return dataType;
        } catch (Exception e) {
            log.debug("Can't resolve data type '" + typeName + "' in database '" + getName() + "'");
            return null;
        }
    }

    /////////////////////////////////////////////////////////////////////////////////////
    // Stats

    @Override
    public boolean hasStatistics() {
        return true;
    }

    @Override
    public long getStatObjectSize() {
        return dbTotalSize;
    }

    public void setDbTotalSize(long dbTotalSize) {
        this.dbTotalSize = dbTotalSize;
    }

    @Nullable
    @Override
    public DBPPropertySource getStatProperties() {
        return null;
    }

    @Override
    public String toString() {
        return name;
    }

    /////////////////////////////////////////////////////////////////////////////////////
    // Caches

    @NotNull
    protected PostgreDatabaseJDBCObjectCache<? extends PostgreRole> createRoleCache() {
        return new RoleCache();
    }

    protected static abstract class PostgreDatabaseJDBCObjectCache<OBJECT extends DBSObject> extends JDBCObjectCache<PostgreDatabase, OBJECT> {
        boolean handlePermissionDeniedError(Exception e) {
            if (PostgreConstants.EC_PERMISSION_DENIED.equals(SQLState.getStateFromException(e))) {
                log.warn(e);
                setCache(Collections.emptyList());
                return true;
            }
            return false;
        }
    }

    static class RoleCache extends PostgreDatabaseJDBCObjectCache<PostgreRole> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull PostgreDatabase owner)
            throws SQLException {
            boolean supportsCommentsOnRole = owner.getDataSource().getServerType().supportsCommentsOnRole();
            String sql = "SELECT a.oid,a.*" + (supportsCommentsOnRole ? ",pd.description" : "") +
                " FROM pg_catalog.pg_roles a " +
                (supportsCommentsOnRole ? "\nleft join pg_catalog.pg_shdescription pd on a.oid = pd.objoid" : "") +
                "\nORDER BY a.rolname";
            return session.prepareStatement(sql);
        }

        @Override
        protected PostgreRole fetchObject(@NotNull JDBCSession session, @NotNull PostgreDatabase owner, @NotNull JDBCResultSet dbResult)
            throws SQLException, DBException {
            return new PostgreRole(owner, dbResult);
        }

        @Override
        protected boolean handleCacheReadError(Exception error) {
            // #271, #501: in some databases (AWS?) pg_authid is not accessible
            // FIXME: maybe some better workaround?
            return handlePermissionDeniedError(error);
        }
    }

    static class AccessMethodCache extends PostgreDatabaseJDBCObjectCache<PostgreAccessMethod> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull PostgreDatabase owner)
            throws SQLException {
            return session.prepareStatement(
                "SELECT am.oid,am.* FROM pg_catalog.pg_am am " +
                    "\nORDER BY am.oid"
            );
        }

        @Override
        protected PostgreAccessMethod fetchObject(@NotNull JDBCSession session, @NotNull PostgreDatabase owner, @NotNull JDBCResultSet dbResult)
            throws SQLException, DBException {
            return new PostgreAccessMethod(owner, dbResult);
        }
    }

    static class EncodingCache extends PostgreDatabaseJDBCObjectCache<PostgreCharset> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull PostgreDatabase owner)
            throws SQLException {
            return session.prepareStatement(
                "SELECT c.contoencoding as encid,pg_catalog.pg_encoding_to_char(c.contoencoding) as encname\n" +
                    "FROM pg_catalog.pg_conversion c\n" +
                    "GROUP BY c.contoencoding\n" +
                    "ORDER BY 2\n"
            );
        }

        @Override
        protected PostgreCharset fetchObject(@NotNull JDBCSession session, @NotNull PostgreDatabase owner, @NotNull JDBCResultSet dbResult)
            throws SQLException, DBException {
            return new PostgreCharset(owner, dbResult);
        }
    }

    static class CollationCache extends PostgreDatabaseJDBCObjectCache<PostgreCollation> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull PostgreDatabase owner)
            throws SQLException {
            return session.prepareStatement(
                "SELECT c.oid,c.* FROM pg_catalog.pg_collation c " +
                    "\nORDER BY c.oid"
            );
        }

        @Override
        protected PostgreCollation fetchObject(@NotNull JDBCSession session, @NotNull PostgreDatabase owner, @NotNull JDBCResultSet dbResult)
            throws SQLException, DBException
        {
            return new PostgreCollation(session.getProgressMonitor(), owner, dbResult);
        }
    }

    static class LanguageCache extends PostgreDatabaseJDBCObjectCache<PostgreLanguage> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull PostgreDatabase owner)
            throws SQLException {
            return session.prepareStatement(
                "SELECT l.oid,l.* FROM pg_catalog.pg_language l " +
                    "\nORDER BY l.oid"
            );
        }

        @Override
        protected PostgreLanguage fetchObject(@NotNull JDBCSession session, @NotNull PostgreDatabase owner, @NotNull JDBCResultSet dbResult)
            throws SQLException, DBException {
            return new PostgreLanguage(owner, dbResult);
        }
    }

    static class ForeignDataWrapperCache extends PostgreDatabaseJDBCObjectCache<PostgreForeignDataWrapper> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull PostgreDatabase owner)
            throws SQLException {
            return session.prepareStatement(
                "SELECT l.oid,l.*,p.pronamespace as handler_schema_id " +
                    "\nFROM pg_catalog.pg_foreign_data_wrapper l" +
                    "\nLEFT OUTER JOIN pg_catalog.pg_proc p ON p.oid=l.fdwhandler " +
                    "\nORDER BY l.fdwname"
            );
        }

        @Override
        protected PostgreForeignDataWrapper fetchObject(@NotNull JDBCSession session, @NotNull PostgreDatabase owner, @NotNull JDBCResultSet dbResult)
            throws SQLException, DBException {
            return new PostgreForeignDataWrapper(owner, dbResult);
        }
    }

    static class ForeignServerCache extends PostgreDatabaseJDBCObjectCache<PostgreForeignServer> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull PostgreDatabase owner)
            throws SQLException {
            return session.prepareStatement(
                "SELECT l.oid,l.* FROM pg_catalog.pg_foreign_server l" +
                    "\nORDER BY l.srvname"
            );
        }

        @Override
        protected PostgreForeignServer fetchObject(@NotNull JDBCSession session, @NotNull PostgreDatabase owner, @NotNull JDBCResultSet dbResult)
            throws SQLException, DBException {
            return new PostgreForeignServer(owner, dbResult);
        }
    }

    static class TablespaceCache extends PostgreDatabaseJDBCObjectCache<PostgreTablespace> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull PostgreDatabase owner)
            throws SQLException {
            return session.prepareStatement(
                "SELECT t.oid,t.*" +
                    (owner.getDataSource().getServerType().supportsTablespaceLocation() ? ",pg_tablespace_location(t.oid) loc" : "") +
                    "\nFROM pg_catalog.pg_tablespace t " +
                    "\nORDER BY t.oid"
            );
        }

        @Override
        protected PostgreTablespace fetchObject(@NotNull JDBCSession session, @NotNull PostgreDatabase owner, @NotNull JDBCResultSet dbResult)
            throws SQLException, DBException {
            return new PostgreTablespace(owner, dbResult);
        }

        @Override
        protected boolean handleCacheReadError(Exception error) {
            return handlePermissionDeniedError(error);
        }
    }

    static class AvailableExtensionCache extends PostgreDatabaseJDBCObjectCache<PostgreAvailableExtension> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull PostgreDatabase owner)
            throws SQLException {
            return session.prepareStatement(
                  "SELECT name,default_version,installed_version,comment FROM pg_catalog.pg_available_extensions ORDER BY name"
            );
        }

        @Override
        protected PostgreAvailableExtension fetchObject(@NotNull JDBCSession session, @NotNull PostgreDatabase owner, @NotNull JDBCResultSet dbResult)
            throws SQLException, DBException {
            return new PostgreAvailableExtension(owner, dbResult);
        }
    }

    static class EventTriggersCache extends JDBCObjectLookupCache<PostgreDatabase, PostgreEventTrigger> {

        @NotNull
        @Override
        public JDBCStatement prepareLookupStatement(@NotNull JDBCSession session, @NotNull PostgreDatabase database, @Nullable PostgreEventTrigger object, @Nullable String objectName) throws SQLException {
            String statement = "SELECT pet.*, d.description FROM pg_catalog.pg_event_trigger pet\n" +
                "LEFT OUTER JOIN pg_catalog.pg_description d ON pet.\"oid\" = d.objoid" +
                (object != null || CommonUtils.isNotEmpty(objectName) ? " WHERE pet.evtname = ?" : "");
            JDBCPreparedStatement prepareStatement = session.prepareStatement(statement);
            if (object != null || CommonUtils.isNotEmpty(objectName)) {
                prepareStatement.setString(1, object != null ? object.getName() : objectName);
            }
            return prepareStatement;
        }

        @Nullable
        @Override
        protected PostgreEventTrigger fetchObject(@NotNull JDBCSession session, @NotNull PostgreDatabase database, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            String eventTriggerName = JDBCUtils.safeGetString(resultSet, "evtname");
            if (CommonUtils.isEmpty(eventTriggerName)) {
                return null;
            }
            return new PostgreEventTrigger(database, eventTriggerName, resultSet);
        }

        @Override
        protected boolean handleCacheReadError(Exception error) {
            if (PostgreConstants.EC_PERMISSION_DENIED.equals(SQLState.getStateFromException(error))) {
                log.warn(error);
                setCache(Collections.emptyList());
                return true;
            }
            return false;
        }
    }
    
    static class ExtensionCache extends PostgreDatabaseJDBCObjectCache<PostgreExtension> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull PostgreDatabase owner)
            throws SQLException {
            // dbStat.setLong(1, PostgreSchema.this.getObjectId());
            return session.prepareStatement(
                  "SELECT \n" +
                  " e.oid,\n" +
                  " cfg.tbls,\n" +
                  "  n.nspname as schema_name,\n" +
                  " e.* \n" +
                  "FROM \n" +
                  " pg_catalog.pg_extension e \n" +
                  " join pg_namespace n on n.oid =e.extnamespace\n" +
                  " left join  (\n" +
                  "         select\n" +
                  "            ARRAY_AGG(ns.nspname || '.' ||  cls.relname) tbls, oid_ext\n" +
                  "          from\n" +
                  "            (\n" +
                  "            select\n" +
                  "                unnest(e1.extconfig) oid , e1.oid oid_ext\n" +
                  "            from\n" +
                  "                pg_catalog.pg_extension e1 ) c \n" +
                  "                join    pg_class cls on cls.oid = c.oid \n" +
                  "                join pg_namespace ns on ns.oid = cls.relnamespace\n" +
                  "            group by oid_ext        \n" +
                  "         ) cfg on cfg.oid_ext = e.oid\n" +
                  "ORDER BY e.oid"
            );
        }

        @Override
        protected PostgreExtension fetchObject(@NotNull JDBCSession session, @NotNull PostgreDatabase owner, @NotNull JDBCResultSet dbResult)
            throws SQLException, DBException {
            return new PostgreExtension(owner, dbResult);
        }
    }

    public static class SchemaCache extends JDBCObjectLookupCache<PostgreDatabase, PostgreSchema> {
        @NotNull
        @Override
        public JDBCStatement prepareLookupStatement(@NotNull JDBCSession session, @NotNull PostgreDatabase database, @Nullable PostgreSchema object, @Nullable String objectName) throws SQLException {
            StringBuilder catalogQuery = new StringBuilder(
                "SELECT n.oid,n.*,d.description FROM pg_catalog.pg_namespace n\n" +
                "LEFT OUTER JOIN pg_catalog.pg_description d ON d.objoid=n.oid AND d.objsubid=0 AND d.classoid='pg_namespace'::regclass\n");
            boolean extraConditionAdded = addExtraCondition(session, catalogQuery);
            DBSObjectFilter catalogFilters = database.getDataSource().getContainer().getObjectFilter(PostgreSchema.class, null, false);
            if ((catalogFilters != null && !catalogFilters.isNotApplicable()) || object != null || objectName != null) {
                if (object != null || objectName != null) {
                    catalogFilters = new DBSObjectFilter();
                    catalogFilters.addInclude(object != null ? object.getName() : objectName);
                } else {
                    catalogFilters = new DBSObjectFilter(catalogFilters);
                    // Always read catalog schema
                    List<String> includeFilters = catalogFilters.getInclude();
                    if (!CommonUtils.isEmpty(includeFilters) && !includeFilters.contains(PostgreConstants.CATALOG_SCHEMA_NAME)) {
                        catalogFilters.addInclude(PostgreConstants.CATALOG_SCHEMA_NAME);
                    }
                }
                JDBCUtils.appendFilterClause(
                    catalogQuery,
                    catalogFilters,
                    "nspname",
                    !extraConditionAdded,
                    database.getDataSource());
            }
            catalogQuery.append(" ORDER BY nspname");
            JDBCPreparedStatement dbStat = session.prepareStatement(catalogQuery.toString());
            if (catalogFilters != null) {
                JDBCUtils.setFilterParameters(dbStat, 1, catalogFilters);
            }
            return dbStat;
        }

        @Override
        protected PostgreSchema fetchObject(@NotNull JDBCSession session, @NotNull PostgreDatabase owner, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            String name = JDBCUtils.safeGetString(resultSet, "nspname");
            if (name == null) {
                log.debug("Skipping schema with NULL name");
                return null;
            }
            if (PostgreSchema.isUtilitySchema(name) && !owner.getDataSource().getContainer().getNavigatorSettings().isShowUtilityObjects()) {
                return null;
            }
            return owner.createSchemaImpl(owner, name, resultSet);
        }

        /**
         * Adds condition in the query and returns true if condition is added.
         *
         * @param session to check columns existing
         * @param query query text needed for additions
         * @return true if condition added
         */
        protected boolean addExtraCondition(@NotNull JDBCSession session, @NotNull StringBuilder query) {
            // Do not do anything.
            return false;
        }
    }

    public static class JobCache extends JDBCObjectLookupCache<PostgreDatabase, PostgreJob> {
        @NotNull
        @Override
        public JDBCStatement prepareLookupStatement(@NotNull JDBCSession session, @NotNull PostgreDatabase database, @Nullable PostgreJob object, @Nullable String objectName) throws SQLException {
            final StringBuilder sql = new StringBuilder("SELECT * FROM pgagent.pga_job");
            if (object != null) {
                sql.append(" WHERE jobid=").append(object.getObjectId());
            }
            return session.prepareStatement(sql.toString());
        }

        @Nullable
        @Override
        protected PostgreJob fetchObject(@NotNull JDBCSession session, @NotNull PostgreDatabase database, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new PostgreJob(session.getProgressMonitor(), database, resultSet);
        }

        @Override
        protected boolean handleCacheReadError(Exception error) {
            DBWorkbench.getPlatformUI().showError("Error accessing pgAgent jobs", "Can't access pgAgent jobs.\n\nThis database may not have the extension installed or you don't have sufficient permissions to access them.\n\nIf you believe that this is DBeaver's fault, please report it.", error);
            setCache(Collections.emptyList());
            return true;
        }
    }

    public static class JobClassCache extends PostgreDatabaseJDBCObjectCache<PostgreJobClass> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull PostgreDatabase database) throws SQLException {
            return session.prepareStatement("SELECT * FROM pgagent.pga_jobclass");
        }

        @Nullable
        @Override
        protected PostgreJobClass fetchObject(@NotNull JDBCSession session, @NotNull PostgreDatabase database, @NotNull JDBCResultSet dbResult) {
            return new PostgreJobClass(database, dbResult);
        }
    }

    public static class EnumValueCache extends PostgreDatabaseJDBCObjectCache<PostgreEnumValue> {

        @NotNull
        @Override
        public JDBCStatement prepareObjectsStatement(
            @NotNull JDBCSession session,
            @NotNull PostgreDatabase database
        ) throws SQLException {
            if (!database.getDataSource().isSupportsEnumTable()) {
                // For those who missed previous warnings
                return session.prepareStatement("SELECT 1");
            }
            return session.prepareStatement("SELECT * FROM pg_catalog.pg_enum");
        }

        @Nullable
        @Override
        protected PostgreEnumValue fetchObject(
            @NotNull JDBCSession session,
            @NotNull PostgreDatabase database,
            @NotNull JDBCResultSet resultSet
        ) throws SQLException, DBException {
            return new PostgreEnumValue(database.getDataSource(), database, resultSet);
        }
    }

    public static class TablespaceListProvider implements IPropertyValueListProvider<PostgreDatabase> {
        @Override
        public boolean allowCustomValue()
        {
            return false;
        }
        @Override
        public Object[] getPossibleValues(PostgreDatabase object)
        {
            try {
                Collection<PostgreTablespace> tablespaces = object.getTablespaces(new VoidProgressMonitor());
                return tablespaces.toArray(new Object[0]);
            } catch (DBException e) {
                log.error(e);
                return new Object[0];
            }
        }
    }

    public static class RoleListProvider implements IPropertyValueListProvider<PostgreDatabase> {
        @Override
        public boolean allowCustomValue()
        {
            return false;
        }

        @Override
        public Object[] getPossibleValues(PostgreDatabase object)
        {
            try {
                Collection<? extends PostgreRole> roles = object.getAuthIds(new VoidProgressMonitor());
                return roles.toArray(new Object[0]);
            } catch (DBException e) {
                log.error(e);
                return new Object[0];
            }
        }
    }

    public static class CharsetListProvider implements IPropertyValueListProvider<PostgreDatabase> {
        @Override
        public boolean allowCustomValue()
        {
            return false;
        }

        @Override
        public Object[] getPossibleValues(PostgreDatabase object)
        {
            try {
                Collection<PostgreCharset> tablespaces = object.getEncodings(new VoidProgressMonitor());
                return tablespaces.toArray(new Object[0]);
            } catch (DBException e) {
                log.error(e);
                return new Object[0];
            }
        }
    }
}
