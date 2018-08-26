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
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.postgresql.PostgreConstants;
import org.jkiss.dbeaver.ext.postgresql.PostgreUtils;
import org.jkiss.dbeaver.model.DBPNamedObject2;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.DBPStatefulObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.DBCSession;
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
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectFilter;
import org.jkiss.dbeaver.model.struct.DBSObjectSelector;
import org.jkiss.dbeaver.model.struct.DBSObjectState;
import org.jkiss.dbeaver.model.struct.rdb.DBSCatalog;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.LongKeyMap;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * PostgreDatabase
 */
public class PostgreDatabase extends JDBCRemoteInstance<PostgreDataSource> implements DBSCatalog, DBPRefreshableObject, DBPStatefulObject, DBPNamedObject2, PostgreObject, DBSObjectSelector {

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

    public final RoleCache roleCache = new RoleCache();
    public final AccessMethodCache accessMethodCache = new AccessMethodCache();
    public final ForeignDataWrapperCache foreignDataWrapperCache = new ForeignDataWrapperCache();
    public final ForeignServerCache foreignServerCache = new ForeignServerCache();
    public final LanguageCache languageCache = new LanguageCache();
    public final EncodingCache encodingCache = new EncodingCache();
    public final TablespaceCache tablespaceCache = new TablespaceCache();
    public final SchemaCache schemaCache = new SchemaCache();
    public final LongKeyMap<PostgreDataType> dataTypeCache = new LongKeyMap<>();

    private String activeSchemaName;
    private final List<String> searchPath = new ArrayList<>();
    private List<String> defaultSearchPath = new ArrayList<>();
    private String activeUser;

    public PostgreDatabase(DBRProgressMonitor monitor, PostgreDataSource dataSource, ResultSet dbResult)
        throws DBException
    {
        super(monitor, dataSource, false);
        this.initCaches();
        this.loadInfo(dbResult);
    }

    private void initCaches() {
/*
        if (!getDataSource().isServerVersionAtLeast(8, 1)) {
            // Roles not supported
            roleCache.setCache(Collections.emptyList());
        }
*/
    }

