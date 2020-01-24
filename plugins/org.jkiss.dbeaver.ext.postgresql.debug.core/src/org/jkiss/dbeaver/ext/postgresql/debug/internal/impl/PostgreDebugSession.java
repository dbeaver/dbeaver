/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.debug.*;
import org.jkiss.dbeaver.debug.core.DebugUtils;
import org.jkiss.dbeaver.debug.jdbc.DBGJDBCSession;
import org.jkiss.dbeaver.ext.postgresql.PostgreConstants;
import org.jkiss.dbeaver.ext.postgresql.debug.PostgreDebugConstants;
import org.jkiss.dbeaver.ext.postgresql.debug.core.PostgreSqlDebugCore;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDataSource;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDatabase;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreProcedure;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreProcedureParameter;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCCallableStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSourceInfo;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.IOUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Typical scenario for debug session <br/>
 * <br/>
 * 0. create session (now it can only attached to target Procedure)<br/>
 * <br/>
 * 1. attach to target this method attaches to a debugging target and listening
 * on the given port - waiting for run procedure in other session(s) debugger
 * client should invoke this function after creation also created implicit
 * breakpoint for target procedure, after this call debug session in
 * <b>WAITING</b> state - isDone returns false and is isWaiting returns
 * true<br/>
 * <br/>
 * 2. when target procedure will called debug session implicit breakpoint will
 * be reached and session goes in state <b>READY</b> (isDone - true, isWaiting -
 * true) in this state possible to call getStack, getVariables, setVariables,
 * setBreakpoint or execStepXXX\continue<br/>
 * <br/>
 * 3. when execStepXXX or continue will called session goes in <b>WAITING</b>
 * state until next breakpoint or end of procedure will be reached <br/>
 */
public class PostgreDebugSession extends DBGJDBCSession {

    private final JDBCExecutionContext controllerConnection;

    private int functionOid = -1;
    private int sessionId = -1;
    private int localPortNumber = -1;

    private PostgreDebugAttachKind attachKind = PostgreDebugAttachKind.UNKNOWN;
    private DBGSessionInfo sessionInfo;

    private PostgreDebugBreakpointDescriptor bpGlobal;
    private volatile JDBCCallableStatement localStatement;

    private static final int LOCAL_WAIT = 50; // 0.5 sec

    private static final int LOCAL_TIMEOT = 1000 * LOCAL_WAIT; // 50 sec

    private static final String MAGIC_PORT = "PLDBGBREAK";

    private static final String SQL_CHECK_PLUGIN = "select 'Server version: ' || serverversionstr || '.\nProxy API version: ' ||  proxyapiver from pldbg_get_proxy_info()";

    private static final String SQL_ATTACH = "select pldbg_wait_for_target(?sessionid)";
    private static final String SQL_ATTACH_TO_PORT = "select pldbg_attach_to_port(?portnumber)";
    private static final String SQL_PREPARE_SLOT = " select pldbg_oid_debug(?objectid)";
    private static final String SQL_LISTEN = "select pldbg_create_listener() as sessionid";
    private static final String SQL_GET_SRC = "select pldbg_get_source(?sessionid,?oid)";
    private static final String SQL_GET_VARS = "select * from pldbg_get_variables(?sessionid)";
    private static final String SQL_SET_VAR = "select pldbg_deposit_value(?,?,?,?)";
    private static final String SQL_GET_STACK = "select * from pldbg_get_stack(?sessionid)";
    private static final String SQL_SELECT_FRAME = "select * from pldbg_select_frame(?sessionid,?frameno)";
    private static final String SQL_STEP_OVER = "select pldbg_step_over(?sessionid)";
    private static final String SQL_STEP_INTO = "select pldbg_step_into(?sessionid)";
    private static final String SQL_CONTINUE = "select pldbg_continue(?sessionid)";
    private static final String SQL_ABORT = "select pldbg_abort_target(?sessionid)";
    private static final String SQL_SET_GLOBAL_BREAKPOINT = "select pldbg_set_global_breakpoint(?sessionid, ?obj, ?line, ?target)";
    private static final String SQL_SET_BREAKPOINT = "select pldbg_set_breakpoint(?sessionid, ?obj, ?line)";
    private static final String SQL_DROP_BREAKPOINT = "select pldbg_drop_breakpoint(?sessionid, ?obj, ?line)";

