/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ext.informix.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.dbeaver.DBException;
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

/**
 * InformixDataSource
 */
public class InformixMetaModel extends GenericMetaModel
{
    private static final Log log = Log.getLog(InformixMetaModel.class);

    public InformixMetaModel(IConfigurationElement cfg) {
        super(cfg);
    }

    public String getViewDDL(DBRProgressMonitor monitor, GenericTable sourceObject) throws DBException {
        return InformixUtils.getViewSource(monitor, sourceObject);
    }

    @Override
    public String getProcedureDDL(DBRProgressMonitor monitor, GenericProcedure sourceObject) throws DBException {
        return InformixUtils.getProcedureSource(monitor, sourceObject);
    }
    
    @Override
    public String getTableDDL(DBRProgressMonitor monitor, GenericTable sourceObject) throws DBException {
    	String tableDDL = super.getTableDDL(monitor, sourceObject);
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
                dbStat.setString(2, table.getName());
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