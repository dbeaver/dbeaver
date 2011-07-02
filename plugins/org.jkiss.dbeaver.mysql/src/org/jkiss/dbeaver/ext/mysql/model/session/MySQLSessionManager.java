/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.model.session;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mysql.model.MySQLDataSource;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.admin.sessions.DBAServerSessionManager;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
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

    public DBPDataSource getDataSource()
    {
        return dataSource;
    }

    public Collection<MySQLSession> getSessions(DBCExecutionContext context, Map<String, Object> options) throws DBException
    {
        try {
            JDBCPreparedStatement dbStat = ((JDBCExecutionContext)context).prepareStatement("SHOW FULL PROCESSLIST");
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
            throw new DBException(e);
        }
    }

    public void alterSession(DBCExecutionContext context, MySQLSession session, Map<String, Object> options) throws DBException
    {
        try {
            JDBCPreparedStatement dbStat = ((JDBCExecutionContext)context).prepareStatement(
                Boolean.TRUE.equals(options.get(PROP_KILL_QUERY)) ?
                    "KILL CONNECTION " + session.getPid() :
                    "KILL QUERY " + session.getPid());
            try {
                dbStat.execute();
            } finally {
                dbStat.close();
            }
        }
        catch (SQLException e) {
            throw new DBException(e);
        }
    }

}
