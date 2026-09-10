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
package org.jkiss.dbeaver.ext.informix.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBDatabaseException;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.generic.model.*;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModel;
import org.jkiss.dbeaver.ext.informix.InformixUtils;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCConstants;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.rdb.DBSForeignKeyModifyRule;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * InformixDataSource
 */
public class InformixMetaModel extends GenericMetaModel
{
    private static final Log log = Log.getLog(InformixMetaModel.class);

    public InformixMetaModel() {
        super();
    }

    public String getViewDDL(
        @NotNull DBRProgressMonitor monitor,
        @NotNull GenericView sourceObject,
        @NotNull Map<String, Object> options) throws DBException {
        return InformixUtils.getViewSource(monitor, sourceObject);
    }

    @Override
    public String getProcedureDDL(@NotNull DBRProgressMonitor monitor, @NotNull GenericProcedure sourceObject) throws DBException {
        return InformixUtils.getProcedureSource(monitor, (InformixProcedure) sourceObject);
    }
    
    @Override
    public String getTableDDL(
        @NotNull DBRProgressMonitor monitor,
        @NotNull GenericTableBase sourceObject,
        @NotNull Map<String, Object> options) throws DBException {
    	String tableDDL = super.getTableDDL(monitor, sourceObject, options);
    	// Triggers, Serials
    	// 
    	return tableDDL + InformixUtils.getTriggerDDL(monitor, sourceObject);
    }

    @Override
    public boolean supportsTriggers(@NotNull GenericDataSource dataSource) {
        return true;
    }

    @NotNull
    @Override
    public JDBCStatement prepareTableTriggersLoadStatement(@NotNull JDBCSession session, @NotNull GenericStructContainer container, @Nullable GenericTableBase table) throws SQLException {
        String query = "SELECT T1.trigname as TRIGGER_NAME, T1.*, T2.tabname AS OWNER FROM informix.systriggers AS T1, informix.systables AS T2 \n" +
                        "WHERE T2.tabid = T1.tabid " + (table != null ? "AND T2.tabname = ?" : "");

        JDBCPreparedStatement dbStat = session.prepareStatement(query);
        if (table != null) {
            dbStat.setString(1, table.getName());
        }

        return dbStat;
    }

    @NotNull
    @Override
    public GenericTableTrigger createTableTriggerImpl(@NotNull JDBCSession session, @NotNull GenericStructContainer container, @NotNull GenericTableBase genericTableBase, String triggerName, @NotNull JDBCResultSet resultSet) {
        if (CommonUtils.isEmpty(triggerName)) {
            triggerName = JDBCUtils.safeGetString(resultSet, "TRIGGER_NAME");
        }
        if (triggerName == null) {
            return null;
        }
        triggerName = triggerName.trim();
        return new InformixTrigger(genericTableBase, triggerName, resultSet);
    }

