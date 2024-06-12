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
package org.jkiss.dbeaver.ext.intersystems.model;

import java.sql.SQLException;
import java.util.Map;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.ext.generic.model.GenericProcedure;
import org.jkiss.dbeaver.ext.generic.model.GenericSchema;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.ext.generic.model.GenericTableTrigger;
import org.jkiss.dbeaver.ext.generic.model.GenericTrigger;
import org.jkiss.dbeaver.ext.generic.model.GenericView;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModel;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.format.SQLFormatUtils;

public class InterSystemsMetaModel extends GenericMetaModel
{
    private static final Log log = Log.getLog(InterSystemsMetaModel.class);

    public InterSystemsMetaModel() {
        super();
    }

    public String getViewDDL(
            @NotNull DBRProgressMonitor monitor,
            @NotNull GenericView view,
            @NotNull Map<String, Object> options) throws DBException {
        String sqlStatement = "SELECT View_Definition "
                + " FROM INFORMATION_SCHEMA.VIEWS "
                + "where TABLE_NAME=? and TABLE_SCHEMA=? ";
        try (JDBCSession session = DBUtils.openMetaSession(monitor, view, "Load source code")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(sqlStatement)) {
                dbStat.setString(1, view.getName());
                dbStat.setString(2, view.getContainer().getSchema().getName());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    if (dbResult.nextRow()) {
                         String viewplain = dbResult.getString(1);
                         return SQLFormatUtils.formatSQL(view.getDataSource(), viewplain);
                    }
                }
            }
        } catch (Exception e) {
            throw new DBException("Can't read source code of '" + view.getFullyQualifiedName(DBPEvaluationContext.DDL) + "'", e);
        }
        return super.getViewDDL(monitor, view, options);
    }
    
    public boolean isSystemSchema(GenericSchema schema) {
        if (schema.getName().startsWith("%") && !schema.getName().toUpperCase().startsWith("%Z")) {
            return true;
        }
        if (schema.getName().startsWith("%ZEN_")) {
            return true;
        }
        if (schema.getName().equals("INFORMATION_SCHEMA")) {
            return true;
        }
        return false;
    }
    public boolean supportsTriggers(@NotNull GenericDataSource dataSource) {
        return true;
    }
    @Override
    public JDBCStatement prepareTableTriggersLoadStatement(@NotNull JDBCSession session, @NotNull GenericStructContainer container, @Nullable GenericTableBase table) throws SQLException {
        String query = "SELECT trigger_name, table_name as OWNER, "
                + " event_manipulation, action_order, action_orientation, action_timing"
                + " FROM INFORMATION_SCHEMA.TRIGGERS WHERE table_schema=?";
        
        if (table != null) {
            query += " AND table_name = ?";
        } 
        
        JDBCPreparedStatement dbStat = session.prepareStatement(query);
        
        dbStat.setString(1, container.getSchema().getName());
        if (table != null) {
            dbStat.setString(2, table.getName());
        } 
        return dbStat;
    }
    
    @Override
    public GenericTableTrigger createTableTriggerImpl(@NotNull JDBCSession session, @NotNull GenericStructContainer genericStructContainer, @NotNull GenericTableBase genericTableBase, String triggerName, @NotNull JDBCResultSet resultSet) throws DBException {
        StringBuilder triggerDefinition = new StringBuilder();
        triggerDefinition.append(JDBCUtils.safeGetString(resultSet, "action_timing"));
        triggerDefinition.append(" ");
        triggerDefinition.append(JDBCUtils.safeGetString(resultSet, "event_manipulation").replace('/', ','));
        String action_order = JDBCUtils.safeGetString(resultSet, "action_order"); 
        if (action_order != null) {
            triggerDefinition.append(" ORDER ").append(action_order);
        }
        triggerDefinition.append(" FOR EACH ").append(JDBCUtils.safeGetString(resultSet, "action_orientation"));
        return new GenericTableTrigger(genericTableBase, triggerName, triggerDefinition.toString());
    }
    
    @Override
    public String getTriggerDDL(@NotNull DBRProgressMonitor monitor, @NotNull GenericTrigger trigger) throws DBException {
        if (!(trigger instanceof GenericTableTrigger)) {
            return super.getTriggerDDL(monitor, trigger);
        }
        
        trigger = (GenericTableTrigger) trigger;
        
        String sqlStatement = "SELECT ACTION_STATEMENT "
                + " FROM INFORMATION_SCHEMA.TRIGGERS "
                + "where TABLE_NAME=? and TABLE_SCHEMA=? and Trigger_Name = ?";
        try (JDBCSession session = DBUtils.openMetaSession(monitor, trigger, "Load source code")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(sqlStatement)) {
                dbStat.setString(1, trigger.getTable().getName());
                dbStat.setString(2, ((GenericTableBase)trigger.getTable()).getSchema().getName());
                dbStat.setString(3, trigger.getName());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    if (dbResult.nextRow()) {
                         return dbResult.getString(1);
                    }
                }
            }
        } catch (Exception e) {
            throw new DBException("Can't read source code of '" + trigger.getFullyQualifiedName(DBPEvaluationContext.DDL) + "'", e);
        }
        return super.getTriggerDDL(monitor, trigger);
    }
    
    @Override
    public String getProcedureDDL(DBRProgressMonitor monitor, GenericProcedure procedure) throws DBException {
        String sqlStatement = "SELECT ROUTINE_DEFINITION "
                + " FROM INFORMATION_SCHEMA.ROUTINES "
                + " WHERE ROUTINE_NAME = ? and ROUTINE_SCHEMA = ? ";
        try (JDBCSession session = DBUtils.openMetaSession(monitor, procedure, "Load source code")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(sqlStatement)) {
                dbStat.setString(1, procedure.getName());
                dbStat.setString(2, procedure.getContainer().getSchema().getName());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    if (dbResult.nextRow()) {
                         return dbResult.getString(1);
                    }
                }
            }
        } catch (Exception e) {
            throw new DBException("Can't read source code of '" + procedure.getFullyQualifiedName(DBPEvaluationContext.DDL) + "'", e);
        }
        return super.getProcedureDDL(monitor, procedure);
    }
}