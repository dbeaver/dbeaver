/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2016-2016 Karl Griesser (fullref@gmail.com)
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
import org.jkiss.dbeaver.ext.exasol.model.ExasolDataSource;
import org.jkiss.dbeaver.ext.exasol.tools.ExasolUtils;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.admin.sessions.DBAServerSessionManager;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;

/**
 * @author Karl Griesser
 */
public class ExasolServerSessionManager implements DBAServerSessionManager<ExasolServerSession> {

    public static final String PROP_KILL_QUERY = "killQuery";
    private static final String KILL_APP_CMD = "kill session %s";
    private static final String KILL_STMT_CMD = "kill statement in session %s";

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
            return ExasolUtils.readSessions(session.getProgressMonitor(), (JDBCSession) session);
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


}
