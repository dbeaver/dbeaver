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
package org.jkiss.dbeaver.ext.netezza.model;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.ext.generic.model.GenericProcedure;
import org.jkiss.dbeaver.ext.generic.model.GenericTable;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModel;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.SQLException;

/**
 * NetezzaMetaModel
 */
public class NetezzaMetaModel extends GenericMetaModel
{
    public NetezzaMetaModel(IConfigurationElement cfg) {
        super(cfg);
    }

    public String getViewDDL(DBRProgressMonitor monitor, GenericTable sourceObject) throws DBException {
        GenericDataSource dataSource = sourceObject.getDataSource();
        try (JDBCSession session = DBUtils.openMetaSession(monitor, dataSource, "Read Netezza view source")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT v.definition " +
                "FROM _v_view v, _v_objs_owned o " +
                "WHERE v.objid = o.objid AND o.DATABASE=? AND v.VIEWNAME=?"))
            {
                dbStat.setString(1, sourceObject.getContainer().getName());
                dbStat.setString(2, sourceObject.getName());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    if (dbResult.nextRow()) {
                        return
                            "CREATE OR REPLACE VIEW " + sourceObject.getName() + " AS\n" +
                            dbResult.getString(1);
                    }
                    return "-- Netezza view definition not found";
                }
            }
        } catch (SQLException e) {
            throw new DBException(e, dataSource);
        }
    }

    @Override
    public String getProcedureDDL(DBRProgressMonitor monitor, GenericProcedure sourceObject) throws DBException {
        GenericDataSource dataSource = sourceObject.getDataSource();
        try (JDBCSession session = DBUtils.openMetaSession(monitor, dataSource, "Read Netezza procedure source")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT p.proceduresignature,p.returns,p.proceduresource " +
                "FROM _v_procedure p " +
                "WHERE p.owner=? AND p.procedure=?"))
            {
                dbStat.setString(1, sourceObject.getContainer().getName());
                dbStat.setString(2, sourceObject.getName());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    if (dbResult.nextRow()) {
                        return
                            "CREATE OR REPLACE PROCEDURE " + dbResult.getString(1) + " RETURNS " + dbResult.getString(2) +
                            "LANGUAGE NZPLSQL AS BEGIN_PROC\n" + dbResult.getString(3) + "\nEND_PROC;";
                    }
                    return "-- Netezza procedure source not found";
                }
            }
        } catch (SQLException e) {
            throw new DBException(e, dataSource);
        }
    }

}
