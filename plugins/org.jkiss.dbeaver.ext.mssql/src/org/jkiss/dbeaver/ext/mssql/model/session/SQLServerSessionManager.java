/*
 * DBeaver - Universal Database Manager
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
package org.jkiss.dbeaver.ext.mssql.model.session;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mssql.model.SQLServerDataSource;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.admin.sessions.DBAServerSessionManager;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * SQLServer session manager
 */
public class SQLServerSessionManager implements DBAServerSessionManager<SQLServerSession> {

    public static final String OPTION_SHOW_ONLY_CONNECTIONS = "showOnlyConnections";

    private final SQLServerDataSource dataSource;

    public SQLServerSessionManager(SQLServerDataSource dataSource)
    {
        this.dataSource = dataSource;
    }

    @Override
    public DBPDataSource getDataSource()
    {
        return dataSource;
    }

    @Override
    public Collection<SQLServerSession> getSessions(DBCSession session, Map<String, Object> options) throws DBException
    {
        try {
            boolean onlyConnections = CommonUtils.getOption(options, OPTION_SHOW_ONLY_CONNECTIONS);
            try (JDBCPreparedStatement dbStat = ((JDBCSession) session).prepareStatement(
                "SELECT s.*,db.name as database_name,c.connection_id,(select text from sys.dm_exec_sql_text(c.most_recent_sql_handle)) as sql_text\n" +
                    "FROM sys.dm_exec_sessions s\n" +
                    (onlyConnections ? "" : "LEFT OUTER ") + "JOIN sys.dm_exec_connections c ON c.session_id=s.session_id\n" +
                    "LEFT OUTER JOIN sys.sysdatabases db on db.dbid=s.database_id\n" +
                    "ORDER BY s.session_id DESC")) {
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    List<SQLServerSession> sessions = new ArrayList<>();
                    while (dbResult.next()) {
                        sessions.add(new SQLServerSession(dbResult));
                    }
                    return sessions;
                }
            }
        } catch (SQLException e) {
            throw new DBException(e, session.getDataSource());
        }
    }

    @Override
    public void alterSession(DBCSession session, SQLServerSession sessionType, Map<String, Object> options) throws DBException
    {
        try {
            try (Statement dbStat = ((JDBCSession) session).createStatement()) {
                dbStat.execute("KILL " + sessionType.getId() + ")");
            }
        }
        catch (SQLException e) {
            throw new DBException(e, session.getDataSource());
        }
    }

}
