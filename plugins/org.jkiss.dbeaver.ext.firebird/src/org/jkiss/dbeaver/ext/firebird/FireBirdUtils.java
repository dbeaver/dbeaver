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

import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericProcedure;
import org.jkiss.dbeaver.ext.generic.model.GenericTable;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

/**
 * FireBird utils
 */
public class FireBirdUtils {

    static final Log log = Log.getLog(FireBirdUtils.class);

    public static String getProcedureSource(DBRProgressMonitor monitor, GenericProcedure procedure)
        throws DBException
    {
        JDBCSession session = procedure.getDataSource().getDefaultContext(true).openSession(monitor, DBCExecutionPurpose.META, "Load procedure source code");
        try {
            DatabaseMetaData fbMetaData = session.getOriginal().getMetaData();
            return (String)fbMetaData.getClass().getMethod("getProcedureSourceCode", String.class).invoke(fbMetaData, procedure.getName());
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
        JDBCSession session = view.getDataSource().getDefaultContext(true).openSession(monitor, DBCExecutionPurpose.META, "Load view source code");
        try {
            DatabaseMetaData fbMetaData = session.getOriginal().getMetaData();
            return (String)fbMetaData.getClass().getMethod("getViewSourceCode", String.class).invoke(fbMetaData, view.getName());
        } catch (SQLException e) {
            throw new DBException("Can't read source code of view '" + view.getName() + "'", e);
        } catch (Exception e) {
            log.debug(e);
            return null;
        }
    }
}
