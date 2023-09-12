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

package org.jkiss.dbeaver.ext.dm.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
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
public class DMMetaModel extends GenericMetaModel {

    public DMMetaModel() {
        super();
    }

    @Override
    public GenericDataSource createDataSourceImpl(DBRProgressMonitor monitor, DBPDataSourceContainer container) throws DBException {
        return new DMDataSource(monitor, container, this);
    }

    @Override
    public DMSchema createSchemaImpl(GenericDataSource dataSource, GenericCatalog catalog, String schemaName) throws DBException {
        return new DMSchema(dataSource, catalog, schemaName);
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
        return new DMTable(container, tableName, tableType, dbResult);
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
        JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT SEQ_OBJ_INNER.NAME,SEQ_OBJ_INNER.INFO1,SEQ_OBJ_INNER.INFO3,SEQ_OBJ_INNER.INFO4 " +
                        "FROM " +
                        "SYSOBJECTS SEQ_OBJ_INNER, " +
                        "SYSOBJECTS SCH_OBJ_INNER " +
                        "WHERE " +
                        "SEQ_OBJ_INNER.SUBTYPE$ = 'SEQ' " +
                        "AND SCH_OBJ_INNER.ID = SEQ_OBJ_INNER.SCHID " +
                        "AND SCH_OBJ_INNER.name = ?");
        dbStat.setString(1, container.getName());
        return dbStat;
    }

    @Override
    public GenericSequence createSequenceImpl(JDBCSession session, GenericStructContainer container, JDBCResultSet dbResult) throws DBException {
        String name = JDBCUtils.safeGetString(dbResult, 1);
        if (CommonUtils.isEmpty(name)) {
            return null;
        }
        Number lastValue = JDBCUtils.safeGetBigDecimal(dbResult, 3);
        Number incrementBy = JDBCUtils.safeGetBigDecimal(dbResult, 4);
        return new GenericSequence(container, name, "", lastValue, null, null, incrementBy);
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
        if (sourceObject.getContainer() instanceof DMTable) {
            return DMUtils.getDDL(monitor, sourceObject, "TRIGGER", ((DMTable) sourceObject.getContainer()).getContainer().getName());
        }
        return DMUtils.getDDL(monitor, sourceObject, "TRIGGER", sourceObject.getContainer().getName());
    }

    @Override
    public String getViewDDL(DBRProgressMonitor monitor, GenericView sourceObject, Map<String, Object> options) throws DBException {
        return DMUtils.getDDL(monitor, sourceObject, "VIEW", sourceObject.getParentObject().getName());
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
}