    @Override
    public List<InformixTrigger> loadTriggers(@NotNull DBRProgressMonitor monitor, @NotNull GenericStructContainer container, @Nullable GenericTableBase table) throws DBException {
        assert table != null;
        try (JDBCSession session = DBUtils.openMetaSession(monitor, container, "Read triggers")) {
            String query =
                "SELECT T1.trigname \n" +
                "FROM informix.systriggers AS T1, informix.systables AS T2 \n" +
                "WHERE T2.tabid = T1.tabid AND T2.tabname = ?";

            try (JDBCPreparedStatement dbStat = session.prepareStatement(query)) {
                dbStat.setString(1, table.getName());
                List<InformixTrigger> result = new ArrayList<>();

                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    while (dbResult.next()) {
                        String name = JDBCUtils.safeGetString(dbResult, 1);
                        if (name == null) {
                            continue;
                        }
                        name = name.trim();
                        InformixTrigger trigger = new InformixTrigger(table, name, dbResult);
                        result.add(trigger);
                    }
                }
                return result;
            }
        } catch (SQLException e) {
            throw new DBDatabaseException(e, container.getDataSource());
        }
    }

    @Override
    public boolean supportsTableDDLSplit(@NotNull GenericTableBase sourceObject) {
        return false;
    }

    @Override
    public boolean supportNestedForeignKeys() {
        return false;
    }

    @Override
    public boolean isFKConstraintWordDuplicated() {
        return true;
    }

    @Override
    public String generateOnDeleteFK(@NotNull DBSForeignKeyModifyRule deleteRule) {
        if (deleteRule != null && deleteRule.getId().equals("CASCADE")) {
            return "ON DELETE CASCADE";
        }
        return null;
    }

    @Override
    public String generateOnUpdateFK(@NotNull DBSForeignKeyModifyRule updateRule) {
        return null;
    }

    @Override
    public String getTriggerDDL(@NotNull DBRProgressMonitor monitor, @NotNull GenericTrigger trigger) throws DBException {
        return InformixUtils.getTriggerDDL(monitor, trigger);
    }

    @Override
    public boolean hasFunctionSupport() {
        return false;
    }

    @Override
    public boolean isTrimObjectNames() {
        // Some old drivers can return object names with spaces around. And we can't create names with spaces. So let's trim them.
        return true;
    }

    @Override
    public void loadProcedures(@NotNull DBRProgressMonitor monitor, @NotNull GenericObjectContainer container) throws DBException {
        // The Informix JDBC driver reports the correct set of user routines through
        // getProcedures()/getFunctions() (built-in/system routines are hidden), but it
        // collapses overloaded routines into a single row and never exposes procid.
        // So use the JDBC metadata to learn which routines are user-visible, then enrich
        // each one from informix.sysprocedures to create a separate node per overload
        // (identified by procid, with its own parameter signature).
        GenericDataSource dataSource = container.getDataSource();
        String schemaName = container.getSchema() == null || DBUtils.isVirtualObject(container.getSchema())
            ? null : container.getSchema().getName();
        try (JDBCSession session = DBUtils.openMetaSession(monitor, container, "Load Informix procedures")) {
            Map<String, DBSProcedureType> routineNames = readRoutineNames(monitor, session, container, schemaName);
            Map<String, List<InformixProcedureMeta>> overloads = readProcedureOverloads(session, schemaName);
            for (Map.Entry<String, DBSProcedureType> entry : routineNames.entrySet()) {
                if (monitor.isCanceled()) {
                    break;
                }
                String procName = entry.getKey();
                List<InformixProcedureMeta> metas = overloads.get(procName);
                if (CommonUtils.isEmpty(metas)) {
                    // Not found in sysprocedures - fall back to a single node without procid.
                    container.addProcedure(new InformixProcedure(
                        container, -1, procName, procName, "", null, entry.getValue(), null));
                    continue;
                }
                for (InformixProcedureMeta meta : metas) {
                    String specificName = procName + "(" + meta.paramTypes + ")";
                    container.addProcedure(new InformixProcedure(
                        container, meta.procid, procName, specificName, meta.paramTypes, null, meta.procedureType, null));
                }
            }
        } catch (SQLException e) {
            throw new DBDatabaseException(e, dataSource);
        }
    }

    // Reads the user-visible routine names (and their type) via JDBC metadata. This honors
    // the driver's filtering, so system built-ins do not leak into the navigator.
    private Map<String, DBSProcedureType> readRoutineNames(
        DBRProgressMonitor monitor,
        JDBCSession session,
        GenericObjectContainer container,
        String schemaName
    ) throws SQLException {
        Map<String, DBSProcedureType> result = new LinkedHashMap<>();
        String catalog = container.getCatalog() == null ? null : container.getCatalog().getName();
        String schemaPattern = schemaName == null ? null : JDBCUtils.escapeWildCards(session, schemaName);
        String pattern = container.getDataSource().getAllObjectsPattern();
        if (hasFunctionSupport()) {
            try (JDBCResultSet dbResult = session.getMetaData().getFunctions(catalog, schemaPattern, pattern)) {
                while (dbResult.next()) {
                    if (monitor.isCanceled()) {
                        break;
                    }
                    String name = JDBCUtils.safeGetString(dbResult, JDBCConstants.FUNCTION_NAME);
                    if (!CommonUtils.isEmpty(name)) {
                        result.putIfAbsent(name.trim(), DBSProcedureType.FUNCTION);
                    }
                }
            } catch (Throwable e) {
                // getFunctions() is optional in JDBC and may not be implemented by the driver.
                log.debug("Can't read Informix functions", e);
            }
        }
        if (hasProcedureSupport()) {
            try (JDBCResultSet dbResult = session.getMetaData().getProcedures(catalog, schemaPattern, pattern)) {
                while (dbResult.next()) {
                    if (monitor.isCanceled()) {
                        break;
                    }
                    String name = JDBCUtils.safeGetString(dbResult, JDBCConstants.PROCEDURE_NAME);
                    if (!CommonUtils.isEmpty(name)) {
                        result.putIfAbsent(name.trim(), DBSProcedureType.PROCEDURE);
                    }
                }
            }
        }
        return result;
    }

    // Maps routine name -> overloads (procid + parameter signature + type) from sysprocedures.
    private Map<String, List<InformixProcedureMeta>> readProcedureOverloads(
        JDBCSession session,
        String schemaName
    ) throws SQLException {
        Map<String, List<InformixProcedureMeta>> overloads = new HashMap<>();
        StringBuilder sql = new StringBuilder(
            "SELECT procid, TRIM(procname) AS procname, paramtypes, " +
            // isproc/internal are BOOLEAN columns - compare against the boolean literals
            // and project a plain char so the driver returns a clean 't'/'f' string.
            "CASE WHEN isproc = 't' THEN 't' ELSE 'f' END AS isproc " +
            "FROM informix.sysprocedures " +
            "WHERE internal = 'f'");
        if (schemaName != null) {
            sql.append(" AND TRIM(owner) = ?");
        }
        sql.append(" ORDER BY procname, procid");
        try (JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString())) {
            if (schemaName != null) {
                dbStat.setString(1, schemaName);
            }
            try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                while (dbResult.next()) {
                    String procname = JDBCUtils.safeGetString(dbResult, "procname");
                    if (CommonUtils.isEmpty(procname)) {
                        continue;
                    }
                    procname = procname.trim();
                    String paramtypes = JDBCUtils.safeGetString(dbResult, "paramtypes");
                    String trimmedParams = paramtypes == null ? "" : paramtypes.trim();
                    int procid = JDBCUtils.safeGetInt(dbResult, "procid");
                    String isproc = JDBCUtils.safeGetString(dbResult, "isproc");
                    DBSProcedureType procedureType = "t".equalsIgnoreCase(CommonUtils.notEmpty(isproc).trim())
                        ? DBSProcedureType.PROCEDURE
                        : DBSProcedureType.FUNCTION;
                    overloads.computeIfAbsent(procname, k -> new ArrayList<>())
                        .add(new InformixProcedureMeta(procid, trimmedParams, procedureType));
                }
            }
        }
        return overloads;
    }

    private static class InformixProcedureMeta {
        final int procid;
        final String paramTypes;
        final DBSProcedureType procedureType;

        InformixProcedureMeta(int procid, String paramTypes, DBSProcedureType procedureType) {
            this.procid = procid;
            this.paramTypes = paramTypes;
            this.procedureType = procedureType;
        }
    }
}