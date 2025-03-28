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

package org.jkiss.dbeaver.ext.gbase8s.model.meta;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBDatabaseException;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.gbase8s.GBase8sUtils;
import org.jkiss.dbeaver.ext.gbase8s.model.GBase8sCatalog;
import org.jkiss.dbeaver.ext.gbase8s.model.GBase8sDataTypeCache;
import org.jkiss.dbeaver.ext.gbase8s.model.GBase8sProcedure;
import org.jkiss.dbeaver.ext.gbase8s.model.GBase8sSchema;
import org.jkiss.dbeaver.ext.gbase8s.model.GBase8sSynonym;
import org.jkiss.dbeaver.ext.gbase8s.model.GBase8sTable;
import org.jkiss.dbeaver.ext.gbase8s.model.GBase8sTableColumn;
import org.jkiss.dbeaver.ext.gbase8s.model.GBase8sTableTrigger;
import org.jkiss.dbeaver.ext.generic.GenericConstants;
import org.jkiss.dbeaver.ext.generic.model.GenericCatalog;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.ext.generic.model.GenericFunctionResultType;
import org.jkiss.dbeaver.ext.generic.model.GenericObjectContainer;
import org.jkiss.dbeaver.ext.generic.model.GenericProcedure;
import org.jkiss.dbeaver.ext.generic.model.GenericSchema;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.ext.generic.model.GenericSynonym;
import org.jkiss.dbeaver.ext.generic.model.GenericTable;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.ext.generic.model.GenericTableColumn;
import org.jkiss.dbeaver.ext.generic.model.GenericTableTrigger;
import org.jkiss.dbeaver.ext.generic.model.GenericTrigger;
import org.jkiss.dbeaver.ext.generic.model.GenericUtils;
import org.jkiss.dbeaver.ext.generic.model.GenericView;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModel;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCBasicDataTypeCache;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCDataType;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSObjectFilter;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;
import org.jkiss.utils.CommonUtils;

/**
 * @author Chao Tian
 */
public class GBase8sMetaModel extends GenericMetaModel {

    private static final Log log = Log.getLog(GBase8sMetaModel.class);

    // TABLE_TYPE: String {@code =>} table type. ('TABLE', 'VIEW', 'SYSTEM TABLE')
    private static final String[] VALID_TABLE_TYPES = { "T", "V" };
    // TABLE_TYPE: String {@code =>} table type. ('SYNONYM')
    private static final String[] VALID_SYNONYM_TYPES = { "S" };

    public GBase8sMetaModel() {
        super();
    }

    /**
     * Catalog
     */
    @Override
    public GenericCatalog createCatalogImpl(@NotNull GenericDataSource dataSource, @NotNull String catalogName) {
        return new GBase8sCatalog(dataSource, catalogName);
    }

    @Override
    public JDBCBasicDataTypeCache<GenericStructContainer, ? extends JDBCDataType> createDataTypeCache(
            @NotNull GenericStructContainer container) {
        return new GBase8sDataTypeCache(container);
    }

    /**
     * Procedure
     */
    public GenericProcedure createProcedureImpl(GenericStructContainer container, String procedureName,
            String specificName, String remarks, DBSProcedureType procedureType,
            GenericFunctionResultType functionResultType) {
        return new GBase8sProcedure(container, procedureName, specificName, remarks, procedureType, functionResultType);
    }

    /**
     * Schema
     */
    @Override
    public GenericSchema createSchemaImpl(@NotNull GenericDataSource dataSource, @Nullable GenericCatalog catalog,
            @NotNull String schemaName) throws DBException {
        return new GBase8sSchema(dataSource, catalog, schemaName);
    }

    /**
     * Synonym
     */
    @Override
    public GenericSynonym createSynonymImpl(@NotNull JDBCSession session, @NotNull GenericStructContainer container,
            @NotNull JDBCResultSet dbResult) throws DBException {
        String name = JDBCUtils.safeGetString(dbResult, "TABLE_NAME");
        String description = JDBCUtils.safeGetString(dbResult, "REMARKS");
        return new GBase8sSynonym(container, name, description, dbResult);
    }

