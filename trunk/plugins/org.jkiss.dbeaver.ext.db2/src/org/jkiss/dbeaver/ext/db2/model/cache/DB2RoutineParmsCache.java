/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2015 Denis Forveille (titou10.titou10@gmail.com)
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
package org.jkiss.dbeaver.ext.db2.model.cache;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.db2.model.DB2Routine;
import org.jkiss.dbeaver.ext.db2.model.DB2RoutineParm;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Cache for Routine parameters
 * 
 * @author Denis Forveille
 * 
 */
public class DB2RoutineParmsCache extends JDBCObjectCache<DB2Routine, DB2RoutineParm> {

    private static final String SQL;

    static {
        StringBuilder sb = new StringBuilder(256);
        sb.append("SELECT *");
        sb.append("  FROM SYSCAT.ROUTINEPARMS");
        sb.append(" WHERE ROUTINESCHEMA = ?");
        sb.append("   AND SPECIFICNAME = ?");
        sb.append(" ORDER BY ORDINAL");
        sb.append(" WITH UR");
        SQL = sb.toString();
    }

    @Override
    protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull DB2Routine db2Routine) throws SQLException
    {
        JDBCPreparedStatement dbStat = session.prepareStatement(SQL);
        dbStat.setString(1, db2Routine.getSchema().getName());
        dbStat.setString(2, db2Routine.getUniqueName());
        return dbStat;
    }

    @Override
    protected DB2RoutineParm fetchObject(@NotNull JDBCSession session, @NotNull DB2Routine db2Routine, @NotNull ResultSet resultSet) throws SQLException,
        DBException
    {
        return new DB2RoutineParm(session.getProgressMonitor(), db2Routine, resultSet);
    }

}