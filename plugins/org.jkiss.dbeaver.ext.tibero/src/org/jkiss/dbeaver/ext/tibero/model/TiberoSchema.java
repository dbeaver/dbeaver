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
package org.jkiss.dbeaver.ext.tibero.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBDatabaseException;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.oracle.model.OracleConstants;
import org.jkiss.dbeaver.ext.oracle.model.OracleMaterializedView;
import org.jkiss.dbeaver.ext.oracle.model.OraclePackage;
import org.jkiss.dbeaver.ext.oracle.model.OracleProcedureStandalone;
import org.jkiss.dbeaver.ext.oracle.model.OracleSchema;
import org.jkiss.dbeaver.ext.oracle.model.OracleSchemaTrigger;
import org.jkiss.dbeaver.ext.oracle.model.OracleSequence;
import org.jkiss.dbeaver.ext.oracle.model.OracleSynonym;
import org.jkiss.dbeaver.ext.oracle.model.OracleTable;
import org.jkiss.dbeaver.ext.oracle.model.OracleTableBase;
import org.jkiss.dbeaver.ext.oracle.model.OracleTableColumn;
import org.jkiss.dbeaver.ext.oracle.model.OracleTableConstraint;
import org.jkiss.dbeaver.ext.oracle.model.OracleTableConstraintColumn;
import org.jkiss.dbeaver.ext.oracle.model.OracleTableForeignKey;
import org.jkiss.dbeaver.ext.oracle.model.OracleTableForeignKeyColumn;
import org.jkiss.dbeaver.ext.oracle.model.OracleTableIndex;
import org.jkiss.dbeaver.ext.oracle.model.OracleTableIndexColumn;
import org.jkiss.dbeaver.ext.oracle.model.OracleTableTrigger;
import org.jkiss.dbeaver.ext.oracle.model.OracleTriggerColumn;
import org.jkiss.dbeaver.ext.oracle.model.OracleUtils;
import org.jkiss.dbeaver.ext.oracle.model.OracleView;
import org.jkiss.dbeaver.ext.tibero.TiberoConstants;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCCompositeCache;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectLookupCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

/**
 * TiberoSchema
 */
public class TiberoSchema extends OracleSchema {

    private static final Log log = Log.getLog(TiberoSchema.class);
    private final JDBCObjectCache<OracleSchema, OracleSequence> sequenceCache = new SequenceCache();
    private final JDBCObjectCache<OracleSchema, OraclePackage> packageCache = new PackageCache();

    public TiberoSchema(@NotNull TiberoDataSource dataSource, long id, @NotNull String name) {
        super(dataSource, id, name);
    }

    public TiberoSchema(@NotNull TiberoDataSource dataSource, @NotNull ResultSet dbResult) {
        super(dataSource, dbResult);
    }

    @NotNull
    @Override
    public TiberoDataSource getDataSource() {
        return (TiberoDataSource) super.getDataSource();
    }

    @Override
    public TiberoTable createTableImpl(@NotNull DBRProgressMonitor monitor
                                     , @NotNull OracleSchema owner
                                     , @NotNull JDBCResultSet dbResult
    ) {
        return new TiberoTable(monitor, owner, dbResult);
    }

    @Override
    public boolean isSystem() {
        return ArrayUtils.contains(TiberoConstants.SYSTEM_SCHEMAS, getName());
    }

