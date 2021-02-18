/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2015 Denis Forveille (titou10.titou10@gmail.com)
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
import org.jkiss.dbeaver.ext.db2.model.DB2Schema;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2RoutineType;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;

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

    @NotNull
    @Override
    protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull DB2Schema db2Schema) throws SQLException
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
    protected DB2Routine fetchObject(@NotNull JDBCSession session, @NotNull DB2Schema db2Schema, @NotNull JDBCResultSet dbResult) throws SQLException, DBException
    {
        return new DB2Routine(db2Schema, dbResult);
    }
}