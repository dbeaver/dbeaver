/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.jkiss.dbeaver.ext.postgresql;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.Log;
import org.jkiss.dbeaver.ext.generic.model.GenericProcedure;
import org.jkiss.dbeaver.ext.generic.model.GenericTable;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

/**
 * postgresql utils
 */
public class PostgreUtils {

    static final Log log = Log.getLog(PostgreUtils.class);

    public static String getProcedureSource(DBRProgressMonitor monitor, GenericProcedure procedure)
        throws DBException
    {
        JDBCSession session = procedure.getDataSource().openSession(monitor, DBCExecutionPurpose.META, "Load procedure source code");
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
        JDBCSession session = view.getDataSource().openSession(monitor, DBCExecutionPurpose.META, "Load view source code");
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