    @Override
    public GenericTableColumn createTableColumnImpl(@NotNull DBRProgressMonitor monitor,
            @Nullable JDBCResultSet dbResult, @NotNull GenericTableBase table, String columnName, String typeName,
            int valueType, int sourceType, int ordinalPos, long columnSize, long charLength, Integer scale,
            Integer precision, int radix, boolean notNull, String remarks, String defaultValue, boolean autoIncrement,
            boolean autoGenerated) throws DBException {
        return new GBase8sTableColumn(table, columnName, typeName, valueType, sourceType, ordinalPos, columnSize,
                charLength, scale, precision, radix, notNull, remarks, defaultValue, autoIncrement, autoGenerated);
    }

    /**
     * Table/View
     */
    @Override
    public GenericTableBase createTableOrViewImpl(GenericStructContainer container, @Nullable String tableName,
            @Nullable String tableType, @Nullable JDBCResultSet dbResult) {
        if (tableType != null && isView(tableType)) {
            return new GenericView(container, tableName, tableType, dbResult);
        }
        return new GBase8sTable(container, tableName, tableType, dbResult);
    }

    @Override
    public GenericTableTrigger createTableTriggerImpl(@NotNull JDBCSession session,
            @NotNull GenericStructContainer container, @NotNull GenericTableBase genericTableBase, String triggerName,
            @NotNull JDBCResultSet resultSet) {
        if (CommonUtils.isEmpty(triggerName)) {
            triggerName = JDBCUtils.safeGetString(resultSet, "TRIGGER_NAME");
        }
        if (triggerName == null) {
            return null;
        }
        triggerName = triggerName.trim();
        return new GBase8sTableTrigger(genericTableBase, triggerName, resultSet);
    }

    @Override
    public String getProcedureDDL(DBRProgressMonitor monitor, GenericProcedure sourceObject) throws DBException {
        return GBase8sUtils.getProcedureSource(monitor, sourceObject);
    }

    @Override
    public String getTableDDL(@NotNull DBRProgressMonitor monitor, @NotNull GenericTableBase sourceObject,
            @NotNull Map<String, Object> options) throws DBException {
        String tableDDL = super.getTableDDL(monitor, sourceObject, options);
        // Triggers, Serials
        return tableDDL + GBase8sUtils.getTriggerDDL(monitor, sourceObject);
    }

    @Override
    public String getTriggerDDL(@NotNull DBRProgressMonitor monitor, @NotNull GenericTrigger trigger)
            throws DBException {
        return GBase8sUtils.getTriggerDDL(monitor, trigger);
    }

    public String getViewDDL(@NotNull DBRProgressMonitor monitor, @NotNull GenericView sourceObject,
            @NotNull Map<String, Object> options) throws DBException {
        return GBase8sUtils.getViewDDL(monitor, sourceObject);
    }

