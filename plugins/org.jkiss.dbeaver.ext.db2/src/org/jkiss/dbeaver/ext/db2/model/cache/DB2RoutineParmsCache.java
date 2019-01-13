/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2015 Denis Forveille (titou10.titou10@gmail.com)
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.db2.model.cache;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.db2.model.DB2Routine;
import org.jkiss.dbeaver.ext.db2.model.DB2RoutineParm;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;

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
    protected DB2RoutineParm fetchObject(@NotNull JDBCSession session, @NotNull DB2Routine db2Routine, @NotNull JDBCResultSet resultSet) throws SQLException,
        DBException
    {
        return new DB2RoutineParm(session.getProgressMonitor(), db2Routine, resultSet);
    }

}