    private static final String SQL_CURRENT_SESSION =
        "SELECT pid,usename,application_name,state,query\n" +
            "FROM pg_stat_activity WHERE pid = pg_backend_pid()"; //$NON-NLS-1$


    private static final Log log = Log.getLog(PostgreDebugSession.class);

    /**
     * Create session with two description after creation session need to be
     * attached to postgres procedure by attach method
     */
    PostgreDebugSession(DBRProgressMonitor monitor, DBGBaseController controller)
        throws DBGException {
        super(controller);
        log.debug("Creating controller session.");

        PostgreDataSource dataSource = (PostgreDataSource) controller.getDataSourceContainer().getDataSource();
        try {
            log.debug("Controller session creating.");
            PostgreDatabase instance;
            if (isGlobalSession(controller.getDebugConfiguration())) {
                instance = dataSource.getDefaultInstance();
            } else {
                PostgreProcedure function = PostgreSqlDebugCore.resolveFunction(monitor, controller.getDataSourceContainer(), controller.getDebugConfiguration());
                instance = function.getDatabase();
            }
            this.controllerConnection = (JDBCExecutionContext) instance.openIsolatedContext(monitor, "Debug controller session", null);

            log.debug("Debug controller session created.");
            JDBCDataSource src = this.controllerConnection.getDataSource();
            if (src instanceof PostgreDataSource) {
                PostgreDataSource pgSrc = (PostgreDataSource) src;
                log.debug(String.format("Active user %s", instance.getMetaContext().getActiveUser()));
                log.debug(String.format("Active schema %s", instance.getMetaContext().getDefaultSchema()));
                if (pgSrc.getInfo() instanceof JDBCDataSourceInfo) {
                    JDBCDataSourceInfo JDBCinfo = (JDBCDataSourceInfo) pgSrc.getInfo();
                    log.debug("------------DATABASE DRIVER INFO---------------");
                    log.debug(String.format("Database Product Name %s", JDBCinfo.getDatabaseProductName()));
                    log.debug(String.format("Database Product Version %s", JDBCinfo.getDatabaseProductVersion()));
                    log.debug(String.format("Database Version %s", JDBCinfo.getDatabaseVersion()));
                    log.debug(String.format("Driver Name %s", JDBCinfo.getDriverName()));
                    log.debug(String.format("Driver Version %s", JDBCinfo.getDriverVersion()));
                    log.debug("-----------------------------------------------");
                } else {
                    log.debug("No additional Driver info");
                }
            } else {
                log.debug("Unknown Driver version");
            }

        } catch (DBException e) {
            log.debug(String.format("Error creating debug session %s", e.getMessage()));
            throw new DBGException(e, dataSource);
        }
    }

    @Override
    public JDBCExecutionContext getControllerConnection() {
        return controllerConnection;
    }

