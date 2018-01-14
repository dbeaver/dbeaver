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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.debug.DBGBreakpoint;
import org.jkiss.dbeaver.debug.DBGBreakpointProperties;
import org.jkiss.dbeaver.debug.DBGException;
import org.jkiss.dbeaver.debug.DBGObject;
import org.jkiss.dbeaver.debug.DBGSession;
import org.jkiss.dbeaver.debug.DBGSessionInfo;
import org.jkiss.dbeaver.debug.DBGStackFrame;
import org.jkiss.dbeaver.debug.DBGVariable;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;

/**
 * Typical scenario for debug session <br/>
 * <br/>
 * 0. create session (now it can only attached to target Procedure)<br/><br/>
 * 1. attach to target this method attaches to a debugging target and listening on the given port - 
 *    waiting for run procedure in other session(s) debugger client should invoke this function after creation
 *    also created implicit breakpoint for target procedure, after this call debug session in <b>WAITING</b> state - 
 *    isDone returns false and is isWaiting returns true<br/><br/>
 * 2. when target procedure will called debug session implicit breakpoint will be reached
 *    and session goes in state <b>READY</b> (isDone - true, isWaiting - true) in this state possible to call
 *    getStack, getVariables, setVariables, setBreakpoint or execStepXXX\continue<br/><br/>
 * 3. when execStepXXX or continue will called session goes in <b>WAITING</b> state until next breakpoint or end of 
 *    procedure will be reached     <br/>
 *
 */
@SuppressWarnings("nls")
public class PostgreDebugSession implements DBGSession {

    private static final Log log = Log.getLog(PostgreDebugSession.class);
    
    private final PostgreDebugSessionInfo sessionManagerInfo;

    private final PostgreDebugSessionInfo sessionDebugInfo;

    private JDBCExecutionContext connection = null;

    private final String title;

    private int sessionId = -1;

    private static final String SQL_ATTACH = "select pldbg_wait_for_target(?sessionid)";

    private static final String SQL_ATTACH_BREAKPOINT = "select pldbg_wait_for_breakpoint(?sessionid)";

    private static final String SQL_LISTEN = "select pldbg_create_listener() as sessionid";

    private static final String SQL_GET_VARS = "select * from pldbg_get_variables(?sessionid)";
    
    private static final String SQL_SET_VAR = "select pldbg_deposit_value(?,?,?,?)";

    private static final String SQL_GET_STACK = "select * from pldbg_get_stack(?sessionid)";

    private static final String SQL_STEP_OVER = "select pldbg_step_over(?sessionid)";

    private static final String SQL_STEP_INTO = "select pldbg_step_into(?sessionid)";

    private static final String SQL_CONTINUE = "select pldbg_continue(?sessionid)";

