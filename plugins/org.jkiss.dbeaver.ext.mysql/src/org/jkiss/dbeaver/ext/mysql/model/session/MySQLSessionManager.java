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
package org.jkiss.dbeaver.ext.mysql.model.session;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mysql.model.MySQLDataSource;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.admin.sessions.DBAServerSessionManager;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * MySQL session manager
 */
public class MySQLSessionManager implements DBAServerSessionManager<MySQLSession> {

    public static final String PROP_KILL_QUERY = "killQuery";

    private final MySQLDataSource dataSource;

    public MySQLSessionManager(MySQLDataSource dataSource)
    {
        this.dataSource = dataSource;
    }

    @Override
    public DBPDataSource getDataSource()
    {
        return dataSource;
    }

    @Override
    public Collection<MySQLSession> getSessions(DBCSession session, Map<String, Object> options) throws DBException
    {
        try {
            try (JDBCPreparedStatement dbStat = ((JDBCSession) session).prepareStatement("SHOW FULL PROCESSLIST")) {
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    List<MySQLSession> sessions = new ArrayList<>();
                    while (dbResult.next()) {
                        sessions.add(new MySQLSession(dbResult));
                    }
                    return sessions;
                }
            }
        } catch (SQLException e) {
            throw new DBException(e, session.getDataSource());
        }
    }

    @Override
    public void alterSession(DBCSession session, MySQLSession sessionType, Map<String, Object> options) throws DBException
    {
        try {
            try (JDBCPreparedStatement dbStat = ((JDBCSession) session).prepareStatement(
                Boolean.TRUE.equals(options.get(PROP_KILL_QUERY)) ?
                    "KILL QUERY " + sessionType.getPid() :
                    "KILL CONNECTION " + sessionType.getPid())) {
                dbStat.execute();
            }
        }
        catch (SQLException e) {
            throw new DBException(e, session.getDataSource());
        }
    }

}