    /**
     * Loads the table list with a Tibero-compatible query and injects the result into the
     * inherited table cache. The Oracle table list query references ALL_[ALL_]TABLES columns
     * that are missing from Tibero's data dictionary (IOT_NAME, SECONDARY, NESTED), so Tibero
     * takes over the loading while the storage stays in the shared Oracle cache — all Oracle
     * model machinery (column loading, composite caches, lookups) keeps working on the same
     * objects. Every table-list entry point below triggers this before delegating to Oracle.
     */
    private synchronized void cacheTables(@NotNull DBRProgressMonitor monitor) throws DBException {
        if (tableCache.isFullyCached()) {
            return;
        }

        final String tablesView = "TABLES";

        final List<OracleTableBase> tables = new ArrayList<>();
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load Tibero tables")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT O.*\n" +
                "     , NULL AS TABLE_TYPE_OWNER\n" +
                "     , NULL AS TABLE_TYPE\n" +
                "     , t.TABLESPACE_NAME\n" +
                "     , t.PARTITIONED\n" +
                "     , t.IOT_TYPE\n" +
                "     , NULL AS IOT_NAME\n" +
                "     , t.TEMPORARY\n" +
                "     , NULL AS SECONDARY\n" +
                "     , NULL AS NESTED\n" +
                "     , t.NUM_ROWS\n" +
                "FROM " + OracleUtils.getAdminAllViewPrefix(monitor, getDataSource(), "OBJECTS") + " O\n" +
                "LEFT JOIN " + OracleUtils.getAdminAllViewPrefix(monitor, getDataSource(), tablesView) + " t\n" +
                "  ON t.OWNER = O.OWNER\n" +
                " AND t.TABLE_NAME = O.OBJECT_NAME\n" +
                "WHERE O.OWNER = ?\n" +
                "  AND O.OBJECT_TYPE IN ('TABLE', 'VIEW', 'MATERIALIZED VIEW')"
            )) {
                dbStat.setString(1, getName());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    while (dbResult.next()) {
                        if (monitor.isCanceled()) {
                            break;
                        }
                        final String objectType = JDBCUtils.safeGetString(dbResult, "OBJECT_TYPE");
                        if ("TABLE".equals(objectType)) {
                            tables.add(createTableImpl(monitor, this, dbResult));
                        } else if ("MATERIALIZED VIEW".equals(objectType)) {
                            tables.add(new OracleMaterializedView(this, dbResult));
                        } else {
                            tables.add(new TiberoView(this, dbResult));
                        }
                    }
                }
            }
        } catch (SQLException e) {
            throw new DBDatabaseException(e, getDataSource());
        }
        tables.sort(DBUtils.nameComparator());
        tableCache.setCache(tables);
    }

    /**
     * Index loading is delegated to a dedicated Tibero loader so the schema keeps only the
     * lifecycle hook and the shared Oracle index cache remains the storage layer.
     */
    private synchronized void cacheIndexes(@NotNull DBRProgressMonitor monitor) throws DBException {
        JDBCCompositeCache<OracleSchema, OracleTableBase, OracleTableIndex, OracleTableIndexColumn> cache = indexCache;
        if (cache.isFullyCached()) {
            return;
        }
        cacheTables(monitor);
        List<OracleTableIndex> indexes = loadIndexes(monitor);
        for (OracleTableIndex index : indexes) {
            cache.cacheObject(index);
        }
        cache.setCache(indexes);
    }

    private List<OracleTableIndex> loadIndexes(@NotNull DBRProgressMonitor monitor) throws DBException {
        List<OracleTableIndex> indexes = new ArrayList<>();
        AtomicReference<OracleTableIndex> curIndex = new AtomicReference<>();
        AtomicReference<OracleTableBase> curTable = new AtomicReference<>();
        AtomicReference<String> curKey = new AtomicReference<>();
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load Tibero indexes")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT i.OWNER\n" +
                "     , i.INDEX_NAME\n" +
                "     , i.INDEX_TYPE\n" +
                "     , i.TABLE_OWNER\n" +
                "     , i.TABLE_NAME\n" +
                "     , i.UNIQUENESS\n" +
                "     , i.TABLESPACE_NAME\n" +
                "     , i.STATUS\n" +
                "     , i.NUM_ROWS\n" +
                "     , NULL AS SAMPLE_SIZE\n" +
                "     , ic.COLUMN_NAME\n" +
                "     , ic.COLUMN_POSITION\n" +
                "     , ic.COLUMN_LENGTH\n" +
                "     , ic.DESCEND\n" +
                "     , iex.COLUMN_EXPRESSION\n" +
                "FROM " + OracleUtils.getAdminAllViewPrefix(monitor, getDataSource(), "INDEXES") + " i\n" +
                "JOIN " + OracleUtils.getAdminAllViewPrefix(monitor, getDataSource(), "IND_COLUMNS") + " ic\n" +
                "  ON ic.INDEX_OWNER = i.OWNER\n" +
                " AND ic.INDEX_NAME = i.INDEX_NAME\n" +
                "LEFT JOIN " + OracleUtils.getAdminAllViewPrefix(monitor, getDataSource(), "IND_EXPRESSIONS") + " iex\n" +
                "  ON iex.INDEX_OWNER = ic.INDEX_OWNER\n" +
                " AND iex.INDEX_NAME = ic.INDEX_NAME\n" +
                " AND iex.COLUMN_POSITION = ic.COLUMN_POSITION\n" +
                "WHERE i.OWNER = ?\n" +
                "ORDER BY i.TABLE_NAME, i.INDEX_NAME, ic.COLUMN_POSITION"
            )) {
                dbStat.setString(1, getName());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    while (dbResult.next()) {
                        if (monitor.isCanceled()) {
                            break;
                        }
                        processIndexRow(monitor, dbResult, indexes, curIndex, curTable, curKey);
                    }
                }
            }
        } catch (SQLException e) {
            throw new DBDatabaseException(e, getDataSource());
        }
        return indexes;
    }

    private void processIndexRow(
        @NotNull DBRProgressMonitor monitor,
        @NotNull JDBCResultSet dbResult,
        @NotNull List<OracleTableIndex> indexes,
        @NotNull AtomicReference<OracleTableIndex> curIndex,
        @NotNull AtomicReference<OracleTableBase> curTable,
        @NotNull AtomicReference<String> curKey
    ) throws DBException {
        String tableName = JDBCUtils.safeGetStringTrimmed(dbResult, "TABLE_NAME");
        String indexName = JDBCUtils.safeGetStringTrimmed(dbResult, "INDEX_NAME");
        if (CommonUtils.isEmpty(tableName) || CommonUtils.isEmpty(indexName)) {
            return;
        }
        String key = tableName + "." + indexName;
        if (!key.equals(curKey.get())) {
            curKey.set(key);
            curIndex.set(null);
            curTable.set(tableCache.getObject(monitor, this, tableName));
            if (curTable.get() == null) {
                log.debug("Table '" + tableName + "' not found for index '" + indexName + "'");
                return;
            }
            OracleTableIndex index = new TiberoTableIndex(this, curTable.get(), indexName, dbResult);
            curIndex.set(index);
            indexes.add(index);
        }
        OracleTableIndex index = curIndex.get();
        OracleTableBase table = curTable.get();
        if (index == null || table == null) {
            return;
        }
        String columnName = JDBCUtils.safeGetStringTrimmed(dbResult, "COLUMN_NAME");
        OracleTableColumn tableColumn = columnName == null ? null : table.getAttribute(monitor, columnName);
        if (tableColumn == null) {
            log.debug("Column '" + columnName + "' not found in table '" + tableName + "' for index '" + indexName + "'");
            return;
        }
        index.addColumn(new OracleTableIndexColumn(
            index,
            tableColumn,
            JDBCUtils.safeGetInt(dbResult, "COLUMN_POSITION"),
            "ASC".equals(JDBCUtils.safeGetStringTrimmed(dbResult, "DESCEND")),
            JDBCUtils.safeGetStringTrimmed(dbResult, "COLUMN_EXPRESSION")));
    }

    @NotNull
    List<OracleTableIndex> getTableIndexes(@NotNull DBRProgressMonitor monitor, @NotNull OracleTableBase table) throws DBException {
        cacheIndexes(monitor);
        List<OracleTableIndex> result = new ArrayList<>();
        for (OracleTableIndex index : super.getIndexes(monitor)) {
            if (index.getTable() == table) {
                result.add(index);
            }
        }
        return result;
    }

    /**
     * Same takeover for triggers: Tibero's ALL_TRIGGERS has no BASE_OBJECT_TYPE column —
     * schema-level triggers are the rows without a base table instead.
     */
    private synchronized void cacheSchemaTriggers(@NotNull DBRProgressMonitor monitor) throws DBException {
        JDBCObjectCache<OracleSchema, OracleSchemaTrigger> cache = triggerCache;
        if (cache.isFullyCached()) {
            return;
        }
        cache.setCache(loadSchemaTriggers(monitor));
    }

    private List<OracleSchemaTrigger> loadSchemaTriggers(@NotNull DBRProgressMonitor monitor) throws DBException {
        List<OracleSchemaTrigger> triggers = new ArrayList<>();
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load Tibero schema triggers")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT t.OWNER,t.TRIGGER_NAME,t.TRIGGER_TYPE,t.TRIGGERING_EVENT,t.TABLE_OWNER,t.TABLE_NAME," +
                "t.REFERENCING_NAMES,t.WHEN_CLAUSE,t.STATUS," +
                "'SCHEMA' AS BASE_OBJECT_TYPE,NULL AS COLUMN_NAME,NULL AS DESCRIPTION,NULL AS ACTION_TYPE\n" +
                "FROM " + OracleUtils.getAdminAllViewPrefix(monitor, getDataSource(), "TRIGGERS") + " t\n" +
                "WHERE t.OWNER=? AND t.TABLE_NAME IS NULL\n" +
                "ORDER BY t.TRIGGER_NAME"
            )) {
                dbStat.setString(1, getName());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    while (dbResult.next()) {
                        if (monitor.isCanceled()) {
                            break;
                        }
                        if (!CommonUtils.isEmpty(JDBCUtils.safeGetStringTrimmed(dbResult, "TRIGGER_NAME"))) {
                            triggers.add(createSchemaTrigger(dbResult));
                        }
                    }
                }
            }
        } catch (SQLException e) {
            throw new DBDatabaseException(e, getDataSource());
        }
        return triggers;
    }

    private synchronized void cacheTableTriggers(@NotNull DBRProgressMonitor monitor) throws DBException {
        JDBCCompositeCache<OracleSchema, OracleTableBase, OracleTableTrigger, OracleTriggerColumn> cache = tableTriggerCache;
        if (cache.isFullyCached()) {
            return;
        }
        cache.setCache(loadTableTriggers(monitor));
    }

    private List<OracleTableTrigger> loadTableTriggers(@NotNull DBRProgressMonitor monitor) throws DBException {
        cacheTables(monitor);
        List<OracleTableTrigger> triggers = loadTableTriggerRows(monitor);
        return triggers;
    }

    private List<OracleTableTrigger> loadTableTriggerRows(@NotNull DBRProgressMonitor monitor) throws DBException {
        List<OracleTableTrigger> triggers = new ArrayList<>();
        AtomicReference<OracleTableTrigger> curTrigger = new AtomicReference<>();
        AtomicReference<OracleTableBase> curTable = new AtomicReference<>();
        AtomicReference<List<OracleTriggerColumn>> curColumns = new AtomicReference<>(new ArrayList<>());
        AtomicReference<String> curKey = new AtomicReference<>();
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load Tibero table triggers")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT t.OWNER\n" +
                "     , t.TRIGGER_NAME\n" +
                "     , t.TRIGGER_TYPE\n" +
                "     , t.TRIGGERING_EVENT\n" +
                "     , t.TABLE_OWNER\n" +
                "     , t.TABLE_NAME\n" +
                "     , t.REFERENCING_NAMES\n" +
                "     , t.WHEN_CLAUSE\n" +
                "     , t.STATUS\n" +
                "     , 'TABLE' AS BASE_OBJECT_TYPE\n" +
                "     , NULL AS COLUMN_NAME\n" +
                "     , NULL AS DESCRIPTION\n" +
                "     , NULL AS ACTION_TYPE\n" +
                "     , c.COLUMN_NAME AS TRIGGER_COLUMN_NAME\n" +
                "     , c.COLUMN_LIST\n" +
                "FROM " + OracleUtils.getAdminAllViewPrefix(monitor, getDataSource(), "TRIGGERS") + " t\n" +
                "LEFT JOIN " + OracleUtils.getAdminAllViewPrefix(monitor, getDataSource(), "TRIGGER_COLS") + " c\n" +
                "  ON t.TABLE_OWNER = c.TABLE_OWNER\n" +
                " AND t.TABLE_NAME = c.TABLE_NAME\n" +
                " AND t.OWNER = c.TRIGGER_OWNER\n" +
                " AND t.TRIGGER_NAME = c.TRIGGER_NAME\n" +
                "WHERE t.TABLE_OWNER = ?\n" +
                "  AND t.TABLE_NAME IS NOT NULL\n" +
                "ORDER BY t.TABLE_NAME, t.TRIGGER_NAME"
            )) {
                dbStat.setString(1, getName());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    while (dbResult.next()) {
                        if (monitor.isCanceled()) {
                            break;
                        }
                        processTableTriggerRow(monitor, dbResult, triggers, curTrigger, curTable, curColumns, curKey);
                    }
                }
            }
        } catch (SQLException e) {
            throw new DBDatabaseException(e, getDataSource());
        }
        finalizeCurrentTrigger(curTrigger.get(), curColumns.get());
        return triggers;
    }

    private void processTableTriggerRow(
        @NotNull DBRProgressMonitor monitor,
        @NotNull JDBCResultSet dbResult,
        @NotNull List<OracleTableTrigger> triggers,
        @NotNull AtomicReference<OracleTableTrigger> curTrigger,
        @NotNull AtomicReference<OracleTableBase> curTable,
        @NotNull AtomicReference<List<OracleTriggerColumn>> curColumns,
        @NotNull AtomicReference<String> curKey
    ) throws DBException {
        String tableName = JDBCUtils.safeGetStringTrimmed(dbResult, "TABLE_NAME");
        String triggerName = JDBCUtils.safeGetStringTrimmed(dbResult, "TRIGGER_NAME");
        if (CommonUtils.isEmpty(tableName) || CommonUtils.isEmpty(triggerName)) {
            return;
        }
        String key = tableName + "." + triggerName;
        if (!key.equals(curKey.get())) {
            curKey.set(key);
            finalizeCurrentTrigger(curTrigger.get(), curColumns.get());
            curTrigger.set(null);
            curColumns.set(new ArrayList<>());
            curTable.set(tableCache.getObject(monitor, this, tableName));
            if (curTable.get() == null) {
                log.debug("Table '" + tableName + "' not found for trigger '" + triggerName + "'");
                return;
            }
            OracleTableTrigger trigger = createTableTrigger(curTable.get(), dbResult);
            curTrigger.set(trigger);
            triggers.add(trigger);
        }
        OracleTableTrigger trigger = curTrigger.get();
        OracleTableBase table = curTable.get();
        if (trigger == null || table == null) {
            return;
        }
        String columnName = JDBCUtils.safeGetStringTrimmed(dbResult, "TRIGGER_COLUMN_NAME");
        OracleTableColumn tableColumn = columnName == null ? null : table.getAttribute(monitor, columnName);
        if (tableColumn == null) {
            return;
        }
        curColumns.get().add(createTriggerColumn(monitor, trigger, tableColumn, dbResult));
    }

    private void finalizeCurrentTrigger(@Nullable OracleTableTrigger trigger, @NotNull List<OracleTriggerColumn> columns) {
        if (trigger != null) {
            trigger.setColumns(columns);
        }
    }

    private OracleSchemaTrigger createSchemaTrigger(@NotNull JDBCResultSet dbResult) {
        return new OracleSchemaTrigger(this, dbResult);
    }

    private OracleTableTrigger createTableTrigger(@NotNull OracleTableBase table, @NotNull JDBCResultSet dbResult) {
        return new OracleTableTrigger(table, dbResult);
    }

    private OracleTriggerColumn createTriggerColumn(
        @NotNull DBRProgressMonitor monitor,
        @NotNull OracleTableTrigger trigger,
        @NotNull OracleTableColumn tableColumn,
        @NotNull JDBCResultSet dbResult
    ) throws DBException {
        return new OracleTriggerColumn(monitor, trigger, tableColumn, dbResult);
    }

    @NotNull
    List<OracleTableTrigger> getTableTriggers(@NotNull DBRProgressMonitor monitor, @NotNull OracleTableBase table) throws DBException {
        cacheTableTriggers(monitor);
        List<OracleTableTrigger> result = new ArrayList<>();
        for (OracleTableTrigger trigger : super.getTableTriggers(monitor)) {
            if (trigger.getParentObject() == table) {
                result.add(trigger);
            }
        }
        return result;
    }

    @NotNull
    List<OracleTableConstraint> getTableConstraints(@NotNull DBRProgressMonitor monitor, @NotNull OracleTableBase table) throws DBException {
        List<OracleTableConstraint> constraints = loadTableConstraints(monitor, table);
        return constraints;
    }

    @NotNull
    List<OracleTableForeignKey> getTableForeignKeys(@NotNull DBRProgressMonitor monitor, @NotNull OracleTableBase table) throws DBException {
        return loadTableForeignKeys(monitor, table);
    }

    @NotNull
    List<OracleTableForeignKey> getTableForeignKeyReferences(@NotNull DBRProgressMonitor monitor, @NotNull OracleTableBase table) throws DBException {
        return loadTableForeignKeyReferences(monitor, table);
    }

    private List<OracleTableConstraint> loadTableConstraints(@NotNull DBRProgressMonitor monitor, @NotNull OracleTableBase table) throws DBException {
        Map<String, OracleTableConstraint> constraints = new LinkedHashMap<>();
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load Tibero table constraints")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT c.TABLE_NAME\n" +
                "     , c.CONSTRAINT_NAME\n" +
                "     , c.CONSTRAINT_TYPE\n" +
                "     , c.STATUS\n" +
                "     , c.SEARCH_CONDITION\n" +
                "FROM " + OracleUtils.getAdminAllViewPrefix(monitor, getDataSource(), "CONSTRAINTS") + " c\n" +
                "WHERE c.OWNER = ?\n" +
                "  AND c.TABLE_NAME = ?\n" +
                "  AND c.CONSTRAINT_TYPE <> 'R'\n" +
                "ORDER BY c.CONSTRAINT_NAME"
            )) {
                dbStat.setString(1, getName());
                dbStat.setString(2, table.getName());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    while (dbResult.next()) {
                        if (monitor.isCanceled()) {
                            break;
                        }
                        OracleTableConstraint constraint = new OracleTableConstraint(table, dbResult);
                        constraints.put(constraint.getName(), constraint);
                    }
                }
            }
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT CONSTRAINT_NAME\n" +
                "     , COLUMN_NAME\n" +
                "     , POSITION\n" +
                "FROM " + OracleUtils.getAdminAllViewPrefix(monitor, getDataSource(), "CONS_COLUMNS") + "\n" +
                "WHERE OWNER = ?\n" +
                "  AND TABLE_NAME = ?\n" +
                "ORDER BY CONSTRAINT_NAME, POSITION"
            )) {
                dbStat.setString(1, getName());
                dbStat.setString(2, table.getName());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    while (dbResult.next()) {
                        if (monitor.isCanceled()) {
                            break;
                        }
                        OracleTableConstraint constraint = constraints.get(JDBCUtils.safeGetStringTrimmed(dbResult, "CONSTRAINT_NAME"));
                        if (constraint == null) {
                            continue;
                        }
                        String columnName = JDBCUtils.safeGetStringTrimmed(dbResult, "COLUMN_NAME");
                        OracleTableColumn tableColumn = columnName == null ? null : table.getAttribute(monitor, columnName);
                        if (tableColumn == null) {
                            continue;
                        }
                        List<OracleTableConstraintColumn> refs = constraint.getAttributeReferences(null);
                        if (refs == null) {
                            refs = new ArrayList<>();
                            constraint.setAttributeReferences(refs);
                        }
                        refs.add(new OracleTableConstraintColumn(constraint, tableColumn, JDBCUtils.safeGetInt(dbResult, "POSITION")));
                    }
                }
            }
        } catch (SQLException e) {
            throw new DBDatabaseException(e, getDataSource());
        }
        return new ArrayList<>(constraints.values());
    }

    private List<OracleTableForeignKey> loadTableForeignKeys(@NotNull DBRProgressMonitor monitor, @NotNull OracleTableBase table) throws DBException {
        Map<String, OracleTableForeignKey> foreignKeys = new LinkedHashMap<>();
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load Tibero table foreign keys")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT c.TABLE_NAME\n" +
                "     , c.CONSTRAINT_NAME\n" +
                "     , c.CONSTRAINT_TYPE\n" +
                "     , c.STATUS\n" +
                "     , c.R_OWNER\n" +
                "     , c.R_CONSTRAINT_NAME\n" +
                "     , rc.TABLE_NAME AS R_TABLE_NAME\n" +
                "     , c.DELETE_RULE\n" +
                "FROM " + OracleUtils.getAdminAllViewPrefix(monitor, getDataSource(), "CONSTRAINTS") + " c\n" +
                "LEFT JOIN " + OracleUtils.getAdminAllViewPrefix(monitor, getDataSource(), "CONSTRAINTS") + " rc\n" +
                "  ON rc.OWNER = c.R_OWNER\n" +
                " AND rc.CONSTRAINT_NAME = c.R_CONSTRAINT_NAME\n" +
                " AND rc.CONSTRAINT_TYPE = 'P'\n" +
                "WHERE c.OWNER = ?\n" +
                "  AND c.TABLE_NAME = ?\n" +
                "  AND c.CONSTRAINT_TYPE = 'R'\n" +
                "ORDER BY c.CONSTRAINT_NAME"
            )) {
                dbStat.setString(1, getName());
                dbStat.setString(2, table.getName());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    while (dbResult.next()) {
                        if (monitor.isCanceled()) {
                            break;
                        }
                        OracleTableForeignKey fk = new OracleTableForeignKey(monitor, (OracleTable) table, dbResult);
                        foreignKeys.put(fk.getName(), fk);
                    }
                }
            }
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT CONSTRAINT_NAME\n" +
                "     , COLUMN_NAME\n" +
                "     , POSITION\n" +
                "FROM " + OracleUtils.getAdminAllViewPrefix(monitor, getDataSource(), "CONS_COLUMNS") + "\n" +
                "WHERE OWNER = ?\n" +
                "  AND TABLE_NAME = ?\n" +
                "ORDER BY CONSTRAINT_NAME, POSITION"
            )) {
                dbStat.setString(1, getName());
                dbStat.setString(2, table.getName());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    while (dbResult.next()) {
                        if (monitor.isCanceled()) {
                            break;
                        }
                        OracleTableForeignKey fk = foreignKeys.get(JDBCUtils.safeGetStringTrimmed(dbResult, "CONSTRAINT_NAME"));
                        if (fk == null) {
                            continue;
                        }
                        String columnName = JDBCUtils.safeGetStringTrimmed(dbResult, "COLUMN_NAME");
                        OracleTableColumn tableColumn = columnName == null ? null : table.getAttribute(monitor, columnName);
                        if (tableColumn == null) {
                            continue;
                        }
                        List<OracleTableConstraintColumn> refs = fk.getAttributeReferences(null);
                        if (refs == null) {
                            refs = new ArrayList<>();
                            fk.setAttributeReferences(refs);
                        }
                        refs.add(new OracleTableForeignKeyColumn(fk, tableColumn, JDBCUtils.safeGetInt(dbResult, "POSITION")));
                    }
                }
            }
        } catch (SQLException e) {
            throw new DBDatabaseException(e, getDataSource());
        }
        return new ArrayList<>(foreignKeys.values());
    }

    private List<OracleTableForeignKey> loadTableForeignKeyReferences(@NotNull DBRProgressMonitor monitor, @NotNull OracleTableBase table) throws DBException {
        cacheTables(monitor);
        Map<String, OracleTableForeignKey> foreignKeys = new LinkedHashMap<>();
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load Tibero table foreign key references")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT c.TABLE_NAME\n" +
                "     , c.CONSTRAINT_NAME\n" +
                "     , c.CONSTRAINT_TYPE\n" +
                "     , c.STATUS\n" +
                "     , c.R_OWNER\n" +
                "     , c.R_CONSTRAINT_NAME\n" +
                "     , rc.TABLE_NAME AS R_TABLE_NAME\n" +
                "     , c.DELETE_RULE\n" +
                "FROM " + OracleUtils.getAdminAllViewPrefix(monitor, getDataSource(), "CONSTRAINTS") + " c\n" +
                "JOIN " + OracleUtils.getAdminAllViewPrefix(monitor, getDataSource(), "CONSTRAINTS") + " rc\n" +
                "  ON rc.OWNER = c.R_OWNER\n" +
                " AND rc.CONSTRAINT_NAME = c.R_CONSTRAINT_NAME\n" +
                " AND rc.CONSTRAINT_TYPE = 'P'\n" +
                "WHERE c.OWNER = ?\n" +
                "  AND c.CONSTRAINT_TYPE = 'R'\n" +
                "  AND rc.OWNER = ?\n" +
                "  AND rc.TABLE_NAME = ?\n" +
                "ORDER BY c.TABLE_NAME, c.CONSTRAINT_NAME"
            )) {
                dbStat.setString(1, getName());
                dbStat.setString(2, table.getSchema().getName());
                dbStat.setString(3, table.getName());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    while (dbResult.next()) {
                        if (monitor.isCanceled()) {
                            break;
                        }
                        String referencingTableName = JDBCUtils.safeGetStringTrimmed(dbResult, "TABLE_NAME");
                        OracleTableBase referencingTable = CommonUtils.isEmpty(referencingTableName) ?
                            null :
                            tableCache.getObject(monitor, this, referencingTableName);
                        if (!(referencingTable instanceof OracleTable oracleTable)) {
                            continue;
                        }
                        OracleTableForeignKey fk = new OracleTableForeignKey(monitor, oracleTable, dbResult);
                        foreignKeys.put(getForeignKeyCacheKey(referencingTableName, fk.getName()), fk);
                    }
                }
            }
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT c.TABLE_NAME\n" +
                "     , cc.CONSTRAINT_NAME\n" +
                "     , cc.COLUMN_NAME\n" +
                "     , cc.POSITION\n" +
                "FROM " + OracleUtils.getAdminAllViewPrefix(monitor, getDataSource(), "CONSTRAINTS") + " c\n" +
                "JOIN " + OracleUtils.getAdminAllViewPrefix(monitor, getDataSource(), "CONSTRAINTS") + " rc\n" +
                "  ON rc.OWNER = c.R_OWNER\n" +
                " AND rc.CONSTRAINT_NAME = c.R_CONSTRAINT_NAME\n" +
                " AND rc.CONSTRAINT_TYPE = 'P'\n" +
                "JOIN " + OracleUtils.getAdminAllViewPrefix(monitor, getDataSource(), "CONS_COLUMNS") + " cc\n" +
                "  ON cc.OWNER = c.OWNER\n" +
                " AND cc.TABLE_NAME = c.TABLE_NAME\n" +
                " AND cc.CONSTRAINT_NAME = c.CONSTRAINT_NAME\n" +
                "WHERE c.OWNER = ?\n" +
                "  AND c.CONSTRAINT_TYPE = 'R'\n" +
                "  AND rc.OWNER = ?\n" +
                "  AND rc.TABLE_NAME = ?\n" +
                "ORDER BY c.TABLE_NAME, cc.CONSTRAINT_NAME, cc.POSITION"
            )) {
                dbStat.setString(1, getName());
                dbStat.setString(2, table.getSchema().getName());
                dbStat.setString(3, table.getName());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    while (dbResult.next()) {
                        if (monitor.isCanceled()) {
                            break;
                        }
                        String referencingTableName = JDBCUtils.safeGetStringTrimmed(dbResult, "TABLE_NAME");
                        OracleTableForeignKey fk = foreignKeys.get(
                            getForeignKeyCacheKey(referencingTableName, JDBCUtils.safeGetStringTrimmed(dbResult, "CONSTRAINT_NAME")));
                        if (fk == null) {
                            continue;
                        }
                        String columnName = JDBCUtils.safeGetStringTrimmed(dbResult, "COLUMN_NAME");
                        OracleTableColumn tableColumn = columnName == null ? null : fk.getTable().getAttribute(monitor, columnName);
                        if (tableColumn == null) {
                            continue;
                        }
                        List<OracleTableConstraintColumn> refs = fk.getAttributeReferences(null);
                        if (refs == null) {
                            refs = new ArrayList<>();
                            fk.setAttributeReferences(refs);
                        }
                        refs.add(new OracleTableForeignKeyColumn(fk, tableColumn, JDBCUtils.safeGetInt(dbResult, "POSITION")));
                    }
                }
            }
        } catch (SQLException e) {
            throw new DBDatabaseException(e, getDataSource());
        }
        return new ArrayList<>(foreignKeys.values());
    }

    private String getForeignKeyCacheKey(@Nullable String tableName, @Nullable String constraintName) {
        return tableName + "." + constraintName;
    }

    /**
     * Takeover for procedures/functions. The list query itself (ALL_OBJECTS based) is Tibero-safe,
     * but Oracle's ProceduresCache instantiates OracleProcedureStandalone whose parameter loading
     * queries ALL_ARGUMENTS ordered by the missing SEQUENCE column. We re-own the load so the cache
     * holds TiberoProcedureStandalone instances, which override getParameters() with a
     * Tibero-compatible arguments query. Every procedure entry point below triggers this first
     * (getProceduresOnly/getFunctionsOnly funnel through getProcedures).
     */
    private synchronized void cacheProcedures(@NotNull DBRProgressMonitor monitor) throws DBException {
        JDBCObjectCache<OracleSchema, OracleProcedureStandalone> cache = proceduresCache;
        if (cache.isFullyCached()) {
            return;
        }
        cache.setCache(loadProcedures(monitor));
    }

    private synchronized void cacheSequences(@NotNull DBRProgressMonitor monitor) throws DBException {
        JDBCObjectCache<OracleSchema, OracleSequence> cache = sequenceCache;
        if (cache.isFullyCached()) {
            return;
        }
        cache.setCache(loadSequences(monitor));
    }

    private synchronized void cachePackages(@NotNull DBRProgressMonitor monitor) throws DBException {
        JDBCObjectCache<OracleSchema, OraclePackage> cache = packageCache;
        if (cache.isFullyCached()) {
            return;
        }
        cache.setCache(loadPackages(monitor));
    }

    private List<OracleProcedureStandalone> loadProcedures(@NotNull DBRProgressMonitor monitor) throws DBException {
        List<OracleProcedureStandalone> procedures = new ArrayList<>();
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load Tibero procedures")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT " + OracleUtils.getSysCatalogHint(getDataSource()) + " *\n" +
                "FROM " + OracleUtils.getAdminAllViewPrefix(monitor, getDataSource(), "OBJECTS") + "\n" +
                "WHERE OBJECT_TYPE IN ('PROCEDURE', 'FUNCTION')\n" +
                "  AND OWNER = ?\n" +
                "ORDER BY OBJECT_NAME"
            )) {
                dbStat.setString(1, getName());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    while (dbResult.next()) {
                        if (monitor.isCanceled()) {
                            break;
                        }
                        procedures.add(createProcedure(dbResult));
                    }
                }
            }
        } catch (SQLException e) {
            throw new DBDatabaseException(e, getDataSource());
        }
        return procedures;
    }

    private List<OracleSequence> loadSequences(@NotNull DBRProgressMonitor monitor) throws DBException {
        List<OracleSequence> sequences = new ArrayList<>();
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load Tibero sequences")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT " + OracleUtils.getSysCatalogHint(getDataSource()) + " *\n" +
                "FROM " + OracleUtils.getAdminAllViewPrefix(monitor, getDataSource(), "SEQUENCES") + "\n" +
                "WHERE SEQUENCE_OWNER = ?\n" +
                "ORDER BY SEQUENCE_NAME"
            )) {
                dbStat.setString(1, getName());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    while (dbResult.next()) {
                        if (monitor.isCanceled()) {
                            break;
                        }
                        sequences.add(new TiberoSequence(this, dbResult));
                    }
                }
            }
        } catch (SQLException e) {
            throw new DBDatabaseException(e, getDataSource());
        }
        return sequences;
    }

    private List<OraclePackage> loadPackages(@NotNull DBRProgressMonitor monitor) throws DBException {
        List<OraclePackage> packages = new ArrayList<>();
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load Tibero packages")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT " + OracleUtils.getSysCatalogHint(getDataSource()) + " OBJECT_NAME\n" +
                "     , STATUS\n" +
                "     , CREATED\n" +
                "     , LAST_DDL_TIME\n" +
                "     , TEMPORARY\n" +
                "FROM " + OracleUtils.getAdminAllViewPrefix(monitor, getDataSource(), "OBJECTS") + "\n" +
                "WHERE OBJECT_TYPE = 'PACKAGE'\n" +
                "  AND OWNER = ?\n" +
                "ORDER BY OBJECT_NAME"
            )) {
                dbStat.setString(1, getName());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    while (dbResult.next()) {
                        if (monitor.isCanceled()) {
                            break;
                        }
                        packages.add(new TiberoPackage(this, dbResult));
                    }
                }
            }
        } catch (SQLException e) {
            throw new DBDatabaseException(e, getDataSource());
        }
        return packages;
    }

    private OracleSynonym createSynonym(@NotNull JDBCResultSet dbResult) {
        return new OracleSynonym(this, dbResult);
    }

    /**
     * Synonyms are still loaded through a Tibero-specific query because Oracle's synonym cache
     * expects columns and joins that Tibero does not expose in the same form.
     */
    private synchronized void cacheSynonyms(@NotNull DBRProgressMonitor monitor) throws DBException {
        JDBCObjectLookupCache<OracleSchema, OracleSynonym> cache = synonymCache;
        if (cache.isFullyCached()) {
            return;
        }
        cache.setCache(loadSynonyms(monitor));
    }

    private List<OracleSynonym> loadSynonyms(@NotNull DBRProgressMonitor monitor) throws DBException {
        final String ownerName = getName();
        final boolean readAllSynonyms = getDataSource().getContainer().getPreferenceStore()
            .getBoolean(OracleConstants.PREF_DBMS_READ_ALL_SYNONYMS);
        final String synonymTypeFilter = readAllSynonyms ? "" : "AND O.OBJECT_TYPE NOT IN ('JAVA CLASS','PACKAGE BODY')\n";
        final String synonymsView = OracleUtils.getAdminAllViewPrefix(monitor, getDataSource(), "SYNONYMS");
        final String objectsView = OracleUtils.getAdminAllViewPrefix(monitor, getDataSource(), "OBJECTS");
        List<OracleSynonym> synonyms = new ArrayList<>();
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load Tibero synonyms")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT OWNER\n" +
                "     , SYNONYM_NAME\n" +
                "     , MAX(TABLE_OWNER) AS TABLE_OWNER\n" +
                "     , MAX(TABLE_NAME) AS TABLE_NAME\n" +
                "     , MAX(OBJECT_TYPE) AS OBJECT_TYPE\n" +
                "FROM (\n" +
                "    SELECT S.OWNER\n" +
                "         , S.SYNONYM_NAME\n" +
                "         , S.ORG_OBJECT_OWNER AS TABLE_OWNER\n" +
                "         , S.ORG_OBJECT_NAME AS TABLE_NAME\n" +
                "         , NULL AS OBJECT_TYPE\n" +
                "    FROM " + synonymsView + " S\n" +
                "    WHERE S.OWNER = ?\n" +
                "    UNION ALL\n" +
                "    SELECT S.OWNER\n" +
                "         , S.SYNONYM_NAME\n" +
                "         , S.ORG_OBJECT_OWNER AS TABLE_OWNER\n" +
                "         , S.ORG_OBJECT_NAME AS TABLE_NAME\n" +
                "         , O.OBJECT_TYPE\n" +
                "    FROM " + synonymsView + " S\n" +
                "    JOIN " + objectsView + " O\n" +
                "      ON O.OWNER = S.ORG_OBJECT_OWNER\n" +
                "     AND O.OBJECT_NAME = S.ORG_OBJECT_NAME\n" +
                "     AND O.SUBOBJECT_NAME IS NULL\n" +
                "    WHERE S.OWNER = ?\n" +
                synonymTypeFilter +
                ")\n" +
                "GROUP BY OWNER, SYNONYM_NAME\n" +
                "ORDER BY SYNONYM_NAME"
            )) {
                dbStat.setString(1, ownerName);
                dbStat.setString(2, ownerName);
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    while (dbResult.next()) {
                        if (monitor.isCanceled()) {
                            break;
                        }
                        if (!CommonUtils.isEmpty(JDBCUtils.safeGetStringTrimmed(dbResult, "SYNONYM_NAME"))) {
                            synonyms.add(createSynonym(dbResult));
                        }
                    }
                }
            }
        } catch (SQLException e) {
            throw new DBDatabaseException(e, getDataSource());
        }
        return synonyms;
    }

    private TiberoProcedureStandalone createProcedure(@NotNull JDBCResultSet dbResult) {
        return new TiberoProcedureStandalone(this, dbResult);
    }

    @NotNull
    @Association
    @Override
    public Collection<? extends OracleTable> getTables(DBRProgressMonitor monitor) throws DBException {
        cacheTables(monitor);
        return super.getTables(monitor);
    }

    @Override
    public OracleTable getTable(DBRProgressMonitor monitor, String name) throws DBException {
        cacheTables(monitor);
        return super.getTable(monitor, name);
    }

    @NotNull
    @Association
    @Override
    public Collection<OracleView> getViews(DBRProgressMonitor monitor) throws DBException {
        cacheTables(monitor);
        return super.getViews(monitor);
    }

    @NotNull
    @Association
    @Override
    public Collection<OracleSequence> getSequences(DBRProgressMonitor monitor) throws DBException {
        cacheSequences(monitor);
        return sequenceCache.getAllObjects(monitor, this);
    }

    @NotNull
    @Association
    @Override
    public Collection<OraclePackage> getPackages(DBRProgressMonitor monitor) throws DBException {
        cachePackages(monitor);
        return packageCache.getAllObjects(monitor, this);
    }

    @NotNull
    @Association
    @Override
    public Collection<OracleMaterializedView> getMaterializedViews(DBRProgressMonitor monitor) throws DBException {
        cacheTables(monitor);
        return super.getMaterializedViews(monitor);
    }

    @NotNull
    @Association
    @Override
    public Collection<OracleTableIndex> getIndexes(DBRProgressMonitor monitor) throws DBException {
        cacheIndexes(monitor);
        return super.getIndexes(monitor);
    }

    @NotNull
    @Association
    @Override
    public Collection<OracleSchemaTrigger> getTriggers(DBRProgressMonitor monitor) throws DBException {
        cacheSchemaTriggers(monitor);
        return super.getTriggers(monitor);
    }

    @NotNull
    @Association
    @Override
    public Collection<OracleTableTrigger> getTableTriggers(DBRProgressMonitor monitor) throws DBException {
        cacheTableTriggers(monitor);
        return super.getTableTriggers(monitor);
    }

    @Association
    @Override
    public Collection<OracleSynonym> getSynonyms(DBRProgressMonitor monitor) throws DBException {
        cacheSynonyms(monitor);
        return super.getSynonyms(monitor);
    }

    @Association
    @Override
    public OracleSynonym getSynonym(DBRProgressMonitor monitor, String name) throws DBException {
        cacheSynonyms(monitor);
        return super.getSynonym(monitor, name);
    }

    @Association
    @Override
    public Collection<OracleProcedureStandalone> getProcedures(DBRProgressMonitor monitor) throws DBException {
        cacheProcedures(monitor);
        return super.getProcedures(monitor);
    }

    @Override
    public OracleProcedureStandalone getProcedure(DBRProgressMonitor monitor, String uniqueName) throws DBException {
        cacheProcedures(monitor);
        return super.getProcedure(monitor, uniqueName);
    }

    @Override
    public Collection<DBSObject> getChildren(@NotNull DBRProgressMonitor monitor) throws DBException {
        cacheTables(monitor);
        cachePackages(monitor);
        if (isSequencesAsChildrenEnabled()) {
            cacheSequences(monitor);
        }
        List<DBSObject> children = new ArrayList<>(tableCache.getAllObjects(monitor, this));
        if (isSynonymsAsChildrenEnabled()) {
            children.addAll(super.getSynonyms(monitor));
        }
        if (isSequencesAsChildrenEnabled()) {
            children.addAll(sequenceCache.getAllObjects(monitor, this));
        }
        children.addAll(packageCache.getAllObjects(monitor, this));
        return children;
    }

    @NotNull
    @Override
    public List<DBSObjectContainer> getPublicScopes(@NotNull DBRProgressMonitor monitor) {
        // Temporary workaround:
        // SQL semantic resolution can fall back to the Oracle public schema and
        // trigger Oracle-only metadata SQL. Keep Tibero lookups inside the active
        // Tibero schema until an Oracle hook allows a proper public schema override.
        return Collections.emptyList();
    }

    @Nullable
    @Override
    public DBSObject getChild(@NotNull DBRProgressMonitor monitor, @NotNull String childName) throws DBException {
        cacheTables(monitor);
        DBSObject child = tableCache.getObject(monitor, this, childName);
        if (child != null) {
            return child;
        }
        if (isSynonymsAsChildrenEnabled()) {
            child = super.getSynonym(monitor, childName);
            if (child != null) {
                return child;
            }
        }
        if (isSequencesAsChildrenEnabled()) {
            child = sequenceCache.getObject(monitor, this, childName);
            if (child != null) {
                return child;
            }
        }
        return packageCache.getObject(monitor, this, childName);
    }

    @Override
    public synchronized void cacheStructure(@NotNull DBRProgressMonitor monitor, int scope) throws DBException {
        cacheTables(monitor);
        cacheSequences(monitor);
        cachePackages(monitor);
        if ((scope & STRUCT_ASSOCIATIONS) != 0) {
            cacheIndexes(monitor);
            cacheTableTriggers(monitor);
        }
    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        sequenceCache.clearCache();
        packageCache.clearCache();
        return super.refreshObject(monitor);
    }

    private boolean isSynonymsAsChildrenEnabled() {
        DBPConnectionConfiguration cfg = getDataSource().getContainer().getConnectionConfiguration();
        return CommonUtils.getBoolean(cfg.getProviderProperty(OracleConstants.PROP_SEARCH_METADATA_IN_SYNONYMS));
    }

    private boolean isSequencesAsChildrenEnabled() {
        DBPConnectionConfiguration cfg = getDataSource().getContainer().getConnectionConfiguration();
        return CommonUtils.getBoolean(cfg.getProviderProperty(OracleConstants.PROP_SEARCH_METADATA_IN_SEQUENCES));
    }

    private class SequenceCache extends JDBCObjectCache<OracleSchema, OracleSequence> {
        @NotNull
        @Override
        protected JDBCPreparedStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull OracleSchema owner) throws SQLException {
            final JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT " + OracleUtils.getSysCatalogHint(owner.getDataSource()) + " * FROM " +
                OracleUtils.getAdminAllViewPrefix(session.getProgressMonitor(), owner.getDataSource(), "SEQUENCES") +
                " WHERE SEQUENCE_OWNER=? ORDER BY SEQUENCE_NAME");
            dbStat.setString(1, owner.getName());
            return dbStat;
        }

        @Override
        protected OracleSequence fetchObject(@NotNull JDBCSession session, @NotNull OracleSchema owner, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new TiberoSequence(owner, resultSet);
        }
    }

    private class PackageCache extends JDBCObjectCache<OracleSchema, OraclePackage> {
        @NotNull
        @Override
        protected JDBCPreparedStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull OracleSchema owner) throws SQLException {
            JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT " + OracleUtils.getSysCatalogHint(owner.getDataSource()) +
                " OBJECT_NAME, STATUS, CREATED, LAST_DDL_TIME, TEMPORARY FROM " +
                OracleUtils.getAdminAllViewPrefix(session.getProgressMonitor(), owner.getDataSource(), "OBJECTS") +
                " WHERE OBJECT_TYPE='PACKAGE' AND OWNER=? ORDER BY OBJECT_NAME");
            dbStat.setString(1, owner.getName());
            return dbStat;
        }

        @Override
        protected OraclePackage fetchObject(@NotNull JDBCSession session, @NotNull OracleSchema owner, @NotNull JDBCResultSet dbResult) throws SQLException, DBException {
            return new TiberoPackage(owner, dbResult);
        }
    }
}
