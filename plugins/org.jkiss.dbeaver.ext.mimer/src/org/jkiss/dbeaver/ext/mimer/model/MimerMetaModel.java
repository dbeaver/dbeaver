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
package org.jkiss.dbeaver.ext.mimer.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBDatabaseException;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericCatalog;
import org.jkiss.dbeaver.ext.generic.model.GenericContainerTrigger;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.ext.generic.model.GenericFunctionResultType;
import org.jkiss.dbeaver.ext.generic.model.GenericObjectContainer;
import org.jkiss.dbeaver.ext.generic.model.GenericProcedure;
import org.jkiss.dbeaver.ext.generic.model.GenericSchema;
import org.jkiss.dbeaver.ext.generic.model.GenericSequence;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.ext.generic.model.GenericSynonym;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.ext.generic.model.GenericTableColumn;
import org.jkiss.dbeaver.ext.generic.model.GenericTableIndex;
import org.jkiss.dbeaver.ext.generic.model.GenericTableTrigger;
import org.jkiss.dbeaver.ext.generic.model.GenericTrigger;
import org.jkiss.dbeaver.ext.generic.model.GenericUniqueKey;
import org.jkiss.dbeaver.ext.generic.model.GenericView;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModel;
import org.jkiss.dbeaver.ext.mimer.MimerConstants;
import org.jkiss.dbeaver.ext.mimer.model.plan.MimerQueryPlanner;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.exec.plan.DBCQueryPlanner;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCBasicDataTypeCache;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCDataType;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.rdb.DBSIndexType;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Mimer SQL meta model. Most of the catalog reading is inherited from {@link GenericMetaModel}
 * (JDBC {@code DatabaseMetaData}), since Mimer SQL sticks close to the standard there; this
 * class adds the reads JDBC doesn't expose: sequences, triggers, modules/domains, and native
 * DDL from {@code INFORMATION_SCHEMA.EXT_SOURCE_DEFINITION}.
 * <p>
 * This plugin's object model follows one convention throughout, worth knowing before browsing
 * further: each catalog object type (table, sequence, domain, ...) has up to three classes, one
 * per concern. A read-only model class lives here in {@code model} (usually a {@code Generic*}
 * subclass, e.g. {@link MimerTable}); an object that supports create/alter/drop also gets a
 * {@code DBEObjectManager} in the sibling {@code edit} package (e.g. {@code MimerTableManager});
 * an object whose create dialog needs more than a bare name also gets a create-dialog page in the
 * separate {@code org.jkiss.dbeaver.ext.mimer.ui} plugin's {@code config} package (e.g. {@code
 * MimerCreateSequencePage}). A purely read-only object simply has no {@code edit}/{@code ui}
 * counterpart.
 *
 * @author Mimer Information Technology
 */
public class MimerMetaModel extends GenericMetaModel {

    public MimerMetaModel() {
        super();
    }

    @NotNull
    @Override
    public GenericDataSource createDataSourceImpl(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBPDataSourceContainer container
    ) throws DBException {
        return new MimerDataSource(monitor, container, this);
    }

    @Override
    public GenericSchema createSchemaImpl(
        @NotNull GenericDataSource dataSource,
        @Nullable GenericCatalog catalog,
        @NotNull String schemaName
    ) throws DBException {
        return new MimerSchema(dataSource, catalog, schemaName);
    }

    /**
     * Adds Mimer SQL's {@code INTERVAL} type family to the Data Type list - see {@link MimerDataTypeCache}.
     */
    @Override
    public JDBCBasicDataTypeCache<GenericStructContainer, ? extends JDBCDataType> createDataTypeCache(
        @NotNull GenericStructContainer container
    ) {
        return new MimerDataTypeCache(container);
    }

