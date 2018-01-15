/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2017 Alexander Fedorov (alexander.fedorov@jkiss.org)
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
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.util.NLS;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.debug.DBGBaseController;
import org.jkiss.dbeaver.debug.DBGException;
import org.jkiss.dbeaver.debug.DBGSession;
import org.jkiss.dbeaver.debug.DBGSessionInfo;
import org.jkiss.dbeaver.debug.core.DebugCore;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDataSource;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDatabase;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreProcedure;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreSchema;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.DBCStatement;
import org.jkiss.dbeaver.model.exec.DBCStatementType;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedure;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureParameter;

public class PostgreDebugController extends DBGBaseController {
    
    private static Log log = Log.getLog(PostgreDebugController.class);

    private static final String SQL_SESSION = "select pid,usename,application_name,state,query from pg_stat_activity"; //$NON-NLS-1$
    
    private static final String SQL_OBJECT = "select  p.oid,p.proname,u.usename as owner,n.nspname, l.lanname as lang " //$NON-NLS-1$
            + " from " + "  pg_catalog.pg_namespace n " + " join pg_catalog.pg_proc p on p.pronamespace = n.oid "  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
            + "  join pg_user u on u.usesysid =   p.proowner " + "   join pg_language l on l.oid = p. prolang " //$NON-NLS-1$ //$NON-NLS-2$
            + " where  " + "   l.lanname = 'plpgsql' " + "   and p.proname like '%?nameCtx%' "   //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
            + "  and u.usename like '%?userCtx%' " + "  order by  " + "  n.nspname,p.proname"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    
    private static final String SQL_CURRENT_SESSION = "select pid,usename,application_name,state,query from pg_stat_activity where pid = pg_backend_pid()"; //$NON-NLS-1$
    
    public PostgreDebugController(DBPDataSourceContainer dataSourceDescriptor) {
        super(dataSourceDescriptor);
    }
    
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
        DBCExecutionContext executionContext = getExecutionContext();
        try (Statement stmt = getConnection(executionContext).createStatement(); ResultSet rs = stmt.executeQuery(SQL_SESSION)) {
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

    @Override
    public List<PostgreDebugObjectDescriptor> getObjects(String ownerCtx, String nameCtx) throws DBGException {
        DBCExecutionContext executionContext = getExecutionContext();
        String sql = SQL_OBJECT.replaceAll("\\?nameCtx", nameCtx).replaceAll("\\?userCtx", ownerCtx).toLowerCase();
        try (Statement stmt = getConnection(executionContext).createStatement(); ResultSet rs = stmt.executeQuery(sql)) {

            List<PostgreDebugObjectDescriptor> res = new ArrayList<PostgreDebugObjectDescriptor>();

            while (rs.next()) {
                int oid = rs.getInt("oid");
                String proname = rs.getString("proname");
                String owner = rs.getString("owner");
                String nspname = rs.getString("nspname");
                String lang = rs.getString("lang");
                PostgreDebugObjectDescriptor object = new PostgreDebugObjectDescriptor(oid, proname, owner, nspname, lang);
                res.add(object);
            }

            return res;

        } catch (SQLException e) {
            throw new DBGException("SQL error", e);
        }
    }

    @Override
    public PostgreDebugSession createDebugSession(DBGSessionInfo targetInfo, DBCExecutionContext sessionContext) throws DBGException {
        PostgreDebugSessionInfo sessionInfo = getSessionInfo(sessionContext);
        PostgreDebugSession debugSession = new PostgreDebugSession(sessionInfo, targetInfo.getID());
        
        return debugSession;

    }
    
    @Override
    public void attachSession(DBGSession session, DBCExecutionContext sessionContext, Map<String, Object> configuration, DBRProgressMonitor monitor) throws DBException {
        PostgreDebugSession pgSession = (PostgreDebugSession) session;
        JDBCExecutionContext sessionJdbc = (JDBCExecutionContext) sessionContext;
        //FIXME 16749 - OID for debug proc
        //FIXME -1 - target PID (-1 for ANY PID)
        int oid = Integer.parseInt(String.valueOf(configuration.get(PROCEDURE_OID)));
        int pid = Integer.parseInt(String.valueOf(configuration.get(PROCESS_ID)));
        String databaseName = String.valueOf(configuration.get(DATABASE_NAME));
        pgSession.attach(sessionJdbc, oid, pid);
        DBPDataSource dataSource = sessionContext.getDataSource();
        executeProcedure(configuration, monitor, oid, databaseName, dataSource);
    }

    private void executeProcedure(Map<String, Object> configuration, DBRProgressMonitor monitor, int oid,
            String databaseName, DBPDataSource dataSource) throws DBException {
        if (dataSource instanceof PostgreDataSource) {
            PostgreDataSource pgDS = (PostgreDataSource) dataSource;
            PostgreDatabase database = pgDS.getDatabase(databaseName);
            PostgreSchema schema = database.getSchema(monitor, "public");
            PostgreProcedure procedure = schema.getProcedure(monitor, oid);
            String call = composeProcedureCall(procedure, configuration, monitor);
            String taskName = NLS.bind("Execute procedure {0}", procedure.getName());
            Job job = new Job(taskName) {
                
                @Override
                protected IStatus run(IProgressMonitor monitor) {
                    try {
                        try (final DBCSession execSession = DBUtils.openUtilSession(new VoidProgressMonitor(), dataSource, taskName)) {
                            try (final DBCStatement dbStat = execSession.prepareStatement(DBCStatementType.EXEC, call, true, false,
                                    false)) {
                                dbStat.executeStatement();
                            }
                        }
                    } catch (DBCException e) {
                        log.error(taskName, e);
                        return DebugCore.newErrorStatus(taskName, e);
                        
                    }
                    return Status.OK_STATUS;
                }
            };
            job.schedule();
        }
    }

    private String composeProcedureCall(DBSProcedure procedure, Map<String, Object> configuration,
            DBRProgressMonitor monitor) throws DBException {
        StringBuilder sb = new StringBuilder();
        sb.append("select").append(' ').append(procedure.getName());
        sb.append('(');
        Collection<? extends DBSProcedureParameter> parameters = procedure.getParameters(monitor);
        if (parameters.size() > 0) {
            for (DBSProcedureParameter parameter : parameters) {
                String name = parameter.getName();
                Object value = configuration.get(name);
                if (value == null) {
                    value = '?';
                    sb.append(value);
                } else {
                    DBSTypedObject parameterType = parameter.getParameterType();
                    DBPDataKind dataKind = parameterType.getDataKind();
                    switch (dataKind) {
                    case STRING:
                        sb.append('\'');
                        sb.append(value);
                        sb.append('\'');
                        break;

                    default:
                        sb.append(value);
                        break;
                    }
                }
                sb.append(',');
            }
            sb.deleteCharAt(sb.length()-1);
        }
        sb.append(')');
        String call = sb.toString();
        return call;
    }
    
}
