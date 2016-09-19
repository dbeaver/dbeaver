/*
 * DBeaver - Universal Database Manager
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
package org.jkiss.dbeaver.ext.postgresql.model.session;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDataSource;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.admin.sessions.DBAServerSessionManager;
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
 * Postgre session manager
 */
public class PostgreSessionManager implements DBAServerSessionManager<PostgreSession> {

    public static final String PROP_KILL_QUERY = "killQuery";

    private final PostgreDataSource dataSource;

    public PostgreSessionManager(PostgreDataSource dataSource)
    {
        this.dataSource = dataSource;
    }

    @Override
    public DBPDataSource getDataSource()
    {
        return dataSource;
    }

    @Override
    public Collection<PostgreSession> getSessions(DBCSession session, Map<String, Object> options) throws DBException
    {
        try {
            try (JDBCPreparedStatement dbStat = ((JDBCSession) session).prepareStatement("SELECT sa.* FROM pg_catalog.pg_stat_activity sa")) {
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    List<PostgreSession> sessions = new ArrayList<>();
                    while (dbResult.next()) {
                        sessions.add(new PostgreSession(dbResult));
                    }
                    return sessions;
                }
            }
        } catch (SQLException e) {
            throw new DBException(e, session.getDataSource());
        }
    }

    @Override
    public void alterSession(DBCSession session, PostgreSession sessionType, Map<String, Object> options) throws DBException
    {
        try {
            try (JDBCPreparedStatement dbStat = ((JDBCSession) session).prepareStatement("SELECT pg_catalog.pg_terminate_backend(?)")) {
                dbStat.setInt(1, sessionType.getPid());
                dbStat.execute();
            }
        }
        catch (SQLException e) {
            throw new DBException(e, session.getDataSource());
        }
    }

}
