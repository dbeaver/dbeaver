/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
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
package org.jkiss.dbeaver.ext.oracle.model.session;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.oracle.model.OracleDataSource;
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
public class OracleServerSessionManager implements DBAServerSessionManager<OracleServerSession> {

    public static final String PROP_KILL_SESSION = "killSession";
    public static final String PROP_IMMEDIATE = "immediate";

    private final OracleDataSource dataSource;

    public OracleServerSessionManager(OracleDataSource dataSource)
    {
        this.dataSource = dataSource;
    }

    @Override
    public DBPDataSource getDataSource()
    {
        return dataSource;
    }

    @Override
    public Collection<OracleServerSession> getSessions(DBCSession session, Map<String, Object> options) throws DBException
    {
        try {
            JDBCPreparedStatement dbStat = ((JDBCSession) session).prepareStatement(
                "SELECT s.*,sq.SQL_TEXT FROM V$SESSION s\n" +
                "LEFT OUTER JOIN V$SQL sq ON sq.SQL_ID=s.SQL_ID\n" +
                "WHERE s.TYPE='USER'");
            try {
                JDBCResultSet dbResult = dbStat.executeQuery();
                try {
                    List<OracleServerSession> sessions = new ArrayList<OracleServerSession>();
                    while (dbResult.next()) {
                        sessions.add(new OracleServerSession(dbResult));
                    }
                    return sessions;
                } finally {
                    dbResult.close();
                }
            } finally {
                dbStat.close();
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
            JDBCPreparedStatement dbStat = ((JDBCSession) session).prepareStatement(sql.toString());
            try {
                dbStat.execute();
            } finally {
                dbStat.close();
            }
        }
        catch (SQLException e) {
            throw new DBException(e, session.getDataSource());
        }
    }

}
