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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBDatabaseException;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.kingbase.KingbaseConstants;
import org.jkiss.dbeaver.ext.kingbase.KingbaseUtils;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPDataTypeProvider;
import org.jkiss.dbeaver.model.DBPNamedObject2;
import org.jkiss.dbeaver.model.DBPObjectStatistics;
import org.jkiss.dbeaver.model.DBPObjectWithLazyDescription;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.DBPStatefulObject;
import org.jkiss.dbeaver.model.DBUtils;
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
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.ForTest;
import org.jkiss.dbeaver.model.meta.IPropertyValueListProvider;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.PropertyLength;
import org.jkiss.dbeaver.model.preferences.DBPPropertySource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLState;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSInstanceLazy;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectFilter;
import org.jkiss.dbeaver.model.struct.DBSObjectState;
import org.jkiss.dbeaver.model.struct.rdb.DBSCatalog;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.LongKeyMap;

/**
 * KingbaseDatabase
 */
public class KingbaseDatabase extends JDBCRemoteInstance
    implements
        DBSCatalog,
        DBPRefreshableObject,
        DBPStatefulObject,
        DBPNamedObject2,
        KingbaseObject,
        DBPDataTypeProvider,
        DBSInstanceLazy,
        DBPObjectStatistics,
        DBPObjectWithLazyDescription
{

    private static final Log log = Log.getLog(KingbaseDatabase.class);

    private transient KingbaseRole initialOwner;
    private transient KingbaseTablespace initialTablespace;
    private transient KingbaseCharset initialEncoding;

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

    private final KingbaseDatabaseJDBCObjectCache<? extends KingbaseRole> roleCache = createRoleCache();
    final AccessMethodCache accessMethodCache = new AccessMethodCache();
    final LanguageCache languageCache = new LanguageCache();
    private final EncodingCache encodingCache = new EncodingCache();
    private final CollationCache collationCache = new CollationCache();
    public final TablespaceCache tablespaceCache = new TablespaceCache();
    private final LongKeyMap<KingbaseDataType> dataTypeCache = new LongKeyMap<>();

    public JDBCObjectLookupCache<KingbaseDatabase, KingbaseSchema> schemaCache;
    private final EnumValueCache enumValueCache = new EnumValueCache();

    protected KingbaseDatabase(DBRProgressMonitor monitor, KingbaseDataSource dataSource, ResultSet dbResult)
        throws DBException {
        super(monitor, dataSource, false);
        this.initCaches();
        this.loadInfo(dbResult);
    }

    protected KingbaseDatabase(DBRProgressMonitor monitor, KingbaseDataSource dataSource, String databaseName)
        throws DBException {
        super(monitor, dataSource, false);
        // We need to set name first
        this.name = databaseName;
        this.initCaches();
        checkInstanceConnection(monitor, false);

        try {
            readDatabaseInfo(monitor);
        } catch (DBCException e) {
            log.debug("Error reading database info", e);
        }
    }

    protected KingbaseDatabase(DBRProgressMonitor monitor, KingbaseDataSource dataSource, String name, KingbaseRole owner, String templateName, KingbaseTablespace tablespace, KingbaseCharset encoding) throws DBException {
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
    KingbaseDatabase(KingbaseDataSource dataSource, String databaseName) {
        super(dataSource);
        this.name = databaseName;
        this.initCaches();
        KingbaseSchema sysSchema = new KingbaseSchema(this, KingbaseConstants.CATALOG_SCHEMA_NAME);
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
    public KingbaseExecutionContext getMetaContext() {
        return (KingbaseExecutionContext) super.getDefaultContext(true);
    }

    private void initCaches() {
        schemaCache = getDataSource().getServerType().createSchemaCache(this);

    }

    private void initEnumTypesCache(@NotNull DBRProgressMonitor monitor) throws DBException {
        if (((KingbaseDataSource) dataSource).isSupportsEnumTable()) {
            enumValueCache.getAllObjects(monitor, this);
        }
    }

    private void readDatabaseInfo(DBRProgressMonitor monitor) throws DBCException {
        try (JDBCSession session = getMetaContext().openSession(monitor, DBCExecutionPurpose.META, "Load database info")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement("SELECT db.oid,db.* FROM sys_catalog.sys_database db WHERE datname=?")) {
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

    public KingbaseRole getInitialOwner() {
        return initialOwner;
    }

    public void setInitialOwner(KingbaseRole initialOwner) {
        this.initialOwner = initialOwner;
    }

    public KingbaseTablespace getInitialTablespace() {
        return initialTablespace;
    }

    public void setInitialTablespace(KingbaseTablespace initialTablespace) {
        this.initialTablespace = initialTablespace;
    }

    public KingbaseCharset getInitialEncoding() {
        return initialEncoding;
    }

    public void setInitialEncoding(KingbaseCharset initialEncoding) {
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
        
        this.collate = JDBCUtils.safeGetString(dbResult, "datcollate");
        this.ctype = JDBCUtils.safeGetString(dbResult, "datctype");
        
        this.isTemplate = JDBCUtils.safeGetBoolean(dbResult, "datistemplate");
        this.allowConnect = JDBCUtils.safeGetBoolean(dbResult, "datallowconn");
    
        this.connectionLimit = JDBCUtils.safeGetInt(dbResult, "datconnlimit");
       
        this.tablespaceId = JDBCUtils.safeGetLong(dbResult, "dattablespace");
    }

    @NotNull
    @Override
    public KingbaseDatabase getDatabase() {
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
    public KingbaseExecutionContext openIsolatedContext(@NotNull DBRProgressMonitor monitor, @NotNull String purpose, @Nullable DBCExecutionContext initFrom) throws DBException {
        KingbaseExecutionContext ec = (KingbaseExecutionContext) super.openIsolatedContext(monitor, purpose, initFrom);
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
            description = JDBCUtils.queryString(session, "select description from sys_shdescription "
                    + "join sys_database on objoid = sys_database.oid where datname = ?", getName());
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
    public KingbaseDataSource getDataSource() {
        return (KingbaseDataSource) dataSource;
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
    public KingbaseRole getDBA(DBRProgressMonitor monitor) throws DBException {
        checkInstanceConnection(monitor);
        return getRoleById(monitor, ownerId);
    }

    public void setDBA(KingbaseRole owner) {
        this.ownerId = owner.getObjectId();
    }

    public KingbaseDatabaseJDBCObjectCache<? extends KingbaseRole> getRoleCache() {
        return roleCache;
    }

    @Nullable
    public KingbaseRole getRoleById(DBRProgressMonitor monitor, long roleId) throws DBException {
        if (!getDataSource().getServerType().supportsRoles()) {
            return null;
        }
        checkInstanceConnection(monitor);
        return KingbaseUtils.getObjectById(monitor, roleCache, this, roleId);
    }

    @Nullable
    public KingbaseRole getRoleByReference(@NotNull DBRProgressMonitor monitor, @NotNull KingbaseRoleReference reference) throws DBException {
        if (!getDataSource().getServerType().supportsRoles()) {
            return null;
        }
        checkInstanceConnection(monitor);
        return roleCache.getObject(monitor, this, reference.getRoleName());
    }

    @Property(editable = false, updatable = false, order = 5/*, listProvider = CharsetListProvider.class*/)
    public KingbaseCharset getDefaultEncoding(DBRProgressMonitor monitor) throws DBException {
        if (!getDataSource().getServerType().supportsEncodings()) {
            return null;
        }
        checkInstanceConnection(monitor);
        return KingbaseUtils.getObjectById(monitor, encodingCache, this, encodingId);
    }

    public void setDefaultEncoding(KingbaseCharset charset) throws DBException {
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

    @Association
    public Collection<? extends KingbaseRole> getAuthIds(DBRProgressMonitor monitor) throws DBException {
        if (!getDataSource().supportsRoles()) {
            return Collections.emptyList();
        }
        checkInstanceConnection(monitor);
        return roleCache.getAllObjects(monitor, this);
    }

    @Association
    public Collection<KingbaseAccessMethod> getAccessMethods(DBRProgressMonitor monitor) throws DBException {
        checkInstanceConnection(monitor);
        return accessMethodCache.getAllObjects(monitor, this);
    }

    @Association
    public Collection<KingbaseLanguage> getLanguages(DBRProgressMonitor monitor) throws DBException {
        checkInstanceConnection(monitor);
        return languageCache.getAllObjects(monitor, this);
    }

    @Association
    public Collection<KingbaseSetting> getSettings(DBRProgressMonitor monitor) throws DBException {
        checkInstanceConnection(monitor);
        return getDataSource().getSettingCache().getAllObjects(monitor, getDataSource());
    }

    @Association
    public Collection<KingbaseCharset> getEncodings(DBRProgressMonitor monitor) throws DBException {
        if (!getDataSource().getServerType().supportsEncodings()) {
            return null;
        }
        checkInstanceConnection(monitor);
        return encodingCache.getAllObjects(monitor, this);
    }
   

    @Association
    public Collection<KingbaseCollation> getCollations(DBRProgressMonitor monitor)
        throws DBException {
        return collationCache.getAllObjects(monitor, this);
    }

    @Association
    public KingbaseCollation getCollation(DBRProgressMonitor monitor, long id)
        throws DBException {
        for (KingbaseCollation collation : collationCache.getAllObjects(monitor, this)) {
            if (collation.getObjectId() == id) {
                return collation;
            }
        }
        log.debug("Collation '" + id + "' not found in schema " + getName());
        return null;
    }



    @NotNull
    @Override
    public DBPDataKind resolveDataKind(@NotNull String typeName, int typeID) {
        return dataSource.resolveDataKind(typeName, typeID);
    }

    @Override
    public DBSDataType resolveDataType(@NotNull DBRProgressMonitor monitor, @NotNull String typeFullName) throws DBException {
        return KingbaseUtils.resolveTypeFullName(monitor, this, typeFullName);
    }

    @Override
    public Collection<KingbaseDataType> getLocalDataTypes() {
        synchronized (dataTypeCache) {
            if (!CommonUtils.isEmpty(dataTypeCache)) {
                return new ArrayList<>(dataTypeCache.values());
            }
        }
        final KingbaseSchema schema = getCatalogSchema();
        if (schema != null) {
            return schema.getDataTypeCache().getCachedObjects();
        }
        return null;
    }

    @Override
    public KingbaseDataType getLocalDataType(String typeName) {
        return getDataType(null, typeName);
    }

    @Override
    public DBSDataType getLocalDataType(int typeID) {
        return getDataType(new VoidProgressMonitor(), typeID);
    }

    @Override
    public String getDefaultDataTypeName(@NotNull DBPDataKind dataKind) {
        return KingbaseUtils.getDefaultDataTypeName(dataKind);
    }

    /**
     * @return enum values cache. Do not use is if database do not support enams. Check {@code KingbaseDatasource#isSupportsEnumTable}
     */
    EnumValueCache getEnumValueCache() {
        return enumValueCache;
    }

    @Association
    public Collection<KingbaseTablespace> getTablespaces(DBRProgressMonitor monitor) throws DBException {
        checkInstanceConnection(monitor);
        return tablespaceCache.getAllObjects(monitor, this);
    }

    @Property(editable = true, updatable = true, order = 4, listProvider = TablespaceListProvider.class)
    public KingbaseTablespace getDefaultTablespace(DBRProgressMonitor monitor) throws DBException {
        checkInstanceConnection(monitor);
        return KingbaseUtils.getObjectById(monitor, tablespaceCache, this, tablespaceId);
    }

    public void setDefaultTablespace(KingbaseTablespace tablespace) throws DBException {
        this.tablespaceId = tablespace.getObjectId();
    }

    public KingbaseTablespace getTablespace(DBRProgressMonitor monitor, long tablespaceId) throws DBException {
        checkInstanceConnection(monitor);
        for (KingbaseTablespace ts : tablespaceCache.getAllObjects(monitor, this)) {
            if (ts.getObjectId() == tablespaceId) {
                return ts;
            }
        }
        return null;
    }

    @Association
    public Collection<KingbaseSchema> getSchemas(DBRProgressMonitor monitor) throws DBException {
        if (monitor != null) {
            checkInstanceConnection(monitor);
        }
        // Get all schemas
        return monitor == null ? schemaCache.getCachedObjects() : schemaCache.getAllObjects(monitor, this);
    }

    @Nullable
    public KingbaseSchema getCatalogSchema(DBRProgressMonitor monitor) throws DBException {
        return getSchema(monitor, KingbaseConstants.CATALOG_SCHEMA_NAME);
    }

    @Nullable
    KingbaseSchema getCatalogSchema() {
        return schemaCache.getCachedObject(KingbaseConstants.CATALOG_SCHEMA_NAME);
    }

    @Nullable
    KingbaseSchema getActiveSchema() {
        return getMetaContext().getDefaultSchema();
    }

    @Nullable
    KingbaseSchema getPublicSchema() {
        return schemaCache.getCachedObject(KingbaseConstants.PUBLIC_SCHEMA_NAME);
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

            KingbaseDataSource kingbaseDataSource = getDataSource();
            boolean readAllTypes = kingbaseDataSource.supportReadingAllDataTypes();

            try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Read data types")) {
                StringBuilder sql = new StringBuilder(256);
                boolean supportsSysTypColumn = supportsSysTypCategoryColumn(session); // Do not read all array and table types, unless the user has decided otherwise
                sql.append("SELECT t.oid,t.*,c.relkind,").append(KingbaseDataTypeCache.getBaseTypeNameClause(kingbaseDataSource)).append(", d.description" +
                        "\nFROM sys_catalog.sys_type t");
                if (!readAllTypes && supportsSysTypColumn) {
                    sql.append("\nLEFT OUTER JOIN sys_catalog.sys_type et ON et.oid=t.typelem "); // If typelem is not 0 then it identifies another row in sys_type
                }
                sql.append("\nLEFT OUTER JOIN sys_catalog.sys_class c ON c.oid=t.typrelid" +
                        "\nLEFT OUTER JOIN sys_catalog.sys_description d ON t.oid=d.objoid" +
                        "\nWHERE t.typname IS NOT NULL");
                if (!readAllTypes) {
                    sql.append("\nAND (c.relkind IS NULL OR c.relkind = 'c')");
                    if (supportsSysTypColumn) {
                        sql.append(" AND (et.typcategory IS NULL OR et.typcategory <> 'C')");
                    }
                }

                List<KingbaseDataType> loadedDataTypes = new ArrayList<>();
                try (JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString())) {
                    try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                        Set<KingbaseSchema> schemaList = new HashSet<>();
                        while (dbResult.next()) {
                            KingbaseDataType dataType = KingbaseDataType.readDataType(session, this, dbResult, !readAllTypes);
                            if (dataType != null) {
                                KingbaseSchema schema = dataType.getParentObject();
                                schemaList.add(schema);
                                schema.getDataTypeCache().cacheObject(dataType);
                                loadedDataTypes.add(dataType);
                            }
                        }
                        if (!schemaList.isEmpty()) {
                            for (KingbaseSchema schema : schemaList) {
                                schema.getDataTypeCache().setFullCache(true);
                            }
                        }
                        KingbaseSchema catalogSchema = getCatalogSchema();
                        if (catalogSchema != null) {
                            catalogSchema.getDataTypeCache().mapAliases(catalogSchema);
                        }
                    }
                }
                synchronized (dataTypeCache) {
                    for (KingbaseDataType dataType : loadedDataTypes) {
                        dataTypeCache.put(dataType.getObjectId(), dataType);
                    }
                }
            } catch (SQLException e) {
                throw new DBDatabaseException(e, kingbaseDataSource);
            }
            initEnumTypesCache(monitor);
        }
    }
    boolean supportsSysTypCategoryColumn(JDBCSession session) {
        if (supportTypColumn == null) {
       
            supportTypColumn = true;
           
        }
        return supportTypColumn;
    }

    public KingbaseSchema getSchema(DBRProgressMonitor monitor, String name) throws DBException {
        checkInstanceConnection(monitor);
        return schemaCache.getObject(monitor, this, name);
    }

    public KingbaseSchema getSchema(DBRProgressMonitor monitor, long oid) throws DBException {
        checkInstanceConnection(monitor);
        for (KingbaseSchema schema : schemaCache.getAllObjects(monitor, this)) {
            if (schema.getObjectId() == oid) {
                return schema;
            }
        }
        return null;
    }

    @Nullable
    public KingbaseSchema getSchema(long oid) {
        for (KingbaseSchema schema : schemaCache.getCachedObjects()) {
            if (schema.getObjectId() == oid) {
                return schema;
            }
        }
        return null;
    }

    public KingbaseSchema createSchemaImpl(@NotNull KingbaseDatabase owner, @NotNull String name, @NotNull JDBCResultSet resultSet) throws SQLException {
        return new KingbaseSchema(owner, name, resultSet);
    }

    public KingbaseSchema createSchemaImpl(@NotNull KingbaseDatabase owner, @NotNull String name, @Nullable KingbaseRole kingbaseRole) {
        return new KingbaseSchema(owner, name, kingbaseRole);
    }

    KingbaseTableBase findTable(DBRProgressMonitor monitor, long schemaId, long tableId)
        throws DBException {
        KingbaseSchema schema = getSchema(monitor, schemaId);
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
        return KingbaseSchema.class;
    }

    @Override
    public void cacheStructure(@NotNull DBRProgressMonitor monitor, int scope) throws DBException {

    }

    @NotNull
    @Override
    public DBSObjectState getObjectState() {
        if (this == dataSource.getDefaultInstance() || this.isSharedDatabase()) {
            return DBSObjectState.NORMAL;
        } else {
            return KingbaseConstants.STATE_UNAVAILABLE;
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
        languageCache.clearCache();
        encodingCache.clearCache();
        collationCache.clearCache();
        tablespaceCache.clearCache();
        schemaCache.clearCache();
        cacheDataTypes(monitor, true);
        enumValueCache.clearCache();

        return this;
    }

    public Collection<? extends KingbaseRole> getUsers(DBRProgressMonitor monitor) throws DBException {
        if (!getDataSource().getServerType().supportsRoles()) {
            return Collections.emptyList();
        }
        checkInstanceConnection(monitor);
        return roleCache.getAllObjects(monitor, this);
    }

    public KingbaseProcedure getProcedure(DBRProgressMonitor monitor, long schemaId, long procId)
        throws DBException {
        final KingbaseSchema schema = getSchema(monitor, schemaId);
        if (schema != null) {
            return KingbaseUtils.getObjectById(monitor, schema.getProceduresCache(), schema, procId);
        }
        return null;
    }

    public KingbaseProcedure getProcedure(DBRProgressMonitor monitor, long procId)
        throws DBException {
        for (final KingbaseSchema schema : getSchemas(monitor)) {
            KingbaseProcedure procedure = KingbaseUtils.getObjectById(monitor, schema.getProceduresCache(), schema, procId);
            if (procedure != null) {
                return procedure;
            }
        }
        return null;
    }

    public KingbaseDataType getDataType(DBRProgressMonitor monitor, long typeId) {
        if (typeId <= 0) {
            return null;
        }

        KingbaseDataType dataType;
        synchronized (dataTypeCache) {
            dataType = dataTypeCache.get(typeId);
            if (dataType != null) {
                return dataType;
            }
        }
        for (KingbaseSchema schema : schemaCache.getCachedObjects()) {
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
            dataType = KingbaseDataTypeCache.resolveDataType(monitor, this, typeId);
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

    public KingbaseDataType getDataType(@Nullable DBRProgressMonitor monitor, String typeName) {
        if (typeName.endsWith("[]")) {
            typeName = "_" + typeName.substring(0, typeName.length() - 2);
        }
        {
            // First check system catalog
            final KingbaseSchema schema = getCatalogSchema();
            if (schema != null) {
                final KingbaseDataType dataType = schema.getDataTypeCache().getCachedObject(typeName);
                if (dataType != null) {
                    return dataType;
                }
            }
        }

        // Check schemas in search path
        KingbaseExecutionContext metaContext = getMetaContext();
        Vector<String> schemaes = new Vector<String>();
        schemaes.add(KingbaseConstants.CATALOG_SCHEMA_NAME);
        schemaes.add(KingbaseConstants.SYS_CATALOG_SCHEMA_NAME);
        Enumeration<String> enumSchemaes = schemaes.elements();
        List<String> searchPath = metaContext == null ? Collections.list(enumSchemaes): metaContext.getSearchPath();
        for (String schemaName : searchPath) {
            final KingbaseSchema schema = schemaCache.getCachedObject(schemaName);
            if (schema != null) {
                final KingbaseDataType dataType = schema.getDataTypeCache().getCachedObject(typeName);
                if (dataType != null) {
                    return dataType;
                }
            }
        }
        // Check the rest
        for (KingbaseSchema schema : schemaCache.getCachedObjects()) {
            if (searchPath.contains(schema.getName())) {
                continue;
            }
            final KingbaseDataType dataType = schema.getDataTypeCache().getCachedObject(typeName);
            if (dataType != null) {
                return dataType;
            }
        }

        if (monitor == null || monitor.isForceCacheUsage()) {
            return null;
        }

        // Type not found. Let's resolve it
        try {
            KingbaseDataType dataType = KingbaseDataTypeCache.resolveDataType(monitor, this, typeName);
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

    @NotNull
    protected KingbaseDatabaseJDBCObjectCache<? extends KingbaseRole> createRoleCache() {
        return new RoleCache();
    }

    protected static abstract class KingbaseDatabaseJDBCObjectCache<OBJECT extends DBSObject> extends JDBCObjectCache<KingbaseDatabase, OBJECT> {
        boolean handlePermissionDeniedError(Exception e) {
            if (KingbaseConstants.EC_PERMISSION_DENIED.equals(SQLState.getStateFromException(e))) {
                log.warn(e);
                setCache(Collections.emptyList());
                return true;
            }
            return false;
        }
    }

    static class RoleCache extends KingbaseDatabaseJDBCObjectCache<KingbaseRole> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull KingbaseDatabase owner)
            throws SQLException {
            boolean supportsCommentsOnRole = owner.getDataSource().getServerType().supportsCommentsOnRole();
            String sql = "SELECT a.oid,a.*" + (supportsCommentsOnRole ? ",pd.description" : "") +
                " FROM sys_catalog.sys_roles a " +
                (supportsCommentsOnRole ? "\nleft join sys_catalog.sys_shdescription pd on a.oid = pd.objoid" : "") +
                "\nwhere a.rolname not like 'pg_%'" +
                "\nORDER BY a.rolname";
            return session.prepareStatement(sql);
        }

        @Override
        protected KingbaseRole fetchObject(@NotNull JDBCSession session, @NotNull KingbaseDatabase owner, @NotNull JDBCResultSet dbResult)
            throws SQLException, DBException {
            return new KingbaseRole(owner, dbResult);
        }

        @Override
        protected boolean handleCacheReadError(Exception error) {
            return handlePermissionDeniedError(error);
        }
    }

    static class AccessMethodCache extends KingbaseDatabaseJDBCObjectCache<KingbaseAccessMethod> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull KingbaseDatabase owner)
            throws SQLException {
            return session.prepareStatement(
                "SELECT am.oid,am.* FROM sys_catalog.sys_am am " +
                    "\nORDER BY am.oid"
            );
        }

        @Override
        protected KingbaseAccessMethod fetchObject(@NotNull JDBCSession session, @NotNull KingbaseDatabase owner, @NotNull JDBCResultSet dbResult)
            throws SQLException, DBException {
            return new KingbaseAccessMethod(owner, dbResult);
        }
    }

    static class EncodingCache extends KingbaseDatabaseJDBCObjectCache<KingbaseCharset> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull KingbaseDatabase owner)
            throws SQLException {
            return session.prepareStatement(
                "SELECT c.contoencoding as encid,sys_catalog.sys_encoding_to_char(c.contoencoding) as encname\n" +
                    "FROM sys_catalog.sys_conversion c\n" +
                    "GROUP BY c.contoencoding\n" +
                    "ORDER BY 2\n"
            );
        }

        @Override
        protected KingbaseCharset fetchObject(@NotNull JDBCSession session, @NotNull KingbaseDatabase owner, @NotNull JDBCResultSet dbResult)
            throws SQLException, DBException {
            return new KingbaseCharset(owner, dbResult);
        }
    }

    static class CollationCache extends KingbaseDatabaseJDBCObjectCache<KingbaseCollation> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull KingbaseDatabase owner)
            throws SQLException {
            return session.prepareStatement(
                "SELECT c.oid,c.* FROM sys_catalog.sys_collation c " +
                    "\nORDER BY c.oid"
            );
        }

        @Override
        protected KingbaseCollation fetchObject(@NotNull JDBCSession session, @NotNull KingbaseDatabase owner, @NotNull JDBCResultSet dbResult)
            throws SQLException, DBException
        {
            return new KingbaseCollation(session.getProgressMonitor(), owner, dbResult);
        }
    }

    static class LanguageCache extends KingbaseDatabaseJDBCObjectCache<KingbaseLanguage> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull KingbaseDatabase owner)
            throws SQLException {
            return session.prepareStatement(
                "SELECT l.oid,l.* FROM sys_catalog.sys_language l " +
                    "\nORDER BY l.oid"
            );
        }

        @Override
        protected KingbaseLanguage fetchObject(@NotNull JDBCSession session, @NotNull KingbaseDatabase owner, @NotNull JDBCResultSet dbResult)
            throws SQLException, DBException {
            return new KingbaseLanguage(owner, dbResult);
        }
    }

    static class TablespaceCache extends KingbaseDatabaseJDBCObjectCache<KingbaseTablespace> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull KingbaseDatabase owner)
            throws SQLException {
            return session.prepareStatement(
                "SELECT t.oid,t.*" +
                    (owner.getDataSource().getServerType().supportsTablespaceLocation() ? ",sys_tablespace_location(t.oid) loc" : "") +
                    "\nFROM sys_catalog.sys_tablespace t " +
                    "\nORDER BY t.oid"
            );
        }

        @Override
        protected KingbaseTablespace fetchObject(@NotNull JDBCSession session, @NotNull KingbaseDatabase owner, @NotNull JDBCResultSet dbResult)
            throws SQLException, DBException {
            return new KingbaseTablespace(owner, dbResult);
        }

        @Override
        protected boolean handleCacheReadError(Exception error) {
            return handlePermissionDeniedError(error);
        }
    }

    public static class SchemaCache extends JDBCObjectLookupCache<KingbaseDatabase, KingbaseSchema> {
        @NotNull
        @Override
        public JDBCStatement prepareLookupStatement(@NotNull JDBCSession session, @NotNull KingbaseDatabase database, @Nullable KingbaseSchema object, @Nullable String objectName) throws SQLException {
            StringBuilder catalogQuery = new StringBuilder(
                "SELECT n.oid,n.*,d.description FROM sys_catalog.sys_namespace n\n" +
                "LEFT OUTER JOIN sys_catalog.sys_description d ON d.objoid=n.oid AND d.objsubid=0 AND d.classoid='sys_namespace'::regclass\n");
            boolean extraConditionAdded = addExtraCondition(session, catalogQuery);
            DBSObjectFilter catalogFilters = database.getDataSource().getContainer().getObjectFilter(KingbaseSchema.class, null, false);
            if ((catalogFilters != null && !catalogFilters.isNotApplicable()) || object != null || objectName != null) {
                if (object != null || objectName != null) {
                    catalogFilters = new DBSObjectFilter();
                    catalogFilters.addInclude(object != null ? object.getName() : objectName);
                } else {
                    catalogFilters = new DBSObjectFilter(catalogFilters);
                    // Always read catalog schema
                    List<String> includeFilters = catalogFilters.getInclude();
                    if (!CommonUtils.isEmpty(includeFilters) && !includeFilters.contains(KingbaseConstants.CATALOG_SCHEMA_NAME) && !includeFilters.contains(KingbaseConstants.SYS_CATALOG_SCHEMA_NAME)) {
                        catalogFilters.addInclude(KingbaseConstants.CATALOG_SCHEMA_NAME);
                        catalogFilters.addInclude(KingbaseConstants.SYS_CATALOG_SCHEMA_NAME);
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
        protected KingbaseSchema fetchObject(@NotNull JDBCSession session, @NotNull KingbaseDatabase owner, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            String name = JDBCUtils.safeGetString(resultSet, "nspname");
            
            if (name == null) {
                return null;
            }
            if (KingbaseSchema.isUtilitySchema(name) && !owner.getDataSource().getContainer().getNavigatorSettings().isShowUtilityObjects()) {
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

    public static class EnumValueCache extends KingbaseDatabaseJDBCObjectCache<KingbaseEnumValue> {

        @NotNull
        @Override
        public JDBCStatement prepareObjectsStatement(
            @NotNull JDBCSession session,
            @NotNull KingbaseDatabase database
        ) throws SQLException {
            if (!database.getDataSource().isSupportsEnumTable()) {
                return session.prepareStatement("SELECT 1");
            }
            return session.prepareStatement("SELECT * FROM sys_catalog.sys_enum");
        }

        @Nullable
        @Override
        protected KingbaseEnumValue fetchObject(
            @NotNull JDBCSession session,
            @NotNull KingbaseDatabase database,
            @NotNull JDBCResultSet resultSet
        ) throws SQLException, DBException {
            return new KingbaseEnumValue(database.getDataSource(), database, resultSet);
        }
    }

    public static class TablespaceListProvider implements IPropertyValueListProvider<KingbaseDatabase> {
        @Override
        public boolean allowCustomValue()
        {
            return false;
        }
        @Override
        public Object[] getPossibleValues(KingbaseDatabase object)
        {
            try {
                Collection<KingbaseTablespace> tablespaces = object.getTablespaces(new VoidProgressMonitor());
                return tablespaces.toArray(new Object[0]);
            } catch (DBException e) {
                log.error(e);
                return new Object[0];
            }
        }
    }

    public static class RoleListProvider implements IPropertyValueListProvider<KingbaseDatabase> {
        @Override
        public boolean allowCustomValue()
        {
            return false;
        }

        @Override
        public Object[] getPossibleValues(KingbaseDatabase object)
        {
            try {
                Collection<? extends KingbaseRole> roles = object.getAuthIds(new VoidProgressMonitor());
                return roles.toArray(new Object[0]);
            } catch (DBException e) {
                log.error(e);
                return new Object[0];
            }
        }
    }

    public static class CharsetListProvider implements IPropertyValueListProvider<KingbaseDatabase> {
        @Override
        public boolean allowCustomValue()
        {
            return false;
        }

        @Override
        public Object[] getPossibleValues(KingbaseDatabase object)
        {
            try {
                Collection<KingbaseCharset> tablespaces = object.getEncodings(new VoidProgressMonitor());
                return tablespaces.toArray(new Object[0]);
            } catch (DBException e) {
                log.error(e);
                return new Object[0];
            }
        }
    }
}