    public PostgreDatabase(DBRProgressMonitor monitor, PostgreDataSource dataSource, String databaseName)
        throws DBException
    {
        super(monitor, dataSource, false);
        // We need to set name first
        this.name = databaseName;
        this.initCaches();
        checkDatabaseConnection(monitor);

        try (JDBCSession session = getDefaultContext(true).openSession(monitor, DBCExecutionPurpose.META, "Load database info")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement("SELECT db.oid,db.*" +
                    "\nFROM pg_catalog.pg_database db WHERE datname=?")) {
                dbStat.setString(1, databaseName);
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    if (dbResult.nextRow()) {
                        loadInfo(dbResult);
                    }
                }
            }
        } catch (SQLException e) {
            throw new DBCException(e, getDataSource());
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

    public PostgreRole getInitialOwner() {
        return initialOwner;
    }

    public PostgreTablespace getInitialTablespace() {
        return initialTablespace;
    }

    public PostgreCharset getInitialEncoding() {
        return initialEncoding;
    }

    void checkDatabaseConnection(DBRProgressMonitor monitor) throws DBException {
        if (executionContext == null) {
            initializeMainContext(monitor);
            initializeMetaContext(monitor);

            try (JDBCSession session = getDefaultContext(true).openSession(monitor, DBCExecutionPurpose.UTIL, "Detect default schema/user")) {
                determineDefaultObjects(session);
            } catch (SQLException e) {
                throw new DBException(e, getDataSource());
            }
        }
    }

    private void loadInfo(ResultSet dbResult)
    {
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
    public String getName()
    {
        return name;
    }

    @Override
    public void setName(String newName) {
        this.name = newName;
    }

    @Nullable
    @Override
    public String getDescription()
    {
        return null;
    }

    @Override
    public DBSObject getParentObject()
    {
        return dataSource.getContainer();
    }

    @NotNull
    @Override
    public PostgreDataSource getDataSource() {
        return dataSource;
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

    @Property(order = 3)
    public PostgreRole getDBA(DBRProgressMonitor monitor) throws DBException {
        checkDatabaseConnection(monitor);
        return PostgreUtils.getObjectById(monitor, roleCache, this, ownerId);
    }

    @Property(order = 5)
    public PostgreCharset getDefaultEncoding(DBRProgressMonitor monitor) throws DBException {
        checkDatabaseConnection(monitor);
        return PostgreUtils.getObjectById(monitor, encodingCache, this, encodingId);
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
        checkDatabaseConnection(monitor);
        return roleCache.getAllObjects(monitor, this);
    }

    @Association
    public Collection<PostgreAccessMethod> getAccessMethods(DBRProgressMonitor monitor) throws DBException {
        checkDatabaseConnection(monitor);
        return accessMethodCache.getAllObjects(monitor, this);
    }

    @Association
    public Collection<PostgreForeignDataWrapper> getForeignDataWrappers(DBRProgressMonitor monitor) throws DBException {
        checkDatabaseConnection(monitor);
        return foreignDataWrapperCache.getAllObjects(monitor, this);
    }

    @Association
    public Collection<PostgreForeignServer> getForeignServers(DBRProgressMonitor monitor) throws DBException {
        checkDatabaseConnection(monitor);
        return foreignServerCache.getAllObjects(monitor, this);
    }

    @Association
    public Collection<PostgreLanguage> getLanguages(DBRProgressMonitor monitor) throws DBException {
        checkDatabaseConnection(monitor);
        return languageCache.getAllObjects(monitor, this);
    }

    @Association
    public Collection<PostgreCharset> getEncodings(DBRProgressMonitor monitor) throws DBException {
        checkDatabaseConnection(monitor);
        return encodingCache.getAllObjects(monitor, this);
    }

    ///////////////////////////////////////////////
    // Tablespaces

    @Association
    public Collection<PostgreTablespace> getTablespaces(DBRProgressMonitor monitor) throws DBException {
        checkDatabaseConnection(monitor);
        return tablespaceCache.getAllObjects(monitor, this);
    }

    @Property(order = 4)
    public PostgreTablespace getDefaultTablespace(DBRProgressMonitor monitor) throws DBException {
        checkDatabaseConnection(monitor);
        return PostgreUtils.getObjectById(monitor, tablespaceCache, this, tablespaceId);
    }

    public PostgreTablespace getTablespace(DBRProgressMonitor monitor, long tablespaceId) throws DBException {
        checkDatabaseConnection(monitor);
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
        checkDatabaseConnection(monitor);
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

    void cacheDataTypes(DBRProgressMonitor monitor, boolean forceRefresh) throws DBException {
        if (dataTypeCache.isEmpty() || forceRefresh) {
            dataTypeCache.clear();
            // Cache data types
            for (final PostgreSchema pgSchema : getSchemas(monitor)) {
                pgSchema.getDataTypes(monitor);
            }
        }
    }

    public PostgreSchema getSchema(DBRProgressMonitor monitor, String name) throws DBException {
        checkDatabaseConnection(monitor);
        return schemaCache.getObject(monitor, this, name);
    }

    public PostgreSchema getSchema(DBRProgressMonitor monitor, long oid) throws DBException {
        checkDatabaseConnection(monitor);
        for (PostgreSchema schema : schemaCache.getAllObjects(monitor, this)) {
            if (schema.getObjectId() == oid) {
                return schema;
            }
        }
        return null;
    }

    PostgreTableBase findTable(DBRProgressMonitor monitor, long schemaId, long tableId)
        throws DBException
    {
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
        // Refresh all properties
        PostgreDatabase refDatabase = dataSource.getDatabaseCache().refreshObject(monitor, dataSource, this);
        if (refDatabase != null && refDatabase == dataSource.getDefaultInstance()) {
            // Cache types
            refDatabase.cacheDataTypes(monitor, true);
        }
        return refDatabase;
    }

    public Collection<PostgreRole> getUsers(DBRProgressMonitor monitor) throws DBException {
        checkDatabaseConnection(monitor);
        return roleCache.getAllObjects(monitor, this);
    }

    ////////////////////////////////////////////////////
    // Default schema and search path

    public String getActiveUser() {
        return activeUser;
    }

    public String getActiveSchemaName() {
        return activeSchemaName;
    }

    public void setActiveSchemaName(String activeSchemaName) {
        this.activeSchemaName = activeSchemaName;
    }

    public List<String> getSearchPath() {
        return searchPath;
    }

    List<String> getDefaultSearchPath() {
        return defaultSearchPath;
    }

    public void setSearchPath(String path) {
        searchPath.clear();
        searchPath.add(path);
        if (!path.equals(activeUser)) {
            searchPath.add(activeUser);
        }
    }

    private void determineDefaultObjects(JDBCSession session) throws DBCException, SQLException {
        try (JDBCPreparedStatement stat = session.prepareStatement("SELECT current_schema(),session_user")) {
            try (JDBCResultSet rs = stat.executeQuery()) {
                if (rs.nextRow()) {
                    activeSchemaName = JDBCUtils.safeGetString(rs, 1);
                    activeUser = JDBCUtils.safeGetString(rs, 2);
                }
            }
        }
        String searchPathStr = JDBCUtils.queryString(session, "SHOW search_path");
        this.searchPath.clear();
        if (searchPathStr != null) {
            for (String str : searchPathStr.split(",")) {
                str = str.trim();
                this.searchPath.add(DBUtils.getUnQuotedIdentifier(getDataSource(), str));
            }
        } else {
            this.searchPath.add(PostgreConstants.PUBLIC_SCHEMA_NAME);
        }

        defaultSearchPath = new ArrayList<>(searchPath);
    }

    @Override
    public boolean supportsDefaultChange() {
        return true;
    }

    @Nullable
    @Override
    public PostgreSchema getDefaultObject() {
        return schemaCache.getCachedObject(activeSchemaName);
    }
    
    @Override
    public void setDefaultObject(@NotNull DBRProgressMonitor monitor, @NotNull DBSObject object) throws DBException {
        if (object instanceof PostgreSchema) {
            PostgreSchema oldActive = getDefaultObject();
            if (oldActive == object) {
                return;
            }

            for (JDBCExecutionContext context : getAllContexts()) {
                setSearchPath(monitor, (PostgreSchema)object, context);
            }
            activeSchemaName = object.getName();
            setSearchPath(object.getName());

            if (oldActive != null) {
                DBUtils.fireObjectSelect(oldActive, false);
            }
            DBUtils.fireObjectSelect(object, true);
        }
    }

    @Override
    public boolean refreshDefaultObject(@NotNull DBCSession session) throws DBException {
        try {
            String oldDefSchema = activeSchemaName;
            determineDefaultObjects((JDBCSession) session);
            if (activeSchemaName != null && !CommonUtils.equalObjects(oldDefSchema, activeSchemaName)) {
                final PostgreSchema newSchema = getSchema(session.getProgressMonitor(), activeSchemaName);
                if (newSchema != null) {
                    setDefaultObject(session.getProgressMonitor(), newSchema);
                    return true;
                }
            }
            return false;
        } catch (SQLException e) {
            throw new DBException(e, getDataSource());
        }
    }

    void setSearchPath(DBRProgressMonitor monitor, PostgreSchema schema, JDBCExecutionContext context) throws DBCException {
        // Construct search path from current search path but put default schema first
        List<String> newSearchPath = new ArrayList<>(getDefaultSearchPath());
        {
            String defSchemaName = schema.getName();
            int schemaIndex = newSearchPath.indexOf(defSchemaName);
            if (schemaIndex == 0) {
                // Already default schema
            } else {
                if (schemaIndex > 0) {
                    // Remove from previous position
                    newSearchPath.remove(schemaIndex);
                }
                // Add it first
                newSearchPath.add(0, defSchemaName);
            }
        }
        StringBuilder spString = new StringBuilder();
        for (String sp : newSearchPath) {
            if (spString.length() > 0) spString.append(",");
            spString.append(DBUtils.getQuotedIdentifier(getDataSource(), sp));
        }
        try (JDBCSession session = context.openSession(monitor, DBCExecutionPurpose.UTIL, "Change search path")) {
            JDBCUtils.executeSQL(session, "SET search_path = " + spString);
        } catch (SQLException e) {
            throw new DBCException("Error setting search path", e, dataSource);
        }
    }

    /////////////////////////////////////////////////
    // Procedures

    public PostgreProcedure getProcedure(DBRProgressMonitor monitor, long schemaId, long procId)
        throws DBException
    {
        final PostgreSchema schema = getSchema(monitor, schemaId);
        if (schema != null) {
            return PostgreUtils.getObjectById(monitor, schema.proceduresCache, schema, procId);
        }
        return null;
    }

    public PostgreProcedure getProcedure(DBRProgressMonitor monitor, long procId)
        throws DBException
    {
        for (final PostgreSchema schema : getSchemas(monitor)) {
            PostgreProcedure procedure = PostgreUtils.getObjectById(monitor, schema.proceduresCache, schema, procId);
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

    public PostgreDataType getDataType(DBRProgressMonitor monitor, String typeName) {
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

        // Type not found. Let's resolve it
        try {
            PostgreDataType dataType = PostgreDataTypeCache.resolveDataType(monitor, this, typeName);
            dataType.getParentObject().dataTypeCache.cacheObject(dataType);
            return dataType;
        } catch (Exception e) {
            log.debug("Can't resolve data type " + typeName, e);
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
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull PostgreDatabase owner)
            throws SQLException
        {
            return session.prepareStatement(
                "SELECT a.oid,a.* FROM pg_catalog.pg_roles a " +
                    "\nORDER BY a.oid"
            );
        }

        @Override
        protected PostgreRole fetchObject(@NotNull JDBCSession session, @NotNull PostgreDatabase owner, @NotNull JDBCResultSet dbResult)
            throws SQLException, DBException
        {
            return new PostgreRole(owner, dbResult);
        }

        @Override
        protected boolean handleCacheReadError(DBException error) {
            // #271, #501: in some databases (AWS?) pg_authid is not accessible
            // FIXME: maybe some better workaround?
            if (PostgreConstants.EC_PERMISSION_DENIED.equals(error.getDatabaseState())) {
                log.warn(error);
                setCache(Collections.emptyList());
                return true;
            }
            return false;
        }
    }

    class AccessMethodCache extends JDBCObjectCache<PostgreDatabase, PostgreAccessMethod> {

        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull PostgreDatabase owner)
            throws SQLException
        {
            return session.prepareStatement(
                "SELECT am.oid,am.* FROM pg_catalog.pg_am am " +
                    "\nORDER BY am.oid"
            );
        }

        @Override
        protected PostgreAccessMethod fetchObject(@NotNull JDBCSession session, @NotNull PostgreDatabase owner, @NotNull JDBCResultSet dbResult)
            throws SQLException, DBException
        {
            return new PostgreAccessMethod(owner, dbResult);
        }
    }

    class EncodingCache extends JDBCObjectCache<PostgreDatabase, PostgreCharset> {

        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull PostgreDatabase owner)
            throws SQLException
        {
            return session.prepareStatement(
                "SELECT c.contoencoding as encid,pg_catalog.pg_encoding_to_char(c.contoencoding) as encname\n" +
                "FROM pg_catalog.pg_conversion c\n" +
                "GROUP BY c.contoencoding\n" +
                "ORDER BY 2\n"
            );
        }

        @Override
        protected PostgreCharset fetchObject(@NotNull JDBCSession session, @NotNull PostgreDatabase owner, @NotNull JDBCResultSet dbResult)
            throws SQLException, DBException
        {
            return new PostgreCharset(owner, dbResult);
        }
    }

    class LanguageCache extends JDBCObjectCache<PostgreDatabase, PostgreLanguage> {

        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull PostgreDatabase owner)
            throws SQLException
        {
            return session.prepareStatement(
                "SELECT l.oid,l.* FROM pg_catalog.pg_language l " +
                    "\nORDER BY l.oid"
            );
        }

        @Override
        protected PostgreLanguage fetchObject(@NotNull JDBCSession session, @NotNull PostgreDatabase owner, @NotNull JDBCResultSet dbResult)
            throws SQLException, DBException
        {
            return new PostgreLanguage(owner, dbResult);
        }
    }

    class ForeignDataWrapperCache extends JDBCObjectCache<PostgreDatabase, PostgreForeignDataWrapper> {

        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull PostgreDatabase owner)
            throws SQLException
        {
            return session.prepareStatement(
                "SELECT l.oid,l.*,p.pronamespace as handler_schema_id " +
                "\nFROM pg_catalog.pg_foreign_data_wrapper l" +
                "\nLEFT OUTER JOIN pg_catalog.pg_proc p ON p.oid=l.fdwhandler " +
                "\nORDER BY l.fdwname"
            );
        }

        @Override
        protected PostgreForeignDataWrapper fetchObject(@NotNull JDBCSession session, @NotNull PostgreDatabase owner, @NotNull JDBCResultSet dbResult)
            throws SQLException, DBException
        {
            return new PostgreForeignDataWrapper(owner, dbResult);
        }
    }

    class ForeignServerCache extends JDBCObjectCache<PostgreDatabase, PostgreForeignServer> {

        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull PostgreDatabase owner)
            throws SQLException
        {
            return session.prepareStatement(
                "SELECT l.oid,l.* FROM pg_catalog.pg_foreign_server l" +
                "\nORDER BY l.srvname"
            );
        }

        @Override
        protected PostgreForeignServer fetchObject(@NotNull JDBCSession session, @NotNull PostgreDatabase owner, @NotNull JDBCResultSet dbResult)
            throws SQLException, DBException
        {
            return new PostgreForeignServer(owner, dbResult);
        }
    }

    class TablespaceCache extends JDBCObjectCache<PostgreDatabase, PostgreTablespace> {

        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull PostgreDatabase owner)
            throws SQLException
        {
            return session.prepareStatement(
                "SELECT t.oid,t.* FROM pg_catalog.pg_tablespace t " +
                    "\nORDER BY t.oid"
            );
        }

        @Override
        protected PostgreTablespace fetchObject(@NotNull JDBCSession session, @NotNull PostgreDatabase owner, @NotNull JDBCResultSet dbResult)
            throws SQLException, DBException
        {
            return new PostgreTablespace(owner, dbResult);
        }
    }
    
    static class SchemaCache extends JDBCObjectLookupCache<PostgreDatabase, PostgreSchema>
    {
        @NotNull
        @Override
        public JDBCStatement prepareLookupStatement(@NotNull JDBCSession session, @NotNull PostgreDatabase database, @Nullable PostgreSchema object, @Nullable String objectName) throws SQLException
        {
/*
            // Do not apply filters
            // We need all schemas to have access to types
            return session.prepareStatement(
                "SELECT n.oid,n.* FROM pg_catalog.pg_namespace n ORDER BY nspname");
*/
            StringBuilder catalogQuery = new StringBuilder("SELECT n.oid,n.*,d.description FROM pg_catalog.pg_namespace n\n" +
                "LEFT OUTER JOIN pg_catalog.pg_description d ON d.objoid=n.oid\n");
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
        protected PostgreSchema fetchObject(@NotNull JDBCSession session, @NotNull PostgreDatabase owner, @NotNull JDBCResultSet resultSet) throws SQLException, DBException
        {
            String name = JDBCUtils.safeGetString(resultSet, "nspname");
            if (name == null) {
                return null;
            }
            if (PostgreSchema.isUtilitySchema(name) && !owner.getDataSource().getContainer().isShowUtilityObjects()) {
                return null;
            }
            return new PostgreSchema(owner, name, resultSet);
        }
    }
}
