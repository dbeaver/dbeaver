/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2016-2016 Karl Griesser (fullref@gmail.com)
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
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
