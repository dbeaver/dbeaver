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

import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.debug.*;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@SuppressWarnings("nls")
public class PostgreDebugSession implements DBGSession<PostgreDebugSessionInfo, PostgreDebugObject> {

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
    
    private PostgreDebugBreakpoint entry;

    private FutureTask<Void> task;

    private Thread workerThread = null;

    public void attach(JDBCExecutionContext connection,int OID,int targetPID) throws DBGException {

        lock.writeLock().lock();

        try {
            
            this.connection = connection;
            
            try (Statement stmt = getConnection(connection).createStatement()) {

                ResultSet rs = stmt.executeQuery(SQL_LISTEN);

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
            
            entry = new PostgreDebugBreakpoint(this,obj,properties);
            
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

    public PostgreDebugSession(PostgreDebugSessionInfo sessionManagerInfo, PostgreDebugSessionInfo sessionDebugInfo) throws DBGException {
        this.sessionManagerInfo = sessionManagerInfo;
        this.sessionDebugInfo = sessionDebugInfo;
        this.title = sessionManagerInfo.application;

    }

    private static Connection getConnection(DBCExecutionContext connectionTarget) throws SQLException {
        return ((JDBCExecutionContext) connectionTarget).getConnection(new VoidProgressMonitor());
    }

    @Override
    public PostgreDebugSessionInfo getSessionInfo() {

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
    public DBGBreakpoint setBreakpoint(PostgreDebugObject obj, DBGBreakpointProperties properties) throws DBGException {

        acquireReadLock();

        PostgreDebugBreakpoint bp = null;

        try {
            bp = new PostgreDebugBreakpoint(this, obj, (PostgreDebugBreakpointProperties) properties);
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

        try (Statement stmt = getConnection(connection).createStatement()) {

            ResultSet rs = stmt.executeQuery(SQL_GET_VARS.replaceAll("\\?sessionid", String.valueOf(sessionId)));

            while (rs.next()) {

                PostgreDebugVariable var = new PostgreDebugVariable(rs.getString("name"), rs.getString("varclass"),
                        rs.getInt("linenumber"), rs.getBoolean("isunique"), rs.getBoolean("isconst"),
                        rs.getBoolean("isnotnull"), rs.getInt("dtype"), rs.getString("value"));

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

        try (Statement stmt = getConnection(connection).createStatement()) {

            ResultSet rs = stmt.executeQuery(SQL_GET_STACK.replaceAll("\\?sessionid", String.valueOf(sessionId)));

            while (rs.next()) {
                PostgreDebugStackFrame frame = new PostgreDebugStackFrame(rs.getInt("level"), rs.getString("targetname"),
                        rs.getInt("func"), rs.getInt("linenumber"), rs.getString("args"));
                stack.add(frame);
            }

        } catch (SQLException e) {
            throw new DBGException("SQL error", e);
        } finally {
            lock.readLock().unlock();
        }
        return stack;
    }

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

    public boolean isWaiting() {
        return (task == null ? false : !task.isDone()) && (workerThread == null ? false : workerThread.isAlive());
    }

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
    
    public boolean isAttached() {
        return (connection != null && sessionId > 0);
    }

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
