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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.debug.*;
import org.jkiss.dbeaver.debug.core.DebugCore;
import org.jkiss.dbeaver.ext.postgresql.PostgreConstants;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.utils.IOUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

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
 *
 */
public class PostgreDebugSession extends DBGBaseSession {

    private final JDBCExecutionContext connection;
    private final JDBCExecutionContext controllerConnection;

    private final DBGSessionInfo sessionInfo;

    private int sessionId = -1;
    private int localPortNumber = -1;

    private PostgreDebugAttachKind attachKind = PostgreDebugAttachKind.UNKNOWN;

    private PostgreDebugBreakpointDescriptor bpGlobal;
    private volatile Statement localStatement;

    private static final int LOCAL_WAIT = 50; // 0.5 sec

    private static final int LOCAL_TIMEOT = 1000 * LOCAL_WAIT; // 50 sec

    private static final String MAGIC_PORT = "PLDBGBREAK";

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

        DBPDataSource dataSource = controller.getDataSourceContainer().getDataSource();
        try {
            this.connection = (JDBCExecutionContext) dataSource.openIsolatedContext(monitor, "Debug process session");
            this.controllerConnection = (JDBCExecutionContext) dataSource.openIsolatedContext(monitor, "Debug controller session");
        } catch (DBException e) {
            throw new DBGException(e, dataSource);
        }

