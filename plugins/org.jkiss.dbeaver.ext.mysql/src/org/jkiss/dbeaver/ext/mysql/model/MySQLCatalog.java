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
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.ext.mysql.MySQLConstants;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCConstants;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCCompositeCache;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectLookupCache;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructLookupCache;
import org.jkiss.dbeaver.model.meta.*;
import org.jkiss.dbeaver.model.preferences.DBPPropertySource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectFilter;
import org.jkiss.dbeaver.model.struct.rdb.DBSCatalog;
import org.jkiss.dbeaver.model.struct.rdb.DBSIndexType;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureParameterKind;
import org.jkiss.utils.ByteNumberFormat;
import org.jkiss.utils.CommonUtils;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * MySQLCatalog
 */
public class MySQLCatalog implements
    DBSCatalog, DBPSaveableObject, DBPRefreshableObject, DBPSystemObject,
    DBSProcedureContainer, DBPObjectStatisticsCollector, DBPObjectStatistics,
    DBPScriptObject, DBPScriptObjectExt2
{

    private final TableCache tableCache = new TableCache() {
        protected void detectCaseSensitivity(DBSObject object) {
            this.setCaseSensitive(!getDataSource().getSQLDialect().useCaseInsensitiveNameLookup());
        }
    };
    private final ProceduresCache proceduresCache = new ProceduresCache();
    private final PackageCache packageCache = new PackageCache();
    final TriggerCache triggerCache = new TriggerCache();
    final UniqueKeyCache uniqueKeyCache = new UniqueKeyCache(tableCache);
    final CheckConstraintCache checkConstraintCache = new CheckConstraintCache(tableCache);
    final IndexCache indexCache = new IndexCache(tableCache);
    private final EventCache eventCache = new EventCache();
    private final SequenceCache sequenceCache = new SequenceCache();

    private final MySQLDataSource dataSource;
    private String name;
    private Long databaseSize;
    private boolean persisted;
    private volatile boolean hasStatistics;
    private long dbSize;

    private transient String databaseDDL;

    public static class AdditionalInfo {
        private volatile boolean loaded = false;
        private MySQLCharset defaultCharset;
        private MySQLCollation defaultCollation;
        private String sqlPath;

        @Property(viewable = true, editable = true, updatable = true, listProvider = CharsetListProvider.class, order = 2)
        public MySQLCharset getDefaultCharset()
        {
            return defaultCharset;
        }

        public void setDefaultCharset(MySQLCharset defaultCharset)
        {
            this.defaultCharset = defaultCharset;
        }

        @Property(viewable = true, editable = true, updatable = true, listProvider = CollationListProvider.class, order = 3)
        public MySQLCollation getDefaultCollation()
        {
            return defaultCollation;
        }

        public void setDefaultCollation(MySQLCollation defaultCollation)
        {
            this.defaultCollation = defaultCollation;
        }

        @Property(viewable = true, order = 4)
        public String getSqlPath()
        {
            return sqlPath;
        }

        void setSqlPath(String sqlPath)
        {
            this.sqlPath = sqlPath;
        }

    }

    public static class AdditionalInfoValidator implements IPropertyCacheValidator<MySQLCatalog> {
        @Override
        public boolean isPropertyCached(MySQLCatalog object, Object propertyId)
        {
            return object.additionalInfo.loaded;
        }
    }

    private final AdditionalInfo additionalInfo = new AdditionalInfo();

    @PropertyGroup()
    @LazyProperty(cacheValidator = AdditionalInfoValidator.class)
    public AdditionalInfo getAdditionalInfo(DBRProgressMonitor monitor) throws DBCException {
        synchronized (additionalInfo) {
            if (!additionalInfo.loaded) {
                loadAdditionalInfo(monitor);
            }
            return additionalInfo;
        }
    }

    // for internal use only
    public AdditionalInfo getAdditionalInfo() {
        return additionalInfo;
    }

    private void loadAdditionalInfo(DBRProgressMonitor monitor) throws DBCException
    {
        if (!isPersisted()) {
            additionalInfo.loaded = true;
            return;
        }
        MySQLDataSource dataSource = getDataSource();
        if (!getDataSource().supportsInformationSchema()) {
            additionalInfo.loaded = false;
            return;
        }
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load table status")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT * FROM " + MySQLConstants.INFO_SCHEMA_NAME + ".SCHEMATA WHERE SCHEMA_NAME=?")) {
                dbStat.setString(1, getName());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    if (dbResult.next()) {
                        additionalInfo.defaultCharset = dataSource.getCharset(JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_DEFAULT_CHARACTER_SET_NAME));
                        additionalInfo.defaultCollation = dataSource.getCollation(JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_DEFAULT_COLLATION_NAME));
                        additionalInfo.sqlPath = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_SQL_PATH);
                    }
                    additionalInfo.loaded = true;
                }
            } catch (SQLException e) {
                throw new DBCException(e, session.getExecutionContext());
            }
        }
    }

    public MySQLCatalog(MySQLDataSource dataSource, ResultSet dbResult)
    {
        tableCache.setCaseSensitive(false);
        this.dataSource = dataSource;
        if (dbResult != null) {
            this.name = JDBCUtils.safeGetString(dbResult, 1);
            persisted = true;
        } else {
            this.additionalInfo.loaded = true;
            this.additionalInfo.defaultCharset = dataSource.getCharset("utf8");
            this.additionalInfo.defaultCollation = dataSource.getCollation("utf8_general_ci");
            this.additionalInfo.sqlPath = "";
            persisted = false;
        }
    }

    @Override
    public boolean hasStatistics() {
        return true;
    }

    @Override
    public long getStatObjectSize() {
        return dbSize;
    }

    @Nullable
    @Override
    public DBPPropertySource getStatProperties() {
        return null;
    }

    void setDatabaseSize(long dbSize) {
        this.dbSize = dbSize;
    }

    @Override
    public DBSObject getParentObject()
    {
        return dataSource.getContainer();
    }

    @NotNull
    @Override
    public MySQLDataSource getDataSource()
    {
        return dataSource;
    }

    @NotNull
    @Override
    @Property(viewable = true, editable = true, order = 1)
    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    @Override
    public boolean isPersisted()
    {
        return persisted;
    }

    @Override
    public void setPersisted(boolean persisted)
    {
        this.persisted = persisted;
    }

    @Nullable
    @Override
    public String getDescription()
    {
        return null;
    }

    @Property(viewable = true, order = 20, formatter = ByteNumberFormat.class)
    public Long getDatabaseSize(DBRProgressMonitor monitor) throws DBException {
        if (databaseSize == null && getDataSource().supportsInformationSchema()) {
            try (JDBCSession session = DBUtils.openUtilSession(monitor, this, "Read database size")) {
                try (JDBCPreparedStatement dbStat = session.prepareStatement(
                    "SELECT SUM((DATA_LENGTH+INDEX_LENGTH))\n" +
                    "FROM INFORMATION_SCHEMA.TABLES \n" +
                    "WHERE TABLE_SCHEMA=?"))
                {
                    dbStat.setString(1, getName());
                    try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                        if (dbResult.next()) {
                            databaseSize = dbResult.getLong(1);
                        } else {
                            databaseSize = 0L;
                        }
                    }
                }
            } catch (SQLException e) {
                throw new DBDatabaseException(e, getDataSource());
            }
        }
        return databaseSize;
    }

    public TableCache getTableCache()
    {
        return tableCache;
    }

    public ProceduresCache getProceduresCache()
    {
        return proceduresCache;
    }

    public TriggerCache getTriggerCache()
    {
        return triggerCache;
    }

    public UniqueKeyCache getUniqueKeyCache()
    {
        return uniqueKeyCache;
    }

    public CheckConstraintCache getCheckConstraintCache()
    {
        return checkConstraintCache;
    }

    public IndexCache getIndexCache()
    {
        return indexCache;
    }

    public EventCache getEventCache() {
        return eventCache;
    }

    public SequenceCache getSequenceCache() {
        return sequenceCache;
    }

    @Association
    public Collection<MySQLTableIndex> getIndexes(DBRProgressMonitor monitor) throws DBException {
        return getDataSource().supportsInformationSchema() ?
                indexCache.getObjects(monitor, this, null) :
                Collections.emptyList();
    }

    @Association
    public Collection<MySQLTable> getTables(DBRProgressMonitor monitor) throws DBException {
        List<MySQLTable> tables = getTableCache().getTypedObjects(monitor, this, MySQLTable.class);
        if (getDataSource().readKeysWithColumns()) {
            // Read constraints with columns
            uniqueKeyCache.getAllObjects(monitor, this);
        }
        return tables;
    }

    public MySQLTable getTable(DBRProgressMonitor monitor, String name)
        throws DBException
    {
        return getTableCache().getObject(monitor, this, name, MySQLTable.class);
    }

    @Association
    public Collection<MySQLView> getViews(DBRProgressMonitor monitor)
        throws DBException
    {
        return getTableCache().getTypedObjects(monitor, this, MySQLView.class);
    }

    @Override
    @Association
    public Collection<MySQLProcedure> getProcedures(DBRProgressMonitor monitor) throws DBException {
        return getDataSource().supportsInformationSchema() ?
                getProceduresCache().getAllObjects(monitor, this) :
                Collections.emptyList();
    }

    @Override
    public MySQLProcedure getProcedure(DBRProgressMonitor monitor, String procName)
        throws DBException
    {
        return getProceduresCache().getObject(monitor, this, procName);
    }

    @Association
    public Collection<MySQLPackage> getPackages(DBRProgressMonitor monitor)
        throws DBException
    {
        return monitor == null ? packageCache.getCachedObjects() : packageCache.getAllObjects(monitor, this);
    }

    @Association
    public Collection<MySQLTrigger> getTriggers(DBRProgressMonitor monitor) throws DBException {
        return getDataSource().supportsInformationSchema() ?
            (monitor == null ? triggerCache.getCachedObjects() : triggerCache.getAllObjects(monitor, this)) :
                Collections.emptyList();
    }

    public MySQLTrigger getTrigger(DBRProgressMonitor monitor, String name)
        throws DBException
    {
        return triggerCache.getObject(monitor, this, name);
    }

    @Association
    public Collection<MySQLEvent> getEvents(DBRProgressMonitor monitor) throws DBException {
        return getDataSource().supportsInformationSchema() ?
            (monitor == null ? eventCache.getCachedObjects() : eventCache.getAllObjects(monitor, this)) :
                Collections.emptyList();
    }

    @Association
    public Collection<MySQLSequence> getSequences(DBRProgressMonitor monitor) throws DBException {
        return getDataSource().supportsInformationSchema() ?
            (monitor == null ? sequenceCache.getCachedObjects() : sequenceCache.getAllObjects(monitor, this)) :
                Collections.emptyList();
    }

    @Override
    public Collection<MySQLTableBase> getChildren(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
        return tableCache.getAllObjects(monitor, this);
    }

    @Override
    public MySQLTableBase getChild(@NotNull DBRProgressMonitor monitor, @NotNull String childName)
        throws DBException
    {
        return getTableCache().getObject(monitor, this, childName);
    }

    @NotNull
    @Override
    public Class<? extends DBSEntity> getPrimaryChildType(@Nullable DBRProgressMonitor monitor)
        throws DBException
    {
        return MySQLTable.class;
    }

    @Override
    public synchronized void cacheStructure(@NotNull DBRProgressMonitor monitor, int scope)
        throws DBException
    {
        monitor.subTask("Cache tables");
        getTableCache().getAllObjects(monitor, this);
        if ((scope & STRUCT_ATTRIBUTES) != 0) {
            monitor.subTask("Cache table columns");
            getTableCache().loadChildren(monitor, this, null);
        }
        if ((scope & STRUCT_ASSOCIATIONS) != 0) {
            monitor.subTask("Cache table constraints");
            uniqueKeyCache.getAllObjects(monitor, this);
            if (getDataSource().supportsCheckConstraints()) {
                checkConstraintCache.getAllObjects(monitor, this);
            }
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
        if (isSystem()) {
            hasStatistics = true;
            return;
        }
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load table status")) {
            try (JDBCStatement dbStat = session.createStatement()) {
                try (JDBCResultSet dbResult = dbStat.executeQuery("SHOW TABLE STATUS FROM " + DBUtils.getQuotedIdentifier(this))) {
                    while (dbResult.next()) {
                        String tableName = dbResult.getString("Name");
                        MySQLTableBase table = getTableCache().getObject(monitor, this, tableName);
                        if (table instanceof MySQLTable) {
                            ((MySQLTable) table).fetchAdditionalInfo(dbResult);
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

    @Override
    public boolean supportsObjectDefinitionOption(String option) {
        return OPTION_INCLUDE_NESTED_OBJECTS.equals(option);
    }

    @Override
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
        if (databaseDDL == null) {
            try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load database DDL")) {
                try (JDBCStatement dbStat = session.createStatement()) {
                    try (JDBCResultSet dbResult = dbStat.executeQuery("SHOW CREATE DATABASE " + DBUtils.getQuotedIdentifier(this))) {
                        if (dbResult.nextRow()) {
                            databaseDDL = JDBCUtils.safeGetString(dbResult, "Create Database");
                        } else {
                            databaseDDL = "-- Database definition is not available";
                        }
                    }
                }
            } catch (SQLException e) {
                throw new DBException("Error reading database DDL", e);
            }
        }

        if (CommonUtils.getOption(options, OPTION_INCLUDE_NESTED_OBJECTS)) {

        }
        return databaseDDL;
    }

    @Override
    public synchronized DBSObject refreshObject(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
        hasStatistics = false;
        databaseDDL = null;
        getTableCache().clearCache();
        indexCache.clearCache();
        uniqueKeyCache.clearCache();
        if (getDataSource().supportsCheckConstraints()) {
            checkConstraintCache.clearCache();
        }
        getProceduresCache().clearCache();
        triggerCache.clearCache();
        eventCache.clearCache();
        sequenceCache.clearCache();
        return this;
    }

    @Override
    public boolean isSystem() {
        return getDataSource().isSystemCatalog(getName());
    }

    @Override
    public String toString()
    {
        return name + " [" + dataSource.getContainer().getName() + "]";
    }

    public static class TableCache extends JDBCStructLookupCache<MySQLCatalog, MySQLTableBase, MySQLTableColumn> {

        public TableCache()
        {
            super(JDBCConstants.TABLE_NAME);
        }

        @NotNull
        @Override
        public JDBCStatement prepareLookupStatement(
            @NotNull JDBCSession session,
            @NotNull MySQLCatalog owner,
            @Nullable MySQLTableBase object,
            @Nullable String objectName
        ) throws SQLException {
            StringBuilder sql = new StringBuilder("SHOW ");
            MySQLDataSource dataSource = owner.getDataSource();
            if (session.getMetaData().getDatabaseMajorVersion() > 4) {
                sql.append("FULL ");
            }
            sql.append("TABLES FROM ").append(DBUtils.getQuotedIdentifier(owner));
            if (!session.getDataSource().getContainer().getPreferenceStore().getBoolean(ModelPreferences.META_USE_SERVER_SIDE_FILTERS)) {
                // Client side filter
                if (object != null || objectName != null) {
                    appendTableNameCondition(session, object, objectName, sql, true);
                }
            } else {
                String tableNameCol = DBUtils.getQuotedIdentifier(dataSource, "Tables_in_" + owner.getName());
                if (object != null || objectName != null) {
                    sql.append(" WHERE ").append(tableNameCol);
                    appendTableNameCondition(session, object, objectName, sql, false);
                    if (dataSource.supportsSequences()) {
                        sql.append(" AND Table_type <> 'SEQUENCE'");
                    }
                } else {
                    DBSObjectFilter tableFilters = dataSource.getContainer().getObjectFilter(MySQLTable.class, owner, true);
                    if (tableFilters != null && !tableFilters.isNotApplicable()) {
                        sql.append(" WHERE ");
                        if (!CommonUtils.isEmpty(tableFilters.getInclude())) {
                            sql.append("(");
                            boolean hasCond = false;
                            for (String incName : tableFilters.getInclude()) {
                                if (hasCond) sql.append(" OR ");
                                hasCond = true;
                                sql.append(tableNameCol).append(" LIKE ").append(SQLUtils.quoteString(session.getDataSource(), SQLUtils.makeSQLLike(incName)));
                            }
                            sql.append(")");
                        }
                        if (!CommonUtils.isEmpty(tableFilters.getExclude())) {
                            if (!CommonUtils.isEmpty(tableFilters.getInclude())) {
                                sql.append(" AND ");
                            }
                            sql.append("(");
                            boolean hasCond = false;
                            for (String incName : tableFilters.getExclude()) {
                                if (hasCond) sql.append(" OR ");
                                hasCond = true;
                                sql.append(tableNameCol).append(" NOT LIKE ").append(SQLUtils.quoteString(session.getDataSource(), incName));
                            }
                            sql.append(")");
                        }
                    } else if (dataSource.supportsSequences()) {
                        sql.append(" WHERE Table_type <> 'SEQUENCE'");
                    }
                }
            }

            return session.prepareStatement(sql.toString());
        }

        private static void appendTableNameCondition(@NotNull JDBCSession session, @Nullable MySQLTableBase object, @Nullable String objectName, StringBuilder sql, boolean forceUseLike) {
            if (forceUseLike || objectName != null && SQLUtils.isLikePattern(objectName)) {
                sql.append(" LIKE ");
            } else {
                sql.append(" = ");
            }
            sql.append(SQLUtils.quoteString(session.getDataSource(), object != null ? object.getName() : objectName));
        }

        @Override
        protected MySQLTableBase fetchObject(@NotNull JDBCSession session, @NotNull MySQLCatalog owner, @NotNull JDBCResultSet dbResult)
            throws SQLException, DBException
        {
            final String tableType = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_TABLE_TYPE);
            if (tableType != null && tableType.contains("VIEW")) {
                return new MySQLView(owner, dbResult);
            } else {
                return new MySQLTable(owner, dbResult);
            }
        }

        @Override
        protected JDBCStatement prepareChildrenStatement(@NotNull JDBCSession session, @NotNull MySQLCatalog owner, @Nullable MySQLTableBase forTable)
            throws SQLException
        {
            StringBuilder sql = new StringBuilder();
            sql
                .append("SELECT * FROM ").append(MySQLConstants.META_TABLE_COLUMNS)
                .append(" WHERE ").append(MySQLConstants.COL_TABLE_SCHEMA).append("=?");
            if (forTable != null) {
                sql.append(" AND ").append(MySQLConstants.COL_TABLE_NAME).append("=?");
            }
            sql.append(" ORDER BY ").append(MySQLConstants.COL_ORDINAL_POSITION);

            JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString());
            dbStat.setString(1, owner.getName());
            if (forTable != null) {
                dbStat.setString(2, forTable.getName());
            }
            return dbStat;
        }

        @Override
        protected MySQLTableColumn fetchChild(@NotNull JDBCSession session, @NotNull MySQLCatalog owner, @NotNull MySQLTableBase table, @NotNull JDBCResultSet dbResult)
            throws SQLException, DBException
        {
            return new MySQLTableColumn(table, dbResult);
        }

    }

    /**
     * Index cache implementation
     */
    static class IndexCache extends JDBCCompositeCache<MySQLCatalog, MySQLTable, MySQLTableIndex, MySQLTableIndexColumn> {
        IndexCache(TableCache tableCache)
        {
            super(tableCache, MySQLTable.class, MySQLConstants.COL_TABLE_NAME, MySQLConstants.COL_INDEX_NAME);
        }

        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(JDBCSession session, MySQLCatalog owner, MySQLTable forTable)
            throws SQLException
        {
            StringBuilder sql = new StringBuilder();
            sql
                .append("SELECT * FROM ").append(MySQLConstants.META_TABLE_STATISTICS)
                .append(" WHERE ").append(MySQLConstants.COL_TABLE_SCHEMA).append("=?");
            if (forTable != null) {
                sql.append(" AND ").append(MySQLConstants.COL_TABLE_NAME).append("=?");
            }
            sql.append(" ORDER BY ").append(MySQLConstants.COL_TABLE_NAME).append(",").append(MySQLConstants.COL_INDEX_NAME).append(",").append(MySQLConstants.COL_SEQ_IN_INDEX);

            JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString());
            dbStat.setString(1, owner.getName());
            if (forTable != null) {
                dbStat.setString(2, forTable.getName());
            }
            return dbStat;
        }

        @Nullable
        @Override
        protected MySQLTableIndex fetchObject(JDBCSession session, MySQLCatalog owner, MySQLTable parent, String indexName, JDBCResultSet dbResult)
            throws SQLException, DBException
        {
            String indexTypeName = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_INDEX_TYPE);
            DBSIndexType indexType;
            if (MySQLConstants.INDEX_TYPE_BTREE.getId().equals(indexTypeName)) {
                indexType = MySQLConstants.INDEX_TYPE_BTREE;
            } else if (MySQLConstants.INDEX_TYPE_FULLTEXT.getId().equals(indexTypeName)) {
                indexType = MySQLConstants.INDEX_TYPE_FULLTEXT;
            } else if (CommonUtils.isNotEmpty(indexTypeName) &&
                indexTypeName.toUpperCase(Locale.ENGLISH).contains(MySQLConstants.INDEX_TYPE_HASH.getId().toUpperCase(Locale.ENGLISH))
            ) {
                indexType = MySQLConstants.INDEX_TYPE_HASH;
            } else if (MySQLConstants.INDEX_TYPE_RTREE.getId().equals(indexTypeName)) {
                indexType = MySQLConstants.INDEX_TYPE_RTREE;
            } else {
                indexType = DBSIndexType.OTHER;
            }
            return new MySQLTableIndex(
                parent,
                indexName,
                indexType,
                dbResult);
        }

        @Nullable
        @Override
        protected MySQLTableIndexColumn[] fetchObjectRow(
            JDBCSession session,
            MySQLTable parent, MySQLTableIndex object, JDBCResultSet dbResult)
            throws SQLException, DBException
        {
            int ordinalPosition = JDBCUtils.safeGetInt(dbResult, MySQLConstants.COL_SEQ_IN_INDEX);
            String columnName = JDBCUtils.safeGetStringTrimmed(dbResult, MySQLConstants.COL_COLUMN_NAME);
            String ascOrDesc = JDBCUtils.safeGetStringTrimmed(dbResult, MySQLConstants.COL_COLLATION);
            boolean nullable = "YES".equals(JDBCUtils.safeGetStringTrimmed(dbResult, MySQLConstants.COL_NULLABLE));
            String subPart = JDBCUtils.safeGetStringTrimmed(dbResult, MySQLConstants.COL_SUB_PART);

            MySQLTableColumn tableColumn = columnName == null ? null : parent.getAttribute(session.getProgressMonitor(), columnName);
            if (tableColumn == null) {
                log.debug("Column '" + columnName + "' not found in table '" + parent.getName() + "' for index '" + object.getName() + "'");
                return null;
            }

            return new MySQLTableIndexColumn[] { new MySQLTableIndexColumn(
                object,
                tableColumn,
                ordinalPosition,
                "A".equalsIgnoreCase(ascOrDesc),
                nullable,
                subPart)
            };
        }

        @Override
        protected void cacheChildren(DBRProgressMonitor monitor, MySQLTableIndex index, List<MySQLTableIndexColumn> rows)
        {
            index.setColumns(rows);
        }
    }

    /**
     * Constraint cache implementation
     */
    static class UniqueKeyCache extends JDBCCompositeCache<MySQLCatalog, MySQLTable, MySQLTableConstraint, MySQLTableConstraintColumn> {
        UniqueKeyCache(TableCache tableCache)
        {
            super(tableCache, MySQLTable.class, MySQLConstants.COL_TABLE_NAME, MySQLConstants.COL_CONSTRAINT_NAME);
        }

        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(JDBCSession session, MySQLCatalog owner, MySQLTable forTable)
            throws SQLException
        {
            StringBuilder sql = new StringBuilder(500);
            sql.append(
                "SELECT kc.CONSTRAINT_NAME,kc.TABLE_NAME,kc.COLUMN_NAME,kc.ORDINAL_POSITION\n" +
                "FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE kc WHERE kc.TABLE_SCHEMA=? AND kc.REFERENCED_TABLE_NAME IS NULL");
            if (forTable != null) {
                sql.append(" AND kc.TABLE_NAME=?");
            }
            sql.append("\nORDER BY kc.CONSTRAINT_NAME,kc.ORDINAL_POSITION");

            JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString());
            dbStat.setString(1, owner.getName());
            if (forTable != null) {
                dbStat.setString(2, forTable.getName());
            }
            return dbStat;
        }

        @Nullable
        @Override
        protected MySQLTableConstraint fetchObject(JDBCSession session, MySQLCatalog owner, MySQLTable parent, String constraintName, JDBCResultSet dbResult)
            throws SQLException, DBException
        {
            if (constraintName.equals(MySQLConstants.CONSTRAINT_PRIMARY_KEY_NAME)) {
                return new MySQLTableConstraint(
                    parent, constraintName, null, DBSEntityConstraintType.PRIMARY_KEY, true);
            } else {
                return new MySQLTableConstraint(
                    parent, constraintName, null, DBSEntityConstraintType.UNIQUE_KEY, true);
            }
        }

        @Nullable
        @Override
        protected MySQLTableConstraintColumn[] fetchObjectRow(
                JDBCSession session,
                MySQLTable parent, MySQLTableConstraint object, JDBCResultSet dbResult)
            throws SQLException, DBException
        {
            String columnName = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_COLUMN_NAME);
            MySQLTableColumn column = parent.getAttribute(session.getProgressMonitor(), columnName);
            if (column == null) {
                log.warn("Column '" + columnName + "' not found in table '" + parent.getFullyQualifiedName(DBPEvaluationContext.DDL) + "'");
                return null;
            }
            int ordinalPosition = JDBCUtils.safeGetInt(dbResult, MySQLConstants.COL_ORDINAL_POSITION);

            return new MySQLTableConstraintColumn[] { new MySQLTableConstraintColumn(
                object,
                column,
                ordinalPosition) };
        }

        @Override
        protected void cacheChildren(DBRProgressMonitor monitor, MySQLTableConstraint constraint, List<MySQLTableConstraintColumn> rows)
        {
            constraint.setAttributeReferences(rows);
        }
    }

    /**
     * Check constraint cache implementation
     */

    static class CheckConstraintCache extends JDBCCompositeCache<MySQLCatalog, MySQLTable, MySQLTableConstraint, MySQLTableConstraintColumn> {
        CheckConstraintCache(TableCache tableCache)
        {
            super(tableCache, MySQLTable.class, MySQLConstants.COL_TABLE_NAME, MySQLConstants.COL_CONSTRAINT_NAME);
        }

        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(JDBCSession session, MySQLCatalog owner, MySQLTable forTable) throws SQLException {
            StringBuilder sql = new StringBuilder(500);
            sql.append(
                    "SELECT cc.CONSTRAINT_NAME, cc.CHECK_CLAUSE, tc.TABLE_NAME\n" +
                        "FROM (SELECT CONSTRAINT_NAME, CHECK_CLAUSE\n" +
                        "FROM INFORMATION_SCHEMA.CHECK_CONSTRAINTS\n" +
                        "WHERE CONSTRAINT_SCHEMA = ?\n" +
                        "ORDER BY CONSTRAINT_NAME) cc,\n" +
                        "(SELECT TABLE_NAME, CONSTRAINT_NAME\n" +
                        "FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS\n" +
                        "WHERE TABLE_SCHEMA = ?");
            if (forTable != null) {
                sql.append(" AND TABLE_NAME=?");
            }
            sql.append(") tc\n" +
                "WHERE cc.CONSTRAINT_NAME = tc.CONSTRAINT_NAME" +
                "\nORDER BY cc.CONSTRAINT_NAME");

            JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString());
            String ownerName = owner.getName();
            dbStat.setString(1, ownerName);
            dbStat.setString(2, ownerName);
            if (forTable != null) {
                dbStat.setString(3, forTable.getName());
            }
            return dbStat;
        }

        @Override
        protected MySQLTableConstraint fetchObject(JDBCSession session, MySQLCatalog owner, MySQLTable parent, String checkConstraintName, JDBCResultSet resultSet) throws SQLException, DBException {
            return new MySQLTableConstraint(parent, checkConstraintName, null, DBSEntityConstraintType.CHECK, true, resultSet);
        }

        @Override
        protected MySQLTableConstraintColumn[] fetchObjectRow(JDBCSession session, MySQLTable mySQLTable, MySQLTableConstraint forObject, JDBCResultSet resultSet) throws SQLException, DBException {
            return new MySQLTableConstraintColumn[0];
        }

        @Override
        protected void cacheChildren(DBRProgressMonitor monitor, MySQLTableConstraint object, List<MySQLTableConstraintColumn> children) {

        }
    }

    /**
     * Procedures cache implementation
     */
    public class ProceduresCache extends JDBCStructLookupCache<MySQLCatalog, MySQLProcedure, MySQLProcedureParameter> {

        public ProceduresCache()
        {
            super(JDBCConstants.PROCEDURE_NAME);
        }

        @Override
        protected MySQLProcedure fetchObject(@NotNull JDBCSession session, @NotNull MySQLCatalog owner, @NotNull JDBCResultSet dbResult)
            throws SQLException, DBException
        {
            return new MySQLProcedure(owner, dbResult);
        }

        @Override
        protected JDBCStatement prepareChildrenStatement(@NotNull JDBCSession session, @NotNull MySQLCatalog owner, @Nullable MySQLProcedure procedure)
            throws SQLException
        {
            // Load procedure columns through MySQL metadata
            // There is no metadata table about proc/func columns -
            // it should be parsed from SHOW CREATE PROCEDURE/FUNCTION query
            // Lets driver do it instead of me
            return session.getMetaData().getProcedureColumns(
                owner.getName(),
                null,
                procedure == null ? null : JDBCUtils.escapeWildCards(session, procedure.getName()),
                "%").getSourceStatement();
        }

        @Override
        protected MySQLProcedureParameter fetchChild(@NotNull JDBCSession session, @NotNull MySQLCatalog owner, @NotNull MySQLProcedure parent, @NotNull JDBCResultSet dbResult)
            throws SQLException, DBException
        {
            String columnName = JDBCUtils.safeGetString(dbResult, JDBCConstants.COLUMN_NAME);
            int columnTypeNum = JDBCUtils.safeGetInt(dbResult, JDBCConstants.COLUMN_TYPE);
            int valueType = JDBCUtils.safeGetInt(dbResult, JDBCConstants.DATA_TYPE);
            String typeName = JDBCUtils.safeGetString(dbResult, JDBCConstants.TYPE_NAME);
            int position = JDBCUtils.safeGetInt(dbResult, JDBCConstants.ORDINAL_POSITION);
            long columnSize = JDBCUtils.safeGetLong(dbResult, JDBCConstants.LENGTH);
            boolean notNull = JDBCUtils.safeGetInt(dbResult, JDBCConstants.NULLABLE) == DatabaseMetaData.procedureNoNulls;
            int scale = JDBCUtils.safeGetInt(dbResult, JDBCConstants.SCALE);
            int precision = JDBCUtils.safeGetInt(dbResult, JDBCConstants.PRECISION);
            //int radix = JDBCUtils.safeGetInt(dbResult, JDBCConstants.RADIX);
            //DBSDataType dataType = getDataSourceContainer().getInfo().getSupportedDataType(typeName);
            DBSProcedureParameterKind parameterType;
            switch (columnTypeNum) {
                case DatabaseMetaData.procedureColumnIn: parameterType = DBSProcedureParameterKind.IN; break;
                case DatabaseMetaData.procedureColumnInOut: parameterType = DBSProcedureParameterKind.INOUT; break;
                case DatabaseMetaData.procedureColumnOut: parameterType = DBSProcedureParameterKind.OUT; break;
                case DatabaseMetaData.procedureColumnReturn: parameterType = DBSProcedureParameterKind.RETURN; break;
                case DatabaseMetaData.procedureColumnResult: parameterType = DBSProcedureParameterKind.RESULTSET; break;
                default: parameterType = DBSProcedureParameterKind.UNKNOWN; break;
            }
            if (CommonUtils.isEmpty(columnName) && parameterType == DBSProcedureParameterKind.RETURN) {
                columnName = "RETURN";
            }
            return new MySQLProcedureParameter(
                parent,
                columnName,
                typeName,
                valueType,
                position,
                columnSize,
                scale, precision, notNull,
                    parameterType);
        }

        @NotNull
        @Override
        public JDBCStatement prepareLookupStatement(@NotNull JDBCSession session, @NotNull MySQLCatalog owner, @Nullable MySQLProcedure object, @Nullable String objectName) throws SQLException {
            JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT * FROM " + MySQLConstants.META_TABLE_ROUTINES +
                    "\nWHERE " + MySQLConstants.COL_ROUTINE_SCHEMA + "=?" +
                    (object == null && objectName == null ? "" : " AND " + MySQLConstants.COL_ROUTINE_NAME + "=?") +
                    " AND ROUTINE_TYPE" + (object == null ? " IN ('PROCEDURE','FUNCTION')" : "=?") +
                    "\nORDER BY " + MySQLConstants.COL_ROUTINE_NAME
            );
            dbStat.setString(1, owner.getName());
            if (object != null || objectName != null) {
                dbStat.setString(2, object != null ? object.getName() : objectName);
                if (object != null) {
                    dbStat.setString(3, String.valueOf(object.getProcedureType()));
                }
            }
            return dbStat;
        }
    }

    static class PackageCache extends JDBCObjectLookupCache<MySQLCatalog, MySQLPackage> {

        @Override
        protected MySQLPackage fetchObject(@NotNull JDBCSession session, @NotNull MySQLCatalog owner, @NotNull JDBCResultSet dbResult)
            throws SQLException, DBException
        {
            return new MySQLPackage(owner, dbResult);
        }

        @NotNull
        @Override
        public JDBCStatement prepareLookupStatement(@NotNull JDBCSession session, @NotNull MySQLCatalog owner, @Nullable MySQLPackage object, @Nullable String objectName) throws SQLException {
            JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT name,comment FROM mysql.proc\n" +
                    "WHERE db = ? AND type = 'PACKAGE'" +
                    (object == null && objectName == null ? "" : " \nAND name = ?")
            );
            dbStat.setString(1, owner.getName());
            if (object != null || objectName != null) {
                dbStat.setString(2, object != null ? object.getName() : objectName);
            }
            return dbStat;
        }
    }

    static class TriggerCache extends JDBCObjectLookupCache<MySQLCatalog, MySQLTrigger> {

        @Override
        protected MySQLTrigger fetchObject(@NotNull JDBCSession session, @NotNull MySQLCatalog owner, @NotNull JDBCResultSet dbResult)
            throws SQLException, DBException
        {
            String tableName = JDBCUtils.safeGetString(dbResult, "EVENT_OBJECT_TABLE");
            MySQLTable triggerTable = CommonUtils.isEmpty(tableName) ? null : owner.getTable(session.getProgressMonitor(), tableName);
            return new MySQLTrigger(owner, triggerTable, dbResult);
        }

        @NotNull
        @Override
        public JDBCStatement prepareLookupStatement(@NotNull JDBCSession session, @NotNull MySQLCatalog owner, @Nullable MySQLTrigger object, @Nullable String objectName) throws SQLException {
            JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT * FROM INFORMATION_SCHEMA.TRIGGERS\n" +
                    "WHERE TRIGGER_SCHEMA = ?" +
                    (object == null && objectName == null ? "" : " \nAND TRIGGER_NAME = ?")
            );
            dbStat.setString(1, owner.getName());
            if (object != null || objectName != null) {
                dbStat.setString(2, object != null ? object.getName() : objectName);
            }
            return dbStat;
        }
    }

    static class EventCache extends JDBCObjectCache<MySQLCatalog, MySQLEvent> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull MySQLCatalog owner)
            throws SQLException
        {
            final JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT * FROM information_schema.EVENTS WHERE EVENT_SCHEMA=?");
            dbStat.setString(1, owner.getName());
            return dbStat;
        }

        @Override
        protected MySQLEvent fetchObject(@NotNull JDBCSession session, @NotNull MySQLCatalog owner, @NotNull JDBCResultSet dbResult)
            throws SQLException, DBException
        {
            return new MySQLEvent(owner, dbResult);
        }

    }

    static class SequenceCache extends JDBCObjectCache<MySQLCatalog, MySQLSequence> {

        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull MySQLCatalog mySQLCatalog) throws SQLException {
            final JDBCPreparedStatement dbStat = session.prepareStatement(
                    "SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA=? AND TABLE_TYPE = 'SEQUENCE'");
            dbStat.setString(1, mySQLCatalog.getName());
            return dbStat;
        }

        @Nullable
        @Override
        protected MySQLSequence fetchObject(@NotNull JDBCSession session, @NotNull MySQLCatalog mySQLCatalog, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            String sequenceName = JDBCUtils.safeGetString(resultSet, "TABLE_NAME");
            return new MySQLSequence(mySQLCatalog, sequenceName);
        }
    }

    public static class CharsetListProvider implements IPropertyValueListProvider<MySQLCatalog> {
        @Override
        public boolean allowCustomValue()
        {
            return false;
        }
        @Override
        public Object[] getPossibleValues(MySQLCatalog object)
        {
            return object.getDataSource().getCharsets().toArray();
        }
    }

    public static class CollationListProvider implements IPropertyValueListProvider<MySQLCatalog> {
        @Override
        public boolean allowCustomValue()
        {
            return false;
        }
        @Override
        public Object[] getPossibleValues(MySQLCatalog object)
        {
            if (object.additionalInfo.defaultCharset == null) {
                return null;
            } else {
                return object.additionalInfo.defaultCharset.getCollations().toArray();
            }
        }
    }
}
