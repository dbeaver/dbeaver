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
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.DBPStatefulObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectLookupCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.struct.rdb.DBSCatalog;
import org.jkiss.utils.LongKeyMap;

import java.sql.SQLException;
import java.util.*;

/**
 * PostgreDatabase
 */
public class PostgreDatabase implements DBSInstance, DBSCatalog, DBPRefreshableObject, DBPStatefulObject, PostgreObject, DBSObjectSelector {

    private static final Log log = Log.getLog(PostgreDatabase.class);

    private PostgreDataSource dataSource;
    private long oid;
    private String name;
    private long ownerId;
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

    public PostgreDatabase(PostgreDataSource dataSource, JDBCResultSet dbResult)
        throws SQLException
    {
        this.dataSource = dataSource;
        this.loadInfo(dbResult);
    }

    private void loadInfo(JDBCResultSet dbResult)
        throws SQLException
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

    ///////////////////////////////////////////////////
    // Properties

    @Property(viewable = false, order = 3)
    public PostgreRole getDBA(DBRProgressMonitor monitor) throws DBException {
        return PostgreUtils.getObjectById(monitor, roleCache, this, ownerId);
    }

    @Property(viewable = false, order = 4)
    public PostgreTablespace getDefaultTablespace(DBRProgressMonitor monitor) throws DBException {
        return PostgreUtils.getObjectById(monitor, tablespaceCache, this, tablespaceId);
    }

    @Property(viewable = false, order = 5)
    public PostgreCharset getDefaultEncoding(DBRProgressMonitor monitor) throws DBException {
        return PostgreUtils.getObjectById(monitor, encodingCache, this, encodingId);
    }

    @Property(viewable = false, order = 10)
    public String getCollate() {
        return collate;
    }

    @Property(viewable = false, order = 11)
    public String getCtype() {
        return ctype;
    }

    @Property(viewable = false, order = 12)
    public boolean isTemplate() {
        return isTemplate;
    }

    @Property(viewable = false, order = 13)
    public boolean isAllowConnect() {
        return allowConnect;
    }

    @Property(viewable = false, order = 14)
    public int getConnectionLimit() {
        return connectionLimit;
    }
///////////////////////////////////////////////////
    // Instance methods

    @NotNull
    @Override
    public DBCExecutionContext getDefaultContext(boolean meta) {
        return dataSource.getDefaultContext(meta);
    }

    @NotNull
    @Override
    public DBCExecutionContext[] getAllContexts() {
        return dataSource.getAllContexts();
    }

    @NotNull
    @Override
    public DBCExecutionContext openIsolatedContext(@NotNull DBRProgressMonitor monitor, @NotNull String purpose) throws DBException {
        return dataSource.openIsolatedContext(monitor, purpose);
    }

    @Override
    public void shutdown(DBRProgressMonitor monitor) {

    }

    ///////////////////////////////////////////////
    // Infos

    @Association
    public Collection<PostgreRole> getAuthIds(DBRProgressMonitor monitor) throws DBException {
        return roleCache.getAllObjects(monitor, this);
    }

    @Association
    public Collection<PostgreAccessMethod> getAccessMethods(DBRProgressMonitor monitor) throws DBException {
        return accessMethodCache.getAllObjects(monitor, this);
    }

    @Association
    public Collection<PostgreForeignDataWrapper> getForeignDataWrappers(DBRProgressMonitor monitor) throws DBException {
        return foreignDataWrapperCache.getAllObjects(monitor, this);
    }

    @Association
    public Collection<PostgreForeignServer> getForeignServers(DBRProgressMonitor monitor) throws DBException {
        return foreignServerCache.getAllObjects(monitor, this);
    }

    @Association
    public Collection<PostgreLanguage> getLanguages(DBRProgressMonitor monitor) throws DBException {
        return languageCache.getAllObjects(monitor, this);
    }

    @Association
    public Collection<PostgreCharset> getEncodings(DBRProgressMonitor monitor) throws DBException {
        return encodingCache.getAllObjects(monitor, this);
    }

    @Association
    public Collection<PostgreTablespace> getTablespaces(DBRProgressMonitor monitor) throws DBException {
        return tablespaceCache.getAllObjects(monitor, this);
    }

    ///////////////////////////////////////////////
    // Object container

