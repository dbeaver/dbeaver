/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2017 Andrew Khitrin (ahitrin@gmail.com)
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

package org.jkiss.dbeaver.ext.postgresql.debug.internal.impl;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jkiss.dbeaver.debug.DBGException;
import org.jkiss.dbeaver.debug.DBGSession;
import org.jkiss.dbeaver.debug.DBGSessionManager;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;

@SuppressWarnings("nls")
public class PostgreDebugSessionManager implements DBGSessionManager {

    private final DBCExecutionContext context;

    private static final String SQL_SESSION = "select pid,usename,application_name,state,query from pg_stat_activity";

    private static final String SQL_OBJECT = "select  p.oid,p.proname,u.usename as owner,n.nspname, l.lanname as lang "
        + " from " + "	pg_catalog.pg_namespace n " + " join pg_catalog.pg_proc p on p.pronamespace = n.oid "
        + "	 join pg_user u on u.usesysid =   p.proowner " + "	 join pg_language l on l.oid = p. prolang "
        + "	where  " + "   l.lanname = 'plpgsql' " + "	 and p.proname like '%?nameCtx%' "
        + "	 and u.usename like '%?userCtx%' " + "	order by  " + "	 n.nspname,p.proname";

    private static final String SQL_CURRENT_SESSION = "select pid,usename,application_name,state,query from pg_stat_activity where pid = pg_backend_pid()";

    private final Map<Integer, PostgreDebugSession> sessions = new HashMap<Integer, PostgreDebugSession>(1);

    @Override
    public PostgreDebugSessionInfo getSessionInfo(DBCExecutionContext connectionTarget) throws DBGException {
        try (Statement stmt = getConnection(connectionTarget).createStatement();
                ResultSet rs = stmt.executeQuery(SQL_CURRENT_SESSION)) {

            if (rs.next()) {
                int pid = rs.getInt("pid");
                String usename = rs.getString("usename");
                String applicationName = rs.getString("application_name");
                String state = rs.getString("state");
                String query = rs.getString("query");
                PostgreDebugSessionInfo res = new PostgreDebugSessionInfo(pid, usename, applicationName, state, query);
                return res;
            }

            throw new DBGException("Error getting session");

        } catch (SQLException e) {
            throw new DBGException("SQ Lerror", e);
        }

    }

    private static Connection getConnection(DBCExecutionContext connectionTarget) throws SQLException {
        return ((JDBCExecutionContext) connectionTarget).getConnection(new VoidProgressMonitor());
    }

    @Override
    public List<PostgreDebugSessionInfo> getSessions() throws DBGException {

        try (Statement stmt = getConnection(context).createStatement(); ResultSet rs = stmt.executeQuery(SQL_SESSION)) {
            List<PostgreDebugSessionInfo> res = new ArrayList<PostgreDebugSessionInfo>();

            while (rs.next()) {
                int pid = rs.getInt("pid");
                String usename = rs.getString("usename");
                String state = rs.getString("state");
                String applicationName = rs.getString("application_name");
                String query = rs.getString("query");
                PostgreDebugSessionInfo info = new PostgreDebugSessionInfo(pid, usename, applicationName, state, query);
                res.add(info);
            }

            return res;

        } catch (SQLException e) {
            throw new DBGException("SQL error", e);
        }

    }

    /**
     * @param context
     */
    public PostgreDebugSessionManager(DBCExecutionContext context) {
        super();
        this.context = context;
    }

    @Override
    public List<PostgreDebugObject> getObjects(String ownerCtx, String nameCtx) throws DBGException {
        String sql = SQL_OBJECT.replaceAll("\\?nameCtx", nameCtx).replaceAll("\\?userCtx", ownerCtx).toLowerCase();
        try (Statement stmt = getConnection(context).createStatement(); ResultSet rs = stmt.executeQuery(sql)) {

            List<PostgreDebugObject> res = new ArrayList<PostgreDebugObject>();

            while (rs.next()) {
                int oid = rs.getInt("oid");
                String proname = rs.getString("proname");
                String owner = rs.getString("owner");
                String nspname = rs.getString("nspname");
                String lang = rs.getString("lang");
                PostgreDebugObject object = new PostgreDebugObject(oid, proname, owner, nspname, lang);
                res.add(object);
            }

            return res;

        } catch (SQLException e) {
            throw new DBGException("SQL error", e);
        }
    }

    @Override
    public DBGSession getDebugSession(Object id)
        throws DBGException {
        return sessions.get(id);
    }

    @Override
    public PostgreDebugSession createDebugSession(DBCExecutionContext connectionTarget) throws DBGException {

        PostgreDebugSessionInfo targetInfo = getSessionInfo(connectionTarget);

        PostgreDebugSession debugSession = new PostgreDebugSession(getSessionInfo(this.context), targetInfo);
        
        debugSession.attach((JDBCExecutionContext) connectionTarget, 16749, -1);
        //FIXME 16749 - OID for debug proc
        //FIXME -1 - target PID (-1 for ANY PID)
        
        sessions.put(targetInfo.getPid(), debugSession);

        return debugSession;

    }

    @Override
    public boolean isSessionExists(Object id) {
        return sessions.containsKey(id);
    }

    @Override
    public void terminateSession(Object id) {

        PostgreDebugSession session = sessions.get(id);

        if (session != null) {

            session.close();

            sessions.remove(id);

        }

    }

    @Override
    public List<DBGSession> getDebugSessions() throws DBGException {
        return new ArrayList<DBGSession>(sessions.values());
    }

    @Override
    public void dispose() {
        context.close();
        //FIXME: AF: perform cleanup for everything cached
    }

}