    /**
     * Returns {@link MimerTable}/{@link MimerView} - unlike the plain generic classes, both
     * carry a "Privileges" folder (see {@link MimerObjectPrivilege}).
     * <p>
     * Also filters out {@code TABLE_TYPE = 'SYNONYM'} rows. The driver's {@code getTables()}
     * reports Mimer SQL synonyms this way, but a synonym owns no columns/constraints/indexes of
     * its own - its target does - so showing it as a table just produced a broken-looking empty
     * one. It already has its own real tree node (see {@link MimerSynonym}). Returning {@code
     * null} here is an intentional, cache-handled skip, same as {@link #createIndexImpl} below.
     */
    @Nullable
    @Override
    public GenericTableBase createTableOrViewImpl(
        @NotNull GenericStructContainer container,
        @Nullable String tableName,
        @Nullable String tableType,
        @Nullable JDBCResultSet dbResult
    ) {
        if ("SYNONYM".equalsIgnoreCase(tableType)) {
            return null;
        }
        if (tableType != null && isView(tableType)) {
            return new MimerView(container, tableName, tableType, dbResult);
        }
        return new MimerTable(container, tableName, tableType, dbResult);
    }

    /**
     * Returns {@link MimerTableColumn} - carries the {@code sequenceName} attribute the
     * "Auto Generated" checkbox needs.
     */
    @NotNull
    @Override
    public GenericTableColumn createTableColumnImpl(
        @NotNull DBRProgressMonitor monitor,
        @Nullable JDBCResultSet dbResult,
        @NotNull GenericTableBase table,
        String columnName,
        String typeName,
        int valueType,
        int sourceType,
        int ordinalPos,
        long columnSize,
        long charLength,
        Integer scale,
        Integer precision,
        int radix,
        boolean notNull,
        String remarks,
        String defaultValue,
        boolean autoIncrement,
        boolean autoGenerated
    ) {
        return new MimerTableColumn(table, columnName, typeName, valueType, sourceType, ordinalPos, columnSize,
            charLength, scale, precision, radix, notNull, remarks, defaultValue, autoIncrement, autoGenerated);
    }

    /**
     * The base class always returns {@code false}; without this, Mimer SQL's built-in schemas
     * stay visible as empty husks instead of being hidden by "Show system objects".
     */
    @Override
    public boolean isSystemSchema(GenericSchema schema) {
        return MimerConstants.SYSTEM_SCHEMAS.contains(schema.getName());
    }

    /**
     * Hooks the SQL Editor's "Explain Execution Plan" action up to {@link MimerQueryPlanner}.
     */
    @Nullable
    @Override
    public DBCQueryPlanner getQueryPlanner(@NotNull GenericDataSource dataSource) {
        return new MimerQueryPlanner((MimerDataSource) dataSource);
    }

    ///////////////////////////////////////////////
    // Sequences

    @Override
    public boolean supportsSequences(@NotNull GenericDataSource dataSource) {
        return true;
    }

    @Override
    public JDBCStatement prepareSequencesLoadStatement(
        @NotNull JDBCSession session,
        @NotNull GenericStructContainer container
    ) throws SQLException {
        // INFORMATION_SCHEMA.SEQUENCES has no start-value column on Mimer SQL 10.1, and neither
        // version's SEQUENCES has a databank column - both only exist on the extension view
        // EXT_SEQUENCES (INITIAL_VALUE, DATABANK_NAME). 10.1 queries EXT_SEQUENCES directly
        // (aliasing INITIAL_VALUE to START_VALUE; it has no CYCLE_OPTION, which correctly reads
        // as "not cycling" via safeGetStringTrimmed returning null); 11.0+ stays on SEQUENCES for
        // START_VALUE/CYCLE_OPTION and left-joins EXT_SEQUENCES just for DATABANK_NAME.
        boolean modern = !(container.getDataSource() instanceof MimerDataSource ds) || ds.supportsModernSequenceSyntax();
        JDBCPreparedStatement dbStat = session.prepareStatement(
            modern ?
            "SELECT S.SEQUENCE_NAME, S.SEQUENCE_SCHEMA, S.DATA_TYPE, S.START_VALUE, S.MINIMUM_VALUE, S.MAXIMUM_VALUE, S.INCREMENT, S.CYCLE_OPTION, E.DATABANK_NAME\n" +
            "FROM INFORMATION_SCHEMA.SEQUENCES S\n" +
            "LEFT JOIN INFORMATION_SCHEMA.EXT_SEQUENCES E\n" +
            "  ON E.SEQUENCE_SCHEMA = S.SEQUENCE_SCHEMA AND E.SEQUENCE_NAME = S.SEQUENCE_NAME\n" +
            "WHERE S.SEQUENCE_SCHEMA = ?\n" +
            "ORDER BY S.SEQUENCE_NAME" :
            "SELECT SEQUENCE_NAME, SEQUENCE_SCHEMA, DATA_TYPE, INITIAL_VALUE AS START_VALUE, MINIMUM_VALUE, MAXIMUM_VALUE, INCREMENT, DATABANK_NAME\n" +
            "FROM INFORMATION_SCHEMA.EXT_SEQUENCES\n" +
            "WHERE SEQUENCE_SCHEMA = ?\n" +
            "ORDER BY SEQUENCE_NAME");
        dbStat.setString(1, container.getName());
        return dbStat;
    }

