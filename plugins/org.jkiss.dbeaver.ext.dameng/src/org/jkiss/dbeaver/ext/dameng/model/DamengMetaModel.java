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

package org.jkiss.dbeaver.ext.dameng.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBDatabaseException;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.dameng.DamengConstants;
import org.jkiss.dbeaver.ext.generic.model.*;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModel;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaObject;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCBasicDataTypeCache;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCDataType;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author Shengkai Bai
 */
public class DamengMetaModel extends GenericMetaModel {

    private static final Log log = Log.getLog(DamengMetaModel.class);

    public DamengMetaModel() {
        super();
    }

    @Override
    public GenericDataSource createDataSourceImpl(DBRProgressMonitor monitor, DBPDataSourceContainer container) throws DBException {
        return new DamengDataSource(monitor, container, this);
    }

    @Override
    public JDBCBasicDataTypeCache<GenericStructContainer, ? extends JDBCDataType> createDataTypeCache(@NotNull GenericStructContainer container) {
        return new DamengDataTypeCache(container);
    }

    @Override
    public DamengSchema createSchemaImpl(@NotNull GenericDataSource dataSource, GenericCatalog catalog, @NotNull String schemaName) throws DBException {
        return new DamengSchema(dataSource, schemaName, true);
    }

    @Override
    public GenericTableBase createTableOrViewImpl(GenericStructContainer container, String tableName, String tableType, JDBCResultSet dbResult) {
        if (tableType != null && isView(tableType)) {
            return new GenericView(
                    container,
                    tableName,
                    tableType,
                    dbResult);
        }
        return new DamengTable(container, tableName, tableType, dbResult);
    }

    @Override
    public GenericTableBase createTableImpl(@NotNull JDBCSession session, @NotNull GenericStructContainer owner, @NotNull GenericMetaObject tableObject, @NotNull JDBCResultSet dbResult) {
        return super.createTableImpl(session, owner, tableObject, dbResult);
    }

    @Override
    public boolean supportsSequences(@NotNull GenericDataSource dataSource) {
        return true;
    }

    @Override
    public JDBCStatement prepareSequencesLoadStatement(JDBCSession session, GenericStructContainer container) throws SQLException {
        JDBCPreparedStatement dbStat = session.prepareStatement("SELECT " +
            "SEQ_OBJ.NAME, " +
            "SEQ_OBJ.INFO4 AS INCREMENT, " +
            "SF_SEQUENCE_GET_MAX(SCH_OBJ.NAME,SEQ_OBJ.NAME) AS MAX_VALUE," +
            "SF_SEQUENCE_GET_MIN(SCH_OBJ.NAME,SEQ_OBJ.NAME) AS MIN_VALUE," +
            "SF_SEQ_CURRVAL(SCH_OBJ.NAME,SEQ_OBJ.NAME) AS LAST_VALUE, " +
            "SF_SEQUENCE_GET_CACHE_NUM(SCH_OBJ.NAME,SEQ_OBJ.NAME) CACHE_SIZE," +
            "SEQ_OBJ.INFO1 & 0x0000FF IS_CYCLE," +
            "SEQ_OBJ.INFO1 & 0x00FF00 IS_ORDER," +
            "SEQ_OBJ.INFO1 & 0xFF0000 IS_CACHE" +
            "\nFROM " +
            "SYSOBJECTS SEQ_OBJ, " +
            "SYSOBJECTS SCH_OBJ " +
            "\nWHERE SEQ_OBJ.SCHID = SCH_OBJ.ID " +
            "AND SEQ_OBJ.SUBTYPE$ = 'SEQ' " +
            "AND SCH_OBJ.NAME = ?");
        dbStat.setString(1, container.getName());
        return dbStat;
    }

    @Override
    public GenericSequence createSequenceImpl(
        @NotNull JDBCSession session,
        @NotNull GenericStructContainer container,
        @NotNull JDBCResultSet dbResult
    ) {
        String name = JDBCUtils.safeGetString(dbResult, 1);
        if (CommonUtils.isEmpty(name)) {
            return null;
        }
        long lastValue = JDBCUtils.safeGetLong(dbResult, "LAST_VALUE");
        long incrementBy = JDBCUtils.safeGetLong(dbResult, "INCREMENT");
        long minValue = JDBCUtils.safeGetLong(dbResult, "MIN_VALUE");
        long maxValue = JDBCUtils.safeGetLong(dbResult, "MAX_VALUE");
        return new DamengSequence(container, name, null, lastValue, minValue, maxValue, incrementBy, dbResult);
    }