    @Association
    public Collection<PostgreSchema> getSchemas(DBRProgressMonitor monitor) throws DBException {
        if (this != dataSource.getDefaultInstance()) {
            throw new DBException("Can't access non-default database");
        }
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

    void cacheDataTypes(DBRProgressMonitor monitor) throws DBException {
        dataTypeCache.clear();
        // Cache data types
        for (final PostgreSchema pgSchema : getSchemas(monitor)) {
            pgSchema.getDataTypes(monitor);
        }
    }

    public PostgreSchema getSchema(DBRProgressMonitor monitor, String name) throws DBException {
        if (this != dataSource.getDefaultInstance()) {
            throw new DBException("Can't access non-default database");
        }
        return schemaCache.getObject(monitor, this, name);
    }

    public PostgreSchema getSchema(DBRProgressMonitor monitor, long oid) throws DBException {
        if (this != dataSource.getDefaultInstance()) {
            throw new DBException("Can't access non-default database");
        }
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
        roleCache.clearCache();
        accessMethodCache.clearCache();
        languageCache.clearCache();
        encodingCache.clearCache();
        tablespaceCache.clearCache();
        schemaCache.clearCache();

        cacheDataTypes(monitor);
        return this;
    }

    public Collection<PostgreRole> getUsers(DBRProgressMonitor monitor) throws DBException {
        return roleCache.getAllObjects(monitor, this);
    }

    @Override
    public boolean supportsDefaultChange() {
        return true;
    }

    @Nullable
    @Override
    public PostgreSchema getDefaultObject() {
        return schemaCache.getCachedObject(dataSource.getActiveSchemaName());
    }

    @Override
    public void setDefaultObject(@NotNull DBRProgressMonitor monitor, @NotNull DBSObject object) throws DBException {
        if (object instanceof PostgreSchema) {
            PostgreSchema oldActive = getDefaultObject();
            if (oldActive == object) {
                return;
            }

            for (JDBCExecutionContext context : dataSource.getAllContexts()) {
                setSearchPath(monitor, (PostgreSchema)object, context);
            }
            dataSource.setActiveSchemaName(object.getName());
            dataSource.setSearchPath(object.getName());

            if (oldActive != null) {
                DBUtils.fireObjectSelect(oldActive, false);
            }
            DBUtils.fireObjectSelect(object, true);
        }
    }

    @Override
    public boolean refreshDefaultObject(@NotNull DBCSession session) throws DBException {
        return dataSource.refreshDefaultObject(session);
    }

    void setSearchPath(DBRProgressMonitor monitor, PostgreSchema schema, JDBCExecutionContext context) throws DBCException {
        try (JDBCSession session = context.openSession(monitor, DBCExecutionPurpose.UTIL, "Change search path")) {
            JDBCUtils.executeSQL(session, "SET search_path = \"$user\"," + DBUtils.getQuotedIdentifier(schema));
        } catch (SQLException e) {
            throw new DBCException("Error setting search path", e, dataSource);
        }
    }

    public PostgreProcedure getProcedure(DBRProgressMonitor monitor, long schemaId, long procId)
        throws DBException
    {
        final PostgreSchema schema = getSchema(monitor, schemaId);
        if (schema != null) {
            return PostgreUtils.getObjectById(monitor, schema.proceduresCache, schema, procId);
        }
        return null;
    }

    public PostgreDataType getDataType(long typeId) {
        if (typeId <= 0) {
            return null;
        }
        PostgreDataType dataType = dataTypeCache.get(typeId);
        if (dataType != null) {
            return dataType;
        }
        for (PostgreSchema schema : getDatabase().schemaCache.getCachedObjects()) {
            dataType = schema.dataTypeCache.getDataType(typeId);
            if (dataType != null) {
                dataTypeCache.put(typeId, dataType);
                return dataType;
            }
        }
        log.debug("Data type '" + typeId + "' not found");
        return null;
    }

    public PostgreDataType getDataType(String typeName) {
        if (typeName.endsWith("[]")) {
            // In some cases ResultSetMetadata returns it as []
            typeName = "_" + typeName.substring(0, typeName.length() - 2);
        }
        String alias = PostgreConstants.DATA_TYPE_ALIASES.get(typeName);
        if (alias != null) {
            typeName = alias;
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
        final List<String> searchPath = dataSource.getSearchPath();
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
        log.debug("Data type '" + typeName + "' not found in database '" + getName() + "'");
        return null;
    }

    @Override
    public String toString() {
        return name;
    }

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
                setCache(Collections.<PostgreRole>emptyList());
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