    private static final String SQL_ABORT = "select pldbg_abort_target(?sessionid)";

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);

    private List<PostgreDebugBreakpoint> breakpoints = new ArrayList<PostgreDebugBreakpoint>(1);
    
    private PostgreDebugBreakpoint entry = null;

    private FutureTask<Void> task;

    private Thread workerThread = null;

    /**
     * This method attach debug session to debug object (procedure) 
     * and wait forever while target or any (depend on targetPID) session will run target procedure  
     * 
     * @param connection - connection for debug session after attach this connection will forever belong to debug
     * @param OID - OID for target procedure
     * @param targetPID - target session PID (-1 for any target)
     * @throws DBGException
     */
    public void attach(JDBCExecutionContext connection,int OID,int targetPID) throws DBGException {

        lock.writeLock().lock();

        try {
            
            this.connection = connection;
            
            try (Statement stmt = getConnection(connection).createStatement();
                    ResultSet rs = stmt.executeQuery(SQL_LISTEN)) {

                if (rs.next()) {
                    getConnection(connection).setClientInfo("ApplicationName", "Debug Mode : " + String.valueOf(sessionId));
                    sessionId =  rs.getInt("sessionid");
                } else {
                    throw new DBGException("Unable to create debug instance");
                }

            } catch (SQLException e) {
                throw new DBGException("SQL error", e);
            } 

            PostgreDebugBreakpointProperties properties = new PostgreDebugBreakpointProperties(true);
            PostgreDebugObject obj = new PostgreDebugObject(OID,"ENTRY","SESSION","THIS","PG"); 
            
            this.entry = new PostgreDebugBreakpoint(this,obj,properties);
            
            runAsync(SQL_ATTACH.replaceAll("\\?sessionid", String.valueOf(sessionId)),
                    String.valueOf(sessionId) + " global attached to " + String.valueOf(sessionManagerInfo.pid));

            /*if (breakpoint) {
                runAsync(SQL_ATTACH_BREAKPOINT.replaceAll("\\?sessionid", String.valueOf(sessionId)),
                        String.valueOf(sessionId) + " breakpoint attached to "
                                + String.valueOf(sessionManagerInfo.pid));

            } else {
                runAsync(SQL_ATTACH.replaceAll("\\?sessionid", String.valueOf(sessionId)),
                        String.valueOf(sessionId) + " global attached to " + String.valueOf(sessionManagerInfo.pid));
            }*/

        } finally {
            lock.writeLock().unlock();
        }

    }

    /**
     * Create session with two description 
     * after creation session need to be attached to postgres procedure by attach method
     * 
     * @param sessionManagerInfo - manager (caller connection) description
     * @param sessionDebugInfo - session (debugger client connection) description
     * @throws DBGException
     */
    public PostgreDebugSession(PostgreDebugSessionInfo sessionManagerInfo, PostgreDebugSessionInfo sessionDebugInfo) throws DBGException {
        this.sessionManagerInfo = sessionManagerInfo;
        this.sessionDebugInfo = sessionDebugInfo;
        this.title = sessionManagerInfo.application;

    }

    /**
     * @param connectionTarget - DBCExecutionContext of debug client (will be used in debug process)
     * @return Connection - java.sql.Connection
     * @throws SQLException
     */
    private static Connection getConnection(DBCExecutionContext connectionTarget) throws SQLException {
        return ((JDBCExecutionContext) connectionTarget).getConnection(new VoidProgressMonitor());
    }


    @Override
    public DBGSessionInfo getSessionInfo() {
        return sessionDebugInfo;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public List<PostgreDebugBreakpoint> getBreakpoints() {
        return breakpoints;
    }

    @Override
    public DBGBreakpoint setBreakpoint(DBGObject obj, DBGBreakpointProperties properties) throws DBGException {

        acquireReadLock();

        PostgreDebugBreakpoint bp = null;

        try {
            bp = new PostgreDebugBreakpoint(this, (PostgreDebugObject)obj, (PostgreDebugBreakpointProperties) properties);
            breakpoints.add(bp);

        } finally {
            lock.readLock().unlock();
        }

        return bp;
    }

    @Override
    public void removeBreakpoint(DBGBreakpoint bp) throws DBGException {

        acquireReadLock();

        try {

            bp.drop();

            breakpoints.remove(bp);

        } finally {
            lock.readLock().unlock();
        }

    }

    @Override
    public void execContinue() throws DBGException {
        execStep(SQL_CONTINUE, " continue for ");

    }

    @Override
    public void execStepInto() throws DBGException {
        execStep(SQL_STEP_INTO, " step into for ");

    }

    @Override
    public void execStepOver() throws DBGException {
        execStep(SQL_STEP_OVER, " step over for ");

    }

    /**
     * Execute step SQL command  asynchronously, set debug session name to 
     * [sessionID] name [managerPID] 
     * 
     * @param commandSQL - SQL command for execute step
     * @param name - session 'name' part
     * @throws DBGException
     */
    public void execStep(String commandSQL, String name) throws DBGException {

        acquireWriteLock();

        try {

            runAsync(commandSQL.replaceAll("\\?sessionid", String.valueOf(sessionId)),
                    String.valueOf(sessionId) + name + String.valueOf(sessionManagerInfo.pid));

        } finally {
            lock.writeLock().unlock();
        }

    }

    @Override
    public void abort() throws DBGException {

        acquireReadLock();

        try (Statement stmt = getConnection(connection).createStatement()) {

            stmt.execute(SQL_ABORT.replaceAll("\\?sessionid", String.valueOf(sessionId)));

            task = null;

        } catch (SQLException e) {
            throw new DBGException("SQL error", e);
        } finally {
            lock.readLock().unlock();
        }

    }

    public void close() {

        lock.writeLock().lock();

        try {

                if (!isDone()) {
                    task.cancel(true);
                }

                connection.close();

        } finally {
            lock.writeLock().unlock();
        }

    }

    @Override
    public List<DBGVariable<?>> getVarables() throws DBGException {

        acquireReadLock();

        List<DBGVariable<?>> vars = new ArrayList<>();

        String sql = SQL_GET_VARS.replaceAll("\\?sessionid", String.valueOf(sessionId));
        try (Statement stmt = getConnection(connection).createStatement(); ResultSet rs = stmt.executeQuery(sql)) {

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
            
            try (PreparedStatement stmt = getConnection(connection).prepareStatement(SQL_SET_VAR)) {
                
              if (variable instanceof PostgreDebugVariable){  
                
                if (value instanceof String){
                    
                    PostgreDebugVariable var = (PostgreDebugVariable) variable;
                    
                    stmt.setInt(1,sessionId);
                    stmt.setString(2,var.getName());
                    stmt.setInt(3,var.getLinenumber());
                    stmt.setString(4,(String) value);
                    
                    stmt.execute();

                }else {
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

        String sql = SQL_GET_STACK.replaceAll("\\?sessionid", String.valueOf(sessionId));
        try (Statement stmt = getConnection(connection).createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
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

    /**
     * Return connection used in debug session 
     * 
     * @return java.sql.Connection
     * @throws DBGException
     */
    public Connection getConnection() throws DBGException {
        try {
            return getConnection(connection);
        } catch (SQLException e) {
            throw new DBGException("SQL error", e);
        }
    }

    @Override
    public String toString() {
        return "PostgreDebugSession " + (isWaiting() ? "WAITING" : "READY") + " [connection=" + connection + ", title="
                + title + ", sessionId=" + sessionId + ", breakpoints=" + breakpoints + "ManagerSession=("
                + sessionManagerInfo.toString() + ") Session=(" + sessionDebugInfo.toString() + ") " + "]";
    }

    @Override
    public Integer getSessionId() {
        return sessionId;
    }
    
    /**
     * Return true if session up and running debug thread
     * 
     * @return boolean
     */
    public boolean isWaiting() {
        return (task == null ? false : !task.isDone()) && (workerThread == null ? false : workerThread.isAlive());
    }

    /**
     * Return true if session waiting target connection (on breakpoint, after step or continue) in debug thread
     * 
     * @return boolean
     */
    public boolean isDone(){

        if (task == null)
            return true;

        if (task.isDone()) {
            try {
                task.get();
            } catch (InterruptedException e) {
                log.error("DEBUG INTERRUPT ERROR ",e);
                return false;
            } catch (ExecutionException e) {
                log.error("DEBUG WARNING ",e);
                return false;
            }
            return true;

        }

        return false;

    }
    
    /**
     * Return true if debug session up and running on server 
     * 
     * @return boolean
     */
    public boolean isAttached() {
        return (connection != null && sessionId > 0);
    }

    /**
     *  Start thread for SQL command
     * 
     * @param commandSQL
     * @param name
     * @throws DBGException
     */
    private void runAsync(String commandSQL, String name) throws DBGException {

        Connection connection = getConnection();
        try (Statement stmt = connection.createStatement()) {

            connection.setAutoCommit(false);

            PostgreDebugSessionWorker worker = new PostgreDebugSessionWorker(connection, commandSQL);

            task = new FutureTask<Void>(worker);

            workerThread = new Thread(task);

            workerThread.setName(name);

            workerThread.start();

        } catch (SQLException e) {
            throw new DBGException("SQL error", e);

        }
    }

    /**
     * Try to acquire shared lock 
     * 
     * @throws DBGException
     */
    private void acquireReadLock() throws DBGException {

        try {

            lock.readLock().lockInterruptibly();

        } catch (InterruptedException e1) {

            throw new DBGException(e1.getMessage());

        }

        if (!isAttached()) {
            lock.readLock().unlock();
            throw new DBGException("Debug session not attached");
        }
        
        if (isWaiting()) {
            lock.readLock().unlock();
            throw new DBGException("Debug session in waiting state");
        }

        if (!isDone()) {
            lock.readLock().unlock();
            throw new DBGException("Debug session in incorrect state");
        }

    }

    /**
     *  Try to acquire exclusive lock 
     * 
     * @throws DBGException
     */
    private void acquireWriteLock() throws DBGException {

        try {

            lock.writeLock().lockInterruptibly();

        } catch (InterruptedException e1) {
            throw new DBGException(e1.getMessage());
        }
        
        if (!isAttached()) {
            lock.writeLock().unlock();
            throw new DBGException("Debug session not attached");
        }

        if (isWaiting()) {
            lock.writeLock().unlock();
            throw new DBGException("Debug session in waiting state");
        }

        if (!isDone()) {
            lock.writeLock().unlock();
            throw new DBGException("Debug session in incorrect state");
        }

    }

}