    @Override
    public boolean supportsDatabaseTriggers(@NotNull GenericDataSource dataSource) {
        return true;
    }

    @Override
    public boolean supportsTriggers(@NotNull GenericDataSource dataSource) {
        return true;
    }


    @Override
    public JDBCStatement prepareTableTriggersLoadStatement(
        JDBCSession session,
        @NotNull GenericStructContainer container,
        GenericTableBase table
    ) throws SQLException {
        JDBCPreparedStatement dbStat = session.prepareStatement("SELECT " +
                "TABTRIG_OBJ_INNER.NAME AS TRIGGER_NAME, TAB_OBJ_INNER.NAME AS OWNER,* " +
                "FROM " +
                "SYSOBJECTS TABTRIG_OBJ_INNER, " +
                "SYSOBJECTS TAB_OBJ_INNER " +
                "WHERE " +
                "TABTRIG_OBJ_INNER.SUBTYPE$ = 'TRIG' " +
                "AND TABTRIG_OBJ_INNER.PID = TAB_OBJ_INNER.ID " + (table != null ? "AND TAB_OBJ_INNER.NAME= ? " : ""));
        if (table != null) {
            dbStat.setString(1, table.getName());
        }
        return dbStat;
    }

    @Override
    public GenericTrigger createTableTriggerImpl(JDBCSession session, GenericStructContainer genericStructContainer, GenericTableBase genericTableBase, String triggerName, JDBCResultSet resultSet) throws DBException {
        if (CommonUtils.isEmpty(triggerName)) {
            triggerName = JDBCUtils.safeGetString(resultSet, "TRIGGER_NAME");
        }
        return new GenericTableTrigger(genericTableBase, triggerName, null);
    }

    @Override
    public JDBCStatement prepareContainerTriggersLoadStatement(JDBCSession session, GenericStructContainer forParent) throws SQLException {
        JDBCPreparedStatement dbStat = session.prepareStatement("SELECT TABTRIG_OBJ_INNER.NAME " +
                "FROM " +
                "SYSOBJECTS TABTRIG_OBJ_INNER, " +
                "SYSOBJECTS SCH_OBJ_INNER " +
                "WHERE " +
                "TABTRIG_OBJ_INNER.SUBTYPE$ = 'TRIG' " +
                "AND TABTRIG_OBJ_INNER.PID = SCH_OBJ_INNER.ID " +
                "AND SCH_OBJ_INNER.NAME = ?");
        dbStat.setString(1, forParent.getName());
        return dbStat;
    }

    @Override
    public GenericTrigger createContainerTriggerImpl(GenericStructContainer container, JDBCResultSet resultSet) throws DBException {
        String name = JDBCUtils.safeGetStringTrimmed(resultSet, DamengConstants.NAME);
        if (name == null) {
            return null;
        }
        return new GenericContainerTrigger(container, name, null);
    }

    @Override
    public String getTriggerDDL(DBRProgressMonitor monitor, GenericTrigger sourceObject) throws DBException {
        if (sourceObject.getContainer() instanceof DamengTable) {
            return DamengUtils.getDDL(monitor, sourceObject, DamengConstants.ObjectType.TRIGGER, ((DamengTable) sourceObject.getContainer()).getContainer().getName());
        }
        return DamengUtils.getDDL(monitor, sourceObject, DamengConstants.ObjectType.TRIGGER, sourceObject.getContainer().getName());
    }

    @Override
    public String getViewDDL(@NotNull DBRProgressMonitor monitor, @NotNull GenericView sourceObject, @NotNull Map<String, Object> options) throws DBException {
        return DamengUtils.getDDL(monitor, sourceObject, DamengConstants.ObjectType.VIEW, sourceObject.getParentObject().getName());
    }

    @Override
    public DamengTableColumn createTableColumnImpl(@NotNull DBRProgressMonitor monitor, JDBCResultSet dbResult, @NotNull GenericTableBase table, String columnName, String typeName, int valueType, int sourceType, int ordinalPos, long columnSize, long charLength, Integer scale, Integer precision, int radix, boolean notNull, String remarks, String defaultValue, boolean autoIncrement, boolean autoGenerated) throws DBException {
        return new DamengTableColumn(table, columnName, typeName, valueType, sourceType, ordinalPos, columnSize,
                charLength, scale, precision, radix, notNull, remarks, defaultValue, autoIncrement, autoGenerated);
    }

