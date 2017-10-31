/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.generic.model.*;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModel;
import org.jkiss.dbeaver.ext.informix.InformixUtils;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.SQLException;
import java.util.ArrayList;
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

    public String getViewDDL(DBRProgressMonitor monitor, GenericTable sourceObject, Map<String, Object> options) throws DBException {
        return InformixUtils.getViewSource(monitor, sourceObject);
    }

    @Override
    public String getProcedureDDL(DBRProgressMonitor monitor, GenericProcedure sourceObject) throws DBException {
        return InformixUtils.getProcedureSource(monitor, sourceObject);
    }
    
    @Override
    public String getTableDDL(DBRProgressMonitor monitor, GenericTable sourceObject, Map<String, Object> options) throws DBException {
    	String tableDDL = super.getTableDDL(monitor, sourceObject, options);
    	// Triggers, Serials
    	// 
    	return tableDDL + InformixUtils.getTriggerDDL(monitor, sourceObject);
    }

    @Override
    public boolean supportsTriggers(@NotNull GenericDataSource dataSource) {
        return true;
    }

    @Override
    public List<? extends GenericTrigger> loadTriggers(DBRProgressMonitor monitor, @NotNull GenericStructContainer container, @Nullable GenericTable table) throws DBException {
        assert table != null;
        try (JDBCSession session = DBUtils.openMetaSession(monitor, container.getDataSource(), "Read triggers")) {
            String query =
                "SELECT T1.trigname \n" +
                "FROM informix.systriggers AS T1, informix.systables AS T2 \n" +
                "WHERE T2.tabid = T1.tabid AND T2.tabname = ?";

            try (JDBCPreparedStatement dbStat = session.prepareStatement(query)) {
                dbStat.setString(1, table.getName());
                List<GenericTrigger> result = new ArrayList<>();

                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    while (dbResult.next()) {
                        String name = JDBCUtils.safeGetString(dbResult, 1);
                        if (name == null) {
                            continue;
                        }
                        name = name.trim();
                        GenericTrigger trigger = new GenericTrigger(container, table, name, null);
                        result.add(trigger);
                    }
                }
                return result;
            }
        } catch (SQLException e) {
            throw new DBException(e, container.getDataSource());
        }
    }

    @Override
    public String getTriggerDDL(@NotNull DBRProgressMonitor monitor, @NotNull GenericTrigger trigger) throws DBException {
        GenericTable table = trigger.getTable();
        assert table != null;
        return InformixUtils.getTriggerDDL(monitor, trigger);
    }

}