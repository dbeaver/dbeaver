/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.oracle.model.session;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.admin.sessions.DBAServerSessionManager;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * MySQL session manager
 */
public class OracleServerSessionManager implements DBAServerSessionManager<OracleServerSession> {

    public static final String PROP_KILL_SESSION = "killSession";
    public static final String PROP_IMMEDIATE = "immediate";

    private final DBCExecutionContext executionContext;

    public OracleServerSessionManager(DBCExecutionContext executionContext)
    {
        this.executionContext = executionContext;
    }

    @Override
    public DBPDataSource getDataSource()
    {
        return executionContext.getDataSource();
    }

    @Override
    public Collection<OracleServerSession> getSessions(DBCSession session, Map<String, Object> options) throws DBException
    {
        try {
            try (JDBCPreparedStatement dbStat = ((JDBCSession) session).prepareStatement(
                "SELECT s.*, sq.SQL_TEXT\n" +
                "FROM V$SESSION s, V$SQL sq\n" +
                "WHERE sq.ADDRESS(+) = s.SQL_ADDRESS AND s.TYPE = 'USER'"))
            {
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    List<OracleServerSession> sessions = new ArrayList<>();
                    while (dbResult.next()) {
                        sessions.add(new OracleServerSession(dbResult));
                    }
                    return sessions;
                }
            }
        } catch (SQLException e) {
            throw new DBException(e, session.getDataSource());
        }
    }

    @Override
    public void alterSession(DBCSession session, OracleServerSession sessionType, Map<String, Object> options) throws DBException
    {
        final boolean toKill = Boolean.TRUE.equals(options.get(PROP_KILL_SESSION));
        final boolean immediate = Boolean.TRUE.equals(options.get(PROP_IMMEDIATE));

        try {
            StringBuilder sql = new StringBuilder("ALTER SYSTEM ");
            if (toKill) {
                sql.append("KILL SESSION ");
            } else {
                sql.append("DISCONNECT SESSION ");
            }
            sql.append("'").append(sessionType.getSid()).append(',').append(sessionType.getSerial()).append("'");
            if (immediate) {
                sql.append(" IMMEDIATE");
            } else if (!toKill) {
                sql.append(" POST_TRANSACTION");
            }
            try (JDBCPreparedStatement dbStat = ((JDBCSession) session).prepareStatement(sql.toString())) {
                dbStat.execute();
            }
        }
        catch (SQLException e) {
            throw new DBException(e, session.getDataSource());
        }
    }

}