    @Override
    public List<? extends GenericTrigger> loadTriggers(DBRProgressMonitor monitor, GenericStructContainer container, GenericTableBase table) throws DBException {
        if (table == null) {
            return Collections.emptyList();
        }
        try (JDBCSession session = DBUtils.openMetaSession(monitor, container, "Read triggers")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement("SELECT " +
                    "TABTRIG_OBJ_INNER.NAME " +
                    "FROM " +
                    "SYSOBJECTS TABTRIG_OBJ_INNER, " +
                    "SYSOBJECTS SCH_OBJ_INNER " +
                    "WHERE " +
                    "TABTRIG_OBJ_INNER.SUBTYPE$ = 'TRIG' " +
                    "AND TABTRIG_OBJ_INNER.SCHID = SCH_OBJ_INNER.ID " +
                    "AND SCH_OBJ_INNER.NAME = ? " +
                    "AND TABTRIG_OBJ_INNER.NAME = ?")) {
                dbStat.setString(1, table.getSchema().getName());
                dbStat.setString(2, table.getName());
                List<GenericTrigger> result = new ArrayList<>();
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    while (dbResult.next()) {
                        String name = JDBCUtils.safeGetString(dbResult, 1);
                        result.add(new GenericTableTrigger(table, name, null));
                    }
                }
                return result;
            }
        } catch (SQLException e) {
            throw new DBDatabaseException(e, container.getDataSource());
        }
    }

    @Override
    public void loadProcedures(DBRProgressMonitor monitor, @NotNull GenericObjectContainer container) throws DBException {
        GenericDataSource dataSource = container.getDataSource();
        try (JDBCSession session = DBUtils.openMetaSession(monitor, container, "Read Dameng procedure source")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                    "SELECT\n" +
                            "PROC_OBJ.ID,\n" +
                            "PROC_OBJ.NAME,\n" +
                            "PROC_OBJ.CRTDATE,\n" +
                            "PROC_OBJ.INFO1,\n" +
                            "PROC_OBJ.VALID\n" +
                            "FROM\n" +
                            "SYSOBJECTS PROC_OBJ,\n" +
                            "SYSOBJECTS SCH_OBJ,\n" +
                            "SYSOBJECTS USER_OBJ\n" +
                            "WHERE\n" +
                            "PROC_OBJ.SCHID = SCH_OBJ.ID\n" +
                            "AND SCH_OBJ.PID = USER_OBJ.ID\n" +
                            "AND PROC_OBJ.SUBTYPE$ = 'PROC'\n" +
                            "AND SF_CHECK_PRIV_OPT(UID(),\n" +
                            "CURRENT_USERTYPE(),\n" +
                            "PROC_OBJ.ID,\n" +
                            "USER_OBJ.ID,\n" +
                            "USER_OBJ.INFO1,\n" +
                            "PROC_OBJ.ID) = 1\n" +
                            "AND SCH_OBJ.NAME = ? ")) {
                dbStat.setString(1, container.getName());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    while (dbResult.nextRow()) {
                        DBSProcedureType routineType = DBSProcedureType.PROCEDURE;
                        try {
                            int type = JDBCUtils.safeGetInt(dbResult, "INFO1");
                            routineType = switch (type) {
                                case 1 -> DBSProcedureType.PROCEDURE;
                                case 0 -> DBSProcedureType.FUNCTION;
                                default -> routineType;
                            };
                        } catch (IllegalArgumentException e) {
                            log.warn(e);
                        }
                        final GenericProcedure procedure = createProcedureImpl(
                                container,
                                JDBCUtils.safeGetString(dbResult, "NAME"),
                                JDBCUtils.safeGetString(dbResult, "NAME"),
                                null,
                                routineType,
                                null);
                        container.addProcedure(procedure);
                    }
                }
            }
        } catch (SQLException e) {
            throw new DBDatabaseException(e, dataSource);
        }
    }

    @Override
    public String getProcedureDDL(DBRProgressMonitor monitor, GenericProcedure sourceObject) throws DBException {
        DBSProcedureType procedureType = sourceObject.getProcedureType();
        DamengConstants.ObjectType objectType = switch (procedureType) {
            case PROCEDURE -> DamengConstants.ObjectType.PROCEDURE;
            case FUNCTION -> DamengConstants.ObjectType.FUNCTION;
            case UNKNOWN -> null;
        };
        return DamengUtils.getDDL(monitor, sourceObject, objectType, sourceObject.getContainer().getName());
    }

    @Override
    public String getAutoIncrementClause(GenericTableColumn column) {
        return "AUTO_INCREMENT";
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
    public boolean isColumnNotNullByDefault() {
        return true;
    }

}
