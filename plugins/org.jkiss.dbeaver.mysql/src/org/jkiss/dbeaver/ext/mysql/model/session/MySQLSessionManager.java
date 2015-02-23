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
            JDBCPreparedStatement dbStat = ((JDBCSession) session).prepareStatement("SHOW FULL PROCESSLIST");
            try {
                JDBCResultSet dbResult = dbStat.executeQuery();
                try {
                    List<MySQLSession> sessions = new ArrayList<MySQLSession>();
                    while (dbResult.next()) {
                        sessions.add(new MySQLSession(dbResult));
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
    public void alterSession(DBCSession session, MySQLSession sessionType, Map<String, Object> options) throws DBException
    {
        try {
            JDBCPreparedStatement dbStat = ((JDBCSession) session).prepareStatement(
                Boolean.TRUE.equals(options.get(PROP_KILL_QUERY)) ?
                    "KILL QUERY " + sessionType.getPid() :
                    "KILL CONNECTION " + sessionType.getPid());
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