    @Override
    public void loadProcedures(DBRProgressMonitor monitor, @NotNull GenericObjectContainer container)
            throws DBException {

        Map<String, GenericProcedure> funcMap = new LinkedHashMap<>();

        JDBCPreparedStatement dbState = null;
        JDBCResultSet dbResult = null;

        GenericDataSource dataSource = container.getDataSource();
        GenericMetaObject procObject = dataSource.getMetaObject(GenericConstants.OBJECT_PROCEDURE);
        try (JDBCSession session = DBUtils.openMetaSession(monitor, container, "Load procedures")) {
            if (hasProcedureSupport()) {
                // Read procedures/functions
                String sql = """
                        SELECT procid, procname, isproc, specificname, type, procflags, paramtypes::LVARCHAR AS columntypenames, mode
                        FROM sysprocedures
                        """;
                dbState = session.prepareStatement(sql);
                dbResult = dbState.executeQuery();

                while (dbResult.next()) {
                    if (monitor.isCanceled()) {
                        break;
                    }
                    String procedureName = GenericUtils.safeGetStringTrimmed(procObject, dbResult, "procname");
                    String specificName = GenericUtils.safeGetStringTrimmed(procObject, dbResult, "specificname");
                    String isProc = GenericUtils.safeGetString(procObject, dbResult, "isproc");
                    String procType = GenericUtils.safeGetString(procObject, dbResult, "type");
                    // procedureType ( 0:Procedure/Function 1:Package Specification, 2:Package Body,
                    // 3:Stored Procedure, 4:Stored Function )
                    if (!"0".equalsIgnoreCase(procType)) {
                        continue;
                    }
                    if (container.hasProcedure(procedureName)) {
                        // Seems to be a duplicate
                        continue;
                    }
                    DBSProcedureType procedureType = switch (isProc) {
                    case "t" -> DBSProcedureType.PROCEDURE;
                    case "f" -> hasFunctionSupport() ? DBSProcedureType.FUNCTION : DBSProcedureType.UNKNOWN;
                    case "u" -> DBSProcedureType.PROCEDURE;
                    default -> DBSProcedureType.UNKNOWN;
                    };
                    if (CommonUtils.isEmpty(specificName)) {
                        specificName = procedureName;
                    }
                    GenericProcedure function = funcMap.get(procedureName);
                    if (function != null && !supportsEqualFunctionsAndProceduresNames()) {
                        // Broken driver
                        log.debug("Broken driver [" + session.getDataSource().getContainer().getDriver().getName()
                                + "] - returns the same list for getProcedures and getFunctons");
                        break;
                    }

                    final GenericProcedure procedure = createProcedureImpl(container, procedureName, specificName, null,
                            procedureType,
                            DBSProcedureType.FUNCTION.equals(procedureType) ? GenericFunctionResultType.TABLE : null);
                    container.addProcedure(procedure);
                }
            }

        } catch (SQLException e) {
            throw new DBDatabaseException(e, dataSource);
        }
    }

