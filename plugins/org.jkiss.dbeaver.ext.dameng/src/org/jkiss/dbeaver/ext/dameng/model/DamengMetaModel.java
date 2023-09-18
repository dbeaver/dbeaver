/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
import org.jkiss.dbeaver.DBException;
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

    public DamengMetaModel() {
        super();
    }

    @Override
    public GenericDataSource createDataSourceImpl(DBRProgressMonitor monitor, DBPDataSourceContainer container) throws DBException {
        return new DamengDataSource(monitor, container, this);
    }

    @Override
    public JDBCBasicDataTypeCache<GenericStructContainer, ? extends JDBCDataType> createDataTypeCache(GenericStructContainer container) {
        return new DamengDataTypeCache(container);
    }

    @Override
    public DamengSchema createSchemaImpl(GenericDataSource dataSource, GenericCatalog catalog, String schemaName) throws DBException {
        return new DamengSchema(dataSource, catalog, schemaName);
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
    public GenericTableBase createTableImpl(JDBCSession session, GenericStructContainer owner, GenericMetaObject tableObject, JDBCResultSet dbResult) {
        return super.createTableImpl(session, owner, tableObject, dbResult);
    }

    @Override
    public boolean supportsSequences(GenericDataSource dataSource) {
        return true;
    }

    @Override
    public JDBCStatement prepareSequencesLoadStatement(JDBCSession session, GenericStructContainer container) throws SQLException {
        JDBCPreparedStatement dbStat = session.prepareStatement("SELECT " +
                "SEQ_OBJ.NAME, " +
                "SEQ_OBJ.INFO4 AS INCREMENT, " +
                "SF_SEQUENCE_GET_MAX(SCH_OBJ.NAME,SEQ_OBJ.NAME) AS MAX_VALUE," +
                "SF_SEQUENCE_GET_MIN(SCH_OBJ.NAME,SEQ_OBJ.NAME) AS MIN_VALUE," +
                "SF_SEQ_CURRVAL(SCH_OBJ.NAME,SEQ_OBJ.NAME) AS LAST_VALUE " +
                "FROM " +
                "SYSOBJECTS SEQ_OBJ, " +
                "SYSOBJECTS SCH_OBJ " +
                "WHERE SEQ_OBJ.SCHID = SCH_OBJ.ID " +
                "AND SEQ_OBJ.SUBTYPE$ = 'SEQ' " +
                "AND SCH_OBJ.NAME = ?");
        dbStat.setString(1, container.getName());
        return dbStat;
    }

    @Override
    public GenericSequence createSequenceImpl(JDBCSession session, GenericStructContainer container, JDBCResultSet dbResult) throws DBException {
        String name = JDBCUtils.safeGetString(dbResult, 1);
        if (CommonUtils.isEmpty(name)) {
            return null;
        }
        Number lastValue = JDBCUtils.safeGetBigDecimal(dbResult, "LAST_VALUE");
        Number incrementBy = JDBCUtils.safeGetBigDecimal(dbResult, "INCREMENT");
        Number minValue = JDBCUtils.safeGetBigDecimal(dbResult, "MIN_VALUE");
        Number maxValue = JDBCUtils.safeGetBigDecimal(dbResult, "MAX_VALUE");
        return new GenericSequence(container, name, "", lastValue, minValue, maxValue, incrementBy);
    }

    @Override
    public boolean supportsDatabaseTriggers(GenericDataSource dataSource) {
        return true;
    }

    @Override
    public boolean supportsTriggers(@NotNull GenericDataSource dataSource) {
        return true;
    }


    @Override
    public JDBCStatement prepareTableTriggersLoadStatement(JDBCSession session, GenericStructContainer container, GenericTableBase table) throws SQLException {
        JDBCPreparedStatement dbStat = session.prepareStatement("SELECT " +
                "TABTRIG_OBJ_INNER.NAME,TAB_OBJ_INNER.NAME,* " +
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
    public JDBCStatement prepareContainerTriggersLoadStatement(JDBCSession session, GenericStructContainer forParent) throws SQLException {
        JDBCPreparedStatement dbStat = session.prepareStatement("SELECT TABTRIG_OBJ_INNER.NAME " +
                "FROM " +
                "SYSOBJECTS TABTRIG_OBJ_INNER, " +
                "SYSOBJECTS SCH_OBJ_INNER " +
                "WHERE " +
                "TABTRIG_OBJ_INNER.SUBTYPE$ = 'TRIG' " +
                "AND TABTRIG_OBJ_INNER.SCHID = SCH_OBJ_INNER.ID " +
                "AND SCH_OBJ_INNER.NAME = ?");
        dbStat.setString(1, forParent.getName());
        return dbStat;
    }

    @Override
    public GenericTrigger createContainerTriggerImpl(GenericStructContainer container, JDBCResultSet resultSet) throws DBException {
        String name = JDBCUtils.safeGetStringTrimmed(resultSet, "NAME");
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
    public String getViewDDL(DBRProgressMonitor monitor, GenericView sourceObject, Map<String, Object> options) throws DBException {
        return DamengUtils.getDDL(monitor, sourceObject, DamengConstants.ObjectType.VIEW, sourceObject.getParentObject().getName());
    }

    @Override
    public DamengTableColumn createTableColumnImpl(DBRProgressMonitor monitor, JDBCResultSet dbResult, GenericTableBase table, String columnName, String typeName, int valueType, int sourceType, int ordinalPos, long columnSize, long charLength, Integer scale, Integer precision, int radix, boolean notNull, String remarks, String defaultValue, boolean autoIncrement, boolean autoGenerated) throws DBException {
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
            throw new DBException(e, container.getDataSource());
        }
    }

    @Override
    public String getProcedureDDL(DBRProgressMonitor monitor, GenericProcedure sourceObject) throws DBException {
        return DamengUtils.getDDL(monitor, sourceObject, DamengConstants.ObjectType.PROCEDURE, sourceObject.getContainer().getName());
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
