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

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.postgresql.PostgreConstants;
import org.jkiss.dbeaver.ext.postgresql.PostgreUtils;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.exec.DBCException;
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
import org.jkiss.dbeaver.model.meta.IPropertyValueListProvider;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.struct.rdb.DBSCatalog;
import org.jkiss.utils.LongKeyMap;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

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
        DBSInstanceLazy {

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

    public final RoleCache roleCache = new RoleCache();
    public final AccessMethodCache accessMethodCache = new AccessMethodCache();
    public final ForeignDataWrapperCache foreignDataWrapperCache = new ForeignDataWrapperCache();
    public final ForeignServerCache foreignServerCache = new ForeignServerCache();
    public final LanguageCache languageCache = new LanguageCache();
    public final EncodingCache encodingCache = new EncodingCache();
    public final ExtensionCache extensionCache = new ExtensionCache();
    public final AvailableExtensionCache availableExtensionCache = new AvailableExtensionCache();
    public final CollationCache collationCache = new CollationCache();
    public final TablespaceCache tablespaceCache = new TablespaceCache();
    public final LongKeyMap<PostgreDataType> dataTypeCache = new LongKeyMap<>();

    public JDBCObjectLookupCache<PostgreDatabase, PostgreSchema> schemaCache;

    public PostgreDatabase(DBRProgressMonitor monitor, PostgreDataSource dataSource, ResultSet dbResult)
        throws DBException {
        super(monitor, dataSource, false);
        this.initCaches();
        this.loadInfo(dbResult);
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

    public PostgreDatabase(DBRProgressMonitor monitor, PostgreDataSource dataSource, String databaseName)
        throws DBException {
        super(monitor, dataSource, false);
        // We need to set name first
        this.name = databaseName;
        this.initCaches();
        checkInstanceConnection(monitor);

        try {
            readDatabaseInfo(monitor);
        } catch (DBCException e) {
            // On some multi-tenant servers pg_database is not public so error may gappen here
            log.debug("Error reading database info", e);
        }
    }

    public PostgreDatabase(DBRProgressMonitor monitor, PostgreDataSource dataSource, String name, PostgreRole owner, String templateName, PostgreTablespace tablespace, PostgreCharset encoding) throws DBException {
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

    private void readDatabaseInfo(DBRProgressMonitor monitor) throws DBCException {
        try (JDBCSession session = getMetaContext().openSession(monitor, DBCExecutionPurpose.META, "Load database info")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement("SELECT db.oid,db.*" +
                "\nFROM pg_catalog.pg_database db WHERE datname=?")) {
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
        if (executionContext == null) {
            initializeMainContext(monitor);
            initializeMetaContext(monitor);
        }
    }

    @Override
    public boolean isInstanceConnected() {
        return metaContext != null || executionContext != null;
    }

    private void loadInfo(ResultSet dbResult) {
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
        this.connectionLimit = JDBCUtils.safeGetInt(dbResult, "datconnlimit");
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
        return null;
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

    @Property(viewable = true, multiline = true, order = 100)
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
        } catch (SQLException e) {
            log.debug("Error reading database description ", e);
        }
        if (description == null) {
            description = "";
        }
        
        return description;
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

    @Nullable
    public PostgreRole getRoleById(DBRProgressMonitor monitor, long roleId) throws DBException {
        if (!getDataSource().getServerType().supportsRoles()) {
            return null;
        }
        checkInstanceConnection(monitor);
        return PostgreUtils.getObjectById(monitor, roleCache, this, roleId);
    }

    @Nullable
    public PostgreRole getRoleByName(DBRProgressMonitor monitor, PostgreDatabase owner, String roleName) throws DBException {
        if (!getDataSource().getServerType().supportsRoles()) {
            return null;
        }
        checkInstanceConnection(monitor);
        return roleCache.getObject(monitor, owner, roleName);
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
    public Collection<PostgreRole> getAuthIds(DBRProgressMonitor monitor) throws DBException {
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
    public Collection<PostgreCharset> getEncodings(DBRProgressMonitor monitor) throws DBException {
        if (!getDataSource().getServerType().supportsEncodings()) {
            return null;
        }
        checkInstanceConnection(monitor);
        return encodingCache.getAllObjects(monitor, this);
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

    @Override
    public DBPDataKind resolveDataKind(String typeName, int typeID) {
        return dataSource.resolveDataKind(typeName, typeID);
    }

    @Override
    public DBSDataType resolveDataType(DBRProgressMonitor monitor, String typeFullName) throws DBException {
        return dataSource.resolveDataType(monitor, typeFullName);
    }

    @Override
    public Collection<PostgreDataType> getLocalDataTypes() {
        final PostgreSchema schema = getCatalogSchema();
        if (schema != null) {
            return schema.dataTypeCache.getCachedObjects();
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

    ///////////////////////////////////////////////
    // Tablespaces

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

    ///////////////////////////////////////////////
    // Object container

    @Association
    public Collection<PostgreSchema> getSchemas(DBRProgressMonitor monitor) throws DBException {
        checkInstanceConnection(monitor);
        // Get all schemas
        return schemaCache.getAllObjects(monitor, this);
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
        if (dataTypeCache.isEmpty() || forceRefresh) {
            dataTypeCache.clear();
            // Cache data types
            for (final PostgreSchema pgSchema : getSchemas(monitor)) {
                if (PostgreConstants.CATALOG_SCHEMA_NAME.equals(pgSchema.getName())) {
                    pgSchema.getDataTypes(monitor);
                }
            }
        }
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

    @Override
    public Class<? extends DBSObject> getChildType(@NotNull DBRProgressMonitor monitor) throws DBException {
        return PostgreSchema.class;
    }

    @Override
    public void cacheStructure(@NotNull DBRProgressMonitor monitor, int scope) throws DBException {

    }

    @NotNull
    @Override
    public DBSObjectState getObjectState() {
        if (this == dataSource.getDefaultInstance()) {
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
        if (metaContext == null && executionContext == null) {
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
        extensionCache.clearCache();
        availableExtensionCache.clearCache();
        collationCache.clearCache();
        tablespaceCache.clearCache();
        schemaCache.clearCache();
        cacheDataTypes(monitor, true);

        return this;
    }

    public Collection<PostgreRole> getUsers(DBRProgressMonitor monitor) throws DBException {
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
        PostgreDataType dataType = dataTypeCache.get(typeId);
        if (dataType != null) {
            return dataType;
        }
        for (PostgreSchema schema : schemaCache.getCachedObjects()) {
            dataType = schema.dataTypeCache.getDataType(typeId);
            if (dataType != null) {
                dataTypeCache.put(typeId, dataType);
                return dataType;
            }
        }
        // Type not found. Let's resolve it
        try {
            dataType = PostgreDataTypeCache.resolveDataType(monitor, this, typeId);
            dataType.getParentObject().dataTypeCache.cacheObject(dataType);
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
                final PostgreDataType dataType = schema.dataTypeCache.getCachedObject(typeName);
                if (dataType != null) {
                    return dataType;
                }
            }
        }

        // Check schemas in search path
        List<String> searchPath = getMetaContext().getSearchPath();
        for (String schemaName : searchPath) {
            final PostgreSchema schema = schemaCache.getCachedObject(schemaName);
            if (schema != null) {
                final PostgreDataType dataType = schema.dataTypeCache.getCachedObject(typeName);
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
            final PostgreDataType dataType = schema.dataTypeCache.getCachedObject(typeName);
            if (dataType != null) {
                return dataType;
            }
        }

        if (monitor == null) {
            return null;
        }

        // Type not found. Let's resolve it
        try {
            PostgreDataType dataType = PostgreDataTypeCache.resolveDataType(monitor, this, typeName);
            dataType.getParentObject().dataTypeCache.cacheObject(dataType);
            return dataType;
        } catch (Exception e) {
            log.debug("Can't resolve data type '" + typeName + "' in database '" + getName() + "'");
            return null;
        }
    }

    @Override
    public String toString() {
        return name;
    }

    /////////////////////////////////////////////////////////////////////////////////////
    // Caches

    class RoleCache extends JDBCObjectCache<PostgreDatabase, PostgreRole> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull PostgreDatabase owner)
            throws SQLException {
            return session.prepareStatement(
                "SELECT a.oid,a.* FROM pg_catalog.pg_roles a " +
                    "\nORDER BY a.rolname"
            );
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
            if (error instanceof DBException && PostgreConstants.EC_PERMISSION_DENIED.equals(((DBException) error).getDatabaseState())) {
                log.warn(error);
                setCache(Collections.emptyList());
                return true;
            }
            return false;
        }
    }

    class AccessMethodCache extends JDBCObjectCache<PostgreDatabase, PostgreAccessMethod> {

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

    class EncodingCache extends JDBCObjectCache<PostgreDatabase, PostgreCharset> {

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

    class CollationCache extends JDBCObjectCache<PostgreDatabase, PostgreCollation> {

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

    class LanguageCache extends JDBCObjectCache<PostgreDatabase, PostgreLanguage> {

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

    class ForeignDataWrapperCache extends JDBCObjectCache<PostgreDatabase, PostgreForeignDataWrapper> {

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

    class ForeignServerCache extends JDBCObjectCache<PostgreDatabase, PostgreForeignServer> {

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

    class TablespaceCache extends JDBCObjectCache<PostgreDatabase, PostgreTablespace> {

        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull PostgreDatabase owner)
            throws SQLException {
            return session.prepareStatement(
                "SELECT t.oid,t.*" +
                    (owner.getDataSource().getServerType().supportsTeblespaceLocation() ? ",pg_tablespace_location(t.oid) loc" : "") +
                    "\nFROM pg_catalog.pg_tablespace t " +
                    "\nORDER BY t.oid"
            );
        }

        @Override
        protected PostgreTablespace fetchObject(@NotNull JDBCSession session, @NotNull PostgreDatabase owner, @NotNull JDBCResultSet dbResult)
            throws SQLException, DBException {
            return new PostgreTablespace(owner, dbResult);
        }
    }
    
 class AvailableExtensionCache extends JDBCObjectCache<PostgreDatabase, PostgreAvailableExtension> {
        
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
    
    class ExtensionCache extends JDBCObjectCache<PostgreDatabase, PostgreExtension> {

        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull PostgreDatabase owner)
            throws SQLException {
            // dbStat.setLong(1, PostgreSchema.this.getObjectId());
            return session.prepareStatement(
                  "SELECT \n" +
                  " e.oid,\n" +
                  " a.rolname oname,\n" +
                  " cfg.tbls,\n" +
                  "  n.nspname as schema_name,\n" +
                  " e.* \n" +
                  "FROM \n" +
                  " pg_catalog.pg_extension e \n" +
                  " join pg_authid a on a.oid = e.extowner\n" +
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
            DBSObjectFilter catalogFilters = database.getDataSource().getContainer().getObjectFilter(PostgreSchema.class, null, false);
            if ((catalogFilters != null && !catalogFilters.isNotApplicable()) || object != null || objectName != null) {
                if (object != null || objectName != null) {
                    catalogFilters = new DBSObjectFilter();
                    catalogFilters.addInclude(object != null ? object.getName() : objectName);
                } else {
                    catalogFilters = new DBSObjectFilter(catalogFilters);
                    // Always read catalog schema
                    catalogFilters.addInclude(PostgreConstants.CATALOG_SCHEMA_NAME);
                }
                JDBCUtils.appendFilterClause(catalogQuery, catalogFilters, "nspname", true);
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
                return null;
            }
            if (PostgreSchema.isUtilitySchema(name) && !owner.getDataSource().getContainer().getNavigatorSettings().isShowUtilityObjects()) {
                return null;
            }
            return new PostgreSchema(owner, name, resultSet);
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
                Collection<PostgreRole> roles = object.getAuthIds(new VoidProgressMonitor());
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
