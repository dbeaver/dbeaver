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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.debug.DBGBaseController;
import org.jkiss.dbeaver.debug.DBGBaseSession;
import org.jkiss.dbeaver.debug.DBGBreakpointDescriptor;
import org.jkiss.dbeaver.debug.DBGEvent;
import org.jkiss.dbeaver.debug.DBGException;
import org.jkiss.dbeaver.debug.DBGSessionInfo;
import org.jkiss.dbeaver.debug.DBGStackFrame;
import org.jkiss.dbeaver.debug.DBGVariable;
import org.jkiss.dbeaver.debug.core.DebugCore;
import org.jkiss.dbeaver.ext.postgresql.PostgreConstants;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;

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

    private final DBGSessionInfo sessionInfo;
    private final Object targetId;

    private int sessionId = -1;

    private int localPortNumber = -1;

    private PostgreDebugAttachKind attachKind = PostgreDebugAttachKind.UNKNOWN;

    private Statement localStatement;

    private Job job;

    private Connection executionTarget;

    private PostgreDebugBreakpointDescriptor bpGlobal;

    private static final int LOCAL_WAIT = 500; // 0.5 sec

    private static final int LOCAL_TIMEOT = 50 * 1000; // 50 sec

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
    // private static final String SQL_ATTACH_BREAKPOINT = "select
    // pldbg_wait_for_breakpoint(?sessionid)";

    private static final Log log = Log.getLog(PostgreDebugSession.class);

    /**
     * Create session with two description after creation session need to be
     * attached to postgres procedure by attach method
     * 
     * @param sessionManagerInfo
     *            - manager (caller connection) description
     * @param sessionDebugInfo
     *            - session (debugger client connection) description
     * @throws DBGException
     */
    PostgreDebugSession(DBGBaseController controller, PostgreDebugSessionInfo sessionInfo, Object targetId)
            throws DBGException {
        super(controller);
        this.sessionInfo = sessionInfo;
        this.targetId = targetId;
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

    private int attachToPort() throws DBGException {

        String sql = SQL_ATTACH_TO_PORT.replaceAll("\\?portnumber", String.valueOf(localPortNumber));
        try (Statement stmt = getConnection().createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }

            throw new DBGException("Error while attaching to port");

        } catch (SQLException e) {
            throw new DBGException("Error attaching to port", e);
        }
    }

    private void createSlot(Connection executionTarget, int OID) throws DBGException {

        String sql = SQL_PREPARE_SLOT.replaceAll("\\?objectid", String.valueOf(OID));
        try (Statement stmt = executionTarget.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            if (!rs.next()) {
                throw new DBGException("Error creating target slot");
            }

        } catch (SQLException e) {
            throw new DBGException("Error creating target", e);
        }
    }

    private Connection createExecutionTarget() throws DBGException {

        DBPDataSource dataSource = getController().getDataSourceContainer().getDataSource();
        if (!getController().getDataSourceContainer().isConnected()) {
            throw new DBGException("Not connected to database");
        }

        try {
            return ((JDBCExecutionContext) dataSource.openIsolatedContext(new VoidProgressMonitor(),
                    "Target debug session")).getConnection(new VoidProgressMonitor());
        } catch (DBException | SQLException e) {
            throw new DBGException("Error creating target session", e);
        }
    }

    private void waitPortNumber() throws DBGException {

        int totalWait = 0;

        while (totalWait < LOCAL_TIMEOT) {

            try {

                if (localStatement != null) {

                    if (localPortRcv(localStatement.getWarnings())) {
                        break;
                    }

                }

                // Please forgive me !
                Thread.sleep(LOCAL_WAIT);

            } catch (SQLException | InterruptedException e) {
                throw new DBGException("Error rcv port number");
            }

            totalWait += LOCAL_WAIT;

        }

        if (localPortNumber < 0) {
            throw new DBGException("Unable to rcv port number");
        }
    }

    protected void runProc(Connection connection, String commandSQL, String name) throws DBGException {
        job = new Job(name) {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                try {
                    try (final Statement stmt = connection.createStatement()) {
                        localStatement = stmt;
                        stmt.execute(commandSQL);
                        // And Now His Watch Is Ended
                        fireEvent(new DBGEvent(this, DBGEvent.RESUME, DBGEvent.STEP_RETURN));
                    }
                } catch (SQLException e) {
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

    private void attachLocal(int OID, String call) throws DBGException {

        executionTarget = createExecutionTarget();

        createSlot(executionTarget, OID);

        String taskName = "Local attached to " + String.valueOf(targetId);

        runProc(executionTarget, call, taskName);

        waitPortNumber();

        sessionId = attachToPort();
        getController().fireEvent(new DBGEvent(this, DBGEvent.SUSPEND, DBGEvent.MODEL_SPECIFIC));

        try {
            getConnection().setClientInfo("ApplicationName", "Debug Mode (local) : " + String.valueOf(sessionId));
        } catch (SQLClientInfoException e) {
            log.warn("Unable to set Application name", e);
            e.printStackTrace();
        }

    }

    private void attachGlobal(int oid, int targetPID) throws DBGException {

        try (Statement stmt = getConnection().createStatement(); ResultSet rs = stmt.executeQuery(SQL_LISTEN)) {

            if (rs.next()) {
                sessionId = rs.getInt("sessionid");
                getConnection().setClientInfo("ApplicationName", "Debug Mode : " + String.valueOf(sessionId));
            } else {
                throw new DBGException("Unable to create debug instance");
            }

        } catch (SQLException e) {
            throw new DBGException("SQL error", e);
        }

        PostgreDebugBreakpointProperties properties = new PostgreDebugBreakpointProperties(true);
        bpGlobal = new PostgreDebugBreakpointDescriptor(oid, properties);
        addBreakpoint(bpGlobal);

        String sessionParam = String.valueOf(getSessionId());
        String taskName = sessionParam + " global attached to " + String.valueOf(targetId);
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
     * @param connection
     *            - connection for debug session after attach this connection
     *            will forever belong to debug
     * @param OID
     *            - OID for target procedure
     * @param targetPID
     *            - target session PID (-1 for any target)
     * @param global
     *            - is target session global
     * @param call
     *            - SQL call for target session
     * @throws DBGException
     */
    public void attach(JDBCExecutionContext connection, int OID, int targetPID, boolean global, String call)
            throws DBGException {

        lock.writeLock().lock();

        try {

            setConnection(connection);

            if (global) {
                attachKind = PostgreDebugAttachKind.GLOBAL;
                attachGlobal(OID, targetPID);
            } else {
                attachKind = PostgreDebugAttachKind.LOCAL;
                attachLocal(OID, call);
            }

        } finally {
            lock.writeLock().unlock();
        }

    }

    private void detachLocal() throws DBGException {
        if (!isWaiting()) {
            try (Statement stmt = getConnection().createStatement()) {
                String sqlCommand = composeAbortCommand();
                stmt.execute(sqlCommand);
            } catch (SQLException e) {
                log.error("Unable to abort target", e);
            }
        }
        if (job != null) {
            job.cancel();
            job = null;
        }
        if (executionTarget != null) {
            try {
                executionTarget.close();
            } catch (SQLException e) {
                log.error("Unable to close target session", e);
            }
        }
    }

    private void detachGlobal() throws DBGException {
        if (!isWaiting() && !isDone()) {
            try (Statement stmt = getConnection().createStatement()) {
                String sql = SQL_CONTINUE.replaceAll("\\?sessionid", String.valueOf(sessionId));
                stmt.execute(sql);
            } catch (SQLException e) {
                log.error("Unable to abort target", e);
            }

        }
        removeBreakpoint(bpGlobal);
    }

    protected void doDetach() throws DBGException {
        switch (attachKind) {
        case GLOBAL:
            detachGlobal();
        case LOCAL:
            detachLocal();
        default:
        }
    }

    @Override
    public DBGSessionInfo getSessionInfo() {
        return sessionInfo;
    }

    protected String composeAddBreakpointCommand(DBGBreakpointDescriptor descriptor) {
        PostgreDebugBreakpointDescriptor bp = (PostgreDebugBreakpointDescriptor) descriptor;
        PostgreDebugBreakpointProperties bpd = (PostgreDebugBreakpointProperties) bp.getProperties();
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
     * @throws DBGException
     */
    public void execStep(String commandPattern, String nameParameter, int eventDetail) throws DBGException {

        acquireWriteLock();

        try {
            String sql = commandPattern.replaceAll("\\?sessionid", String.valueOf(sessionId));
            String taskName = String.valueOf(sessionId) + nameParameter + String.valueOf(targetId);
            DBGEvent begin = new DBGEvent(this, DBGEvent.RESUME, eventDetail);
            DBGEvent end = new DBGEvent(this, DBGEvent.SUSPEND, eventDetail);
            runAsync(sql, taskName, begin, end);
        } finally {
            lock.writeLock().unlock();
        }

    }

    protected String composeAbortCommand() {
        return SQL_ABORT.replaceAll("\\?sessionid", String.valueOf(sessionId));
    }

    @Override
    public List<DBGVariable<?>> getVariables() throws DBGException {

        acquireReadLock();

        List<DBGVariable<?>> vars = new ArrayList<>();

        String sql = SQL_GET_VARS.replaceAll("\\?sessionid", String.valueOf(sessionId));
        try (Statement stmt = getConnection().createStatement(); ResultSet rs = stmt.executeQuery(sql)) {

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

        } catch (SQLException e) {
            throw new DBGException("SQL error", e);
        } finally {
            lock.readLock().unlock();
        }

        return vars;

    }

    @Override
    public void setVariableVal(DBGVariable<?> variable, Object value) throws DBGException {

        acquireReadLock();

        try (PreparedStatement stmt = getConnection().prepareStatement(SQL_SET_VAR)) {

            if (variable instanceof PostgreDebugVariable) {

                if (value instanceof String) {

                    PostgreDebugVariable var = (PostgreDebugVariable) variable;

                    stmt.setInt(1, sessionId);
                    stmt.setString(2, var.getName());
                    stmt.setInt(3, var.getLineNumber());
                    stmt.setString(4, (String) value);

                    stmt.execute();

                } else {
                    lock.readLock().unlock();
                    throw new DBGException("Incorrect variable value class");
                }

            } else {
                lock.readLock().unlock();
                throw new DBGException("Incorrect variable class");
            }

        } catch (SQLException e) {
            throw new DBGException("SQL error", e);
        } finally {
            lock.readLock().unlock();
        }

    }

    @Override
    public List<DBGStackFrame> getStack() throws DBGException {
        acquireReadLock();

        List<DBGStackFrame> stack = new ArrayList<DBGStackFrame>(1);

        String sql = SQL_GET_STACK.replaceAll("\\?sessionid", String.valueOf(getSessionId()));
        try (Statement stmt = getConnection().createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                int level = rs.getInt("level");
                String targetname = rs.getString("targetname");
                int func = rs.getInt("func");
                int linenumber = rs.getInt("linenumber");
                String args = rs.getString("args");
                PostgreDebugStackFrame frame = new PostgreDebugStackFrame(level, targetname, func, linenumber, args);
                stack.add(frame);
            }

        } catch (SQLException e) {
            throw new DBGException("SQL error", e);
        } finally {
            lock.readLock().unlock();
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

        acquireReadLock();

        String src = "";

        String sql = SQL_GET_SRC.replaceAll("\\?sessionid", String.valueOf(sessionId)).replaceAll("\\?oid",
                String.valueOf(OID));
        try (Statement stmt = getConnection().createStatement(); ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                src = rs.getString(1);
            }

        } catch (SQLException e) {
            throw new DBGException("SQL error", e);
        } finally {
            lock.readLock().unlock();
        }

        return src;

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
     * 
     * @return DBGStackFrame
     */

    public void selectFrame(int frameNumber) throws DBGException {

        acquireReadLock();

        String pattern = SQL_SELECT_FRAME;
        pattern = "select * from pldbg_select_frame(?sessionid,?frameno)";
        String sql = pattern.replaceAll("\\?sessionid", String.valueOf(sessionId)).replaceAll("\\?frameno",
                String.valueOf(frameNumber));

        try (Statement stmt = getConnection().createStatement(); ResultSet rs = stmt.executeQuery(sql)) {

            if (!rs.next()) {
                throw new DBGException("Unable to select frame");
            }

        } catch (SQLException e) {
            throw new DBGException("SQL error", e);
        } finally {
            lock.readLock().unlock();
        }

    }

    @Override
    public String toString() {
        return "PostgreDebugSession " + (isWaiting() ? "WAITING" : "READY") + " [sessionId=" + sessionId
                + ", breakpoints=" + getBreakpoints() + "targetId=(" + targetId + ") Session=(" + sessionInfo.toString()
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
            return super.isAttached() && (sessionId > 0);
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
            if (task == null) {
                return true;
            }
            if (task.isDone()) {
                try {
                    task.get();
                } catch (InterruptedException e) {
                    log.error("DEBUG INTERRUPT ERROR ", e);
                    return false;
                } catch (ExecutionException e) {
                    log.error("DEBUG WARNING ", e);
                    return false;
                }
                return true;
            }
            return false;

        case LOCAL:
            return sessionId > 0;

        default:
            return false;
        }
    }

}