    @Override
    public GenericSequence createSequenceImpl(
        @NotNull JDBCSession session,
        @NotNull GenericStructContainer container,
        @NotNull JDBCResultSet dbResult
    ) {
        if (CommonUtils.isEmpty(JDBCUtils.safeGetString(dbResult, "SEQUENCE_NAME"))) {
            return null;
        }
        return new MimerSequence(container, dbResult);
    }

    ///////////////////////////////////////////////
    // Synonyms

    @Override
    public boolean supportsSynonyms(@NotNull GenericDataSource dataSource) {
        return true;
    }

    @Override
    public JDBCStatement prepareSynonymsLoadStatement(
        @NotNull JDBCSession session,
        @NotNull GenericStructContainer container
    ) throws SQLException {
        // Only table-like targets - EXT_SYNONYMS has no column for any other object kind.
        JDBCPreparedStatement dbStat = session.prepareStatement(
            "SELECT SYNONYM_NAME, TABLE_SCHEMA, TABLE_NAME\n" +
            "FROM INFORMATION_SCHEMA.EXT_SYNONYMS\n" +
            "WHERE SYNONYM_SCHEMA = ?\n" +
            "ORDER BY SYNONYM_NAME");
        dbStat.setString(1, container.getName());
        return dbStat;
    }

    @Override
    public GenericSynonym createSynonymImpl(
        @NotNull JDBCSession session,
        @NotNull GenericStructContainer container,
        @NotNull JDBCResultSet dbResult
    ) {
        return new MimerSynonym(container, dbResult);
    }

    ///////////////////////////////////////////////
    // Indexes

    /**
     * Returns {@link MimerTableIndex}, and filters out the internal structure backing a primary
     * or foreign key - the driver's {@code getIndexInfo()} reports those like real indexes, but
     * Mimer SQL doesn't treat them as such; they show up instead under "Access Path" (see
     * {@link MimerAccessPath}). Returning {@code null} is an intentional, cache-handled skip.
     * Uses a throwaway {@code VoidProgressMonitor} since this override has none of its own -
     * safe because {@link MimerTable#isStandaloneIndex} caches its query per table.
     */
    @Nullable
    @Override
    public GenericTableIndex createIndexImpl(
        @NotNull GenericTableBase table,
        boolean nonUnique,
        String qualifier,
        long cardinality,
        String indexName,
        DBSIndexType indexType,
        boolean persisted
    ) {
        if (table instanceof MimerTable mimerTable
            && CommonUtils.isNotEmpty(indexName)
            && !mimerTable.isStandaloneIndex(new VoidProgressMonitor(), indexName)
        ) {
            return null;
        }
        return new MimerTableIndex(table, nonUnique, qualifier, cardinality, indexName, indexType, persisted);
    }

    ///////////////////////////////////////////////
    // Tables / views