    @Override
    public List<GBase8sTableTrigger> loadTriggers(DBRProgressMonitor monitor, @NotNull GenericStructContainer container,
            @Nullable GenericTableBase table) throws DBException {
        assert table != null;
        try (JDBCSession session = DBUtils.openMetaSession(monitor, container, "Read triggers")) {
            String query = "SELECT T1.trigname FROM systriggers AS T1, systables AS T2 WHERE T2.tabid = T1.tabid AND T2.tabname = ?";
            try (JDBCPreparedStatement dbStat = session.prepareStatement(query)) {
                dbStat.setString(1, table.getName());
                List<GBase8sTableTrigger> result = new ArrayList<>();

                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    while (dbResult.next()) {
                        String name = JDBCUtils.safeGetString(dbResult, 1);
                        if (name == null) {
                            continue;
                        }
                        result.add(new GBase8sTableTrigger(table, name.trim(), dbResult));
                    }
                }
                return result;
            }
        } catch (SQLException e) {
            throw new DBDatabaseException(e, container.getDataSource());
        }
    }

    @Override
    public JDBCStatement prepareSynonymsLoadStatement(@NotNull JDBCSession session,
            @NotNull GenericStructContainer container) throws SQLException {
        return prepareTSObjectLoadStatement(session, container, null, "%", VALID_SYNONYM_TYPES);
    }

    @Override
    public JDBCStatement prepareTableLoadStatement(@NotNull JDBCSession session, @NotNull GenericStructContainer owner,
            @Nullable GenericTableBase object, @Nullable String objectName) throws SQLException {
        return prepareTSObjectLoadStatement(session, owner, object, objectName, VALID_TABLE_TYPES);
    }

    @Override
    public JDBCStatement prepareTableColumnLoadStatement(
            @NotNull JDBCSession session,
            @NotNull GenericStructContainer owner,
            @Nullable GenericTableBase forTable) throws SQLException {
        String tableName = forTable == null ? owner.getDataSource().getAllObjectsPattern()
                : JDBCUtils.escapeWildCards(session, forTable.getName());
        String catalog = owner.getCatalog() == null ? null : owner.getCatalog().getName();
        String schema = owner.getSchema() == null || DBUtils.isVirtualObject(owner.getSchema()) ? null
                : JDBCUtils.escapeWildCards(session, owner.getSchema().getName());
        boolean isOracleMode = GBase8sUtils.isOracleSqlMode(owner.getDataSource().getContainer());
        String ownerPattern = "%s%s".formatted(isOracleMode ? schema : catalog, isOracleMode ? "." : ":");
        String sql = """
                SELECT
                    t.tabname::VARCHAR(128) AS TABLE_NAME,
                    c.colname::VARCHAR(128) AS COLUMN_NAME,
                    get_data_type(c.coltype, c.extended_id, 0)::SMALLINT AS DATA_TYPE,
                    0::SMALLINT AS SOURCE_DATA_TYPE,
                    schema_coltypename(c.coltype, c.extended_id)::VARCHAR(128) AS TYPE_NAME,
                    schema_precision(c.coltype, c.extended_id, c.collength)::INTEGER AS COLUMN_SIZE,
                    schema_numscale(c.coltype, c.collength)::INTEGER AS DECIMAL_DIGITS,
                    schema_numprecradix(c.coltype)::INTEGER AS NUM_PREC_RADIX,
                    CASE d.type
                        WHEN 'L' THEN get_default_value(c.coltype, c.extended_id, c.collength, d.default::lvarchar(256))::VARCHAR(254)
                        WHEN 'E' THEN read_defaultstr(c.tabid, c.colno, c.coltype, c.collength, c.extended_id)::VARCHAR(32731)
                        WHEN 'C' THEN 'current' || replace(get_colname(c.coltype, c.collength, c.extended_id, 1), schema_coltypename(c.coltype, c.extended_id), '')::VARCHAR(254)
                        WHEN 'S' THEN 'dbservername'::VARCHAR(254)
                        WHEN 'U' THEN 'user'::VARCHAR(254)
                        WHEN 'T' THEN 'today'::VARCHAR(254)
                        WHEN 'N' THEN 'null'::VARCHAR(10)
                        ELSE null::VARCHAR(254)
                    END AS COLUMN_DEF,
                    cc.comments AS REMARKS,
                    schema_isnullable(c.coltype)::INTEGER AS NULLABLE,
                    schema_nullable(c.coltype)::VARCHAR(3) AS ISNULLABLE,
                    c.coltype::INTEGER AS SQL_DATA_TYPE,
                    schema_datetype(c.coltype, c.collength)::INTEGER AS DB_DATA_TYPE,
                    schema_charlen(c.coltype, c.extended_id, c.collength)::INTEGER AS CHAR_OCTET_LENGTH,
                    c.colno::integer AS ORDINAL_POSITION,
                    schema_isautoincr(c.coltype)::VARCHAR(3) AS IS_AUTOINCREMENT,
                    null::VARCHAR(254) AS IS_GENERATEDCOLUMN,
                    c.extended_id::INTEGER AS EXTENDED_ID,
                    c.colattr::INTEGER AS COLATTR,
                    c.coltype
                FROM
                    %ssystables t,
                    OUTER %ssysdefaults d,
                    %ssyscolumns c,
                    OUTER %ssyscolcomms cc
                WHERE
                    t.tabid = c.tabid
                    AND d.tabid = t.tabid
                    AND c.colno = d.colno
                    AND t.tabid = cc.tabid
                    AND c.colno = cc.colno
                    AND t.tabname = ?
                """
                .formatted(ownerPattern, ownerPattern, ownerPattern, ownerPattern);
        JDBCPreparedStatement dbStat = session.prepareStatement(sql);
        dbStat.setString(1, tableName);
        return dbStat;
    }

    @Override
    public JDBCStatement prepareTableTriggersLoadStatement(@NotNull JDBCSession session,
            @NotNull GenericStructContainer container, @Nullable GenericTableBase table) throws SQLException {
        String query = """
                SELECT T1.trigname as TRIGGER_NAME, T1.*, T2.tabname AS OWNER
                FROM systriggers AS T1, systables AS T2
                WHERE T2.tabid = T1.tabid
                %s
                """.formatted(table != null ? "AND T2.tabname = ?" : "");
        JDBCPreparedStatement dbStat = session.prepareStatement(query);
        if (table != null) {
            dbStat.setString(1, table.getName());
        }
        return dbStat;
    }

    JDBCStatement prepareTSObjectLoadStatement(@NotNull JDBCSession session, @NotNull GenericStructContainer owner,
            @Nullable GenericTableBase object, @Nullable String objectName, @Nullable String[] types)
            throws SQLException {
        String tableNamePattern;
        if (object == null && objectName == null) {
            final DBSObjectFilter tableFilters = session.getDataSource().getContainer()
                    .getObjectFilter(GenericTable.class, owner, false);

            if (tableFilters != null && tableFilters.hasSingleMask()) {
                tableNamePattern = tableFilters.getSingleMask();
                if (!CommonUtils.isEmpty(tableNamePattern)) {
                    tableNamePattern = SQLUtils.makeSQLLike(tableNamePattern);
                }
            } else {
                tableNamePattern = owner.getDataSource().getAllObjectsPattern();
            }
        } else {
            tableNamePattern = JDBCUtils.escapeWildCards(session, (object != null ? object.getName() : objectName));
        }

        String catalog = owner.getCatalog() == null ? null : owner.getCatalog().getName();
        String schema = owner.getSchema() == null || DBUtils.isVirtualObject(owner.getSchema()) ? null
                : JDBCUtils.escapeWildCards(session, owner.getSchema().getName());
        boolean isOracleMode = GBase8sUtils.isOracleSqlMode(owner.getDataSource().getContainer());
        String ownerPattern = "%s%s".formatted(isOracleMode ? schema : catalog, isOracleMode ? "." : ":");
        String sql = """
                SELECT t.tabid, t.tabname AS TABLE_NAME, t.owner AS
                    %s,
                    CASE
                        WHEN t.tabtype = 'T' AND t.tabid <= (SELECT tabid FROM systables WHERE trim(tabname) = 'VERSION') THEN 'SYSTEM TABLE'
                        WHEN t.tabtype = 'V' AND t.tabid <= (SELECT tabid FROM systables WHERE trim(tabname) = 'VERSION') THEN 'SYSTEM VIEW'
                        WHEN t.tabtype = 'T' THEN 'TABLE'
                        WHEN t.tabtype = 'V' THEN 'VIEW'
                        ELSE 'SYNONYM'
                    END AS TABLE_TYPE,
                    c.comments AS REMARKS
                FROM %ssystables t
                LEFT JOIN %ssyscomms c ON t.tabid = c.tabid
                WHERE t.tabname LIKE ?
                %s
                """
                .formatted(isOracleMode ? "TABLE_SCHEM" : "TABLE_CATALOG", ownerPattern, ownerPattern,
                        (types != null
                                ? " AND t.tabtype IN (" + String.join(", ", Collections.nCopies(types.length, "?"))
                                        + ")"
                                : ""));
        JDBCPreparedStatement dbStat = session.prepareStatement(sql);
        int paramIndex = 1;
        dbStat.setString(paramIndex++, tableNamePattern);
        if (types != null) {
            for (String type : types) {
                dbStat.setString(paramIndex++, type);
            }
        }
        return dbStat;
    }

    @Override
    public boolean supportNestedForeignKeys() {
        return false;
    }

    @Override
    public boolean supportsSynonyms(@NotNull GenericDataSource dataSource) {
        return false;
    }

    @Override
    public boolean supportsTriggers(@NotNull GenericDataSource dataSource) {
        return true;
    }

    @Override
    public boolean hasFunctionSupport() {
        return true;
    }

    @Override
    public boolean isTableCommentEditable() {
        return true;
    }

    @Override
    public boolean isTableColumnCommentEditable() {
        return true;
    }

    @Override
    public boolean isTrimObjectNames() {
        return true;
    }
}