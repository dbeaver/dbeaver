/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2016-2016 Karl Griesser (fullref@gmail.com)
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
package org.jkiss.dbeaver.ext.exasol.model.app;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.exasol.model.ExasolDataSource;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.admin.sessions.DBAServerSessionManager;
import org.jkiss.dbeaver.model.admin.sessions.DBAServerSessionManagerSQL;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author Karl Griesser
 */
public class ExasolServerSessionManager implements DBAServerSessionManager<ExasolServerSession>, DBAServerSessionManagerSQL {

    public static final String PROP_KILL_QUERY = "killQuery";
    private static final String KILL_APP_CMD = "kill session %s";
    private static final String KILL_STMT_CMD = "kill statement in session %s";

    private static final Log log = Log.getLog(ExasolServerSessionManager.class);

    // list sessions
    private static final String SESS_DBA_QUERY = "/*snapshot execution*/ select * from exa_dba_sessions";
    private static final String SESS_ALL_QUERY = "/*snapshot execution*/ select * from exa_ALL_sessions";


    private final ExasolDataSource dataSource;

    /**
     *
     */
    public ExasolServerSessionManager(ExasolDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public DBPDataSource getDataSource() {
        return this.dataSource;
    }

    @Override
    public Collection<ExasolServerSession> getSessions(DBCSession session, Map<String, Object> options)
        throws DBException {
        try {
            return readSessions((JDBCSession) session);
        } catch (SQLException e) {
            throw new DBException(e, session.getDataSource());
        }
    }

    @Override
    public void alterSession(DBCSession session, ExasolServerSession sessionType, Map<String, Object> options)
        throws DBException {
        try {
            String cmd = String.format(Boolean.TRUE.equals(options.get(PROP_KILL_QUERY)) ? KILL_STMT_CMD : KILL_APP_CMD, sessionType.getSessionID().toString());
            PreparedStatement dbStat = ((JDBCSession) session).prepareStatement(cmd);
            dbStat.execute();


        } catch (SQLException e) {
            throw new DBException(e, session.getDataSource());
        }

    }

    public static Collection<ExasolServerSession> readSessions(JDBCSession session) throws SQLException {

        log.debug("read sessions");

        List<ExasolServerSession> listSessions = new ArrayList<>();

        //check dba view
        try {
            try (JDBCStatement dbStat = session.createStatement()) {
                try (JDBCResultSet dbResult = dbStat.executeQuery(SESS_DBA_QUERY)) {
                    while (dbResult.next()) {
                        listSessions.add(new ExasolServerSession(dbResult));
                    }
                }
            }

            //now try all view
        } catch (SQLException e) {
            try (JDBCStatement dbStat = session.createStatement()) {
                try (JDBCResultSet dbResult = dbStat.executeQuery(SESS_ALL_QUERY)) {
                    while (dbResult.next()) {
                        listSessions.add(new ExasolServerSession(dbResult));
                    }
                }
            }
        }

        return listSessions;
    }

    @Override
    public boolean canGenerateSessionReadQuery() {
        return true;
    }

    @Override
    public String generateSessionReadQuery(Map<String, Object> options) {
        return SESS_ALL_QUERY;
    }
}