    /**
     * The driver's {@code getTables()} reports every object in Mimer SQL's built-in schemas
     * (INFORMATION_SCHEMA/SYSTEM/MIMER) as TABLE_TYPE "SYSTEM TABLE", losing the real
     * VIEW/TABLE distinction ({@link #isView} then never sees "VIEW"). Read those three
     * schemas' {@code TABLES} catalog view directly instead; every other schema stays on
     * the normal JDBC metadata path.
     */
    @Override
    public JDBCStatement prepareTableLoadStatement(
        @NotNull JDBCSession session,
        @NotNull GenericStructContainer owner,
        @Nullable GenericTableBase object,
        @Nullable String objectName
    ) throws SQLException {
        if (!MimerConstants.SYSTEM_SCHEMAS.contains(owner.getName())) {
            return super.prepareTableLoadStatement(session, owner, object, objectName);
        }
        String name = object != null ? object.getName() : objectName;
        JDBCPreparedStatement dbStat = session.prepareStatement(
            "SELECT CAST(NULL AS VARCHAR(128)) AS TABLE_CAT, TABLE_SCHEMA AS TABLE_SCHEM, TABLE_NAME, TABLE_TYPE,\n" +
            "       CAST(NULL AS VARCHAR(1)) AS REMARKS\n" +
            "FROM INFORMATION_SCHEMA.TABLES\n" +
            "WHERE TABLE_SCHEMA = ?\n" +
            (name != null ? "  AND TABLE_NAME = ?\n" : "") +
            "ORDER BY TABLE_NAME");
        dbStat.setString(1, owner.getName());
        if (name != null) {
            dbStat.setString(2, name);
        }
        return dbStat;
    }

    /**
     * Reading these schemas' catalog directly (see {@link #prepareTableLoadStatement}) makes
     * their objects report a plain "VIEW"/"TABLE" type, so the base class's type-string check
     * would stop flagging them as system objects. Flag by schema membership instead.
     */
    @Override
    public boolean isSystemTable(@NotNull GenericTableBase table) {
        return MimerConstants.SYSTEM_SCHEMAS.contains(table.getSchema().getName()) || super.isSystemTable(table);
    }

    /**
     * Generic's own table/view/column managers already build the correct
     * {@code COMMENT ON ...} DDL, gated by these two hooks (both default {@code false}) -
     * turning them on is all that's needed. Covers Views for free too, since
     * {@code GenericView extends GenericTableBase}.
     */
    @Override
    public boolean isTableCommentEditable() {
        return true;
    }

    @Override
    public boolean isTableColumnCommentEditable() {
        return true;
    }

    ///////////////////////////////////////////////
    // Constraints (unique / primary / check)

    @Override
    public boolean supportsUniqueKeys() {
        return true;
    }

    @Override
    public boolean supportsCheckConstraints() {
        return true;
    }

    @Override
    public JDBCStatement prepareUniqueConstraintsLoadStatement(
        @NotNull JDBCSession session,
        @NotNull GenericStructContainer owner,
        @Nullable GenericTableBase forParent
    ) throws SQLException {
        JDBCPreparedStatement dbStat = session.prepareStatement(
            "SELECT tc.CONSTRAINT_NAME AS PK_NAME, tc.CONSTRAINT_NAME, tc.CONSTRAINT_TYPE, tc.TABLE_NAME,\n" +
            "       kcu.COLUMN_NAME, kcu.ORDINAL_POSITION AS KEY_SEQ,\n" +
            "       cc.CHECK_CLAUSE AS CHECK_EXPRESSION\n" +
            "FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS tc\n" +
            "LEFT JOIN INFORMATION_SCHEMA.KEY_COLUMN_USAGE kcu\n" +
            "  ON tc.CONSTRAINT_SCHEMA = kcu.CONSTRAINT_SCHEMA AND tc.CONSTRAINT_NAME = kcu.CONSTRAINT_NAME\n" +
            "LEFT JOIN INFORMATION_SCHEMA.CHECK_CONSTRAINTS cc\n" +
            "  ON tc.CONSTRAINT_SCHEMA = cc.CONSTRAINT_SCHEMA AND tc.CONSTRAINT_NAME = cc.CONSTRAINT_NAME\n" +
            "WHERE tc.CONSTRAINT_TYPE <> 'FOREIGN KEY' AND tc.CONSTRAINT_SCHEMA = ?\n" +
            (forParent != null ? "  AND tc.TABLE_NAME = ?\n" : "") +
            "ORDER BY tc.CONSTRAINT_NAME, kcu.ORDINAL_POSITION");
        dbStat.setString(1, owner.getName());
        if (forParent != null) {
            dbStat.setString(2, forParent.getName());
        }
        return dbStat;
    }

