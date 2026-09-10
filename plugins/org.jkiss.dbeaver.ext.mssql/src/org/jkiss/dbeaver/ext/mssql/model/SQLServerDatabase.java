/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.mssql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.mssql.SQLServerConstants;
import org.jkiss.dbeaver.ext.mssql.SQLServerUtils;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectLookupCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.PropertyLength;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.sql.DBSQLException;
import org.jkiss.dbeaver.model.struct.DBSCollationProvider;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectFilter;
import org.jkiss.dbeaver.model.struct.rdb.DBSCatalog;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.LongKeyMap;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;

/**
* SQL Server database
*/
public class SQLServerDatabase
    implements
        DBSCatalog,
        DBPSaveableObject,
        DBPRefreshableObject,
        DBPSystemObject,
        DBPNamedObject2,
        DBPObjectStatistics,
        DBPObjectWithLazyDescription,
        DBSCollationProvider {

    private static final Log log = Log.getLog(SQLServerDatabase.class);

    // uniqueidentifier and xml are deliberately not here: their data kind is STRING/CONTENT
    // but SQL Server rejects COLLATE on them
    private static final Set<String> COLLATABLE_TYPES = Set.of(
        SQLServerConstants.TYPE_CHAR,
        SQLServerConstants.TYPE_NCHAR,
        SQLServerConstants.TYPE_VARCHAR,
        SQLServerConstants.TYPE_NVARCHAR,
        SQLServerConstants.TYPE_TEXT,
        SQLServerConstants.TYPE_NTEXT,
        SQLServerConstants.TYPE_SYSNAME);

    // trailing parts of a collation name which select sensitivity options rather than the language
    private static final Set<String> COLLATION_FLAGS = Set.of(
        "CI", "CS", "AI", "AS", "KS", "WS", "BIN", "BIN2", "SC", "UTF8");

    private final SQLServerDataSource dataSource;
    private final long databaseId;
    private final boolean isTempDatabase;
    private boolean persisted;
    private String name;
    private String description;
    private String collationName;
    private volatile NavigableSet<String> supportedCollations;
    private DataTypeCache typesCache = new DataTypeCache();
    private SchemaCache schemaCache = new SchemaCache();
    private TriggerCache triggerCache = new TriggerCache();

    private Long databaseTotalSize;

    SQLServerDatabase(
        @NotNull JDBCSession session,
        @NotNull SQLServerDataSource dataSource,
        @NotNull JDBCResultSet resultSet,
        @NotNull String name
    ) {
        this.dataSource = dataSource;
        this.databaseId = JDBCUtils.safeGetLong(resultSet, "database_id");
        this.name = name;
        this.isTempDatabase = name.equalsIgnoreCase(SQLServerConstants.TEMPDB_DATABASE);
        this.collationName = JDBCUtils.safeGetString(resultSet, "collation_name");
        //this.description = JDBCUtils.safeGetString(resultSet, "description");

        this.persisted = true;

        if (CommonUtils.equalObjects(
            ((SQLServerExecutionContext) session.getExecutionContext()).getActiveDatabaseName(),
            this.name))
        {
            try {
                getSchemas(session.getProgressMonitor());
            } catch (DBException e) {
                log.debug("Error reading default database schemas", e);
            }
        }
    }

    public SQLServerDatabase(SQLServerDataSource dataSource) {
        this.dataSource = dataSource;
        this.databaseId = 0;
        this.persisted = false;
        this.isTempDatabase = false;
    }

    @NotNull
    @Override
    public SQLServerDataSource getDataSource() {
        return dataSource;
    }

    @NotNull
    @Override
    @Property(viewable = true, editable = true, order = 1)
    public String getName() {
        return name;
    }

    @Override
    public void setName(@NotNull String newName) {
        name = newName;
    }

    @Nullable
    @Override
    public String getDescription() {
        return description;
    }

    @Nullable
    @Override
    @Property(viewable = true, editable = true, updatable = true, length = PropertyLength.MULTILINE, order = 100)
    public String getDescription(@NotNull DBRProgressMonitor monitor) {
        if (description != null) {
            return description;
        }
        // Database-level extended properties live in the database itself, so they cannot be read
        // together with the database list and are not readable at all while the database is not online
        if (isExtendedPropertiesAddressable()) {
            try (JDBCSession session = DBUtils.openUtilSession(monitor, this, "Read database description")) {
                description = JDBCUtils.queryString(
                    session,
                    "SELECT CAST([value] AS nvarchar(max)) FROM " + SQLServerUtils.getExtendedPropsTableName(this) +
                        " WHERE [class] = ? AND [major_id] = 0 AND [minor_id] = 0 AND [name] = ?",
                    SQLServerObjectClass.DATABASE.getClassId(),
                    SQLServerConstants.PROP_MS_DESCRIPTION);
            } catch (Exception e) {
                log.debug("Error reading description of database " + getName(), e);
            }
        }
        if (description == null) {
            description = "";
        }
        return description;
    }

    /**
     * Without cross-database queries the extended properties table cannot be qualified with a catalog name,
     * so it resolves inside the database the connection currently uses - reading it for any other database
     * would report that one's description instead.
     */
    private boolean isExtendedPropertiesAddressable() {
        if (SQLServerUtils.supportsCrossDatabaseQueries(dataSource)) {
            return true;
        }
        if (DBUtils.getDefaultContext(this, true) instanceof SQLServerExecutionContext context) {
            return this == context.getDefaultCatalog();
        }
        return false;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getDatabaseId() {
        return databaseId;
    }

    @Override
    public DBSObject getParentObject() {
        return dataSource;
    }

    @Override
    public boolean isPersisted() {
        return this.persisted;
    }

    @Override
    public void setPersisted(boolean persisted) {
        this.persisted = persisted;
    }

    @Override
    public boolean isSystem() {
        SQLServerDatabase defaultDatabase = dataSource.getDefaultDatabase(new VoidProgressMonitor());
        return ArrayUtils.contains(SQLServerConstants.SYSTEM_DATABASES, name)
            && !CommonUtils.equalObjects(this, defaultDatabase);
    }

    public DataTypeCache getDataTypesCache() {
        return typesCache;
    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) {
        typesCache.clearCache();
        schemaCache.clearCache();
        triggerCache.clearCache();
        databaseTotalSize = null;
        description = null;
        supportedCollations = null;

        return this;
    }

    void refreshDataTypes() {
        typesCache.clearCache();
    }

    /**
     * Whether this database represents the {@code tempdb} database.
     */
    public boolean isTempDatabase() {
        return isTempDatabase;
    }

    //////////////////////////////////////////////////
    // Collations

    @Nullable
    @Override
    @Property(viewable = true, order = 20)
    public String getDefaultCollation() {
        return collationName;
    }

    @Override
    public boolean isCollatableType(@NotNull String typeName) {
        return COLLATABLE_TYPES.contains(typeName.toLowerCase(Locale.ENGLISH));
    }

    @Override
    public boolean isCollationSupported(@NotNull DBRProgressMonitor monitor, @NotNull String collationName) {
        NavigableSet<String> collations = getSupportedCollations(monitor);
        // an unreadable list must not reject anything
        return collations.isEmpty() || collations.contains(collationName);
    }

    @NotNull
    @Override
    public List<String> getRelatedCollations(@NotNull DBRProgressMonitor monitor, @Nullable String collationName) {
        String family = getCollationFamily(collationName);
        if (family == null) {
            return List.of();
        }
        List<String> related = new ArrayList<>();
        for (String collation : getSupportedCollations(monitor)) {
            if (collation.regionMatches(true, 0, family, 0, family.length())) {
                related.add(collation);
            }
        }
        return related;
    }

    @NotNull
    private NavigableSet<String> getSupportedCollations(@NotNull DBRProgressMonitor monitor) {
        if (supportedCollations == null) {
            supportedCollations = readSupportedCollations(monitor);
        }
        return supportedCollations;
    }

    @NotNull
    private NavigableSet<String> readSupportedCollations(@NotNull DBRProgressMonitor monitor) {
        NavigableSet<String> collations = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Read supported collations")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT name FROM sys.fn_helpcollations() ORDER BY name")) {
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    while (dbResult.next()) {
                        String collation = JDBCUtils.safeGetString(dbResult, "name");
                        if (!CommonUtils.isEmpty(collation)) {
                            collations.add(collation);
                        }
                    }
                }
            }
        } catch (Exception e) {
            // not every SQL Server flavor exposes fn_helpcollations, e.g. Synapse.
            // an empty set means "unknown" and disables validation, refreshObject retries the read
            log.debug("Can't read supported collations", e);
        }
        return collations;
    }

    /**
     * Name prefix shared by collations which differ only in sensitivity flags,
     * e.g. Danish_Norwegian_CI_AS -> Danish_Norwegian_. Null if no flags can be stripped.
     */
    @Nullable
    private static String getCollationFamily(@Nullable String collation) {
        if (CommonUtils.isEmpty(collation)) {
            return null;
        }
        String[] parts = collation.split("_");
        int end = parts.length;
        while (end > 1 && COLLATION_FLAGS.contains(parts[end - 1].toUpperCase(Locale.ENGLISH))) {
            end--;
        }
        if (end == parts.length) {
            return null;
        }
        return String.join("_", Arrays.copyOfRange(parts, 0, end)) + "_";
    }

    //////////////////////////////////////////////////
    // Data types

    @Association
    public Collection<SQLServerDataType> getDataTypes(DBRProgressMonitor monitor) throws DBException {
        return typesCache.getAllObjects(monitor, this);
    }

    public SQLServerDataType getDataType(DBRProgressMonitor monitor, String typeName) throws DBException {
        return typesCache.getObject(monitor, this, typeName);
    }

    SQLServerDataType getDataTypeByUserTypeId(DBRProgressMonitor monitor, int typeID) throws DBException {
        try {
            typesCache.getAllObjects(monitor, this);

            SQLServerDataType dataType = typesCache.getDataType(typeID);
            if (dataType != null) {
                return dataType;
            }
        } catch (DBException e) {
            log.error("Error reading database data types", e);
        }

        SQLServerDataType dataType = dataSource.getSystemDataType(typeID);
        if (dataType != null) {
            return dataType;
        }
        log.debug("Data type '" + typeID + "' not found in database " + getName());
        return null;
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public boolean hasStatistics() {
        return databaseTotalSize != null;
    }

    @Override
    public long getStatObjectSize() {
        return databaseTotalSize == null ? 0 : databaseTotalSize;
    }

    void setDatabaseTotalSize(long databaseTotalSize) {
        this.databaseTotalSize = databaseTotalSize;
    }

    ///////////////////////////////////////////////////////
    // Caches

    private class DataTypeCache extends JDBCObjectCache<SQLServerDatabase, SQLServerDataType> {

        private final LongKeyMap<SQLServerDataType> dataTypeMap = new LongKeyMap<>();
        
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull SQLServerDatabase database) throws SQLException {
            String statement;
            if (database.getDataSource().isSynapseDatabase()) {
                // sys.table_types is supported only for SQL Server and Azure SQL Database, not for Azure Synapse.
                statement = "SELECT * FROM " + SQLServerUtils.getSystemTableName(database, "types") + " WHERE is_user_defined = 1";
            } else {
                statement = "SELECT ss.*, tt.type_table_object_id,tto.schema_id as type_table_schema_id\n" +
                    "FROM " + SQLServerUtils.getSystemTableName(database, "types") + " ss\n" +
                    "LEFT JOIN " + SQLServerUtils.getSystemTableName(database, "table_types") + " tt ON " +
                        "ss.name = tt.name AND ss.user_type_id = tt.user_type_id\n" +
                    "LEFT OUTER JOIN " + SQLServerUtils.getSystemTableName(database, "objects") + " tto ON " +
                        "tto.object_id = tt.type_table_object_id\n" +
                    "WHERE ss.is_user_defined = 1";
            }
            return session.prepareStatement(statement);
        }

        @Override
        protected SQLServerDataType fetchObject(@NotNull JDBCSession session, @NotNull SQLServerDatabase database, @NotNull JDBCResultSet resultSet) {
            return new SQLServerDataType(database, resultSet);
        }

        SQLServerDataType getDataType(long typeID) {
            return dataTypeMap.get(typeID);
        }

        @Override
        public void clearCache() {
            super.clearCache();
            dataTypeMap.clear();
        }

        @Override
        public void removeObject(@NotNull SQLServerDataType object, boolean resetFullCache) {
            super.removeObject(object, resetFullCache);
            dataTypeMap.remove(object.getObjectId());
        }

        @Override
        public void cacheObject(@NotNull SQLServerDataType object) {
            super.cacheObject(object);
            dataTypeMap.put(object.getObjectId(), object);
        }

        @Override
        public void setCache(@NotNull List<SQLServerDataType> cache) {
            super.setCache(cache);
            for (SQLServerDataType dt : cache) {
                dataTypeMap.put(dt.getObjectId(), dt);
            }
        }

    }

    //////////////////////////////////////////////////
    // Schemas

    @Association
    public Collection<SQLServerSchema> getSchemas(DBRProgressMonitor monitor) throws DBException {
        return getChildren(monitor);
    }

    public SQLServerSchema getSchema(DBRProgressMonitor monitor, long schemaId) throws DBException {
        for (SQLServerSchema schema : getSchemas(monitor)) {
            if (schema.getObjectId() == schemaId) {
                return schema;
            }
        }
        if (!monitor.isCanceled()) {
            log.debug("Schema '" + schemaId + "' not found");
        }
        return null;
    }

    public SQLServerSchema getSchema(DBRProgressMonitor monitor, String name) throws DBException {
        return schemaCache.getObject(monitor, this, name);
    }

    public SQLServerSchema getSchema(String name) {
        return schemaCache.getCachedObject(name);
    }

    public SQLServerSchema getSysSchema(DBRProgressMonitor monitor) throws DBException {
        for (SQLServerSchema schema : getSchemas(monitor)) {
            if (schema.getName().equalsIgnoreCase("sys")) {
                return schema;
            }
        }
        if (!monitor.isCanceled()) {
            log.debug("System schema not found");
        }
        return null;
    }

    @Nullable
    @Override
    public Collection<SQLServerSchema> getChildren(@NotNull DBRProgressMonitor monitor) throws DBException {
        try {
            return schemaCache.getAllObjects(monitor, this);
        } catch (DBSQLException exception) {
            throw SQLServerUtils.mapException(exception);
        }
    }

    @Override
    public DBSObject getChild(@NotNull DBRProgressMonitor monitor, @NotNull String childName) throws DBException {
        return schemaCache.getObject(monitor, this, childName);
    }

    @NotNull
    @Override
    public Class<? extends DBSObject> getPrimaryChildType(@Nullable DBRProgressMonitor monitor) {
        return SQLServerSchema.class;
    }

    @Override
    public void cacheStructure(@NotNull DBRProgressMonitor monitor, int scope) throws DBException {
        schemaCache.getAllObjects(monitor, this);
    }

    static class SchemaCache extends JDBCObjectCache<SQLServerDatabase, SQLServerSchema> {
        SchemaCache() {
            setListOrderComparator(DBUtils.nameComparatorIgnoreCase());
        }

        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull SQLServerDatabase owner) throws SQLException {
            SQLServerDataSource dataSource = owner.getDataSource();
            boolean showAllSchemas = SQLServerUtils.isShowAllSchemas(dataSource);

            String sysSchema = SQLServerUtils.getSystemSchemaFQN(dataSource, owner.getName(), SQLServerConstants.SQL_SERVER_SYSTEM_SCHEMA);
            StringBuilder sql = new StringBuilder();
            sql.append("SELECT ");
            if (!showAllSchemas) {
                sql.append("DISTINCT ");
            }
            sql.append("s.*,ep.value as description FROM ");
            if (SQLServerUtils.isDriverBabelfish(dataSource.getContainer().getDriver())) {
                sql.append("(SELECT CAST(ext.orig_name AS sysname) AS name, base.oid AS schema_id, base.nspowner AS principal_id FROM pg_namespace base JOIN babelfish_namespace_ext ext ON base.nspname = ext.nspname JOIN babelfish_sysdatabases dbs ON dbs.dbid = ext.dbid WHERE dbs.name = '" + DBUtils.getQuotedIdentifier(dataSource, owner.getName()) + "') AS s");
            }
            else {
                sql.append(sysSchema).append(".schemas s");
            }
            sql.append("\nLEFT OUTER JOIN ").append(SQLServerUtils.getExtendedPropsTableName(owner)).append(" ep ON ep.class=").append(SQLServerObjectClass.SCHEMA.getClassId())
                .append(" AND ep.major_id=s.schema_id AND ep.minor_id=0 AND ep.name='").append(SQLServerConstants.PROP_MS_DESCRIPTION).append("'");
            if (!showAllSchemas) {
                sql.append("\nINNER JOIN ").append(sysSchema).append(".");
                if (dataSource.isServerVersionAtLeast(SQLServerConstants.SQL_SERVER_2008_VERSION_MAJOR, 0)) {
                    sql.append("all_objects o ").append("ON s.schema_id=o.schema_id");
                } else {
                    sql.append("sysobjects o ").append("ON s.schema_id=o.uid");
                }
            }
            final DBSObjectFilter schemaFilters = dataSource.getContainer().getObjectFilter(SQLServerSchema.class, owner, false);
            if (schemaFilters != null && schemaFilters.isEnabled()) {
                sql.append("\n");
                JDBCUtils.appendFilterClause(sql, schemaFilters, "s.name", true, owner.getDataSource());
            }

            JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString());
            if (schemaFilters != null) {
                JDBCUtils.setFilterParameters(dbStat, 1, schemaFilters);
            }

            return dbStat;
        }

        @Override
        protected SQLServerSchema fetchObject(@NotNull JDBCSession session, @NotNull SQLServerDatabase owner, @NotNull JDBCResultSet resultSet) {
            return new SQLServerSchema(owner, resultSet);
        }

    }

    //////////////////////////////////////////////////
    // Triggers

    @Association
    public Collection<SQLServerDatabaseTrigger> getTriggers(DBRProgressMonitor monitor) throws DBException {
        try {
            return triggerCache.getAllObjects(monitor, this);
        } catch (DBSQLException exception) {
            throw SQLServerUtils.mapException(exception);
        }
    }

    TriggerCache getTriggerCache() {
        return triggerCache;
    }

    class TriggerCache extends JDBCObjectLookupCache<SQLServerDatabase, SQLServerDatabaseTrigger> {

        @NotNull
        @Override
        public JDBCStatement prepareLookupStatement(@NotNull JDBCSession session, @NotNull SQLServerDatabase database, @Nullable SQLServerDatabaseTrigger object, @Nullable String objectName) throws SQLException {
            StringBuilder sql = new StringBuilder(500);
            sql.append(
                "SELECT t.* FROM \n")
                .append(SQLServerUtils.getSystemTableName(database, "triggers")).append(" t");
            sql.append("\nWHERE t.parent_id=0");
            if (object != null || objectName != null) {
                sql.append(" AND t.name=?");
            }
            sql.append("\nORDER BY t.name");

            JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString());
            if (object != null || objectName != null) {
                dbStat.setString(1, object != null ? object.getName() : objectName);
            }
            return dbStat;
        }

        @Override
        protected SQLServerDatabaseTrigger fetchObject(@NotNull JDBCSession session, @NotNull SQLServerDatabase database, @NotNull JDBCResultSet resultSet) {
            return new SQLServerDatabaseTrigger(database, resultSet);
        }

    }

}
