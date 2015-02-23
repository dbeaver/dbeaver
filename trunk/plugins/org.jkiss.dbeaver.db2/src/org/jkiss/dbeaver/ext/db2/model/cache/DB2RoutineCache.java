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
import org.jkiss.dbeaver.ext.db2.model.DB2Schema;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2RoutineType;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Cache for DB2 Routines (UDF + Methods + Procedures)
 * 
 * @author Denis Forveille
 */
public class DB2RoutineCache extends JDBCObjectCache<DB2Schema, DB2Routine> {

    private static final String SQL_BASE_V95 = "SELECT * FROM SYSCAT.ROUTINES WHERE ROUTINESCHEMA = ? AND ROUTINETYPE= '%s' ORDER BY ROUTINENAME WITH UR";
    private static final String SQL_BASE_ALL = "SELECT * FROM SYSCAT.ROUTINES WHERE ROUTINESCHEMA = ? AND ROUTINETYPE= '%s' AND ROUTINEMODULENAME IS NULL ORDER BY ROUTINENAME WITH UR";

    private final String SQL_V95;
    private final String SQL_ALL;

    public DB2RoutineCache(DB2RoutineType routineType)
    {
        super();

        SQL_V95 = String.format(SQL_BASE_V95, routineType.name());
        SQL_ALL = String.format(SQL_BASE_ALL, routineType.name());
    }

    @Override
    protected JDBCStatement prepareObjectsStatement(JDBCSession session, DB2Schema db2Schema) throws SQLException
    {
        String sql;
        if (db2Schema.getDataSource().isAtLeastV9_7()) {
            sql = SQL_ALL;
        } else {
            sql = SQL_V95;
        }
        JDBCPreparedStatement dbStat = session.prepareStatement(sql);
        dbStat.setString(1, db2Schema.getName());
        return dbStat;
    }

    @Override
    protected DB2Routine fetchObject(JDBCSession session, DB2Schema db2Schema, ResultSet dbResult) throws SQLException, DBException
    {
        return new DB2Routine(db2Schema, dbResult);
    }
}