    @Override
    public DBSEntityConstraintType getUniqueConstraintType(@NotNull JDBCResultSet dbResult) throws DBException, SQLException {
        // Trimmed, not plain safeGetString - CONSTRAINT_TYPE is blank-padded (CHAR, not
        // VARCHAR), so "CHECK"/"UNIQUE" never matched the switch below untrimmed and silently
        // fell through to the PRIMARY_KEY default, mistyping every CHECK/UNIQUE constraint.
        String type = JDBCUtils.safeGetStringTrimmed(dbResult, "CONSTRAINT_TYPE");
        if (CommonUtils.isNotEmpty(type)) {
            return switch (type.toUpperCase()) {
                case "UNIQUE" -> DBSEntityConstraintType.UNIQUE_KEY;
                case "CHECK" -> DBSEntityConstraintType.CHECK;
                default -> DBSEntityConstraintType.PRIMARY_KEY;
            };
        }
        return super.getUniqueConstraintType(dbResult);
    }

    @NotNull
    @Override
    public GenericUniqueKey createConstraintImpl(
        @NotNull GenericTableBase table,
        String constraintName,
        DBSEntityConstraintType constraintType,
        JDBCResultSet dbResult,
        boolean persisted
    ) {
        String checkClause = dbResult == null ? null : JDBCUtils.safeGetString(dbResult, "CHECK_EXPRESSION");
        return new MimerTableConstraint(table, constraintName, null, constraintType, persisted, checkClause);
    }

    ///////////////////////////////////////////////
    // Triggers

    @Override
    public boolean supportsTriggers(@NotNull GenericDataSource dataSource) {
        return true;
    }

    @Override
    public boolean supportsDatabaseTriggers(@NotNull GenericDataSource dataSource) {
        return true;
    }

    @NotNull
    @Override
    public JDBCStatement prepareTableTriggersLoadStatement(
        @NotNull JDBCSession session,
        @NotNull GenericStructContainer container,
        @Nullable GenericTableBase forParent
    ) throws SQLException {
        JDBCPreparedStatement dbStat = session.prepareStatement(
            "SELECT TRIGGER_NAME, EVENT_OBJECT_TABLE AS OWNER, ACTION_TIMING, EVENT_MANIPULATION, ACTION_ORIENTATION\n" +
            "FROM INFORMATION_SCHEMA.TRIGGERS\n" +
            "WHERE TRIGGER_SCHEMA = ?\n" +
            (forParent != null ? "  AND EVENT_OBJECT_TABLE = ?\n" : "") +
            "ORDER BY EVENT_OBJECT_TABLE, TRIGGER_NAME");
        dbStat.setString(1, container.getName());
        if (forParent != null) {
            dbStat.setString(2, forParent.getName());
        }
        return dbStat;
    }

    @NotNull
    @Override
    public GenericTableTrigger createTableTriggerImpl(
        @NotNull JDBCSession session,
        @NotNull GenericStructContainer container,
        @NotNull GenericTableBase parent,
        String triggerName,
        @NotNull JDBCResultSet dbResult
    ) {
        if (CommonUtils.isEmpty(triggerName)) {
            triggerName = JDBCUtils.safeGetString(dbResult, "TRIGGER_NAME");
        }
        return new MimerTableTrigger(parent, triggerName, null);
    }

