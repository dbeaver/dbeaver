/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.dbeaver.DBException;
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
public class OracleServerSessionManager implements DBAServerSessionManager<OracleServerSession> {

    public static final String PROP_KILL_SESSION = "killSession";

    private final OracleDataSource dataSource;

    public OracleServerSessionManager(OracleDataSource dataSource)
    {
        this.dataSource = dataSource;
    }

    public DBPDataSource getDataSource()
    {
        return dataSource;
    }

    public Collection<OracleServerSession> getSessions(DBCExecutionContext context, Map<String, Object> options) throws DBException
    {
        try {
            JDBCPreparedStatement dbStat = ((JDBCExecutionContext)context).prepareStatement(
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
            throw new DBException(e);
        }
    }

    public void alterSession(DBCExecutionContext context, OracleServerSession session, Map<String, Object> options) throws DBException
    {
        try {
            JDBCPreparedStatement dbStat = ((JDBCExecutionContext)context).prepareStatement(
                Boolean.TRUE.equals(options.get(PROP_KILL_SESSION)) ?
                    "KILL CONNECTION " + session.getSid() :
                    "KILL QUERY " + session.getSid());
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
