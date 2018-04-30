/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2018 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2017-2018 Andrew Khitrin (ahitrin@gmail.com)
 * Copyright (C) 2017-2018 Alexander Fedorov (alexander.fedorov@jkiss.org)
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
import java.util.List;
import java.util.Map;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.debug.DBGBaseController;
import org.jkiss.dbeaver.debug.DBGBreakpointDescriptor;
import org.jkiss.dbeaver.debug.DBGController;
import org.jkiss.dbeaver.debug.DBGException;
import org.jkiss.dbeaver.debug.DBGSession;
import org.jkiss.dbeaver.debug.DBGSessionInfo;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;

public class PostgreDebugController extends DBGBaseController {

    private static final String SQL_SESSION =
        "SELECT pid,usename,application_name,state,query FROM pg_stat_activity"; //$NON-NLS-1$

    private static final String SQL_OBJECT =
        "SELECT  p.oid,p.proname,u.usename as owner,n.nspname, l.lanname as lang\n" +
            "FROM pg_catalog.pg_namespace n\n" +
            "JOIN pg_catalog.pg_proc p on p.pronamespace = n.oid\n" +
            "JOIN pg_user u on u.usesysid =   p.proowner\n" +
            "JOIN pg_language l on l.oid = p. prolang\n" +
            "WHERE l.lanname = 'plpgsql' and p.proname like '%?nameCtx%' and u.usename like '%?userCtx%'\n" +
            "ORDER BY  n.nspname,p.proname";

    private static final String SQL_CURRENT_SESSION =
        "SELECT pid,usename,application_name,state,query\n" +
        "FROM pg_stat_activity WHERE pid = pg_backend_pid()"; //$NON-NLS-1$

    public PostgreDebugController(DBPDataSourceContainer dataSourceDescriptor) {
        super(dataSourceDescriptor);
    }

    @Override
    public PostgreDebugSessionInfo getSessionDescriptor(DBRProgressMonitor monitor, DBCExecutionContext connectionTarget) throws DBGException {
        try (JDBCSession session = (JDBCSession) connectionTarget.openSession(monitor, DBCExecutionPurpose.UTIL, "Read session info")) {
            try (Statement stmt = session.createStatement()) {
                 try (ResultSet rs = stmt.executeQuery(SQL_CURRENT_SESSION)) {
                     if (rs.next()) {
                         int pid = rs.getInt("pid");
                         String usename = rs.getString("usename");
                         String applicationName = rs.getString("application_name");
                         String state = rs.getString("state");
                         String query = rs.getString("query");
                         return new PostgreDebugSessionInfo(pid, usename, applicationName, state, query);
                     }
                     throw new DBGException("Error getting session");
                 }
            }
        } catch (SQLException e) {
            throw new DBGException("SQL error", e);
        }
    }

    private static Connection getConnection(DBCExecutionContext connectionTarget) throws SQLException {
        return ((JDBCExecutionContext) connectionTarget).getConnection(new VoidProgressMonitor());
    }

    @Override
    public List<PostgreDebugSessionInfo> getSessionDescriptors(DBRProgressMonitor monitor) throws DBGException {
        try (JDBCSession session = (JDBCSession) getExecutionContext().openSession(monitor, DBCExecutionPurpose.UTIL, "Read session descriptor")) {
            try (Statement stmt = session.createStatement()) {
                try (ResultSet rs = stmt.executeQuery(SQL_SESSION)) {
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
                }
            }

        } catch (SQLException e) {
            throw new DBGException("SQL error", e);
        }

    }

    @Override
    public List<PostgreDebugObjectDescriptor> getObjects(String ownerCtx, String nameCtx) throws DBGException {
        DBCExecutionContext executionContext = getExecutionContext();
        String sql = SQL_OBJECT.replaceAll("\\?nameCtx", nameCtx).replaceAll("\\?userCtx", ownerCtx).toLowerCase();
        try (Statement stmt = getConnection(executionContext).createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            List<PostgreDebugObjectDescriptor> res = new ArrayList<PostgreDebugObjectDescriptor>();

            while (rs.next()) {
                int oid = rs.getInt("oid");
                String proname = rs.getString("proname");
                String owner = rs.getString("owner");
                String nspname = rs.getString("nspname");
                String lang = rs.getString("lang");
                PostgreDebugObjectDescriptor object = new PostgreDebugObjectDescriptor(oid, proname, owner, nspname,
                        lang);
                res.add(object);
            }

            return res;

        } catch (SQLException e) {
            throw new DBGException("SQL error", e);
        }
    }

    @Override
    public PostgreDebugSession createSession(DBRProgressMonitor monitor, DBGSessionInfo targetInfo, DBCExecutionContext sessionContext)
            throws DBGException
    {
        PostgreDebugSessionInfo sessionInfo = getSessionDescriptor(monitor, sessionContext);

        return new PostgreDebugSession(this, sessionInfo, targetInfo.getID());
    }

    @Override
    public void attachSession(DBRProgressMonitor monitor, DBGSession session, DBCExecutionContext sessionContext, Map<String, Object> configuration) throws DBException {
        PostgreDebugSession pgSession = (PostgreDebugSession) session;
        JDBCExecutionContext sessionJdbc = (JDBCExecutionContext) sessionContext;
        int oid = Integer.parseInt(String.valueOf(configuration.get(PROCEDURE_OID)));
        int pid = Integer.parseInt(String.valueOf(configuration.get(ATTACH_PROCESS)));
        String kind = String.valueOf(configuration.get(ATTACH_KIND));
        boolean global = DBGController.ATTACH_KIND_GLOBAL.equals(kind);
        String call = (String) configuration.get(SCRIPT_TEXT);
        pgSession.attach(sessionJdbc, oid, pid, global, call);
    }

    @Override
    public DBGBreakpointDescriptor describeBreakpoint(Map<String, Object> attributes) {
        Object oid = attributes.get(DBGController.PROCEDURE_OID);
        Object lineNumber = attributes.get(DBGController.BREAKPOINT_LINE_NUMBER);
        long parsed = Long.parseLong(String.valueOf(lineNumber));
        PostgreDebugBreakpointProperties properties = new PostgreDebugBreakpointProperties(parsed, false);
        return new PostgreDebugBreakpointDescriptor(oid, properties );
    }

}