        this.sessionInfo = getSessionDescriptor(monitor);
    }

    public JDBCExecutionContext getConnection() {
        return connection;
    }

    @Override
    public JDBCExecutionContext getControllerConnection() {
        return controllerConnection;
    }

    private PostgreDebugSessionInfo getSessionDescriptor(DBRProgressMonitor monitor) throws DBGException {
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

            while (notice != null) {

                if (notice.startsWith(MAGIC_PORT)) {

                    try {
                        localPortNumber = Integer.valueOf(notice.substring(MAGIC_PORT.length() + 1).trim());
                    } catch (Exception e) {
                        return false;
                    }

                    return true;
                }

                warn = warn.getNextWarning();

                notice = warn == null ? null : warn.getMessage();

            }
        }

        return false;
    }

    private int attachToPort(DBRProgressMonitor monitor) throws DBGException {
        // Use controller connection
        String sql = SQL_ATTACH_TO_PORT.replaceAll("\\?portnumber", String.valueOf(localPortNumber));
        try (JDBCSession session = getControllerConnection().openSession(monitor, DBCExecutionPurpose.UTIL, "Attach to port")) {
            try (Statement stmt = session.createStatement()) {
                try (ResultSet rs = stmt.executeQuery(sql)) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }

                    throw new DBGException("Error while attaching to port");
                }
            }
        } catch (SQLException e) {
            throw new DBGException("Error attaching to port", e);
        }
    }

    private String createSlot(DBRProgressMonitor monitor, int OID) throws DBGException {

        String sql = SQL_PREPARE_SLOT.replaceAll("\\?objectid", String.valueOf(OID));
        try (JDBCSession session = getConnection().openSession(monitor, DBCExecutionPurpose.UTIL, "Attach to port")) {
            try (Statement stmt = session.createStatement()) {
                try (ResultSet rs = stmt.executeQuery(sql)) {
                    if (!rs.next()) {
                        throw new DBGException("Error creating target slot");
                    }
                    return rs.getString(1);
                }
            }
        } catch (SQLException e) {
            throw new DBGException("Error creating target", e);
        }
    }

    /**
     * Wait for port number passed from main executed statement
     */
    private void waitPortNumber() throws DBGException {

        int totalWait = 0;
        boolean hasStatement = false;
        while (totalWait < LOCAL_TIMEOT) {
            try {
                if (localStatement != null) {
                    hasStatement = true;
                    if (localPortRcv(localStatement.getWarnings())) {
                        break;
                    }
                } else if (hasStatement) {
                    // Statement has been closed
                    break;
                }
                // Please forgive me !
                Thread.sleep(LOCAL_WAIT);

            } catch (SQLException | InterruptedException e) {
                throw new DBGException("Error rcv port number", e);
            }

            totalWait += LOCAL_WAIT;

        }

        if (localPortNumber < 0) {
            throw new DBGException("Unable to rcv port number");
        }
    }

    protected void runLocalProc(String commandSQL, String name) throws DBGException {
        Job job = new AbstractJob(name) {
            @Override
            protected IStatus run(DBRProgressMonitor monitor) {
                try (JDBCSession session = getConnection().openSession(monitor, DBCExecutionPurpose.UTIL, "Run SQL command")) {
                    localStatement = session.createStatement();
                    localStatement.execute(commandSQL);
                    // And Now His Watch Is Ended
                    fireEvent(new DBGEvent(this, DBGEvent.RESUME, DBGEvent.STEP_RETURN));
                } catch (SQLException e) {
                    try {
                        if (localStatement != null) {
                            localStatement.close();
                            localStatement = null;
                        }
                    } catch (SQLException e1) {
                        log.error(e1);
                    }
                    fireEvent(new DBGEvent(this, DBGEvent.TERMINATE, DBGEvent.CLIENT_REQUEST));
                    String sqlState = e.getSQLState();
                    if (!PostgreConstants.EC_QUERY_CANCELED.equals(sqlState)) {
                        log.error(name, e);
                        return DebugCore.newErrorStatus(name, e);
                    }
                }
                return Status.OK_STATUS;
            }
        };
        job.schedule();
    }

    private void attachLocal(DBRProgressMonitor monitor, int OID, String call) throws DBGException {

        createSlot(monitor, OID);

        String taskName = "Local attached to " + sessionInfo.getID();

        runLocalProc(call, taskName);

        waitPortNumber();

        sessionId = attachToPort(monitor);
        getController().fireEvent(new DBGEvent(this, DBGEvent.SUSPEND, DBGEvent.MODEL_SPECIFIC));
    }

    private void attachGlobal(DBRProgressMonitor monitor, int oid, String call, int targetPID) throws DBGException {

        try (JDBCSession session = getControllerConnection().openSession(monitor, DBCExecutionPurpose.UTIL, "Attach global")) {
            try (Statement stmt = session.createStatement()) {
                try (ResultSet rs = stmt.executeQuery(SQL_LISTEN)) {

                    if (rs.next()) {
                        sessionId = rs.getInt("sessionid");
                    } else {
                        throw new DBGException("Unable to create debug instance");
                    }

                }
            }
        } catch (SQLException e) {
            throw new DBGException("SQL error", e);
        }

        PostgreDebugBreakpointProperties properties = new PostgreDebugBreakpointProperties(true);
        bpGlobal = new PostgreDebugBreakpointDescriptor(oid, properties);
        addBreakpoint(monitor, bpGlobal);

        String sessionParam = String.valueOf(getSessionId());
        String taskName = sessionParam + " global attached to " + sessionInfo.getID();
        String sql = SQL_ATTACH.replaceAll("\\?sessionid", sessionParam);
        DBGEvent begin = new DBGEvent(this, DBGEvent.RESUME, DBGEvent.MODEL_SPECIFIC);
        DBGEvent end = new DBGEvent(this, DBGEvent.SUSPEND, DBGEvent.BREAKPOINT);
        runAsync(sql, taskName, begin, end);
    }

    /**
     * This method attach debug session to debug object (procedure) and wait
     * forever while target or any (depend on targetPID) session will run target
     * procedure
     * 
     *
     * @param monitor
     * @param OID
     *            - OID for target procedure
     * @param targetPID
     *            - target session PID (-1 for any target)
     * @param global
     *            - is target session global
     * @param call
     *            - SQL call for target session
     */
    public void attach(DBRProgressMonitor monitor, int OID, int targetPID, boolean global, String call) throws DBGException {
        if (global) {
            attachKind = PostgreDebugAttachKind.GLOBAL;
            attachGlobal(monitor, OID, call, targetPID);
        } else {
            attachKind = PostgreDebugAttachKind.LOCAL;
            attachLocal(monitor, OID, call);
        }
    }

    private void detachLocal(DBRProgressMonitor monitor) throws DBGException {
        try (JDBCSession session = getControllerConnection().openSession(monitor, DBCExecutionPurpose.UTIL, "Abort local session")) {
            JDBCUtils.executeQuery(session, composeAbortCommand());
        } catch (SQLException e) {
            log.error("Unable to abort target", e);
        }
    }

    private void detachGlobal(DBRProgressMonitor monitor) throws DBGException {
        removeBreakpoint(monitor, bpGlobal);

        try (JDBCSession session = getControllerConnection().openSession(monitor, DBCExecutionPurpose.UTIL, "Abort global session")) {
            JDBCUtils.executeQuery(session, composeAbortCommand());
        } catch (SQLException e) {
            log.error("Unable to abort target", e);
        }
    }

    protected void doDetach(DBRProgressMonitor monitor) throws DBGException {
        switch (attachKind) {
        case GLOBAL:
            detachGlobal(monitor);
            break;
        case LOCAL:
            detachLocal(monitor);
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
        PostgreDebugBreakpointProperties bpd = bp.getProperties();
        String sqlPattern = bpd.isGlobal() ? SQL_SET_GLOBAL_BREAKPOINT : SQL_SET_BREAKPOINT;

        String sqlCommand = sqlPattern.replaceAll("\\?sessionid", String.valueOf(getSessionId()))
                .replaceAll("\\?obj", String.valueOf(descriptor.getObjectId()))
                .replaceAll("\\?line", bpd.isOnStart() ? "-1" : String.valueOf(bpd.getLineNo()))
                .replaceAll("\\?target", bpd.isAll() ? "null" : String.valueOf(bpd.getTargetId()));
        return sqlCommand;
    }

    protected String composeRemoveBreakpointCommand(DBGBreakpointDescriptor bp) {
        PostgreDebugBreakpointProperties properties = (PostgreDebugBreakpointProperties) bp.getProperties();
        String sqlCommand = SQL_DROP_BREAKPOINT.replaceAll("\\?sessionid", String.valueOf(getSessionId()))
                .replaceAll("\\?obj", String.valueOf(bp.getObjectId()))
                .replaceAll("\\?line", properties.isOnStart() ? "-1" : String.valueOf(properties.getLineNo()));
        return sqlCommand;
    }

    @Override
    public void execContinue() throws DBGException {
        execStep(SQL_CONTINUE, " continue for ", DBGEvent.RESUME);
    }

    @Override
    public void execStepInto() throws DBGException {
        execStep(SQL_STEP_INTO, " step into for ", DBGEvent.STEP_INTO);
    }

    @Override
    public void execStepOver() throws DBGException {
        execStep(SQL_STEP_OVER, " step over for ", DBGEvent.STEP_OVER);

    }

    /**
     * Execute step SQL command asynchronously, set debug session name to
     * [sessionID] name [managerPID]
     * 
     * @param commandPattern
     *            - SQL command for execute step
     * @param nameParameter
     *            - session 'name' part
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
    public List<DBGVariable<?>> getVariables() throws DBGException {
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
            throw new DBGException("SQL error", e);
        }

        return vars;

    }

    @Override
    public void setVariableVal(DBGVariable<?> variable, Object value) throws DBGException {

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

                    } else {
                        throw new DBGException("Incorrect variable value class");
                    }

                } else {
                    throw new DBGException("Incorrect variable class");
                }
            }

        } catch (SQLException e) {
            throw new DBGException("SQL error", e);
        }
    }

    @Override
    public List<DBGStackFrame> getStack() throws DBGException {
        List<DBGStackFrame> stack = new ArrayList<>(1);

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
            throw new DBGException("SQL error", e);
        }
        return stack;
    }

    @Override
    public String getSource(DBGStackFrame stack) throws DBGException {
        if (stack instanceof PostgreDebugStackFrame) {
            PostgreDebugStackFrame postgreStack = (PostgreDebugStackFrame) stack;
            return getSource(postgreStack.getOid());
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
        String sql = SQL_GET_SRC.replaceAll("\\?sessionid", String.valueOf(sessionId)).replaceAll("\\?oid",
                String.valueOf(OID));
        try (JDBCSession session = getControllerConnection().openSession(new VoidProgressMonitor(), DBCExecutionPurpose.UTIL, "Get session source")) {
            try (Statement stmt = session.createStatement()) {
                try (ResultSet rs = stmt.executeQuery(sql)) {
                    if (rs.next()) {
                        return rs.getString(1);
                    }
                    return null;
                }
            }
        } catch (SQLException e) {
            throw new DBGException("SQL error", e);
        }
    }

    /**
     * This function changes the debugger focus to the indicated frame (in the
     * call stack). Whenever the target stops (at a breakpoint or as the result
     * of a step/into or step/over), the debugger changes focus to most deeply
     * nested function in the call stack (because that's the function that's
     * executing).
     *
     * You can change the debugger focus to other stack frames - once you do
     * that, you can examine the source code for that frame, the variable values
     * in that frame, and the breakpoints in that target.
     *
     * The debugger focus remains on the selected frame until you change it or
     * the target stops at another breakpoint.
     */

    public void selectFrame(int frameNumber) throws DBGException {
        String sql = SQL_SELECT_FRAME.replaceAll("\\?sessionid", String.valueOf(sessionId)).replaceAll("\\?frameno",
                String.valueOf(frameNumber));

        try (JDBCSession session = getControllerConnection().openSession(new VoidProgressMonitor(), DBCExecutionPurpose.UTIL, "Select debug frame")) {
            try (Statement stmt = session.createStatement()) {
                try (ResultSet rs = stmt.executeQuery(sql)) {
                    if (!rs.next()) {
                        throw new DBGException("Unable to select frame");
                    }

                }
            }
        } catch (SQLException e) {
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

    /**
     * Return true if debug session up and running on server
     * 
     * @return boolean
     */
    public boolean isAttached() {
        switch (attachKind) {
        case GLOBAL:
            return connection != null && (sessionId > 0);
        case LOCAL:
            return sessionId > 0;
        default:
            return false;
        }

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
        try {
            super.closeSession(monitor);
        } finally {
            if (connection != null) {
                IOUtils.close(connection);
            }
            if (controllerConnection != null) {
                IOUtils.close(controllerConnection);
            }
        }
    }

}
