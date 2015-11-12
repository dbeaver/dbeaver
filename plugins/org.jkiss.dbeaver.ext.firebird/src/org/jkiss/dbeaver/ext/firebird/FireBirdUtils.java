/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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

package org.jkiss.dbeaver.ext.firebird;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.generic.model.*;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureParameterKind;
import org.jkiss.utils.CommonUtils;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * FireBird utils
 */
public class FireBirdUtils {

    static final Log log = Log.getLog(FireBirdUtils.class);

    public static String getProcedureSource(DBRProgressMonitor monitor, GenericProcedure procedure)
        throws DBException
    {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, procedure.getDataSource(), "Load procedure source code")) {
            DatabaseMetaData fbMetaData = session.getOriginal().getMetaData();
            String source = (String) fbMetaData.getClass().getMethod("getProcedureSourceCode", String.class).invoke(fbMetaData, procedure.getName());
            if (CommonUtils.isEmpty(source)) {
                return null;
            }

            return getProcedureSourceWithHeader(monitor, procedure, source);
        } catch (SQLException e) {
            throw new DBException("Can't read source code of procedure '" + procedure.getName() + "'", e);
        } catch (Exception e) {
            log.debug(e);
            return null;
        }
    }

    public static String getViewSource(DBRProgressMonitor monitor, GenericTable view)
        throws DBException
    {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, view.getDataSource(), "Load view source code")) {
            DatabaseMetaData fbMetaData = session.getOriginal().getMetaData();
            String source = (String) fbMetaData.getClass().getMethod("getViewSourceCode", String.class).invoke(fbMetaData, view.getName());
            if (CommonUtils.isEmpty(source)) {
                return null;
            }

            return getViewSourceWithHeader(monitor, view, source);
        } catch (SQLException e) {
            throw new DBException("Can't read source code of view '" + view.getName() + "'", e);
        } catch (Exception e) {
            log.debug(e);
            return null;
        }
    }

    public static String getProcedureSourceWithHeader(DBRProgressMonitor monitor, GenericProcedure procedure, String source) throws DBException {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE OR ALTER PROCEDURE ").append(procedure.getName()).append("\n");
        Collection<GenericProcedureParameter> parameters = procedure.getParameters(monitor);
        if (parameters != null && !parameters.isEmpty()) {
            List<GenericProcedureParameter> args = new ArrayList<>();
            List<GenericProcedureParameter> results = new ArrayList<>();
            for (GenericProcedureParameter param : parameters) {
                if (param.getParameterKind() == DBSProcedureParameterKind.OUT || param.getParameterKind() == DBSProcedureParameterKind.RETURN) {
                    results.add(param);
                } else {
                    args.add(param);
                }
            }
            if (!args.isEmpty()) {
                sql.append("(");
                for (int i = 0; i < args.size(); i++) {
                    GenericProcedureParameter param = args.get(i);
                    if (i > 0) sql.append(", ");
                    printParam(sql, param);
                }
                sql.append(")\n");
            }
            if (!results.isEmpty()) {
                sql.append("RETURNS (");
                for (int i = 0; i < results.size(); i++) {
                    GenericProcedureParameter param = results.get(i);
                    if (i > 0) sql.append(", ");
                    printParam(sql, param);
                }
                sql.append(")\n");
            }
        }

        sql.append("AS\n").append(source);

        return sql.toString();
    }

    private static void printParam(StringBuilder sql, GenericProcedureParameter param) {
        sql.append(param.getName()).append(" ").append(param.getTypeName());
        if (param.getDataKind() == DBPDataKind.STRING) {
            sql.append("(").append(param.getMaxLength()).append(")");
        }
    }

    public static String getViewSourceWithHeader(DBRProgressMonitor monitor, GenericTable view, String source) throws DBException {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE VIEW ").append(view.getName()).append(" ");
        Collection<GenericTableColumn> columns = view.getAttributes(monitor);
        if (columns != null) {
            sql.append("(");
            boolean first = true;
            for (GenericTableColumn column : columns) {
                if (!first) {
                    sql.append(", ");
                }
                first = false;
                sql.append(column.getName());
            }
            sql.append(")\n");
        }
        sql.append("AS\n").append(source);

        return sql.toString();
    }

}
