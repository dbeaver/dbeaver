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

    private static final String[] VALID_TABLE_TYPES = { "TABLE", "VIEW", "SYSTEM TABLE" };

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
                String query = "SELECT procid, procname, isproc, specificname, type, procflags, paramtypes::LVARCHAR AS columntypenames,"
                        + " mode FROM sysprocedures ";
                dbState = session.prepareStatement(query);
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
        return prepareTSObjectLoadStatement(session, container, null, "%", new String[] { "SYNONYM" });
    }

    @Override
    public JDBCStatement prepareTableLoadStatement(@NotNull JDBCSession session, @NotNull GenericStructContainer owner,
            @Nullable GenericTableBase object, @Nullable String objectName) throws SQLException {
        return prepareTSObjectLoadStatement(session, owner, object, objectName, VALID_TABLE_TYPES);
    }

    @Override
    public JDBCStatement prepareTableTriggersLoadStatement(@NotNull JDBCSession session,
            @NotNull GenericStructContainer container, @Nullable GenericTableBase table) throws SQLException {
        String query = "SELECT T1.trigname as TRIGGER_NAME, T1.*, T2.tabname AS OWNER FROM systriggers AS T1, systables AS T2 "
                + "WHERE T2.tabid = T1.tabid " + (table != null ? "AND T2.tabname = ?" : "");

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
        String schemaPattern = owner.getSchema() == null || DBUtils.isVirtualObject(owner.getSchema()) ? null
                : JDBCUtils.escapeWildCards(session, owner.getSchema().getName());
        boolean isOracleMode = GBase8sUtils.isOracleSqlMode(owner.getDataSource().getContainer());

        return session.getMetaData().getTables(isOracleMode ? catalog : schemaPattern,
                isOracleMode ? schemaPattern : catalog, tableNamePattern, types).getSourceStatement();
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
    public boolean isTrimObjectNames() {
        return true;
    }
}