    @Override
    public List<? extends GenericTrigger> loadTriggers(
        @NotNull DBRProgressMonitor monitor,
        @NotNull GenericStructContainer container,
        @Nullable GenericTableBase table
    ) throws DBException {
        if (table == null) {
            return Collections.emptyList();
        }
        try (JDBCSession session = DBUtils.openMetaSession(monitor, container, "Read Mimer SQL triggers")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT TRIGGER_NAME FROM INFORMATION_SCHEMA.TRIGGERS\n" +
                "WHERE TRIGGER_SCHEMA = ? AND EVENT_OBJECT_TABLE = ?\n" +
                "ORDER BY TRIGGER_NAME")
            ) {
                dbStat.setString(1, table.getSchema().getName());
                dbStat.setString(2, table.getName());
                List<GenericTrigger> result = new ArrayList<>();
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    while (dbResult.next()) {
                        result.add(new MimerTableTrigger(table, JDBCUtils.safeGetString(dbResult, 1), null));
                    }
                }
                return result;
            }
        } catch (SQLException e) {
            throw new DBDatabaseException(e, container.getDataSource());
        }
    }

    @Override
    public JDBCStatement prepareContainerTriggersLoadStatement(
        @NotNull JDBCSession session,
        @Nullable GenericStructContainer forParent
    ) throws SQLException {
        JDBCPreparedStatement dbStat = session.prepareStatement(
            "SELECT TRIGGER_NAME FROM INFORMATION_SCHEMA.TRIGGERS\n" +
            (forParent != null ? "WHERE TRIGGER_SCHEMA = ?\n" : "") +
            "ORDER BY TRIGGER_NAME");
        if (forParent != null) {
            dbStat.setString(1, forParent.getName());
        }
        return dbStat;
    }

    @Override
    public GenericTrigger createContainerTriggerImpl(
        @NotNull GenericStructContainer container,
        @NotNull JDBCResultSet dbResult
    ) {
        String name = JDBCUtils.safeGetString(dbResult, "TRIGGER_NAME");
        if (CommonUtils.isEmpty(name)) {
            return null;
        }
        return new GenericContainerTrigger(container, name, null);
    }

    ///////////////////////////////////////////////
    // Native DDL

    /**
     * Unlike Procedure/Function/Trigger/Module, {@code EXT_SOURCE_DEFINITION} for {@code
     * OBJECT_TYPE = 'VIEW'} stores only the view's query body, not the full {@code CREATE VIEW}
     * statement, so the header is built here instead. The explicit column list is
     * technically optional per {@code CREATE_VIEW.htm} (omitting it inherits the source columns'
     * own names), but is spelled out here to match real Mimer SQL DDL dumps and because it's the
     * only way to rename a view's columns when the underlying query has none (an expression with
     * no correlation name) or duplicate source names. Columns are read from the view's own
     * already-loaded attributes rather than a second query. Without this header, the Source tab's
     * Save would re-execute the bare {@code SELECT} as a query instead of DDL, so edits would
     * never actually persist back to the view.
     */
    @Override
    public String getViewDDL(
        @NotNull DBRProgressMonitor monitor,
        @NotNull GenericView sourceObject,
        @NotNull Map<String, Object> options
    ) throws DBException {
        String body = MimerUtils.readSourceDefinition(
            monitor, sourceObject, sourceObject.getContainer().getName(), sourceObject.getName(),
            MimerConstants.SOURCE_TYPE_VIEW, "OBJECT_NAME");

        StringBuilder columns = new StringBuilder();
        for (GenericTableColumn column : sourceObject.getAttributes(monitor)) {
            if (columns.length() > 0) {
                columns.append(", ");
            }
            columns.append('"').append(column.getName()).append('"');
        }

        return "CREATE VIEW \"" + sourceObject.getContainer().getName() + "\".\"" + sourceObject.getName() + "\""
            + " (" + columns + ")\nAS " + body;
    }

    /**
     * Mimer SQL keeps procedures and functions in separate namespaces, so they can legitimately
     * share a specific name. Without this, the base class's "broken driver" heuristic treats
     * that overlap as duplicate results and silently truncates the procedure list.
     */
    @Override
    public boolean supportsEqualFunctionsAndProceduresNames() {
        return true;
    }

    /**
     * Two cleanup passes over the driver's raw routine results, both cross-checked against
     * {@code INFORMATION_SCHEMA.ROUTINES}.
     * <p>
     * First: a routine declared inside a {@code CREATE MODULE} shows up in the driver's results
     * exactly like a standalone one, but it should only appear nested under that module (see
     * {@code MimerModule#getRoutineCache()}). Anything with {@code MODULE_NAME IS NOT NULL} is
     * removed from the flat list.
     * <p>
     * Second: the driver's own {@code getProcedures()} call can misreport a routine that's
     * really only a function - for example, the implicit constructor/cast functions that {@code
     * CREATE TYPE ... AS ...} creates (a {@code timeit} domain over {@code TIME} yields
     * functions named {@code TIME} and {@code timeit}, with no real {@code PROCEDURE}-type row
     * for either in {@code ROUTINES}). These would otherwise be double-listed under both
     * Procedures and Functions: {@link #supportsEqualFunctionsAndProceduresNames()} - needed for
     * the legitimate case of a real procedure and a real function sharing a specific name - also
     * disables the base loader's own duplicate-driver-result detection, which would otherwise
     * have caught this. So a {@code PROCEDURE}-typed entry is only kept if {@code ROUTINES}
     * shows a genuine {@code PROCEDURE}-type row for its specific name - checking for the
     * presence of a real procedure row, not the absence of a function row, so a legitimately
     * shared specific name isn't wrongly dropped.
     */
    @Override
    public void loadProcedures(@NotNull DBRProgressMonitor monitor, @NotNull GenericObjectContainer container) throws DBException {
        super.loadProcedures(monitor, container);
        List<GenericProcedure> procedures = container.getProcedureCache();
        if (CommonUtils.isEmpty(procedures)) {
            return;
        }
        boolean supportsExternal = container.getDataSource() instanceof MimerDataSource ds && ds.supportsExternalLibraries();
        Set<String> moduleRoutineNames = new HashSet<>();
        Set<String> realProcedureNames = new HashSet<>();
        // Keyed by both ROUTINE_NAME and SPECIFIC_NAME, same as moduleRoutineNames/
        // realProcedureNames below - a MimerProcedure's own getUniqueName() falls back to the
        // plain name whenever no specific name was ever assigned.
        Map<String, MimerExternalRoutineInfo> externalInfoByName = new HashMap<>();
        try (JDBCSession session = DBUtils.openMetaSession(monitor, container, "Read Mimer SQL routine types")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT ROUTINE_NAME, SPECIFIC_NAME, ROUTINE_TYPE, MODULE_NAME" +
                (supportsExternal ? ", ROUTINE_BODY, EXTERNAL_LANGUAGE, EXTERNAL_NAME, EXTERNAL_LIBRARY" : "") + "\n" +
                "FROM INFORMATION_SCHEMA.ROUTINES\n" +
                "WHERE ROUTINE_SCHEMA = ?")
            ) {
                dbStat.setString(1, container.getName());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    while (dbResult.next()) {
                        String routineName = JDBCUtils.safeGetString(dbResult, "ROUTINE_NAME");
                        String specificName = JDBCUtils.safeGetString(dbResult, "SPECIFIC_NAME");
                        if (JDBCUtils.safeGetStringTrimmed(dbResult, "MODULE_NAME") != null) {
                            moduleRoutineNames.add(routineName);
                            moduleRoutineNames.add(specificName);
                        }
                        if ("PROCEDURE".equalsIgnoreCase(JDBCUtils.safeGetStringTrimmed(dbResult, "ROUTINE_TYPE"))) {
                            realProcedureNames.add(routineName);
                            realProcedureNames.add(specificName);
                        }
                        if (supportsExternal && "EXTERNAL".equalsIgnoreCase(JDBCUtils.safeGetStringTrimmed(dbResult, "ROUTINE_BODY"))) {
                            MimerExternalRoutineInfo info = new MimerExternalRoutineInfo(
                                true,
                                JDBCUtils.safeGetStringTrimmed(dbResult, "EXTERNAL_LANGUAGE"),
                                JDBCUtils.safeGetString(dbResult, "EXTERNAL_NAME"),
                                JDBCUtils.safeGetStringTrimmed(dbResult, "EXTERNAL_LIBRARY"));
                            externalInfoByName.put(routineName, info);
                            externalInfoByName.put(specificName, info);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            throw new DBDatabaseException(e, container.getDataSource());
        }
        procedures.removeIf(p ->
            moduleRoutineNames.contains(p.getName()) || moduleRoutineNames.contains(p.getUniqueName()) ||
            (p.getProcedureType() == DBSProcedureType.PROCEDURE
                && !realProcedureNames.contains(p.getName()) && !realProcedureNames.contains(p.getUniqueName())));
        if (supportsExternal) {
            for (GenericProcedure p : procedures) {
                if (p instanceof MimerProcedure mp) {
                    MimerExternalRoutineInfo info = externalInfoByName.get(mp.getUniqueName());
                    mp.setExternalInfo(info != null ? info : MimerExternalRoutineInfo.NOT_EXTERNAL);
                }
            }
        }
    }

    @NotNull
    @Override
    public GenericProcedure createProcedureImpl(
        @NotNull GenericStructContainer container,
        @NotNull String procedureName,
        String specificName,
        String remarks,
        @NotNull DBSProcedureType procedureType,
        GenericFunctionResultType functionResultType
    ) {
        return new MimerProcedure(container, procedureName, specificName, remarks, procedureType, functionResultType);
    }

    @Override
    public String getProcedureDDL(
        @NotNull DBRProgressMonitor monitor,
        @NotNull GenericProcedure sourceObject
    ) throws DBException {
        String schemaName = sourceObject.getContainer().getName();
        String externalSource = MimerUtils.buildExternalRoutineSource(
            monitor, sourceObject, schemaName, sourceObject.getName(), sourceObject.getUniqueName());
        if (externalSource != null) {
            return externalSource;
        }
        String type = sourceObject.getProcedureType() == DBSProcedureType.FUNCTION
            ? MimerConstants.SOURCE_TYPE_FUNCTION : MimerConstants.SOURCE_TYPE_PROCEDURE;
        // Keyed by SPECIFIC_NAME, not OBJECT_NAME since Mimer SQL allows overloading
        // a procedure/function name by parameter list, and EXT_SOURCE_DEFINITION carries one row
        // (or set of LINE_NUMBER rows) per overload, distinguished only by SPECIFIC_NAME.
        return MimerUtils.readSourceDefinition(
            monitor, sourceObject, schemaName, sourceObject.getUniqueName(),
            type, "SPECIFIC_NAME");
    }

    @Override
    public String getTriggerDDL(
        @NotNull DBRProgressMonitor monitor,
        @NotNull GenericTrigger sourceObject
    ) throws DBException {
        // A table trigger's container is its table; a schema trigger's container is the schema.
        String schemaName = sourceObject.getContainer() instanceof GenericTableBase table
            ? table.getSchema().getName()
            : sourceObject.getContainer().getName();
        return MimerUtils.readSourceDefinition(
            monitor, sourceObject, schemaName, sourceObject.getName(),
            MimerConstants.SOURCE_TYPE_TRIGGER, "OBJECT_NAME");
    }

    // getAutoIncrementClause() is intentionally not overridden - Mimer SQL has no identity-column
    // syntax; auto-generated values use a sequence + DEFAULT NEXT VALUE FOR instead.
}