    private PostgreDebugSessionInfo getSessionDescriptor(DBRProgressMonitor monitor, JDBCExecutionContext connection) throws DBGException {
        try (JDBCSession session = connection.openSession(monitor, DBCExecutionPurpose.UTIL, "Read session info")) {
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

    private boolean localPortRcv(SQLWarning warn) {

        if (warn != null) {

            String notice = warn.getMessage();

            log.debug("Start local port waiting....");

            while (notice != null) {

                if (notice.startsWith(MAGIC_PORT)) {

                    try {
                        localPortNumber = Integer.valueOf(notice.substring(MAGIC_PORT.length() + 1).trim());
                        log.debug(String.format("Catch local port number %d", localPortNumber));
                    } catch (Exception e) {
                        log.debug(String.format("Error catching local port number %s", e.getMessage()));
                        return false;
                    }

                    return true;
                }

                warn = warn.getNextWarning();

                notice = warn == null ? null : warn.getMessage();
                log.debug(String.format("Next warning %s", (notice == null ? "[NULL]" : notice)));

            }
        }

        return false;
    }

    private int attachToPort(DBRProgressMonitor monitor) throws DBGException {
        // Use controller connection
        String sql = SQL_ATTACH_TO_PORT.replaceAll("\\?portnumber", String.valueOf(localPortNumber));
        log.debug(String.format("Attach to local port number %d", localPortNumber));
        try (JDBCSession session = getControllerConnection().openSession(monitor, DBCExecutionPurpose.UTIL, "Attach to port")) {
            try (Statement stmt = session.createStatement()) {
                try (ResultSet rs = stmt.executeQuery(sql)) {
                    if (rs.next()) {
                        int attResult = rs.getInt(1);
                        log.debug(String.format("Attached to local port %d", attResult));
                        return attResult;
                    }
                    log.debug("Error while attaching to port");
                    throw new DBGException("Error while attaching to port");
                }
            }
        } catch (SQLException e) {
            log.debug("Error while attaching to port");
            throw new DBGException("Error attaching to port", e);
        }
    }

    private String createSlot(DBRProgressMonitor monitor, JDBCExecutionContext connection, PostgreProcedure function) throws DBGException {

        String objId = String.valueOf(function.getObjectId());
        String sql = SQL_PREPARE_SLOT.replaceAll("\\?objectid", objId);
        log.debug(String.format("Create slot for object ID %s", objId));
        try (JDBCSession session = connection.openSession(monitor, DBCExecutionPurpose.UTIL, "Attach to port")) {
            try (Statement stmt = session.createStatement()) {
                try (ResultSet rs = stmt.executeQuery(sql)) {
                    if (!rs.next()) {
                        log.debug("Error creating target slot");
                        throw new DBGException("Error creating target slot");
                    }
                    String dbgOID = rs.getString(1);
                    log.debug(String.format("Create slot OID %s", dbgOID));
                    return dbgOID;
                }
            }
        } catch (SQLException e) {
            log.debug(String.format("Error creating target %s", e.getMessage()));
            throw new DBGException("Error creating target", e);
        }
    }

    /**
     * Wait for port number passed from main executed statement
     */
    private void waitPortNumber() throws DBGException {

        int totalWait = 0;
        boolean hasStatement = false;
        log.debug(String.format("Waiting for port number with timeout %d", LOCAL_TIMEOT));
        while (totalWait < LOCAL_TIMEOT) {
            try {
                CallableStatement statement = this.localStatement;
                if (statement != null) {
                    hasStatement = true;
                    if (localPortRcv(statement.getWarnings())) {
                        log.debug("Local port recived");
                        break;
                    }
                } else if (hasStatement) {
                    // Statement has been closed
                    log.debug("Statement has been closed");
                    break;
                }
                // Please forgive me !
                Thread.sleep(LOCAL_WAIT);
                log.debug("Thread waked up");

            } catch (SQLException | InterruptedException e) {
                log.debug(String.format("Error rcv port number %s", e.getMessage()));
                throw new DBGException("Error rcv port number", e);
            }

            totalWait += LOCAL_WAIT;

        }

        if (localPortNumber < 0) {
            log.debug(String.format("Unable to rcv port number %d", localPortNumber));
            throw new DBGException("Unable to rcv port number");
        }
    }

    protected void runLocalProc(JDBCExecutionContext connection, PostgreProcedure function, List<String> paramValues, String name) throws DBGException {
        List<PostgreProcedureParameter> parameters = function.getInputParameters();
        log.debug("Run local proc");
        if (parameters.size() != paramValues.size()) {
            String unmatched = "Parameter value count (" + paramValues.size() + ") doesn't match actual function parameters (" + parameters.size() + ")";
            log.debug(unmatched);
            throw new DBGException(unmatched);
        }
        Job job = new AbstractJob(name) {
            @Override
            protected IStatus run(DBRProgressMonitor monitor) {
                try (JDBCSession session = connection.openSession(monitor, DBCExecutionPurpose.USER, "Run SQL command")) {
                    try {
                        StringBuilder query = new StringBuilder();
                        query.append("{ CALL ").append(function.getFullyQualifiedName(DBPEvaluationContext.DML)).append("(");
                        for (int i = 0; i < parameters.size(); i++) {
                            if (i > 0) query.append(",");
                            String paramValue = paramValues.get(i);
                            query.append(paramValue);
                        }
                        query.append(") }");
                        log.debug(String.format("Prepared local call %s", query));
                        localStatement = session.prepareCall(query.toString());

/*
                        for (int i = 0; i < parameters.size(); i++) {
                            PostgreProcedureParameter parameter = parameters.get(i);
                            String paramValue = paramValues.get(i);
                            DBDValueHandler valueHandler = DBUtils.findValueHandler(session, parameter);
                            valueHandler.bindValueObject(session, localStatement, parameter, i, paramValue);
                        }
*/
                        localStatement.execute();
                        // And Now His Watch Is Ended
                        log.debug("Local statement executed (ANHWIE)");
                        fireEvent(new DBGEvent(this, DBGEvent.RESUME, DBGEvent.STEP_RETURN));
                    } catch (Exception e) {
                        log.debug("Error execute local statement: " + e.getMessage());
                        String sqlState = e instanceof SQLException ? ((SQLException) e).getSQLState() : null;
                        if (!PostgreConstants.EC_QUERY_CANCELED.equals(sqlState)) {
                            log.error(name, e);
                            return DebugUtils.newErrorStatus(name, e);
                        }
                    } finally {
                        try {
                            if (localStatement != null) {
                                localStatement.close();
                                localStatement = null;
                            }
                        } catch (Exception e1) {
                            log.debug("Error clearing local statment");
                            log.error(e1);
                        }
                        connection.close();

                        fireEvent(new DBGEvent(this, DBGEvent.TERMINATE, DBGEvent.CLIENT_REQUEST));
                    }
                }
                log.debug("Local statement executed");
                return Status.OK_STATUS;
            }
        };
        job.schedule();
    }

    private void attachLocal(DBRProgressMonitor monitor, PostgreProcedure function, List<String> parameters) throws DBGException {

        try {
            JDBCExecutionContext connection = (JDBCExecutionContext) controllerConnection.getOwnerInstance().openIsolatedContext(monitor, "Debug process session", null);
            log.debug("Attaching locally....");
            this.sessionInfo = getSessionDescriptor(monitor, connection);

            createSlot(monitor, connection, function);

            String taskName = "PostgreSQL Debug - Local session " + sessionInfo.getID();

            runLocalProc(connection, function, parameters, taskName);

            waitPortNumber();

            sessionId = attachToPort(monitor);
            log.debug(String.format("Attached local session UD = %d", sessionId));
            getController().fireEvent(new DBGEvent(this, DBGEvent.SUSPEND, DBGEvent.MODEL_SPECIFIC));
        } catch (DBException e) {
            throw new DBGException("Error opening debug session", e);
        }

    }

    private void attachGlobal(DBRProgressMonitor monitor, int oid, int targetPID) throws DBGException {

        log.debug("Attaching globally....");

        try (JDBCSession session = getControllerConnection().openSession(monitor, DBCExecutionPurpose.UTIL, "Attach global")) {
            try (Statement stmt = session.createStatement()) {
                try (ResultSet rs = stmt.executeQuery(SQL_LISTEN)) {

                    if (rs.next()) {
                        sessionId = rs.getInt("sessionid");
                        log.debug(String.format("Global session ID %d", sessionId));
                    } else {
                        log.debug("Unable to create debug instance");
                        throw new DBGException("Unable to create debug instance");
                    }

                }
            }
        } catch (SQLException e) {
            throw new DBGException("SQL error", e);
        }

        bpGlobal = new PostgreDebugBreakpointDescriptor(oid, -1);
        addBreakpoint(monitor, bpGlobal);
        log.debug("Global breakpoint added");

        String sessionParam = String.valueOf(getSessionId());
        String taskName = "PostgreSQL Debug - Global session " + sessionParam;
        String sql = SQL_ATTACH.replaceAll("\\?sessionid", sessionParam);
        DBGEvent begin = new DBGEvent(this, DBGEvent.RESUME, DBGEvent.MODEL_SPECIFIC);
        DBGEvent end = new DBGEvent(this, DBGEvent.SUSPEND, DBGEvent.BREAKPOINT);
        runAsync(sql, taskName, begin, end);
        log.debug("Global session started");
    }

    /**
     * This method attach debug session to debug object (procedure) and wait
     * forever while target or any (depend on targetPID) session will run target
     * procedure
     */
    public void attach(DBRProgressMonitor monitor, Map<String, Object> configuration) throws DBException {
        if (!checkDebugPlagin(monitor)) {
            throw new DBGException("PostgreSQL debug plugin is not installed on the server.\n" +
                "Refer to this WIKI article for installation instructions:\n" +
                "https://github.com/dbeaver/dbeaver/wiki/PGDebugger#installation");
        }

        log.debug("Attaching...");

        functionOid = CommonUtils.toInt(configuration.get(PostgreDebugConstants.ATTR_FUNCTION_OID));
        log.debug(String.format("Function OID %d", functionOid));

        boolean global = isGlobalSession(configuration);

        if (global) {
            int processId = CommonUtils.toInt(configuration.get(PostgreDebugConstants.ATTR_ATTACH_PROCESS));
            attachKind = PostgreDebugAttachKind.GLOBAL;
            attachGlobal(monitor, functionOid, processId);
            log.debug("Global attached");
        } else {
            attachKind = PostgreDebugAttachKind.LOCAL;
            PostgreProcedure function = PostgreSqlDebugCore.resolveFunction(monitor, controllerConnection.getDataSource().getContainer(), configuration);
            List<String> parameterValues = (List<String>) configuration.get(PostgreDebugConstants.ATTR_FUNCTION_PARAMETERS);

            attachLocal(monitor, function, parameterValues);
            log.debug("Local attached");
        }
    }

    private boolean isGlobalSession(Map<String, Object> configuration) {
        return PostgreDebugConstants.ATTACH_KIND_GLOBAL.equals(String.valueOf(configuration.get(PostgreDebugConstants.ATTR_ATTACH_KIND)));
    }

    private boolean checkDebugPlagin(DBRProgressMonitor monitor) {
        try (JDBCSession session = getControllerConnection().openSession(monitor, DBCExecutionPurpose.UTIL, "Check debug plugin installation")) {
            String version = JDBCUtils.executeQuery(session, SQL_CHECK_PLUGIN);
            log.debug("Debug plugin is installed:\n" + version);
            return true;
        } catch (Exception e) {
            log.debug("Debug plugin not installed: " + e.getMessage());
            return false;
        }
    }

    private void detachLocal(DBRProgressMonitor monitor, JDBCExecutionContext connection) throws DBGException {
        if (localStatement == null) {
            // Execution already terminated
            return;
        }
        try (JDBCSession session = connection.openSession(monitor, DBCExecutionPurpose.UTIL, "Abort local session")) {
            JDBCUtils.executeQuery(session, composeAbortCommand());
            log.debug("Local detached");
        } catch (SQLException e) {
            log.debug("Unable to abort local session");
            log.error("Unable to abort local target", e);
        }
    }

    private void detachGlobal(DBRProgressMonitor monitor) throws DBGException {
        removeBreakpoint(monitor, bpGlobal);

        try (JDBCSession session = getControllerConnection().openSession(monitor, DBCExecutionPurpose.UTIL, "Abort global session")) {
            JDBCUtils.executeQuery(session, composeAbortCommand());
            log.debug("Global deattached");
        } catch (SQLException e) {
            log.error("Unable to abort global target", e);
        }
    }

    protected void doDetach(DBRProgressMonitor monitor) throws DBGException {
        switch (attachKind) {
            case GLOBAL:
                detachGlobal(monitor);
                break;
            case LOCAL:
                detachLocal(monitor, getControllerConnection());
                break;
            default:
                break;
        }
    }

    @Override
    public DBGSessionInfo getSessionInfo() {
        return sessionInfo;
    }

    protected String composeAddBreakpointCommand(DBGBreakpointDescriptor descriptor) {
        PostgreDebugBreakpointDescriptor bp = (PostgreDebugBreakpointDescriptor) descriptor;
        String sqlPattern = attachKind == PostgreDebugAttachKind.GLOBAL ? SQL_SET_GLOBAL_BREAKPOINT : SQL_SET_BREAKPOINT;

        return sqlPattern.replaceAll("\\?sessionid", String.valueOf(getSessionId()))
            .replaceAll("\\?obj", String.valueOf(functionOid))
            .replaceAll("\\?line", bp.isOnStart() ? "-1" : String.valueOf(bp.getLineNo()))
            .replaceAll("\\?target", bp.isAll() ? "null" : String.valueOf(bp.getTargetId()));
    }

    protected String composeRemoveBreakpointCommand(DBGBreakpointDescriptor breakpointDescriptor) {
        PostgreDebugBreakpointDescriptor bp = (PostgreDebugBreakpointDescriptor) breakpointDescriptor;
        return SQL_DROP_BREAKPOINT.replaceAll("\\?sessionid", String.valueOf(getSessionId()))
            .replaceAll("\\?obj", String.valueOf(functionOid))
            .replaceAll("\\?line", bp.isOnStart() ? "-1" : String.valueOf(bp.getLineNo()));
    }

    @Override
    public void execContinue() throws DBGException {
        log.debug("try continue for");
        execStep(SQL_CONTINUE, " continue for ", DBGEvent.RESUME);
        log.debug("continue for realized");
    }

    @Override
    public void execStepInto() throws DBGException {
        log.debug("try step into");
        execStep(SQL_STEP_INTO, " step into for ", DBGEvent.STEP_INTO);
        log.debug("step into realized");
    }

    @Override
    public void execStepOver() throws DBGException {
        log.debug("try step over");
        execStep(SQL_STEP_OVER, " step over for ", DBGEvent.STEP_OVER);
        log.debug("step over realized");
    }

    @Override
    public void execStepReturn() throws DBGException {
        log.debug("Exec return not implemented");
        throw new DBGException("Exec return not implemented");
    }

    @Override
    public void resume() throws DBGException {
        log.debug("try continue execution");
        execContinue();
        log.debug("continue execution realized");
    }

    @Override
    public void suspend() throws DBGException {
        throw new DBGException("Suspend not implemented");
    }

    /**
     * Execute step SQL command asynchronously, set debug session name to
     * [sessionID] name [managerPID]
     *
     * @param commandPattern - SQL command for execute step
     * @param nameParameter  - session 'name' part
     */
    public void execStep(String commandPattern, String nameParameter, int eventDetail) throws DBGException {
        String sql = commandPattern.replaceAll("\\?sessionid", String.valueOf(sessionId));
        String taskName = String.valueOf(sessionId) + nameParameter + sessionInfo.getID();
        DBGEvent begin = new DBGEvent(this, DBGEvent.RESUME, eventDetail);
        DBGEvent end = new DBGEvent(this, DBGEvent.SUSPEND, eventDetail);
        runAsync(sql, taskName, begin, end);
    }

    protected String composeAbortCommand() {
        return SQL_ABORT.replaceAll("\\?sessionid", String.valueOf(sessionId));
    }

    @Override
    public List<DBGVariable<?>> getVariables(DBGStackFrame stack) throws DBGException {
        if (stack != null) {
            selectFrame(stack.getLevel());
        }

        log.debug("Get vars values");
        List<DBGVariable<?>> vars = new ArrayList<>();

        String sql = SQL_GET_VARS.replaceAll("\\?sessionid", String.valueOf(sessionId));
        try (JDBCSession session = getControllerConnection().openSession(new VoidProgressMonitor(), DBCExecutionPurpose.UTIL, "Read debug variables")) {
            try (Statement stmt = session.createStatement()) {
                try (ResultSet rs = stmt.executeQuery(sql)) {

                    while (rs.next()) {
                        String name = rs.getString("name");
                        String varclass = rs.getString("varclass");
                        int linenumber = rs.getInt("linenumber");
                        boolean isunique = rs.getBoolean("isunique");
                        boolean isconst = rs.getBoolean("isconst");
                        boolean isnotnull = rs.getBoolean("isnotnull");
                        int dtype = rs.getInt("dtype");
                        String value = rs.getString("value");
                        PostgreDebugVariable var = new PostgreDebugVariable(name, varclass, linenumber, isunique, isconst,
                            isnotnull, dtype, value);
                        vars.add(var);
                    }

                }
            }
        } catch (SQLException e) {
            log.debug("Error getting vars: " + e.getMessage());
            throw new DBGException("SQL error", e);
        }

        log.debug(String.format("Return %d var(s)", vars.size()));
        return vars;

    }

    @Override
    public void setVariableVal(DBGVariable<?> variable, Object value) throws DBGException {
        log.debug("Set var value");
        try (JDBCSession session = getControllerConnection().openSession(new VoidProgressMonitor(), DBCExecutionPurpose.UTIL, "Set debug variable")) {
            try (PreparedStatement stmt = session.prepareStatement(SQL_SET_VAR)) {

                if (variable instanceof PostgreDebugVariable) {

                    if (value instanceof String) {

                        PostgreDebugVariable var = (PostgreDebugVariable) variable;

                        stmt.setInt(1, sessionId);
                        stmt.setString(2, var.getName());
                        stmt.setInt(3, var.getLineNumber());
                        stmt.setString(4, (String) value);

                        stmt.execute();
                        log.debug("Var value set");
                    } else {
                        log.debug("Incorrect variable value class");
                        throw new DBGException("Incorrect variable value class");
                    }

                } else {
                    log.debug("Incorrect variable class");
                    throw new DBGException("Incorrect variable class");
                }
            }

        } catch (SQLException e) {
            log.debug("Error setting var: " + e.getMessage());
            throw new DBGException("SQL error", e);
        }
    }

    @Override
    public List<DBGStackFrame> getStack() throws DBGException {
        List<DBGStackFrame> stack = new ArrayList<>(1);
        log.debug("Get stack");

        String sql = SQL_GET_STACK.replaceAll("\\?sessionid", String.valueOf(getSessionId()));
        try (JDBCSession session = getControllerConnection().openSession(new VoidProgressMonitor(), DBCExecutionPurpose.UTIL, "Get debug stack frame")) {
            try (Statement stmt = session.createStatement()) {
                try (ResultSet rs = stmt.executeQuery(sql)) {
                    while (rs.next()) {
                        int level = rs.getInt("level");
                        String targetname = rs.getString("targetname");
                        int func = rs.getInt("func");
                        int linenumber = rs.getInt("linenumber");
                        String args = rs.getString("args");
                        PostgreDebugStackFrame frame = new PostgreDebugStackFrame(level, targetname, func, linenumber, args);
                        stack.add(frame);
                    }

                }
            }
        } catch (SQLException e) {
            log.debug("Error loading stack frame: " + e.getMessage());
            throw new DBGException("SQL error", e);
        }
        log.debug(String.format("Return %d stack frame(s)", stack.size()));
        return stack;
    }

    @Override
    public String getSource(DBGStackFrame stack) throws DBGException {
        log.debug("Get source");
        if (stack instanceof PostgreDebugStackFrame) {
            PostgreDebugStackFrame postgreStack = (PostgreDebugStackFrame) stack;
            String src = getSource(postgreStack.getOid());
            log.debug(String.format("Return %d src char(s)", src.length()));
            return src;
        }
        String message = String.format("Unable to get source for stack %s", stack);
        throw new DBGException(message);
    }

    /**
     * Return source for func OID in debug session
     *
     * @return String
     */

    public String getSource(int OID) throws DBGException {
        log.debug("Get source for func OID in debug session");
        String sql = SQL_GET_SRC.replaceAll("\\?sessionid", String.valueOf(sessionId)).replaceAll("\\?oid",
            String.valueOf(OID));
        try (JDBCSession session = getControllerConnection().openSession(new VoidProgressMonitor(), DBCExecutionPurpose.UTIL, "Get session source")) {
            try (Statement stmt = session.createStatement()) {
                try (ResultSet rs = stmt.executeQuery(sql)) {
                    if (rs.next()) {
                        String src = rs.getString(1);
                        log.debug(String.format("Return %d src char(s)", src.length()));
                        return src;
                    }
                    return null;
                }
            }
        } catch (SQLException e) {
            log.debug(String.format("Unable to get source for OID %s", e.getMessage()));
            throw new DBGException("SQL error", e);
        }
    }

    /**
     * This function changes the debugger focus to the indicated frame (in the
     * call stack). Whenever the target stops (at a breakpoint or as the result
     * of a step/into or step/over), the debugger changes focus to most deeply
     * nested function in the call stack (because that's the function that's
     * executing).
     * <p>
     * You can change the debugger focus to other stack frames - once you do
     * that, you can examine the source code for that frame, the variable values
     * in that frame, and the breakpoints in that target.
     * <p>
     * The debugger focus remains on the selected frame until you change it or
     * the target stops at another breakpoint.
     */

    public void selectFrame(int frameNumber) throws DBGException {
        log.debug("Select frame");
        String sql = SQL_SELECT_FRAME.replaceAll("\\?sessionid", String.valueOf(sessionId)).replaceAll("\\?frameno",
            String.valueOf(frameNumber));

        try (JDBCSession session = getControllerConnection().openSession(new VoidProgressMonitor(), DBCExecutionPurpose.UTIL, "Select debug frame")) {
            try (Statement stmt = session.createStatement()) {
                try (ResultSet rs = stmt.executeQuery(sql)) {
                    if (!rs.next()) {
                        log.debug("Unable to select frame");
                        throw new DBGException("Unable to select frame");
                    }

                    log.debug("Frame selected");

                }
            }
        } catch (SQLException e) {
            log.debug(String.format("Unable to select frame %s", e.getMessage()));
            throw new DBGException("SQL error", e);
        }
    }

    @Override
    public String toString() {
        return "PostgreDebugSession " + (isWaiting() ? "WAITING" : "READY") + " [sessionId=" + sessionId
            + ", breakpoints=" + getBreakpoints() + "targetId=(" + sessionInfo.getID() + ") Session=(" + sessionInfo.toString()
            + ") " + "]";
    }

    @Override
    public Integer getSessionId() {
        return sessionId;
    }

    @Override
    public boolean canStepInto() {
        return true;
    }

    @Override
    public boolean canStepOver() {
        return true;
    }

    @Override
    public boolean canStepReturn() {
        return true;
    }

    /**
     * Return true if debug session up and running on server
     *
     * @return boolean
     */
    public boolean isAttached() {
        return sessionId > 0;
    }

    /**
     * Return true if session waiting target connection (on breakpoint, after
     * step or continue) in debug thread
     *
     * @return boolean
     */
    public boolean isDone() {
        switch (attachKind) {
            case GLOBAL:
                return workerJob == null || workerJob.isFinished();
            case LOCAL:
                return sessionId > 0;
            default:
                return false;
        }
    }

    @Override
    public void closeSession(DBRProgressMonitor monitor) throws DBGException {
        if (!isAttached()) {
            return;
        }
        log.debug("Closing session.");
        try {
            super.closeSession(monitor);
            log.debug("Session closed.");
        } finally {
            if (controllerConnection != null) {
                IOUtils.close(controllerConnection);
            }
        }
    }

}
