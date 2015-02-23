/*
 * Copyright (C) 2013      Denis Forveille titou10.titou10@gmail.com
 * Copyright (C) 2010-2015 Serge Rieder serge@jkiss.org
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
package org.jkiss.dbeaver.ext.db2.model.cache;

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
    protected JDBCStatement prepareObjectsStatement(JDBCSession session, DB2Routine db2Routine) throws SQLException
    {
        JDBCPreparedStatement dbStat = session.prepareStatement(SQL);
        dbStat.setString(1, db2Routine.getSchema().getName());
        dbStat.setString(2, db2Routine.getUniqueName());
        return dbStat;
    }

    @Override
    protected DB2RoutineParm fetchObject(JDBCSession session, DB2Routine db2Routine, ResultSet resultSet) throws SQLException,
        DBException
    {
        return new DB2RoutineParm(session.getProgressMonitor(), db2Routine, resultSet);
    }

}